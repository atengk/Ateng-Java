package local.ateng.java.redis.controller;

import local.ateng.java.redis.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redisson")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedissonController {
    private final RedissonService redissonService;

    @GetMapping("/map")
    public void map() {
        redissonService.example();
    }

    @GetMapping("/lock")
    public void lock() {
        redissonService.exampleLock();
    }

}
