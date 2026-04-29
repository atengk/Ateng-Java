package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilExpiryTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 4, 29, 10, 0).atZone(ZONE).toInstant(), ZONE);

    @Test
    void shouldCheckLifecycleStatus() {
        assertTrue(DateTimeUtil.isExpired(LocalDateTime.of(2026, 4, 29, 10, 0), CLOCK));
        assertTrue(DateTimeUtil.isNotStarted(LocalDateTime.of(2026, 4, 29, 11, 0), CLOCK));
        assertTrue(DateTimeUtil.isInProgress(LocalDateTime.of(2026, 4, 29, 9, 0), LocalDateTime.of(2026, 4, 29, 11, 0), CLOCK));
        assertFalse(DateTimeUtil.isInProgress(LocalDateTime.of(2026, 4, 29, 10, 0), LocalDateTime.of(2026, 4, 29, 11, 0), Clock.fixed(LocalDateTime.of(2026, 4, 29, 11, 0).atZone(ZONE).toInstant(), ZONE)));
        assertTrue(DateTimeUtil.isEnded(LocalDateTime.of(2026, 4, 29, 9, 0), CLOCK));
    }

    @Test
    void shouldCalculateRemainingTime() {
        assertEquals(3_600, DateTimeUtil.remainingSeconds(LocalDateTime.of(2026, 4, 29, 11, 0), CLOCK));
        assertEquals(3, DateTimeUtil.remainingDays(LocalDateTime.of(2026, 5, 2, 10, 0), CLOCK));
    }

    @Test
    void shouldCalculateExpireTime() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 29, 10, 0);

        assertEquals(LocalDateTime.of(2026, 5, 1, 10, 0), DateTimeUtil.expireAfterDays(start, 2));
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 1), DateTimeUtil.expireAfterSeconds(start, 60));
    }
}
