package io.github.atengk.state;


/**
 * 订单状态接口
 * 定义状态的行为规范
 */
public interface OrderState {

    /**
     * 支付操作
     */
    void pay();

    /**
     * 发货操作
     */
    void ship();

    /**
     * 完成订单操作
     */
    void complete();
}
