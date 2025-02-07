package local.ateng.java.es.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.easyes.annotation.HighLight;
import org.dromara.easyes.annotation.IndexField;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.Settings;
import org.dromara.easyes.annotation.rely.Analyzer;
import org.dromara.easyes.annotation.rely.FieldType;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Settings(
        shardsNum = 1,
        replicasNum = 0,
        maxResultWindow = 10000
)
@IndexName(value = "my_user")
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private String id;

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
     * IP地址
     */
    @IndexField(fieldType = FieldType.IP)
    private String ipaddress;

    /**
     * 公司名称
     */
    private String company;

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 生日
     */
    @IndexField(fieldType = FieldType.DATE, dateFormat = "yyyy-MM-dd")
    private Date birthday;

    /**
     * 所在省份
     */
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 地址
     */
    private String address;

    /**
     * 经纬度(lat,lng)
     */
    @IndexField(fieldType = FieldType.GEO_POINT)
    private String location;

    /**
     * 一段文字
     */
    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.IK_SMART, searchAnalyzer = Analyzer.IK_MAX_WORD)
    private String paragraph;

    /**
     * 记录创建时间，默认当前时间
     */
    @IndexField(fieldType = FieldType.DATE, dateFormat = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createTime;

}
