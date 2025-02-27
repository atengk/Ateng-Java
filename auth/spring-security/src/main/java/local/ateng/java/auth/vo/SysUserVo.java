package local.ateng.java.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * 返回客户端的实体类信息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String userName;
    private String nickName;
    private String sex;
    private String email;
    private String phoneNumber;
    private Timestamp createTime;
    private Timestamp updateTime;
    /**
     * token
     */
    private String token;
    /**
     * Refresh Token
     */
    private String refreshToken;
    /**
     * 菜单权限
     */
    private List<String> permissionList;

    /**
     * 角色权限
     */
    private List<String> roleList;

}
