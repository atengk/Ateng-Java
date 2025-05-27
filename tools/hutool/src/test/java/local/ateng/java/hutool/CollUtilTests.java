package local.ateng.java.hutool;

import cn.hutool.core.collection.CollUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 集合工具-CollUtil
 * https://www.hutool.cn/docs/#/core/%E9%9B%86%E5%90%88%E7%B1%BB/%E9%9B%86%E5%90%88%E5%B7%A5%E5%85%B7-CollUtil
 *
 * @author 孔余
 * @since 2024-02-02 11:48
 */
public class CollUtilTests {
    // 此方法也是来源于Python的一个语法糖，给定两个集合，然后两个集合中的元素一一对应，成为一个Map。此方法还有一个重载方法，可以传字符，然后给定分分隔符，字符串会被split成列表。栗子：
    @Test
    void test01() {
        Collection<String> keys = CollUtil.newArrayList("a", "b", "c", "d");
        Collection<Integer> values = CollUtil.newArrayList(1, 2, 3, 4);

        // {a=1,b=2,c=3,d=4}
        Map<String, Integer> map = CollUtil.zip(keys, values);
        System.out.println(map);
    }

    @Test
    void test02() {
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("c", "d", "e", "f");

        // 交集
        List<String> intersection = (List<String>) CollUtil.intersection(list1, list2);
        System.out.println("交集: " + intersection); // [c, d]
    }

    @Test
    void test03() {
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("c", "d", "e", "f");

        // 并集
        List<String> union = (List<String>) CollUtil.union(list1, list2);
        System.out.println("并集: " + union); // [a, b, c, d, c, d, e, f]

    }

    @Test
    void test04() {
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("c", "d", "e", "f");

        // 差集（list1 - list2）
        List<String> difference = (List<String>) CollUtil.subtract(list1, list2);
        System.out.println("差集（list1 - list2）: " + difference); // [a, b]

    }

}
