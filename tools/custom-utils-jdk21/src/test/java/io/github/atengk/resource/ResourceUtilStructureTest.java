package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilStructureTest {

    @Test
    void utilityClassCannotBeInstantiated() throws Exception {
        assertTrue(Modifier.isFinal(ResourceUtil.class.getModifiers()));
        Constructor<ResourceUtil> constructor = ResourceUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertThrows(Exception.class, constructor::newInstance);
    }

    @Test
    void nestedValueObjectsExposeState() {
        var response = ResourceUtil.asResponseEntity(ResourceUtil.bytes("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8), "a.txt"));
        var region = ResourceUtil.getRangeRegion(response.getResource(), 0, 1);
        assertNotNull(response.getResource());
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().isEmpty());
        assertEquals(0, region.getPosition());
        assertEquals(1, region.getCount());
    }
}
