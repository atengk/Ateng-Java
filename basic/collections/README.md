# 集合框架

## 集合框架概述

集合框架是 Java 中用于存储、操作和管理一组对象的标准 API。它提供了统一的接口、常用的数据结构和大量工具方法，使开发者不需要从零实现数组扩容、元素查找、排序、去重、映射关系维护等基础能力。

在实际开发中，集合几乎贯穿所有业务代码，例如用户列表、订单集合、配置映射、缓存数据、去重结果、任务队列等都可以通过集合框架完成。

### 集合框架的作用

集合框架的核心作用是对一组对象进行统一管理。相比手动使用数组或自定义数据结构，集合框架提供了更高层次的封装，能够提升代码的可读性、复用性和开发效率。

集合框架主要解决以下问题：

1. **动态存储数据**

数组长度固定，创建后不能直接改变容量。集合可以根据数据量自动扩容，更适合处理数量不确定的数据。

例如查询数据库中的用户列表时，返回的数据条数可能是 0 条、10 条，也可能是几千条，使用 `List` 比数组更灵活。

1. **提供常见数据结构**

Java 集合框架内置了多种常用数据结构，例如：

| 数据结构   | 常见实现             | 适用场景           |
| ---------- | -------------------- | ------------------ |
| 动态数组   | `ArrayList`          | 查询多、随机访问多 |
| 链表       | `LinkedList`         | 头尾插入、删除较多 |
| 哈希表     | `HashMap`、`HashSet` | 快速查找、去重     |
| 红黑树     | `TreeMap`、`TreeSet` | 自动排序           |
| 队列       | `Queue`、`Deque`     | 任务排队、先进先出 |
| 优先级队列 | `PriorityQueue`      | 按优先级处理任务   |

1. **统一操作方式**

集合框架通过接口定义了一套统一的操作方法，例如添加、删除、判断、遍历等。

常见方法包括：

```java
add()
remove()
contains()
size()
isEmpty()
clear()
iterator()
```

不同集合实现类虽然底层结构不同，但很多基础操作方式是统一的，这降低了学习和使用成本。

1. **支持泛型，提升类型安全**

集合通常配合泛型使用，用于限制集合中元素的类型，避免运行时类型转换异常。

```java
List<String> names = new ArrayList<>();
names.add("张三");
names.add("李四");
```

使用泛型后，集合只能存放指定类型的数据。编译器会在编译阶段检查类型问题，减少运行时错误。

1. **提供排序、查找、同步、不可变集合等工具能力**

Java 提供了 `Collections`、`Arrays` 等工具类，可以对集合进行排序、反转、查找、线程安全包装、不可变包装等操作。

例如：

```java
Collections.sort(list);
Collections.reverse(list);
Collections.unmodifiableList(list);
```

在业务开发中，如果项目引入了 Hutool，也可以使用 `CollUtil`、`MapUtil` 简化集合判空、构建和转换逻辑。

### Collection 与 Map 体系

Java 集合框架主要分为两大体系：`Collection` 体系和 `Map` 体系。

`Collection` 用于存储单个元素，`Map` 用于存储键值对。二者都是集合框架的重要组成部分，但它们的设计目标不同。

#### Collection 体系

`Collection` 是单列集合的根接口，用于表示一组独立元素。

它下面主要有三个常用子接口：

| 接口    | 特点                                   | 常见实现类                                  |
| ------- | -------------------------------------- | ------------------------------------------- |
| `List`  | 元素有序、可重复                       | `ArrayList`、`LinkedList`、`Vector`         |
| `Set`   | 元素不可重复                           | `HashSet`、`LinkedHashSet`、`TreeSet`       |
| `Queue` | 队列结构，通常用于先进先出或优先级处理 | `LinkedList`、`ArrayDeque`、`PriorityQueue` |

`List` 关注元素顺序和下标访问，适合存储列表数据。

`Set` 关注元素唯一性，适合做去重。

`Queue` 关注元素的进入和取出顺序，适合任务队列、消息缓冲、广度优先搜索等场景。

#### Map 体系

`Map` 是双列集合的根接口，用于存储键值对数据。

每个元素由 `key` 和 `value` 组成，通过 `key` 查找对应的 `value`。

常见实现类包括：

| 实现类              | 特点                                      |
| ------------------- | ----------------------------------------- |
| `HashMap`           | 最常用，基于哈希表，查询效率高，键无序    |
| `LinkedHashMap`     | 在 `HashMap` 基础上维护插入顺序或访问顺序 |
| `TreeMap`           | 基于红黑树，键会自动排序                  |
| `Hashtable`         | 早期线程安全 Map，性能较低，已较少使用    |
| `ConcurrentHashMap` | 并发场景下常用的线程安全 Map              |

`Map` 适合处理映射关系，例如：

```text
用户ID -> 用户对象
商品编号 -> 商品库存
配置项名称 -> 配置项值
城市编码 -> 城市名称
```

#### Collection 与 Map 的核心区别

| 对比项               | Collection                              | Map                                       |
| -------------------- | --------------------------------------- | ----------------------------------------- |
| 数据结构             | 单列集合                                | 双列集合                                  |
| 存储内容             | 一个一个的元素                          | 一组一组的键值对                          |
| 根接口               | `Collection`                            | `Map`                                     |
| 是否直接继承同一接口 | 是集合单列体系                          | 独立的键值对体系                          |
| 常见实现             | `ArrayList`、`HashSet`、`PriorityQueue` | `HashMap`、`TreeMap`、`ConcurrentHashMap` |
| 典型场景             | 用户列表、订单列表、去重集合            | ID 映射对象、配置映射、缓存结构           |

简单理解：

```text
Collection 存的是一批对象
Map 存的是一批 key-value 映射关系
```

下面的示例演示 `Collection` 和 `Map` 的基本使用方式。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 集合框架基础示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CollectionOverviewDemo {

    /**
     * 演示 Collection 体系中的 List 用法
     *
     * @return 用户名称列表
     */
    public static List<String> buildNameList() {
        List<String> names = new ArrayList<>();
        names.add("张三");
        names.add("李四");
        names.add("王五");

        if (CollUtil.isNotEmpty(names)) {
            System.out.println("用户列表不为空，数量：" + names.size());
        }

        return names;
    }

    /**
     * 演示 Map 体系中的 HashMap 用法
     *
     * @return 用户ID与用户名称的映射关系
     */
    public static Map<Long, String> buildUserMap() {
        Map<Long, String> userMap = new HashMap<>();
        userMap.put(1001L, "张三");
        userMap.put(1002L, "李四");
        userMap.put(1003L, "王五");

        if (MapUtil.isNotEmpty(userMap)) {
            System.out.println("用户映射不为空，数量：" + userMap.size());
        }

        return userMap;
    }

    public static void main(String[] args) {
        List<String> names = buildNameList();
        Map<Long, String> userMap = buildUserMap();

        System.out.println(names);
        System.out.println(userMap);
    }
}
```

这个示例中，`List<String>` 用于存储一组用户名，属于 `Collection` 单列集合体系；`Map<Long, String>` 用于存储用户 ID 和用户名之间的映射关系，属于 `Map` 双列集合体系。

### 集合与数组的区别

数组和集合都可以用来存储多个数据，但它们的设计目标不同。

数组是 Java 语言层面的基础数据结构，集合是 Java 标准库提供的对象容器。数组更底层，集合更灵活。

#### 长度区别

数组在创建时必须指定长度，并且长度固定。

```java
String[] names = new String[3];
```

集合不需要提前指定固定长度，可以根据元素数量动态扩容。

```java
List<String> names = new ArrayList<>();
names.add("张三");
names.add("李四");
```

因此，当数据数量不确定时，优先使用集合。

#### 存储类型区别

数组既可以存储基本数据类型，也可以存储引用数据类型。

```java
int[] numbers = {1, 2, 3};
String[] names = {"张三", "李四"};
```

集合只能存储引用数据类型，不能直接存储基本数据类型。如果要存储基本类型，需要使用包装类。

```java
List<Integer> numbers = new ArrayList<>();
numbers.add(1);
numbers.add(2);
```

这里的 `Integer` 是 `int` 的包装类。

#### 功能区别

数组本身提供的功能较少，主要通过下标访问元素。

```java
String name = names[0];
```

集合提供了更丰富的方法，例如添加、删除、判断、遍历、排序、转换等。

```java
names.add("王五");
names.remove("张三");
names.contains("李四");
names.size();
```

集合更适合复杂业务场景。

#### 性能区别

数组结构简单，内存连续，随机访问速度快，适合长度固定、性能要求较高的场景。

集合内部通常封装了更复杂的数据结构，例如数组、链表、哈希表、红黑树等，功能更强，但也会有额外的对象开销。

在多数业务开发中，集合的灵活性比数组更重要，因此集合使用频率更高。

#### 使用场景区别

| 场景                           | 推荐使用      |
| ------------------------------ | ------------- |
| 数据长度固定                   | 数组          |
| 数据长度不确定                 | 集合          |
| 需要频繁添加、删除元素         | 集合          |
| 需要去重                       | `Set`         |
| 需要键值对映射                 | `Map`         |
| 需要按下标访问                 | 数组或 `List` |
| 需要排序、查找、转换等高级操作 | 集合          |

#### 对比总结

| 对比项     | 数组                       | 集合                                 |
| ---------- | -------------------------- | ------------------------------------ |
| 长度       | 固定                       | 可动态变化                           |
| 存储类型   | 基本类型、引用类型都可以   | 只能存储引用类型                     |
| 功能       | 功能较少，主要依靠下标访问 | 功能丰富，支持增删改查、遍历、排序等 |
| 泛型支持   | 不支持泛型                 | 支持泛型                             |
| 数据结构   | 单一连续结构               | 多种实现结构                         |
| 使用复杂度 | 简单但不够灵活             | 灵活，适合业务开发                   |
| 典型场景   | 固定长度数据、底层算法     | 业务列表、映射关系、去重、队列       |

下面的示例演示数组和集合在使用方式上的区别。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 数组与集合区别示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ArrayAndCollectionDemo {

    /**
     * 演示数组的固定长度特性
     *
     * @return 数组数据
     */
    public static String[] buildArray() {
        String[] names = new String[3];
        names[0] = "张三";
        names[1] = "李四";
        names[2] = "王五";
        return names;
    }

    /**
     * 演示集合的动态扩容特性
     *
     * @return 集合数据
     */
    public static List<String> buildList() {
        List<String> names = new ArrayList<>();
        names.add("张三");
        names.add("李四");
        names.add("王五");
        names.add("赵六");

        if (CollUtil.isNotEmpty(names)) {
            System.out.println("集合数据：" + names);
        }

        return names;
    }

    public static void main(String[] args) {
        String[] arrayNames = buildArray();
        List<String> listNames = buildList();

        System.out.println("数组数据：" + Arrays.toString(arrayNames));
        System.out.println("集合数据：" + listNames);
    }
}
```

数组适合保存固定长度的数据，集合适合处理动态变化的数据。在实际 Java 后端开发中，接口返回列表、数据库查询结果、批量处理参数、去重、分组、映射关系等场景通常优先使用集合。



## Collection 体系

`Collection` 是 Java 单列集合的根接口，表示一组独立元素。它不关心键值映射关系，只负责管理一个个对象。常见的 `List`、`Set`、`Queue` 都继承自 `Collection`，但它们对元素顺序、重复性和取出规则的要求不同。

`Collection` 接口定义了一批集合通用操作，例如添加元素、删除元素、判断元素是否存在、获取集合大小、清空集合和遍历集合等。

常用方法如下：

| 方法                 | 说明                 |
| -------------------- | -------------------- |
| `add(E e)`           | 添加元素             |
| `remove(Object o)`   | 删除指定元素         |
| `contains(Object o)` | 判断是否包含指定元素 |
| `size()`             | 获取元素数量         |
| `isEmpty()`          | 判断集合是否为空     |
| `clear()`            | 清空集合             |
| `iterator()`         | 获取迭代器           |
| `toArray()`          | 转为数组             |

### List 接口

`List` 是有序、可重复的集合接口。这里的有序指的是元素按照添加顺序保存，并且可以通过下标访问元素。`List` 适合存储列表型数据，例如用户列表、订单明细、菜单列表、查询结果列表等。

`List` 的主要特点如下：

| 特点         | 说明                               |
| ------------ | ---------------------------------- |
| 有序         | 元素按照插入顺序保存               |
| 可重复       | 可以保存多个相同元素               |
| 支持下标     | 可以通过索引获取、修改、删除元素   |
| 适合查询列表 | 后端接口返回列表数据时使用频率很高 |

常见实现类包括：

| 实现类       | 特点                                  |
| ------------ | ------------------------------------- |
| `ArrayList`  | 基于动态数组，查询快，增删慢          |
| `LinkedList` | 基于双向链表，头尾增删方便            |
| `Vector`     | 早期线程安全 List，性能较低，较少使用 |

`List` 的基本使用示例如下。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * List 接口基础示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ListDemo {

    /**
     * 构建用户名称列表
     *
     * @return 用户名称列表
     */
    public static List<String> buildUserNames() {
        List<String> names = new ArrayList<>();
        names.add("张三");
        names.add("李四");
        names.add("王五");
        names.add("张三");

        if (CollUtil.isNotEmpty(names)) {
            System.out.println("用户列表数量：" + names.size());
            System.out.println("第一个用户：" + names.get(0));
        }

        return names;
    }

    public static void main(String[] args) {
        List<String> names = buildUserNames();
        System.out.println("用户列表：" + names);
    }
}
```

这个示例中，`List` 可以保存重复的 `"张三"`，并且可以通过 `get(0)` 获取第一个元素。

### Set 接口

`Set` 是无重复元素的集合接口。它不允许保存重复元素，常用于去重场景，例如去重用户 ID、去重订单编号、去重权限编码、去重标签等。

`Set` 的主要特点如下：

| 特点           | 说明                           |
| -------------- | ------------------------------ |
| 不允许重复     | 相同元素只能保存一个           |
| 通常不支持下标 | 不能像 `List` 一样通过索引访问 |
| 适合去重       | 常用于 ID、编码、名称等去重    |
| 实现类规则不同 | 不同实现类的顺序和排序规则不同 |

常见实现类包括：

| 实现类          | 特点                         |
| --------------- | ---------------------------- |
| `HashSet`       | 无序，基于哈希表，去重效率高 |
| `LinkedHashSet` | 保证插入顺序                 |
| `TreeSet`       | 自动排序，底层基于红黑树     |

`Set` 的基本使用示例如下。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Set 接口基础示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SetDemo {

    /**
     * 构建去重后的用户编号集合
     *
     * @return 用户编号集合
     */
    public static Set<Long> buildUserIds() {
        Set<Long> userIds = new HashSet<>();
        userIds.add(1001L);
        userIds.add(1002L);
        userIds.add(1003L);
        userIds.add(1001L);

        if (CollUtil.isNotEmpty(userIds)) {
            System.out.println("去重后的用户数量：" + userIds.size());
        }

        return userIds;
    }

    public static void main(String[] args) {
        Set<Long> userIds = buildUserIds();
        System.out.println("用户编号集合：" + userIds);
    }
}
```

