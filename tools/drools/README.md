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
    <!-- Drools 9 RuleUnit 核心执行引擎依赖 -->
    <!-- 用于支持 RuleUnit（规则单元）模式的执行 -->
    <dependency>
        <groupId>org.drools</groupId>
        <artifactId>drools-ruleunits-engine</artifactId>
        <version>${drools.version}</version>
    </dependency>

    <!-- KIE Maven 插件 -->
    <!-- 作用：在 Maven 构建阶段对 DRL 规则进行编译、校验与打包 -->
    <!-- 用于生成 KIE 模块（KieModule），确保规则可以被 Drools 引擎正确加载 -->
    <!-- scope=provided 表示仅在构建阶段使用，运行时不需要 -->
    <dependency>
        <groupId>org.kie</groupId>
        <artifactId>kie-maven-plugin</artifactId>
        <version>${drools.version}</version>
        <type>maven-plugin</type>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 快速开始

### 创建 DRL

`src/main/resources/io/github/atengk/drools/unit/PersonUnit.drl`

```
package io.github.atengk.drools.unit

unit PersonUnit;

import io.github.atengk.drools.model.Person;

rule "Mark adult person"
when
    $person : /persons[ age >= 18 ]
then
    modify($person) {
        setAdult(true)
    };
    adultNames.add($person.getName());
end

query FindAdults
    $person : /persons[ adult == true ]
end
```

### 对象类

```java
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
```

### 规则结果类

```java
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
```

### 人员规则单元数据类

```java
package io.github.atengk.drools.unit;

import io.github.atengk.drools.model.Person;
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

    private final DataStore<Person> persons;
    private final Set<String> adultNames;

    public PersonUnit() {
        this(DataSource.createStore());
    }

    public PersonUnit(DataStore<Person> persons) {
        this.persons = persons;
        this.adultNames = new HashSet<>();
    }

    public DataStore<Person> getPersons() {
        return persons;
    }

    public Set<String> getAdultNames() {
        return adultNames;
    }
}
```

### 规则执行服务

```java
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
```

### 人员规则接口

```java
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
```

请求示例

```
curl -X POST "http://localhost:8080/drools/evaluate" \
  -H "Content-Type: application/json" \
  -d '[
    {"name":"Tom","age":17,"adult":false},
    {"name":"Jerry","age":18,"adult":false},
    {"name":"Lucy","age":22,"adult":false}
  ]'
```

返回结果

```
{
  "persons": [
    {"name":"Tom","age":17,"adult":false},
    {"name":"Jerry","age":18,"adult":true},
    {"name":"Lucy","age":22,"adult":true}
  ],
  "adultNames": ["Jerry", "Lucy"]
}
```

