package local.ateng.java.rocketmq.listener;

import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    // 模拟订单状态记录（实际项目中可接数据库）
    private final Map<String, Boolean> orderDB = new ConcurrentHashMap<>();

    // 执行本地事务
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        System.out.println("【本地事务执行中】订单ID: " + orderId);

        try {
            // 模拟业务逻辑：成功或失败（例如模拟失败的订单）
            if (orderId.startsWith("fail")) {
                System.out.println("模拟本地事务失败，回滚消息");
                return RocketMQLocalTransactionState.ROLLBACK;
            }

            // 模拟写入“数据库”
            orderDB.put(orderId, true);
            System.out.println("本地事务成功，提交消息");
            return RocketMQLocalTransactionState.COMMIT;

        } catch (Exception e) {
            e.printStackTrace();
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }

    // MQ 事务回查逻辑（如果上面返回 UNKNOWN）
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        System.out.println("【事务回查】订单ID: " + orderId);

        Boolean status = orderDB.get(orderId);
        if (status != null && status) {
            return RocketMQLocalTransactionState.COMMIT;
        } else {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }
}

