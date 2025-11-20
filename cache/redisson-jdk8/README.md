# Redisson

Redisson 是一个基于 Redis 的 Java 客户端，提供了丰富的分布式数据结构和服务，如分布式锁、集合、队列、Map 等。它简化了与 Redis 的交互，并且支持高可用性、分布式事务、监控等特性，非常适合构建高性能和高可扩展性的应用。

- [官网链接](https://redisson.org)



## 基础配置

### 添加依赖

```xml
<!-- 项目属性 -->
<properties>
    <redisson.version>3.17.7</redisson.version>
</properties>
<!-- Redisson 依赖 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>${redisson.version}</version>
</dependency>
```

### 编辑配置文件

#### 单机配置

```yaml
---
# Redisson 的相关配置
redisson:
  config: |
    singleServerConfig:
      address: redis://192.168.1.10:42784
      password: Admin@123
      database: 41
      clientName: redisson-client
      connectionPoolSize: 64      # 最大连接数
      connectionMinimumIdleSize: 24 # 最小空闲连接
      idleConnectionTimeout: 10000 # 空闲连接超时时间（ms）
      connectTimeout: 5000        # 连接超时时间
      timeout: 3000               # 命令等待超时
      retryAttempts: 3            # 命令重试次数
      retryInterval: 1500         # 命令重试间隔（ms）
    threads: 16                   # 处理Redis事件的线程数
    nettyThreads: 32              # Netty线程数
    codec: !<org.redisson.codec.JsonJacksonCodec> {} # 推荐JSON序列化
```

#### 集群配置

```yaml
---
# Redisson 的相关配置
redisson:
  config: |
    clusterServersConfig:
      nodeAddresses:
        - "redis://192.168.1.41:6379"
        - "redis://192.168.1.42:6379"
        - "redis://192.168.1.43:6379"
        - "redis://192.168.1.44:6379"
        - "redis://192.168.1.45:6379"
        - "redis://192.168.1.46:6379"
      password: "Admin@123"       # 集群密码（如果集群有密码）
      scanInterval: 2000          # 集群状态扫描间隔（ms）
      readMode: "SLAVE"           # 读取模式（MASTER/SLAVE/MASTER_SLAVE）
      subscriptionMode: "SLAVE"  # 订阅模式（MASTER/SLAVE/MASTER_SLAVE）
      loadBalancer: !<org.redisson.connection.balancer.RoundRobinLoadBalancer> {} # 负载均衡策略
      masterConnectionPoolSize: 64      # 主节点连接池大小
      slaveConnectionPoolSize: 64       # 从节点连接池大小
      masterConnectionMinimumIdleSize: 24 # 主节点最小空闲连接
      slaveConnectionMinimumIdleSize: 24  # 从节点最小空闲连接
      idleConnectionTimeout: 10000      # 空闲连接超时时间（ms）
      connectTimeout: 5000              # 连接超时时间
      timeout: 3000                     # 命令等待超时
      retryAttempts: 3                  # 命令重试次数
      retryInterval: 1500               # 命令重试间隔（ms）
      failedSlaveReconnectionInterval: 3000 # 从节点重连间隔（ms）
      failedSlaveCheckInterval: 60000   # 从节点健康检查间隔（ms）
    threads: 16                         # 处理Redis事件的线程数
    nettyThreads: 32                    # Netty线程数
    codec: !<org.redisson.codec.JsonJacksonCodec> {} # 推荐JSON序列化
```

### 创建配置属性

```java
package local.ateng.java.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "redisson")
@Configuration
@Data
public class RedissonProperties {
    private String config;
}
```

### 创建客户端Bean

```java
package local.ateng.java.redisjdk8.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class RedissonConfig {
    private final RedissonProperties redissonProperties;

    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = Config.fromYAML(redissonProperties.getConfig());
        // config.setCodec(new CustomJacksonCodec());
        return Redisson.create(config);
    }

}
```

### 自定义序列化（可选）

#### 创建序列化器

```java
package local.ateng.java.redisjdk8.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.codec.JsonJacksonCodec;

public class CustomJacksonCodec extends JsonJacksonCodec {

    public CustomJacksonCodec() {
        super(createObjectMapper());
    }

    private static ObjectMapper createObjectMapper() {
        // 创建 ObjectMapper 实例，用于 JSON 序列化和反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 模块，支持 Java 8 日期时间类型（如 LocalDateTime、LocalDate）
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳，改为标准 ISO-8601 字符串格式（如 "2025-08-01T15:30:00"）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 启用默认类型信息，解决反序列化时无法恢复原始对象类型的问题（类似 fastjson 的 @type）
        objectMapper.activateDefaultTyping(
                // - 使用 LaissezFaireSubTypeValidator：一个宽松的子类型校验器
                LaissezFaireSubTypeValidator.instance,
                // - DefaultTyping.NON_FINAL：仅对非 final 类型（如 Object、List、Map、自定义类）启用类型信息
                ObjectMapper.DefaultTyping.NON_FINAL,
                // - JsonTypeInfo.As.PROPERTY：将类型信息作为 JSON 属性（字段）存储
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

}

```

#### 创建Bean

```java
package local.ateng.java.redisjdk8.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class RedissonConfig {
    private final RedissonProperties redissonProperties;

    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = Config.fromYAML(redissonProperties.getConfig());
        config.setCodec(new CustomJacksonCodec());
        return Redisson.create(config);
    }

}
```



## 创建Redisson Service

### 创建Service接口

```java
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

    /**
     * 获取 RedissonClient 实例。
     *
     * @return RedissonClient
     */
    RedissonClient getClient();
    
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
     * 将元素添加到指定队列尾部（阻塞方式，队列满时无限等待）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param value    要入队的元素
     * @param <T>      元素类型
     * @throws InterruptedException 阻塞等待时被中断异常
     */
    <T> void enqueueBlocking(String queueKey, T value) throws InterruptedException;

    /**
     * 将元素添加到指定队列尾部（阻塞方式，队列满时等待超时）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param value    要入队的元素
     * @param timeout  最大等待时间
     * @param timeUnit 时间单位
     * @param <T>      元素类型
     * @return 是否成功入队，超时返回 false
     * @throws InterruptedException 阻塞等待时被中断异常
     */
    <T> boolean enqueueBlocking(String queueKey, T value, long timeout, TimeUnit timeUnit) throws InterruptedException;

    /**
     * 将元素添加到指定队列尾部（非阻塞方式）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param value    要入队的元素
     * @param <T>      元素类型
     * @return 是否成功入队，失败可能是队列已满
     */
    <T> boolean enqueue(String queueKey, T value);

    /**
     * 从指定队列头部获取并移除元素（阻塞方式，队列为空时等待）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param timeout  最大等待时间，单位秒
     * @param <T>      元素类型
     * @return 队头元素，超时返回 null
     * @throws InterruptedException 阻塞等待时被中断异常
     */
    <T> T dequeueBlocking(String queueKey, long timeout) throws InterruptedException;

    /**
     * 从指定队列头部获取并移除元素（非阻塞方式）。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param <T>      元素类型
     * @return 队头元素，若队列为空返回 null
     */
    <T> T dequeue(String queueKey);

    /**
     * 获取队列长度。
     *
     * @param queueKey 队列对应的 Redis 键
     * @return 当前队列长度
     */
    long queueSize(String queueKey);

    // --------------------- 延迟队列操作 ---------------------

    /**
     * 将元素添加到延迟队列，延迟指定时间后才能被消费。
     *
     * @param queueKey 队列对应的 Redis 键
     * @param value    要入队的元素
     * @param delay    延迟时间
     * @param timeUnit 时间单位
     * @param <T>      元素类型
     */
    <T> void enqueueDelayed(String queueKey, T value, long delay, TimeUnit timeUnit);

    // --------------------- 队列辅助操作 ---------------------

    /**
     * 清空队列中的所有元素
     *
     * @param queueKey 队列对应的 Redis 键
     */
    void clearQueue(String queueKey);

    /**
     * 判断队列是否为空
     *
     * @param queueKey 队列对应的 Redis 键
     * @return true 如果队列为空
     */
    boolean isQueueEmpty(String queueKey);

    /**
     * 移除队列中指定元素
     *
     * @param queueKey 队列对应的 Redis 键
     * @param value    要移除的元素
     * @return true 如果移除成功
     */
    boolean removeFromQueue(String queueKey, Object value);

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

```

### 创建Service实现

```java
package local.ateng.java.redisjdk8.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.redisjdk8.service.RedissonService;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    /**
     * 获取 RedissonClient 实例。
     *
     * @return RedissonClient
     */
    @Override
    public RedissonClient getClient() {
        return this.redissonClient;
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
        RBucket<Object> bucket = redissonClient.getBucket(key);
        Object value = bucket.get();
        return convertValue(value, clazz);
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
        RBucket<Object> bucket = redissonClient.getBucket(key);
        Object value = bucket.get();
        return convertValue(value, typeReference);
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
        return convertValue(oldValue, clazz);
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
        return convertValue(oldValue, typeReference);
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

    /**
     * 批量获取多个字符串 key 对应的值
     *
     * @param keys Redis 键列表
     * @return 包含 key 和对应 value 的 Map，不存在的 key 不会出现在结果中
     */
    @Override
    public Map<String, Object> entries(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyMap();
        }
        return redissonClient.getBuckets().get(keys.toArray(new String[0]));
    }


    /**
     * 批量获取多个字符串 key 的值，并将其转换为指定类型
     *
     * @param keys  Redis 键列表
     * @param clazz 目标类型（如 User.class）
     * @param <T>   返回值泛型类型
     * @return 包含 key 和对应类型化 value 的 Map，不存在的 key 不会出现在结果中
     */
    @Override
    public <T> Map<String, T> entries(Collection<String> keys, Class<T> clazz) {
        if (CollectionUtils.isEmpty(keys) || clazz == null) {
            return Collections.emptyMap();
        }
        // 复用 entries(Collection) 方法
        Map<String, Object> rawMap = entries(keys);
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            // 复用转换方法
            T value = convertValue(entry.getValue(), clazz);
            if (value != null) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * 批量获取多个字符串 key 的值，并将其转换为指定复杂类型（支持泛型）
     *
     * @param keys          Redis 键列表
     * @param typeReference 类型引用（如 new TypeReference<List<User>>() {}）
     * @param <T>           返回值泛型类型
     * @return 包含 key 和对应类型化 value 的 Map，不存在的 key 不会出现在结果中
     */
    @Override
    public <T> Map<String, T> entries(Collection<String> keys, TypeReference<T> typeReference) {
        if (CollectionUtils.isEmpty(keys) || typeReference == null) {
            return Collections.emptyMap();
        }
        // 复用 entries(Collection) 方法
        Map<String, Object> rawMap = entries(keys);
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            // 复用转换方法
            T value = convertValue(entry.getValue(), typeReference);
            if (value != null) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    // -------------------------- 哈希（Hash）操作 --------------------------

    /**
     * 设置哈希字段值
     *
     * @param key   Redis 键
     * @param field 哈希字段名
     * @param value 要存储的对象（会自动序列化）
     */
    @Override
    public void hPut(String key, String field, Object value) {
        RMap<String, Object> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    /**
     * 获取哈希字段值
     *
     * @param key   Redis 键
     * @param field 哈希字段名
     * @param clazz 返回类型
     * @param <T>   类型泛型
     * @return 字段对应的值，若不存在返回 null
     */
    @Override
    public <T> T hGet(String key, String field, Class<T> clazz) {
        RMap<String, Object> map = redissonClient.getMap(key);
        Object value = map.get(field);
        return convertValue(value, clazz);
    }

    /**
     * 获取哈希字段值（支持复杂泛型类型）
     *
     * @param key           Redis 键
     * @param field         哈希字段名
     * @param typeReference 返回类型引用（支持泛型）
     * @param <T>           类型泛型
     * @return 字段对应的值，若不存在返回 null
     */
    @Override
    public <T> T hGet(String key, String field, TypeReference<T> typeReference) {
        RMap<String, Object> map = redissonClient.getMap(key);
        Object value = map.get(field);
        return convertValue(value, typeReference);
    }

    /**
     * 删除一个或多个哈希字段
     *
     * @param key    Redis 键
     * @param fields 要删除的字段名，可多个
     */
    @Override
    public void hDelete(String key, String... fields) {
        RMap<String, Object> map = redissonClient.getMap(key);
        if (fields != null && fields.length > 0) {
            map.fastRemove(fields);
        }
    }

    /**
     * 判断哈希中是否存在指定字段
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 若存在返回 true，否则返回 false
     */
    @Override
    public boolean hHasKey(String key, String field) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.containsKey(field);
    }

    /**
     * 获取哈希表中所有字段与值
     *
     * @param key Redis 键
     * @return 包含所有字段及其值的 Map
     */
    @Override
    public Map<String, Object> hEntries(String key) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.readAllMap();
    }

    /**
     * 获取哈希表中所有字段与值，并转换为指定类型的 Map
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 包含所有字段及其值的 Map，值均转换为指定类型，若 key 不存在返回空 Map
     */
    @Override
    public <T> Map<String, T> hEntries(String key, Class<T> clazz) {
        RMap<String, Object> map = redissonClient.getMap(key);
        Map<String, Object> rawMap = map.readAllMap();
        if (rawMap == null || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            T converted = convertValue(entry.getValue(), clazz);
            result.put(entry.getKey(), converted);
        }
        return result;
    }

    /**
     * 获取哈希表中所有字段与值，并转换为指定泛型类型的 Map
     *
     * @param key           Redis 键
     * @param typeReference 目标类型引用（支持泛型）
     * @param <T>           目标类型泛型
     * @return 包含所有字段及其值的 Map，值均转换为指定类型，若 key 不存在返回空 Map
     */
    @Override
    public <T> Map<String, T> hEntries(String key, TypeReference<T> typeReference) {
        RMap<String, Object> map = redissonClient.getMap(key);
        Map<String, Object> rawMap = map.readAllMap();
        if (rawMap == null || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            T converted = convertValue(entry.getValue(), typeReference);
            result.put(entry.getKey(), converted);
        }
        return result;
    }

    /**
     * 获取哈希表中所有字段名
     *
     * @param key Redis 键
     * @return 所有字段名组成的 Set
     */
    @Override
    public Set<String> hKeys(String key) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.keySet();
    }

    /**
     * 获取哈希表中所有字段值
     *
     * @param key Redis 键
     * @return 所有字段值组成的集合
     */
    @Override
    public Collection<Object> hValues(String key) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.values();
    }

    /**
     * 获取哈希表中所有字段值，并转换为指定类型集合
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 所有字段值组成的指定类型集合，若转换失败则对应元素为 null
     */
    @Override
    public <T> Collection<T> hValues(String key, Class<T> clazz) {
        RMap<String, Object> map = redissonClient.getMap(key);
        Collection<Object> values = map.values();
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(values.size());
        for (Object value : values) {
            T converted = convertValue(value, clazz);
            result.add(converted);
        }
        return result;
    }

    /**
     * 获取哈希表中所有字段值，并转换为指定泛型集合
     *
     * @param key           Redis 键
     * @param typeReference 目标类型引用（支持泛型）
     * @param <T>           泛型类型
     * @return 所有字段值组成的指定类型集合，若转换失败则对应元素为 null
     */
    @Override
    public <T> Collection<T> hValues(String key, TypeReference<T> typeReference) {
        RMap<String, Object> map = redissonClient.getMap(key);
        Collection<Object> values = map.values();
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(values.size());
        for (Object value : values) {
            T converted = convertValue(value, typeReference);
            result.add(converted);
        }
        return result;
    }

    /**
     * 获取哈希字段数量
     *
     * @param key Redis 键
     * @return 字段个数
     */
    @Override
    public int hSize(String key) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.size();
    }

    /**
     * 清空哈希表（删除所有字段）
     *
     * @param key Redis 键
     */
    @Override
    public void hClear(String key) {
        RMap<String, Object> map = redissonClient.getMap(key);
        map.clear();
    }

    // -------------------------- 列表（List）操作 --------------------------

    /**
     * 将元素添加到列表右端（尾部）
     *
     * @param key   Redis 键
     * @param value 要添加的元素
     */
    @Override
    public void lRightPush(String key, Object value) {
        RList<Object> list = redissonClient.getList(key);
        list.add(value);
    }

    /**
     * 将多个元素添加到列表右端（尾部）
     *
     * @param key    Redis 键
     * @param values 要添加的多个元素
     */
    @Override
    public void lRightPushAll(String key, Collection<?> values) {
        RList<Object> list = redissonClient.getList(key);
        if (values != null && !values.isEmpty()) {
            list.addAll(values);
        }
    }

    /**
     * 从列表左端弹出元素
     *
     * @param key Redis 键
     * @return 弹出的元素，若列表为空或不存在返回 null
     */
    @Override
    public Object lLeftPop(String key) {
        RDeque<Object> deque = redissonClient.getDeque(key);
        return deque.pollFirst();
    }

    /**
     * 从列表左端弹出元素，并转换为指定类型
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 弹出的元素，若列表为空或不存在返回 null
     */
    @Override
    public <T> T lLeftPop(String key, Class<T> clazz) {
        Object value = lLeftPop(key);
        return convertValue(value, clazz);
    }

    /**
     * 获取列表指定范围内的元素（包含 start 和 end）
     *
     * @param key   Redis 键
     * @param start 起始索引（0-based）
     * @param end   结束索引（-1 表示最后一个元素）
     * @return 元素集合，若列表不存在返回空集合
     */
    @Override
    public List<Object> lRange(String key, long start, long end) {
        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        int fromIndex = (int) (start < 0 ? size + start : start);
        int toIndex = (int) (end < 0 ? size + end : end);
        fromIndex = Math.max(0, fromIndex);
        toIndex = Math.min(size - 1, toIndex);
        if (fromIndex > toIndex) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list.subList(fromIndex, toIndex + 1));
    }

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
    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        List<Object> rawList = lRange(key, start, end);
        if (rawList.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(rawList.size());
        for (Object obj : rawList) {
            result.add(convertValue(obj, clazz));
        }
        return result;
    }

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
    @Override
    public <T> List<T> lRange(String key, long start, long end, TypeReference<T> typeReference) {
        List<Object> rawList = lRange(key, start, end);
        if (rawList.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(rawList.size());
        for (Object obj : rawList) {
            result.add(convertValue(obj, typeReference));
        }
        return result;
    }

    /**
     * 获取列表长度
     *
     * @param key Redis 键
     * @return 列表长度，若不存在返回 0
     */
    @Override
    public long lSize(String key) {
        RList<Object> list = redissonClient.getList(key);
        return list.size();
    }

    /**
     * 删除列表中等于 value 的元素，count 指定删除数量
     *
     * @param key   Redis 键
     * @param count 删除数量（>0 从头开始删除，<0 从尾开始删除，=0 删除所有）
     * @param value 要删除的元素
     * @return 删除的元素数量
     */
    @Override
    public long lRemove(String key, long count, Object value) {
        RList<Object> list = redissonClient.getList(key);
        if (count == 0) {
            long removed = 0;
            while (list.remove(value)) {
                removed++;
            }
            return removed;
        } else if (count > 0) {
            long removed = 0;
            Iterator<Object> it = list.iterator();
            while (it.hasNext() && removed < count) {
                if (Objects.equals(it.next(), value)) {
                    it.remove();
                    removed++;
                }
            }
            return removed;
        } else {
            long removed = 0;
            List<Object> copy = new ArrayList<>(list);
            ListIterator<Object> it = copy.listIterator(copy.size());
            while (it.hasPrevious() && removed < -count) {
                if (Objects.equals(it.previous(), value)) {
                    it.remove();
                    removed++;
                }
            }
            list.clear();
            list.addAll(copy);
            return removed;
        }
    }

    /**
     * 获取列表中指定索引的元素
     *
     * @param key   Redis 键
     * @param index 索引位置（0-based，负数从尾部计数）
     * @return 元素，若索引不存在返回 null
     */
    @Override
    public Object lIndex(String key, long index) {
        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        if (size == 0) {
            return null;
        }
        int idx = (int) (index < 0 ? size + index : index);
        if (idx < 0 || idx >= size) {
            return null;
        }
        return list.get(idx);
    }

    /**
     * 获取列表中指定索引的元素，并转换为指定类型
     *
     * @param key   Redis 键
     * @param index 索引位置
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素，若索引不存在返回 null
     */
    @Override
    public <T> T lIndex(String key, long index, Class<T> clazz) {
        Object value = lIndex(key, index);
        return convertValue(value, clazz);
    }

    /**
     * 根据索引修改列表元素的值
     *
     * @param key   Redis 键
     * @param index 索引位置
     * @param value 新值
     * @throws IndexOutOfBoundsException 索引超出列表范围时抛出
     */
    @Override
    public void lSet(String key, long index, Object value) {
        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        int idx = (int) (index < 0 ? size + index : index);
        if (idx < 0 || idx >= size) {
            throw new IndexOutOfBoundsException("索引 " + index + " 超出列表范围");
        }
        list.set(idx, value);
    }

    /**
     * 清空整个列表
     *
     * @param key Redis 键
     */
    @Override
    public void lClear(String key) {
        RList<Object> list = redissonClient.getList(key);
        list.clear();
    }

    // -------------------------- 集合（Set）操作 --------------------------

    /**
     * 添加一个或多个元素到集合中（去重）
     *
     * @param key   Redis 键
     * @param value 元素，可传多个
     * @return 实际添加成功的元素数量（已存在的元素不会重复添加）
     */
    @Override
    public boolean sAdd(String key, Object... value) {
        if (key == null || value == null || value.length == 0) {
            return false;
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.addAll(Arrays.asList(value));
    }

    /**
     * 添加多个元素到集合中（去重）
     *
     * @param key   Redis 键
     * @param value 元素，多个
     * @return 实际添加成功的元素数量（已存在的元素不会重复添加）
     */
    @Override
    public boolean sAdd(String key, List<Object> value) {
        if (key == null || value == null || value.isEmpty()) {
            return false;
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.addAll(value);
    }

    /**
     * 判断集合中是否存在指定元素
     *
     * @param key   Redis 键
     * @param value 要判断的元素
     * @return true 存在，false 不存在
     */
    @Override
    public boolean sIsMember(String key, Object value) {
        if (key == null || value == null) {
            return false;
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.contains(value);
    }

    /**
     * 获取集合中的所有元素
     *
     * @param key Redis 键
     * @return 元素集合（无序去重）
     */
    @Override
    public Set<Object> sMembers(String key) {
        if (key == null) {
            return Collections.emptySet();
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.readAll();
    }

    /**
     * 获取集合中的所有元素并转换为指定类型
     *
     * @param key   Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 元素集合（无序去重）
     */
    @Override
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        Set<Object> members = sMembers(key);
        if (members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> result = new HashSet<>(members.size());
        for (Object member : members) {
            T converted = convertValue(member, clazz);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * 获取集合中的所有元素并转换为指定类型（支持复杂泛型结构）
     *
     * @param key           Redis 键
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 元素集合（无序去重）
     */
    @Override
    public <T> Set<T> sMembers(String key, TypeReference<T> typeReference) {
        Set<Object> members = sMembers(key);
        if (members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> result = new HashSet<>(members.size());
        for (Object member : members) {
            T converted = convertValue(member, typeReference);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * 获取集合中元素的数量
     *
     * @param key Redis 键
     * @return 集合大小（元素个数）
     */
    @Override
    public long sSize(String key) {
        if (key == null) {
            return 0L;
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.size();
    }

    /**
     * 从集合中随机弹出一个元素
     *
     * @param key Redis 键
     * @return 被移除的元素，若集合为空则返回 null
     */
    @Override
    public Object sPop(String key) {
        if (key == null) {
            return null;
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.removeRandom();
    }

    /**
     * 从集合中随机弹出一个元素并转换为指定类型
     *
     * @param key   Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 被移除并转换后的元素，若集合为空则返回 null
     */
    @Override
    public <T> T sPop(String key, Class<T> clazz) {
        Object popped = sPop(key);
        return convertValue(popped, clazz);
    }

    /**
     * 从集合中随机弹出一个元素并转换为指定类型（支持复杂泛型结构）
     *
     * @param key           Redis 键
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 被移除并转换后的元素，若集合为空则返回 null
     */
    @Override
    public <T> T sPop(String key, TypeReference<T> typeReference) {
        Object popped = sPop(key);
        return convertValue(popped, typeReference);
    }

    /**
     * 从集合中移除一个或多个元素
     *
     * @param key    Redis 键
     * @param values 要移除的元素
     * @return 实际移除的元素数量
     */
    @Override
    public boolean sRemove(String key, Object... values) {
        if (key == null || values == null || values.length == 0) {
            return false;
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.removeAll(Arrays.asList(values));
    }

    /**
     * 从集合中移除多个元素
     *
     * @param key    Redis 键
     * @param values 要移除的元素
     * @return 实际移除的元素数量
     */
    @Override
    public boolean sRemove(String key, List<Object> values) {
        if (key == null || values == null || values.isEmpty()) {
            return false;
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.removeAll(values);
    }

    /**
     * 随机获取集合中的一个元素（不移除）
     *
     * @param key Redis 键
     * @return 随机元素，若集合为空则返回 null
     */
    @Override
    public Object sRandomMember(String key) {
        if (key == null) {
            return null;
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.random();
    }

    /**
     * 随机获取集合中的一个元素并转换为指定类型（不移除）
     *
     * @param key   Redis 键
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 转换后的随机元素，若集合为空则返回 null
     */
    @Override
    public <T> T sRandomMember(String key, Class<T> clazz) {
        Object randomMember = sRandomMember(key);
        return convertValue(randomMember, clazz);
    }

    /**
     * 随机获取集合中的一个元素并转换为指定类型（不移除，支持复杂泛型结构）
     *
     * @param key           Redis 键
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 转换后的随机元素，若集合为空则返回 null
     */
    @Override
    public <T> T sRandomMember(String key, TypeReference<T> typeReference) {
        Object randomMember = sRandomMember(key);
        return convertValue(randomMember, typeReference);
    }

    /**
     * 获取集合中的多个随机元素
     *
     * @param key   Redis 键
     * @param count 获取的元素数量
     * @return 随机元素集合（数量可能小于 count）
     */
    @Override
    public Set<Object> sRandomMembers(String key, int count) {
        if (key == null || count <= 0) {
            return Collections.emptySet();
        }
        RSet<Object> rSet = redissonClient.getSet(key);
        return rSet.random(count);
    }

    /**
     * 获取集合中的多个随机元素并转换为指定类型
     *
     * @param key   Redis 键
     * @param count 获取的元素数量
     * @param clazz 目标类型 Class
     * @param <T>   泛型类型
     * @return 转换后的随机元素集合（数量可能小于 count）
     */
    @Override
    public <T> Set<T> sRandomMembers(String key, int count, Class<T> clazz) {
        Set<Object> randoms = sRandomMembers(key, count);
        if (randoms.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> result = new HashSet<>(randoms.size());
        for (Object obj : randoms) {
            T converted = convertValue(obj, clazz);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * 获取集合中的多个随机元素并转换为指定类型（支持复杂泛型结构）
     *
     * @param key           Redis 键
     * @param count         获取的元素数量
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 转换后的随机元素集合（数量可能小于 count）
     */
    @Override
    public <T> Set<T> sRandomMembers(String key, int count, TypeReference<T> typeReference) {
        Set<Object> randoms = sRandomMembers(key, count);
        if (randoms.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> result = new HashSet<>(randoms.size());
        for (Object obj : randoms) {
            T converted = convertValue(obj, typeReference);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * 获取两个集合的并集（不改变原集合）
     *
     * @param key1 第一个 Redis 键
     * @param key2 第二个 Redis 键
     * @return 两个集合的并集（去重）
     */
    @Override
    public Set<Object> sUnion(String key1, String key2) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        return set1.readUnion(set2.getName());
    }

    /**
     * 获取两个集合的并集（不改变原集合），并转换为指定类型
     *
     * @param key1  第一个 Redis 键
     * @param key2  第二个 Redis 键
     * @param clazz 返回元素的类型 Class
     * @param <T>   元素泛型类型
     * @return 并集结果集合（去重）并转换为指定类型
     */
    @Override
    public <T> Set<T> sUnion(String key1, String key2, Class<T> clazz) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        Set<Object> raw = set1.readUnion(set2.getName());
        return raw.stream()
                .map(obj -> convertValue(obj, clazz))
                .collect(Collectors.toSet());
    }

    /**
     * 获取两个集合的并集（不改变原集合），并转换为指定复杂类型
     *
     * @param key1          第一个 Redis 键
     * @param key2          第二个 Redis 键
     * @param typeReference 返回元素的类型 TypeReference（支持复杂类型）
     * @param <T>           元素泛型类型
     * @return 并集结果集合（去重）并转换为指定类型
     */
    @Override
    public <T> Set<T> sUnion(String key1, String key2, TypeReference<T> typeReference) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        Set<Object> raw = set1.readUnion(set2.getName());
        return raw.stream()
                .map(obj -> convertValue(obj, typeReference))
                .collect(Collectors.toSet());
    }

    /**
     * 获取两个集合的交集（不改变原集合）
     *
     * @param key1 第一个 Redis 键
     * @param key2 第二个 Redis 键
     * @return 两个集合的交集，若任一 key 为空或 null，则返回空集合
     */
    @Override
    public Set<Object> sIntersect(String key1, String key2) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        return set1.readIntersection(set2.getName());
    }

    /**
     * 获取两个集合的交集（不改变原集合），并转换为指定类型
     *
     * @param key1  第一个 Redis 键
     * @param key2  第二个 Redis 键
     * @param clazz 返回元素的类型 Class
     * @param <T>   元素泛型类型
     * @return 交集结果集合并转换为指定类型，若任一 key 为空或 null，则返回空集合
     */
    @Override
    public <T> Set<T> sIntersect(String key1, String key2, Class<T> clazz) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        Set<Object> raw = set1.readIntersection(set2.getName());
        return raw.stream()
                .map(obj -> convertValue(obj, clazz))
                .collect(Collectors.toSet());
    }

    /**
     * 获取两个集合的交集（不改变原集合），并转换为指定复杂类型
     *
     * @param key1          第一个 Redis 键
     * @param key2          第二个 Redis 键
     * @param typeReference 返回元素的类型 TypeReference（支持复杂类型）
     * @param <T>           元素泛型类型
     * @return 交集结果集合并转换为指定类型，若任一 key 为空或 null，则返回空集合
     */
    @Override
    public <T> Set<T> sIntersect(String key1, String key2, TypeReference<T> typeReference) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        Set<Object> raw = set1.readIntersection(set2.getName());
        return raw.stream()
                .map(obj -> convertValue(obj, typeReference))
                .collect(Collectors.toSet());
    }

    /**
     * 获取两个集合的差集（key1 相对于 key2 的差）
     *
     * @param key1 第一个 Redis 键（原始集合）
     * @param key2 第二个 Redis 键（要排除的集合）
     * @return 差集结果（存在于 key1 而不存在于 key2 的元素），若任一 key 为空或 null，则返回空集合
     */
    @Override
    public Set<Object> sDifference(String key1, String key2) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        return set1.readDiff(set2.getName());
    }

    /**
     * 获取两个集合的差集（key1 相对于 key2 的差），并转换为指定类型
     *
     * @param key1  第一个 Redis 键（原始集合）
     * @param key2  第二个 Redis 键（要排除的集合）
     * @param clazz 返回元素的类型 Class
     * @param <T>   元素泛型类型
     * @return 差集结果集合并转换为指定类型，若任一 key 为空或 null，则返回空集合
     */
    @Override
    public <T> Set<T> sDifference(String key1, String key2, Class<T> clazz) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        Set<Object> raw = set1.readDiff(set2.getName());
        return raw.stream()
                .map(obj -> convertValue(obj, clazz))
                .collect(Collectors.toSet());
    }

    /**
     * 获取两个集合的差集（key1 相对于 key2 的差），并转换为指定复杂类型
     *
     * @param key1          第一个 Redis 键（原始集合）
     * @param key2          第二个 Redis 键（要排除的集合）
     * @param typeReference 返回元素的类型 TypeReference（支持复杂类型）
     * @param <T>           元素泛型类型
     * @return 差集结果集合并转换为指定类型，若任一 key 为空或 null，则返回空集合
     */
    @Override
    public <T> Set<T> sDifference(String key1, String key2, TypeReference<T> typeReference) {
        if (key1 == null || key1.isEmpty() || key2 == null || key2.isEmpty()) {
            return Collections.emptySet();
        }
        RSet<Object> set1 = redissonClient.getSet(key1);
        RSet<Object> set2 = redissonClient.getSet(key2);
        Set<Object> raw = set1.readDiff(set2.getName());
        return raw.stream()
                .map(obj -> convertValue(obj, typeReference))
                .collect(Collectors.toSet());
    }

    // -------------------------- 有序集合（ZSet / SortedSet）操作 --------------------------

    /**
     * 添加一个元素及其分数到有序集合中。
     *
     * @param key   有序集合的 key
     * @param value 要添加的元素
     * @param score 元素的分数（用于排序）
     * @return 是否添加成功，若元素已存在则更新分数
     */
    @Override
    public boolean zAdd(String key, Object value, double score) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        return scoredSortedSet.add(score, value);
    }

    /**
     * 批量添加元素及其分数到有序集合中。
     *
     * @param key      有序集合的 key
     * @param scoreMap 元素与对应分数的映射
     * @return 成功添加的元素数量（不包括更新）
     */
    @Override
    public int zAddAll(String key, Map<Object, Double> scoreMap) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        int count = 0;
        for (Map.Entry<Object, Double> entry : scoreMap.entrySet()) {
            boolean added = scoredSortedSet.add(entry.getValue(), entry.getKey());
            if (added) {
                count++;
            }
        }
        return count;
    }

    /**
     * 从有序集合中移除指定元素。
     *
     * @param key    有序集合的 key
     * @param values 要移除的元素列表
     * @return 实际移除的元素数量
     */
    @Override
    public boolean zRemove(String key, Object... values) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        return scoredSortedSet.removeAll(Arrays.asList(values));
    }

    /**
     * 从有序集合中移除指定元素。
     *
     * @param key    有序集合的 key
     * @param values 要移除的元素列表
     * @return 实际移除的元素数量
     */
    @Override
    public boolean zRemove(String key, List<Object> values) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        return scoredSortedSet.removeAll(values);
    }

    /**
     * 获取有序集合中某个元素的分数（score）。
     * <p>
     * 如果元素不存在于集合中，则返回 {@code null}。
     * </p>
     *
     * @param key   有序集合的 Redis key，不可为空或空字符串
     * @param value 指定的元素对象，不可为 {@code null}
     * @return 元素的分数（score），若元素不存在或参数非法则返回 {@code null}
     */
    @Override
    public Double zScore(String key, Object value) {
        if (ObjectUtils.isEmpty(key) || value == null) {
            return null;
        }
        RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
        return sortedSet.getScore(value);
    }

    /**
     * 获取有序集合中指定元素的排名（按分数升序）。
     * <p>
     * 分数越小，排名越靠前，排名从 0 开始计数。<br>
     * 如果元素不存在于集合中，则返回 {@code null}。
     * </p>
     *
     * @param key   有序集合的 Redis key，不可为空或空字符串
     * @param value 指定的元素对象，不可为 {@code null}
     * @return 元素的升序排名（从 0 开始），若元素不存在或参数非法则返回 {@code null}
     */
    @Override
    public Integer zRank(String key, Object value) {
        if (ObjectUtils.isEmpty(key) || value == null) {
            return null;
        }
        RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
        return sortedSet.rank(value);
    }

    /**
     * 获取有序集合中指定元素的排名（按分数降序）。
     * <p>
     * 分数越大，排名越靠前，排名从 0 开始计数。<br>
     * 如果元素不存在于集合中，则返回 {@code null}。
     * </p>
     *
     * @param key   有序集合的 Redis key，不可为空或空字符串
     * @param value 指定的元素对象，不可为 {@code null}
     * @return 元素的降序排名（从 0 开始），若元素不存在或参数非法则返回 {@code null}
     */
    @Override
    public Integer zRevRank(String key, Object value) {
        if (ObjectUtils.isEmpty(key) || value == null) {
            return null;
        }
        RScoredSortedSet<Object> sortedSet = redissonClient.getScoredSortedSet(key);
        return sortedSet.revRank(value);
    }

    /**
     * 获取有序集合中指定分数区间内的元素（按升序）。
     *
     * @param key 有序集合的 key
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 区间内的所有元素，按分数升序排列，元素为原始对象
     */
    @Override
    public Set<Object> zRangeByScore(String key, double min, double max) {
        // 判断 key 是否为空或空白
        if (key == null || key.trim().isEmpty()) {
            return Collections.emptySet();
        }

        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);

        Collection<Object> values = scoredSortedSet.valueRange(min, true, max, true);

        return values == null ? Collections.emptySet() : new LinkedHashSet<>(values);
    }

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
    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, Class<T> clazz) {
        // 判断 key 或 clazz 是否为空
        if (key == null || key.trim().isEmpty() || clazz == null) {
            return Collections.emptySet();
        }

        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<Object> rawValues = scoredSortedSet.valueRange(min, true, max, true);

        // 定义结果集合
        Set<T> result = new LinkedHashSet<>();
        for (Object value : rawValues) {
            result.add(convertValue(value, clazz));
        }

        return result;
    }

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
    @Override
    public <T> Set<T> zRangeByScore(String key, double min, double max, TypeReference<T> typeReference) {
        // 判断 key 或 typeReference 是否为空
        if (key == null || key.trim().isEmpty() || typeReference == null) {
            return Collections.emptySet();
        }

        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<Object> rawValues = scoredSortedSet.valueRange(min, true, max, true);

        // 定义结果集合
        Set<T> result = new LinkedHashSet<>();
        for (Object value : rawValues) {
            result.add(convertValue(value, typeReference));
        }

        return result;
    }

    /**
     * 获取有序集合中指定分数区间内的元素及其分数（按升序）
     *
     * @param key 有序集合的 key
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 区间内元素及其分数的 Map，按分数升序排列
     */
    @Override
    public Map<Object, Double> zRangeByScoreWithScores(String key, double min, double max) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<ScoredEntry<Object>> entries = scoredSortedSet.entryRange(min, true, max, true);
        Map<Object, Double> result = new LinkedHashMap<>();
        for (ScoredEntry<Object> entry : entries) {
            result.put(entry.getValue(), entry.getScore());
        }
        return result;
    }

    /**
     * 获取有序集合中指定分数区间内的元素及其分数（按降序）
     *
     * @param key 有序集合的 key
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 区间内元素及其分数的 Map，按分数降序排列
     */
    @Override
    public Map<Object, Double> zRevRangeByScoreWithScores(String key, double min, double max) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<ScoredEntry<Object>> entries = scoredSortedSet.entryRangeReversed(max, true, min, true);
        Map<Object, Double> result = new LinkedHashMap<>();
        for (ScoredEntry<Object> entry : entries) {
            result.put(entry.getValue(), entry.getScore());
        }
        return result;
    }

    /**
     * 获取有序集合中指定排名区间的元素（按分数升序）
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 指定排名区间的元素集合，按分数升序排列
     */
    @Override
    public Set<Object> zRange(String key, int start, int end) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<Object> values = scoredSortedSet.valueRange(start, end);
        // 保持顺序
        return new LinkedHashSet<>(values);
    }

    /**
     * 获取有序集合中指定排名区间的元素及其分数（按升序）
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 区间内元素及其分数的 Map，按分数升序排列
     */
    @Override
    public Map<Object, Double> zRangeWithScores(String key, int start, int end) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Collection<ScoredEntry<Object>> entries = scoredSortedSet.entryRange(start, end);
        Map<Object, Double> result = new LinkedHashMap<>();
        for (ScoredEntry<Object> entry : entries) {
            result.put(entry.getValue(), entry.getScore());
        }
        return result;
    }

    /**
     * 获取有序集合中指定排名区间内的元素（按降序）。
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 区间内元素集合，按分数降序排列
     */
    @Override
    public Set<Object> zRevRange(String key, int start, int end) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        return new LinkedHashSet<>(scoredSortedSet.valueRangeReversed(start, end));
    }

    /**
     * 获取有序集合中指定排名区间内的元素及其分数（按降序）。
     *
     * @param key   有序集合的 key
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 区间内元素及其分数的 Map，按分数降序排列
     */
    @Override
    public Map<Object, Double> zRevRangeWithScores(String key, int start, int end) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        Map<Object, Double> result = new LinkedHashMap<>();
        for (Object value : scoredSortedSet.valueRangeReversed(start, end)) {
            Double score = scoredSortedSet.getScore(value);
            if (score != null) {
                result.put(value, score);
            }
        }
        return result;
    }

    /**
     * 为有序集合中指定元素的分数增加指定值。
     *
     * @param key   有序集合的 key
     * @param value 指定元素
     * @param delta 要增加的分数（可为负）
     * @return 增加后的新分数
     */
    @Override
    public Double zIncrBy(String key, Object value, double delta) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        return scoredSortedSet.addScore(value, delta);
    }

    /**
     * 获取有序集合中的元素总数
     *
     * @param key 有序集合的 key
     * @return 元素个数
     */
    @Override
    public int zCard(String key) {
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        return scoredSortedSet.size();
    }

    /**
     * 获取有序集合中分数在指定区间内的元素数量。
     *
     * @param key 有序集合的 key，不能为空
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 指定分数范围内的元素个数；若 key 为空或不存在，则返回 0
     */
    @Override
    public long zCount(String key, double min, double max) {
        // 校验 key
        if (key == null || key.trim().isEmpty()) {
            return 0L;
        }
        // 获取 Redis 中的有序集合
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        if (scoredSortedSet == null) {
            return 0L;
        }
        return scoredSortedSet.count(min, true, max, true);
    }

    /**
     * 移除指定分数区间内的所有元素。
     *
     * @param key 有序集合的 key，不能为空
     * @param min 最小分数（包含）
     * @param max 最大分数（包含）
     * @return 实际移除的元素个数；若 key 为空或不存在，则返回 0
     */
    @Override
    public long zRemoveRangeByScore(String key, double min, double max) {
        // 校验 key
        if (key == null || key.trim().isEmpty()) {
            return 0L;
        }
        // 获取 Redis 中的有序集合
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        if (scoredSortedSet == null) {
            return 0L;
        }
        return scoredSortedSet.removeRangeByScore(min, true, max, true);
    }

    /**
     * 移除指定排名区间内的所有元素。
     *
     * @param key   有序集合的 key，不能为空
     * @param start 起始排名（0 基础）
     * @param end   结束排名（包含）
     * @return 实际移除的元素个数；若 key 为空或不存在，则返回 0
     */
    @Override
    public long zRemoveRangeByRank(String key, int start, int end) {
        // 校验 key
        if (key == null || key.trim().isEmpty()) {
            return 0L;
        }
        // 获取 Redis 中的有序集合
        RScoredSortedSet<Object> scoredSortedSet = redissonClient.getScoredSortedSet(key);
        if (scoredSortedSet == null) {
            return 0L;
        }
        return scoredSortedSet.removeRangeByRank(start, end);
    }

    // -------------------------- 分布式锁与同步器 --------------------------


    /**
     * 获取可重入分布式锁（默认锁名）。
     *
     * @param lockKey 锁的 key
     * @return RLock 实例
     */
    @Override
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 阻塞式获取锁，直到成功。
     *
     * @param lockKey 锁的 key
     */
    @Override
    public void lock(String lockKey) {
        RLock lock = getLock(lockKey);
        lock.lock();
    }

    /**
     * 阻塞式获取锁，设置自动释放时间。
     *
     * @param lockKey   锁的 key
     * @param leaseTime 自动释放时间，单位：秒
     */
    @Override
    public void lock(String lockKey, long leaseTime) {
        RLock lock = getLock(lockKey);
        lock.lock(leaseTime, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取锁，如果获取到则在指定时间后自动释放。
     *
     * @param lockKey   锁的 key
     * @param waitTime  等待时间
     * @param leaseTime 自动释放时间
     * @param unit      时间单位
     * @return 是否成功获取锁
     */
    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放锁。
     *
     * @param lockKey 锁的 key
     */
    @Override
    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 判断当前线程是否持有指定的锁。
     *
     * @param key 锁的名称
     * @return 如果当前线程持有该锁，返回 true；否则返回 false
     */
    @Override
    public boolean isHeldByCurrentThread(String key) {
        RLock lock = getLock(key);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 判断指定的锁当前是否被任意线程持有。
     *
     * @param key 锁的名称
     * @return 如果该锁已被任意线程持有，返回 true；否则返回 false
     */
    @Override
    public boolean isLocked(String key) {
        RLock lock = getLock(key);
        return lock.isLocked();
    }

    /**
     * 获取读锁（阻塞式）。
     *
     * @param lockKey 锁的 key
     */
    @Override
    public void readLock(String lockKey) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = readWriteLock.readLock();
        readLock.lock();
    }

    /**
     * 获取写锁（阻塞式）。
     *
     * @param lockKey 锁的 key
     */
    @Override
    public void writeLock(String lockKey) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = readWriteLock.writeLock();
        writeLock.lock();
    }

    /**
     * 尝试获取读锁，在指定等待时间内尝试获取锁，获取成功后在指定时间后自动释放。
     *
     * @param lockKey   锁的 key
     * @param waitTime  最大等待时间
     * @param leaseTime 获取成功后持有的时间
     * @param unit      时间单位
     * @return 是否成功获取读锁
     */
    @Override
    public boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = readWriteLock.readLock();
        try {
            return readLock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 尝试获取写锁，在指定等待时间内尝试获取锁，获取成功后在指定时间后自动释放。
     *
     * @param lockKey   锁的 key
     * @param waitTime  最大等待时间
     * @param leaseTime 获取成功后持有的时间
     * @param unit      时间单位
     * @return 是否成功获取写锁
     */
    @Override
    public boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = readWriteLock.writeLock();
        try {
            return writeLock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放读锁。
     *
     * @param lockKey 锁的 key
     */
    @Override
    public void unlockRead(String lockKey) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = readWriteLock.readLock();
        if (readLock.isHeldByCurrentThread()) {
            readLock.unlock();
        }
    }

    /**
     * 释放写锁。
     *
     * @param lockKey 锁的 key
     */
    @Override
    public void unlockWrite(String lockKey) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = readWriteLock.writeLock();
        if (writeLock.isHeldByCurrentThread()) {
            writeLock.unlock();
        }
    }

    /**
     * 设置闭锁的计数。
     *
     * @param latchKey 闭锁 key
     * @param count    计数器初始值
     */
    @Override
    public void setCount(String latchKey, int count) {
        RCountDownLatch latch = redissonClient.getCountDownLatch(latchKey);
        latch.trySetCount(count);
    }

    /**
     * 递减计数器，释放等待线程。
     *
     * @param latchKey 闭锁 key
     */
    @Override
    public void countDown(String latchKey) {
        RCountDownLatch latch = redissonClient.getCountDownLatch(latchKey);
        latch.countDown();
    }

    /**
     * 阻塞等待直到计数器归零。
     *
     * @param latchKey 闭锁 key
     * @throws InterruptedException 中断异常
     */
    @Override
    public void await(String latchKey) throws InterruptedException {
        RCountDownLatch latch = redissonClient.getCountDownLatch(latchKey);
        latch.await();
    }

    /**
     * 在指定时间内等待计数器归零。
     *
     * @param latchKey 闭锁 key
     * @param timeout  最大等待时长
     * @param unit     时间单位
     * @return 是否成功等待完成
     * @throws InterruptedException 中断异常
     */
    @Override
    public boolean await(String latchKey, long timeout, TimeUnit unit) throws InterruptedException {
        RCountDownLatch latch = redissonClient.getCountDownLatch(latchKey);
        return latch.await(timeout, unit);
    }

    /**
     * 初始化信号量许可数。
     *
     * @param semaphoreKey 信号量 key
     * @param permits      初始许可数量
     */
    @Override
    public void trySetPermits(String semaphoreKey, int permits) {
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        semaphore.trySetPermits(permits);
    }

    /**
     * 获取一个信号量许可（阻塞直到成功）。
     *
     * @param semaphoreKey 信号量 key
     * @throws InterruptedException 中断异常
     */
    @Override
    public void acquire(String semaphoreKey) throws InterruptedException {
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        semaphore.acquire();
    }

    /**
     * 尝试获取一个信号量许可，限时等待。
     *
     * @param semaphoreKey 信号量 key
     * @param timeout      最大等待时长
     * @param unit         时间单位
     * @return 是否成功获取许可
     * @throws InterruptedException 中断异常
     */
    @Override
    public boolean tryAcquire(String semaphoreKey, long timeout, TimeUnit unit) throws InterruptedException {
        if (semaphoreKey == null || semaphoreKey.trim().isEmpty() || timeout <= 0 || unit == null) {
            return false;
        }
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        return semaphore.tryAcquire(timeout, unit);
    }

    /**
     * 释放一个许可。
     *
     * @param semaphoreKey 信号量 key
     */
    @Override
    public void release(String semaphoreKey) {
        if (semaphoreKey == null || semaphoreKey.trim().isEmpty()) {
            return;
        }
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        semaphore.release();
    }

    /**
     * 获取当前可用许可数。
     *
     * @param semaphoreKey 信号量 key
     * @return 可用许可数
     */
    @Override
    public int availablePermits(String semaphoreKey) {
        if (semaphoreKey == null || semaphoreKey.trim().isEmpty()) {
            return 0;
        }
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        return semaphore.availablePermits();
    }

    // -------------------------- 布隆过滤器 --------------------------

    /**
     * 初始化布隆过滤器，设置预期插入元素数量和误判率。
     *
     * @param key                布隆过滤器对应的 Redis 键
     * @param expectedInsertions 预期插入的元素数量（用于计算位数组大小）
     * @param falseProbability   期望的误判率（一般建议0.03或更小）
     */
    @Override
    public void bloomInit(String key, long expectedInsertions, double falseProbability) {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        bloomFilter.tryInit(expectedInsertions, falseProbability);
    }

    /**
     * 判断元素是否可能存在布隆过滤器中。
     *
     * @param key   布隆过滤器对应的 Redis 键
     * @param value 要检测的元素
     * @return true 表示元素可能存在（误判存在）；false 表示一定不存在
     */
    @Override
    public boolean bloomContains(String key, Object value) {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        return bloomFilter.contains(value);
    }

    /**
     * 添加元素到布隆过滤器中。
     *
     * @param key   布隆过滤器对应的 Redis 键
     * @param value 要添加的元素
     * @return true 如果元素之前不存在且已成功添加，false 如果元素可能已存在
     */
    @Override
    public boolean bloomAdd(String key, Object value) {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        return bloomFilter.add(value);
    }

    /**
     * 批量添加元素到布隆过滤器中。
     *
     * @param key    布隆过滤器对应的 Redis 键
     * @param values 批量元素集合
     * @return 添加成功的元素数量
     */
    @Override
    public long bloomAddAll(String key, Collection<?> values) {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        long addedCount = 0;
        for (Object value : values) {
            if (bloomFilter.add(value)) {
                addedCount++;
            }
        }
        return addedCount;
    }

    /**
     * 删除布隆过滤器（删除对应的 Redis 键）。
     *
     * @param key 布隆过滤器对应的 Redis 键
     * @return 是否成功删除
     */
    @Override
    public boolean bloomDelete(String key) {
        return redissonClient.getKeys().delete(key) > 0;
    }

    /**
     * 判断布隆过滤器是否已经初始化（是否存在）。
     *
     * @param key 布隆过滤器对应的 Redis 键
     * @return true 表示已初始化，false 表示未初始化
     */
    @Override
    public boolean bloomExists(String key) {
        return redissonClient.getKeys().countExists(key) > 0;
    }

    /**
     * 获取布隆过滤器的预计插入容量。
     *
     * @param key 布隆过滤器对应的 Redis 键
     * @return 预计插入元素数量，若未初始化则返回 0
     */
    @Override
    public long bloomGetExpectedInsertions(String key) {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        try {
            return bloomFilter.getExpectedInsertions();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 获取布隆过滤器的误判率。
     *
     * @param key 布隆过滤器对应的 Redis 键
     * @return 当前设置的误判率，若未初始化则返回 0.0
     */
    @Override
    public double bloomGetFalseProbability(String key) {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        try {
            return bloomFilter.getFalseProbability();
        } catch (Exception e) {
            return 0.0;
        }
    }

    // --------------------- 分布式队列操作 ---------------------

    @Override
    public <T> void enqueueBlocking(String queueKey, T value) throws InterruptedException {
        RBlockingQueue<T> queue = redissonClient.getBlockingQueue(queueKey);
        queue.put(value);
    }

    @Override
    public <T> boolean enqueueBlocking(String queueKey, T value, long timeout, TimeUnit timeUnit) throws InterruptedException {
        RBlockingQueue<T> queue = redissonClient.getBlockingQueue(queueKey);
        return queue.offer(value, timeout, timeUnit);
    }

    @Override
    public <T> boolean enqueue(String queueKey, T value) {
        RQueue<T> queue = redissonClient.getQueue(queueKey);
        return queue.offer(value);
    }

    @Override
    public <T> T dequeueBlocking(String queueKey, long timeout) throws InterruptedException {
        RBlockingQueue<T> queue = redissonClient.getBlockingQueue(queueKey);
        return queue.poll(timeout, TimeUnit.SECONDS);
    }

    @Override
    public <T> T dequeue(String queueKey) {
        RQueue<T> queue = redissonClient.getQueue(queueKey);
        return queue.poll();
    }

    @Override
    public long queueSize(String queueKey) {
        RQueue<Object> queue = redissonClient.getQueue(queueKey);
        return queue.size();
    }

    // --------------------- 延迟队列操作 ---------------------

    @Override
    public <T> void enqueueDelayed(String queueKey, T value, long delay, TimeUnit timeUnit) {
        RQueue<T> queue = redissonClient.getQueue(queueKey);
        RDelayedQueue<T> delayedQueue = redissonClient.getDelayedQueue(queue);
        delayedQueue.offer(value, delay, timeUnit);
        // 延迟队列使用完后不手动销毁，让 Redisson 管理
    }

    // --------------------- 队列辅助操作 ---------------------

    @Override
    public void clearQueue(String queueKey) {
        RQueue<Object> queue = redissonClient.getQueue(queueKey);
        queue.clear();
    }

    @Override
    public boolean isQueueEmpty(String queueKey) {
        RQueue<Object> queue = redissonClient.getQueue(queueKey);
        return queue.isEmpty();
    }

    @Override
    public boolean removeFromQueue(String queueKey, Object value) {
        RQueue<Object> queue = redissonClient.getQueue(queueKey);
        return queue.remove(value);
    }

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
    @Override
    public boolean rateLimiterInit(String key, RateType rateType, long rate, long interval, RateIntervalUnit unit) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        if (rateLimiter.isExists()) {
            return false;
        }
        rateLimiter.trySetRate(rateType, rate, interval, unit);
        return true;
    }

    /**
     * 尝试获取一个令牌（非阻塞式）
     *
     * @param key 限流器的 Redis Key
     * @return true 表示获取成功，false 表示被限流
     */
    @Override
    public boolean rateLimiterTryAcquire(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        return rateLimiter.tryAcquire();
    }

    /**
     * 尝试在指定时间内获取一个令牌（阻塞等待，超时返回）
     *
     * @param key     限流器的 Redis Key
     * @param timeout 最大等待时间
     * @param unit    时间单位
     * @return true 表示获取成功，false 表示超时未获取
     */
    @Override
    public boolean rateLimiterTryAcquire(String key, long timeout, TimeUnit unit) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        return rateLimiter.tryAcquire(timeout, unit);
    }

    /**
     * 获取限流器对象（可用于自定义高级操作）
     *
     * @param key 限流器 Redis Key
     * @return RRateLimiter 实例
     */
    @Override
    public RRateLimiter rateLimiterGet(String key) {
        return redissonClient.getRateLimiter(key);
    }

    /**
     * 删除限流器配置（从 Redis 清除）
     *
     * @param key 限流器 Redis Key
     * @return true 表示删除成功；false 表示不存在
     */
    @Override
    public boolean rateLimiterDelete(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        return rateLimiter.delete();
    }

    // --------------------- 发布订阅操作 ---------------------

    /**
     * 用于存储当前订阅的频道及其监听器ID
     */
    private final Map<String, Integer> listenerIdMap = new ConcurrentHashMap<>();

    /**
     * 向指定频道发布消息。
     *
     * @param channel 频道名称
     * @param message 要发布的消息内容
     */
    @Override
    public void publish(String channel, Object message) {
        RTopic topic = redissonClient.getTopic(channel);
        topic.publish(message);
    }

    /**
     * 订阅指定频道，异步接收消息。
     *
     * @param channel         频道名称
     * @param messageConsumer 消息回调函数，接收到消息时执行
     */
    @Override
    public void subscribe(String channel, Consumer<Object> messageConsumer) {
        RTopic topic = redissonClient.getTopic(channel);
        // 注册消息监听器
        int listenerId = topic.addListener(Object.class, (c, msg) -> messageConsumer.accept(msg));
        listenerIdMap.put(channel, listenerId);
    }

    /**
     * 取消订阅指定频道。
     *
     * @param channel 频道名称
     */
    @Override
    public void unsubscribe(String channel) {
        Integer listenerId = listenerIdMap.remove(channel);
        if (listenerId != null) {
            RTopic topic = redissonClient.getTopic(channel);
            topic.removeListener(listenerId);
        }
    }

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
    @Override
    public <T> T eval(String script, Class<T> returnType, List<Object> keys, Object... args) {
        return redissonClient.getScript(StringCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE, script, RScript.ReturnType.VALUE, keys, args);
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
    public void evalNoResult(String script, List<Object> keys, Object... args) {
        redissonClient.getScript(StringCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE, script, RScript.ReturnType.VALUE, keys, args);
    }

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
    @Override
    public <T> T evalBySha(String sha1, Class<T> returnType, List<Object> keys, Object... values) {
        return redissonClient.getScript(StringCodec.INSTANCE)
                .evalSha(RScript.Mode.READ_WRITE, sha1, RScript.ReturnType.VALUE, keys, values);
    }

    /**
     * 将 Lua 脚本加载到 Redis 并返回 SHA1。
     *
     * @param script Lua 脚本内容
     * @return Lua 脚本在 Redis 中的 SHA1
     * <p>
     * 核心逻辑：
     * 1. 脚本不会立即执行，只是加载到 Redis
     * 2. 返回 SHA1 后可通过 evalBySha 执行，减少网络传输
     */
    @Override
    public String loadScript(String script) {
        return redissonClient.getScript(StringCodec.INSTANCE).scriptLoad(script);
    }

}

```



## 使用Redisson

```java
package local.ateng.java.redisjdk8;


import com.fasterxml.jackson.core.type.TypeReference;
import local.ateng.java.redisjdk8.entity.UserInfoEntity;
import local.ateng.java.redisjdk8.init.InitData;
import local.ateng.java.redisjdk8.service.RedissonService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisServiceTests {
    @Autowired
    private RedissonService redissonService;

    @Test
    void set() {
        List<UserInfoEntity> list = new InitData().getList();
        redissonService.set("my:user", list.get(0));
        Map<String, UserInfoEntity> map = new HashMap<>();
        map.put("test", list.get(0));
        redissonService.set("my:userMap", map);
        redissonService.set("my:userList", list);
    }

    @Test
    void get() {
        UserInfoEntity userInfoEntity = redissonService.get("my:user",new TypeReference<UserInfoEntity>(){});
        System.out.println(userInfoEntity.getCity());
    }

    @Test
    void getList() {
        List<UserInfoEntity> list = redissonService.get("my:userList", new TypeReference<List<UserInfoEntity>>() {});
        System.out.println(list.get(0).getClass());
    }

    @Test
    void getMap() {
        Map<String, UserInfoEntity> map = redissonService.get("my:userMap", new TypeReference<Map<String, UserInfoEntity>>() {});
        System.out.println(map.get("test").getClass());
    }

    /**
     * 获取可重入锁（Reentrant Lock）
     */
    @Test
    void lock1() throws InterruptedException {
        // 获取锁对象
        RLock lock = redissonService.getLock("myLock");

        // 阻塞式加锁，直到获得锁
        lock.lock();

        // 业务代码
        try {
            // 执行临界区操作
            Thread.sleep(1000000);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 设置自动释放时间的锁（防止死锁）
     */
    @Test
    void lock2() throws InterruptedException {
        // 获取锁对象
        RLock lock = redissonService.getLock("myLock");

        // 阻塞式加锁，10秒后自动释放锁
        lock.lock(10, TimeUnit.SECONDS);

        // 业务代码
        try {
            // 执行临界区操作
            Thread.sleep(1000000);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 尝试加锁（带超时）
     */
    @Test
    void lock3() throws InterruptedException {
        RLock lock = redissonService.getLock("myLock");

        boolean locked = false;
        try {
            // 尝试等待最多5秒获取锁，获取后锁自动10秒释放
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (locked) {
                // 获得锁，执行业务逻辑
                Thread.sleep(1000000);
            } else {
                // 未获得锁，可以做其他处理（比如返回失败或重试）
                System.out.println("未获得锁，可以做其他处理（比如返回失败或重试）");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Test
    void bloom1() {
        redissonService.bloomInit("test:bloomfilter", 1000L, 0.03);
    }

    @Test
    void bloom2() {
        long addedCount = redissonService.bloomAddAll("test:bloomfilter", java.util.Arrays.asList("a", "b", "c"));
        System.out.println(addedCount);
        long addedCount2 = redissonService.bloomAddAll("test:bloomfilter", java.util.Arrays.asList("a", "b", "c", "d"));
        System.out.println(addedCount2);
    }

    /**
     * 测试 eval 方法（返回整数）
     */
    @Test
    void testEval() {
        String script = "return tonumber(ARGV[1]) + tonumber(ARGV[2])";
        Long result = redissonService.eval(script, Long.class, Collections.emptyList(), 5, 7);
        System.out.println(result);
    }

    /**
     * 测试 evalNoResult 方法（无返回值）
     */
    @Test
    void testEvalNoResult() {
        String script = "redis.call('SET', KEYS[1], ARGV[1])";
        redissonService.evalNoResult(script, Collections.singletonList("test:key"), "hello");
        String value = redissonService.eval("return redis.call('GET', KEYS[1])", String.class, Collections.singletonList("test:key"));
        System.out.println(value);
    }

    /**
     * 测试 loadScript + evalBySha
     */
    @Test
    void testEvalBySha() {
        String script = "return tonumber(ARGV[1]) * tonumber(ARGV[2])";
        String sha1 = redissonService.loadScript(script);
        Long result = redissonService.evalBySha(sha1, Long.class, Collections.emptyList(), 3, 4);
        System.out.println(result);
    }

    /**
     * 测试 eval 获取 Redis key 的值
     */
    @Test
    void testEvalGetKey() {
        // 先写一个 key
        redissonService.evalNoResult("redis.call('SET', KEYS[1], ARGV[1])",
                Collections.singletonList("user:1:name"), "blair");

        // 再读取这个 key
        String value = redissonService.eval("return redis.call('GET', KEYS[1])",
                String.class, Collections.singletonList("user:1:name"));

        System.out.println(value);
    }

    /**
     * 测试 eval 方法：JSON.SET
     */
    @Test
    void testEval_JsonSet() {
        String script = "return redis.call('JSON.SET', KEYS[1], '$', ARGV[1])";
        String jsonData = "{\"name\":\"blair\",\"age\":25}";
        String result = redissonService.eval(script, String.class,
                Collections.singletonList("user:1001"), jsonData);
        System.out.println(result);
    }

    /**
     * 测试 evalNoResult 方法：JSON.SET + JSON.GET
     */
    @Test
    void testEvalNoResult_JsonSetGet() {
        String setScript = "redis.call('JSON.SET', KEYS[1], '$', ARGV[1])";
        String jsonData = "{\"title\":\"Developer\",\"skills\":[\"Java\",\"Vue\"]}";
        redissonService.evalNoResult(setScript,
                Collections.singletonList("user:1002"), jsonData);

        String getScript = "return redis.call('JSON.GET', KEYS[1], '$.title')";
        String title = redissonService.eval(getScript, String.class,
                Collections.singletonList("user:1002"));

        System.out.println(title);
    }


}

```

## 分布式锁

### Redisson 分布式锁类型总览表

| 锁类型                     | 类名                        | 是否可重入         | 是否支持自动续期（看门狗） | 是否支持公平性 | 特点说明                            | 典型业务场景                      |
| -------------------------- | --------------------------- | ------------------ | -------------------------- | -------------- | ----------------------------------- | --------------------------------- |
| **可重入锁**               | `RLock`                     | 是                 | 是                         | 否             | 最常用，可重入，默认 30s 看门狗续期 | 秒杀、库存扣减、幂等处理          |
| **公平锁**                 | `RFairLock`                 | 是                 | 是                         | 是             | 按申请顺序排队，避免线程饥饿        | 排队公平要求高的系统，如下单排队  |
| **读写锁**                 | `RReadWriteLock`            | 是                 | 是                         | 否             | 多读一写模型。读读共享、写独占      | 配置更新、字典表更新              |
| **红锁（RedLock）**        | `RedissonRedLock`           | 否（实际不可重入） | 部分支持                   | 否             | 基于多个 Redis 节点的高可用锁       | 多 Redis 主从或多 Pod 部署        |
| **联锁（MultiLock）**      | `RedissonMultiLock`         | 依赖实际锁         | 依赖实际锁                 | 否             | 同时加多个锁，任一失败则全部失败    | 对多个资源同时加锁（如商品+店铺） |
| **信号量**                 | `RSemaphore`                | 不适用             | 否                         | 否             | 限流器，允许 N 个线程进入           | 比如一个资源只能 3 人访问         |
| **可过期信号量**           | `RPermitExpirableSemaphore` | -                  | -                          | -              | 信号可自动过期                      | 临时许可证、临时权限控制          |
| **可重入闭锁（计数器）**   | `RCountDownLatch`           | -                  | -                          | -              | 分布式版 CountDownLatch             | 等待多个节点任务完成              |
| **原子锁（基于原子操作）** | `RAtomicLong`               | -                  | -                          | -              | 不能算真正的分布式锁，但常配合使用  | 全局递增 ID、库存扣减             |

------

### 关键机制对照表（Redisson 为什么安全）

| 能力                          | 说明                                                     |
| ----------------------------- | -------------------------------------------------------- |
| **看门狗（Watchdog）**        | 默认锁租期 30 秒，任务正常执行时自动续期，避免锁提前释放 |
| **可重入性**                  | 同一线程多次加锁不会死锁                                 |
| **原子加锁脚本（Lua）**       | Redis 侧保证加锁原子性                                   |
| **锁 Key 绑定线程标识**       | 防止 A 线程误释放 B 线程的锁                             |
| **自动尝试获取锁（tryLock）** | 支持等待时间与锁时间分离                                 |
| **公平性队列（FairLock）**    | 使用有序队列避免抢占不公平                               |

------

### Redisson 常用方法对照表

| 方法                                 | 作用                                                  | 示例解释                 |
| ------------------------------------ | ----------------------------------------------------- | ------------------------ |
| `lock()`                             | 阻塞加锁，永久等待                                    | 常用于必须成功的业务     |
| `lock(leaseTime, unit)`              | 手动指定过期时间（无续期）                            | 适合预计任务时间短的操作 |
| `tryLock(waitTime, leaseTime, unit)` | 在 waitTime 内尝试获取锁，成功则 leaseTime 后自动释放 | 秒杀等业务常用           |
| `unlock()`                           | 释放锁                                                | 只能释放自己加的锁       |

------

### 常见 Redisson 锁使用示例

1. **最常用可重入锁 RLock**

```java
RLock lock = redissonClient.getLock("order:create");
try {
    if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
        // 业务代码
    }
} finally {
    lock.unlock();
}
```

------

2. **公平锁 RFairLock**

```java
RLock fairLock = redissonClient.getFairLock("queue:lock");
fairLock.lock();
try {
    // 按申请顺序排队执行
} finally {
    fairLock.unlock();
}
```

------

3. **读写锁 RReadWriteLock**

```java
RReadWriteLock rwLock = redissonClient.getReadWriteLock("config:rw");

// 写锁
rwLock.writeLock().lock();

// 读锁
rwLock.readLock().lock();
```

------

4. **RedLock（高可用锁）**

```java
RLock lock1 = client1.getLock("lock");
RLock lock2 = client2.getLock("lock");
RLock lock3 = client3.getLock("lock");

RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);

redLock.lock();
try {
    // 高可用
} finally {
    redLock.unlock();
}
```

------

### Redisson 分布式锁常见问题对照表

| 问题             | 原因                 | 解决方案                            |
| ---------------- | -------------------- | ----------------------------------- |
| 锁提前失效       | 忘记开启看门狗       | 使用 `lock()` 或 `tryLock` 默认续期 |
| 死锁             | 未 unlock 或异常导致 | 用 try-finally                      |
| 锁不释放         | leaseTime 太长       | 使用合理 leaseTime 或看门狗         |
| RedLock 获取失败 | 多节点不同步         | 不建议在单 Redis 架构使用 RedLock   |



## 发布和订阅

### 发布

```java
    @PostMapping("/publish")
    public String publish(@RequestParam String channel, @RequestBody Object message) {
        redissonService.publish(channel, message);
        return "消息已发布到频道: " + channel;
    }
```

### 订阅

```java
package local.ateng.java.redisjdk8.consumer;

import local.ateng.java.redisjdk8.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Redisson 发布/订阅的消费者（SmartLifecycle 管理）
 * 支持：
 * - Spring 启动自动订阅
 * - Spring 停止自动取消订阅
 * - 异步接收消息
 */
@Component
@RequiredArgsConstructor
public class RedissonPubSubConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RedissonPubSubConsumer.class);

    private final RedissonService redissonService;

    /**
     * 运行状态，线程安全
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 订阅的频道名称，可根据业务修改或注入配置
     */
    private static final String CHANNEL_NAME = "my_channel";

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("【Redisson Pub/Sub 消费者】开始启动，订阅频道: {}", CHANNEL_NAME);

            // 订阅频道
            redissonService.subscribe(CHANNEL_NAME, message -> {
                log.info("【Redisson Pub/Sub 消费者】收到消息: {}", message);
                process(message);
            });
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("【Redisson Pub/Sub 消费者】停止订阅频道: {}", CHANNEL_NAME);
            // 取消订阅
            redissonService.unsubscribe(CHANNEL_NAME);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 消息处理逻辑
     */
    private void process(Object message) {
        // TODO: 根据业务处理消息
        log.info("【Redisson Pub/Sub 消费者】处理消息完成: {}", message);
    }
}


```



## 使用队列

### 创建线程池

这部分参考：[线程池](/work/Ateng-Java/tools/thread-pool/) 文档添加配置即可

```yaml
---
# 线程池配置
thread-pool:
  # 是否启用线程池功能
  enabled: true
  # 是否将第一个线程池作为默认 @Async 线程池
  use-first-as-default: false
  pools:
    # 默认线程池
    defaultPool:
      # ...
    # Redisson 队列线程池
    queuePool:
      core-size: 2
      max-size: 4
      queue-capacity: 100
      keep-alive-seconds: 60
      thread-name-prefix: queue-consumer-
      allow-core-thread-timeout: false
      rejected-policy: callerRuns
      await-termination-seconds: 30
```

### 生产者

```java
package local.ateng.java.redisjdk8.controller;

import local.ateng.java.redisjdk8.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/redisson")
@RequiredArgsConstructor
public class RedissonController {
    private final RedissonService redissonService;


    /**
     * 发送消息到普通队列（非阻塞）
     *
     * @param queueKey 队列名
     * @param message  消息内容
     * @return 是否发送成功
     */
    @PostMapping("/send")
    public boolean sendMessage(@RequestParam String queueKey, @RequestBody Object message) {
        return redissonService.enqueue(queueKey, message);
    }

    /**
     * 发送消息到阻塞队列（阻塞等待队列可用位置）
     *
     * @param queueKey 队列名
     * @param message  消息内容
     * @param timeout  等待时间（秒）
     * @return 是否发送成功
     * @throws InterruptedException 阻塞等待时被中断
     */
    @PostMapping("/send/blocking")
    public boolean sendMessageBlocking(@RequestParam String queueKey,
                                       @RequestBody Object message,
                                       @RequestParam(defaultValue = "10") long timeout) throws InterruptedException {
        return redissonService.enqueueBlocking(queueKey, message, timeout, TimeUnit.SECONDS);
    }

    /**
     * 发送消息到延迟队列
     *
     * @param queueKey 队列名
     * @param message  消息内容
     * @param delay    延迟时间
     * @param unit     时间单位（默认秒）
     */
    @PostMapping("/send/delayed")
    public void sendDelayedMessage(@RequestParam String queueKey,
                                   @RequestBody Object message,
                                   @RequestParam long delay,
                                   @RequestParam(defaultValue = "SECONDS") TimeUnit unit) {
        redissonService.enqueueDelayed(queueKey, message, delay, unit);
    }
}

```

### 消费者

```java
package local.ateng.java.redisjdk8.consumer;

import local.ateng.java.redisjdk8.config.ThreadPoolManager;
import local.ateng.java.redisjdk8.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redisson 队列消费者（阻塞 & 非阻塞分开）
 */
@Component
@RequiredArgsConstructor
public class RedissonQueueConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RedissonQueueConsumer.class);

    private final RedissonService redissonService;
    private final ThreadPoolManager threadPoolManager;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final String QUEUE_KEY = "job:queue";

    /**
     * 阻塞消费超时时间（秒），防止无限阻塞
     */
    private static final long BLOCKING_POLL_TIMEOUT = 5;

    /**
     * 非阻塞轮询间隔（秒）
     */
    private static final long NON_BLOCKING_INTERVAL = 2;

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("【Redisson 队列消费者】开始启动…");

            // 阻塞消费者线程
            threadPoolManager.getExecutor("queuePool").submit(this::blockingConsumer);

            // 非阻塞消费者线程
            threadPoolManager.getExecutor("queuePool").submit(this::nonBlockingConsumer);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("【Redisson 队列消费者】收到停止指令，正在关闭…");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 阻塞消费任务
     */
    private void blockingConsumer() {
        log.info("【阻塞队列消费者】已启动，等待任务中…");
        while (running.get()) {
            try {
                Object job = redissonService.dequeueBlocking(QUEUE_KEY, BLOCKING_POLL_TIMEOUT);
                if (job != null) {
                    process(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("【阻塞队列消费者】线程被中断，准备停止…");
            } catch (Exception e) {
                log.error("【阻塞队列消费者】处理任务异常：{}", e.getMessage(), e);
            }
        }
        log.info("【阻塞队列消费者】已停止");
    }

    /**
     * 非阻塞轮询消费任务
     */
    private void nonBlockingConsumer() {
        log.info("【非阻塞队列消费者】已启动，轮询任务中…");
        while (running.get()) {
            try {
                Object job = redissonService.dequeue(QUEUE_KEY);
                if (job != null) {
                    process(job);
                } else {
                    TimeUnit.SECONDS.sleep(NON_BLOCKING_INTERVAL);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("【非阻塞队列消费者】线程被中断，准备停止…");
            } catch (Exception e) {
                log.error("【非阻塞队列消费者】处理任务异常：{}", e.getMessage(), e);
            }
        }
        log.info("【非阻塞队列消费者】已停止");
    }

    /**
     * 业务处理逻辑
     */
    private void process(Object job) {
        log.info("【队列消费者】开始处理任务：{}", job);

        // TODO: 实际业务处理逻辑
        // ...

        log.info("【队列消费者】任务处理完成：{}", job);
    }
}

```

