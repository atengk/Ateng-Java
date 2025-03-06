# FastJson2

FASTJSON v2是FASTJSON项目的重要升级，目标是为下一个十年提供一个高性能的JSON库。通过同一套API，

支持JSON/JSONB两种协议，JSONPath是一等公民。
支持全量解析和部分解析。
支持Java服务端、客户端Android、大数据场景。
支持Kotlin
支持 JSON Schema https://github.com/alibaba/fastjson2/wiki/json_schema_cn
支持Android (2.0.10.android)
支持Graal Native-Image (2.0.10.graal)

- [官方文档](https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn)
- [自定义序列化和反序列化](https://github.com/alibaba/fastjson2/wiki/register_custom_reader_writer_cn)
- [Features配置](https://github.com/alibaba/fastjson2/wiki/Features_cn)



## 使用Fastjson2

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





## 常用注解及使用方法

### 常用注解

#### 1. `@JSONField` - 指定序列化和反序列化的字段名称

**作用**：用于指定字段在 JSON 中的名称，可以用于字段、getter 或 setter 方法，支持对字段进行更细粒度的配置，如格式化日期、指定序列化特性等。

**示例**：

```java
import com.alibaba.fastjson.annotation.JSONField;

public class User {
    private int id;

    @JSONField(name = "full_name")
    private String name;

    @JSONField(format = "yyyy-MM-dd")
    private Date birthDate;

    // 省略构造方法、getter 和 setter
}
```

**序列化结果**：

```json
{
    "id": 1,
    "full_name": "Tom",
    "birthDate": "1990-01-01"
}
```

#### 2. `@JSONField(serialize = false)` - 禁用字段序列化

**作用**：标记某个字段在序列化时被忽略，不会出现在 JSON 输出中。

**示例**：

```java
import com.alibaba.fastjson.annotation.JSONField;

public class User {
    private int id;

    @JSONField(serialize = false)
    private String password;

    // 省略构造方法、getter 和 setter
}
```

**序列化结果**：

```json
{
    "id": 1
}
```

---

#### 3. `@JSONField(deserialize = false)` - 禁用字段反序列化

**作用**：标记某个字段在反序列化时被忽略，Fastjson 会忽略传入 JSON 中的该字段。

**示例**：

```java
import com.alibaba.fastjson.annotation.JSONField;

public class User {
    private int id;

    @JSONField(deserialize = false)
    private String password;

    // 省略构造方法、getter 和 setter
}
```

**反序列化时**，即使 JSON 中包含 `"password"` 字段，Fastjson 会忽略并不会赋值给该字段。

---

#### 4. **大整数转字符串** - `@JSONField(serializeFeatures = JSONWriter.Feature.WriteBigDecimalAsPlain)`

**作用**：避免大整数或 BigDecimal 失真，转换为字符串输出。

```java
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.JSONWriter;
import java.math.BigDecimal;

public class Order {
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteBigDecimalAsPlain)
    private BigDecimal amount;
}
```

**输入数据**：

```json
{
    "amount": 9999999999999999999.99
}
```

**序列化结果**：

```json
{
    "amount": "9999999999999999999.99"
}
```

---

#### 5. **小数保留固定位数** - `@JSONField(format = "#0.00")`

**作用**：控制小数位数。

```java
import com.alibaba.fastjson2.annotation.JSONField;
import java.math.BigDecimal;

public class Product {
    @JSONField(format = "#0.00")
    private BigDecimal price;
}
```

**序列化结果**：

```json
{
    "price": "123.46"
}
```

---

#### 6. **小数转百分比** - `@JSONField(format = "#0.##%")`

**作用**：把小数转换为百分比格式。

```java
import com.alibaba.fastjson2.annotation.JSONField;
import java.math.BigDecimal;

public class Rate {
    @JSONField(format = "#0.##%")
    private BigDecimal discountRate;
}
```

**输入对象**：

```java
Rate rate = new Rate();
rate.discountRate = new BigDecimal("0.075");
```

**序列化结果**：

```json
{
    "discountRate": "7.5%"
}
```

#### 7. `@JSONField(ordinal = 1)` - 控制字段序列化顺序

**作用**：指定字段在序列化时的顺序，可以通过 `ordinal` 属性来控制。

**示例**：

```java
import com.alibaba.fastjson.annotation.JSONField;

public class User {
    @JSONField(ordinal = 2)
    private int id;

    @JSONField(ordinal = 1)
    private String name;

    // 省略构造方法、getter 和 setter
}
```

**序列化结果**：

```json
{
    "name": "Tom",
    "id": 1
}
```

---

#### 8. `@JSONField(defaultValue = "default")` - 设置默认值

**作用**：为字段设置一个默认值，在序列化或反序列化时，如果字段值为 `null` 或缺失，则使用该默认值。

**示例**：

```java
import com.alibaba.fastjson.annotation.JSONField;

public class User {
    private int id;

    @JSONField(defaultValue = "Anonymous")
    private String name;

    // 省略构造方法、getter 和 setter
}
```

**序列化结果**：

```json
{
    "id": 1,
    "name": "Anonymous"
}
```

#### 9. **自定义序列化** - `@JSONField(serializeUsing = xx.class)`

**作用**：用于指定自定义的序列化逻辑。

```java
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;

public class Product {
    @JSONField(serializeUsing = PriceSerializer.class)
    private double price;
}

// 自定义序列化器
class PriceSerializer implements ObjectWriter<Double> {
    @Override
    public void write(JSONWriter jsonWriter, Object price, Object fieldName, Type fieldType, long features) {
        jsonWriter.writeString("$" + String.format("%.2f", price));
    }
}
```

**序列化结果**：

```json
{
    "price": "$99.99"
}
```

---

#### 10. **自定义反序列化** - `@JSONField(deserializeUsing = xx.class)`

**作用**：用于自定义反序列化逻辑。

```java
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;

public class Product {
    @JSONField(deserializeUsing = PriceDeserializer.class)
    private double price;
}

// 自定义反序列化器
class PriceDeserializer implements ObjectReader<Double> {
    @Override
    public Double readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        String value = jsonReader.readString();
        return Double.parseDouble(value.replace("$", ""));
    }
}
```

**输入 JSON**：

```json
{
    "price": "$99.99"
}
```

**反序列化**：

```java
Product product = JSON.parseObject(json, Product.class);
System.out.println(product.price); // 输出: 99.99
```

#### 11. @JSONType 全局控制类序列化和反序列化

##### **1. `orders` - 指定字段的序列化顺序**

**作用**：控制 JSON 输出时的字段顺序。

```java
import com.alibaba.fastjson2.annotation.JSONType;

@JSONType(orders = {"name", "age", "id"})
public class User {
    private int id;
    private String name;
    private int age;

    // 省略构造方法、getter 和 setter
}
```

**序列化结果**：

```json
{
    "name": "Tom",
    "age": 25,
    "id": 1
}
```

> **注意**：`orders` 指定的字段顺序会优先于 `@JSONField(ordinal = xx)`

---

##### **2. `ignores` - 忽略指定字段**

**作用**：不让某些字段出现在 JSON 序列化结果中，相当于 `@JSONField(serialize = false)`。

```java
import com.alibaba.fastjson2.annotation.JSONType;

@JSONType(ignores = {"password", "phoneNumber"})
public class User {
    private int id;
    private String name;
    private String password;
    private String phoneNumber;
}
```

**序列化结果**（`password` 和 `phoneNumber` 被忽略）：

```json
{
    "id": 1,
    "name": "Tom"
}
```

---

##### **3. `naming` - 统一修改字段命名风格**

**作用**：自动转换 JSON 字段的命名风格（如驼峰转下划线）。

```java
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.PropertyNamingStrategy;

@JSONType(naming = PropertyNamingStrategy.SnakeCase)
public class User {
    private int userId;
    private String userName;
}
```

**序列化结果**（驼峰命名转换为下划线）：

```json
{
    "user_id": 1,
    "user_name": "Tom"
}
```

> **支持的命名策略**：
>
> - `CamelCase`（默认）- 驼峰命名（userName）
> - `SnakeCase` - 下划线命名（user_name）
> - `KebabCase` - 短横线命名（user-name）
> - `UpperCase` - 全大写（USERNAME）

---

##### **4. `serializeFeatures` - 启用额外的序列化特性**

**作用**：启用 `Fastjson2` 的特定序列化规则，例如 **忽略 `null` 值等**。

```java
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;

import java.util.Date;

@JSONType(serializeFeatures = {JSONWriter.Feature.WriteMapNullValue})
public class Order {
    private int id;
    private Date createTime;
    private String remark; // 可能为 null

    // 省略构造方法、getter 和 setter
}
```

**序列化结果**（`null` 字段不被忽略，日期使用 ISO8601 格式）：

```json
{
    "id": 1,
    "createTime": "2024-03-06T15:30:00Z",
    "remark": null
}
```

> **常见 `serializeFeatures`：**
>
> - `WriteNulls`：输出 `null` 值字段（默认不输出 `null`）。
> - `WriteBigDecimalAsPlain`：防止 `BigDecimal` 以科学计数法表示。
> - `PrettyFormat`：格式化 JSON 以便于阅读。

---

##### **5. `deserializeFeatures` - 反序列化时启用额外特性**

**作用**：修改 JSON 解析时的行为，比如 **忽略大小写、自动去掉下划线等**。

```java
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONType;

@JSONType(deserializeFeatures = JSONReader.Feature.SupportSmartMatch)
public class User {
    private int userId;
    private String userName;
}
```

**输入 JSON**（即使字段大小写不同或有下划线，也能正确解析）：

```json
{
    "User_Id": 1,
    "USER_NAME": "Tom"
}
```

> **常见 `deserializeFeatures`：**
>
> - `SupportSmartMatch`：忽略字段大小写、下划线等自动匹配字段。
> - `IgnoreSetNullValue`：如果 JSON 里有 `null`，不会赋值给对象的属性。

---

##### **6. `@JSONType` + `@JSONField` 组合使用**

**作用**：`@JSONType` 设置全局规则，`@JSONField` 控制单个字段。

```java
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.annotation.JSONField;

@JSONType(naming = PropertyNamingStrategy.SnakeCase, ignores = {"password"})
public class User {
    private int userId;
    private String userName;

    @JSONField(serialize = false)
    private String password;

    @JSONField(format = "#0.00")
    private double balance;
}
```

**序列化结果**（`password` 被忽略，`balance` 保留两位小数）：

```json
{
    "user_id": 1,
    "user_name": "Tom",
    "balance": "99.99"
}
```



### 使用方法

#### 创建实体类

```java
package local.ateng.java.serialize.entity;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.annotation.JSONType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JSONType(orders = {"name", "age", "id"}, naming = PropertyNamingStrategy.SnakeCase, serializeFeatures = {JSONWriter.Feature.WriteNulls, JSONWriter.Feature.PrettyFormat})
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @JSONField(ordinal = 1, serializeFeatures = JSONWriter.Feature.WriteLongAsString)
    private Long id;

    /**
     * 名称
     */
    @JSONField(name = "full_name", label = "User Name")
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
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteBigDecimalAsPlain)
    private BigDecimal score;

    /**
     * 比例
     */
    @JSONField(format = "##.##%")
    private Double ratio;

    /**
     * 生日
     */
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @JSONField(defaultValue = "Chongqing")
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createTime;
    private Date createTime2;
    private Date createTime3;
    private int num;
    private List<String> list;

}
```



#### 使用JSON

使用测试类来进行演示

```java
package local.ateng.java.serialize;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.serialize.entity.MyUser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Fastjson2AnnotationTests {

    @Test
    public void serialization() {
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
                .province(null)
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .build();
        // 进行序列化
        String json = JSONObject.toJSONString(myUser);
        System.out.println(json);
    }

    @Test
    public void deserialization() {
        // 创建数据
        String json = "{\"id\":\"1\",\"age\":25,\"phoneNumber\":\"1762306666\",\"email\":\"kongyu2385569970@gmail.com\",\"score\":\"88.91\",\"ratio\":0.7147,\"birthday\":\"2000-01-01\",\"province\":\"重庆市\",\"city\":\"重庆市\",\"createTime\":\"2025-03-05 11:02:56\",\"full_name\":\"ateng\"}";
        // 进行反序列化
        MyUser myUser = JSONObject.parseObject(json, MyUser.class);;
        System.out.println(myUser);
    }

}
```

序列化serialization结果：

```json
{"age":25,"id":null,"birthday":"2000-01-01","city":"重庆市","create_time":"2025-03-06 10:01:52.693","create_time2":null,"create_time3":null,"email":"kongyu2385569970@gmail.com","full_name":"ateng","num":0,"phone_number":"1762306666","province":null,"ratio":null,"score":88.911}
```

反序列化deserialization结果

```
MyUser(id=1, name=ateng, age=25, phoneNumber=null, email=kongyu2385569970@gmail.com, score=88.91, ratio=0.7147, birthday=2000-01-01, province=重庆市, city=重庆市, createTime=null, createTime2=null, createTime3=null, num=0, list=null)
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

参考：[官方文档](https://github.com/alibaba/fastjson2/wiki/register_custom_reader_writer_cn)

### 自定义序列化

自定义序列化允许你控制对象在序列化过程中的行为，特别是当你需要将某些对象转换为特殊格式时（如日期、金额、对象的某些字段等）。

#### 1. **实现 `ObjectWriter` 接口自定义序列化**

在 Fastjson2 中，序列化过程是通过实现 `ObjectWriter` 接口来完成的。你可以实现该接口来自定义如何序列化某个类。

**步骤**：

1. 创建自定义的 `ObjectWriter`。
2. 使用 `@JSONField(serializeUsing = CustomSerializer.class)` 注解指定自定义序列化器。

**示例：自定义金额序列化器**

假设你希望在序列化时将金额字段格式化为 `￥100.00`。

```java
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;

public class Product {
    private String name;
    @JSONField(serializeUsing = PriceSerializer.class)  // 指定自定义序列化器
    private double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }
}

// 自定义序列化器：将金额格式化为人民币符号
class PriceSerializer implements ObjectWriter<Double> {
    @Override
    public void write(JSONWriter jsonWriter, Object value, Object fieldName, Type fieldType, long features) {
        String formattedPrice = "￥" + String.format("%.2f", value);
        jsonWriter.writeString(formattedPrice);
    }
}
```

**序列化结果**：

```json
{
    "name": "Laptop",
    "price": "￥1999.99"
}
```

---

#### 2. **自定义枚举序列化**

你可能希望将枚举的值自定义为特定的字符串，而不是默认的 `name()` 方法返回值。

**示例：自定义枚举序列化**

```java
import com.alibaba.fastjson2.annotation.JSONField;

public class Order {
    private int id;
    
    @JSONField(serializeUsing = StatusSerializer.class)
    private OrderStatus status;

    public Order(int id, OrderStatus status) {
        this.id = id;
        this.status = status;
    }
}

enum OrderStatus {
    PENDING,
    SHIPPED,
    DELIVERED
}

// 自定义枚举序列化器
class StatusSerializer implements ObjectWriter<OrderStatus> {
    @Override
    public void write(JSONWriter jsonWriter, Object status, Object fieldName, Type fieldType, long features) {
        switch (status) {
            case PENDING:
                jsonWriter.writeString("Processing");
                break;
            case SHIPPED:
                jsonWriter.writeString("In Transit");
                break;
            case DELIVERED:
                jsonWriter.writeString("Delivered");
                break;
            default:
                jsonWriter.writeString("Unknown");
                break;
        }
    }
}
```

**序列化结果**：

```json
{
    "id": 123,
    "status": "Processing"
}
```

---

#### 3. **自定义日期格式化**

如果你需要自定义日期的序列化格式，可以实现 `ObjectWriter` 来定制日期的输出格式。

**示例：自定义日期格式**

```java
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Event {
    private String name;
    
    @JSONField(serializeUsing = DateSerializer.class)
    private Date eventDate;

    public Event(String name, Date eventDate) {
        this.name = name;
        this.eventDate = eventDate;
    }
}

// 自定义日期序列化器：将日期格式化为自定义格式
class DateSerializer implements ObjectWriter<Date> {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void write(JSONWriter jsonWriter, Object date, Object fieldName, Type fieldType, long features) {
        String formattedDate = sdf.format(date);
        jsonWriter.writeString(formattedDate);
    }
}
```

**序列化结果**：

```json
{
    "name": "Conference",
    "eventDate": "2025-03-06 10:00:00"
}
```

---

### **自定义反序列化**

自定义反序列化允许你在对象从 JSON 反序列化时，应用特殊的转换逻辑。通过实现 `ObjectReader` 接口，可以定义如何将 JSON 中的某些字段转换为你想要的对象。

#### 1. **实现 `ObjectReader` 接口自定义反序列化**

**步骤**：

1. 创建自定义的 `ObjectReader`。
2. 使用 `@JSONField(deserializeUsing = CustomDeserializer.class)` 注解指定自定义反序列化器。

**示例：自定义金额反序列化器**

假设你的 JSON 数据包含金额的字符串格式，例如 `"￥100.00"`，你希望将其转换为 `double` 类型。

```java
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;

public class Product {
    private String name;
    @JSONField(deserializeUsing = PriceDeserializer.class)  // 指定自定义反序列化器
    private double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }
}

