package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilContentReadTest {

    @Test
    void readBytesStringAndLines() {
        Resource resource = ResourceUtil.classpath("sample.txt");
        assertTrue(ResourceUtil.readBytes(resource).length > 0);
        assertTrue(ResourceUtil.readString(resource).contains("hello"));
        assertEquals(List.of("hello", "world"), ResourceUtil.readLines(resource));
        assertEquals("hello\nworld\n", ResourceUtil.readUtf8String(resource));
    }

    @Test
    void readPropertiesAndXml() {
        Properties properties = ResourceUtil.readProperties(ResourceUtil.classpath("config/app.properties"));
        assertEquals("resource-util", properties.getProperty("app.name"));
        Document document = ResourceUtil.readXmlDocument(ResourceUtil.classpath("config/app.xml"));
        assertEquals("app", document.getDocumentElement().getTagName());
    }

    @Test
    void readJsonYamlXmlWithConverter() {
        String json = ResourceUtil.readJson(ResourceUtil.classpath("config/app.json"), text -> text);
        String yaml = ResourceUtil.readYaml(ResourceUtil.classpath("config/app.yml"), text -> text);
        String xml = ResourceUtil.readXml(ResourceUtil.classpath("config/app.xml"), text -> text);
        assertTrue(json.contains("resource-util"));
        assertTrue(yaml.contains("enabled"));
        assertTrue(xml.contains("<app>"));
    }

    @Test
    void readWithAndConsume() {
        Resource resource = ResourceUtil.string("abc", StandardCharsets.UTF_8);
        int size = ResourceUtil.readWith(resource, inputStream -> {
            try {
                return inputStream.readAllBytes().length;
            } catch (Exception ex) {
                throw new UncheckedIOException((java.io.IOException) ex);
            }
        });
        AtomicInteger consumed = new AtomicInteger();
        ResourceUtil.consume(resource, inputStream -> {
            try {
                consumed.set(inputStream.readAllBytes().length);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
        assertEquals(3, size);
        assertEquals(3, consumed.get());
    }

    @Test
    void rejectUnreadableRead() {
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.readBytes(ResourceUtil.classpath("missing.txt")));
    }
}
