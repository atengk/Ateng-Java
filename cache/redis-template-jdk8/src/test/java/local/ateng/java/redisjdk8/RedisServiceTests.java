package local.ateng.java.redisjdk8;

import cn.hutool.core.thread.ThreadUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import local.ateng.java.redisjdk8.entity.UserInfoEntity;
import local.ateng.java.redisjdk8.init.InitData;
import local.ateng.java.redisjdk8.service.RLock;
import local.ateng.java.redisjdk8.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisServiceTests {
    private final RedisService redisService;

    @Test
    void set() {
        List<UserInfoEntity> list = new InitData().getList();
        redisService.set("my:user", list.get(0));
        redisService.set("my:userList", list);
    }

    @Test
    void get() {
        UserInfoEntity user = redisService.get("my:user", UserInfoEntity.class);
        List<UserInfoEntity> userList = redisService.get("my:userList", List.class);
        System.out.println(user.getClass());
        System.out.println(userList.get(0).getClass());
    }

    @Test
    void getTypeReference() {
        List<UserInfoEntity> userList = redisService.get("my:userList", new TypeReference<List<UserInfoEntity>>() {
        });
        System.out.println(userList.get(0).getClass());
    }

    @Test
    void getList() {
        List<UserInfoEntity> userList = redisService.getList("my:userList", UserInfoEntity.class);
        System.out.println(userList.get(0).getClass());
    }

    @Test
    void multiGet() {
        List<UserInfoEntity> entityList = redisService.multiGet(Arrays.asList("my:user", "my:user2"), UserInfoEntity.class);
        System.out.println(entityList.get(0).getClass());
    }

    @Test
    void hSet() {
        redisService.hSet("my:userMap", "a", new InitData().getList().get(0));
    }

    @Test
    void tryLock() {
        String lockKey = "lock:my";
        if (redisService.tryLock(lockKey, 10, TimeUnit.SECONDS)) { // 加锁 10 秒
            try {
                // 执行业务逻辑
                System.out.println("获取锁成功，执行任务...");
                ThreadUtil.sleep(5000);
            } finally {
                redisService.unlock(lockKey); // 释放锁
            }
        } else {
            System.out.println("获取锁失败，稍后重试...");
        }
    }

    @Test
    void tryLock2() {
        String lockKey = "lock:my";
        if (redisService.tryLock(lockKey, 10, 10, TimeUnit.SECONDS)) { // 加锁 10 秒
            try {
                // 执行业务逻辑
                System.out.println("获取锁成功，执行任务...");
                ThreadUtil.sleep(5000);
            } finally {
                redisService.unlock(lockKey); // 释放锁
            }
        } else {
            System.out.println("获取锁失败，稍后重试...");
        }
    }

    @Test
    void tryLock3() {
        String lockKey = "lock:my";
        // 阻塞式加锁，直到获得锁
        redisService.tryLock(lockKey);

        // 业务代码
        try {
            // 执行临界区操作
            ThreadUtil.sleep(3, TimeUnit.MINUTES);
        } finally {
            // 释放锁
            redisService.unlock(lockKey);
        }
    }

    @Test
    void lock() {
        final String lockKey = "lock:my";
        final RLock lock = redisService.getLock(lockKey);

        // 阻塞式加锁，租期 30 秒（watchdog 会自动续期）
        lock.lock(10, TimeUnit.SECONDS);

        try {
            // 模拟长时间业务（例如 3 分钟）
            Thread.sleep(Duration.ofMinutes(3).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }


}
