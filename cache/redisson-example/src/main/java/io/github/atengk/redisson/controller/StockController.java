package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.SeckillBizService;
import io.github.atengk.redisson.service.SeckillStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀库存测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final SeckillStockService seckillStockService;
    private final SeckillBizService seckillBizService;

    /**
     * 初始化库存
     *
     * curl -X POST "http://localhost:8080/stock/init?productId=1&stock=10"
     */
    @PostMapping("/init")
    public Object init(@RequestParam Long productId,
                       @RequestParam Long stock) {

        if (ObjectUtil.hasEmpty(productId, stock)) {
            return "参数不能为空";
        }

        seckillStockService.initStock(productId, stock);

        return "初始化成功";
    }

    /**
     * 秒杀
     *
     * curl -X POST "http://localhost:8080/stock/do?userId=1&productId=1"
     */
    @PostMapping("/do")
    public Object seckill(@RequestParam Long userId,
                          @RequestParam Long productId) {

        if (ObjectUtil.hasEmpty(userId, productId)) {
            return "参数不能为空";
        }

        return seckillBizService.seckill(userId, productId);
    }

    /**
     * 查询库存
     *
     * curl "http://localhost:8080/stock/get?productId=1"
     */
    @GetMapping("/get")
    public Object get(@RequestParam Long productId) {

        if (ObjectUtil.isEmpty(productId)) {
            return "productId不能为空";
        }

        return seckillStockService.getStock(productId);
    }

}