这个示例中，`1001L` 添加了两次，但最终集合中只会保存一份。

### Queue 接口

`Queue` 是队列接口，通常用于按照特定顺序处理元素。普通队列遵循先进先出原则，即先进入队列的元素先被取出。

`Queue` 适合任务排队、消息缓冲、异步处理、限流队列、广度优先搜索等场景。

`Queue` 的常用方法分为两组：

| 操作                 | 抛异常方法  | 返回特殊值方法 |
| -------------------- | ----------- | -------------- |
| 添加元素             | `add(e)`    | `offer(e)`     |
| 取出并删除队首元素   | `remove()`  | `poll()`       |
| 查看队首元素但不删除 | `element()` | `peek()`       |

实际开发中，更推荐使用 `offer()`、`poll()`、`peek()`，因为它们在队列为空或添加失败时不会直接抛出异常，而是返回特殊值。

常见实现类包括：

| 实现类          | 特点                             |
| --------------- | -------------------------------- |
| `LinkedList`    | 可作为普通队列或双端队列使用     |
| `ArrayDeque`    | 基于数组实现的双端队列，性能较好 |
| `PriorityQueue` | 优先级队列，按优先级出队         |

`Queue` 的基本使用示例如下。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Queue 接口基础示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class QueueDemo {

    /**
     * 演示普通队列的先进先出
     */
    public static void processTaskQueue() {
        Queue<String> taskQueue = new LinkedList<>();
        taskQueue.offer("任务A");
        taskQueue.offer("任务B");
        taskQueue.offer("任务C");

        if (CollUtil.isNotEmpty(taskQueue)) {
            System.out.println("当前队首任务：" + taskQueue.peek());
        }

        while (CollUtil.isNotEmpty(taskQueue)) {
            String task = taskQueue.poll();
            System.out.println("正在处理：" + task);
        }
    }

    public static void main(String[] args) {
        processTaskQueue();
    }
}
```

输出顺序会按照 `"任务A"`、`"任务B"`、`"任务C"` 依次处理。

## List 集合

`List` 是 Java 开发中使用频率最高的集合类型之一。它适合保存有顺序、允许重复的数据，尤其适合接口返回列表、数据库查询结果、批量参数和前端表格数据。

`List` 的核心能力包括按下标访问、按顺序遍历、插入指定位置、修改指定位置元素等。

### ArrayList

`ArrayList` 是最常用的 `List` 实现类，底层基于动态数组实现。它支持快速随机访问，通过下标获取元素的效率较高。

`ArrayList` 的主要特点如下：

| 特点     | 说明                           |
| -------- | ------------------------------ |
| 底层结构 | 动态数组                       |
| 查询效率 | 高，支持下标快速访问           |
| 增删效率 | 中间位置增删较慢，需要移动元素 |
| 线程安全 | 非线程安全                     |
| 适用场景 | 查询多、增删少的列表数据       |

`ArrayList` 默认容量会随着元素增加自动扩容。扩容会创建新数组并复制旧数据，因此在明确数据量较大时，可以提前指定容量，减少扩容次数。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ArrayList 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ArrayListDemo {

    /**
     * 演示 ArrayList 的基本操作
     *
     * @return 商品名称列表
     */
    public static List<String> buildGoodsList() {
        List<String> goodsList = new ArrayList<>(8);
        goodsList.add("苹果");
        goodsList.add("香蕉");
        goodsList.add("橙子");

        goodsList.set(1, "雪梨");

        if (CollUtil.isNotEmpty(goodsList)) {
            System.out.println("第一个商品：" + goodsList.get(0));
            System.out.println("商品数量：" + goodsList.size());
        }

        return goodsList;
    }

    public static void main(String[] args) {
        List<String> goodsList = buildGoodsList();
        System.out.println("商品列表：" + goodsList);
    }
}
```

`ArrayList` 适合大部分业务列表场景。例如分页查询返回结果、配置列表、菜单列表、订单明细列表等，通常都可以优先选择 `ArrayList`。

需要注意的是，`ArrayList` 不是线程安全的。如果多个线程同时修改同一个 `ArrayList`，可能出现数据不一致或并发修改异常。并发场景应考虑 `CopyOnWriteArrayList`、`Collections.synchronizedList()` 或其他并发容器。

### LinkedList

`LinkedList` 是基于双向链表实现的 `List`，同时也实现了 `Deque` 接口，因此既可以作为列表使用，也可以作为队列或双端队列使用。

`LinkedList` 的主要特点如下：

| 特点     | 说明                               |
| -------- | ---------------------------------- |
| 底层结构 | 双向链表                           |
| 查询效率 | 按下标查询较慢，需要遍历节点       |
| 增删效率 | 头尾增删效率较高                   |
| 线程安全 | 非线程安全                         |
| 适用场景 | 头尾插入、删除较多，或作为队列使用 |

`LinkedList` 的基本使用如下。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.LinkedList;

/**
 * LinkedList 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LinkedListDemo {

    /**
     * 演示 LinkedList 作为双端队列使用
     *
     * @return 任务队列
     */
    public static LinkedList<String> buildTaskQueue() {
        LinkedList<String> tasks = new LinkedList<>();
        tasks.addLast("任务A");
        tasks.addLast("任务B");
        tasks.addFirst("紧急任务");

        if (CollUtil.isNotEmpty(tasks)) {
            System.out.println("队首任务：" + tasks.getFirst());
            System.out.println("队尾任务：" + tasks.getLast());
        }

        return tasks;
    }

    public static void main(String[] args) {
        LinkedList<String> tasks = buildTaskQueue();

        while (CollUtil.isNotEmpty(tasks)) {
            String task = tasks.removeFirst();
            System.out.println("处理任务：" + task);
        }
    }
}
```

`LinkedList` 不适合频繁通过下标随机访问元素。如果业务中大量使用 `get(index)`，通常应优先选择 `ArrayList`。

### Vector

`Vector` 是 Java 早期提供的线程安全 `List` 实现类，底层同样基于数组。它的大部分方法都使用 `synchronized` 修饰，因此在多线程环境下可以保证单个方法调用的线程安全。

`Vector` 的主要特点如下：

| 特点         | 说明                 |
| ------------ | -------------------- |
| 底层结构     | 动态数组             |
| 线程安全     | 是，方法级别加锁     |
| 性能         | 通常低于 `ArrayList` |
| 出现时间     | JDK 早期集合类       |
| 当前使用情况 | 新项目中较少使用     |

虽然 `Vector` 是线程安全的，但它的锁粒度较粗，性能较低。现代 Java 开发中，一般不推荐优先使用 `Vector`。如果需要线程安全列表，应根据场景选择 `CopyOnWriteArrayList` 或其他并发集合。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.List;
import java.util.Vector;

/**
 * Vector 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class VectorDemo {

    /**
     * 演示 Vector 的基本使用
     *
     * @return 编号列表
     */
    public static List<Integer> buildNumbers() {
        List<Integer> numbers = new Vector<>();
        numbers.add(10);
        numbers.add(20);
        numbers.add(30);

        if (CollUtil.isNotEmpty(numbers)) {
            System.out.println("编号数量：" + numbers.size());
        }

        return numbers;
    }

    public static void main(String[] args) {
        List<Integer> numbers = buildNumbers();
        System.out.println("编号列表：" + numbers);
    }
}
```

`Vector` 在面试中经常用于和 `ArrayList` 对比。实际项目中，如果没有维护历史代码的需要，一般不主动使用 `Vector`。

### List 的遍历方式

`List` 常见遍历方式包括普通 `for` 循环、增强 `for` 循环、`Iterator` 迭代器、`forEach` 方法和 `Stream` 流式遍历。

不同遍历方式适合不同场景。

| 遍历方式        | 适用场景                         |
| --------------- | -------------------------------- |
| 普通 `for` 循环 | 需要使用下标                     |
| 增强 `for` 循环 | 简单读取元素                     |
| `Iterator`      | 遍历时需要安全删除元素           |
| `forEach`       | 简洁遍历                         |
| `Stream`        | 过滤、映射、收集、统计等链式处理 |

下面的示例演示几种常见遍历方式。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * List 遍历方式示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ListEachDemo {

    /**
     * 构建示例用户列表
     *
     * @return 用户名称列表
     */
    public static List<String> buildNames() {
        List<String> names = new ArrayList<>();
        names.add("张三");
        names.add("李四");
        names.add("王五");
        names.add("赵六");
        return names;
    }

    /**
     * 演示 List 的常见遍历方式
     */
    public static void eachList() {
        List<String> names = buildNames();

        if (CollUtil.isEmpty(names)) {
            System.out.println("用户列表为空");
            return;
        }

        for (int i = 0; i < names.size(); i++) {
            System.out.println("普通 for 遍历：" + i + " -> " + names.get(i));
        }

        for (String name : names) {
            System.out.println("增强 for 遍历：" + name);
        }

        names.forEach(name -> System.out.println("forEach 遍历：" + name));

        List<String> filteredNames = names.stream()
                .filter(name -> name.contains("张") || name.contains("李"))
                .collect(Collectors.toList());

        System.out.println("Stream 过滤结果：" + filteredNames);
    }

    /**
     * 使用 Iterator 安全删除元素
     */
    public static void removeByIterator() {
        List<String> names = buildNames();
        Iterator<String> iterator = names.iterator();

        while (iterator.hasNext()) {
            String name = iterator.next();
            if ("李四".equals(name)) {
                iterator.remove();
            }
        }

        System.out.println("删除后的用户列表：" + names);
    }

    public static void main(String[] args) {
        eachList();
        removeByIterator();
    }
}
```

需要注意，不能在增强 `for` 循环中直接调用 `list.remove()` 删除元素，否则可能触发 `ConcurrentModificationException`。遍历时删除元素，推荐使用 `Iterator` 的 `remove()` 方法。

## Set 集合

`Set` 用于保存不重复元素。它不强调下标访问，而是强调元素唯一性。实际开发中，`Set` 常用于去重、判断是否存在、保存唯一编码、保存权限标识等场景。

`Set` 的不同实现类在顺序和排序方面有明显区别：

| 实现类          | 是否有序     | 是否排序             | 去重依据                      |
| --------------- | ------------ | -------------------- | ----------------------------- |
| `HashSet`       | 不保证顺序   | 不排序               | `hashCode()` + `equals()`     |
| `LinkedHashSet` | 保证插入顺序 | 不排序               | `hashCode()` + `equals()`     |
| `TreeSet`       | 有序         | 自然排序或比较器排序 | `compareTo()` 或 `Comparator` |

### HashSet

`HashSet` 是最常用的 `Set` 实现类，底层基于 `HashMap` 实现。它不保证元素的存储顺序，但添加、删除、判断元素是否存在的效率通常较高。

`HashSet` 的主要特点如下：

| 特点         | 说明                           |
| ------------ | ------------------------------ |
| 底层结构     | 基于 `HashMap`                 |
| 是否重复     | 不允许重复                     |
| 是否有序     | 不保证顺序                     |
| 是否线程安全 | 非线程安全                     |
| 适用场景     | 高效去重、快速判断元素是否存在 |

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * HashSet 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashSetDemo {

    /**
     * 演示 HashSet 去重
     *
     * @return 去重后的标签集合
     */
    public static Set<String> buildTags() {
        Set<String> tags = new HashSet<>();
        tags.add("Java");
        tags.add("Spring Boot");
        tags.add("MySQL");
        tags.add("Java");

        if (CollUtil.isNotEmpty(tags)) {
            System.out.println("标签数量：" + tags.size());
        }

        return tags;
    }

    public static void main(String[] args) {
        Set<String> tags = buildTags();
        System.out.println("标签集合：" + tags);
    }
}
```

`HashSet` 不适合依赖输出顺序的场景。如果需要按照插入顺序输出，应使用 `LinkedHashSet`。

### LinkedHashSet

`LinkedHashSet` 是 `HashSet` 的子类，它在哈希表的基础上维护了一条双向链表，因此可以按照元素插入顺序遍历。

`LinkedHashSet` 的主要特点如下：

| 特点     | 说明                     |
| -------- | ------------------------ |
| 底层结构 | 哈希表 + 双向链表        |
| 是否重复 | 不允许重复               |
| 是否有序 | 保证插入顺序             |
| 是否排序 | 不自动排序               |
| 适用场景 | 去重后仍需要保留原始顺序 |

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * LinkedHashSet 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LinkedHashSetDemo {

    /**
     * 演示 LinkedHashSet 保留插入顺序
     *
     * @return 去重后的菜单编码集合
     */
    public static Set<String> buildMenuCodes() {
        Set<String> menuCodes = new LinkedHashSet<>();
        menuCodes.add("system");
        menuCodes.add("user");
        menuCodes.add("role");
        menuCodes.add("user");

        if (CollUtil.isNotEmpty(menuCodes)) {
            System.out.println("菜单编码数量：" + menuCodes.size());
        }

        return menuCodes;
    }

    public static void main(String[] args) {
        Set<String> menuCodes = buildMenuCodes();
        System.out.println("菜单编码集合：" + menuCodes);
    }
}
```

输出结果会保留第一次插入的顺序，即 `system`、`user`、`role`。重复添加的 `user` 不会再次保存。

### TreeSet

`TreeSet` 是可以自动排序的 `Set` 实现类，底层基于红黑树。它不允许重复元素，并且会按照自然顺序或自定义比较器进行排序。

`TreeSet` 的主要特点如下：

| 特点     | 说明                 |
| -------- | -------------------- |
| 底层结构 | 红黑树               |
| 是否重复 | 不允许重复           |
| 是否有序 | 有序                 |
| 排序方式 | 自然排序或自定义排序 |
| 适用场景 | 去重并排序           |

如果元素是 `Integer`、`Long`、`String` 等已经实现 `Comparable` 接口的类型，`TreeSet` 可以直接排序。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.Set;
import java.util.TreeSet;

/**
 * TreeSet 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TreeSetDemo {

    /**
     * 演示 TreeSet 自动排序
     *
     * @return 排序后的分数集合
     */
    public static Set<Integer> buildScores() {
        Set<Integer> scores = new TreeSet<>();
        scores.add(90);
        scores.add(75);
        scores.add(88);
        scores.add(90);
        scores.add(100);

        if (CollUtil.isNotEmpty(scores)) {
            System.out.println("分数数量：" + scores.size());
        }

        return scores;
    }

    public static void main(String[] args) {
        Set<Integer> scores = buildScores();
        System.out.println("分数集合：" + scores);
    }
}
```

