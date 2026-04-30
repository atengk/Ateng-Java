package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailUtilBatchTest {

    @Test
    void splitBatchShouldSplitBySize() {
        List<List<Integer>> batches = MailUtil.splitBatch(List.of(1, 2, 3, 4, 5), 2);
        assertEquals(3, batches.size());
        assertEquals(List.of(5), batches.get(2));
    }

    @Test
    void splitBatchShouldRejectInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> MailUtil.splitBatch(List.of(1), 0));
    }

    @Test
    void calculateBatchCountShouldReturnExpectedValue() {
        assertEquals(3, MailUtil.calculateBatchCount(5, 2));
    }

    @Test
    void buildBatchResultShouldCountSuccessAndFailure() {
        MailUtil.MailSendResult success = MailUtil.buildSuccessResult("id", 1);
        MailUtil.MailSendResult failure = MailUtil.buildFailureResult(new RuntimeException("失败"), 2);
        MailUtil.MailBatchSendResult result = MailUtil.buildBatchResult(List.of(success, failure));
        assertEquals(2, result.total);
        assertEquals(1, result.successCount);
        assertEquals(1, result.failureCount);
    }

    @Test
    void filterResultsShouldSplitByStatus() {
        MailUtil.MailSendResult success = MailUtil.buildSuccessResult("id", 1);
        MailUtil.MailSendResult failure = MailUtil.buildFailureResult(new RuntimeException("失败"), 2);
        assertEquals(1, MailUtil.filterSuccessResults(List.of(success, failure)).size());
        assertEquals(1, MailUtil.filterFailedResults(List.of(success, failure)).size());
    }
}
