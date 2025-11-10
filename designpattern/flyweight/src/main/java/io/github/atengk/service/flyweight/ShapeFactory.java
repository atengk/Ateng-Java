package io.github.atengk.service.flyweight;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 享元工厂，管理共享对象
 */
@Component
public class ShapeFactory {

    private final Map<String, Shape> circleMap = new HashMap<>();

    /**
     * 获取圆形对象
     *
     * @param type 类型
     * @return Circle 实例
     */
    public Shape getCircle(String type) {
        Shape circle = circleMap.get(type);
        if (circle == null) {
            circle = new Circle();
            circleMap.put(type, circle);
        }
        return circle;
    }

    /**
     * 获取当前缓存数量
     */
    public int getCircleCount() {
        return circleMap.size();
    }
}
