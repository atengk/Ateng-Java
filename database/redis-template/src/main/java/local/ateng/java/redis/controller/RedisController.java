package local.ateng.java.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisController {
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping("/send")
    public String sendMessage() {
        stringRedisTemplate.convertAndSend("myChannel", "Hello from Redis!");
        return "Message Sent!";
    }
}
