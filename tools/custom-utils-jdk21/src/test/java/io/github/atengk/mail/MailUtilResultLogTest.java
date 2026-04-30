package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilResultLogTest {

    @Test
    void buildSuccessResultShouldSetSuccess() {
        MailUtil.MailSendResult result = MailUtil.buildSuccessResult("id", 10);
        assertTrue(result.success);
        assertEquals("id", result.messageId);
        assertTrue(MailUtil.isSendSuccess(result));
    }

    @Test
    void buildFailureResultShouldCaptureRootCause() {
        RuntimeException exception = new RuntimeException("外层", new IllegalArgumentException("根因"));
        MailUtil.MailSendResult result = MailUtil.buildFailureResult(exception, 5);
        assertFalse(result.success);
        assertEquals("根因", result.rootCauseMessage);
    }

    @Test
    void formatSendLogShouldContainStatus() {
        String log = MailUtil.formatSendLog(MailUtil.buildSuccessResult("id", 1));
        assertTrue(log.contains("成功"));
    }

    @Test
    void formatReceiveLogShouldContainSubject() {
        MailUtil.MailReadResult result = new MailUtil.MailReadResult();
        result.subject = "主题";
        assertTrue(MailUtil.formatReceiveLog(result).contains("主题"));
    }

    @Test
    void calculateCostTimeShouldReturnNonNegative() {
        assertTrue(MailUtil.calculateCostTime(Instant.now()) >= 0);
    }

    @Test
    void buildBatchSendResultShouldDelegateBatchResult() {
        MailUtil.MailBatchSendResult result = MailUtil.buildBatchSendResult(List.of(MailUtil.buildSuccessResult("id", 1)));
        assertEquals(1, result.successCount);
    }
}
