package io.github.atengk.receiver;

import org.springframework.stereotype.Service;

/**
 * 文件系统接收者，负责执行实际的文件操作逻辑
 */
@Service
public class FileSystemReceiver {

    public void createFile(String fileName) {
        System.out.println("正在创建文件：" + fileName);
    }

    public void deleteFile(String fileName) {
        System.out.println("正在删除文件：" + fileName);
    }
}
