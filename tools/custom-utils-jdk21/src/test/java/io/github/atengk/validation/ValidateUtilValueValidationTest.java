package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateException;
import io.github.atengk.utils.validation.ValidateUtil;
import io.github.atengk.utils.validation.model.ValidateResult;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilValueValidationTest {

    @Test
    void validateValueShouldPassWhenValueIsValid() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validateValue(UserForm.class, "email", "a@example.com");

        assertTrue(violations.isEmpty());
        assertTrue(ValidateUtil.isValueValid(UserForm.class, "email", "a@example.com"));
    }

    @Test
    void validateValueShouldReturnErrorsWhenValueIsInvalid() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validateValue(UserForm.class, "email", "bad-email");
        ValidateResult result = ValidateUtil.validateValueResult(UserForm.class, "email", "bad-email");

        assertFalse(violations.isEmpty());
        assertFalse(result.isValid());
        assertTrue(ValidateUtil.validateValueFirst(UserForm.class, "email", "bad-email").isPresent());
    }

    @Test
    void validateValueOrThrowShouldThrowWhenValueIsInvalid() {
        assertThrows(ValidateException.class, () -> ValidateUtil.validateValueOrThrow(UserForm.class, "username", ""));
    }

    @Test
    void validateValueShouldRejectInvalidArguments() {
        assertThrows(NullPointerException.class, () -> ValidateUtil.validateValue(null, "email", "a@example.com"));
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validateValue(UserForm.class, " ", "a@example.com"));
    }
}
