# JDK 21 Stream 开发

JDK 21 中的 Stream 是 Java 集合数据处理的重要工具，适合用于过滤、转换、排序、分组、统计、聚合等场景。Stream 并不是新的集合类型，而是一套面向数据流水线处理的 API，它可以让集合处理逻辑更加声明式、链式化和可组合。

## Stream 概述

本章用于说明 Stream 在 Java 开发中的定位、它与集合的关系，以及在实际开发中需要掌握的核心特性。理解这些概念后，再学习 `filter`、`map`、`collect`、`groupingBy` 等操作会更容易。

### Stream 的定位

Stream 是 Java 8 引入的数据处理 API，在 JDK 21 中依然是集合处理、批量数据转换和统计聚合的核心能力之一。它的主要作用不是替代集合，而是为集合、数组、I/O 数据源等提供一套统一的数据处理流水线。

在传统写法中，开发者通常使用 `for`、`if`、临时变量和可变集合完成数据处理。例如从员工列表中筛选启用状态的员工，再提取姓名，最后组成新的列表。使用 Stream 后，可以把这个过程表达为一条清晰的数据处理链。

```java
List<String> names = employees.stream()
        .filter(Employee::enabled)
        .map(Employee::name)
        .toList();
```

这段代码表达的是：从员工集合中筛选启用员工，然后提取姓名并收集为列表。相比传统循环，它更关注“做什么”，而不是“怎么循环”。

Stream 常用于以下场景：

| 场景     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 数据过滤 | 根据条件筛选集合元素，例如筛选启用用户、有效订单、指定状态数据 |
| 字段转换 | 将对象列表转换为字段列表、VO 列表、Map 结构                  |
| 数据排序 | 根据单字段或多字段排序                                       |
| 分组统计 | 按部门、状态、类型等字段进行分组                             |
| 聚合计算 | 求和、平均值、最大值、最小值、统计汇总                       |
| 嵌套展开 | 将订单明细、角色权限、标签列表等嵌套集合展开                 |
| 去重处理 | 根据对象自身或业务字段进行去重                               |

Stream 不适合所有场景。如果逻辑中存在复杂的状态变更、多层异常处理、提前中断、频繁调试或强副作用操作，普通循环通常更直观。

### Stream 与集合的关系

集合用于存储数据，Stream 用于处理数据。二者的职责不同。

集合是数据容器，例如 `List`、`Set`、`Map`。集合强调数据的保存、访问、增删和遍历。Stream 则强调对数据源进行声明式计算，它本身不存储数据，只是定义并执行一组数据处理操作。

| 对比项         | 集合                               | Stream                               |
| -------------- | ---------------------------------- | ------------------------------------ |
| 核心职责       | 保存数据                           | 处理数据                             |
| 是否存储元素   | 存储元素                           | 不存储元素                           |
| 是否可重复使用 | 可以重复遍历                       | 一般只能消费一次                     |
| 执行方式       | 直接操作                           | 中间操作惰性执行，终止操作触发执行   |
| 关注点         | 数据结构                           | 数据计算过程                         |
| 常见 API       | `add`、`remove`、`get`、`contains` | `filter`、`map`、`sorted`、`collect` |

示例：

```java
List<String> names = List.of("张三", "李四", "王五");

Stream<String> stream = names.stream();

List<String> result = stream
        .filter(name -> name.startsWith("张"))
        .toList();
```

在这个例子中，`names` 是集合，负责保存数据；`stream` 是基于集合创建的数据处理流水线，负责执行过滤逻辑。

需要注意的是，Stream 通常只能被消费一次。下面的写法会抛出 `IllegalStateException`：

```java
Stream<String> stream = names.stream();

long count = stream.count();

// 错误：同一个 Stream 已经被终止操作消费，不能再次使用
List<String> list = stream.toList();
```

正确做法是每次重新从数据源创建新的 Stream：

```java
long count = names.stream().count();

List<String> list = names.stream().toList();
```

### Stream 的核心特点

Stream 的核心特点可以概括为声明式、链式调用、惰性求值、不可复用、不修改原数据源以及支持串行和并行执行。

第一，Stream 是声明式的。开发者通过 `filter`、`map`、`sorted`、`collect` 等 API 描述数据处理意图，而不是手动控制循环过程。

```java
List<String> enabledEmployeeNames = employees.stream()
        .filter(Employee::enabled)
        .map(Employee::name)
        .toList();
```

第二，Stream 支持链式调用。多个操作可以组合成一条数据处理流水线，每一步都表达一个明确的数据处理动作。

```java
List<Employee> result = employees.stream()
        .filter(Employee::enabled)
        .filter(employee -> employee.age() >= 30)
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .toList();
```

第三，Stream 的中间操作具有惰性求值特点。`filter`、`map`、`sorted` 等中间操作不会立即执行，只有遇到 `count`、`toList`、`collect`、`forEach` 等终止操作时，整条流水线才会真正执行。

```java
Stream<Employee> stream = employees.stream()
        .filter(employee -> employee.age() >= 30)
        .map(employee -> employee);

// 执行到终止操作时，前面的 filter 和 map 才会真正运行
long count = stream.count();
```

第四，Stream 不会主动修改原始集合。大多数 Stream 操作会产生新的处理结果，原集合仍然保持不变。

```java
List<String> names = List.of("Tom", "Jerry", "Alice");

List<String> upperNames = names.stream()
        .map(String::toUpperCase)
        .toList();

// names 仍然是 ["Tom", "Jerry", "Alice"]
// upperNames 是 ["TOM", "JERRY", "ALICE"]
```

第五，Stream 可以串行执行，也可以并行执行。默认通过 `stream()` 创建的是串行流，通过 `parallelStream()` 或 `parallel()` 可以创建并行流。

```java
long count = employees.parallelStream()
        .filter(Employee::enabled)
        .count();
```

并行 Stream 并不一定更快。它适合数据量较大、计算密集、无共享状态、无顺序依赖的场景。如果数据量较小，或者操作中包含数据库访问、远程调用、文件写入、共享变量修改等副作用，并行 Stream 反而可能带来性能下降或线程安全问题。

## 开发环境准备

本章用于准备后续 Stream 示例所需的 JDK 21 环境、Maven 配置、示例数据和常用实体类。后续章节中的过滤、转换、分组、统计、排序等操作都可以基于本章的数据模型进行演示。

### JDK 21 环境配置

开发前需要确保本地已经安装 JDK 21，并且构建工具能够正确识别 Java 21。建议使用 IntelliJ IDEA、Maven 3.9+ 和 JDK 21 作为基础开发环境。

可以通过以下命令检查 JDK 版本：

```bash
java -version
javac -version
```

期望输出中包含 `21`，例如：

```bash
java version "21.0.x"
javac 21.0.x
```

如果本机安装了多个 JDK，需要确认 `JAVA_HOME` 指向 JDK 21。

Linux 或 macOS 示例：

```bash
export JAVA_HOME=/opt/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

java -version
```

Windows PowerShell 示例：

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

java -version
```

Maven 项目建议统一指定 Java 21 编译版本，避免 IDE 和命令行构建环境不一致。

文件位置：`pom.xml`

下面的配置用于创建一个基于 JDK 21 的普通 Maven 示例项目，并引入 Hutool 和 JUnit 5，方便后续编写 Stream 示例和单元测试。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>jdk21-stream-demo</artifactId>
    <version>1.0.0</version>
    <name>jdk21-stream-demo</name>

    <properties>
        <!-- 统一源码和目标字节码版本，确保使用 JDK 21 编译 -->
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- 依赖版本集中管理，便于后续升级 -->
        <hutool.version>5.8.38</hutool.version>
        <junit.version>5.10.2</junit.version>
    </properties>

    <dependencies>
        <!-- Hutool 工具包，用于集合、字符串、日期、对象等常用工具处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- JUnit 5，用于编写 Stream 示例的单元测试 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven 编译插件，明确指定 Java 21 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <release>21</release>
                </configuration>
            </plugin>

            <!-- Maven 测试插件，支持运行 JUnit 5 测试用例 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

推荐的项目结构如下：

```text
jdk21-stream-demo
├── pom.xml
└── src
    ├── main
    │   └── java
    │       └── io
    │           └── github
    │               └── atengk
    │                   └── stream
    │                       ├── data
    │                       │   └── StreamSampleData.java
    │                       └── model
    │                           ├── Department.java
    │                           ├── Employee.java
    │                           ├── Order.java
    │                           ├── OrderItem.java
    │                           └── OrderStatus.java
    └── test
        └── java
            └── io
                └── github
                    └── atengk
                        └── stream
```

可以使用以下命令验证项目是否能够正常编译：

```bash
mvn clean test
```

如果命令执行成功，说明 JDK 21、Maven 和依赖配置已经准备完成。

### 示例数据准备

为了让后续章节的 Stream 示例更加贴近业务场景，可以准备员工、部门、订单和订单明细等数据。员工数据适合演示过滤、映射、排序、分组、统计；订单数据适合演示嵌套集合展开、订单金额汇总、多条件筛选等操作。

文件位置：`src/main/java/io/github/atengk/stream/data/StreamSampleData.java`

下面的代码提供统一的示例数据入口，后续所有 Stream 示例都可以复用这些方法。

```java
package io.github.atengk.stream.data;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.stream.model.Department;
import io.github.atengk.stream.model.Employee;
import io.github.atengk.stream.model.Order;
import io.github.atengk.stream.model.OrderItem;
import io.github.atengk.stream.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stream 示例数据工厂
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class StreamSampleData {

    private StreamSampleData() {
    }

    /**
     * 获取部门示例数据。
     *
     * @return 部门列表
     */
    public static List<Department> departments() {
        return CollUtil.newArrayList(
                new Department(1L, "研发部"),
                new Department(2L, "测试部"),
                new Department(3L, "产品部"),
                new Department(4L, "运维部")
        );
    }

    /**
     * 获取员工示例数据。
     *
     * @return 员工列表
     */
    public static List<Employee> employees() {
        return CollUtil.newArrayList(
                new Employee(1L, "张三", 1L, "研发工程师", 28, new BigDecimal("18000.00"), true, LocalDate.of(2021, 3, 15)),
                new Employee(2L, "李四", 1L, "高级研发工程师", 34, new BigDecimal("26000.00"), true, LocalDate.of(2018, 7, 1)),
                new Employee(3L, "王五", 2L, "测试工程师", 26, new BigDecimal("14000.00"), true, LocalDate.of(2022, 5, 20)),
                new Employee(4L, "赵六", 2L, "测试主管", 36, new BigDecimal("22000.00"), false, LocalDate.of(2017, 11, 3)),
                new Employee(5L, "钱七", 3L, "产品经理", 31, new BigDecimal("24000.00"), true, LocalDate.of(2019, 9, 10)),
                new Employee(6L, "孙八", 4L, "运维工程师", 29, new BigDecimal("17000.00"), true, LocalDate.of(2020, 12, 8)),
                new Employee(7L, "周九", 1L, "架构师", 39, new BigDecimal("38000.00"), true, LocalDate.of(2016, 4, 18))
        );
    }

    /**
     * 获取订单示例数据。
     *
     * @return 订单列表
     */
    public static List<Order> orders() {
        return CollUtil.newArrayList(
                new Order(
                        1001L,
                        "C001",
                        OrderStatus.PAID,
                        LocalDateTime.of(2026, 1, 10, 10, 30),
                        CollUtil.newArrayList(
                                new OrderItem(1L, 1001L, "机械键盘", 1, new BigDecimal("399.00")),
                                new OrderItem(2L, 1001L, "无线鼠标", 2, new BigDecimal("129.00"))
                        )
                ),
                new Order(
                        1002L,
                        "C002",
                        OrderStatus.CREATED,
                        LocalDateTime.of(2026, 1, 11, 14, 5),
                        CollUtil.newArrayList(
                                new OrderItem(3L, 1002L, "显示器", 1, new BigDecimal("1299.00"))
                        )
                ),
                new Order(
                        1003L,
                        "C001",
                        OrderStatus.FINISHED,
                        LocalDateTime.of(2026, 1, 12, 9, 20),
                        CollUtil.newArrayList(
                                new OrderItem(4L, 1003L, "USB-C 扩展坞", 1, new BigDecimal("259.00")),
                                new OrderItem(5L, 1003L, "笔记本支架", 1, new BigDecimal("159.00"))
                        )
                ),
                new Order(
                        1004L,
                        "C003",
                        OrderStatus.CANCELED,
                        LocalDateTime.of(2026, 1, 13, 16, 45),
                        CollUtil.newArrayList(
                                new OrderItem(6L, 1004L, "移动硬盘", 1, new BigDecimal("499.00"))
                        )
                )
        );
    }
}
```

后续章节可以直接通过以下方式获取数据：

```java
List<Employee> employees = StreamSampleData.employees();
List<Order> orders = StreamSampleData.orders();
```

例如，统计启用员工数量：

```java
long enabledCount = StreamSampleData.employees()
        .stream()
        .filter(Employee::enabled)
        .count();
```

例如，展开订单明细：

```java
List<OrderItem> items = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .toList();
```

### 常用实体类设计

实体类应尽量保持简单、不可变、字段语义明确。JDK 21 中可以使用 `record` 定义示例数据模型，减少样板代码，并天然提供构造器、字段访问方法、`equals`、`hashCode` 和 `toString`。

在 Stream 示例中，实体类设计建议遵循以下原则：

| 原则             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 字段类型明确     | 金额使用 `BigDecimal`，日期使用 `LocalDate` 或 `LocalDateTime` |
| 避免无意义字段   | 只保留演示 Stream 所需字段                                   |
| 优先不可变模型   | 示例代码中优先使用 `record`，避免 Stream 处理中意外修改对象  |
| 保持业务关系清晰 | 员工关联部门，订单包含订单明细                               |
| 枚举表达状态     | 订单状态使用枚举，避免字符串魔法值                           |

文件位置：`src/main/java/io/github/atengk/stream/model/Department.java`

下面的代码定义部门实体，用于演示员工按部门分组、部门名称映射等场景。

```java
package io.github.atengk.stream.model;

/**
 * 部门实体
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record Department(
        Long id,
        String name
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/model/Employee.java`

下面的代码定义员工实体，用于演示过滤、排序、字段提取、分组统计、薪资汇总等场景。

```java
package io.github.atengk.stream.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工实体
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record Employee(
        Long id,
        String name,
        Long departmentId,
        String position,
        Integer age,
        BigDecimal salary,
        Boolean enabled,
        LocalDate entryDate
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/model/OrderStatus.java`

下面的代码定义订单状态枚举，用于演示按状态过滤、分组和统计。

```java
package io.github.atengk.stream.model;

/**
 * 订单状态
 *
 * @author Ateng
 * @since 2026-05-07
 */
public enum OrderStatus {

    /**
     * 已创建，未支付
     */
    CREATED,

    /**
     * 已支付
     */
    PAID,

    /**
     * 已完成
     */
    FINISHED,

    /**
     * 已取消
     */
    CANCELED
}
```

文件位置：`src/main/java/io/github/atengk/stream/model/OrderItem.java`

下面的代码定义订单明细实体，用于演示嵌套集合展开、商品数量统计和订单金额计算。

```java
package io.github.atengk.stream.model;

import java.math.BigDecimal;

