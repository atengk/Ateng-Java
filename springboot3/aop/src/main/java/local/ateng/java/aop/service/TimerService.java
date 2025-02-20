package local.ateng.java.aop.service;

import local.ateng.java.aop.annotation.PerformanceMonitor;
import org.springframework.stereotype.Service;

@Service
public class TimerService {

    /**
     * 模拟一个耗时操作的方法，使用 @PerformanceMonitor 注解监控执行时间。
     */
    @PerformanceMonitor  // 使用自定义注解标记需要监控执行时间的方法
    public void one() {
        try {
            Thread.sleep(2000);  // 模拟一个耗时 2 秒的操作
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 另一个示例方法，模拟不同的执行时间。
     */
    @PerformanceMonitor  // 使用自定义注解标记需要监控执行时间的方法
    public void two() {
        try {
            Thread.sleep(500);  // 模拟一个耗时 0.5 秒的操作
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

