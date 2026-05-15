# 日期时间 API

Java 日期时间 API 用于处理日期、时间、时间戳、时区、格式化、解析和时间计算等场景。Java 8 之前主要使用 `Date`、`Calendar`、`SimpleDateFormat`，Java 8 之后推荐使用 `java.time` 包。

在新项目中，优先使用 `java.time` 包。只有在兼容老系统、第三方接口、数据库驱动或历史代码时，才需要和旧版日期时间 API 进行转换。

## 日期时间 API 概述

日期时间 API 可以分为旧版日期时间 API 和新版日期时间 API。旧版 API 主要位于 `java.util` 和 `java.text` 包中，新版 API 主要位于 `java.time` 包中。

新版日期时间 API 的类型划分更清晰，例如日期、时间、日期时间、时间戳、时区日期时间分别由不同类型表示，避免了旧版 API 中一个类型承担过多职责的问题。

常见 API 对比如下：

| API 类型 | 常用类             | 推荐程度             | 说明                 |
| -------- | ------------------ | -------------------- | -------------------- |
| 旧版 API | `Date`             | 不推荐新代码直接使用 | 可用于兼容历史接口   |
| 旧版 API | `Calendar`         | 不推荐               | API 复杂，设计不直观 |
| 旧版 API | `SimpleDateFormat` | 不推荐多线程共享     | 线程不安全           |
| 新版 API | `LocalDate`        | 推荐                 | 表示日期             |
| 新版 API | `LocalTime`        | 推荐                 | 表示时间             |
| 新版 API | `LocalDateTime`    | 推荐                 | 表示本地日期时间     |
| 新版 API | `Instant`          | 推荐                 | 表示 UTC 时间戳      |
| 新版 API | `ZonedDateTime`    | 推荐                 | 表示带时区日期时间   |

### 旧版日期时间 API 的问题

旧版日期时间 API 最大的问题是设计不清晰、对象可变、线程不安全，并且时区处理不够直观。

`Date` 虽然名字叫日期，但它实际表示的是一个时间点，内部保存的是从 `1970-01-01T00:00:00Z` 开始的毫秒数。它既不是单纯的日期，也不是单纯的时间。

`Calendar` 虽然解决了一部分日期计算问题，但 API 设计复杂，例如月份从 `0` 开始，`Calendar.JANUARY` 才表示一月，这种设计容易导致开发错误。

`SimpleDateFormat` 是线程不安全的。如果多个线程共享同一个 `SimpleDateFormat` 实例，可能会出现解析异常或格式化结果错误。

下面示例演示旧版 `Date` 的可变性，以及使用 Hutool 对旧版 `Date` 进行格式化。

```java
package io.github.atengk.datetime;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;

import java.util.Date;

/**
 * 旧版日期时间 API 示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LegacyDateExample {

    public static void main(String[] args) {
        Date now = new Date();
        System.out.println("当前时间：" + DateUtil.format(now, DatePattern.NORM_DATETIME_PATTERN));

        // Date 是可变对象，可以直接修改内部时间戳
        now.setTime(0L);
        System.out.println("修改后的时间：" + DateUtil.format(now, DatePattern.NORM_DATETIME_PATTERN));
    }
}
```

旧版 API 的主要问题如下：

| 问题         | 说明                                              |
| ------------ | ------------------------------------------------- |
| 可变性       | `Date` 和 `Calendar` 都是可变对象，容易产生副作用 |
| 线程不安全   | `SimpleDateFormat` 在多线程环境下不能安全共享     |
| 设计不直观   | `Date` 语义不清晰，`Calendar` 月份从 `0` 开始     |
| 时区处理复杂 | 时区、偏移量、本地时间概念混杂                    |
| API 分散     | 日期计算、格式化、解析能力分散在多个类中          |

### java.time 包

`java.time` 是 Java 8 引入的新日期时间 API，基于 JSR-310 规范设计。它提供了更加清晰、不可变、线程安全的日期时间模型。

`java.time` 包的核心特点：

| 特点       | 说明                                            |
| ---------- | ----------------------------------------------- |
| 类型清晰   | 日期、时间、时间戳、时区分别由不同类型表示      |
| 不可变     | 大多数日期时间类都是不可变对象                  |
| 线程安全   | 不可变对象天然适合多线程环境                    |
| 标准统一   | 默认支持 ISO-8601 标准                          |
| 功能完整   | 支持格式化、解析、计算、时区、时间间隔          |
| 兼容旧 API | 可以和 `Date`、`Calendar`、`Timestamp` 互相转换 |

`java.time` 常用包如下：

| 包名                 | 说明                       |
| -------------------- | -------------------------- |
| `java.time`          | 核心日期时间类型           |
| `java.time.format`   | 日期时间格式化与解析       |
| `java.time.temporal` | 日期时间字段、单位、调整器 |
| `java.time.zone`     | 时区规则相关类型           |

