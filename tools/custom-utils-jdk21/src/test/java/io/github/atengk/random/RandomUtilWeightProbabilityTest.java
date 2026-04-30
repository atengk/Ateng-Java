package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilWeightProbabilityTest {

    record Prize(String name, int weight) {
    }

    @Test
    void shouldGenerateByWeightAndRate() {
        assertEquals("A", RandomUtil.randomByWeight(Map.of("A", 10, "B", 0)));
        assertEquals("A", RandomUtil.randomByRate(Map.of("A", 1.0, "B", 0.0)));
        assertEquals(0, RandomUtil.randomWeightedIndex(new int[]{10, 0, 0}));
        assertEquals("bucketA", RandomUtil.randomBucket(Map.of("bucketA", 1)));
        assertEquals("groupA", RandomUtil.randomTrafficGroup(Map.of("groupA", 1)));
    }

    @Test
    void shouldGenerateByObjectWeight() {
        Prize prize = RandomUtil.randomByWeight(List.of(new Prize("A", 1), new Prize("B", 0)), Prize::weight);
        assertEquals("A", prize.name());
    }

    @Test
    void shouldRejectInvalidWeightArguments() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomByWeight(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomByWeight(Map.of("A", -1)));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomByWeight(Map.of("A", 0)));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomByRate(Map.of("A", -0.1)));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomByRate(Map.of("A", 0.0)));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomWeightedIndex(new int[]{}));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomWeightedIndex(new int[]{0, 0}));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomWeightedIndex(new int[]{1, -1}));
    }
}
