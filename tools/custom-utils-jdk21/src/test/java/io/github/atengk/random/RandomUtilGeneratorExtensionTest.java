package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilGeneratorExtensionTest {

    @AfterEach
    void tearDown() {
        RandomUtil.setRandomGenerator(null);
    }

    @Test
    void shouldCreateGenerators() {
        assertNotNull(RandomUtil.newRandomGenerator());
        assertInstanceOf(SecureRandom.class, RandomUtil.newSecureRandom());
        assertNotNull(RandomUtil.getRandomGenerator());
    }

    @Test
    void shouldSetCustomGenerator() {
        RandomGenerator generator = RandomUtil.withSeed(1L);
        RandomUtil.setRandomGenerator(generator);
        assertSame(generator, RandomUtil.getRandomGenerator());
    }

    @Test
    void shouldRunRandomWithCustomGenerator() {
        Integer value = RandomUtil.randomWith(RandomUtil.withSeed(1L), g -> g.nextInt(10));
        assertNotNull(value);
        assertTrue(value >= 0 && value < 10);
    }

    @Test
    void shouldRejectInvalidRandomWithArguments() {
        assertThrows(NullPointerException.class, () -> RandomUtil.randomWith(null, g -> 1));
        assertThrows(NullPointerException.class, () -> RandomUtil.randomWith(RandomUtil.withSeed(1L), null));
    }
}
