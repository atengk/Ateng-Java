package local.ateng.java.hutool;

import cn.hutool.core.lang.Console;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.Test;

/**
 * 在JDK中，我们可以借助URL对象完成URL的格式化，但是无法完成一些特殊URL的解析和处理，例如编码过的URL、不标准的路径和参数。
 * 在旧版本的hutool中，URL的规范完全靠字符串的替换来完成，不但效率低，而且处理过程及其复杂。
 * 于是在5.3.1之后，加入了UrlBuilder类，拆分URL的各个部分，分别处理和格式化，完成URL的规范。
 *
 * UrlBuilder主要应用于http模块，在构建HttpRequest时，用户传入的URL五花八门，为了做大最好的适应性，减少用户对URL的处理，使用UrlBuilder完成URL的规范化。
 * https://www.hutool.cn/docs/#/core/%E7%BD%91%E7%BB%9C/URL%E7%94%9F%E6%88%90%E5%99%A8-UrlBuilder
 *
 * @author 孔余
 * @since 2024-01-17 18:51
 */
public class UrlBuilderUtilTests {
    // 使用
    @Test
    void test01() {
        // 输出 http://www.hutool.cn/
        String buildUrl = UrlBuilder.of().setHost("www.hutool.cn").build();
        System.out.println(buildUrl);
    }

    // 完整构建
    @Test
    void test02() {
        // https://www.hutool.cn/aaa/bbb?ie=UTF-8&wd=test
        String buildUrl = UrlBuilder.of()
                .setScheme("https")
                .setHost("www.hutool.cn")
                .addPath("/aaa").addPath("bbb")
                .addQuery("ie", "UTF-8")
                .addQuery("wd", "test")
                .build();
        System.out.println(buildUrl);
    }

    // 中文编码
    @Test
    void test03() {
        // https://www.hutool.cn/s?ie=UTF-8&ie=GBK&wd=%E6%B5%8B%E8%AF%95
        String buildUrl = UrlBuilder.of()
                .setScheme("https")
                .setHost("www.hutool.cn")
                .addPath("/s")
                .addQuery("ie", "UTF-8")
                .addQuery("ie", "GBK")
                .addQuery("wd", "测试")
                .build();
        System.out.println(buildUrl);
    }

    // 解析
    @Test
    void test04() {
        // 当有一个URL字符串时，可以使用of方法解析：
        UrlBuilder builder = UrlBuilder.ofHttp("www.hutool.cn/aaa/bbb/?a=张三&b=%e6%9d%8e%e5%9b%9b#frag1", CharsetUtil.CHARSET_UTF_8);
        // 输出张三
        Console.log(builder.getQuery().get("a"));
        // 输出李四
        Console.log(builder.getQuery().get("b"));
    }

    // 特殊URL解析
    // 有时候URL中会存在&amp;这种分隔符，谷歌浏览器会将此字符串转换为&使用，Hutool中也同样如此：
    @Test
    void test05() {
        String urlStr = "https://mp.weixin.qq.com/s?__biz=MzI5NjkyNTIxMg==&amp;mid=100000465&amp;idx=1";
        UrlBuilder builder = UrlBuilder.ofHttp(urlStr, CharsetUtil.CHARSET_UTF_8);
        // https://mp.weixin.qq.com/s?__biz=MzI5NjkyNTIxMg==&mid=100000465&idx=1
        Console.log(builder.build());
    }

}
