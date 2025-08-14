package local.ateng.java.redisjdk8.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
     * @param clazz 目标类型（如 User.class）
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

    /**
     * 批量获取多个字符串 key 对应的值
     *
     * @param keys Redis 键列表
     * @return 包含 key 和对应 value 的 Map，不存在的 key 不会出现在结果中
     */
    Map<String, Object> entries(Collection<String> keys);

    /**
     * 批量获取多个字符串 key 对应的值
     *
     * @param keys  Redis 键列表
     * @param clazz 目标类型（如 User.class）
     * @param <T>   旧值的返回类型
     * @return 包含 key 和对应 value 的 Map，不存在的 key 不会出现在结果中
     */
    <T> Map<String, T> entries(Collection<String> keys, Class<T> clazz);

    /**
     * 批量获取多个字符串 key 对应的值
     *
     * @param keys          Redis 键列表
     * @param typeReference 旧值类型引用
     * @param <T>           旧值的返回类型
     * @return 包含 key 和对应 value 的 Map，不存在的 key 不会出现在结果中
     */
    <T> Map<String, T> entries(Collection<String> keys, TypeReference<T> typeReference);

    // -------------------------- 哈希（Hash）操作 --------------------------

    /**
     * 设置哈希字段值
     *
     * @param key   Redis 键
     * @param field 哈希字段名
     * @param value 要存储的对象（会自动序列化）
     */
    void hPut(String key, String field, Object value);

    /**
     * 获取哈希字段值
     *
     * @param key   Redis 键
     * @param field 哈希字段名
     * @param clazz 返回类型
     * @param <T>   类型泛型
     * @return 字段对应的值，若不存在返回 null
     */
    <T> T hGet(String key, String field, Class<T> clazz);

    /**
     * 获取哈希字段值（支持复杂泛型类型）
     *
     * @param key           Redis 键
     * @param field         哈希字段名
     * @param typeReference 返回类型引用（支持泛型）
     * @param <T>           类型泛型
     * @return 字段对应的值，若不存在返回 null
     */
    <T> T hGet(String key, String field, TypeReference<T> typeReference);

    /**
     * 删除一个或多个哈希字段
     *
     * @param key    Redis 键
     * @param fields 要删除的字段名，可多个
     */
    void hDelete(String key, String... fields);

    /**
     * 判断哈希中是否存在指定字段
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 若存在返回 true，否则返回 false
     */
    boolean hHasKey(String key, String field);

    /**
     * 获取哈希表中所有字段与值
     *
     * @param key Redis 键
     * @return 包含所有字段及其值的 Map
     */
    Map<String, Object> hEntries(String key);

    /**
     * 获取哈希表中所有字段与值，并转换为指定类型的 Map
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 包含所有字段及其值的 Map，值均转换为指定类型，若 key 不存在返回空 Map
     */
    <T> Map<String, T> hEntries(String key, Class<T> clazz);

    /**
     * 获取哈希表中所有字段与值，并转换为指定泛型类型的 Map
     *
     * @param key           Redis 键
     * @param typeReference 目标类型引用（支持泛型）
     * @param <T>           目标类型泛型
     * @return 包含所有字段及其值的 Map，值均转换为指定类型，若 key 不存在返回空 Map
     */
    <T> Map<String, T> hEntries(String key, TypeReference<T> typeReference);

    /**
     * 获取哈希表中所有字段名
     *
     * @param key Redis 键
     * @return 所有字段名组成的 Set
     */
    Set<String> hKeys(String key);

    /**
     * 获取哈希表中所有字段值
     *
     * @param key Redis 键
     * @return 所有字段值组成的集合
     */
    Collection<Object> hValues(String key);

    /**
     * 获取哈希表中所有字段值，并转换为指定类型集合
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 所有字段值组成的指定类型集合，若转换失败则对应元素为 null
     */
    <T> Collection<T> hValues(String key, Class<T> clazz);

    /**
     * 获取哈希表中所有字段值，并转换为指定泛型集合
     *
     * @param key           Redis 键
     * @param typeReference 目标类型引用（支持泛型）
     * @param <T>           泛型类型
     * @return 所有字段值组成的指定类型集合，若转换失败则对应元素为 null
     */
    <T> Collection<T> hValues(String key, TypeReference<T> typeReference);

    /**
     * 获取哈希字段数量
     *
     * @param key Redis 键
     * @return 字段个数
     */
    int hSize(String key);

    /**
     * 清空哈希表（删除所有字段）
     *
     * @param key Redis 键
     */
    void hClear(String key);

    // -------------------------- 列表（List）操作 --------------------------

    /**
     * 将元素添加到列表右端（尾部）
     *
     * @param key   Redis 键
     * @param value 要添加的元素
     */
    void lRightPush(String key, Object value);

    /**
     * 将多个元素添加到列表右端（尾部）
     *
     * @param key    Redis 键
     * @param values 要添加的多个元素
     */
    void lRightPushAll(String key, Collection<?> values);

    /**
     * 从列表左端弹出元素
     *
     * @param key Redis 键
     * @return 弹出的元素，若列表为空或不存在返回 null
     */
    Object lLeftPop(String key);

    /**
     * 从列表左端弹出元素，并转换为指定类型
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 弹出的元素，若列表为空或不存在返回 null
     */
    <T> T lLeftPop(String key, Class<T> clazz);

    /**
     * 获取列表指定范围内的元素（包含 start 和 end）
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引（-1 表示最后一个元素）
     * @return 元素集合，若列表不存在返回空集合
     */
    List<Object> lRange(String key, long start, long end);

    /**
     * 获取列表指定范围内的元素，并转换为指定类型集合
     *
     * @param key   Redis 键
     * @param start 起始索引
     * @param end   结束索引
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素集合，若列表不存在返回空集合
     */
    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    /**
     * 获取列表指定范围内的元素，并转换为指定泛型类型集合
     *
     * @param key           Redis 键
     * @param start         起始索引
     * @param end           结束索引
     * @param typeReference 目标类型引用（支持泛型）
     * @param <T>           泛型类型
     * @return 元素集合，若列表不存在返回空集合
     */
    <T> List<T> lRange(String key, long start, long end, TypeReference<T> typeReference);

    /**
     * 获取列表长度
     *
     * @param key Redis 键
     * @return 列表长度，若不存在返回 0
     */
    long lSize(String key);

    /**
     * 删除列表中等于 value 的元素，count 指定删除数量
     *
     * @param key   Redis 键
     * @param count 删除数量（>0 从头开始删除，<0 从尾开始删除，=0 删除所有）
     * @param value 要删除的元素
     * @return 删除的元素数量
     */
    long lRemove(String key, long count, Object value);

    /**
     * 获取列表中指定索引的元素
     *
     * @param key   Redis 键
     * @param index 索引位置（0-based，负数从尾部计数）
     * @return 元素，若索引不存在返回 null
     */
    Object lIndex(String key, long index);

    /**
     * 获取列表中指定索引的元素，并转换为指定类型
     *
     * @param key   Redis 键
     * @param index 索引位置
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素，若索引不存在返回 null
     */
    <T> T lIndex(String key, long index, Class<T> clazz);

    /**
     * 根据索引修改列表元素的值
     *
     * @param key   Redis 键
     * @param index 索引位置
     * @param value 新值
     */
    void lSet(String key, long index, Object value);

    /**
     * 清空整个列表
     *
     * @param key Redis 键
     */
    void lClear(String key);

    // -------------------------- 集合（Set）操作 --------------------------

    /**
     * 添加一个或多个元素到集合中（去重）
     *
     * @param key   Redis 键
     * @param value 元素，可传多个
     * @return true 表示集合有新增元素，false 表示无新增元素
     */
    boolean sAdd(String key, Object... value);

    /**
     * 添加多个元素到集合中（去重）
     *
     * @param key   Redis 键
     * @param value 元素，多个
     * @return true 表示集合有新增元素，false 表示无新增元素
     */
    boolean sAdd(String key, List<Object> value);

    /**
     * 判断集合中是否存在指定元素
     *
     * @param key   Redis 键
     * @param value 要判断的元素
     * @return true 存在，false 不存在
     */
    boolean sIsMember(String key, Object value);

    /**
     * 获取集合中的所有元素
     *
     * @param key Redis 键
     * @return 元素集合（无序去重）
     */
    Set<Object> sMembers(String key);

    /**
     * 获取集合中的所有元素并转换为指定类型
     *
     * @param key   Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 元素集合（无序去重）
     */
    <T> Set<T> sMembers(String key, Class<T> clazz);

    /**
     * 获取集合中的所有元素并转换为指定类型（支持复杂泛型结构）
     *
     * @param key           Redis 键
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 元素集合（无序去重）
     */
    <T> Set<T> sMembers(String key, TypeReference<T> typeReference);

    /**
     * 获取集合中元素的数量
     *
     * @param key Redis 键
     * @return 集合大小（元素个数）
     */
    long sSize(String key);

    /**
     * 从集合中随机弹出一个元素
     *
     * @param key Redis 键
     * @return 被移除的元素，若集合为空则返回 null
     */
    Object sPop(String key);

    /**
     * 从集合中随机弹出一个元素并转换为指定类型
     *
     * @param key   Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 被移除并转换后的元素，若集合为空则返回 null
     */
    <T> T sPop(String key, Class<T> clazz);

    /**
     * 从集合中随机弹出一个元素并转换为指定类型（支持复杂泛型结构）
     *
     * @param key           Redis 键
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 被移除并转换后的元素，若集合为空则返回 null
     */
    <T> T sPop(String key, TypeReference<T> typeReference);

    /**
     * 从集合中移除一个或多个元素
     *
     * @param key    Redis 键
     * @param values 要移除的元素
     * @return 实际移除的元素数量
     */
    boolean sRemove(String key, Object... values);

    /**
     * 从集合中移除多个元素
     *
     * @param key    Redis 键
     * @param values 要移除的元素
     * @return 实际移除的元素数量
     */
    boolean sRemove(String key, List<Object> values);

    /**
     * 随机获取集合中的一个元素（不移除）
     *
     * @param key Redis 键
     * @return 随机元素，若集合为空则返回 null
     */
    Object sRandomMember(String key);

    /**
     * 随机获取集合中的一个元素并转换为指定类型（不移除）
     *
     * @param key   Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 转换后的随机元素，若集合为空则返回 null
     */
    <T> T sRandomMember(String key, Class<T> clazz);

    /**
     * 随机获取集合中的一个元素并转换为指定类型（不移除，支持复杂泛型结构）
     *
     * @param key           Redis 键
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 转换后的随机元素，若集合为空则返回 null
     */
    <T> T sRandomMember(String key, TypeReference<T> typeReference);

    /**
     * 获取集合中的多个随机元素
     *
     * @param key   Redis 键
     * @param count 获取的元素数量
     * @return 随机元素集合（数量可能小于 count）
     */
    Set<Object> sRandomMembers(String key, int count);

    /**
     * 获取集合中的多个随机元素并转换为指定类型
     *
     * @param key   Redis 键
     * @param count 获取的元素数量
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 转换后的随机元素集合（数量可能小于 count）
     */
    <T> Set<T> sRandomMembers(String key, int count, Class<T> clazz);

    /**
     * 获取集合中的多个随机元素并转换为指定类型（支持复杂泛型结构）
     *
     * @param key           Redis 键
     * @param count         获取的元素数量
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 转换后的随机元素集合（数量可能小于 count）
     */
    <T> Set<T> sRandomMembers(String key, int count, TypeReference<T> typeReference);

    /**
     * 获取两个集合的并集（不改变原集合）
     *
     * @param key1 第一个 Redis 键
     * @param key2 第二个 Redis 键
     * @return 两个集合的并集（去重）
     */
    Set<Object> sUnion(String key1, String key2);

    /**
     * 获取两个集合的并集（不改变原集合），并转换为指定类型
     *
     * @param key1  第一个 Redis 键
     * @param key2  第二个 Redis 键
     * @param clazz 返回元素的类型 Class
     * @param <T>   元素泛型类型
     * @return 并集结果集合（去重）并转换为指定类型
     */
    <T> Set<T> sUnion(String key1, String key2, Class<T> clazz);

    /**
     * 获取两个集合的并集（不改变原集合），并转换为指定复杂类型
     *
     * @param key1          第一个 Redis 键
     * @param key2          第二个 Redis 键
     * @param typeReference 返回元素的类型 TypeReference（支持复杂类型）
     * @param <T>           元素泛型类型
     * @return 并集结果集合（去重）并转换为指定类型
     */
    <T> Set<T> sUnion(String key1, String key2, TypeReference<T> typeReference);

    /**
     * 获取两个集合的交集（不改变原集合）
     *
     * @param key1 第一个 Redis 键
     * @param key2 第二个 Redis 键
     * @return 两个集合的交集
     */
    Set<Object> sIntersect(String key1, String key2);

    /**
     * 获取两个集合的交集（不改变原集合），并转换为指定类型
     *
     * @param key1  第一个 Redis 键
     * @param key2  第二个 Redis 键
     * @param clazz 返回元素的类型 Class
     * @param <T>   元素泛型类型
     * @return 交集结果集合并转换为指定类型
     */
    <T> Set<T> sIntersect(String key1, String key2, Class<T> clazz);

    /**
     * 获取两个集合的交集（不改变原集合），并转换为指定复杂类型
     *
     * @param key1          第一个 Redis 键
     * @param key2          第二个 Redis 键
     * @param typeReference 返回元素的类型 TypeReference（支持复杂类型）
     * @param <T>           元素泛型类型
     * @return 交集结果集合并转换为指定类型
     */
    <T> Set<T> sIntersect(String key1, String key2, TypeReference<T> typeReference);

    /**
     * 获取两个集合的差集（key1 相对于 key2 的差）
     *
     * @param key1 第一个 Redis 键（原始集合）
     * @param key2 第二个 Redis 键（要排除的集合）
     * @return 差集结果（存在于 key1 而不存在于 key2 的元素）
     */
    Set<Object> sDifference(String key1, String key2);

    /**
     * 获取两个集合的差集（key1 相对于 key2 的差），并转换为指定类型
     *
     * @param key1  第一个 Redis 键（原始集合）
     * @param key2  第二个 Redis 键（要排除的集合）
     * @param clazz 返回元素的类型 Class
     * @param <T>   元素泛型类型
     * @return 差集结果集合并转换为指定类型
     */
    <T> Set<T> sDifference(String key1, String key2, Class<T> clazz);

    /**
     * 获取两个集合的差集（key1 相对于 key2 的差），并转换为指定复杂类型
     *
     * @param key1          第一个 Redis 键（原始集合）
     * @param key2          第二个 Redis 键（要排除的集合）
     * @param typeReference 返回元素的类型 TypeReference（支持复杂类型）
     * @param <T>           元素泛型类型
     * @return 差集结果集合并转换为指定类型
     */
    <T> Set<T> sDifference(String key1, String key2, TypeReference<T> typeReference);

    // -------------------------- 有序集合（ZSet / SortedSet）操作 --------------------------

    /**
     * 添加一个元素及其分数到有序集合中。
     *
     * @param key   有序集合的 key
     * @param value 要添加的元素
     * @param score 元素的分数（用于排序）
     * @return 是否添加成功，若元素已存在则更新分数
     */
    boolean zAdd(String key, Object value, double score);

    /**
     * 批量添加元素及其分数到有序集合中。
     *
     * @param key      有序集合的 key
     * @param scoreMap 元素与对应分数的映射
     * @return 成功添加的元素数量（不包括更新）
     */
    int zAddAll(String key, Map<Object, Double> scoreMap);

    /**
     * 从有序集合中移除指定元素。
     *
     * @param key    有序集合的 key
     * @param values 要移除的元素列表
     * @return 实际移除的元素数量
     */
    boolean zRemove(String key, Object... values);

    /**
     * 从有序集合中移除指定元素。
     *
     * @param key    有序集合的 key
     * @param values 要移除的元素列表
     * @return 实际移除的元素数量
     */
    boolean zRemove(String key, List<Object> values);

    /**
     * 获取有序集合中某个元素的分数。
     *
     * @param key   有序集合的 key
     * @param value 指定的元素
     * @return 元素的分数，若元素不存在则返回 null
     */
    Double zScore(String key, Object value);

    /**
     * 获取有序集合中指定元素的排名（按分数升序）。
     *
     * @param key   有序集合的 key
     * @param value 指定的元素
     * @return 元素的排名（0 基础），若不存在返回 null
     */
    Integer zRank(String key, Object value);

    /**
     * 获取有序集合中指定元素的排名（按分数降序）。
     *
     * @param key   有序集合的 key
     * @param value 指定的元素
     * @return 元素的排名（0 基础），若不存在返回 null
     */
    Integer zRevRank(String key, Object value);

    /**
     * 获取有序集合中指定分数区间内的元素（按升序）。
     *
     * @param key 有序集合的 key
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 区间内的所有元素，按分数升序排列，元素为原始对象
     */
    Set<Object> zRangeByScore(String key, double min, double max);

    /**
     * 获取有序集合中指定分数区间内的元素（按升序），并转换为指定类型。
     *
     * @param key   有序集合的 key
     * @param min   最小分数（包含）
     * @param max   最大分数（包含）
     * @param clazz 目标类型的 Class
     * @param <T>   返回集合中元素的目标类型
     * @return 区间内的所有元素，按分数升序排列，并转换为目标类型
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, Class<T> clazz);

    /**
     * 获取有序集合中指定分数区间内的元素（按升序），并转换为复杂泛型类型。
     *
     * @param key           有序集合的 key
     * @param min           最小分数（包含）
     * @param max           最大分数（包含）
     * @param typeReference Jackson 的 TypeReference，用于描述复杂泛型类型
     * @param <T>           返回集合中元素的目标类型
     * @return 区间内的所有元素，按分数升序排列，并转换为目标类型
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, TypeReference<T> typeReference);

    /**
     * 获取有序集合中指定分数区间内的元素及其分数（按升序）。
     *
     * @param key 有序集合的 key
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 区间内元素及其分数的 Map，按分数升序排列
     */
    Map<Object, Double> zRangeByScoreWithScores(String key, double min, double max);

    /**
     * 获取有序集合中指定分数区间内的元素及其分数（按降序）。
     *
     * @param key 有序集合的 key
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 区间内元素及其分数的 Map，按分数降序排列
     */
    Map<Object, Double> zRevRangeByScoreWithScores(String key, double min, double max);

    /**
     * 获取有序集合中指定排名区间内的元素（按升序）。
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 指定区间内的元素集合，按分数升序排列
     */
    Set<Object> zRange(String key, int start, int end);

    /**
     * 获取有序集合中指定排名区间内的元素及其分数（按升序）。
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 区间内元素及其分数的 Map，按分数升序排列
     */
    Map<Object, Double> zRangeWithScores(String key, int start, int end);

    /**
     * 获取有序集合中指定排名区间内的元素（按降序）。
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 区间内元素集合，按分数降序排列
     */
    Set<Object> zRevRange(String key, int start, int end);

    /**
     * 获取有序集合中指定排名区间内的元素及其分数（按降序）。
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 区间内元素及其分数的 Map，按分数降序排列
     */
    Map<Object, Double> zRevRangeWithScores(String key, int start, int end);

    /**
     * 为有序集合中指定元素的分数增加指定值。
     *
     * @param key   有序集合的 key
     * @param value 指定元素
     * @param delta 要增加的分数（可为负）
     * @return 增加后的新分数
     */
    Double zIncrBy(String key, Object value, double delta);

    /**
     * 获取有序集合的元素数量。
     *
     * @param key 有序集合的 key
     * @return 元素总数
     */
    int zCard(String key);

    /**
     * 获取有序集合中分数在指定区间内的元素数量。
     *
     * @param key 有序集合的 key
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 指定分数范围内的元素个数
     */
    long zCount(String key, double min, double max);

    /**
     * 移除指定分数区间内的所有元素。
     *
     * @param key 有序集合的 key
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 实际移除的元素个数
     */
    long zRemoveRangeByScore(String key, double min, double max);

    /**
     * 移除指定排名区间内的所有元素。
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 实际移除的元素个数
     */
    long zRemoveRangeByRank(String key, int start, int end);

    // -------------------------- 分布式锁与同步器 --------------------------

    /**
     * 获取可重入分布式锁（默认锁名）。
     *
     * @param lockKey 锁的 key
     * @return RLock 实例
     */
    RLock getLock(String lockKey);

    /**
     * 阻塞式获取锁，直到成功。
     *
     * @param lockKey 锁的 key
     */
    void lock(String lockKey);

    /**
     * 阻塞式获取锁，设置自动释放时间。
     *
     * @param lockKey   锁的 key
     * @param leaseTime 自动释放时间，单位：秒
     */
    void lock(String lockKey, long leaseTime);

    /**
     * 尝试获取锁，如果获取到则在指定时间后自动释放。
     *
     * @param lockKey   锁的 key
     * @param waitTime  等待时间
     * @param leaseTime 自动释放时间
     * @param unit      时间单位
     * @return 是否成功获取锁
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁。
     *
     * @param lockKey 锁的 key
     */
    void unlock(String lockKey);

    /**
     * 判断当前线程是否持有指定的锁。
     *
     * @param key 锁的名称
     * @return 如果当前线程持有该锁，返回 true；否则返回 false
     */
    boolean isHeldByCurrentThread(String key);

    /**
     * 判断指定的锁当前是否被任意线程持有。
     *
     * @param key 锁的名称
     * @return 如果该锁已被任意线程持有，返回 true；否则返回 false
     */
    boolean isLocked(String key);

    /**
     * 获取读锁。
     *
     * @param lockKey 锁的 key
     */
    void readLock(String lockKey);

    /**
     * 获取写锁。
     *
     * @param lockKey 锁的 key
     */
    void writeLock(String lockKey);

    /**
     * 尝试获取读锁。
     *
     * @param lockKey   锁的 key
     * @param waitTime  等待时间
     * @param leaseTime 自动释放时间
     * @param unit      时间单位
     * @return 是否成功获取读锁
     */
    boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 尝试获取写锁。
     *
     * @param lockKey   锁的 key
     * @param waitTime  等待时间
     * @param leaseTime 自动释放时间
     * @param unit      时间单位
     * @return 是否成功获取写锁
     */
    boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放读锁。
     *
     * @param lockKey 锁的 key
     */
    void unlockRead(String lockKey);

    /**
     * 释放写锁。
     *
     * @param lockKey 锁的 key
     */
    void unlockWrite(String lockKey);

    /**
     * 设置闭锁的计数。
     *
     * @param latchKey 闭锁 key
     * @param count    计数器初始值
     */
    void setCount(String latchKey, int count);

    /**
     * 递减计数器，释放等待线程。
     *
     * @param latchKey 闭锁 key
     */
    void countDown(String latchKey);

    /**
     * 阻塞等待直到计数器归零。
     *
     * @param latchKey 闭锁 key
     * @throws InterruptedException 中断异常
     */
    void await(String latchKey) throws InterruptedException;

    /**
     * 在指定时间内等待计数器归零。
     *
     * @param latchKey 闭锁 key
     * @param timeout  最大等待时长
     * @param unit     时间单位
     * @return 是否成功等待完成
     * @throws InterruptedException 中断异常
     */
    boolean await(String latchKey, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 初始化信号量许可数。
     *
     * @param semaphoreKey 信号量 key
     * @param permits      初始许可数量
     */
    void trySetPermits(String semaphoreKey, int permits);

    /**
     * 获取一个信号量许可（阻塞直到成功）。
     *
     * @param semaphoreKey 信号量 key
     * @throws InterruptedException 中断异常
     */
    void acquire(String semaphoreKey) throws InterruptedException;

    /**
     * 尝试获取一个信号量许可，限时等待。
     *
     * @param semaphoreKey 信号量 key
     * @param timeout      最大等待时长
     * @param unit         时间单位
     * @return 是否成功获取许可
     * @throws InterruptedException 中断异常
     */
    boolean tryAcquire(String semaphoreKey, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 释放一个许可。
     *
     * @param semaphoreKey 信号量 key
     */
    void release(String semaphoreKey);

    /**
     * 获取当前可用许可数。
     *
     * @param semaphoreKey 信号量 key
     * @return 可用许可数
     */
    int availablePermits(String semaphoreKey);

    // -------------------------- 布隆过滤器 --------------------------

    /**
     * 初始化布隆过滤器，设置预期插入元素数量和误判率。
     *
     * @param key                布隆过滤器对应的 Redis 键
     * @param expectedInsertions 预期插入的元素数量（用于计算位数组大小）
     * @param falseProbability   期望的误判率（一般建议0.03或更小）
     */
    void bloomInit(String key, long expectedInsertions, double falseProbability);

    /**
     * 判断元素是否可能存在布隆过滤器中。
     *
     * @param key   布隆过滤器对应的 Redis 键
     * @param value 要检测的元素
     * @return true 表示元素可能存在（误判存在）；false 表示一定不存在
     */
    boolean bloomContains(String key, Object value);

    /**
     * 添加元素到布隆过滤器中。
     *
     * @param key   布隆过滤器对应的 Redis 键
     * @param value 要添加的元素
     * @return true 如果元素之前不存在且已成功添加，false 如果元素可能已存在
     */
    boolean bloomAdd(String key, Object value);

    /**
     * 批量添加元素到布隆过滤器中。
     *
     * @param key    布隆过滤器对应的 Redis 键
     * @param values 批量元素集合
     * @return 添加成功的元素数量
     */
    long bloomAddAll(String key, Collection<?> values);

    /**
     * 删除布隆过滤器（删除对应的 Redis 键）。
     *
     * @param key 布隆过滤器对应的 Redis 键
     * @return 是否成功删除
     */
    boolean bloomDelete(String key);

    /**
     * 判断布隆过滤器是否已经初始化（是否存在）。
     *
     * @param key 布隆过滤器对应的 Redis 键
     * @return true 表示已初始化，false 表示未初始化
     */
    boolean bloomExists(String key);

    /**
     * 获取布隆过滤器的预计插入容量。
     *
     * @param key 布隆过滤器对应的 Redis 键
     * @return 预计插入元素数量，若未初始化则返回 0
     */
    long bloomGetExpectedInsertions(String key);

    /**
     * 获取布隆过滤器的误判率。
     *
     * @param key 布隆过滤器对应的 Redis 键
     * @return 当前设置的误判率，若未初始化则返回 0.0
     */
    double bloomGetFalseProbability(String key);

    // --------------------- 分布式队列操作 ---------------------

    /**
     * 将元素添加到指定队列尾部（阻塞方式，队列满时等待）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param value    要入队的元素
     * @throws InterruptedException 阻塞等待时被中断异常
     */
    void enqueueBlocking(String queueKey, Object value) throws InterruptedException;

    /**
     * 将元素添加到指定队列尾部（非阻塞方式）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param value    要入队的元素
     * @return 是否成功入队，失败可能是队列已满
     */
    boolean enqueue(String queueKey, Object value);

    /**
     * 从指定队列头部获取并移除元素（阻塞方式，队列为空时等待）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param timeout  最大等待时间，单位秒
     * @return 队头元素，超时返回 null
     * @throws InterruptedException 阻塞等待时被中断异常
     */
    Object dequeueBlocking(String queueKey, long timeout) throws InterruptedException;

    /**
     * 从指定队列头部获取并移除元素（非阻塞方式）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @return 队头元素，若队列为空返回 null
     */
    Object dequeue(String queueKey);

    /**
     * 获取队列长度。
     *
     * @param queueKey 队列对应的 Redis 键
     * @return 当前队列长度
     */
    long queueSize(String queueKey);

    // --------------------- 限流操作 ---------------------

    /**
     * 初始化分布式限流器（令牌桶算法）
     *
     * @param key      限流器的 Redis Key（唯一标识）
     * @param rateType 限流模式：OVERALL（全局限流）或 PER_CLIENT（每客户端限流）
     * @param rate     每个时间间隔允许的最大请求数
     * @param interval 时间间隔值
     * @param unit     时间单位（秒、分钟等）
     * @return true 表示设置成功；false 表示限流器已存在
     */
    boolean rateLimiterInit(String key, RateType rateType, long rate, long interval, RateIntervalUnit unit);

    /**
     * 尝试获取一个令牌（非阻塞式）
     *
     * @param key 限流器的 Redis Key
     * @return true 表示获取成功，false 表示被限流
     */
    boolean rateLimiterTryAcquire(String key);

    /**
     * 尝试在指定时间内获取一个令牌（阻塞等待，超时返回）
     *
     * @param key     限流器的 Redis Key
     * @param timeout 最大等待时间
     * @param unit    时间单位
     * @return true 表示获取成功，false 表示超时未获取
     */
    boolean rateLimiterTryAcquire(String key, long timeout, TimeUnit unit);

    /**
     * 获取限流器对象（可用于自定义高级操作）
     *
     * @param key 限流器 Redis Key
     * @return RRateLimiter 实例
     */
    RRateLimiter rateLimiterGet(String key);

    /**
     * 删除限流器配置（从 Redis 清除）
     *
     * @param key 限流器 Redis Key
     * @return true 表示删除成功；false 表示不存在
     */
    boolean rateLimiterDelete(String key);

    // --------------------- 发布订阅操作 ---------------------

    /**
     * 向指定频道发布消息。
     *
     * @param channel 频道名称
     * @param message 要发布的消息内容
     */
    void publish(String channel, Object message);

    /**
     * 订阅指定频道，异步接收消息。
     *
     * @param channel         频道名称
     * @param messageConsumer 消息回调函数，接收到消息时执行
     */
    void subscribe(String channel, java.util.function.Consumer<Object> messageConsumer);

    /**
     * 取消订阅指定频道。
     *
     * @param channel 频道名称
     */
    void unsubscribe(String channel);

    // --------------------- Lua 脚本操作 ---------------------

    /**
     * 在 Redis 中执行 Lua 脚本（返回单一结果）。
     *
     * @param script     Lua 脚本内容（例如 "return redis.call('set', KEYS[1], ARGV[1])"）
     * @param keys       脚本中需要用到的 KEYS 参数（如 KEYS[1]、KEYS[2]）
     * @param args       脚本中需要用到的 ARGV 参数（如 ARGV[1]、ARGV[2]）
     * @param returnType 返回值类型（用于指定 Redis 返回的数据类型，如 Boolean、Long、String、List 等）
     * @param <T>        返回值类型（根据 Redis 返回的类型自动转换，例如 String、Long、Boolean 等）
     * @return 执行结果
     * <p>
     * 核心逻辑：
     * 1. 使用 RScript 对象执行 Lua 脚本
     * 2. RScript.Mode.READ_WRITE 表示既能读也能写（一般 Lua 脚本会修改数据）
     * 3. StringCodec 用于将 Redis 数据以字符串方式编码/解码
     * 4. RScript.ReturnType.VALUE 表示返回单一值（也可以改为 MULTI、BOOLEAN 等）
     * 5. keys 是脚本的 KEYS 数组，args 是 ARGV 数组
     */
    <T> T eval(String script, Class<T> returnType, List<Object> keys, Object... args);

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
    void evalNoResult(String script, List<Object> keys, Object... args);

    /**
     * 通过 SHA1 执行已加载的 Lua 脚本，并返回指定类型结果。
     *
     * @param sha1       Lua 脚本的 SHA1
     * @param returnType 返回类型 Class
     * @param keys       脚本中的 KEYS
     * @param values     脚本中的 ARGV
     * @param <T>        返回值泛型
     * @return 脚本执行结果
     * <p>
     * 核心逻辑：
     * 1. 使用 RScript.evalSha 执行 Redis 缓存的 Lua 脚本
     * 2. 避免重复传输脚本内容，提高性能
     */
    <T> T evalBySha(String sha1, Class<T> returnType, List<Object> keys, Object... values);

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
    String loadScript(String script);

}
