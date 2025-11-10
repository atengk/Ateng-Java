# 🧩 享元模式（Flyweight Pattern）

---

## 🌟 一、模式简介

**享元模式（Flyweight Pattern）** 是一种**结构型设计模式**，用于**减少创建对象的数量**，通过共享对象来提高内存效率。
它将对象的**内在状态（共享）**和**外在状态（不共享）**区分开，多个对象共享相同的内在状态，节省系统资源。

在 **Spring Boot** 项目中，享元模式常用于：

* 大量相似对象管理（如图形、字符、模板等）
* 缓存与对象池
* 内存优化和性能提升

---

## 🧠 二、场景举例（实战导向）

假设我们有一个「图形绘制系统」，需要渲染大量相同类型的图形（如圆形、方形）。
如果每个图形都创建独立对象，会消耗大量内存。
使用享元模式，将相同类型的图形对象共享，只保存外部参数（如位置、大小）即可。

---

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── FlyweightController.java
 ├── service/
 │    ├── flyweight/
 │    │     ├── Shape.java
 │    │     ├── Circle.java
 │    │     └── ShapeFactory.java
 └── DesignPatternApplication.java
```

---

## 💡 四、代码实现（Spring Boot 实战版）

---

### 1️⃣ 抽象享元类：`Shape`

```java
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
```

---

### 2️⃣ 具体享元类：`Circle`

```java
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
```

---

### 3️⃣ 享元工厂：`ShapeFactory`

```java
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
```

---

### 4️⃣ 控制层：`FlyweightController`

```java
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
```

---

## 🧩 五、运行效果

请求：

```
http://localhost:8080/flyweight/draw?x=10&y=20&color=Blue
```

控制台输出：

```
【绘制图形】类型：Circle, 位置：(10,20), 颜色：Blue
```

再次请求：

```
http://localhost:8080/flyweight/draw?x=30&y=50&color=Green
```

控制台输出：

```
【绘制图形】类型：Circle, 位置：(30,50), 颜色：Green
```

返回结果：

```
绘制完成，当前共享对象数量：1
```

> 💡 注意：不管绘制多少次，Circle 对象都是共享的，体现享元模式节省内存的效果。

---

## 📘 六、总结与要点

| 特性              | 说明                                              |
| --------------- | ----------------------------------------------- |
| **模式类型**        | 结构型（Structural Pattern）                         |
| **核心角色**        | 抽象享元（Shape） + 具体享元（Circle） + 享元工厂（ShapeFactory） |
| **Spring 实战应用** | 管理共享对象 Bean，通过工厂获取，减少内存开销                       |
| **适用场景**        | 系统中存在大量相似对象，可共享内在状态以优化性能                        |

