package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.StringUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class StringUtilTests {


    @Test
    void split1() {
        String str1 = "1,  2  ,3,4,,,";
        System.out.println(StringUtil.splitToList(str1, ",", true));
        System.out.println(Arrays.asList(StringUtil.split(str1, ",", false)));
    }

    @Test
    void buildUrl() {
        String baseUrl = "https://api.example.com/user/{id}/detail";

        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("q", "张三");
        queryParams.put("name", "{name}");
        queryParams.put("tags", new String[]{"java", "spring"});

        Map<String, Object> uriVariables = new LinkedHashMap<>();
        uriVariables.put("id", 1);
        uriVariables.put("name", "阿腾");

        String url1 = StringUtil.buildUrl(baseUrl, queryParams, uriVariables, true);
        System.out.println("encode=true  : " + url1);

        String url2 = StringUtil.buildUrl(baseUrl, queryParams, uriVariables, false);
        System.out.println("encode=false : " + url2);
    }

    @Test
    void repeat() {
        String repeat = StringUtil.repeat("=", 50);
        System.out.println(repeat);
    }

    @Test
    void format() {
        String format = StringUtil.format("你好，我是{}，来自{}，{}、{}", "阿腾", "重庆");
        System.out.println(format);
    }

    @Test
    void testTrimString() {
        System.out.println(StringUtil.trim("##hello##", "##"));
        System.out.println(StringUtil.trim("a,b,c,x,", ","));
        System.out.println(StringUtil.trim("--abc--", "--"));
        System.out.println(StringUtil.trim("  xyz  ", null));
        System.out.println(StringUtil.trim("", "##"));
        System.out.println(StringUtil.trim(null, "--"));
        System.out.println(StringUtil.trim("####", "##"));
        System.out.println(StringUtil.trim("##abc##xyz", "##"));
    }

}
