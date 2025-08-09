package local.ateng.java.customutils.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数字处理工具类
 *
 * <p>提供常用的数字格式化、类型转换、比较等方法。</p>
 *
 * @author Ateng
 * @since 2025-08-09
 */
public final class NumberUtil {

    /**
     * 禁止实例化工具类
     */
    private NumberUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 判断数字对象是否为null或者值为0
     *
     * @param number 待检查的数字对象，可以是任意继承自Number的类型
     * @return true：数字为null或者数值为0；false：数字不为null且数值不为0
     */
    public static boolean isNullOrZero(Number number) {
        return number == null || number.doubleValue() == 0D;
    }

    /**
     * 判断数字是否为正数（大于0）
     *
     * @param number 待检查的数字对象
     * @return true：数字为正数；false：数字为null或非正数
     */
    public static boolean isPositive(Number number) {
        return number != null && number.doubleValue() > 0D;
    }

    /**
     * 判断数字是否为负数（小于0）
     *
     * @param number 待检查的数字对象
     * @return true：数字为负数；false：数字为null或非负数
     */
    public static boolean isNegative(Number number) {
        return number != null && number.doubleValue() < 0D;
    }

    /**
     * 判断两个数字是否相等（通过doubleValue比较）
     *
     * @param num1 第一个数字
     * @param num2 第二个数字
     * @return true：两个数字都为null或者两个数字数值相等；false：不相等
     */
    public static boolean equals(Number num1, Number num2) {
        if (num1 == null && num2 == null) {
            return true;
        }
        if (num1 == null || num2 == null) {
            return false;
        }
        return Double.compare(num1.doubleValue(), num2.doubleValue()) == 0;
    }

