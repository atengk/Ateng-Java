package local.ateng.java.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import local.ateng.java.auth.bo.SysUserLoginBo;
import local.ateng.java.auth.bo.SysUserPageBo;
import local.ateng.java.auth.entity.SysUser;
import local.ateng.java.auth.service.SysUserService;
import local.ateng.java.auth.utils.Result;
import local.ateng.java.auth.vo.SysUserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统设置/用户设置
 *
 * @author 孔余
 * @since 1.0.0
 */
@RestController
@RequestMapping("/auth/user")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 登录
     */
    @SaIgnore
    @PostMapping("/login")
    public Result login(@RequestBody SysUserLoginBo bo) {
        SysUserVo sysUserVo = sysUserService.loginUser(bo);
        return Result.success(sysUserVo);
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public Result logout() {
        StpUtil.logout();
        return Result.success();
    }

    /**
     * 新增
     */
    @SaCheckRole("admin")
    @SaCheckPermission("ateng.user.add")
    @PostMapping("/add")
    public Result add(@RequestBody SysUser entity) {
        sysUserService.addUser(entity);
        return Result.success();
    }

    /**
     * 查询所有
     */
    @SaCheckPermission(value = "ateng.user.get", orRole = "admin")
    @GetMapping("/list")
    public Result list() {
        List<SysUser> list = sysUserService.list();
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @SaCheckPermission(value = "ateng.user.get", orRole = "admin")
    @PostMapping("/page")
    public Result page(@RequestBody SysUserPageBo bo) {
        Page<SysUser> sysUserPage = sysUserService.pageUser(bo);
        return Result.success(sysUserPage);
    }

    /**
     * 批量删除
     */
    @SaCheckRole("admin")
    @SaCheckPermission("ateng.user.delete")
    @PostMapping("/delete-batch")
    public Result deleteBatch(List<Long> ids) {
        sysUserService.deleteBatchUser(ids);
        return Result.success();
    }

    /**
     * 更新
     */
    @SaCheckRole("admin")
    @SaCheckPermission("ateng.user.update")
    @PostMapping("/update")
    public Result update(@RequestBody SysUser entity) {
        sysUserService.updateUser(entity);
        return Result.success();
    }

    /**
     * 获取详细信息
     */
    @SaCheckPermission(value = "ateng.user.get", orRole = "admin")
    @GetMapping("/get")
    public Result get(Integer id) {
        SysUser sysUser = sysUserService.getById(id);
        return Result.success(sysUser);
    }

    /**
     * 获取账号权限
     */
    @GetMapping("/get-permission-list")
    public Result getPermissionList() {
        List<String> permissionList = StpUtil.getPermissionList();
        return Result.success(permissionList);
    }

    /**
     * 获取账号角色
     */
    @GetMapping("/get-role-list")
    public Result getRoleList() {
        List<String> roleList = StpUtil.getRoleList();
        return Result.success(roleList);
    }

}