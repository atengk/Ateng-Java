package local.ateng.java.postgis.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import local.ateng.java.postgis.entity.PointEntities;
import local.ateng.java.postgis.service.PointEntitiesService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 *  控制层。
 *
 * @author ATeng
 * @since 2025-04-21
 */
@RestController
@RequestMapping("/pointEntities")
public class PointEntitiesController {

    @Autowired
    private PointEntitiesService pointEntitiesService;

    /**
     * 添加。
     *
     * @param pointEntities 
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody PointEntities pointEntities) {
        return pointEntitiesService.save(pointEntities);
    }

    /**
     * 根据主键删除。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Integer id) {
        return pointEntitiesService.removeById(id);
    }

    /**
     * 根据主键更新。
     *
     * @param pointEntities 
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody PointEntities pointEntities) {
        return pointEntitiesService.updateById(pointEntities);
    }

    /**
     * 查询所有。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<PointEntities> list() {
        return pointEntitiesService.list();
    }

    /**
     * 根据主键获取详细信息。
     *
     * @param id 主键
     * @return 详情
     */
    @GetMapping("getInfo/{id}")
    public PointEntities getInfo(@PathVariable Integer id) {
        return pointEntitiesService.getById(id);
    }

    /**
     * 分页查询。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<PointEntities> page(Page<PointEntities> page) {
        return pointEntitiesService.page(page);
    }

}