/**
 * 订单明细实体
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record OrderItem(
        Long id,
        Long orderId,
        String productName,
        Integer quantity,
        BigDecimal price
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/model/Order.java`

下面的代码定义订单实体，包含订单状态、下单时间和订单明细列表，用于后续演示复杂 Stream 操作。

```java
package io.github.atengk.stream.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record Order(
        Long id,
        String customerNo,
        OrderStatus status,
        LocalDateTime createTime,
        List<OrderItem> items
) {
}
```

完成以上实体和示例数据后，后续章节可以围绕以下典型数据处理目标展开。

下面的代码演示基于示例数据完成常见 Stream 操作，包括过滤、字段提取、分组和嵌套集合展开。

```java
import io.github.atengk.stream.data.StreamSampleData;
import io.github.atengk.stream.model.Employee;
import io.github.atengk.stream.model.OrderItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 筛选启用员工
List<Employee> enabledEmployees = StreamSampleData.employees()
        .stream()
        .filter(Employee::enabled)
        .toList();

// 提取员工姓名
List<String> employeeNames = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();

// 按部门 ID 分组
Map<Long, List<Employee>> employeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(Employee::departmentId));

// 展开订单明细
List<OrderItem> orderItems = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .toList();
```

这些数据模型覆盖了 Stream 开发中最常用的几类操作：单集合过滤、字段映射、对象排序、分组统计、嵌套集合展开、枚举状态处理和数值聚合。



## Stream 基础使用

本章用于介绍 Stream 的基础使用方式，包括如何创建 Stream、常见中间操作、常见终止操作，以及 Stream 的惰性求值机制。掌握这些内容后，后续的过滤、转换、排序、分组和统计会更容易理解。

### Stream 创建方式

Stream 可以从集合、数组、固定元素、生成函数、迭代函数、数值区间等多种数据源创建。实际开发中最常见的方式是从 `List`、`Set` 等集合创建 Stream。

从集合创建 Stream 是业务开发中最常用的方式，适合处理数据库查询结果、接口返回列表、内存集合等数据。

```java
List<Employee> employees = StreamSampleData.employees();

List<String> names = employees.stream()
        .map(Employee::name)
        .toList();
```

如果需要从数组创建 Stream，可以使用 `Arrays.stream`。这种方式常用于处理固定数组、配置项数组或临时数据。

```java
String[] names = {"张三", "李四", "王五"};

List<String> result = Arrays.stream(names)
        .filter(name -> name.length() == 2)
        .toList();
```

如果数据量较少，并且数据直接写在代码中，可以使用 `Stream.of` 创建 Stream。

```java
List<String> statusList = Stream.of("CREATED", "PAID", "FINISHED", "CANCELED")
        .filter(status -> status.contains("ED"))
        .toList();
```

如果需要从单个可能为空的对象创建 Stream，可以使用 `Stream.ofNullable`。这种方式适合减少空值判断，避免显式编写 `if (obj != null)`。

```java
Employee employee = StreamSampleData.employees().getFirst();

List<String> names = Stream.ofNullable(employee)
        .map(Employee::name)
        .toList();
```

如果需要创建数值范围，可以使用 `IntStream.range` 或 `IntStream.rangeClosed`。前者不包含结束值，后者包含结束值。

```java
List<Integer> numbers = IntStream.rangeClosed(1, 5)
        .boxed()
        .toList();
```

执行结果为：

```text
[1, 2, 3, 4, 5]
```

如果需要生成无限流，可以使用 `Stream.generate` 或 `Stream.iterate`，但必须配合 `limit` 限制数量，否则可能导致程序持续运行。

```java
List<String> codes = Stream.generate(() -> IdUtil.fastSimpleUUID())
        .limit(3)
        .toList();
```

上面代码使用 Hutool 的 `IdUtil.fastSimpleUUID()` 生成 UUID 字符串，并通过 `limit(3)` 限制只生成 3 条数据。

`Stream.iterate` 适合根据前一个值推导下一个值。

```java
List<Integer> numbers = Stream.iterate(1, num -> num + 2)
        .limit(5)
        .toList();
```

执行结果为：

```text
[1, 3, 5, 7, 9]
```

JDK 9 之后，`Stream.iterate` 增加了带终止条件的重载方法。JDK 21 中可以继续使用这种写法，让代码更接近普通循环。

```java
List<Integer> numbers = Stream.iterate(1, num -> num <= 10, num -> num + 1)
        .toList();
```

执行结果为：

```text
[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
```

常见 Stream 创建方式如下：

| 创建方式     | 示例                           | 适用场景                  |
| ------------ | ------------------------------ | ------------------------- |
| 集合创建     | `list.stream()`                | 处理 `List`、`Set` 等集合 |
| 数组创建     | `Arrays.stream(array)`         | 处理数组数据              |
| 固定元素创建 | `Stream.of(...)`               | 少量固定数据              |
| 空值安全创建 | `Stream.ofNullable(obj)`       | 单个对象可能为空          |
| 数值范围创建 | `IntStream.rangeClosed(1, 10)` | 连续数字处理              |
| 函数生成     | `Stream.generate(...)`         | 随机值、UUID、模拟数据    |
| 迭代生成     | `Stream.iterate(...)`          | 等差序列、递推数据        |

### 中间操作

中间操作用于定义数据处理过程，例如过滤、转换、排序、去重、截取等。中间操作不会立即执行，只有遇到终止操作时，整条 Stream 流水线才会真正执行。

常见中间操作如下：

| 方法       | 作用                   | 示例                       |
| ---------- | ---------------------- | -------------------------- |
| `filter`   | 按条件过滤元素         | 筛选启用员工               |
| `map`      | 将元素转换为另一种形式 | 员工对象转员工姓名         |
| `flatMap`  | 将嵌套流展开为一个流   | 订单列表展开为订单明细列表 |
| `sorted`   | 对元素排序             | 按薪资排序                 |
| `distinct` | 去重                   | 去除重复元素               |
| `limit`    | 限制返回数量           | 取前 3 条                  |
| `skip`     | 跳过指定数量           | 跳过前 5 条                |
| `peek`     | 调试或观察元素         | 打印中间数据               |

中间操作可以链式组合，形成一条完整的数据处理流水线。

下面的代码演示筛选启用员工、按薪资倒序排序，并提取员工姓名。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .filter(Employee::enabled)
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .map(Employee::name)
        .toList();
```

在这条流水线中，`filter`、`sorted`、`map` 都是中间操作，`toList` 是终止操作。

`peek` 一般用于调试，不建议在正式业务逻辑中依赖它修改数据。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .filter(Employee::enabled)
        .peek(employee -> System.out.println("过滤后的员工：" + employee.name()))
        .map(Employee::name)
        .toList();
```

`peek` 的定位是观察流中元素，不是执行业务修改。实际开发中，如果需要记录关键业务信息，更建议在业务代码中使用明确的日志逻辑，而不是把副作用隐藏在 Stream 链中。

### 终止操作

终止操作用于触发 Stream 执行，并产生最终结果。一个 Stream 在执行终止操作后就被消费，不能再次使用。

常见终止操作如下：

| 方法        | 作用             | 返回值         |
| ----------- | ---------------- | -------------- |
| `toList`    | 收集为不可变列表 | `List<T>`      |
| `collect`   | 自定义收集结果   | 由收集器决定   |
| `forEach`   | 遍历处理每个元素 | `void`         |
| `count`     | 统计元素数量     | `long`         |
| `findFirst` | 获取第一个元素   | `Optional<T>`  |
| `findAny`   | 获取任意一个元素 | `Optional<T>`  |
| `anyMatch`  | 是否存在匹配元素 | `boolean`      |
| `allMatch`  | 是否全部匹配     | `boolean`      |
| `noneMatch` | 是否全部不匹配   | `boolean`      |
| `max`       | 获取最大值       | `Optional<T>`  |
| `min`       | 获取最小值       | `Optional<T>`  |
| `reduce`    | 归约计算         | 由归约逻辑决定 |

`toList` 是 JDK 16 引入的便捷终止操作，JDK 21 中可以直接使用。需要注意，`Stream.toList()` 返回的是不可变列表，不能再执行 `add`、`remove` 等修改操作。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();
```

如果需要可变列表，可以使用 `Collectors.toCollection` 明确指定集合类型。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.toCollection(ArrayList::new));

names.add("新员工");
```

`count` 用于统计满足条件的数据数量。

```java
long enabledCount = StreamSampleData.employees()
        .stream()
        .filter(Employee::enabled)
        .count();
```

`findFirst` 用于获取第一个匹配元素，通常配合 `Optional` 处理空结果。

```java
Optional<Employee> employeeOptional = StreamSampleData.employees()
        .stream()
        .filter(employee -> employee.salary().compareTo(new BigDecimal("30000")) > 0)
        .findFirst();
```

`anyMatch`、`allMatch`、`noneMatch` 用于判断集合中元素是否满足条件。

```java
boolean hasDisabledEmployee = StreamSampleData.employees()
        .stream()
        .anyMatch(employee -> Boolean.FALSE.equals(employee.enabled()));

boolean allSalaryValid = StreamSampleData.employees()
        .stream()
        .allMatch(employee -> employee.salary().compareTo(BigDecimal.ZERO) > 0);

boolean noneMinorEmployee = StreamSampleData.employees()
        .stream()
        .noneMatch(employee -> employee.age() < 18);
```

`max` 和 `min` 通常需要传入比较器。

```java
Optional<Employee> maxSalaryEmployee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary));

Optional<Employee> minAgeEmployee = StreamSampleData.employees()
        .stream()
        .min(Comparator.comparing(Employee::age));
```

`reduce` 适合做归约计算，例如金额累加、字符串合并、对象聚合等。

```java
BigDecimal totalSalary = StreamSampleData.employees()
        .stream()
        .map(Employee::salary)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

### 惰性求值机制

Stream 的中间操作是惰性执行的。也就是说，调用 `filter`、`map`、`sorted` 等方法时，并不会立即遍历数据。只有调用终止操作时，Stream 才会开始执行整条流水线。

下面的代码中，如果没有 `toList`，`filter` 和 `map` 中的打印语句不会执行。

```java
Stream<String> stream = StreamSampleData.employees()
        .stream()
        .filter(employee -> {
            System.out.println("执行过滤：" + employee.name());
            return employee.enabled();
        })
        .map(employee -> {
            System.out.println("执行转换：" + employee.name());
            return employee.name();
        });
```

只有添加终止操作后，流水线才会真正执行。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .filter(employee -> {
            System.out.println("执行过滤：" + employee.name());
            return employee.enabled();
        })
        .map(employee -> {
            System.out.println("执行转换：" + employee.name());
            return employee.name();
        })
        .toList();
```

惰性求值可以带来两个好处。

第一，可以减少无效计算。Stream 会把多个操作组合成一条流水线执行，而不是每个中间操作都生成完整的临时集合。

第二，可以配合短路操作提前结束处理。`findFirst`、`findAny`、`anyMatch`、`allMatch`、`noneMatch`、`limit` 都具有短路特性，在满足条件后可以提前结束遍历。

下面的代码只要找到第一个启用员工，就会停止继续查找。

```java
Optional<Employee> firstEnabledEmployee = StreamSampleData.employees()
        .stream()
        .filter(employee -> {
            System.out.println("检查员工：" + employee.name());
            return employee.enabled();
        })
        .findFirst();
```

下面的代码只要发现存在薪资大于 30000 的员工，就会返回 `true`，不一定会遍历完整集合。

```java
boolean existsHighSalaryEmployee = StreamSampleData.employees()
        .stream()
        .anyMatch(employee -> employee.salary().compareTo(new BigDecimal("30000")) > 0);
```

使用惰性求值时需要注意：不要认为中间操作一定会执行，也不要在中间操作中编写必须执行的业务逻辑。中间操作应尽量保持纯函数风格，避免修改外部变量、写数据库、发请求、写文件等副作用行为。

## 常用操作实践

本章围绕 Stream 最常用的几个中间操作展开，包括 `filter`、`map`、`flatMap`、`sorted`、`distinct`、`limit` 和 `skip`。这些操作是日常业务开发中使用频率最高的部分，适合优先掌握。

### filter 过滤

`filter` 用于根据条件筛选元素。它接收一个 `Predicate<T>` 函数式接口，返回值为 `true` 的元素会被保留，返回值为 `false` 的元素会被过滤掉。

筛选启用员工：

```java
List<Employee> enabledEmployees = StreamSampleData.employees()
        .stream()
        .filter(Employee::enabled)
        .toList();
```

筛选年龄大于等于 30 岁的员工：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .filter(employee -> employee.age() >= 30)
        .toList();
```

筛选启用状态，并且薪资大于 20000 的员工：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .filter(Employee::enabled)
        .filter(employee -> employee.salary().compareTo(new BigDecimal("20000")) > 0)
        .toList();
```

多个过滤条件可以连续写多个 `filter`，也可以合并到一个 `filter` 中。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .filter(employee -> Employee::enabled != null)
        .filter(employee -> Boolean.TRUE.equals(employee.enabled())
                && employee.age() >= 30
                && employee.salary().compareTo(new BigDecimal("20000")) > 0)
        .toList();
```

上面的第一段 `filter(employee -> Employee::enabled != null)` 写法是错误的，因为 `Employee::enabled` 是方法引用，不是当前员工的启用状态判断。正确写法如下：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled())
                && employee.age() >= 30
                && employee.salary().compareTo(new BigDecimal("20000")) > 0)
        .toList();
```

实际开发中，如果字段可能为空，建议使用 `Boolean.TRUE.equals(...)` 判断布尔包装类型，避免空指针异常。

```java
List<Employee> enabledEmployees = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .toList();
```

### map 转换

`map` 用于将流中的元素转换为另一种形式。它接收一个 `Function<T, R>` 函数式接口，可以把 `Employee` 转成 `String`、把实体对象转成 VO、把订单明细转成金额等。

提取员工姓名：

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();
```

提取员工 ID：

```java
List<Long> employeeIds = StreamSampleData.employees()
        .stream()
        .map(Employee::id)
        .toList();
```

将员工名称转换为带前缀的展示文本：

```java
List<String> displayNames = StreamSampleData.employees()
        .stream()
        .map(employee -> "员工：" + employee.name())
        .toList();
```

将员工对象转换为简单展示对象时，可以使用 `record` 定义轻量 VO。

文件位置：`src/main/java/io/github/atengk/stream/model/EmployeeSimpleVO.java`

下面的代码定义员工简单展示对象，用于演示 `map` 将实体转换为 VO。

```java
package io.github.atengk.stream.model;

import java.math.BigDecimal;

/**
 * 员工简单展示对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record EmployeeSimpleVO(
        Long id,
        String name,
        String position,
        BigDecimal salary
) {
}
```

转换示例：

```java
List<EmployeeSimpleVO> voList = StreamSampleData.employees()
        .stream()
        .map(employee -> new EmployeeSimpleVO(
                employee.id(),
                employee.name(),
                employee.position(),
                employee.salary()
        ))
        .toList();
```

如果转换逻辑较复杂，不建议在 `map` 中堆叠大量代码，可以提取为独立方法，提升可读性。

```java
private static EmployeeSimpleVO toSimpleVO(Employee employee) {
    return new EmployeeSimpleVO(
            employee.id(),
            employee.name(),
            employee.position(),
            employee.salary()
    );
}
```

调用时保持 Stream 链简洁：

```java
List<EmployeeSimpleVO> voList = StreamSampleData.employees()
        .stream()
        .map(StreamBasicDemo::toSimpleVO)
        .toList();
```

### flatMap 扁平化

`flatMap` 用于将多个流合并成一个流。它适合处理嵌套集合，例如订单列表中包含订单明细列表、用户列表中包含角色列表、角色列表中包含权限列表等。

普通 `map` 处理嵌套集合时，得到的是 `Stream<List<OrderItem>>`，也就是列表的流。

```java
List<List<OrderItem>> itemList = StreamSampleData.orders()
        .stream()
        .map(Order::items)
        .toList();
```

如果目标是得到所有订单明细组成的一个列表，就应该使用 `flatMap`。

```java
List<OrderItem> items = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .toList();
```

`flatMap` 的核心作用是把“多个子集合”展开成“一个大集合”。

可以在展开后继续进行过滤、转换和统计。例如，统计所有订单中商品数量大于 1 的明细。

```java
List<OrderItem> items = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .filter(item -> item.quantity() > 1)
        .toList();
```

计算所有订单明细的总金额：

```java
BigDecimal totalAmount = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

如果嵌套集合可能为空，建议先进行空值兜底。使用 Hutool 可以让集合空值处理更直接。

```java
List<OrderItem> items = StreamSampleData.orders()
        .stream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
        .toList();
```

`CollUtil.emptyIfNull(order.items())` 可以在 `items` 为 `null` 时返回空集合，避免调用 `stream()` 时出现空指针异常。

### sorted 排序

`sorted` 用于对 Stream 中的元素进行排序。对于字符串、数字等实现了 `Comparable` 的类型，可以直接调用无参 `sorted`。对于对象排序，一般需要传入 `Comparator`。

对数字进行自然排序：

```java
List<Integer> numbers = Stream.of(5, 3, 1, 4, 2)
        .sorted()
        .toList();
```

对员工按年龄升序排序：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(Employee::age))
        .toList();
```

对员工按薪资降序排序：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .toList();
```

多字段排序可以使用 `thenComparing`。例如先按部门 ID 升序，再按薪资降序。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(
                Comparator.comparing(Employee::departmentId)
                        .thenComparing(Comparator.comparing(Employee::salary).reversed())
        )
        .toList();
```

如果排序字段可能为空，需要显式指定空值排在前面还是后面。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(
                Comparator.comparing(
                        Employee::salary,
                        Comparator.nullsLast(BigDecimal::compareTo)
                )
        )
        .toList();
```

实际开发中，排序规则建议保持清晰，不要在一个 `sorted` 中写过长的 Lambda。复杂排序可以提前定义比较器。

```java
Comparator<Employee> employeeComparator = Comparator
        .comparing(Employee::departmentId)
        .thenComparing(Comparator.comparing(Employee::salary).reversed())
        .thenComparing(Employee::age);

List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(employeeComparator)
        .toList();
```

### distinct 去重

`distinct` 用于去除重复元素。它依赖对象的 `equals` 和 `hashCode` 方法判断是否重复。对于 `String`、`Integer`、`Long` 等常见类型，可以直接使用。

```java
List<String> names = Stream.of("张三", "李四", "张三", "王五")
        .distinct()
        .toList();
```

执行结果为：

```text
[张三, 李四, 王五]
```

对于 `record` 类型，Java 会自动根据所有字段生成 `equals` 和 `hashCode`，因此可以直接使用 `distinct` 去除完全相同的对象。

```java
List<Employee> employees = Stream.concat(
                StreamSampleData.employees().stream(),
                StreamSampleData.employees().stream()
        )
        .distinct()
        .toList();
```

需要注意，`distinct` 默认是按整个对象去重。如果业务上只想按某个字段去重，例如按员工 ID 去重，不能直接依赖 `distinct`，需要结合 `Map` 或自定义去重逻辑。

按员工 ID 去重，并保留第一条数据：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                employee -> employee,
                (first, second) -> first,
                LinkedHashMap::new
        ))
        .values()
        .stream()
        .toList();
```

上面代码中，`Collectors.toMap` 的第三个参数用于处理重复 key。`(first, second) -> first` 表示如果员工 ID 重复，保留第一条数据。`LinkedHashMap::new` 用于保持原始顺序。

也可以提取一个通用的按字段去重方法，但需要注意线程安全问题。下面的方法适合串行 Stream 使用。

```java
public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = new HashSet<>();
    return element -> seen.add(keyExtractor.apply(element));
}
```

调用示例：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .filter(distinctByKey(Employee::id))
        .toList();
```

如果是并行 Stream，应使用线程安全集合。

```java
public static <T> Predicate<T> distinctByKeyConcurrent(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return element -> seen.add(keyExtractor.apply(element));
}
```

### limit 与 skip 截取

`limit` 用于限制返回数量，`skip` 用于跳过指定数量。二者经常用于分页、Top N 查询、数据截取等场景。

取薪资最高的前 3 名员工：

```java
List<Employee> top3Employees = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .limit(3)
        .toList();
```

跳过前 2 条员工数据：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .skip(2)
        .toList();
```

`limit` 和 `skip` 可以组合实现简单内存分页。

```java
int pageNum = 2;
int pageSize = 3;

List<Employee> pageData = StreamSampleData.employees()
        .stream()
        .skip((long) (pageNum - 1) * pageSize)
        .limit(pageSize)
        .toList();
```

上面代码表示查询第 2 页，每页 3 条。先跳过前 3 条，再取 3 条。

为了避免分页参数不合法，实际开发中应先做参数校验。

```java
int pageNum = Math.max(1, pageNumParam);
int pageSize = Math.max(1, pageSizeParam);

List<Employee> pageData = StreamSampleData.employees()
        .stream()
        .skip((long) (pageNum - 1) * pageSize)
        .limit(pageSize)
        .toList();
```

如果是数据库分页，不建议先查询全部数据再使用 Stream 分页。正确做法应该是在 SQL 或 ORM 层完成分页，只对当前页数据进行 Stream 处理。Stream 分页更适合内存数据、少量配置数据、测试数据或已经加载到内存的小集合。

`limit` 具有短路特性。如果前面的操作不需要完整遍历数据，Stream 可以在达到指定数量后提前结束。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .filter(Employee::enabled)
        .limit(2)
        .toList();
```

上面代码只需要找到前 2 个启用员工即可，不一定会遍历完整员工列表。



## 结果收集

本章用于说明 Stream 处理完成后如何收集结果。Stream 的中间操作只定义处理过程，最终需要通过 `toList`、`collect`、`toSet`、`toMap`、`groupingBy`、`partitioningBy`、`joining` 等终止操作生成最终结果。

### collect 基础用法

`collect` 是 Stream 中最常用的结果收集方法之一，适合把 Stream 中的数据收集为集合、Map、分组结果、字符串或统计结果。

`collect` 通常配合 `Collectors` 工具类使用。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.toList());
```

上面代码将员工对象转换为员工姓名，并收集为 `List<String>`。

`collect` 也可以收集为指定类型的集合。例如，如果希望结果集合是 `ArrayList`，可以使用 `Collectors.toCollection`。

```java
ArrayList<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.toCollection(ArrayList::new));
```

如果需要保持插入顺序并去重，可以收集为 `LinkedHashSet`。

```java
LinkedHashSet<Long> departmentIds = StreamSampleData.employees()
        .stream()
        .map(Employee::departmentId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
```

`collect` 的优势是灵活。它不仅可以完成简单集合收集，还可以进行分组、分区、拼接、统计和自定义归约。

常见收集器如下：

| 收集器                           | 作用             |
| -------------------------------- | ---------------- |
| `Collectors.toList()`            | 收集为列表       |
| `Collectors.toSet()`             | 收集为集合并去重 |
| `Collectors.toMap()`             | 收集为 Map       |
| `Collectors.groupingBy()`        | 按字段分组       |
| `Collectors.partitioningBy()`    | 按布尔条件分区   |
| `Collectors.joining()`           | 字符串拼接       |
| `Collectors.counting()`          | 统计数量         |
| `Collectors.summingInt()`        | int 求和         |
| `Collectors.averagingInt()`      | int 平均值       |
| `Collectors.mapping()`           | 分组后字段转换   |
| `Collectors.collectingAndThen()` | 收集后再转换     |

在实际开发中，`collect` 更适合需要自定义结果结构的场景。如果只是简单收集为不可变列表，JDK 21 中可以优先使用 `Stream.toList()`。

### toList 与 Collectors.toList

`Stream.toList()` 和 `Collectors.toList()` 都可以把 Stream 收集为 List，但二者在语义上有明显差异。

`Stream.toList()` 是 JDK 16 引入的方法，JDK 21 中可以直接使用。它返回的是不可变 List，不能继续执行 `add`、`remove`、`clear` 等修改操作。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();
```

如果执行下面的代码，会抛出 `UnsupportedOperationException`。

```java
names.add("新员工");
```

`Collectors.toList()` 返回一个 List，但 Java 规范不保证它一定是可变 List，也不保证具体实现类型。因此，如果业务上明确要求可变集合，不建议依赖 `Collectors.toList()` 的当前实现表现。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.toList());
```

如果明确需要可变 `ArrayList`，推荐使用 `Collectors.toCollection(ArrayList::new)`。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.toCollection(ArrayList::new));

names.add("新员工");
```

二者对比如下：

| 写法                                               | 返回结果                 | 是否建议修改结果集合 | 适用场景                         |
| -------------------------------------------------- | ------------------------ | -------------------- | -------------------------------- |
| `stream.toList()`                                  | 不可变 List              | 不建议，修改会报错   | 只读结果、接口返回、临时计算结果 |
| `collect(Collectors.toList())`                     | List，规范不保证具体类型 | 不建议依赖可变性     | 兼容旧代码、收集器组合           |
| `collect(Collectors.toCollection(ArrayList::new))` | 明确的 `ArrayList`       | 可以修改             | 后续需要新增、删除、排序结果集合 |

实际开发建议如下：

```java
// 推荐：只读结果
List<String> readonlyNames = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();

// 推荐：需要可变结果
List<String> mutableNames = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.toCollection(ArrayList::new));
```

如果返回值只用于读取、遍历、序列化或接口响应，优先使用 `toList()`。如果后续还需要修改集合，明确使用 `Collectors.toCollection(ArrayList::new)`。

### toSet 与 toMap

`toSet` 用于将 Stream 结果收集为 Set，适合去重场景。需要注意，`Collectors.toSet()` 不保证结果集合的顺序。

收集员工所属部门 ID，并自动去重：

```java
Set<Long> departmentIds = StreamSampleData.employees()
        .stream()
        .map(Employee::departmentId)
        .collect(Collectors.toSet());
```

如果希望去重后仍然保持原始顺序，可以使用 `LinkedHashSet`。

```java
Set<Long> departmentIds = StreamSampleData.employees()
        .stream()
        .map(Employee::departmentId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
```

`toMap` 用于将 Stream 结果收集为 Map。它通常需要指定 key 和 value 的生成方式。

按员工 ID 构建员工 Map：

```java
Map<Long, Employee> employeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                employee -> employee
        ));
```

如果 value 就是元素本身，也可以使用 `Function.identity()`。

```java
Map<Long, Employee> employeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                Function.identity()
        ));
```

按员工 ID 构建员工姓名 Map：

```java
Map<Long, String> employeeNameMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                Employee::name
        ));
```

使用 `toMap` 时需要重点关注重复 key。如果 key 重复，并且没有指定合并函数，会抛出 `IllegalStateException`。

下面的写法按部门 ID 构建 Map，由于一个部门可能有多个员工，容易出现重复 key。

```java
Map<Long, Employee> departmentEmployeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::departmentId,
                Function.identity()
        ));
```

正确写法应该指定重复 key 的合并策略。例如保留第一个员工：

```java
Map<Long, Employee> departmentEmployeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::departmentId,
                Function.identity(),
                (first, second) -> first
        ));
```

如果希望保持 Map 的插入顺序，可以指定 `LinkedHashMap`。

```java
Map<Long, Employee> employeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
        ));
```

实际开发中，`toMap` 的推荐写法是显式提供合并函数和 Map 类型，尤其是在 key 可能重复或结果顺序重要时。

```java
Map<Long, String> employeeNameMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                Employee::name,
                (first, second) -> first,
                LinkedHashMap::new
        ));
```

### groupingBy 分组

`groupingBy` 用于按指定字段对数据进行分组，返回结果通常是 `Map<K, List<T>>`。这是业务开发中使用频率很高的收集操作，常用于按部门、状态、类型、日期等维度组织数据。

按部门 ID 分组员工：

```java
Map<Long, List<Employee>> employeeGroupMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(Employee::departmentId));
```

结果结构类似如下：

```text
{
  1=[研发部员工列表],
  2=[测试部员工列表],
  3=[产品部员工列表],
  4=[运维部员工列表]
}
```

如果需要保持分组 key 的插入顺序，可以指定 `LinkedHashMap`。

```java
Map<Long, List<Employee>> employeeGroupMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                LinkedHashMap::new,
                Collectors.toList()
        ));
```

分组后统计每个部门的员工数量：

```java
Map<Long, Long> departmentCountMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                Collectors.counting()
        ));
```

分组后提取每个部门的员工姓名：

```java
Map<Long, List<String>> departmentNameMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                Collectors.mapping(Employee::name, Collectors.toList())
        ));
```

分组后统计每个部门的薪资总和。由于薪资使用 `BigDecimal`，不能直接使用 `summingInt` 或 `summingDouble`，推荐使用 `reducing`。

```java
Map<Long, BigDecimal> departmentSalaryMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                Collectors.mapping(
                        Employee::salary,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
        ));
```

分组后获取每个部门薪资最高的员工：

```java
Map<Long, Optional<Employee>> departmentMaxSalaryEmployeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                Collectors.maxBy(Comparator.comparing(Employee::salary))
        ));
```

如果不希望结果中出现 `Optional<Employee>`，可以使用 `collectingAndThen` 做后置处理。

```java
Map<Long, Employee> departmentMaxSalaryEmployeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparing(Employee::salary)),
                        optional -> optional.orElse(null)
                )
        ));
```

`groupingBy` 适合一对多关系的数据组织。如果一个 key 只允许对应一个 value，应优先考虑 `toMap`；如果一个 key 对应多个 value，应优先使用 `groupingBy`。

### partitioningBy 分区

`partitioningBy` 是一种特殊的分组操作，它根据布尔条件把数据分为两组，返回结果类型是 `Map<Boolean, List<T>>`。

按员工是否启用分区：

```java
Map<Boolean, List<Employee>> employeePartitionMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.partitioningBy(Employee::enabled));
```

返回结果中通常包含两个 key：

| key     | 含义             |
| ------- | ---------------- |
| `true`  | 满足条件的数据   |
| `false` | 不满足条件的数据 |

获取启用员工和禁用员工：

```java
Map<Boolean, List<Employee>> employeePartitionMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.partitioningBy(employee -> Boolean.TRUE.equals(employee.enabled())));

List<Employee> enabledEmployees = employeePartitionMap.get(Boolean.TRUE);
List<Employee> disabledEmployees = employeePartitionMap.get(Boolean.FALSE);
```

按薪资是否大于等于 20000 分区：

```java
Map<Boolean, List<Employee>> salaryPartitionMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.partitioningBy(
                employee -> employee.salary().compareTo(new BigDecimal("20000")) >= 0
        ));
```

分区后统计数量：

```java
Map<Boolean, Long> enabledCountMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.partitioningBy(
                employee -> Boolean.TRUE.equals(employee.enabled()),
                Collectors.counting()
        ));
```

分区后提取员工姓名：

```java
Map<Boolean, List<String>> enabledNameMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.partitioningBy(
                employee -> Boolean.TRUE.equals(employee.enabled()),
                Collectors.mapping(Employee::name, Collectors.toList())
        ));
```

`partitioningBy` 适合明确的二分类场景，例如启用/禁用、成功/失败、有效/无效、达标/未达标。如果分类结果不止两类，应使用 `groupingBy`。

### joining 字符串拼接

`joining` 用于将 Stream 中的字符串元素拼接成一个字符串。它通常配合 `map` 使用，把对象先转换为字符串字段，再进行拼接。

拼接所有员工姓名，默认不加分隔符：

```java
String names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.joining());
```

使用逗号分隔员工姓名：

```java
String names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.joining(","));
```

增加前缀和后缀：

```java
String names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.joining(",", "[", "]"));
```

执行结果类似如下：

```text
[张三,李四,王五,赵六,钱七,孙八,周九]
```

如果字段可能为空，建议先过滤空值，避免拼接出 `"null"`。

```java
String names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.joining(","));
```

上面代码使用 Hutool 的 `StrUtil.isNotBlank` 过滤空字符串和空白字符串。

也可以将订单商品名称拼接为展示文本：

```java
String productNames = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .map(OrderItem::productName)
        .filter(StrUtil::isNotBlank)
        .distinct()
        .collect(Collectors.joining("、"));
```

`joining` 适合生成日志文本、导出字段、接口展示字段、SQL 片段或简单的提示信息。对于复杂字符串拼接逻辑，应优先考虑单独封装方法，避免 Stream 链过长。

## 数值统计处理

本章用于说明 Stream 中的数值处理方式，包括基本类型流转换、求和、平均值、最大值、最小值和统计汇总。数值统计是 Stream 的高频使用场景，常用于年龄统计、数量汇总、订单金额计算、分页数据聚合等业务。

### mapToInt 与 mapToLong

`mapToInt`、`mapToLong` 和 `mapToDouble` 用于将普通对象流转换为基本类型流。转换后可以直接使用 `sum`、`average`、`max`、`min`、`summaryStatistics` 等数值统计方法。

普通对象流 `Stream<Employee>` 不能直接调用 `sum`。

```java
Stream<Employee> employeeStream = StreamSampleData.employees().stream();
```

需要先通过 `mapToInt` 转换为 `IntStream`。

```java
IntStream ageStream = StreamSampleData.employees()
        .stream()
        .mapToInt(Employee::age);
```

统计员工年龄列表：

```java
List<Integer> ages = StreamSampleData.employees()
        .stream()
        .mapToInt(Employee::age)
        .boxed()
        .toList();
```

`mapToInt(Employee::age)` 得到的是 `IntStream`，它不是 `Stream<Integer>`。如果需要重新收集为 `List<Integer>`，需要使用 `boxed()` 装箱。

`mapToLong` 适合处理 ID、数量、时间戳等 long 类型数据。

```java
long[] employeeIds = StreamSampleData.employees()
        .stream()
        .mapToLong(Employee::id)
        .toArray();
```

如果字段是包装类型，且可能为空，需要先过滤或设置默认值。

```java
int totalAge = StreamSampleData.employees()
        .stream()
        .map(Employee::age)
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .sum();
```

也可以使用 Hutool 的 `ObjectUtil.defaultIfNull` 做默认值兜底。

```java
int totalAge = StreamSampleData.employees()
        .stream()
        .mapToInt(employee -> ObjectUtil.defaultIfNull(employee.age(), 0))
        .sum();
```

常见基本类型流如下：

| 方法          | 返回类型       | 适用字段                         |
| ------------- | -------------- | -------------------------------- |
| `mapToInt`    | `IntStream`    | 年龄、数量、分数、库存           |
| `mapToLong`   | `LongStream`   | ID、访问量、时间戳               |
| `mapToDouble` | `DoubleStream` | 比率、浮点分数、坐标、部分统计值 |

如果是金额字段，尤其是财务金额，通常不建议使用 `double`，应优先使用 `BigDecimal` 并通过 `reduce` 进行精确计算。

### sum 求和

`sum` 用于对基本类型流进行求和。`IntStream`、`LongStream` 和 `DoubleStream` 都提供了 `sum` 方法。

统计员工年龄总和：

```java
int totalAge = StreamSampleData.employees()
        .stream()
        .mapToInt(Employee::age)
        .sum();
```

统计订单明细商品总数量：

```java
int totalQuantity = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .mapToInt(OrderItem::quantity)
        .sum();
```

统计员工 ID 总和：

```java
long totalEmployeeId = StreamSampleData.employees()
        .stream()
        .mapToLong(Employee::id)
        .sum();
```

对于 `BigDecimal` 类型，例如薪资、订单金额、支付金额，应该使用 `reduce` 求和。

统计员工薪资总和：

```java
BigDecimal totalSalary = StreamSampleData.employees()
        .stream()
        .map(Employee::salary)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

统计订单总金额：

```java
BigDecimal totalAmount = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

如果订单明细中的价格或数量可能为空，需要先做兜底处理。

```java
BigDecimal totalAmount = StreamSampleData.orders()
        .stream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
        .map(item -> {
            BigDecimal price = ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO);
            Integer quantity = ObjectUtil.defaultIfNull(item.quantity(), 0);
            return price.multiply(BigDecimal.valueOf(quantity));
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

`sum` 适合基本数值类型统计；金额类数据建议使用 `BigDecimal`，避免浮点精度问题。

### average 平均值

`average` 用于计算平均值，返回值类型是 `OptionalDouble`。之所以返回 Optional，是因为空集合无法计算平均值。

计算员工平均年龄：

```java
OptionalDouble averageAgeOptional = StreamSampleData.employees()
        .stream()
        .mapToInt(Employee::age)
        .average();
```

如果希望在没有数据时返回默认值，可以使用 `orElse`。

```java
double averageAge = StreamSampleData.employees()
        .stream()
        .mapToInt(Employee::age)
        .average()
        .orElse(0D);
```

计算启用员工的平均年龄：

```java
double averageAge = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .mapToInt(Employee::age)
        .average()
        .orElse(0D);
```

如果需要保留两位小数，可以使用 `BigDecimal` 处理结果。

```java
BigDecimal averageAge = BigDecimal.valueOf(
                StreamSampleData.employees()
                        .stream()
                        .mapToInt(Employee::age)
                        .average()
                        .orElse(0D)
        )
        .setScale(2, RoundingMode.HALF_UP);
```

对于薪资等 `BigDecimal` 类型字段，不能直接调用 `average`。可以先求总和，再除以数量。

```java
List<Employee> employees = StreamSampleData.employees();

BigDecimal totalSalary = employees.stream()
        .map(Employee::salary)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal averageSalary = CollUtil.isEmpty(employees)
        ? BigDecimal.ZERO
        : totalSalary.divide(BigDecimal.valueOf(employees.size()), 2, RoundingMode.HALF_UP);
```

如果需要只统计启用员工的平均薪资，可以先过滤再计算。

```java
List<Employee> enabledEmployees = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .toList();

BigDecimal totalSalary = enabledEmployees.stream()
        .map(Employee::salary)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal averageSalary = CollUtil.isEmpty(enabledEmployees)
        ? BigDecimal.ZERO
        : totalSalary.divide(BigDecimal.valueOf(enabledEmployees.size()), 2, RoundingMode.HALF_UP);
```

平均值计算要特别注意空集合、空字段和除法精度。金额平均值建议统一指定精度和舍入模式。

### max 与 min

`max` 和 `min` 用于获取最大值和最小值。基本类型流中可以直接调用 `max` 和 `min`，返回 `OptionalInt`、`OptionalLong` 或 `OptionalDouble`。

获取最大年龄：

```java
int maxAge = StreamSampleData.employees()
        .stream()
        .mapToInt(Employee::age)
        .max()
        .orElse(0);
```

获取最小年龄：

```java
int minAge = StreamSampleData.employees()
        .stream()
        .mapToInt(Employee::age)
        .min()
        .orElse(0);
```

对于对象流，需要通过比较器指定比较规则。

获取薪资最高的员工：

```java
Optional<Employee> maxSalaryEmployeeOptional = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary));
```

如果需要直接获取员工对象，并在不存在时返回 `null`：

```java
Employee maxSalaryEmployee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary))
        .orElse(null);
```

获取年龄最小的员工：

```java
Employee minAgeEmployee = StreamSampleData.employees()
        .stream()
        .min(Comparator.comparing(Employee::age))
        .orElse(null);
```

如果比较字段可能为空，需要使用 `Comparator.nullsLast` 或 `Comparator.nullsFirst`。

```java
Employee maxSalaryEmployee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(
                Employee::salary,
                Comparator.nullsLast(BigDecimal::compareTo)
        ))
        .orElse(null);
```

多字段比较也可以用于 `max` 和 `min`。例如获取薪资最高的员工，如果薪资相同，则比较年龄。

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .max(
                Comparator.comparing(Employee::salary)
                        .thenComparing(Employee::age)
        )
        .orElse(null);
```

`max` 和 `min` 都属于终止操作，会触发 Stream 执行。由于返回值可能为空，实际开发中应优先使用 `Optional` 进行安全处理，避免直接调用 `get()`。

不推荐：

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary))
        .get();
```

推荐：

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary))
        .orElseThrow(() -> new IllegalArgumentException("未找到员工数据"));
```

### summaryStatistics 统计汇总

`summaryStatistics` 用于一次性获取数量、总和、最小值、最大值和平均值。它适合对基本数值类型做综合统计。

统计员工年龄汇总信息：

```java
IntSummaryStatistics statistics = StreamSampleData.employees()
        .stream()
        .mapToInt(Employee::age)
        .summaryStatistics();
```

可以从统计结果中获取各项指标：

```java
long count = statistics.getCount();
int sum = statistics.getSum();
int min = statistics.getMin();
int max = statistics.getMax();
double average = statistics.getAverage();
```

输出统计结果：

```java
System.out.println("员工数量：" + statistics.getCount());
System.out.println("年龄总和：" + statistics.getSum());
System.out.println("最小年龄：" + statistics.getMin());
System.out.println("最大年龄：" + statistics.getMax());
System.out.println("平均年龄：" + statistics.getAverage());
```

统计订单商品数量汇总：

```java
IntSummaryStatistics quantityStatistics = StreamSampleData.orders()
        .stream()
        .flatMap(order -> order.items().stream())
        .mapToInt(OrderItem::quantity)
        .summaryStatistics();
```

如果是 long 类型字段，可以使用 `LongSummaryStatistics`。

```java
LongSummaryStatistics idStatistics = StreamSampleData.employees()
        .stream()
        .mapToLong(Employee::id)
        .summaryStatistics();
```

如果是 double 类型字段，可以使用 `DoubleSummaryStatistics`。

```java
DoubleSummaryStatistics salaryStatistics = StreamSampleData.employees()
        .stream()
        .map(Employee::salary)
        .filter(Objects::nonNull)
        .mapToDouble(BigDecimal::doubleValue)
        .summaryStatistics();
```

需要注意，`BigDecimal::doubleValue` 会转换为 double，可能产生精度损失。因此金额类统计如果要求精确，不建议使用 `DoubleSummaryStatistics` 作为最终财务结果，只适合临时展示或非精确统计。

如果需要对 `BigDecimal` 做精确汇总，可以单独计算数量、总和、最大值、最小值和平均值。

```java
List<BigDecimal> salaries = StreamSampleData.employees()
        .stream()
        .map(Employee::salary)
        .filter(Objects::nonNull)
        .toList();

long count = salaries.size();

BigDecimal sum = salaries.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal max = salaries.stream()
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

BigDecimal min = salaries.stream()
        .min(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

BigDecimal average = CollUtil.isEmpty(salaries)
        ? BigDecimal.ZERO
        : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
```

如果项目中多处需要金额统计，可以封装为专门的统计对象。

文件位置：`src/main/java/io/github/atengk/stream/model/AmountStatistics.java`

下面的代码定义金额统计结果对象，用于保存 BigDecimal 类型的精确统计结果。

```java
package io.github.atengk.stream.model;

import java.math.BigDecimal;

/**
 * 金额统计结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record AmountStatistics(
        long count,
        BigDecimal sum,
        BigDecimal max,
        BigDecimal min,
        BigDecimal average
) {
}
```

文件位置：`src/main/java/io/github/atengk/stream/util/AmountStatisticsUtil.java`

下面的代码封装 BigDecimal 金额统计逻辑，避免在业务代码中重复编写数量、总和、最大值、最小值和平均值计算。

```java
package io.github.atengk.stream.util;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.stream.model.AmountStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * 金额统计工具类
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class AmountStatisticsUtil {

    private AmountStatisticsUtil() {
    }

    /**
     * 统计金额集合。
     *
     * @param amounts 金额集合
     * @return 金额统计结果
     */
    public static AmountStatistics statistics(List<BigDecimal> amounts) {
        List<BigDecimal> validAmounts = CollUtil.emptyIfNull(amounts)
                .stream()
                .filter(Objects::nonNull)
                .toList();

        if (CollUtil.isEmpty(validAmounts)) {
            return new AmountStatistics(
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        long count = validAmounts.size();

        BigDecimal sum = validAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal max = validAmounts.stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal min = validAmounts.stream()
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal average = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        return new AmountStatistics(count, sum, max, min, average);
    }
}
```

调用示例：

```java
AmountStatistics statistics = AmountStatisticsUtil.statistics(
        StreamSampleData.employees()
                .stream()
                .map(Employee::salary)
                .toList()
);
```

`summaryStatistics` 适合基本类型的快速统计；如果统计字段是金额、积分余额、结算数据等对精度敏感的字段，应优先使用 `BigDecimal` 并封装专门的统计逻辑。



## Optional 与 Stream

本章用于说明 Stream 中常见的空结果处理方式。`findFirst`、`findAny`、`max`、`min` 等终止操作不会直接返回对象，而是返回 `Optional<T>`，用于显式表达“结果可能不存在”。

### findFirst 与 findAny

`findFirst` 用于获取 Stream 中第一个元素，常用于按顺序查找第一条符合条件的数据。它返回 `Optional<T>`，当 Stream 中没有匹配数据时，返回 `Optional.empty()`。

查找第一个启用员工：

```java
Optional<Employee> employeeOptional = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .findFirst();
```

如果需要在没有结果时返回 `null`，可以使用 `orElse(null)`。

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .filter(employeeItem -> Boolean.TRUE.equals(employeeItem.enabled()))
        .findFirst()
        .orElse(null);
```

如果业务要求必须存在结果，可以使用 `orElseThrow` 抛出明确异常。

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .filter(employeeItem -> employeeItem.id().equals(1L))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("未找到员工数据"));
```

`findAny` 用于获取任意一个匹配元素。在串行 Stream 中，`findAny` 通常也会返回第一个匹配元素，但它的语义不是“第一个”，而是“任意一个”。在并行 Stream 中，`findAny` 可能返回任意匹配项，因此性能上可能比 `findFirst` 更容易优化。

```java
Optional<Employee> employeeOptional = StreamSampleData.employees()
        .parallelStream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .findAny();
