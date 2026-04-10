package io.github.atengk.basic.service;

import io.github.atengk.basic.holder.IdempotentTokenContextHolder;
import org.springframework.stereotype.Service;

/**
 * 示例业务类（使用幂等 Token）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class OrderIdempotentService {

    public String createOrder() {
        String token = IdempotentTokenContextHolder.get();
        return "订单创建成功，Token：" + token;
    }
}
