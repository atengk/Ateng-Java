# Drools 规则引擎

Drools 是一个用 Java 编写的开源 **业务规则管理系统（BRMS）** 和 **规则引擎（rule engine）**，目前由 Apache KIE 社区维护（Apache 孵化中）。它用于将业务逻辑从应用程序代码中分离，通过声明式规则实现自动决策与业务流程自动化。

- [官网地址](https://kie.apache.org/)



## 基础配置

**添加依赖**

```xml
<!-- 项目属性 -->
<properties>
    <drools.version>9.44.0.Final</drools.version>
</properties>
<!-- 项目依赖 -->
<dependencies>
    <!-- Drools 规则引擎 -->
    <dependency>
        <groupId>org.drools</groupId>
        <artifactId>drools-engine</artifactId>
    </dependency>

    <!-- RuleUnit 核心 -->
    <dependency>
        <groupId>org.drools</groupId>
        <artifactId>drools-ruleunits-engine</artifactId>
    </dependency>

</dependencies>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.drools</groupId>
                <artifactId>drools-bom</artifactId>
                <version>${drools.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

## 快速开始

### 创建 DRL

`src/main/resources/META-INF/ruleunits/person.drl`

```
package io.github.atengk.drools.ruleunit

import io.github.atengk.drools.model.Person
import io.github.atengk.drools.ruleunit.PersonUnit

unit PersonUnit;

rule "adult person"
when
    $p : /persons[age >= 18]
then
    System.out.println($p.getName() + " is adult");
end

rule "minor person"
when
    $p : /persons[age < 18]
then
    System.out.println($p.getName() + " is minor");
end
```

### Person 类

```java
package io.github.atengk.drools.model;

import lombok.Data;

/**
 * 人员对象
 *
 * @author Ateng
 * @date 2026-04-08
 */
@Data
public class Person {

    private String name;
    private Integer age;

}
```

### RuleUnit 数据类

```java
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
```

### 规则执行服务

```java
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
```

### 测试接口

```java
package io.github.atengk.drools.controller;

import io.github.atengk.drools.model.Person;
import io.github.atengk.drools.service.RuleService;
import org.springframework.web.bind.annotation.*;

/**
 * RuleUnit 测试接口
 *
 * @author Ateng
 * @date 2026-04-08
 */
@RestController
@RequestMapping("/rule")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping("/test")
    public String test(@RequestBody Person person) {
        ruleService.execute(person);
        return "ok";
    }
}
```

