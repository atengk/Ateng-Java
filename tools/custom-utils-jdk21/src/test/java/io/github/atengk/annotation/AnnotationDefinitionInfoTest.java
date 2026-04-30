package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationDefinitionInfoTest {

    @Test
    void shouldReadRetentionTargetAndFlags() {
        assertEquals(RetentionPolicy.RUNTIME, AnnotationUtil.getRetentionPolicy(Marker.class).orElseThrow());
        assertEquals(RetentionPolicy.CLASS, AnnotationUtil.getRetentionPolicy(ClassRetentionFlag.class).orElseThrow());
        assertTrue(AnnotationUtil.getElementTypes(Marker.class).contains(ElementType.TYPE));
        assertTrue(AnnotationUtil.hasTarget(Marker.class, ElementType.METHOD));
        assertTrue(AnnotationUtil.isRuntimeRetention(Marker.class));
        assertTrue(AnnotationUtil.isInheritedAnnotation(InheritedFlag.class));
        assertTrue(AnnotationUtil.isRepeatableAnnotation(Tag.class));
        assertEquals(Tags.class, AnnotationUtil.getRepeatableContainerType(Tag.class).orElseThrow());
    }

    @Test
    void shouldReturnAllTargetsWhenTargetIsMissing() {
        Set<ElementType> elementTypes = AnnotationUtil.getElementTypes(NoTargetFlag.class);
        assertTrue(elementTypes.contains(ElementType.TYPE));
        assertTrue(elementTypes.contains(ElementType.METHOD));
    }

    @Test
    void shouldHandleInvalidDefinitionArguments() {
        assertTrue(AnnotationUtil.getRetentionPolicy(null).isEmpty());
        assertTrue(AnnotationUtil.getElementTypes(null).isEmpty());
        assertFalse(AnnotationUtil.hasTarget(Marker.class, null));
        assertFalse(AnnotationUtil.isRuntimeRetention(null));
        assertFalse(AnnotationUtil.isInheritedAnnotation(null));
        assertFalse(AnnotationUtil.isRepeatableAnnotation(null));
        assertTrue(AnnotationUtil.getRepeatableContainerType(null).isEmpty());
    }
}
