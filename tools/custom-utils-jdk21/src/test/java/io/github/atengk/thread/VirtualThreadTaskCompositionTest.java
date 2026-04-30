package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadTaskCompositionTest {

    @Test
    void parallelAndSequenceShouldReturnValues() throws Exception {
        List<java.util.function.Supplier<Integer>> suppliers = List.of(() -> 1, () -> 2, () -> 3);
        assertEquals(List.of(1, 2, 3), VirtualThreadUtil.sequence(suppliers));
        assertEquals(List.of(1, 2, 3), VirtualThreadUtil.parallel(suppliers));
    }

    @Test
    void anyOfAndRaceShouldReturnSuccessfulValue() throws Exception {
        List<java.util.function.Supplier<String>> suppliers = List.of(() -> "a", () -> "b");
        List<Callable<String>> tasks = List.of(() -> "x", () -> "y");
        assertNotNull(VirtualThreadUtil.anyOf(suppliers));
        assertNotNull(VirtualThreadUtil.race(tasks));
    }

    @Test
    void parallelFlatMapAndCombineShouldWork() throws Exception {
        List<Integer> flat = VirtualThreadUtil.parallelFlatMap(List.of(1, 2), value -> List.of(value, value * 10));
        assertEquals(List.of(1, 10, 2, 20), flat);
        String combined = VirtualThreadUtil.combine(() -> "A", () -> "B", (a, b) -> a + b);
        assertEquals("AB", combined);
    }

    @Test
    void thenShouldChainTask() {
        Integer value = VirtualThreadUtil.then(() -> 10, number -> number + 5);
        assertEquals(Integer.valueOf(15), value);
    }

    @Test
    void compositionInvalidArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.sequence(null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.then(null, value -> value));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.combine(() -> "a", () -> "b", null));
    }
}
