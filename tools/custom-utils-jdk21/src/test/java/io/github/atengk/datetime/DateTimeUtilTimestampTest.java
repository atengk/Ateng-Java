package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeUtilTimestampTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Test
    void shouldConvertEpochSecondAndMilli() {
        long epochSecond = Instant.parse("2026-04-29T10:30:15Z").getEpochSecond();
        long epochMilli = Instant.parse("2026-04-29T10:30:15Z").toEpochMilli();

        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 30, 15), DateTimeUtil.epochSecondToDateTime(epochSecond, UTC));
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 30, 15), DateTimeUtil.epochMilliToDateTime(epochMilli, UTC));
        assertEquals(epochSecond, DateTimeUtil.toEpochSecond(LocalDateTime.of(2026, 4, 29, 10, 30, 15), UTC));
        assertEquals(epochMilli, DateTimeUtil.toEpochMilli(LocalDateTime.of(2026, 4, 29, 10, 30, 15), UTC));
    }

    @Test
    void shouldDetectAndNormalizeEpochUnit() {
        assertEquals(DateTimeUtil.EpochUnit.SECOND, DateTimeUtil.detectEpochUnit(1_777_456_215L));
        assertEquals(DateTimeUtil.EpochUnit.MILLISECOND, DateTimeUtil.detectEpochUnit(1_777_456_215_000L));
        assertEquals(1_777_456_215_000L, DateTimeUtil.normalizeToEpochMilli(1_777_456_215L));
        assertEquals(1_777_456_215_000L, DateTimeUtil.normalizeToEpochMilli(1_777_456_215_000L));
    }
}
