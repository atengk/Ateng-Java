package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessNoDesensitizedTest {

    @Test
    void shouldMaskBusinessNo() {
        assertEquals("ORD2*******0001", DesensitizedUtil.businessNo("ORD202604300001"));
        assertEquals("TRA2*******0001", DesensitizedUtil.tradeNo("TRA202604300001"));
        assertEquals("SER2*******0001", DesensitizedUtil.serialNo("SER202604300001"));
        assertEquals("CON2*******0001", DesensitizedUtil.contractNo("CON202604300001"));
        assertEquals("TIC2*******0001", DesensitizedUtil.ticketNo("TIC202604300001"));
        assertEquals("CAS2*******0001", DesensitizedUtil.caseNo("CAS202604300001"));
        assertEquals("CUS2*******0001", DesensitizedUtil.customerNo("CUS202604300001"));
        assertEquals("MEM2*******0001", DesensitizedUtil.memberNo("MEM202604300001"));
        assertEquals("EMP2*******0001", DesensitizedUtil.employeeNo("EMP202604300001"));
        assertEquals("DEV2*******0001", DesensitizedUtil.deviceNo("DEV202604300001"));
        assertEquals("LIC2*******0001", DesensitizedUtil.licenseNo("LIC202604300001"));
    }

    @Test
    void shouldHandleBoundaryBusinessNoValue() {
        assertNull(DesensitizedUtil.businessNo(null));
        assertEquals("", DesensitizedUtil.businessNo(""));
        assertEquals("ABCD", DesensitizedUtil.businessNo("ABCD"));
    }
}
