package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatchDiffUtilTest {
    @Test
    void shouldCreateApplyAndReversePatch() {
        DiffUtil.DiffPatch patch = DiffUtil.createPatch("a\nb", "a\nc");
        assertTrue(DiffUtil.canApplyPatch("a\nb", patch));
        assertEquals("a\nc", DiffUtil.applyPatch("a\nb", patch));
        assertEquals("a\nb", DiffUtil.applyPatch("a\nc", DiffUtil.reversePatch(patch)));
        assertTrue(patch.toText().contains("c"));
        assertTrue(DiffUtil.patchToDiff(patch).hasDiff());
        assertEquals("target", DiffUtil.mergePatch("base", "target"));
    }

    @Test
    void shouldConvertDiffToPatchAndValidate() {
        DiffUtil.DiffPatch patch = DiffUtil.diffToPatch(DiffUtil.diffLines("a", "b"));
        assertEquals("b", DiffUtil.applyPatch("a", patch));
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.applyPatch("x", patch));
        assertThrows(NullPointerException.class, () -> DiffUtil.patchToDiff(null));
    }
}
