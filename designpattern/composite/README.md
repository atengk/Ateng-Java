## 🌳 组合模式（Composite Pattern）

### 一、模式简介

**组合模式（Composite Pattern）** 是一种结构型设计模式，它将对象组织成树形结构以表示“整体–部分”的层次关系，使得客户端可以统一地处理单个对象和组合对象。

📘 **核心思想：**

> 把“个体对象（叶子）”和“组合对象（容器）”都当作统一的组件（Component）来处理。

在实际开发中常见于：

* 菜单 / 树形结构（例如前端菜单、部门组织树）
* 文件系统（文件夹 + 文件）
* 权限结构（权限组 + 权限项）

---

### 二、模式结构说明

```
Component（抽象组件）
 ├── 统一接口：定义叶子与组合的共同行为

Leaf（叶子节点）
 ├── 无子节点，具体实现操作

Composite（组合节点）
 ├── 含有多个 Component 子节点
 ├── 递归调用子节点操作
```

---

### 三、实战案例：公司组织架构树

**场景说明：**
我们构建一个简单的公司组织结构，包括部门（可包含子部门或员工）与员工（叶子节点）。
最终使用组合模式统一管理整个组织架构。

---

### 四、代码结构

```
io.github.atengk
└── service
    └── composite
        ├── component
        │    └── OrganizationComponent.java
        ├── leaf
        │    └── Employee.java
        ├── composite
        │    └── Department.java
        └── CompositePatternDemo.java
```

---

### 五、代码实现

#### 1️⃣ 抽象组件（Component）

```java
package io.github.atengk.service.composite.component;

/**
 * 组织结构抽象组件
 */
public abstract class OrganizationComponent {

    /**
     * 组件名称
     */
    protected String name;

    public OrganizationComponent(String name) {
        this.name = name;
    }

    /**
     * 添加子节点
     */
    public void add(OrganizationComponent component) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 移除子节点
     */
    public void remove(OrganizationComponent component) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 显示组织结构（核心抽象方法）
     */
    public abstract void show(int level);
}
```

---

#### 2️⃣ 叶子节点（Leaf）—— 员工

```java
package io.github.atengk.service.composite.leaf;

import io.github.atengk.service.composite.component.OrganizationComponent;

/**
 * 员工（叶子节点）
 */
public class Employee extends OrganizationComponent {

    public Employee(String name) {
        super(name);
    }

    @Override
    public void show(int level) {
        String prefix = " ".repeat(level * 2);
        System.out.println(prefix + "👤 员工：" + name);
    }
}
```

---

#### 3️⃣ 组合节点（Composite）—— 部门

```java
package io.github.atengk.service.composite.composite;

import io.github.atengk.service.composite.component.OrganizationComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门（组合节点）
 */
public class Department extends OrganizationComponent {

    private final List<OrganizationComponent> children = new ArrayList<>();

    public Department(String name) {
        super(name);
    }

    @Override
    public void add(OrganizationComponent component) {
        children.add(component);
    }

    @Override
    public void remove(OrganizationComponent component) {
        children.remove(component);
    }

    @Override
    public void show(int level) {
        String prefix = " ".repeat(level * 2);
        System.out.println(prefix + "🏢 部门：" + name);
        for (OrganizationComponent child : children) {
            child.show(level + 1);
        }
    }
}
```

---

#### 4️⃣ 运行示例类

```java
package io.github.atengk.service.composite;

import io.github.atengk.service.composite.composite.Department;
import io.github.atengk.service.composite.leaf.Employee;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 组合模式示例演示类
 */
@Component
public class CompositePatternDemo {

    @PostConstruct
    public void runDemo() {
        System.out.println("=== 组合模式（Composite Pattern）示例 ===");

        // 顶层部门
        Department root = new Department("公司总部");

        // 一级部门
        Department techDept = new Department("技术部");
        Department hrDept = new Department("人事部");

        // 二级部门
        Department backendTeam = new Department("后端组");
        Department frontendTeam = new Department("前端组");

        // 添加员工
        backendTeam.add(new Employee("张三"));
        backendTeam.add(new Employee("李四"));
        frontendTeam.add(new Employee("王五"));
        hrDept.add(new Employee("赵六"));

        // 组装结构
        techDept.add(backendTeam);
        techDept.add(frontendTeam);
        root.add(techDept);
        root.add(hrDept);

        // 显示组织结构
        root.show(0);
    }
}
```

---

### 六、输出示例

```
=== 组合模式（Composite Pattern）示例 ===
🏢 部门：公司总部
  🏢 部门：技术部
    🏢 部门：后端组
      👤 员工：张三
      👤 员工：李四
    🏢 部门：前端组
      👤 员工：王五
  🏢 部门：人事部
    👤 员工：赵六
```

---

### 七、总结与应用场景

✅ **优点：**

* 统一叶子与组合对象的接口，递归结构自然、可扩展性强。
* 客户端可一致地对待单个对象与组合对象。

⚠️ **缺点：**

* 设计较为抽象，不易限制组件类型（可能被误用）。

📌 **常见应用场景：**

* 树形结构（组织架构、菜单、权限）
* 文件系统、目录树
* 报表层级、表单组合结构

