package local.ateng.java.serialize.controller;

import local.ateng.java.serialize.entity.MyUser;
import local.ateng.java.serialize.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/jackson")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JacksonController {

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
                .aBBCCdd("aBBCCdd")
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
    public String deserialize2(@RequestBody Map<String, Object> myUser) {
        System.out.println(myUser);
        return "ok";
    }

    // 反序列化
    @PostMapping("/deserialize3")
    public String deserialize3(@RequestBody Result<MyUser> list) {
        System.out.println(list);
        System.out.println(list.getData().getCreateTime());
        return "ok";
    }

    // 反序列化
    @PostMapping("/deserialize4")
    public String deserialize4(@RequestBody List<MyUser> myUserList) {
        System.out.println(myUserList);
        return "ok";
    }

}
