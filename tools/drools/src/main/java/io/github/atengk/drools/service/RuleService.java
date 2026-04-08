package io.github.atengk.drools.service;

import io.github.atengk.drools.model.Person;
import io.github.atengk.drools.ruleunit.PersonUnit;
import org.drools.ruleunits.api.RuleUnitInstance;
import org.drools.ruleunits.api.RuleUnitProvider;
import org.springframework.stereotype.Service;

/**
 * 规则执行服务
 *
 * @author Ateng
 * @since 2026-04-08
 */
@Service
public class RuleService {

    public void execute(Person person) {

        PersonUnit unit = new PersonUnit();
        unit.getPersons().add(person);

        RuleUnitInstance<PersonUnit> instance =
                RuleUnitProvider.get().createRuleUnitInstance(unit);

        try {
            instance.fire();
        } finally {
            instance.close();
        }
    }
}