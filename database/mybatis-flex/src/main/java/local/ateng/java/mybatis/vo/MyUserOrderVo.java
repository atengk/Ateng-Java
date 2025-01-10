package local.ateng.java.mybatis.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import local.ateng.java.mybatis.entity.MyOrder;
import local.ateng.java.mybatis.entity.MyUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUserOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，主键，自增
     */
    private Long id;

    /**
     * 用户名
     */
    private String name;

    /**
     * 用户年龄，允许为空
     */
    private Integer age;

    /**
     * 用户分数，默认为0
     */
    private Double score;

    /**
     * 用户生日，允许为空
     */
    private LocalDateTime birthday;

    /**
     * 用户所在省份，允许为空
     */
    private String province;

    /**
     * 用户所在城市，允许为空
     */
    private String city;

    /**
     * 记录创建时间，默认当前时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createTime;

    /**
     * 订单表id
     */
    private Long orderId;

    /**
     * 订单日期
     */
    private Date orderDate;

    /**
     * 订单总金额，精确到小数点后两位
     */
    private BigDecimal totalAmount;
}
