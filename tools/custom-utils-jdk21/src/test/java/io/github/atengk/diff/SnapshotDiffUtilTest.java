package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotDiffUtilTest {
    @Test
    void shouldDiffSnapshots() {
        UserData oldUser = new UserData(1L, "A", 18, null);
        UserData newUser = new UserData(1L, "B", 18, null);
        assertTrue(DiffUtil.diffSnapshot(oldUser, newUser).hasDiff());
        assertTrue(DiffUtil.diffSnapshot("{\"a\":1}", "{\"a\":2}").hasDiff());
        assertTrue(DiffUtil.hasSnapshotChanged(oldUser, newUser));
        assertTrue(DiffUtil.createSnapshotDiff(oldUser, newUser).hasDiff());
        assertTrue(DiffUtil.compareSnapshotVersion(oldUser, newUser).hasDiff());
        assertEquals(2, DiffUtil.diffHistory(List.of(new VersionData(1, "A"), new VersionData(2, "B"), new VersionData(3, "C")), VersionData::version).size());
        assertTrue(DiffUtil.getLatestDiff(List.of(new VersionData(1, "A"), new VersionData(2, "B"))).hasDiff());
    }

    @Test
    void shouldHandleBoundarySnapshots() {
        assertFalse(DiffUtil.getLatestDiff(List.of(new VersionData(1, "A"))).hasDiff());
        assertThrows(NullPointerException.class, () -> DiffUtil.diffHistory(List.of(new VersionData(1, "A")), null));
    }
}
