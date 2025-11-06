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

#### 反序列化部分

##### 字符串转换为JSONObject

```java
    // 字符串转换为JSONObject
    @Test
    void testDeSerializer1() {
        String str = "{\"id\":12345,\"name\":\"John Doe\",\"age\":25,\"score\":85.5,\"birthday\":\"1997-03-15\",\"province\":\"Example Province\",\"city\":\"Example City\"}";
        JSONObject jsonObject = JSONObject.parseObject(str);
        System.out.println(jsonObject);
        // {"id":12345,"name":"John Doe","age":25,"score":85.5,"birthday":"1997-03-15","province":"Example Province","city":"Example City"}
    }
```

##### 字符串转换为java对象

```java
    // 字符串转换为java对象
    @Test
    void testDeSerializer2() {
        String str = "{\"id\":null,\"name\":\"John Doe\",\"age\":25,\"score\":0.0,\"birthday\":\"2024-01-18 15:05:10.102\",\"province\":\"\",\"city\":\"Example City\"}";
        UserInfoEntity user = JSONObject.parseObject(str, UserInfoEntity.class);
        System.out.println(user);
        // UserInfoEntity(id=null, name=John Doe, age=25, score=0.0, birthday=Thu Jan 18 15:05:10 CST 2024, province=, city=Example City, createAt=null)
    }
```

##### JSONObject转换为java对象

```java
    // JSONObject转换为java对象
    @Test
    void testDeSerializer3() {
        JSONObject jsonObject = JSONObject.of(
                        "name", "John Doe",
                        "age", 25,
                        "birthday", "2024-01-18 15:05:10.102",
                        "city", "Example City",
                        "province", "Example province",
                        "score", 85.5
                );
        UserInfoEntity user = jsonObject.toJavaObject(UserInfoEntity.class);
        System.out.println(user);
        // UserInfoEntity(id=null, name=John Doe, age=25, score=85.5, num=null, birthday=Thu Jan 18 15:05:10 CST 2024, province=Example province, city=Example City, createAt=null, list=null, user=null, children=null)
    }
```

##### 容忍松散 JSON 格式

```java
    // 容忍松散 JSON 格式
    @Test
    void testDeSerializer4() {
        String str = "{\n" +
                "    // id\n" +
                "    \"id\": 12345,\n" +
                "    name: \"John Doe\", // 姓名\n" +
                "    /*  年龄 */\n" +
                "    \"age\": 25,\n" +
                "    \"score\": 85.5,\n" +
                "    'birthday': \"1997-03-15\",\n" +
                "    \"province\": \"Example Province\",\n" +
                "    \"city\": \"Example City\",\n" +
                "    other: 1\n" +
                "}";
        JSONObject user = JSONObject.parse(
                str,
                JSONReader.Feature.SupportSmartMatch,         // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                JSONReader.Feature.AllowUnQuotedFieldNames,   // 允许字段名不带引号
                JSONReader.Feature.IgnoreNoneSerializable,    // 忽略无法序列化的字段
                JSONReader.Feature.IgnoreAutoTypeNotMatch     // 防止类型不匹配时报错（更安全）
        );
        System.out.println(user);
        // {"birthday":"1997-03-15","score":85.5,"other":1,"province":"Example Province","city":"Example City","name":"John Doe","id":12345,"age":25}
    }
```

#### 序列化部分

这些还可以放在类或者字段上
类：@JSONType(serializeFeatures = {JSONWriter.Feature.WriteNulls})
字段：@JSONField(serialzeFeatures =  {SerializerFeature.WriteMapNullValue})

##### 输出空字段

```java
    // 输出空字段
    @Test
    void testSerializer1() {
        TestSerializerEntity1 user = new TestSerializerEntity1();
        user.setId(1L);
        String str = JSONObject.toJSONString(user,
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls
        );
        System.out.println(str);
        // {"age":null,"birthday":null,"datetime":null,"entity":null,"id":1,"list":null,"map":null,"name":null,"num":null,"score":null,"status":null}
    }
    @Data
    public static class TestSerializerEntity1 {
        private Long id;
        private String name;
        private Integer age;
        private Double score;
        private BigDecimal num;
        private Boolean status;
        private Date birthday;
        private LocalDateTime datetime;
        private List<String> list;
        private TestSerializerEntity1 entity;
        private Map<String, String> map;
    }
```

##### 输出缺省值

