package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateUtil;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilMessageValidationTest {

    @Test
    void messageMethodsShouldReadMessageAndTemplate() {
        Set<ConstraintViolation<UserForm>> violations = ValidateUtil.validateProperty(UserForm.invalid(), "username");
        ConstraintViolation<UserForm> violation = violations.iterator().next();

        assertEquals("用户名不能为空", ValidateUtil.getMessage(violation));
        assertFalse(ValidateUtil.getMessageTemplate(violation).isBlank());
        assertTrue(ValidateUtil.formatMessage(violation).contains("用户名不能为空"));
        assertEquals("用户名不能为空", ValidateUtil.resolveMessage(violation));
    }

    @Test
    void joinMessagesShouldFilterBlankMessagesAndUseDelimiter() {
        String message = ValidateUtil.joinMessages(List.of(" 用户名不能为空 ", "", "邮箱不能为空"), " | ");

        assertEquals("用户名不能为空 | 邮箱不能为空", message);
    }

    @Test
    void messageMethodsShouldHandleNullAndEmptyValues() {
        assertEquals("", ValidateUtil.getMessage(null));
        assertEquals("", ValidateUtil.getMessageTemplate(null));
        assertEquals("", ValidateUtil.formatMessage(null));
        assertEquals("", ValidateUtil.resolveMessage(null));
        assertEquals("", ValidateUtil.joinMessages(null));
        assertEquals("", ValidateUtil.joinMessages(List.of("", " ")));
    }
}
