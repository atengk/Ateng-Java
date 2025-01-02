package local.ateng.java.hutool;

import cn.hutool.core.date.*;
import cn.hutool.core.thread.ThreadUtil;
import org.junit.jupiter.api.Test;

import java.time.format.TextStyle;
import java.util.List;

/**
 * DateUtil 针对日期时间操作提供一系列静态方法
 * https://www.hutool.cn/docs/#/core/%E6%97%A5%E6%9C%9F%E6%97%B6%E9%97%B4/%E6%97%A5%E6%9C%9F%E6%97%B6%E9%97%B4%E5%B7%A5%E5%85%B7-DateUtil
 *
 * @author 孔余
 * @since 2024-02-02 13:33
 */
public class DateUtilTests {

    /**
     * 根据时间戳获取时间
     */
    @Test
    void test00() {
        // 注意是毫秒单位，需要乘1000
        long now = 1733906940L * 1000;
        System.out.println(now);
        DateTime dateTime = DateUtil.date(now);
        System.out.println(dateTime);
    }

    /**
     * 获取当前时间
     */
    @Test
    void test01() {
        // 获取当前时间：2024-02-22 11:01:04
        DateTime dateTime = DateUtil.date();
        System.out.println(dateTime);

        // 获取当前时间字符串：2024-02-22 11:01:04
        String dateTimeStr = DateUtil.now();
        System.out.println(dateTimeStr);

        //当前日期字符串，格式：yyyy-MM-dd 2024-02-22
        String today = DateUtil.today();
        System.out.println(today);
    }

    /**
     * 字符串转日期
     */
    @Test
    void test02() {
        // 字符串转日期(自动识别一些常用格式)，默认格式：2017-03-01 00:00:00
        String dateStr = "2017-03-01";
        DateTime date = DateUtil.parse(dateStr);
        System.out.println(date);

        // 自定义日期格式转化，
        String dateStr2 = "2017-03-01";
        DateTime date2 = DateUtil.parse(dateStr2, "yyyy-MM-dd");
        System.out.println(date2);

        // 保留毫秒
        String dateStr3 = "2017-03-01 12:34:56.112";
        DateTime date3 = DateUtil.parse(dateStr3,"yyyy-MM-dd HH:mm:ss.SSS");
        System.out.println(date3);
    }

