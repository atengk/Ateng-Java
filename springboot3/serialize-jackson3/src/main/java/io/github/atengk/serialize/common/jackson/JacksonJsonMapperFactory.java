package io.github.atengk.serialize.common.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.introspect.VisibilityChecker;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

/**
 * Jackson JsonMapper 统一构建工厂。
 *
 * <p>
 * 该工厂用于集中管理 Spring Boot 4 / Jackson 3 的 JSON 序列化与反序列化策略，避免不同模块各自创建
 * JsonMapper 导致时间策略、类型信息、字段可见性、数值精度、枚举策略和容错边界不一致。
 * </p>
 *
 * <p>
 * 使用边界：Default 适合通用 JSON 处理；Storage 适合 Redis、数据库 JSON 字段等可信内部存储；Web 适合
 * Controller 入参与出参；Audit 适合操作审计、数据变更记录和日志落库。Storage 会写入 {@code @class}
 * 类型信息，用于恢复 Object、接口、抽象类和集合元素的真实类型；Web 与 Audit 默认不写入类型信息。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class JacksonJsonMapperFactory {

    /**
     * 默认类型信息字段名。
     */
    private static final String TYPE_PROPERTY_NAME = "@class";

    /**
     * 默认信任的业务包前缀。
     */
    private static final List<String> DEFAULT_TRUSTED_BASE_PACKAGES = List.of(
            "io.github.atengk",
            "local.ateng.java"
    );

    /**
     * 允许参与默认类型反序列化的 JDK 包前缀。
     */
    private static final List<String> TRUSTED_JDK_PACKAGE_PREFIXES = List.of(
            "java.util.",
            "java.time.",
            "java.math.",
            "tools.jackson.databind.node."
    );

    /**
     * 允许参与默认类型反序列化的 JDK 精确类名。
     */
    private static final Set<String> TRUSTED_JDK_CLASS_NAMES = Set.of(
            "java.lang.Object",
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Void",
            "java.lang.StackTraceElement",
            "java.lang.Throwable",
            "java.lang.Exception",
            "java.lang.RuntimeException"
    );

    /**
     * 明确拒绝参与默认类型反序列化的类名前缀。
     */
    private static final List<String> DENIED_CLASS_PREFIXES = List.of(
            "com.sun.",
            "jdk.",
            "org.apache.commons.collections.",
            "org.apache.xalan.",
            "sun."
    );

    /**
     * 明确拒绝参与默认类型反序列化的精确类名。
     */
    private static final Set<String> DENIED_CLASS_NAMES = Set.of(
            "java.io.File",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.ProcessBuilder",
            "java.lang.Runtime",
            "java.lang.System",
            "java.nio.file.Path"
    );

    /**
     * JVM 基础类型数组名称。
     */
    private static final Set<String> JVM_PRIMITIVE_ARRAY_NAMES = Set.of(
            "[Z",
            "[B",
            "[S",
            "[I",
            "[J",
            "[F",
            "[D",
            "[C"
    );

    /**
     * 默认时区。
     */
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");

    /**
     * 日期时间格式，精确到秒。
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期时间格式，精确到毫秒。
     */
    private static final String DATE_TIME_MILLIS_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 日期格式。
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式。
     */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 时间格式，精确到毫秒。
     */
    private static final String TIME_MILLIS_PATTERN = "HH:mm:ss.SSS";

    /**
     * 日期时间格式化器。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /**
     * 毫秒日期时间格式化器。
     */
    private static final DateTimeFormatter DATE_TIME_MILLIS_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_MILLIS_PATTERN);

    /**
     * 日期格式化器。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * 时间格式化器。
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    /**
     * 毫秒时间格式化器。
     */
    private static final DateTimeFormatter TIME_MILLIS_FORMATTER = DateTimeFormatter.ofPattern(TIME_MILLIS_PATTERN);

    /**
     * 禁止实例化工具工厂。
     */
    private JacksonJsonMapperFactory() {
    }

    /**
     * 构建默认 JsonMapper。
     *
     * <p>
     * 默认场景只保留公共能力，不做业务时间格式化，不启用 {@code @class}，适合通用 JSON 处理、对象转换和内部工具方法。
     * </p>
     *
     * @return 默认 JsonMapper
     */
    public static JsonMapper buildDefaultJsonMapper() {
        // 基于公共基础配置继续追加当前场景的专属配置。
        return baseBuilder()
                // 默认场景使用 Web 可见性策略，只处理公开成员。
                .changeDefaultVisibility(JacksonJsonMapperFactory::applyWebVisibility)
                // 构建并返回默认场景 JsonMapper 实例。
                .build();
    }

    /**
     * 构建用于 Redis / 数据库存储的 JsonMapper。
     *
     * <p>
     * Storage 会写入 {@code @class} 类型信息，用于恢复 Object、接口、抽象类和集合元素的真实类型。
     * 该 Mapper 只允许可信包和常用 JDK 类型参与默认类型反序列化，且仅建议解析可信内部数据。
     * </p>
     *
     * @return 存储场景专用 JsonMapper
     */
    public static JsonMapper buildStorageJsonMapper() {
        return buildStorageJsonMapper(DEFAULT_TRUSTED_BASE_PACKAGES);
    }

    /**
     * 构建用于 Redis / 数据库存储的 JsonMapper。
     *
     * @param trustedBasePackages 可信业务包前缀
     * @return 存储场景专用 JsonMapper
     */
    public static JsonMapper buildStorageJsonMapper(String... trustedBasePackages) {
        if (trustedBasePackages == null || trustedBasePackages.length == 0) {
            return buildStorageJsonMapper(DEFAULT_TRUSTED_BASE_PACKAGES);
        }
        List<String> packageList = new ArrayList<>(trustedBasePackages.length);
        for (String packageName : trustedBasePackages) {
            if (!isBlank(packageName)) {
                packageList.add(packageName.trim());
            }
        }
        return buildStorageJsonMapper(packageList.isEmpty() ? DEFAULT_TRUSTED_BASE_PACKAGES : packageList);
    }

    /**
     * 构建用于 Redis / 数据库存储的 JsonMapper。
     *
     * @param trustedBasePackages 可信业务包前缀
     * @return 存储场景专用 JsonMapper
     */
    public static JsonMapper buildStorageJsonMapper(Collection<String> trustedBasePackages) {
        // 创建基于公共基础配置的 Builder 实例。
        JsonMapper.Builder builder = baseBuilder();
        // 应用字段优先可见性，优先保留对象内部字段状态。
        applyFieldVisibility(builder);
        // 应用稳定序列化策略，保证输出结果便于比对、签名和审计。
        applyStableSerialization(builder);
        // 启用受限默认多态类型信息，用于可信内部存储类型恢复。
        applyDefaultTyping(builder, trustedBasePackages);
        // 挂载存储场景异常对象 MixIn，增强 Throwable 序列化兼容性。
        applyStorageThrowableMixIn(builder);
        // 构建并返回当前场景配置完成后的 JsonMapper 实例。
        return builder.build();
    }

    /**
     * 构建用于 Spring Web 的 JsonMapper。
     *
     * <p>
     * Web 场景对外输出业务时间格式，Long / BigInteger 输出为字符串以规避 JavaScript 整数精度问题，
     * 同时保留常见接口入参兼容能力。该场景不启用 {@link DeserializationFeature#USE_BIG_INTEGER_FOR_INTS}，
     * 避免 Map / Object 入参中的普通整数全部变成 BigInteger。
     * </p>
     *
     * @return Web 场景专用 JsonMapper
     */
    public static JsonMapper buildWebJsonMapper() {
        // 创建基于公共基础配置的 Builder 实例。
        JsonMapper.Builder builder = baseBuilder();
        // 应用 Web 场景公开成员可见性，契合 DTO 常规访问方式。
        applyWebVisibility(builder);
        // 注册统一日期时间模块，规范 java.time 类型的输入输出格式。
        registerUnifiedDateTimeModule(builder);
        // 配置传统 Date 类型格式，保持 Date 与 java.time 输出风格一致。
        configureLegacyDateFormat(builder);
        // 应用接口入参容错反序列化策略。
        applyLenientDeserialization(builder);
        // 应用非标准 JSON 读取兼容特性。
        applyJsonReadFeature(builder);
        // 应用 Web 数值序列化策略，Long 和 BigInteger 输出为字符串。
        applyNumberSerialization(builder, false);
        // 应用枚举序列化与反序列化策略。
        applyEnumFeature(builder);
        // 构建并返回当前场景配置完成后的 JsonMapper 实例。
        return builder.build();
    }

    /**
     * 构建用于审计日志的 JsonMapper。
     *
     * <p>
     * 审计日志优先保证可读性、字段完整性、输出稳定性和跨系统检索友好性；默认不写入 {@code @class}，
     * 避免日志与 Java 类名强耦合。
     * </p>
     *
     * @return 审计日志专用 JsonMapper
     */
    public static JsonMapper buildAuditJsonMapper() {
        // 创建基于公共基础配置的 Builder 实例。
        JsonMapper.Builder builder = baseBuilder();
        // 应用字段优先可见性，优先保留对象内部字段状态。
        applyFieldVisibility(builder);
        // 应用稳定序列化策略，保证输出结果便于比对、签名和审计。
        applyStableSerialization(builder);
        // 注册统一日期时间模块，规范 java.time 类型的输入输出格式。
        registerUnifiedDateTimeModule(builder);
        // 配置传统 Date 类型格式，保持 Date 与 java.time 输出风格一致。
        configureLegacyDateFormat(builder);
        // 应用审计数值序列化策略，并启用整数 BigInteger 反序列化。
        applyNumberSerialization(builder, true);
        // 应用枚举序列化与反序列化策略。
        applyEnumFeature(builder);
        // 挂载审计场景异常对象 MixIn，避免日志输出与 Java 类型信息强耦合。
        applyAuditThrowableMixIn(builder);
        // 构建并返回当前场景配置完成后的 JsonMapper 实例。
        return builder.build();
    }

    /**
     * 构建基础 Builder。
     *
     * <p>
     * 该方法只负责装载所有场景都需要的公共能力，包括时区、空值策略、时间戳策略和未知字段处理策略。
     * 差异化能力由上层构建方法按场景追加。
     * </p>
     *
     * @return 基础 Builder
     */
    private static JsonMapper.Builder baseBuilder() {
        return JsonMapper.builder()
                // 设置全局默认时区，统一传统 Date 与上下文时间处理。
                .defaultTimeZone(defaultTimeZone())
                // 保留 null 字段，避免接口、存储或审计结构因空值被隐藏。
                .changeDefaultPropertyInclusion(inclusion -> JsonInclude.Value.construct(
                        JsonInclude.Include.ALWAYS,
                        JsonInclude.Include.ALWAYS
                ))
                // 禁用日期时间戳输出，统一使用文本时间表达。
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 禁用 Duration 时间戳输出，保持持续时间文本可读。
                .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                // 禁止反序列化时按上下文时区自动调整时间，避免时间偏移。
                .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                // 禁用空 Bean 序列化失败，避免无属性对象导致异常。
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // 忽略未知字段，提升接口和存储结构演进兼容性。
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 应用 Web 场景对象可见性。
     *
     * @param builder Builder 实例
     */
    private static void applyWebVisibility(JsonMapper.Builder builder) {
        // 将 Web 可见性规则应用到 Builder。
        builder.changeDefaultVisibility(JacksonJsonMapperFactory::applyWebVisibility);
    }

    /**
     * 应用字段优先对象可见性。
     *
     * @param builder Builder 实例
     */
    private static void applyFieldVisibility(JsonMapper.Builder builder) {
        // 将字段优先可见性规则应用到 Builder。
        builder.changeDefaultVisibility(JacksonJsonMapperFactory::applyFieldVisibility);
    }

    /**
     * 设置 Web 场景对象可见性。
     *
     * @param visibilityChecker 可见性检查器
     * @return 调整后的可见性检查器
     */
    private static VisibilityChecker applyWebVisibility(VisibilityChecker visibilityChecker) {
        return visibilityChecker
                // 字段仅允许 public 可见，避免直接暴露私有字段。
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                // getter 仅允许 public 可见，符合常规 JavaBean 输出规则。
                .withGetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                // setter 仅允许 public 可见，符合常规 DTO 入参绑定规则。
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                // isGetter 仅允许 public 可见，支持布尔属性标准输出。
                .withIsGetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                // 构造器探测保持 Jackson 默认策略，兼顾构造器反序列化兼容性。
                .withCreatorVisibility(JsonAutoDetect.Visibility.DEFAULT);
    }

    /**
     * 设置字段优先对象可见性。
     *
     * @param visibilityChecker 可见性检查器
     * @return 调整后的可见性检查器
     */
    private static VisibilityChecker applyFieldVisibility(VisibilityChecker visibilityChecker) {
        return visibilityChecker
                // 字段允许任意可见性，便于存储和审计完整对象状态。
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                // 禁用 getter 探测，避免触发计算属性或重复输出。
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                // 禁用 setter 探测，优先通过字段还原对象状态。
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                // 禁用 isGetter 探测，避免布尔计算属性干扰结构。
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                // 构造器探测保持 Jackson 默认策略，兼顾构造器反序列化兼容性。
                .withCreatorVisibility(JsonAutoDetect.Visibility.DEFAULT);
    }

    /**
     * 启用稳定序列化能力。
     *
     * @param builder Builder 实例
     */
    private static void applyStableSerialization(JsonMapper.Builder builder) {
        // 启用 BigDecimal 普通数字输出，避免科学计数法影响稳定性。
        builder.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        // 启用属性按字母排序，保证对象字段输出顺序稳定。
        builder.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        // 启用 Map 按 Key 排序，保证 Map 输出顺序稳定。
        builder.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    /**
     * 注册统一日期时间模块。
     *
     * @param builder Builder 实例
     */
    private static void registerUnifiedDateTimeModule(JsonMapper.Builder builder) {
        // 创建统一日期时间模块，集中注册 java.time 类型处理规则。
        SimpleModule module = new SimpleModule("AtengUnifiedDateTimeModule");

        // 注册 LocalDateTime 序列化器，输出 yyyy-MM-dd HH:mm:ss。
        module.addSerializer(LocalDateTime.class, new StringValueSerializer<>(
                LocalDateTime.class,
                value -> DATE_TIME_FORMATTER.format(value)
        ));
        // 注册 LocalDateTime 反序列化器，兼容业务格式和 ISO 格式。
        module.addDeserializer(LocalDateTime.class, new StringValueDeserializer<>(
                LocalDateTime.class,
                JacksonJsonMapperFactory::parseLocalDateTime
        ));

        // 注册 LocalDate 序列化器，输出 yyyy-MM-dd。
        module.addSerializer(LocalDate.class, new StringValueSerializer<>(
                LocalDate.class,
                value -> DATE_FORMATTER.format(value)
        ));
        // 注册 LocalDate 反序列化器，兼容业务日期格式和 ISO 日期格式。
        module.addDeserializer(LocalDate.class, new StringValueDeserializer<>(
                LocalDate.class,
                JacksonJsonMapperFactory::parseLocalDate
        ));

        // 注册 LocalTime 序列化器，输出 HH:mm:ss。
        module.addSerializer(LocalTime.class, new StringValueSerializer<>(
                LocalTime.class,
                value -> TIME_FORMATTER.format(value)
        ));
        // 注册 LocalTime 反序列化器，兼容秒级、毫秒级和 ISO 时间格式。
        module.addDeserializer(LocalTime.class, new StringValueDeserializer<>(
                LocalTime.class,
                JacksonJsonMapperFactory::parseLocalTime
        ));

        // 注册 Instant 序列化器，输出 ISO_INSTANT，保留 UTC 时间语义。
        module.addSerializer(Instant.class, new StringValueSerializer<>(
                Instant.class,
                DateTimeFormatter.ISO_INSTANT::format
        ));
        // 注册 Instant 反序列化器，兼容 ISO_INSTANT 和 epoch millis。
        module.addDeserializer(Instant.class, new StringValueDeserializer<>(
                Instant.class,
                JacksonJsonMapperFactory::parseInstant
        ));

        // 注册 OffsetDateTime 序列化器，输出 ISO_OFFSET_DATE_TIME，保留 offset 信息。
        module.addSerializer(OffsetDateTime.class, new StringValueSerializer<>(
                OffsetDateTime.class,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME::format
        ));
        // 注册 OffsetDateTime 反序列化器，按 ISO_OFFSET_DATE_TIME 解析。
        module.addDeserializer(OffsetDateTime.class, new StringValueDeserializer<>(
                OffsetDateTime.class,
                value -> OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        ));

        // 注册 ZonedDateTime 序列化器，输出 ISO_ZONED_DATE_TIME，保留 ZoneId 信息。
        module.addSerializer(ZonedDateTime.class, new StringValueSerializer<>(
                ZonedDateTime.class,
                DateTimeFormatter.ISO_ZONED_DATE_TIME::format
        ));
        // 注册 ZonedDateTime 反序列化器，按 ISO_ZONED_DATE_TIME 解析。
        module.addDeserializer(ZonedDateTime.class, new StringValueDeserializer<>(
                ZonedDateTime.class,
                value -> ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        ));

        // 将当前自定义模块注册到 Builder。
        builder.addModule(module);
    }

    /**
     * 配置传统 Date 类型的全局格式。
     *
     * @param builder Builder 实例
     */
    private static void configureLegacyDateFormat(JsonMapper.Builder builder) {
        // 创建传统 Date 格式化器，使用统一日期时间格式。
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
        // 为传统 Date 格式化器设置默认时区。
        dateFormat.setTimeZone(defaultTimeZone());
        // 将传统 Date 全局格式应用到 Builder。
        builder.defaultDateFormat(dateFormat);
    }

    /**
     * 应用宽松反序列化策略。
     *
     * @param builder Builder 实例
     */
    private static void applyLenientDeserialization(JsonMapper.Builder builder) {
        // 允许标量类型自动转换，例如字符串数字转换为数值类型。
        builder.enable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        // 允许单个值按数组处理，提高接口入参兼容性。
        builder.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        // 未知枚举值反序列化为 null，避免枚举扩展导致接口直接失败。
        builder.enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        // 浮点数反序列化优先使用 BigDecimal，降低精度损失风险。
        builder.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    /**
     * 应用 JSON 读取容错特性。
     *
     * @param builder Builder 实例
     */
    private static void applyJsonReadFeature(JsonMapper.Builder builder) {
        // 允许 JSON 中存在 Java/C 风格注释，兼容部分非严格输入。
        builder.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true);
        // 允许单引号字符串，兼容部分非严格 JSON 输入。
        builder.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true);
        // 允许数组或对象末尾存在多余逗号。
        builder.configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true);
        // 允许字段名不带双引号，兼容非标准 JSON 输入。
        builder.configure(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES, true);
        // 允许字符串中出现未转义控制字符，增强第三方输入兼容性。
        builder.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true);
        // 允许反斜杠转义任意字符，兼容宽松 JSON 输入。
        builder.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        // 允许 NaN、Infinity 等非数值字面量。
        builder.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS, true);
        // 忽略 undefined 内容，兼容部分非标准输入源。
        builder.configure(StreamReadFeature.IGNORE_UNDEFINED, true);
    }

    /**
     * 统一数值序列化策略。
     *
     * @param builder              Builder 实例
     * @param useBigIntegerForInts 是否将整数反序列化为 BigInteger
     */
    private static void applyNumberSerialization(JsonMapper.Builder builder, boolean useBigIntegerForInts) {
        // 创建数值兼容模块，集中注册跨端数值序列化规则。
        SimpleModule module = new SimpleModule("AtengNumberCompatibilityModule");
        // 创建 Long 字符串序列化器，避免前端 JavaScript 整数精度丢失。
        ValueSerializer<Long> longSerializer = new StringValueSerializer<>(Long.class, value -> value.toString());
        // 注册 Long 包装类型序列化器。
        module.addSerializer(Long.class, longSerializer);
        // 注册 long 基本类型序列化器。
        module.addSerializer(Long.TYPE, longSerializer);
        // 注册 BigInteger 字符串序列化器，避免超大整数跨端精度丢失。
        module.addSerializer(BigInteger.class, new StringValueSerializer<>(BigInteger.class, BigInteger::toString));
        // 将当前自定义模块注册到 Builder。
        builder.addModule(module);
        // 浮点数反序列化优先使用 BigDecimal，降低精度损失风险。
        builder.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        if (useBigIntegerForInts) {
            // 整数反序列化优先使用 BigInteger，降低审计回放整数溢出风险。
            builder.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        }
    }

    /**
     * 应用枚举序列化与反序列化策略。
     *
     * @param builder Builder 实例
     */
    private static void applyEnumFeature(JsonMapper.Builder builder) {
        // 枚举序列化使用 toString，便于对外展示值与内部 name 解耦。
        builder.enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING);
        // 枚举反序列化使用 toString，与序列化策略保持一致。
        builder.enable(EnumFeature.READ_ENUMS_USING_TO_STRING);
    }

    /**
     * 启用默认多态能力，并限制反序列化来源类型范围。
     *
     * @param builder             Builder 实例
     * @param trustedBasePackages 可信业务包前缀
     */
    private static void applyDefaultTyping(JsonMapper.Builder builder, Collection<String> trustedBasePackages) {
        // 遇到非法或未被允许的子类型时直接失败，收敛反序列化攻击面。
        builder.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        // 启用默认类型信息，并指定类型信息字段名。
        builder.activateDefaultTypingAsProperty(
                // 使用受限多态类型校验器控制可反序列化类型范围。
                buildPolymorphicTypeValidator(trustedBasePackages),
                // 仅对非 final 类型写入默认类型信息。
                DefaultTyping.NON_FINAL,
                // 使用统一的类型信息字段名。
                TYPE_PROPERTY_NAME
        );
    }

    /**
     * 为存储场景异常类型挂载混入配置。
     *
     * @param builder Builder 实例
     */
    private static void applyStorageThrowableMixIn(JsonMapper.Builder builder) {
        // 为 Throwable 绑定存储场景 MixIn。
        builder.addMixIn(Throwable.class, ThrowableStorageMixIn.class);
    }

    /**
     * 为审计场景异常类型挂载混入配置。
     *
     * @param builder Builder 实例
     */
    private static void applyAuditThrowableMixIn(JsonMapper.Builder builder) {
        // 为 Throwable 绑定审计场景 MixIn。
        builder.addMixIn(Throwable.class, ThrowableAuditMixIn.class);
    }

    /**
     * 构建多态类型校验器。
     *
     * @param trustedBasePackages 可信业务包前缀
     * @return 多态类型校验器
     */
    private static PolymorphicTypeValidator buildPolymorphicTypeValidator(Collection<String> trustedBasePackages) {
        return new TrustedPolymorphicTypeValidator(normalizeTrustedPackages(trustedBasePackages));
    }

    /**
     * 获取默认时区实例。
     *
     * @return 默认时区
     */
    private static TimeZone defaultTimeZone() {
        return TimeZone.getTimeZone(DEFAULT_ZONE_ID);
    }

    /**
     * 规范化可信业务包前缀。
     *
     * @param trustedBasePackages 原始可信业务包前缀
     * @return 规范化后的可信业务包前缀
     */
    private static Set<String> normalizeTrustedPackages(Collection<String> trustedBasePackages) {
        Collection<String> source = trustedBasePackages == null || trustedBasePackages.isEmpty()
                ? DEFAULT_TRUSTED_BASE_PACKAGES
                : trustedBasePackages;
        Set<String> result = new LinkedHashSet<>();
        for (String packageName : source) {
            if (isBlank(packageName)) {
                continue;
            }
            String normalized = packageName.trim();
            result.add(normalized.endsWith(".") ? normalized : normalized + ".");
        }
        if (result.isEmpty()) {
            return normalizeTrustedPackages(DEFAULT_TRUSTED_BASE_PACKAGES);
        }
        return Set.copyOf(result);
    }

    /**
     * 判断类名是否属于明确拒绝范围。
     *
     * @param className 类名
     * @return true 表示拒绝
     */
    private static boolean isDeniedClassName(String className) {
        if (DENIED_CLASS_NAMES.contains(className)) {
            return true;
        }
        for (String prefix : DENIED_CLASS_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断类名是否属于可信范围。
     *
     * @param className           类名
     * @param trustedBasePackages 可信业务包前缀
     * @return true 表示可信
     */
    private static boolean isTrustedClassName(String className, Collection<String> trustedBasePackages) {
        if (isBlank(className)) {
            return false;
        }

        String trimmedClassName = className.trim();
        if (JVM_PRIMITIVE_ARRAY_NAMES.contains(trimmedClassName)) {
            return true;
        }

        String normalizedClassName = normalizeJvmArrayClassName(trimmedClassName);
        if (isBlank(normalizedClassName) || isDeniedClassName(normalizedClassName)) {
            return false;
        }

        if (TRUSTED_JDK_CLASS_NAMES.contains(normalizedClassName)) {
            return true;
        }

        if (normalizedClassName.startsWith("java.lang.")
                && (normalizedClassName.endsWith("Exception") || normalizedClassName.endsWith("Error"))) {
            return true;
        }

        for (String packagePrefix : TRUSTED_JDK_PACKAGE_PREFIXES) {
            if (normalizedClassName.startsWith(packagePrefix)) {
                return true;
            }
        }

        for (String packagePrefix : trustedBasePackages) {
            if (normalizedClassName.startsWith(packagePrefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 规范化 JVM 数组类名。
     *
     * @param className 类名
     * @return 规范化后的类名
     */
    private static String normalizeJvmArrayClassName(String className) {
        String normalizedClassName = className;
        while (normalizedClassName.startsWith("[")) {
            if (JVM_PRIMITIVE_ARRAY_NAMES.contains(normalizedClassName)) {
                return normalizedClassName;
            }
            if (normalizedClassName.startsWith("[L") && normalizedClassName.endsWith(";")) {
                normalizedClassName = normalizedClassName.substring(2, normalizedClassName.length() - 1);
                continue;
            }
            normalizedClassName = normalizedClassName.substring(1);
        }
        return normalizedClassName;
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 字符串值
     * @return true 表示 null、空字符串或纯空白字符串
     */
    private static boolean isBlank(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isWhitespace(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析 LocalDateTime。
     *
     * @param value 字符串值
     * @return LocalDateTime 实例
     */
    private static LocalDateTime parseLocalDateTime(String value) {
        return parseWithFormatters(
                value,
                List.of(DATE_TIME_FORMATTER, DATE_TIME_MILLIS_FORMATTER, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                formatter -> LocalDateTime.parse(value, formatter)
        );
    }

    /**
     * 解析 LocalDate。
     *
     * @param value 字符串值
     * @return LocalDate 实例
     */
    private static LocalDate parseLocalDate(String value) {
        return parseWithFormatters(
                value,
                List.of(DATE_FORMATTER, DateTimeFormatter.ISO_LOCAL_DATE),
                formatter -> LocalDate.parse(value, formatter)
        );
    }

    /**
     * 解析 LocalTime。
     *
     * @param value 字符串值
     * @return LocalTime 实例
     */
    private static LocalTime parseLocalTime(String value) {
        return parseWithFormatters(
                value,
                List.of(TIME_FORMATTER, TIME_MILLIS_FORMATTER, DateTimeFormatter.ISO_LOCAL_TIME),
                formatter -> LocalTime.parse(value, formatter)
        );
    }

    /**
     * 解析 Instant。
     *
     * @param value 字符串值
     * @return Instant 实例
     */
    private static Instant parseInstant(String value) {
        if (isIntegerText(value)) {
            return Instant.ofEpochMilli(Long.parseLong(value));
        }
        return Instant.parse(value);
    }

    /**
     * 按多个格式化器解析值。
     *
     * @param value      字符串值
     * @param formatters 格式化器列表
     * @param parser     解析函数
     * @param <T>        目标类型
     * @return 目标类型实例
     */
    private static <T> T parseWithFormatters(String value, List<DateTimeFormatter> formatters, Function<DateTimeFormatter, T> parser) {
        DateTimeParseException lastException = null;
        for (DateTimeFormatter formatter : formatters) {
            try {
                return parser.apply(formatter);
            } catch (DateTimeParseException ex) {
                lastException = ex;
            }
        }
        throw Objects.requireNonNull(lastException, "日期时间解析失败");
    }

    /**
     * 判断字符串是否为整数文本。
     *
     * @param value 字符串值
     * @return true 表示整数文本
     */
    private static boolean isIntegerText(String value) {
        if (isBlank(value)) {
            return false;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int index = start; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 可信多态类型校验器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class TrustedPolymorphicTypeValidator extends PolymorphicTypeValidator.Base implements Serializable {

        /**
         * 序列化版本号。
         */
        @Serial
        private static final long serialVersionUID = 8750194016448019822L;

        /**
         * 可信业务包前缀。
         */
        private final Set<String> trustedBasePackages;

        /**
         * 创建可信多态类型校验器。
         *
         * @param trustedBasePackages 可信业务包前缀
         */
        private TrustedPolymorphicTypeValidator(Set<String> trustedBasePackages) {
            this.trustedBasePackages = trustedBasePackages;
        }

        /**
         * 基于子类名称校验多态类型是否合法。
         *
         * @param context      Databind 上下文
         * @param baseType     基础类型
         * @param subClassName 子类名称
         * @return 多态类型合法性结果
         */
        @Override
        public Validity validateSubClassName(DatabindContext context, JavaType baseType, String subClassName) {
            return isTrustedClassName(subClassName, trustedBasePackages) ? Validity.ALLOWED : Validity.DENIED;
        }

        /**
         * 基于 JavaType 校验多态类型是否合法。
         *
         * @param context  Databind 上下文
         * @param baseType 基础类型
         * @param subType  子类型
         * @return 多态类型合法性结果
         */
        @Override
        public Validity validateSubType(DatabindContext context, JavaType baseType, JavaType subType) {
            Class<?> rawClass = subType.getRawClass();
            while (rawClass.isArray()) {
                rawClass = rawClass.getComponentType();
            }
            if (rawClass.isPrimitive()) {
                return Validity.ALLOWED;
            }
            return isTrustedClassName(rawClass.getName(), trustedBasePackages) ? Validity.ALLOWED : Validity.DENIED;
        }
    }

    /**
     * 字符串值序列化器。
     *
     * @param <T> 序列化类型
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class StringValueSerializer<T> extends ValueSerializer<T> {

        /**
         * 当前序列化器处理的类型。
         */
        private final Class<?> handledType;

        /**
         * 格式化函数。
         */
        private final Function<T, String> formatter;

        /**
         * 创建字符串值序列化器。
         *
         * @param handledType 当前序列化器处理的类型
         * @param formatter   格式化函数
         */
        private StringValueSerializer(Class<?> handledType, Function<T, String> formatter) {
            this.handledType = handledType;
            this.formatter = formatter;
        }

        /**
         * 获取当前序列化器处理的类型。
         *
         * @return 当前序列化器处理的类型
         */
        @Override
        public Class<?> handledType() {
            return handledType;
        }

        /**
         * 将值序列化为字符串。
         *
         * @param value     待序列化值
         * @param generator JSON 生成器
         * @param context   序列化上下文
         * @throws JacksonException JSON 写入异常
         */
        @Override
        public void serialize(T value, JsonGenerator generator, SerializationContext context) throws JacksonException {
            generator.writeString(formatter.apply(value));
        }
    }

    /**
     * 字符串值反序列化器。
     *
     * @param <T> 反序列化类型
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class StringValueDeserializer<T> extends ValueDeserializer<T> {

        /**
         * 当前反序列化器处理的类型。
         */
        private final Class<?> handledType;

        /**
         * 解析函数。
         */
        private final Function<String, T> parser;

        /**
         * 创建字符串值反序列化器。
         *
         * @param handledType 当前反序列化器处理的类型
         * @param parser      解析函数
         */
        private StringValueDeserializer(Class<?> handledType, Function<String, T> parser) {
            this.handledType = handledType;
            this.parser = parser;
        }

        /**
         * 获取当前反序列化器处理的类型。
         *
         * @return 当前反序列化器处理的类型
         */
        @Override
        public Class<?> handledType() {
            return handledType;
        }

        /**
         * 将 JSON 字符串反序列化为目标类型。
         *
         * @param parser  JSON 解析器
         * @param context 反序列化上下文
         * @return 目标类型实例
         * @throws JacksonException JSON 读取异常
         */
        @Override
        public T deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
            String text = parser.getText();
            if (isBlank(text)) {
                return null;
            }
            return this.parser.apply(text.trim());
        }
    }

    /**
     * 异常对象存储混入配置。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    @JsonAutoDetect(
            // 异常对象不直接暴露字段，避免输出 JDK 内部复杂状态。
            fieldVisibility = JsonAutoDetect.Visibility.NONE,
            // 异常对象仅允许 public getter 参与序列化。
            getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            // 异常对象禁用 setter 探测，避免反序列化修改异常状态。
            setterVisibility = JsonAutoDetect.Visibility.NONE,
            // 异常对象禁用 isGetter 探测，减少无关布尔属性输出。
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            // 异常对象禁用构造器探测，避免反序列化构造歧义。
            creatorVisibility = JsonAutoDetect.Visibility.NONE
    )
    // 使用对象 id 处理异常链路中的循环引用。
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    public abstract static class ThrowableStorageMixIn {
    }

    /**
     * 异常对象审计混入配置。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    @JsonAutoDetect(
            // 异常对象不直接暴露字段，避免输出 JDK 内部复杂状态。
            fieldVisibility = JsonAutoDetect.Visibility.NONE,
            // 异常对象仅允许 public getter 参与序列化。
            getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            // 异常对象禁用 setter 探测，避免反序列化修改异常状态。
            setterVisibility = JsonAutoDetect.Visibility.NONE,
            // 异常对象禁用 isGetter 探测，减少无关布尔属性输出。
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            // 异常对象禁用构造器探测，避免反序列化构造歧义。
            creatorVisibility = JsonAutoDetect.Visibility.NONE
    )
    // 使用对象 id 处理异常链路中的循环引用。
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    public abstract static class ThrowableAuditMixIn {
    }
}
