package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class BasicAnnotationRetrievalTest {

    @Test
    void shouldGetAndFindAnnotation() {
        Marker marker = AnnotationUtil.getAnnotation(ChildSample.class, Marker.class);
        assertNotNull(marker);
        assertEquals("child", marker.value());
        assertTrue(AnnotationUtil.findAnnotation(ChildSample.class, Marker.class).isPresent());
        assertNotNull(AnnotationUtil.getDeclaredAnnotation(ChildSample.class, Marker.class));
    }

    @Test
    void shouldGetAllAnnotations() {
        Annotation[] annotations = AnnotationUtil.getAnnotations(ChildSample.class);
        Annotation[] declaredAnnotations = AnnotationUtil.getDeclaredAnnotations(ChildSample.class);
        assertTrue(annotations.length >= 3);
        assertTrue(declaredAnnotations.length >= 3);
    }

    @Test
    void shouldGetRepeatableAnnotationsByType() throws Exception {
        Method method = BaseSample.class.getDeclaredMethod("baseMethod");
        List<Tag> tags = AnnotationUtil.getAnnotationsByType(method, Tag.class);
        List<Tag> declaredTags = AnnotationUtil.getDeclaredAnnotationsByType(method, Tag.class);
        assertEquals(2, tags.size());
        assertEquals(2, declaredTags.size());
    }

    @Test
    void shouldReturnEmptyWhenArgumentsInvalid() {
        assertNull(AnnotationUtil.getAnnotation(null, Marker.class));
        assertNull(AnnotationUtil.getDeclaredAnnotation(null, Marker.class));
        assertEquals(0, AnnotationUtil.getAnnotations(null).length);
        assertEquals(0, AnnotationUtil.getDeclaredAnnotations(null).length);
        assertTrue(AnnotationUtil.getAnnotationsByType(null, Tag.class).isEmpty());
    }
}
