package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilStatisticsTest {

    record Item(BigDecimal amount) {}

    @Test
    void shouldCalculateStatistics() {
        List<BigDecimal> values = List.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
        assertEquals(new BigDecimal("6"), NumberUtil.sum(values));
        assertEquals(new BigDecimal("2.000000"), NumberUtil.avg(values));
        assertEquals(new BigDecimal("3"), NumberUtil.max(values));
        assertEquals(new BigDecimal("1"), NumberUtil.min(values));
        assertEquals(new BigDecimal("2"), NumberUtil.median(values));
    }

    @Test
    void shouldCountAndCalculateVariance() {
        List<BigDecimal> values = List.of(new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ONE);
        assertEquals(1L, NumberUtil.countPositive(values));
        assertEquals(1L, NumberUtil.countNegative(values));
        assertEquals(1L, NumberUtil.countZero(values));
        assertEquals(new BigDecimal("0.666667"), NumberUtil.variance(values));
        assertEquals(new BigDecimal("0.816497"), NumberUtil.standardDeviation(values));
    }

    @Test
    void shouldCalculateByMapper() {
        List<Item> items = List.of(new Item(new BigDecimal("1.5")), new Item(new BigDecimal("2.5")), new Item(null));
        assertEquals(new BigDecimal("4.0"), NumberUtil.sumBy(items, Item::amount));
        assertEquals(new BigDecimal("2.000000"), NumberUtil.avgBy(items, Item::amount));
        assertEquals(new BigDecimal("2.5"), NumberUtil.maxBy(items, Item::amount));
        assertEquals(new BigDecimal("1.5"), NumberUtil.minBy(items, Item::amount));
        assertThrows(NullPointerException.class, () -> NumberUtil.sumBy(items, null));
    }
}
