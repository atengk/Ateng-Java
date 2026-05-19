package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateException;
import io.github.atengk.utils.validation.ValidateUtil;
import io.github.atengk.utils.validation.model.ValidateResult;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilBeanValidationTest {

    @Test
    void validateShouldReturnEmptyWhenBeanIsValid() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validate(UserForm.valid());

        assertTrue(violations.isEmpty());
        assertTrue(ValidateUtil.isValid(UserForm.valid()));
    }

    @Test
    void validateShouldReturnErrorsWhenBeanIsInvalid() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validate(UserForm.invalid());
        ValidateResult result = ValidateUtil.validateResult(UserForm.invalid());

        assertFalse(violations.isEmpty());
        assertFalse(result.isValid());
        assertTrue(result.getMessages().contains("用户名不能为空"));
        assertTrue(ValidateUtil.validateFirst(UserForm.invalid()).isPresent());
    }

    @Test
    void validateOrThrowShouldThrowWhenBeanIsInvalid() {
        ValidateException exception = assertThrows(ValidateException.class, () -> ValidateUtil.validateOrThrow(UserForm.invalid()));

        assertFalse(exception.getErrors().isEmpty());
        assertTrue(exception.getMessage().contains("不能为空") || exception.getMessage().contains("不正确") || exception.getMessage().contains("不能小于"));
    }

    @Test
    void validateFirstOrThrowShouldThrowWhenBeanIsInvalid() {
        ValidateException exception = assertThrows(ValidateException.class, () -> ValidateUtil.validateFirstOrThrow(UserForm.invalid()));

        assertFalse(exception.getErrors().isEmpty());
        assertTrue(exception.getMessage().contains("不能为空") || exception.getMessage().contains("不正确") || exception.getMessage().contains("不能小于"));
    }

    @Test
    void validateShouldRejectNullBeanAndNullGroup() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validate(null));
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validate(UserForm.valid(), (Class<?>) null));
    }
}
