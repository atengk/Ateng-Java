package local.ateng.java.hutool;

import cn.hutool.core.util.URLUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

/**
 * URL工具
 * https://www.hutool.cn/docs/#/core/%E5%B7%A5%E5%85%B7%E7%B1%BB/URL%E5%B7%A5%E5%85%B7-URLUtil
 *
 * @author 孔余
 * @since 2024-01-17 19:54
 */
public class URLUtilTests {
    // 标准化化URL链接。对于不带http://头的地址做简单补全。
    @Test
    void test01() {
        String url1 = "http://www.hutool.cn//aaa/bbb";
        // 结果为：http://www.hutool.cn/aaa/bbb
        String normalize1 = URLUtil.normalize(url1);
        System.out.println(normalize1);

        String url2 = "http://www.hutool.cn//aaa/\\bbb?a=1&b=2";
        // 结果为：http://www.hutool.cn/aaa/bbb?a=1&b=2
        String normalize2 = URLUtil.normalize(url2);
        System.out.println(normalize2);
    }
    // URL转码和解码
    @Test
    void test02() {
        String decode = URLUtil.decode("https://www.hutool.cn/docs/#/core/%E5%B7%A5%E5%85%B7%E7%B1%BB/URL%E5%B7%A5%E5%85%B7-URLUtil");
        System.out.println(decode);

        String encode = URLUtil.encode("https://www.hutool.cn/docs/#/core/工具类/URL工具-URLUtil", Charset.defaultCharset());
        System.out.println(encode);
    }

}
