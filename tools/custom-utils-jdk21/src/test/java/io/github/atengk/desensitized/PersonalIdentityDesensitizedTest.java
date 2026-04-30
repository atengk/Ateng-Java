package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonalIdentityDesensitizedTest {

    @Test
    void shouldMaskPersonalIdentity() {
        assertEquals("张*", DesensitizedUtil.chineseName("张三"));
        assertEquals("张*丰", DesensitizedUtil.chineseName("张三丰"));
        assertEquals("A***e", DesensitizedUtil.name("Alice"));
        assertEquals("王*明", DesensitizedUtil.realName("王小明"));
        assertEquals("110***********1234", DesensitizedUtil.idCard("110101199001011234"));
        assertEquals("E1*****78", DesensitizedUtil.passport("E12345678"));
        assertEquals("A1*******90", DesensitizedUtil.driverLicense("A1234567890"));
        assertEquals("J1**45", DesensitizedUtil.officerCard("J12345"));
    }

    @Test
    void shouldMaskBirthAgeAndGender() {
        assertEquals("1990-**-**", DesensitizedUtil.birthDate("1990-01-02"));
        assertEquals("1990/ **/ **".replace(" ", ""), DesensitizedUtil.birthDate("1990/01/02"));
        assertEquals("1990****", DesensitizedUtil.birthDate("19900102"));
        assertEquals("***", DesensitizedUtil.age("36"));
        assertEquals("*", DesensitizedUtil.gender("男"));
    }

    @Test
    void shouldHandleBoundaryIdentityValue() {
        assertNull(DesensitizedUtil.name(null));
        assertEquals("", DesensitizedUtil.chineseName(""));
        assertEquals("***", DesensitizedUtil.chineseName("张"));
        assertEquals("1*3", DesensitizedUtil.idCard("123"));
    }
}
