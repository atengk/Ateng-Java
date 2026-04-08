package io.github.atengk.drools.controller;

import io.github.atengk.drools.model.Person;
import io.github.atengk.drools.service.RuleService;
import org.springframework.web.bind.annotation.*;

/**
 * RuleUnit 测试接口
 *
 * @author Ateng
 * @date 2026-04-08
 */
@RestController
@RequestMapping("/rule")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping("/test")
    public String test(@RequestBody Person person) {
        ruleService.execute(person);
        return "ok";
    }
}