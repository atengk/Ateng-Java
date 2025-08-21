package local.ateng.java.mybatisjdk8.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import local.ateng.java.mybatisjdk8.enums.StatusEnum;
import local.ateng.java.mybatisjdk8.handler.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 项目表，包含常用字段类型
 * </p>
 *
 * @author Ateng
 * @since 2025-07-17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "project_mini", autoResultMap = true)
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 顺序UUID，用于替代ID，全局唯一
     */
    @TableField(value = "uuid", typeHandler = UUIDTypeHandler.class)
    private UUID uuid;

    /**
     * 名称，最大100字符
     */
    @TableField("name")
    private String name;

    /**
     * 项目编号，唯一逻辑标识
     */
    @TableField("code")
    private String code;

    /**
     * 描述，可以存储较长的文本
     */
    @TableField("description")
    private String description;

    /**
     * 金额，最多10位，小数2位
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 浮动分数
     */
    @TableField("score")
    private Double score;

    /**
     * 账户余额，支持大整数
     */
    @TableField("balance")
    private Long balance;

    /**
     * 标签集合
     */
    @TableField("tags")
    private String tags;

    /**
     * 优先级
     */
    @TableField("priority")
    private String priority;

    /**
     * 状态：0=草稿, 1=进行中, 2=已完成, 3=已取消
     */
    @TableField("status")
    private StatusEnum status;

    /**
     * 是否激活
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * 是否已删除，逻辑删除标志
     */
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 版本号，用于乐观锁
     */
    @TableField("version")
    private Integer version;

    /**
     * 用户数量，正整数
     */
    @TableField("user_count")
    private Integer userCount;

    /**
     * 出生日期
     */
    @TableField("birth_date")
    private LocalDate birthDate;

    /**
     * 最后登录时间
     */
    @TableField("last_login")
    private LocalTime lastLogin;

    /**
     * 开始日期
     */
    @TableField("start_date")
    private LocalDateTime startDate;

    /**
     * 结束日期
     */
    @TableField("end_date")
    private LocalDateTime endDate;

    /**
     * 地区名称
     */
    @TableField("region")
    private String region;

    /**
     * 文件路径，例如上传到OSS的路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * JSON对象类型数据
     */
    @TableField(value = "json_object", typeHandler = Fastjson2TypeHandler.class)
    private MyData jsonObject;

    /**
     * JSON数组类型数据
     */
    @TableField(value = "json_array", typeHandler = Fastjson2TypeHandler.class)
    private List<MyData> jsonArray;

    /**
     * 地理坐标（经纬度）
     */
    @TableField(value = "location", typeHandler = GeometryTypeHandler.class)
    @JSONField(serialize = false)
    private Geometry location;

    /**
     * IP地址，支持IPv6
     */
    @TableField(value = "ip_address", typeHandler = IPAddressTypeHandler.class)
    private String ipAddress;

    /**
     * 二进制大数据字段
     */
    @TableField(value = "binary_data", typeHandler = Base64TypeHandler.class)
    private String binaryData;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