下面示例演示 `java.time` 中几个常用类型的基本使用。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * java.time 包基础示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class JavaTimeOverviewExample {

    public static void main(String[] args) {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        LocalDateTime dateTime = LocalDateTime.now();
        Instant instant = Instant.now();
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        System.out.println("当前日期：" + date);
        System.out.println("当前时间：" + time);
        System.out.println("当前日期时间：" + dateTime);
        System.out.println("当前 UTC 时间戳：" + instant);
        System.out.println("当前带时区日期时间：" + zonedDateTime);
    }
}
```

### 日期时间类型选择

日期时间类型的选择应根据业务语义决定，不建议所有场景都使用 `LocalDateTime`。

常用选择规则如下：

| 业务场景                     | 推荐类型        | 示例                                       |
| ---------------------------- | --------------- | ------------------------------------------ |
| 生日、节假日、统计日期       | `LocalDate`     | `2026-05-15`                               |
| 每天固定时间                 | `LocalTime`     | `09:30:00`                                 |
| 本地业务日期时间             | `LocalDateTime` | `2026-05-15 14:30:00`                      |
| 日志时间、事件时间、消息时间 | `Instant`       | `2026-05-15T06:30:00Z`                     |
| 跨时区会议、航班、国际化业务 | `ZonedDateTime` | `2026-05-15T14:30:00+08:00[Asia/Shanghai]` |

选择建议：

| 类型            | 是否包含日期 | 是否包含时间 | 是否包含时区 | 适用场景           |
| --------------- | ------------ | ------------ | ------------ | ------------------ |
| `LocalDate`     | 是           | 否           | 否           | 只关心年月日       |
| `LocalTime`     | 否           | 是           | 否           | 只关心时分秒       |
| `LocalDateTime` | 是           | 是           | 否           | 本地业务时间       |
| `Instant`       | 是           | 是           | UTC          | 机器时间、时间戳   |
| `ZonedDateTime` | 是           | 是           | 是           | 明确地区时区的时间 |

开发中需要注意：

- 只表示日期时，不要使用 `LocalDateTime`
- 表示全局唯一时间点时，不要使用 `LocalDateTime`
- 需要跨时区传输时，优先使用 `Instant`、`OffsetDateTime` 或 `ZonedDateTime`
- 数据库中的 `DATE` 字段通常适合映射为 `LocalDate`
- 数据库中的 `TIME` 字段通常适合映射为 `LocalTime`
- 数据库中的 `DATETIME` 或 `TIMESTAMP` 字段需要结合数据库和业务时区规则选择类型

## 常用日期时间类型

常用日期时间类型包括 `LocalDate`、`LocalTime`、`LocalDateTime`、`Instant` 和 `ZonedDateTime`。这些类型覆盖了大部分 Java 后端开发中的日期时间处理场景。

### LocalDate

`LocalDate` 表示不带时间、不带时区的日期，只包含年、月、日。

适用场景：

- 用户生日
- 节假日
- 账单日期
- 统计日期
- 合同开始日期
- 合同结束日期

下面示例演示 `LocalDate` 的创建、解析、计算、比较和格式化。

```java
package io.github.atengk.datetime;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * LocalDate 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LocalDateExample {

    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        LocalDate birthday = LocalDate.of(1998, 8, 18);
        LocalDate parsedDate = LocalDate.parse("2026-05-15");

        LocalDate nextWeek = today.plusWeeks(1);
        LocalDate lastMonth = today.minusMonths(1);

        boolean after = parsedDate.isAfter(birthday);
        boolean before = parsedDate.isBefore(today);
        boolean equal = parsedDate.isEqual(LocalDate.of(2026, 5, 15));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        String formattedDate = parsedDate.format(formatter);

        System.out.println("当前日期：" + today);
        System.out.println("生日日期：" + birthday);
        System.out.println("解析日期：" + parsedDate);
        System.out.println("一周后：" + nextWeek);
        System.out.println("上个月：" + lastMonth);
        System.out.println("是否晚于生日：" + after);
        System.out.println("是否早于今天：" + before);
        System.out.println("是否等于指定日期：" + equal);
        System.out.println("格式化日期：" + formattedDate);
    }
}
```

常用方法：

| 方法                             | 说明                 |
| -------------------------------- | -------------------- |
| `LocalDate.now()`                | 获取当前日期         |
| `LocalDate.of(year, month, day)` | 创建指定日期         |
| `LocalDate.parse(text)`          | 解析 ISO 日期字符串  |
| `plusDays(days)`                 | 增加天数             |
| `minusMonths(months)`            | 减少月份             |
| `isAfter(date)`                  | 判断是否晚于指定日期 |
| `isBefore(date)`                 | 判断是否早于指定日期 |
| `format(formatter)`              | 格式化日期           |

开发注意事项：

- `LocalDate` 不包含时间
- `LocalDate` 不包含时区
- `LocalDate` 不能表示某个具体瞬间
- 数据库中一般对应 `DATE` 类型

### LocalTime

`LocalTime` 表示不带日期、不带时区的时间，只包含时、分、秒、纳秒。

适用场景：

- 每天营业开始时间
- 每天营业结束时间
- 每天固定执行时间
- 打卡规则时间
- 限流窗口时间

下面示例演示 `LocalTime` 的创建、解析、计算、比较和格式化。

```java
package io.github.atengk.datetime;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * LocalTime 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LocalTimeExample {

    public static void main(String[] args) {
        LocalTime now = LocalTime.now();
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 30, 0);
        LocalTime parsedTime = LocalTime.parse("14:30:00");

        LocalTime afterThirtyMinutes = parsedTime.plusMinutes(30);
        LocalTime beforeTwoHours = parsedTime.minusHours(2);

        boolean inWorkTime = !now.isBefore(startTime) && !now.isAfter(endTime);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = parsedTime.format(formatter);

        System.out.println("当前时间：" + now);
        System.out.println("开始时间：" + startTime);
        System.out.println("结束时间：" + endTime);
        System.out.println("解析时间：" + parsedTime);
        System.out.println("30 分钟后：" + afterThirtyMinutes);
        System.out.println("2 小时前：" + beforeTwoHours);
        System.out.println("当前是否在时间范围内：" + inWorkTime);
        System.out.println("格式化时间：" + formattedTime);
    }
}
```

常用方法：

| 方法                                 | 说明                 |
| ------------------------------------ | -------------------- |
| `LocalTime.now()`                    | 获取当前时间         |
| `LocalTime.of(hour, minute, second)` | 创建指定时间         |
| `LocalTime.parse(text)`              | 解析时间字符串       |
| `plusHours(hours)`                   | 增加小时             |
| `plusMinutes(minutes)`               | 增加分钟             |
| `minusSeconds(seconds)`              | 减少秒数             |
| `isBefore(time)`                     | 判断是否早于指定时间 |
| `isAfter(time)`                      | 判断是否晚于指定时间 |

开发注意事项：

- `LocalTime` 不包含日期
- `LocalTime` 不包含时区
- 处理 `22:00` 到次日 `02:00` 这种跨天时间段时，需要单独处理跨天逻辑
- 数据库中一般对应 `TIME` 类型

### LocalDateTime

`LocalDateTime` 表示不带时区的日期时间，包含年、月、日、时、分、秒、纳秒。

适用场景：

- 订单创建时间
- 数据更新时间
- 审批时间
- 预约时间
- 本地系统业务时间

需要注意的是，`LocalDateTime` 不包含时区信息。它表示的是“某个本地日期时间”，不能直接表示全局唯一时间点。

下面示例演示 `LocalDateTime` 的创建、解析、日期时间组合、计算和格式化。

```java
package io.github.atengk.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * LocalDateTime 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LocalDateTimeExample {

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime appointTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        LocalDate date = LocalDate.of(2026, 5, 15);
        LocalTime time = LocalTime.of(18, 0, 0);
        LocalDateTime combinedDateTime = LocalDateTime.of(date, time);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime parsedDateTime = LocalDateTime.parse("2026-05-15 14:30:00", formatter);

        LocalDateTime expireTime = parsedDateTime.plusDays(7);
        LocalDateTime beforeOneHour = parsedDateTime.minusHours(1);

        System.out.println("当前日期时间：" + now);
        System.out.println("预约时间：" + appointTime);
        System.out.println("组合日期时间：" + combinedDateTime);
        System.out.println("解析日期时间：" + parsedDateTime);
        System.out.println("过期时间：" + expireTime);
        System.out.println("提前一小时：" + beforeOneHour);
        System.out.println("格式化结果：" + parsedDateTime.format(formatter));
    }
}
```

常用方法：

| 方法                       | 说明                 |
| -------------------------- | -------------------- |
| `LocalDateTime.now()`      | 获取当前本地日期时间 |
| `LocalDateTime.of(...)`    | 创建指定日期时间     |
| `LocalDateTime.parse(...)` | 解析日期时间字符串   |
| `toLocalDate()`            | 获取日期部分         |
| `toLocalTime()`            | 获取时间部分         |
| `plusDays(days)`           | 增加天数             |
| `minusHours(hours)`        | 减少小时             |
| `format(formatter)`        | 格式化日期时间       |

开发注意事项：

- `LocalDateTime` 不包含时区
- 不建议用它表示跨时区的绝对时间
- 前后端传输时要约定清楚时区语义
- 数据库中一般对应 `DATETIME` 或不带时区的时间字段

### Instant

`Instant` 表示 UTC 时间线上的一个瞬间，通常用于表示机器时间、时间戳和全局唯一时间点。

适用场景：

- 日志记录时间
- 消息产生时间
- 事件发生时间
- 数据同步时间
- 分布式系统中的统一时间点

`Instant` 默认使用 UTC，不依赖本地时区。它适合用于系统内部存储和跨系统传输。

下面示例演示 `Instant` 的创建、时间戳转换、时间计算，以及与 `LocalDateTime` 的转换。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Instant 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class InstantExample {

    public static void main(String[] args) {
        Instant now = Instant.now();
        Instant specified = Instant.ofEpochMilli(1778826600000L);

        long epochMilli = now.toEpochMilli();
        Instant afterTenSeconds = now.plusSeconds(10);
        Instant beforeOneHour = now.minusSeconds(3600);

        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        LocalDateTime localDateTime = LocalDateTime.ofInstant(now, zoneId);

        Instant instantFromLocalDateTime = localDateTime.atZone(zoneId).toInstant();

        System.out.println("当前 UTC 时间戳：" + now);
        System.out.println("指定毫秒时间戳：" + specified);
        System.out.println("当前毫秒值：" + epochMilli);
        System.out.println("10 秒后：" + afterTenSeconds);
        System.out.println("1 小时前：" + beforeOneHour);
        System.out.println("转换为本地日期时间：" + localDateTime);
        System.out.println("本地日期时间转回 Instant：" + instantFromLocalDateTime);
    }
}
```

常用方法：

| 方法                             | 说明                           |
| -------------------------------- | ------------------------------ |
| `Instant.now()`                  | 获取当前 UTC 时间戳            |
| `Instant.ofEpochMilli(millis)`   | 根据毫秒时间戳创建             |
| `Instant.ofEpochSecond(seconds)` | 根据秒级时间戳创建             |
| `toEpochMilli()`                 | 转为毫秒时间戳                 |
| `plusSeconds(seconds)`           | 增加秒数                       |
| `minusSeconds(seconds)`          | 减少秒数                       |
| `atZone(zoneId)`                 | 结合时区转换为 `ZonedDateTime` |

开发注意事项：

- `Instant` 表示 UTC 时间点
- `Instant` 不适合直接展示给用户
- 展示前通常需要结合 `ZoneId` 转换成本地时间
- 分布式系统中推荐使用 `Instant` 表示事件发生时间

### ZonedDateTime

`ZonedDateTime` 表示带时区的日期时间，由本地日期时间、时区和时区规则共同组成。

适用场景：

- 国际化系统
- 跨时区会议
- 航班时间
- 海外业务订单
- 需要明确地区时区的定时任务

`ZonedDateTime` 不只是简单的日期时间加偏移量，它还包含具体时区规则。例如 `Asia/Shanghai`、`America/New_York` 都是具体时区，不同地区可能存在夏令时规则。

