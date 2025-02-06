package local.ateng.java.sse.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import local.ateng.java.sse.service.SSEService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
public class SSEController {

    @Autowired
    private SSEService sseService;

    @GetMapping("/events")
    public SseEmitter handleEvents(HttpServletRequest request) {
        // 获取请求的URI
        String endpoint = request.getRequestURI();
        // 用户ID
        int userId = RandomUtil.randomInt(1, 3);
        // 传递自定义数据
        String data = "hello";
        return sseService.createSseEmitter(endpoint, String.valueOf(userId), data);
    }

    @GetMapping("/count")
    public JSONObject countEvents(@RequestParam(required = false, defaultValue = "/events") String endpoint) {
        Map<String, Integer> endpointEmitterCount = sseService.getEndpointEmitterCount();
        Map<String, Integer> userEmitterCountInEndpoint = sseService.getUserEmitterCountInEndpoint(endpoint);
        Map<String, Map<String, Integer>> userEmitterCountInAllEndpoints = sseService.getUserEmitterCountInAllEndpoints();
        return JSONObject.of(
                "endpointEmitterCount", endpointEmitterCount,
                "userEmitterCountInEndpoint", userEmitterCountInEndpoint,
                "userEmitterCountInAllEndpoints", userEmitterCountInAllEndpoints
        );
    }
}
