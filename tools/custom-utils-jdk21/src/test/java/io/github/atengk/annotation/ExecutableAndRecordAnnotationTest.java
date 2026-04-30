package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.List;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ExecutableAndRecordAnnotationTest {

    @Test
    void shouldFindConstructorAnnotation() throws Exception {
        Constructor<ChildSample> constructor = ChildSample.class.getDeclaredConstructor();
        assertTrue(AnnotationUtil.findConstructorAnnotation(constructor, ConstructorFlag.class).isPresent());
        assertEquals(1, AnnotationUtil.getConstructorAnnotations(constructor).length);
        assertEquals(1, AnnotationUtil.getAnnotatedConstructors(ChildSample.class, ConstructorFlag.class).size());
    }

    @Test
    void shouldFindParameterAnnotation() throws Exception {
        Method method = ChildSample.class.getDeclaredMethod("childMethod", String.class);
        Parameter parameter = method.getParameters()[0];
        assertTrue(AnnotationUtil.findParameterAnnotation(parameter, ParameterFlag.class).isPresent());
        assertEquals(1, AnnotationUtil.getParameterAnnotations(parameter).length);
        assertEquals(1, AnnotationUtil.getAnnotatedParameters(method, ParameterFlag.class).size());
    }

    @Test
    void shouldFindRecordComponentAnnotation() {
        RecordComponent component = UserRecord.class.getRecordComponents()[0];
        assertTrue(AnnotationUtil.findRecordComponentAnnotation(component, RecordFlag.class).isPresent());
        assertEquals(1, AnnotationUtil.getRecordComponentAnnotations(component).length);
        List<RecordComponent> components = AnnotationUtil.getAnnotatedRecordComponents(UserRecord.class, RecordFlag.class);
        assertEquals(1, components.size());
    }

    @Test
    void shouldHandleInvalidExecutableArguments() {
        assertTrue(AnnotationUtil.findConstructorAnnotation(null, ConstructorFlag.class).isEmpty());
        assertTrue(AnnotationUtil.findParameterAnnotation(null, ParameterFlag.class).isEmpty());
        assertTrue(AnnotationUtil.findRecordComponentAnnotation(null, RecordFlag.class).isEmpty());
        assertTrue(AnnotationUtil.getAnnotatedParameters(null, ParameterFlag.class).isEmpty());
        assertTrue(AnnotationUtil.getAnnotatedRecordComponents(ChildSample.class, RecordFlag.class).isEmpty());
    }
}
