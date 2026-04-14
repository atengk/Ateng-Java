package io.github.atengk.factory;

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