输出结果会按照数字升序排列，重复的 `90` 只保留一份。

如果需要对对象排序，可以使用 `Comparator` 指定排序规则。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * TreeSet 对象排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TreeSetObjectDemo {

    /**
     * 构建按照价格排序的商品集合
     *
     * @return 商品集合
     */
    public static Set<Product> buildProducts() {
        Set<Product> products = new TreeSet<>(
                Comparator.comparing(Product::getPrice)
                        .thenComparing(Product::getName)
        );

        products.add(new Product("苹果", new BigDecimal("6.50")));
        products.add(new Product("香蕉", new BigDecimal("4.20")));
        products.add(new Product("橙子", new BigDecimal("5.80")));

        if (CollUtil.isNotEmpty(products)) {
            System.out.println("商品数量：" + products.size());
        }

        return products;
    }

    public static void main(String[] args) {
        Set<Product> products = buildProducts();
        products.forEach(System.out::println);
    }

    /**
     * 商品对象
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class Product {

        private String name;

        private BigDecimal price;

        public Product(String name, BigDecimal price) {
            this.name = name;
            this.price = price;
        }

        /**
         * 获取商品名称
         *
         * @return 商品名称
         */
        public String getName() {
            return name;
        }

        /**
         * 获取商品价格
         *
         * @return 商品价格
         */
        public BigDecimal getPrice() {
            return price;
        }

        @Override
        public String toString() {
            return "Product{" +
                    "name='" + name + '\'' +
                    ", price=" + price +
                    '}';
        }
    }
}
```

`TreeSet` 判断元素是否重复时，主要依据比较结果。如果比较器返回 `0`，`TreeSet` 会认为两个元素重复。因此定义比较规则时要特别注意，不要只比较一个可能重复的字段，除非业务上确实认为该字段相同就是同一个元素。

### Set 去重机制

`Set` 的去重机制取决于具体实现类。不同 `Set` 对重复元素的判断方式并不完全相同。

`HashSet` 和 `LinkedHashSet` 依赖 `hashCode()` 和 `equals()` 判断元素是否重复。

`TreeSet` 依赖自然排序的 `compareTo()` 或自定义排序的 `Comparator` 判断元素是否重复。

#### HashSet 的去重机制

`HashSet` 添加元素时，会先根据元素的 `hashCode()` 计算存储位置。如果哈希值不同，通常认为不是重复元素。如果哈希值相同，还会继续调用 `equals()` 判断两个元素是否真正相等。

判断流程可以简化理解为：

```text
先比较 hashCode()
如果 hashCode 不同，认为不是同一个元素
如果 hashCode 相同，再比较 equals()
如果 equals 返回 true，认为是重复元素
```

因此，使用自定义对象作为 `HashSet` 元素时，如果希望按照业务字段去重，必须正确重写 `hashCode()` 和 `equals()`。

下面示例按照用户 ID 对用户对象去重。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * HashSet 自定义对象去重示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashSetDistinctDemo {

    /**
     * 构建用户集合
     *
     * @return 去重后的用户集合
     */
    public static Set<UserInfo> buildUsers() {
        Set<UserInfo> users = new HashSet<>();
        users.add(new UserInfo(1001L, "张三"));
        users.add(new UserInfo(1002L, "李四"));
        users.add(new UserInfo(1001L, "张三-重复数据"));

        if (CollUtil.isNotEmpty(users)) {
            System.out.println("去重后的用户数量：" + users.size());
        }

        return users;
    }

    public static void main(String[] args) {
        Set<UserInfo> users = buildUsers();
        users.forEach(System.out::println);
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserInfo {

        private Long userId;

        private String userName;

        public UserInfo(Long userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        /**
         * 获取用户ID
         *
         * @return 用户ID
         */
        public Long getUserId() {
            return userId;
        }

        /**
         * 获取用户名称
         *
         * @return 用户名称
         */
        public String getUserName() {
            return userName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UserInfo userInfo)) {
                return false;
            }
            return Objects.equals(userId, userInfo.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userId=" + userId +
                    ", userName='" + userName + '\'' +
                    '}';
        }
    }
}
```

这个示例中，`equals()` 和 `hashCode()` 只使用 `userId` 判断是否相同。因此 `userId` 相同的两个对象会被认为是重复元素。

#### TreeSet 的去重机制

`TreeSet` 不依赖 `hashCode()` 和 `equals()` 判断重复，而是依赖比较结果。

如果比较器返回 `0`，`TreeSet` 会认为两个元素相同，只保留一个。

下面示例按照用户 ID 对用户对象排序并去重。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * TreeSet 自定义对象去重示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TreeSetDistinctDemo {

    /**
     * 构建按照用户ID排序并去重的集合
     *
     * @return 用户集合
     */
    public static Set<UserInfo> buildUsers() {
        Set<UserInfo> users = new TreeSet<>(Comparator.comparing(UserInfo::getUserId));

        users.add(new UserInfo(1003L, "王五"));
        users.add(new UserInfo(1001L, "张三"));
        users.add(new UserInfo(1002L, "李四"));
        users.add(new UserInfo(1001L, "张三-重复数据"));

        if (CollUtil.isNotEmpty(users)) {
            System.out.println("TreeSet 用户数量：" + users.size());
        }

        return users;
    }

    public static void main(String[] args) {
        Set<UserInfo> users = buildUsers();
        users.forEach(System.out::println);
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserInfo {

        private Long userId;

        private String userName;

        public UserInfo(Long userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        /**
         * 获取用户ID
         *
         * @return 用户ID
         */
        public Long getUserId() {
            return userId;
        }

        /**
         * 获取用户名称
         *
         * @return 用户名称
         */
        public String getUserName() {
            return userName;
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userId=" + userId +
                    ", userName='" + userName + '\'' +
                    '}';
        }
    }
}
```

这个示例中，`Comparator.comparing(UserInfo::getUserId)` 表示只按照 `userId` 比较。如果两个对象的 `userId` 相同，比较结果为 `0`，`TreeSet` 会认为它们是重复元素。

#### Set 去重注意事项

使用 `Set` 去重时，需要重点注意以下几点：

1. 使用 `HashSet` 保存自定义对象时，必须根据业务唯一字段重写 `equals()` 和 `hashCode()`。
2. 使用 `TreeSet` 保存自定义对象时，必须提供合理的排序规则。
3. `TreeSet` 中比较器返回 `0` 就表示重复，不一定会调用 `equals()`。
4. `HashSet` 不保证遍历顺序，如果需要保留插入顺序，应使用 `LinkedHashSet`。
5. `Set` 适合去重，但不适合通过下标访问元素。

在业务开发中，可以按以下规则选择：

| 需求               | 推荐集合        |
| ------------------ | --------------- |
| 只需要高效去重     | `HashSet`       |
| 去重后保留插入顺序 | `LinkedHashSet` |
| 去重后还需要排序   | `TreeSet`       |
| 需要下标访问       | `ArrayList`     |
| 需要键值映射       | `HashMap`       |



## Queue 集合

`Queue` 表示队列结构，主要用于按照特定顺序保存和取出元素。普通队列通常遵循先进先出原则，即先放入队列的元素先被取出。除了普通队列，Java 还提供了双端队列 `Deque` 和优先级队列 `PriorityQueue`，用于满足不同的数据处理场景。

队列在实际开发中常用于任务排队、异步任务缓冲、消息处理、广度优先搜索、限流队列、最近操作记录等场景。

### Queue 接口

`Queue` 是单端队列接口，通常用于先进先出场景。元素从队尾进入，从队首取出。

`Queue` 中常用方法分为两类：一类在操作失败时抛出异常，另一类在操作失败时返回特殊值。实际开发中更推荐使用返回特殊值的方法，代码更安全。

| 操作                 | 抛异常方法  | 返回特殊值方法 | 推荐       |
| -------------------- | ----------- | -------------- | ---------- |
| 添加元素             | `add(e)`    | `offer(e)`     | `offer(e)` |
| 删除并返回队首元素   | `remove()`  | `poll()`       | `poll()`   |
| 查看队首元素但不删除 | `element()` | `peek()`       | `peek()`   |

`Queue` 的典型使用方式如下：

```java
Queue<String> queue = new LinkedList<>();
queue.offer("任务A");
queue.offer("任务B");

String firstTask = queue.peek();
String task = queue.poll();
```

需要注意，`poll()` 在队列为空时返回 `null`，不会抛出异常；`remove()` 在队列为空时会抛出异常。因此业务代码中通常优先使用 `poll()`。

下面示例演示普通队列的先进先出处理逻辑。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Queue 接口使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class QueueInterfaceDemo {

    /**
     * 模拟任务队列处理
     */
    public static void processTaskQueue() {
        Queue<String> taskQueue = new LinkedList<>();
        taskQueue.offer("导入用户数据");
        taskQueue.offer("生成统计报表");
        taskQueue.offer("发送通知消息");

        if (CollUtil.isEmpty(taskQueue)) {
            System.out.println("任务队列为空");
            return;
        }

        System.out.println("当前队首任务：" + taskQueue.peek());

        while (CollUtil.isNotEmpty(taskQueue)) {
            String task = taskQueue.poll();
            System.out.println("正在处理任务：" + task);
        }
    }

    public static void main(String[] args) {
        processTaskQueue();
    }
}
```

这个示例中，任务按照添加顺序依次处理，符合普通队列先进先出的特点。

### Deque 接口

`Deque` 是双端队列接口，全称是 Double Ended Queue。它允许在队首和队尾两端添加、删除和获取元素。

`Deque` 既可以当作普通队列使用，也可以当作栈使用。

常用方法如下：

| 操作位置 | 添加            | 删除并返回    | 查看但不删除  |
| -------- | --------------- | ------------- | ------------- |
| 队首     | `offerFirst(e)` | `pollFirst()` | `peekFirst()` |
| 队尾     | `offerLast(e)`  | `pollLast()`  | `peekLast()`  |

当 `Deque` 作为普通队列使用时，通常从队尾添加，从队首取出。

```java
deque.offerLast("任务A");
deque.pollFirst();
```

当 `Deque` 作为栈使用时，通常从队首压入，从队首弹出。

```java
deque.offerFirst("页面A");
deque.pollFirst();
```

下面示例演示 `Deque` 同时作为队列和栈使用。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Deque 接口使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DequeInterfaceDemo {

    /**
     * 演示 Deque 作为普通队列使用
     */
    public static void useAsQueue() {
        Deque<String> queue = new ArrayDeque<>();
        queue.offerLast("任务A");
        queue.offerLast("任务B");
        queue.offerLast("任务C");

        while (CollUtil.isNotEmpty(queue)) {
            String task = queue.pollFirst();
            System.out.println("队列处理：" + task);
        }
    }

    /**
     * 演示 Deque 作为栈使用
     */
    public static void useAsStack() {
        Deque<String> stack = new ArrayDeque<>();
        stack.offerFirst("页面A");
        stack.offerFirst("页面B");
        stack.offerFirst("页面C");

        while (CollUtil.isNotEmpty(stack)) {
            String page = stack.pollFirst();
            System.out.println("栈弹出：" + page);
        }
    }

    public static void main(String[] args) {
        useAsQueue();
        useAsStack();
    }
}
```

`Deque` 在很多场景下可以替代旧的 `Stack` 类。现代 Java 开发中，如果需要栈结构，通常推荐使用 `ArrayDeque`，不推荐使用 `Stack`。

### PriorityQueue

`PriorityQueue` 是优先级队列。它不是按照元素插入顺序出队，而是按照元素优先级出队。

默认情况下，`PriorityQueue` 按自然顺序排序。对于数字来说，默认是小的元素优先出队；对于字符串来说，默认按照字典顺序排序。

`PriorityQueue` 的主要特点如下：

| 特点            | 说明                        |
| --------------- | --------------------------- |
| 底层结构        | 二叉堆                      |
| 出队规则        | 按优先级出队                |
| 是否允许重复    | 允许重复                    |
| 是否允许 `null` | 不允许                      |
| 是否线程安全    | 非线程安全                  |
| 典型场景        | 优先级任务、Top N、调度算法 |

下面示例演示数字优先级队列。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.PriorityQueue;
import java.util.Queue;

/**
 * PriorityQueue 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PriorityQueueDemo {

    /**
     * 演示数字按照自然顺序出队
     */
    public static void processScoreQueue() {
        Queue<Integer> scoreQueue = new PriorityQueue<>();
        scoreQueue.offer(90);
        scoreQueue.offer(70);
        scoreQueue.offer(100);
        scoreQueue.offer(85);

        while (CollUtil.isNotEmpty(scoreQueue)) {
            Integer score = scoreQueue.poll();
            System.out.println("当前分数：" + score);
        }
    }

    public static void main(String[] args) {
        processScoreQueue();
    }
}
```

输出时会按照 `70、85、90、100` 的顺序出队，而不是按照插入顺序出队。

如果需要自定义优先级，可以传入 `Comparator`。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * PriorityQueue 自定义优先级示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PriorityQueueTaskDemo {

    /**
     * 按照任务优先级处理任务，数值越小优先级越高
     */
    public static void processTaskByPriority() {
        Queue<TaskInfo> taskQueue = new PriorityQueue<>(Comparator.comparing(TaskInfo::getPriority));
        taskQueue.offer(new TaskInfo("普通数据同步", 3));
        taskQueue.offer(new TaskInfo("紧急告警通知", 1));
        taskQueue.offer(new TaskInfo("报表生成", 2));

        while (CollUtil.isNotEmpty(taskQueue)) {
            TaskInfo task = taskQueue.poll();
            System.out.println("处理任务：" + task);
        }
    }

    public static void main(String[] args) {
        processTaskByPriority();
    }

    /**
     * 任务信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class TaskInfo {

        private final String taskName;

        private final Integer priority;

        public TaskInfo(String taskName, Integer priority) {
            this.taskName = taskName;
            this.priority = priority;
        }

        /**
         * 获取任务优先级
         *
         * @return 任务优先级
         */
        public Integer getPriority() {
            return priority;
        }

        @Override
        public String toString() {
            return "TaskInfo{" +
                    "taskName='" + taskName + '\'' +
                    ", priority=" + priority +
                    '}';
        }
    }
}
```

`PriorityQueue` 只保证每次 `poll()` 取出的元素是当前优先级最高的元素，不保证整体遍历顺序完全有序。如果要获取完整排序结果，应持续调用 `poll()`，而不是直接遍历队列。

### ArrayDeque

`ArrayDeque` 是基于数组实现的双端队列，常用于替代 `LinkedList` 作为队列，也常用于替代 `Stack` 作为栈。

`ArrayDeque` 的主要特点如下：

| 特点            | 说明                      |
| --------------- | ------------------------- |
| 底层结构        | 可扩容数组                |
| 是否允许 `null` | 不允许                    |
| 是否线程安全    | 非线程安全                |
| 作为队列        | 性能通常优于 `LinkedList` |
| 作为栈          | 推荐替代 `Stack`          |
| 适用场景        | 队列、双端队列、栈        |

下面示例演示 `ArrayDeque` 的常见操作。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ArrayDeque 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ArrayDequeDemo {

    /**
     * 演示 ArrayDeque 作为双端队列使用
     */
    public static void processDeque() {
        Deque<String> deque = new ArrayDeque<>();
        deque.offerLast("普通任务A");
        deque.offerLast("普通任务B");
        deque.offerFirst("紧急任务");

        if (CollUtil.isNotEmpty(deque)) {
            System.out.println("队首元素：" + deque.peekFirst());
            System.out.println("队尾元素：" + deque.peekLast());
        }

        while (CollUtil.isNotEmpty(deque)) {
            String task = deque.pollFirst();
            System.out.println("处理任务：" + task);
        }
    }

    public static void main(String[] args) {
        processDeque();
    }
}
```

`ArrayDeque` 不允许插入 `null`，因为很多队列方法会用 `null` 表示空队列结果。例如 `poll()` 在队列为空时返回 `null`，如果允许保存 `null`，就会造成语义混乱。

## Map 集合

`Map` 是双列集合，用于保存键值对数据。每个元素由一个 `key` 和一个 `value` 组成。通过 `key` 可以快速查找对应的 `value`。

`Map` 常用于保存映射关系，例如用户 ID 到用户对象、商品编号到库存数量、配置名称到配置值、字典编码到字典名称等。

`Map` 的常用方法如下：

| 方法                          | 说明                   |
| ----------------------------- | ---------------------- |
| `put(K key, V value)`         | 添加或修改键值对       |
| `get(Object key)`             | 根据 key 获取 value    |
| `remove(Object key)`          | 根据 key 删除键值对    |
| `containsKey(Object key)`     | 判断是否包含指定 key   |
| `containsValue(Object value)` | 判断是否包含指定 value |
| `size()`                      | 获取键值对数量         |
| `isEmpty()`                   | 判断 Map 是否为空      |
| `keySet()`                    | 获取所有 key           |
| `values()`                    | 获取所有 value         |
| `entrySet()`                  | 获取所有键值对         |

### HashMap

`HashMap` 是 Java 中最常用的 `Map` 实现类，底层基于哈希表实现。它根据 key 的哈希值定位存储位置，因此查询、插入和删除效率通常较高。

`HashMap` 的主要特点如下：

| 特点             | 说明                                       |
| ---------------- | ------------------------------------------ |
| 底层结构         | 数组 + 链表 + 红黑树                       |
| key 是否可重复   | 不可重复                                   |
| value 是否可重复 | 可以重复                                   |
| 是否允许 `null`  | 允许一个 `null` key，允许多个 `null` value |
| 是否有序         | 不保证顺序                                 |
| 是否线程安全     | 非线程安全                                 |
| 适用场景         | 大多数普通键值映射场景                     |

下面示例演示 `HashMap` 的基本使用。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * HashMap 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashMapDemo {

    /**
     * 构建用户映射关系
     *
     * @return 用户ID与用户名称映射关系
     */
    public static Map<Long, String> buildUserMap() {
        Map<Long, String> userMap = new HashMap<>(16);
        userMap.put(1001L, "张三");
        userMap.put(1002L, "李四");
        userMap.put(1003L, "王五");

        userMap.put(1002L, "李四-已更新");

        if (MapUtil.isNotEmpty(userMap)) {
            System.out.println("用户数量：" + userMap.size());
            System.out.println("用户1002：" + userMap.get(1002L));
        }

        return userMap;
    }

    public static void main(String[] args) {
        Map<Long, String> userMap = buildUserMap();
        System.out.println("用户映射：" + userMap);
    }
}
```

当 `put()` 的 key 已经存在时，新的 value 会覆盖旧的 value。因此 `HashMap` 中 key 不能重复，但 value 可以重复。

### LinkedHashMap

`LinkedHashMap` 是 `HashMap` 的子类，它在哈希表的基础上维护了一条双向链表，因此可以保持元素的迭代顺序。

默认情况下，`LinkedHashMap` 按照插入顺序遍历。它也可以通过构造方法开启访问顺序，常用于实现简单的 LRU 缓存。

`LinkedHashMap` 的主要特点如下：

| 特点            | 说明                   |
| --------------- | ---------------------- |
| 底层结构        | 哈希表 + 双向链表      |
| 是否允许 `null` | 允许                   |
| 是否有序        | 保证插入顺序或访问顺序 |
| 是否线程安全    | 非线程安全             |
| 适用场景        | 需要保持顺序的键值映射 |

下面示例演示 `LinkedHashMap` 保持插入顺序。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LinkedHashMap 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LinkedHashMapDemo {

    /**
     * 构建有序菜单映射
     *
     * @return 菜单编码与菜单名称映射关系
     */
    public static Map<String, String> buildMenuMap() {
        Map<String, String> menuMap = new LinkedHashMap<>();
        menuMap.put("system", "系统管理");
        menuMap.put("user", "用户管理");
        menuMap.put("role", "角色管理");
        menuMap.put("permission", "权限管理");

        if (MapUtil.isNotEmpty(menuMap)) {
            menuMap.forEach((code, name) -> System.out.println(code + " -> " + name));
        }

        return menuMap;
    }

    public static void main(String[] args) {
        buildMenuMap();
    }
}
```

`LinkedHashMap` 适合既要使用 key 快速查找，又要保留插入顺序的场景，例如菜单、字段配置、表头配置、导出列配置等。

### TreeMap

`TreeMap` 是基于红黑树实现的 `Map`，它会按照 key 的自然顺序或自定义比较器进行排序。

`TreeMap` 的主要特点如下：

| 特点                    | 说明                        |
| ----------------------- | --------------------------- |
| 底层结构                | 红黑树                      |
| 是否排序                | 按 key 排序                 |
| key 是否允许为 `null`   | 通常不允许                  |
| value 是否允许为 `null` | 允许                        |
| 是否线程安全            | 非线程安全                  |
| 适用场景                | 需要按照 key 排序的映射关系 |

下面示例演示 `TreeMap` 按 key 排序。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.Map;
import java.util.TreeMap;

/**
 * TreeMap 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TreeMapDemo {

    /**
     * 构建按年份排序的销售额映射
     *
     * @return 年份与销售额映射关系
     */
    public static Map<Integer, Long> buildYearSalesMap() {
        Map<Integer, Long> salesMap = new TreeMap<>();
        salesMap.put(2024, 120000L);
        salesMap.put(2022, 90000L);
        salesMap.put(2023, 110000L);
        salesMap.put(2025, 150000L);

        if (MapUtil.isNotEmpty(salesMap)) {
            salesMap.forEach((year, amount) -> System.out.println(year + " -> " + amount));
        }

        return salesMap;
    }

    public static void main(String[] args) {
        buildYearSalesMap();
    }
}
```

输出结果会按照年份从小到大排列。

如果需要自定义排序，可以传入 `Comparator`。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * TreeMap 自定义排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TreeMapComparatorDemo {

    /**
     * 构建按年份倒序排列的销售额映射
     *
     * @return 年份与销售额映射关系
     */
    public static Map<Integer, Long> buildDescYearSalesMap() {
        Map<Integer, Long> salesMap = new TreeMap<>(Comparator.reverseOrder());
        salesMap.put(2024, 120000L);
        salesMap.put(2022, 90000L);
        salesMap.put(2023, 110000L);
        salesMap.put(2025, 150000L);

        if (MapUtil.isNotEmpty(salesMap)) {
            salesMap.forEach((year, amount) -> System.out.println(year + " -> " + amount));
        }

        return salesMap;
    }

    public static void main(String[] args) {
        buildDescYearSalesMap();
    }
}
```

