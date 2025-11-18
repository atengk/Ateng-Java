package local.ateng.java.config.runner;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class QueueConsumer implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = new Thread(() -> {
                while (running.get()) {
                    try {
                        // 模拟消费
                        System.out.println("Consuming message...");
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }, "queue-consumer-thread");

            worker.start();
            System.out.println("Consumer started!");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("Stopping consumer...");
            worker.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        // 自动启动
        return true;
    }

    @Override
    public int getPhase() {
        // 越小越早启动
        return 0;
    }
}