```

`findFirst` 和 `findAny` 的区别如下：

| 方法        | 语义                 | 是否关注顺序 | 适用场景                                      |
| ----------- | -------------------- | ------------ | --------------------------------------------- |
| `findFirst` | 获取第一个匹配元素   | 关注顺序     | 按时间、排序结果、原始顺序查找第一条数据      |
| `findAny`   | 获取任意一个匹配元素 | 不关注顺序   | 只要存在任意匹配数据即可，尤其适合并行 Stream |

如果业务逻辑依赖顺序，例如获取薪资排序后的第一名，应使用 `findFirst`。

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .filter(employeeItem -> Boolean.TRUE.equals(employeeItem.enabled()))
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("未找到启用员工数据"));
```

如果只需要判断是否存在符合条件的数据，通常不需要使用 `findFirst` 或 `findAny`，使用 `anyMatch` 更直接。

```java
boolean exists = StreamSampleData.employees()
        .stream()
        .anyMatch(employee -> employee.salary().compareTo(new BigDecimal("30000")) > 0);
```

### max 与 min 返回结果处理

`max` 和 `min` 用于获取最大值和最小值。由于 Stream 可能为空，所以它们返回的也是 `Optional<T>`。

获取薪资最高的员工：

```java
Optional<Employee> maxSalaryEmployeeOptional = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary));
```