下面示例演示 `ZonedDateTime` 的创建、时区转换、格式化，以及转换为 `Instant`。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ZonedDateTime 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ZonedDateTimeExample {

    public static void main(String[] args) {
        ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
        ZoneId newYorkZone = ZoneId.of("America/New_York");

        ZonedDateTime shanghaiTime = ZonedDateTime.now(shanghaiZone);
        ZonedDateTime tokyoTime = shanghaiTime.withZoneSameInstant(tokyoZone);
        ZonedDateTime newYorkTime = shanghaiTime.withZoneSameInstant(newYorkZone);

        Instant instant = shanghaiTime.toInstant();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String formattedShanghaiTime = shanghaiTime.format(formatter);

        System.out.println("上海时间：" + shanghaiTime);
        System.out.println("东京时间：" + tokyoTime);
        System.out.println("纽约时间：" + newYorkTime);
        System.out.println("转换为 Instant：" + instant);
        System.out.println("格式化上海时间：" + formattedShanghaiTime);
    }
}
```

常用方法：

| 方法                          | 说明                             |
| ----------------------------- | -------------------------------- |
| `ZonedDateTime.now()`         | 获取系统默认时区的当前日期时间   |
| `ZonedDateTime.now(zoneId)`   | 获取指定时区的当前日期时间       |
| `ZoneId.of(zone)`             | 创建时区对象                     |
| `withZoneSameInstant(zoneId)` | 保持同一时间点，转换到另一个时区 |
| `toInstant()`                 | 转换为 UTC 时间戳                |
| `format(formatter)`           | 格式化带时区日期时间             |

开发注意事项：

- 跨时区场景优先考虑 `ZonedDateTime`
- `withZoneSameInstant` 表示同一个瞬间换算到另一个时区
- 不要简单地手动加减小时处理时区
- 夏令时地区必须使用 `ZoneId`，不要只使用固定偏移量
- 存储时可以使用 `Instant`，展示时再转换为用户所在时区



## 日期时间创建

日期时间创建主要包括获取当前日期时间、创建指定日期时间、从字符串解析日期时间，以及根据时间戳转换日期时间。实际开发中，建议根据业务场景选择合适的类型，而不是统一使用 `LocalDateTime`。

### 当前日期时间

获取当前日期时间时，常用 `now()` 方法。不同类型的 `now()` 返回不同语义的时间。

| 类型                  | 示例               | 说明             |
| --------------------- | ------------------ | ---------------- |
| `LocalDate.now()`     | 当前日期           | 不包含时间和时区 |
| `LocalTime.now()`     | 当前时间           | 不包含日期和时区 |
| `LocalDateTime.now()` | 当前本地日期时间   | 不包含时区       |
| `Instant.now()`       | 当前 UTC 时间戳    | 表示全局时间点   |
| `ZonedDateTime.now()` | 当前带时区日期时间 | 包含时区信息     |

下面示例演示常用当前日期时间的创建方式。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 当前日期时间创建示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CurrentDateTimeExample {

    public static void main(String[] args) {
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        LocalDateTime currentDateTime = LocalDateTime.now();
        Instant currentInstant = Instant.now();

        ZonedDateTime currentShanghaiTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        ZonedDateTime currentTokyoTime = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));

        System.out.println("当前日期：" + currentDate);
        System.out.println("当前时间：" + currentTime);
        System.out.println("当前本地日期时间：" + currentDateTime);
        System.out.println("当前 UTC 时间戳：" + currentInstant);
        System.out.println("当前上海时间：" + currentShanghaiTime);
        System.out.println("当前东京时间：" + currentTokyoTime);
    }
}
```

开发注意事项：

- `LocalDateTime.now()` 使用系统默认时区生成本地日期时间，但结果本身不保存时区信息。
- `Instant.now()` 表示 UTC 时间点，更适合日志、事件、消息、审计字段。
- 跨时区业务建议显式指定 `ZoneId`，不要依赖服务器默认时区。

### 指定日期时间

指定日期时间通常使用 `of()` 方法。它适合用于构造固定日期、业务规则时间、测试数据、定时任务时间等。

下面示例演示指定日期、指定时间、指定日期时间和指定时区日期时间的创建方式。

```java
package io.github.atengk.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 指定日期时间创建示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SpecificDateTimeExample {

    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2026, 5, 15);
        LocalTime time = LocalTime.of(14, 30, 0);
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime, zoneId);

        LocalDateTime combinedDateTime = LocalDateTime.of(date, time);

        System.out.println("指定日期：" + date);
        System.out.println("指定时间：" + time);
        System.out.println("指定日期时间：" + dateTime);
        System.out.println("指定时区日期时间：" + zonedDateTime);
        System.out.println("日期和时间组合：" + combinedDateTime);
    }
}
```

常用创建方法：

| 方法                                      | 说明                 |
| ----------------------------------------- | -------------------- |
| `LocalDate.of(year, month, day)`          | 创建指定日期         |
| `LocalTime.of(hour, minute, second)`      | 创建指定时间         |
| `LocalDateTime.of(...)`                   | 创建指定日期时间     |
| `ZonedDateTime.of(localDateTime, zoneId)` | 创建指定时区日期时间 |
| `LocalDateTime.of(localDate, localTime)`  | 日期和时间组合       |

开发注意事项：

- `LocalDate.of(2026, 13, 1)` 会抛出 `DateTimeException`，因为月份不合法。
- `LocalTime.of(24, 0, 0)` 会抛出异常，小时范围应为 `0` 到 `23`。
- 创建固定测试时间时，建议使用 `of()`，不要使用 `now()`，避免测试结果不稳定。

### 字符串解析

字符串解析用于将接口参数、配置文件参数、用户输入等字符串转换为日期时间对象。标准 ISO 格式可以直接使用 `parse()`，非标准格式需要指定 `DateTimeFormatter`。

下面示例演示日期、时间、日期时间字符串的解析方式，并使用 Hutool 的 `StrUtil` 做基础空值校验。

```java
package io.github.atengk.datetime;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 字符串解析日期时间示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DateTimeParseExample {

    public static void main(String[] args) {
        LocalDate date = LocalDate.parse("2026-05-15");
        LocalTime time = LocalTime.parse("14:30:00");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);
        LocalDateTime dateTime = parseDateTime("2026-05-15 14:30:00", dateTimeFormatter);

        System.out.println("解析日期：" + date);
        System.out.println("解析时间：" + time);
        System.out.println("解析日期时间：" + dateTime);
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateTimeText 日期时间字符串
     * @param formatter 格式化器
     * @return 日期时间
     */
    public static LocalDateTime parseDateTime(String dateTimeText, DateTimeFormatter formatter) {
        if (StrUtil.isBlank(dateTimeText)) {
            throw new IllegalArgumentException("日期时间字符串不能为空");
        }
        return LocalDateTime.parse(dateTimeText, formatter);
    }
}
```

常用解析方式：

| 字符串                | 解析方式                    | 说明             |
| --------------------- | --------------------------- | ---------------- |
| `2026-05-15`          | `LocalDate.parse(text)`     | ISO 日期格式     |
| `14:30:00`            | `LocalTime.parse(text)`     | ISO 时间格式     |
| `2026-05-15T14:30:00` | `LocalDateTime.parse(text)` | ISO 日期时间格式 |
| `2026-05-15 14:30:00` | 指定 `DateTimeFormatter`    | 常用业务格式     |

开发注意事项：

- `LocalDateTime.parse("2026-05-15 14:30:00")` 会失败，因为默认解析格式要求中间是 `T`。
- 业务接口中建议统一日期时间格式，例如 `yyyy-MM-dd HH:mm:ss`。
- 解析外部传入参数时，应处理空字符串、格式错误和非法日期。

### 时间戳转换

时间戳转换常用于前后端交互、日志记录、消息队列、数据库存储和第三方接口对接。Java 中推荐使用 `Instant` 表示时间戳语义。

时间戳常见单位：

| 单位 | 示例            | 说明                       |
| ---- | --------------- | -------------------------- |
| 秒   | `1778826600`    | Unix 秒级时间戳            |
| 毫秒 | `1778826600000` | Java 常用毫秒时间戳        |
| 纳秒 | 精度更高        | `Instant` 内部支持秒和纳秒 |

下面示例演示时间戳、`Instant`、`LocalDateTime` 之间的转换。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间戳转换示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TimestampConvertExample {

    public static void main(String[] args) {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");

        long epochMilli = 1778826600000L;
        long epochSecond = 1778826600L;

        Instant instantFromMilli = Instant.ofEpochMilli(epochMilli);
        Instant instantFromSecond = Instant.ofEpochSecond(epochSecond);

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instantFromMilli, zoneId);
        Instant instant = localDateTime.atZone(zoneId).toInstant();

        long convertedEpochMilli = instant.toEpochMilli();

        System.out.println("毫秒时间戳转 Instant：" + instantFromMilli);
        System.out.println("秒级时间戳转 Instant：" + instantFromSecond);
        System.out.println("Instant 转 LocalDateTime：" + localDateTime);
        System.out.println("LocalDateTime 转 Instant：" + instant);
        System.out.println("Instant 转毫秒时间戳：" + convertedEpochMilli);
    }
}
```

开发注意事项：

- 秒级时间戳使用 `Instant.ofEpochSecond()`。
- 毫秒时间戳使用 `Instant.ofEpochMilli()`。
- `Instant` 转 `LocalDateTime` 必须指定 `ZoneId`。
- `LocalDateTime` 转时间戳也必须指定 `ZoneId`，否则无法确定它对应时间线上的哪一个瞬间。
- 前端传时间戳时，需要明确单位是秒还是毫秒。

## 日期时间格式化

日期时间格式化用于日期时间对象和字符串之间的转换。Java 8 之后推荐使用 `DateTimeFormatter`，它是线程安全的，比 `SimpleDateFormat` 更适合在多线程环境中使用。

### DateTimeFormatter

`DateTimeFormatter` 位于 `java.time.format` 包中，用于格式化和解析 `java.time` 中的日期时间类型。

常用创建方式：

| 创建方式                                | 说明                 |
| --------------------------------------- | -------------------- |
| `DateTimeFormatter.ISO_LOCAL_DATE`      | ISO 本地日期格式     |
| `DateTimeFormatter.ISO_LOCAL_TIME`      | ISO 本地时间格式     |
| `DateTimeFormatter.ISO_LOCAL_DATE_TIME` | ISO 本地日期时间格式 |
| `DateTimeFormatter.ofPattern(pattern)`  | 自定义格式模板       |

下面示例演示 `DateTimeFormatter` 的基本使用方式。

```java
package io.github.atengk.datetime;

