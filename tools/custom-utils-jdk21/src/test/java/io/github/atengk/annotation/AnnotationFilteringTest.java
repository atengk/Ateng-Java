package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.List;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationFilteringTest {

    @Test
    void shouldFilterAnnotationsByPredicatePackageAndMeta() {
        Annotation[] annotations = ChildSample.class.getAnnotations();
        List<Annotation> filtered = AnnotationUtil.filterAnnotations(annotations, annotation -> annotation.annotationType().equals(Marker.class));
        List<Annotation> byPredicate = AnnotationUtil.getAnnotationsByPredicate(ChildSample.class, annotation -> annotation.annotationType().equals(Secondary.class));
        List<Annotation> byPackage = AnnotationUtil.getAnnotationsByPackage(ChildSample.class, "io.github.atengk.util");
        List<Annotation> byMeta = AnnotationUtil.getAnnotationsByMeta(ChildSample.class, MetaFlag.class);
        assertEquals(1, filtered.size());
        assertEquals(1, byPredicate.size());
        assertFalse(byPackage.isEmpty());
        assertEquals(1, byMeta.size());
    }

    @Test
    void shouldExcludeJdkAnnotationsAndJudgeCustomAnnotation() {
        Annotation[] annotations = Composed.class.getAnnotations();
        List<Annotation> nonJdkAnnotations = AnnotationUtil.excludeJdkAnnotations(annotations);
        assertTrue(AnnotationUtil.isJdkAnnotation(Retention.class));
        assertTrue(AnnotationUtil.isCustomAnnotation(Marker.class));
        assertTrue(nonJdkAnnotations.stream().anyMatch(annotation -> annotation.annotationType().equals(MetaFlag.class)));
    }

    @Test
    void shouldHandleInvalidFilterArguments() {
        assertTrue(AnnotationUtil.filterAnnotations(null, annotation -> true).isEmpty());
        assertTrue(AnnotationUtil.getAnnotationsByPredicate(null, annotation -> true).isEmpty());
        assertTrue(AnnotationUtil.getAnnotationsByPackage(null, "io.github").isEmpty());
        assertTrue(AnnotationUtil.getAnnotationsByPackage(ChildSample.class, " ").isEmpty());
        assertFalse(AnnotationUtil.isJdkAnnotation(null));
        assertFalse(AnnotationUtil.isCustomAnnotation(null));
    }
}
