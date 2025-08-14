package local.ateng.java.redisjdk8;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisTests {

    private final RedissonClient redissonClient;

    @Test
    void test01() {
        Object obj = redissonClient.getScript()
                .eval(
                        RScript.Mode.READ_WRITE, // 脚本执行模式：READ_ONLY 或 READ_WRITE
                        "return redis.call('EXISTS', KEYS[1]) == 1",
                        RScript.ReturnType.VALUE, // 返回类型，可选 VALUE, BOOLEAN, MULTI 等
                        Arrays.asList("my:user")
                );
        System.out.println(obj);
    }

    @Test
    void test02() {
        Object obj = redissonClient.getScript()
                .eval(
                        RScript.Mode.READ_WRITE, // 脚本执行模式：READ_ONLY 或 READ_WRITE
                        "return redis.call('JSON.OBJLEN', KEYS[1], ARGV[1])",
                        RScript.ReturnType.VALUE, // 返回类型，可选 VALUE, BOOLEAN, MULTI 等
                        Arrays.asList("user:1001"), "$"
                );
        System.out.println(obj);
    }

}
