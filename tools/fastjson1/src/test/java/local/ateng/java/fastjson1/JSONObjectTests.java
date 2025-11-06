package local.ateng.java.fastjson1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.*;
import local.ateng.java.fastjson.entity.UserInfoEntity;
import local.ateng.java.fastjson.init.InitData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class JSONObjectTests {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public JSONObjectTests() {
        InitData initData = new InitData();
        list = initData.getList();
        list2 = initData.getList2();
    }

    /* =======================  反序列化部分 =======================  */

    // 字符串转换为JSONObject
    @Test
    void testDeSerializer1() {
        String str = "{\"id\":12345,\"name\":\"John Doe\",\"age\":25,\"score\":85.5,\"birthday\":\"1997-03-15\",\"province\":\"Example Province\",\"city\":\"Example City\"}";
        JSONObject jsonObject = JSONObject.parseObject(str);
        System.out.println(jsonObject);
        // {"birthday":"1997-03-15","score":85.5,"province":"Example Province","city":"Example City","name":"John Doe","id":12345,"age":25}
    }

    // 字符串转换为java对象
    @Test
    void testDeSerializer2() {
        String str = "{\"id\":null,\"name\":\"John Doe\",\"age\":25,\"score\":0.0,\"birthday\":\"2024-01-18 15:05:10.102\",\"province\":\"\",\"city\":\"Example City\"}";
        UserInfoEntity user = JSONObject.parseObject(str, UserInfoEntity.class);
        System.out.println(user);
        // UserInfoEntity(id=null, name=John Doe, age=25, score=0.0, num=null, birthday=Thu Jan 18 15:05:10 CST 2024, province=, city=Example City, createAt=null, list=null, user=null, children=null)
    }

    // JSONObject转换为java对象
    @Test
    void testDeSerializer3() {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("name", "John Doe")
                .fluentPut("age", 25)
                .fluentPut("birthday", "2024-01-18 15:05:10.102")
                .fluentPut("city", "Example City")
                .fluentPut("province", "Example province")
                .fluentPut("score", 85.5);
        UserInfoEntity user = jsonObject.toJavaObject(UserInfoEntity.class);
        System.out.println(user);
        // UserInfoEntity(id=null, name=John Doe, age=25, score=85.5, num=null, birthday=Thu Jan 18 15:05:10 CST 2024, province=Example province, city=Example City, createAt=null, list=null, user=null, children=null)
    }

    // 容忍松散 JSON 格式
    @Test
    void testDeSerializer4() {
        String str = "{\n" +
                "    // id\n" +
                "    \"id\": 12345,\n" +
                "    name: \"John Doe\", // 姓名\n" +
                "    /*  年龄 */\n" +
                "    \"age\": 25,\n" +
                "    \"score\": 85.5,,\n" +
                "    'birthday': \"1997-03-15\",\n" +
                "    \"province\": \"Example Province\",\n" +
                "    \"city\": \"Example City\",\n" +
                "    other: 1\n" +
                "}";
        JSONObject user = JSONObject.parseObject(
                str,
                Feature.AllowComment,  // 允许 JSON 中出现 // 或 /* */ 注释
                Feature.AllowUnQuotedFieldNames,  // 允许字段名不带引号 {name:"Tom"}
                Feature.AllowSingleQuotes,  // 允许使用单引号包裹字符串 {'name':'Tom'}
                Feature.InternFieldNames,  // 将字段名字符串 intern() 优化内存，解析大量相同字段名时
                Feature.AllowArbitraryCommas,  // 将字段名字符串 intern() 优化内存，解析大量相同字段名时
                Feature.IgnoreNotMatch  // 忽略 JSON 中 Java 类不存在的字段，JSON 多字段时不报错
        );
        System.out.println(user);
        // {"birthday":"1997-03-15","score":85.5,"other":1,"province":"Example Province","city":"Example City","name":"John Doe","id":12345,"age":25}
    }

    // 将小数解析为 BigDecimal（而不是 Double）
    @Test
    void testDeSerializer5() {
        String str1 = "{\"id\":12345,\"name\":\"John Doe\",\"age\":25,\"score\":85.5,\"birthday\":\"1997-03-15\",\"province\":\"Example Province\",\"city\":\"Example City\"}";
        JSONObject jsonObject1 = JSONObject.parseObject(str1);
        System.out.println(jsonObject1);
        System.out.println(jsonObject1.get("score").getClass());
        // {"birthday":"1997-03-15","score":85.5,"province":"Example Province","city":"Example City","name":"John Doe","id":12345,"age":25}
        // class java.math.BigDecimal
        String str2 = "{\"id\":12345,\"name\":\"John Doe\",\"age\":25,\"score\":85.5,\"birthday\":\"1997-03-15\",\"province\":\"Example Province\",\"city\":\"Example City\"}";
        JSONObject jsonObject2 = JSONObject.parseObject(str2, Feature.UseBigDecimal);
        System.out.println(jsonObject2);
        System.out.println(jsonObject2.get("score").getClass());
        // {"birthday":"1997-03-15","score":85.5,"province":"Example Province","city":"Example City","name":"John Doe","id":12345,"age":25}
        // class java.math.BigDecimal
    }

    // 支持 ISO8601 日期格式自动解析 "2025-11-04T13:50:00"
    @Test
    void testDeSerializer6() {
        String str1 = "{\n" +
                "    \"id\": 12345,\n" +
                "    \"name\": \"John Doe\",\n" +
                "    \"age\": 25,\n" +
                "    \"score\": 85.5,\n" +
                "    \"birthday\": \"1997-03-15\",\n" +
                "    \"province\": \"Example Province\",\n" +
                "    \"city\": \"Example City\",\n" +
                "    \"datetime\": \"2025-11-04T13:50:00\",\n" +
                "    \"datetime2\": \"2025-11-04T15:42:08.123+09:00\",\n" +
                "}";
        JSONObject jsonObject1 = JSONObject.parseObject(str1);
        System.out.println(jsonObject1);
        System.out.println(jsonObject1.get("datetime").getClass());
        System.out.println(jsonObject1.get("datetime2").getClass());
        /**
         * {"birthday":"1997-03-15","score":85.5,"datetime":"2025-11-04T13:50:00","province":"Example Province","city":"Example City","name":"John Doe","datetime2":"2025-11-04T15:42:08.123+09:00","id":12345,"age":25}
         * class java.lang.String
         * class java.lang.String
         */
        String str2 = "{\n" +
                "    \"id\": 12345,\n" +
                "    \"name\": \"John Doe\",\n" +
                "    \"age\": 25,\n" +
                "    \"score\": 85.5,\n" +
                "    \"birthday\": \"1997-03-15\",\n" +
                "    \"province\": \"Example Province\",\n" +
                "    \"city\": \"Example City\",\n" +
                "    \"datetime\": \"2025-11-04T13:50:00\",\n" +
                "    \"datetime2\": \"2025-11-04T15:42:08.123+09:00\",\n" +
                "}";
        JSONObject jsonObject2 = JSONObject.parseObject(str2, Feature.AllowISO8601DateFormat);
        System.out.println(jsonObject2);
        System.out.println(jsonObject2.get("datetime").getClass());
        System.out.println(jsonObject2.get("datetime2").getClass());
        /**
         * {"birthday":858355200000,"score":85.5,"datetime":1762235400000,"province":"Example Province","city":"Example City","name":"John Doe","datetime2":1762238528123,"id":12345,"age":25}
         * class java.util.Date
         * class java.util.Date
         */
    }

    /* =======================  序列化部分 =======================  */
    // 这些还可以放在类或者字段上
    // 类：@JSONType(serialzeFeatures = {SerializerFeature.WriteMapNullValue})
    // 字段：@JSONField(serialzeFeatures =  {SerializerFeature.WriteMapNullValue})

    // 输出空字段
    @Test
    void testSerializer1() {
        TestSerializerEntity1 user = new TestSerializerEntity1();
        user.setId(1L);
        // 核心组合：将 null 输出为对应默认值
        String str = JSONObject.toJSONString(user,
                // 输出为 null 的字段，否则默认会被忽略
                SerializerFeature.WriteMapNullValue,
                // String 类型为 null 时输出 ""
                SerializerFeature.WriteNullStringAsEmpty,
                // Number 类型为 null 时输出 0
                SerializerFeature.WriteNullNumberAsZero,
                // Boolean 类型为 null 时输出 false
                SerializerFeature.WriteNullBooleanAsFalse,
                // 集合类型为 null 时输出 []
                SerializerFeature.WriteNullListAsEmpty
        );
        System.out.println(str);
        // {"age":0,"birthday":null,"datetime":null,"entity":null,"id":1,"list":[],"map":null,"name":"","num":0,"score":0,"status":false}
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

        // 格式化输出
        String str = JSONObject.toJSONString(user, SerializerFeature.PrettyFormat);
        System.out.println(str);
        /*
        {
            "age":25,
            "birthday":1762304706882,
            "children":null,
            "city":"Example City",
            "createAt":null,
            "id":null,
            "list":null,
            "name":"John Doe",
            "num":null,
            "province":null,
            "score":85.5,
            "user":null
        }
         */
    }

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

        // 默认情况下（未禁用循环引用检测）
        String json1 = JSONObject.toJSONString(user1);
        System.out.println("默认序列化：");
        System.out.println(json1);

        // 禁用循环引用检测
        String json2 = JSONObject.toJSONString(user1, SerializerFeature.DisableCircularReferenceDetect);
        System.out.println("禁用循环引用检测：");
        System.out.println(json2);
    }

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

        String str = JSONObject.toJSONString(user, SerializerFeature.WriteBigDecimalAsPlain);
        System.out.println(str);
        // 输出：{"age":25,"birthday":1762242201359,"city":"Example City","name":"John Doe","num":100000000000000000000,"score":85.5}
    }

    // 把 Long 类型转为字符串，避免前端精度丢失
    @Test
    void testSerializer44() {
        UserSerializer44 user = new UserSerializer44();
        user.setId(9223372036854775807L);
        user.setNum(new BigDecimal("1E+20"));
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // 输出：{"age":25,"birthday":1762242201359,"city":"Example City","name":"John Doe","num":100000000000000000000,"score":85.5}
    }
    @Data
    public static class UserSerializer44 {
        @JSONField(serializeUsing = ToStringSerializer.class)
        private Long id;
        private BigDecimal num;
    }
    // 也可以通过 SerializerFeature.BrowserCompatible
    @Test
    void testSerializer46() {
        UserSerializer44 user = new UserSerializer44();
        user.setId(9223372036854775807L);
        user.setNum(new BigDecimal("1E+20"));
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // 输出：{"id":"9223372036854775807","num":1E+20}
    }

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

        String str = JSONObject.toJSONString(user, SerializerFeature.BrowserCompatible);
        System.out.println(str);
        // 输出：{"age":25,"birthday":1762311724052,"children":null,"city":"Example City <script>alert('XSS')<\/script>","createAt":null,"id":"9223372036854775807","list":null,"name":"John Doe","num":null,"province":null,"score":85.5,"user":null}
    }

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

        String str = JSONObject.toJSONString(user, SerializerFeature.WriteClassName);
        System.out.println(str);
        // 输出：{"@type":"local.ateng.java.fastjson.entity.UserInfoEntity","age":25,"birthday":1762242155749,"city":"Example City ","name":"John Doe","score":85.5D}
    }

    // 日期时间 格式化
    @Test
    void testSerializer7() {
        UserInfoEntity user = UserInfoEntity.builder()
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province(null)
                .city("Example City")
                .build();

        String str = JSONObject.toJSONString(user, SerializerFeature.UseISO8601DateFormat);
        String str2 = JSONObject.toJSONString(user, SerializerFeature.WriteDateUseDateFormat);
        String str3 = JSONObject.toJSONStringWithDateFormat(user, "yyyy年MM月dd日 HH时mm分ss秒",SerializerFeature.WriteDateUseDateFormat);
        System.out.println(str);
        // {"age":25,"birthday":"2025-11-04T16:10:15.714+08:00","city":"Example City","name":"John Doe","score":85.5}
        System.out.println(str2);
        // {"age":25,"birthday":"2025-11-04 16:10:15","city":"Example City","name":"John Doe","score":85.5}
        System.out.println(str3);
        // {"age":25,"birthday":"2025年11月04日 16时15分11秒","city":"Example City","name":"John Doe","score":85.5}
    }

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

    /* =======================  序列化（过滤器）部分 =======================  */

    /**
     * PropertyFilter: 根据字段名过滤字段
     * PropertyFilter 可以根据字段的名称来决定是否序列化该字段。
     */
    @Test
    void testFilter0() {
        // 序列化
        FilterEntity0 user = new FilterEntity0();
        user.setId(1L);
        user.setName("阿腾");
        user.setAge(25);

        // 使用 PropertyFilter 过滤掉 "age" 字段
        String str = JSONObject.toJSONString(user, Filter0());
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
    public static SerializeFilter Filter0() {
        return (PropertyFilter) (object, name, value) -> {
            // 过滤掉 "age" 字段
            return !"age".equals(name);
        };
    }

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
        String str = JSONObject.toJSONString(user, Filter1());
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
    public static SerializeFilter Filter1() {
        return (ValueFilter) (object, name, value) -> {
            if (value == null) {
                if ("age".equals(name)) {
                    return "未知";  // 如果 age 字段为 null，替换为 "未知"
                }
            }
            return value;
        };
    }

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
        String json = JSONObject.toJSONString(user, defaultValueFilter());
        System.out.println(json);
        // 输出: {"age":18,"id":1,"name":"未知用户"}
    }
    /**
     * 定义 ContextValueFilter：
     * 可通过 context（包含字段元信息）访问注解，实现动态默认值替换
     */
    public static SerializeFilter defaultValueFilter() {
        return (ContextValueFilter) (context, object, name, value) -> {
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
        String str = JSONObject.toJSONString(user, Filter2());
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
    public static SerializeFilter Filter2() {
        return (NameFilter) (object, name, value) -> {
            if ("userName".equals(name)) {
                return "user_name";  // 将 "userName" 字段改为 "user_name"
            }
            return name;
        };
    }


    /* =======================  字段注解部分 =======================  */
    /**
     * 指定默认值，在序列化时生效，反序列化不生效
     */
    @Test
    void testField0() {
        // 序列化
        User0 user = new User0();
        user.setId(1L);
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"age":"25","id":1,"name":"阿腾"}
        // 反序列化
        String str2 = "{\"id\":1}";
        User0 user2 = JSONObject.parseObject(str2, User0.class);
        System.out.println(user2);
        // JSONObjectTests.User0(id=1, name=null, age=null)
    }
    @Data
    public static class User0 {
        private Long id;
        @JSONField(defaultValue = "阿腾")
        private String name;
        @JSONField(defaultValue = "25")
        private Integer age;
    }

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
        // {"birthday":"2025年11月04 16:24:35","dateTime":"2025年11月04日 16时24分35秒"}
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

    /**
     * 小数（如 Double、Float）字段格式化
     * 注意 BigDecimal 不会生效，如果需要使用 SerializeUsing 自定义序列化器
     */
    @Test
    void testField3() {
        // 序列化
        User3 user = new User3();
        user.setScore1(1234.5678);
        user.setScore2(1234.0);
        user.setScore3(0.1267);
        user.setScore4(85.0);
        user.setScore5(9999999.99);
        String str = JSONObject.toJSONString(user);
        System.out.println(str);
        // {"score1":1234.5678,"score2":1234.00,"score3":12.67%,"score4":85,"score5":9,999,999.99}
        // 反序列化
        // 无，格式化后的 12.67% 9,999,999.99 数据无法转换成数字类型
    }
    @Data
    public static class User3 {
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

    /* =======================  自定义序列化器/反序列化器 =======================  */
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
    public static class CurrencySerializer implements ObjectSerializer {
        private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");
        @Override
        public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
            if (object == null) {
                serializer.write("0.00");
                return;
            }
            BigDecimal val = (BigDecimal) object;
            serializer.write("￥" + FORMAT.format(val));
        }
    }
    /** 金额反序列化（去掉￥和,） */
    public static class CurrencyDeserializer implements ObjectDeserializer {
        @Override
        public Object deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            Object val = parser.parse();
            if (val == null) return null;
            String str = val.toString().replace("￥", "").replace(",", "").trim();
            try {
                return new BigDecimal(str);
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
        @Override public int getFastMatchToken() { return 0; }
    }

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
    public static class SensitiveSerializer  implements ObjectSerializer {
        @Override
        public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
            SerializeWriter out = serializer.out;
            if (object == null) {
                out.writeNull();
                return;
            }

            String value = object.toString();

            // 简单示例：手机号脱敏
            if (value.matches("\\d{11}")) {
                value = value.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
            }

            out.writeString(value);

        }
    }


    /* =======================  AutoType部分 =======================  */
    @Test
    void testAutoType1() {
        // 序列化
        AutoType1 entity = new AutoType1(1L, "ateng");
        String str = JSONObject.toJSONString(entity, SerializerFeature.WriteClassName);
        System.out.println(str);
        // {"@type":"local.ateng.java.fastjson1.JSONObjectTests$AutoType1","id":1,"name":"ateng"}
        // 反序列化
        String str2 = "{\"id\":1,\"@type\":\"local.ateng.java.fastjson1.JSONObjectTests$AutoType1\",\"name\":\"ateng\"}";
        AutoType1 entity2 = (AutoType1) JSON.parseObject(str2, Object.class, Feature.SupportAutoType);
        // 开启 AutoType ，可以将有@type字段的JSON字符串直接强转类型
        System.out.println(entity2.getClass());
        // class local.ateng.java.fastjson1.JSONObjectTests$AutoType1
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

    /**
     * 示例2：全局开启 AutoType 支持
     * 通过 ParserConfig 全局配置 setAutoTypeSupport(true)，
     * 可以让所有解析操作都支持 @type 自动反序列化。
     */
    @Test
    void testAutoType2() {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        String json = "{\"@type\":\"local.ateng.java.fastjson1.JSONObjectTests$AutoType1\",\"id\":1,\"name\":\"ateng\"}";
        AutoType1 result = (AutoType1) JSON.parseObject(json, Object.class);
        System.out.println(result.getClass());
        // class local.ateng.java.fastjson1.JSONObjectTests$AutoType1
        System.out.println(result);
        // JSONObjectTests.AutoType1(id=1, name=ateng)
    }

    /**
     * 示例3：禁止特定类（安全防护）
     * 通过 addDeny(prefix) 禁止某些包的类被反序列化。
     */
    @Test
    void testAutoType3() {
        ParserConfig config = ParserConfig.getGlobalInstance();
        config.setAutoTypeSupport(true);
        // 禁止 com.sun.、java.、org.apache. 等类被加载（防止安全漏洞）
        config.addDeny("java.");
        config.addDeny("javax.");
        config.addDeny("com.sun.");
        config.addDeny("sun.");
        config.addDeny("org.apache.");
        config.addDeny("org.springframework.");
        config.addDeny("com.alibaba.");
        config.addDeny("ognl.");
        config.addDeny("bsh.");
        config.addDeny("c3p0.");
        config.addDeny("net.sf.ehcache.");
        config.addDeny("org.yaml.");
        config.addDeny("org.hibernate.");
        config.addDeny("org.jboss.");
        // 添加了后会报错：com.alibaba.fastjson.JSONException: autoType is not support. local.ateng.java.fastjson1.JSONObjectTests$AutoType1
        //config.addDeny("local.ateng.java.");
        String json = "{\"@type\":\"local.ateng.java.fastjson1.JSONObjectTests$AutoType1\",\"id\":1,\"name\":\"ateng\"}";
        AutoType1 result = (AutoType1) JSON.parseObject(json, Object.class);
        System.out.println(result.getClass());
        // class local.ateng.java.fastjson1.JSONObjectTests$AutoType1
        System.out.println(result);
        // JSONObjectTests.AutoType1(id=1, name=ateng)
    }

    /**
     * 示例4：集合类型中使用 AutoType
     * List/Map 中带有 @type 的对象也能正确反序列化。
     */
    @Test
    void testAutoType4() {
        // 序列化
        List<AutoType1> list = new ArrayList<>();
        list.add(new AutoType1(1L, "ateng"));
        list.add(new AutoType1(2L, "blair"));
        String json = JSON.toJSONString(list, SerializerFeature.WriteClassName);
        System.out.println(json);
        // [{"@type":"local.ateng.java.fastjson1.JSONObjectTests$AutoType1","id":1,"name":"ateng"},{"@type":"local.ateng.java.fastjson1.JSONObjectTests$AutoType1","id":2,"name":"blair"}]
        // 反序列化
        String str = "[{\"@type\":\"local.ateng.java.fastjson1.JSONObjectTests$AutoType1\",\"id\":1,\"name\":\"ateng\"},{\"@type\":\"local.ateng.java.fastjson1.JSONObjectTests$AutoType1\",\"id\":2,\"name\":\"blair\"}]";
        List<AutoType1> parsed = (List<AutoType1>) JSON.parseObject(str, Object.class, Feature.SupportAutoType);
        System.out.println(parsed.getClass());
        // 这里是 JSONArray，JSONArray 实现了 java.util.List 接口，强转后也可以正常使用
        // class com.alibaba.fastjson.JSONArray
        System.out.println(parsed.get(0).getClass());
        // class local.ateng.java.fastjson1.JSONObjectTests$AutoType1
        System.out.println(parsed);
        // [{"id":1,"name":"ateng"},{"id":2,"name":"blair"}]
        System.out.println(parsed.get(0).getName());
        // ateng
    }

    /* =======================  其他部分 =======================  */

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


}
