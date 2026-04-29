package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExceptionAssertUtilTest {

    @Test
    void shouldSupportCustomExceptionSupplier() {
        assertDoesNotThrow(() -> AssertUtil.isTrue(true, () -> new BusinessException("业务异常")));
        assertThrows(BusinessException.class, () -> AssertUtil.isTrue(false, () -> new BusinessException("业务异常")));
        assertThrows(BusinessException.class, () -> AssertUtil.isFalse(true, () -> new BusinessException("业务异常")));
        assertThrows(BusinessException.class, () -> AssertUtil.valid(false, () -> new BusinessException("业务异常")));
        assertThrows(BusinessException.class, () -> AssertUtil.state(false, () -> new BusinessException("业务异常")));
        assertThrows(BusinessException.class, () -> AssertUtil.fail(() -> new BusinessException("业务异常")));
        assertThrows(BusinessException.class, () -> AssertUtil.failIf(true, () -> new BusinessException("业务异常")));
    }

    @Test
    void shouldSupportBusinessAliasMethods() {
        String value = "abc";

        assertDoesNotThrow(() -> AssertUtil.validBiz(true, () -> new BusinessException("业务条件不满足")));
        assertSame(value, AssertUtil.notNullBiz(value, () -> new BusinessException("对象不能为空")));
        assertSame(value, AssertUtil.notBlankBiz(value, () -> new BusinessException("字符串不能为空白")));

        assertThrows(BusinessException.class, () -> AssertUtil.validBiz(false, () -> new BusinessException("业务条件不满足")));
        assertThrows(BusinessException.class, () -> AssertUtil.failBiz(() -> new BusinessException("业务失败")));
        assertThrows(BusinessException.class, () -> AssertUtil.notNullBiz(null, () -> new BusinessException("对象不能为空")));
        assertThrows(BusinessException.class, () -> AssertUtil.notBlankBiz(" ", () -> new BusinessException("字符串不能为空白")));
    }

    @Test
    void shouldFallbackWhenExceptionSupplierIsNullOrReturnsNull() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.fail((java.util.function.Supplier<RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.fail(() -> null));
    }

    static class BusinessException extends RuntimeException {

        BusinessException(String message) {
            super(message);
        }
    }
}
