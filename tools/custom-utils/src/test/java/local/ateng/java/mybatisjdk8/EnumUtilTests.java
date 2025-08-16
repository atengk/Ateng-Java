package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.enums.StatusEnum;
import local.ateng.java.customutils.utils.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class EnumUtilTests {

    @Test
    void test01() {
        Map<Integer, String> map = EnumUtil.toMap(StatusEnum.class, "code", "name");
        System.out.println(map); // {1=启用, 0=禁用}
    }

}
