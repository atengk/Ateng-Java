package local.ateng.java.auth.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import local.ateng.java.auth.entity.SysPermission;
import local.ateng.java.auth.mapper.SysPermissionMapper;
import local.ateng.java.auth.service.SysPermissionService;
import org.springframework.stereotype.Service;

/**
 * 存储系统中的权限信息 服务层实现。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>  implements SysPermissionService{

}
