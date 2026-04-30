package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonDiffUtilTest {
    @Test
    void shouldDiffJsonPath() {
        String oldJson = "{\"user\":{\"name\":\"A\",\"age\":18},\"items\":[{\"id\":1,\"name\":\"old\"}]}";
        String newJson = "{\"user\":{\"name\":\"B\",\"age\":18},\"items\":[{\"id\":1,\"name\":\"new\"},{\"id\":2,\"name\":\"add\"}]}";
        assertTrue(DiffUtil.diffJson(oldJson, newJson).hasDiff());
        assertTrue(DiffUtil.getChangedJsonPaths(oldJson, newJson).contains("$.user.name"));
        assertTrue(DiffUtil.hasJsonChanged(oldJson, newJson));
        assertTrue(DiffUtil.hasJsonPathChanged(oldJson, newJson, "$.user.name"));
        assertTrue(DiffUtil.diffJsonArrayByKey(oldJson, newJson, "$.items", "id").hasDiff());
        assertTrue(DiffUtil.flattenJsonDiff(oldJson, newJson).hasDiff());
    }

    @Test
    void shouldNormalizeAndIgnoreOrder() {
        assertEquals("{\"a\":1,\"b\":2}", DiffUtil.normalizeJson("{\"b\":2,\"a\":1}"));
        assertFalse(DiffUtil.normalizeJsonAndDiff("{\"b\":2,\"a\":1}", "{\"a\":1,\"b\":2}").hasDiff());
        assertFalse(DiffUtil.diffJsonIgnoreOrder("[2,1]", "[1,2]").hasDiff());
        assertFalse(DiffUtil.diffJson(Map.of("a", 1), Map.of("a", 1)).hasDiff());
    }

    @Test
    void shouldValidateInvalidJson() {
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffJson("{", "{}"));
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffJsonArrayByKey("{}", "{}", "$.items", "id"));
    }
}
