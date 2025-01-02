package local.ateng.java.hutool;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * https://doc.hutool.cn/pages/Ftp
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2024-12-30
 */
public class FtpUtilTests {

    @Test
    public void ftpTest01() {
        // FTP服务器的配置信息
        String ftpHost = "192.168.1.13";
        String username = "admin";
        String password = "Admin@123";
        int port = 21; // 默认FTP端口
        String remotePath = "/tmp"; // 远程目录

        // 设置连接超时时间和读取超时时间（单位：毫秒）
        int connectTimeout = 5000; // 连接超时 5秒
        int readTimeout = 5000; // 读取超时 5秒

        // 创建FtpConfig对象，配置FTP相关信息
        FtpConfig config = FtpConfig.create()
                .setHost(ftpHost)
                .setPort(port)
                .setUser(username)
                .setPassword(password)
                .setConnectionTimeout(connectTimeout)
                .setSoTimeout(readTimeout);

        // 创建 Ftp 客户端
        try (Ftp ftp = new Ftp(config, FtpMode.Passive)) {
            // 要上传的字符串内容
            String content = "[{\"id\":56873,\"name\":\"李尚银\",\"code\":\"H-99078\",\"risk\":\"低风险\",\"town\":\"石壕镇\",\"status\":\"否\",\"createTime\":\"2024-11-12 18:00:37.073\"}]";
            // 上传后的文件名
            String fileName = "users.json";
            // 将字符串内容转换为输入流
            InputStream inputStream = IoUtil.toStream(content, CharsetUtil.CHARSET_UTF_8);
            // 上传文件
            boolean result = ftp.upload(remotePath, fileName, inputStream);
            if (result) {
                System.out.println("文件上传成功！");
            } else {
                System.out.println("文件上传失败！");
            }
            // 关闭InputStream，防止内存泄漏
            IoUtil.close(inputStream);

            // 列出文件
            List<FTPFile> ftpFiles = ftp.lsFiles(remotePath, ftpFile -> {
                Boolean ftpFilterResult = false;
                String keyword = "user";
                if (ftpFile.getName().contains(keyword) && !ftpFile.isDirectory()) {
                    ftpFilterResult = true;
                }
                return ftpFilterResult;
            });
            ftpFiles.forEach(ftpFile -> {
                System.out.println("匹配文件：" + ftpFile.getName());
            });

            // 读取文件
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ftp.download(remotePath, fileName, outputStream);
            String contentRead = outputStream.toString(CharsetUtil.CHARSET_UTF_8);
            System.out.println("读取文件内容" + contentRead);
            outputStream.close();

            // 删除文件
            boolean delled = ftp.delFile(remotePath + "/" + fileName);
            if (delled) {
                System.out.println("文件删除成功！");
            } else {
                System.out.println("文件删除失败！");
            }

            // 创建目录
            String newDir = remotePath + "/test/01";
            ftp.mkDirs(newDir);
            // 删除目录
            boolean delledDir = ftp.delDir(remotePath + "/test");
            if (delledDir) {
                System.out.println("目录删除成功！");
            } else {
                System.out.println("目录删除失败！");
            }

        } catch (IOException e) {
            System.err.println("FTP操作失败: " + e.getMessage());
        }
    }

    @Test
    void readFtp() throws IOException {
        // FTP服务器的配置信息
        String ftpHost = "192.168.1.13";
        String username = "admin";
        String password = "Admin@123";
        int port = 21; // 默认FTP端口
        String dir = "/data"; // 远程目录
        String fileName = "rrs-risk-1234567890.txt"; // 远程文件路径

        // 设置连接超时时间和读取超时时间（单位：毫秒）
        int connectTimeout = 5000; // 连接超时 5秒
        int readTimeout = 5000; // 读取超时 5秒

        // 创建FtpConfig对象，配置FTP相关信息
        FtpConfig config = FtpConfig.create()
                .setHost(ftpHost)
                .setPort(port)
                .setUser(username)
                .setPassword(password)
                .setConnectionTimeout(connectTimeout)
                .setSoTimeout(readTimeout);

        // 创建 Ftp 客户端
        Ftp ftp = null;
        try {
            ftp = new Ftp(config, FtpMode.Passive);

            // 切换到远程目录
            ftp.cd(dir);

            // 读取文件
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ftp.download(dir, fileName, byteArrayOutputStream, StandardCharsets.UTF_8);
            String content = byteArrayOutputStream.toString("UTF-8");
            System.out.println(content);
        } catch (IOException e) {
            System.out.println("FTP操作失败: " + e.getMessage());
        } finally {
            // 关闭FTP连接
            ftp.close();
        }
    }

}
