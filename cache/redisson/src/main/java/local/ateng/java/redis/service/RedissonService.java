package local.ateng.java.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.redisson.api.*;
import org.redisson.api.geo.GeoSearchArgs;
import org.redisson.api.queue.*;
import org.redisson.api.queue.event.QueueEventListener;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.api.stream.StreamTrimArgs;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.codec.JsonCodec;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
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
    // 编码生成
    // -------------------------------------------------------------------------

    /**
     * 生成企业业务编码。
     * <p>
     * 示例：
     * USER000001
     *
     * @param prefix 编码前缀
     * @param length 序号长度
     * @return 编码
     */
    String generateCode(String prefix, int length);

    /**
     * 批量获取编码。
     *
     * 适用于批量导入、批量新增场景。
     *
     * @param prefix 编码前缀
     * @param length 序号长度
     * @param size   获取数量
     * @return 编码集合
     */
    List<String> generateCodes(String prefix, int length, int size);

    /**
     * 生成带日期企业业务编码。
     * <p>
     * 示例：
     * USER20260729000001
     *
     * @param prefix 编码前缀
     * @param length 序号长度
     * @return 编码
     */
    String generateDateCode(String prefix, int length);

    /**
     * 根据指定业务 Key 生成企业业务编码。
     * <p>
     * 用于同一个前缀下不同业务隔离序号。
     * <p>
     * 示例：
     * PLAN000001
     *
     * @param key    Redis序号Key
     * @param prefix 编码前缀
     * @param length 序号长度
     * @return 编码
     */
    String generateCode(String key, String prefix, int length);

    /**
     * 重置编码序号。
     *
     * @param key Redis序号Key
     */
    void resetCodeSequence(String key);

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
     * @param key     Redis 键
     * @param field   字段名
     * @param value   字段值
     * @param ttl     字段 TTL
     * @param maxIdle 最大空闲时间
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

    // -------------------------------------------------------------------------
    // LocalCachedMap / 本地缓存 Map
    // -------------------------------------------------------------------------

    /**
     * 获取本地缓存 Map。
     * 注意：同一个 RedissonClient 内，相同 name 建议复用相同 LocalCachedMapOptions。
     *
     * @param key     Redis 键
     * @param options 本地缓存配置
     * @param <K>     字段类型
     * @param <V>     值类型
     * @return RLocalCachedMap
     */
    <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String key, LocalCachedMapOptions<K, V> options);

    /**
     * 设置本地缓存 Map 字段值。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param value   字段值
     * @param options 本地缓存配置
     */
    void lcPut(String key, Object field, Object value, LocalCachedMapOptions<Object, Object> options);

    /**
     * 字段不存在时设置本地缓存 Map 字段值。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param value   字段值
     * @param options 本地缓存配置
     * @return 是否设置成功
     */
    boolean lcPutIfAbsent(String key, Object field, Object value, LocalCachedMapOptions<Object, Object> options);

    /**
     * 获取本地缓存 Map 字段值。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param clazz   目标类型
     * @param options 本地缓存配置
     * @param <T>     泛型类型
     * @return 字段值
     */
    <T> T lcGet(String key, Object field, Class<T> clazz, LocalCachedMapOptions<Object, Object> options);

    /**
     * 获取本地缓存 Map 字段值。
     *
     * @param key           Redis 键
     * @param field         字段
     * @param typeReference 目标类型
     * @param options       本地缓存配置
     * @param <T>           泛型类型
     * @return 字段值
     */
    <T> T lcGet(String key, Object field, TypeReference<T> typeReference, LocalCachedMapOptions<Object, Object> options);

    /**
     * 删除本地缓存 Map 字段。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param options 本地缓存配置
     * @return 删除前的值
     */
    Object lcRemove(String key, Object field, LocalCachedMapOptions<Object, Object> options);

    /**
     * 判断本地缓存 Map 字段是否存在。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param options 本地缓存配置
     * @return 存在返回 true
     */
    boolean lcContainsKey(String key, Object field, LocalCachedMapOptions<Object, Object> options);

    /**
     * 获取本地缓存 Map 大小。
     *
     * @param key     Redis 键
     * @param options 本地缓存配置
     * @return 大小
     */
    int lcSize(String key, LocalCachedMapOptions<Object, Object> options);

    /**
     * 清空本地缓存 Map。
     *
     * @param key     Redis 键
     * @param options 本地缓存配置
     */
    void lcClear(String key, LocalCachedMapOptions<Object, Object> options);

    /**
     * 仅清空当前 JVM 内的本地缓存，不清空 Redis 远端数据。
     *
     * @param key     Redis 键
     * @param options 本地缓存配置
     */
    void lcClearLocalCache(String key, LocalCachedMapOptions<Object, Object> options);

    // -------------------------------------------------------------------------
    // JsonBucket / RedisJSON
    // -------------------------------------------------------------------------

    /**
     * 获取 JSON Bucket。
     *
     * @param key   Redis 键
     * @param codec JSON 编解码器
     * @param <T>   值类型
     * @return RJsonBucket
     */
    <T> RJsonBucket<T> getJsonBucket(String key, JsonCodec codec);

    /**
     * 设置 JSON 文档。
     *
     * @param key   Redis 键
     * @param value JSON 对象
     * @param codec JSON 编解码器
     * @param <T>   值类型
     */
    <T> void jsonSet(String key, T value, JsonCodec codec);

    /**
     * 设置 JSON 文档并指定过期时间。
     *
     * @param key   Redis 键
     * @param value JSON 对象
     * @param ttl   过期时间
     * @param codec JSON 编解码器
     * @param <T>   值类型
     */
    <T> void jsonSet(String key, T value, Duration ttl, JsonCodec codec);

    /**
     * 获取完整 JSON 文档。
     *
     * @param key   Redis 键
     * @param codec JSON 编解码器
     * @param <T>   值类型
     * @return JSON 对象
     */
    <T> T jsonGet(String key, JsonCodec codec);

    /**
     * 根据 JSONPath 获取 JSON 局部内容。
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param codec JSON 编解码器
     * @param <T>   返回类型
     * @return JSONPath 对应的值
     */
    <T> T jsonGet(String key, String path, JsonCodec codec);

    /**
     * 根据 JSONPath 设置 JSON 局部内容。
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     */
    <T> void jsonSet(String key, String path, Object value, JsonCodec codec);

    /**
     * JSONPath 不存在时设置局部内容。
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     * @return 是否设置成功
     */
    <T> boolean jsonSetIfAbsent(String key, String path, Object value, JsonCodec codec);

    /**
     * JSONPath 存在时设置局部内容。
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     * @return 是否设置成功
     */
    <T> boolean jsonSetIfExists(String key, String path, Object value, JsonCodec codec);

    /**
     * 删除 JSONPath 对应内容。
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     * @return 删除数量
     */
    <T> long jsonDelete(String key, String path, JsonCodec codec);

    /**
     * 向 JSON 数组追加元素。
     *
     * @param key    Redis 键
     * @param path   JSONPath
     * @param codec  JSON 编解码器
     * @param values 追加值
     * @param <T>    JSON 文档类型
     * @return 追加后的数组长度
     */
    <T> long jsonArrayAppend(String key, String path, JsonCodec codec, Object... values);

    /**
     * 获取 JSON 对象字段名。
     *
     * @param key   Redis 键
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     * @return 字段名集合
     */
    <T> List<String> jsonKeys(String key, JsonCodec codec);

    /**
     * 清空 JSON 文档。
     *
     * @param key   Redis 键
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     */
    <T> void jsonClear(String key, JsonCodec codec);

    // -------------------------------------------------------------------------
    // BinaryStream / 二进制流
    // -------------------------------------------------------------------------

    /**
     * 获取二进制流对象。
     *
     * @param key Redis 键
     * @return RBinaryStream
     */
    RBinaryStream getBinaryStream(String key);

    /**
     * 获取二进制输入流。
     *
     * @param key Redis 键
     * @return InputStream
     */
    InputStream binaryInputStream(String key);

    /**
     * 获取二进制输出流。
     *
     * @param key Redis 键
     * @return OutputStream
     */
    OutputStream binaryOutputStream(String key);

    /**
     * 写入二进制数据。
     *
     * @param key  Redis 键
     * @param data 字节数组
     */
    void binaryWrite(String key, byte[] data);

    /**
     * 读取全部二进制数据。
     *
     * @param key Redis 键
     * @return 字节数组
     */
    byte[] binaryReadAll(String key);

    /**
     * 获取二进制数据大小。
     *
     * @param key Redis 键
     * @return 字节大小
     */
    long binarySize(String key);

    /**
     * 删除二进制数据。
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    boolean binaryDelete(String key);

    // -------------------------------------------------------------------------
    // Multimap / 一键多值
    // -------------------------------------------------------------------------

    /**
     * 获取 SetMultimap。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RSetMultimap
     */
    <K, V> RSetMultimap<K, V> getSetMultimap(String key);

    /**
     * 获取 ListMultimap。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RListMultimap
     */
    <K, V> RListMultimap<K, V> getListMultimap(String key);

    /**
     * 向 SetMultimap 添加值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否新增
     */
    boolean smmPut(String key, Object mapKey, Object mapValue);

    /**
     * 获取 SetMultimap 指定字段的值集合。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 值集合
     */
    Set<Object> smmGet(String key, Object mapKey);

    /**
     * 删除 SetMultimap 指定字段的指定值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否删除成功
     */
    boolean smmRemove(String key, Object mapKey, Object mapValue);

    /**
     * 删除 SetMultimap 指定字段的全部值。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 删除的值集合
     */
    Set<Object> smmRemoveAll(String key, Object mapKey);

    /**
     * 判断 SetMultimap 是否包含指定字段。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 存在返回 true
     */
    boolean smmContainsKey(String key, Object mapKey);

    /**
     * 判断 SetMultimap 是否包含指定字段和值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 存在返回 true
     */
    boolean smmContainsEntry(String key, Object mapKey, Object mapValue);

    /**
     * 获取 SetMultimap 总值数量。
     *
     * @param key Redis 键
     * @return 总值数量
     */
    int smmSize(String key);

    /**
     * 清空 SetMultimap。
     *
     * @param key Redis 键
     */
    void smmClear(String key);

    /**
     * 向 ListMultimap 添加值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否新增
     */
    boolean lmmPut(String key, Object mapKey, Object mapValue);

    /**
     * 获取 ListMultimap 指定字段的值列表。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 值列表
     */
    List<Object> lmmGet(String key, Object mapKey);

    /**
     * 删除 ListMultimap 指定字段的指定值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否删除成功
     */
    boolean lmmRemove(String key, Object mapKey, Object mapValue);

    /**
     * 删除 ListMultimap 指定字段的全部值。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 删除的值列表
     */
    List<Object> lmmRemoveAll(String key, Object mapKey);

    /**
     * 判断 ListMultimap 是否包含指定字段。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 存在返回 true
     */
    boolean lmmContainsKey(String key, Object mapKey);

    /**
     * 判断 ListMultimap 是否包含指定字段和值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 存在返回 true
     */
    boolean lmmContainsEntry(String key, Object mapKey, Object mapValue);

    /**
     * 获取 ListMultimap 总值数量。
     *
     * @param key Redis 键
     * @return 总值数量
     */
    int lmmSize(String key);

    /**
     * 清空 ListMultimap。
     *
     * @param key Redis 键
     */
    void lmmClear(String key);

    // -------------------------------------------------------------------------
    // SortedSet / LexSortedSet
    // -------------------------------------------------------------------------

    /**
     * 获取自然排序集合。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RSortedSet
     */
    <T> RSortedSet<T> getSortedSet(String key);

    /**
     * 获取字典序排序集合。
     *
     * @param key Redis 键
     * @return RLexSortedSet
     */
    RLexSortedSet getLexSortedSet(String key);

    /**
     * 添加自然排序集合元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    boolean sortedSetAdd(String key, Object value);

    /**
     * 批量添加自然排序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    boolean sortedSetAddAll(String key, Collection<?> values);

    /**
     * 获取自然排序集合全部元素。
     *
     * @param key Redis 键
     * @return 元素集合
     */
    Collection<Object> sortedSetReadAll(String key);

    /**
     * 删除自然排序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    boolean sortedSetRemove(String key, Object... values);

    /**
     * 获取自然排序集合大小。
     *
     * @param key Redis 键
     * @return 大小
     */
    int sortedSetSize(String key);

    /**
     * 清空自然排序集合。
     *
     * @param key Redis 键
     */
    void sortedSetClear(String key);

    /**
     * 添加字典序排序集合元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    boolean lexAdd(String key, String value);

    /**
     * 批量添加字典序排序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    boolean lexAddAll(String key, Collection<String> values);

    /**
     * 获取字典序排序集合全部元素。
     *
     * @param key Redis 键
     * @return 元素集合
     */
    Collection<String> lexReadAll(String key);

    /**
     * 获取大于等于指定元素的字典序集合。
     *
     * @param key  Redis 键
     * @param from 开始元素
     * @return 元素集合
     */
    Collection<String> lexRangeTail(String key, String from);

    /**
     * 获取小于等于指定元素的字典序集合。
     *
     * @param key Redis 键
     * @param to  结束元素
     * @return 元素集合
     */
    Collection<String> lexRangeHead(String key, String to);

    /**
     * 删除字典序排序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    boolean lexRemove(String key, String... values);

    /**
     * 获取字典序排序集合大小。
     *
     * @param key Redis 键
     * @return 大小
     */
    int lexSize(String key);

    /**
     * 清空字典序排序集合。
     *
     * @param key Redis 键
     */
    void lexClear(String key);

    // -------------------------------------------------------------------------
    // LongAdder / DoubleAdder
    // -------------------------------------------------------------------------

    /**
     * 获取分布式 LongAdder。
     *
     * @param key Redis 键
     * @return RLongAdder
     */
    RLongAdder getLongAdder(String key);

    /**
     * 获取分布式 DoubleAdder。
     *
     * @param key Redis 键
     * @return RDoubleAdder
     */
    RDoubleAdder getDoubleAdder(String key);

    /**
     * LongAdder 增加。
     *
     * @param key   Redis 键
     * @param delta 增量
     */
    void longAdderAdd(String key, long delta);

    /**
     * LongAdder 求和。
     *
     * @param key Redis 键
     * @return 当前总和
     */
    long longAdderSum(String key);

    /**
     * LongAdder 重置。
     *
     * @param key Redis 键
     */
    void longAdderReset(String key);

    /**
     * LongAdder 求和后重置。
     *
     * @param key Redis 键
     * @return 重置前总和
     */
    long longAdderSumThenReset(String key);

    /**
     * DoubleAdder 增加。
     *
     * @param key   Redis 键
     * @param delta 增量
     */
    void doubleAdderAdd(String key, double delta);

    /**
     * DoubleAdder 求和。
     *
     * @param key Redis 键
     * @return 当前总和
     */
    double doubleAdderSum(String key);

    /**
     * DoubleAdder 重置。
     *
     * @param key Redis 键
     */
    void doubleAdderReset(String key);

    /**
     * DoubleAdder 求和后重置。
     *
     * @param key Redis 键
     * @return 重置前总和
     */
    double doubleAdderSumThenReset(String key);

    // -------------------------------------------------------------------------
    // Bounded / Priority 队列增强
    // -------------------------------------------------------------------------

    /**
     * 获取有界阻塞队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RBoundedBlockingQueue
     */
    <T> RBoundedBlockingQueue<T> getBoundedBlockingQueue(String key);

    /**
     * 初始化有界阻塞队列容量。
     *
     * @param key      Redis 键
     * @param capacity 容量
     * @return 是否初始化成功
     */
    boolean boundedQueueTrySetCapacity(String key, int capacity);

    /**
     * 有界阻塞队列入队。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param <T>   元素类型
     * @return 是否入队成功
     */
    <T> boolean boundedQueueOffer(String key, T value);

    /**
     * 有界阻塞队列超时入队。
     *
     * @param key     Redis 键
     * @param value   元素
     * @param timeout 等待时间
     * @param unit    时间单位
     * @param <T>     元素类型
     * @return 是否入队成功
     * @throws InterruptedException 线程中断时抛出
     */
    <T> boolean boundedQueueOffer(String key, T value, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 有界阻塞队列出队。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return 元素
     */
    <T> T boundedQueuePoll(String key);

    /**
     * 有界阻塞队列超时出队。
     *
     * @param key     Redis 键
     * @param timeout 等待时间
     * @param unit    时间单位
     * @param <T>     元素类型
     * @return 元素
     * @throws InterruptedException 线程中断时抛出
     */
    <T> T boundedQueuePoll(String key, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 获取有界阻塞队列大小。
     *
     * @param key Redis 键
     * @return 队列大小
     */
    int boundedQueueSize(String key);

    /**
     * 获取有界阻塞队列剩余容量。
     *
     * @param key Redis 键
     * @return 剩余容量
     */
    int boundedQueueRemainingCapacity(String key);

    /**
     * 获取优先级双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityDeque
     */
    <T> RPriorityDeque<T> getPriorityDeque(String key);

    /**
     * 获取优先级阻塞队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityBlockingQueue
     */
    <T> RPriorityBlockingQueue<T> getPriorityBlockingQueue(String key);

    /**
     * 获取优先级阻塞双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityBlockingDeque
     */
    <T> RPriorityBlockingDeque<T> getPriorityBlockingDeque(String key);

    /**
     * 优先级队列入队。
     *
     * @param key   Redis 键
     * @param value 元素，建议实现 Comparable
     * @param <T>   元素类型
     * @return 是否入队成功
     */
    <T> boolean priorityQueueOffer(String key, T value);

    /**
     * 优先级队列出队。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return 元素
     */
    <T> T priorityQueuePoll(String key);

    /**
     * 优先级双端队列从头部入队。
     *
     * @param key   Redis 键
     * @param value 元素，建议实现 Comparable
     * @param <T>   元素类型
     * @return 是否入队成功
     */
    <T> boolean priorityDequeOfferFirst(String key, T value);

    /**
     * 优先级双端队列从尾部入队。
     *
     * @param key   Redis 键
     * @param value 元素，建议实现 Comparable
     * @param <T>   元素类型
     * @return 是否入队成功
     */
    <T> boolean priorityDequeOfferLast(String key, T value);

    /**
     * 优先级双端队列从头部出队。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return 元素
     */
    <T> T priorityDequePollFirst(String key);

    /**
     * 优先级双端队列从尾部出队。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return 元素
     */
    <T> T priorityDequePollLast(String key);

    // -------------------------------------------------------------------------
    // ReliableQueue / 可靠队列
    // -------------------------------------------------------------------------

    /**
     * 设置可靠队列配置。
     *
     * @param key    队列 key
     * @param config 队列配置
     */
    void reliableQueueSetConfig(String key, QueueConfig config);

    /**
     * 队列配置不存在时设置可靠队列配置。
     *
     * @param key    队列 key
     * @param config 队列配置
     * @return 是否设置成功
     */
    boolean reliableQueueSetConfigIfAbsent(String key, QueueConfig config);

    /**
     * 可靠队列添加消息。
     *
     * @param key  队列 key
     * @param args 添加参数
     * @param <T>  消息类型
     * @return 添加后的消息
     */
    <T> Message<T> reliableQueueAdd(String key, QueueAddArgs<T> args);

    /**
     * 可靠队列批量添加消息。
     *
     * @param key  队列 key
     * @param args 添加参数
     * @param <T>  消息类型
     * @return 添加后的消息集合
     */
    <T> List<Message<T>> reliableQueueAddMany(String key, QueueAddArgs<T> args);

    /**
     * 可靠队列拉取一条消息。
     *
     * @param key 队列 key
     * @param <T> 消息类型
     * @return 消息
     */
    <T> Message<T> reliableQueuePoll(String key);

    /**
     * 可靠队列按参数拉取一条消息。
     *
     * @param key  队列 key
     * @param args 拉取参数
     * @param <T>  消息类型
     * @return 消息
     */
    <T> Message<T> reliableQueuePoll(String key, QueuePollArgs args);

    /**
     * 可靠队列批量拉取消息。
     *
     * @param key  队列 key
     * @param args 拉取参数
     * @param <T>  消息类型
     * @return 消息集合
     */
    <T> List<Message<T>> reliableQueuePollMany(String key, QueuePollArgs args);

    /**
     * 确认可靠队列消息处理成功。
     *
     * @param key  队列 key
     * @param args ACK 参数
     */
    void reliableQueueAck(String key, QueueAckArgs args);

    /**
     * 标记可靠队列消息处理失败。
     *
     * @param key  队列 key
     * @param args NACK 参数
     */
    void reliableQueueNack(String key, QueueNegativeAckArgs args);

    /**
     * 根据消息 ID 获取可靠队列消息。
     *
     * @param key 队列 key
     * @param id  消息 ID
     * @param <T> 消息类型
     * @return 消息
     */
    <T> Message<T> reliableQueueGet(String key, String id);

    /**
     * 根据消息 ID 批量获取可靠队列消息。
     *
     * @param key 队列 key
     * @param ids 消息 ID
     * @param <T> 消息类型
     * @return 消息集合
     */
    <T> List<Message<T>> reliableQueueGetAll(String key, String... ids);

    /**
     * 获取可靠队列所有可拉取消息。
     *
     * @param key 队列 key
     * @param <T> 消息类型
     * @return 消息集合
     */
    <T> List<Message<T>> reliableQueueListAll(String key);

    /**
     * 判断可靠队列是否包含指定消息 ID。
     *
     * @param key 队列 key
     * @param id  消息 ID
     * @return 包含返回 true
     */
    boolean reliableQueueContains(String key, String id);

    /**
     * 判断可靠队列包含的消息 ID 数量。
     *
     * @param key 队列 key
     * @param ids 消息 ID
     * @return 匹配数量
     */
    int reliableQueueContainsMany(String key, String... ids);

    /**
     * 删除可靠队列消息。
     *
     * @param key  队列 key
     * @param args 删除参数
     * @return 是否删除成功
     */
    boolean reliableQueueRemove(String key, QueueRemoveArgs args);

    /**
     * 批量删除可靠队列消息。
     *
     * @param key  队列 key
     * @param args 删除参数
     * @return 删除数量
     */
    int reliableQueueRemoveMany(String key, QueueRemoveArgs args);

    /**
     * 移动可靠队列消息。
     *
     * @param key  队列 key
     * @param args 移动参数
     * @return 移动数量
     */
    int reliableQueueMove(String key, QueueMoveArgs args);

    /**
     * 获取可靠队列消息数量。
     *
     * @param key 队列 key
     * @return 消息数量
     */
    int reliableQueueSize(String key);

    /**
     * 获取可靠队列延迟消息数量。
     *
     * @param key 队列 key
     * @return 延迟消息数量
     */
    int reliableQueueDelayedSize(String key);

    /**
     * 获取可靠队列未确认消息数量。
     *
     * @param key 队列 key
     * @return 未确认消息数量
     */
    int reliableQueueUnacknowledgedSize(String key);

    /**
     * 清空可靠队列全部状态消息。
     *
     * @param key 队列 key
     * @return 是否清空成功
     */
    boolean reliableQueueClear(String key);

    /**
     * 获取将当前队列作为死信队列的源队列名称。
     *
     * @param key 队列 key
     * @return 源队列名称集合
     */
    Set<String> reliableQueueDeadLetterSources(String key);

    /**
     * 添加可靠队列事件监听器。
     *
     * @param key      队列 key
     * @param listener 监听器
     * @return 监听器 ID
     */
    String reliableQueueAddListener(String key, QueueEventListener listener);

    /**
     * 移除可靠队列事件监听器。
     *
     * @param key        队列 key
     * @param listenerId 监听器 ID
     */
    void reliableQueueRemoveListener(String key, String listenerId);

    /**
     * 启用可靠队列指定操作。
     *
     * @param key       队列 key
     * @param operation 队列操作
     */
    void reliableQueueEnableOperation(String key, QueueOperation operation);

    /**
     * 禁用可靠队列指定操作。
     *
     * @param key       队列 key
     * @param operation 队列操作
     */
    void reliableQueueDisableOperation(String key, QueueOperation operation);

    // -------------------------------------------------------------------------
    // Stream / 高级消费治理
    // -------------------------------------------------------------------------

    /**
     * 获取 Stream 详细信息。
     *
     * @param streamKey Stream key
     * @return Stream 信息
     */
    StreamInfo<Object, Object> streamInfo(String streamKey);

    /**
     * 获取 Stream 消费组列表。
     *
     * @param streamKey Stream key
     * @return 消费组列表
     */
    List<StreamGroup> streamListGroups(String streamKey);

    /**
     * 获取 Stream 指定消费组的消费者列表。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 消费者列表
     */
    List<StreamConsumer> streamListConsumers(String streamKey, String groupName);

    /**
     * 创建 Stream 消费者。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     */
    void streamCreateConsumer(String streamKey, String groupName, String consumerName);

    /**
     * 删除 Stream 消费者。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @return 该消费者名下的待处理消息数量
     */
    long streamRemoveConsumer(String streamKey, String groupName, String consumerName);

    /**
     * 删除 Stream 消费组。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     */
    void streamRemoveGroup(String streamKey, String groupName);

    /**
     * 更新 Stream 消费组读取起始 ID。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param id        消息 ID
     */
    void streamUpdateGroupMessageId(String streamKey, String groupName, StreamMessageId id);

    /**
     * 获取 Stream 消费组待处理消息概要。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 待处理概要
     */
    PendingResult streamPendingInfo(String streamKey, String groupName);

    /**
     * 获取 Stream 消费组待处理消息列表。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 待处理消息列表
     */
    List<PendingEntry> streamListPending(String streamKey, String groupName, StreamMessageId startId, StreamMessageId endId, int count);

    /**
     * 获取 Stream 指定消费者的待处理消息列表。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @param startId      开始 ID
     * @param endId        结束 ID
     * @param count        数量
     * @return 待处理消息列表
     */
    List<PendingEntry> streamListPending(String streamKey, String groupName, String consumerName, StreamMessageId startId, StreamMessageId endId, int count);

    /**
     * 获取 Stream 指定消费者满足最小空闲时间的待处理消息列表。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @param startId      开始 ID
     * @param endId        结束 ID
     * @param idleTime     最小空闲时间
     * @param unit         时间单位
     * @param count        数量
     * @return 待处理消息列表
     */
    List<PendingEntry> streamListPending(String streamKey, String groupName, String consumerName,
                                         StreamMessageId startId, StreamMessageId endId,
                                         long idleTime, TimeUnit unit, int count);

    /**
     * 按 ID 范围读取 Stream 消息。
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @return 消息 Map
     */
    Map<StreamMessageId, Map<Object, Object>> streamRange(String streamKey, StreamMessageId startId, StreamMessageId endId);

    /**
     * 按 ID 范围读取 Stream 消息并限制数量。
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 消息 Map
     */
    Map<StreamMessageId, Map<Object, Object>> streamRange(String streamKey, StreamMessageId startId, StreamMessageId endId, int count);

    /**
     * 按 ID 范围倒序读取 Stream 消息。
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @return 消息 Map
     */
    Map<StreamMessageId, Map<Object, Object>> streamRangeReversed(String streamKey, StreamMessageId startId, StreamMessageId endId);

    /**
     * 按 ID 范围倒序读取 Stream 消息并限制数量。
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 消息 Map
     */
    Map<StreamMessageId, Map<Object, Object>> streamRangeReversed(String streamKey, StreamMessageId startId, StreamMessageId endId, int count);

    /**
     * 转移待处理 Stream 消息所有权。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 新消费者
     * @param idleTime     最小空闲时间
     * @param unit         时间单位
     * @param ids          消息 ID
     * @return 转移后的消息 Map
     */
    Map<StreamMessageId, Map<Object, Object>> streamClaim(String streamKey, String groupName, String consumerName,
                                                          long idleTime, TimeUnit unit, StreamMessageId... ids);

    /**
     * 自动转移待处理 Stream 消息所有权。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 新消费者
     * @param idleTime     最小空闲时间
     * @param unit         时间单位
     * @param startId      起始 ID
     * @param count        数量
     * @return 自动转移结果
     */
    AutoClaimResult<Object, Object> streamAutoClaim(String streamKey, String groupName, String consumerName,
                                                    long idleTime, TimeUnit unit, StreamMessageId startId, int count);

    /**
     * 裁剪 Stream。
     *
     * @param streamKey Stream key
     * @param args      裁剪参数
     * @return 裁剪数量
     */
    long streamTrim(String streamKey, StreamTrimArgs args);

    /**
     * 添加 Stream 对象监听器。
     *
     * @param streamKey Stream key
     * @param listener  对象监听器
     * @return 监听器 ID
     */
    int streamAddListener(String streamKey, ObjectListener listener);

    /**
     * 移除 Stream 对象监听器。
     *
     * @param streamKey  Stream key
     * @param listenerId 监听器 ID
     */
    void streamRemoveListener(String streamKey, int listenerId);

    // -------------------------------------------------------------------------
    // Executor / Scheduler 分布式任务
    // -------------------------------------------------------------------------

    /**
     * 获取分布式执行器。
     *
     * @param name 执行器名称
     * @return RExecutorService
     */
    RExecutorService getExecutorService(String name);

    /**
     * 获取分布式定时执行器。
     *
     * @param name 执行器名称
     * @return RScheduledExecutorService
     */
    RScheduledExecutorService getScheduledExecutorService(String name);

    /**
     * 执行 Runnable 分布式任务。
     *
     * @param name 执行器名称
     * @param task 任务
     */
    void executorExecute(String name, Runnable task);

    /**
     * 提交 Runnable 分布式任务。
     *
     * @param name 执行器名称
     * @param task 任务
     * @return Future
     */
    Future<?> executorSubmit(String name, Runnable task);

    /**
     * 提交 Callable 分布式任务。
     *
     * @param name 执行器名称
     * @param task 任务
     * @param <T>  返回类型
     * @return Future
     */
    <T> Future<T> executorSubmit(String name, Callable<T> task);

    /**
     * 关闭分布式执行器。
     *
     * @param name 执行器名称
     */
    void executorShutdown(String name);

    /**
     * 立即关闭分布式执行器。
     *
     * @param name 执行器名称
     * @return 未执行任务集合
     */
    List<Runnable> executorShutdownNow(String name);

    /**
     * 调度 Runnable 分布式任务。
     *
     * @param name  执行器名称
     * @param task  任务
     * @param delay 延迟时间
     * @param unit  时间单位
     * @return ScheduledFuture
     */
    ScheduledFuture<?> schedule(String name, Runnable task, long delay, TimeUnit unit);

    /**
     * 调度 Callable 分布式任务。
     *
     * @param name  执行器名称
     * @param task  任务
     * @param delay 延迟时间
     * @param unit  时间单位
     * @param <T>   返回类型
     * @return ScheduledFuture
     */
    <T> ScheduledFuture<T> schedule(String name, Callable<T> task, long delay, TimeUnit unit);

    /**
     * 固定频率调度 Runnable 分布式任务。
     *
     * @param name         执行器名称
     * @param task         任务
     * @param initialDelay 初始延迟
     * @param period       执行周期
     * @param unit         时间单位
     * @return ScheduledFuture
     */
    ScheduledFuture<?> scheduleAtFixedRate(String name, Runnable task, long initialDelay, long period, TimeUnit unit);

    /**
     * 固定延迟调度 Runnable 分布式任务。
     *
     * @param name         执行器名称
     * @param task         任务
     * @param initialDelay 初始延迟
     * @param delay        执行间隔
     * @param unit         时间单位
     * @return ScheduledFuture
     */
    ScheduledFuture<?> scheduleWithFixedDelay(String name, Runnable task, long initialDelay, long delay, TimeUnit unit);

    // -------------------------------------------------------------------------
    // RemoteService / LiveObject
    // -------------------------------------------------------------------------

    /**
     * 获取远程服务。
     *
     * @return RRemoteService
     */
    RRemoteService getRemoteService();

    /**
     * 获取指定名称的远程服务。
     *
     * @param name 服务名称
     * @return RRemoteService
     */
    RRemoteService getRemoteService(String name);

    /**
     * 注册远程服务实现。
     *
     * @param remoteInterface 远程服务接口
     * @param implementation  远程服务实现
     * @param <T>             服务类型
     */
    <T> void remoteRegister(Class<T> remoteInterface, T implementation);

    /**
     * 注册远程服务实现并指定工作线程数量。
     *
     * @param remoteInterface 远程服务接口
     * @param implementation  远程服务实现
     * @param workers         工作线程数量
     * @param <T>             服务类型
     */
    <T> void remoteRegister(Class<T> remoteInterface, T implementation, int workers);

    /**
     * 获取远程服务代理。
     *
     * @param remoteInterface 远程服务接口
     * @param <T>             服务类型
     * @return 服务代理
     */
    <T> T remoteGet(Class<T> remoteInterface);

    /**
     * 获取 LiveObject 服务。
     *
     * @return RLiveObjectService
     */
    RLiveObjectService getLiveObjectService();

    /**
     * 附加 LiveObject。
     *
     * @param detachedObject 游离对象
     * @param <T>            对象类型
     * @return LiveObject
     */
    <T> T liveObjectAttach(T detachedObject);

    /**
     * 合并 LiveObject。
     *
     * @param detachedObject 游离对象
     * @param <T>            对象类型
     * @return LiveObject
     */
    <T> T liveObjectMerge(T detachedObject);

    /**
     * 获取 LiveObject。
     *
     * @param entityClass 实体类型
     * @param id          实体 ID
     * @param <T>         对象类型
     * @return LiveObject
     */
    <T> T liveObjectGet(Class<T> entityClass, Object id);

    /**
     * 删除 LiveObject。
     *
     * @param attachedObject 已附加对象
     */
    void liveObjectDelete(Object attachedObject);

    /**
     * 根据类型和 ID 删除 LiveObject。
     *
     * @param entityClass 实体类型
     * @param id          实体 ID
     */
    void liveObjectDelete(Class<?> entityClass, Object id);

    // -------------------------------------------------------------------------
    // Object Listener / 对象监听
    // -------------------------------------------------------------------------

    /**
     * 添加全局对象监听器。
     *
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    int addGlobalObjectListener(ObjectListener listener);

    /**
     * 移除全局对象监听器。
     *
     * @param listenerId 监听器 ID
     */
    void removeGlobalObjectListener(int listenerId);

    /**
     * 添加 Bucket 对象监听器。
     *
     * @param key      Redis 键
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    int addBucketListener(String key, ObjectListener listener);

    /**
     * 移除 Bucket 对象监听器。
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     */
    void removeBucketListener(String key, int listenerId);

    /**
     * 添加 Map 对象监听器。
     *
     * @param key      Redis 键
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    int addMapListener(String key, ObjectListener listener);

    /**
     * 添加 Map Entry 监听器。
     *
     * @param key      Redis 键
     * @param listener Entry 监听器
     * @return 监听器 ID
     */
    int addMapEntryListener(String key, ObjectListener listener);

    /**
     * 移除 Map 监听器。
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     */
    void removeMapListener(String key, int listenerId);

    /**
     * 添加 Queue 对象监听器。
     *
     * @param key      Redis 键
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    int addQueueListener(String key, ObjectListener listener);

    /**
     * 移除 Queue 对象监听器。
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     */
    void removeQueueListener(String key, int listenerId);

    /**
     * 添加 Set 对象监听器。
     *
     * @param key      Redis 键
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    int addSetListener(String key, ObjectListener listener);

    /**
     * 移除 Set 对象监听器。
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     */
    void removeSetListener(String key, int listenerId);
}
