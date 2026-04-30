package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ValueDiffUtilTest {
    enum Status { ENABLED, DISABLED }

    @Test
    void shouldDiffCommonValues() {
        assertEquals(DiffUtil.DiffType.MODIFIED, DiffUtil.diffValue("name", "A", "B").type());
        assertTrue(DiffUtil.diffIfChanged("name", "A", "A").isEmpty());
        assertEquals(DiffUtil.DiffType.ADDED, DiffUtil.diffNullable("name", null, "A").type());
        assertEquals(DiffUtil.DiffType.MODIFIED, DiffUtil.diffEnum("status", Status.ENABLED, Status.DISABLED).type());
        assertEquals(DiffUtil.DiffType.UNCHANGED, DiffUtil.diffBoolean("flag", true, true).type());
        assertEquals(DiffUtil.DiffType.MODIFIED, DiffUtil.diffNumber("num", 1, 2).type());
        assertEquals(DiffUtil.DiffType.UNCHANGED, DiffUtil.diffBigDecimal("amount", new BigDecimal("1.234"), new BigDecimal("1.233"), 2).type());
        assertEquals(DiffUtil.DiffType.MODIFIED, DiffUtil.diffDateTime("time", LocalDateTime.now(), LocalDateTime.now().plusSeconds(1)).type());
        assertEquals(DiffUtil.DiffType.UNCHANGED, DiffUtil.diffWithComparator("name", "a", "A", String::equalsIgnoreCase).type());
    }

    @Test
    void shouldDiffWithToleranceAndValidateArguments() {
        assertFalse(DiffUtil.isDifferentWithTolerance(new BigDecimal("1.00"), new BigDecimal("1.01"), new BigDecimal("0.02")));
        assertEquals(DiffUtil.DiffType.UNCHANGED, DiffUtil.diffNumberWithTolerance("score", new BigDecimal("1.00"), new BigDecimal("1.01"), new BigDecimal("0.02")).type());
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffBigDecimal("amount", BigDecimal.ONE, BigDecimal.TEN, -1));
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.isDifferentWithTolerance(BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffValue(" ", 1, 2));
    }
}
