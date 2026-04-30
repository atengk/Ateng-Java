package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateException;
import io.github.atengk.utils.validation.ValidateUtil;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilExceptionValidationTest {

    @Test
    void throwIfHasErrorsShouldNotThrowWhenEmpty() {
        assertDoesNotThrow(() -> ValidateUtil.throwIfHasErrors(Set.of()));
        assertDoesNotThrow(() -> ValidateUtil.throwIfHasErrors(null));
    }

    @Test
    void throwIfHasErrorsShouldThrowWhenViolationsExist() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validate(UserForm.invalid());

        ValidateException exception = assertThrows(ValidateException.class, () -> ValidateUtil.throwIfHasErrors(violations));

        assertFalse(exception.getErrors().isEmpty());
        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    void buildValidateExceptionShouldSupportMessagePrefix() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validate(UserForm.invalid());

        ValidateException exception = ValidateUtil.buildValidateException("用户参数错误", violations);

        assertTrue(exception.getMessage().startsWith("用户参数错误"));
        assertFalse(exception.getErrors().isEmpty());
        assertThrows(ValidateException.class, () -> ValidateUtil.throwIfHasErrors(violations, "用户参数错误"));
    }

    @Test
    void buildMessageShouldReturnBlankWhenNoViolations() {
        assertEquals("", ValidateUtil.buildMessage(Set.of()));
    }
}
