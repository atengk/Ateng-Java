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
 * 模板邮件示例服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailTemplateExampleService {

    private static final Logger log = LoggerFactory.getLogger(MailTemplateExampleService.class);

    private final MailSendClient mailSendClient;

    public MailTemplateExampleService(MailSendClient mailSendClient) {
        this.mailSendClient = mailSendClient;
    }

    /**
     * 发送验证码模板邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendVerifyCodeTemplate(String receiver) {
        String code = RandomUtil.randomNumbers(6);
        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("code", code)
                .put("expireMinutes", 5)
                .put("productName", "业务管理系统")
                .build();

        MailSendRequest request = MailSendRequest.template(
                ListUtil.of(receiver),
                "邮箱验证码",
                "mail/verify-code",
                variables
        );

        MailSendResult result = mailSendClient.sendTemplateMail(request);
        log.info("验证码模板邮件发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }
}