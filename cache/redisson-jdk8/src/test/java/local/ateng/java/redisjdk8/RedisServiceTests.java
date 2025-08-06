package local.ateng.java.redisjdk8;


import com.fasterxml.jackson.core.type.TypeReference;
import local.ateng.java.redisjdk8.entity.UserInfoEntity;
import local.ateng.java.redisjdk8.init.InitData;
import local.ateng.java.redisjdk8.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisServiceTests {
    private final RedissonService redissonService;

    @Test
    void set() {
        List<UserInfoEntity> list = new InitData().getList();
        redissonService.set("my:user", list.get(0));
        Map<String, UserInfoEntity> map = new HashMap<>();
        map.put("test", list.get(0));
        redissonService.set("my:userMap", map);
        redissonService.set("my:userList", list);
    }

    @Test
    void get() {
        UserInfoEntity userInfoEntity = redissonService.get("my:user",new TypeReference<UserInfoEntity>(){});
        System.out.println(userInfoEntity.getCity());
    }

    @Test
    void getList() {
        List<UserInfoEntity> list = redissonService.get("my:userList", new TypeReference<List<UserInfoEntity>>() {});
        System.out.println(list.get(0).getClass());
    }

    @Test
    void getMap() {
        Map<String, UserInfoEntity> map = redissonService.get("my:userMap", new TypeReference<Map<String, UserInfoEntity>>() {});
        System.out.println(map.get("test").getClass());
    }

    /**
     * 获取可重入锁（Reentrant Lock）
     */
    @Test
    void lock1() throws InterruptedException {
        // 获取锁对象
        RLock lock = redissonService.getLock("myLock");

        // 阻塞式加锁，直到获得锁
        lock.lock();

        // 业务代码
        try {
            // 执行临界区操作
            Thread.sleep(1000000);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 设置自动释放时间的锁（防止死锁）
     */
    @Test
    void lock2() throws InterruptedException {
        // 获取锁对象
        RLock lock = redissonService.getLock("myLock");

        // 阻塞式加锁，10秒后自动释放锁
        lock.lock(10, TimeUnit.SECONDS);

        // 业务代码
        try {
            // 执行临界区操作
            Thread.sleep(1000000);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 尝试加锁（带超时）
     */
    @Test
    void lock3() throws InterruptedException {
        RLock lock = redissonService.getLock("myLock");

        boolean locked = false;
        try {
            // 尝试等待最多5秒获取锁，获取后锁自动10秒释放
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (locked) {
                // 获得锁，执行业务逻辑
                Thread.sleep(1000000);
            } else {
                // 未获得锁，可以做其他处理（比如返回失败或重试）
                System.out.println("未获得锁，可以做其他处理（比如返回失败或重试）");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Test
    void bloom1() {
        redissonService.bloomInit("test:bloomfilter", 1000L, 0.03);
    }

    @Test
    void bloom2() {
        long addedCount = redissonService.bloomAddAll("test:bloomfilter", java.util.Arrays.asList("a", "b", "c"));
        System.out.println(addedCount);
        long addedCount2 = redissonService.bloomAddAll("test:bloomfilter", java.util.Arrays.asList("a", "b", "c", "d"));
        System.out.println(addedCount2);
    }

}
