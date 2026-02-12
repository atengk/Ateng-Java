package io.github.atengk.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 一次性任务 / 异步补偿任务表
 * </p>
 *
 * <p>
 * 用于存储需要执行的任务信息，
 * 支持延迟执行、失败重试、乐观锁控制。
 * </p>
 *
 * @author Ateng
 * @since 2026-02-12
 */
@Getter
@Setter
@ToString
@TableName("task_job")
public class TaskJob implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务唯一编码
     */
    @TableField("job_code")
    private String jobCode;

    /**
     * 任务名称
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 任务描述
     */
    @TableField("job_desc")
    private String jobDesc;

    /**
     * 业务类型
     */
    @TableField("biz_type")
    private String bizType;

    /**
     * 业务ID
     */
    @TableField("biz_id")
    private String bizId;

    /**
     * Spring Bean名称
     */
    @TableField("bean_name")
    private String beanName;

    /**
     * 方法名
     */
    @TableField("method_name")
    private String methodName;

    /**
     * 方法参数类型(JSON数组)
     */
    @TableField("method_param_types")
    private String methodParamTypes;

    /**
     * 方法参数值(JSON数组)
     */
    @TableField("method_params")
    private String methodParams;

    /**
     * 执行状态
     * 0=待执行 1=执行中 2=失败 3=成功
     */
    @TableField("execute_status")
    private Integer executeStatus;

    /**
     * 已重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @TableField("max_retry_count")
    private Integer maxRetryCount;

    /**
     * 重试间隔(秒)
     */
    @TableField("retry_interval_seconds")
    private Integer retryIntervalSeconds;

    /**
     * 下次执行时间
     */
    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;

    /**
     * 执行开始时间
     */
    @TableField("execute_start_time")
    private LocalDateTime executeStartTime;

    /**
     * 锁定时间
     */
    @TableField("lock_time")
    private LocalDateTime lockTime;

    /**
     * 最终失败原因
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
