package local.ateng.java.redisjdk8;

import com.fasterxml.jackson.core.type.TypeReference;
import local.ateng.java.redisjdk8.entity.UserInfoEntity;
import local.ateng.java.redisjdk8.init.InitData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RJsonBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Redisson RJsonBucket 操作 Redis ReJSON模块 的 JSONPath 测试类
 *
 * @author ateng
 * @since 2026-04-11
 */
@Slf4j
@SpringBootTest
public class RedisJsonBucketJSONPathTests {

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY = "user:json:jsonpath";

    /**
     * 初始化数组数据
     */
    @Test
    void initArray() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> list = new InitData().getList();
        bucket.set(list);

        log.info("初始化数组完成 size={}", list.size());
    }

    /**
     * JSONPath：取整个数组
     */
    @Test
    void path_array_all() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> result = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[*]"
        );

        log.info("取整个数组 size={}", result == null ? 0 : result.size());
    }

    /**
     * JSONPath：取单个元素
     */
    @Test
    void path_array_index() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> list = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[0]"
        );

        UserInfoEntity user = (list != null && !list.isEmpty()) ? list.get(0) : null;

        log.info("第一个元素={}", user);
    }

    /**
     * JSONPath：数组切片（分页）
     */
    @Test
    void path_array_slice() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> result = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[0:2]"
        );

        log.info("切片结果 size={}", result == null ? 0 : result.size());
    }

    /**
     * JSONPath：取所有 name 字段
     */
    @Test
    void path_array_field_projection() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<String> names = bucket.get(
                new JacksonCodec<>(new TypeReference<List<String>>() {}),
                "$[*].name"
        );

        log.info("names={}", names);
    }

    /**
     * JSONPath：嵌套字段 city
     */
    @Test
    void path_array_nested_field() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<String> cities = bucket.get(
                new JacksonCodec<>(new TypeReference<List<String>>() {}),
                "$[*].address.city"
        );

        log.info("cities={}", cities);
    }

    /**
     * JSONPath：数组 where（age > 18）
     */
    @Test
    void path_where_age() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> result = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[?(@.age > 18)]"
        );

        log.info("age>18 size={}", result == null ? 0 : result.size());
    }

    /**
     * JSONPath：where + AND 条件
     */
    @Test
    void path_where_and() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> result = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[?(@.age >= 18 && @.name == '傅立诚')]"
        );

        log.info("AND条件 size={}", result == null ? 0 : result.size());
    }

    /**
     * JSONPath：where + OR 条件
     */
    @Test
    void path_where_or() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> result = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[?(@.age > 30 || @.name == '张三')]"
        );

        log.info("OR条件 size={}", result == null ? 0 : result.size());
    }

    /**
     * JSONPath：tags 数组包含过滤
     */
    @Test
    void path_where_tags_contains() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> result = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[?(@.tags[*] == 'java')]"
        );

        log.info("tags contains java size={}", result == null ? 0 : result.size());
    }

    /**
     * JSONPath：where + 返回字段（name）
     */
    @Test
    void path_where_projection() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<String> names = bucket.get(
                new JacksonCodec<>(new TypeReference<List<String>>() {}),
                "$[?(@.age > 18)].name"
        );

        log.info("过滤后names={}", names);
    }

    /**
     * JSONPath：多层嵌套 + where
     */
    @Test
    void path_where_nested() {
        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        KEY,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        List<UserInfoEntity> result = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[?(@.address.city == 'SG')]"
        );

        log.info("city=SG size={}", result == null ? 0 : result.size());
    }
}