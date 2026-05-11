# Redisson

Redisson 是一个基于 Redis 的 Java 客户端，提供了丰富的分布式数据结构和服务，如分布式锁、集合、队列、Map 等。它简化了与 Redis 的交互，并且支持高可用性、分布式事务、监控等特性，非常适合构建高性能和高可扩展性的应用。

- [官网链接](https://redisson.org)



## 基础配置

本节用于完成 Redisson 在 Spring Boot 项目中的基础接入，包括 Maven 依赖、`application.yml` 配置、配置属性类以及 `RedissonClient` Bean 的创建。配置方式采用 Redisson 原生 YAML 内容，便于在单机、集群、哨兵等不同 Redis 部署模式之间切换。

### 添加依赖

在 `pom.xml` 中添加 Redisson Spring Boot Starter 依赖。版本号建议统一放在 `properties` 中，便于后续升级维护。

```xml
<!-- 项目属性 -->
<properties>
    <redisson.version>3.52.0</redisson.version>
</properties>
<!-- Redisson 依赖 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>${redisson.version}</version>
</dependency>
```

### 编辑配置文件

Redisson 支持通过 YAML 字符串加载原生配置。这里将配置统一放在 `application.yml` 的 `redisson.config` 字段中，再通过 `Config.fromYAML(...)` 解析成 Redisson 配置对象。

这种方式的优点是配置结构和 Redisson 官方配置保持一致，后续从单机模式切换到集群模式时，只需要调整配置文件，不需要修改 Java 代码。

文件位置：`src/main/resources/application.yml`

#### 单机模式

单机模式适用于开发环境、测试环境，或者 Redis 本身没有部署为集群的简单业务场景。需要注意的是，`address` 必须带上协议前缀，普通 Redis 使用 `redis://`，启用 TLS 的 Redis 使用 `rediss://`。

```yaml
---
# Redisson 配置
redisson:
  config: |
    singleServerConfig:
      # Redis 节点地址，必须包含 redis:// 或 rediss:// 协议
      address: "redis://192.168.1.12:40003"

      # Redis 密码；如果 Redis 未设置密码，可以删除该配置项
      password: "Admin@123"

      # Redis 数据库编号，单机模式支持 database 配置
      database: 0

      # 客户端名称，便于在 Redis CLIENT LIST 中识别连接来源
      clientName: "redisson-client"

      # 连接池最大连接数
      connectionPoolSize: 64

      # 连接池最小空闲连接数
      connectionMinimumIdleSize: 24

      # 空闲连接超时时间，单位：毫秒
      idleConnectionTimeout: 10000

      # 建立连接超时时间，单位：毫秒
      connectTimeout: 5000

      # Redis 命令等待超时时间，单位：毫秒
      timeout: 3000

      # Redis 命令重试次数
      retryAttempts: 3

      # Redis 命令重试间隔，单位：毫秒
      retryInterval: 1500

    # Redisson 业务线程数，主要用于监听器、异步回调等任务
    threads: 16

    # Netty IO 线程数，负责 Redis 网络通信
    nettyThreads: 32

    # 对象序列化方式，推荐使用 JSON，便于排查和跨语言读取
    codec: !<org.redisson.codec.JsonJacksonCodec> {}
```

#### 集群模式

集群模式适用于生产环境中 Redis Cluster 部署场景。`nodeAddresses` 建议填写所有主从节点地址，Redisson 会根据集群拓扑自动发现主从关系并刷新节点状态。

Redis Cluster 不支持按客户端选择普通 `database`，因此集群配置中不要配置 `database`。

文件位置：`src/main/resources/application.yml`

```yaml
---
# Redisson 配置
redisson:
  config: |
    clusterServersConfig:
      # Redis Cluster 节点地址，建议填写全部主从节点
      nodeAddresses:
        - "redis://192.168.1.41:6379"
        - "redis://192.168.1.42:6379"
        - "redis://192.168.1.43:6379"
        - "redis://192.168.1.44:6379"
        - "redis://192.168.1.45:6379"
        - "redis://192.168.1.46:6379"

      # Redis 集群密码；如果集群未设置密码，可以删除该配置项
      password: "Admin@123"

      # 集群拓扑扫描间隔，单位：毫秒
      scanInterval: 2000

      # 读取模式：
      # MASTER：只从主节点读取
      # SLAVE：优先从从节点读取
      # MASTER_SLAVE：主从节点都可读取
      readMode: "SLAVE"

      # 订阅模式：
      # MASTER：只使用主节点订阅
      # SLAVE：使用从节点订阅
      # MASTER_SLAVE：主从节点都可订阅
      subscriptionMode: "SLAVE"

      # 负载均衡策略
      loadBalancer: !<org.redisson.connection.balancer.RoundRobinLoadBalancer> {}

      # 主节点连接池最大连接数
      masterConnectionPoolSize: 64

      # 从节点连接池最大连接数
      slaveConnectionPoolSize: 64

      # 主节点连接池最小空闲连接数
      masterConnectionMinimumIdleSize: 24

      # 从节点连接池最小空闲连接数
      slaveConnectionMinimumIdleSize: 24

      # 空闲连接超时时间，单位：毫秒
      idleConnectionTimeout: 10000

      # 建立连接超时时间，单位：毫秒
      connectTimeout: 5000

      # Redis 命令等待超时时间，单位：毫秒
      timeout: 3000

      # Redis 命令重试次数
      retryAttempts: 3

      # Redis 命令重试间隔，单位：毫秒
      retryInterval: 1500

      # 从节点重连间隔，单位：毫秒
      failedSlaveReconnectionInterval: 3000

      # 从节点健康检查间隔，单位：毫秒
      failedSlaveCheckInterval: 60000

    # Redisson 业务线程数，主要用于监听器、异步回调等任务
    threads: 16

    # Netty IO 线程数，负责 Redis 网络通信
    nettyThreads: 32

    # 对象序列化方式，推荐使用 JSON，便于排查和跨语言读取
    codec: !<org.redisson.codec.JsonJacksonCodec> {}
```

#### 配置说明

| 配置项                            | 说明                       | 建议                                             |
| --------------------------------- | -------------------------- | ------------------------------------------------ |
| `address`                         | 单机 Redis 地址            | 必须包含 `redis://` 或 `rediss://`               |
| `nodeAddresses`                   | Redis Cluster 节点地址列表 | 生产环境建议填写全部主从节点                     |
| `password`                        | Redis 密码                 | 无密码时删除该配置项                             |
| `database`                        | Redis 数据库编号           | 仅单机模式使用，集群模式不要配置                 |
| `clientName`                      | Redis 客户端名称           | 建议配置，便于定位连接来源                       |
| `connectionPoolSize`              | 单机连接池最大连接数       | 根据并发量和 Redis 承载能力调整                  |
| `masterConnectionPoolSize`        | 主节点连接池最大连接数     | 集群模式使用                                     |
| `slaveConnectionPoolSize`         | 从节点连接池最大连接数     | 集群模式使用                                     |
| `connectionMinimumIdleSize`       | 单机最小空闲连接数         | 不宜过大，避免空闲连接浪费                       |
| `masterConnectionMinimumIdleSize` | 主节点最小空闲连接数       | 集群模式使用                                     |
| `slaveConnectionMinimumIdleSize`  | 从节点最小空闲连接数       | 集群模式使用                                     |
| `connectTimeout`                  | 建立连接超时时间           | 网络较差时可适当增大                             |
| `timeout`                         | 命令等待超时时间           | 过小容易误判超时，过大影响失败响应速度           |
| `retryAttempts`                   | 命令重试次数               | 常用值为 `3`                                     |
| `retryInterval`                   | 命令重试间隔               | 常用值为 `1000` 到 `1500` 毫秒                   |
| `threads`                         | Redisson 业务线程数        | 默认可满足多数场景，高并发可适当增加             |
| `nettyThreads`                    | Netty IO 线程数            | 一般按 CPU 核心数和连接规模调整                  |
| `codec`                           | 序列化方式                 | 推荐 `JsonJacksonCodec`，便于查看 Redis 中的数据 |

### 创建配置属性

创建配置属性类，用于读取 `application.yml` 中 `redisson` 前缀下的配置内容。

文件位置：`src/main/java/local/ateng/java/redis/config/RedissonProperties.java`

```java
package local.ateng.java.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置属性
 * 用于读取配置文件中 redisson 前缀下的配置项。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@ConfigurationProperties(prefix = "redisson")
@Configuration
@Data
public class RedissonProperties {

    /**
     * Redisson YAML 配置内容。
     * <p>
     * 通常在 application.yml 中配置为完整的 Redisson YAML 字符串，
     * 然后通过 {@link org.redisson.config.Config#fromYAML(String)} 解析为 Redisson 配置对象。
     * </p>
     */
    private String config;

}
```

### 创建客户端Bean

创建 Redisson 自动配置类，用于解析配置文件中的 YAML 内容，并注册 `RedissonClient` 到 Spring 容器中。

文件位置：`src/main/java/local/ateng/java/redis/config/RedissonConfig.java`

```java
package local.ateng.java.redis.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JacksonCodec;
import org.redisson.codec.JsonCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Redisson 自动配置
 * 用于根据配置文件创建 RedissonClient，并提供 RedisJSON 使用的 JsonCodec。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Configuration
@RequiredArgsConstructor
public class RedissonConfig {

    /**
     * Redisson 配置属性。
     */
    private final RedissonProperties redissonProperties;

    /**
     * 创建 Redisson 客户端。
     * <p>
     * 这里从配置文件读取 Redisson YAML 配置内容，
     * 并通过 {@link Config#fromYAML(String)} 转换为 Redisson 原生配置对象。
     * </p>
     *
     * @return RedissonClient
     * @throws IOException Redisson YAML 配置解析异常
     */
    @Bean
    public RedissonClient redissonClient() throws IOException {
        // 解析 application.yml 中配置的 Redisson YAML 内容
        Config config = Config.fromYAML(redissonProperties.getConfig());

        // 如果需要统一使用自定义 Jackson 序列化配置，可以启用下面这一行
        // config.setCodec(new CustomJacksonCodec());

        // 根据配置创建 Redisson 客户端实例
        return Redisson.create(config);
    }

    /**
     * 创建 Redisson JSON 编解码器。
     * <p>
     * 该 Bean 主要用于 RJsonBucket / RedisJSON 相关能力。
     * 注意：这里需要使用实现 JsonCodec 的 JacksonCodec，
     * 不能使用普通对象序列化用的 JsonJacksonCodec。
     * </p>
     *
     * @return JsonCodec
     */
    @Bean
    public JsonCodec jsonCodec() {
        return new JacksonCodec(Object.class);
    }

}
```

### 自定义序列化（可选）

#### ObjectMapper 构建工厂

```java
package local.ateng.java.redis.config;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson ObjectMapper 统一构建工厂。
 *
 * <p>
 * 该工厂用于集中管理 JSON 序列化与反序列化策略，避免项目中分散配置导致行为不一致。
 * 当前提供三类构建能力：
 * </p>
 * <ul>
 *     <li>默认配置：适用于通用 JSON 处理场景</li>
 *     <li>存储配置：适用于 Redis、数据库 JSON 字段等持久化场景</li>
 *     <li>Web 配置：适用于 Spring Web 接口入参与返回值场景</li>
 * </ul>
 *
 * @author Ateng
 * @since 2026-04-13
 */
public final class JacksonObjectMapperFactory {

    /**
     * 默认信任的业务包前缀。
     */
    private static final String[] TRUSTED_BASE_PACKAGES = {
            "io.github.atengk",
            "local.ateng.java",
    };

    /**
     * 默认时区标识。
     */
    private static final String DEFAULT_TIME_ZONE_ID = "Asia/Shanghai";

    /**
     * 定义日期时间格式（精确到秒）
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 定义日期格式（不包含时间）
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 定义时间格式（仅时间部分）
     */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 禁止实例化。
     */
    private JacksonObjectMapperFactory() {
    }

    /**
     * 构建默认 ObjectMapper。
     *
     * <p>
     * 默认配置仅保留所有场景都需要的公共能力，适合通用 JSON 处理。
     * </p>
     *
     * @return 默认 ObjectMapper
     */
    public static ObjectMapper buildDefaultObjectMapper() {
        ObjectMapper objectMapper = baseObjectMapper();
        registerDefaultJavaTimeModule(objectMapper);
        return objectMapper;
    }

    /**
     * 构建用于 Redis / 数据库存储的 ObjectMapper。
     *
     * <p>
     * 该配置优先保证序列化结果稳定、类型信息完整、BigDecimal 精度不丢失，并支持异常对象与复杂对象结构。
     * </p>
     *
     * @return 存储场景专用 ObjectMapper
     */
    public static ObjectMapper buildStorageObjectMapper() {
        ObjectMapper objectMapper = baseObjectMapper();
        applyVisibility(
                objectMapper,
                JsonAutoDetect.Visibility.ANY,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE
        );
        applyStableSerialization(objectMapper);
        registerDefaultJavaTimeModule(objectMapper);
        applyDefaultTyping(objectMapper);
        applyThrowableMixIn(objectMapper);
        return objectMapper;
    }

    /**
     * 构建用于 Spring Web（前后端交互）的 ObjectMapper。
     *
     * <p>
     * 该配置优先保证接口输出可读、输入兼容、反序列化安全边界清晰，适合 Controller 层统一使用。
     * </p>
     *
     * @return Web 场景专用 ObjectMapper
     */
    public static ObjectMapper buildWebObjectMapper() {
        ObjectMapper objectMapper = baseObjectMapper();
        applyVisibility(
                objectMapper,
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                JsonAutoDetect.Visibility.DEFAULT
        );
        registerUnifiedDateTimeModule(objectMapper);
        configureLegacyDateFormat(objectMapper);
        applyEnumStrategy(objectMapper);
        applyLenientDeserialization(objectMapper);
        applyJsonReadFeature(objectMapper);
        applyNumberSerialization(objectMapper);
        disableStableSerializationOptions(objectMapper);
        return objectMapper;
    }

    /**
     * 构建用于审计日志（Audit Log）的 ObjectMapper。
     *
     * <p>
     * 该配置用于日志落库、操作审计、数据变更记录等场景，
     * 重点保证序列化结果具备“稳定性、完整性、可追溯性”：
     * </p>
     *
     * @return 审计日志专用 ObjectMapper
     */
    public static ObjectMapper buildAuditObjectMapper() {
        ObjectMapper objectMapper = baseObjectMapper();
        // 可见性：字段优先（保证完整数据输出）
        applyVisibility(
                objectMapper,
                JsonAutoDetect.Visibility.ANY,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE
        );
        applyStableSerialization(objectMapper);
        registerDefaultJavaTimeModule(objectMapper);
        configureLegacyDateFormat(objectMapper);
        applyEnumStrategy(objectMapper);
        applyNumberSerialization(objectMapper);
        applyDefaultTyping(objectMapper);
        return objectMapper;
    }

    /**
     * 构建基础 ObjectMapper。
     *
     * <p>
     * 该方法只负责装载所有场景都需要的公共能力，包括时区、时间模块、空值策略和未知字段处理策略。
     * 具体差异化能力由上层构建方法按场景追加。
     * </p>
     *
     * @return 基础 ObjectMapper
     */
    private static ObjectMapper baseObjectMapper() {

        // 创建 ObjectMapper 实例，用于统一 JSON 序列化与反序列化配置
        ObjectMapper objectMapper = new ObjectMapper();

        // 设置全局默认时区，确保时间序列化与反序列化行为一致
        objectMapper.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIME_ZONE_ID));

        // 注册 JDK8 模块，支持 Optional 等类型
        objectMapper.registerModule(new Jdk8Module());

        // 注册构造参数名模块，支持基于构造方法参数名进行反序列化（需开启 -parameters 编译参数）
        objectMapper.registerModule(new ParameterNamesModule());

        // 禁用时间戳格式输出，统一使用字符串格式，提升可读性
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 禁用空 Bean 序列化失败，避免无属性对象导致异常
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 序列化时忽略值为 null 的字段，减少无效数据输出
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 保留 null 字段
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // 反序列化时忽略未知字段，增强兼容性，避免字段扩展导致失败
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 禁用反序列化自动时区调整，避免时间偏移
        objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

        // 返回基础配置完成的 ObjectMapper
        return objectMapper;
    }

    /**
     * 统一设置对象可见性策略。
     *
     * @param objectMapper       ObjectMapper 实例
     * @param fieldVisibility    字段可见性
     * @param getterVisibility   getter 可见性
     * @param setterVisibility   setter 可见性
     * @param isGetterVisibility isGetter 可见性
     * @param creatorVisibility  构造器可见性
     */
    private static void applyVisibility(ObjectMapper objectMapper,
                                        JsonAutoDetect.Visibility fieldVisibility,
                                        JsonAutoDetect.Visibility getterVisibility,
                                        JsonAutoDetect.Visibility setterVisibility,
                                        JsonAutoDetect.Visibility isGetterVisibility,
                                        JsonAutoDetect.Visibility creatorVisibility) {
        objectMapper.setVisibility(
                objectMapper.getSerializationConfig()
                        .getDefaultVisibilityChecker()
                        .withFieldVisibility(fieldVisibility)
                        .withGetterVisibility(getterVisibility)
                        .withSetterVisibility(setterVisibility)
                        .withIsGetterVisibility(isGetterVisibility)
                        .withCreatorVisibility(creatorVisibility)
        );
    }

    /**
     * 启用存储场景所需的稳定序列化能力。
     *
     * <p>
     * 该配置用于保证序列化结果具备较强可比性和可读性，同时避免 BigDecimal 精度问题。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyStableSerialization(ObjectMapper objectMapper) {

        // 启用 BigDecimal 按原始字符串输出，避免科学计数法导致精度或格式问题
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

        // 启用属性按字母排序，保证序列化结果稳定，便于缓存比对与签名计算
        objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

        // 启用 Map 按 key 排序，确保输出顺序一致
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    /**
     * 注册默认 Java 8 时间模块。
     *
     * <p>
     * 仅用于启用 java.time 类型的基础支持，例如 LocalDate、LocalDateTime、LocalTime 等。
     * 具体的日期时间格式由后续的统一时间模块单独配置。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void registerDefaultJavaTimeModule(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 注册统一日期时间模块。
     *
     * <p>
     * 用于统一 java.time 各类型的序列化与反序列化策略，
     * 明确区分“展示格式”和“时间语义格式”，避免时区信息丢失问题。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void registerUnifiedDateTimeModule(ObjectMapper objectMapper) {

        // 构建统一时区（用于默认时间处理）
        ZoneId zoneId = ZoneId.of(DEFAULT_TIME_ZONE_ID);

        // 构建日期时间格式化器（精确到秒）
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

        // 构建日期格式化器
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

        // 构建时间格式化器
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

        // 创建 JavaTimeModule，用于统一管理时间序列化规则
        JavaTimeModule module = new JavaTimeModule();

        // 注册 LocalDateTime 序列化器
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));

        // 注册 LocalDateTime 反序列化器
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        // 注册 LocalDate 序列化器
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));

        // 注册 LocalDate 反序列化器
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        // 注册 LocalTime 序列化器
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));

        // 注册 LocalTime 反序列化器
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        // 注册 Instant 序列化器（统一输出为 UTC 标准时间）
        module.addSerializer(Instant.class, new JsonSerializer<Instant>() {

            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

                // 空值直接写 null
                if (value == null) {
                    gen.writeNull();
                    return;
                }

                // 使用 ISO_INSTANT 格式输出（带 Z 标识）
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value));
            }
        });

        // 注册 Instant 反序列化器（基于 ISO_INSTANT 解析）
        module.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {

            @Override
            public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

                // 获取文本值
                String text = p.getText();

                // 空字符串返回 null
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }

                // 按 ISO_INSTANT 解析为 Instant
                return Instant.parse(text);
            }
        });

        // 注册 OffsetDateTime 序列化器（保留 offset 信息）
        module.addSerializer(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {

            @Override
            public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

                // 空值直接写 null
                if (value == null) {
                    gen.writeNull();
                    return;
                }

                // 使用 ISO_OFFSET_DATE_TIME 输出（包含偏移量）
                gen.writeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
            }
        });

        // 注册 OffsetDateTime 反序列化器
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {

            @Override
            public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

                // 获取文本值
                String text = p.getText();

                // 空字符串返回 null
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }

                // 按 ISO_OFFSET_DATE_TIME 解析
                return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
        });

        // 注册 ZonedDateTime 序列化器（保留完整时区信息）
        module.addSerializer(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {

            @Override
            public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

                // 空值直接写 null
                if (value == null) {
                    gen.writeNull();
                    return;
                }

                // 使用 ISO_ZONED_DATE_TIME 输出（包含 ZoneId）
                gen.writeString(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value));
            }
        });

        // 注册 ZonedDateTime 反序列化器
        module.addDeserializer(ZonedDateTime.class, new JsonDeserializer<ZonedDateTime>() {

            @Override
            public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

                // 获取文本值
                String text = p.getText();

                // 空字符串返回 null
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }

                // 按 ISO_ZONED_DATE_TIME 解析
                return ZonedDateTime.parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            }
        });

        // 注册时间模块
        objectMapper.registerModule(module);

        // 设置全局时区
        objectMapper.setTimeZone(TimeZone.getTimeZone(zoneId));

        // 禁用时间戳输出（统一为字符串格式）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 禁用反序列化自动时区调整，避免时间偏移
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }


    /**
     * 配置传统 Date 类型的全局格式。
     *
     * <p>
     * 用于统一 java.util.Date 的序列化与反序列化行为，
     * 避免与 java.time 类型出现格式不一致问题。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void configureLegacyDateFormat(ObjectMapper objectMapper) {

        // 创建日期格式化对象（非线程安全，但 ObjectMapper 内部会安全使用）
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);

        // 设置统一时区
        dateFormat.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIME_ZONE_ID));

        // 应用到 ObjectMapper
        objectMapper.setDateFormat(dateFormat);
    }


    /**
     * 应用枚举序列化与反序列化策略。
     *
     * <p>
     * 默认情况下，Jackson 使用 Enum.name() 进行序列化，
     * 当枚举名称变更时容易导致反序列化失败。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyEnumStrategy(ObjectMapper objectMapper) {

        // 使用 toString() 进行序列化（而不是 name）
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        // 反序列化也基于 toString
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }


    /**
     * 应用宽松反序列化策略。
     *
     * <p>
     * 用于增强接口输入的容错能力，适用于 Web 场景，
     * 避免因前端类型不规范导致反序列化失败。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyLenientDeserialization(ObjectMapper objectMapper) {

        // 允许字符串转数字（"1" -> 1）
        objectMapper.enable(MapperFeature.ALLOW_COERCION_OF_SCALARS);

        // 允许单值当数组使用（"a" -> ["a"]）
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // 允许空字符串当 null
        //objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        // 允许空数组当 null
        //objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

        // 忽略枚举非法值（避免直接报错）
        objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    }


    /**
     * 应用 JSON 读取容错特性。
     *
     * <p>
     * 用于兼容非标准 JSON 输入（常见于前端或第三方系统），
     * 提升系统整体兼容性。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyJsonReadFeature(ObjectMapper objectMapper) {

        // 允许 JSON 末尾存在多余逗号
        objectMapper.enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);

        // 允许 JSON 中带注释，方便开发阶段使用
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        // 允许字段名不带引号（可处理某些特殊格式的 JSON）
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        // 允许单引号作为 JSON 字符串的定界符（适用于某些特殊格式）
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        // 允许控制字符的转义（例如，`\n` 或 `\t`）
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

        // 允许反斜杠转义任何字符（如：`\\`）
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        // 允许无效的 UTF-8 字符（如果 JSON 编码不完全符合标准）
        objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);

        // 允许 JSON 中无序字段（通常是为了性能优化）
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);

    }

    /**
     * 统一数值序列化策略（安全 + 前端兼容）
     *
     * <p>
     * 解决 Java Long / BigInteger 在前端 JS 精度丢失问题，
     * </p>
     */
    private static void applyNumberSerialization(ObjectMapper objectMapper) {

        // 创建自定义模块，用于扩展数值序列化策略
        SimpleModule module = new SimpleModule();

        // 使用 ToStringSerializer，将数值序列化为字符串
        ToStringSerializer stringSerializer = ToStringSerializer.instance;

        // 注册 Long 包装类型序列化器
        module.addSerializer(Long.class, stringSerializer);

        // 注册 long 基本类型序列化器
        module.addSerializer(Long.TYPE, stringSerializer);

        // 注册 BigInteger 序列化器，避免精度丢失
        module.addSerializer(BigInteger.class, stringSerializer);

        // 防止 float/double 精度问题
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        // 防止 int 溢出
        objectMapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

        // 将模块注册到 ObjectMapper
        objectMapper.registerModule(module);
    }

    /**
     * 关闭 Web 场景不需要的稳定排序能力。
     *
     * <p>
     * Web 层更关注接口可读性与自然输出顺序，因此不强制属性和 Map 键排序。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void disableStableSerializationOptions(ObjectMapper objectMapper) {

        // 关闭属性字母排序，保留原始定义顺序，提高可读性
        objectMapper.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

        // 关闭 Map key 排序，避免影响前端展示顺序
        objectMapper.disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    /**
     * 启用默认多态能力，并限制反序列化来源类型范围。
     *
     * <p>
     * 仅允许业务包前缀和常用 JDK 容器类型参与 default typing，用于收敛反序列化攻击面。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyDefaultTyping(ObjectMapper objectMapper) {

        // 反序列化时遇到非法或未被允许的子类型直接失败（配合 PolymorphicTypeValidator 使用，增强多态反序列化安全性）
        objectMapper.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

        // 启用默认多态机制，用于保留对象类型信息（适用于 Object 或抽象类型）
        objectMapper.activateDefaultTyping(

                // 使用受限的多态校验器，控制可反序列化的类型范围
                buildPolymorphicTypeValidator(),

                // 仅对非 final 类启用类型信息
                ObjectMapper.DefaultTyping.NON_FINAL,

                // 以 JSON 属性形式写入类型信息（默认字段为 @class）
                JsonTypeInfo.As.PROPERTY
        );
    }

    /**
     * 为异常类型挂载混入配置。
     *
     * <p>
     * 该配置用于增强 Throwable 的序列化兼容性，并降低 cause 链和对象引用环路带来的问题。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyThrowableMixIn(ObjectMapper objectMapper) {

        // 为 Throwable 类型绑定混入类，增强异常对象序列化能力
        objectMapper.addMixIn(Throwable.class, ThrowableMixIn.class);
    }

    /**
     * 构建默认多态类型校验器。
     *
     * <p>
     * 只允许受信任业务包及常用 JDK 容器类型参与多态反序列化，避免宽松校验带来的安全风险。
     * </p>
     *
     * @return 多态类型校验器
     */
    private static PolymorphicTypeValidator buildPolymorphicTypeValidator() {

        // 创建多态类型校验器构建器
        BasicPolymorphicTypeValidator.Builder builder = BasicPolymorphicTypeValidator.builder();

        // 遍历受信任的业务包前缀
        for (String basePackage : TRUSTED_BASE_PACKAGES) {

            // 允许该包路径下的所有子类型参与多态反序列化
            builder.allowIfSubType(basePackage);
        }

        // 基础类型
        builder.allowIfSubType("java.lang");

        // 集合类型
        builder.allowIfSubType("java.util");

        // 时间类型
        builder.allowIfSubType("java.time");

        // 数值类型（BigDecimal / BigInteger）
        builder.allowIfSubType("java.math");

        // 构建并返回多态类型校验器
        return builder.build();
    }

    /**
     * 异常对象序列化混入配置。
     *
     * <p>
     * 该混入仅用于补充 Throwable 的序列化视图，不修改业务异常本身的代码结构。
     * </p>
     *
     * @author Ateng
     * @since 2026-04-13
     */
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            setterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            creatorVisibility = JsonAutoDetect.Visibility.NONE
    )
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    public static class ThrowableMixIn {
    }
}
```

#### 创建序列化器

```java
package local.ateng.java.redis.config;

import org.redisson.codec.JsonJacksonCodec;

/**
 * 自定义 Redisson Jackson 编解码器
 * 用于让 Redisson 普通对象序列化使用项目自定义的 ObjectMapper 配置。
 *
 * @author Ateng
 * @since 2026-04-27
 */
public class CustomJacksonCodec extends JsonJacksonCodec {

    /**
     * 创建自定义 Jackson 编解码器。
     * <p>
     * 这里使用 {@link JacksonObjectMapperFactory#buildStorageObjectMapper()}
     * 构建用于 Redis 存储的 ObjectMapper，便于统一处理时间格式、类型信息、
     * 空值策略、反序列化兼容性等配置。
     * </p>
     */
    public CustomJacksonCodec() {
        super(JacksonObjectMapperFactory.buildStorageObjectMapper());
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

`src/main/java/local/ateng/java/redis/service/RedissonService.java`

### 创建Service实现

`src/main/java/local/ateng/java/redis/service/impl/RedissonServiceImpl.java`



## 使用Redisson

### 基础能力测试控制器

用于演示 RedissonService 的 Key 管理、Bucket、计数器、分布式 ID 等基础能力。

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 基础能力测试控制器
 * 用于演示 RedissonService 的 Key 管理、Bucket、计数器、分布式 ID 等基础能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/basic")
@RequiredArgsConstructor
public class RedissonBasicController {

    private final RedissonService redissonService;

    /**
     * 获取 RedissonClient 基础信息。
     * <p>
     * curl "http://localhost:8080/redisson/basic/client"
     *
     * @return RedissonClient 信息
     */
    @GetMapping("/client")
    public Map<String, Object> client() {
        RedissonClient client = redissonService.getClient();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", client.getClass().getName());
        data.put("shutdown", client.isShutdown());
        data.put("shuttingDown", client.isShuttingDown());

        return ok(data);
    }

    /**
     * 判断 Key 是否存在。
     * <p>
     * curl "http://localhost:8080/redisson/basic/key/exists?key=user:1"
     *
     * @param key Redis 键
     * @return 是否存在
     */
    @GetMapping("/key/exists")
    public Map<String, Object> hasKey(@RequestParam String key) {
        checkKey(key);

        boolean exists = redissonService.hasKey(key);
        return ok(exists);
    }

    /**
     * 统计多个 Key 中存在的数量。
     * <p>
     * curl "http://localhost:8080/redisson/basic/key/count-exists?keys=user:1&keys=user:2"
     *
     * @param keys Redis 键集合
     * @return 存在数量
     */
    @GetMapping("/key/count-exists")
    public Map<String, Object> countExists(@RequestParam List<String> keys) {
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.countExists(keys.toArray(new String[0]));
        return ok(count);
    }

    /**
     * 删除指定 Key。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/basic/key?key=user:1"
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @DeleteMapping("/key")
    public Map<String, Object> deleteKey(@RequestParam String key) {
        checkKey(key);

        boolean deleted = redissonService.deleteKey(key);
        log.info("删除 Redis Key，key={}，deleted={}", key, deleted);
        return ok(deleted);
    }

    /**
     * 批量删除 Key。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/basic/keys?keys=user:1&keys=user:2"
     *
     * @param keys Redis 键集合
     * @return 删除数量
     */
    @DeleteMapping("/keys")
    public Map<String, Object> deleteKeys(@RequestParam List<String> keys) {
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.deleteKeys(keys);
        log.info("批量删除 Redis Key，keys={}，count={}", keys, count);
        return ok(count);
    }

    /**
     * 根据 pattern 删除 Key。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/basic/keys/pattern?pattern=test:*"
     *
     * @param pattern 通配符表达式
     * @return 删除数量
     */
    @DeleteMapping("/keys/pattern")
    public Map<String, Object> deleteByPattern(@RequestParam String pattern) {
        checkKey(pattern);

        long count = redissonService.deleteByPattern(pattern);
        log.info("根据 pattern 删除 Redis Key，pattern={}，count={}", pattern, count);
        return ok(count);
    }

    /**
     * 设置 Key 过期时间。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/key/expire?key=user:1&timeout=60"
     *
     * @param key     Redis 键
     * @param timeout 过期时间，单位秒
     * @return 是否设置成功
     */
    @PutMapping("/key/expire")
    public Map<String, Object> expire(@RequestParam String key,
                                      @RequestParam Long timeout) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeout) && timeout > 0, "timeout必须大于0");

        boolean success = redissonService.expire(key, timeout, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 获取 Key 剩余过期时间。
     * <p>
     * curl "http://localhost:8080/redisson/basic/key/ttl?key=user:1"
     *
     * @param key Redis 键
     * @return 剩余秒数，-1 表示永久，-2 表示不存在
     */
    @GetMapping("/key/ttl")
    public Map<String, Object> ttl(@RequestParam String key) {
        checkKey(key);

        long ttl = redissonService.getTtl(key, TimeUnit.SECONDS);
        return ok(ttl);
    }

    /**
     * 移除 Key 过期时间。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/key/persist?key=user:1"
     *
     * @param key Redis 键
     * @return 是否成功
     */
    @PutMapping("/key/persist")
    public Map<String, Object> persist(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.persist(key);
        return ok(success);
    }

    /**
     * 重命名 Key。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/key/rename?oldKey=user:1&newKey=user:100"
     *
     * @param oldKey 旧 Key
     * @param newKey 新 Key
     * @return 是否成功
     */
    @PutMapping("/key/rename")
    public Map<String, Object> rename(@RequestParam String oldKey,
                                      @RequestParam String newKey) {
        checkKey(oldKey);
        checkKey(newKey);

        boolean success = redissonService.renameKey(oldKey, newKey);
        return ok(success);
    }

    /**
     * 新 Key 不存在时重命名。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/key/rename-if-absent?oldKey=user:1&newKey=user:100"
     *
     * @param oldKey 旧 Key
     * @param newKey 新 Key
     * @return 是否成功
     */
    @PutMapping("/key/rename-if-absent")
    public Map<String, Object> renameIfAbsent(@RequestParam String oldKey,
                                              @RequestParam String newKey) {
        checkKey(oldKey);
        checkKey(newKey);

        boolean success = redissonService.renameKeyIfAbsent(oldKey, newKey);
        return ok(success);
    }

    /**
     * 查询匹配 pattern 的 Key。
     * <p>
     * curl "http://localhost:8080/redisson/basic/keys/scan?pattern=user:*&count=20"
     *
     * @param pattern 通配符表达式
     * @param count   最大返回数量
     * @return Key 集合
     */
    @GetMapping("/keys/scan")
    public Map<String, Object> scanKeys(@RequestParam String pattern,
                                        @RequestParam(defaultValue = "20") Integer count) {
        checkKey(pattern);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        return ok(redissonService.scanKeys(pattern, count));
    }

    /**
     * 获取 Key 类型。
     * <p>
     * curl "http://localhost:8080/redisson/basic/key/type?key=user:1"
     *
     * @param key Redis 键
     * @return 类型
     */
    @GetMapping("/key/type")
    public Map<String, Object> keyType(@RequestParam String key) {
        checkKey(key);

        String type = redissonService.getKeyType(key);
        return ok(type);
    }

    /**
     * 设置 Bucket 值。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/bucket/set?key=user:1&ttlSeconds=300" \\
     * -H "Content-Type: application/json" \\
     * -d '{"id":1,"name":"Ateng"}'
     *
     * @param key        Redis 键
     * @param ttlSeconds 过期秒数，不传则永久
     * @param value      值
     * @return 执行结果
     */
    @PostMapping("/bucket/set")
    public Map<String, Object> set(@RequestParam String key,
                                   @RequestParam(required = false) Long ttlSeconds,
                                   @RequestBody Object value) {
        checkKey(key);

        if (ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0) {
            redissonService.set(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            redissonService.set(key, value);
        }

        log.info("设置 Redis Bucket 成功，key={}，ttlSeconds={}", key, ttlSeconds);
        return ok(true);
    }

    /**
     * 获取 Bucket 值。
     * <p>
     * curl "http://localhost:8080/redisson/basic/bucket/get?key=user:1"
     *
     * @param key Redis 键
     * @return 值
     */
    @GetMapping("/bucket/get")
    public Map<String, Object> get(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.get(key, Object.class);
        return ok(value);
    }

    /**
     * Key 不存在时设置 Bucket 值。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/bucket/set-if-absent?key=lock:test&ttlSeconds=60" \\
     * -H "Content-Type: application/json" \\
     * -d '"value"'
     *
     * @param key        Redis 键
     * @param ttlSeconds 过期秒数
     * @param value      值
     * @return 是否设置成功
     */
    @PostMapping("/bucket/set-if-absent")
    public Map<String, Object> setIfAbsent(@RequestParam String key,
                                           @RequestParam Long ttlSeconds,
                                           @RequestBody Object value) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");

        boolean success = redissonService.setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds));
        return ok(success);
    }

    /**
     * Key 存在时设置 Bucket 值。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/bucket/set-if-exists?key=user:1&ttlSeconds=300" \\
     * -H "Content-Type: application/json" \\
     * -d '{"id":1,"name":"Ateng Updated"}'
     *
     * @param key        Redis 键
     * @param ttlSeconds 过期秒数，不传则不改过期时间
     * @param value      值
     * @return 是否设置成功
     */
    @PostMapping("/bucket/set-if-exists")
    public Map<String, Object> setIfExists(@RequestParam String key,
                                           @RequestParam(required = false) Long ttlSeconds,
                                           @RequestBody Object value) {
        checkKey(key);

        boolean success;
        if (ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0) {
            success = redissonService.setIfExists(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            success = redissonService.setIfExists(key, value);
        }

        return ok(success);
    }

    /**
     * 原子替换 Bucket 值并返回旧值。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/bucket/get-and-set?key=user:1" \\
     * -H "Content-Type: application/json" \\
     * -d '{"id":1,"name":"new"}'
     *
     * @param key   Redis 键
     * @param value 新值
     * @return 旧值
     */
    @PostMapping("/bucket/get-and-set")
    public Map<String, Object> getAndSet(@RequestParam String key,
                                         @RequestBody Object value) {
        checkKey(key);

        Object oldValue = redissonService.getAndSet(key, value, Object.class);
        return ok(oldValue);
    }

    /**
     * 获取并删除 Bucket 值。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/basic/bucket/get-and-delete?key=user:1"
     *
     * @param key Redis 键
     * @return 删除前的值
     */
    @DeleteMapping("/bucket/get-and-delete")
    public Map<String, Object> getAndDelete(@RequestParam String key) {
        checkKey(key);

        Object oldValue = redissonService.getAndDelete(key, Object.class);
        return ok(oldValue);
    }

    /**
     * 批量获取 Bucket 值。
     * <p>
     * curl "http://localhost:8080/redisson/basic/bucket/entries?keys=user:1&keys=user:2"
     *
     * @param keys Redis 键集合
     * @return Key-Value Map
     */
    @GetMapping("/bucket/entries")
    public Map<String, Object> entries(@RequestParam List<String> keys) {
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        Map<String, Object> data = redissonService.entries(keys);
        return ok(data);
    }

    /**
     * 获取 Bucket 值大小。
     * <p>
     * curl "http://localhost:8080/redisson/basic/bucket/size?key=user:1"
     *
     * @param key Redis 键
     * @return 字节大小
     */
    @GetMapping("/bucket/size")
    public Map<String, Object> size(@RequestParam String key) {
        checkKey(key);

        long size = redissonService.size(key);
        return ok(size);
    }

    /**
     * AtomicLong 自增。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/atomic-long/increment?key=count:like:1&delta=1"
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    @PostMapping("/atomic-long/increment")
    public Map<String, Object> increment(@RequestParam String key,
                                         @RequestParam(defaultValue = "1") Long delta) {
        checkKey(key);

        long value = redissonService.increment(key, delta);
        return ok(value);
    }

    /**
     * AtomicLong 自减。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/atomic-long/decrement?key=count:like:1&delta=1"
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    @PostMapping("/atomic-long/decrement")
    public Map<String, Object> decrement(@RequestParam String key,
                                         @RequestParam(defaultValue = "1") Long delta) {
        checkKey(key);

        long value = redissonService.decrement(key, delta);
        return ok(value);
    }

    /**
     * 设置 AtomicLong 值。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/atomic-long/set?key=count:like:1&value=100"
     *
     * @param key   Redis 键
     * @param value 值
     * @return 执行结果
     */
    @PutMapping("/atomic-long/set")
    public Map<String, Object> setAtomicLong(@RequestParam String key,
                                             @RequestParam Long value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        redissonService.setAtomicLong(key, value);
        return ok(true);
    }

    /**
     * 获取 AtomicLong 值。
     * <p>
     * curl "http://localhost:8080/redisson/basic/atomic-long/get?key=count:like:1"
     *
     * @param key Redis 键
     * @return 当前值
     */
    @GetMapping("/atomic-long/get")
    public Map<String, Object> getAtomicLong(@RequestParam String key) {
        checkKey(key);

        long value = redissonService.getAtomicLongValue(key);
        return ok(value);
    }

    /**
     * 重置 AtomicLong 值。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/basic/atomic-long/reset?key=count:like:1"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @PutMapping("/atomic-long/reset")
    public Map<String, Object> resetAtomicLong(@RequestParam String key) {
        checkKey(key);

        redissonService.resetAtomicLong(key);
        return ok(true);
    }

    /**
     * AtomicDouble 自增。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/atomic-double/increment?key=score:user:1&delta=1.5"
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    @PostMapping("/atomic-double/increment")
    public Map<String, Object> incrementDouble(@RequestParam String key,
                                               @RequestParam(defaultValue = "1") Double delta) {
        checkKey(key);

        double value = redissonService.incrementDouble(key, delta);
        return ok(value);
    }

    /**
     * AtomicDouble 自减。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/atomic-double/decrement?key=score:user:1&delta=1.5"
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    @PostMapping("/atomic-double/decrement")
    public Map<String, Object> decrementDouble(@RequestParam String key,
                                               @RequestParam(defaultValue = "1") Double delta) {
        checkKey(key);

        double value = redissonService.decrementDouble(key, delta);
        return ok(value);
    }

    /**
     * 初始化分布式 ID 生成器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/basic/id-generator/init?key=id:order&initialValue=1000&allocationSize=100"
     *
     * @param key            Redis 键
     * @param initialValue   初始值
     * @param allocationSize 分配步长
     * @return 是否初始化成功
     */
    @PostMapping("/id-generator/init")
    public Map<String, Object> idGeneratorInit(@RequestParam String key,
                                               @RequestParam(defaultValue = "1") Long initialValue,
                                               @RequestParam(defaultValue = "100") Long allocationSize) {
        checkKey(key);

        boolean success = redissonService.idGeneratorInit(key, initialValue, allocationSize);
        return ok(success);
    }

    /**
     * 获取下一个分布式 ID。
     * <p>
     * curl "http://localhost:8080/redisson/basic/id-generator/next?key=id:order"
     *
     * @param key Redis 键
     * @return ID
     */
    @GetMapping("/id-generator/next")
    public Map<String, Object> nextId(@RequestParam String key) {
        checkKey(key);

        long id = redissonService.nextId(key);
        return ok(id);
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }
}
```

### 数据结构测试控制器

用于演示 RedissonService 的 Hash、MapCache、List、Deque、Set、SetCache、ZSet 等能力。

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

/**
 * Redisson 数据结构测试控制器
 * 用于演示 RedissonService 的 Hash、MapCache、List、Deque、Set、SetCache、ZSet 等能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/data")
@RequiredArgsConstructor
public class RedissonDataStructureController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // Hash / Map
    // -------------------------------------------------------------------------

    /**
     * 设置 Hash 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/put?key=user:hash:1&field=name" \
     *   -H "Content-Type: application/json" \
     *   -d '"Ateng"'
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @return 执行结果
     */
    @PostMapping("/hash/put")
    public Map<String, Object> hPut(@RequestParam String key,
                                    @RequestParam String field,
                                    @RequestBody Object value) {
        checkKey(key);
        checkField(field);

        redissonService.hPut(key, field, value);
        log.info("Hash 字段写入成功，key={}，field={}", key, field);
        return ok(true);
    }

    /**
     * 批量设置 Hash 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/put-all?key=user:hash:1" \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"Ateng","age":18,"city":"Chongqing"}'
     *
     * @param key Redis 键
     * @param map 字段 Map
     * @return 执行结果
     */
    @PostMapping("/hash/put-all")
    public Map<String, Object> hPutAll(@RequestParam String key,
                                       @RequestBody Map<String, Object> map) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(map), "字段Map不能为空");

        redissonService.hPutAll(key, map);
        log.info("Hash 字段批量写入成功，key={}，size={}", key, map.size());
        return ok(true);
    }

    /**
     * 字段不存在时设置 Hash 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/put-if-absent?key=user:hash:1&field=email" \
     *   -H "Content-Type: application/json" \
     *   -d '"ateng@example.com"'
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @return 是否设置成功
     */
    @PostMapping("/hash/put-if-absent")
    public Map<String, Object> hPutIfAbsent(@RequestParam String key,
                                            @RequestParam String field,
                                            @RequestBody Object value) {
        checkKey(key);
        checkField(field);

        boolean success = redissonService.hPutIfAbsent(key, field, value);
        return ok(success);
    }

    /**
     * 获取 Hash 字段值。
     *
     * curl "http://localhost:8080/redisson/data/hash/get?key=user:hash:1&field=name"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 字段值
     */
    @GetMapping("/hash/get")
    public Map<String, Object> hGet(@RequestParam String key,
                                    @RequestParam String field) {
        checkKey(key);
        checkField(field);

        Object value = redissonService.hGet(key, field, Object.class);
        return ok(value);
    }

    /**
     * 批量获取 Hash 字段值。
     *
     * curl "http://localhost:8080/redisson/data/hash/multi-get?key=user:hash:1&fields=name&fields=age"
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 字段值 Map
     */
    @GetMapping("/hash/multi-get")
    public Map<String, Object> hMultiGet(@RequestParam String key,
                                         @RequestParam List<String> fields) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(fields), "fields不能为空");

        Map<String, Object> data = redissonService.hMultiGet(key, fields);
        return ok(data);
    }

    /**
     * 删除 Hash 字段。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/hash/delete?key=user:hash:1&fields=name&fields=age"
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 删除数量
     */
    @DeleteMapping("/hash/delete")
    public Map<String, Object> hDelete(@RequestParam String key,
                                       @RequestParam List<String> fields) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(fields), "fields不能为空");

        long count = redissonService.hDelete(key, fields.toArray(new String[0]));
        log.info("Hash 字段删除成功，key={}，fields={}，count={}", key, fields, count);
        return ok(count);
    }

    /**
     * 判断 Hash 字段是否存在。
     *
     * curl "http://localhost:8080/redisson/data/hash/has-key?key=user:hash:1&field=name"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 是否存在
     */
    @GetMapping("/hash/has-key")
    public Map<String, Object> hHasKey(@RequestParam String key,
                                       @RequestParam String field) {
        checkKey(key);
        checkField(field);

        boolean exists = redissonService.hHasKey(key, field);
        return ok(exists);
    }

    /**
     * 获取 Hash 全部字段和值。
     *
     * curl "http://localhost:8080/redisson/data/hash/entries?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 字段值 Map
     */
    @GetMapping("/hash/entries")
    public Map<String, Object> hEntries(@RequestParam String key) {
        checkKey(key);

        Map<String, Object> data = redissonService.hEntries(key);
        return ok(data);
    }

    /**
     * 获取 Hash 全部字段名。
     *
     * curl "http://localhost:8080/redisson/data/hash/keys?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 字段名集合
     */
    @GetMapping("/hash/keys")
    public Map<String, Object> hKeys(@RequestParam String key) {
        checkKey(key);

        Set<String> data = redissonService.hKeys(key);
        return ok(data);
    }

    /**
     * 获取 Hash 全部字段值。
     *
     * curl "http://localhost:8080/redisson/data/hash/values?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 字段值集合
     */
    @GetMapping("/hash/values")
    public Map<String, Object> hValues(@RequestParam String key) {
        checkKey(key);

        Collection<Object> data = redissonService.hValues(key);
        return ok(data);
    }

    /**
     * 获取 Hash 字段数量。
     *
     * curl "http://localhost:8080/redisson/data/hash/size?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 字段数量
     */
    @GetMapping("/hash/size")
    public Map<String, Object> hSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.hSize(key);
        return ok(size);
    }

    /**
     * Hash 字段整数自增。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/increment?key=user:hash:1&field=count&delta=1"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    @PostMapping("/hash/increment")
    public Map<String, Object> hIncrement(@RequestParam String key,
                                          @RequestParam String field,
                                          @RequestParam(defaultValue = "1") Long delta) {
        checkKey(key);
        checkField(field);

        long value = redissonService.hIncrement(key, field, delta);
        return ok(value);
    }

    /**
     * Hash 字段浮点数自增。
     *
     * curl -X POST "http://localhost:8080/redisson/data/hash/increment-double?key=user:hash:1&field=score&delta=1.5"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    @PostMapping("/hash/increment-double")
    public Map<String, Object> hIncrementDouble(@RequestParam String key,
                                                @RequestParam String field,
                                                @RequestParam(defaultValue = "1") Double delta) {
        checkKey(key);
        checkField(field);

        double value = redissonService.hIncrementDouble(key, field, delta);
        return ok(value);
    }

    /**
     * 清空 Hash。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/hash/clear?key=user:hash:1"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/hash/clear")
    public Map<String, Object> hClear(@RequestParam String key) {
        checkKey(key);

        redissonService.hClear(key);
        log.info("Hash 清空成功，key={}", key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // MapCache
    // -------------------------------------------------------------------------

    /**
     * 设置 MapCache 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/data/map-cache/put?key=user:cache:1&field=profile&ttlSeconds=300" \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"Ateng","age":18}'
     *
     * @param key            Redis 键
     * @param field          字段名
     * @param ttlSeconds     字段 TTL 秒数
     * @param maxIdleSeconds 最大空闲秒数，可选
     * @param value          字段值
     * @return 执行结果
     */
    @PostMapping("/map-cache/put")
    public Map<String, Object> hcPut(@RequestParam String key,
                                     @RequestParam String field,
                                     @RequestParam Long ttlSeconds,
                                     @RequestParam(required = false) Long maxIdleSeconds,
                                     @RequestBody Object value) {
        checkKey(key);
        checkField(field);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");

        if (ObjectUtil.isNotNull(maxIdleSeconds) && maxIdleSeconds > 0) {
            redissonService.hcPut(key, field, value, Duration.ofSeconds(ttlSeconds), Duration.ofSeconds(maxIdleSeconds));
        } else {
            redissonService.hcPut(key, field, value, Duration.ofSeconds(ttlSeconds));
        }

        log.info("MapCache 字段写入成功，key={}，field={}，ttlSeconds={}，maxIdleSeconds={}",
                key, field, ttlSeconds, maxIdleSeconds);
        return ok(true);
    }

    /**
     * 获取 MapCache 字段值。
     *
     * curl "http://localhost:8080/redisson/data/map-cache/get?key=user:cache:1&field=profile"
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 字段值
     */
    @GetMapping("/map-cache/get")
    public Map<String, Object> hcGet(@RequestParam String key,
                                     @RequestParam String field) {
        checkKey(key);
        checkField(field);

        Object value = redissonService.hcGet(key, field, Object.class);
        return ok(value);
    }

    /**
     * 删除 MapCache 字段。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/map-cache/delete?key=user:cache:1&fields=profile"
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 删除数量
     */
    @DeleteMapping("/map-cache/delete")
    public Map<String, Object> hcDelete(@RequestParam String key,
                                        @RequestParam List<String> fields) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(fields), "fields不能为空");

        long count = redissonService.hcDelete(key, fields.toArray(new String[0]));
        return ok(count);
    }

    // -------------------------------------------------------------------------
    // List / Deque
    // -------------------------------------------------------------------------

    /**
     * 从左侧压入列表。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/left-push?key=list:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 执行结果
     */
    @PostMapping("/list/left-push")
    public Map<String, Object> lLeftPush(@RequestParam String key,
                                         @RequestBody Object value) {
        checkKey(key);

        redissonService.lLeftPush(key, value);
        return ok(true);
    }

    /**
     * 从右侧压入列表。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/right-push?key=list:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"B"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 执行结果
     */
    @PostMapping("/list/right-push")
    public Map<String, Object> lRightPush(@RequestParam String key,
                                          @RequestBody Object value) {
        checkKey(key);

        redissonService.lRightPush(key, value);
        return ok(true);
    }

    /**
     * 批量从右侧压入列表。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/right-push-all?key=list:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B","C"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 执行结果
     */
    @PostMapping("/list/right-push-all")
    public Map<String, Object> lRightPushAll(@RequestParam String key,
                                             @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        redissonService.lRightPushAll(key, values);
        return ok(true);
    }

    /**
     * 从左侧弹出列表元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/left-pop?key=list:test"
     *
     * @param key Redis 键
     * @return 弹出的元素
     */
    @PostMapping("/list/left-pop")
    public Map<String, Object> lLeftPop(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.lLeftPop(key);
        return ok(value);
    }

    /**
     * 从右侧弹出列表元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/right-pop?key=list:test"
     *
     * @param key Redis 键
     * @return 弹出的元素
     */
    @PostMapping("/list/right-pop")
    public Map<String, Object> lRightPop(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.lRightPop(key);
        return ok(value);
    }

    /**
     * 获取列表范围。
     *
     * curl "http://localhost:8080/redisson/data/list/range?key=list:test&start=0&end=-1"
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素列表
     */
    @GetMapping("/list/range")
    public Map<String, Object> lRange(@RequestParam String key,
                                      @RequestParam(defaultValue = "0") Long start,
                                      @RequestParam(defaultValue = "-1") Long end) {
        checkKey(key);

        List<Object> data = redissonService.lRange(key, start, end);
        return ok(data);
    }

    /**
     * 获取列表长度。
     *
     * curl "http://localhost:8080/redisson/data/list/size?key=list:test"
     *
     * @param key Redis 键
     * @return 长度
     */
    @GetMapping("/list/size")
    public Map<String, Object> lSize(@RequestParam String key) {
        checkKey(key);

        long size = redissonService.lSize(key);
        return ok(size);
    }

    /**
     * 删除列表元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/list/remove?key=list:test&count=0" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param count 删除数量规则
     * @param value 元素
     * @return 删除数量
     */
    @PostMapping("/list/remove")
    public Map<String, Object> lRemove(@RequestParam String key,
                                       @RequestParam(defaultValue = "0") Long count,
                                       @RequestBody Object value) {
        checkKey(key);

        long removed = redissonService.lRemove(key, count, value);
        return ok(removed);
    }

    /**
     * 获取列表指定索引元素。
     *
     * curl "http://localhost:8080/redisson/data/list/index?key=list:test&index=0"
     *
     * @param key   Redis 键
     * @param index 索引
     * @return 元素
     */
    @GetMapping("/list/index")
    public Map<String, Object> lIndex(@RequestParam String key,
                                      @RequestParam Long index) {
        checkKey(key);
        Assert.notNull(index, "index不能为空");

        Object value = redissonService.lIndex(key, index);
        return ok(value);
    }

    /**
     * 设置列表指定索引元素。
     *
     * curl -X PUT "http://localhost:8080/redisson/data/list/set?key=list:test&index=0" \
     *   -H "Content-Type: application/json" \
     *   -d '"NEW"'
     *
     * @param key   Redis 键
     * @param index 索引
     * @param value 元素
     * @return 执行结果
     */
    @PutMapping("/list/set")
    public Map<String, Object> lSet(@RequestParam String key,
                                    @RequestParam Long index,
                                    @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(index, "index不能为空");

        redissonService.lSet(key, index, value);
        return ok(true);
    }

    /**
     * 裁剪列表范围。
     *
     * curl -X PUT "http://localhost:8080/redisson/data/list/trim?key=list:test&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     * @return 执行结果
     */
    @PutMapping("/list/trim")
    public Map<String, Object> lTrim(@RequestParam String key,
                                     @RequestParam Integer start,
                                     @RequestParam Integer end) {
        checkKey(key);
        Assert.notNull(start, "start不能为空");
        Assert.notNull(end, "end不能为空");

        redissonService.lTrim(key, start, end);
        return ok(true);
    }

    /**
     * 清空列表。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/list/clear?key=list:test"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/list/clear")
    public Map<String, Object> lClear(@RequestParam String key) {
        checkKey(key);

        redissonService.lClear(key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // Set / SetCache
    // -------------------------------------------------------------------------

    /**
     * 添加集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/add?key=set:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B","C"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @PostMapping("/set/add")
    public Map<String, Object> sAdd(@RequestParam String key,
                                    @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.sAdd(key, values);
        return ok(success);
    }

    /**
     * 添加 SetCache 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set-cache/add?key=set:cache:test&ttlSeconds=60" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key        Redis 键
     * @param ttlSeconds 元素 TTL 秒数
     * @param value      元素
     * @return 是否添加成功
     */
    @PostMapping("/set-cache/add")
    public Map<String, Object> scAdd(@RequestParam String key,
                                     @RequestParam Long ttlSeconds,
                                     @RequestBody Object value) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");

        boolean success = redissonService.scAdd(key, value, Duration.ofSeconds(ttlSeconds));
        return ok(success);
    }

    /**
     * 判断集合是否包含元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/is-member?key=set:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否包含
     */
    @PostMapping("/set/is-member")
    public Map<String, Object> sIsMember(@RequestParam String key,
                                         @RequestBody Object value) {
        checkKey(key);

        boolean exists = redissonService.sIsMember(key, value);
        return ok(exists);
    }

    /**
     * 获取集合全部元素。
     *
     * curl "http://localhost:8080/redisson/data/set/members?key=set:test"
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @GetMapping("/set/members")
    public Map<String, Object> sMembers(@RequestParam String key) {
        checkKey(key);

        Set<Object> data = redissonService.sMembers(key);
        return ok(data);
    }

    /**
     * 获取集合大小。
     *
     * curl "http://localhost:8080/redisson/data/set/size?key=set:test"
     *
     * @param key Redis 键
     * @return 集合大小
     */
    @GetMapping("/set/size")
    public Map<String, Object> sSize(@RequestParam String key) {
        checkKey(key);

        long size = redissonService.sSize(key);
        return ok(size);
    }

    /**
     * 随机弹出集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/pop?key=set:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/set/pop")
    public Map<String, Object> sPop(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.sPop(key);
        return ok(value);
    }

    /**
     * 删除集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/remove?key=set:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否删除成功
     */
    @PostMapping("/set/remove")
    public Map<String, Object> sRemove(@RequestParam String key,
                                       @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.sRemove(key, values.toArray());
        return ok(success);
    }

    /**
     * 随机获取集合元素但不删除。
     *
     * curl "http://localhost:8080/redisson/data/set/random-member?key=set:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @GetMapping("/set/random-member")
    public Map<String, Object> sRandomMember(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.sRandomMember(key);
        return ok(value);
    }

    /**
     * 随机获取多个集合元素。
     *
     * curl "http://localhost:8080/redisson/data/set/random-members?key=set:test&count=2"
     *
     * @param key   Redis 键
     * @param count 数量
     * @return 元素集合
     */
    @GetMapping("/set/random-members")
    public Map<String, Object> sRandomMembers(@RequestParam String key,
                                              @RequestParam Integer count) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        Set<Object> data = redissonService.sRandomMembers(key, count);
        return ok(data);
    }

    /**
     * 获取两个集合的并集。
     *
     * curl "http://localhost:8080/redisson/data/set/union?key1=set:a&key2=set:b"
     *
     * @param key1 第一个 Key
     * @param key2 第二个 Key
     * @return 并集
     */
    @GetMapping("/set/union")
    public Map<String, Object> sUnion(@RequestParam String key1,
                                      @RequestParam String key2) {
        checkKey(key1);
        checkKey(key2);

        Set<Object> data = redissonService.sUnion(key1, key2);
        return ok(data);
    }

    /**
     * 获取两个集合的交集。
     *
     * curl "http://localhost:8080/redisson/data/set/intersect?key1=set:a&key2=set:b"
     *
     * @param key1 第一个 Key
     * @param key2 第二个 Key
     * @return 交集
     */
    @GetMapping("/set/intersect")
    public Map<String, Object> sIntersect(@RequestParam String key1,
                                          @RequestParam String key2) {
        checkKey(key1);
        checkKey(key2);

        Set<Object> data = redissonService.sIntersect(key1, key2);
        return ok(data);
    }

    /**
     * 获取两个集合的差集。
     *
     * curl "http://localhost:8080/redisson/data/set/difference?key1=set:a&key2=set:b"
     *
     * @param key1 第一个 Key
     * @param key2 第二个 Key
     * @return 差集
     */
    @GetMapping("/set/difference")
    public Map<String, Object> sDifference(@RequestParam String key1,
                                           @RequestParam String key2) {
        checkKey(key1);
        checkKey(key2);

        Set<Object> data = redissonService.sDifference(key1, key2);
        return ok(data);
    }

    /**
     * 并集存储。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/union-store?destKey=set:dest&keys=set:a&keys=set:b"
     *
     * @param destKey 目标 Key
     * @param keys    源 Key 集合
     * @return 存储数量
     */
    @PostMapping("/set/union-store")
    public Map<String, Object> sUnionStore(@RequestParam String destKey,
                                           @RequestParam List<String> keys) {
        checkKey(destKey);
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.sUnionStore(destKey, keys.toArray(new String[0]));
        return ok(count);
    }

    /**
     * 交集存储。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/intersect-store?destKey=set:dest&keys=set:a&keys=set:b"
     *
     * @param destKey 目标 Key
     * @param keys    源 Key 集合
     * @return 存储数量
     */
    @PostMapping("/set/intersect-store")
    public Map<String, Object> sIntersectStore(@RequestParam String destKey,
                                               @RequestParam List<String> keys) {
        checkKey(destKey);
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.sIntersectStore(destKey, keys.toArray(new String[0]));
        return ok(count);
    }

    /**
     * 差集存储。
     *
     * curl -X POST "http://localhost:8080/redisson/data/set/difference-store?destKey=set:dest&keys=set:a&keys=set:b"
     *
     * @param destKey 目标 Key
     * @param keys    源 Key 集合
     * @return 存储数量
     */
    @PostMapping("/set/difference-store")
    public Map<String, Object> sDifferenceStore(@RequestParam String destKey,
                                                @RequestParam List<String> keys) {
        checkKey(destKey);
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.sDifferenceStore(destKey, keys.toArray(new String[0]));
        return ok(count);
    }

    // -------------------------------------------------------------------------
    // ZSet / 排行榜
    // -------------------------------------------------------------------------

    /**
     * 添加 ZSet 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/add?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"value":"user:1","score":100}'
     *
     * @param key     Redis 键
     * @param request 请求体
     * @return 是否新增
     */
    @PostMapping("/zset/add")
    public Map<String, Object> zAdd(@RequestParam String key,
                                    @RequestBody ZSetValueScoreRequest request) {
        checkKey(key);
        Assert.notNull(request, "请求体不能为空");
        Assert.notNull(request.getValue(), "value不能为空");
        Assert.notNull(request.getScore(), "score不能为空");

        boolean success = redissonService.zAdd(key, request.getValue(), request.getScore());
        return ok(success);
    }

    /**
     * 批量添加 ZSet 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/add-all?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"scoreMap":{"user:1":100,"user:2":90}}'
     *
     * @param key     Redis 键
     * @param request 请求体
     * @return 新增数量
     */
    @PostMapping("/zset/add-all")
    public Map<String, Object> zAddAll(@RequestParam String key,
                                       @RequestBody ZSetAddAllRequest request) {
        checkKey(key);
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getScoreMap()), "scoreMap不能为空");

        int count = redissonService.zAddAll(key, request.getScoreMap());
        return ok(count);
    }

    /**
     * 删除 ZSet 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/remove?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '["user:1","user:2"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否删除成功
     */
    @PostMapping("/zset/remove")
    public Map<String, Object> zRemove(@RequestParam String key,
                                       @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.zRemove(key, values.toArray());
        return ok(success);
    }

    /**
     * 获取 ZSet 元素分数。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/score?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 分数
     */
    @PostMapping("/zset/score")
    public Map<String, Object> zScore(@RequestParam String key,
                                      @RequestBody Object value) {
        checkKey(key);

        Double score = redissonService.zScore(key, value);
        return ok(score);
    }

    /**
     * 获取 ZSet 升序排名。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/rank?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    @PostMapping("/zset/rank")
    public Map<String, Object> zRank(@RequestParam String key,
                                     @RequestBody Object value) {
        checkKey(key);

        Integer rank = redissonService.zRank(key, value);
        return ok(rank);
    }

    /**
     * 获取 ZSet 降序排名。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/rev-rank?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    @PostMapping("/zset/rev-rank")
    public Map<String, Object> zRevRank(@RequestParam String key,
                                        @RequestBody Object value) {
        checkKey(key);

        Integer rank = redissonService.zRevRank(key, value);
        return ok(rank);
    }

    /**
     * 获取 ZSet 分数区间元素。
     *
     * curl "http://localhost:8080/redisson/data/zset/range-by-score?key=rank:score&min=0&max=100"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    @GetMapping("/zset/range-by-score")
    public Map<String, Object> zRangeByScore(@RequestParam String key,
                                             @RequestParam Double min,
                                             @RequestParam Double max) {
        checkKey(key);
        Assert.notNull(min, "min不能为空");
        Assert.notNull(max, "max不能为空");

        Set<Object> data = redissonService.zRangeByScore(key, min, max);
        return ok(data);
    }

    /**
     * 获取 ZSet 分数区间元素并分页。
     *
     * curl "http://localhost:8080/redisson/data/zset/range-by-score-page?key=rank:score&min=0&max=100&offset=0&count=10"
     *
     * @param key    Redis 键
     * @param min    最小分数
     * @param max    最大分数
     * @param offset 偏移量
     * @param count  数量
     * @return 元素集合
     */
    @GetMapping("/zset/range-by-score-page")
    public Map<String, Object> zRangeByScorePage(@RequestParam String key,
                                                 @RequestParam Double min,
                                                 @RequestParam Double max,
                                                 @RequestParam(defaultValue = "0") Integer offset,
                                                 @RequestParam(defaultValue = "10") Integer count) {
        checkKey(key);

        Collection<Object> data = redissonService.zRangeByScore(key, min, max, offset, count);
        return ok(data);
    }

    /**
     * 获取 ZSet 分数区间元素及分数。
     *
     * curl "http://localhost:8080/redisson/data/zset/range-by-score-with-scores?key=rank:score&min=0&max=100"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    @GetMapping("/zset/range-by-score-with-scores")
    public Map<String, Object> zRangeByScoreWithScores(@RequestParam String key,
                                                       @RequestParam Double min,
                                                       @RequestParam Double max) {
        checkKey(key);

        Map<Object, Double> data = redissonService.zRangeByScoreWithScores(key, min, max);
        return ok(data);
    }

    /**
     * 获取 ZSet 降序分数区间元素及分数。
     *
     * curl "http://localhost:8080/redisson/data/zset/rev-range-by-score-with-scores?key=rank:score&min=0&max=100"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    @GetMapping("/zset/rev-range-by-score-with-scores")
    public Map<String, Object> zRevRangeByScoreWithScores(@RequestParam String key,
                                                          @RequestParam Double min,
                                                          @RequestParam Double max) {
        checkKey(key);

        Map<Object, Double> data = redissonService.zRevRangeByScoreWithScores(key, min, max);
        return ok(data);
    }

    /**
     * 获取 ZSet 排名区间元素。
     *
     * curl "http://localhost:8080/redisson/data/zset/range?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    @GetMapping("/zset/range")
    public Map<String, Object> zRange(@RequestParam String key,
                                      @RequestParam(defaultValue = "0") Integer start,
                                      @RequestParam(defaultValue = "9") Integer end) {
        checkKey(key);

        Set<Object> data = redissonService.zRange(key, start, end);
        return ok(data);
    }

    /**
     * 获取 ZSet 排名区间元素及分数。
     *
     * curl "http://localhost:8080/redisson/data/zset/range-with-scores?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    @GetMapping("/zset/range-with-scores")
    public Map<String, Object> zRangeWithScores(@RequestParam String key,
                                                @RequestParam(defaultValue = "0") Integer start,
                                                @RequestParam(defaultValue = "9") Integer end) {
        checkKey(key);

        Map<Object, Double> data = redissonService.zRangeWithScores(key, start, end);
        return ok(data);
    }

    /**
     * 获取 ZSet 降序排名区间元素。
     *
     * curl "http://localhost:8080/redisson/data/zset/rev-range?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    @GetMapping("/zset/rev-range")
    public Map<String, Object> zRevRange(@RequestParam String key,
                                         @RequestParam(defaultValue = "0") Integer start,
                                         @RequestParam(defaultValue = "9") Integer end) {
        checkKey(key);

        Set<Object> data = redissonService.zRevRange(key, start, end);
        return ok(data);
    }

    /**
     * 获取 ZSet 降序排名区间元素及分数。
     *
     * curl "http://localhost:8080/redisson/data/zset/rev-range-with-scores?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    @GetMapping("/zset/rev-range-with-scores")
    public Map<String, Object> zRevRangeWithScores(@RequestParam String key,
                                                   @RequestParam(defaultValue = "0") Integer start,
                                                   @RequestParam(defaultValue = "9") Integer end) {
        checkKey(key);

        Map<Object, Double> data = redissonService.zRevRangeWithScores(key, start, end);
        return ok(data);
    }

    /**
     * ZSet 元素分数自增。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/increment-score?key=rank:score" \
     *   -H "Content-Type: application/json" \
     *   -d '{"value":"user:1","score":10}'
     *
     * @param key     Redis 键
     * @param request 请求体
     * @return 最新分数
     */
    @PostMapping("/zset/increment-score")
    public Map<String, Object> zIncrBy(@RequestParam String key,
                                       @RequestBody ZSetValueScoreRequest request) {
        checkKey(key);
        Assert.notNull(request, "请求体不能为空");
        Assert.notNull(request.getValue(), "value不能为空");
        Assert.notNull(request.getScore(), "score不能为空");

        Double score = redissonService.zIncrBy(key, request.getValue(), request.getScore());
        return ok(score);
    }

    /**
     * 获取 ZSet 元素数量。
     *
     * curl "http://localhost:8080/redisson/data/zset/card?key=rank:score"
     *
     * @param key Redis 键
     * @return 数量
     */
    @GetMapping("/zset/card")
    public Map<String, Object> zCard(@RequestParam String key) {
        checkKey(key);

        int count = redissonService.zCard(key);
        return ok(count);
    }

    /**
     * 获取 ZSet 指定分数区间元素数量。
     *
     * curl "http://localhost:8080/redisson/data/zset/count?key=rank:score&min=0&max=100"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 数量
     */
    @GetMapping("/zset/count")
    public Map<String, Object> zCount(@RequestParam String key,
                                      @RequestParam Double min,
                                      @RequestParam Double max) {
        checkKey(key);

        long count = redissonService.zCount(key, min, max);
        return ok(count);
    }

    /**
     * 删除 ZSet 指定分数区间元素。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/zset/remove-range-by-score?key=rank:score&min=0&max=10"
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 删除数量
     */
    @DeleteMapping("/zset/remove-range-by-score")
    public Map<String, Object> zRemoveRangeByScore(@RequestParam String key,
                                                   @RequestParam Double min,
                                                   @RequestParam Double max) {
        checkKey(key);

        long count = redissonService.zRemoveRangeByScore(key, min, max);
        return ok(count);
    }

    /**
     * 删除 ZSet 指定排名区间元素。
     *
     * curl -X DELETE "http://localhost:8080/redisson/data/zset/remove-range-by-rank?key=rank:score&start=0&end=9"
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 删除数量
     */
    @DeleteMapping("/zset/remove-range-by-rank")
    public Map<String, Object> zRemoveRangeByRank(@RequestParam String key,
                                                  @RequestParam Integer start,
                                                  @RequestParam Integer end) {
        checkKey(key);

        long count = redissonService.zRemoveRangeByRank(key, start, end);
        return ok(count);
    }

    /**
     * 弹出 ZSet 分数最小元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/pop-first?key=rank:score"
     *
     * @param key Redis 键
     * @return 元素和分数
     */
    @PostMapping("/zset/pop-first")
    public Map<String, Object> zPopFirst(@RequestParam String key) {
        checkKey(key);

        ScoredEntry<Object> entry = redissonService.zPopFirst(key);
        return ok(scoredEntryToMap(entry));
    }

    /**
     * 弹出 ZSet 分数最大元素。
     *
     * curl -X POST "http://localhost:8080/redisson/data/zset/pop-last?key=rank:score"
     *
     * @param key Redis 键
     * @return 元素和分数
     */
    @PostMapping("/zset/pop-last")
    public Map<String, Object> zPopLast(@RequestParam String key) {
        checkKey(key);

        ScoredEntry<Object> entry = redissonService.zPopLast(key);
        return ok(scoredEntryToMap(entry));
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 校验字段名。
     *
     * @param field 字段名
     */
    private void checkField(String field) {
        Assert.isTrue(StrUtil.isNotBlank(field), "字段名不能为空");
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

    /**
     * 转换 ScoredEntry。
     *
     * @param entry ScoredEntry
     * @return Map
     */
    private Map<String, Object> scoredEntryToMap(ScoredEntry<Object> entry) {
        if (ObjectUtil.isNull(entry)) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", entry.getValue());
        result.put("score", entry.getScore());
        return result;
    }

    /**
     * ZSet 元素分数请求体。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class ZSetValueScoreRequest {

        /**
         * 元素值。
         */
        private Object value;

        /**
         * 分数。
         */
        private Double score;

    }

    /**
     * ZSet 批量添加请求体。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class ZSetAddAllRequest {

        /**
         * 元素分数 Map。
         */
        private Map<Object, Double> scoreMap;

    }

}
```

### 分布式能力测试控制器

用于演示 RedissonService 的分布式锁、读写锁、闭锁、信号量、限流器、布隆过滤器等能力。

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式能力测试控制器
 * 用于演示 RedissonService 的分布式锁、读写锁、闭锁、信号量、限流器、布隆过滤器等能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/distributed")
@RequiredArgsConstructor
public class RedissonDistributedController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // Lock / FairLock / SpinLock
    // -------------------------------------------------------------------------

    /**
     * 执行带可重入锁的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/lock/execute?lockKey=lock:demo&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 执行结果
     */
    @PostMapping("/lock/execute")
    public Map<String, Object> executeWithLock(@RequestParam String lockKey,
                                               @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkBusinessMillis(businessMillis);

        redissonService.executeWithLock(lockKey, () -> {
            log.info("执行可重入锁保护的业务，lockKey={}，businessMillis={}", lockKey, businessMillis);
            ThreadUtil.sleep(businessMillis);
        });

        return ok("可重入锁业务执行完成");
    }

    /**
     * 尝试执行带可重入锁的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/lock/try-execute?lockKey=lock:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数，传 0 或负数表示使用 watchdog
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/lock/try-execute")
    public Map<String, Object> tryExecuteWithLock(@RequestParam String lockKey,
                                                  @RequestParam(defaultValue = "3") Long waitSeconds,
                                                  @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                  @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        Assert.isTrue(ObjectUtil.isNotNull(waitSeconds) && waitSeconds >= 0, "waitSeconds不能小于0");
        Assert.notNull(leaseSeconds, "leaseSeconds不能为空");
        checkBusinessMillis(businessMillis);

        boolean success = redissonService.tryExecuteWithLock(
                lockKey,
                waitSeconds,
                leaseSeconds,
                TimeUnit.SECONDS,
                () -> {
                    log.info("执行可重入锁保护的业务，lockKey={}，businessMillis={}", lockKey, businessMillis);
                    ThreadUtil.sleep(businessMillis);
                }
        );

        return ok(success);
    }

    /**
     * 执行带公平锁的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/fair-lock/try-execute?lockKey=lock:fair:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/fair-lock/try-execute")
    public Map<String, Object> tryExecuteWithFairLock(@RequestParam String lockKey,
                                                      @RequestParam(defaultValue = "3") Long waitSeconds,
                                                      @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                      @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkLockTime(waitSeconds, leaseSeconds);
        checkBusinessMillis(businessMillis);

        boolean success = executeWithRLock(
                lockKey,
                redissonService.getFairLock(lockKey),
                waitSeconds,
                leaseSeconds,
                businessMillis
        );

        return ok(success);
    }

    /**
     * 执行带自旋锁的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/spin-lock/try-execute?lockKey=lock:spin:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/spin-lock/try-execute")
    public Map<String, Object> tryExecuteWithSpinLock(@RequestParam String lockKey,
                                                      @RequestParam(defaultValue = "3") Long waitSeconds,
                                                      @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                      @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkLockTime(waitSeconds, leaseSeconds);
        checkBusinessMillis(businessMillis);

        boolean success = executeWithRLock(
                lockKey,
                redissonService.getSpinLock(lockKey),
                waitSeconds,
                leaseSeconds,
                businessMillis
        );

        return ok(success);
    }

    /**
     * 查询锁状态。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/lock/status?lockKey=lock:demo"
     *
     * @param lockKey 锁 Key
     * @return 锁状态
     */
    @GetMapping("/lock/status")
    public Map<String, Object> lockStatus(@RequestParam String lockKey) {
        checkKey(lockKey);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("locked", redissonService.isLocked(lockKey));
        data.put("heldByCurrentThread", redissonService.isHeldByCurrentThread(lockKey));
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // ReadWriteLock
    // -------------------------------------------------------------------------

    /**
     * 执行读锁保护的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/read-write-lock/read?lockKey=rw:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/read-write-lock/read")
    public Map<String, Object> executeWithReadLock(@RequestParam String lockKey,
                                                   @RequestParam(defaultValue = "3") Long waitSeconds,
                                                   @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                   @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkLockTime(waitSeconds, leaseSeconds);
        checkBusinessMillis(businessMillis);

        boolean success = executeWithRLock(
                lockKey + ":read",
                redissonService.getReadWriteLock(lockKey).readLock(),
                waitSeconds,
                leaseSeconds,
                businessMillis
        );

        return ok(success);
    }

    /**
     * 执行写锁保护的业务。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/read-write-lock/write?lockKey=rw:demo&waitSeconds=3&leaseSeconds=10&businessMillis=1000"
     *
     * @param lockKey        锁 Key
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时，单位毫秒
     * @return 是否执行成功
     */
    @PostMapping("/read-write-lock/write")
    public Map<String, Object> executeWithWriteLock(@RequestParam String lockKey,
                                                    @RequestParam(defaultValue = "3") Long waitSeconds,
                                                    @RequestParam(defaultValue = "10") Long leaseSeconds,
                                                    @RequestParam(defaultValue = "1000") Long businessMillis) {
        checkKey(lockKey);
        checkLockTime(waitSeconds, leaseSeconds);
        checkBusinessMillis(businessMillis);

        boolean success = executeWithRLock(
                lockKey + ":write",
                redissonService.getReadWriteLock(lockKey).writeLock(),
                waitSeconds,
                leaseSeconds,
                businessMillis
        );

        return ok(success);
    }

    // -------------------------------------------------------------------------
    // CountDownLatch
    // -------------------------------------------------------------------------

    /**
     * 设置闭锁计数。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/latch/set-count?latchKey=latch:demo&count=3"
     *
     * @param latchKey 闭锁 Key
     * @param count    计数
     * @return 执行结果
     */
    @PostMapping("/latch/set-count")
    public Map<String, Object> setCount(@RequestParam String latchKey,
                                        @RequestParam Integer count) {
        checkKey(latchKey);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        redissonService.setCount(latchKey, count);
        log.info("设置闭锁计数，latchKey={}，count={}", latchKey, count);
        return ok(true);
    }

    /**
     * 闭锁计数减一。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/latch/count-down?latchKey=latch:demo"
     *
     * @param latchKey 闭锁 Key
     * @return 执行结果
     */
    @PostMapping("/latch/count-down")
    public Map<String, Object> countDown(@RequestParam String latchKey) {
        checkKey(latchKey);

        redissonService.countDown(latchKey);
        log.info("闭锁计数减一，latchKey={}", latchKey);
        return ok(true);
    }

    /**
     * 等待闭锁完成。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/latch/await?latchKey=latch:demo&timeoutSeconds=10"
     *
     * @param latchKey       闭锁 Key
     * @param timeoutSeconds 超时秒数
     * @return 是否完成
     * @throws InterruptedException 线程中断时抛出
     */
    @GetMapping("/latch/await")
    public Map<String, Object> await(@RequestParam String latchKey,
                                     @RequestParam(defaultValue = "10") Long timeoutSeconds) throws InterruptedException {
        checkKey(latchKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        boolean success = redissonService.await(latchKey, timeoutSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    // -------------------------------------------------------------------------
    // Semaphore
    // -------------------------------------------------------------------------

    /**
     * 初始化信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/semaphore/init?semaphoreKey=semaphore:demo&permits=5"
     *
     * @param semaphoreKey 信号量 Key
     * @param permits      许可数量
     * @return 执行结果
     */
    @PostMapping("/semaphore/init")
    public Map<String, Object> trySetPermits(@RequestParam String semaphoreKey,
                                             @RequestParam Integer permits) {
        checkKey(semaphoreKey);
        Assert.isTrue(ObjectUtil.isNotNull(permits) && permits > 0, "permits必须大于0");

        redissonService.trySetPermits(semaphoreKey, permits);
        return ok(true);
    }

    /**
     * 尝试获取信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/semaphore/try-acquire?semaphoreKey=semaphore:demo&permits=1&waitSeconds=3"
     *
     * @param semaphoreKey 信号量 Key
     * @param permits      许可数量
     * @param waitSeconds  等待秒数
     * @return 是否获取成功
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/semaphore/try-acquire")
    public Map<String, Object> tryAcquire(@RequestParam String semaphoreKey,
                                          @RequestParam(defaultValue = "1") Integer permits,
                                          @RequestParam(defaultValue = "3") Long waitSeconds) throws InterruptedException {
        checkKey(semaphoreKey);
        Assert.isTrue(ObjectUtil.isNotNull(permits) && permits > 0, "permits必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(waitSeconds) && waitSeconds >= 0, "waitSeconds不能小于0");

        boolean success = redissonService.tryAcquire(semaphoreKey, permits, waitSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 释放一个信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/semaphore/release?semaphoreKey=semaphore:demo"
     *
     * @param semaphoreKey 信号量 Key
     * @return 执行结果
     */
    @PostMapping("/semaphore/release")
    public Map<String, Object> release(@RequestParam String semaphoreKey) {
        checkKey(semaphoreKey);

        redissonService.release(semaphoreKey);
        return ok(true);
    }

    /**
     * 获取信号量可用许可数量。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/semaphore/available?semaphoreKey=semaphore:demo"
     *
     * @param semaphoreKey 信号量 Key
     * @return 可用许可数量
     */
    @GetMapping("/semaphore/available")
    public Map<String, Object> availablePermits(@RequestParam String semaphoreKey) {
        checkKey(semaphoreKey);

        int permits = redissonService.availablePermits(semaphoreKey);
        return ok(permits);
    }

    // -------------------------------------------------------------------------
    // PermitExpirableSemaphore
    // -------------------------------------------------------------------------

    /**
     * 初始化可过期信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/permit-expirable-semaphore/init?semaphoreKey=permit:demo&permits=5"
     *
     * @param semaphoreKey 信号量 Key
     * @param permits      许可数量
     * @return 执行结果
     */
    @PostMapping("/permit-expirable-semaphore/init")
    public Map<String, Object> permitTrySetPermits(@RequestParam String semaphoreKey,
                                                   @RequestParam Integer permits) {
        checkKey(semaphoreKey);
        Assert.isTrue(ObjectUtil.isNotNull(permits) && permits > 0, "permits必须大于0");

        boolean success = redissonService.getPermitExpirableSemaphore(semaphoreKey).trySetPermits(permits);
        return ok(success);
    }

    /**
     * 尝试获取可过期信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/permit-expirable-semaphore/try-acquire?semaphoreKey=permit:demo&waitSeconds=3&leaseSeconds=30"
     *
     * @param semaphoreKey 信号量 Key
     * @param waitSeconds  等待秒数
     * @param leaseSeconds 许可租约秒数
     * @return permitId，返回 null 表示未获取到
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/permit-expirable-semaphore/try-acquire")
    public Map<String, Object> permitTryAcquire(@RequestParam String semaphoreKey,
                                                @RequestParam(defaultValue = "3") Long waitSeconds,
                                                @RequestParam(defaultValue = "30") Long leaseSeconds) throws InterruptedException {
        checkKey(semaphoreKey);
        Assert.isTrue(ObjectUtil.isNotNull(waitSeconds) && waitSeconds >= 0, "waitSeconds不能小于0");
        Assert.isTrue(ObjectUtil.isNotNull(leaseSeconds) && leaseSeconds > 0, "leaseSeconds必须大于0");

        String permitId = redissonService.getPermitExpirableSemaphore(semaphoreKey)
                .tryAcquire(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        return ok(permitId);
    }

    /**
     * 释放可过期信号量许可。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/permit-expirable-semaphore/release?semaphoreKey=permit:demo&permitId=xxxx"
     *
     * @param semaphoreKey 信号量 Key
     * @param permitId     许可 ID
     * @return 执行结果
     */
    @PostMapping("/permit-expirable-semaphore/release")
    public Map<String, Object> permitRelease(@RequestParam String semaphoreKey,
                                             @RequestParam String permitId) {
        checkKey(semaphoreKey);
        Assert.isTrue(StrUtil.isNotBlank(permitId), "permitId不能为空");

        redissonService.getPermitExpirableSemaphore(semaphoreKey).release(permitId);
        return ok(true);
    }

    /**
     * 获取可过期信号量可用许可数量。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/permit-expirable-semaphore/available?semaphoreKey=permit:demo"
     *
     * @param semaphoreKey 信号量 Key
     * @return 可用许可数量
     */
    @GetMapping("/permit-expirable-semaphore/available")
    public Map<String, Object> permitAvailablePermits(@RequestParam String semaphoreKey) {
        checkKey(semaphoreKey);

        int permits = redissonService.getPermitExpirableSemaphore(semaphoreKey).availablePermits();
        return ok(permits);
    }

    // -------------------------------------------------------------------------
    // RateLimiter
    // -------------------------------------------------------------------------

    /**
     * 初始化限流器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/rate-limiter/init?key=rate:sms&rate=5&interval=1&unit=SECONDS&rateType=OVERALL"
     *
     * @param key      限流器 Key
     * @param rate     令牌数量
     * @param interval 时间间隔
     * @param unit     时间单位
     * @param rateType 限流类型
     * @return 是否初始化成功
     */
    @PostMapping("/rate-limiter/init")
    public Map<String, Object> rateLimiterInit(@RequestParam String key,
                                               @RequestParam Long rate,
                                               @RequestParam(defaultValue = "1") Long interval,
                                               @RequestParam(defaultValue = "SECONDS") RateIntervalUnit unit,
                                               @RequestParam(defaultValue = "OVERALL") RateType rateType) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(rate) && rate > 0, "rate必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(interval) && interval > 0, "interval必须大于0");

        boolean success = redissonService.rateLimiterInit(key, rateType, rate, interval, unit);
        return ok(success);
    }

    /**
     * 更新限流器速率。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/distributed/rate-limiter/rate?key=rate:sms&rate=10&interval=1&unit=SECONDS&rateType=OVERALL"
     *
     * @param key      限流器 Key
     * @param rate     令牌数量
     * @param interval 时间间隔
     * @param unit     时间单位
     * @param rateType 限流类型
     * @return 执行结果
     */
    @PutMapping("/rate-limiter/rate")
    public Map<String, Object> rateLimiterSetRate(@RequestParam String key,
                                                  @RequestParam Long rate,
                                                  @RequestParam(defaultValue = "1") Long interval,
                                                  @RequestParam(defaultValue = "SECONDS") RateIntervalUnit unit,
                                                  @RequestParam(defaultValue = "OVERALL") RateType rateType) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(rate) && rate > 0, "rate必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(interval) && interval > 0, "interval必须大于0");

        redissonService.rateLimiterSetRate(key, rateType, rate, interval, unit);
        return ok(true);
    }

    /**
     * 尝试获取限流令牌。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/rate-limiter/try-acquire?key=rate:sms&permits=1"
     *
     * @param key     限流器 Key
     * @param permits 令牌数量
     * @return 是否获取成功
     */
    @PostMapping("/rate-limiter/try-acquire")
    public Map<String, Object> rateLimiterTryAcquire(@RequestParam String key,
                                                     @RequestParam(defaultValue = "1") Long permits) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(permits) && permits > 0, "permits必须大于0");

        boolean success = redissonService.rateLimiterTryAcquire(key, permits);
        return ok(success);
    }

    /**
     * 等待获取限流令牌。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/rate-limiter/try-acquire-wait?key=rate:sms&timeoutSeconds=3"
     *
     * @param key            限流器 Key
     * @param timeoutSeconds 等待秒数
     * @return 是否获取成功
     */
    @PostMapping("/rate-limiter/try-acquire-wait")
    public Map<String, Object> rateLimiterTryAcquireWait(@RequestParam String key,
                                                         @RequestParam(defaultValue = "3") Long timeoutSeconds) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        boolean success = redissonService.rateLimiterTryAcquire(key, timeoutSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 删除限流器。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/distributed/rate-limiter?key=rate:sms"
     *
     * @param key 限流器 Key
     * @return 是否删除成功
     */
    @DeleteMapping("/rate-limiter")
    public Map<String, Object> rateLimiterDelete(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.rateLimiterDelete(key);
        return ok(success);
    }

    // -------------------------------------------------------------------------
    // BloomFilter
    // -------------------------------------------------------------------------

    /**
     * 初始化布隆过滤器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/bloom/init?key=bloom:user&expectedInsertions=100000&falseProbability=0.01"
     *
     * @param key                Redis 键
     * @param expectedInsertions 预计插入量
     * @param falseProbability   误判率
     * @return 执行结果
     */
    @PostMapping("/bloom/init")
    public Map<String, Object> bloomInit(@RequestParam String key,
                                         @RequestParam(defaultValue = "100000") Long expectedInsertions,
                                         @RequestParam(defaultValue = "0.01") Double falseProbability) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(expectedInsertions) && expectedInsertions > 0, "expectedInsertions必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(falseProbability) && falseProbability > 0 && falseProbability < 1,
                "falseProbability必须在0到1之间");

        redissonService.bloomInit(key, expectedInsertions, falseProbability);
        return ok(true);
    }

    /**
     * 添加布隆过滤器元素。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/bloom/add?key=bloom:user" \
     * -H "Content-Type: application/json" \
     * -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    @PostMapping("/bloom/add")
    public Map<String, Object> bloomAdd(@RequestParam String key,
                                        @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        boolean success = redissonService.bloomAdd(key, value);
        return ok(success);
    }

    /**
     * 批量添加布隆过滤器元素。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/bloom/add-all?key=bloom:user" \
     * -H "Content-Type: application/json" \
     * -d '["user:1","user:2","user:3"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 新增数量
     */
    @PostMapping("/bloom/add-all")
    public Map<String, Object> bloomAddAll(@RequestParam String key,
                                           @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        long count = redissonService.bloomAddAll(key, values);
        return ok(count);
    }

    /**
     * 判断布隆过滤器是否可能包含元素。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/distributed/bloom/contains?key=bloom:user" \
     * -H "Content-Type: application/json" \
     * -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否可能包含
     */
    @PostMapping("/bloom/contains")
    public Map<String, Object> bloomContains(@RequestParam String key,
                                             @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        boolean exists = redissonService.bloomContains(key, value);
        return ok(exists);
    }

    /**
     * 判断布隆过滤器是否存在。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/bloom/exists?key=bloom:user"
     *
     * @param key Redis 键
     * @return 是否存在
     */
    @GetMapping("/bloom/exists")
    public Map<String, Object> bloomExists(@RequestParam String key) {
        checkKey(key);

        boolean exists = redissonService.bloomExists(key);
        return ok(exists);
    }

    /**
     * 获取布隆过滤器信息。
     * <p>
     * curl "http://localhost:8080/redisson/distributed/bloom/info?key=bloom:user"
     *
     * @param key Redis 键
     * @return 布隆过滤器信息
     */
    @GetMapping("/bloom/info")
    public Map<String, Object> bloomInfo(@RequestParam String key) {
        checkKey(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exists", redissonService.bloomExists(key));
        data.put("expectedInsertions", redissonService.bloomGetExpectedInsertions(key));
        data.put("falseProbability", redissonService.bloomGetFalseProbability(key));

        RBloomFilter<Object> bloomFilter = redissonService.getBloomFilter(key);
        if (bloomFilter.isExists()) {
            data.put("count", bloomFilter.count());
            data.put("size", bloomFilter.getSize());
            data.put("hashIterations", bloomFilter.getHashIterations());
        }

        return ok(data);
    }

    /**
     * 删除布隆过滤器。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/distributed/bloom?key=bloom:user"
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @DeleteMapping("/bloom")
    public Map<String, Object> bloomDelete(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.bloomDelete(key);
        return ok(success);
    }

    /**
     * 执行带锁任务。
     *
     * @param lockKey        锁 Key
     * @param lock           锁对象
     * @param waitSeconds    最大等待秒数
     * @param leaseSeconds   锁持有秒数
     * @param businessMillis 模拟业务耗时
     * @return 是否执行成功
     */
    private boolean executeWithRLock(String lockKey,
                                     RLock lock,
                                     long waitSeconds,
                                     long leaseSeconds,
                                     long businessMillis) {
        boolean locked = false;

        try {
            locked = leaseSeconds <= 0
                    ? lock.tryLock(waitSeconds, TimeUnit.SECONDS)
                    : lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);

            if (!locked) {
                log.warn("获取分布式锁失败，lockKey={}", lockKey);
                return false;
            }

            log.info("获取分布式锁成功，lockKey={}，businessMillis={}", lockKey, businessMillis);
            ThreadUtil.sleep(businessMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，lockKey={}", lockKey, e);
            return false;
        } finally {
            unlockSafely(lockKey, lock, locked);
        }
    }

    /**
     * 安全释放锁。
     *
     * @param lockKey 锁 Key
     * @param lock    锁对象
     * @param locked  是否已获取锁
     */
    private void unlockSafely(String lockKey, RLock lock, boolean locked) {
        if (!locked || ObjectUtil.isNull(lock)) {
            return;
        }

        if (!lock.isHeldByCurrentThread()) {
            log.warn("当前线程未持有分布式锁，跳过释放，lockKey={}", lockKey);
            return;
        }

        try {
            lock.unlock();
            log.info("释放分布式锁成功，lockKey={}", lockKey);
        } catch (Exception e) {
            log.error("释放分布式锁异常，lockKey={}", lockKey, e);
        }
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 校验锁时间参数。
     *
     * @param waitSeconds  等待秒数
     * @param leaseSeconds 持有秒数
     */
    private void checkLockTime(Long waitSeconds, Long leaseSeconds) {
        Assert.isTrue(ObjectUtil.isNotNull(waitSeconds) && waitSeconds >= 0, "waitSeconds不能小于0");
        Assert.notNull(leaseSeconds, "leaseSeconds不能为空");
    }

    /**
     * 校验模拟业务耗时。
     *
     * @param businessMillis 模拟业务耗时
     */
    private void checkBusinessMillis(Long businessMillis) {
        Assert.isTrue(ObjectUtil.isNotNull(businessMillis) && businessMillis >= 0, "businessMillis不能小于0");
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

}
```

