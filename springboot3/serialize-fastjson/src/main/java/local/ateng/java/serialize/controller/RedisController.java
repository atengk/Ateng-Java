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
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisController {
    private final RedisTemplate<String, Object> redisTemplate;

    // 序列化
    @GetMapping("/serialize")
    public String serialize() {
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
                .list(List.of("1","2"))
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