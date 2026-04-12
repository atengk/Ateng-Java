package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 购物车业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Service
@RequiredArgsConstructor
public class CartBizService {

    private final CartService cartService;

    public void add(Long userId, Long productId, int count) {
        cartService.add(userId, productId, count);
    }

    public void decrease(Long userId, Long productId, int count) {
        cartService.decrease(userId, productId, count);
    }

    public Map<Long, Integer> list(Long userId) {
        return cartService.list(userId);
    }

    public void clear(Long userId) {
        cartService.clear(userId);
    }

}