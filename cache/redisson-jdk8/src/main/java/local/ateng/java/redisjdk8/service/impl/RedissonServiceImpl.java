package local.ateng.java.redisjdk8.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.redisjdk8.service.RedissonService;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务实现类
 * 基于 Redisson 实现 key 相关的操作
 *
 * @author Ateng
 * @since 2025-08-01
 */
@Service
public class RedissonServiceImpl implements RedissonService {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public RedissonServiceImpl(
            RedissonClient redissonClient,
            ObjectMapper objectMapper
    ) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    // -------------------------- 通用 Key 管理 --------------------------

    /**
     * 判断指定 key 是否存在
     *
     * @param key redis 键
     * @return 存在返回 true，否则 false
     */
    @Override
    public boolean hasKey(String key) {
        return redissonClient.getKeys().countExists(key) > 0;
    }

    /**
     * 删除指定 key
     *
     * @param key redis 键
     * @return 是否成功删除
     */
    @Override
    public boolean deleteKey(String key) {
        return redissonClient.getKeys().delete(key) > 0;
    }

    /**
     * 批量删除指定 key 集合
     *
     * @param keys redis 键集合
     * @return 成功删除的数量
     */
    @Override
    public long deleteKeys(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return redissonClient.getKeys().delete(keys.toArray(new String[0]));
    }

    /**
     * 设置 key 的过期时间
     *
     * @param key     redis 键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redissonClient.getBucket(key).expire(Duration.ofMillis(unit.toMillis(timeout)));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 key 的剩余过期时间
     *
     * @param key  redis 键
     * @param unit 时间单位
     * @return 剩余时间（-1 表示永久；-2 表示不存在）
     */
    @Override
    public long getTtl(String key, TimeUnit unit) {
        return redissonClient.getBucket(key).remainTimeToLive() > 0
                ? unit.convert(redissonClient.getBucket(key).remainTimeToLive(), TimeUnit.MILLISECONDS)
                : -1;
    }

    /**
     * 让 key 永久不过期（移除过期时间）
     *
     * @param key redis 键
     * @return 是否成功移除
     */
    @Override
    public boolean persist(String key) {
        return redissonClient.getBucket(key).clearExpire();
    }

