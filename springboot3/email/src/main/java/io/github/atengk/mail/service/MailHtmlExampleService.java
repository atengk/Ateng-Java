package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HTML 邮件示例服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailHtmlExampleService {

    private static final Logger log = LoggerFactory.getLogger(MailHtmlExampleService.class);

    private final MailSendClient mailSendClient;

    public MailHtmlExampleService(MailSendClient mailSendClient) {
        this.mailSendClient = mailSendClient;
    }

    /**
     * 发送验证码 HTML 邮件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendVerifyCode(String receiver) {
        String code = RandomUtil.randomNumbers(6);
        String subject = "邮箱验证码";
        String htmlContent = """
                <div style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.8;">
                    <h2 style="color: #333;">邮箱验证码</h2>
                    <p>您好，您的验证码为：</p>
                    <p style="font-size: 26px; font-weight: bold; color: #1677ff;">%s</p>
                    <p>验证码有效期为 5 分钟，请勿泄露给他人。</p>
                    <p style="font-size: 12px; color: #999;">如果不是您本人操作，请忽略本邮件。</p>
                </div>
                """.formatted(code);

        MailSendRequest request = MailSendRequest.html(
                ListUtil.of(receiver),
                subject,
                htmlContent
        );

        MailSendResult result = mailSendClient.sendHtmlMail(request);
        log.info("验证码 HTML 邮件发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }
}