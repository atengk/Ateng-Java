package local.ateng.java.cloud.controller;

import local.ateng.java.cloud.entity.MyOrder;
import local.ateng.java.cloud.service.IMyOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 订单信息表，存储用户的订单数据 前端控制器
 * </p>
 *
 * @author Ateng
 * @since 2025-03-19
 */
@RestController
@RequestMapping("/myOrder")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyOrderController {
    private final IMyOrderService myOrderService;

    @PostMapping("/save")
    public MyOrder save(@RequestBody MyOrder myOrder) {
        myOrderService.save(myOrder);
        return myOrder;
    }

}
