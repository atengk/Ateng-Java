package local.ateng.java.redis.service;

import org.springframework.data.redis.connection.MessageListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务
 *
 * @author 孔余
 * @since 2025-07-31
 */
public interface RedisService {

    // ---------------------------------- 字符串操作 ----------------------------------

    /**
     * 设置值（无过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 设置值并指定过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间（例如 10）
     * @param unit    时间单位（例如 TimeUnit.SECONDS）
     */
    void set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 设置值并指定过期时间（JDK8 Duration）
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param duration 过期时间（如 Duration.ofMinutes(5)）
     */
    void set(String key, Object value, Duration duration);

    /**
     * 获取缓存值（Object 原始类型）
     *
     * @param key 缓存键
     * @return 缓存值，可能为 null
     */
    Object get(String key);

    /**
     * 获取缓存值并自动反序列化为指定类型
     *
     * @param key   缓存键
     * @param clazz 目标类型类
     * @param <T>   泛型类型
     * @return 指定类型的对象，可能为 null
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 获取缓存值并反序列化为 List 类型
     *
     * @param key   缓存键
     * @param clazz 列表中元素的类型
     * @param <T>   泛型类型
     * @return 列表对象，可能为空
     */
    <T> List<T> getList(String key, Class<T> clazz);

    /**
     * 获取缓存值并反序列化为 Set 类型
     *
     * @param key   缓存键
     * @param clazz 集合中元素的类型
     * @param <T>   泛型类型
     * @return Set 集合对象，可能为空
     */
    <T> Set<T> getSet(String key, Class<T> clazz);

    /**
     * 获取缓存值并反序列化为 Map 类型
     *
     * @param key   缓存键
     * @param clazz Map 中 value 的类型（key 默认为 String）
     * @param <T>   泛型类型
     * @return Map 对象，可能为空
     */
    <T> Map<String, T> getMap(String key, Class<T> clazz);

    /**
     * 批量获取多个 key 的缓存值（原始类型）
     *
     * @param keys key 集合
     * @return 原始对象列表（顺序对应 keys）
     */
    List<Object> multiGet(Collection<String> keys);

    /**
     * 批量获取多个 key 并反序列化为目标类型
     *
     * @param keys  key 集合
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 对象列表（顺序对应 keys）
     */
    <T> List<T> multiGet(Collection<String> keys, Class<T> clazz);

    /**
     * 获取旧值并设置新值
     *
     * @param key      缓存键
     * @param newValue 新值
     * @return 原始旧值对象
     */
    Object getAndSet(String key, Object newValue);

    /**
     * 获取旧值并设置新值，并反序列化为目标类型
     *
     * @param key      缓存键
     * @param newValue 新值
     * @param clazz    返回对象类型
     * @param <T>      泛型
     * @return 旧值反序列化结果
     */
    <T> T getAndSet(String key, Object newValue, Class<T> clazz);

    /**
     * 如果缓存中不存在该 key，则设置（等同于 SETNX）
     *
     * @param key   缓存键
     * @param value 值
     * @return true 设置成功，false 已存在
     */
    boolean setIfAbsent(String key, Object value);

    /**
     * 如果缓存中不存在该 key，则设置并设置过期时间
     *
     * @param key     缓存键
     * @param value   值
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 设置成功，false 已存在
     */
    boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 向已存在的字符串值追加内容
     *
     * @param key   缓存键
     * @param value 要追加的内容
     * @return 原始字符串追加后的长度
     */
    int append(String key, String value);

    /**
     * 获取值并立即删除（一次性消费场景）
     *
     * @param key   缓存键
     * @param clazz 返回对象类型
     * @param <T>   目标类型
     * @return 缓存值（已删除）
     */
    <T> T getAndDelete(String key, Class<T> clazz);

    // ---------------------------------- Key 管理操作 ----------------------------------

    /**
     * 删除指定 key
     *
     * @param key 要删除的 key
     * @return 是否删除成功
     */
    boolean delete(String key);

