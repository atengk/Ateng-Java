package local.ateng.java.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import local.ateng.java.auth.bo.SysUserLoginBo;
import local.ateng.java.auth.bo.SysUserPageBo;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.entity.SysUser;
import local.ateng.java.auth.entity.SysUserRole;
import local.ateng.java.auth.exception.ServiceException;
import local.ateng.java.auth.mapper.SysUserMapper;
import local.ateng.java.auth.service.SysRoleService;
import local.ateng.java.auth.service.SysUserRoleService;
import local.ateng.java.auth.service.SysUserService;
import local.ateng.java.auth.vo.SysUserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;

import static local.ateng.java.auth.entity.table.SysPermissionTableDef.SYS_PERMISSION;
import static local.ateng.java.auth.entity.table.SysRolePermissionTableDef.SYS_ROLE_PERMISSION;
import static local.ateng.java.auth.entity.table.SysRoleTableDef.SYS_ROLE;
import static local.ateng.java.auth.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;
import static local.ateng.java.auth.entity.table.SysUserTableDef.SYS_USER;

/**
 * 存储用户的基本信息 服务层实现。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleService sysRoleService;

    @Override
    public SysUserVo loginUser(SysUserLoginBo bo) {
        String userName = bo.getUserName();
        String password = bo.getPassword();
        SysUser user = this.queryChain()
                .select(SYS_USER.ALL_COLUMNS)
                .from(SYS_USER)
                .where(SYS_USER.USER_NAME.eq(userName))
                .one();
        // 判断用户是否存在
        cn.hutool.core.lang.Assert.isTrue(!ObjectUtils.isEmpty(user), () -> new ServiceException(AppCodeEnum.AUTH_USER_NOT_FOUND.getCode(), AppCodeEnum.AUTH_USER_NOT_FOUND.getDescription()));
        // 校验密码
        boolean checkpw = BCrypt.checkpw(password, user.getPassword());
        cn.hutool.core.lang.Assert.isTrue(checkpw, () -> new ServiceException(AppCodeEnum.AUTH_PASSWORD_INCORRECT.getCode(), AppCodeEnum.AUTH_PASSWORD_INCORRECT.getDescription()));
        // 登录用户
        StpUtil.login(user.getUserId());
        SysUserVo sysUserVo = Convert.convert(SysUserVo.class, user);
        sysUserVo.setToken(StpUtil.getTokenValue());
        sysUserVo.setRoleList(this.getUserRoleList(user.getUserId()));
        sysUserVo.setPermissionList(this.getUserPermissionList(user.getUserId()));
        // 返回用户信息
        return sysUserVo;
    }

    @Override
    @Transactional
    public void addUser(SysUser entity) {
        String userName = entity.getUserName();
        String password = entity.getPassword();
        SysUser user = this.queryChain()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.USER_NAME.eq(userName))
                .one();
        // 判断用户是否存在
        cn.hutool.core.lang.Assert.isTrue(ObjectUtils.isEmpty(user), () -> new ServiceException(AppCodeEnum.AUTH_USER_ALREADY_EXISTS.getCode(), AppCodeEnum.AUTH_USER_ALREADY_EXISTS.getDescription()));
        // 新增用户
        String passwordEncrypt = BCrypt.hashpw(password);
        entity.setPassword(passwordEncrypt);
        this.save(entity);
        // 关联角色
        Integer userRoleId = sysRoleService
                .queryChain()
                .select(SYS_ROLE.ROLE_ID)
                .from(SYS_ROLE)
                .where(SYS_ROLE.ROLE_NAME.eq("user"))
                .one()
                .getRoleId();
        sysUserRoleService.save(new SysUserRole(entity.getUserId(), userRoleId));
    }

    @Override
    public Page<SysUser> pageUser(SysUserPageBo bo) {
        String nickName = bo.getNickName();
        Page<SysUser> page = this.queryChain()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.NICK_NAME.like(nickName, !ObjectUtils.isEmpty(nickName)))
                .page(new Page<>(bo.getPageNumber(), bo.getPageSize()));
        return page;
    }

    @Override
    public void deleteBatchUser(List<Long> ids) {
        this.removeByIds(ids);
    }

    @Override
    public void updateUser(SysUser entity) {
        String userName = entity.getUserName();
        String password = entity.getPassword();
        SysUser user = this.getById(entity.getUserId());
        // 判断用户是否存在
        cn.hutool.core.lang.Assert.isTrue(!ObjectUtils.isEmpty(user), () -> new ServiceException(AppCodeEnum.AUTH_USER_NOT_FOUND.getCode(), AppCodeEnum.AUTH_USER_NOT_FOUND.getDescription()));
        cn.hutool.core.lang.Assert.isTrue(user.getUserName().equals(userName), () -> new ServiceException(AppCodeEnum.AUTH_USER_NOT_INCONSISTENT.getCode(), AppCodeEnum.AUTH_USER_NOT_INCONSISTENT.getDescription()));
        // 更新用户
        String passwordEncrypt = BCrypt.hashpw(password);
        entity.setPassword(passwordEncrypt);
        entity.setUpdateTime(DateUtil.date().toTimestamp());
        this.updateById(entity, true);
    }

    @Override
    public List<String> getUserRoleList(Integer id) {
        // 根据用户id查询角色列表
        List<String> list = this
                .queryChain()
                .select(SYS_ROLE.ROLE_NAME)
                .from(SYS_USER)
                .where(SYS_USER.USER_ID.eq(id))
                .leftJoin(SYS_USER_ROLE)
                .on(SYS_USER.USER_ID.eq(SYS_USER_ROLE.USER_ID))
                .leftJoin(SYS_ROLE)
                .on(SYS_USER_ROLE.ROLE_ID.eq(SYS_ROLE.ROLE_ID))
                .listAs(String.class);
        return list;
    }

    @Override
    public List<String> getUserPermissionList(Integer id) {
        // 根据用户id查询权限列表
        List<String> list = this
                .queryChain()
                .select(SYS_PERMISSION.PERMISSION_NAME)
                .from(SYS_USER)
                .where(SYS_USER.USER_ID.eq(id))
                .leftJoin(SYS_USER_ROLE)
                .on(SYS_USER.USER_ID.eq(SYS_USER_ROLE.USER_ID))
                .leftJoin(SYS_ROLE)
                .on(SYS_USER_ROLE.ROLE_ID.eq(SYS_ROLE.ROLE_ID))
                .leftJoin(SYS_ROLE_PERMISSION)
                .on(SYS_ROLE_PERMISSION.ROLE_ID.eq(SYS_ROLE.ROLE_ID))
                .leftJoin(SYS_PERMISSION)
                .on(SYS_PERMISSION.PERMISSION_ID.eq(SYS_ROLE_PERMISSION.PERMISSION_ID))
                .listAs(String.class);
        return list;
    }

}

