package io.github.atengk.utils.random;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * 随机工具类，提供数字、字符串、集合、时间、安全随机、权重随机、模拟数据等常用随机能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class RandomUtil {

    private static final String LOWER_LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LETTERS = LOWER_LETTERS + UPPER_LETTERS;
    private static final String NUMBERS = "0123456789";
    private static final String ALPHA_NUMERIC = LETTERS + NUMBERS;
    private static final String READABLE_CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{};:,.?";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final AtomicReference<RandomGenerator> CUSTOM_GENERATOR = new AtomicReference<>();
    private static final DateTimeFormatter BIZ_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String[] COLOR_NAMES = {"red", "orange", "yellow", "green", "blue", "purple", "pink", "gray", "black", "white"};
    private static final String[] MOBILE_PREFIXES = {"130", "131", "132", "133", "135", "136", "137", "138", "139", "150", "151", "152", "157", "158", "159", "170", "171", "172", "173", "175", "176", "177", "178", "180", "181", "182", "183", "185", "186", "187", "188", "189", "191", "193", "195", "196", "197", "198", "199"};
    private static final String[] SURNAMES = {"赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫", "蒋", "沈", "韩", "杨", "朱", "秦", "尤", "许"};
    private static final String[] GIVEN_NAMES = {"伟", "芳", "娜", "敏", "静", "强", "磊", "军", "洋", "勇", "艳", "杰", "娟", "涛", "明", "超", "秀英", "华", "慧", "巧美"};
    private static final String[] ENGLISH_FIRST_NAMES = {"James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda", "William", "Elizabeth"};
    private static final String[] ENGLISH_LAST_NAMES = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Wilson", "Taylor"};
    private static final String[] PROVINCES = {"北京市", "上海市", "广东省", "浙江省", "江苏省", "四川省", "湖北省", "湖南省", "山东省", "福建省"};
    private static final String[] CITIES = {"北京市", "上海市", "广州市", "深圳市", "杭州市", "南京市", "成都市", "武汉市", "长沙市", "厦门市"};

    private RandomUtil() {
        throw new AssertionError("RandomUtil 不允许实例化");
    }

    /**
     * 随机生成布尔值。
     *
     * @return 随机布尔值
     */
    public static boolean randomBoolean() {
        return currentGenerator().nextBoolean();
    }

    /**
     * 按指定概率随机返回 true。
     *
     * @param probability true 的概率，范围为 0.0 到 1.0
     * @return 是否命中
     */
    public static boolean randomBoolean(double probability) {
        requireProbability(probability);
        return probability == 1.0 || (probability > 0.0 && currentGenerator().nextDouble() < probability);
    }

    /**
     * 随机生成二进制位。
     *
     * @return 0 或 1
     */
    public static int randomBit() {
        return currentGenerator().nextInt(2);
    }

    /**
     * 随机生成符号值。
     *
     * @return 1 或 -1
     */
    public static int randomSign() {
        return randomBoolean() ? 1 : -1;
    }

    /**
     * 随机生成百分比小数。
     *
     * @return 0.0 到 1.0 之间的小数，包含 0.0，不包含 1.0
     */
    public static double randomPercent() {
        return currentGenerator().nextDouble();
    }

    /**
     * 按百分比概率判断是否命中。
     *
     * @param percent 命中百分比，范围为 0 到 100
     * @return 是否命中
     */
    public static boolean randomChance(int percent) {
        requirePercent(percent);
        return percent == 100 || (percent > 0 && currentGenerator().nextInt(100) < percent);
    }

    /**
     * 随机生成 int 值。
     *
     * @return 随机 int 值
     */
    public static int randomInt() {
        return currentGenerator().nextInt();
    }

    /**
     * 随机生成指定上界内的 int 值。
     *
     * @param bound 上界，不包含该值，必须大于 0
     * @return 0 到 bound 之间的随机整数，不包含 bound
     */
    public static int randomInt(int bound) {
        requirePositive(bound, "bound");
        return currentGenerator().nextInt(bound);
    }

    /**
     * 随机生成指定范围内的 int 值。
     *
     * @param min 最小值，包含
     * @param max 最大值，包含
     * @return 指定范围内的随机整数
     */
    public static int randomInt(int min, int max) {
        return Math.toIntExact(nextLongInclusive(currentGenerator(), min, max));
    }

    /**
     * 随机生成 long 值。
     *
     * @return 随机 long 值
     */
    public static long randomLong() {
        return currentGenerator().nextLong();
    }

    /**
     * 随机生成指定上界内的 long 值。
     *
     * @param bound 上界，不包含该值，必须大于 0
     * @return 0 到 bound 之间的随机长整数，不包含 bound
     */
    public static long randomLong(long bound) {
        requirePositive(bound, "bound");
        return currentGenerator().nextLong(bound);
    }

    /**
     * 随机生成指定范围内的 long 值。
     *
     * @param min 最小值，包含
     * @param max 最大值，包含
     * @return 指定范围内的随机长整数
     */
    public static long randomLong(long min, long max) {
        return nextLongInclusive(currentGenerator(), min, max);
    }

    /**
     * 随机生成 double 值。
     *
     * @return 0.0 到 1.0 之间的小数，包含 0.0，不包含 1.0
     */
    public static double randomDouble() {
        return currentGenerator().nextDouble();
    }

    /**
     * 随机生成指定范围内的 double 值。
     *
     * @param min 最小值，包含
     * @param max 最大值，不包含
     * @return 指定范围内的随机小数
     */
    public static double randomDouble(double min, double max) {
        requireFinite(min, "min");
        requireFinite(max, "max");
        if (min >= max) {
            throw new IllegalArgumentException("min 必须小于 max");
        }
        return currentGenerator().nextDouble(min, max);
    }

    /**
     * 随机生成指定小数位数的 BigDecimal。
     *
     * @param scale 小数位数，不能小于 0
     * @return 0.0 到 1.0 之间的随机 BigDecimal
     */
    public static BigDecimal randomDecimal(int scale) {
        requireNonNegative(scale, "scale");
        return BigDecimal.valueOf(randomDouble()).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 随机生成指定范围和小数位数的 BigDecimal。
     *
     * @param min 最小值，包含
     * @param max 最大值，不包含
     * @param scale 小数位数，不能小于 0
     * @return 指定范围内的随机 BigDecimal
     */
    public static BigDecimal randomDecimal(double min, double max, int scale) {
        requireNonNegative(scale, "scale");
        return BigDecimal.valueOf(randomDouble(min, max)).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 随机生成指定范围内的偶数。
     *
     * @param min 最小值，包含
     * @param max 最大值，包含
     * @return 指定范围内的随机偶数
     */
    public static int randomEven(int min, int max) {
        requireRange(min, max);
        long start = min % 2 == 0 ? min : (long) min + 1;
        long end = max % 2 == 0 ? max : (long) max - 1;
        if (start > end) {
            throw new IllegalArgumentException("指定范围内不存在偶数");
        }
        return Math.toIntExact(start + randomLong(0, (end - start) / 2) * 2);
    }

    /**
     * 随机生成指定范围内的奇数。
     *
     * @param min 最小值，包含
     * @param max 最大值，包含
     * @return 指定范围内的随机奇数
     */
    public static int randomOdd(int min, int max) {
        requireRange(min, max);
        long start = min % 2 != 0 ? min : (long) min + 1;
        long end = max % 2 != 0 ? max : (long) max - 1;
        if (start > end) {
            throw new IllegalArgumentException("指定范围内不存在奇数");
        }
        return Math.toIntExact(start + randomLong(0, (end - start) / 2) * 2);
    }

    /**
     * 生成默认字符集随机字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 随机字符串
     */
    public static String randomString(int length) {
        return randomString(ALPHA_NUMERIC, length);
    }

    /**
     * 从指定字符集中生成随机字符串。
     *
     * @param chars 候选字符集，不能为空
     * @param length 字符串长度，不能小于 0
     * @return 随机字符串
     */
    public static String randomString(String chars, int length) {
        return randomString(currentGenerator(), chars, length);
    }

    /**
     * 生成纯字母随机字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 纯字母随机字符串
     */
    public static String randomLetters(int length) {
        return randomString(LETTERS, length);
    }

    /**
     * 生成小写字母随机字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 小写字母随机字符串
     */
    public static String randomLowerLetters(int length) {
        return randomString(LOWER_LETTERS, length);
    }

    /**
     * 生成大写字母随机字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 大写字母随机字符串
     */
    public static String randomUpperLetters(int length) {
        return randomString(UPPER_LETTERS, length);
    }

    /**
     * 生成纯数字随机字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 纯数字随机字符串
     */
    public static String randomNumbers(int length) {
        return randomString(NUMBERS, length);
    }

    /**
     * 生成字母数字混合随机字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 字母数字混合随机字符串
     */
    public static String randomAlphaNumeric(int length) {
        return randomString(ALPHA_NUMERIC, length);
    }

    /**
     * 生成 ASCII 可见字符随机字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return ASCII 可见字符随机字符串
     */
    public static String randomAscii(int length) {
        requireNonNegative(length, "length");
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append((char) randomInt(33, 126));
        }
        return builder.toString();
    }

    /**
     * 生成随机中文字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 随机中文字符串
     */
    public static String randomChinese(int length) {
        requireNonNegative(length, "length");
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append((char) randomInt(0x4E00, 0x9FA5));
        }
        return builder.toString();
    }

    /**
     * 按简单模板生成随机字符串，# 表示数字，? 表示字母，* 表示字母数字混合，@ 表示大写字母，$ 表示小写字母。
     *
     * @param pattern 模板字符串，不能为空
     * @return 按模板生成的随机字符串
     */
    public static String randomByPattern(String pattern) {
        requireNonBlank(pattern, "pattern");
        String normalized = pattern.replace("yyyyMMdd", LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            builder.append(switch (ch) {
                case '#' -> randomChar(NUMBERS, currentGenerator());
                case '?' -> randomChar(LETTERS, currentGenerator());
                case '*' -> randomChar(ALPHA_NUMERIC, currentGenerator());
                case '@' -> randomChar(UPPER_LETTERS, currentGenerator());
                case '$' -> randomChar(LOWER_LETTERS, currentGenerator());
                default -> ch;
            });
        }
        return builder.toString();
    }

    /**
     * 生成默认数字验证码。
     *
     * @param length 验证码长度，不能小于 0
     * @return 数字验证码
     */
    public static String randomCode(int length) {
        return randomNumberCode(length);
    }

    /**
     * 生成数字验证码。
     *
     * @param length 验证码长度，不能小于 0
     * @return 数字验证码
     */
    public static String randomNumberCode(int length) {
        return randomNumbers(length);
    }

    /**
     * 生成字母验证码。
     *
     * @param length 验证码长度，不能小于 0
     * @return 字母验证码
     */
    public static String randomLetterCode(int length) {
        return randomLetters(length);
    }

    /**
     * 生成字母数字混合验证码。
     *
     * @param length 验证码长度，不能小于 0
     * @return 字母数字混合验证码
     */
    public static String randomMixedCode(int length) {
        return randomString(READABLE_CODE_CHARS, length);
    }

    /**
     * 生成邀请码。
     *
     * @param length 邀请码长度，不能小于 0
     * @return 邀请码
     */
    public static String randomInviteCode(int length) {
        return randomString(READABLE_CODE_CHARS, length);
    }

    /**
     * 生成分享码。
     *
     * @param length 分享码长度，不能小于 0
     * @return 分享码
     */
    public static String randomShareCode(int length) {
        return randomString(READABLE_CODE_CHARS, length);
    }

    /**
     * 生成默认长度短码。
     *
     * @return 8 位短码
     */
    public static String randomShortCode() {
        return randomShortCode(8);
    }

    /**
     * 生成指定长度短码。
     *
     * @param length 短码长度，不能小于 0
     * @return 短码
     */
    public static String randomShortCode(int length) {
        return randomString(READABLE_CODE_CHARS, length);
    }

    /**
     * 生成标准 UUID 字符串。
     *
     * @return 标准 UUID 字符串
     */
    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成不带横线的 UUID 字符串。
     *
     * @return 简化 UUID 字符串
     */
    public static String randomSimpleUuid() {
        return randomUuid().replace("-", "");
    }

    /**
     * 生成链路追踪 ID。
     *
     * @return 链路追踪 ID
     */
    public static String randomTraceId() {
        return randomSimpleUuid();
    }

    /**
     * 生成请求 ID。
     *
     * @return 请求 ID
     */
    public static String randomRequestId() {
        return "REQ" + LocalDateTime.now().format(BIZ_TIME_FORMATTER) + randomNumbers(6);
    }

    /**
     * 生成带业务前缀的随机 ID。
     *
     * @param prefix 业务前缀，不能为空
     * @return 业务 ID
     */
    public static String randomBizId(String prefix) {
        return randomBizId(prefix, 8);
    }

    /**
     * 生成带业务前缀和指定随机长度的随机 ID。
     *
     * @param prefix 业务前缀，不能为空
     * @param length 随机片段长度，不能小于 0
     * @return 业务 ID
     */
    public static String randomBizId(String prefix, int length) {
        requireNonBlank(prefix, "prefix");
        requireNonNegative(length, "length");
        return prefix + LocalDateTime.now().format(BIZ_TIME_FORMATTER) + randomNumbers(length);
    }

    /**
     * 生成类似雪花 ID 的长整数，不保证分布式全局唯一。
     *
     * @return 类雪花 ID
     */
    public static long randomSnowflakeLikeId() {
        long timestamp = System.currentTimeMillis() & 0x1FFFFFFFFFFL;
        long randomPart = randomLong(0, 0x3FFFFFL);
        return (timestamp << 22) | randomPart;
    }

    /**
     * 从集合中随机获取一个元素。
     *
     * @param collection 集合，不能为空
     * @param <T> 元素类型
     * @return 随机元素
     */
    public static <T> T randomElement(Collection<T> collection) {
        requireNotEmpty(collection, "collection");
        int index = randomIndex(collection.size());
        if (collection instanceof List<T> list) {
            return list.get(index);
        }
        int current = 0;
        for (T item : collection) {
            if (current++ == index) {
                return item;
            }
        }
        throw new IllegalStateException("随机元素获取失败");
    }

    /**
     * 从数组中随机获取一个元素。
     *
     * @param array 数组，不能为空
     * @param <T> 元素类型
     * @return 随机元素
     */
    public static <T> T randomElement(T[] array) {
        Objects.requireNonNull(array, "array 不能为 null");
        if (array.length == 0) {
            throw new IllegalArgumentException("array 不能为空");
        }
        return array[randomIndex(array.length)];
    }

    /**
     * 从集合中随机获取多个不重复元素。
     *
     * @param collection 集合，不能为空
     * @param count 获取数量，不能小于 0
     * @param <T> 元素类型
     * @return 随机元素列表
     */
    public static <T> List<T> randomElements(Collection<T> collection, int count) {
        return randomElements(collection, count, false);
    }

    /**
     * 从集合中随机获取多个元素。
     *
     * @param collection 集合，不能为空
     * @param count 获取数量，不能小于 0
     * @param repeatable 是否允许重复抽取
     * @param <T> 元素类型
     * @return 随机元素列表
     */
    public static <T> List<T> randomElements(Collection<T> collection, int count, boolean repeatable) {
        Objects.requireNonNull(collection, "collection 不能为 null");
        requireNonNegative(count, "count");
        if (count == 0) {
            return new ArrayList<>();
        }
        requireNotEmpty(collection, "collection");
        if (!repeatable && count > collection.size()) {
            throw new IllegalArgumentException("count 不能大于集合大小");
        }
        List<T> source = new ArrayList<>(collection);
        if (repeatable) {
            List<T> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(source.get(randomIndex(source.size())));
            }
            return result;
        }
        shuffleInPlace(source);
        return new ArrayList<>(source.subList(0, count));
    }

    /**
     * 根据大小随机生成索引。
     *
     * @param size 大小，必须大于 0
     * @return 0 到 size 之间的随机索引，不包含 size
     */
    public static int randomIndex(int size) {
        requirePositive(size, "size");
        return randomInt(size);
    }

    /**
     * 返回打乱后的新列表，不修改原列表。
     *
     * @param list 原列表，不能为 null
     * @param <T> 元素类型
     * @return 打乱后的新列表
     */
    public static <T> List<T> shuffle(List<T> list) {
        Objects.requireNonNull(list, "list 不能为 null");
        List<T> result = new ArrayList<>(list);
        shuffleInPlace(result);
        return result;
    }

    /**
     * 原地打乱列表。
     *
     * @param list 待打乱列表，不能为 null
     * @param <T> 元素类型
     */
    public static <T> void shuffleInPlace(List<T> list) {
        Objects.requireNonNull(list, "list 不能为 null");
        shuffleInPlace(list, currentGenerator());
    }

    /**
     * 从列表中随机获取指定数量的子列表。
     *
     * @param list 原列表，不能为 null
     * @param count 获取数量，不能小于 0
     * @param <T> 元素类型
     * @return 随机子列表
     */
    public static <T> List<T> randomSubList(List<T> list, int count) {
        return randomElements(list, count, false);
    }

    /**
     * 从 Map 中随机获取 key。
     *
     * @param map Map，不能为空
     * @param <K> key 类型
     * @param <V> value 类型
     * @return 随机 key
     */
    public static <K, V> K randomMapKey(Map<K, V> map) {
        requireNotEmpty(map, "map");
        return randomElement(map.keySet());
    }

    /**
     * 从 Map 中随机获取 value。
     *
     * @param map Map，不能为空
     * @param <K> key 类型
     * @param <V> value 类型
     * @return 随机 value
     */
    public static <K, V> V randomMapValue(Map<K, V> map) {
        requireNotEmpty(map, "map");
        return randomElement(map.values());
    }

    /**
     * 从 Map 中随机获取 entry。
     *
     * @param map Map，不能为空
     * @param <K> key 类型
     * @param <V> value 类型
     * @return 随机 entry
     */
    public static <K, V> Map.Entry<K, V> randomMapEntry(Map<K, V> map) {
        requireNotEmpty(map, "map");
        return randomElement(map.entrySet());
    }

    /**
     * 从枚举类型中随机获取一个枚举值。
     *
     * @param enumClass 枚举类型，不能为空
     * @param <E> 枚举类型
     * @return 随机枚举值
     */
    public static <E extends Enum<E>> E randomEnum(Class<E> enumClass) {
        Objects.requireNonNull(enumClass, "enumClass 不能为 null");
        E[] values = enumClass.getEnumConstants();
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("enumClass 必须是非空枚举类型");
        }
        return randomElement(values);
    }

    /**
     * 从枚举类型中排除指定枚举后随机获取一个枚举值。
     *
     * @param enumClass 枚举类型，不能为空
     * @param excludes 需要排除的枚举值
     * @param <E> 枚举类型
     * @return 随机枚举值
     */
    @SafeVarargs
    public static <E extends Enum<E>> E randomEnum(Class<E> enumClass, E... excludes) {
        Objects.requireNonNull(enumClass, "enumClass 不能为 null");
        E[] values = enumClass.getEnumConstants();
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("enumClass 必须是非空枚举类型");
        }
        Set<E> excludeSet = excludes == null ? Set.of() : new HashSet<>(Arrays.asList(excludes));
        List<E> candidates = Arrays.stream(values).filter(value -> !excludeSet.contains(value)).toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("排除后没有可用枚举值");
        }
        return randomElement(candidates);
    }

    /**
     * 从枚举集合中随机获取一个枚举值。
     *
     * @param enums 枚举集合，不能为空
     * @param <E> 枚举类型
     * @return 随机枚举值
     */
    public static <E extends Enum<E>> E randomEnum(Collection<E> enums) {
        return randomElement(enums);
    }

    /**
     * 从枚举类型中随机获取枚举名称。
     *
     * @param enumClass 枚举类型，不能为空
     * @param <E> 枚举类型
     * @return 随机枚举名称
     */
    public static <E extends Enum<E>> String randomEnumName(Class<E> enumClass) {
        return randomEnum(enumClass).name();
    }

    /**
     * 从枚举类型中随机获取枚举序号。
     *
     * @param enumClass 枚举类型，不能为空
     * @param <E> 枚举类型
     * @return 随机枚举序号
     */
    public static <E extends Enum<E>> int randomEnumOrdinal(Class<E> enumClass) {
        return randomEnum(enumClass).ordinal();
    }

    /**
     * 随机生成指定范围内的日期。
     *
     * @param start 开始日期，包含
     * @param end 结束日期，包含
     * @return 随机日期
     */
    public static LocalDate randomLocalDate(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start 不能为 null");
        Objects.requireNonNull(end, "end 不能为 null");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start 不能晚于 end");
        }
        long startDay = start.toEpochDay();
        long endDay = end.toEpochDay();
        return LocalDate.ofEpochDay(randomLong(startDay, endDay));
    }

    /**
     * 随机生成一天内的时间。
     *
     * @return 随机时间
     */
    public static LocalTime randomLocalTime() {
        return LocalTime.ofNanoOfDay(randomLong(0, LocalTime.MAX.toNanoOfDay()));
    }

    /**
     * 随机生成指定范围内的时间。
     *
     * @param start 开始时间，包含
     * @param end 结束时间，包含
     * @return 随机时间
     */
    public static LocalTime randomLocalTime(LocalTime start, LocalTime end) {
        Objects.requireNonNull(start, "start 不能为 null");
        Objects.requireNonNull(end, "end 不能为 null");
        long startNano = start.toNanoOfDay();
        long endNano = end.toNanoOfDay();
        if (startNano > endNano) {
            throw new IllegalArgumentException("start 不能晚于 end");
        }
        return LocalTime.ofNanoOfDay(randomLong(startNano, endNano));
    }

    /**
     * 随机生成指定范围内的日期时间。
     *
     * @param start 开始日期时间，包含
     * @param end 结束日期时间，包含
     * @return 随机日期时间
     */
    public static LocalDateTime randomLocalDateTime(LocalDateTime start, LocalDateTime end) {
        Objects.requireNonNull(start, "start 不能为 null");
        Objects.requireNonNull(end, "end 不能为 null");
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instant = randomInstant(start.atZone(zoneId).toInstant(), end.atZone(zoneId).toInstant());
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    /**
     * 随机生成指定范围内的 Instant。
     *
     * @param start 开始时间，包含
     * @param end 结束时间，包含
     * @return 随机 Instant
     */
    public static Instant randomInstant(Instant start, Instant end) {
        Objects.requireNonNull(start, "start 不能为 null");
        Objects.requireNonNull(end, "end 不能为 null");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start 不能晚于 end");
        }
        long startMillis = start.toEpochMilli();
        long endMillis = end.toEpochMilli();
        return Instant.ofEpochMilli(randomLong(startMillis, endMillis));
    }

    /**
     * 随机生成指定范围内的旧版 Date。
     *
     * @param start 开始日期，包含
     * @param end 结束日期，包含
     * @return 随机 Date
     */
    public static Date randomDate(Date start, Date end) {
        Objects.requireNonNull(start, "start 不能为 null");
        Objects.requireNonNull(end, "end 不能为 null");
        return Date.from(randomInstant(start.toInstant(), end.toInstant()));
    }

    /**
     * 随机生成最近指定天数内的日期时间。
     *
     * @param days 最近天数，不能小于 0
     * @return 过去日期时间
     */
    public static LocalDateTime randomPastDate(int days) {
        requireNonNegative(days, "days");
        LocalDateTime now = LocalDateTime.now();
        if (days == 0) {
            return now;
        }
        return randomLocalDateTime(now.minusDays(days), now);
    }

    /**
     * 随机生成未来指定天数内的日期时间。
     *
     * @param days 未来天数，不能小于 0
     * @return 未来日期时间
     */
    public static LocalDateTime randomFutureDate(int days) {
        requireNonNegative(days, "days");
        LocalDateTime now = LocalDateTime.now();
        if (days == 0) {
            return now;
        }
        return randomLocalDateTime(now, now.plusDays(days));
    }

    /**
     * 随机生成指定年龄范围内的生日。
     *
     * @param minAge 最小年龄，不能小于 0
     * @param maxAge 最大年龄，不能小于最小年龄
     * @return 随机生日
     */
    public static LocalDate randomBirthday(int minAge, int maxAge) {
        requireNonNegative(minAge, "minAge");
        requireNonNegative(maxAge, "maxAge");
        if (minAge > maxAge) {
            throw new IllegalArgumentException("minAge 不能大于 maxAge");
        }
        LocalDate today = LocalDate.now();
        return randomLocalDate(today.minusYears(maxAge), today.minusYears(minAge));
    }

    /**
     * 按概率判断是否命中。
     *
     * @param probability 命中概率，范围为 0.0 到 1.0
     * @return 是否命中
     */
    public static boolean hit(double probability) {
        return randomBoolean(probability);
    }

    /**
     * 按百分比概率判断是否命中。
     *
     * @param percent 命中百分比，范围为 0 到 100
     * @return 是否命中
     */
    public static boolean hitPercent(int percent) {
        return randomChance(percent);
    }

    /**
     * 按整数权重从 Map 中随机获取 key。
     *
     * @param weightMap 权重 Map，权重不能为负，总权重必须大于 0
     * @param <T> key 类型
     * @return 随机 key
     */
    public static <T> T randomByWeight(Map<T, Integer> weightMap) {
        requireNotEmpty(weightMap, "weightMap");
        long total = 0;
        for (Map.Entry<T, Integer> entry : weightMap.entrySet()) {
            Integer weight = entry.getValue();
            if (weight == null || weight < 0) {
                throw new IllegalArgumentException("权重不能为 null 或负数");
            }
            total += weight;
        }
        if (total <= 0) {
            throw new IllegalArgumentException("总权重必须大于 0");
        }
        long point = randomLong(1, total);
        long cursor = 0;
        for (Map.Entry<T, Integer> entry : weightMap.entrySet()) {
            cursor += entry.getValue();
            if (point <= cursor) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("权重随机失败");
    }

    /**
     * 按对象权重从集合中随机获取一个元素。
     *
     * @param items 元素集合，不能为空
     * @param weightGetter 权重获取函数，不能为 null
     * @param <T> 元素类型
     * @return 随机元素
     */
    public static <T> T randomByWeight(Collection<T> items, Function<T, Integer> weightGetter) {
        requireNotEmpty(items, "items");
        Objects.requireNonNull(weightGetter, "weightGetter 不能为 null");
        long total = 0;
        List<WeightItem<T>> weightItems = new ArrayList<>();
        for (T item : items) {
            Integer weight = weightGetter.apply(item);
            if (weight == null || weight < 0) {
                throw new IllegalArgumentException("权重不能为 null 或负数");
            }
            total += weight;
            weightItems.add(new WeightItem<>(item, weight));
        }
        if (total <= 0) {
            throw new IllegalArgumentException("总权重必须大于 0");
        }
        long point = randomLong(1, total);
        long cursor = 0;
        for (WeightItem<T> item : weightItems) {
            cursor += item.weight();
            if (point <= cursor) {
                return item.value();
            }
        }
        throw new IllegalStateException("权重随机失败");
    }

    /**
     * 按小数概率权重从 Map 中随机获取 key。
     *
     * @param rateMap 概率 Map，概率不能为负，总概率必须大于 0
     * @param <T> key 类型
     * @return 随机 key
     */
    public static <T> T randomByRate(Map<T, Double> rateMap) {
        requireNotEmpty(rateMap, "rateMap");
        double total = 0.0;
        for (Map.Entry<T, Double> entry : rateMap.entrySet()) {
            Double rate = entry.getValue();
            if (rate == null || !Double.isFinite(rate) || rate < 0) {
                throw new IllegalArgumentException("概率不能为 null、非有限值或负数");
            }
            total += rate;
        }
        if (total <= 0.0) {
            throw new IllegalArgumentException("总概率必须大于 0");
        }
        double point = currentGenerator().nextDouble(total);
        double cursor = 0.0;
        for (Map.Entry<T, Double> entry : rateMap.entrySet()) {
            cursor += entry.getValue();
            if (point < cursor) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("概率随机失败");
    }

    /**
     * 按权重数组随机返回索引。
     *
     * @param weights 权重数组，不能为空，权重不能为负，总权重必须大于 0
     * @return 随机索引
     */
    public static int randomWeightedIndex(int[] weights) {
        Objects.requireNonNull(weights, "weights 不能为 null");
        if (weights.length == 0) {
            throw new IllegalArgumentException("weights 不能为空");
        }
        long total = 0;
        for (int weight : weights) {
            if (weight < 0) {
                throw new IllegalArgumentException("权重不能为负数");
            }
            total += weight;
        }
        if (total <= 0) {
            throw new IllegalArgumentException("总权重必须大于 0");
        }
        long point = randomLong(1, total);
        long cursor = 0;
        for (int i = 0; i < weights.length; i++) {
            cursor += weights[i];
            if (point <= cursor) {
                return i;
            }
        }
        throw new IllegalStateException("权重索引随机失败");
    }

    /**
     * 按权重随机返回桶名称。
     *
     * @param buckets 桶权重 Map，不能为空
     * @return 随机桶名称
     */
    public static String randomBucket(Map<String, Integer> buckets) {
        return randomByWeight(buckets);
    }

    /**
     * 按权重随机返回流量分组名称。
     *
     * @param groupWeights 分组权重 Map，不能为空
     * @return 随机流量分组名称
     */
    public static String randomTrafficGroup(Map<String, Integer> groupWeights) {
        return randomByWeight(groupWeights);
    }

    /**
     * 使用安全随机生成指定上界内的 int 值。
     *
     * @param bound 上界，不包含该值，必须大于 0
     * @return 安全随机整数
     */
    public static int secureInt(int bound) {
        requirePositive(bound, "bound");
        return SECURE_RANDOM.nextInt(bound);
    }

    /**
     * 使用安全随机生成 long 值。
     *
     * @return 安全随机长整数
     */
    public static long secureLong() {
        return SECURE_RANDOM.nextLong();
    }

    /**
     * 使用安全随机生成默认字符集字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 安全随机字符串
     */
    public static String secureString(int length) {
        return randomString(SECURE_RANDOM, READABLE_CODE_CHARS, length);
    }

    /**
     * 使用安全随机生成字母数字字符串。
     *
     * @param length 字符串长度，不能小于 0
     * @return 安全随机字母数字字符串
     */
    public static String secureAlphaNumeric(int length) {
        return randomString(SECURE_RANDOM, ALPHA_NUMERIC, length);
    }

    /**
     * 使用安全随机生成 Token。
     *
     * @param length Token 长度，不能小于 0
     * @return 安全随机 Token
     */
    public static String secureToken(int length) {
        return secureAlphaNumeric(length);
    }

    /**
     * 使用安全随机生成十六进制字符串。
     *
     * @param byteLength 原始字节长度，不能小于 0
     * @return 十六进制字符串
     */
    public static String secureHex(int byteLength) {
        requireNonNegative(byteLength, "byteLength");
        byte[] bytes = secureBytes(byteLength);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * 使用安全随机生成 URL 安全的 Base64 字符串。
     *
     * @param byteLength 原始字节长度，不能小于 0
     * @return Base64 字符串
     */
    public static String secureBase64(int byteLength) {
        requireNonNegative(byteLength, "byteLength");
        byte[] bytes = secureBytes(byteLength);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 使用安全随机生成强度较高的密码。
     *
     * @param length 密码长度，至少为 4
     * @return 安全随机密码
     */
    public static String securePassword(int length) {
        return randomStrongPassword(length);
    }

    /**
     * 使用安全随机生成盐值。
     *
     * @param byteLength 原始字节长度，不能小于 0
     * @return 十六进制盐值
     */
    public static String secureSalt(int byteLength) {
        return secureHex(byteLength);
    }

    /**
     * 生成默认规则密码。
     *
     * @param length 密码长度，必须大于 0
     * @return 随机密码
     */
    public static String randomPassword(int length) {
        return randomPassword(length, true);
    }

    /**
     * 生成可控制是否包含特殊字符的密码。
     *
     * @param length 密码长度，必须大于 0
     * @param specialChar 是否包含特殊字符
     * @return 随机密码
     */
    public static String randomPassword(int length, boolean specialChar) {
        return randomPasswordWithRule(new PasswordRule(length, true, true, true, specialChar, "", specialChar ? 4 : 3));
    }

    /**
     * 生成强密码，至少包含大小写字母、数字和特殊字符。
     *
     * @param length 密码长度，至少为 4
     * @return 强密码
     */
    public static String randomStrongPassword(int length) {
        return randomPasswordWithRule(new PasswordRule(length, true, true, true, true, "", 4));
    }

    /**
     * 按密码规则生成随机密码。
     *
     * @param rule 密码规则，不能为 null
     * @return 随机密码
     */
    public static String randomPasswordWithRule(PasswordRule rule) {
        Objects.requireNonNull(rule, "rule 不能为 null");
        validatePasswordRule(rule);
        List<String> pools = new ArrayList<>();
        if (rule.isIncludeLower()) {
            pools.add(removeChars(LOWER_LETTERS, rule.getExcludeChars()));
        }
        if (rule.isIncludeUpper()) {
            pools.add(removeChars(UPPER_LETTERS, rule.getExcludeChars()));
        }
        if (rule.isIncludeNumber()) {
            pools.add(removeChars(NUMBERS, rule.getExcludeChars()));
        }
        if (rule.isIncludeSpecial()) {
            pools.add(removeChars(SPECIAL_CHARS, rule.getExcludeChars()));
        }
        pools.removeIf(String::isEmpty);
        if (pools.isEmpty()) {
            throw new IllegalArgumentException("密码候选字符集不能为空");
        }
        if (rule.getMinTypes() > pools.size()) {
            throw new IllegalArgumentException("minTypes 不能大于可用字符类型数量");
        }
        if (rule.getLength() < rule.getMinTypes()) {
            throw new IllegalArgumentException("密码长度不能小于 minTypes");
        }
        shuffleInPlace(pools, SECURE_RANDOM);
        StringBuilder poolBuilder = new StringBuilder();
        pools.forEach(poolBuilder::append);
        String allChars = poolBuilder.toString();
        List<Character> chars = new ArrayList<>(rule.getLength());
        for (int i = 0; i < rule.getMinTypes(); i++) {
            chars.add(randomChar(pools.get(i), SECURE_RANDOM));
        }
        while (chars.size() < rule.getLength()) {
            chars.add(randomChar(allChars, SECURE_RANDOM));
        }
        shuffleInPlace(chars, SECURE_RANDOM);
        StringBuilder result = new StringBuilder(chars.size());
        chars.forEach(result::append);
        return result.toString();
    }

    /**
     * 随机生成特殊字符。
     *
     * @return 特殊字符
     */
    public static char randomSpecialChar() {
        return randomChar(SPECIAL_CHARS, currentGenerator());
    }

    /**
     * 生成易读密码，排除容易混淆的字符。
     *
     * @param length 密码长度，必须大于 0
     * @return 易读密码
     */
    public static String randomReadablePassword(int length) {
        requirePositive(length, "length");
        return randomString(SECURE_RANDOM, READABLE_CODE_CHARS, length);
    }

    /**
     * 生成十六进制颜色。
     *
     * @return 十六进制颜色字符串
     */
    public static String randomHexColor() {
        return "#" + randomString("0123456789ABCDEF", 6);
    }

    /**
     * 生成 RGB 颜色字符串。
     *
     * @return RGB 颜色字符串
     */
    public static String randomRgbColor() {
        return "rgb(" + randomInt(0, 255) + "," + randomInt(0, 255) + "," + randomInt(0, 255) + ")";
    }

    /**
     * 生成 RGBA 颜色字符串。
     *
     * @param alpha 透明度，范围为 0.0 到 1.0
     * @return RGBA 颜色字符串
     */
    public static String randomRgbaColor(double alpha) {
        requireProbability(alpha);
        return "rgba(" + randomInt(0, 255) + "," + randomInt(0, 255) + "," + randomInt(0, 255) + "," + alpha + ")";
    }

    /**
     * 从预设颜色名称中随机获取一个颜色名。
     *
     * @return 颜色名称
     */
    public static String randomColorName() {
        return randomElement(COLOR_NAMES);
    }

    /**
     * 生成随机浅色十六进制颜色。
     *
     * @return 浅色十六进制颜色
     */
    public static String randomLightColor() {
        return toHexColor(randomInt(160, 255), randomInt(160, 255), randomInt(160, 255));
    }

    /**
     * 生成随机深色十六进制颜色。
     *
     * @return 深色十六进制颜色
     */
    public static String randomDarkColor() {
        return toHexColor(randomInt(0, 95), randomInt(0, 95), randomInt(0, 95));
    }

    /**
     * 生成随机文件名。
     *
     * @return 随机文件名
     */
    public static String randomFileName() {
        return randomSimpleUuid();
    }

    /**
     * 生成带扩展名的随机文件名。
     *
     * @param extension 文件扩展名，可以带点
     * @return 随机文件名
     */
    public static String randomFileName(String extension) {
        return randomFileName("", extension);
    }

    /**
     * 生成带前缀和扩展名的随机文件名。
     *
     * @param prefix 文件名前缀，不能为 null
     * @param extension 文件扩展名，可以带点或为空
     * @return 随机文件名
     */
    public static String randomFileName(String prefix, String extension) {
        Objects.requireNonNull(prefix, "prefix 不能为 null");
        String ext = normalizeExtension(extension);
        return prefix + randomSimpleUuid() + ext;
    }

    /**
     * 生成随机路径片段。
     *
     * @param length 片段长度，不能小于 0
     * @return 路径片段
     */
    public static String randomPathSegment(int length) {
        return randomString(READABLE_CODE_CHARS, length);
    }

    /**
     * 生成对象存储 key。
     *
     * @param prefix key 前缀，可以为空
     * @param extension 文件扩展名，可以带点或为空
     * @return 对象存储 key
     */
    public static String randomObjectKey(String prefix, String extension) {
        String safePrefix = prefix == null || prefix.isBlank() ? "" : prefix.strip();
        if (!safePrefix.isEmpty() && !safePrefix.endsWith("/")) {
            safePrefix += "/";
        }
        return safePrefix + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "/" + randomFileName(extension);
    }

    /**
     * 生成临时名称。
     *
     * @return 临时名称
     */
    public static String randomTempName() {
        return "tmp-" + randomSimpleUuid();
    }

    /**
     * 生成随机手机号测试数据。
     *
     * @return 随机手机号
     */
    public static String randomMobile() {
        return randomElement(MOBILE_PREFIXES) + randomNumbers(8);
    }

    /**
     * 生成随机邮箱测试数据。
     *
     * @return 随机邮箱
     */
    public static String randomEmail() {
        return randomEmail("example.com");
    }

    /**
     * 生成指定域名的随机邮箱测试数据。
     *
     * @param domain 邮箱域名，不能为空
     * @return 随机邮箱
     */
    public static String randomEmail(String domain) {
        requireNonBlank(domain, "domain");
        return randomUsername() + "@" + domain.strip();
    }

    /**
     * 生成随机用户名测试数据。
     *
     * @return 随机用户名
     */
    public static String randomUsername() {
        return "user" + randomLowerLetters(4) + randomNumbers(4);
    }

    /**
     * 生成随机昵称测试数据。
     *
     * @return 随机昵称
     */
    public static String randomNickname() {
        return "用户" + randomChinese(2) + randomNumbers(2);
    }

    /**
     * 生成随机中文姓名测试数据。
     *
     * @return 随机中文姓名
     */
    public static String randomChineseName() {
        return randomElement(SURNAMES) + randomElement(GIVEN_NAMES);
    }

    /**
     * 生成随机英文姓名测试数据。
     *
     * @return 随机英文姓名
     */
    public static String randomEnglishName() {
        return randomElement(ENGLISH_FIRST_NAMES) + " " + randomElement(ENGLISH_LAST_NAMES);
    }

    /**
     * 生成随机性别测试数据。
     *
     * @return 男或女
     */
    public static String randomGender() {
        return randomBoolean() ? "男" : "女";
    }

    /**
     * 生成指定范围内的随机年龄。
     *
     * @param min 最小年龄，包含
     * @param max 最大年龄，包含
     * @return 随机年龄
     */
    public static int randomAge(int min, int max) {
        requireNonNegative(min, "min");
        requireNonNegative(max, "max");
        return randomInt(min, max);
    }

    /**
     * 生成格式类似身份证号的测试数据，不保证真实有效。
     *
     * @return 类身份证号测试数据
     */
    public static String randomIdCardLike() {
        String area = "110101";
        String birthday = randomBirthday(18, 65).format(DateTimeFormatter.BASIC_ISO_DATE);
        String sequence = randomNumbers(3);
        String check = randomString("0123456789X", 1);
        return area + birthday + sequence + check;
    }

    /**
     * 随机获取省份测试数据。
     *
     * @return 省份名称
     */
    public static String randomProvince() {
        return randomElement(PROVINCES);
    }

    /**
     * 随机获取城市测试数据。
     *
     * @return 城市名称
     */
    public static String randomCity() {
        return randomElement(CITIES);
    }

    /**
     * 生成随机地址测试数据。
     *
     * @return 随机地址
     */
    public static String randomAddress() {
        return randomProvince() + randomCity() + "示例路" + randomInt(1, 999) + "号";
    }

    /**
     * 随机生成经度。
     *
     * @return 经度，范围为 -180 到 180
     */
    public static double randomLongitude() {
        return randomDouble(-180.0, 180.0);
    }

    /**
     * 随机生成纬度。
     *
     * @return 纬度，范围为 -90 到 90
     */
    public static double randomLatitude() {
        return randomDouble(-90.0, 90.0);
    }

    /**
     * 随机生成经纬度坐标。
     *
     * @return 经纬度坐标
     */
    public static Coordinate randomCoordinate() {
        return new Coordinate(randomLongitude(), randomLatitude());
    }

    /**
     * 在指定经纬度范围内随机生成坐标。
     *
     * @param minLng 最小经度，包含
     * @param maxLng 最大经度，不包含
     * @param minLat 最小纬度，包含
     * @param maxLat 最大纬度，不包含
     * @return 经纬度坐标
     */
    public static Coordinate randomCoordinate(double minLng, double maxLng, double minLat, double maxLat) {
        requireLongitude(minLng, "minLng");
        requireLongitude(maxLng, "maxLng");
        requireLatitude(minLat, "minLat");
        requireLatitude(maxLat, "maxLat");
        return new Coordinate(randomDouble(minLng, maxLng), randomDouble(minLat, maxLat));
    }

    /**
     * 生成默认订单号。
     *
     * @return 订单号
     */
    public static String randomOrderNo() {
        return randomOrderNo("ORD");
    }

    /**
     * 生成带前缀的订单号。
     *
     * @param prefix 订单号前缀，不能为空
     * @return 订单号
     */
    public static String randomOrderNo(String prefix) {
        return randomNo(prefix, 8);
    }

    /**
     * 生成通用流水号。
     *
     * @param prefix 流水号前缀，不能为空
     * @return 流水号
     */
    public static String randomSerialNo(String prefix) {
        return randomNo(prefix, 8);
    }

    /**
     * 生成批次号。
     *
     * @param prefix 批次号前缀，不能为空
     * @return 批次号
     */
    public static String randomBatchNo(String prefix) {
        return randomNo(prefix, 6);
    }

    /**
     * 生成交易号。
     *
     * @return 交易号
     */
    public static String randomTradeNo() {
        return randomNo("TRD", 10);
    }

    /**
     * 生成任务号。
     *
     * @return 任务号
     */
    public static String randomTaskNo() {
        return randomNo("TASK", 8);
    }

    /**
     * 生成带前缀和指定随机片段长度的编号。
     *
     * @param prefix 编号前缀，不能为空
     * @param length 随机数字片段长度，不能小于 0
     * @return 随机编号
     */
    public static String randomNo(String prefix, int length) {
        requireNonBlank(prefix, "prefix");
        requireNonNegative(length, "length");
        return prefix.strip() + LocalDateTime.now().format(BIZ_TIME_FORMATTER) + randomNumbers(length);
    }

    /**
     * 生成随机睡眠毫秒数。
     *
     * @param min 最小毫秒数，包含
     * @param max 最大毫秒数，包含
     * @return 随机毫秒数
     */
    public static long randomSleepMillis(long min, long max) {
        requireNonNegative(min, "min");
        requireNonNegative(max, "max");
        return randomLong(min, max);
    }

    /**
     * 生成随机延迟时长。
     *
     * @param min 最小时长，包含
     * @param max 最大时长，包含
     * @return 随机时长
     */
    public static Duration randomDelay(Duration min, Duration max) {
        Objects.requireNonNull(min, "min 不能为 null");
        Objects.requireNonNull(max, "max 不能为 null");
        if (min.isNegative() || max.isNegative()) {
            throw new IllegalArgumentException("Duration 不能为负数");
        }
        long millis = randomLong(min.toMillis(), max.toMillis());
        return Duration.ofMillis(millis);
    }

    /**
     * 基于基础毫秒数生成随机抖动值。
     *
     * @param baseMillis 基础毫秒数，不能小于 0
     * @param jitterRate 抖动比例，不能小于 0
     * @return 带抖动的毫秒数
     */
    public static long randomJitter(long baseMillis, double jitterRate) {
        requireNonNegative(baseMillis, "baseMillis");
        requireFinite(jitterRate, "jitterRate");
        if (jitterRate < 0) {
            throw new IllegalArgumentException("jitterRate 不能小于 0");
        }
        long jitter = Math.round(baseMillis * jitterRate);
        long min = Math.max(0, baseMillis - jitter);
        long max = safeAdd(baseMillis, jitter);
        return randomLong(min, max);
    }

    /**
     * 根据重试次数生成退避等待时间。
     *
     * @param retryTimes 重试次数，不能小于 0
     * @param baseMillis 基础毫秒数，必须大于 0
     * @param maxMillis 最大毫秒数，不能小于基础毫秒数
     * @return 退避等待毫秒数
     */
    public static long randomBackoff(int retryTimes, long baseMillis, long maxMillis) {
        requireNonNegative(retryTimes, "retryTimes");
        requirePositive(baseMillis, "baseMillis");
        if (maxMillis < baseMillis) {
            throw new IllegalArgumentException("maxMillis 不能小于 baseMillis");
        }
        int shift = Math.min(retryTimes, 30);
        long calculated = baseMillis > Long.MAX_VALUE / (1L << shift) ? Long.MAX_VALUE : baseMillis * (1L << shift);
        long upper = Math.min(calculated, maxMillis);
        return randomLong(baseMillis, upper);
    }

    /**
     * 随机生成 cron 秒字段。
     *
     * @return 0 到 59 的秒值
     */
    public static int randomCronSecond() {
        return randomInt(0, 59);
    }

    /**
     * 随机生成 cron 分钟字段。
     *
     * @return 0 到 59 的分钟值
     */
    public static int randomCronMinute() {
        return randomInt(0, 59);
    }

    /**
     * 基于种子创建可复现随机生成器。
     *
     * @param seed 随机种子
     * @return 可复现随机生成器
     */
    public static RandomGenerator withSeed(long seed) {
        return new Random(seed);
    }

    /**
     * 使用指定种子随机生成指定范围内的 int 值。
     *
     * @param seed 随机种子
     * @param min 最小值，包含
     * @param max 最大值，包含
     * @return 可复现随机整数
     */
    public static int randomInt(long seed, int min, int max) {
        return Math.toIntExact(nextLongInclusive(withSeed(seed), min, max));
    }

    /**
     * 使用指定种子生成随机字符串。
     *
     * @param seed 随机种子
     * @param length 字符串长度，不能小于 0
     * @return 可复现随机字符串
     */
    public static String randomString(long seed, int length) {
        return randomString(withSeed(seed), ALPHA_NUMERIC, length);
    }

    /**
     * 使用指定种子从集合中随机获取多个不重复元素。
     *
     * @param seed 随机种子
     * @param collection 集合，不能为空
     * @param count 获取数量，不能小于 0
     * @param <T> 元素类型
     * @return 可复现随机元素列表
     */
    public static <T> List<T> randomElements(long seed, Collection<T> collection, int count) {
        Objects.requireNonNull(collection, "collection 不能为 null");
        requireNonNegative(count, "count");
        if (count == 0) {
            return new ArrayList<>();
        }
        requireNotEmpty(collection, "collection");
        if (count > collection.size()) {
            throw new IllegalArgumentException("count 不能大于集合大小");
        }
        List<T> result = new ArrayList<>(collection);
        shuffleInPlace(result, withSeed(seed));
        return new ArrayList<>(result.subList(0, count));
    }

    /**
     * 使用指定种子打乱列表并返回新列表。
     *
     * @param seed 随机种子
     * @param list 原列表，不能为 null
     * @param <T> 元素类型
     * @return 可复现打乱列表
     */
    public static <T> List<T> shuffle(long seed, List<T> list) {
        Objects.requireNonNull(list, "list 不能为 null");
        List<T> result = new ArrayList<>(list);
        shuffleInPlace(result, withSeed(seed));
        return result;
    }

    /**
     * 设置全局随机生成器，传入 null 表示恢复默认线程本地随机生成器。
     *
     * @param generator 随机生成器，可为 null
     */
    public static void setRandomGenerator(RandomGenerator generator) {
        CUSTOM_GENERATOR.set(generator);
    }

    /**
     * 获取当前随机生成器。
     *
     * @return 当前随机生成器
     */
    public static RandomGenerator getRandomGenerator() {
        return currentGenerator();
    }

    /**
     * 创建默认随机生成器。
     *
     * @return 随机生成器
     */
    public static RandomGenerator newRandomGenerator() {
        return RandomGenerator.of("L64X128MixRandom");
    }

    /**
     * 创建安全随机生成器。
     *
     * @return 安全随机生成器
     */
    public static SecureRandom newSecureRandom() {
        return new SecureRandom();
    }

    /**
     * 使用指定随机生成器执行随机逻辑。
     *
     * @param generator 随机生成器，不能为 null
     * @param supplier 随机逻辑，不能为 null
     * @param <T> 返回类型
     * @return 随机逻辑返回值
     */
    public static <T> T randomWith(RandomGenerator generator, Function<RandomGenerator, T> supplier) {
        Objects.requireNonNull(generator, "generator 不能为 null");
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        return supplier.apply(generator);
    }

    /**
     * 密码生成规则。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class PasswordRule {
        private final int length;
        private final boolean includeLower;
        private final boolean includeUpper;
        private final boolean includeNumber;
        private final boolean includeSpecial;
        private final String excludeChars;
        private final int minTypes;

        /**
         * 创建密码生成规则。
         *
         * @param length 密码长度，必须大于 0
         * @param includeLower 是否包含小写字母
         * @param includeUpper 是否包含大写字母
         * @param includeNumber 是否包含数字
         * @param includeSpecial 是否包含特殊字符
         * @param excludeChars 需要排除的字符，可以为 null
         * @param minTypes 最少字符类型数，不能小于 1
         */
        public PasswordRule(int length, boolean includeLower, boolean includeUpper, boolean includeNumber, boolean includeSpecial, String excludeChars, int minTypes) {
            this.length = length;
            this.includeLower = includeLower;
            this.includeUpper = includeUpper;
            this.includeNumber = includeNumber;
            this.includeSpecial = includeSpecial;
            this.excludeChars = excludeChars == null ? "" : excludeChars;
            this.minTypes = minTypes;
        }

        /**
         * 创建默认密码规则。
         *
         * @param length 密码长度，必须大于 0
         * @return 默认密码规则
         */
        public static PasswordRule defaultRule(int length) {
            return new PasswordRule(length, true, true, true, true, "", 4);
        }

        /**
         * 获取密码长度。
         *
         * @return 密码长度
         */
        public int getLength() {
            return length;
        }

        /**
         * 是否包含小写字母。
         *
         * @return 是否包含小写字母
         */
        public boolean isIncludeLower() {
            return includeLower;
        }

        /**
         * 是否包含大写字母。
         *
         * @return 是否包含大写字母
         */
        public boolean isIncludeUpper() {
            return includeUpper;
        }

        /**
         * 是否包含数字。
         *
         * @return 是否包含数字
         */
        public boolean isIncludeNumber() {
            return includeNumber;
        }

        /**
         * 是否包含特殊字符。
         *
         * @return 是否包含特殊字符
         */
        public boolean isIncludeSpecial() {
            return includeSpecial;
        }

        /**
         * 获取排除字符。
         *
         * @return 排除字符
         */
        public String getExcludeChars() {
            return excludeChars;
        }

        /**
         * 获取最少字符类型数。
         *
         * @return 最少字符类型数
         */
        public int getMinTypes() {
            return minTypes;
        }
    }

    /**
     * 经纬度坐标。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class Coordinate {
        private final double longitude;
        private final double latitude;

        /**
         * 创建经纬度坐标。
         *
         * @param longitude 经度，范围为 -180 到 180
         * @param latitude 纬度，范围为 -90 到 90
         */
        public Coordinate(double longitude, double latitude) {
            requireLongitude(longitude, "longitude");
            requireLatitude(latitude, "latitude");
            this.longitude = longitude;
            this.latitude = latitude;
        }

        /**
         * 获取经度。
         *
         * @return 经度
         */
        public double getLongitude() {
            return longitude;
        }

        /**
         * 获取纬度。
         *
         * @return 纬度
         */
        public double getLatitude() {
            return latitude;
        }

        /**
         * 返回坐标字符串。
         *
         * @return 坐标字符串
         */
        @Override
        public String toString() {
            return "Coordinate{longitude=" + longitude + ", latitude=" + latitude + '}';
        }
    }

    private record WeightItem<T>(T value, int weight) {
    }

    private static RandomGenerator currentGenerator() {
        RandomGenerator generator = CUSTOM_GENERATOR.get();
        return generator == null ? ThreadLocalRandom.current() : generator;
    }

    private static String randomString(RandomGenerator generator, String chars, int length) {
        Objects.requireNonNull(generator, "generator 不能为 null");
        requireNonBlank(chars, "chars");
        requireNonNegative(length, "length");
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(randomChar(chars, generator));
        }
        return builder.toString();
    }

    private static char randomChar(String chars, RandomGenerator generator) {
        return chars.charAt(generator.nextInt(chars.length()));
    }

    private static byte[] secureBytes(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static long nextLongInclusive(RandomGenerator generator, long min, long max) {
        requireRange(min, max);
        if (min == max) {
            return min;
        }
        if (max < Long.MAX_VALUE) {
            return generator.nextLong(min, max + 1);
        }
        long bound = max - min + 1;
        if (bound > 0) {
            return min + generator.nextLong(bound);
        }
        long value;
        do {
            value = generator.nextLong();
        } while (value < min || value > max);
        return value;
    }

    private static <T> void shuffleInPlace(List<T> list, RandomGenerator generator) {
        for (int i = list.size() - 1; i > 0; i--) {
            int index = generator.nextInt(i + 1);
            Collections.swap(list, i, index);
        }
    }

    private static String removeChars(String source, String excludes) {
        if (excludes == null || excludes.isEmpty()) {
            return source;
        }
        StringBuilder builder = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (excludes.indexOf(ch) < 0) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static void validatePasswordRule(PasswordRule rule) {
        requirePositive(rule.getLength(), "length");
        requirePositive(rule.getMinTypes(), "minTypes");
        if (!rule.isIncludeLower() && !rule.isIncludeUpper() && !rule.isIncludeNumber() && !rule.isIncludeSpecial()) {
            throw new IllegalArgumentException("至少需要启用一种字符类型");
        }
    }

    private static String toHexColor(int red, int green, int blue) {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "";
        }
        String ext = extension.strip();
        return ext.startsWith(".") ? ext : "." + ext;
    }

    private static long safeAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static void requireProbability(double probability) {
        requireFinite(probability, "probability");
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("probability 必须在 0.0 到 1.0 之间");
        }
    }

    private static void requirePercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent 必须在 0 到 100 之间");
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " 必须是有限数字");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " 不能小于 0");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " 不能小于 0");
        }
    }

    private static void requireRange(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("min 不能大于 max");
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private static void requireNotEmpty(Collection<?> collection, String name) {
        Objects.requireNonNull(collection, name + " 不能为 null");
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private static void requireNotEmpty(Map<?, ?> map, String name) {
        Objects.requireNonNull(map, name + " 不能为 null");
        if (map.isEmpty()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private static void requireLongitude(double value, String name) {
        requireFinite(value, name);
        if (value < -180.0 || value > 180.0) {
            throw new IllegalArgumentException(name + " 必须在 -180 到 180 之间");
        }
    }

    private static void requireLatitude(double value, String name) {
        requireFinite(value, name);
        if (value < -90.0 || value > 90.0) {
            throw new IllegalArgumentException(name + " 必须在 -90 到 90 之间");
        }
    }
}