`TreeMap` 的排序依据是 key，不是 value。如果需要按照 value 排序，通常需要将 `entrySet()` 转为 `List` 后再排序。

### Hashtable

`Hashtable` 是 Java 早期提供的线程安全 `Map` 实现类。它的方法大多使用 `synchronized` 修饰，因此可以保证单个方法级别的线程安全。

`Hashtable` 的主要特点如下：

| 特点                  | 说明               |
| --------------------- | ------------------ |
| 底层结构              | 哈希表             |
| 是否线程安全          | 是，方法级别同步   |
| 是否允许 `null` key   | 不允许             |
| 是否允许 `null` value | 不允许             |
| 性能                  | 通常低于 `HashMap` |
| 当前使用情况          | 新项目中较少使用   |

`Hashtable` 示例：

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.Hashtable;
import java.util.Map;

/**
 * Hashtable 使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashtableDemo {

    /**
     * 构建配置映射
     *
     * @return 配置映射
     */
    public static Map<String, String> buildConfigMap() {
        Map<String, String> configMap = new Hashtable<>();
        configMap.put("appName", "demo-service");
        configMap.put("env", "dev");
        configMap.put("timeout", "3000");

        if (MapUtil.isNotEmpty(configMap)) {
            System.out.println("配置数量：" + configMap.size());
        }

        return configMap;
    }

    public static void main(String[] args) {
        Map<String, String> configMap = buildConfigMap();
        System.out.println("配置映射：" + configMap);
    }
}
```

现代 Java 开发中，一般不推荐主动使用 `Hashtable`。如果是普通单线程或局部变量场景，优先使用 `HashMap`；如果是高并发场景，优先使用 `ConcurrentHashMap`。

## HashMap 核心原理

`HashMap` 是集合框架中最重要的实现类之一。理解 `HashMap` 的底层结构、添加流程、查询流程、扩容机制和冲突处理方式，有助于理解很多 Java 面试题和实际性能问题。

以下内容以 JDK 8 及之后的 `HashMap` 为主。

### 数据结构

JDK 8 中，`HashMap` 的底层结构可以概括为：

```text
数组 + 链表 + 红黑树
```

数组称为哈希桶数组。数组中的每个位置称为一个桶。每个 key 经过哈希计算后，会定位到数组中的某个桶位。

如果多个 key 定位到了同一个桶位，就发生了哈希冲突。冲突元素会以链表形式挂在同一个桶位上。当链表长度达到一定阈值，并且数组容量足够大时，链表会转换为红黑树，以提升查询效率。

核心结构可以简化理解为：

```text
HashMap
└── table 数组
    ├── bucket[0] -> Node
    ├── bucket[1] -> Node -> Node -> Node
    ├── bucket[2] -> TreeNode
    └── bucket[n] -> null
```

`HashMap` 中的节点大致包含以下信息：

```text
hash：key 的哈希值
key：键
value：值
next：下一个节点
```

JDK 8 中几个重要参数如下：

| 参数         | 默认值    | 说明                                     |
| ------------ | --------- | ---------------------------------------- |
| 默认初始容量 | `16`      | 第一次创建哈希桶数组时的默认容量         |
| 最大容量     | `1 << 30` | 最大容量                                 |
| 默认负载因子 | `0.75`    | 控制扩容时机                             |
| 树化阈值     | `8`       | 单个桶中链表长度达到该值时可能树化       |
| 反树化阈值   | `6`       | 红黑树节点减少到该值附近时可能退化为链表 |
| 最小树化容量 | `64`      | 数组容量至少达到该值才会树化             |

负载因子用于计算扩容阈值：

```text
扩容阈值 = 当前数组容量 * 负载因子
```

例如默认容量为 `16`，负载因子为 `0.75`，则扩容阈值为：

```text
16 * 0.75 = 12
```

当元素数量超过 12 时，`HashMap` 会触发扩容。

### put 流程

`put()` 用于向 `HashMap` 中添加或更新键值对。

整体流程可以概括为：

```text
1. 根据 key 计算 hash 值
2. 如果 table 数组为空，先初始化数组
3. 根据 hash 值计算桶下标
4. 如果桶位置为空，直接创建新节点
5. 如果桶位置不为空，说明发生哈希冲突
6. 判断 key 是否已经存在
7. 如果 key 已存在，覆盖旧 value
8. 如果 key 不存在，将新节点追加到链表或红黑树中
9. 如果链表长度达到树化条件，尝试转换为红黑树
10. 插入后元素数量超过阈值，触发扩容
```

桶下标通常通过以下方式计算：

```text
index = (table.length - 1) & hash
```

由于 `HashMap` 的数组长度通常是 2 的幂，这种位运算可以替代取模运算，效率更高。

下面示例演示 `put()` 的覆盖效果。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * HashMap put 流程示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashMapPutDemo {

    /**
     * 演示相同 key 会覆盖旧 value
     */
    public static void putAndUpdate() {
        Map<Long, String> userMap = new HashMap<>(16);
        userMap.put(1001L, "张三");
        userMap.put(1002L, "李四");
        userMap.put(1001L, "张三-更新后");

        if (MapUtil.isNotEmpty(userMap)) {
            System.out.println("用户数量：" + userMap.size());
            System.out.println("用户1001：" + userMap.get(1001L));
        }
    }

    public static void main(String[] args) {
        putAndUpdate();
    }
}
```

最终 `1001L` 对应的值会变成 `"张三-更新后"`，并且 `Map` 的元素数量不会因为覆盖而增加。

### get 流程

`get()` 用于根据 key 查找对应的 value。

整体流程可以概括为：

