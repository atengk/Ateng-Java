package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class BasicAnnotationJudgementTest {

    @Test
    void shouldJudgeAnnotationPresence() {
        assertTrue(AnnotationUtil.hasAnnotation(ChildSample.class, Marker.class));
        assertTrue(AnnotationUtil.hasAnyAnnotation(ChildSample.class, Marker.class, Secondary.class));
        assertTrue(AnnotationUtil.hasAllAnnotations(ChildSample.class, Marker.class, Secondary.class));
        assertTrue(AnnotationUtil.hasDeclaredAnnotation(ChildSample.class, Marker.class));
    }

    @Test
    void shouldReturnFalseForMissingOrInvalidArguments() {
        assertFalse(AnnotationUtil.hasAnnotation(EmptySample.class, Marker.class));
        assertFalse(AnnotationUtil.hasAnnotation(null, Marker.class));
        assertFalse(AnnotationUtil.hasAnnotation(ChildSample.class, null));
        assertFalse(AnnotationUtil.hasAnyAnnotation(ChildSample.class));
        assertFalse(AnnotationUtil.hasAllAnnotations(ChildSample.class));
    }
}
