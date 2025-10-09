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
//                .province("重庆市")
                .province(null)
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
        String json = "{\"id\":\"1\",\"age\":25,\"phoneNumber\":\"1762306666\",\"email\":\"kongyu2385569970@gmail.com\",\"score\":\"88.91\",\"ratio\":0.7147,\"birthday\":\"2000-01-01\",\"city\":\"重庆市\"}";
        // 进行反序列化
        MyUser myUser = objectMapper.readValue(json, MyUser.class);
        System.out.println(myUser);
    }

}
