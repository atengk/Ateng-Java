# Stream API

## Stream API 概述

Stream API 是 Java 8 引入的函数式数据处理 API，主要用于对集合、数组等数据源进行过滤、映射、排序、统计、分组和聚合等操作。

Stream 不是集合，也不是数据容器。它更像是一条数据处理流水线，数据从源头进入 Stream，经过多个中间操作处理，最后通过终止操作得到结果。

### Stream 的作用

Stream 的主要作用是简化集合数据处理逻辑，让代码从传统的“过程式遍历”转向“声明式处理”。

在实际开发中，Stream 常用于以下场景：

| 场景     | 说明                                 |
| -------- | ------------------------------------ |
| 数据过滤 | 根据条件筛选集合中的元素             |
| 数据转换 | 将对象转换成字段、DTO、VO 或其他结构 |
| 数据排序 | 根据字段或自定义规则排序             |
| 数据去重 | 对集合元素进行去重处理               |
| 数据统计 | 统计数量、最大值、最小值、求和等     |
| 数据分组 | 根据对象字段进行分组                 |
| 数据聚合 | 将多个元素合并成一个结果             |

传统写法需要手动创建结果集合，并通过循环和条件判断完成数据处理。

```java
List<String> names = Arrays.asList("Tom", "Jerry", "Jack", "Tony");
List<String> result = new ArrayList<>();

for (String name : names) {
    if (name.startsWith("T")) {
        result.add(name.toUpperCase());
    }
}

System.out.println(result);
```

使用 Stream 后，可以将过滤、转换和收集操作组织成一条清晰的调用链。

```java
List<String> names = Arrays.asList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.stream()
        .filter(name -> name.startsWith("T"))
        .map(String::toUpperCase)
        .toList();

System.out.println(result);
```

下面的示例演示如何在对象集合中筛选成年人，并提取有效用户名。

```java
package io.github.atengk.stream;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * Stream API 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StreamOverviewDemo {

    public static void main(String[] args) {
        List<User> users = CollUtil.newArrayList(
                new User(1L, "张三", 18),
                new User(2L, "李四", 22),
                new User(3L, "", 25),
                new User(4L, "王五", 16)
        );

        List<String> adultNames = users.stream()
                .filter(user -> user.age() >= 18)
                .map(User::name)
                .filter(StrUtil::isNotBlank)
                .toList();

        System.out.println(adultNames);
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public record User(Long id, String name, Integer age) {
    }
}
```

这段代码中，Stream 完成了三个步骤：先过滤年龄大于等于 18 的用户，再提取用户名，最后过滤空白用户名。相比传统循环，Stream 更适合表达连续的数据处理流程。

### Stream 与集合的区别

集合用于存储数据，Stream 用于处理数据。集合关注的是“数据本身”，Stream 关注的是“数据如何被计算”。

| 对比项         | 集合                       | Stream                       |
| -------------- | -------------------------- | ---------------------------- |
| 主要作用       | 存储和管理数据             | 处理和计算数据               |
| 是否保存元素   | 保存元素                   | 不保存元素                   |
| 是否可重复遍历 | 可以重复遍历               | 通常只能消费一次             |
| 操作方式       | 外部迭代，由开发者控制循环 | 内部迭代，由 Stream 控制遍历 |
| 执行时机       | 调用方法后通常立即执行     | 中间操作不会立即执行         |
| 是否修改原数据 | 可以直接修改集合           | 默认不会修改原集合           |
| 关注重点       | 数据结构                   | 数据处理流程                 |

集合可以理解为数据容器，Stream 可以理解为基于数据容器创建出来的数据处理管道。

下面的代码中，`names` 是原始集合，`result` 是 Stream 处理后的新结果。

```java
List<String> names = new ArrayList<>(List.of("Tom", "Jerry", "Jack"));

List<String> result = names.stream()
        .filter(name -> name.length() > 3)
        .toList();

System.out.println(names);
System.out.println(result);
```

输出结果如下：

```text
[Tom, Jerry, Jack]
[Jerry, Jack]
```

可以看到，Stream 的过滤操作没有修改原集合，而是生成了一个新的处理结果。

需要注意的是，Stream 不能像集合一样重复使用。一个 Stream 执行过终止操作后，就不能再次执行其他操作。

```java
Stream<String> stream = Stream.of("Tom", "Jerry", "Jack");

long count = stream.count();

// 错误示例：同一个 Stream 已经被消费，不能再次使用
// List<String> result = stream.toList();
```

如果需要对同一批数据进行多次处理，应当从原集合重新创建 Stream。

```java
List<String> names = List.of("Tom", "Jerry", "Jack");

long count = names.stream().count();

List<String> result = names.stream()
        .filter(name -> name.length() > 3)
        .toList();
```

### Stream 的执行特点

Stream 的执行特点主要包括链式调用、惰性执行、一次消费、不修改原数据和支持并行处理。

#### 1. 支持链式调用

Stream 的多个操作可以连续调用，形成一条数据处理流水线。

```java
List<String> result = List.of("Tom", "Jerry", "Jack", "Tony").stream()
        .filter(name -> name.startsWith("T"))
        .map(String::toUpperCase)
        .sorted()
        .toList();

System.out.println(result);
```

这段代码的处理流程是：先筛选以 `T` 开头的字符串，再转换为大写，然后排序，最后收集为 List。

#### 2. 中间操作惰性执行

Stream 的中间操作不会立即执行，例如 `filter`、`map`、`sorted`、`limit` 等。只有调用终止操作时，整条流水线才会真正执行。

```java
List<String> names = List.of("Tom", "Jerry", "Jack");

Stream<String> stream = names.stream()
        .filter(name -> {
            System.out.println("执行过滤：" + name);
            return name.length() > 3;
        });

// 调用终止操作后，filter 才会真正执行
long count = stream.count();

System.out.println(count);
```

如果没有执行 `count()`、`toList()`、`forEach()` 这类终止操作，前面的 `filter` 不会真正运行。

#### 3. Stream 只能消费一次

Stream 执行终止操作后就会被关闭，不能再次使用。

```java
Stream<String> stream = Stream.of("Tom", "Jerry", "Jack");

long count = stream.count();

// 再次使用会抛出 IllegalStateException
// stream.forEach(System.out::println);
```

正确做法是从数据源重新创建新的 Stream。

```java
List<String> names = List.of("Tom", "Jerry", "Jack");

long count = names.stream().count();

names.stream()
        .filter(name -> name.startsWith("J"))
        .forEach(System.out::println);
```

#### 4. 默认不会修改原集合

Stream 的大多数操作会返回新的结果，不会直接改变原始集合。

```java
List<String> names = new ArrayList<>(List.of("Tom", "Jerry", "Jack"));

List<String> result = names.stream()
        .map(String::toUpperCase)
        .toList();

System.out.println(names);
System.out.println(result);
```

输出结果如下：

```text
[Tom, Jerry, Jack]
[TOM, JERRY, JACK]
```

如果需要修改原集合，应使用集合本身提供的方法，例如 `replaceAll`、`removeIf` 等，而不是依赖 Stream。

#### 5. 支持短路操作

部分 Stream 操作支持短路执行。短路操作在满足条件后可以提前结束遍历，不一定会处理所有元素。

常见短路操作包括：

| 方法          | 说明                            |
| ------------- | ------------------------------- |
| `findFirst()` | 获取第一个元素                  |
| `findAny()`   | 获取任意一个元素                |
| `anyMatch()`  | 任意元素满足条件即返回 true     |
| `allMatch()`  | 所有元素都满足条件才返回 true   |
| `noneMatch()` | 所有元素都不满足条件才返回 true |
| `limit()`     | 限制处理的数据数量              |

示例：

```java
boolean exists = List.of("Tom", "Jerry", "Jack").stream()
        .anyMatch(name -> name.startsWith("J"));

System.out.println(exists);
```

