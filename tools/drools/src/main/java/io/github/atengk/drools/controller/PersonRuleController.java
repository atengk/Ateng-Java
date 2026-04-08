package io.github.atengk.drools.controller;

import io.github.atengk.drools.model.Person;
import io.github.atengk.drools.model.RuleResult;
import io.github.atengk.drools.service.PersonRuleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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