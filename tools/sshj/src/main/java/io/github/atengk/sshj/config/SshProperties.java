package io.github.atengk.sshj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "sshj")
@Component
public class SshProperties {

    /**
     * SSH 主机地址
     */
    private String host;

    /**
     * SSH 端口
     */
    private int port = 22;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 密码认证
     */
    private String password;

    /**
     * 私钥路径
     */
    private String privateKeyPath;

    /**
     * 私钥密码
     */
    private String passphrase;

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * kex/transport 超时（毫秒）
     * 这里先保留给后续扩展使用
     */
    private int kexTimeout = 10000;

    /**
     * 心跳间隔（秒）
     */
    private int heartbeatInterval = 30;

    /**
     * 连接池最大连接数
     */
    private int poolSize = 8;

    /**
     * 是否严格校验 known_hosts
     */
    private boolean strictHostKeyChecking = false;

    /**
     * known_hosts 路径；为空则尝试默认 known_hosts
     */
    private String knownHostsPath;
}