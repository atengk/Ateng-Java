package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilReceiptBounceTest {

    @Test
    void buildReceiptHeadersShouldContainStandardHeaders() {
        Map<String, String> headers = MailUtil.buildReceiptHeaders("receipt@example.com");
        assertTrue(headers.containsKey("Disposition-Notification-To"));
        assertTrue(headers.containsKey("Return-Receipt-To"));
    }

    @Test
    void requestReceiptShouldAddHeadersToMessage() {
        MailUtil.MailMessage message = TestFixtures.message();
        MailUtil.requestReadReceipt(message, "receipt@example.com");
        MailUtil.requestDeliveryReceipt(message, "receipt@example.com");
        assertTrue(message.headers.containsKey("Disposition-Notification-To"));
        assertTrue(message.headers.containsKey("Delivery-Notification-To"));
    }

    @Test
    void bounceDetectionShouldRecognizeMailerDaemon() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("Undelivered Mail Returned to Sender", "Reason: user unknown\nfailed@example.com");
        message.setFrom("mailer-daemon@example.com");
        message.saveChanges();
        assertTrue(MailUtil.isBounceMail(message));
        assertFalse(MailUtil.parseBounceAddress(message).isEmpty());
    }

    @Test
    void handleBounceMailShouldReturnSummary() throws Exception {
        MimeMessage message = TestFixtures.mimeMessage("Failure", "Diagnostic-Code: failed\nfailed@example.com");
        message.setFrom("postmaster@example.com");
        message.saveChanges();
        Map<String, Object> summary = MailUtil.handleBounceMail(message);
        assertEquals(true, summary.get("bounce"));
        assertTrue(summary.containsKey("reason"));
    }
}
