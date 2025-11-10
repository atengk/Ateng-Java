# 抽象工厂模式（Abstract Factory Pattern）

抽象工厂模式是一种**创建型设计模式**，用于创建一组**相互依赖或相关的对象**，而无需指定它们的具体类。
相比普通工厂方法模式，抽象工厂是**“工厂的工厂”**——它可以生产一整套“产品族”（例如按钮+输入框+复选框等一组 UI 控件）。

在 Spring Boot 项目中，抽象工厂模式常用于：

* 多系统环境下的组件族切换（如不同品牌、系统、主题风格）
* 封装第三方 SDK 初始化逻辑（不同厂商提供的支付、短信、推送 SDK）
* 不同环境（dev、uat、prod）下的策略组合创建

---

## 一、模式思想

在抽象工厂中，每个工厂都生产一族产品（而非单个产品）。
客户端通过抽象接口创建对象，而不依赖具体实现，从而实现灵活的切换与扩展。

```
Client → AbstractFactory → ConcreteFactory
                       ↳ ProductA, ProductB ...
```

例如：

* WindowsFactory → 生产 WindowsButton、WindowsTextField
* MacFactory → 生产 MacButton、MacTextField

---

## 二、项目结构

包名统一为：`io.github.atengk.designpattern.abstractfactory`

```
io.github.atengk.designpattern.abstractfactory
├── AbstractFactoryDemoController.java
├── factory
│   ├── UIFactory.java
│   ├── WindowsFactory.java
│   └── MacFactory.java
└── product
    ├── Button.java
    ├── TextField.java
    ├── WindowsButton.java
    ├── WindowsTextField.java
    ├── MacButton.java
    └── MacTextField.java
```

---

## 三、代码实现

```java
package io.github.atengk.designpattern.abstractfactory.product;

/**
 * 抽象产品：按钮
 */
public interface Button {
    /**
     * 展示按钮
     */
    void display();
}
```

```java
package io.github.atengk.designpattern.abstractfactory.product;

/**
 * 抽象产品：输入框
 */
public interface TextField {
    /**
     * 展示输入框
     */
    void show();
}
```

```java
package io.github.atengk.designpattern.abstractfactory.product;

/**
 * 具体产品：Windows 风格按钮
 */
public class WindowsButton implements Button {
    @Override
    public void display() {
        System.out.println("显示 Windows 风格的按钮");
    }
}
```

```java
package io.github.atengk.designpattern.abstractfactory.product;

/**
 * 具体产品：Mac 风格按钮
 */
public class MacButton implements Button {
    @Override
    public void display() {
        System.out.println("显示 Mac 风格的按钮");
    }
}
```

```java
package io.github.atengk.designpattern.abstractfactory.product;

/**
 * 具体产品：Windows 风格输入框
 */
public class WindowsTextField implements TextField {
    @Override
    public void show() {
        System.out.println("显示 Windows 风格的输入框");
    }
}
```

```java
package io.github.atengk.designpattern.abstractfactory.product;

/**
 * 具体产品：Mac 风格输入框
 */
public class MacTextField implements TextField {
    @Override
    public void show() {
        System.out.println("显示 Mac 风格的输入框");
    }
}
```

```java
package io.github.atengk.designpattern.abstractfactory.factory;

import io.github.atengk.designpattern.abstractfactory.product.Button;
import io.github.atengk.designpattern.abstractfactory.product.TextField;

/**
 * 抽象工厂接口
 *
 * <p>用于创建一系列相关产品（按钮、输入框等）。</p>
 */
public interface UIFactory {
    /**
     * 创建按钮
     *
     * @return Button 实例
     */
    Button createButton();

    /**
     * 创建输入框
     *
     * @return TextField 实例
     */
    TextField createTextField();
}
```

```java
package io.github.atengk.designpattern.abstractfactory.factory;

import io.github.atengk.designpattern.abstractfactory.product.*;

/**
 * 具体工厂：Windows 风格工厂
 */
public class WindowsFactory implements UIFactory {

    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public TextField createTextField() {
        return new WindowsTextField();
    }
}
```

```java
package io.github.atengk.designpattern.abstractfactory.factory;

import io.github.atengk.designpattern.abstractfactory.product.*;

/**
 * 具体工厂：Mac 风格工厂
 */
public class MacFactory implements UIFactory {

    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public TextField createTextField() {
        return new MacTextField();
    }
}
```

---

## 四、Spring Boot 控制器演示

```java
package io.github.atengk.designpattern.abstractfactory;

import io.github.atengk.designpattern.abstractfactory.factory.MacFactory;
import io.github.atengk.designpattern.abstractfactory.factory.UIFactory;
import io.github.atengk.designpattern.abstractfactory.factory.WindowsFactory;
import io.github.atengk.designpattern.abstractfactory.product.Button;
import io.github.atengk.designpattern.abstractfactory.product.TextField;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽象工厂模式演示控制器
 *
 * <p>通过 HTTP 请求参数切换不同的 UI 工厂。</p>
 */
@RestController
@RequestMapping("/designpattern/abstractfactory")
public class AbstractFactoryDemoController {

    /**
     * 根据类型参数创建不同风格的 UI 控件
     *
     * @param type "windows" 或 "mac"
     * @return 工厂创建的产品信息
     */
    @GetMapping("/ui")
    public Map<String, Object> createUI(@RequestParam(defaultValue = "windows") String type) {
        UIFactory factory;

        if ("mac".equalsIgnoreCase(type)) {
            factory = new MacFactory();
        } else {
            factory = new WindowsFactory();
        }

        Button button = factory.createButton();
        TextField textField = factory.createTextField();

        Map<String, Object> result = new HashMap<>(4);
        result.put("factoryType", factory.getClass().getSimpleName());
        result.put("button", button.getClass().getSimpleName());
        result.put("textField", textField.getClass().getSimpleName());

        // 控制台演示效果
        button.display();
        textField.show();

        return result;
    }
}
```

---

## 五、运行效果

访问接口：

```
GET /designpattern/abstractfactory/ui?type=windows
```

返回：

```json
{
  "factoryType": "WindowsFactory",
  "button": "WindowsButton",
  "textField": "WindowsTextField"
}
```

访问：

```
GET /designpattern/abstractfactory/ui?type=mac
```

返回：

```json
{
  "factoryType": "MacFactory",
  "button": "MacButton",
  "textField": "MacTextField"
}
```

---

## 六、实践建议

| 场景                                | 建议用法                    |
| --------------------------------- | ----------------------- |
| 多主题 UI（Windows / Mac / Dark Mode） | 使用抽象工厂为每个主题提供完整控件族      |
| 多厂商 SDK（阿里 / 腾讯 / 华为）             | 每个 SDK 定义一组抽象接口，由对应工厂实现 |
| 多租户或多环境配置                         | 为不同租户提供不同的资源创建工厂        |

**优点：**

* 封装产品族创建逻辑，客户端无须关心细节；
* 能保证同一产品族之间的风格与行为一致；
* 扩展新产品族时只需新增工厂类。

**缺点：**

* 新增产品等级（例如增加“复选框”）时，必须修改所有工厂；
* 类数量较多，结构稍复杂。
