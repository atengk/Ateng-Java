package io.github.atengk.word;

import io.github.atengk.utils.word.WordUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordFormatTest {

    enum Status {
        ENABLED
    }

    @Test
    void formatCommonValuesShouldWork() {
        assertEquals("2026-04-30", WordUtil.formatDate(LocalDate.of(2026, 4, 30), null));
        assertEquals("2026-04-30 10:20:30", WordUtil.formatDateTime(LocalDateTime.of(2026, 4, 30, 10, 20, 30), null));
        assertEquals("是", WordUtil.formatBoolean(true, "是", "否"));
        assertEquals("ENABLED", WordUtil.formatEnum(Status.ENABLED));
    }

    @Test
    void formatAmountPercentAndCollectionShouldWork() {
        assertTrue(WordUtil.formatAmount(new BigDecimal("12.30")).contains("12.30"));
        assertEquals("12.3%", WordUtil.formatPercent(new BigDecimal("0.123")));
        assertEquals("A,B", WordUtil.joinValues(List.of("A", "B"), ","));
        assertEquals(List.of("A", "B"), WordUtil.splitLines("A\nB"));
    }

    @Test
    void formatNullShouldReturnBlank() {
        assertEquals("", WordUtil.formatValue(null));
        assertEquals("", WordUtil.formatDate(null, null));
        assertEquals("", WordUtil.joinValues(null, ","));
    }
}
