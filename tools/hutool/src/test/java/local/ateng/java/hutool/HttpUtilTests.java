package local.ateng.java.hutool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * HTTP客户端
 * https://www.hutool.cn/docs/#/http/%E6%A6%82%E8%BF%B0
 *
 * @author 孔余
 * @since 2024-01-17 20:05
 */
public class HttpUtilTests {
    // 最简单的HTTP请求，可以自动通过header等信息判断编码，不区分HTTP和HTTPS
    @Test
    void test01() {
        String str = HttpUtil.get("https://www.baidu.com");
        System.out.println(str);
    }

    // 当无法识别页面编码的时候，可以自定义请求页面的编码
    @Test
    void test02() {
        String str = HttpUtil.get("https://www.baidu.com", CharsetUtil.CHARSET_UTF_8);
        System.out.println(str);
    }

    // 可以单独传入http参数，这样参数会自动做URL编码，拼接在URL中
    @Test
    void test03() {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("city", "北京");
        String str = HttpUtil.get("https://www.baidu.com", paramMap);
        System.out.println(str);
    }

    // 可以单独传入http参数，这样参数会自动做URL编码，拼接在URL中
    @Test
    void test04() {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("city", "北京");
        String str = HttpUtil.post("https://www.baidu.com", paramMap);
        System.out.println(str);
    }

    // 文件上传
    @Test
    void test05() {
        HashMap<String, Object> paramMap = new HashMap<>();
        //文件上传只需将参数中的键指定（默认file），值设为文件对象即可，对于使用者来说，文件上传与普通表单提交并无区别
        paramMap.put("file", FileUtil.file("D:\\My\\图片\\logo.jpg"));
        String result = HttpUtil.post("https://www.baidu.com", paramMap);
        System.out.println(result);
    }

    // 下载文件
    @Test
    void test06() {
        // 因为Hutool-http机制问题，请求页面返回结果是一次性解析为byte[]的，如果请求URL返回结果太大（比如文件下载），那内存会爆掉，因此针对文件下载HttpUtil单独做了封装。文件下载在面对大文件时采用流的方式读写，内存中只是保留一定量的缓存，然后分块写入硬盘，因此大文件情况下不会对内存有压力。
        String fileUrl = "https://mirrors.aliyun.com/centos/8.5.2111/isos/x86_64/CentOS-8.5.2111-x86_64-dvd1.iso";
        //将文件下载后保存在E盘，返回结果为下载文件大小
        long size = HttpUtil.downloadFile(fileUrl, FileUtil.file("d:/"));
        System.out.println("Download size: " + size);
    }

}
