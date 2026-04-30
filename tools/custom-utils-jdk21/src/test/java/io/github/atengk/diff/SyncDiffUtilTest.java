package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyncDiffUtilTest {
    @Test
    void shouldDiffSyncData() {
        ItemData targetOld = new ItemData(1L, "old", 1);
        ItemData sourceNew = new ItemData(1L, "new", 1);
        ItemData sourceAdd = new ItemData(2L, "add", 2);
        DiffUtil.CollectionDiff<ItemData> diff = DiffUtil.diffSyncData(List.of(sourceNew, sourceAdd), List.of(targetOld), ItemData::id);
        assertEquals(1, diff.added().size());
        assertEquals(1, diff.modified().size());
        assertEquals(1, DiffUtil.getNeedInsert(List.of(sourceNew, sourceAdd), List.of(targetOld), ItemData::id).size());
        assertEquals(1, DiffUtil.getNeedUpdate(List.of(sourceNew), List.of(targetOld), ItemData::id).size());
        assertEquals(1, DiffUtil.getNeedDelete(List.of(sourceNew), List.of(targetOld, new ItemData(3L, "delete", 3)), ItemData::id).size());
        assertTrue(DiffUtil.hasSyncDiff(List.of(sourceNew, sourceAdd), List.of(targetOld), ItemData::id));
    }

    @Test
    void shouldDiffResponseAndPayload() {
        assertTrue(DiffUtil.diffResponse(new UserData(1L, "A", 1, null), new UserData(1L, "B", 1, null)).hasDiff());
        assertTrue(DiffUtil.diffPayload(new UserData(1L, "A", 1, null), new UserData(1L, "B", 1, null)).hasDiff());
    }
}
