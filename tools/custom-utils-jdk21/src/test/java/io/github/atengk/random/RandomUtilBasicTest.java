package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilBasicTest {

    @Test
    void shouldGenerateBasicRandomValues() {
        assertTrue(RandomUtil.randomBit() == 0 || RandomUtil.randomBit() == 1);
        assertTrue(RandomUtil.randomSign() == 1 || RandomUtil.randomSign() == -1);
        assertTrue(RandomUtil.randomPercent() >= 0.0 && RandomUtil.randomPercent() < 1.0);
    }

    @Test
    void shouldHandleProbabilityBoundaries() {
        assertFalse(RandomUtil.randomBoolean(0.0));
        assertTrue(RandomUtil.randomBoolean(1.0));
        assertFalse(RandomUtil.randomChance(0));
        assertTrue(RandomUtil.randomChance(100));
        assertFalse(RandomUtil.hit(0.0));
        assertTrue(RandomUtil.hitPercent(100));
    }

    @Test
    void shouldRejectInvalidProbability() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomBoolean(-0.01));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomBoolean(1.01));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomChance(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomChance(101));
    }
}