当 Stream 找到第一个以 `J` 开头的元素后，`anyMatch` 就可以提前返回结果。

#### 6. 支持并行处理

Stream 可以通过 `parallelStream()` 或 `stream().parallel()` 创建并行流，由多个线程共同处理数据。

```java
List<String> result = List.of("Tom", "Jerry", "Jack", "Tony").parallelStream()
        .filter(name -> name.length() > 3)
        .map(String::toUpperCase)
        .toList();

System.out.println(result);
```

并行流适合数据量较大、每个元素处理逻辑相互独立、没有共享状态的场景。对于数据量较小或处理逻辑很简单的场景，并行流不一定更快，因为线程调度和任务拆分本身也有额外成本。

Stream 的核心执行特点可以总结为：中间操作不立即执行，终止操作触发计算；Stream 通常只能使用一次；默认不会修改原始集合；在适合的场景下可以使用并行流提升处理效率。



## Stream 创建

Stream 必须基于数据源创建，常见数据源包括集合、数组、静态方法、基本类型 Stream 等。创建 Stream 之后，通常会继续拼接中间操作和终止操作，形成完整的数据处理链路。

需要注意，创建 Stream 本身不会立即遍历数据。只有执行 `collect`、`forEach`、`count`、`reduce` 等终止操作时，Stream 才会真正开始执行。

### 集合创建 Stream

集合是创建 Stream 最常见的数据源。`Collection` 接口从 Java 8 开始提供了 `stream()` 和 `parallelStream()` 方法，因此 `List`、`Set` 等集合都可以直接创建 Stream。

普通集合创建 Stream，并过滤空白字符串。

```java
List<String> names = CollUtil.newArrayList("Tom", "", "Jerry", "Jack", " ");

List<String> result = names.stream()
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(result);
```

这里的 `names.stream()` 会基于集合创建一个顺序流，`filter(StrUtil::isNotBlank)` 用于过滤空白字符串，最后通过 `collect(Collectors.toList())` 收集为新的 List。

`Set` 也可以直接创建 Stream，常用于去重后的数据处理。

```java
Set<String> citySet = CollUtil.newHashSet("北京", "上海", "广州", "北京");

List<String> result = citySet.stream()
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(result);
```

如果需要并行处理，可以使用 `parallelStream()`。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.parallelStream()
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());

System.out.println(result);
```

并行流不是默认选择。只有在数据量较大、处理逻辑独立、没有共享状态时，才更适合使用 `parallelStream()`。

### 数组创建 Stream

数组可以通过 `Arrays.stream()` 创建 Stream。对象数组会创建普通 `Stream<T>`，基本类型数组会创建对应的基本类型 Stream，例如 `IntStream`、`LongStream`、`DoubleStream`。

字符串数组创建 Stream，并筛选长度大于 3 的元素。

```java
String[] names = {"Tom", "Jerry", "Jack", "Tony"};

List<String> result = Arrays.stream(names)
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());

System.out.println(result);
```

整型数组创建 `IntStream`，并进行统计计算。

```java
int[] numbers = {1, 2, 3, 4, 5};

int sum = Arrays.stream(numbers)
        .filter(num -> num > 2)
        .sum();

System.out.println(sum);
```

如果只想处理数组中的一部分元素，可以使用 `Arrays.stream(array, startInclusive, endExclusive)`。

```java
String[] names = {"Tom", "Jerry", "Jack", "Tony"};

List<String> result = Arrays.stream(names, 1, 3)
        .collect(Collectors.toList());

System.out.println(result);
```

上面的代码只会处理下标 `[1, 3)` 范围内的数据，也就是 `Jerry` 和 `Jack`。

### Stream 静态方法创建

`Stream` 接口提供了一些静态方法，可以在没有集合或数组的情况下直接创建 Stream。常用方法包括 `Stream.of()`、`Stream.empty()`、`Stream.generate()`、`Stream.iterate()`。

使用 `Stream.of()` 创建固定元素 Stream。

```java
List<String> result = Stream.of("Tom", "Jerry", "Jack")
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());

System.out.println(result);
```

使用 `Stream.empty()` 创建空 Stream，常用于避免返回 `null`。

```java
Stream<String> stream = Stream.empty();

List<String> result = stream.collect(Collectors.toList());

System.out.println(result);
```

在实际开发中，如果一个方法没有查询到数据，返回空 Stream 或空集合通常比返回 `null` 更安全。

使用 `Stream.generate()` 创建无限流，通常需要配合 `limit()` 限制数量。

```java
List<String> result = Stream.generate(() -> "默认值")
        .limit(3)
        .collect(Collectors.toList());

System.out.println(result);
```

`Stream.generate()` 会不断生成元素，如果不使用 `limit()`，后续终止操作可能一直执行下去。

使用 `Stream.iterate()` 创建有规律的无限流，也需要配合 `limit()` 使用。

```java
List<Integer> result = Stream.iterate(1, num -> num + 2)
        .limit(5)
        .collect(Collectors.toList());

System.out.println(result);
```

输出结果如下：

```text
[1, 3, 5, 7, 9]
```

`Stream.iterate()` 适合生成递增序列、分页页码、测试数据等有规律的数据。

### 基本类型 Stream

Java 提供了专门处理基本类型的 Stream，主要包括 `IntStream`、`LongStream` 和 `DoubleStream`。它们可以避免基本类型和包装类型之间频繁装箱、拆箱，适合处理数字计算。

创建整数范围 Stream。

```java
int sum = IntStream.range(1, 5)
        .sum();

System.out.println(sum);
```

`IntStream.range(1, 5)` 生成的是 `[1, 5)`，也就是 `1、2、3、4`。

如果需要包含结束值，可以使用 `rangeClosed()`。

```java
int sum = IntStream.rangeClosed(1, 5)
        .sum();

System.out.println(sum);
```

`IntStream.rangeClosed(1, 5)` 生成的是 `[1, 5]`，也就是 `1、2、3、4、5`。

基本类型 Stream 常用统计方法如下：

| 方法                  | 说明             |
| --------------------- | ---------------- |
| `sum()`               | 求和             |
| `average()`           | 求平均值         |
| `max()`               | 获取最大值       |
| `min()`               | 获取最小值       |
| `count()`             | 统计数量         |
| `summaryStatistics()` | 获取综合统计结果 |

使用 `summaryStatistics()` 获取综合统计信息。

```java
IntSummaryStatistics statistics = IntStream.of(10, 20, 30, 40)
        .summaryStatistics();

System.out.println("数量：" + statistics.getCount());
System.out.println("总和：" + statistics.getSum());
System.out.println("最大值：" + statistics.getMax());
System.out.println("最小值：" + statistics.getMin());
System.out.println("平均值：" + statistics.getAverage());
```

普通 Stream 和基本类型 Stream 可以互相转换。

```java
List<String> result = IntStream.rangeClosed(1, 5)
        .boxed()
        .map(num -> "编号：" + num)
        .collect(Collectors.toList());

System.out.println(result);
```

`boxed()` 可以将 `IntStream` 转换为 `Stream<Integer>`，这样就可以继续使用普通 Stream 的对象处理能力。

## 中间操作

中间操作用于对 Stream 中的数据进行处理，例如过滤、转换、排序、去重、截取等。中间操作的返回值仍然是 Stream，因此可以继续链式调用。

中间操作具有惰性执行特点。单独调用 `filter`、`map`、`sorted` 等方法不会立即执行，只有遇到终止操作时，整条 Stream 流水线才会真正运行。

### filter 过滤

`filter` 用于根据条件筛选元素。它接收一个 `Predicate` 函数式接口，返回 `true` 的元素会被保留，返回 `false` 的元素会被过滤掉。

过滤非空字符串。

```java
List<String> names = CollUtil.newArrayList("Tom", "", "Jerry", " ", "Jack");

