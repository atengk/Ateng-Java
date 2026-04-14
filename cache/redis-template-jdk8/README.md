# Spring Data Redis

RedisTemplate 是 Spring Data Redis 提供的一个核心类，用于操作 Redis 数据库，支持对 Redis 各种数据结构的访问，例如字符串（String）、哈希（Hash）、列表（List）、集合（Set）、有序集合（Sorted Set）等。

**主要特点**
通用性强：支持 Redis 的所有数据结构和命令。
扩展性好：可以通过自定义序列化器来适配不同的数据格式。
线程安全：可以在多个线程中安全使用。



## 基础配置

### 添加依赖

添加Spring Boot Redis和Lettuce

```xml
<!-- Spring Boot Redis 数据库集成，支持多种 Redis 数据结构和操作 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Lettuce 客户端连接池实现，基于 Apache Commons Pool2 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 编辑配置

编辑配置文件 `application.yml`

```yaml
---
# Redis的相关配置
spring:
  redis:
    host: 175.178.193.128 # Redis服务器地址
    database: 1 # Redis数据库索引（默认为0）
    port: 20045 # Redis服务器连接端口
    password: Admin@123 # Redis服务器连接密码（默认为空）
    client-type: lettuce  # 默认使用Lettuce作为Redis客户端
    lettuce:
      pool:
        max-active: 100 # 连接池最大连接数（使用负值表示没有限制）
        max-wait: -1s # 连接池最大阻塞等待时间（使用负值表示没有限制）
        max-idle: 100 # 连接池中的最大空闲连接
        min-idle: 0 # 连接池最小空闲连接数
        time-between-eviction-runs: 1s # 空闲对象逐出器线程的运行间隔时间.空闲连接线程释放周期时间
    timeout: 5000ms # 连接超时时间（毫秒）
```



## 序列化（可选）

如果不配置序列化在使用 `RedisTemplate` 时记得指定泛型，例如：RedisTemplate<String, MyUser>，不然使用的是Java序列化（二进制）。

如果需要配置序列化，以下方式任选一种，实现序列化后就可以直接使用RedisTemplate redisTemplate

### 使用 Jackson 序列化

Spring Boot 默认集成的 Jackson 序列化库也可以用来做 Redis 序列化。具体来说，就是可以通过 `Jackson2JsonRedisSerializer` 来进行对象的 JSON 序列化和反序列化。

#### ObjectMapper 统一构建工厂

```java
package local.ateng.java.redisjdk8.config;

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

#### 配置序列化和反序列化（最小化版）

最小化配置序列化， 如需详细的自定义配置序列化参考下面的步骤。

```java
package local.ateng.java.redisjdk8.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> jacksonRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 设置 Key 序列化器
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // 创建 ObjectMapper 实例，用于 JSON 序列化和反序列化
        ObjectMapper objectMapper = JacksonObjectMapperFactory.buildStorageObjectMapper();

        // 设置 Value 序列化器
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
```



#### 配置序列化和反序列化

```java
package local.ateng.java.redisjdk8.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 配置类
 * <p>
 * 该类负责配置 RedisTemplate，允许对象进行序列化和反序列化。
 * 在这里，我们使用了 StringRedisSerializer 来序列化和反序列化 Redis 键，
 * 使用 Jackson2JsonRedisSerializer 来序列化和反序列化 Redis 值，确保 Redis 能够存储 Java 对象。
 * 另外，ObjectMapper 的配置确保 JSON 的格式和解析行为符合预期。
 * </p>
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        /**
         * 使用StringRedisSerializer来序列化和反序列化redis的key值
         */
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        /**
         * 创建 ObjectMapper 实例，用于配置 Jackson 的序列化和反序列化行为
         */
        ObjectMapper objectMapper = JacksonObjectMapperFactory.buildStorageObjectMapper();

        // 创建 Jackson2JsonRedisSerializer，用于序列化和反序列化值
        // 该序列化器使用配置好的 ObjectMapper
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 设置 RedisTemplate 的值的序列化器
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);  // 设置哈希值的序列化器

        // 返回redisTemplate
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
```



### 使用 Fastjson2 序列化

