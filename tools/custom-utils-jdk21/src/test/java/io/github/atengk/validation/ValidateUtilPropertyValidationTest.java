package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateException;
import io.github.atengk.utils.validation.ValidateUtil;
import io.github.atengk.utils.validation.model.ValidateResult;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilPropertyValidationTest {

    @Test
    void validatePropertyShouldPassWhenPropertyIsValid() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validateProperty(UserForm.valid(), "email");

        assertTrue(violations.isEmpty());
        assertTrue(ValidateUtil.isPropertyValid(UserForm.valid(), "email"));
    }

    @Test
    void validatePropertyShouldReturnErrorsWhenPropertyIsInvalid() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validateProperty(UserForm.invalid(), "email");
        ValidateResult result = ValidateUtil.validatePropertyResult(UserForm.invalid(), "email");

        assertFalse(violations.isEmpty());
        assertFalse(result.isValid());
        assertTrue(ValidateUtil.validatePropertyFirst(UserForm.invalid(), "email").isPresent());
    }

    @Test
    void validatePropertyOrThrowShouldThrowWhenPropertyIsInvalid() {
        assertThrows(ValidateException.class, () -> ValidateUtil.validatePropertyOrThrow(UserForm.invalid(), "username"));
    }

    @Test
    void validatePropertyShouldRejectBlankPropertyName() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validateProperty(UserForm.valid(), " "));
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validateProperty(null, "username"));
    }
}
