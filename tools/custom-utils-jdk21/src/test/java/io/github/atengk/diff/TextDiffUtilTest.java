package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextDiffUtilTest {
    @Test
    void shouldDiffTextByLineWordAndChar() {
        assertTrue(DiffUtil.diffText("a\nb", "a\nc").items().size() > 0);
        assertEquals(1, DiffUtil.diffLines("a", "b").items().size());
        assertEquals(1, DiffUtil.diffWords("hello java", "hello jdk").items().size());
        assertEquals(1, DiffUtil.diffChars("ab", "ac").items().size());
        assertTrue(DiffUtil.hasTextChanged("a", "b"));
        assertTrue(DiffUtil.hasLineChanged("a\nb", "a\nc", 2));
        assertEquals("c", DiffUtil.getAddedLines("a", "a\nc").getFirst());
        assertEquals("b", DiffUtil.getRemovedLines("a\nb", "a").getFirst());
        assertFalse(DiffUtil.getChangedLines("a\nb", "a\nc").isEmpty());
    }

    @Test
    void shouldDiffWithIgnoreRules() {
        assertFalse(DiffUtil.diffTextIgnoreCase("ABC", "abc").items().stream().anyMatch(i -> i.type() != DiffUtil.DiffType.UNCHANGED));
        assertFalse(DiffUtil.diffTextIgnoreBlank(" a ", "a").hasDiff());
        assertFalse(DiffUtil.diffTextIgnoreLineSeparator("a\nb", "ab").hasDiff());
        assertFalse(DiffUtil.diffTextIgnoreWhitespace("a b", "ab").hasDiff());
        assertFalse(DiffUtil.diffLinesIgnoreBlank("a\n\n", "a").hasDiff());
        assertFalse(DiffUtil.normalizeAndDiffText(" a   b ", "a b").hasDiff());
    }

    @Test
    void shouldValidateLineNo() {
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.hasLineChanged("a", "b", 0));
    }
}