```java
    // 输出缺省值
    @Test
    void testSerializer12() {
        TestSerializerEntity1 user = new TestSerializerEntity1();
        user.setId(1L);
        String str = JSONObject.toJSONString(user,
                // 将空置输出为缺省值，Number类型的null都输出为0，String类型的null输出为""，数组和Collection类型的输出为[]
                JSONWriter.Feature.NullAsDefaultValue
        );
        System.out.println(str);
        // {"age":0,"birthday":null,"id":1,"list":[],"name":"","num":0,"score":0,"status":false}
    }
```

##### 格式化

```java
    // 格式化
    @Test
    void testSerializer2() {
        UserInfoEntity user = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province(null)
                .city("Example City")
                .build();

        // JSONWriter.Feature.PrettyFormat -> 格式化输出
        String str = JSONObject.toJSONString(user, JSONWriter.Feature.PrettyFormat);
        System.out.println(str);
        /*
        {
            "age":25,
            "birthday":"2025-11-05 17:25:58.053",
            "city":"Example City",
            "name":"John Doe",
            "score":85.5
        }
         */
    }
```

##### 防止循环引用导致 "$ref"

```java
    // 防止循环引用导致 "$ref"
    @Test
    void testSerializer3() {
        // 创建两个相互引用的对象
        UserInfoEntity user1 = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province(null)
                .city("Example City")
                .build();
        UserInfoEntity user2 = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province(null)
                .city("Example City")
                .build();

        // 相互引用，形成循环
        user1.setUser(user2);
        user2.setUser(user1);

        // 打开循环引用检测
        String json2 = JSONObject.toJSONString(user1, JSONWriter.Feature.ReferenceDetection);
        System.out.println("打开循环引用检测：");
        System.out.println(json2);
        // {"age":25,"birthday":"2025-11-05 17:27:45.362","city":"Example City","name":"John Doe","score":85.5,"user":{"age":25,"birthday":"2025-11-05 17:27:45.362","city":"Example City","name":"John Doe","score":85.5,"user":{"$ref":"$"}}}

        // 默认情况下（关闭循环引用检测）
        String json1 = JSONObject.toJSONString(user1);
        System.out.println("默认序列化：");
        System.out.println(json1);

    }
```

##### 保证 BigDecimal 精度

```java
    // 保证 BigDecimal 精度（金融系统常用）
    @Test
    void testSerializer4() {
        UserInfoEntity user = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province(null)
                .num(new BigDecimal("1E+20"))
                .city("Example City")
                .build();

        String str = JSONObject.toJSONString(user, JSONWriter.Feature.WriteBigDecimalAsPlain);
        System.out.println(str);
        // 输出：{"age":25,"birthday":"2025-11-05 17:29:39.404","city":"Example City","name":"John Doe","num":100000000000000000000,"score":85.5}
    }
```

##### 把 Long 类型转为字符串

```java
    // 把 Long 类型转为字符串，避免前端精度丢失
    @Test
    void testSerializer44() {
        UserSerializer44 user = new UserSerializer44();
        user.setId(9223372036854775807L);
        user.setNum(new BigDecimal("1E+20"));
        String str = JSONObject.toJSONString(user, JSONWriter.Feature.WriteLongAsString);
        System.out.println(str);
        // 输出：{"id":"9223372036854775807","num":1E+20}
    }
    @Data
    public static class UserSerializer44 {
        private Long id;
        private BigDecimal num;
    }
```

##### 浏览器安全输出（防止前端注入）

```java
    // 浏览器安全输出（防止前端注入）
    @Test
    void testSerializer5() {
        UserInfoEntity user = UserInfoEntity.builder()
                .id(9223372036854775807L)
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province(null)
                .city("Example City <script>alert('XSS')</script>")
                .build();

        String str = JSONObject.toJSONString(user, JSONWriter.Feature.BrowserCompatible, JSONWriter.Feature.BrowserSecure);
        System.out.println(str);
        // 输出：{"age":25,"birthday":"2025-11-05 17:36:45.81","city":"Example City \u003Cscript\u003Ealert\u0028'XSS'\u0029\u003C/script\u003E","id":"9223372036854775807","name":"John Doe","score":85.5}
    }
```

##### 输出类型信息（反序列化时保留类型）

```java
    // 输出类型信息（反序列化时保留类型）
    @Test
    void testSerializer6() {
        UserInfoEntity user = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province(null)
                .city("Example City ")
                .build();

        String str = JSONObject.toJSONString(user, JSONWriter.Feature.WriteClassName);
        System.out.println(str);
        // 输出：{"@type":"local.ateng.java.fastjson2.entity.UserInfoEntity","age":25,"birthday":"2025-11-05 17:37:54.721","city":"Example City ","name":"John Doe","score":85.5}
    }
```

