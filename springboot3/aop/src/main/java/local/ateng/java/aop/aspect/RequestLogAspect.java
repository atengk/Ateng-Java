package local.ateng.java.aop.aspect;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import local.ateng.java.aop.annotation.RequestLog;
import local.ateng.java.aop.entity.RequestLogInfo;
import local.ateng.java.aop.event.RequestLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 日志记录切面，记录 HTTP 请求和响应日志
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RequestLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    // 环绕通知：方法执行前后都记录日志
    @Around("@annotation(requestLog)")
    public Object logRequestAndResponse(ProceedingJoinPoint joinPoint, RequestLog requestLog) throws Throwable {
        // 获取 HTTP 请求信息
        RequestLogInfo logInfo = new RequestLogInfo();
        HttpServletRequest request = getHttpServletRequest();

        if (request != null) {
            // 获取请求信息
            logInfo.setUsername(getUsername(request));
            logInfo.setUrl(request.getRequestURL().toString());
            logInfo.setMethod(request.getMethod());
            logInfo.setIp(JakartaServletUtil.getClientIP(request));
            logInfo.setUserAgent(request.getHeader("User-Agent"));

            // 获取注解中配置的模块名、操作类型、操作说明
            logInfo.setModule(requestLog.module().getDescription());
            logInfo.setOperationType(requestLog.operation().getDescription());
            logInfo.setDescription(requestLog.description());

            // 记录类名和方法名（全路径）
            logInfo.setClassMethod(joinPoint.getSignature().toString());

            // 记录请求参数
            if (requestLog.logParams()) {
                logInfo.setParams(JSONUtil.toJsonStr(JakartaServletUtil.getParams(request)));
            }

            // 记录请求头
            if (requestLog.logHeaders()) {
                logInfo.setHeaders(JSONUtil.toJsonStr(JakartaServletUtil.getHeadersMap(request)));
            }

            // 记录请求体
            if (requestLog.logBody()) {
                String body = JakartaServletUtil.getBody(request);
                logInfo.setBody(StrUtil.cleanBlank(body));
            }
        }

        // 记录方法执行的开始时间
        TimeInterval timer = DateUtil.timer();
        Object result = null;
        String exceptionMessage = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            logInfo.setSuccess(true);  // 标记为成功
        } catch (Throwable throwable) {
            exceptionMessage = ExceptionUtil.stacktraceToString(throwable, 10000);
            logInfo.setSuccess(false);  // 标记为失败
            logInfo.setExceptionMessage(exceptionMessage);
            throw throwable;  // 重新抛出异常
        } finally {
            // 记录方法执行时间
            logInfo.setExecutionTime(timer.intervalPretty());

            // 记录响应数据
            if (requestLog.logResponse()) {
                logInfo.setResponse(StrUtil.subPre(String.valueOf(result), 10000));
            }

            // 如果出现异常，记录异常信息
            if (exceptionMessage != null) {
                logInfo.setExceptionMessage(exceptionMessage);
            }

            // 输出日志信息
            log.info("请求日志：{}", logInfo);

            // 发布日志事件
            eventPublisher.publishEvent(new RequestLogEvent(this, logInfo));
        }

        return result;
    }

    /**
     * 获取当前请求所属用户
     */
    private String getUsername(HttpServletRequest request) {
        String username = null;
        // 根据实际项目逻辑获取用户名
        username = RandomUtil.randomString(5);
        return username;
    }

    /**
     * 获取当前 HTTP 请求
     */
    private HttpServletRequest getHttpServletRequest() {
        HttpServletRequest request = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            request = attributes.getRequest();
        }
        return request;
    }

}
