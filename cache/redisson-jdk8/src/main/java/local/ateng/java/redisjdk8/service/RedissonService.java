package local.ateng.java.redisjdk8.service;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务接口
 * 封装常用的 key 操作方法，统一基于 Redisson 实现
 *
 * @author Ateng
 * @since 2025-08-01
 */
public interface RedissonService {

    // -------------------------- 通用 Key 管理 --------------------------

    /**
     * 判断指定 key 是否存在
     *
     * @param key redis 键
     * @return 存在返回 true，否则 false
     */
    boolean hasKey(String key);

    /**
     * 删除指定 key
     *
     * @param key redis 键
     * @return 是否成功删除
     */
    boolean deleteKey(String key);

    /**
     * 批量删除指定 key 集合
     *
     * @param keys redis 键集合
     * @return 成功删除的数量
     */
    long deleteKeys(Set<String> keys);

    /**
     * 设置 key 的过期时间
     *
     * @param key     redis 键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取 key 的剩余过期时间
     *
     * @param key  redis 键
     * @param unit 时间单位
     * @return 剩余时间（-1 表示永久；-2 表示不存在）
     */
    long getTtl(String key, TimeUnit unit);

    /**
     * 让 key 永久不过期（移除过期时间）
     *
     * @param key redis 键
     * @return 是否成功移除
     */
    boolean persist(String key);

    /**
     * 修改 key 名称（key 必须存在，且新 key 不存在）
     *
     * @param oldKey 旧的 redis 键
     * @param newKey 新的 redis 键
     * @return 是否成功
     */
    boolean renameKey(String oldKey, String newKey);

    /**
     * 如果新 key 不存在则重命名
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return 是否成功
     */
    boolean renameKeyIfAbsent(String oldKey, String newKey);

    /**
     * 获取所有匹配 pattern 的 key（慎用，生产环境建议加前缀限制）
     *
     * @param pattern 通配符表达式，如 user:*、session_*
     * @return 匹配的 key 集合
     */
    Set<String> keys(String pattern);

    /**
     * 判断 key 是否已经过期（不存在或 ttl <= 0 视为过期）
     *
     * @param key redis 键
     * @return true 表示已经过期或不存在
     */
    boolean isExpired(String key);

    /**
     * 获取 key 的 value 类型名称
     * （string、list、set、zset、hash 等）
     *
     * @param key redis 键
     * @return 类型名称，若不存在返回 null
     */
    String getKeyType(String key);

    /**
     * 对指定 key 执行原子整数加法操作（适用于计数器）
     *
     * @param key   redis 键
     * @param delta 要增加的整数值（正负均可）
     * @return 操作后的最新值
     */
    long increment(String key, long delta);

    /**
     * 对指定 key 执行原子整数减法操作（适用于计数器）
     *
     * @param key   redis 键
     * @param delta 要减少的整数值（正数）
     * @return 操作后的最新值
     */
    long decrement(String key, long delta);

    /**
     * 对指定 key 执行原子浮点数加法操作（支持 double，适用于余额、分数等）
     *
     * @param key   redis 键
     * @param delta 要增加的浮点数值（正负均可）
     * @return 操作后的最新值
     */
    double incrementDouble(String key, double delta);

    /**
     * 对指定 key 执行原子浮点数减法操作（支持 double）
     *
     * @param key   redis 键
     * @param delta 要减少的浮点数值（正数）
     * @return 操作后的最新值
     */
    double decrementDouble(String key, double delta);

    // -------------------------- 字符串操作 --------------------------

    /**
     * 设置任意对象缓存（无过期时间）
     *
     * @param key   redis 键
     * @param value 要缓存的对象（可以是任意 JavaBean、集合、基本类型等）
     */
    void set(String key, Object value);

    /**
     * 设置任意对象缓存（带过期时间）
     *
     * @param key     redis 键
     * @param value   要缓存的对象
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    void set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 类型转换工具方法：将 Object 转换为指定类型
     *
     * @param value 原始对象
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的对象，或 null（若原始对象为 null）
     */
    <T> T convertValue(Object value, Class<T> clazz);

    /**
     * 类型转换工具方法：将 Object 转换为指定类型
     *
     * @param value         原始对象
     * @param typeReference 目标类型引用（支持泛型）
     * @param <T>           目标类型泛型
     * @return 转换后的对象，失败返回 null
     */
    <T> T convertValue(Object value, TypeReference<T> typeReference);

    /**
     * 获取指定类型的缓存对象
     *
     * @param key   redis 键
     * @param clazz 目标类型（如 User.class）
     * @param <T>   返回值的泛型
     * @return 反序列化后的对象；若 key 不存在返回 null
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 获取指定类型的缓存对象（支持泛型）
     *
     * @param key           redis 键
     * @param typeReference 目标类型引用（支持泛型）
     * @param <T>           返回值泛型
     * @return 反序列化后的对象；若 key 不存在返回 null
     */
    <T> T get(String key, TypeReference<T> typeReference);

    /**
     * 设置对象值，如果 key 不存在才设置（原子操作）
     *
     * @param key     redis 键
     * @param value   对象值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true 表示成功设置，false 表示 key 已存在
     */
    boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 原子替换对象值并返回旧值
     *
     * @param key   redis 键
     * @param value 新值对象
     * @param <T>   旧值的返回类型
     * @return 原先存在的旧值对象；若 key 不存在返回 null
     */
    <T> T getAndSet(String key, Object value, Class<T> clazz);

    /**
     * 原子替换对象值并返回旧值（支持泛型）
     *
     * @param key           redis 键
     * @param value         新值对象
     * @param typeReference 旧值类型引用
     * @param <T>           旧值泛型
     * @return 原先存在的旧值对象；若 key 不存在返回 null
     */
    <T> T getAndSet(String key, Object value, TypeReference<T> typeReference);

    /**
     * 获取对象值的序列化字节大小（不是业务字段长度）
     *
     * @param key redis 键
     * @return 序列化后的大小（单位：字节），不存在时返回 0
     */
    long size(String key);


}
