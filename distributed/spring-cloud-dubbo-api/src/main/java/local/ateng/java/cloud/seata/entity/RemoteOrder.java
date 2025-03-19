package local.ateng.java.cloud.seata.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteOrder implements Serializable {

    @Serial
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