package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilCollectionTest {

    @Test
    void shouldConvertCollections() {
        List<Object> values = List.of("1", "2", "bad", "3.0");
        assertEquals(List.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3.0")), NumberUtil.toBigDecimalList(values));
        assertEquals(List.of(1L, 2L, 3L), NumberUtil.toLongList(values));
        assertEquals(List.of(1, 2, 3), NumberUtil.toIntList(values));
    }

    @Test
    void shouldFilterDistinctAndSort() {
        List<BigDecimal> values = java.util.Arrays.asList(new BigDecimal("1.0"), new BigDecimal("1.00"), new BigDecimal("-2"), BigDecimal.ZERO, null);
        assertEquals(4, NumberUtil.filterNull(values).size());
        assertEquals(List.of(new BigDecimal("1.0"), new BigDecimal("1.00")), NumberUtil.filterPositive(values));
        assertEquals(List.of(new BigDecimal("-2")), NumberUtil.filterNegative(values));
        assertEquals(List.of(BigDecimal.ZERO), NumberUtil.filterZero(values));
        assertEquals(3, NumberUtil.distinct(values).size());
        assertEquals(new BigDecimal("-2"), NumberUtil.sortAsc(values).getFirst());
        assertEquals(new BigDecimal("1.0"), NumberUtil.sortDesc(values).getFirst());
    }

    @Test
    void shouldGenerateRanges() {
        assertEquals(List.of(1, 2, 3), NumberUtil.range(1, 3));
        assertEquals(List.of(3, 2, 1), NumberUtil.range(3, 1));
        assertEquals(List.of(1L, 2L, 3L), NumberUtil.range(1L, 3L));
        assertEquals(List.of(), NumberUtil.range((Integer) null, 3));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.range(1, 100_002));
    }
}