    /**
     * 将字符串转换为整数类型
     *
     * @param str          待转换的字符串
     * @param defaultValue 转换失败时的默认值
     * @return 转换后的整数值，转换失败返回defaultValue
     */
    public static int toInt(String str, int defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 将字符串转换为double类型
     *
     * @param str          待转换的字符串
     * @param defaultValue 转换失败时的默认值
     * @return 转换后的double值，转换失败返回defaultValue
     */
    public static double toDouble(String str, double defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 格式化数字，保留指定小数位数
     *
     * @param number 待格式化的数字
     * @param scale  保留的小数位数（>=0）
     * @return 格式化后的字符串，如果number为null返回空字符串
     */
    public static String formatScale(Number number, int scale) {
        if (number == null) {
            return "";
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale不能为负数");
        }
        BigDecimal bd = new BigDecimal(number.toString());
        bd = bd.setScale(scale, BigDecimal.ROUND_HALF_UP);
        return bd.toPlainString();
    }

    /**
     * 格式化数字为货币格式，带千分位分隔符，默认保留2位小数
     *
     * @param number 待格式化的数字
     * @return 格式化后的字符串，如果number为null返回空字符串
     */
    public static String formatMoney(Number number) {
        if (number == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(number);
    }

    /**
     * 比较两个数字的大小
     *
     * @param num1 第一个数字
     * @param num2 第二个数字
     * @return 返回1表示num1 > num2；返回-1表示num1 < num2；返回0表示相等
     */
    public static int compare(Number num1, Number num2) {
        if (num1 == null && num2 == null) {
            return 0;
        }
        if (num1 == null) {
            return -1;
        }
        if (num2 == null) {
            return 1;
        }
        return Double.compare(num1.doubleValue(), num2.doubleValue());
    }

    /**
     * 安全的加法运算，null视为0
     *
     * @param a 加数1
     * @param b 加数2
     * @return 返回两数之和，若参数为null视为0
     */
    public static BigDecimal add(Number a, Number b) {
        BigDecimal bd1 = toBigDecimal(a);
        BigDecimal bd2 = toBigDecimal(b);
        return bd1.add(bd2);
    }

    /**
     * 安全的减法运算，null视为0
     *
     * @param a 被减数
     * @param b 减数
     * @return 返回a - b结果，参数null视为0
     */
    public static BigDecimal subtract(Number a, Number b) {
        BigDecimal bd1 = toBigDecimal(a);
        BigDecimal bd2 = toBigDecimal(b);
        return bd1.subtract(bd2);
    }

    /**
     * 安全的乘法运算，null视为0
     *
     * @param a 乘数1
     * @param b 乘数2
     * @return 返回两数相乘结果，参数null视为0
     */
    public static BigDecimal multiply(Number a, Number b) {
        BigDecimal bd1 = toBigDecimal(a);
        BigDecimal bd2 = toBigDecimal(b);
        return bd1.multiply(bd2);
    }

    /**
     * 安全的除法运算，null视为0，除数为0时抛出异常
     *
     * @param a     被除数
     * @param b     除数
     * @param scale 保留小数位数，必须>=0
     * @return 返回a / b结果，参数null视为0
     * @throws ArithmeticException 除数为0时抛出异常
     */
    public static BigDecimal divide(Number a, Number b, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("scale不能为负数");
        }
        BigDecimal bd1 = toBigDecimal(a);
        BigDecimal bd2 = toBigDecimal(b);
        if (bd2.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("除数不能为0");
        }
        return bd1.divide(bd2, scale, RoundingMode.HALF_UP);
    }

    /**
     * 向上取整到整数（无小数部分）
     *
     * @param number 待取整数字
     * @return 返回向上取整后的整数，number为null返回0
     */
    public static int ceilToInt(Number number) {
        if (number == null) {
            return 0;
        }
        return (int) Math.ceil(number.doubleValue());
    }

    /**
     * 向下取整到整数（无小数部分）
     *
     * @param number 待取整数字
     * @return 返回向下取整后的整数，number为null返回0
     */
    public static int floorToInt(Number number) {
        if (number == null) {
            return 0;
        }
        return (int) Math.floor(number.doubleValue());
    }

    /**
     * 四舍五入取整到整数
     *
     * @param number 待取整数字
     * @return 返回四舍五入后的整数，number为null返回0
     */
    public static int roundToInt(Number number) {
        if (number == null) {
            return 0;
        }
        return (int) Math.round(number.doubleValue());
    }

    /**
     * 判断整数是否为偶数
     *
     * @param number 待判断的整数
     * @return true：偶数；false：奇数或null
     */
    public static boolean isEven(Integer number) {
        return number != null && number % 2 == 0;
    }

    /**
     * 判断整数是否为奇数
     *
     * @param number 待判断的整数
     * @return true：奇数；false：偶数或null
     */
    public static boolean isOdd(Integer number) {
        return number != null && number % 2 != 0;
    }

    /**
     * 生成指定范围内的随机整数，包含min和max
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机整数[min, max]
     * @throws IllegalArgumentException 当min > max时抛出异常
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min不能大于max");
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 将数字转成百分比字符串，默认保留2位小数
     *
     * @param number 百分比的数字，如0.1234转成"12.34%"
     * @return 百分比字符串，如果number为null返回空字符串
     */
    public static String toPercent(Number number) {
        return toPercent(number, 2);
    }

    /**
     * 将数字转成百分比字符串，保留指定小数位数
     *
     * @param number 百分比的数字
     * @param scale  保留小数位数
     * @return 百分比字符串，number为null返回空字符串
     */
    public static String toPercent(Number number, int scale) {
        if (number == null) {
            return "";
        }
        BigDecimal bd = new BigDecimal(number.toString())
                .multiply(BigDecimal.valueOf(100))
                .setScale(scale, RoundingMode.HALF_UP);
        return bd.toPlainString() + "%";
    }

    /**
     * 将任意Number类型转换成BigDecimal，null转换成0
     *
     * @param number 待转换数字
     * @return 转换后的BigDecimal，不会返回null
     */
    public static BigDecimal toBigDecimal(Number number) {
        if (number == null) {
            return BigDecimal.ZERO;
        }
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
            return BigDecimal.valueOf(number.longValue());
        }
        if (number instanceof Double || number instanceof Float) {
            // 直接用构造函数可能存在精度问题，用toString避免
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }


    /**
     * 返回两个数字中的最大值，null视为最小值
     *
     * @param a 数字a
     * @param b 数字b
     * @return 两数中较大者，如果两者都为null返回null
     */
    public static Number max(Number a, Number b) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.doubleValue() >= b.doubleValue() ? a : b;
    }

    /**
     * 返回两个数字中的最小值，null视为最大值
     *
     * @param a 数字a
     * @param b 数字b
     * @return 两数中较小者，如果两者都为null返回null
     */
    public static Number min(Number a, Number b) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.doubleValue() <= b.doubleValue() ? a : b;
    }

