package local.ateng.java.redisjdk8.consumer;

import local.ateng.java.redisjdk8.config.ThreadPoolManager;
import local.ateng.java.redisjdk8.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redisson 队列消费者（阻塞 & 非阻塞分开）
 */
@Component
@RequiredArgsConstructor
public class RedissonQueueConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RedissonQueueConsumer.class);

    private final RedissonService redissonService;
    private final ThreadPoolManager threadPoolManager;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final String QUEUE_KEY = "job:queue";

    /**
     * 阻塞消费超时时间（秒），防止无限阻塞
     */
    private static final long BLOCKING_POLL_TIMEOUT = 5;

    /**
     * 非阻塞轮询间隔（秒）
     */
    private static final long NON_BLOCKING_INTERVAL = 2;

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("【Redisson 队列消费者】开始启动…");

            // 阻塞消费者线程
            threadPoolManager.getExecutor("queuePool").submit(this::blockingConsumer);

            // 非阻塞消费者线程
            threadPoolManager.getExecutor("queuePool").submit(this::nonBlockingConsumer);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("【Redisson 队列消费者】收到停止指令，正在关闭…");
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
     * 阻塞消费任务
     */
    private void blockingConsumer() {
        log.info("【阻塞队列消费者】已启动，等待任务中…");
        while (running.get()) {
            try {
                Object job = redissonService.dequeueBlocking(QUEUE_KEY, BLOCKING_POLL_TIMEOUT);
                if (job != null) {
                    process(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("【阻塞队列消费者】线程被中断，准备停止…");
            } catch (Exception e) {
                log.error("【阻塞队列消费者】处理任务异常：{}", e.getMessage(), e);
            }
        }
        log.info("【阻塞队列消费者】已停止");
    }

    /**
     * 非阻塞轮询消费任务
     */
    private void nonBlockingConsumer() {
        log.info("【非阻塞队列消费者】已启动，轮询任务中…");
        while (running.get()) {
            try {
                Object job = redissonService.dequeue(QUEUE_KEY);
                if (job != null) {
                    process(job);
                } else {
                    TimeUnit.SECONDS.sleep(NON_BLOCKING_INTERVAL);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("【非阻塞队列消费者】线程被中断，准备停止…");
            } catch (Exception e) {
                log.error("【非阻塞队列消费者】处理任务异常：{}", e.getMessage(), e);
            }
        }
        log.info("【非阻塞队列消费者】已停止");
    }

    /**
     * 业务处理逻辑
     */
    private void process(Object job) {
        log.info("【队列消费者】开始处理任务：{}", job);

        // TODO: 实际业务处理逻辑
        // ...

        log.info("【队列消费者】任务处理完成：{}", job);
    }
}
