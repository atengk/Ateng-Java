package local.ateng.java.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.redisson.api.*;
import org.redisson.api.geo.GeoSearchArgs;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.protocol.ScoredEntry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Redis 服务接口
 * 基于 Spring Boot 3 + Redisson 封装 Redis 常用能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
public interface RedissonService {

    // -------------------------------------------------------------------------
    // Redisson 原生对象访问
    // -------------------------------------------------------------------------

    /**
     * 获取 RedissonClient 实例。
     *
     * @return RedissonClient 实例
     */
    RedissonClient getClient();

    /**
     * 获取对象桶。
     *
     * @param key Redis 键
     * @param <T> 值类型
     * @return RBucket
     */
    <T> RBucket<T> getBucket(String key);

    /**
     * 获取哈希 Map。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RMap
     */
    <K, V> RMap<K, V> getMap(String key);

    /**
     * 获取带 TTL 能力的 MapCache。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RMapCache
     */
    <K, V> RMapCache<K, V> getMapCache(String key);

    /**
     * 获取列表。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RList
     */
    <T> RList<T> getList(String key);

    /**
     * 获取双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RDeque
     */
    <T> RDeque<T> getDeque(String key);

    /**
     * 获取集合。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RSet
     */
    <T> RSet<T> getSet(String key);

    /**
     * 获取带元素 TTL 能力的 SetCache。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RSetCache
     */
    <T> RSetCache<T> getSetCache(String key);

    /**
     * 获取有序集合。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RScoredSortedSet
     */
    <T> RScoredSortedSet<T> getScoredSortedSet(String key);

    /**
     * 获取 BitSet。
     *
     * @param key Redis 键
     * @return RBitSet
     */
    RBitSet getBitSet(String key);

    /**
     * 获取 HyperLogLog。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RHyperLogLog
     */
    <T> RHyperLogLog<T> getHyperLogLog(String key);

    /**
     * 获取 Geo。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RGeo
     */
    <T> RGeo<T> getGeo(String key);

    /**
     * 获取普通队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RQueue
     */
    <T> RQueue<T> getQueue(String key);

    /**
     * 获取阻塞队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RBlockingQueue
     */
    <T> RBlockingQueue<T> getBlockingQueue(String key);

    /**
     * 获取可靠队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RReliableQueue
     */
    <T> RReliableQueue<T> getReliableQueue(String key);

    /**
     * 获取消息主题。
     *
     * @param topic 主题名称
     * @return RTopic
     */
    RTopic getTopic(String topic);

    /**
     * 获取 Stream。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RStream
     */
    <K, V> RStream<K, V> getStream(String key);

    /**
     * 获取脚本执行对象。
     *
     * @return RScript
     */
    RScript getScript();

    // -------------------------------------------------------------------------
    // 通用 Key 管理
    // -------------------------------------------------------------------------

    /**
     * 判断指定 key 是否存在。
     *
     * @param key Redis 键
     * @return 存在返回 true
     */
    boolean hasKey(String key);

    /**
     * 统计多个 key 中实际存在的数量。
     *
     * @param keys Redis 键集合
     * @return 存在数量
     */
    long countExists(String... keys);

    /**
     * 删除指定 key。
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    boolean deleteKey(String key);

    /**
     * 批量删除 key。
     *
     * @param keys Redis 键集合
     * @return 删除数量
     */
    long deleteKeys(Collection<String> keys);

    /**
     * 根据通配符删除 key。
     *
     * @param pattern 通配符表达式
     * @return 删除数量
     */
    long deleteByPattern(String pattern);

    /**
     * 设置 key 过期时间。
     *
     * @param key     Redis 键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 设置 key 过期时间。
     *
     * @param key Redis 键
     * @param ttl 过期时间
     * @return 是否设置成功
     */
    boolean expire(String key, Duration ttl);

    /**
     * 获取 key 剩余过期时间。
     *
     * @param key  Redis 键
     * @param unit 时间单位
     * @return 剩余时间，-1 表示永久，-2 表示不存在
     */
    long getTtl(String key, TimeUnit unit);

    /**
     * 移除 key 过期时间。
     *
     * @param key Redis 键
     * @return 是否成功
     */
    boolean persist(String key);

    /**
     * 修改 key 名称。
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return 是否成功
     */
    boolean renameKey(String oldKey, String newKey);

    /**
     * 新 key 不存在时修改 key 名称。
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return 是否成功
     */
    boolean renameKeyIfAbsent(String oldKey, String newKey);

    /**
     * 查询匹配通配符的 key。
     *
     * @param pattern 通配符表达式
     * @return key 集合
     */
    Set<String> keys(String pattern);

