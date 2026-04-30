package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 邮件内容组装服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailContentAssembleService {

    private static final Logger log = LoggerFactory.getLogger(MailContentAssembleService.class);

    private final MailTemplateService mailTemplateService;
    private final MailSendClient mailSendClient;

    public MailContentAssembleService(MailTemplateService mailTemplateService,
                                      MailSendClient mailSendClient) {
        this.mailTemplateService = mailTemplateService;
        this.mailSendClient = mailSendClient;
    }

    /**
     * 组装并发送验证码邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendVerifyCodeMail(String receiver) {
        String code = RandomUtil.randomNumbers(6);

        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("productName", "业务管理系统")
                .put("code", code)
                .put("expireMinutes", 5)
                .build();

        String htmlContent = mailTemplateService.renderHtml("mail/verify-code", variables);

        MailSendRequest request = MailSendRequest.html(
                ListUtil.of(receiver),
                "邮箱验证码",
                htmlContent
        );

        MailSendResult result = mailSendClient.sendHtmlMail(request);
        log.info("验证码邮件组装并发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }

    /**
     * 组装并发送审批通知邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendApprovalNoticeMail(String receiver) {
        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("approvalTitle", "采购申请审批")
                .put("applicantName", "张三")
                .put("applyTime", "2026-04-30 10:30:00")
                .put("approvalStatus", "待审批")
                .put("approvalUrl", "https://example.com/approval/10001")
                .build();

        String htmlContent = mailTemplateService.renderHtml("mail/approval-notice", variables);

        MailSendRequest request = MailSendRequest.html(
                ListUtil.of(receiver),
                "审批通知",
                htmlContent
        );

        MailSendResult result = mailSendClient.sendHtmlMail(request);
        log.info("审批通知邮件组装并发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }
}