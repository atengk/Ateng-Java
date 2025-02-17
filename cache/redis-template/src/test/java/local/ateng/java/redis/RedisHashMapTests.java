package local.ateng.java.redis;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.TypeUtil;
import local.ateng.java.redis.entity.UserInfoEntity;
import local.ateng.java.redis.init.InitData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.*;

/**
 * Redis HashMap相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisHashMapTests {
    private final RedisTemplate redisTemplate;

    // 新增hashMap值
    @Test
    void put() {
        String key = "my:hashmap:user";
        String hashKey = "user1";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForHash().put(key, hashKey, user);
    }

    @Test
    void putAll() {
        Map<String, Object> map = new HashMap<>();
        List<UserInfoEntity> list = new InitData().getList();
        list.forEach(user -> map.put("user" + user.getId(), user));
        redisTemplate.opsForHash().putAll("my:hashmap:userList", map);
    }

    // 获取hashMap值，不存在为null
    @Test
    void get() {
        String key = "my:hashmap:user";
        String hashKey = "user1";
        UserInfoEntity result = (UserInfoEntity) redisTemplate.opsForHash().get(key, hashKey);
        System.out.println(result);
        System.out.println(result.getName());
    }

    // 获取Key
    @Test
    void getKey() {
        String key = "my:hashmap:user";
        Set keys = redisTemplate.opsForHash().keys(key);
        String next = (String) keys.iterator().next();
        System.out.println(next);
    }

    // 获取多个hashMap的值
    @Test
    void multiGet() {
        String key = "my:hashmap:userList";
        List<UserInfoEntity> list = (List<UserInfoEntity>) redisTemplate.opsForHash().multiGet(key, Arrays.asList("user1", "user2"));
        System.out.println(list);
        list.forEach(user -> System.out.println(user.getName()));
    }

    // 获取所有hashMap的值
    @Test
    void getAll() {
        String key = "my:hashmap:userList";
        Map<String, UserInfoEntity> entries = (Map<String, UserInfoEntity>) redisTemplate.opsForHash().entries(key);
        System.out.println(entries);
        entries.values().forEach(value-> System.out.println(value.getName()));
    }

    // 获取所有hashMap的key值
    @Test
    void keys() {
        String key = "my:hashmap:userList";
        Set<String> keys = (Set<String>) redisTemplate.opsForHash().keys(key);
        System.out.println(keys);
    }

    // 获取所有hashMap的key值
    @Test
    void values() {
        String key = "my:hashmap:userList";
        List<UserInfoEntity> values = (List<UserInfoEntity>) redisTemplate.opsForHash().values(key);
        System.out.println(values);
        values.forEach(user -> System.out.println(user.getName()));
    }

    // 删除一个或者多个hash表字段
    @Test
    void delete() {
        String key = "my:hashmap:userList";
        String hashKey1 = "user1";
        String hashKey2 = "user2";
        Long result = redisTemplate.opsForHash().delete(key, hashKey1, hashKey2);
        System.out.println(result);
    }

    // 匹配获取键值对
    @Test
    void scan() {
        String key = "my:hashmap:userList";
        // 模糊匹配
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("user1*")
                .build();
        Cursor<Map.Entry<Object, Object>> result = redisTemplate.opsForHash().scan(key, scanOptions);
        while (result.hasNext()) {
            Map.Entry<Object, Object> entry = result.next();
            System.out.println(entry.getKey() + "：" + entry.getValue());
        }
    }

}