    /**
     * 查询匹配通配符的 key，并限制返回数量。
     *
     * @param pattern 通配符表达式
     * @param count   最大数量
     * @return key 集合
     */
    Set<String> scanKeys(String pattern, int count);

    /**
     * 判断 key 是否已过期或不存在。
     *
     * @param key Redis 键
     * @return 已过期或不存在返回 true
     */
    boolean isExpired(String key);

    /**
     * 获取 key 类型。
     *
     * @param key Redis 键
     * @return 类型名称
     */
    String getKeyType(String key);

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
    <T> T convertValue(Object value, Class<T> clazz);

    /**
     * 将对象转换为指定泛型类型。
     *
     * @param value         原始值
     * @param typeReference 目标类型引用
     * @param <T>           泛型类型
     * @return 转换后的值
     */
    <T> T convertValue(Object value, TypeReference<T> typeReference);

    // -------------------------------------------------------------------------
    // 字符串 / Bucket 操作
    // -------------------------------------------------------------------------

    /**
     * 设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 设置缓存值并指定过期时间。
     *
     * @param key     Redis 键
     * @param value   缓存值
     * @param timeout 超时时间
     * @param unit    时间单位
     */
    void set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 设置缓存值并指定过期时间。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     */
    void set(String key, Object value, Duration ttl);

    /**
     * 获取缓存值。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 获取缓存值。
     *
     * @param key           Redis 键
     * @param typeReference 目标泛型类型
     * @param <T>           泛型类型
     * @return 缓存值
     */
    <T> T get(String key, TypeReference<T> typeReference);

    /**
     * key 不存在时设置缓存值。
     *
     * @param key     Redis 键
     * @param value   缓存值
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);

    /**
     * key 不存在时设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     * @return 是否设置成功
     */
    boolean setIfAbsent(String key, Object value, Duration ttl);

    /**
     * key 存在时设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @return 是否设置成功
     */
    boolean setIfExists(String key, Object value);

    /**
     * key 存在时设置缓存值并指定过期时间。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     * @return 是否设置成功
     */
    boolean setIfExists(String key, Object value, Duration ttl);

    /**
     * 原子替换并返回旧值。
     *
     * @param key   Redis 键
     * @param value 新值
     * @param clazz 旧值类型
     * @param <T>   泛型类型
     * @return 旧值
     */
    <T> T getAndSet(String key, Object value, Class<T> clazz);

    /**
     * 原子替换并返回旧值。
     *
     * @param key           Redis 键
     * @param value         新值
     * @param typeReference 旧值类型
     * @param <T>           泛型类型
     * @return 旧值
     */
    <T> T getAndSet(String key, Object value, TypeReference<T> typeReference);

    /**
     * 获取并删除缓存值。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 删除前的值
     */
    <T> T getAndDelete(String key, Class<T> clazz);

    /**
     * 批量获取缓存值。
     *
     * @param keys Redis 键集合
     * @return 键值 Map
     */
    Map<String, Object> entries(Collection<String> keys);

    /**
     * 批量获取缓存值并转换类型。
     *
     * @param keys  Redis 键集合
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 键值 Map
     */
    <T> Map<String, T> entries(Collection<String> keys, Class<T> clazz);

    /**
     * 批量获取缓存值并转换泛型类型。
     *
     * @param keys          Redis 键集合
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 键值 Map
     */
    <T> Map<String, T> entries(Collection<String> keys, TypeReference<T> typeReference);

    /**
     * 获取序列化后的字节大小。
     *
     * @param key Redis 键
     * @return 字节大小
     */
    long size(String key);

    // -------------------------------------------------------------------------
    // 原子数值 / 计数器 / ID
    // -------------------------------------------------------------------------

    /**
     * 获取长整型原子对象。
     *
     * @param key Redis 键
     * @return RAtomicLong
     */
    RAtomicLong getAtomicLong(String key);

    /**
     * 获取浮点型原子对象。
     *
     * @param key Redis 键
     * @return RAtomicDouble
     */
    RAtomicDouble getAtomicDouble(String key);

    /**
     * 整数自增。
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    long increment(String key, long delta);

    /**
     * 整数自减。
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    long decrement(String key, long delta);

    /**
     * 浮点数自增。
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    double incrementDouble(String key, double delta);

    /**
     * 浮点数自减。
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    double decrementDouble(String key, double delta);

    /**
     * 设置整数计数器值。
     *
     * @param key   Redis 键
     * @param value 值
     */
    void setAtomicLong(String key, long value);

    /**
     * 获取整数计数器值。
     *
     * @param key Redis 键
     * @return 当前值
     */
    long getAtomicLongValue(String key);

    /**
     * 重置整数计数器。
     *
     * @param key Redis 键
     */
    void resetAtomicLong(String key);

