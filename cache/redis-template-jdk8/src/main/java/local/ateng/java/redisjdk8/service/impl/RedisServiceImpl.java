package local.ateng.java.redisjdk8.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.redisjdk8.service.RLock;
import local.ateng.java.redisjdk8.service.RedisLockService;
import local.ateng.java.redisjdk8.service.RedisService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * RedisService 接口的实现类，封装了基于 RedisTemplate 的 Redis 操作。
 *
 * @author 孔余
 * @since 2025-07-31
 */
@Service
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisLockService redisLockService;

    public RedisServiceImpl(
            @Qualifier("jacksonRedisTemplate")
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            RedisLockService redisLockService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redisLockService = redisLockService;
    }

    /**
     * 设置值（无过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置值并指定过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间（例如 10）
     * @param unit    时间单位（例如 TimeUnit.SECONDS）
     */
    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 设置值并指定过期时间（JDK8 Duration）
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param duration 过期时间（如 Duration.ofMinutes(5)）
     */
    @Override
    public void set(String key, Object value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    /**
     * 获取缓存值（Object 原始类型）
     *
     * @param key 缓存键
     * @return 缓存值，可能为 null
     */
    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存值并自动反序列化为指定类型
     *
     * @param key   缓存键
     * @param clazz 目标类型类
     * @return 指定类型的对象，可能为 null
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return convertValue(value, clazz);
    }

    /**
     * 获取缓存值并自动反序列化为指定泛型类型（支持复杂类型，如 List<T>、Map<String, T> 等）。
     *
     * @param key           缓存键
     * @param typeReference 泛型类型引用，用于指定目标类型
     * @param <T>           泛型类型
     * @return 指定类型的对象，可能为 null
     */
    @Override
    public <T> T get(String key, TypeReference<T> typeReference) {
        Object value = redisTemplate.opsForValue().get(key);
        return convertValue(value, typeReference);
    }

    /**
     * 获取缓存值并反序列化为 List 类型
     *
     * @param key   缓存键
     * @param clazz 列表中元素的类型
     * @param <T>   泛型类型
     * @return 列表对象，可能为空
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getList(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof List) {
            List<?> rawList = (List<?>) value;
            if (rawList.isEmpty() || clazz.isInstance(rawList.get(0))) {
                return rawList.stream()
                        .map(item -> (T) item)
                        .collect(Collectors.toList());
            } else {
                throw new IllegalStateException("缓存中的 List 元素类型与目标类型不一致，key: " + key);
            }
        }

        throw new IllegalStateException("缓存中的值不是 List 类型，key: " + key);
    }

    /**
     * 批量获取多个 key 的缓存值（原始类型）
     *
     * @param keys key 集合
     * @return 原始对象列表（顺序对应 keys）
     */
    @Override
    public List<Object> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        return values != null ? values : Collections.emptyList();
    }

    /**
     * 批量获取多个 key 并反序列化为目标类型
     *
     * @param keys  key 集合
     * @param clazz 目标类型
     * @return 对象列表（顺序对应 keys）
     */
    @Override
    public <T> List<T> multiGet(Collection<String> keys, Class<T> clazz) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(convertValue(value, clazz));
        }

        return result;
    }

    /**
     * 获取旧值并设置新值
     *
     * @param key      缓存键
     * @param newValue 新值
     * @return 原始旧值对象
     */
    @Override
    public Object getAndSet(String key, Object newValue) {
        return redisTemplate.opsForValue().getAndSet(key, newValue);
    }

    /**
     * 获取旧值并设置新值，并反序列化为目标类型
     *
     * @param key      缓存键
     * @param newValue 新值
     * @param clazz    返回对象类型
     * @return 旧值反序列化结果
     */
    @Override
    public <T> T getAndSet(String key, Object newValue, Class<T> clazz) {
        Object oldValue = redisTemplate.opsForValue().getAndSet(key, newValue);
        return convertValue(oldValue, clazz);
    }

    /**
     * 如果缓存中不存在该 key，则设置（等同于 SETNX）
     *
     * @param key   缓存键
     * @param value 值
     * @return true 设置成功，false 已存在
     */
    @Override
    public boolean setIfAbsent(String key, Object value) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 如果缓存中不存在该 key，则设置并设置过期时间
     *
     * @param key     缓存键
     * @param value   值
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 设置成功，false 已存在
     */
    @Override
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        if (ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(value) || timeout <= 0) {
            return false;
        }
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 向已存在的字符串值追加内容
     *
     * @param key   缓存键
     * @param value 要追加的内容
     * @return 原始字符串追加后的长度
     */
    @Override
    public int append(String key, String value) {
        Integer result = redisTemplate.opsForValue().append(key, value);
        return result != null ? result : 0;
    }

    /**
     * 获取值并立即删除（一次性消费场景）
     *
     * @param key   缓存键
     * @param clazz 返回对象类型
     * @return 缓存值（已删除）
     */
    @Override
    public <T> T getAndDelete(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        redisTemplate.delete(key);
        return convertValue(value, clazz);
    }

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
     * 删除指定 key
     *
     * @param key 要删除的 key
     * @return 是否删除成功
     */
    @Override
    public boolean delete(String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 批量删除多个 key
     *
     * @param keys 要删除的 key 集合
     * @return 成功删除的 key 数量
     */
    @Override
    public long deleteKeys(Collection<String> keys) {
        Long result = redisTemplate.delete(keys);
        return result != null ? result : 0L;
    }

    /**
     * 判断指定 key 是否存在
     *
     * @param key 要检查的 key
     * @return key 是否存在
     */
    @Override
    public boolean hasKey(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 设置 key 的过期时间（单位：秒）
     *
     * @param key     缓存 key
     * @param timeout 过期时间（秒）
     * @return 是否设置成功
     */
    @Override
    public boolean expire(String key, long timeout) {
        Boolean result = redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 设置 key 的过期时间（支持自定义时间单位）
     *
     * @param key     缓存 key
     * @param timeout 过期时间
     * @param unit    时间单位（如 TimeUnit.SECONDS）
     * @return 是否设置成功
     */
    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.expire(key, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 设置 key 的过期时间点（绝对时间）
     *
     * @param key      缓存 key
     * @param dateTime 到期时间点
     * @return 是否设置成功
     */
    @Override
    public boolean expireAt(String key, LocalDateTime dateTime) {
        Instant instant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
        Boolean result = redisTemplate.expireAt(key, Date.from(instant));
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获取 key 的剩余过期时间（单位：秒）
     *
     * @param key 缓存 key
     * @return 剩余时间（秒），如果为 -1 表示永久不过期；-2 表示 key 不存在
     */
    @Override
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -2L;
    }

    /**
     * 获取 key 的剩余过期时间（支持指定时间单位）
     *
     * @param key  缓存 key
     * @param unit 时间单位（如 TimeUnit.SECONDS）
     * @return 剩余时间（指定单位），-1 表示永久；-2 表示不存在
     */
    @Override
    public long getExpire(String key, TimeUnit unit) {
        Long expire = redisTemplate.getExpire(key, unit);
        return expire != null ? expire : -2L;
    }

    /**
     * 移除 key 的过期时间，使其永久存在
     *
     * @param key 缓存 key
     * @return 是否成功移除过期时间
     */
    @Override
    public boolean persist(String key) {
        Boolean result = redisTemplate.persist(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 重命名指定 key（如新 key 已存在会覆盖）
     *
     * @param oldKey 原 key
     * @param newKey 新 key
     */
    @Override
    public void rename(String oldKey, String newKey) {
        try {
            redisTemplate.rename(oldKey, newKey);
        } catch (Exception e) {
            throw new IllegalStateException("重命名失败，可能原 key 不存在或发生其他错误，oldKey: " + oldKey + ", newKey: " + newKey);
        }
    }

    /**
     * 重命名指定 key（仅当新 key 不存在时生效）
     *
     * @param oldKey 原 key
     * @param newKey 新 key
     * @return 是否重命名成功
     */
    @Override
    public boolean renameIfAbsent(String oldKey, String newKey) {
        Boolean result = redisTemplate.renameIfAbsent(oldKey, newKey);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获取所有匹配 pattern 的 key（⚠️ 避免在生产环境使用，可能引发阻塞）
     *
     * @param pattern 匹配模式（如：user:*）
     * @return 匹配到的 key 集合
     */
    @Override
    public Set<String> keys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys : Collections.emptySet();
    }

    /**
     * 使用 SCAN 命令匹配 key（推荐替代 keys，适合大数据量）
     *
     * @param pattern 匹配模式（如：user:*）
     * @return 匹配到的 key 集合（非阻塞）
     */
    @Override
    public Set<String> scanKeys(String pattern) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();

        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(options)) {
            while (cursor.hasNext()) {
                byte[] rawKey = cursor.next();
                String key = redisTemplate.getStringSerializer().deserialize(rawKey);
                if (key != null) {
                    result.add(key);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("使用 SCAN 命令扫描 key 时出错，pattern: " + pattern);
        }

        return result;
    }

    /**
     * 使用 SCAN 命令匹配 key（支持自定义每批次返回数量）
     *
     * @param pattern 匹配模式（如：user:*）
     * @param count   每批次 scan 数量（建议 >= 100）
     * @return 匹配到的 key 集合（非阻塞）
     */
    @Override
    public Set<String> scanKeys(String pattern, int count) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(count).build();

        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(options)) {
            while (cursor.hasNext()) {
                byte[] rawKey = cursor.next();
                String key = redisTemplate.getStringSerializer().deserialize(rawKey);
                if (key != null) {
                    result.add(key);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("使用 SCAN 命令扫描 key 时出错，pattern: " + pattern + ", count: " + count);
        }

        return result;
    }

    /**
     * 遍历 Hash 中的所有字段，返回匹配 pattern 的字段列表。
     *
     * @param hash    Hash 键
     * @param pattern 字段匹配模式，支持通配符
     * @return 匹配的字段列表
     */
    @Override
    public Set<String> scanHashKeys(String hash, String pattern) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();

        try (Cursor<Map.Entry<Object, Object>> cursor =
                     redisTemplate.opsForHash().scan(hash, options)) {
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                Object field = entry.getKey();
                if (field instanceof String) {
                    result.add((String) field);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("扫描 Hash 字段时出错，hash: " + hash + ", pattern: " + pattern);
        }

        return result;
    }

    /**
     * 遍历 Hash 中的所有字段，返回匹配 pattern 的字段列表（支持自定义每批次返回数量）
     *
     * @param hash    Hash 键
     * @param pattern 字段匹配模式，支持通配符
     * @param count   每批次 scan 数量（建议 >= 100）
     * @return 匹配的字段列表
     */
    @Override
    public Set<String> scanHashKeys(String hash, String pattern, int count) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(count).build();

        try (Cursor<Map.Entry<Object, Object>> cursor =
                     redisTemplate.opsForHash().scan(hash, options)) {
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                Object field = entry.getKey();
                if (field instanceof String) {
                    result.add((String) field);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("扫描 Hash 字段时出错，hash: " + hash + ", pattern: " + pattern + ", count: " + count);
        }

        return result;
    }

    /**
     * 获取指定 key 的类型（string、list、hash、set、zset 等）
     *
     * @param key 要查询的 key
     * @return 类型字符串（如："string"、"list"）
     */
    @Override
    public String type(String key) {
        DataType dataType = redisTemplate.type(key);
        return dataType != null ? dataType.code() : "none";
    }

    /**
     * 获取 Redis 中的随机一个 key
     *
     * @return 随机 key 或 null（若数据库为空）
     */
    @Override
    public String randomKey() {
        byte[] rawKey = redisTemplate.getConnectionFactory().getConnection().randomKey();
        return rawKey != null ? redisTemplate.getStringSerializer().deserialize(rawKey) : null;
    }

    // ---------------------------------- Hash（哈希表）操作 ----------------------------------

    /**
     * 设置 Hash 中指定字段的值，如果字段已存在则会覆盖。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param value   值
     */
    @Override
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 设置 Hash 中指定字段的值，并设置主键过期时间（秒）。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param value   值
     * @param timeout 过期时间（单位：秒）
     */
    @Override
    public void hSet(String key, String hashKey, Object value, long timeout) {
        hSet(key, hashKey, value);
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置 Hash 字段的值，并设置过期时间（支持自定义时间单位）。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位（如 TimeUnit.SECONDS）
     */
    @Override
    public void hSet(String key, String hashKey, Object value, long timeout, TimeUnit unit) {
        hSet(key, hashKey, value);
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 一次性设置多个 Hash 字段及其对应的值。
     *
     * @param key Redis 键
     * @param map 字段值映射表
     */
    @Override
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 一次性设置多个 Hash 字段及其对应的值，并设置过期时间。
     *
     * @param key     Redis 键
     * @param map     字段值映射表
     * @param timeout 过期时间（单位：秒）
     */
    @Override
    public void hSetAll(String key, Map<String, Object> map, long timeout) {
        hSetAll(key, map);
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 一次性设置多个 Hash 字段，并设置过期时间（支持自定义时间单位）。
     *
     * @param key     Redis 键
     * @param map     字段值映射
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    @Override
    public void hSetAll(String key, Map<String, Object> map, long timeout, TimeUnit unit) {
        hSetAll(key, map);
        redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取指定 Hash 字段的值（返回原始对象）。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @return 字段对应的值
     */
    @Override
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取指定 Hash 字段的值，并自动转换为指定类型。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param clazz   目标类型类对象
     * @return 指定类型的字段值
     */
    @Override
    public <T> T hGet(String key, String hashKey, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        return convertValue(value, clazz);
    }

    /**
     * 批量获取 Hash 中多个字段的值。
     *
     * @param key      Redis 键
     * @param hashKeys 字段名集合
     * @return 字段值列表
     */
    @Override
    public List<Object> hMultiGet(String key, Collection<String> hashKeys) {
        return redisTemplate.opsForHash().multiGet(key, new ArrayList<>(hashKeys));
    }

    /**
     * 批量获取 Hash 中多个字段的值，并转换为指定类型。
     *
     * @param key      Redis 键
     * @param hashKeys 字段名集合
     * @param clazz    值的目标类型
     * @return 指定类型的字段值列表
     */
    @Override
    public <T> List<T> hMultiGet(String key, Collection<String> hashKeys, Class<T> clazz) {
        List<Object> values = redisTemplate.opsForHash().multiGet(key, new ArrayList<>(hashKeys));
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(value -> convertValue(value, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 获取整个 Hash 的所有键值对。
     *
     * @param key Redis 键
     * @return Hash 的键值对
     */
    @Override
    public Map<String, Object> hGetAll(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptyMap();
        }

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (CollectionUtils.isEmpty(entries)) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        return result;
    }

    /**
     * 获取整个 Hash 的所有键值对，并将值转换为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 值的目标类型
     * @return Hash 的键值对（值为指定类型）
     */
    @Override
    public <T> Map<String, T> hGetAll(String key, Class<T> clazz) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptyMap();
        }

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (CollectionUtils.isEmpty(entries)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String field = String.valueOf(entry.getKey());
            T value = convertValue(entry.getValue(), clazz);
            result.put(field, value);
        }

        return result;
    }

    /**
     * 判断指定字段是否存在于 Hash 中。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @return 是否存在
     */
    @Override
    public boolean hHasKey(String key, String hashKey) {
        if (ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(hashKey)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, hashKey));
    }

    /**
     * 删除一个或多个 Hash 字段。
     *
     * @param key      Redis 键
     * @param hashKeys 要删除的字段
     * @return 删除的字段数量
     */
    @Override
    public long hDelete(String key, String... hashKeys) {
        if (ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(hashKeys)) {
            return 0L;
        }
        return redisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
    }

    /**
     * 获取 Hash 中字段的数量。
     *
     * @param key Redis 键
     * @return 字段数量
     */
    @Override
    public long hSize(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return 0L;
        }
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * 获取 Hash 中所有字段名称。
     *
     * @param key Redis 键
     * @return 字段名集合
     */
    @Override
    public Set<String> hKeys(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptySet();
        }

        Set<Object> rawKeys = redisTemplate.opsForHash().keys(key);
        if (CollectionUtils.isEmpty(rawKeys)) {
            return Collections.emptySet();
        }

        Set<String> keys = new LinkedHashSet<>(rawKeys.size());
        for (Object rawKey : rawKeys) {
            keys.add(String.valueOf(rawKey));
        }

        return keys;
    }

    /**
     * 获取 Hash 中所有字段值（不指定类型）。
     *
     * @param key Redis 键
     * @return 字段值列表
     */
    @Override
    public List<Object> hValues(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptyList();
        }

        List<Object> values = redisTemplate.opsForHash().values(key);
        if (ObjectUtils.isEmpty(values)) {
            return Collections.emptyList();
        }

        return values;
    }

    /**
     * 获取 Hash 中所有字段值，并转换为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 值的类型
     * @return 字段值列表（指定类型）
     */
    @Override
    public <T> List<T> hValues(String key, Class<T> clazz) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptyList();
        }

        List<Object> values = redisTemplate.opsForHash().values(key);
        if (ObjectUtils.isEmpty(values)) {
            return Collections.emptyList();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 将 Hash 中指定字段值递增指定的 long 值。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param delta   增量值
     * @return 增加后的结果
     */
    @Override
    public long hIncrBy(String key, String hashKey, long delta) {
        if (ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(hashKey)) {
            throw new IllegalArgumentException("key 和 hashKey 不可为空");
        }
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    /**
     * 将 Hash 中指定字段值递增指定的 double 值。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param delta   增量值
     * @return 增加后的结果
     */
    @Override
    public double hIncrByFloat(String key, String hashKey, double delta) {
        if (ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(hashKey)) {
            throw new IllegalArgumentException("key 和 hashKey 不可为空");
        }
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    /**
     * 如果字段不存在则设置值，若存在则不操作。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param value   值
     * @return true 表示设置成功；false 表示字段已存在
     */
    @Override
    public boolean hPutIfAbsent(String key, String hashKey, Object value) {
        if (ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(hashKey)) {
            throw new IllegalArgumentException("key 和 hashKey 不可为空");
        }
        Boolean result = redisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 使用 SCAN 操作匹配 Hash 中字段名（适用于大数据量场景）。
     *
     * @param key     Redis 键
     * @param pattern 字段名匹配模式（支持 * ? [] 通配）
     * @return 匹配到的字段名集合
     */
    @Override
    public Set<String> hScan(String key, String pattern) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptySet();
        }

        Set<String> result = new LinkedHashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern == null ? "*" : pattern)
                // 默认每次扫描 1000 条，可做成配置
                .count(1000)
                .build();

        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(key, options)) {
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                result.add(String.valueOf(entry.getKey()));
            }
        }
        return result;
    }

    // ---------------------------------- List 操作 ----------------------------------

    /**
     * 从左侧插入一个元素
     *
     * @param key   Redis键
     * @param value 要插入的值
     */
    @Override
    public void lPush(String key, Object value) {
        if (ObjectUtils.isEmpty(key) || value == null) {
            return;
        }
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 从右侧插入一个元素
     *
     * @param key   Redis键
     * @param value 要插入的值
     */
    @Override
    public void rPush(String key, Object value) {
        if (ObjectUtils.isEmpty(key) || value == null) {
            return;
        }
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 从左侧批量插入多个元素
     *
     * @param key    Redis键
     * @param values 要插入的值集合
     */
    @Override
    public void lPushAll(String key, Collection<?> values) {
        if (ObjectUtils.isEmpty(key) || CollectionUtils.isEmpty(values)) {
            return;
        }
        redisTemplate.opsForList().leftPushAll(key, values);
    }

    /**
     * 从右侧批量插入多个元素
     *
     * @param key    Redis键
     * @param values 要插入的值集合
     */
    @Override
    public void rPushAll(String key, Collection<?> values) {
        if (ObjectUtils.isEmpty(key) || CollectionUtils.isEmpty(values)) {
            return;
        }
        redisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * 弹出并返回左侧第一个元素
     *
     * @param key Redis键
     * @return 弹出的元素（Object 类型）
     */
    @Override
    public Object lPop(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 弹出并返回左侧第一个元素，并转换为指定类型
     *
     * @param key   Redis键
     * @param clazz 目标类型
     * @return 弹出的元素，已转换为指定类型
     */
    @Override
    public <T> T lPop(String key, Class<T> clazz) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        Object value = redisTemplate.opsForList().leftPop(key);
        return convertValue(value, clazz);
    }

    /**
     * 弹出并返回右侧第一个元素
     *
     * @param key Redis键
     * @return 弹出的元素（Object 类型）
     */
    @Override
    public Object rPop(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 弹出并返回右侧第一个元素，并转换为指定类型
     *
     * @param key   Redis键
     * @param clazz 目标类型
     * @return 弹出的元素，已转换为指定类型
     */
    @Override
    public <T> T rPop(String key, Class<T> clazz) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        Object value = redisTemplate.opsForList().rightPop(key);
        return convertValue(value, clazz);
    }

    /**
     * 获取列表指定区间的所有元素
     *
     * @param key   Redis键
     * @param start 起始索引
     * @param end   结束索引
     * @return 元素列表（Object 类型）
     */
    @Override
    public List<Object> lRange(String key, long start, long end) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptyList();
        }
        List<Object> range = redisTemplate.opsForList().range(key, start, end);
        return range == null ? Collections.emptyList() : range;
    }

    /**
     * 获取列表指定区间的所有元素，并反序列化为指定类型
     *
     * @param key   Redis键
     * @param start 起始索引
     * @param end   结束索引
     * @param clazz 目标类型
     * @return 元素列表（指定类型）
     */
    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptyList();
        }
        List<Object> range = redisTemplate.opsForList().range(key, start, end);
        if (ObjectUtils.isEmpty(range)) {
            return Collections.emptyList();
        }
        return range.stream()
                .map(item -> convertValue(item, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 获取列表长度
     *
     * @param key Redis键
     * @return 列表长度
     */
    @Override
    public long lLen(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return 0L;
        }
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0L;
    }

    /**
     * 获取指定下标的元素
     *
     * @param key   Redis键
     * @param index 索引
     * @return 获取的元素（Object 类型）
     */
    @Override
    public Object lIndex(String key, long index) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * 获取指定下标的元素，并转换为指定类型
     *
     * @param key   Redis键
     * @param index 索引
     * @param clazz 目标类型
     * @return 获取的元素（指定类型）
     */
    @Override
    public <T> T lIndex(String key, long index, Class<T> clazz) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        Object value = redisTemplate.opsForList().index(key, index);
        return convertValue(value, clazz);
    }

    /**
     * 设置指定位置的元素值
     *
     * @param key   Redis键
     * @param index 位置索引
     * @param value 要设置的值
     */
    @Override
    public void lSet(String key, long index, Object value) {
        if (ObjectUtils.isEmpty(key) || value == null) {
            return;
        }
        redisTemplate.opsForList().set(key, index, value);
    }

    /**
     * 裁剪列表，仅保留指定区间的元素
     *
     * @param key   Redis键
     * @param start 起始索引
     * @param end   结束索引
     */
    @Override
    public void lTrim(String key, long start, long end) {
        if (ObjectUtils.isEmpty(key)) {
            return;
        }
        redisTemplate.opsForList().trim(key, start, end);
    }

    /**
     * 移除列表中与指定值相等的元素
     *
     * @param key   Redis键
     * @param count 移除的个数（为 0 表示全部移除）
     * @param value 要移除的值
     * @return 实际移除的元素数量
     */
    @Override
    public long lRemove(String key, long count, Object value) {
        if (ObjectUtils.isEmpty(key) || value == null) {
            return 0;
        }
        Long removed = redisTemplate.opsForList().remove(key, count, value);
        return removed != null ? removed : 0L;
    }

    // ---------------------------------- Set 操作 ----------------------------------

    /**
     * 向集合中添加一个或多个元素（无过期时间）。
     *
     * @param key    Redis 键
     * @param values 要添加的元素
     * @return 添加成功的元素数量
     */
    @Override
    public long sAdd(String key, Object... values) {
        if (ObjectUtils.isEmpty(key) || values == null || values.length == 0) {
            return 0L;
        }
        Long added = redisTemplate.opsForSet().add(key, values);
        return added != null ? added : 0L;
    }

    /**
     * 向集合中添加一个或多个元素，并设置过期时间。
     *
     * @param key     Redis 键
     * @param timeout 超时时间
     * @param unit    时间单位（如 TimeUnit.SECONDS）
     * @param values  要添加的元素
     * @return 添加成功的元素数量
     */
    @Override
    public long sAdd(String key, long timeout, TimeUnit unit, Object... values) {
        if (ObjectUtils.isEmpty(key) || values == null || values.length == 0) {
            return 0L;
        }
        Long added = redisTemplate.opsForSet().add(key, values);
        if (added != null && added > 0 && timeout > 0) {
            redisTemplate.expire(key, timeout, unit);
        }
        return added != null ? added : 0L;
    }

    /**
     * 获取集合中的所有元素。
     *
     * @param key Redis 键
     * @return 所有成员组成的 Set
     */
    @Override
    public Set<Object> sMembers(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptySet();
        }
        Set<Object> members = redisTemplate.opsForSet().members(key);
        return members != null ? members : Collections.emptySet();
    }

    /**
     * 获取集合中的所有元素，并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型的 Class
     * @return 指定类型的 Set
     */
    @Override
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptySet();
        }
        Set<Object> members = redisTemplate.opsForSet().members(key);
        return convertToSet(members, clazz);
    }

    /**
     * 判断元素是否是集合中的成员。
     *
     * @param key   Redis 键
     * @param value 要判断的元素
     * @return true 表示存在；false 表示不存在
     */
    @Override
    public boolean sIsMember(String key, Object value) {
        if (ObjectUtils.isEmpty(key) || value == null) {
            return false;
        }
        Boolean isMember = redisTemplate.opsForSet().isMember(key, value);
        return Boolean.TRUE.equals(isMember);
    }

    /**
     * 从集合中移除一个或多个元素。
     *
     * @param key    Redis 键
     * @param values 要移除的元素
     * @return 实际移除的元素个数
     */
    @Override
    public long sRemove(String key, Object... values) {
        if (ObjectUtils.isEmpty(key) || values == null || values.length == 0) {
            return 0L;
        }
        Long removed = redisTemplate.opsForSet().remove(key, values);
        return removed != null ? removed : 0L;
    }

    /**
     * 随机弹出并移除集合中的一个元素。
     *
     * @param key Redis 键
     * @return 被移除的元素
     */
    @Override
    public Object sPop(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return null;
        }
        return redisTemplate.opsForSet().pop(key);
    }

    /**
     * 随机弹出并移除集合中的一个元素，并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型的 Class
     * @return 被移除的指定类型元素
     */
    @Override
    public <T> T sPop(String key, Class<T> clazz) {
        Object value = sPop(key);
        return convertValue(value, clazz);
    }

    /**
     * 获取集合中元素的数量。
     *
     * @param key Redis 键
     * @return 集合大小
     */
    @Override
    public long sSize(String key) {
        if (ObjectUtils.isEmpty(key)) {
            return 0L;
        }
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }

    /**
     * 将集合元素转换为指定类型的 Set。
     *
     * @param source 原始集合
     * @param clazz  目标类型 Class
     * @param <T>    目标类型
     * @return 转换后的 Set
     */
    @Override
    public <T> Set<T> convertToSet(Collection<?> source, Class<T> clazz) {
        return convertToCollection(source, clazz, new HashSet<>());
    }

    /**
     * 将集合元素转换为指定类型的 List。
     *
     * @param source 原始集合
     * @param clazz  目标类型 Class
     * @param <T>    目标类型
     * @return 转换后的 List
     */
    @Override
    public <T> List<T> convertToList(Collection<?> source, Class<T> clazz) {
        return convertToCollection(source, clazz, new ArrayList<>());
    }

    /**
     * 将原始集合转换为指定类型的集合（List 或 Set）。
     *
     * @param source     原始集合（可能为 null）
     * @param clazz      目标类型 Class
     * @param collection 创建好的目标集合实例（如 new ArrayList<>(), new HashSet<>())
     * @param <T>        目标类型
     * @param <C>        返回集合类型（List 或 Set）
     * @return 转换后的集合（若 source 为空则返回空集合）
     */
    @Override
    public <T, C extends Collection<T>> C convertToCollection(Collection<?> source, Class<T> clazz, C collection) {
        if (source == null || source.isEmpty()) {
            return collection;
        }
        for (Object o : source) {
            collection.add(convertValue(o, clazz));
        }
        return collection;
    }

    /**
     * 获取两个集合的并集。
     *
     * @param key1 第一个集合
     * @param key2 第二个集合
     * @return 并集结果
     */
    @Override
    public Set<Object> sUnion(String key1, String key2) {
        if (ObjectUtils.isEmpty(key1) || ObjectUtils.isEmpty(key2)) {
            return Collections.emptySet();
        }
        Set<Object> result = redisTemplate.opsForSet().union(key1, key2);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * 获取两个集合的并集，并反序列化为指定类型。
     *
     * @param key1  第一个集合的 Redis 键
     * @param key2  第二个集合的 Redis 键
     * @param clazz 目标类型 Class
     * @return 并集结果（指定类型的 Set）
     */
    @Override
    public <T> Set<T> sUnion(String key1, String key2, Class<T> clazz) {
        Set<Object> unionSet = sUnion(key1, key2);
        return convertToSet(unionSet, clazz);
    }

    /**
     * 获取两个集合的交集。
     *
     * @param key1 第一个集合
     * @param key2 第二个集合
     * @return 交集结果
     */
    @Override
    public Set<Object> sIntersect(String key1, String key2) {
        if (ObjectUtils.isEmpty(key1) || ObjectUtils.isEmpty(key2)) {
            return Collections.emptySet();
        }
        Set<Object> result = redisTemplate.opsForSet().intersect(key1, key2);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * 获取两个集合的交集，并反序列化为指定类型。
     *
     * @param key1  第一个集合的 Redis 键
     * @param key2  第二个集合的 Redis 键
     * @param clazz 目标类型 Class
     * @return 交集结果（指定类型的 Set）
     */
    @Override
    public <T> Set<T> sIntersect(String key1, String key2, Class<T> clazz) {
        Set<Object> intersectSet = sIntersect(key1, key2);
        return convertToSet(intersectSet, clazz);
    }

    /**
     * 获取两个集合的差集（key1 - key2）。
     *
     * @param key1 第一个集合
     * @param key2 第二个集合
     * @return 差集结果
     */
    @Override
    public Set<Object> sDifference(String key1, String key2) {
        if (ObjectUtils.isEmpty(key1) || ObjectUtils.isEmpty(key2)) {
            return Collections.emptySet();
        }
        Set<Object> result = redisTemplate.opsForSet().difference(key1, key2);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * 获取两个集合的差集（key1 - key2），并反序列化为指定类型。
     *
     * @param key1  第一个集合的 Redis 键
     * @param key2  第二个集合的 Redis 键
     * @param clazz 目标类型 Class
     * @return 差集结果（指定类型的 Set）
     */
    @Override
    public <T> Set<T> sDifference(String key1, String key2, Class<T> clazz) {
        Set<Object> diffSet = sDifference(key1, key2);
        return convertToSet(diffSet, clazz);
    }

    /**
     * 随机获取集合中的一个元素（不移除）。
     *
     * @param key Redis 键
     * @return 随机的元素
     */
    @Override
    public Object sRandMember(String key) {
        return redisTemplate.opsForSet().randomMember(key);
    }

    /**
     * 随机获取集合中的一个元素（不移除），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型 Class
     * @return 随机元素（指定类型）
     */
    @Override
    public <T> T sRandMember(String key, Class<T> clazz) {
        Object value = sRandMember(key);
        return convertValue(value, clazz);
    }

    /**
     * 随机获取集合中的多个元素（可能重复，且不移除）。
     *
     * @param key   Redis 键
     * @param count 获取的数量
     * @return 随机元素组成的列表
     */
    @Override
    public List<Object> sRandMember(String key, int count) {
        List<Object> values = redisTemplate.opsForSet().randomMembers(key, count);
        return values == null ? Collections.emptyList() : values;
    }

    /**
     * 随机获取集合中的多个元素（可能重复，且不移除），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param count 获取的数量
     * @param clazz 目标类型 Class
     * @return 随机元素列表（指定类型）
     */
    @Override
    public <T> List<T> sRandMember(String key, int count, Class<T> clazz) {
        List<Object> values = sRandMember(key, count);
        return convertToList(values, clazz);
    }

    /**
     * 将一个元素从 sourceKey 移动到 destKey。
     *
     * @param sourceKey 原集合 key
     * @param value     要移动的元素
     * @param destKey   目标集合 key
     * @return true 表示移动成功；false 表示元素不存在或移动失败
     */
    @Override
    public boolean sMove(String sourceKey, Object value, String destKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().move(sourceKey, value, destKey));
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------- Set 操作 ----------------------------------

    /**
     * 向有序集合中添加一个元素及其分数，若元素已存在则更新分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param score 分数
     * @return 是否成功添加（新元素返回 true，更新分数返回 false）
     */
    @Override
    public boolean zAdd(String key, Object value, double score) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 批量向有序集合中添加多个元素及其分数。
     *
     * @param key           Redis 键
     * @param valueScoreMap 元素-分数映射
     * @return 添加成功的元素数量
     */
    @Override
    public long zAdd(String key, Map<Object, Double> valueScoreMap) {
        if (ObjectUtils.isEmpty(valueScoreMap)) {
            return 0L;
        }
        try {
            Set<ZSetOperations.TypedTuple<Object>> tuples = valueScoreMap.entrySet().stream()
                    .map(entry -> new DefaultTypedTuple<>(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet());
            Long count = redisTemplate.opsForZSet().add(key, tuples);
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 按索引区间正序获取有序集合中的元素（分数从低到高）。
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引（-1 表示末尾）
     * @return 元素列表（Object 类型）
     */
    @Override
    public Set<Object> zRange(String key, long start, long end) {
        try {
            Set<Object> range = redisTemplate.opsForZSet().range(key, start, end);
            return range != null ? range : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按索引区间正序获取有序集合中的元素（分数从低到高），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引（-1 表示末尾）
     * @param clazz 目标类型 Class
     * @return 指定类型的元素集合（有序）
     */
    @Override
    public <T> Set<T> zRange(String key, long start, long end, Class<T> clazz) {
        try {
            Set<Object> range = redisTemplate.opsForZSet().range(key, start, end);
            return convertToSet(range, clazz);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按索引区间倒序获取有序集合中的元素（分数从高到低）。
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引
     * @return 元素列表（Object 类型）
     */
    @Override
    public Set<Object> zRevRange(String key, long start, long end) {
        try {
            Set<Object> range = redisTemplate.opsForZSet().reverseRange(key, start, end);
            return range != null ? range : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按索引区间倒序获取有序集合中的元素（分数从高到低），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引
     * @param clazz 目标类型 Class
     * @return 指定类型的元素集合（有序）
     */
    @Override
    public <T> Set<T> zRevRange(String key, long start, long end, Class<T> clazz) {
        try {
            Set<Object> range = redisTemplate.opsForZSet().reverseRange(key, start, end);
            return convertToSet(range, clazz);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按分数区间获取有序集合中的元素（分数从低到高）。
     *
     * @param key Redis 键
     * @param min 最小分数（闭区间）
     * @param max 最大分数（闭区间）
     * @return 元素列表（Object 类型）
     */
    @Override
    public Set<Object> zRangeByScore(String key, double min, double max) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().rangeByScore(key, min, max);
            return result != null ? result : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按分数区间获取有序集合中的元素（分数从低到高），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param min   最小分数（闭区间）
     * @param max   最大分数（闭区间）
     * @param clazz 目标类型 Class
     * @return 指定类型的元素集合（有序）
     */
    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, Class<T> clazz) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().rangeByScore(key, min, max);
            return convertToSet(result, clazz);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按分数区间分页获取有序集合中的元素（分数从低到高）。
     *
     * @param key    Redis 键
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量
     * @param count  返回数量
     * @return 元素列表（Object 类型）
     */
    @Override
    public Set<Object> zRangeByScore(String key, double min, double max, long offset, long count) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().rangeByScore(key, min, max, offset, count);
            return result != null ? result : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按分数区间分页获取有序集合中的元素（分数从低到高），并反序列化为指定类型。
     *
     * @param key    Redis 键
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量
     * @param count  返回数量
     * @param clazz  目标类型 Class
     * @return 指定类型的元素集合（有序）
     */
    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, long offset, long count, Class<T> clazz) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().rangeByScore(key, min, max, offset, count);
            return convertToSet(result, clazz);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按分数区间倒序获取有序集合中的元素（分数从高到低）。
     *
     * @param key Redis 键
     * @param max 最大分数（闭区间）
     * @param min 最小分数（闭区间）
     * @return 元素列表（Object 类型）
     */
    @Override
    public Set<Object> zRevRangeByScore(String key, double max, double min) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
            return result != null ? result : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按分数区间倒序获取有序集合中的元素（分数从高到低），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param max   最大分数（闭区间）
     * @param min   最小分数（闭区间）
     * @param clazz 目标类型 Class
     * @return 指定类型的元素集合（有序）
     */
    @Override
    public <T> Set<T> zRevRangeByScore(String key, double max, double min, Class<T> clazz) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
            return convertToSet(result, clazz);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按分数区间倒序分页获取有序集合中的元素（分数从高到低）。
     *
     * @param key    Redis 键
     * @param max    最大分数
     * @param min    最小分数
     * @param offset 偏移量
     * @param count  返回数量
     * @return 元素列表（Object 类型）
     */
    @Override
    public Set<Object> zRevRangeByScore(String key, double max, double min, long offset, long count) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count);
            return result != null ? result : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 按分数区间倒序分页获取有序集合中的元素（分数从高到低），并反序列化为指定类型。
     *
     * @param key    Redis 键
     * @param max    最大分数
     * @param min    最小分数
     * @param offset 偏移量
     * @param count  返回数量
     * @param clazz  目标类型 Class
     * @return 指定类型的元素集合（有序）
     */
    @Override
    public <T> Set<T> zRevRangeByScore(String key, double max, double min, long offset, long count, Class<T> clazz) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count);
            return convertToSet(result, clazz);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * 获取元素在有序集合中的排名（按分数升序，从0开始）。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名，元素不存在返回 null
     */
    @Override
    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    /**
     * 获取元素在有序集合中的排名（按分数降序，从0开始）。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名，元素不存在返回 null
     */
    @Override
    public Long zRevRank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    /**
     * 删除有序集合中的一个或多个元素。
     *
     * @param key    Redis 键
     * @param values 要删除的元素
     * @return 实际删除的元素数量
     */
    @Override
    public long zRemove(String key, Object... values) {
        Long removed = redisTemplate.opsForZSet().remove(key, values);
        return removed != null ? removed : 0L;
    }

    /**
     * 获取有序集合中指定分数范围内的元素数量。
     *
     * @param key Redis 键
     * @param min 最小分数（闭区间）
     * @param max 最大分数（闭区间）
     * @return 元素数量
     */
    @Override
    public long zCount(String key, double min, double max) {
        Long count = redisTemplate.opsForZSet().count(key, min, max);
        return count != null ? count : 0L;
    }

    /**
     * 获取有序集合中指定元素的分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 分数，元素不存在返回 null
     */
    @Override
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 给指定元素的分数增加 delta（可正可负）。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param delta 增量
     * @return 增加后的分数
     */
    @Override
    public Double zIncrScore(String key, Object value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 获取有序集合中的元素数量。
     *
     * @param key Redis 键
     * @return 元素数量
     */
    @Override
    public long zSize(String key) {
        Long size = redisTemplate.opsForZSet().size(key);
        return size != null ? size : 0L;
    }

    /**
     * 使用 Scan 命令迭代匹配有序集合中的元素，适合大数据量扫描。
     *
     * @param key     Redis 键
     * @param pattern 匹配模式，支持通配符（如 *）
     * @return 匹配的元素集合（Object 类型）
     */
    @Override
    public Set<Object> zScan(String key, String pattern) {
        if (ObjectUtils.isEmpty(key)) {
            return Collections.emptySet();
        }
        Set<Object> result = new HashSet<>();
        Cursor<ZSetOperations.TypedTuple<Object>> cursor = redisTemplate.opsForZSet().scan(key, ScanOptions.scanOptions().match(pattern).count(1000).build());
        while (cursor.hasNext()) {
            ZSetOperations.TypedTuple<Object> tuple = cursor.next();
            if (tuple != null) {
                result.add(tuple.getValue());
            }
        }
        cursor.close();
        return result;
    }

    /**
     * 使用 Scan 命令迭代匹配有序集合中的元素，反序列化为指定类型。
     *
     * @param key     Redis 键
     * @param pattern 匹配模式，支持通配符（如 *）
     * @param clazz   目标类型 Class
     * @return 匹配的指定类型元素集合
     */
    @Override
    public <T> Set<T> zScan(String key, String pattern, Class<T> clazz) {
        Set<Object> rawSet = zScan(key, pattern);
        return convertToSet(rawSet, clazz);
    }

    // ---------------------------------- 高级功能 ----------------------------------

    // ------------------ 分布式锁 ------------------

    /**
     * 释放锁的Lua脚本
     */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then return redis.call('del', KEYS[1]) else return 0 end";
    /**
     * 请求标识（建议使用 UUID，保证释放锁时是自己的锁）
     */
    private final ThreadLocal<String> requestIdHolder = new ThreadLocal<>();

    /**
     * 生成并保存一个请求 id（UUID）到当前线程，用于后续释放锁时验证 ownership。
     *
     * @return 请求 id
     */
    private String generateRequestId() {
        String id = UUID.randomUUID().toString();
        requestIdHolder.set(id);
        return id;
    }

    /**
     * 尝试获取锁（一直等待，最多30秒）
     *
     * @param key 锁 key
     * @return true 成功获取锁；false 失败
     */
    @Override
    public boolean tryLock(String key) {
        long waitTime = 30;
        long leaseTime = waitTime * 2;
        return tryLock(key, waitTime, leaseTime, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取锁（立即返回）
     *
     * @param key       锁 key
     * @param leaseTime 锁的生存时间
     * @param unit      时间单位
     * @return true 成功获取锁；false 失败
     */
    @Override
    public boolean tryLock(String key, long leaseTime, TimeUnit unit) {
        String reqId = requestIdHolder.get();
        if (reqId == null) {
            reqId = generateRequestId();
        }
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, reqId, leaseTime, unit);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 尝试获取锁（支持等待）
     *
     * @param key       锁 key
     * @param waitTime  最长等待时间
     * @param leaseTime 锁的生存时间
     * @param unit      时间单位
     * @return true 成功获取锁；false 失败
     */
    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        long endTime = System.nanoTime() + unit.toNanos(waitTime);
        String reqId = requestIdHolder.get();
        if (reqId == null) {
            reqId = generateRequestId();
        }
        long sleepTime = 50;

        while (System.nanoTime() < endTime) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, reqId, leaseTime, unit);
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
            try {
                // 等待一会再尝试，避免占满 CPU
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 原子释放锁：只有持有相同 requestId 的线程才会成功删除 key
     *
     * @param key 锁 key
     * @return true 释放成功；false 未释放（可能不是当前持有者或锁已过期）
     */
    @Override
    public boolean unlock(String key) {
        String reqId = requestIdHolder.get();
        if (reqId == null) {
            return false;
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_LUA, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), reqId);
        requestIdHolder.remove();
        return result != null && result > 0;
    }

    /**
     * 获取分布式锁对象
     *
     * @param name 锁名称
     * @return RLock 实例
     */
    @Override
    public RLock getLock(String name) {
        class RLockImpl implements RLock {
            private final String key;
            private final RedisService redisService;

            RLockImpl(String key, RedisService redisService) {
                this.key = key;
                this.redisService = redisService;
            }

            @Override
            public void lock() {
                redisService.tryLock(key);
            }

            @Override
            public void lock(long leaseTime, TimeUnit unit) {
                redisService.tryLock(key, leaseTime, unit);
            }

            @Override
            public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
                return redisService.tryLock(key, waitTime, leaseTime, unit);
            }

            @Override
            public boolean tryLock(long leaseTime, TimeUnit unit) {
                return redisService.tryLock(key, leaseTime, unit);
            }

            @Override
            public CompletableFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit) {
                return CompletableFuture.supplyAsync(() -> tryLock(waitTime, leaseTime, unit));
            }

            @Override
            public boolean unlock() {
                return redisService.unlock(key);
            }

            @Override
            public void forceUnlock() {
                redisService.delete(key);
            }
        }

        return new RLockImpl(name, this);
    }

    /**
     * 获取分布式锁对象（加强版，支持自动锁续期）
     *
     * @param name 锁名称
     * @return RLock 实例
     */
    @Override
    public RLock getLockPlus(String name) {
        return redisLockService.getLock(name);
    }

    // ------------------ 计数器操作 ------------------

    /**
     * 执行递增/递减操作的统一私有方法（支持整数和浮点数）
     *
     * @param key   Redis 键
     * @param delta 步长，可以为负值（表示递减）
     * @return 增加后的数值（Long 或 Double）
     */
    private Number doIncrement(String key, Number delta) {
        if (ObjectUtils.isEmpty(key) || delta == null || redisTemplate == null) {
            return 0;
        }

        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        if (delta instanceof Long) {
            return ops.increment(key, delta.longValue());
        } else if (delta instanceof Double) {
            return ops.increment(key, delta.doubleValue());
        }

        throw new IllegalArgumentException("Unsupported increment type: " + delta.getClass());
    }

    /**
     * 对指定 key 执行原子递增操作，步长为 1。
     *
     * @param key Redis 键
     * @return 递增后的值
     */
    @Override
    public long incr(String key) {
        return doIncrement(key, 1L).longValue();
    }

    /**
     * 对指定 key 执行原子递增操作，步长为指定的 delta。
     *
     * @param key   Redis 键
     * @param delta 增量值（必须大于 0）
     * @return 递增后的值
     */
    @Override
    public long incrBy(String key, long delta) {
        return (delta <= 0) ? 0L : doIncrement(key, delta).longValue();
    }

    /**
     * 对指定 key 执行原子递减操作，步长为 1。
     *
     * @param key Redis 键
     * @return 递减后的值
     */
    @Override
    public long decr(String key) {
        return doIncrement(key, -1L).longValue();
    }

    /**
     * 对指定 key 执行原子递减操作，步长为指定的 delta。
     *
     * @param key   Redis 键
     * @param delta 减量值（必须大于 0）
     * @return 递减后的值
     */
    @Override
    public long decrBy(String key, long delta) {
        return (delta <= 0) ? 0L : doIncrement(key, -delta).longValue();
    }

    /**
     * 对指定 key 执行原子递增操作，支持浮点数增量。
     *
     * @param key   Redis 键
     * @param delta 增量值（可以是小数）
     * @return 递增后的浮点数值
     */
    @Override
    public double incrByFloat(String key, double delta) {
        return doIncrement(key, delta).doubleValue();
    }

    /**
     * 对指定 key 执行原子递减操作，支持浮点数减量。
     *
     * @param key   Redis 键
     * @param delta 减量值（可以是小数）
     * @return 递减后的浮点数值
     */
    @Override
    public double decrByFloat(String key, double delta) {
        return doIncrement(key, -delta).doubleValue();
    }

    // ------------------ 发布订阅 ------------------

    /**
     * 实际的消息发布逻辑，支持对象序列化。
     *
     * @param channel 发布频道
     * @param message 消息内容
     */
    private void doPublish(String channel, Object message) {
        if (ObjectUtils.isEmpty(channel) || message == null) {
            return;
        }
        redisTemplate.convertAndSend(channel, message);
    }

    /**
     * 向指定频道发布消息。
     *
     * @param channel 频道名
     * @param message 消息内容
     */
    @Override
    public void publish(String channel, Object message) {
        doPublish(channel, message);
    }

    /**
     * 支持延迟发布消息（业务层配合实现延时机制）。
     *
     * @param channel     频道名
     * @param message     消息内容
     * @param delayMillis 延迟毫秒数
     */
    @Override
    public void publish(String channel, Object message, long delayMillis) {
        if (delayMillis <= 0) {
            doPublish(channel, message);
        } else {
            Executors.newSingleThreadScheduledExecutor().schedule(() ->
                    doPublish(channel, message), delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 异步发布消息，避免阻塞调用线程。
     *
     * @param channel 频道名
     * @param message 消息内容
     */
    @Override
    public void publishAsync(String channel, Object message) {
        CompletableFuture.runAsync(() -> doPublish(channel, message));
    }

    // --------------------- Lua 脚本操作 ---------------------

    /**
     * 在 Redis 中执行 Lua 脚本（返回单一结果）。
     *
     * @param script     Lua 脚本内容（例如 "return redis.call('set', KEYS[1], ARGV[1])"）
     * @param returnType 返回值类型（用于指定 Redis 返回的数据类型，如 Boolean、Long、String、List 等）
     * @param keys       脚本中需要用到的 KEYS 参数（如 KEYS[1]、KEYS[2]）
     * @param args       脚本中需要用到的 ARGV 参数（如 ARGV[1]、ARGV[2]）
     * @return 执行结果
     * <p>
     * 核心逻辑：
     * 1. 使用 RScript 对象执行 Lua 脚本
     * 2. RScript.Mode.READ_WRITE 表示既能读也能写（一般 Lua 脚本会修改数据）
     * 3. StringCodec 用于将 Redis 数据以字符串方式编码/解码
     * 4. RScript.ReturnType.VALUE 表示返回单一值（也可以改为 MULTI、BOOLEAN 等）
     * 5. keys 是脚本的 KEYS 数组，args 是 ARGV 数组
     */
    @Override
    public <T> T eval(String script, Class<T> returnType, List<String> keys, Object... args) {
        Objects.requireNonNull(script, "Lua script must not be null");
        Objects.requireNonNull(returnType, "Return type must not be null");

        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(returnType);

        // 执行 Lua 脚本
        return redisTemplate.execute(redisScript, keys, args);
    }

    /**
     * 执行 Lua 脚本但不返回结果。
     *
     * @param script Lua 脚本内容
     * @param keys   脚本中的 KEYS
     * @param args   脚本中的 ARGV
     *               <p>
     *               核心逻辑：
     *               1. 使用 RScript.eval 执行 Lua 脚本
     *               2. RScript.ReturnType.VALUE 用于兼容调用，但结果不保存
     *               3. 常用于只修改 Redis 数据但不关心返回值的场景
     */
    @Override
    public void evalNoResult(String script, List<String> keys, Object... args) {
        Objects.requireNonNull(script, "Lua script must not be null");
        DefaultRedisScript<Object> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Object.class);

        redisTemplate.execute(redisScript, keys, args);
    }

    /**
     * 通过 SHA1 执行已加载的 Lua 脚本，并返回指定类型结果。
     *
     * @param sha1       Lua 脚本的 SHA1
     * @param returnType 返回类型 Class
     * @param keys       脚本中的 KEYS
     * @param values     脚本中的 ARGV
     * @return 脚本执行结果
     * <p>
     * 核心逻辑：
     * 1. 使用 RScript.evalSha 执行 Redis 缓存的 Lua 脚本
     * 2. 避免重复传输脚本内容，提高性能
     */
    @Override
    public <T> T evalBySha(String sha1, Class<T> returnType, List<String> keys, Object... values) {
        RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();

        return redisTemplate.execute((RedisConnection connection) ->
                connection.evalSha(
                        sha1.getBytes(StandardCharsets.UTF_8),
                        getReturnType(returnType),
                        keys.size(),
                        serializeArgs(keys, values, stringSerializer)
                ), false, true);
    }

    /**
     * 将 Lua 脚本加载到 Redis，并返回脚本的 SHA1 值。
     *
     * <p>适用于需要多次执行同一脚本的场景，结合 {@link #evalBySha(String, Class, List, Object...)} 可减少传输和解析开销。</p>
     *
     * @param script Lua 脚本内容
     * @return 脚本在 Redis 中的 SHA1 摘要
     *
     * <p>关键代码说明：</p>
     * <ul>
     *     <li>底层执行 {@code SCRIPT LOAD} 命令，将脚本缓存到 Redis 端</li>
     *     <li>返回 SHA1 值可直接用于后续的 {@code EVALSHA} 调用</li>
     * </ul>
     */
    @Override
    public String loadScript(String script) {
        return redisTemplate.execute((RedisCallback<String>) connection ->
                connection.scriptingCommands().scriptLoad(script.getBytes(StandardCharsets.UTF_8))
        );
    }

    /**
     * 根据类型映射到 Redis 的 ReturnType
     */
    private ReturnType getReturnType(Class<?> returnType) {
        if (returnType == String.class) {
            return ReturnType.VALUE;
        } else if (Number.class.isAssignableFrom(returnType)) {
            return ReturnType.INTEGER;
        } else if (returnType == Boolean.class) {
            return ReturnType.BOOLEAN;
        } else if (returnType == List.class) {
            return ReturnType.MULTI;
        }
        return ReturnType.VALUE;
    }

    /**
     * 序列化 key 和参数
     */
    private byte[][] serializeArgs(List<String> keys, Object[] args, RedisSerializer<String> stringSerializer) {
        byte[][] result = new byte[keys.size() + args.length][];
        int i = 0;
        for (String key : keys) {
            result[i++] = stringSerializer.serialize(String.valueOf(key));
        }
        for (Object arg : args) {
            result[i++] = stringSerializer.serialize(String.valueOf(arg));
        }
        return result;
    }

}