    /**
     * 批量删除多个 key
     *
     * @param keys 要删除的 key 集合
     * @return 成功删除的 key 数量
     */
    long deleteKeys(Collection<String> keys);

    /**
     * 判断指定 key 是否存在
     *
     * @param key 要检查的 key
     * @return key 是否存在
     */
    boolean hasKey(String key);

    /**
     * 设置 key 的过期时间（单位：秒）
     *
     * @param key     缓存 key
     * @param timeout 过期时间（秒）
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout);

    /**
     * 设置 key 的过期时间（支持自定义时间单位）
     *
     * @param key     缓存 key
     * @param timeout 过期时间
     * @param unit    时间单位（如 TimeUnit.SECONDS）
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 设置 key 的过期时间点（绝对时间）
     *
     * @param key      缓存 key
     * @param dateTime 到期时间点
     * @return 是否设置成功
     */
    boolean expireAt(String key, LocalDateTime dateTime);

    /**
     * 获取 key 的剩余过期时间（单位：秒）
     *
     * @param key 缓存 key
     * @return 剩余时间（秒），如果为 -1 表示永久不过期；-2 表示 key 不存在
     */
    long getExpire(String key);

    /**
     * 获取 key 的剩余过期时间（支持指定时间单位）
     *
     * @param key  缓存 key
     * @param unit 时间单位（如 TimeUnit.SECONDS）
     * @return 剩余时间（指定单位），-1 表示永久；-2 表示不存在
     */
    long getExpire(String key, TimeUnit unit);

    /**
     * 移除 key 的过期时间，使其永久存在
     *
     * @param key 缓存 key
     * @return 是否成功移除过期时间
     */
    boolean persist(String key);

    /**
     * 重命名指定 key（如新 key 已存在会覆盖）
     *
     * @param oldKey 原 key
     * @param newKey 新 key
     */
    void rename(String oldKey, String newKey);

    /**
     * 重命名指定 key（仅当新 key 不存在时生效）
     *
     * @param oldKey 原 key
     * @param newKey 新 key
     * @return 是否重命名成功
     */
    boolean renameIfAbsent(String oldKey, String newKey);

    /**
     * 获取所有匹配 pattern 的 key（⚠️ 避免在生产环境使用，可能引发阻塞）
     *
     * @param pattern 匹配模式（如：user:*）
     * @return 匹配到的 key 集合
     */
    Set<String> keys(String pattern);

    /**
     * 使用 SCAN 命令匹配 key（推荐替代 keys，适合大数据量）
     *
     * @param pattern 匹配模式（如：user:*）
     * @return 匹配到的 key 集合（非阻塞）
     */
    Set<String> scanKeys(String pattern);

    /**
     * 遍历 Hash 中的所有字段，返回匹配 pattern 的字段列表。
     *
     * @param hash    Hash 键
     * @param pattern 字段匹配模式，支持通配符
     * @return 匹配的字段列表
     */
    Set<String> scanHashKeys(String hash, String pattern);

    /**
     * 获取指定 key 的类型（string、list、hash、set、zset 等）
     *
     * @param key 要查询的 key
     * @return 类型字符串（如："string"、"list"）
     */
    String type(String key);

    /**
     * 获取 Redis 中的随机一个 key
     *
     * @return 随机 key 或 null（若数据库为空）
     */
    String randomKey();

    // ---------------------------------- Hash（哈希表）操作 ----------------------------------

    /**
     * 设置 Hash 中指定字段的值，如果字段已存在则会覆盖。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param value   值
     */
    void hSet(String key, String hashKey, Object value);

    /**
     * 设置 Hash 中指定字段的值，并设置主键过期时间（秒）。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param value   值
     * @param timeout 过期时间（单位：秒）
     */
    void hSet(String key, String hashKey, Object value, long timeout);

    /**
     * 设置 Hash 字段的值，并设置过期时间（支持自定义时间单位）。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位（如 TimeUnit.SECONDS）
     */
    void hSet(String key, String hashKey, Object value, long timeout, TimeUnit unit);

