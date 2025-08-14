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
import java.util.Collections;
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

        // 阻塞式加锁，租期 60 秒
        lock.lock();

        try {
            // 模拟长时间业务（例如 30 秒）
            System.out.println("获取锁成功，执行任务...");
            Thread.sleep(Duration.ofSeconds(30).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    @Test
    void lockPlus() {
        final String lockKey = "lock:my";
        final RLock lock = redisService.getLockPlus(lockKey);

        // 阻塞式加锁，租期 30 秒（watchdog 会自动续期）
        lock.lock(10, TimeUnit.SECONDS);

        try {
            // 模拟长时间业务（例如 3 分钟）
            System.out.println("获取锁失败，稍后重试...");
            Thread.sleep(Duration.ofMinutes(3).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 测试 eval 方法（返回整数）
     */
    @Test
    void testEval() {
        String script = "return tonumber(ARGV[1]) + tonumber(ARGV[2])";
        Long result = redisService.eval(script, Long.class, Collections.emptyList(), 5, 7);
        System.out.println(result);
    }

    @Test
    void testEval_LockScript() {
        String script = "if redis.call('SETNX', KEYS[1], ARGV[1]) == 1 then\n" +
                "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
                "    return 1\n" +
                "else\n" +
                "    return 0\n" +
                "end";

        String lockKey = "lock:test";
        String lockValue = "uuid-12345"; // 可以是唯一标识，比如 UUID
        int expireSeconds = 30;          // 锁过期时间 30 秒

        Long result = redisService.eval(script, Long.class,
                Collections.singletonList(lockKey),
                lockValue, expireSeconds);

        if (result != null && result == 1) {
            System.out.println("获取锁成功");
        } else {
            System.out.println("获取锁失败");
        }
    }


    /**
     * 测试 evalNoResult 方法（无返回值）
     */
    @Test
    void testEvalNoResult() {
        String script = "redis.call('SET', KEYS[1], ARGV[1])";
        redisService.evalNoResult(script, Collections.singletonList("test:key"), "hello");
        String value = redisService.eval("return redis.call('GET', KEYS[1])", String.class, Collections.singletonList("test:key"));
        System.out.println(value);
    }

    /**
     * 测试 evalNoResult 方法：JSON.SET + JSON.GET
     */
    @Test
    void testEvalNoResult_JsonSetGet() {
        String setScript = "redis.call('JSON.SET', KEYS[1], '$', ARGV[1])";
        String jsonData = "{\"title\":\"Developer\",\"skills\":[\"Java\",\"Vue\"]}";
        redisService.evalNoResult(setScript,
                Collections.singletonList("user:1002"), jsonData);

        String getScript = "return redis.call('JSON.GET', KEYS[1], '$.title')";
        String title = redisService.eval(getScript, String.class,
                Collections.singletonList("user:1002"));

        System.out.println(title);
    }


    /**
     * 测试 evalNoResult() — 执行 Lua 脚本无返回值
     */
    @Test
    void testEvalNoResult2() {
        String script = "redis.call('JSON.SET', KEYS[1], '$', ARGV[1])";
        redisService.evalNoResult(script,
                Collections.singletonList("json:test:user2"),
                "{\"name\":\"Blair\",\"age\":30}");
        Object verify = redisService.eval("return redis.call('JSON.GET', KEYS[1], '$')",
                Object.class, Collections.singletonList("user:1002"));
        System.out.println(verify);
    }

    /**
     * 测试 loadScript() + evalBySha()
     */
    @Test
    void testEvalBySha() {
        String script = "redis.call('JSON.SET', KEYS[1], '$', ARGV[1]) " +
                "return redis.call('JSON.GET', KEYS[1], '$')";
        String sha1 = redisService.loadScript(script);

        String result = redisService.evalBySha(sha1, String.class,
                Collections.singletonList("json:test:user3"),
                "{\"name\":\"Alice\",\"age\":28}");
        System.out.println(result);
    }
    
}
