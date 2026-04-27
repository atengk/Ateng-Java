package local.ateng.java.customutils.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 时间工具类
 * <p>
 * 提供常用的时间处理方法，基于 Java 8 时间 API
 * </p>
 *
 * @author Ateng
 * @since 2025-07-25
 */
public final class DateTimeUtil {

    /**
     * 默认日期时间格式：yyyy-MM-dd HH:mm:ss
     */
    public static final String DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认日期格式：yyyy-MM-dd
     */
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 默认时间格式：HH:mm:ss
     */
    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    /**
     * 禁止实例化工具类
     */
    private DateTimeUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 获取当前时间
     *
     * @return 当前 LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期
     *
     * @return 当前 LocalDate
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 获取当前时间（不包含日期）
     *
     * @return 当前 LocalTime
     */
    public static LocalTime currentTime() {
        return LocalTime.now();
    }

    /**
     * 格式化 LocalDateTime
     *
     * @param dateTime LocalDateTime 对象
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_DATE_TIME_PATTERN);
    }

    /**
     * 格式化 LocalDateTime
     *
     * @param dateTime LocalDateTime 对象
     * @param pattern  格式字符串
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化 LocalDate
     *
     * @param date LocalDate 对象
     * @return 格式化后的字符串
     */
    public static String format(LocalDate date) {
        if (date == null) {
            return null;
        }
        return format(date, DEFAULT_DATE_PATTERN);
    }