    /**
     * 一次性设置多个 Hash 字段及其对应的值。
     *
     * @param key Redis 键
     * @param map 字段值映射表
     */
    void hSetAll(String key, Map<String, Object> map);

    /**
     * 一次性设置多个 Hash 字段及其对应的值，并设置过期时间。
     *
     * @param key     Redis 键
     * @param map     字段值映射表
     * @param timeout 过期时间（单位：秒）
     */
    void hSetAll(String key, Map<String, Object> map, long timeout);

    /**
     * 一次性设置多个 Hash 字段，并设置过期时间（支持自定义时间单位）。
     *
     * @param key     Redis 键
     * @param map     字段值映射
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    void hSetAll(String key, Map<String, Object> map, long timeout, TimeUnit unit);

    /**
     * 获取指定 Hash 字段的值（返回原始对象）。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @return 字段对应的值
     */
    Object hGet(String key, String hashKey);

    /**
     * 获取指定 Hash 字段的值，并自动转换为指定类型。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param clazz   目标类型类对象
     * @param <T>     目标类型
     * @return 指定类型的字段值
     */
    <T> T hGet(String key, String hashKey, Class<T> clazz);

    /**
     * 批量获取 Hash 中多个字段的值。
     *
     * @param key      Redis 键
     * @param hashKeys 字段名集合
     * @return 字段值列表
     */
    List<Object> hMultiGet(String key, Collection<String> hashKeys);

    /**
     * 批量获取 Hash 中多个字段的值，并转换为指定类型。
     *
     * @param key      Redis 键
     * @param hashKeys 字段名集合
     * @param clazz    值的目标类型
     * @param <T>      泛型类型
     * @return 指定类型的字段值列表
     */
    <T> List<T> hMultiGet(String key, Collection<String> hashKeys, Class<T> clazz);

    /**
     * 获取整个 Hash 的所有键值对。
     *
     * @param key Redis 键
     * @return Hash 的键值对
     */
    Map<String, Object> hGetAll(String key);

    /**
     * 获取整个 Hash 的所有键值对，并将值转换为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 值的目标类型
     * @param <T>   泛型类型
     * @return Hash 的键值对（值为指定类型）
     */
    <T> Map<String, T> hGetAll(String key, Class<T> clazz);

    /**
     * 判断指定字段是否存在于 Hash 中。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @return 是否存在
     */
    boolean hHasKey(String key, String hashKey);

    /**
     * 删除一个或多个 Hash 字段。
     *
     * @param key      Redis 键
     * @param hashKeys 要删除的字段
     * @return 删除的字段数量
     */
    long hDelete(String key, String... hashKeys);

    /**
     * 获取 Hash 中字段的数量。
     *
     * @param key Redis 键
     * @return 字段数量
     */
    long hSize(String key);

    /**
     * 获取 Hash 中所有字段名称。
     *
     * @param key Redis 键
     * @return 字段名集合
     */
    Set<String> hKeys(String key);

    /**
     * 获取 Hash 中所有字段值（不指定类型）。
     *
     * @param key Redis 键
     * @return 字段值列表
     */
    List<Object> hValues(String key);

    /**
     * 获取 Hash 中所有字段值，并转换为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 值的类型
     * @param <T>   泛型类型
     * @return 字段值列表（指定类型）
     */
    <T> List<T> hValues(String key, Class<T> clazz);

    /**
     * 将 Hash 中指定字段值递增指定的 long 值。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param delta   增量值
     * @return 增加后的结果
     */
    long hIncrBy(String key, String hashKey, long delta);

    /**
     * 将 Hash 中指定字段值递增指定的 double 值。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param delta   增量值
     * @return 增加后的结果
     */
    double hIncrByFloat(String key, String hashKey, double delta);

