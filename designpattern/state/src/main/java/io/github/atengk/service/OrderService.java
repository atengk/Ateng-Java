package io.github.atengk.service;


import io.github.atengk.context.OrderContext;
import io.github.atengk.state.PendingPaymentState;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * 订单状态服务类
 * 用于初始化和状态流转操作
 */
@Service
public class OrderService {

    private final OrderContext orderContext;
    private final ApplicationContext applicationContext;

    public OrderService(OrderContext orderContext, ApplicationContext applicationContext) {
        this.orderContext = orderContext;
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化订单状态为“待支付”
     */
    public void initOrder() {
        orderContext.setState(applicationContext.getBean(PendingPaymentState.class));
        System.out.println("订单初始化完成，当前状态：待支付");
    }

    /**
     * 执行支付操作
     */
    public void payOrder() {
        orderContext.pay();
    }

    /**
     * 执行发货操作
     */
    public void shipOrder() {
        orderContext.ship();
    }

    /**
     * 执行完成操作
     */
    public void completeOrder() {
        orderContext.complete();
    }
}