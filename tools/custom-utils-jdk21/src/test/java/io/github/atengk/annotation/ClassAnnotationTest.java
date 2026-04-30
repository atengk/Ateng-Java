package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ClassAnnotationTest {

    @Test
    void shouldFindClassAnnotation() {
        assertTrue(AnnotationUtil.findClassAnnotation(ChildSample.class, Marker.class).isPresent());
        assertEquals("child", AnnotationUtil.findClassAnnotation(ChildSample.class, Marker.class).orElseThrow().value());
        assertTrue(AnnotationUtil.hasClassAnnotation(ChildSample.class, Marker.class));
    }

    @Test
    void shouldFindAnnotationInHierarchy() {
        assertTrue(AnnotationUtil.findInheritedAnnotation(ChildWithInherited.class, InheritedFlag.class).isPresent());
        assertTrue(AnnotationUtil.findAnnotationInHierarchy(ChildSample.class, Marker.class).isPresent());
        assertTrue(AnnotationUtil.hasAnnotationInHierarchy(ChildWithInherited.class, InheritedFlag.class));
    }

    @Test
    void shouldGetClassAnnotations() {
        assertTrue(AnnotationUtil.getClassAnnotations(ChildSample.class).length >= 3);
        assertTrue(AnnotationUtil.getDeclaredClassAnnotations(ChildSample.class).length >= 3);
    }

    @Test
    void shouldHandleInvalidClassArguments() {
        assertTrue(AnnotationUtil.findClassAnnotation(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.findInheritedAnnotation(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.findAnnotationInHierarchy(null, Marker.class).isEmpty());
        assertFalse(AnnotationUtil.hasClassAnnotation(null, Marker.class));
    }
}
