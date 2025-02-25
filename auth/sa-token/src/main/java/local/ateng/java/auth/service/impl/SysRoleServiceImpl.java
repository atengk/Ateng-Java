package local.ateng.java.auth.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import local.ateng.java.auth.entity.SysRole;
import local.ateng.java.auth.mapper.SysRoleMapper;
import local.ateng.java.auth.service.SysRoleService;
import org.springframework.stereotype.Service;

/**
 * 存储系统中的角色信息 服务层实现。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>  implements SysRoleService{

}