    /**
     * 如果字段不存在则设置值，若存在则不操作。
     *
     * @param key     Redis 键
     * @param hashKey Hash 字段
     * @param value   值
     * @return true 表示设置成功；false 表示字段已存在
     */
    boolean hPutIfAbsent(String key, String hashKey, Object value);

    /**
     * 使用 SCAN 操作匹配 Hash 中字段名（适用于大数据量场景）。
     *
     * @param key     Redis 键
     * @param pattern 字段名匹配模式（支持 * ? [] 通配）
     * @return 匹配到的字段名集合
     */
    Set<String> hScan(String key, String pattern);

    // ---------------------------------- List 操作 ----------------------------------

    /**
     * 从左侧插入一个元素
     *
     * @param key   Redis键
     * @param value 要插入的值
     */
    void lPush(String key, Object value);

    /**
     * 从右侧插入一个元素
     *
     * @param key   Redis键
     * @param value 要插入的值
     */
    void rPush(String key, Object value);

    /**
     * 从左侧批量插入多个元素
     *
     * @param key    Redis键
     * @param values 要插入的值集合
     */
    void lPushAll(String key, Collection<?> values);

    /**
     * 从右侧批量插入多个元素
     *
     * @param key    Redis键
     * @param values 要插入的值集合
     */
    void rPushAll(String key, Collection<?> values);

    /**
     * 弹出并返回左侧第一个元素
     *
     * @param key Redis键
     * @return 弹出的元素（Object 类型）
     */
    Object lPop(String key);

    /**
     * 弹出并返回左侧第一个元素，并转换为指定类型
     *
     * @param key   Redis键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 弹出的元素，已转换为指定类型
     */
    <T> T lPop(String key, Class<T> clazz);

    /**
     * 弹出并返回右侧第一个元素
     *
     * @param key Redis键
     * @return 弹出的元素（Object 类型）
     */
    Object rPop(String key);

    /**
     * 弹出并返回右侧第一个元素，并转换为指定类型
     *
     * @param key   Redis键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 弹出的元素，已转换为指定类型
     */
    <T> T rPop(String key, Class<T> clazz);

    /**
     * 获取列表指定区间的所有元素
     *
     * @param key   Redis键
     * @param start 起始索引
     * @param end   结束索引
     * @return 元素列表（Object 类型）
     */
    List<Object> lRange(String key, long start, long end);

    /**
     * 获取列表指定区间的所有元素，并反序列化为指定类型
     *
     * @param key   Redis键
     * @param start 起始索引
     * @param end   结束索引
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素列表（指定类型）
     */
    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    /**
     * 获取列表长度
     *
     * @param key Redis键
     * @return 列表长度
     */
    long lLen(String key);

    /**
     * 获取指定下标的元素
     *
     * @param key   Redis键
     * @param index 索引
     * @return 获取的元素（Object 类型）
     */
    Object lIndex(String key, long index);

    /**
     * 获取指定下标的元素，并转换为指定类型
     *
     * @param key   Redis键
     * @param index 索引
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 获取的元素（指定类型）
     */
    <T> T lIndex(String key, long index, Class<T> clazz);

    /**
     * 设置指定位置的元素值
     *
     * @param key   Redis键
     * @param index 位置索引
     * @param value 要设置的值
     */
    void lSet(String key, long index, Object value);

    /**
     * 裁剪列表，仅保留指定区间的元素
     *
     * @param key   Redis键
     * @param start 起始索引
     * @param end   结束索引
     */
    void lTrim(String key, long start, long end);

    /**
     * 移除列表中与指定值相等的元素
     *
     * @param key   Redis键
     * @param count 移除的个数（为 0 表示全部移除）
     * @param value 要移除的值
     * @return 实际移除的元素数量
     */
    long lRemove(String key, long count, Object value);

    /**
     * 阻塞方式弹出左侧元素（支持多个 key）
     *
     * @param timeout 超时时间（秒）
     * @param keys    Redis键数组
     * @return 弹出的元素封装（键值对）
     */
    Map.Entry<String, Object> blPop(int timeout, String... keys);

