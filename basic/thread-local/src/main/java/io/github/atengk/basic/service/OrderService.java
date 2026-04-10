package io.github.atengk.basic.service;

import io.github.atengk.basic.holder.UserContextHolder;
import org.springframework.stereotype.Service;

/**
 * 示例业务代码
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class OrderService {

    public void createOrder() {
        Long userId = UserContextHolder.getUserId();
        System.out.println("当前用户ID：" + userId);
    }
}