```text
1. 根据 key 计算 hash 值
2. 根据 hash 值计算桶下标
3. 定位到 table 数组中的桶位
4. 如果桶为空，返回 null
5. 如果桶中第一个节点 key 匹配，直接返回 value
6. 如果是链表，依次遍历链表查找
7. 如果是红黑树，按照树结构查找
8. 找到返回 value，找不到返回 null
```

需要注意，`get()` 返回 `null` 有两种可能：

```text
1. key 不存在
2. key 存在，但对应的 value 本身就是 null
```

如果要明确判断 key 是否存在，应使用 `containsKey()`。

```java
package io.github.atengk.collection;

import java.util.HashMap;
import java.util.Map;

/**
 * HashMap get 流程示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashMapGetDemo {

    /**
     * 演示 get 和 containsKey 的区别
     */
    public static void getValue() {
        Map<String, String> configMap = new HashMap<>(16);
        configMap.put("timeout", "3000");
        configMap.put("remark", null);

        String timeout = configMap.get("timeout");
        String remark = configMap.get("remark");
        String missing = configMap.get("missing");

        System.out.println("timeout：" + timeout);
        System.out.println("remark：" + remark);
        System.out.println("missing：" + missing);

        System.out.println("remark 是否存在：" + configMap.containsKey("remark"));
        System.out.println("missing 是否存在：" + configMap.containsKey("missing"));
    }

    public static void main(String[] args) {
        getValue();
    }
}
```

如果业务上允许 value 为 `null`，不能只通过 `get(key) == null` 判断 key 不存在。

### 扩容机制

`HashMap` 的数组容量不是无限的。当元素数量超过扩容阈值时，会触发扩容。

默认情况下：

```text
初始容量 = 16
负载因子 = 0.75
扩容阈值 = 16 * 0.75 = 12
```

当第 13 个元素加入时，会触发扩容。扩容后容量通常变为原来的 2 倍。

```text
16 -> 32 -> 64 -> 128
```

扩容时，`HashMap` 会创建新的数组，并将旧数组中的节点迁移到新数组中。JDK 8 对迁移过程做了优化，节点在新数组中的位置要么保持原下标，要么移动到 `原下标 + 旧容量` 的位置。

扩容的核心影响如下：

| 影响     | 说明                             |
| -------- | -------------------------------- |
| 性能开销 | 扩容需要重新分配数组并迁移节点   |
| 容量变化 | 通常扩大为原来的 2 倍            |
| 下标变化 | 部分元素会移动到新位置           |
| 触发条件 | 元素数量超过阈值                 |
| 优化建议 | 已知数据量时提前设置合理初始容量 |

如果预计要存储较多数据，可以在创建 `HashMap` 时设置初始容量，减少扩容次数。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * HashMap 初始容量示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashMapCapacityDemo {

    /**
     * 根据预计数据量创建 HashMap
     *
     * @return 用户映射关系
     */
    public static Map<Long, String> buildUserMapWithCapacity() {
        int expectedSize = 100;
        int initialCapacity = (int) (expectedSize / 0.75F) + 1;

        Map<Long, String> userMap = new HashMap<>(initialCapacity);
        for (long i = 1; i <= expectedSize; i++) {
            userMap.put(i, "用户" + i);
        }

        if (MapUtil.isNotEmpty(userMap)) {
            System.out.println("用户数量：" + userMap.size());
        }

        return userMap;
    }

    public static void main(String[] args) {
        buildUserMapWithCapacity();
    }
}
```

在实际开发中，如果明确知道数据规模，例如批量查询后需要将 1000 条数据转为 `Map`，可以提前指定容量，避免多次扩容。

### hash 冲突处理

哈希冲突是指不同的 key 经过哈希计算后定位到了同一个数组桶位。

产生冲突的原因包括：

```text
1. 不同 key 的 hash 值可能相同
2. hash 值不同，但经过数组下标计算后可能落到同一个桶位
3. 数组容量有限，而 key 的数量可能很多
```

JDK 8 中，`HashMap` 使用链表和红黑树处理哈希冲突。

冲突处理过程可以简化为：

```text
1. 桶为空：直接放入新节点
2. 桶不为空：判断 key 是否相同
3. key 相同：覆盖旧 value
4. key 不同：追加到链表或插入红黑树
5. 链表过长：在满足条件时转换为红黑树
```

链表转红黑树需要同时考虑两个条件：

```text
1. 当前桶中链表长度达到树化阈值，通常是 8
2. HashMap 数组容量至少达到 64
```

如果链表长度达到 8，但数组容量还小于 64，`HashMap` 通常会优先扩容，而不是立即树化。因为容量较小时，冲突可能是数组太小导致的，扩容后冲突可能自然减少。

下面示例通过自定义 key 模拟哈希冲突。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HashMap 哈希冲突示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashMapConflictDemo {

    /**
     * 模拟多个 key 拥有相同 hashCode 的场景
     */
    public static void simulateHashConflict() {
        Map<ConflictKey, String> dataMap = new HashMap<>(16);
        dataMap.put(new ConflictKey(1L, "A"), "数据A");
        dataMap.put(new ConflictKey(2L, "B"), "数据B");
        dataMap.put(new ConflictKey(3L, "C"), "数据C");

        if (MapUtil.isNotEmpty(dataMap)) {
            System.out.println("数据数量：" + dataMap.size());
        }

        String value = dataMap.get(new ConflictKey(2L, "B"));
        System.out.println("查询结果：" + value);
    }

    public static void main(String[] args) {
        simulateHashConflict();
    }

    /**
     * 用于模拟哈希冲突的 key
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class ConflictKey {

        private final Long id;

        private final String code;

        public ConflictKey(Long id, String code) {
            this.id = id;
            this.code = code;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ConflictKey other)) {
                return false;
            }
            return Objects.equals(id, other.id) && Objects.equals(code, other.code);
        }
    }
}
```

这个示例中，所有 `ConflictKey` 的 `hashCode()` 都返回 `1`，因此它们会尽量落到同一个桶位，形成哈希冲突。虽然发生冲突，只要 `equals()` 正确实现，`HashMap` 仍然可以正确区分不同 key。

实际开发中，自定义对象作为 `HashMap` 的 key 时，应重点遵守以下规则：

1. 重写 `equals()` 时，必须同步重写 `hashCode()`。
2. 相等对象必须返回相同的 `hashCode()`。
3. 不相等对象可以返回相同的 `hashCode()`，但会增加哈希冲突。
4. 作为 key 的字段尽量不要在放入 `HashMap` 后被修改。
5. key 的哈希分布越均匀，`HashMap` 性能越稳定。

整体来看，`HashMap` 的高效主要来自哈希定位。理想情况下，添加和查询的时间复杂度接近 `O(1)`；当冲突严重时，链表查询可能退化为 `O(n)`；JDK 8 引入红黑树后，极端冲突下的查询效率可以优化到 `O(log n)`。



## ConcurrentHashMap

`ConcurrentHashMap` 是 Java 并发包中提供的线程安全 `Map` 实现类，位于 `java.util.concurrent` 包下。它适合在多线程环境中保存共享键值数据，能够在保证线程安全的同时，尽量降低锁竞争带来的性能损耗。

相比 `HashMap`，`ConcurrentHashMap` 支持并发访问；相比 `Hashtable`，`ConcurrentHashMap` 的锁粒度更细，并发性能更好。

### ConcurrentHashMap 的使用场景

`ConcurrentHashMap` 适合多个线程同时读写同一个 `Map` 的场景。它常用于本地缓存、统计计数、任务状态表、在线用户表、接口限流数据、配置快照等业务中。

常见使用场景如下：

| 场景     | 说明                                |
| -------- | ----------------------------------- |
| 本地缓存 | 保存热点数据，多个线程可同时读取    |
| 状态记录 | 保存任务 ID 与任务状态的映射        |
| 统计计数 | 保存业务类型与访问次数、失败次数    |
| 在线用户 | 保存用户 ID 与会话信息              |
| 并发去重 | 多线程场景下判断某个 key 是否已处理 |
| 限流控制 | 保存接口 key 与访问记录             |

需要注意，`ConcurrentHashMap` 不允许 `null` key，也不允许 `null` value。这是为了避免在并发环境下无法区分“key 不存在”和“key 存在但 value 为 null”的问题。

下面示例演示 `ConcurrentHashMap` 的基础使用方式。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashMap 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConcurrentHashMapDemo {

    /**
     * 构建任务状态映射
     *
     * @return 任务状态映射
     */
    public static Map<String, String> buildTaskStatusMap() {
        Map<String, String> taskStatusMap = new ConcurrentHashMap<>();
        taskStatusMap.put("task-1001", "处理中");
        taskStatusMap.put("task-1002", "待执行");
        taskStatusMap.put("task-1003", "已完成");

        taskStatusMap.put("task-1002", "执行中");

        if (MapUtil.isNotEmpty(taskStatusMap)) {
            System.out.println("任务数量：" + taskStatusMap.size());
            System.out.println("task-1002 状态：" + taskStatusMap.get("task-1002"));
        }

        return taskStatusMap;
    }

    public static void main(String[] args) {
        Map<String, String> taskStatusMap = buildTaskStatusMap();
        System.out.println("任务状态映射：" + taskStatusMap);
    }
}
```

这个示例中，`ConcurrentHashMap` 可以安全地用于多个线程共享的任务状态映射。普通局部变量或单线程场景不一定需要使用它，直接使用 `HashMap` 即可。

### JDK 8 数据结构

JDK 8 中，`ConcurrentHashMap` 的底层结构与 `HashMap` 类似，都可以概括为：

```text
数组 + 链表 + 红黑树
```

但 `ConcurrentHashMap` 针对并发访问做了大量控制。它不再使用 JDK 7 中的 `Segment` 分段锁结构，而是采用更细粒度的桶级别锁，并配合 CAS 操作提升并发性能。

JDK 8 中的核心结构可以简化理解为：

```text
ConcurrentHashMap
└── table 数组
    ├── bucket[0] -> Node
    ├── bucket[1] -> Node -> Node
    ├── bucket[2] -> TreeBin
    └── bucket[n] -> null
```

核心组件如下：

| 组件             | 说明                                       |
| ---------------- | ------------------------------------------ |
| `Node`           | 普通链表节点，保存 hash、key、value、next  |
| `TreeNode`       | 红黑树节点                                 |
| `TreeBin`        | 红黑树根节点的包装对象，负责树结构并发控制 |
| `ForwardingNode` | 扩容迁移时的占位节点                       |
| `sizeCtl`        | 控制初始化、扩容状态和扩容阈值             |
| `table`          | 哈希桶数组                                 |
| `nextTable`      | 扩容时的新数组                             |

`ConcurrentHashMap` 的读操作通常不加锁，依赖 `volatile` 保证可见性；写操作在桶为空时优先使用 CAS 插入，在桶不为空时会对桶头节点加锁。

这使得它在高并发读、多线程写入场景下，比 `Hashtable` 的整表锁方式更高效。

### 并发写入机制

JDK 8 的 `ConcurrentHashMap` 写入主要依赖 CAS 和 `synchronized`。它不是对整个 Map 加锁，而是尽量只锁住发生冲突的桶位置。

`put()` 的简化流程如下：

```text
1. 判断 table 是否初始化，没有则初始化
2. 根据 key 计算 hash
3. 根据 hash 定位桶下标
4. 如果桶为空，使用 CAS 尝试插入新节点
5. 如果桶不为空，并且正在扩容，则协助扩容
6. 如果桶不为空，对桶头节点加 synchronized 锁
7. 在锁内判断链表或红黑树，执行插入或覆盖
8. 插入后判断是否需要树化
9. 更新元素数量，必要时触发扩容
```

桶为空时，多个线程会通过 CAS 竞争插入，成功的线程完成写入，失败的线程重新尝试。

桶不为空时，只锁当前桶的头节点，不影响其他桶的并发读写。因此，只要不同线程操作的 key 分布在不同桶上，就可以并发执行。

下面示例演示多线程写入 `ConcurrentHashMap`。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * ConcurrentHashMap 并发写入示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConcurrentHashMapWriteDemo {

    /**
     * 演示多个线程并发写入 ConcurrentHashMap
     *
     * @throws InterruptedException 线程等待被中断时抛出
     */
    public static void writeByMultiThread() throws InterruptedException {
        int threadCount = 5;
        int dataSizePerThread = 100;
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        Map<String, Integer> dataMap = new ConcurrentHashMap<>();

        for (int i = 0; i < threadCount; i++) {
            int threadIndex = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < dataSizePerThread; j++) {
                        String key = "thread-" + threadIndex + "-data-" + j;
                        dataMap.put(key, j);
                    }
                    System.out.println("线程写入完成：" + threadIndex);
                } finally {
                    countDownLatch.countDown();
                }
            });
            thread.start();
        }

        countDownLatch.await();

        if (MapUtil.isNotEmpty(dataMap)) {
            System.out.println("最终数据数量：" + dataMap.size());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        writeByMultiThread();
    }
}
```

该示例中，5 个线程同时写入同一个 `ConcurrentHashMap`。最终数据量应该是 `5 * 100 = 500`。如果使用普通 `HashMap` 并发写入，就可能出现数据丢失、结构异常或其他不可预期问题。

在并发更新数值时，不建议直接使用 `get()` 后再 `put()`，因为这两个操作组合起来不是原子操作。可以使用 `compute()`、`merge()` 或 `AtomicInteger` 等方式。

