package io.github.atengk.task.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 任务执行日志表
 * </p>
 *
 * @author Ateng
 * @since 2026-02-11
 */
@Getter
@Setter
@ToString
@TableName("task_job_log")
public class TaskJobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("job_id")
    private Long jobId;

    /**
     * 任务编码
     */
    @TableField("job_code")
    private String jobCode;

    /**
     * 执行时间
     */
    @TableField("execute_time")
    private LocalDateTime executeTime;

    /**
     * 1=成功 2=失败
     */
    @TableField("execute_status")
    private Integer executeStatus;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 耗时(ms)
     */
    @TableField("execute_duration")
    private Long executeDuration;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
