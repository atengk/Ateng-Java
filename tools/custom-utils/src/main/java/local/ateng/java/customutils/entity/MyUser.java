package local.ateng.java.customutils.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 手机号码
     */
    private String phoneNumber;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 比例
     */
    private Double ratio;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 所在省份
     */
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * Date 时间
     */
    private Date dateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * UTC 时间戳
     */
    private Instant instantTime;

    /**
     * 带时区偏移的日期时间
     */
    private OffsetDateTime offsetDateTime;

    /**
     * 带完整时区的日期时间
     */
    private ZonedDateTime zonedDateTime;
}
