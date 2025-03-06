package local.ateng.java.serialize.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@JSONType(orders = {"name", "age", "id"}, naming = PropertyNamingStrategy.SnakeCase, serializeFeatures = {JSONWriter.Feature.WriteNulls, JSONWriter.Feature.PrettyFormat})
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
//    @JSONField(ordinal = 1, serializeFeatures = JSONWriter.Feature.WriteLongAsString)
    private Long id;

    /**
     * 名称
     */
//    @JSONField(name = "full_name", label = "User Name")
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
//    @JSONField(serializeFeatures = JSONWriter.Feature.WriteBigDecimalAsPlain)
    private BigDecimal score;

    /**
     * 比例
     */
//    @JSONField(format = "##.##%")
    private Double ratio;

    /**
     * 生日
     */
//    @JSONField(format = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 所在省份
     */
//    @JSONField(defaultValue = "Chongqing")
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createTime;
    private Date createTime2;
    private Date createTime3;
    private int num;
    private List<String> list;

}

