package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilEnumTest {

    enum Status {
        ENABLED, DISABLED, DELETED
    }

    @Test
    void shouldGenerateEnumValues() {
        assertNotNull(RandomUtil.randomEnum(Status.class));
        assertTrue(List.of(Status.values()).contains(RandomUtil.randomEnum(List.of(Status.ENABLED, Status.DISABLED))));
        assertTrue(RandomUtil.randomEnumName(Status.class).matches("ENABLED|DISABLED|DELETED"));
        assertTrue(RandomUtil.randomEnumOrdinal(Status.class) >= 0);
    }

    @Test
    void shouldExcludeEnumValues() {
        assertEquals(Status.ENABLED, RandomUtil.randomEnum(Status.class, Status.DISABLED, Status.DELETED));
    }

    @Test
    void shouldRejectInvalidEnumArguments() {
        assertThrows(NullPointerException.class, () -> RandomUtil.randomEnum((Class<Status>) null));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomEnum(Status.class, Status.values()));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomEnum(List.<Status>of()));
    }
}
