package io.github.atengk.controller;

import io.github.atengk.service.DoubleCheckedLockingSingleton;
import io.github.atengk.service.EnumSingleton;
import io.github.atengk.service.SpringSingletonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 单例模式演示控制器。
 *
 * <p>提供简单的 HTTP 接口以验证三种单例用法在运行时的行为。</p>
 */
@RestController
@RequestMapping("/designpattern/singleton")
public class SingletonDemoController {

    private final SpringSingletonService springSingletonService;

    /**
     * Spring 会注入 {@link SpringSingletonService}，该 Bean 在容器中为单例。
     *
     * @param springSingletonService 注入的单例服务
     */
    public SingletonDemoController(SpringSingletonService springSingletonService) {
        this.springSingletonService = springSingletonService;
    }

    /**
     * 演示 Spring 单例 Bean 的 ID 生成功能。
     *
     * @return 包含生成 ID 的 JSON 对象
     */
    @GetMapping("/spring/id")
    public Map<String, Object> springId() {
        Map<String, Object> result = new HashMap<>(4);
        long id = springSingletonService.nextId();
        result.put("type", "spring-singleton");
        result.put("id", id);
        result.put("currentId", springSingletonService.currentId());
        return result;
    }

    /**
     * 演示枚举单例的读取与修改状态。
     *
     * @return 当前枚举单例的状态
     */
    @GetMapping("/enum/value")
    public Map<String, Object> enumValue() {
        Map<String, Object> result = new HashMap<>(4);
        EnumSingleton singleton = EnumSingleton.INSTANCE;
        String before = singleton.getConfigValue();
        singleton.setConfigValue(before + "-updated");
        result.put("type", "enum-singleton");
        result.put("before", before);
        result.put("after", singleton.getConfigValue());
        return result;
    }

    /**
     * 演示 DCL 单例的简单方法调用。
     *
     * @return DCL 单例返回的示例字符串
     */
    @GetMapping("/dcl/hello")
    public Map<String, Object> dclHello() {
        Map<String, Object> result = new HashMap<>(3);
        DoubleCheckedLockingSingleton instance = DoubleCheckedLockingSingleton.getInstance();
        result.put("type", "dcl-singleton");
        result.put("message", instance.hello());
        return result;
    }

}
