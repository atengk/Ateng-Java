package local.ateng.java.email.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import local.ateng.java.email.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * 邮件服务接口
 * <p>
 * 该接口封装了邮件发送的常见功能，支持发送：
 * <ul>
 *     <li>纯文本邮件</li>
 *     <li>HTML 格式邮件</li>
 *     <li>带附件邮件</li>
 *     <li>带内嵌资源的邮件（例如内嵌图片）</li>
 *     <li>模板邮件（如基于 FreeMarker、Thymeleaf 等）</li>
 *     <li>群发邮件、抄送、密送</li>
 * </ul>
 *
 * @author 孔余
 * @since 2025-08-09
 */
@Service
public class MailServiceImpl implements MailService {

    /**
     * 邮箱格式正则，用于基础校验
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    /**
     * 系统默认发件人地址
     */
    private final String defaultFrom = SpringUtil.getProperty("spring.mail.username");

    /**
     * 注入 Spring Boot JavaMailSender
     */
    private final JavaMailSender mailSender;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人邮箱地址（单个）
     * @param subject 邮件主题
     * @param content 邮件内容（纯文本）
     */
    @Override
    public void sendTextMail(String to, String subject, String content) {
        sendTextMail(to, subject, content, true);
    }

    /**
     * 发送纯文本邮件（带邮箱格式校验开关）
     *
     * @param to                  收件人邮箱
     * @param subject             邮件主题
     * @param content             邮件内容
     * @param throwOnInvalidEmail 是否在邮箱格式错误时抛出异常（true 抛出，false 跳过）
     */
    @Override
    public void sendTextMail(String to, String subject, String content, boolean throwOnInvalidEmail) {
        /* 1. 校验邮箱格式 */
        if (!isValidEmail(to)) {
            if (throwOnInvalidEmail) {
                /* 邮箱格式错误，记录中文错误日志并抛出异常 */
                log.error("邮箱地址格式不正确，收件人：{}", to);
                throw new IllegalArgumentException("邮箱地址格式不正确：" + to);
            } else {
                /* 邮箱格式错误但跳过异常，仅记录警告 */
                log.warn("检测到无效邮箱地址（已跳过）：{}", to);
                return;
            }
        }

        /* 2. 创建邮件对象 */
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            /* 3. 设置发件人、收件人、主题、内容 */
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);