List<String> result = names.stream()
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(result);
```

过滤数字集合中的偶数。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5, 6);

List<Integer> result = numbers.stream()
        .filter(num -> num % 2 == 0)
        .collect(Collectors.toList());

System.out.println(result);
```

多个过滤条件可以连续调用，也可以写在同一个 `filter` 中。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony", "");

List<String> result = names.stream()
        .filter(StrUtil::isNotBlank)
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());

System.out.println(result);
```

连续调用多个 `filter` 时，每个条件的职责更清晰；写在一个 `filter` 中时，代码更集中。实际开发中可以根据复杂度选择。

### map 映射

`map` 用于将 Stream 中的每个元素转换成另一个元素。它接收一个 `Function` 函数式接口，常用于字段提取、类型转换、对象转换等场景。

将字符串转换为大写。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

List<String> result = names.stream()
        .map(String::toUpperCase)
        .collect(Collectors.toList());

System.out.println(result);
```

将字符串转换为字符串长度。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

List<Integer> result = names.stream()
        .map(String::length)
        .collect(Collectors.toList());

System.out.println(result);
```

在对象集合中，`map` 最常见的用途是提取字段。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 22)
);

List<String> names = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(names);
```

`map` 的重点是“一对一转换”：一个输入元素，转换成一个输出元素。转换前后元素类型可以相同，也可以不同。

### flatMap 扁平化

`flatMap` 用于将多个 Stream 合并成一个 Stream，常用于处理嵌套集合。它适合“一对多转换后再合并”的场景。

例如，一个集合中包含多个 List，现在需要把这些 List 中的元素合并成一个普通 List。

```java
List<List<String>> nestedList = CollUtil.newArrayList(
        CollUtil.newArrayList("Java", "Spring"),
        CollUtil.newArrayList("MySQL", "Redis"),
        CollUtil.newArrayList("Docker", "Linux")
);

List<String> result = nestedList.stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

System.out.println(result);
```

输出结果如下：

```text
[Java, Spring, MySQL, Redis, Docker, Linux]
```

`map` 和 `flatMap` 的区别在于，`map` 不会自动展开内部集合，而 `flatMap` 会将内部 Stream 展开并合并。

使用 `map` 的结果仍然是嵌套结构。

```java
List<List<String>> nestedList = CollUtil.newArrayList(
        CollUtil.newArrayList("Java", "Spring"),
        CollUtil.newArrayList("MySQL", "Redis")
);

List<Stream<String>> result = nestedList.stream()
        .map(Collection::stream)
        .collect(Collectors.toList());

System.out.println(result);
```

使用 `flatMap` 的结果是扁平结构。

```java
List<List<String>> nestedList = CollUtil.newArrayList(
        CollUtil.newArrayList("Java", "Spring"),
        CollUtil.newArrayList("MySQL", "Redis")
);

List<String> result = nestedList.stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

System.out.println(result);
```

再看一个字符串拆分场景。将多个字符串按逗号拆分，并合并成一个标签集合。

```java
List<String> tagTexts = CollUtil.newArrayList("Java,Spring", "MySQL,Redis", "Docker,Linux");

List<String> tags = tagTexts.stream()
        .flatMap(text -> Arrays.stream(text.split(",")))
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(tags);
```

`flatMap` 的核心作用可以概括为：先把每个元素转换成一个 Stream，再把多个 Stream 合并成一个 Stream。

### distinct 去重

`distinct` 用于去除 Stream 中重复的元素。它依赖元素的 `equals()` 和 `hashCode()` 方法判断是否重复。

字符串去重。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Tom", "Jack", "Jerry");

List<String> result = names.stream()
        .distinct()
        .collect(Collectors.toList());

System.out.println(result);
```

输出结果如下：

```text
[Tom, Jerry, Jack]
```

对于字符串、包装类型等常见类型，`distinct` 可以直接使用。对于自定义对象，如果没有正确重写 `equals()` 和 `hashCode()`，`distinct` 可能无法按照业务字段去重。

如果需要根据对象的某个字段去重，可以借助 Map 实现。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(1L, "张三", 18)
);

List<User> result = users.stream()
        .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                        User::getId,
                        user -> user,
                        (oldValue, newValue) -> oldValue
                ),
                map -> new ArrayList<>(map.values())
        ));

System.out.println(result);
```

上面的代码根据用户 ID 去重。如果遇到相同 ID 的用户，保留第一个对象。

如果需要保持原集合顺序，可以使用 `LinkedHashMap`。

```java
List<User> result = users.stream()
        .collect(Collectors.collectingAndThen(
                Collectors.toMap(
                        User::getId,
                        user -> user,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ),
                map -> new ArrayList<>(map.values())
        ));
```

### sorted 排序

`sorted` 用于对 Stream 中的元素排序。无参 `sorted()` 使用自然排序，有参 `sorted(Comparator)` 使用自定义排序规则。

字符串自然排序。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.stream()
        .sorted()
        .collect(Collectors.toList());

System.out.println(result);
```

数字从小到大排序。

```java
List<Integer> numbers = CollUtil.newArrayList(5, 2, 4, 1, 3);

List<Integer> result = numbers.stream()
        .sorted()
        .collect(Collectors.toList());

System.out.println(result);
```

对象集合通常使用 `Comparator.comparing()` 按字段排序。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 22),
        new User(3L, "王五", 20)
);

List<User> result = users.stream()
        .sorted(Comparator.comparing(User::getAge))
        .collect(Collectors.toList());

System.out.println(result);
```

如果需要倒序，可以使用 `reversed()`。

```java
List<User> result = users.stream()
        .sorted(Comparator.comparing(User::getAge).reversed())
        .collect(Collectors.toList());

System.out.println(result);
```

如果需要多级排序，可以使用 `thenComparing()`。

```java
List<User> result = users.stream()
        .sorted(
                Comparator.comparing(User::getAge)
                        .thenComparing(User::getId)
        )
        .collect(Collectors.toList());

System.out.println(result);
```

这段代码会先按年龄升序排序；年龄相同时，再按用户 ID 升序排序。

排序时需要注意空值问题。如果排序字段可能为 `null`，应使用 `Comparator.nullsFirst()` 或 `Comparator.nullsLast()`。

```java
List<User> result = users.stream()
        .sorted(
                Comparator.comparing(
                        User::getAge,
                        Comparator.nullsLast(Integer::compareTo)
                )
        )
        .collect(Collectors.toList());

System.out.println(result);
```

### limit 与 skip

`limit` 和 `skip` 常用于截取 Stream 中的一部分数据。

`limit(n)` 表示只保留前 `n` 个元素。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.stream()
        .limit(2)
        .collect(Collectors.toList());

System.out.println(result);
```

输出结果如下：

```text
[Tom, Jerry]
```

`skip(n)` 表示跳过前 `n` 个元素。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.stream()
        .skip(2)
        .collect(Collectors.toList());

System.out.println(result);
```

输出结果如下：

```text
[Jack, Tony]
```

`skip` 和 `limit` 经常组合使用，实现简单分页。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony", "Lucy", "Lily");

int pageNum = 2;
int pageSize = 2;

List<String> result = names.stream()
        .skip((long) (pageNum - 1) * pageSize)
        .limit(pageSize)
        .collect(Collectors.toList());

