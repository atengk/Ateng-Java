package io.github.atengk.restclient.interceptor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RestClient 拦截器：用于在请求失败时自动进行重试，并支持指数退避策略。
 *
 * <p>支持以下两类失败情况自动重试：
 * <ul>
 *     <li>网络异常（如连接超时、读取超时等）</li>
 *     <li>服务端错误（状态码 5xx）</li>
 * </ul>
 *
 * <p>支持通过静态方法 {@link #setMaxRetries(int)} 动态设置最大重试次数，具备线程安全性。
 * 默认采用指数退避策略，避免高并发下请求雪崩。
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class RetryInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 最大重试次数（支持线程安全修改）
     * 默认为 3 次
     */
    private static final AtomicInteger MAX_RETRIES = new AtomicInteger(3);

    /**
     * 初始重试等待时间（单位：毫秒），每次重试会指数增长
     */
    private static final long INITIAL_INTERVAL_MS = 300;

    /**
     * 最大重试等待时间（单位：毫秒），用于限制指数退避的上限
     */
    private static final long MAX_INTERVAL_MS = 5000;

    /**
     * 设置最大重试次数（必须大于 0）
     *
     * @param retries 新的最大重试次数
     */
    public static void setMaxRetries(int retries) {
        if (retries > 0) {
            MAX_RETRIES.set(retries);
        } else {
            log.warn("设置的最大重试次数无效：{}", retries);
        }
    }

    /**
     * 拦截请求并执行重试逻辑
     *
     * @param request   当前请求对象
     * @param body      请求体内容字节数组
     * @param execution 请求执行器，用于继续调用链
     * @return 请求响应
     * @throws IOException 当超过最大重试次数后仍然失败，则抛出异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        int attempt = 0;

        while (true) {
            try {
                // 正常执行请求
                return execution.execute(request, body);

            } catch (HttpStatusCodeException e) {
                // 处理服务端错误（5xx）重试
                if (shouldRetry(e.getStatusCode().value()) && attempt < MAX_RETRIES.get()) {
                    attempt++;
                    long waitTime = calculateBackoffTime(attempt);
                    log.warn("请求失败（状态码：{}），第 {} 次重试：{}，等待 {} 毫秒",
                            e.getStatusCode().value(), attempt, request.getURI(), waitTime);
                    sleep(waitTime);
                } else {
                    log.error("请求失败，状态码：{}，不再重试：{}", e.getStatusCode(), request.getURI());
                    throw e;
                }

            } catch (IOException e) {
                // 网络异常重试
                if (attempt < MAX_RETRIES.get()) {
                    attempt++;
                    long waitTime = calculateBackoffTime(attempt);
                    log.warn("网络异常，第 {} 次重试：{}，等待 {} 毫秒，异常信息：{}",
                            attempt, request.getURI(), waitTime, e.getMessage());
                    sleep(waitTime);
                } else {
                    log.error("网络异常，重试结束：{}，异常信息：{}", request.getURI(), e.getMessage());
                    throw e;
                }
            }
        }
    }

    /**
     * 判断当前状态码是否应当进行重试
     *
     * @param statusCode HTTP 状态码
     * @return 是否应当重试
     */
    private boolean shouldRetry(int statusCode) {
        // 默认只重试服务端错误（5xx）
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * 根据当前重试次数计算指数退避时间，并限制最大等待时间
     *
     * @param attempt 当前重试次数（从 1 开始）
     * @return 等待时间（单位：毫秒）
     */
    private long calculateBackoffTime(int attempt) {
        long waitTime = (long) (INITIAL_INTERVAL_MS * Math.pow(2, attempt - 1));
        return Math.min(waitTime, MAX_INTERVAL_MS);
    }

    /**
     * 安全执行线程等待，如果被中断则恢复线程中断状态
     *
     * @param millis 等待时间（单位：毫秒）
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

