package local.ateng.java.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储系统中的权限信息 实体类。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_permission")
public class SysPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID，主键，自增
     */
    @Id(keyType = KeyType.Auto)
    private Integer permissionId;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Timestamp createdTime;

    /**
     * 更新时间
     */
    private Timestamp updatedTime;

}
