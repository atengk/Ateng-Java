package io.github.atengk.mail.model;

import java.util.Map;

/**
 * 邮件模板渲染请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailTemplateRenderRequest(
        String templateName,
        Map<String, Object> variables
) {
}