##### 枚举字段 序列化和反序列化

```java
   /**
     * 枚举字段 序列化和反序列化
     */
    @Test
    void testEnum() {
        // 序列化
        userEnumEntity user = new userEnumEntity();
        user.setId(1L);
        user.setUserEnum(UserEnum.ENABLE);
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"id":1,"userEnum":"开启"}
        // 反序列化
        String str2 = "{\"id\":1,\"userEnum\":1}";
        userEnumEntity user2 = JSONObject.parseObject(str2, userEnumEntity.class);
        System.out.println(user2);
        // JSONObjectTests.userEnumEntity(id=1, userEnum=ENABLE)
    }
    @Data
    public static class userEnumEntity {
        private Long id;
        private UserEnum userEnum;
    }
    public enum UserEnum {
        ENABLE(1, "开启"),
        DISABLE(2, "关闭");
        private Integer code;
        private String name;
        UserEnum(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public Integer getCode() {
            return code;
        }

        @JSONField
        public String getName() {
            return this.name;
        }

        @JSONCreator
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
```

#### 字段注解部分

##### 指定默认值，在序列化时生效，反序列化不生效

```java
    /**
     * 指定默认值，在反序列化时生效，序列化不生效
     */
    @Test
    void testField0() {
        // 序列化
        User0 user = new User0();
        user.setId(1L);
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"id":1}
        // 反序列化
        String str2 = "{\"id\":1}";
        User0 user2 = JSONObject.parseObject(str2, User0.class);
        System.out.println(user2);
        // JSONObjectTests.User0(id=1, name=阿腾, age=25)
    }
    @Data
    public static class User0 {
        private Long id;
        @JSONField(defaultValue = "阿腾")
        private String name;
        @JSONField(defaultValue = "25")
        private Integer age;
    }
```

##### 禁用序列化/反序列化字段

```java
    /**
     * 禁用序列化/反序列化字段
     */
    @Test
    void testField00() {
        // 序列化
        User00 user = new User00();
        user.setId(1L);
        user.setName("阿腾");
        user.setAge(25);
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"id":1,"name":"阿腾"}
        // 反序列化
        String str2 = "{\"age\":\"25\",\"id\":1,\"name\":\"阿腾\"}";
        User00 user2 = JSONObject.parseObject(str2, User00.class);
        System.out.println(user2);
        // JSONObjectTests.User00(id=1, name=阿腾, age=null)
    }
    @Data
    public static class User00 {
        private Long id;
        private String name;
        @JSONField(serialize = false, deserialize = false)
        private Integer age;
    }
```

##### 指定 JSON 字段名

```java
    /**
     * 指定 JSON 字段名
     * 🔹 序列化结果字段名变为 "user_name"
     * 🔹 反序列化时会自动匹配 "user_name"
     */
    @Test
    void testField1() {
        // 序列化
        User1 user = new User1();
        user.setName("John Doe");
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"user_name":"John Doe"}
        // 反序列化
        String str2 = "{\"user_name\":\"John Doe\"}";
        User1 user2 = JSONObject.parseObject(str2, User1.class);
        System.out.println(user2);
        // JSONObjectTests.User1(name=John Doe)
    }
    @Data
    public static class User1 {
        @JSONField(name = "user_name")
        private String name;
    }
```

##### 日期格式化

```java
    /**
     * 日期格式化
     * 🔹 控制日期序列化输出格式
     * 🔹 反序列化时也会自动识别同格式字符串
     * 🔹 优先级高于 JSON.DEFFAULT_DATE_FORMAT
     */
    @Test
    void testField2() {
        // 序列化
        User2 user = new User2();
        user.setBirthday(new Date());
        user.setDateTime(LocalDateTime.now());
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"birthday":"2025年11月06 09:14:43","dateTime":"2025年11月06日 09时14分43秒"}
        // 反序列化
        String str2 = "{\"birthday\":\"2025年11月04 16:24:35\",\"dateTime\":\"2025年11月04日 16时24分35秒\"}";
        User2 user2 = JSONObject.parseObject(str2, User2.class);
        System.out.println(user2);
        // JSONObjectTests.User2(birthday=Tue Nov 04 16:24:35 CST 2025, dateTime=2025-11-04T16:24:35)
    }
    @Data
    public static class User2 {
        @JSONField(format = "yyyy年MM月dd HH:mm:ss")
        private Date birthday;
        @JSONField(format = "yyyy年MM月dd日 HH时mm分ss秒")
        private LocalDateTime dateTime;
    }
```

