package io.github.atengk.drools.service;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.drools.model.Person;
import io.github.atengk.drools.model.RuleResult;
import io.github.atengk.drools.unit.PersonUnit;
import org.drools.ruleunits.api.RuleUnitInstance;
import org.drools.ruleunits.api.RuleUnitProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 人员规则执行服务。
 *
 * @author Ateng
 * @since 2026-04-08
 */
@Service
public class PersonRuleService {

    public RuleResult evaluate(List<Person> persons) {
        if (CollUtil.isEmpty(persons)) {
            return new RuleResult(Collections.emptyList(), Collections.emptySet());
        }

        PersonUnit unit = new PersonUnit();
        for (Person person : persons) {
            unit.getPersons().add(person);
        }

        RuleUnitInstance<PersonUnit> instance = RuleUnitProvider.get().createRuleUnitInstance(unit);
        try {
            instance.fire();
        } finally {
            instance.close();
        }

        return new RuleResult(persons, unit.getAdultNames());
    }
}