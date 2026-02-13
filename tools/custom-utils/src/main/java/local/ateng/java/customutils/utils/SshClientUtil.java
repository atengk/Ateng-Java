package local.ateng.java.customutils.utils;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.config.keys.loader.openssh.OpenSSHKeyPairResourceParser;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * SSH客户端工具类
 *
 * <p>
 * 基于 Apache Mina SSHD 实现
 * </p>
 *
 * <p>
 * 支持：
 * 1. 密码认证
 * 2. 私钥认证
 * 3. 命令执行
 * 4. 超时控制
 * 5. 标准输出与错误输出分离
 * 6. 批量命令执行
 * </p>
 *
 * @author Ateng
 * @since 2026-02-13
 */
public final class SshClientUtil {

    /**
     * 默认连接超时时间（毫秒）
     */
    private static final long DEFAULT_CONNECT_TIMEOUT = 10000L;

    /**
     * 默认命令执行超时时间（毫秒）
     */
    private static final long DEFAULT_COMMAND_TIMEOUT = 20000L;

    /**
     * 默认字符编码
     */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 禁止实例化
     */
    private SshClientUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * SSH连接配置
     */
    public static class SshConfig {

        private String host;

        private int port;

        private String username;

        private String password;

        private File privateKey;

        private String privateKeyPassword;

        private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;

        private long commandTimeout = DEFAULT_COMMAND_TIMEOUT;

        private Charset charset = DEFAULT_CHARSET;

        public String getHost() {
            return host;
        }

        public SshConfig setHost(String host) {
            this.host = host;
            return this;
        }

        public int getPort() {
            return port;
        }

        public SshConfig setPort(int port) {
            this.port = port;
            return this;
        }

        public String getUsername() {
            return username;
        }

        public SshConfig setUsername(String username) {
            this.username = username;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public SshConfig setPassword(String password) {
            this.password = password;
            return this;
        }

        public File getPrivateKey() {
            return privateKey;
        }

        public SshConfig setPrivateKey(File privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public String getPrivateKeyPassword() {
            return privateKeyPassword;
        }

        public SshConfig setPrivateKeyPassword(String privateKeyPassword) {
            this.privateKeyPassword = privateKeyPassword;
            return this;
        }

        public long getConnectTimeout() {
            return connectTimeout;
        }

        public SshConfig setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public long getCommandTimeout() {
            return commandTimeout;
        }

        public SshConfig setCommandTimeout(long commandTimeout) {
            this.commandTimeout = commandTimeout;
            return this;
        }

        public Charset getCharset() {
            return charset;
        }

        public SshConfig setCharset(Charset charset) {
            this.charset = charset;
            return this;
        }
    }

    /**
     * 执行单条命令
     *
     * @param config  SSH配置
     * @param command 命令
     * @return 执行结果
     */
    public static SshResult exec(SshConfig config, String command) {
        return exec(config, Collections.singletonList(command));
    }

    /**
     * 批量执行命令
     *
     * @param config   SSH配置
     * @param commands 命令列表
     * @return 执行结果
     */
    public static SshResult exec(SshConfig config, List<String> commands) {

        validateConfig(config);

        SshClient client = SshClient.setUpDefaultClient();
        client.start();

        try (ClientSession session = client
                .connect(config.getUsername(), config.getHost(), config.getPort())
                .verify(config.getConnectTimeout())
                .getSession()) {

            authenticate(session, config);

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            for (String command : commands) {

                ExecResult result = doExec(session, command, config);

                stdout.append(result.getStdout());
                stderr.append(result.getStderr());
            }

            return new SshResult(stdout.toString(), stderr.toString());

        } catch (Exception e) {
            throw new RuntimeException("SSH执行失败：" + e.getMessage(), e);
        } finally {
            client.stop();
        }
    }

    private static void authenticate(ClientSession session, SshConfig config) throws Exception {

        if (config.getPassword() != null) {
            session.addPasswordIdentity(config.getPassword());
        }

        if (config.getPrivateKey() != null) {

            Path path = config.getPrivateKey().toPath();

            FilePasswordProvider passwordProvider =
                    config.getPrivateKeyPassword() == null
                            ? FilePasswordProvider.EMPTY
                            : FilePasswordProvider.of(config.getPrivateKeyPassword());

            Collection<KeyPair> keyPairs =
                    SecurityUtils.loadKeyPairIdentities(
                            session,
                            path,
                            passwordProvider
                    );

            for (KeyPair keyPair : keyPairs) {
                session.addPublicKeyIdentity(keyPair);
            }
        }

        session.auth().verify(config.getConnectTimeout());
    }


    private static ExecResult doExec(
            ClientSession session,
            String command,
            SshConfig config
    ) throws Exception {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayOutputStream err = new ByteArrayOutputStream();
             ChannelExec channel = session.createExecChannel(command)) {

            channel.setOut(out);
            channel.setErr(err);

            channel.open().verify(config.getCommandTimeout());

            channel.waitFor(
                    EnumSet.of(ClientChannelEvent.CLOSED),
                    config.getCommandTimeout()
            );

            return new ExecResult(
                    out.toString(config.getCharset().name()),
                    err.toString(config.getCharset().name())
            );
        }
    }

    /**
     * SSH执行结果
     */
    public static class SshResult {

        private final String stdout;

        private final String stderr;

        public SshResult(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public boolean hasError() {
            return stderr != null && !stderr.isEmpty();
        }
    }

    private static class ExecResult {

        private final String stdout;

        private final String stderr;

        public ExecResult(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }
    }

    private static void validateConfig(SshConfig config) {

        if (config == null) {
            throw new IllegalArgumentException("SSH配置不能为空");
        }

        if (config.getHost() == null) {
            throw new IllegalArgumentException("SSH主机不能为空");
        }

        if (config.getUsername() == null) {
            throw new IllegalArgumentException("SSH用户名不能为空");
        }

        if (config.getPort() <= 0) {
            throw new IllegalArgumentException("SSH端口不合法");
        }
    }



}
