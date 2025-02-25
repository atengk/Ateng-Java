package local.ateng.java.auth.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import local.ateng.java.auth.entity.SysRolePermission;
import local.ateng.java.auth.service.SysRolePermissionService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 实现角色与权限之间的多对多关系 控制层。
 *
 * @author 孔余
 * @since 1.0.0
 */
@RestController
@RequestMapping("/sysRolePermission")
public class SysRolePermissionController {

    @Autowired
    private SysRolePermissionService sysRolePermissionService;

    /**
     * 添加实现角色与权限之间的多对多关系。
     *
     * @param sysRolePermission 实现角色与权限之间的多对多关系
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody SysRolePermission sysRolePermission) {
        return sysRolePermissionService.save(sysRolePermission);
    }

    /**
     * 根据主键删除实现角色与权限之间的多对多关系。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Integer id) {
        return sysRolePermissionService.removeById(id);
    }

    /**
     * 根据主键更新实现角色与权限之间的多对多关系。
     *
     * @param sysRolePermission 实现角色与权限之间的多对多关系
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody SysRolePermission sysRolePermission) {
        return sysRolePermissionService.updateById(sysRolePermission);
    }

    /**
     * 查询所有实现角色与权限之间的多对多关系。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<SysRolePermission> list() {
        return sysRolePermissionService.list();
    }

    /**
     * 根据实现角色与权限之间的多对多关系主键获取详细信息。
     *
     * @param id 实现角色与权限之间的多对多关系主键
     * @return 实现角色与权限之间的多对多关系详情
     */
    @GetMapping("getInfo/{id}")
    public SysRolePermission getInfo(@PathVariable Integer id) {
        return sysRolePermissionService.getById(id);
    }

    /**
     * 分页查询实现角色与权限之间的多对多关系。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<SysRolePermission> page(Page<SysRolePermission> page) {
        return sysRolePermissionService.page(page);
    }

}
