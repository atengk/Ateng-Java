package local.ateng.java.hutool.util;

/**
 * 通用基础工具类（基于 Hutool 工具类）
 *
 * @author Ateng
 * @since 2026-04-21
 */
public final class CommonUtil {

    private CommonUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断字符串是否为空白
     *
     * @param value 字符串
     * @return true-为空白，false-不为空白
     */
    public static boolean isBlank(CharSequence value) {
        return cn.hutool.core.util.StrUtil.isBlank(value);
    }

    /**
     * 判断字符串是否非空白
     *
     * @param value 字符串
     * @return true-非空白，false-为空白
     */
    public static boolean isNotBlank(CharSequence value) {
        return cn.hutool.core.util.StrUtil.isNotBlank(value);
    }

    /**
     * 判断对象是否为空
     *
     * @param value 对象
     * @return true-为空，false-不为空
     */
    public static boolean isEmpty(Object value) {
        return cn.hutool.core.util.ObjectUtil.isEmpty(value);
    }

    /**
     * 判断对象是否非空
     *
     * @param value 对象
     * @return true-非空，false-为空
     */
    public static boolean isNotEmpty(Object value) {
        return cn.hutool.core.util.ObjectUtil.isNotEmpty(value);
    }

    /**
     * 提供默认字符串值
     *
     * @param value        原字符串
     * @param defaultValue 默认值
     * @return 原字符串或默认值
     */
    public static String defaultIfBlank(CharSequence value, String defaultValue) {
        return cn.hutool.core.util.StrUtil.blankToDefault(value, defaultValue);
    }

    /**
     * 去除首尾空格
     *
     * @param value 字符串
     * @return 去除首尾空格后的结果
     */
    public static String trim(CharSequence value) {
        return cn.hutool.core.util.StrUtil.trim(value);
    }

    /**
     * 去除首尾空白并保留空串
     *
     * @param value 字符串
     * @return 去除首尾空白后的结果
     */
    public static String trimToEmpty(CharSequence value) {
        return cn.hutool.core.util.StrUtil.trimToEmpty(value);
    }

    /**
     * 转换为空串时返回默认值
     *
     * @param value        原字符串
     * @param defaultValue 默认值
     * @return 结果字符串
     */
    public static String emptyIfNull(CharSequence value, String defaultValue) {
        return cn.hutool.core.util.StrUtil.nullToDefault(value, defaultValue);
    }

    /**
     * 忽略大小写比较字符串
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return true-相等，false-不相等
     */
    public static boolean equalsIgnoreCase(CharSequence str1, CharSequence str2) {
        return cn.hutool.core.util.StrUtil.equalsIgnoreCase(str1, str2);
    }

    /**
     * 格式化字符串
     *
     * @param template 模板
     * @param args     参数
     * @return 格式化结果
     */
    public static String format(CharSequence template, Object... args) {
        return cn.hutool.core.util.StrUtil.format(template, args);
    }

    /**
     * 按分隔符拼接字符串
     *
     * @param delimiter 分隔符
     * @param values    值集合
     * @return 拼接结果
     */
    public static String join(CharSequence delimiter, Object... values) {
        return cn.hutool.core.util.StrUtil.join(delimiter, values);
    }

    /**
     * 按分隔符拆分字符串
     *
     * @param value     原字符串
     * @param separator 分隔符
     * @return 拆分结果
     */
    public static java.util.List<String> split(CharSequence value, CharSequence separator) {
        return cn.hutool.core.util.StrUtil.split(value, separator);
    }

    /**
     * 截取字符串
     *
     * @param value 原字符串
     * @param start 开始位置
     * @param end   结束位置
     * @return 截取结果
     */
    public static String sub(CharSequence value, int start, int end) {
        return cn.hutool.core.util.StrUtil.sub(value, start, end);
    }

    /**
     * 判断是否包含任意一个子串
     *
     * @param value      原字符串
     * @param searchText 搜索内容
     * @return true-包含，false-不包含
     */
    public static boolean containsAny(CharSequence value, CharSequence... searchText) {
        return cn.hutool.core.util.StrUtil.containsAny(value, searchText);
    }

    /**
     * 判断是否包含全部子串
     *
     * @param value      原字符串
     * @param searchText 搜索内容
     * @return true-全部包含，false-不全部包含
     */
    public static boolean containsAll(CharSequence value, CharSequence... searchText) {
        return cn.hutool.core.util.StrUtil.containsAll(value, searchText);
    }

    /**
     * 下划线转驼峰
     *
     * @param value 原字符串
     * @return 驼峰结果
     */
    public static String toCamelCase(CharSequence value) {
        return cn.hutool.core.util.StrUtil.toCamelCase(value);
    }

    /**
     * 驼峰转下划线
     *
     * @param value 原字符串
     * @return 下划线结果
     */
    public static String toUnderlineCase(CharSequence value) {
        return cn.hutool.core.util.StrUtil.toUnderlineCase(value);
    }

    /**
     * 首字母大写
     *
     * @param value 原字符串
     * @return 首字母大写结果
     */
    public static String upperFirst(CharSequence value) {
        return cn.hutool.core.util.StrUtil.upperFirst(value);
    }

    /**
     * 首字母小写
     *
     * @param value 原字符串
     * @return 首字母小写结果
     */
    public static String lowerFirst(CharSequence value) {
        return cn.hutool.core.util.StrUtil.lowerFirst(value);
    }

    /**
     * 生成 UUID
     *
     * @return UUID 字符串
     */
    public static String uuid() {
        return cn.hutool.core.util.IdUtil.simpleUUID();
    }

    /**
     * 生成不带横线 UUID
     *
     * @return UUID 字符串
     */
    public static String fastUUID() {
        return cn.hutool.core.util.IdUtil.fastSimpleUUID();
    }

    /**
     * 生成随机字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        return cn.hutool.core.util.RandomUtil.randomString(length);
    }

    /**
     * 生成随机数字字符串
     *
     * @param length 长度
     * @return 随机数字字符串
     */
    public static String randomNumbers(int length) {
        return cn.hutool.core.util.RandomUtil.randomNumbers(length);
    }