System.out.println(result);
```

输出结果如下：

```text
[Jack, Tony]
```

这种分页方式适合内存中少量数据的处理。如果数据来自数据库，不建议先查询全部数据再使用 Stream 分页，而应该优先使用数据库分页，例如 MyBatis-Plus 的 `Page`、SQL 的 `limit`，或者其他持久层分页能力。

`limit` 和 `skip` 都是中间操作，只有后面调用终止操作时才会真正执行。



## 终止操作

终止操作用于触发 Stream 流水线真正执行，并产生最终结果。终止操作执行完成后，当前 Stream 就被消费，不能再次使用。

常见终止操作包括遍历、收集、统计、归约、查找、匹配等。和中间操作不同，终止操作不会再返回 Stream，而是返回具体结果，例如集合、数量、布尔值、Optional 或普通对象。

### forEach 遍历

`forEach` 用于遍历 Stream 中的每个元素，通常适合打印日志、输出结果、调用外部方法等场景。

遍历字符串集合。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

names.stream()
        .filter(StrUtil::isNotBlank)
        .forEach(System.out::println);
```

`forEach` 是终止操作，执行到它时，前面的 `filter` 才会真正执行。

在业务代码中，`forEach` 常用于对每个元素执行一个动作。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

names.stream()
        .map(String::toUpperCase)
        .forEach(name -> System.out.println("处理用户名称：" + name));
```

需要注意，`forEach` 更适合执行“动作”，不适合生成新的集合。如果需要得到处理后的结果，应优先使用 `collect`。

不推荐在 `forEach` 中手动向外部集合添加数据。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");
List<String> result = CollUtil.newArrayList();

names.stream()
        .filter(name -> name.length() > 3)
        .forEach(result::add);
```

更推荐使用 `collect` 收集结果。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

List<String> result = names.stream()
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());
```

如果使用并行流，`forEach` 不保证元素处理顺序。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

names.parallelStream()
        .forEach(System.out::println);
```

如果需要在并行流中保持顺序，可以使用 `forEachOrdered`。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

names.parallelStream()
        .forEachOrdered(System.out::println);
```

### collect 收集

`collect` 用于将 Stream 中的元素收集成集合、Map、字符串或分组结果，是实际开发中使用频率最高的终止操作之一。

收集为 List。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.stream()
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());

System.out.println(result);
```

收集为 Set。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Tom", "Jack");

Set<String> result = names.stream()
        .collect(Collectors.toSet());

System.out.println(result);
```

收集为 Map。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 22)
);

Map<Long, String> userNameMap = users.stream()
        .collect(Collectors.toMap(
                User::getId,
                User::getName
        ));

System.out.println(userNameMap);
```

`collect` 的核心作用是把 Stream 的处理结果转换成最终数据结构。后续“收集器”部分会继续展开常见的 `Collectors` 用法。

### count 统计

`count` 用于统计 Stream 中元素的数量，返回值类型是 `long`。

统计集合元素数量。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

long count = names.stream()
        .count();

System.out.println(count);
```

统计满足条件的元素数量。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

long count = names.stream()
        .filter(name -> name.startsWith("T"))
        .count();

System.out.println(count);
```

在对象集合中，`count` 常用于统计符合业务条件的数据量。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 16)
);

long adultCount = users.stream()
        .filter(user -> user.getAge() >= 18)
        .count();

System.out.println(adultCount);
```

如果只是判断是否存在某类数据，通常不需要先 `count` 再比较，可以直接使用 `anyMatch`。

```java
boolean exists = users.stream()
        .anyMatch(user -> user.getAge() >= 18);
```

### reduce 归约

`reduce` 用于将 Stream 中的多个元素合并成一个结果。它常用于求和、累乘、字符串拼接、对象聚合等场景。

对数字求和。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5);

Integer sum = numbers.stream()
        .reduce(0, Integer::sum);

System.out.println(sum);
```

这里的 `0` 是初始值，`Integer::sum` 表示每次将当前累计值和下一个元素相加。

也可以写成 Lambda 表达式。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5);

Integer sum = numbers.stream()
        .reduce(0, (total, num) -> total + num);

System.out.println(sum);
```

字符串拼接。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

String result = names.stream()
        .reduce("", (text, name) -> text + name);

System.out.println(result);
```

不过，字符串拼接通常更推荐使用 `Collectors.joining()`，可读性更好，也更适合指定分隔符。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

String result = names.stream()
        .collect(Collectors.joining(","));

System.out.println(result);
```

`reduce` 也可以不指定初始值，此时返回 `Optional<T>`，因为 Stream 可能为空。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5);

Optional<Integer> sumOptional = numbers.stream()
        .reduce(Integer::sum);

sumOptional.ifPresent(System.out::println);
```

如果集合可能为空，应使用 Optional 的安全处理方式。

```java
List<Integer> numbers = CollUtil.newArrayList();

Integer sum = numbers.stream()
        .reduce(Integer::sum)
        .orElse(0);

System.out.println(sum);
```

### min 与 max

`min` 和 `max` 用于获取 Stream 中的最小值和最大值。它们都需要传入比较器，返回值都是 `Optional<T>`。

获取数字最小值和最大值。

```java
List<Integer> numbers = CollUtil.newArrayList(5, 2, 8, 1, 6);

Optional<Integer> min = numbers.stream()
        .min(Integer::compareTo);

Optional<Integer> max = numbers.stream()
        .max(Integer::compareTo);

System.out.println("最小值：" + min.orElse(0));
System.out.println("最大值：" + max.orElse(0));
```

在对象集合中，通常按某个字段比较。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 16)
);

Optional<User> minAgeUser = users.stream()
        .min(Comparator.comparing(User::getAge));

Optional<User> maxAgeUser = users.stream()
        .max(Comparator.comparing(User::getAge));

minAgeUser.ifPresent(user -> System.out.println("年龄最小用户：" + user.getName()));
maxAgeUser.ifPresent(user -> System.out.println("年龄最大用户：" + user.getName()));
```

如果集合为空，`min` 和 `max` 会返回 `Optional.empty()`，不会直接返回 `null`。

```java
List<User> users = CollUtil.newArrayList();

User user = users.stream()
        .max(Comparator.comparing(User::getAge))
        .orElse(null);

System.out.println(user);
```

如果业务上不允许结果为空，可以使用 `orElseThrow` 抛出异常。

```java
User user = users.stream()
        .max(Comparator.comparing(User::getAge))
        .orElseThrow(() -> new IllegalArgumentException("未找到用户数据"));
```

### anyMatch 与 allMatch

`anyMatch` 和 `allMatch` 用于判断 Stream 中的元素是否满足指定条件，返回值都是 `boolean`。

`anyMatch` 表示只要有任意一个元素满足条件，就返回 `true`。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

boolean exists = names.stream()
        .anyMatch(name -> name.startsWith("J"));

System.out.println(exists);
```

`allMatch` 表示所有元素都满足条件，才返回 `true`。

```java
List<Integer> ages = CollUtil.newArrayList(18, 20, 22);

boolean allAdult = ages.stream()
        .allMatch(age -> age >= 18);

System.out.println(allAdult);
```

在对象集合中，常用于业务条件判断。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 16)
);

boolean hasAdult = users.stream()
        .anyMatch(user -> user.getAge() >= 18);

boolean allAdult = users.stream()
        .allMatch(user -> user.getAge() >= 18);

System.out.println("是否存在成年人：" + hasAdult);
System.out.println("是否全部成年：" + allAdult);
```

`anyMatch` 和 `allMatch` 都属于短路操作。`anyMatch` 找到第一个满足条件的元素后就可以结束；`allMatch` 找到第一个不满足条件的元素后就可以结束。

补充一个常见的匹配方法：`noneMatch`。它表示所有元素都不满足条件时返回 `true`。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20)
);

boolean noneChild = users.stream()
        .noneMatch(user -> user.getAge() < 18);

System.out.println(noneChild);
```

## 收集器

收集器是 `collect` 终止操作中使用的工具，主要由 `Collectors` 工具类提供。它可以把 Stream 的元素收集成 List、Set、Map，也可以完成分组、拼接、统计等操作。

在 Java 8 中，常见收集器都来自 `java.util.stream.Collectors`。

### toList

