package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterFormatDiffUtilTest {
    @Test
    void shouldFilterAndConvertResult() {
        DiffUtil.DiffResult result = new DiffUtil.DiffResult(List.of(
                new DiffUtil.DiffItem("a", "A", "a", null, 1, DiffUtil.DiffType.ADDED),
                new DiffUtil.DiffItem("b", "B", "b", 1, null, DiffUtil.DiffType.REMOVED),
                new DiffUtil.DiffItem("c", "C", "c", 1, 2, DiffUtil.DiffType.MODIFIED),
                new DiffUtil.DiffItem("d", "D", "d", 1, 1, DiffUtil.DiffType.UNCHANGED)
        ));
        assertEquals(3, DiffUtil.filterChanged(result).items().size());
        assertEquals(1, DiffUtil.filterAdded(result).items().size());
        assertEquals(1, DiffUtil.filterRemoved(result).items().size());
        assertEquals(1, DiffUtil.filterModified(result).items().size());
        assertEquals(1, DiffUtil.filterByField(result, List.of("a")).items().size());
        assertEquals(1, DiffUtil.filterByPath(result, List.of("b")).items().size());
        assertEquals(1, DiffUtil.filterByType(result, DiffUtil.DiffType.MODIFIED).items().size());
        assertEquals(3, DiffUtil.excludeFields(result, List.of("a")).items().size());
        assertEquals(3, DiffUtil.excludePaths(result, List.of("a")).items().size());
        assertEquals(4, DiffUtil.toDiffItems(result).size());
        assertEquals(4, DiffUtil.toDiffMap(result).size());
        assertEquals(4, DiffUtil.toSummary(result).totalCount());
    }

    @Test
    void shouldFormatResult() {
        DiffUtil.DiffResult result = new DiffUtil.DiffResult(List.of(new DiffUtil.DiffItem("name", "名称", "name", "A", "B", DiffUtil.DiffType.MODIFIED)));
        assertTrue(DiffUtil.format(result).contains("A"));
        assertTrue(DiffUtil.formatAsText(result).contains("A"));
        assertEquals(1, DiffUtil.formatAsLines(result).size());
        assertTrue(DiffUtil.formatAsMarkdown(result).contains("名称"));
        assertTrue(DiffUtil.formatAsHtml(result).contains("<table>"));
        assertTrue(DiffUtil.formatAsJson(result).contains("MODIFIED"));
        assertTrue(DiffUtil.formatSummary(result).contains("修改：1"));
        assertEquals("名称：A -> B", DiffUtil.formatFieldDiff(new DiffUtil.FieldDiff("name", "名称", "A", "B", DiffUtil.DiffType.MODIFIED)));
        assertTrue(DiffUtil.formatCollectionDiff(DiffUtil.diffCollection(List.of(1), List.of(1, 2))).contains("新增"));
        assertTrue(DiffUtil.formatTextDiff(DiffUtil.diffLines("a", "b")).contains("a"));
    }

    @Test
    void shouldValidateFilterFormatArguments() {
        assertThrows(NullPointerException.class, () -> DiffUtil.filterByType(null, null));
        assertThrows(NullPointerException.class, () -> DiffUtil.formatFieldDiff(null));
        assertThrows(NullPointerException.class, () -> DiffUtil.formatCollectionDiff(null));
        assertThrows(NullPointerException.class, () -> DiffUtil.formatTextDiff(null));
    }
}
