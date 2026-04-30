package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilSecurityValidationTest {

    @Test
    void validateMessageShouldAcceptNormalMessage() {
        assertDoesNotThrow(() -> MailUtil.validateMessage(TestFixtures.message()));
    }

    @Test
    void validateMessageShouldRejectInvalidSender() {
        MailUtil.MailMessage message = TestFixtures.message();
        message.from = "bad";
        assertThrows(IllegalArgumentException.class, () -> MailUtil.validateMessage(message));
    }

    @Test
    void domainCheckShouldWork() {
        assertTrue(MailUtil.isAllowedDomain("a@example.com", List.of("example.com")));
        assertTrue(MailUtil.isBlockedDomain("a@example.com", List.of("example.com")));
    }

    @Test
    void sensitiveWordAndMaskShouldWork() {
        assertTrue(MailUtil.containsSensitiveWords("包含密码", List.of("密码")));
        assertEquals("u***r@example.com", MailUtil.maskAddress("user@example.com"));
        assertEquals("联系 ***@***", MailUtil.maskContent("联系 user@example.com"));
    }

    @Test
    void validateRecipientLimitShouldRejectOverflow() {
        assertThrows(IllegalArgumentException.class, () -> MailUtil.validateRecipientLimit(List.of("a@example.com", "b@example.com"), 1));
    }
}
