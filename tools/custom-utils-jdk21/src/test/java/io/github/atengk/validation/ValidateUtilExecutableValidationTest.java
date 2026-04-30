package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateException;
import io.github.atengk.utils.validation.ValidateUtil;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilExecutableValidationTest {

    @Test
    void validateParametersShouldPassWhenArgumentsAreValid() throws Exception {
        ExecutableService service = new ExecutableService();
        Method method = ExecutableService.class.getDeclaredMethod("create", String.class, int.class);

        Set<ConstraintViolation<ExecutableService>> violations = ValidateUtil.validateParameters(service, method, new Object[]{"ateng", 1});

        assertTrue(violations.isEmpty());
        assertTrue(ValidateUtil.isParametersValid(service, method, new Object[]{"ateng", 1}));
    }

    @Test
    void validateParametersShouldReturnErrorsWhenArgumentsAreInvalid() throws Exception {
        ExecutableService service = new ExecutableService();
        Method method = ExecutableService.class.getDeclaredMethod("create", String.class, int.class);

        Set<ConstraintViolation<ExecutableService>> violations = ValidateUtil.validateParameters(service, method, new Object[]{"", 0});

        assertFalse(violations.isEmpty());
        assertThrows(ValidateException.class, () -> ValidateUtil.validateParametersOrThrow(service, method, new Object[]{"", 0}));
    }

    @Test
    void validateReturnValueShouldValidateMethodReturnValue() throws Exception {
        ExecutableService service = new ExecutableService();
        Method method = ExecutableService.class.getDeclaredMethod("getName", boolean.class);

        assertTrue(ValidateUtil.validateReturnValue(service, method, "ateng").isEmpty());
        assertTrue(ValidateUtil.isReturnValueValid(service, method, "ateng"));
        assertFalse(ValidateUtil.validateReturnValue(service, method, "").isEmpty());
        assertThrows(ValidateException.class, () -> ValidateUtil.validateReturnValueOrThrow(service, method, ""));
    }

    @Test
    void executableValidationShouldRejectInvalidArguments() throws Exception {
        ExecutableService service = new ExecutableService();
        Method method = ExecutableService.class.getDeclaredMethod("create", String.class, int.class);

        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validateParameters(null, method, new Object[]{"ateng", 1}));
        assertThrows(NullPointerException.class, () -> ValidateUtil.validateParameters(service, null, new Object[]{}));
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validateParameters(service, method, new Object[]{"ateng"}));
    }
}
