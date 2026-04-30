package io.github.atengk.mail.model;

import jakarta.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.Map;

/**
 * 邮件预览请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailPreviewRequest(

        /**
         * 模板名称
         */
        @NotBlank(message = "模板名称不能为空")
        String templateName,

        /**
         * 模板变量
         */
        Map<String, Object> templateVariables
) {

    /**
     * 获取模板变量
     *
     * @return 模板变量
     */
    public Map<String, Object> safeTemplateVariables() {
        return templateVariables == null ? Collections.emptyMap() : templateVariables;
    }
}