package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class BatchElementScanTest {

    @Test
    void shouldScanAnnotatedMethodsFieldsConstructorsAndRecordComponents() {
        List<Method> methods = AnnotationUtil.getAllAnnotatedMethods(ChildSample.class, Marker.class);
        List<java.lang.reflect.Field> fields = AnnotationUtil.getAllAnnotatedFields(ChildSample.class, FieldFlag.class);
        List<Constructor<?>> constructors = AnnotationUtil.getAnnotatedConstructors(ChildSample.class, ConstructorFlag.class);
        List<RecordComponent> recordComponents = AnnotationUtil.getAnnotatedRecordComponents(UserRecord.class, RecordFlag.class);
        assertFalse(methods.isEmpty());
        assertEquals(2, fields.size());
        assertEquals(1, constructors.size());
        assertEquals(1, recordComponents.size());
    }

    @Test
    void shouldScanAllAnnotatedElements() {
        List<AnnotatedElement> elements = AnnotationUtil.getAnnotatedElements(ChildSample.class, FieldFlag.class);
        assertEquals(2, elements.size());
        assertTrue(AnnotationUtil.getAnnotatedElements(ChildSample.class, Marker.class).size() >= 2);
    }

    @Test
    void shouldHandleInvalidScanArguments() {
        assertTrue(AnnotationUtil.getAnnotatedMethods(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.getAllAnnotatedMethods(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.getAnnotatedConstructors(null, ConstructorFlag.class).isEmpty());
        assertTrue(AnnotationUtil.getAnnotatedElements(null, Marker.class).isEmpty());
    }
}
