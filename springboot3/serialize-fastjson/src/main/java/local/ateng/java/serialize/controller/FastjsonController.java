package local.ateng.java.serialize.controller;

import com.alibaba.fastjson.JSONObject;
import local.ateng.java.serialize.entity.MyUser;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/fastjson")
public class FastjsonController {

    // 序列化
    @GetMapping("/serialize")
    public MyUser serialize() {
        return MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(25)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("8800000000000000000000000000.911000000000000000000000"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province("<")
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .createTime2(new Date())
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