### 消息能力测试控制器

用于演示 RedissonService 的队列、延迟队列、发布订阅、模式订阅、可靠消息对象入口、Stream 消息流等能力。

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.listener.PatternMessageListener;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Redisson 消息能力测试控制器
 * 用于演示 RedissonService 的队列、延迟队列、发布订阅、模式订阅、可靠消息对象入口、Stream 消息流等能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/message")
@RequiredArgsConstructor
public class RedissonMessageController {

    private final RedissonService redissonService;

    private final Map<String, List<Integer>> topicListenerMap = new ConcurrentHashMap<>();

    private final Map<String, List<Integer>> patternTopicListenerMap = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Queue / BlockingQueue / DelayedQueue
    // -------------------------------------------------------------------------

    /**
     * 普通队列入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/queue/enqueue?queueKey=queue:test" \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"Ateng"}'
     *
     * @param queueKey 队列 Key
     * @param value    元素
     * @return 是否入队成功
     */
    @PostMapping("/queue/enqueue")
    public Map<String, Object> enqueue(@RequestParam String queueKey,
                                       @RequestBody Object value) {
        checkKey(queueKey);

        boolean success = redissonService.enqueue(queueKey, value);
        log.info("普通队列入队成功，queueKey={}，success={}", queueKey, success);
        return ok(success);
    }

