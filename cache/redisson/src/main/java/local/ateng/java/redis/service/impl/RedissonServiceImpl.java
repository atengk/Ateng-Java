package local.ateng.java.redis.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.redis.service.RedissonService;
import org.redisson.api.*;
import org.redisson.api.geo.GeoSearchArgs;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Redis 服务实现类
 * 基于 Spring Boot 3 + Redisson 封装 Redis 常用能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Service
public class RedissonServiceImpl implements RedissonService {

    private static final Logger log = LoggerFactory.getLogger(RedissonServiceImpl.class);

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    public RedissonServiceImpl(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Redisson 原生对象访问
    // -------------------------------------------------------------------------

    /**
     * 获取 RedissonClient 实例。
     *
     * @return RedissonClient 实例
     */
    @Override
    public RedissonClient getClient() {
        return redissonClient;
    }

    /**
     * 获取对象桶。
     *
     * @param key Redis 键
     * @param <T> 值类型
     * @return RBucket
     */
    @Override
    public <T> RBucket<T> getBucket(String key) {
        checkKey(key);
        return redissonClient.getBucket(key);
    }

    /**
     * 获取哈希 Map。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RMap
     */
    @Override
    public <K, V> RMap<K, V> getMap(String key) {
        checkKey(key);
        return redissonClient.getMap(key);
    }

    /**
     * 获取带 TTL 能力的 MapCache。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RMapCache
     */
    @Override
    public <K, V> RMapCache<K, V> getMapCache(String key) {
        checkKey(key);
        return redissonClient.getMapCache(key);
    }

    /**
     * 获取列表。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RList
     */
    @Override
    public <T> RList<T> getList(String key) {
        checkKey(key);
        return redissonClient.getList(key);
    }

    /**
     * 获取双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RDeque
     */
    @Override
    public <T> RDeque<T> getDeque(String key) {
        checkKey(key);
        return redissonClient.getDeque(key);
    }

    /**
     * 获取集合。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RSet
     */
    @Override
    public <T> RSet<T> getSet(String key) {
        checkKey(key);
        return redissonClient.getSet(key);
    }

    /**
     * 获取带元素 TTL 能力的 SetCache。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RSetCache
     */
    @Override
    public <T> RSetCache<T> getSetCache(String key) {
        checkKey(key);
        return redissonClient.getSetCache(key);
    }

    /**
     * 获取有序集合。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RScoredSortedSet
     */
    @Override
    public <T> RScoredSortedSet<T> getScoredSortedSet(String key) {
        checkKey(key);
        return redissonClient.getScoredSortedSet(key);
    }

    /**
     * 获取 BitSet。
     *
     * @param key Redis 键
     * @return RBitSet
     */
    @Override
    public RBitSet getBitSet(String key) {
        checkKey(key);
        return redissonClient.getBitSet(key);
    }

    /**
     * 获取 HyperLogLog。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RHyperLogLog
     */
    @Override
    public <T> RHyperLogLog<T> getHyperLogLog(String key) {
        checkKey(key);
        return redissonClient.getHyperLogLog(key);
    }

    /**
     * 获取 Geo。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RGeo
     */
    @Override
    public <T> RGeo<T> getGeo(String key) {
        checkKey(key);
        return redissonClient.getGeo(key);
    }

    /**
     * 获取普通队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RQueue
     */
    @Override
    public <T> RQueue<T> getQueue(String key) {
        checkKey(key);
        return redissonClient.getQueue(key);
    }

    /**
     * 获取阻塞队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RBlockingQueue
     */
    @Override
    public <T> RBlockingQueue<T> getBlockingQueue(String key) {
        checkKey(key);
        return redissonClient.getBlockingQueue(key);
    }

    /**
     * 获取可靠队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RReliableQueue
     */
    @Override
    public <T> RReliableQueue<T> getReliableQueue(String key) {
        checkKey(key);
        return redissonClient.getReliableQueue(key);
    }

    /**
     * 获取消息主题。
     *
     * @param topic 主题名称
     * @return RTopic
     */
    @Override
    public RTopic getTopic(String topic) {
        checkKey(topic);
        return redissonClient.getTopic(topic);
    }

    /**
     * 获取 Stream。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RStream
     */
    @Override
    public <K, V> RStream<K, V> getStream(String key) {
        checkKey(key);
        return redissonClient.getStream(key);
    }

    /**
     * 获取脚本执行对象。
     *
     * @return RScript
     */
    @Override
    public RScript getScript() {
        return redissonClient.getScript();
    }

    // -------------------------------------------------------------------------
    // 通用 Key 管理
    // -------------------------------------------------------------------------

    /**
     * 判断指定 key 是否存在。
     *
     * @param key Redis 键
     * @return 存在返回 true
     */
    @Override
    public boolean hasKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return redissonClient.getKeys().countExists(key) > 0;
    }

