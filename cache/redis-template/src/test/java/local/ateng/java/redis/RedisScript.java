package local.ateng.java.redis;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisScript {
    private final RedisTemplate redisTemplate;

    @Test
    public void test() {
        Long success = setIfAbsentWithExpire("myKey", 100, 60);
        System.out.println("是否设置成功: " + success);
    }

    /**
     * 尝试设置一个键值对到 Redis 中，只有当该 key 不存在时才设置，并带有过期时间。
     *
     * @param key           要设置的 Redis key
     * @param value         要设置的值
     * @param expireSeconds 过期时间（单位：秒）
     * @return 如果设置成功（即 key 原本不存在），返回 true；否则返回 false
     */
    public Long setIfAbsentWithExpire(String key, Integer value, int expireSeconds) {
        // Lua 脚本：如果 key 不存在，则 setex 设置键值并设置过期时间，否则不做任何操作
        String script = "local stock = tonumber(redis.call('GET', KEYS[1]))\n" +
                "local reduce = tonumber(ARGV[1])\n" +
                "if stock >= reduce then\n" +
                "    return redis.call('DECRBY', KEYS[1], reduce)\n" +
                "else\n" +
                "    return -1\n" +
                "end";

        // 构造 RedisScript 对象，指定返回类型为 Long（Lua 返回 1 或 0）
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        // 执行脚本：传入 key（作为 KEYS[1]），value 和 expireSeconds（作为 ARGV[1] 和 ARGV[2]）
        Long result = (Long) redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),  // 只有一个 key
                value,                           // ARGV[1]：要设置的值
                expireSeconds                    // ARGV[2]：过期时间
        );

        // 返回脚本执行结果（1 表示设置成功，0 表示 key 已存在）
        return result;
    }
}
