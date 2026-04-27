# Redisson

Redisson 是一个基于 Redis 的 Java 客户端，提供了丰富的分布式数据结构和服务，如分布式锁、集合、队列、Map 等。它简化了与 Redis 的交互，并且支持高可用性、分布式事务、监控等特性，非常适合构建高性能和高可扩展性的应用。

- [官网链接](https://redisson.org)



## 基础配置

### 添加依赖

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

#### 单机配置

```yaml
---
# Redisson 的相关配置
redisson:
  config: |
    singleServerConfig:
      address: redis://192.168.1.12:40003
      password: Admin@123
      database: 0
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

```java
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
```

### 创建Service实现

```java
package local.ateng.java.redis.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.redis.service.RedissonService;
import org.redisson.api.*;
import org.redisson.api.geo.GeoSearchArgs;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.queue.*;
import org.redisson.api.queue.event.QueueEventListener;
import org.redisson.api.stream.*;
import org.redisson.client.protocol.ScoredEntry;
import org.redisson.codec.JsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Redis 服务实现类
 * 基于 Spring Boot 3 + Redisson 封装 Redis 常用能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Service
public class RedissonServiceImpl implements RedissonService {

    private static final Logger log = LoggerFactory.getLogger(RedissonServiceImpl.class);

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    public RedissonServiceImpl(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Redisson 原生对象访问
    // -------------------------------------------------------------------------

    /**
     * 获取 RedissonClient 实例。
     *
     * @return RedissonClient 实例
     */
    @Override
    public RedissonClient getClient() {
        return redissonClient;
    }

    /**
     * 获取对象桶。
     *
     * @param key Redis 键
     * @param <T> 值类型
     * @return RBucket
     */
    @Override
    public <T> RBucket<T> getBucket(String key) {
        checkKey(key);
        return redissonClient.getBucket(key);
    }

    /**
     * 获取哈希 Map。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RMap
     */
    @Override
    public <K, V> RMap<K, V> getMap(String key) {
        checkKey(key);
        return redissonClient.getMap(key);
    }

    /**
     * 获取带 TTL 能力的 MapCache。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RMapCache
     */
    @Override
    public <K, V> RMapCache<K, V> getMapCache(String key) {
        checkKey(key);
        return redissonClient.getMapCache(key);
    }

    /**
     * 获取列表。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RList
     */
    @Override
    public <T> RList<T> getList(String key) {
        checkKey(key);
        return redissonClient.getList(key);
    }

    /**
     * 获取双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RDeque
     */
    @Override
    public <T> RDeque<T> getDeque(String key) {
        checkKey(key);
        return redissonClient.getDeque(key);
    }

    /**
     * 获取集合。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RSet
     */
    @Override
    public <T> RSet<T> getSet(String key) {
        checkKey(key);
        return redissonClient.getSet(key);
    }

    /**
     * 获取带元素 TTL 能力的 SetCache。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RSetCache
     */
    @Override
    public <T> RSetCache<T> getSetCache(String key) {
        checkKey(key);
        return redissonClient.getSetCache(key);
    }

    /**
     * 获取有序集合。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RScoredSortedSet
     */
    @Override
    public <T> RScoredSortedSet<T> getScoredSortedSet(String key) {
        checkKey(key);
        return redissonClient.getScoredSortedSet(key);
    }

    /**
     * 获取 BitSet。
     *
     * @param key Redis 键
     * @return RBitSet
     */
    @Override
    public RBitSet getBitSet(String key) {
        checkKey(key);
        return redissonClient.getBitSet(key);
    }

    /**
     * 获取 HyperLogLog。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RHyperLogLog
     */
    @Override
    public <T> RHyperLogLog<T> getHyperLogLog(String key) {
        checkKey(key);
        return redissonClient.getHyperLogLog(key);
    }

    /**
     * 获取 Geo。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RGeo
     */
    @Override
    public <T> RGeo<T> getGeo(String key) {
        checkKey(key);
        return redissonClient.getGeo(key);
    }

    /**
     * 获取普通队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RQueue
     */
    @Override
    public <T> RQueue<T> getQueue(String key) {
        checkKey(key);
        return redissonClient.getQueue(key);
    }

    /**
     * 获取阻塞队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RBlockingQueue
     */
    @Override
    public <T> RBlockingQueue<T> getBlockingQueue(String key) {
        checkKey(key);
        return redissonClient.getBlockingQueue(key);
    }

    /**
     * 获取可靠队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RReliableQueue
     */
    @Override
    public <T> RReliableQueue<T> getReliableQueue(String key) {
        checkKey(key);
        return redissonClient.getReliableQueue(key);
    }

    /**
     * 获取消息主题。
     *
     * @param topic 主题名称
     * @return RTopic
     */
    @Override
    public RTopic getTopic(String topic) {
        checkKey(topic);
        return redissonClient.getTopic(topic);
    }

    /**
     * 获取 Stream。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RStream
     */
    @Override
    public <K, V> RStream<K, V> getStream(String key) {
        checkKey(key);
        return redissonClient.getStream(key);
    }

    /**
     * 获取脚本执行对象。
     *
     * @return RScript
     */
    @Override
    public RScript getScript() {
        return redissonClient.getScript();
    }

    // -------------------------------------------------------------------------
    // 通用 Key 管理
    // -------------------------------------------------------------------------

    /**
     * 判断指定 key 是否存在。
     *
     * @param key Redis 键
     * @return 存在返回 true
     */
    @Override
    public boolean hasKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return redissonClient.getKeys().countExists(key) > 0;
    }

    /**
     * 统计多个 key 中实际存在的数量。
     *
     * @param keys Redis 键集合
     * @return 存在数量
     */
    @Override
    public long countExists(String... keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return 0L;
        }
        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }
        return redissonClient.getKeys().countExists(keyArray);
    }

    /**
     * 删除指定 key。
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @Override
    public boolean deleteKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return redissonClient.getKeys().delete(key) > 0;
    }

    /**
     * 批量删除 key。
     *
     * @param keys Redis 键集合
     * @return 删除数量
     */
    @Override
    public long deleteKeys(Collection<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return 0L;
        }
        String[] keyArray = keys.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }
        return redissonClient.getKeys().delete(keyArray);
    }

    /**
     * 根据通配符删除 key。
     *
     * @param pattern 通配符表达式
     * @return 删除数量
     */
    @Override
    public long deleteByPattern(String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return 0L;
        }
        return redissonClient.getKeys().deleteByPattern(pattern);
    }

    /**
     * 设置 key 过期时间。
     *
     * @param key     Redis 键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout > 0, "过期时间必须大于 0");
        return redissonClient.getBucket(key).expire(timeout, unit);
    }

    /**
     * 设置 key 过期时间。
     *
     * @param key Redis 键
     * @param ttl 过期时间
     * @return 是否设置成功
     */
    @Override
    public boolean expire(String key, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "过期时间不能为空且必须大于 0");
        return redissonClient.getBucket(key).expire(ttl);
    }

    /**
     * 获取 key 剩余过期时间。
     *
     * @param key  Redis 键
     * @param unit 时间单位
     * @return 剩余时间，-1 表示永久，-2 表示不存在
     */
    @Override
    public long getTtl(String key, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");

        long ttlMillis = redissonClient.getBucket(key).remainTimeToLive();
        if (ttlMillis < 0) {
            return ttlMillis;
        }
        return unit.convert(ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 移除 key 过期时间。
     *
     * @param key Redis 键
     * @return 是否成功
     */
    @Override
    public boolean persist(String key) {
        checkKey(key);
        return redissonClient.getBucket(key).clearExpire();
    }

    /**
     * 修改 key 名称。
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return 是否成功
     */
    @Override
    public boolean renameKey(String oldKey, String newKey) {
        checkKey(oldKey);
        checkKey(newKey);

        try {
            redissonClient.getKeys().rename(oldKey, newKey);
            return true;
        } catch (Exception e) {
            log.warn("重命名 Redis Key 失败，oldKey={}，newKey={}", oldKey, newKey, e);
            return false;
        }
    }

    /**
     * 新 key 不存在时修改 key 名称。
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return 是否成功
     */
    @Override
    public boolean renameKeyIfAbsent(String oldKey, String newKey) {
        checkKey(oldKey);
        checkKey(newKey);
        return redissonClient.getKeys().renamenx(oldKey, newKey);
    }

    /**
     * 查询匹配通配符的 key。
     *
     * @param pattern 通配符表达式
     * @return key 集合
     */
    @Override
    public Set<String> keys(String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return Collections.emptySet();
        }

        Set<String> result = new LinkedHashSet<>();
        Iterable<String> iterable = redissonClient.getKeys().getKeysByPattern(pattern);
        for (String key : iterable) {
            result.add(key);
        }
        return result;
    }

    /**
     * 查询匹配通配符的 key，并限制返回数量。
     *
     * @param pattern 通配符表达式
     * @param count   最大数量
     * @return key 集合
     */
    @Override
    public Set<String> scanKeys(String pattern, int count) {
        if (StrUtil.isBlank(pattern) || count <= 0) {
            return Collections.emptySet();
        }

        Set<String> result = new LinkedHashSet<>();
        Iterable<String> iterable = redissonClient.getKeys().getKeysByPattern(pattern);
        for (String key : iterable) {
            result.add(key);
            if (result.size() >= count) {
                break;
            }
        }
        return result;
    }

    /**
     * 判断 key 是否已过期或不存在。
     *
     * @param key Redis 键
     * @return 已过期或不存在返回 true
     */
    @Override
    public boolean isExpired(String key) {
        if (StrUtil.isBlank(key)) {
            return true;
        }

        long ttlMillis = redissonClient.getBucket(key).remainTimeToLive();
        return ttlMillis == -2 || ttlMillis == 0;
    }

    /**
     * 获取 key 类型。
     *
     * @param key Redis 键
     * @return 类型名称
     */
    @Override
    public String getKeyType(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        RType type = redissonClient.getKeys().getType(key);
        return ObjectUtil.isNull(type) ? null : type.name().toLowerCase();
    }

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
    @Override
    public <T> T convertValue(Object value, Class<T> clazz) {
        // 原始值或目标类型为空时，无法进行有效转换，直接返回 null
        if (ObjectUtil.isNull(value) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        // 如果原始值本身已经是目标类型，直接强转返回，避免重复序列化和反序列化
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        try {
            // Redis 中常见的值可能是 JSON 字符串，这里优先按 JSON 字符串处理
            if (value instanceof String text) {
                // 目标类型就是 String 时，直接返回原字符串
                if (String.class.equals(clazz)) {
                    return clazz.cast(text);
                }

                // 目标类型不是 String 时，尝试将字符串按 JSON 反序列化为目标类型
                return objectMapper.readValue(text, clazz);
            }

            // 非字符串对象使用 Jackson 的 convertValue 进行类型转换
            // 适用于 LinkedHashMap -> JavaBean、Map -> DTO、基础类型转换等场景
            return objectMapper.convertValue(value, clazz);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            // 转换失败时不向外抛出异常，避免缓存值格式异常影响主流程
            // 这里记录目标类型和原始值类型，便于排查 Redis 中的数据结构问题
            log.warn("Redis 值类型转换失败，targetType={}，valueType={}",
                    clazz.getName(), value.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 将对象转换为指定泛型类型。
     *
     * @param value         原始值
     * @param typeReference 目标类型引用
     * @param <T>           泛型类型
     * @return 转换后的值
     */
    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeReference) {
        // 原始值或泛型类型引用为空时，无法进行有效转换，直接返回 null
        if (ObjectUtil.isNull(value) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        try {
            // Redis 中存储的泛型数据可能是 JSON 字符串，例如 List<User>、Map<String, User>
            if (value instanceof String text) {
                try {
                    // 优先将字符串按 JSON 反序列化为指定泛型类型
                    return objectMapper.readValue(text, typeReference);
                } catch (JsonProcessingException ignored) {
                    // 如果字符串不是合法 JSON，则退回到 convertValue
                    // 例如目标泛型实际可以接收 String、Object 等简单类型时，仍有机会转换成功
                    return objectMapper.convertValue(value, typeReference);
                }
            }

            // 非字符串对象使用 Jackson 的 convertValue 进行泛型转换
            // 适用于 LinkedHashMap -> 泛型 DTO、List<LinkedHashMap> -> List<DTO> 等场景
            return objectMapper.convertValue(value, typeReference);
        } catch (IllegalArgumentException e) {
            // 泛型转换失败通常是结构不匹配，例如字段类型不兼容、集合元素类型不一致等
            // 记录目标泛型类型和原始值类型，便于定位缓存数据问题
            log.warn("Redis 值泛型转换失败，targetType={}，valueType={}",
                    typeReference.getType(), value.getClass().getName(), e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // 字符串 / Bucket 操作
    // -------------------------------------------------------------------------

    /**
     * 设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     */
    @Override
    public void set(String key, Object value) {
        checkKey(key);
        redissonClient.getBucket(key).set(value);
    }

    /**
     * 设置缓存值并指定过期时间。
     *
     * @param key     Redis 键
     * @param value   缓存值
     * @param timeout 超时时间
     * @param unit    时间单位
     */
    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout > 0, "过期时间必须大于 0");

        redissonClient.getBucket(key).set(value, timeout, unit);
    }

    /**
     * 设置缓存值并指定过期时间。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     */
    @Override
    public void set(String key, Object value, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "过期时间不能为空且必须大于 0");

        redissonClient.getBucket(key).set(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 获取缓存值。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 缓存值
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        Object value = redissonClient.getBucket(key).get();
        return convertValue(value, clazz);
    }

    /**
     * 获取缓存值。
     *
     * @param key           Redis 键
     * @param typeReference 目标泛型类型
     * @param <T>           泛型类型
     * @return 缓存值
     */
    @Override
    public <T> T get(String key, TypeReference<T> typeReference) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        Object value = redissonClient.getBucket(key).get();
        return convertValue(value, typeReference);
    }

    /**
     * key 不存在时设置缓存值。
     *
     * @param key     Redis 键
     * @param value   缓存值
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    @Override
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout > 0, "过期时间必须大于 0");

        return redissonClient.getBucket(key).setIfAbsent(value, Duration.ofMillis(unit.toMillis(timeout)));
    }

    /**
     * key 不存在时设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     * @return 是否设置成功
     */
    @Override
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "过期时间不能为空且必须大于 0");

        return redissonClient.getBucket(key).setIfAbsent(value, ttl);
    }

    /**
     * key 存在时设置缓存值。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @return 是否设置成功
     */
    @Override
    public boolean setIfExists(String key, Object value) {
        checkKey(key);
        return redissonClient.getBucket(key).setIfExists(value);
    }

    /**
     * key 存在时设置缓存值并指定过期时间。
     *
     * @param key   Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     * @return 是否设置成功
     */
    @Override
    public boolean setIfExists(String key, Object value, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "过期时间不能为空且必须大于 0");

        return redissonClient.getBucket(key).setIfExists(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 原子替换并返回旧值。
     *
     * @param key   Redis 键
     * @param value 新值
     * @param clazz 旧值类型
     * @param <T>   泛型类型
     * @return 旧值
     */
    @Override
    public <T> T getAndSet(String key, Object value, Class<T> clazz) {
        checkKey(key);

        Object oldValue = redissonClient.getBucket(key).getAndSet(value);
        return convertValue(oldValue, clazz);
    }

    /**
     * 原子替换并返回旧值。
     *
     * @param key           Redis 键
     * @param value         新值
     * @param typeReference 旧值类型
     * @param <T>           泛型类型
     * @return 旧值
     */
    @Override
    public <T> T getAndSet(String key, Object value, TypeReference<T> typeReference) {
        checkKey(key);

        Object oldValue = redissonClient.getBucket(key).getAndSet(value);
        return convertValue(oldValue, typeReference);
    }

    /**
     * 获取并删除缓存值。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 删除前的值
     */
    @Override
    public <T> T getAndDelete(String key, Class<T> clazz) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        Object value = redissonClient.getBucket(key).getAndDelete();
        return convertValue(value, clazz);
    }

    /**
     * 批量获取缓存值。
     *
     * @param keys Redis 键集合
     * @return 键值 Map
     */
    @Override
    public Map<String, Object> entries(Collection<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyMap();
        }

        String[] keyArray = keys.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(keyArray)) {
            return Collections.emptyMap();
        }

        return redissonClient.getBuckets().get(keyArray);
    }

    /**
     * 批量获取缓存值并转换类型。
     *
     * @param keys  Redis 键集合
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 键值 Map
     */
    @Override
    public <T> Map<String, T> entries(Collection<String> keys, Class<T> clazz) {
        if (CollUtil.isEmpty(keys) || ObjectUtil.isNull(clazz)) {
            return Collections.emptyMap();
        }

        Map<String, Object> rawMap = entries(keys);
        if (CollUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        rawMap.forEach((key, value) -> {
            T converted = convertValue(value, clazz);
            if (ObjectUtil.isNotNull(converted)) {
                result.put(key, converted);
            }
        });
        return result;
    }

    /**
     * 批量获取缓存值并转换泛型类型。
     *
     * @param keys          Redis 键集合
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 键值 Map
     */
    @Override
    public <T> Map<String, T> entries(Collection<String> keys, TypeReference<T> typeReference) {
        if (CollUtil.isEmpty(keys) || ObjectUtil.isNull(typeReference)) {
            return Collections.emptyMap();
        }

        Map<String, Object> rawMap = entries(keys);
        if (CollUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        rawMap.forEach((key, value) -> {
            T converted = convertValue(value, typeReference);
            if (ObjectUtil.isNotNull(converted)) {
                result.put(key, converted);
            }
        });
        return result;
    }

    /**
     * 获取序列化后的字节大小。
     *
     * @param key Redis 键
     * @return 字节大小
     */
    @Override
    public long size(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }
        return redissonClient.getBucket(key).size();
    }

    // -------------------------------------------------------------------------
    // 原子数值 / 计数器 / ID
    // -------------------------------------------------------------------------

    /**
     * 获取长整型原子对象。
     *
     * @param key Redis 键
     * @return RAtomicLong
     */
    @Override
    public RAtomicLong getAtomicLong(String key) {
        checkKey(key);
        return redissonClient.getAtomicLong(key);
    }

    /**
     * 获取浮点型原子对象。
     *
     * @param key Redis 键
     * @return RAtomicDouble
     */
    @Override
    public RAtomicDouble getAtomicDouble(String key) {
        checkKey(key);
        return redissonClient.getAtomicDouble(key);
    }

    /**
     * 整数自增。
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    @Override
    public long increment(String key, long delta) {
        checkKey(key);
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    /**
     * 整数自减。
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    @Override
    public long decrement(String key, long delta) {
        checkKey(key);
        Assert.isTrue(delta >= 0, "递减值不能小于 0");

        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    /**
     * 浮点数自增。
     *
     * @param key   Redis 键
     * @param delta 增量
     * @return 最新值
     */
    @Override
    public double incrementDouble(String key, double delta) {
        checkKey(key);
        return redissonClient.getAtomicDouble(key).addAndGet(delta);
    }

    /**
     * 浮点数自减。
     *
     * @param key   Redis 键
     * @param delta 减量
     * @return 最新值
     */
    @Override
    public double decrementDouble(String key, double delta) {
        checkKey(key);
        Assert.isTrue(delta >= 0, "递减值不能小于 0");

        return redissonClient.getAtomicDouble(key).addAndGet(-delta);
    }

    /**
     * 设置整数计数器值。
     *
     * @param key   Redis 键
     * @param value 值
     */
    @Override
    public void setAtomicLong(String key, long value) {
        checkKey(key);
        redissonClient.getAtomicLong(key).set(value);
    }

    /**
     * 获取整数计数器值。
     *
     * @param key Redis 键
     * @return 当前值
     */
    @Override
    public long getAtomicLongValue(String key) {
        checkKey(key);
        return redissonClient.getAtomicLong(key).get();
    }

    /**
     * 重置整数计数器。
     *
     * @param key Redis 键
     */
    @Override
    public void resetAtomicLong(String key) {
        checkKey(key);

        redissonClient.getAtomicLong(key).set(0L);
        log.info("Redis 计数器已重置，key={}", key);
    }

    /**
     * 获取分布式 ID 生成器。
     *
     * @param key Redis 键
     * @return RIdGenerator
     */
    @Override
    public RIdGenerator getIdGenerator(String key) {
        checkKey(key);
        return redissonClient.getIdGenerator(key);
    }

    /**
     * 初始化分布式 ID 生成器。
     *
     * @param key            Redis 键
     * @param initialValue   初始值
     * @param allocationSize 每次分配步长
     * @return 是否初始化成功
     */
    @Override
    public boolean idGeneratorInit(String key, long initialValue, long allocationSize) {
        checkKey(key);
        Assert.isTrue(allocationSize > 0, "ID 分配步长必须大于 0");

        boolean initialized = redissonClient.getIdGenerator(key).tryInit(initialValue, allocationSize);
        if (initialized) {
            log.info("Redis 分布式 ID 生成器初始化成功，key={}，initialValue={}，allocationSize={}",
                    key, initialValue, allocationSize);
        } else {
            log.debug("Redis 分布式 ID 生成器已存在，跳过初始化，key={}", key);
        }
        return initialized;
    }

    /**
     * 获取下一个分布式 ID。
     *
     * @param key Redis 键
     * @return ID
     */
    @Override
    public long nextId(String key) {
        checkKey(key);
        return redissonClient.getIdGenerator(key).nextId();
    }

    // -------------------------------------------------------------------------
    // Hash / MapCache 操作
    // -------------------------------------------------------------------------

    /**
     * 设置哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     */
    @Override
    public void hPut(String key, String field, Object value) {
        checkKey(key);
        checkField(field);

        redissonClient.<String, Object>getMap(key).fastPut(field, value);
    }

    /**
     * 批量设置哈希字段值。
     *
     * @param key Redis 键
     * @param map 字段 Map
     */
    @Override
    public void hPutAll(String key, Map<String, ?> map) {
        checkKey(key);
        if (CollUtil.isEmpty(map)) {
            return;
        }

        Map<String, Object> valueMap = new LinkedHashMap<>(map.size());
        map.forEach((field, value) -> {
            if (StrUtil.isNotBlank(field)) {
                valueMap.put(field, value);
            }
        });

        if (CollUtil.isNotEmpty(valueMap)) {
            redissonClient.<String, Object>getMap(key).putAll(valueMap);
        }
    }

    /**
     * 字段不存在时设置哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @return 是否设置成功
     */
    @Override
    public boolean hPutIfAbsent(String key, String field, Object value) {
        checkKey(key);
        checkField(field);

        return redissonClient.<String, Object>getMap(key).fastPutIfAbsent(field, value);
    }

    /**
     * 获取哈希字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值
     */
    @Override
    public <T> T hGet(String key, String field, Class<T> clazz) {
        if (StrUtil.hasBlank(key, field) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        Object value = redissonClient.<String, Object>getMap(key).get(field);
        return convertValue(value, clazz);
    }

    /**
     * 获取哈希字段值。
     *
     * @param key           Redis 键
     * @param field         字段名
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 字段值
     */
    @Override
    public <T> T hGet(String key, String field, TypeReference<T> typeReference) {
        if (StrUtil.hasBlank(key, field) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        Object value = redissonClient.<String, Object>getMap(key).get(field);
        return convertValue(value, typeReference);
    }

    /**
     * 批量获取哈希字段值。
     *
     * @param key    Redis 键
     * @param fields 字段集合
     * @return 字段值 Map
     */
    @Override
    public Map<String, Object> hMultiGet(String key, Collection<String> fields) {
        if (StrUtil.isBlank(key) || CollUtil.isEmpty(fields)) {
            return Collections.emptyMap();
        }

        Set<String> fieldSet = fields.stream()
                .filter(StrUtil::isNotBlank)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (CollUtil.isEmpty(fieldSet)) {
            return Collections.emptyMap();
        }

        return redissonClient.<String, Object>getMap(key).getAll(fieldSet);
    }

    /**
     * 删除哈希字段。
     *
     * @param key    Redis 键
     * @param fields 字段名
     * @return 删除数量
     */
    @Override
    public long hDelete(String key, String... fields) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(fields)) {
            return 0L;
        }

        String[] fieldArray = Arrays.stream(fields)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(fieldArray)) {
            return 0L;
        }

        return redissonClient.<String, Object>getMap(key).fastRemove(fieldArray);
    }

    /**
     * 判断哈希字段是否存在。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @return 存在返回 true
     */
    @Override
    public boolean hHasKey(String key, String field) {
        if (StrUtil.hasBlank(key, field)) {
            return false;
        }

        return redissonClient.<String, Object>getMap(key).containsKey(field);
    }

    /**
     * 获取哈希全部字段和值。
     *
     * @param key Redis 键
     * @return 字段值 Map
     */
    @Override
    public Map<String, Object> hEntries(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        return redissonClient.<String, Object>getMap(key).readAllMap();
    }

    /**
     * 获取哈希全部字段和值并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值 Map
     */
    @Override
    public <T> Map<String, T> hEntries(String key, Class<T> clazz) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(clazz)) {
            return Collections.emptyMap();
        }

        Map<String, Object> rawMap = hEntries(key);
        if (CollUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        rawMap.forEach((field, value) -> {
            T converted = convertValue(value, clazz);
            if (ObjectUtil.isNotNull(converted)) {
                result.put(field, converted);
            }
        });
        return result;
    }

    /**
     * 获取哈希全部字段和值并转换泛型类型。
     *
     * @param key           Redis 键
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 字段值 Map
     */
    @Override
    public <T> Map<String, T> hEntries(String key, TypeReference<T> typeReference) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(typeReference)) {
            return Collections.emptyMap();
        }

        Map<String, Object> rawMap = hEntries(key);
        if (CollUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }

        Map<String, T> result = new LinkedHashMap<>(rawMap.size());
        rawMap.forEach((field, value) -> {
            T converted = convertValue(value, typeReference);
            if (ObjectUtil.isNotNull(converted)) {
                result.put(field, converted);
            }
        });
        return result;
    }

    /**
     * 获取哈希字段名集合。
     *
     * @param key Redis 键
     * @return 字段集合
     */
    @Override
    public Set<String> hKeys(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        return redissonClient.<String, Object>getMap(key).readAllKeySet();
    }

    /**
     * 获取哈希字段值集合。
     *
     * @param key Redis 键
     * @return 字段值集合
     */
    @Override
    public Collection<Object> hValues(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyList();
        }

        return redissonClient.<String, Object>getMap(key).readAllValues();
    }

    /**
     * 获取哈希字段数量。
     *
     * @param key Redis 键
     * @return 字段数量
     */
    @Override
    public int hSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.<String, Object>getMap(key).size();
    }

    /**
     * 哈希字段整数自增。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    @Override
    public long hIncrement(String key, String field, long delta) {
        checkKey(key);
        checkField(field);

        Number value = redissonClient.<String, Number>getMap(key).addAndGet(field, delta);
        return ObjectUtil.isNull(value) ? 0L : value.longValue();
    }

    /**
     * 哈希字段浮点数自增。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param delta 增量
     * @return 最新值
     */
    @Override
    public double hIncrementDouble(String key, String field, double delta) {
        checkKey(key);
        checkField(field);

        Number value = redissonClient.<String, Number>getMap(key).addAndGet(field, delta);
        return ObjectUtil.isNull(value) ? 0D : value.doubleValue();
    }

    /**
     * 清空哈希。
     *
     * @param key Redis 键
     */
    @Override
    public void hClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.<String, Object>getMap(key).clear();
    }

    /**
     * 设置 MapCache 字段值并指定字段级 TTL。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param value 字段值
     * @param ttl   字段 TTL
     */
    @Override
    public void hcPut(String key, String field, Object value, Duration ttl) {
        checkKey(key);
        checkField(field);
        checkPositiveDuration(ttl, "字段过期时间不能为空且必须大于 0");

        redissonClient.<String, Object>getMapCache(key)
                .fastPut(field, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 设置 MapCache 字段值并指定字段级 TTL 与最大空闲时间。
     *
     * @param key     Redis 键
     * @param field   字段名
     * @param value   字段值
     * @param ttl     字段 TTL
     * @param maxIdle 最大空闲时间
     */
    @Override
    public void hcPut(String key, String field, Object value, Duration ttl, Duration maxIdle) {
        checkKey(key);
        checkField(field);
        checkPositiveDuration(ttl, "字段过期时间不能为空且必须大于 0");
        checkPositiveDuration(maxIdle, "字段最大空闲时间不能为空且必须大于 0");

        redissonClient.<String, Object>getMapCache(key)
                .fastPut(
                        field,
                        value,
                        ttl.toMillis(),
                        TimeUnit.MILLISECONDS,
                        maxIdle.toMillis(),
                        TimeUnit.MILLISECONDS
                );
    }

    /**
     * 获取 MapCache 字段值。
     *
     * @param key   Redis 键
     * @param field 字段名
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 字段值
     */
    @Override
    public <T> T hcGet(String key, String field, Class<T> clazz) {
        if (StrUtil.hasBlank(key, field) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        Object value = redissonClient.<String, Object>getMapCache(key).get(field);
        return convertValue(value, clazz);
    }

    /**
     * 删除 MapCache 字段。
     *
     * @param key    Redis 键
     * @param fields 字段名
     * @return 删除数量
     */
    @Override
    public long hcDelete(String key, String... fields) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(fields)) {
            return 0L;
        }

        String[] fieldArray = Arrays.stream(fields)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(fieldArray)) {
            return 0L;
        }

        return redissonClient.<String, Object>getMapCache(key).fastRemove(fieldArray);
    }

    // -------------------------------------------------------------------------
    // List / Deque 操作
    // -------------------------------------------------------------------------

    /**
     * 左侧压入列表。
     *
     * @param key   Redis 键
     * @param value 元素
     */
    @Override
    public void lLeftPush(String key, Object value) {
        checkKey(key);

        redissonClient.<Object>getDeque(key).addFirst(value);
    }

    /**
     * 右侧压入列表。
     *
     * @param key   Redis 键
     * @param value 元素
     */
    @Override
    public void lRightPush(String key, Object value) {
        checkKey(key);

        redissonClient.<Object>getDeque(key).addLast(value);
    }

    /**
     * 批量右侧压入列表。
     *
     * @param key    Redis 键
     * @param values 元素集合
     */
    @Override
    public void lRightPushAll(String key, Collection<?> values) {
        checkKey(key);
        if (CollUtil.isEmpty(values)) {
            return;
        }

        redissonClient.<Object>getList(key).addAll(values);
    }

    /**
     * 左侧弹出列表元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    @Override
    public Object lLeftPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<Object>getDeque(key).pollFirst();
    }

    /**
     * 左侧弹出列表元素并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素
     */
    @Override
    public <T> T lLeftPop(String key, Class<T> clazz) {
        Object value = lLeftPop(key);
        return convertValue(value, clazz);
    }

    /**
     * 右侧弹出列表元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    @Override
    public Object lRightPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<Object>getDeque(key).pollLast();
    }

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
    @Override
    public <T> T lLeftPop(String key, long timeout, TimeUnit unit, Class<T> clazz) throws InterruptedException {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        Object value = redissonClient.<Object>getBlockingDeque(key).pollFirst(timeout, unit);
        return convertValue(value, clazz);
    }

    /**
     * 获取列表范围。
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素集合
     */
    @Override
    public List<Object> lRange(String key, long start, long end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyList();
        }

        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        if (size <= 0) {
            return Collections.emptyList();
        }

        int fromIndex = normalizeIndex(start, size);
        int toIndex = normalizeIndex(end, size);

        fromIndex = Math.max(fromIndex, 0);
        toIndex = Math.min(toIndex, size - 1);

        if (fromIndex > toIndex) {
            return Collections.emptyList();
        }

        return new ArrayList<>(list.subList(fromIndex, toIndex + 1));
    }

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
    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        if (ObjectUtil.isNull(clazz)) {
            return Collections.emptyList();
        }

        List<Object> rawList = lRange(key, start, end);
        if (CollUtil.isEmpty(rawList)) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            T converted = convertValue(item, clazz);
            if (ObjectUtil.isNotNull(converted)) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * 获取列表长度。
     *
     * @param key Redis 键
     * @return 长度
     */
    @Override
    public long lSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getList(key).size();
    }

    /**
     * 删除列表元素。
     *
     * @param key   Redis 键
     * @param count 删除数量规则
     * @param value 元素
     * @return 删除数量
     */
    @Override
    public long lRemove(String key, long count, Object value) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        RList<Object> list = redissonClient.getList(key);
        if (list.isEmpty()) {
            return 0L;
        }

        if (count == 0) {
            long removed = 0L;
            while (list.remove(value)) {
                removed++;
            }
            return removed;
        }

        List<Object> copy = new ArrayList<>(list);
        long target = Math.abs(count);
        long removed = 0L;

        if (count > 0) {
            for (int i = 0; i < copy.size() && removed < target; i++) {
                if (ObjectUtil.equal(copy.get(i), value)) {
                    copy.remove(i--);
                    removed++;
                }
            }
        } else {
            for (int i = copy.size() - 1; i >= 0 && removed < target; i--) {
                if (ObjectUtil.equal(copy.get(i), value)) {
                    copy.remove(i);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            list.clear();
            list.addAll(copy);
        }

        return removed;
    }

    /**
     * 获取列表指定索引元素。
     *
     * @param key   Redis 键
     * @param index 索引
     * @return 元素
     */
    @Override
    public Object lIndex(String key, long index) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        if (size <= 0) {
            return null;
        }

        int realIndex = normalizeIndex(index, size);
        if (realIndex < 0 || realIndex >= size) {
            return null;
        }

        return list.get(realIndex);
    }

    /**
     * 获取列表指定索引元素并转换类型。
     *
     * @param key   Redis 键
     * @param index 索引
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素
     */
    @Override
    public <T> T lIndex(String key, long index, Class<T> clazz) {
        Object value = lIndex(key, index);
        return convertValue(value, clazz);
    }

    /**
     * 设置列表指定索引元素。
     *
     * @param key   Redis 键
     * @param index 索引
     * @param value 元素
     */
    @Override
    public void lSet(String key, long index, Object value) {
        checkKey(key);

        RList<Object> list = redissonClient.getList(key);
        int size = list.size();
        int realIndex = normalizeIndex(index, size);

        Assert.isTrue(realIndex >= 0 && realIndex < size, "列表索引超出范围");
        list.set(realIndex, value);
    }

    /**
     * 裁剪列表范围。
     *
     * @param key   Redis 键
     * @param start 开始索引
     * @param end   结束索引
     */
    @Override
    public void lTrim(String key, int start, int end) {
        checkKey(key);

        List<Object> range = lRange(key, start, end);
        RList<Object> list = redissonClient.getList(key);

        list.clear();
        if (CollUtil.isNotEmpty(range)) {
            list.addAll(range);
        }
    }

    /**
     * 清空列表。
     *
     * @param key Redis 键
     */
    @Override
    public void lClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.<Object>getList(key).clear();
    }

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
    @Override
    public boolean sAdd(String key, Object... values) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getSet(key).addAll(Arrays.asList(values));
    }

    /**
     * 添加集合元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @Override
    public boolean sAdd(String key, Collection<?> values) {
        if (StrUtil.isBlank(key) || CollUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getSet(key).addAll(values);
    }

    /**
     * 添加 SetCache 元素并指定元素 TTL。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param ttl   TTL
     * @return 是否添加成功
     */
    @Override
    public boolean scAdd(String key, Object value, Duration ttl) {
        checkKey(key);
        checkPositiveDuration(ttl, "元素过期时间不能为空且必须大于 0");

        return redissonClient.<Object>getSetCache(key).add(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 判断集合中是否存在元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 存在返回 true
     */
    @Override
    public boolean sIsMember(String key, Object value) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.<Object>getSet(key).contains(value);
    }

    /**
     * 获取集合所有元素。
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @Override
    public Set<Object> sMembers(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key).readAll();
    }

    /**
     * 获取集合所有元素并转换类型。
     *
     * @param key   Redis 键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 元素集合
     */
    @Override
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        if (ObjectUtil.isNull(clazz)) {
            return Collections.emptySet();
        }

        return convertSet(sMembers(key), clazz);
    }

    /**
     * 获取集合大小。
     *
     * @param key Redis 键
     * @return 大小
     */
    @Override
    public long sSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getSet(key).size();
    }

    /**
     * 随机弹出集合元素。
     *
     * @param key Redis 键
     * @return 元素
     */
    @Override
    public Object sPop(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<Object>getSet(key).removeRandom();
    }

    /**
     * 删除集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    @Override
    public boolean sRemove(String key, Object... values) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getSet(key).removeAll(Arrays.asList(values));
    }

    /**
     * 随机获取集合元素但不删除。
     *
     * @param key Redis 键
     * @return 元素
     */
    @Override
    public Object sRandomMember(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<Object>getSet(key).random();
    }

    /**
     * 随机获取多个集合元素。
     *
     * @param key   Redis 键
     * @param count 数量
     * @return 元素集合
     */
    @Override
    public Set<Object> sRandomMembers(String key, int count) {
        if (StrUtil.isBlank(key) || count <= 0) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key).random(count);
    }

    /**
     * 获取并集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 并集
     */
    @Override
    public Set<Object> sUnion(String key1, String key2) {
        if (StrUtil.hasBlank(key1, key2)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key1).readUnion(key2);
    }

    /**
     * 获取交集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 交集
     */
    @Override
    public Set<Object> sIntersect(String key1, String key2) {
        if (StrUtil.hasBlank(key1, key2)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key1).readIntersection(key2);
    }

    /**
     * 获取差集。
     *
     * @param key1 第一个 key
     * @param key2 第二个 key
     * @return 差集
     */
    @Override
    public Set<Object> sDifference(String key1, String key2) {
        if (StrUtil.hasBlank(key1, key2)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object>getSet(key1).readDiff(key2);
    }

    /**
     * 将集合并集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    @Override
    public long sUnionStore(String destKey, String... keys) {
        checkKey(destKey);

        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }

        Set<Object> result = redissonClient.<Object>getSet(keyArray[0])
                .readUnion(Arrays.copyOfRange(keyArray, 1, keyArray.length));

        RSet<Object> destSet = redissonClient.getSet(destKey);
        destSet.clear();
        if (CollUtil.isNotEmpty(result)) {
            destSet.addAll(result);
        }

        return result.size();
    }

    /**
     * 将集合交集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    @Override
    public long sIntersectStore(String destKey, String... keys) {
        checkKey(destKey);

        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }

        Set<Object> result = redissonClient.<Object>getSet(keyArray[0])
                .readIntersection(Arrays.copyOfRange(keyArray, 1, keyArray.length));

        RSet<Object> destSet = redissonClient.getSet(destKey);
        destSet.clear();
        if (CollUtil.isNotEmpty(result)) {
            destSet.addAll(result);
        }

        return result.size();
    }

    /**
     * 将集合差集存储到目标 key。
     *
     * @param destKey 目标 key
     * @param keys    源 key 集合
     * @return 存储数量
     */
    @Override
    public long sDifferenceStore(String destKey, String... keys) {
        checkKey(destKey);

        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return 0L;
        }

        Set<Object> result = redissonClient.<Object>getSet(keyArray[0])
                .readDiff(Arrays.copyOfRange(keyArray, 1, keyArray.length));

        RSet<Object> destSet = redissonClient.getSet(destKey);
        destSet.clear();
        if (CollUtil.isNotEmpty(result)) {
            destSet.addAll(result);
        }

        return result.size();
    }

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
    @Override
    public boolean zAdd(String key, Object value, double score) {
        checkKey(key);

        return redissonClient.<Object>getScoredSortedSet(key).add(score, value);
    }

    /**
     * 批量添加有序集合元素。
     *
     * @param key      Redis 键
     * @param scoreMap 元素分数 Map
     * @return 新增数量
     */
    @Override
    public int zAddAll(String key, Map<Object, Double> scoreMap) {
        checkKey(key);
        if (CollUtil.isEmpty(scoreMap)) {
            return 0;
        }

        return redissonClient.<Object>getScoredSortedSet(key).addAll(scoreMap);
    }

    /**
     * 删除有序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    @Override
    public boolean zRemove(String key, Object... values) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getScoredSortedSet(key).removeAll(Arrays.asList(values));
    }

    /**
     * 获取元素分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 分数
     */
    @Override
    public Double zScore(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }

        return redissonClient.<Object>getScoredSortedSet(key).getScore(value);
    }

    /**
     * 获取升序排名，从 0 开始。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    @Override
    public Integer zRank(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }

        return redissonClient.<Object>getScoredSortedSet(key).rank(value);
    }

    /**
     * 获取降序排名，从 0 开始。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 排名
     */
    @Override
    public Integer zRevRank(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return null;
        }

        return redissonClient.<Object>getScoredSortedSet(key).revRank(value);
    }

    /**
     * 获取分数区间元素。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素集合
     */
    @Override
    public Set<Object> zRangeByScore(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        Collection<Object> values = redissonClient.<Object>getScoredSortedSet(key)
                .valueRange(min, true, max, true);
        return new LinkedHashSet<>(values);
    }

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
    @Override
    public Collection<Object> zRangeByScore(String key, double min, double max, int offset, int count) {
        if (StrUtil.isBlank(key) || offset < 0 || count <= 0) {
            return Collections.emptyList();
        }

        return redissonClient.<Object>getScoredSortedSet(key)
                .valueRange(min, true, max, true, offset, count);
    }

    /**
     * 获取分数区间元素及分数。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    @Override
    public Map<Object, Double> zRangeByScoreWithScores(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        Collection<ScoredEntry<Object>> entries = redissonClient.<Object>getScoredSortedSet(key)
                .entryRange(min, true, max, true);
        return scoredEntriesToMap(entries);
    }

    /**
     * 获取降序分数区间元素及分数。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素分数 Map
     */
    @Override
    public Map<Object, Double> zRevRangeByScoreWithScores(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        List<ScoredEntry<Object>> entries = new ArrayList<>(
                redissonClient.<Object>getScoredSortedSet(key).entryRange(min, true, max, true)
        );
        Collections.reverse(entries);

        return scoredEntriesToMap(entries);
    }

    /**
     * 获取排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    @Override
    public Set<Object> zRange(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        Collection<Object> values = redissonClient.<Object>getScoredSortedSet(key).valueRange(start, end);
        return new LinkedHashSet<>(values);
    }

    /**
     * 获取排名区间元素及分数。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    @Override
    public Map<Object, Double> zRangeWithScores(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        Collection<ScoredEntry<Object>> entries = redissonClient.<Object>getScoredSortedSet(key)
                .entryRange(start, end);
        return scoredEntriesToMap(entries);
    }

    /**
     * 获取降序排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素集合
     */
    @Override
    public Set<Object> zRevRange(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        Collection<Object> values = redissonClient.<Object>getScoredSortedSet(key)
                .valueRangeReversed(start, end);
        return new LinkedHashSet<>(values);
    }

    /**
     * 获取降序排名区间元素及分数。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 元素分数 Map
     */
    @Override
    public Map<Object, Double> zRevRangeWithScores(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyMap();
        }

        Collection<ScoredEntry<Object>> entries = redissonClient.<Object>getScoredSortedSet(key)
                .entryRangeReversed(start, end);
        return scoredEntriesToMap(entries);
    }

    /**
     * 增加元素分数。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param delta 分数增量
     * @return 最新分数
     */
    @Override
    public Double zIncrBy(String key, Object value, double delta) {
        checkKey(key);
        Assert.notNull(value, "有序集合元素不能为空");

        return redissonClient.<Object>getScoredSortedSet(key).addScore(value, delta);
    }

    /**
     * 获取有序集合元素数量。
     *
     * @param key Redis 键
     * @return 数量
     */
    @Override
    public int zCard(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.<Object>getScoredSortedSet(key).size();
    }

    /**
     * 获取分数区间元素数量。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 数量
     */
    @Override
    public long zCount(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getScoredSortedSet(key).count(min, true, max, true);
    }

    /**
     * 删除分数区间元素。
     *
     * @param key Redis 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 删除数量
     */
    @Override
    public long zRemoveRangeByScore(String key, double min, double max) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getScoredSortedSet(key)
                .removeRangeByScore(min, true, max, true);
    }

    /**
     * 删除排名区间元素。
     *
     * @param key   Redis 键
     * @param start 开始排名
     * @param end   结束排名
     * @return 删除数量
     */
    @Override
    public long zRemoveRangeByRank(String key, int start, int end) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.<Object>getScoredSortedSet(key).removeRangeByRank(start, end);
    }

    /**
     * 弹出分数最小的元素。
     * 注意：当前实现是先查再删，不是严格原子弹出；如需高并发严格原子语义，后续可改为 Lua 或 Redisson 原生弹出 API。
     *
     * @param key Redis 键
     * @return 元素及分数
     */
    @Override
    public ScoredEntry<Object> zPopFirst(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        RScoredSortedSet<Object> zSet = redissonClient.getScoredSortedSet(key);
        Collection<ScoredEntry<Object>> entries = zSet.entryRange(0, 0);
        if (CollUtil.isEmpty(entries)) {
            return null;
        }

        ScoredEntry<Object> entry = entries.iterator().next();
        zSet.remove(entry.getValue());
        return entry;
    }

    /**
     * 弹出分数最大的元素。
     * 注意：当前实现是先查再删，不是严格原子弹出；如需高并发严格原子语义，后续可改为 Lua 或 Redisson 原生弹出 API。
     *
     * @param key Redis 键
     * @return 元素及分数
     */
    @Override
    public ScoredEntry<Object> zPopLast(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        RScoredSortedSet<Object> zSet = redissonClient.getScoredSortedSet(key);
        Collection<ScoredEntry<Object>> entries = zSet.entryRangeReversed(0, 0);
        if (CollUtil.isEmpty(entries)) {
            return null;
        }

        ScoredEntry<Object> entry = entries.iterator().next();
        zSet.remove(entry.getValue());
        return entry;
    }

    // -------------------------------------------------------------------------
    // 分布式锁与同步器
    // -------------------------------------------------------------------------

    /**
     * 获取可重入锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    @Override
    public RLock getLock(String lockKey) {
        checkKey(lockKey);
        return redissonClient.getLock(lockKey);
    }

    /**
     * 获取公平锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    @Override
    public RLock getFairLock(String lockKey) {
        checkKey(lockKey);
        return redissonClient.getFairLock(lockKey);
    }

    /**
     * 获取自旋锁。
     *
     * @param lockKey 锁 key
     * @return RLock
     */
    @Override
    public RLock getSpinLock(String lockKey) {
        checkKey(lockKey);
        return redissonClient.getSpinLock(lockKey);
    }

    /**
     * 获取联锁。
     *
     * @param locks 多个锁
     * @return RLock
     */
    @Override
    public RLock getMultiLock(RLock... locks) {
        Assert.isTrue(ArrayUtil.isNotEmpty(locks), "联锁对象不能为空");
        for (RLock lock : locks) {
            Assert.notNull(lock, "联锁对象不能包含空锁");
        }
        return redissonClient.getMultiLock(locks);
    }

    /**
     * 阻塞加锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void lock(String lockKey) {
        RLock lock = getLock(lockKey);
        lock.lock();
    }

    /**
     * 阻塞加锁并指定自动释放时间。
     *
     * @param lockKey   锁 key
     * @param leaseTime 持有时间
     * @param unit      时间单位
     */
    @Override
    public void lock(String lockKey, long leaseTime, TimeUnit unit) {
        RLock lock = getLock(lockKey);
        Assert.notNull(unit, "时间单位不能为空");

        if (leaseTime <= 0) {
            lock.lock();
            return;
        }

        lock.lock(leaseTime, unit);
    }

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
    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = getLock(lockKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        if (leaseTime <= 0) {
            return lock.tryLock(waitTime, unit);
        }

        return lock.tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 释放锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void unlock(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return;
        }

        RLock lock = redissonClient.getLock(lockKey);
        if (!lock.isHeldByCurrentThread()) {
            log.warn("当前线程未持有分布式锁，跳过释放，lockKey={}", lockKey);
            return;
        }

        try {
            lock.unlock();
        } catch (Exception e) {
            log.error("释放分布式锁异常，lockKey={}", lockKey, e);
        }
    }

    /**
     * 执行带锁任务。
     *
     * @param lockKey 锁 key
     * @param task    任务
     */
    @Override
    public void executeWithLock(String lockKey, Runnable task) {
        Assert.notNull(task, "锁内任务不能为空");

        RLock lock = getLock(lockKey);
        boolean locked = false;

        try {
            lock.lock();
            locked = true;
            task.run();
        } finally {
            unlockSafely(lockKey, lock, locked);
        }
    }

    /**
     * 执行带锁任务并返回结果。
     *
     * @param lockKey  锁 key
     * @param supplier 任务
     * @param <T>      返回类型
     * @return 任务结果
     */
    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        Assert.notNull(supplier, "锁内任务不能为空");

        RLock lock = getLock(lockKey);
        boolean locked = false;

        try {
            lock.lock();
            locked = true;
            return supplier.get();
        } finally {
            unlockSafely(lockKey, lock, locked);
        }
    }

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
    @Override
    public boolean tryExecuteWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable task) {
        Assert.notNull(task, "锁内任务不能为空");
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        RLock lock = getLock(lockKey);
        boolean locked = false;

        try {
            locked = leaseTime <= 0
                    ? lock.tryLock(waitTime, unit)
                    : lock.tryLock(waitTime, leaseTime, unit);

            if (!locked) {
                log.warn("获取分布式锁失败，lockKey={}", lockKey);
                return false;
            }

            task.run();
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
     * 判断当前线程是否持有锁。
     *
     * @param lockKey 锁 key
     * @return 当前线程持有返回 true
     */
    @Override
    public boolean isHeldByCurrentThread(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return false;
        }

        return redissonClient.getLock(lockKey).isHeldByCurrentThread();
    }

    /**
     * 判断锁是否被任意线程持有。
     *
     * @param lockKey 锁 key
     * @return 已加锁返回 true
     */
    @Override
    public boolean isLocked(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return false;
        }

        return redissonClient.getLock(lockKey).isLocked();
    }

    /**
     * 获取读写锁。
     *
     * @param lockKey 锁 key
     * @return RReadWriteLock
     */
    @Override
    public RReadWriteLock getReadWriteLock(String lockKey) {
        checkKey(lockKey);
        return redissonClient.getReadWriteLock(lockKey);
    }

    /**
     * 获取读锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void readLock(String lockKey) {
        getReadWriteLock(lockKey).readLock().lock();
    }

    /**
     * 获取写锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void writeLock(String lockKey) {
        getReadWriteLock(lockKey).writeLock().lock();
    }

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
    @Override
    public boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = getReadWriteLock(lockKey).readLock();
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        if (leaseTime <= 0) {
            return lock.tryLock(waitTime, unit);
        }

        return lock.tryLock(waitTime, leaseTime, unit);
    }

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
    @Override
    public boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = getReadWriteLock(lockKey).writeLock();
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        if (leaseTime <= 0) {
            return lock.tryLock(waitTime, unit);
        }

        return lock.tryLock(waitTime, leaseTime, unit);
    }

    /**
     * 释放读锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void unlockRead(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return;
        }

        RLock lock = redissonClient.getReadWriteLock(lockKey).readLock();
        unlockSafely(lockKey + ":read", lock, true);
    }

    /**
     * 释放写锁。
     *
     * @param lockKey 锁 key
     */
    @Override
    public void unlockWrite(String lockKey) {
        if (StrUtil.isBlank(lockKey)) {
            return;
        }

        RLock lock = redissonClient.getReadWriteLock(lockKey).writeLock();
        unlockSafely(lockKey + ":write", lock, true);
    }

    /**
     * 获取闭锁。
     *
     * @param latchKey 闭锁 key
     * @return RCountDownLatch
     */
    @Override
    public RCountDownLatch getCountDownLatch(String latchKey) {
        checkKey(latchKey);
        return redissonClient.getCountDownLatch(latchKey);
    }

    /**
     * 设置闭锁计数。
     *
     * @param latchKey 闭锁 key
     * @param count    计数
     */
    @Override
    public void setCount(String latchKey, int count) {
        checkKey(latchKey);
        Assert.isTrue(count > 0, "闭锁计数必须大于 0");

        boolean success = redissonClient.getCountDownLatch(latchKey).trySetCount(count);
        if (!success) {
            log.warn("设置闭锁计数失败，可能闭锁已存在，latchKey={}，count={}", latchKey, count);
        }
    }

    /**
     * 闭锁计数减一。
     *
     * @param latchKey 闭锁 key
     */
    @Override
    public void countDown(String latchKey) {
        checkKey(latchKey);
        redissonClient.getCountDownLatch(latchKey).countDown();
    }

    /**
     * 等待闭锁完成。
     *
     * @param latchKey 闭锁 key
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public void await(String latchKey) throws InterruptedException {
        checkKey(latchKey);
        redissonClient.getCountDownLatch(latchKey).await();
    }

    /**
     * 等待闭锁完成。
     *
     * @param latchKey 闭锁 key
     * @param timeout  超时时间
     * @param unit     时间单位
     * @return 是否完成
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public boolean await(String latchKey, long timeout, TimeUnit unit) throws InterruptedException {
        checkKey(latchKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        return redissonClient.getCountDownLatch(latchKey).await(timeout, unit);
    }

    /**
     * 获取信号量。
     *
     * @param semaphoreKey 信号量 key
     * @return RSemaphore
     */
    @Override
    public RSemaphore getSemaphore(String semaphoreKey) {
        checkKey(semaphoreKey);
        return redissonClient.getSemaphore(semaphoreKey);
    }

    /**
     * 初始化信号量许可数量。
     *
     * @param semaphoreKey 信号量 key
     * @param permits      许可数量
     */
    @Override
    public void trySetPermits(String semaphoreKey, int permits) {
        checkKey(semaphoreKey);
        Assert.isTrue(permits > 0, "信号量许可数量必须大于 0");

        boolean success = redissonClient.getSemaphore(semaphoreKey).trySetPermits(permits);
        if (!success) {
            log.warn("初始化信号量许可失败，可能信号量已存在，semaphoreKey={}，permits={}", semaphoreKey, permits);
        }
    }

    /**
     * 获取一个信号量许可。
     *
     * @param semaphoreKey 信号量 key
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public void acquire(String semaphoreKey) throws InterruptedException {
        checkKey(semaphoreKey);
        redissonClient.getSemaphore(semaphoreKey).acquire();
    }

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
    @Override
    public boolean tryAcquire(String semaphoreKey, int permits, long waitTime, TimeUnit unit) throws InterruptedException {
        checkKey(semaphoreKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(permits > 0, "信号量许可数量必须大于 0");
        Assert.isTrue(waitTime >= 0, "等待时间不能小于 0");

        return redissonClient.getSemaphore(semaphoreKey).tryAcquire(permits, waitTime, unit);
    }

    /**
     * 释放信号量许可。
     *
     * @param semaphoreKey 信号量 key
     */
    @Override
    public void release(String semaphoreKey) {
        checkKey(semaphoreKey);
        redissonClient.getSemaphore(semaphoreKey).release();
    }

    /**
     * 获取可用许可数量。
     *
     * @param semaphoreKey 信号量 key
     * @return 可用许可数量
     */
    @Override
    public int availablePermits(String semaphoreKey) {
        checkKey(semaphoreKey);
        return redissonClient.getSemaphore(semaphoreKey).availablePermits();
    }

    /**
     * 获取可过期信号量。
     *
     * @param semaphoreKey 信号量 key
     * @return RPermitExpirableSemaphore
     */
    @Override
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String semaphoreKey) {
        checkKey(semaphoreKey);
        return redissonClient.getPermitExpirableSemaphore(semaphoreKey);
    }

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
    @Override
    public boolean rateLimiterInit(String key, RateType rateType, long rate, long interval, RateIntervalUnit unit) {
        checkKey(key);
        Assert.notNull(rateType, "限流类型不能为空");
        Assert.notNull(unit, "限流时间单位不能为空");
        Assert.isTrue(rate > 0, "令牌数必须大于 0");
        Assert.isTrue(interval > 0, "限流间隔必须大于 0");

        boolean initialized = redissonClient.getRateLimiter(key)
                .trySetRate(rateType, rate, interval, unit);

        if (initialized) {
            log.info("Redis 限流器初始化成功，key={}，rate={}，interval={}，unit={}", key, rate, interval, unit);
        } else {
            log.debug("Redis 限流器已存在，跳过初始化，key={}", key);
        }

        return initialized;
    }

    /**
     * 更新限流器速率。
     *
     * @param key      限流器 key
     * @param rateType 限流类型
     * @param rate     令牌数
     * @param interval 间隔
     * @param unit     间隔单位
     */
    @Override
    public void rateLimiterSetRate(String key, RateType rateType, long rate, long interval, RateIntervalUnit unit) {
        checkKey(key);
        Assert.notNull(rateType, "限流类型不能为空");
        Assert.notNull(unit, "限流时间单位不能为空");
        Assert.isTrue(rate > 0, "令牌数必须大于 0");
        Assert.isTrue(interval > 0, "限流间隔必须大于 0");

        redissonClient.getRateLimiter(key).setRate(rateType, rate, interval, unit);
        log.info("Redis 限流器速率已更新，key={}，rate={}，interval={}，unit={}", key, rate, interval, unit);
    }

    /**
     * 尝试获取一个令牌。
     *
     * @param key 限流器 key
     * @return 是否获取成功
     */
    @Override
    public boolean rateLimiterTryAcquire(String key) {
        checkKey(key);
        return redissonClient.getRateLimiter(key).tryAcquire();
    }

    /**
     * 尝试获取指定数量令牌。
     *
     * @param key     限流器 key
     * @param permits 令牌数量
     * @return 是否获取成功
     */
    @Override
    public boolean rateLimiterTryAcquire(String key, long permits) {
        checkKey(key);
        Assert.isTrue(permits > 0, "令牌数量必须大于 0");

        return redissonClient.getRateLimiter(key).tryAcquire(permits);
    }

    /**
     * 阻塞获取指定数量令牌。
     *
     * @param key     限流器 key
     * @param permits 令牌数量
     */
    @Override
    public void rateLimiterAcquire(String key, long permits) {
        checkKey(key);
        Assert.isTrue(permits > 0, "令牌数量必须大于 0");

        redissonClient.getRateLimiter(key).acquire(permits);
    }

    /**
     * 尝试等待获取令牌。
     *
     * @param key     限流器 key
     * @param timeout 等待时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    @Override
    public boolean rateLimiterTryAcquire(String key, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        return redissonClient.getRateLimiter(key).tryAcquire(timeout, unit);
    }

    /**
     * 获取限流器对象。
     *
     * @param key 限流器 key
     * @return RRateLimiter
     */
    @Override
    public RRateLimiter rateLimiterGet(String key) {
        checkKey(key);
        return redissonClient.getRateLimiter(key);
    }

    /**
     * 删除限流器。
     *
     * @param key 限流器 key
     * @return 是否删除成功
     */
    @Override
    public boolean rateLimiterDelete(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.getRateLimiter(key).delete();
    }

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
    @Override
    public <T> RBloomFilter<T> getBloomFilter(String key) {
        checkKey(key);
        return redissonClient.getBloomFilter(key);
    }

    /**
     * 初始化布隆过滤器。
     *
     * @param key                Redis 键
     * @param expectedInsertions 预计插入量
     * @param falseProbability   误判率
     */
    @Override
    public void bloomInit(String key, long expectedInsertions, double falseProbability) {
        checkKey(key);
        Assert.isTrue(expectedInsertions > 0, "预计插入量必须大于 0");
        Assert.isTrue(falseProbability > 0 && falseProbability < 1, "误判率必须在 0 到 1 之间");

        boolean initialized = redissonClient.getBloomFilter(key)
                .tryInit(expectedInsertions, falseProbability);

        if (initialized) {
            log.info("Redis 布隆过滤器初始化成功，key={}，expectedInsertions={}，falseProbability={}",
                    key, expectedInsertions, falseProbability);
        } else {
            log.debug("Redis 布隆过滤器已存在，跳过初始化，key={}", key);
        }
    }

    /**
     * 判断布隆过滤器是否可能包含元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 可能包含返回 true
     */
    @Override
    public boolean bloomContains(String key, Object value) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(value)) {
            return false;
        }

        return redissonClient.getBloomFilter(key).contains(value);
    }

    /**
     * 添加布隆过滤器元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    @Override
    public boolean bloomAdd(String key, Object value) {
        checkKey(key);
        Assert.notNull(value, "布隆过滤器元素不能为空");

        return redissonClient.getBloomFilter(key).add(value);
    }

    /**
     * 批量添加布隆过滤器元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 新增数量
     */
    @Override
    public long bloomAddAll(String key, Collection<?> values) {
        checkKey(key);
        if (CollUtil.isEmpty(values)) {
            return 0L;
        }

        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        long added = 0L;

        for (Object value : values) {
            if (ObjectUtil.isNotNull(value) && bloomFilter.add(value)) {
                added++;
            }
        }

        return added;
    }

    /**
     * 删除布隆过滤器。
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @Override
    public boolean bloomDelete(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.getBloomFilter(key).delete();
    }

    /**
     * 判断布隆过滤器是否存在。
     *
     * @param key Redis 键
     * @return 存在返回 true
     */
    @Override
    public boolean bloomExists(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.getBloomFilter(key).isExists();
    }

    /**
     * 获取预计插入量。
     *
     * @param key Redis 键
     * @return 预计插入量
     */
    @Override
    public long bloomGetExpectedInsertions(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        if (!bloomFilter.isExists()) {
            return 0L;
        }

        return bloomFilter.getExpectedInsertions();
    }

    /**
     * 获取误判率。
     *
     * @param key Redis 键
     * @return 误判率
     */
    @Override
    public double bloomGetFalseProbability(String key) {
        if (StrUtil.isBlank(key)) {
            return 0D;
        }

        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
        if (!bloomFilter.isExists()) {
            return 0D;
        }

        return bloomFilter.getFalseProbability();
    }

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
    @Override
    public void bitSet(String key, long index, boolean value) {
        checkKey(key);
        Assert.isTrue(index >= 0, "位图索引不能小于 0");

        redissonClient.getBitSet(key).set(index, value);
    }

    /**
     * 获取位图指定位置。
     *
     * @param key   Redis 键
     * @param index 位索引
     * @return 位值
     */
    @Override
    public boolean bitGet(String key, long index) {
        if (StrUtil.isBlank(key) || index < 0) {
            return false;
        }

        return redissonClient.getBitSet(key).get(index);
    }

    /**
     * 获取位图中 true 的数量。
     *
     * @param key Redis 键
     * @return 数量
     */
    @Override
    public long bitCount(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.getBitSet(key).cardinality();
    }

    /**
     * 清空位图。
     *
     * @param key Redis 键
     */
    @Override
    public void bitClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getBitSet(key).clear();
    }

    /**
     * 用户指定日期签到。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     */
    @Override
    public void sign(String keyPrefix, Object userId, LocalDate date) {
        checkKey(keyPrefix);
        Assert.notNull(userId, "用户 ID 不能为空");
        Assert.notNull(date, "签到日期不能为空");

        String key = buildSignKey(keyPrefix, userId, date.getYear());
        int bitIndex = date.getDayOfYear() - 1;

        redissonClient.getBitSet(key).set(bitIndex, true);
        log.info("用户签到成功，key={}，userId={}，date={}", key, userId, date);
    }

    /**
     * 判断用户指定日期是否签到。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     * @return 已签到返回 true
     */
    @Override
    public boolean isSigned(String keyPrefix, Object userId, LocalDate date) {
        if (StrUtil.isBlank(keyPrefix) || ObjectUtil.isNull(userId) || ObjectUtil.isNull(date)) {
            return false;
        }

        String key = buildSignKey(keyPrefix, userId, date.getYear());
        int bitIndex = date.getDayOfYear() - 1;

        return redissonClient.getBitSet(key).get(bitIndex);
    }

    /**
     * 获取用户指定年份签到天数。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param year      年份
     * @return 签到天数
     */
    @Override
    public long getSignCount(String keyPrefix, Object userId, int year) {
        if (StrUtil.isBlank(keyPrefix) || ObjectUtil.isNull(userId) || year <= 0) {
            return 0L;
        }

        String key = buildSignKey(keyPrefix, userId, year);
        return redissonClient.getBitSet(key).cardinality();
    }

    /**
     * 获取从指定日期向前连续签到天数。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期
     * @return 连续签到天数
     */
    @Override
    public int getContinuousSignCount(String keyPrefix, Object userId, LocalDate date) {
        if (StrUtil.isBlank(keyPrefix) || ObjectUtil.isNull(userId) || ObjectUtil.isNull(date)) {
            return 0;
        }

        String key = buildSignKey(keyPrefix, userId, date.getYear());
        RBitSet bitSet = redissonClient.getBitSet(key);

        int count = 0;
        int bitIndex = date.getDayOfYear() - 1;

        for (int i = bitIndex; i >= 0; i--) {
            if (bitSet.get(i)) {
                count++;
            } else {
                break;
            }
        }

        return count;
    }

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
    @Override
    public boolean hllAdd(String key, Object value) {
        checkKey(key);
        Assert.notNull(value, "HyperLogLog 元素不能为空");

        return redissonClient.getHyperLogLog(key).add(value);
    }

    /**
     * 批量添加 HyperLogLog 元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否改变基数估算
     */
    /**
     * 批量添加 HyperLogLog 元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否改变基数估算
     */
    @Override
    public boolean hllAddAll(String key, Collection<?> values) {
        checkKey(key);
        if (CollUtil.isEmpty(values)) {
            return false;
        }

        Collection<Object> valueList = new ArrayList<>(values.size());
        for (Object value : values) {
            if (ObjectUtil.isNotNull(value)) {
                valueList.add(value);
            }
        }

        if (CollUtil.isEmpty(valueList)) {
            return false;
        }

        return redissonClient.<Object>getHyperLogLog(key).addAll(valueList);
    }

    /**
     * 获取 HyperLogLog 基数估算。
     *
     * @param key Redis 键
     * @return 基数估算
     */
    @Override
    public long hllCount(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.getHyperLogLog(key).count();
    }

    /**
     * 合并 HyperLogLog。
     *
     * @param destKey 目标 key
     * @param keys    源 key
     * @return 合并后基数估算
     */
    @Override
    public long hllMerge(String destKey, String... keys) {
        checkKey(destKey);

        String[] keyArray = filterKeys(keys);
        if (ArrayUtil.isEmpty(keyArray)) {
            return hllCount(destKey);
        }

        RHyperLogLog<Object> dest = redissonClient.getHyperLogLog(destKey);
        dest.mergeWith(keyArray);
        return dest.count();
    }

    /**
     * 按日期记录 UV。
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param userFlag  用户唯一标识
     * @param date      日期
     * @return 是否改变基数估算
     */
    @Override
    public boolean uvRecord(String keyPrefix, String bizKey, Object userFlag, LocalDate date) {
        checkKey(keyPrefix);
        checkKey(bizKey);
        Assert.notNull(userFlag, "用户唯一标识不能为空");
        Assert.notNull(date, "UV 日期不能为空");

        String key = buildUvKey(keyPrefix, bizKey, date);
        return hllAdd(key, userFlag);
    }

    /**
     * 获取指定日期 UV。
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param date      日期
     * @return UV 数量
     */
    @Override
    public long uvCount(String keyPrefix, String bizKey, LocalDate date) {
        if (StrUtil.hasBlank(keyPrefix, bizKey) || ObjectUtil.isNull(date)) {
            return 0L;
        }

        String key = buildUvKey(keyPrefix, bizKey, date);
        return hllCount(key);
    }

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
    @Override
    public long geoAdd(String key, double longitude, double latitude, Object member) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.notNull(member, "Geo 成员不能为空");

        return redissonClient.<Object>getGeo(key).add(longitude, latitude, member);
    }

    /**
     * 批量添加地理位置。
     *
     * @param key     Redis 键
     * @param entries 坐标集合
     * @return 添加数量
     */
    @Override
    public long geoAdd(String key, GeoEntry... entries) {
        checkKey(key);
        if (ArrayUtil.isEmpty(entries)) {
            return 0L;
        }

        return redissonClient.getGeo(key).add(entries);
    }

    /**
     * 仅成员不存在时添加地理位置。
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param member    成员
     * @return 是否添加成功
     */
    @Override
    public boolean geoTryAdd(String key, double longitude, double latitude, Object member) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.notNull(member, "Geo 成员不能为空");

        return redissonClient.<Object>getGeo(key).tryAdd(longitude, latitude, member);
    }

    /**
     * 计算两个成员距离。
     *
     * @param key     Redis 键
     * @param member1 成员 1
     * @param member2 成员 2
     * @param unit    单位
     * @return 距离
     */
    @Override
    public Double geoDistance(String key, Object member1, Object member2, GeoUnit unit) {
        if (StrUtil.isBlank(key) || ObjectUtil.hasEmpty(member1, member2, unit)) {
            return null;
        }

        return redissonClient.<Object>getGeo(key).dist(member1, member2, unit);
    }

    /**
     * 查询成员 GeoHash。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return GeoHash Map
     */
    @Override
    public Map<Object, String> geoHash(String key, Object... members) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(members)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object>getGeo(key).hash(members);
    }

    /**
     * 查询成员坐标。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return 坐标 Map
     */
    @Override
    public Map<Object, GeoPosition> geoPosition(String key, Object... members) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(members)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object>getGeo(key).pos(members);
    }

    /**
     * 根据条件搜索地理位置成员。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员列表
     */
    @Override
    public List<Object> geoSearch(String key, GeoSearchArgs args) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(args)) {
            return Collections.emptyList();
        }

        return redissonClient.<Object>getGeo(key).search(args);
    }

    /**
     * 根据条件搜索地理位置成员和距离。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员距离 Map
     */
    @Override
    public Map<Object, Double> geoSearchWithDistance(String key, GeoSearchArgs args) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(args)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object>getGeo(key).searchWithDistance(args);
    }

    /**
     * 根据条件搜索地理位置成员和坐标。
     *
     * @param key  Redis 键
     * @param args 搜索参数
     * @return 成员坐标 Map
     */
    @Override
    public Map<Object, GeoPosition> geoSearchWithPosition(String key, GeoSearchArgs args) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(args)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object>getGeo(key).searchWithPosition(args);
    }

    /**
     * 删除地理位置成员。
     *
     * @param key     Redis 键
     * @param members 成员
     * @return 删除数量
     */
    @Override
    public long geoRemove(String key, Object... members) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(members)) {
            return 0L;
        }

        return redissonClient.<Object>getGeo(key).removeAll(Arrays.asList(members)) ? members.length : 0L;
    }

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
    @Override
    public void sessionSet(String keyPrefix, String token, Object session, Duration ttl) {
        checkKey(keyPrefix);
        checkKey(token);
        Assert.notNull(session, "会话对象不能为空");
        checkPositiveDuration(ttl, "会话过期时间不能为空且必须大于 0");

        String key = buildSessionKey(keyPrefix, token);
        redissonClient.getBucket(key).set(session, ttl.toMillis(), TimeUnit.MILLISECONDS);

        log.info("Redis 会话写入成功，key={}", key);
    }

    /**
     * 获取会话。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @param clazz     目标类型
     * @param <T>       泛型类型
     * @return 会话对象
     */
    @Override
    public <T> T sessionGet(String keyPrefix, String token, Class<T> clazz) {
        if (StrUtil.hasBlank(keyPrefix, token) || ObjectUtil.isNull(clazz)) {
            return null;
        }

        String key = buildSessionKey(keyPrefix, token);
        Object value = redissonClient.getBucket(key).get();
        return convertValue(value, clazz);
    }

    /**
     * 获取会话。
     *
     * @param keyPrefix     会话 key 前缀
     * @param token         Token
     * @param typeReference 目标类型
     * @param <T>           泛型类型
     * @return 会话对象
     */
    @Override
    public <T> T sessionGet(String keyPrefix, String token, TypeReference<T> typeReference) {
        if (StrUtil.hasBlank(keyPrefix, token) || ObjectUtil.isNull(typeReference)) {
            return null;
        }

        String key = buildSessionKey(keyPrefix, token);
        Object value = redissonClient.getBucket(key).get();
        return convertValue(value, typeReference);
    }

    /**
     * 刷新会话过期时间。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @param ttl       过期时间
     * @return 是否刷新成功
     */
    @Override
    public boolean sessionRefresh(String keyPrefix, String token, Duration ttl) {
        if (StrUtil.hasBlank(keyPrefix, token)) {
            return false;
        }
        checkPositiveDuration(ttl, "会话过期时间不能为空且必须大于 0");

        String key = buildSessionKey(keyPrefix, token);
        boolean success = redissonClient.getBucket(key).expire(ttl);

        if (success) {
            log.info("Redis 会话续期成功，key={}", key);
        } else {
            log.warn("Redis 会话续期失败，可能会话不存在，key={}", key);
        }

        return success;
    }

    /**
     * 删除会话。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 是否删除成功
     */
    @Override
    public boolean sessionDelete(String keyPrefix, String token) {
        if (StrUtil.hasBlank(keyPrefix, token)) {
            return false;
        }

        String key = buildSessionKey(keyPrefix, token);
        boolean deleted = redissonClient.getBucket(key).delete();

        if (deleted) {
            log.info("Redis 会话删除成功，key={}", key);
        }

        return deleted;
    }

    // -------------------------------------------------------------------------
    // Queue / BlockingQueue / DelayedQueue / ReliableQueue
    // -------------------------------------------------------------------------

    /**
     * 入普通队列。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param <T>      元素类型
     * @return 是否入队成功
     */
    @Override
    public <T> boolean enqueue(String queueKey, T value) {
        checkKey(queueKey);

        return redissonClient.<T>getQueue(queueKey).offer(value);
    }

    /**
     * 出普通队列。
     *
     * @param queueKey 队列 key
     * @param <T>      元素类型
     * @return 元素
     */
    @Override
    public <T> T dequeue(String queueKey) {
        if (StrUtil.isBlank(queueKey)) {
            return null;
        }

        return redissonClient.<T>getQueue(queueKey).poll();
    }

    /**
     * 阻塞入队。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param <T>      元素类型
     * @throws InterruptedException 线程中断时抛出
     */
    @Override
    public <T> void enqueueBlocking(String queueKey, T value) throws InterruptedException {
        checkKey(queueKey);

        redissonClient.<T>getBlockingQueue(queueKey).put(value);
    }

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
    @Override
    public <T> boolean enqueueBlocking(String queueKey, T value, long timeout, TimeUnit unit) throws InterruptedException {
        checkKey(queueKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        return redissonClient.<T>getBlockingQueue(queueKey).offer(value, timeout, unit);
    }

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
    @Override
    public <T> T dequeueBlocking(String queueKey, long timeout, TimeUnit unit) throws InterruptedException {
        checkKey(queueKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于 0");

        return redissonClient.<T>getBlockingQueue(queueKey).poll(timeout, unit);
    }

    /**
     * 获取队列长度。
     *
     * @param queueKey 队列 key
     * @return 长度
     */
    @Override
    public long queueSize(String queueKey) {
        if (StrUtil.isBlank(queueKey)) {
            return 0L;
        }

        return redissonClient.getQueue(queueKey).size();
    }

    /**
     * 添加延迟队列任务。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @param delay    延迟时间
     * @param unit     时间单位
     * @param <T>      元素类型
     */
    @Override
    public <T> void enqueueDelayed(String queueKey, T value, long delay, TimeUnit unit) {
        checkKey(queueKey);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(delay >= 0, "延迟时间不能小于 0");

        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueKey);
        RDelayedQueue<T> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(value, delay, unit);

        log.info("Redis 延迟任务入队成功，queueKey={}，delay={}，unit={}", queueKey, delay, unit);
    }

    /**
     * 获取延迟队列对象。
     *
     * @param queueKey 队列 key
     * @param <T>      元素类型
     * @return RDelayedQueue
     */
    @Override
    public <T> RDelayedQueue<T> getDelayedQueue(String queueKey) {
        checkKey(queueKey);

        RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue(queueKey);
        return redissonClient.getDelayedQueue(blockingQueue);
    }

    /**
     * 清空队列。
     *
     * @param queueKey 队列 key
     */
    @Override
    public void clearQueue(String queueKey) {
        if (StrUtil.isBlank(queueKey)) {
            return;
        }

        redissonClient.getQueue(queueKey).clear();
    }

    /**
     * 判断队列是否为空。
     *
     * @param queueKey 队列 key
     * @return 为空返回 true
     */
    @Override
    public boolean isQueueEmpty(String queueKey) {
        if (StrUtil.isBlank(queueKey)) {
            return true;
        }

        return redissonClient.getQueue(queueKey).isEmpty();
    }

    /**
     * 删除队列元素。
     *
     * @param queueKey 队列 key
     * @param value    元素
     * @return 是否删除成功
     */
    @Override
    public boolean removeFromQueue(String queueKey, Object value) {
        if (StrUtil.isBlank(queueKey)) {
            return false;
        }

        return redissonClient.getQueue(queueKey).remove(value);
    }

    /**
     * 获取环形缓冲队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RRingBuffer
     */
    @Override
    public <T> RRingBuffer<T> getRingBuffer(String key) {
        checkKey(key);

        return redissonClient.getRingBuffer(key);
    }

    /**
     * 获取优先级队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityQueue
     */
    @Override
    public <T> RPriorityQueue<T> getPriorityQueue(String key) {
        checkKey(key);

        return redissonClient.getPriorityQueue(key);
    }

    /**
     * 获取阻塞双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RBlockingDeque
     */
    @Override
    public <T> RBlockingDeque<T> getBlockingDeque(String key) {
        checkKey(key);

        return redissonClient.getBlockingDeque(key);
    }

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
    @Override
    public long publish(String channel, Object message) {
        checkKey(channel);

        return redissonClient.getTopic(channel).publish(message);
    }

    /**
     * 订阅频道。
     *
     * @param channel         频道
     * @param messageConsumer 消费回调
     * @return 监听器 ID
     */
    @Override
    public int subscribe(String channel, Consumer<Object> messageConsumer) {
        checkKey(channel);
        Assert.notNull(messageConsumer, "消息消费回调不能为空");

        MessageListener<Object> listener = (topic, message) -> {
            try {
                messageConsumer.accept(message);
            } catch (Exception e) {
                log.error("Redis 订阅消息消费异常，channel={}，message={}", channel, message, e);
            }
        };

        int listenerId = redissonClient.getTopic(channel).addListener(Object.class, listener);
        log.info("Redis 频道订阅成功，channel={}，listenerId={}", channel, listenerId);
        return listenerId;
    }

    /**
     * 取消订阅频道。
     *
     * @param channel    频道
     * @param listenerId 监听器 ID
     */
    @Override
    public void unsubscribe(String channel, int listenerId) {
        if (StrUtil.isBlank(channel)) {
            return;
        }

        redissonClient.getTopic(channel).removeListener(listenerId);
        log.info("Redis 频道取消订阅成功，channel={}，listenerId={}", channel, listenerId);
    }

    /**
     * 取消订阅频道全部监听器。
     *
     * @param channel 频道
     */
    @Override
    public void unsubscribe(String channel) {
        if (StrUtil.isBlank(channel)) {
            return;
        }

        redissonClient.getTopic(channel).removeAllListeners();
        log.info("Redis 频道全部监听器已移除，channel={}", channel);
    }

    /**
     * 获取模式主题。
     *
     * @param pattern 频道通配符
     * @return RPatternTopic
     */
    @Override
    public RPatternTopic getPatternTopic(String pattern) {
        checkKey(pattern);

        return redissonClient.getPatternTopic(pattern);
    }

    /**
     * 获取可靠主题。
     *
     * @param topic 主题名
     * @return RReliableTopic
     */
    @Override
    public RReliableTopic getReliableTopic(String topic) {
        checkKey(topic);

        return redissonClient.getReliableTopic(topic);
    }

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
    @Override
    public StreamMessageId streamAdd(String streamKey, Map<Object, Object> entries) {
        checkKey(streamKey);
        Assert.isTrue(CollUtil.isNotEmpty(entries), "Stream 消息字段不能为空");

        return redissonClient.<Object, Object>getStream(streamKey)
                .add(StreamAddArgs.entries(entries));
    }

    /**
     * 添加 Stream 消息。
     *
     * @param streamKey Stream key
     * @param args      添加参数
     * @param <K>       字段类型
     * @param <V>       值类型
     * @return 消息 ID
     */
    @Override
    public <K, V> StreamMessageId streamAdd(String streamKey, StreamAddArgs<K, V> args) {
        checkKey(streamKey);
        Assert.notNull(args, "Stream 添加参数不能为空");

        return redissonClient.<K, V>getStream(streamKey).add(args);
    }

    /**
     * 读取 Stream 消息。
     *
     * @param streamKey Stream key
     * @param args      读取参数
     * @return 消息 Map
     */
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamRead(String streamKey, StreamReadArgs args) {
        checkKey(streamKey);
        Assert.notNull(args, "Stream 读取参数不能为空");

        return redissonClient.<Object, Object>getStream(streamKey).read(args);
    }

    /**
     * 创建消费组。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param id        起始消息 ID
     */
    @Override
    public void streamCreateGroup(String streamKey, String groupName, StreamMessageId id) {
        checkKey(streamKey);
        checkKey(groupName);
        Assert.notNull(id, "Stream 起始消息 ID 不能为空");

        try {
            StreamCreateGroupArgs args = StreamCreateGroupArgs.name(groupName)
                    .id(id)
                    .makeStream();

            redissonClient.<Object, Object>getStream(streamKey).createGroup(args);
            log.info("Redis Stream 消费组创建成功，streamKey={}，groupName={}，id={}", streamKey, groupName, id);
        } catch (Exception e) {
            log.warn("Redis Stream 消费组创建失败，可能消费组已存在，streamKey={}，groupName={}", streamKey, groupName, e);
        }
    }

    /**
     * 读取消费组消息。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @param args         读取参数
     * @return 消息 Map
     */
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamReadGroup(
            String streamKey,
            String groupName,
            String consumerName,
            StreamReadGroupArgs args
    ) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        Assert.notNull(args, "Stream 消费组读取参数不能为空");

        return redissonClient.<Object, Object>getStream(streamKey)
                .readGroup(groupName, consumerName, args);
    }

    /**
     * 确认 Stream 消息。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param ids       消息 ID
     * @return 确认数量
     */
    @Override
    public long streamAck(String streamKey, String groupName, StreamMessageId... ids) {
        checkKey(streamKey);
        checkKey(groupName);
        if (ArrayUtil.isEmpty(ids)) {
            return 0L;
        }

        return redissonClient.<Object, Object>getStream(streamKey).ack(groupName, ids);
    }

    /**
     * 删除 Stream 消息。
     *
     * @param streamKey Stream key
     * @param ids       消息 ID
     * @return 删除数量
     */
    @Override
    public long streamRemove(String streamKey, StreamMessageId... ids) {
        checkKey(streamKey);
        if (ArrayUtil.isEmpty(ids)) {
            return 0L;
        }

        return redissonClient.<Object, Object>getStream(streamKey).remove(ids);
    }

    /**
     * 获取 Stream 长度。
     *
     * @param streamKey Stream key
     * @return 长度
     */
    @Override
    public long streamSize(String streamKey) {
        if (StrUtil.isBlank(streamKey)) {
            return 0L;
        }

        return redissonClient.getStream(streamKey).size();
    }

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
    @Override
    public <T> T eval(String script, RScript.Mode mode, RScript.ReturnType returnType, List<Object> keys, Object... values) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua 脚本不能为空");
        Assert.notNull(mode, "Lua 执行模式不能为空");
        Assert.notNull(returnType, "Lua 返回类型不能为空");

        List<Object> safeKeys = safeScriptKeys(keys);
        return redissonClient.getScript().eval(mode, script, returnType, safeKeys, values);
    }

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
    @Override
    public <T> T eval(String script, Class<T> returnType, List<Object> keys, Object... args) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua 脚本不能为空");
        Assert.notNull(returnType, "Lua 返回类型不能为空");

        Object result = redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.VALUE,
                safeScriptKeys(keys),
                args
        );
        return convertValue(result, returnType);
    }

    /**
     * 执行 Lua 脚本但不关心结果。
     *
     * @param script Lua 脚本
     * @param keys   KEYS
     * @param args   ARGV
     */
    @Override
    public void evalNoResult(String script, List<Object> keys, Object... args) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua 脚本不能为空");

        redissonClient.getScript().eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.VALUE,
                safeScriptKeys(keys),
                args
        );
    }

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
    @Override
    public <T> T evalBySha(String sha1, Class<T> returnType, List<Object> keys, Object... values) {
        Assert.isTrue(StrUtil.isNotBlank(sha1), "Lua 脚本 SHA1 不能为空");
        Assert.notNull(returnType, "Lua 返回类型不能为空");

        Object result = redissonClient.getScript().evalSha(
                RScript.Mode.READ_WRITE,
                sha1,
                RScript.ReturnType.VALUE,
                safeScriptKeys(keys),
                values
        );
        return convertValue(result, returnType);
    }

    /**
     * 加载 Lua 脚本。
     *
     * @param script Lua 脚本
     * @return SHA1
     */
    @Override
    public String loadScript(String script) {
        Assert.isTrue(StrUtil.isNotBlank(script), "Lua 脚本不能为空");

        String sha1 = redissonClient.getScript().scriptLoad(script);
        log.info("Redis Lua 脚本加载成功，sha1={}", sha1);
        return sha1;
    }

    /**
     * 创建批处理对象。
     *
     * @return RBatch
     */
    @Override
    public RBatch createBatch() {
        return redissonClient.createBatch();
    }

    /**
     * 创建事务对象。
     *
     * @return RTransaction
     */
    @Override
    public RTransaction createTransaction() {
        return redissonClient.createTransaction(TransactionOptions.defaults());
    }


    // -------------------------------------------------------------------------
    // LocalCachedMap / 本地缓存 Map
    // -------------------------------------------------------------------------

    /**
     * 获取本地缓存 Map。
     *
     * @param key     Redis 键
     * @param options 本地缓存配置
     * @param <K>     字段类型
     * @param <V>     值类型
     * @return RLocalCachedMap
     */
    @Override
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String key, LocalCachedMapOptions<K, V> options) {
        checkKey(key);
        Assert.notNull(options, "本地缓存配置不能为空");
        return redissonClient.getLocalCachedMap(key, options);
    }

    /**
     * 设置本地缓存 Map 字段值。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param value   字段值
     * @param options 本地缓存配置
     */
    @Override
    public void lcPut(String key, Object field, Object value, LocalCachedMapOptions<Object, Object> options) {
        checkKey(key);
        Assert.notNull(field, "本地缓存 Map 字段不能为空");

        getLocalCachedMap(key, options).fastPut(field, value);
    }

    /**
     * 字段不存在时设置本地缓存 Map 字段值。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param value   字段值
     * @param options 本地缓存配置
     * @return 是否设置成功
     */
    @Override
    public boolean lcPutIfAbsent(String key, Object field, Object value, LocalCachedMapOptions<Object, Object> options) {
        checkKey(key);
        Assert.notNull(field, "本地缓存 Map 字段不能为空");

        return getLocalCachedMap(key, options).fastPutIfAbsent(field, value);
    }

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
    @Override
    public <T> T lcGet(String key, Object field, Class<T> clazz, LocalCachedMapOptions<Object, Object> options) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(field) || ObjectUtil.isNull(clazz) || ObjectUtil.isNull(options)) {
            return null;
        }

        Object value = getLocalCachedMap(key, options).get(field);
        return convertValue(value, clazz);
    }

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
    @Override
    public <T> T lcGet(String key, Object field, TypeReference<T> typeReference, LocalCachedMapOptions<Object, Object> options) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(field) || ObjectUtil.isNull(typeReference) || ObjectUtil.isNull(options)) {
            return null;
        }

        Object value = getLocalCachedMap(key, options).get(field);
        return convertValue(value, typeReference);
    }

    /**
     * 删除本地缓存 Map 字段。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param options 本地缓存配置
     * @return 删除前的值
     */
    @Override
    public Object lcRemove(String key, Object field, LocalCachedMapOptions<Object, Object> options) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(field) || ObjectUtil.isNull(options)) {
            return null;
        }

        return getLocalCachedMap(key, options).remove(field);
    }

    /**
     * 判断本地缓存 Map 字段是否存在。
     *
     * @param key     Redis 键
     * @param field   字段
     * @param options 本地缓存配置
     * @return 存在返回 true
     */
    @Override
    public boolean lcContainsKey(String key, Object field, LocalCachedMapOptions<Object, Object> options) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(field) || ObjectUtil.isNull(options)) {
            return false;
        }

        return getLocalCachedMap(key, options).containsKey(field);
    }

    /**
     * 获取本地缓存 Map 大小。
     *
     * @param key     Redis 键
     * @param options 本地缓存配置
     * @return 大小
     */
    @Override
    public int lcSize(String key, LocalCachedMapOptions<Object, Object> options) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(options)) {
            return 0;
        }

        return getLocalCachedMap(key, options).size();
    }

    /**
     * 清空本地缓存 Map。
     *
     * @param key     Redis 键
     * @param options 本地缓存配置
     */
    @Override
    public void lcClear(String key, LocalCachedMapOptions<Object, Object> options) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(options)) {
            return;
        }

        getLocalCachedMap(key, options).clear();
    }

    /**
     * 仅清空当前 JVM 内的本地缓存，不清空 Redis 远端数据。
     *
     * @param key     Redis 键
     * @param options 本地缓存配置
     */
    @Override
    public void lcClearLocalCache(String key, LocalCachedMapOptions<Object, Object> options) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(options)) {
            return;
        }

        getLocalCachedMap(key, options).clearLocalCache();
    }

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
    @Override
    public <T> RJsonBucket<T> getJsonBucket(String key, JsonCodec codec) {
        checkKey(key);
        Assert.notNull(codec, "JSON编解码器不能为空");
        return redissonClient.getJsonBucket(key, codec);
    }

    /**
     * 设置 JSON 文档。
     *
     * @param key   Redis 键
     * @param value JSON 对象
     * @param codec JSON 编解码器
     * @param <T>   值类型
     */
    @Override
    public <T> void jsonSet(String key, T value, JsonCodec codec) {
        checkKey(key);
        Assert.notNull(codec, "JSON编解码器不能为空");

        getJsonBucket(key, codec).set(value);
    }

    /**
     * 设置 JSON 文档并指定过期时间。
     *
     * @param key   Redis 键
     * @param value JSON 对象
     * @param ttl   过期时间
     * @param codec JSON 编解码器
     * @param <T>   值类型
     */
    @Override
    public <T> void jsonSet(String key, T value, Duration ttl, JsonCodec codec) {
        checkKey(key);
        checkPositiveDuration(ttl, "JSON过期时间不能为空且必须大于0");
        Assert.notNull(codec, "JSON编解码器不能为空");

        getJsonBucket(key, codec).set(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 获取完整 JSON 文档。
     *
     * @param key   Redis 键
     * @param codec JSON 编解码器
     * @param <T>   值类型
     * @return JSON 对象
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T jsonGet(String key, JsonCodec codec) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(codec)) {
            return null;
        }

        return (T) getJsonBucket(key, codec).get();
    }

    /**
     * 根据 JSONPath 获取 JSON 局部内容。
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param codec JSON 编解码器
     * @param <T>   返回类型
     * @return JSONPath 对应的值
     */
    @Override
    public <T> T jsonGet(String key, String path, JsonCodec codec) {
        if (StrUtil.hasBlank(key, path) || ObjectUtil.isNull(codec)) {
            return null;
        }

        return getJsonBucket(key, codec).get(codec, path);
    }

    /**
     * 根据 JSONPath 设置 JSON 局部内容。
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param value 值
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     */
    @Override
    public <T> void jsonSet(String key, String path, Object value, JsonCodec codec) {
        checkKey(key);
        Assert.isTrue(StrUtil.isNotBlank(path), "JSONPath不能为空");
        Assert.notNull(codec, "JSON编解码器不能为空");

        getJsonBucket(key, codec).set(path, value);
    }

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
    @Override
    public <T> boolean jsonSetIfAbsent(String key, String path, Object value, JsonCodec codec) {
        checkKey(key);
        Assert.isTrue(StrUtil.isNotBlank(path), "JSONPath不能为空");
        Assert.notNull(codec, "JSON编解码器不能为空");

        return getJsonBucket(key, codec).setIfAbsent(path, value);
    }

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
    @Override
    public <T> boolean jsonSetIfExists(String key, String path, Object value, JsonCodec codec) {
        checkKey(key);
        Assert.isTrue(StrUtil.isNotBlank(path), "JSONPath不能为空");
        Assert.notNull(codec, "JSON编解码器不能为空");

        return getJsonBucket(key, codec).setIfExists(path, value);
    }

    /**
     * 删除 JSONPath 对应内容。
     *
     * @param key   Redis 键
     * @param path  JSONPath
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     * @return 删除数量
     */
    @Override
    public <T> long jsonDelete(String key, String path, JsonCodec codec) {
        if (StrUtil.hasBlank(key, path) || ObjectUtil.isNull(codec)) {
            return 0L;
        }

        return getJsonBucket(key, codec).delete(path);
    }

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
    @Override
    public <T> long jsonArrayAppend(String key, String path, JsonCodec codec, Object... values) {
        checkKey(key);
        Assert.isTrue(StrUtil.isNotBlank(path), "JSONPath不能为空");
        Assert.notNull(codec, "JSON编解码器不能为空");

        if (ArrayUtil.isEmpty(values)) {
            return 0L;
        }

        return getJsonBucket(key, codec).arrayAppend(path, values);
    }

    /**
     * 获取 JSON 对象字段名。
     *
     * @param key   Redis 键
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     * @return 字段名集合
     */
    @Override
    public <T> List<String> jsonKeys(String key, JsonCodec codec) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(codec)) {
            return Collections.emptyList();
        }

        return getJsonBucket(key, codec).getKeys();
    }

    /**
     * 清空 JSON 文档。
     *
     * @param key   Redis 键
     * @param codec JSON 编解码器
     * @param <T>   JSON 文档类型
     */
    @Override
    public <T> void jsonClear(String key, JsonCodec codec) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(codec)) {
            return;
        }

        getJsonBucket(key, codec).clear();
    }

    // -------------------------------------------------------------------------
    // BinaryStream / 二进制流
    // -------------------------------------------------------------------------

    /**
     * 获取二进制流对象。
     *
     * @param key Redis 键
     * @return RBinaryStream
     */
    @Override
    public RBinaryStream getBinaryStream(String key) {
        checkKey(key);
        return redissonClient.getBinaryStream(key);
    }

    /**
     * 获取二进制输入流。
     *
     * @param key Redis 键
     * @return InputStream
     */
    @Override
    public InputStream binaryInputStream(String key) {
        return getBinaryStream(key).getInputStream();
    }

    /**
     * 获取二进制输出流。
     *
     * @param key Redis 键
     * @return OutputStream
     */
    @Override
    public OutputStream binaryOutputStream(String key) {
        return getBinaryStream(key).getOutputStream();
    }

    /**
     * 写入二进制数据。
     *
     * @param key  Redis 键
     * @param data 字节数组
     */
    @Override
    public void binaryWrite(String key, byte[] data) {
        checkKey(key);
        Assert.notNull(data, "二进制数据不能为空");

        getBinaryStream(key).set(data);
    }

    /**
     * 读取全部二进制数据。
     *
     * @param key Redis 键
     * @return 字节数组
     */
    @Override
    public byte[] binaryReadAll(String key) {
        if (StrUtil.isBlank(key)) {
            return new byte[0];
        }

        byte[] data = getBinaryStream(key).get();
        return ObjectUtil.defaultIfNull(data, new byte[0]);
    }

    /**
     * 获取二进制数据大小。
     *
     * @param key Redis 键
     * @return 字节大小
     */
    @Override
    public long binarySize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return getBinaryStream(key).size();
    }

    /**
     * 删除二进制数据。
     *
     * @param key Redis 键
     * @return 是否删除成功
     */
    @Override
    public boolean binaryDelete(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return getBinaryStream(key).delete();
    }

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
    @Override
    public <K, V> RSetMultimap<K, V> getSetMultimap(String key) {
        checkKey(key);
        return redissonClient.getSetMultimap(key);
    }

    /**
     * 获取 ListMultimap。
     *
     * @param key Redis 键
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RListMultimap
     */
    @Override
    public <K, V> RListMultimap<K, V> getListMultimap(String key) {
        checkKey(key);
        return redissonClient.getListMultimap(key);
    }

    /**
     * 向 SetMultimap 添加值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否新增
     */
    @Override
    public boolean smmPut(String key, Object mapKey, Object mapValue) {
        checkKey(key);
        Assert.notNull(mapKey, "Multimap字段不能为空");

        return redissonClient.<Object, Object>getSetMultimap(key).put(mapKey, mapValue);
    }

    /**
     * 获取 SetMultimap 指定字段的值集合。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 值集合
     */
    @Override
    public Set<Object> smmGet(String key, Object mapKey) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object, Object>getSetMultimap(key).getAll(mapKey);
    }

    /**
     * 删除 SetMultimap 指定字段的指定值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否删除成功
     */
    @Override
    public boolean smmRemove(String key, Object mapKey, Object mapValue) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return false;
        }

        return redissonClient.<Object, Object>getSetMultimap(key).remove(mapKey, mapValue);
    }

    /**
     * 删除 SetMultimap 指定字段的全部值。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 删除的值集合
     */
    @Override
    public Set<Object> smmRemoveAll(String key, Object mapKey) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return Collections.emptySet();
        }

        return redissonClient.<Object, Object>getSetMultimap(key).removeAll(mapKey);
    }

    /**
     * 判断 SetMultimap 是否包含指定字段。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 存在返回 true
     */
    @Override
    public boolean smmContainsKey(String key, Object mapKey) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return false;
        }

        return redissonClient.<Object, Object>getSetMultimap(key).containsKey(mapKey);
    }

    /**
     * 判断 SetMultimap 是否包含指定字段和值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 存在返回 true
     */
    @Override
    public boolean smmContainsEntry(String key, Object mapKey, Object mapValue) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return false;
        }

        return redissonClient.<Object, Object>getSetMultimap(key).containsEntry(mapKey, mapValue);
    }

    /**
     * 获取 SetMultimap 总值数量。
     *
     * @param key Redis 键
     * @return 总值数量
     */
    @Override
    public int smmSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.<Object, Object>getSetMultimap(key).size();
    }

    /**
     * 清空 SetMultimap。
     *
     * @param key Redis 键
     */
    @Override
    public void smmClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.<Object, Object>getSetMultimap(key).clear();
    }

    /**
     * 向 ListMultimap 添加值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否新增
     */
    @Override
    public boolean lmmPut(String key, Object mapKey, Object mapValue) {
        checkKey(key);
        Assert.notNull(mapKey, "Multimap字段不能为空");

        return redissonClient.<Object, Object>getListMultimap(key).put(mapKey, mapValue);
    }

    /**
     * 获取 ListMultimap 指定字段的值列表。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 值列表
     */
    @Override
    public List<Object> lmmGet(String key, Object mapKey) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return Collections.emptyList();
        }

        return redissonClient.<Object, Object>getListMultimap(key).getAll(mapKey);
    }

    /**
     * 删除 ListMultimap 指定字段的指定值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 是否删除成功
     */
    @Override
    public boolean lmmRemove(String key, Object mapKey, Object mapValue) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return false;
        }

        return redissonClient.<Object, Object>getListMultimap(key).remove(mapKey, mapValue);
    }

    /**
     * 删除 ListMultimap 指定字段的全部值。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 删除的值列表
     */
    @Override
    public List<Object> lmmRemoveAll(String key, Object mapKey) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return Collections.emptyList();
        }

        return redissonClient.<Object, Object>getListMultimap(key).removeAll(mapKey);
    }

    /**
     * 判断 ListMultimap 是否包含指定字段。
     *
     * @param key    Redis 键
     * @param mapKey Multimap 字段
     * @return 存在返回 true
     */
    @Override
    public boolean lmmContainsKey(String key, Object mapKey) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return false;
        }

        return redissonClient.<Object, Object>getListMultimap(key).containsKey(mapKey);
    }

    /**
     * 判断 ListMultimap 是否包含指定字段和值。
     *
     * @param key      Redis 键
     * @param mapKey   Multimap 字段
     * @param mapValue Multimap 值
     * @return 存在返回 true
     */
    @Override
    public boolean lmmContainsEntry(String key, Object mapKey, Object mapValue) {
        if (StrUtil.isBlank(key) || ObjectUtil.isNull(mapKey)) {
            return false;
        }

        return redissonClient.<Object, Object>getListMultimap(key).containsEntry(mapKey, mapValue);
    }

    /**
     * 获取 ListMultimap 总值数量。
     *
     * @param key Redis 键
     * @return 总值数量
     */
    @Override
    public int lmmSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.<Object, Object>getListMultimap(key).size();
    }

    /**
     * 清空 ListMultimap。
     *
     * @param key Redis 键
     */
    @Override
    public void lmmClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.<Object, Object>getListMultimap(key).clear();
    }

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
    @Override
    public <T> RSortedSet<T> getSortedSet(String key) {
        checkKey(key);
        return redissonClient.getSortedSet(key);
    }

    /**
     * 获取字典序排序集合。
     *
     * @param key Redis 键
     * @return RLexSortedSet
     */
    @Override
    public RLexSortedSet getLexSortedSet(String key) {
        checkKey(key);
        return redissonClient.getLexSortedSet(key);
    }

    /**
     * 添加自然排序集合元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    @Override
    public boolean sortedSetAdd(String key, Object value) {
        checkKey(key);
        Assert.notNull(value, "自然排序集合元素不能为空");

        return redissonClient.<Object>getSortedSet(key).add(value);
    }

    /**
     * 批量添加自然排序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @Override
    public boolean sortedSetAddAll(String key, Collection<?> values) {
        checkKey(key);
        if (CollUtil.isEmpty(values)) {
            return false;
        }

        Collection<Object> valueList = new ArrayList<>(values.size());
        for (Object value : values) {
            if (ObjectUtil.isNotNull(value)) {
                valueList.add(value);
            }
        }

        if (CollUtil.isEmpty(valueList)) {
            return false;
        }

        return redissonClient.<Object>getSortedSet(key).addAll(valueList);
    }

    /**
     * 获取自然排序集合全部元素。
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @Override
    public Collection<Object> sortedSetReadAll(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyList();
        }

        return new ArrayList<>(redissonClient.<Object>getSortedSet(key));
    }

    /**
     * 删除自然排序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    @Override
    public boolean sortedSetRemove(String key, Object... values) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(values)) {
            return false;
        }

        return redissonClient.<Object>getSortedSet(key).removeAll(Arrays.asList(values));
    }

    /**
     * 获取自然排序集合大小。
     *
     * @param key Redis 键
     * @return 大小
     */
    @Override
    public int sortedSetSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.<Object>getSortedSet(key).size();
    }

    /**
     * 清空自然排序集合。
     *
     * @param key Redis 键
     */
    @Override
    public void sortedSetClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.<Object>getSortedSet(key).clear();
    }

    /**
     * 添加字典序排序集合元素。
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否新增
     */
    @Override
    public boolean lexAdd(String key, String value) {
        checkKey(key);
        Assert.isTrue(StrUtil.isNotBlank(value), "字典序集合元素不能为空");

        return redissonClient.getLexSortedSet(key).add(value);
    }

    /**
     * 批量添加字典序排序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否有新增
     */
    @Override
    public boolean lexAddAll(String key, Collection<String> values) {
        checkKey(key);
        if (CollUtil.isEmpty(values)) {
            return false;
        }

        List<String> valueList = values.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();

        if (CollUtil.isEmpty(valueList)) {
            return false;
        }

        return redissonClient.getLexSortedSet(key).addAll(valueList);
    }

    /**
     * 获取字典序排序集合全部元素。
     *
     * @param key Redis 键
     * @return 元素集合
     */
    @Override
    public Collection<String> lexReadAll(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyList();
        }

        return new ArrayList<>(redissonClient.getLexSortedSet(key));
    }

    /**
     * 获取大于等于指定元素的字典序集合。
     *
     * @param key  Redis 键
     * @param from 开始元素
     * @return 元素集合
     */
    @Override
    public Collection<String> lexRangeTail(String key, String from) {
        if (StrUtil.hasBlank(key, from)) {
            return Collections.emptyList();
        }

        return redissonClient.getLexSortedSet(key)
                .rangeTail(from, true, 0, Integer.MAX_VALUE);
    }

    /**
     * 获取小于等于指定元素的字典序集合。
     *
     * @param key Redis 键
     * @param to  结束元素
     * @return 元素集合
     */
    @Override
    public Collection<String> lexRangeHead(String key, String to) {
        if (StrUtil.hasBlank(key, to)) {
            return Collections.emptyList();
        }

        return redissonClient.getLexSortedSet(key)
                .rangeHead(to, true, 0, Integer.MAX_VALUE);
    }

    /**
     * 删除字典序排序集合元素。
     *
     * @param key    Redis 键
     * @param values 元素
     * @return 是否删除成功
     */
    @Override
    public boolean lexRemove(String key, String... values) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(values)) {
            return false;
        }

        List<String> valueList = Arrays.stream(values)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();

        if (CollUtil.isEmpty(valueList)) {
            return false;
        }

        return redissonClient.getLexSortedSet(key).removeAll(valueList);
    }

    /**
     * 获取字典序排序集合大小。
     *
     * @param key Redis 键
     * @return 大小
     */
    @Override
    public int lexSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.getLexSortedSet(key).size();
    }

    /**
     * 清空字典序排序集合。
     *
     * @param key Redis 键
     */
    @Override
    public void lexClear(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getLexSortedSet(key).clear();
    }

    // -------------------------------------------------------------------------
    // LongAdder / DoubleAdder
    // -------------------------------------------------------------------------

    /**
     * 获取分布式 LongAdder。
     *
     * @param key Redis 键
     * @return RLongAdder
     */
    @Override
    public RLongAdder getLongAdder(String key) {
        checkKey(key);
        return redissonClient.getLongAdder(key);
    }

    /**
     * 获取分布式 DoubleAdder。
     *
     * @param key Redis 键
     * @return RDoubleAdder
     */
    @Override
    public RDoubleAdder getDoubleAdder(String key) {
        checkKey(key);
        return redissonClient.getDoubleAdder(key);
    }

    /**
     * LongAdder 增加。
     *
     * @param key   Redis 键
     * @param delta 增量
     */
    @Override
    public void longAdderAdd(String key, long delta) {
        checkKey(key);
        redissonClient.getLongAdder(key).add(delta);
    }

    /**
     * LongAdder 求和。
     *
     * @param key Redis 键
     * @return 当前总和
     */
    @Override
    public long longAdderSum(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        return redissonClient.getLongAdder(key).sum();
    }

    /**
     * LongAdder 重置。
     *
     * @param key Redis 键
     */
    @Override
    public void longAdderReset(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getLongAdder(key).reset();
    }

    /**
     * LongAdder 求和后重置。
     * 注意：Redisson RLongAdder 无原生 sumThenReset，同步语义为先 sum 再 reset。
     *
     * @param key Redis 键
     * @return 重置前总和
     */
    @Override
    public long longAdderSumThenReset(String key) {
        if (StrUtil.isBlank(key)) {
            return 0L;
        }

        RLongAdder adder = redissonClient.getLongAdder(key);
        long value = adder.sum();
        adder.reset();
        return value;
    }

    /**
     * DoubleAdder 增加。
     *
     * @param key   Redis 键
     * @param delta 增量
     */
    @Override
    public void doubleAdderAdd(String key, double delta) {
        checkKey(key);
        redissonClient.getDoubleAdder(key).add(delta);
    }

    /**
     * DoubleAdder 求和。
     *
     * @param key Redis 键
     * @return 当前总和
     */
    @Override
    public double doubleAdderSum(String key) {
        if (StrUtil.isBlank(key)) {
            return 0D;
        }

        return redissonClient.getDoubleAdder(key).sum();
    }

    /**
     * DoubleAdder 重置。
     *
     * @param key Redis 键
     */
    @Override
    public void doubleAdderReset(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getDoubleAdder(key).reset();
    }

    /**
     * DoubleAdder 求和后重置。
     * 注意：Redisson RDoubleAdder 无原生 sumThenReset，同步语义为先 sum 再 reset。
     *
     * @param key Redis 键
     * @return 重置前总和
     */
    @Override
    public double doubleAdderSumThenReset(String key) {
        if (StrUtil.isBlank(key)) {
            return 0D;
        }

        RDoubleAdder adder = redissonClient.getDoubleAdder(key);
        double value = adder.sum();
        adder.reset();
        return value;
    }

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
    @Override
    public <T> RBoundedBlockingQueue<T> getBoundedBlockingQueue(String key) {
        checkKey(key);
        return redissonClient.getBoundedBlockingQueue(key);
    }

    /**
     * 初始化有界阻塞队列容量。
     *
     * @param key      Redis 键
     * @param capacity 容量
     * @return 是否初始化成功
     */
    @Override
    public boolean boundedQueueTrySetCapacity(String key, int capacity) {
        checkKey(key);
        Assert.isTrue(capacity > 0, "有界阻塞队列容量必须大于0");

        return redissonClient.getBoundedBlockingQueue(key).trySetCapacity(capacity);
    }

    /**
     * 有界阻塞队列入队。
     *
     * @param key   Redis 键
     * @param value 元素
     * @param <T>   元素类型
     * @return 是否入队成功
     */
    @Override
    public <T> boolean boundedQueueOffer(String key, T value) {
        checkKey(key);

        return redissonClient.<T>getBoundedBlockingQueue(key).offer(value);
    }

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
    @Override
    public <T> boolean boundedQueueOffer(String key, T value, long timeout, TimeUnit unit) throws InterruptedException {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于0");

        return redissonClient.<T>getBoundedBlockingQueue(key).offer(value, timeout, unit);
    }

    /**
     * 有界阻塞队列出队。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return 元素
     */
    @Override
    public <T> T boundedQueuePoll(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<T>getBoundedBlockingQueue(key).poll();
    }

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
    @Override
    public <T> T boundedQueuePoll(String key, long timeout, TimeUnit unit) throws InterruptedException {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(timeout >= 0, "等待时间不能小于0");

        return redissonClient.<T>getBoundedBlockingQueue(key).poll(timeout, unit);
    }

    /**
     * 获取有界阻塞队列大小。
     *
     * @param key Redis 键
     * @return 队列大小
     */
    @Override
    public int boundedQueueSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.getBoundedBlockingQueue(key).size();
    }

    /**
     * 获取有界阻塞队列剩余容量。
     *
     * @param key Redis 键
     * @return 剩余容量
     */
    @Override
    public int boundedQueueRemainingCapacity(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.getBoundedBlockingQueue(key).remainingCapacity();
    }

    /**
     * 获取优先级双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityDeque
     */
    @Override
    public <T> RPriorityDeque<T> getPriorityDeque(String key) {
        checkKey(key);
        return redissonClient.getPriorityDeque(key);
    }

    /**
     * 获取优先级阻塞队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityBlockingQueue
     */
    @Override
    public <T> RPriorityBlockingQueue<T> getPriorityBlockingQueue(String key) {
        checkKey(key);
        return redissonClient.getPriorityBlockingQueue(key);
    }

    /**
     * 获取优先级阻塞双端队列。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return RPriorityBlockingDeque
     */
    @Override
    public <T> RPriorityBlockingDeque<T> getPriorityBlockingDeque(String key) {
        checkKey(key);
        return redissonClient.getPriorityBlockingDeque(key);
    }

    /**
     * 优先级队列入队。
     *
     * @param key   Redis 键
     * @param value 元素，建议实现 Comparable
     * @param <T>   元素类型
     * @return 是否入队成功
     */
    @Override
    public <T> boolean priorityQueueOffer(String key, T value) {
        checkKey(key);

        return redissonClient.<T>getPriorityQueue(key).offer(value);
    }

    /**
     * 优先级队列出队。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return 元素
     */
    @Override
    public <T> T priorityQueuePoll(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<T>getPriorityQueue(key).poll();
    }

    /**
     * 优先级双端队列从头部入队。
     *
     * @param key   Redis 键
     * @param value 元素，建议实现 Comparable
     * @param <T>   元素类型
     * @return 是否入队成功
     */
    @Override
    public <T> boolean priorityDequeOfferFirst(String key, T value) {
        checkKey(key);

        return redissonClient.<T>getPriorityDeque(key).offerFirst(value);
    }

    /**
     * 优先级双端队列从尾部入队。
     *
     * @param key   Redis 键
     * @param value 元素，建议实现 Comparable
     * @param <T>   元素类型
     * @return 是否入队成功
     */
    @Override
    public <T> boolean priorityDequeOfferLast(String key, T value) {
        checkKey(key);

        return redissonClient.<T>getPriorityDeque(key).offerLast(value);
    }

    /**
     * 优先级双端队列从头部出队。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return 元素
     */
    @Override
    public <T> T priorityDequePollFirst(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<T>getPriorityDeque(key).pollFirst();
    }

    /**
     * 优先级双端队列从尾部出队。
     *
     * @param key Redis 键
     * @param <T> 元素类型
     * @return 元素
     */
    @Override
    public <T> T priorityDequePollLast(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<T>getPriorityDeque(key).pollLast();
    }


    // -------------------------------------------------------------------------
    // ReliableQueue / 可靠队列
    // -------------------------------------------------------------------------

    /**
     * 设置可靠队列配置。
     *
     * @param key    队列 key
     * @param config 队列配置
     */
    @Override
    public void reliableQueueSetConfig(String key, QueueConfig config) {
        checkKey(key);
        Assert.notNull(config, "可靠队列配置不能为空");

        redissonClient.getReliableQueue(key).setConfig(config);
        log.info("Redis 可靠队列配置已设置，key={}", key);
    }

    /**
     * 队列配置不存在时设置可靠队列配置。
     *
     * @param key    队列 key
     * @param config 队列配置
     * @return 是否设置成功
     */
    @Override
    public boolean reliableQueueSetConfigIfAbsent(String key, QueueConfig config) {
        checkKey(key);
        Assert.notNull(config, "可靠队列配置不能为空");

        return redissonClient.getReliableQueue(key).setConfigIfAbsent(config);
    }

    /**
     * 可靠队列添加消息。
     *
     * @param key  队列 key
     * @param args 添加参数
     * @param <T>  消息类型
     * @return 添加后的消息
     */
    @Override
    public <T> Message<T> reliableQueueAdd(String key, QueueAddArgs<T> args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列添加参数不能为空");

        return redissonClient.<T>getReliableQueue(key).add(args);
    }

    /**
     * 可靠队列批量添加消息。
     *
     * @param key  队列 key
     * @param args 添加参数
     * @param <T>  消息类型
     * @return 添加后的消息集合
     */
    @Override
    public <T> List<Message<T>> reliableQueueAddMany(String key, QueueAddArgs<T> args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列添加参数不能为空");

        return redissonClient.<T>getReliableQueue(key).addMany(args);
    }

    /**
     * 可靠队列拉取一条消息。
     *
     * @param key 队列 key
     * @param <T> 消息类型
     * @return 消息
     */
    @Override
    public <T> Message<T> reliableQueuePoll(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return redissonClient.<T>getReliableQueue(key).poll();
    }

    /**
     * 可靠队列按参数拉取一条消息。
     *
     * @param key  队列 key
     * @param args 拉取参数
     * @param <T>  消息类型
     * @return 消息
     */
    @Override
    public <T> Message<T> reliableQueuePoll(String key, QueuePollArgs args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列拉取参数不能为空");

        return redissonClient.<T>getReliableQueue(key).poll(args);
    }

    /**
     * 可靠队列批量拉取消息。
     *
     * @param key  队列 key
     * @param args 拉取参数
     * @param <T>  消息类型
     * @return 消息集合
     */
    @Override
    public <T> List<Message<T>> reliableQueuePollMany(String key, QueuePollArgs args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列拉取参数不能为空");

        return redissonClient.<T>getReliableQueue(key).pollMany(args);
    }

    /**
     * 确认可靠队列消息处理成功。
     *
     * @param key  队列 key
     * @param args ACK 参数
     */
    @Override
    public void reliableQueueAck(String key, QueueAckArgs args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列ACK参数不能为空");

        redissonClient.getReliableQueue(key).acknowledge(args);
    }

    /**
     * 标记可靠队列消息处理失败。
     *
     * @param key  队列 key
     * @param args NACK 参数
     */
    @Override
    public void reliableQueueNack(String key, QueueNegativeAckArgs args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列NACK参数不能为空");

        redissonClient.getReliableQueue(key).negativeAcknowledge(args);
    }

    /**
     * 根据消息 ID 获取可靠队列消息。
     *
     * @param key 队列 key
     * @param id  消息 ID
     * @param <T> 消息类型
     * @return 消息
     */
    @Override
    public <T> Message<T> reliableQueueGet(String key, String id) {
        if (StrUtil.hasBlank(key, id)) {
            return null;
        }

        return redissonClient.<T>getReliableQueue(key).get(id);
    }

    /**
     * 根据消息 ID 批量获取可靠队列消息。
     *
     * @param key 队列 key
     * @param ids 消息 ID
     * @param <T> 消息类型
     * @return 消息集合
     */
    @Override
    public <T> List<Message<T>> reliableQueueGetAll(String key, String... ids) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }

        String[] idArray = Arrays.stream(ids)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(idArray)) {
            return Collections.emptyList();
        }

        return redissonClient.<T>getReliableQueue(key).getAll(idArray);
    }

    /**
     * 获取可靠队列所有可拉取消息。
     *
     * @param key 队列 key
     * @param <T> 消息类型
     * @return 消息集合
     */
    @Override
    public <T> List<Message<T>> reliableQueueListAll(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptyList();
        }

        return redissonClient.<T>getReliableQueue(key).listAll();
    }

    /**
     * 判断可靠队列是否包含指定消息 ID。
     *
     * @param key 队列 key
     * @param id  消息 ID
     * @return 包含返回 true
     */
    @Override
    public boolean reliableQueueContains(String key, String id) {
        if (StrUtil.hasBlank(key, id)) {
            return false;
        }

        return redissonClient.getReliableQueue(key).contains(id);
    }

    /**
     * 判断可靠队列包含的消息 ID 数量。
     *
     * @param key 队列 key
     * @param ids 消息 ID
     * @return 匹配数量
     */
    @Override
    public int reliableQueueContainsMany(String key, String... ids) {
        if (StrUtil.isBlank(key) || ArrayUtil.isEmpty(ids)) {
            return 0;
        }

        String[] idArray = Arrays.stream(ids)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        if (ArrayUtil.isEmpty(idArray)) {
            return 0;
        }

        return redissonClient.getReliableQueue(key).containsMany(idArray);
    }

    /**
     * 删除可靠队列消息。
     *
     * @param key  队列 key
     * @param args 删除参数
     * @return 是否删除成功
     */
    @Override
    public boolean reliableQueueRemove(String key, QueueRemoveArgs args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列删除参数不能为空");

        return redissonClient.getReliableQueue(key).remove(args);
    }

    /**
     * 批量删除可靠队列消息。
     *
     * @param key  队列 key
     * @param args 删除参数
     * @return 删除数量
     */
    @Override
    public int reliableQueueRemoveMany(String key, QueueRemoveArgs args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列删除参数不能为空");

        return redissonClient.getReliableQueue(key).removeMany(args);
    }

    /**
     * 移动可靠队列消息。
     *
     * @param key  队列 key
     * @param args 移动参数
     * @return 移动数量
     */
    @Override
    public int reliableQueueMove(String key, QueueMoveArgs args) {
        checkKey(key);
        Assert.notNull(args, "可靠队列移动参数不能为空");

        return redissonClient.getReliableQueue(key).move(args);
    }

    /**
     * 获取可靠队列消息数量。
     *
     * @param key 队列 key
     * @return 消息数量
     */
    @Override
    public int reliableQueueSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.getReliableQueue(key).size();
    }

    /**
     * 获取可靠队列延迟消息数量。
     *
     * @param key 队列 key
     * @return 延迟消息数量
     */
    @Override
    public int reliableQueueDelayedSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.getReliableQueue(key).countDelayedMessages();
    }

    /**
     * 获取可靠队列未确认消息数量。
     *
     * @param key 队列 key
     * @return 未确认消息数量
     */
    @Override
    public int reliableQueueUnacknowledgedSize(String key) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        return redissonClient.getReliableQueue(key).countUnacknowledgedMessages();
    }

    /**
     * 清空可靠队列全部状态消息。
     *
     * @param key 队列 key
     * @return 是否清空成功
     */
    @Override
    public boolean reliableQueueClear(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }

        return redissonClient.getReliableQueue(key).clear();
    }

    /**
     * 获取将当前队列作为死信队列的源队列名称。
     *
     * @param key 队列 key
     * @return 源队列名称集合
     */
    @Override
    public Set<String> reliableQueueDeadLetterSources(String key) {
        if (StrUtil.isBlank(key)) {
            return Collections.emptySet();
        }

        return redissonClient.getReliableQueue(key).getDeadLetterQueueSources();
    }

    /**
     * 添加可靠队列事件监听器。
     *
     * @param key      队列 key
     * @param listener 监听器
     * @return 监听器 ID
     */
    @Override
    public String reliableQueueAddListener(String key, QueueEventListener listener) {
        checkKey(key);
        Assert.notNull(listener, "可靠队列监听器不能为空");

        return redissonClient.getReliableQueue(key).addListener(listener);
    }

    /**
     * 移除可靠队列事件监听器。
     *
     * @param key        队列 key
     * @param listenerId 监听器 ID
     */
    @Override
    public void reliableQueueRemoveListener(String key, String listenerId) {
        if (StrUtil.hasBlank(key, listenerId)) {
            return;
        }

        redissonClient.getReliableQueue(key).removeListener(listenerId);
    }

    /**
     * 启用可靠队列指定操作。
     *
     * @param key       队列 key
     * @param operation 队列操作
     */
    @Override
    public void reliableQueueEnableOperation(String key, QueueOperation operation) {
        checkKey(key);
        Assert.notNull(operation, "可靠队列操作不能为空");

        redissonClient.getReliableQueue(key).enableOperation(operation);
    }

    /**
     * 禁用可靠队列指定操作。
     *
     * @param key       队列 key
     * @param operation 队列操作
     */
    @Override
    public void reliableQueueDisableOperation(String key, QueueOperation operation) {
        checkKey(key);
        Assert.notNull(operation, "可靠队列操作不能为空");

        redissonClient.getReliableQueue(key).disableOperation(operation);
    }

    // -------------------------------------------------------------------------
    // Stream / 高级消费治理
    // -------------------------------------------------------------------------

    /**
     * 获取 Stream 详细信息。
     *
     * @param streamKey Stream key
     * @return Stream 信息
     */
    @Override
    public StreamInfo<Object, Object> streamInfo(String streamKey) {
        checkKey(streamKey);

        return redissonClient.<Object, Object>getStream(streamKey).getInfo();
    }

    /**
     * 获取 Stream 消费组列表。
     *
     * @param streamKey Stream key
     * @return 消费组列表
     */
    @Override
    public List<StreamGroup> streamListGroups(String streamKey) {
        if (StrUtil.isBlank(streamKey)) {
            return Collections.emptyList();
        }

        return redissonClient.<Object, Object>getStream(streamKey).listGroups();
    }

    /**
     * 获取 Stream 指定消费组的消费者列表。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 消费者列表
     */
    @Override
    public List<StreamConsumer> streamListConsumers(String streamKey, String groupName) {
        if (StrUtil.hasBlank(streamKey, groupName)) {
            return Collections.emptyList();
        }

        return redissonClient.<Object, Object>getStream(streamKey).listConsumers(groupName);
    }

    /**
     * 创建 Stream 消费者。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     */
    @Override
    public void streamCreateConsumer(String streamKey, String groupName, String consumerName) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);

        redissonClient.<Object, Object>getStream(streamKey).createConsumer(groupName, consumerName);
    }

    /**
     * 删除 Stream 消费者。
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @return 该消费者名下的待处理消息数量
     */
    @Override
    public long streamRemoveConsumer(String streamKey, String groupName, String consumerName) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);

        return redissonClient.<Object, Object>getStream(streamKey).removeConsumer(groupName, consumerName);
    }

    /**
     * 删除 Stream 消费组。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     */
    @Override
    public void streamRemoveGroup(String streamKey, String groupName) {
        checkKey(streamKey);
        checkKey(groupName);

        redissonClient.<Object, Object>getStream(streamKey).removeGroup(groupName);
    }

    /**
     * 更新 Stream 消费组读取起始 ID。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param id        消息 ID
     */
    @Override
    public void streamUpdateGroupMessageId(String streamKey, String groupName, StreamMessageId id) {
        checkKey(streamKey);
        checkKey(groupName);
        Assert.notNull(id, "Stream消息ID不能为空");

        redissonClient.<Object, Object>getStream(streamKey).updateGroupMessageId(groupName, id);
    }

    /**
     * 获取 Stream 消费组待处理消息概要。
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 待处理概要
     */
    @Override
    public PendingResult streamPendingInfo(String streamKey, String groupName) {
        checkKey(streamKey);
        checkKey(groupName);

        return redissonClient.<Object, Object>getStream(streamKey).getPendingInfo(groupName);
    }

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
    @Override
    public List<PendingEntry> streamListPending(String streamKey, String groupName, StreamMessageId startId, StreamMessageId endId, int count) {
        checkKey(streamKey);
        checkKey(groupName);
        checkStreamRangeArgs(startId, endId, count);

        return redissonClient.<Object, Object>getStream(streamKey).listPending(groupName, startId, endId, count);
    }

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
    @Override
    public List<PendingEntry> streamListPending(String streamKey, String groupName, String consumerName, StreamMessageId startId, StreamMessageId endId, int count) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        checkStreamRangeArgs(startId, endId, count);

        return redissonClient.<Object, Object>getStream(streamKey).listPending(groupName, consumerName, startId, endId, count);
    }

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
    @Override
    public List<PendingEntry> streamListPending(String streamKey, String groupName, String consumerName,
                                                StreamMessageId startId, StreamMessageId endId,
                                                long idleTime, TimeUnit unit, int count) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        checkStreamRangeArgs(startId, endId, count);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(idleTime >= 0, "最小空闲时间不能小于0");

        return redissonClient.<Object, Object>getStream(streamKey)
                .listPending(groupName, consumerName, startId, endId, idleTime, unit, count);
    }

    /**
     * 按 ID 范围读取 Stream 消息。
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @return 消息 Map
     */
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamRange(String streamKey, StreamMessageId startId, StreamMessageId endId) {
        checkKey(streamKey);
        Assert.notNull(startId, "开始消息ID不能为空");
        Assert.notNull(endId, "结束消息ID不能为空");

        return redissonClient.<Object, Object>getStream(streamKey).range(startId, endId);
    }

    /**
     * 按 ID 范围读取 Stream 消息并限制数量。
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 消息 Map
     */
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamRange(String streamKey, StreamMessageId startId, StreamMessageId endId, int count) {
        checkKey(streamKey);
        checkStreamRangeArgs(startId, endId, count);

        return redissonClient.<Object, Object>getStream(streamKey).range(count, startId, endId);
    }

    /**
     * 按 ID 范围倒序读取 Stream 消息。
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @return 消息 Map
     */
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamRangeReversed(String streamKey, StreamMessageId startId, StreamMessageId endId) {
        checkKey(streamKey);
        Assert.notNull(startId, "开始消息ID不能为空");
        Assert.notNull(endId, "结束消息ID不能为空");

        return redissonClient.<Object, Object>getStream(streamKey).rangeReversed(startId, endId);
    }

    /**
     * 按 ID 范围倒序读取 Stream 消息并限制数量。
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 消息 Map
     */
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamRangeReversed(String streamKey, StreamMessageId startId, StreamMessageId endId, int count) {
        checkKey(streamKey);
        checkStreamRangeArgs(startId, endId, count);

        return redissonClient.<Object, Object>getStream(streamKey).rangeReversed(count, startId, endId);
    }

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
    @Override
    public Map<StreamMessageId, Map<Object, Object>> streamClaim(String streamKey, String groupName, String consumerName,
                                                                 long idleTime, TimeUnit unit, StreamMessageId... ids) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(idleTime >= 0, "最小空闲时间不能小于0");

        if (ArrayUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }

        return redissonClient.<Object, Object>getStream(streamKey)
                .claim(groupName, consumerName, idleTime, unit, ids);
    }

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
    @Override
    public AutoClaimResult<Object, Object> streamAutoClaim(String streamKey, String groupName, String consumerName,
                                                           long idleTime, TimeUnit unit, StreamMessageId startId, int count) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        Assert.notNull(unit, "时间单位不能为空");
        Assert.notNull(startId, "起始消息ID不能为空");
        Assert.isTrue(idleTime >= 0, "最小空闲时间不能小于0");
        Assert.isTrue(count > 0, "读取数量必须大于0");

        return redissonClient.<Object, Object>getStream(streamKey)
                .autoClaim(groupName, consumerName, idleTime, unit, startId, count);
    }

    /**
     * 裁剪 Stream。
     *
     * @param streamKey Stream key
     * @param args      裁剪参数
     * @return 裁剪数量
     */
    @Override
    public long streamTrim(String streamKey, StreamTrimArgs args) {
        checkKey(streamKey);
        Assert.notNull(args, "Stream裁剪参数不能为空");

        return redissonClient.<Object, Object>getStream(streamKey).trim(args);
    }

    /**
     * 添加 Stream 对象监听器。
     *
     * @param streamKey Stream key
     * @param listener  对象监听器
     * @return 监听器 ID
     */
    @Override
    public int streamAddListener(String streamKey, ObjectListener listener) {
        checkKey(streamKey);
        Assert.notNull(listener, "Stream监听器不能为空");

        return redissonClient.<Object, Object>getStream(streamKey).addListener(listener);
    }

    /**
     * 移除 Stream 对象监听器。
     *
     * @param streamKey  Stream key
     * @param listenerId 监听器 ID
     */
    @Override
    public void streamRemoveListener(String streamKey, int listenerId) {
        if (StrUtil.isBlank(streamKey)) {
            return;
        }

        redissonClient.<Object, Object>getStream(streamKey).removeListener(listenerId);
    }

    // -------------------------------------------------------------------------
    // Executor / Scheduler 分布式任务
    // -------------------------------------------------------------------------

    /**
     * 获取分布式执行器。
     *
     * @param name 执行器名称
     * @return RExecutorService
     */
    @Override
    public RExecutorService getExecutorService(String name) {
        checkKey(name);
        return redissonClient.getExecutorService(name);
    }

    /**
     * 获取分布式定时执行器。
     *
     * @param name 执行器名称
     * @return RScheduledExecutorService
     */
    @Override
    public RScheduledExecutorService getScheduledExecutorService(String name) {
        checkKey(name);

        // Redisson 3.52.0 中 getExecutorService 返回 RScheduledExecutorService，可同时作为普通执行器和定时执行器使用
        return redissonClient.getExecutorService(name);
    }

    /**
     * 执行 Runnable 分布式任务。
     *
     * @param name 执行器名称
     * @param task 任务
     */
    @Override
    public void executorExecute(String name, Runnable task) {
        checkKey(name);
        Assert.notNull(task, "分布式任务不能为空");

        redissonClient.getExecutorService(name).execute(task);
    }

    /**
     * 提交 Runnable 分布式任务。
     *
     * @param name 执行器名称
     * @param task 任务
     * @return Future
     */
    @Override
    public Future<?> executorSubmit(String name, Runnable task) {
        checkKey(name);
        Assert.notNull(task, "分布式任务不能为空");

        return redissonClient.getExecutorService(name).submit(task);
    }

    /**
     * 提交 Callable 分布式任务。
     *
     * @param name 执行器名称
     * @param task 任务
     * @param <T>  返回类型
     * @return Future
     */
    @Override
    public <T> Future<T> executorSubmit(String name, Callable<T> task) {
        checkKey(name);
        Assert.notNull(task, "分布式任务不能为空");

        return redissonClient.getExecutorService(name).submit(task);
    }

    /**
     * 关闭分布式执行器。
     *
     * @param name 执行器名称
     */
    @Override
    public void executorShutdown(String name) {
        if (StrUtil.isBlank(name)) {
            return;
        }

        redissonClient.getExecutorService(name).shutdown();
    }

    /**
     * 立即关闭分布式执行器。
     *
     * @param name 执行器名称
     * @return 未执行任务集合
     */
    @Override
    public List<Runnable> executorShutdownNow(String name) {
        if (StrUtil.isBlank(name)) {
            return Collections.emptyList();
        }

        return redissonClient.getExecutorService(name).shutdownNow();
    }

    /**
     * 调度 Runnable 分布式任务。
     *
     * @param name  执行器名称
     * @param task  任务
     * @param delay 延迟时间
     * @param unit  时间单位
     * @return ScheduledFuture
     */
    @Override
    public ScheduledFuture<?> schedule(String name, Runnable task, long delay, TimeUnit unit) {
        checkKey(name);
        Assert.notNull(task, "分布式定时任务不能为空");
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(delay >= 0, "延迟时间不能小于0");

        return redissonClient.getExecutorService(name).schedule(task, delay, unit);
    }

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
    @Override
    public <T> ScheduledFuture<T> schedule(String name, Callable<T> task, long delay, TimeUnit unit) {
        checkKey(name);
        Assert.notNull(task, "分布式定时任务不能为空");
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(delay >= 0, "延迟时间不能小于0");

        return redissonClient.getExecutorService(name).schedule(task, delay, unit);
    }

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
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(String name, Runnable task, long initialDelay, long period, TimeUnit unit) {
        checkKey(name);
        Assert.notNull(task, "分布式定时任务不能为空");
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(initialDelay >= 0, "初始延迟时间不能小于0");
        Assert.isTrue(period > 0, "执行周期必须大于0");

        return redissonClient.getExecutorService(name).scheduleAtFixedRate(task, initialDelay, period, unit);
    }

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
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(String name, Runnable task, long initialDelay, long delay, TimeUnit unit) {
        checkKey(name);
        Assert.notNull(task, "分布式定时任务不能为空");
        Assert.notNull(unit, "时间单位不能为空");
        Assert.isTrue(initialDelay >= 0, "初始延迟时间不能小于0");
        Assert.isTrue(delay > 0, "执行间隔必须大于0");

        return redissonClient.getExecutorService(name).scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    // -------------------------------------------------------------------------
    // RemoteService / LiveObject
    // -------------------------------------------------------------------------

    /**
     * 获取远程服务。
     *
     * @return RRemoteService
     */
    @Override
    public RRemoteService getRemoteService() {
        return redissonClient.getRemoteService();
    }

    /**
     * 获取指定名称的远程服务。
     *
     * @param name 服务名称
     * @return RRemoteService
     */
    @Override
    public RRemoteService getRemoteService(String name) {
        checkKey(name);
        return redissonClient.getRemoteService(name);
    }

    /**
     * 注册远程服务实现。
     *
     * @param remoteInterface 远程服务接口
     * @param implementation  远程服务实现
     * @param <T>             服务类型
     */
    @Override
    public <T> void remoteRegister(Class<T> remoteInterface, T implementation) {
        Assert.notNull(remoteInterface, "远程服务接口不能为空");
        Assert.notNull(implementation, "远程服务实现不能为空");

        redissonClient.getRemoteService().register(remoteInterface, implementation);
    }

    /**
     * 注册远程服务实现并指定工作线程数量。
     *
     * @param remoteInterface 远程服务接口
     * @param implementation  远程服务实现
     * @param workers         工作线程数量
     * @param <T>             服务类型
     */
    @Override
    public <T> void remoteRegister(Class<T> remoteInterface, T implementation, int workers) {
        Assert.notNull(remoteInterface, "远程服务接口不能为空");
        Assert.notNull(implementation, "远程服务实现不能为空");
        Assert.isTrue(workers > 0, "远程服务工作线程数量必须大于0");

        redissonClient.getRemoteService().register(remoteInterface, implementation, workers);
    }

    /**
     * 获取远程服务代理。
     *
     * @param remoteInterface 远程服务接口
     * @param <T>             服务类型
     * @return 服务代理
     */
    @Override
    public <T> T remoteGet(Class<T> remoteInterface) {
        Assert.notNull(remoteInterface, "远程服务接口不能为空");

        return redissonClient.getRemoteService().get(remoteInterface);
    }

    /**
     * 获取 LiveObject 服务。
     *
     * @return RLiveObjectService
     */
    @Override
    public RLiveObjectService getLiveObjectService() {
        return redissonClient.getLiveObjectService();
    }

    /**
     * 附加 LiveObject。
     *
     * @param detachedObject 游离对象
     * @param <T>            对象类型
     * @return LiveObject
     */
    @Override
    public <T> T liveObjectAttach(T detachedObject) {
        Assert.notNull(detachedObject, "LiveObject游离对象不能为空");

        return redissonClient.getLiveObjectService().attach(detachedObject);
    }

    /**
     * 合并 LiveObject。
     *
     * @param detachedObject 游离对象
     * @param <T>            对象类型
     * @return LiveObject
     */
    @Override
    public <T> T liveObjectMerge(T detachedObject) {
        Assert.notNull(detachedObject, "LiveObject游离对象不能为空");

        return redissonClient.getLiveObjectService().merge(detachedObject);
    }

    /**
     * 获取 LiveObject。
     *
     * @param entityClass 实体类型
     * @param id          实体 ID
     * @param <T>         对象类型
     * @return LiveObject
     */
    @Override
    public <T> T liveObjectGet(Class<T> entityClass, Object id) {
        Assert.notNull(entityClass, "LiveObject实体类型不能为空");
        Assert.notNull(id, "LiveObject实体ID不能为空");

        return redissonClient.getLiveObjectService().get(entityClass, id);
    }

    /**
     * 删除 LiveObject。
     *
     * @param attachedObject 已附加对象
     */
    @Override
    public void liveObjectDelete(Object attachedObject) {
        if (ObjectUtil.isNull(attachedObject)) {
            return;
        }

        redissonClient.getLiveObjectService().delete(attachedObject);
    }

    /**
     * 根据类型和 ID 删除 LiveObject。
     *
     * @param entityClass 实体类型
     * @param id          实体 ID
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void liveObjectDelete(Class<?> entityClass, Object id) {
        Assert.notNull(entityClass, "LiveObject实体类型不能为空");
        Assert.notNull(id, "LiveObject实体ID不能为空");

        redissonClient.getLiveObjectService().delete((Class) entityClass, id);
    }

    // -------------------------------------------------------------------------
    // Object Listener / 对象监听
    // -------------------------------------------------------------------------

    /**
     * 添加全局对象监听器。
     *
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    @Override
    public int addGlobalObjectListener(ObjectListener listener) {
        Assert.notNull(listener, "全局对象监听器不能为空");

        return redissonClient.getKeys().addListener(listener);
    }

    /**
     * 移除全局对象监听器。
     *
     * @param listenerId 监听器 ID
     */
    @Override
    public void removeGlobalObjectListener(int listenerId) {
        redissonClient.getKeys().removeListener(listenerId);
    }

    /**
     * 添加 Bucket 对象监听器。
     *
     * @param key      Redis 键
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    @Override
    public int addBucketListener(String key, ObjectListener listener) {
        checkKey(key);
        Assert.notNull(listener, "Bucket监听器不能为空");

        return redissonClient.getBucket(key).addListener(listener);
    }

    /**
     * 移除 Bucket 对象监听器。
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     */
    @Override
    public void removeBucketListener(String key, int listenerId) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getBucket(key).removeListener(listenerId);
    }

    /**
     * 添加 Map 对象监听器。
     *
     * @param key      Redis 键
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    @Override
    public int addMapListener(String key, ObjectListener listener) {
        checkKey(key);
        Assert.notNull(listener, "Map监听器不能为空");

        return redissonClient.getMap(key).addListener(listener);
    }

    /**
     * 添加 Map Entry 监听器。
     *
     * @param key      Redis 键
     * @param listener Entry 监听器
     * @return 监听器 ID
     */
    @Override
    public int addMapEntryListener(String key, ObjectListener listener) {
        checkKey(key);
        Assert.notNull(listener, "Map Entry监听器不能为空");

        return redissonClient.getMap(key).addListener(listener);
    }

    /**
     * 移除 Map 监听器。
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     */
    @Override
    public void removeMapListener(String key, int listenerId) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getMap(key).removeListener(listenerId);
    }

    /**
     * 添加 Queue 对象监听器。
     *
     * @param key      Redis 键
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    @Override
    public int addQueueListener(String key, ObjectListener listener) {
        checkKey(key);
        Assert.notNull(listener, "Queue监听器不能为空");

        return redissonClient.getQueue(key).addListener(listener);
    }

    /**
     * 移除 Queue 对象监听器。
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     */
    @Override
    public void removeQueueListener(String key, int listenerId) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getQueue(key).removeListener(listenerId);
    }

    /**
     * 添加 Set 对象监听器。
     *
     * @param key      Redis 键
     * @param listener 对象监听器
     * @return 监听器 ID
     */
    @Override
    public int addSetListener(String key, ObjectListener listener) {
        checkKey(key);
        Assert.notNull(listener, "Set监听器不能为空");

        return redissonClient.getSet(key).addListener(listener);
    }

    /**
     * 移除 Set 对象监听器。
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     */
    @Override
    public void removeSetListener(String key, int listenerId) {
        if (StrUtil.isBlank(key)) {
            return;
        }

        redissonClient.getSet(key).removeListener(listenerId);
    }

    // -------------------------------------------------------------------------
    // 私有辅助方法
    // -------------------------------------------------------------------------

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key 不能为空");
    }

    /**
     * 校验正数时间。
     *
     * @param duration 时间
     * @param message  异常消息
     */
    private void checkPositiveDuration(Duration duration, String message) {
        Assert.notNull(duration, message);
        Assert.isTrue(!duration.isZero() && !duration.isNegative(), message);
    }

    /**
     * 过滤空白 Redis Key。
     *
     * @param keys Redis 键数组
     * @return 过滤后的 Redis 键数组
     */
    private String[] filterKeys(String... keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return new String[0];
        }
        return java.util.Arrays.stream(keys)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toArray(String[]::new);
    }

    /**
     * 校验哈希字段。
     *
     * @param field 字段名
     */
    private void checkField(String field) {
        Assert.isTrue(StrUtil.isNotBlank(field), "Redis Hash 字段不能为空");
    }

    /**
     * 规范化列表索引，支持负数索引。
     *
     * @param index 原始索引
     * @param size  列表长度
     * @return 实际索引
     */
    private int normalizeIndex(long index, int size) {
        long realIndex = index < 0 ? size + index : index;
        if (realIndex > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (realIndex < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) realIndex;
    }

    /**
     * 将集合元素转换为指定类型 Set。
     *
     * @param source 原始集合
     * @param clazz  目标类型
     * @param <T>    泛型类型
     * @return 转换后的 Set
     */
    private <T> Set<T> convertSet(Collection<?> source, Class<T> clazz) {
        if (CollUtil.isEmpty(source) || ObjectUtil.isNull(clazz)) {
            return Collections.emptySet();
        }

        Set<T> result = new LinkedHashSet<>(source.size());
        for (Object item : source) {
            T converted = convertValue(item, clazz);
            if (ObjectUtil.isNotNull(converted)) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * 将 ScoredEntry 集合转换为有序 Map。
     *
     * @param entries ScoredEntry 集合
     * @return 元素分数 Map
     */
    private Map<Object, Double> scoredEntriesToMap(Collection<ScoredEntry<Object>> entries) {
        if (CollUtil.isEmpty(entries)) {
            return Collections.emptyMap();
        }

        Map<Object, Double> result = new LinkedHashMap<>(entries.size());
        for (ScoredEntry<Object> entry : entries) {
            result.put(entry.getValue(), entry.getScore());
        }
        return result;
    }

    /**
     * 安全释放分布式锁。
     *
     * @param lockKey 锁 key
     * @param lock    锁对象
     * @param locked  是否已加锁
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
        } catch (Exception e) {
            log.error("释放分布式锁异常，lockKey={}", lockKey, e);
        }
    }

    /**
     * 构建签到 Key。
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param year      年份
     * @return 签到 Key
     */
    private String buildSignKey(String keyPrefix, Object userId, int year) {
        return StrUtil.format("{}:{}:{}", keyPrefix, userId, year);
    }

    /**
     * 构建 UV Key。
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param date      日期
     * @return UV Key
     */
    private String buildUvKey(String keyPrefix, String bizKey, LocalDate date) {
        String dateText = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return StrUtil.format("{}:{}:{}", keyPrefix, bizKey, dateText);
    }

    /**
     * 构建会话 Key。
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 会话 Key
     */
    private String buildSessionKey(String keyPrefix, String token) {
        return StrUtil.format("{}:{}", keyPrefix, token);
    }

    /**
     * 校验 Geo 坐标。
     *
     * @param longitude 经度
     * @param latitude  纬度
     */
    private void checkGeoCoordinate(double longitude, double latitude) {
        Assert.isTrue(longitude >= -180D && longitude <= 180D, "经度必须在 -180 到 180 之间");
        Assert.isTrue(latitude >= -90D && latitude <= 90D, "纬度必须在 -90 到 90 之间");
    }

    /**
     * 获取安全的 Lua KEYS 参数。
     *
     * @param keys 原始 KEYS
     * @return 非空 KEYS
     */
    private List<Object> safeScriptKeys(List<Object> keys) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyList();
        }
        return keys;
    }

    /**
     * 校验 Stream 范围参数。
     *
     * @param startId 开始消息 ID
     * @param endId   结束消息 ID
     * @param count   数量
     */
    private void checkStreamRangeArgs(StreamMessageId startId, StreamMessageId endId, int count) {
        Assert.notNull(startId, "开始消息ID不能为空");
        Assert.notNull(endId, "结束消息ID不能为空");
        Assert.isTrue(count > 0, "读取数量必须大于0");
    }

}
```



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

