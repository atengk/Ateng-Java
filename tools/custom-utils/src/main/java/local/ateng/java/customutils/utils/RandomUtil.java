package local.ateng.java.customutils.utils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 随机数工具类
 * 提供常用随机数据生成方法
 *
 * @author Ateng
 * @since 2025-07-30
 */
public final class RandomUtil {

    // 使用线程安全的 SecureRandom 生成器
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 禁止实例化工具类
     */
    private RandomUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 生成指定范围内的随机整数，包含 min 和 max
     *
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 随机整数
     * @throws IllegalArgumentException 如果 min > max 抛出异常
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min不能大于max");
        }
        return SECURE_RANDOM.nextInt((max - min) + 1) + min;
    }

    /**
     * 生成随机长整型数，范围从 0（包含）到 Long.MAX_VALUE（包含）
     *
     * @return 随机长整型数
     */
    public static long randomLong() {
        return Math.abs(SECURE_RANDOM.nextLong());
    }

    /**
     * 生成指定长度的随机数字字符串（只包含数字0-9）
     *
     * @param length 随机数字字符串长度
     * @return 随机数字字符串
     * @throws IllegalArgumentException 如果 length <= 0 抛出异常
     */
    public static String randomNumeric(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成指定长度的随机字母字符串（只包含大小写字母）
     *
     * @param length 随机字符串长度
     * @return 随机字母字符串
     * @throws IllegalArgumentException 如果 length <= 0 抛出异常
     */
    public static String randomAlphabetic(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(letters.length());
            sb.append(letters.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成指定长度的随机字母数字混合字符串（包含大小写字母和数字）
     *
     * @param length 随机字符串长度
     * @return 随机字母数字混合字符串
     * @throws IllegalArgumentException 如果 length <= 0 抛出异常
     */
    public static String randomAlphanumeric(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成随机布尔值
     *
     * @return true 或 false
     */
    public static boolean randomBoolean() {
        return SECURE_RANDOM.nextBoolean();
    }

    /**
     * 生成随机 UUID 字符串（标准格式，包含连字符）
     *
     * @return UUID 字符串
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }


    /**
     * 生成指定范围内的随机 double，min <= 返回值 < max
     *
     * @param min 最小值（包含）
     * @param max 最大值（不包含）
     * @return 随机 double
     * @throws IllegalArgumentException 如果 min >= max 抛出异常
     */
    public static double randomDouble(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("min必须小于max");
        }
        return min + SECURE_RANDOM.nextDouble() * (max - min);
    }

    /**
     * 生成指定范围内的随机 float，min <= 返回值 < max
     *
     * @param min 最小值（包含）
     * @param max 最大值（不包含）
     * @return 随机 float
     * @throws IllegalArgumentException 如果 min >= max 抛出异常
     */
    public static float randomFloat(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("min必须小于max");
        }
        return min + SECURE_RANDOM.nextFloat() * (max - min);
    }

    /**
     * 生成指定长度的随机字节数组
     *
     * @param length 字节数组长度
     * @return 随机字节数组
     * @throws IllegalArgumentException 如果 length <= 0 抛出异常
     */
    public static byte[] randomBytes(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * 从指定数组中随机获取一个元素，数组不能为空且长度必须大于0
     *
     * @param <T>   元素类型
     * @param array 数组
     * @return 随机元素
     * @throws IllegalArgumentException 如果数组为空或 null 抛出异常
     */
    public static <T> T randomElement(T[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("数组不能为空且长度必须大于0");
        }
        int index = SECURE_RANDOM.nextInt(array.length);
        return array[index];
    }

    /**
     * 从指定集合中随机获取一个元素，集合不能为空且不为空
     *
     * @param <T>        元素类型
     * @param collection 集合
     * @return 随机元素
     * @throws IllegalArgumentException 如果集合为空或 null 抛出异常
     */
    public static <T> T randomElement(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException("集合不能为空且不为空");
        }
        int index = SECURE_RANDOM.nextInt(collection.size());
        int i = 0;
        for (T element : collection) {
            if (i == index) {
                return element;
            }
            i++;
        }
        // 理论上不会执行到这里
        throw new IllegalStateException("随机元素选择失败");
    }

    /**
     * 生成指定时间范围内的随机 LocalDateTime，包含 start 和 end
     *
     * @param start 起始时间（包含）
     * @param end   结束时间（包含）
     * @return 随机 LocalDateTime
     * @throws IllegalArgumentException 如果 start 为空或 end 为空，或 start 晚于 end 抛异常
     */
    public static LocalDateTime randomLocalDateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start和end不能为空");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start必须早于或等于end");
        }
        long startEpoch = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endEpoch = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long randomEpoch = startEpoch + (long) (SECURE_RANDOM.nextDouble() * (endEpoch - startEpoch));
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(randomEpoch), ZoneId.systemDefault());
    }

    /**
     * 根据自定义字符集生成指定长度的随机字符串
     *
     * @param length 字符串长度
     * @param chars  用于随机的字符集，不能为空且长度大于0
     * @return 随机字符串
     * @throws IllegalArgumentException 如果参数非法抛异常
     */
    public static String randomString(int length, String chars) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        if (chars == null || chars.isEmpty()) {
            throw new IllegalArgumentException("字符集不能为空");
        }
        StringBuilder sb = new StringBuilder(length);
        int maxIndex = chars.length();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(maxIndex)));
        }
        return sb.toString();
    }


    /**
     * 生成一个符合强密码要求的随机密码，包含大写字母、小写字母、数字、特殊字符
     *
     * @param length 密码长度，建议至少8位
     * @return 随机密码字符串
     * @throws IllegalArgumentException 如果长度小于4抛出异常（保证四类字符至少一位）
     */
    public static String randomStrongPassword(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("密码长度必须至少4");
        }
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()-_=+[]{}|;:,.<>?";

        StringBuilder password = new StringBuilder(length);

        // 保证每类至少一个字符
        password.append(upper.charAt(SECURE_RANDOM.nextInt(upper.length())));
        password.append(lower.charAt(SECURE_RANDOM.nextInt(lower.length())));
        password.append(digits.charAt(SECURE_RANDOM.nextInt(digits.length())));
        password.append(special.charAt(SECURE_RANDOM.nextInt(special.length())));

        String allChars = upper + lower + digits + special;

        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(SECURE_RANDOM.nextInt(allChars.length())));
        }

        // 打乱顺序，防止固定顺序
        return shuffleString(password.toString());
    }

    /**
     * 生成指定长度的随机中文字符串（常用汉字区）
     *
     * @param length 字符串长度
     * @return 随机中文字符串
     * @throws IllegalArgumentException 如果 length <= 0 抛出异常
     */
    public static String randomChinese(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        StringBuilder sb = new StringBuilder(length);
        // 汉字常用范围：0x4E00 - 0x9FA5
        int start = 0x4E00;
        int end = 0x9FA5;
        for (int i = 0; i < length; i++) {
            int codePoint = start + SECURE_RANDOM.nextInt(end - start + 1);
            sb.append((char) codePoint);
        }
        return sb.toString();
    }

    /**
     * 生成随机IPv4地址字符串
     *
     * @return 随机IPv4地址
     */
    public static String randomIPv4() {
        return String.format("%d.%d.%d.%d",
                SECURE_RANDOM.nextInt(256),
                SECURE_RANDOM.nextInt(256),
                SECURE_RANDOM.nextInt(256),
                SECURE_RANDOM.nextInt(256));
    }

    /**
     * 生成随机MAC地址字符串，格式为 XX:XX:XX:XX:XX:XX
     *
     * @return 随机MAC地址字符串
     */
    public static String randomMACAddress() {
        byte[] macAddr = new byte[6];
        SECURE_RANDOM.nextBytes(macAddr);
        // 设置本地管理位，去掉广播和组播标志位
        macAddr[0] = (byte) (macAddr[0] & (byte) 254);

        StringBuilder sb = new StringBuilder(17);
        for (byte b : macAddr) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            // 转为两位16进制字符串
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 从指定枚举类中随机获取一个枚举值
     *
     * @param <T>       枚举类型
     * @param enumClass 枚举类
     * @return 随机枚举值
     * @throws IllegalArgumentException 如果 enumClass 为 null 或没有枚举值
     */
    public static <T extends Enum<?>> T randomEnum(Class<T> enumClass) {
        if (enumClass == null) {
            throw new IllegalArgumentException("枚举类不能为空");
        }
        T[] values = enumClass.getEnumConstants();
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("枚举类无枚举值");
        }
        int index = SECURE_RANDOM.nextInt(values.length);
        return values[index];
    }

    /**
     * 生成不含中划线的UUID字符串
     *
     * @return 无中划线的UUID字符串
     */
    public static String randomUUIDNoDash() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成指定长度随机字节数组并进行Base64编码
     *
     * @param byteLength 生成随机字节数组长度
     * @return Base64编码字符串
     * @throws IllegalArgumentException 如果长度 <= 0 抛异常
     */
    public static String randomBase64(int byteLength) {
        if (byteLength <= 0) {
            throw new IllegalArgumentException("字节长度必须大于0");
        }
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 字符串打乱顺序
     *
     * @param input 输入字符串
     * @return 打乱顺序后的字符串
     */
    private static String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }


    /**
     * 生成指定长度的随机Hex字符串（只包含0-9, a-f）
     *
     * @param length 长度，必须是偶数（每两个字符代表一个字节）
     * @return 随机Hex字符串
     * @throws IllegalArgumentException 长度小于1或为奇数抛异常
     */
    public static String randomHex(int length) {
        if (length <= 0 || length % 2 != 0) {
            throw new IllegalArgumentException("长度必须为正偶数");
        }
        byte[] bytes = new byte[length / 2];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(length);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 生成随机中国大陆手机号，格式：1开头，第二位3-9，后9位随机数字
     *
     * @return 随机手机号字符串
     */
    public static String randomChinaMobile() {
        String secondDigit = "3456789";
        StringBuilder sb = new StringBuilder("1");
        sb.append(secondDigit.charAt(SECURE_RANDOM.nextInt(secondDigit.length())));
        for (int i = 0; i < 9; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成随机Email地址，格式为：随机字符串@随机域名.随机顶级域名
     *
     * @return 随机Email地址
     */
    public static String randomEmail() {
        String user = randomAlphanumeric(6 + SECURE_RANDOM.nextInt(5)).toLowerCase();
        String[] domains = {"example", "test", "demo", "email", "random"};
        String[] tlds = {"com", "net", "org", "cn", "io"};
        String domain = domains[SECURE_RANDOM.nextInt(domains.length)];
        String tld = tlds[SECURE_RANDOM.nextInt(tlds.length)];
        return user + "@" + domain + "." + tld;
    }

    /**
     * 生成随机URL，简单格式：http(s)://随机域名.随机顶级域名/随机路径
     *
     * @return 随机URL字符串
     */
    public static String randomURL() {
        String protocol = SECURE_RANDOM.nextBoolean() ? "http" : "https";
        String[] domains = {"example", "test", "demo", "site", "random"};
        String[] tlds = {"com", "net", "org", "io", "cn"};
        String domain = domains[SECURE_RANDOM.nextInt(domains.length)];
        String tld = tlds[SECURE_RANDOM.nextInt(tlds.length)];
        String path = randomAlphanumeric(5 + SECURE_RANDOM.nextInt(5)).toLowerCase();
        return protocol + "://" + domain + "." + tld + "/" + path;
    }

    /**
     * 批量生成指定数量的UUID字符串列表
     *
     * @param count 生成数量
     * @return UUID字符串列表
     * @throws IllegalArgumentException 如果count <= 0抛异常
     */
    public static List<String> randomUUIDList(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(UUID.randomUUID().toString());
        }
        return list;
    }

    /**
     * 从指定List中随机抽取指定数量的不重复元素，返回新的List
     *
     * @param <T>   元素类型
     * @param list  原始List，不能为空且大小>=count
     * @param count 抽取元素数量
     * @return 随机抽取的元素List
     * @throws IllegalArgumentException 参数非法抛异常
     */
    public static <T> List<T> randomSubset(List<T> list, int count) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("原始列表不能为空");
        }
        if (count < 0 || count > list.size()) {
            throw new IllegalArgumentException("抽取数量不合法");
        }
        Set<Integer> indices = new HashSet<>(count);
        while (indices.size() < count) {
            indices.add(SECURE_RANDOM.nextInt(list.size()));
        }
        List<T> subset = new ArrayList<>(count);
        for (Integer index : indices) {
            subset.add(list.get(index));
        }
        return subset;
    }

    /**
     * 生成随机密码，支持自定义是否包含大写、小写、数字、特殊字符
     *
     * @param length     密码长度
     * @param useUpper   是否包含大写字母
     * @param useLower   是否包含小写字母
     * @param useDigits  是否包含数字
     * @param useSpecial 是否包含特殊字符
     * @return 随机密码字符串
     * @throws IllegalArgumentException 如果没有选择任何字符集或长度不合法抛异常
     */
    public static String randomPassword(int length, boolean useUpper, boolean useLower,
                                        boolean useDigits, boolean useSpecial) {
        if (length <= 0) {
            throw new IllegalArgumentException("密码长度必须大于0");
        }
        StringBuilder charPool = new StringBuilder();
        if (useUpper) charPool.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (useLower) charPool.append("abcdefghijklmnopqrstuvwxyz");
        if (useDigits) charPool.append("0123456789");
        if (useSpecial) charPool.append("!@#$%^&*()-_=+[]{}|;:,.<>?");

        if (charPool.length() == 0) {
            throw new IllegalArgumentException("必须至少选择一种字符类型");
        }

        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(charPool.charAt(SECURE_RANDOM.nextInt(charPool.length())));
        }
        return password.toString();
    }


    /**
     * 生成基于当前时间戳和随机数的全局唯一ID，格式：yyyyMMddHHmmssSSS + 4位随机数字
     *
     * @return 唯一ID字符串
     */
    public static String uniqueTimestampId() {
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int randomPart = SECURE_RANDOM.nextInt(10_000);
        return timestamp + String.format("%04d", randomPart);
    }

    /**
     * 生成一个随机强密码，保证包含大写字母、小写字母、数字、特殊字符且每类至少1个
     *
     * @param length 密码长度，必须 >= 4
     * @return 随机强密码字符串
     * @throws IllegalArgumentException 长度不合法抛异常
     */
    public static String randomStrongPasswordStrict(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("密码长度必须至少4");
        }

        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()-_=+[]{}|;:,.<>?";

        char[] password = new char[length];

        // 固定位置放保证每类字符至少一个
        password[0] = upper.charAt(SECURE_RANDOM.nextInt(upper.length()));
        password[1] = lower.charAt(SECURE_RANDOM.nextInt(lower.length()));
        password[2] = digits.charAt(SECURE_RANDOM.nextInt(digits.length()));
        password[3] = special.charAt(SECURE_RANDOM.nextInt(special.length()));

        String allChars = upper + lower + digits + special;

        for (int i = 4; i < length; i++) {
            password[i] = allChars.charAt(SECURE_RANDOM.nextInt(allChars.length()));
        }

        // 打乱数组顺序
        for (int i = password.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }

        return new String(password);
    }

    /**
     * 生成指定范围和数量的随机double数组，范围为[min, max)
     *
     * @param min   最小值（包含）
     * @param max   最大值（不包含）
     * @param count 数量
     * @return double数组
     * @throws IllegalArgumentException 参数非法抛异常
     */
    public static double[] randomDoubleArray(double min, double max, int count) {
        if (min >= max) {
            throw new IllegalArgumentException("min必须小于max");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        double[] arr = new double[count];
        for (int i = 0; i < count; i++) {
            arr[i] = min + SECURE_RANDOM.nextDouble() * (max - min);
        }
        return arr;
    }

    /**
     * 生成指定范围和数量的随机LocalDateTime列表，包含start和end
     *
     * @param start 起始时间（包含）
     * @param end   结束时间（包含）
     * @param count 数量
     * @return LocalDateTime列表
     * @throws IllegalArgumentException 参数非法抛异常
     */
    public static List<LocalDateTime> randomLocalDateTimeList(LocalDateTime start, LocalDateTime end, int count) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start和end不能为空");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start必须早于或等于end");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }

        long startEpoch = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endEpoch = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<LocalDateTime> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long randomEpoch = startEpoch + (long) (SECURE_RANDOM.nextDouble() * (endEpoch - startEpoch));
            list.add(LocalDateTime.ofInstant(Instant.ofEpochMilli(randomEpoch), ZoneId.systemDefault()));
        }
        return list;
    }

    /**
     * 生成指定长度的随机布尔数组
     *
     * @param length 长度
     * @return 随机布尔数组
     * @throws IllegalArgumentException 如果length <= 0抛异常
     */
    public static boolean[] randomBooleanArray(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        boolean[] arr = new boolean[length];
        for (int i = 0; i < length; i++) {
            arr[i] = SECURE_RANDOM.nextBoolean();
        }
        return arr;
    }

    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    /**
     * 生成随机UUID的短字符串表示（Base62编码，长度约22字符）
     *
     * @return Base62编码的UUID字符串
     */
    public static String randomUUIDBase62() {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = asBytes(uuid);
        return encodeBase62(bytes);
    }

    // UUID转byte[16]
    private static byte[] asBytes(UUID uuid) {
        byte[] buffer = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            buffer[i] = (byte) (lsb >>> (8 * (15 - i)));
        }
        return buffer;
    }

    // Base62编码
    private static String encodeBase62(byte[] data) {
        StringBuilder sb = new StringBuilder();
        java.math.BigInteger bi = new java.math.BigInteger(1, data);
        java.math.BigInteger base = java.math.BigInteger.valueOf(62);
        while (bi.compareTo(java.math.BigInteger.ZERO) > 0) {
            int mod = bi.mod(base).intValue();
            sb.append(BASE62[mod]);
            bi = bi.divide(base);
        }
        return sb.reverse().toString();
    }

    // 1. 雪花ID 简易版 -------------------------------------------------

    private static final long EPOCH = 1627776000000L; // 2021-08-01 起始时间戳（毫秒）
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static long workerId = 1L;      // 可配置
    private static long datacenterId = 1L;  // 可配置
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    /**
     * 生成唯一雪花ID（线程安全）
     *
     * @return 64位唯一ID
     */
    public synchronized static long nextSnowflakeId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("时钟回退，拒绝生成ID %d 毫秒", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1);
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private static long timeGen() {
        return System.currentTimeMillis();
    }

    // 2. 简单正则表达式随机字符串生成（支持[]范围，如 [a-z0-9]，长度固定） ---------------------

    /**
     * 根据简易正则字符集生成随机字符串（仅支持单字符集合如[a-zA-Z0-9]，不支持复杂正则）
     *
     * @param charSetRegEx 如 "[a-z0-9]"，必须形如[...]
     * @param length 生成字符串长度
     * @return 随机字符串
     * @throws IllegalArgumentException 参数不合法抛异常
     */
    public static String randomStringBySimpleRegex(String charSetRegEx, int length) {
        if (charSetRegEx == null || length <= 0) {
            throw new IllegalArgumentException("参数非法");
        }
        if (!charSetRegEx.matches("^\\[[^\\]]+\\]$")) {
            throw new IllegalArgumentException("仅支持简单字符集合形式如 [a-zA-Z0-9]");
        }

        String charSet = parseCharSet(charSetRegEx);
        if (charSet.isEmpty()) {
            throw new IllegalArgumentException("字符集为空");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charSet.charAt(SECURE_RANDOM.nextInt(charSet.length())));
        }
        return sb.toString();
    }
    // 解析类似 [a-zA-Z0-9] 到字符串集合 "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private static String parseCharSet(String charSetRegEx) {
        String content = charSetRegEx.substring(1, charSetRegEx.length() - 1);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (i + 2 < content.length() && content.charAt(i + 1) == '-') {
                // 范围，如a-z
                char start = c;
                char end = content.charAt(i + 2);
                for (char ch = start; ch <= end; ch++) {
                    sb.append(ch);
                }
                i += 3;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

}
