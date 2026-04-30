package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BeanDiffUtilTest {
    @Test
    void shouldDiffBeanFields() {
        UserData oldUser = new UserData(1L, "张三", 18, new AddressData("杭州", "西湖"));
        UserData newUser = new UserData(1L, "李四", 18, new AddressData("杭州", "西湖"));
        DiffUtil.DiffResult result = DiffUtil.diffBean(oldUser, newUser);
        assertTrue(result.hasDiff());
        assertEquals(List.of("name"), DiffUtil.getChangedFieldNames(oldUser, newUser));
        assertTrue(DiffUtil.hasFieldChanged(oldUser, newUser, "name"));
        assertFalse(DiffUtil.hasFieldChanged(oldUser, newUser, "age"));
        assertTrue(DiffUtil.hasAnyFieldChanged(oldUser, newUser, List.of("age", "name")));
        assertFalse(DiffUtil.hasAllFieldsChanged(oldUser, newUser, List.of("age", "name")));
    }

    @Test
    void shouldUseIncludeExcludeAndOptions() {
        UserData oldUser = new UserData(1L, "A", null, null);
        UserData newUser = new UserData(1L, "B", null, null);
        assertEquals(1, DiffUtil.diffIncludeFields(oldUser, newUser, List.of("name")).items().size());
        assertEquals(0, DiffUtil.diffExcludeFields(oldUser, newUser, List.of("name")).items().size());

        DiffUtil.DiffOptions options = DiffUtil.DiffOptions.builder()
                .fieldLabels(Map.of("name", "用户名"))
                .fieldComparator("name", (a, b) -> String.valueOf(a).equalsIgnoreCase(String.valueOf(b)))
                .build();
        assertFalse(DiffUtil.diffBean(new UserData(1L, "a", 1, null), new UserData(1L, "A", 1, null), options).hasDiff());
        assertEquals(4, DiffUtil.diffFields(oldUser, newUser).size());
        assertEquals(3, DiffUtil.diffUnchangedFields(oldUser, newUser).size());
    }

    @Test
    void shouldValidateBeanArguments() {
        UserData user = new UserData(1L, "A", 1, null);
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffField(user, user, "missing"));
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffFields(null, user));
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffBean(user, "other"));
    }
}
