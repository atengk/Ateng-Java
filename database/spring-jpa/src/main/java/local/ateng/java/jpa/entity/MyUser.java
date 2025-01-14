package local.ateng.java.jpa.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "my_user")
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，主键，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 使用数据库的自增策略
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

}
