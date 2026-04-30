package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilSendTest {

    @Test
    void sendShouldReturnFailureWhenMessageInvalid() {
        MailUtil.MailMessage message = TestFixtures.message();
        message.from = "invalid";
        MailUtil.MailSendResult result = MailUtil.send(TestFixtures.smtpConfig(), message);
        assertFalse(result.success);
        assertNotNull(result.errorMessage);
    }

    @Test
    void sendBatchShouldReturnEmptyForEmptyInput() {
        assertTrue(MailUtil.sendBatch(TestFixtures.smtpConfig(), List.of()).isEmpty());
    }

    @Test
    void sendAsyncShouldReturnFailureForInvalidMessage() throws ExecutionException, InterruptedException {
        MailUtil.MailMessage message = TestFixtures.message();
        message.subject = null;
        MailUtil.MailSendResult result = MailUtil.sendAsync(TestFixtures.smtpConfig(), message).get();
        assertFalse(result.success);
    }

    @Test
    void sendWithRetryShouldRejectNegativeRetryTimes() {
        assertThrows(IllegalArgumentException.class, () -> MailUtil.sendWithRetry(TestFixtures.smtpConfig(), TestFixtures.message(), -1, Duration.ZERO));
    }
}