参考官方文档：[地址](https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md#4-%E5%9C%A8-spring-data-redis-%E4%B8%AD%E9%9B%86%E6%88%90-fastjson2)

#### 添加依赖

```xml
<!-- 高性能的JSON库 -->
<!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>${fastjson2.version}</version>
</dependency>
<!-- Spring 中集成 Fastjson2 -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2-extension-spring5</artifactId>
    <version>${fastjson2.version}</version>
</dependency>
```

#### 配置序列化和反序列化（最小化版）

最小化配置序列化， 如需详细的自定义配置序列化参考下面的步骤。

```java
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> fastjson2RedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 设置 Key 序列化器
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // 设置 Value 序列化器
        GenericFastJsonRedisSerializer valueSerializer = new GenericFastJsonRedisSerializer();
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }


}
```



#### 配置序列化器

注意修改为自己的包名：`config.setReaderFilters(JSONReader.autoTypeFilter("local.ateng.java."));`

```java
package local.ateng.java.redis.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

public class FastJson2RedisSerializer<T> implements RedisSerializer<T> {
    private final Class<T> type;
    private FastJsonConfig config = new FastJsonConfig();

    public FastJson2RedisSerializer(Class<T> type) {
        config.setCharset(Charset.forName("UTF-8"));
        config.setDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        // 配置 JSONWriter 的特性
        config.setWriterFeatures(
                // 序列化时输出类型信息
                JSONWriter.Feature.WriteClassName,
                // 不输出数字类型的类名
                JSONWriter.Feature.NotWriteNumberClassName,
                // 不输出 Set 类型的类名
                JSONWriter.Feature.NotWriteSetClassName,
                // 序列化输出空值字段
                JSONWriter.Feature.WriteNulls,
                // 在大范围超过JavaScript支持的整数，输出为字符串格式
                JSONWriter.Feature.BrowserCompatible,
                // 序列化BigDecimal使用toPlainString，避免科学计数法
                JSONWriter.Feature.WriteBigDecimalAsPlain
        );

        // 配置 JSONReader 的特性
        config.setReaderFeatures(
                // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                JSONReader.Feature.SupportSmartMatch
        );

        // 支持自动类型，要读取带"@type"类型信息的JSON数据，需要显式打开SupportAutoType
        config.setReaderFilters(
                JSONReader.autoTypeFilter(
                        // 按需加上需要支持自动类型的类名前缀，范围越小越安全
                        "local.ateng.java."
                )
        );
        this.type = type;
    }

    public FastJsonConfig getFastJsonConfig() {
        return config;
    }

    public void setFastJsonConfig(FastJsonConfig fastJsonConfig) {
        this.config = fastJsonConfig;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        try {
            if (config.isJSONB()) {
                return JSONB.toBytes(t, config.getSymbolTable(), config.getWriterFilters(), config.getWriterFeatures());
            } else {
                return JSON.toJSONBytes(t, config.getDateFormat(), config.getWriterFilters(), config.getWriterFeatures());
            }
        } catch (Exception ex) {
            throw new SerializationException("Could not serialize: " + ex.getMessage(), ex);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            if (config.isJSONB()) {
                return JSONB.parseObject(bytes, type, config.getSymbolTable(), config.getReaderFilters(), config.getReaderFeatures());
            } else {
                return JSON.parseObject(bytes, type, config.getDateFormat(), config.getReaderFilters(), config.getReaderFeatures());
            }
        } catch (Exception ex) {
            throw new SerializationException("Could not deserialize: " + ex.getMessage(), ex);
        }
    }
}
```

#### 配置序列化和反序列化

```java
package local.ateng.java.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 配置类
 *
 * <p>
 * 该类负责配置 RedisTemplate，允许对象进行序列化和反序列化。
 * 在这里，我们使用了 StringRedisSerializer 来序列化和反序列化 Redis 键，
 * 使用 FastJsonRedisSerializer 来序列化和反序列化 Redis 值，确保 Redis 能够存储 Java 对象。
 * </p>
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-06
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        /**
         * 使用StringRedisSerializer来序列化和反序列化redis的key值
         */
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);

        /**
         * 使用自定义的Fastjson2的Serializer来序列化和反序列化redis的value值
         */
        FastJson2RedisSerializer fastJson2RedisSerializer = new FastJson2RedisSerializer(Object.class);
        redisTemplate.setValueSerializer(fastJson2RedisSerializer);
        redisTemplate.setHashValueSerializer(fastJson2RedisSerializer);

        // 返回redisTemplate
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
```



## 添加多个Redis（可选）

### 添加配置文件

```yaml
---
# Redis的相关配置
spring:
  # ...
  redis-dev:
    host: 192.168.1.10 # Redis服务器地址
    database: 101 # Redis数据库索引（默认为0）
    port: 42784 # Redis服务器连接端口
    password: Admin@123 # Redis服务器连接密码（默认为空）
    client-type: lettuce  # 默认使用Lettuce作为Redis客户端
    lettuce:
      pool:
        max-active: 100 # 连接池最大连接数（使用负值表示没有限制）
        max-wait: -1s # 连接池最大阻塞等待时间（使用负值表示没有限制）
        max-idle: 100 # 连接池中的最大空闲连接
        min-idle: 0 # 连接池最小空闲连接数
        time-between-eviction-runs: 1s # 空闲对象逐出器线程的运行间隔时间.空闲连接线程释放周期时间
    timeout: 10000ms # 连接超时时间（毫秒）
```

### 添加配置属性

```java
/**
 * 自定义Redis配置文件
 *
 * @author 孔余
 * @since 2024-01-18 11:02
 */
@ConfigurationProperties(prefix = "spring")
@Configuration
@Data
public class MyRedisProperties {
    private RedisProperties redisDev;
    // private RedisProperties redisTest;
}
```

### 添加连接工厂

在 RedisTemplateConfig 文件中添加连接工厂信息

```java
    /**
     * Lettuce的Redis连接工厂
     *
     * @param redisProperties Redis服务的参数
     * @return Lettuce连接工厂
     */
    private LettuceConnectionFactory createLettuceConnectionFactory(RedisProperties redisProperties) {
        RedisProperties.Pool pool = redisProperties.getLettuce().getPool();
        // 设置Redis的服务参数
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setDatabase(redisProperties.getDatabase());
        redisConfig.setPort(redisProperties.getPort());
        redisConfig.setPassword(redisProperties.getPassword());
        // 设置连接池属性
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(pool.getMaxActive());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setMinIdle(pool.getMinIdle());
        poolConfig.setMaxWait(pool.getMaxWait());
        poolConfig.setTimeBetweenEvictionRuns(pool.getTimeBetweenEvictionRuns());
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration
                .builder()
                .commandTimeout(redisProperties.getTimeout())
                .poolConfig(poolConfig)
                .build();
        // 返回Lettuce的Redis连接工厂
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }
```

### 创建Bean

在 RedisTemplateConfig 文件中添加连接工厂信息

#### 注入Properties

```java
private final MyRedisProperties myRedisProperties;

public RedisTemplateConfig(MyRedisProperties myRedisProperties) {
    this.myRedisProperties = myRedisProperties;
}
```

#### 创建Bean

```java
/**
 * 自定义Redis配置，从MyRedisProperties获取redis-dev的配置，redisTemplateDev
 * 使用：
 *
 * @return
 * @Qualifier("redisTemplateDev") private final RedisTemplate redisTemplateDev;
 */
@Bean
public RedisTemplate<String, Object> redisTemplateDev() {
    // 连接Redis
    LettuceConnectionFactory factory = this.createLettuceConnectionFactory(myRedisProperties.getRedisDev());
    RedisTemplate redisTemplate = new RedisTemplate();
    redisTemplate.setConnectionFactory(factory);

    /**
     * 使用StringRedisSerializer来序列化和反序列化redis的key值
     */
    StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
    redisTemplate.setKeySerializer(stringRedisSerializer);
    redisTemplate.setHashKeySerializer(stringRedisSerializer);
    
    // 值序列化，使用Jackson或者Fastjson2...

    // 返回redisTemplate
    redisTemplate.afterPropertiesSet();
    return redisTemplate;
}
```

### 使用新Redis

```java
    @Qualifier("redisTemplateDev") 
    private final RedisTemplate redisTemplateDev;

    @Test
    void test05() {
        UserInfoEntity user = UserInfoEntity.builder()
                .id(100L)
                .name("John Doe")
                .age(25)
                .score(85.5)
                .birthday(new Date())
                .province("")
                .city("Example City")
                .build();
        redisTemplateDev.opsForValue().set("test:user", user);
    }

    @Test
    void test05_1() {
        UserInfoEntity user = (UserInfoEntity) redisTemplateDev.opsForValue().get("test:user");
        System.out.println(user);
        System.out.println(user.getName());
    }
```



## 注入使用

### 不写泛型

注入RedisTemplate

```
private final RedisTemplate redisTemplate;
```

写入数据

```
redisTemplate.opsForValue().set("myUser", myUser);
```

读取数据

```
MyUser myUser = (MyUser) redisTemplate.opsForValue().get("myUser");
```



### 写入泛型

注入RedisTemplate

```
private final RedisTemplate<String, Object> redisTemplate;
```

写入数据

```
redisTemplate.opsForValue().set("myUser", myUser);
```

读取数据

```
MyUser myUser = (MyUser) redisTemplate.opsForValue().get("myUser");
```



## 数据准备

### 添加依赖

```xml
        <!-- Hutool: Java工具库，提供了许多实用的工具方法 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- JavaFaker: 用于生成虚假数据的Java库 -->
        <dependency>
            <groupId>com.github.javafaker</groupId>
            <artifactId>javafaker</artifactId>
            <version>1.0.2</version>
        </dependency>
```

### 创建实体类

```java
package local.kongyu.redisTemplate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户信息实体类
 * 用于表示系统中的用户信息。
 *
 * @author 孔余
 * @since 2024-01-10 15:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户年龄
     * 注意：这里使用Integer类型，表示年龄是一个整数值。
     */
    private Integer age;

    /**
     * 分数
     */
    private Double score;

    /**
     * 用户生日
     * 注意：这里使用Date类型，表示用户的生日。
     */
    private Date birthday;

    /**
     * 用户所在省份
     */
    private String province;

    /**
     * 用户所在城市
     */
    private String city;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

### 创建初始数据

```java
package local.kongyu.redisTemplate.init;

import com.github.javafaker.Faker;
import local.kongyu.redisTemplate.entity.UserInfoEntity;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 初始化数据
 *
 * @author 孔余
 * @since 2024-01-18 14:17
 */
@Getter
public class InitData {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public InitData() {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        // 创建一个包含不少于100条JSON数据的列表
        List<UserInfoEntity> userList = new ArrayList();
        for (int i = 1; i <= 10; i++) {
            UserInfoEntity user = new UserInfoEntity();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setBirthday(faker.date().birthday());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setScore(faker.number().randomDouble(3, 1, 100));
            user.setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        list = userList;
        for (int i = 1; i <= 20; i++) {
            UserInfoEntity user = new UserInfoEntity();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setBirthday(faker.date().birthday());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setScore(faker.number().randomDouble(3, 1, 100));
            user.setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        list2 = userList;
    }

}
```



## 使用String

### 创建测试类

```java
/**
 * Redis String相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisStringTests {
    private final RedisTemplate redisTemplate;
    
}
```

### 写入对象

```java
    //设置key对应的值
    @Test
    void set() {
        String key = "my:user";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForValue().set(key, user);
        // 并设置过期时间
        redisTemplate.opsForValue().set(key + ":expire", user, Duration.ofHours(1));
    }

    @Test
    void setMany() {
        String key = "my:user:data";
        List<UserInfoEntity> list = new InitData().getList();
        list.forEach(user -> redisTemplate.opsForValue().set(key + ":" + user.getId(), user));
    }

    @Test
    void setList() {
        String key = "my:userList";
        List<UserInfoEntity> list = new InitData().getList();
        redisTemplate.opsForValue().set(key, list);
    }
```

### 读取对象

```java
    //取出key值所对应的值
    @Test
    void get() {
        UserInfoEntity user = (UserInfoEntity) redisTemplate.opsForValue().get("my:user");
        System.out.println(user);
        System.out.println(user.getName());
    }

    @Test
    void getList() {
        List<UserInfoEntity> userList = (List<UserInfoEntity>) redisTemplate.opsForValue().get("my:userList");
        System.out.println(userList);
        System.out.println(userList.get(0).getName());
    }
```

### 判断key是否存在

```java
    //判断是否有key所对应的值，有则返回true，没有则返回false
    @Test
    void hashKey() {
        String key = "my:user";
        Boolean result = redisTemplate.hasKey(key);
        System.out.println(result);
    }
```

### 删除key

```java
    //删除单个key值
    @Test
    void delete() {
        String key = "my:user";
        Boolean result = redisTemplate.delete(key);
        System.out.println(result);
    }

    //删除多个key值
    @Test
    void deletes() {
        List<String> keys = new ArrayList<>();
        keys.add("my:user");
        keys.add("my:userList");
        Long result = redisTemplate.delete(keys);
        System.out.println(result);
    }
```

### 设置过期时间

```java
    //设置过期时间
    @Test
    void expire() {
        String key = "my:user";
        long timeout = 1;
        Boolean result = redisTemplate.expire(key, timeout, TimeUnit.HOURS);
        System.out.println(result);
    }

    // 设置指定时间过期
    // 如果超过当前时间则key会被清除
    @Test
    void expireAt() {
        String key = "my:user";
        String dateTime = "2025-12-12 22:22:22";
        Boolean result = redisTemplate.expireAt(key, DateUtil.parse(dateTime));
        System.out.println(result);
    }

    //返回剩余过期时间并且指定时间单位
    @Test
    void getExpire() {
        String key = "my:user";
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        System.out.println(expire);
    }
```

### 查找匹配的key值

```java
    // 查找匹配的key值，返回一个Set集合类型
    @Test
    void keysAndValues() {
        // 返回所有key，保证这些key的值的数据类型一致
        String pattern = "my:user:data:*";
        Set<String> keys = redisTemplate.keys(pattern);
        keys.forEach(System.out::println);
        System.out.println(keys.size());
        // 返回所有value
        List<UserInfoEntity> values = (List<UserInfoEntity>) redisTemplate.opsForValue().multiGet(keys);
        values.forEach(value-> System.out.println(value.getName()));
    }
```

### 自增值increment

```java
    //以增量的方式将double值存储在变量中
    @Test
    void incrementDouble() {
        String key = "my:double";
        double delta = 0.1;
        Double result = redisTemplate.opsForValue().increment(key, delta);
        System.out.println(result);
    }
    //通过increment(K key, long delta)方法以增量方式存储long值（正值则自增，负值则自减）
    @Test
    void incrementLong() {
        String key = "my:long";
        long delta = 1;
        Long result = redisTemplate.opsForValue().increment(key, delta);
        System.out.println(result);
    }
```



## 使用List

### 创建测试类

```java
/**
 * Redis List相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisListTests {
    private final RedisTemplate redisTemplate;
    
}
```

### rightPush 队列 先进先出

```java
    /**
     * 功能: 将指定的值 value 添加到列表 key 的 右端（尾部）。
     * 使用场景: 通常用于像队列一样的操作，先进先出（FIFO）。例如，添加数据到消息队列的末尾，等待后续的消费
     */
    @Test
    void rightPush() {
        String key = "my:list:user";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForList().rightPush(key, user);
    }

    @Test
    void rightPushAll() {
        String key = "my:list:userList";
        List<UserInfoEntity> list = new InitData().getList();
        redisTemplate.opsForList().rightPushAll(key, list);
    }

    /**
     * 用于从Redis的列表中 弹出并移除 右端（尾部）的元素。这个操作类似于从队列的尾部取出元素。
     */
    @Test
    void rightPop() {
        String key = "my:list:userList";
        UserInfoEntity userInfoEntity = (UserInfoEntity) redisTemplate.opsForList().rightPop(key);
        System.out.println(userInfoEntity.getId());
    }
```

### leftPush 栈 先进后出

```java
    /**
     * 功能: 将指定的值 value 添加到列表 key 的 左端（头部）。
     * 使用场景: 通常用于像栈一样的操作，后进先出（LIFO）。例如，添加数据到列表的开头。
     */
    @Test
    void leftPush() {
        String key = "my:list:user";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForList().leftPush(key, user);
    }

    @Test
    void leftPushAll() {
        String key = "my:list:userList";
        List<UserInfoEntity> list = new InitData().getList();
        redisTemplate.opsForList().leftPushAll(key, list);
    }

    /**
     * 用于从 Redis 列表中 弹出并移除 左端（头部）元素的方法。这个操作类似于从队列的头部取出元素。
     */
    @Test
    void leftPop() {
        String key = "my:list:userList";
        UserInfoEntity userInfoEntity = (UserInfoEntity) redisTemplate.opsForList().leftPop(key);
        System.out.println(userInfoEntity.getId());
    }
```

### 获取列表元素

```java
    // 获取列表指定范围内的元素(start开始位置, 0是开始位置，end 结束位置, -1返回所有)
    @Test
    void range() {
        String key = "my:list:userList";
        long start = 0;
        long end = -1;
        List<UserInfoEntity> result = redisTemplate.opsForList().range(key, start, end);
        System.out.println(result);
    }
```



## 使用Hash

### 创建测试类

```java
/**
 * Redis HashMap相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisHashMapTests {
    private final RedisTemplate redisTemplate;
    
}
```

### 新增数据

```java
    // 新增hashMap值
    @Test
    void put() {
        String key = "my:hashmap:user";
        String hashKey = "user1";
        UserInfoEntity user = new InitData().getList().get(0);
        redisTemplate.opsForHash().put(key, hashKey, user);
    }

    @Test
    void putAll() {
        Map<String, Object> map = new HashMap<>();
        List<UserInfoEntity> list = new InitData().getList();
        list.forEach(user -> map.put("user" + user.getId(), user));
        redisTemplate.opsForHash().putAll("my:hashmap:userList", map);
    }
```

### 获取数据

```java
    // 获取hashMap值，不存在为null
    @Test
    void get() {
        String key = "my:hashmap:user";
        String hashKey = "user1";
        UserInfoEntity result = (UserInfoEntity) redisTemplate.opsForHash().get(key, hashKey);
        System.out.println(result);
        System.out.println(result.getName());
    }

    // 获取Key
    @Test
    void getKey() {
        String key = "my:hashmap:user";
        Set keys = redisTemplate.opsForHash().keys(key);
        String next = (String) keys.iterator().next();
        System.out.println(next);
    }

    // 获取多个hashMap的值
    @Test
    void multiGet() {
        String key = "my:hashmap:userList";
        List<UserInfoEntity> list = (List<UserInfoEntity>) redisTemplate.opsForHash().multiGet(key, Arrays.asList("user1", "user2"));
        System.out.println(list);
        list.forEach(user -> System.out.println(user.getName()));
    }

    // 获取所有hashMap的值
    @Test
    void getAll() {
        String key = "my:hashmap:userList";
        Map<String, UserInfoEntity> entries = (Map<String, UserInfoEntity>) redisTemplate.opsForHash().entries(key);
        System.out.println(entries);
        entries.values().forEach(value-> System.out.println(value.getName()));
    }

    // 获取所有hashMap的key值
    @Test
    void keys() {
        String key = "my:hashmap:userList";
        Set<String> keys = (Set<String>) redisTemplate.opsForHash().keys(key);
        System.out.println(keys);
    }

    // 获取所有hashMap的key值
    @Test
    void values() {
        String key = "my:hashmap:userList";
        List<UserInfoEntity> values = (List<UserInfoEntity>) redisTemplate.opsForHash().values(key);
        System.out.println(values);
        values.forEach(user -> System.out.println(user.getName()));
    }
```

### 删除数据

```java
    // 删除一个或者多个hash表字段
    @Test
    void delete() {
        String key = "my:hashmap:userList";
        String hashKey1 = "user1";
        String hashKey2 = "user2";
        Long result = redisTemplate.opsForHash().delete(key, hashKey1, hashKey2);
        System.out.println(result);
    }
```

### 匹配获取键值对

```java
    // 匹配获取键值对
    @Test
    void scan() {
        String key = "my:hashmap:userList";
        // 模糊匹配
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("user1*")
                .build();
        Cursor<Map.Entry<Object, Object>> result = redisTemplate.opsForHash().scan(key, scanOptions);
        while (result.hasNext()) {
            Map.Entry<Object, Object> entry = result.next();
            System.out.println(entry.getKey() + "：" + entry.getValue());
        }
    }
```

## 使用Set

### 创建测试类

```java
/**
 * Redis Set集合相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisSetTests {
    private final RedisTemplate redisTemplate;
    
}
```

### 新增数据

```java
    // 添加数据
    @Test
    void add() {
        String key = "my:set:number";
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(RandomUtil.randomInt(1, 100));
        }
        redisTemplate.opsForSet().add(key, list.toArray(new Integer[0]));
    }
```

### 获取数据

```java
    // 获取集合中的所有数据
    @Test
    void members() {
        String key = "my:set:number";
        Set<Integer> members = (LinkedHashSet<Integer>) redisTemplate.opsForSet().members(key);
        System.out.println(members);
    }
```

### 删除数据

```java
    // 删除数据
    @Test
    void remove() {
        String key = "my:set:number";
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(RandomUtil.randomInt(1, 1000));
        }
        redisTemplate.opsForSet().remove(key, list.toArray(new Integer[0]));
    }

    // 删除并且返回一个随机的元素
    @Test
    void pop() {
        String key = "my:set:number";
        Integer result1 = (Integer) redisTemplate.opsForSet().pop(key);
        List<Integer> result2 = (ArrayList<Integer>) redisTemplate.opsForSet().pop(key, 2); // pop多个
        System.out.println(result1);
        System.out.println(result2);
    }
```

### 查询数据

```java
    // 判断集合是否包含value
    @Test
    void isMember() {
        String key = "my:set:number";
        String value = "60";
        Boolean result = redisTemplate.opsForSet().isMember(key, value);
        System.out.println(result);
    }

    // 获取两个集合的交集
    @Test
    void intersect() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().intersect(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的交集，并将结果存储到新的key
    @Test
    void intersectAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:intersect";
        redisTemplate.opsForSet().intersectAndStore(key1, key2, destKey);
    }

    // 获取两个集合的并集
    @Test
    void union() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().union(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的并集，并将结果存储到新的key
    @Test
    void unionAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:union";
        redisTemplate.opsForSet().unionAndStore(key1, key2, destKey);
    }

    // 获取两个集合的差集
    @Test
    void difference() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        Set<Integer> result = (LinkedHashSet<Integer>) redisTemplate.opsForSet().difference(key1, key2);
        System.out.println(result);
    }

    // 获取两个集合的并集，并将结果存储到新的key
    @Test
    void differenceAndStore() {
        String key1 = "my:set:number1";
        String key2 = "my:set:number2";
        String destKey = "my:set:difference";
        redisTemplate.opsForSet().differenceAndStore(key1, key2, destKey);
    }

    // 随机获取集合中的一个元素
    @Test
    void randomMember() {
        String key = "my:set:number";
        Integer result = (Integer) redisTemplate.opsForSet().randomMember(key);
        System.out.println(result);
    }

    // 随机获取集合中count个元素
    @Test
    void randomMembers() {
        String key = "my:set:number";
        List<Integer> strings = (ArrayList<Integer>) redisTemplate.opsForSet().randomMembers(key, 2);
        System.out.println(strings);
    }

    // 模糊匹配数据
    @Test
    void scan() {
        String key = "my:set:number";
        // 模糊匹配
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match("2*")
                .build();
        Cursor result1 = redisTemplate.opsForSet().scan(key, scanOptions);
        while (result1.hasNext()) {
            Object next = result1.next();
            System.out.println(next);
        }
    }
```

## 使用ZSet

### 创建测试类

```java
/**
 * Redis ZSet集合相关的操作
 *
 * @author 孔余
 * @since 2024-02-22 14:40
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisZSetTests {
    private final RedisTemplate redisTemplate;
    
}
```

### 新增数据

```java
    // 添加元素(有序集合是按照元素的score值由小到大进行排列)
    @Test
    void add() {
        String key = "my:zset:number";
        String value = "value";
        double score = 1.1;
        Boolean result = redisTemplate.opsForZSet().add(key, value, score);
        System.out.println(result);
    }
    @Test
    void addMany() {
        String key = "my:zset:number";
        for (int i = 0; i < 30; i++) {
            String value = "value" + i;
            double score = RandomUtil.randomDouble(0,1,2, RoundingMode.UP);
            Boolean result = redisTemplate.opsForZSet().add(key, value, score);
            System.out.println(result);
        }
    }
```

### 删除数据

```java
    // 删除元素
    @Test
    void remove() {
        String key = "my:zset:number";
        String value1 = "value1";
        String value2 = "value2";
        Long result = redisTemplate.opsForZSet().remove(key, value1, value2);
        System.out.println(result);
    }

    // 移除指定索引位置处的成员
    @Test
    void removeRange() {
        String key = "my:zset:number";
        long start = 1;
        long end = 3;
        Long result = redisTemplate.opsForZSet().removeRange(key, start, end);
        System.out.println(result);
    }

    // 移除指定score范围的集合成员
    @Test
    void removeRangeByScore() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        Long result = redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
        System.out.println(result);
    }
```

### 查询数据

```java
    // 返回元素在集合的排名,有序集合是按照元素的score值由小到大排列
    @Test
    void rank() {
        String key = "my:zset:number";
        String value = "value10";
        Long result = redisTemplate.opsForZSet().rank(key, value);
        System.out.println(result);
    }

    // 返回元素在集合的排名,按元素的score值由大到小排列
    @Test
    void reverseRank() {
        String key = "my:zset:number";
        String value = "value10";
        Long result = redisTemplate.opsForZSet().reverseRank(key, value);
        System.out.println(result);
    }

    // 获取集合中给定区间的元素(start 开始位置，end 结束位置, -1查询所有)
    @Test
    void reverseRangeWithScores() {
        String key = "my:zset:number";
        long start = 1;
        long end = 4;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        List<ZSetOperations.TypedTuple<String>> list = new ArrayList<>(typedTuples);
        list.forEach(data -> System.out.println("value=" + data.getValue() + ", score=" + data.getScore()));
    }

    // 按照Score值查询集合中的元素，结果从小到大排序
    @Test
    void reverseRangeByScore() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        Set<String> result = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
        System.out.println(result);
    }

    // 按照Score值查询集合中的元素，结果从小到大排序
    @Test
    void reverseRangeByScoreWithScores() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min, max);
        List<ZSetOperations.TypedTuple<String>> list = new ArrayList<>(typedTuples);
        list.forEach(data -> System.out.println("value=" + data.getValue() + ", score=" + data.getScore()));
    }

    // 从高到低的排序集中获取分数在最小和最大值之间的元素
    @Test
    void reverseRangeByScoreOffset() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        long offset = 1;
        long count = 3;
        Set<String> result = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count);
        System.out.println(result);
    }

    // 根据score值获取集合元素数量
    @Test
    void count() {
        String key = "my:zset:number";
        double min = 0.8;
        double max = 3.0;
        Long count = redisTemplate.opsForZSet().count(key, min, max);
        System.out.println(count);
    }

    // 获取集合的大小
    @Test
    void size() {
        String key = "my:zset:number";
        Long size = redisTemplate.opsForZSet().size(key);
        Long zCard = redisTemplate.opsForZSet().zCard(key);
        System.out.println(size);
        System.out.println(zCard);
    }

    // 获取集合中key、value元素对应的score值
    @Test
    void score() {
        String key = "my:zset:number";
        String value = "value";
        Double score = redisTemplate.opsForZSet().score(key, value);
        System.out.println(score);
    }

    // 获取所有元素
    @Test
    void scan() {
        String key = "my:zset:number";
        Cursor<ZSetOperations.TypedTuple<String>> scan = redisTemplate.opsForZSet().scan(key, ScanOptions.NONE);
        while (scan.hasNext()) {
            ZSetOperations.TypedTuple<String> item = scan.next();
            System.out.println(item.getValue() + ":" + item.getScore());
        }
    }
```

### 查询并存储数据

```java
    // 获取key和otherKey的并集并存储在destKey中（其中otherKeys可以为单个字符串或者字符串集合）
    @Test
    void unionAndStore() {
        String key = "my:zset:number";
        String otherKey = "my:zset:number2";
        String destKey = "my:zset:number3";
        Long result = redisTemplate.opsForZSet().unionAndStore(key, otherKey, destKey);
        System.out.println(result);
    }

    // 获取key和otherKey的交集并存储在destKey中（其中otherKeys可以为单个字符串或者字符串集合）
    @Test
    void intersectAndStore() {
        String key = "my:zset:number";
        String otherKey = "my:zset:number2";
        String destKey = "my:zset:number4";
        Long result = redisTemplate.opsForZSet().intersectAndStore(key, otherKey, destKey);
        System.out.println(result);
    }
```

## 发布与订阅

### 创建Listener

```java
package local.ateng.java.redis.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * RedisMessageListener 类用于监听来自 Redis 频道的消息。
 * 它实现了 Spring Data Redis 提供的 MessageListener 接口，
 * 并且作为一个 Spring Bean 被自动注册，可以用于接收发布/订阅（Pub/Sub）模式下的消息。
 * <p>
 * 该类将会监听一个指定的 Redis 频道，并处理接收到的消息。
 */
@Component  // 注解表明这是一个 Spring 管理的组件，将自动被注册为 Bean
public class RedisMessageListener implements MessageListener {

    /**
     * 当 Redis 频道接收到消息时，调用此方法。
     * 该方法由 Spring Data Redis 自动触发，当 Redis 发布消息到指定频道时会被调用。
     *
     * @param message Redis 消息对象，包含了消息的具体内容
     * @param pattern 可选的消息模式，通常为空，但如果使用了模式匹配订阅（如 PSUBSCRIBE），此参数表示匹配的模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 获取消息的内容，将其从字节数组转换为字符串
        String msg = new String(message.getBody());

        // 打印接收到的消息内容
        // 这里可以根据需求做进一步的消息处理，如日志记录、存储等
        System.out.println("Received message: " + msg);
    }
}
```

### 创建Config

```java
package local.ateng.java.redis.config;

import local.ateng.java.redis.listener.RedisMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * RedisConfig 类是一个 Spring 配置类，用于配置 Redis 的消息监听器容器。
 * 该配置类定义了 Redis 消息的订阅和处理机制，允许应用监听指定的 Redis 频道，接收并处理消息。
 */
@Configuration  // 注解标识此类是一个配置类，Spring 将在启动时加载该配置
public class RedisConfig {

    /**
     * 创建一个 RedisMessageListenerContainer，用于监听 Redis 频道中的消息。
     * 它会管理消息监听器的生命周期，确保可以接收来自 Redis 发布/订阅模式的消息。
     *
     * @param factory         Redis 连接工厂，用于创建 Redis 连接
     * @param messageListener 消息监听器，接收到消息时会执行处理逻辑
     * @param topic           订阅的 Redis 频道
     * @return RedisMessageListenerContainer 实例
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory,
                                                                       MessageListener messageListener,
                                                                       Topic topic) {
        // 创建一个 Redis 消息监听容器
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        // 设置 Redis 连接工厂，确保 Redis 连接的正确配置
        container.setConnectionFactory(factory);

        // 添加消息监听器和订阅的 Redis 频道
        container.addMessageListener(messageListener, topic);

        // 返回 Redis 消息监听容器
        return container;
    }

    /**
     * 创建一个 Topic 对象，用于指定订阅的 Redis 频道。
     * 此处使用的是 PatternTopic，表示订阅一个具体的频道名称。
     *
     * @return Topic 对象，表示一个 Redis 频道
     */
    @Bean
    public Topic topic() {
        // 返回一个新的 PatternTopic，表示订阅名为 "myChannel" 的 Redis 频道
        return new org.springframework.data.redis.listener.PatternTopic("myChannel");
    }

    /**
     * 创建一个消息监听器，用于接收来自 Redis 频道的消息。
     * 该监听器将处理接收到的消息，并触发相应的回调方法。
     *
     * @return MessageListener 实例，处理来自 Redis 的消息
     */
    @Bean
    public MessageListener messageListener() {
        // 使用 MessageListenerAdapter 封装 RedisMessageListener，确保消息传递和处理逻辑的适配
        return new MessageListenerAdapter(new RedisMessageListener());
    }

}
```

### 发送消息

创建接口发送消息测试。

这里使用了StringRedisTemplate而不是RedisTemplate，是因为做了Fastjson2序列化会导致序列化机制不一致，消息会变得不可读。

```java
package local.ateng.java.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisController {
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping("/send")
    public String sendMessage() {
        stringRedisTemplate.convertAndSend("myChannel", "Hello from Redis!");
        return "Message Sent!";
    }
}
```

![image-20250217112843688](./assets/image-20250217112843688.png)



## Lua脚本

### 使用示例

```java
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisScript {
    private final RedisTemplate redisTemplate;

    @Test
    public void test() {
        boolean success = setIfAbsentWithExpire("myKey", "someValue", 60);
        System.out.println("是否设置成功: " + success);
    }

    /**
     * 尝试设置一个键值对到 Redis 中，只有当该 key 不存在时才设置，并带有过期时间。
     *
     * @param key           要设置的 Redis key
     * @param value         要设置的值
     * @param expireSeconds 过期时间（单位：秒）
     * @return 如果设置成功（即 key 原本不存在），返回 true；否则返回 false
     */
    public boolean setIfAbsentWithExpire(String key, String value, int expireSeconds) {
        // Lua 脚本：如果 key 不存在，则 setex 设置键值并设置过期时间，否则不做任何操作
        String script = "" +
                // 判断 key 是否存在（返回 0 表示不存在）
                "if redis.call('exists', KEYS[1]) == 0 then " +
                // 如果不存在，则 setex 设置 key，ARGV[2] 是过期时间
                "   redis.call('setex', KEYS[1], ARGV[2], ARGV[1]); " +
                // 设置成功，返回 1
                "   return 1; " +
                "else " +
                // 如果已存在，返回 0
                "   return 0; " +
                "end";

        // 构造 RedisScript 对象，指定返回类型为 Long（Lua 返回 1 或 0）
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        // 执行脚本：传入 key（作为 KEYS[1]），value 和 expireSeconds（作为 ARGV[1] 和 ARGV[2]）
        Long result = (Long) redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),  // 只有一个 key
                value,                           // ARGV[1]：要设置的值
                expireSeconds                    // ARGV[2]：过期时间
        );

        // 返回脚本执行结果（1 表示设置成功，0 表示 key 已存在）
        return result != null && result == 1;
    }
}
```

在项目中使用 Redis Lua 脚本主要是为了**原子性操作**和**提高性能**。以下是一些常见场景及对应的 Redis Lua 脚本：

------

### 1. **分布式锁（简单实现）**

```lua
-- 尝试设置锁
-- KEYS[1]: 锁的key
-- ARGV[1]: 锁的值（通常是UUID）
-- ARGV[2]: 过期时间（单位：秒）

if redis.call('SETNX', KEYS[1], ARGV[1]) == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
    return 1
else
    return 0
end
```

------

### 2. **分布式锁释放（避免误删）**

```lua
-- 安全释放锁
-- KEYS[1]: 锁的key
-- ARGV[1]: 请求者设置的值（必须匹配才能删除）

if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
else
    return 0
end
```

------

### 3. **限流器（令牌桶/漏斗）**

```lua
-- 固定窗口限流（每分钟允许访问N次）
-- KEYS[1]: 计数器key
-- ARGV[1]: 限流阈值
-- ARGV[2]: 过期时间（单位：秒）

local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end

if current > tonumber(ARGV[1]) then
    return 0 -- 拒绝
else
    return 1 -- 允许
end
```

------

### 4. **队列的原子弹出多个元素**

```lua
-- 从列表中原子弹出N个元素（例如实现批处理）
-- KEYS[1]: 列表key
-- ARGV[1]: 弹出的数量N

local result = {}
for i = 1, tonumber(ARGV[1]) do
    local item = redis.call('LPOP', KEYS[1])
    if not item then
        break
    end
    table.insert(result, item)
end
return result
```

------

### 5. **库存扣减（防止超卖）**

```lua
-- KEYS[1]: 商品库存key
-- ARGV[1]: 扣减的数量

local stock = tonumber(redis.call('GET', KEYS[1]))
local reduce = tonumber(ARGV[1])
if stock >= reduce then
    return redis.call('DECRBY', KEYS[1], reduce)
else
    return -1
end
```

------

### 6. **Set 集合的幂等写入 + 限长**

```lua
-- KEYS[1]: Set 集合key
-- ARGV[1]: 要加入的元素
-- ARGV[2]: 最大长度

local added = redis.call('SADD', KEYS[1], ARGV[1])
if redis.call('SCARD', KEYS[1]) > tonumber(ARGV[2]) then
    redis.call('SPOP', KEYS[1])  -- 随机踢出一个
end
return added
```



## RedisService

提供了常用的服务类，详情见代码：

```
RedisService：local.ateng.java.redisjdk8.service.RedisService
RedisServiceImpl:local.ateng.java.redisjdk8.service.impl.RedisServiceImpl
```



## 签到功能 BitMap

### 创建功能接口

```java
package local.ateng.java.redis.controller;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Bitmap 控制器
 *
 * @author Ateng
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/bitmap")
@RequiredArgsConstructor
public class BitmapController {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置某一位为 true（签到）
     *
     * @param key    Redis Key
     * @param offset 偏移量（从0开始）
     * @return 是否成功
     */
    @PostMapping("/set")
    public Boolean setBit(@RequestParam String key,
                          @RequestParam Long offset) {
        if (ObjectUtil.hasEmpty(key, offset)) {
            return false;
        }
        return redisTemplate.opsForValue().setBit(key, offset, true);
    }

    /**
     * 获取某一位的值
     *
     * @param key    Redis Key
     * @param offset 偏移量
     * @return true/false
     */
    @GetMapping("/get")
    public Boolean getBit(@RequestParam String key,
                          @RequestParam Long offset) {
        if (ObjectUtil.hasEmpty(key, offset)) {
            return false;
        }
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 统计 Bitmap 中 1 的个数（签到总天数）
     *
     * @param key Redis Key
     * @return 数量
     */
    @GetMapping("/count")
    public Long bitCount(@RequestParam String key) {
        if (ObjectUtil.isEmpty(key)) {
            return 0L;
        }
        return redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.bitCount(key.getBytes())
        );
    }

    /**
     * 获取某一段 Bitmap（用于分析）
     *
     * @param key   Redis Key
     * @param start 开始位
     * @param end   结束位
     * @return bit 列表
     */
    @GetMapping("/range")
    public List<Integer> getBitRange(@RequestParam String key,
                                     @RequestParam Integer start,
                                     @RequestParam Integer end) {
        if (ObjectUtil.hasEmpty(key, start, end)) {
            return new ArrayList<>();
        }

        return redisTemplate.execute((RedisCallback<List<Integer>>) connection -> {
            List<Integer> result = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                Boolean bit = connection.getBit(key.getBytes(), i);
                result.add(Boolean.TRUE.equals(bit) ? 1 : 0);
            }
            return result;
        });
    }

    /**
     * 计算连续签到天数（从当前 offset 往前统计）
     *
     * @param key    Redis Key
     * @param offset 当前天（例如：今天是第几天 - 1）
     * @return 连续签到天数
     */
    @GetMapping("/continuous")
    public Integer getContinuousSignCount(@RequestParam String key,
                                          @RequestParam Integer offset) {
        if (ObjectUtil.hasEmpty(key, offset)) {
            return 0;
        }

        return redisTemplate.execute((RedisCallback<Integer>) connection -> {
            int count = 0;

            for (int i = offset; i >= 0; i--) {
                Boolean bit = connection.getBit(key.getBytes(), i);
                if (Boolean.TRUE.equals(bit)) {
                    count++;
                } else {
                    break;
                }
            }
            return count;
        });
    }

    /**
     * 使用 BITFIELD 获取连续签到天数（优化连续签到性能）
     *
     * @param key    Redis Key
     * @param offset 今天是第几天（day-1）
     * @param limit  最大回溯天数（建议 7 / 16 / 32 / 64）
     * @return long 值（二进制表示）
     */
    @GetMapping("/bitfield")
    public Integer getContinuousByBitField(@RequestParam String key,
                                           @RequestParam Integer offset,
                                           @RequestParam(defaultValue = "64") Integer limit) {

        if (ObjectUtil.hasEmpty(key, offset, limit)) {
            return 0;
        }

        return redisTemplate.execute((RedisCallback<Integer>) connection -> {

            /**
             * 计算起始位置（防止负数）
             */
            int start = offset - limit + 1;
            if (start < 0) {
                start = 0;
            }

            /**
             * 实际读取长度
             */
            int realLimit = offset - start + 1;

            List<Long> result = connection.bitField(
                    redisTemplate.getStringSerializer().serialize(key),
                    BitFieldSubCommands.create()
                            .get(BitFieldSubCommands.BitFieldType.unsigned(realLimit))
                            .valueAt(start)
            );

            if (ObjectUtil.isEmpty(result)) {
                return 0;
            }

            Long num = result.get(0);
            if (num == null || num == 0) {
                return 0;
            }

            /**
             * 位运算：计算连续签到天数（从最低位开始）
             */
            int count = 0;
            while ((num & 1) == 1) {
                count++;
                num >>= 1;
            }

            return count;
        });
    }

    /**
     * 清空 Bitmap
     *
     * @param key Redis Key
     * @return 是否删除成功
     */
    @DeleteMapping("/clear")
    public Boolean clear(@RequestParam String key) {
        if (ObjectUtil.isEmpty(key)) {
            return false;
        }
        return redisTemplate.delete(key);
    }
}
```

### 调用接口功能

设置签到（setBit）

```
POST /bitmap/set?key=sign:1001:202604&offset=8
```

查询是否签到（getBit）

```
GET /bitmap/get?key=sign:1001:202604&offset=8
```

统计签到总天数（bitCount）

```
GET /bitmap/count?key=sign:1001:202604
```

获取签到区间（range）

```
GET /bitmap/range?key=sign:1001:202604&start=0&end=8
```

连续签到天数（continuous）

```
GET /bitmap/continuous?key=sign:1001:202604&offset=8
```

使用 BITFIELD 获取连续签到天数（优化连续签到性能）

```
GET /bitmap/bitfield?key=sign:1001:202604&offset=8&limit=16
```

清空数据（clear）

```
DELETE /bitmap/clear?key=sign:1001:202604
```

