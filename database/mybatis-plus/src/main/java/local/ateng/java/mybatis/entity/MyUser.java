package local.ateng.java.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 用户信息表，存储用户的基本信息
 * </p>
 *
 * @author 孔余
 * @since 2025-01-13
 */
@Getter
@Setter
@ToString
@TableName("my_user")
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，主键，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("name")
    private String name;

    /**
     * 用户年龄，允许为空
     */
    @TableField("age")
    private Integer age;

    /**
     * 用户分数，默认为0
     */
    @TableField("score")
    private Double score;

    /**
     * 用户生日，允许为空
     */
    @TableField("birthday")
    private LocalDateTime birthday;

    /**
     * 用户所在省份，允许为空
     */
    @TableField("province")
    private String province;

    /**
     * 用户所在城市，允许为空
     */
    @TableField("city")
    private String city;

    /**
     * 记录创建时间，默认当前时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
