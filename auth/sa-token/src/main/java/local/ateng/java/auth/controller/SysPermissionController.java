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
import local.ateng.java.auth.entity.SysPermission;
import local.ateng.java.auth.service.SysPermissionService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 存储系统中的权限信息 控制层。
 *
 * @author 孔余
 * @since 1.0.0
 */
@RestController
@RequestMapping("/sysPermission")
public class SysPermissionController {

    @Autowired
    private SysPermissionService sysPermissionService;

    /**
     * 添加存储系统中的权限信息。
     *
     * @param sysPermission 存储系统中的权限信息
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody SysPermission sysPermission) {
        return sysPermissionService.save(sysPermission);
    }

    /**
     * 根据主键删除存储系统中的权限信息。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Integer id) {
        return sysPermissionService.removeById(id);
    }

    /**
     * 根据主键更新存储系统中的权限信息。
     *
     * @param sysPermission 存储系统中的权限信息
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody SysPermission sysPermission) {
        return sysPermissionService.updateById(sysPermission);
    }

    /**
     * 查询所有存储系统中的权限信息。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<SysPermission> list() {
        return sysPermissionService.list();
    }

    /**
     * 根据存储系统中的权限信息主键获取详细信息。
     *
     * @param id 存储系统中的权限信息主键
     * @return 存储系统中的权限信息详情
     */
    @GetMapping("getInfo/{id}")
    public SysPermission getInfo(@PathVariable Integer id) {
        return sysPermissionService.getById(id);
    }

    /**
     * 分页查询存储系统中的权限信息。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<SysPermission> page(Page<SysPermission> page) {
        return sysPermissionService.page(page);
    }

}
