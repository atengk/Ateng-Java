package local.ateng.java.redis;

import local.ateng.java.redis.entity.UserInfoEntity;
import local.ateng.java.redis.init.InitData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * Redis List相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisListTests {
    private final RedisTemplate redisTemplate;

    /**
     * 功能: 将指定的值 value 添加到列表 key 的 右端（尾部）。
     * 使用场景: 通常用于像队列一样的操作，先进先出（FIFO）。例如，添加数据到消息队列的末尾，等待后续的消费
     */
    @Test
    void rightPush() {
        String key = "my:list:user";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForList().rightPush(key, user);
    }

    @Test
    void rightPushAll() {
        String key = "my:list:userList";
        List<UserInfoEntity> list = new InitData().getList();
        redisTemplate.opsForList().rightPushAll(key, list);
    }

    /**
     * 用于从Redis的列表中 弹出并移除 右端（尾部）的元素。这个操作类似于从队列的尾部取出元素。
     */
    @Test
    void rightPop() {
        String key = "my:list:userList";
        UserInfoEntity userInfoEntity = (UserInfoEntity) redisTemplate.opsForList().rightPop(key);
        System.out.println(userInfoEntity.getId());
    }

    /**
     * 功能: 将指定的值 value 添加到列表 key 的 左端（头部）。
     * 使用场景: 通常用于像栈一样的操作，后进先出（LIFO）。例如，添加数据到列表的开头。
     */
    @Test
    void leftPush() {
        String key = "my:list:user";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForList().leftPush(key, user);
    }

    @Test
    void leftPushAll() {
        String key = "my:list:userList";
        List<UserInfoEntity> list = new InitData().getList();
        redisTemplate.opsForList().leftPushAll(key, list);
    }

    /**
     * 用于从 Redis 列表中 弹出并移除 左端（头部）元素的方法。这个操作类似于从队列的头部取出元素。
     */
    @Test
    void leftPop() {
        String key = "my:list:userList";
        UserInfoEntity userInfoEntity = (UserInfoEntity) redisTemplate.opsForList().leftPop(key);
        System.out.println(userInfoEntity.getId());
    }

    // 获取列表指定范围内的元素(start开始位置, 0是开始位置，end 结束位置, -1返回所有)
    @Test
    void range() {
        String key = "my:list:userList";
        long start = 0;
        long end = -1;
        List<UserInfoEntity> result = redisTemplate.opsForList().range(key, start, end);
        System.out.println(result);
    }

}
