package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateUtil;
import io.github.atengk.utils.validation.model.ValidateError;
import io.github.atengk.utils.validation.model.ValidateResult;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilErrorConversionTest {

    @Test
    void toMessagesAndToMessageShouldConvertViolations() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validate(UserForm.invalid());
        List<String> messages = ValidateUtil.toMessages(violations);
        String message = ValidateUtil.toMessage(violations);

        assertFalse(messages.isEmpty());
        assertFalse(message.isBlank());
    }

    @Test
    void toFieldErrorsAndMapShouldConvertViolations() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validate(UserForm.invalid());
        List<ValidateError> errors = ValidateUtil.toFieldErrors(violations);
        Map<String, String> fieldMap = ValidateUtil.toFieldErrorMap(violations);

        assertFalse(errors.isEmpty());
        assertFalse(fieldMap.isEmpty());
        assertTrue(fieldMap.containsKey("username") || fieldMap.containsKey("email") || fieldMap.containsKey("age"));
    }

    @Test
    void toValidateResultShouldHandleEmptyAndNullViolations() {
        ValidateResult validResult = ValidateUtil.toValidateResult(Set.of());

        assertTrue(validResult.isValid());
        assertTrue(ValidateUtil.toMessages(null).isEmpty());
        assertEquals("", ValidateUtil.toMessage(null));
        assertTrue(ValidateUtil.toFieldErrors(null).isEmpty());
        assertTrue(ValidateUtil.toFieldErrorMap(null).isEmpty());
        assertTrue(ValidateUtil.toErrorDetails(null).isEmpty());
        assertTrue(ValidateUtil.getFirstMessage(null).isEmpty());
        assertTrue(ValidateUtil.getFirstField(null).isEmpty());
    }
}
