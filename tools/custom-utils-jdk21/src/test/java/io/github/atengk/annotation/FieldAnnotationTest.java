package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class FieldAnnotationTest {

    @Test
    void shouldFindFieldAnnotation() throws Exception {
        Field field = ChildSample.class.getDeclaredField("childField");
        assertTrue(AnnotationUtil.findFieldAnnotation(field, FieldFlag.class).isPresent());
        assertTrue(AnnotationUtil.findFieldAnnotation(ChildSample.class, "childField", FieldFlag.class).isPresent());
        assertTrue(AnnotationUtil.hasFieldAnnotation(field, FieldFlag.class));
    }

    @Test
    void shouldFindParentFieldAnnotationInHierarchy() {
        assertTrue(AnnotationUtil.findFieldAnnotationInHierarchy(ChildSample.class, "baseField", FieldFlag.class).isPresent());
    }

    @Test
    void shouldGetAnnotatedFields() {
        List<Field> declaredFields = AnnotationUtil.getAnnotatedFields(ChildSample.class, FieldFlag.class);
        List<Field> allFields = AnnotationUtil.getAllAnnotatedFields(ChildSample.class, FieldFlag.class);
        assertEquals(1, declaredFields.size());
        assertEquals(2, allFields.size());
    }

    @Test
    void shouldHandleInvalidFieldArguments() throws Exception {
        Field field = EmptySample.class.getDeclaredField("emptyField");
        assertTrue(AnnotationUtil.findFieldAnnotation(null, FieldFlag.class).isEmpty());
        assertTrue(AnnotationUtil.findFieldAnnotation(ChildSample.class, "missing", FieldFlag.class).isEmpty());
        assertTrue(AnnotationUtil.findFieldAnnotationInHierarchy(null, "childField", FieldFlag.class).isEmpty());
        assertFalse(AnnotationUtil.hasFieldAnnotation(field, FieldFlag.class));
        assertEquals(0, AnnotationUtil.getFieldAnnotations(null).length);
    }
}
