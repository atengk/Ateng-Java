package io.github.atengk.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务定义表
 * </p>
 *
 * @author Ateng
 * @since 2026-02-11
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
     * 业务类型，如 order/report/user 等
     */
    @TableField("biz_type")
    private String bizType;

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
     * 最大重试次数
     */
    @TableField("max_retry_count")
    private Integer maxRetryCount;

    /**
     * 重试间隔(秒)
     */
    @TableField("retry_interval")
    private Integer retryInterval;

    /**
     * 执行状态 0=待执行 1=执行中 2=执行失败
     */
    @TableField("execute_status")
    private Integer executeStatus;

    /**
     * 最终失败原因
     */
    @TableField("next_execute_time")
    private LocalDateTime nextExecuteTime;

    /**
     * 最终失败原因
     */
    @TableField("fail_reason")
    private String failReason;

    /**
     * 乐观锁版本号
     */
    @TableField("version")
    @Version
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