##### 小数（如 BigDecimal、Double、Float）字段格式化

```java
    /**
     * 小数（如 BigDecimal、Double、Float）字段格式化
     */
    @Test
    void testField3() {
        // 序列化
        User3 user = new User3();
        user.setScore(new BigDecimal("1234.5678"));
        user.setScore1(1234.5678);
        user.setScore2(1234.0);
        user.setScore3(0.1267);
        user.setScore4(85.0);
        user.setScore5(9999999.99);
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"score":1234.6,"score1":1234.5678,"score2":1234.00,"score3":12.67%,"score4":85,"score5":9,999,999.99}
        // 反序列化
        // 无，格式化后的 12.67% 9,999,999.99 数据无法转换成数字类型
    }
    @Data
    public static class User3 {

        /** 保留两位小数（常用于金额、分数等） */
        @JSONField(format = "#0.0")
        private BigDecimal score;

        /** 默认输出，不指定 format */
        private Double score1;

        /** 保留两位小数（常用于金额、分数等） */
        @JSONField(format = "#0.00")
        private Double score2;

        /** 百分比格式（乘100并附加%符号） */
        @JSONField(format = "0.##%")
        private Double score3;

        /** 没有小数（取整显示） */
        @JSONField(format = "#")
        private Double score4;

        /** 千分位 + 两位小数（财务场景常用） */
        @JSONField(format = "#,##0.00")
        private Double score5;
    }

```

##### 字段排序

**字段注解排序**

```java
    // 字段排序
    @Test
    void testField4() {
        UserField4 user = new UserField4();
        user.setId1(9223372036854775807L);
        user.setId2(9223372036854775807L);
        user.setId3(9223372036854775807L);
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // 输出：{"id3":9223372036854775807,"id2":9223372036854775807,"id1":9223372036854775807}
    }
    @Data
    public static class UserField4 {
        @JSONField(ordinal = 3)
        private Long id1;
        @JSONField(ordinal = 2)
        private Long id2;
        @JSONField(ordinal = 1)
        private Long id3;
    }
```

**类注解排序**

```java
    @Test
    void testField5() {
        UserField5 user = new UserField5();
        user.setId1(9223372036854775807L);
        user.setId2(9223372036854775807L);
        user.setId3(9223372036854775807L);
        user.setId4(9223372036854775807L);
        user.setId5(9223372036854775807L);
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // 输出：{"id5":9223372036854775807,"id3":9223372036854775807,"id1":9223372036854775807,"id2":9223372036854775807,"id4":9223372036854775807}
    }
    @Data
    @JSONType(orders = {"id5", "id3", "id1"})
    public static class UserField5 {
        private Long id1;
        private Long id2;
        private Long id3;
        private Long id4;
        private Long id5;
    }
```



#### 自定义序列化器/反序列化器

##### 数值格式化

```java
    /**
     * 数值格式化
     */
    @Test
    void testCustom1() {
        // 序列化
        Custom1 user = new Custom1();
        user.setScore1(66.666);
        user.setScore2(new BigDecimal("12345.67890"));
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"score1":66.67,"score2":"￥12,345.68"}
        // 反序列化
        String str2 = "{\"score1\":66.67,\"score2\":\"￥12,345.68\"}";
        Custom1 user2 = JSONObject.parseObject(str2, Custom1.class);
        System.out.println(user2);
        // JSONObjectTests.Custom1(score1=66.67, score2=12345.68)
    }
    @Data
    public static class Custom1 {
        /** 保留两位小数（常用于金额、分数等） */
        @JSONField(format = "#0.00")
        private Double score1;
        /** 保留两位小数（常用于金额、分数等） */
        @JSONField(serializeUsing =  CurrencySerializer.class, deserializeUsing = CurrencyDeserializer.class)
        private BigDecimal score2;
    }
    /** 金额格式化：#,##0.00 带千分位 */
    public static class CurrencySerializer implements ObjectWriter<BigDecimal> {
        private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");
        @Override
        public void write(JSONWriter jsonWriter, Object value, Object fieldName, Type fieldType, long features) {
            // 检查是否为 null
            if (value == null) {
                jsonWriter.writeString("￥0.00");
                return;
            }
            // 如果不是 BigDecimal 类型，直接返回 "￥0.00" 作为默认值
            if (!(value instanceof BigDecimal)) {
                jsonWriter.writeString("￥0.00");
                return;
            }
            // 类型转换安全地进行
            BigDecimal val = (BigDecimal) value;
            // 格式化并写入数据
            jsonWriter.writeString("￥" + FORMAT.format(val));  // 写入格式化后的字符串
        }
    }
    /** 金额反序列化（去掉￥和,） */
    public static class CurrencyDeserializer implements ObjectReader<BigDecimal> {
        @Override
        public BigDecimal readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            String val = jsonReader.readString();
            if (val == null) return BigDecimal.ZERO;
            String cleaned = val.replace("￥", "").replace(",", "").trim();
            try {
                return new BigDecimal(cleaned);
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
    }
```

