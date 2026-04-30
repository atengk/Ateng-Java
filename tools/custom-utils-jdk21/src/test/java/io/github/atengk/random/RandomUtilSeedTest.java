package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilSeedTest {

    @Test
    void shouldGenerateRepeatableValuesWithSeed() {
        RandomGenerator first = RandomUtil.withSeed(1L);
        RandomGenerator second = RandomUtil.withSeed(1L);
        assertEquals(first.nextInt(), second.nextInt());

        assertEquals(RandomUtil.randomInt(1L, 1, 100), RandomUtil.randomInt(1L, 1, 100));
        assertEquals(RandomUtil.randomString(1L, 10), RandomUtil.randomString(1L, 10));
    }

    @Test
    void shouldGenerateRepeatableCollectionResultsWithSeed() {
        List<Integer> values = List.of(1, 2, 3, 4, 5);
        assertEquals(RandomUtil.randomElements(1L, values, 3), RandomUtil.randomElements(1L, values, 3));
        assertEquals(RandomUtil.shuffle(1L, values), RandomUtil.shuffle(1L, values));
    }

    @Test
    void shouldHandleBoundaryAndException() {
        assertEquals("", RandomUtil.randomString(1L, 0));
        assertEquals(List.of(), RandomUtil.randomElements(1L, List.of(1), 0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomString(1L, -1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(1L, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomElements(1L, List.of(1), 2));
        assertThrows(NullPointerException.class, () -> RandomUtil.shuffle(1L, null));
    }
}
