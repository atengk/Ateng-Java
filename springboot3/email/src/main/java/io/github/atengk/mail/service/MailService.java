package io.github.atengk.mail.service;

import io.github.atengk.mail.dto.MailBatchSendRequest;
import io.github.atengk.mail.dto.MailSendRequest;
import io.github.atengk.mail.dto.MailSendResult;
import io.github.atengk.mail.dto.MailSendSummary;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 邮件服务接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface MailService {

    /**
     * 根据请求中的正文类型发送邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    MailSendResult send(MailSendRequest request);

    /**
     * 发送纯文本邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    MailSendResult sendText(MailSendRequest request);

    /**
     * 发送 HTML 邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    MailSendResult sendHtml(MailSendRequest request);

    /**
     * 发送模板邮件。
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    MailSendResult sendTemplate(MailSendRequest request);

    /**
     * 批量发送邮件。
     *
     * @param request 批量邮件发送请求
     * @return 批量邮件发送汇总结果
     */
    MailSendSummary sendBatch(MailBatchSendRequest request);

    /**
     * 异步发送单封邮件。
     *
     * @param request 邮件发送请求
     * @return 异步邮件发送结果
     */
    CompletableFuture<MailSendResult> sendAsync(MailSendRequest request);

    /**
     * 使用指定线程池异步发送单封邮件。
     *
     * @param request  邮件发送请求
     * @param executor 指定线程池
     * @return 异步邮件发送结果
     */
    CompletableFuture<MailSendResult> sendAsync(MailSendRequest request, Executor executor);

    /**
     * 异步批量发送邮件。
     *
     * @param request 批量邮件发送请求
     * @return 异步批量邮件发送汇总结果
     */
    CompletableFuture<MailSendSummary> sendBatchAsync(MailBatchSendRequest request);

    /**
     * 使用指定线程池异步批量发送邮件。
     *
     * @param request  批量邮件发送请求
     * @param executor 指定线程池
     * @return 异步批量邮件发送汇总结果
     */
    CompletableFuture<MailSendSummary> sendBatchAsync(MailBatchSendRequest request, Executor executor);

    /**
     * 渲染邮件模板。
     *
     * @param templateName 模板名称
     * @param variables 模板变量
     * @return 渲染后的邮件正文
     */
    String renderTemplate(String templateName, Map<String, Object> variables);

    /**
     * 校验邮件发送请求。
     *
     * @param request 邮件发送请求
     */
    void validate(MailSendRequest request);

    /**
     * 检查邮件服务器连接状态。
     *
     * @return true 表示连接正常，false 表示连接异常
     */
    boolean checkConnection();
}