    /**
     * 生成随机中文字符串
     *
     * @param length 长度
     * @return 中文字符串
     */
    public static String randomChinese(int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(cn.hutool.core.util.RandomUtil.randomChinese());
        }
        return builder.toString();
    }

    /**
     * 对象转字符串
     *
     * @param value 对象
     * @return 字符串
     */
    public static String toStr(Object value) {
        return cn.hutool.core.convert.Convert.toStr(value);
    }

    /**
     * 对象转整数
     *
     * @param value 对象
     * @return Integer
     */
    public static Integer toInt(Object value) {
        return cn.hutool.core.convert.Convert.toInt(value);
    }

    /**
     * 对象转长整型
     *
     * @param value 对象
     * @return Long
     */
    public static Long toLong(Object value) {
        return cn.hutool.core.convert.Convert.toLong(value);
    }

    /**
     * 对象转布尔值
     *
     * @param value 对象
     * @return Boolean
     */
    public static Boolean toBool(Object value) {
        return cn.hutool.core.convert.Convert.toBool(value);
    }

    /**
     * 对象转 BigDecimal
     *
     * @param value 对象
     * @return BigDecimal
     */
    public static java.math.BigDecimal toBigDecimal(Object value) {
        return cn.hutool.core.convert.Convert.toBigDecimal(value);
    }

    /**
     * 对象转日期
     *
     * @param value 对象
     * @return 日期
     */
    public static java.util.Date toDate(Object value) {
        return cn.hutool.core.convert.Convert.toDate(value);
    }

    /**
     * 解析日期字符串（yyyy-MM-dd）
     *
     * @param value 日期字符串
     * @return 日期
     */
    public static java.util.Date parseDate(String value) {
        return cn.hutool.core.date.DateUtil.parseDate(value);
    }

    /**
     * 解析日期时间字符串（yyyy-MM-dd HH:mm:ss）
     *
     * @param value 日期时间字符串
     * @return 日期
     */
    public static java.util.Date parseDateTime(String value) {
        return cn.hutool.core.date.DateUtil.parseDateTime(value);
    }

    /**
     * 格式化日期（yyyy-MM-dd）
     *
     * @param date 日期
     * @return 字符串
     */
    public static String formatDate(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.formatDate(date);
    }

    /**
     * 格式化日期时间（yyyy-MM-dd HH:mm:ss）
     *
     * @param date 日期
     * @return 字符串
     */
    public static String formatDateTime(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.formatDateTime(date);
    }

    /**
     * 获取当前日期（yyyy-MM-dd）
     *
     * @return 日期字符串
     */
    public static String today() {
        return cn.hutool.core.date.DateUtil.today();
    }

    /**
     * 获取当前日期时间
     *
     * @return 日期时间字符串
     */
    public static String now() {
        return cn.hutool.core.date.DateUtil.now();
    }

    /**
     * 获取指定日期偏移后的日期（按天）
     *
     * @param date   日期
     * @param offset 偏移天数
     * @return 日期
     */
    public static java.util.Date offsetDate(java.util.Date date, int offset) {
        return cn.hutool.core.date.DateUtil.offsetDay(date, offset);
    }

    /**
     * 获取两个日期之间的天数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 天数
     */
    public static long betweenDay(java.util.Date start, java.util.Date end) {
        return cn.hutool.core.date.DateUtil.between(start, end,
                cn.hutool.core.date.DateUnit.DAY, true);
    }

    /**
     * 获取两个日期之间的小时差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 小时
     */
    public static long betweenHour(java.util.Date start, java.util.Date end) {
        return cn.hutool.core.date.DateUtil.between(start, end,
                cn.hutool.core.date.DateUnit.HOUR, true);
    }

    /**
     * 获取两个日期之间的分钟差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 分钟
     */
    public static long betweenMinute(java.util.Date start, java.util.Date end) {
        return cn.hutool.core.date.DateUtil.between(start, end,
                cn.hutool.core.date.DateUnit.MINUTE, true);
    }

    /**
     * 获取两个日期之间的秒数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 秒数
     */
    public static long betweenSecond(java.util.Date start, java.util.Date end) {
        return cn.hutool.core.date.DateUtil.between(start, end,
                cn.hutool.core.date.DateUnit.SECOND, true);
    }

    /**
     * 获取年龄
     *
     * @param birthDay 出生日期
     * @return 年龄
     */
    public static int age(java.util.Date birthDay) {
        return cn.hutool.core.date.DateUtil.age(birthDay, new java.util.Date());
    }

    /**
     * 获取月份第一天
     *
     * @param date 日期
     * @return 日期
     */
    public static java.util.Date beginOfMonth(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.beginOfMonth(date);
    }

    /**
     * 获取月份最后一天
     *
     * @param date 日期
     * @return 日期
     */
    public static java.util.Date endOfMonth(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.endOfMonth(date);
    }

    /**
     * 获取年份第一天
     *
     * @param date 日期
     * @return 日期
     */
    public static java.util.Date beginOfYear(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.beginOfYear(date);
    }

    /**
     * 获取年份最后一天
     *
     * @param date 日期
     * @return 日期
     */
    public static java.util.Date endOfYear(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.endOfYear(date);
    }

    /**
     * 判断日期是否同一天
     *
     * @param date1 日期1
     * @param date2 日期2
     * @return true-同一天，false-不同
     */
    public static boolean isSameDay(java.util.Date date1, java.util.Date date2) {
        return cn.hutool.core.date.DateUtil.isSameDay(date1, date2);
    }

    /**
     * 判断日期是否为今天
     *
     * @param date 日期
     * @return true-是今天，false-不是
     */
    public static boolean isToday(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.isSameDay(date, new java.util.Date());
    }

    /**
     * 将日期转时间戳（毫秒）
     *
     * @param date 日期
     * @return 时间戳
     */
    public static long toEpochMilli(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.date(date).getTime();
    }

    /**
     * 将时间戳转日期
     *
     * @param epochMilli 时间戳
     * @return 日期
     */
    public static java.util.Date ofEpochMilli(long epochMilli) {
        return cn.hutool.core.date.DateUtil.date(epochMilli);
    }

    /**
     * 集合是否为空
     *
     * @param collection 集合
     * @return true-为空，false-不为空
     */
    public static boolean isEmpty(java.util.Collection<?> collection) {
        return cn.hutool.core.collection.CollUtil.isEmpty(collection);
    }

    /**
     * Date 转 LocalDateTime
     *
     * @param date 日期
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime toLocalDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.of(date);
    }

    /**
     * LocalDateTime 转 Date
     *
     * @param dateTime 日期时间
     * @return Date
     */
    public static java.util.Date toDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        long epochMilli = cn.hutool.core.date.LocalDateTimeUtil.toEpochMilli(dateTime);
        return cn.hutool.core.date.DateUtil.date(epochMilli);
    }

    /**
     * 解析日期时间字符串
     *
     * @param value 日期时间字符串
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime parseLocalDateTime(String value) {
        if (cn.hutool.core.util.StrUtil.isBlank(value)) {
            return null;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.parse(value);
    }

    /**
     * 按指定格式解析日期时间字符串
     *
     * @param value   日期时间字符串
     * @param pattern 格式
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime parseLocalDateTime(String value, String pattern) {
        if (cn.hutool.core.util.StrUtil.isBlank(value) || cn.hutool.core.util.StrUtil.isBlank(pattern)) {
            return null;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.parse(value, pattern);
    }

    /**
     * 格式化日期时间，默认格式 yyyy-MM-dd HH:mm:ss
     *
     * @param dateTime 日期时间
     * @return 字符串
     */
    public static String formatLocalDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.formatNormal(dateTime);
    }

    /**
     * 按指定格式格式化日期时间
     *
     * @param dateTime 日期时间
     * @param pattern  格式
     * @return 字符串
     */
    public static String formatLocalDateTime(java.time.LocalDateTime dateTime, String pattern) {
        if (dateTime == null || cn.hutool.core.util.StrUtil.isBlank(pattern)) {
            return null;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.format(dateTime, pattern);
    }

    /**
     * 获取当前日期时间
     *
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime nowLocalDateTime() {
        return cn.hutool.core.date.LocalDateTimeUtil.now();
    }

    /**
     * 获取当前日期
     *
     * @return LocalDate
     */
    public static java.time.LocalDate todayLocalDate() {
        return cn.hutool.core.date.LocalDateTimeUtil.ofDate(cn.hutool.core.date.LocalDateTimeUtil.now());
    }

    /**
     * 获取指定日期偏移后的日期时间
     *
     * @param dateTime   日期时间
     * @param offsetDays 偏移天数
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime offsetLocalDateTime(java.time.LocalDateTime dateTime, long offsetDays) {
        if (dateTime == null) {
            return null;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.offset(dateTime, offsetDays, java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * 获取两个日期时间之间的天数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 天数
     */
    public static long betweenDay(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Math.abs(cn.hutool.core.date.LocalDateTimeUtil.between(start, end, java.time.temporal.ChronoUnit.DAYS));
    }

    /**
     * 获取两个日期时间之间的小时差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 小时
     */
    public static long betweenHour(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Math.abs(cn.hutool.core.date.LocalDateTimeUtil.between(start, end, java.time.temporal.ChronoUnit.HOURS));
    }

    /**
     * 获取两个日期时间之间的分钟差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 分钟
     */
    public static long betweenMinute(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Math.abs(cn.hutool.core.date.LocalDateTimeUtil.between(start, end, java.time.temporal.ChronoUnit.MINUTES));
    }

    /**
     * 获取两个日期时间之间的秒数差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 秒数
     */
    public static long betweenSecond(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Math.abs(cn.hutool.core.date.LocalDateTimeUtil.between(start, end, java.time.temporal.ChronoUnit.SECONDS));
    }

    /**
     * 获取月份第一天开始时间
     *
     * @param dateTime 日期时间
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime beginOfMonth(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfMonth(1).atStartOfDay();
    }

    /**
     * 获取月份最后一天结束时间
     *
     * @param dateTime 日期时间
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime endOfMonth(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return java.time.YearMonth.from(dateTime).atEndOfMonth().atTime(java.time.LocalTime.MAX);
    }

    /**
     * 获取年份第一天开始时间
     *
     * @param dateTime 日期时间
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime beginOfYear(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfYear(1).atStartOfDay();
    }

    /**
     * 获取年份最后一天结束时间
     *
     * @param dateTime 日期时间
     * @return LocalDateTime
     */
    public static java.time.LocalDateTime endOfYear(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().withDayOfYear(dateTime.toLocalDate().lengthOfYear()).atTime(java.time.LocalTime.MAX);
    }

    /**
     * 判断日期时间是否同一天
     *
     * @param dateTime1 日期时间1
     * @param dateTime2 日期时间2
     * @return true-同一天，false-不同
     */
    public static boolean isSameDay(java.time.LocalDateTime dateTime1, java.time.LocalDateTime dateTime2) {
        if (dateTime1 == null || dateTime2 == null) {
            return false;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.isSameDay(dateTime1, dateTime2);
    }

    /**
     * 判断日期时间是否为今天
     *
     * @param dateTime 日期时间
     * @return true-是今天，false-不是
     */
    public static boolean isToday(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.isSameDay(dateTime, cn.hutool.core.date.LocalDateTimeUtil.now());
    }

    /**
     * 将日期时间转时间戳
     *
     * @param dateTime 日期时间
     * @return 时间戳
     */
    public static long toEpochMilli(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.toEpochMilli(dateTime);
    }

    /**
     * 将时间戳转日期时间
     *
     * @param epochMilli 时间戳
     * @return 日期时间
     */
    public static java.time.LocalDateTime ofEpochMilliLocalDateTime(long epochMilli) {
        return cn.hutool.core.date.LocalDateTimeUtil.of(epochMilli);
    }

    /**
     * 判断日期时间是否为周末
     *
     * @param dateTime 日期时间
     * @return true-是周末，false-不是
     */
    public static boolean isWeekend(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.isWeekend(dateTime);
    }

    /**
     * 判断当前日期时间是否在指定范围内
     *
     * @param dateTime 日期时间
     * @param begin    开始时间
     * @param end      结束时间
     * @return true-在范围内，false-不在
     */
    public static boolean isIn(java.time.LocalDateTime dateTime,
                               java.time.LocalDateTime begin,
                               java.time.LocalDateTime end) {
        if (dateTime == null || begin == null || end == null) {
            return false;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.isIn(dateTime, begin, end);
    }

    /**
     * 获取指定日期时间是本年的第几周
     *
     * @param dateTime 日期时间
     * @return 周序号
     */
    public static int weekOfYear(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.weekOfYear(dateTime);
    }

    /**
     * 集合是否非空
     *
     * @param collection 集合
     * @return true-非空，false-为空
     */
    public static boolean isNotEmpty(java.util.Collection<?> collection) {
        return cn.hutool.core.collection.CollUtil.isNotEmpty(collection);
    }

    /**
     * 获取集合大小
     *
     * @param collection 集合
     * @return 大小
     */
    public static int size(java.util.Collection<?> collection) {
        return cn.hutool.core.collection.CollUtil.size(collection);
    }

    /**
     * 获取集合第一个元素
     *
     * @param collection 集合
     * @param <T>        元素类型
     * @return 元素
     */
    public static <T> T first(java.util.Collection<T> collection) {
        return cn.hutool.core.collection.CollUtil.getFirst(collection);
    }

    /**
     * 获取集合最后一个元素
     *
     * @param collection 集合
     * @param <T>        元素类型
     * @return 元素
     */
    public static <T> T last(java.util.List<T> collection) {
        return cn.hutool.core.collection.CollUtil.getLast(collection);
    }

    /**
     * 反转列表
     *
     * @param list 列表
     * @param <T>  类型
     * @return 反转后的列表
     */
    public static <T> java.util.List<T> reverse(java.util.List<T> list) {
        return cn.hutool.core.collection.CollUtil.reverse(list);
    }

    /**
     * 排序集合（自然排序，不修改原集合）
     */
    public static <T extends Comparable<? super T>> java.util.List<T> sort(
            java.util.Collection<T> collection) {
        return cn.hutool.core.collection.CollUtil.sort(
                collection,
                java.util.Comparator.naturalOrder()
        );
    }

    /**
     * 排序集合（自定义比较器，不修改原集合）
     */
    public static <T> java.util.List<T> sort(
            java.util.Collection<T> collection,
            java.util.Comparator<? super T> comparator) {
        return cn.hutool.core.collection.CollUtil.sort(collection, comparator);
    }

    /**
     * 原地排序集合（自然排序，会修改原List）
     *
     * @param list 列表
     * @param <T>  元素类型
     */
    public static <T extends Comparable<? super T>> void sortInPlace(java.util.List<T> list) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(list)) {
            return;
        }
        cn.hutool.core.collection.CollUtil.sort(list, java.util.Comparator.naturalOrder());
    }

    /**
     * 原地排序集合（自定义比较器，会修改原List）
     *
     * @param list       列表
     * @param comparator 比较器
     * @param <T>        元素类型
     */
    public static <T> void sortInPlace(java.util.List<T> list,
                                       java.util.Comparator<? super T> comparator) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(list)) {
            return;
        }
        cn.hutool.core.collection.CollUtil.sort(list, comparator);
    }

    /**
     * 去重集合
     *
     * @param collection 集合
     * @param <T>        类型
     * @return 去重后的集合
     */
    public static <T> java.util.List<T> distinct(java.util.Collection<T> collection) {
        return cn.hutool.core.collection.CollUtil.distinct(collection);
    }

    /**
     * 过滤集合
     *
     * @param collection 集合
     * @param filter     过滤条件
     * @param <T>        类型
     * @return 过滤后的集合
     */
    public static <T> java.util.List<T> filter(java.util.Collection<T> collection,
                                               cn.hutool.core.lang.Filter<T> filter) {
        if (collection == null) {
            return new java.util.ArrayList<>();
        }
        return new java.util.ArrayList<>(cn.hutool.core.collection.CollUtil.filter(collection, filter));
    }

    /**
     * 映射集合
     *
     * @param collection 集合
     * @param mapper     映射函数
     * @param <T>        原类型
     * @param <R>        目标类型
     * @return 映射后的集合
     */
    public static <T, R> java.util.List<R> map(java.util.Collection<T> collection,
                                               java.util.function.Function<T, R> mapper) {
        return cn.hutool.core.collection.CollUtil.map(collection, mapper, true);
    }

    /**
     * 按字段分组集合
     *
     * @param collection 集合
     * @param keyMapper  分组字段函数
     * @param <T>        元素类型
     * @param <K>        key类型
     * @return 分组结果
     */
    public static <T, K> java.util.Map<K, java.util.List<T>> groupBy(
            java.util.Collection<T> collection,
            java.util.function.Function<T, K> keyMapper) {
        if (collection == null || keyMapper == null) {
            return new java.util.HashMap<>();
        }
        return cn.hutool.core.collection.CollStreamUtil.groupByKey(collection, keyMapper);
    }

    /**
     * 将集合转为列表
     *
     * @param collection 集合
     * @param <T>        类型
     * @return List
     */
    public static <T> java.util.List<T> toList(java.util.Collection<T> collection) {
        return cn.hutool.core.collection.CollUtil.newArrayList(collection);
    }

    /**
     * 将集合转为Set
     *
     * @param collection 集合
     * @param <T>        类型
     * @return Set
     */
    public static <T> java.util.Set<T> toSet(java.util.Collection<T> collection) {
        return cn.hutool.core.collection.CollUtil.newHashSet(collection);
    }

    /**
     * 合并两个集合
     *
     * @param c1  集合1
     * @param c2  集合2
     * @param <T> 类型
     * @return 合并后的集合
     */
    public static <T> java.util.List<T> merge(java.util.Collection<T> c1, java.util.Collection<T> c2) {
        return new java.util.ArrayList<>(cn.hutool.core.collection.CollUtil.union(c1, c2));
    }

    /**
     * 交集
     *
     * @param c1  集合1
     * @param c2  集合2
     * @param <T> 类型
     * @return 交集
     */
    public static <T> java.util.Collection<T> intersection(java.util.Collection<T> c1, java.util.Collection<T> c2) {
        return cn.hutool.core.collection.CollUtil.intersection(c1, c2);
    }

    /**
     * 并集
     *
     * @param c1  集合1
     * @param c2  集合2
     * @param <T> 类型
     * @return 并集
     */
    public static <T> java.util.Collection<T> union(java.util.Collection<T> c1, java.util.Collection<T> c2) {
        return cn.hutool.core.collection.CollUtil.unionDistinct(c1, c2);
    }

    /**
     * 差集（c1 - c2）
     *
     * @param c1  集合1
     * @param c2  集合2
     * @param <T> 类型
     * @return 差集
     */
    public static <T> java.util.Collection<T> subtract(java.util.Collection<T> c1, java.util.Collection<T> c2) {
        return cn.hutool.core.collection.CollUtil.subtract(c1, c2);
    }

    /**
     * 判断集合是否包含元素
     *
     * @param collection 集合
     * @param value      元素
     * @return true-包含
     */
    public static boolean contains(java.util.Collection<?> collection, Object value) {
        return cn.hutool.core.collection.CollUtil.contains(collection, value);
    }

    /**
     * 判断集合是否包含全部元素
     *
     * @param collection 集合
     * @param values     元素集合
     * @return true-全部包含
     */
    public static boolean containsAll(java.util.Collection<?> collection, java.util.Collection<?> values) {
        return cn.hutool.core.collection.CollUtil.containsAll(collection, values);
    }

    /**
     * 安全获取集合元素
     *
     * @param list  列表
     * @param index 索引
     * @param <T>   类型
     * @return 元素或null
     */
    public static <T> T get(java.util.List<T> list, int index) {
        return cn.hutool.core.collection.CollUtil.get(list, index);
    }

    /**
     * 判断Map是否为空
     *
     * @param map Map
     * @return true-为空
     */
    public static boolean isMapEmpty(java.util.Map<?, ?> map) {
        return cn.hutool.core.map.MapUtil.isEmpty(map);
    }

    /**
     * 判断Map是否非空
     *
     * @param map Map
     * @return true-非空
     */
    public static boolean isMapNotEmpty(java.util.Map<?, ?> map) {
        return cn.hutool.core.map.MapUtil.isNotEmpty(map);
    }

    /**
     * 获取Map指定键值
     *
     * @param map Map
     * @param key 键
     * @param <T> 返回类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(java.util.Map<?, ?> map, Object key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    /**
     * 获取Map指定键值并带默认值
     *
     * @param map          Map
     * @param key          键
     * @param defaultValue 默认值
     * @param <T>          类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrDefault(java.util.Map<?, ?> map, Object key, T defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        return value == null ? defaultValue : (T) value;
    }

    /**
     * Map 转 Bean
     *
     * @param map   Map
     * @param clazz 目标类型
     * @param <T>   类型
     * @return Bean
     */
    public static <T> T toBean(java.util.Map<?, ?> map, Class<T> clazz) {
        return cn.hutool.core.bean.BeanUtil.toBean(map, clazz);
    }

    /**
     * Map 转 List（value集合）
     *
     * @param map Map
     * @param <T> 类型
     * @return List
     */
    @SuppressWarnings("unchecked")
    public static <T> java.util.List<T> toList(java.util.Map<?, ?> map) {
        return new java.util.ArrayList<>((java.util.Collection<T>) map.values());
    }

    /**
     * 获取Map的key集合
     *
     * @param map Map
     * @return key集合
     */
    public static java.util.Set<?> keySet(java.util.Map<?, ?> map) {
        return map == null ? java.util.Collections.emptySet() : map.keySet();
    }

    /**
     * 获取Map的value集合
     *
     * @param map Map
     * @return value集合
     */
    public static java.util.Collection<?> values(java.util.Map<?, ?> map) {
        return map == null ? java.util.Collections.emptyList() : map.values();
    }

    /**
     * 合并两个Map（后者覆盖前者）
     *
     * @param map1 Map1
     * @param map2 Map2
     * @param <K>  key类型
     * @param <V>  value类型
     * @return 合并后的Map
     */
    public static <K, V> java.util.Map<K, V> mergeMap(java.util.Map<K, V> map1, java.util.Map<K, V> map2) {
        java.util.Map<K, V> result = cn.hutool.core.map.MapUtil.newHashMap();
        if (map1 != null && !map1.isEmpty()) {
            result.putAll(map1);
        }
        if (map2 != null && !map2.isEmpty()) {
            result.putAll(map2);
        }
        return result;
    }

    /**
     * 过滤Map
     *
     * @param map    Map
     * @param filter 过滤条件
     * @param <K>    key类型
     * @param <V>    value类型
     * @return 过滤后的Map
     */
    public static <K, V> java.util.Map<K, V> filter(java.util.Map<K, V> map,
                                                    cn.hutool.core.lang.Filter<java.util.Map.Entry<K, V>> filter) {
        return cn.hutool.core.map.MapUtil.filter(map, filter);
    }

    /**
     * 反转Map（key和value交换）
     *
     * @param map Map
     * @param <K> key类型
     * @param <V> value类型
     * @return 反转后的Map
     */
    public static <K, V> java.util.Map<V, K> invert(java.util.Map<K, V> map) {
        return cn.hutool.core.map.MapUtil.inverse(map);
    }

    /**
     * Bean属性拷贝
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        cn.hutool.core.bean.BeanUtil.copyProperties(source, target);
    }

    /**
     * Bean转Bean
     *
     * @param source 源对象
     * @param clazz  目标类型
     * @param <T>    类型
     * @return 新对象
     */
    public static <T> T toBean(Object source, Class<T> clazz) {
        return cn.hutool.core.bean.BeanUtil.toBean(source, clazz);
    }

    /**
     * Bean转Map
     *
     * @param bean Bean
     * @return Map
     */
    public static java.util.Map<String, Object> beanToMap(Object bean) {
        return cn.hutool.core.bean.BeanUtil.beanToMap(bean);
    }

    /**
     * Map转Bean
     *
     * @param map   Map
     * @param clazz 类型
     * @param <T>   类型
     * @return Bean
     */
    public static <T> T mapToBean(java.util.Map<?, ?> map, Class<T> clazz) {
        return cn.hutool.core.bean.BeanUtil.toBean(map, clazz);
    }

    /**
     * Bean转JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJsonStr(Object obj) {
        return cn.hutool.json.JSONUtil.toJsonStr(obj);
    }

    /**
     * JSON字符串转Bean
     *
     * @param json  JSON字符串
     * @param clazz 类型
     * @param <T>   类型
     * @return Bean
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        return cn.hutool.json.JSONUtil.toBean(json, clazz);
    }

    /**
     * JSON字符串转Map
     *
     * @param json JSON字符串
     * @return Map
     */
    public static java.util.Map<String, Object> parseMap(String json) {
        return cn.hutool.json.JSONUtil.parseObj(json);
    }

    /**
     * JSON字符串转列表
     *
     * @param json  JSON字符串
     * @param clazz 元素类型
     * @param <T>   类型
     * @return List
     */
    public static <T> java.util.List<T> parseArray(String json, Class<T> clazz) {
        return cn.hutool.json.JSONUtil.toList(json, clazz);
    }

    /**
     * 对象深拷贝
     *
     * @param obj 对象
     * @param <T> 类型
     * @return 拷贝对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T obj) {
        return (T) cn.hutool.core.util.ObjectUtil.clone(obj);
    }

    /**
     * 提取对象指定字段
     *
     * @param bean   对象
     * @param fields 字段名
     * @return Map
     */
    public static java.util.Map<String, Object> pick(Object bean, String... fields) {
        java.util.Map<String, Object> map = cn.hutool.core.bean.BeanUtil.beanToMap(bean);
        return cn.hutool.core.map.MapUtil.filter(map, entry -> cn.hutool.core.util.ArrayUtil.contains(fields, entry.getKey()));
    }

    /**
     * 创建文件
     *
     * @param path 路径
     * @return 文件
     */
    public static java.io.File createFile(String path) {
        return cn.hutool.core.io.FileUtil.touch(path);
    }

    /**
     * 创建目录
     *
     * @param path 路径
     * @return 目录
     */
    public static java.io.File createDir(String path) {
        return cn.hutool.core.io.FileUtil.mkdir(path);
    }

    /**
     * 复制文件或目录
     *
     * @param src  源
     * @param dest 目标
     */
    public static void copy(String src, String dest) {
        cn.hutool.core.io.FileUtil.copy(src, dest, true);
    }

    /**
     * 移动文件或目录
     *
     * @param src      源
     * @param dest     目标
     * @param override 是否覆盖
     */
    public static void move(String src, String dest, boolean override) {
        cn.hutool.core.io.FileUtil.move(new java.io.File(src), new java.io.File(dest), override);
    }

    /**
     * 删除文件或目录
     *
     * @param path 路径
     */
    public static void delete(String path) {
        cn.hutool.core.io.FileUtil.del(path);
    }

    /**
     * 判断字符串是否为数字
     *
     * @param value 字符串
     * @return true-是数字
     */
    public static boolean isNumber(CharSequence value) {
        return cn.hutool.core.util.NumberUtil.isNumber(value);
    }

    /**
     * 判断字符串是否为整数
     *
     * @param value 字符串
     * @return true-是整数
     */
    public static boolean isInteger(String value) {
        return cn.hutool.core.util.NumberUtil.isInteger(value);
    }

    /**
     * 判断字符串是否为浮点数
     *
     * @param value 字符串
     * @return true-是浮点数
     */
    public static boolean isDouble(String value) {
        return cn.hutool.core.util.NumberUtil.isDouble(value);
    }

    /**
     * 判断是否为邮箱
     *
     * @param value 字符串
     * @return true-是邮箱
     */
    public static boolean isEmail(CharSequence value) {
        return cn.hutool.core.lang.Validator.isEmail(value);
    }

    /**
     * 判断是否为手机号（中国）
     *
     * @param value 字符串
     * @return true-是手机号
     */
    public static boolean isMobile(CharSequence value) {
        return cn.hutool.core.lang.Validator.isMobile(value);
    }

    /**
     * 判断是否为身份证号（中国）
     *
     * @param value 字符串
     * @return true-是身份证
     */
    public static boolean isIdCard(CharSequence value) {
        return cn.hutool.core.lang.Validator.isCitizenId(value);
    }

    /**
     * 判断是否为URL
     *
     * @param value 字符串
     * @return true-是URL
     */
    public static boolean isUrl(CharSequence value) {
        return cn.hutool.core.lang.Validator.isUrl(value);
    }

    /**
     * 判断是否为IPv4
     *
     * @param value 字符串
     * @return true-是IPv4
     */
    public static boolean isIpv4(CharSequence value) {
        return cn.hutool.core.util.StrUtil.isNotBlank(value)
                && cn.hutool.core.lang.Validator.isIpv4(value.toString());
    }

    /**
     * 判断是否为IPv6
     *
     * @param value 字符串
     * @return true-是IPv6
     */
    public static boolean isIpv6(CharSequence value) {
        return cn.hutool.core.util.StrUtil.isNotBlank(value)
                && cn.hutool.core.lang.Validator.isIpv6(value.toString());
    }

    /**
     * 正则匹配
     *
     * @param regex 正则
     * @param value 字符串
     * @return true-匹配
     */
    public static boolean isMatch(String regex, CharSequence value) {
        return cn.hutool.core.util.ReUtil.isMatch(regex, value);
    }

    /**
     * 提取第一个匹配
     *
     * @param regex 正则
     * @param value 字符串
     * @return 匹配结果
     */
    public static String extractFirst(String regex, CharSequence value) {
        return cn.hutool.core.util.ReUtil.get(regex, value, 0);
    }

    /**
     * 提取全部匹配
     *
     * @param regex 正则
     * @param value 字符串
     * @return 匹配列表
     */
    public static java.util.List<String> extractAll(String regex, CharSequence value) {
        return cn.hutool.core.util.ReUtil.findAll(regex, value, 0);
    }

    /**
     * 正则替换
     *
     * @param regex       正则
     * @param replacement 替换内容
     * @param value       字符串
     * @return 结果
     */
    public static String replaceAll(String regex, String replacement, CharSequence value) {
        return cn.hutool.core.util.ReUtil.replaceAll(value, regex, replacement);
    }

    /**
     * 模糊匹配（通配符*）
     *
     * @param pattern 模式
     * @param value   字符串
     * @return true-匹配
     */
    public static boolean likeMatch(String pattern, CharSequence value) {
        String regex = pattern.replace("*", ".*");
        return cn.hutool.core.util.ReUtil.isMatch(regex, value);
    }

    /**
     * 手机号脱敏
     *
     * @param value 手机号
     * @return 脱敏结果
     */
    public static String desensitizeMobile(CharSequence value) {
        return cn.hutool.core.util.DesensitizedUtil.mobilePhone(value.toString());
    }

    /**
     * 身份证脱敏
     *
     * @param value 身份证
     * @return 脱敏结果
     */
    public static String desensitizeIdCard(CharSequence value) {
        return cn.hutool.core.util.DesensitizedUtil.idCardNum(value.toString(), 6, 4);
    }

    /**
     * 邮箱脱敏
     *
     * @param value 邮箱
     * @return 脱敏结果
     */
    public static String desensitizeEmail(CharSequence value) {
        return cn.hutool.core.util.DesensitizedUtil.email(value.toString());
    }

    /**
     * 隐藏字符串中间部分
     *
     * @param value 字符串
     * @param start 前保留
     * @param end   后保留
     * @return 结果
     */
    public static String hide(CharSequence value, int start, int end) {
        return cn.hutool.core.util.StrUtil.hide(value, start, value.length() - end);
    }

    /**
     * 生成掩码字符串
     *
     * @param value    字符串
     * @param maskChar 掩码字符
     * @return 掩码结果
     */
    public static String mask(CharSequence value, char maskChar) {
        return cn.hutool.core.util.StrUtil.repeat(maskChar, value.length());
    }

    /**
     * 字符串左侧补齐
     *
     * @param value   字符串
     * @param length  目标长度
     * @param padChar 填充字符
     * @return 结果
     */
    public static String padPre(CharSequence value, int length, char padChar) {
        return cn.hutool.core.util.StrUtil.padPre(value, length, padChar);
    }

    /**
     * 字符串右侧补齐
     *
     * @param value   字符串
     * @param length  目标长度
     * @param padChar 填充字符
     * @return 结果
     */
    public static String padAfter(CharSequence value, int length, char padChar) {
        return cn.hutool.core.util.StrUtil.padAfter(value, length, padChar);
    }

    /**
     * 字符串重复
     *
     * @param value 字符串
     * @param count 次数
     * @return 结果
     */
    public static String repeat(CharSequence value, int count) {
        return cn.hutool.core.util.StrUtil.repeat(value, count);
    }

    /**
     * 字符串反转
     *
     * @param value 字符串
     * @return 结果
     */
    public static String reverse(String value) {
        return cn.hutool.core.util.StrUtil.reverse(value);
    }

    /**
     * 统计子串出现次数
     *
     * @param value 原字符串
     * @param sub   子串
     * @return 次数
     */
    public static int count(CharSequence value, CharSequence sub) {
        return cn.hutool.core.util.StrUtil.count(value, sub);
    }

    /**
     * 清理不可见字符
     *
     * @param value 字符串
     * @return 结果
     */
    public static String cleanBlank(CharSequence value) {
        return cn.hutool.core.util.StrUtil.cleanBlank(value);
    }

    /**
     * 转换为全角字符
     *
     * @param value 字符串
     * @return 结果
     */
    public static String toSBC(CharSequence value) {
        if (value == null) {
            return null;
        }
        return cn.hutool.core.convert.Convert.toSBC(value.toString());
    }

    /**
     * 转换为半角字符
     *
     * @param value 字符串
     * @return 结果
     */
    public static String toDBC(CharSequence value) {
        if (value == null) {
            return null;
        }
        return cn.hutool.core.convert.Convert.toDBC(value.toString());
    }

    /**
     * 字符串编码转换
     *
     * @param value       字符串
     * @param srcCharset  源编码
     * @param destCharset 目标编码
     * @return 结果
     */
    public static String convertCharset(String value, String srcCharset,
                                        String destCharset) {
        if (value == null || srcCharset == null || destCharset == null) {
            return value;
        }
        return cn.hutool.core.convert.Convert.convertCharset(value, srcCharset, destCharset);
    }

    /**
     * Base64编码
     *
     * @param value 字符串
     * @return 编码结果
     */
    public static String base64Encode(CharSequence value) {
        return cn.hutool.core.codec.Base64.encode(value.toString());
    }

    /**
     * Base64解码
     *
     * @param value Base64字符串
     * @return 解码结果
     */
    public static String base64Decode(CharSequence value) {
        return cn.hutool.core.codec.Base64.decodeStr(value.toString());
    }

    /**
     * URL编码
     *
     * @param value   字符串
     * @param charset 编码
     * @return 编码结果
     */
    public static String urlEncode(String value, java.nio.charset.Charset charset) {
        return cn.hutool.core.net.URLEncodeUtil.encode(value, charset);
    }

    /**
     * URL解码
     *
     * @param value   字符串
     * @param charset 编码
     * @return 解码结果
     */
    public static String urlDecode(String value, java.nio.charset.Charset charset) {
        return cn.hutool.core.net.URLDecoder.decode(value, charset);
    }

    /**
     * HTML转义
     *
     * @param value 字符串
     * @return 转义结果
     */
    public static String escapeHtml(CharSequence value) {
        return cn.hutool.http.HtmlUtil.escape(value.toString());
    }

    /**
     * HTML反转义
     *
     * @param value 字符串
     * @return 结果
     */
    public static String unescapeHtml(CharSequence value) {
        return cn.hutool.http.HtmlUtil.unescape(value.toString());
    }

    /**
     * XML转义
     *
     * @param value 字符串
     * @return 转义结果
     */
    public static String escapeXml(CharSequence value) {
        return cn.hutool.core.util.XmlUtil.escape(value.toString());
    }

    /**
     * XML反转义
     *
     * @param value 字符串
     * @return 结果
     */
    public static String unescapeXml(CharSequence value) {
        return cn.hutool.core.util.XmlUtil.unescape(value.toString());
    }

    /**
     * Unicode编码
     *
     * @param value 字符串
     * @return 编码结果
     */
    public static String unicodeEncode(String value) {
        if (value == null) {
            return null;
        }
        return cn.hutool.core.text.UnicodeUtil.toUnicode(value);
    }

    /**
     * Unicode解码
     *
     * @param value 字符串
     * @return 解码结果
     */
    public static String unicodeDecode(String value) {
        if (value == null) {
            return null;
        }
        return cn.hutool.core.text.UnicodeUtil.toString(value);
    }

    /**
     * Hex编码
     *
     * @param value 字符串
     * @return 编码结果
     */
    public static String hexEncode(CharSequence value) {
        return cn.hutool.core.util.HexUtil.encodeHexStr(value.toString());
    }

    /**
     * Hex解码
     *
     * @param value Hex字符串
     * @return 解码结果
     */
    public static String hexDecode(CharSequence value) {
        return cn.hutool.core.util.HexUtil.decodeHexStr(value.toString());
    }

    /**
     * MD5加密
     *
     * @param value 字符串
     * @return 加密结果
     */
    public static String md5(String value) {
        return cn.hutool.crypto.SecureUtil.md5(value);
    }

    /**
     * SHA-1加密
     *
     * @param value 字符串
     * @return 加密结果
     */
    public static String sha1(String value) {
        return cn.hutool.crypto.SecureUtil.sha1(value);
    }

    /**
     * SHA-256加密
     *
     * @param value 字符串
     * @return 加密结果
     */
    public static String sha256(String value) {
        return cn.hutool.crypto.SecureUtil.sha256(value);
    }

    /**
     * SHA-512加密
     *
     * @param value 字符串
     * @return 加密结果
     */
    public static String sha512(String value) {
        if (cn.hutool.core.util.StrUtil.isBlank(value)) {
            return null;
        }
        return cn.hutool.crypto.digest.DigestUtil.sha512Hex(value);
    }

    /**
     * BCrypt加密
     *
     * @param value 明文
     * @return 密文
     */
    public static String bcrypt(String value) {
        return cn.hutool.crypto.digest.BCrypt.hashpw(value);
    }

    /**
     * 验证BCrypt
     *
     * @param value  明文
     * @param hashed 密文
     * @return true-匹配
     */
    public static boolean bcryptCheck(String value, String hashed) {
        return cn.hutool.crypto.digest.BCrypt.checkpw(value, hashed);
    }

    /**
     * AES加密
     *
     * @param value 明文
     * @param key   密钥（16/24/32位）
     * @return 密文（Base64）
     */
    public static String aesEncrypt(String value, String key) {
        return cn.hutool.crypto.SecureUtil.aes(key.getBytes()).encryptBase64(value);
    }

    /**
     * AES解密
     *
     * @param value 密文（Base64）
     * @param key   密钥
     * @return 明文
     */
    public static String aesDecrypt(String value, String key) {
        return cn.hutool.crypto.SecureUtil.aes(key.getBytes()).decryptStr(value);
    }

    /**
     * DES加密
     *
     * @param value 明文
     * @param key   密钥（8位）
     * @return 密文
     */
    public static String desEncrypt(String value, String key) {
        return cn.hutool.crypto.SecureUtil.des(key.getBytes()).encryptBase64(value);
    }

    /**
     * DES解密
     *
     * @param value 密文
     * @param key   密钥
     * @return 明文
     */
    public static String desDecrypt(String value, String key) {
        return cn.hutool.crypto.SecureUtil.des(key.getBytes()).decryptStr(value);
    }

    /**
     * RSA公钥加密
     *
     * @param value     明文
     * @param publicKey 公钥(Base64)
     * @return 密文(Base64)
     */
    public static String rsaEncryptByPublicKey(String value, String publicKey) {
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(null, publicKey);
        return rsa.encryptBase64(value, cn.hutool.crypto.asymmetric.KeyType.PublicKey);
    }

    /**
     * RSA私钥解密
     *
     * @param value      密文
     * @param privateKey 私钥(Base64)
     * @return 明文
     */
    public static String rsaDecryptByPrivateKey(String value, String privateKey) {
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(privateKey, null);
        return rsa.decryptStr(value, cn.hutool.crypto.asymmetric.KeyType.PrivateKey);
    }

    /**
     * RSA私钥加密
     *
     * @param value      明文
     * @param privateKey 私钥
     * @return 密文
     */
    public static String rsaEncryptByPrivateKey(String value, String privateKey) {
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(privateKey, null);
        return rsa.encryptBase64(value, cn.hutool.crypto.asymmetric.KeyType.PrivateKey);
    }

    /**
     * RSA公钥解密
     *
     * @param value     密文
     * @param publicKey 公钥
     * @return 明文
     */
    public static String rsaDecryptByPublicKey(String value, String publicKey) {
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(null, publicKey);
        return rsa.decryptStr(value, cn.hutool.crypto.asymmetric.KeyType.PublicKey);
    }

    /**
     * 生成RSA密钥对
     *
     * @return Map（publicKey / privateKey）
     */
    public static java.util.Map<String, String> generateRsaKeyPair() {
        cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA();
        java.util.Map<String, String> map = new java.util.HashMap<>(2);
        map.put("publicKey", rsa.getPublicKeyBase64());
        map.put("privateKey", rsa.getPrivateKeyBase64());
        return map;
    }

    /**
     * 生成签名（SHA256withRSA）
     *
     * @param data       数据
     * @param privateKey 私钥（Base64或PEM格式）
     * @return 签名(Base64)
     */
    public static String sign(String data, String privateKey) {
        if (cn.hutool.core.util.StrUtil.isBlank(data)
                || cn.hutool.core.util.StrUtil.isBlank(privateKey)) {
            return null;
        }

        cn.hutool.crypto.asymmetric.Sign signer =
                cn.hutool.crypto.SecureUtil.sign(
                        cn.hutool.crypto.asymmetric.SignAlgorithm.SHA256withRSA,
                        privateKey,
                        null
                );

        byte[] signBytes = signer.sign(data, java.nio.charset.StandardCharsets.UTF_8);
        return cn.hutool.core.codec.Base64.encode(signBytes);
    }

    /**
     * 验证签名
     *
     * @param data      数据
     * @param signStr   签名
     * @param publicKey 公钥
     * @return true-通过
     */
    public static boolean verify(String data, String signStr, String publicKey) {
        cn.hutool.crypto.asymmetric.Sign sign =
                new cn.hutool.crypto.asymmetric.Sign(
                        cn.hutool.crypto.asymmetric.SignAlgorithm.SHA256withRSA,
                        null,
                        publicKey);
        return sign.verify(data.getBytes(), cn.hutool.core.codec.Base64.decode(signStr));
    }

    /**
     * HmacMD5
     *
     * @param data 数据
     * @param key  密钥
     * @return 结果
     */
    public static String hmacMd5(String data, String key) {
        if (cn.hutool.core.util.StrUtil.isBlank(data)
                || cn.hutool.core.util.StrUtil.isBlank(key)) {
            return null;
        }
        return cn.hutool.crypto.SecureUtil.hmacMd5(key).digestHex(data);
    }

    /**
     * HmacSHA256
     *
     * @param data 数据
     * @param key  密钥
     * @return 结果
     */
    public static String hmacSha256(String data, String key) {
        if (cn.hutool.core.util.StrUtil.isBlank(data)
                || cn.hutool.core.util.StrUtil.isBlank(key)) {
            return null;
        }
        return cn.hutool.crypto.SecureUtil.hmacSha256(key).digestHex(data);
    }

    /**
     * HmacSHA512
     *
     * @param data 数据
     * @param key  密钥
     * @return 结果
     */
    public static String hmacSha512(String data, String key) {
        if (cn.hutool.core.util.StrUtil.isBlank(data)
                || cn.hutool.core.util.StrUtil.isBlank(key)) {
            return null;
        }
        return cn.hutool.crypto.SecureUtil.hmac(
                cn.hutool.crypto.digest.HmacAlgorithm.HmacSHA512,
                key.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ).digestHex(data);
    }

    /**
     * 读取文件为字符串（UTF-8）
     *
     * @param path 文件路径
     * @return 内容
     */
    public static String readUtf8String(String path) {
        return cn.hutool.core.io.FileUtil.readUtf8String(path);
    }

    /**
     * 读取文件为字节数组
     *
     * @param path 文件路径
     * @return 字节数组
     */
    public static byte[] readBytes(String path) {
        return cn.hutool.core.io.FileUtil.readBytes(path);
    }

    /**
     * 写入字符串到文件（UTF-8）
     *
     * @param content 内容
     * @param path    路径
     */
    public static void writeUtf8String(String content, String path) {
        cn.hutool.core.io.FileUtil.writeUtf8String(content, path);
    }

    /**
     * 写入字节数组到文件
     *
     * @param data 数据
     * @param path 路径
     */
    public static void writeBytes(byte[] data, String path) {
        cn.hutool.core.io.FileUtil.writeBytes(data, path);
    }

    /**
     * 追加写入文件
     *
     * @param content 内容
     * @param path    路径
     */
    public static void appendUtf8String(String content, String path) {
        cn.hutool.core.io.FileUtil.appendUtf8String(content, path);
    }

    /**
     * 判断文件是否存在
     *
     * @param path 路径
     * @return true-存在
     */
    public static boolean exist(String path) {
        return cn.hutool.core.io.FileUtil.exist(path);
    }

    /**
     * 获取文件大小（字节）
     *
     * @param path 路径
     * @return 大小
     */
    public static long size(String path) {
        return cn.hutool.core.io.FileUtil.size(new java.io.File(path));
    }

    /**
     * 获取文件扩展名
     *
     * @param path 路径
     * @return 扩展名
     */
    public static String extName(String path) {
        return cn.hutool.core.io.FileUtil.extName(path);
    }

    /**
     * 获取文件名（不含扩展名）
     *
     * @param path 路径
     * @return 文件名
     */
    public static String mainName(String path) {
        return cn.hutool.core.io.FileUtil.mainName(path);
    }

    /**
     * 获取文件MIME类型
     *
     * @param path 路径
     * @return MIME类型
     */
    public static String getMimeType(String path) {
        return cn.hutool.core.io.FileUtil.getMimeType(path);
    }

    /**
     * 创建临时文件
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 文件
     */
    public static java.io.File createTempFile(String prefix, String suffix) {
        return cn.hutool.core.io.FileUtil.createTempFile(prefix, suffix, true);
    }

    /**
     * 创建临时目录
     *
     * @return 目录
     */
    public static java.io.File createTempDir() {
        return cn.hutool.core.io.FileUtil.mkdir(cn.hutool.core.util.IdUtil.fastSimpleUUID());
    }

    /**
     * 文件重命名
     *
     * @param path     原路径
     * @param newName  新名称
     * @param override 是否覆盖
     * @return 文件
     */
    public static java.io.File rename(String path, String newName, boolean override) {
        return cn.hutool.core.io.FileUtil.rename(new java.io.File(path), newName, override);
    }

    /**
     * 判断是否为文件
     *
     * @param path 路径
     * @return true-是文件
     */
    public static boolean isFile(String path) {
        return cn.hutool.core.io.FileUtil.isFile(path);
    }

    /**
     * 判断是否为目录
     *
     * @param path 路径
     * @return true-是目录
     */
    public static boolean isDirectory(String path) {
        return cn.hutool.core.io.FileUtil.isDirectory(path);
    }

    /**
     * 列出目录下所有文件
     *
     * @param path 路径
     * @return 文件列表
     */
    public static java.util.List<java.io.File> loopFiles(String path) {
        return cn.hutool.core.io.FileUtil.loopFiles(path);
    }

    /**
     * 文件路径拼接
     *
     * @param parent 父路径
     * @param paths  子路径
     * @return File
     */
    public static java.io.File file(String parent, String... paths) {
        if (cn.hutool.core.util.StrUtil.isBlank(parent)) {
            return cn.hutool.core.io.FileUtil.file(paths);
        }
        if (paths == null || paths.length == 0) {
            return cn.hutool.core.io.FileUtil.file(parent);
        }
        return cn.hutool.core.io.FileUtil.file(
                new java.io.File(parent),
                paths
        );
    }

    /**
     * 标准化路径
     *
     * @param path 路径
     * @return 标准路径
     */
    public static String normalize(String path) {
        return cn.hutool.core.io.FileUtil.normalize(path);
    }

    /**
     * 判断是否为绝对路径
     *
     * @param path 路径
     * @return true-绝对路径
     */
    public static boolean isAbsolutePath(String path) {
        return cn.hutool.core.io.FileUtil.isAbsolutePath(path);
    }

    /**
     * 获取用户目录
     *
     * @return 路径
     */
    public static String getUserHomePath() {
        return cn.hutool.system.SystemUtil.getUserInfo().getHomeDir();
    }

    /**
     * 发送GET请求
     *
     * @param url 地址
     * @return 响应内容
     */
    public static String httpGet(String url) {
        return cn.hutool.http.HttpUtil.get(url);
    }

    /**
     * 发送POST请求
     *
     * @param url    地址
     * @param params 参数
     * @return 响应内容
     */
    public static String httpPost(String url, java.util.Map<String, Object> params) {
        return cn.hutool.http.HttpUtil.post(url, params);
    }

    /**
     * 发送PUT请求
     *
     * @param url  地址
     * @param body 请求体
     * @return 响应内容
     */
    public static String httpPut(String url, String body) {
        return cn.hutool.http.HttpRequest.put(url).body(body).execute().body();
    }

    /**
     * 发送DELETE请求
     *
     * @param url 地址
     * @return 响应内容
     */
    public static String httpDelete(String url) {
        return cn.hutool.http.HttpRequest.delete(url).execute().body();
    }

    /**
     * 构建HTTP请求
     *
     * @param url 地址
     * @return HttpRequest
     */
    public static cn.hutool.http.HttpRequest createRequest(String url) {
        return cn.hutool.http.HttpRequest.of(url);
    }

    /**
     * 设置请求头
     *
     * @param request 请求
     * @param key     键
     * @param value   值
     * @return request
     */
    public static cn.hutool.http.HttpRequest header(cn.hutool.http.HttpRequest request,
                                                    String key,
                                                    String value) {
        return request.header(key, value);
    }

    /**
     * 设置表单参数
     *
     * @param request 请求
     * @param params  参数
     * @return request
     */
    public static cn.hutool.http.HttpRequest form(cn.hutool.http.HttpRequest request,
                                                  java.util.Map<String, Object> params) {
        return request.form(params);
    }

    /**
     * 上传文件
     *
     * @param url  地址
     * @param file 文件
     * @return 响应内容
     */
    public static String upload(String url, java.io.File file) {
        return cn.hutool.http.HttpRequest.post(url)
                .form("file", file)
                .execute()
                .body();
    }

    /**
     * 下载文件
     *
     * @param url      地址
     * @param destPath 保存路径
     */
    public static void download(String url, String destPath) {
        cn.hutool.http.HttpUtil.downloadFile(url, destPath);
    }

    /**
     * 获取响应状态码
     *
     * @param response 响应
     * @return 状态码
     */
    public static int getStatus(cn.hutool.http.HttpResponse response) {
        return response.getStatus();
    }

    /**
     * 获取当前线程ID
     *
     * @return ID
     */
    public static long threadId() {
        return Thread.currentThread().getId();
    }

    /**
     * 获取当前线程名称
     *
     * @return 名称
     */
    public static String threadName() {
        return Thread.currentThread().getName();
    }

    /**
     * 线程休眠
     *
     * @param millis 毫秒
     */
    public static void sleep(long millis) {
        cn.hutool.core.thread.ThreadUtil.sleep(millis);
    }

    /**
     * 创建线程池
     *
     * @param coreSize 核心线程数
     * @return 线程池
     */
    public static java.util.concurrent.ExecutorService newExecutor(int coreSize) {
        return cn.hutool.core.thread.ThreadUtil.newExecutor(coreSize);
    }

    /**
     * 提交异步任务
     *
     * @param executor 线程池
     * @param task     任务
     * @param <T>      返回类型
     * @return Future
     */
    public static <T> java.util.concurrent.Future<T> submit(
            java.util.concurrent.ExecutorService executor,
            java.util.concurrent.Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * 执行异步任务
     *
     * @param executor 线程池
     * @param task     任务
     */
    public static void execute(java.util.concurrent.ExecutorService executor, Runnable task) {
        executor.execute(task);
    }

    /**
     * 获取Future结果
     *
     * @param future Future
     * @param <T>    类型
     * @return 结果
     */
    public static <T> T getFuture(java.util.concurrent.Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 并发执行任务集合
     *
     * @param executor 线程池
     * @param tasks    任务集合
     * @param <T>      类型
     * @return Future集合
     */
    public static <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(
            java.util.concurrent.ExecutorService executor,
            java.util.Collection<java.util.concurrent.Callable<T>> tasks) {
        try {
            return executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置线程名称
     *
     * @param name 名称
     */
    public static void setThreadName(String name) {
        Thread.currentThread().setName(name);
    }

}
