package local.ateng.java.mybatis.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import local.ateng.java.mybatis.entity.MyUser;
import local.ateng.java.mybatis.service.MyUserService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 用户信息表，存储用户的基本信息 控制层。
 *
 * @author 孔余
 * @since 1.0.0
 */
@RestController
@RequestMapping("/myUser")
public class MyUserController {

    @Autowired
    private MyUserService myUserService;

    /**
     * 添加用户信息表，存储用户的基本信息。
     *
     * @param myUser 用户信息表，存储用户的基本信息
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody MyUser myUser) {
        return myUserService.save(myUser);
    }

    /**
     * 根据主键删除用户信息表，存储用户的基本信息。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return myUserService.removeById(id);
    }

    /**
     * 根据主键更新用户信息表，存储用户的基本信息。
     *
     * @param myUser 用户信息表，存储用户的基本信息
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody MyUser myUser) {
        return myUserService.updateById(myUser);
    }

    /**
     * 查询所有用户信息表，存储用户的基本信息。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<MyUser> list() {
        return myUserService.list();
    }

    /**
     * 根据用户信息表，存储用户的基本信息主键获取详细信息。
     *
     * @param id 用户信息表，存储用户的基本信息主键
     * @return 用户信息表，存储用户的基本信息详情
     */
    @GetMapping("getInfo/{id}")
    public MyUser getInfo(@PathVariable Long id) {
        return myUserService.getById(id);
    }

    /**
     * 分页查询用户信息表，存储用户的基本信息。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<MyUser> page(Page<MyUser> page) {
        return myUserService.page(page);
    }

}
