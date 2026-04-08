package io.github.atengk.drools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 规则结果。
 *
 * @author Ateng
 * @since 2026-04-08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleResult {

    private List<Person> persons;
    private Set<String> adultNames;

}
