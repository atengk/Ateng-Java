package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeUtilSerializationTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 4, 29, 10, 30, 15).atZone(ZONE).toInstant(), ZONE);

    @Test
    void shouldSerializeDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 29, 10, 30, 15);

        assertEquals("2026-04-29 10:30:15", DateTimeUtil.toNormDateTime(dateTime));
        assertEquals("20260429103015", DateTimeUtil.toPureDateTime(dateTime));
        assertEquals("2026-04-29T10:30:15", DateTimeUtil.toIsoString(dateTime));
    }

    @Test
    void shouldSerializeDatePathAndFilename() {
        assertEquals("2026/04/29", DateTimeUtil.toDatePath(LocalDate.of(2026, 4, 29)));
        assertEquals("report_20260429103015.xlsx", DateTimeUtil.appendTimestamp("report.xlsx", CLOCK));
        assertEquals("report_20260429103015", DateTimeUtil.appendTimestamp("report", CLOCK));
    }

    @Test
    void shouldGenerateBatchTimePart() {
        assertEquals("20260429103015", DateTimeUtil.batchTimePart(CLOCK));
    }
}
