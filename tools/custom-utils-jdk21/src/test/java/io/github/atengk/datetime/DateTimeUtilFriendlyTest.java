package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeUtilFriendlyTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 4, 29, 12, 0).atZone(ZONE).toInstant(), ZONE);

    @Test
    void shouldFormatFriendlyTime() {
        assertEquals("刚刚", DateTimeUtil.friendlyTime(LocalDateTime.of(2026, 4, 29, 11, 59, 30), CLOCK));
        assertEquals("5分钟前", DateTimeUtil.friendlyTime(LocalDateTime.of(2026, 4, 29, 11, 55), CLOCK));
        assertEquals("2小时前", DateTimeUtil.friendlyTime(LocalDateTime.of(2026, 4, 29, 10, 0), CLOCK));
        assertEquals("昨天 10:30", DateTimeUtil.friendlyTime(LocalDateTime.of(2026, 4, 28, 10, 30), CLOCK));
    }

    @Test
    void shouldFormatFriendlyDate() {
        assertEquals("今天", DateTimeUtil.friendlyDate(LocalDate.of(2026, 4, 29), CLOCK));
        assertEquals("昨天", DateTimeUtil.friendlyDate(LocalDate.of(2026, 4, 28), CLOCK));
        assertEquals("明天", DateTimeUtil.friendlyDate(LocalDate.of(2026, 4, 30), CLOCK));
        assertEquals("2026-05-01", DateTimeUtil.friendlyDate(LocalDate.of(2026, 5, 1), CLOCK));
    }

    @Test
    void shouldFormatCountdownAndDuration() {
        assertEquals("1小时30分钟", DateTimeUtil.countdownText(LocalDateTime.of(2026, 4, 29, 13, 30), CLOCK));
        assertEquals("1天2小时3分钟4秒", DateTimeUtil.formatDuration(Duration.ofDays(1).plusHours(2).plusMinutes(3).plusSeconds(4)));
    }
}
