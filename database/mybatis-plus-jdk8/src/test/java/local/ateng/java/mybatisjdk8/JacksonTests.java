package local.ateng.java.mybatisjdk8;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import local.ateng.java.mybatisjdk8.entity.MyData;
import local.ateng.java.mybatisjdk8.entity.Project;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JacksonTests {
    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, // 允许所有子类型的验证器（最宽松）
                ObjectMapper.DefaultTyping.NON_FINAL,  // 仅对非 final 类启用类型信息
                JsonTypeInfo.As.PROPERTY                // 以 JSON 属性的形式存储类型信息
        );
        /*objectMapper.setDefaultTyping(new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL,
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("local.ateng.java.")
                        .build()) {
            {
                init(JsonTypeInfo.Id.CLASS, null);
                inclusion(JsonTypeInfo.As.PROPERTY);
                // 自定义类型字段名
                typeProperty("@type");
            }
        });*/

        // Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime 序列化 & 反序列化
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));

        // LocalDate 序列化 & 反序列化
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // 注册 JavaTimeModule
        objectMapper.registerModule(javaTimeModule);

    }

    @Test
    public void test1() throws JsonProcessingException {
        MyData myData = new MyData();
        myData.setId(1L);
        myData.setName("test0");
        myData.setAddress("重庆市");
        myData.setDateTime(LocalDateTime.now());

        String str = objectMapper.writeValueAsString(myData);
        System.out.println(str);
    }

    @Test
    public void jsonObject() throws JsonProcessingException {
        String json = "{\"id\":1,\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"name\":\"test0\",\"address\":\"重庆市\",\"dateTime\":\"2025-07-28 12:09:24.090000\"}";
        Object obj = objectMapper.readValue(json, Object.class);
        System.out.println(obj.getClass());
    }

    @Test
    public void test2() throws JsonProcessingException {
        List<MyData> list = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            MyData myData = new MyData();
            myData.setId((long) i);
            myData.setName("test" + i);
            myData.setAddress("重庆市" + i);
            myData.setDateTime(LocalDateTime.now());
            list.add(myData);
        }
        String str = objectMapper.writeValueAsString(list);
        System.out.println(str);

        List list2 = objectMapper.readValue(str, List.class);
        System.out.println(list2);

    }

    @Test
    public void jsonArray() throws JsonProcessingException {
        String json = "[\"java.util.ArrayList\",[{\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"id\":0,\"name\":\"test0\",\"address\":\"重庆市0\",\"dateTime\":\"2025-07-28 13:54:59.977000\"},{\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"id\":1,\"name\":\"test1\",\"address\":\"重庆市1\",\"dateTime\":\"2025-07-28 13:54:59.977000\"}]]";
        Object list = objectMapper.readValue(json, Object.class);
        System.out.println(list);
//        System.out.println(list.get(0).getClass());
    }

    @Test
    public void enum01() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        String json = "{\"name\":\"阿腾\",\"status\":3}";
        Project project = om.readValue(json, Project.class);
        System.out.println(project);
    }

}
