package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.AnnotatedItemEnum;
import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.enumutil.model.EnumOption;
import io.github.atengk.utils.enumutil.EnumUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilExtraFieldTest {

    @AfterEach
    void clean() {
        EnumUtil.setExtraFields(List.of());
    }

    @Test
    void shouldGetExtraFields() {
        Map<String, Object> extra = EnumUtil.getExtra(UserStatusEnum.ENABLED);
        assertEquals("green", extra.get("color"));
        assertEquals("success", extra.get("tagType"));
        assertEquals("user:enabled", EnumUtil.getExtraField(UserStatusEnum.ENABLED, "permission"));
        assertEquals(extra, EnumUtil.toExtraMap(UserStatusEnum.ENABLED));
    }

    @Test
    void shouldGetAnnotationExtraFields() {
        Map<String, Object> extra = EnumUtil.getExtra(AnnotatedItemEnum.SUCCESS);
        assertEquals("green", extra.get("color"));
        assertEquals("success", extra.get("tagType"));
        assertEquals("root", extra.get("parentCode"));
    }

    @Test
    void shouldFilterExtraFields() {
        Map<String, Object> extra = Map.of("color", "green", "tagType", "success", "ignored", "x");
        Map<String, Object> filtered = EnumUtil.filterExtraFields(extra, List.of("color"));
        assertEquals(Map.of("color", "green"), filtered);
        assertEquals(extra, EnumUtil.filterExtraFields(extra, null));
        assertTrue(EnumUtil.filterExtraFields(null, List.of("color")).isEmpty());
    }

    @Test
    void shouldApplyGlobalExtraFields() {
        EnumUtil.setExtraFields(List.of("color"));
        EnumOption option = EnumUtil.toOptionsWithExtra(UserStatusEnum.class).getFirst();
        assertTrue(option.extra().containsKey("color"));
        assertFalse(option.extra().containsKey("tagType"));
        assertTrue(EnumUtil.toFullDict(UserStatusEnum.class).containsKey("metadata"));
    }

    @Test
    void shouldRejectBlankExtraField() {
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.getExtraField(UserStatusEnum.ENABLED, ""));
    }
}
