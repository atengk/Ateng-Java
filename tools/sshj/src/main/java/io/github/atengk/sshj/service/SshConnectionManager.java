package io.github.atengk.sshj.service;

import io.github.atengk.sshj.config.SshProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SshConnectionManager implements AutoCloseable {

    private final SshProperties properties;
    private SSHClient sshClient;

    /**
     * 服务启动时初始化连接
     */
    @PostConstruct
    public void init() {
        establishConnection();
    }

    /**
     * 核心建立连接方法（支持重试）
     */
    public synchronized void establishConnection() {
        if (sshClient != null && sshClient.isConnected() && sshClient.isAuthenticated()) {
            return;
        }

        try {
            // 清理旧连接
            closeQuietly();

            log.info("Attempting to establish SSH connection to {}:{}", properties.getHost(), properties.getPort());
            sshClient = new SSHClient();
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            // 设置超时
            sshClient.setConnectTimeout(properties.getConnectTimeout());
            sshClient.setTimeout(properties.getKexTimeout());

            sshClient.connect(properties.getHost(), properties.getPort());

            if (StringUtils.hasLength(properties.getPrivateKeyPath())) {
                sshClient.authPublickey(properties.getUsername(), properties.getPrivateKeyPath());
            } else {
                sshClient.authPassword(properties.getUsername(), properties.getPassword());
            }

            // 重要：开启心跳，防止被防火墙或 Server 踢掉
            sshClient.getConnection().getKeepAlive().setKeepAliveInterval(properties.getHeartbeatInterval());

            log.info("SSH connection established successfully.");
        } catch (IOException e) {
            log.error("Failed to establish SSH connection: {}", e.getMessage());
            // 生产环境建议这里抛出自定义异常，或者触发告警
        }
    }

    /**
     * 获取当前可用的 Client，如果断开了则尝试重连
     */
    public SSHClient getClient() {
        if (sshClient == null || !sshClient.isConnected() || !sshClient.isAuthenticated()) {
            establishConnection();
        }
        return sshClient;
    }

    /**
     * 定期检查连接健康状态 (每分钟执行一次)
     */
    @Scheduled(fixedDelay = 60000)
    public void healthCheck() {
        try {
            if (sshClient != null && sshClient.isConnected() && sshClient.isAuthenticated()) {
                // 修正：增加第三个参数 new byte[0]
                // 参数含义：请求名称, 是否需要服务器响应, 请求携带的特定数据
                sshClient.getConnection().sendGlobalRequest("keepalive@openssh.com", true, new byte[0]);
                log.debug("SSH Heartbeat (Global Request) sent.");
            } else {
                establishConnection();
            }
        } catch (Exception e) {
            log.warn("SSH link is dead ({}). Reconnecting...", e.getMessage());
            establishConnection();
        }
    }

    private void closeQuietly() {
        if (sshClient != null) {
            try {
                sshClient.disconnect();
                sshClient.close();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void close() {
        closeQuietly();
    }
}