    /**
     * 获取分布式 ID 生成器。
     *
     * @param key Redis 键
     * @return RIdGenerator
     */
    RIdGenerator getIdGenerator(String key);

    /**
     * 初始化分布式 ID 生成器。
     *
     * @param key            Redis 键
     * @param initialValue   初始值
     * @param allocationSize 每次分配步长
     * @return 是否初始化成功
     */
    boolean idGeneratorInit(String key, long initialValue, long allocationSize);

    /**
     * 获取下一个分布式 ID。
     *
     * @param key Redis 键
     * @return ID
     */
    long nextId(String key);

    // -------------------------------------------------------------------------
    // Hash / Map 操作
    // -------------------------------------------------------------------------

    /**
     * 设置哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     */
    void hPut(String key, String field, Object value);

    /**
     * 批量设置哈希字段值。
     *
     * @param key Redis 键
     * @param map 字段 Map
     */
    void hPutAll(String key, Map<String, ?> map);

    /**
     * 字段不存在时设置哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @return 是否设置成功
     */
    boolean hPutIfAbsent(String key, String field, Object value);

    /**
     * 获取哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值
     */
    <T> T hGet(String key, String field, Class<T> clazz);

    /**
     * 获取哈希字段值。
     *
     * @param key           Redis 键
     * @param field         字段名
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 字段值
     */
    <T> T hGet(String key, String field, TypeReference<T> typeReference);

    /**
     * 批量获取哈希字段值。
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 字段值 Map
     */
    Map<String, Object> hMultiGet(String key, Collection<String> fields);

    /**
     * 删除哈希字段。
     *
     * @param key    Redis 键
     * @param fields 字段名
     * @return 删除数量
     */
    long hDelete(String key, String... fields);

    /**
     * 判断哈希字段是否存在。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 存在返回 true
     */
    boolean hHasKey(String key, String field);

    /**
     * 获取哈希全部字段和值。
     *
     * @param key Redis 键
     * @return 字段值 Map
     */
    Map<String, Object> hEntries(String key);

    /**
     * 获取哈希全部字段和值并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值 Map
     */
    <T> Map<String, T> hEntries(String key, Class<T> clazz);

    /**
     * 获取哈希全部字段和值并转换泛型类型。
     *
     * @param key           Redis 键
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 字段值 Map
     */
    <T> Map<String, T> hEntries(String key, TypeReference<T> typeReference);

    /**
     * 获取哈希字段名集合。
     *
     * @param key Redis 键
     * @return 字段集合
     */
    Set<String> hKeys(String key);

    /**
     * 获取哈希字段值集合。
     *
     * @param key Redis 键
     * @return 字段值集合
     */
    Collection<Object> hValues(String key);

    /**
     * 获取哈希字段数量。
     *
     * @param key Redis 键
     * @return 字段数量
     */
    int hSize(String key);

    /**
     * 哈希字段整数自增。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    long hIncrement(String key, String field, long delta);

    /**
     * 哈希字段浮点数自增。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    double hIncrementDouble(String key, String field, double delta);

    /**
     * 清空哈希。
     *
     * @param key Redis 键
     */
    void hClear(String key);

    /**
     * 设置 MapCache 字段值并指定字段级 TTL。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @param ttl   字段 TTL
     */
    void hcPut(String key, String field, Object value, Duration ttl);

    /**
     * 设置 MapCache 字段值并指定字段级 TTL 与最大空闲时间。
     *
     * @param key      Redis 键
     * @param field    字段名
     * @param value    字段值
     * @param ttl      字段 TTL
     * @param maxIdle  最大空闲时间
     */
    void hcPut(String key, String field, Object value, Duration ttl, Duration maxIdle);

    /**
     * 获取 MapCache 字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值
     */
    <T> T hcGet(String key, String field, Class<T> clazz);

    /**
     * 删除 MapCache 字段。
     *
     * @param key    Redis 键
     * @param fields 字段名
     * @return 删除数量
     */
    long hcDelete(String key, String... fields);

    // -------------------------------------------------------------------------
    // List / Deque 操作
    // -------------------------------------------------------------------------

    /**
     * 左侧压入列表。
     *
     * @param key   Redis 键
     * @param value 元素
     */
    void lLeftPush(String key, Object value);

    /**
     * 右侧压入列表。
     *
     * @param key   Redis 键
     * @param value 元素
     */
    void lRightPush(String key, Object value);

    /**
     * 批量右侧压入列表。
     *
     * @param key    Redis 键
     * @param values 元素集合
     */
    void lRightPushAll(String key, Collection<?> values);

    /**
     * 左侧弹出列表元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    Object lLeftPop(String key);

    /**
     * 左侧弹出列表元素并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素
     */
    <T> T lLeftPop(String key, Class<T> clazz);

