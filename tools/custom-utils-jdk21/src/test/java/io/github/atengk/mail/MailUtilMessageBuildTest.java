package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilMessageBuildTest {

    @Test
    void buildTextMessageShouldCreateMimeMessage() throws Exception {
        MimeMessage message = MailUtil.buildTextMessage(TestFixtures.session(), "sender@example.com", List.of("receiver@example.com"), "主题", "正文");
        assertEquals("主题", message.getSubject());
        assertEquals(1, message.getRecipients(Message.RecipientType.TO).length);
    }

    @Test
    void buildHtmlMessageShouldCreateHtmlContent() throws Exception {
        MimeMessage message = MailUtil.buildHtmlMessage(TestFixtures.session(), "sender@example.com", List.of("receiver@example.com"), "主题", "<p>正文</p>");
        assertTrue(message.getContentType().toLowerCase().contains("text/html"));
    }

    @Test
    void copyMessageShouldCreateIndependentMimeMessage() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        MimeMessage copy = MailUtil.copyMessage(message);
        assertNotSame(message, copy);
        assertEquals(message.getSubject(), copy.getSubject());
    }

    @Test
    void buildMimeMessageShouldRejectMissingRecipient() {
        MailUtil.MailMessage message = TestFixtures.message();
        message.to.clear();
        assertThrows(IllegalArgumentException.class, () -> MailUtil.buildMimeMessage(TestFixtures.session(), message));
    }
}
