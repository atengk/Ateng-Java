package local.ateng.java.email.controller;

import jakarta.mail.MessagingException;
import local.ateng.java.email.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
public class MailController {

    @Autowired
    private MailService mailService;

    @GetMapping("/send")
    public String sendMail(@RequestParam String to) {
        mailService.sendSimpleMail(to, "测试邮件", "这是一封测试邮件！");
        return "邮件发送成功！";
    }

    @GetMapping("/send-html")
    public String sendHTMLMail(@RequestParam String to) throws MessagingException {
        mailService.sendHtmlMail(to, "HTML 邮件测试",
                "<h2 style='color:blue'>这是一封 HTML 格式的测试邮件</h2><p>内容支持 <b>HTML</b> 标签。</p>");
        return "邮件发送成功！";
    }

}