下面示例演示使用 `merge()` 做并发计数。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashMap 并发计数示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConcurrentHashMapCountDemo {

    /**
     * 使用 merge 方法统计接口访问次数
     *
     * @return 接口访问次数映射
     */
    public static Map<String, Integer> countApiAccess() {
        Map<String, Integer> accessCountMap = new ConcurrentHashMap<>();

        accessCountMap.merge("/api/user/list", 1, Integer::sum);
        accessCountMap.merge("/api/user/list", 1, Integer::sum);
        accessCountMap.merge("/api/order/page", 1, Integer::sum);

        if (MapUtil.isNotEmpty(accessCountMap)) {
            accessCountMap.forEach((api, count) -> System.out.println(api + " 访问次数：" + count));
        }

        return accessCountMap;
    }

    public static void main(String[] args) {
        countApiAccess();
    }
}
```

`merge()` 可以把“key 不存在时初始化”和“key 存在时累加”合并为一个操作，适合简单统计场景。

### 与 Hashtable 的区别

`ConcurrentHashMap` 和 `Hashtable` 都是线程安全的 `Map`，但它们的实现方式和性能差异明显。

| 对比项                | ConcurrentHashMap      | Hashtable               |
| --------------------- | ---------------------- | ----------------------- |
| 所属包                | `java.util.concurrent` | `java.util`             |
| 线程安全方式          | CAS + 桶级别锁         | 方法级别 `synchronized` |
| 锁粒度                | 较细                   | 较粗                    |
| 并发性能              | 较高                   | 较低                    |
| 是否允许 `null` key   | 不允许                 | 不允许                  |
| 是否允许 `null` value | 不允许                 | 不允许                  |
| 读操作                | 大多数情况下无锁       | 方法加锁                |
| 推荐程度              | 并发场景推荐           | 新项目较少使用          |

`Hashtable` 的主要问题是锁粒度太粗。它的大多数方法都使用 `synchronized` 修饰，多个线程访问时容易竞争同一把锁。

`ConcurrentHashMap` 的锁粒度更细，读操作通常不加锁，写操作通常只锁某个桶，因此更适合高并发环境。

选择建议如下：

| 场景                       | 推荐                 |
| -------------------------- | -------------------- |
| 单线程或局部变量使用       | `HashMap`            |
| 需要保持插入顺序           | `LinkedHashMap`      |
| 需要按 key 排序            | `TreeMap`            |
| 多线程并发读写             | `ConcurrentHashMap`  |
| 维护历史代码               | 可能遇到 `Hashtable` |
| 新项目主动选择线程安全 Map | `ConcurrentHashMap`  |

## 集合遍历

集合遍历是对集合中元素逐个访问的过程。Java 中常见遍历方式包括普通 `for` 循环、`Iterator`、增强 `foreach` 和 `Stream`。不同遍历方式适合不同场景，选择时要考虑是否需要下标、是否需要删除元素、是否需要函数式处理。

### for 循环

普通 `for` 循环通常用于遍历 `List`，尤其适合需要使用下标的场景。例如需要获取元素位置、相邻元素比较、分页截取、按索引修改元素等。

普通 `for` 循环示例：

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * for 循环遍历 List 示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ForEachByIndexDemo {

    /**
     * 使用普通 for 循环遍历用户列表
     */
    public static void eachByIndex() {
        List<String> userNames = new ArrayList<>();
        userNames.add("张三");
        userNames.add("李四");
        userNames.add("王五");

        if (CollUtil.isEmpty(userNames)) {
            System.out.println("用户列表为空");
            return;
        }

        for (int i = 0; i < userNames.size(); i++) {
            String userName = userNames.get(i);
            System.out.println("下标：" + i + "，用户：" + userName);
        }
    }

    public static void main(String[] args) {
        eachByIndex();
    }
}
```

普通 `for` 循环适合 `ArrayList`，因为 `ArrayList` 支持高效的随机访问。对于 `LinkedList`，频繁使用 `get(index)` 可能导致性能较差，因为它需要从链表头部或尾部遍历节点。

### Iterator

`Iterator` 是迭代器，适合统一遍历不同类型的集合。它最大的优势是可以在遍历过程中安全删除元素。

常用方法如下：

| 方法        | 说明                   |
| ----------- | ---------------------- |
| `hasNext()` | 判断是否还有下一个元素 |
| `next()`    | 获取下一个元素         |
| `remove()`  | 删除当前迭代到的元素   |

如果在增强 `for` 遍历中直接调用集合的 `remove()` 方法，可能触发 `ConcurrentModificationException`。需要遍历时删除元素，应使用 `Iterator` 的 `remove()` 方法。

下面示例演示使用 `Iterator` 删除无效用户。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator 遍历删除元素示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class IteratorRemoveDemo {

    /**
     * 使用 Iterator 安全删除空白用户名
     */
    public static void removeBlankName() {
        List<String> userNames = new ArrayList<>();
        userNames.add("张三");
        userNames.add("");
        userNames.add("李四");
        userNames.add(" ");
        userNames.add("王五");

        if (CollUtil.isEmpty(userNames)) {
            System.out.println("用户列表为空");
            return;
        }

        Iterator<String> iterator = userNames.iterator();
        while (iterator.hasNext()) {
            String userName = iterator.next();
            if (StrUtil.isBlank(userName)) {
                iterator.remove();
            }
        }

        System.out.println("清理后的用户列表：" + userNames);
    }

    public static void main(String[] args) {
        removeBlankName();
    }
}
```

`Iterator` 适合需要边遍历边删除的场景。如果只是读取元素，可以使用增强 `for` 或 `forEach()`，代码更简洁。

### foreach

`foreach` 通常指增强 `for` 循环，也可以泛指 `Collection` 的 `forEach()` 方法。它适合简单读取集合元素，不关心下标，也不需要在遍历时删除元素。

增强 `for` 语法如下：

```java
for (元素类型 变量名 : 集合或数组) {
    // 处理元素
}
```

下面示例演示增强 `for` 和 `forEach()` 两种写法。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * foreach 遍历集合示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ForeachDemo {

    /**
     * 演示增强 for 和 forEach 方法
     */
    public static void eachUserNames() {
        List<String> userNames = new ArrayList<>();
        userNames.add("张三");
        userNames.add("李四");
        userNames.add("王五");

        if (CollUtil.isEmpty(userNames)) {
            System.out.println("用户列表为空");
            return;
        }

        for (String userName : userNames) {
            System.out.println("增强 for 遍历：" + userName);
        }

        userNames.forEach(userName -> System.out.println("forEach 方法遍历：" + userName));
    }

    public static void main(String[] args) {
        eachUserNames();
    }
}
```

增强 `for` 底层通常也是基于迭代器实现的。它语法简单，但不适合在遍历过程中直接删除集合元素。

### Stream 遍历

`Stream` 是 Java 8 引入的流式处理 API，适合对集合进行过滤、映射、排序、分组、统计、收集等操作。

常见 Stream 操作如下：

| 方法           | 说明     |
| -------------- | -------- |
| `filter()`     | 过滤元素 |
| `map()`        | 转换元素 |
| `sorted()`     | 排序     |
| `distinct()`   | 去重     |
| `limit()`      | 限制数量 |
| `collect()`    | 收集结果 |
| `forEach()`    | 遍历处理 |
| `groupingBy()` | 分组     |
| `count()`      | 统计数量 |

下面示例演示使用 Stream 过滤、转换和收集用户名称。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stream 遍历集合示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StreamEachDemo {

    /**
     * 使用 Stream 处理用户名称
     *
     * @return 处理后的用户名称列表
     */
    public static List<String> handleUserNames() {
        List<String> userNames = new ArrayList<>();
        userNames.add(" 张三 ");
        userNames.add("");
        userNames.add(" 李四 ");
        userNames.add("王五");

        if (CollUtil.isEmpty(userNames)) {
            return new ArrayList<>();
        }

        List<String> result = userNames.stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .filter(name -> name.length() >= 2)
                .collect(Collectors.toList());

        System.out.println("处理后的用户列表：" + result);
        return result;
    }

    public static void main(String[] args) {
        handleUserNames();
    }
}
```

`Stream` 的优势是表达能力强，适合链式数据处理；缺点是调试成本相对普通循环更高。对于非常简单的遍历，普通 `for` 或增强 `for` 反而更直接。

遍历方式选择建议如下：

| 场景                   | 推荐方式     |
| ---------------------- | ------------ |
| 需要下标               | 普通 `for`   |
| 需要遍历时删除         | `Iterator`   |
| 只读遍历               | 增强 `for`   |
| 简单函数式处理         | `forEach()`  |
| 过滤、转换、分组、统计 | `Stream`     |
| 遍历 `Map`             | `entrySet()` |

遍历 `Map` 时，通常推荐遍历 `entrySet()`，这样可以同时拿到 key 和 value，效率也更直接。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Map 遍历示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MapEachDemo {

    /**
     * 使用 entrySet 遍历 Map
     */
    public static void eachMap() {
        Map<Long, String> userMap = new HashMap<>(16);
        userMap.put(1001L, "张三");
        userMap.put(1002L, "李四");
        userMap.put(1003L, "王五");

        if (MapUtil.isEmpty(userMap)) {
            System.out.println("用户映射为空");
            return;
        }

        for (Map.Entry<Long, String> entry : userMap.entrySet()) {
            System.out.println("用户ID：" + entry.getKey() + "，用户名称：" + entry.getValue());
        }
    }

    public static void main(String[] args) {
        eachMap();
    }
}
```

## 集合排序

集合排序用于按照指定规则重新排列元素。Java 中常见排序方式包括自然排序 `Comparable`、自定义排序 `Comparator`、`List` 排序，以及 `TreeSet`、`TreeMap` 的自动排序。

排序时要重点区分两个问题：谁提供排序规则，以及排序结果是否会改变原集合。

### Comparable

`Comparable` 是自然排序接口，由被排序对象自己实现。实现该接口后，对象就具备了默认排序规则。

接口方法如下：

```java
int compareTo(T o);
```

返回值含义如下：

| 返回值 | 含义                     |
| ------ | ------------------------ |
| 负数   | 当前对象排在参数对象前面 |
| 0      | 两个对象排序位置相同     |
| 正数   | 当前对象排在参数对象后面 |

`Comparable` 适合对象本身有明确默认排序规则的场景，例如学生按分数排序、商品按价格排序、版本号按编号排序等。

下面示例演示用户对象实现 `Comparable`，按照年龄升序排序。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comparable 自然排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ComparableDemo {

    /**
     * 演示 Comparable 排序
     */
    public static void sortByComparable() {
        List<UserInfo> users = new ArrayList<>();
        users.add(new UserInfo("张三", 28));
        users.add(new UserInfo("李四", 22));
        users.add(new UserInfo("王五", 35));

        if (CollUtil.isEmpty(users)) {
            System.out.println("用户列表为空");
            return;
        }

        Collections.sort(users);
        users.forEach(System.out::println);
    }

    public static void main(String[] args) {
        sortByComparable();
    }

    /**
     * 用户信息，默认按照年龄升序排序
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserInfo implements Comparable<UserInfo> {

        private final String userName;

        private final Integer age;

        public UserInfo(String userName, Integer age) {
            this.userName = userName;
            this.age = age;
        }

        /**
         * 按照年龄升序比较
         *
         * @param other 另一个用户
         * @return 比较结果
         */
        @Override
        public int compareTo(UserInfo other) {
            return this.age.compareTo(other.age);
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userName='" + userName + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
}
```

`Comparable` 的排序规则写在类内部，适合稳定、通用的默认排序。如果同一个类在不同业务场景下需要不同排序规则，则更适合使用 `Comparator`。

### Comparator

`Comparator` 是外部比较器，不要求被排序类自己实现排序接口。它适合临时排序、多规则排序、不同场景使用不同排序规则等情况。

常用写法如下：

```java
Comparator.comparing(UserInfo::getAge)
Comparator.comparing(UserInfo::getAge).reversed()
Comparator.comparing(UserInfo::getAge).thenComparing(UserInfo::getUserName)
```

下面示例演示使用 `Comparator` 按年龄升序、再按名称排序。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Comparator 自定义排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ComparatorDemo {

    /**
     * 使用 Comparator 对用户列表排序
     */
    public static void sortByComparator() {
        List<UserInfo> users = new ArrayList<>();
        users.add(new UserInfo("王五", 28));
        users.add(new UserInfo("张三", 22));
        users.add(new UserInfo("李四", 28));

        if (CollUtil.isEmpty(users)) {
            System.out.println("用户列表为空");
            return;
        }

        users.sort(
                Comparator.comparing(UserInfo::getAge)
                        .thenComparing(UserInfo::getUserName)
        );

        users.forEach(System.out::println);
    }

    public static void main(String[] args) {
        sortByComparator();
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserInfo {

        private final String userName;

        private final Integer age;

        public UserInfo(String userName, Integer age) {
            this.userName = userName;
            this.age = age;
        }

        /**
         * 获取用户名称
         *
         * @return 用户名称
         */
        public String getUserName() {
            return userName;
        }

        /**
         * 获取年龄
         *
         * @return 年龄
         */
        public Integer getAge() {
            return age;
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userName='" + userName + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
}
```

`Comparator` 比 `Comparable` 更灵活。实际业务开发中，如果排序规则来自接口参数、页面字段或临时业务需求，一般优先使用 `Comparator`。

### List 排序

`List` 排序常用两种方式：

```java
Collections.sort(list);
list.sort(comparator);
```

如果元素实现了 `Comparable`，可以直接使用 `Collections.sort(list)`。如果要指定排序规则，推荐使用 `list.sort(comparator)`。

常见排序场景如下：

| 场景         | 写法                                            |
| ------------ | ----------------------------------------------- |
| 数字升序     | `list.sort(Integer::compareTo)`                 |
| 数字降序     | `list.sort(Comparator.reverseOrder())`          |
| 字符串升序   | `list.sort(String::compareTo)`                  |
| 对象字段升序 | `Comparator.comparing(User::getAge)`            |
| 对象字段降序 | `Comparator.comparing(User::getAge).reversed()` |
| 多字段排序   | `thenComparing()`                               |

下面示例演示 `List` 的常见排序方式。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * List 排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ListSortDemo {

    /**
     * 演示基础类型排序
     */
    public static void sortNumbers() {
        List<Integer> numbers = new ArrayList<>();
        numbers.add(30);
        numbers.add(10);
        numbers.add(50);
        numbers.add(20);

        if (CollUtil.isEmpty(numbers)) {
            System.out.println("数字列表为空");
            return;
        }

        numbers.sort(Integer::compareTo);
        System.out.println("升序结果：" + numbers);

        numbers.sort(Comparator.reverseOrder());
        System.out.println("降序结果：" + numbers);
    }

    /**
     * 演示对象列表排序
     */
    public static void sortProducts() {
        List<ProductInfo> products = new ArrayList<>();
        products.add(new ProductInfo("苹果", new BigDecimal("6.50"), 100));
        products.add(new ProductInfo("香蕉", new BigDecimal("4.20"), 80));
        products.add(new ProductInfo("橙子", new BigDecimal("6.50"), 120));

        if (CollUtil.isEmpty(products)) {
            System.out.println("商品列表为空");
            return;
        }

        products.sort(
                Comparator.comparing(ProductInfo::getPrice)
                        .thenComparing(ProductInfo::getStock, Comparator.reverseOrder())
        );

        products.forEach(System.out::println);
    }

    public static void main(String[] args) {
        sortNumbers();
        sortProducts();
    }

    /**
     * 商品信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class ProductInfo {

        private final String productName;

        private final BigDecimal price;

        private final Integer stock;

        public ProductInfo(String productName, BigDecimal price, Integer stock) {
            this.productName = productName;
            this.price = price;
            this.stock = stock;
        }

        /**
         * 获取价格
         *
         * @return 价格
         */
        public BigDecimal getPrice() {
            return price;
        }

        /**
         * 获取库存
         *
         * @return 库存
         */
        public Integer getStock() {
            return stock;
        }

        @Override
        public String toString() {
            return "ProductInfo{" +
                    "productName='" + productName + '\'' +
                    ", price=" + price +
                    ", stock=" + stock +
                    '}';
        }
    }
}
```

这个示例中，商品先按照价格升序排序；如果价格相同，再按照库存降序排序。

需要注意，`list.sort()` 会直接修改原列表。如果不希望改变原列表，可以先复制一份再排序。

```java
List<ProductInfo> sortedProducts = new ArrayList<>(products);
sortedProducts.sort(Comparator.comparing(ProductInfo::getPrice));
```

### TreeSet 与 TreeMap 排序

`TreeSet` 和 `TreeMap` 都是基于红黑树实现的有序集合。

`TreeSet` 对元素本身排序，`TreeMap` 对 key 排序。

| 集合      | 排序对象 | 说明                       |
| --------- | -------- | -------------------------- |
| `TreeSet` | 元素     | 元素按自然顺序或比较器排序 |
| `TreeMap` | key      | key 按自然顺序或比较器排序 |

`TreeSet` 示例：

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * TreeSet 排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TreeSetSortDemo {

    /**
     * 使用 TreeSet 按分数降序排序
     *
     * @return 分数集合
     */
    public static Set<Integer> sortScores() {
        Set<Integer> scores = new TreeSet<>(Comparator.reverseOrder());
        scores.add(90);
        scores.add(75);
        scores.add(100);
        scores.add(88);
        scores.add(90);

        if (CollUtil.isNotEmpty(scores)) {
            System.out.println("排序后的分数：" + scores);
        }

        return scores;
    }

    public static void main(String[] args) {
        sortScores();
    }
}
```

