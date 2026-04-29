package io.github.atengk.utils;


import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 日期时间工具类。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class DateTimeUtil {

    public static final String NORM_DATE_PATTERN = "yyyy-MM-dd";
    public static final String NORM_TIME_PATTERN = "HH:mm:ss";
    public static final String NORM_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String NORM_MINUTE_PATTERN = "yyyy-MM-dd HH:mm";
    public static final String PURE_DATE_PATTERN = "yyyyMMdd";
    public static final String PURE_DATETIME_PATTERN = "yyyyMMddHHmmss";
    public static final String ISO_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_PATH_PATTERN = "yyyy/MM/dd";

    public static final DateTimeFormatter NORM_DATE_FORMATTER = DateTimeFormatter.ofPattern(NORM_DATE_PATTERN);
    public static final DateTimeFormatter NORM_TIME_FORMATTER = DateTimeFormatter.ofPattern(NORM_TIME_PATTERN);
    public static final DateTimeFormatter NORM_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(NORM_DATETIME_PATTERN);
    public static final DateTimeFormatter NORM_MINUTE_FORMATTER = DateTimeFormatter.ofPattern(NORM_MINUTE_PATTERN);
    public static final DateTimeFormatter PURE_DATE_FORMATTER = DateTimeFormatter.ofPattern(PURE_DATE_PATTERN);
    public static final DateTimeFormatter PURE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(PURE_DATETIME_PATTERN);
    public static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATETIME_PATTERN);
    public static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATH_PATTERN);

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final long EPOCH_SECOND_MAX_ABS = 99_999_999_999L;
    private static final List<DateTimeFormatter> SMART_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            ISO_DATETIME_FORMATTER,
            NORM_DATETIME_FORMATTER,
            NORM_MINUTE_FORMATTER,
            PURE_DATETIME_FORMATTER
    );
    private static final List<DateTimeFormatter> SMART_DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            NORM_DATE_FORMATTER,
            PURE_DATE_FORMATTER
    );

    private DateTimeUtil() {
    }

    /**
     * 获取系统默认时区的当前日期。
     *
     * @return 当前日期
     */
    public static LocalDate nowDate() {
        return LocalDate.now();
    }

    /**
     * 使用指定时钟获取当前日期。
     *
     * @param clock 时钟
     * @return 当前日期
     */
    public static LocalDate nowDate(Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        return LocalDate.now(clock);
    }

    /**
     * 获取系统默认时区的当前时间。
     *
     * @return 当前时间
     */
    public static LocalTime nowTime() {
        return LocalTime.now();
    }

    /**
     * 使用指定时钟获取当前时间。
     *
     * @param clock 时钟
     * @return 当前时间
     */
    public static LocalTime nowTime(Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        return LocalTime.now(clock);
    }

    /**
     * 获取系统默认时区的当前日期时间。
     *
     * @return 当前日期时间
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 使用指定时区获取当前日期时间。
     *
     * @param zoneId 时区
     * @return 当前日期时间
     */
    public static LocalDateTime nowDateTime(ZoneId zoneId) {
        requireNonNull(zoneId, "时区不能为空");
        return LocalDateTime.now(zoneId);
    }

    /**
     * 使用指定时钟获取当前日期时间。
     *
     * @param clock 时钟
     * @return 当前日期时间
     */
    public static LocalDateTime nowDateTime(Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        return LocalDateTime.now(clock);
    }

    /**
     * 获取当前毫秒时间戳。
     *
     * @return 毫秒时间戳
     */
    public static long nowEpochMilli() {
        return Instant.now().toEpochMilli();
    }

    /**
     * 使用指定时钟获取当前毫秒时间戳。
     *
     * @param clock 时钟
     * @return 毫秒时间戳
     */
    public static long nowEpochMilli(Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        return Instant.now(clock).toEpochMilli();
    }

    /**
     * 获取当前秒级时间戳。
     *
     * @return 秒级时间戳
     */
    public static long nowEpochSecond() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 使用指定时钟获取当前秒级时间戳。
     *
     * @param clock 时钟
     * @return 秒级时间戳
     */
    public static long nowEpochSecond(Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        return Instant.now(clock).getEpochSecond();
    }

    /**
     * 格式化日期时间。
     *
     * @param dateTime 日期时间
     * @param pattern 格式
     * @return 格式化结果
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.format(formatter(pattern));
    }

    /**
     * 格式化日期。
     *
     * @param date 日期
     * @param pattern 格式
     * @return 格式化结果
     */
    public static String format(LocalDate date, String pattern) {
        requireNonNull(date, "日期不能为空");
        return date.format(formatter(pattern));
    }

    /**
     * 格式化时间。
     *
     * @param time 时间
     * @param pattern 格式
     * @return 格式化结果
     */
    public static String format(LocalTime time, String pattern) {
        requireNonNull(time, "时间不能为空");
        return time.format(formatter(pattern));
    }

    /**
     * 按指定格式解析日期时间。
     *
     * @param text 文本
     * @param pattern 格式
     * @return 日期时间
     */
    public static LocalDateTime parseDateTime(String text, String pattern) {
        return LocalDateTime.parse(requireText(text, "日期时间文本不能为空"), formatter(pattern));
    }

    /**
     * 按指定格式解析日期。
     *
     * @param text 文本
     * @param pattern 格式
     * @return 日期
     */
    public static LocalDate parseDate(String text, String pattern) {
        return LocalDate.parse(requireText(text, "日期文本不能为空"), formatter(pattern));
    }

    /**
     * 按指定格式解析时间。
     *
     * @param text 文本
     * @param pattern 格式
     * @return 时间
     */
    public static LocalTime parseTime(String text, String pattern) {
        return LocalTime.parse(requireText(text, "时间文本不能为空"), formatter(pattern));
    }

    /**
     * 智能解析常见日期时间格式，日期格式会补齐为当天开始时间。
     *
     * @param text 文本
     * @return 日期时间
     */
    public static LocalDateTime parseDateTimeSmart(String text) {
        String value = requireText(text, "日期时间文本不能为空");
        for (DateTimeFormatter dateTimeFormatter : SMART_DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, dateTimeFormatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个格式
            }
        }
        for (DateTimeFormatter dateFormatter : SMART_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, dateFormatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // 尝试下一个格式
            }
        }
        throw new DateTimeParseException("无法解析日期时间", value, 0);
    }

    /**
     * 判断文本是否符合指定日期格式。
     *
     * @param text 文本
     * @param pattern 格式
     * @return 是否符合
     */
    public static boolean isDateFormat(String text, String pattern) {
        if (isBlank(text) || isBlank(pattern)) {
            return false;
        }
        try {
            DateTimeFormatter dateTimeFormatter = formatter(pattern);
            LocalDateTime.parse(text, dateTimeFormatter);
            return true;
        } catch (DateTimeException ignored) {
            try {
                LocalDate.parse(text, formatter(pattern));
                return true;
            } catch (DateTimeException ignoredAgain) {
                return false;
            }
        }
    }

    /**
     * 将 Date 转为 LocalDateTime。
     *
     * @param date 日期
     * @return 日期时间
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        requireNonNull(date, "Date不能为空");
        return toLocalDateTime(date.toInstant());
    }

    /**
     * 将 LocalDateTime 转为 Date。
     *
     * @param dateTime 日期时间
     * @return Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        return Date.from(toInstant(dateTime));
    }

    /**
     * 将 Instant 转为系统默认时区的 LocalDateTime。
     *
     * @param instant 时间点
     * @return 日期时间
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        requireNonNull(instant, "Instant不能为空");
        return LocalDateTime.ofInstant(instant, systemZone());
    }

    /**
     * 将 Instant 转为指定时区的 LocalDateTime。
     *
     * @param instant 时间点
     * @param zoneId 时区
     * @return 日期时间
     */
    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {
        requireNonNull(instant, "Instant不能为空");
        requireNonNull(zoneId, "时区不能为空");
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    /**
     * 将毫秒时间戳转为系统默认时区的 LocalDateTime。
     *
     * @param epochMilli 毫秒时间戳
     * @return 日期时间
     */
    public static LocalDateTime toLocalDateTime(long epochMilli) {
        return toLocalDateTime(Instant.ofEpochMilli(epochMilli));
    }

    /**
     * 将 LocalDateTime 转为系统默认时区的 Instant。
     *
     * @param dateTime 日期时间
     * @return 时间点
     */
    public static Instant toInstant(LocalDateTime dateTime) {
        return toInstant(dateTime, systemZone());
    }

    /**
     * 将 LocalDateTime 转为指定时区的 Instant。
     *
     * @param dateTime 日期时间
     * @param zoneId 时区
     * @return 时间点
     */
    public static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {
        requireNonNull(dateTime, "日期时间不能为空");
        requireNonNull(zoneId, "时区不能为空");
        return dateTime.atZone(zoneId).toInstant();
    }

    /**
     * 将 LocalDateTime 转为系统默认时区的毫秒时间戳。
     *
     * @param dateTime 日期时间
     * @return 毫秒时间戳
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        return toInstant(dateTime).toEpochMilli();
    }

    /**
     * 获取日期当天开始时间。
     *
     * @param date 日期
     * @return 当天开始时间
     */
    public static LocalDateTime toStartDateTime(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return date.atStartOfDay();
    }

    /**
     * 获取日期当天结束时间。
     *
     * @param date 日期
     * @return 当天结束时间
     */
    public static LocalDateTime toEndDateTime(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 获取月份开始时间。
     *
     * @param yearMonth 年月
     * @return 月份开始时间
     */
    public static LocalDateTime toMonthStart(YearMonth yearMonth) {
        requireNonNull(yearMonth, "年月不能为空");
        return yearMonth.atDay(1).atStartOfDay();
    }

    /**
     * 获取月份结束时间。
     *
     * @param yearMonth 年月
     * @return 月份结束时间
     */
    public static LocalDateTime toMonthEnd(YearMonth yearMonth) {
        requireNonNull(yearMonth, "年月不能为空");
        return yearMonth.atEndOfMonth().atTime(LocalTime.MAX);
    }

    /**
     * 获取当天开始时间。
     *
     * @param date 日期
     * @return 当天开始时间
     */
    public static LocalDateTime beginOfDay(LocalDate date) {
        return toStartDateTime(date);
    }

    /**
     * 获取日期时间所在天的开始时间。
     *
     * @param dateTime 日期时间
     * @return 当天开始时间
     */
    public static LocalDateTime beginOfDay(LocalDateTime dateTime) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.toLocalDate().atStartOfDay();
    }

    /**
     * 获取当天结束时间。
     *
     * @param date 日期
     * @return 当天结束时间
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return toEndDateTime(date);
    }

    /**
     * 获取日期时间所在天的结束时间。
     *
     * @param dateTime 日期时间
     * @return 当天结束时间
     */
    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.toLocalDate().atTime(LocalTime.MAX);
    }

    /**
     * 获取日期所在 ISO 周的开始日期。
     *
     * @param date 日期
     * @return 周开始日期
     */
    public static LocalDate beginOfWeek(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * 获取日期所在 ISO 周的结束日期。
     *
     * @param date 日期
     * @return 周结束日期
     */
    public static LocalDate endOfWeek(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /**
     * 获取日期所在月的开始日期。
     *
     * @param date 日期
     * @return 月开始日期
     */
    public static LocalDate beginOfMonth(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return date.withDayOfMonth(1);
    }

    /**
     * 获取日期所在月的结束日期。
     *
     * @param date 日期
     * @return 月结束日期
     */
    public static LocalDate endOfMonth(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    /**
     * 获取日期所在季度的开始日期。
     *
     * @param date 日期
     * @return 季度开始日期
     */
    public static LocalDate beginOfQuarter(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        int startMonth = quarterStartMonth(quarter(date));
        return LocalDate.of(date.getYear(), startMonth, 1);
    }

    /**
     * 获取日期所在季度的结束日期。
     *
     * @param date 日期
     * @return 季度结束日期
     */
    public static LocalDate endOfQuarter(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        int endMonth = quarterEndMonth(quarter(date));
        YearMonth yearMonth = YearMonth.of(date.getYear(), endMonth);
        return yearMonth.atEndOfMonth();
    }

    /**
     * 获取日期所在年的开始日期。
     *
     * @param date 日期
     * @return 年开始日期
     */
    public static LocalDate beginOfYear(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return LocalDate.of(date.getYear(), Month.JANUARY, 1);
    }

    /**
     * 获取日期所在年的结束日期。
     *
     * @param date 日期
     * @return 年结束日期
     */
    public static LocalDate endOfYear(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return LocalDate.of(date.getYear(), Month.DECEMBER, 31);
    }

    /**
     * 获取日期所在月份天数。
     *
     * @param date 日期
     * @return 月份天数
     */
    public static int lengthOfMonth(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return date.lengthOfMonth();
    }

    /**
     * 获取日期所在年份天数。
     *
     * @param date 日期
     * @return 年份天数
     */
    public static int lengthOfYear(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return date.lengthOfYear();
    }

    /**
     * 增加指定天数。
     *
     * @param dateTime 日期时间
     * @param days 天数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.plusDays(days);
    }

    /**
     * 减少指定天数。
     *
     * @param dateTime 日期时间
     * @param days 天数
     * @return 计算后的日期时间
     */
    public static LocalDateTime minusDays(LocalDateTime dateTime, long days) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.minusDays(days);
    }

    /**
     * 增加指定小时数。
     *
     * @param dateTime 日期时间
     * @param hours 小时数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.plusHours(hours);
    }

    /**
     * 增加指定分钟数。
     *
     * @param dateTime 日期时间
     * @param minutes 分钟数
     * @return 计算后的日期时间
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.plusMinutes(minutes);
    }

    /**
     * 增加指定月份数。
     *
     * @param date 日期
     * @param months 月份数
     * @return 计算后的日期
     */
    public static LocalDate plusMonths(LocalDate date, long months) {
        requireNonNull(date, "日期不能为空");
        return date.plusMonths(months);
    }

    /**
     * 增加指定年份数。
     *
     * @param date 日期
     * @param years 年份数
     * @return 计算后的日期
     */
    public static LocalDate plusYears(LocalDate date, long years) {
        requireNonNull(date, "日期不能为空");
        return date.plusYears(years);
    }

    /**
     * 获取 N 天前的日期。
     *
     * @param days 天数
     * @return 日期
     */
    public static LocalDate daysAgo(long days) {
        return LocalDate.now().minusDays(days);
    }

    /**
     * 使用指定时钟获取 N 天前的日期。
     *
     * @param days 天数
     * @param clock 时钟
     * @return 日期
     */
    public static LocalDate daysAgo(long days, Clock clock) {
        return nowDate(clock).minusDays(days);
    }

    /**
     * 获取 N 天后的日期。
     *
     * @param days 天数
     * @return 日期
     */
    public static LocalDate daysLater(long days) {
        return LocalDate.now().plusDays(days);
    }

    /**
     * 使用指定时钟获取 N 天后的日期。
     *
     * @param days 天数
     * @param clock 时钟
     * @return 日期
     */
    public static LocalDate daysLater(long days, Clock clock) {
        return nowDate(clock).plusDays(days);
    }

    /**
     * 获取下一个整点。
     *
     * @param dateTime 日期时间
     * @return 下一个整点
     */
    public static LocalDateTime nextHour(LocalDateTime dateTime) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.truncatedTo(ChronoUnit.HOURS).plusHours(1);
    }

    /**
     * 获取下一个自然日开始时间。
     *
     * @param dateTime 日期时间
     * @return 下一天开始时间
     */
    public static LocalDateTime nextDayBegin(LocalDateTime dateTime) {
        requireNonNull(dateTime, "日期时间不能为空");
        return dateTime.toLocalDate().plusDays(1).atStartOfDay();
    }

    /**
     * 计算两个日期时间相差秒数。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 秒数
     */
    public static long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.SECONDS.between(requireDateTime(start, "开始时间不能为空"), requireDateTime(end, "结束时间不能为空"));
    }

    /**
     * 计算两个日期时间相差分钟数。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 分钟数
     */
    public static long betweenMinutes(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(requireDateTime(start, "开始时间不能为空"), requireDateTime(end, "结束时间不能为空"));
    }

    /**
     * 计算两个日期时间相差小时数。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 小时数
     */
    public static long betweenHours(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(requireDateTime(start, "开始时间不能为空"), requireDateTime(end, "结束时间不能为空"));
    }

    /**
     * 计算两个日期相差天数。
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 天数
     */
    public static long betweenDays(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(requireDate(start, "开始日期不能为空"), requireDate(end, "结束日期不能为空"));
    }

    /**
     * 计算两个日期相差月份数。
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 月份数
     */
    public static long betweenMonths(LocalDate start, LocalDate end) {
        return ChronoUnit.MONTHS.between(requireDate(start, "开始日期不能为空"), requireDate(end, "结束日期不能为空"));
    }

    /**
     * 计算两个日期相差年份数。
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 年份数
     */
    public static long betweenYears(LocalDate start, LocalDate end) {
        return ChronoUnit.YEARS.between(requireDate(start, "开始日期不能为空"), requireDate(end, "结束日期不能为空"));
    }

    /**
     * 获取两个日期时间之间的 Duration。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return Duration
     */
    public static Duration durationBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(requireDateTime(start, "开始时间不能为空"), requireDateTime(end, "结束时间不能为空"));
    }

    /**
     * 获取两个日期之间的 Period。
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return Period
     */
    public static Period periodBetween(LocalDate start, LocalDate end) {
        return Period.between(requireDate(start, "开始日期不能为空"), requireDate(end, "结束日期不能为空"));
    }

    /**
     * 按系统当前日期计算年龄。
     *
     * @param birthday 生日
     * @return 年龄
     */
    public static int age(LocalDate birthday) {
        return age(birthday, Clock.systemDefaultZone());
    }

    /**
     * 按指定时钟计算年龄。
     *
     * @param birthday 生日
     * @param clock 时钟
     * @return 年龄
     */
    public static int age(LocalDate birthday, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        LocalDate today = LocalDate.now(clock);
        requireCondition(!requireDate(birthday, "生日不能为空").isAfter(today), "生日不能晚于当前日期");
        return Period.between(birthday, today).getYears();
    }

    /**
     * 获取截止时间的剩余时间描述。
     *
     * @param deadline 截止时间
     * @return 剩余时间描述
     */
    public static String remainingText(LocalDateTime deadline) {
        return remainingText(deadline, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟获取截止时间的剩余时间描述。
     *
     * @param deadline 截止时间
     * @param clock 时钟
     * @return 剩余时间描述
     */
    public static String remainingText(LocalDateTime deadline, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        Duration duration = Duration.between(LocalDateTime.now(clock), requireDateTime(deadline, "截止时间不能为空"));
        if (duration.isZero() || duration.isNegative()) {
            return "已结束";
        }
        return formatDuration(duration);
    }

    /**
     * 判断目标时间是否在闭区间内。
     *
     * @param target 目标时间
     * @param start 开始时间
     * @param end 结束时间
     * @return 是否在范围内
     */
    public static boolean isBetween(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
        requireDateTime(target, "目标时间不能为空");
        checkRange(start, end);
        return !target.isBefore(start) && !target.isAfter(end);
    }

    /**
     * 判断目标时间是否在左闭右开区间内。
     *
     * @param target 目标时间
     * @param start 开始时间
     * @param end 结束时间
     * @return 是否在范围内
     */
    public static boolean isBetweenHalfOpen(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
        requireDateTime(target, "目标时间不能为空");
        checkRange(start, end);
        return !target.isBefore(start) && target.isBefore(end);
    }

    /**
     * 判断两个左闭右开时间范围是否重叠。
     *
     * @param start1 第一个开始时间
     * @param end1 第一个结束时间
     * @param start2 第二个开始时间
     * @param end2 第二个结束时间
     * @return 是否重叠
     */
    public static boolean isOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        checkRange(start1, end1);
        checkRange(start2, end2);
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    /**
     * 获取两个左闭右开时间范围的交集。
     *
     * @param start1 第一个开始时间
     * @param end1 第一个结束时间
     * @param start2 第二个开始时间
     * @param end2 第二个结束时间
     * @return 交集
     */
    public static Optional<TimeRange> intersection(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        checkRange(start1, end1);
        checkRange(start2, end2);
        LocalDateTime start = start1.isAfter(start2) ? start1 : start2;
        LocalDateTime end = end1.isBefore(end2) ? end1 : end2;
        if (!start.isBefore(end)) {
            return Optional.empty();
        }
        return Optional.of(new TimeRange(start, end));
    }

    /**
     * 合并重叠或连续的时间范围。
     *
     * @param ranges 时间范围列表
     * @return 合并后的时间范围列表
     */
    public static List<TimeRange> mergeRanges(List<TimeRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return List.of();
        }
        List<TimeRange> sortedRanges = ranges.stream()
                .sorted(Comparator.comparing(TimeRange::start))
                .toList();
        List<TimeRange> result = new ArrayList<>();
        TimeRange current = sortedRanges.getFirst();
        for (int i = 1; i < sortedRanges.size(); i++) {
            TimeRange next = sortedRanges.get(i);
            if (!current.end().isBefore(next.start())) {
                LocalDateTime maxEnd = current.end().isAfter(next.end()) ? current.end() : next.end();
                current = new TimeRange(current.start(), maxEnd);
            } else {
                result.add(current);
                current = next;
            }
        }
        result.add(current);
        return result;
    }

    /**
     * 按天拆分左闭右开时间范围。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 时间范围列表
     */
    public static List<TimeRange> splitByDay(LocalDateTime start, LocalDateTime end) {
        return splitByUnit(start, end, ChronoUnit.DAYS);
    }

    /**
     * 按小时拆分左闭右开时间范围。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 时间范围列表
     */
    public static List<TimeRange> splitByHour(LocalDateTime start, LocalDateTime end) {
        return splitByUnit(start, end, ChronoUnit.HOURS);
    }

    /**
     * 判断时间范围是否合法。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @return 是否合法
     */
    public static boolean isValidRange(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null && start.isBefore(end);
    }

    /**
     * 判断日期是否为周末。
     *
     * @param date 日期
     * @return 是否周末
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = requireDate(date, "日期不能为空").getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 判断日期是否为工作日，仅排除周末。
     *
     * @param date 日期
     * @return 是否工作日
     */
    public static boolean isWorkday(LocalDate date) {
        return isWorkday(date, day -> false);
    }

    /**
     * 判断日期是否为工作日，可传入节假日规则。
     *
     * @param date 日期
     * @param holidayPredicate 节假日判断规则
     * @return 是否工作日
     */
    public static boolean isWorkday(LocalDate date, Predicate<LocalDate> holidayPredicate) {
        return !isWeekend(date) && !isHoliday(date, holidayPredicate);
    }

    /**
     * 获取下一个工作日，仅排除周末。
     *
     * @param date 日期
     * @return 下一个工作日
     */
    public static LocalDate nextWorkday(LocalDate date) {
        return nextWorkday(date, day -> false);
    }

    /**
     * 获取下一个工作日，可传入节假日规则。
     *
     * @param date 日期
     * @param holidayPredicate 节假日判断规则
     * @return 下一个工作日
     */
    public static LocalDate nextWorkday(LocalDate date, Predicate<LocalDate> holidayPredicate) {
        LocalDate current = requireDate(date, "日期不能为空");
        do {
            current = current.plusDays(1);
        } while (!isWorkday(current, holidayPredicate));
        return current;
    }

    /**
     * 获取上一个工作日，仅排除周末。
     *
     * @param date 日期
     * @return 上一个工作日
     */
    public static LocalDate previousWorkday(LocalDate date) {
        return previousWorkday(date, day -> false);
    }

    /**
     * 获取上一个工作日，可传入节假日规则。
     *
     * @param date 日期
     * @param holidayPredicate 节假日判断规则
     * @return 上一个工作日
     */
    public static LocalDate previousWorkday(LocalDate date, Predicate<LocalDate> holidayPredicate) {
        LocalDate current = requireDate(date, "日期不能为空");
        do {
            current = current.minusDays(1);
        } while (!isWorkday(current, holidayPredicate));
        return current;
    }

    /**
     * 增加 N 个工作日，仅排除周末。
     *
     * @param date 日期
     * @param days 工作日天数
     * @return 计算后的日期
     */
    public static LocalDate plusWorkdays(LocalDate date, int days) {
        return plusWorkdays(date, days, day -> false);
    }

    /**
     * 增加 N 个工作日，可传入节假日规则。
     *
     * @param date 日期
     * @param days 工作日天数
     * @param holidayPredicate 节假日判断规则
     * @return 计算后的日期
     */
    public static LocalDate plusWorkdays(LocalDate date, int days, Predicate<LocalDate> holidayPredicate) {
        LocalDate current = requireDate(date, "日期不能为空");
        if (days == 0) {
            return current;
        }
        int step = days > 0 ? 1 : -1;
        int remaining = Math.abs(days);
        while (remaining > 0) {
            current = current.plusDays(step);
            if (isWorkday(current, holidayPredicate)) {
                remaining--;
            }
        }
        return current;
    }

    /**
     * 计算左闭右开日期区间内的工作日数量，仅排除周末。
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 工作日数量
     */
    public static long betweenWorkdays(LocalDate start, LocalDate end) {
        return betweenWorkdays(start, end, day -> false);
    }

    /**
     * 计算左闭右开日期区间内的工作日数量，可传入节假日规则。
     *
     * @param start 开始日期
     * @param end 结束日期
     * @param holidayPredicate 节假日判断规则
     * @return 工作日数量
     */
    public static long betweenWorkdays(LocalDate start, LocalDate end, Predicate<LocalDate> holidayPredicate) {
        requireDate(start, "开始日期不能为空");
        requireDate(end, "结束日期不能为空");
        requireCondition(!start.isAfter(end), "开始日期不能晚于结束日期");
        return start.datesUntil(end)
                .filter(date -> isWorkday(date, holidayPredicate))
                .count();
    }

    /**
     * 判断日期是否为节假日。
     *
     * @param date 日期
     * @param holidayPredicate 节假日判断规则
     * @return 是否节假日
     */
    public static boolean isHoliday(LocalDate date, Predicate<LocalDate> holidayPredicate) {
        requireDate(date, "日期不能为空");
        requireNonNull(holidayPredicate, "节假日判断规则不能为空");
        return holidayPredicate.test(date);
    }

    /**
     * 获取指定月份的工作日列表，仅排除周末。
     *
     * @param yearMonth 年月
     * @return 工作日列表
     */
    public static List<LocalDate> workdaysOfMonth(YearMonth yearMonth) {
        return workdaysOfMonth(yearMonth, day -> false);
    }

    /**
     * 获取指定月份的工作日列表，可传入节假日规则。
     *
     * @param yearMonth 年月
     * @param holidayPredicate 节假日判断规则
     * @return 工作日列表
     */
    public static List<LocalDate> workdaysOfMonth(YearMonth yearMonth, Predicate<LocalDate> holidayPredicate) {
        requireNonNull(yearMonth, "年月不能为空");
        return datesBetween(yearMonth.atDay(1), yearMonth.plusMonths(1).atDay(1)).stream()
                .filter(date -> isWorkday(date, holidayPredicate))
                .toList();
    }

    /**
     * 获取日期所在年的本地化周编号。
     *
     * @param date 日期
     * @return 周编号
     */
    public static int weekOfYear(LocalDate date) {
        return requireDate(date, "日期不能为空").get(WeekFields.of(Locale.getDefault()).weekOfYear());
    }

    /**
     * 获取日期所在年的 ISO 周编号。
     *
     * @param date 日期
     * @return ISO 周编号
     */
    public static int isoWeekOfYear(LocalDate date) {
        return requireDate(date, "日期不能为空").get(WeekFields.ISO.weekOfWeekBasedYear());
    }

    /**
     * 获取日期所在季度，范围为 1 到 4。
     *
     * @param date 日期
     * @return 季度
     */
    public static int quarter(LocalDate date) {
        requireNonNull(date, "日期不能为空");
        return (date.getMonthValue() - 1) / 3 + 1;
    }

    /**
     * 获取季度开始月份。
     *
     * @param quarter 季度
     * @return 开始月份
     */
    public static int quarterStartMonth(int quarter) {
        checkQuarter(quarter);
        return (quarter - 1) * 3 + 1;
    }

    /**
     * 获取季度结束月份。
     *
     * @param quarter 季度
     * @return 结束月份
     */
    public static int quarterEndMonth(int quarter) {
        checkQuarter(quarter);
        return quarter * 3;
    }

    /**
     * 获取指定年份和季度的日期范围，闭区间。
     *
     * @param year 年份
     * @param quarter 季度
     * @return 日期范围
     */
    public static DateRange quarterRange(int year, int quarter) {
        LocalDate start = LocalDate.of(year, quarterStartMonth(quarter), 1);
        LocalDate end = YearMonth.of(year, quarterEndMonth(quarter)).atEndOfMonth();
        return new DateRange(start, end);
    }

    /**
     * 获取指定年月的日期范围，闭区间。
     *
     * @param yearMonth 年月
     * @return 日期范围
     */
    public static DateRange monthRange(YearMonth yearMonth) {
        requireNonNull(yearMonth, "年月不能为空");
        return new DateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    /**
     * 获取指定年份的日期范围，闭区间。
     *
     * @param year 年份
     * @return 日期范围
     */
    public static DateRange yearRange(int year) {
        return new DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    /**
     * 获取左闭右开日期区间内的年月列表。
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 年月列表
     */
    public static List<YearMonth> monthsBetween(LocalDate start, LocalDate end) {
        requireDate(start, "开始日期不能为空");
        requireDate(end, "结束日期不能为空");
        requireCondition(!start.isAfter(end), "开始日期不能晚于结束日期");
        YearMonth startMonth = YearMonth.from(start);
        YearMonth endMonth = YearMonth.from(end.minusDays(1));
        List<YearMonth> result = new ArrayList<>();
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            result.add(current);
            current = current.plusMonths(1);
        }
        return result;
    }

    /**
     * 获取左闭右开日期区间内的日期列表。
     *
     * @param start 开始日期
     * @param end 结束日期
     * @return 日期列表
     */
    public static List<LocalDate> datesBetween(LocalDate start, LocalDate end) {
        requireDate(start, "开始日期不能为空");
        requireDate(end, "结束日期不能为空");
        requireCondition(!start.isAfter(end), "开始日期不能晚于结束日期");
        return start.datesUntil(end).toList();
    }

    /**
     * 获取系统默认时区。
     *
     * @return 系统默认时区
     */
    public static ZoneId systemZone() {
        return ZoneId.systemDefault();
    }

    /**
     * 获取 UTC 当前时间。
     *
     * @return UTC 时间
     */
    public static ZonedDateTime utcNow() {
        return ZonedDateTime.now(UTC_ZONE);
    }

    /**
     * 使用指定时钟获取 UTC 当前时间。
     *
     * @param clock 时钟
     * @return UTC 时间
     */
    public static ZonedDateTime utcNow(Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        return ZonedDateTime.now(clock).withZoneSameInstant(UTC_ZONE);
    }

    /**
     * 将 ZonedDateTime 转换到目标时区。
     *
     * @param dateTime 日期时间
     * @param targetZone 目标时区
     * @return 转换后的日期时间
     */
    public static ZonedDateTime convertZone(ZonedDateTime dateTime, ZoneId targetZone) {
        requireNonNull(dateTime, "日期时间不能为空");
        requireNonNull(targetZone, "目标时区不能为空");
        return dateTime.withZoneSameInstant(targetZone);
    }

    /**
     * 为 LocalDateTime 绑定时区。
     *
     * @param dateTime 日期时间
     * @param zoneId 时区
     * @return 带时区日期时间
     */
    public static ZonedDateTime atZone(LocalDateTime dateTime, ZoneId zoneId) {
        requireNonNull(dateTime, "日期时间不能为空");
        requireNonNull(zoneId, "时区不能为空");
        return dateTime.atZone(zoneId);
    }

    /**
     * 将 ZonedDateTime 转为 LocalDateTime。
     *
     * @param zonedDateTime 带时区日期时间
     * @return 日期时间
     */
    public static LocalDateTime toLocalDateTime(ZonedDateTime zonedDateTime) {
        requireNonNull(zonedDateTime, "带时区日期时间不能为空");
        return zonedDateTime.toLocalDateTime();
    }

    /**
     * 判断时区 ID 是否有效。
     *
     * @param zoneId 时区 ID
     * @return 是否有效
     */
    public static boolean isValidZoneId(String zoneId) {
        if (isBlank(zoneId)) {
            return false;
        }
        try {
            ZoneId.of(zoneId);
            return true;
        } catch (DateTimeException ignored) {
            return false;
        }
    }

    /**
     * 获取全部可用时区 ID。
     *
     * @return 时区 ID 集合
     */
    public static Set<String> availableZoneIds() {
        return ZoneId.getAvailableZoneIds();
    }

    /**
     * 将秒级时间戳转为系统默认时区日期时间。
     *
     * @param epochSecond 秒级时间戳
     * @return 日期时间
     */
    public static LocalDateTime epochSecondToDateTime(long epochSecond) {
        return toLocalDateTime(Instant.ofEpochSecond(epochSecond));
    }

    /**
     * 将秒级时间戳转为指定时区日期时间。
     *
     * @param epochSecond 秒级时间戳
     * @param zoneId 时区
     * @return 日期时间
     */
    public static LocalDateTime epochSecondToDateTime(long epochSecond, ZoneId zoneId) {
        return toLocalDateTime(Instant.ofEpochSecond(epochSecond), zoneId);
    }

    /**
     * 将毫秒时间戳转为系统默认时区日期时间。
     *
     * @param epochMilli 毫秒时间戳
     * @return 日期时间
     */
    public static LocalDateTime epochMilliToDateTime(long epochMilli) {
        return toLocalDateTime(epochMilli);
    }

    /**
     * 将毫秒时间戳转为指定时区日期时间。
     *
     * @param epochMilli 毫秒时间戳
     * @param zoneId 时区
     * @return 日期时间
     */
    public static LocalDateTime epochMilliToDateTime(long epochMilli, ZoneId zoneId) {
        return toLocalDateTime(Instant.ofEpochMilli(epochMilli), zoneId);
    }

    /**
     * 将日期时间转为系统默认时区秒级时间戳。
     *
     * @param dateTime 日期时间
     * @return 秒级时间戳
     */
    public static long toEpochSecond(LocalDateTime dateTime) {
        return toInstant(dateTime).getEpochSecond();
    }

    /**
     * 将日期时间转为指定时区秒级时间戳。
     *
     * @param dateTime 日期时间
     * @param zoneId 时区
     * @return 秒级时间戳
     */
    public static long toEpochSecond(LocalDateTime dateTime, ZoneId zoneId) {
        return toInstant(dateTime, zoneId).getEpochSecond();
    }

    /**
     * 将日期时间转为指定时区毫秒时间戳。
     *
     * @param dateTime 日期时间
     * @param zoneId 时区
     * @return 毫秒时间戳
     */
    public static long toEpochMilli(LocalDateTime dateTime, ZoneId zoneId) {
        return toInstant(dateTime, zoneId).toEpochMilli();
    }

    /**
     * 识别时间戳单位。
     *
     * @param timestamp 时间戳
     * @return 时间戳单位
     */
    public static EpochUnit detectEpochUnit(long timestamp) {
        return Math.abs(timestamp) <= EPOCH_SECOND_MAX_ABS ? EpochUnit.SECOND : EpochUnit.MILLISECOND;
    }

    /**
     * 将秒级或毫秒级时间戳标准化为毫秒。
     *
     * @param timestamp 时间戳
     * @return 毫秒时间戳
     */
    public static long normalizeToEpochMilli(long timestamp) {
        return detectEpochUnit(timestamp) == EpochUnit.SECOND ? Math.multiplyExact(timestamp, 1000L) : timestamp;
    }

    /**
     * 获取友好时间描述。
     *
     * @param dateTime 日期时间
     * @return 友好时间描述
     */
    public static String friendlyTime(LocalDateTime dateTime) {
        return friendlyTime(dateTime, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟获取友好时间描述。
     *
     * @param dateTime 日期时间
     * @param clock 时钟
     * @return 友好时间描述
     */
    public static String friendlyTime(LocalDateTime dateTime, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        LocalDateTime target = requireDateTime(dateTime, "日期时间不能为空");
        LocalDateTime now = LocalDateTime.now(clock);
        Duration duration = Duration.between(target, now);
        if (duration.isNegative() || duration.toSeconds() < 60) {
            return "刚刚";
        }
        if (duration.toMinutes() < 60) {
            return duration.toMinutes() + "分钟前";
        }
        if (duration.toHours() < 24 && target.toLocalDate().equals(now.toLocalDate())) {
            return duration.toHours() + "小时前";
        }
        if (target.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "昨天 " + target.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (target.getYear() == now.getYear()) {
            return target.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        }
        return target.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * 获取友好日期描述。
     *
     * @param date 日期
     * @return 友好日期描述
     */
    public static String friendlyDate(LocalDate date) {
        return friendlyDate(date, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟获取友好日期描述。
     *
     * @param date 日期
     * @param clock 时钟
     * @return 友好日期描述
     */
    public static String friendlyDate(LocalDate date, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        LocalDate target = requireDate(date, "日期不能为空");
        LocalDate today = LocalDate.now(clock);
        if (target.equals(today)) {
            return "今天";
        }
        if (target.equals(today.minusDays(1))) {
            return "昨天";
        }
        if (target.equals(today.plusDays(1))) {
            return "明天";
        }
        return target.format(NORM_DATE_FORMATTER);
    }

    /**
     * 获取倒计时描述。
     *
     * @param deadline 截止时间
     * @return 倒计时描述
     */
    public static String countdownText(LocalDateTime deadline) {
        return countdownText(deadline, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟获取倒计时描述。
     *
     * @param deadline 截止时间
     * @param clock 时钟
     * @return 倒计时描述
     */
    public static String countdownText(LocalDateTime deadline, Clock clock) {
        return remainingText(deadline, clock);
    }

    /**
     * 格式化 Duration 为中文耗时。
     *
     * @param duration 耗时
     * @return 中文耗时
     */
    public static String formatDuration(Duration duration) {
        requireNonNull(duration, "耗时不能为空");
        if (duration.isZero()) {
            return "0秒";
        }
        boolean negative = duration.isNegative();
        Duration positiveDuration = negative ? duration.abs() : duration;
        long seconds = positiveDuration.getSeconds();
        long days = seconds / 86_400;
        long hours = seconds % 86_400 / 3_600;
        long minutes = seconds % 3_600 / 60;
        long remainingSeconds = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (negative) {
            builder.append('-');
        }
        if (days > 0) {
            builder.append(days).append('天');
        }
        if (hours > 0) {
            builder.append(hours).append("小时");
        }
        if (minutes > 0) {
            builder.append(minutes).append("分钟");
        }
        if (remainingSeconds > 0 || builder.isEmpty() || "-".contentEquals(builder)) {
            builder.append(remainingSeconds).append('秒');
        }
        return builder.toString();
    }

    /**
     * 判断时间是否已过期。
     *
     * @param expireTime 过期时间
     * @return 是否过期
     */
    public static boolean isExpired(LocalDateTime expireTime) {
        return isExpired(expireTime, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟判断时间是否已过期。
     *
     * @param expireTime 过期时间
     * @param clock 时钟
     * @return 是否过期
     */
    public static boolean isExpired(LocalDateTime expireTime, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        return !LocalDateTime.now(clock).isBefore(requireDateTime(expireTime, "过期时间不能为空"));
    }

    /**
     * 判断开始时间是否未到。
     *
     * @param startTime 开始时间
     * @return 是否未开始
     */
    public static boolean isNotStarted(LocalDateTime startTime) {
        return isNotStarted(startTime, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟判断开始时间是否未到。
     *
     * @param startTime 开始时间
     * @param clock 时钟
     * @return 是否未开始
     */
    public static boolean isNotStarted(LocalDateTime startTime, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        return LocalDateTime.now(clock).isBefore(requireDateTime(startTime, "开始时间不能为空"));
    }

    /**
     * 判断当前时间是否在进行中，范围为左闭右开。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否进行中
     */
    public static boolean isInProgress(LocalDateTime startTime, LocalDateTime endTime) {
        return isInProgress(startTime, endTime, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟判断是否进行中，范围为左闭右开。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param clock 时钟
     * @return 是否进行中
     */
    public static boolean isInProgress(LocalDateTime startTime, LocalDateTime endTime, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        checkRange(startTime, endTime);
        return isBetweenHalfOpen(LocalDateTime.now(clock), startTime, endTime);
    }

    /**
     * 判断结束时间是否已到。
     *
     * @param endTime 结束时间
     * @return 是否已结束
     */
    public static boolean isEnded(LocalDateTime endTime) {
        return isEnded(endTime, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟判断结束时间是否已到。
     *
     * @param endTime 结束时间
     * @param clock 时钟
     * @return 是否已结束
     */
    public static boolean isEnded(LocalDateTime endTime, Clock clock) {
        return isExpired(endTime, clock);
    }

    /**
     * 获取剩余秒数，小于 0 时返回 0。
     *
     * @param expireTime 过期时间
     * @return 剩余秒数
     */
    public static long remainingSeconds(LocalDateTime expireTime) {
        return remainingSeconds(expireTime, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟获取剩余秒数，小于 0 时返回 0。
     *
     * @param expireTime 过期时间
     * @param clock 时钟
     * @return 剩余秒数
     */
    public static long remainingSeconds(LocalDateTime expireTime, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        long seconds = Duration.between(LocalDateTime.now(clock), requireDateTime(expireTime, "过期时间不能为空")).toSeconds();
        return Math.max(seconds, 0L);
    }

    /**
     * 获取剩余天数，小于 0 时返回 0。
     *
     * @param expireTime 过期时间
     * @return 剩余天数
     */
    public static long remainingDays(LocalDateTime expireTime) {
        return remainingDays(expireTime, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟获取剩余天数，小于 0 时返回 0。
     *
     * @param expireTime 过期时间
     * @param clock 时钟
     * @return 剩余天数
     */
    public static long remainingDays(LocalDateTime expireTime, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(clock), requireDateTime(expireTime, "过期时间不能为空"));
        return Math.max(days, 0L);
    }

    /**
     * 根据有效天数计算过期时间。
     *
     * @param start 开始时间
     * @param days 有效天数
     * @return 过期时间
     */
    public static LocalDateTime expireAfterDays(LocalDateTime start, long days) {
        return requireDateTime(start, "开始时间不能为空").plusDays(days);
    }

    /**
     * 根据有效秒数计算过期时间。
     *
     * @param start 开始时间
     * @param seconds 有效秒数
     * @return 过期时间
     */
    public static LocalDateTime expireAfterSeconds(LocalDateTime start, long seconds) {
        return requireDateTime(start, "开始时间不能为空").plusSeconds(seconds);
    }

    /**
     * 构造当天查询时间范围，左闭右开。
     *
     * @param date 日期
     * @return 时间范围
     */
    public static TimeRange dayRange(LocalDate date) {
        LocalDate start = requireDate(date, "日期不能为空");
        return new TimeRange(start.atStartOfDay(), start.plusDays(1).atStartOfDay());
    }

    /**
     * 构造本周查询时间范围，左闭右开。
     *
     * @param date 日期
     * @return 时间范围
     */
    public static TimeRange weekRange(LocalDate date) {
        LocalDate start = beginOfWeek(date);
        return new TimeRange(start.atStartOfDay(), start.plusWeeks(1).atStartOfDay());
    }

    /**
     * 构造本月查询时间范围，左闭右开。
     *
     * @param date 日期
     * @return 时间范围
     */
    public static TimeRange monthRange(LocalDate date) {
        LocalDate start = beginOfMonth(date);
        return new TimeRange(start.atStartOfDay(), start.plusMonths(1).atStartOfDay());
    }

    /**
     * 构造最近 N 天到当前时刻的查询范围。
     *
     * @param days 天数
     * @return 时间范围
     */
    public static TimeRange lastDaysRange(int days) {
        return lastDaysRange(days, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟构造最近 N 天到当前时刻的查询范围。
     *
     * @param days 天数
     * @param clock 时钟
     * @return 时间范围
     */
    public static TimeRange lastDaysRange(int days, Clock clock) {
        requireCondition(days > 0, "天数必须大于0");
        requireNonNull(clock, "时钟不能为空");
        LocalDateTime end = LocalDateTime.now(clock);
        return new TimeRange(end.minusDays(days), end);
    }

    /**
     * 构造最近 N 小时到当前时刻的查询范围。
     *
     * @param hours 小时数
     * @return 时间范围
     */
    public static TimeRange lastHoursRange(int hours) {
        return lastHoursRange(hours, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟构造最近 N 小时到当前时刻的查询范围。
     *
     * @param hours 小时数
     * @param clock 时钟
     * @return 时间范围
     */
    public static TimeRange lastHoursRange(int hours, Clock clock) {
        requireCondition(hours > 0, "小时数必须大于0");
        requireNonNull(clock, "时钟不能为空");
        LocalDateTime end = LocalDateTime.now(clock);
        return new TimeRange(end.minusHours(hours), end);
    }

    /**
     * 获取查询开始时间。
     *
     * @param date 日期
     * @return 查询开始时间
     */
    public static LocalDateTime queryStart(LocalDate date) {
        return beginOfDay(date);
    }

    /**
     * 获取日期对应的排他查询结束时间。
     *
     * @param date 日期
     * @return 排他结束时间
     */
    public static LocalDateTime queryEndExclusive(LocalDate date) {
        return requireDate(date, "日期不能为空").plusDays(1).atStartOfDay();
    }

    /**
     * 判断 Temporal 是否为空。
     *
     * @param temporal Temporal 对象
     * @return 是否为空
     */
    public static boolean isNull(Temporal temporal) {
        return temporal == null;
    }

    /**
     * 校验开始时间必须早于结束时间。
     *
     * @param start 开始时间
     * @param end 结束时间
     */
    public static void checkRange(LocalDateTime start, LocalDateTime end) {
        requireNonNull(start, "开始时间不能为空");
        requireNonNull(end, "结束时间不能为空");
        requireCondition(start.isBefore(end), "开始时间必须早于结束时间");
    }

    /**
     * 校验日期不能早于今天。
     *
     * @param date 日期
     */
    public static void checkNotBeforeToday(LocalDate date) {
        checkNotBeforeToday(date, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟校验日期不能早于今天。
     *
     * @param date 日期
     * @param clock 时钟
     */
    public static void checkNotBeforeToday(LocalDate date, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        requireCondition(!requireDate(date, "日期不能为空").isBefore(LocalDate.now(clock)), "日期不能早于今天");
    }

    /**
     * 校验日期不能晚于今天。
     *
     * @param date 日期
     */
    public static void checkNotAfterToday(LocalDate date) {
        checkNotAfterToday(date, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟校验日期不能晚于今天。
     *
     * @param date 日期
     * @param clock 时钟
     */
    public static void checkNotAfterToday(LocalDate date, Clock clock) {
        requireNonNull(clock, "时钟不能为空");
        requireCondition(!requireDate(date, "日期不能为空").isAfter(LocalDate.now(clock)), "日期不能晚于今天");
    }

    /**
     * 判断生日是否合法，不能晚于今天。
     *
     * @param birthday 生日
     * @return 是否合法
     */
    public static boolean isValidBirthday(LocalDate birthday) {
        return isValidBirthday(birthday, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟判断生日是否合法，不能晚于今天。
     *
     * @param birthday 生日
     * @param clock 时钟
     * @return 是否合法
     */
    public static boolean isValidBirthday(LocalDate birthday, Clock clock) {
        if (birthday == null || clock == null) {
            return false;
        }
        return !birthday.isAfter(LocalDate.now(clock));
    }

    /**
     * 判断时间范围是否在最大跨度天数内。
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param maxDays 最大天数
     * @return 是否在限制内
     */
    public static boolean isRangeWithinDays(LocalDateTime start, LocalDateTime end, long maxDays) {
        checkRange(start, end);
        requireCondition(maxDays >= 0, "最大天数不能小于0");
        return Duration.between(start, end).toDays() <= maxDays;
    }

    /**
     * 格式化为标准日期时间字符串。
     *
     * @param dateTime 日期时间
     * @return 标准日期时间字符串
     */
    public static String toNormDateTime(LocalDateTime dateTime) {
        return format(dateTime, NORM_DATETIME_PATTERN);
    }

    /**
     * 格式化为纯数字日期时间字符串。
     *
     * @param dateTime 日期时间
     * @return 纯数字日期时间字符串
     */
    public static String toPureDateTime(LocalDateTime dateTime) {
        return format(dateTime, PURE_DATETIME_PATTERN);
    }

    /**
     * 格式化为 ISO 日期时间字符串。
     *
     * @param dateTime 日期时间
     * @return ISO 字符串
     */
    public static String toIsoString(LocalDateTime dateTime) {
        return format(dateTime, ISO_DATETIME_PATTERN);
    }

    /**
     * 将日期格式化为日期目录。
     *
     * @param date 日期
     * @return 日期目录
     */
    public static String toDatePath(LocalDate date) {
        return format(date, DATE_PATH_PATTERN);
    }

    /**
     * 给文件名追加当前时间戳。
     *
     * @param filename 文件名
     * @return 带时间戳文件名
     */
    public static String appendTimestamp(String filename) {
        return appendTimestamp(filename, Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟给文件名追加时间戳。
     *
     * @param filename 文件名
     * @param clock 时钟
     * @return 带时间戳文件名
     */
    public static String appendTimestamp(String filename, Clock clock) {
        String value = requireText(filename, "文件名不能为空");
        String timestamp = batchTimePart(clock);
        int slashIndex = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dotIndex = value.lastIndexOf('.');
        if (dotIndex > slashIndex) {
            return value.substring(0, dotIndex) + '_' + timestamp + value.substring(dotIndex);
        }
        return value + '_' + timestamp;
    }

    /**
     * 生成当前批次时间片段。
     *
     * @return 批次时间片段
     */
    public static String batchTimePart() {
        return batchTimePart(Clock.systemDefaultZone());
    }

    /**
     * 使用指定时钟生成批次时间片段。
     *
     * @param clock 时钟
     * @return 批次时间片段
     */
    public static String batchTimePart(Clock clock) {
        return nowDateTime(clock).format(PURE_DATETIME_FORMATTER);
    }

    /**
     * 创建固定时间时钟。
     *
     * @param dateTime 日期时间
     * @return 固定时间时钟
     */
    public static Clock fixedClock(LocalDateTime dateTime) {
        return fixedClock(dateTime, systemZone());
    }

    /**
     * 创建指定时区的固定时间时钟。
     *
     * @param dateTime 日期时间
     * @param zoneId 时区
     * @return 固定时间时钟
     */
    public static Clock fixedClock(LocalDateTime dateTime, ZoneId zoneId) {
        return Clock.fixed(toInstant(requireDateTime(dateTime, "日期时间不能为空"), zoneId), zoneId);
    }

    /**
     * 基于系统时钟创建偏移时钟。
     *
     * @param duration 偏移量
     * @return 偏移时钟
     */
    public static Clock offsetClock(Duration duration) {
        requireNonNull(duration, "偏移量不能为空");
        return Clock.offset(Clock.systemDefaultZone(), duration);
    }

    /**
     * 解析测试日期时间，使用标准日期时间格式。
     *
     * @param text 文本
     * @return 日期时间
     */
    public static LocalDateTime testDateTime(String text) {
        return parseDateTime(text, NORM_DATETIME_PATTERN);
    }

    private static <T> T requireNonNull(T object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
        return object;
    }

    private static void requireCondition(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static boolean isNotBlank(String text) {
        return !isBlank(text);
    }

    private static DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(requireText(pattern, "日期格式不能为空"));
    }

    private static String requireText(String text, String message) {
        requireCondition(isNotBlank(text), message);
        return text;
    }

    private static LocalDate requireDate(LocalDate date, String message) {
        requireNonNull(date, message);
        return date;
    }

    private static LocalDateTime requireDateTime(LocalDateTime dateTime, String message) {
        requireNonNull(dateTime, message);
        return dateTime;
    }

    private static void checkQuarter(int quarter) {
        requireCondition(quarter >= 1 && quarter <= 4, "季度必须在1到4之间");
    }

    private static List<TimeRange> splitByUnit(LocalDateTime start, LocalDateTime end, ChronoUnit unit) {
        checkRange(start, end);
        List<TimeRange> result = new ArrayList<>();
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            LocalDateTime next = switch (unit) {
                case DAYS -> current.toLocalDate().plusDays(1).atStartOfDay();
                case HOURS -> current.truncatedTo(ChronoUnit.HOURS).plusHours(1);
                default -> throw new IllegalArgumentException("不支持的拆分单位");
            };
            if (next.isAfter(end)) {
                next = end;
            }
            result.add(new TimeRange(current, next));
            current = next;
        }
        return result;
    }

    /**
     * 左闭右开时间范围。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record TimeRange(LocalDateTime start, LocalDateTime end) {

        /**
         * 创建左闭右开时间范围。
         *
         * @param start 开始时间
         * @param end 结束时间
         */
        public TimeRange {
            requireNonNull(start, "开始时间不能为空");
            requireNonNull(end, "结束时间不能为空");
            requireCondition(start.isBefore(end), "开始时间必须早于结束时间");
        }

        /**
         * 获取持续时间。
         *
         * @return 持续时间
         */
        public Duration duration() {
            return Duration.between(start, end);
        }
    }

    /**
     * 闭区间日期范围。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public record DateRange(LocalDate start, LocalDate end) {

        /**
         * 创建闭区间日期范围。
         *
         * @param start 开始日期
         * @param end 结束日期
         */
        public DateRange {
            requireNonNull(start, "开始日期不能为空");
            requireNonNull(end, "结束日期不能为空");
            requireCondition(!start.isAfter(end), "开始日期不能晚于结束日期");
        }
    }

    /**
     * 时间戳单位。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public enum EpochUnit {
        SECOND,
        MILLISECOND
    }
}
