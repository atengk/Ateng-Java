package local.ateng.java.redisjdk8.consumer;

import local.ateng.java.redisjdk8.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Redisson 发布/订阅的消费者（SmartLifecycle 管理）
 * 支持：
 * - Spring 启动自动订阅
 * - Spring 停止自动取消订阅
 * - 异步接收消息
 */
@Component
@RequiredArgsConstructor
public class RedissonPubSubConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RedissonPubSubConsumer.class);

    private final RedissonService redissonService;

    /**
     * 运行状态，线程安全
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 订阅的频道名称，可根据业务修改或注入配置
     */
    private static final String CHANNEL_NAME = "my_channel";

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("【Redisson Pub/Sub 消费者】开始启动，订阅频道: {}", CHANNEL_NAME);

            // 订阅频道
            redissonService.subscribe(CHANNEL_NAME, message -> {
                log.info("【Redisson Pub/Sub 消费者】收到消息: {}", message);
                process(message);
            });
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("【Redisson Pub/Sub 消费者】停止订阅频道: {}", CHANNEL_NAME);
            // 取消订阅
            redissonService.unsubscribe(CHANNEL_NAME);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 消息处理逻辑
     */
    private void process(Object message) {
        // TODO: 根据业务处理消息
        log.info("【Redisson Pub/Sub 消费者】处理消息完成: {}", message);
    }
}

