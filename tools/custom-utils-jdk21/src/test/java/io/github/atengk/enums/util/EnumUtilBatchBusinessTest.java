package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilBatchBusinessTest {

    @Test
    void shouldBatchConvert() {
        assertEquals(List.of(UserStatusEnum.ENABLED, UserStatusEnum.DISABLED), EnumUtil.codesToEnums(UserStatusEnum.class, List.of(1, 0, 99)));
        assertEquals(List.of(1, 0), EnumUtil.enumsToCodes(List.of(UserStatusEnum.ENABLED, UserStatusEnum.DISABLED)));
        assertEquals(List.of("启用", "禁用"), EnumUtil.codesToLabels(UserStatusEnum.class, List.of(1, 0)));
        assertEquals(List.of(UserStatusEnum.ENABLED), EnumUtil.namesToEnums(UserStatusEnum.class, List.of("ENABLED", "UNKNOWN")));
    }

    @Test
    void shouldBuildLabelMapAndReplaceLabel() {
        Map<Object, String> labelMap = EnumUtil.toLabelMap(UserStatusEnum.class, List.of(1, 0, 99));
        assertEquals("启用", labelMap.get(1));
        assertNull(labelMap.get(99));
        assertEquals("启用", EnumUtil.replaceCodeWithLabel(UserStatusEnum.class, 1));
        assertEquals("99", EnumUtil.replaceCodeWithLabel(UserStatusEnum.class, 99));
    }

    @Test
    void shouldFillBusinessLabel() {
        List<UserRecord> records = new ArrayList<>();
        records.add(new UserRecord(1));
        records.add(new UserRecord(0));
        EnumUtil.fillLabel(records, UserRecord::code, UserRecord::label, UserStatusEnum.class);
        assertEquals("启用", records.get(0).label());
        assertEquals("禁用", records.get(1).label());
    }

    @Test
    void shouldParseCodes() {
        assertEquals(List.of(UserStatusEnum.ENABLED, UserStatusEnum.DISABLED), EnumUtil.parseCodes(UserStatusEnum.class, "1,0,99", ","));
        assertTrue(EnumUtil.parseCodes(UserStatusEnum.class, " ", ",").isEmpty());
        assertTrue(EnumUtil.codesToEnums(UserStatusEnum.class, null).isEmpty());
        assertTrue(EnumUtil.enumsToCodes(null).isEmpty());
        assertTrue(EnumUtil.namesToEnums(UserStatusEnum.class, null).isEmpty());
    }

    private static final class UserRecord {
        private final Integer code;
        private String label;

        private UserRecord(Integer code) {
            this.code = code;
        }

        private Integer code() {
            return code;
        }

        private String label() {
            return label;
        }

        private void label(String label) {
            this.label = label;
        }
    }
}
