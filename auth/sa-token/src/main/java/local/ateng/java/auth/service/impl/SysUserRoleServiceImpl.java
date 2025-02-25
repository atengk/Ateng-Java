package local.ateng.java.auth.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import local.ateng.java.auth.entity.SysUserRole;
import local.ateng.java.auth.mapper.SysUserRoleMapper;
import local.ateng.java.auth.service.SysUserRoleService;
import org.springframework.stereotype.Service;

/**
 * 实现用户与角色之间的多对多关系 服务层实现。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>  implements SysUserRoleService{

}
