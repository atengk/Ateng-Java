package local.ateng.java.mybatis.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息表，存储用户的基本信息 实体类。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("my_user")
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，主键，自增
     */
    @Id(keyType = KeyType.Auto)
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
    private LocalDateTime createTime;

}
