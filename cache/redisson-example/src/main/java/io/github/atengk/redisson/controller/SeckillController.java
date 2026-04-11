package io.github.atengk.redisson.controller;


import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 秒杀接口
     *
     * curl "http://localhost:8080/seckill/do?userId=1"
     *
     * @param userId 用户ID
     * @return 执行结果
     */
    @PostMapping("/do")
    public String seckill(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        try {
            seckillService.seckill(userId);
            return "秒杀成功";
        } catch (Exception e) {
            log.error("秒杀失败，userId={}", userId, e);
            return e.getMessage();
        }

    }

}