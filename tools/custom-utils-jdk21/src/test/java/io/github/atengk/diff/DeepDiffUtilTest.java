package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeepDiffUtilTest {
    @Test
    void shouldDiffNestedObjectPath() {
        UserData oldUser = new UserData(1L, "A", 18, new AddressData("杭州", "西湖"));
        UserData newUser = new UserData(1L, "A", 18, new AddressData("上海", "浦东"));
        assertTrue(DiffUtil.diffDeep(oldUser, newUser).hasDiff());
        assertTrue(DiffUtil.getChangedPaths(oldUser, newUser).stream().anyMatch(path -> path.contains("city")));
        assertTrue(DiffUtil.hasPathChanged(oldUser, newUser, "address.city"));
        assertEquals(1, DiffUtil.diffByPath(oldUser, newUser, "address.city").items().size());
        assertTrue(DiffUtil.flattenDiff(oldUser, newUser).hasDiff());
        assertTrue(DiffUtil.diffNestedBean(oldUser.address(), newUser.address(), "user.address").hasDiff());
    }

    @Test
    void shouldValidateInvalidPath() {
        UserData user = new UserData(1L, "A", 18, null);
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffByPath(user, user, " "));
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffByPath(user, user, "missing.name"));
    }
}
