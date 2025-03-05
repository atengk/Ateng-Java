package local.ateng.java.serialize.controller;

import local.ateng.java.serialize.entity.MyUser;
import local.ateng.java.serialize.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jackson")
public class JacksonController {

    // 序列化
    @GetMapping("/serialize")
    public Result<MyUser> serialize() {
        MyUser myUser = MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(null)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("88.911"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province(null)
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .createTime2(new Date())
                .list(List.of())
                .build();
        Result<MyUser> success = Result.success()
                .withCode("200")
                .withMsg("成功")
                .withData(myUser)
                .withExtra("a", "bv")
                .withExtra("b", "cv")
                .withExtra(Map.of("1",2,"3","4"));
        System.out.println(success);
        return success;
    }

    // 反序列化
    @PostMapping("/deserialize")
    public String deserialize(@RequestBody MyUser myUser) {
        System.out.println(myUser);
        return "ok";
    }

    // 反序列化
    @PostMapping("/deserialize2")
    public String deserialize2(@RequestBody Result<MyUser> list) {
        System.out.println(list);
        System.out.println(list.getData().getCreateTime());
        return "ok";
    }

}
