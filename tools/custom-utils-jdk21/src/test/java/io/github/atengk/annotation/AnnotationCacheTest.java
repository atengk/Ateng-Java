package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Map;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationCacheTest {

    @AfterEach
    void resetCache() {
        AnnotationUtil.setCacheEnabled(true);
        AnnotationUtil.clearCache();
    }

    @Test
    void shouldGetCachedAnnotationAndAnnotations() {
        Annotation[] annotations = AnnotationUtil.getCachedAnnotations(ChildSample.class);
        assertTrue(annotations.length >= 3);
        assertTrue(AnnotationUtil.getCachedAnnotation(ChildSample.class, Marker.class).isPresent());
        AnnotationUtil.clearCache(ChildSample.class);
        assertTrue(AnnotationUtil.getCachedAnnotation(ChildSample.class, Marker.class).isPresent());
    }

    @Test
    void shouldGetCachedAttributesAndToggleCache() {
        Marker marker = ChildSample.class.getAnnotation(Marker.class);
        Map<String, Object> attributes = AnnotationUtil.getCachedAttributes(marker);
        assertEquals("child", attributes.get("value"));
        AnnotationUtil.setCacheEnabled(false);
        assertFalse(AnnotationUtil.isCacheEnabled());
        assertEquals("child", AnnotationUtil.getCachedAttributes(marker).get("value"));
    }

    @Test
    void shouldHandleInvalidCacheArguments() {
        assertEquals(0, AnnotationUtil.getCachedAnnotations(null).length);
        assertTrue(AnnotationUtil.getCachedAnnotation(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.getCachedAttributes(null).isEmpty());
    }
}