import cn.hutool.core.date.DatePattern;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DateTimeFormatter 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DateTimeFormatterExample {

    public static void main(String[] args) {
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        DateTimeFormatter normalFormatter = DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);
        DateTimeFormatter chineseFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分ss秒");

        System.out.println("ISO 格式：" + dateTime.format(isoFormatter));
        System.out.println("常用格式：" + dateTime.format(normalFormatter));
        System.out.println("中文格式：" + dateTime.format(chineseFormatter));
    }
}
```

开发注意事项：

- `DateTimeFormatter` 是线程安全的，可以定义为常量复用。
- `DateTimeFormatter.ofPattern()` 中的模板大小写敏感。
- `yyyy` 表示年，`MM` 表示月，`dd` 表示日，`HH` 表示 24 小时制小时。
- 不建议继续使用共享的 `SimpleDateFormat`。

### 日期时间转字符串

日期时间转字符串通常用于接口返回、页面展示、日志输出和文件导出。推荐使用 `format()` 方法。

下面示例演示 `LocalDate`、`LocalTime`、`LocalDateTime`、`ZonedDateTime` 转字符串。

```java
package io.github.atengk.datetime;

import cn.hutool.core.date.DatePattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间转字符串示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DateTimeToStringExample {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);
    private static final DateTimeFormatter ZONED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2026, 5, 15);
        LocalTime time = LocalTime.of(14, 30, 0);
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime, ZoneId.of("Asia/Shanghai"));

        String dateText = date.format(DATE_FORMATTER);
        String timeText = time.format(TIME_FORMATTER);
        String dateTimeText = dateTime.format(DATE_TIME_FORMATTER);
        String zonedDateTimeText = zonedDateTime.format(ZONED_FORMATTER);

        System.out.println("日期字符串：" + dateText);
        System.out.println("时间字符串：" + timeText);
        System.out.println("日期时间字符串：" + dateTimeText);
        System.out.println("带时区日期时间字符串：" + zonedDateTimeText);
    }
}
```

开发注意事项：

- 接口返回日期时间字符串时，建议统一格式。
- 展示给用户的时间应根据用户时区转换后再格式化。
- `Instant` 不建议直接格式化展示，通常先转换为 `ZonedDateTime` 或 `LocalDateTime`。

### 字符串转日期时间

字符串转日期时间通常用于处理请求参数、配置项、导入文件数据和第三方接口数据。对于非 ISO 格式字符串，必须指定匹配的格式模板。

下面示例演示字符串转不同日期时间类型。

```java
package io.github.atengk.datetime;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 字符串转日期时间示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StringToDateTimeExample {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);

    public static void main(String[] args) {
        LocalDate date = parseDate("2026-05-15");
        LocalTime time = parseTime("14:30:00");
        LocalDateTime dateTime = parseDateTime("2026-05-15 14:30:00");

        System.out.println("日期：" + date);
        System.out.println("时间：" + time);
        System.out.println("日期时间：" + dateTime);
    }

    /**
     * 解析日期字符串
     *
     * @param text 日期字符串
     * @return 日期
     */
    public static LocalDate parseDate(String text) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("日期字符串不能为空");
        }
        return LocalDate.parse(text, DATE_FORMATTER);
    }

    /**
     * 解析时间字符串
     *
     * @param text 时间字符串
     * @return 时间
     */
    public static LocalTime parseTime(String text) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }
        return LocalTime.parse(text, TIME_FORMATTER);
    }

    /**
     * 解析日期时间字符串
     *
     * @param text 日期时间字符串
     * @return 日期时间
     */
    public static LocalDateTime parseDateTime(String text) {
        if (StrUtil.isBlank(text)) {
            throw new IllegalArgumentException("日期时间字符串不能为空");
        }
        return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
    }
}
```

开发注意事项：

- 字符串格式必须和 `DateTimeFormatter` 模板完全匹配。
- `2026-5-15` 和 `yyyy-MM-dd` 不完全匹配，可能解析失败。
- 接口参数建议由统一工具类处理，避免每个业务类重复写解析逻辑。
- 对外部数据进行解析时，应捕获 `DateTimeParseException` 并返回明确错误信息。

### 常用格式模板

日期时间格式模板用于定义字符串和日期时间对象之间的转换规则。模板中的字母大小写含义不同，开发时需要特别注意。

常用模板如下：

| 模板                      | 示例                      | 说明             |
| ------------------------- | ------------------------- | ---------------- |
| `yyyy-MM-dd`              | `2026-05-15`              | 常用日期         |
| `HH:mm:ss`                | `14:30:00`                | 常用时间         |
| `yyyy-MM-dd HH:mm:ss`     | `2026-05-15 14:30:00`     | 常用日期时间     |
| `yyyy/MM/dd HH:mm:ss`     | `2026/05/15 14:30:00`     | 斜杠日期时间     |
| `yyyy年MM月dd日`          | `2026年05月15日`          | 中文日期         |
| `yyyyMMddHHmmss`          | `20260515143000`          | 紧凑日期时间     |
| `yyyy-MM-dd'T'HH:mm:ss`   | `2026-05-15T14:30:00`     | ISO 风格日期时间 |
| `yyyy-MM-dd HH:mm:ss.SSS` | `2026-05-15 14:30:00.123` | 带毫秒日期时间   |

常用符号说明：

| 符号   | 含义          | 示例     |
| ------ | ------------- | -------- |
| `yyyy` | 年            | `2026`   |
| `MM`   | 月            | `05`     |
| `dd`   | 日            | `15`     |
| `HH`   | 24 小时制小时 | `14`     |
| `hh`   | 12 小时制小时 | `02`     |
| `mm`   | 分钟          | `30`     |
| `ss`   | 秒            | `00`     |
| `SSS`  | 毫秒          | `123`    |
| `z`    | 时区名称      | `CST`    |
| `XXX`  | 时区偏移      | `+08:00` |

下面示例演示多个常用格式模板的使用。

```java
package io.github.atengk.datetime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 常用日期时间格式模板示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DateTimePatternExample {

    public static void main(String[] args) {
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        List<String> patterns = List.of(
                "yyyy-MM-dd",
                "HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy年MM月dd日 HH时mm分ss秒",
                "yyyyMMddHHmmss",
                "yyyy-MM-dd'T'HH:mm:ss"
        );

        for (String pattern : patterns) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            System.out.println(pattern + " -> " + dateTime.format(formatter));
        }
    }
}
```

开发注意事项：

- `MM` 表示月份，`mm` 表示分钟，不能混用。
- `HH` 表示 24 小时制，`hh` 表示 12 小时制。
- ISO 日期时间中间的 `T` 是普通字符，需要使用单引号包裹：`yyyy-MM-dd'T'HH:mm:ss`。
- 接口字段格式建议在团队内统一，避免同一个系统中出现多种日期时间格式。

## 日期时间计算

日期时间计算主要包括增加时间、减少时间、计算两个时间之间的间隔，以及计算两个日期之间的间隔。`java.time` 提供了 `plus`、`minus`、`Duration`、`Period` 等工具。

### plus 方法

`plus` 方法用于在日期时间对象上增加指定时间。由于 `java.time` 中大多数类型是不可变对象，所以调用 `plus` 后会返回一个新的对象，原对象不会被修改。

下面示例演示常用 `plus` 方法。

```java
package io.github.atengk.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * plus 方法日期时间计算示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PlusDateTimeExample {

    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2026, 5, 15);
        LocalTime time = LocalTime.of(14, 30, 0);
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        LocalDate nextDay = date.plusDays(1);
        LocalDate nextMonth = date.plusMonths(1);
        LocalDate nextYear = date.plusYears(1);

        LocalTime afterThirtyMinutes = time.plusMinutes(30);
        LocalTime afterTwoHours = time.plusHours(2);

        LocalDateTime expireTime = dateTime.plusDays(7);
        LocalDateTime retryTime = dateTime.plusMinutes(15);

        System.out.println("原日期：" + date);
        System.out.println("下一天：" + nextDay);
        System.out.println("下个月：" + nextMonth);
        System.out.println("下一年：" + nextYear);
        System.out.println("30 分钟后：" + afterThirtyMinutes);
        System.out.println("2 小时后：" + afterTwoHours);
        System.out.println("过期时间：" + expireTime);
        System.out.println("重试时间：" + retryTime);
    }
}
```

常用 `plus` 方法：

| 方法                   | 说明     |
| ---------------------- | -------- |
| `plusYears(years)`     | 增加年   |
| `plusMonths(months)`   | 增加月   |
| `plusWeeks(weeks)`     | 增加周   |
| `plusDays(days)`       | 增加天   |
| `plusHours(hours)`     | 增加小时 |
| `plusMinutes(minutes)` | 增加分钟 |
| `plusSeconds(seconds)` | 增加秒   |
| `plusNanos(nanos)`     | 增加纳秒 |

开发注意事项：

- `plus` 方法不会修改原对象。
- `LocalDate.of(2026, 1, 31).plusMonths(1)` 会得到 `2026-02-28`，不是非法日期。
- 计算过期时间、延迟重试时间、任务下次执行时间时，适合使用 `plus`。

### minus 方法

`minus` 方法用于在日期时间对象上减少指定时间。它和 `plus` 一样，也会返回新对象，不会修改原对象。

下面示例演示常用 `minus` 方法。

```java
package io.github.atengk.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * minus 方法日期时间计算示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MinusDateTimeExample {

    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2026, 5, 15);
        LocalTime time = LocalTime.of(14, 30, 0);
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        LocalDate previousDay = date.minusDays(1);
        LocalDate previousMonth = date.minusMonths(1);
        LocalDate previousYear = date.minusYears(1);

        LocalTime beforeThirtyMinutes = time.minusMinutes(30);
        LocalTime beforeTwoHours = time.minusHours(2);

        LocalDateTime beforeSevenDays = dateTime.minusDays(7);
        LocalDateTime beforeFifteenMinutes = dateTime.minusMinutes(15);

        System.out.println("原日期：" + date);
        System.out.println("前一天：" + previousDay);
        System.out.println("上个月：" + previousMonth);
        System.out.println("上一年：" + previousYear);
        System.out.println("30 分钟前：" + beforeThirtyMinutes);
        System.out.println("2 小时前：" + beforeTwoHours);
        System.out.println("7 天前：" + beforeSevenDays);
        System.out.println("15 分钟前：" + beforeFifteenMinutes);
    }
}
```

常用 `minus` 方法：

| 方法                    | 说明     |
| ----------------------- | -------- |
| `minusYears(years)`     | 减少年   |
| `minusMonths(months)`   | 减少月   |
| `minusWeeks(weeks)`     | 减少周   |
| `minusDays(days)`       | 减少天   |
| `minusHours(hours)`     | 减少小时 |
| `minusMinutes(minutes)` | 减少分钟 |
| `minusSeconds(seconds)` | 减少秒   |
| `minusNanos(nanos)`     | 减少纳秒 |

开发注意事项：

- 查询最近 7 天数据时，可以使用 `LocalDateTime.now().minusDays(7)`。
- 查询最近 30 分钟数据时，可以使用 `LocalDateTime.now().minusMinutes(30)`。
- 对日期边界敏感的业务，需要注意月末、闰年等情况。

### Duration

`Duration` 用于计算两个时间点之间的时间间隔，适合处理小时、分钟、秒、毫秒、纳秒级别的间隔。

适用场景：

- 计算接口耗时
- 计算任务执行时间
- 计算两个 `LocalDateTime` 的时间差
- 计算两个 `Instant` 的时间差
- 判断验证码是否过期
- 判断 Token 是否超时

下面示例演示 `Duration` 的使用方式。

```java
package io.github.atengk.datetime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Duration 时间间隔计算示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DurationExample {

    public static void main(String[] args) {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 15, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 15, 12, 30, 0);

        Duration duration = Duration.between(startTime, endTime);

        long totalHours = duration.toHours();
        long totalMinutes = duration.toMinutes();
        long totalSeconds = duration.getSeconds();

        System.out.println("相差小时数：" + totalHours);
        System.out.println("相差分钟数：" + totalMinutes);
        System.out.println("相差秒数：" + totalSeconds);

        Instant requestStart = Instant.now();

        // 模拟业务处理耗时
        Instant requestEnd = requestStart.plusMillis(350);

        Duration cost = Duration.between(requestStart, requestEnd);
        System.out.println("接口耗时毫秒数：" + cost.toMillis());
    }
}
```

常用方法：

| 方法                           | 说明                   |
| ------------------------------ | ---------------------- |
| `Duration.between(start, end)` | 计算两个时间之间的间隔 |
| `toDays()`                     | 转为总天数             |
| `toHours()`                    | 转为总小时数           |
| `toMinutes()`                  | 转为总分钟数           |
| `getSeconds()`                 | 获取总秒数             |
| `toMillis()`                   | 转为总毫秒数           |
| `plus(duration)`               | 增加间隔               |
| `minus(duration)`              | 减少间隔               |

开发注意事项：

- `Duration` 适合时间维度计算，例如秒、分钟、小时。
- `Duration` 不适合表达“几个月”这种日期维度，因为月份天数不固定。
- 如果 `end` 早于 `start`，结果是负数间隔。
- 计算接口耗时时，建议使用 `Instant` 或 `System.nanoTime()`，不要使用格式化后的字符串计算。

### Period

`Period` 用于计算两个日期之间的日期间隔，适合处理年、月、日级别的间隔。

适用场景：

- 计算年龄
- 计算合同剩余日期
- 计算两个日期相差几年几个月几天
- 计算会员有效期
- 计算账单周期

下面示例演示 `Period` 的使用方式。

```java
package io.github.atengk.datetime;

