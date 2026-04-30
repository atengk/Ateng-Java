package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FinancePaymentDesensitizedTest {

    @Test
    void shouldMaskFinanceAndPaymentInfo() {
        assertEquals("6222***********2020", DesensitizedUtil.bankCard("6222020202020202020"));
        assertEquals("6222********2020", DesensitizedUtil.creditCard("6222020202022020"));
        assertEquals("6222********2020", DesensitizedUtil.debitCard("6222020202022020"));
        assertEquals("***", DesensitizedUtil.cvv("123"));
        assertEquals("6222***********2020", DesensitizedUtil.bankAccount("6222020202020202020"));
        assertEquals("t**t@example.com", DesensitizedUtil.payAccount("test@example.com"));
        assertEquals("138****5678", DesensitizedUtil.alipayAccount("13812345678"));
        assertEquals("138****5678", DesensitizedUtil.wechatPayAccount("13812345678"));
        assertEquals("TRA0********0001", DesensitizedUtil.transactionNo("TRA0202604300001"));
        assertEquals("ORD2*******0001", DesensitizedUtil.orderNo("ORD202604300001"));
        assertEquals("INV2*******0001", DesensitizedUtil.invoiceNo("INV202604300001"));
        assertEquals("9133**********1234", DesensitizedUtil.taxNo("913301001234561234"));
        assertEquals("***", DesensitizedUtil.amount("100.00"));
        assertEquals("***", DesensitizedUtil.balance("9999.00"));
    }

    @Test
    void shouldHandleBoundaryFinanceValue() {
        assertNull(DesensitizedUtil.bankCard(null));
        assertEquals("", DesensitizedUtil.amount(""));
        assertEquals("1234", DesensitizedUtil.bankCard("1234"));
    }
}
