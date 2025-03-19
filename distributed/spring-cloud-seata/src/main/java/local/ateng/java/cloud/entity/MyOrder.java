package local.ateng.java.cloud.entity;

import lombok.Builder;
import lombok.Data;

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
@Data
@Builder
public class MyOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID，主键，自增
     */
    private Long id;

    /**
     * 用户ID，外键，关联用户表
     */
    private Long userId;

    /**
     * 订单日期
     */
    private LocalDate date;

    /**
     * 订单总金额，精确到小数点后两位
     */
    private BigDecimal totalAmount;
}