    /**
     * 普通队列出队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/queue/dequeue?queueKey=queue:test"
     *
     * @param queueKey 队列 Key
     * @return 出队元素
     */
    @PostMapping("/queue/dequeue")
    public Map<String, Object> dequeue(@RequestParam String queueKey) {
        checkKey(queueKey);

        Object value = redissonService.dequeue(queueKey);
        return ok(value);
    }

    /**
     * 阻塞队列入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-queue/enqueue?queueKey=queue:blocking:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"message-1"'
     *
     * @param queueKey 队列 Key
     * @param value    元素
     * @return 执行结果
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/blocking-queue/enqueue")
    public Map<String, Object> enqueueBlocking(@RequestParam String queueKey,
                                               @RequestBody Object value) throws InterruptedException {
        checkKey(queueKey);

        redissonService.enqueueBlocking(queueKey, value);
        log.info("阻塞队列入队成功，queueKey={}", queueKey);
        return ok(true);
    }

    /**
     * 阻塞队列超时入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-queue/enqueue-timeout?queueKey=queue:blocking:test&timeoutSeconds=3" \
     *   -H "Content-Type: application/json" \
     *   -d '"message-1"'
     *
     * @param queueKey       队列 Key
     * @param timeoutSeconds 超时秒数
     * @param value          元素
     * @return 是否入队成功
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/blocking-queue/enqueue-timeout")
    public Map<String, Object> enqueueBlockingTimeout(@RequestParam String queueKey,
                                                      @RequestParam(defaultValue = "3") Long timeoutSeconds,
                                                      @RequestBody Object value) throws InterruptedException {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        boolean success = redissonService.enqueueBlocking(queueKey, value, timeoutSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 阻塞队列超时出队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-queue/dequeue?queueKey=queue:blocking:test&timeoutSeconds=10"
     *
     * @param queueKey       队列 Key
     * @param timeoutSeconds 超时秒数
     * @return 出队元素
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/blocking-queue/dequeue")
    public Map<String, Object> dequeueBlocking(@RequestParam String queueKey,
                                               @RequestParam(defaultValue = "10") Long timeoutSeconds) throws InterruptedException {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        Object value = redissonService.dequeueBlocking(queueKey, timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 获取队列长度。
     *
     * curl "http://localhost:8080/redisson/message/queue/size?queueKey=queue:test"
     *
     * @param queueKey 队列 Key
     * @return 队列长度
     */
    @GetMapping("/queue/size")
    public Map<String, Object> queueSize(@RequestParam String queueKey) {
        checkKey(queueKey);

        long size = redissonService.queueSize(queueKey);
        return ok(size);
    }

