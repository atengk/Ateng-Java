package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditDiffUtilTest {
    @Test
    void shouldCreateAuditLogs() {
        UserData oldUser = new UserData(1L, "A", 18, null);
        UserData newUser = new UserData(1L, "B", 18, null);
        DiffUtil.DiffResult result = DiffUtil.diffForAudit(oldUser, newUser);
        assertTrue(result.hasDiff());
        assertTrue(DiffUtil.toChangeLog(result).content().contains("A"));
        assertEquals(1, DiffUtil.toChangeLogs(result.items()).size());
        assertTrue(DiffUtil.toReadableText(result).contains("name"));
        assertEquals(1, DiffUtil.toReadableLines(result).size());
        assertTrue(DiffUtil.toFieldChangeMap(result).containsKey("name"));
        assertEquals(List.of("name"), DiffUtil.getChangedLabels(result));
        assertEquals("用户名：A -> B", DiffUtil.formatChange("用户名", "A", "B"));
        assertTrue(DiffUtil.formatChangeLog(result.items().getFirst()).contains("A"));
    }

    @Test
    void shouldUseAuditOptions() {
        UserData oldUser = new UserData(1L, "A", 18, null);
        UserData newUser = new UserData(1L, "B", 18, null);
        assertFalse(DiffUtil.diffForAudit(oldUser, newUser, DiffUtil.ignoreFields(List.of("name"))).hasDiff());
    }
}
