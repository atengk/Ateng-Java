package local.ateng.java.redis;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Redis Set集合相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisSetTests {
    private final RedisTemplate redisTemplate;

    // 添加数据
    @Test
    void add() {
        String key = "my:set:number";
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(RandomUtil.randomInt(1, 100));
        }
        redisTemplate.opsForSet().add(key, list.toArray(new Integer[0]));
    }

    // 获取集合中的所有数据
    @Test
    void members() {
        String key = "my:set:number";
        Set<Integer> members = (LinkedHashSet<Integer>) redisTemplate.opsForSet().members(key);
        System.out.println(members);
    }

    // 删除数据
    @Test
    void remove() {
        String key = "my:set:number";
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(RandomUtil.randomInt(1, 1000));
        }
        redisTemplate.opsForSet().remove(key, list.toArray(new Integer[0]));
    }

    // 删除并且返回一个随机的元素
    @Test
    void pop() {
        String key = "my:set:number";
        Integer result1 = (Integer) redisTemplate.opsForSet().pop(key);
        List<Integer> result2 = (ArrayList<Integer>) redisTemplate.opsForSet().pop(key, 2); // pop多个
        System.out.println(result1);
        System.out.println(result2);
    }

    // 判断集合是否包含value
    @Test
    void isMember() {
        String key = "my:set:number";
        String value = "60";
        Boolean result = redisTemplate.opsForSet().isMember(key, value);
        System.out.println(result);
    }

    // 获取两个集合的交集
    @Test
    void intersect() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().intersect(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的交集，并将结果存储到新的key
    @Test
    void intersectAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:intersect";
        redisTemplate.opsForSet().intersectAndStore(key1, key2, destKey);
    }

    // 获取两个集合的并集
    @Test
    void union() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().union(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的并集，并将结果存储到新的key
    @Test
    void unionAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:union";
        redisTemplate.opsForSet().unionAndStore(key1, key2, destKey);
    }

    // 获取两个集合的差集
    @Test
    void difference() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().difference(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的并集，并将结果存储到新的key
    @Test
    void differenceAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:difference";
        redisTemplate.opsForSet().differenceAndStore(key1, key2, destKey);
    }

    // 随机获取集合中的一个元素
    @Test
    void randomMember() {
        String key = "my:set:number";
        Integer result = (Integer) redisTemplate.opsForSet().randomMember(key);
        System.out.println(result);
    }

    // 随机获取集合中count个元素
    @Test
    void randomMembers() {
        String key = "my:set:number";
        List<Integer> strings = (ArrayList<Integer>) redisTemplate.opsForSet().randomMembers(key, 2);
        System.out.println(strings);
    }

    // 模糊匹配数据
    @Test
    void scan() {
        String key = "my:set:number";
        // 模糊匹配
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("2*")
                .build();
        Cursor result1 = redisTemplate.opsForSet().scan(key, scanOptions);
        while (result1.hasNext()) {
            Object next = result1.next();
            System.out.println(next);
        }
    }
}
