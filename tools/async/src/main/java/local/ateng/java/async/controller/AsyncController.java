package local.ateng.java.async.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/async")
@Slf4j
public class AsyncController {
    // 固定大小的线程池
    ExecutorService fixedExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
        private int count = 1;

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "MyPool-Fixed-" + count++);
            thread.setDaemon(false); // true=守护线程, false=用户线程
            thread.setPriority(Thread.MAX_PRIORITY); // 设置最大优先级
            return thread;
        }
    });
    // 可缓存的线程池
    ExecutorService cachedExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        private int count = 1;

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "MyPool-Cached-" + count++);
            thread.setDaemon(false); // true=守护线程, false=用户线程
            thread.setPriority(Thread.MAX_PRIORITY); // 设置最大优先级
            return thread;
        }
    });
    // 工作窃取算法的线程池
    ExecutorService workStealingExecutor = Executors.newWorkStealingPool(4);
    // 虚拟线程池
    ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    ExecutorService virtualExecutorCustom = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("MyVirtual-", 0).factory()
    );

    // 自定义线程池
    ExecutorService customPool = new ThreadPoolExecutor(
            2, // 核心线程数
            5, // 最大线程数
            10, // 线程空闲存活时间
            TimeUnit.SECONDS, // 存活时间单位
            new LinkedBlockingQueue<>(10), // 任务队列
            Executors.defaultThreadFactory(), // 线程工厂
            new ThreadPoolExecutor.AbortPolicy() // 拒绝策略
    );


    @GetMapping("/async1")
    public String async1() {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            String threadInfo = Thread.currentThread().toString();
            log.info("[Current thread: {}] 异步任务正在执行...", threadInfo);
            ThreadUtil.safeSleep(2000);
            log.info("[Current thread: {}] 异步任务执行完毕", threadInfo);
        });
        return DateUtil.now();
    }

    @GetMapping("/async11")
    public String async11() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            String threadInfo = Thread.currentThread().toString();
            log.info("[Current thread: {}] 异步任务正在执行...", threadInfo);
            ThreadUtil.safeSleep(2000);
            log.info("[Current thread: {}] 异步任务执行完毕", threadInfo);
            return RandomUtil.randomInt(0, 10000);
        });
        Integer result = future.get();  // 阻塞式等待
        return DateUtil.now() + "-" + result;
    }

    @GetMapping("/async12")
    public String async12() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            ThreadUtil.safeSleep(RandomUtil.randomInt(0, 10000));
            return 1;
        });
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            ThreadUtil.safeSleep(RandomUtil.randomInt(0, 10000));
            return 2;
        });
        CompletableFuture<Void> allOf = CompletableFuture.allOf(future1, future2);
        allOf.get();  // 等待所有任务完成

        return DateUtil.now();
    }

    @GetMapping("/async13")
    public String async13() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            ThreadUtil.safeSleep(RandomUtil.randomInt(0, 10000));
            return 1;
        });
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            ThreadUtil.safeSleep(RandomUtil.randomInt(0, 10000));
            return 2;
        });
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(future1, future2);
        Object result = anyOf.get();  // 获取第一个完成的任务结果
        return DateUtil.now() + "-" + result;
    }

    @GetMapping("/async2")
    public String async2() {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            String threadInfo = Thread.currentThread().toString();
            log.info("[Current thread: {}] 异步任务正在执行...", threadInfo);
            ThreadUtil.safeSleep(2000);
            log.info("[Current thread: {}] 异步任务执行完毕", threadInfo);
        }, fixedExecutor);
        return DateUtil.now();
    }

    @GetMapping("/async3")
    public String async3() {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            String threadInfo = Thread.currentThread().toString();
            log.info("[Current thread: {}] 异步任务正在执行...", threadInfo);
            ThreadUtil.safeSleep(2000);
            log.info("[Current thread: {}] 异步任务执行完毕", threadInfo);
        }, cachedExecutor);
        return DateUtil.now();
    }

    @GetMapping("/async4")
    public String async4() {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            String threadInfo = Thread.currentThread().toString();
            log.info("[Current thread: {}] 异步任务正在执行...", threadInfo);
            ThreadUtil.safeSleep(2000);
            log.info("[Current thread: {}] 异步任务执行完毕", threadInfo);
        }, workStealingExecutor);
        return DateUtil.now();
    }

    @GetMapping("/async5")
    public String async5() {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            String threadInfo = Thread.currentThread().toString();
            log.info("[Current thread: {}] 异步任务正在执行...", threadInfo);
            ThreadUtil.safeSleep(2000);
            log.info("[Current thread: {}] 异步任务执行完毕", threadInfo);
        }, virtualExecutorCustom);
        return DateUtil.now();
    }

    @GetMapping("/async6")
    public String async6() {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            String threadInfo = Thread.currentThread().toString();
            log.info("[Current thread: {}] 异步任务正在执行...", threadInfo);
            ThreadUtil.safeSleep(2000);
            log.info("[Current thread: {}] 异步任务执行完毕", threadInfo);
        }, customPool);
        return DateUtil.now();
    }

    private final ReentrantLock lock = new ReentrantLock();
    private int count = 0;
    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    @GetMapping("/thread-safety3")
    public Integer threadSafety3() {
        return atomicInteger.incrementAndGet();
    }

    @GetMapping("/thread-safety2")
    public Integer threadSafety2() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
        return count;
    }

    @GetMapping("/thread-safety1")
    public Integer threadSafety1() {
        increment();
        return count;
    }

    public synchronized void increment() {
        count++;
    }
    public void decrement() {
        synchronized (this) {
            count--;
        }
    }

}
