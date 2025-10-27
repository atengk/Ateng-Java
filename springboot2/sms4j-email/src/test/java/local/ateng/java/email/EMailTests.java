package local.ateng.java.email;

import org.dromara.email.api.MailClient;
import org.dromara.email.comm.entity.MailMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SpringBootTest
public class EMailTests {
    @Autowired
    private MailClient mailClient;

    @Test
    public void test1() {
        List<String> mailAddress = new ArrayList<>();
        mailAddress.add("2385569970@qq.com");
        mailAddress.add("kongyu@beraising.cn");
        MailMessage message = MailMessage.Builder()
                .mailAddress(mailAddress)
                .title("测试标题")
                .body("测试邮件发送")
                .htmlContent("11111111111")
                .build();
        mailClient.send(message);
    }

    @Test
    public void test2() {
        List<String> mailAddress = new ArrayList<>();
        mailAddress.add("2385569970@qq.com");
        mailAddress.add("kongyu@beraising.cn");
        MailMessage message = MailMessage.Builder()
                .mailAddress(mailAddress)
                .title("测试标题")
                .body("测试邮件发送")
                .htmlContent("11111111111")
                .files("logo.png", "https://sms4j.com/logo.png")
                //.zipName("压缩文件名称")
                .build();

        mailClient.send(message);

    }

    @Test
    public void test3() {
        List<String> mailAddress = new ArrayList<>();
        mailAddress.add("2385569970@qq.com");
        mailAddress.add("kongyu@beraising.cn");
        HashMap<String, String> filesMap = new HashMap<>();
        filesMap.put("sms4j.png", "https://sms4j.com/logo.png");
        filesMap.put("sa-token.png", "https://oss.dev33.cn/sa-token/doc/home/sa-token-jss--tran.png");
        MailMessage message = MailMessage.Builder()
                .mailAddress(mailAddress)
                .title("测试标题")
                .body("测试邮件发送")
                .htmlContent("11111111111")
                .files(filesMap)
                .zipName("附件-归档.zip")
                .build();

        mailClient.send(message);

    }

}
