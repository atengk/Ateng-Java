package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeDiffUtilTest {
    @Test
    void shouldDiffTreeByKey() {
        NodeData oldRoot = new NodeData(1L, "root", 0L);
        NodeData oldChild = new NodeData(2L, "old", 1L);
        NodeData newChild = new NodeData(2L, "new", 1L);
        NodeData addChild = new NodeData(3L, "add", 1L);
        DiffUtil.TreeDiff<NodeData> diff = DiffUtil.diffTree(List.of(oldRoot, oldChild), List.of(oldRoot, newChild, addChild), NodeData::id);
        assertEquals(1, diff.addedNodes().size());
        assertEquals(1, diff.modifiedNodes().size());
        assertTrue(DiffUtil.hasTreeChanged(List.of(oldRoot, oldChild), List.of(oldRoot, newChild, addChild), NodeData::id));
        assertEquals(1, DiffUtil.getAddedNodes(List.of(oldRoot), List.of(oldRoot, addChild), NodeData::id).size());
        assertEquals(1, DiffUtil.getRemovedNodes(List.of(oldRoot, oldChild), List.of(oldRoot), NodeData::id).size());
        assertEquals(1, DiffUtil.getModifiedNodes(List.of(oldChild), List.of(newChild), NodeData::id).size());
        assertTrue(DiffUtil.getMovedNodes(List.of(oldRoot), List.of(oldRoot), NodeData::id).isEmpty());
        assertTrue(DiffUtil.flattenTreeDiff(diff).hasDiff());
    }

    @Test
    void shouldValidateTreeKeyExtractor() {
        assertThrows(NullPointerException.class, () -> DiffUtil.diffTreeByKey(List.of(new NodeData(1L, "A", 0L)), List.of(), null));
    }
}
