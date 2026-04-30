package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.Session;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilConfigConnectionTest {

    @Test
    void createSmtpConfigShouldSetProtocolAndProperties() {
        MailUtil.MailConfig config = MailUtil.createSmtpConfig("smtp.example.com", 465, "user", "pwd", true, false);
        assertEquals("smtps", config.protocol);
        assertEquals(465, config.port);

        Properties properties = MailUtil.toProperties(config);
        assertEquals("smtp.example.com", properties.getProperty("mail.smtp.host"));
        assertEquals("true", properties.getProperty("mail.smtp.ssl.enable"));
    }

    @Test
    void buildSessionShouldReturnSession() {
        Session session = MailUtil.buildSession(TestFixtures.smtpConfig());
        assertNotNull(session);
    }

    @Test
    void validateConfigShouldRejectInvalidPort() {
        MailUtil.MailConfig config = MailUtil.createSmtpConfig("localhost", 0, "user", "pwd", false, false);
        assertThrows(IllegalArgumentException.class, () -> MailUtil.validateConfig(config));
    }

    @Test
    void closeQuietlyShouldIgnoreNull() {
        assertDoesNotThrow(() -> MailUtil.closeQuietly((AutoCloseable) null));
    }
}
