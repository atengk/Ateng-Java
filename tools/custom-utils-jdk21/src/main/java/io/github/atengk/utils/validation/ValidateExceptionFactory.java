package io.github.atengk.utils.validation;

import io.github.atengk.utils.validation.model.ValidateError;

import java.util.List;

@FunctionalInterface
public interface ValidateExceptionFactory {

    RuntimeException create(String message, List<ValidateError> errors);

}