    /**
     * 统计多个 key 中实际存在的数量。
     *
     * @param keys Redis 键集合
     * @return 存在数量
     */
    @Override
    public long countExists(String... keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return 0L;
        }
        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }
        return redissonClient.getKeys().countExists(keyArray);
    }

    /**
     * 删除指定 key。
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @Override
    public boolean deleteKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return redissonClient.getKeys().delete(key) > 0;
    }

    /**
     * 批量删除 key。
     *
     * @param keys Redis 键集合
     * @return 删除数量
     */
    @Override
    public long deleteKeys(Collection<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return 0L;
        }
        String[] keyArray = keys.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }
        return redissonClient.getKeys().delete(keyArray);
    }

    /**
     * 根据通配符删除 key。
     *
     * @param pattern 通配符表达式
     * @return 删除数量
     */
    @Override
    public long deleteByPattern(String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return 0L;
        }
        return redissonClient.getKeys().deleteByPattern(pattern);
    }

    /**
     * 设置 key 过期时间。
     *
     * @param key     Redis 键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout > 0, "过期时间必须大于 0");
        return redissonClient.getBucket(key).expire(timeout, unit);
    }

    /**
     * 设置 key 过期时间。
     *
     * @param key Redis 键
     * @param ttl 过期时间
     * @return 是否设置成功
     */
    @Override
    public boolean expire(String key, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "过期时间不能为空且必须大于 0");
        return redissonClient.getBucket(key).expire(ttl);
    }

    /**
     * 获取 key 剩余过期时间。
     *
     * @param key  Redis 键
     * @param unit 时间单位
     * @return 剩余时间，-1 表示永久，-2 表示不存在
     */
    @Override
    public long getTtl(String key, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");

        long ttlMillis = redissonClient.getBucket(key).remainTimeToLive();
        if (ttlMillis < 0) {
            return ttlMillis;
        }
        return unit.convert(ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 移除 key 过期时间。
     *
     * @param key Redis 键
     * @return 是否成功
     */
    @Override
    public boolean persist(String key) {
        checkKey(key);
        return redissonClient.getBucket(key).clearExpire();
    }

    /**
     * 修改 key 名称。
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return 是否成功
     */
    @Override
    public boolean renameKey(String oldKey, String newKey) {
        checkKey(oldKey);
        checkKey(newKey);

        try {
            redissonClient.getKeys().rename(oldKey, newKey);
            return true;
        } catch (Exception e) {
            log.warn("重命名 Redis Key 失败，oldKey={}，newKey={}", oldKey, newKey, e);
            return false;
        }
    }

    /**
     * 新 key 不存在时修改 key 名称。
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return 是否成功
     */
    @Override
    public boolean renameKeyIfAbsent(String oldKey, String newKey) {
        checkKey(oldKey);
        checkKey(newKey);
        return redissonClient.getKeys().renamenx(oldKey, newKey);
    }

    /**
     * 查询匹配通配符的 key。
     *
     * @param pattern 通配符表达式
     * @return key 集合
     */
    @Override
    public Set<String> keys(String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return Collections.emptySet();
        }

        Set<String> result = new LinkedHashSet<>();
        Iterable<String> iterable = redissonClient.getKeys().getKeysByPattern(pattern);
        for (String key : iterable) {
            result.add(key);
        }
        return result;
    }

    /**
     * 查询匹配通配符的 key，并限制返回数量。
     *
     * @param pattern 通配符表达式
     * @param count   最大数量
     * @return key 集合
     */
    @Override
    public Set<String> scanKeys(String pattern, int count) {
        if (StrUtil.isBlank(pattern) || count <= 0) {
            return Collections.emptySet();
        }

        Set<String> result = new LinkedHashSet<>();
        Iterable<String> iterable = redissonClient.getKeys().getKeysByPattern(pattern);
        for (String key : iterable) {
            result.add(key);
            if (result.size() >= count) {
                break;
            }
        }
        return result;
    }

    /**
     * 判断 key 是否已过期或不存在。
     *
     * @param key Redis 键
     * @return 已过期或不存在返回 true
     */
    @Override
    public boolean isExpired(String key) {
        if (StrUtil.isBlank(key)) {
            return true;
        }

        long ttlMillis = redissonClient.getBucket(key).remainTimeToLive();
        return ttlMillis == -2 || ttlMillis == 0;
    }

    /**
     * 获取 key 类型。
     *
     * @param key Redis 键
     * @return 类型名称
     */
    @Override
    public String getKeyType(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        RType type = redissonClient.getKeys().getType(key);
        return ObjectUtil.isNull(type) ? null : type.name().toLowerCase();
    }

    // -------------------------------------------------------------------------
    // 类型转换
    // -------------------------------------------------------------------------

    /**
     * 将对象转换为指定类型。
     *
     * @param value 原始值
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 转换后的值
     */
    @Override
    public <T> T convertValue(Object value, Class<T> clazz) {
        // 原始值或目标类型为空时，无法进行有效转换，直接返回 null
        if (ObjectUtil.isNull(value) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        // 如果原始值本身已经是目标类型，直接强转返回，避免重复序列化和反序列化
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        try {
            // Redis 中常见的值可能是 JSON 字符串，这里优先按 JSON 字符串处理
            if (value instanceof String text) {
                // 目标类型就是 String 时，直接返回原字符串
                if (String.class.equals(clazz)) {
                    return clazz.cast(text);
                }

                // 目标类型不是 String 时，尝试将字符串按 JSON 反序列化为目标类型
                return objectMapper.readValue(text, clazz);
            }

            // 非字符串对象使用 Jackson 的 convertValue 进行类型转换
            // 适用于 LinkedHashMap -> JavaBean、Map -> DTO、基础类型转换等场景
            return objectMapper.convertValue(value, clazz);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            // 转换失败时不向外抛出异常，避免缓存值格式异常影响主流程
            // 这里记录目标类型和原始值类型，便于排查 Redis 中的数据结构问题
            log.warn("Redis 值类型转换失败，targetType={}，valueType={}",
                    clazz.getName(), value.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 将对象转换为指定泛型类型。
     *
     * @param value         原始值
     * @param typeReference 目标类型引用
     * @param <T>           泛型类型
     * @return 转换后的值
     */
    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeReference) {
        // 原始值或泛型类型引用为空时，无法进行有效转换，直接返回 null
        if (ObjectUtil.isNull(value) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        try {
            // Redis 中存储的泛型数据可能是 JSON 字符串，例如 List<User>、Map<String, User>
            if (value instanceof String text) {
                try {
                    // 优先将字符串按 JSON 反序列化为指定泛型类型
                    return objectMapper.readValue(text, typeReference);
                } catch (JsonProcessingException ignored) {
                    // 如果字符串不是合法 JSON，则退回到 convertValue
                    // 例如目标泛型实际可以接收 String、Object 等简单类型时，仍有机会转换成功
                    return objectMapper.convertValue(value, typeReference);
                }
            }

            // 非字符串对象使用 Jackson 的 convertValue 进行泛型转换
            // 适用于 LinkedHashMap -> 泛型 DTO、List<LinkedHashMap> -> List<DTO> 等场景
            return objectMapper.convertValue(value, typeReference);
        } catch (IllegalArgumentException e) {
            // 泛型转换失败通常是结构不匹配，例如字段类型不兼容、集合元素类型不一致等
            // 记录目标泛型类型和原始值类型，便于定位缓存数据问题
            log.warn("Redis 值泛型转换失败，targetType={}，valueType={}",
                    typeReference.getType(), value.getClass().getName(), e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // 字符串 / Bucket 操作
    // -------------------------------------------------------------------------

    /**
     * 设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     */
    @Override
    public void set(String key, Object value) {
        checkKey(key);
        redissonClient.getBucket(key).set(value);
    }

    /**
     * 设置缓存值并指定过期时间。
     *
     * @param key     Redis 键
     * @param value   缓存值
     * @param timeout 超时时间
     * @param unit    时间单位
     */
    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout > 0, "过期时间必须大于 0");

        redissonClient.getBucket(key).set(value, timeout, unit);
    }

    /**
     * 设置缓存值并指定过期时间。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     */
    @Override
    public void set(String key, Object value, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "过期时间不能为空且必须大于 0");

        redissonClient.getBucket(key).set(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 获取缓存值。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 缓存值
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        Object value = redissonClient.getBucket(key).get();
        return convertValue(value, clazz);
    }

    /**
     * 获取缓存值。
     *
     * @param key           Redis 键
     * @param typeReference 目标泛型类型
     * @param <T>           泛型类型
     * @return 缓存值
     */
    @Override
    public <T> T get(String key, TypeReference<T> typeReference) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        Object value = redissonClient.getBucket(key).get();
        return convertValue(value, typeReference);
    }

    /**
     * key 不存在时设置缓存值。
     *
     * @param key     Redis 键
     * @param value   缓存值
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    @Override
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout > 0, "过期时间必须大于 0");

        return redissonClient.getBucket(key).setIfAbsent(value, Duration.ofMillis(unit.toMillis(timeout)));
    }

    /**
     * key 不存在时设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     * @return 是否设置成功
     */
    @Override
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "过期时间不能为空且必须大于 0");

        return redissonClient.getBucket(key).setIfAbsent(value, ttl);
    }

    /**
     * key 存在时设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @return 是否设置成功
     */
    @Override
    public boolean setIfExists(String key, Object value) {
        checkKey(key);
        return redissonClient.getBucket(key).setIfExists(value);
    }

    /**
     * key 存在时设置缓存值并指定过期时间。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     * @return 是否设置成功
     */
    @Override
    public boolean setIfExists(String key, Object value, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "过期时间不能为空且必须大于 0");

        return redissonClient.getBucket(key).setIfExists(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 原子替换并返回旧值。
     *
     * @param key   Redis 键
     * @param value 新值
     * @param clazz 旧值类型
     * @param <T>   泛型类型
     * @return 旧值
     */
    @Override
    public <T> T getAndSet(String key, Object value, Class<T> clazz) {
        checkKey(key);

        Object oldValue = redissonClient.getBucket(key).getAndSet(value);
        return convertValue(oldValue, clazz);
    }

    /**
     * 原子替换并返回旧值。
     *
     * @param key           Redis 键
     * @param value         新值
     * @param typeReference 旧值类型
     * @param <T>           泛型类型
     * @return 旧值
     */
    @Override
    public <T> T getAndSet(String key, Object value, TypeReference<T> typeReference) {
        checkKey(key);

        Object oldValue = redissonClient.getBucket(key).getAndSet(value);
        return convertValue(oldValue, typeReference);
    }

    /**
     * 获取并删除缓存值。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 删除前的值
     */
    @Override
    public <T> T getAndDelete(String key, Class<T> clazz) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        Object value = redissonClient.getBucket(key).getAndDelete();
        return convertValue(value, clazz);
    }

    /**
     * 批量获取缓存值。
     *
     * @param keys Redis 键集合
     * @return 键值 Map
     */
    @Override
    public Map<String, Object> entries(Collection<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyMap();
        }

        String[] keyArray = keys.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(keyArray)) {
            return Collections.emptyMap();
        }

        return redissonClient.getBuckets().get(keyArray);
    }

    /**
     * 批量获取缓存值并转换类型。
     *
     * @param keys  Redis 键集合
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 键值 Map
     */
    @Override
    public <T> Map<String, T> entries(Collection<String> keys, Class<T> clazz) {
        if (CollUtil.isEmpty(keys) || ObjectUtil.isNull(clazz)) {
            return Collections.emptyMap();
        }

        Map<String, Object> rawMap = entries(keys);
        if (CollUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        rawMap.forEach((key, value) -> {
            T converted = convertValue(value, clazz);
            if (ObjectUtil.isNotNull(converted)) {
                result.put(key, converted);
            }
        });
        return result;
    }

    /**
     * 批量获取缓存值并转换泛型类型。
     *
     * @param keys          Redis 键集合
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 键值 Map
     */
    @Override
    public <T> Map<String, T> entries(Collection<String> keys, TypeReference<T> typeReference) {
        if (CollUtil.isEmpty(keys) || ObjectUtil.isNull(typeReference)) {
            return Collections.emptyMap();
        }

        Map<String, Object> rawMap = entries(keys);
        if (CollUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        rawMap.forEach((key, value) -> {
            T converted = convertValue(value, typeReference);
            if (ObjectUtil.isNotNull(converted)) {
                result.put(key, converted);
            }
        });
        return result;
    }

    /**
     * 获取序列化后的字节大小。
     *
     * @param key Redis 键
     * @return 字节大小
     */
    @Override
    public long size(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }
        return redissonClient.getBucket(key).size();
    }

    // -------------------------------------------------------------------------
    // 原子数值 / 计数器 / ID
    // -------------------------------------------------------------------------

    /**
     * 获取长整型原子对象。
     *
     * @param key Redis 键
     * @return RAtomicLong
     */
    @Override
    public RAtomicLong getAtomicLong(String key) {
        checkKey(key);
        return redissonClient.getAtomicLong(key);
    }

    /**
     * 获取浮点型原子对象。
     *
     * @param key Redis 键
     * @return RAtomicDouble
     */
    @Override
    public RAtomicDouble getAtomicDouble(String key) {
        checkKey(key);
        return redissonClient.getAtomicDouble(key);
    }

    /**
     * 整数自增。
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    @Override
    public long increment(String key, long delta) {
        checkKey(key);
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    /**
     * 整数自减。
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    @Override
    public long decrement(String key, long delta) {
        checkKey(key);
        Assert.isTrue(delta >= 0, "递减值不能小于 0");

        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    /**
     * 浮点数自增。
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    @Override
    public double incrementDouble(String key, double delta) {
        checkKey(key);
        return redissonClient.getAtomicDouble(key).addAndGet(delta);
    }

    /**
     * 浮点数自减。
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    @Override
    public double decrementDouble(String key, double delta) {
        checkKey(key);
        Assert.isTrue(delta >= 0, "递减值不能小于 0");

        return redissonClient.getAtomicDouble(key).addAndGet(-delta);
    }

    /**
     * 设置整数计数器值。
     *
     * @param key   Redis 键
     * @param value 值
     */
    @Override
    public void setAtomicLong(String key, long value) {
        checkKey(key);
        redissonClient.getAtomicLong(key).set(value);
    }

    /**
     * 获取整数计数器值。
     *
     * @param key Redis 键
     * @return 当前值
     */
    @Override
    public long getAtomicLongValue(String key) {
        checkKey(key);
        return redissonClient.getAtomicLong(key).get();
    }

    /**
     * 重置整数计数器。
     *
     * @param key Redis 键
     */
    @Override
    public void resetAtomicLong(String key) {
        checkKey(key);

        redissonClient.getAtomicLong(key).set(0L);
        log.info("Redis 计数器已重置，key={}", key);
    }

    /**
     * 获取分布式 ID 生成器。
     *
     * @param key Redis 键
     * @return RIdGenerator
     */
    @Override
    public RIdGenerator getIdGenerator(String key) {
        checkKey(key);
        return redissonClient.getIdGenerator(key);
    }

    /**
     * 初始化分布式 ID 生成器。
     *
     * @param key            Redis 键
     * @param initialValue   初始值
     * @param allocationSize 每次分配步长
     * @return 是否初始化成功
     */
    @Override
    public boolean idGeneratorInit(String key, long initialValue, long allocationSize) {
        checkKey(key);
        Assert.isTrue(allocationSize > 0, "ID 分配步长必须大于 0");

        boolean initialized = redissonClient.getIdGenerator(key).tryInit(initialValue, allocationSize);
        if (initialized) {
            log.info("Redis 分布式 ID 生成器初始化成功，key={}，initialValue={}，allocationSize={}",
                    key, initialValue, allocationSize);
        } else {
            log.debug("Redis 分布式 ID 生成器已存在，跳过初始化，key={}", key);
        }
        return initialized;
    }

    /**
     * 获取下一个分布式 ID。
     *
     * @param key Redis 键
     * @return ID
     */
    @Override
    public long nextId(String key) {
        checkKey(key);
        return redissonClient.getIdGenerator(key).nextId();
    }

    // -------------------------------------------------------------------------
    // Hash / MapCache 操作
    // -------------------------------------------------------------------------

    /**
     * 设置哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     */
    @Override
    public void hPut(String key, String field, Object value) {
        checkKey(key);
        checkField(field);

        redissonClient.<String, Object>getMap(key).fastPut(field, value);
    }

    /**
     * 批量设置哈希字段值。
     *
     * @param key Redis 键
     * @param map 字段 Map
     */
    @Override
    public void hPutAll(String key, Map<String, ?> map) {
        checkKey(key);
        if (CollUtil.isEmpty(map)) {
            return;
        }

        Map<String, Object> valueMap = new LinkedHashMap<>(map.size());
        map.forEach((field, value) -> {
            if (StrUtil.isNotBlank(field)) {
                valueMap.put(field, value);
            }
        });

        if (CollUtil.isNotEmpty(valueMap)) {
            redissonClient.<String, Object>getMap(key).putAll(valueMap);
        }
    }

    /**
     * 字段不存在时设置哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @return 是否设置成功
     */
    @Override
    public boolean hPutIfAbsent(String key, String field, Object value) {
        checkKey(key);
        checkField(field);

        return redissonClient.<String, Object>getMap(key).fastPutIfAbsent(field, value);
    }

    /**
     * 获取哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值
     */
    @Override
    public <T> T hGet(String key, String field, Class<T> clazz) {
        if (StrUtil.hasBlank(key, field) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        Object value = redissonClient.<String, Object>getMap(key).get(field);
        return convertValue(value, clazz);
    }

    /**
     * 获取哈希字段值。
     *
     * @param key           Redis 键
     * @param field         字段名
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 字段值
     */
    @Override
    public <T> T hGet(String key, String field, TypeReference<T> typeReference) {
        if (StrUtil.hasBlank(key, field) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        Object value = redissonClient.<String, Object>getMap(key).get(field);
        return convertValue(value, typeReference);
    }

    /**
     * 批量获取哈希字段值。
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 字段值 Map
     */
    @Override
    public Map<String, Object> hMultiGet(String key, Collection<String> fields) {
        if (StrUtil.isBlank(key) || CollUtil.isEmpty(fields)) {
            return Collections.emptyMap();
        }

        Set<String> fieldSet = fields.stream()
                .filter(StrUtil::isNotBlank)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (CollUtil.isEmpty(fieldSet)) {
            return Collections.emptyMap();
        }

        return redissonClient.<String, Object>getMap(key).getAll(fieldSet);
    }

    /**
     * 删除哈希字段。
     *
     * @param key    Redis 键
     * @param fields 字段名
     * @return 删除数量
     */
    @Override
    public long hDelete(String key, String... fields) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(fields)) {
            return 0L;
        }

        String[] fieldArray = Arrays.stream(fields)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(fieldArray)) {
            return 0L;
        }

        return redissonClient.<String, Object>getMap(key).fastRemove(fieldArray);
    }

    /**
     * 判断哈希字段是否存在。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 存在返回 true
     */
    @Override
    public boolean hHasKey(String key, String field) {
        if (StrUtil.hasBlank(key, field)) {
            return false;
        }

        return redissonClient.<String, Object>getMap(key).containsKey(field);
    }

    /**
     * 获取哈希全部字段和值。
     *
     * @param key Redis 键
     * @return 字段值 Map
     */
    @Override
    public Map<String, Object> hEntries(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        return redissonClient.<String, Object>getMap(key).readAllMap();
    }

    /**
     * 获取哈希全部字段和值并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值 Map
     */
    @Override
    public <T> Map<String, T> hEntries(String key, Class<T> clazz) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(clazz)) {
            return Collections.emptyMap();
        }

        Map<String, Object> rawMap = hEntries(key);
        if (CollUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        rawMap.forEach((field, value) -> {
            T converted = convertValue(value, clazz);
            if (ObjectUtil.isNotNull(converted)) {
                result.put(field, converted);
            }
        });
        return result;
    }

    /**
     * 获取哈希全部字段和值并转换泛型类型。
     *
     * @param key           Redis 键
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 字段值 Map
     */
    @Override
    public <T> Map<String, T> hEntries(String key, TypeReference<T> typeReference) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(typeReference)) {
            return Collections.emptyMap();
        }

        Map<String, Object> rawMap = hEntries(key);
        if (CollUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        rawMap.forEach((field, value) -> {
            T converted = convertValue(value, typeReference);
            if (ObjectUtil.isNotNull(converted)) {
                result.put(field, converted);
            }
        });
        return result;
    }

    /**
     * 获取哈希字段名集合。
     *
     * @param key Redis 键
     * @return 字段集合
     */
    @Override
    public Set<String> hKeys(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        return redissonClient.<String, Object>getMap(key).readAllKeySet();
    }

    /**
     * 获取哈希字段值集合。
     *
     * @param key Redis 键
     * @return 字段值集合
     */
    @Override
    public Collection<Object> hValues(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyList();
        }

        return redissonClient.<String, Object>getMap(key).readAllValues();
    }

    /**
     * 获取哈希字段数量。
     *
     * @param key Redis 键
     * @return 字段数量
     */
    @Override
    public int hSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.<String, Object>getMap(key).size();
    }

    /**
     * 哈希字段整数自增。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    @Override
    public long hIncrement(String key, String field, long delta) {
        checkKey(key);
        checkField(field);

        Number value = redissonClient.<String, Number>getMap(key).addAndGet(field, delta);
        return ObjectUtil.isNull(value) ? 0L : value.longValue();
    }

    /**
     * 哈希字段浮点数自增。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    @Override
    public double hIncrementDouble(String key, String field, double delta) {
        checkKey(key);
        checkField(field);

        Number value = redissonClient.<String, Number>getMap(key).addAndGet(field, delta);
        return ObjectUtil.isNull(value) ? 0D : value.doubleValue();
    }

    /**
     * 清空哈希。
     *
     * @param key Redis 键
     */
    @Override
    public void hClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.<String, Object>getMap(key).clear();
    }

    /**
     * 设置 MapCache 字段值并指定字段级 TTL。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @param ttl   字段 TTL
     */
    @Override
    public void hcPut(String key, String field, Object value, Duration ttl) {
        checkKey(key);
        checkField(field);
        checkPositiveDuration(ttl, "字段过期时间不能为空且必须大于 0");

        redissonClient.<String, Object>getMapCache(key)
                .fastPut(field, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 设置 MapCache 字段值并指定字段级 TTL 与最大空闲时间。
     *
     * @param key     Redis 键
     * @param field   字段名
     * @param value   字段值
     * @param ttl     字段 TTL
     * @param maxIdle 最大空闲时间
     */
    @Override
    public void hcPut(String key, String field, Object value, Duration ttl, Duration maxIdle) {
        checkKey(key);
        checkField(field);
        checkPositiveDuration(ttl, "字段过期时间不能为空且必须大于 0");
        checkPositiveDuration(maxIdle, "字段最大空闲时间不能为空且必须大于 0");

        redissonClient.<String, Object>getMapCache(key)
                .fastPut(
                        field,
                        value,
                        ttl.toMillis(),
                        TimeUnit.MILLISECONDS,
                        maxIdle.toMillis(),
                        TimeUnit.MILLISECONDS
                );
    }

    /**
     * 获取 MapCache 字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值
     */
    @Override
    public <T> T hcGet(String key, String field, Class<T> clazz) {
        if (StrUtil.hasBlank(key, field) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        Object value = redissonClient.<String, Object>getMapCache(key).get(field);
        return convertValue(value, clazz);
    }

    /**
     * 删除 MapCache 字段。
     *
     * @param key    Redis 键
     * @param fields 字段名
     * @return 删除数量
     */
    @Override
    public long hcDelete(String key, String... fields) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(fields)) {
            return 0L;
        }

        String[] fieldArray = Arrays.stream(fields)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(fieldArray)) {
            return 0L;
        }

        return redissonClient.<String, Object>getMapCache(key).fastRemove(fieldArray);
    }

    // -------------------------------------------------------------------------
    // List / Deque 操作
    // -------------------------------------------------------------------------

    /**
     * 左侧压入列表。
     *
     * @param key   Redis 键
     * @param value 元素
     */
    @Override
    public void lLeftPush(String key, Object value) {
        checkKey(key);

        redissonClient.<Object>getDeque(key).addFirst(value);
    }

    /**
     * 右侧压入列表。
     *
     * @param key   Redis 键
     * @param value 元素
     */
    @Override
    public void lRightPush(String key, Object value) {
        checkKey(key);

        redissonClient.<Object>getDeque(key).addLast(value);
    }

    /**
     * 批量右侧压入列表。
     *
     * @param key    Redis 键
     * @param values 元素集合
     */
    @Override
    public void lRightPushAll(String key, Collection<?> values) {
        checkKey(key);
        if (CollUtil.isEmpty(values)) {
            return;
        }

        redissonClient.<Object>getList(key).addAll(values);
    }

    /**
     * 左侧弹出列表元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    @Override
    public Object lLeftPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<Object>getDeque(key).pollFirst();
    }

    /**
     * 左侧弹出列表元素并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素
     */
    @Override
    public <T> T lLeftPop(String key, Class<T> clazz) {
        Object value = lLeftPop(key);
        return convertValue(value, clazz);
    }

    /**
     * 右侧弹出列表元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    @Override
    public Object lRightPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<Object>getDeque(key).pollLast();
    }

    /**
     * 阻塞式左侧弹出列表元素。
     *
     * @param key     Redis 键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param clazz   目标类型
     * @param <T>     泛型类型
     * @return 元素
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public <T> T lLeftPop(String key, long timeout, TimeUnit unit, Class<T> clazz) throws InterruptedException {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        Object value = redissonClient.<Object>getBlockingDeque(key).pollFirst(timeout, unit);
        return convertValue(value, clazz);
    }

    /**
     * 获取列表范围。
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素集合
     */
    @Override
    public List<Object> lRange(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyList();
        }

        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        if (size <= 0) {
            return Collections.emptyList();
        }

        int fromIndex = normalizeIndex(start, size);
        int toIndex = normalizeIndex(end, size);

        fromIndex = Math.max(fromIndex, 0);
        toIndex = Math.min(toIndex, size - 1);

        if (fromIndex > toIndex) {
            return Collections.emptyList();
        }

        return new ArrayList<>(list.subList(fromIndex, toIndex + 1));
    }

    /**
     * 获取列表范围并转换类型。
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素集合
     */
    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        if (ObjectUtil.isNull(clazz)) {
            return Collections.emptyList();
        }

        List<Object> rawList = lRange(key, start, end);
        if (CollUtil.isEmpty(rawList)) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            T converted = convertValue(item, clazz);
            if (ObjectUtil.isNotNull(converted)) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * 获取列表长度。
     *
     * @param key Redis 键
     * @return 长度
     */
    @Override
    public long lSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getList(key).size();
    }

    /**
     * 删除列表元素。
     *
     * @param key   Redis 键
     * @param count 删除数量规则
     * @param value 元素
     * @return 删除数量
     */
    @Override
    public long lRemove(String key, long count, Object value) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        RList<Object> list = redissonClient.getList(key);
        if (list.isEmpty()) {
            return 0L;
        }

        if (count == 0) {
            long removed = 0L;
            while (list.remove(value)) {
                removed++;
            }
            return removed;
        }

        List<Object> copy = new ArrayList<>(list);
        long target = Math.abs(count);
        long removed = 0L;

        if (count > 0) {
            for (int i = 0; i < copy.size() && removed < target; i++) {
                if (ObjectUtil.equal(copy.get(i), value)) {
                    copy.remove(i--);
                    removed++;
                }
            }
        } else {
            for (int i = copy.size() - 1; i >= 0 && removed < target; i--) {
                if (ObjectUtil.equal(copy.get(i), value)) {
                    copy.remove(i);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            list.clear();
            list.addAll(copy);
        }

        return removed;
    }

    /**
     * 获取列表指定索引元素。
     *
     * @param key   Redis 键
     * @param index 索引
     * @return 元素
     */
    @Override
    public Object lIndex(String key, long index) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        if (size <= 0) {
            return null;
        }

        int realIndex = normalizeIndex(index, size);
        if (realIndex < 0 || realIndex >= size) {
            return null;
        }

        return list.get(realIndex);
    }

    /**
     * 获取列表指定索引元素并转换类型。
     *
     * @param key   Redis 键
     * @param index 索引
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素
     */
    @Override
    public <T> T lIndex(String key, long index, Class<T> clazz) {
        Object value = lIndex(key, index);
        return convertValue(value, clazz);
    }

    /**
     * 设置列表指定索引元素。
     *
     * @param key   Redis 键
     * @param index 索引
     * @param value 元素
     */
    @Override
    public void lSet(String key, long index, Object value) {
        checkKey(key);

        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        int realIndex = normalizeIndex(index, size);

        Assert.isTrue(realIndex >= 0 && realIndex < size, "列表索引超出范围");
        list.set(realIndex, value);
    }

    /**
     * 裁剪列表范围。
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     */
    @Override
    public void lTrim(String key, int start, int end) {
        checkKey(key);

        List<Object> range = lRange(key, start, end);
        RList<Object> list = redissonClient.getList(key);

        list.clear();
        if (CollUtil.isNotEmpty(range)) {
            list.addAll(range);
        }
    }

    /**
     * 清空列表。
     *
     * @param key Redis 键
     */
    @Override
    public void lClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.<Object>getList(key).clear();
    }

    // -------------------------------------------------------------------------
    // Set / SetCache 操作
    // -------------------------------------------------------------------------

    /**
     * 添加集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否有新增
     */
    @Override
    public boolean sAdd(String key, Object... values) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getSet(key).addAll(Arrays.asList(values));
    }

    /**
     * 添加集合元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @Override
    public boolean sAdd(String key, Collection<?> values) {
        if (StrUtil.isBlank(key) || CollUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getSet(key).addAll(values);
    }

    /**
     * 添加 SetCache 元素并指定元素 TTL。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param ttl   TTL
     * @return 是否添加成功
     */
    @Override
    public boolean scAdd(String key, Object value, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "元素过期时间不能为空且必须大于 0");

        return redissonClient.<Object>getSetCache(key).add(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 判断集合中是否存在元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 存在返回 true
     */
    @Override
    public boolean sIsMember(String key, Object value) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.<Object>getSet(key).contains(value);
    }

    /**
     * 获取集合所有元素。
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @Override
    public Set<Object> sMembers(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key).readAll();
    }

    /**
     * 获取集合所有元素并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素集合
     */
    @Override
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        if (ObjectUtil.isNull(clazz)) {
            return Collections.emptySet();
        }

        return convertSet(sMembers(key), clazz);
    }

    /**
     * 获取集合大小。
     *
     * @param key Redis 键
     * @return 大小
     */
    @Override
    public long sSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getSet(key).size();
    }

    /**
     * 随机弹出集合元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    @Override
    public Object sPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<Object>getSet(key).removeRandom();
    }

    /**
     * 删除集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    @Override
    public boolean sRemove(String key, Object... values) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getSet(key).removeAll(Arrays.asList(values));
    }

    /**
     * 随机获取集合元素但不删除。
     *
     * @param key Redis 键
     * @return 元素
     */
    @Override
    public Object sRandomMember(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<Object>getSet(key).random();
    }

    /**
     * 随机获取多个集合元素。
     *
     * @param key   Redis 键
     * @param count 数量
     * @return 元素集合
     */
    @Override
    public Set<Object> sRandomMembers(String key, int count) {
        if (StrUtil.isBlank(key) || count <= 0) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key).random(count);
    }

    /**
     * 获取并集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 并集
     */
    @Override
    public Set<Object> sUnion(String key1, String key2) {
        if (StrUtil.hasBlank(key1, key2)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key1).readUnion(key2);
    }

    /**
     * 获取交集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 交集
     */
    @Override
    public Set<Object> sIntersect(String key1, String key2) {
        if (StrUtil.hasBlank(key1, key2)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key1).readIntersection(key2);
    }

    /**
     * 获取差集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 差集
     */
    @Override
    public Set<Object> sDifference(String key1, String key2) {
        if (StrUtil.hasBlank(key1, key2)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key1).readDiff(key2);
    }

    /**
     * 将集合并集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    @Override
    public long sUnionStore(String destKey, String... keys) {
        checkKey(destKey);

        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }

        Set<Object> result = redissonClient.<Object>getSet(keyArray[0])
                .readUnion(Arrays.copyOfRange(keyArray, 1, keyArray.length));

        RSet<Object> destSet = redissonClient.getSet(destKey);
        destSet.clear();
        if (CollUtil.isNotEmpty(result)) {
            destSet.addAll(result);
        }

        return result.size();
    }

    /**
     * 将集合交集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    @Override
    public long sIntersectStore(String destKey, String... keys) {
        checkKey(destKey);

        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }

        Set<Object> result = redissonClient.<Object>getSet(keyArray[0])
                .readIntersection(Arrays.copyOfRange(keyArray, 1, keyArray.length));

        RSet<Object> destSet = redissonClient.getSet(destKey);
        destSet.clear();
        if (CollUtil.isNotEmpty(result)) {
            destSet.addAll(result);
        }

        return result.size();
    }

    /**
     * 将集合差集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    @Override
    public long sDifferenceStore(String destKey, String... keys) {
        checkKey(destKey);

        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }

        Set<Object> result = redissonClient.<Object>getSet(keyArray[0])
                .readDiff(Arrays.copyOfRange(keyArray, 1, keyArray.length));

        RSet<Object> destSet = redissonClient.getSet(destKey);
        destSet.clear();
        if (CollUtil.isNotEmpty(result)) {
            destSet.addAll(result);
        }

        return result.size();
    }

    // -------------------------------------------------------------------------
    // ZSet / 排行榜操作
    // -------------------------------------------------------------------------

    /**
     * 添加有序集合元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param score 分数
     * @return 是否新增
     */
    @Override
    public boolean zAdd(String key, Object value, double score) {
        checkKey(key);

        return redissonClient.<Object>getScoredSortedSet(key).add(score, value);
    }

    /**
     * 批量添加有序集合元素。
     *
     * @param key      Redis 键
     * @param scoreMap 元素分数 Map
     * @return 新增数量
     */
    @Override
    public int zAddAll(String key, Map<Object, Double> scoreMap) {
        checkKey(key);
        if (CollUtil.isEmpty(scoreMap)) {
            return 0;
        }

        return redissonClient.<Object>getScoredSortedSet(key).addAll(scoreMap);
    }

    /**
     * 删除有序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    @Override
    public boolean zRemove(String key, Object... values) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getScoredSortedSet(key).removeAll(Arrays.asList(values));
    }

    /**
     * 获取元素分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 分数
     */
    @Override
    public Double zScore(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }

        return redissonClient.<Object>getScoredSortedSet(key).getScore(value);
    }

    /**
     * 获取升序排名，从 0 开始。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    @Override
    public Integer zRank(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }

        return redissonClient.<Object>getScoredSortedSet(key).rank(value);
    }

    /**
     * 获取降序排名，从 0 开始。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    @Override
    public Integer zRevRank(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }

        return redissonClient.<Object>getScoredSortedSet(key).revRank(value);
    }

    /**
     * 获取分数区间元素。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    @Override
    public Set<Object> zRangeByScore(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        Collection<Object> values = redissonClient.<Object>getScoredSortedSet(key)
                .valueRange(min, true, max, true);
        return new LinkedHashSet<>(values);
    }

    /**
     * 获取分数区间元素并分页。
     *
     * @param key    Redis 键
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量
     * @param count  数量
     * @return 元素集合
     */
    @Override
    public Collection<Object> zRangeByScore(String key, double min, double max, int offset, int count) {
        if (StrUtil.isBlank(key) || offset < 0 || count <= 0) {
            return Collections.emptyList();
        }

        return redissonClient.<Object>getScoredSortedSet(key)
                .valueRange(min, true, max, true, offset, count);
    }

    /**
     * 获取分数区间元素及分数。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    @Override
    public Map<Object, Double> zRangeByScoreWithScores(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        Collection<ScoredEntry<Object>> entries = redissonClient.<Object>getScoredSortedSet(key)
                .entryRange(min, true, max, true);
        return scoredEntriesToMap(entries);
    }

    /**
     * 获取降序分数区间元素及分数。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    @Override
    public Map<Object, Double> zRevRangeByScoreWithScores(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        List<ScoredEntry<Object>> entries = new ArrayList<>(
                redissonClient.<Object>getScoredSortedSet(key).entryRange(min, true, max, true)
        );
        Collections.reverse(entries);

        return scoredEntriesToMap(entries);
    }

    /**
     * 获取排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    @Override
    public Set<Object> zRange(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        Collection<Object> values = redissonClient.<Object>getScoredSortedSet(key).valueRange(start, end);
        return new LinkedHashSet<>(values);
    }

    /**
     * 获取排名区间元素及分数。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    @Override
    public Map<Object, Double> zRangeWithScores(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        Collection<ScoredEntry<Object>> entries = redissonClient.<Object>getScoredSortedSet(key)
                .entryRange(start, end);
        return scoredEntriesToMap(entries);
    }

    /**
     * 获取降序排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    @Override
    public Set<Object> zRevRange(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        Collection<Object> values = redissonClient.<Object>getScoredSortedSet(key)
                .valueRangeReversed(start, end);
        return new LinkedHashSet<>(values);
    }

    /**
     * 获取降序排名区间元素及分数。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    @Override
    public Map<Object, Double> zRevRangeWithScores(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        Collection<ScoredEntry<Object>> entries = redissonClient.<Object>getScoredSortedSet(key)
                .entryRangeReversed(start, end);
        return scoredEntriesToMap(entries);
    }

    /**
     * 增加元素分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param delta 分数增量
     * @return 最新分数
     */
    @Override
    public Double zIncrBy(String key, Object value, double delta) {
        checkKey(key);
        Assert.notNull(value, "有序集合元素不能为空");

        return redissonClient.<Object>getScoredSortedSet(key).addScore(value, delta);
    }

    /**
     * 获取有序集合元素数量。
     *
     * @param key Redis 键
     * @return 数量
     */
    @Override
    public int zCard(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.<Object>getScoredSortedSet(key).size();
    }

    /**
     * 获取分数区间元素数量。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 数量
     */
    @Override
    public long zCount(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getScoredSortedSet(key).count(min, true, max, true);
    }

    /**
     * 删除分数区间元素。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 删除数量
     */
    @Override
    public long zRemoveRangeByScore(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getScoredSortedSet(key)
                .removeRangeByScore(min, true, max, true);
    }

    /**
     * 删除排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 删除数量
     */
    @Override
    public long zRemoveRangeByRank(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getScoredSortedSet(key).removeRangeByRank(start, end);
    }

    /**
     * 弹出分数最小的元素。
     * 注意：当前实现是先查再删，不是严格原子弹出；如需高并发严格原子语义，后续可改为 Lua 或 Redisson 原生弹出 API。
     *
     * @param key Redis 键
     * @return 元素及分数
     */
    @Override
    public ScoredEntry<Object> zPopFirst(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        RScoredSortedSet<Object> zSet = redissonClient.getScoredSortedSet(key);
        Collection<ScoredEntry<Object>> entries = zSet.entryRange(0, 0);
        if (CollUtil.isEmpty(entries)) {
            return null;
        }

        ScoredEntry<Object> entry = entries.iterator().next();
        zSet.remove(entry.getValue());
        return entry;
    }

    /**
     * 弹出分数最大的元素。
     * 注意：当前实现是先查再删，不是严格原子弹出；如需高并发严格原子语义，后续可改为 Lua 或 Redisson 原生弹出 API。
     *
     * @param key Redis 键
     * @return 元素及分数
     */
    @Override
    public ScoredEntry<Object> zPopLast(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        RScoredSortedSet<Object> zSet = redissonClient.getScoredSortedSet(key);
        Collection<ScoredEntry<Object>> entries = zSet.entryRangeReversed(0, 0);
        if (CollUtil.isEmpty(entries)) {
            return null;
        }

        ScoredEntry<Object> entry = entries.iterator().next();
        zSet.remove(entry.getValue());
        return entry;
    }

    // -------------------------------------------------------------------------
    // 分布式锁与同步器
    // -------------------------------------------------------------------------

    /**
     * 获取可重入锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    @Override
    public RLock getLock(String lockKey) {
        checkKey(lockKey);
        return redissonClient.getLock(lockKey);
    }

    /**
     * 获取公平锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    @Override
    public RLock getFairLock(String lockKey) {
        checkKey(lockKey);
        return redissonClient.getFairLock(lockKey);
    }

    /**
     * 获取自旋锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    @Override
    public RLock getSpinLock(String lockKey) {
        checkKey(lockKey);
        return redissonClient.getSpinLock(lockKey);
    }

    /**
     * 获取联锁。
     *
     * @param locks 多个锁
     * @return RLock
     */
    @Override
    public RLock getMultiLock(RLock... locks) {
        Assert.isTrue(ArrayUtil.isNotEmpty(locks), "联锁对象不能为空");
        for (RLock lock : locks) {
            Assert.notNull(lock, "联锁对象不能包含空锁");
        }
        return redissonClient.getMultiLock(locks);
    }

    /**
     * 阻塞加锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void lock(String lockKey) {
        RLock lock = getLock(lockKey);
        lock.lock();
    }

    /**
     * 阻塞加锁并指定自动释放时间。
     *
     * @param lockKey   锁 key
     * @param leaseTime 持有时间
     * @param unit      时间单位
     */
    @Override
    public void lock(String lockKey, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        Assert.notNull(unit, "时间单位不能为空");

        if (leaseTime <= 0) {
            lock.lock();
            return;
        }

        lock.lock(leaseTime, unit);
    }

    /**
     * 尝试获取锁。
     *
     * @param lockKey   锁 key
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return 是否获取成功
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = getLock(lockKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        if (leaseTime <= 0) {
            return lock.tryLock(waitTime, unit);
        }

        return lock.tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 释放锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void unlock(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return;
        }

        RLock lock = redissonClient.getLock(lockKey);
        if (!lock.isHeldByCurrentThread()) {
            log.warn("当前线程未持有分布式锁，跳过释放，lockKey={}", lockKey);
            return;
        }

        try {
            lock.unlock();
        } catch (Exception e) {
            log.error("释放分布式锁异常，lockKey={}", lockKey, e);
        }
    }

    /**
     * 执行带锁任务。
     *
     * @param lockKey 锁 key
     * @param task    任务
     */
    @Override
    public void executeWithLock(String lockKey, Runnable task) {
        Assert.notNull(task, "锁内任务不能为空");

        RLock lock = getLock(lockKey);
        boolean locked = false;

        try {
            lock.lock();
            locked = true;
            task.run();
        } finally {
            unlockSafely(lockKey, lock, locked);
        }
    }

    /**
     * 执行带锁任务并返回结果。
     *
     * @param lockKey  锁 key
     * @param supplier 任务
     * @param <T>      返回类型
     * @return 任务结果
     */
    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        Assert.notNull(supplier, "锁内任务不能为空");

        RLock lock = getLock(lockKey);
        boolean locked = false;

        try {
            lock.lock();
            locked = true;
            return supplier.get();
        } finally {
            unlockSafely(lockKey, lock, locked);
        }
    }

    /**
     * 尝试执行带锁任务。
     *
     * @param lockKey   锁 key
     * @param waitTime  等待时间
     * @param leaseTime 持有时间，-1 表示使用 watchdog
     * @param unit      时间单位
     * @param task      任务
     * @return 是否执行成功
     */
    @Override
    public boolean tryExecuteWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable task) {
        Assert.notNull(task, "锁内任务不能为空");
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        RLock lock = getLock(lockKey);
        boolean locked = false;

        try {
            locked = leaseTime <= 0
                    ? lock.tryLock(waitTime, unit)
                    : lock.tryLock(waitTime, leaseTime, unit);

            if (!locked) {
                log.warn("获取分布式锁失败，lockKey={}", lockKey);
                return false;
            }

            task.run();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，lockKey={}", lockKey, e);
            return false;
        } finally {
            unlockSafely(lockKey, lock, locked);
        }
    }

    /**
     * 判断当前线程是否持有锁。
     *
     * @param lockKey 锁 key
     * @return 当前线程持有返回 true
     */
    @Override
    public boolean isHeldByCurrentThread(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return false;
        }

        return redissonClient.getLock(lockKey).isHeldByCurrentThread();
    }

    /**
     * 判断锁是否被任意线程持有。
     *
     * @param lockKey 锁 key
     * @return 已加锁返回 true
     */
    @Override
    public boolean isLocked(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return false;
        }

        return redissonClient.getLock(lockKey).isLocked();
    }

    /**
     * 获取读写锁。
     *
     * @param lockKey 锁 key
     * @return RReadWriteLock
     */
    @Override
    public RReadWriteLock getReadWriteLock(String lockKey) {
        checkKey(lockKey);
        return redissonClient.getReadWriteLock(lockKey);
    }

    /**
     * 获取读锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void readLock(String lockKey) {
        getReadWriteLock(lockKey).readLock().lock();
    }

    /**
     * 获取写锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void writeLock(String lockKey) {
        getReadWriteLock(lockKey).writeLock().lock();
    }

    /**
     * 尝试获取读锁。
     *
     * @param lockKey   锁 key
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return 是否成功
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = getReadWriteLock(lockKey).readLock();
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        if (leaseTime <= 0) {
            return lock.tryLock(waitTime, unit);
        }

        return lock.tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 尝试获取写锁。
     *
     * @param lockKey   锁 key
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return 是否成功
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = getReadWriteLock(lockKey).writeLock();
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        if (leaseTime <= 0) {
            return lock.tryLock(waitTime, unit);
        }

        return lock.tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 释放读锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void unlockRead(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return;
        }

        RLock lock = redissonClient.getReadWriteLock(lockKey).readLock();
        unlockSafely(lockKey + ":read", lock, true);
    }

    /**
     * 释放写锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void unlockWrite(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return;
        }

        RLock lock = redissonClient.getReadWriteLock(lockKey).writeLock();
        unlockSafely(lockKey + ":write", lock, true);
    }

    /**
     * 获取闭锁。
     *
     * @param latchKey 闭锁 key
     * @return RCountDownLatch
     */
    @Override
    public RCountDownLatch getCountDownLatch(String latchKey) {
        checkKey(latchKey);
        return redissonClient.getCountDownLatch(latchKey);
    }

    /**
     * 设置闭锁计数。
     *
     * @param latchKey 闭锁 key
     * @param count    计数
     */
    @Override
    public void setCount(String latchKey, int count) {
        checkKey(latchKey);
        Assert.isTrue(count > 0, "闭锁计数必须大于 0");

        boolean success = redissonClient.getCountDownLatch(latchKey).trySetCount(count);
        if (!success) {
            log.warn("设置闭锁计数失败，可能闭锁已存在，latchKey={}，count={}", latchKey, count);
        }
    }

    /**
     * 闭锁计数减一。
     *
     * @param latchKey 闭锁 key
     */
    @Override
    public void countDown(String latchKey) {
        checkKey(latchKey);
        redissonClient.getCountDownLatch(latchKey).countDown();
    }

    /**
     * 等待闭锁完成。
     *
     * @param latchKey 闭锁 key
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public void await(String latchKey) throws InterruptedException {
        checkKey(latchKey);
        redissonClient.getCountDownLatch(latchKey).await();
    }

    /**
     * 等待闭锁完成。
     *
     * @param latchKey 闭锁 key
     * @param timeout  超时时间
     * @param unit     时间单位
     * @return 是否完成
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public boolean await(String latchKey, long timeout, TimeUnit unit) throws InterruptedException {
        checkKey(latchKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        return redissonClient.getCountDownLatch(latchKey).await(timeout, unit);
    }

    /**
     * 获取信号量。
     *
     * @param semaphoreKey 信号量 key
     * @return RSemaphore
     */
    @Override
    public RSemaphore getSemaphore(String semaphoreKey) {
        checkKey(semaphoreKey);
        return redissonClient.getSemaphore(semaphoreKey);
    }

    /**
     * 初始化信号量许可数量。
     *
     * @param semaphoreKey 信号量 key
     * @param permits      许可数量
     */
    @Override
    public void trySetPermits(String semaphoreKey, int permits) {
        checkKey(semaphoreKey);
        Assert.isTrue(permits > 0, "信号量许可数量必须大于 0");

        boolean success = redissonClient.getSemaphore(semaphoreKey).trySetPermits(permits);
        if (!success) {
            log.warn("初始化信号量许可失败，可能信号量已存在，semaphoreKey={}，permits={}", semaphoreKey, permits);
        }
    }

    /**
     * 获取一个信号量许可。
     *
     * @param semaphoreKey 信号量 key
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public void acquire(String semaphoreKey) throws InterruptedException {
        checkKey(semaphoreKey);
        redissonClient.getSemaphore(semaphoreKey).acquire();
    }

    /**
     * 尝试获取信号量许可。
     *
     * @param semaphoreKey 信号量 key
     * @param permits      许可数量
     * @param waitTime     等待时间
     * @param unit         时间单位
     * @return 是否获取成功
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public boolean tryAcquire(String semaphoreKey, int permits, long waitTime, TimeUnit unit) throws InterruptedException {
        checkKey(semaphoreKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(permits > 0, "信号量许可数量必须大于 0");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        return redissonClient.getSemaphore(semaphoreKey).tryAcquire(permits, waitTime, unit);
    }

    /**
     * 释放信号量许可。
     *
     * @param semaphoreKey 信号量 key
     */
    @Override
    public void release(String semaphoreKey) {
        checkKey(semaphoreKey);
        redissonClient.getSemaphore(semaphoreKey).release();
    }

    /**
     * 获取可用许可数量。
     *
     * @param semaphoreKey 信号量 key
     * @return 可用许可数量
     */
    @Override
    public int availablePermits(String semaphoreKey) {
        checkKey(semaphoreKey);
        return redissonClient.getSemaphore(semaphoreKey).availablePermits();
    }

    /**
     * 获取可过期信号量。
     *
     * @param semaphoreKey 信号量 key
     * @return RPermitExpirableSemaphore
     */
    @Override
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String semaphoreKey) {
        checkKey(semaphoreKey);
        return redissonClient.getPermitExpirableSemaphore(semaphoreKey);
    }

    // -------------------------------------------------------------------------
    // 限流器
    // -------------------------------------------------------------------------

    /**
     * 初始化限流器。
     *
     * @param key      限流器 key
     * @param rateType 限流类型
     * @param rate     令牌数
     * @param interval 间隔
     * @param unit     间隔单位
     * @return 是否初始化成功
     */
    @Override
    public boolean rateLimiterInit(String key, RateType rateType, long rate, long interval, RateIntervalUnit unit) {
        checkKey(key);
        Assert.notNull(rateType, "限流类型不能为空");
        Assert.notNull(unit, "限流时间单位不能为空");
        Assert.isTrue(rate > 0, "令牌数必须大于 0");
        Assert.isTrue(interval > 0, "限流间隔必须大于 0");

        boolean initialized = redissonClient.getRateLimiter(key)
                .trySetRate(rateType, rate, interval, unit);

        if (initialized) {
            log.info("Redis 限流器初始化成功，key={}，rate={}，interval={}，unit={}", key, rate, interval, unit);
        } else {
            log.debug("Redis 限流器已存在，跳过初始化，key={}", key);
        }

        return initialized;
    }

    /**
     * 更新限流器速率。
     *
     * @param key      限流器 key
     * @param rateType 限流类型
     * @param rate     令牌数
     * @param interval 间隔
     * @param unit     间隔单位
     */
    @Override
    public void rateLimiterSetRate(String key, RateType rateType, long rate, long interval, RateIntervalUnit unit) {
        checkKey(key);
        Assert.notNull(rateType, "限流类型不能为空");
        Assert.notNull(unit, "限流时间单位不能为空");
        Assert.isTrue(rate > 0, "令牌数必须大于 0");
        Assert.isTrue(interval > 0, "限流间隔必须大于 0");

        redissonClient.getRateLimiter(key).setRate(rateType, rate, interval, unit);
        log.info("Redis 限流器速率已更新，key={}，rate={}，interval={}，unit={}", key, rate, interval, unit);
    }

    /**
     * 尝试获取一个令牌。
     *
     * @param key 限流器 key
     * @return 是否获取成功
     */
    @Override
    public boolean rateLimiterTryAcquire(String key) {
        checkKey(key);
        return redissonClient.getRateLimiter(key).tryAcquire();
    }

    /**
     * 尝试获取指定数量令牌。
     *
     * @param key     限流器 key
     * @param permits 令牌数量
     * @return 是否获取成功
     */
    @Override
    public boolean rateLimiterTryAcquire(String key, long permits) {
        checkKey(key);
        Assert.isTrue(permits > 0, "令牌数量必须大于 0");

        return redissonClient.getRateLimiter(key).tryAcquire(permits);
    }

    /**
     * 阻塞获取指定数量令牌。
     *
     * @param key     限流器 key
     * @param permits 令牌数量
     */
    @Override
    public void rateLimiterAcquire(String key, long permits) {
        checkKey(key);
        Assert.isTrue(permits > 0, "令牌数量必须大于 0");

        redissonClient.getRateLimiter(key).acquire(permits);
    }

    /**
     * 尝试等待获取令牌。
     *
     * @param key     限流器 key
     * @param timeout 等待时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    @Override
    public boolean rateLimiterTryAcquire(String key, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        return redissonClient.getRateLimiter(key).tryAcquire(timeout, unit);
    }

    /**
     * 获取限流器对象。
     *
     * @param key 限流器 key
     * @return RRateLimiter
     */
    @Override
    public RRateLimiter rateLimiterGet(String key) {
        checkKey(key);
        return redissonClient.getRateLimiter(key);
    }

    /**
     * 删除限流器。
     *
     * @param key 限流器 key
     * @return 是否删除成功
     */
    @Override
    public boolean rateLimiterDelete(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.getRateLimiter(key).delete();
    }

    // -------------------------------------------------------------------------
    // 布隆过滤器
    // -------------------------------------------------------------------------

    /**
     * 获取布隆过滤器。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RBloomFilter
     */
    @Override
    public <T> RBloomFilter<T> getBloomFilter(String key) {
        checkKey(key);
        return redissonClient.getBloomFilter(key);
    }

    /**
     * 初始化布隆过滤器。
     *
     * @param key                Redis 键
     * @param expectedInsertions 预计插入量
     * @param falseProbability   误判率
     */
    @Override
    public void bloomInit(String key, long expectedInsertions, double falseProbability) {
        checkKey(key);
        Assert.isTrue(expectedInsertions > 0, "预计插入量必须大于 0");
        Assert.isTrue(falseProbability > 0 && falseProbability < 1, "误判率必须在 0 到 1 之间");

        boolean initialized = redissonClient.getBloomFilter(key)
                .tryInit(expectedInsertions, falseProbability);

        if (initialized) {
            log.info("Redis 布隆过滤器初始化成功，key={}，expectedInsertions={}，falseProbability={}",
                    key, expectedInsertions, falseProbability);
        } else {
            log.debug("Redis 布隆过滤器已存在，跳过初始化，key={}", key);
        }
    }

    /**
     * 判断布隆过滤器是否可能包含元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 可能包含返回 true
     */
    @Override
    public boolean bloomContains(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return false;
        }

        return redissonClient.getBloomFilter(key).contains(value);
    }

    /**
     * 添加布隆过滤器元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    @Override
    public boolean bloomAdd(String key, Object value) {
        checkKey(key);
        Assert.notNull(value, "布隆过滤器元素不能为空");

        return redissonClient.getBloomFilter(key).add(value);
    }

    /**
     * 批量添加布隆过滤器元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 新增数量
     */
    @Override
    public long bloomAddAll(String key, Collection<?> values) {
        checkKey(key);
        if (CollUtil.isEmpty(values)) {
            return 0L;
        }

        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        long added = 0L;

        for (Object value : values) {
            if (ObjectUtil.isNotNull(value) && bloomFilter.add(value)) {
                added++;
            }
        }

        return added;
    }

    /**
     * 删除布隆过滤器。
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @Override
    public boolean bloomDelete(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.getBloomFilter(key).delete();
    }

    /**
     * 判断布隆过滤器是否存在。
     *
     * @param key Redis 键
     * @return 存在返回 true
     */
    @Override
    public boolean bloomExists(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.getBloomFilter(key).isExists();
    }

    /**
     * 获取预计插入量。
     *
     * @param key Redis 键
     * @return 预计插入量
     */
    @Override
    public long bloomGetExpectedInsertions(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        if (!bloomFilter.isExists()) {
            return 0L;
        }

        return bloomFilter.getExpectedInsertions();
    }

    /**
     * 获取误判率。
     *
     * @param key Redis 键
     * @return 误判率
     */
    @Override
    public double bloomGetFalseProbability(String key) {
        if (StrUtil.isBlank(key)) {
            return 0D;
        }

        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        if (!bloomFilter.isExists()) {
            return 0D;
        }

        return bloomFilter.getFalseProbability();
    }

    // -------------------------------------------------------------------------
    // BitSet / 签到 / 位图统计
    // -------------------------------------------------------------------------

    /**
     * 设置位图指定位置。
     *
     * @param key   Redis 键
     * @param index 位索引
     * @param value 位值
     */
    @Override
    public void bitSet(String key, long index, boolean value) {
        checkKey(key);
        Assert.isTrue(index >= 0, "位图索引不能小于 0");

        redissonClient.getBitSet(key).set(index, value);
    }

    /**
     * 获取位图指定位置。
     *
     * @param key   Redis 键
     * @param index 位索引
     * @return 位值
     */
    @Override
    public boolean bitGet(String key, long index) {
        if (StrUtil.isBlank(key) || index < 0) {
            return false;
        }

        return redissonClient.getBitSet(key).get(index);
    }

    /**
     * 获取位图中 true 的数量。
     *
     * @param key Redis 键
     * @return 数量
     */
    @Override
    public long bitCount(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.getBitSet(key).cardinality();
    }

    /**
     * 清空位图。
     *
     * @param key Redis 键
     */
    @Override
    public void bitClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getBitSet(key).clear();
    }

    /**
     * 用户指定日期签到。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     */
    @Override
    public void sign(String keyPrefix, Object userId, LocalDate date) {
        checkKey(keyPrefix);
        Assert.notNull(userId, "用户 ID 不能为空");
        Assert.notNull(date, "签到日期不能为空");

        String key = buildSignKey(keyPrefix, userId, date.getYear());
        int bitIndex = date.getDayOfYear() - 1;

        redissonClient.getBitSet(key).set(bitIndex, true);
        log.info("用户签到成功，key={}，userId={}，date={}", key, userId, date);
    }

    /**
     * 判断用户指定日期是否签到。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     * @return 已签到返回 true
     */
    @Override
    public boolean isSigned(String keyPrefix, Object userId, LocalDate date) {
        if (StrUtil.isBlank(keyPrefix) || ObjectUtil.isNull(userId) || ObjectUtil.isNull(date)) {
            return false;
        }

        String key = buildSignKey(keyPrefix, userId, date.getYear());
        int bitIndex = date.getDayOfYear() - 1;

        return redissonClient.getBitSet(key).get(bitIndex);
    }

    /**
     * 获取用户指定年份签到天数。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param year      年份
     * @return 签到天数
     */
    @Override
    public long getSignCount(String keyPrefix, Object userId, int year) {
        if (StrUtil.isBlank(keyPrefix) || ObjectUtil.isNull(userId) || year <= 0) {
            return 0L;
        }

        String key = buildSignKey(keyPrefix, userId, year);
        return redissonClient.getBitSet(key).cardinality();
    }

    /**
     * 获取从指定日期向前连续签到天数。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     * @return 连续签到天数
     */
    @Override
    public int getContinuousSignCount(String keyPrefix, Object userId, LocalDate date) {
        if (StrUtil.isBlank(keyPrefix) || ObjectUtil.isNull(userId) || ObjectUtil.isNull(date)) {
            return 0;
        }

        String key = buildSignKey(keyPrefix, userId, date.getYear());
        RBitSet bitSet = redissonClient.getBitSet(key);

        int count = 0;
        int bitIndex = date.getDayOfYear() - 1;

        for (int i = bitIndex; i >= 0; i--) {
            if (bitSet.get(i)) {
                count++;
            } else {
                break;
            }
        }

        return count;
    }

    // -------------------------------------------------------------------------
    // HyperLogLog / UV 统计
    // -------------------------------------------------------------------------

    /**
     * 添加 HyperLogLog 元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否改变基数估算
     */
    @Override
    public boolean hllAdd(String key, Object value) {
        checkKey(key);
        Assert.notNull(value, "HyperLogLog 元素不能为空");

        return redissonClient.getHyperLogLog(key).add(value);
    }

    /**
     * 批量添加 HyperLogLog 元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否改变基数估算
     */
    /**
     * 批量添加 HyperLogLog 元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否改变基数估算
     */
    @Override
    public boolean hllAddAll(String key, Collection<?> values) {
        checkKey(key);
        if (CollUtil.isEmpty(values)) {
            return false;
        }

        Collection<Object> valueList = new ArrayList<>(values.size());
        for (Object value : values) {
            if (ObjectUtil.isNotNull(value)) {
                valueList.add(value);
            }
        }

        if (CollUtil.isEmpty(valueList)) {
            return false;
        }

        return redissonClient.<Object>getHyperLogLog(key).addAll(valueList);
    }

    /**
     * 获取 HyperLogLog 基数估算。
     *
     * @param key Redis 键
     * @return 基数估算
     */
    @Override
    public long hllCount(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.getHyperLogLog(key).count();
    }

    /**
     * 合并 HyperLogLog。
     *
     * @param destKey 目标 key
     * @param keys    源 key
     * @return 合并后基数估算
     */
    @Override
    public long hllMerge(String destKey, String... keys) {
        checkKey(destKey);

        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return hllCount(destKey);
        }

        RHyperLogLog<Object> dest = redissonClient.getHyperLogLog(destKey);
        dest.mergeWith(keyArray);
        return dest.count();
    }

    /**
     * 按日期记录 UV。
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param userFlag  用户唯一标识
     * @param date      日期
     * @return 是否改变基数估算
     */
    @Override
    public boolean uvRecord(String keyPrefix, String bizKey, Object userFlag, LocalDate date) {
        checkKey(keyPrefix);
        checkKey(bizKey);
        Assert.notNull(userFlag, "用户唯一标识不能为空");
        Assert.notNull(date, "UV 日期不能为空");

        String key = buildUvKey(keyPrefix, bizKey, date);
        return hllAdd(key, userFlag);
    }

    /**
     * 获取指定日期 UV。
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param date      日期
     * @return UV 数量
     */
    @Override
    public long uvCount(String keyPrefix, String bizKey, LocalDate date) {
        if (StrUtil.hasBlank(keyPrefix, bizKey) || ObjectUtil.isNull(date)) {
            return 0L;
        }

        String key = buildUvKey(keyPrefix, bizKey, date);
        return hllCount(key);
    }

    // -------------------------------------------------------------------------
    // Geo / LBS 地理位置
    // -------------------------------------------------------------------------

    /**
     * 添加地理位置。
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param member    成员
     * @return 添加数量
     */
    @Override
    public long geoAdd(String key, double longitude, double latitude, Object member) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.notNull(member, "Geo 成员不能为空");

        return redissonClient.<Object>getGeo(key).add(longitude, latitude, member);
    }

    /**
     * 批量添加地理位置。
     *
     * @param key     Redis 键
     * @param entries 坐标集合
     * @return 添加数量
     */
    @Override
    public long geoAdd(String key, GeoEntry... entries) {
        checkKey(key);
        if (ArrayUtil.isEmpty(entries)) {
            return 0L;
        }

        return redissonClient.getGeo(key).add(entries);
    }

    /**
     * 仅成员不存在时添加地理位置。
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param member    成员
     * @return 是否添加成功
     */
    @Override
    public boolean geoTryAdd(String key, double longitude, double latitude, Object member) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.notNull(member, "Geo 成员不能为空");

        return redissonClient.<Object>getGeo(key).tryAdd(longitude, latitude, member);
    }

    /**
     * 计算两个成员距离。
     *
     * @param key     Redis 键
     * @param member1 成员 1
     * @param member2 成员 2
     * @param unit    单位
     * @return 距离
     */
    @Override
    public Double geoDistance(String key, Object member1, Object member2, GeoUnit unit) {
        if (StrUtil.isBlank(key) || ObjectUtil.hasEmpty(member1, member2, unit)) {
            return null;
        }

        return redissonClient.<Object>getGeo(key).dist(member1, member2, unit);
    }

    /**
     * 查询成员 GeoHash。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return GeoHash Map
     */
    @Override
    public Map<Object, String> geoHash(String key, Object... members) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(members)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object>getGeo(key).hash(members);
    }

    /**
     * 查询成员坐标。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return 坐标 Map
     */
    @Override
    public Map<Object, GeoPosition> geoPosition(String key, Object... members) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(members)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object>getGeo(key).pos(members);
    }

    /**
     * 根据条件搜索地理位置成员。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员列表
     */
    @Override
    public List<Object> geoSearch(String key, GeoSearchArgs args) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(args)) {
            return Collections.emptyList();
        }

        return redissonClient.<Object>getGeo(key).search(args);
    }

    /**
     * 根据条件搜索地理位置成员和距离。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员距离 Map
     */
    @Override
    public Map<Object, Double> geoSearchWithDistance(String key, GeoSearchArgs args) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(args)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object>getGeo(key).searchWithDistance(args);
    }

    /**
     * 根据条件搜索地理位置成员和坐标。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员坐标 Map
     */
    @Override
    public Map<Object, GeoPosition> geoSearchWithPosition(String key, GeoSearchArgs args) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(args)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object>getGeo(key).searchWithPosition(args);
    }

    /**
     * 删除地理位置成员。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return 删除数量
     */
    @Override
    public long geoRemove(String key, Object... members) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(members)) {
            return 0L;
        }

        return redissonClient.<Object>getGeo(key).removeAll(Arrays.asList(members)) ? members.length : 0L;
    }

    // -------------------------------------------------------------------------
    // 分布式会话
    // -------------------------------------------------------------------------

    /**
     * 创建或覆盖会话。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @param session   会话对象
     * @param ttl       过期时间
     */
    @Override
    public void sessionSet(String keyPrefix, String token, Object session, Duration ttl) {
        checkKey(keyPrefix);
        checkKey(token);
        Assert.notNull(session, "会话对象不能为空");
        checkPositiveDuration(ttl, "会话过期时间不能为空且必须大于 0");

        String key = buildSessionKey(keyPrefix, token);
        redissonClient.getBucket(key).set(session, ttl.toMillis(), TimeUnit.MILLISECONDS);

        log.info("Redis 会话写入成功，key={}", key);
    }

    /**
     * 获取会话。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @param clazz     目标类型
     * @param <T>       泛型类型
     * @return 会话对象
     */
    @Override
    public <T> T sessionGet(String keyPrefix, String token, Class<T> clazz) {
        if (StrUtil.hasBlank(keyPrefix, token) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        String key = buildSessionKey(keyPrefix, token);
        Object value = redissonClient.getBucket(key).get();
        return convertValue(value, clazz);
    }

    /**
     * 获取会话。
     *
     * @param keyPrefix     会话 key 前缀
     * @param token         Token
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 会话对象
     */
    @Override
    public <T> T sessionGet(String keyPrefix, String token, TypeReference<T> typeReference) {
        if (StrUtil.hasBlank(keyPrefix, token) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        String key = buildSessionKey(keyPrefix, token);
        Object value = redissonClient.getBucket(key).get();
        return convertValue(value, typeReference);
    }

    /**
     * 刷新会话过期时间。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @param ttl       过期时间
     * @return 是否刷新成功
     */
    @Override
    public boolean sessionRefresh(String keyPrefix, String token, Duration ttl) {
        if (StrUtil.hasBlank(keyPrefix, token)) {
            return false;
        }
        checkPositiveDuration(ttl, "会话过期时间不能为空且必须大于 0");

        String key = buildSessionKey(keyPrefix, token);
        boolean success = redissonClient.getBucket(key).expire(ttl);

        if (success) {
            log.info("Redis 会话续期成功，key={}", key);
        } else {
            log.warn("Redis 会话续期失败，可能会话不存在，key={}", key);
        }

        return success;
    }

    /**
     * 删除会话。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 是否删除成功
     */
    @Override
    public boolean sessionDelete(String keyPrefix, String token) {
        if (StrUtil.hasBlank(keyPrefix, token)) {
            return false;
        }

        String key = buildSessionKey(keyPrefix, token);
        boolean deleted = redissonClient.getBucket(key).delete();

        if (deleted) {
            log.info("Redis 会话删除成功，key={}", key);
        }

        return deleted;
    }

    // -------------------------------------------------------------------------
    // Queue / BlockingQueue / DelayedQueue / ReliableQueue
    // -------------------------------------------------------------------------

    /**
     * 入普通队列。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param <T>      元素类型
     * @return 是否入队成功
     */
    @Override
    public <T> boolean enqueue(String queueKey, T value) {
        checkKey(queueKey);

        return redissonClient.<T>getQueue(queueKey).offer(value);
    }

    /**
     * 出普通队列。
     *
     * @param queueKey 队列 key
     * @param <T>      元素类型
     * @return 元素
     */
    @Override
    public <T> T dequeue(String queueKey) {
        if (StrUtil.isBlank(queueKey)) {
            return null;
        }

        return redissonClient.<T>getQueue(queueKey).poll();
    }

    /**
     * 阻塞入队。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param <T>      元素类型
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public <T> void enqueueBlocking(String queueKey, T value) throws InterruptedException {
        checkKey(queueKey);

        redissonClient.<T>getBlockingQueue(queueKey).put(value);
    }

    /**
     * 超时阻塞入队。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param <T>      元素类型
     * @return 是否入队成功
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public <T> boolean enqueueBlocking(String queueKey, T value, long timeout, TimeUnit unit) throws InterruptedException {
        checkKey(queueKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        return redissonClient.<T>getBlockingQueue(queueKey).offer(value, timeout, unit);
    }

    /**
     * 超时阻塞出队。
     *
     * @param queueKey 队列 key
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param <T>      元素类型
     * @return 元素
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public <T> T dequeueBlocking(String queueKey, long timeout, TimeUnit unit) throws InterruptedException {
        checkKey(queueKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        return redissonClient.<T>getBlockingQueue(queueKey).poll(timeout, unit);
    }

    /**
     * 获取队列长度。
     *
     * @param queueKey 队列 key
     * @return 长度
     */
    @Override
    public long queueSize(String queueKey) {
        if (StrUtil.isBlank(queueKey)) {
            return 0L;
        }

        return redissonClient.getQueue(queueKey).size();
    }

    /**
     * 添加延迟队列任务。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param delay    延迟时间
     * @param unit     时间单位
     * @param <T>      元素类型
     */
    @Override
    public <T> void enqueueDelayed(String queueKey, T value, long delay, TimeUnit unit) {
        checkKey(queueKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(delay >= 0, "延迟时间不能小于 0");

        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueKey);
        RDelayedQueue<T> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(value, delay, unit);

        log.info("Redis 延迟任务入队成功，queueKey={}，delay={}，unit={}", queueKey, delay, unit);
    }

    /**
     * 获取延迟队列对象。
     *
     * @param queueKey 队列 key
     * @param <T>      元素类型
     * @return RDelayedQueue
     */
    @Override
    public <T> RDelayedQueue<T> getDelayedQueue(String queueKey) {
        checkKey(queueKey);

        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueKey);
        return redissonClient.getDelayedQueue(blockingQueue);
    }

    /**
     * 清空队列。
     *
     * @param queueKey 队列 key
     */
    @Override
    public void clearQueue(String queueKey) {
        if (StrUtil.isBlank(queueKey)) {
            return;
        }

        redissonClient.getQueue(queueKey).clear();
    }

    /**
     * 判断队列是否为空。
     *
     * @param queueKey 队列 key
     * @return 为空返回 true
     */
    @Override
    public boolean isQueueEmpty(String queueKey) {
        if (StrUtil.isBlank(queueKey)) {
            return true;
        }

        return redissonClient.getQueue(queueKey).isEmpty();
    }

    /**
     * 删除队列元素。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @return 是否删除成功
     */
    @Override
    public boolean removeFromQueue(String queueKey, Object value) {
        if (StrUtil.isBlank(queueKey)) {
            return false;
        }

        return redissonClient.getQueue(queueKey).remove(value);
    }

    /**
     * 获取环形缓冲队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RRingBuffer
     */
    @Override
    public <T> RRingBuffer<T> getRingBuffer(String key) {
        checkKey(key);

        return redissonClient.getRingBuffer(key);
    }

    /**
     * 获取优先级队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityQueue
     */
    @Override
    public <T> RPriorityQueue<T> getPriorityQueue(String key) {
        checkKey(key);

        return redissonClient.getPriorityQueue(key);
    }

    /**
     * 获取阻塞双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RBlockingDeque
     */
    @Override
    public <T> RBlockingDeque<T> getBlockingDeque(String key) {
        checkKey(key);

        return redissonClient.getBlockingDeque(key);
    }

    // -------------------------------------------------------------------------
    // 发布订阅 / Topic
    // -------------------------------------------------------------------------

    /**
     * 发布消息。
     *
     * @param channel 频道
     * @param message 消息
     * @return 接收客户端数量
     */
    @Override
    public long publish(String channel, Object message) {
        checkKey(channel);

        return redissonClient.getTopic(channel).publish(message);
    }

    /**
     * 订阅频道。
     *
     * @param channel         频道
     * @param messageConsumer 消费回调
     * @return 监听器 ID
     */
    @Override
    public int subscribe(String channel, Consumer<Object> messageConsumer) {
        checkKey(channel);
        Assert.notNull(messageConsumer, "消息消费回调不能为空");

        MessageListener<Object> listener = (topic, message) -> {
            try {
                messageConsumer.accept(message);
            } catch (Exception e) {
                log.error("Redis 订阅消息消费异常，channel={}，message={}", channel, message, e);
            }
        };

        int listenerId = redissonClient.getTopic(channel).addListener(Object.class, listener);
        log.info("Redis 频道订阅成功，channel={}，listenerId={}", channel, listenerId);
        return listenerId;
    }

    /**
     * 取消订阅频道。
     *
     * @param channel    频道
     * @param listenerId 监听器 ID
     */
    @Override
    public void unsubscribe(String channel, int listenerId) {
        if (StrUtil.isBlank(channel)) {
            return;
        }

        redissonClient.getTopic(channel).removeListener(listenerId);
        log.info("Redis 频道取消订阅成功，channel={}，listenerId={}", channel, listenerId);
    }

    /**
     * 取消订阅频道全部监听器。
     *
     * @param channel 频道
     */
    @Override
    public void unsubscribe(String channel) {
        if (StrUtil.isBlank(channel)) {
            return;
        }

        redissonClient.getTopic(channel).removeAllListeners();
        log.info("Redis 频道全部监听器已移除，channel={}", channel);
    }

    /**
     * 获取模式主题。
     *
     * @param pattern 频道通配符
     * @return RPatternTopic
     */
    @Override
    public RPatternTopic getPatternTopic(String pattern) {
        checkKey(pattern);

        return redissonClient.getPatternTopic(pattern);
    }

    /**
     * 获取可靠主题。
     *
     * @param topic 主题名
     * @return RReliableTopic
     */
    @Override
    public RReliableTopic getReliableTopic(String topic) {
        checkKey(topic);

        return redissonClient.getReliableTopic(topic);
    }

    // -------------------------------------------------------------------------
    // Stream 消息流
    // -------------------------------------------------------------------------

    /**
     * 添加 Stream 消息。
     *
     * @param streamKey Stream key
     * @param entries   消息字段
     * @return 消息 ID
     */
    @Override
    public StreamMessageId streamAdd(String streamKey, Map<Object, Object> entries) {
        checkKey(streamKey);
        Assert.isTrue(CollUtil.isNotEmpty(entries), "Stream 消息字段不能为空");

        return redissonClient.<Object, Object>getStream(streamKey)
                .add(StreamAddArgs.entries(entries));
    }

    /**
     * 添加 Stream 消息。
     *
     * @param streamKey Stream key
     * @param args      添加参数
     * @param <K>       字段类型
     * @param <V>       值类型
     * @return 消息 ID
     */
    @Override
    public <K, V> StreamMessageId streamAdd(String streamKey, StreamAddArgs<K, V> args) {
        checkKey(streamKey);
        Assert.notNull(args, "Stream 添加参数不能为空");

        return redissonClient.<K, V>getStream(streamKey).add(args);
    }

    /**
     * 读取 Stream 消息。
     *
     * @param streamKey Stream key
     * @param args      读取参数
     * @return 消息 Map
     */
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamRead(String streamKey, StreamReadArgs args) {
        checkKey(streamKey);
        Assert.notNull(args, "Stream 读取参数不能为空");

        return redissonClient.<Object, Object>getStream(streamKey).read(args);
    }

    /**
     * 创建消费组。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param id        起始消息 ID
     */
    @Override
    public void streamCreateGroup(String streamKey, String groupName, StreamMessageId id) {
        checkKey(streamKey);
        checkKey(groupName);
        Assert.notNull(id, "Stream 起始消息 ID 不能为空");

        try {
            StreamCreateGroupArgs args = StreamCreateGroupArgs.name(groupName)
                    .id(id)
                    .makeStream();

            redissonClient.<Object, Object>getStream(streamKey).createGroup(args);
            log.info("Redis Stream 消费组创建成功，streamKey={}，groupName={}，id={}", streamKey, groupName, id);
        } catch (Exception e) {
            log.warn("Redis Stream 消费组创建失败，可能消费组已存在，streamKey={}，groupName={}", streamKey, groupName, e);
        }
    }

    /**
     * 读取消费组消息。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @param args         读取参数
     * @return 消息 Map
     */
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamReadGroup(
            String streamKey,
            String groupName,
            String consumerName,
            StreamReadGroupArgs args
    ) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        Assert.notNull(args, "Stream 消费组读取参数不能为空");

        return redissonClient.<Object, Object>getStream(streamKey)
                .readGroup(groupName, consumerName, args);
    }

    /**
     * 确认 Stream 消息。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param ids       消息 ID
     * @return 确认数量
     */
    @Override
    public long streamAck(String streamKey, String groupName, StreamMessageId... ids) {
        checkKey(streamKey);
        checkKey(groupName);
        if (ArrayUtil.isEmpty(ids)) {
            return 0L;
        }

        return redissonClient.<Object, Object>getStream(streamKey).ack(groupName, ids);
    }

    /**
     * 删除 Stream 消息。
     *
     * @param streamKey Stream key
     * @param ids       消息 ID
     * @return 删除数量
     */
    @Override
    public long streamRemove(String streamKey, StreamMessageId... ids) {
        checkKey(streamKey);
        if (ArrayUtil.isEmpty(ids)) {
            return 0L;
        }

        return redissonClient.<Object, Object>getStream(streamKey).remove(ids);
    }

    /**
     * 获取 Stream 长度。
     *
     * @param streamKey Stream key
     * @return 长度
     */
    @Override
    public long streamSize(String streamKey) {
        if (StrUtil.isBlank(streamKey)) {
            return 0L;
        }

        return redissonClient.getStream(streamKey).size();
    }

    // -------------------------------------------------------------------------
    // Lua / 脚本 / 批处理 / 事务
    // -------------------------------------------------------------------------

    /**
     * 执行 Lua 脚本。
     *
     * @param script     Lua 脚本
     * @param mode       执行模式
     * @param returnType 返回类型
     * @param keys       KEYS
     * @param values     ARGV
     * @param <T>        返回泛型
     * @return 执行结果
     */
    @Override
    public <T> T eval(String script, RScript.Mode mode, RScript.ReturnType returnType, List<Object> keys, Object... values) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua 脚本不能为空");
        Assert.notNull(mode, "Lua 执行模式不能为空");
        Assert.notNull(returnType, "Lua 返回类型不能为空");

        List<Object> safeKeys = safeScriptKeys(keys);
        return redissonClient.getScript().eval(mode, script, returnType, safeKeys, values);
    }

    /**
     * 执行 Lua 脚本并返回指定类型。
     *
     * @param script     Lua 脚本
     * @param returnType 目标类型
     * @param keys       KEYS
     * @param args       ARGV
     * @param <T>        返回泛型
     * @return 执行结果
     */
    @Override
    public <T> T eval(String script, Class<T> returnType, List<Object> keys, Object... args) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua 脚本不能为空");
        Assert.notNull(returnType, "Lua 返回类型不能为空");

        Object result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.VALUE,
                safeScriptKeys(keys),
                args
        );
        return convertValue(result, returnType);
    }

    /**
     * 执行 Lua 脚本但不关心结果。
     *
     * @param script Lua 脚本
     * @param keys   KEYS
     * @param args   ARGV
     */
    @Override
    public void evalNoResult(String script, List<Object> keys, Object... args) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua 脚本不能为空");

        redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.VALUE,
                safeScriptKeys(keys),
                args
        );
    }

    /**
     * 根据 SHA1 执行 Lua 脚本。
     *
     * @param sha1       脚本 SHA1
     * @param returnType 目标类型
     * @param keys       KEYS
     * @param values     ARGV
     * @param <T>        返回泛型
     * @return 执行结果
     */
    @Override
    public <T> T evalBySha(String sha1, Class<T> returnType, List<Object> keys, Object... values) {
        Assert.isTrue(StrUtil.isNotBlank(sha1), "Lua 脚本 SHA1 不能为空");
        Assert.notNull(returnType, "Lua 返回类型不能为空");

        Object result = redissonClient.getScript().evalSha(
                RScript.Mode.READ_WRITE,
                sha1,
                RScript.ReturnType.VALUE,
                safeScriptKeys(keys),
                values
        );
        return convertValue(result, returnType);
    }

    /**
     * 加载 Lua 脚本。
     *
     * @param script Lua 脚本
     * @return SHA1
     */
    @Override
    public String loadScript(String script) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua 脚本不能为空");

        String sha1 = redissonClient.getScript().scriptLoad(script);
        log.info("Redis Lua 脚本加载成功，sha1={}", sha1);
        return sha1;
    }

    /**
     * 创建批处理对象。
     *
     * @return RBatch
     */
    @Override
    public RBatch createBatch() {
        return redissonClient.createBatch();
    }

    /**
     * 创建事务对象。
     *
     * @return RTransaction
     */
    @Override
    public RTransaction createTransaction() {
        return redissonClient.createTransaction(TransactionOptions.defaults());
    }

    // -------------------------------------------------------------------------
    // 私有辅助方法
    // -------------------------------------------------------------------------

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key 不能为空");
    }

    /**
     * 校验正数时间。
     *
     * @param duration 时间
     * @param message  异常消息
     */
    private void checkPositiveDuration(Duration duration, String message) {
        Assert.notNull(duration, message);
        Assert.isTrue(!duration.isZero() && !duration.isNegative(), message);
    }

    /**
     * 过滤空白 Redis Key。
     *
     * @param keys Redis 键数组
     * @return 过滤后的 Redis 键数组
     */
    private String[] filterKeys(String... keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return new String[0];
        }
        return java.util.Arrays.stream(keys)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
    }

    /**
     * 校验哈希字段。
     *
     * @param field 字段名
     */
    private void checkField(String field) {
        Assert.isTrue(StrUtil.isNotBlank(field), "Redis Hash 字段不能为空");
    }

    /**
     * 规范化列表索引，支持负数索引。
     *
     * @param index 原始索引
     * @param size  列表长度
     * @return 实际索引
     */
    private int normalizeIndex(long index, int size) {
        long realIndex = index < 0 ? size + index : index;
        if (realIndex > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (realIndex < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) realIndex;
    }

    /**
     * 将集合元素转换为指定类型 Set。
     *
     * @param source 原始集合
     * @param clazz  目标类型
     * @param <T>    泛型类型
     * @return 转换后的 Set
     */
    private <T> Set<T> convertSet(Collection<?> source, Class<T> clazz) {
        if (CollUtil.isEmpty(source) || ObjectUtil.isNull(clazz)) {
            return Collections.emptySet();
        }

        Set<T> result = new LinkedHashSet<>(source.size());
        for (Object item : source) {
            T converted = convertValue(item, clazz);
            if (ObjectUtil.isNotNull(converted)) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * 将 ScoredEntry 集合转换为有序 Map。
     *
     * @param entries ScoredEntry 集合
     * @return 元素分数 Map
     */
    private Map<Object, Double> scoredEntriesToMap(Collection<ScoredEntry<Object>> entries) {
        if (CollUtil.isEmpty(entries)) {
            return Collections.emptyMap();
        }

        Map<Object, Double> result = new LinkedHashMap<>(entries.size());
        for (ScoredEntry<Object> entry : entries) {
            result.put(entry.getValue(), entry.getScore());
        }
        return result;
    }

    /**
     * 安全释放分布式锁。
     *
     * @param lockKey 锁 key
     * @param lock    锁对象
     * @param locked  是否已加锁
     */
    private void unlockSafely(String lockKey, RLock lock, boolean locked) {
        if (!locked || ObjectUtil.isNull(lock)) {
            return;
        }

        if (!lock.isHeldByCurrentThread()) {
            log.warn("当前线程未持有分布式锁，跳过释放，lockKey={}", lockKey);
            return;
        }

        try {
            lock.unlock();
        } catch (Exception e) {
            log.error("释放分布式锁异常，lockKey={}", lockKey, e);
        }
    }

    /**
     * 构建签到 Key。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param year      年份
     * @return 签到 Key
     */
    private String buildSignKey(String keyPrefix, Object userId, int year) {
        return StrUtil.format("{}:{}:{}", keyPrefix, userId, year);
    }

    /**
     * 构建 UV Key。
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param date      日期
     * @return UV Key
     */
    private String buildUvKey(String keyPrefix, String bizKey, LocalDate date) {
        String dateText = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return StrUtil.format("{}:{}:{}", keyPrefix, bizKey, dateText);
    }

    /**
     * 构建会话 Key。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 会话 Key
     */
    private String buildSessionKey(String keyPrefix, String token) {
        return StrUtil.format("{}:{}", keyPrefix, token);
    }

    /**
     * 校验 Geo 坐标。
     *
     * @param longitude 经度
     * @param latitude  纬度
     */
    private void checkGeoCoordinate(double longitude, double latitude) {
        Assert.isTrue(longitude >= -180D && longitude <= 180D, "经度必须在 -180 到 180 之间");
        Assert.isTrue(latitude >= -90D && latitude <= 90D, "纬度必须在 -90 到 90 之间");
    }

    /**
     * 获取安全的 Lua KEYS 参数。
     *
     * @param keys 原始 KEYS
     * @return 非空 KEYS
     */
    private List<Object> safeScriptKeys(List<Object> keys) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyList();
        }
        return keys;
    }

}