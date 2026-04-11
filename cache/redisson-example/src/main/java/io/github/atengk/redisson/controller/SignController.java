package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.UserSignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 签到测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/sign")
@RequiredArgsConstructor
public class SignController {

    private final UserSignService userSignService;

    /**
     * 签到
     *
     * curl -X POST "http://localhost:8080/sign/do?userId=1"
     */
    @PostMapping("/do")
    public Object sign(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        userSignService.sign(userId);

        return "签到成功";
    }

    /**
     * 是否签到
     *
     * curl "http://localhost:8080/sign/check?userId=1"
     */
    @GetMapping("/check")
    public Object check(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return userSignService.isSigned(userId);
    }

    /**
     * 签到总天数
     *
     * curl "http://localhost:8080/sign/count?userId=1"
     */
    @GetMapping("/count")
    public Object count(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return userSignService.count(userId);
    }

    /**
     * 连续签到天数
     *
     * curl "http://localhost:8080/sign/continuous?userId=1"
     */
    @GetMapping("/continuous")
    public Object continuous(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return userSignService.continuous(userId);
    }

}