推荐使用 `ifPresent` 处理存在结果的情况。

```java
StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary))
        .ifPresent(employee -> System.out.println("薪资最高员工：" + employee.name()));
```

如果需要返回默认对象，可以使用 `orElse`。

```java
Employee defaultEmployee = new Employee(
        0L,
        "默认员工",
        0L,
        "未知岗位",
        0,
        BigDecimal.ZERO,
        false,
        LocalDate.now()
);

Employee employee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary))
        .orElse(defaultEmployee);
```

如果默认对象创建成本较高，建议使用 `orElseGet`，避免不必要的对象创建。

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary))
        .orElseGet(() -> new Employee(
                0L,
                "默认员工",
                0L,
                "未知岗位",
                0,
                BigDecimal.ZERO,
                false,
                LocalDate.now()
        ));
```

`orElse` 和 `orElseGet` 的区别如下：

| 方法                  | 默认值执行时机             | 适用场景                             |
| --------------------- | -------------------------- | ------------------------------------ |
| `orElse(value)`       | 默认值会先创建             | 默认值简单、无额外成本               |
| `orElseGet(supplier)` | 只有 Optional 为空时才执行 | 默认值创建较复杂，或需要调用方法生成 |

如果没有结果就应该中断业务流程，推荐使用 `orElseThrow`。

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .min(Comparator.comparing(Employee::age))
        .orElseThrow(() -> new IllegalStateException("员工列表为空，无法获取年龄最小员工"));
```

需要注意，不推荐直接调用 `get()`。

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(Employee::salary))
        .get();
```

这种写法在 Stream 为空时会抛出 `NoSuchElementException`，并且异常语义不明确。更推荐使用 `orElseThrow` 给出清晰的业务异常。

如果比较字段可能为空，需要使用 `Comparator.nullsFirst` 或 `Comparator.nullsLast` 处理空值。

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .max(Comparator.comparing(
                Employee::salary,
                Comparator.nullsLast(BigDecimal::compareTo)
        ))
        .orElseThrow(() -> new IllegalStateException("员工列表为空，无法获取薪资最高员工"));
```

### Optional 常用方法

`Optional` 是一个用于表达“可能有值，也可能无值”的容器。它的目标不是替代所有空值判断，而是让返回结果的不确定性更加明确。

常用方法如下：

| 方法                | 说明                        |
| ------------------- | --------------------------- |
| `isPresent()`       | 判断是否有值                |
| `isEmpty()`         | 判断是否为空                |
| `ifPresent()`       | 有值时执行逻辑              |
| `ifPresentOrElse()` | 有值和无值时分别执行逻辑    |
| `orElse()`          | 无值时返回默认值            |
| `orElseGet()`       | 无值时通过函数生成默认值    |
| `orElseThrow()`     | 无值时抛出异常              |
| `map()`             | 有值时转换内部对象          |
| `flatMap()`         | 有值时转换为另一个 Optional |
| `filter()`          | 有值时继续按条件过滤        |

使用 `ifPresent` 处理存在结果：

```java
StreamSampleData.employees()
        .stream()
        .filter(employee -> employee.id().equals(1L))
        .findFirst()
        .ifPresent(employee -> System.out.println("找到员工：" + employee.name()));
```

使用 `ifPresentOrElse` 同时处理存在和不存在两种情况：

```java
StreamSampleData.employees()
        .stream()
        .filter(employee -> employee.id().equals(100L))
        .findFirst()
        .ifPresentOrElse(
                employee -> System.out.println("找到员工：" + employee.name()),
                () -> System.out.println("未找到员工")
        );
```

使用 `map` 提取 Optional 中对象的字段：

```java
String employeeName = StreamSampleData.employees()
        .stream()
        .filter(employee -> employee.id().equals(1L))
        .findFirst()
        .map(Employee::name)
        .orElse("未知员工");
```

使用 `filter` 对 Optional 内部对象继续判断：

```java
Employee employee = StreamSampleData.employees()
        .stream()
        .filter(employeeItem -> employeeItem.id().equals(1L))
        .findFirst()
        .filter(employeeItem -> Boolean.TRUE.equals(employeeItem.enabled()))
        .orElseThrow(() -> new IllegalArgumentException("员工不存在或已禁用"));
```

`Optional.ofNullable` 适合把可能为空的对象包装为 Optional。

```java
Employee employee = null;

String employeeName = Optional.ofNullable(employee)
        .map(Employee::name)
        .orElse("未知员工");
```

使用 Hutool 时，也可以在简单默认值场景中使用 `ObjectUtil.defaultIfNull`。它适合普通对象兜底，不适合表达复杂的链式查找逻辑。

```java
String name = ObjectUtil.defaultIfNull(employeeName, "未知员工");
```

`Optional` 推荐用于方法返回值，表示调用方必须处理空结果。不建议把实体类字段定义为 `Optional`，也不建议把方法参数定义为 `Optional`。

推荐：

```java
public Optional<Employee> findEmployeeById(Long employeeId) {
    return StreamSampleData.employees()
            .stream()
            .filter(employee -> employee.id().equals(employeeId))
            .findFirst();
}
```

不推荐：

```java
public Employee findEmployeeById(Optional<Long> employeeIdOptional) {
    return null;
}
```

### 空值处理建议

Stream 中的空值处理需要分清三类情况：数据源为空、集合中元素为空、对象字段为空。不同情况应采用不同处理方式。

第一，数据源集合可能为 `null`。这种情况下，不能直接调用 `list.stream()`，否则会抛出 `NullPointerException`。可以使用 Hutool 的 `CollUtil.emptyIfNull` 兜底。

```java
List<Employee> employees = null;

List<String> names = CollUtil.emptyIfNull(employees)
        .stream()
        .map(Employee::name)
        .toList();
```

第二，集合中的元素可能为 `null`。这种情况下，应先过滤空元素。

```java
List<Employee> employees = CollUtil.newArrayList(
        StreamSampleData.employees().getFirst(),
        null,
        StreamSampleData.employees().getLast()
);

List<String> names = employees.stream()
        .filter(Objects::nonNull)
        .map(Employee::name)
        .toList();
```

