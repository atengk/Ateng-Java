package io.github.atengk.mail.client;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.config.MailBizProperties;
import io.github.atengk.mail.model.MailAttachment;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 邮件发送客户端
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
public class MailSendClient {

    private static final Logger log = LoggerFactory.getLogger(MailSendClient.class);

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final MailBizProperties mailBizProperties;
    private final TemplateEngine templateEngine;

    public MailSendClient(JavaMailSender javaMailSender,
                          MailProperties mailProperties,
                          MailBizProperties mailBizProperties,
                          TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
        this.mailBizProperties = mailBizProperties;
        this.templateEngine = templateEngine;
    }

    /**
     * 发送普通文本邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    public MailSendResult sendTextMail(MailSendRequest request) {
        return sendMimeMail(request, false, false);
    }

    /**
     * 发送 HTML 邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    public MailSendResult sendHtmlMail(MailSendRequest request) {
        return sendMimeMail(request, true, false);
    }

    /**
     * 发送附件邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    public MailSendResult sendAttachmentMail(MailSendRequest request) {
        return sendMimeMail(request, request != null && request.html(), true);
    }

    /**
     * 发送模板邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    public MailSendResult sendTemplateMail(MailSendRequest request) {
        try {
            MailSendResult skippedResult = checkSendSwitch(request);
            if (skippedResult != null) {
                return skippedResult;
            }

            checkTemplateParams(request);

            Context context = new Context(Locale.CHINA);
            Map<String, Object> variables = MapUtil.emptyIfNull(request.templateVariables());
            context.setVariables(variables);

            String htmlContent = templateEngine.process(request.templateName(), context);
            MailSendRequest htmlRequest = new MailSendRequest(
                    request.receivers(),
                    request.ccList(),
                    request.bccList(),
                    request.subject(),
                    htmlContent,
                    true,
                    request.attachments(),
                    request.templateName(),
                    request.templateVariables()
            );

            return sendMimeMail(htmlRequest, true, CollUtil.isNotEmpty(request.attachments()));
        } catch (Exception exception) {
            String stacktrace = ExceptionUtil.stacktraceToString(exception);
            log.error("模板邮件发送失败，主题：{}，原因：{}", request == null ? null : request.subject(), exception.getMessage(), exception);
            return MailSendResult.failure("模板邮件发送失败：" + exception.getMessage(), stacktrace);
        }
    }

    /**
     * 发送 MIME 邮件
     *
     * @param request   邮件发送请求
     * @param html      是否为 HTML 正文
     * @param multipart 是否为复杂邮件
     * @return 邮件发送结果
     */
    private MailSendResult sendMimeMail(MailSendRequest request, boolean html, boolean multipart) {
        try {
            MailSendResult skippedResult = checkSendSwitch(request);
            if (skippedResult != null) {
                return skippedResult;
            }

            checkBaseParams(request);
            if (multipart) {
                checkAttachments(request.attachments());
            }

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    multipart,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(buildFromAddress());
            helper.setTo(request.receivers().toArray(String[]::new));

            if (CollUtil.isNotEmpty(request.ccList())) {
                helper.setCc(request.ccList().toArray(String[]::new));
            }

            if (CollUtil.isNotEmpty(request.bccList())) {
                helper.setBcc(request.bccList().toArray(String[]::new));
            }

            helper.setSubject(request.subject());
            helper.setText(request.content(), html);

            if (multipart) {
                addAttachments(helper, request.attachments());
            }

            javaMailSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}，主题：{}", request.receivers(), request.subject());
            return MailSendResult.success("邮件发送成功");
        } catch (Exception exception) {
            String stacktrace = ExceptionUtil.stacktraceToString(exception);
            log.error("邮件发送失败，主题：{}，原因：{}", request == null ? null : request.subject(), exception.getMessage(), exception);
            return MailSendResult.failure("邮件发送失败：" + exception.getMessage(), stacktrace);
        }
    }

    /**
     * 检查发送开关
     *
     * @param request 邮件发送请求
     * @return 跳过发送结果
     */
    private MailSendResult checkSendSwitch(MailSendRequest request) {
        if (!Boolean.TRUE.equals(mailBizProperties.getEnabled())) {
            log.warn("邮件发送开关已关闭，跳过邮件发送，收件人：{}，主题：{}",
                    request == null ? null : request.receivers(),
                    request == null ? null : request.subject());
            return MailSendResult.skipped("邮件发送开关已关闭");
        }
        return null;
    }

    /**
     * 检查基础参数
     *
     * @param request 邮件发送请求
     */
    private void checkBaseParams(MailSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("邮件发送请求不能为空");
        }

        if (CollUtil.isEmpty(request.receivers())) {
            throw new IllegalArgumentException("邮件收件人不能为空");
        }

        if (StrUtil.isBlank(request.subject())) {
            throw new IllegalArgumentException("邮件主题不能为空");
        }

        if (StrUtil.isBlank(request.content())) {
            throw new IllegalArgumentException("邮件正文不能为空");
        }

        checkReceivers(request);
    }

    /**
     * 检查模板参数
     *
     * @param request 邮件发送请求
     */
    private void checkTemplateParams(MailSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("模板邮件发送请求不能为空");
        }

        if (CollUtil.isEmpty(request.receivers())) {
            throw new IllegalArgumentException("模板邮件收件人不能为空");
        }

        if (StrUtil.isBlank(request.subject())) {
            throw new IllegalArgumentException("模板邮件主题不能为空");
        }

        if (StrUtil.isBlank(request.templateName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        checkReceivers(request);
    }

    /**
     * 检查收件人参数
     *
     * @param request 邮件发送请求
     */
    private void checkReceivers(MailSendRequest request) {
        List<String> allReceivers = new ArrayList<>();
        allReceivers.addAll(CollUtil.emptyIfNull(request.receivers()));
        allReceivers.addAll(CollUtil.emptyIfNull(request.ccList()));
        allReceivers.addAll(CollUtil.emptyIfNull(request.bccList()));

        Integer maxReceiverCount = mailBizProperties.getMaxReceiverCount();
        if (maxReceiverCount != null && maxReceiverCount > 0 && allReceivers.size() > maxReceiverCount) {
            throw new IllegalArgumentException("邮件收件人数超过限制：" + maxReceiverCount);
        }

        List<String> invalidReceivers = allReceivers.stream()
                .filter(email -> !Validator.isEmail(email))
                .toList();

        if (CollUtil.isNotEmpty(invalidReceivers)) {
            throw new IllegalArgumentException("邮箱格式不正确：" + invalidReceivers);
        }

        List<String> whitelist = mailBizProperties.getTestReceiverWhitelist();
        if (CollUtil.isNotEmpty(whitelist)) {
            List<String> blockedReceivers = allReceivers.stream()
                    .filter(email -> !whitelist.contains(email))
                    .toList();

            if (CollUtil.isNotEmpty(blockedReceivers)) {
                throw new IllegalArgumentException("收件人不在测试白名单中：" + blockedReceivers);
            }
        }
    }

    /**
     * 检查附件参数
     *
     * @param attachments 附件列表
     */
    private void checkAttachments(List<MailAttachment> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            throw new IllegalArgumentException("附件邮件的附件不能为空");
        }

        Integer maxAttachmentSizeMb = mailBizProperties.getMaxAttachmentSizeMb();
        long maxAttachmentSize = maxAttachmentSizeMb == null ? 20L * 1024 * 1024 : maxAttachmentSizeMb * 1024L * 1024L;

        for (MailAttachment attachment : attachments) {
            if (attachment == null || attachment.file() == null) {
                throw new IllegalArgumentException("附件文件不能为空");
            }

            File file = attachment.file();
            if (!FileUtil.exist(file.getAbsolutePath())) {
                throw new IllegalArgumentException("附件文件不存在：" + file.getAbsolutePath());
            }

            if (FileUtil.size(file) > maxAttachmentSize) {
                throw new IllegalArgumentException("附件文件超过大小限制：" + file.getAbsolutePath());
            }
        }
    }

    /**
     * 添加附件
     *
     * @param helper      MIME 消息辅助对象
     * @param attachments 附件列表
     * @throws Exception 附件添加异常
     */
    private void addAttachments(MimeMessageHelper helper, List<MailAttachment> attachments) throws Exception {
        for (MailAttachment attachment : attachments) {
            helper.addAttachment(attachment.getDisplayFileName(), attachment.file());
        }
    }

    /**
     * 构建发件人地址
     *
     * @return 发件人地址
     * @throws Exception 地址构建异常
     */
    private InternetAddress buildFromAddress() throws Exception {
        String username = mailProperties.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("发件邮箱账号不能为空");
        }

        String fromName = StrUtil.blankToDefault(mailBizProperties.getFromName(), username);
        return new InternetAddress(username, fromName, StandardCharsets.UTF_8.name());
    }
}