    /**
     * 判断队列是否为空。
     *
     * curl "http://localhost:8080/redisson/message/queue/empty?queueKey=queue:test"
     *
     * @param queueKey 队列 Key
     * @return 是否为空
     */
    @GetMapping("/queue/empty")
    public Map<String, Object> isQueueEmpty(@RequestParam String queueKey) {
        checkKey(queueKey);

        boolean empty = redissonService.isQueueEmpty(queueKey);
        return ok(empty);
    }

    /**
     * 清空队列。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/queue/clear?queueKey=queue:test"
     *
     * @param queueKey 队列 Key
     * @return 执行结果
     */
    @DeleteMapping("/queue/clear")
    public Map<String, Object> clearQueue(@RequestParam String queueKey) {
        checkKey(queueKey);

        redissonService.clearQueue(queueKey);
        log.info("队列清空成功，queueKey={}", queueKey);
        return ok(true);
    }

    /**
     * 删除队列元素。
     *
     * curl -X POST "http://localhost:8080/redisson/message/queue/remove?queueKey=queue:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"message-1"'
     *
     * @param queueKey 队列 Key
     * @param value    元素
     * @return 是否删除成功
     */
    @PostMapping("/queue/remove")
    public Map<String, Object> removeFromQueue(@RequestParam String queueKey,
                                               @RequestBody Object value) {
        checkKey(queueKey);

        boolean success = redissonService.removeFromQueue(queueKey, value);
        return ok(success);
    }

    /**
     * 添加延迟队列任务。
     *
     * curl -X POST "http://localhost:8080/redisson/message/delayed-queue/enqueue?queueKey=queue:delay:test&delaySeconds=10" \
     *   -H "Content-Type: application/json" \
     *   -d '{"taskId":1,"type":"timeout-close-order"}'
     *
     * @param queueKey     队列 Key
     * @param delaySeconds 延迟秒数
     * @param value        元素
     * @return 执行结果
     */
    @PostMapping("/delayed-queue/enqueue")
    public Map<String, Object> enqueueDelayed(@RequestParam String queueKey,
                                              @RequestParam(defaultValue = "10") Long delaySeconds,
                                              @RequestBody Object value) {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(delaySeconds) && delaySeconds >= 0, "delaySeconds不能小于0");

