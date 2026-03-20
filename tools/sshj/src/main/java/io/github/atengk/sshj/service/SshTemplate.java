package io.github.atengk.sshj.service;

import lombok.RequiredArgsConstructor;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SshTemplate {

    private final SshConnectionManager connectionManager;

    /**
     * 执行命令：复用 TCP 连接，只开启新的 Channel (Session)
     */
    public String execute(String command) {
        // 从长连接中开启一个新的 Session (Channel)
        try (Session session = connectionManager.getClient().startSession()) {
            Session.Command cmd = session.exec(command);
            String result = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("SSH execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * SFTP 操作：同样复用 TCP 连接
     */
    public void download(String remotePath, String localPath) {
        try (SFTPClient sftp = connectionManager.getClient().newSFTPClient()) {
            sftp.get(remotePath, new FileSystemFile(localPath));
        } catch (IOException e) {
            throw new RuntimeException("SFTP download failed", e);
        }
    }
}