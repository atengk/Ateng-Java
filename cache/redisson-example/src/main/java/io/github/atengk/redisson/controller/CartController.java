package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.CartBizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 购物车测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartBizService cartBizService;

    /**
     * 加入购物车
     * <p>
     * curl -X POST "http://localhost:8080/cart/add?userId=1&productId=100&count=2"
     */
    @PostMapping("/add")
    public Object add(@RequestParam Long userId,
                      @RequestParam Long productId,
                      @RequestParam Integer count) {

        if (ObjectUtil.hasEmpty(userId, productId, count)) {
            return "参数不能为空";
        }

        cartBizService.add(userId, productId, count);

        return "加入成功";
    }

    /**
     * 减少数量
     * <p>
     * curl -X POST "http://localhost:8080/cart/decrease?userId=1&productId=100&count=1"
     */
    @PostMapping("/decrease")
    public Object decrease(@RequestParam Long userId,
                           @RequestParam Long productId,
                           @RequestParam Integer count) {

        if (ObjectUtil.hasEmpty(userId, productId, count)) {
            return "参数不能为空";
        }

        cartBizService.decrease(userId, productId, count);

        return "操作成功";
    }

    /**
     * 查询购物车
     * <p>
     * curl "http://localhost:8080/cart/list?userId=1"
     */
    @GetMapping("/list")
    public Object list(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        Map<Long, Integer> result = cartBizService.list(userId);

        return result;
    }

    /**
     * 清空购物车
     * <p>
     * curl -X POST "http://localhost:8080/cart/clear?userId=1"
     */
    @PostMapping("/clear")
    public Object clear(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        cartBizService.clear(userId);

        return "清空成功";
    }

}
