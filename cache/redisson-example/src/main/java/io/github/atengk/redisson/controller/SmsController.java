package io.github.atengk.redisson.controller;


import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 限流测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * 发送短信验证码
     * <p>
     * curl -X POST "http://localhost:8080/sms/send?phone=13800000000"
     *
     * @param phone 手机号
     * @return 执行结果
     */
    @PostMapping("/send")
    public String send(@RequestParam String phone) {

        if (ObjectUtil.isEmpty(phone)) {
            return "phone不能为空";
        }

        try {
            smsService.sendSms(phone);
            return "发送成功";
        } catch (Exception e) {
            log.error("发送短信失败，phone={}", phone, e);
            return e.getMessage();
        }

    }

}