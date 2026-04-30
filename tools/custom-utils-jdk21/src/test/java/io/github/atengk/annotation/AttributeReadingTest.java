package io.github.atengk.annotation;

import io.github.atengk.utils.annotation.AnnotationUtil;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import static io.github.atengk.annotation.AnnotationTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class AttributeReadingTest {

    @Test
    void shouldReadTypedAttributes() {
        Marker marker = ChildSample.class.getAnnotation(Marker.class);
        assertEquals("child", AnnotationUtil.readStringAttribute(marker, "value").orElseThrow());
        assertTrue(AnnotationUtil.readBooleanAttribute(marker, "enabled").orElseThrow());
        assertEquals(Void.class, AnnotationUtil.readClassAttribute(marker, "type").orElseThrow());
        assertEquals(Level.LOW, AnnotationUtil.readEnumAttribute(marker, "level", Level.class).orElseThrow());
        assertEquals("child", AnnotationUtil.readValue(marker).orElseThrow());
    }

    @Test
    void shouldReadArrayAndAllAttributes() throws Exception {
        Annotation annotation = ChildSample.class.getDeclaredMethod("childMethod", String.class).getAnnotation(Marker.class);
        List<String> tags = AnnotationUtil.readArrayAttribute(annotation, "tags", String.class);
        Map<String, Object> attributes = AnnotationUtil.readAttributes(annotation);
        Map<String, Object> filteredAttributes = AnnotationUtil.readAttributes(annotation, method -> method.getName().equals("value"));
        assertEquals(List.of("method"), tags);
        assertEquals("childMethod", attributes.get("value"));
        assertEquals(1, filteredAttributes.size());
    }

    @Test
    void shouldReadDefaultsAndNonDefaultAttributes() {
        Marker marker = ChildSample.class.getAnnotation(Marker.class);
        assertEquals("default", AnnotationUtil.readDefaultValue(Marker.class, "value").orElseThrow());
        assertTrue(AnnotationUtil.hasDefaultValue(Marker.class, "enabled"));
        assertTrue(AnnotationUtil.isDefaultValue(marker, "enabled"));
        assertTrue(AnnotationUtil.readDefaultAttributes(Marker.class).containsKey("type"));
        assertEquals("child", AnnotationUtil.getNonDefaultAttributes(marker).get("value"));
    }

    @Test
    void shouldHandleInvalidAttributeArguments() {
        Marker marker = ChildSample.class.getAnnotation(Marker.class);
        assertTrue(AnnotationUtil.readAttribute(null, "value").isEmpty());
        assertTrue(AnnotationUtil.readAttribute(marker, "missing").isEmpty());
        assertTrue(AnnotationUtil.readStringAttribute(marker, "enabled").isEmpty());
        assertTrue(AnnotationUtil.readArrayAttribute(marker, "value", String.class).isEmpty());
        assertTrue(AnnotationUtil.readDefaultValue(Marker.class, "missing").isEmpty());
        assertFalse(AnnotationUtil.isDefaultValue(null, "value"));
        assertTrue(AnnotationUtil.getNonDefaultAttributes(null).isEmpty());
    }
}