`Collectors.toList()` 用于将 Stream 中的元素收集为 List，是最常用的收集器。

筛选字符串并收集为 List。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.stream()
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());

System.out.println(result);
```

对象字段提取后收集为 List。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 22)
);

List<String> names = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(names);
```

`Collectors.toList()` 不保证返回的具体 List 类型。如果业务要求明确返回 `ArrayList`，可以使用 `Collectors.toCollection()`。

```java
ArrayList<String> names = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toCollection(ArrayList::new));
```

### toSet

`Collectors.toSet()` 用于将 Stream 中的元素收集为 Set，可以去除重复元素。

字符串集合收集为 Set。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Tom", "Jack");

Set<String> result = names.stream()
        .collect(Collectors.toSet());

System.out.println(result);
```

提取对象字段并去重。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "张三", 22)
);

Set<String> nameSet = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toSet());

System.out.println(nameSet);
```

`Collectors.toSet()` 不保证元素顺序。如果需要保持插入顺序，可以使用 `LinkedHashSet`。

```java
Set<String> nameSet = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toCollection(LinkedHashSet::new));
```

### toMap

`Collectors.toMap()` 用于将 Stream 中的元素收集为 Map。它通常需要指定 key 和 value 的生成规则。

根据用户 ID 构建用户 Map。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 22)
);

Map<Long, User> userMap = users.stream()
        .collect(Collectors.toMap(
                User::getId,
                user -> user
        ));

System.out.println(userMap);
```

根据用户 ID 构建用户名 Map。

```java
Map<Long, String> userNameMap = users.stream()
        .collect(Collectors.toMap(
                User::getId,
                User::getName
        ));

System.out.println(userNameMap);
```

使用 `toMap` 时，必须注意 key 重复问题。如果 key 重复且没有指定合并规则，会抛出 `IllegalStateException`。

错误示例：

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(1L, "张三-重复", 20)
);

Map<Long, User> userMap = users.stream()
        .collect(Collectors.toMap(
                User::getId,
                user -> user
        ));
```

正确做法是指定重复 key 的合并规则。

```java
Map<Long, User> userMap = users.stream()
        .collect(Collectors.toMap(
                User::getId,
                user -> user,
                (oldValue, newValue) -> oldValue
        ));

System.out.println(userMap);
```

上面的写法表示如果 key 重复，保留旧值。

如果需要保留新值，可以这样写。

```java
Map<Long, User> userMap = users.stream()
        .collect(Collectors.toMap(
                User::getId,
                user -> user,
                (oldValue, newValue) -> newValue
        ));

System.out.println(userMap);
```

如果需要保持 Map 的插入顺序，可以指定 `LinkedHashMap`。

```java
Map<Long, User> userMap = users.stream()
        .collect(Collectors.toMap(
                User::getId,
                user -> user,
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new
        ));

System.out.println(userMap);
```

### groupingBy 分组

`Collectors.groupingBy()` 用于根据指定规则对元素进行分组，返回结果通常是 `Map<K, List<T>>`。

根据年龄分组。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 18),
        new User(4L, "赵六", 20)
);

Map<Integer, List<User>> groupMap = users.stream()
        .collect(Collectors.groupingBy(User::getAge));

System.out.println(groupMap);
```

根据是否成年分组。

```java
Map<Boolean, List<User>> groupMap = users.stream()
        .collect(Collectors.groupingBy(user -> user.getAge() >= 18));

System.out.println(groupMap);
```

分组后统计数量。

```java
Map<Integer, Long> countMap = users.stream()
        .collect(Collectors.groupingBy(
                User::getAge,
                Collectors.counting()
        ));

System.out.println(countMap);
```

分组后提取用户名列表。

```java
Map<Integer, List<String>> nameGroupMap = users.stream()
        .collect(Collectors.groupingBy(
                User::getAge,
                Collectors.mapping(User::getName, Collectors.toList())
        ));

System.out.println(nameGroupMap);
```

分组后还可以继续对结果进行统计、映射、求和等操作。

```java
Map<Integer, Integer> ageSumMap = users.stream()
        .collect(Collectors.groupingBy(
                User::getAge,
                Collectors.summingInt(User::getAge)
        ));

System.out.println(ageSumMap);
```

`groupingBy` 适合做内存中的小规模分组统计。如果数据量较大，或者数据来自数据库，优先考虑使用 SQL 的 `GROUP BY`，避免把大量数据加载到 JVM 内存中处理。

### joining 拼接

`Collectors.joining()` 用于将字符串 Stream 拼接成一个字符串。

直接拼接字符串。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

String result = names.stream()
        .collect(Collectors.joining());

System.out.println(result);
```

使用分隔符拼接。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

String result = names.stream()
        .collect(Collectors.joining(","));

System.out.println(result);
```

添加前缀和后缀。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

String result = names.stream()
        .collect(Collectors.joining(",", "[", "]"));

System.out.println(result);
```

输出结果如下：

```text
[Tom,Jerry,Jack]
```

对象集合拼接字段。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 22)
);

String names = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.joining(","));

System.out.println(names);
```

`joining` 只能直接用于 `Stream<String>`。如果 Stream 中是对象，需要先使用 `map` 转换成字符串。

## Optional 与 Stream

Stream 中很多查找类、最值类操作都会返回 Optional，例如 `findFirst`、`findAny`、`min`、`max`。Optional 用于表达“结果可能存在，也可能不存在”，避免直接返回 `null` 带来的空指针风险。

Optional 不是集合，也不是 Stream，它是一个容器对象，最多只保存一个值。

### findFirst

`findFirst` 用于获取 Stream 中的第一个元素，返回值是 `Optional<T>`。

获取第一个字符串。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

Optional<String> first = names.stream()
        .findFirst();

first.ifPresent(System.out::println);
```

获取第一个满足条件的元素。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

Optional<String> first = names.stream()
        .filter(name -> name.startsWith("J"))
        .findFirst();

System.out.println(first.orElse("未找到"));
```

在对象集合中，`findFirst` 常用于获取第一条符合条件的数据。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 22)
);

Optional<User> userOptional = users.stream()
        .filter(user -> user.getAge() >= 20)
        .findFirst();

userOptional.ifPresent(user -> System.out.println(user.getName()));
```

在顺序流中，`findFirst` 返回符合条件的第一个元素。对于有顺序的数据源，例如 List，它会遵守原始顺序。

### findAny

`findAny` 用于获取 Stream 中任意一个元素，返回值也是 `Optional<T>`。

在顺序流中，`findAny` 的结果通常和 `findFirst` 类似。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

Optional<String> any = names.stream()
        .filter(name -> name.startsWith("J"))
        .findAny();

System.out.println(any.orElse("未找到"));
```

在并行流中，`findAny` 更适合获取任意一个满足条件的元素，因为它不要求保持顺序，可能有更好的执行效率。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

Optional<String> any = names.parallelStream()
        .filter(name -> name.length() > 3)
        .findAny();

System.out.println(any.orElse("未找到"));
```

如果业务要求必须获取第一个元素，应使用 `findFirst`。如果只关心是否能获取到任意一个符合条件的元素，可以使用 `findAny`。

### max 与 min 返回值

`max` 和 `min` 返回 Optional，是因为 Stream 可能没有任何元素。如果直接返回普通对象，就无法安全表达“没有结果”。

空集合调用 `max`。

```java
List<Integer> numbers = CollUtil.newArrayList();

Optional<Integer> max = numbers.stream()
        .max(Integer::compareTo);

System.out.println(max.isPresent());
```

输出结果如下：

```text
false
```

对象集合中获取年龄最大的用户。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 22)
);

Optional<User> maxAgeUser = users.stream()
        .max(Comparator.comparing(User::getAge));

String userName = maxAgeUser
        .map(User::getName)
        .orElse("未找到用户");

