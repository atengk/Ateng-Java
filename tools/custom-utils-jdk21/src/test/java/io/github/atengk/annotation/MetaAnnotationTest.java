package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class MetaAnnotationTest {

    @Test
    void shouldFindMetaAnnotation() {
        assertTrue(AnnotationUtil.hasMetaAnnotation(Composed.class, MetaFlag.class));
        assertTrue(AnnotationUtil.findMetaAnnotation(Composed.class, MetaFlag.class).isPresent());
        assertEquals("composed-meta", AnnotationUtil.findMetaAnnotation(Composed.class, MetaFlag.class).orElseThrow().value());
    }

    @Test
    void shouldFindMergedAnnotationAndAnnotationByMeta() {
        assertTrue(AnnotationUtil.findMergedAnnotation(ChildSample.class, MetaFlag.class).isPresent());
        assertTrue(AnnotationUtil.findAnnotationByMeta(ChildSample.class, MetaFlag.class).isPresent());
        Annotation annotation = AnnotationUtil.findAnnotationByMeta(ChildSample.class, MetaFlag.class).orElseThrow();
        assertEquals(Composed.class, annotation.annotationType());
    }

    @Test
    void shouldResolveMetaAnnotationsAndFilterByMeta() {
        Set<Annotation> metaAnnotations = AnnotationUtil.resolveMetaAnnotations(DeepComposed.class);
        List<Annotation> annotations = AnnotationUtil.getAnnotationsByMeta(ChildSample.class, MetaFlag.class);
        assertTrue(metaAnnotations.stream().anyMatch(annotation -> annotation.annotationType().equals(MiddleMeta.class)));
        assertEquals(1, annotations.size());
        assertTrue(AnnotationUtil.isMetaAnnotation(ChildSample.class.getAnnotation(Composed.class), MetaFlag.class));
    }

    @Test
    void shouldHandleInvalidMetaArguments() {
        assertFalse(AnnotationUtil.hasMetaAnnotation(null, MetaFlag.class));
        assertTrue(AnnotationUtil.findMetaAnnotation(null, MetaFlag.class).isEmpty());
        assertTrue(AnnotationUtil.findMergedAnnotation(null, MetaFlag.class).isEmpty());
        assertTrue(AnnotationUtil.findAnnotationByMeta(null, MetaFlag.class).isEmpty());
        assertTrue(AnnotationUtil.getMetaAnnotations(null).isEmpty());
        assertTrue(AnnotationUtil.resolveMetaAnnotations(null).isEmpty());
        assertTrue(AnnotationUtil.getAnnotationsByMeta(null, MetaFlag.class).isEmpty());
    }
}
