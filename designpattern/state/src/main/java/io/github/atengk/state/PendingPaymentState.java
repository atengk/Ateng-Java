package io.github.atengk.state;

import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.context.OrderContext;
import org.springframework.stereotype.Component;

/**
 * 待支付状态
 */
@Component
public class PendingPaymentState implements OrderState {

    private final OrderContext context;

    public PendingPaymentState(OrderContext context) {
        this.context = context;
    }

    @Override
    public void pay() {
        System.out.println("订单已支付，状态切换为：已支付");
        context.setState(SpringUtil.getBean(PaidState.class));
    }

    @Override
    public void ship() {
        System.out.println("不能发货，订单尚未支付");
    }

    @Override
    public void complete() {
        System.out.println("不能完成订单，订单尚未支付");
    }
}
