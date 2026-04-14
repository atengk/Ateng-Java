package local.ateng.java.serialize.controller;

import local.ateng.java.serialize.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisController {
    private final RedisTemplate<String, Object> redisTemplate;

    // 序列化
    @GetMapping("/serialize")
    public String serialize() {
        Map<String, Object> map = Map.of("name", "ateng", "age", 26L);
        MyUser myUser = MyUser.builder()
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
                .build();
        redisTemplate.opsForValue().set("myUser", myUser);
        return "ok";
    }

    // 反序列化
    @GetMapping("/deserialize")
    public String deserialize() {
        MyUser myUser = (MyUser) redisTemplate.opsForValue().get("myUser");
        System.out.println(myUser);
        System.out.println(myUser.getCreateTime());
        return "ok";
    }

}
