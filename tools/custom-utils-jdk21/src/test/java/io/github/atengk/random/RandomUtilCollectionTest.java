package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilCollectionTest {

    @Test
    void shouldSelectElementsFromCollectionAndArray() {
        List<String> values = List.of("A", "B", "C");
        assertTrue(values.contains(RandomUtil.randomElement(values)));
        assertTrue(values.contains(RandomUtil.randomElement(values.toArray(new String[0]))));
        assertTrue(RandomUtil.randomIndex(3) >= 0);
    }

    @Test
    void shouldSelectMultipleElementsAndShuffle() {
        List<Integer> values = List.of(1, 2, 3, 4, 5);
        assertEquals(3, RandomUtil.randomElements(values, 3).size());
        assertEquals(10, RandomUtil.randomElements(values, 10, true).size());
        assertEquals(2, RandomUtil.randomSubList(values, 2).size());

        List<Integer> shuffled = RandomUtil.shuffle(values);
        assertEquals(values.size(), shuffled.size());
        assertEquals(Set.copyOf(values), Set.copyOf(shuffled));

        List<Integer> mutable = new ArrayList<>(values);
        RandomUtil.shuffleInPlace(mutable);
        assertEquals(Set.copyOf(values), Set.copyOf(mutable));
    }

    @Test
    void shouldSelectMapData() {
        Map<String, Integer> map = Map.of("A", 1, "B", 2);
        assertTrue(map.containsKey(RandomUtil.randomMapKey(map)));
        assertTrue(map.containsValue(RandomUtil.randomMapValue(map)));
        assertTrue(map.entrySet().contains(RandomUtil.randomMapEntry(map)));
    }

    @Test
    void shouldRejectInvalidCollectionArguments() {
        assertThrows(NullPointerException.class, () -> RandomUtil.randomElement((List<String>) null));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomElement(List.of()));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomIndex(0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomElements(List.of(1), 2));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomElements(List.of(1), -1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomMapKey(Map.of()));
    }
}
