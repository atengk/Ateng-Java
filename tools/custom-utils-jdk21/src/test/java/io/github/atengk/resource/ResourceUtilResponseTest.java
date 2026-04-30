package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilResponseTest {

    @Test
    void createDownloadAndInlineResponse() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        ResourceUtil.ResourceHttpResponse download = ResourceUtil.asDownload(resource);
        ResourceUtil.ResourceHttpResponse inline = ResourceUtil.asInline(resource);
        assertEquals(200, download.getStatus());
        assertTrue(download.getHeaders().get("Content-Disposition").startsWith("attachment"));
        assertTrue(inline.getHeaders().get("Content-Disposition").startsWith("inline"));
    }

    @Test
    void createHeadersAndContentDisposition() {
        Map<String, String> attachment = ResourceUtil.asAttachmentHeaders("中文.txt");
        Map<String, String> inline = ResourceUtil.asInlineHeaders("a.txt");
        String disposition = ResourceUtil.getContentDisposition("a.txt");
        assertTrue(attachment.get("Content-Disposition").contains("filename*="));
        assertTrue(inline.get("Content-Disposition").startsWith("inline"));
        assertTrue(disposition.startsWith("attachment"));
    }

    @Test
    void createRangeRegion() {
        Resource resource = ResourceUtil.bytes("abcdef".getBytes(StandardCharsets.UTF_8), "a.txt");
        ResourceUtil.ResourceRegion region = ResourceUtil.getRangeRegion(resource, 2, 10);
        assertEquals(2, region.getPosition());
        assertEquals(4, region.getCount());
        assertTrue(ResourceUtil.supportsRange(resource));
    }

    @Test
    void rejectInvalidRange() {
        Resource resource = ResourceUtil.bytes("abc".getBytes(StandardCharsets.UTF_8), "a.txt");
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.getRangeRegion(resource, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.getRangeRegion(resource, 5, 1));
    }
}