import java.time.LocalDate;
import java.time.Period;

/**
 * Period 日期间隔计算示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PeriodExample {

    public static void main(String[] args) {
        LocalDate birthday = LocalDate.of(1998, 8, 18);
        LocalDate currentDate = LocalDate.of(2026, 5, 15);

        Period agePeriod = Period.between(birthday, currentDate);

        System.out.println("年龄年数：" + agePeriod.getYears());
        System.out.println("剩余月数：" + agePeriod.getMonths());
        System.out.println("剩余天数：" + agePeriod.getDays());

        LocalDate contractStartDate = LocalDate.of(2026, 1, 1);
        LocalDate contractEndDate = LocalDate.of(2027, 3, 15);

        Period contractPeriod = Period.between(contractStartDate, contractEndDate);

        System.out.println("合同相差年：" + contractPeriod.getYears());
        System.out.println("合同相差月：" + contractPeriod.getMonths());
        System.out.println("合同相差日：" + contractPeriod.getDays());

        LocalDate afterThreeMonths = currentDate.plus(Period.ofMonths(3));
        System.out.println("三个月后：" + afterThreeMonths);
    }
}
```

常用方法：

| 方法                                 | 说明                   |
| ------------------------------------ | ---------------------- |
| `Period.between(startDate, endDate)` | 计算两个日期之间的间隔 |
| `Period.ofYears(years)`              | 创建年间隔             |
| `Period.ofMonths(months)`            | 创建月间隔             |
| `Period.ofDays(days)`                | 创建日间隔             |
| `getYears()`                         | 获取年部分             |
| `getMonths()`                        | 获取月部分             |
| `getDays()`                          | 获取日部分             |
| `toTotalMonths()`                    | 转为总月数             |

开发注意事项：

- `Period` 适合日期维度计算，例如年、月、日。
- `Period` 不适合计算小时、分钟、秒。
- `Period.between()` 的结果是分段的年、月、日，不是总天数。
- 如果需要计算两个日期相差总天数，应使用 `ChronoUnit.DAYS.between(startDate, endDate)`。

下面示例演示使用 `ChronoUnit` 计算总天数。

```java
package io.github.atengk.datetime;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ChronoUnit 日期总天数计算示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ChronoUnitDaysExample {

    public static void main(String[] args) {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 15);

        long days = ChronoUnit.DAYS.between(startDate, endDate);

        System.out.println("相差总天数：" + days);
    }
}
```



## 时区处理

时区处理是日期时间开发中最容易出错的部分。`LocalDateTime` 本身不包含时区，`Instant` 表示 UTC 时间线上的时间点，`ZonedDateTime` 表示带具体时区规则的日期时间。

实际开发中要区分三个概念：

| 概念         | 示例                                       | 说明                                   |
| ------------ | ------------------------------------------ | -------------------------------------- |
| 本地日期时间 | `2026-05-15 14:30:00`                      | 不包含时区，不能单独表示全球唯一时间点 |
| UTC 时间     | `2026-05-15T06:30:00Z`                     | 全球统一时间点                         |
| 带时区时间   | `2026-05-15T14:30:00+08:00[Asia/Shanghai]` | 包含地区时区和时区规则                 |

跨时区系统建议遵循以下原则：

- 系统内部存储统一使用 `Instant` 或 UTC 时间戳
- 展示给用户时，再根据用户所在时区转换
- 不要手动加减小时处理时区
- 涉及夏令时地区时，优先使用 `ZoneId`
- 接口传输时，明确时间格式、时区和偏移量

### ZoneId

`ZoneId` 表示一个具体时区，例如 `Asia/Shanghai`、`Asia/Tokyo`、`America/New_York`。它不仅表示固定偏移量，还包含时区规则，例如夏令时规则。

常用时区示例：

| ZoneId             | 说明                     |
| ------------------ | ------------------------ |
| `Asia/Shanghai`    | 中国上海时间             |
| `Asia/Tokyo`       | 日本东京时间             |
| `UTC`              | 世界协调时间             |
| `America/New_York` | 纽约时间，包含夏令时规则 |
| `Europe/London`    | 伦敦时间，包含夏令时规则 |

下面示例演示 `ZoneId` 的基本使用，以及不同时区之间的时间转换。

```java
package io.github.atengk.datetime;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * ZoneId 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ZoneIdExample {

    public static void main(String[] args) {
        ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
        ZoneId newYorkZone = ZoneId.of("America/New_York");

        LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        ZonedDateTime shanghaiTime = localDateTime.atZone(shanghaiZone);
        ZonedDateTime tokyoTime = shanghaiTime.withZoneSameInstant(tokyoZone);
        ZonedDateTime newYorkTime = shanghaiTime.withZoneSameInstant(newYorkZone);

        System.out.println("上海时间：" + shanghaiTime);
        System.out.println("东京时间：" + tokyoTime);
        System.out.println("纽约时间：" + newYorkTime);
    }
}
```

开发注意事项：

- `ZoneId.of("Asia/Shanghai")` 表示具体地区时区。
- `withZoneSameInstant()` 表示保持同一个时间点，换算到另一个时区。
- 不建议使用 `GMT+8` 这种写法替代具体地区时区。
- 涉及国际业务、海外用户、航班、会议时，建议使用 `ZoneId`。

### ZoneOffset

`ZoneOffset` 表示相对于 UTC 的固定偏移量，例如 `+08:00`、`+09:00`、`-05:00`。它是 `ZoneId` 的子类，但不包含夏令时等地区规则。

`ZoneOffset` 适合表示固定偏移量场景，例如接口传输中的 `+08:00`。如果业务需要处理地区规则，应使用 `ZoneId`。

下面示例演示 `ZoneOffset` 的基本使用。

```java
package io.github.atengk.datetime;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * ZoneOffset 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ZoneOffsetExample {

    public static void main(String[] args) {
        LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        ZoneOffset chinaOffset = ZoneOffset.ofHours(8);
        ZoneOffset japanOffset = ZoneOffset.of("+09:00");

        OffsetDateTime chinaOffsetDateTime = OffsetDateTime.of(localDateTime, chinaOffset);
        OffsetDateTime japanOffsetDateTime = OffsetDateTime.of(localDateTime, japanOffset);

        System.out.println("中国偏移时间：" + chinaOffsetDateTime);
        System.out.println("日本偏移时间：" + japanOffsetDateTime);
    }
}
```

`ZoneId` 和 `ZoneOffset` 的区别：

| 类型         | 示例            | 是否包含地区规则 | 适用场景               |
| ------------ | --------------- | ---------------- | ---------------------- |
| `ZoneId`     | `Asia/Shanghai` | 是               | 跨地区、国际化、夏令时 |
| `ZoneOffset` | `+08:00`        | 否               | 固定偏移量、接口传输   |

开发注意事项：

- `ZoneOffset.ofHours(8)` 表示固定 `+08:00`。
- `ZoneOffset` 不处理夏令时。
- 仅知道偏移量时可以使用 `ZoneOffset`。
- 知道用户所在地区时优先使用 `ZoneId`。

### UTC 时间

UTC 是世界协调时间，Java 中通常使用 `Instant` 表示 UTC 时间线上的时间点。`Instant.now()` 获取的是当前 UTC 时间点。

UTC 时间适合系统内部统一存储和跨系统传输，因为它不受服务器本地时区影响。

下面示例演示 UTC 时间的创建和转换。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * UTC 时间使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UtcTimeExample {

    public static void main(String[] args) {
        Instant utcInstant = Instant.now();

        ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");

        ZonedDateTime shanghaiTime = utcInstant.atZone(shanghaiZone);
        ZonedDateTime tokyoTime = utcInstant.atZone(tokyoZone);

        LocalDateTime shanghaiLocalDateTime = LocalDateTime.ofInstant(utcInstant, shanghaiZone);

        System.out.println("UTC 时间点：" + utcInstant);
        System.out.println("上海时间：" + shanghaiTime);
        System.out.println("东京时间：" + tokyoTime);
        System.out.println("上海本地日期时间：" + shanghaiLocalDateTime);
    }
}
```

开发注意事项：

- `Instant` 本身就是 UTC 时间点。
- `Instant` 适合存储事件发生时间、日志时间、消息时间。
- `Instant` 展示给用户前，需要结合 `ZoneId` 转为用户本地时间。
- 数据库存储 UTC 时间时，接口返回时要明确转换规则。

### 本地时间与时区时间转换

本地时间与时区时间转换时，关键是明确 `LocalDateTime` 应该按照哪个时区解释。因为 `LocalDateTime` 没有时区信息，只有结合 `ZoneId` 后才能转换为 `Instant`。

常见转换关系如下：

| 转换                               | 方法                                       |
| ---------------------------------- | ------------------------------------------ |
| `LocalDateTime` 转 `ZonedDateTime` | `localDateTime.atZone(zoneId)`             |
| `ZonedDateTime` 转 `LocalDateTime` | `zonedDateTime.toLocalDateTime()`          |
| `Instant` 转 `LocalDateTime`       | `LocalDateTime.ofInstant(instant, zoneId)` |
| `LocalDateTime` 转 `Instant`       | `localDateTime.atZone(zoneId).toInstant()` |
| `Instant` 转 `ZonedDateTime`       | `instant.atZone(zoneId)`                   |
| `ZonedDateTime` 转 `Instant`       | `zonedDateTime.toInstant()`                |

下面示例演示本地时间、时区时间和 UTC 时间点之间的转换。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 本地时间与时区时间转换示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LocalAndZoneConvertExample {

    public static void main(String[] args) {
        ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");

        LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        ZonedDateTime shanghaiZonedDateTime = localDateTime.atZone(shanghaiZone);
        Instant instant = shanghaiZonedDateTime.toInstant();

        ZonedDateTime tokyoZonedDateTime = instant.atZone(tokyoZone);
        LocalDateTime tokyoLocalDateTime = tokyoZonedDateTime.toLocalDateTime();

        System.out.println("上海本地日期时间：" + localDateTime);
        System.out.println("上海时区日期时间：" + shanghaiZonedDateTime);
        System.out.println("UTC 时间点：" + instant);
        System.out.println("东京时区日期时间：" + tokyoZonedDateTime);
        System.out.println("东京本地日期时间：" + tokyoLocalDateTime);
    }
}
```

开发注意事项：

- `LocalDateTime` 转 `Instant` 必须指定 `ZoneId`。
- `Instant` 转本地时间也必须指定 `ZoneId`。
- 不要把 `LocalDateTime` 当成 UTC 时间使用。
- 不要通过手动 `plusHours(8)` 处理 UTC 到北京时间转换。

## 新旧 API 转换

新旧 API 转换主要用于兼容历史代码、第三方库、数据库驱动和老接口。新代码建议优先使用 `java.time`，只在边界位置进行转换。

常见转换关系如下：

| 旧 API      | 新 API          | 说明                        |
| ----------- | --------------- | --------------------------- |
| `Date`      | `Instant`       | `Date` 内部本质上也是时间戳 |
| `Date`      | `LocalDateTime` | 需要指定时区                |
| `Timestamp` | `LocalDateTime` | JDBC 常见转换               |
| `Instant`   | `Date`          | 兼容旧接口                  |

### Date 与 LocalDateTime 转换

`Date` 表示一个时间点，`LocalDateTime` 表示不带时区的本地日期时间。因此二者转换时必须指定 `ZoneId`。

下面示例演示 `Date` 与 `LocalDateTime` 的相互转换，并使用 Hutool 简化 `Date` 的格式化输出。

```java
package io.github.atengk.datetime;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Date 与 LocalDateTime 转换示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DateLocalDateTimeConvertExample {

    public static void main(String[] args) {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");

        Date date = new Date();

        LocalDateTime localDateTime = date.toInstant()
                .atZone(zoneId)
                .toLocalDateTime();

        Date convertedDate = Date.from(localDateTime.atZone(zoneId).toInstant());

        System.out.println("Date 格式化结果：" + DateUtil.format(date, DatePattern.NORM_DATETIME_PATTERN));
        System.out.println("Date 转 LocalDateTime：" + localDateTime);
        System.out.println("LocalDateTime 转 Date：" + DateUtil.format(convertedDate, DatePattern.NORM_DATETIME_PATTERN));
    }
}
```

转换方法说明：

| 转换方向                  | 写法                                                  |
| ------------------------- | ----------------------------------------------------- |
| `Date` 转 `LocalDateTime` | `date.toInstant().atZone(zoneId).toLocalDateTime()`   |
| `LocalDateTime` 转 `Date` | `Date.from(localDateTime.atZone(zoneId).toInstant())` |

开发注意事项：

- `Date` 转 `LocalDateTime` 时必须指定时区。
- `LocalDateTime` 转 `Date` 时也必须指定时区。
- 如果服务器默认时区不一致，使用 `ZoneId.systemDefault()` 可能导致不同环境结果不同。
- 推荐业务代码中显式使用 `ZoneId.of("Asia/Shanghai")` 或配置化时区。

### Timestamp 与 LocalDateTime 转换

`Timestamp` 常见于 JDBC、MyBatis、老项目数据库字段处理中。Java 8 之后，`Timestamp` 已经提供了和 `LocalDateTime` 互转的方法。

下面示例演示 `Timestamp` 与 `LocalDateTime` 的相互转换。

```java
package io.github.atengk.datetime;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Timestamp 与 LocalDateTime 转换示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TimestampLocalDateTimeConvertExample {

    public static void main(String[] args) {
        LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        Timestamp timestamp = Timestamp.valueOf(localDateTime);
        LocalDateTime convertedLocalDateTime = timestamp.toLocalDateTime();

        System.out.println("LocalDateTime：" + localDateTime);
        System.out.println("Timestamp：" + timestamp);
        System.out.println("Timestamp 转 LocalDateTime：" + convertedLocalDateTime);
    }
}
```

转换方法说明：

| 转换方向                       | 写法                               |
| ------------------------------ | ---------------------------------- |
| `LocalDateTime` 转 `Timestamp` | `Timestamp.valueOf(localDateTime)` |
| `Timestamp` 转 `LocalDateTime` | `timestamp.toLocalDateTime()`      |

开发注意事项：

- `Timestamp` 常用于数据库兼容场景。
- 新项目中实体类字段可以直接使用 `LocalDateTime`。
- 使用 MyBatis-Plus、JPA 等框架时，一般可以自动处理 `LocalDateTime` 映射。
- 数据库字段类型和 JDBC 驱动行为会影响最终时区表现，跨时区系统需要统一规范。

### Instant 与 Date 转换

`Instant` 和 `Date` 都可以表示时间线上的一个时间点，因此二者转换比较直接，不需要额外指定时区。

下面示例演示 `Instant` 和 `Date` 的相互转换。

```java
package io.github.atengk.datetime;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;

import java.time.Instant;
import java.util.Date;

/**
 * Instant 与 Date 转换示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class InstantDateConvertExample {

    public static void main(String[] args) {
        Instant instant = Instant.now();

        Date date = Date.from(instant);
        Instant convertedInstant = date.toInstant();

        System.out.println("Instant：" + instant);
        System.out.println("Date：" + DateUtil.format(date, DatePattern.NORM_DATETIME_PATTERN));
        System.out.println("Date 转 Instant：" + convertedInstant);
    }
}
```

转换方法说明：

| 转换方向            | 写法                 |
| ------------------- | -------------------- |
| `Instant` 转 `Date` | `Date.from(instant)` |
| `Date` 转 `Instant` | `date.toInstant()`   |

开发注意事项：

- `Instant` 和 `Date` 都表示时间点。
- `Date` 输出字符串时会受到默认时区影响。
- `Instant` 更适合新代码。
- `Date` 主要用于兼容历史接口。

## 日期时间实践

日期时间实践主要关注业务开发中的常用处理方式，包括获取一天开始和结束时间、计算时间间隔、判断日期范围、接口时间参数处理等。

### 获取一天开始和结束时间

获取一天开始和结束时间常用于查询某一天的数据，例如订单查询、日志查询、统计报表等。

常见规则：

| 边界           | 推荐值                                   |
| -------------- | ---------------------------------------- |
| 一天开始时间   | `00:00:00`                               |
| 一天结束时间   | `23:59:59.999999999`                     |
| 数据库查询推荐 | 左闭右开：`>= start` 且 `< nextDayStart` |

实际开发中，数据库查询更推荐使用“左闭右开”范围，而不是使用 `23:59:59` 作为结束时间。这样可以避免毫秒、微秒、纳秒精度导致的边界问题。

下面示例演示获取一天开始时间、结束时间和下一天开始时间。

```java
package io.github.atengk.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 获取一天开始和结束时间示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DayStartEndExample {

    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2026, 5, 15);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);
        LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();

        System.out.println("当天开始时间：" + startOfDay);
        System.out.println("当天结束时间：" + endOfDay);
        System.out.println("下一天开始时间：" + nextDayStart);

        System.out.println("推荐查询条件：create_time >= " + startOfDay + " AND create_time < " + nextDayStart);
    }
}
```

开发注意事项：

- 查询某一天数据时，推荐使用 `>= 当天开始时间` 和 `< 下一天开始时间`。
- 不建议使用 `<= 23:59:59`，因为数据库字段可能存在毫秒或更高精度。
- `LocalTime.MAX` 是 `23:59:59.999999999`。
- 如果数据库精度只到秒，`LocalTime.MAX` 可能需要结合数据库字段精度处理。

### 计算时间间隔

计算时间间隔常用于接口耗时、任务耗时、验证码有效期、订单超时、会员有效期等场景。

常用选择：

| 场景                 | 推荐类型                         |
| -------------------- | -------------------------------- |
| 秒、分钟、小时级间隔 | `Duration`                       |
| 年、月、日级间隔     | `Period`                         |
| 总天数、总小时数     | `ChronoUnit`                     |
| 接口耗时             | `Instant` 或 `System.nanoTime()` |

下面示例演示常见时间间隔计算。

```java
package io.github.atengk.datetime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * 计算时间间隔示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TimeIntervalExample {

    public static void main(String[] args) {
        LocalDateTime startDateTime = LocalDateTime.of(2026, 5, 15, 10, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2026, 5, 15, 12, 30, 0);

        Duration duration = Duration.between(startDateTime, endDateTime);

        System.out.println("相差小时数：" + duration.toHours());
        System.out.println("相差分钟数：" + duration.toMinutes());

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2027, 3, 15);

        Period period = Period.between(startDate, endDate);
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);

        System.out.println("相差年：" + period.getYears());
        System.out.println("相差月：" + period.getMonths());
        System.out.println("相差日：" + period.getDays());
        System.out.println("相差总天数：" + totalDays);

        Instant requestStart = Instant.now();
        Instant requestEnd = requestStart.plusMillis(350);

        System.out.println("接口耗时毫秒数：" + Duration.between(requestStart, requestEnd).toMillis());
    }
}
```

开发注意事项：

- `Period.between()` 得到的是年、月、日分段结果。
- 如果要计算总天数，使用 `ChronoUnit.DAYS.between()`。
- 如果要计算接口耗时，可以使用 `Duration.between(start, end).toMillis()`。
- 如果要做高精度性能测试，优先使用 `System.nanoTime()`。

### 判断日期范围

判断日期范围常用于查询条件校验、活动有效期判断、订单超时判断、会员有效期判断等场景。

常见判断规则：

| 场景                 | 判断方式                                      |
| -------------------- | --------------------------------------------- |
| 是否在开始时间之后   | `!time.isBefore(start)`                       |
| 是否在结束时间之前   | `!time.isAfter(end)`                          |
| 是否在闭区间内       | `!time.isBefore(start) && !time.isAfter(end)` |
| 是否在左闭右开区间内 | `!time.isBefore(start) && time.isBefore(end)` |

下面示例演示日期时间范围判断，并使用 Hutool 进行参数空值校验。

```java
package io.github.atengk.datetime;

import cn.hutool.core.util.ObjectUtil;

import java.time.LocalDateTime;

/**
 * 判断日期范围示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DateRangeExample {

    public static void main(String[] args) {
        LocalDateTime currentTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 15, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 15, 18, 0, 0);

        boolean inClosedRange = isInClosedRange(currentTime, startTime, endTime);
        boolean inLeftClosedRightOpenRange = isInLeftClosedRightOpenRange(currentTime, startTime, endTime);

        System.out.println("是否在闭区间内：" + inClosedRange);
        System.out.println("是否在左闭右开区间内：" + inLeftClosedRightOpenRange);
    }

    /**
     * 判断时间是否在闭区间内
     *
     * @param targetTime 目标时间
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否在闭区间内
     */
    public static boolean isInClosedRange(LocalDateTime targetTime, LocalDateTime startTime, LocalDateTime endTime) {
        if (ObjectUtil.hasEmpty(targetTime, startTime, endTime)) {
            throw new IllegalArgumentException("时间参数不能为空");
        }
        return !targetTime.isBefore(startTime) && !targetTime.isAfter(endTime);
    }

    /**
     * 判断时间是否在左闭右开区间内
     *
     * @param targetTime 目标时间
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 是否在左闭右开区间内
     */
    public static boolean isInLeftClosedRightOpenRange(LocalDateTime targetTime, LocalDateTime startTime, LocalDateTime endTime) {
        if (ObjectUtil.hasEmpty(targetTime, startTime, endTime)) {
            throw new IllegalArgumentException("时间参数不能为空");
        }
        return !targetTime.isBefore(startTime) && targetTime.isBefore(endTime);
    }
}
```

开发注意事项：

- 数据库时间范围查询推荐使用左闭右开。
- 活动有效期判断通常可以使用闭区间。
- 判断范围前应校验开始时间不能晚于结束时间。
- 范围判断要提前约定边界是否包含开始时间和结束时间。

### 接口时间参数处理

接口时间参数处理主要包括请求参数解析、响应字段格式化和时区约定。Spring Boot 项目中，常见方式是使用 `@DateTimeFormat` 处理请求参数，使用 `@JsonFormat` 处理 JSON 序列化格式。

常见接口约定：

| 场景                | 建议                                   |
| ------------------- | -------------------------------------- |
| GET 请求时间参数    | 使用 `@DateTimeFormat`                 |
| JSON 请求体时间字段 | 使用全局 Jackson 配置或 `@JsonFormat`  |
| 返回给前端的时间    | 统一格式和时区                         |
| 跨时区接口          | 优先传 `Instant`、时间戳或带偏移量时间 |
| 查询时间范围        | 使用开始时间和结束时间，并约定左闭右开 |

下面示例给出一个 Spring Boot 中处理时间参数的常见写法。

文件位置：`src/main/java/io/github/atengk/datetime/controller/OrderQueryController.java`

```java
package io.github.atengk.datetime.controller;

