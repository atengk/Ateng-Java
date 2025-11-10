package io.github.atengk.service.flyweight;

/**
 * 圆形实现（具体享元）
 */
public class Circle implements Shape {

    private final String type; // 内部状态：类型

    public Circle() {
        this.type = "Circle"; // 固定类型，共享对象
    }

    @Override
    public void draw(int x, int y, String color) {
        System.out.println("【绘制图形】类型：" + type + ", 位置：(" + x + "," + y + "), 颜色：" + color);
    }
}