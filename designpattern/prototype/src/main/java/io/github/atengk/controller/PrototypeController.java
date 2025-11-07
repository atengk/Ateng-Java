package io.github.atengk.controller;


import io.github.atengk.service.PrototypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 原型模式测试控制器
 */
@RestController
public class PrototypeController {

    private final PrototypeService prototypeService;

    public PrototypeController(PrototypeService prototypeService) {
        this.prototypeService = prototypeService;
    }

    @GetMapping("/prototype/test")
    public Object testPrototype() {
        return Stream.of("Alice", "Bob", "Charlie")
                .map(prototypeService::cloneUser)
                .collect(Collectors.toList());
    }
}
