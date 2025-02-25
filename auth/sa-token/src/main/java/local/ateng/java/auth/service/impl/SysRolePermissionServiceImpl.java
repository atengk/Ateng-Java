package local.ateng.java.auth.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import local.ateng.java.auth.entity.SysRolePermission;
import local.ateng.java.auth.mapper.SysRolePermissionMapper;
import local.ateng.java.auth.service.SysRolePermissionService;
import org.springframework.stereotype.Service;

/**
 * 实现角色与权限之间的多对多关系 服务层实现。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>  implements SysRolePermissionService{

}
