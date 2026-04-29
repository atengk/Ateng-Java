package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilTypeTest {

    @Test
    void shouldCheckInstance() {
        assertTrue(ObjectUtil.isInstance("a", String.class));
        assertTrue(ObjectUtil.isAnyInstance("a", Integer.class, String.class));
        assertTrue(ObjectUtil.isAllInstance(new ArrayList<>(), List.class, Iterable.class));
        assertTrue(ObjectUtil.isNotInstance("a", Integer.class));
        assertFalse(ObjectUtil.isInstance("a", null));
    }

    @Test
    void shouldCheckAssignable() {
        assertTrue(ObjectUtil.isAssignable(ArrayList.class, List.class));
        assertFalse(ObjectUtil.isAssignable(List.class, ArrayList.class));
        assertFalse(ObjectUtil.isAssignable(null, List.class));
    }

    @Test
    void shouldCheckBasicAndJdkTypes() {
        assertTrue(ObjectUtil.isBasicType("a"));
        assertTrue(ObjectUtil.isBasicType(BigDecimal.ONE));
        assertTrue(ObjectUtil.isBasicType(LocalDate.now()));
        assertTrue(ObjectUtil.isPrimitiveWrapper(1));
        assertFalse(ObjectUtil.isPrimitiveWrapper("a"));
        assertTrue(ObjectUtil.isJdkType("a"));
        assertFalse(ObjectUtil.isJdkType(new CustomType()));
    }

    static class CustomType {
    }
}
