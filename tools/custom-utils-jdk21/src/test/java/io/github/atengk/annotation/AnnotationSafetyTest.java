package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationSafetyTest {

    @Test
    void shouldCheckAndRequireAnnotationType() {
        assertTrue(AnnotationUtil.checkAnnotationType(Marker.class));
        assertTrue(AnnotationUtil.isAnnotationType(Marker.class));
        assertEquals(Marker.class, AnnotationUtil.requireAnnotationType(Marker.class));
        assertThrows(IllegalArgumentException.class, () -> AnnotationUtil.requireAnnotationType(String.class));
    }

    @Test
    void shouldRequireElementAndSafeRead() {
        Marker marker = ChildSample.class.getAnnotation(Marker.class);
        assertEquals(ChildSample.class, AnnotationUtil.requireElement(ChildSample.class));
        assertThrows(IllegalArgumentException.class, () -> AnnotationUtil.requireElement(null));
        assertTrue(AnnotationUtil.safeGetAnnotation(ChildSample.class, Marker.class).isPresent());
        assertEquals("child", AnnotationUtil.safeReadAttribute(marker, "value").orElseThrow());
        assertTrue(AnnotationUtil.safeReadAttribute(marker, "missing").isEmpty());
    }

    @Test
    void shouldFindAttributeMethods() throws Exception {
        Method valueMethod = Marker.class.getDeclaredMethod("value");
        List<Method> methods = AnnotationUtil.getAttributeMethods(Marker.class);
        assertTrue(AnnotationUtil.isAttributeMethod(valueMethod));
        assertTrue(methods.stream().anyMatch(method -> method.getName().equals("value")));
        assertFalse(AnnotationUtil.isAttributeMethod(null));
        assertTrue(AnnotationUtil.getAttributeMethods(null).isEmpty());
    }
}
