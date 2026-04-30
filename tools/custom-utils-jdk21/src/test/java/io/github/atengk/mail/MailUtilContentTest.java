package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilContentTest {

    @Test
    void encodeAndDecodeTextShouldRoundTrip() {
        String encoded = MailUtil.encodeText("中文主题");
        assertEquals("中文主题", MailUtil.decodeText(encoded));
    }

    @Test
    void htmlEscapeAndUnescapeShouldRoundTrip() {
        String escaped = MailUtil.escapeHtml("<p>\"A\"</p>");
        assertEquals("&lt;p&gt;&quot;A&quot;&lt;/p&gt;", escaped);
        assertEquals("<p>\"A\"</p>", MailUtil.unescapeHtml(escaped));
    }

    @Test
    void stripHtmlAndPlainTextShouldWork() {
        assertEquals("Hello", MailUtil.toPlainText("<p>Hello</p>"));
        assertTrue(MailUtil.isHtmlContent("<div>Hello</div>"));
    }

    @Test
    void buildMultipartContentShouldIncludeAttachment() throws Exception {
        MailUtil.MailAttachment attachment = MailUtil.buildAttachment("a.txt", "abc".getBytes(), "text/plain");
        MimeMultipart multipart = MailUtil.buildMultipartContent("正文", false, List.of(attachment));
        assertEquals(2, multipart.getCount());
    }

    @Test
    void parseMultipartContentShouldReturnMap() throws Exception {
        MimeMessage message = MailUtil.buildTextMessage(TestFixtures.session(), "sender@example.com", List.of("receiver@example.com"), "主题", "正文");
        Map<String, Object> result = MailUtil.parseMultipartContent(message);
        assertTrue(result.containsKey("text"));
    }
}