第三，对象字段可能为 `null`。这种情况下，可以使用 `filter`、`Optional`、`ObjectUtil.defaultIfNull` 或 `StrUtil` 进行处理。

过滤姓名不为空的员工：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .filter(employee -> StrUtil.isNotBlank(employee.name()))
        .toList();
```

薪资为空时按 `BigDecimal.ZERO` 处理：

```java
BigDecimal totalSalary = StreamSampleData.employees()
        .stream()
        .map(employee -> ObjectUtil.defaultIfNull(employee.salary(), BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

布尔包装类型建议使用 `Boolean.TRUE.equals(...)` 判断，避免空指针异常。

```java
List<Employee> enabledEmployees = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .toList();
```

排序字段可能为空时，需要显式指定空值排序规则。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(
                Employee::salary,
                Comparator.nullsLast(BigDecimal::compareTo)
        ))
        .toList();
```

嵌套集合可能为空时，建议在 `flatMap` 中兜底。

```java
List<OrderItem> items = StreamSampleData.orders()
        .stream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
        .toList();
```

空值处理建议如下：

| 场景                  | 推荐处理方式                                    |
| --------------------- | ----------------------------------------------- |
| 集合对象可能为 `null` | `CollUtil.emptyIfNull(list).stream()`           |
| 集合元素可能为 `null` | `.filter(Objects::nonNull)`                     |
| 字符串字段可能为空    | `StrUtil.isNotBlank(value)`                     |
| 布尔包装类型可能为空  | `Boolean.TRUE.equals(value)`                    |
| 数值字段可能为空      | `ObjectUtil.defaultIfNull(value, defaultValue)` |
| 查找结果可能不存在    | `Optional` + `orElse` / `orElseThrow`           |
| 嵌套集合可能为空      | `CollUtil.emptyIfNull(childList).stream()`      |

实际开发中不要为了追求链式写法而让 Stream 变得难以阅读。空值处理逻辑较复杂时，可以先拆成局部变量或独立方法。

## 并行 Stream

本章用于说明并行 Stream 的基础使用方式、执行特点、适用场景和常见风险。并行 Stream 可以利用多线程处理数据，但它不是性能优化的通用方案，使用前需要明确数据规模、任务类型、线程安全和执行顺序要求。

### parallelStream 基础使用

集合可以通过 `parallelStream()` 创建并行 Stream，也可以先创建普通 Stream，再调用 `parallel()` 转换为并行 Stream。

使用 `parallelStream()` 统计启用员工数量：

```java
long enabledCount = StreamSampleData.employees()
        .parallelStream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .count();
```

使用 `parallel()` 转换为并行 Stream：

```java
long enabledCount = StreamSampleData.employees()
        .stream()
        .parallel()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .count();
```

并行 Stream 的写法与普通 Stream 基本一致，但底层会尝试把数据拆分成多个任务并行执行。

下面的代码演示并行处理时打印当前线程名称。

```java
StreamSampleData.employees()
        .parallelStream()
        .forEach(employee -> System.out.println(
                Thread.currentThread().getName() + " 处理员工：" + employee.name()
        ));
```

输出顺序可能不是原始集合顺序，因为多个线程会并行处理不同元素。

如果需要保持最终处理顺序，可以使用 `forEachOrdered`。

```java
StreamSampleData.employees()
        .parallelStream()
        .forEachOrdered(employee -> System.out.println("处理员工：" + employee.name()));
```

需要注意，`forEachOrdered` 会维持顺序，但也可能降低并行 Stream 的性能收益。

### 并行 Stream 执行特点

并行 Stream 通常基于公共的 `ForkJoinPool.commonPool()` 执行。它会尝试把数据源拆分成多个子任务，再由多个线程并行处理，最后合并结果。

并行 Stream 的核心特点如下：

| 特点           | 说明                                   |
| -------------- | -------------------------------------- |
| 多线程执行     | 同一条 Stream 流水线可能由多个线程处理 |
| 数据拆分       | 数据源会被拆分成多个子任务             |
| 结果合并       | 子任务处理结果会被合并成最终结果       |
| 顺序不稳定     | `forEach`、`findAny` 等操作不保证顺序  |
| 有额外开销     | 任务拆分、线程调度、结果合并都有成本   |
| 使用公共线程池 | 默认使用 `ForkJoinPool.commonPool()`   |

下面代码可以观察并行 Stream 的执行线程。

```java
List<String> names = StreamSampleData.employees()
        .parallelStream()
        .map(employee -> {
            System.out.println(Thread.currentThread().getName() + " 转换员工：" + employee.name());
            return employee.name();
        })
        .toList();
```

并行 Stream 中，`findAny` 通常比 `findFirst` 更适合无顺序要求的场景。

```java
Optional<Employee> employeeOptional = StreamSampleData.employees()
        .parallelStream()
        .filter(employee -> employee.salary().compareTo(new BigDecimal("20000")) > 0)
        .findAny();
```

如果必须按原始顺序获取第一条匹配数据，应使用 `findFirst`，但它可能削弱并行性能。

```java
Optional<Employee> employeeOptional = StreamSampleData.employees()
        .parallelStream()
        .filter(employee -> employee.salary().compareTo(new BigDecimal("20000")) > 0)
        .findFirst();
```

并行 Stream 对数据源也有要求。`ArrayList`、数组、`IntStream.range` 这类容易拆分的数据源更适合并行；`LinkedList`、I/O 流、迭代器来源的数据通常拆分成本较高，并行收益有限。

### 适用场景

并行 Stream 适合数据量较大、计算密集、无共享状态、无顺序要求的数据处理场景。它更适合 CPU 密集型计算，而不是数据库查询、远程接口调用或文件写入等 I/O 操作。

适合场景如下：

| 场景           | 说明                             |
| -------------- | -------------------------------- |
| 大集合计算     | 数据量较大，单元素处理成本较高   |
| CPU 密集型任务 | 加密、压缩、复杂计算、规则匹配等 |
| 无共享状态     | 每个元素处理互不影响             |
| 无顺序依赖     | 不依赖原始集合顺序               |
| 易拆分数据源   | 数组、`ArrayList`、数值范围等    |

示例：批量计算订单明细金额。

```java
BigDecimal totalAmount = StreamSampleData.orders()
        .parallelStream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
        .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

示例：对大量数字执行计算密集型操作。

```java
long result = LongStream.rangeClosed(1, 10_000_000)
        .parallel()
        .map(number -> number * number)
        .sum();
```

示例：无顺序要求时查找任意符合条件的员工。

```java
Optional<Employee> employeeOptional = StreamSampleData.employees()
        .parallelStream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .filter(employee -> employee.salary().compareTo(new BigDecimal("30000")) > 0)
        .findAny();
```

并行 Stream 使用前建议先确认三个问题：

| 问题                   | 判断标准                               |
| ---------------------- | -------------------------------------- |
| 数据量是否足够大       | 小集合通常不值得并行                   |
| 单个元素处理是否足够重 | 简单字段提取、字符串拼接通常不值得并行 |
| 是否没有共享状态       | 不应修改共享集合、共享变量或外部资源   |

如果这三个条件不满足，优先使用普通 `stream()`。

### 使用风险

并行 Stream 最大的风险来自共享状态、副作用操作、顺序依赖和公共线程池竞争。它可以让代码看起来很简洁，但并不代表逻辑一定安全或性能一定更好。

第一，不要在并行 Stream 中修改普通集合。

错误示例：

```java
List<String> names = new ArrayList<>();

StreamSampleData.employees()
        .parallelStream()
        .forEach(employee -> names.add(employee.name()));
```

`ArrayList` 不是线程安全集合，并行写入可能导致数据丢失、顺序混乱，甚至抛出异常。

正确做法是使用 Stream 自身的收集操作。

```java
List<String> names = StreamSampleData.employees()
        .parallelStream()
        .map(Employee::name)
        .toList();
```

第二，不要在并行 Stream 中修改共享变量。

错误示例：

```java
AtomicInteger totalAge = new AtomicInteger();

StreamSampleData.employees()
        .parallelStream()
        .forEach(employee -> totalAge.addAndGet(employee.age()));
```

这段代码虽然使用了 `AtomicInteger`，线程安全问题有所缓解，但仍然不是推荐写法。Stream 更推荐使用无副作用的归约操作。

推荐写法：

```java
int totalAge = StreamSampleData.employees()
        .parallelStream()
        .mapToInt(Employee::age)
        .sum();
```

第三，谨慎在并行 Stream 中执行 I/O 操作。

不推荐：

```java
StreamSampleData.employees()
        .parallelStream()
        .forEach(employee -> {
            // 不推荐在并行 Stream 中调用数据库、远程接口或写文件
            System.out.println("同步员工：" + employee.name());
        });
```

数据库连接池、HTTP 连接池、文件句柄等资源通常都有并发限制。并行 Stream 默认使用公共线程池，容易造成资源争用，甚至影响应用中其他并行任务。

第四，不要依赖 `forEach` 的执行顺序。

```java
StreamSampleData.employees()
        .parallelStream()
        .forEach(employee -> System.out.println(employee.name()));
```

这段代码的输出顺序不一定与原始员工列表一致。如果必须保持顺序，应使用 `forEachOrdered`，或者改用普通 Stream。

```java
StreamSampleData.employees()
        .parallelStream()
        .forEachOrdered(employee -> System.out.println(employee.name()));
```

第五，避免在并行 Stream 中使用复杂的有状态 Lambda。

不推荐：

```java
Set<Long> departmentIds = new HashSet<>();

List<Employee> employees = StreamSampleData.employees()
        .parallelStream()
        .filter(employee -> departmentIds.add(employee.departmentId()))
        .toList();
```

`HashSet` 不是线程安全集合，这种写法在并行场景下存在并发问题。按字段去重更推荐使用 `toMap` 或分组收集。

```java
List<Employee> employees = StreamSampleData.employees()
        .parallelStream()
        .collect(Collectors.toMap(
                Employee::departmentId,
                Function.identity(),
                (first, second) -> first,
                ConcurrentHashMap::new
        ))
        .values()
        .stream()
        .toList();
```

第六，小数据量使用并行 Stream 可能更慢。

```java
List<String> names = StreamSampleData.employees()
        .parallelStream()
        .map(Employee::name)
        .toList();
```

如果员工列表只有几十条或几百条，任务拆分、线程调度和结果合并的成本可能高于并行带来的收益。普通写法通常更合适。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();
```

并行 Stream 使用建议如下：

| 建议                | 说明                                                |
| ------------------- | --------------------------------------------------- |
| 默认使用 `stream()` | 普通业务处理优先串行 Stream                         |
| 不要修改外部集合    | 使用 `map`、`collect`、`reduce` 生成结果            |
| 不要依赖执行顺序    | 有顺序要求时使用串行 Stream 或 `forEachOrdered`     |
| 避免 I/O 操作       | 数据库、远程接口、文件操作不适合直接放入并行 Stream |
| 先验证再优化        | 并行 Stream 是否更快需要压测或基准测试确认          |
| 关注公共线程池      | 避免影响同一 JVM 中其他并行任务                     |

实际开发中，并行 Stream 应被视为一种有条件的性能优化手段，而不是默认写法。对于大多数业务集合处理，普通 Stream 的可读性、稳定性和可控性更好。



## JDK 21 中的 Stream 相关增强

本章用于说明在 JDK 21 环境下编写 Stream 代码时经常配合使用的能力，包括 `Stream.toList()`、`takeWhile`、`dropWhile`、`Stream.iterate` 重载方法、`record` 以及 Pattern Matching。这些能力并不全部是在 JDK 21 才首次出现，但在 JDK 21 项目中可以稳定使用。

### Stream.toList 使用建议

`Stream.toList()` 是 JDK 16 引入的终止操作，在 JDK 21 中可以直接作为收集 List 的常用写法。它的语义清晰，代码比 `collect(Collectors.toList())` 更简洁。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();
```

需要注意，`Stream.toList()` 返回的是不可变 List，不能继续执行 `add`、`remove`、`clear` 等修改操作。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();

// 会抛出 UnsupportedOperationException
names.add("新员工");
```

如果结果列表只用于读取、遍历、接口返回、日志输出或继续参与计算，推荐使用 `toList()`。

```java
List<Employee> enabledEmployees = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .toList();
```

如果后续需要修改结果集合，应明确使用 `Collectors.toCollection(ArrayList::new)`。

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .collect(Collectors.toCollection(ArrayList::new));

names.add("新员工");
```

实际开发中建议按照以下规则选择：

| 场景                 | 推荐写法                                           |
| -------------------- | -------------------------------------------------- |
| 只读结果             | `stream.toList()`                                  |
| 需要可变 `ArrayList` | `collect(Collectors.toCollection(ArrayList::new))` |
| 需要指定集合类型     | `collect(Collectors.toCollection(...))`            |
| 需要收集后再转换     | `collect(Collectors.collectingAndThen(...))`       |
| 旧代码兼容           | 可以保留 `collect(Collectors.toList())`            |

如果需要返回不可变集合，并且希望表达“结果不允许修改”的语义，`toList()` 是更合适的选择。

```java
public List<String> listEnabledEmployeeNames() {
    return StreamSampleData.employees()
            .stream()
            .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
            .map(Employee::name)
            .toList();
}
```

如果方法内部需要继续追加数据，则不要使用 `toList()` 作为中间结果。

```java
public List<String> listEmployeeNamesWithDefault() {
    List<String> names = StreamSampleData.employees()
            .stream()
            .map(Employee::name)
            .collect(Collectors.toCollection(ArrayList::new));

    names.add("默认员工");
    return names;
}
```

### takeWhile 与 dropWhile

`takeWhile` 和 `dropWhile` 是 JDK 9 引入的 Stream 操作，在 JDK 21 中可以稳定使用。它们适合处理有序数据，按照条件从流的开头进行截取或丢弃。

`takeWhile` 表示：从 Stream 开头开始取元素，直到第一次条件不满足为止。

```java
List<Integer> numbers = Stream.of(1, 2, 3, 4, 5, 1, 2)
        .takeWhile(number -> number < 4)
        .toList();
```

执行结果为：

```text
[1, 2, 3]
```

注意，`takeWhile` 不是过滤所有满足条件的元素。它只从开头连续截取，一旦遇到第一个不满足条件的元素，就停止处理。

下面代码不会返回 `[1, 2, 3, 1, 2]`：

```java
List<Integer> numbers = Stream.of(1, 2, 3, 4, 5, 1, 2)
        .takeWhile(number -> number < 4)
        .toList();
```

如果想要过滤所有小于 4 的元素，应使用 `filter`。

```java
List<Integer> numbers = Stream.of(1, 2, 3, 4, 5, 1, 2)
        .filter(number -> number < 4)
        .toList();
```

`dropWhile` 表示：从 Stream 开头开始丢弃元素，直到第一次条件不满足为止，之后的元素全部保留。

```java
List<Integer> numbers = Stream.of(1, 2, 3, 4, 5, 1, 2)
        .dropWhile(number -> number < 4)
        .toList();
```

执行结果为：

```text
[4, 5, 1, 2]
```

在业务场景中，`takeWhile` 和 `dropWhile` 常用于处理已经排序的数据。例如，先按入职日期升序排序，然后获取 2020 年以前入职的员工。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(Employee::entryDate))
        .takeWhile(employee -> employee.entryDate().isBefore(LocalDate.of(2020, 1, 1)))
        .toList();
```

跳过 2020 年以前入职的员工，保留后续员工：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(Employee::entryDate))
        .dropWhile(employee -> employee.entryDate().isBefore(LocalDate.of(2020, 1, 1)))
        .toList();
```

`takeWhile`、`dropWhile` 与 `filter` 的区别如下：

| 方法        | 处理方式                     | 是否遇到不满足条件后停止 | 适用场景         |
| ----------- | ---------------------------- | ------------------------ | ---------------- |
| `filter`    | 筛选所有满足条件的元素       | 否                       | 普通过滤         |
| `takeWhile` | 从开头连续取满足条件的元素   | 是                       | 有序数据前段截取 |
| `dropWhile` | 从开头连续丢弃满足条件的元素 | 是                       | 有序数据前段跳过 |

如果数据没有明确顺序，或者业务上不是“从开头连续处理”，应优先使用 `filter`，不要误用 `takeWhile` 和 `dropWhile`。

### iterate 重载方法

`Stream.iterate` 用于根据初始值和迭代规则生成 Stream。它适合生成等差序列、日期序列、分页参数、批次编号等数据。

传统 `iterate` 写法需要配合 `limit` 限制数量，否则会生成无限流。

```java
List<Integer> numbers = Stream.iterate(1, number -> number + 1)
        .limit(5)
        .toList();
```

执行结果为：

```text
[1, 2, 3, 4, 5]
```

JDK 9 之后，`Stream.iterate` 提供了带终止条件的重载方法，JDK 21 中可以继续使用。它的形式更接近普通 `for` 循环。

```java
List<Integer> numbers = Stream.iterate(
                1,
                number -> number <= 5,
                number -> number + 1
        )
        .toList();
```

