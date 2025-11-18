package local.ateng.java.redisjdk8.controller;

import local.ateng.java.redisjdk8.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/redisson")
@RequiredArgsConstructor
public class RedissonController {
    private final RedissonService redissonService;


    /**
     * 发送消息到普通队列（非阻塞）
     *
     * @param queueKey 队列名
     * @param message  消息内容
     * @return 是否发送成功
     */
    @PostMapping("/send")
    public boolean sendMessage(@RequestParam String queueKey, @RequestBody Object message) {
        return redissonService.enqueue(queueKey, message);
    }

    /**
     * 发送消息到阻塞队列（阻塞等待队列可用位置）
     *
     * @param queueKey 队列名
     * @param message  消息内容
     * @param timeout  等待时间（秒）
     * @return 是否发送成功
     * @throws InterruptedException 阻塞等待时被中断
     */
    @PostMapping("/send/blocking")
    public boolean sendMessageBlocking(@RequestParam String queueKey,
                                       @RequestBody Object message,
                                       @RequestParam(defaultValue = "10") long timeout) throws InterruptedException {
        return redissonService.enqueueBlocking(queueKey, message, timeout, TimeUnit.SECONDS);
    }

    /**
     * 发送消息到延迟队列
     *
     * @param queueKey 队列名
     * @param message  消息内容
     * @param delay    延迟时间
     * @param unit     时间单位（默认秒）
     */
    @PostMapping("/send/delayed")
    public void sendDelayedMessage(@RequestParam String queueKey,
                                   @RequestBody Object message,
                                   @RequestParam long delay,
                                   @RequestParam(defaultValue = "SECONDS") TimeUnit unit) {
        redissonService.enqueueDelayed(queueKey, message, delay, unit);
    }

    @PostMapping("/publish")
    public String publish(@RequestParam String channel, @RequestBody Object message) {
        redissonService.publish(channel, message);
        return "消息已发布到频道: " + channel;
    }
}
