package local.ateng.java.sse.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSEService类是一个管理Server-Sent Events（SSE）连接的服务类。
 * 它允许创建新的SSE连接，并发送数据到指定的端点和用户。
 *
 * @author 孔余
 * @since 2024-05-11 15:31:40
 */
@Component
public class SSEService {

    // 存储端点(endpoint)和对应SSE连接的Map
    private final Map<String, Map<String, CopyOnWriteArrayList<SseEmitter>>> endpointUserEmittersMap = new ConcurrentHashMap<>();

    /**
     * 创建SSE连接，并发送初始数据
     *
     * @param endpoint    端点
     * @param userId      用户ID
     * @param initialData 初始数据
     * @param <T>         数据类型
     * @return SSE连接
     */
    public <T> SseEmitter createSseEmitter(String endpoint, String userId, T initialData) {
        // 获取该端点下的用户SSE连接列表
        Map<String, CopyOnWriteArrayList<SseEmitter>> userEmittersMap = endpointUserEmittersMap.computeIfAbsent(endpoint, key -> new ConcurrentHashMap<>());

        // 创建一个SseEmitter对象，设置超时时间为永不超时
        SseEmitter emitter = new SseEmitter(-1L);

        // 发送初始数据
        try {
            emitter.send(initialData);
        } catch (IOException e) {
            // 发送失败时，完成该连接并返回
            emitter.completeWithError(e);
            return emitter;
        }

        // 将新的SSE Emitter添加到用户ID管理器中
        userEmittersMap.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        // 当连接关闭时，从用户ID管理器中移除SSE Emitter
        emitter.onCompletion(() -> {
            userEmittersMap.getOrDefault(userId, new CopyOnWriteArrayList<>()).remove(emitter);
        });

        return emitter;
    }

    /**
     * 发送数据到指定端点的指定用户的所有SSE连接
     *
     * @param endpoint 端点
     * @param userId   用户ID
     * @param data     数据
     */
    public void sendDataToUserInEndpoint(String endpoint, String userId, Object data) {
        Map<String, CopyOnWriteArrayList<SseEmitter>> userEmittersMap = endpointUserEmittersMap.getOrDefault(endpoint, new ConcurrentHashMap<>());
        // 遍历指定用户的所有SSE连接并发送数据
        for (SseEmitter emitter : userEmittersMap.getOrDefault(userId, new CopyOnWriteArrayList<>())) {
            try {
                emitter.send(data);
            } catch (IOException e) {
                // 发送失败时，完成该连接并从用户管理器中移除
                emitter.completeWithError(e);
                userEmittersMap.getOrDefault(userId, new CopyOnWriteArrayList<>()).remove(emitter);
            }
        }
    }

    /**
     * 发送数据到指定端点的指定用户列表的所有SSE连接
     *
     * @param endpoint 端点
     * @param userIds  用户ID列表
     * @param data     数据
     */
    public void sendDataToUsersInEndpoint(String endpoint, List<String> userIds, Object data) {
        for (String userId : userIds) {
            this.sendDataToUserInEndpoint(endpoint, userId, data);
        }
    }

    /**
     * 发送数据到指定端点的所有SSE连接
     *
     * @param endpoint 端点
     * @param data     数据
     */
    public void sendDataToAllUsersInEndpoint(String endpoint, Object data) {
        Map<String, CopyOnWriteArrayList<SseEmitter>> userEmittersMap = endpointUserEmittersMap.getOrDefault(endpoint, new ConcurrentHashMap<>());
        // 遍历指定端点下的所有SSE连接并发送数据
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : userEmittersMap.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(data);
                } catch (IOException e) {
                    // 发送失败时，完成该连接并从用户管理器中移除
                    emitter.completeWithError(e);
                    entry.getValue().remove(emitter);
                }
            }
        }
    }

    /**
     * 删除指定端点的指定用户的SSE连接
     *
     * @param endpoint 端点
     * @param userId   用户ID
     * @param emitter  SSE连接
     */
    public void removeUserEmitterInEndpoint(String endpoint, String userId, SseEmitter emitter) {
        Map<String, CopyOnWriteArrayList<SseEmitter>> userEmittersMap = endpointUserEmittersMap.get(endpoint);
        if (userEmittersMap != null) {
            CopyOnWriteArrayList<SseEmitter> emitters = userEmittersMap.get(userId);
            if (emitters != null) {
                emitters.remove(emitter);
            }
        }
    }

    /**
     * 删除指定端点的所有SSE连接
     *
     * @param endpoint 端点
     */
    public void removeAllEmittersInEndpoint(String endpoint) {
        endpointUserEmittersMap.remove(endpoint);
    }

    /**
     * 获取指定端点下的所有用户的SSE连接数量信息
     *
     * @param endpoint 端点
     * @return 用户ID和其对应的SSE连接数量的Map
     */
    public Map<String, Integer> getUserEmitterCountInEndpoint(String endpoint) {
        Map<String, CopyOnWriteArrayList<SseEmitter>> userEmittersMap = endpointUserEmittersMap.get(endpoint);
        Map<String, Integer> countMap = new ConcurrentHashMap<>();
        if (userEmittersMap != null) {
            // 遍历端点下的所有用户ID，并获取其对应的SSE连接数量
            for (String userId : userEmittersMap.keySet()) {
                countMap.put(userId, userEmittersMap.get(userId).size());
            }
        }
        return countMap;
    }

    /**
     * 获取所有端点下的用户的SSE连接数量信息
     *
     * @return 各端点下用户ID和其对应的SSE连接数量的Map
     */
    public Map<String, Map<String, Integer>> getUserEmitterCountInAllEndpoints() {
        Map<String, Map<String, Integer>> endpointUserCountMap = new ConcurrentHashMap<>();
        // 遍历所有端点，获取各端点下的用户的SSE连接数量信息
        for (String endpoint : endpointUserEmittersMap.keySet()) {
            endpointUserCountMap.put(endpoint, getUserEmitterCountInEndpoint(endpoint));
        }
        return endpointUserCountMap;
    }

    /**
     * 获取所有端点的SSE连接数量信息
     *
     * @return 端点和其对应的SSE连接数量的Map
     */
    public Map<String, Integer> getEndpointEmitterCount() {
        Map<String, Integer> countMap = new ConcurrentHashMap<>();
        // 遍历所有端点，获取各端点下的SSE连接数量信息并计算总数
        for (String endpoint : endpointUserEmittersMap.keySet()) {
            int totalEmitters = 0;
            for (String userId : endpointUserEmittersMap.get(endpoint).keySet()) {
                totalEmitters += endpointUserEmittersMap.get(endpoint).get(userId).size();
            }
            countMap.put(endpoint, totalEmitters);
        }
        return countMap;
    }
}

