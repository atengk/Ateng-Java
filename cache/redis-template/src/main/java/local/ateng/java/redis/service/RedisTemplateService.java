package local.ateng.java.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * RedisTemplate 通用操作服务接口。
 *
 * @author Ateng
 * @since 2026-04-28
 */
public interface RedisTemplateService {

    // ==============================
    // 类型转换
    // ==============================

    /**
     * 将 Redis 中读取到的原始值转换为指定类型。
     *
     * @param value 原始值，通常来自 Redis 反序列化结果
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的对象；参数为空或转换失败时返回 null
     */
    <T> T convertValue(Object value, Class<T> clazz);

    /**
     * 将 Redis 中读取到的原始值转换为指定泛型类型。
     *
     * @param value         原始值，通常来自 Redis 反序列化结果
     * @param typeReference 目标泛型类型引用，例如 List<User>、Map<String, User>
     * @param <T>           目标泛型类型
     * @return 转换后的对象；参数为空或转换失败时返回 null
     */
    <T> T convertValue(Object value, TypeReference<T> typeReference);


    // ==============================
    // Key 通用操作
    // ==============================

    /**
     * 判断指定 Key 是否存在。
     *
     * @param key Redis Key
     * @return true 表示存在，false 表示不存在或参数无效
     */
    boolean hasKey(String key);

    /**
     * 删除指定 Key。
     *
     * @param key Redis Key
     * @return true 表示删除成功，false 表示 Key 不存在或参数无效
     */
    boolean delete(String key);

    /**
     * 批量删除指定 Key。
     *
     * @param keys Redis Key 集合
     * @return 成功删除的 Key 数量
     */
    long delete(Collection<String> keys);

    /**
     * 设置指定 Key 的过期时间。
     *
     * @param key     Redis Key
     * @param timeout 过期时间
     * @return true 表示设置成功，false 表示设置失败或参数无效
     */
    boolean expire(String key, Duration timeout);

    /**
     * 设置指定 Key 在指定时间点过期。
     *
     * @param key      Redis Key
     * @param expireAt 过期时间点
     * @return true 表示设置成功，false 表示设置失败或参数无效
     */
    boolean expireAt(String key, Instant expireAt);

    /**
     * 移除指定 Key 的过期时间，使其永久有效。
     *
     * @param key Redis Key
     * @return true 表示移除成功，false 表示 Key 不存在、无过期时间或参数无效
     */
    boolean persist(String key);

    /**
     * 获取指定 Key 的剩余过期时间。
     *
     * @param key Redis Key
     * @return 剩余过期时间；-1 表示永久有效，-2 表示 Key 不存在
     */
    Duration getExpire(String key);

    /**
     * 按匹配规则扫描 Redis Key。
     *
     * @param pattern Key 匹配表达式，例如 user:*、order:2026:*
     * @return 匹配到的 Key 集合
     */
    Set<String> scanKeys(String pattern);

    /**
     * 按匹配规则和扫描数量扫描 Redis Key。
     *
     * @param pattern Key 匹配表达式，例如 user:*、order:2026:*
     * @param count   每批扫描数量，数值越大单次扫描量越多
     * @return 匹配到的 Key 集合
     */
    Set<String> scanKeys(String pattern, long count);


    // ==============================
    // Value 操作
    // ==============================

    /**
     * 设置指定 Key 的缓存值。
     *
     * @param key   Redis Key
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 设置指定 Key 的缓存值，并指定过期时间。
     *
     * @param key     Redis Key
     * @param value   缓存值
     * @param timeout 过期时间
     */
    void set(String key, Object value, Duration timeout);

    /**
     * 当指定 Key 不存在时设置缓存值。
     *
     * @param key   Redis Key
     * @param value 缓存值
     * @return true 表示设置成功，false 表示 Key 已存在或参数无效
     */
    boolean setIfAbsent(String key, Object value);

    /**
     * 当指定 Key 不存在时设置缓存值，并指定过期时间。
     *
     * @param key     Redis Key
     * @param value   缓存值
     * @param timeout 过期时间
     * @return true 表示设置成功，false 表示 Key 已存在或参数无效
     */
    boolean setIfAbsent(String key, Object value, Duration timeout);

    /**
     * 当指定 Key 已存在时设置缓存值。
     *
     * @param key   Redis Key
     * @param value 缓存值
     * @return true 表示设置成功，false 表示 Key 不存在或参数无效
     */
    boolean setIfPresent(String key, Object value);

    /**
     * 当指定 Key 已存在时设置缓存值，并指定过期时间。
     *
     * @param key     Redis Key
     * @param value   缓存值
     * @param timeout 过期时间
     * @return true 表示设置成功，false 表示 Key 不存在或参数无效
     */
    boolean setIfPresent(String key, Object value, Duration timeout);

    /**
     * 获取指定 Key 的缓存值。
     *
     * @param key Redis Key
     * @return 缓存值；Key 不存在或参数无效时返回 null
     */
    Object get(String key);

    /**
     * 获取指定 Key 的缓存值，并转换为指定类型。
     *
     * @param key   Redis Key
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的缓存值；Key 不存在、参数无效或转换失败时返回 null
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 获取指定 Key 的缓存值，并转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param typeReference 目标泛型类型引用，例如 List<User>、Map<String, User>
     * @param <T>           目标泛型类型
     * @return 转换后的缓存值；Key 不存在、参数无效或转换失败时返回 null
     */
    <T> T get(String key, TypeReference<T> typeReference);

    /**
     * 获取指定 Key 的旧值，并设置新值。
     *
     * @param key   Redis Key
     * @param value 新缓存值
     * @return 旧缓存值；Key 不存在或参数无效时返回 null
     */
    Object getAndSet(String key, Object value);

    /**
     * 获取指定 Key 的旧值并转换为指定类型，同时设置新值。
     *
     * @param key   Redis Key
     * @param value 新缓存值
     * @param clazz 旧值目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的旧缓存值；Key 不存在、参数无效或转换失败时返回 null
     */
    <T> T getAndSet(String key, Object value, Class<T> clazz);

