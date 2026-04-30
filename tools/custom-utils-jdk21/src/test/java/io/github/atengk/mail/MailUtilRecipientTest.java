package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilRecipientTest {

    @Test
    void addRecipientsShouldNormalizeAddress() {
        MailUtil.MailMessage message = TestFixtures.message();
        MailUtil.addCc(message, "CC@Example.com");
        MailUtil.addBcc(message, "BCC@Example.com");
        assertTrue(message.cc.contains("cc@example.com"));
        assertTrue(message.bcc.contains("bcc@example.com"));
    }

    @Test
    void mergeRecipientsShouldDeduplicate() {
        List<String> merged = MailUtil.mergeRecipients(List.of("a@example.com"), List.of("A@example.com", "b@example.com"));
        assertEquals(List.of("a@example.com", "b@example.com"), merged);
    }

    @Test
    void groupRecipientsShouldContainAllGroups() {
        MailUtil.MailMessage message = TestFixtures.message();
        Map<String, List<String>> grouped = MailUtil.groupRecipients(message);
        assertTrue(grouped.containsKey("to"));
        assertEquals(1, grouped.get("to").size());
    }

    @Test
    void validateRecipientsShouldRejectMissingRecipient() {
        MailUtil.MailMessage message = TestFixtures.message();
        MailUtil.clearRecipients(message);
        assertThrows(IllegalArgumentException.class, () -> MailUtil.validateRecipients(message));
    }
}
