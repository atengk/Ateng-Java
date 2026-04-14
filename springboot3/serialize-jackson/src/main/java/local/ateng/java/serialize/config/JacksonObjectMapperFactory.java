package local.ateng.java.serialize.config;

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