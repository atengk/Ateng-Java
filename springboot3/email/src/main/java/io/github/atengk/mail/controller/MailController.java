package io.github.atengk.mail.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.ApiResult;
import io.github.atengk.mail.model.MailPreviewRequest;
import io.github.atengk.mail.model.MailRecordQueryRequest;
import io.github.atengk.mail.model.MailRecordVO;
import io.github.atengk.mail.model.MailSendApiRequest;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import io.github.atengk.mail.model.MailSendType;
import io.github.atengk.mail.model.PageResult;
import io.github.atengk.mail.service.MailRecordQueryService;
import io.github.atengk.mail.service.MailTemplateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 邮件接口控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Validated
@RestController
@RequestMapping("/api/mail")
public class MailController {

    private static final Logger log = LoggerFactory.getLogger(MailController.class);

    private final MailSendClient mailSendClient;
    private final MailTemplateService mailTemplateService;
    private final MailRecordQueryService mailRecordQueryService;

    public MailController(MailSendClient mailSendClient,
                          MailTemplateService mailTemplateService,
                          MailRecordQueryService mailRecordQueryService) {
        this.mailSendClient = mailSendClient;
        this.mailTemplateService = mailTemplateService;
        this.mailRecordQueryService = mailRecordQueryService;
    }

    /**
     * 发送邮件
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send")
    public ApiResult<MailSendResult> send(@Valid @RequestBody MailSendApiRequest request) {
        log.info("收到邮件发送请求，类型：{}，收件人：{}，主题：{}",
                request.type(), request.receivers(), request.subject());

        MailSendResult result = switch (request.type()) {
            case TEXT -> sendTextMail(request);
            case HTML -> sendHtmlMail(request);
            case TEMPLATE -> sendTemplateMail(request);
        };

        if (result.success() || result.skipped()) {
            return ApiResult.success(result);
        }

        return ApiResult.failure(result.message());
    }

    /**
     * 预览邮件内容
     *
     * @param request 邮件预览请求
     * @return HTML 内容
     */
    @PostMapping("/preview")
    public ApiResult<String> preview(@Valid @RequestBody MailPreviewRequest request) {
        log.info("收到邮件预览请求，模板名称：{}", request.templateName());
        String htmlContent = mailTemplateService.renderHtml(request.templateName(), request.safeTemplateVariables());
        return ApiResult.success(htmlContent);
    }

    /**
     * 分页查询邮件发送记录
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/records/page")
    public ApiResult<PageResult<MailRecordVO>> pageRecords(@Valid @RequestBody MailRecordQueryRequest request) {
        log.info("收到邮件发送记录查询请求，页码：{}，每页条数：{}，收件人：{}，状态：{}",
                request.safePageNum(), request.safePageSize(), request.receiver(), request.status());

        PageResult<MailRecordVO> pageResult = mailRecordQueryService.page(request);
        return ApiResult.success(pageResult);
    }

    /**
     * 发送普通文本邮件
     *
     * @param request 邮件发送接口请求
     * @return 邮件发送结果
     */
    private MailSendResult sendTextMail(MailSendApiRequest request) {
        if (StrUtil.isBlank(request.content())) {
            throw new IllegalArgumentException("普通文本邮件正文不能为空");
        }

        MailSendRequest sendRequest = new MailSendRequest(
                request.receivers(),
                request.safeCcList(),
                request.safeBccList(),
                request.subject(),
                request.content(),
                false,
                null,
                null,
                null
        );

        return mailSendClient.sendTextMail(sendRequest);
    }

    /**
     * 发送 HTML 邮件
     *
     * @param request 邮件发送接口请求
     * @return 邮件发送结果
     */
    private MailSendResult sendHtmlMail(MailSendApiRequest request) {
        if (StrUtil.isBlank(request.content())) {
            throw new IllegalArgumentException("HTML 邮件正文不能为空");
        }

        MailSendRequest sendRequest = new MailSendRequest(
                request.receivers(),
                request.safeCcList(),
                request.safeBccList(),
                request.subject(),
                request.content(),
                true,
                null,
                null,
                null
        );

        return mailSendClient.sendHtmlMail(sendRequest);
    }

    /**
     * 发送模板邮件
     *
     * @param request 邮件发送接口请求
     * @return 邮件发送结果
     */
    private MailSendResult sendTemplateMail(MailSendApiRequest request) {
        if (StrUtil.isBlank(request.templateName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        MailSendRequest sendRequest = new MailSendRequest(
                request.receivers(),
                request.safeCcList(),
                request.safeBccList(),
                request.subject(),
                null,
                true,
                null,
                request.templateName(),
                request.safeTemplateVariables()
        );

        return mailSendClient.sendTemplateMail(sendRequest);
    }
}