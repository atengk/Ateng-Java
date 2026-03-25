package io.github.atengk.basic;

import java.util.List;

/**
 * Text Blocks 示例（JDK15+）
 */
public class TextBlockExample {

    /**
     * 核心方法：Text Blocks 常用用法
     */
    public static void textBlockUsage() {

        // =========================
        // 1. 基础用法（多行字符串）
        // =========================
        String json = """
                {
                    "name": "张三",
                    "age": 20
                }
                """;

        System.out.println("JSON：\n" + json);


        // =========================
        // 2. SQL（项目常用🔥）
        // =========================
        String sql = """
                SELECT id, name, age
                FROM user
                WHERE age > 18
                ORDER BY age DESC
                """;

        System.out.println("\nSQL：\n" + sql);


        // =========================
        // 3. HTML 模板
        // =========================
        String html = """
                <html>
                    <body>
                        <h1>Hello</h1>
                    </body>
                </html>
                """;

        System.out.println("\nHTML：\n" + html);


        // =========================
        // 4. 自动去缩进（重要特性）
        // =========================
        String text = """
                    line1
                    line2
                """;

        System.out.println("\n自动去缩进：\n" + text);


        // =========================
        // 5. 保留换行（\）
        // =========================
        String noNewLine = """
                line1 \
                line2 \
                line3
                """;

        System.out.println("\n取消换行：" + noNewLine);


        // =========================
        // 6. formatted（动态拼接）
        // =========================
        String name = "张三";
        int age = 20;

        String template = """
                用户信息：
                姓名：%s
                年龄：%d
                """.formatted(name, age);

        System.out.println("\n模板填充：\n" + template);


        // =========================
        // 7. 结合 String API 使用
        // =========================
        String config = """
                A
                B
                C
                """;

        List<String> list = config.lines().toList();

        System.out.println("按行拆分：" + list);


        // =========================
        // 8. 转义字符
        // =========================
        String escape = """
                \"引号\"
                \\反斜杠
                """;

        System.out.println("\n转义：\n" + escape);


        // =========================
        // 9. 实战：日志模板
        // =========================
        String logTemplate = """
                [INFO] 用户登录
                用户：%s
                时间：%s
                """.formatted("admin", java.time.LocalDateTime.now());

        System.out.println("\n日志：\n" + logTemplate);


        // =========================
        // 10. 实战：配置文件
        // =========================
        String yaml = """
                server:
                  port: 8080
                spring:
                  application:
                    name: demo
                """;

        System.out.println("\nYAML：\n" + yaml);
    }

    public static void main(String[] args) {
        textBlockUsage();
    }
}