System.out.println(userName);
```

对象集合中获取年龄最小的用户。

```java
Optional<User> minAgeUser = users.stream()
        .min(Comparator.comparing(User::getAge));

User user = minAgeUser.orElse(null);

System.out.println(user);
```

如果业务要求必须存在最大值或最小值，可以使用 `orElseThrow`。

```java
User maxAgeUser = users.stream()
        .max(Comparator.comparing(User::getAge))
        .orElseThrow(() -> new IllegalArgumentException("用户列表为空，无法获取年龄最大用户"));

System.out.println(maxAgeUser.getName());
```

### Optional 结果处理

Optional 提供了多种安全处理结果的方式，常用方法包括 `isPresent`、`ifPresent`、`orElse`、`orElseGet`、`orElseThrow`、`map`、`filter` 等。

使用 `ifPresent` 在值存在时执行逻辑。

```java
Optional<String> nameOptional = Optional.of("Tom");

nameOptional.ifPresent(name -> System.out.println("用户名：" + name));
```

使用 `orElse` 设置默认值。

```java
Optional<String> nameOptional = Optional.empty();

String name = nameOptional.orElse("默认用户");

System.out.println(name);
```

使用 `orElseGet` 延迟生成默认值。

```java
Optional<String> nameOptional = Optional.empty();

String name = nameOptional.orElseGet(() -> "默认用户");

System.out.println(name);
```

`orElse` 和 `orElseGet` 的区别在于：`orElse` 中的默认值会提前计算，`orElseGet` 中的 Supplier 只有在 Optional 为空时才会执行。因此，默认值计算比较复杂时，优先使用 `orElseGet`。

使用 `orElseThrow` 在值不存在时抛出异常。

```java
List<User> users = CollUtil.newArrayList();

User user = users.stream()
        .filter(item -> item.getId().equals(1L))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

System.out.println(user);
```

使用 `map` 提取 Optional 中对象的字段。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18)
);

String userName = users.stream()
        .filter(user -> user.getId().equals(1L))
        .findFirst()
        .map(User::getName)
        .orElse("未知用户");

System.out.println(userName);
```

使用 `filter` 对 Optional 中的值继续判断。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18)
);

String userName = users.stream()
        .filter(user -> user.getId().equals(1L))
        .findFirst()
        .filter(user -> user.getAge() >= 18)
        .map(User::getName)
        .orElse("未找到成年用户");

System.out.println(userName);
```

Optional 的推荐用法是用于返回值处理，而不是作为实体类字段或方法参数大量使用。实际开发中，Optional 更适合配合 Stream 的查找、最大值、最小值等操作，安全处理可能为空的结果。

下面给出本文示例中使用到的 `User` 类，便于直接运行对象集合相关示例。

文件位置：`src/main/java/io/github/atengk/stream/User.java`

```java
package io.github.atengk.stream;

