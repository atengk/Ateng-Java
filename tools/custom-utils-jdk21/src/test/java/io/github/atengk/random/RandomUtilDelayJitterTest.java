package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilDelayJitterTest {

    @Test
    void shouldGenerateDelayAndJitterValues() {
        long sleep = RandomUtil.randomSleepMillis(10, 20);
        assertTrue(sleep >= 10 && sleep <= 20);

        Duration delay = RandomUtil.randomDelay(Duration.ofMillis(10), Duration.ofMillis(20));
        assertTrue(delay.toMillis() >= 10 && delay.toMillis() <= 20);

        long jitter = RandomUtil.randomJitter(100, 0.1);
        assertTrue(jitter >= 90 && jitter <= 110);

        long backoff = RandomUtil.randomBackoff(2, 100, 1000);
        assertTrue(backoff >= 100 && backoff <= 400);

        assertTrue(RandomUtil.randomCronSecond() >= 0 && RandomUtil.randomCronSecond() <= 59);
        assertTrue(RandomUtil.randomCronMinute() >= 0 && RandomUtil.randomCronMinute() <= 59);
    }

    @Test
    void shouldHandleBoundaryAndException() {
        assertEquals(10, RandomUtil.randomSleepMillis(10, 10));
        assertEquals(Duration.ofMillis(5), RandomUtil.randomDelay(Duration.ofMillis(5), Duration.ofMillis(5)));
        assertEquals(100, RandomUtil.randomJitter(100, 0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomSleepMillis(20, 10));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomDelay(Duration.ofMillis(20), Duration.ofMillis(10)));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomJitter(100, -0.1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomBackoff(-1, 100, 1000));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomBackoff(1, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomBackoff(1, 1000, 100));
    }
}