    /**
     * 阻塞方式弹出右侧元素（支持多个 key）
     *
     * @param timeout 超时时间（秒）
     * @param keys    Redis键数组
     * @return 弹出的元素封装（键值对）
     */
    Map.Entry<String, Object> brPop(int timeout, String... keys);

    // ---------------------------------- Set 操作 ----------------------------------

    /**
     * 向集合中添加一个或多个元素（无过期时间）。
     *
     * @param key    Redis 键
     * @param values 要添加的元素
     * @return 添加成功的元素数量
     */
    long sAdd(String key, Object... values);

    /**
     * 向集合中添加一个或多个元素，并设置过期时间。
     *
     * @param key     Redis 键
     * @param timeout 超时时间
     * @param unit    时间单位（如 TimeUnit.SECONDS）
     * @param values  要添加的元素
     * @return 添加成功的元素数量
     */
    long sAdd(String key, long timeout, TimeUnit unit, Object... values);

    /**
     * 获取集合中的所有元素。
     *
     * @param key Redis 键
     * @return 所有成员组成的 Set
     */
    Set<Object> sMembers(String key);

    /**
     * 获取集合中的所有元素，并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型的 Class
     * @param <T>   类型参数
     * @return 指定类型的 Set
     */
    <T> Set<T> sMembers(String key, Class<T> clazz);

    /**
     * 判断元素是否是集合中的成员。
     *
     * @param key   Redis 键
     * @param value 要判断的元素
     * @return true 表示存在；false 表示不存在
     */
    boolean sIsMember(String key, Object value);

    /**
     * 从集合中移除一个或多个元素。
     *
     * @param key    Redis 键
     * @param values 要移除的元素
     * @return 实际移除的元素个数
     */
    long sRemove(String key, Object... values);

    /**
     * 随机弹出并移除集合中的一个元素。
     *
     * @param key Redis 键
     * @return 被移除的元素
     */
    Object sPop(String key);

    /**
     * 随机弹出并移除集合中的一个元素，并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型的 Class
     * @param <T>   类型参数
     * @return 被移除的指定类型元素
     */
    <T> T sPop(String key, Class<T> clazz);

    /**
     * 获取集合中元素的数量。
     *
     * @param key Redis 键
     * @return 集合大小
     */
    long sSize(String key);

    /**
     * 获取两个集合的并集。
     *
     * @param key1 第一个集合
     * @param key2 第二个集合
     * @return 并集结果
     */
    Set<Object> sUnion(String key1, String key2);

    /**
     * 获取两个集合的并集，并反序列化为指定类型。
     *
     * @param key1  第一个集合的 Redis 键
     * @param key2  第二个集合的 Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 并集结果（指定类型的 Set）
     */
    <T> Set<T> sUnion(String key1, String key2, Class<T> clazz);

    /**
     * 获取两个集合的交集。
     *
     * @param key1 第一个集合
     * @param key2 第二个集合
     * @return 交集结果
     */
    Set<Object> sIntersect(String key1, String key2);

    /**
     * 获取两个集合的交集，并反序列化为指定类型。
     *
     * @param key1  第一个集合的 Redis 键
     * @param key2  第二个集合的 Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 交集结果（指定类型的 Set）
     */
    <T> Set<T> sIntersect(String key1, String key2, Class<T> clazz);

    /**
     * 获取两个集合的差集（key1 - key2）。
     *
     * @param key1 第一个集合
     * @param key2 第二个集合
     * @return 差集结果
     */
    Set<Object> sDifference(String key1, String key2);

    /**
     * 获取两个集合的差集（key1 - key2），并反序列化为指定类型。
     *
     * @param key1  第一个集合的 Redis 键
     * @param key2  第二个集合的 Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 差集结果（指定类型的 Set）
     */
    <T> Set<T> sDifference(String key1, String key2, Class<T> clazz);

    /**
     * 随机获取集合中的一个元素（不移除）。
     *
     * @param key Redis 键
     * @return 随机的元素
     */
    Object sRandMember(String key);

