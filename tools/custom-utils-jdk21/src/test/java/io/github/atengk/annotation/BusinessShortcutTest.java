package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class BusinessShortcutTest {

    @Test
    void shouldFindMethodOrClassAnnotation() throws Exception {
        Method method = ChildSample.class.getDeclaredMethod("childMethod", String.class);
        Optional<Marker> annotation = AnnotationUtil.findMethodOrClassAnnotation(ChildSample.class, method, Marker.class);
        Optional<Marker> aliasAnnotation = AnnotationUtil.findClassOrMethodAnnotation(ChildSample.class, method, Marker.class);
        assertTrue(annotation.isPresent());
        assertEquals("childMethod", annotation.orElseThrow().value());
        assertTrue(aliasAnnotation.isPresent());
        assertTrue(AnnotationUtil.hasClassOrMethodAnnotation(ChildSample.class, method, Marker.class));
    }

    @Test
    void shouldFindFirstAnnotationAndAttributes() {
        Optional<Annotation> firstAnnotation = AnnotationUtil.findFirstAnnotation(ChildSample.class, Secondary.class, Marker.class);
        Optional<Object> value = AnnotationUtil.getAnnotationValue(ChildSample.class, Marker.class);
        Optional<Object> attribute = AnnotationUtil.getAnnotationAttribute(ChildSample.class, Marker.class, "value");
        Map<String, Object> attributes = AnnotationUtil.getAnnotationAttributes(ChildSample.class, Marker.class);
        assertTrue(firstAnnotation.isPresent());
        assertEquals(Secondary.class, firstAnnotation.orElseThrow().annotationType());
        assertEquals("child", value.orElseThrow());
        assertEquals("child", attribute.orElseThrow());
        assertEquals("child", attributes.get("value"));
    }

    @Test
    void shouldHandleInvalidShortcutArguments() {
        assertTrue(AnnotationUtil.findMethodOrClassAnnotation(null, null, Marker.class).isEmpty());
        assertFalse(AnnotationUtil.hasClassOrMethodAnnotation(null, null, Marker.class));
        assertTrue(AnnotationUtil.findFirstAnnotation(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.getAnnotationValue(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.getAnnotationAttribute(null, Marker.class, "value").isEmpty());
        assertTrue(AnnotationUtil.getAnnotationAttributes(null, Marker.class).isEmpty());
    }
}
