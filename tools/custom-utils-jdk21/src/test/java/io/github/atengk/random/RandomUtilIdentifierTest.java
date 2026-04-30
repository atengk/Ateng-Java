package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilIdentifierTest {

    @Test
    void shouldGenerateUuidAndSimpleUuid() {
        assertTrue(RandomUtil.randomUuid().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertTrue(RandomUtil.randomSimpleUuid().matches("[0-9a-f]{32}"));
        assertEquals(32, RandomUtil.randomTraceId().length());
    }

    @Test
    void shouldGenerateBusinessIdentifiers() {
        assertTrue(RandomUtil.randomRequestId().startsWith("REQ"));
        assertTrue(RandomUtil.randomBizId("USER").startsWith("USER"));
        assertTrue(RandomUtil.randomBizId("USER", 4).matches("USER\\d{21}"));
        assertTrue(RandomUtil.randomSnowflakeLikeId() > 0);
    }

    @Test
    void shouldRejectInvalidBusinessIdentifierArguments() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomBizId(""));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomBizId("USER", -1));
    }
}
