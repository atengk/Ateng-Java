package io.github.atengk.mail.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.model.MailTemplateRenderRequest;
import io.github.atengk.mail.model.MailTemplateRenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

/**
 * 邮件模板服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(MailTemplateService.class);

    private final TemplateEngine templateEngine;

    public MailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * 渲染邮件模板
     *
     * @param request 模板渲染请求
     * @return 模板渲染结果
     */
    public MailTemplateRenderResult render(MailTemplateRenderRequest request) {
        checkRenderRequest(request);

        Map<String, Object> variables = MapUtil.emptyIfNull(request.variables());
        Context context = new Context(Locale.CHINA);
        context.setVariables(variables);

        String htmlContent = templateEngine.process(request.templateName(), context);
        log.info("邮件模板渲染完成，模板名称：{}，变量数量：{}", request.templateName(), variables.size());

        return MailTemplateRenderResult.of(request.templateName(), htmlContent);
    }

    /**
     * 根据模板名称和变量渲染 HTML 内容
     *
     * @param templateName 模板名称
     * @param variables    模板变量
     * @return HTML 内容
     */
    public String renderHtml(String templateName, Map<String, Object> variables) {
        MailTemplateRenderRequest request = new MailTemplateRenderRequest(templateName, variables);
        return render(request).htmlContent();
    }

    /**
     * 校验模板渲染请求
     *
     * @param request 模板渲染请求
     */
    private void checkRenderRequest(MailTemplateRenderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("模板渲染请求不能为空");
        }

        if (StrUtil.isBlank(request.templateName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
    }
}