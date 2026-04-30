package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnterpriseOrganizationDesensitizedTest {

    @Test
    void shouldMaskEnterpriseInfo() {
        assertEquals("阿里******公司", DesensitizedUtil.companyName("阿里巴巴科技有限公司"));
        assertEquals("9133**********1234", DesensitizedUtil.unifiedSocialCreditCode("913301001234561234"));
        assertEquals("9133**********1234", DesensitizedUtil.taxpayerNo("913301001234561234"));
        assertEquals("9133**********1234", DesensitizedUtil.businessLicenseNo("913301001234561234"));
        assertEquals("ORG2*******0001", DesensitizedUtil.organizationCode("ORG202604300001"));
        assertEquals("张*丰", DesensitizedUtil.legalPersonName("张三丰"));
        assertEquals("010-******78", DesensitizedUtil.companyPhone("010-12345678"));
        assertEquals("浙江省杭州市***", DesensitizedUtil.companyAddress("浙江省杭州市西湖区文三路100号"));
        assertEquals("中国******支行", DesensitizedUtil.bankName("中国工商银行杭州支行"));
        assertEquals("6222***********2020", DesensitizedUtil.publicBankAccount("6222020202020202020"));
    }

    @Test
    void shouldHandleBoundaryEnterpriseValue() {
        assertNull(DesensitizedUtil.companyName(null));
        assertEquals("阿*", DesensitizedUtil.companyName("阿里"));
        assertEquals("", DesensitizedUtil.legalPersonName(""));
    }
}
