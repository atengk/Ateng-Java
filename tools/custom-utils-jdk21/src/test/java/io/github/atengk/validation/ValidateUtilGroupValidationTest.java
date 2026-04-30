package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateGroups;
import io.github.atengk.utils.validation.ValidateUtil;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilGroupValidationTest {

    @Test
    void createGroupShouldValidateCreateOnlyFields() {
        UserForm form = new UserForm(null, "ateng", "ateng@example.com", 20, "", java.util.List.of("java"));

        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validate(form, ValidateGroups.Create.class);

        assertFalse(violations.isEmpty());
        assertTrue(ValidateUtil.toMessages(violations).contains("创建来源不能为空"));
    }

    @Test
    void updateGroupShouldValidateUpdateOnlyFields() {
        UserForm form = new UserForm(null, "ateng", "ateng@example.com", 20, "", java.util.List.of("java"));

        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validate(form, ValidateGroups.Update.class);

        assertFalse(violations.isEmpty());
        assertTrue(ValidateUtil.toMessages(violations).contains("用户ID不能为空"));
    }

    @Test
    void groupValidationShouldPassWhenGroupFieldsAreValid() {
        assertTrue(ValidateUtil.validate(UserForm.valid(), ValidateGroups.Create.class).isEmpty());
        assertTrue(ValidateUtil.validate(UserForm.valid(), ValidateGroups.Update.class).isEmpty());
    }

    @Test
    void groupValidationShouldRejectNullGroup() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtil.validate(UserForm.valid(), ValidateGroups.Create.class, null));
    }
}
