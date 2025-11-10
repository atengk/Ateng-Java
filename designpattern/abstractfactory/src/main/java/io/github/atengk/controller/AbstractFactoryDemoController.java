package io.github.atengk.controller;

import io.github.atengk.factory.MacFactory;
import io.github.atengk.factory.UIFactory;
import io.github.atengk.factory.WindowsFactory;
import io.github.atengk.product.Button;
import io.github.atengk.product.TextField;
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