##### 数据脱敏

```java
    /**
     * 数据脱敏
     */
    @Test
    void testCustom2() {
        // 序列化
        Custom2 user = new Custom2();
        user.setId(1L);
        user.setMobile("17623062936");
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"id":1,"mobile":"176****2936"}
        // 反序列化
        // 无
    }
    @Data
    public static class Custom2 {
        private Long id ;
        /** 手机号脱敏 */
        @JSONField(serializeUsing =  SensitiveSerializer.class)
        private String mobile;
    }
    /** 脱敏序列化器 */
    public static class SensitiveSerializer implements ObjectWriter<String> {
        @Override
        public void write(JSONWriter jsonWriter, Object value, Object fieldName, Type fieldType, long features) {
            if (value == null) {
                jsonWriter.writeNull();
                return;
            }
            String strValue = value.toString();
            // 简单示例：手机号脱敏
            if (strValue.matches("\\d{11}")) {
                strValue = strValue.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
            }
            jsonWriter.writeString(strValue);
        }
    }
```

#### 序列化过滤器

##### 使用 `PropertyPreFilter` 过滤字段

`PropertyPreFilter` 可以根据字段的名称来决定是否序列化该字段。

```java
    /**
     * PropertyPreFilter: 根据字段名过滤字段
     * PropertyPreFilter 可以根据字段的名称来决定是否序列化该字段。
     */
    @Test
    void testFilter0() {
        // 序列化
        FilterEntity0 user = new FilterEntity0();
        user.setId(1L);
        user.setName("阿腾");
        user.setAge(25);

        // 使用 PropertyFilter 过滤掉 "age" 字段
        String str = JSON.toJSONString(user, Filter0());
        System.out.println(str);
        // 输出: {"id":1,"name":"阿腾"}
    }
    // 实体类定义
    @Data
    public static class FilterEntity0 {
        private Long id;
        private String name;
        private Integer age;
    }
    // 定义一个 PropertyFilter，过滤掉 "age" 字段
    public static PropertyPreFilter Filter0() {
        return (writer, source, name) -> !"age".equals(name);
    }
```

##### 使用 **`ValueFilter`** 修改字段的值

`ValueFilter` 可以在序列化时修改字段的值。例如，将 `null` 值替换为其他值，或者对某个字段进行格式化。

```java
    /**
     * ValueFilter: 修改字段的值
     * ValueFilter 可以在序列化时修改字段的值。例如，将 null 值替换为其他值，或者对某个字段进行格式化。
     */
    @Test
    void testFilter1() {
        // 序列化
        FilterEntity1 user = new FilterEntity1();
        user.setId(1L);
        user.setName("阿腾");
        user.setAge(null);  // Age 为 null

        // 使用 ValueFilter 将 null 值替换为默认值
        // JSONWriter.Feature.WriteNulls 特性需要加上，否则为null的值就不会走Filter
        String str = JSON.toJSONString(user, Filter1(), JSONWriter.Feature.WriteNulls);
        System.out.println(str);
        // 输出: {"age":"未知","id":1,"name":"阿腾"}
    }
    // 实体类定义
    @Data
    public static class FilterEntity1 {
        private Long id;
        private String name;
        private Integer age;
    }
    // 定义一个 ValueFilter，修改字段的值
    public static ValueFilter Filter1() {
        return (object, name, value) -> {
            if (value == null) {
                if ("age".equals(name)) {
                    return "未知";  // 如果 age 字段为 null，替换为 "未知"
                }
            }
            return value;
        };
    }
```

##### 使用 `ContextValueFilter` 修改字段

ContextValueFilter: 基于上下文修改字段的值

ContextValueFilter 与 ValueFilter 类似，但它能通过 context 获取序列化上下文信息，

如字段的 Field 对象，从而访问注解或类型信息，实现更灵活的序列化控制。

