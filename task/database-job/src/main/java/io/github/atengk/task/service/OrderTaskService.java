package io.github.atengk.task.service;

import cn.hutool.json.JSONUtil;
import io.github.atengk.task.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("orderTaskService")
public class OrderTaskService {

    /**
     * 无参方法
     */
    public void noParamTask() {
        log.info("执行无参任务成功");
    }

    /**
     * 基础类型参数
     */
    public void syncOrder(Long orderId, String operator) {
        log.info("同步订单，orderId={}, operator={}", orderId, operator);
    }

    /**
     * 复杂对象参数
     */
    public void createOrder(OrderDTO dto) {
        log.info("创建订单：{}", JSONUtil.toJsonStr(dto));
    }
}
