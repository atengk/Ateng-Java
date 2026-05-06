package io.github.atengk.mail.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import io.github.atengk.mail.dto.MailAttachment;
import io.github.atengk.mail.dto.MailBatchSendRequest;
import io.github.atengk.mail.dto.MailContact;
import io.github.atengk.mail.dto.MailInlineResource;
import io.github.atengk.mail.dto.MailSendRequest;
import io.github.atengk.mail.dto.MailSendResult;
import io.github.atengk.mail.dto.MailSendSummary;
import io.github.atengk.mail.enums.MailContentType;
import io.github.atengk.mail.enums.MailPriority;
import io.github.atengk.mail.service.MailService;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 邮件服务默认实现。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final JavaMailSender javaMailSender;

    private final MailProperties mailProperties;

    private final Validator validator;

    private final TemplateEngine templateEngine = TemplateUtil.createEngine(
            new TemplateConfig("templates", TemplateConfig.ResourceMode.CLASSPATH)
    );

    @Qualifier("mailTaskExecutor")
    private final Executor mailTaskExecutor;

    /**
     * 根据邮件正文类型发送邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @Override
    public MailSendResult send(MailSendRequest request) {
        validate(request);

        MailContentType contentType = ObjectUtil.defaultIfNull(request.getContentType(), MailContentType.TEXT);
        return switch (contentType) {
            case TEXT -> sendText(request);
            case HTML -> sendHtml(request);
            case TEMPLATE -> sendTemplate(request);
        };
    }

    /**
     * 发送纯文本邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @Override
    public MailSendResult sendText(MailSendRequest request) {
        request.setContentType(MailContentType.TEXT);
        return doSend(request);
    }

    /**
     * 发送 HTML 邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @Override
    public MailSendResult sendHtml(MailSendRequest request) {
        request.setContentType(MailContentType.HTML);
        return doSend(request);
    }

    /**
     * 发送模板邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @Override
    public MailSendResult sendTemplate(MailSendRequest request) {
        request.setContentType(MailContentType.TEMPLATE);
        return doSend(request);
    }

    /**
     * 批量发送邮件。
     *
     * @param request 批量邮件发送请求
     * @return 批量邮件发送汇总结果
     */
    @Override
    public MailSendSummary sendBatch(MailBatchSendRequest request) {
        validateBatch(request);

        List<MailSendRequest> messages = request.getMessages();
        List<MailSendResult> results = CollUtil.newArrayList();
        int batchSize = Math.max(ObjectUtil.defaultIfNull(request.getBatchSize(), 50), 1);
        Duration batchInterval = ObjectUtil.defaultIfNull(request.getBatchInterval(), Duration.ZERO);
        boolean continueOnError = BooleanUtil.isTrue(request.getContinueOnError());

        log.info("开始批量发送邮件，批量编号：{}，总数量：{}，批次大小：{}",
                request.getBatchBizId(), messages.size(), batchSize);

        for (int start = 0; start < messages.size(); start += batchSize) {
            int end = Math.min(start + batchSize, messages.size());
            List<MailSendRequest> batchMessages = messages.subList(start, end);

            for (MailSendRequest message : batchMessages) {
                MailSendResult result;
                try {
                    result = send(message);
                } catch (Exception ex) {
                    result = buildFailureResult(message, ex);
                    log.error("批量邮件发送异常，业务编号：{}，原因：{}", message.getBizId(), ex.getMessage(), ex);
                }

                results.add(result);

                if (BooleanUtil.isFalse(result.getSuccess()) && !continueOnError) {
                    log.warn("批量邮件发送中断，批量编号：{}，失败业务编号：{}",
                            request.getBatchBizId(), message.getBizId());
                    return buildSummary(request.getBatchBizId(), messages.size(), results);
                }
            }

            if (end < messages.size() && !batchInterval.isZero() && !batchInterval.isNegative()) {
                ThreadUtil.safeSleep(batchInterval.toMillis());
            }
        }

        MailSendSummary summary = buildSummary(request.getBatchBizId(), messages.size(), results);
        log.info("批量邮件发送完成，批量编号：{}，成功：{}，失败：{}",
                summary.getBatchBizId(), summary.getSuccessCount(), summary.getFailureCount());
        return summary;
    }

    /**
     * 异步发送单封邮件。
     *
     * @param request 邮件发送请求
     * @return 异步邮件发送结果
     */
    @Override
    public CompletableFuture<MailSendResult> sendAsync(MailSendRequest request) {
        return CompletableFuture.supplyAsync(() -> send(request), mailTaskExecutor);
    }

    /**
     * 使用指定线程池异步发送单封邮件。
     *
     * @param request  邮件发送请求
     * @param executor 指定线程池
     * @return 异步邮件发送结果
     */
    @Override
    public CompletableFuture<MailSendResult> sendAsync(MailSendRequest request, Executor executor) {
        Executor actualExecutor = ObjectUtil.defaultIfNull(executor, mailTaskExecutor);
        return CompletableFuture.supplyAsync(() -> send(request), actualExecutor);
    }

    /**
     * 异步批量发送邮件。
     *
     * @param request 批量邮件发送请求
     * @return 异步批量邮件发送汇总结果
     */
    @Override
    public CompletableFuture<MailSendSummary> sendBatchAsync(MailBatchSendRequest request) {
        return CompletableFuture.supplyAsync(() -> sendBatch(request), mailTaskExecutor);
    }

    /**
     * 使用指定线程池异步批量发送邮件。
     *
     * @param request  批量邮件发送请求
     * @param executor 指定线程池
     * @return 异步批量邮件发送汇总结果
     */
    @Override
    public CompletableFuture<MailSendSummary> sendBatchAsync(MailBatchSendRequest request, Executor executor) {
        Executor actualExecutor = ObjectUtil.defaultIfNull(executor, mailTaskExecutor);
        return CompletableFuture.supplyAsync(() -> sendBatch(request), actualExecutor);
    }

    /**
     * 渲染邮件模板。
     *
     * @param templateName 模板名称
     * @param variables    模板变量
     * @return 渲染后的邮件正文
     */
    @Override
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        if (StrUtil.isBlank(templateName)) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        Template template = templateEngine.getTemplate(templateName);
        return template.render(MapUtil.emptyIfNull(variables));
    }

    /**
     * 校验邮件发送请求。
     *
     * @param request 邮件发送请求
     */
    @Override
    public void validate(MailSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("邮件发送请求不能为空");
        }

        Set<ConstraintViolation<MailSendRequest>> violations = validator.validate(request);
        if (CollUtil.isNotEmpty(violations)) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .reduce((left, right) -> left + "；" + right)
                    .orElse("邮件发送请求参数不合法");
            throw new IllegalArgumentException(message);
        }

        MailContentType contentType = ObjectUtil.defaultIfNull(request.getContentType(), MailContentType.TEXT);
        if (contentType == MailContentType.TEMPLATE) {
            if (StrUtil.isBlank(request.getTemplateName())) {
                throw new IllegalArgumentException("模板邮件的模板名称不能为空");
            }
        } else if (StrUtil.isBlank(request.getContent())) {
            throw new IllegalArgumentException("邮件正文不能为空");
        }

        validateAttachments(request.getAttachments());
        validateInlineResources(request.getInlineResources());
    }

    /**
     * 检查邮件服务器连接状态。
     *
     * @return true 表示连接正常，false 表示连接异常
     */
    @Override
    public boolean checkConnection() {
        try {
            if (javaMailSender instanceof JavaMailSenderImpl sender) {
                sender.testConnection();
            } else {
                javaMailSender.createMimeMessage();
            }
            log.info("邮件服务器连接检查通过");
            return true;
        } catch (Exception ex) {
            log.warn("邮件服务器连接检查失败，原因：{}", ex.getMessage());
            return false;
        }
    }

    /**
     * 执行邮件发送。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    private MailSendResult doSend(MailSendRequest request) {
        try {
            validate(request);

            JavaMailSender actualMailSender = resolveMailSender(request);
            MimeMessage mimeMessage = actualMailSender.createMimeMessage();
            String charset = resolveCharset(request);
            boolean multipart = CollUtil.isNotEmpty(request.getAttachments())
                    || CollUtil.isNotEmpty(request.getInlineResources());

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, charset);
            fillBasicMessage(helper, mimeMessage, request, charset);
            addAttachments(helper, request.getAttachments());
            addInlineResources(helper, request.getInlineResources());

            actualMailSender.send(mimeMessage);

            String messageId = ArrayUtil.get(mimeMessage.getHeader("Message-ID"), 0);
            MailSendResult result = MailSendResult.builder()
                    .bizId(request.getBizId())
                    .messageId(messageId)
                    .subject(request.getSubject())
                    .recipients(toAddressList(request.getTo()))
                    .success(true)
                    .sendTime(LocalDateTime.now())
                    .build();

            log.info("邮件发送成功，业务编号：{}，主题：{}，收件人数：{}",
                    request.getBizId(), request.getSubject(), result.getRecipients().size());
            return result;
        } catch (Exception ex) {
            MailSendResult result = buildFailureResult(request, ex);
            log.error("邮件发送失败，业务编号：{}，主题：{}，原因：{}",
                    request == null ? null : request.getBizId(),
                    request == null ? null : request.getSubject(),
                    ex.getMessage(),
                    ex);

            if (request != null && BooleanUtil.isTrue(request.getThrowException())) {
                throw new IllegalStateException("邮件发送失败：" + ex.getMessage(), ex);
            }
            return result;
        }
    }

    /**
     * 填充邮件基础信息。
     *
     * @param helper      邮件助手
     * @param mimeMessage MIME 消息
     * @param request     邮件发送请求
     * @param charset     字符编码
     */
    private void fillBasicMessage(MimeMessageHelper helper,
                                  MimeMessage mimeMessage,
                                  MailSendRequest request,
                                  String charset) throws Exception {
        fillFrom(helper, request, charset);

        helper.setTo(toInternetAddresses(request.getTo(), charset));

        if (CollUtil.isNotEmpty(request.getCc())) {
            helper.setCc(toInternetAddresses(request.getCc(), charset));
        }
        if (CollUtil.isNotEmpty(request.getBcc())) {
            helper.setBcc(toInternetAddresses(request.getBcc(), charset));
        }
        if (CollUtil.isNotEmpty(request.getReplyTo())) {
            mimeMessage.setReplyTo(toInternetAddresses(request.getReplyTo(), charset));
        }

        helper.setSubject(StrUtil.nullToEmpty(request.getSubject()));
        helper.setSentDate(new Date());

        String content = resolveContent(request);
        boolean html = request.getContentType() == MailContentType.HTML
                || request.getContentType() == MailContentType.TEMPLATE;
        helper.setText(StrUtil.nullToEmpty(content), html);

        fillPriorityHeader(mimeMessage, request.getPriority());
        fillCustomHeaders(mimeMessage, request.getHeaders());
    }

    /**
     * 填充发件人。
     *
     * <p>
     * 不传 from.mailConfig 时使用 spring.mail 默认配置；传入 from.mailConfig 时使用本次请求的动态 SMTP 配置。
     * from.address 不传时默认取当前 SMTP 配置的 username。
     * </p>
     *
     * @param helper  邮件助手
     * @param request 邮件发送请求
     * @param charset 字符编码
     */
    private void fillFrom(MimeMessageHelper helper, MailSendRequest request, String charset) throws Exception {
        Object from = request.getFrom();
        Object dynamicConfig = getDynamicMailConfig(request);

        String username = dynamicConfig == null ? mailProperties.getUsername() : getStringProperty(dynamicConfig, "username");
        String actualAddress = from == null ? username : StrUtil.blankToDefault(getStringProperty(from, "address"), username);
        String actualName = from == null ? null : getStringProperty(from, "name");

        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("SMTP 认证账号不能为空，请检查 spring.mail.username 或 from.mailConfig.username");
        }
        if (StrUtil.isBlank(actualAddress)) {
            throw new IllegalArgumentException("发件邮箱不能为空");
        }

        boolean forceSameAsUsername = dynamicConfig == null
                || ObjectUtil.defaultIfNull(getBooleanProperty(dynamicConfig, "forceFromSameAsUsername"), true);
        if (forceSameAsUsername && !StrUtil.equalsIgnoreCase(actualAddress, username)) {
            throw new IllegalArgumentException(
                    StrUtil.format("发件邮箱必须和当前 SMTP 认证账号一致，from.address={}，username={}", actualAddress, username)
            );
        }

        if (StrUtil.isBlank(actualName)) {
            helper.setFrom(new InternetAddress(actualAddress));
            return;
        }

        helper.setFrom(new InternetAddress(actualAddress, actualName, charset));
    }

    /**
     * 解析邮件发送器。
     *
     * @param request 邮件发送请求
     * @return 邮件发送器
     */
    private JavaMailSender resolveMailSender(MailSendRequest request) {
        Object dynamicConfig = getDynamicMailConfig(request);
        if (dynamicConfig == null) {
            return javaMailSender;
        }

        validateDynamicMailConfig(dynamicConfig);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(getStringProperty(dynamicConfig, "host"));
        sender.setPort(ObjectUtil.defaultIfNull(getIntegerProperty(dynamicConfig, "port"), 25));
        sender.setUsername(getStringProperty(dynamicConfig, "username"));
        sender.setPassword(getStringProperty(dynamicConfig, "password"));
        sender.setDefaultEncoding(StrUtil.blankToDefault(getStringProperty(dynamicConfig, "defaultEncoding"), CharsetUtil.UTF_8));
        sender.setJavaMailProperties(buildJavaMailProperties(dynamicConfig));

        log.info("使用动态 SMTP 配置发送邮件，host={}，port={}，username={}",
                getStringProperty(dynamicConfig, "host"),
                getIntegerProperty(dynamicConfig, "port"),
                getStringProperty(dynamicConfig, "username"));
        return sender;
    }

    /**
     * 解析邮件编码。
     *
     * @param request 邮件发送请求
     * @return 邮件编码
     */
    private String resolveCharset(MailSendRequest request) {
        if (StrUtil.isNotBlank(request.getCharset())) {
            return request.getCharset();
        }

        Object dynamicConfig = getDynamicMailConfig(request);
        if (dynamicConfig != null && StrUtil.isNotBlank(getStringProperty(dynamicConfig, "defaultEncoding"))) {
            return getStringProperty(dynamicConfig, "defaultEncoding");
        }

        if (mailProperties.getDefaultEncoding() != null) {
            return mailProperties.getDefaultEncoding().name();
        }
        return CharsetUtil.UTF_8;
    }

    /**
     * 获取动态 SMTP 配置。
     *
     * @param request 邮件发送请求
     * @return 动态 SMTP 配置
     */
    private Object getDynamicMailConfig(MailSendRequest request) {
        if (request == null || request.getFrom() == null) {
            return null;
        }
        return readProperty(request.getFrom(), "mailConfig");
    }

    /**
     * 校验动态 SMTP 配置。
     *
     * @param dynamicConfig 动态 SMTP 配置
     */
    private void validateDynamicMailConfig(Object dynamicConfig) {
        if (StrUtil.isBlank(getStringProperty(dynamicConfig, "host"))) {
            throw new IllegalArgumentException("动态 SMTP 配置 host 不能为空");
        }
        if (ObjectUtil.isNull(getIntegerProperty(dynamicConfig, "port"))) {
            throw new IllegalArgumentException("动态 SMTP 配置 port 不能为空");
        }
        if (StrUtil.isBlank(getStringProperty(dynamicConfig, "username"))) {
            throw new IllegalArgumentException("动态 SMTP 配置 username 不能为空");
        }
        if (StrUtil.isBlank(getStringProperty(dynamicConfig, "password"))) {
            throw new IllegalArgumentException("动态 SMTP 配置 password 不能为空");
        }
    }

    /**
     * 构建 JavaMail 属性。
     *
     * @param dynamicConfig 动态 SMTP 配置
     * @return JavaMail 属性
     */
    private Properties buildJavaMailProperties(Object dynamicConfig) {
        Properties properties = new Properties();

        properties.put("mail.smtp.auth", String.valueOf(ObjectUtil.defaultIfNull(getBooleanProperty(dynamicConfig, "auth"), true)));
        putIfNotNull(properties, "mail.smtp.starttls.enable", getBooleanProperty(dynamicConfig, "starttlsEnable"));
        putIfNotNull(properties, "mail.smtp.starttls.required", getBooleanProperty(dynamicConfig, "starttlsRequired"));
        putIfNotNull(properties, "mail.smtp.ssl.enable", getBooleanProperty(dynamicConfig, "sslEnable"));
        putIfNotNull(properties, "mail.smtp.connectiontimeout", getIntegerProperty(dynamicConfig, "connectionTimeout"));
        putIfNotNull(properties, "mail.smtp.timeout", getIntegerProperty(dynamicConfig, "timeout"));
        putIfNotNull(properties, "mail.smtp.writetimeout", getIntegerProperty(dynamicConfig, "writeTimeout"));

        Object extraProperties = readProperty(dynamicConfig, "properties");
        if (extraProperties instanceof Properties mailProperties) {
            properties.putAll(mailProperties);
        } else if (extraProperties instanceof Map<?, ?> mailPropertiesMap) {
            mailPropertiesMap.forEach((key, value) -> {
                if (key != null && value != null) {
                    properties.put(String.valueOf(key), String.valueOf(value));
                }
            });
        }

        return properties;
    }

    /**
     * 设置非空 JavaMail 属性。
     *
     * @param properties 属性集合
     * @param key        属性键
     * @param value      属性值
     */
    private void putIfNotNull(Properties properties, String key, Object value) {
        if (ObjectUtil.isNotNull(value)) {
            properties.put(key, String.valueOf(value));
        }
    }

    /**
     * 读取字符串属性。
     *
     * @param target   目标对象
     * @param property 属性名称
     * @return 字符串属性值
     */
    private String getStringProperty(Object target, String property) {
        Object value = readProperty(target, property);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取整数属性。
     *
     * @param target   目标对象
     * @param property 属性名称
     * @return 整数属性值
     */
    private Integer getIntegerProperty(Object target, String property) {
        Object value = readProperty(target, property);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value);
        return StrUtil.isBlank(text) ? null : Integer.valueOf(text);
    }

    /**
     * 读取布尔属性。
     *
     * @param target   目标对象
     * @param property 属性名称
     * @return 布尔属性值
     */
    private Boolean getBooleanProperty(Object target, String property) {
        Object value = readProperty(target, property);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value);
        return StrUtil.isBlank(text) ? null : Boolean.valueOf(text);
    }

    /**
     * 读取对象属性。
     *
     * @param target   目标对象
     * @param property 属性名称
     * @return 属性值
     */
    private Object readProperty(Object target, String property) {
        if (target == null || StrUtil.isBlank(property)) {
            return null;
        }

        String suffix = StrUtil.upperFirst(property);
        Object value = invokeGetter(target, "get" + suffix);
        if (value != null) {
            return value;
        }

        value = invokeGetter(target, "is" + suffix);
        if (value != null) {
            return value;
        }

        try {
            Field field = target.getClass().getDeclaredField(property);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 调用 Getter 方法。
     *
     * @param target     目标对象
     * @param methodName 方法名称
     * @return Getter 返回值
     */
    private Object invokeGetter(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 解析邮件正文。
     *
     * @param request 邮件发送请求
     * @return 邮件正文
     */
    private String resolveContent(MailSendRequest request) {
        if (request.getContentType() == MailContentType.TEMPLATE) {
            return renderTemplate(request.getTemplateName(), request.getTemplateVariables());
        }
        return request.getContent();
    }

    /**
     * 添加附件。
     *
     * @param helper      邮件助手
     * @param attachments 附件列表
     */
    private void addAttachments(MimeMessageHelper helper, List<MailAttachment> attachments) throws Exception {
        if (CollUtil.isEmpty(attachments)) {
            return;
        }

        for (MailAttachment attachment : attachments) {
            String fileName = attachment.getFileName();
            String contentType = StrUtil.blankToDefault(attachment.getContentType(), DEFAULT_CONTENT_TYPE);

            if (StrUtil.isNotBlank(attachment.getFilePath())) {
                helper.addAttachment(fileName, new FileSystemResource(FileUtil.file(attachment.getFilePath())), contentType);
            } else {
                helper.addAttachment(fileName, buildByteArrayResource(attachment.getContent(), fileName), contentType);
            }
        }
    }

    /**
     * 添加 HTML 内嵌资源。
     *
     * @param helper          邮件助手
     * @param inlineResources 内嵌资源列表
     */
    private void addInlineResources(MimeMessageHelper helper, List<MailInlineResource> inlineResources) throws Exception {
        if (CollUtil.isEmpty(inlineResources)) {
            return;
        }

        for (MailInlineResource resource : inlineResources) {
            String contentType = StrUtil.blankToDefault(resource.getContentType(), DEFAULT_CONTENT_TYPE);

            if (StrUtil.isNotBlank(resource.getFilePath())) {
                helper.addInline(resource.getContentId(), new FileSystemResource(FileUtil.file(resource.getFilePath())), contentType);
            } else {
                InputStreamSource source = buildByteArrayResource(resource.getContent(), resource.getName());
                helper.addInline(resource.getContentId(), source, contentType);
            }
        }
    }

    /**
     * 填充邮件优先级请求头。
     *
     * @param mimeMessage MIME 消息
     * @param priority    邮件优先级
     */
    private void fillPriorityHeader(MimeMessage mimeMessage, MailPriority priority) throws Exception {
        MailPriority mailPriority = ObjectUtil.defaultIfNull(priority, MailPriority.NORMAL);
        switch (mailPriority) {
            case HIGH -> {
                mimeMessage.setHeader("X-Priority", "1");
                mimeMessage.setHeader("Importance", "High");
            }
            case LOW -> {
                mimeMessage.setHeader("X-Priority", "5");
                mimeMessage.setHeader("Importance", "Low");
            }
            case NORMAL -> {
                mimeMessage.setHeader("X-Priority", "3");
                mimeMessage.setHeader("Importance", "Normal");
            }
        }
    }

    /**
     * 填充自定义请求头。
     *
     * @param mimeMessage MIME 消息
     * @param headers     自定义请求头
     */
    private void fillCustomHeaders(MimeMessage mimeMessage, Map<String, String> headers) throws Exception {
        if (MapUtil.isEmpty(headers)) {
            return;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (StrUtil.isNotBlank(entry.getKey()) && StrUtil.isNotBlank(entry.getValue())) {
                mimeMessage.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 校验批量请求。
     *
     * @param request 批量邮件发送请求
     */
    private void validateBatch(MailBatchSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("批量邮件发送请求不能为空");
        }

        Set<ConstraintViolation<MailBatchSendRequest>> violations = validator.validate(request);
        if (CollUtil.isNotEmpty(violations)) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .reduce((left, right) -> left + "；" + right)
                    .orElse("批量邮件发送请求参数不合法");
            throw new IllegalArgumentException(message);
        }

        if (CollUtil.isEmpty(request.getMessages())) {
            throw new IllegalArgumentException("批量邮件列表不能为空");
        }
    }

    /**
     * 校验附件参数。
     *
     * @param attachments 附件列表
     */
    private void validateAttachments(List<MailAttachment> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return;
        }

        for (MailAttachment attachment : attachments) {
            if (attachment == null) {
                throw new IllegalArgumentException("附件信息不能为空");
            }
            if (StrUtil.isBlank(attachment.getFileName())) {
                throw new IllegalArgumentException("附件文件名不能为空");
            }
            if (StrUtil.isBlank(attachment.getFilePath()) && ArrayUtil.isEmpty(attachment.getContent())) {
                throw new IllegalArgumentException("附件文件路径和字节内容不能同时为空");
            }
            if (StrUtil.isNotBlank(attachment.getFilePath()) && !FileUtil.exist(attachment.getFilePath())) {
                throw new IllegalArgumentException("附件文件不存在：" + attachment.getFilePath());
            }
        }
    }

    /**
     * 校验内嵌资源参数。
     *
     * @param inlineResources 内嵌资源列表
     */
    private void validateInlineResources(List<MailInlineResource> inlineResources) {
        if (CollUtil.isEmpty(inlineResources)) {
            return;
        }

        for (MailInlineResource resource : inlineResources) {
            if (resource == null) {
                throw new IllegalArgumentException("内嵌资源信息不能为空");
            }
            if (StrUtil.isBlank(resource.getContentId())) {
                throw new IllegalArgumentException("内嵌资源 contentId 不能为空");
            }
            if (StrUtil.isBlank(resource.getFilePath()) && ArrayUtil.isEmpty(resource.getContent())) {
                throw new IllegalArgumentException("内嵌资源文件路径和字节内容不能同时为空");
            }
            if (StrUtil.isNotBlank(resource.getFilePath()) && !FileUtil.exist(resource.getFilePath())) {
                throw new IllegalArgumentException("内嵌资源文件不存在：" + resource.getFilePath());
            }
        }
    }

    /**
     * 构建批量发送汇总结果。
     *
     * @param batchBizId 批量业务编号
     * @param totalCount 总数量
     * @param results    明细结果
     * @return 批量发送汇总结果
     */
    private MailSendSummary buildSummary(String batchBizId, int totalCount, List<MailSendResult> results) {
        int successCount = (int) results.stream()
                .filter(result -> BooleanUtil.isTrue(result.getSuccess()))
                .count();

        return MailSendSummary.builder()
                .batchBizId(batchBizId)
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(results.size() - successCount)
                .results(results)
                .finishTime(LocalDateTime.now())
                .build();
    }

    /**
     * 构建失败结果。
     *
     * @param request 邮件发送请求
     * @param ex      异常
     * @return 失败结果
     */
    private MailSendResult buildFailureResult(MailSendRequest request, Exception ex) {
        return MailSendResult.builder()
                .bizId(request == null ? null : request.getBizId())
                .subject(request == null ? null : request.getSubject())
                .recipients(request == null ? CollUtil.newArrayList() : toAddressList(request.getTo()))
                .success(false)
                .errorCode(ex.getClass().getSimpleName())
                .errorMessage(ExceptionUtil.getRootCauseMessage(ex))
                .sendTime(LocalDateTime.now())
                .build();
    }

    /**
     * 转换联系人地址列表。
     *
     * @param contacts 联系人列表
     * @return 邮箱地址列表
     */
    private List<String> toAddressList(List<MailContact> contacts) {
        if (CollUtil.isEmpty(contacts)) {
            return CollUtil.newArrayList();
        }

        return contacts.stream()
                .filter(ObjectUtil::isNotNull)
                .map(MailContact::getAddress)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    /**
     * 转换联系人为 InternetAddress 数组。
     *
     * @param contacts 联系人列表
     * @param charset  字符编码
     * @return InternetAddress 数组
     */
    private InternetAddress[] toInternetAddresses(List<MailContact> contacts, String charset) {
        if (CollUtil.isEmpty(contacts)) {
            return new InternetAddress[0];
        }

        return contacts.stream()
                .filter(ObjectUtil::isNotNull)
                .filter(contact -> StrUtil.isNotBlank(contact.getAddress()))
                .map(contact -> toInternetAddress(contact, charset))
                .toArray(InternetAddress[]::new);
    }

    /**
     * 转换联系人为 InternetAddress。
     *
     * @param contact 联系人
     * @param charset 字符编码
     * @return InternetAddress
     */
    private InternetAddress toInternetAddress(MailContact contact, String charset) {
        try {
            if (StrUtil.isBlank(contact.getName())) {
                return new InternetAddress(contact.getAddress());
            }
            return new InternetAddress(contact.getAddress(), contact.getName(), charset);
        } catch (AddressException | UnsupportedEncodingException ex) {
            throw new IllegalArgumentException("邮件联系人编码失败：" + contact.getAddress(), ex);
        }
    }

    /**
     * 构建字节资源。
     *
     * @param content  字节内容
     * @param fileName 文件名
     * @return 字节资源
     */
    private ByteArrayResource buildByteArrayResource(byte[] content, String fileName) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }
}