package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilReceiveTest {

    @Test
    void readMessageShouldParseMimeMessage() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        MailUtil.MailReadResult result = MailUtil.readMessage(message);
        assertEquals("主题", result.subject);
        assertTrue(result.textContent.contains("正文"));
    }

    @Test
    void readMessagesShouldReturnEmptyForEmptyArray() {
        assertTrue(MailUtil.readMessages(new Message[0]).isEmpty());
    }

    @Test
    void getMessageHeaderShouldReturnMessageId() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("主题", "正文");
        assertNotNull(MailUtil.getMessageHeader(message, "Message-ID"));
    }

    @Test
    void listMessagesShouldRejectClosedOrNullFolder() {
        assertThrows(NullPointerException.class, () -> MailUtil.listMessages(null));
    }

    @Test
    void countMessagesShouldRejectNullFolder() {
        assertThrows(NullPointerException.class, () -> MailUtil.countMessages((Folder) null));
    }
}
