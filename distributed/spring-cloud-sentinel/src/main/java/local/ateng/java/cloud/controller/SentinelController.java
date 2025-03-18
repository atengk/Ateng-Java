package local.ateng.java.cloud.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/sentinel")
public class SentinelController {

    /**
     * **1. 基本限流**
     * 演示 Sentinel 对接口的 QPS 限流
     */
    @GetMapping("/hello")
    @SentinelResource(value = "hello", blockHandler = "handleBlock")
    public String hello() {
        return "Hello Sentinel!";
    }

    /**
     * **2. 熔断降级**
     * 模拟耗时请求，当响应时间过长时触发降级
     */
    @GetMapping("/slowRequest")
    @SentinelResource(value = "slowRequest", blockHandler = "handleBlock", fallback = "handleFallback")
    public String slowRequest() throws InterruptedException {
        int delay = new Random().nextInt(3); // 随机 0~2 秒延迟
        TimeUnit.SECONDS.sleep(delay);

        // **主动抛出异常，触发 fallback**
        if (delay > 1) {
            throw new RuntimeException("请求超时，触发 fallback");
        }

        return "慢请求处理完成";

    }

    /**
     * **3. 热点参数限流**
     * 对特定参数进行限流
     */
    @GetMapping("/hotParam")
    @SentinelResource(value = "hotParam", blockHandler = "handleHotParam")
    public String hotParam(@RequestParam(value = "item", required = false) String item) {
        return "热点参数请求：" + item;
    }

    /**
     * **4. 访问控制**
     * 限制特定 IP 或应用访问
     */
    @GetMapping("/auth")
    @SentinelResource(value = "auth", blockHandler = "handleBlock")
    public String auth() {
        return "访问通过";
    }

    // ==================== 限流 / 熔断 / 访问控制 处理方法 ====================

    /**
     * **限流处理方法**
     * `blockHandler`：被限流时的处理逻辑
     */
    public String handleBlock(BlockException e) {
        // 通过 ThreadLocal 方式获取 response 并设置状态码
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        if (response != null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);  // 设置 HTTP 500 错误码
        }
        return "请求被限流：" + e.getMessage();
    }

    /**
     * **熔断降级处理方法**
     * `fallback`：接口发生异常时的处理逻辑
     */
    public String handleFallback(Throwable t) {
        // 通过 ThreadLocal 方式获取 response 并设置状态码
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        if (response != null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);  // 设置 HTTP 500 错误码
        }
        return "请求被熔断：" + t.getMessage();
    }

    /**
     * **热点参数限流处理**
     */
    public String handleHotParam(String item, BlockException e) {
        // 通过 ThreadLocal 方式获取 response 并设置状态码
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        if (response != null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);  // 设置 HTTP 500 错误码
        }
        return "热点参数限流：" + item;
    }
}
