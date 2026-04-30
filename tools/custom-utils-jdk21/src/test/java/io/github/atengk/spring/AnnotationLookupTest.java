package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationLookupTest {

    @Test
    void shouldFindClassAndMethodAnnotations() throws Exception {
        Method method = AnnotatedSample.class.getDeclaredMethod("run");
        assertNotNull(SpringUtil.findAnnotation(AnnotatedSample.class, SampleAnnotation.class));
        assertNotNull(SpringUtil.getAnnotation(AnnotatedSample.class, SampleAnnotation.class));
        assertTrue(SpringUtil.hasAnnotation(AnnotatedSample.class, SampleAnnotation.class));
        assertNotNull(SpringUtil.findAnnotation(method, SampleAnnotation.class));
        assertTrue(SpringUtil.hasMethodAnnotation(method, SampleAnnotation.class));
        assertNotNull(SpringUtil.findMergedAnnotation(AnnotatedSample.class, SampleAnnotation.class));
        assertNotNull(SpringUtil.findMergedAnnotation(method, SampleAnnotation.class));
    }

    @Test
    void shouldReturnNullWhenAnnotationMissing() throws Exception {
        Method method = PlainSample.class.getDeclaredMethod("run");
        assertNull(SpringUtil.findAnnotation(PlainSample.class, SampleAnnotation.class));
        assertNull(SpringUtil.findAnnotation(method, SampleAnnotation.class));
        assertFalse(SpringUtil.hasAnnotation(PlainSample.class, SampleAnnotation.class));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SampleAnnotation {
    }

    @SampleAnnotation
    static class AnnotatedSample {
        @SampleAnnotation
        void run() {
        }
    }

    static class PlainSample {
        void run() {
        }
    }
}