`TreeSet` 不允许重复元素。这里重复添加的 `90` 最终只会保留一个。

`TreeMap` 示例：

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * TreeMap 排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TreeMapSortDemo {

    /**
     * 使用 TreeMap 按年份倒序排序
     *
     * @return 年份与销售额映射
     */
    public static Map<Integer, Long> sortSalesByYearDesc() {
        Map<Integer, Long> salesMap = new TreeMap<>(Comparator.reverseOrder());
        salesMap.put(2024, 120000L);
        salesMap.put(2022, 90000L);
        salesMap.put(2023, 110000L);
        salesMap.put(2025, 150000L);

        if (MapUtil.isNotEmpty(salesMap)) {
            salesMap.forEach((year, sales) -> System.out.println(year + " -> " + sales));
        }

        return salesMap;
    }

    public static void main(String[] args) {
        sortSalesByYearDesc();
    }
}
```

`TreeMap` 排序的是 key，不是 value。如果需要按照 value 排序，可以将 `entrySet()` 转为 `List` 后排序。

下面示例演示按照销售额 value 排序。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Map 按 value 排序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MapValueSortDemo {

    /**
     * 按销售额降序排序
     *
     * @return 排序后的键值对列表
     */
    public static List<Map.Entry<Integer, Long>> sortByValueDesc() {
        Map<Integer, Long> salesMap = new TreeMap<>();
        salesMap.put(2024, 120000L);
        salesMap.put(2022, 90000L);
        salesMap.put(2023, 110000L);
        salesMap.put(2025, 150000L);

        if (MapUtil.isEmpty(salesMap)) {
            return new ArrayList<>();
        }

        List<Map.Entry<Integer, Long>> entries = new ArrayList<>(salesMap.entrySet());
        entries.sort(Map.Entry.<Integer, Long>comparingByValue().reversed());

        if (CollUtil.isNotEmpty(entries)) {
            entries.forEach(entry -> System.out.println(entry.getKey() + " -> " + entry.getValue()));
        }

        return entries;
    }

    public static void main(String[] args) {
        sortByValueDesc();
    }
}
```

排序选择建议如下：

| 需求                 | 推荐方式                      |
| -------------------- | ----------------------------- |
| 类本身有固定排序规则 | `Comparable`                  |
| 临时指定排序规则     | `Comparator`                  |
| 对列表排序           | `list.sort()`                 |
| 去重并排序元素       | `TreeSet`                     |
| 按 key 自动排序      | `TreeMap`                     |
| 按 Map 的 value 排序 | `entrySet()` 转 `List` 后排序 |

实际开发中，`Comparator` 使用频率更高，因为它可以在不同业务场景下定义不同排序规则，不会把某一种排序逻辑固定到实体类内部。



## 集合工具类

集合工具类用于简化集合、数组、不可变集合等常见操作。Java 标准库中最常用的是 `Collections` 和 `Arrays`，实际后端开发中也经常配合 Hutool 的 `CollUtil`、`ArrayUtil`、`MapUtil` 等工具类提升代码可读性。

### Collections 工具类

`Collections` 是 Java 提供的集合工具类，位于 `java.util` 包下。它主要用于操作 `Collection` 体系中的集合，例如 `List`、`Set` 等。

`Collections` 常用能力包括排序、反转、打乱、查找最大值最小值、填充、复制、线程安全包装、不可变包装等。

常用方法如下：

| 方法                                           | 说明                   |
| ---------------------------------------------- | ---------------------- |
| `sort(List<T> list)`                           | 对 List 按自然顺序排序 |
| `sort(List<T> list, Comparator<? super T> c)`  | 按指定比较器排序       |
| `reverse(List<?> list)`                        | 反转 List              |
| `shuffle(List<?> list)`                        | 随机打乱 List          |
| `max(Collection<?> coll)`                      | 获取最大值             |
| `min(Collection<?> coll)`                      | 获取最小值             |
| `frequency(Collection<?> c, Object o)`         | 统计元素出现次数       |
| `replaceAll(List<T> list, T oldVal, T newVal)` | 替换元素               |
| `synchronizedList(List<T> list)`               | 包装为线程安全 List    |
| `unmodifiableList(List<? extends T> list)`     | 包装为不可修改 List    |

下面示例演示 `Collections` 的排序、反转、统计和不可变包装。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Collections 工具类使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CollectionsDemo {

    /**
     * 演示 Collections 常见操作
     */
    public static void useCollections() {
        List<Integer> numbers = new ArrayList<>();
        numbers.add(30);
        numbers.add(10);
        numbers.add(50);
        numbers.add(20);
        numbers.add(20);

        if (CollUtil.isEmpty(numbers)) {
            System.out.println("数字列表为空");
            return;
        }

        Collections.sort(numbers);
        System.out.println("升序排序：" + numbers);

        numbers.sort(Comparator.reverseOrder());
        System.out.println("降序排序：" + numbers);

        Collections.reverse(numbers);
        System.out.println("反转结果：" + numbers);

        int count = Collections.frequency(numbers, 20);
        System.out.println("数字 20 出现次数：" + count);

        Integer max = Collections.max(numbers);
        Integer min = Collections.min(numbers);
        System.out.println("最大值：" + max + "，最小值：" + min);

        List<Integer> readonlyNumbers = Collections.unmodifiableList(numbers);
        System.out.println("只读列表：" + readonlyNumbers);
    }

    public static void main(String[] args) {
        useCollections();
    }
}
```

需要注意，`Collections.unmodifiableList()` 返回的是只读视图，不是深拷贝。原始集合如果被修改，只读视图中看到的数据也会变化。

```java
List<String> source = new ArrayList<>();
source.add("A");

List<String> readonlyList = Collections.unmodifiableList(source);

source.add("B");

System.out.println(readonlyList); // [A, B]
```

如果希望得到真正独立的不可变副本，可以先复制，再进行不可变包装，或者使用 `List.copyOf()`。

```java
List<String> readonlyList = List.copyOf(source);
```

`Collections.synchronizedList()` 可以把普通 `List` 包装为线程安全集合，但它只是对方法调用加同步锁。如果需要遍历，仍然建议手动加锁。

```java
List<String> synchronizedList = Collections.synchronizedList(new ArrayList<>());

synchronized (synchronizedList) {
    for (String item : synchronizedList) {
        System.out.println(item);
    }
}
```

在高并发读多写少的场景中，通常可以考虑 `CopyOnWriteArrayList`；在普通局部变量或单线程场景中，不需要使用同步集合。

### Arrays 工具类

`Arrays` 是 Java 提供的数组工具类，位于 `java.util` 包下。它主要用于操作数组，例如排序、查找、填充、比较、转字符串、数组转集合等。

常用方法如下：

| 方法                           | 说明                 |
| ------------------------------ | -------------------- |
| `toString(array)`              | 将一维数组转为字符串 |
| `deepToString(array)`          | 将多维数组转为字符串 |
| `sort(array)`                  | 数组排序             |
| `binarySearch(array, key)`     | 二分查找             |
| `copyOf(array, newLength)`     | 复制数组             |
| `copyOfRange(array, from, to)` | 复制指定范围         |
| `fill(array, value)`           | 填充数组             |
| `equals(array1, array2)`       | 比较数组内容         |
| `asList(array)`                | 数组转 List          |

下面示例演示 `Arrays` 的常见操作，并配合 Hutool 的 `ArrayUtil` 做数组判空。

```java
package io.github.atengk.collection;

import cn.hutool.core.util.ArrayUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Arrays 工具类使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ArraysDemo {

    /**
     * 演示 Arrays 常见操作
     */
    public static void useArrays() {
        int[] numbers = {30, 10, 50, 20};

        if (ArrayUtil.isEmpty(numbers)) {
            System.out.println("数组为空");
            return;
        }

        Arrays.sort(numbers);
        System.out.println("排序结果：" + Arrays.toString(numbers));

        int index = Arrays.binarySearch(numbers, 20);
        System.out.println("数字 20 的下标：" + index);

        int[] copiedNumbers = Arrays.copyOf(numbers, 6);
        System.out.println("复制扩容结果：" + Arrays.toString(copiedNumbers));

        Arrays.fill(copiedNumbers, 4, 6, 100);
        System.out.println("填充结果：" + Arrays.toString(copiedNumbers));

        String[] names = {"张三", "李四", "王五"};
        List<String> nameList = Arrays.asList(names);
        System.out.println("数组转 List：" + nameList);
    }

    public static void main(String[] args) {
        useArrays();
    }
}
```

`Arrays.asList()` 有一个常见陷阱：它返回的是固定长度的 List，不能执行 `add()` 或 `remove()`，否则会抛出 `UnsupportedOperationException`。

```java
String[] names = {"张三", "李四"};
List<String> nameList = Arrays.asList(names);

// 可以修改已有位置的元素
nameList.set(0, "王五");

// 不可以增加或删除元素
// nameList.add("赵六");     // UnsupportedOperationException
// nameList.remove("李四");  // UnsupportedOperationException
```

如果需要得到可以增删的 `ArrayList`，应再包装一层。

```java
List<String> nameList = new ArrayList<>(Arrays.asList(names));
```

JDK 8 中经常使用 `Arrays.asList()`，但在 JDK 9 及之后，如果是手动创建少量不可变元素，通常可以使用 `List.of()`、`Set.of()`、`Map.of()`。

### 不可变集合

不可变集合是创建后不能再修改的集合。不可变集合适合保存固定配置、常量数据、白名单、状态枚举映射、接口字段定义等内容。

不可变集合的优势如下：

| 优势           | 说明                               |
| -------------- | ---------------------------------- |
| 防止误修改     | 创建后不能添加、删除、替换元素     |
| 表达意图清晰   | 表示这份数据不应该被业务代码修改   |
| 线程共享更安全 | 不存在并发修改问题                 |
| 适合常量配置   | 适合保存固定枚举、状态码、字段名等 |

JDK 9 之后可以使用 `List.of()`、`Set.of()`、`Map.of()` 创建不可变集合。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 不可变集合使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ImmutableCollectionDemo {

    /**
     * 演示 JDK 不可变集合
     */
    public static void useImmutableCollection() {
        List<String> roleCodes = List.of("admin", "manager", "user");
        Set<String> allowStatusSet = Set.of("ENABLE", "DISABLE");
        Map<Integer, String> orderStatusMap = Map.of(
                1, "待支付",
                2, "已支付",
                3, "已取消"
        );

        if (CollUtil.isNotEmpty(roleCodes)) {
            System.out.println("角色编码：" + roleCodes);
        }

        if (CollUtil.isNotEmpty(allowStatusSet)) {
            System.out.println("允许状态：" + allowStatusSet);
        }

        if (MapUtil.isNotEmpty(orderStatusMap)) {
            System.out.println("订单状态：" + orderStatusMap);
        }

        // roleCodes.add("guest"); // UnsupportedOperationException
    }

    public static void main(String[] args) {
        useImmutableCollection();
    }
}
```

不可变集合需要注意以下几点：

1. `List.of()`、`Set.of()`、`Map.of()` 创建的集合不能修改。
2. `Set.of()` 不能包含重复元素。
3. `List.of()`、`Set.of()`、`Map.of()` 都不允许 `null` 元素。
4. `Map.of()` 最多适合少量键值对，键值对较多时可以使用 `Map.ofEntries()`。
5. 不可变集合只限制集合结构不可变，不代表元素对象内部状态一定不可变。

例如：

```java
List<UserInfo> users = List.of(new UserInfo("张三"));
```

这里不能向 `users` 添加新元素，但如果 `UserInfo` 本身是可变对象，仍然可能修改对象内部字段。因此真正意义上的不可变，还需要元素对象本身也是不可变的。

