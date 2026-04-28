package local.ateng.java.redis.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.redis.service.RedisTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * RedisTemplate 通用操作服务实现类。
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTemplateServiceImpl implements RedisTemplateService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    private static final long DEFAULT_LOCK_RETRY_INTERVAL_MILLIS = 100L;

    private static final String COMPARE_AND_DELETE_LUA_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """;

    private static final String COMPARE_AND_SET_LUA_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                redis.call('set', KEYS[1], ARGV[2])
                local ttl = tonumber(ARGV[3])
                if ttl ~= nil and ttl > 0 then
                    redis.call('expire', KEYS[1], ttl)
                end
                return 1
            else
                return 0
            end
            """;

    private static final String INCREMENT_AND_EXPIRE_LUA_SCRIPT = """
            local value = redis.call('incrby', KEYS[1], ARGV[1])
            local ttl = tonumber(ARGV[2])
            if ttl ~= nil and ttl > 0 then
                redis.call('expire', KEYS[1], ttl)
            end
            return value
            """;

    @Override
    public <T> T convertValue(Object value, Class<T> clazz) {
        if (ObjectUtil.isNull(value) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        try {
            if (value instanceof String text) {
                if (String.class.equals(clazz)) {
                    return clazz.cast(text);
                }

                // Redis 中常见字符串值通常是 JSON，这里优先按 JSON 反序列化
                return objectMapper.readValue(text, clazz);
            }

            // 非字符串对象通常来自 Jackson 反序列化后的 Map/List 结构，使用 convertValue 进行二次转换
            return objectMapper.convertValue(value, clazz);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.warn("Redis 值类型转换失败，targetType={}，valueType={}",
                    clazz.getName(), value.getClass().getName(), e);
            return null;
        }
    }

    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeReference) {
        if (ObjectUtil.isNull(value) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        try {
            if (value instanceof String text) {
                try {
                    // 泛型类型优先按 JSON 字符串解析，适用于 List<User>、Map<String, User> 等结构
                    return objectMapper.readValue(text, typeReference);
                } catch (JsonProcessingException ignored) {
                    // 字符串不是合法 JSON 时，回退到 Jackson convertValue，兼容 String/Object 等简单接收类型
                    return objectMapper.convertValue(value, typeReference);
                }
            }

            // 非字符串对象直接交给 Jackson 转换，适用于 LinkedHashMap -> DTO、List<Map> -> List<DTO>
            return objectMapper.convertValue(value, typeReference);
        } catch (IllegalArgumentException e) {
            log.warn("Redis 值泛型转换失败，targetType={}，valueType={}",
                    typeReference.getType(), value.getClass().getName(), e);
            return null;
        }
    }

    @Override
    public boolean hasKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public boolean delete(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public long delete(Collection<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return 0L;
        }

        List<String> validKeys = keys.stream()
                .filter(StrUtil::isNotBlank)
                .toList();

        if (CollUtil.isEmpty(validKeys)) {
            return 0L;
        }

        Long count = redisTemplate.delete(validKeys);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public boolean expire(String key, Duration timeout) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(timeout) || timeout.isNegative() || timeout.isZero()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
    }

    @Override
    public boolean expireAt(String key, Instant expireAt) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(expireAt)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.expireAt(key, expireAt));
    }

    @Override
    public boolean persist(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    @Override
    public Duration getExpire(String key) {
        if (StrUtil.isBlank(key)) {
            return Duration.ofMillis(-2);
        }

        Long millis = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);

        // Redis TTL 语义：-1 表示永久有效，-2 表示 Key 不存在
        if (ObjectUtil.isNull(millis)) {
            return Duration.ofMillis(-2);
        }

        return Duration.ofMillis(millis);
    }

    @Override
    public Set<String> scanKeys(String pattern) {
        return scanKeys(pattern, 1000);
    }

    @Override
    public Set<String> scanKeys(String pattern, long count) {
        if (StrUtil.isBlank(pattern)) {
            return Set.of();
        }

        long scanCount = count > 0 ? count : 1000;

        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new LinkedHashSet<>();

            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(scanCount)
                    .build();

            // 使用 SCAN 分批扫描，避免 Redis KEYS 命令在大 Key 空间下阻塞服务
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    String key = deserializeKey(cursor.next());
                    if (StrUtil.isNotBlank(key)) {
                        result.add(key);
                    }
                }
            } catch (Exception e) {
                log.warn("Redis Key 扫描失败，pattern={}，count={}", pattern, scanCount, e);
            }

            return result;
        });

        return ObjectUtil.defaultIfNull(keys, Set.of());
    }

    private String deserializeKey(byte[] keyBytes) {
        if (ObjectUtil.isNull(keyBytes)) {
            return null;
        }

        try {
            RedisSerializer<?> keySerializer = redisTemplate.getKeySerializer();
            if (ObjectUtil.isNotNull(keySerializer)) {
                Object key = keySerializer.deserialize(keyBytes);
                return ObjectUtil.isNull(key) ? null : key.toString();
            }
        } catch (Exception e) {
            log.warn("Redis Key 反序列化失败，使用 UTF-8 字符串兜底", e);
        }

        return new String(keyBytes, StandardCharsets.UTF_8);
    }

    @Override
    public void set(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, Object value, Duration timeout) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return;
        }

        if (ObjectUtil.isNull(timeout) || timeout.isNegative() || timeout.isZero()) {
            set(key, value);
            return;
        }

        redisTemplate.opsForValue().set(key, value, timeout);
    }

    @Override
    public boolean setIfAbsent(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value));
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration timeout) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return false;
        }

        if (ObjectUtil.isNull(timeout) || timeout.isNegative() || timeout.isZero()) {
            return setIfAbsent(key, value);
        }

        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
    }

    @Override
    public boolean setIfPresent(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfPresent(key, value));
    }

    @Override
    public boolean setIfPresent(String key, Object value, Duration timeout) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return false;
        }

        if (ObjectUtil.isNull(timeout) || timeout.isNegative() || timeout.isZero()) {
            return setIfPresent(key, value);
        }

        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfPresent(key, value, timeout));
    }

    @Override
    public Object get(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        return convertValue(value, clazz);
    }

    @Override
    public <T> T get(String key, TypeReference<T> typeReference) {
        Object value = get(key);
        return convertValue(value, typeReference);
    }

    @Override
    public Object getAndSet(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }
        return redisTemplate.opsForValue().getAndSet(key, value);
    }

    @Override
    public <T> T getAndSet(String key, Object value, Class<T> clazz) {
        Object oldValue = getAndSet(key, value);
        return convertValue(oldValue, clazz);
    }

    @Override
    public <T> T getAndSet(String key, Object value, TypeReference<T> typeReference) {
        Object oldValue = getAndSet(key, value);
        return convertValue(oldValue, typeReference);
    }

    @Override
    public List<Object> multiGet(Collection<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return List.of();
        }

        List<String> validKeys = keys.stream()
                .filter(StrUtil::isNotBlank)
                .toList();

        if (CollUtil.isEmpty(validKeys)) {
            return List.of();
        }

        List<Object> values = redisTemplate.opsForValue().multiGet(validKeys);
        return ObjectUtil.defaultIfNull(values, List.of());
    }

    @Override
    public <T> List<T> multiGet(Collection<String> keys, Class<T> clazz) {
        List<Object> values = multiGet(keys);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .toList();
    }

    @Override
    public <T> List<T> multiGet(Collection<String> keys, TypeReference<T> typeReference) {
        List<Object> values = multiGet(keys);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, typeReference))
                .toList();
    }

    @Override
    public Map<String, Object> multiGetAsMap(Collection<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return Map.of();
        }

        List<String> validKeys = keys.stream()
                .filter(StrUtil::isNotBlank)
                .toList();

        if (CollUtil.isEmpty(validKeys)) {
            return Map.of();
        }

        List<Object> values = redisTemplate.opsForValue().multiGet(validKeys);
        if (CollUtil.isEmpty(values)) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>(validKeys.size());

        // multiGet 返回值顺序与请求 Key 顺序一致，这里按下标组装为 Map
        for (int i = 0; i < validKeys.size(); i++) {
            Object value = i < values.size() ? values.get(i) : null;
            result.put(validKeys.get(i), value);
        }

        return result;
    }

    @Override
    public <T> Map<String, T> multiGetAsMap(Collection<String> keys, Class<T> clazz) {
        Map<String, Object> valueMap = multiGetAsMap(keys);
        if (CollUtil.isEmpty(valueMap)) {
            return Map.of();
        }

        Map<String, T> result = new LinkedHashMap<>(valueMap.size());
        valueMap.forEach((key, value) -> result.put(key, convertValue(value, clazz)));
        return result;
    }

    @Override
    public <T> Map<String, T> multiGetAsMap(Collection<String> keys, TypeReference<T> typeReference) {
        Map<String, Object> valueMap = multiGetAsMap(keys);
        if (CollUtil.isEmpty(valueMap)) {
            return Map.of();
        }

        Map<String, T> result = new LinkedHashMap<>(valueMap.size());
        valueMap.forEach((key, value) -> result.put(key, convertValue(value, typeReference)));
        return result;
    }

    @Override
    public long increment(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long value = redisTemplate.opsForValue().increment(key);
        return ObjectUtil.defaultIfNull(value, 0L);
    }

    @Override
    public long increment(String key, long delta) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long value = redisTemplate.opsForValue().increment(key, delta);
        return ObjectUtil.defaultIfNull(value, 0L);
    }

    @Override
    public double increment(String key, double delta) {
        if (StrUtil.isBlank(key)) {
            return 0D;
        }

        Double value = redisTemplate.opsForValue().increment(key, delta);
        return ObjectUtil.defaultIfNull(value, 0D);
    }

    @Override
    public long decrement(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long value = redisTemplate.opsForValue().decrement(key);
        return ObjectUtil.defaultIfNull(value, 0L);
    }

    @Override
    public long decrement(String key, long delta) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long value = redisTemplate.opsForValue().decrement(key, delta);
        return ObjectUtil.defaultIfNull(value, 0L);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> supplier) {
        return getOrLoad(key, clazz, supplier, null);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> supplier, Duration timeout) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(clazz) || ObjectUtil.isNull(supplier)) {
            return null;
        }

        T cacheValue = get(key, clazz);
        if (ObjectUtil.isNotNull(cacheValue)) {
            return cacheValue;
        }

        T loadedValue = supplier.get();
        if (ObjectUtil.isNull(loadedValue)) {
            return null;
        }

        // 数据源加载成功后再写入缓存，避免空值污染缓存
        set(key, loadedValue, timeout);
        return loadedValue;
    }

    @Override
    public <T> T getOrLoad(String key, TypeReference<T> typeReference, Supplier<T> supplier) {
        return getOrLoad(key, typeReference, supplier, null);
    }

    @Override
    public <T> T getOrLoad(String key, TypeReference<T> typeReference, Supplier<T> supplier, Duration timeout) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(typeReference) || ObjectUtil.isNull(supplier)) {
            return null;
        }

        T cacheValue = get(key, typeReference);
        if (ObjectUtil.isNotNull(cacheValue)) {
            return cacheValue;
        }

        T loadedValue = supplier.get();
        if (ObjectUtil.isNull(loadedValue)) {
            return null;
        }

        // 支持 List<User>、Map<String, User> 等复杂泛型结果的缓存回填
        set(key, loadedValue, timeout);
        return loadedValue;
    }

    @Override
    public <T> T getOrLoadWithLock(String key, String lockKey, Class<T> clazz, Supplier<T> supplier,
                                   Duration cacheTimeout, Duration lockWaitTime, Duration lockLeaseTime) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(lockKey) || ObjectUtil.isNull(clazz) || ObjectUtil.isNull(supplier)) {
            return null;
        }

        T cacheValue = get(key, clazz);
        if (ObjectUtil.isNotNull(cacheValue)) {
            return cacheValue;
        }

        return executeWithLock(lockKey, lockWaitTime, lockLeaseTime, () -> {
            // 获得锁后再次读取缓存，避免并发场景下重复加载数据源
            T lockedCacheValue = get(key, clazz);
            if (ObjectUtil.isNotNull(lockedCacheValue)) {
                return lockedCacheValue;
            }

            T loadedValue = supplier.get();
            if (ObjectUtil.isNotNull(loadedValue)) {
                set(key, loadedValue, cacheTimeout);
            }
            return loadedValue;
        });
    }

    @Override
    public <T> T getOrLoadWithLock(String key, String lockKey, TypeReference<T> typeReference, Supplier<T> supplier,
                                   Duration cacheTimeout, Duration lockWaitTime, Duration lockLeaseTime) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(lockKey) || ObjectUtil.isNull(typeReference) || ObjectUtil.isNull(supplier)) {
            return null;
        }

        T cacheValue = get(key, typeReference);
        if (ObjectUtil.isNotNull(cacheValue)) {
            return cacheValue;
        }

        return executeWithLock(lockKey, lockWaitTime, lockLeaseTime, () -> {
            // 获得锁后进行二次检查，降低缓存击穿时的数据源压力
            T lockedCacheValue = get(key, typeReference);
            if (ObjectUtil.isNotNull(lockedCacheValue)) {
                return lockedCacheValue;
            }

            T loadedValue = supplier.get();
            if (ObjectUtil.isNotNull(loadedValue)) {
                set(key, loadedValue, cacheTimeout);
            }
            return loadedValue;
        });
    }

    @Override
    public void hPut(String key, Object hashKey, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(hashKey) || ObjectUtil.isNull(value)) {
            return;
        }
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    public void hPutAll(String key, Map<?, ?> map) {
        if (StrUtil.isBlank(key) || CollUtil.isEmpty(map)) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, map);
    }

    @Override
    public boolean hPutIfAbsent(String key, Object hashKey, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(hashKey) || ObjectUtil.isNull(value)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForHash().putIfAbsent(key, hashKey, value));
    }

    @Override
    public Object hGet(String key, Object hashKey) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(hashKey)) {
            return null;
        }
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    @Override
    public <T> T hGet(String key, Object hashKey, Class<T> clazz) {
        Object value = hGet(key, hashKey);
        return convertValue(value, clazz);
    }

    @Override
    public <T> T hGet(String key, Object hashKey, TypeReference<T> typeReference) {
        Object value = hGet(key, hashKey);
        return convertValue(value, typeReference);
    }

    @Override
    public List<Object> hMultiGet(String key, Collection<?> hashKeys) {
        if (StrUtil.isBlank(key) || CollUtil.isEmpty(hashKeys)) {
            return List.of();
        }

        List<Object> validHashKeys = hashKeys.stream()
                .filter(ObjectUtil::isNotNull)
                .map(Object.class::cast)
                .toList();

        if (CollUtil.isEmpty(validHashKeys)) {
            return List.of();
        }

        List<Object> values = redisTemplate.opsForHash().multiGet(key, validHashKeys);
        return ObjectUtil.defaultIfNull(values, List.of());
    }

    @Override
    public <T> List<T> hMultiGet(String key, Collection<?> hashKeys, Class<T> clazz) {
        List<Object> values = hMultiGet(key, hashKeys);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .toList();
    }

    @Override
    public <T> List<T> hMultiGet(String key, Collection<?> hashKeys, TypeReference<T> typeReference) {
        List<Object> values = hMultiGet(key, hashKeys);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, typeReference))
                .toList();
    }

    @Override
    public Map<Object, Object> hGetAll(String key) {
        if (StrUtil.isBlank(key)) {
            return Map.of();
        }

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return ObjectUtil.defaultIfNull(entries, Map.of());
    }

    @Override
    public <T> Map<Object, T> hGetAll(String key, Class<T> clazz) {
        Map<Object, Object> entries = hGetAll(key);
        if (CollUtil.isEmpty(entries)) {
            return Map.of();
        }

        Map<Object, T> result = new LinkedHashMap<>(entries.size());

        // Hash Value 逐个转换，保留原始 HashKey 类型
        entries.forEach((hashKey, value) -> result.put(hashKey, convertValue(value, clazz)));

        return result;
    }

    @Override
    public <T> Map<Object, T> hGetAll(String key, TypeReference<T> typeReference) {
        Map<Object, Object> entries = hGetAll(key);
        if (CollUtil.isEmpty(entries)) {
            return Map.of();
        }

        Map<Object, T> result = new LinkedHashMap<>(entries.size());

        // 支持 Hash Value 为复杂泛型结构，例如 List<User>、Map<String, User>
        entries.forEach((hashKey, value) -> result.put(hashKey, convertValue(value, typeReference)));

        return result;
    }

    @Override
    public boolean hHasKey(String key, Object hashKey) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(hashKey)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, hashKey));
    }

    @Override
    public long hDelete(String key, Object... hashKeys) {
        if (StrUtil.isBlank(key) || ObjectUtil.isEmpty(hashKeys)) {
            return 0L;
        }

        Long count = redisTemplate.opsForHash().delete(key, hashKeys);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public long hSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long size = redisTemplate.opsForHash().size(key);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    @Override
    public Set<Object> hKeys(String key) {
        if (StrUtil.isBlank(key)) {
            return Set.of();
        }

        Set<Object> keys = redisTemplate.opsForHash().keys(key);
        return ObjectUtil.defaultIfNull(keys, Set.of());
    }

    @Override
    public List<Object> hValues(String key) {
        if (StrUtil.isBlank(key)) {
            return List.of();
        }

        List<Object> values = redisTemplate.opsForHash().values(key);
        return ObjectUtil.defaultIfNull(values, List.of());
    }

    @Override
    public <T> List<T> hValues(String key, Class<T> clazz) {
        List<Object> values = hValues(key);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .toList();
    }

    @Override
    public <T> List<T> hValues(String key, TypeReference<T> typeReference) {
        List<Object> values = hValues(key);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, typeReference))
                .toList();
    }

    @Override
    public long hIncrement(String key, Object hashKey, long delta) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(hashKey)) {
            return 0L;
        }
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    @Override
    public double hIncrement(String key, Object hashKey, double delta) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(hashKey)) {
            return 0D;
        }
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    @Override
    public long lLeftPush(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return 0L;
        }

        Long size = redisTemplate.opsForList().leftPush(key, value);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    @Override
    public long lLeftPushAll(String key, Collection<?> values) {
        if (StrUtil.isBlank(key) || CollUtil.isEmpty(values)) {
            return 0L;
        }

        List<Object> validValues = values.stream()
                .filter(ObjectUtil::isNotNull)
                .map(Object.class::cast)
                .toList();

        if (CollUtil.isEmpty(validValues)) {
            return 0L;
        }

        Long size = redisTemplate.opsForList().leftPushAll(key, validValues);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    @Override
    public long lRightPush(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return 0L;
        }

        Long size = redisTemplate.opsForList().rightPush(key, value);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    @Override
    public long lRightPushAll(String key, Collection<?> values) {
        if (StrUtil.isBlank(key) || CollUtil.isEmpty(values)) {
            return 0L;
        }

        List<Object> validValues = values.stream()
                .filter(ObjectUtil::isNotNull)
                .map(Object.class::cast)
                .toList();

        if (CollUtil.isEmpty(validValues)) {
            return 0L;
        }

        Long size = redisTemplate.opsForList().rightPushAll(key, validValues);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    @Override
    public Object lLeftPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public Object lLeftPop(String key, Duration timeout) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        if (ObjectUtil.isNull(timeout) || timeout.isNegative() || timeout.isZero()) {
            return lLeftPop(key);
        }

        return redisTemplate.opsForList().leftPop(key, timeout);
    }

    @Override
    public <T> T lLeftPop(String key, Class<T> clazz) {
        Object value = lLeftPop(key);
        return convertValue(value, clazz);
    }

    @Override
    public <T> T lLeftPop(String key, TypeReference<T> typeReference) {
        Object value = lLeftPop(key);
        return convertValue(value, typeReference);
    }

    @Override
    public Object lRightPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return redisTemplate.opsForList().rightPop(key);
    }

    @Override
    public Object lRightPop(String key, Duration timeout) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        if (ObjectUtil.isNull(timeout) || timeout.isNegative() || timeout.isZero()) {
            return lRightPop(key);
        }

        return redisTemplate.opsForList().rightPop(key, timeout);
    }

    @Override
    public <T> T lRightPop(String key, Class<T> clazz) {
        Object value = lRightPop(key);
        return convertValue(value, clazz);
    }

    @Override
    public <T> T lRightPop(String key, TypeReference<T> typeReference) {
        Object value = lRightPop(key);
        return convertValue(value, typeReference);
    }

    @Override
    public Object lIndex(String key, long index) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return redisTemplate.opsForList().index(key, index);
    }

    @Override
    public <T> T lIndex(String key, long index, Class<T> clazz) {
        Object value = lIndex(key, index);
        return convertValue(value, clazz);
    }

    @Override
    public <T> T lIndex(String key, long index, TypeReference<T> typeReference) {
        Object value = lIndex(key, index);
        return convertValue(value, typeReference);
    }

    @Override
    public List<Object> lRange(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return List.of();
        }

        List<Object> values = redisTemplate.opsForList().range(key, start, end);
        return ObjectUtil.defaultIfNull(values, List.of());
    }

    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        List<Object> values = lRange(key, start, end);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .toList();
    }

    @Override
    public <T> List<T> lRange(String key, long start, long end, TypeReference<T> typeReference) {
        List<Object> values = lRange(key, start, end);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, typeReference))
                .toList();
    }

    @Override
    public void lSet(String key, long index, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return;
        }
        redisTemplate.opsForList().set(key, index, value);
    }

    @Override
    public void lTrim(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        // Redis LTRIM 会只保留指定区间内的元素，常用于限制队列长度
        redisTemplate.opsForList().trim(key, start, end);
    }

    @Override
    public long lRemove(String key, long count, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return 0L;
        }

        Long removeCount = redisTemplate.opsForList().remove(key, count, value);
        return ObjectUtil.defaultIfNull(removeCount, 0L);
    }

    @Override
    public long lSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long size = redisTemplate.opsForList().size(key);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    @Override
    public long sAdd(String key, Object... values) {
        if (StrUtil.isBlank(key) || ObjectUtil.isEmpty(values)) {
            return 0L;
        }

        Object[] validValues = List.of(values).stream()
                .filter(ObjectUtil::isNotNull)
                .toArray();

        if (ObjectUtil.isEmpty(validValues)) {
            return 0L;
        }

        Long count = redisTemplate.opsForSet().add(key, validValues);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public long sRemove(String key, Object... values) {
        if (StrUtil.isBlank(key) || ObjectUtil.isEmpty(values)) {
            return 0L;
        }

        Object[] validValues = List.of(values).stream()
                .filter(ObjectUtil::isNotNull)
                .toArray();

        if (ObjectUtil.isEmpty(validValues)) {
            return 0L;
        }

        Long count = redisTemplate.opsForSet().remove(key, validValues);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public boolean sIsMember(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    @Override
    public Set<Object> sMembers(String key) {
        if (StrUtil.isBlank(key)) {
            return Set.of();
        }

        Set<Object> members = redisTemplate.opsForSet().members(key);
        return ObjectUtil.defaultIfNull(members, Set.of());
    }

    @Override
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        Set<Object> members = sMembers(key);
        if (CollUtil.isEmpty(members)) {
            return Set.of();
        }

        return members.stream()
                .map(member -> convertValue(member, clazz))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public <T> Set<T> sMembers(String key, TypeReference<T> typeReference) {
        Set<Object> members = sMembers(key);
        if (CollUtil.isEmpty(members)) {
            return Set.of();
        }

        return members.stream()
                .map(member -> convertValue(member, typeReference))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Object sPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return redisTemplate.opsForSet().pop(key);
    }

    @Override
    public <T> T sPop(String key, Class<T> clazz) {
        Object value = sPop(key);
        return convertValue(value, clazz);
    }

    @Override
    public <T> T sPop(String key, TypeReference<T> typeReference) {
        Object value = sPop(key);
        return convertValue(value, typeReference);
    }

    @Override
    public List<Object> sPop(String key, long count) {
        if (StrUtil.isBlank(key) || count <= 0) {
            return List.of();
        }

        List<Object> values = redisTemplate.opsForSet().pop(key, count);
        return ObjectUtil.defaultIfNull(values, List.of());
    }

    @Override
    public <T> List<T> sPop(String key, long count, Class<T> clazz) {
        List<Object> values = sPop(key, count);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .toList();
    }

    @Override
    public <T> List<T> sPop(String key, long count, TypeReference<T> typeReference) {
        List<Object> values = sPop(key, count);
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .map(value -> convertValue(value, typeReference))
                .toList();
    }

    @Override
    public long sSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long size = redisTemplate.opsForSet().size(key);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    @Override
    public Set<Object> sIntersect(String key, String otherKey) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(otherKey)) {
            return Set.of();
        }

        Set<Object> values = redisTemplate.opsForSet().intersect(key, otherKey);
        return ObjectUtil.defaultIfNull(values, Set.of());
    }

    @Override
    public <T> Set<T> sIntersect(String key, String otherKey, Class<T> clazz) {
        Set<Object> values = sIntersect(key, otherKey);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public <T> Set<T> sIntersect(String key, String otherKey, TypeReference<T> typeReference) {
        Set<Object> values = sIntersect(key, otherKey);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values.stream()
                .map(value -> convertValue(value, typeReference))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Set<Object> sUnion(String key, String otherKey) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(otherKey)) {
            return Set.of();
        }

        Set<Object> values = redisTemplate.opsForSet().union(key, otherKey);
        return ObjectUtil.defaultIfNull(values, Set.of());
    }

    @Override
    public <T> Set<T> sUnion(String key, String otherKey, Class<T> clazz) {
        Set<Object> values = sUnion(key, otherKey);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public <T> Set<T> sUnion(String key, String otherKey, TypeReference<T> typeReference) {
        Set<Object> values = sUnion(key, otherKey);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values.stream()
                .map(value -> convertValue(value, typeReference))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Set<Object> sDifference(String key, String otherKey) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(otherKey)) {
            return Set.of();
        }

        Set<Object> values = redisTemplate.opsForSet().difference(key, otherKey);
        return ObjectUtil.defaultIfNull(values, Set.of());
    }

    @Override
    public <T> Set<T> sDifference(String key, String otherKey, Class<T> clazz) {
        Set<Object> values = sDifference(key, otherKey);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values.stream()
                .map(value -> convertValue(value, clazz))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public <T> Set<T> sDifference(String key, String otherKey, TypeReference<T> typeReference) {
        Set<Object> values = sDifference(key, otherKey);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values.stream()
                .map(value -> convertValue(value, typeReference))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public boolean zAdd(String key, Object value, double score) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    @Override
    public long zRemove(String key, Object... values) {
        if (StrUtil.isBlank(key) || ObjectUtil.isEmpty(values)) {
            return 0L;
        }

        Object[] validValues = List.of(values).stream()
                .filter(ObjectUtil::isNotNull)
                .toArray();

        if (ObjectUtil.isEmpty(validValues)) {
            return 0L;
        }

        Long count = redisTemplate.opsForZSet().remove(key, validValues);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public Double zScore(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }
        return redisTemplate.opsForZSet().score(key, value);
    }

    @Override
    public Long zRank(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }
        return redisTemplate.opsForZSet().rank(key, value);
    }

    @Override
    public Long zReverseRank(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }
        return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    @Override
    public long zSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long size = redisTemplate.opsForZSet().size(key);
        return ObjectUtil.defaultIfNull(size, 0L);
    }

    @Override
    public long zCount(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long count = redisTemplate.opsForZSet().count(key, min, max);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public Set<Object> zRange(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return Set.of();
        }

        Set<Object> values = redisTemplate.opsForZSet().range(key, start, end);
        return ObjectUtil.defaultIfNull(values, Set.of());
    }

    @Override
    public <T> Set<T> zRange(String key, long start, long end, Class<T> clazz) {
        Set<Object> values = zRange(key, start, end);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        Set<T> result = new LinkedHashSet<>(values.size());

        // ZSet 查询结果有顺序，使用 LinkedHashSet 保留 Redis 返回顺序
        values.forEach(value -> result.add(convertValue(value, clazz)));

        return result;
    }

    @Override
    public <T> Set<T> zRange(String key, long start, long end, TypeReference<T> typeReference) {
        Set<Object> values = zRange(key, start, end);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        Set<T> result = new LinkedHashSet<>(values.size());

        // 支持 ZSet 成员为复杂对象或泛型结构的场景
        values.forEach(value -> result.add(convertValue(value, typeReference)));

        return result;
    }

    @Override
    public Set<Object> zReverseRange(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return Set.of();
        }

        Set<Object> values = redisTemplate.opsForZSet().reverseRange(key, start, end);
        return ObjectUtil.defaultIfNull(values, Set.of());
    }

    @Override
    public <T> Set<T> zReverseRange(String key, long start, long end, Class<T> clazz) {
        Set<Object> values = zReverseRange(key, start, end);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        Set<T> result = new LinkedHashSet<>(values.size());

        // 反向排名结果同样需要保留 Redis 返回顺序
        values.forEach(value -> result.add(convertValue(value, clazz)));

        return result;
    }

    @Override
    public <T> Set<T> zReverseRange(String key, long start, long end, TypeReference<T> typeReference) {
        Set<Object> values = zReverseRange(key, start, end);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        Set<T> result = new LinkedHashSet<>(values.size());

        // 反向范围查询结果逐个转换为指定泛型类型
        values.forEach(value -> result.add(convertValue(value, typeReference)));

        return result;
    }

    @Override
    public Set<Object> zRangeByScore(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return Set.of();
        }

        Set<Object> values = redisTemplate.opsForZSet().rangeByScore(key, min, max);
        return ObjectUtil.defaultIfNull(values, Set.of());
    }

    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, Class<T> clazz) {
        Set<Object> values = zRangeByScore(key, min, max);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        Set<T> result = new LinkedHashSet<>(values.size());

        // 按分数区间查询时，Redis 返回结果默认按 score 升序排列
        values.forEach(value -> result.add(convertValue(value, clazz)));

        return result;
    }

    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, TypeReference<T> typeReference) {
        Set<Object> values = zRangeByScore(key, min, max);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        Set<T> result = new LinkedHashSet<>(values.size());

        // 分数区间查询结果转换为指定泛型结构
        values.forEach(value -> result.add(convertValue(value, typeReference)));

        return result;
    }

    @Override
    public Set<Object> zRangeByScore(String key, double min, double max, long offset, long count) {
        if (StrUtil.isBlank(key) || offset < 0 || count <= 0) {
            return Set.of();
        }

        Set<Object> values = redisTemplate.opsForZSet().rangeByScore(key, min, max, offset, count);
        return ObjectUtil.defaultIfNull(values, Set.of());
    }

    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, long offset, long count, Class<T> clazz) {
        Set<Object> values = zRangeByScore(key, min, max, offset, count);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        Set<T> result = new LinkedHashSet<>(values.size());

        // 分页区间查询结果需要保留原始顺序，便于排行榜、时间线等场景使用
        values.forEach(value -> result.add(convertValue(value, clazz)));

        return result;
    }

    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, long offset, long count, TypeReference<T> typeReference) {
        Set<Object> values = zRangeByScore(key, min, max, offset, count);
        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        Set<T> result = new LinkedHashSet<>(values.size());

        // 分页区间查询结果逐个转换为指定泛型类型
        values.forEach(value -> result.add(convertValue(value, typeReference)));

        return result;
    }

    @Override
    public Set<ZSetOperations.TypedTuple<Object>> zRangeWithScores(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return Set.of();
        }

        Set<ZSetOperations.TypedTuple<Object>> values = redisTemplate.opsForZSet().rangeWithScores(key, start, end);
        return ObjectUtil.defaultIfNull(values, Set.of());
    }

    @Override
    public <T> Set<ZSetOperations.TypedTuple<T>> zRangeWithScores(String key, long start, long end, Class<T> clazz) {
        Set<ZSetOperations.TypedTuple<Object>> tuples = zRangeWithScores(key, start, end);
        if (CollUtil.isEmpty(tuples)) {
            return Set.of();
        }

        Set<ZSetOperations.TypedTuple<T>> result = new LinkedHashSet<>(tuples.size());

        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            if (ObjectUtil.isNull(tuple)) {
                continue;
            }

            T value = convertValue(tuple.getValue(), clazz);
            Double score = tuple.getScore();

            // 重新构造 TypedTuple，保留原始 score，并只转换 value 类型
            result.add(new DefaultTypedTuple<>(value, score));
        }

        return result;
    }

    @Override
    public <T> Set<ZSetOperations.TypedTuple<T>> zRangeWithScores(String key, long start, long end, TypeReference<T> typeReference) {
        Set<ZSetOperations.TypedTuple<Object>> tuples = zRangeWithScores(key, start, end);
        if (CollUtil.isEmpty(tuples)) {
            return Set.of();
        }

        Set<ZSetOperations.TypedTuple<T>> result = new LinkedHashSet<>(tuples.size());

        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            if (ObjectUtil.isNull(tuple)) {
                continue;
            }

            T value = convertValue(tuple.getValue(), typeReference);
            Double score = tuple.getScore();

            // 泛型转换只处理成员值，score 原样保留
            result.add(new DefaultTypedTuple<>(value, score));
        }

        return result;
    }

    @Override
    public long zRemoveRange(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long count = redisTemplate.opsForZSet().removeRange(key, start, end);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public long zRemoveRangeByScore(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        Long count = redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
        return ObjectUtil.defaultIfNull(count, 0L);
    }

    @Override
    public Double zIncrementScore(String key, Object value, double delta) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    @Override
    public boolean tryLock(String lockKey, String lockValue, Duration leaseTime) {
        if (StrUtil.isBlank(lockKey) || StrUtil.isBlank(lockValue)
                || ObjectUtil.isNull(leaseTime) || leaseTime.isNegative() || leaseTime.isZero()) {
            return false;
        }

        // 使用 Redis SET NX + 过期时间实现互斥锁，避免进程异常退出后锁永久不释放
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, leaseTime));
    }

    @Override
    public boolean tryLock(String lockKey, String lockValue, Duration waitTime, Duration leaseTime) {
        if (StrUtil.isBlank(lockKey) || StrUtil.isBlank(lockValue)
                || ObjectUtil.isNull(leaseTime) || leaseTime.isNegative() || leaseTime.isZero()) {
            return false;
        }

        if (ObjectUtil.isNull(waitTime) || waitTime.isNegative() || waitTime.isZero()) {
            return tryLock(lockKey, lockValue, leaseTime);
        }

        long deadline = System.nanoTime() + waitTime.toNanos();

        while (System.nanoTime() <= deadline) {
            if (tryLock(lockKey, lockValue, leaseTime)) {
                return true;
            }

            // 未获取到锁时短暂休眠，避免高频自旋压垮 Redis
            sleepBeforeRetry(deadline);
        }

        return false;
    }

    @Override
    public boolean unlock(String lockKey, String lockValue) {
        if (StrUtil.isBlank(lockKey) || StrUtil.isBlank(lockValue)) {
            return false;
        }

        boolean unlocked = compareAndDeleteByLua(lockKey, lockValue);
        if (!unlocked) {
            log.warn("Redis 分布式锁未释放或已过期，lockKey={}", lockKey);
        }

        return unlocked;
    }

    @Override
    public boolean tryExecuteWithLock(String lockKey, Duration leaseTime, Runnable task) {
        return tryExecuteWithLock(lockKey, null, leaseTime, task);
    }

    @Override
    public boolean tryExecuteWithLock(String lockKey, Duration waitTime, Duration leaseTime, Runnable task) {
        if (StrUtil.isBlank(lockKey) || ObjectUtil.isNull(task)) {
            return false;
        }

        String lockValue = generateLockValue();
        boolean locked = tryLock(lockKey, lockValue, waitTime, leaseTime);
        if (!locked) {
            log.warn("Redis 分布式锁获取失败，lockKey={}", lockKey);
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            boolean unlocked = unlock(lockKey, lockValue);
            if (!unlocked) {
                log.warn("Redis 分布式锁未释放或已过期，lockKey={}", lockKey);
            }
        }
    }

    @Override
    public <T> Optional<T> tryExecuteWithLock(String lockKey, Duration leaseTime, Supplier<T> supplier) {
        return tryExecuteWithLock(lockKey, null, leaseTime, supplier);
    }

    @Override
    public <T> Optional<T> tryExecuteWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> supplier) {
        if (StrUtil.isBlank(lockKey) || ObjectUtil.isNull(supplier)) {
            return Optional.empty();
        }

        String lockValue = generateLockValue();
        boolean locked = tryLock(lockKey, lockValue, waitTime, leaseTime);
        if (!locked) {
            log.warn("Redis 分布式锁获取失败，lockKey={}", lockKey);
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(supplier.get());
        } finally {
            boolean unlocked = unlock(lockKey, lockValue);
            if (!unlocked) {
                log.warn("Redis 分布式锁未释放或已过期，lockKey={}", lockKey);
            }
        }
    }

    @Override
    public void executeWithLock(String lockKey, Duration leaseTime, Runnable task) {
        executeWithLock(lockKey, null, leaseTime, task);
    }

    @Override
    public void executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Runnable task) {
        boolean executed = tryExecuteWithLock(lockKey, waitTime, leaseTime, task);
        if (!executed) {
            throw new IllegalStateException("Redis 分布式锁获取失败，lockKey=" + lockKey);
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration leaseTime, Supplier<T> supplier) {
        return executeWithLock(lockKey, null, leaseTime, supplier);
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> supplier) {
        Optional<T> result = tryExecuteWithLock(lockKey, waitTime, leaseTime, supplier);
        if (result.isEmpty()) {
            throw new IllegalStateException("Redis 分布式锁获取失败，lockKey=" + lockKey);
        }
        return result.get();
    }

    private String generateLockValue() {
        return IdUtil.fastSimpleUUID();
    }

    private void sleepBeforeRetry(long deadline) {
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) {
            return;
        }

        long sleepMillis = Math.min(DEFAULT_LOCK_RETRY_INTERVAL_MILLIS, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
        if (sleepMillis <= 0) {
            sleepMillis = 1L;
        }

        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Redis 分布式锁等待被中断", e);
        }
    }

    @Override
    public Object executeLua(String script, List<String> keys, Object... args) {
        return executeLua(script, Object.class, keys, args);
    }

    @Override
    public <T> T executeLua(String script, Class<T> clazz, List<String> keys, Object... args) {
        if (StrUtil.isBlank(script) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        List<String> validKeys = buildLuaKeys(keys);

        try {
            RedisScript<T> redisScript = RedisScript.of(script, clazz);

            // RedisScript 的 resultType 会影响 Redis 返回值解析方式，例如 Long、Boolean、List、String
            T result = redisTemplate.execute(redisScript, validKeys, args);

            return convertValue(result, clazz);
        } catch (Exception e) {
            log.warn("Redis Lua 脚本执行失败，resultType={}，keyCount={}，argCount={}",
                    clazz.getName(), validKeys.size(), ObjectUtil.isNull(args) ? 0 : args.length, e);
            return null;
        }
    }

    @Override
    public <T> T executeLua(String script, TypeReference<T> typeReference, List<String> keys, Object... args) {
        if (StrUtil.isBlank(script) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        Class<?> resultClass = resolveLuaResultClass(typeReference);
        Object result = executeLua(script, resultClass, keys, args);

        return convertValue(result, typeReference);
    }

    @Override
    public Object executeLuaFromResource(String resourceLocation, List<String> keys, Object... args) {
        return executeLuaFromResource(resourceLocation, Object.class, keys, args);
    }

    @Override
    public <T> T executeLuaFromResource(String resourceLocation, Class<T> clazz, List<String> keys, Object... args) {
        if (StrUtil.isBlank(resourceLocation) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        String script = readLuaScriptFromResource(resourceLocation);
        if (StrUtil.isBlank(script)) {
            return null;
        }

        return executeLua(script, clazz, keys, args);
    }

    @Override
    public <T> T executeLuaFromResource(String resourceLocation, TypeReference<T> typeReference, List<String> keys, Object... args) {
        if (StrUtil.isBlank(resourceLocation) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        String script = readLuaScriptFromResource(resourceLocation);
        if (StrUtil.isBlank(script)) {
            return null;
        }

        return executeLua(script, typeReference, keys, args);
    }

    @Override
    public boolean executeLuaAsBoolean(String script, List<String> keys, Object... args) {
        Boolean result = executeLua(script, Boolean.class, keys, args);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public long executeLuaAsLong(String script, List<String> keys, Object... args) {
        Long result = executeLua(script, Long.class, keys, args);
        return ObjectUtil.defaultIfNull(result, 0L);
    }

    @Override
    public String executeLuaAsString(String script, List<String> keys, Object... args) {
        return executeLua(script, String.class, keys, args);
    }

    @Override
    public List<Object> executeLuaAsList(String script, List<String> keys, Object... args) {
        List<?> result = executeLua(script, List.class, keys, args);
        if (CollUtil.isEmpty(result)) {
            return List.of();
        }

        return result.stream()
                .map(Object.class::cast)
                .toList();
    }

    @Override
    public boolean compareAndDeleteByLua(String key, Object expectedValue) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(expectedValue)) {
            return false;
        }

        Long result = executeLua(
                COMPARE_AND_DELETE_LUA_SCRIPT,
                Long.class,
                List.of(key),
                expectedValue
        );

        return ObjectUtil.defaultIfNull(result, 0L) > 0;
    }

    @Override
    public boolean compareAndSetByLua(String key, Object expectedValue, Object newValue) {
        return compareAndSetByLua(key, expectedValue, newValue, null);
    }

    @Override
    public boolean compareAndSetByLua(String key, Object expectedValue, Object newValue, Duration timeout) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(expectedValue) || ObjectUtil.isNull(newValue)) {
            return false;
        }

        long ttlSeconds = ObjectUtil.isNotNull(timeout) && !timeout.isNegative() && !timeout.isZero()
                ? timeout.toSeconds()
                : 0L;

        Long result = executeLua(
                COMPARE_AND_SET_LUA_SCRIPT,
                Long.class,
                List.of(key),
                expectedValue,
                newValue,
                ttlSeconds
        );

        return ObjectUtil.defaultIfNull(result, 0L) > 0;
    }

    @Override
    public long incrementAndExpireByLua(String key, long delta, Duration timeout) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        long ttlSeconds = ObjectUtil.isNotNull(timeout) && !timeout.isNegative() && !timeout.isZero()
                ? timeout.toSeconds()
                : 0L;

        Long result = executeLua(
                INCREMENT_AND_EXPIRE_LUA_SCRIPT,
                Long.class,
                List.of(key),
                delta,
                ttlSeconds
        );

        return ObjectUtil.defaultIfNull(result, 0L);
    }

    private List<String> buildLuaKeys(List<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return List.of();
        }

        return keys.stream()
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private String readLuaScriptFromResource(String resourceLocation) {
        if (StrUtil.isBlank(resourceLocation)) {
            return null;
        }

        try {
            String location = StrUtil.removePrefix(resourceLocation, "classpath:");

            // 支持读取 resources 目录下的 Lua 文件，例如 lua/stock_decrease.lua
            return ResourceUtil.readUtf8Str(location);
        } catch (Exception e) {
            log.warn("读取 Redis Lua 脚本资源失败，resourceLocation={}", resourceLocation, e);
            return null;
        }
    }

    private Class<?> resolveLuaResultClass(TypeReference<?> typeReference) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(typeReference);
        Class<?> rawClass = javaType.getRawClass();

        if (Boolean.class.equals(rawClass) || boolean.class.equals(rawClass)) {
            return Boolean.class;
        }

        if (Long.class.equals(rawClass)
                || long.class.equals(rawClass)
                || Integer.class.equals(rawClass)
                || int.class.equals(rawClass)
                || Number.class.equals(rawClass)) {
            return Long.class;
        }

        if (String.class.equals(rawClass)) {
            return String.class;
        }

        if (List.class.isAssignableFrom(rawClass)
                || Collection.class.isAssignableFrom(rawClass)
                || Set.class.isAssignableFrom(rawClass)) {
            return List.class;
        }

        return Object.class;
    }
}