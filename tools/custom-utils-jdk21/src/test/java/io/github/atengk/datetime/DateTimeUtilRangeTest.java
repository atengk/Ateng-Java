package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilRangeTest {

    @Test
    void shouldCheckBetweenAndOverlap() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 29, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 29, 12, 0);

        assertTrue(DateTimeUtil.isBetween(LocalDateTime.of(2026, 4, 29, 12, 0), start, end));
        assertFalse(DateTimeUtil.isBetweenHalfOpen(LocalDateTime.of(2026, 4, 29, 12, 0), start, end));
        assertTrue(DateTimeUtil.isOverlap(start, end, LocalDateTime.of(2026, 4, 29, 11, 0), LocalDateTime.of(2026, 4, 29, 13, 0)));
        assertFalse(DateTimeUtil.isOverlap(start, end, end, LocalDateTime.of(2026, 4, 29, 13, 0)));
    }

    @Test
    void shouldGetIntersectionAndMergeRanges() {
        LocalDateTime ten = LocalDateTime.of(2026, 4, 29, 10, 0);
        LocalDateTime eleven = LocalDateTime.of(2026, 4, 29, 11, 0);
        LocalDateTime twelve = LocalDateTime.of(2026, 4, 29, 12, 0);
        LocalDateTime thirteen = LocalDateTime.of(2026, 4, 29, 13, 0);

        Optional<DateTimeUtil.TimeRange> intersection = DateTimeUtil.intersection(ten, twelve, eleven, thirteen);
        assertTrue(intersection.isPresent());
        assertEquals(new DateTimeUtil.TimeRange(eleven, twelve), intersection.get());

        List<DateTimeUtil.TimeRange> merged = DateTimeUtil.mergeRanges(List.of(
                new DateTimeUtil.TimeRange(eleven, twelve),
                new DateTimeUtil.TimeRange(ten, eleven),
                new DateTimeUtil.TimeRange(twelve, thirteen)
        ));
        assertEquals(List.of(new DateTimeUtil.TimeRange(ten, thirteen)), merged);
    }

    @Test
    void shouldSplitRanges() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 29, 22, 30);
        LocalDateTime end = LocalDateTime.of(2026, 4, 30, 1, 30);

        assertEquals(2, DateTimeUtil.splitByDay(start, end).size());
        assertEquals(4, DateTimeUtil.splitByHour(start, end).size());
        assertEquals(Duration.ofHours(3), new DateTimeUtil.TimeRange(start, end).duration());
    }

    @Test
    void shouldCheckValidRange() {
        assertTrue(DateTimeUtil.isValidRange(LocalDateTime.of(2026, 4, 29, 10, 0), LocalDateTime.of(2026, 4, 29, 11, 0)));
        assertFalse(DateTimeUtil.isValidRange(LocalDateTime.of(2026, 4, 29, 11, 0), LocalDateTime.of(2026, 4, 29, 10, 0)));
    }
}
