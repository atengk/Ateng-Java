package io.github.atengk.basic;

import java.io.*;
import java.nio.file.*;

/**
 * try-with-resources 增强写法示例（JDK9+）
 */
public class TryWithResourcesExample {

    /**
     * 模拟资源（实现 AutoCloseable）
     */
    static class MyResource implements AutoCloseable {

        private final String name;

        public MyResource(String name) {
            this.name = name;
        }

        public void doWork() {
            System.out.println(name + " 执行任务");
        }

        @Override
        public void close() {
            System.out.println(name + " 已关闭");
        }
    }

    /**
     * 核心方法：try-with-resources 用法
     */
    public static void tryWithResourcesUsage() throws Exception {

        // =========================
        // 1. 传统写法（JDK7）
        // =========================
        try (BufferedReader br = new BufferedReader(new FileReader("target/test.txt"))) {
            System.out.println("读取：" + br.readLine());
        }


        // =========================
        // 2. JDK9 增强（变量可复用🔥）
        // =========================
        BufferedReader reader = new BufferedReader(new FileReader("target/test.txt"));

        // ❗ 不需要重新声明变量
        try (reader) {
            System.out.println("\nJDK9 读取：" + reader.readLine());
        }


        // =========================
        // 3. 多资源自动关闭
        // =========================
        try (
                BufferedReader br = new BufferedReader(new FileReader("target/test.txt"));
                BufferedWriter bw = new BufferedWriter(new FileWriter("target/out.txt"))
        ) {
            String line = br.readLine();
            bw.write(line);
            System.out.println("\n多资源处理完成");
        }


        // =========================
        // 4. 自定义资源（推荐理解）
        // =========================
        try (MyResource r = new MyResource("资源1")) {
            r.doWork();
        }


        // =========================
        // 5. 异常处理（自动关闭资源）
        // =========================
        try (MyResource r = new MyResource("资源2")) {
            r.doWork();
            throw new RuntimeException("业务异常");
        } catch (Exception e) {
            System.out.println("\n捕获异常：" + e.getMessage());
        }


        // =========================
        // 6. Files + try-with-resources（项目常用🔥）
        // =========================
        Path path = Path.of("target/test.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("Hello");
        }

        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.lines().forEach(System.out::println);
        }


        // =========================
        // 7. Stream 资源关闭（重要）
        // =========================
        try (var lines = Files.lines(path)) {
            lines.forEach(System.out::println);
        }


        // =========================
        // 8. 多资源 + 逻辑拆分（推荐写法）
        // =========================
        BufferedReader br = Files.newBufferedReader(path);
        BufferedWriter bw = Files.newBufferedWriter(Path.of("target/copy.txt"));

        try (br; bw) { // JDK9 写法🔥
            br.lines().forEach(line -> {
                try {
                    bw.write(line);
                    bw.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }


        // =========================
        // 9. 注意：资源必须 final / effectively final
        // =========================
        BufferedReader reader2 = Files.newBufferedReader(path);

        // reader2 = null; ❌ 如果修改就不能用于 try-with-resources

        try (reader2) {
            System.out.println("\nreader2 使用成功");
        }


        // =========================
        // 10. 项目最佳实践（模板方法）
        // =========================
        readFile(path, line -> System.out.println("处理：" + line));
    }

    /**
     * 通用读取方法（结合函数式接口）
     */
    public static void readFile(Path path, java.util.function.Consumer<String> consumer) {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            br.lines().forEach(consumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        tryWithResourcesUsage();
    }
}