执行结果为：

```text
[1, 2, 3, 4, 5]
```

生成最近 7 天的日期列表：

```java
List<LocalDate> dates = Stream.iterate(
                LocalDate.now().minusDays(6),
                date -> !date.isAfter(LocalDate.now()),
                date -> date.plusDays(1)
        )
        .toList();
```

生成分页页码：

```java
int totalPage = 5;

List<Integer> pageNumbers = Stream.iterate(
                1,
                page -> page <= totalPage,
                page -> page + 1
        )
        .toList();
```

生成批处理偏移量：

```java
int total = 1050;
int batchSize = 200;

List<Integer> offsets = Stream.iterate(
                0,
                offset -> offset < total,
                offset -> offset + batchSize
        )
        .toList();
```

执行结果类似如下：

```text
[0, 200, 400, 600, 800, 1000]
```

如果需要处理分页查询，可以基于页码 Stream 生成每一页的查询参数。

```java
int totalPage = 3;

List<String> pageDescriptions = Stream.iterate(
                1,
                page -> page <= totalPage,
                page -> page + 1
        )
        .map(page -> "第 " + page + " 页")
        .toList();
```

使用 `iterate` 时需要注意：

| 注意点             | 说明                            |
| ------------------ | ------------------------------- |
| 必须有终止条件     | 无限流需要 `limit` 或带条件重载 |
| 迭代函数应可预测   | 避免依赖外部可变状态            |
| 不适合复杂业务循环 | 复杂循环使用普通 `for` 更清晰   |
| 注意时间边界       | 日期序列要明确开始和结束条件    |

### 与 Record 搭配使用

JDK 21 中可以稳定使用 `record`。`record` 适合定义不可变的数据载体，用于 Stream 的中间结果、接口返回对象、统计结果和分组结果。

在前面的示例中，`Employee`、`Order`、`OrderItem` 都可以使用 `record` 定义。`record` 与 Stream 搭配时，可以减少样板代码，让转换逻辑更清晰。

文件位置：`src/main/java/io/github/atengk/stream/model/EmployeeView.java`

下面的代码定义员工展示对象，用于演示 Stream 将实体转换为展示结果。

```java
package io.github.atengk.stream.model;

import java.math.BigDecimal;

/**
 * 员工展示对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record EmployeeView(
        Long id,
        String name,
        String position,
        BigDecimal salaryText
) {
}
```

使用 `map` 将员工实体转换为 `EmployeeView`：

```java
List<EmployeeView> views = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .map(employee -> new EmployeeView(
                employee.id(),
                employee.name(),
                employee.position(),
                employee.salary()
        ))
        .toList();
```

分组统计时，也可以使用 `record` 保存结果。

文件位置：`src/main/java/io/github/atengk/stream/model/DepartmentEmployeeStatistics.java`

下面的代码定义部门员工统计结果，用于保存每个部门的人数和薪资总额。

```java
package io.github.atengk.stream.model;

import java.math.BigDecimal;

/**
 * 部门员工统计结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record DepartmentEmployeeStatistics(
        Long departmentId,
        long employeeCount,
        BigDecimal totalSalary
) {
}
```

基于 Stream 生成部门统计结果：

```java
List<DepartmentEmployeeStatistics> statisticsList = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(Employee::departmentId))
        .entrySet()
        .stream()
        .map(entry -> {
            Long departmentId = entry.getKey();
            List<Employee> employees = entry.getValue();

            BigDecimal totalSalary = employees.stream()
                    .map(Employee::salary)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new DepartmentEmployeeStatistics(
                    departmentId,
                    employees.size(),
                    totalSalary
            );
        })
        .toList();
```

`record` 的优势如下：

| 优势             | 说明                                                       |
| ---------------- | ---------------------------------------------------------- |
| 代码简洁         | 自动生成构造器、访问方法、`equals`、`hashCode`、`toString` |
| 默认不可变       | 字段为 `final` 语义，适合作为 Stream 结果对象              |
| 适合临时结果     | 可用于统计结果、转换结果、聚合结果                         |
| 与模式匹配配合好 | 可以结合 record pattern 提取字段                           |

使用建议：

```java
// 推荐：用 record 表达 Stream 转换结果
public record EmployeeOption(Long value, String label) {
}

// 推荐：Stream 中直接生成轻量结果对象
List<EmployeeOption> options = StreamSampleData.employees()
        .stream()
        .map(employee -> new EmployeeOption(employee.id(), employee.name()))
        .toList();
```

不建议把复杂业务行为放进 `record`。如果对象包含大量状态变更、复杂校验、领域行为或生命周期管理，普通 class 会更合适。

### 与 Pattern Matching 搭配使用

JDK 21 中可以使用 Pattern Matching for `switch` 和 Record Patterns。它们与 Stream 搭配时，适合处理多类型数据流、事件流、命令流或混合结果集合。

例如，系统中可能有多种待处理对象：员工、订单、订单明细。可以使用 Pattern Matching 对不同类型进行分类转换。

文件位置：`src/main/java/io/github/atengk/stream/model/DataLabel.java`

下面的代码定义统一展示标签对象，用于保存不同类型数据转换后的展示结果。

```java
package io.github.atengk.stream.model;

/**
 * 数据展示标签
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record DataLabel(
        String type,
        String label
) {
}
```

下面的代码演示在 Stream 的 `map` 操作中使用 Pattern Matching for `switch` 处理不同类型对象。

```java
List<Object> sourceList = CollUtil.newArrayList(
        StreamSampleData.employees().getFirst(),
        StreamSampleData.orders().getFirst(),
        StreamSampleData.orders().getFirst().items().getFirst(),
        "其他数据"
);

List<DataLabel> labels = sourceList.stream()
        .map(source -> switch (source) {
            case Employee employee -> new DataLabel("EMPLOYEE", employee.name());
            case Order order -> new DataLabel("ORDER", String.valueOf(order.id()));
            case OrderItem item -> new DataLabel("ORDER_ITEM", item.productName());
            case null, default -> new DataLabel("UNKNOWN", "未知数据");
        })
        .toList();
```

如果需要在类型匹配的同时增加条件判断，可以使用 `when` 保护条件。

```java
List<DataLabel> labels = sourceList.stream()
        .map(source -> switch (source) {
            case Employee employee when Boolean.TRUE.equals(employee.enabled()) ->
                    new DataLabel("ENABLED_EMPLOYEE", employee.name());
            case Employee employee ->
                    new DataLabel("DISABLED_EMPLOYEE", employee.name());
            case Order order when OrderStatus.PAID.equals(order.status()) ->
                    new DataLabel("PAID_ORDER", String.valueOf(order.id()));
            case Order order ->
                    new DataLabel("OTHER_ORDER", String.valueOf(order.id()));
            case OrderItem item ->
                    new DataLabel("ORDER_ITEM", item.productName());
            case null, default ->
                    new DataLabel("UNKNOWN", "未知数据");
        })
        .toList();
```

Record Patterns 可以直接解构 `record` 对象，适合从多类型对象中提取字段。

```java
List<String> labels = sourceList.stream()
        .map(source -> switch (source) {
            case Employee(Long id, String name, Long departmentId, String position,
                          Integer age, BigDecimal salary, Boolean enabled, LocalDate entryDate) ->
                    "员工：" + name + "，岗位：" + position;
            case Order(Long id, String customerNo, OrderStatus status, LocalDateTime createTime, List<OrderItem> items) ->
                    "订单：" + id + "，状态：" + status;
            case OrderItem(Long id, Long orderId, String productName, Integer quantity, BigDecimal price) ->
                    "商品：" + productName + "，数量：" + quantity;
            case null, default ->
                    "未知数据";
        })
        .toList();
```

Pattern Matching 与 Stream 搭配的适用场景如下：

| 场景           | 说明                               |
| -------------- | ---------------------------------- |
| 多类型数据转换 | `List<Object>`、事件列表、消息列表 |
| 命令分发       | 根据命令类型转换为不同处理结果     |
| 日志展示       | 对不同类型对象生成统一日志文本     |
| 规则处理       | 根据对象类型和字段条件执行不同逻辑 |
| record 解构    | 直接提取 record 字段参与转换       |

需要注意，Pattern Matching 不应该被滥用。如果集合中元素类型本来就统一，直接使用普通方法引用或 Lambda 更清晰。

```java
// 类型统一时，普通写法更清晰
List<String> employeeNames = StreamSampleData.employees()
        .stream()
        .map(Employee::name)
        .toList();
```

Pattern Matching 更适合类型确实存在差异的场景，而不是为了使用新语法强行改写普通 Stream 代码。

## 实战场景

本章围绕业务开发中最常见的 Stream 使用场景展开，包括列表过滤与字段提取、对象列表转 Map、多条件分组统计、嵌套集合展开、数据去重与排序。示例继续使用前面准备的员工、部门、订单和订单明细数据。

### 列表过滤与字段提取

列表过滤与字段提取是 Stream 最基础、也是最常见的使用场景。它通常用于从对象列表中筛选符合条件的数据，然后提取 ID、名称、编码等字段。

场景一：筛选启用员工，并提取员工 ID。

```java
List<Long> enabledEmployeeIds = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .map(Employee::id)
        .toList();
```

场景二：筛选研发部员工，并提取姓名。

```java
Long developDepartmentId = 1L;

List<String> names = StreamSampleData.employees()
        .stream()
        .filter(employee -> developDepartmentId.equals(employee.departmentId()))
        .map(Employee::name)
        .toList();
```

场景三：筛选启用且薪资大于 20000 的员工，转换为展示对象。

文件位置：`src/main/java/io/github/atengk/stream/model/EmployeeOption.java`

下面的代码定义员工下拉选项对象，用于接口返回或前端展示。

```java
package io.github.atengk.stream.model;

/**
 * 员工下拉选项
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record EmployeeOption(
        Long value,
        String label
) {
}
```

转换代码如下：

```java
List<EmployeeOption> options = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .filter(employee -> employee.salary().compareTo(new BigDecimal("20000")) > 0)
        .map(employee -> new EmployeeOption(employee.id(), employee.name()))
        .toList();
```

场景四：过滤空集合和空字段。实际开发中，接口返回数据或外部系统数据可能存在空值，可以使用 Hutool 兜底。

```java
List<Employee> employees = null;

List<String> names = CollUtil.emptyIfNull(employees)
        .stream()
        .filter(Objects::nonNull)
        .map(Employee::name)
        .filter(StrUtil::isNotBlank)
        .toList();
```

这种写法可以同时避免集合为空、元素为空、字段为空导致的问题。

### 对象列表转 Map

对象列表转 Map 常用于根据 ID 快速查询对象、构建字典映射、数据关联、减少循环嵌套等场景。使用 `Collectors.toMap` 时，需要重点处理重复 key。

场景一：按员工 ID 构建员工 Map。

```java
Map<Long, Employee> employeeMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
        ));
```

使用方式：

```java
Employee employee = employeeMap.get(1L);
```

场景二：按员工 ID 构建员工姓名 Map。

```java
Map<Long, String> employeeNameMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                Employee::name,
                (first, second) -> first,
                LinkedHashMap::new
        ));
```

场景三：构建部门 ID 和部门名称的映射。

```java
Map<Long, String> departmentNameMap = StreamSampleData.departments()
        .stream()
        .collect(Collectors.toMap(
                Department::id,
                Department::name,
                (first, second) -> first,
                LinkedHashMap::new
        ));
```

场景四：员工列表关联部门名称，生成展示对象。

文件位置：`src/main/java/io/github/atengk/stream/model/EmployeeDetailView.java`

下面的代码定义员工详情展示对象，用于保存员工信息和部门名称。

```java
package io.github.atengk.stream.model;

import java.math.BigDecimal;

/**
 * 员工详情展示对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record EmployeeDetailView(
        Long id,
        String name,
        String departmentName,
        String position,
        BigDecimal salary
) {
}
```

转换代码如下：

```java
Map<Long, String> departmentNameMap = StreamSampleData.departments()
        .stream()
        .collect(Collectors.toMap(
                Department::id,
                Department::name,
                (first, second) -> first,
                LinkedHashMap::new
        ));

List<EmployeeDetailView> views = StreamSampleData.employees()
        .stream()
        .map(employee -> new EmployeeDetailView(
                employee.id(),
                employee.name(),
                ObjectUtil.defaultIfNull(departmentNameMap.get(employee.departmentId()), "未知部门"),
                employee.position(),
                employee.salary()
        ))
        .toList();
```

场景五：按客户编号构建订单列表 Map。由于一个客户可能有多个订单，应使用 `groupingBy`，而不是 `toMap`。

```java
Map<String, List<Order>> customerOrderMap = StreamSampleData.orders()
        .stream()
        .collect(Collectors.groupingBy(
                Order::customerNo,
                LinkedHashMap::new,
                Collectors.toList()
        ));
```

对象列表转 Map 的建议如下：

| 场景                  | 推荐方式                              |
| --------------------- | ------------------------------------- |
| 一个 key 对应一个对象 | `Collectors.toMap`                    |
| 一个 key 对应多个对象 | `Collectors.groupingBy`               |
| key 可能重复          | 必须指定合并函数                      |
| 需要保持顺序          | 指定 `LinkedHashMap::new`             |
| value 是对象本身      | 使用 `Function.identity()`            |
| value 是字段          | 使用字段方法引用，如 `Employee::name` |

### 多条件分组统计

多条件分组统计通常用于报表、看板、列表汇总、导出统计等场景。常见需求包括按部门统计员工数量、按部门和启用状态统计人数、按订单状态统计金额等。

场景一：按部门统计员工数量。

```java
Map<Long, Long> departmentEmployeeCountMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                LinkedHashMap::new,
                Collectors.counting()
        ));
```

场景二：按部门统计薪资总额。

```java
Map<Long, BigDecimal> departmentSalaryMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                LinkedHashMap::new,
                Collectors.mapping(
                        employee -> ObjectUtil.defaultIfNull(employee.salary(), BigDecimal.ZERO),
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
        ));
```

场景三：按部门和启用状态进行二级分组。

```java
Map<Long, Map<Boolean, List<Employee>>> departmentEnabledMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                LinkedHashMap::new,
                Collectors.groupingBy(
                        employee -> Boolean.TRUE.equals(employee.enabled()),
                        LinkedHashMap::new,
                        Collectors.toList()
                )
        ));
```

场景四：按部门和启用状态统计人数。

```java
Map<Long, Map<Boolean, Long>> departmentEnabledCountMap = StreamSampleData.employees()
        .stream()
        .collect(Collectors.groupingBy(
                Employee::departmentId,
                LinkedHashMap::new,
                Collectors.groupingBy(
                        employee -> Boolean.TRUE.equals(employee.enabled()),
                        LinkedHashMap::new,
                        Collectors.counting()
                )
        ));
```

场景五：按订单状态统计订单数量。

```java
Map<OrderStatus, Long> orderStatusCountMap = StreamSampleData.orders()
        .stream()
        .collect(Collectors.groupingBy(
                Order::status,
                LinkedHashMap::new,
                Collectors.counting()
        ));
```

场景六：按订单状态统计订单金额。

```java
Map<OrderStatus, BigDecimal> orderStatusAmountMap = StreamSampleData.orders()
        .stream()
        .collect(Collectors.groupingBy(
                Order::status,
                LinkedHashMap::new,
                Collectors.mapping(
                        order -> CollUtil.emptyIfNull(order.items())
                                .stream()
                                .map(item -> {
                                    BigDecimal price = ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO);
                                    Integer quantity = ObjectUtil.defaultIfNull(item.quantity(), 0);
                                    return price.multiply(BigDecimal.valueOf(quantity));
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
        ));
```

如果分组统计结果需要返回给接口或前端，不建议直接暴露嵌套 Map。可以转换为明确的统计对象。

文件位置：`src/main/java/io/github/atengk/stream/model/DepartmentEnabledStatistics.java`

下面的代码定义部门启用状态统计对象，用于表达部门下启用和禁用员工数量。

```java
package io.github.atengk.stream.model;

/**
 * 部门启用状态统计
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record DepartmentEnabledStatistics(
        Long departmentId,
        long enabledCount,
        long disabledCount
) {
}
```

基于分组结果转换为统计对象：

```java
List<DepartmentEnabledStatistics> statisticsList = departmentEnabledCountMap.entrySet()
        .stream()
        .map(entry -> {
            Long departmentId = entry.getKey();
            Map<Boolean, Long> countMap = entry.getValue();

            return new DepartmentEnabledStatistics(
                    departmentId,
                    ObjectUtil.defaultIfNull(countMap.get(Boolean.TRUE), 0L),
                    ObjectUtil.defaultIfNull(countMap.get(Boolean.FALSE), 0L)
            );
        })
        .toList();
```

多条件分组建议如下：

| 建议                      | 说明                                       |
| ------------------------- | ------------------------------------------ |
| 分组层级不要过深          | 超过两层后可读性明显下降                   |
| 统计结果优先转对象        | 接口返回不要直接暴露复杂嵌套 Map           |
| 金额统计使用 `BigDecimal` | 避免使用 `double` 做财务金额               |
| 空值字段先兜底            | 使用 `ObjectUtil.defaultIfNull` 或过滤空值 |
| 复杂统计可拆分            | 不要强行写成一条超长 Stream                |