    /**
     * 格式化日期输出
     */
    @Test
    void test3() {
        DateTime dateTime = DateUtil.date();

        // 常见日期格式
        /**
         * yyyy-MM-dd HH:mm:ss
         * yyyy/MM/dd HH:mm:ss
         * yyyy.MM.dd HH:mm:ss
         * yyyy年MM月dd日 HH时mm分ss秒
         * yyyy-MM-dd
         * yyyy/MM/dd
         * yyyy.MM.dd
         * HH:mm:ss
         * HH时mm分ss秒
         * yyyy-MM-dd HH:mm
         * yyyy-MM-dd HH:mm:ss.SSS
         * yyyyMMddHHmmss
         * yyyyMMddHHmmssSSS
         * yyyyMMdd
         * EEE, dd MMM yyyy HH:mm:ss z
         * EEE MMM dd HH:mm:ss zzz yyyy
         * yyyy-MM-dd'T'HH:mm:ss'Z'
         * yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
         * yyyy-MM-dd'T'HH:mm:ssZ
         * yyyy-MM-dd'T'HH:mm:ss.SSSZ
         */

        // 格式化为指定时间格式
        String format = DateUtil.format(dateTime, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        System.out.println(format);

        // 常规格式：2024-02-22 11:14:06
        String dateTimeStr = DateUtil.formatDateTime(dateTime);
        System.out.println(dateTimeStr);

        // 日期格式：2024-02-22
        String date = DateUtil.formatDate(dateTime);
        System.out.println(date);

        // 时间格式：11:13:09
        String time = DateUtil.formatTime(dateTime);
        System.out.println(time);

        // 中文时间格式：二〇二四年二月二十二日十一时十六分十九秒
        String chineseDateTime = DateUtil.formatChineseDate(dateTime, true, true);
        System.out.println(chineseDateTime);
    }

    /**
     * 获取Date对象的某个部分
     */
    @Test
    void test4() {
        DateTime dateTime = DateUtil.date();

        // 获得年的部分: 2024
        int year = DateUtil.year(dateTime);
        System.out.println(year);

        //获得月份，从0开始计数: 1
        int month = DateUtil.month(dateTime);
        System.out.println(month);

        //获得月份枚举: 二月
        Month monthEnum = DateUtil.monthEnum(dateTime);
        System.out.println(monthEnum.getDisplayName(TextStyle.FULL));
    }

    /**
     * 开始和结束时间
     * 有的时候我们需要获得每天的开始时间、结束时间，每月的开始和结束时间等等，DateUtil也提供了相关方法：
     */
    @Test
    void test5() {
        DateTime dateTime = DateUtil.date();

        // 一天的开始时间：2024-02-22 00:00:00
        DateTime beginOfDay = DateUtil.beginOfDay(dateTime);
        System.out.println(beginOfDay);
        // 一天的结束时间: 2024-02-22 23:59:59
        DateTime endOfDay = DateUtil.endOfDay(dateTime);
        System.out.println(endOfDay);

        // 本周的开始时间：2024-02-19 00:00:00
        DateTime beginOfWeek = DateUtil.beginOfWeek(dateTime);
        System.out.println(beginOfWeek);
        // 本周的开始时间：2024-02-25 23:59:59
        DateTime endOfWeek = DateUtil.endOfWeek(dateTime);
        System.out.println(endOfWeek);

        // 获取某月的开始时间: 2024-02-01 00:00:00
        DateTime beginOfMonth = DateUtil.beginOfMonth(dateTime);
        System.out.println(beginOfMonth);
        // 获取某月的结束时间：2024-02-29 23:59:59
        DateTime endOfMonth = DateUtil.endOfMonth(dateTime);
        System.out.println(endOfMonth);

        // 获取某季度的开始时间: 2024-01-01 00:00:00
        DateTime beginOfQuarter = DateUtil.beginOfQuarter(dateTime);
        System.out.println(beginOfQuarter);
        // 获取某季度的结束时间: 2024-03-31 23:59:59
        DateTime endOfQuarter = DateUtil.endOfQuarter(dateTime);
        System.out.println(endOfQuarter);
    }

    /**
     * 日期时间偏移
     * 日期或时间的偏移指针对某个日期增加或减少分、小时、天等等，达到日期变更的目的。Hutool也针对其做了大量封装
     */
    @Test
    void test6() {
        DateTime dateTime = DateUtil.date();

        // 时间偏移月，加2个月：2024-04-22 11:56:54
        DateTime offsetMonth = DateUtil.offsetMonth(dateTime, 2);
        System.out.println(offsetMonth);

        // 时间偏移周，加2周：2024-03-07 11:56:54
        DateTime offsetWeek = DateUtil.offsetWeek(dateTime, 2);
        System.out.println(offsetWeek);

        // 时间偏移天，减2天：2024-02-20 11:56:54
        DateTime offsetDay = DateUtil.offsetDay(dateTime, -2);
        System.out.println(offsetDay);

        // 时间偏移小时，加2小时：2024-02-22 13:56:54
        DateTime offsetHour = DateUtil.offsetHour(dateTime, 2);
        System.out.println(offsetHour);

        // 时间偏移分钟，加2分钟：2024-02-22 11:58:54
        DateTime offsetMinute = DateUtil.offsetMinute(dateTime, 2);
        System.out.println(offsetMinute);

        // 时间偏移秒，加2秒：2024-02-22 11:56:56
        DateTime offsetSecond = DateUtil.offsetSecond(dateTime, 2);
        System.out.println(offsetSecond);

        // 时间偏移毫秒，加2毫秒：2024-02-22 11:56:54.371 -> 2024-02-22 11:56:54.373
        DateTime offsetMillisecond = DateUtil.offsetMillisecond(dateTime, 2);
        System.out.println(DateUtil.format(dateTime, "yyyy-MM-dd HH:mm:ss.SSS"));
        System.out.println(DateUtil.format(offsetMillisecond, "yyyy-MM-dd HH:mm:ss.SSS"));

        // 针对当前时间，提供了简化的偏移方法（例如昨天、上周、上个月等）：
        //昨天
        System.out.println(DateUtil.yesterday());
        //明天
        System.out.println(DateUtil.tomorrow());
        //上周
        System.out.println(DateUtil.lastWeek());
        //下周
        System.out.println(DateUtil.nextWeek());
        //上个月
        System.out.println(DateUtil.lastMonth());
        //下个月
        System.out.println(DateUtil.nextMonth());
    }

    /**
     * 日期时间差
     * 有时候我们需要计算两个日期之间的时间差（相差天数、相差小时数等等），Hutool将此类方法封装为between方法：
     */
    @Test
    void test7() {
        DateTime dateTime1 = DateUtil.parse("2023-01-12 12:12:12");
        DateTime dateTime2 = DateUtil.parse("2024-02-08 08:08:08");

        // 相差年数
        long year = DateUtil.betweenYear(dateTime1, dateTime2, false);
        System.out.println(year);

        // 相差月数
        long month = DateUtil.betweenMonth(dateTime1, dateTime2, false);
        System.out.println(month);

        // 相差天数
        long day = DateUtil.betweenDay(dateTime1, dateTime2, false);
        System.out.println(day);

        // 相差小时数
        long hour = DateUtil.between(dateTime1, dateTime2, DateUnit.HOUR);
        System.out.println(hour);

        // 相差分钟数
        long minute = DateUtil.between(dateTime1, dateTime2, DateUnit.MINUTE);
        System.out.println(minute);

        // 相差秒数
        long second = DateUtil.between(dateTime1, dateTime2, DateUnit.SECOND);
        System.out.println(second);

        // 相差毫秒数
        long ms = DateUtil.betweenMs(dateTime1, dateTime2);
        System.out.println(ms);
    }

    /**
     * 格式化时间差
     * 有时候我们希望看到易读的时间差，比如XX天XX小时XX分XX秒，此时使用DateUtil.formatBetween方法：
     */
    @Test
    void test8() {
        DateTime dateTime1 = DateUtil.parse("2023-01-12 12:12:12");
        DateTime dateTime2 = DateUtil.parse("2024-02-08 08:08:08");

        // 相差天数
        String formatBetween = DateUtil.formatBetween(dateTime1, dateTime2, BetweenFormatter.Level.DAY);
        System.out.println(formatBetween);
    }

    /**
     * 星座和属相
     */
    @Test
    void test9() {
        // 星座
        String zodiac = DateUtil.getZodiac(4, 29);
        System.out.println(zodiac);
        // 生肖
        String chineseZodiac = DateUtil.getChineseZodiac(2000);
        System.out.println(chineseZodiac);
    }

    /**
     * 计时器
     */
    @Test
    void test10() {
        TimeInterval timer = DateUtil.timer();
        // ...
        ThreadUtil.sleep(1000);
        // 打印方便查看的格式
        String interval = timer.intervalPretty();
        System.out.println(interval);
    }

    /**
     * 生成时间列表
     */
    @Test
    void test11() {
        // 每2分钟生成一个时间
        DateTime startTime1 = DateUtil.parse("2024-02-01 08:00:00");
        DateTime endTime1 = DateUtil.parse("2024-02-01 09:00:00");
        List<DateTime> dateTimes1 = DateUtil.rangeToList(startTime1, endTime1, DateField.MINUTE, 2);
        System.out.println(dateTimes1);
        // 每1小时生成一个时间
        DateTime startTime2 = DateUtil.parse("2024-02-01 00:00:00");
        DateTime endTime2 = DateUtil.parse("2024-02-02 00:00:00");
        List<DateTime> dateTimes2 = DateUtil.rangeToList(startTime2, endTime2, DateField.HOUR, 2);
        System.out.println(dateTimes2);
        // 每天生成一个时间
        DateTime startTime3 = DateUtil.parse("2024-02-01 00:00:00");
        DateTime endTime3 = DateUtil.parse("2024-03-02 00:00:00");
        List<DateTime> dateTimes3 = DateUtil.rangeToList(startTime3, endTime3, DateField.DAY_OF_YEAR, 1);
        System.out.println(dateTimes3);
        // 每月生成一个时间
        DateTime startTime4 = DateUtil.parse("2023-01-01 00:00:00");
        DateTime endTime4 = DateUtil.parse("2024-04-11 00:00:00");
        List<DateTime> dateTimes4 = DateUtil.rangeToList(startTime4, endTime4, DateField.MONTH, 1);
        System.out.println(dateTimes4);
    }

    /**
     * 生成时间列表，返回字符串
     */
    @Test
    void test11_1() {
        // 近7日日期
        List<String> dateTimes = DateUtil.rangeFunc(DateUtil.offsetDay(DateUtil.date(), -7), DateUtil.offsetDay(DateUtil.date(), -1), DateField.DAY_OF_YEAR, time -> DateUtil.format(time, "yyyy-MM-dd"));
        System.out.println(dateTimes);
        // 今日时间
        DateTime startTime2 = DateUtil.parse("2024-02-01 00:00:00");
        DateTime endTime2 = DateUtil.parse("2024-02-02 00:00:00");
        List<String> dateTimes2 = DateUtil.rangeFunc(startTime2, endTime2, DateField.HOUR_OF_DAY, time -> DateUtil.format(time, "HH:00"));
        System.out.println(dateTimes2);
        // 近7月日期
        List<String> dateTimes3 = DateUtil.rangeFunc(DateUtil.offsetMonth(DateUtil.date(), -7), DateUtil.offsetMonth(DateUtil.date(), -1), DateField.MONTH, time -> DateUtil.format(time, "yyyy年MM月"));
        System.out.println(dateTimes3);
    }

    /**
     * 判断时间是否在某个时间段内
     */
    @Test
    void test12() {
        DateTime startTime = DateUtil.parse("09:00:00");
        DateTime endTime = DateUtil.parse("18:00:00");
        //DateTime currentTime = DateUtil.parse("09:00:00");
        DateTime currentTime = DateUtil.date();
        boolean isIn = DateUtil.isIn(currentTime, startTime, endTime);
        System.out.println(isIn);
    }
}
