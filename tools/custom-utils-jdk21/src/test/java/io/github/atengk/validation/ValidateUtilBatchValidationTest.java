package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateException;
import io.github.atengk.utils.validation.ValidateUtil;
import io.github.atengk.utils.validation.model.BatchValidateResult;
import io.github.atengk.utils.validation.model.ValidateResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilBatchValidationTest {

    @Test
    void validateEachShouldReturnResultForEveryBean() {
        List<ValidateResult> results = ValidateUtil.validateEach(List.of(UserForm.valid(), UserForm.invalid()));

        assertEquals(2, results.size());
        assertTrue(results.get(0).isValid());
        assertFalse(results.get(1).isValid());
    }

    @Test
    void validateAllShouldCollectRowIndexErrors() {
        BatchValidateResult result = ValidateUtil.validateAll(List.of(UserForm.valid(), UserForm.invalid()));

        assertFalse(result.isValid());
        assertEquals(2, result.getTotal());
        assertTrue(result.getErrorCount() > 0);
        assertEquals(1, result.getErrors().get(0).getIndex());
        assertEquals(2, result.getErrors().get(0).getRowNumber());
    }

    @Test
    void validateEachFirstShouldOnlyKeepFirstErrorForEachInvalidBean() {
        List<ValidateResult> results = ValidateUtil.validateEachFirst(List.of(UserForm.invalid()));

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getErrors().size());
    }

    @Test
    void batchValidationShouldHandleEmptyAndRejectNull() {
        assertTrue(ValidateUtil.validateAll(List.of()).isValid());
        assertTrue(ValidateUtil.isAllValid(List.of(UserForm.valid())));
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validateAll(null));
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validateAll(java.util.Arrays.asList(UserForm.valid(), null)));
        assertThrows(ValidateException.class, () -> ValidateUtil.validateAllOrThrow(List.of(UserForm.invalid())));
    }
}
