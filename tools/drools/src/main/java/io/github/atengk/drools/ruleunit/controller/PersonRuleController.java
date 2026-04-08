package io.github.atengk.drools.ruleunit.controller;

import io.github.atengk.drools.ruleunit.model.Person;
import io.github.atengk.drools.ruleunit.service.PersonRuleService;
import io.github.atengk.drools.ruleunit.service.PersonRuleService.RuleResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人员规则接口。
 *
 * @author Ateng
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/drools")
public class PersonRuleController {

    private final PersonRuleService personRuleService;

    public PersonRuleController(PersonRuleService personRuleService) {
        this.personRuleService = personRuleService;
    }

    @PostMapping("/evaluate")
    public RuleResult evaluate(@RequestBody List<Person> persons) {
        return personRuleService.evaluate(persons);
    }
}