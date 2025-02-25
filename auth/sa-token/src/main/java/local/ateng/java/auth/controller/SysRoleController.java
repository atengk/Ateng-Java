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
import local.ateng.java.auth.entity.SysRole;
import local.ateng.java.auth.service.SysRoleService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 存储系统中的角色信息 控制层。
 *
 * @author 孔余
 * @since 1.0.0
 */
@RestController
@RequestMapping("/sysRole")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    /**
     * 添加存储系统中的角色信息。
     *
     * @param sysRole 存储系统中的角色信息
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody SysRole sysRole) {
        return sysRoleService.save(sysRole);
    }

    /**
     * 根据主键删除存储系统中的角色信息。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Integer id) {
        return sysRoleService.removeById(id);
    }

    /**
     * 根据主键更新存储系统中的角色信息。
     *
     * @param sysRole 存储系统中的角色信息
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody SysRole sysRole) {
        return sysRoleService.updateById(sysRole);
    }

    /**
     * 查询所有存储系统中的角色信息。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<SysRole> list() {
        return sysRoleService.list();
    }

    /**
     * 根据存储系统中的角色信息主键获取详细信息。
     *
     * @param id 存储系统中的角色信息主键
     * @return 存储系统中的角色信息详情
     */
    @GetMapping("getInfo/{id}")
    public SysRole getInfo(@PathVariable Integer id) {
        return sysRoleService.getById(id);
    }

    /**
     * 分页查询存储系统中的角色信息。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<SysRole> page(Page<SysRole> page) {
        return sysRoleService.page(page);
    }

}
