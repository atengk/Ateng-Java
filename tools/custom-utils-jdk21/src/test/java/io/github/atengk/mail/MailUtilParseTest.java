package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilParseTest {

    @Test
    void parseRawMessageShouldCreateMimeMessage() throws Exception {
        String raw = "From: sender@example.com\r\nTo: receiver@example.com\r\nSubject: Raw\r\n\r\nBody";
        MimeMessage message = MailUtil.parseRawMessage(TestFixtures.session(), new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        assertEquals("Raw", message.getSubject());
    }

    @Test
    void parseHeadersShouldReturnHeaders() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        Map<String, List<String>> headers = MailUtil.parseHeaders(message);
        assertTrue(headers.containsKey("Message-ID"));
    }

    @Test
    void parseRecipientsShouldReturnToList() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        assertEquals(1, MailUtil.parseRecipients(message, Message.RecipientType.TO).size());
    }

    @Test
    void parseMessageShouldReturnReadResult() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        MailUtil.MailReadResult result = MailUtil.parseMessage(message);
        assertEquals("主题", result.subject);
        assertEquals(1, result.to.size());
    }

    @Test
    void parseRawMessageShouldRejectNullInput() {
        assertThrows(NullPointerException.class, () -> MailUtil.parseRawMessage(TestFixtures.session(), null));
    }
}
