package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilScanFilterTest {

    @Test
    void scanAndFindResources() {
        List<Resource> resources = ResourceUtil.scanClasspath("scan/**/*.*");
        assertTrue(resources.size() >= 3);
        assertTrue(ResourceUtil.findFirst("classpath*:scan/**/*.json").isPresent());
        assertFalse(ResourceUtil.findByExtension("classpath*:scan/**/*.*", "txt").isEmpty());
        assertEquals(1, ResourceUtil.findByFilename("classpath*:scan/**/*.*", "b.json").size());
    }

    @Test
    void filterSortGroupAndMap() {
        List<Resource> resources = ResourceUtil.scanClasspath("scan/**/*.*");
        List<Resource> readable = ResourceUtil.filterReadable(resources);
        List<Resource> exists = ResourceUtil.filterExists(resources);
        List<Resource> sorted = ResourceUtil.sortByFilename(resources);
        Map<String, List<Resource>> grouped = ResourceUtil.groupByExtension(resources);
        Map<String, Resource> mapped = ResourceUtil.mapByFilename(resources);
        assertEquals(resources.size(), readable.size());
        assertEquals(resources.size(), exists.size());
        assertEquals(resources.size(), sorted.size());
        assertTrue(grouped.containsKey("txt"));
        assertTrue(mapped.containsKey("a.txt"));
    }

    @Test
    void rejectInvalidFilterArguments() {
        assertThrows(NullPointerException.class, () -> ResourceUtil.filter(null, ResourceUtil::exists));
        assertThrows(NullPointerException.class, () -> ResourceUtil.filter(List.of(), null));
    }
}