/**
 * 用户信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class User {

    private Long id;

    private String name;

    private Integer age;

    public User() {
    }

    public User(Long id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户ID
     *
     * @param id 用户ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置用户名
     *
     * @param name 用户名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取年龄
     *
     * @return 年龄
     */
    public Integer getAge() {
        return age;
    }

    /**
     * 设置年龄
     *
     * @param age 年龄
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
```



## 并行流

并行流用于将 Stream 中的数据拆分成多个任务，并交给多个线程并发执行。它的目标是利用多核 CPU 提升数据处理效率，但并行流并不是所有场景都适合使用。

在 Java 中，可以通过 `parallelStream()` 或 `stream().parallel()` 创建并行流。并行流底层默认使用 `ForkJoinPool.commonPool()` 执行任务。

### parallelStream

`parallelStream()` 是集合提供的并行流创建方法，常用于对集合中的大量数据进行并发处理。

下面的示例演示普通顺序流和并行流的区别。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony", "Lucy");

names.stream()
        .forEach(name -> System.out.println("顺序流：" + name + "，线程：" + Thread.currentThread().getName()));

names.parallelStream()
        .forEach(name -> System.out.println("并行流：" + name + "，线程：" + Thread.currentThread().getName()));
```

顺序流通常由主线程按顺序执行，并行流可能由多个线程同时执行，因此输出顺序不一定和原集合顺序一致。

也可以通过 `stream().parallel()` 将普通流转换成并行流。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.stream()
        .parallel()
        .filter(name -> name.length() > 3)
        .map(String::toUpperCase)
        .collect(Collectors.toList());

System.out.println(result);
```

在实际开发中，`parallelStream()` 更直观，适合从集合直接创建并行流；`stream().parallel()` 更适合在已有 Stream 流水线中切换执行模式。

### 并行流执行特点

并行流的核心特点是任务拆分、并发执行、结果合并。它会把数据源拆分成多个子任务，由多个线程分别处理，最后将结果合并成最终结果。

并行流不保证 `forEach` 的执行顺序。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5, 6);

numbers.parallelStream()
        .forEach(num -> System.out.println("数字：" + num + "，线程：" + Thread.currentThread().getName()));
```

如果需要保持原始顺序，可以使用 `forEachOrdered`。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5, 6);

numbers.parallelStream()
        .forEachOrdered(num -> System.out.println("数字：" + num));
```

需要注意，`forEachOrdered` 虽然可以保证顺序，但会降低并行流的执行优势。

并行流适合无状态操作。下面的写法是安全的，因为每个元素的处理逻辑相互独立。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack", "Tony");

List<String> result = names.parallelStream()
        .filter(StrUtil::isNotBlank)
        .map(String::toUpperCase)
        .collect(Collectors.toList());

System.out.println(result);
```

不推荐在并行流中修改共享变量。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5);
List<Integer> result = CollUtil.newArrayList();

numbers.parallelStream()
        .forEach(result::add);
```

上面的代码存在并发安全风险，因为多个线程可能同时修改同一个 `ArrayList`。正确写法是使用 `collect` 收集结果。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5);

List<Integer> result = numbers.parallelStream()
        .filter(num -> num > 2)
        .collect(Collectors.toList());

System.out.println(result);
```

### 并行流适用场景

并行流适合数据量较大、每个元素处理成本较高、元素之间没有依赖关系的场景。

常见适用场景包括：

| 场景           | 说明                                       |
| -------------- | ------------------------------------------ |
| 大集合计算     | 数据量较大，单个元素处理逻辑较重           |
| CPU 密集型任务 | 例如复杂计算、规则匹配、数据转换           |
| 无共享状态     | 每个元素独立处理，不依赖外部可变变量       |
| 顺序不敏感     | 不要求严格保持原始处理顺序                 |
| 数据容易拆分   | 例如 ArrayList、数组这类结构更容易并行拆分 |

示例：批量计算平方值。

```java
List<Integer> numbers = IntStream.rangeClosed(1, 10000)
        .boxed()
        .collect(Collectors.toList());

List<Integer> result = numbers.parallelStream()
        .map(num -> num * num)
        .collect(Collectors.toList());

System.out.println(result.size());
```

示例：批量转换对象字段。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 20),
        new User(3L, "王五", 22)
);

List<String> names = users.parallelStream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(names);
```

如果只是少量数据处理，或者只是简单字段提取、简单过滤，普通顺序流通常更合适。

### 并行流注意事项

并行流使用不当容易引入线程安全问题、顺序问题和性能问题。

第一，不要在并行流中修改非线程安全的共享变量。

```java
List<Integer> numbers = CollUtil.newArrayList(1, 2, 3, 4, 5);
List<Integer> result = new ArrayList<>();

numbers.parallelStream()
        .forEach(result::add);
```

应改为使用 `collect`。

```java
List<Integer> result = numbers.parallelStream()
        .filter(num -> num > 2)
        .collect(Collectors.toList());
```

第二，不要在并行流中执行强依赖顺序的业务逻辑。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

names.parallelStream()
        .forEach(System.out::println);
```

如果业务要求严格顺序，使用顺序流更清晰。

```java
names.stream()
        .forEach(System.out::println);
```

第三，不要盲目认为并行流一定更快。并行流有任务拆分、线程调度、结果合并的成本。

下面这类简单操作通常没有必要使用并行流。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

List<String> result = names.parallelStream()
        .map(String::toUpperCase)
        .collect(Collectors.toList());
```

第四，避免在并行流中执行阻塞操作，例如大量远程接口调用、数据库访问、文件 IO 等。并行流默认使用公共线程池，阻塞操作可能影响同一 JVM 中其他并行任务。

第五，注意异常处理。并行流中某个任务抛出异常，可能导致整体任务失败。

```java
List<String> numbers = CollUtil.newArrayList("1", "2", "a", "4");

List<Integer> result = numbers.parallelStream()
        .map(Integer::valueOf)
        .collect(Collectors.toList());
```

上面的代码遇到 `"a"` 会抛出 `NumberFormatException`。如果数据不可靠，应提前过滤或单独处理异常。

```java
List<String> numbers = CollUtil.newArrayList("1", "2", "a", "4");

List<Integer> result = numbers.parallelStream()
        .filter(NumberUtil::isInteger)
        .map(Integer::valueOf)
        .collect(Collectors.toList());

System.out.println(result);
```

## Stream 实践

Stream 在实际开发中常用于集合过滤、字段提取、分组统计、多级排序和数据转换。下面通过常见业务场景说明 Stream 的使用方式。

示例中的对象仍然使用 `User`，假设它包含 `id`、`name`、`age` 等字段。

### 集合过滤

集合过滤是 Stream 最基础、最常见的使用场景。它通常通过 `filter` 实现。

过滤非空字符串。

```java
List<String> names = CollUtil.newArrayList("Tom", "", "Jerry", " ", "Jack");

List<String> result = names.stream()
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(result);
```

过滤年龄大于等于 18 的用户。

```java
List<User> users = CollUtil.newArrayList(
        new User(1L, "张三", 18),
        new User(2L, "李四", 16),
        new User(3L, "王五", 22)
);

List<User> adultUsers = users.stream()
        .filter(user -> user.getAge() != null)
        .filter(user -> user.getAge() >= 18)
        .collect(Collectors.toList());

System.out.println(adultUsers);
```

如果过滤条件较复杂，可以先拆成多个 `filter`，提高可读性。

```java
List<User> result = users.stream()
        .filter(user -> user.getId() != null)
        .filter(user -> StrUtil.isNotBlank(user.getName()))
        .filter(user -> user.getAge() != null && user.getAge() >= 18)
        .collect(Collectors.toList());
```

### 对象字段提取

对象字段提取通常使用 `map`，用于从对象集合中提取某个字段。

提取用户 ID 列表。

```java
List<Long> userIds = users.stream()
        .map(User::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

System.out.println(userIds);
```

提取用户名并过滤空白值。

```java
List<String> userNames = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(userNames);
```

提取字段并去重。

```java
Set<String> nameSet = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toSet());

System.out.println(nameSet);
```

如果需要保持字段顺序并去重，可以使用 `LinkedHashSet`。

```java
Set<String> nameSet = users.stream()
        .map(User::getName)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toCollection(LinkedHashSet::new));
```

### 分组统计

分组统计通常使用 `Collectors.groupingBy()`，可以按照对象字段进行分组，也可以在分组后统计数量、求和、提取字段等。

按年龄分组。

```java
Map<Integer, List<User>> ageGroupMap = users.stream()
        .filter(user -> user.getAge() != null)
        .collect(Collectors.groupingBy(User::getAge));

System.out.println(ageGroupMap);
```

按是否成年分组。

```java
Map<Boolean, List<User>> adultGroupMap = users.stream()
        .filter(user -> user.getAge() != null)
        .collect(Collectors.groupingBy(user -> user.getAge() >= 18));

System.out.println(adultGroupMap);
```

按年龄统计人数。

```java
Map<Integer, Long> ageCountMap = users.stream()
        .filter(user -> user.getAge() != null)
        .collect(Collectors.groupingBy(
                User::getAge,
                Collectors.counting()
        ));

System.out.println(ageCountMap);
```

按年龄分组，并提取每组用户名称。

```java
Map<Integer, List<String>> ageNameMap = users.stream()
        .filter(user -> user.getAge() != null)
        .collect(Collectors.groupingBy(
                User::getAge,
                Collectors.mapping(User::getName, Collectors.toList())
        ));

System.out.println(ageNameMap);
```

实际项目中，如果数据来自数据库并且数据量较大，分组统计优先交给 SQL 完成。Stream 分组更适合已经在内存中的小批量数据。

### 多级排序

多级排序通常使用 `Comparator.comparing()` 和 `thenComparing()`。常见场景是先按一个字段排序，字段相同时再按另一个字段排序。

按年龄升序排序。

```java
List<User> result = users.stream()
        .sorted(Comparator.comparing(User::getAge))
        .collect(Collectors.toList());

System.out.println(result);
```

按年龄降序排序。

```java
List<User> result = users.stream()
        .sorted(Comparator.comparing(User::getAge).reversed())
        .collect(Collectors.toList());

System.out.println(result);
```

先按年龄升序，再按 ID 升序。

```java
List<User> result = users.stream()
        .sorted(
                Comparator.comparing(User::getAge)
                        .thenComparing(User::getId)
        )
        .collect(Collectors.toList());

System.out.println(result);
```

如果排序字段可能为空，应处理空值，避免出现空指针异常。

```java
List<User> result = users.stream()
        .sorted(
                Comparator.comparing(
                        User::getAge,
                        Comparator.nullsLast(Integer::compareTo)
                )
        )
        .collect(Collectors.toList());

System.out.println(result);
```

多字段排序时，也可以分别处理每个字段的空值。

```java
List<User> result = users.stream()
        .sorted(
                Comparator.comparing(
                                User::getAge,
                                Comparator.nullsLast(Integer::compareTo)
                        )
                        .thenComparing(
                                User::getId,
                                Comparator.nullsLast(Long::compareTo)
                        )
        )
        .collect(Collectors.toList());

System.out.println(result);
```

### 数据转换

数据转换是 Stream 的典型应用场景，通常使用 `map` 将一种对象转换成另一种对象，例如 Entity 转 DTO、DTO 转 VO、对象转 Map 等。

下面示例将 `User` 转换为 `UserVO`。

文件位置：`src/main/java/io/github/atengk/stream/UserVO.java`

```java
package io.github.atengk.stream;

/**
 * 用户展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserVO {

    private Long id;

    private String name;

    private String ageText;

    public UserVO() {
    }

    public UserVO(Long id, String name, String ageText) {
        this.id = id;
        this.name = name;
        this.ageText = ageText;
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户ID
     *
     * @param id 用户ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置用户名
     *
     * @param name 用户名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取年龄文本
     *
     * @return 年龄文本
     */
    public String getAgeText() {
        return ageText;
    }

    /**
     * 设置年龄文本
     *
     * @param ageText 年龄文本
     */
    public void setAgeText(String ageText) {
        this.ageText = ageText;
    }

    @Override
    public String toString() {
        return "UserVO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ageText='" + ageText + '\'' +
                '}';
    }
}
```

将用户集合转换为展示对象集合。

```java
List<UserVO> userVOList = users.stream()
        .filter(user -> user.getId() != null)
        .map(user -> new UserVO(
                user.getId(),
                user.getName(),
                user.getAge() == null ? "未知" : user.getAge() + "岁"
        ))
        .collect(Collectors.toList());

