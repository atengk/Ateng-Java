# Jackson

[Jackson](https://github.com/FasterXML/jackson) 是一个 Java 的 JSON 处理库，广泛用于对象与 JSON 之间的转换（序列化和反序列化）。Spring Boot 默认集成了 Jackson，并将其作为 `spring-boot-starter-web` 依赖的一部分来处理 JSON 数据。

以下是序列化和反序列化的应用场景

| **应用场景**                       | **序列化**                  | **反序列化**              |
| ---------------------------------- | --------------------------- | ------------------------- |
| **Spring Boot API** 返回 JSON 响应 | Java 对象 → JSON            | 前端请求 JSON → Java 对象 |
| **数据库存储 JSON**                | Java 对象 → JSON 存储       | 读取 JSON → Java 对象     |
| **Redis 缓存**                     | Java 对象 → JSON 存入 Redis | 取出 JSON → Java 对象     |
| **消息队列（MQ）**                 | Java 对象 → JSON 发送       | 监听 JSON → Java 对象     |

- [Jackson使用文档](/tools/jackson3/README.md)



## 配置 ObjectMapper 构建工厂

Jackson ObjectMapper 统一构建工厂

```java
package io.github.atengk.serialize.config;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson JsonMapper 统一构建工厂。
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
public final class JacksonJsonMapperFactory {

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
     * 定义日期时间格式（精确到秒）。
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 定义日期格式（不包含时间）。
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 定义时间格式（仅时间部分）。
     */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 禁止实例化。
     */
    private JacksonJsonMapperFactory() {
    }

    /**
     * 构建默认 JsonMapper。
     *
     * <p>
     * 默认配置仅保留所有场景都需要的公共能力，适合通用 JSON 处理。
     * </p>
     *
     * @return 默认 JsonMapper
     */
    public static JsonMapper buildDefaultJsonMapper() {
        JsonMapper.Builder builder = baseBuilder();
        return builder.build();
    }

    /**
     * 构建用于 Redis / 数据库存储的 JsonMapper。
     *
     * <p>
     * 该配置优先保证序列化结果稳定、类型信息完整、BigDecimal 精度不丢失，并支持异常对象与复杂对象结构。
     * </p>
     *
     * @return 存储场景专用 JsonMapper
     */
    public static JsonMapper buildStorageJsonMapper() {
        JsonMapper.Builder builder = baseBuilder();
        applyVisibility(
                builder,
                JsonAutoDetect.Visibility.ANY,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE
        );
        applyStableSerialization(builder);
        applyDefaultTyping(builder);
        applyThrowableMixIn(builder);
        return builder.build();
    }

    /**
     * 构建用于 Spring Web（前后端交互）的 JsonMapper。
     *
     * <p>
     * 该配置优先保证接口输出可读、输入兼容、反序列化安全边界清晰，适合 Controller 层统一使用。
     * </p>
     *
     * @return Web 场景专用 JsonMapper
     */
    public static JsonMapper buildWebJsonMapper() {
        JsonMapper.Builder builder = baseBuilder();
        applyVisibility(
                builder,
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                JsonAutoDetect.Visibility.DEFAULT
        );
        registerUnifiedDateTimeModule(builder);
        configureLegacyDateFormat(builder);
        applyLenientDeserialization(builder);
        applyJsonReadFeature(builder);
        applyNumberSerialization(builder);
        disableStableSerializationOptions(builder);
        return builder.build();
    }

    /**
     * 构建用于审计日志（Audit Log）的 JsonMapper。
     *
     * <p>
     * 该配置用于日志落库、操作审计、数据变更记录等场景，
     * 重点保证序列化结果具备稳定性、完整性、可追溯性。
     * </p>
     *
     * @return 审计日志专用 JsonMapper
     */
    public static JsonMapper buildAuditJsonMapper() {
        JsonMapper.Builder builder = baseBuilder();
        applyVisibility(
                builder,
                JsonAutoDetect.Visibility.ANY,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE,
                JsonAutoDetect.Visibility.NONE
        );
        applyStableSerialization(builder);
        configureLegacyDateFormat(builder);
        applyNumberSerialization(builder);
        applyDefaultTyping(builder);
        return builder.build();
    }

    /**
     * 构建基础 Builder。
     *
     * <p>
     * 该方法只负责装载所有场景都需要的公共能力，包括时区、空值策略和未知字段处理策略。
     * 具体差异化能力由上层构建方法按场景追加。
     * </p>
     *
     * @return 基础 Builder
     */
    private static JsonMapper.Builder baseBuilder() {

        // 创建 JsonMapper Builder，用于统一 JSON 序列化与反序列化配置
        JsonMapper.Builder builder = JsonMapper.builder();

        // 设置全局默认时区，确保时间序列化与反序列化行为一致
        builder.defaultTimeZone(TimeZone.getTimeZone(DEFAULT_TIME_ZONE_ID));

        // 禁用时间戳格式输出，统一使用字符串格式，提升可读性
        builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 禁用空 Bean 序列化失败，避免无属性对象导致异常
        builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 保留 null 字段
        builder.changeDefaultPropertyInclusion(incl ->
                JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS));

        // 反序列化时忽略未知字段，增强兼容性，避免字段扩展导致失败
        builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 返回基础配置完成的 Builder
        return builder;
    }

    /**
     * 统一设置对象可见性策略。
     *
     * @param builder            Builder 实例
     * @param fieldVisibility    字段可见性
     * @param getterVisibility   getter 可见性
     * @param setterVisibility   setter 可见性
     * @param isGetterVisibility isGetter 可见性
     * @param creatorVisibility  构造器可见性
     */
    private static void applyVisibility(JsonMapper.Builder builder,
                                        JsonAutoDetect.Visibility fieldVisibility,
                                        JsonAutoDetect.Visibility getterVisibility,
                                        JsonAutoDetect.Visibility setterVisibility,
                                        JsonAutoDetect.Visibility isGetterVisibility,
                                        JsonAutoDetect.Visibility creatorVisibility) {
        builder.changeDefaultVisibility(vc -> vc
                .withFieldVisibility(fieldVisibility)
                .withGetterVisibility(getterVisibility)
                .withSetterVisibility(setterVisibility)
                .withIsGetterVisibility(isGetterVisibility)
                .withCreatorVisibility(creatorVisibility));
    }

    /**
     * 启用存储场景所需的稳定序列化能力。
     *
     * <p>
     * 该配置用于保证序列化结果具备较强可比性和可读性，同时避免 BigDecimal 精度问题。
     * </p>
     *
     * @param builder Builder 实例
     */
    private static void applyStableSerialization(JsonMapper.Builder builder) {

        // 启用 BigDecimal 按原始字符串输出，避免科学计数法导致精度或格式问题
        builder.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);

        // 启用属性按字母排序，保证序列化结果稳定，便于缓存比对与签名计算
        builder.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

        // 启用 Map 按 key 排序，确保输出顺序一致
        builder.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    /**
     * 注册统一日期时间模块。
     *
     * <p>
     * 用于统一 java.time 各类型的序列化与反序列化策略，
     * 明确区分“展示格式”和“时间语义格式”，避免时区信息丢失问题。
     * </p>
     *
     * @param builder Builder 实例
     */
    private static void registerUnifiedDateTimeModule(JsonMapper.Builder builder) {

        // 构建统一时区（用于默认时间处理）
        ZoneId zoneId = ZoneId.of(DEFAULT_TIME_ZONE_ID);

        // 构建日期时间格式化器（精确到秒）
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

        // 构建日期格式化器
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

        // 构建时间格式化器
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

        // 创建自定义模块，用于统一管理时间序列化规则
        SimpleModule module = new SimpleModule();

        // 注册 LocalDateTime 序列化器
        module.addSerializer(LocalDateTime.class, new ValueSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeString(dateTimeFormatter.format(value));
            }
        });

        // 注册 LocalDateTime 反序列化器
        module.addDeserializer(LocalDateTime.class, new ValueDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return LocalDateTime.parse(text, dateTimeFormatter);
            }
        });

        // 注册 LocalDate 序列化器
        module.addSerializer(LocalDate.class, new ValueSerializer<LocalDate>() {
            @Override
            public void serialize(LocalDate value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeString(dateFormatter.format(value));
            }
        });

        // 注册 LocalDate 反序列化器
        module.addDeserializer(LocalDate.class, new ValueDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return LocalDate.parse(text, dateFormatter);
            }
        });

        // 注册 LocalTime 序列化器
        module.addSerializer(LocalTime.class, new ValueSerializer<LocalTime>() {
            @Override
            public void serialize(LocalTime value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeString(timeFormatter.format(value));
            }
        });

        // 注册 LocalTime 反序列化器
        module.addDeserializer(LocalTime.class, new ValueDeserializer<LocalTime>() {
            @Override
            public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return LocalTime.parse(text, timeFormatter);
            }
        });

        // 注册 Instant 序列化器（统一输出为 UTC 标准时间）
        module.addSerializer(Instant.class, new ValueSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value));
            }
        });

        // 注册 Instant 反序列化器（基于 ISO_INSTANT 解析）
        module.addDeserializer(Instant.class, new ValueDeserializer<Instant>() {
            @Override
            public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return Instant.parse(text);
            }
        });

        // 注册 OffsetDateTime 序列化器（保留 offset 信息）
        module.addSerializer(OffsetDateTime.class, new ValueSerializer<OffsetDateTime>() {
            @Override
            public void serialize(OffsetDateTime value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
            }
        });

        // 注册 OffsetDateTime 反序列化器
        module.addDeserializer(OffsetDateTime.class, new ValueDeserializer<OffsetDateTime>() {
            @Override
            public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
        });

        // 注册 ZonedDateTime 序列化器（保留完整时区信息）
        module.addSerializer(ZonedDateTime.class, new ValueSerializer<ZonedDateTime>() {
            @Override
            public void serialize(ZonedDateTime value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeString(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value));
            }
        });

        // 注册 ZonedDateTime 反序列化器
        module.addDeserializer(ZonedDateTime.class, new ValueDeserializer<ZonedDateTime>() {
            @Override
            public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
                String text = p.getText();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return ZonedDateTime.parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            }
        });

        // 注册时间模块
        builder.addModule(module);

        // 设置全局时区
        builder.defaultTimeZone(TimeZone.getTimeZone(zoneId));

    }

    /**
     * 配置传统 Date 类型的全局格式。
     *
     * <p>
     * 用于统一 java.util.Date 的序列化与反序列化行为，
     * 避免与 java.time 类型出现格式不一致问题。
     * </p>
     *
     * @param builder Builder 实例
     */
    private static void configureLegacyDateFormat(JsonMapper.Builder builder) {

        // 创建日期格式化对象（非线程安全，但仅用于构建阶段）
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);

        // 设置统一时区
        dateFormat.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIME_ZONE_ID));

        // 应用到 Builder
        builder.defaultDateFormat(dateFormat);
    }

    /**
     * 应用宽松反序列化策略。
     *
     * <p>
     * 用于增强接口输入的容错能力，适用于 Web 场景，
     * 避免因前端类型不规范导致反序列化失败。
     * </p>
     *
     * @param builder Builder 实例
     */
    private static void applyLenientDeserialization(JsonMapper.Builder builder) {

        // 允许字符串转数字（"1" -> 1）
        builder.enable(MapperFeature.ALLOW_COERCION_OF_SCALARS);

        // 允许单值当数组使用（"a" -> ["a"]）
        builder.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    }

    /**
     * 应用 JSON 读取容错特性。
     *
     * <p>
     * 用于兼容非标准 JSON 输入（常见于前端或第三方系统），
     * 提升系统整体兼容性。
     * </p>
     *
     * @param builder Builder 实例
     */
    private static void applyJsonReadFeature(JsonMapper.Builder builder) {

        // 允许 JSON 末尾存在多余逗号
        builder.configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true);

        // 允许 JSON 中带注释，方便开发阶段使用
        builder.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true);

        // 允许字段名不带引号
        builder.configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, true);

        // 允许单引号作为 JSON 字符串的定界符
        builder.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true);

        // 允许控制字符不经转义直接出现在字符串中
        builder.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true);

        // 允许反斜杠转义任何字符
        builder.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        // 允许无效的 UTF-8 字符或未定义内容
        builder.configure(StreamReadFeature.IGNORE_UNDEFINED, true);

        // 允许 JSON 中出现非数值字面量（NaN / Infinity）
        builder.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS, true);
    }

    /**
     * 统一数值序列化策略（安全 + 前端兼容）。
     *
     * <p>
     * 解决 Java Long / BigInteger 在前端 JS 精度丢失问题。
     * </p>
     *
     * @param builder Builder 实例
     */
    private static void applyNumberSerialization(JsonMapper.Builder builder) {

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
        builder.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        // 防止 int 溢出
        builder.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

        // 将模块注册到 Builder
        builder.addModule(module);
    }

    /**
     * 关闭 Web 场景不需要的稳定排序能力。
     *
     * <p>
     * Web 层更关注接口可读性与自然输出顺序，因此不强制属性和 Map 键排序。
     * </p>
     *
     * @param builder Builder 实例
     */
    private static void disableStableSerializationOptions(JsonMapper.Builder builder) {

        // 关闭属性字母排序，保留原始定义顺序，提高可读性
        builder.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

        // 关闭 Map key 排序，避免影响前端展示顺序
        builder.disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    /**
     * 启用默认多态能力，并限制反序列化来源类型范围。
     *
     * <p>
     * 仅允许业务包前缀和常用 JDK 容器类型参与 default typing，用于收敛反序列化攻击面。
     * </p>
     *
     * @param builder Builder 实例
     */
    private static void applyDefaultTyping(JsonMapper.Builder builder) {

        // 反序列化时遇到非法或未被允许的子类型直接失败
        builder.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

        // 启用默认多态机制，用于保留对象类型信息（适用于 Object 或抽象类型）
        builder.activateDefaultTyping(

                // 使用受限的多态校验器，控制可反序列化的类型范围
                buildPolymorphicTypeValidator(),

                // 仅对非 final 类启用类型信息
                DefaultTyping.NON_FINAL,

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
     * @param builder Builder 实例
     */
    private static void applyThrowableMixIn(JsonMapper.Builder builder) {

        // 为 Throwable 类型绑定混入类，增强异常对象序列化能力
        builder.addMixIn(Throwable.class, ThrowableMixIn.class);
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
    private static tools.jackson.databind.jsontype.PolymorphicTypeValidator buildPolymorphicTypeValidator() {

        // 创建多态类型校验器构建器
        tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator.Builder builder =
                tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder();

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



## Spring Web MVC序列化和反序列化

在 Spring Boot 中，定义 `@Bean ObjectMapper` 会 **覆盖默认的 JSON 配置**，影响 **`@RestController` 返回值**、`@RequestBody` 解析、以及 `@Autowired ObjectMapper` 注入。这样可以 **统一全局 JSON 格式**（如时间格式、属性命名）并 **修改 Jackson 默认行为**，确保应用中的 JSON 处理符合需求。如果不定义，Spring Boot 会使用默认 `ObjectMapper`，但无法定制其行为。

### 配置

```java
package io.github.atengk.serialize.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson Web 层 JsonMapper 配置。
 *
 * <p>
 * 向 Spring 容器注册自定义 JsonMapper，用于统一控制 Web（Controller）层
 * 的 JSON 序列化与反序列化行为。
 * </p>
 *
 * <p>
 * 该配置会覆盖 Spring Boot 默认的 Jackson 3 自动配置结果。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-13
 */
@Configuration
public class WebJacksonConfig {

    /**
     * Web 场景 JsonMapper。
     *
     * @return Web 场景 JsonMapper
     */
    @Bean
    public JsonMapper jsonMapper() {
        return JacksonJsonMapperFactory.buildWebJsonMapper();
    }

}
```

### 使用

```java
package io.github.atengk.serialize.controller;

import io.github.atengk.serialize.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/jackson")
@RequiredArgsConstructor
public class JacksonController {

    // 序列化
    @GetMapping("/serialize")
    public MyUser serialize() {
        Map<String, Object> map = Map.of("name", "ateng", "age", 26L);
        return MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(25)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("1E+20"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province(null)
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .createTime2(new Date())
                .list(List.of("1", "2"))
                .set(Set.of("1", "2", "3"))
                .map(new HashMap<>(map))
                .aBBCCdd("aBBCCdd")
                .build();
    }

    // 反序列化
    @PostMapping("/deserialize")
    public String deserialize(@RequestBody MyUser myUser) {
        System.out.println(myUser);
        return "ok";
    }

    // 反序列化
    @PostMapping("/deserialize2")
    public String deserialize2(@RequestBody Map<String, Object> myUser) {
        System.out.println(myUser);
        return "ok";
    }

    // 反序列化
    @PostMapping("/deserialize4")
    public String deserialize4(@RequestBody List<MyUser> myUserList) {
        System.out.println(myUserList);
        return "ok";
    }

}

```

**访问序列化接口**

```
curl -X GET http://localhost:12014/jackson/serialize
```

示例输出：

```json
{
  "id": "1",
  "name": "ateng",
  "age": 25,
  "phoneNumber": "1762306666",
  "email": "kongyu2385569970@gmail.com",
  "score": 1E+20,
  "ratio": 0.7147,
  "birthday": "2000-01-01",
  "province": "/",
  "city": "重庆市",
  "createTime": "2026-04-14 08:16:30",
  "createTime2": "2026-04-14 08:16:30",
  "createTime3": null,
  "num": 0,
  "list": [
    "1",
    "2"
  ],
  "set": [
    "1",
    "2",
    "3"
  ],
  "map": {
    "name": "ateng",
    "age": "26"
  }
}
```

**访问反序列化接口**

```
curl -X POST http://192.168.100.2:12014/jackson/deserialize \
     -H "Content-Type: application/json" \
     -d '{
  "id": "1",
  "name": "ateng",
  "age": 25,
  "phoneNumber": "1762306666",
  "email": "kongyu2385569970@gmail.com",
  "score": 1E+20,
  "ratio": 0.7147,
  "birthday": "2000-01-01",
  "province": "/",
  "city": "重庆市",
  "createTime": "2026-04-14 08:16:30",
  "createTime2": "2026-04-14 08:16:30",
  "createTime3": null,
  "num": 0,
  "list": [
    "1",
    "2"
  ],
  "set": [
    "1",
    "2",
    "3"
  ],
  "map": {
    "name": "ateng",
    "age": "26"
  }
}'
```

控制台打印

```
MyUser(id=1, name=ateng, age=25, phoneNumber=1762306666, email=kongyu2385569970@gmail.com, score=1E+20, ratio=0.7147, birthday=2000-01-01, province=/, city=重庆市, createTime=2026-04-14T08:16:30, createTime2=Tue Apr 14 08:16:30 CST 2026, createTime3=null, num=0, list=[1, 2], set=[1, 2, 3], map={name=ateng, age=26})
```



## Spring Data Redis序列化和反序列化

在 **Spring Data Redis** 中，Jackson 主要用于将 Java 对象序列化为 JSON 存入 Redis，并在读取时反序列化回 Java 对象。由于 Redis 只能存储字符串或二进制数据，因此 `RedisTemplate` 需要配置合适的序列化器，如 `Jackson2JsonRedisSerializer`，以确保对象能正确存储和恢复。

### 配置

```java
package io.github.atengk.serialize.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * RedisTemplate 配置类，统一定义 Key/Value 的序列化策略。
 * Key 使用字符串序列化，Value 使用 Jackson JSON 序列化。
 *
 * @author Ateng
 * @since 2026-04-14
 */
@Configuration
public class RedisTemplateConfig {

    /**
     * 构建并初始化 RedisTemplate，配置序列化器。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 设置 Redis 连接工厂
        template.setConnectionFactory(redisConnectionFactory);

        // 创建字符串序列化器（用于 Key）
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置 Key 序列化器
        template.setKeySerializer(stringRedisSerializer);

        // 设置 Hash Key 序列化器
        template.setHashKeySerializer(stringRedisSerializer);

        // 构建自定义 JsonMapper（统一 JSON 规则）
        JsonMapper jsonMapper = JacksonJsonMapperFactory.buildStorageJsonMapper();

        // 创建 Jackson 序列化器（用于 Value）
        JacksonJsonRedisSerializer<Object> jacksonJsonRedisSerializer =
                new JacksonJsonRedisSerializer<>(jsonMapper, Object.class);

        // 设置 Value 序列化器
        template.setValueSerializer(jacksonJsonRedisSerializer);

        // 设置 Hash Value 序列化器
        template.setHashValueSerializer(jacksonJsonRedisSerializer);

        // 初始化 RedisTemplate
        template.afterPropertiesSet();

        // 返回配置完成的 RedisTemplate
        return template;
    }

}

```

### 使用

```java
package io.github.atengk.serialize.controller;

import io.github.atengk.serialize.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {
    private final RedisTemplate<String, Object> redisTemplate;

    // 序列化
    @GetMapping("/serialize")
    public String serialize() {
        Map<String, Object> map = Map.of("name", "ateng", "age", 26L);
        MyUser myUser = MyUser.builder()
                .id(1L)
                .name("ateng")
                .age(25)
                .phoneNumber("1762306666")
                .email("kongyu2385569970@gmail.com")
                .score(new BigDecimal("1E+20"))
                .ratio(0.7147)
                .birthday(LocalDate.parse("2000-01-01"))
                .province(null)
                .city("重庆市")
                .createTime(LocalDateTime.now())
                .createTime2(new Date())
                .list(List.of("1", "2"))
                .set(Set.of("1", "2", "3"))
                .map(new HashMap<>(map))
                .build();
        redisTemplate.opsForValue().set("myUser", myUser);
        return "ok";
    }

    // 反序列化
    @GetMapping("/deserialize")
    public String deserialize() {
        MyUser myUser = (MyUser) redisTemplate.opsForValue().get("myUser");
        System.out.println(myUser);
        System.out.println(myUser.getCreateTime());
        return "ok";
    }

}

```

序列化到Redis

```json
{
    "@class": "io.github.atengk.serialize.entity.MyUser",
    "aBBCCdd": null,
    "age": 25,
    "birthday": "2000-01-01",
    "city": "重庆市",
    "createTime": "2026-04-14T17:51:28.8368228",
    "createTime2": [
        "java.util.Date",
        "2026-04-14T17:51:28.836+08:00"
    ],
    "createTime3": null,
    "email": "kongyu2385569970@gmail.com",
    "id": 1,
    "list": [
        "java.util.ImmutableCollections$List12",
        [
            "1",
            "2"
        ]
    ],
    "map": {
        "@class": "java.util.HashMap",
        "age": [
            "java.lang.Long",
            26
        ],
        "name": "ateng"
    },
    "name": "ateng",
    "num": 0,
    "phoneNumber": "1762306666",
    "province": null,
    "ratio": 0.7147,
    "score": [
        "java.math.BigDecimal",
        100000000000000000000
    ],
    "set": [
        "java.util.ImmutableCollections$SetN",
        [
            "3",
            "2",
            "1"
        ]
    ]
}
```

反序列化输出

```
MyUser(id=1, name=ateng, age=25, phoneNumber=1762306666, email=kongyu2385569970@gmail.com, score=100000000000000000000, ratio=0.7147, birthday=2000-01-01, province=null, city=重庆市, createTime=2026-04-14T17:48:43.640521, createTime2=Tue Apr 14 17:48:43 CST 2026, createTime3=null, num=0, list=[1, 2], set=[1, 2, 3], map={name=ateng, age=26}, aBBCCdd=null)
2026-04-14T17:48:43.640521
```



## 自定义序列化

### 配置自定义序列化器

```java
package io.github.atengk.serialize.serializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class DefaultValueStringSerializer extends ValueSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (value == null) {
            gen.writeString("/");
        } else {
            gen.writeString(value + "~");
        }
    }
}

```

### 使用

使用 @JsonSerialize 的 using 和 nullsUsing 指定自定义的序列化器，最终序列化后就可以实现自定义

```java
@JsonSerialize(using = DefaultValueStringSerializer.class, nullsUsing = DefaultValueStringSerializer.class)
private String province;
```

