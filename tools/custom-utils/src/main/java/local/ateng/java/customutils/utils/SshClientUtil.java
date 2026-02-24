package local.ateng.java.customutils.utils;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.core.CoreModuleProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Collections;

/**
 * SSH 客户端工具类
 * <p>
 * 支持客户端创建、会话创建、命令执行
 * 所有命令执行基于会话
 *
 * @author Ateng
 * @since 2026-02-14
 */
public final class SshClientUtil {

    /**
     * 默认连接超时时间
     */
    private static final long DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10).toMillis();

    /**
     * 默认认证超时时间
     */
    private static final long DEFAULT_AUTH_TIMEOUT = Duration.ofSeconds(10).toMillis();

    /**
     * 默认命令执行超时时间
     */
    private static final long DEFAULT_COMMAND_TIMEOUT = Duration.ofSeconds(30).toMillis();

    /**
     * 禁止实例化
     */
    private SshClientUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 创建默认 SSH 客户端
     *
     * @return SshClient
     */
    public static SshClient createDefaultClient() {
        SshClient client = SshClient.setUpDefaultClient();
        client.setServerKeyVerifier((clientSession, remoteAddress, serverKey) -> true);
        client.start();
        return client;
    }

    /**
     * 创建开启 KeepAlive 的客户端
     *
     * @param intervalSeconds 心跳间隔（秒）
     * @return SshClient
     */
    public static SshClient createKeepAliveClient(long intervalSeconds) {

        SshClient client = createDefaultClient();

        CoreModuleProperties.HEARTBEAT_INTERVAL.set(
                client,
                Duration.ofSeconds(intervalSeconds)
        );

        CoreModuleProperties.HEARTBEAT_REQUEST.set(
                client,
                "keepalive@mina.apache.org"
        );

        return client;
    }

    /**
     * 使用密码创建会话
     */
    public static ClientSession createPasswordSession(
            SshClient client,
            String host,
            int port,
            String username,
            String password
    ) throws IOException {

        ClientSession session = connect(client, host, port, username);
        session.addPasswordIdentity(password);
        auth(session);
        return session;
    }

    /**
     * 使用私钥创建会话
     */
    public static ClientSession createKeySession(
            SshClient client,
            String host,
            int port,
            String username,
            Iterable<KeyPair> keyPairs
    ) throws IOException {

        ClientSession session = connect(client, host, port, username);
        session.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(keyPairs));
        auth(session);
        return session;
    }

    /**
     * 执行命令
     *
     * @param session 会话
     * @param command 命令
     * @return 执行结果
     */
    public static String execute(
            ClientSession session,
            String command
    ) throws IOException {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayOutputStream err = new ByteArrayOutputStream();
             ClientChannel channel = session.createExecChannel(command)) {

            channel.setOut(out);
            channel.setErr(err);

            channel.open().verify(DEFAULT_COMMAND_TIMEOUT);

            channel.waitFor(
                    Collections.singleton(ClientChannelEvent.CLOSED),
                    DEFAULT_COMMAND_TIMEOUT
            );

            String stdout = new String(out.toByteArray(), StandardCharsets.UTF_8);
            String stderr = new String(err.toByteArray(), StandardCharsets.UTF_8);

            return stderr.isEmpty()
                    ? stdout
                    : stdout + System.lineSeparator() + stderr;
        }
    }

    /**
     * 关闭会话
     */
    public static void closeSession(ClientSession session) {
        if (session != null && !session.isClosed()) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 关闭客户端
     */
    public static void closeClient(SshClient client) {
        if (client != null && client.isStarted()) {
            client.stop();
        }
    }

    private static ClientSession connect(
            SshClient client,
            String host,
            int port,
            String username
    ) throws IOException {

        ConnectFuture connectFuture =
                client.connect(username, host, port)
                        .verify(DEFAULT_CONNECT_TIMEOUT);

        return connectFuture.getSession();
    }

    private static void auth(ClientSession session) throws IOException {

        AuthFuture authFuture =
                session.auth().verify(DEFAULT_AUTH_TIMEOUT);

        if (!authFuture.isSuccess()) {
            throw new IllegalStateException("SSH 认证失败");
        }
    }
}
