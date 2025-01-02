package local.ateng.java.hutool;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 类型转换工具类-Convert
 * https://doc.hutool.cn/pages/Convert
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2024-09-13
 */
public class ConvertTests {

    @Test
    void toStr() {
        int a = 1;
        //aStr为"1"
        String aStr = Convert.toStr(a);
        System.out.println(aStr);

        long[] b = {1, 2, 3, 4, 5};
        //bStr为："[1, 2, 3, 4, 5]"
        String bStr = Convert.toStr(b);
        System.out.println(bStr);

    }

    @Test
    void toIntArray() {
        String[] b = { "1", "2", "3", "4" };
        //结果为Integer数组
        Integer[] intArray = Convert.toIntArray(b);
        System.out.println(Convert.toStr(intArray));

        long[] c = {1,2,3,4,5};
        //结果为Integer数组
        Integer[] intArray2 = Convert.toIntArray(c);
        System.out.println(Convert.toStr(intArray2));
    }

    @Test
    void toDate() {
        String a = "2017-05-06";
        Date value = Convert.toDate(a);
        System.out.println(value);
    }

    @Test
    void toList() {
        Object[] a = {"a", "你", "好", "", 1};
        List<Object> list = Convert.toList(Object.class, a);
        System.out.println(list);
    }

    @Test
    void toHex() {
        String a = "我是一个小小的可爱的字符串";

        //结果："e68891e698afe4b880e4b8aae5b08fe5b08fe79a84e58fafe788b1e79a84e5ad97e7aca6e4b8b2"
        String hex = Convert.toHex(a, CharsetUtil.CHARSET_UTF_8);
        System.out.println(hex);
    }

    @Test
    void strToUnicode() {
        String a = "我是一个小小的可爱的字符串";

        //结果为："\\u6211\\u662f\\u4e00\\u4e2a\\u5c0f\\u5c0f\\u7684\\u53ef\\u7231\\u7684\\u5b57\\u7b26\\u4e32"
        String unicode = Convert.strToUnicode(a);
        System.out.println(unicode);

        //结果为："我是一个小小的可爱的字符串"
        String raw = Convert.unicodeToStr(unicode);
        System.out.println(raw);
    }

    @Test
    void convertCharset() {
        String a = "我不是乱码";
        //转换后result为乱码
        String result = Convert.convertCharset(a, CharsetUtil.UTF_8, CharsetUtil.ISO_8859_1);
        String raw = Convert.convertCharset(result, CharsetUtil.ISO_8859_1, "UTF-8");
        System.out.println(result);
        System.out.println(raw);
    }

    @Test
    void convertTime() {
        long a = 4535345;

        //结果为：75
        long minutes = Convert.convertTime(a, TimeUnit.MILLISECONDS, TimeUnit.MINUTES);
        System.out.println(minutes);
    }

    @Test
    void digitToChinese() {
        double a = 67556.32;

        //结果为："陆万柒仟伍佰伍拾陆元叁角贰分"
        String digitUppercase = Convert.digitToChinese(a);
        System.out.println(digitUppercase);
    }

}
