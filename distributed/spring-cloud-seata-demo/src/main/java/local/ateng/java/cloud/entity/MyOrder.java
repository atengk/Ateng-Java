package local.ateng.java.cloud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <p>
 * 订单信息表，存储用户的订单数据
 * </p>
 *
 * @author Ateng
 * @since 2025-03-19
 */
@Getter
@Setter
@ToString
@TableName("my_order")
public class MyOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID，主键，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，外键，关联用户表
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 订单日期
     */
    @TableField("date")
    private LocalDate date;

    /**
     * 订单总金额，精确到小数点后两位
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;
}
