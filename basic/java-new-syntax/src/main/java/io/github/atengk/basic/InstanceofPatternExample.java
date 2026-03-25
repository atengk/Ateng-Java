package io.github.atengk.basic;

import java.util.List;

/**
 * instanceof 模式匹配示例（JDK16+）
 */
public class InstanceofPatternExample {

    /**
     * 父类
     */
    static class Shape {}

    static class Circle extends Shape {
        double radius;
        Circle(double radius) { this.radius = radius; }
    }

    static class Rectangle extends Shape {
        double width;
        double height;
        Rectangle(double w, double h) {
            this.width = w;
            this.height = h;
        }
    }

    /**
     * 核心方法：instanceof 模式匹配
     */
    public static void instanceofUsage() {

        // =========================
        // 1. 传统写法（对比）
        // =========================
        Object obj1 = "hello";

        if (obj1 instanceof String) {
            String s = (String) obj1;
            System.out.println("传统写法：" + s.toUpperCase());
        }

        // =========================
        // 2. 新写法（推荐🔥）
        // =========================
        Object obj2 = "world";

        if (obj2 instanceof String s) {
            System.out.println("模式匹配：" + s.toUpperCase());
        }


        // =========================
        // 3. 作用域（仅在 if 内有效）
        // =========================
        Object obj3 = 123;

        if (obj3 instanceof Integer i) {
            System.out.println("数字：" + (i + 1));
        }


        // =========================
        // 4. 结合条件判断（更强🔥）
        // =========================
        Object obj4 = "Java";

        if (obj4 instanceof String s && s.length() > 3) {
            System.out.println("长度大于3：" + s);
        }


        // =========================
        // 5. 多类型判断（项目常见）
        // =========================
        List<Object> list = List.of(
                "text",
                100,
                new Circle(2),
                new Rectangle(3, 4)
        );

        for (Object obj : list) {
            handle(obj);
        }


        // =========================
        // 6. 替代复杂强转（推荐🔥）
        // =========================
        Shape shape = new Circle(3);

        double area = calcArea(shape);

        System.out.println("面积：" + area);


        // =========================
        // 7. null 安全（不会 NPE）
        // =========================
        Object obj5 = null;

        if (obj5 instanceof String s) {
            // 不会进入
            System.out.println(s);
        } else {
            System.out.println("null 不匹配");
        }


        // =========================
        // 8. 项目实战：通用处理器
        // =========================
        process("日志");
        process(123);
    }

    /**
     * 通用处理方法
     */
    public static void handle(Object obj) {
        if (obj instanceof String s) {
            System.out.println("字符串：" + s);
        } else if (obj instanceof Integer i) {
            System.out.println("整数：" + i);
        } else if (obj instanceof Circle c) {
            System.out.println("圆：" + c.radius);
        } else if (obj instanceof Rectangle r) {
            System.out.println("矩形：" + r.width + "," + r.height);
        }
    }

    /**
     * 计算面积
     */
    public static double calcArea(Shape shape) {
        if (shape instanceof Circle c) {
            return Math.PI * c.radius * c.radius;
        } else if (shape instanceof Rectangle r) {
            return r.width * r.height;
        }
        throw new IllegalArgumentException("未知类型");
    }

    /**
     * 项目示例：统一处理
     */
    public static void process(Object obj) {
        if (obj instanceof String s) {
            System.out.println("处理字符串：" + s);
        } else if (obj instanceof Integer i) {
            System.out.println("处理数字：" + i);
        } else {
            System.out.println("未知类型");
        }
    }

    public static void main(String[] args) {
        instanceofUsage();
    }
}