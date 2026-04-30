package io.github.atengk.mail.model;

/**
 * 邮件发送记录状态
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum MailRecordStatus {

    /**
     * 待发送
     */
    PENDING,

    /**
     * 发送中
     */
    SENDING,

    /**
     * 发送成功
     */
    SUCCESS,

    /**
     * 发送失败
     */
    FAIL,

    /**
     * 跳过发送
     */
    SKIPPED
}