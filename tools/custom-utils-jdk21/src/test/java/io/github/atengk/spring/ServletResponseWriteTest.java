package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServletResponseWriteTest {

    @AfterEach
    void tearDown() {
        ServletTestSupport.clear();
    }

    @Test
    void shouldWriteTextJsonAndBytes() throws Exception {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        SpringUtil.setStatus(201);
        SpringUtil.setHeader("X-A", "1");
        SpringUtil.addHeader("X-A", "2");
        SpringUtil.setContentType("text/plain;charset=UTF-8");
        SpringUtil.setUtf8Encoding();
        SpringUtil.writeText("ok");
        SpringUtil.flush();

        assertEquals(201, pair.response().getStatus());
        assertEquals("1", pair.response().getHeader("X-A"));
        assertEquals("ok", pair.response().getContentAsString(StandardCharsets.UTF_8));

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        SpringUtil.writeJson(Map.of("name", "Ateng", "age", 18));
        assertTrue(pair.response().getContentAsString(StandardCharsets.UTF_8).contains("\"name\":\"Ateng\""));

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        SpringUtil.writeBytes("bin".getBytes(StandardCharsets.UTF_8));
        assertEquals("bin", pair.response().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldWriteDownload() throws Exception {
        ServletTestSupport.ServletPair pair = ServletTestSupport.bind();
        SpringUtil.setDownloadHeader("测试.txt");
        assertNotNull(pair.response().getHeader("Content-Disposition"));

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        SpringUtil.writeDownload("a.txt", "abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("abc", pair.response().getContentAsString(StandardCharsets.UTF_8));

        ServletTestSupport.clear();
        pair = ServletTestSupport.bind();
        SpringUtil.writeDownload("b.txt", new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8)));
        assertEquals("xyz", pair.response().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldRejectInvalidResponseArguments() {
        ServletTestSupport.bind();
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.setHeader("", "1"));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.setContentType(" "));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.setDownloadHeader(""));
    }
}
