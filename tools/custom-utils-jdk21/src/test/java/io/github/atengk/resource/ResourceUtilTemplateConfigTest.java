package io.github.atengk.resource;

import io.github.atengk.utils.ResourceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilTemplateConfigTest {

    @Test
    void readTemplateAndRender() {
        String template = ResourceUtil.readTemplate("classpath:templates/email.txt", StandardCharsets.UTF_8);
        String rendered = ResourceUtil.renderTemplate(ResourceUtil.classpath("templates/email.txt"), Map.of("name", "Ateng", "code", "OK"));
        assertTrue(template.contains("${name}"));
        assertTrue(rendered.contains("Ateng"));
        assertTrue(rendered.contains("OK"));
    }

    @Test
    void readConfigFiles() {
        String json = ResourceUtil.readJsonConfig("classpath:config/app.json", text -> text);
        String yaml = ResourceUtil.readYamlConfig("classpath:config/app.yml", text -> text);
        Properties properties = ResourceUtil.readPropertiesConfig("classpath:config/app.properties");
        String sql = ResourceUtil.readSql("classpath:sql/init.sql");
        String xml = ResourceUtil.readXml("classpath:config/app.xml");
        assertTrue(json.contains("resource-util"));
        assertTrue(yaml.contains("enabled"));
        assertEquals("resource-util", properties.getProperty("app.name"));
        assertTrue(sql.contains("select"));
        assertTrue(xml.contains("<app>"));
    }

    @Test
    void loadTemplatesAndConfigs() {
        List<Resource> templates = ResourceUtil.loadTemplates("classpath*:templates/*.txt");
        List<Resource> configs = ResourceUtil.loadConfigs("classpath*:config/*.*");
        assertFalse(templates.isEmpty());
        assertTrue(configs.size() >= 4);
    }

    @Test
    void rejectMissingTemplate() {
        assertThrows(ResourceUtil.ResourceOperationException.class, () -> ResourceUtil.readTemplate("classpath:templates/missing.txt"));
    }
}