    /**
     * 修改 key 名称（key 必须存在，且新 key 不存在）
     *
     * @param oldKey 旧的 redis 键
     * @param newKey 新的 redis 键
     * @return 是否成功
     */
    @Override
    public boolean renameKey(String oldKey, String newKey) {
        try {
            redissonClient.getKeys().rename(oldKey, newKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 如果新 key 不存在则重命名
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return 是否成功
     */
    @Override
    public boolean renameKeyIfAbsent(String oldKey, String newKey) {
        return redissonClient.getKeys().renamenx(oldKey, newKey);
    }

    /**
     * 获取所有匹配 pattern 的 key（慎用，生产环境建议加前缀限制）
     *
     * @param pattern 通配符表达式，如 user:*、session_*
     * @return 匹配的 key 集合
     */
    @Override
    public Set<String> keys(String pattern) {
        RKeys rKeys = redissonClient.getKeys();
        Iterable<String> iterable = rKeys.getKeysByPattern(pattern);
        Set<String> result = new java.util.HashSet<>();
        for (String key : iterable) {
            result.add(key);
        }
        return result;
    }


    /**
     * 判断 key 是否已经过期（不存在或 ttl <= 0 视为过期）
     *
     * @param key redis 键
     * @return true 表示已经过期或不存在
     */
    @Override
    public boolean isExpired(String key) {
        return !hasKey(key) || getTtl(key, TimeUnit.MILLISECONDS) <= 0;
    }

    /**
     * 获取 key 的 value 类型名称
     * （string、list、set、zset、hash 等）
     *
     * @param key redis 键
     * @return 类型名称，若不存在返回 null
     */
    @Override
    public String getKeyType(String key) {
        RType type = redissonClient.getKeys().getType(key);
        return type != null ? type.name() : null;
    }

    /**
     * 对指定 key 执行原子整数加法操作（适用于计数器）
     *
     * @param key   redis 键
     * @param delta 要增加的整数值（正负均可）
     * @return 操作后的最新值
     */
    @Override
    public long increment(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    /**
     * 对指定 key 执行原子整数减法操作（适用于计数器）
     *
     * @param key   redis 键
     * @param delta 要减少的整数值（正数）
     * @return 操作后的最新值
     */
    @Override
    public long decrement(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    /**
     * 对指定 key 执行原子浮点数加法操作（支持 double，适用于余额、分数等）
     *
     * @param key   redis 键
     * @param delta 要增加的浮点数值（正负均可）
     * @return 操作后的最新值
     */
    @Override
    public double incrementDouble(String key, double delta) {
        return redissonClient.getAtomicDouble(key).addAndGet(delta);
    }

    /**
     * 对指定 key 执行原子浮点数减法操作（支持 double）
     *
     * @param key   redis 键
     * @param delta 要减少的浮点数值（正数）
     * @return 操作后的最新值
     */
    @Override
    public double decrementDouble(String key, double delta) {
        return redissonClient.getAtomicDouble(key).addAndGet(-delta);
    }

    // -------------------------- 字符串操作 --------------------------

    /**
     * 类型转换工具方法：将 Object 转换为指定类型
     *
     * @param value 原始对象
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的对象，或 null（若原始对象为 null）
     */
    @Override
    public <T> T convertValue(Object value, Class<T> clazz) {
        if (value == null || clazz == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return objectMapper.convertValue(value, clazz);
    }

    /**
     * 类型转换工具方法：将 Object 转换为指定类型
     *
     * @param value         原始对象
     * @param typeReference 目标类型引用（支持泛型）
     * @param <T>           目标类型泛型
     * @return 转换后的对象，失败返回 null
     */
    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeReference) {
        if (value == null || typeReference == null || typeReference.getType() == null) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            T casted = (T) value;
            return casted;
        } catch (ClassCastException e) {
            try {
                return objectMapper.convertValue(value, typeReference);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    /**
     * 设置任意对象缓存（无过期时间）
     *
     * @param key   redis 键
     * @param value 任意对象
     */
    @Override
    public void set(String key, Object value) {
        redissonClient.getBucket(key).set(value);
    }

    /**
     * 设置任意对象缓存（带过期时间）
     *
     * @param key     redis 键
     * @param value   任意对象
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redissonClient.getBucket(key).set(value, timeout, unit);
    }

    /**
     * 根据 key 获取缓存的对象，并转换为指定类型
     *
     * @param key   Redis中的键
     * @param clazz 目标对象类型
     * @param <T>   返回类型
     * @return 转换后的对象，获取失败或类型不匹配时返回 null
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            Object value = bucket.get();
            if (value == null) {
                return null;
            }

            // 步骤1：如果类型本身就是目标类型，直接强转返回
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }

            // 步骤2：尝试使用 ObjectMapper 进行类型转换
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据 key 获取缓存的对象（泛型版本），可指定复杂泛型类型（如 List<User>）
     * <p>
     * 优先尝试强转并检查类型是否匹配，避免重复序列化；若类型不匹配则使用 ObjectMapper 转换
     *
     * @param key           Redis键
     * @param typeReference 类型引用（支持泛型）
     * @param <T>           返回值类型
     * @return 指定类型的对象，若不存在或转换失败则返回 null
     */
    @Override
    public <T> T get(String key, TypeReference<T> typeReference) {
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            Object value = bucket.get();
            if (value == null) {
                return null;
            }

            // 步骤1：尝试强转
            try {
                T casted = (T) value;

                // 步骤2：判断泛型兼容（只对常见结构如 List、Map 做判断）
                JavaType expectedType = objectMapper.getTypeFactory().constructType(typeReference);

                if (expectedType.isCollectionLikeType() && value instanceof Collection) {
                    // 判断 Collection 的元素类型是否匹配
                    Collection<?> collection = (Collection<?>) value;
                    if (!collection.isEmpty()) {
                        Class<?> actualElementClass = collection.iterator().next().getClass();
                        Class<?> expectedElementClass = expectedType.getContentType().getRawClass();
                        if (expectedElementClass.isAssignableFrom(actualElementClass)) {
                            return casted;
                        }
                    } else {
                        // 空集合无法判断元素类型，直接返回
                        return casted;
                    }
                } else if (expectedType.isMapLikeType() && value instanceof Map) {
                    JavaType valueType = expectedType.containedType(1);
                    if (valueType != null) {
                        Class<?> expectedValueClass = valueType.getRawClass();
                        Map<?, ?> map = (Map<?, ?>) value;
                        if (!map.isEmpty()) {
                            Object firstValue = map.values().iterator().next();
                            Class<?> actualValueClass = firstValue.getClass();
                            // 判断实际类是否是期望类的子类或相同类
                            if (expectedValueClass.isAssignableFrom(actualValueClass)) {
                                return casted;
                            }
                            // 这里不匹配，交给外层去处理转换
                        } else {
                            // 空 map，无法判断，直接返回
                            return casted;
                        }
                    }
                } else if (expectedType.getRawClass().isAssignableFrom(value.getClass())) {
                    return casted;
                }

            } catch (Exception ignore) {
                // 忽略强转失败，继续走反序列化
            }

            // 步骤3：反序列化处理
            return objectMapper.convertValue(value, typeReference);

        } catch (Exception e) {
            return null;
        }
    }


    /**
     * 原子设置值，只有当 key 不存在时才成功
     *
     * @param key     redis 键
     * @param value   任意对象
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true 设置成功，false 已存在
     */
    @Override
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(key).setIfAbsent(value, Duration.ofMillis(unit.toMillis(timeout)));
    }

    /**
     * 原子替换对象值并返回旧值
     *
     * @param key   redis 键
     * @param value 新值对象
     * @param clazz 旧值类型
     * @param <T>   泛型
     * @return 旧值，key 不存在返回 null
     */
    @Override
    public <T> T getAndSet(String key, Object value, Class<T> clazz) {
        Object oldValue = redissonClient.getBucket(key).getAndSet(value);
        if (oldValue == null) {
            return null;
        }
        return clazz.isInstance(oldValue) ? clazz.cast(oldValue) : null;
    }

    /**
     * 原子替换对象值并返回旧值（泛型）
     *
     * @param key           redis 键
     * @param value         新值对象
     * @param typeReference 泛型类型引用
     * @param <T>           泛型
     * @return 旧值，key 不存在返回 null
     */
    @Override
    public <T> T getAndSet(String key, Object value, TypeReference<T> typeReference) {
        Object oldValue = redissonClient.getBucket(key).getAndSet(value);
        if (oldValue == null) {
            return null;
        }
        try {
            return (T) oldValue;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * 获取对象值的序列化字节大小（不保证是业务字段长度）
     *
     * @param key redis 键
     * @return 字节大小，key 不存在返回 0
     */
    @Override
    public long size(String key) {
        Object obj = redissonClient.getBucket(key).get();
        if (obj == null) {
            return 0L;
        }
        try {
            byte[] bytes = SerializationUtils.serialize(obj);
            return bytes != null ? bytes.length : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

}
