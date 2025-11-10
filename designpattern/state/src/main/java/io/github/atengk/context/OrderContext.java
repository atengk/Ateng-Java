package io.github.atengk.context;


import io.github.atengk.state.OrderState;
import org.springframework.stereotype.Component;

/**
 * 订单上下文类
 * 保存当前状态，并将操作委托给具体状态实现
 */
@Component
public class OrderContext {

    private OrderState currentState;

    /**
     * 设置当前状态
     *
     * @param state 状态实例
     */
    public void setState(OrderState state) {
        this.currentState = state;
    }

    /**
     * 支付操作
     */
    public void pay() {
        currentState.pay();
    }

    /**
     * 发货操作
     */
    public void ship() {
        currentState.ship();
    }

    /**
     * 完成订单操作
     */
    public void complete() {
        currentState.complete();
    }
}
