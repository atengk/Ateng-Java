package io.github.atengk.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务执行日志表
 * </p>
 *
 * <p>
 * 用于记录任务每次执行情况，
 * 包括执行结果、耗时、错误信息等。
 * </p>
 *
 * @author Ateng
 * @since 2026-02-12
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
     * 业务类型
     */
    @TableField("biz_type")
    private String bizType;

    /**
     * 执行时间
     */
    @TableField("execute_time")
    private LocalDateTime executeTime;

    /**
     * 执行状态
     * 2=失败 3=成功
     */
    @TableField("execute_status")
    private Integer executeStatus;

    /**
     * 本次执行前的重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 执行耗时(ms)
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
