package io.github.atengk.state;

import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.context.OrderContext;
import org.springframework.stereotype.Component;

/**
 * 已支付状态
 */
@Component
public class PaidState implements OrderState {

    private final OrderContext context;

    public PaidState(OrderContext context) {
        this.context = context;
    }

    @Override
    public void pay() {
        System.out.println("订单已支付，请勿重复支付");
    }

    @Override
    public void ship() {
        System.out.println("订单发货成功，状态切换为：已发货");
        context.setState(SpringUtil.getBean(ShippedState.class));
    }

    @Override
    public void complete() {
        System.out.println("不能直接完成，需先发货");
    }
}