    /**
     * 右侧弹出列表元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    Object lRightPop(String key);

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
    <T> T lLeftPop(String key, long timeout, TimeUnit unit, Class<T> clazz) throws InterruptedException;

    /**
     * 获取列表范围。
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素集合
     */
    List<Object> lRange(String key, long start, long end);

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
    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    /**
     * 获取列表长度。
     *
     * @param key Redis 键
     * @return 长度
     */
    long lSize(String key);

    /**
     * 删除列表元素。
     *
     * @param key   Redis 键
     * @param count 删除数量规则
     * @param value 元素
     * @return 删除数量
     */
    long lRemove(String key, long count, Object value);

    /**
     * 获取列表指定索引元素。
     *
     * @param key   Redis 键
     * @param index 索引
     * @return 元素
     */
    Object lIndex(String key, long index);

    /**
     * 获取列表指定索引元素并转换类型。
     *
     * @param key   Redis 键
     * @param index 索引
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素
     */
    <T> T lIndex(String key, long index, Class<T> clazz);

    /**
     * 设置列表指定索引元素。
     *
     * @param key   Redis 键
     * @param index 索引
     * @param value 元素
     */
    void lSet(String key, long index, Object value);

    /**
     * 裁剪列表范围。
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     */
    void lTrim(String key, int start, int end);

    /**
     * 清空列表。
     *
     * @param key Redis 键
     */
    void lClear(String key);

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
    boolean sAdd(String key, Object... values);

    /**
     * 添加集合元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    boolean sAdd(String key, Collection<?> values);

    /**
     * 添加 SetCache 元素并指定元素 TTL。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param ttl   TTL
     * @return 是否添加成功
     */
    boolean scAdd(String key, Object value, Duration ttl);

    /**
     * 判断集合中是否存在元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 存在返回 true
     */
    boolean sIsMember(String key, Object value);

    /**
     * 获取集合所有元素。
     *
     * @param key Redis 键
     * @return 元素集合
     */
    Set<Object> sMembers(String key);

    /**
     * 获取集合所有元素并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素集合
     */
    <T> Set<T> sMembers(String key, Class<T> clazz);

    /**
     * 获取集合大小。
     *
     * @param key Redis 键
     * @return 大小
     */
    long sSize(String key);

    /**
     * 随机弹出集合元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    Object sPop(String key);

    /**
     * 删除集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    boolean sRemove(String key, Object... values);

    /**
     * 随机获取集合元素但不删除。
     *
     * @param key Redis 键
     * @return 元素
     */
    Object sRandomMember(String key);

    /**
     * 随机获取多个集合元素。
     *
     * @param key   Redis 键
     * @param count 数量
     * @return 元素集合
     */
    Set<Object> sRandomMembers(String key, int count);

    /**
     * 获取并集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 并集
     */
    Set<Object> sUnion(String key1, String key2);

    /**
     * 获取交集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 交集
     */
    Set<Object> sIntersect(String key1, String key2);

    /**
     * 获取差集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 差集
     */
    Set<Object> sDifference(String key1, String key2);

    /**
     * 将集合并集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    long sUnionStore(String destKey, String... keys);

    /**
     * 将集合交集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    long sIntersectStore(String destKey, String... keys);

    /**
     * 将集合差集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    long sDifferenceStore(String destKey, String... keys);

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
    boolean zAdd(String key, Object value, double score);

    /**
     * 批量添加有序集合元素。
     *
     * @param key      Redis 键
     * @param scoreMap 元素分数 Map
     * @return 新增数量
     */
    int zAddAll(String key, Map<Object, Double> scoreMap);

    /**
     * 删除有序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    boolean zRemove(String key, Object... values);

    /**
     * 获取元素分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 分数
     */
    Double zScore(String key, Object value);

    /**
     * 获取升序排名，从 0 开始。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    Integer zRank(String key, Object value);

    /**
     * 获取降序排名，从 0 开始。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    Integer zRevRank(String key, Object value);

    /**
     * 获取分数区间元素。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    Set<Object> zRangeByScore(String key, double min, double max);

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
    Collection<Object> zRangeByScore(String key, double min, double max, int offset, int count);

    /**
     * 获取分数区间元素及分数。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    Map<Object, Double> zRangeByScoreWithScores(String key, double min, double max);

    /**
     * 获取降序分数区间元素及分数。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    Map<Object, Double> zRevRangeByScoreWithScores(String key, double min, double max);

    /**
     * 获取排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    Set<Object> zRange(String key, int start, int end);

    /**
     * 获取排名区间元素及分数。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    Map<Object, Double> zRangeWithScores(String key, int start, int end);

    /**
     * 获取降序排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    Set<Object> zRevRange(String key, int start, int end);

    /**
     * 获取降序排名区间元素及分数。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    Map<Object, Double> zRevRangeWithScores(String key, int start, int end);

    /**
     * 增加元素分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param delta 分数增量
     * @return 最新分数
     */
    Double zIncrBy(String key, Object value, double delta);

