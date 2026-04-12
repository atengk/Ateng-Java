package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.AccessService;
import io.github.atengk.redisson.service.BlackWhiteListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 黑白名单测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/bw")
@RequiredArgsConstructor
public class BlackWhiteController {

    private final BlackWhiteListService blackWhiteListService;
    private final AccessService accessService;

    /**
     * 加入黑名单
     *
     * curl -X POST "http://localhost:8080/bw/black/add?value=127.0.0.1"
     */
    @PostMapping("/black/add")
    public Object addBlack(@RequestParam String value) {

        if (ObjectUtil.isEmpty(value)) {
            return "value不能为空";
        }

        blackWhiteListService.addBlack(value);

        return "加入黑名单成功";
    }

    /**
     * 加入白名单
     *
     * curl -X POST "http://localhost:8080/bw/white/add?value=127.0.0.1"
     */
    @PostMapping("/white/add")
    public Object addWhite(@RequestParam String value) {

        if (ObjectUtil.isEmpty(value)) {
            return "value不能为空";
        }

        blackWhiteListService.addWhite(value);

        return "加入白名单成功";
    }

    /**
     * 访问校验
     *
     * curl "http://localhost:8080/bw/check?ip=127.0.0.1"
     */
    @GetMapping("/check")
    public Object check(@RequestParam String ip) {

        if (ObjectUtil.isEmpty(ip)) {
            return "ip不能为空";
        }

        return accessService.check(ip);
    }

}
