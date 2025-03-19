package local.ateng.java.cloud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author Ateng
 * @since 2025-03-19
 */
@Getter
@Setter
@ToString
@TableName("my_user")
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户姓名
     */
    @TableField("name")
    private String name;

    @TableField("age")
    private Integer age;

    @TableField("score")
    private Double score;

    /**
     * 用户生日
     */
    @TableField("birthday")
    private LocalDate birthday;

    /**
     * 用户所在省份
     */
    @TableField("province")
    private String province;

    /**
     * 用户所在城市
     */
    @TableField("city")
    private String city;

    @TableField("create_time")
    private LocalDateTime createTime;
}