    /**
     * 获取有序集合元素数量。
     *
     * @param key Redis 键
     * @return 数量
     */
    int zCard(String key);

    /**
     * 获取分数区间元素数量。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 数量
     */
    long zCount(String key, double min, double max);

    /**
     * 删除分数区间元素。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 删除数量
     */
    long zRemoveRangeByScore(String key, double min, double max);

    /**
     * 删除排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 删除数量
     */
    long zRemoveRangeByRank(String key, int start, int end);

    /**
     * 弹出分数最小的元素。
     *
     * @param key Redis 键
     * @return 元素及分数
     */
    ScoredEntry<Object> zPopFirst(String key);

    /**
     * 弹出分数最大的元素。
     *
     * @param key Redis 键
     * @return 元素及分数
     */
    ScoredEntry<Object> zPopLast(String key);

    // -------------------------------------------------------------------------
    // 分布式锁与同步器
    // -------------------------------------------------------------------------

    /**
     * 获取可重入锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    RLock getLock(String lockKey);

    /**
     * 获取公平锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    RLock getFairLock(String lockKey);

    /**
     * 获取自旋锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    RLock getSpinLock(String lockKey);

    /**
     * 获取联锁。
     *
     * @param locks 多个锁
     * @return RLock
     */
    RLock getMultiLock(RLock... locks);

    /**
     * 阻塞加锁。
     *
     * @param lockKey 锁 key
     */
    void lock(String lockKey);

    /**
     * 阻塞加锁并指定自动释放时间。
     *
     * @param lockKey   锁 key
     * @param leaseTime 持有时间
     * @param unit      时间单位
     */
    void lock(String lockKey, long leaseTime, TimeUnit unit);

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
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * 释放锁。
     *
     * @param lockKey 锁 key
     */
    void unlock(String lockKey);

    /**
     * 执行带锁任务。
     *
     * @param lockKey 锁 key
     * @param task    任务
     */
    void executeWithLock(String lockKey, Runnable task);

