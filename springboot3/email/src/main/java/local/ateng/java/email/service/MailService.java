package local.ateng.java.email.service;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MailService {
    private final JavaMailSender mailSender;

    // 发送 普通 邮件
    public void sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(SpringUtil.getProperty("spring.mail.username")); // 发件人邮箱，要和配置一致
        message.setTo(to);                     // 收件人邮箱
        message.setSubject(subject);           // 邮件标题
        message.setText(content);              // 邮件内容
        mailSender.send(message);
    }

    // 发送 HTML 邮件
    public void sendHtmlMail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true); // 第二个参数 true 表示支持 multipart

        helper.setFrom(SpringUtil.getProperty("spring.mail.username"));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true 表示 HTML 格式

        mailSender.send(message);
    }

}
