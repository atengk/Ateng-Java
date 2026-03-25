package io.github.atengk.basic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * Files 新 API 示例（JDK11+）
 */
public class FilesExample {

    /**
     * 核心方法：Files 常用操作
     */
    public static void filesUsage() throws IOException {

        // =========================
        // 1. 创建文件路径
        // =========================
        Path path = Path.of("target/test.txt");

        // =========================
        // 2. 写入字符串（最常用🔥）
        // =========================
        Files.writeString(path, "Hello Java21", StandardCharsets.UTF_8);

        System.out.println("写入完成");


        // =========================
        // 3. 读取字符串
        // =========================
        String content = Files.readString(path, StandardCharsets.UTF_8);

        System.out.println("\n读取内容：" + content);


        // =========================
        // 4. 写入多行数据
        // =========================
        List<String> lines = List.of("A", "B", "C");

        Files.write(path, lines, StandardCharsets.UTF_8);

        System.out.println("\n写入多行完成");


        // =========================
        // 5. 读取多行数据
        // =========================
        List<String> readLines = Files.readAllLines(path, StandardCharsets.UTF_8);

        System.out.println("读取多行：" + readLines);


        // =========================
        // 6. 追加写入
        // =========================
        Files.writeString(
                path,
                "\n追加内容",
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND
        );

        System.out.println("\n追加完成");


        // =========================
        // 7. 判断文件是否存在
        // =========================
        boolean exists = Files.exists(path);

        System.out.println("\n文件是否存在：" + exists);


        // =========================
        // 8. 创建目录
        // =========================
        Path dir = Path.of("target/tempDir");

        Files.createDirectories(dir);

        System.out.println("目录创建完成");


        // =========================
        // 9. 复制文件
        // =========================
        Path target = Path.of("target/copy.txt");

        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("复制完成");


        // =========================
        // 10. 移动文件
        // =========================
        Path moved = Path.of("target/moved.txt");

        Files.move(target, moved, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("移动完成");


        // =========================
        // 11. 删除文件
        // =========================
        Files.deleteIfExists(moved);

        System.out.println("删除完成");


        // =========================
        // 12. 遍历目录（Stream🔥）
        // =========================
        Files.list(Path.of("target/"))
                .filter(Files::isRegularFile)
                .forEach(p -> System.out.println("文件：" + p));


        // =========================
        // 13. 查找文件（递归）
        // =========================
        Files.walk(Path.of("target/"))
                .filter(p -> p.toString().endsWith(".txt"))
                .forEach(p -> System.out.println("txt文件：" + p));


        // =========================
        // 14. 文件大小
        // =========================
        long size = Files.size(path);

        System.out.println("\n文件大小：" + size + " 字节");


        // =========================
        // 15. 临时文件（项目常用）
        // =========================
        Path tempFile = Files.createTempFile("demo", ".txt");

        System.out.println("临时文件：" + tempFile);


        // =========================
        // 16. 权限判断
        // =========================
        System.out.println("可读：" + Files.isReadable(path));
        System.out.println("可写：" + Files.isWritable(path));


        // =========================
        // 17. 读取为流（大文件推荐🔥）
        // =========================
        try (var stream = Files.lines(path)) {
            stream.forEach(System.out::println);
        }


        // =========================
        // 18. 一次性写入（覆盖）
        // =========================
        Files.writeString(path, "覆盖写入");

        System.out.println("\n覆盖写入完成");
    }

    public static void main(String[] args) throws IOException {
        filesUsage();
    }
}