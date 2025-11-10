package io.github.atengk.state;


import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.context.OrderContext;
import org.springframework.stereotype.Component;

/**
 * 已发货状态
 */
@Component
public class ShippedState implements OrderState {

    private final OrderContext context;

    public ShippedState(OrderContext context) {
        this.context = context;
    }

    @Override
    public void pay() {
        System.out.println("订单已发货，不能再支付");
    }

    @Override
    public void ship() {
        System.out.println("订单已发货，请勿重复操作");
    }

    @Override
    public void complete() {
        System.out.println("订单已完成，状态切换为：已完成");
        context.setState(SpringUtil.getBean(CompletedState.class));
    }
}