```java
    /**
     * ContextValueFilter: 基于上下文修改字段的值
     * ContextValueFilter 与 ValueFilter 类似，但它能通过 context 获取序列化上下文信息，
     * 如字段的 Field 对象，从而访问注解或类型信息，实现更灵活的序列化控制。
     *
     * 下面的示例通过自定义 @DefaultValue 注解，
     * 在序列化时如果字段值为 null，就自动替换为注解中指定的默认值。
     */
    @Test
    void testFilter3() {
        // 序列化
        User user = new User();
        user.setId(1L);
        user.setName(null); // 没有设置名称
        user.setAge(null);  // 没有设置年龄

        // 使用 ContextValueFilter 序列化
        // JSONWriter.Feature.WriteNulls 特性需要加上，否则为null的值就不会走Filter
        String json = JSON.toJSONString(user, defaultValueFilter(), JSONWriter.Feature.WriteNulls);
        System.out.println(json);
        // 输出: {"age":18,"id":1,"name":"未知用户"}
    }
    /**
     * 定义 ContextValueFilter：
     * 可通过 context（包含字段元信息）访问注解，实现动态默认值替换
     */
    public static ContextValueFilter defaultValueFilter() {
        return (context, object, name, value) -> {
            if (value == null) {
                try {
                    // 从当前字段读取 @DefaultValue 注解
                    DefaultValue annotation = context.getField().getAnnotation(DefaultValue.class);
                    if (annotation != null) {
                        return annotation.value(); // 使用注解中的默认值
                    }
                } catch (Exception ignored) {
                }
            }
            return value;
        };
    }
    // 自定义注解，用于定义默认值
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultValue {
        String value();
    }
    @Data
    public static class User {
        private Long id;
        @DefaultValue("未知用户")
        private String name;
        @DefaultValue("18")
        private Integer age;
    }

```

##### 使用 `NameFilter` 修改字段名

`NameFilter` 可以在序列化时修改字段的名称（例如，改变驼峰命名风格为下划线命名风格）。

```java
    /**
     * NameFilter: 修改字段的名称
     * NameFilter 可以在序列化时修改字段的名称（例如，改变驼峰命名风格为下划线命名风格）。
     */
    @Test
    void testFilter2() {
        // 序列化
        FilterEntity2 user = new FilterEntity2();
        user.setId(1L);
        user.setUserName("阿腾");
        user.setAge(25);

        // 使用 NameFilter 将字段名从 "userName" 改为 "user_name"
        String str = JSON.toJSONString(user, Filter2());
        System.out.println(str);
        // 输出: {"age":25,"id":1,"user_name":"阿腾"}
    }
    // 实体类定义
    @Data
    public static class FilterEntity2 {
        private Long id;
        private String userName;
        private Integer age;
    }
    // 定义一个 NameFilter，修改字段名
    public static NameFilter Filter2() {
        return (object, name, value) -> {
            if ("userName".equals(name)) {
                return "user_name";  // 将 "userName" 字段改为 "user_name"
            }
            return name;
        };
    }
```

#### AutoType

##### 基本使用

```java
    /**
     * 示例1：基本使用
     */
    @Test
    void testAutoType1() {
        // 序列化
        AutoType1 entity = new AutoType1(1L, "ateng");
        String str = JSONObject.toJSONString(entity, JSONWriter.Feature.WriteClassName);
        System.out.println(str);
        // {"@type":"local.ateng.java.fastjson2.JSONObjectTests$AutoType1","id":1L,"name":"ateng"}
        // 反序列化
        /*
        注意开启了AutoType，反序列化时@type字段需要在首位，否则反序列化会报错：
        java.lang.ClassCastException: class com.alibaba.fastjson2.JSONObject cannot be cast to
        class local.ateng.java.fastjson2.JSONObjectTests$AutoType1 (com.alibaba.fastjson2.JSONObject
        and local.ateng.java.fastjson2.JSONObjectTests$AutoType1 are in unnamed module of loader 'app')
         */
        String str2 = "{\"id\":1L,\"@type\":\"local.ateng.java.fastjson2.JSONObjectTests$AutoType1\",\"name\":\"ateng\"}";
        AutoType1 entity2 = (AutoType1) JSON.parseObject(str2, Object.class, JSONReader.Feature.SupportAutoType);
        // 开启 AutoType ，可以将有@type字段的JSON字符串直接强转类型
        System.out.println(entity2.getClass());
        // class local.ateng.java.fastjson2.JSONObjectTests$AutoType1
        System.out.println(entity2);
        // JSONObjectTests.AutoType1(id=1, name=ateng)
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AutoType1 {
        private Long id;
        private String name;
    }
```

