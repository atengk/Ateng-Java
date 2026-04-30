package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationMergeTest {

    @Test
    void shouldMergeAnnotationAttributes() throws Exception {
        Marker primary = ChildSample.class.getDeclaredMethod("childMethod", String.class).getAnnotation(Marker.class);
        Marker fallback = BaseSample.class.getAnnotation(Marker.class);
        Map<String, Object> mergedAnnotations = AnnotationUtil.mergeAnnotations(primary, fallback);
        Map<String, Object> mergedAttributes = AnnotationUtil.mergeAttributes(primary, fallback);
        assertEquals("childMethod", mergedAnnotations.get("value"));
        assertEquals("childMethod", mergedAttributes.get("value"));
    }

    @Test
    void shouldMergeAttributeMapsAndOverride() {
        Map<String, Object> fallback = Map.of("value", "fallback", "enabled", false);
        Map<String, Object> primary = Map.of("value", "primary");
        Map<String, Object> merged = AnnotationUtil.mergeAttributes(primary, fallback);
        Map<String, Object> overridden = AnnotationUtil.overrideAttributes(fallback, primary);
        assertEquals("primary", merged.get("value"));
        assertEquals(false, merged.get("enabled"));
        assertEquals("primary", overridden.get("value"));
    }

    @Test
    void shouldFindMergedAttributes() throws Exception {
        Method method = ChildSample.class.getDeclaredMethod("childMethod", String.class);
        Map<String, Object> classAttributes = AnnotationUtil.findMergedAttributes(ChildSample.class, Marker.class);
        Map<String, Object> methodAttributes = AnnotationUtil.findMethodMergedAttributes(ChildSample.class, method, Marker.class);
        assertEquals("child", classAttributes.get("value"));
        assertEquals("childMethod", methodAttributes.get("value"));
        assertEquals(true, methodAttributes.get("enabled"));
    }

    @Test
    void shouldHandleInvalidMergeArguments() {
        assertTrue(AnnotationUtil.mergeAnnotations(null, null).isEmpty());
        assertTrue(AnnotationUtil.mergeAttributes((Map<String, Object>) null, (Map<String, Object>) null).isEmpty());
        assertTrue(AnnotationUtil.findMergedAttributes(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.findMethodMergedAttributes(null, null, Marker.class).isEmpty());
    }
}
