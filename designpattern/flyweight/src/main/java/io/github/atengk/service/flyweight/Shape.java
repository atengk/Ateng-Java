package io.github.atengk.service.flyweight;

/**
 * 图形接口（享元角色）
 */
public interface Shape {

    /**
     * 绘制图形
     *
     * @param x 横坐标
     * @param y 纵坐标
     * @param color 颜色
     */
    void draw(int x, int y, String color);
}