    /**
     * 随机获取集合中的一个元素（不移除），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 随机元素（指定类型）
     */
    <T> T sRandMember(String key, Class<T> clazz);

    /**
     * 随机获取集合中的多个元素（可能重复，且不移除）。
     *
     * @param key   Redis 键
     * @param count 获取的数量
     * @return 随机元素组成的列表
     */
    List<Object> sRandMember(String key, int count);

    /**
     * 随机获取集合中的多个元素（可能重复，且不移除），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param count 获取的数量
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 随机元素列表（指定类型）
     */
    <T> List<T> sRandMember(String key, int count, Class<T> clazz);

    /**
     * 将一个元素从 sourceKey 移动到 destKey。
     *
     * @param sourceKey 原集合 key
     * @param value     要移动的元素
     * @param destKey   目标集合 key
     * @return true 表示移动成功；false 表示元素不存在或移动失败
     */
    boolean sMove(String sourceKey, Object value, String destKey);

    // ---------------------------------- Set 操作 ----------------------------------

    /**
     * 向有序集合中添加一个元素及其分数，若元素已存在则更新分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param score 分数
     * @return 是否成功添加（新元素返回 true，更新分数返回 false）
     */
    boolean zAdd(String key, Object value, double score);

    /**
     * 批量向有序集合中添加多个元素及其分数。
     *
     * @param key           Redis 键
     * @param valueScoreMap 元素-分数映射
     * @return 添加成功的元素数量
     */
    long zAdd(String key, Map<Object, Double> valueScoreMap);

    /**
     * 按索引区间正序获取有序集合中的元素（分数从低到高）。
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引（-1 表示末尾）
     * @return 元素列表（Object 类型）
     */
    Set<Object> zRange(String key, long start, long end);

    /**
     * 按索引区间正序获取有序集合中的元素（分数从低到高），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引（-1 表示末尾）
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 指定类型的元素集合（有序）
     */
    <T> Set<T> zRange(String key, long start, long end, Class<T> clazz);

    /**
     * 按索引区间倒序获取有序集合中的元素（分数从高到低）。
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引
     * @return 元素列表（Object 类型）
     */
    Set<Object> zRevRange(String key, long start, long end);

    /**
     * 按索引区间倒序获取有序集合中的元素（分数从高到低），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 指定类型的元素集合（有序）
     */
    <T> Set<T> zRevRange(String key, long start, long end, Class<T> clazz);

    /**
     * 按分数区间获取有序集合中的元素（分数从低到高）。
     *
     * @param key Redis 键
     * @param min 最小分数（闭区间）
     * @param max 最大分数（闭区间）
     * @return 元素列表（Object 类型）
     */
    Set<Object> zRangeByScore(String key, double min, double max);

    /**
     * 按分数区间获取有序集合中的元素（分数从低到高），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param min   最小分数（闭区间）
     * @param max   最大分数（闭区间）
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 指定类型的元素集合（有序）
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, Class<T> clazz);

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
    Set<Object> zRangeByScore(String key, double min, double max, long offset, long count);

    /**
     * 按分数区间分页获取有序集合中的元素（分数从低到高），并反序列化为指定类型。
     *
     * @param key    Redis 键
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量
     * @param count  返回数量
     * @param clazz  目标类型 Class
     * @param <T>    泛型类型
     * @return 指定类型的元素集合（有序）
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, long offset, long count, Class<T> clazz);

    /**
     * 按分数区间倒序获取有序集合中的元素（分数从高到低）。
     *
     * @param key Redis 键
     * @param max 最大分数（闭区间）
     * @param min 最小分数（闭区间）
     * @return 元素列表（Object 类型）
     */
    Set<Object> zRevRangeByScore(String key, double max, double min);