##### 运行特定类（安全防护）

```java
    /**
     * 示例2：运行特定类（安全防护）
     */
    @Test
    void testAutoType2() {
        String json = "{\"@type\":\"local.ateng.java.fastjson2.JSONObjectTests$AutoType1\",\"id\":1,\"name\":\"ateng\"}";
        JSONReader.AutoTypeBeforeHandler autoedTypeFilter = JSONReader.autoTypeFilter(
                // 按需加上需要支持自动类型的类名前缀，范围越小越安全
                "local.ateng.java."
        );
        /*
        如果带有 @type 字段的数据反序列化时，其类型不在 autoedTypeFilter 范围内会抛出异常
        java.lang.ClassCastException: class com.alibaba.fastjson2.JSONObject cannot be cast to class 
        local.ateng.java.fastjson2.JSONObjectTests$AutoType1 (com.alibaba.fastjson2.JSONObject and 
        local.ateng.java.fastjson2.JSONObjectTests$AutoType1 are in unnamed module of loader 'app')
         */
        AutoType1 result = (AutoType1) JSON.parseObject(json, Object.class, autoedTypeFilter);
        System.out.println(result.getClass());
        // class local.ateng.java.fastjson2.JSONObjectTests$AutoType1
        System.out.println(result);
        // JSONObjectTests.AutoType1(id=1, name=ateng)
    }
```

##### 集合类型中使用 AutoType

```java
    /**
     * 示例3：集合类型中使用 AutoType
     * List/Map 中带有 @type 的对象也能正确反序列化。
     */
    @Test
    void testAutoType3() {
        // 序列化
        List<AutoType1> list = new ArrayList<>();
        list.add(new AutoType1(1L, "ateng"));
        list.add(new AutoType1(2L, "blair"));
        String json = JSON.toJSONString(list, JSONWriter.Feature.WriteClassName);
        System.out.println(json);
        // [{"@type":"local.ateng.java.fastjson2.JSONObjectTests$AutoType1","id":1,"name":"ateng"},{"@type":"local.ateng.java.fastjson2.JSONObjectTests$AutoType1","id":2,"name":"blair"}]
        // 反序列化
        JSONReader.AutoTypeBeforeHandler autoedTypeFilter = JSONReader.autoTypeFilter(
                // 按需加上需要支持自动类型的类名前缀，范围越小越安全
                "local.ateng.java."
        );
        String str = "[{\"@type\":\"local.ateng.java.fastjson2.JSONObjectTests$AutoType1\",\"id\":1,\"name\":\"ateng\"},{\"@type\":\"local.ateng.java.fastjson2.JSONObjectTests$AutoType1\",\"id\":2,\"name\":\"blair\"}]";
        List<AutoType1> parsed = (List<AutoType1>) JSON.parseObject(str, Object.class, autoedTypeFilter);
        System.out.println(parsed.getClass());
        // 这里是 JSONArray，JSONArray 实现了 java.util.List 接口，强转后也可以正常使用
        // class com.alibaba.fastjson.JSONArray
        System.out.println(parsed.get(0).getClass());
        // class local.ateng.java.fastjson2.JSONObjectTests$AutoType1
        System.out.println(parsed);
        // [{"id":1,"name":"ateng"},{"id":2,"name":"blair"}]
        System.out.println(parsed.get(0).getName());
        // ateng
    }
```



#### 其他部分

##### java对象转JSONObject

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
        JSONObject jsonObject = JSONObject.parseObject(str);
        System.out.println(jsonObject);
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
        JSONArray stringList = (JSONArray) JSONPath.eval(list, "$[*].name");
        System.out.println(stringList);
        // ["段哲瀚","汪昊然","万修杰", ... ]
    }
