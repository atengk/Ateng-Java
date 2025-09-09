# Email



## 基础配置

**添加依赖**

```xml
<!-- Spring Boot 的邮件启动器依赖，用于发送邮件功能 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

**编辑配置文件**

- [QQ邮箱使用说明](https://wx.mail.qq.com/list/readtemplate?name=app_intro.html#/agreement/authorizationCode)
- [163邮箱使用说明](https://help.mail.163.com/faqDetail.do?code=d7a5dc8471cd0c0e8b4b8f4f8e49998b374173cfe9171305fa1ce630d7f67ac2a5feb28b66796d3b)

```yaml
---
# 邮箱配置
spring:
  mail:
    host: smtp.qq.com
    port: 465
    username: 2385569970@qq.com
    password: "***********"
    protocol: smtps
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true
    default-encoding: UTF-8
```

**无认证配置**

```yaml
---
# 邮箱配置
spring:
  mail:
    host: 192.168.1.12
    port: 1025
    username: 2385569970@qq.com
    password: 
    protocol: smtp
    default-encoding: UTF-8
```



## 创建服务

### 创建Service

```java
package local.ateng.java.email.service;

import org.springframework.scheduling.annotation.Async;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 邮件服务接口
 *
 * @author 孔余
 * @since 2025-08-09
 */
public interface MailService {

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本格式）
     */
    void sendTextMail(String to, String subject, String content);

    /**
     * 发送 HTML 格式的邮件
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendHtmlMail(String to, String subject, String html);

    /**
     * 使用动态发件人发送 HTML 邮件
     *
     * @param from     发件人邮箱
     * @param password 发件人授权码/密码
     * @param to       收件人邮箱
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    void sendHtmlMail(String from, String password, String to, String subject, String html);

    /**
     * 使用动态发件人 + 动态主机配置发送 HTML 邮件
     *
     * @param host     SMTP 服务器地址（例如 smtp.qq.com）
     * @param port     SMTP 服务器端口（25 / 465 / 587）
     * @param from     发件人邮箱
     * @param password 发件人授权码/密码
     * @param to       收件人邮箱
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    void sendHtmlMail(String host, int port, String from, String password, String to, String subject, String html);

    /**
     * 发送带附件的邮件（支持多个附件）
     *
     * @param to          收件人邮箱地址
     * @param subject     邮件主题
     * @param html        邮件正文（HTML 格式）
     * @param attachments 附件集合
     *                    key   为附件文件名
     *                    value 为附件的输入流
     */
    void sendMailWithAttachments(String to, String subject, String html, Map<String, InputStream> attachments);

    /**
     * 发送带图片的 HTML 邮件
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     * @param images  图片集合
     *                key   为图片 contentId（HTML 中引用时使用 cid:contentId）
     *                value 为图片的输入流
     */
    void sendMailWithImages(String to, String subject, String html, Map<String, InputStream> images);

    /**
     * 批量发送纯文本邮件
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本格式）
     */
    void sendBatchTextMail(List<String> toList, String subject, String content);

