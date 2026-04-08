package io.github.atengk.drools.ruleunit;

import org.drools.ruleunits.api.DataStore;
import org.drools.ruleunits.api.DataSource;
import org.drools.ruleunits.api.RuleUnitData;
import io.github.atengk.drools.model.Person;

/**
 * 人员规则单元
 *
 * @author Ateng
 * @since 2026-04-08
 */
public class PersonUnit implements RuleUnitData {

    private final DataStore<Person> persons = DataSource.createStore();

    public DataStore<Person> getPersons() {
        return persons;
    }
}