    /**
     * 按分数区间倒序获取有序集合中的元素（分数从高到低），并反序列化为指定类型。
     *
     * @param key   Redis 键
     * @param max   最大分数（闭区间）
     * @param min   最小分数（闭区间）
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 指定类型的元素集合（有序）
     */
    <T> Set<T> zRevRangeByScore(String key, double max, double min, Class<T> clazz);

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
    Set<Object> zRevRangeByScore(String key, double max, double min, long offset, long count);

    /**
     * 按分数区间倒序分页获取有序集合中的元素（分数从高到低），并反序列化为指定类型。
     *
     * @param key    Redis 键
     * @param max    最大分数
     * @param min    最小分数
     * @param offset 偏移量
     * @param count  返回数量
     * @param clazz  目标类型 Class
     * @param <T>    泛型类型
     * @return 指定类型的元素集合（有序）
     */
    <T> Set<T> zRevRangeByScore(String key, double max, double min, long offset, long count, Class<T> clazz);

    /**
     * 获取元素在有序集合中的排名（按分数升序，从0开始）。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名，元素不存在返回 null
     */
    Long zRank(String key, Object value);

    /**
     * 获取元素在有序集合中的排名（按分数降序，从0开始）。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名，元素不存在返回 null
     */
    Long zRevRank(String key, Object value);

    /**
     * 删除有序集合中的一个或多个元素。
     *
     * @param key    Redis 键
     * @param values 要删除的元素
     * @return 实际删除的元素数量
     */
    long zRemove(String key, Object... values);

    /**
     * 获取有序集合中指定分数范围内的元素数量。
     *
     * @param key Redis 键
     * @param min 最小分数（闭区间）
     * @param max 最大分数（闭区间）
     * @return 元素数量
     */
    long zCount(String key, double min, double max);

    /**
     * 获取有序集合中指定元素的分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 分数，元素不存在返回 null
     */
    Double zScore(String key, Object value);

    /**
     * 给指定元素的分数增加 delta（可正可负）。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param delta 增量
     * @return 增加后的分数
     */
    Double zIncrScore(String key, Object value, double delta);

    /**
     * 获取有序集合中的元素数量。
     *
     * @param key Redis 键
     * @return 元素数量
     */
    long zSize(String key);

    /**
     * 使用 Scan 命令迭代匹配有序集合中的元素，适合大数据量扫描。
     *
     * @param key     Redis 键
     * @param pattern 匹配模式，支持通配符（如 *）
     * @return 匹配的元素集合（Object 类型）
     */
    Set<Object> zScan(String key, String pattern);

    /**
     * 使用 Scan 命令迭代匹配有序集合中的元素，反序列化为指定类型。
     *
     * @param key     Redis 键
     * @param pattern 匹配模式，支持通配符（如 *）
     * @param clazz   目标类型 Class
     * @param <T>     泛型类型
     * @return 匹配的指定类型元素集合
     */
    <T> Set<T> zScan(String key, String pattern, Class<T> clazz);

    // ---------------------------------- 高级功能 ----------------------------------

    // ------------------ 分布式锁 ------------------

    /**
     * 尝试获取分布式锁，设置锁的唯一标识和过期时间。
     *
     * @param key    锁的 Redis 键
     * @param value  锁的持有者标识（唯一）
     * @param expire 过期时间（秒）
     * @return 获取成功返回 true，失败返回 false
     */
    boolean tryLock(String key, String value, long expire);

    /**
     * 尝试获取分布式锁，支持灵活的过期时间单位。
     *
     * @param key     锁的 Redis 键
     * @param value   锁的持有者标识（唯一）
     * @param timeout 过期时间
     * @param unit    时间单位（如 TimeUnit.SECONDS）
     * @return 获取成功返回 true，失败返回 false
     */
    boolean tryLock(String key, String value, long timeout, TimeUnit unit);

    /**
     * 释放分布式锁，只有持有锁的客户端（value 匹配）才能释放。
     *
     * @param key           锁的 Redis 键
     * @param expectedValue 期望释放锁的持有者标识
     * @return 释放成功返回 true，失败返回 false
     */
    boolean releaseLock(String key, String expectedValue);

