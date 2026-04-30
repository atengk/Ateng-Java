package io.github.atengk.mail.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

/**
 * 邮件发送记录查询请求对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailRecordQueryRequest(

        /**
         * 当前页
         */
        @Min(value = 1, message = "页码不能小于 1")
        Long pageNum,

        /**
         * 每页条数
         */
        @Min(value = 1, message = "每页条数不能小于 1")
        @Max(value = 100, message = "每页条数不能大于 100")
        Long pageSize,

        /**
         * 收件人邮箱
         */
        String receiver,

        /**
         * 邮件主题
         */
        String subject,

        /**
         * 发送状态，例如 SUCCESS、FAIL、SKIPPED
         */
        String status,

        /**
         * 开始时间
         */
        LocalDateTime startTime,

        /**
         * 结束时间
         */
        LocalDateTime endTime
) {

    /**
     * 获取安全页码
     *
     * @return 页码
     */
    public Long safePageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    /**
     * 获取安全每页条数
     *
     * @return 每页条数
     */
    public Long safePageSize() {
        return pageSize == null ? 10L : pageSize;
    }
}