    /**
     * 执行带锁任务并返回结果。
     *
     * @param lockKey  锁 key
     * @param supplier 任务
     * @param <T>      返回类型
     * @return 任务结果
     */
    <T> T executeWithLock(String lockKey, Supplier<T> supplier);

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
    boolean tryExecuteWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable task);

    /**
     * 判断当前线程是否持有锁。
     *
     * @param lockKey 锁 key
     * @return 当前线程持有返回 true
     */
    boolean isHeldByCurrentThread(String lockKey);

    /**
     * 判断锁是否被任意线程持有。
     *
     * @param lockKey 锁 key
     * @return 已加锁返回 true
     */
    boolean isLocked(String lockKey);

    /**
     * 获取读写锁。
     *
     * @param lockKey 锁 key
     * @return RReadWriteLock
     */
    RReadWriteLock getReadWriteLock(String lockKey);

    /**
     * 获取读锁。
     *
     * @param lockKey 锁 key
     */
    void readLock(String lockKey);

    /**
     * 获取写锁。
     *
     * @param lockKey 锁 key
     */
    void writeLock(String lockKey);

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
    boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

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
    boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * 释放读锁。
     *
     * @param lockKey 锁 key
     */
    void unlockRead(String lockKey);

    /**
     * 释放写锁。
     *
     * @param lockKey 锁 key
     */
    void unlockWrite(String lockKey);

    /**
     * 获取闭锁。
     *
     * @param latchKey 闭锁 key
     * @return RCountDownLatch
     */
    RCountDownLatch getCountDownLatch(String latchKey);

    /**
     * 设置闭锁计数。
     *
     * @param latchKey 闭锁 key
     * @param count    计数
     */
    void setCount(String latchKey, int count);

    /**
     * 闭锁计数减一。
     *
     * @param latchKey 闭锁 key
     */
    void countDown(String latchKey);

    /**
     * 等待闭锁完成。
     *
     * @param latchKey 闭锁 key
     * @throws InterruptedException 线程中断时抛出
     */
    void await(String latchKey) throws InterruptedException;

    /**
     * 等待闭锁完成。
     *
     * @param latchKey 闭锁 key
     * @param timeout  超时时间
     * @param unit     时间单位
     * @return 是否完成
     * @throws InterruptedException 线程中断时抛出
     */
    boolean await(String latchKey, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 获取信号量。
     *
     * @param semaphoreKey 信号量 key
     * @return RSemaphore
     */
    RSemaphore getSemaphore(String semaphoreKey);

    /**
     * 初始化信号量许可数量。
     *
     * @param semaphoreKey 信号量 key
     * @param permits      许可数量
     */
    void trySetPermits(String semaphoreKey, int permits);

    /**
     * 获取一个信号量许可。
     *
     * @param semaphoreKey 信号量 key
     * @throws InterruptedException 线程中断时抛出
     */
    void acquire(String semaphoreKey) throws InterruptedException;

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
    boolean tryAcquire(String semaphoreKey, int permits, long waitTime, TimeUnit unit) throws InterruptedException;

    /**
     * 释放信号量许可。
     *
     * @param semaphoreKey 信号量 key
     */
    void release(String semaphoreKey);

    /**
     * 获取可用许可数量。
     *
     * @param semaphoreKey 信号量 key
     * @return 可用许可数量
     */
    int availablePermits(String semaphoreKey);

    /**
     * 获取可过期信号量。
     *
     * @param semaphoreKey 信号量 key
     * @return RPermitExpirableSemaphore
     */
    RPermitExpirableSemaphore getPermitExpirableSemaphore(String semaphoreKey);

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
    boolean rateLimiterInit(String key, RateType rateType, long rate, long interval, RateIntervalUnit unit);

    /**
     * 更新限流器速率。
     *
     * @param key      限流器 key
     * @param rateType 限流类型
     * @param rate     令牌数
     * @param interval 间隔
     * @param unit     间隔单位
     */
    void rateLimiterSetRate(String key, RateType rateType, long rate, long interval, RateIntervalUnit unit);

    /**
     * 尝试获取一个令牌。
     *
     * @param key 限流器 key
     * @return 是否获取成功
     */
    boolean rateLimiterTryAcquire(String key);

    /**
     * 尝试获取指定数量令牌。
     *
     * @param key     限流器 key
     * @param permits 令牌数量
     * @return 是否获取成功
     */
    boolean rateLimiterTryAcquire(String key, long permits);

    /**
     * 阻塞获取指定数量令牌。
     *
     * @param key     限流器 key
     * @param permits 令牌数量
     */
    void rateLimiterAcquire(String key, long permits);

    /**
     * 尝试等待获取令牌。
     *
     * @param key     限流器 key
     * @param timeout 等待时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    boolean rateLimiterTryAcquire(String key, long timeout, TimeUnit unit);

    /**
     * 获取限流器对象。
     *
     * @param key 限流器 key
     * @return RRateLimiter
     */
    RRateLimiter rateLimiterGet(String key);

    /**
     * 删除限流器。
     *
     * @param key 限流器 key
     * @return 是否删除成功
     */
    boolean rateLimiterDelete(String key);

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
    <T> RBloomFilter<T> getBloomFilter(String key);

    /**
     * 初始化布隆过滤器。
     *
     * @param key                Redis 键
     * @param expectedInsertions 预计插入量
     * @param falseProbability   误判率
     */
    void bloomInit(String key, long expectedInsertions, double falseProbability);

    /**
     * 判断布隆过滤器是否可能包含元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 可能包含返回 true
     */
    boolean bloomContains(String key, Object value);

    /**
     * 添加布隆过滤器元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    boolean bloomAdd(String key, Object value);

    /**
     * 批量添加布隆过滤器元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 新增数量
     */
    long bloomAddAll(String key, Collection<?> values);

    /**
     * 删除布隆过滤器。
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    boolean bloomDelete(String key);

    /**
     * 判断布隆过滤器是否存在。
     *
     * @param key Redis 键
     * @return 存在返回 true
     */
    boolean bloomExists(String key);

    /**
     * 获取预计插入量。
     *
     * @param key Redis 键
     * @return 预计插入量
     */
    long bloomGetExpectedInsertions(String key);

    /**
     * 获取误判率。
     *
     * @param key Redis 键
     * @return 误判率
     */
    double bloomGetFalseProbability(String key);

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
    void bitSet(String key, long index, boolean value);

    /**
     * 获取位图指定位置。
     *
     * @param key   Redis 键
     * @param index 位索引
     * @return 位值
     */
    boolean bitGet(String key, long index);

    /**
     * 获取位图中 true 的数量。
     *
     * @param key Redis 键
     * @return 数量
     */
    long bitCount(String key);

    /**
     * 清空位图。
     *
     * @param key Redis 键
     */
    void bitClear(String key);

    /**
     * 用户指定日期签到。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     */
    void sign(String keyPrefix, Object userId, LocalDate date);

    /**
     * 判断用户指定日期是否签到。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     * @return 已签到返回 true
     */
    boolean isSigned(String keyPrefix, Object userId, LocalDate date);

    /**
     * 获取用户指定年份签到天数。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param year      年份
     * @return 签到天数
     */
    long getSignCount(String keyPrefix, Object userId, int year);

    /**
     * 获取从指定日期向前连续签到天数。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     * @return 连续签到天数
     */
    int getContinuousSignCount(String keyPrefix, Object userId, LocalDate date);

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
    boolean hllAdd(String key, Object value);

    /**
     * 批量添加 HyperLogLog 元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否改变基数估算
     */
    boolean hllAddAll(String key, Collection<?> values);

    /**
     * 获取 HyperLogLog 基数估算。
     *
     * @param key Redis 键
     * @return 基数估算
     */
    long hllCount(String key);

    /**
     * 合并 HyperLogLog。
     *
     * @param destKey 目标 key
     * @param keys    源 key
     * @return 合并后基数估算
     */
    long hllMerge(String destKey, String... keys);

    /**
     * 按日期记录 UV。
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param userFlag  用户唯一标识
     * @param date      日期
     * @return 是否改变基数估算
     */
    boolean uvRecord(String keyPrefix, String bizKey, Object userFlag, LocalDate date);

    /**
     * 获取指定日期 UV。
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param date      日期
     * @return UV 数量
     */
    long uvCount(String keyPrefix, String bizKey, LocalDate date);

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
    long geoAdd(String key, double longitude, double latitude, Object member);

    /**
     * 批量添加地理位置。
     *
     * @param key     Redis 键
     * @param entries 坐标集合
     * @return 添加数量
     */
    long geoAdd(String key, GeoEntry... entries);

    /**
     * 仅成员不存在时添加地理位置。
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param member    成员
     * @return 是否添加成功
     */
    boolean geoTryAdd(String key, double longitude, double latitude, Object member);

    /**
     * 计算两个成员距离。
     *
     * @param key     Redis 键
     * @param member1 成员 1
     * @param member2 成员 2
     * @param unit    单位
     * @return 距离
     */
    Double geoDistance(String key, Object member1, Object member2, GeoUnit unit);

    /**
     * 查询成员 GeoHash。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return GeoHash Map
     */
    Map<Object, String> geoHash(String key, Object... members);

    /**
     * 查询成员坐标。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return 坐标 Map
     */
    Map<Object, GeoPosition> geoPosition(String key, Object... members);

    /**
     * 根据条件搜索地理位置成员。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员列表
     */
    List<Object> geoSearch(String key, GeoSearchArgs args);

    /**
     * 根据条件搜索地理位置成员和距离。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员距离 Map
     */
    Map<Object, Double> geoSearchWithDistance(String key, GeoSearchArgs args);

    /**
     * 根据条件搜索地理位置成员和坐标。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员坐标 Map
     */
    Map<Object, GeoPosition> geoSearchWithPosition(String key, GeoSearchArgs args);

    /**
     * 删除地理位置成员。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return 删除数量
     */
    long geoRemove(String key, Object... members);

    // -------------------------------------------------------------------------
    // Queue / Deque / DelayedQueue / MQ
    // -------------------------------------------------------------------------

    /**
     * 入普通队列。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param <T>      元素类型
     * @return 是否入队成功
     */
    <T> boolean enqueue(String queueKey, T value);

    /**
     * 出普通队列。
     *
     * @param queueKey 队列 key
     * @param <T>      元素类型
     * @return 元素
     */
    <T> T dequeue(String queueKey);

    /**
     * 阻塞入队。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param <T>      元素类型
     * @throws InterruptedException 线程中断时抛出
     */
    <T> void enqueueBlocking(String queueKey, T value) throws InterruptedException;

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
    <T> boolean enqueueBlocking(String queueKey, T value, long timeout, TimeUnit unit) throws InterruptedException;

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
    <T> T dequeueBlocking(String queueKey, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 获取队列长度。
     *
     * @param queueKey 队列 key
     * @return 长度
     */
    long queueSize(String queueKey);

    /**
     * 添加延迟队列任务。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param delay    延迟时间
     * @param unit     时间单位
     * @param <T>      元素类型
     */
    <T> void enqueueDelayed(String queueKey, T value, long delay, TimeUnit unit);

    /**
     * 获取延迟队列对象。
     *
     * @param queueKey 队列 key
     * @param <T>      元素类型
     * @return RDelayedQueue
     */
    <T> RDelayedQueue<T> getDelayedQueue(String queueKey);

    /**
     * 清空队列。
     *
     * @param queueKey 队列 key
     */
    void clearQueue(String queueKey);

    /**
     * 判断队列是否为空。
     *
     * @param queueKey 队列 key
     * @return 为空返回 true
     */
    boolean isQueueEmpty(String queueKey);

    /**
     * 删除队列元素。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @return 是否删除成功
     */
    boolean removeFromQueue(String queueKey, Object value);

    /**
     * 获取环形缓冲队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RRingBuffer
     */
    <T> RRingBuffer<T> getRingBuffer(String key);

    /**
     * 获取优先级队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityQueue
     */
    <T> RPriorityQueue<T> getPriorityQueue(String key);

    /**
     * 获取阻塞双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RBlockingDeque
     */
    <T> RBlockingDeque<T> getBlockingDeque(String key);

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
    long publish(String channel, Object message);

    /**
     * 订阅频道。
     *
     * @param channel         频道
     * @param messageConsumer 消费回调
     * @return 监听器 ID
     */
    int subscribe(String channel, Consumer<Object> messageConsumer);

    /**
     * 取消订阅频道。
     *
     * @param channel    频道
     * @param listenerId 监听器 ID
     */
    void unsubscribe(String channel, int listenerId);

    /**
     * 取消订阅频道全部监听器。
     *
     * @param channel 频道
     */
    void unsubscribe(String channel);

    /**
     * 获取模式主题。
     *
     * @param pattern 频道通配符
     * @return RPatternTopic
     */
    RPatternTopic getPatternTopic(String pattern);

    /**
     * 获取可靠主题。
     *
     * @param topic 主题名
     * @return RReliableTopic
     */
    RReliableTopic getReliableTopic(String topic);

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
    StreamMessageId streamAdd(String streamKey, Map<Object, Object> entries);

    /**
     * 添加 Stream 消息。
     *
     * @param streamKey Stream key
     * @param args      添加参数
     * @param <K>       字段类型
     * @param <V>       值类型
     * @return 消息 ID
     */
    <K, V> StreamMessageId streamAdd(String streamKey, StreamAddArgs<K, V> args);

    /**
     * 读取 Stream 消息。
     *
     * @param streamKey Stream key
     * @param args      读取参数
     * @return 消息 Map
     */
    Map<StreamMessageId, Map<Object, Object>> streamRead(String streamKey, StreamReadArgs args);

    /**
     * 创建消费组。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param id        起始消息 ID
     */
    void streamCreateGroup(String streamKey, String groupName, StreamMessageId id);

    /**
     * 读取消费组消息。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @param args         读取参数
     * @return 消息 Map
     */
    Map<StreamMessageId, Map<Object, Object>> streamReadGroup(String streamKey, String groupName, String consumerName, StreamReadGroupArgs args);

    /**
     * 确认 Stream 消息。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param ids       消息 ID
     * @return 确认数量
     */
    long streamAck(String streamKey, String groupName, StreamMessageId... ids);

    /**
     * 删除 Stream 消息。
     *
     * @param streamKey Stream key
     * @param ids       消息 ID
     * @return 删除数量
     */
    long streamRemove(String streamKey, StreamMessageId... ids);

    /**
     * 获取 Stream 长度。
     *
     * @param streamKey Stream key
     * @return 长度
     */
    long streamSize(String streamKey);

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
    void sessionSet(String keyPrefix, String token, Object session, Duration ttl);

    /**
     * 获取会话。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @param clazz     目标类型
     * @param <T>       泛型类型
     * @return 会话对象
     */
    <T> T sessionGet(String keyPrefix, String token, Class<T> clazz);

    /**
     * 获取会话。
     *
     * @param keyPrefix     会话 key 前缀
     * @param token         Token
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 会话对象
     */
    <T> T sessionGet(String keyPrefix, String token, TypeReference<T> typeReference);

    /**
     * 刷新会话过期时间。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @param ttl       过期时间
     * @return 是否刷新成功
     */
    boolean sessionRefresh(String keyPrefix, String token, Duration ttl);

    /**
     * 删除会话。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 是否删除成功
     */
    boolean sessionDelete(String keyPrefix, String token);

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
    <T> T eval(String script, RScript.Mode mode, RScript.ReturnType returnType, List<Object> keys, Object... values);

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
    <T> T eval(String script, Class<T> returnType, List<Object> keys, Object... args);

    /**
     * 执行 Lua 脚本但不关心结果。
     *
     * @param script Lua 脚本
     * @param keys   KEYS
     * @param args   ARGV
     */
    void evalNoResult(String script, List<Object> keys, Object... args);

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
    <T> T evalBySha(String sha1, Class<T> returnType, List<Object> keys, Object... values);

    /**
     * 加载 Lua 脚本。
     *
     * @param script Lua 脚本
     * @return SHA1
     */
    String loadScript(String script);

    /**
     * 创建批处理对象。
     *
     * @return RBatch
     */
    RBatch createBatch();

    /**
     * 创建事务对象。
     *
     * @return RTransaction
     */
    RTransaction createTransaction();

}