### 嵌套集合展开

嵌套集合展开通常使用 `flatMap`，适合处理一对多结构，例如订单包含订单明细、用户包含角色、角色包含权限、文章包含标签等。

场景一：展开所有订单明细。

```java
List<OrderItem> items = StreamSampleData.orders()
        .stream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
        .toList();
```

场景二：提取所有商品名称并去重。

```java
List<String> productNames = StreamSampleData.orders()
        .stream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
        .map(OrderItem::productName)
        .filter(StrUtil::isNotBlank)
        .distinct()
        .toList();
```

场景三：统计所有订单明细商品总数量。

```java
int totalQuantity = StreamSampleData.orders()
        .stream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
        .mapToInt(item -> ObjectUtil.defaultIfNull(item.quantity(), 0))
        .sum();
```

场景四：计算所有订单明细总金额。

```java
BigDecimal totalAmount = StreamSampleData.orders()
        .stream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
        .map(item -> {
            BigDecimal price = ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO);
            Integer quantity = ObjectUtil.defaultIfNull(item.quantity(), 0);
            return price.multiply(BigDecimal.valueOf(quantity));
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

场景五：把订单和订单明细组合为明细展示对象。由于 `flatMap` 中需要同时使用订单和明细，可以在内部 `map` 中创建结果对象。

文件位置：`src/main/java/io/github/atengk/stream/model/OrderItemView.java`

下面的代码定义订单明细展示对象，用于保存订单信息和商品明细信息。

```java
package io.github.atengk.stream.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细展示对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
public record OrderItemView(
        Long orderId,
        String customerNo,
        OrderStatus status,
        LocalDateTime createTime,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal amount
) {
}
```

转换代码如下：

```java
List<OrderItemView> views = StreamSampleData.orders()
        .stream()
        .flatMap(order -> CollUtil.emptyIfNull(order.items())
                .stream()
                .map(item -> {
                    BigDecimal price = ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO);
                    Integer quantity = ObjectUtil.defaultIfNull(item.quantity(), 0);
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));

                    return new OrderItemView(
                            order.id(),
                            order.customerNo(),
                            order.status(),
                            order.createTime(),
                            item.productName(),
                            quantity,
                            price,
                            amount
                    );
                }))
        .toList();
```

嵌套集合展开时要注意两点：

第一，子集合可能为空时，应使用 `CollUtil.emptyIfNull` 兜底。

```java
.flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
```

第二，如果展开后还需要父对象字段，应在 `flatMap` 内部完成父子对象组合，不要在展开后再反向查找父对象。

### 数据去重与排序

数据去重与排序经常一起出现，例如按业务字段去重后排序、获取薪资最高的前几名员工、按订单时间倒序展示商品明细等。

场景一：按员工对象整体去重。由于 `Employee` 是 `record`，会自动生成 `equals` 和 `hashCode`，可以直接使用 `distinct`。

```java
List<Employee> employees = Stream.concat(
                StreamSampleData.employees().stream(),
                StreamSampleData.employees().stream()
        )
        .distinct()
        .toList();
```

场景二：按员工 ID 去重，并保持原始顺序。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::id,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
        ))
        .values()
        .stream()
        .toList();
```

场景三：按部门 ID 去重，保留每个部门第一个员工。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::departmentId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
        ))
        .values()
        .stream()
        .toList();
```

场景四：按薪资倒序排序，获取前 3 名员工。

```java
List<Employee> top3Employees = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .limit(3)
        .toList();
```

场景五：先按部门升序，再按薪资倒序排序。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(
                Comparator.comparing(Employee::departmentId)
                        .thenComparing(Comparator.comparing(Employee::salary).reversed())
        )
        .toList();
```

场景六：按订单创建时间倒序展开明细。

```java
List<OrderItemView> views = StreamSampleData.orders()
        .stream()
        .sorted(Comparator.comparing(Order::createTime).reversed())
        .flatMap(order -> CollUtil.emptyIfNull(order.items())
                .stream()
                .map(item -> {
                    BigDecimal price = ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO);
                    Integer quantity = ObjectUtil.defaultIfNull(item.quantity(), 0);
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));

                    return new OrderItemView(
                            order.id(),
                            order.customerNo(),
                            order.status(),
                            order.createTime(),
                            item.productName(),
                            quantity,
                            price,
                            amount
                    );
                }))
        .toList();
```

如果排序字段可能为空，应明确指定空值排序规则。

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(
                Employee::salary,
                Comparator.nullsLast(BigDecimal::compareTo)
        ))
        .toList();
```

如果需要封装按字段去重方法，可以定义一个工具类。下面的方法适合串行 Stream 使用。

文件位置：`src/main/java/io/github/atengk/stream/util/StreamDistinctUtil.java`

下面的代码定义按字段去重工具类，用于在串行 Stream 中根据业务字段去重。

```java
package io.github.atengk.stream.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Stream 去重工具类
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class StreamDistinctUtil {

    private StreamDistinctUtil() {
    }

    /**
     * 根据指定字段去重，适用于串行 Stream。
     *
     * @param keyExtractor 去重字段提取函数
     * @param <T>          元素类型
     * @return 去重判断函数
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = new HashSet<>();
        return element -> seen.add(keyExtractor.apply(element));
    }
}
```

调用示例：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .filter(StreamDistinctUtil.distinctByKey(Employee::departmentId))
        .sorted(Comparator.comparing(Employee::departmentId))
        .toList();
```

如果是并行 Stream，不要使用基于 `HashSet` 的去重方法。并行场景应使用线程安全集合，或者优先改用 `toMap`、`groupingByConcurrent` 等收集方式。

去重与排序建议如下：

| 场景             | 推荐方式                                         |
| ---------------- | ------------------------------------------------ |
| 对象整体去重     | `distinct()`                                     |
| 按字段去重并保序 | `toMap` + `LinkedHashMap`                        |
| 简单字段排序     | `Comparator.comparing(...)`                      |
| 倒序排序         | `.reversed()`                                    |
| 多字段排序       | `.thenComparing(...)`                            |
| 空值字段排序     | `Comparator.nullsFirst` / `Comparator.nullsLast` |
| Top N            | `sorted(...).limit(n)`                           |

在实际业务中，去重、排序、截取的顺序会影响结果。通常建议先明确业务语义，再决定操作顺序。

```java
// 先去重，再排序：适合保留每个业务 key 的第一条数据后排序
List<Employee> employees1 = StreamSampleData.employees()
        .stream()
        .collect(Collectors.toMap(
                Employee::departmentId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
        ))
        .values()
        .stream()
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .toList();

// 先排序，再去重：适合每个业务 key 保留排序后的最优数据
List<Employee> employees2 = StreamSampleData.employees()
        .stream()
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .collect(Collectors.toMap(
                Employee::departmentId,
                Function.identity(),
                (first, second) -> first,
                LinkedHashMap::new
        ))
        .values()
        .stream()
        .toList();
```

第一种写法是“每个部门保留原始列表中的第一条员工，然后按薪资排序”。第二种写法是“先按薪资倒序排序，再每个部门保留薪资最高的员工”。两者结果可能不同，开发时必须根据业务规则选择。



## 性能与编码规范

本章用于说明 Stream 在实际项目中的编码边界。Stream 可以提升集合处理代码的表达能力，但不代表所有循环都应该改成 Stream。实际开发中应优先保证代码清晰、可维护、无副作用，再考虑链式写法和性能优化。

### 避免过度链式调用

Stream 的优势是链式表达，但链式调用过长会降低可读性。尤其是同时包含过滤、排序、分组、嵌套展开、金额计算、对象转换和空值处理时，一条 Stream 链可能会变得难以调试和维护。

不推荐将复杂逻辑全部压缩在一条链中：

```java
List<OrderItemView> views = StreamSampleData.orders()
        .stream()
        .filter(order -> OrderStatus.PAID.equals(order.status()) || OrderStatus.FINISHED.equals(order.status()))
        .sorted(Comparator.comparing(Order::createTime).reversed())
        .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream().filter(item -> StrUtil.isNotBlank(item.productName())).map(item -> new OrderItemView(order.id(), order.customerNo(), order.status(), order.createTime(), item.productName(), ObjectUtil.defaultIfNull(item.quantity(), 0), ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO), ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO).multiply(BigDecimal.valueOf(ObjectUtil.defaultIfNull(item.quantity(), 0))))))
        .toList();
```

这段代码虽然可以运行，但一旦出现空值、金额计算错误或字段映射错误，定位问题会比较困难。

更推荐将复杂逻辑拆分为局部变量和独立方法：

```java
List<Order> validOrders = StreamSampleData.orders()
        .stream()
        .filter(order -> OrderStatus.PAID.equals(order.status())
                || OrderStatus.FINISHED.equals(order.status()))
        .sorted(Comparator.comparing(Order::createTime).reversed())
        .toList();

List<OrderItemView> views = validOrders.stream()
        .flatMap(order -> buildOrderItemViews(order).stream())
        .toList();
```

文件位置：`src/main/java/io/github/atengk/stream/service/OrderViewService.java`

下面的代码将订单明细转换逻辑封装到独立方法中，避免主流程 Stream 链过长。

```java
package io.github.atengk.stream.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.data.StreamSampleData;
import io.github.atengk.stream.model.Order;
import io.github.atengk.stream.model.OrderItemView;
import io.github.atengk.stream.model.OrderStatus;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 订单展示服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class OrderViewService {

    /**
     * 查询有效订单明细展示列表。
     *
     * @return 订单明细展示列表
     */
    public List<OrderItemView> listValidOrderItemViews() {
        List<Order> validOrders = StreamSampleData.orders()
                .stream()
                .filter(this::isValidOrder)
                .sorted(Comparator.comparing(Order::createTime).reversed())
                .toList();

        return validOrders.stream()
                .flatMap(order -> buildOrderItemViews(order).stream())
                .toList();
    }

    /**
     * 判断订单是否为有效订单。
     *
     * @param order 订单
     * @return true 表示有效订单
     */
    private boolean isValidOrder(Order order) {
        return OrderStatus.PAID.equals(order.status())
                || OrderStatus.FINISHED.equals(order.status());
    }

    /**
     * 构建订单明细展示列表。
     *
     * @param order 订单
     * @return 订单明细展示列表
     */
    private List<OrderItemView> buildOrderItemViews(Order order) {
        return CollUtil.emptyIfNull(order.items())
                .stream()
                .filter(item -> StrUtil.isNotBlank(item.productName()))
                .map(item -> {
                    BigDecimal price = ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO);
                    Integer quantity = ObjectUtil.defaultIfNull(item.quantity(), 0);
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));

                    return new OrderItemView(
                            order.id(),
                            order.customerNo(),
                            order.status(),
                            order.createTime(),
                            item.productName(),
                            quantity,
                            price,
                            amount
                    );
                })
                .toList();
    }
}
```

过度链式调用的判断标准如下：

| 判断点                         | 建议                   |
| ------------------------------ | ---------------------- |
| 一条链超过 5 到 7 个操作       | 考虑拆分局部变量       |
| Lambda 中包含多行业务逻辑      | 考虑提取私有方法       |
| 同时处理过滤、计算、转换、分组 | 考虑分阶段处理         |
| 调试时很难定位哪一步出错       | 考虑拆分并增加变量命名 |
| Stream 中出现复杂空值兜底      | 考虑提前清洗数据       |

Stream 链应该表达清晰的数据处理流程，而不是为了减少代码行数而牺牲可读性。

### 避免副作用操作

Stream 的中间操作和终止操作应尽量保持无副作用。所谓副作用，是指 Stream 处理过程中修改外部变量、修改共享集合、写数据库、调用远程接口、写文件、改变对象状态等行为。

不推荐在 `forEach` 中修改外部集合：

```java
List<String> names = new ArrayList<>();

StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .forEach(employee -> names.add(employee.name()));
```

推荐使用 `map` 和 `toList` 生成新结果：

```java
List<String> names = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .map(Employee::name)
        .toList();
```

不推荐在 `map` 中修改对象状态：

```java
List<Employee> employees = StreamSampleData.employees()
        .stream()
        .map(employee -> {
            // 不推荐：map 中隐藏对象修改行为
            employee.name().trim();
            return employee;
        })
        .toList();
```

由于前面示例使用的是 `record`，对象本身不可变，可以天然减少这种问题。如果使用普通 Java Bean，也不建议在 Stream 链中调用 setter 修改对象，而是创建新的结果对象。

推荐将转换结果放入新的对象中：

```java
List<EmployeeOption> options = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .map(employee -> new EmployeeOption(employee.id(), employee.name()))
        .toList();
```

不推荐在 Stream 中执行数据库写入、远程调用或文件写入：

```java
StreamSampleData.employees()
        .stream()
        .forEach(employee -> {
            // 不推荐：隐藏 I/O 副作用，异常处理和事务边界都不清晰
            System.out.println("同步员工：" + employee.name());
        });
```

如果确实需要批量执行有副作用的业务操作，建议使用普通循环，让异常处理、日志、事务边界更明确。

```java
for (Employee employee : StreamSampleData.employees()) {
    if (!Boolean.TRUE.equals(employee.enabled())) {
        continue;
    }

    System.out.println("同步员工：" + employee.name());
}
```

副作用操作建议如下：

| 场景         | 建议                                      |
| ------------ | ----------------------------------------- |
| 收集结果     | 使用 `map`、`collect`、`toList`           |
| 累加数值     | 使用 `sum`、`reduce`、`summaryStatistics` |
| 修改外部集合 | 避免，改用收集器                          |
| 修改外部变量 | 避免，改用归约                            |
| 数据库写入   | 优先普通循环                              |
| 远程接口调用 | 优先普通循环，明确超时、重试和异常处理    |
| 日志调试     | 临时可用 `peek`，正式逻辑不依赖 `peek`    |

Stream 更适合描述数据转换，不适合隐藏复杂业务动作。

### 合理选择 Stream 与普通循环

Stream 和普通循环不是替代关系。Stream 更适合声明式数据处理，普通循环更适合复杂控制流程。实际开发中应根据业务逻辑选择更清晰的写法。

适合使用 Stream 的场景：

| 场景     | 示例                       |
| -------- | -------------------------- |
| 简单过滤 | 筛选启用员工               |
| 字段提取 | 员工列表转员工 ID 列表     |
| 对象转换 | Entity 转 VO               |
| 分组统计 | 按部门统计员工数量         |
| 聚合计算 | 求和、平均值、最大值       |
| 去重排序 | 按字段去重后排序           |
| 嵌套展开 | 订单列表展开为订单明细列表 |

示例：使用 Stream 进行字段提取更清晰。

```java
List<Long> employeeIds = StreamSampleData.employees()
        .stream()
        .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
        .map(Employee::id)
        .toList();
```

适合使用普通循环的场景：

| 场景                          | 原因                       |
| ----------------------------- | -------------------------- |
| 多层条件分支                  | 普通循环可读性更好         |
| 需要频繁 `break` / `continue` | 控制流程更直接             |
| 复杂异常处理                  | try-catch 边界更清晰       |
| 涉及外部资源调用              | 日志、重试、事务更容易控制 |
| 需要修改多个外部状态          | Stream 容易隐藏副作用      |
| 调试复杂流程                  | 普通循环断点更直观         |

示例：复杂业务校验更适合普通循环。

```java
List<String> invalidMessages = new ArrayList<>();

for (Employee employee : StreamSampleData.employees()) {
    if (employee.id() == null) {
        invalidMessages.add("员工 ID 不能为空");
        continue;
    }

    if (StrUtil.isBlank(employee.name())) {
        invalidMessages.add("员工名称不能为空：" + employee.id());
        continue;
    }

    if (employee.salary() == null || employee.salary().compareTo(BigDecimal.ZERO) <= 0) {
        invalidMessages.add("员工薪资必须大于 0：" + employee.id());
    }
}
```

如果强行使用 Stream，反而会让代码难以阅读：

```java
List<String> invalidMessages = StreamSampleData.employees()
        .stream()
        .map(employee -> {
            if (employee.id() == null) {
                return "员工 ID 不能为空";
            }
            if (StrUtil.isBlank(employee.name())) {
                return "员工名称不能为空：" + employee.id();
            }
            if (employee.salary() == null || employee.salary().compareTo(BigDecimal.ZERO) <= 0) {
                return "员工薪资必须大于 0：" + employee.id();
            }
            return null;
        })
        .filter(Objects::nonNull)
        .toList();
```

这类代码虽然减少了循环语句，但将复杂分支塞进了 `map`，可读性并没有提升。

选择建议如下：

| 目标                   | 推荐方式                      |
| ---------------------- | ----------------------------- |
| 数据筛选、转换、收集   | Stream                        |
| 简单统计聚合           | Stream                        |
| 多层业务校验           | 普通循环                      |
| 有明确中断流程         | 普通循环                      |
| 有数据库或远程调用     | 普通循环                      |
| 需要复杂日志和异常处理 | 普通循环                      |
| 逻辑非常短且线性       | Stream 或循环均可，优先可读性 |

不要把 Stream 当作“高级写法”。在团队协作中，更可维护的写法才是更好的写法。

### 并行 Stream 使用边界

并行 Stream 是有边界的性能优化手段，不是默认写法。它适合大数据量、CPU 密集、无共享状态、无顺序要求的数据处理场景。

适合使用并行 Stream 的示例：

```java
long result = LongStream.rangeClosed(1, 10_000_000)
        .parallel()
        .map(number -> number * number)
        .sum();
