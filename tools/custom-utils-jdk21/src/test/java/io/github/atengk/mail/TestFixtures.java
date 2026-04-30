package io.github.atengk.mail;

import io.github.atengk.utils.mail.MailUtil;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

final class TestFixtures {

    private TestFixtures() {
    }

    static Session session() {
        return Session.getInstance(new Properties());
    }

    static MailUtil.MailConfig smtpConfig() {
        MailUtil.MailConfig config = MailUtil.createSmtpConfig("localhost", 25, "user", "password", false, false);
        config.connectionTimeoutMillis = 100;
        config.timeoutMillis = 100;
        config.writeTimeoutMillis = 100;
        return config;
    }

    static MailUtil.MailMessage message() {
        MailUtil.MailMessage message = new MailUtil.MailMessage();
        message.from = "sender@example.com";
        message.to.add("receiver@example.com");
        message.subject = "测试主题";
        message.content = "测试正文";
        message.html = false;
        return message;
    }

    static MailUtil.MailMessage htmlMessage() {
        MailUtil.MailMessage message = message();
        message.content = "<p>测试正文</p>";
        message.html = true;
        return message;
    }

    static MimeMessage mimeMessage(String subject, String content) throws Exception {
        MimeMessage message = new MimeMessage(session());
        message.setFrom("sender@example.com");
        message.setRecipients(Message.RecipientType.TO, "receiver@example.com");
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(content, StandardCharsets.UTF_8.name());
        message.saveChanges();
        return message;
    }

    static List<String> recipients() {
        return List.of("receiver@example.com");
    }
}
