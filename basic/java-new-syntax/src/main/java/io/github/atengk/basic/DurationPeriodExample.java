package io.github.atengk.basic;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化 & 时间计算（Duration / Period）
 */
public class DurationPeriodExample {

    /**
     * 核心方法：时间格式化 + Duration + Period 使用
     */
    public static void timeCalcUsage() {

        // =========================
        // 1. 时间格式化（DateTimeFormatter）
        // =========================
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatted = now.format(formatter);

        System.out.println("格式化时间：" + formatted);


        // =========================
        // 2. Duration（时间差：时分秒）
        // =========================
        LocalDateTime start = LocalDateTime.now().minusHours(2).minusMinutes(30);

        Duration duration = Duration.between(start, now);

        long hours = duration.toHours();       // 小时
        long minutes = duration.toMinutes();   // 分钟
        long seconds = duration.getSeconds();  // 秒

        System.out.println("\nDuration：");
        System.out.println("小时：" + hours);
        System.out.println("分钟：" + minutes);
        System.out.println("秒：" + seconds);


        // =========================
        // 3. Period（时间差：年月日）
        // =========================
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.now();

        Period period = Period.between(startDate, endDate);

        System.out.println("\nPeriod：");
        System.out.println("年：" + period.getYears());
        System.out.println("月：" + period.getMonths());
        System.out.println("日：" + period.getDays());


        // =========================
        // 4. Duration 实战（接口耗时统计）
        // =========================
        Instant begin = Instant.now();

        // 模拟接口耗时
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Instant end = Instant.now();

        long cost = Duration.between(begin, end).toMillis();

        System.out.println("\n接口耗时：" + cost + " ms");


        // =========================
        // 5. Period 实战（年龄计算）
        // =========================
        LocalDate birthday = LocalDate.of(1995, 5, 20);

        int age = Period.between(birthday, LocalDate.now()).getYears();

        System.out.println("\n年龄：" + age);


        // =========================
        // 6. Duration 转换（单位转换）
        // =========================
        Duration d = Duration.ofSeconds(3600);

        System.out.println("\n转换：");
        System.out.println("小时：" + d.toHours());
        System.out.println("分钟：" + d.toMinutes());
        System.out.println("秒：" + d.getSeconds());


        // =========================
        // 7. 格式化 Duration（自定义）
        // =========================
        Duration custom = Duration.ofSeconds(3661);

        long h = custom.toHours();
        long m = custom.toMinutesPart();  // JDK9+
        long s = custom.toSecondsPart();

        String formattedDuration = String.format("%02d:%02d:%02d", h, m, s);

        System.out.println("\n格式化 Duration：" + formattedDuration);


        // =========================
        // 8. 时间加减（结合 Duration / Period）
        // =========================
        LocalDateTime future = now.plus(Duration.ofHours(5));
        LocalDate past = LocalDate.now().minus(Period.ofDays(10));

        System.out.println("\n5小时后：" + future);
        System.out.println("10天前：" + past);


        // =========================
        // 9. 判断时间区间
        // =========================
        LocalDateTime t1 = LocalDateTime.now().minusHours(1);
        LocalDateTime t2 = LocalDateTime.now().plusHours(1);

        boolean inRange = now.isAfter(t1) && now.isBefore(t2);

        System.out.println("\n是否在区间内：" + inRange);


        // =========================
        // 10. 项目常用：过期判断
        // =========================
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(10);

        boolean expired = Duration.between(expireTime, LocalDateTime.now()).toMinutes() > 5;

        System.out.println("是否过期：" + expired);
    }

    public static void main(String[] args) {
        timeCalcUsage();
    }
}