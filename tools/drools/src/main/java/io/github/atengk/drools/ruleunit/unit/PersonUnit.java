package io.github.atengk.drools.ruleunit.unit;

import org.drools.ruleunits.api.DataSource;
import org.drools.ruleunits.api.DataStore;
import org.drools.ruleunits.api.RuleUnitData;

import java.util.HashSet;
import java.util.Set;

/**
 * 人员规则单元数据。
 *
 * @author Ateng
 * @since 2026-04-08
 */
public class PersonUnit implements RuleUnitData {

    private final DataStore<io.github.atengk.drools.ruleunit.model.Person> persons;
    private final Set<String> adultNames;

    public PersonUnit() {
        this(DataSource.createStore());
    }

    public PersonUnit(DataStore<io.github.atengk.drools.ruleunit.model.Person> persons) {
        this.persons = persons;
        this.adultNames = new HashSet<>();
    }

    public DataStore<io.github.atengk.drools.ruleunit.model.Person> getPersons() {
        return persons;
    }

    public Set<String> getAdultNames() {
        return adultNames;
    }
}