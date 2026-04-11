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

import java.util.List;

/**
 * Redisson RJsonBucket 操作 Redis ReJSON模块 的 JSON 数组 测试类
 *
 * @author ateng
 * @since 2026-04-11
 */
@Slf4j
@SpringBootTest
public class RedisJsonBucketJSONArrayTests {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 写入 JSON 数组（整个 key 是数组）
     */
    @Test
    void setArray() {
        String key = "user:json:list";

        // 注意：这里必须用 TypeReference
        JsonCodec codec = new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
        });

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(key, codec);

        List<UserInfoEntity> list = new InitData().getList();

        bucket.set(list);

        log.info("写入JSON数组成功，size={}", list.size());
    }

    /**
     * 读取整个 JSON 数组
     */
    @Test
    void getArray() {
        String key = "user:json:list";

        JsonCodec codec = new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
        });
        RJsonBucket<List<UserInfoEntity>> bucket = redissonClient.getJsonBucket(key, codec);

        List<UserInfoEntity> list = bucket.get();

        log.info("读取数组成功，size={}", list == null ? 0 : list.size());
    }

    /**
     * 获取数组指定元素
     */
    @Test
    void getArrayIndex() {
        String key = "user:json:list";

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(key,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
                        }));

        List<UserInfoEntity> list = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {}),
                "$[0]"
        );

        UserInfoEntity user = (list != null && !list.isEmpty()) ? list.get(0) : null;

        log.info("第一个元素={}", user);
    }

    /**
     * 数组追加元素（对象）
     */
    @Test
    void addArrayElement() {
        String key = "user:json:list";

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(key,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
                        }));

        UserInfoEntity user = new InitData().getList().get(0);

        // 根数组用 "$"
        bucket.arrayAppend("$", user);

        log.info("追加元素成功");
    }

    /**
     * 批量追加元素
     */
    @Test
    void addArrayBatch() {
        String key = "user:json:list";

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(key,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
                        }));

        List<UserInfoEntity> list = new InitData().getList();

        bucket.arrayAppend("$", list.toArray());

        log.info("批量追加成功，count={}", list.size());
    }

    /**
     * 指定位置插入元素
     */
    @Test
    void insertArrayElement() {
        String key = "user:json:list";

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(key,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
                        }));

        UserInfoEntity user = new InitData().getList().get(1);

        bucket.arrayInsertMulti("$", 1, user);

        log.info("插入成功");
    }

    /**
     * 删除数组指定索引元素
     */
    @Test
    void removeArrayElement() {
        String key = "user:json:list";

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(
                        key,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {})
                );

        // 删除第0个元素，并返回被删除对象
        List<UserInfoEntity> removed = bucket.arrayPopMulti(
                new JacksonCodec<>(UserInfoEntity.class),
                "$",
                0
        );

        log.info("删除元素={}", removed);
    }

    /**
     * 获取数组长度
     */
    @Test
    void arraySize() {
        String key = "user:json:list";

        RJsonBucket<Object> bucket =
                redissonClient.getJsonBucket(
                        key,
                        new JacksonCodec<>(Object.class)
                );

        List<Long> list = bucket.arraySizeMulti("$");
        long size = list.get(0);

        log.info("数组长度={}", size);
    }

    /**
     * 数组分页（区间获取）
     */
    @Test
    void arrayRange() {
        String key = "user:json:list";

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(key,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
                        }));

        List<UserInfoEntity> subList = bucket.get(
                new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
                }),
                "$[0:2]"
        );

        log.info("分页结果 size={}", subList == null ? 0 : subList.size());
    }

    /**
     * 更新数组指定位置元素
     */
    @Test
    void updateArrayIndex() {
        String key = "user:json:list";

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(key,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
                        }));

        UserInfoEntity user = new InitData().getList().get(2);

        bucket.set("$[0]", user);

        log.info("更新第0个元素成功");
    }

    /**
     * 清空数组
     */
    @Test
    void clearArray() {
        String key = "user:json:list";

        RJsonBucket<List<UserInfoEntity>> bucket =
                redissonClient.getJsonBucket(key,
                        new JacksonCodec<>(new TypeReference<List<UserInfoEntity>>() {
                        }));

        List<Long> list = bucket.arrayTrimMulti("$", 1, 0);

        log.info("数组已清空, {}", list);
    }

}