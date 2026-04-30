package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilLoadingTest {

    @Test
    void loadRequiredClasspathResource() {
        Resource resource = ResourceUtil.loadRequired("classpath:sample.txt");
        assertEquals("sample.txt", resource.getFilename());
    }

    @Test
    void loadAllByPattern() {
        List<Resource> resources = ResourceUtil.loadAll("classpath*:scan/**/*.txt");
        assertTrue(resources.size() >= 2);
        assertTrue(resources.stream().allMatch(ResourceUtil::isReadable));
    }

    @Test
    void loadFirstReadableResource() {
        assertTrue(ResourceUtil.loadFirst("classpath*:scan/**/*.txt").isPresent());
    }

    @Test
    void resolveRelativeResource() {
        Resource base = ResourceUtil.classpath("scan/a.txt");
        Resource relative = ResourceUtil.resolveRelative(base, "nested/c.txt");
        assertEquals("c.txt", relative.getFilename());
        assertTrue(ResourceUtil.isReadable(relative));
    }

    @Test
    void rejectMissingRequiredResource() {
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.loadRequired("classpath:not-exists.txt"));
    }
}