    /**
     * 格式化 LocalDate
     *
     * @param date    LocalDate 对象
     * @param pattern 格式字符串
     * @return 格式化后的字符串
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null || pattern == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析字符串为 LocalDateTime
     *
     * @param dateTimeStr 字符串
     * @return LocalDateTime 对象
     */
    public static LocalDateTime parse(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_PATTERN));
    }

    /**
     * 解析字符串为 LocalDateTime
     *
     * @param dateTimeStr 字符串
     * @param pattern     格式
     * @return LocalDateTime 对象
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || pattern == null) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 计算两个时间之间的秒数差
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 秒数差
     */
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 当前时间加指定分钟数
     *
     * @param minutes 分钟数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusMinutes(long minutes) {
        return LocalDateTime.now().plusMinutes(minutes);
    }

    /**
     * 当前时间加指定天数
     *
     * @param days 天数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusDays(long days) {
        return LocalDateTime.now().plusDays(days);
    }

    /**
     * 判断时间是否在某两个时间之间（含边界）
     *
     * @param target 目标时间
     * @param start  起始时间
     * @param end    结束时间
     * @return true 表示在范围内
     */
    public static boolean isBetween(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
        return target != null && start != null && end != null
                && !target.isBefore(start) && !target.isAfter(end);
    }

    /**
     * 判断两个时间段是否重叠
     *
     * @param start1 第一个时间段起始时间
     * @param end1   第一个时间段结束时间
     * @param start2 第二个时间段起始时间
     * @param end2   第二个时间段结束时间
     * @return true 表示时间段有重叠
     */
    public static boolean isOverlap(LocalDateTime start1, LocalDateTime end1,
                                    LocalDateTime start2, LocalDateTime end2) {
        return start1 != null && end1 != null && start2 != null && end2 != null
                && !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    /**
     * 判断两个时间是否在同一天
     *
     * @param dt1 时间1
     * @param dt2 时间2
     * @return 是否是同一天
     */
    public static boolean isSameDay(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null || dt2 == null) {
            return false;
        }
        return dt1.toLocalDate().isEqual(dt2.toLocalDate());
    }

    /**
     * 判断第一个时间是否早于第二个时间
     *
     * @param dt1 时间1
     * @param dt2 时间2
     * @return 是否早于
     */
    public static boolean isBefore(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null || dt2 == null) {
            return false;
        }
        return dt1.isBefore(dt2);
    }

    /**
     * 判断第一个时间是否晚于第二个时间
     *
     * @param dt1 时间1
     * @param dt2 时间2
     * @return 是否晚于
     */
    public static boolean isAfter(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null || dt2 == null) {
            return false;
        }
        return dt1.isAfter(dt2);
    }

    /**
     * 判断两个时间是否相等（精确到秒）
     *
     * @param dt1 时间1
     * @param dt2 时间2
     * @return 是否相等
     */
    public static boolean isEqualIgnoreNano(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null || dt2 == null) {
            return false;
        }
        return dt1.truncatedTo(ChronoUnit.SECONDS).isEqual(dt2.truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * 判断两个日期是否在同一个月
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 是否同月
     */
    public static boolean isSameMonth(LocalDate d1, LocalDate d2) {
        if (d1 == null || d2 == null) {
            return false;
        }
        return d1.getYear() == d2.getYear() && d1.getMonth() == d2.getMonth();
    }

    /**
     * 判断目标时间是否在指定时间段内（闭区间）
     *
     * @param target 目标时间
     * @param start  起始时间（含）
     * @param end    结束时间（含）
     * @return 是否在时间段内
     */
    public static boolean isTimeInRange(LocalTime target, LocalTime start, LocalTime end) {
        if (target == null || start == null || end == null) {
            return false;
        }
        return !target.isBefore(start) && !target.isAfter(end);
    }

    /**
     * 判断两个 ZonedDateTime 是否代表同一瞬间（忽略时区差异）
     *
     * @param zdt1 时间1
     * @param zdt2 时间2
     * @return 是否同一时刻
     */
    public static boolean isSameInstant(ZonedDateTime zdt1, ZonedDateTime zdt2) {
        if (zdt1 == null || zdt2 == null) {
            return false;
        }
        return zdt1.toInstant().equals(zdt2.toInstant());
    }

    /**
     * 判断两个 ZonedDateTime 是否在同一天（按其各自的本地日期）
     *
     * @param zdt1 时间1
     * @param zdt2 时间2
     * @return 是否为同一天
     */
    public static boolean isSameDay(ZonedDateTime zdt1, ZonedDateTime zdt2) {
        if (zdt1 == null || zdt2 == null) {
            return false;
        }
        return zdt1.toLocalDate().isEqual(zdt2.toLocalDate());
    }

    /**
     * 判断 LocalDateTime 是否为今天
     *
     * @param dateTime 要判断的时间
     * @return 是否是今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return LocalDate.now().isEqual(dateTime.toLocalDate());
    }

    /**
     * 将指定时间按单位向下对齐（去除小单位时间）
     *
     * @param time 时间
     * @param unit 对齐单位（如 ChronoUnit.MINUTES）
     * @return 向下对齐后的时间
     */
    public static LocalDateTime truncateTo(LocalDateTime time, ChronoUnit unit) {
        return time != null && unit != null ? time.truncatedTo(unit) : null;
    }

    /**
     * 将指定时间向上对齐到下一个单位起点
     *
     * @param time 时间
     * @param unit 单位（如 MINUTES、HOURS、DAYS）
     * @return 向上对齐后的时间
     */
    public static LocalDateTime ceilToNext(LocalDateTime time, ChronoUnit unit) {
        if (time == null || unit == null) {
            return null;
        }
        LocalDateTime truncated = time.truncatedTo(unit);
        return time.equals(truncated) ? time : truncated.plus(1, unit);
    }

    /**
     * 将时间从一个时区转换到另一个时区
     *
     * @param time     时间
     * @param fromZone 原时区
     * @param toZone   目标时区
     * @return 转换后的时间
     */
    public static LocalDateTime convertToZone(LocalDateTime time, ZoneId fromZone, ZoneId toZone) {
        if (time == null || fromZone == null || toZone == null) {
            return null;
        }
        return time.atZone(fromZone).withZoneSameInstant(toZone).toLocalDateTime();
    }

    /**
     * 获取指定日期的星期名称
     *
     * @param date    日期
     * @param chinese 是否返回中文（true = 星期一，false = MONDAY）
     * @return 星期字符串
     */
    public static String getDayOfWeekName(LocalDate date, boolean chinese) {
        if (date == null) {
            return null;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return chinese
                ? new String[]{"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"}[dayOfWeek.getValue() - 1]
                : dayOfWeek.name();
    }

    /**
     * 获取指定日期是第几季度
     *
     * @param date 日期
     * @return 季度值（1-4）
     */
    public static int getQuarter(LocalDate date) {
        return date != null ? (date.getMonthValue() - 1) / 3 + 1 : 0;
    }

    /**
     * 返回距离当前时间的中文描述（如：刚刚、5分钟前、2天前、3个月前、1年前）
     *
     * @param time 待描述的时间，非空
     * @return 距离当前时间的可读描述，time为null返回空字符串
     */
    public static String describeDurationFromNow(LocalDateTime time) {
        if (time == null) {
            return "";
        }

        /*
         * 时间单位换算（单位：秒）
         */
        final long second = 1L;
        final long minute = 60 * second;
        final long hour = 60 * minute;
        final long day = 24 * hour;

        /*
         * 阈值定义（单位：秒）
         * 小于等于justNowMaxSeconds秒，返回“刚刚”
         * 小于等于minutesMaxSeconds秒，返回“XX分钟前”
         * 小于等于hoursMaxSeconds秒，返回“XX小时前”
         * 小于等于daysMaxSeconds秒，返回“XX天前”
         * 小于monthsMaxCount个月，返回“XX个月前”
         * 其它，返回“XX年前”
         */
        final long justNowMaxSeconds = 59;
        final long minutesMaxSeconds = hour - 1;
        final long hoursMaxSeconds = day - 1;
        final long daysMaxSeconds = 30 * day - 1;
        final long monthsMaxCount = 12;

        LocalDateTime now = LocalDateTime.now();
        long secondsBetween = ChronoUnit.SECONDS.between(time, now);

        if (secondsBetween <= justNowMaxSeconds) {
            return "刚刚";
        }

        if (secondsBetween <= minutesMaxSeconds) {
            long minutes = secondsBetween / minute;
            return minutes + "分钟前";
        }

        if (secondsBetween <= hoursMaxSeconds) {
            long hours = secondsBetween / hour;
            return hours + "小时前";
        }

        if (secondsBetween <= daysMaxSeconds) {
            long days = secondsBetween / day;
            return days + "天前";
        }

        long monthsBetween = ChronoUnit.MONTHS.between(time, now);
        if (monthsBetween < monthsMaxCount) {
            return monthsBetween + "个月前";
        }

        long yearsBetween = ChronoUnit.YEARS.between(time, now);
        return yearsBetween + "年前";
    }

    /**
     * 获取当前时间戳（秒）
     *
     * @return 当前时间戳（秒）
     */
    public static long currentEpochSecond() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前时间戳（毫秒）
     */
    public static long currentEpochMilli() {
        return Instant.now().toEpochMilli();
    }

    /**
     * 将 LocalDateTime 转为时间戳（毫秒）
     *
     * @param dateTime LocalDateTime
     * @return 毫秒时间戳
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 从时间戳（毫秒）构造 LocalDateTime
     *
     * @param epochMilli 毫秒时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }

    /**
     * 将 java.util.Date 转为 LocalDateTime
     *
     * @param date Date 对象
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 将 LocalDateTime 转为 java.util.Date
     *
     * @param dateTime LocalDateTime 对象
     * @return Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取指定日期所在周的开始时间（周一 00:00:00）
     *
     * @param date 指定日期
     * @return 周开始时间
     */
    public static LocalDateTime getStartOfWeek(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(DayOfWeek.MONDAY).atStartOfDay();
    }

    /**
     * 获取指定日期所在周的结束时间（周日 23:59:59.999999999）
     *
     * @param date 指定日期
     * @return 周结束时间
     */
    public static LocalDateTime getEndOfWeek(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期所在月的开始时间（1号 00:00:00）
     *
     * @param date 指定日期
     * @return 月开始时间
     */
    public static LocalDateTime getStartOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfMonth(1).atStartOfDay();
    }

    /**
     * 获取指定日期所在月的结束时间（最后一天 23:59:59.999999999）
     *
     * @param date 指定日期
     * @return 月结束时间
     */
    public static LocalDateTime getEndOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX);
    }

    /**
     * 计算两个日期之间的天数差
     *
     * @param start 起始日期
     * @param end   结束日期
     * @return 相差天数
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期之间的月数差
     *
     * @param start 起始日期
     * @param end   结束日期
     * @return 相差月数
     */
    public static long monthsBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(start, end);
    }

    /**
     * 计算两个日期之间的年数差
     *
     * @param start 起始日期
     * @param end   结束日期
     * @return 相差年数
     */
    public static long yearsBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.YEARS.between(start, end);
    }

    /**
     * 校验字符串是否符合指定的时间格式
     *
     * @param dateTimeStr 字符串
     * @param pattern     格式
     * @return true 表示格式正确
     */
    public static boolean isValidFormat(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || pattern == null) {
            return false;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDateTime.parse(dateTimeStr, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 当前日期加指定天数
     *
     * @param days 天数（可为负数）
     * @return 计算后的 LocalDate
     */
    public static LocalDate plusDaysFromToday(long days) {
        return LocalDate.now().plusDays(days);
    }

    /**
     * 当前日期加指定周数
     *
     * @param weeks 周数（可为负数）
     * @return 计算后的 LocalDate
     */
    public static LocalDate plusWeeksFromToday(long weeks) {
        return LocalDate.now().plusWeeks(weeks);
    }

    /**
     * 当前日期加指定月数
     *
     * @param months 月数（可为负数）
     * @return 计算后的 LocalDate
     */
    public static LocalDate plusMonthsFromToday(long months) {
        return LocalDate.now().plusMonths(months);
    }

    /**
     * 当前日期加指定年数
     *
     * @param years 年数（可为负数）
     * @return 计算后的 LocalDate
     */
    public static LocalDate plusYearsFromToday(long years) {
        return LocalDate.now().plusYears(years);
    }

    /**
     * 计算两个日期之间的详细差异：年、月、日，顺序自动调整，结果总为正差异。
     * 返回格式类似 "2年3个月5天"，且省略值为0的部分。
     *
     * @param start 起始日期，非空
     * @param end   结束日期，非空
     * @return 差异描述字符串，如果参数为空，返回空字符串
     */
    public static String differenceDetail(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        // 如果起始日期晚于结束日期，则交换，保证start <= end
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        Period period = Period.between(start, end);

        List<String> parts = new ArrayList<>();

        if (period.getYears() != 0) {
            parts.add(period.getYears() + "年");
        }
        if (period.getMonths() != 0) {
            parts.add(period.getMonths() + "个月");
        }
        if (period.getDays() != 0) {
            parts.add(period.getDays() + "天");
        }

        // 如果全部为0，则表示同一天
        if (parts.isEmpty()) {
            return "0天";
        }

        return String.join("", parts);
    }

    /**
     * 判断是否为工作日（周一至周五）
     *
     * @param date 日期
     * @return true 表示工作日
     */
    public static boolean isWeekday(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /**
     * 判断是否为节假日（根据给定节假日列表）
     *
     * @param date        日期
     * @param holidayList 节假日列表（LocalDate集合）
     * @return true 表示是节假日
     */
    public static boolean isHoliday(LocalDate date, Set<LocalDate> holidayList) {
        if (date == null || holidayList == null) {
            return false;
        }
        return holidayList.contains(date);
    }

    /**
     * 判断是否为调休工作日（即：本是周末但被调为工作日）
     *
     * @param date        日期
     * @param workdayList 调休工作日列表
     * @return true 表示为调休工作日
     */
    public static boolean isMakeupWorkday(LocalDate date, Set<LocalDate> workdayList) {
        if (date == null || workdayList == null) {
            return false;
        }
        return workdayList.contains(date);
    }

    /**
     * 将 LocalDate 转换为 LocalDateTime（默认时间为 00:00:00）
     *
     * @param date LocalDate
     * @return LocalDateTime
     */
    public static LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    /**
     * 将 LocalDate 转换为 LocalDateTime（时间为当天最大值 23:59:59.999999999）
     *
     * @param date LocalDate
     * @return LocalDateTime
     */
    public static LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    /**
     * 从 LocalDateTime 中提取 LocalDate
     *
     * @param dateTime LocalDateTime
     * @return LocalDate
     */
    public static LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    /**
     * 从 LocalDateTime 中提取 LocalTime
     *
     * @param dateTime LocalDateTime
     * @return LocalTime
     */
    public static LocalTime toLocalTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalTime() : null;
    }

    /**
     * 内部通用方法：按指定单位和步长列出时间
     *
     * @param start 开始时间（含）
     * @param end   结束时间（含）
     * @param unit  单位（如 ChronoUnit.HOURS）
     * @param step  步长（如每2小时传2）
     * @return 时间字符串列表
     */
    public static List<LocalDateTime> listDateTimeRange(LocalDateTime start, LocalDateTime end,
                                                        ChronoUnit unit, long step) {
        if (start == null || end == null || unit == null || step <= 0 || start.isAfter(end)) {
            return Collections.emptyList();
        }
        List<LocalDateTime> result = new ArrayList<>();
        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            result.add(current);
            current = current.plus(step, unit);
        }
        return result;
    }

    /**
     * 内部通用方法：按指定单位和步长列出时间字符串
     *
     * @param start   开始时间（含）
     * @param end     结束时间（含）
     * @param unit    单位（如 ChronoUnit.HOURS）
     * @param step    步长（如每2小时传2）
     * @param pattern 格式化字符串
     * @return 时间字符串列表
     */
    public static List<String> listDateTimeRange(LocalDateTime start, LocalDateTime end,
                                                 ChronoUnit unit, long step, String pattern) {
        if (start == null || end == null || unit == null || pattern == null || step <= 0 || start.isAfter(end)) {
            return Collections.emptyList();
        }
        List<LocalDateTime> dateTimeList = listDateTimeRange(start, end, unit, step);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTimeList
                .stream()
                .map(formatter::format)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定日期范围内的每年时间点（格式化）
     *
     * @param start   开始日期（包含）
     * @param end     结束日期（包含）
     * @param pattern 格式化字符串（如 yyyy）
     * @return 每年的时间字符串列表
     */
    public static List<String> listYearlyRange(LocalDate start, LocalDate end, String pattern) {
        return listDateTimeRange(toStartOfDay(start), toStartOfDay(end), ChronoUnit.YEARS, 1, pattern);
    }

    /**
     * 获取指定日期范围内的每月时间点（格式化）
     *
     * @param start   开始日期（包含）
     * @param end     结束日期（包含）
     * @param pattern 格式化字符串（如 yyyy-MM）
     * @return 每月的时间字符串列表
     */
    public static List<String> listMonthlyRange(LocalDate start, LocalDate end, String pattern) {
        return listDateTimeRange(toStartOfDay(start), toStartOfDay(end), ChronoUnit.MONTHS, 1, pattern);
    }

    /**
     * 获取指定日期范围内的所有日期字符串（格式：yyyy-MM-dd）
     *
     * @param start   开始日期（包含）
     * @param end     结束日期（包含）
     * @param pattern 格式字符串，如 "yyyy-MM-dd"
     * @return 日期字符串列表
     */
    public static List<String> listDayRange(LocalDate start, LocalDate end, String pattern) {
        return listDateTimeRange(toStartOfDay(start), toStartOfDay(end), ChronoUnit.DAYS, 1, pattern);
    }

    /**
     * 获取指定时间范围内的每小时时间点（格式化）
     *
     * @param start   开始时间（含）
     * @param end     结束时间（含）
     * @param pattern 时间格式（如 yyyy-MM-dd HH）
     * @return 每小时的时间字符串列表
     */
    public static List<String> listHourlyRange(LocalDateTime start, LocalDateTime end, String pattern) {
        return listDateTimeRange(start, end, ChronoUnit.HOURS, 1, pattern);
    }

    /**
     * 获取指定时间范围内的每分钟时间点（格式化）
     *
     * @param start   开始时间（含）
     * @param end     结束时间（含）
     * @param pattern 时间格式（如 yyyy-MM-dd HH:mm）
     * @return 每分钟的时间字符串列表
     */
    public static List<String> listMinuteRange(LocalDateTime start, LocalDateTime end, String pattern) {
        return listDateTimeRange(start, end, ChronoUnit.MINUTES, 1, pattern);
    }

    /**
     * 获取指定时间范围内的每秒时间点（格式化）
     *
     * @param start   开始时间（含）
     * @param end     结束时间（含）
     * @param pattern 时间格式（如 yyyy-MM-dd HH:mm:ss）
     * @return 每秒的时间字符串列表
     */
    public static List<String> listSecondRange(LocalDateTime start, LocalDateTime end, String pattern) {
        return listDateTimeRange(start, end, ChronoUnit.SECONDS, 1, pattern);
    }

    /**
     * 解析字符串为 LocalDate
     *
     * @param dateStr 日期字符串
     * @return LocalDate 对象
     */
    public static LocalDate parseDate(String dateStr) {
        return parseDate(dateStr, DEFAULT_DATE_PATTERN);
    }

    /**
     * 解析字符串为 LocalDate
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return LocalDate 对象
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (dateStr == null || pattern == null) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析字符串为 LocalDateTime
     *
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime 对象
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return parseDateTime(dateTimeStr, DEFAULT_DATE_TIME_PATTERN);
    }

    /**
     * 解析字符串为 LocalDateTime
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     日期时间格式
     * @return LocalDateTime 对象
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || pattern == null) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 校验字符串是否符合指定日期格式
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return true 表示格式正确
     */
    public static boolean isValidDateFormat(String dateStr, String pattern) {
        if (dateStr == null || pattern == null) {
            return false;
        }
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验字符串是否符合指定日期时间格式
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     日期时间格式
     * @return true 表示格式正确
     */
    public static boolean isValidDateTimeFormat(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || pattern == null) {
            return false;
        }
        try {
            LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 指定日期加指定天数
     *
     * @param date 日期
     * @param days 天数（可为负数）
     * @return 计算后的 LocalDate
     */
    public static LocalDate plusDays(LocalDate date, long days) {
        return date != null ? date.plusDays(days) : null;
    }

    /**
     * 指定日期加指定周数
     *
     * @param date  日期
     * @param weeks 周数（可为负数）
     * @return 计算后的 LocalDate
     */
    public static LocalDate plusWeeks(LocalDate date, long weeks) {
        return date != null ? date.plusWeeks(weeks) : null;
    }

    /**
     * 指定日期加指定月数
     *
     * @param date   日期
     * @param months 月数（可为负数）
     * @return 计算后的 LocalDate
     */
    public static LocalDate plusMonths(LocalDate date, long months) {
        return date != null ? date.plusMonths(months) : null;
    }

    /**
     * 指定日期加指定年数
     *
     * @param date  日期
     * @param years 年数（可为负数）
     * @return 计算后的 LocalDate
     */
    public static LocalDate plusYears(LocalDate date, long years) {
        return date != null ? date.plusYears(years) : null;
    }

    /**
     * 指定时间加指定秒数
     *
     * @param dateTime 日期时间
     * @param seconds  秒数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusSeconds(LocalDateTime dateTime, long seconds) {
        return dateTime != null ? dateTime.plusSeconds(seconds) : null;
    }

    /**
     * 指定时间加指定分钟数
     *
     * @param dateTime 日期时间
     * @param minutes  分钟数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime != null ? dateTime.plusMinutes(minutes) : null;
    }

    /**
     * 指定时间加指定小时数
     *
     * @param dateTime 日期时间
     * @param hours    小时数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        return dateTime != null ? dateTime.plusHours(hours) : null;
    }

    /**
     * 指定时间加指定天数
     *
     * @param dateTime 日期时间
     * @param days     天数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime != null ? dateTime.plusDays(days) : null;
    }

    /**
     * 指定时间加指定周数
     *
     * @param dateTime 日期时间
     * @param weeks    周数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusWeeks(LocalDateTime dateTime, long weeks) {
        return dateTime != null ? dateTime.plusWeeks(weeks) : null;
    }

    /**
     * 指定时间加指定月数
     *
     * @param dateTime 日期时间
     * @param months   月数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusMonths(LocalDateTime dateTime, long months) {
        return dateTime != null ? dateTime.plusMonths(months) : null;
    }

    /**
     * 指定时间加指定年数
     *
     * @param dateTime 日期时间
     * @param years    年数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime plusYears(LocalDateTime dateTime, long years) {
        return dateTime != null ? dateTime.plusYears(years) : null;
    }

    /**
     * 获取指定日期当天开始时间
     *
     * @param dateTime 日期时间
     * @return 当天开始时间
     */
    public static LocalDateTime getStartOfDay(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().atStartOfDay() : null;
    }

    /**
     * 获取指定日期当天结束时间
     *
     * @param dateTime 日期时间
     * @return 当天结束时间
     */
    public static LocalDateTime getEndOfDay(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().atTime(LocalTime.MAX) : null;
    }

    /**
     * 获取指定日期所在年的开始时间
     *
     * @param date 日期
     * @return 年开始时间
     */
    public static LocalDateTime getStartOfYear(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfYear(1).atStartOfDay();
    }

    /**
     * 获取指定日期所在年的结束时间
     *
     * @param date 日期
     * @return 年结束时间
     */
    public static LocalDateTime getEndOfYear(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfYear(date.lengthOfYear()).atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期所在季度的开始时间
     *
     * @param date 日期
     * @return 季度开始时间
     */
    public static LocalDateTime getStartOfQuarter(LocalDate date) {
        if (date == null) {
            return null;
        }
        int firstMonthOfQuarter = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDate.of(date.getYear(), firstMonthOfQuarter, 1).atStartOfDay();
    }

    /**
     * 获取指定日期所在季度的结束时间
     *
     * @param date 日期
     * @return 季度结束时间
     */
    public static LocalDateTime getEndOfQuarter(LocalDate date) {
        if (date == null) {
            return null;
        }
        int firstMonthOfQuarter = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        LocalDate lastMonthOfQuarter = LocalDate.of(date.getYear(), firstMonthOfQuarter, 1).plusMonths(2);
        return lastMonthOfQuarter.withDayOfMonth(lastMonthOfQuarter.lengthOfMonth()).atTime(LocalTime.MAX);
    }

    /**
     * 获取指定时间所在小时的开始时间
     *
     * @param dateTime 日期时间
     * @return 小时开始时间
     */
    public static LocalDateTime getStartOfHour(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.truncatedTo(ChronoUnit.HOURS) : null;
    }

    /**
     * 获取指定时间所在小时的结束时间
     *
     * @param dateTime 日期时间
     * @return 小时结束时间
     */
    public static LocalDateTime getEndOfHour(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.truncatedTo(ChronoUnit.HOURS).plusHours(1).minusNanos(1);
    }

    /**
     * 获取指定时间所在分钟的开始时间
     *
     * @param dateTime 日期时间
     * @return 分钟开始时间
     */
    public static LocalDateTime getStartOfMinute(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.truncatedTo(ChronoUnit.MINUTES) : null;
    }

    /**
     * 获取指定时间所在分钟的结束时间
     *
     * @param dateTime 日期时间
     * @return 分钟结束时间
     */
    public static LocalDateTime getEndOfMinute(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1).minusNanos(1);
    }

    /**
     * 获取指定日期所在月第一天
     *
     * @param date 日期
     * @return 月第一天
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date != null ? date.withDayOfMonth(1) : null;
    }

    /**
     * 获取指定日期所在月最后一天
     *
     * @param date 日期
     * @return 月最后一天
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
        return date != null ? date.withDayOfMonth(date.lengthOfMonth()) : null;
    }

    /**
     * 获取指定日期所在周的周一日期
     *
     * @param date 日期
     * @return 周一日期
     */
    public static LocalDate getMondayOfWeek(LocalDate date) {
        return date != null ? date.with(DayOfWeek.MONDAY) : null;
    }

    /**
     * 获取指定日期所在周的周日日期
     *
     * @param date 日期
     * @return 周日日期
     */
    public static LocalDate getSundayOfWeek(LocalDate date) {
        return date != null ? date.with(DayOfWeek.SUNDAY) : null;
    }

    /**
     * 判断 LocalDate 是否为今天
     *
     * @param date 日期
     * @return 是否是今天
     */
    public static boolean isToday(LocalDate date) {
        return date != null && LocalDate.now().isEqual(date);
    }

    /**
     * 判断 LocalDate 是否为昨天
     *
     * @param date 日期
     * @return 是否是昨天
     */
    public static boolean isYesterday(LocalDate date) {
        return date != null && LocalDate.now().minusDays(1).isEqual(date);
    }

    /**
     * 判断 LocalDateTime 是否为昨天
     *
     * @param dateTime 日期时间
     * @return 是否是昨天
     */
    public static boolean isYesterday(LocalDateTime dateTime) {
        return dateTime != null && isYesterday(dateTime.toLocalDate());
    }

    /**
     * 判断 LocalDate 是否为明天
     *
     * @param date 日期
     * @return 是否是明天
     */
    public static boolean isTomorrow(LocalDate date) {
        return date != null && LocalDate.now().plusDays(1).isEqual(date);
    }

    /**
     * 判断 LocalDateTime 是否为明天
     *
     * @param dateTime 日期时间
     * @return 是否是明天
     */
    public static boolean isTomorrow(LocalDateTime dateTime) {
        return dateTime != null && isTomorrow(dateTime.toLocalDate());
    }

    /**
     * 判断是否为周末
     *
     * @param date 日期
     * @return true 表示周末
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * 判断是否为闰年
     *
     * @param date 日期
     * @return true 表示闰年
     */
    public static boolean isLeapYear(LocalDate date) {
        return date != null && date.isLeapYear();
    }

    /**
     * 判断两个日期是否在同一年
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 是否同年
     */
    public static boolean isSameYear(LocalDate d1, LocalDate d2) {
        return d1 != null && d2 != null && d1.getYear() == d2.getYear();
    }

    /**
     * 判断两个日期是否在同一季度
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 是否同季度
     */
    public static boolean isSameQuarter(LocalDate d1, LocalDate d2) {
        return d1 != null && d2 != null
                && d1.getYear() == d2.getYear()
                && getQuarter(d1) == getQuarter(d2);
    }

    /**
     * 判断两个日期是否在同一 ISO 周
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 是否同一周
     */
    public static boolean isSameWeek(LocalDate d1, LocalDate d2) {
        if (d1 == null || d2 == null) {
            return false;
        }
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
        return d1.get(weekFields.weekBasedYear()) == d2.get(weekFields.weekBasedYear())
                && d1.get(weekFields.weekOfWeekBasedYear()) == d2.get(weekFields.weekOfWeekBasedYear());
    }

    /**
     * 判断目标日期是否在指定日期范围内（含边界）
     *
     * @param target 目标日期
     * @param start  开始日期
     * @param end    结束日期
     * @return true 表示在范围内
     */
    public static boolean isBetween(LocalDate target, LocalDate start, LocalDate end) {
        return target != null && start != null && end != null
                && !target.isBefore(start) && !target.isAfter(end);
    }

    /**
     * 判断日期是否早于另一个日期
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 是否早于
     */
    public static boolean isBefore(LocalDate d1, LocalDate d2) {
        return d1 != null && d2 != null && d1.isBefore(d2);
    }

    /**
     * 判断日期是否晚于另一个日期
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 是否晚于
     */
    public static boolean isAfter(LocalDate d1, LocalDate d2) {
        return d1 != null && d2 != null && d1.isAfter(d2);
    }

    /**
     * 判断两个日期是否相等
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 是否相等
     */
    public static boolean isEqual(LocalDate d1, LocalDate d2) {
        return d1 != null && d2 != null && d1.isEqual(d2);
    }

    /**
     * 计算两个时间之间的毫秒数差
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 毫秒数差
     */
    public static long millisBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return ChronoUnit.MILLIS.between(start, end);
    }

    /**
     * 计算两个时间之间的分钟数差
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 分钟数差
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个时间之间的小时数差
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 小时数差
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个时间之间的天数差
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 天数差
     */
    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 将 LocalDateTime 转为时间戳（秒）
     *
     * @param dateTime LocalDateTime
     * @return 秒级时间戳
     */
    public static long toEpochSecond(LocalDateTime dateTime) {
        return toEpochSecond(dateTime, ZoneId.systemDefault());
    }

    /**
     * 将 LocalDateTime 按指定时区转为时间戳（秒）
     *
     * @param dateTime LocalDateTime
     * @param zoneId   时区
     * @return 秒级时间戳
     */
    public static long toEpochSecond(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null || zoneId == null) {
            return 0L;
        }
        return dateTime.atZone(zoneId).toEpochSecond();
    }

    /**
     * 将 LocalDateTime 按指定时区转为时间戳（毫秒）
     *
     * @param dateTime LocalDateTime
     * @param zoneId   时区
     * @return 毫秒时间戳
     */
    public static long toEpochMilli(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null || zoneId == null) {
            return 0L;
        }
        return dateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 从时间戳（秒）构造 LocalDateTime
     *
     * @param epochSecond 秒级时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime fromEpochSecond(long epochSecond) {
        return fromEpochSecond(epochSecond, ZoneId.systemDefault());
    }

    /**
     * 从时间戳（秒）按指定时区构造 LocalDateTime
     *
     * @param epochSecond 秒级时间戳
     * @param zoneId      时区
     * @return LocalDateTime
     */
    public static LocalDateTime fromEpochSecond(long epochSecond, ZoneId zoneId) {
        if (zoneId == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), zoneId);
    }

    /**
     * 从时间戳（毫秒）按指定时区构造 LocalDateTime
     *
     * @param epochMilli 毫秒时间戳
     * @param zoneId     时区
     * @return LocalDateTime
     */
    public static LocalDateTime fromEpochMilli(long epochMilli, ZoneId zoneId) {
        if (zoneId == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), zoneId);
    }

    /**
     * 将 java.util.Date 转为 LocalDate
     *
     * @param date Date 对象
     * @return LocalDate
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 将 LocalDate 转为 java.util.Date（当天开始时间）
     *
     * @param date LocalDate 对象
     * @return Date
     */
    public static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 合并 LocalDate 和 LocalTime 为 LocalDateTime
     *
     * @param date 日期
     * @param time 时间
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return null;
        }
        return LocalDateTime.of(date, time);
    }

    /**
     * 按指定时间构造 LocalDateTime
     *
     * @param date   日期
     * @param hour   小时
     * @param minute 分钟
     * @param second 秒
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(LocalDate date, int hour, int minute, int second) {
        if (date == null) {
            return null;
        }
        return date.atTime(hour, minute, second);
    }

    /**
     * 获取两个日期中较早的日期
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 较早日期
     */
    public static LocalDate min(LocalDate d1, LocalDate d2) {
        if (d1 == null) {
            return d2;
        }
        if (d2 == null) {
            return d1;
        }
        return d1.isBefore(d2) ? d1 : d2;
    }

    /**
     * 获取两个日期中较晚的日期
     *
     * @param d1 日期1
     * @param d2 日期2
     * @return 较晚日期
     */
    public static LocalDate max(LocalDate d1, LocalDate d2) {
        if (d1 == null) {
            return d2;
        }
        if (d2 == null) {
            return d1;
        }
        return d1.isAfter(d2) ? d1 : d2;
    }

    /**
     * 获取两个时间中较早的时间
     *
     * @param dt1 时间1
     * @param dt2 时间2
     * @return 较早时间
     */
    public static LocalDateTime min(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null) {
            return dt2;
        }
        if (dt2 == null) {
            return dt1;
        }
        return dt1.isBefore(dt2) ? dt1 : dt2;
    }

    /**
     * 获取两个时间中较晚的时间
     *
     * @param dt1 时间1
     * @param dt2 时间2
     * @return 较晚时间
     */
    public static LocalDateTime max(LocalDateTime dt1, LocalDateTime dt2) {
        if (dt1 == null) {
            return dt2;
        }
        if (dt2 == null) {
            return dt1;
        }
        return dt1.isAfter(dt2) ? dt1 : dt2;
    }

    /**
     * 将日期限制在指定范围内
     *
     * @param date  目标日期
     * @param start 开始日期
     * @param end   结束日期
     * @return 限制后的日期
     */
    public static LocalDate clamp(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null) {
            return null;
        }
        if (start != null && date.isBefore(start)) {
            return start;
        }
        if (end != null && date.isAfter(end)) {
            return end;
        }
        return date;
    }

    /**
     * 将时间限制在指定范围内
     *
     * @param dateTime 目标时间
     * @param start    开始时间
     * @param end      结束时间
     * @return 限制后的时间
     */
    public static LocalDateTime clamp(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        if (dateTime == null) {
            return null;
        }
        if (start != null && dateTime.isBefore(start)) {
            return start;
        }
        if (end != null && dateTime.isAfter(end)) {
            return end;
        }
        return dateTime;
    }

    /**
     * 获取指定日期范围内的所有日期
     *
     * @param start 开始日期（含）
     * @param end   结束日期（含）
     * @return 日期列表
     */
    public static List<LocalDate> listDateRange(LocalDate start, LocalDate end) {
        return listDateRange(start, end, 1);
    }

    /**
     * 按指定步长获取日期范围内的日期
     *
     * @param start 开始日期（含）
     * @param end   结束日期（含）
     * @param step  步长，单位：天
     * @return 日期列表
     */
    public static List<LocalDate> listDateRange(LocalDate start, LocalDate end, long step) {
        if (start == null || end == null || step <= 0 || start.isAfter(end)) {
            return Collections.emptyList();
        }
        List<LocalDate> result = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            result.add(current);
            current = current.plusDays(step);
        }
        return result;
    }

    /**
     * 获取两个日期范围的交集
     *
     * @param start1 第一个日期范围开始日期
     * @param end1   第一个日期范围结束日期
     * @param start2 第二个日期范围开始日期
     * @param end2   第二个日期范围结束日期
     * @return 日期范围交集，空数组表示无交集
     */
    public static LocalDate[] intersection(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null || start1.isAfter(end1) || start2.isAfter(end2)) {
            return new LocalDate[0];
        }
        LocalDate start = max(start1, start2);
        LocalDate end = min(end1, end2);
        return start.isAfter(end) ? new LocalDate[0] : new LocalDate[]{start, end};
    }

    /**
     * 获取两个时间范围的交集
     *
     * @param start1 第一个时间范围开始时间
     * @param end1   第一个时间范围结束时间
     * @param start2 第二个时间范围开始时间
     * @param end2   第二个时间范围结束时间
     * @return 时间范围交集，空数组表示无交集
     */
    public static LocalDateTime[] intersection(LocalDateTime start1, LocalDateTime end1,
                                               LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null || start1.isAfter(end1) || start2.isAfter(end2)) {
            return new LocalDateTime[0];
        }
        LocalDateTime start = max(start1, start2);
        LocalDateTime end = min(end1, end2);
        return start.isAfter(end) ? new LocalDateTime[0] : new LocalDateTime[]{start, end};
    }

}
