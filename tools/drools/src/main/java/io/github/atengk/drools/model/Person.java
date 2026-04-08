package io.github.atengk.drools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人员对象。
 *
 * @author Ateng
 * @since 2026-04-08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Person {

    private String name;
    private Integer age;
    private Boolean adult;

}