        redissonService.enqueueDelayed(queueKey, value, delaySeconds, TimeUnit.SECONDS);
        log.info("延迟队列任务写入成功，queueKey={}，delaySeconds={}", queueKey, delaySeconds);
        return ok(true);
    }

    /**
     * 获取延迟队列对象信息。
     *
     * curl "http://localhost:8080/redisson/message/delayed-queue/info?queueKey=queue:delay:test"
     *
     * @param queueKey 队列 Key
     * @return 延迟队列信息
     */
    @GetMapping("/delayed-queue/info")
    public Map<String, Object> delayedQueueInfo(@RequestParam String queueKey) {
        checkKey(queueKey);

        RDelayedQueue<Object> delayedQueue = redissonService.getDelayedQueue(queueKey);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", delayedQueue.getClass().getName());
        data.put("queueKey", queueKey);
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // Deque / RingBuffer / PriorityQueue / ReliableQueue
    // -------------------------------------------------------------------------

    /**
     * 阻塞双端队列左侧入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-deque/left-push?key=deque:blocking:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 执行结果
     */
    @PostMapping("/blocking-deque/left-push")
    public Map<String, Object> blockingDequeLeftPush(@RequestParam String key,
                                                     @RequestBody Object value) {
        checkKey(key);

        RBlockingDeque<Object> deque = redissonService.getBlockingDeque(key);
        deque.offerFirst(value);
        return ok(true);
    }

    /**
     * 阻塞双端队列右侧入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-deque/right-push?key=deque:blocking:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"B"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 执行结果
     */
    @PostMapping("/blocking-deque/right-push")
    public Map<String, Object> blockingDequeRightPush(@RequestParam String key,
                                                      @RequestBody Object value) {
        checkKey(key);

        RBlockingDeque<Object> deque = redissonService.getBlockingDeque(key);
        deque.offerLast(value);
        return ok(true);
    }

    /**
     * 阻塞双端队列左侧出队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-deque/left-pop?key=deque:blocking:test&timeoutSeconds=5"
     *
     * @param key            Redis 键
     * @param timeoutSeconds 等待秒数
     * @return 元素
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/blocking-deque/left-pop")
    public Map<String, Object> blockingDequeLeftPop(@RequestParam String key,
                                                    @RequestParam(defaultValue = "5") Long timeoutSeconds) throws InterruptedException {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        RBlockingDeque<Object> deque = redissonService.getBlockingDeque(key);
        Object value = deque.pollFirst(timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 初始化环形缓冲队列容量。
     *
     * curl -X POST "http://localhost:8080/redisson/message/ring-buffer/init?key=ring:test&capacity=10"
     *
     * @param key      Redis 键
     * @param capacity 容量
     * @return 是否初始化成功
     */
    @PostMapping("/ring-buffer/init")
    public Map<String, Object> ringBufferInit(@RequestParam String key,
                                              @RequestParam Integer capacity) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(capacity) && capacity > 0, "capacity必须大于0");

        RRingBuffer<Object> ringBuffer = redissonService.getRingBuffer(key);
        boolean success = ringBuffer.trySetCapacity(capacity);
        return ok(success);
    }

    /**
     * 环形缓冲队列添加元素。
     *
     * curl -X POST "http://localhost:8080/redisson/message/ring-buffer/add?key=ring:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否添加成功
     */
    @PostMapping("/ring-buffer/add")
    public Map<String, Object> ringBufferAdd(@RequestParam String key,
                                             @RequestBody Object value) {
        checkKey(key);

        RRingBuffer<Object> ringBuffer = redissonService.getRingBuffer(key);
        boolean success = ringBuffer.add(value);
        return ok(success);
    }

    /**
     * 获取环形缓冲队列信息。
     *
     * curl "http://localhost:8080/redisson/message/ring-buffer/info?key=ring:test"
     *
     * @param key Redis 键
     * @return 信息
     */
    @GetMapping("/ring-buffer/info")
    public Map<String, Object> ringBufferInfo(@RequestParam String key) {
        checkKey(key);

        RRingBuffer<Object> ringBuffer = redissonService.getRingBuffer(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("size", ringBuffer.size());
        data.put("capacity", ringBuffer.capacity());
        data.put("remainingCapacity", ringBuffer.remainingCapacity());
        data.put("values", ringBuffer.readAll());
        return ok(data);
    }

    /**
     * 优先级队列添加元素。
     * 注意：元素需要实现 Comparable，否则会在运行时排序失败。
     *
     * curl -X POST "http://localhost:8080/redisson/message/priority-queue/add?key=priority:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否添加成功
     */
    @PostMapping("/priority-queue/add")
    public Map<String, Object> priorityQueueAdd(@RequestParam String key,
                                                @RequestBody Object value) {
        checkKey(key);

        RPriorityQueue<Object> priorityQueue = redissonService.getPriorityQueue(key);
        boolean success = priorityQueue.offer(value);
        return ok(success);
    }

    /**
     * 优先级队列弹出元素。
     *
     * curl -X POST "http://localhost:8080/redisson/message/priority-queue/poll?key=priority:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/priority-queue/poll")
    public Map<String, Object> priorityQueuePoll(@RequestParam String key) {
        checkKey(key);

        RPriorityQueue<Object> priorityQueue = redissonService.getPriorityQueue(key);
        Object value = priorityQueue.poll();
        return ok(value);
    }

    /**
     * 获取可靠队列对象信息。
     *
     * curl "http://localhost:8080/redisson/message/reliable-queue/info?key=reliable:test"
     *
     * @param key Redis 键
     * @return 可靠队列信息
     */
    @GetMapping("/reliable-queue/info")
    public Map<String, Object> reliableQueueInfo(@RequestParam String key) {
        checkKey(key);

        RReliableQueue<Object> reliableQueue = redissonService.getReliableQueue(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", reliableQueue.getClass().getName());
        data.put("key", key);
        data.put("exists", reliableQueue.isExists());
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // Topic / PatternTopic / ReliableTopic
    // -------------------------------------------------------------------------

    /**
     * 发布消息。
     *
     * curl -X POST "http://localhost:8080/redisson/message/topic/publish?channel=topic:test" \
     *   -H "Content-Type: application/json" \
     *   -d '{"type":"notice","content":"hello"}'
     *
     * @param channel 频道
     * @param message 消息
     * @return 接收客户端数量
     */
    @PostMapping("/topic/publish")
    public Map<String, Object> publish(@RequestParam String channel,
                                       @RequestBody Object message) {
        checkKey(channel);

        long receivers = redissonService.publish(channel, message);
        log.info("Redis Topic 消息发布成功，channel={}，receivers={}", channel, receivers);
        return ok(receivers);
    }

    /**
     * 订阅频道。
     *
     * curl -X POST "http://localhost:8080/redisson/message/topic/subscribe?channel=topic:test"
     *
     * @param channel 频道
     * @return 监听器 ID
     */
    @PostMapping("/topic/subscribe")
    public Map<String, Object> subscribe(@RequestParam String channel) {
        checkKey(channel);

        Consumer<Object> consumer = message -> log.info("收到 Redis Topic 消息，channel={}，message={}", channel, message);
        int listenerId = redissonService.subscribe(channel, consumer);

        topicListenerMap.computeIfAbsent(channel, item -> new CopyOnWriteArrayList<>()).add(listenerId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("channel", channel);
        data.put("listenerId", listenerId);
        return ok(data);
    }

    /**
     * 取消指定频道监听器。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/topic/unsubscribe?channel=topic:test&listenerId=1"
     *
     * @param channel    频道
     * @param listenerId 监听器 ID
     * @return 执行结果
     */
    @DeleteMapping("/topic/unsubscribe")
    public Map<String, Object> unsubscribe(@RequestParam String channel,
                                           @RequestParam Integer listenerId) {
        checkKey(channel);
        Assert.notNull(listenerId, "listenerId不能为空");

        redissonService.unsubscribe(channel, listenerId);
        removeListenerId(topicListenerMap, channel, listenerId);
        return ok(true);
    }

    /**
     * 取消频道全部监听器。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/topic/unsubscribe-all?channel=topic:test"
     *
     * @param channel 频道
     * @return 执行结果
     */
    @DeleteMapping("/topic/unsubscribe-all")
    public Map<String, Object> unsubscribeAll(@RequestParam String channel) {
        checkKey(channel);

        redissonService.unsubscribe(channel);
        topicListenerMap.remove(channel);
        return ok(true);
    }

    /**
     * 查看当前 Controller 记录的 Topic 监听器。
     *
     * curl "http://localhost:8080/redisson/message/topic/listeners"
     *
     * @return 监听器记录
     */
    @GetMapping("/topic/listeners")
    public Map<String, Object> topicListeners() {
        return ok(topicListenerMap);
    }

    /**
     * 订阅模式频道。
     *
     * curl -X POST "http://localhost:8080/redisson/message/pattern-topic/subscribe?pattern=topic:*"
     *
     * @param pattern 频道通配符
     * @return 监听器 ID
     */
    @PostMapping("/pattern-topic/subscribe")
    public Map<String, Object> patternSubscribe(@RequestParam String pattern) {
        checkKey(pattern);

        RPatternTopic patternTopic = redissonService.getPatternTopic(pattern);
        PatternMessageListener<Object> listener = (patternText, channel, message) ->
                log.info("收到 Redis PatternTopic 消息，pattern={}，channel={}，message={}", patternText, channel, message);

        int listenerId = patternTopic.addListener(Object.class, listener);
        patternTopicListenerMap.computeIfAbsent(pattern, item -> new CopyOnWriteArrayList<>()).add(listenerId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pattern", pattern);
        data.put("listenerId", listenerId);
        return ok(data);
    }

    /**
     * 取消模式频道监听器。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/pattern-topic/unsubscribe?pattern=topic:*&listenerId=1"
     *
     * @param pattern    频道通配符
     * @param listenerId 监听器 ID
     * @return 执行结果
     */
    @DeleteMapping("/pattern-topic/unsubscribe")
    public Map<String, Object> patternUnsubscribe(@RequestParam String pattern,
                                                  @RequestParam Integer listenerId) {
        checkKey(pattern);
        Assert.notNull(listenerId, "listenerId不能为空");

        redissonService.getPatternTopic(pattern).removeListener(listenerId);
        removeListenerId(patternTopicListenerMap, pattern, listenerId);
        return ok(true);
    }

    /**
     * 获取可靠主题对象信息。
     *
     * curl "http://localhost:8080/redisson/message/reliable-topic/info?topic=reliable-topic:test"
     *
     * @param topic 主题
     * @return 可靠主题信息
     */
    @GetMapping("/reliable-topic/info")
    public Map<String, Object> reliableTopicInfo(@RequestParam String topic) {
        checkKey(topic);

        RReliableTopic reliableTopic = redissonService.getReliableTopic(topic);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", reliableTopic.getClass().getName());
        data.put("topic", topic);
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // Stream
    // -------------------------------------------------------------------------

    /**
     * 添加 Stream 消息。
     *
     * curl -X POST "http://localhost:8080/redisson/message/stream/add?streamKey=stream:test" \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":"1001","event":"login","time":"2026-04-26 10:00:00"}'
     *
     * @param streamKey Stream Key
     * @param entries   消息字段
     * @return 消息 ID
     */
    @PostMapping("/stream/add")
    public Map<String, Object> streamAdd(@RequestParam String streamKey,
                                         @RequestBody Map<Object, Object> entries) {
        checkKey(streamKey);
        Assert.isTrue(CollUtil.isNotEmpty(entries), "entries不能为空");

        StreamMessageId id = redissonService.streamAdd(streamKey, entries);
        log.info("Redis Stream 消息写入成功，streamKey={}，id={}", streamKey, id);
        return ok(idToMap(id));
    }

    /**
     * 读取 Stream 消息。
     *
     * curl "http://localhost:8080/redisson/message/stream/read?streamKey=stream:test&id=0-0&count=10"
     *
     * @param streamKey Stream Key
     * @param id        起始 ID，格式如 0-0
     * @param count     数量
     * @return 消息 Map
     */
    @GetMapping("/stream/read")
    public Map<String, Object> streamRead(@RequestParam String streamKey,
                                          @RequestParam(defaultValue = "0-0") String id,
                                          @RequestParam(defaultValue = "10") Integer count) {
        checkKey(streamKey);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        StreamMessageId startId = parseStreamMessageId(id);
        StreamReadArgs args = StreamReadArgs.greaterThan(startId).count(count);
        Map<StreamMessageId, Map<Object, Object>> data = redissonService.streamRead(streamKey, args);

        return ok(streamMapToResponse(data));
    }

    /**
     * 创建 Stream 消费组。
     *
     * curl -X POST "http://localhost:8080/redisson/message/stream/group/create?streamKey=stream:test&groupName=group:test&id=0-0"
     *
     * @param streamKey Stream Key
     * @param groupName 消费组
     * @param id        起始 ID，格式如 0-0
     * @return 执行结果
     */
    @PostMapping("/stream/group/create")
    public Map<String, Object> streamCreateGroup(@RequestParam String streamKey,
                                                 @RequestParam String groupName,
                                                 @RequestParam(defaultValue = "0-0") String id) {
        checkKey(streamKey);
        checkKey(groupName);

        StreamMessageId startId = parseStreamMessageId(id);
        redissonService.streamCreateGroup(streamKey, groupName, startId);

        return ok(true);
    }

    /**
     * 读取消费组未投递的新消息。
     *
     * curl "http://localhost:8080/redisson/message/stream/group/read-new?streamKey=stream:test&groupName=group:test&consumerName=consumer:1&count=10"
     *
     * @param streamKey    Stream Key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @param count        数量
     * @return 消息 Map
     */
    @GetMapping("/stream/group/read-new")
    public Map<String, Object> streamReadGroupNew(@RequestParam String streamKey,
                                                  @RequestParam String groupName,
                                                  @RequestParam String consumerName,
                                                  @RequestParam(defaultValue = "10") Integer count) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        StreamReadGroupArgs args = StreamReadGroupArgs.neverDelivered().count(count);
        Map<StreamMessageId, Map<Object, Object>> data = redissonService.streamReadGroup(
                streamKey,
                groupName,
                consumerName,
                args
        );

        return ok(streamMapToResponse(data));
    }

    /**
     * 确认 Stream 消息。
     *
     * curl -X POST "http://localhost:8080/redisson/message/stream/ack?streamKey=stream:test&groupName=group:test&ids=1714096800000-0"
     *
     * @param streamKey Stream Key
     * @param groupName 消费组
     * @param ids       消息 ID 集合
     * @return 确认数量
     */
    @PostMapping("/stream/ack")
    public Map<String, Object> streamAck(@RequestParam String streamKey,
                                         @RequestParam String groupName,
                                         @RequestParam List<String> ids) {
        checkKey(streamKey);
        checkKey(groupName);
        Assert.isTrue(CollUtil.isNotEmpty(ids), "ids不能为空");

        StreamMessageId[] messageIds = ids.stream()
                .map(this::parseStreamMessageId)
                .toArray(StreamMessageId[]::new);

        long count = redissonService.streamAck(streamKey, groupName, messageIds);
        return ok(count);
    }

    /**
     * 删除 Stream 消息。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/stream/remove?streamKey=stream:test&ids=1714096800000-0"
     *
     * @param streamKey Stream Key
     * @param ids       消息 ID 集合
     * @return 删除数量
     */
    @DeleteMapping("/stream/remove")
    public Map<String, Object> streamRemove(@RequestParam String streamKey,
                                            @RequestParam List<String> ids) {
        checkKey(streamKey);
        Assert.isTrue(CollUtil.isNotEmpty(ids), "ids不能为空");

        StreamMessageId[] messageIds = ids.stream()
                .map(this::parseStreamMessageId)
                .toArray(StreamMessageId[]::new);

        long count = redissonService.streamRemove(streamKey, messageIds);
        return ok(count);
    }

    /**
     * 获取 Stream 长度。
     *
     * curl "http://localhost:8080/redisson/message/stream/size?streamKey=stream:test"
     *
     * @param streamKey Stream Key
     * @return 长度
     */
    @GetMapping("/stream/size")
    public Map<String, Object> streamSize(@RequestParam String streamKey) {
        checkKey(streamKey);

        long size = redissonService.streamSize(streamKey);
        return ok(size);
    }

    /**
     * 模拟延迟队列消费者。
     * 调用后会阻塞等待一条消息，适合本地测试，不建议生产接口直接暴露。
     *
     * curl "http://localhost:8080/redisson/message/debug/delayed-queue/take?queueKey=queue:delay:test&timeoutSeconds=30"
     *
     * @param queueKey       队列 Key
     * @param timeoutSeconds 等待秒数
     * @return 消费到的消息
     * @throws InterruptedException 线程中断时抛出
     */
    @GetMapping("/debug/delayed-queue/take")
    public Map<String, Object> debugDelayedQueueTake(@RequestParam String queueKey,
                                                     @RequestParam(defaultValue = "30") Long timeoutSeconds) throws InterruptedException {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        Object value = redissonService.getBlockingQueue(queueKey).poll(timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 模拟阻塞消费者。
     * 调用后会阻塞等待一条消息，适合本地测试，不建议生产接口直接暴露。
     *
     * curl "http://localhost:8080/redisson/message/debug/blocking-queue/take?queueKey=queue:blocking:test&timeoutSeconds=30"
     *
     * @param queueKey       队列 Key
     * @param timeoutSeconds 等待秒数
     * @return 消费到的消息
     * @throws InterruptedException 线程中断时抛出
     */
    @GetMapping("/debug/blocking-queue/take")
    public Map<String, Object> debugBlockingQueueTake(@RequestParam String queueKey,
                                                      @RequestParam(defaultValue = "30") Long timeoutSeconds) throws InterruptedException {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        Object value = redissonService.dequeueBlocking(queueKey, timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 移除监听器 ID。
     *
     * @param listenerMap 监听器 Map
     * @param key         监听 Key
     * @param listenerId  监听器 ID
     */
    private void removeListenerId(Map<String, List<Integer>> listenerMap, String key, Integer listenerId) {
        List<Integer> listenerIds = listenerMap.get(key);
        if (CollUtil.isEmpty(listenerIds)) {
            return;
        }

        listenerIds.remove(listenerId);
        if (CollUtil.isEmpty(listenerIds)) {
            listenerMap.remove(key);
        }
    }

    /**
     * 解析 Stream 消息 ID。
     *
     * @param id ID 文本，格式为 0-0
     * @return StreamMessageId
     */
    private StreamMessageId parseStreamMessageId(String id) {
        Assert.isTrue(StrUtil.isNotBlank(id), "Stream消息ID不能为空");

        List<String> parts = StrUtil.split(id, "-");
        Assert.isTrue(parts.size() == 2, "Stream消息ID格式错误，正确格式如 0-0");

        long first = Long.parseLong(parts.get(0));
        long second = Long.parseLong(parts.get(1));
        return new StreamMessageId(first, second);
    }

    /**
     * StreamMessageId 转换为 Map。
     *
     * @param id StreamMessageId
     * @return Map
     */
    private Map<String, Object> idToMap(StreamMessageId id) {
        if (ObjectUtil.isNull(id)) {
            return null;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id.toString());
        return data;
    }

    /**
     * Stream 消息转换为响应结构。
     *
     * @param streamMap Stream 消息 Map
     * @return 响应 Map
     */
    private Map<String, Object> streamMapToResponse(Map<StreamMessageId, Map<Object, Object>> streamMap) {
        if (CollUtil.isEmpty(streamMap)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        streamMap.forEach((id, body) -> result.put(id.toString(), body));
        return result;
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

    /**
     * Stream 添加请求体。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class StreamAddRequest {

        /**
         * Stream Key。
         */
        private String streamKey;

        /**
         * 消息字段。
         */
        private Map<Object, Object> entries;

    }

}
```

### 场景能力测试控制器

用于演示 RedissonService 的 BitSet 签到、HyperLogLog UV、Geo 地理位置、分布式 Session 等能力。

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.GeoPosition;
import org.redisson.api.GeoUnit;
import org.redisson.api.geo.GeoSearchArgs;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redisson 场景能力测试控制器
 * 用于演示 RedissonService 的 BitSet 签到、HyperLogLog UV、Geo 地理位置、分布式 Session 等能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/scene")
@RequiredArgsConstructor
public class RedissonSceneController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // BitSet / 签到
    // -------------------------------------------------------------------------

    /**
     * 设置位图指定位置。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/bit/set?key=bit:test&index=1&value=true"
     *
     * @param key   Redis 键
     * @param index 位索引
     * @param value 位值
     * @return 执行结果
     */
    @PostMapping("/bit/set")
    public Map<String, Object> bitSet(@RequestParam String key,
                                      @RequestParam Long index,
                                      @RequestParam Boolean value) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(index) && index >= 0, "index不能小于0");
        Assert.notNull(value, "value不能为空");

        redissonService.bitSet(key, index, value);
        log.info("BitSet 设置成功，key={}，index={}，value={}", key, index, value);
        return ok(true);
    }

    /**
     * 获取位图指定位置。
     *
     * curl "http://localhost:8080/redisson/scene/bit/get?key=bit:test&index=1"
     *
     * @param key   Redis 键
     * @param index 位索引
     * @return 位值
     */
    @GetMapping("/bit/get")
    public Map<String, Object> bitGet(@RequestParam String key,
                                      @RequestParam Long index) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(index) && index >= 0, "index不能小于0");

        boolean value = redissonService.bitGet(key, index);
        return ok(value);
    }

    /**
     * 获取位图 true 数量。
     *
     * curl "http://localhost:8080/redisson/scene/bit/count?key=bit:test"
     *
     * @param key Redis 键
     * @return 数量
     */
    @GetMapping("/bit/count")
    public Map<String, Object> bitCount(@RequestParam String key) {
        checkKey(key);

        long count = redissonService.bitCount(key);
        return ok(count);
    }

    /**
     * 清空位图。
     *
     * curl -X DELETE "http://localhost:8080/redisson/scene/bit/clear?key=bit:test"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/bit/clear")
    public Map<String, Object> bitClear(@RequestParam String key) {
        checkKey(key);

        redissonService.bitClear(key);
        log.info("BitSet 清空成功，key={}", key);
        return ok(true);
    }

    /**
     * 用户签到。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/sign/do?keyPrefix=sign&userId=1001&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期，不传默认今天
     * @return 执行结果
     */
    @PostMapping("/sign/do")
    public Map<String, Object> sign(@RequestParam(defaultValue = "sign") String keyPrefix,
                                    @RequestParam String userId,
                                    @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(userId);

        LocalDate targetDate = parseDate(date);
        redissonService.sign(keyPrefix, userId, targetDate);

        log.info("用户签到成功，keyPrefix={}，userId={}，date={}", keyPrefix, userId, targetDate);
        return ok(true);
    }

    /**
     * 判断用户是否签到。
     *
     * curl "http://localhost:8080/redisson/scene/sign/check?keyPrefix=sign&userId=1001&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期，不传默认今天
     * @return 是否签到
     */
    @GetMapping("/sign/check")
    public Map<String, Object> isSigned(@RequestParam(defaultValue = "sign") String keyPrefix,
                                        @RequestParam String userId,
                                        @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(userId);

        LocalDate targetDate = parseDate(date);
        boolean signed = redissonService.isSigned(keyPrefix, userId, targetDate);

        return ok(signed);
    }

    /**
     * 获取用户指定年份签到天数。
     *
     * curl "http://localhost:8080/redisson/scene/sign/count?keyPrefix=sign&userId=1001&year=2026"
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param year      年份，不传默认当前年
     * @return 签到天数
     */
    @GetMapping("/sign/count")
    public Map<String, Object> getSignCount(@RequestParam(defaultValue = "sign") String keyPrefix,
                                            @RequestParam String userId,
                                            @RequestParam(required = false) Integer year) {
        checkKey(keyPrefix);
        checkKey(userId);

        int targetYear = ObjectUtil.defaultIfNull(year, LocalDate.now().getYear());
        long count = redissonService.getSignCount(keyPrefix, userId, targetYear);

        return ok(count);
    }

    /**
     * 获取用户连续签到天数。
     *
     * curl "http://localhost:8080/redisson/scene/sign/continuous?keyPrefix=sign&userId=1001&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期，不传默认今天
     * @return 连续签到天数
     */
    @GetMapping("/sign/continuous")
    public Map<String, Object> getContinuousSignCount(@RequestParam(defaultValue = "sign") String keyPrefix,
                                                      @RequestParam String userId,
                                                      @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(userId);

        LocalDate targetDate = parseDate(date);
        int count = redissonService.getContinuousSignCount(keyPrefix, userId, targetDate);

        return ok(count);
    }

    // -------------------------------------------------------------------------
    // HyperLogLog / UV
    // -------------------------------------------------------------------------

    /**
     * 添加 HyperLogLog 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/hll/add?key=hll:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否改变基数估算
     */
    @PostMapping("/hll/add")
    public Map<String, Object> hllAdd(@RequestParam String key,
                                      @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        boolean success = redissonService.hllAdd(key, value);
        return ok(success);
    }

    /**
     * 批量添加 HyperLogLog 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/hll/add-all?key=hll:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["user:1","user:2","user:3"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否改变基数估算
     */
    @PostMapping("/hll/add-all")
    public Map<String, Object> hllAddAll(@RequestParam String key,
                                         @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.hllAddAll(key, values);
        return ok(success);
    }

    /**
     * 获取 HyperLogLog 基数估算。
     *
     * curl "http://localhost:8080/redisson/scene/hll/count?key=hll:test"
     *
     * @param key Redis 键
     * @return 基数估算
     */
    @GetMapping("/hll/count")
    public Map<String, Object> hllCount(@RequestParam String key) {
        checkKey(key);

        long count = redissonService.hllCount(key);
        return ok(count);
    }

    /**
     * 合并 HyperLogLog。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/hll/merge?destKey=hll:merge&keys=hll:a&keys=hll:b"
     *
     * @param destKey 目标 Key
     * @param keys    源 Key 集合
     * @return 合并后基数估算
     */
    @PostMapping("/hll/merge")
    public Map<String, Object> hllMerge(@RequestParam String destKey,
                                        @RequestParam List<String> keys) {
        checkKey(destKey);
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.hllMerge(destKey, keys.toArray(new String[0]));
        return ok(count);
    }

    /**
     * 记录 UV。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/uv/record?keyPrefix=uv&bizKey=home&userFlag=user:1&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param userFlag  用户唯一标识
     * @param date      日期，不传默认今天
     * @return 是否改变基数估算
     */
    @PostMapping("/uv/record")
    public Map<String, Object> uvRecord(@RequestParam(defaultValue = "uv") String keyPrefix,
                                        @RequestParam String bizKey,
                                        @RequestParam String userFlag,
                                        @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(bizKey);
        checkKey(userFlag);

        LocalDate targetDate = parseDate(date);
        boolean success = redissonService.uvRecord(keyPrefix, bizKey, userFlag, targetDate);

        log.info("UV 记录成功，keyPrefix={}，bizKey={}，userFlag={}，date={}",
                keyPrefix, bizKey, userFlag, targetDate);
        return ok(success);
    }

    /**
     * 获取 UV。
     *
     * curl "http://localhost:8080/redisson/scene/uv/count?keyPrefix=uv&bizKey=home&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param date      日期，不传默认今天
     * @return UV 数量
     */
    @GetMapping("/uv/count")
    public Map<String, Object> uvCount(@RequestParam(defaultValue = "uv") String keyPrefix,
                                       @RequestParam String bizKey,
                                       @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(bizKey);

        LocalDate targetDate = parseDate(date);
        long count = redissonService.uvCount(keyPrefix, bizKey, targetDate);

        return ok(count);
    }

    // -------------------------------------------------------------------------
    // Geo / LBS
    // -------------------------------------------------------------------------

    /**
     * 添加地理位置。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/geo/add?key=geo:store&member=store:1&longitude=116.397128&latitude=39.916527"
     *
     * @param key       Redis 键
     * @param member    成员
     * @param longitude 经度
     * @param latitude  纬度
     * @return 添加数量
     */
    @PostMapping("/geo/add")
    public Map<String, Object> geoAdd(@RequestParam String key,
                                      @RequestParam String member,
                                      @RequestParam Double longitude,
                                      @RequestParam Double latitude) {
        checkKey(key);
        checkKey(member);
        checkGeoCoordinate(longitude, latitude);

        long count = redissonService.geoAdd(key, longitude, latitude, member);
        log.info("Geo 添加成功，key={}，member={}，longitude={}，latitude={}", key, member, longitude, latitude);
        return ok(count);
    }

    /**
     * 成员不存在时添加地理位置。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/geo/try-add?key=geo:store&member=store:2&longitude=116.407128&latitude=39.926527"
     *
     * @param key       Redis 键
     * @param member    成员
     * @param longitude 经度
     * @param latitude  纬度
     * @return 是否添加成功
     */
    @PostMapping("/geo/try-add")
    public Map<String, Object> geoTryAdd(@RequestParam String key,
                                         @RequestParam String member,
                                         @RequestParam Double longitude,
                                         @RequestParam Double latitude) {
        checkKey(key);
        checkKey(member);
        checkGeoCoordinate(longitude, latitude);

        boolean success = redissonService.geoTryAdd(key, longitude, latitude, member);
        return ok(success);
    }

    /**
     * 计算两个成员距离。
     *
     * curl "http://localhost:8080/redisson/scene/geo/distance?key=geo:store&member1=store:1&member2=store:2&unit=KILOMETERS"
     *
     * @param key     Redis 键
     * @param member1 成员 1
     * @param member2 成员 2
     * @param unit    距离单位
     * @return 距离
     */
    @GetMapping("/geo/distance")
    public Map<String, Object> geoDistance(@RequestParam String key,
                                           @RequestParam String member1,
                                           @RequestParam String member2,
                                           @RequestParam(defaultValue = "KILOMETERS") GeoUnit unit) {
        checkKey(key);
        checkKey(member1);
        checkKey(member2);
        Assert.notNull(unit, "unit不能为空");

        Double distance = redissonService.geoDistance(key, member1, member2, unit);
        return ok(distance);
    }

    /**
     * 查询成员 GeoHash。
     *
     * curl "http://localhost:8080/redisson/scene/geo/hash?key=geo:store&members=store:1&members=store:2"
     *
     * @param key     Redis 键
     * @param members 成员集合
     * @return GeoHash Map
     */
    @GetMapping("/geo/hash")
    public Map<String, Object> geoHash(@RequestParam String key,
                                       @RequestParam List<String> members) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(members), "members不能为空");

        Map<Object, String> data = redissonService.geoHash(key, members.toArray());
        return ok(data);
    }

    /**
     * 查询成员坐标。
     *
     * curl "http://localhost:8080/redisson/scene/geo/position?key=geo:store&members=store:1&members=store:2"
     *
     * @param key     Redis 键
     * @param members 成员集合
     * @return 坐标 Map
     */
    @GetMapping("/geo/position")
    public Map<String, Object> geoPosition(@RequestParam String key,
                                           @RequestParam List<String> members) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(members), "members不能为空");

        Map<Object, GeoPosition> data = redissonService.geoPosition(key, members.toArray());
        return ok(data);
    }

    /**
     * 根据坐标查询附近成员。
     *
     * curl "http://localhost:8080/redisson/scene/geo/search?key=geo:store&longitude=116.397128&latitude=39.916527&radius=5&unit=KILOMETERS"
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半径
     * @param unit      距离单位
     * @return 附近成员
     */
    @GetMapping("/geo/search")
    public Map<String, Object> geoSearch(@RequestParam String key,
                                         @RequestParam Double longitude,
                                         @RequestParam Double latitude,
                                         @RequestParam Double radius,
                                         @RequestParam(defaultValue = "KILOMETERS") GeoUnit unit) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.isTrue(ObjectUtil.isNotNull(radius) && radius > 0, "radius必须大于0");
        Assert.notNull(unit, "unit不能为空");

        GeoSearchArgs args = GeoSearchArgs.from(longitude, latitude)
                .radius(radius, unit);

        List<Object> data = redissonService.geoSearch(key, args);
        return ok(data);
    }

    /**
     * 根据坐标查询附近成员及距离。
     *
     * curl "http://localhost:8080/redisson/scene/geo/search-with-distance?key=geo:store&longitude=116.397128&latitude=39.916527&radius=5&unit=KILOMETERS"
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半径
     * @param unit      距离单位
     * @return 成员距离 Map
     */
    @GetMapping("/geo/search-with-distance")
    public Map<String, Object> geoSearchWithDistance(@RequestParam String key,
                                                     @RequestParam Double longitude,
                                                     @RequestParam Double latitude,
                                                     @RequestParam Double radius,
                                                     @RequestParam(defaultValue = "KILOMETERS") GeoUnit unit) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.isTrue(ObjectUtil.isNotNull(radius) && radius > 0, "radius必须大于0");
        Assert.notNull(unit, "unit不能为空");

        GeoSearchArgs args = GeoSearchArgs.from(longitude, latitude)
                .radius(radius, unit);

        Map<Object, Double> data = redissonService.geoSearchWithDistance(key, args);
        return ok(data);
    }

    /**
     * 根据坐标查询附近成员及坐标。
     *
     * curl "http://localhost:8080/redisson/scene/geo/search-with-position?key=geo:store&longitude=116.397128&latitude=39.916527&radius=5&unit=KILOMETERS"
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半径
     * @param unit      距离单位
     * @return 成员坐标 Map
     */
    @GetMapping("/geo/search-with-position")
    public Map<String, Object> geoSearchWithPosition(@RequestParam String key,
                                                     @RequestParam Double longitude,
                                                     @RequestParam Double latitude,
                                                     @RequestParam Double radius,
                                                     @RequestParam(defaultValue = "KILOMETERS") GeoUnit unit) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.isTrue(ObjectUtil.isNotNull(radius) && radius > 0, "radius必须大于0");
        Assert.notNull(unit, "unit不能为空");

        GeoSearchArgs args = GeoSearchArgs.from(longitude, latitude)
                .radius(radius, unit);

        Map<Object, GeoPosition> data = redissonService.geoSearchWithPosition(key, args);
        return ok(data);
    }

    /**
     * 删除地理位置成员。
     *
     * curl -X DELETE "http://localhost:8080/redisson/scene/geo/remove?key=geo:store&members=store:1&members=store:2"
     *
     * @param key     Redis 键
     * @param members 成员集合
     * @return 删除数量
     */
    @DeleteMapping("/geo/remove")
    public Map<String, Object> geoRemove(@RequestParam String key,
                                         @RequestParam List<String> members) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(members), "members不能为空");

        long count = redissonService.geoRemove(key, members.toArray());
        log.info("Geo 成员删除成功，key={}，members={}，count={}", key, members, count);
        return ok(count);
    }

    // -------------------------------------------------------------------------
    // Session
    // -------------------------------------------------------------------------

    /**
     * 创建或覆盖会话。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/session/set?keyPrefix=session&token=token001&ttlSeconds=1800" \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":1001,"username":"ateng"}'
     *
     * @param keyPrefix  会话 key 前缀
     * @param token      Token
     * @param ttlSeconds 过期秒数
     * @param session    会话对象
     * @return 执行结果
     */
    @PostMapping("/session/set")
    public Map<String, Object> sessionSet(@RequestParam(defaultValue = "session") String keyPrefix,
                                          @RequestParam String token,
                                          @RequestParam(defaultValue = "1800") Long ttlSeconds,
                                          @RequestBody Map<String, Object> session) {
        checkKey(keyPrefix);
        checkKey(token);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");
        Assert.isTrue(CollUtil.isNotEmpty(session), "session不能为空");

        redissonService.sessionSet(keyPrefix, token, session, Duration.ofSeconds(ttlSeconds));
        log.info("Session 写入成功，keyPrefix={}，token={}，ttlSeconds={}", keyPrefix, token, ttlSeconds);
        return ok(true);
    }

    /**
     * 获取会话。
     *
     * curl "http://localhost:8080/redisson/scene/session/get?keyPrefix=session&token=token001"
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 会话对象
     */
    @GetMapping("/session/get")
    public Map<String, Object> sessionGet(@RequestParam(defaultValue = "session") String keyPrefix,
                                          @RequestParam String token) {
        checkKey(keyPrefix);
        checkKey(token);

        Object session = redissonService.sessionGet(keyPrefix, token, Object.class);
        return ok(session);
    }

    /**
     * 刷新会话过期时间。
     *
     * curl -X PUT "http://localhost:8080/redisson/scene/session/refresh?keyPrefix=session&token=token001&ttlSeconds=1800"
     *
     * @param keyPrefix  会话 key 前缀
     * @param token      Token
     * @param ttlSeconds 过期秒数
     * @return 是否刷新成功
     */
    @PutMapping("/session/refresh")
    public Map<String, Object> sessionRefresh(@RequestParam(defaultValue = "session") String keyPrefix,
                                              @RequestParam String token,
                                              @RequestParam(defaultValue = "1800") Long ttlSeconds) {
        checkKey(keyPrefix);
        checkKey(token);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");

        boolean success = redissonService.sessionRefresh(keyPrefix, token, Duration.ofSeconds(ttlSeconds));
        return ok(success);
    }

    /**
     * 删除会话。
     *
     * curl -X DELETE "http://localhost:8080/redisson/scene/session/delete?keyPrefix=session&token=token001"
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 是否删除成功
     */
    @DeleteMapping("/session/delete")
    public Map<String, Object> sessionDelete(@RequestParam(defaultValue = "session") String keyPrefix,
                                             @RequestParam String token) {
        checkKey(keyPrefix);
        checkKey(token);

        boolean success = redissonService.sessionDelete(keyPrefix, token);
        log.info("Session 删除完成，keyPrefix={}，token={}，success={}", keyPrefix, token, success);
        return ok(success);
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 解析日期。
     *
     * @param date 日期文本
     * @return 日期
     */
    private LocalDate parseDate(String date) {
        if (StrUtil.isBlank(date)) {
            return LocalDate.now();
        }
        return LocalDate.parse(date);
    }

    /**
     * 校验 Geo 坐标。
     *
     * @param longitude 经度
     * @param latitude  纬度
     */
    private void checkGeoCoordinate(Double longitude, Double latitude) {
        Assert.notNull(longitude, "longitude不能为空");
        Assert.notNull(latitude, "latitude不能为空");
        Assert.isTrue(longitude >= -180D && longitude <= 180D, "经度必须在 -180 到 180 之间");
        Assert.isTrue(latitude >= -90D && latitude <= 90D, "纬度必须在 -90 到 90 之间");
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

    /**
     * Geo 批量添加请求体。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class GeoAddBatchRequest {

        /**
         * Redis 键。
         */
        private String key;

        /**
         * 地理位置集合。
         */
        private List<GeoItem> items;

    }

    /**
     * Geo 位置项。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class GeoItem {

        /**
         * 成员。
         */
        private String member;

        /**
         * 经度。
         */
        private Double longitude;

        /**
         * 纬度。
         */
        private Double latitude;

    }

}
```

### 高级能力测试控制器

用于演示 RedissonService 的 Lua 脚本、脚本缓存、批处理、事务等高级能力。

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RTransaction;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 高级能力测试控制器
 * 用于演示 RedissonService 的 Lua 脚本、脚本缓存、批处理、事务等高级能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/advanced")
@RequiredArgsConstructor
public class RedissonAdvancedController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // Lua / Script
    // -------------------------------------------------------------------------

    /**
     * 执行 Lua 脚本。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/eval" \
     *   -H "Content-Type: application/json" \
     *   -d '{"script":"return redis.call(\"get\", KEYS[1])","mode":"READ_ONLY","returnType":"VALUE","keys":["lua:test"],"args":[]}'
     *
     * @param request Lua 执行请求
     * @return 执行结果
     */
    @PostMapping("/lua/eval")
    public Map<String, Object> eval(@RequestBody LuaEvalRequest request) {
        checkLuaEvalRequest(request);

        Object result = redissonService.eval(
                request.getScript(),
                request.getMode(),
                request.getReturnType(),
                safeKeys(request.getKeys()),
                safeArgs(request.getArgs())
        );

        return ok(result);
    }

    /**
     * 执行 Lua 脚本并按 Object 返回。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/eval-value" \
     *   -H "Content-Type: application/json" \
     *   -d '{"script":"redis.call(\"set\", KEYS[1], ARGV[1]); return redis.call(\"get\", KEYS[1])","keys":["lua:test"],"args":["hello"]}'
     *
     * @param request Lua 执行请求
     * @return 执行结果
     */
    @PostMapping("/lua/eval-value")
    public Map<String, Object> evalValue(@RequestBody LuaSimpleEvalRequest request) {
        checkLuaSimpleEvalRequest(request);

        Object result = redissonService.eval(
                request.getScript(),
                Object.class,
                safeKeys(request.getKeys()),
                safeArgs(request.getArgs())
        );

        return ok(result);
    }

    /**
     * 执行 Lua 脚本但不关心返回值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/eval-no-result" \
     *   -H "Content-Type: application/json" \
     *   -d '{"script":"redis.call(\"set\", KEYS[1], ARGV[1])","keys":["lua:test"],"args":["hello"]}'
     *
     * @param request Lua 执行请求
     * @return 执行结果
     */
    @PostMapping("/lua/eval-no-result")
    public Map<String, Object> evalNoResult(@RequestBody LuaSimpleEvalRequest request) {
        checkLuaSimpleEvalRequest(request);

        redissonService.evalNoResult(
                request.getScript(),
                safeKeys(request.getKeys()),
                safeArgs(request.getArgs())
        );

        return ok(true);
    }

    /**
     * 加载 Lua 脚本。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/load" \
     *   -H "Content-Type: text/plain" \
     *   -d 'return redis.call("get", KEYS[1])'
     *
     * @param script Lua 脚本
     * @return SHA1
     */
    @PostMapping("/lua/load")
    public Map<String, Object> loadScript(@RequestBody String script) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua脚本不能为空");

        String sha1 = redissonService.loadScript(script);
        log.info("Lua 脚本加载成功，sha1={}", sha1);
        return ok(sha1);
    }

    /**
     * 根据 SHA1 执行 Lua 脚本。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/eval-sha" \
     *   -H "Content-Type: application/json" \
     *   -d '{"sha1":"xxxx","keys":["lua:test"],"args":[]}'
     *
     * @param request Lua SHA 执行请求
     * @return 执行结果
     */
    @PostMapping("/lua/eval-sha")
    public Map<String, Object> evalBySha(@RequestBody LuaEvalShaRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(StrUtil.isNotBlank(request.getSha1()), "sha1不能为空");

        Object result = redissonService.evalBySha(
                request.getSha1(),
                Object.class,
                safeKeys(request.getKeys()),
                safeArgs(request.getArgs())
        );

        return ok(result);
    }

    /**
     * 使用 Lua 实现原子设置并返回旧值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/get-and-set?key=lua:atomic&value=newValue"
     *
     * @param key   Redis 键
     * @param value 新值
     * @return 旧值
     */
    @PostMapping("/lua/get-and-set")
    public Map<String, Object> luaGetAndSet(@RequestParam String key,
                                            @RequestParam String value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        String script = """
                local oldValue = redis.call('get', KEYS[1])
                redis.call('set', KEYS[1], ARGV[1])
                return oldValue
                """;

        Object oldValue = redissonService.eval(
                script,
                RScript.Mode.READ_WRITE,
                RScript.ReturnType.VALUE,
                List.of(key),
                value
        );

        return ok(oldValue);
    }

    /**
     * 使用 Lua 实现存在则递增，不存在则初始化。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/lua/increment-or-init?key=lua:counter&delta=2&initialValue=100"
     *
     * @param key          Redis 键
     * @param delta        增量
     * @param initialValue 初始值
     * @return 最新值
     */
    @PostMapping("/lua/increment-or-init")
    public Map<String, Object> luaIncrementOrInit(@RequestParam String key,
                                                  @RequestParam(defaultValue = "1") Long delta,
                                                  @RequestParam(defaultValue = "0") Long initialValue) {
        checkKey(key);
        Assert.notNull(delta, "delta不能为空");
        Assert.notNull(initialValue, "initialValue不能为空");

        String script = """
                if redis.call('exists', KEYS[1]) == 1 then
                    return redis.call('incrby', KEYS[1], ARGV[1])
                else
                    redis.call('set', KEYS[1], ARGV[2])
                    return tonumber(ARGV[2])
                end
                """;

        Object value = redissonService.eval(
                script,
                RScript.Mode.READ_WRITE,
                RScript.ReturnType.INTEGER,
                List.of(key),
                delta,
                initialValue
        );

        return ok(value);
    }

    // -------------------------------------------------------------------------
    // Batch
    // -------------------------------------------------------------------------

    /**
     * 批量设置 Bucket 值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/batch/bucket-set" \
     *   -H "Content-Type: application/json" \
     *   -d '{"ttlSeconds":300,"items":[{"key":"batch:user:1","value":{"id":1,"name":"Ateng"}},{"key":"batch:user:2","value":{"id":2,"name":"Tom"}}]}'
     *
     * @param request 批量设置请求
     * @return 批处理结果
     */
    @PostMapping("/batch/bucket-set")
    public Map<String, Object> batchBucketSet(@RequestBody BatchBucketSetRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getItems()), "items不能为空");

        RBatch batch = redissonService.createBatch();

        for (BucketItem item : request.getItems()) {
            checkBucketItem(item);

            if (ObjectUtil.isNotNull(request.getTtlSeconds()) && request.getTtlSeconds() > 0) {
                batch.getBucket(item.getKey()).setAsync(item.getValue(), request.getTtlSeconds(), TimeUnit.SECONDS);
            } else {
                batch.getBucket(item.getKey()).setAsync(item.getValue());
            }
        }

        BatchResult<?> result = batch.execute();
        log.info("批量设置 Bucket 完成，size={}", request.getItems().size());

        return ok(batchResultToMap(result));
    }

    /**
     * 批量获取 Bucket 值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/batch/bucket-get" \
     *   -H "Content-Type: application/json" \
     *   -d '{"keys":["batch:user:1","batch:user:2"]}'
     *
     * @param request 批量获取请求
     * @return 批处理结果
     */
    @PostMapping("/batch/bucket-get")
    public Map<String, Object> batchBucketGet(@RequestBody BatchBucketGetRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getKeys()), "keys不能为空");

        RBatch batch = redissonService.createBatch();

        for (String key : request.getKeys()) {
            checkKey(key);
            batch.getBucket(key).getAsync();
        }

        BatchResult<?> result = batch.execute();
        return ok(batchResultToMap(result));
    }

    /**
     * 批量删除 Key。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/batch/key-delete" \
     *   -H "Content-Type: application/json" \
     *   -d '{"keys":["batch:user:1","batch:user:2"]}'
     *
     * @param request 批量删除请求
     * @return 批处理结果
     */
    @PostMapping("/batch/key-delete")
    public Map<String, Object> batchKeyDelete(@RequestBody BatchBucketGetRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getKeys()), "keys不能为空");

        RBatch batch = redissonService.createBatch();

        for (String key : request.getKeys()) {
            checkKey(key);
            batch.getKeys().deleteAsync(key);
        }

        BatchResult<?> result = batch.execute();
        log.info("批量删除 Key 完成，keys={}", request.getKeys());

        return ok(batchResultToMap(result));
    }

    /**
     * 批量计数器递增。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/batch/atomic-long-increment" \
     *   -H "Content-Type: application/json" \
     *   -d '{"items":[{"key":"batch:count:1","delta":1},{"key":"batch:count:2","delta":10}]}'
     *
     * @param request 批量递增请求
     * @return 批处理结果
     */
    @PostMapping("/batch/atomic-long-increment")
    public Map<String, Object> batchAtomicLongIncrement(@RequestBody BatchAtomicLongIncrementRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getItems()), "items不能为空");

        RBatch batch = redissonService.createBatch();

        for (AtomicLongIncrementItem item : request.getItems()) {
            Assert.notNull(item, "item不能为空");
            checkKey(item.getKey());
            Assert.notNull(item.getDelta(), "delta不能为空");

            batch.getAtomicLong(item.getKey()).addAndGetAsync(item.getDelta());
        }

        BatchResult<?> result = batch.execute();
        return ok(batchResultToMap(result));
    }

    // -------------------------------------------------------------------------
    // Transaction
    // -------------------------------------------------------------------------

    /**
     * 事务设置 Bucket 值。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/transaction/bucket-set" \
     *   -H "Content-Type: application/json" \
     *   -d '{"items":[{"key":"tx:user:1","value":{"id":1,"name":"Ateng"}},{"key":"tx:user:2","value":{"id":2,"name":"Tom"}}]}'
     *
     * @param request 事务设置请求
     * @return 执行结果
     */
    @PostMapping("/transaction/bucket-set")
    public Map<String, Object> transactionBucketSet(@RequestBody BatchBucketSetRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(CollUtil.isNotEmpty(request.getItems()), "items不能为空");

        RTransaction transaction = redissonService.createTransaction();

        try {
            for (BucketItem item : request.getItems()) {
                checkBucketItem(item);

                RBucket<Object> bucket = transaction.getBucket(item.getKey());
                if (ObjectUtil.isNotNull(request.getTtlSeconds()) && request.getTtlSeconds() > 0) {
                    bucket.set(item.getValue(), request.getTtlSeconds(), TimeUnit.SECONDS);
                } else {
                    bucket.set(item.getValue());
                }
            }

            transaction.commit();
            log.info("事务设置 Bucket 成功，size={}", request.getItems().size());
            return ok(true);
        } catch (Exception e) {
            rollbackSafely(transaction);
            log.error("事务设置 Bucket 失败，已回滚", e);
            throw e;
        }
    }

    /**
     * Lua 原子转账示例。
     * 使用 Redis 数值 Key 模拟账户余额，from 扣减，to 增加。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/transaction/transfer?fromKey=account:1001&toKey=account:1002&amount=10"
     *
     * @param fromKey 转出账户 Key
     * @param toKey   转入账户 Key
     * @param amount  金额
     * @return 执行结果
     */
    @PostMapping("/transaction/transfer")
    public Map<String, Object> transactionTransfer(@RequestParam String fromKey,
                                                   @RequestParam String toKey,
                                                   @RequestParam Long amount) {
        checkKey(fromKey);
        checkKey(toKey);
        Assert.isTrue(ObjectUtil.isNotNull(amount) && amount > 0, "amount必须大于0");

        String script = """
                local amount = tonumber(ARGV[1])
                if amount == nil or amount <= 0 then
                    return redis.error_reply('amount invalid')
                end

                local fromBalance = tonumber(redis.call('get', KEYS[1]) or '0')
                if fromBalance < amount then
                    return redis.error_reply('balance not enough')
                end

                local latestFrom = redis.call('decrby', KEYS[1], amount)
                local latestTo = redis.call('incrby', KEYS[2], amount)

                return tostring(latestFrom) .. ':' .. tostring(latestTo)
                """;

        Object result = redissonService.eval(
                script,
                RScript.Mode.READ_WRITE,
                RScript.ReturnType.VALUE,
                List.of(fromKey, toKey),
                amount
        );

        String resultText = String.valueOf(result);
        List<String> parts = StrUtil.split(resultText, ":");
        Assert.isTrue(parts.size() == 2, "Lua转账结果格式异常");

        long latestFrom = Long.parseLong(parts.get(0));
        long latestTo = Long.parseLong(parts.get(1));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fromKey", fromKey);
        data.put("toKey", toKey);
        data.put("amount", amount);
        data.put("fromBalance", latestFrom);
        data.put("toBalance", latestTo);

        log.info("Lua 原子转账成功，fromKey={}，toKey={}，amount={}", fromKey, toKey, amount);
        return ok(data);
    }

    /**
     * 初始化事务转账账户余额。
     *
     * curl -X POST "http://localhost:8080/redisson/advanced/transaction/account/init?accountKey=account:1001&balance=100"
     *
     * @param accountKey 账户 Key
     * @param balance    余额
     * @return 执行结果
     */
    @PostMapping("/transaction/account/init")
    public Map<String, Object> initAccount(@RequestParam String accountKey,
                                           @RequestParam Long balance) {
        checkKey(accountKey);
        Assert.isTrue(ObjectUtil.isNotNull(balance) && balance >= 0, "balance不能小于0");

        redissonService.setAtomicLong(accountKey, balance);
        log.info("账户余额初始化成功，accountKey={}，balance={}", accountKey, balance);
        return ok(true);
    }

    /**
     * 查询事务转账账户余额。
     *
     * curl "http://localhost:8080/redisson/advanced/transaction/account/balance?accountKey=account:1001"
     *
     * @param accountKey 账户 Key
     * @return 余额
     */
    @GetMapping("/transaction/account/balance")
    public Map<String, Object> accountBalance(@RequestParam String accountKey) {
        checkKey(accountKey);

        long balance = redissonService.getAtomicLongValue(accountKey);
        return ok(balance);
    }

    /**
     * 获取批处理和事务对象信息。
     *
     * curl "http://localhost:8080/redisson/advanced/info"
     *
     * @return 对象信息
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scriptClass", redissonService.getScript().getClass().getName());
        data.put("batchClass", redissonService.createBatch().getClass().getName());
        data.put("transactionClass", redissonService.createTransaction().getClass().getName());
        return ok(data);
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 校验 Bucket 项。
     *
     * @param item Bucket 项
     */
    private void checkBucketItem(BucketItem item) {
        Assert.notNull(item, "item不能为空");
        checkKey(item.getKey());
        Assert.notNull(item.getValue(), "value不能为空");
    }

    /**
     * 获取安全 KEYS。
     *
     * @param keys KEYS
     * @return KEYS
     */
    private List<Object> safeKeys(List<Object> keys) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyList();
        }
        return keys;
    }

    /**
     * 获取安全 ARGV。
     *
     * @param args ARGV
     * @return ARGV 数组
     */
    private Object[] safeArgs(List<Object> args) {
        if (CollUtil.isEmpty(args)) {
            return new Object[0];
        }
        return args.toArray();
    }

    /**
     * 转换批处理结果。
     *
     * @param result 批处理结果
     * @return 响应数据
     */
    private Map<String, Object> batchResultToMap(BatchResult<?> result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("responses", ObjectUtil.isNull(result) ? Collections.emptyList() : result.getResponses());
        return data;
    }

    /**
     * 安全回滚事务。
     *
     * @param transaction 事务对象
     */
    private void rollbackSafely(RTransaction transaction) {
        if (ObjectUtil.isNull(transaction)) {
            return;
        }

        try {
            transaction.rollback();
        } catch (Exception e) {
            log.error("Redis 事务回滚异常", e);
        }
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

    /**
     * Lua 完整执行请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class LuaEvalRequest {

        /**
         * Lua 脚本。
         */
        private String script;

        /**
         * 执行模式。
         */
        private RScript.Mode mode = RScript.Mode.READ_WRITE;

        /**
         * 返回类型。
         */
        private RScript.ReturnType returnType = RScript.ReturnType.VALUE;

        /**
         * KEYS 参数。
         */
        private List<Object> keys;

        /**
         * ARGV 参数。
         */
        private List<Object> args;

    }

    /**
     * Lua 简单执行请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class LuaSimpleEvalRequest {

        /**
         * Lua 脚本。
         */
        private String script;

        /**
         * KEYS 参数。
         */
        private List<Object> keys;

        /**
         * ARGV 参数。
         */
        private List<Object> args;

    }

    /**
     * Lua SHA 执行请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class LuaEvalShaRequest {

        /**
         * 脚本 SHA1。
         */
        private String sha1;

        /**
         * KEYS 参数。
         */
        private List<Object> keys;

        /**
         * ARGV 参数。
         */
        private List<Object> args;

    }

    /**
     * 批量 Bucket 设置请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class BatchBucketSetRequest {

        /**
         * 过期秒数。
         */
        private Long ttlSeconds;

        /**
         * Bucket 项集合。
         */
        private List<BucketItem> items;

    }

    /**
     * Bucket 项。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class BucketItem {

        /**
         * Redis 键。
         */
        private String key;

        /**
         * Redis 值。
         */
        private Object value;

    }

    /**
     * 批量 Bucket 获取请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class BatchBucketGetRequest {

        /**
         * Redis 键集合。
         */
        private List<String> keys;

    }

    /**
     * 批量 AtomicLong 递增请求。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class BatchAtomicLongIncrementRequest {

        /**
         * 递增项集合。
         */
        private List<AtomicLongIncrementItem> items;

    }

    /**
     * AtomicLong 递增项。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class AtomicLongIncrementItem {

        /**
         * Redis 键。
         */
        private String key;

        /**
         * 增量。
         */
        private Long delta;

    }

    /**
     * 校验 Lua 完整执行请求。
     *
     * @param request Lua 请求
     */
    private void checkLuaEvalRequest(LuaEvalRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(StrUtil.isNotBlank(request.getScript()), "Lua脚本不能为空");
        Assert.notNull(request.getMode(), "mode不能为空");
        Assert.notNull(request.getReturnType(), "returnType不能为空");
    }

    /**
     * 校验 Lua 简单执行请求。
     *
     * @param request Lua 请求
     */
    private void checkLuaSimpleEvalRequest(LuaSimpleEvalRequest request) {
        Assert.notNull(request, "请求体不能为空");
        Assert.isTrue(StrUtil.isNotBlank(request.getScript()), "Lua脚本不能为空");
    }

}
```

### 增强数据结构测试控制器

用于演示 RedissonService 的 LocalCachedMap、JsonBucket、BinaryStream、Multimap、SortedSet、LexSortedSet、Adder、有界队列和优先级队列能力。

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.codec.JsonCodec;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 增强数据结构测试控制器
 * 用于演示 RedissonService 的 LocalCachedMap、JsonBucket、BinaryStream、Multimap、SortedSet、LexSortedSet、Adder、有界队列和优先级队列能力。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Slf4j
@RestController
@RequestMapping("/redisson/enhanced")
@RequiredArgsConstructor
public class RedissonEnhancedDataController {

    private final RedissonService redissonService;

    /**
     * JSON 编解码器。
     * 建议在配置类中注入 JsonCodec Bean，例如 JsonJacksonCodec。
     */
    private final JsonCodec jsonCodec;

    // -------------------------------------------------------------------------
    // LocalCachedMap
    // -------------------------------------------------------------------------

    /**
     * 设置 LocalCachedMap 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/local-cache/put?key=lc:user&field=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"Ateng"}'
     *
     * @param key   Redis 键
     * @param field 字段
     * @param value 字段值
     * @return 执行结果
     */
    @PostMapping("/local-cache/put")
    public Map<String, Object> lcPut(@RequestParam String key,
                                     @RequestParam String field,
                                     @RequestBody Object value) {
        checkKey(key);
        checkKey(field);

        redissonService.lcPut(key, field, value, localCachedMapOptions());
        log.info("LocalCachedMap 写入成功，key={}，field={}", key, field);
        return ok(true);
    }

    /**
     * 字段不存在时设置 LocalCachedMap 字段值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/local-cache/put-if-absent?key=lc:user&field=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"Ateng"}'
     *
     * @param key   Redis 键
     * @param field 字段
     * @param value 字段值
     * @return 是否设置成功
     */
    @PostMapping("/local-cache/put-if-absent")
    public Map<String, Object> lcPutIfAbsent(@RequestParam String key,
                                             @RequestParam String field,
                                             @RequestBody Object value) {
        checkKey(key);
        checkKey(field);

        boolean success = redissonService.lcPutIfAbsent(key, field, value, localCachedMapOptions());
        return ok(success);
    }

    /**
     * 获取 LocalCachedMap 字段值。
     *
     * curl "http://localhost:8080/redisson/enhanced/local-cache/get?key=lc:user&field=user:1"
     *
     * @param key   Redis 键
     * @param field 字段
     * @return 字段值
     */
    @GetMapping("/local-cache/get")
    public Map<String, Object> lcGet(@RequestParam String key,
                                     @RequestParam String field) {
        checkKey(key);
        checkKey(field);

        Object value = redissonService.lcGet(key, field, Object.class, localCachedMapOptions());
        return ok(value);
    }

    /**
     * 删除 LocalCachedMap 字段。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/local-cache/remove?key=lc:user&field=user:1"
     *
     * @param key   Redis 键
     * @param field 字段
     * @return 删除前的值
     */
    @DeleteMapping("/local-cache/remove")
    public Map<String, Object> lcRemove(@RequestParam String key,
                                        @RequestParam String field) {
        checkKey(key);
        checkKey(field);

        Object oldValue = redissonService.lcRemove(key, field, localCachedMapOptions());
        return ok(oldValue);
    }

    /**
     * 判断 LocalCachedMap 字段是否存在。
     *
     * curl "http://localhost:8080/redisson/enhanced/local-cache/contains?key=lc:user&field=user:1"
     *
     * @param key   Redis 键
     * @param field 字段
     * @return 是否存在
     */
    @GetMapping("/local-cache/contains")
    public Map<String, Object> lcContainsKey(@RequestParam String key,
                                             @RequestParam String field) {
        checkKey(key);
        checkKey(field);

        boolean exists = redissonService.lcContainsKey(key, field, localCachedMapOptions());
        return ok(exists);
    }

    /**
     * 获取 LocalCachedMap 大小。
     *
     * curl "http://localhost:8080/redisson/enhanced/local-cache/size?key=lc:user"
     *
     * @param key Redis 键
     * @return 大小
     */
    @GetMapping("/local-cache/size")
    public Map<String, Object> lcSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.lcSize(key, localCachedMapOptions());
        return ok(size);
    }

    /**
     * 清空 LocalCachedMap 远端和本地数据。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/local-cache/clear?key=lc:user"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/local-cache/clear")
    public Map<String, Object> lcClear(@RequestParam String key) {
        checkKey(key);

        redissonService.lcClear(key, localCachedMapOptions());
        log.info("LocalCachedMap 清空成功，key={}", key);
        return ok(true);
    }

    /**
     * 仅清空当前 JVM 的 LocalCachedMap 本地缓存。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/local-cache/clear-local?key=lc:user"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/local-cache/clear-local")
    public Map<String, Object> lcClearLocalCache(@RequestParam String key) {
        checkKey(key);

        redissonService.lcClearLocalCache(key, localCachedMapOptions());
        log.info("LocalCachedMap 本地缓存清空成功，key={}", key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // JsonBucket / RedisJSON
    // -------------------------------------------------------------------------

    /**
     * 设置完整 JSON 文档。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/set?key=json:user:1&ttlSeconds=300" \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"Ateng","tags":["java","redis"]}'
     *
     * @param key        Redis 键
     * @param ttlSeconds 过期秒数，可选
     * @param value      JSON 对象
     * @return 执行结果
     */
    @PostMapping("/json/set")
    public Map<String, Object> jsonSet(@RequestParam String key,
                                       @RequestParam(required = false) Long ttlSeconds,
                                       @RequestBody Object value) {
        checkKey(key);

        if (ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0) {
            redissonService.jsonSet(key, value, Duration.ofSeconds(ttlSeconds), jsonCodec);
        } else {
            redissonService.jsonSet(key, value, jsonCodec);
        }

        log.info("JSON 文档写入成功，key={}，ttlSeconds={}", key, ttlSeconds);
        return ok(true);
    }

    /**
     * 获取完整 JSON 文档。
     *
     * curl "http://localhost:8080/redisson/enhanced/json/get?key=json:user:1"
     *
     * @param key Redis 键
     * @return JSON 对象
     */
    @GetMapping("/json/get")
    public Map<String, Object> jsonGet(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.jsonGet(key, jsonCodec);
        return ok(value);
    }

    /**
     * 根据 JSONPath 获取局部内容。
     *
     * curl "http://localhost:8080/redisson/enhanced/json/path-get?key=json:user:1&path=$.name"
     *
     * @param key  Redis 键
     * @param path JSONPath
     * @return JSONPath 对应值
     */
    @GetMapping("/json/path-get")
    public Map<String, Object> jsonPathGet(@RequestParam String key,
                                           @RequestParam String path) {
        checkKey(key);
        checkKey(path);

        Object value = redissonService.jsonGet(key, path, jsonCodec);
        return ok(value);
    }

    /**
     * 根据 JSONPath 设置局部内容。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/path-set?key=json:user:1&path=$.name" \
     *   -H "Content-Type: application/json" \
     *   -d '"Ateng Updated"'
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @return 执行结果
     */
    @PostMapping("/json/path-set")
    public Map<String, Object> jsonPathSet(@RequestParam String key,
                                           @RequestParam String path,
                                           @RequestBody Object value) {
        checkKey(key);
        checkKey(path);

        redissonService.jsonSet(key, path, value, jsonCodec);
        return ok(true);
    }

    /**
     * JSONPath 不存在时设置局部内容。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/path-set-if-absent?key=json:user:1&path=$.email" \
     *   -H "Content-Type: application/json" \
     *   -d '"ateng@example.com"'
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @return 是否设置成功
     */
    @PostMapping("/json/path-set-if-absent")
    public Map<String, Object> jsonSetIfAbsent(@RequestParam String key,
                                               @RequestParam String path,
                                               @RequestBody Object value) {
        checkKey(key);
        checkKey(path);

        boolean success = redissonService.jsonSetIfAbsent(key, path, value, jsonCodec);
        return ok(success);
    }

    /**
     * JSONPath 存在时设置局部内容。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/path-set-if-exists?key=json:user:1&path=$.name" \
     *   -H "Content-Type: application/json" \
     *   -d '"Ateng Updated"'
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @return 是否设置成功
     */
    @PostMapping("/json/path-set-if-exists")
    public Map<String, Object> jsonSetIfExists(@RequestParam String key,
                                               @RequestParam String path,
                                               @RequestBody Object value) {
        checkKey(key);
        checkKey(path);

        boolean success = redissonService.jsonSetIfExists(key, path, value, jsonCodec);
        return ok(success);
    }

    /**
     * 删除 JSONPath 对应内容。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/json/path-delete?key=json:user:1&path=$.email"
     *
     * @param key  Redis 键
     * @param path JSONPath
     * @return 删除数量
     */
    @DeleteMapping("/json/path-delete")
    public Map<String, Object> jsonDelete(@RequestParam String key,
                                          @RequestParam String path) {
        checkKey(key);
        checkKey(path);

        long count = redissonService.jsonDelete(key, path, jsonCodec);
        return ok(count);
    }

    /**
     * 向 JSON 数组追加元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/json/array-append?key=json:user:1&path=$.tags" \
     *   -H "Content-Type: application/json" \
     *   -d '["springboot","redisson"]'
     *
     * @param key    Redis 键
     * @param path   JSONPath
     * @param values 追加元素集合
     * @return 追加后数组长度
     */
    @PostMapping("/json/array-append")
    public Map<String, Object> jsonArrayAppend(@RequestParam String key,
                                               @RequestParam String path,
                                               @RequestBody List<Object> values) {
        checkKey(key);
        checkKey(path);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        long length = redissonService.jsonArrayAppend(key, path, jsonCodec, values.toArray());
        return ok(length);
    }

    /**
     * 获取 JSON 对象字段名。
     *
     * curl "http://localhost:8080/redisson/enhanced/json/keys?key=json:user:1"
     *
     * @param key Redis 键
     * @return 字段名集合
     */
    @GetMapping("/json/keys")
    public Map<String, Object> jsonKeys(@RequestParam String key) {
        checkKey(key);

        List<String> keys = redissonService.jsonKeys(key, jsonCodec);
        return ok(keys);
    }

    /**
     * 清空 JSON 文档。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/json/clear?key=json:user:1"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/json/clear")
    public Map<String, Object> jsonClear(@RequestParam String key) {
        checkKey(key);

        redissonService.jsonClear(key, jsonCodec);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // BinaryStream
    // -------------------------------------------------------------------------

    /**
     * 写入文本到二进制流。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/binary/write-text?key=binary:test" \
     *   -H "Content-Type: text/plain" \
     *   -d 'hello redisson'
     *
     * @param key  Redis 键
     * @param text 文本内容
     * @return 执行结果
     */
    @PostMapping("/binary/write-text")
    public Map<String, Object> binaryWriteText(@RequestParam String key,
                                               @RequestBody String text) {
        checkKey(key);
        Assert.notNull(text, "text不能为空");

        redissonService.binaryWrite(key, text.getBytes(StandardCharsets.UTF_8));
        log.info("二进制文本写入成功，key={}，length={}", key, text.length());
        return ok(true);
    }

    /**
     * 写入 Base64 二进制数据。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/binary/write-base64?key=binary:test" \
     *   -H "Content-Type: text/plain" \
     *   -d 'aGVsbG8='
     *
     * @param key        Redis 键
     * @param base64Text Base64 文本
     * @return 执行结果
     */
    @PostMapping("/binary/write-base64")
    public Map<String, Object> binaryWriteBase64(@RequestParam String key,
                                                 @RequestBody String base64Text) {
        checkKey(key);
        Assert.isTrue(StrUtil.isNotBlank(base64Text), "base64Text不能为空");

        redissonService.binaryWrite(key, Base64.decode(base64Text));
        return ok(true);
    }

    /**
     * 读取二进制数据并按 UTF-8 文本返回。
     *
     * curl "http://localhost:8080/redisson/enhanced/binary/read-text?key=binary:test"
     *
     * @param key Redis 键
     * @return 文本内容
     */
    @GetMapping("/binary/read-text")
    public Map<String, Object> binaryReadText(@RequestParam String key) {
        checkKey(key);

        byte[] data = redissonService.binaryReadAll(key);
        return ok(new String(data, StandardCharsets.UTF_8));
    }

    /**
     * 读取二进制数据并按 Base64 返回。
     *
     * curl "http://localhost:8080/redisson/enhanced/binary/read-base64?key=binary:test"
     *
     * @param key Redis 键
     * @return Base64 文本
     */
    @GetMapping("/binary/read-base64")
    public Map<String, Object> binaryReadBase64(@RequestParam String key) {
        checkKey(key);

        byte[] data = redissonService.binaryReadAll(key);
        return ok(Base64.encode(data));
    }

    /**
     * 获取二进制数据大小。
     *
     * curl "http://localhost:8080/redisson/enhanced/binary/size?key=binary:test"
     *
     * @param key Redis 键
     * @return 字节大小
     */
    @GetMapping("/binary/size")
    public Map<String, Object> binarySize(@RequestParam String key) {
        checkKey(key);

        long size = redissonService.binarySize(key);
        return ok(size);
    }

    /**
     * 删除二进制数据。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/binary/delete?key=binary:test"
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @DeleteMapping("/binary/delete")
    public Map<String, Object> binaryDelete(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.binaryDelete(key);
        return ok(success);
    }

    // -------------------------------------------------------------------------
    // SetMultimap / ListMultimap
    // -------------------------------------------------------------------------

    /**
     * 向 SetMultimap 添加值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/set-multimap/put?key=smm:user-role&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"admin"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否新增
     */
    @PostMapping("/set-multimap/put")
    public Map<String, Object> smmPut(@RequestParam String key,
                                      @RequestParam String mapKey,
                                      @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean success = redissonService.smmPut(key, mapKey, mapValue);
        return ok(success);
    }

    /**
     * 获取 SetMultimap 字段值集合。
     *
     * curl "http://localhost:8080/redisson/enhanced/set-multimap/get?key=smm:user-role&mapKey=user:1"
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 值集合
     */
    @GetMapping("/set-multimap/get")
    public Map<String, Object> smmGet(@RequestParam String key,
                                      @RequestParam String mapKey) {
        checkKey(key);
        checkKey(mapKey);

        Set<Object> data = redissonService.smmGet(key, mapKey);
        return ok(data);
    }

    /**
     * 删除 SetMultimap 字段的指定值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/set-multimap/remove?key=smm:user-role&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"admin"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否删除成功
     */
    @PostMapping("/set-multimap/remove")
    public Map<String, Object> smmRemove(@RequestParam String key,
                                         @RequestParam String mapKey,
                                         @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean success = redissonService.smmRemove(key, mapKey, mapValue);
        return ok(success);
    }

    /**
     * 删除 SetMultimap 字段的全部值。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/set-multimap/remove-all?key=smm:user-role&mapKey=user:1"
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 删除的值集合
     */
    @DeleteMapping("/set-multimap/remove-all")
    public Map<String, Object> smmRemoveAll(@RequestParam String key,
                                            @RequestParam String mapKey) {
        checkKey(key);
        checkKey(mapKey);

        Set<Object> data = redissonService.smmRemoveAll(key, mapKey);
        return ok(data);
    }

    /**
     * 判断 SetMultimap 是否包含字段和值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/set-multimap/contains-entry?key=smm:user-role&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"admin"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否包含
     */
    @PostMapping("/set-multimap/contains-entry")
    public Map<String, Object> smmContainsEntry(@RequestParam String key,
                                                @RequestParam String mapKey,
                                                @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean exists = redissonService.smmContainsEntry(key, mapKey, mapValue);
        return ok(exists);
    }

    /**
     * 获取 SetMultimap 总值数量。
     *
     * curl "http://localhost:8080/redisson/enhanced/set-multimap/size?key=smm:user-role"
     *
     * @param key Redis 键
     * @return 总值数量
     */
    @GetMapping("/set-multimap/size")
    public Map<String, Object> smmSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.smmSize(key);
        return ok(size);
    }

    /**
     * 清空 SetMultimap。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/set-multimap/clear?key=smm:user-role"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/set-multimap/clear")
    public Map<String, Object> smmClear(@RequestParam String key) {
        checkKey(key);

        redissonService.smmClear(key);
        return ok(true);
    }

    /**
     * 向 ListMultimap 添加值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/list-multimap/put?key=lmm:user-tag&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"java"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否新增
     */
    @PostMapping("/list-multimap/put")
    public Map<String, Object> lmmPut(@RequestParam String key,
                                      @RequestParam String mapKey,
                                      @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean success = redissonService.lmmPut(key, mapKey, mapValue);
        return ok(success);
    }

    /**
     * 获取 ListMultimap 字段值列表。
     *
     * curl "http://localhost:8080/redisson/enhanced/list-multimap/get?key=lmm:user-tag&mapKey=user:1"
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 值列表
     */
    @GetMapping("/list-multimap/get")
    public Map<String, Object> lmmGet(@RequestParam String key,
                                      @RequestParam String mapKey) {
        checkKey(key);
        checkKey(mapKey);

        List<Object> data = redissonService.lmmGet(key, mapKey);
        return ok(data);
    }

    /**
     * 删除 ListMultimap 字段的指定值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/list-multimap/remove?key=lmm:user-tag&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"java"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否删除成功
     */
    @PostMapping("/list-multimap/remove")
    public Map<String, Object> lmmRemove(@RequestParam String key,
                                         @RequestParam String mapKey,
                                         @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean success = redissonService.lmmRemove(key, mapKey, mapValue);
        return ok(success);
    }

    /**
     * 删除 ListMultimap 字段的全部值。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/list-multimap/remove-all?key=lmm:user-tag&mapKey=user:1"
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 删除的值列表
     */
    @DeleteMapping("/list-multimap/remove-all")
    public Map<String, Object> lmmRemoveAll(@RequestParam String key,
                                            @RequestParam String mapKey) {
        checkKey(key);
        checkKey(mapKey);

        List<Object> data = redissonService.lmmRemoveAll(key, mapKey);
        return ok(data);
    }

    /**
     * 判断 ListMultimap 是否包含字段和值。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/list-multimap/contains-entry?key=lmm:user-tag&mapKey=user:1" \
     *   -H "Content-Type: application/json" \
     *   -d '"java"'
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否包含
     */
    @PostMapping("/list-multimap/contains-entry")
    public Map<String, Object> lmmContainsEntry(@RequestParam String key,
                                                @RequestParam String mapKey,
                                                @RequestBody Object mapValue) {
        checkKey(key);
        checkKey(mapKey);

        boolean exists = redissonService.lmmContainsEntry(key, mapKey, mapValue);
        return ok(exists);
    }

    /**
     * 获取 ListMultimap 总值数量。
     *
     * curl "http://localhost:8080/redisson/enhanced/list-multimap/size?key=lmm:user-tag"
     *
     * @param key Redis 键
     * @return 总值数量
     */
    @GetMapping("/list-multimap/size")
    public Map<String, Object> lmmSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.lmmSize(key);
        return ok(size);
    }

    /**
     * 清空 ListMultimap。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/list-multimap/clear?key=lmm:user-tag"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/list-multimap/clear")
    public Map<String, Object> lmmClear(@RequestParam String key) {
        checkKey(key);

        redissonService.lmmClear(key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // SortedSet / LexSortedSet
    // -------------------------------------------------------------------------

    /**
     * 添加自然排序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/sorted-set/add?key=sorted:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"B"'
     *
     * @param key   Redis 键
     * @param value 元素，建议实现 Comparable
     * @return 是否新增
     */
    @PostMapping("/sorted-set/add")
    public Map<String, Object> sortedSetAdd(@RequestParam String key,
                                            @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        boolean success = redissonService.sortedSetAdd(key, value);
        return ok(success);
    }

    /**
     * 批量添加自然排序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/sorted-set/add-all?key=sorted:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B","C"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @PostMapping("/sorted-set/add-all")
    public Map<String, Object> sortedSetAddAll(@RequestParam String key,
                                               @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.sortedSetAddAll(key, values);
        return ok(success);
    }

    /**
     * 获取自然排序集合全部元素。
     *
     * curl "http://localhost:8080/redisson/enhanced/sorted-set/read-all?key=sorted:test"
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @GetMapping("/sorted-set/read-all")
    public Map<String, Object> sortedSetReadAll(@RequestParam String key) {
        checkKey(key);

        Collection<Object> data = redissonService.sortedSetReadAll(key);
        return ok(data);
    }

    /**
     * 删除自然排序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/sorted-set/remove?key=sorted:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["A","B"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否删除成功
     */
    @PostMapping("/sorted-set/remove")
    public Map<String, Object> sortedSetRemove(@RequestParam String key,
                                               @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.sortedSetRemove(key, values.toArray());
        return ok(success);
    }

    /**
     * 获取自然排序集合大小。
     *
     * curl "http://localhost:8080/redisson/enhanced/sorted-set/size?key=sorted:test"
     *
     * @param key Redis 键
     * @return 大小
     */
    @GetMapping("/sorted-set/size")
    public Map<String, Object> sortedSetSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.sortedSetSize(key);
        return ok(size);
    }

    /**
     * 清空自然排序集合。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/sorted-set/clear?key=sorted:test"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/sorted-set/clear")
    public Map<String, Object> sortedSetClear(@RequestParam String key) {
        checkKey(key);

        redissonService.sortedSetClear(key);
        return ok(true);
    }

    /**
     * 添加字典序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/lex/add?key=lex:test&value=apple"
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    @PostMapping("/lex/add")
    public Map<String, Object> lexAdd(@RequestParam String key,
                                      @RequestParam String value) {
        checkKey(key);
        checkKey(value);

        boolean success = redissonService.lexAdd(key, value);
        return ok(success);
    }

    /**
     * 批量添加字典序集合元素。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/lex/add-all?key=lex:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["apple","banana","cat"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @PostMapping("/lex/add-all")
    public Map<String, Object> lexAddAll(@RequestParam String key,
                                         @RequestBody List<String> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.lexAddAll(key, values);
        return ok(success);
    }

    /**
     * 获取字典序集合全部元素。
     *
     * curl "http://localhost:8080/redisson/enhanced/lex/read-all?key=lex:test"
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @GetMapping("/lex/read-all")
    public Map<String, Object> lexReadAll(@RequestParam String key) {
        checkKey(key);

        Collection<String> data = redissonService.lexReadAll(key);
        return ok(data);
    }

    /**
     * 获取大于等于指定元素的字典序集合。
     *
     * curl "http://localhost:8080/redisson/enhanced/lex/range-tail?key=lex:test&from=banana"
     *
     * @param key  Redis 键
     * @param from 开始元素
     * @return 元素集合
     */
    @GetMapping("/lex/range-tail")
    public Map<String, Object> lexRangeTail(@RequestParam String key,
                                            @RequestParam String from) {
        checkKey(key);
        checkKey(from);

        Collection<String> data = redissonService.lexRangeTail(key, from);
        return ok(data);
    }

    /**
     * 获取小于等于指定元素的字典序集合。
     *
     * curl "http://localhost:8080/redisson/enhanced/lex/range-head?key=lex:test&to=banana"
     *
     * @param key Redis 键
     * @param to  结束元素
     * @return 元素集合
     */
    @GetMapping("/lex/range-head")
    public Map<String, Object> lexRangeHead(@RequestParam String key,
                                            @RequestParam String to) {
        checkKey(key);
        checkKey(to);

        Collection<String> data = redissonService.lexRangeHead(key, to);
        return ok(data);
    }

    /**
     * 删除字典序集合元素。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/lex/remove?key=lex:test&values=apple&values=banana"
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否删除成功
     */
    @DeleteMapping("/lex/remove")
    public Map<String, Object> lexRemove(@RequestParam String key,
                                         @RequestParam List<String> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.lexRemove(key, values.toArray(new String[0]));
        return ok(success);
    }

    /**
     * 获取字典序集合大小。
     *
     * curl "http://localhost:8080/redisson/enhanced/lex/size?key=lex:test"
     *
     * @param key Redis 键
     * @return 大小
     */
    @GetMapping("/lex/size")
    public Map<String, Object> lexSize(@RequestParam String key) {
        checkKey(key);

        int size = redissonService.lexSize(key);
        return ok(size);
    }

    /**
     * 清空字典序集合。
     *
     * curl -X DELETE "http://localhost:8080/redisson/enhanced/lex/clear?key=lex:test"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/lex/clear")
    public Map<String, Object> lexClear(@RequestParam String key) {
        checkKey(key);

        redissonService.lexClear(key);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // LongAdder / DoubleAdder
    // -------------------------------------------------------------------------

    /**
     * LongAdder 增加。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/long-adder/add?key=adder:pv&delta=1"
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 执行结果
     */
    @PostMapping("/long-adder/add")
    public Map<String, Object> longAdderAdd(@RequestParam String key,
                                            @RequestParam(defaultValue = "1") Long delta) {
        checkKey(key);
        Assert.notNull(delta, "delta不能为空");

        redissonService.longAdderAdd(key, delta);
        return ok(true);
    }

    /**
     * LongAdder 求和。
     *
     * curl "http://localhost:8080/redisson/enhanced/long-adder/sum?key=adder:pv"
     *
     * @param key Redis 键
     * @return 当前总和
     */
    @GetMapping("/long-adder/sum")
    public Map<String, Object> longAdderSum(@RequestParam String key) {
        checkKey(key);

        long value = redissonService.longAdderSum(key);
        return ok(value);
    }

    /**
     * LongAdder 重置。
     *
     * curl -X PUT "http://localhost:8080/redisson/enhanced/long-adder/reset?key=adder:pv"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @PutMapping("/long-adder/reset")
    public Map<String, Object> longAdderReset(@RequestParam String key) {
        checkKey(key);

        redissonService.longAdderReset(key);
        return ok(true);
    }

    /**
     * LongAdder 求和后重置。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/long-adder/sum-then-reset?key=adder:pv"
     *
     * @param key Redis 键
     * @return 重置前总和
     */
    @PostMapping("/long-adder/sum-then-reset")
    public Map<String, Object> longAdderSumThenReset(@RequestParam String key) {
        checkKey(key);

        long value = redissonService.longAdderSumThenReset(key);
        return ok(value);
    }

    /**
     * DoubleAdder 增加。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/double-adder/add?key=adder:score&delta=1.5"
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 执行结果
     */
    @PostMapping("/double-adder/add")
    public Map<String, Object> doubleAdderAdd(@RequestParam String key,
                                              @RequestParam(defaultValue = "1") Double delta) {
        checkKey(key);
        Assert.notNull(delta, "delta不能为空");

        redissonService.doubleAdderAdd(key, delta);
        return ok(true);
    }

    /**
     * DoubleAdder 求和。
     *
     * curl "http://localhost:8080/redisson/enhanced/double-adder/sum?key=adder:score"
     *
     * @param key Redis 键
     * @return 当前总和
     */
    @GetMapping("/double-adder/sum")
    public Map<String, Object> doubleAdderSum(@RequestParam String key) {
        checkKey(key);

        double value = redissonService.doubleAdderSum(key);
        return ok(value);
    }

    /**
     * DoubleAdder 重置。
     *
     * curl -X PUT "http://localhost:8080/redisson/enhanced/double-adder/reset?key=adder:score"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @PutMapping("/double-adder/reset")
    public Map<String, Object> doubleAdderReset(@RequestParam String key) {
        checkKey(key);

        redissonService.doubleAdderReset(key);
        return ok(true);
    }

    /**
     * DoubleAdder 求和后重置。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/double-adder/sum-then-reset?key=adder:score"
     *
     * @param key Redis 键
     * @return 重置前总和
     */
    @PostMapping("/double-adder/sum-then-reset")
    public Map<String, Object> doubleAdderSumThenReset(@RequestParam String key) {
        checkKey(key);

        double value = redissonService.doubleAdderSumThenReset(key);
        return ok(value);
    }

    // -------------------------------------------------------------------------
    // Bounded / Priority Queue
    // -------------------------------------------------------------------------

    /**
     * 初始化有界阻塞队列容量。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/init?key=bq:test&capacity=10"
     *
     * @param key      Redis 键
     * @param capacity 容量
     * @return 是否初始化成功
     */
    @PostMapping("/bounded-queue/init")
    public Map<String, Object> boundedQueueTrySetCapacity(@RequestParam String key,
                                                          @RequestParam Integer capacity) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(capacity) && capacity > 0, "capacity必须大于0");

        boolean success = redissonService.boundedQueueTrySetCapacity(key, capacity);
        return ok(success);
    }

    /**
     * 有界阻塞队列入队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/offer?key=bq:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否入队成功
     */
    @PostMapping("/bounded-queue/offer")
    public Map<String, Object> boundedQueueOffer(@RequestParam String key,
                                                 @RequestBody Object value) {
        checkKey(key);

        boolean success = redissonService.boundedQueueOffer(key, value);
        return ok(success);
    }

    /**
     * 有界阻塞队列超时入队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/offer-timeout?key=bq:test&timeoutSeconds=3" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key            Redis 键
     * @param timeoutSeconds 等待秒数
     * @param value          元素
     * @return 是否入队成功
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/bounded-queue/offer-timeout")
    public Map<String, Object> boundedQueueOfferTimeout(@RequestParam String key,
                                                        @RequestParam(defaultValue = "3") Long timeoutSeconds,
                                                        @RequestBody Object value) throws InterruptedException {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        boolean success = redissonService.boundedQueueOffer(key, value, timeoutSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 有界阻塞队列出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/poll?key=bq:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/bounded-queue/poll")
    public Map<String, Object> boundedQueuePoll(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.boundedQueuePoll(key);
        return ok(value);
    }

    /**
     * 有界阻塞队列超时出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/bounded-queue/poll-timeout?key=bq:test&timeoutSeconds=3"
     *
     * @param key            Redis 键
     * @param timeoutSeconds 等待秒数
     * @return 元素
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/bounded-queue/poll-timeout")
    public Map<String, Object> boundedQueuePollTimeout(@RequestParam String key,
                                                       @RequestParam(defaultValue = "3") Long timeoutSeconds) throws InterruptedException {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        Object value = redissonService.boundedQueuePoll(key, timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 获取有界阻塞队列信息。
     *
     * curl "http://localhost:8080/redisson/enhanced/bounded-queue/info?key=bq:test"
     *
     * @param key Redis 键
     * @return 队列信息
     */
    @GetMapping("/bounded-queue/info")
    public Map<String, Object> boundedQueueInfo(@RequestParam String key) {
        checkKey(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("size", redissonService.boundedQueueSize(key));
        data.put("remainingCapacity", redissonService.boundedQueueRemainingCapacity(key));
        return ok(data);
    }

    /**
     * 优先级队列入队。
     * 注意：元素建议使用 String、Integer 等可比较类型，或自定义实现 Comparable。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-queue/offer?key=pq:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否入队成功
     */
    @PostMapping("/priority-queue/offer")
    public Map<String, Object> priorityQueueOffer(@RequestParam String key,
                                                  @RequestBody Object value) {
        checkKey(key);

        boolean success = redissonService.priorityQueueOffer(key, value);
        return ok(success);
    }

    /**
     * 优先级队列出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-queue/poll?key=pq:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/priority-queue/poll")
    public Map<String, Object> priorityQueuePoll(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.priorityQueuePoll(key);
        return ok(value);
    }

    /**
     * 优先级双端队列头部入队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-deque/offer-first?key=pdq:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否入队成功
     */
    @PostMapping("/priority-deque/offer-first")
    public Map<String, Object> priorityDequeOfferFirst(@RequestParam String key,
                                                       @RequestBody Object value) {
        checkKey(key);

        boolean success = redissonService.priorityDequeOfferFirst(key, value);
        return ok(success);
    }

    /**
     * 优先级双端队列尾部入队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-deque/offer-last?key=pdq:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"B"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否入队成功
     */
    @PostMapping("/priority-deque/offer-last")
    public Map<String, Object> priorityDequeOfferLast(@RequestParam String key,
                                                      @RequestBody Object value) {
        checkKey(key);

        boolean success = redissonService.priorityDequeOfferLast(key, value);
        return ok(success);
    }

    /**
     * 优先级双端队列头部出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-deque/poll-first?key=pdq:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/priority-deque/poll-first")
    public Map<String, Object> priorityDequePollFirst(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.priorityDequePollFirst(key);
        return ok(value);
    }

    /**
     * 优先级双端队列尾部出队。
     *
     * curl -X POST "http://localhost:8080/redisson/enhanced/priority-deque/poll-last?key=pdq:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/priority-deque/poll-last")
    public Map<String, Object> priorityDequePollLast(@RequestParam String key) {
        checkKey(key);

        Object value = redissonService.priorityDequePollLast(key);
        return ok(value);
    }

    /**
     * 构建 LocalCachedMap 默认配置。
     *
     * @return LocalCachedMapOptions
     */
    private LocalCachedMapOptions<Object, Object> localCachedMapOptions() {
        return LocalCachedMapOptions.<Object, Object>defaults();
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

    /**
     * JSONPath 设置请求体。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    @Data
    public static class JsonPathSetRequest {

        /**
         * JSONPath。
         */
        private String path;

        /**
         * 值。
         */
        private Object value;

    }

}
```

