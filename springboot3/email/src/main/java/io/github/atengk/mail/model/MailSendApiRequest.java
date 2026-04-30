package io.github.atengk.mail.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送接口请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailSendApiRequest(

        /**
         * 收件人列表
         */
        @NotEmpty(message = "收件人不能为空")
        List<@Email(message = "收件人邮箱格式不正确") String> receivers,

        /**
         * 抄送人列表
         */
        List<@Email(message = "抄送人邮箱格式不正确") String> ccList,

        /**
         * 密送人列表
         */
        List<@Email(message = "密送人邮箱格式不正确") String> bccList,

        /**
         * 邮件主题
         */
        @NotBlank(message = "邮件主题不能为空")
        String subject,

        /**
         * 邮件类型
         */
        @NotNull(message = "邮件类型不能为空")
        MailSendType type,

        /**
         * 邮件正文，TEXT 和 HTML 类型必填
         */
        String content,

        /**
         * 模板名称，TEMPLATE 类型必填
         */
        String templateName,

        /**
         * 模板变量
         */
        Map<String, Object> templateVariables
) {

    /**
     * 获取抄送人列表
     *
     * @return 抄送人列表
     */
    public List<String> safeCcList() {
        return ccList == null ? Collections.emptyList() : ccList;
    }

    /**
     * 获取密送人列表
     *
     * @return 密送人列表
     */
    public List<String> safeBccList() {
        return bccList == null ? Collections.emptyList() : bccList;
    }

    /**
     * 获取模板变量
     *
     * @return 模板变量
     */
    public Map<String, Object> safeTemplateVariables() {
        return templateVariables == null ? Collections.emptyMap() : templateVariables;
    }
}