System.out.println(userVOList);
```

也可以将对象集合转换为 Map，便于后续按 ID 查询。

```java
Map<Long, UserVO> userVOMap = users.stream()
        .filter(user -> user.getId() != null)
        .map(user -> new UserVO(
                user.getId(),
                user.getName(),
                user.getAge() == null ? "未知" : user.getAge() + "岁"
        ))
        .collect(Collectors.toMap(
                UserVO::getId,
                userVO -> userVO,
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new
        ));

System.out.println(userVOMap);
```

数据转换时应注意空值、重复 key、字段格式化等问题。对于复杂对象转换，可以单独封装转换方法，避免 Stream 链路过长。

```java
List<UserVO> userVOList = users.stream()
        .filter(user -> user.getId() != null)
        .map(StreamPracticeDemo::convertToUserVO)
        .collect(Collectors.toList());
```

转换方法可以这样写。

```java
/**
 * 转换用户展示对象
 *
 * @param user 用户信息
 * @return 用户展示对象
 */
private static UserVO convertToUserVO(User user) {
    String ageText = user.getAge() == null ? "未知" : user.getAge() + "岁";
    return new UserVO(user.getId(), user.getName(), ageText);
}
```

## 常见问题

本节整理 Stream 使用中比较常见的问题，包括是否修改原集合、中间操作执行时机、`map` 和 `flatMap` 的区别、`forEach` 和 `collect` 的选择，以及并行流的性能判断。

### Stream 是否会修改原集合

Stream 默认不会修改原集合。大多数 Stream 操作会基于原数据生成新的处理结果，而不是直接改变原集合。

下面的代码中，`map` 会生成新的字符串结果，但不会修改原来的 `names` 集合。

```java
List<String> names = new ArrayList<>(CollUtil.newArrayList("Tom", "Jerry", "Jack"));

List<String> result = names.stream()
        .map(String::toUpperCase)
        .collect(Collectors.toList());

System.out.println(names);
System.out.println(result);
```

输出结果如下：

```text
[Tom, Jerry, Jack]
[TOM, JERRY, JACK]
```

如果需要修改原集合，应使用集合自身的方法，例如 `replaceAll`。

```java
List<String> names = new ArrayList<>(CollUtil.newArrayList("Tom", "Jerry", "Jack"));

names.replaceAll(String::toUpperCase);

System.out.println(names);
```

如果需要删除原集合中的元素，可以使用 `removeIf`。

```java
List<String> names = new ArrayList<>(CollUtil.newArrayList("Tom", "", "Jerry", " "));

names.removeIf(StrUtil::isBlank);

System.out.println(names);
```

结论是：Stream 更适合生成处理后的新结果；如果业务目标是直接修改原集合，应使用集合提供的修改方法。

### 中间操作为什么不立即执行

中间操作不立即执行，是因为 Stream 采用惰性执行机制。`filter`、`map`、`sorted` 等中间操作只是记录处理规则，不会马上遍历数据。

只有调用终止操作时，Stream 才会真正开始执行。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

Stream<String> stream = names.stream()
        .filter(name -> {
            System.out.println("执行过滤：" + name);
            return name.length() > 3;
        })
        .map(name -> {
            System.out.println("执行转换：" + name);
            return name.toUpperCase();
        });

System.out.println("中间操作定义完成");
```

上面的代码不会打印 `执行过滤` 和 `执行转换`，因为还没有终止操作。

添加终止操作后才会执行。

```java
List<String> result = stream.collect(Collectors.toList());

System.out.println(result);
```

惰性执行的好处是可以减少不必要的计算，并支持短路操作。

```java
Optional<String> first = names.stream()
        .filter(name -> {
            System.out.println("过滤：" + name);
            return name.startsWith("J");
        })
        .findFirst();

System.out.println(first.orElse("未找到"));
```

`findFirst` 找到第一个符合条件的元素后，就可以提前结束，不需要继续处理后面的所有元素。

### map 与 flatMap 的区别

`map` 用于一对一转换，`flatMap` 用于一对多转换后再扁平化合并。

`map` 的结果会保留嵌套结构。

```java
List<List<String>> nestedList = CollUtil.newArrayList(
        CollUtil.newArrayList("Java", "Spring"),
        CollUtil.newArrayList("MySQL", "Redis")
);

List<Stream<String>> result = nestedList.stream()
        .map(Collection::stream)
        .collect(Collectors.toList());

System.out.println(result);
```

`flatMap` 会把多个内部 Stream 合并成一个 Stream。

```java
List<List<String>> nestedList = CollUtil.newArrayList(
        CollUtil.newArrayList("Java", "Spring"),
        CollUtil.newArrayList("MySQL", "Redis")
);

List<String> result = nestedList.stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

System.out.println(result);
```

输出结果如下：

```text
[Java, Spring, MySQL, Redis]
```

再看一个字符串拆分示例。`map` 会得到多个数组或多个 Stream，`flatMap` 可以直接合并为一个标签列表。

```java
List<String> tagTexts = CollUtil.newArrayList("Java,Spring", "MySQL,Redis");

List<String> tags = tagTexts.stream()
        .flatMap(text -> Arrays.stream(text.split(",")))
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toList());

System.out.println(tags);
```

可以用一句话区分：`map` 是转换元素，`flatMap` 是转换后再展开。

### forEach 与 collect 的选择

`forEach` 适合执行动作，`collect` 适合生成结果。

如果只是打印、记录日志、调用某个方法，可以使用 `forEach`。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

names.stream()
        .filter(StrUtil::isNotBlank)
        .forEach(name -> System.out.println("处理名称：" + name));
```

如果需要得到一个新的集合，应使用 `collect`。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

List<String> result = names.stream()
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());

System.out.println(result);
```

不推荐使用 `forEach` 手动收集数据。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");
List<String> result = new ArrayList<>();

names.stream()
        .filter(name -> name.length() > 3)
        .forEach(result::add);
```

更推荐使用下面的写法。

```java
List<String> result = names.stream()
        .filter(name -> name.length() > 3)
        .collect(Collectors.toList());
```

选择原则如下：

| 操作目标     | 推荐方法                           |
| ------------ | ---------------------------------- |
| 打印数据     | `forEach`                          |
| 调用外部方法 | `forEach`                          |
| 生成 List    | `collect(Collectors.toList())`     |
| 生成 Set     | `collect(Collectors.toSet())`      |
| 生成 Map     | `collect(Collectors.toMap())`      |
| 分组统计     | `collect(Collectors.groupingBy())` |

### 并行流一定更快吗

并行流不一定更快。它是否更快取决于数据量、任务复杂度、线程调度成本、数据结构是否容易拆分，以及是否存在共享状态或阻塞操作。

对于少量数据或简单操作，顺序流通常更合适。

```java
List<String> names = CollUtil.newArrayList("Tom", "Jerry", "Jack");

List<String> result = names.stream()
        .map(String::toUpperCase)
        .collect(Collectors.toList());
```

对于大量数据和较重的 CPU 计算，并行流才可能体现优势。

```java
List<Integer> numbers = IntStream.rangeClosed(1, 1000000)
        .boxed()
        .collect(Collectors.toList());

List<Integer> result = numbers.parallelStream()
        .map(num -> num * num)
        .collect(Collectors.toList());

System.out.println(result.size());
```

并行流不适合下面这些场景：

| 不适合场景           | 原因                           |
| -------------------- | ------------------------------ |
| 数据量很小           | 线程调度成本可能大于收益       |
| 操作很简单           | 并行拆分和合并成本不划算       |
| 需要严格顺序         | 保序会削弱并行优势             |
| 修改共享变量         | 容易出现线程安全问题           |
| 大量 IO 操作         | 可能阻塞公共线程池             |
| 依赖数据库事务上下文 | 多线程执行可能导致上下文不一致 |

实际开发中，默认优先使用顺序流。只有在明确满足“大数据量、CPU 密集、无共享状态、顺序不敏感”这些条件时，再考虑使用并行流。
