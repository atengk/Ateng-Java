package io.github.atengk.state;

import io.github.atengk.context.OrderContext;
import org.springframework.stereotype.Component;

/**
 * 已完成状态
 */
@Component
public class CompletedState implements OrderState {

    public CompletedState(OrderContext context) {}

    @Override
    public void pay() {
        System.out.println("订单已完成，无法支付");
    }

    @Override
    public void ship() {
        System.out.println("订单已完成，无法发货");
    }

    @Override
    public void complete() {
        System.out.println("订单已完成，请勿重复操作");
    }
}