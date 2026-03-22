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