// 自定义反序列化器：将 "￥100.00" 转换为 double
class PriceDeserializer implements ObjectReader<Double> {
    @Override
    public Double readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        String value = jsonReader.readString();
        return Double.parseDouble(value.replace("￥", ""));
    }
}
```

**反序列化时**：

```java
String json = "{\"name\":\"Laptop\",\"price\":\"￥1999.99\"}";
Product product = JSON.parseObject(json, Product.class);
System.out.println(product.price); // 输出 1999.99
```

---

#### 2. **自定义枚举反序列化**

你也可以自定义枚举类型的反序列化规则。例如，你的 JSON 数据包含 `Processing`、`Shipped` 等字符串，而不是枚举的默认名称。

**示例：自定义枚举反序列化**

```java
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;

public class Order {
    private int id;
    
    @JSONField(deserializeUsing = StatusDeserializer.class)
    private OrderStatus status;

    public Order(int id, OrderStatus status) {
        this.id = id;
        this.status = status;
    }
}

enum OrderStatus {
    PENDING,
    SHIPPED,
    DELIVERED
}

// 自定义反序列化器：将字符串转为枚举
class StatusDeserializer implements ObjectReader<OrderStatus> {
    @Override
    public OrderStatus readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        String status = jsonReader.readString();
        switch (status) {
            case "Processing":
                return OrderStatus.PENDING;
            case "In Transit":
                return OrderStatus.SHIPPED;
            case "Delivered":
                return OrderStatus.DELIVERED;
            default:
                return OrderStatus.PENDING;
        }
    }
}
```

**反序列化时**：

```java
String json = "{\"id\":123,\"status\":\"In Transit\"}";
Order order = JSON.parseObject(json, Order.class);
System.out.println(order.status); // 输出 SHIPPED
```

---

#### 3. **自定义日期反序列化**

如果 JSON 中的日期字段格式与你的 Java 对象不匹配，你可以实现一个自定义的反序列化器来解析它。

**示例：自定义日期反序列化器**

```java
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Event {
    private String name;
    
    @JSONField(deserializeUsing = DateDeserializer.class)
    private Date eventDate;

    public Event(String name, Date eventDate) {
        this.name = name;
        this.eventDate = eventDate;
    }
}

// 自定义日期反序列化器：将 "yyyy-MM-dd HH:mm:ss" 格式的日期字符串解析为 Date
class DateDeserializer implements ObjectReader<Date> {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Date readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        String dateStr = jsonReader.readString();
        try {
            return sdf.parse(dateStr);
        } catch (Exception e) {
            return null;  // 解析失败时返回 null
       

 }
    }
}
```

**反序列化时**：

```java
String json = "{\"name\":\"Conference\",\"eventDate\":\"2025-03-06 10:00:00\"}";
Event event = JSON.parseObject(json, Event.class);
System.out.println(event.eventDate); // 输出：Thu Mar 06 10:00:00 GMT 2025
```

