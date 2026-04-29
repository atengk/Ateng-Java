package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeAssertUtilTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldPassWhenTimeRulesAreValid() {
        LocalDate today = LocalDate.now(clock);
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        assertEquals(yesterday, AssertUtil.before(yesterday, today, "必须早于目标时间"));
        assertEquals(tomorrow, AssertUtil.after(tomorrow, today, "必须晚于目标时间"));
        assertEquals(today, AssertUtil.between(today, yesterday, tomorrow, "必须在时间范围内"));
        assertEquals(today.minusDays(2), AssertUtil.notBetween(today.minusDays(2), yesterday, tomorrow, "不能在时间范围内"));
        assertEquals(yesterday, AssertUtil.past(yesterday, clock, "必须是过去时间"));
        assertEquals(tomorrow, AssertUtil.future(tomorrow, clock, "必须是未来时间"));
        assertEquals(today, AssertUtil.pastOrPresent(today, clock, "必须是过去或当前时间"));
        assertEquals(today, AssertUtil.futureOrPresent(today, clock, "必须是未来或当前时间"));

        assertDoesNotThrow(() -> AssertUtil.startBeforeEnd(yesterday, today, "开始时间必须早于结束时间"));
        assertDoesNotThrow(() -> AssertUtil.startBeforeOrEqualEnd(today, today, "开始时间必须早于或等于结束时间"));
    }

    @Test
    void shouldSupportLocalDateTime() {
        LocalDateTime now = LocalDateTime.now(clock);
        assertEquals(now.minusSeconds(1), AssertUtil.past(now.minusSeconds(1), clock, "必须是过去时间"));
        assertEquals(now.plusSeconds(1), AssertUtil.future(now.plusSeconds(1), clock, "必须是未来时间"));
    }

    @Test
    void shouldThrowWhenTimeRulesAreInvalid() {
        LocalDate today = LocalDate.now(clock);
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.before(today, yesterday, "必须早于目标时间"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.after(today, tomorrow, "必须晚于目标时间"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.between(today.minusDays(2), yesterday, tomorrow, "必须在时间范围内"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notBetween(today, yesterday, tomorrow, "不能在时间范围内"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.past(tomorrow, clock, "必须是过去时间"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.future(yesterday, clock, "必须是未来时间"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.pastOrPresent(tomorrow, clock, "必须是过去或当前时间"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.futureOrPresent(yesterday, clock, "必须是未来或当前时间"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.startBeforeEnd(today, today, "开始时间必须早于结束时间"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.startBeforeOrEqualEnd(tomorrow, today, "开始时间必须早于或等于结束时间"));
    }

    @Test
    void shouldThrowWhenTimeArgumentsAreIllegal() {
        LocalDate today = LocalDate.now(clock);

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.before(null, today, "时间不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.between(today, today.plusDays(1), today, "范围错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.past(today, null, "时钟不能为空"));
    }
}
