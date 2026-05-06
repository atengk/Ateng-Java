package io.github.atengk.mail.dto;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import io.github.atengk.mail.enums.MailContentType;
import io.github.atengk.mail.enums.MailPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 单封邮件发送请求。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务编号，用于外部系统追踪邮件发送结果。
     */
    private String bizId;

    /**
     * 发件人。
     *
     * <p>
     * 不传时使用 spring.mail 默认配置。
     * 如果传入 from.mailConfig，则本次邮件使用该动态 SMTP 配置发送。
     * </p>
     */
    private MailFrom from;

    /**
     * 收件人列表。
     */
    @Valid
    @NotEmpty(message = "收件人不能为空")
    @Builder.Default
    private List<MailContact> to = CollUtil.newArrayList();

    /**
     * 抄送人列表。
     */
    @Valid
    @Builder.Default
    private List<MailContact> cc = CollUtil.newArrayList();

    /**
     * 密送人列表。
     */
    @Valid
    @Builder.Default
    private List<MailContact> bcc = CollUtil.newArrayList();

    /**
     * 回复地址列表。
     */
    @Valid
    @Builder.Default
    private List<MailContact> replyTo = CollUtil.newArrayList();

    /**
     * 邮件主题。
     */
    @NotBlank(message = "邮件主题不能为空")
    private String subject;

    /**
     * 邮件正文。文本和 HTML 邮件直接使用该字段，模板邮件可存放渲染后的内容。
     */
    private String content;

    /**
     * 邮件正文类型。
     */
    @Builder.Default
    private MailContentType contentType = MailContentType.TEXT;

    /**
     * 模板名称，例如 notice.ftl、mail/order.html。
     */
    private String templateName;

    /**
     * 模板变量。
     */
    @Builder.Default
    private Map<String, Object> templateVariables = MapUtil.newHashMap();

    /**
     * 邮件附件列表。
     */
    @Valid
    @Builder.Default
    private List<MailAttachment> attachments = CollUtil.newArrayList();

    /**
     * HTML 内嵌资源列表，例如内嵌图片。
     */
    @Valid
    @Builder.Default
    private List<MailInlineResource> inlineResources = CollUtil.newArrayList();

    /**
     * 自定义邮件头。
     */
    @Builder.Default
    private Map<String, String> headers = MapUtil.newHashMap();

    /**
     * 邮件优先级。
     */
    @Builder.Default
    private MailPriority priority = MailPriority.NORMAL;

    /**
     * 字符编码。
     */
    @Builder.Default
    private String charset = CharsetUtil.UTF_8;

    /**
     * 发送失败时是否抛出异常，false 时由结果对象承载失败信息。
     */
    @Builder.Default
    private Boolean throwException = false;
}
