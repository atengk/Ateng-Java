package local.ateng.java.hutool;

import cn.hutool.core.collection.CollUtil;
import org.junit.jupiter.api.Test;

import java.util.Collection;
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
}
