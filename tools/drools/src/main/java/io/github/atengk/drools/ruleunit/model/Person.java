package io.github.atengk.drools.ruleunit.model;

/**
 * 人员对象。
 *
 * @author Ateng
 * @since 2026-04-08
 */
public class Person {

    private String name;
    private Integer age;
    private Boolean adult;

    public Person() {
    }

    public Person(String name, Integer age) {
        this.name = name;
        this.age = age;
        this.adult = Boolean.FALSE;
    }

    public String getName() {
        return name;
    }

    public Person setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getAge() {
        return age;
    }

    public Person setAge(Integer age) {
        this.age = age;
        return this;
    }

    public Boolean getAdult() {
        return adult;
    }

    public Person setAdult(Boolean adult) {
        this.adult = adult;
        return this;
    }
}