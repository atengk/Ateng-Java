package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilChooseTest {

    @Test
    void shouldChooseFallback() {
        assertEquals("b", ObjectUtil.choose(null, "b"));
        assertEquals("a", ObjectUtil.choose("a", "b"));
        assertEquals("b", ObjectUtil.chooseIfNull(null, "b"));
        assertEquals("b", ObjectUtil.chooseIfEmpty("", "b"));
        assertEquals("b", ObjectUtil.chooseIfBlank(" ", "b"));
    }

    @Test
    void shouldFindFirstMatchAndPresent() {
        assertEquals("bb", ObjectUtil.firstMatch(value -> value != null && value.length() == 2, null, "a", "bb"));
        assertNull(ObjectUtil.firstMatch(value -> false, "a", "b"));
        assertEquals(Optional.of("a"), ObjectUtil.firstPresent(Optional.empty(), Optional.of("a")));
        assertTrue(ObjectUtil.firstPresent(Optional.empty(), null).isEmpty());
        assertEquals("a", ObjectUtil.coalesce(null, "a", "b"));
    }

    @Test
    void shouldThrowWhenPredicateMissing() {
        assertThrows(NullPointerException.class, () -> ObjectUtil.firstMatch(null, "a"));
    }
}
