package io.github.atengk.basic;

import java.util.List;

/**
 * switch 模式匹配示例（JDK21🔥）
 */
public class SwitchPatternExample {

    /**
     * 示例类
     */
    static class Circle {
        double radius;
        Circle(double radius) { this.radius = radius; }
    }

    static class Rectangle {
        double width;
        double height;
        Rectangle(double w, double h) {
            this.width = w;
            this.height = h;
        }
    }

    /**
     * sealed 类型（推荐结合使用🔥）
     */
    sealed interface Shape permits CircleShape, RectShape {}

    static final class CircleShape implements Shape {
        double r;
        CircleShape(double r) { this.r = r; }
    }

    static final class RectShape implements Shape {
        double w, h;
        RectShape(double w, double h) {
            this.w = w;
            this.h = h;
        }
    }

    /**
     * 核心方法：switch 模式匹配
     */
    public static void switchPatternUsage() {

        // =========================
        // 1. 基础类型匹配🔥
        // =========================
        Object obj = "hello";

        String result = switch (obj) {
            case String s -> "字符串：" + s.toUpperCase();
            case Integer i -> "整数：" + (i + 1);
            case null -> "空值";
            default -> "未知类型";
        };

        System.out.println("结果：" + result);


        // =========================
        // 2. 多类型分支（替代 if-else）
        // =========================
        List<Object> list = List.of("A", 1, 2.5);

        for (Object o : list) {
            String res = switch (o) {
                case String s -> "String:" + s;
                case Integer i -> "Integer:" + i;
                case Double d -> "Double:" + d;
                default -> "Other";
            };
            System.out.println(res);
        }


        // =========================
        // 3. 条件守卫（when🔥）
        // =========================
        Object value = "Java";

        String msg = switch (value) {
            case String s when s.length() > 3 -> "长字符串：" + s;
            case String s -> "短字符串：" + s;
            default -> "其他";
        };

        System.out.println("条件匹配：" + msg);


        // =========================
        // 4. 结合 sealed（最佳实践🔥）
        // =========================
        Shape shape = new CircleShape(2);

        double area = switch (shape) {
            case CircleShape c -> Math.PI * c.r * c.r;
            case RectShape r -> r.w * r.h;
        };

        System.out.println("面积：" + area);


        // =========================
        // 5. null 处理（JDK21支持）
        // =========================
        Object obj2 = null;

        String res2 = switch (obj2) {
            case null -> "空对象";
            case String s -> s;
            default -> "其他";
        };

        System.out.println("null处理：" + res2);


        // =========================
        // 6. 嵌套结构处理（项目常用🔥）
        // =========================
        System.out.println(handleResult("OK"));
        System.out.println(handleResult(500));


        // =========================
        // 7. 替代 instanceof + 强转（核心价值🔥）
        // =========================
        Object input = new Rectangle(3, 4);

        double area2 = calc(input);

        System.out.println("面积2：" + area2);
    }

    /**
     * 项目实战：统一处理返回值
     */
    public static String handleResult(Object obj) {
        return switch (obj) {
            case String s -> "成功：" + s;
            case Integer code when code >= 400 -> "错误码：" + code;
            default -> "未知响应";
        };
    }

    /**
     * 计算面积（替代 instanceof）
     */
    public static double calc(Object obj) {
        return switch (obj) {
            case Circle c -> Math.PI * c.radius * c.radius;
            case Rectangle r -> r.width * r.height;
            default -> throw new IllegalArgumentException("未知类型");
        };
    }

    public static void main(String[] args) {
        switchPatternUsage();
    }
}
