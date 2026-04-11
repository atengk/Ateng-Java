package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RateLimitService rateLimitService;

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     */
    public void sendSms(String phone) {

        String key = "rate:sms:" + phone;

        boolean allowed = rateLimitService.tryAcquire(key, 5);

        if (!allowed) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }

        log.info("发送短信成功，phone={}", phone);

    }

}
