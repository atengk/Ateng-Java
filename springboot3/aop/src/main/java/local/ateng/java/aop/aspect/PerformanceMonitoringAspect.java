package local.ateng.java.aop.aspect;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import local.ateng.java.aop.annotation.PerformanceMonitor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP 切面类，用于监控方法执行时间
 * <p>
 * 使用 @Around 注解定义环绕通知，计算方法执行的时间。
 */
@Aspect
@Component
@Slf4j
public class PerformanceMonitoringAspect {

    /**
     * 环绕通知，拦截标记了 @PerformanceMonitor 注解的方法，计算执行时间
     *
     * @param joinPoint 连接点，表示目标方法的信息
     * @return 目标方法的返回值
     * @throws Throwable 如果目标方法抛出异常，继续抛出异常
     */
    @Around("@annotation(performanceMonitor)") // 拦截所有标记了 @PerformanceMonitor 注解的方法
    public Object monitorExecutionTime(ProceedingJoinPoint joinPoint, PerformanceMonitor performanceMonitor) throws Throwable {
        // 记录方法开始执行的时间
        TimeInterval timer = DateUtil.timer();

        // 执行目标方法
        Object result = joinPoint.proceed();

        // 计算方法执行的时间（单位：毫秒）
        long executionTimeSecond = timer.intervalSecond();
        long executionTime = timer.intervalRestart();

        // 获取目标方法的名称，并记录方法执行的时间
        String methodName = joinPoint.getSignature().toShortString();
        log.info("方法 {} 执行时间：{} 秒（{} ms）", methodName, executionTimeSecond, executionTime);

        // 返回目标方法的执行结果
        return result;
    }
}