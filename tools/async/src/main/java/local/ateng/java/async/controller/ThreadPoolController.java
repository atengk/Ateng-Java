package local.ateng.java.async.controller;

import cn.hutool.core.thread.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

//@RestController
@RequestMapping("/thread-pool")
public class ThreadPoolController {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolController.class);

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @GetMapping("/status")
    public Map<String, Object> getThreadPoolStatus() {
        ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();

        Map<String, Object> status = new HashMap<>();
        status.put("CorePoolSize", executor.getCorePoolSize());   // 核心线程数
        status.put("MaximumPoolSize", executor.getMaximumPoolSize()); // 最大线程数
        status.put("PoolSize", executor.getPoolSize());  // 当前线程池中的线程数
        status.put("ActiveCount", executor.getActiveCount()); // 正在执行任务的线程数
        status.put("QueueSize", executor.getQueue().size());  // 任务队列中的任务数
        status.put("CompletedTaskCount", executor.getCompletedTaskCount()); // 已完成任务数
        status.put("TaskCount", executor.getTaskCount()); // 线程池曾经执行过的任务总数
        status.put("LargestPoolSize", executor.getLargestPoolSize()); // 线程池曾经达到的最大线程数

        // 记录日志，方便监控
        logger.info("线程池状态: {}", status);

        return status;
    }

    @GetMapping("/executor1")
    public void execute1() {
        logger.info("执行异步任务，线程：{}", Thread.currentThread().toString());
        taskExecutor.execute(() -> {
            logger.info("执行异步任务，线程：{}", Thread.currentThread().toString());
            ThreadUtil.sleep(5000);
        });
    }

    @GetMapping("/executor2")
    public void execute2() {
        Future<String> future = taskExecutor.submit(() -> {
            logger.info("执行异步任务，线程：{}", Thread.currentThread().toString());
            ThreadUtil.sleep(5000);
            return "任务执行完成: " + Thread.currentThread().getName();
        });
        // 获取任务执行结果
        try {
            String result = future.get(); // 阻塞等待结果
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/executor3")
    public void execute3() {
        CompletableFuture<String> future = taskExecutor.submitCompletable(() -> {
            logger.info("执行异步任务，线程：{}", Thread.currentThread().toString());
            ThreadUtil.sleep(5000);
            return "任务执行完成: " + Thread.currentThread().getName();
        });
        // 监听任务状态
        future.thenAccept(result -> System.out.println("任务成功执行，结果：" + result))
                .exceptionally(ex -> {
                    System.out.println("任务执行失败：" + ex.getMessage());
                    return null;
                });
    }

}

