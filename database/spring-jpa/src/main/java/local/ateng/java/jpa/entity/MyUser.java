package local.ateng.java.jpa.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


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
    @Column(name = "id", nullable = false)
    @Comment("用户ID，主键，自增")
    private Long id;

    /**
     * 用户名
     */
    @Column(name = "name", nullable = false, unique = true)
    @Comment("用户姓名")
    private String name;

    /**
     * 用户年龄，允许为空
     */
    @Column(name = "age")
    @Comment("用户年龄")
    private Integer age;

    /**
     * 用户分数
     */
    @Column(name = "score", columnDefinition = "DOUBLE DEFAULT 0.0")
    @Comment("用户分数")
    private Double score;

    /**
     * 用户生日，允许为空
     */
    @Column(name = "birthday")
    @Comment("用户生日")
    private LocalDateTime birthday;

    /**
     * 用户所在省份，允许为空
     */
    @Column(name = "province")
    @Comment("用户所在省份")
    private String province;

    /**
     * 用户所在城市，允许为空
     */
    @Column(name = "city")
    @Comment("用户所在城市")
    private String city;

    /**
     * 记录创建时间，默认当前时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss.SSS")
    @Column(name = "createTime")
    @Comment("创建时间")
    private LocalDateTime createTime;

}
