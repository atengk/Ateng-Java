package io.github.atengk.util;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;

/**
 * NodeId 工具类，用于获取当前节点的唯一标识。
 *
 * <p>
 * 节点标识用于：
 * - Redis key 前缀
 * - WebSocket 心跳检测
 * - 日志追踪
 * - RabbitMQ 消息分发等
 * </p>
 *
 * <p>
 * 获取节点 ID 的优先级：
 * 1. 环境变量 NODE_ID（推荐，适用于手动部署或有固定节点标识的集群）
 * 2. 环境变量 POD_NAME（适用于 Kubernetes 集群，需要结合 StatefulSet 保证重启不变）
 * 3. 本机 IP + server.port（兜底，单机或无法保证环境变量的情况）
 * </p>
 *
 * <p>
 * 注意事项：
 * - 如果使用 NODE_ID，每个节点必须手动设置环境变量，确保集群中唯一。
 * - 如果使用 POD_NAME，需要配合 StatefulSet，否则 Pod 重启后名字会变化，导致节点标识变化。
 * - 如果兜底使用 IP + 端口，在多副本部署下可能出现冲突，仅适用于单节点或开发环境。
 * </p>
 *
 * @author 孔余
 * @since 2026-02-01
 */
public class NodeIdUtil {

    /**
     * 环境变量优先使用
     */
    private static final String ENV_NODE_ID = "NODE_ID";

    /**
     * Kubernetes Pod 名称，需配合 StatefulSet 使用
     */
    private static final String ENV_POD_NAME = "POD_NAME";

    /**
     * 缓存节点 ID
     */
    private static volatile String NODE_ID;

    /**
     * 获取当前节点唯一标识
     *
     * @return 节点 ID
     */
    public static String getNodeId() {
        if (NODE_ID != null) {
            return NODE_ID;
        }

        synchronized (NodeIdUtil.class) {
            if (NODE_ID != null) {
                return NODE_ID;
            }

            String nodeId = resolveNodeId();
            NODE_ID = nodeId;
            return nodeId;
        }
    }

    /**
     * 解析节点 ID，按优先级返回：
     * NODE_ID > POD_NAME > IP+Port
     *
     * @return 节点 ID
     */
    private static String resolveNodeId() {
        // 优先使用环境变量 NODE_ID
        String envNodeId = System.getenv(ENV_NODE_ID);
        if (StrUtil.isNotBlank(envNodeId)) {
            return envNodeId;
        }

        // 如果是 K8s 环境，可使用 POD_NAME（必须 StatefulSet 保证重启不变）
        String podName = System.getenv(ENV_POD_NAME);
        if (StrUtil.isNotBlank(podName)) {
            return podName;
        }

        // 兜底使用本机 IP + server.port（仅用于单机或无法设置环境变量的情况）
        try {
            String ip = NetUtil.getLocalhost().getHostAddress();
            String port = SpringUtil.getProperty("server.port", "unknown");
            return ip + "." + port;
        } catch (Exception e) {
            return "unknown-node";
        }
    }

    private NodeIdUtil() {
    }
}