```

这种场景中，数据量较大、每个元素计算相互独立、没有共享变量，也没有 I/O 操作，更容易获得并行收益。

不适合使用并行 Stream 的示例：

```java
StreamSampleData.employees()
        .parallelStream()
        .forEach(employee -> {
            // 不推荐：并行 Stream 中执行外部 I/O 操作
            System.out.println("同步员工：" + employee.name());
        });
```

并行 Stream 的使用边界如下：

| 边界           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 数据量太小     | 线程调度成本可能高于收益                                     |
| 单元素处理太轻 | 简单字段提取不适合并行                                       |
| 有共享状态     | 容易出现线程安全问题                                         |
| 有顺序依赖     | 并行会削弱顺序保证                                           |
| 有 I/O 操作    | 容易造成连接池、线程池、文件资源竞争                         |
| 公共线程池竞争 | 默认使用 `ForkJoinPool.commonPool()`，可能影响同 JVM 其他任务 |

错误示例：并行写入普通集合。

```java
List<String> names = new ArrayList<>();

StreamSampleData.employees()
        .parallelStream()
        .forEach(employee -> names.add(employee.name()));
```

推荐写法：使用收集操作生成结果。

```java
List<String> names = StreamSampleData.employees()
        .parallelStream()
        .map(Employee::name)
        .toList();
```

错误示例：依赖并行 `forEach` 的输出顺序。

```java
StreamSampleData.employees()
        .parallelStream()
        .forEach(employee -> System.out.println(employee.name()));
```

如果必须保持顺序，优先使用普通 Stream：

```java
StreamSampleData.employees()
        .stream()
        .forEach(employee -> System.out.println(employee.name()));
```

并行 Stream 使用前建议先做压测或基准测试。不要只因为代码可以改成 `parallelStream()` 就认为性能一定会提升。

## 单元测试与验证

本章用于说明如何对 Stream 代码进行单元测试。Stream 代码通常不需要测试 Java API 本身，而是测试业务筛选条件、字段转换规则、分组统计结果、边界数据和空集合处理是否符合预期。

### 基础操作测试

基础操作测试主要验证过滤、字段提取、排序、截取和对象转换是否正确。测试时应关注结果数量、字段内容、顺序和不可变性。

文件位置：`src/test/java/io/github/atengk/stream/StreamBasicOperationTest.java`

下面的代码验证 Stream 的基础过滤、字段提取、排序和 `toList()` 不可变特性。

```java
package io.github.atengk.stream;

import io.github.atengk.stream.data.StreamSampleData;
import io.github.atengk.stream.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stream 基础操作测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class StreamBasicOperationTest {

    /**
     * 测试筛选启用员工。
     */
    @Test
    void testFilterEnabledEmployees() {
        List<Employee> employees = StreamSampleData.employees()
                .stream()
                .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
                .toList();

        assertEquals(6, employees.size());
        assertTrue(employees.stream().allMatch(employee -> Boolean.TRUE.equals(employee.enabled())));
    }

    /**
     * 测试提取员工姓名。
     */
    @Test
    void testMapEmployeeNames() {
        List<String> names = StreamSampleData.employees()
                .stream()
                .map(Employee::name)
                .toList();

        assertEquals(7, names.size());
        assertEquals("张三", names.getFirst());
        assertTrue(names.contains("周九"));
    }

    /**
     * 测试按薪资倒序排序。
     */
    @Test
    void testSortBySalaryDesc() {
        List<Employee> employees = StreamSampleData.employees()
                .stream()
                .sorted(Comparator.comparing(Employee::salary).reversed())
                .toList();

        assertEquals("周九", employees.getFirst().name());
        assertEquals(new BigDecimal("38000.00"), employees.getFirst().salary());
    }

    /**
     * 测试获取薪资最高的前 3 名员工。
     */
    @Test
    void testLimitTop3SalaryEmployees() {
        List<Employee> employees = StreamSampleData.employees()
                .stream()
                .sorted(Comparator.comparing(Employee::salary).reversed())
                .limit(3)
                .toList();

        assertEquals(3, employees.size());
        assertEquals("周九", employees.get(0).name());
        assertEquals("李四", employees.get(1).name());
        assertEquals("钱七", employees.get(2).name());
    }

    /**
     * 测试 Stream.toList 返回不可变列表。
     */
    @Test
    void testStreamToListIsUnmodifiable() {
        List<String> names = StreamSampleData.employees()
                .stream()
                .map(Employee::name)
                .toList();

        assertThrows(UnsupportedOperationException.class, () -> names.add("新员工"));
    }
}
```

执行测试命令：

```bash
mvn test -Dtest=StreamBasicOperationTest
```

`-Dtest=StreamBasicOperationTest` 表示只运行指定测试类，适合在编写某一章节示例时快速验证。

### 分组统计测试

分组统计测试主要验证 `groupingBy`、`partitioningBy`、`counting`、`reducing` 等收集逻辑是否正确。测试重点是分组 key 是否正确、每组数量是否正确、统计金额是否准确。

文件位置：`src/test/java/io/github/atengk/stream/StreamGroupStatisticsTest.java`

下面的代码验证按部门分组、按启用状态分区、薪资汇总和订单状态统计。

```java
package io.github.atengk.stream;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.stream.data.StreamSampleData;
import io.github.atengk.stream.model.Employee;
import io.github.atengk.stream.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stream 分组统计测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class StreamGroupStatisticsTest {

    /**
     * 测试按部门统计员工数量。
     */
    @Test
    void testGroupByDepartmentCount() {
        Map<Long, Long> countMap = StreamSampleData.employees()
                .stream()
                .collect(Collectors.groupingBy(
                        Employee::departmentId,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        assertEquals(4, countMap.size());
        assertEquals(3L, countMap.get(1L));
        assertEquals(2L, countMap.get(2L));
        assertEquals(1L, countMap.get(3L));
        assertEquals(1L, countMap.get(4L));
    }

    /**
     * 测试按启用状态分区。
     */
    @Test
    void testPartitionByEnabled() {
        Map<Boolean, List<Employee>> partitionMap = StreamSampleData.employees()
                .stream()
                .collect(Collectors.partitioningBy(employee -> Boolean.TRUE.equals(employee.enabled())));

        assertEquals(6, partitionMap.get(Boolean.TRUE).size());
        assertEquals(1, partitionMap.get(Boolean.FALSE).size());
        assertEquals("赵六", partitionMap.get(Boolean.FALSE).getFirst().name());
    }

    /**
     * 测试按部门统计薪资总额。
     */
    @Test
    void testGroupByDepartmentSalarySum() {
        Map<Long, BigDecimal> salaryMap = StreamSampleData.employees()
                .stream()
                .collect(Collectors.groupingBy(
                        Employee::departmentId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                employee -> ObjectUtil.defaultIfNull(employee.salary(), BigDecimal.ZERO),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        assertEquals(new BigDecimal("82000.00"), salaryMap.get(1L));
        assertEquals(new BigDecimal("36000.00"), salaryMap.get(2L));
        assertEquals(new BigDecimal("24000.00"), salaryMap.get(3L));
        assertEquals(new BigDecimal("17000.00"), salaryMap.get(4L));
    }

    /**
     * 测试按订单状态统计订单数量。
     */
    @Test
    void testGroupByOrderStatusCount() {
        Map<OrderStatus, Long> countMap = StreamSampleData.orders()
                .stream()
                .collect(Collectors.groupingBy(
                        order -> order.status(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        assertEquals(1L, countMap.get(OrderStatus.CREATED));
        assertEquals(1L, countMap.get(OrderStatus.PAID));
        assertEquals(1L, countMap.get(OrderStatus.FINISHED));
        assertEquals(1L, countMap.get(OrderStatus.CANCELED));
    }

    /**
     * 测试订单明细总金额。
     */
    @Test
    void testOrderItemTotalAmount() {
        BigDecimal totalAmount = StreamSampleData.orders()
                .stream()
                .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
                .map(item -> {
                    BigDecimal price = ObjectUtil.defaultIfNull(item.price(), BigDecimal.ZERO);
                    Integer quantity = ObjectUtil.defaultIfNull(item.quantity(), 0);
                    return price.multiply(BigDecimal.valueOf(quantity));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("2873.00"), totalAmount);
    }
}
```

执行测试命令：

```bash
mvn test -Dtest=StreamGroupStatisticsTest
```

分组统计测试建议使用明确的预期值，不要在断言中再次复制同一套 Stream 统计逻辑，否则测试容易变成“用同样的代码验证同样的代码”。

### 边界数据测试

边界数据测试用于验证空字段、重复 key、重复对象、空子集合、排序字段为空等情况。Stream 代码中的很多线上问题并不是 API 使用错误，而是边界数据没有处理好。

文件位置：`src/test/java/io/github/atengk/stream/StreamBoundaryDataTest.java`

下面的代码验证空字段过滤、重复 key 合并、按字段去重和嵌套集合空值兜底。

```java
package io.github.atengk.stream;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stream.data.StreamSampleData;
import io.github.atengk.stream.model.Employee;
import io.github.atengk.stream.model.Order;
import io.github.atengk.stream.model.OrderItem;
import io.github.atengk.stream.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stream 边界数据测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class StreamBoundaryDataTest {

    /**
     * 测试过滤空字符串字段。
     */
    @Test
    void testFilterBlankName() {
        List<Employee> employees = CollUtil.newArrayList(
                new Employee(1L, "张三", 1L, "研发工程师", 28, new BigDecimal("18000.00"), true, LocalDate.now()),
                new Employee(2L, " ", 1L, "研发工程师", 30, new BigDecimal("20000.00"), true, LocalDate.now()),
                new Employee(3L, null, 1L, "研发工程师", 32, new BigDecimal("22000.00"), true, LocalDate.now())
        );

        List<String> names = employees.stream()
                .map(Employee::name)
                .filter(StrUtil::isNotBlank)
                .toList();

        assertEquals(1, names.size());
        assertEquals("张三", names.getFirst());
    }

    /**
     * 测试 toMap 重复 key 时保留第一条数据。
     */
    @Test
    void testToMapDuplicateKeyKeepFirst() {
        List<Employee> employees = CollUtil.newArrayList(
                new Employee(1L, "张三", 1L, "研发工程师", 28, new BigDecimal("18000.00"), true, LocalDate.now()),
                new Employee(1L, "李四", 1L, "高级研发工程师", 34, new BigDecimal("26000.00"), true, LocalDate.now())
        );

        Map<Long, Employee> employeeMap = employees.stream()
                .collect(Collectors.toMap(
                        Employee::id,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        assertEquals(1, employeeMap.size());
        assertEquals("张三", employeeMap.get(1L).name());
    }

    /**
     * 测试按部门去重后按部门 ID 排序。
     */
    @Test
    void testDistinctByDepartmentAndSort() {
        List<Employee> employees = StreamSampleData.employees()
                .stream()
                .collect(Collectors.toMap(
                        Employee::departmentId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(Employee::departmentId))
                .toList();

        assertEquals(4, employees.size());
        assertEquals(1L, employees.get(0).departmentId());
        assertEquals(2L, employees.get(1).departmentId());
        assertEquals(3L, employees.get(2).departmentId());
        assertEquals(4L, employees.get(3).departmentId());
    }

    /**
     * 测试嵌套集合为空时 flatMap 不抛出异常。
     */
    @Test
    void testFlatMapWithNullItems() {
        List<Order> orders = CollUtil.newArrayList(
                new Order(1L, "C001", OrderStatus.PAID, LocalDateTime.now(), null),
                new Order(2L, "C002", OrderStatus.PAID, LocalDateTime.now(), CollUtil.newArrayList(
                        new OrderItem(1L, 2L, "机械键盘", 1, new BigDecimal("399.00"))
                ))
        );

        List<OrderItem> items = orders.stream()
                .flatMap(order -> CollUtil.emptyIfNull(order.items()).stream())
                .toList();

        assertEquals(1, items.size());
        assertEquals("机械键盘", items.getFirst().productName());
    }
}
```

执行测试命令：

```bash
mvn test -Dtest=StreamBoundaryDataTest
```

边界测试建议覆盖以下数据：

| 边界类型 | 示例                                |
| -------- | ----------------------------------- |
| 空字符串 | `""`、`" "`、`null`                 |
| 空集合   | `List.of()`                         |
| 空对象   | 集合中包含 `null` 元素              |
| 空字段   | `salary == null`、`enabled == null` |
| 重复 key | `toMap` 中 key 重复                 |
| 空子集合 | `order.items() == null`             |
| 极值数据 | 金额为 0、数量为 0、年龄为 0        |

边界数据越贴近真实业务数据，Stream 代码越不容易在生产环境中出现空指针、重复 key 或统计错误。

### 空集合测试

空集合测试用于验证 Stream 在没有数据时的返回结果是否符合业务预期。大多数 Stream 操作可以天然处理空集合，但 `findFirst`、`max`、`min`、`average`、`reduce`、`toMap` 等场景仍然需要确认默认值和异常策略。

文件位置：`src/test/java/io/github/atengk/stream/StreamEmptyCollectionTest.java`

下面的代码验证空集合下的过滤、统计、查找、分组和金额汇总行为。

```java
package io.github.atengk.stream;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.stream.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stream 空集合测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class StreamEmptyCollectionTest {

    /**
     * 测试空集合过滤结果仍为空集合。
     */
    @Test
    void testFilterEmptyList() {
        List<Employee> employees = List.of();

        List<Employee> result = employees.stream()
                .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
                .toList();

        assertTrue(result.isEmpty());
    }

    /**
     * 测试空集合 count 返回 0。
     */
    @Test
    void testCountEmptyList() {
        List<Employee> employees = List.of();

        long count = employees.stream()
                .filter(employee -> Boolean.TRUE.equals(employee.enabled()))
                .count();

        assertEquals(0L, count);
    }

    /**
     * 测试空集合 findFirst 返回 Optional.empty。
     */
    @Test
    void testFindFirstEmptyList() {
        List<Employee> employees = List.of();

        Optional<Employee> employeeOptional = employees.stream()
                .findFirst();

        assertTrue(employeeOptional.isEmpty());
    }

    /**
     * 测试空集合 max 返回 Optional.empty。
     */
    @Test
    void testMaxEmptyList() {
        List<Employee> employees = List.of();

        Optional<Employee> employeeOptional = employees.stream()
                .max(Comparator.comparing(Employee::salary));

        assertTrue(employeeOptional.isEmpty());
    }

    /**
     * 测试空集合 average 返回 OptionalDouble.empty。
     */
    @Test
    void testAverageEmptyList() {
        List<Employee> employees = List.of();

        OptionalDouble averageOptional = employees.stream()
                .mapToInt(Employee::age)
                .average();

        assertTrue(averageOptional.isEmpty());
        assertEquals(0D, averageOptional.orElse(0D));
    }

    /**
     * 测试空集合分组返回空 Map。
     */
    @Test
    void testGroupByEmptyList() {
        List<Employee> employees = List.of();

        Map<Long, List<Employee>> groupMap = employees.stream()
                .collect(Collectors.groupingBy(Employee::departmentId));

        assertTrue(groupMap.isEmpty());
    }

    /**
     * 测试空集合 BigDecimal 求和返回 0。
     */
    @Test
    void testBigDecimalSumEmptyList() {
        List<Employee> employees = List.of();

        BigDecimal totalSalary = employees.stream()
                .map(Employee::salary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(BigDecimal.ZERO, totalSalary);
    }

    /**
     * 测试 null 集合通过 Hutool 兜底为空集合。
     */
    @Test
    void testNullListWithHutoolEmptyIfNull() {
        List<Employee> employees = null;

        List<Long> ids = CollUtil.emptyIfNull(employees)
                .stream()
                .map(Employee::id)
                .toList();

        assertTrue(ids.isEmpty());
    }
}
```

执行所有测试：

```bash
mvn clean test
```

如果只执行 Stream 相关测试，可以使用通配符：

```bash
mvn test -Dtest=Stream*Test
```

空集合测试建议明确以下预期：

| 操作                            | 空集合结果               |
| ------------------------------- | ------------------------ |
| `filter(...).toList()`          | 空 List                  |
| `count()`                       | `0`                      |
| `findFirst()`                   | `Optional.empty()`       |
| `max()` / `min()`               | `Optional.empty()`       |
| `average()`                     | `OptionalDouble.empty()` |
| `groupingBy()`                  | 空 Map                   |
| `reduce(identity, accumulator)` | 返回 identity            |
| `toMap()`                       | 空 Map                   |
| `CollUtil.emptyIfNull(null)`    | 空集合                   |

完成这些测试后，可以基本覆盖 Stream 开发中的主要风险点：基础逻辑错误、分组统计错误、重复 key 问题、空字段问题、空集合问题和不可变列表误用问题。