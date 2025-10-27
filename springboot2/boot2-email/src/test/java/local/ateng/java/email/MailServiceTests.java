package local.ateng.java.email;

import local.ateng.java.email.service.MailService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MailServiceTests {
    private final MailService mailService;
    private static final String TEST_EMAIL = "2385569970@qq.com";


    @Test
    public void testSendTextMail() {
        mailService.sendTextMail(TEST_EMAIL, "纯文本邮件测试", "这是一个纯文本邮件内容示例。");
    }

    @Test
    public void testSendHtmlMail() {
        String html = "<h1 style='color:blue'>HTML 邮件测试</h1><p>这是一封测试 HTML 邮件。</p>";
        mailService.sendHtmlMail(TEST_EMAIL, "HTML 邮件测试", html);
    }

    @Test
    public void testSendHtmlMail2() {
        String FROM_EMAIL = "2385569970@qq.com";
        String FROM_PASSWORD = "xxxxxxxxxxx";

        String html = "<h1 style='color:blue'>HTML 邮件测试</h1>"
                + "<p>这是一封测试 HTML 邮件。</p>";
        mailService.sendHtmlMail(FROM_EMAIL, FROM_PASSWORD, "17623062936@163.com", "HTML 邮件测试", html);
    }

    @Test
    public void testSendHtmlMail3() {
        String FROM_EMAIL = "2385569970@qq.com";

        String html = "<h1 style='color:blue'>HTML 邮件测试</h1>"
                + "<p>这是一封测试 HTML 邮件。</p>";
        mailService.sendHtmlMail("192.168.1.12", 11025, FROM_EMAIL, null, "17623062936@163.com", "HTML 邮件测试", html);
    }

    @Test
    public void testSendMailWithAttachments() {
        String html = "<p>请查收附件。</p>";

        // 模拟生成一个文件流
        byte[] fileBytes = "这是附件的内容".getBytes(StandardCharsets.UTF_8);
        InputStream fileStream = new ByteArrayInputStream(fileBytes);

        Map<String, InputStream> attachments = new HashMap<>();
        attachments.put("测试附件.txt", fileStream);

        mailService.sendMailWithAttachments(TEST_EMAIL, "带附件邮件测试", html, attachments);
    }

    @Test
    public void testSendMailWithAttachments2() throws IOException {
        String html = "<p>请查收附件：demo.xlsx</p>";

        // 附件路径
        File file = new File("D:\\Temp\\demo.xlsx");
        if (!file.exists()) {
            throw new FileNotFoundException("附件文件不存在：" + file.getAbsolutePath());
        }

        // 将文件读成 InputStream
        InputStream fileStream = new FileInputStream(file);

        // 组装附件集合
        Map<String, InputStream> attachments = new HashMap<>();
        attachments.put(file.getName(), fileStream);

        // 调用发送方法
        mailService.sendMailWithAttachments(TEST_EMAIL, "带本地附件邮件测试", html, attachments);

        fileStream.close();
    }

    @Test
    public void testSendMailWithImages() {
        String html = "<h1>带图片的邮件</h1><img src='cid:testImage'/>";

        // 模拟一张图片流（这里用文字代替图片，实际应读取图片二进制）
        byte[] imageBytes = "FakeImageContent".getBytes(StandardCharsets.UTF_8);
        InputStream imageStream = new ByteArrayInputStream(imageBytes);

        Map<String, InputStream> images = new HashMap<>();
        images.put("testImage", imageStream);

        mailService.sendMailWithImages(TEST_EMAIL, "带图片的邮件测试", html, images);
    }

    @Test
    public void testSendMailWithImages2() throws IOException {
        String html = "<h1>带图片的邮件</h1><p>下面是内嵌图片：</p><img src='cid:testImage'/>";

        // 从本地文件读取图片为 InputStream
        try (InputStream imageStream = new FileInputStream("D:\\Temp\\flower-9453062_1280.jpg")) {

            Map<String, InputStream> images = new HashMap<>();
            images.put("testImage", imageStream);

            mailService.sendMailWithImages(TEST_EMAIL, "带图片的邮件测试", html, images);
        }
    }

    @Test
    public void testSendBatchTextMail() {
        List<String> recipients = Arrays.asList(TEST_EMAIL, "another@example.com");
        mailService.sendBatchTextMail(recipients, "批量纯文本邮件测试", "这是一封批量纯文本邮件。");
    }

    @Test
    public void testSendBatchHtmlMail() {
        List<String> recipients = Arrays.asList(TEST_EMAIL, "another@example.com");
        String html = "<h1>批量 HTML 邮件测试</h1><p>这是一封批量 HTML 邮件。</p>";
        mailService.sendBatchHtmlMail(recipients, "批量 HTML 邮件测试", html);
    }

    @Test
    public void testSendHtmlMailAsync() {
        String html = "<h1>异步 HTML 邮件测试</h1><p>这是异步发送的 HTML 邮件。</p>";
        mailService.sendHtmlMailAsync(TEST_EMAIL, "异步 HTML 邮件测试", html);
    }

    @Test
    public void testSendHtmlMailWithCcBcc() {
        String html = "<h1>邮件测试</h1><p>这封邮件带可选抄送和密送。</p>";

        List<String> ccList = Arrays.asList("cc@example.com");
        List<String> bccList = Arrays.asList("bcc@example.com");

        mailService.sendHtmlMailWithCcBcc(
                TEST_EMAIL,
                ccList,
                bccList,
                "带抄送和密送邮件测试",
                html
        );
    }

    @Test
    public void testSendMailAsync() throws InterruptedException {
        String html = "<h1>异步 HTML 邮件测试</h1><p>这是异步发送的 HTML 邮件。</p>";
        mailService.sendTextMail(TEST_EMAIL, "纯文本邮件测试", "这是一个纯文本邮件内容示例。");
        mailService.sendMailAsync(TEST_EMAIL, null, null, "异步 HTML 邮件测试", html, null, null);
        Thread.sleep(10_000);
    }

}
