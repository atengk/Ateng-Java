package io.github.atengk.mail.model;

import java.time.LocalDateTime;

/**
 * 邮件发送结果对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailSendResult(
        boolean success,
        boolean skipped,
        String message,
        LocalDateTime sendTime,
        String errorStack
) {

    /**
     * 创建发送成功结果
     *
     * @param message 结果说明
     * @return 邮件发送结果
     */
    public static MailSendResult success(String message) {
        return new MailSendResult(true, false, message, LocalDateTime.now(), null);
    }

    /**
     * 创建跳过发送结果
     *
     * @param message 结果说明
     * @return 邮件发送结果
     */
    public static MailSendResult skipped(String message) {
        return new MailSendResult(false, true, message, LocalDateTime.now(), null);
    }

    /**
     * 创建发送失败结果
     *
     * @param message    结果说明
     * @param errorStack 异常堆栈
     * @return 邮件发送结果
     */
    public static MailSendResult failure(String message, String errorStack) {
        return new MailSendResult(false, false, message, LocalDateTime.now(), errorStack);
    }
}