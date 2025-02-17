package local.ateng.java.redis.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedissonService {
    private final RedissonClient redissonClient;

    public void example() {
        // 获取 Redis 中的 RMap（类似于 HashMap）
        RMap<String, String> map = redissonClient.getMap("myMap");

        // 向 Redis 中添加数据
        map.put("key1", "value1");

        // 获取数据
        String value = map.get("key1");
        System.out.println("Value for 'key1': " + value);
    }

    public void exampleLock() {
        RLock lock = redissonClient.getLock("myLock");

        // 加锁
        lock.lock();
        try {
            // 执行需要保证互斥的代码
            System.out.println("Executing critical section...");
            Thread.sleep(Duration.ofSeconds(10));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

}