```

#### 读取集合多个元素的某个属性

```java
    // 读取集合多个元素的某个属性
    @Test
    void test01_2() {
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        JSONArray names = (JSONArray) JSONPath.eval(json, "$.students[*].name");
        System.out.println(names);
        // ["John","Alice"]
    }
    @Test
    void test01_2_1() {
        String json = "{\"张三\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        JSONArray names = (JSONArray) JSONPath.eval(json, "$.张三[*].name");
        System.out.println(names);
        // ["John","Alice"]
    }
    @Test
    void test01_3() {
        // 递归查询
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        JSONArray names = (JSONArray) JSONPath.eval(json, "$.students..name");
        System.out.println(names);
        // ["John","Alice"]
    }
    @Test
    void test01_04() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        JSONArray result = (JSONArray) JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')].name");
        System.out.println(result);
        // ["John","Bob"]
    }
    @Test
    void test01_05() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取1条件筛选的结果
        JSONObject result = (JSONObject) JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')][0]");
        System.out.println(result);
        // {"name":"John","age":25,"gender":"Male"}
    }
    @Test
    void test01_06() {
        String jsonString = "[{\"name\":\"高新区管委会\",\"congestionIndex\":1.2,\"realSpeed\":\"60.918\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-B区\",\"congestionIndex\":1.2,\"realSpeed\":\"39.3355\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"康居西城\",\"congestionIndex\":1.3,\"realSpeed\":\"29.8503\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"白市驿\",\"congestionIndex\":1.1,\"realSpeed\":\"45.5646\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"金凤\",\"congestionIndex\":1.2,\"realSpeed\":\"44.2227\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-A区\",\"congestionIndex\":1.4,\"realSpeed\":\"30.8192\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"高新天街\",\"congestionIndex\":1.4,\"realSpeed\":\"19.2326\",\"yoy\":0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"熙街\",\"congestionIndex\":1.6,\"realSpeed\":\"23.2695\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"缓行\"}]";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        JSONArray congestionIndexList1 = (JSONArray) JSONPath.eval(jsonString, "$[?(@.name == '高新区管委会')].congestionIndex");
        System.out.println(congestionIndexList1);
        // [1.2]
        System.out.println(congestionIndexList1.getFirst().getClass());
        // class java.math.BigDecimal
    }
```

#### 返回集合中多个元素

```java
    // 返回集合中多个元素
    @Test
    void test02() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "[1,5]"); // 返回下标为1和5的元素
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=2, name=宋鹏, age=86, score=48.951, num=null, bi ...
    }
```

#### 按范围返回集合的子集

```java
    // 按范围返回集合的子集
    @Test
    void test03() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "[1:5]"); // 返回下标从1到5的元素
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=2, name=林浩, age=95, score=69.378, num=null, ...
    }
```

#### 通过条件过滤，返回集合的子集

```java
    // 通过条件过滤，返回集合的子集
    @Test
    void test04() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list,"$[?(@.id in (88,99))]"); // 返回下标从1到5的元素
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=88, name=方越彬, age=63, score=63.994, num=null,
    }
    @Test
    void test04_2() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age = 88)]"); // 返回列表对象的age=88的数据
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=28, name=邵浩然, age=88, score=24.06, num=null, birthday=Tue Jun ...
    }
```

#### 多条件筛选，返回集合的子集

```java
    // 多条件筛选，返回集合的子集
    @Test
    void test04_3() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age == 88 || @.age == 95)]"); // 返回列表对象的age=88或者=95的数据
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=11, name=朱烨磊, age=88, score=66.78, ...
    }
    @Test
    void test04_4() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age > 50 && @.age < 95 && @.city='东莞')]");
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=64, name=崔志泽, age=70, score=64.082, num=null ...
    }
```

#### 通过条件过滤，获取数组长度

```java
    // 通过条件过滤，获取数组长度
    @Test
    void test04_5() {
        Integer length = (Integer) JSONPath.eval(list, "$[?(@.age = 88)].length()"); // 返回列表对象的age=88的数据的长度
        System.out.println(length);
    }
```



### 全局配置

在 Springboot 中配置了Fastjson2 的全局配置，那么在使用 JSONObject、JSONArray、JSON 这些时就会生效

```java
package local.ateng.java.fastjson2.config;

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
 * @since 2025-11-06
 */
@Configuration
public class FastJsonConfig {

    @EventListener
    public void run(ApplicationReadyEvent event) {
        JSON.configReaderDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        JSON.configWriterDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        JSON.config(
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 将空置输出为缺省值，Number类型的null都输出为0，String类型的null输出为""，数组和Collection类型的输出为[]
                JSONWriter.Feature.NullAsDefaultValue,
                // 打开循环引用检测
                JSONWriter.Feature.ReferenceDetection,
                // 保证 BigDecimal 精度
                JSONWriter.Feature.WriteBigDecimalAsPlain,
                // 把 Long 类型转为字符串，避免前端精度丢失
                JSONWriter.Feature.WriteLongAsString,
                // 浏览器安全输出（防止前端注入）
                JSONWriter.Feature.BrowserCompatible,
                JSONWriter.Feature.BrowserSecure
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

