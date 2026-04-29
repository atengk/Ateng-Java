package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilUnitConversionTest {

    @Test
    void shouldConvertPercentBaseValues() {
        assertEquals(new BigDecimal("200"), NumberUtil.multiplyBy100(2));
        assertEquals(0, new BigDecimal("0.02").compareTo(NumberUtil.divideBy100(2)));
        assertEquals(new BigDecimal("250.00"), NumberUtil.permillage(1, 4));
        assertEquals(new BigDecimal("25.00"), NumberUtil.basisPoint(new BigDecimal("0.0025")));
    }

    @Test
    void shouldConvertByteUnits() {
        assertEquals(1024L, NumberUtil.kbToBytes(1));
        assertEquals(1048576L, NumberUtil.mbToBytes(1));
        assertEquals(1073741824L, NumberUtil.gbToBytes(1));
        assertEquals(new BigDecimal("1.00"), NumberUtil.bytesToKb(1024));
        assertEquals(new BigDecimal("1.00"), NumberUtil.bytesToMb(1048576));
        assertEquals(new BigDecimal("1.00"), NumberUtil.bytesToGb(1073741824));
    }

    @Test
    void shouldFormatBytes() {
        assertEquals("512 B", NumberUtil.formatBytes(512));
        assertEquals("1 KB", NumberUtil.formatBytes(1024));
    }
}
