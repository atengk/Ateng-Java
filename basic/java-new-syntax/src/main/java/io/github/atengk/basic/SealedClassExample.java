package io.github.atengk.basic;

import java.util.List;

/**
 * sealed 类示例（JDK17+）
 */
public class SealedClassExample {

    // =========================
    // 1. 定义 sealed 父类（限制继承）
    // =========================
    public sealed interface Shape
            permits Circle, Rectangle, Triangle {
    }

    // =========================
    // 2. 子类必须声明：final / sealed / non-sealed
    // =========================
    public static final class Circle implements Shape {
        private final double radius;

        public Circle(double radius) {
            this.radius = radius;
        }

        public double radius() {
            return radius;
        }
    }

    public static final class Rectangle implements Shape {
        private final double width;
        private final double height;

        public Rectangle(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double width() {
            return width;
        }

        public double height() {
            return height;
        }
    }

    // sealed 子类（还能继续限制）
    public static sealed class Triangle implements Shape
            permits RightTriangle {
    }

    // final 子类（不能再被继承）
    public static final class RightTriangle extends Triangle {
        private final double a;
        private final double b;

        public RightTriangle(double a, double b) {
            this.a = a;
            this.b = b;
        }

        public double a() { return a; }
        public double b() { return b; }
    }

    /**
     * 核心方法：sealed 类使用
     */
    public static void sealedUsage() {

        // =========================
        // 3. 创建对象
        // =========================
        List<Shape> shapes = List.of(
                new Circle(2),
                new Rectangle(3, 4),
                new RightTriangle(3, 4)
        );

        // =========================
        // 4. switch + 类型判断（推荐🔥）
        // =========================
        for (Shape shape : shapes) {

            double area = calculateArea(shape);

            System.out.println("面积：" + area);
        }


        // =========================
        // 5. 项目场景：状态/类型控制
        // =========================
        Result result = new Success("成功数据");

        handleResult(result);
    }

    /**
     * 计算面积（结合 instanceof 模式匹配）
     */
    public static double calculateArea(Shape shape) {

        if (shape instanceof Circle c) {
            return Math.PI * c.radius() * c.radius();
        } else if (shape instanceof Rectangle r) {
            return r.width() * r.height();
        } else if (shape instanceof RightTriangle t) {
            return t.a() * t.b() / 2;
        } else {
            throw new IllegalStateException("未知类型");
        }
    }


    // =========================
    // 6. 项目实战：返回结果封装（推荐🔥）
    // =========================
    public sealed interface Result permits Success, Error {
    }

    public static final class Success implements Result {
        private final String data;

        public Success(String data) {
            this.data = data;
        }

        public String data() { return data; }
    }

    public static final class Error implements Result {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String message() { return message; }
    }

    /**
     * 统一处理结果
     */
    public static void handleResult(Result result) {

        if (result instanceof Success s) {
            System.out.println("成功：" + s.data());
        } else if (result instanceof Error e) {
            System.out.println("失败：" + e.message());
        }
    }

    public static void main(String[] args) {
        sealedUsage();
    }
}
