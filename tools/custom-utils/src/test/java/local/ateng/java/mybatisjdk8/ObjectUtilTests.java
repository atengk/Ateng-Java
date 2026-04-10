package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

public class ObjectUtilTests {

    @Test
    void test() {
        System.out.println(ObjectUtil.equals(null, null));
        System.out.println(ObjectUtil.equals(null, 1));
        System.out.println(ObjectUtil.equals(1, 2));
        System.out.println(ObjectUtil.equals(1, 1));
    }

}
