package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateUtil;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilFieldPathValidationTest {

    @Test
    void fieldPathMethodsShouldExtractFieldNameAndPath() {
        Set<ConstraintViolation<OrderForm>> violations = ValidateUtil.validateNested(new OrderForm(new AddressForm(""), List.of(new ItemForm("apple"))));
        ConstraintViolation<OrderForm> violation = violations.iterator().next();

        assertEquals("city", ValidateUtil.getFieldName(violation));
        assertEquals("city", ValidateUtil.getLeafFieldName(violation));
        assertTrue(ValidateUtil.getFieldPath(violation).contains("address.city"));
    }

    @Test
    void fieldPathStringMethodsShouldNormalizeAndParseIndex() {
        String path = " items[12].children[3].name ";

        assertEquals("items[12].children[3].name", ValidateUtil.normalizeFieldPath(path));
        assertTrue(ValidateUtil.isNestedField(path));
        assertEquals("items.children.name", ValidateUtil.removeIndexFromPath(path));
        assertEquals(OptionalInt.of(12), ValidateUtil.extractIndexFromPath(path));
        assertEquals(List.of(12, 3), ValidateUtil.extractIndexesFromPath(path));
    }

    @Test
    void fieldPathMethodsShouldHandleBlankAndFlatPath() {
        assertEquals("", ValidateUtil.normalizeFieldPath(null));
        assertEquals("", ValidateUtil.normalizeFieldPath("   "));
        assertEquals("user.name", ValidateUtil.normalizeFieldPath("..user..name.."));
        assertFalse(ValidateUtil.isNestedField("username"));
        assertTrue(ValidateUtil.extractIndexFromPath("username").isEmpty());
        assertTrue(ValidateUtil.extractIndexesFromPath(null).isEmpty());
    }
}
