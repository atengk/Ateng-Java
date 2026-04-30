package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilShortcutTest {

    @TempDir
    Path tempDir;

    @Test
    void sendSimpleTextShouldReturnFailureForInvalidSenderWithoutNetwork() {
        MailUtil.MailSendResult result = MailUtil.sendSimpleText(TestFixtures.smtpConfig(), "bad", "receiver@example.com", "主题", "正文");
        assertFalse(result.success);
    }

    @Test
    void sendSimpleHtmlShouldReturnFailureForInvalidSenderWithoutNetwork() {
        MailUtil.MailSendResult result = MailUtil.sendSimpleHtml(TestFixtures.smtpConfig(), "bad", "receiver@example.com", "主题", "<p>正文</p>");
        assertFalse(result.success);
    }

    @Test
    void sendSimpleTemplateShouldReturnFailureForInvalidSenderWithoutNetwork() {
        MailUtil.MailTemplate template = new MailUtil.MailTemplate();
        template.subjectTemplate = "主题 ${name}";
        template.contentTemplate = "正文 ${name}";
        template.variables = Map.of("name", "张三");
        MailUtil.MailSendResult result = MailUtil.sendSimpleTemplate(TestFixtures.smtpConfig(), "bad", "receiver@example.com", template);
        assertFalse(result.success);
    }

    @Test
    void sendVerificationCodeShouldRejectBlankCode() {
        assertThrows(IllegalArgumentException.class, () -> MailUtil.sendVerificationCode(TestFixtures.smtpConfig(), "sender@example.com", "receiver@example.com", " "));
    }

    @Test
    void sendFileShouldReturnFailureForInvalidSenderWithoutNetwork() throws Exception {
        Path file = tempDir.resolve("report.txt");
        Files.writeString(file, "report");
        MailUtil.MailSendResult result = MailUtil.sendFile(TestFixtures.smtpConfig(), "bad", "receiver@example.com", "报表", file);
        assertFalse(result.success);
    }
}
