package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateUtil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilContextValidationTest {

    @AfterEach
    void resetValidator() {
        ValidateUtil.resetValidator();
    }

    @Test
    void contextMethodsShouldReturnDefaultValidatorObjects() {
        assertNotNull(ValidateUtil.getValidatorFactory());
        assertNotNull(ValidateUtil.getValidator());
        assertNotNull(ValidateUtil.getExecutableValidator());
    }

    @Test
    void setValidatorShouldReplaceCurrentValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            ValidateUtil.setValidator(validator);

            assertSame(validator, ValidateUtil.getValidator());
            assertTrue(ValidateUtil.validateWith(validator, UserForm.valid()).isEmpty());
        }
    }

    @Test
    void contextMethodsShouldRejectNullValidatorAndReset() {
        Validator original = ValidateUtil.getValidator();

        assertThrows(NullPointerException.class, () -> ValidateUtil.setValidator(null));
        ValidateUtil.resetValidator();

        assertSame(original, ValidateUtil.getValidator());
    }
}