    /**
     * 批量发送 HTML 邮件
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendBatchHtmlMail(List<String> toList, String subject, String html);

    /**
     * 使用动态发件人批量发送 HTML 邮件
     *
     * @param from     发件人邮箱
     * @param password 发件人授权码/密码
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendBatchHtmlMail(String from, String password, List<String> toList, String subject, String html);

    /**
     * 异步发送邮件（适用于耗时的发送场景）
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    @Async
    void sendHtmlMailAsync(String to, String subject, String html);

    /**
     * 批量异步发送 HTML 邮件
     *
     * <p>每封邮件都是异步发送，不阻塞调用线程。</p>
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    @Async
    void sendBatchHtmlMailAsync(List<String> toList, String subject, String html);

    /**
     * 发送 HTML 邮件，支持抄送（Cc）和密送（Bcc），可为空
     *
     * @param to      收件人邮箱地址
     * @param ccList  抄送邮箱地址列表，可为空
     * @param bccList 密送邮箱地址列表，可为空
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendHtmlMailWithCcBcc(String to, List<String> ccList, List<String> bccList, String subject, String html);

    /**
     * 异步发送 HTML 邮件，支持抄送（Cc）和密送（Bcc）
     *
     * @param to      收件人邮箱地址
     * @param ccList  抄送邮箱地址列表，可为空
     * @param bccList 密送邮箱地址列表，可为空
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    @Async
    void sendHtmlMailWithCcBccAsync(String to, List<String> ccList, List<String> bccList, String subject, String html);

    /**
     * 发送全功能邮件（HTML + 可选抄送/密送 + 附件 + 内嵌图片）
     *
     * @param to          收件人邮箱地址
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文（HTML 格式）
     * @param attachments 附件集合，key=附件名，value=InputStream，可为空
     * @param images      图片集合，key=contentId，value=InputStream，可为空
     */
    void sendMail(String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

    /**
     * 发送全功能邮件（HTML + 可选抄送/密送 + 附件 + 内嵌图片）异步
     *
     * @param to          收件人邮箱地址
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文（HTML 格式）
     * @param attachments 附件集合，key=附件名，value=InputStream，可为空
     * @param images      图片集合，key=contentId，value=InputStream，可为空
     */
    @Async
    void sendMailAsync(String to,
                       List<String> ccList,
                       List<String> bccList,
                       String subject,
                       String html,
                       Map<String, InputStream> attachments,
                       Map<String, InputStream> images);

    /**
     * 发送全功能邮件（支持自定义发件账号，HTML + 可选抄送/密送 + 附件 + 内嵌图片）
     *
     * <p>该方法允许调用时指定发件人邮箱及密码（授权码），
     * 其他服务器参数（host、port、协议等）依然从配置文件中读取。</p>
     *
     * @param from        发件人邮箱地址
     * @param password    发件人邮箱密码或授权码
     * @param to          收件人邮箱地址，不能为空
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文内容（HTML 格式）
     * @param attachments 附件集合，key = 附件名，value = InputStream，可为空
     * @param images      内嵌图片集合，key = contentId，value = InputStream，可为空，
     *                    contentId 对应 HTML 内容中的 "cid:xxx" 引用
     */
    void sendMail(String from,
                  String password,
                  String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

    /**
     * 发送全功能邮件（支持自定义发件账号，HTML + 可选抄送/密送 + 附件 + 内嵌图片） 异步
     *
     * <p>该方法允许调用时指定发件人邮箱及密码（授权码），
     * 其他服务器参数（host、port、协议等）依然从配置文件中读取。</p>
     *
     * @param from        发件人邮箱地址
     * @param password    发件人邮箱密码或授权码
     * @param to          收件人邮箱地址，不能为空
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文内容（HTML 格式）
     * @param attachments 附件集合，key = 附件名，value = InputStream，可为空
     * @param images      内嵌图片集合，key = contentId，value = InputStream，可为空，
     *                    contentId 对应 HTML 内容中的 "cid:xxx" 引用
     */
    @Async
    void sendMailAsync(String from,
                  String password,
                  String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

    /**
     * 发送全功能邮件（支持自定义发件账号，HTML + 可选抄送/密送 + 附件 + 内嵌图片）
     *
     * <p>该方法允许在调用时指定邮件服务器主机、端口、账号及密码，
     * 适用于多租户场景或需要动态切换发件人账号的场景。</p>
     *
     * @param host        邮件服务器地址，例如 "smtp.example.com"
     * @param port        邮件服务器端口，通常为 25、465（SSL）、587（TLS）
     * @param from        发件人邮箱地址（即实际账号）
     * @param password    发件人邮箱密码或授权码
     * @param to          收件人邮箱地址，不能为空
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文内容（HTML 格式）
     * @param attachments 附件集合，key = 附件名，value = InputStream，可为空
     * @param images      内嵌图片集合，key = contentId，value = InputStream，可为空，
     *                    contentId 对应 HTML 内容中的 "cid:xxx" 引用
     */
    void sendMail(String host,
                  int port,
                  String from,
                  String password,
                  String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

    /**
     * 发送全功能邮件（支持自定义发件账号，HTML + 可选抄送/密送 + 附件 + 内嵌图片） 异步
     *
     * <p>该方法允许在调用时指定邮件服务器主机、端口、账号及密码，
     * 适用于多租户场景或需要动态切换发件人账号的场景。</p>
     *
     * @param host        邮件服务器地址，例如 "smtp.example.com"
     * @param port        邮件服务器端口，通常为 25、465（SSL）、587（TLS）
     * @param from        发件人邮箱地址（即实际账号）
     * @param password    发件人邮箱密码或授权码
     * @param to          收件人邮箱地址，不能为空
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文内容（HTML 格式）
     * @param attachments 附件集合，key = 附件名，value = InputStream，可为空
     * @param images      内嵌图片集合，key = contentId，value = InputStream，可为空，
     *                    contentId 对应 HTML 内容中的 "cid:xxx" 引用
     */
    @Async
    void sendMailAsync(String host,
                  int port,
                  String from,
                  String password,
                  String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

}
```

### 创建ServiceImpl

```java
package local.ateng.java.email.service.impl;

import local.ateng.java.email.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 邮件服务接口实现
 *
 * @author 孔余
 * @since 2025-08-09
 */
@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    /**
     * 邮件发送器
     */
    private final JavaMailSender mailSender;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发件人邮箱地址
     */
    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendTextMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
            log.info("邮件发送成功，收件人：{}", to);
        } catch (MailException e) {
            log.error("邮件发送失败，收件人：{}，错误信息：{}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtmlMail(String to, String subject, String html) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}", to);
        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败，收件人：{}，错误信息：{}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtmlMail(String from, String password, String to, String subject, String html) {
        JavaMailSenderImpl base = (JavaMailSenderImpl) mailSender;
        sendHtmlMail(base.getHost(), base.getPort(), from, password, to, subject, html);
    }

    @Override
    public void sendHtmlMail(String host, int port, String from, String password, String to, String subject, String html) {
        JavaMailSenderImpl base = (JavaMailSenderImpl) mailSender;
        JavaMailSenderImpl customSender = new JavaMailSenderImpl();
        customSender.setHost(host);
        customSender.setPort(port);
        customSender.setProtocol(base.getProtocol());
        customSender.setDefaultEncoding(base.getDefaultEncoding());
        customSender.setJavaMailProperties(base.getJavaMailProperties());

        customSender.setUsername(from);
        customSender.setPassword(password);

        MimeMessage mimeMessage = customSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            customSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}", to);
        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败，收件人：{}，错误信息：{}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendMailWithAttachments(String to, String subject, String html, Map<String, InputStream> attachments) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            if (attachments != null && !attachments.isEmpty()) {
                for (Map.Entry<String, InputStream> entry : attachments.entrySet()) {
                    String fileName = entry.getKey();
                    byte[] data = toByteArrayAndClose(entry.getValue());
                    InputStreamSource reusable = reusableSource(data);
                    helper.addAttachment(fileName, reusable);
                }
            }

            mailSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}", to);
        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败，收件人：{}，错误信息：{}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendMailWithImages(String to, String subject, String html, Map<String, InputStream> images) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            if (images != null && !images.isEmpty()) {
                for (Map.Entry<String, InputStream> entry : images.entrySet()) {
                    String contentId = entry.getKey();
                    byte[] data = toByteArrayAndClose(entry.getValue());
                    String contentType = sniffImageContentType(data);
                    InputStreamSource reusable = reusableSource(data);
                    helper.addInline(contentId, reusable, contentType);
                }
            }

            mailSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}", to);
        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败，收件人：{}，错误信息：{}", to, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendBatchTextMail(List<String> toList, String subject, String content) {
        if (toList == null || toList.isEmpty()) {
            log.warn("批量邮件发送失败：收件人列表为空");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toList.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(content);

        try {
            mailSender.send(message);
            log.info("批量邮件发送成功，收件人数：{}", toList.size());
        } catch (MailException e) {
            log.error("批量邮件发送失败，收件人数：{}，错误信息：{}", toList.size(), e.getMessage(), e);
            throw new RuntimeException("批量邮件发送失败", e);
        }
    }

    @Override
    public void sendBatchHtmlMail(List<String> toList, String subject, String html) {
        if (toList == null || toList.isEmpty()) {
            log.warn("批量邮件发送失败：收件人列表为空");
            return;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toList.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("批量邮件发送成功，收件人数：{}", toList.size());
        } catch (MessagingException | MailException e) {
            log.error("批量邮件发送失败，收件人数：{}，错误信息：{}", toList.size(), e.getMessage(), e);
            throw new RuntimeException("批量邮件发送失败", e);
        }
    }

    @Override
    public void sendBatchHtmlMail(String from, String password, List<String> toList, String subject, String html) {
        JavaMailSenderImpl base = (JavaMailSenderImpl) mailSender;
        JavaMailSenderImpl customSender = new JavaMailSenderImpl();
        customSender.setHost(base.getHost());
        customSender.setPort(base.getPort());
        customSender.setProtocol(base.getProtocol());
        customSender.setDefaultEncoding(base.getDefaultEncoding());
        customSender.setJavaMailProperties(base.getJavaMailProperties());

        customSender.setUsername(from);
        customSender.setPassword(password);

        MimeMessage mimeMessage = customSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toList.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(html, true);

            customSender.send(mimeMessage);
            log.info("批量邮件发送成功，收件人数：{}", toList.size());
        } catch (MessagingException | MailException e) {
            log.error("批量邮件发送失败，收件人数：{}，错误信息：{}", toList.size(), e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtmlMailAsync(String to, String subject, String html) {
        log.info("异步邮件发送任务已提交，收件人：{}", to);
        sendHtmlMail(to, subject, html);
    }

    @Override
    public void sendBatchHtmlMailAsync(List<String> toList, String subject, String html) {
        if (toList == null || toList.isEmpty()) {
            log.warn("批量异步邮件发送失败：收件人列表为空");
            return;
        }

        for (String to : toList) {
            // 每个收件人都调用异步发送方法
            sendHtmlMailAsync(to, subject, html);
        }

        log.info("批量异步邮件发送任务已提交，收件人数：{}", toList.size());
    }

    @Override
    public void sendHtmlMailWithCcBcc(String to, List<String> ccList, List<String> bccList, String subject, String html) {
        if (to == null || to.isEmpty()) {
            log.warn("邮件发送失败：收件人为空");
            return;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);

            if (ccList != null && !ccList.isEmpty()) {
                helper.setCc(ccList.toArray(new String[0]));
            }

            if (bccList != null && !bccList.isEmpty()) {
                helper.setBcc(bccList.toArray(new String[0]));
            }

            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}，抄送：{}，密送：{}", to, ccList, bccList);
        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败，收件人：{}，抄送：{}，密送：{}，错误信息：{}", to, ccList, bccList, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtmlMailWithCcBccAsync(String to, List<String> ccList, List<String> bccList, String subject, String html) {
        log.info("异步邮件发送任务已提交，线程：{}", Thread.currentThread().getName());
        sendHtmlMailWithCcBcc(to, ccList, bccList, subject, html);
    }

    @Override
    public void sendMail(String to,
                         List<String> ccList,
                         List<String> bccList,
                         String subject,
                         String html,
                         Map<String, InputStream> attachments,
                         Map<String, InputStream> images) {
        if (to == null || to.isEmpty()) {
            log.warn("邮件发送失败：收件人为空");
            return;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);

            if (ccList != null && !ccList.isEmpty()) {
                helper.setCc(ccList.toArray(new String[0]));
            }

            if (bccList != null && !bccList.isEmpty()) {
                helper.setBcc(bccList.toArray(new String[0]));
            }

            helper.setSubject(subject);
            helper.setText(html, true);

            // 附件处理
            if (attachments != null && !attachments.isEmpty()) {
                for (Map.Entry<String, InputStream> entry : attachments.entrySet()) {
                    String fileName = entry.getKey();
                    byte[] data = toByteArrayAndClose(entry.getValue());
                    InputStreamSource reusable = reusableSource(data);
                    helper.addAttachment(fileName, reusable);
                }
            }

            // 内嵌图片处理
            if (images != null && !images.isEmpty()) {
                for (Map.Entry<String, InputStream> entry : images.entrySet()) {
                    String contentId = entry.getKey();
                    byte[] data = toByteArrayAndClose(entry.getValue());
                    String contentType = sniffImageContentType(data);
                    InputStreamSource reusable = reusableSource(data);
                    helper.addInline(contentId, reusable, contentType);
                }
            }

            mailSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}，抄送：{}，密送：{}", to, ccList, bccList);
        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败，收件人：{}，抄送：{}，密送：{}，错误信息：{}", to, ccList, bccList, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendMailAsync(String to, List<String> ccList, List<String> bccList, String subject, String html, Map<String, InputStream> attachments, Map<String, InputStream> images) {
        sendMail(to, ccList, bccList, subject, html, attachments, images);
    }

    @Override
    public void sendMail(String from,
                         String password,
                         String to,
                         List<String> ccList,
                         List<String> bccList,
                         String subject,
                         String html,
                         Map<String, InputStream> attachments,
                         Map<String, InputStream> images) {

        if (to == null || to.isEmpty()) {
            log.warn("邮件发送失败：收件人为空");
            return;
        }

        try {
            // 克隆配置文件中的 mailSender，并覆盖账号/密码
            JavaMailSenderImpl senderImpl = (JavaMailSenderImpl) this.mailSender;
            JavaMailSenderImpl customSender = new JavaMailSenderImpl();
            customSender.setHost(senderImpl.getHost());
            customSender.setPort(senderImpl.getPort());
            customSender.setProtocol(senderImpl.getProtocol());
            customSender.setDefaultEncoding(senderImpl.getDefaultEncoding());
            customSender.setJavaMailProperties(senderImpl.getJavaMailProperties());
            customSender.setUsername(from);
            customSender.setPassword(password);

            MimeMessage mimeMessage = customSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);

            if (ccList != null && !ccList.isEmpty()) {
                helper.setCc(ccList.toArray(new String[0]));
            }

            if (bccList != null && !bccList.isEmpty()) {
                helper.setBcc(bccList.toArray(new String[0]));
            }

            helper.setSubject(subject);
            helper.setText(html, true);

            // 附件处理
            if (attachments != null && !attachments.isEmpty()) {
                for (Map.Entry<String, InputStream> entry : attachments.entrySet()) {
                    String fileName = entry.getKey();
                    byte[] data = toByteArrayAndClose(entry.getValue());
                    InputStreamSource reusable = reusableSource(data);
                    helper.addAttachment(fileName, reusable);
                }
            }

            // 内嵌图片处理
            if (images != null && !images.isEmpty()) {
                for (Map.Entry<String, InputStream> entry : images.entrySet()) {
                    String contentId = entry.getKey();
                    byte[] data = toByteArrayAndClose(entry.getValue());
                    String contentType = sniffImageContentType(data);
                    InputStreamSource reusable = reusableSource(data);
                    helper.addInline(contentId, reusable, contentType);
                }
            }

            customSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}，抄送：{}，密送：{}", to, ccList, bccList);

        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败，收件人：{}，抄送：{}，密送：{}，错误信息：{}", to, ccList, bccList, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendMailAsync(String from,
                              String password,
                              String to,
                              List<String> ccList,
                              List<String> bccList,
                              String subject,
                              String html,
                              Map<String, InputStream> attachments,
                              Map<String, InputStream> images) {
        sendMail(from, password, to, ccList, bccList, subject, html, attachments, images);
    }

    @Override
    public void sendMail(String host,
                         int port,
                         String from,
                         String password,
                         String to,
                         List<String> ccList,
                         List<String> bccList,
                         String subject,
                         String html,
                         Map<String, InputStream> attachments,
                         Map<String, InputStream> images) {
        if (to == null || to.isEmpty()) {
            log.warn("邮件发送失败：收件人为空");
            return;
        }

        JavaMailSenderImpl base = (JavaMailSenderImpl) mailSender;
        JavaMailSenderImpl customSender = new JavaMailSenderImpl();
        customSender.setHost(host);
        customSender.setPort(port);
        customSender.setProtocol(base.getProtocol());
        customSender.setDefaultEncoding(base.getDefaultEncoding());
        customSender.setJavaMailProperties(base.getJavaMailProperties());

        customSender.setUsername(from);
        customSender.setPassword(password);

        MimeMessage mimeMessage = customSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);

            if (ccList != null && !ccList.isEmpty()) {
                helper.setCc(ccList.toArray(new String[0]));
            }
            if (bccList != null && !bccList.isEmpty()) {
                helper.setBcc(bccList.toArray(new String[0]));
            }

            helper.setSubject(subject);
            helper.setText(html, true);

            // 附件处理
            if (attachments != null && !attachments.isEmpty()) {
                for (Map.Entry<String, InputStream> entry : attachments.entrySet()) {
                    String fileName = entry.getKey();
                    byte[] data = toByteArrayAndClose(entry.getValue());
                    InputStreamSource reusable = reusableSource(data);
                    helper.addAttachment(fileName, reusable);
                }
            }

            // 内嵌图片处理
            if (images != null && !images.isEmpty()) {
                for (Map.Entry<String, InputStream> entry : images.entrySet()) {
                    String contentId = entry.getKey();
                    byte[] data = toByteArrayAndClose(entry.getValue());
                    String contentType = sniffImageContentType(data);
                    InputStreamSource reusable = reusableSource(data);
                    helper.addInline(contentId, reusable, contentType);
                }
            }

            customSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}，抄送：{}，密送：{}", to, ccList, bccList);
        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败，收件人：{}，抄送：{}，密送：{}，错误信息：{}", to, ccList, bccList, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendMailAsync(String host,
                              int port,
                              String from,
                              String password,
                              String to,
                              List<String> ccList,
                              List<String> bccList,
                              String subject,
                              String html,
                              Map<String, InputStream> attachments,
                              Map<String, InputStream> images) {
        sendMail(host, port, from, password, to, ccList, bccList, subject, html, attachments, images);
    }

    /**
     * 将输入流完整读取为字节数组并关闭输入流
     *
     * @param input 输入流
     * @return 字节数组
     */
    private byte[] toByteArrayAndClose(InputStream input) {
        final int bufferSize = 8192;
        if (input == null) {
            return new byte[0];
        }
        try (InputStream in = input; java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[bufferSize];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            log.error("读取输入流失败，错误信息：{}", e.getMessage(), e);
            return new byte[0];
        }
    }

    /**
     * 构造可重复读取的 InputStreamSource
     *
     * @param data 原始字节数组
     * @return 可重复读取的 InputStreamSource
     */
    private org.springframework.core.io.InputStreamSource reusableSource(byte[] data) {
        final byte[] copied = (data == null) ? new byte[0] : data.clone();
        return () -> new java.io.ByteArrayInputStream(copied);
    }

    /**
     * 简单嗅探图片 MIME 类型
     *
     * @param data 图片字节数组
     * @return MIME 类型字符串
     */
    private String sniffImageContentType(byte[] data) {
        // 内部常量定义
        final int minLength = 12;
        final byte[] jpegPrefix = {(byte) 0xFF, (byte) 0xD8};
        final byte[] pngPrefix = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        final byte[] gifPrefix = {'G', 'I', 'F'};
        final byte[] bmpPrefix = {'B', 'M'};
        final byte[] webpPrefix = {'R', 'I', 'F', 'F'};
        final int webpSuffixOffset = 8;
        final byte[] webpSuffix = {'W', 'E', 'B', 'P'};
        final String defaultMime = "application/octet-stream";

        if (data == null || data.length < minLength) {
            return defaultMime;
        }

        // 工具函数判断前缀
        final java.util.function.BiPredicate<byte[], byte[]> startsWith = (d, prefix) -> {
            if (d.length < prefix.length) {
                return false;
            }
            for (int i = 0; i < prefix.length; i++) {
                if (d[i] != prefix[i]) {
                    return false;
                }
            }
            return true;
        };

        if (startsWith.test(data, jpegPrefix)) {
            return "image/jpeg";
        }

        if (startsWith.test(data, pngPrefix)) {
            return "image/png";
        }

        if (startsWith.test(data, gifPrefix)) {
            return "image/gif";
        }

        if (startsWith.test(data, bmpPrefix)) {
            return "image/bmp";
        }

        // WEBP 特殊处理
        boolean webpPrefixMatch = startsWith.test(java.util.Arrays.copyOfRange(data, 0, webpPrefix.length), webpPrefix);
        boolean webpSuffixMatch = startsWith.test(java.util.Arrays.copyOfRange(data, webpSuffixOffset, webpSuffixOffset + webpSuffix.length), webpSuffix);
        if (webpPrefixMatch && webpSuffixMatch) {
            return "image/webp";
        }

        return defaultMime;
    }

}
```

## 使用测试

```java
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


}

```

