package local.ateng.java.hutool;

import cn.hutool.core.util.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数字工具针对数学运算做工具性封装
 * https://www.hutool.cn/docs/#/core/%E5%B7%A5%E5%85%B7%E7%B1%BB/%E6%95%B0%E5%AD%97%E5%B7%A5%E5%85%B7-NumberUtil
 *
 * @author 孔余
 * @since 2024-03-14 20:53
 */
public class NumberUtilTests {
    // 加减乘除
    @Test
    void test01() {
        double num = 10;
        // 加
        BigDecimal add = NumberUtil.add(num, 10, 10);
        System.out.println(add);
        // 减
        BigDecimal sub = NumberUtil.sub(num, 10, 10);
        System.out.println(sub);
        // 乘
        BigDecimal mul = NumberUtil.mul(num, 10, 10);
        System.out.println(mul);
        // 除
        double div = NumberUtil.div(num, 3, 2);
        System.out.println(div);

    }

    // 保留小数
    @Test
    void test02_1() {
        double num = 3.54;
        BigDecimal roundedNum1 = NumberUtil.round(num, 0, RoundingMode.UP);
        System.out.println("向上取整后的结果：" + roundedNum1.intValue());
        BigDecimal roundedNum2 = NumberUtil.round(num, 0, RoundingMode.DOWN);
        System.out.println("向下取整后的结果：" + roundedNum2.intValue());
    }

    @Test
    void test02_1_1() {
        double num = 3.54;
        BigDecimal roundedNum1 = NumberUtil.round(num, 1, RoundingMode.UP);
        System.out.println("向上取整后的结果：" + roundedNum1.doubleValue());
        BigDecimal roundedNum2 = NumberUtil.round(num, 1, RoundingMode.DOWN);
        System.out.println("向下取整后的结果：" + roundedNum2.doubleValue());
    }

    @Test
    void test02_2() {
        double num = 3.54;
        String roundedNum1 = NumberUtil.roundStr(num, 0, RoundingMode.UP);
        System.out.println("向上取整后的结果：" + roundedNum1);
        String roundedNum2 = NumberUtil.roundStr(num, 0, RoundingMode.DOWN);
        System.out.println("向下取整后的结果：" + roundedNum2);
    }

    // 数字格式化
    @Test
    void test03() {
        long c = 299792458;//光速
        String format = NumberUtil.decimalFormat(",###", c);//299,792,458
        System.out.println(format);
        String format1 = NumberUtil.decimalFormat("#.##%", 0.12345);//12.35%
        System.out.println(format1);
    }

    @Test
    void test03_1() {
        String format1 = NumberUtil.decimalFormat("#%", 0.1245);//12%
        System.out.println(format1);
    }

    // 随机数
    @Test
    void test04_1() {
        int[] ints = NumberUtil.generateRandomNumber(1, 100, 10);
        List<Integer> list = Arrays.stream(ints).boxed().collect(Collectors.toList());
        System.out.println(list);
    }

    @Test
    void test04_2() {
        Integer[] integers = NumberUtil.generateBySet(1, 1000, 10);
        System.out.println(Arrays.asList(integers));
    }

    // 整数列表
    @Test
    void test05() {
        int[] range = NumberUtil.range(1, 100, 1);
        List<Integer> list = Arrays.stream(range).boxed().collect(Collectors.toList());
        System.out.println(list);
    }

    /**
     * 处理double精度问题
     *
     */
    @Test
    void test06() {
        double a = 220.0;
        double b = 0.1 + 0.2;
        double c = 1.0 - 0.9;

        System.out.println("220.0 的值: " + a);
        System.out.println("0.1 + 0.2 的值: " + b);  // 预期为 0.3，但可能会看到 0.30000000000000004
        System.out.println("1.0 - 0.9 的值: " + c);  // 预期为 0.1，但可能会看到 0.09999999999999998


//        double count = 330.0;
        double count = 16;
//        double countLast = 100.0;
        double countLast = 5;

        // 使用 NumberUtil.div 进行除法计算，并保留 1 位小数
        double result = NumberUtil.div(count - countLast, countLast, 1) * 100;

        System.out.println("Result: " + result); // 可能出现类似 220.00000000000003 的结果
        System.out.println("Result: " + NumberUtil.round(result, 1)); // 可能出现类似 220.00000000000003 的结果

    }

}