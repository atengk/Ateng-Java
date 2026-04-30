package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateException;
import io.github.atengk.utils.validation.ValidateUtil;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilNestedValidationTest {

    @Test
    void validateCascadeShouldValidateNestedObject() {
        OrderForm order = new OrderForm(new AddressForm(""), List.of(new ItemForm("apple")));

        Set<ConstraintViolation<OrderForm>> violations = ValidateUtil.validateCascade(order);

        assertFalse(violations.isEmpty());
        assertTrue(ValidateUtil.hasNestedError(violations));
        assertTrue(violations.stream().anyMatch(violation -> ValidateUtil.getPropertyPath(violation).contains("address.city")));
    }

    @Test
    void validateNestedShouldValidateNestedCollection() {
        OrderForm order = new OrderForm(new AddressForm("杭州"), List.of(new ItemForm("")));

        Set<ConstraintViolation<OrderForm>> violations = ValidateUtil.validateNested(order);

        assertFalse(violations.isEmpty());
        assertTrue(ValidateUtil.hasNestedError(violations));
        assertTrue(violations.stream().anyMatch(violation -> ValidateUtil.getPropertyPath(violation).contains("items")));
    }

    @Test
    void nestedValidationShouldPassWhenNestedBeanIsValid() {
        OrderForm order = new OrderForm(new AddressForm("杭州"), List.of(new ItemForm("apple")));

        assertTrue(ValidateUtil.validateCascade(order).isEmpty());
        assertFalse(ValidateUtil.hasNestedError(Set.of()));
    }

    @Test
    void nestedValidationOrThrowShouldThrowWhenInvalid() {
        OrderForm order = new OrderForm(new AddressForm(""), List.of(new ItemForm("")));

        assertThrows(ValidateException.class, () -> ValidateUtil.validateNestedOrThrow(order));
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validateCascade(null));
    }
}