            /* 4. 发送邮件 */
            mailSender.send(message);
        } catch (Exception e) {
            if (throwOnInvalidEmail) {
                /* 记录中文异常日志，包含收件人、邮件主题和异常原因 */
                log.error("发送纯文本邮件失败，收件人：{}，主题：{}，原因：{}", to, subject, e.getMessage(), e);
                throw new RuntimeException("发送纯文本邮件失败，收件人：" + to + "，主题：" + subject, e);
            } else {
                /* 仅记录错误日志，不抛出异常 */
                log.warn("发送纯文本邮件失败（已跳过异常），收件人：{}，主题：{}，原因：{}", to, subject, e.getMessage());
            }
        }
    }

    /**
     * 发送纯文本邮件（支持多个收件人）
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param content 邮件内容（纯文本）
     */
    @Override
    public void sendTextMailList(List<String> toList, String subject, String content) {
        if (CollectionUtils.isEmpty(toList)) {
            return;
        }
        for (String to : toList) {
            sendTextMail(to, subject, content, false);
        }
    }

    /**
     * 发送 HTML 邮件（带邮箱格式校验）
     *
     * @param to                  收件人邮箱
     * @param subject             邮件主题
     * @param html                HTML 格式邮件正文
     * @param throwOnInvalidEmail 当邮箱格式无效时是否抛出异常
     */
    @Override
    public void sendHtmlMail(String to, String subject, String html, boolean throwOnInvalidEmail) {
        if (!isValidEmail(to)) {
            if (throwOnInvalidEmail) {
                log.error("邮箱地址格式不正确，收件人：{}", to);
                throw new IllegalArgumentException("邮箱地址格式不正确：" + to);
            } else {
                log.warn("检测到无效邮箱地址（已跳过）：{}", to);
                return;
            }
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("HTML 邮件发送成功，收件人：{}", to);
        } catch (Exception e) {
            log.error("发送 HTML 邮件失败，收件人：{}，原因：{}", to, e.getMessage(), e);
            if (throwOnInvalidEmail) {
                throw new RuntimeException("发送 HTML 邮件失败，收件人：" + to, e);
            }
        }
    }

    /**
     * 发送 HTML 格式邮件
     *
     * @param to      收件人邮箱地址（单个）
     * @param subject 邮件主题
     * @param html    邮件内容（HTML 格式）
     */
    @Override
    public void sendHtmlMail(String to, String subject, String html) {
        sendHtmlMail(to, subject, html, true);
    }

    /**
     * 发送 HTML 格式邮件（支持多个收件人）
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件内容（HTML 格式）
     */
    @Override
    public void sendHtmlMailList(List<String> toList, String subject, String html) {
        if (CollectionUtils.isEmpty(toList)) {
            return;
        }
        for (String to : toList) {
            sendHtmlMail(to, subject, html, false);
        }
    }

    /**
     * 发送带附件的邮件
     *
     * @param to      收件人邮箱地址（单个）
     * @param subject 邮件主题
     * @param html    邮件内容（HTML 格式，可为空）
     * @param files   附件文件列表
     */
    @Override
    public void sendMailWithAttachments(String to, String subject, String html, List<File> files) {
        if (!isValidEmail(to)) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            if (files != null) {
                for (File file : files) {
                    if (file.exists()) {
                        helper.addAttachment(file.getName(), file);
                    }
                }
            }
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with attachments to " + to, e);
        }
    }

    /**
     * 发送带附件的邮件（支持多个收件人）
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件内容（HTML 格式，可为空）
     * @param files   附件文件列表
     */
    @Override
    public void sendMailWithAttachmentsList(List<String> toList, String subject, String html, List<File> files) {
        if (CollectionUtils.isEmpty(toList)) {
            return;
        }
        for (String to : toList) {
            sendMailWithAttachments(to, subject, html, files);
        }
    }

    /**
     * 发送带内嵌静态资源的 HTML 邮件
     *
     * @param to         收件人邮箱地址（单个）
     * @param subject    邮件主题
     * @param html       邮件内容（HTML 格式）
     * @param resourceId 静态资源 ID（HTML 中引用，例如 <img src='cid:logo'>）
     * @param resource   静态资源文件
     */
    @Override
    public void sendMailWithInlineResource(String to, String subject, String html, String resourceId, File resource) {
        if (!isValidEmail(to)) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (resource != null && resource.exists()) {
                helper.addInline(resourceId, resource);
            }
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with inline resource to " + to, e);
        }
    }

    /**
     * 发送带内嵌静态资源的 HTML 邮件（支持多个资源）
     *
     * @param to        收件人邮箱地址（单个）
     * @param subject   邮件主题
     * @param html      邮件内容（HTML 格式）
     * @param resources 资源 Map（key 为 resourceId，value 为文件对象）
     */
    @Override
    public void sendMailWithInlineResources(String to, String subject, String html, Map<String, File> resources) {
        if (!isValidEmail(to)) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (resources != null) {
                for (Map.Entry<String, File> entry : resources.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().exists()) {
                        helper.addInline(entry.getKey(), entry.getValue());
                    }
                }
            }
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with inline resources to " + to, e);
        }
    }

    /**
     * 发送模板邮件（例如基于 FreeMarker、Thymeleaf 等）
     *
     * @param to        收件人邮箱地址（单个）
     * @param subject   邮件主题
     * @param template  模板名称（如 template.html 或 template.ftl）
     * @param variables 模板变量（key 为变量名，value 为变量值）
     */
    @Override
    public void sendTemplateMail(String to, String subject, String template, Map<String, Object> variables) {
        // 模板渲染部分可根据项目使用的模板引擎（Thymeleaf、Freemarker）来实现
        // 此处仅保留接口调用逻辑
        String renderedHtml = "[Template rendering not implemented]";
        sendHtmlMail(to, subject, renderedHtml);
    }

    /**
     * 群发邮件（支持 To、Cc、Bcc）
     *
     * @param toList  收件人邮箱地址列表
     * @param ccList  抄送邮箱地址列表（可为空）
     * @param bccList 密送邮箱地址列表（可为空）
     * @param subject 邮件主题
     * @param html    邮件内容（HTML 或纯文本）
     * @param files   附件列表（可为空）
     */
    @Override
    public void sendMailWithCcBcc(List<String> toList, List<String> ccList, List<String> bccList, String subject, String html, List<File> files) {
        if (CollectionUtils.isEmpty(toList)) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(defaultFrom);
            helper.setTo(toList.stream().filter(this::isValidEmail).toArray(String[]::new));
            if (!CollectionUtils.isEmpty(ccList)) {
                helper.setCc(ccList.stream().filter(this::isValidEmail).toArray(String[]::new));
            }
            if (!CollectionUtils.isEmpty(bccList)) {
                helper.setBcc(bccList.stream().filter(this::isValidEmail).toArray(String[]::new));
            }
            helper.setSubject(subject);
            helper.setText(html, true);
            if (files != null) {
                for (File file : files) {
                    if (file.exists()) {
                        helper.addAttachment(file.getName(), file);
                    }
                }
            }
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email with CC/BCC", e);
        }
    }

    /**
     * 判断邮箱地址是否符合格式
     *
     * @param email 邮箱
     * @return 是否匹配
     */
    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

}
