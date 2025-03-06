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
