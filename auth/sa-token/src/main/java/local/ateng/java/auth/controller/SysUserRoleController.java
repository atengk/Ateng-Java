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
import local.ateng.java.auth.entity.SysUserRole;
import local.ateng.java.auth.service.SysUserRoleService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 实现用户与角色之间的多对多关系 控制层。
 *
 * @author 孔余
 * @since 1.0.0
 */
@RestController
@RequestMapping("/sysUserRole")
public class SysUserRoleController {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    /**
     * 添加实现用户与角色之间的多对多关系。
     *
     * @param sysUserRole 实现用户与角色之间的多对多关系
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody SysUserRole sysUserRole) {
        return sysUserRoleService.save(sysUserRole);
    }

    /**
     * 根据主键删除实现用户与角色之间的多对多关系。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Integer id) {
        return sysUserRoleService.removeById(id);
    }

    /**
     * 根据主键更新实现用户与角色之间的多对多关系。
     *
     * @param sysUserRole 实现用户与角色之间的多对多关系
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody SysUserRole sysUserRole) {
        return sysUserRoleService.updateById(sysUserRole);
    }

    /**
     * 查询所有实现用户与角色之间的多对多关系。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<SysUserRole> list() {
        return sysUserRoleService.list();
    }

    /**
     * 根据实现用户与角色之间的多对多关系主键获取详细信息。
     *
     * @param id 实现用户与角色之间的多对多关系主键
     * @return 实现用户与角色之间的多对多关系详情
     */
    @GetMapping("getInfo/{id}")
    public SysUserRole getInfo(@PathVariable Integer id) {
        return sysUserRoleService.getById(id);
    }

    /**
     * 分页查询实现用户与角色之间的多对多关系。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<SysUserRole> page(Page<SysUserRole> page) {
        return sysUserRoleService.page(page);
    }

}
