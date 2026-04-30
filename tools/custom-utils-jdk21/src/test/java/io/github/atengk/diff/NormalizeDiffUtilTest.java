package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NormalizeDiffUtilTest {
    @Test
    void shouldNormalizeValues() {
        assertEquals("a b", DiffUtil.normalizeText(" a   b "));
        assertEquals(new BigDecimal("1").stripTrailingZeros(), DiffUtil.normalizeValue(new BigDecimal("1.00")));
        assertEquals(List.of("a b"), DiffUtil.normalizeCollection(List.of(" a  b ")));
        assertTrue(DiffUtil.normalizeMap(Map.of("b", 2, "a", 1)).containsKey("a"));
    }

    @Test
    void shouldCreateIgnoreOptions() {
        assertFalse(DiffUtil.diffBean(new UserData(1L, "A", null, null), new UserData(1L, "A", 1, null), DiffUtil.ignoreNull()).hasDiff());
        assertFalse(DiffUtil.diffBean(new UserData(1L, "", 1, null), new UserData(1L, " ", 1, null), DiffUtil.ignoreBlank()).hasDiff());
        assertFalse(DiffUtil.diffBean(new UserData(1L, "a", 1, null), new UserData(1L, "A", 1, null), DiffUtil.ignoreCase()).hasDiff());
        assertFalse(DiffUtil.diffMap(Map.of("a", List.of(1, 2)), Map.of("a", List.of(2, 1)), DiffUtil.ignoreOrder()).changedEntries().containsKey("a"));
        assertFalse(DiffUtil.diffBean(new UserData(1L, "A", 1, null), new UserData(1L, "B", 1, null), DiffUtil.ignoreFields(List.of("name"))).hasDiff());
        assertFalse(DiffUtil.diffDeep(Map.of("a", 1), Map.of("a", 2), DiffUtil.ignorePaths(List.of("$.a"))).hasDiff());
    }

    @Test
    void shouldIgnoreTimeAndNumberPrecision() {
        class TimeBox {
            LocalDateTime time;
            BigDecimal amount;
            TimeBox(LocalDateTime time, BigDecimal amount) { this.time = time; this.amount = amount; }
        }
        TimeBox oldBox = new TimeBox(LocalDateTime.of(2026, 1, 1, 1, 1, 1), new BigDecimal("1.0"));
        TimeBox newBox = new TimeBox(LocalDateTime.of(2026, 1, 1, 1, 1, 50), new BigDecimal("1.00"));
        DiffUtil.DiffOptions options = DiffUtil.DiffOptions.builder()
                .ignoreTimePrecision(ChronoUnit.MINUTES)
                .ignoreNumberScale(true)
                .build();
        assertFalse(DiffUtil.diffBean(oldBox, newBox, options).hasDiff());
        assertTrue(DiffUtil.ignoreTimePrecision(ChronoUnit.SECONDS).ignoreTimePrecision() == ChronoUnit.SECONDS);
        assertTrue(DiffUtil.ignoreNumberScale().ignoreNumberScale());
    }
}