import io.github.atengk.datetime.dto.OrderQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单时间参数处理接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderQueryController {

    /**
     * 使用 GET 请求查询指定日期订单
     *
     * @param orderDate 订单日期
     * @return 查询结果
     */
    @GetMapping("/by-date")
    public String queryByDate(@RequestParam("orderDate")
                              @DateTimeFormat(pattern = "yyyy-MM-dd")
                              LocalDate orderDate) {
        log.info("查询指定日期订单，orderDate={}", orderDate);
        return "查询日期：" + orderDate;
    }

    /**
     * 使用 GET 请求查询指定时间范围订单
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 查询结果
     */
    @GetMapping("/range")
    public String queryByRange(@RequestParam("startTime")
                               @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                               LocalDateTime startTime,
                               @RequestParam("endTime")
                               @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                               LocalDateTime endTime) {
        log.info("查询时间范围订单，startTime={}，endTime={}", startTime, endTime);
        return "查询时间范围：" + startTime + " ~ " + endTime;
    }

    /**
     * 使用 POST 请求查询订单
     *
     * @param request 查询请求
     * @return 查询结果
     */
    @PostMapping("/query")
    public String query(@RequestBody OrderQueryRequest request) {
        log.info("查询订单，请求参数={}", request);
        return "查询成功";
    }
}
```

文件位置：`src/main/java/io/github/atengk/datetime/dto/OrderQueryRequest.java`

```java
package io.github.atengk.datetime.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单查询请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderQueryRequest {

    /**
     * 订单日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
```

GET 请求示例：

```bash
curl "http://localhost:8080/orders/by-date?orderDate=2026-05-15"

curl "http://localhost:8080/orders/range?startTime=2026-05-15%2000:00:00&endTime=2026-05-16%2000:00:00"
```

POST 请求示例：

```bash
curl -X POST "http://localhost:8080/orders/query" \
  -H "Content-Type: application/json" \
  -d '{
    "orderDate": "2026-05-15",
    "startTime": "2026-05-15 00:00:00",
    "endTime": "2026-05-16 00:00:00"
  }'
```

开发注意事项：

- `@DateTimeFormat` 主要用于请求参数绑定，例如 `@RequestParam`。
- `@JsonFormat` 主要用于 JSON 序列化和反序列化。
- 项目中更推荐配置全局 Jackson 日期时间格式，避免每个字段重复写注解。
- 时间范围查询建议前端传 `startTime` 和 `endTime`，后端使用左闭右开查询。
- 跨时区接口应明确传输 UTC 时间、时间戳或带偏移量时间。

## 常见问题

常见问题主要集中在时区语义、类型选择、线程安全和跨时区转换上。理解这些问题可以避免大部分日期时间相关 Bug。

### LocalDateTime 为什么不包含时区

`LocalDateTime` 表示本地日期时间，它只描述“某个日期的某个时间”，不描述这个时间属于哪个时区。

例如：

```text
2026-05-15 14:30:00
```

这个时间在上海表示一个时间点，在东京表示另一个时间点，在纽约又表示另一个时间点。如果没有时区信息，它不能唯一定位到 UTC 时间线上的一个瞬间。

下面示例演示同一个 `LocalDateTime` 在不同时区下会转换为不同的 `Instant`。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * LocalDateTime 不包含时区示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LocalDateTimeWithoutZoneExample {

    public static void main(String[] args) {
        LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 15, 14, 30, 0);

        Instant shanghaiInstant = localDateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant();
        Instant tokyoInstant = localDateTime.atZone(ZoneId.of("Asia/Tokyo")).toInstant();
        Instant newYorkInstant = localDateTime.atZone(ZoneId.of("America/New_York")).toInstant();

        System.out.println("本地日期时间：" + localDateTime);
        System.out.println("按上海时区解释：" + shanghaiInstant);
        System.out.println("按东京时区解释：" + tokyoInstant);
        System.out.println("按纽约时区解释：" + newYorkInstant);
    }
}
```

结论：

- `LocalDateTime` 不表示全球唯一时间点。
- `LocalDateTime` 适合表示本地业务时间。
- 如果需要表示时间线上的唯一瞬间，应使用 `Instant`。
- 如果需要保留地区时区，应使用 `ZonedDateTime`。

### Instant 与 LocalDateTime 的区别

`Instant` 和 `LocalDateTime` 的核心区别是：`Instant` 表示时间线上的唯一时间点，`LocalDateTime` 表示不带时区的本地日期时间。

| 对比项             | Instant                  | LocalDateTime |
| ------------------ | ------------------------ | ------------- |
| 是否包含日期       | 是                       | 是            |
| 是否包含时间       | 是                       | 是            |
| 是否包含时区       | UTC 时间线               | 不包含        |
| 是否表示唯一时间点 | 是                       | 否            |
| 适合场景           | 日志、事件、消息、时间戳 | 本地业务时间  |
| 是否适合直接展示   | 不适合                   | 适合本地展示  |

示例说明：

```text
Instant:
2026-05-15T06:30:00Z

LocalDateTime:
2026-05-15T14:30:00
```

如果将 `Instant` 按 `Asia/Shanghai` 转换，可能得到 `2026-05-15 14:30:00`。但 `LocalDateTime` 本身并不知道它属于上海时间。

开发建议：

- 系统内部记录事件发生时间，使用 `Instant`。
- 用户输入的预约时间、生日、业务时间，使用 `LocalDateTime` 或 `LocalDate`。
- 跨时区传输时，不要只传 `LocalDateTime`。
- 对外接口需要明确时区时，使用 `OffsetDateTime`、`ZonedDateTime` 或时间戳。

### DateTimeFormatter 是否线程安全

`DateTimeFormatter` 是线程安全的，可以定义为静态常量复用。

与之相对，`SimpleDateFormat` 是线程不安全的，不能在多线程环境中共享同一个实例，除非使用加锁、`ThreadLocal` 或每次创建新对象。

下面示例演示推荐的 `DateTimeFormatter` 常量写法。

```java
package io.github.atengk.datetime;

import cn.hutool.core.date.DatePattern;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DateTimeFormatter 线程安全示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DateTimeFormatterSafeExample {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();

        String formatted = now.format(DATE_TIME_FORMATTER);
        LocalDateTime parsed = LocalDateTime.parse(formatted, DATE_TIME_FORMATTER);

        System.out.println("格式化结果：" + formatted);
        System.out.println("解析结果：" + parsed);
    }
}
```

开发建议：

- 新代码使用 `DateTimeFormatter`。
- 不要将 `SimpleDateFormat` 定义为共享静态变量。
- 常用格式可以定义为 `private static final DateTimeFormatter`。
- Spring Boot 项目建议统一配置 Jackson 日期时间格式。

### 如何避免时区转换问题

避免时区转换问题的关键是统一规则，并且不要混淆本地时间、UTC 时间和带时区时间。

推荐规范：

| 场景             | 建议                             |
| ---------------- | -------------------------------- |
| 数据库存储       | 统一 UTC 或明确使用业务时区      |
| 系统内部事件时间 | 使用 `Instant`                   |
| 用户本地展示     | 使用用户 `ZoneId` 转换后展示     |
| 国际化接口       | 使用 ISO-8601 带偏移格式         |
| 定时任务         | 明确指定执行时区                 |
| 时间范围查询     | 使用左闭右开区间                 |
| 服务器部署       | 统一服务器时区或显式配置应用时区 |

不推荐做法：

- 不要手动 `plusHours(8)` 处理北京时间。
- 不要把 `LocalDateTime` 当 UTC 时间传输。
- 不要依赖服务器默认时区处理核心业务时间。
- 不要在接口中混用秒级时间戳和毫秒级时间戳。
- 不要让同一个字段在不同接口中返回不同格式。

下面示例演示推荐的跨时区处理流程：存储使用 `Instant`，展示时按用户时区转换。

```java
package io.github.atengk.datetime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 避免时区转换问题示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AvoidTimezoneProblemExample {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    public static void main(String[] args) {
        Instant eventTime = Instant.now();

        ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
        ZoneId newYorkZone = ZoneId.of("America/New_York");

        String shanghaiDisplayTime = formatForUser(eventTime, shanghaiZone);
        String tokyoDisplayTime = formatForUser(eventTime, tokyoZone);
        String newYorkDisplayTime = formatForUser(eventTime, newYorkZone);

        System.out.println("事件 UTC 时间：" + eventTime);
        System.out.println("上海用户展示时间：" + shanghaiDisplayTime);
        System.out.println("东京用户展示时间：" + tokyoDisplayTime);
        System.out.println("纽约用户展示时间：" + newYorkDisplayTime);
    }

    /**
     * 根据用户时区格式化展示时间
     *
     * @param instant UTC 时间点
     * @param userZoneId 用户时区
     * @return 用户本地展示时间
     */
    public static String formatForUser(Instant instant, ZoneId userZoneId) {
        ZonedDateTime userTime = instant.atZone(userZoneId);
        return userTime.format(DISPLAY_FORMATTER);
    }
}
```

最终建议：

- 业务字段先明确语义，再选择日期时间类型。
- 只表示日期用 `LocalDate`。
- 只表示时间用 `LocalTime`。
- 表示本地业务日期时间用 `LocalDateTime`。
- 表示全局唯一时间点用 `Instant`。
- 表示带地区时区的日期时间用 `ZonedDateTime`。
- 对外接口和数据库字段必须统一时间格式和时区规范。
