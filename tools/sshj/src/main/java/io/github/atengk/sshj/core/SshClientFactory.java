package io.github.atengk.sshj.core;

import io.github.atengk.sshj.config.SshProperties;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;

@Component
public class SshClientFactory {

    private final SshProperties properties;

    public SshClientFactory(SshProperties properties) {
        this.properties = properties;
    }

    public SSHClient create() throws IOException {
        SSHClient sshClient = new SSHClient();

        // 1. 主机校验
        if (properties.isStrictHostKeyChecking()) {
            if (StringUtils.hasText(properties.getKnownHostsPath())) {
                sshClient.loadKnownHosts(new File(properties.getKnownHostsPath()));
            } else {
                sshClient.loadKnownHosts();
            }
        } else {
            // 开发/测试方便；生产环境建议开启 known_hosts 校验
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        }

        // 2. 连接超时
        sshClient.setConnectTimeout(properties.getConnectTimeout());

        // 3. 建立连接
        sshClient.connect(properties.getHost(), properties.getPort());

        // 4. 设置心跳
        if (properties.getHeartbeatInterval() > 0) {
            sshClient.getConnection().getKeepAlive().setKeepAliveInterval(properties.getHeartbeatInterval());
        }

        // 5. 认证
        authenticate(sshClient);

        return sshClient;
    }

    private void authenticate(SSHClient sshClient) throws IOException {
        if (!StringUtils.hasText(properties.getUsername())) {
            throw new IllegalArgumentException("sshj.username 不能为空");
        }

        // 私钥优先
        if (StringUtils.hasText(properties.getPrivateKeyPath())) {
            KeyProvider keyProvider;
            if (StringUtils.hasText(properties.getPassphrase())) {
                keyProvider = sshClient.loadKeys(properties.getPrivateKeyPath(), properties.getPassphrase());
            } else {
                keyProvider = sshClient.loadKeys(properties.getPrivateKeyPath());
            }
            sshClient.authPublickey(properties.getUsername(), keyProvider);
            return;
        }

        // 默认密码登录
        if (StringUtils.hasText(properties.getPassword())) {
            sshClient.authPassword(properties.getUsername(), properties.getPassword());
            return;
        }

        throw new IllegalArgumentException("请配置 password 或 privateKeyPath");
    }
}