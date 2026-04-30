package io.github.atengk.utils.id;

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * ID 工具类，提供 UUID、UUID v7、Snowflake、ULID、业务单号、TraceId、短 ID、验证码、文件名、安全随机 ID 等常用能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class IdUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final long SNOWFLAKE_EPOCH = 1_704_067_200_000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
    private static final Snowflake DEFAULT_SNOWFLAKE = new Snowflake(workerId(), dataCenterId());
    private static final ConcurrentMap<String, Snowflake> SNOWFLAKE_MAP = new ConcurrentHashMap<>();

    private static final String NUMBER_ALPHABET = "0123456789";
    private static final String UPPER_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final String LETTER_ALPHABET = UPPER_ALPHABET + LOWER_ALPHABET;
    private static final String MIX_ALPHABET = NUMBER_ALPHABET + UPPER_ALPHABET + LOWER_ALPHABET;
    private static final String URL_SAFE_ALPHABET = MIX_ALPHABET + "-_";
    private static final String READABLE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final char[] BASE62_ALPHABET = MIX_ALPHABET.toCharArray();
    private static final String ULID_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final char[] ULID_CHARS = ULID_ALPHABET.toCharArray();
    private static final int[] ULID_DECODE = new int[128];
    private static final Object ULID_LOCK = new Object();
    private static long lastUlidTime = -1L;
    private static byte[] lastUlidRandom = new byte[10];

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern UUID_SIMPLE_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern TRACE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");
    private static final Pattern BIZ_NO_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*[0-9A-Za-z]{6,}$");
    private static final Pattern SIMPLE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern FILE_INVALID_PATTERN = Pattern.compile("[\\\\/:*?\"<>|\\s]+");
    private static final Pattern DIR_INVALID_PATTERN = Pattern.compile("[\\\\:*?\"<>|\\s]+");

    static {
        for (int i = 0; i < ULID_DECODE.length; i++) {
            ULID_DECODE[i] = -1;
        }
        for (int i = 0; i < ULID_ALPHABET.length(); i++) {
            char c = ULID_ALPHABET.charAt(i);
            ULID_DECODE[c] = i;
            ULID_DECODE[Character.toLowerCase(c)] = i;
        }
    }

    private IdUtil() {
        throw new UnsupportedOperationException("IdUtil 不允许实例化");
    }

    /**
     * ID 类型枚举，用于识别字符串 ID 的大致类型。
     */
    public enum IdType {
        /** UUID v7。 */
        UUID_V7,
        /** 普通 UUID。 */
        UUID,
        /** ULID。 */
        ULID,
        /** Snowflake ID。 */
        SNOWFLAKE,
        /** 业务单号。 */
        BIZ_NO,
        /** TraceId 或 RequestId。 */
        TRACE_ID,
        /** 短 ID。 */
        SHORT_ID,
        /** 普通数字 ID。 */
        NUMERIC,
        /** 未知类型。 */
        UNKNOWN
    }

    /**
     * 生成标准 UUID 字符串，默认使用 UUID v4，包含横线。
     *
     * @return 标准 UUID 字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成无横线 UUID 字符串。
     *
     * @return 32 位无横线 UUID 字符串
     */
    public static String uuidSimple() {
        return removeUuidHyphen(uuid());
    }

    /**
     * 生成大写标准 UUID 字符串。
     *
     * @return 大写标准 UUID 字符串
     */
    public static String uuidUpper() {
        return uuid().toUpperCase(Locale.ROOT);
    }

    /**
     * 生成大写无横线 UUID 字符串。
     *
     * @return 大写无横线 UUID 字符串
     */
    public static String uuidSimpleUpper() {
        return uuidSimple().toUpperCase(Locale.ROOT);
    }

    /**
     * 生成 UUID v4 字符串，包含横线。
     *
     * @return UUID v4 字符串
     */
    public static String uuidV4() {
        return uuid();
    }

    /**
     * 生成无横线 UUID v4 字符串。
     *
     * @return 32 位无横线 UUID v4 字符串
     */
    public static String uuidV4Simple() {
        return uuidSimple();
    }

    /**
     * 判断字符串是否为标准 UUID 格式。
     *
     * @param value 待判断字符串
     * @return 是否为标准 UUID
     */
    public static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }
        return UUID_PATTERN.matcher(value).matches() && canParseUuid(value);
    }

    /**
     * 判断字符串是否为 32 位无横线 UUID 格式。
     *
     * @param value 待判断字符串
     * @return 是否为无横线 UUID
     */
    public static boolean isUuidSimple(String value) {
        return value != null && UUID_SIMPLE_PATTERN.matcher(value).matches() && canParseUuid(toStandardUuid(value));
    }

    /**
     * 将标准 UUID 或无横线 UUID 规范化为标准 UUID 格式。
     *
     * @param value UUID 字符串
     * @return 标准 UUID 字符串
     */
    public static String normalizeUuid(String value) {
        String text = requireText(value, "UUID 不能为空");
        String standard = isUuidSimple(text) ? toStandardUuid(text) : text;
        if (!isUuid(standard)) {
            throw new IllegalArgumentException("UUID 格式不正确: " + value);
        }
        return UUID.fromString(standard).toString();
    }

    /**
     * 移除 UUID 中的横线。
     *
     * @param value UUID 字符串
     * @return 无横线 UUID 字符串
     */
    public static String removeUuidHyphen(String value) {
        return normalizeUuid(value).replace("-", "");
    }

    /**
     * 生成标准 UUID v7 字符串，包含横线。
     *
     * @return UUID v7 字符串
     */
    public static String uuidV7() {
        long timestamp = System.currentTimeMillis();
        long randA = SECURE_RANDOM.nextLong() & 0xFFFL;
        long randB = SECURE_RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL;
        long mostSigBits = ((timestamp & 0xFFFF_FFFF_FFFFL) << 16) | (0x7L << 12) | randA;
        long leastSigBits = 0x8000_0000_0000_0000L | randB;
        return new UUID(mostSigBits, leastSigBits).toString();
    }

    /**
     * 生成无横线 UUID v7 字符串。
     *
     * @return 无横线 UUID v7 字符串
     */
    public static String uuidV7Simple() {
        return removeUuidHyphen(uuidV7());
    }

    /**
     * 生成大写标准 UUID v7 字符串。
     *
     * @return 大写 UUID v7 字符串
     */
    public static String uuidV7Upper() {
        return uuidV7().toUpperCase(Locale.ROOT);
    }

    /**
     * 生成大写无横线 UUID v7 字符串。
     *
     * @return 大写无横线 UUID v7 字符串
     */
    public static String uuidV7SimpleUpper() {
        return uuidV7Simple().toUpperCase(Locale.ROOT);
    }

    /**
     * 判断字符串是否为 UUID v7。
     *
     * @param value 待判断字符串
     * @return 是否为 UUID v7
     */
    public static boolean isUuidV7(String value) {
        try {
            String standard = normalizeUuid(value);
            return UUID.fromString(standard).version() == 7;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 获取 UUID v7 中的毫秒时间戳。
     *
     * @param value UUID v7 字符串
     * @return 毫秒时间戳
     */
    public static long getUuidV7Timestamp(String value) {
        if (!isUuidV7(value)) {
            throw new IllegalArgumentException("不是合法的 UUID v7: " + value);
        }
        UUID uuid = UUID.fromString(normalizeUuid(value));
        return (uuid.getMostSignificantBits() >>> 16) & 0xFFFF_FFFF_FFFFL;
    }

    /**
     * 获取 UUID v7 对应的本地时间。
     *
     * @param value UUID v7 字符串
     * @return 本地时间
     */
    public static LocalDateTime getUuidV7Time(String value) {
        return toLocalDateTime(getUuidV7Timestamp(value));
    }

    /**
     * 按 UUID v7 的时间顺序比较两个 ID。
     *
     * @param left 左侧 UUID v7
     * @param right 右侧 UUID v7
     * @return 比较结果
     */
    public static int compareUuidV7(String left, String right) {
        long leftTime = getUuidV7Timestamp(left);
        long rightTime = getUuidV7Timestamp(right);
        int result = Long.compare(leftTime, rightTime);
        if (result != 0) {
            return result;
        }
        return normalizeUuid(left).compareToIgnoreCase(normalizeUuid(right));
    }

    /**
     * 生成默认 Snowflake ID。
     *
     * @return Snowflake ID
     */
    public static long snowflakeId() {
        return DEFAULT_SNOWFLAKE.nextId();
    }

    /**
     * 生成默认 Snowflake ID 字符串。
     *
     * @return Snowflake ID 字符串
     */
    public static String snowflakeIdStr() {
        return Long.toString(snowflakeId());
    }

    /**
     * 使用指定机器 ID 和数据中心 ID 生成 Snowflake ID。
     *
     * @param workerId 机器 ID，范围 0 到 31
     * @param dataCenterId 数据中心 ID，范围 0 到 31
     * @return Snowflake ID
     */
    public static long snowflakeId(long workerId, long dataCenterId) {
        checkWorkerId(workerId);
        checkDataCenterId(dataCenterId);
        String key = workerId + ":" + dataCenterId;
        return SNOWFLAKE_MAP.computeIfAbsent(key, ignored -> new Snowflake(workerId, dataCenterId)).nextId();
    }

    /**
     * 使用指定机器 ID 和数据中心 ID 生成 Snowflake ID 字符串。
     *
     * @param workerId 机器 ID，范围 0 到 31
     * @param dataCenterId 数据中心 ID，范围 0 到 31
     * @return Snowflake ID 字符串
     */
    public static String snowflakeIdStr(long workerId, long dataCenterId) {
        return Long.toString(snowflakeId(workerId, dataCenterId));
    }

    /**
     * 生成下一个默认 Snowflake ID。
     *
     * @return Snowflake ID
     */
    public static long nextSnowflakeId() {
        return snowflakeId();
    }

    /**
     * 生成下一个默认 Snowflake ID 字符串。
     *
     * @return Snowflake ID 字符串
     */
    public static String nextSnowflakeIdStr() {
        return snowflakeIdStr();
    }

    /**
     * 从 Snowflake ID 中解析毫秒时间戳。
     *
     * @param id Snowflake ID
     * @return 毫秒时间戳
     */
    public static long getSnowflakeTimestamp(long id) {
        requirePositive(id, "Snowflake ID 必须大于 0");
        return (id >> TIMESTAMP_LEFT_SHIFT) + SNOWFLAKE_EPOCH;
    }

    /**
     * 从 Snowflake ID 中解析本地时间。
     *
     * @param id Snowflake ID
     * @return 本地时间
     */
    public static LocalDateTime getSnowflakeTime(long id) {
        return toLocalDateTime(getSnowflakeTimestamp(id));
    }

    /**
     * 从 Snowflake ID 中解析机器 ID。
     *
     * @param id Snowflake ID
     * @return 机器 ID
     */
    public static long getSnowflakeWorkerId(long id) {
        requirePositive(id, "Snowflake ID 必须大于 0");
        return (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
    }

    /**
     * 从 Snowflake ID 中解析数据中心 ID。
     *
     * @param id Snowflake ID
     * @return 数据中心 ID
     */
    public static long getSnowflakeDataCenterId(long id) {
        requirePositive(id, "Snowflake ID 必须大于 0");
        return (id >> DATA_CENTER_ID_SHIFT) & MAX_DATA_CENTER_ID;
    }

    /**
     * 判断字符串是否可能为当前工具生成的 Snowflake ID。
     *
     * @param value 待判断字符串
     * @return 是否为 Snowflake ID
     */
    public static boolean isSnowflakeId(String value) {
        if (!isLongId(value)) {
            return false;
        }
        long id = Long.parseLong(value);
        if (id < (1L << TIMESTAMP_LEFT_SHIFT)) {
            return false;
        }
        long timestamp = getSnowflakeTimestamp(id);
        long nowUpperBound = System.currentTimeMillis() + 86_400_000L;
        return timestamp >= SNOWFLAKE_EPOCH && timestamp <= nowUpperBound;
    }

    /**
     * 生成标准 ULID。
     *
     * @return ULID 字符串
     */
    public static String ulid() {
        long timestamp = System.currentTimeMillis();
        byte[] random = new byte[10];
        SECURE_RANDOM.nextBytes(random);
        return encodeUlid(timestamp, random);
    }

    /**
     * 生成小写 ULID。
     *
     * @return 小写 ULID 字符串
     */
    public static String ulidLower() {
        return ulid().toLowerCase(Locale.ROOT);
    }

    /**
     * 生成单调 ULID，同一毫秒内递增随机段。
     *
     * @return 单调 ULID 字符串
     */
    public static String monotonicUlid() {
        synchronized (ULID_LOCK) {
            long timestamp = System.currentTimeMillis();
            if (timestamp == lastUlidTime) {
                lastUlidRandom = incrementRandom80(lastUlidRandom);
            } else {
                lastUlidTime = timestamp;
                lastUlidRandom = new byte[10];
                SECURE_RANDOM.nextBytes(lastUlidRandom);
            }
            return encodeUlid(timestamp, lastUlidRandom);
        }
    }

    /**
     * 判断字符串是否为 ULID。
     *
     * @param value 待判断字符串
     * @return 是否为 ULID
     */
    public static boolean isUlid(String value) {
        if (value == null || value.length() != 26) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 128 || ULID_DECODE[c] < 0) {
                return false;
            }
        }
        return ULID_DECODE[value.charAt(0)] <= 7;
    }

    /**
     * 获取 ULID 中的毫秒时间戳。
     *
     * @param value ULID 字符串
     * @return 毫秒时间戳
     */
    public static long getUlidTimestamp(String value) {
        if (!isUlid(value)) {
            throw new IllegalArgumentException("ULID 格式不正确: " + value);
        }
        long time = 0L;
        for (int i = 0; i < 10; i++) {
            time = (time << 5) | ULID_DECODE[value.charAt(i)];
        }
        return time;
    }

    /**
     * 获取 ULID 对应的本地时间。
     *
     * @param value ULID 字符串
     * @return 本地时间
     */
    public static LocalDateTime getUlidTime(String value) {
        return toLocalDateTime(getUlidTimestamp(value));
    }

    /**
     * 按 ULID 字符串顺序比较两个 ULID。
     *
     * @param left 左侧 ULID
     * @param right 右侧 ULID
     * @return 比较结果
     */
    public static int compareUlid(String left, String right) {
        if (!isUlid(left) || !isUlid(right)) {
            throw new IllegalArgumentException("ULID 格式不正确");
        }
        return left.toUpperCase(Locale.ROOT).compareTo(right.toUpperCase(Locale.ROOT));
    }

    /**
     * 生成默认业务单号，格式为前缀 + yyyyMMddHHmmssSSS + 6 位随机码。
     *
     * @param prefix 业务前缀
     * @return 业务单号
     */
    public static String bizNo(String prefix) {
        return bizNo(prefix, "yyyyMMddHHmmssSSS", 6);
    }

    /**
     * 使用指定日期格式生成业务单号。
     *
     * @param prefix 业务前缀
     * @param datePattern 日期格式
     * @return 业务单号
     */
    public static String bizNo(String prefix, String datePattern) {
        return bizNo(prefix, datePattern, 6);
    }

    /**
     * 使用默认日期格式和指定随机位数生成业务单号。
     *
     * @param prefix 业务前缀
     * @param randomLength 随机位数
     * @return 业务单号
     */
    public static String bizNo(String prefix, int randomLength) {
        return bizNo(prefix, "yyyyMMddHHmmssSSS", randomLength);
    }

    /**
     * 使用指定前缀、日期格式和随机位数生成业务单号。
     *
     * @param prefix 业务前缀
     * @param datePattern 日期格式
     * @param randomLength 随机位数
     * @return 业务单号
     */
    public static String bizNo(String prefix, String datePattern, int randomLength) {
        String cleanPrefix = requirePrefix(prefix);
        DateTimeFormatter formatter = dateFormatter(datePattern);
        return cleanPrefix + LocalDateTime.now().format(formatter) + randomMix(randomLength);
    }

    /**
     * 生成前缀 + Snowflake ID 格式的业务单号。
     *
     * @param prefix 业务前缀
     * @return 业务单号
     */
    public static String bizNoBySnowflake(String prefix) {
        return requirePrefix(prefix) + snowflakeIdStr();
    }

    /**
     * 生成前缀 + yyyyMMdd + Snowflake ID 格式的业务单号。
     *
     * @param prefix 业务前缀
     * @return 业务单号
     */
    public static String bizNoByDateAndSnowflake(String prefix) {
        return requirePrefix(prefix) + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE) + snowflakeIdStr();
    }

    /**
     * 生成订单号。
     *
     * @return 订单号
     */
    public static String orderNo() {
        return bizNoByDateAndSnowflake("ORD");
    }

    /**
     * 生成支付单号。
     *
     * @return 支付单号
     */
    public static String payNo() {
        return bizNoByDateAndSnowflake("PAY");
    }

    /**
     * 生成退款单号。
     *
     * @return 退款单号
     */
    public static String refundNo() {
        return bizNoByDateAndSnowflake("REF");
    }

    /**
     * 生成交易流水号。
     *
     * @return 交易流水号
     */
    public static String tradeNo() {
        return bizNoByDateAndSnowflake("TRD");
    }

    /**
     * 生成通用序列号。
     *
     * @param prefix 序列号前缀
     * @return 序列号
     */
    public static String serialNo(String prefix) {
        return bizNoByDateAndSnowflake(prefix);
    }

    /**
     * 生成批次号。
     *
     * @param prefix 批次号前缀
     * @return 批次号
     */
    public static String batchNo(String prefix) {
        return bizNoByDateAndSnowflake(prefix);
    }

    /**
     * 生成默认 TraceId。
     *
     * @return TraceId
     */
    public static String traceId() {
        return uuidV7Simple();
    }

    /**
     * 生成无横线 TraceId。
     *
     * @return 无横线 TraceId
     */
    public static String traceIdSimple() {
        return traceId();
    }

    /**
     * 生成较短 TraceId。
     *
     * @return 短 TraceId
     */
    public static String traceIdShort() {
        return shortId(16);
    }

    /**
     * 生成请求 ID。
     *
     * @return 请求 ID
     */
    public static String requestId() {
        return traceId();
    }

    /**
     * 生成 SpanId。
     *
     * @return SpanId
     */
    public static String spanId() {
        return bytesToHex(randomBytes(8));
    }

    /**
     * 生成父 SpanId。
     *
     * @return 父 SpanId
     */
    public static String parentSpanId() {
        return spanId();
    }

    /**
     * 判断字符串是否为 TraceId。
     *
     * @param value 待判断字符串
     * @return 是否为 TraceId
     */
    public static boolean isTraceId(String value) {
        return value != null && TRACE_PATTERN.matcher(value).matches();
    }

    /**
     * 确保返回合法 TraceId，传入无效时自动生成新的 TraceId。
     *
     * @param value 候选 TraceId
     * @return 可用 TraceId
     */
    public static String ensureTraceId(String value) {
        return isTraceId(value) ? value : traceId();
    }

    /**
     * 获取已有 TraceId，传入无效时创建新的 TraceId。
     *
     * @param value 候选 TraceId
     * @return 可用 TraceId
     */
    public static String getOrCreateTraceId(String value) {
        return ensureTraceId(value);
    }

    /**
     * 生成默认 8 位短 ID。
     *
     * @return 短 ID
     */
    public static String shortId() {
        return shortId(8);
    }

    /**
     * 生成指定长度的短 ID。
     *
     * @param length 长度
     * @return 短 ID
     */
    public static String shortId(int length) {
        return randomMix(length);
    }

    /**
     * 生成指定长度的大写短 ID。
     *
     * @param length 长度
     * @return 大写短 ID
     */
    public static String shortIdUpper(int length) {
        return randomUpperLetter(length);
    }

    /**
     * 生成指定长度的小写短 ID。
     *
     * @param length 长度
     * @return 小写短 ID
     */
    public static String shortIdLower(int length) {
        return randomLowerLetter(length);
    }

    /**
     * 生成指定长度的数字短 ID。
     *
     * @param length 长度
     * @return 数字短 ID
     */
    public static String shortIdWithNumber(int length) {
        return randomNumber(length);
    }

    /**
     * 生成指定长度的字母短 ID。
     *
     * @param length 长度
     * @return 字母短 ID
     */
    public static String shortIdWithLetter(int length) {
        return randomLetter(length);
    }

    /**
     * 生成指定长度的数字字母混合短 ID。
     *
     * @param length 长度
     * @return 混合短 ID
     */
    public static String shortIdWithMix(int length) {
        return randomMix(length);
    }

    /**
     * 生成基于 UUID 的短 ID。
     *
     * @return 短 UUID
     */
    public static String shortUuid() {
        return uuidToBase64Url(uuid());
    }

    /**
     * 生成默认 NanoID 风格短 ID，长度为 21。
     *
     * @return NanoID 风格短 ID
     */
    public static String nanoId() {
        return nanoId(21);
    }

    /**
     * 生成指定长度的 NanoID 风格短 ID。
     *
     * @param length 长度
     * @return NanoID 风格短 ID
     */
    public static String nanoId(int length) {
        return randomWithAlphabet(URL_SAFE_ALPHABET, length);
    }

    /**
     * 判断字符串是否为指定长度短 ID。
     *
     * @param value 待判断字符串
     * @param length 期望长度
     * @return 是否为短 ID
     */
    public static boolean isShortId(String value, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("短 ID 长度必须大于 0");
        }
        return value != null && value.length() == length && SIMPLE_ID_PATTERN.matcher(value).matches();
    }

    /**
     * 生成指定长度的数字随机码。
     *
     * @param length 长度
     * @return 数字随机码
     */
    public static String randomCode(int length) {
        return randomNumber(length);
    }

    /**
     * 生成指定长度的数字随机码。
     *
     * @param length 长度
     * @return 数字随机码
     */
    public static String randomNumber(int length) {
        return randomWithAlphabet(NUMBER_ALPHABET, length);
    }

    /**
     * 生成指定长度的字母随机码。
     *
     * @param length 长度
     * @return 字母随机码
     */
    public static String randomLetter(int length) {
        return randomWithAlphabet(LETTER_ALPHABET, length);
    }

    /**
     * 生成指定长度的大写字母随机码。
     *
     * @param length 长度
     * @return 大写字母随机码
     */
    public static String randomUpperLetter(int length) {
        return randomWithAlphabet(UPPER_ALPHABET, length);
    }

    /**
     * 生成指定长度的小写字母随机码。
     *
     * @param length 长度
     * @return 小写字母随机码
     */
    public static String randomLowerLetter(int length) {
        return randomWithAlphabet(LOWER_ALPHABET, length);
    }

    /**
     * 生成指定长度的数字字母混合随机码。
     *
     * @param length 长度
     * @return 混合随机码
     */
    public static String randomMix(int length) {
        return randomWithAlphabet(MIX_ALPHABET, length);
    }

    /**
     * 生成指定长度的易读随机码，排除部分易混淆字符。
     *
     * @param length 长度
     * @return 易读随机码
     */
    public static String randomReadable(int length) {
        return randomWithAlphabet(READABLE_ALPHABET, length);
    }

    /**
     * 生成 6 位短信验证码。
     *
     * @return 短信验证码
     */
    public static String smsCode() {
        return randomNumber(6);
    }

    /**
     * 生成 6 位邮箱验证码。
     *
     * @return 邮箱验证码
     */
    public static String emailCode() {
        return randomMix(6);
    }

    /**
     * 生成文件 ID。
     *
     * @return 文件 ID
     */
    public static String fileId() {
        return uuidSimple();
    }

    /**
     * 根据原始文件名生成唯一文件名。
     *
     * @param originalName 原始文件名
     * @return 唯一文件名
     */
    public static String fileName(String originalName) {
        return fileId() + getFileSuffix(originalName);
    }

    /**
     * 根据前缀和原始文件名生成唯一文件名。
     *
     * @param prefix 文件名前缀
     * @param originalName 原始文件名
     * @return 唯一文件名
     */
    public static String fileName(String prefix, String originalName) {
        return cleanFilePart(requireText(prefix, "文件名前缀不能为空")) + "_" + fileName(originalName);
    }

    /**
     * 根据原始文件名生成对象存储 Key。
     *
     * @param originalName 原始文件名
     * @return 对象存储 Key
     */
    public static String fileKey(String originalName) {
        return fileKey("", originalName);
    }

    /**
     * 根据目录和原始文件名生成对象存储 Key。
     *
     * @param dir 业务目录，可为空
     * @param originalName 原始文件名
     * @return 对象存储 Key
     */
    public static String fileKey(String dir, String originalName) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String cleanDir = cleanDir(dir);
        String name = fileName(originalName);
        return cleanDir.isEmpty() ? datePath + "/" + name : cleanDir + "/" + datePath + "/" + name;
    }

    /**
     * 生成临时文件名。
     *
     * @param suffix 文件后缀，支持传入 zip 或 .zip
     * @return 临时文件名
     */
    public static String tempFileName(String suffix) {
        return "tmp_" + fileId() + normalizeSuffix(suffix);
    }

    /**
     * 生成导出文件名。
     *
     * @param prefix 文件名前缀
     * @param suffix 文件后缀，支持传入 xlsx 或 .xlsx
     * @return 导出文件名
     */
    public static String exportFileName(String prefix, String suffix) {
        String cleanPrefix = cleanFilePart(requireText(prefix, "导出文件名前缀不能为空"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return cleanPrefix + "_" + time + normalizeSuffix(suffix);
    }

    /**
     * 将非负 long 转为 Base36 字符串。
     *
     * @param value 非负 long
     * @return Base36 字符串
     */
    public static String toBase36(long value) {
        requireNonNegative(value, "Base36 编码值不能为负数");
        return Long.toString(value, 36).toUpperCase(Locale.ROOT);
    }

    /**
     * 将 Base36 字符串转为 long。
     *
     * @param value Base36 字符串
     * @return long 值
     */
    public static long fromBase36(String value) {
        String text = requireText(value, "Base36 字符串不能为空").toUpperCase(Locale.ROOT);
        try {
            return Long.parseLong(text, 36);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Base36 字符串不合法: " + value, e);
        }
    }

    /**
     * 将非负 long 转为 Base62 字符串。
     *
     * @param value 非负 long
     * @return Base62 字符串
     */
    public static String toBase62(long value) {
        requireNonNegative(value, "Base62 编码值不能为负数");
        if (value == 0L) {
            return "0";
        }
        StringBuilder builder = new StringBuilder();
        long current = value;
        while (current > 0) {
            int index = (int) (current % 62);
            builder.append(BASE62_ALPHABET[index]);
            current /= 62;
        }
        return builder.reverse().toString();
    }

    /**
     * 将 Base62 字符串转为 long。
     *
     * @param value Base62 字符串
     * @return long 值
     */
    public static long fromBase62(String value) {
        String text = requireText(value, "Base62 字符串不能为空");
        BigInteger result = BigInteger.ZERO;
        BigInteger radix = BigInteger.valueOf(62);
        for (int i = 0; i < text.length(); i++) {
            int index = base62Index(text.charAt(i));
            if (index < 0) {
                throw new IllegalArgumentException("Base62 字符串不合法: " + value);
            }
            result = result.multiply(radix).add(BigInteger.valueOf(index));
        }
        BigInteger max = BigInteger.valueOf(Long.MAX_VALUE);
        if (result.compareTo(max) > 0) {
            throw new IllegalArgumentException("Base62 字符串超出 long 范围: " + value);
        }
        return result.longValue();
    }

    /**
     * 将字节数组编码为 URL 安全 Base64 字符串，不包含填充符。
     *
     * @param value 字节数组
     * @return URL 安全 Base64 字符串
     */
    public static String toBase64Url(byte[] value) {
        Objects.requireNonNull(value, "字节数组不能为空");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    /**
     * 将 URL 安全 Base64 字符串解码为字节数组。
     *
     * @param value URL 安全 Base64 字符串
     * @return 字节数组
     */
    public static byte[] fromBase64Url(String value) {
        String text = requireText(value, "Base64Url 字符串不能为空");
        try {
            return Base64.getUrlDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Base64Url 字符串不合法: " + value, e);
        }
    }

    /**
     * 将 UUID 压缩为 URL 安全 Base64 字符串。
     *
     * @param uuid UUID 字符串
     * @return 压缩后的 UUID 字符串
     */
    public static String uuidToBase64Url(String uuid) {
        UUID parsed = UUID.fromString(normalizeUuid(uuid));
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(parsed.getMostSignificantBits());
        buffer.putLong(parsed.getLeastSignificantBits());
        return toBase64Url(buffer.array());
    }

    /**
     * 将 URL 安全 Base64 字符串还原为 UUID。
     *
     * @param value 压缩后的 UUID 字符串
     * @return 标准 UUID 字符串
     */
    public static String base64UrlToUuid(String value) {
        byte[] bytes = fromBase64Url(value);
        if (bytes.length != 16) {
            throw new IllegalArgumentException("压缩 UUID 解码后必须为 16 字节");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong()).toString();
    }

    /**
     * 将 Snowflake ID 转为 Base62 字符串。
     *
     * @param id Snowflake ID
     * @return Base62 字符串
     */
    public static String snowflakeToBase62(long id) {
        requirePositive(id, "Snowflake ID 必须大于 0");
        return toBase62(id);
    }

    /**
     * 将 Base62 字符串还原为 Snowflake ID。
     *
     * @param value Base62 字符串
     * @return Snowflake ID
     */
    public static long base62ToSnowflake(String value) {
        long id = fromBase62(value);
        if (!isSnowflakeId(Long.toString(id))) {
            throw new IllegalArgumentException("Base62 字符串不是合法 Snowflake ID: " + value);
        }
        return id;
    }

    /**
     * 判断字符串是否为通用 ID。
     *
     * @param value 待判断字符串
     * @return 是否为通用 ID
     */
    public static boolean isId(String value) {
        if (isBlank(value) || value.length() > 128) {
            return false;
        }
        return isUuid(value)
                || isUuidSimple(value)
                || isUuidV7(value)
                || isUlid(value)
                || isSnowflakeId(value)
                || isBizNo(value)
                || isTraceId(value)
                || SIMPLE_ID_PATTERN.matcher(value).matches();
    }

    /**
     * 判断字符串是否为数字 ID。
     *
     * @param value 待判断字符串
     * @return 是否为数字 ID
     */
    public static boolean isNumericId(String value) {
        return value != null && NUMERIC_PATTERN.matcher(value).matches();
    }

    /**
     * 判断字符串是否为正 long ID。
     *
     * @param value 待判断字符串
     * @return 是否为正 long ID
     */
    public static boolean isLongId(String value) {
        if (!isNumericId(value)) {
            return false;
        }
        try {
            return Long.parseLong(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为业务单号。
     *
     * @param value 待判断字符串
     * @return 是否为业务单号
     */
    public static boolean isBizNo(String value) {
        return value != null && BIZ_NO_PATTERN.matcher(value).matches();
    }

    /**
     * 将字符串解析为正 long ID。
     *
     * @param value ID 字符串
     * @return long ID
     */
    public static long parseLongId(String value) {
        if (!isLongId(value)) {
            throw new IllegalArgumentException("不是合法的正 long ID: " + value);
        }
        return Long.parseLong(value);
    }

    /**
     * 尝试将字符串解析为正 long ID。
     *
     * @param value ID 字符串
     * @return 解析成功时返回 OptionalLong，否则返回空
     */
    public static OptionalLong tryParseLongId(String value) {
        return isLongId(value) ? OptionalLong.of(Long.parseLong(value)) : OptionalLong.empty();
    }

    /**
     * 获取字符串 ID 的类型。
     *
     * @param value ID 字符串
     * @return ID 类型
     */
    public static IdType getIdType(String value) {
        if (isBlank(value)) {
            return IdType.UNKNOWN;
        }
        if (isUuidV7(value)) {
            return IdType.UUID_V7;
        }
        if (isUuid(value) || isUuidSimple(value)) {
            return IdType.UUID;
        }
        if (isUlid(value)) {
            return IdType.ULID;
        }
        if (isSnowflakeId(value)) {
            return IdType.SNOWFLAKE;
        }
        if (isBizNo(value)) {
            return IdType.BIZ_NO;
        }
        if (isTraceId(value)) {
            return IdType.TRACE_ID;
        }
        if (isNumericId(value)) {
            return IdType.NUMERIC;
        }
        if (value.length() >= 4 && value.length() <= 32 && SIMPLE_ID_PATTERN.matcher(value).matches()) {
            return IdType.SHORT_ID;
        }
        return IdType.UNKNOWN;
    }

    /**
     * 校验并返回通用 ID。
     *
     * @param value ID 字符串
     * @return 合法 ID 字符串
     */
    public static String requireId(String value) {
        if (!isId(value)) {
            throw new IllegalArgumentException("ID 不合法: " + value);
        }
        return value;
    }

    /**
     * 校验并返回正 long ID。
     *
     * @param value ID 字符串
     * @return long ID
     */
    public static long requireLongId(String value) {
        return parseLongId(value);
    }

    /**
     * 为 ID 添加前缀。
     *
     * @param prefix 前缀
     * @param id 原始 ID
     * @return 带前缀 ID
     */
    public static String withPrefix(String prefix, String id) {
        return requirePrefix(prefix) + requireText(id, "ID 不能为空");
    }

    /**
     * 移除 ID 中的指定前缀，不存在该前缀时返回原值。
     *
     * @param prefix 前缀
     * @param id ID 字符串
     * @return 移除前缀后的 ID
     */
    public static String removePrefix(String prefix, String id) {
        String cleanPrefix = requirePrefix(prefix);
        String text = requireText(id, "ID 不能为空");
        return text.startsWith(cleanPrefix) ? text.substring(cleanPrefix.length()) : text;
    }

    /**
     * 判断 ID 是否包含指定前缀。
     *
     * @param prefix 前缀
     * @param id ID 字符串
     * @return 是否包含前缀
     */
    public static boolean hasPrefix(String prefix, String id) {
        return !isBlank(id) && id.startsWith(requirePrefix(prefix));
    }

    /**
     * 按前缀、日期格式和后缀格式化业务 ID。
     *
     * @param prefix 前缀
     * @param datePattern 日期格式
     * @param suffix 后缀
     * @return 格式化后的业务 ID
     */
    public static String formatWithDate(String prefix, String datePattern, String suffix) {
        return requirePrefix(prefix) + LocalDateTime.now().format(dateFormatter(datePattern)) + requireText(suffix, "后缀不能为空");
    }

    /**
     * 按前缀、日期字符串和序列字符串格式化业务单号。
     *
     * @param prefix 前缀
     * @param date 日期字符串
     * @param sequence 序列字符串
     * @return 业务单号
     */
    public static String formatBizNo(String prefix, String date, String sequence) {
        return requirePrefix(prefix) + requireText(date, "日期不能为空") + requireText(sequence, "序列不能为空");
    }

    /**
     * 拆分常见业务单号，返回 prefix、date、sequence 三个片段。
     *
     * @param bizNo 业务单号
     * @return 业务单号片段
     */
    public static Map<String, String> splitBizNo(String bizNo) {
        String text = requireText(bizNo, "业务单号不能为空");
        int firstDigit = -1;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                firstDigit = i;
                break;
            }
        }
        if (firstDigit <= 0) {
            throw new IllegalArgumentException("业务单号无法拆分: " + bizNo);
        }
        int dateEnd = Math.min(firstDigit + 14, text.length());
        int end = firstDigit;
        while (end < dateEnd && Character.isDigit(text.charAt(end))) {
            end++;
        }
        if (end - firstDigit < 8 || end >= text.length()) {
            throw new IllegalArgumentException("业务单号无法拆分: " + bizNo);
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("prefix", text.substring(0, firstDigit));
        result.put("date", text.substring(firstDigit, end));
        result.put("sequence", text.substring(end));
        return result;
    }

    /**
     * 将 long 值左侧补零到指定长度。
     *
     * @param value 原始值
     * @param length 目标长度
     * @return 补齐后的字符串
     */
    public static String padLeft(long value, int length) {
        return padLeft(Long.toString(value), length);
    }

    /**
     * 将字符串左侧补零到指定长度。
     *
     * @param value 原始字符串
     * @param length 目标长度
     * @return 补齐后的字符串
     */
    public static String padLeft(String value, int length) {
        String text = requireText(value, "补齐字符串不能为空");
        checkLength(length);
        if (text.length() >= length) {
            return text;
        }
        return "0".repeat(length - text.length()) + text;
    }

    /**
     * 将字符串右侧补零到指定长度。
     *
     * @param value 原始字符串
     * @param length 目标长度
     * @return 补齐后的字符串
     */
    public static String padRight(String value, int length) {
        String text = requireText(value, "补齐字符串不能为空");
        checkLength(length);
        if (text.length() >= length) {
            return text;
        }
        return text + "0".repeat(length - text.length());
    }

    /**
     * 生成默认 32 位安全随机 ID。
     *
     * @return 安全随机 ID
     */
    public static String secureId() {
        return secureId(32);
    }

    /**
     * 生成指定长度的安全随机 ID。
     *
     * @param length 长度
     * @return 安全随机 ID
     */
    public static String secureId(int length) {
        return randomWithAlphabet(URL_SAFE_ALPHABET, length);
    }

    /**
     * 生成默认安全 Token，默认使用 32 字节随机数。
     *
     * @return 安全 Token
     */
    public static String secureToken() {
        return secureToken(32);
    }

    /**
     * 生成指定字节长度的安全 Token。
     *
     * @param byteLength 随机字节长度
     * @return 安全 Token
     */
    public static String secureToken(int byteLength) {
        checkLength(byteLength);
        return toBase64Url(randomBytes(byteLength));
    }

    /**
     * 生成默认临时密钥字符串。
     *
     * @return 临时密钥
     */
    public static String secretKey() {
        return secureToken(32);
    }

    /**
     * 生成默认 nonce 字符串。
     *
     * @return nonce 字符串
     */
    public static String nonce() {
        return nonce(16);
    }

    /**
     * 生成指定长度的 nonce 字符串。
     *
     * @param length 长度
     * @return nonce 字符串
     */
    public static String nonce(int length) {
        return secureId(length);
    }

    /**
     * 获取默认机器 ID。
     *
     * @return 机器 ID，范围 0 到 31
     */
    public static long workerId() {
        return workerIdByIp();
    }

    /**
     * 获取默认数据中心 ID。
     *
     * @return 数据中心 ID，范围 0 到 31
     */
    public static long dataCenterId() {
        return workerIdByHostName();
    }

    /**
     * 根据本机 IP 计算机器 ID。
     *
     * @return 机器 ID，范围 0 到 31
     */
    public static long workerIdByIp() {
        try {
            return hashToRange(InetAddress.getLocalHost().getHostAddress(), MAX_WORKER_ID);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 根据本机主机名计算机器 ID。
     *
     * @return 机器 ID，范围 0 到 31
     */
    public static long workerIdByHostName() {
        try {
            return hashToRange(InetAddress.getLocalHost().getHostName(), MAX_DATA_CENTER_ID);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 从环境变量中读取机器 ID。
     *
     * @param envName 环境变量名称
     * @return 机器 ID
     */
    public static long workerIdByEnv(String envName) {
        String name = requireText(envName, "环境变量名称不能为空");
        String value = System.getenv(name);
        if (isBlank(value)) {
            throw new IllegalArgumentException("环境变量不存在或为空: " + envName);
        }
        try {
            long workerId = Long.parseLong(value.trim());
            checkWorkerId(workerId);
            return workerId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("环境变量不是合法机器 ID: " + envName, e);
        }
    }

    /**
     * 校验机器 ID 范围。
     *
     * @param workerId 机器 ID
     */
    public static void checkWorkerId(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 范围必须为 0 到 " + MAX_WORKER_ID);
        }
    }

    /**
     * 校验数据中心 ID 范围。
     *
     * @param dataCenterId 数据中心 ID
     */
    public static void checkDataCenterId(long dataCenterId) {
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID) {
            throw new IllegalArgumentException("dataCenterId 范围必须为 0 到 " + MAX_DATA_CENTER_ID);
        }
    }

    private static String requirePrefix(String prefix) {
        String text = requireText(prefix, "前缀不能为空");
        if (!PREFIX_PATTERN.matcher(text).matches()) {
            throw new IllegalArgumentException("前缀只能包含字母、数字、下划线或中横线: " + prefix);
        }
        return text;
    }

    private static String requireText(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean canParseUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String toStandardUuid(String value) {
        return value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16)
                + "-" + value.substring(16, 20) + "-" + value.substring(20);
    }

    private static LocalDateTime toLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(DEFAULT_ZONE).toLocalDateTime();
    }

    private static void requirePositive(long value, String message) {
        if (value <= 0L) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonNegative(long value, String message) {
        if (value < 0L) {
            throw new IllegalArgumentException(message);
        }
    }

    private static DateTimeFormatter dateFormatter(String datePattern) {
        try {
            return DateTimeFormatter.ofPattern(requireText(datePattern, "日期格式不能为空"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("日期格式不正确: " + datePattern, e);
        }
    }

    private static void checkLength(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于 0");
        }
    }

    private static byte[] randomBytes(int length) {
        checkLength(length);
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static String randomWithAlphabet(String alphabet, int length) {
        checkLength(length);
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private static String encodeUlid(long timestamp, byte[] randomBytes) {
        if (timestamp < 0 || timestamp > 0xFFFF_FFFF_FFFFL) {
            throw new IllegalArgumentException("ULID 时间戳超出 48 位范围");
        }
        if (randomBytes == null || randomBytes.length != 10) {
            throw new IllegalArgumentException("ULID 随机段必须为 10 字节");
        }
        char[] chars = new char[26];
        for (int i = 9; i >= 0; i--) {
            chars[i] = ULID_CHARS[(int) (timestamp & 0x1F)];
            timestamp >>>= 5;
        }
        BigInteger random = new BigInteger(1, randomBytes);
        for (int i = 25; i >= 10; i--) {
            BigInteger[] divRem = random.divideAndRemainder(BigInteger.valueOf(32));
            chars[i] = ULID_CHARS[divRem[1].intValue()];
            random = divRem[0];
        }
        return new String(chars);
    }

    private static byte[] incrementRandom80(byte[] random) {
        BigInteger value = new BigInteger(1, random).add(BigInteger.ONE);
        BigInteger limit = BigInteger.ONE.shiftLeft(80);
        if (value.compareTo(limit) >= 0) {
            value = BigInteger.ZERO;
        }
        byte[] raw = value.toByteArray();
        byte[] result = new byte[10];
        int copyLength = Math.min(raw.length, result.length);
        System.arraycopy(raw, raw.length - copyLength, result, result.length - copyLength, copyLength);
        return result;
    }

    private static String getFileSuffix(String originalName) {
        String name = requireText(originalName, "原始文件名不能为空");
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == name.length() - 1) {
            return "";
        }
        return normalizeSuffix(name.substring(dotIndex + 1));
    }

    private static String normalizeSuffix(String suffix) {
        String text = requireText(suffix, "文件后缀不能为空");
        String clean = text.startsWith(".") ? text.substring(1) : text;
        clean = cleanFilePart(clean);
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("文件后缀不能为空");
        }
        return "." + clean;
    }

    private static String cleanFilePart(String value) {
        String clean = FILE_INVALID_PATTERN.matcher(value.trim()).replaceAll("_");
        while (clean.startsWith(".")) {
            clean = clean.substring(1);
        }
        return clean;
    }

    private static String cleanDir(String dir) {
        if (isBlank(dir)) {
            return "";
        }
        String clean = dir.trim().replace('\\', '/');
        clean = clean.replace("..", "");
        clean = DIR_INVALID_PATTERN.matcher(clean).replaceAll("_");
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private static int base62Index(char c) {
        for (int i = 0; i < BASE62_ALPHABET.length; i++) {
            if (BASE62_ALPHABET[i] == c) {
                return i;
            }
        }
        return -1;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            hex[i * 2] = digits[value >>> 4];
            hex[i * 2 + 1] = digits[value & 0x0F];
        }
        return new String(hex);
    }

    private static long hashToRange(String value, long max) {
        long hash = Integer.toUnsignedLong(Objects.requireNonNullElse(value, "").hashCode());
        return hash % (max + 1);
    }

    private static final class Snowflake {
        private final long workerId;
        private final long dataCenterId;
        private long sequence;
        private long lastTimestamp = -1L;

        private Snowflake(long workerId, long dataCenterId) {
            this.workerId = workerId;
            this.dataCenterId = dataCenterId;
        }

        private synchronized long nextId() {
            long timestamp = System.currentTimeMillis();
            if (timestamp < lastTimestamp) {
                timestamp = waitUntil(lastTimestamp);
            }
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0L) {
                    timestamp = waitUntil(lastTimestamp + 1);
                }
            } else {
                sequence = 0L;
            }
            lastTimestamp = timestamp;
            return ((timestamp - SNOWFLAKE_EPOCH) << TIMESTAMP_LEFT_SHIFT)
                    | (dataCenterId << DATA_CENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        }

        private long waitUntil(long targetTimestamp) {
            long timestamp = System.currentTimeMillis();
            while (timestamp < targetTimestamp) {
                try {
                    Thread.sleep(Math.min(1L, targetTimestamp - timestamp));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("等待 Snowflake 时钟恢复时被中断", e);
                }
                timestamp = System.currentTimeMillis();
            }
            return timestamp;
        }
    }
}
