package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilZoneTest {

    @Test
    void shouldGetAndValidateZoneInfo() {
        assertNotNull(DateTimeUtil.systemZone());
        assertTrue(DateTimeUtil.isValidZoneId("Asia/Shanghai"));
        assertFalse(DateTimeUtil.isValidZoneId("Invalid/Zone"));
        assertTrue(DateTimeUtil.availableZoneIds().contains("UTC"));
    }

    @Test
    void shouldConvertZonedDateTime() {
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 29, 10, 30);
        ZonedDateTime shanghaiTime = DateTimeUtil.atZone(dateTime, shanghai);

        assertEquals(LocalDateTime.of(2026, 4, 29, 11, 30), DateTimeUtil.convertZone(shanghaiTime, tokyo).toLocalDateTime());
        assertEquals(dateTime, DateTimeUtil.toLocalDateTime(shanghaiTime));
    }

    @Test
    void shouldGetUtcNowByClock() {
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 4, 29, 10, 30).atZone(shanghai).toInstant(), shanghai);

        assertEquals(ZoneId.of("UTC"), DateTimeUtil.utcNow(clock).getZone());
        assertEquals(LocalDateTime.of(2026, 4, 29, 2, 30), DateTimeUtil.utcNow(clock).toLocalDateTime());
    }
}
