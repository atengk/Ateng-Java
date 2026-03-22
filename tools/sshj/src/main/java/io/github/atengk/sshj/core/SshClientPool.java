package io.github.atengk.sshj.core;

import io.github.atengk.sshj.config.SshProperties;
import jakarta.annotation.PreDestroy;
import net.schmizz.sshj.SSHClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SshClientPool {

    private final SshClientFactory factory;
    private final BlockingQueue<SSHClient> idleQueue;
    private final AtomicInteger createdCount = new AtomicInteger(0);
    private final int maxSize;

    public SshClientPool(SshClientFactory factory, SshProperties properties) {
        this.factory = factory;
        this.maxSize = Math.max(1, properties.getPoolSize());
        this.idleQueue = new ArrayBlockingQueue<>(this.maxSize);
    }

    public SSHClient borrow() throws IOException {
        SSHClient client = idleQueue.poll();
        if (client != null && isUsable(client)) {
            return client;
        }

        synchronized (this) {
            if (createdCount.get() < maxSize) {
                createdCount.incrementAndGet();
                return factory.create();
            }
        }

        // 池已满，等待一个可用连接
        try {
            while (true) {
                client = idleQueue.take();
                if (isUsable(client)) {
                    return client;
                }
                destroy(client);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 SSH 连接被中断", e);
        }
    }

    public void recycle(SSHClient client) {
        if (client == null) {
            return;
        }

        if (isUsable(client)) {
            boolean offered = idleQueue.offer(client);
            if (!offered) {
                destroy(client);
            }
            return;
        }

        destroy(client);
    }

    public void invalidate(SSHClient client) {
        destroy(client);
    }

    private boolean isUsable(SSHClient client) {
        return client != null && client.isConnected();
    }

    private void destroy(SSHClient client) {
        if (client == null) {
            return;
        }

        try {
            client.close();
        } catch (Exception ignored) {
            // 忽略关闭异常
        }

        synchronized (this) {
            int current = createdCount.decrementAndGet();
            if (current < 0) {
                createdCount.set(0);
            }
        }
    }

    @PreDestroy
    public void close() {
        List<SSHClient> clients = new ArrayList<SSHClient>();
        idleQueue.drainTo(clients);
        for (SSHClient client : clients) {
            destroy(client);
        }
    }
}