    /**
     * 判断数字是否为整数（不含小数部分）
     *
     * @param number 待判断数字
     * @return true：是整数；false：是小数或null
     */
    public static boolean isInteger(Number number) {
        if (number == null) {
            return false;
        }
        if (number instanceof Integer || number instanceof Long || number instanceof Short || number instanceof Byte) {
            return true;
        }
        if (number instanceof Double || number instanceof Float || number instanceof BigDecimal) {
            double val = number.doubleValue();
            return val == Math.floor(val) && !Double.isInfinite(val);
        }
        try {
            double val = Double.parseDouble(number.toString());
            return val == Math.floor(val) && !Double.isInfinite(val);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断数字是否为小数（含小数部分）
     *
     * @param number 待判断数字
     * @return true：含小数部分；false：整数或null
     */
    public static boolean isDecimal(Number number) {
        return number != null && !isInteger(number);
    }

    /**
     * 判断数字是否在区间范围内（包含边界）
     *
     * @param number 待判断数字
     * @param min    区间最小值
     * @param max    区间最大值
     * @return true：number >= min且number <= max；false：否则
     */
    public static boolean isBetween(Number number, Number min, Number max) {
        if (number == null || min == null || max == null) {
            return false;
        }
        double val = number.doubleValue();
        return val >= min.doubleValue() && val <= max.doubleValue();
    }

    /**
     * 将数字转换为科学计数法字符串表示，默认保留6位小数
     *
     * @param number 待转换数字
     * @return 科学计数法字符串，number为null返回空字符串
     */
    public static String toScientificNotation(Number number) {
        return toScientificNotation(number, 6);
    }

    /**
     * 将数字转换为科学计数法字符串表示，保留指定小数位数
     *
     * @param number 待转换数字
     * @param scale  保留小数位数
     * @return 科学计数法字符串，number为null返回空字符串
     */
    public static String toScientificNotation(Number number, int scale) {
        if (number == null) {
            return "";
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale不能为负数");
        }
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(scale);
        df.setMinimumFractionDigits(scale);
        df.setGroupingUsed(false);
        df.applyPattern("0." + repeat('0', scale) + "E0");
        return df.format(number);
    }

    /**
     * 计算数组数字的和
     *
     * @param numbers 数组，允许null元素，null数组返回0
     * @return 数组数字之和
     */
    public static BigDecimal sum(Number[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Number num : numbers) {
            total = total.add(toBigDecimal(num));
        }
        return total;
    }

    /**
     * 计算数组数字的平均值
     *
     * @param numbers 数组，允许null元素，null或空数组返回0
     * @return 数组数字平均值
     */
    public static BigDecimal average(Number[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = sum(numbers);
        return total.divide(BigDecimal.valueOf(numbers.length), 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算数组数字的最大值，null元素忽略，全部null返回null
     *
     * @param numbers 数组
     * @return 最大值
     */
    public static Number max(Number... numbers) {
        if (numbers == null || numbers.length == 0) {
            return null;
        }
        Number max = null;
        for (Number num : numbers) {
            if (num == null) {
                continue;
            }
            if (max == null || num.doubleValue() > max.doubleValue()) {
                max = num;
            }
        }
        return max;
    }

    /**
     * 计算数组数字的最小值，null元素忽略，全部null返回null
     *
     * @param numbers 数组
     * @return 最小值
     */
    public static Number min(Number... numbers) {
        if (numbers == null || numbers.length == 0) {
            return null;
        }
        Number min = null;
        for (Number num : numbers) {
            if (num == null) {
                continue;
            }
            if (min == null || num.doubleValue() < min.doubleValue()) {
                min = num;
            }
        }
        return min;
    }

    /**
     * 安全转换为Long类型，转换失败返回默认值
     *
     * @param number       待转换数字
     * @param defaultValue 默认值
     * @return 转换后的Long值
     */
    public static Long toLong(Number number, Long defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        try {
            return number.longValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全转换为Float类型，转换失败返回默认值
     *
     * @param number       待转换数字
     * @param defaultValue 默认值
     * @return 转换后的Float值
     */
    public static Float toFloat(Number number, Float defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        try {
            return number.floatValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全转换为Short类型，转换失败返回默认值
     *
     * @param number       待转换数字
     * @param defaultValue 默认值
     * @return 转换后的Short值
     */
    public static Short toShort(Number number, Short defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        try {
            return number.shortValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 安全转换为Byte类型，转换失败返回默认值
     *
     * @param number       待转换数字
     * @param defaultValue 默认值
     * @return 转换后的Byte值
     */
    public static Byte toByte(Number number, Byte defaultValue) {
        if (number == null) {
            return defaultValue;
        }
        try {
            return number.byteValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 内部工具：重复字符n次
     *
     * @param ch    字符
     * @param count 重复次数
     * @return 重复字符串
     */
    private static String repeat(char ch, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }


    /**
     * 高精度大数相加，指定精度和舍入模式
     *
     * @param a           被加数
     * @param b           加数
     * @param mathContext 精度及舍入模式
     * @return 相加结果
     */
    public static BigDecimal preciseAdd(BigDecimal a, BigDecimal b, MathContext mathContext) {
        BigDecimal bd1 = a == null ? BigDecimal.ZERO : a;
        BigDecimal bd2 = b == null ? BigDecimal.ZERO : b;
        return bd1.add(bd2, mathContext);
    }

    /**
     * 判断字符串是否是合法数字（整数、小数、科学计数法）
     *
     * @param str 待校验字符串
     * @return true：合法数字；false：非数字
     */
    public static boolean isValidNumber(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            new BigDecimal(str.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * 精度控制的四舍五入，支持BigDecimal和double
     *
     * @param number       待处理数字
     * @param scale        保留小数位数，>=0
     * @param roundingMode 舍入模式
     * @return 四舍五入结果，number为null返回null
     */
    public static BigDecimal round(Number number, int scale, RoundingMode roundingMode) {
        if (number == null) {
            return null;
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale不能为负数");
        }
        BigDecimal bd = toBigDecimal(number);
        return bd.setScale(scale, roundingMode);
    }

    /**
     * 将十进制数字转换为指定进制字符串（2~36进制）
     *
     * @param number 十进制整数
     * @param radix  目标进制，2~36之间
     * @return 指定进制字符串
     * @throws IllegalArgumentException radix非法时抛出异常
     */
    public static String toRadixString(long number, int radix) {
        if (radix < 2 || radix > 36) {
            throw new IllegalArgumentException("进制范围必须是2~36");
        }
        return Long.toString(number, radix);
    }

    /**
     * 将指定进制字符串转换为十进制数字
     *
     * @param str   进制字符串
     * @param radix 进制范围2~36
     * @return 十进制long数字
     * @throws NumberFormatException 输入格式非法时抛出异常
     */
    public static long fromRadixString(String str, int radix) {
        if (str == null) {
            throw new NumberFormatException("字符串为null");
        }
        if (radix < 2 || radix > 36) {
            throw new IllegalArgumentException("进制范围必须是2~36");
        }
        return Long.parseLong(str, radix);
    }

    /**
     * 生成等差数列
     *
     * @param start 起始值
     * @param diff  公差
     * @param count 数列长度，必须大于0
     * @return 等差数列List<BigDecimal>
     * @throws IllegalArgumentException count <= 0时抛出异常
     */
    public static List<BigDecimal> arithmeticSequence(Number start, Number diff, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count必须大于0");
        }
        BigDecimal s = toBigDecimal(start);
        BigDecimal d = toBigDecimal(diff);
        List<BigDecimal> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(s.add(d.multiply(BigDecimal.valueOf(i))));
        }
        return result;
    }

    /**
     * 生成等比数列
     *
     * @param start 起始值
     * @param ratio 公比
     * @param count 数列长度，必须大于0
     * @return 等比数列List<BigDecimal>
     * @throws IllegalArgumentException count <= 0时抛出异常
     */
    public static List<BigDecimal> geometricSequence(Number start, Number ratio, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count必须大于0");
        }
        BigDecimal s = toBigDecimal(start);
        BigDecimal r = toBigDecimal(ratio);
        List<BigDecimal> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(s.multiply(r.pow(i)));
        }
        return result;
    }

    /**
     * 根据Locale格式化数字（千分位、小数等本地化）
     *
     * @param number 待格式化数字
     * @param locale 地区设置
     * @return 格式化后的字符串，number为null返回空字符串
     */
    public static String formatByLocale(Number number, Locale locale) {
        if (number == null) {
            return "";
        }
        NumberFormat nf = NumberFormat.getInstance(locale);
        return nf.format(number);
    }

    /**
     * 安全转换为Optional<Integer>
     *
     * @param number 待转换数字
     * @return Optional整数，number为null或无法转换返回Optional.empty()
     */
    public static Optional<Integer> toOptionalInt(Number number) {
        if (number == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(number.intValue());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 计算简单数学表达式结果（仅支持 + - * / 括号）
     *
     * <p>注意：调用JavaScript引擎，性能有限，不建议大规模高频使用</p>
     *
     * @param expression 数学表达式字符串
     * @return 计算结果BigDecimal，表达式非法或计算异常返回null
     */
    public static BigDecimal calculateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        try {
            Object result = engine.eval(expression);
            if (result instanceof Number) {
                return new BigDecimal(result.toString());
            }
            return null;
        } catch (ScriptException e) {
            return null;
        }
    }

}

