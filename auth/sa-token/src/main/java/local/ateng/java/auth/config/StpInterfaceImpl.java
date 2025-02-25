package local.ateng.java.auth.config;


import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import local.ateng.java.auth.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义权限加载接口实现类
 * - 在每次调用鉴权代码时，才会执行相应的方法
 * - 如果权限有了变更，重新登录用户就可以刷新（刷新缓存），也可以将session里面的ROLE_LIST和PERMISSION_LIST删除
 * https://sa-token.cc/doc.html#/use/jur-auth
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-24
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StpInterfaceImpl implements StpInterface {
    private final SysUserService sysUserService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 缓存到session中 获取数据取值 (如果值为 null，则执行 fun 函数获取值，并把函数返回值写入缓存)
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        // 获取权限
        return session.get(SaSession.PERMISSION_LIST, () -> {
            // 从数据库查询这个角色所拥有的权限列表
            List<String> list = sysUserService.getUserPermissionList(Integer.valueOf(loginId.toString()));
            return list;
        });
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 缓存到session中
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        return session.get(SaSession.ROLE_LIST, () -> {
            // 从数据库查询这个账号id拥有的角色列表
            List<String> list = sysUserService.getUserRoleList(Integer.valueOf(loginId.toString()));
            return list;
        });
    }

}

