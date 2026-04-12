package io.github.atengk.redisson.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 秒杀业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillBizService {

    private final SeckillStockService seckillStockService;

    /**
     * 秒杀下单
     *
     * @param userId 用户ID
     * @param productId 商品ID
     */
    public String seckill(Long userId, Long productId) {

        boolean success = seckillStockService.deduct(productId);

        if (!success) {
            return "已售罄";
        }

        log.info("秒杀成功，userId={}，productId={}", userId, productId);

        return "秒杀成功";
    }

}