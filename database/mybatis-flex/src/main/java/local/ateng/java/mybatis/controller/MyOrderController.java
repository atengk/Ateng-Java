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
import local.ateng.java.mybatis.entity.MyOrder;
import local.ateng.java.mybatis.service.MyOrderService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 订单信息表，存储用户的订单数据 控制层。
 *
 * @author 孔余
 * @since 1.0.0
 */
@RestController
@RequestMapping("/myOrder")
public class MyOrderController {

    @Autowired
    private MyOrderService myOrderService;

    /**
     * 添加订单信息表，存储用户的订单数据。
     *
     * @param myOrder 订单信息表，存储用户的订单数据
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody MyOrder myOrder) {
        return myOrderService.save(myOrder);
    }

    /**
     * 根据主键删除订单信息表，存储用户的订单数据。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return myOrderService.removeById(id);
    }

    /**
     * 根据主键更新订单信息表，存储用户的订单数据。
     *
     * @param myOrder 订单信息表，存储用户的订单数据
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody MyOrder myOrder) {
        return myOrderService.updateById(myOrder);
    }

    /**
     * 查询所有订单信息表，存储用户的订单数据。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<MyOrder> list() {
        return myOrderService.list();
    }

    /**
     * 根据订单信息表，存储用户的订单数据主键获取详细信息。
     *
     * @param id 订单信息表，存储用户的订单数据主键
     * @return 订单信息表，存储用户的订单数据详情
     */
    @GetMapping("getInfo/{id}")
    public MyOrder getInfo(@PathVariable Long id) {
        return myOrderService.getById(id);
    }

    /**
     * 分页查询订单信息表，存储用户的订单数据。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<MyOrder> page(Page<MyOrder> page) {
        return myOrderService.page(page);
    }

}
