package io.github.atengk.drools.ruleunit.service;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.drools.ruleunit.model.Person;
import io.github.atengk.drools.ruleunit.unit.PersonUnit;
import org.drools.ruleunits.api.RuleUnitInstance;
import org.drools.ruleunits.api.RuleUnitProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
            return new RuleResult(Collections.<Person>emptyList(), Collections.<String>emptySet());
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

    /**
     * 规则结果。
     *
     * @author Ateng
     * @since 2026-04-08
     */
    public static class RuleResult {

        private List<Person> persons;
        private Set<String> adultNames;

        public RuleResult() {
        }

        public RuleResult(List<Person> persons, Set<String> adultNames) {
            this.persons = persons;
            this.adultNames = adultNames;
        }

        public List<Person> getPersons() {
            return persons;
        }

        public RuleResult setPersons(List<Person> persons) {
            this.persons = persons;
            return this;
        }

        public Set<String> getAdultNames() {
            return adultNames;
        }

        public RuleResult setAdultNames(Set<String> adultNames) {
            this.adultNames = adultNames;
            return this;
        }
    }
}