package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class MethodAnnotationTest {

    @Test
    void shouldFindMethodAnnotation() throws Exception {
        Method method = ChildSample.class.getDeclaredMethod("childMethod", String.class);
        assertTrue(AnnotationUtil.findMethodAnnotation(method, Marker.class).isPresent());
        assertTrue(AnnotationUtil.findMethodAnnotation(ChildSample.class, "childMethod", Marker.class).isPresent());
        assertTrue(AnnotationUtil.hasMethodAnnotation(method, Marker.class));
    }

    @Test
    void shouldFindInterfaceMethodAnnotationInHierarchy() throws Exception {
        Method method = ChildSample.class.getDeclaredMethod("apiMethod");
        assertTrue(AnnotationUtil.findMethodAnnotationInHierarchy(ChildSample.class, method, MethodFlag.class).isPresent());
        assertTrue(AnnotationUtil.hasMethodAnnotationInHierarchy(ChildSample.class, method, MethodFlag.class));
    }

    @Test
    void shouldGetAnnotatedMethods() {
        List<Method> declaredMethods = AnnotationUtil.getAnnotatedMethods(ChildSample.class, Marker.class);
        List<Method> allMethods = AnnotationUtil.getAllAnnotatedMethods(ChildSample.class, Marker.class);
        assertEquals(1, declaredMethods.size());
        assertTrue(allMethods.stream().anyMatch(method -> method.getName().equals("baseMethod")));
    }

    @Test
    void shouldHandleInvalidMethodArguments() throws Exception {
        Method method = ChildSample.class.getDeclaredMethod("noAnnotationMethod");
        assertTrue(AnnotationUtil.findMethodAnnotation(null, Marker.class).isEmpty());
        assertTrue(AnnotationUtil.findMethodAnnotation(ChildSample.class, " ", Marker.class).isEmpty());
        assertTrue(AnnotationUtil.findMethodAnnotationInHierarchy(null, method, Secondary.class).isEmpty());
        assertFalse(AnnotationUtil.hasMethodAnnotation(null, Marker.class));
        assertEquals(0, AnnotationUtil.getMethodAnnotations(null).length);
    }
}