    /**
     * 获取指定 Key 的旧值并转换为指定泛型类型，同时设置新值。
     *
     * @param key           Redis Key
     * @param value         新缓存值
     * @param typeReference 旧值目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的旧缓存值；Key 不存在、参数无效或转换失败时返回 null
     */
    <T> T getAndSet(String key, Object value, TypeReference<T> typeReference);

    /**
     * 批量获取多个 Key 的缓存值。
     *
     * @param keys Redis Key 集合
     * @return 缓存值列表，返回顺序与 Key 集合顺序一致
     */
    List<Object> multiGet(Collection<String> keys);

    /**
     * 批量获取多个 Key 的缓存值，并逐个转换为指定类型。
     *
     * @param keys  Redis Key 集合
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的缓存值列表，返回顺序与 Key 集合顺序一致
     */
    <T> List<T> multiGet(Collection<String> keys, Class<T> clazz);

    /**
     * 批量获取多个 Key 的缓存值，并逐个转换为指定泛型类型。
     *
     * @param keys          Redis Key 集合
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的缓存值列表，返回顺序与 Key 集合顺序一致
     */
    <T> List<T> multiGet(Collection<String> keys, TypeReference<T> typeReference);

    /**
     * 批量获取多个 Key 的缓存值，并按 Key 组装为 Map。
     *
     * @param keys Redis Key 集合
     * @return Key 与缓存值的映射关系
     */
    Map<String, Object> multiGetAsMap(Collection<String> keys);

    /**
     * 批量获取多个 Key 的缓存值，并转换为指定类型后按 Key 组装为 Map。
     *
     * @param keys  Redis Key 集合
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return Key 与转换后缓存值的映射关系
     */
    <T> Map<String, T> multiGetAsMap(Collection<String> keys, Class<T> clazz);

    /**
     * 批量获取多个 Key 的缓存值，并转换为指定泛型类型后按 Key 组装为 Map。
     *
     * @param keys          Redis Key 集合
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return Key 与转换后缓存值的映射关系
     */
    <T> Map<String, T> multiGetAsMap(Collection<String> keys, TypeReference<T> typeReference);

    /**
     * 将指定 Key 的整数值自增 1。
     *
     * @param key Redis Key
     * @return 自增后的值；参数无效时返回 0
     */
    long increment(String key);

    /**
     * 将指定 Key 的整数值按指定步长自增。
     *
     * @param key   Redis Key
     * @param delta 自增步长
     * @return 自增后的值；参数无效时返回 0
     */
    long increment(String key, long delta);

    /**
     * 将指定 Key 的浮点数值按指定步长自增。
     *
     * @param key   Redis Key
     * @param delta 自增步长
     * @return 自增后的值；参数无效时返回 0
     */
    double increment(String key, double delta);

    /**
     * 将指定 Key 的整数值自减 1。
     *
     * @param key Redis Key
     * @return 自减后的值；参数无效时返回 0
     */
    long decrement(String key);

    /**
     * 将指定 Key 的整数值按指定步长自减。
     *
     * @param key   Redis Key
     * @param delta 自减步长
     * @return 自减后的值；参数无效时返回 0
     */
    long decrement(String key, long delta);

