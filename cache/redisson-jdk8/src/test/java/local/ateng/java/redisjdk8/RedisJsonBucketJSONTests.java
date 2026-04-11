package local.ateng.java.redisjdk8;

import com.fasterxml.jackson.core.type.TypeReference;
import local.ateng.java.redisjdk8.entity.UserInfoEntity;
import local.ateng.java.redisjdk8.init.InitData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RJsonBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JacksonCodec;
import org.redisson.codec.JsonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

/**
 * Redisson RJsonBucket 操作 Redis ReJSON模块 的 JSON 测试类
 *
 * @author ateng
 * @since 2026-04-11
 */
@Slf4j
@SpringBootTest
public class RedisJsonBucketJSONTests {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 写入 JSON
     */
    @Test
    void set() {
        String key = "user:json";
        JsonCodec codec = new JacksonCodec<>(UserInfoEntity.class);
        RJsonBucket<UserInfoEntity> bucket = redissonClient.getJsonBucket(key, codec);

        UserInfoEntity user = new InitData().getList().get(0);
        bucket.set(user);
        log.info("保存用户 JSON 成功，key={}", key);
    }

    /**
     * 读取 JSON（整对象）
     */
    @Test
    void get() {
        String key = "user:json";
        JsonCodec codec = new JacksonCodec<>(UserInfoEntity.class);
        RJsonBucket<UserInfoEntity> bucket = redissonClient.getJsonBucket(key, codec);

        UserInfoEntity user = bucket.get();
        log.info("读取用户 JSON 成功，user={}", user);
    }

    /**
     * 覆盖更新 JSON（整对象）
     */
    @Test
    void update() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        UserInfoEntity user = new InitData().getList().get(1);
        bucket.set(user);

        log.info("覆盖更新成功，key={}", key);
    }

    /**
     * CAS 原子更新
     */
    @Test
    void compareAndSet() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        UserInfoEntity oldVal = bucket.get();
        UserInfoEntity newVal = new InitData().getList().get(2);

        boolean result = bucket.compareAndSet(oldVal, newVal);
        log.info("CAS更新结果={}, newVal={}", result, newVal);
    }

    /**
     * 获取旧值并更新
     */
    @Test
    void getAndSet() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        UserInfoEntity newVal = new InitData().getList().get(3);
        UserInfoEntity oldVal = bucket.getAndSet(newVal);

        log.info("旧值={}, 新值={}", oldVal, newVal);
    }

    /**
     * 设置过期时间
     */
    @Test
    void expire() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        boolean result = bucket.expire(Duration.ofMinutes(10));
        log.info("设置过期时间结果={}", result);
    }

    /**
     * 判断 key 是否存在
     */
    @Test
    void isExists() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        boolean exists = bucket.isExists();
        log.info("是否存在={}", exists);
    }

    /**
     * 修改 JSON 指定字段
     */
    @Test
    void setField() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        bucket.set("$.name", "Ateng");
        log.info("修改 name 字段成功");
    }

    /**
     * 获取 JSON 指定字段
     */
    @Test
    void getField() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        List<String> list = bucket.get(
                new JacksonCodec<>(new TypeReference<List<String>>() {}),
                "$.name"
        );

        String name = (list != null && !list.isEmpty()) ? list.get(0) : null;
        log.info("name={}", name);
    }

    /**
     * 删除 JSON 字段
     */
    @Test
    void deleteField() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        long count = bucket.delete("$.age");
        log.info("删除字段数量={}", count);
    }

    /**
     * 字段级 CAS
     */
    @Test
    void compareAndSetField() {
        String key = "user:json";
        RJsonBucket<UserInfoEntity> bucket =
                redissonClient.getJsonBucket(key, new JacksonCodec<>(UserInfoEntity.class));

        boolean result = bucket.compareAndSet("$.age", 26, 18);
        log.info("字段CAS结果={}", result);
    }

}