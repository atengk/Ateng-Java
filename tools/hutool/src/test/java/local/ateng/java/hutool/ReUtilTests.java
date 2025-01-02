package local.ateng.java.hutool;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 正则工具
 * https://www.hutool.cn/docs/#/core/%E5%B7%A5%E5%85%B7%E7%B1%BB/%E6%AD%A3%E5%88%99%E5%B7%A5%E5%85%B7-ReUtil
 *
 * @author 孔余
 * @since 2024-01-17 19:32
 */
public class ReUtilTests {
    // 抽取多个分组然后把它们拼接起来
    @Test
    void test01() {
        String content = "ZZZaaabbbccc中文1234";
        String resultExtractMulti = ReUtil.extractMulti("(\\w)aa(\\w)", content, "$1-$2");
        System.out.println(resultExtractMulti);
    }

    // 删除第一个匹配到的内容
    @Test
    void test02() {
        String content = "ZZZaaabbbccc中文1234";
        String resultDelFirst = ReUtil.delFirst("(\\w)aa(\\w)", content);
        System.out.println(resultDelFirst);
    }

    // 查找所有匹配文本
    @Test
    void test03() {
        String content = "ZZZaaabbbccc中文1234";
        List<String> resultFindAll = ReUtil.findAll("\\w{2}", content, 0, new ArrayList<String>());
        // 结果：["ZZ", "Za", "aa", "bb", "bc", "cc", "12", "34"]
        System.out.println(resultFindAll);
    }

    // 找到匹配的第一个数字
    @Test
    void test04() {
        String content = "ZZZaaabbbccc中文1234";
        Integer resultGetFirstNumber = ReUtil.getFirstNumber(content);
        // 结果：1234
        System.out.println(resultGetFirstNumber);
    }

    // 给定字符串是否匹配给定正则
    @Test
    void test05() {
        String content = "ZZZaaabbbccc中文1234";
        boolean isMatch = ReUtil.isMatch("\\w+[\u4E00-\u9FFF]+\\d+", content);
        // 结果：1234
        System.out.println(isMatch);
    }

    @Test
    void test05_2() {
        String content = "ZZZaaabbbccc中文1234";
        boolean isMatch = StrUtil.contains(content, "中文");
        System.out.println(isMatch);
    }

    // 通过正则查找到字符串，然后把匹配到的字符串加入到replacementTemplate中，$1表示分组1的字符串
    @Test
    void test06() {
        String content = "ZZZaaabbbccc中文1234xx8765";
        //此处把1234替换为 ->1234<-
        String replaceAll = ReUtil.replaceAll(content, "(\\d+)", "->$1<-");        // 结果：1234
        System.out.println(replaceAll);
    }

    // 转义给定字符串，为正则相关的特殊符号转义
    @Test
    void test07() {
        String escape = ReUtil.escape("我有个$符号{}\\");
        // 结果：我有个\\$符号\\{\\}
        System.out.println(escape);
    }

}
