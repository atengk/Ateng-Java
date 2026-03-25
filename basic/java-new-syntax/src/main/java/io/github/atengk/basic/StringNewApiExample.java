package io.github.atengk.basic;

import java.util.List;

/**
 * String 新方法示例（JDK11+）
 */
public class StringNewApiExample {

    /**
     * 核心方法：String 常用新方法
     */
    public static void stringUsage() {

        // =========================
        // 1. isBlank（推荐替代 isEmpty）
        // =========================
        String str1 = "   ";
        String str2 = "";

        System.out.println("isBlank：" + str1.isBlank()); // true（空白字符也算）
        System.out.println("isEmpty：" + str2.isEmpty()); // true（仅判断长度）


        // =========================
        // 2. strip / trim（增强去空格）
        // =========================
        String str3 = "  hello  ";

        System.out.println("\ntrim：" + str3.trim());     // 老方法
        System.out.println("strip：" + str3.strip());   // 支持 Unicode 空白

        System.out.println("stripLeading：" + str3.stripLeading());
        System.out.println("stripTrailing：" + str3.stripTrailing());


        // =========================
        // 3. lines（按行拆分）
        // =========================
        String text = "Java\nPython\nGo";

        List<String> lines = text.lines().toList();

        System.out.println("\nlines：" + lines);


        // =========================
        // 4. repeat（重复字符串）
        // =========================
        String repeated = "-".repeat(10);

        System.out.println("\nrepeat：" + repeated);


        // =========================
        // 5. indent（缩进）
        // =========================
        String multiLine = "line1\nline2";

        String indented = multiLine.indent(4);

        System.out.println("\nindent：\n" + indented);


        // =========================
        // 6. transform（链式处理）
        // =========================
        String result = " hello "
                .transform(s -> s.strip().toUpperCase());

        System.out.println("transform：" + result);


        // =========================
        // 7. formatted（格式化字符串）
        // =========================
        String formatted = "姓名：%s，年龄：%d".formatted("张三", 20);

        System.out.println("\nformatted：" + formatted);


        // =========================
        // 8. 实战：过滤空字符串（推荐写法）
        // =========================
        List<String> list = List.of("A", " ", "", "B");

        List<String> filtered = list.stream()
                .filter(s -> !s.isBlank())
                .toList();

        System.out.println("\n过滤空字符串：" + filtered);


        // =========================
        // 9. 实战：多行文本处理
        // =========================
        String config = "key1=value1\nkey2=value2\nkey3=value3";

        config.lines()
                .map(line -> line.split("="))
                .forEach(arr -> System.out.println(arr[0] + " -> " + arr[1]));


        // =========================
        // 10. 实战：生成分隔线
        // =========================
        String line = "=".repeat(20);

        System.out.println("\n分隔线：\n" + line);
    }

    public static void main(String[] args) {
        stringUsage();
    }
}