如果是 JDK 8，可以使用 `Collections.unmodifiableList()`、`Collections.unmodifiableSet()`、`Collections.unmodifiableMap()` 创建不可修改视图。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JDK 8 不可修改集合示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UnmodifiableCollectionDemo {

    /**
     * 演示 JDK 8 不可修改 List
     *
     * @return 不可修改的用户列表
     */
    public static List<String> buildReadonlyUserNames() {
        List<String> userNames = new ArrayList<>();
        userNames.add("张三");
        userNames.add("李四");
        userNames.add("王五");

        if (CollUtil.isEmpty(userNames)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(new ArrayList<>(userNames));
    }

    public static void main(String[] args) {
        List<String> readonlyUserNames = buildReadonlyUserNames();
        System.out.println("只读用户列表：" + readonlyUserNames);
    }
}
```

这里先使用 `new ArrayList<>(userNames)` 创建副本，再使用 `Collections.unmodifiableList()` 包装，可以避免原集合后续修改影响只读结果。

## 常见问题

集合框架相关问题在 Java 面试和实际开发中都非常常见。下面围绕 `ArrayList`、`LinkedList`、`HashMap`、`Hashtable`、`ConcurrentHashMap`、`HashSet`、`fail-fast` 和 `fail-safe` 做集中说明。

### ArrayList 与 LinkedList 的区别

`ArrayList` 和 `LinkedList` 都实现了 `List` 接口，都可以保存有序、可重复的数据，但底层结构完全不同。

| 对比项       | ArrayList                  | LinkedList                   |
| ------------ | -------------------------- | ---------------------------- |
| 底层结构     | 动态数组                   | 双向链表                     |
| 随机访问     | 快，按下标访问效率高       | 慢，需要遍历链表             |
| 头尾增删     | 中间插入删除需要移动元素   | 头尾增删效率较高             |
| 内存占用     | 相对较少                   | 每个节点需要额外保存前后指针 |
| 是否线程安全 | 非线程安全                 | 非线程安全                   |
| 典型场景     | 查询多、遍历多、按下标访问 | 队列、双端队列、头尾操作较多 |

选择建议如下：

```text
查询多、随机访问多：优先 ArrayList
头尾插入删除多：可以考虑 LinkedList
需要栈或队列：优先 ArrayDeque，而不是 LinkedList
绝大多数普通业务列表：优先 ArrayList
```

实际开发中，`ArrayList` 的使用频率远高于 `LinkedList`。即使存在插入删除操作，只要不是在大数据量中频繁对中间位置操作，`ArrayList` 通常仍然是更常用的选择。

下面示例演示二者的使用差异。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * ArrayList 与 LinkedList 对比示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ArrayListLinkedListCompareDemo {

    /**
     * 演示 ArrayList 按下标访问
     */
    public static void useArrayList() {
        List<String> userNames = new ArrayList<>();
        userNames.add("张三");
        userNames.add("李四");
        userNames.add("王五");

        if (CollUtil.isNotEmpty(userNames)) {
            System.out.println("ArrayList 第一个元素：" + userNames.get(0));
        }
    }

    /**
     * 演示 LinkedList 头尾操作
     */
    public static void useLinkedList() {
        LinkedList<String> taskQueue = new LinkedList<>();
        taskQueue.addLast("普通任务");
        taskQueue.addFirst("紧急任务");

        while (CollUtil.isNotEmpty(taskQueue)) {
            System.out.println("LinkedList 处理任务：" + taskQueue.removeFirst());
        }
    }

    public static void main(String[] args) {
        useArrayList();
        useLinkedList();
    }
}
```

总结一句话：`ArrayList` 适合查，`LinkedList` 适合头尾增删，但普通业务开发中优先考虑 `ArrayList`。

### HashMap 与 Hashtable 的区别

`HashMap` 和 `Hashtable` 都是键值对集合，但它们的设计时代、线程安全策略和使用建议不同。

| 对比项       | HashMap          | Hashtable             |
| ------------ | ---------------- | --------------------- |
| 出现时间     | JDK 1.2 集合框架 | JDK 1.0 早期类        |
| 线程安全     | 非线程安全       | 线程安全              |
| 锁机制       | 不加锁           | 方法级 `synchronized` |
| `null` key   | 允许一个         | 不允许                |
| `null` value | 允许多个         | 不允许                |
| 性能         | 单线程下较高     | 因同步开销较低        |
| 推荐程度     | 普通场景推荐     | 新项目较少主动使用    |

`HashMap` 适合普通单线程或方法内部局部变量场景。`Hashtable` 虽然线程安全，但锁粒度粗，并发性能较差，新项目中通常不推荐主动使用。

示例区别如下：

```java
package io.github.atengk.collection;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * HashMap 与 Hashtable 对比示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashMapHashtableCompareDemo {

    /**
     * 演示 null 支持差异
     */
    public static void compareNullSupport() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put(null, "空 key");
        hashMap.put("remark", null);
        System.out.println("HashMap：" + hashMap);

        Map<String, String> hashtable = new Hashtable<>();
        hashtable.put("name", "demo");

        // 以下代码会抛出 NullPointerException
        // hashtable.put(null, "空 key");
        // hashtable.put("remark", null);

        System.out.println("Hashtable：" + hashtable);
    }

    public static void main(String[] args) {
        compareNullSupport();
    }
}
```

选择建议如下：

```text
普通键值映射：HashMap
需要保持插入顺序：LinkedHashMap
需要按 key 排序：TreeMap
并发读写场景：ConcurrentHashMap
历史代码维护：可能遇到 Hashtable
```

### HashMap 与 ConcurrentHashMap 的区别

`HashMap` 和 `ConcurrentHashMap` 都是常用的键值对集合，但它们面向的场景不同。

`HashMap` 适合非并发场景，`ConcurrentHashMap` 适合多线程并发读写场景。

| 对比项       | HashMap              | ConcurrentHashMap          |
| ------------ | -------------------- | -------------------------- |
| 所属包       | `java.util`          | `java.util.concurrent`     |
| 线程安全     | 非线程安全           | 线程安全                   |
| 底层结构     | 数组 + 链表 + 红黑树 | 数组 + 链表 + 红黑树       |
| 并发控制     | 无                   | CAS + 局部 synchronized    |
| `null` key   | 允许一个             | 不允许                     |
| `null` value | 允许多个             | 不允许                     |
| 读性能       | 单线程快             | 并发环境稳定               |
| 使用场景     | 普通映射、局部变量   | 共享缓存、状态表、并发统计 |

需要注意，`ConcurrentHashMap` 的单个方法是线程安全的，但组合操作不一定自动具备业务原子性。

例如下面写法不是严格安全的：

```java
Integer count = map.get("A");
map.put("A", count + 1);
```

因为 `get()` 和 `put()` 是两个独立操作。并发计数时，可以使用 `merge()`。

```java
package io.github.atengk.collection;

import cn.hutool.core.map.MapUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HashMap 与 ConcurrentHashMap 使用场景示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashMapConcurrentHashMapCompareDemo {

    /**
     * 使用 ConcurrentHashMap 进行并发友好的计数
     */
    public static void countByConcurrentHashMap() {
        Map<String, Integer> accessCountMap = new ConcurrentHashMap<>();

        accessCountMap.merge("/api/user/list", 1, Integer::sum);
        accessCountMap.merge("/api/user/list", 1, Integer::sum);
        accessCountMap.merge("/api/order/page", 1, Integer::sum);

        if (MapUtil.isNotEmpty(accessCountMap)) {
            accessCountMap.forEach((api, count) -> System.out.println(api + " -> " + count));
        }
    }

    public static void main(String[] args) {
        countByConcurrentHashMap();
    }
}
```

选择建议如下：

```text
方法内部临时 Map：HashMap
类成员变量但没有并发访问：HashMap
多个线程共享并读写：ConcurrentHashMap
高并发计数：ConcurrentHashMap + LongAdder 或 merge
```

### HashSet 如何保证元素不重复

`HashSet` 底层基于 `HashMap` 实现。向 `HashSet` 添加元素时，元素会作为 `HashMap` 的 key 保存，而 value 使用一个固定的占位对象。

可以简化理解为：

```text
HashSet.add(element)
底层约等于：
HashMap.put(element, PRESENT)
```

由于 `HashMap` 的 key 不能重复，所以 `HashSet` 的元素也不能重复。

`HashSet` 判断重复主要依赖两个方法：

```text
hashCode()
equals()
```

判断流程如下：

```text
1. 先计算元素的 hashCode
2. 根据 hash 值定位桶位置
3. 如果桶位置没有元素，直接添加
4. 如果桶位置已有元素，继续调用 equals 判断
5. equals 返回 true，认为重复，不添加
6. equals 返回 false，认为不是重复元素，继续保存
```

因此，自定义对象放入 `HashSet` 时，如果要按照业务字段去重，必须重写 `equals()` 和 `hashCode()`。

下面示例演示按照用户 ID 去重。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * HashSet 去重机制示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class HashSetDistinctQuestionDemo {

    /**
     * 演示 HashSet 按用户ID去重
     */
    public static void distinctByUserId() {
        Set<UserInfo> users = new HashSet<>();
        users.add(new UserInfo(1001L, "张三"));
        users.add(new UserInfo(1002L, "李四"));
        users.add(new UserInfo(1001L, "张三-重复"));

        if (CollUtil.isNotEmpty(users)) {
            System.out.println("去重后用户数量：" + users.size());
            users.forEach(System.out::println);
        }
    }

    public static void main(String[] args) {
        distinctByUserId();
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserInfo {

        private final Long userId;

        private final String userName;

        public UserInfo(Long userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        /**
         * 判断用户是否相同
         *
         * @param obj 比较对象
         * @return 是否相同
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof UserInfo other)) {
                return false;
            }
            return Objects.equals(userId, other.userId);
        }

        /**
         * 计算哈希值
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userId=" + userId +
                    ", userName='" + userName + '\'' +
                    '}';
        }
    }
}
```

这个示例中，两个 `UserInfo` 对象的 `userId` 相同，因此会被认为是同一个元素，最终只保留一个。

需要注意：

1. `equals()` 判断相等的对象，`hashCode()` 必须相同。
2. `hashCode()` 相同的对象，`equals()` 不一定相等。
3. 作为 `HashSet` 元素的对象，参与去重的字段尽量不要在放入集合后修改。
4. 如果没有重写 `equals()` 和 `hashCode()`，默认会按对象地址判断，业务字段相同也可能不会去重。

### fail-fast 与 fail-safe 的区别

`fail-fast` 和 `fail-safe` 都和集合遍历时的并发修改有关。

`fail-fast` 表示快速失败机制。当一个线程在遍历集合时，另一个线程或当前线程通过集合本身的方法修改了集合结构，迭代器会尽快抛出 `ConcurrentModificationException`，提醒开发者集合发生了非预期修改。

常见的 `ArrayList`、`HashMap`、`HashSet` 的迭代器通常具有 fail-fast 特性。

示例：

```java
package io.github.atengk.collection;

import java.util.ArrayList;
import java.util.List;

/**
 * fail-fast 示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class FailFastDemo {

    /**
     * 演示增强 for 中直接删除元素触发 fail-fast
     */
    public static void triggerFailFast() {
        List<String> userNames = new ArrayList<>();
        userNames.add("张三");
        userNames.add("李四");
        userNames.add("王五");

        for (String userName : userNames) {
            if ("李四".equals(userName)) {
                userNames.remove(userName);
            }
        }
    }

    public static void main(String[] args) {
        triggerFailFast();
    }
}
```

这段代码可能抛出：

```text
java.util.ConcurrentModificationException
```

正确做法是使用 `Iterator.remove()`。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator 安全删除示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class IteratorSafeRemoveDemo {

    /**
     * 使用 Iterator 删除元素，避免 fail-fast 问题
     */
    public static void removeByIterator() {
        List<String> userNames = new ArrayList<>();
        userNames.add("张三");
        userNames.add("李四");
        userNames.add("王五");

        if (CollUtil.isEmpty(userNames)) {
            return;
        }

        Iterator<String> iterator = userNames.iterator();
        while (iterator.hasNext()) {
            String userName = iterator.next();
            if ("李四".equals(userName)) {
                iterator.remove();
            }
        }

        System.out.println("删除后的用户列表：" + userNames);
    }

    public static void main(String[] args) {
        removeByIterator();
    }
}
```

`fail-safe` 通常用于描述遍历时不会因为集合结构被修改而立即抛出 `ConcurrentModificationException` 的机制。严格来说，Java 官方文档中更常用的说法是弱一致性迭代器或快照迭代器。

常见示例包括：

| 集合                    | 遍历特性                                 |
| ----------------------- | ---------------------------------------- |
| `CopyOnWriteArrayList`  | 快照迭代，遍历的是创建迭代器时的数组副本 |
| `ConcurrentHashMap`     | 弱一致性迭代，遍历过程中可以并发修改     |
| `ConcurrentLinkedQueue` | 弱一致性迭代                             |

下面示例演示 `CopyOnWriteArrayList` 遍历时修改集合不会抛出并发修改异常。

```java
package io.github.atengk.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * fail-safe 风格遍历示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class FailSafeDemo {

    /**
     * 演示 CopyOnWriteArrayList 遍历时修改集合
     */
    public static void eachAndModify() {
        List<String> userNames = new CopyOnWriteArrayList<>();
        userNames.add("张三");
        userNames.add("李四");
        userNames.add("王五");

        if (CollUtil.isEmpty(userNames)) {
            return;
        }

        for (String userName : userNames) {
            if ("李四".equals(userName)) {
                userNames.remove(userName);
            }
            System.out.println("当前遍历用户：" + userName);
        }

        System.out.println("最终用户列表：" + userNames);
    }

    public static void main(String[] args) {
        eachAndModify();
    }
}
```

`CopyOnWriteArrayList` 的遍历基于快照，所以遍历过程中修改集合不会影响当前遍历过程。但它的写操作成本较高，因为每次修改都会复制底层数组，因此适合读多写少场景。

`fail-fast` 与 `fail-safe` 对比如下：

| 对比项       | fail-fast                                  | fail-safe / 弱一致性                        |
| ------------ | ------------------------------------------ | ------------------------------------------- |
| 典型集合     | `ArrayList`、`HashMap`、`HashSet`          | `CopyOnWriteArrayList`、`ConcurrentHashMap` |
| 修改时表现   | 可能抛出 `ConcurrentModificationException` | 通常不抛出该异常                            |
| 遍历数据     | 直接遍历原集合结构                         | 快照或弱一致视图                            |
| 数据实时性   | 较强，但不允许结构性并发修改               | 不一定能看到最新修改                        |
| 适用场景     | 普通集合遍历                               | 并发集合遍历                                |
| 是否绝对保证 | fail-fast 不是强保证                       | 弱一致性不保证看到全部最新数据              |

总结如下：

```text
普通集合遍历时不要直接修改集合结构。
需要遍历时删除元素，使用 Iterator.remove()。
多线程并发遍历和修改，使用并发集合。
读多写少列表场景，可以使用 CopyOnWriteArrayList。
并发 Map 场景，可以使用 ConcurrentHashMap。
```

集合工具类和常见问题这一部分的重点是：普通集合优先保持简单，工具类用于提升代码表达力；并发场景不要强行使用普通集合加手动控制，优先选择合适的并发集合。
