package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilTemplateTest {

    @TempDir
    Path tempDir;

    @Test
    void renderTemplateShouldReplacePlaceholders() {
        String result = MailUtil.renderTemplate("你好 ${name}，编号 {{ code }}", Map.of("name", "张三", "code", "A001"));
        assertEquals("你好 张三，编号 A001", result);
    }

    @Test
    void extractPlaceholdersShouldReturnNames() {
        Set<String> names = MailUtil.extractPlaceholders("${name}-{{code}}");
        assertEquals(Set.of("name", "code"), names);
    }

    @Test
    void validateTemplateVariablesShouldRejectMissingVariable() {
        assertThrows(IllegalArgumentException.class, () -> MailUtil.validateTemplateVariables("${name}", Map.of()));
    }

    @Test
    void loadTemplateShouldReadFile() throws Exception {
        Path file = tempDir.resolve("template.html");
        Files.writeString(file, "${name}");
        assertEquals("${name}", MailUtil.loadTemplate(file));
    }

    @Test
    void buildTemplateMessageShouldSetRenderedContent() {
        MailUtil.MailTemplate template = new MailUtil.MailTemplate();
        template.subjectTemplate = "通知 ${name}";
        template.contentTemplate = "你好 ${name}";
        template.variables = Map.of("name", "张三");
        MailUtil.MailMessage message = MailUtil.buildTemplateMessage("sender@example.com", java.util.List.of("receiver@example.com"), template);
        assertEquals("通知 张三", message.subject);
        assertEquals("你好 张三", message.content);
    }
}
