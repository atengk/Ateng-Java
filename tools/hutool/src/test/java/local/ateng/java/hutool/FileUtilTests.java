package local.ateng.java.hutool;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 文件工具类
 * https://www.hutool.cn/docs/#/core/IO/%E6%96%87%E4%BB%B6%E5%B7%A5%E5%85%B7%E7%B1%BB-FileUtil
 *
 * @author 孔余
 * @since 2024-01-17 21:34
 */
public class FileUtilTests {
    // 写入文件
    @Test
    void test01() {
        FileUtil.writeString("Hello World", "D:\\Temp\\202401\\my_user.sql", "UTF-8");
    }

    // 读取文件，并逐行处理
    @Test
    void test02() throws IOException {
        BufferedReader reader = FileUtil.getUtf8Reader("D:\\Temp\\202401\\my_user.sql");
        for (int i = 0; i < 5; i++) {
            String line = reader.readLine();
            System.out.println(line);
        }
    }

    // 读取文件为二进制，并写入到其他地方
    @Test
    void readBytes() {
        // 读取文件为二进制
        byte[] fileBytes = FileUtil.readBytes("D:\\Temp\\202409\\20240929\\tree.jpg");
        // 写入到其他地方
        FileUtil.writeBytes(fileBytes, "D:\\Temp\\202411\\20241128\\tree-2.jpg");
    }

    // 创建目录
    @Test
    void mkdir() {
        FileUtil.mkdir("D:\\Temp\\202409\\20240929");
    }


    // 判断目录或文件是否存在
    @Test
    void exist() {
        // 目录
        boolean isDirExist = FileUtil.exist("D:\\Temp\\202409\\20240929");
        System.out.println(isDirExist);
        // 文件
        boolean isFileExist = FileUtil.exist("D:\\Temp\\202409\\20240929\\1.txt");
        System.out.println(isFileExist);
    }

    // 获取文件的目录
    @Test
    void getParent() {
        String parent = FileUtil.getParent("D:\\Temp\\202409\\20240929\\1.txt", 1);
        System.out.println(parent);
    }

    // 获取文件的目录
    @Test
    void loopFiles() {
        // 指定要列出的目录路径
        String directoryPath = "D:\\Temp\\202410";

        // 使用Hutool的FileUtil来列出所有以.jpg结尾的文件
        List<File> jpgFiles = FileUtil.loopFiles(FileUtil.file(directoryPath), 1, file -> file.getName().toLowerCase().endsWith(".txt"));

        // 输出文件列表
        jpgFiles.forEach(System.out::println);

    }

    // 删除目录(递归)
    @Test
    void del() {
        boolean isDel = FileUtil.del("D:\\Temp\\202409\\20240929");
        System.out.println(isDel);
    }

}
