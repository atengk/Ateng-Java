package io.github.atengk.sshj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "sshj")
@Component
public class SshProperties {
    private String host;
    private int port = 22;
    private String username;
    private String password;
    private String privateKeyPath; // 私钥路径
    private String passphrase;     // 私钥密码
    private int connectTimeout = 5000;
    private int kexTimeout = 10000;
    private int heartbeatInterval = 30; // 心跳间隔（秒）
}
