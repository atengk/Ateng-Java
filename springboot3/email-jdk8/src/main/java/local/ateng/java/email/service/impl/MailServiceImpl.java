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

        log.info("批量异步邮件发送任务已提交，收件人数：{}", toList.size());

        for (String to : toList) {
            // 每个收件人都调用异步发送方法
            sendHtmlMailAsync(to, subject, html);
        }
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
        log.info("异步邮件发送任务已提交，线程：{}", Thread.currentThread().getName());
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
        log.info("异步邮件发送任务已提交，线程：{}", Thread.currentThread().getName());
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
        log.info("异步邮件发送任务已提交，线程：{}", Thread.currentThread().getName());
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