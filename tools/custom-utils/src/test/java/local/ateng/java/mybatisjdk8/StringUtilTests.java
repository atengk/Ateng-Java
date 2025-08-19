package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.StringUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class StringUtilTests {


    @Test
    void split1() {
        String str1 = "1,  2  ,3,4,,,";
        System.out.println(StringUtil.splitToList(str1,",", true));
        System.out.println(Arrays.asList(StringUtil.split(str1, ",", false)));
    }

}
