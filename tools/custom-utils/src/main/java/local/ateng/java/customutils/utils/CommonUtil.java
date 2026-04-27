package local.ateng.java.customutils.utils;

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
        return java.lang.Math.abs(cn.hutool.core.date.LocalDateTimeUtil.between(start, end, java.time.temporal.ChronoUnit.DAYS));
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
        return java.lang.Math.abs(cn.hutool.core.date.LocalDateTimeUtil.between(start, end, java.time.temporal.ChronoUnit.HOURS));
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
        return java.lang.Math.abs(cn.hutool.core.date.LocalDateTimeUtil.between(start, end, java.time.temporal.ChronoUnit.MINUTES));
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
        return java.lang.Math.abs(cn.hutool.core.date.LocalDateTimeUtil.between(start, end, java.time.temporal.ChronoUnit.SECONDS));
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

    /**
     * 提供默认字符串值（仅空串时使用默认值）
     *
     * @param value        原字符串
     * @param defaultValue 默认值
     * @return 原字符串或默认值
     */
    public static String defaultIfEmpty(CharSequence value, String defaultValue) {
        return cn.hutool.core.util.StrUtil.emptyToDefault(value, defaultValue);
    }

    /**
     * 去除首尾空白，空串返回null
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String trimToNull(CharSequence value) {
        String result = cn.hutool.core.util.StrUtil.trim(value);
        return cn.hutool.core.util.StrUtil.isEmpty(result) ? null : result;
    }

    /**
     * 空串转null
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String emptyToNull(CharSequence value) {
        if (cn.hutool.core.util.StrUtil.isEmpty(value)) {
            return null;
        }
        return value.toString();
    }

    /**
     * 空白字符串转null
     *
     * @param value 字符串
     * @return 处理后的字符串
     */
    public static String blankToNull(CharSequence value) {
        if (cn.hutool.core.util.StrUtil.isBlank(value)) {
            return null;
        }
        return value.toString();
    }

    /**
     * 比较字符串是否相等
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return true-相等，false-不相等
     */
    public static boolean equals(CharSequence str1, CharSequence str2) {
        return cn.hutool.core.util.StrUtil.equals(str1, str2);
    }

    /**
     * 判断字符串是否等于任意一个字符串
     *
     * @param value       原字符串
     * @param candidates  候选字符串
     * @return true-匹配任意一个，false-不匹配
     */
    public static boolean equalsAny(CharSequence value, CharSequence... candidates) {
        return cn.hutool.core.util.StrUtil.equalsAny(value, candidates);
    }

    /**
     * 忽略大小写判断字符串是否等于任意一个字符串
     *
     * @param value      原字符串
     * @param candidates 候选字符串
     * @return true-匹配任意一个，false-不匹配
     */
    public static boolean equalsAnyIgnoreCase(CharSequence value, CharSequence... candidates) {
        return cn.hutool.core.util.StrUtil.equalsAnyIgnoreCase(value, candidates);
    }

    /**
     * 判断字符串是否包含指定内容
     *
     * @param value      原字符串
     * @param searchText 搜索内容
     * @return true-包含，false-不包含
     */
    public static boolean contains(CharSequence value, CharSequence searchText) {
        return cn.hutool.core.util.StrUtil.contains(value, searchText);
    }

    /**
     * 忽略大小写判断字符串是否包含指定内容
     *
     * @param value      原字符串
     * @param searchText 搜索内容
     * @return true-包含，false-不包含
     */
    public static boolean containsIgnoreCase(CharSequence value, CharSequence searchText) {
        return cn.hutool.core.util.StrUtil.containsIgnoreCase(value, searchText);
    }

    /**
     * 判断字符串是否以指定前缀开头
     *
     * @param value  原字符串
     * @param prefix 前缀
     * @return true-是，false-否
     */
    public static boolean startWith(CharSequence value, CharSequence prefix) {
        return cn.hutool.core.util.StrUtil.startWith(value, prefix);
    }

    /**
     * 忽略大小写判断字符串是否以指定前缀开头
     *
     * @param value  原字符串
     * @param prefix 前缀
     * @return true-是，false-否
     */
    public static boolean startWithIgnoreCase(CharSequence value, CharSequence prefix) {
        return cn.hutool.core.util.StrUtil.startWithIgnoreCase(value, prefix);
    }

    /**
     * 判断字符串是否以指定后缀结尾
     *
     * @param value  原字符串
     * @param suffix 后缀
     * @return true-是，false-否
     */
    public static boolean endWith(CharSequence value, CharSequence suffix) {
        return cn.hutool.core.util.StrUtil.endWith(value, suffix);
    }

    /**
     * 忽略大小写判断字符串是否以指定后缀结尾
     *
     * @param value  原字符串
     * @param suffix 后缀
     * @return true-是，false-否
     */
    public static boolean endWithIgnoreCase(CharSequence value, CharSequence suffix) {
        return cn.hutool.core.util.StrUtil.endWithIgnoreCase(value, suffix);
    }

    /**
     * 移除字符串前缀
     *
     * @param value  原字符串
     * @param prefix 前缀
     * @return 处理后的字符串
     */
    public static String removePrefix(CharSequence value, CharSequence prefix) {
        return cn.hutool.core.util.StrUtil.removePrefix(value, prefix);
    }

    /**
     * 移除字符串后缀
     *
     * @param value  原字符串
     * @param suffix 后缀
     * @return 处理后的字符串
     */
    public static String removeSuffix(CharSequence value, CharSequence suffix) {
        return cn.hutool.core.util.StrUtil.removeSuffix(value, suffix);
    }

    /**
     * 不存在前缀时添加前缀
     *
     * @param value  原字符串
     * @param prefix 前缀
     * @return 处理后的字符串
     */
    public static String addPrefixIfNot(CharSequence value, CharSequence prefix) {
        return cn.hutool.core.util.StrUtil.addPrefixIfNot(value, prefix);
    }

    /**
     * 不存在后缀时添加后缀
     *
     * @param value  原字符串
     * @param suffix 后缀
     * @return 处理后的字符串
     */
    public static String addSuffixIfNot(CharSequence value, CharSequence suffix) {
        return cn.hutool.core.util.StrUtil.addSuffixIfNot(value, suffix);
    }

    /**
     * 截取分隔符之前的字符串
     *
     * @param value     原字符串
     * @param separator 分隔符
     * @return 截取结果
     */
    public static String subBefore(CharSequence value, CharSequence separator) {
        return cn.hutool.core.util.StrUtil.subBefore(value, separator, false);
    }

    /**
     * 截取分隔符之后的字符串
     *
     * @param value     原字符串
     * @param separator 分隔符
     * @return 截取结果
     */
    public static String subAfter(CharSequence value, CharSequence separator) {
        return cn.hutool.core.util.StrUtil.subAfter(value, separator, false);
    }

    /**
     * 截取两个字符串之间的内容
     *
     * @param value  原字符串
     * @param before 前置字符串
     * @param after  后置字符串
     * @return 截取结果
     */
    public static String subBetween(CharSequence value, CharSequence before, CharSequence after) {
        return cn.hutool.core.util.StrUtil.subBetween(value, before, after);
    }

    /**
     * 按分隔符拆分并去除元素首尾空白
     *
     * @param value     原字符串
     * @param separator 分隔符
     * @return 拆分结果
     */
    public static java.util.List<String> splitTrim(CharSequence value, CharSequence separator) {
        return cn.hutool.core.util.StrUtil.splitTrim(value, separator);
    }

    /**
     * 按分隔符拆分为数组
     *
     * @param value     原字符串
     * @param separator 分隔符
     * @return 字符串数组
     */
    public static String[] splitToArray(CharSequence value, CharSequence separator) {
        java.util.List<String> list = cn.hutool.core.util.StrUtil.split(value, separator);
        return list.toArray(new String[0]);
    }

    /**
     * 替换字符串内容
     *
     * @param value       原字符串
     * @param searchText  被替换内容
     * @param replacement 替换内容
     * @return 替换结果
     */
    public static String replace(CharSequence value, CharSequence searchText, CharSequence replacement) {
        return cn.hutool.core.util.StrUtil.replace(value, searchText, replacement);
    }

    /**
     * 获取字符串长度
     *
     * @param value 字符串
     * @return 长度
     */
    public static int length(CharSequence value) {
        return cn.hutool.core.util.StrUtil.length(value);
    }

    /**
     * 获取UTF-8字节长度
     *
     * @param value 字符串
     * @return 字节长度
     */
    public static int byteLengthUtf8(CharSequence value) {
        if (value == null) {
            return 0;
        }
        return value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    /**
     * 判断字符串是否只包含数字
     *
     * @param value 字符串
     * @return true-是，false-否
     */
    public static boolean isNumeric(CharSequence value) {
        return cn.hutool.core.util.ReUtil.isMatch("^\\d+$", value);
    }

    /**
     * 判断字符串是否只包含字母
     *
     * @param value 字符串
     * @return true-是，false-否
     */
    public static boolean isAlpha(CharSequence value) {
        return cn.hutool.core.util.ReUtil.isMatch("^[A-Za-z]+$", value);
    }

    /**
     * 判断字符串是否只包含字母和数字
     *
     * @param value 字符串
     * @return true-是，false-否
     */
    public static boolean isAlphaNumeric(CharSequence value) {
        return cn.hutool.core.util.ReUtil.isMatch("^[A-Za-z0-9]+$", value);
    }

    /**
     * 生成指定范围内的随机整数
     *
     * @param min 最小值（包含）
     * @param max 最大值（不包含）
     * @return 随机整数
     */
    public static int randomInt(int min, int max) {
        return cn.hutool.core.util.RandomUtil.randomInt(min, max);
    }

    /**
     * 生成指定范围内的随机长整数
     *
     * @param min 最小值（包含）
     * @param max 最大值（不包含）
     * @return 随机长整数
     */
    public static long randomLong(long min, long max) {
        return cn.hutool.core.util.RandomUtil.randomLong(min, max);
    }

    /**
     * 生成随机布尔值
     *
     * @return 随机布尔值
     */
    public static boolean randomBoolean() {
        return cn.hutool.core.util.RandomUtil.randomBoolean();
    }

    /**
     * 生成随机字节数组
     *
     * @param length 长度
     * @return 字节数组
     */
    public static byte[] randomBytes(int length) {
        return cn.hutool.core.util.RandomUtil.randomBytes(length);
    }

    /**
     * 生成雪花ID
     *
     * @return 雪花ID
     */
    public static long snowflakeId() {
        return cn.hutool.core.util.IdUtil.getSnowflakeNextId();
    }

    /**
     * 生成雪花ID字符串
     *
     * @return 雪花ID字符串
     */
    public static String snowflakeIdStr() {
        return cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 生成ObjectId
     *
     * @return ObjectId字符串
     */
    public static String objectId() {
        return cn.hutool.core.util.IdUtil.objectId();
    }

    /**
     * 对象转Double
     *
     * @param value 对象
     * @return Double
     */
    public static Double toDouble(Object value) {
        return cn.hutool.core.convert.Convert.toDouble(value);
    }

    /**
     * 对象转Float
     *
     * @param value 对象
     * @return Float
     */
    public static Float toFloat(Object value) {
        return cn.hutool.core.convert.Convert.toFloat(value);
    }

    /**
     * 对象转Short
     *
     * @param value 对象
     * @return Short
     */
    public static Short toShort(Object value) {
        return cn.hutool.core.convert.Convert.toShort(value);
    }

    /**
     * 对象转Byte
     *
     * @param value 对象
     * @return Byte
     */
    public static Byte toByte(Object value) {
        return cn.hutool.core.convert.Convert.toByte(value);
    }

    /**
     * 数字相加
     *
     * @param values 数字
     * @return 计算结果
     */
    public static java.math.BigDecimal add(Number... values) {
        return cn.hutool.core.util.NumberUtil.add(values);
    }

    /**
     * 数字相减
     *
     * @param value1 数字1
     * @param value2 数字2
     * @return 计算结果
     */
    public static java.math.BigDecimal sub(Number value1, Number value2) {
        return cn.hutool.core.util.NumberUtil.sub(value1, value2);
    }

    /**
     * 数字相乘
     *
     * @param values 数字
     * @return 计算结果
     */
    public static java.math.BigDecimal mul(Number... values) {
        return cn.hutool.core.util.NumberUtil.mul(values);
    }

    /**
     * 数字相除
     *
     * @param value1 数字1
     * @param value2 数字2
     * @param scale  小数位
     * @return 计算结果
     */
    public static java.math.BigDecimal div(Number value1, Number value2, int scale) {
        return cn.hutool.core.util.NumberUtil.div(value1, value2, scale);
    }

    /**
     * 数字四舍五入
     *
     * @param value 数字
     * @param scale 小数位
     * @return 处理结果
     */
    public static java.math.BigDecimal round(Number value, int scale) {
        if (value == null) {
            return null;
        }
        return cn.hutool.core.util.NumberUtil.round(value.doubleValue(), scale);
    }

    /**
     * 按格式格式化数字
     *
     * @param pattern 格式
     * @param value   数字
     * @return 格式化结果
     */
    public static String decimalFormat(String pattern, Number value) {
        return cn.hutool.core.util.NumberUtil.decimalFormat(pattern, value);
    }

    /**
     * 比较两个数字大小
     *
     * @param value1 数字1
     * @param value2 数字2
     * @return -1/0/1
     */
    public static int compare(Number value1, Number value2) {
        java.math.BigDecimal decimal1 = cn.hutool.core.convert.Convert.toBigDecimal(value1);
        java.math.BigDecimal decimal2 = cn.hutool.core.convert.Convert.toBigDecimal(value2);
        return decimal1.compareTo(decimal2);
    }

    /**
     * 获取一天开始时间
     *
     * @param date 日期
     * @return 开始时间
     */
    public static java.util.Date beginOfDay(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.beginOfDay(date);
    }

    /**
     * 获取一天结束时间
     *
     * @param date 日期
     * @return 结束时间
     */
    public static java.util.Date endOfDay(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.endOfDay(date);
    }

    /**
     * 获取一周开始时间
     *
     * @param date 日期
     * @return 开始时间
     */
    public static java.util.Date beginOfWeek(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.beginOfWeek(date);
    }

    /**
     * 获取一周结束时间
     *
     * @param date 日期
     * @return 结束时间
     */
    public static java.util.Date endOfWeek(java.util.Date date) {
        return cn.hutool.core.date.DateUtil.endOfWeek(date);
    }

    /**
     * 获取指定日期偏移后的日期（按月）
     *
     * @param date   日期
     * @param offset 偏移月数
     * @return 日期
     */
    public static java.util.Date offsetMonth(java.util.Date date, int offset) {
        return cn.hutool.core.date.DateUtil.offsetMonth(date, offset);
    }

    /**
     * 获取指定日期偏移后的日期（按小时）
     *
     * @param date   日期
     * @param offset 偏移小时数
     * @return 日期
     */
    public static java.util.Date offsetHour(java.util.Date date, int offset) {
        return cn.hutool.core.date.DateUtil.offsetHour(date, offset);
    }

    /**
     * 获取指定日期偏移后的日期（按分钟）
     *
     * @param date   日期
     * @param offset 偏移分钟数
     * @return 日期
     */
    public static java.util.Date offsetMinute(java.util.Date date, int offset) {
        return cn.hutool.core.date.DateUtil.offsetMinute(date, offset);
    }

    /**
     * 获取指定日期偏移后的日期（按秒）
     *
     * @param date   日期
     * @param offset 偏移秒数
     * @return 日期
     */
    public static java.util.Date offsetSecond(java.util.Date date, int offset) {
        return cn.hutool.core.date.DateUtil.offsetSecond(date, offset);
    }

    /**
     * 按指定格式解析日期
     *
     * @param value   日期字符串
     * @param pattern 格式
     * @return 日期
     */
    public static java.util.Date parseDate(String value, String pattern) {
        if (cn.hutool.core.util.StrUtil.isBlank(value) || cn.hutool.core.util.StrUtil.isBlank(pattern)) {
            return null;
        }
        return cn.hutool.core.date.DateUtil.parse(value, pattern);
    }

    /**
     * 按指定格式格式化日期
     *
     * @param date    日期
     * @param pattern 格式
     * @return 字符串
     */
    public static String formatDate(java.util.Date date, String pattern) {
        if (date == null || cn.hutool.core.util.StrUtil.isBlank(pattern)) {
            return null;
        }
        return cn.hutool.core.date.DateUtil.format(date, pattern);
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前时间戳
     */
    public static long currentMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳（秒）
     *
     * @return 当前时间戳
     */
    public static long currentSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Date 转 LocalDate
     *
     * @param date 日期
     * @return LocalDate
     */
    public static java.time.LocalDate toLocalDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return cn.hutool.core.date.LocalDateTimeUtil.of(date).toLocalDate();
    }

    /**
     * LocalDate 转 Date
     *
     * @param localDate 日期
     * @return Date
     */
    public static java.util.Date toDate(java.time.LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return cn.hutool.core.date.DateUtil.date(
                localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        );
    }

    /**
     * 解析日期字符串为LocalDate
     *
     * @param value 日期字符串
     * @return LocalDate
     */
    public static java.time.LocalDate parseLocalDate(String value) {
        if (cn.hutool.core.util.StrUtil.isBlank(value)) {
            return null;
        }
        return java.time.LocalDate.parse(value);
    }

    /**
     * 按指定格式解析日期字符串为LocalDate
     *
     * @param value   日期字符串
     * @param pattern 格式
     * @return LocalDate
     */
    public static java.time.LocalDate parseLocalDate(String value, String pattern) {
        if (cn.hutool.core.util.StrUtil.isBlank(value) || cn.hutool.core.util.StrUtil.isBlank(pattern)) {
            return null;
        }
        return java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化LocalDate
     *
     * @param localDate 日期
     * @param pattern   格式
     * @return 字符串
     */
    public static String formatLocalDate(java.time.LocalDate localDate, String pattern) {
        if (localDate == null || cn.hutool.core.util.StrUtil.isBlank(pattern)) {
            return null;
        }
        return localDate.format(java.time.format.DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 获取日期时间当天开始时间
     *
     * @param dateTime 日期时间
     * @return 开始时间
     */
    public static java.time.LocalDateTime beginOfDay(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atStartOfDay();
    }

    /**
     * 获取日期时间当天结束时间
     *
     * @param dateTime 日期时间
     * @return 结束时间
     */
    public static java.time.LocalDateTime endOfDay(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atTime(java.time.LocalTime.MAX);
    }

    /**
     * 获取日期时间所在周开始时间
     *
     * @param dateTime 日期时间
     * @return 开始时间
     */
    public static java.time.LocalDateTime beginOfWeek(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        java.time.LocalDate monday = dateTime.toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        return monday.atStartOfDay();
    }

    /**
     * 获取日期时间所在周结束时间
     *
     * @param dateTime 日期时间
     * @return 结束时间
     */
    public static java.time.LocalDateTime endOfWeek(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        java.time.LocalDate sunday = dateTime.toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        return sunday.atTime(java.time.LocalTime.MAX);
    }

    /**
     * 判断日期时间是否早于目标时间
     *
     * @param dateTime 日期时间
     * @param target   目标时间
     * @return true-早于，false-不早于
     */
    public static boolean isBefore(java.time.LocalDateTime dateTime, java.time.LocalDateTime target) {
        return dateTime != null && target != null && dateTime.isBefore(target);
    }

    /**
     * 判断日期时间是否晚于目标时间
     *
     * @param dateTime 日期时间
     * @param target   目标时间
     * @return true-晚于，false-不晚于
     */
    public static boolean isAfter(java.time.LocalDateTime dateTime, java.time.LocalDateTime target) {
        return dateTime != null && target != null && dateTime.isAfter(target);
    }

    /**
     * 将日期时间转秒级时间戳
     *
     * @param dateTime 日期时间
     * @return 秒级时间戳
     */
    public static long toEpochSecond(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * 将秒级时间戳转日期时间
     *
     * @param epochSecond 秒级时间戳
     * @return 日期时间
     */
    public static java.time.LocalDateTime ofEpochSecondLocalDateTime(long epochSecond) {
        return java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(epochSecond),
                java.time.ZoneId.systemDefault()
        );
    }

    /**
     * 判断数组是否为空
     *
     * @param array 数组
     * @return true-为空，false-不为空
     */
    public static boolean isArrayEmpty(Object[] array) {
        return cn.hutool.core.util.ArrayUtil.isEmpty(array);
    }

    /**
     * 判断数组是否非空
     *
     * @param array 数组
     * @return true-非空，false-为空
     */
    public static boolean isArrayNotEmpty(Object[] array) {
        return cn.hutool.core.util.ArrayUtil.isNotEmpty(array);
    }

    /**
     * 获取数组长度
     *
     * @param array 数组
     * @return 长度
     */
    public static int arrayLength(Object[] array) {
        return cn.hutool.core.util.ArrayUtil.length(array);
    }

    /**
     * 判断数组是否包含指定元素
     *
     * @param array 数组
     * @param value 元素
     * @param <T>   类型
     * @return true-包含，false-不包含
     */
    public static <T> boolean arrayContains(T[] array, T value) {
        return cn.hutool.core.util.ArrayUtil.contains(array, value);
    }

    /**
     * 获取元素在数组中的位置
     *
     * @param array 数组
     * @param value 元素
     * @param <T>   类型
     * @return 索引，不存在返回-1
     */
    public static <T> int arrayIndexOf(T[] array, T value) {
        return cn.hutool.core.util.ArrayUtil.indexOf(array, value);
    }

    /**
     * 数组按分隔符拼接
     *
     * @param array     数组
     * @param delimiter 分隔符
     * @param <T>       类型
     * @return 拼接结果
     */
    public static <T> String joinArray(T[] array, CharSequence delimiter) {
        if (array == null) {
            return "";
        }
        return cn.hutool.core.util.StrUtil.join(delimiter, (Object[]) array);
    }

    /**
     * 创建ArrayList
     *
     * @param values 元素
     * @param <T>    类型
     * @return ArrayList
     */
    @SafeVarargs
    public static <T> java.util.List<T> newArrayList(T... values) {
        return cn.hutool.core.collection.CollUtil.newArrayList(values);
    }

    /**
     * 创建HashSet
     *
     * @param values 元素
     * @param <T>    类型
     * @return HashSet
     */
    @SafeVarargs
    public static <T> java.util.Set<T> newHashSet(T... values) {
        return cn.hutool.core.collection.CollUtil.newHashSet(values);
    }

    /**
     * 集合为null时返回空集合
     *
     * @param collection 集合
     * @param <T>        类型
     * @return 集合
     */
    public static <T> java.util.Collection<T> emptyIfNull(java.util.Collection<T> collection) {
        return collection == null ? java.util.Collections.emptyList() : collection;
    }

    /**
     * 安全获取列表元素，越界返回默认值
     *
     * @param list         列表
     * @param index        索引
     * @param defaultValue 默认值
     * @param <T>          类型
     * @return 元素或默认值
     */
    public static <T> T getOrDefault(java.util.List<T> list, int index, T defaultValue) {
        T value = cn.hutool.core.collection.CollUtil.get(list, index);
        return value == null ? defaultValue : value;
    }

    /**
     * 集合分片
     *
     * @param collection 集合
     * @param size       每片大小
     * @param <T>        类型
     * @return 分片结果
     */
    public static <T> java.util.List<java.util.List<T>> partition(java.util.Collection<T> collection, int size) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(collection) || size <= 0) {
            return new java.util.ArrayList<>();
        }
        return cn.hutool.core.collection.CollUtil.split(collection, size);
    }

    /**
     * 移除集合中的null元素
     *
     * @param collection 集合
     * @param <T>        类型
     * @return 处理后的列表
     */
    public static <T> java.util.List<T> removeNull(java.util.Collection<T> collection) {
        java.util.List<T> result = new java.util.ArrayList<>();
        if (cn.hutool.core.collection.CollUtil.isEmpty(collection)) {
            return result;
        }
        for (T item : collection) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 集合转Map
     *
     * @param collection  集合
     * @param keyMapper   key映射函数
     * @param valueMapper value映射函数
     * @param <T>         元素类型
     * @param <K>         key类型
     * @param <V>         value类型
     * @return Map
     */
    public static <T, K, V> java.util.Map<K, V> toMap(
            java.util.Collection<T> collection,
            java.util.function.Function<T, K> keyMapper,
            java.util.function.Function<T, V> valueMapper) {
        java.util.Map<K, V> result = new java.util.HashMap<>();
        if (collection == null || keyMapper == null || valueMapper == null) {
            return result;
        }
        for (T item : collection) {
            result.put(keyMapper.apply(item), valueMapper.apply(item));
        }
        return result;
    }

    /**
     * 统计满足条件的元素数量
     *
     * @param collection 集合
     * @param predicate  条件
     * @param <T>        类型
     * @return 数量
     */
    public static <T> long count(java.util.Collection<T> collection, java.util.function.Predicate<T> predicate) {
        if (collection == null || predicate == null) {
            return 0L;
        }
        return collection.stream().filter(predicate).count();
    }

    /**
     * 判断集合是否存在满足条件的元素
     *
     * @param collection 集合
     * @param predicate  条件
     * @param <T>        类型
     * @return true-存在，false-不存在
     */
    public static <T> boolean anyMatch(java.util.Collection<T> collection, java.util.function.Predicate<T> predicate) {
        return collection != null && predicate != null && collection.stream().anyMatch(predicate);
    }

    /**
     * 判断集合是否全部满足条件
     *
     * @param collection 集合
     * @param predicate  条件
     * @param <T>        类型
     * @return true-全部满足，false-不全部满足
     */
    public static <T> boolean allMatch(java.util.Collection<T> collection, java.util.function.Predicate<T> predicate) {
        return collection != null && predicate != null && collection.stream().allMatch(predicate);
    }

    /**
     * Map为null时返回空Map
     *
     * @param map Map
     * @param <K> key类型
     * @param <V> value类型
     * @return Map
     */
    public static <K, V> java.util.Map<K, V> emptyIfNull(java.util.Map<K, V> map) {
        return map == null ? java.util.Collections.emptyMap() : map;
    }

    /**
     * 创建HashMap
     *
     * @param <K> key类型
     * @param <V> value类型
     * @return HashMap
     */
    public static <K, V> java.util.HashMap<K, V> newHashMap() {
        return cn.hutool.core.map.MapUtil.newHashMap();
    }

    /**
     * 从Map中获取String值
     *
     * @param map Map
     * @param key 键
     * @return String值
     */
    public static String getStr(java.util.Map<?, ?> map, Object key) {
        return cn.hutool.core.map.MapUtil.getStr(map, key);
    }

    /**
     * 从Map中获取Integer值
     *
     * @param map Map
     * @param key 键
     * @return Integer值
     */
    public static Integer getInt(java.util.Map<?, ?> map, Object key) {
        return cn.hutool.core.map.MapUtil.getInt(map, key);
    }

    /**
     * 从Map中获取Long值
     *
     * @param map Map
     * @param key 键
     * @return Long值
     */
    public static Long getLong(java.util.Map<?, ?> map, Object key) {
        return cn.hutool.core.map.MapUtil.getLong(map, key);
    }

    /**
     * 从Map中获取Boolean值
     *
     * @param map Map
     * @param key 键
     * @return Boolean值
     */
    public static Boolean getBool(java.util.Map<?, ?> map, Object key) {
        return cn.hutool.core.map.MapUtil.getBool(map, key);
    }

    /**
     * 从Map中获取Date值
     *
     * @param map Map
     * @param key 键
     * @return Date值
     */
    public static java.util.Date getDate(java.util.Map<?, ?> map, Object key) {
        return cn.hutool.core.map.MapUtil.getDate(map, key);
    }

    /**
     * 值不为null时放入Map
     *
     * @param map   Map
     * @param key   键
     * @param value 值
     * @param <K>   key类型
     * @param <V>   value类型
     */
    public static <K, V> void putIfNotNull(java.util.Map<K, V> map, K key, V value) {
        if (map != null && value != null) {
            map.put(key, value);
        }
    }

    /**
     * 移除Map中值为null的元素
     *
     * @param map Map
     * @param <K> key类型
     * @param <V> value类型
     * @return 处理后的Map
     */
    public static <K, V> java.util.Map<K, V> removeNullValue(java.util.Map<K, V> map) {
        java.util.Map<K, V> result = new java.util.HashMap<>();
        if (cn.hutool.core.map.MapUtil.isEmpty(map)) {
            return result;
        }
        for (java.util.Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Map转查询参数字符串
     *
     * @param map Map
     * @return 查询参数字符串
     */
    public static String toQueryString(java.util.Map<?, ?> map) {
        if (cn.hutool.core.map.MapUtil.isEmpty(map)) {
            return "";
        }
        return cn.hutool.core.map.MapUtil.join(map, "&", "=", true);
    }

    /**
     * Bean属性拷贝，忽略null值
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        cn.hutool.core.bean.BeanUtil.copyProperties(
                source,
                target,
                cn.hutool.core.bean.copier.CopyOptions.create().ignoreNullValue()
        );
    }

    /**
     * 获取Bean属性值
     *
     * @param bean      Bean
     * @param fieldName 属性名
     * @return 属性值
     */
    public static Object getProperty(Object bean, String fieldName) {
        if (bean == null || cn.hutool.core.util.StrUtil.isBlank(fieldName)) {
            return null;
        }
        return cn.hutool.core.bean.BeanUtil.getProperty(bean, fieldName);
    }

    /**
     * 设置Bean属性值
     *
     * @param bean      Bean
     * @param fieldName 属性名
     * @param value     属性值
     */
    public static void setProperty(Object bean, String fieldName, Object value) {
        if (bean == null || cn.hutool.core.util.StrUtil.isBlank(fieldName)) {
            return;
        }
        cn.hutool.core.bean.BeanUtil.setProperty(bean, fieldName, value);
    }

    /**
     * Bean转Map，忽略null值
     *
     * @param bean Bean
     * @return Map
     */
    public static java.util.Map<String, Object> beanToMapIgnoreNull(Object bean) {
        return cn.hutool.core.bean.BeanUtil.beanToMap(bean, false, true);
    }

    /**
     * Bean转下划线Map
     *
     * @param bean Bean
     * @return Map
     */
    public static java.util.Map<String, Object> beanToUnderlineMap(Object bean) {
        return cn.hutool.core.bean.BeanUtil.beanToMap(bean, true, false);
    }

    /**
     * 判断字符串是否为JSON
     *
     * @param json JSON字符串
     * @return true-是，false-否
     */
    public static boolean isJson(String json) {
        if (cn.hutool.core.util.StrUtil.isBlank(json)) {
            return false;
        }
        try {
            cn.hutool.json.JSONUtil.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为JSON对象
     *
     * @param json JSON字符串
     * @return true-是，false-否
     */
    public static boolean isJsonObj(String json) {
        if (cn.hutool.core.util.StrUtil.isBlank(json)) {
            return false;
        }
        try {
            return cn.hutool.json.JSONUtil.parse(json) instanceof cn.hutool.json.JSONObject;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为JSON数组
     *
     * @param json JSON字符串
     * @return true-是，false-否
     */
    public static boolean isJsonArray(String json) {
        if (cn.hutool.core.util.StrUtil.isBlank(json)) {
            return false;
        }
        try {
            return cn.hutool.json.JSONUtil.parse(json) instanceof cn.hutool.json.JSONArray;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 对象转格式化JSON字符串
     *
     * @param obj 对象
     * @return 格式化JSON字符串
     */
    public static String toJsonPrettyStr(Object obj) {
        return cn.hutool.json.JSONUtil.toJsonPrettyStr(obj);
    }

    /**
     * JSON字符串转JSONObject
     *
     * @param json JSON字符串
     * @return JSONObject
     */
    public static cn.hutool.json.JSONObject parseJsonObj(String json) {
        return cn.hutool.json.JSONUtil.parseObj(json);
    }

    /**
     * JSON字符串转JSONArray
     *
     * @param json JSON字符串
     * @return JSONArray
     */
    public static cn.hutool.json.JSONArray parseJsonArray(String json) {
        return cn.hutool.json.JSONUtil.parseArray(json);
    }

    /**
     * 读取UTF-8文件为行列表
     *
     * @param path 文件路径
     * @return 行列表
     */
    public static java.util.List<String> readUtf8Lines(String path) {
        return cn.hutool.core.io.FileUtil.readUtf8Lines(path);
    }

    /**
     * 写入行列表到UTF-8文件
     *
     * @param lines 行列表
     * @param path  文件路径
     */
    public static void writeUtf8Lines(java.util.Collection<String> lines, String path) {
        cn.hutool.core.io.FileUtil.writeUtf8Lines(lines, path);
    }

    /**
     * 追加行列表到UTF-8文件
     *
     * @param lines 行列表
     * @param path  文件路径
     */
    public static void appendUtf8Lines(java.util.Collection<String> lines, String path) {
        cn.hutool.core.io.FileUtil.appendUtf8Lines(lines, path);
    }

    /**
     * 清空目录内容
     *
     * @param path 目录路径
     */
    public static void clean(String path) {
        cn.hutool.core.io.FileUtil.clean(path);
    }

    /**
     * 可读文件大小
     *
     * @param size 文件大小（字节）
     * @return 可读大小
     */
    public static String readableFileSize(long size) {
        return cn.hutool.core.io.FileUtil.readableFileSize(size);
    }

    /**
     * 获取绝对路径
     *
     * @param path 路径
     * @return 绝对路径
     */
    public static String getAbsolutePath(String path) {
        return cn.hutool.core.io.FileUtil.getAbsolutePath(path);
    }

    /**
     * 获取父级路径
     *
     * @param path  路径
     * @param level 层级
     * @return 父级路径
     */
    public static String getParent(String path, int level) {
        return cn.hutool.core.io.FileUtil.getParent(path, level);
    }

    /**
     * 获取文件最后修改时间
     *
     * @param path 文件路径
     * @return 最后修改时间
     */
    public static java.util.Date lastModified(String path) {
        java.io.File file = cn.hutool.core.io.FileUtil.file(path);
        if (!file.exists()) {
            return null;
        }
        return new java.util.Date(file.lastModified());
    }

    /**
     * 压缩文件或目录
     *
     * @param srcPath 源路径
     * @param zipPath 压缩文件路径
     * @return 压缩文件
     */
    public static java.io.File zip(String srcPath, String zipPath) {
        return cn.hutool.core.util.ZipUtil.zip(srcPath, zipPath, true);
    }

    /**
     * 解压文件
     *
     * @param zipPath 压缩文件路径
     * @param destDir 解压目录
     * @return 解压目录
     */
    public static java.io.File unzip(String zipPath, String destDir) {
        return cn.hutool.core.util.ZipUtil.unzip(zipPath, destDir);
    }

    /**
     * 获取文件MD5
     *
     * @param path 文件路径
     * @return MD5值
     */
    public static String fileMd5(String path) {
        return cn.hutool.crypto.digest.DigestUtil.md5Hex(cn.hutool.core.io.FileUtil.readBytes(path));
    }

    /**
     * 判断是否为中文
     *
     * @param value 字符串
     * @return true-是中文，false-否
     */
    public static boolean isChinese(CharSequence value) {
        return cn.hutool.core.util.ReUtil.isMatch("^[\\u4e00-\\u9fa5]+$", value);
    }

    /**
     * 判断是否包含中文
     *
     * @param value 字符串
     * @return true-包含，false-不包含
     */
    public static boolean containsChinese(CharSequence value) {
        return cn.hutool.core.util.ReUtil.contains("[\\u4e00-\\u9fa5]", value);
    }

    /**
     * 判断是否为邮政编码
     *
     * @param value 字符串
     * @return true-是，false-否
     */
    public static boolean isZipCode(CharSequence value) {
        return cn.hutool.core.util.ReUtil.isMatch("^\\d{6}$", value);
    }

    /**
     * 判断是否为金额格式
     *
     * @param value 字符串
     * @return true-是，false-否
     */
    public static boolean isMoney(CharSequence value) {
        return cn.hutool.core.util.ReUtil.isMatch("^(0|[1-9]\\d*)(\\.\\d{1,2})?$", value);
    }

    /**
     * 判断是否为车牌号
     *
     * @param value 字符串
     * @return true-是，false-否
     */
    public static boolean isPlateNumber(CharSequence value) {
        return cn.hutool.core.util.ReUtil.isMatch(
                "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9挂学警港澳]{5,6}$",
                value
        );
    }

    /**
     * Base64编码字节数组
     *
     * @param bytes 字节数组
     * @return 编码结果
     */
    public static String base64Encode(byte[] bytes) {
        return cn.hutool.core.codec.Base64.encode(bytes);
    }

    /**
     * Base64解码为字节数组
     *
     * @param value Base64字符串
     * @return 字节数组
     */
    public static byte[] base64DecodeBytes(CharSequence value) {
        if (value == null) {
            return new byte[0];
        }
        return cn.hutool.core.codec.Base64.decode(value);
    }

    /**
     * UTF-8 URL编码
     *
     * @param value 字符串
     * @return 编码结果
     */
    public static String urlEncodeUtf8(String value) {
        return cn.hutool.core.net.URLEncodeUtil.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * UTF-8 URL解码
     *
     * @param value 字符串
     * @return 解码结果
     */
    public static String urlDecodeUtf8(String value) {
        return cn.hutool.core.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * HmacSHA1
     *
     * @param data 数据
     * @param key  密钥
     * @return 结果
     */
    public static String hmacSha1(String data, String key) {
        if (cn.hutool.core.util.StrUtil.isBlank(data) || cn.hutool.core.util.StrUtil.isBlank(key)) {
            return null;
        }
        return cn.hutool.crypto.SecureUtil.hmacSha1(key).digestHex(data);
    }

    /**
     * 生成AES密钥
     *
     * @return Base64密钥
     */
    public static String generateAesKeyBase64() {
        javax.crypto.SecretKey key = cn.hutool.crypto.SecureUtil.generateKey(
                cn.hutool.crypto.symmetric.SymmetricAlgorithm.AES.getValue()
        );
        return cn.hutool.core.codec.Base64.encode(key.getEncoded());
    }

    /**
     * 安全比较字符串
     *
     * @param value1 字符串1
     * @param value2 字符串2
     * @return true-相等，false-不相等
     */
    public static boolean secureEquals(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return false;
        }
        byte[] bytes1 = value1.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bytes2 = value2.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(bytes1, bytes2);
    }

    /**
     * 发送带参数GET请求
     *
     * @param url    地址
     * @param params 参数
     * @return 响应内容
     */
    public static String httpGet(String url, java.util.Map<String, Object> params) {
        return cn.hutool.http.HttpRequest.get(url).form(params).execute().body();
    }

    /**
     * 发送带请求头和超时时间的GET请求
     *
     * @param url     地址
     * @param headers 请求头
     * @param timeout 超时时间（毫秒）
     * @return 响应内容
     */
    public static String httpGet(String url, java.util.Map<String, String> headers, int timeout) {
        cn.hutool.http.HttpRequest request = cn.hutool.http.HttpRequest.get(url).timeout(timeout);
        if (cn.hutool.core.map.MapUtil.isNotEmpty(headers)) {
            for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        return request.execute().body();
    }

    /**
     * 发送JSON POST请求
     *
     * @param url  地址
     * @param json JSON请求体
     * @return 响应内容
     */
    public static String httpPostJson(String url, String json) {
        return cn.hutool.http.HttpRequest.post(url)
                .body(json, "application/json;charset=UTF-8")
                .execute()
                .body();
    }

    /**
     * 发送带请求头和超时时间的JSON POST请求
     *
     * @param url     地址
     * @param json    JSON请求体
     * @param headers 请求头
     * @param timeout 超时时间（毫秒）
     * @return 响应内容
     */
    public static String httpPostJson(String url, String json, java.util.Map<String, String> headers, int timeout) {
        cn.hutool.http.HttpRequest request = cn.hutool.http.HttpRequest.post(url)
                .body(json, "application/json;charset=UTF-8")
                .timeout(timeout);
        if (cn.hutool.core.map.MapUtil.isNotEmpty(headers)) {
            for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        return request.execute().body();
    }

    /**
     * 获取响应头
     *
     * @param response   响应
     * @param headerName 响应头名称
     * @return 响应头值
     */
    public static String getHeader(cn.hutool.http.HttpResponse response, String headerName) {
        if (response == null || cn.hutool.core.util.StrUtil.isBlank(headerName)) {
            return null;
        }
        return response.header(headerName);
    }

    /**
     * 关闭线程池
     *
     * @param executor 线程池
     */
    public static void shutdown(java.util.concurrent.ExecutorService executor) {
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * 立即关闭线程池
     *
     * @param executor 线程池
     * @return 未执行任务
     */
    public static java.util.List<Runnable> shutdownNow(java.util.concurrent.ExecutorService executor) {
        if (executor == null) {
            return java.util.Collections.emptyList();
        }
        return executor.shutdownNow();
    }

    /**
     * 获取Future结果，支持超时
     *
     * @param future  Future
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param <T>     类型
     * @return 结果
     */
    public static <T> T getFuture(java.util.concurrent.Future<T> future,
                                  long timeout,
                                  java.util.concurrent.TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 异步执行无返回任务
     *
     * @param executor 线程池
     * @param task     任务
     * @return CompletableFuture
     */
    public static java.util.concurrent.CompletableFuture<Void> runAsync(
            java.util.concurrent.Executor executor,
            Runnable task) {
        return java.util.concurrent.CompletableFuture.runAsync(task, executor);
    }

    /**
     * 异步执行有返回任务
     *
     * @param executor 线程池
     * @param supplier 任务
     * @param <T>      返回类型
     * @return CompletableFuture
     */
    public static <T> java.util.concurrent.CompletableFuture<T> supplyAsync(
            java.util.concurrent.Executor executor,
            java.util.function.Supplier<T> supplier) {
        return java.util.concurrent.CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * 创建CountDownLatch
     *
     * @param count 计数
     * @return CountDownLatch
     */
    public static java.util.concurrent.CountDownLatch newCountDownLatch(int count) {
        return new java.util.concurrent.CountDownLatch(count);
    }
}
