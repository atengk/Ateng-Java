package io.github.atengk.controller;

import io.github.atengk.service.flyweight.Shape;
import io.github.atengk.service.flyweight.ShapeFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 享元模式控制器演示
 */
@RestController
public class FlyweightController {

    private final ShapeFactory shapeFactory;

    public FlyweightController(ShapeFactory shapeFactory) {
        this.shapeFactory = shapeFactory;
    }

    /**
     * 绘制圆形接口
     *
     * @param x 横坐标
     * @param y 纵坐标
     * @param color 颜色
     * @return 状态提示
     */
    @GetMapping("/flyweight/draw")
    public String drawCircle(@RequestParam int x,
                             @RequestParam int y,
                             @RequestParam(defaultValue = "Red") String color) {

        Shape circle = shapeFactory.getCircle("Circle");
        circle.draw(x, y, color);
        return "绘制完成，当前共享对象数量：" + shapeFactory.getCircleCount();
    }
}
