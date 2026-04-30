package io.github.atengk.mail.model;

import java.time.LocalDateTime;

/**
 * 邮件模板渲染结果对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailTemplateRenderResult(
        String templateName,
        String htmlContent,
        LocalDateTime renderTime
) {

    /**
     * 创建模板渲染结果
     *
     * @param templateName 模板名称
     * @param htmlContent  HTML 内容
     * @return 模板渲染结果
     */
    public static MailTemplateRenderResult of(String templateName, String htmlContent) {
        return new MailTemplateRenderResult(templateName, htmlContent, LocalDateTime.now());
    }
}