    /**
     * 获取缓存值；缓存不存在时通过 Supplier 加载数据并写入缓存。
     *
     * @param key      Redis Key
     * @param clazz    目标类型
     * @param supplier 数据加载函数
     * @param <T>      目标泛型类型
     * @return 缓存值或加载后的数据；参数无效或加载结果为空时返回 null
     */
    <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> supplier);

    /**
     * 获取缓存值；缓存不存在时通过 Supplier 加载数据并写入缓存，同时设置过期时间。
     *
     * @param key      Redis Key
     * @param clazz    目标类型
     * @param supplier 数据加载函数
     * @param timeout  缓存过期时间
     * @param <T>      目标泛型类型
     * @return 缓存值或加载后的数据；参数无效或加载结果为空时返回 null
     */
    <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> supplier, Duration timeout);

    /**
     * 获取缓存值并转换为指定泛型类型；缓存不存在时通过 Supplier 加载数据并写入缓存。
     *
     * @param key           Redis Key
     * @param typeReference 目标泛型类型引用
     * @param supplier      数据加载函数
     * @param <T>           目标泛型类型
     * @return 缓存值或加载后的数据；参数无效或加载结果为空时返回 null
     */
    <T> T getOrLoad(String key, TypeReference<T> typeReference, Supplier<T> supplier);

    /**
     * 获取缓存值并转换为指定泛型类型；缓存不存在时通过 Supplier 加载数据并写入缓存，同时设置过期时间。
     *
     * @param key           Redis Key
     * @param typeReference 目标泛型类型引用
     * @param supplier      数据加载函数
     * @param timeout       缓存过期时间
     * @param <T>           目标泛型类型
     * @return 缓存值或加载后的数据；参数无效或加载结果为空时返回 null
     */
    <T> T getOrLoad(String key, TypeReference<T> typeReference, Supplier<T> supplier, Duration timeout);

    /**
     * 获取缓存值；缓存不存在时加锁后通过 Supplier 加载数据并写入缓存。
     *
     * @param key           Redis Key
     * @param lockKey       分布式锁 Key
     * @param clazz         目标类型
     * @param supplier      数据加载函数
     * @param cacheTimeout  缓存过期时间
     * @param lockWaitTime  获取锁最大等待时间
     * @param lockLeaseTime 锁自动释放时间
     * @param <T>           目标泛型类型
     * @return 缓存值或加载后的数据；参数无效、获取锁失败或加载结果为空时返回 null
     */
    <T> T getOrLoadWithLock(String key, String lockKey, Class<T> clazz, Supplier<T> supplier, Duration cacheTimeout, Duration lockWaitTime, Duration lockLeaseTime);

    /**
     * 获取缓存值并转换为指定泛型类型；缓存不存在时加锁后通过 Supplier 加载数据并写入缓存。
     *
     * @param key           Redis Key
     * @param lockKey       分布式锁 Key
     * @param typeReference 目标泛型类型引用
     * @param supplier      数据加载函数
     * @param cacheTimeout  缓存过期时间
     * @param lockWaitTime  获取锁最大等待时间
     * @param lockLeaseTime 锁自动释放时间
     * @param <T>           目标泛型类型
     * @return 缓存值或加载后的数据；参数无效、获取锁失败或加载结果为空时返回 null
     */
    <T> T getOrLoadWithLock(String key, String lockKey, TypeReference<T> typeReference, Supplier<T> supplier, Duration cacheTimeout, Duration lockWaitTime, Duration lockLeaseTime);

    // ==============================
    // Hash 操作
    // ==============================

    /**
     * 设置 Hash 中指定字段的值。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段 Key
     * @param value   Hash 字段值
     */
    void hPut(String key, Object hashKey, Object value);

    /**
     * 批量设置 Hash 字段和值。
     *
     * @param key Redis Key
     * @param map Hash 字段和值映射
     */
    void hPutAll(String key, Map<?, ?> map);

    /**
     * 当 Hash 字段不存在时设置字段值。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段 Key
     * @param value   Hash 字段值
     * @return true 表示设置成功，false 表示字段已存在或参数无效
     */
    boolean hPutIfAbsent(String key, Object hashKey, Object value);

    /**
     * 获取 Hash 中指定字段的值。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段 Key
     * @return Hash 字段值；Key 或字段不存在时返回 null
     */
    Object hGet(String key, Object hashKey);

    /**
     * 获取 Hash 中指定字段的值，并转换为指定类型。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段 Key
     * @param clazz   目标类型
     * @param <T>     目标泛型类型
     * @return 转换后的字段值；Key 或字段不存在、参数无效或转换失败时返回 null
     */
    <T> T hGet(String key, Object hashKey, Class<T> clazz);

    /**
     * 获取 Hash 中指定字段的值，并转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param hashKey       Hash 字段 Key
     * @param typeReference 目标泛型类型引用，例如 List<User>、Map<String, User>
     * @param <T>           目标泛型类型
     * @return 转换后的字段值；Key 或字段不存在、参数无效或转换失败时返回 null
     */
    <T> T hGet(String key, Object hashKey, TypeReference<T> typeReference);

    /**
     * 批量获取 Hash 中多个字段的值。
     *
     * @param key      Redis Key
     * @param hashKeys Hash 字段 Key 集合
     * @return Hash 字段值列表，返回顺序与 hashKeys 顺序一致
     */
    List<Object> hMultiGet(String key, Collection<?> hashKeys);

    /**
     * 批量获取 Hash 中多个字段的值，并逐个转换为指定类型。
     *
     * @param key      Redis Key
     * @param hashKeys Hash 字段 Key 集合
     * @param clazz    目标类型
     * @param <T>      目标泛型类型
     * @return 转换后的字段值列表，返回顺序与 hashKeys 顺序一致
     */
    <T> List<T> hMultiGet(String key, Collection<?> hashKeys, Class<T> clazz);

    /**
     * 批量获取 Hash 中多个字段的值，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param hashKeys      Hash 字段 Key 集合
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的字段值列表，返回顺序与 hashKeys 顺序一致
     */
    <T> List<T> hMultiGet(String key, Collection<?> hashKeys, TypeReference<T> typeReference);

    /**
     * 获取 Hash 中所有字段和值。
     *
     * @param key Redis Key
     * @return Hash 字段和值映射；Key 不存在或参数无效时返回空 Map
     */
    Map<Object, Object> hGetAll(String key);

    /**
     * 获取 Hash 中所有字段和值，并将字段值转换为指定类型。
     *
     * @param key   Redis Key
     * @param clazz 字段值目标类型
     * @param <T>   字段值目标泛型类型
     * @return Hash 字段与转换后字段值的映射
     */
    <T> Map<Object, T> hGetAll(String key, Class<T> clazz);

    /**
     * 获取 Hash 中所有字段和值，并将字段值转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param typeReference 字段值目标泛型类型引用
     * @param <T>           字段值目标泛型类型
     * @return Hash 字段与转换后字段值的映射
     */
    <T> Map<Object, T> hGetAll(String key, TypeReference<T> typeReference);

    /**
     * 判断 Hash 中指定字段是否存在。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段 Key
     * @return true 表示存在，false 表示不存在或参数无效
     */
    boolean hHasKey(String key, Object hashKey);

    /**
     * 删除 Hash 中一个或多个字段。
     *
     * @param key      Redis Key
     * @param hashKeys Hash 字段 Key 数组
     * @return 成功删除的字段数量
     */
    long hDelete(String key, Object... hashKeys);

    /**
     * 获取 Hash 字段数量。
     *
     * @param key Redis Key
     * @return Hash 字段数量；Key 不存在或参数无效时返回 0
     */
    long hSize(String key);

    /**
     * 获取 Hash 中所有字段 Key。
     *
     * @param key Redis Key
     * @return Hash 字段 Key 集合；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> hKeys(String key);

    /**
     * 获取 Hash 中所有字段值。
     *
     * @param key Redis Key
     * @return Hash 字段值列表；Key 不存在或参数无效时返回空 List
     */
    List<Object> hValues(String key);

    /**
     * 获取 Hash 中所有字段值，并逐个转换为指定类型。
     *
     * @param key   Redis Key
     * @param clazz 字段值目标类型
     * @param <T>   字段值目标泛型类型
     * @return 转换后的 Hash 字段值列表
     */
    <T> List<T> hValues(String key, Class<T> clazz);

    /**
     * 获取 Hash 中所有字段值，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param typeReference 字段值目标泛型类型引用
     * @param <T>           字段值目标泛型类型
     * @return 转换后的 Hash 字段值列表
     */
    <T> List<T> hValues(String key, TypeReference<T> typeReference);

    /**
     * 将 Hash 中指定字段的整数值按指定步长递增。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段 Key
     * @param delta   递增步长
     * @return 递增后的字段值；参数无效时返回 0
     */
    long hIncrement(String key, Object hashKey, long delta);

    /**
     * 将 Hash 中指定字段的浮点数值按指定步长递增。
     *
     * @param key     Redis Key
     * @param hashKey Hash 字段 Key
     * @param delta   递增步长
     * @return 递增后的字段值；参数无效时返回 0
     */
    double hIncrement(String key, Object hashKey, double delta);


    // ==============================
    // List 操作
    // ==============================

    /**
     * 从 List 左侧插入一个元素。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @return 插入后 List 的长度；参数无效时返回 0
     */
    long lLeftPush(String key, Object value);

    /**
     * 从 List 左侧批量插入元素。
     *
     * @param key    Redis Key
     * @param values 元素值集合
     * @return 插入后 List 的长度；参数无效时返回 0
     */
    long lLeftPushAll(String key, Collection<?> values);

    /**
     * 从 List 右侧插入一个元素。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @return 插入后 List 的长度；参数无效时返回 0
     */
    long lRightPush(String key, Object value);

    /**
     * 从 List 右侧批量插入元素。
     *
     * @param key    Redis Key
     * @param values 元素值集合
     * @return 插入后 List 的长度；参数无效时返回 0
     */
    long lRightPushAll(String key, Collection<?> values);

    /**
     * 从 List 左侧弹出一个元素。
     *
     * @param key Redis Key
     * @return 弹出的元素；Key 不存在、List 为空或参数无效时返回 null
     */
    Object lLeftPop(String key);

    /**
     * 从 List 左侧阻塞弹出一个元素。
     *
     * @param key     Redis Key
     * @param timeout 阻塞等待时间
     * @return 弹出的元素；超时、Key 不存在、List 为空或参数无效时返回 null
     */
    Object lLeftPop(String key, Duration timeout);

    /**
     * 从 List 左侧弹出一个元素，并转换为指定类型。
     *
     * @param key   Redis Key
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素；弹出为空、参数无效或转换失败时返回 null
     */
    <T> T lLeftPop(String key, Class<T> clazz);

    /**
     * 从 List 左侧弹出一个元素，并转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素；弹出为空、参数无效或转换失败时返回 null
     */
    <T> T lLeftPop(String key, TypeReference<T> typeReference);

    /**
     * 从 List 右侧弹出一个元素。
     *
     * @param key Redis Key
     * @return 弹出的元素；Key 不存在、List 为空或参数无效时返回 null
     */
    Object lRightPop(String key);

    /**
     * 从 List 右侧阻塞弹出一个元素。
     *
     * @param key     Redis Key
     * @param timeout 阻塞等待时间
     * @return 弹出的元素；超时、Key 不存在、List 为空或参数无效时返回 null
     */
    Object lRightPop(String key, Duration timeout);

    /**
     * 从 List 右侧弹出一个元素，并转换为指定类型。
     *
     * @param key   Redis Key
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素；弹出为空、参数无效或转换失败时返回 null
     */
    <T> T lRightPop(String key, Class<T> clazz);

    /**
     * 从 List 右侧弹出一个元素，并转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素；弹出为空、参数无效或转换失败时返回 null
     */
    <T> T lRightPop(String key, TypeReference<T> typeReference);

    /**
     * 根据索引获取 List 中的元素。
     *
     * @param key   Redis Key
     * @param index 元素索引，支持负数索引
     * @return 指定索引位置的元素；索引不存在、Key 不存在或参数无效时返回 null
     */
    Object lIndex(String key, long index);

    /**
     * 根据索引获取 List 中的元素，并转换为指定类型。
     *
     * @param key   Redis Key
     * @param index 元素索引，支持负数索引
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素；索引不存在、参数无效或转换失败时返回 null
     */
    <T> T lIndex(String key, long index, Class<T> clazz);

    /**
     * 根据索引获取 List 中的元素，并转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param index         元素索引，支持负数索引
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素；索引不存在、参数无效或转换失败时返回 null
     */
    <T> T lIndex(String key, long index, TypeReference<T> typeReference);

    /**
     * 获取 List 指定范围内的元素。
     *
     * @param key   Redis Key
     * @param start 开始索引，支持负数索引
     * @param end   结束索引，支持负数索引
     * @return 指定范围内的元素列表；Key 不存在或参数无效时返回空 List
     */
    List<Object> lRange(String key, long start, long end);

    /**
     * 获取 List 指定范围内的元素，并逐个转换为指定类型。
     *
     * @param key   Redis Key
     * @param start 开始索引，支持负数索引
     * @param end   结束索引，支持负数索引
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素列表；Key 不存在、参数无效或转换失败时返回空 List
     */
    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    /**
     * 获取 List 指定范围内的元素，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param start         开始索引，支持负数索引
     * @param end           结束索引，支持负数索引
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素列表；Key 不存在、参数无效或转换失败时返回空 List
     */
    <T> List<T> lRange(String key, long start, long end, TypeReference<T> typeReference);

    /**
     * 根据索引设置 List 中的元素值。
     *
     * @param key   Redis Key
     * @param index 元素索引
     * @param value 新元素值
     */
    void lSet(String key, long index, Object value);

    /**
     * 裁剪 List，只保留指定范围内的元素。
     *
     * @param key   Redis Key
     * @param start 开始索引，支持负数索引
     * @param end   结束索引，支持负数索引
     */
    void lTrim(String key, long start, long end);

    /**
     * 删除 List 中指定数量的匹配元素。
     *
     * @param key   Redis Key
     * @param count 删除数量；大于 0 从左到右删除，小于 0 从右到左删除，等于 0 删除全部匹配元素
     * @param value 要删除的元素值
     * @return 成功删除的元素数量；参数无效时返回 0
     */
    long lRemove(String key, long count, Object value);

    /**
     * 获取 List 长度。
     *
     * @param key Redis Key
     * @return List 长度；Key 不存在或参数无效时返回 0
     */
    long lSize(String key);


    // ==============================
    // Set 操作
    // ==============================

    /**
     * 向 Set 中添加一个或多个元素。
     *
     * @param key    Redis Key
     * @param values 元素值数组
     * @return 成功添加的元素数量；参数无效时返回 0
     */
    long sAdd(String key, Object... values);

    /**
     * 从 Set 中移除一个或多个元素。
     *
     * @param key    Redis Key
     * @param values 元素值数组
     * @return 成功移除的元素数量；参数无效时返回 0
     */
    long sRemove(String key, Object... values);

    /**
     * 判断指定元素是否为 Set 成员。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @return true 表示元素存在，false 表示元素不存在或参数无效
     */
    boolean sIsMember(String key, Object value);

    /**
     * 获取 Set 中所有元素。
     *
     * @param key Redis Key
     * @return Set 元素集合；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> sMembers(String key);

    /**
     * 获取 Set 中所有元素，并逐个转换为指定类型。
     *
     * @param key   Redis Key
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的 Set 元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> sMembers(String key, Class<T> clazz);

    /**
     * 获取 Set 中所有元素，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的 Set 元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> sMembers(String key, TypeReference<T> typeReference);

    /**
     * 从 Set 中随机弹出一个元素。
     *
     * @param key Redis Key
     * @return 弹出的元素；Key 不存在、Set 为空或参数无效时返回 null
     */
    Object sPop(String key);

    /**
     * 从 Set 中随机弹出一个元素，并转换为指定类型。
     *
     * @param key   Redis Key
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素；弹出为空、参数无效或转换失败时返回 null
     */
    <T> T sPop(String key, Class<T> clazz);

    /**
     * 从 Set 中随机弹出一个元素，并转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素；弹出为空、参数无效或转换失败时返回 null
     */
    <T> T sPop(String key, TypeReference<T> typeReference);

    /**
     * 从 Set 中随机弹出指定数量的元素。
     *
     * @param key   Redis Key
     * @param count 弹出数量
     * @return 弹出的元素列表；Key 不存在、Set 为空或参数无效时返回空 List
     */
    List<Object> sPop(String key, long count);

    /**
     * 从 Set 中随机弹出指定数量的元素，并逐个转换为指定类型。
     *
     * @param key   Redis Key
     * @param count 弹出数量
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素列表；Key 不存在、Set 为空、参数无效或转换失败时返回空 List
     */
    <T> List<T> sPop(String key, long count, Class<T> clazz);

    /**
     * 从 Set 中随机弹出指定数量的元素，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param count         弹出数量
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素列表；Key 不存在、Set 为空、参数无效或转换失败时返回空 List
     */
    <T> List<T> sPop(String key, long count, TypeReference<T> typeReference);

    /**
     * 获取 Set 元素数量。
     *
     * @param key Redis Key
     * @return Set 元素数量；Key 不存在或参数无效时返回 0
     */
    long sSize(String key);

    /**
     * 获取两个 Set 的交集。
     *
     * @param key      Redis Key
     * @param otherKey 另一个 Redis Key
     * @return 交集元素集合；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> sIntersect(String key, String otherKey);

    /**
     * 获取两个 Set 的交集，并逐个转换为指定类型。
     *
     * @param key      Redis Key
     * @param otherKey 另一个 Redis Key
     * @param clazz    目标类型
     * @param <T>      目标泛型类型
     * @return 转换后的交集元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> sIntersect(String key, String otherKey, Class<T> clazz);

    /**
     * 获取两个 Set 的交集，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param otherKey      另一个 Redis Key
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的交集元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> sIntersect(String key, String otherKey, TypeReference<T> typeReference);

    /**
     * 获取两个 Set 的并集。
     *
     * @param key      Redis Key
     * @param otherKey 另一个 Redis Key
     * @return 并集元素集合；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> sUnion(String key, String otherKey);

    /**
     * 获取两个 Set 的并集，并逐个转换为指定类型。
     *
     * @param key      Redis Key
     * @param otherKey 另一个 Redis Key
     * @param clazz    目标类型
     * @param <T>      目标泛型类型
     * @return 转换后的并集元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> sUnion(String key, String otherKey, Class<T> clazz);

    /**
     * 获取两个 Set 的并集，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param otherKey      另一个 Redis Key
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的并集元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> sUnion(String key, String otherKey, TypeReference<T> typeReference);

    /**
     * 获取两个 Set 的差集。
     *
     * @param key      Redis Key
     * @param otherKey 另一个 Redis Key
     * @return 差集元素集合，即 key 中存在但 otherKey 中不存在的元素；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> sDifference(String key, String otherKey);

    /**
     * 获取两个 Set 的差集，并逐个转换为指定类型。
     *
     * @param key      Redis Key
     * @param otherKey 另一个 Redis Key
     * @param clazz    目标类型
     * @param <T>      目标泛型类型
     * @return 转换后的差集元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> sDifference(String key, String otherKey, Class<T> clazz);

    /**
     * 获取两个 Set 的差集，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param otherKey      另一个 Redis Key
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的差集元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> sDifference(String key, String otherKey, TypeReference<T> typeReference);


    // ==============================
    // ZSet 操作
    // ==============================

    /**
     * 向 ZSet 中添加元素及其分数。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @param score 元素分数
     * @return true 表示添加成功，false 表示添加失败或参数无效
     */
    boolean zAdd(String key, Object value, double score);

    /**
     * 从 ZSet 中移除一个或多个元素。
     *
     * @param key    Redis Key
     * @param values 元素值数组
     * @return 成功移除的元素数量；参数无效时返回 0
     */
    long zRemove(String key, Object... values);

    /**
     * 获取 ZSet 中指定元素的分数。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @return 元素分数；元素不存在、Key 不存在或参数无效时返回 null
     */
    Double zScore(String key, Object value);

    /**
     * 获取 ZSet 中指定元素的正序排名。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @return 元素排名，排名从 0 开始；元素不存在、Key 不存在或参数无效时返回 null
     */
    Long zRank(String key, Object value);

    /**
     * 获取 ZSet 中指定元素的倒序排名。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @return 元素倒序排名，排名从 0 开始；元素不存在、Key 不存在或参数无效时返回 null
     */
    Long zReverseRank(String key, Object value);

    /**
     * 获取 ZSet 元素数量。
     *
     * @param key Redis Key
     * @return ZSet 元素数量；Key 不存在或参数无效时返回 0
     */
    long zSize(String key);

    /**
     * 获取 ZSet 中指定分数区间内的元素数量。
     *
     * @param key Redis Key
     * @param min 最小分数
     * @param max 最大分数
     * @return 指定分数区间内的元素数量；Key 不存在或参数无效时返回 0
     */
    long zCount(String key, double min, double max);

    /**
     * 按正序排名范围获取 ZSet 元素。
     *
     * @param key   Redis Key
     * @param start 开始排名，支持负数索引
     * @param end   结束排名，支持负数索引
     * @return 指定排名范围内的元素集合；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> zRange(String key, long start, long end);

    /**
     * 按正序排名范围获取 ZSet 元素，并逐个转换为指定类型。
     *
     * @param key   Redis Key
     * @param start 开始排名，支持负数索引
     * @param end   结束排名，支持负数索引
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> zRange(String key, long start, long end, Class<T> clazz);

    /**
     * 按正序排名范围获取 ZSet 元素，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param start         开始排名，支持负数索引
     * @param end           结束排名，支持负数索引
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> zRange(String key, long start, long end, TypeReference<T> typeReference);

    /**
     * 按倒序排名范围获取 ZSet 元素。
     *
     * @param key   Redis Key
     * @param start 开始排名，支持负数索引
     * @param end   结束排名，支持负数索引
     * @return 指定倒序排名范围内的元素集合；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> zReverseRange(String key, long start, long end);

    /**
     * 按倒序排名范围获取 ZSet 元素，并逐个转换为指定类型。
     *
     * @param key   Redis Key
     * @param start 开始排名，支持负数索引
     * @param end   结束排名，支持负数索引
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> zReverseRange(String key, long start, long end, Class<T> clazz);

    /**
     * 按倒序排名范围获取 ZSet 元素，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param start         开始排名，支持负数索引
     * @param end           结束排名，支持负数索引
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> zReverseRange(String key, long start, long end, TypeReference<T> typeReference);

    /**
     * 按分数区间获取 ZSet 元素。
     *
     * @param key Redis Key
     * @param min 最小分数
     * @param max 最大分数
     * @return 指定分数区间内的元素集合；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> zRangeByScore(String key, double min, double max);

    /**
     * 按分数区间获取 ZSet 元素，并逐个转换为指定类型。
     *
     * @param key   Redis Key
     * @param min   最小分数
     * @param max   最大分数
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 转换后的元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, Class<T> clazz);

    /**
     * 按分数区间获取 ZSet 元素，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param min           最小分数
     * @param max           最大分数
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, TypeReference<T> typeReference);

    /**
     * 按分数区间分页获取 ZSet 元素。
     *
     * @param key    Redis Key
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量，从 0 开始
     * @param count  返回数量
     * @return 指定分数区间内分页后的元素集合；Key 不存在或参数无效时返回空 Set
     */
    Set<Object> zRangeByScore(String key, double min, double max, long offset, long count);

    /**
     * 按分数区间分页获取 ZSet 元素，并逐个转换为指定类型。
     *
     * @param key    Redis Key
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量，从 0 开始
     * @param count  返回数量
     * @param clazz  目标类型
     * @param <T>    目标泛型类型
     * @return 转换后的元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, long offset, long count, Class<T> clazz);

    /**
     * 按分数区间分页获取 ZSet 元素，并逐个转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param min           最小分数
     * @param max           最大分数
     * @param offset        偏移量，从 0 开始
     * @param count         返回数量
     * @param typeReference 目标泛型类型引用
     * @param <T>           目标泛型类型
     * @return 转换后的元素集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, long offset, long count, TypeReference<T> typeReference);

    /**
     * 按正序排名范围获取 ZSet 元素及其分数。
     *
     * @param key   Redis Key
     * @param start 开始排名，支持负数索引
     * @param end   结束排名，支持负数索引
     * @return 元素和值分数元组集合；Key 不存在或参数无效时返回空 Set
     */
    Set<ZSetOperations.TypedTuple<Object>> zRangeWithScores(String key, long start, long end);

    /**
     * 按正序排名范围获取 ZSet 元素及其分数，并将元素值转换为指定类型。
     *
     * @param key   Redis Key
     * @param start 开始排名，支持负数索引
     * @param end   结束排名，支持负数索引
     * @param clazz 元素值目标类型
     * @param <T>   元素值目标泛型类型
     * @return 转换后的元素和值分数元组集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<ZSetOperations.TypedTuple<T>> zRangeWithScores(String key, long start, long end, Class<T> clazz);

    /**
     * 按正序排名范围获取 ZSet 元素及其分数，并将元素值转换为指定泛型类型。
     *
     * @param key           Redis Key
     * @param start         开始排名，支持负数索引
     * @param end           结束排名，支持负数索引
     * @param typeReference 元素值目标泛型类型引用
     * @param <T>           元素值目标泛型类型
     * @return 转换后的元素和值分数元组集合；Key 不存在、参数无效或转换失败时返回空 Set
     */
    <T> Set<ZSetOperations.TypedTuple<T>> zRangeWithScores(String key, long start, long end, TypeReference<T> typeReference);

    /**
     * 按排名范围删除 ZSet 元素。
     *
     * @param key   Redis Key
     * @param start 开始排名，支持负数索引
     * @param end   结束排名，支持负数索引
     * @return 成功删除的元素数量；参数无效时返回 0
     */
    long zRemoveRange(String key, long start, long end);

    /**
     * 按分数区间删除 ZSet 元素。
     *
     * @param key Redis Key
     * @param min 最小分数
     * @param max 最大分数
     * @return 成功删除的元素数量；参数无效时返回 0
     */
    long zRemoveRangeByScore(String key, double min, double max);

    /**
     * 将 ZSet 中指定元素的分数按指定步长递增。
     *
     * @param key   Redis Key
     * @param value 元素值
     * @param delta 递增步长
     * @return 递增后的分数；参数无效时返回 null
     */
    Double zIncrementScore(String key, Object value, double delta);

    // ==============================
    // 分布式锁
    // ==============================

    /**
     * 尝试获取分布式锁。
     *
     * @param lockKey   锁 Key
     * @param lockValue 锁值，通常使用唯一标识
     * @param leaseTime 锁自动释放时间
     * @return true 表示获取锁成功，false 表示获取锁失败或参数无效
     */
    boolean tryLock(String lockKey, String lockValue, Duration leaseTime);

    /**
     * 在指定等待时间内尝试获取分布式锁。
     *
     * @param lockKey   锁 Key
     * @param lockValue 锁值，通常使用唯一标识
     * @param waitTime  获取锁最大等待时间
     * @param leaseTime 锁自动释放时间
     * @return true 表示获取锁成功，false 表示等待超时、获取锁失败或参数无效
     */
    boolean tryLock(String lockKey, String lockValue, Duration waitTime, Duration leaseTime);

    /**
     * 释放分布式锁。
     *
     * @param lockKey   锁 Key
     * @param lockValue 锁值，必须与加锁时的锁值一致
     * @return true 表示释放成功，false 表示锁不存在、锁值不匹配或参数无效
     */
    boolean unlock(String lockKey, String lockValue);

    /**
     * 尝试获取分布式锁并执行无返回值任务。
     *
     * @param lockKey   锁 Key
     * @param leaseTime 锁自动释放时间
     * @param task      加锁成功后执行的任务
     * @return true 表示获取锁并执行任务成功，false 表示获取锁失败或参数无效
     */
    boolean tryExecuteWithLock(String lockKey, Duration leaseTime, Runnable task);

    /**
     * 在指定等待时间内尝试获取分布式锁并执行无返回值任务。
     *
     * @param lockKey   锁 Key
     * @param waitTime  获取锁最大等待时间
     * @param leaseTime 锁自动释放时间
     * @param task      加锁成功后执行的任务
     * @return true 表示获取锁并执行任务成功，false 表示等待超时、获取锁失败或参数无效
     */
    boolean tryExecuteWithLock(String lockKey, Duration waitTime, Duration leaseTime, Runnable task);

    /**
     * 尝试获取分布式锁并执行有返回值任务。
     *
     * @param lockKey   锁 Key
     * @param leaseTime 锁自动释放时间
     * @param supplier  加锁成功后执行的数据提供函数
     * @param <T>       返回值泛型类型
     * @return Optional 包装的任务结果；获取锁失败、参数无效或任务返回 null 时返回 Optional.empty()
     */
    <T> Optional<T> tryExecuteWithLock(String lockKey, Duration leaseTime, Supplier<T> supplier);

    /**
     * 在指定等待时间内尝试获取分布式锁并执行有返回值任务。
     *
     * @param lockKey   锁 Key
     * @param waitTime  获取锁最大等待时间
     * @param leaseTime 锁自动释放时间
     * @param supplier  加锁成功后执行的数据提供函数
     * @param <T>       返回值泛型类型
     * @return Optional 包装的任务结果；等待超时、获取锁失败、参数无效或任务返回 null 时返回 Optional.empty()
     */
    <T> Optional<T> tryExecuteWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> supplier);

    /**
     * 获取分布式锁并执行无返回值任务。
     *
     * @param lockKey   锁 Key
     * @param leaseTime 锁自动释放时间
     * @param task      加锁成功后执行的任务
     */
    void executeWithLock(String lockKey, Duration leaseTime, Runnable task);

    /**
     * 在指定等待时间内获取分布式锁并执行无返回值任务。
     *
     * @param lockKey   锁 Key
     * @param waitTime  获取锁最大等待时间
     * @param leaseTime 锁自动释放时间
     * @param task      加锁成功后执行的任务
     */
    void executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Runnable task);

    /**
     * 获取分布式锁并执行有返回值任务。
     *
     * @param lockKey   锁 Key
     * @param leaseTime 锁自动释放时间
     * @param supplier  加锁成功后执行的数据提供函数
     * @param <T>       返回值泛型类型
     * @return 任务执行结果
     */
    <T> T executeWithLock(String lockKey, Duration leaseTime, Supplier<T> supplier);

    /**
     * 在指定等待时间内获取分布式锁并执行有返回值任务。
     *
     * @param lockKey   锁 Key
     * @param waitTime  获取锁最大等待时间
     * @param leaseTime 锁自动释放时间
     * @param supplier  加锁成功后执行的数据提供函数
     * @param <T>       返回值泛型类型
     * @return 任务执行结果
     */
    <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> supplier);

    // ==============================
    // Lua 脚本操作
    // ==============================

    /**
     * 执行 Lua 脚本并返回原始结果。
     *
     * @param script Lua 脚本文本
     * @param keys   Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args   脚本参数数组，对应 Lua 脚本中的 ARGV
     * @return Lua 脚本执行结果；参数无效或执行失败时返回 null
     */
    Object executeLua(String script, List<String> keys, Object... args);

    /**
     * 执行 Lua 脚本，并将结果转换为指定类型。
     *
     * @param script Lua 脚本文本
     * @param clazz  目标返回类型
     * @param keys   Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args   脚本参数数组，对应 Lua 脚本中的 ARGV
     * @param <T>    返回值泛型类型
     * @return 转换后的 Lua 脚本执行结果；参数无效、执行失败或转换失败时返回 null
     */
    <T> T executeLua(String script, Class<T> clazz, List<String> keys, Object... args);

    /**
     * 执行 Lua 脚本，并将结果转换为指定泛型类型。
     *
     * @param script        Lua 脚本文本
     * @param typeReference 目标返回泛型类型引用
     * @param keys          Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args          脚本参数数组，对应 Lua 脚本中的 ARGV
     * @param <T>           返回值泛型类型
     * @return 转换后的 Lua 脚本执行结果；参数无效、执行失败或转换失败时返回 null
     */
    <T> T executeLua(String script, TypeReference<T> typeReference, List<String> keys, Object... args);

    /**
     * 从资源文件读取 Lua 脚本并执行，返回原始结果。
     *
     * @param resourceLocation Lua 脚本资源路径，例如 lua/stock_decrease.lua 或 classpath:lua/stock_decrease.lua
     * @param keys             Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args             脚本参数数组，对应 Lua 脚本中的 ARGV
     * @return Lua 脚本执行结果；资源不存在、参数无效或执行失败时返回 null
     */
    Object executeLuaFromResource(String resourceLocation, List<String> keys, Object... args);

    /**
     * 从资源文件读取 Lua 脚本并执行，将结果转换为指定类型。
     *
     * @param resourceLocation Lua 脚本资源路径，例如 lua/stock_decrease.lua 或 classpath:lua/stock_decrease.lua
     * @param clazz            目标返回类型
     * @param keys             Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args             脚本参数数组，对应 Lua 脚本中的 ARGV
     * @param <T>              返回值泛型类型
     * @return 转换后的 Lua 脚本执行结果；资源不存在、参数无效、执行失败或转换失败时返回 null
     */
    <T> T executeLuaFromResource(String resourceLocation, Class<T> clazz, List<String> keys, Object... args);

    /**
     * 从资源文件读取 Lua 脚本并执行，将结果转换为指定泛型类型。
     *
     * @param resourceLocation Lua 脚本资源路径，例如 lua/stock_decrease.lua 或 classpath:lua/stock_decrease.lua
     * @param typeReference    目标返回泛型类型引用
     * @param keys             Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args             脚本参数数组，对应 Lua 脚本中的 ARGV
     * @param <T>              返回值泛型类型
     * @return 转换后的 Lua 脚本执行结果；资源不存在、参数无效、执行失败或转换失败时返回 null
     */
    <T> T executeLuaFromResource(String resourceLocation, TypeReference<T> typeReference, List<String> keys, Object... args);

    /**
     * 执行 Lua 脚本并按 Boolean 结果返回。
     *
     * @param script Lua 脚本文本
     * @param keys   Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args   脚本参数数组，对应 Lua 脚本中的 ARGV
     * @return true 表示脚本返回 true，false 表示脚本返回 false、参数无效或执行失败
     */
    boolean executeLuaAsBoolean(String script, List<String> keys, Object... args);

    /**
     * 执行 Lua 脚本并按 Long 结果返回。
     *
     * @param script Lua 脚本文本
     * @param keys   Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args   脚本参数数组，对应 Lua 脚本中的 ARGV
     * @return Lua 脚本返回的 Long 值；参数无效或执行失败时返回 0
     */
    long executeLuaAsLong(String script, List<String> keys, Object... args);

    /**
     * 执行 Lua 脚本并按 String 结果返回。
     *
     * @param script Lua 脚本文本
     * @param keys   Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args   脚本参数数组，对应 Lua 脚本中的 ARGV
     * @return Lua 脚本返回的字符串；参数无效或执行失败时返回 null
     */
    String executeLuaAsString(String script, List<String> keys, Object... args);

    /**
     * 执行 Lua 脚本并按 List 结果返回。
     *
     * @param script Lua 脚本文本
     * @param keys   Redis Key 列表，对应 Lua 脚本中的 KEYS
     * @param args   脚本参数数组，对应 Lua 脚本中的 ARGV
     * @return Lua 脚本返回的列表；参数无效、执行失败或结果为空时返回空 List
     */
    List<Object> executeLuaAsList(String script, List<String> keys, Object... args);

    /**
     * 使用 Lua 脚本比较并删除指定 Key。
     *
     * @param key           Redis Key
     * @param expectedValue 期望值，只有当前值与期望值一致时才删除
     * @return true 表示删除成功，false 表示 Key 不存在、值不匹配或参数无效
     */
    boolean compareAndDeleteByLua(String key, Object expectedValue);

    /**
     * 使用 Lua 脚本比较并设置指定 Key 的新值。
     *
     * @param key           Redis Key
     * @param expectedValue 期望值，只有当前值与期望值一致时才设置新值
     * @param newValue      新值
     * @return true 表示设置成功，false 表示 Key 不存在、值不匹配或参数无效
     */
    boolean compareAndSetByLua(String key, Object expectedValue, Object newValue);

    /**
     * 使用 Lua 脚本比较并设置指定 Key 的新值，同时设置过期时间。
     *
     * @param key           Redis Key
     * @param expectedValue 期望值，只有当前值与期望值一致时才设置新值
     * @param newValue      新值
     * @param timeout       过期时间
     * @return true 表示设置成功，false 表示 Key 不存在、值不匹配或参数无效
     */
    boolean compareAndSetByLua(String key, Object expectedValue, Object newValue, Duration timeout);

    /**
     * 使用 Lua 脚本对指定 Key 执行自增并设置过期时间。
     *
     * @param key     Redis Key
     * @param delta   自增步长
     * @param timeout 过期时间
     * @return 自增后的值；参数无效或执行失败时返回 0
     */
    long incrementAndExpireByLua(String key, long delta, Duration timeout);
}