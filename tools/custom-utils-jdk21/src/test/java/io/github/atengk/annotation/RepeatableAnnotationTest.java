package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class RepeatableAnnotationTest {

    @Test
    void shouldGetRepeatableAnnotations() throws Exception {
        Method method = BaseSample.class.getDeclaredMethod("baseMethod");
        List<Tag> tags = AnnotationUtil.getRepeatableAnnotations(method, Tag.class);
        List<Tag> declaredTags = AnnotationUtil.getDeclaredRepeatableAnnotations(method, Tag.class);
        assertEquals(2, tags.size());
        assertEquals(2, declaredTags.size());
        assertTrue(AnnotationUtil.hasRepeatableAnnotation(method, Tag.class));
    }

    @Test
    void shouldFindRepeatableAnnotationsInHierarchy() throws Exception {
        Method method = ChildSample.class.getDeclaredMethod("baseMethod");
        List<Tag> classTags = AnnotationUtil.findRepeatableAnnotationsInHierarchy(ChildSample.class, Tag.class);
        List<Tag> methodTags = AnnotationUtil.findMethodRepeatableAnnotationsInHierarchy(ChildSample.class, method, Tag.class);
        assertEquals(2, classTags.size());
        assertEquals(2, methodTags.size());
    }

    @Test
    void shouldHandleInvalidRepeatableArguments() {
        assertTrue(AnnotationUtil.getRepeatableAnnotations(null, Tag.class).isEmpty());
        assertTrue(AnnotationUtil.getDeclaredRepeatableAnnotations(null, Tag.class).isEmpty());
        assertFalse(AnnotationUtil.hasRepeatableAnnotation(null, Tag.class));
        assertTrue(AnnotationUtil.findRepeatableAnnotationsInHierarchy(null, Tag.class).isEmpty());
        assertTrue(AnnotationUtil.findMethodRepeatableAnnotationsInHierarchy(null, null, Tag.class).isEmpty());
    }
}
