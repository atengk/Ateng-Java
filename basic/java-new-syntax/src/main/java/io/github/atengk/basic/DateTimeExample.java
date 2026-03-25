package io.github.atengk.basic;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * LocalDate / LocalDateTime 常用操作（替代 Date）
 */
public class DateTimeExample {

    /**
     * 核心方法：日期时间常用操作
     */
    public static void dateTimeUsage() {

        // =========================
        // 1. 获取当前时间
        // =========================
        LocalDate date = LocalDate.now();                 // 当前日期
        LocalDateTime dateTime = LocalDateTime.now();     // 当前日期时间
        LocalTime time = LocalTime.now();                 // 当前时间

        System.out.println("当前日期：" + date);
        System.out.println("当前时间：" + time);
        System.out.println("当前日期时间：" + dateTime);


        // =========================
        // 2. 指定时间创建
        // =========================
        LocalDate customDate = LocalDate.of(2026, 3, 24);
        LocalDateTime customDateTime = LocalDateTime.of(2026, 3, 24, 12, 30, 0);

        System.out.println("\n指定日期：" + customDate);
        System.out.println("指定时间：" + customDateTime);


        // =========================
        // 3. 时间加减
        // =========================
        LocalDate nextDay = date.plusDays(1);
        LocalDate lastMonth = date.minusMonths(1);

        System.out.println("\n明天：" + nextDay);
        System.out.println("上个月：" + lastMonth);

        LocalDateTime plusHours = dateTime.plusHours(2);
        System.out.println("2小时后：" + plusHours);


        // =========================
        // 4. 时间比较
        // =========================
        boolean isAfter = date.isAfter(LocalDate.of(2025, 1, 1));
        boolean isBefore = date.isBefore(LocalDate.of(2030, 1, 1));

        System.out.println("\n是否在2025之后：" + isAfter);
        System.out.println("是否在2030之前：" + isBefore);


        // =========================
        // 5. 时间差计算（ChronoUnit）
        // =========================
        long days = ChronoUnit.DAYS.between(
                LocalDate.of(2026, 1, 1),
                date
        );

        System.out.println("\n相差天数：" + days);

        long hours = ChronoUnit.HOURS.between(
                LocalDateTime.now().minusHours(5),
                LocalDateTime.now()
        );

        System.out.println("相差小时：" + hours);


        // =========================
        // 6. 时间格式化
        // =========================
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String formatted = dateTime.format(formatter);

        System.out.println("\n格式化：" + formatted);


        // =========================
        // 7. 字符串转时间（解析）
        // =========================
        LocalDateTime parsed = LocalDateTime.parse("2026-03-24 10:20:30", formatter);

        System.out.println("解析：" + parsed);


        // =========================
        // 8. 获取时间字段
        // =========================
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        System.out.println("\n年：" + year + " 月：" + month + " 日：" + day);


        // =========================
        // 9. 转换（LocalDate ↔ LocalDateTime）
        // =========================
        LocalDateTime startOfDay = date.atStartOfDay(); // 当天开始
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        System.out.println("\n当天开始：" + startOfDay);
        System.out.println("当天结束：" + endOfDay);


        // =========================
        // 10. 时区（项目常用）
        // =========================
        ZonedDateTime zoned = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));

        System.out.println("\n东京时间：" + zoned);


        // =========================
        // 11. 时间戳转换
        // =========================
        long timestamp = System.currentTimeMillis();

        LocalDateTime fromTimestamp = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        System.out.println("\n时间戳转时间：" + fromTimestamp);


        // =========================
        // 12. 判断闰年
        // =========================
        boolean leapYear = date.isLeapYear();

        System.out.println("\n是否闰年：" + leapYear);
    }

    public static void main(String[] args) {
        dateTimeUsage();
    }
}
