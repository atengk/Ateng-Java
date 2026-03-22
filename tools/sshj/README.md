# SSHJ

**SSHJ** 是一个用于 Java 的 **SSH 客户端库**，全称通常指 **Java SSH Library（SSHJ）**，主要用于在 Java 程序中实现通过 SSH 协议进行远程操作。

- [Github](https://github.com/hierynomus/sshj)



## 基础配置

### 添加依赖

```xml
<!-- 项目属性 -->
<properties>
    <sshj.version>0.40.0</sshj.version>
</properties>
<!-- 项目依赖 -->
<dependencies>
    <!-- SSHv2 library for Java -->
    <dependency>
        <groupId>com.hierynomus</groupId>
        <artifactId>sshj</artifactId>
        <version>${sshj.version}</version>
    </dependency>
</dependencies>
```

### 配置类：`SshProperties`

```java
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
```

------

### 回调接口：`SshCallback`

```java
package io.github.atengk.sshj.core;

import net.schmizz.sshj.SSHClient;

@FunctionalInterface
public interface SshCallback<T> {

    T doInSsh(SSHClient sshClient) throws Exception;
}
```

------

### 命令执行结果：`SshCommandResult`

```java
package io.github.atengk.sshj.core;

public class SshCommandResult {

    private final String stdout;
    private final String stderr;
    private final Integer exitStatus;

    public SshCommandResult(String stdout, String stderr, Integer exitStatus) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitStatus = exitStatus;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public Integer getExitStatus() {
        return exitStatus;
    }

    @Override
    public String toString() {
        return "SshCommandResult{" +
                "stdout='" + stdout + '\'' +
                ", stderr='" + stderr + '\'' +
                ", exitStatus=" + exitStatus +
                '}';
    }
}
```

------

## SSH 配置

### 连接工厂：`SshClientFactory`

这里负责：创建 `SSHClient`、设置 host key 校验、连接、认证、心跳。SSHJ 支持 `loadKnownHosts`、`authPassword`、`authPublickey` 和 keepalive 配置；`getConnection().getKeepAlive().setKeepAliveInterval(...)` 也是 SSHJ 示例里的用法。([GitHub](https://github.com/hierynomus/sshj))

```java
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
```

------

### 简单连接池：`SshClientPool`

这个池子不依赖额外三方池库，逻辑更直观：

- `borrow()`：取一个可用连接
- `recycle()`：把连接放回池里
- `invalidate()`：坏连接直接关闭

```java
package io.github.atengk.sshj.core;

import io.github.atengk.sshj.config.SshProperties;
import jakarta.annotation.PreDestroy;
import net.schmizz.sshj.SSHClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SshClientPool {

    private final SshClientFactory factory;
    private final BlockingQueue<SSHClient> idleQueue;
    private final AtomicInteger createdCount = new AtomicInteger(0);
    private final int maxSize;

    public SshClientPool(SshClientFactory factory, SshProperties properties) {
        this.factory = factory;
        this.maxSize = Math.max(1, properties.getPoolSize());
        this.idleQueue = new ArrayBlockingQueue<>(this.maxSize);
    }

    public SSHClient borrow() throws IOException {
        SSHClient client = idleQueue.poll();
        if (client != null && isUsable(client)) {
            return client;
        }

        synchronized (this) {
            if (createdCount.get() < maxSize) {
                createdCount.incrementAndGet();
                return factory.create();
            }
        }

        // 池已满，等待一个可用连接
        try {
            while (true) {
                client = idleQueue.take();
                if (isUsable(client)) {
                    return client;
                }
                destroy(client);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("等待 SSH 连接被中断", e);
        }
    }

    public void recycle(SSHClient client) {
        if (client == null) {
            return;
        }

        if (isUsable(client)) {
            boolean offered = idleQueue.offer(client);
            if (!offered) {
                destroy(client);
            }
            return;
        }

        destroy(client);
    }

    public void invalidate(SSHClient client) {
        destroy(client);
    }

    private boolean isUsable(SSHClient client) {
        return client != null && client.isConnected();
    }

    private void destroy(SSHClient client) {
        if (client == null) {
            return;
        }

        try {
            client.close();
        } catch (Exception ignored) {
            // 忽略关闭异常
        }

        synchronized (this) {
            int current = createdCount.decrementAndGet();
            if (current < 0) {
                createdCount.set(0);
            }
        }
    }

    @PreDestroy
    public void close() {
        List<SSHClient> clients = new ArrayList<SSHClient>();
        idleQueue.drainTo(clients);
        for (SSHClient client : clients) {
            destroy(client);
        }
    }
}
```

------

### 核心模板：`SshTemplate`

这个类就是核心，和 `RedisTemplate` 很像：统一接管连接、统一执行、统一回收。

```java
package io.github.atengk.sshj.core;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class SshTemplate {

    private final SshClientPool pool;

    public SshTemplate(SshClientPool pool) {
        this.pool = pool;
    }

    /**
     * 类似 RedisTemplate.execute(...)
     * 允许你拿到原始 SSHClient 自定义操作
     */
    public <T> T execute(SshCallback<T> callback) throws Exception {
        SSHClient client = pool.borrow();
        boolean success = false;
        try {
            T result = callback.doInSsh(client);
            success = true;
            return result;
        } catch (Exception ex) {
            pool.invalidate(client);
            throw ex;
        } finally {
            if (success) {
                pool.recycle(client);
            }
        }
    }

    /**
     * 执行远程命令，返回 stdout/stderr/exitStatus
     */
    public SshCommandResult exec(final String command) throws Exception {
        return exec(command, StandardCharsets.UTF_8);
    }

    public SshCommandResult exec(final String command, final Charset charset) throws Exception {
        return execute(new SshCallback<SshCommandResult>() {
            @Override
            public SshCommandResult doInSsh(SSHClient sshClient) throws Exception {
                Session session = null;
                Session.Command remoteCommand = null;
                try {
                    session = sshClient.startSession();
                    remoteCommand = session.exec(command);

                    String stdout = readToString(remoteCommand.getInputStream(), charset);
                    String stderr = readToString(remoteCommand.getErrorStream(), charset);

                    Integer exitStatus = null;
                    try {
                        exitStatus = remoteCommand.getExitStatus();
                    } catch (Exception ignored) {
                        // 某些服务器可能不会返回 exit status
                    }

                    return new SshCommandResult(stdout, stderr, exitStatus);
                } finally {
                    if (remoteCommand != null) {
                        try {
                            remoteCommand.close();
                        } catch (Exception ignored) {
                        }
                    }
                    if (session != null) {
                        try {
                            session.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });
    }

    /**
     * 只拿 stdout
     */
    public String execForText(String command) throws Exception {
        SshCommandResult result = exec(command);
        return result.getStdout();
    }

    /**
     * 上传文件
     */
    public void upload(final String localPath, final String remotePath) throws Exception {
        execute(new SshCallback<Void>() {
            @Override
            public Void doInSsh(SSHClient sshClient) throws Exception {
                SFTPClient sftpClient = null;
                try {
                    sftpClient = sshClient.newSFTPClient();
                    sftpClient.put(localPath, remotePath);
                    return null;
                } finally {
                    if (sftpClient != null) {
                        try {
                            sftpClient.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });
    }

    /**
     * 下载文件
     */
    public void download(final String remotePath, final String localPath) throws Exception {
        execute(new SshCallback<Void>() {
            @Override
            public Void doInSsh(SSHClient sshClient) throws Exception {
                SFTPClient sftpClient = null;
                try {
                    sftpClient = sshClient.newSFTPClient();
                    sftpClient.get(remotePath, localPath);
                    return null;
                } finally {
                    if (sftpClient != null) {
                        try {
                            sftpClient.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });
    }

    /**
     * 文件是否存在
     */
    public boolean exists(final String remotePath) throws Exception {
        return execute(new SshCallback<Boolean>() {
            @Override
            public Boolean doInSsh(SSHClient sshClient) throws Exception {
                SFTPClient sftpClient = null;
                try {
                    sftpClient = sshClient.newSFTPClient();
                    return sftpClient.statExistence(remotePath) != null;
                } finally {
                    if (sftpClient != null) {
                        try {
                            sftpClient.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });
    }

    private String readToString(InputStream inputStream, Charset charset) throws IOException {
        if (inputStream == null) {
            return "";
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        return new String(outputStream.toByteArray(), charset);
    }
}
```

------

### 自动配置：`SshAutoConfiguration`

```java
package io.github.atengk.sshj.config;

import io.github.atengk.sshj.core.SshClientFactory;
import io.github.atengk.sshj.core.SshClientPool;
import io.github.atengk.sshj.core.SshTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SshAutoConfiguration {

    @Bean
    public SshClientFactory sshClientFactory(SshProperties properties) {
        return new SshClientFactory(properties);
    }

    @Bean
    public SshClientPool sshClientPool(SshClientFactory factory, SshProperties properties) {
        return new SshClientPool(factory, properties);
    }

    @Bean
    public SshTemplate sshTemplate(SshClientPool pool) {
        return new SshTemplate(pool);
    }
}
```

------

### `application.yml` 示例

```yaml
---
sshj:
  host: 192.168.1.10
  port: 38101
  username: root
  password: Admin@123
  connect-timeout: 5000
  kex-timeout: 10000
  heartbeat-interval: 30
  pool-size: 8
  strict-host-key-checking: false
  #known-hosts-path: ~/.ssh/known_hosts
  known-hosts-path: ""

```

------

## 使用示例

```java
package io.github.atengk.sshj.controller;

import io.github.atengk.sshj.core.SshCommandResult;
import io.github.atengk.sshj.core.SshTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SshController {

    @Autowired
    private SshTemplate sshTemplate;

    @GetMapping("/demo")
    public void demo() throws Exception {
        // 1. 执行命令
        SshCommandResult result = sshTemplate.exec("whoami && pwd && ls -la");
        System.out.println("stdout = " + result.getStdout());
        System.out.println("stderr = " + result.getStderr());
        System.out.println("exit = " + result.getExitStatus());

        // 2. 上传
        sshTemplate.upload("D:\\temp\\2026\\demo.png", "/tmp/demo.png");

        // 3. 下载
        sshTemplate.download("/tmp/demo.png", "D:\\temp\\2026\\demo2.png");

        // 4. 自定义操作
        String host = sshTemplate.execute(sshClient -> sshClient.getRemoteHostname());
        System.out.println(host);
    }

}

```

> 输出：
>
> 192.168.1.10
> stdout = root
> /root
> total 44
> dr-xr-x---  4 root root 4096 Mar 20 19:04 .
> drwxr-xr-x 18 root root 4096 Mar 20 07:52 ..
> -rw-r--r--  1 root root    0 Mar 20 08:17 1.txt
> -rw-r--r--  1 root root    0 Mar 20 08:17 2.txt
> -rw-------  1 root root  384 Mar 20 19:04 .bash_history
> -rw-r--r--  1 root root   18 May 10  2024 .bash_logout
> -rw-r--r--  1 root root  176 May 10  2024 .bash_profile
> -rw-r--r--  1 root root  176 May 10  2024 .bashrc
> drwx------  3 root root 4096 Mar 20 08:44 .config
> -rw-r--r--  1 root root  100 May 10  2024 .cshrc
> drwx------  2 root root 4096 Mar 20 08:33 .ssh
> -rw-r--r--  1 root root  129 May 10  2024 .tcshrc
> -rw-r--r--  1 root root   15 Jul 25  2025 test.txt
>
> stderr = 
> exit = 0
> 2026-03-22T11:59:13.094+08:00  INFO 5940 --- [sshj] [io-11017-exec-8] n.s.s.c.channel.direct.SessionChannel    : Will request `sftp` subsystem
> 2026-03-22T11:59:13.595+08:00  INFO 5940 --- [sshj] [io-11017-exec-8] n.s.s.c.channel.direct.SessionChannel    : Will request `sftp` subsystem
> 2026-03-22T11:59:13.913+08:00  WARN 5940 --- [sshj] [io-11017-exec-8] net.schmizz.sshj.xfer.FileSystemFile     : Could not set permissions for D:\temp\2026\demo2.png to 1a4
> 192.168.1.10

