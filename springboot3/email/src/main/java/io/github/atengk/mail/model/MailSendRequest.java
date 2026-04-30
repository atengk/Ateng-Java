package io.github.atengk.mail.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailSendRequest(
        List<String> receivers,
        List<String> ccList,
        List<String> bccList,
        String subject,
        String content,
        boolean html,
        List<MailAttachment> attachments,
        String templateName,
        Map<String, Object> templateVariables
) {

    /**
     * 创建普通文本邮件请求
     *
     * @param receivers 收件人列表
     * @param subject   邮件主题
     * @param content   邮件正文
     * @return 邮件发送请求
     */
    public static MailSendRequest text(List<String> receivers, String subject, String content) {
        return new MailSendRequest(
                receivers,
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                content,
                false,
                Collections.emptyList(),
                null,
                Collections.emptyMap()
        );
    }

    /**
     * 创建 HTML 邮件请求
     *
     * @param receivers 收件人列表
     * @param subject   邮件主题
     * @param content   HTML 正文
     * @return 邮件发送请求
     */
    public static MailSendRequest html(List<String> receivers, String subject, String content) {
        return new MailSendRequest(
                receivers,
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                content,
                true,
                Collections.emptyList(),
                null,
                Collections.emptyMap()
        );
    }

    /**
     * 创建附件邮件请求
     *
     * @param receivers   收件人列表
     * @param subject     邮件主题
     * @param content     邮件正文
     * @param html        是否为 HTML 正文
     * @param attachments 附件列表
     * @return 邮件发送请求
     */
    public static MailSendRequest attachment(List<String> receivers,
                                             String subject,
                                             String content,
                                             boolean html,
                                             List<MailAttachment> attachments) {
        return new MailSendRequest(
                receivers,
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                content,
                html,
                attachments,
                null,
                Collections.emptyMap()
        );
    }

    /**
     * 创建模板邮件请求
     *
     * @param receivers         收件人列表
     * @param subject           邮件主题
     * @param templateName      模板名称
     * @param templateVariables 模板变量
     * @return 邮件发送请求
     */
    public static MailSendRequest template(List<String> receivers,
                                           String subject,
                                           String templateName,
                                           Map<String, Object> templateVariables) {
        return new MailSendRequest(
                receivers,
                Collections.emptyList(),
                Collections.emptyList(),
                subject,
                null,
                true,
                Collections.emptyList(),
                templateName,
                templateVariables
        );
    }
}