### 高级消息与服务测试控制器

用于演示 RedissonService 的可靠队列、Stream 高级治理、分布式执行器、远程服务、LiveObject 和对象监听能力。

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.queue.*;
import org.redisson.api.stream.StreamTrimArgs;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 高级消息与服务测试控制器
 * 用于演示 RedissonService 的可靠队列、Stream 高级治理、分布式执行器、远程服务、LiveObject 和对象监听能力。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Slf4j
@RestController
@RequestMapping("/redisson/advanced-message")
@RequiredArgsConstructor
public class RedissonAdvancedMessageController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // ReliableQueue / 可靠队列
    // -------------------------------------------------------------------------

    /**
     * 初始化可靠队列配置。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/config/init?key=rq:test&deliveryLimit=5&visibilitySeconds=30&ttlSeconds=3600&delaySeconds=0&maxSize=1000"
     *
     * @param key               队列 key
     * @param deliveryLimit     最大投递次数
     * @param visibilitySeconds 消息可见性超时秒数
     * @param ttlSeconds        消息 TTL 秒数，0 表示不限制
     * @param delaySeconds      默认延迟秒数，0 表示不延迟
     * @param maxSize           最大队列大小，0 表示不限制
     * @return 是否初始化成功
     */
    @PostMapping("/reliable-queue/config/init")
    public Map<String, Object> reliableQueueConfigInit(@RequestParam String key,
                                                       @RequestParam(defaultValue = "10") Integer deliveryLimit,
                                                       @RequestParam(defaultValue = "30") Long visibilitySeconds,
                                                       @RequestParam(defaultValue = "0") Long ttlSeconds,
                                                       @RequestParam(defaultValue = "0") Long delaySeconds,
                                                       @RequestParam(defaultValue = "0") Integer maxSize) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(deliveryLimit) && deliveryLimit > 0, "deliveryLimit必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(visibilitySeconds) && visibilitySeconds >= 0, "visibilitySeconds不能小于0");
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds >= 0, "ttlSeconds不能小于0");
        Assert.isTrue(ObjectUtil.isNotNull(delaySeconds) && delaySeconds >= 0, "delaySeconds不能小于0");
        Assert.isTrue(ObjectUtil.isNotNull(maxSize) && maxSize >= 0, "maxSize不能小于0");

        QueueConfig config = QueueConfig.defaults()
                .deliveryLimit(deliveryLimit)
                .visibility(Duration.ofSeconds(visibilitySeconds))
                .timeToLive(Duration.ofSeconds(ttlSeconds))
                .delay(Duration.ofSeconds(delaySeconds))
                .maxSize(maxSize);

        boolean success = redissonService.reliableQueueSetConfigIfAbsent(key, config);
        return ok(success);
    }

    /**
     * 覆盖可靠队列配置。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/advanced-message/reliable-queue/config?key=rq:test&deliveryLimit=5&visibilitySeconds=30&ttlSeconds=3600&delaySeconds=0&maxSize=1000"
     *
     * @param key               队列 key
     * @param deliveryLimit     最大投递次数
     * @param visibilitySeconds 消息可见性超时秒数
     * @param ttlSeconds        消息 TTL 秒数，0 表示不限制
     * @param delaySeconds      默认延迟秒数，0 表示不延迟
     * @param maxSize           最大队列大小，0 表示不限制
     * @return 执行结果
     */
    @PutMapping("/reliable-queue/config")
    public Map<String, Object> reliableQueueConfigSet(@RequestParam String key,
                                                      @RequestParam(defaultValue = "10") Integer deliveryLimit,
                                                      @RequestParam(defaultValue = "30") Long visibilitySeconds,
                                                      @RequestParam(defaultValue = "0") Long ttlSeconds,
                                                      @RequestParam(defaultValue = "0") Long delaySeconds,
                                                      @RequestParam(defaultValue = "0") Integer maxSize) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(deliveryLimit) && deliveryLimit > 0, "deliveryLimit必须大于0");

        QueueConfig config = QueueConfig.defaults()
                .deliveryLimit(deliveryLimit)
                .visibility(Duration.ofSeconds(visibilitySeconds))
                .timeToLive(Duration.ofSeconds(ttlSeconds))
                .delay(Duration.ofSeconds(delaySeconds))
                .maxSize(maxSize);

        redissonService.reliableQueueSetConfig(key, config);
        return ok(true);
    }

    /**
     * 添加可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/add?key=rq:test&delaySeconds=0&ttlSeconds=3600&deliveryLimit=5&priority=0" \
     * -H "Content-Type: application/json" \
     * -d '{"id":1,"name":"Ateng"}'
     *
     * @param key             队列 key
     * @param delaySeconds    消息延迟秒数
     * @param ttlSeconds      消息 TTL 秒数，0 表示不限制
     * @param deliveryLimit   最大投递次数
     * @param priority        优先级
     * @param deduplicationId 去重 ID，可选
     * @param payload         消息体
     * @return 消息信息
     */
    @PostMapping("/reliable-queue/add")
    public Map<String, Object> reliableQueueAdd(@RequestParam String key,
                                                @RequestParam(defaultValue = "0") Long delaySeconds,
                                                @RequestParam(defaultValue = "0") Long ttlSeconds,
                                                @RequestParam(defaultValue = "10") Integer deliveryLimit,
                                                @RequestParam(defaultValue = "0") Integer priority,
                                                @RequestParam(required = false) String deduplicationId,
                                                @RequestBody Object payload) {
        checkKey(key);
        Assert.notNull(payload, "payload不能为空");

        MessageArgs<Object> messageArgs = MessageArgs.payload(payload)
                .deliveryLimit(deliveryLimit)
                .priority(priority);

        if (ObjectUtil.isNotNull(delaySeconds) && delaySeconds > 0) {
            messageArgs.delay(Duration.ofSeconds(delaySeconds));
        }
        if (ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0) {
            messageArgs.timeToLive(Duration.ofSeconds(ttlSeconds));
        }
        if (StrUtil.isNotBlank(deduplicationId)) {
            messageArgs.deduplicationById(deduplicationId, Duration.ofHours(1));
        }

        Message<Object> message = redissonService.reliableQueueAdd(key, QueueAddArgs.messages(messageArgs));
        return ok(messageToMap(message));
    }

    /**
     * 批量添加可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/add-many?key=rq:test" \
     * -H "Content-Type: application/json" \
     * -d '["A","B","C"]'
     *
     * @param key      队列 key
     * @param payloads 消息体集合
     * @return 消息信息集合
     */
    @PostMapping("/reliable-queue/add-many")
    public Map<String, Object> reliableQueueAddMany(@RequestParam String key,
                                                    @RequestBody List<Object> payloads) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(payloads), "payloads不能为空");

        @SuppressWarnings("unchecked")
        MessageArgs<Object>[] args = payloads.stream()
                .filter(ObjectUtil::isNotNull)
                .map(MessageArgs::payload)
                .toArray(MessageArgs[]::new);

        if (ArrayUtil.isEmpty(args)) {
            return ok(List.of());
        }

        List<Message<Object>> messages = redissonService.reliableQueueAddMany(key, QueueAddArgs.messages(args));
        return ok(messages.stream().map(this::messageToMap).toList());
    }

    /**
     * 拉取一条可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/poll?key=rq:test&ackMode=MANUAL&visibilitySeconds=30&timeoutSeconds=3"
     *
     * @param key               队列 key
     * @param ackMode           确认模式，MANUAL 或 AUTO
     * @param visibilitySeconds 可见性超时秒数
     * @param timeoutSeconds    长轮询等待秒数
     * @return 消息信息
     */
    @PostMapping("/reliable-queue/poll")
    public Map<String, Object> reliableQueuePoll(@RequestParam String key,
                                                 @RequestParam(defaultValue = "MANUAL") AcknowledgeMode ackMode,
                                                 @RequestParam(defaultValue = "30") Long visibilitySeconds,
                                                 @RequestParam(defaultValue = "0") Long timeoutSeconds) {
        checkKey(key);
        Assert.notNull(ackMode, "ackMode不能为空");

        QueuePollArgs args = QueuePollArgs.defaults()
                .acknowledgeMode(ackMode)
                .visibility(Duration.ofSeconds(visibilitySeconds));

        if (ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds > 0) {
            args.timeout(Duration.ofSeconds(timeoutSeconds));
        }

        Message<Object> message = redissonService.reliableQueuePoll(key, args);
        return ok(messageToMap(message));
    }

    /**
     * 批量拉取可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/poll-many?key=rq:test&count=10&ackMode=MANUAL&visibilitySeconds=30&timeoutSeconds=3"
     *
     * @param key               队列 key
     * @param count             拉取数量
     * @param ackMode           确认模式
     * @param visibilitySeconds 可见性超时秒数
     * @param timeoutSeconds    长轮询等待秒数
     * @return 消息信息集合
     */
    @PostMapping("/reliable-queue/poll-many")
    public Map<String, Object> reliableQueuePollMany(@RequestParam String key,
                                                     @RequestParam(defaultValue = "10") Integer count,
                                                     @RequestParam(defaultValue = "MANUAL") AcknowledgeMode ackMode,
                                                     @RequestParam(defaultValue = "30") Long visibilitySeconds,
                                                     @RequestParam(defaultValue = "0") Long timeoutSeconds) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        QueuePollArgs args = QueuePollArgs.defaults()
                .acknowledgeMode(ackMode)
                .visibility(Duration.ofSeconds(visibilitySeconds))
                .count(count);

        if (ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds > 0) {
            args.timeout(Duration.ofSeconds(timeoutSeconds));
        }

        List<Message<Object>> messages = redissonService.reliableQueuePollMany(key, args);
        return ok(messages.stream().map(this::messageToMap).toList());
    }

    /**
     * 确认可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/ack?key=rq:test&ids=xxx&ids=yyy"
     *
     * @param key 队列 key
     * @param ids 消息 ID 集合
     * @return 执行结果
     */
    @PostMapping("/reliable-queue/ack")
    public Map<String, Object> reliableQueueAck(@RequestParam String key,
                                                @RequestParam List<String> ids) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(ids), "ids不能为空");

        redissonService.reliableQueueAck(key, QueueAckArgs.ids(ids.toArray(new String[0])));
        return ok(true);
    }

    /**
     * 拒绝可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/nack/rejected?key=rq:test&id=xxx"
     *
     * @param key 队列 key
     * @param id  消息 ID
     * @return 执行结果
     */
    @PostMapping("/reliable-queue/nack/rejected")
    public Map<String, Object> reliableQueueNackRejected(@RequestParam String key,
                                                         @RequestParam String id) {
        checkKey(key);
        checkKey(id);

        redissonService.reliableQueueNack(key, QueueNegativeAckArgs.rejected(id));
        return ok(true);
    }

    /**
     * 根据 ID 获取可靠队列消息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/reliable-queue/get?key=rq:test&id=xxx"
     *
     * @param key 队列 key
     * @param id  消息 ID
     * @return 消息信息
     */
    @GetMapping("/reliable-queue/get")
    public Map<String, Object> reliableQueueGet(@RequestParam String key,
                                                @RequestParam String id) {
        checkKey(key);
        checkKey(id);

        Message<Object> message = redissonService.reliableQueueGet(key, id);
        return ok(messageToMap(message));
    }

    /**
     * 获取可靠队列所有可拉取消息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/reliable-queue/list-all?key=rq:test"
     *
     * @param key 队列 key
     * @return 消息集合
     */
    @GetMapping("/reliable-queue/list-all")
    public Map<String, Object> reliableQueueListAll(@RequestParam String key) {
        checkKey(key);

        List<Message<Object>> messages = redissonService.reliableQueueListAll(key);
        return ok(messages.stream().map(this::messageToMap).toList());
    }

    /**
     * 获取可靠队列统计信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/reliable-queue/info?key=rq:test"
     *
     * @param key 队列 key
     * @return 统计信息
     */
    @GetMapping("/reliable-queue/info")
    public Map<String, Object> reliableQueueInfo(@RequestParam String key) {
        checkKey(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("size", redissonService.reliableQueueSize(key));
        data.put("delayedSize", redissonService.reliableQueueDelayedSize(key));
        data.put("unacknowledgedSize", redissonService.reliableQueueUnacknowledgedSize(key));
        data.put("deadLetterSources", redissonService.reliableQueueDeadLetterSources(key));
        return ok(data);
    }

    /**
     * 清空可靠队列。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/reliable-queue/clear?key=rq:test"
     *
     * @param key 队列 key
     * @return 是否清空成功
     */
    @DeleteMapping("/reliable-queue/clear")
    public Map<String, Object> reliableQueueClear(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.reliableQueueClear(key);
        return ok(success);
    }

    /**
     * 禁用可靠队列操作。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/advanced-message/reliable-queue/operation/disable?key=rq:test&operation=ADD"
     *
     * @param key       队列 key
     * @param operation 队列操作
     * @return 执行结果
     */
    @PutMapping("/reliable-queue/operation/disable")
    public Map<String, Object> reliableQueueDisableOperation(@RequestParam String key,
                                                             @RequestParam QueueOperation operation) {
        checkKey(key);
        Assert.notNull(operation, "operation不能为空");

        redissonService.reliableQueueDisableOperation(key, operation);
        return ok(true);
    }

    /**
     * 启用可靠队列操作。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/advanced-message/reliable-queue/operation/enable?key=rq:test&operation=ADD"
     *
     * @param key       队列 key
     * @param operation 队列操作
     * @return 执行结果
     */
    @PutMapping("/reliable-queue/operation/enable")
    public Map<String, Object> reliableQueueEnableOperation(@RequestParam String key,
                                                            @RequestParam QueueOperation operation) {
        checkKey(key);
        Assert.notNull(operation, "operation不能为空");

        redissonService.reliableQueueEnableOperation(key, operation);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // Stream / 高级消费治理
    // -------------------------------------------------------------------------

    /**
     * 获取 Stream 详细信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/info?streamKey=stream:test"
     *
     * @param streamKey Stream key
     * @return Stream 信息
     */
    @GetMapping("/stream/info")
    public Map<String, Object> streamInfo(@RequestParam String streamKey) {
        checkKey(streamKey);

        StreamInfo<Object, Object> info = redissonService.streamInfo(streamKey);
        return ok(info);
    }

    /**
     * 获取 Stream 消费组列表。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/groups?streamKey=stream:test"
     *
     * @param streamKey Stream key
     * @return 消费组列表
     */
    @GetMapping("/stream/groups")
    public Map<String, Object> streamListGroups(@RequestParam String streamKey) {
        checkKey(streamKey);

        List<StreamGroup> groups = redissonService.streamListGroups(streamKey);
        return ok(groups);
    }

    /**
     * 获取 Stream 消费者列表。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/consumers?streamKey=stream:test&groupName=group:test"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 消费者列表
     */
    @GetMapping("/stream/consumers")
    public Map<String, Object> streamListConsumers(@RequestParam String streamKey,
                                                   @RequestParam String groupName) {
        checkKey(streamKey);
        checkKey(groupName);

        List<StreamConsumer> consumers = redissonService.streamListConsumers(streamKey, groupName);
        return ok(consumers);
    }

    /**
     * 创建 Stream 消费者。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/stream/consumer/create?streamKey=stream:test&groupName=group:test&consumerName=consumer:1"
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @return 执行结果
     */
    @PostMapping("/stream/consumer/create")
    public Map<String, Object> streamCreateConsumer(@RequestParam String streamKey,
                                                    @RequestParam String groupName,
                                                    @RequestParam String consumerName) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);

        redissonService.streamCreateConsumer(streamKey, groupName, consumerName);
        return ok(true);
    }

    /**
     * 删除 Stream 消费者。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/stream/consumer/remove?streamKey=stream:test&groupName=group:test&consumerName=consumer:1"
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @return 待处理消息数量
     */
    @DeleteMapping("/stream/consumer/remove")
    public Map<String, Object> streamRemoveConsumer(@RequestParam String streamKey,
                                                    @RequestParam String groupName,
                                                    @RequestParam String consumerName) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);

        long pendingCount = redissonService.streamRemoveConsumer(streamKey, groupName, consumerName);
        return ok(pendingCount);
    }

    /**
     * 删除 Stream 消费组。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/stream/group/remove?streamKey=stream:test&groupName=group:test"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 执行结果
     */
    @DeleteMapping("/stream/group/remove")
    public Map<String, Object> streamRemoveGroup(@RequestParam String streamKey,
                                                 @RequestParam String groupName) {
        checkKey(streamKey);
        checkKey(groupName);

        redissonService.streamRemoveGroup(streamKey, groupName);
        return ok(true);
    }

    /**
     * 更新 Stream 消费组读取起始 ID。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/advanced-message/stream/group/message-id?streamKey=stream:test&groupName=group:test&id=0-0"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param id        消息 ID
     * @return 执行结果
     */
    @PutMapping("/stream/group/message-id")
    public Map<String, Object> streamUpdateGroupMessageId(@RequestParam String streamKey,
                                                          @RequestParam String groupName,
                                                          @RequestParam String id) {
        checkKey(streamKey);
        checkKey(groupName);

        redissonService.streamUpdateGroupMessageId(streamKey, groupName, parseStreamMessageId(id));
        return ok(true);
    }

    /**
     * 获取 Stream 待处理消息概要。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/pending/info?streamKey=stream:test&groupName=group:test"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 待处理概要
     */
    @GetMapping("/stream/pending/info")
    public Map<String, Object> streamPendingInfo(@RequestParam String streamKey,
                                                 @RequestParam String groupName) {
        checkKey(streamKey);
        checkKey(groupName);

        PendingResult result = redissonService.streamPendingInfo(streamKey, groupName);
        return ok(result);
    }

    /**
     * 获取 Stream 待处理消息列表。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/pending/list?streamKey=stream:test&groupName=group:test&startId=0-0&endId=9999999999999-0&count=10"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 待处理消息列表
     */
    @GetMapping("/stream/pending/list")
    public Map<String, Object> streamListPending(@RequestParam String streamKey,
                                                 @RequestParam String groupName,
                                                 @RequestParam(defaultValue = "0-0") String startId,
                                                 @RequestParam(defaultValue = "9999999999999-0") String endId,
                                                 @RequestParam(defaultValue = "10") Integer count) {
        checkKey(streamKey);
        checkKey(groupName);

        List<PendingEntry> result = redissonService.streamListPending(
                streamKey,
                groupName,
                parseStreamMessageId(startId),
                parseStreamMessageId(endId),
                count
        );
        return ok(result);
    }

    /**
     * 按范围读取 Stream 消息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/range?streamKey=stream:test&startId=0-0&endId=9999999999999-0&count=10"
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 消息 Map
     */
    @GetMapping("/stream/range")
    public Map<String, Object> streamRange(@RequestParam String streamKey,
                                           @RequestParam(defaultValue = "0-0") String startId,
                                           @RequestParam(defaultValue = "9999999999999-0") String endId,
                                           @RequestParam(required = false) Integer count) {
        checkKey(streamKey);

        Map<StreamMessageId, Map<Object, Object>> data;
        if (ObjectUtil.isNotNull(count) && count > 0) {
            data = redissonService.streamRange(streamKey, parseStreamMessageId(startId), parseStreamMessageId(endId), count);
        } else {
            data = redissonService.streamRange(streamKey, parseStreamMessageId(startId), parseStreamMessageId(endId));
        }

        return ok(streamMapToResponse(data));
    }

    /**
     * 按范围倒序读取 Stream 消息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/range-reversed?streamKey=stream:test&startId=9999999999999-0&endId=0-0&count=10"
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 消息 Map
     */
    @GetMapping("/stream/range-reversed")
    public Map<String, Object> streamRangeReversed(@RequestParam String streamKey,
                                                   @RequestParam(defaultValue = "9999999999999-0") String startId,
                                                   @RequestParam(defaultValue = "0-0") String endId,
                                                   @RequestParam(required = false) Integer count) {
        checkKey(streamKey);

        Map<StreamMessageId, Map<Object, Object>> data;
        if (ObjectUtil.isNotNull(count) && count > 0) {
            data = redissonService.streamRangeReversed(streamKey, parseStreamMessageId(startId), parseStreamMessageId(endId), count);
        } else {
            data = redissonService.streamRangeReversed(streamKey, parseStreamMessageId(startId), parseStreamMessageId(endId));
        }

        return ok(streamMapToResponse(data));
    }

    /**
     * 转移待处理 Stream 消息所有权。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/stream/claim?streamKey=stream:test&groupName=group:test&consumerName=consumer:2&idleSeconds=60&ids=1714096800000-0"
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 新消费者
     * @param idleSeconds  最小空闲秒数
     * @param ids          消息 ID 集合
     * @return 转移后的消息
     */
    @PostMapping("/stream/claim")
    public Map<String, Object> streamClaim(@RequestParam String streamKey,
                                           @RequestParam String groupName,
                                           @RequestParam String consumerName,
                                           @RequestParam(defaultValue = "60") Long idleSeconds,
                                           @RequestParam List<String> ids) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        Assert.isTrue(CollUtil.isNotEmpty(ids), "ids不能为空");

        StreamMessageId[] messageIds = ids.stream()
                .map(this::parseStreamMessageId)
                .toArray(StreamMessageId[]::new);

        Map<StreamMessageId, Map<Object, Object>> data = redissonService.streamClaim(
                streamKey,
                groupName,
                consumerName,
                idleSeconds,
                TimeUnit.SECONDS,
                messageIds
        );

        return ok(streamMapToResponse(data));
    }

    /**
     * 自动转移待处理 Stream 消息所有权。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/stream/auto-claim?streamKey=stream:test&groupName=group:test&consumerName=consumer:2&idleSeconds=60&startId=0-0&count=10"
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 新消费者
     * @param idleSeconds  最小空闲秒数
     * @param startId      起始 ID
     * @param count        数量
     * @return 自动转移结果
     */
    @PostMapping("/stream/auto-claim")
    public Map<String, Object> streamAutoClaim(@RequestParam String streamKey,
                                               @RequestParam String groupName,
                                               @RequestParam String consumerName,
                                               @RequestParam(defaultValue = "60") Long idleSeconds,
                                               @RequestParam(defaultValue = "0-0") String startId,
                                               @RequestParam(defaultValue = "10") Integer count) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);

        AutoClaimResult<Object, Object> result = redissonService.streamAutoClaim(
                streamKey,
                groupName,
                consumerName,
                idleSeconds,
                TimeUnit.SECONDS,
                parseStreamMessageId(startId),
                count
        );

        return ok(result);
    }

    /**
     * 按最大长度裁剪 Stream。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/stream/trim/max-size?streamKey=stream:test&maxSize=1000"
     *
     * @param streamKey Stream key
     * @param maxSize   最大长度
     * @return 裁剪数量
     */
    @DeleteMapping("/stream/trim/max-size")
    public Map<String, Object> streamTrimMaxSize(@RequestParam String streamKey,
                                                 @RequestParam Integer maxSize) {
        checkKey(streamKey);
        Assert.isTrue(ObjectUtil.isNotNull(maxSize) && maxSize > 0, "maxSize必须大于0");

        long count = redissonService.streamTrim(streamKey, StreamTrimArgs.maxLen(maxSize).noLimit());
        return ok(count);
    }

    // -------------------------------------------------------------------------
    // Executor / Scheduler / RemoteService / LiveObject
    // -------------------------------------------------------------------------

    /**
     * 获取分布式执行器信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/executor/info?name=executor:test"
     *
     * @param name 执行器名称
     * @return 执行器信息
     */
    @GetMapping("/executor/info")
    public Map<String, Object> executorInfo(@RequestParam String name) {
        checkKey(name);

        RExecutorService executorService = redissonService.getExecutorService(name);
        RScheduledExecutorService scheduledExecutorService = redissonService.getScheduledExecutorService(name);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("executorClass", executorService.getClass().getName());
        data.put("scheduledExecutorClass", scheduledExecutorService.getClass().getName());
        data.put("shutdown", executorService.isShutdown());
        data.put("terminated", executorService.isTerminated());
        return ok(data);
    }

    /**
     * 关闭分布式执行器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/executor/shutdown?name=executor:test"
     *
     * @param name 执行器名称
     * @return 执行结果
     */
    @PostMapping("/executor/shutdown")
    public Map<String, Object> executorShutdown(@RequestParam String name) {
        checkKey(name);

        redissonService.executorShutdown(name);
        return ok(true);
    }

    /**
     * 获取远程服务对象信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/remote-service/info"
     *
     * @return 远程服务信息
     */
    @GetMapping("/remote-service/info")
    public Map<String, Object> remoteServiceInfo() {
        RRemoteService remoteService = redissonService.getRemoteService();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", remoteService.getClass().getName());
        return ok(data);
    }

    /**
     * 获取指定名称远程服务对象信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/remote-service/named-info?name=remote:test"
     *
     * @param name 服务名称
     * @return 远程服务信息
     */
    @GetMapping("/remote-service/named-info")
    public Map<String, Object> namedRemoteServiceInfo(@RequestParam String name) {
        checkKey(name);

        RRemoteService remoteService = redissonService.getRemoteService(name);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("className", remoteService.getClass().getName());
        return ok(data);
    }

    /**
     * 获取 LiveObject 服务对象信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/live-object/info"
     *
     * @return LiveObject 服务信息
     */
    @GetMapping("/live-object/info")
    public Map<String, Object> liveObjectInfo() {
        RLiveObjectService liveObjectService = redissonService.getLiveObjectService();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", liveObjectService.getClass().getName());
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // Object Listener / 对象监听入口
    // -------------------------------------------------------------------------

    /**
     * 添加 Bucket 删除事件监听器。
     * 注意：该接口只是本地测试监听器注册，服务重启后监听器会丢失。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/listener/bucket/delete?key=bucket:test"
     *
     * @param key Redis 键
     * @return 监听器 ID
     */
    @PostMapping("/listener/bucket/delete")
    public Map<String, Object> addBucketDeleteListener(@RequestParam String key) {
        checkKey(key);

        DeletedObjectListener listener = name -> log.info("监听到 Bucket 删除事件，key={}，name={}", key, name);
        int listenerId = redissonService.addBucketListener(key, listener);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("listenerId", listenerId);
        return ok(data);
    }

    /**
     * 移除 Bucket 监听器。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/listener/bucket?key=bucket:test&listenerId=1"
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     * @return 执行结果
     */
    @DeleteMapping("/listener/bucket")
    public Map<String, Object> removeBucketListener(@RequestParam String key,
                                                    @RequestParam Integer listenerId) {
        checkKey(key);
        Assert.notNull(listenerId, "listenerId不能为空");

        redissonService.removeBucketListener(key, listenerId);
        return ok(true);
    }

    /**
     * 添加 Queue 对象监听器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/listener/queue?key=queue:test"
     *
     * @param key Redis 键
     * @return 监听器 ID
     */
    @PostMapping("/listener/queue")
    public Map<String, Object> addQueueListener(@RequestParam String key) {
        checkKey(key);

        DeletedObjectListener listener = name -> log.info("监听到 Bucket 删除事件，key={}，name={}", key, name);
        int listenerId = redissonService.addQueueListener(key, listener);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("listenerId", listenerId);
        return ok(data);
    }

    /**
     * 移除 Queue 对象监听器。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/listener/queue?key=queue:test&listenerId=1"
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     * @return 执行结果
     */
    @DeleteMapping("/listener/queue")
    public Map<String, Object> removeQueueListener(@RequestParam String key,
                                                   @RequestParam Integer listenerId) {
        checkKey(key);
        Assert.notNull(listenerId, "listenerId不能为空");

        redissonService.removeQueueListener(key, listenerId);
        return ok(true);
    }

    /**
     * 转换可靠队列消息。
     *
     * @param message 可靠队列消息
     * @return Map
     */
    private Map<String, Object> messageToMap(Message<Object> message) {
        if (ObjectUtil.isNull(message)) {
            return null;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", message.getId());
        data.put("payload", message.getPayload());
        data.put("headers", message.getHeaders());
        return data;
    }

    /**
     * 解析 Stream 消息 ID。
     *
     * @param id ID 文本，格式为 0-0
     * @return StreamMessageId
     */
    private StreamMessageId parseStreamMessageId(String id) {
        Assert.isTrue(StrUtil.isNotBlank(id), "Stream消息ID不能为空");

        List<String> parts = StrUtil.split(id, "-");
        Assert.isTrue(parts.size() == 2, "Stream消息ID格式错误，正确格式如 0-0");

        long first = Long.parseLong(parts.get(0));
        long second = Long.parseLong(parts.get(1));
        return new StreamMessageId(first, second);
    }

    /**
     * Stream 消息转换为响应结构。
     *
     * @param streamMap Stream 消息 Map
     * @return 响应 Map
     */
    private Map<String, Object> streamMapToResponse(Map<StreamMessageId, Map<Object, Object>> streamMap) {
        if (CollUtil.isEmpty(streamMap)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        streamMap.forEach((id, body) -> result.put(id.toString(), body));
        return result;
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

    /**
     * 可靠队列添加请求体。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    @Data
    public static class ReliableQueueAddRequest {

        /**
         * 消息体。
         */
        private Object payload;

        /**
         * 延迟秒数。
         */
        private Long delaySeconds;

        /**
         * 消息 TTL 秒数。
         */
        private Long ttlSeconds;

        /**
         * 最大投递次数。
         */
        private Integer deliveryLimit;

        /**
         * 优先级。
         */
        private Integer priority;

        /**
         * 去重 ID。
         */
        private String deduplicationId;

    }

}
```

