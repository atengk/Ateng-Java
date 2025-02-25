package local.ateng.java.auth.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实现角色与权限之间的多对多关系 实体类。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_role_permission")
public class SysRolePermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @Id
    private Integer roleId;

    /**
     * 权限ID
     */
    @Id
    private Integer permissionId;

}
