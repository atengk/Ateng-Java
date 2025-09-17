package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.DesensitizedUtil;
import org.junit.jupiter.api.Test;

public class DesensitizedUtilTests {

    @Test
    void test1() {
        System.out.println(DesensitizedUtil.desensitizeUserId("123456789")); // 12*****89
        System.out.println(DesensitizedUtil.desensitizeChineseName("张三丰")); // 张**
        System.out.println(DesensitizedUtil.desensitizeIdCard("510123199003074321")); // 510***********4321
        System.out.println(DesensitizedUtil.desensitizeFixedPhone("02888889999")); // 0288*****99
        System.out.println(DesensitizedUtil.desensitizeMobile("13812345678")); // 138****5678
    }

    @Test
    void test2() {
        System.out.println(DesensitizedUtil.desensitizeAddress("重庆市渝北区龙塔街道XX小区123号"));
        // 重庆市渝北区**************

        System.out.println(DesensitizedUtil.desensitizeEmail("zhangsan@example.com"));
        // z******n@example.com

        System.out.println(DesensitizedUtil.desensitizePassword("MyPassw0rd!"));
        // **********

        System.out.println(DesensitizedUtil.desensitizeCarNumber("渝A12345"));
        // 渝A***5 (普通车牌)

        System.out.println(DesensitizedUtil.desensitizeCarNumber("渝AD12345"));
        // 渝A****45 (新能源车牌)

        System.out.println(DesensitizedUtil.desensitizeBankCard("6222021234567890123"));
        // 622202******90123
    }

}
