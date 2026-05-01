package local.ateng.java.serialize.common.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Jackson ObjectMapper 统一构建工厂。
 *
 * <p>
 * 该工厂用于集中管理 Spring Boot 3 / Jackson 2 的 JSON 序列化与反序列化策略，避免不同模块各自创建
 * ObjectMapper 导致时间策略、类型信息、字段可见性、数值精度、枚举策略和容错边界不一致。
 * </p>
 *
 * <p>
 * 设计原则：Default 与 Storage 不做业务时间格式化，仅注册 Jackson 原生 JavaTimeModule；Web 输出业务格式时间；
 * Audit 以稳定、可读、可追溯为主，默认不写入 @class，避免审计日志与 Java 类名强耦合。
 * </p>
 *
 * <p>
 * 使用建议：ObjectMapper 完成配置后应作为单例 Bean 复用，不要在高频路径中反复创建。开启 @class 的 Mapper
 * 仅用于可信内部数据，例如 Redis、数据库 JSON 字段或内部消息，不建议直接处理外部请求体。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class JacksonObjectMapperFactory {

    /**
     * 默认类型信息字段名。
     */
    private static final String TYPE_PROPERTY_NAME = "@class";

    /**
     * 默认信任的业务包前缀。
     */
    private static final List<String> TRUSTED_BASE_PACKAGES = List.of(
            // 允许 io.github.atengk 包下的业务类型参与默认类型反序列化
            "io.github.atengk",
            // 允许 local.ateng.java 包下的业务类型参与默认类型反序列化
            "local.ateng.java"
    );

    /**
     * 允许参与默认类型反序列化的 JDK 包前缀。
     */
    private static final List<String> TRUSTED_JDK_PACKAGE_PREFIXES = List.of(
            // 允许 JDK 集合、日期容器和常用工具类型参与默认类型反序列化
            "java.util.",
            // 允许 Java 8 时间类型参与默认类型反序列化
            "java.time.",
            // 允许 BigDecimal、BigInteger 等高精度数值类型参与默认类型反序列化
            "java.math.",
            // 允许 Jackson Tree Model 节点类型参与默认类型反序列化
            "com.fasterxml.jackson.databind.node."
    );

    /**
     * 允许参与默认类型反序列化的 JDK 精确类名。
     */
    private static final Set<String> TRUSTED_JDK_CLASS_NAMES = Set.of(
            // 允许 Object 作为通用根类型
            "java.lang.Object",
            // 允许字符串类型
            "java.lang.String",
            // 允许 Boolean 包装类型
            "java.lang.Boolean",
            // 允许 Character 包装类型
            "java.lang.Character",
            // 允许 Byte 包装类型
            "java.lang.Byte",
            // 允许 Short 包装类型
            "java.lang.Short",
            // 允许 Integer 包装类型
            "java.lang.Integer",
            // 允许 Long 包装类型
            "java.lang.Long",
            // 允许 Float 包装类型
            "java.lang.Float",
            // 允许 Double 包装类型
            "java.lang.Double",
            // 允许 Void 类型
            "java.lang.Void",
            // 允许异常堆栈元素类型
            "java.lang.StackTraceElement",
            // 允许 Throwable 基类
            "java.lang.Throwable",
            // 允许 Exception 基类
            "java.lang.Exception",
            // 允许 RuntimeException 基类
            "java.lang.RuntimeException"
    );

    /**
     * JVM 基础类型数组名称。
     */
    private static final Set<String> JVM_PRIMITIVE_ARRAY_NAMES = Set.of(
            // 允许 boolean[] JVM 内部类型名
            "[Z",
            // 允许 byte[] JVM 内部类型名
            "[B",
            // 允许 short[] JVM 内部类型名
            "[S",
            // 允许 int[] JVM 内部类型名
            "[I",
            // 允许 long[] JVM 内部类型名
            "[J",
            // 允许 float[] JVM 内部类型名
            "[F",
            // 允许 double[] JVM 内部类型名
            "[D",
            // 允许 char[] JVM 内部类型名
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
     * 日期格式。
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式。
     */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 日期时间格式化器。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /**
     * ISO 日期时间格式化器。
     */
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 日期格式化器。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * 时间格式化器。
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    /**
     * 禁止实例化工具工厂。
     */
    private JacksonObjectMapperFactory() {
    }

    /**
     * 构建默认 ObjectMapper。
     *
     * <p>
     * 默认场景不做业务时间格式化，不设置 SimpleDateFormat，不挂载自定义 LocalDateTimeSerializer。
     * 该 Mapper 适合通用 JSON 处理、对象转换、内部工具方法和不希望时间被固定格式改写的场景。
     * </p>
     *
     * @return 默认 ObjectMapper
     */
    public static ObjectMapper buildDefaultObjectMapper() {
        // 创建使用原生时间模块的基础 JsonMapper
        JsonMapper objectMapper = baseBuilder(buildNativeJavaTimeModule())
                // 构建默认 ObjectMapper 实例
                .build();

        // 默认场景使用公开成员可见性策略，避免直接暴露私有字段
        applyWebVisibility(objectMapper);

        // 返回配置完成的默认 ObjectMapper
        return objectMapper;
    }

    /**
     * 构建用于 Redis / 数据库存储的 ObjectMapper。
     *
     * <p>
     * 存储场景不做业务时间格式化，保留 Jackson 原生 JavaTimeModule 行为，并开启 @class 类型信息，
     * 用于恢复 Object、接口、抽象类、集合元素等真实类型。该 Mapper 仅建议用于可信内部数据。
     * </p>
     *
     * @return 存储场景专用 ObjectMapper
     */
    public static ObjectMapper buildStorageObjectMapper() {
        // 创建使用原生时间模块的基础 JsonMapper
        JsonMapper objectMapper = baseBuilder(buildNativeJavaTimeModule())
                // 启用 BigDecimal 普通数字输出，避免科学计数法影响存储稳定性
                .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
                // 启用属性按字母排序，保证序列化结果稳定，便于缓存比对和签名计算
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                // 启用 Map 按 Key 排序，保证 Map 输出顺序稳定
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                // 构建存储场景 ObjectMapper 实例
                .build();

        // 存储场景使用字段优先可见性，尽量完整保存对象内部状态
        applyFieldVisibility(objectMapper);

        // 启用受限默认类型信息，并使用 @class 字段保存类型
        applyDefaultTyping(objectMapper);

        // 为 Throwable 增加混入配置，提升异常对象存储兼容性
        applyThrowableMixIn(objectMapper);

        // 返回配置完成的存储场景 ObjectMapper
        return objectMapper;
    }

    /**
     * 构建用于 Spring Web 的 ObjectMapper。
     *
     * <p>
     * Web 场景需要格式化时间输出，LocalDateTime 输出为 yyyy-MM-dd HH:mm:ss，LocalDate 输出为 yyyy-MM-dd，
     * LocalTime 输出为 HH:mm:ss，java.util.Date 也按统一业务格式输出。该 Mapper 同时处理 Long / BigInteger
     * 前端精度问题和常见入参兼容问题。
     * </p>
     *
     * @return Web 场景专用 ObjectMapper
     */
    public static ObjectMapper buildWebObjectMapper() {
        // 创建使用业务格式化时间模块的基础 JsonMapper
        JsonMapper objectMapper = baseBuilder(buildFormattedJavaTimeModule())
                // 允许 JSON 中存在 Java/C 风格注释，便于兼容部分前端或第三方输入
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                // 允许单引号字符串，兼容部分非严格 JSON 输入
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                // 允许数组或对象末尾存在多余逗号
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                // 允许标量类型自动转换，例如字符串数字转换为数值类型
                .enable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                // 允许单个值按数组处理，提高接口入参兼容性
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                // 未知枚举值反序列化为 null，避免接口因枚举扩展直接失败
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                // 浮点数反序列化优先使用 BigDecimal，降低精度损失风险
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                // 整数反序列化优先使用 BigInteger，降低整数溢出风险
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                // 枚举序列化使用 toString，便于枚举对外展示值与内部 name 解耦
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                // 枚举反序列化使用 toString，与序列化策略保持一致
                .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                // 构建 Web 场景 ObjectMapper 实例
                .build();

        // 配置 java.util.Date 的业务格式化输出
        configureFormattedLegacyDate(objectMapper);

        // Web 场景使用公开成员可见性策略，契合 Controller DTO 常规设计
        applyWebVisibility(objectMapper);

        // Web 场景将 Long 和 BigInteger 序列化为字符串，避免 JavaScript 精度丢失
        applyNumberSerialization(objectMapper);

        // 返回配置完成的 Web 场景 ObjectMapper
        return objectMapper;
    }

    /**
     * 构建用于审计日志的 ObjectMapper。
     *
     * <p>
     * 审计日志场景优先保证可读性、字段完整性、输出稳定性和跨系统检索友好性。因此该 Mapper 使用格式化时间、
     * 字段优先可见性、稳定排序和大整数字符串输出。默认不写入 @class，避免审计日志暴露 Java 类名或强耦合类结构。
     * </p>
     *
     * @return 审计日志专用 ObjectMapper
     */
    public static ObjectMapper buildAuditObjectMapper() {
        // 创建使用业务格式化时间模块的基础 JsonMapper
        JsonMapper objectMapper = baseBuilder(buildFormattedJavaTimeModule())
                // 启用 BigDecimal 普通数字输出，避免科学计数法影响审计可读性
                .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
                // 启用属性按字母排序，保证审计日志结构稳定
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                // 启用 Map 按 Key 排序，保证审计日志集合输出稳定
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                // 枚举序列化使用 toString，提升审计日志可读性
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                // 枚举反序列化使用 toString，保持审计回放策略一致
                .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                // 浮点数反序列化优先使用 BigDecimal，降低审计回放精度损失
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                // 整数反序列化优先使用 BigInteger，降低审计回放整数溢出风险
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                // 构建审计日志场景 ObjectMapper 实例
                .build();

        // 配置 java.util.Date 的业务格式化输出
        configureFormattedLegacyDate(objectMapper);

        // 审计日志使用字段优先可见性，尽量记录完整对象状态
        applyFieldVisibility(objectMapper);

        // 审计日志将 Long 和 BigInteger 序列化为字符串，避免跨系统查看时精度丢失
        applyNumberSerialization(objectMapper);

        // 为 Throwable 增加混入配置，提升异常审计日志可读性和完整性
        applyThrowableMixIn(objectMapper);

        // 返回配置完成的审计日志 ObjectMapper
        return objectMapper;
    }

    /**
     * 构建基础 JsonMapper Builder。
     *
     * @param javaTimeModule Java 8 时间模块
     * @return JsonMapper Builder
     */
    private static JsonMapper.Builder baseBuilder(JavaTimeModule javaTimeModule) {
        // 使用 JsonMapper Builder 创建 Jackson 2 JSON 专用 Mapper 构建器
        return JsonMapper.builder()
                // 注册 JDK8 模块，支持 Optional 等 JDK8 类型
                .addModule(new Jdk8Module())
                // 注册构造参数名模块，支持基于 -parameters 的构造器反序列化
                .addModule(new ParameterNamesModule(JsonCreator.Mode.DEFAULT))
                // 注册调用方传入的 Java 8 时间模块
                .addModule(javaTimeModule)
                // 设置默认时区，保证传统 Date 与上下文时区一致
                .defaultTimeZone(defaultTimeZone())
                // 保留 null 字段，避免接口或存储结构因空值被隐藏
                .serializationInclusion(JsonInclude.Include.ALWAYS)
                // 禁用日期时间戳输出，原生时间模块会输出 ISO/default 字符串，Web/Audit 会输出业务格式字符串
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 禁用 Duration 时间戳输出，保持时间文本可读
                .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                // 空 Bean 不抛异常，避免无属性对象导致序列化失败
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // 忽略未知字段，提升接口和存储结构演进兼容性
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 禁止反序列化时自动调整时区，避免时间偏移
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    /**
     * 构建原生 Java 8 时间模块。
     *
     * <p>
     * 该模块不注册自定义 LocalDateTimeSerializer，不设置业务时间格式，用于 Default 与 Storage 场景，避免时间被
     * yyyy-MM-dd HH:mm:ss 强制改写。
     * </p>
     *
     * @return 原生 JavaTimeModule
     */
    private static JavaTimeModule buildNativeJavaTimeModule() {
        // 返回 Jackson 原生 JavaTimeModule
        return new JavaTimeModule();
    }

    /**
     * 构建业务格式化 Java 8 时间模块。
     *
     * @return 业务格式化 JavaTimeModule
     */
    private static JavaTimeModule buildFormattedJavaTimeModule() {
        // 创建 JavaTimeModule，用于统一 Java 8 时间类型序列化策略
        JavaTimeModule module = new JavaTimeModule();

        // 注册 LocalDateTime 序列化器，输出 yyyy-MM-dd HH:mm:ss
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));

        // 注册 LocalDateTime 反序列化器，兼容 yyyy-MM-dd HH:mm:ss 和 ISO_LOCAL_DATE_TIME
        module.addDeserializer(LocalDateTime.class, new MultiPatternLocalDateTimeDeserializer());

        // 注册 LocalDate 序列化器，输出 yyyy-MM-dd
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));

        // 注册 LocalDate 反序列化器，输入 yyyy-MM-dd
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));

        // 注册 LocalTime 序列化器，输出 HH:mm:ss
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FORMATTER));

        // 注册 LocalTime 反序列化器，输入 HH:mm:ss
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));

        // 注册 Instant 序列化器，输出 ISO_INSTANT，保留 UTC 时间语义
        module.addSerializer(Instant.class, new InstantStringSerializer());

        // 注册 Instant 反序列化器，输入 ISO_INSTANT
        module.addDeserializer(Instant.class, new InstantStringDeserializer());

        // 注册 OffsetDateTime 序列化器，输出 ISO_OFFSET_DATE_TIME，保留 offset 信息
        module.addSerializer(OffsetDateTime.class, new OffsetDateTimeStringSerializer());

        // 注册 OffsetDateTime 反序列化器，输入 ISO_OFFSET_DATE_TIME
        module.addDeserializer(OffsetDateTime.class, new OffsetDateTimeStringDeserializer());

        // 注册 ZonedDateTime 序列化器，输出 ISO_ZONED_DATE_TIME，保留 ZoneId 信息
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeStringSerializer());

        // 注册 ZonedDateTime 反序列化器，输入 ISO_ZONED_DATE_TIME
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeStringDeserializer());

        // 返回配置完成的业务格式化 JavaTimeModule
        return module;
    }

    /**
     * 配置传统 Date 类型的业务格式。
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void configureFormattedLegacyDate(ObjectMapper objectMapper) {
        // 创建传统 Date 格式化器，统一输出 yyyy-MM-dd HH:mm:ss
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);

        // 设置传统 Date 格式化器时区，避免服务器默认时区差异
        dateFormat.setTimeZone(defaultTimeZone());

        // 将 Date 格式化器应用到 ObjectMapper
        objectMapper.setDateFormat(dateFormat);
    }

    /**
     * 设置 Web 场景对象可见性。
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyWebVisibility(ObjectMapper objectMapper) {
        // 统一设置公开成员可见性
        applyVisibility(
                // 当前需要设置可见性的 ObjectMapper 实例
                objectMapper,
                // 字段可见性：仅公开字段参与序列化和反序列化
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                // getter 可见性：仅公开 getter 参与序列化
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                // setter 可见性：仅公开 setter 参与反序列化
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                // isGetter 可见性：仅公开 isXxx getter 参与序列化
                JsonAutoDetect.Visibility.PUBLIC_ONLY,
                // 构造器可见性：保持 Jackson 默认策略
                JsonAutoDetect.Visibility.DEFAULT
        );
    }

    /**
     * 设置字段优先对象可见性。
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyFieldVisibility(ObjectMapper objectMapper) {
        // 统一设置字段优先可见性
        applyVisibility(
                // 当前需要设置可见性的 ObjectMapper 实例
                objectMapper,
                // 字段可见性：所有字段都参与序列化和反序列化
                JsonAutoDetect.Visibility.ANY,
                // getter 可见性：禁用 getter，避免重复输出或触发计算逻辑
                JsonAutoDetect.Visibility.NONE,
                // setter 可见性：禁用 setter，优先直接恢复字段状态
                JsonAutoDetect.Visibility.NONE,
                // isGetter 可见性：禁用 isXxx getter，避免布尔计算属性干扰存储结构
                JsonAutoDetect.Visibility.NONE,
                // 构造器可见性：禁用自动构造器探测，降低存储场景反射歧义
                JsonAutoDetect.Visibility.NONE
        );
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
        // 将统一可见性检查器设置到 ObjectMapper
        objectMapper.setVisibility(
                // 获取当前序列化配置
                objectMapper.getSerializationConfig()
                        // 获取默认可见性检查器
                        .getDefaultVisibilityChecker()
                        // 设置字段可见性策略
                        .withFieldVisibility(fieldVisibility)
                        // 设置 getter 可见性策略
                        .withGetterVisibility(getterVisibility)
                        // 设置 setter 可见性策略
                        .withSetterVisibility(setterVisibility)
                        // 设置 isGetter 可见性策略
                        .withIsGetterVisibility(isGetterVisibility)
                        // 设置构造器可见性策略
                        .withCreatorVisibility(creatorVisibility)
        );
    }

    /**
     * 统一数值序列化策略。
     *
     * <p>
     * JavaScript 对超过安全整数范围的 long / BigInteger 存在精度风险，因此 Web 与审计输出统一写为字符串。
     * BigDecimal 保持数值输出，并通过 WRITE_BIGDECIMAL_AS_PLAIN 避免科学计数法。
     * </p>
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyNumberSerialization(ObjectMapper objectMapper) {
        // 创建数值兼容模块，用于注册前端精度安全序列化器
        SimpleModule module = new SimpleModule("NumberCompatibilityModule");

        // 使用 Jackson 内置字符串序列化器输出大整数
        ToStringSerializer stringSerializer = ToStringSerializer.instance;

        // Long 包装类型序列化为字符串，避免 JavaScript 精度丢失
        module.addSerializer(Long.class, stringSerializer);

        // long 基本类型序列化为字符串，避免 JavaScript 精度丢失
        module.addSerializer(Long.TYPE, stringSerializer);

        // BigInteger 序列化为字符串，避免超大整数跨端精度丢失
        module.addSerializer(BigInteger.class, stringSerializer);

        // 将数值兼容模块注册到 ObjectMapper
        objectMapper.registerModule(module);
    }

    /**
     * 启用默认多态能力，并限制反序列化来源类型范围。
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyDefaultTyping(ObjectMapper objectMapper) {
        // 非法或不可信子类型直接失败，避免静默降级带来安全风险
        objectMapper.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

        // 启用受限默认类型信息，并使用 @class 作为类型字段名
        objectMapper.activateDefaultTypingAsProperty(
                // 使用自定义白名单多态类型校验器
                buildPolymorphicTypeValidator(),
                // 仅对非 final 类型写入类型信息，兼顾类型恢复和 JSON 体积
                ObjectMapper.DefaultTyping.NON_FINAL,
                // 使用 @class 作为默认类型信息字段名
                TYPE_PROPERTY_NAME
        );
    }

    /**
     * 为异常类型挂载混入配置。
     *
     * @param objectMapper ObjectMapper 实例
     */
    private static void applyThrowableMixIn(ObjectMapper objectMapper) {
        // 为 Throwable 绑定混入，处理异常对象字段、类型信息和循环引用
        objectMapper.addMixIn(Throwable.class, ThrowableMixIn.class);
    }

    /**
     * 构建多态类型校验器。
     *
     * @return 多态类型校验器
     */
    private static PolymorphicTypeValidator buildPolymorphicTypeValidator() {
        // 返回受限白名单多态类型校验器
        return new TrustedPolymorphicTypeValidator();
    }

    /**
     * 获取默认时区实例。
     *
     * @return 默认时区
     */
    private static TimeZone defaultTimeZone() {
        // 根据默认 ZoneId 获取 TimeZone 实例
        return TimeZone.getTimeZone(DEFAULT_ZONE_ID);
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 待判断字符串
     * @return true 表示字符串为 null、空串或仅包含空白字符
     */
    private static boolean isBlank(String value) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断类名是否属于可信范围。
     *
     * @param className 类名
     * @return true 表示可信
     */
    private static boolean isTrustedClassName(String className) {
        if (isBlank(className)) {
            return false;
        }
        if (JVM_PRIMITIVE_ARRAY_NAMES.contains(className)) {
            return true;
        }

        String normalizedClassName = normalizeJvmArrayClassName(className.trim());
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
        for (String basePackage : TRUSTED_BASE_PACKAGES) {
            String normalizedPackage = basePackage.endsWith(".") ? basePackage : basePackage + ".";
            if (normalizedClassName.startsWith(normalizedPackage)) {
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
        private static final long serialVersionUID = -8757087763320902321L;

        /**
         * 基于子类名称校验多态类型是否合法。
         *
         * @param config       Mapper 配置
         * @param baseType     基础类型
         * @param subClassName 子类名称
         * @return 多态类型合法性结果
         */
        @Override
        public Validity validateSubClassName(MapperConfig<?> config, JavaType baseType, String subClassName) {
            return isTrustedClassName(subClassName) ? Validity.ALLOWED : Validity.DENIED;
        }

        /**
         * 基于 JavaType 校验多态类型是否合法。
         *
         * @param config  Mapper 配置
         * @param baseType 基础类型
         * @param subType  子类型
         * @return 多态类型合法性结果
         */
        @Override
        public Validity validateSubType(MapperConfig<?> config, JavaType baseType, JavaType subType) {
            Class<?> rawClass = subType.getRawClass();
            while (rawClass.isArray()) {
                rawClass = rawClass.getComponentType();
            }
            if (rawClass.isPrimitive()) {
                return Validity.ALLOWED;
            }
            return isTrustedClassName(rawClass.getName()) ? Validity.ALLOWED : Validity.DENIED;
        }
    }

    /**
     * LocalDateTime 多格式反序列化器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class MultiPatternLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        /**
         * 将 JSON 字符串反序列化为 LocalDateTime。
         *
         * @param parser  JSON 解析器
         * @param context 反序列化上下文
         * @return LocalDateTime 实例
         * @throws IOException JSON 读取异常
         */
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = parser.getValueAsString();
            if (isBlank(text)) {
                return null;
            }

            String value = text.trim();
            try {
                return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME_FORMATTER);
            }
        }
    }

    /**
     * Instant 字符串序列化器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class InstantStringSerializer extends JsonSerializer<Instant> {

        /**
         * 将 Instant 序列化为 ISO_INSTANT 字符串。
         *
         * @param value       Instant 值
         * @param generator   JSON 生成器
         * @param serializers 序列化上下文
         * @throws IOException JSON 写入异常
         */
        @Override
        public void serialize(Instant value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
            if (value == null) {
                generator.writeNull();
                return;
            }
            generator.writeString(DateTimeFormatter.ISO_INSTANT.format(value));
        }
    }

    /**
     * Instant 字符串反序列化器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class InstantStringDeserializer extends JsonDeserializer<Instant> {

        /**
         * 将 JSON 字符串反序列化为 Instant。
         *
         * @param parser  JSON 解析器
         * @param context 反序列化上下文
         * @return Instant 实例
         * @throws IOException JSON 读取异常
         */
        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = parser.getValueAsString();
            return isBlank(text) ? null : Instant.parse(text.trim());
        }
    }

    /**
     * OffsetDateTime 字符串序列化器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class OffsetDateTimeStringSerializer extends JsonSerializer<OffsetDateTime> {

        /**
         * 将 OffsetDateTime 序列化为 ISO_OFFSET_DATE_TIME 字符串。
         *
         * @param value       OffsetDateTime 值
         * @param generator   JSON 生成器
         * @param serializers 序列化上下文
         * @throws IOException JSON 写入异常
         */
        @Override
        public void serialize(OffsetDateTime value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
            if (value == null) {
                generator.writeNull();
                return;
            }
            generator.writeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
        }
    }

    /**
     * OffsetDateTime 字符串反序列化器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class OffsetDateTimeStringDeserializer extends JsonDeserializer<OffsetDateTime> {

        /**
         * 将 JSON 字符串反序列化为 OffsetDateTime。
         *
         * @param parser  JSON 解析器
         * @param context 反序列化上下文
         * @return OffsetDateTime 实例
         * @throws IOException JSON 读取异常
         */
        @Override
        public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = parser.getValueAsString();
            return isBlank(text) ? null : OffsetDateTime.parse(text.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    /**
     * ZonedDateTime 字符串序列化器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class ZonedDateTimeStringSerializer extends JsonSerializer<ZonedDateTime> {

        /**
         * 将 ZonedDateTime 序列化为 ISO_ZONED_DATE_TIME 字符串。
         *
         * @param value       ZonedDateTime 值
         * @param generator   JSON 生成器
         * @param serializers 序列化上下文
         * @throws IOException JSON 写入异常
         */
        @Override
        public void serialize(ZonedDateTime value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
            if (value == null) {
                generator.writeNull();
                return;
            }
            generator.writeString(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value));
        }
    }

    /**
     * ZonedDateTime 字符串反序列化器。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static final class ZonedDateTimeStringDeserializer extends JsonDeserializer<ZonedDateTime> {

        /**
         * 将 JSON 字符串反序列化为 ZonedDateTime。
         *
         * @param parser  JSON 解析器
         * @param context 反序列化上下文
         * @return ZonedDateTime 实例
         * @throws IOException JSON 读取异常
         */
        @Override
        public ZonedDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = parser.getValueAsString();
            return isBlank(text) ? null : ZonedDateTime.parse(text.trim(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }

    /**
     * 异常对象序列化混入配置。
     *
     * <p>
     * 该混入仅用于补充 Throwable 的序列化视图，不修改业务异常本身代码结构。
     * </p>
     *
     * @author Ateng
     * @since 2026-04-30
     */
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
            setterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            creatorVisibility = JsonAutoDetect.Visibility.NONE
    )
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = TYPE_PROPERTY_NAME)
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    public static class ThrowableMixIn {
    }
}
