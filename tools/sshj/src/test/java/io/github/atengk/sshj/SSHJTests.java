package io.github.atengk.sshj;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.List;

/**
 * SSHJ 基础测试类
 */
public class SSHJTests {

    /**
     * 测试 SSH 连接是否成功
     *
     * 步骤：
     * 1. 创建 SSHClient
     * 2. 允许所有主机（仅测试使用）
     * 3. 建立连接并认证
     * 4. 打印连接结果
     */
    @Test
    public void testSshConnection() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试环境使用，生产环境不建议）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);

            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试 SSH 私钥连接
     *
     * 功能：
     * 1. 使用私钥进行认证登录
     * 2. 验证连接是否成功
     */
    @Test
    public void testSshConnectionWithPrivateKey() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);

            /**
             * 使用私钥认证（无密码）
             */
            sshClient.authPublickey("root", "D:/Temp/id_ed25519");

            /**
             * 如果私钥有密码，使用如下方式
             */
            // KeyProvider keyProvider = sshClient.loadKeys("D:/Temp/id_ed25519", "私钥密码");
            // sshClient.authPublickey("root", keyProvider);

            System.out.println("连接成功");

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试 HostKeyVerifier 使用（基于 known_hosts 校验）
     *
     * 功能：
     * 1. 加载 known_hosts 文件
     * 2. 使用 HostKeyVerifier 校验服务器指纹
     * 3. 建立 SSH 连接并验证结果
     */
    @Test
    public void testHostKeyVerifierWithKnownHosts() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 加载 known_hosts 文件（推荐方式）
             * 示例路径可根据实际环境调整
             */
            sshClient.loadKnownHosts(new java.io.File("D:/Temp/known_hosts"));

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);

            /**
             * 认证（这里用密码演示，也可以换成私钥）
             */
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功（HostKey 已校验）");

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试执行单条 SSH 命令
     *
     * 功能：
     * 1. 建立 SSH 连接
     * 2. 执行指定命令
     * 3. 获取标准输出和错误输出
     * 4. 获取命令执行状态码
     */
    @Test
    public void testExecCommandBasic() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

            /**
             * 创建会话
             */
            try (Session session = sshClient.startSession()) {

                String commandStr = "ls -l";

                System.out.println("执行命令：" + commandStr);

                /**
                 * 执行命令
                 */
                Session.Command command = session.exec(commandStr);

                /**
                 * 读取标准输出
                 */
                String successOutput = new String(
                        command.getInputStream().readAllBytes()
                );

                /**
                 * 读取错误输出
                 */
                String errorOutput = new String(
                        command.getErrorStream().readAllBytes()
                );

                /**
                 * 等待命令执行完成
                 */
                command.join();

                /**
                 * 获取退出状态码
                 */
                Integer exitStatus = command.getExitStatus();

                System.out.println("====== 执行结果 ======");
                System.out.println("退出状态码：" + exitStatus);

                System.out.println("---- 标准输出 ----");
                System.out.println(successOutput);

                System.out.println("---- 错误输出 ----");
                System.out.println(errorOutput);
            }

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试 SSH Shell 交互模式
     *
     * 功能：
     * 1. 建立 SSH 连接
     * 2. 启动 Shell（交互式终端）
     * 3. 发送多条命令
     * 4. 实时读取返回结果
     */
    @Test
    public void testShellInteractive() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

            try (Session session = sshClient.startSession()) {

                /**
                 * 启动 Shell
                 */
                Session.Shell shell = session.startShell();

                InputStream inputStream = shell.getInputStream();
                OutputStream outputStream = shell.getOutputStream();

                /**
                 * 发送命令（注意必须带换行）
                 */
                outputStream.write("pwd\n".getBytes());
                outputStream.flush();

                Thread.sleep(500);

                outputStream.write("whoami\n".getBytes());
                outputStream.flush();

                Thread.sleep(500);

                outputStream.write("ls\n".getBytes());
                outputStream.flush();

                Thread.sleep(1000);

                /**
                 * 读取返回结果
                 */
                byte[] buffer = new byte[1024];
                int len;

                System.out.println("====== Shell 输出 ======");

                while (inputStream.available() > 0 && (len = inputStream.read(buffer)) != -1) {
                    System.out.print(new String(buffer, 0, len));
                }

                /**
                 * 关闭 Shell
                 */
                shell.close();
            }

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试多命令执行（每个命令独立 Session）
     *
     * 功能：
     * 1. 建立 SSH 连接
     * 2. 循环执行多条命令
     * 3. 每条命令使用独立 Session
     * 4. 打印每条命令的执行结果
     */
    @Test
    public void testExecMultiCommand() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

            String[] commands = new String[]{
                    "pwd",
                    "whoami",
                    "ls -l"
            };

            for (String commandStr : commands) {

                System.out.println("执行命令：" + commandStr);

                /**
                 * 每个命令一个独立 Session
                 */
                try (Session session = sshClient.startSession()) {

                    Session.Command command = session.exec(commandStr);

                    String successOutput = new String(
                            command.getInputStream().readAllBytes()
                    );

                    String errorOutput = new String(
                            command.getErrorStream().readAllBytes()
                    );

                    command.join();

                    Integer exitStatus = command.getExitStatus();

                    System.out.println("====== 执行结果 ======");
                    System.out.println("退出状态码：" + exitStatus);

                    System.out.println("---- 标准输出 ----");
                    System.out.println(successOutput);

                    System.out.println("---- 错误输出 ----");
                    System.out.println(errorOutput);
                }
            }

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试命令执行超时控制（修正版）
     *
     * 功能：
     * 1. 建立 SSH 连接
     * 2. 执行长时间命令
     * 3. 捕获超时异常
     * 4. 超时后主动关闭命令，防止阻塞
     */
    @Test
    public void testExecWithTimeout() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

            try (Session session = sshClient.startSession()) {

                String commandStr = "sleep 10";

                System.out.println("执行命令：" + commandStr);

                Session.Command command = session.exec(commandStr);

                long timeoutSeconds = 3;

                try {
                    /**
                     * 等待执行（带超时）
                     */
                    command.join(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

                    /**
                     * 如果执行完成
                     */
                    Integer exitStatus = command.getExitStatus();

                    System.out.println("命令执行完成");
                    System.out.println("退出状态码：" + exitStatus);

                    String successOutput = new String(command.getInputStream().readAllBytes());
                    String errorOutput = new String(command.getErrorStream().readAllBytes());

                    System.out.println("---- 标准输出 ----");
                    System.out.println(successOutput);

                    System.out.println("---- 错误输出 ----");
                    System.out.println(errorOutput);

                } catch (net.schmizz.sshj.connection.ConnectionException e) {

                    System.out.println("命令执行超时，已捕获异常");
                    System.out.println("异常信息：" + e.getMessage());

                    /**
                     * 超时后关闭命令
                     */
                    command.close();
                }
            }

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试复用 SSH 连接执行多次命令
     *
     * 功能：
     * 1. 建立一次 SSH 连接
     * 2. 多次执行命令（每次独立 Session）
     * 3. 验证连接复用，减少连接开销
     */
    @Test
    public void testReuseConnection() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

            String[] commands = new String[]{
                    "date",
                    "whoami",
                    "uptime",
                    "pwd"
            };

            /**
             * 复用同一个 SSHClient，多次执行命令
             */
            for (String commandStr : commands) {

                System.out.println("执行命令：" + commandStr);

                try (Session session = sshClient.startSession()) {

                    Session.Command command = session.exec(commandStr);

                    command.join();

                    Integer exitStatus = command.getExitStatus();

                    String successOutput = new String(
                            command.getInputStream().readAllBytes()
                    );

                    String errorOutput = new String(
                            command.getErrorStream().readAllBytes()
                    );

                    System.out.println("====== 执行结果 ======");
                    System.out.println("退出状态码：" + exitStatus);

                    System.out.println("---- 标准输出 ----");
                    System.out.println(successOutput);

                    System.out.println("---- 错误输出 ----");
                    System.out.println(errorOutput);
                }
            }

            System.out.println("所有命令执行完成（连接已复用）");

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试 Shell 持续读取输出（流式读取）
     *
     * 功能：
     * 1. 建立 SSH 连接
     * 2. 启动 Shell（交互式终端）
     * 3. 持续循环读取输出流
     * 4. 模拟真实终端实时输出
     */
    @Test
    public void testShellContinuousRead() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

            try (Session session = sshClient.startSession()) {

                Session.Shell shell = session.startShell();

                InputStream inputStream = shell.getInputStream();
                OutputStream outputStream = shell.getOutputStream();

                /**
                 * 启动读取线程（持续读取输出）
                 */
                Thread readerThread = new Thread(() -> {
                    byte[] buffer = new byte[1024];
                    int len;
                    try {
                        while ((len = inputStream.read(buffer)) != -1) {
                            System.out.print(new String(buffer, 0, len));
                        }
                    } catch (Exception e) {
                        System.out.println("读取结束：" + e.getMessage());
                    }
                });

                readerThread.setDaemon(true);
                readerThread.start();

                /**
                 * 发送命令（模拟用户输入）
                 */
                outputStream.write("top -b -n 1\n".getBytes());
                outputStream.flush();

                Thread.sleep(2000);

                outputStream.write("echo 'done'\n".getBytes());
                outputStream.flush();

                Thread.sleep(2000);

                /**
                 * 退出 Shell
                 */
                outputStream.write("exit\n".getBytes());
                outputStream.flush();

                Thread.sleep(1000);

                shell.close();
            }

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试 Shell 命令发送节奏控制（防止粘包/输出混乱）
     *
     * 功能：
     * 1. 建立 SSH 连接
     * 2. 启动 Shell（交互式终端）
     * 3. 按节奏发送命令（延迟控制）
     * 4. 验证输出顺序是否正常
     */
    @Test
    public void testShellSendCommandWithDelay() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

            try (Session session = sshClient.startSession()) {

                Session.Shell shell = session.startShell();

                InputStream inputStream = shell.getInputStream();
                OutputStream outputStream = shell.getOutputStream();

                /**
                 * 启动读取线程（持续读取输出）
                 */
                Thread readerThread = new Thread(() -> {
                    byte[] buffer = new byte[1024];
                    int len;
                    try {
                        while ((len = inputStream.read(buffer)) != -1) {
                            System.out.print(new String(buffer, 0, len));
                        }
                    } catch (Exception e) {
                        System.out.println("读取结束：" + e.getMessage());
                    }
                });

                readerThread.setDaemon(true);
                readerThread.start();

                /**
                 * 按节奏发送命令（避免粘包）
                 */
                sendCommandWithDelay(outputStream, "pwd\n", 500);
                sendCommandWithDelay(outputStream, "whoami\n", 500);
                sendCommandWithDelay(outputStream, "ls -l\n", 800);
                sendCommandWithDelay(outputStream, "echo 'done'\n", 500);

                /**
                 * 退出 Shell
                 */
                sendCommandWithDelay(outputStream, "exit\n", 500);

                Thread.sleep(1000);

                shell.close();
            }

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 发送命令并控制延迟
     *
     * @param outputStream 输出流
     * @param command      命令
     * @param delayMillis  延迟时间（毫秒）
     */
    private void sendCommandWithDelay(OutputStream outputStream, String command, long delayMillis) {
        try {
            outputStream.write(command.getBytes());
            outputStream.flush();
            Thread.sleep(delayMillis);
        } catch (Exception e) {
            throw new RuntimeException("发送命令失败", e);
        }
    }

    /**
     * 测试 Shell 退出（exit）
     *
     * 功能：
     * 1. 建立 SSH 连接
     * 2. 启动 Shell（交互式终端）
     * 3. 发送 exit 命令关闭 Shell
     * 4. 验证 Shell 和连接是否正常释放
     */
    @Test
    public void testShellExit() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);
            sshClient.authPassword("root", "Admin@123");

            System.out.println("连接成功");

            try (Session session = sshClient.startSession()) {

                Session.Shell shell = session.startShell();

                InputStream inputStream = shell.getInputStream();
                OutputStream outputStream = shell.getOutputStream();

                /**
                 * 启动读取线程（观察输出）
                 */
                Thread readerThread = new Thread(() -> {
                    byte[] buffer = new byte[1024];
                    int len;
                    try {
                        while ((len = inputStream.read(buffer)) != -1) {
                            System.out.print(new String(buffer, 0, len));
                        }
                    } catch (Exception e) {
                        System.out.println("读取结束：" + e.getMessage());
                    }
                });

                readerThread.setDaemon(true);
                readerThread.start();

                /**
                 * 等待 Shell 初始化
                 */
                Thread.sleep(500);

                System.out.println("\n发送 exit 关闭 Shell");

                /**
                 * 发送 exit 命令
                 */
                outputStream.write("exit\n".getBytes());
                outputStream.flush();

                /**
                 * 等待 Shell 关闭
                 */
                Thread.sleep(1000);

                /**
                 * 检查 Shell 是否已关闭
                 */
                boolean isClosed = shell.isOpen();

                System.out.println("Shell 是否关闭：" + !isClosed);

                shell.close();
            }

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

    /**
     * 测试密码 + 私钥双方式认证（失败回退）
     *
     * 功能：
     * 1. 建立 SSH 连接
     * 2. 优先使用私钥认证
     * 3. 私钥失败后回退为密码认证
     * 4. 提高连接成功率
     */
    @Test
    public void testPasswordAndKeyFallback() throws Exception {

        SSHClient sshClient = new SSHClient();

        try {
            /**
             * 允许所有主机（测试用）
             */
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());

            System.out.println("开始连接...");

            sshClient.connect("192.168.1.10", 38101);

            boolean authSuccess = false;

            /**
             * 尝试私钥认证
             */
            try {
                System.out.println("尝试私钥认证...");

                sshClient.authPublickey("root", "C:/Users/xxx/.ssh/id_rsa");

                authSuccess = true;
                System.out.println("私钥认证成功");

            } catch (Exception e) {
                System.out.println("私钥认证失败：" + e.getMessage());
            }

            /**
             * 私钥失败后尝试密码认证
             */
            if (!authSuccess) {
                try {
                    System.out.println("尝试密码认证...");

                    sshClient.authPassword("root", "Admin@123");

                    authSuccess = true;
                    System.out.println("密码认证成功");

                } catch (Exception e) {
                    System.out.println("密码认证失败：" + e.getMessage());
                }
            }

            /**
             * 最终认证结果
             */
            if (!authSuccess) {
                throw new RuntimeException("所有认证方式均失败");
            }

            System.out.println("连接认证成功");

        } finally {
            sshClient.disconnect();
            System.out.println("连接关闭");
        }
    }

}