    /**
     * 尝试续期分布式锁，只有持有锁的客户端（value 匹配）才能续期。
     *
     * @param key           锁的 Redis 键
     * @param expectedValue 当前锁持有者标识
     * @param timeout       新的过期时间
     * @param unit          时间单位
     * @return 续期成功返回 true，否则 false
     */
    boolean renewLock(String key, String expectedValue, long timeout, TimeUnit unit);

    /**
     * 带重试机制的分布式锁，失败后等待指定时间再重试，最多重试次数限制。
     *
     * @param key        锁的 Redis 键
     * @param value      锁的持有者标识（唯一）
     * @param retryTimes 重试次数
     * @param waitMillis 重试等待时间（毫秒）
     * @return 获取成功返回 true，失败返回 false
     */
    boolean lockWithRetry(String key, String value, int retryTimes, long waitMillis);


    // ------------------ 计数器操作 ------------------

    /**
     * 对指定 key 执行原子递增操作，步长为 1。
     *
     * @param key Redis 键
     * @return 递增后的值
     */
    long incr(String key);

    /**
     * 对指定 key 执行原子递增操作，步长为指定的 delta。
     *
     * @param key   Redis 键
     * @param delta 增量值（必须大于 0）
     * @return 递增后的值
     */
    long incrBy(String key, long delta);

    /**
     * 对指定 key 执行原子递减操作，步长为 1。
     *
     * @param key Redis 键
     * @return 递减后的值
     */
    long decr(String key);

    /**
     * 对指定 key 执行原子递减操作，步长为指定的 delta。
     *
     * @param key   Redis 键
     * @param delta 减量值（必须大于 0）
     * @return 递减后的值
     */
    long decrBy(String key, long delta);

    /**
     * 对指定 key 执行原子递增操作，支持浮点数增量。
     *
     * @param key   Redis 键
     * @param delta 增量值（可以是小数）
     * @return 递增后的浮点数值
     */
    double incrByFloat(String key, double delta);

    /**
     * 对指定 key 执行原子递减操作，支持浮点数减量。
     *
     * @param key   Redis 键
     * @param delta 减量值（可以是小数）
     * @return 递减后的浮点数值
     */
    double decrByFloat(String key, double delta);

    // ------------------ 发布订阅 ------------------

    /**
     * 向指定频道发布消息。
     *
     * @param channel 频道名
     * @param message 消息内容
     */
    void publish(String channel, Object message);

    /**
     * 支持延迟发布消息（业务层配合实现延时机制）。
     *
     * @param channel     频道名
     * @param message     消息内容
     * @param delayMillis 延迟毫秒数
     */
    void publish(String channel, Object message, long delayMillis);

    /**
     * 异步发布消息，避免阻塞调用线程。
     *
     * @param channel 频道名
     * @param message 消息内容
     */
    void publishAsync(String channel, Object message);

    /**
     * 订阅指定频道的消息，listener 负责处理接收到的消息。
     *
     * @param channel  频道名
     * @param listener 消息监听器（实现 MessageListener 接口）
     */
    void subscribe(String channel, MessageListener listener);

    /**
     * 订阅指定频道消息，支持是否自动确认机制。
     *
     * @param channel  频道名
     * @param listener 消息监听器
     * @param autoAck  是否自动确认消息（业务相关）
     */
    void subscribe(String channel, MessageListener listener, boolean autoAck);

    /**
     * 订阅多个频道消息。
     *
     * @param channels 频道名数组
     * @param listener 消息监听器
     */
    void subscribeMultiple(String[] channels, MessageListener listener);

    /**
     * 取消订阅指定频道。
     *
     * @param channel  频道名
     * @param listener 消息监听器
     */
    void unsubscribe(String channel, MessageListener listener);

    /**
     * 取消该监听器的所有订阅。
     *
     * @param listener 消息监听器
     */
    void unsubscribeAll(MessageListener listener);

}
