package local.ateng.java.mybatis.entity;

import lombok.Data;
import org.dromara.autotable.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AutoTable(value = "my_user", comment = "用户表")
@Data
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ColumnComment("主键id")
    @PrimaryKey(autoIncrement = true)
    @ColumnNotNull
    private Long id;

    /**
     * 名称
     */
    @ColumnComment("名称")
    @Index
    @ColumnNotNull
    private String name;

    /**
     * 年龄
     */
    @ColumnComment("年龄")
    private Integer age;

    /**
     * 手机号码
     */
    @ColumnComment("手机号码")
    @ColumnType(length = 20)
    private String phoneNumber;

    /**
     * 邮箱
     */
    @ColumnComment("邮箱")
    private String email;

    /**
     * 分数
     */
    @ColumnComment("分数")
    private BigDecimal score;

    /**
     * 比例
     */
    @ColumnComment("比例")
    private Double ratio;

    /**
     * 生日
     */
    @ColumnComment("生日")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @ColumnComment("所在省份")
    private String province;

    /**
     * 所在城市
     */
    @ColumnComment("所在城市")
    private String city;

    /**
     * 创建时间
     */
    @ColumnComment("创建时间")
    private LocalDateTime createTime;

}
