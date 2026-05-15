# 泛型

泛型是 Java 中用于提升类型安全和代码复用能力的重要语法。它允许类、接口、方法在定义时不固定具体类型，而是在使用时再指定实际类型。通过泛型，可以让编译器在编译阶段检查类型是否正确，减少运行时类型转换错误。

## 泛型概述

泛型的核心思想是“参数化类型”，也就是把类型作为参数传递给类、接口或方法。例如 `List<String>` 表示这个集合只能存放 `String` 类型的数据，`List<Integer>` 表示这个集合只能存放 `Integer` 类型的数据。

泛型常见于集合框架、工具类封装、通用响应对象、分页结果、缓存容器、数据转换方法等场景。

### 泛型的作用

泛型主要用于解决类型安全、类型转换和代码复用问题。没有泛型时，程序通常会使用 `Object` 接收任意类型的数据，但这样会导致类型检查延迟到运行阶段，一旦类型转换错误，就会抛出 `ClassCastException`。

使用泛型后，编译器可以提前检查类型是否匹配，避免很多运行时错误。

泛型的主要作用如下：

| 作用             | 说明                                             |
| ---------------- | ------------------------------------------------ |
| 提高类型安全     | 编译阶段检查类型，避免错误类型混入               |
| 减少强制类型转换 | 获取数据时不需要频繁进行类型强转                 |
| 提高代码复用性   | 同一套类、接口或方法可以适配多种类型             |
| 增强代码可读性   | 从声明上就能看出数据类型                         |
| 支持通用组件封装 | 适合封装结果对象、分页对象、缓存对象、工具方法等 |

下面的代码演示不使用泛型和使用泛型时的差异。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericOverviewDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛型概述示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericOverviewDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        useObjectList();
        useGenericList();
    }

    /**
     * 不使用泛型的集合示例
     */
    private static void useObjectList() {
        List list = new ArrayList();

        list.add("Java");
        list.add(100);

        Object first = list.get(0);
        String name = (String) first;

        System.out.println("未使用泛型：" + name);

        // 下面这行代码编译不会报错，但运行时会抛出 ClassCastException
        // String number = (String) list.get(1);
    }

    /**
     * 使用泛型的集合示例
     */
    private static void useGenericList() {
        List<String> list = CollUtil.newArrayList("Java", "Spring Boot", "MyBatis-Plus");

        // list.add(100);
        // 编译阶段直接报错，Integer 不能放入 List<String>

        String first = list.get(0);

        System.out.println("使用泛型：" + first);
    }
}
```

在这个示例中，`List<String>` 明确限制集合中只能存放字符串。错误类型无法通过编译，这就是泛型最重要的价值：把类型错误尽量提前到编译阶段暴露。

### 泛型的使用场景

泛型适合用于“逻辑相同，但数据类型不同”的场景。只要一段代码不应该绑定到某一个固定类型，就可以考虑使用泛型。

常见使用场景如下：

| 场景         | 示例                                          |
| ------------ | --------------------------------------------- |
| 集合容器     | `List<String>`、`Map<String, Integer>`        |
| 通用返回结果 | `Result<UserVO>`、`Result<List<OrderVO>>`     |
| 分页结果     | `PageResult<UserVO>`、`PageResult<ProductVO>` |
| 缓存封装     | `CacheWrapper<T>`                             |
| 数据转换     | `convert(S source, Class<T> targetType)`      |
| 通用工具方法 | `getFirst(List<T> list)`                      |
| 接口抽象     | `Repository<T>`、`Handler<T>`                 |

下面的代码演示一个通用响应对象，实际开发中经常用于接口返回结果封装。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/Result.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.util.ObjectUtil;

/**
 * 通用响应对象
 *
 * @param <T> 响应数据类型
 * @author Ateng
 * @since 2026-05-15
 */
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 通用响应对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 创建失败响应
     *
     * @param message 错误信息
     * @param <T>     响应数据类型
     * @return 通用响应对象
     */
    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(ObjectUtil.defaultIfNull(message, "操作失败"));
        return result;
    }

    /**
     * 判断是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return ObjectUtil.equal(200, this.code);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
```

下面演示 `Result<T>` 在不同数据类型下的使用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/ResultUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 通用响应对象使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ResultUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        Result<String> nameResult = Result.success("Ateng");
        Result<List<String>> languageResult = Result.success(CollUtil.newArrayList("Java", "Spring Boot", "Vue"));

        String name = nameResult.getData();
        List<String> languages = languageResult.getData();

        System.out.println("姓名：" + name);
        System.out.println("技术栈：" + languages);
    }
}
```

在这个示例中，`Result<String>` 的 `data` 是字符串，`Result<List<String>>` 的 `data` 是字符串集合。`Result<T>` 只写了一份代码，却能适配不同返回数据类型，这就是泛型在通用组件封装中的典型使用方式。

### 泛型与 Object 的区别

泛型和 `Object` 都可以让代码接收多种类型，但二者的类型检查时机和安全性完全不同。

`Object` 是 Java 所有类的父类，它可以接收任意引用类型对象。问题是，使用 `Object` 后，编译器不知道对象的真实类型，取出数据时通常需要开发者手动强制转换。如果转换错误，只能在运行时暴露。

泛型则是在定义时使用类型参数，在使用时指定具体类型。编译器会根据泛型声明进行类型检查，从而提前发现类型错误。

| 对比项               | 泛型                         | Object                         |
| -------------------- | ---------------------------- | ------------------------------ |
| 类型检查时机         | 编译阶段                     | 运行阶段                       |
| 是否需要强制类型转换 | 通常不需要                   | 通常需要                       |
| 类型安全性           | 高                           | 低                             |
| 代码可读性           | 明确知道数据类型             | 需要阅读上下文或强转代码       |
| 常见风险             | 类型擦除带来的限制           | `ClassCastException`           |
| 推荐场景             | 通用类、集合、接口、方法封装 | 确实需要接收任意对象的底层逻辑 |

下面的代码对比 `ObjectBox` 和 `GenericBox<T>` 的区别。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/ObjectBox.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * Object 容器示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ObjectBox {

    private Object value;

    /**
     * 设置值
     *
     * @param value 值
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * 获取值
     *
     * @return 值
     */
    public Object getValue() {
        return value;
    }
}
```

下面的代码使用泛型定义容器，容器中存储的数据类型由使用方指定。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericBox.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 泛型容器示例
 *
 * @param <T> 容器数据类型
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericBox<T> {

    private T value;

    /**
     * 设置值
     *
     * @param value 值
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * 获取值
     *
     * @return 值
     */
    public T getValue() {
        return value;
    }
}
```

下面的代码演示两种容器在使用时的区别。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/BoxUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 容器使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BoxUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        useObjectBox();
        useGenericBox();
    }

    /**
     * 使用 Object 容器
     */
    private static void useObjectBox() {
        ObjectBox box = new ObjectBox();

        box.setValue("Java");

        // Object 类型取出后，需要手动强制转换
        String value = (String) box.getValue();

        System.out.println("Object 容器：" + value);

        box.setValue(100);

        // 编译阶段不会报错，但运行时会抛出 ClassCastException
        // String errorValue = (String) box.getValue();
    }

    /**
     * 使用泛型容器
     */
    private static void useGenericBox() {
        GenericBox<String> box = new GenericBox<>();

        box.setValue("Java");

        // 泛型明确指定为 String，取出时不需要强制类型转换
        String value = box.getValue();

        System.out.println("泛型容器：" + value);

        // 编译阶段直接报错，Integer 不能放入 GenericBox<String>
        // box.setValue(100);
    }
}
```

通过这个对比可以看出，`Object` 的灵活性更高，但类型安全较弱；泛型在保持代码复用能力的同时，可以让编译器帮助检查类型错误，因此在业务开发和框架封装中更常用。

泛型并不是完全替代 `Object`。如果方法本身确实需要接收任意类型，并且不关心具体类型，可以使用 `Object`。如果方法、类或接口需要在“保持类型一致”的前提下复用逻辑，应优先使用泛型。

```text
泛型适合表达：传入什么类型，返回或存储的就是什么类型。
Object 适合表达：我只接收一个对象，但不关心它具体是什么类型。
```



## 泛型类

泛型类是指在类名后声明类型参数的类。类中的字段、方法参数、返回值都可以使用这个类型参数。泛型类适合封装容器、结果对象、分页对象、缓存对象、数据节点等通用结构。

### 泛型类定义

泛型类的基本语法是在类名后面使用 `<T>` 声明类型参数，其中 `T` 只是一个类型占位符，实际类型由使用方在创建对象时指定。

常见写法如下：

```java
public class 类名<T> {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
```

其中，`T` 可以理解为一个“临时类型名称”。当使用 `GenericBox<String>` 时，`T` 就代表 `String`；当使用 `GenericBox<Integer>` 时，`T` 就代表 `Integer`。

下面的代码定义了一个通用数据容器，可以存储任意引用类型的数据。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericBox.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.util.ObjectUtil;

/**
 * 泛型数据容器
 *
 * @param <T> 容器中的数据类型
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericBox<T> {

    private T value;

    /**
     * 创建空容器
     */
    public GenericBox() {
    }

    /**
     * 创建带初始值的容器
     *
     * @param value 初始值
     */
    public GenericBox(T value) {
        this.value = value;
    }

    /**
     * 获取容器中的值
     *
     * @return 容器中的值
     */
    public T getValue() {
        return value;
    }

    /**
     * 设置容器中的值
     *
     * @param value 容器中的值
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * 判断容器中的值是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return ObjectUtil.isEmpty(this.value);
    }
}
```

使用泛型类时，需要在类名后面指定具体类型。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericBoxUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 泛型数据容器使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericBoxUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        GenericBox<String> stringBox = new GenericBox<>("Java");
        String language = stringBox.getValue();

        GenericBox<Integer> integerBox = new GenericBox<>(100);
        Integer score = integerBox.getValue();

        System.out.println("字符串容器：" + language);
        System.out.println("整数容器：" + score);
    }
}
```

在这个示例中，`GenericBox<String>` 只能存放字符串，`GenericBox<Integer>` 只能存放整数。泛型类本身只定义了一次，但可以适配不同的数据类型。

### 泛型字段

泛型字段是指类中的成员变量使用泛型类型声明。泛型字段可以是单个对象，也可以和集合、Map、数组等结构组合使用。

常见泛型字段形式如下：

| 字段形式     | 示例                              | 说明                         |
| ------------ | --------------------------------- | ---------------------------- |
| 单个泛型对象 | `private T data;`                 | 存储单个数据                 |
| 泛型集合     | `private List<T> records;`        | 存储一组同类型数据           |
| 泛型 Map     | `private Map<String, T> dataMap;` | 使用固定 Key，Value 使用泛型 |
| 多个泛型参数 | `private K key; private V value;` | 同时表示多个类型             |

下面的代码定义了一个分页结果对象，`records` 字段使用 `List<T>`，表示分页数据列表中的元素类型由使用方决定。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/PageResult.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 分页结果对象
 *
 * @param <T> 分页记录类型
 * @author Ateng
 * @since 2026-05-15
 */
public class PageResult<T> {

    private List<T> records;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    /**
     * 创建分页结果
     *
     * @param records  分页记录
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param <T>      分页记录类型
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setRecords(CollUtil.emptyIfNull(records));
        pageResult.setTotal(total);
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        return pageResult;
    }

    /**
     * 判断分页结果是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return CollUtil.isEmpty(this.records);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
```

下面演示分页结果对象的使用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/PageResultUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 分页结果对象使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PageResultUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<String> users = CollUtil.newArrayList("张三", "李四", "王五");

        PageResult<String> pageResult = PageResult.of(users, 3L, 1, 10);

        List<String> records = pageResult.getRecords();

        System.out.println("分页数据：" + records);
        System.out.println("是否为空：" + pageResult.isEmpty());
    }
}
```

这里的 `PageResult<String>` 表示分页记录是字符串类型。如果业务中需要返回用户对象、订单对象或商品对象，只需要改成 `PageResult<UserVO>`、`PageResult<OrderVO>` 或 `PageResult<ProductVO>`，分页结构本身不需要重复定义。

### 泛型方法调用

泛型类中的方法可以直接使用类上声明的泛型类型。调用泛型类方法时，通常不需要额外指定泛型，编译器会根据对象声明自动推断类型。

下面的代码演示泛型类方法调用过程。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericClassMethodUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.util.StrUtil;

/**
 * 泛型类方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericClassMethodUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        GenericBox<String> box = new GenericBox<>();

        box.setValue("Java 泛型");

        String value = box.getValue();

        if (StrUtil.isNotBlank(value)) {
            System.out.println("容器内容：" + value);
        }
    }
}
```

在这个示例中，`box` 的类型是 `GenericBox<String>`，所以 `setValue` 方法只能接收 `String` 类型参数，`getValue` 方法返回值也会被编译器识别为 `String` 类型。

错误示例如下：

```java
GenericBox<String> box = new GenericBox<>();

box.setValue("Java");

// 编译错误：Integer 不能传入 String 类型的泛型容器
// box.setValue(100);
```

泛型类方法调用的重点是：对象在声明时确定泛型类型，后续方法调用都会围绕这个类型进行编译检查。

## 泛型方法

泛型方法是指在方法上单独声明类型参数的方法。它可以定义在普通类中，也可以定义在泛型类中。泛型方法的类型参数只在当前方法内部有效，适合封装通用工具方法、类型转换方法、集合处理方法等。

### 泛型方法定义

泛型方法需要在返回值类型前面声明类型参数，例如 `<T>`、`<E>`、`<K, V>` 等。

基本语法如下：

```java
public <T> T 方法名(T param) {
    return param;
}
```

注意，方法返回值前面的 `<T>` 是泛型方法的声明，后面的 `T` 才是返回值类型。如果没有前面的 `<T>`，编译器就不知道 `T` 是什么。

下面的代码定义了几个常见的泛型工具方法。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericMethodUtil.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;

import java.util.List;

/**
 * 泛型方法工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericMethodUtil {

    private GenericMethodUtil() {
    }

    /**
     * 返回非空值
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @param <T>          数据类型
     * @return 非空值
     */
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return ObjectUtil.defaultIfNull(value, defaultValue);
    }

    /**
     * 获取列表中的第一个元素
     *
     * @param list 数据列表
     * @param <T>  元素类型
     * @return 第一个元素
     */
    public static <T> T getFirst(List<T> list) {
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 创建泛型数据容器
     *
     * @param value 容器值
     * @param <T>   容器值类型
     * @return 泛型数据容器
     */
    public static <T> GenericBox<T> boxOf(T value) {
        return new GenericBox<>(value);
    }
}
```

下面演示泛型方法的调用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericMethodUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 泛型方法使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericMethodUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String name = GenericMethodUtil.defaultIfNull(null, "默认用户");
        Integer age = GenericMethodUtil.defaultIfNull(null, 18);

        List<String> languageList = CollUtil.newArrayList("Java", "Spring Boot", "Vue");
        String firstLanguage = GenericMethodUtil.getFirst(languageList);

        GenericBox<String> box = GenericMethodUtil.boxOf("泛型方法创建容器");

        System.out.println("姓名：" + name);
        System.out.println("年龄：" + age);
        System.out.println("第一个技术：" + firstLanguage);
        System.out.println("容器内容：" + box.getValue());
    }
}
```

调用泛型方法时，大多数情况下不需要显式写出类型，编译器会根据参数和返回值自动推断。例如 `defaultIfNull(null, 18)` 会被推断为 `Integer` 类型，`getFirst(languageList)` 会被推断为 `String` 类型。

### 静态泛型方法

静态方法不能直接使用泛型类上声明的类型参数，因为类上的泛型参数属于对象级别，而静态方法属于类级别。静态方法如果需要使用泛型，必须在方法自身声明泛型参数。

错误示例如下：

```java
public class CacheBox<T> {

    // 编译错误：静态方法不能直接使用类上的泛型 T
    // public static T getDefaultValue() {
    //     return null;
    // }
}
```

正确做法是在静态方法上单独声明 `<T>`。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/StaticGenericMethodUtil.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.util.ObjectUtil;

/**
 * 静态泛型方法工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StaticGenericMethodUtil {

    private StaticGenericMethodUtil() {
    }

    /**
     * 选择非空值
     *
     * @param first  第一个值
     * @param second 第二个值
     * @param <T>    数据类型
     * @return 非空值
     */
    public static <T> T chooseNotNull(T first, T second) {
        return ObjectUtil.defaultIfNull(first, second);
    }

    /**
     * 根据数据创建成功响应
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 通用响应对象
     */
    public static <T> Result<T> success(T data) {
        return Result.success(data);
    }
}
```

下面演示静态泛型方法的调用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/StaticGenericMethodUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 静态泛型方法使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StaticGenericMethodUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String name = StaticGenericMethodUtil.chooseNotNull(null, "Ateng");
        Integer count = StaticGenericMethodUtil.chooseNotNull(null, 100);

        Result<String> result = StaticGenericMethodUtil.success("操作成功");

        System.out.println("名称：" + name);
        System.out.println("数量：" + count);
        System.out.println("响应数据：" + result.getData());
    }
}
```

静态泛型方法在工具类中非常常见，例如集合工具、对象转换工具、响应结果构建工具等。

### 多泛型参数

一个泛型方法可以声明多个类型参数，例如 `<K, V>`、`<S, T>`。多个泛型参数适合表示键值对、源对象和目标对象、输入类型和输出类型等关系。

常见命名习惯如下：

| 泛型名称 | 含义                  |
| -------- | --------------------- |
| `T`      | Type，普通类型        |
| `E`      | Element，集合元素类型 |
| `K`      | Key，键类型           |
| `V`      | Value，值类型         |
| `S`      | Source，源类型        |
| `R`      | Result，结果类型      |

下面的代码定义了一个键值对对象。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/Pair.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 键值对对象
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author Ateng
 * @since 2026-05-15
 */
public class Pair<K, V> {

    private K key;

    private V value;

    /**
     * 创建键值对对象
     *
     * @param key   键
     * @param value 值
     * @param <K>   键类型
     * @param <V>   值类型
     * @return 键值对对象
     */
    public static <K, V> Pair<K, V> of(K key, V value) {
        Pair<K, V> pair = new Pair<>();
        pair.setKey(key);
        pair.setValue(value);
        return pair;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
```

下面演示多个泛型参数的使用。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/MultiGenericUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 多泛型参数使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MultiGenericUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        Pair<String, Integer> scorePair = Pair.of("Java", 95);
        Pair<Long, String> userPair = Pair.of(1001L, "Ateng");

        String subject = scorePair.getKey();
        Integer score = scorePair.getValue();

        Long userId = userPair.getKey();
        String username = userPair.getValue();

        System.out.println("科目：" + subject + "，分数：" + score);
        System.out.println("用户ID：" + userId + "，用户名：" + username);
    }
}
```

多泛型参数的重点是让多个类型之间的关系更加明确。例如 `Pair<String, Integer>` 表示键是字符串，值是整数；`Pair<Long, String>` 表示键是长整型，值是字符串。

## 泛型接口

泛型接口是在接口名后声明类型参数的接口。它通常用于定义通用能力，例如转换器、处理器、仓储接口、策略接口、校验器等。实现类可以指定具体类型，也可以继续保留泛型参数。

### 泛型接口定义

泛型接口的定义方式和泛型类类似，在接口名后面声明类型参数。

基本语法如下：

```java
public interface 接口名<T> {
    void handle(T data);
}
```

下面的代码定义了一个通用数据处理器接口。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/DataHandler.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 通用数据处理器
 *
 * @param <T> 数据类型
 * @author Ateng
 * @since 2026-05-15
 */
public interface DataHandler<T> {

    /**
     * 处理数据
     *
     * @param data 数据
     */
    void handle(T data);
}
```

下面的代码定义了一个通用类型转换接口，包含两个泛型参数，分别表示源类型和目标类型。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/Converter.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 通用类型转换器
 *
 * @param <S> 源数据类型
 * @param <T> 目标数据类型
 * @author Ateng
 * @since 2026-05-15
 */
public interface Converter<S, T> {

    /**
     * 转换数据
     *
     * @param source 源数据
     * @return 目标数据
     */
    T convert(S source);
}
```

泛型接口定义的是一种通用规则，具体类型可以交给实现类决定。

### 泛型接口实现

泛型接口有两种常见实现方式。

第一种是在实现类中指定具体泛型类型。此时实现类已经绑定了具体类型，方法参数和返回值也会变成具体类型。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/StringDataHandler.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.util.StrUtil;

/**
 * 字符串数据处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StringDataHandler implements DataHandler<String> {

    /**
     * 处理字符串数据
     *
     * @param data 字符串数据
     */
    @Override
    public void handle(String data) {
        if (StrUtil.isBlank(data)) {
            System.out.println("字符串数据为空");
            return;
        }

        System.out.println("处理字符串数据：" + data.trim());
    }
}
```

第二种是实现类继续保留泛型参数。此时实现类本身也是泛型类，具体类型仍然由使用方决定。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/DefaultDataHandler.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.util.ObjectUtil;

/**
 * 默认数据处理器
 *
 * @param <T> 数据类型
 * @author Ateng
 * @since 2026-05-15
 */
public class DefaultDataHandler<T> implements DataHandler<T> {

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    public void handle(T data) {
        if (ObjectUtil.isEmpty(data)) {
            System.out.println("数据为空");
            return;
        }

        System.out.println("处理通用数据：" + data);
    }
}
```

下面的代码演示两种泛型接口实现类的调用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericInterfaceUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 泛型接口使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericInterfaceUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        DataHandler<String> stringHandler = new StringDataHandler();
        stringHandler.handle(" Java 泛型接口 ");

        DataHandler<Integer> integerHandler = new DefaultDataHandler<>();
        integerHandler.handle(100);

        DataHandler<Boolean> booleanHandler = new DefaultDataHandler<>();
        booleanHandler.handle(Boolean.TRUE);
    }
}
```

如果实现类明确写成 `implements DataHandler<String>`，那么该实现类只能处理字符串。如果实现类写成 `class DefaultDataHandler<T> implements DataHandler<T>`，则它仍然是通用实现，可以处理不同类型的数据。

### 泛型接口的常见使用场景

泛型接口适合抽象一类通用行为，同时让具体数据类型由实现类或调用方指定。它在业务开发和框架源码中都非常常见。

常见使用场景如下：

| 场景     | 接口示例            | 说明                         |
| -------- | ------------------- | ---------------------------- |
| 数据转换 | `Converter<S, T>`   | 将一种类型转换为另一种类型   |
| 数据处理 | `Handler<T>`        | 针对不同类型执行不同处理逻辑 |
| 数据校验 | `Validator<T>`      | 对不同对象执行校验           |
| 仓储接口 | `Repository<T, ID>` | 抽象通用 CRUD 能力           |
| 策略模式 | `Strategy<T>`       | 根据不同类型执行不同策略     |
| 回调处理 | `Callback<T>`       | 返回指定类型的处理结果       |

下面的代码定义一个通用仓储接口，模拟常见的 CRUD 抽象。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/Repository.java`

```java
package io.github.atengk.basic.genericdemo;

import java.util.List;

/**
 * 通用仓储接口
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author Ateng
 * @since 2026-05-15
 */
public interface Repository<T, ID> {

    /**
     * 根据主键查询数据
     *
     * @param id 主键
     * @return 实体对象
     */
    T findById(ID id);

    /**
     * 查询全部数据
     *
     * @return 实体列表
     */
    List<T> findAll();

    /**
     * 保存数据
     *
     * @param entity 实体对象
     */
    void save(T entity);
}
```

下面定义一个用户对象。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/User.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 用户对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class User {

    private Long id;

    private String username;

    public User() {
    }

    public User(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
```

下面实现用户仓储接口。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/UserRepository.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户仓储实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserRepository implements Repository<User, Long> {

    private final List<User> userList = new ArrayList<>();

    /**
     * 根据主键查询用户
     *
     * @param id 用户ID
     * @return 用户对象
     */
    @Override
    public User findById(Long id) {
        if (CollUtil.isEmpty(userList)) {
            return null;
        }

        for (User user : userList) {
            if (id.equals(user.getId())) {
                return user;
            }
        }

        return null;
    }

    /**
     * 查询全部用户
     *
     * @return 用户列表
     */
    @Override
    public List<User> findAll() {
        return userList;
    }

    /**
     * 保存用户
     *
     * @param entity 用户对象
     */
    @Override
    public void save(User entity) {
        userList.add(entity);
    }
}
```

下面演示通用仓储接口的使用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/RepositoryUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 通用仓储接口使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class RepositoryUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        Repository<User, Long> userRepository = new UserRepository();

        userRepository.save(new User(1L, "Ateng"));
        userRepository.save(new User(2L, "Java"));

        User user = userRepository.findById(1L);

        if (user != null) {
            System.out.println("查询到用户：" + user.getUsername());
        }

        System.out.println("全部用户数量：" + userRepository.findAll().size());
    }
}
```

泛型接口的价值在于把“行为规则”和“具体类型”解耦。接口只负责定义能力，例如保存、查询、转换、处理；具体处理什么类型的数据，由实现类或调用方决定。

在实际开发中，类似 `Repository<T, ID>`、`Converter<S, T>`、`Handler<T>` 这类接口，可以减少重复代码，也能让业务扩展更加清晰。



## 泛型通配符

泛型通配符用于表示不确定的泛型类型。它通常出现在方法参数、返回值或变量声明中，用来提高泛型代码的灵活性。

Java 中常见的通配符有三种：

| 通配符     | 含义               | 典型写法                 |
| ---------- | ------------------ | ------------------------ |
| 无界通配符 | 任意未知类型       | `List<?>`                |
| 上界通配符 | 某个类型或其子类型 | `List<? extends Number>` |
| 下界通配符 | 某个类型或其父类型 | `List<? super Integer>`  |

通配符的重点不是“泛型是什么类型”，而是“这个泛型集合能安全地读什么、写什么”。

### 无界通配符

无界通配符使用 `?` 表示未知类型。例如 `List<?>` 表示这是一个元素类型未知的列表，它可以接收 `List<String>`、`List<Integer>`、`List<User>` 等任意泛型集合。

`List<?>` 的特点是：可以读取元素，但读取出来只能当作 `Object` 使用；不能向集合中添加具体元素，因为编译器不知道集合真实的元素类型。

下面的代码演示无界通配符的基本使用。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/UnboundedWildcardDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 无界通配符示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UnboundedWildcardDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<String> languageList = CollUtil.newArrayList("Java", "Spring Boot", "Vue");
        List<Integer> scoreList = CollUtil.newArrayList(90, 95, 100);

        printList(languageList);
        printList(scoreList);

        System.out.println("语言数量：" + size(languageList));
        System.out.println("分数数量：" + size(scoreList));
    }

    /**
     * 打印任意类型的列表
     *
     * @param list 数据列表
     */
    public static void printList(List<?> list) {
        if (CollUtil.isEmpty(list)) {
            System.out.println("列表为空");
            return;
        }

        for (Object item : list) {
            System.out.println("元素：" + item);
        }

        // 编译错误：无法确认 list 的真实元素类型
        // list.add("Java");
        // list.add(100);
    }

    /**
     * 获取任意类型列表的大小
     *
     * @param list 数据列表
     * @return 列表大小
     */
    public static int size(List<?> list) {
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        return list.size();
    }
}
```

在这个示例中，`printList(List<?> list)` 可以接收不同类型的集合，但方法内部不能向集合添加具体元素。因为 `List<?>` 的真实类型可能是 `List<String>`，也可能是 `List<Integer>`，如果允许随意添加元素，就会破坏类型安全。

无界通配符适合只读取、不修改的场景，例如打印集合、统计集合大小、判断集合是否为空等。

### 上界通配符

上界通配符使用 `? extends 类型` 表示泛型类型必须是指定类型或其子类型。例如 `List<? extends Number>` 可以接收 `List<Integer>`、`List<Long>`、`List<Double>` 等集合。

上界通配符适合读取数据。因为无论集合真实类型是 `Integer`、`Long` 还是 `Double`，它们都可以安全地当作 `Number` 读取出来。

下面的代码演示上界通配符的使用。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/UpperBoundWildcardDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 上界通配符示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UpperBoundWildcardDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<Integer> integerList = CollUtil.newArrayList(10, 20, 30);
        List<Double> doubleList = CollUtil.newArrayList(10.5, 20.5, 30.5);

        System.out.println("整数列表求和：" + sum(integerList));
        System.out.println("小数列表求和：" + sum(doubleList));
    }

    /**
     * 对 Number 及其子类型列表求和
     *
     * @param numberList 数字列表
     * @return 求和结果
     */
    public static double sum(List<? extends Number> numberList) {
        if (CollUtil.isEmpty(numberList)) {
            return 0D;
        }

        double total = 0D;
        for (Number number : numberList) {
            total += number.doubleValue();
        }

        return total;
    }
}
```

`List<? extends Number>` 表示集合中的元素类型一定是 `Number` 或 `Number` 的子类，所以读取时可以使用 `Number` 接收。

但是，上界通配符通常不能写入具体元素。

```java
List<? extends Number> numberList = CollUtil.newArrayList(1, 2, 3);

// 编译错误：无法确定 numberList 的真实元素类型
// numberList.add(100);
// numberList.add(100.5);
```

原因是 `numberList` 的真实类型可能是 `List<Integer>`，也可能是 `List<Double>`。如果真实类型是 `List<Integer>`，就不能添加 `Double`；如果真实类型是 `List<Double>`，就不能添加 `Integer`。为了保证类型安全，编译器禁止添加具体元素。

上界通配符适合“从集合中读取数据”的场景，例如统计、计算、展示、导出等。

### 下界通配符

下界通配符使用 `? super 类型` 表示泛型类型必须是指定类型或其父类型。例如 `List<? super Integer>` 可以接收 `List<Integer>`、`List<Number>`、`List<Object>`。

下界通配符适合写入数据。因为无论集合真实类型是 `Integer`、`Number` 还是 `Object`，都可以安全地写入 `Integer` 类型数据。

下面的代码演示下界通配符的使用。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/LowerBoundWildcardDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 下界通配符示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LowerBoundWildcardDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<Integer> integerList = new ArrayList<>();
        List<Number> numberList = new ArrayList<>();
        List<Object> objectList = new ArrayList<>();

        addInteger(integerList);
        addInteger(numberList);
        addInteger(objectList);

        System.out.println("Integer 列表：" + integerList);
        System.out.println("Number 列表：" + numberList);
        System.out.println("Object 列表：" + objectList);

        printFirst(numberList);
    }

    /**
     * 向 Integer 或其父类型集合中添加整数
     *
     * @param list 数据列表
     */
    public static void addInteger(List<? super Integer> list) {
        list.add(100);
        list.add(200);

        // 可以添加 null，但实际开发中不建议无意义添加
        // list.add(null);
    }

    /**
     * 读取下界通配符集合中的第一个元素
     *
     * @param list 数据列表
     */
    public static void printFirst(List<? super Integer> list) {
        if (CollUtil.isEmpty(list)) {
            System.out.println("列表为空");
            return;
        }

        Object first = list.get(0);
        System.out.println("第一个元素：" + first);
    }
}
```

`List<? super Integer>` 可以安全写入 `Integer`，但读取时只能使用 `Object` 接收。因为集合真实类型可能是 `List<Integer>`、`List<Number>` 或 `List<Object>`，编译器无法确定读取出来的具体类型。

下界通配符适合“向集合中写入数据”的场景，例如数据填充、批量复制、默认值追加等。

### PECS 原则

PECS 是泛型通配符的重要使用原则，全称是：

```text
Producer Extends, Consumer Super
```

含义如下：

| 原则             | 写法          | 说明                                     |
| ---------------- | ------------- | ---------------------------------------- |
| Producer Extends | `? extends T` | 如果参数主要用于提供数据，使用上界通配符 |
| Consumer Super   | `? super T`   | 如果参数主要用于接收数据，使用下界通配符 |

简单理解：

```text
只读数据，用 extends。
只写数据，用 super。
既要读又要写，通常不要使用通配符，直接使用明确的泛型类型。
```

下面的代码演示 PECS 原则的典型应用：从一个集合复制数据到另一个集合。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/PecsDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * PECS 原则示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PecsDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<Integer> sourceList = CollUtil.newArrayList(10, 20, 30);
        List<Number> targetList = new ArrayList<>();

        copy(sourceList, targetList);

        System.out.println("复制后的目标列表：" + targetList);
    }

    /**
     * 复制集合数据
     *
     * @param source 源列表，负责提供数据
     * @param target 目标列表，负责接收数据
     * @param <T>    元素类型
     */
    public static <T> void copy(List<? extends T> source, List<? super T> target) {
        if (CollUtil.isEmpty(source)) {
            return;
        }

        for (T item : source) {
            target.add(item);
        }
    }
}
```

在这个示例中，`source` 是数据生产者，使用 `List<? extends T>`；`target` 是数据消费者，使用 `List<? super T>`。

`copy(sourceList, targetList)` 中，`sourceList` 是 `List<Integer>`，`targetList` 是 `List<Number>`，可以安全地把 `Integer` 数据复制到 `Number` 集合中。

PECS 原则可以避免很多泛型边界写错的问题，是理解 `extends` 和 `super` 的关键。

## 泛型约束

泛型约束用于限制泛型参数的可选范围。默认情况下，`<T>` 可以代表任意引用类型，但有些场景需要限制 `T` 必须是某个类型的子类，或者必须实现某些接口，这时就需要使用泛型上界和多重边界。

### 泛型上界

泛型上界使用 `T extends 类型` 表示 `T` 必须是指定类型或其子类型。这里的 `extends` 不仅可以表示继承类，也可以表示实现接口。

基本语法如下：

```java
public class 类名<T extends 上界类型> {
}
```

例如 `T extends Number` 表示 `T` 必须是 `Number` 或其子类，常见可用类型包括 `Integer`、`Long`、`Double`、`BigDecimal` 等。

下面的代码定义了一个只能存储数字类型的泛型容器。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/NumberBox.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.util.NumberUtil;

/**
 * 数字泛型容器
 *
 * @param <T> 数字类型
 * @author Ateng
 * @since 2026-05-15
 */
public class NumberBox<T extends Number> {

    private T value;

    /**
     * 创建数字容器
     *
     * @param value 数字值
     */
    public NumberBox(T value) {
        this.value = value;
    }

    /**
     * 获取数字值
     *
     * @return 数字值
     */
    public T getValue() {
        return value;
    }

    /**
     * 判断数字是否大于零
     *
     * @return 是否大于零
     */
    public boolean isPositive() {
        return NumberUtil.compare(this.value.doubleValue(), 0D) > 0;
    }
}
```

下面演示泛型上界的使用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/NumberBoxUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 数字泛型容器使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class NumberBoxUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        NumberBox<Integer> integerBox = new NumberBox<>(100);
        NumberBox<Double> doubleBox = new NumberBox<>(99.5);

        System.out.println("Integer 是否大于零：" + integerBox.isPositive());
        System.out.println("Double 是否大于零：" + doubleBox.isPositive());

        // 编译错误：String 不是 Number 的子类
        // NumberBox<String> stringBox = new NumberBox<>("Java");
    }
}
```

泛型上界的作用是限制类型范围，并允许在泛型代码中调用上界类型的方法。例如 `T extends Number` 后，就可以在代码中安全调用 `doubleValue()`、`intValue()` 等 `Number` 中定义的方法。

### 多重边界

多重边界表示一个泛型参数同时满足多个限制。Java 中使用 `&` 连接多个边界。

基本语法如下：

```java
public class 类名<T extends 父类 & 接口1 & 接口2> {
}
```

需要注意：如果多个边界中包含类和接口，类必须写在最前面，接口写在后面。Java 只支持单继承，所以最多只能有一个类边界，但可以有多个接口边界。

下面的代码定义一个方法，要求泛型类型既是 `Number` 的子类，又实现 `Comparable<T>` 接口。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/MultiBoundDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 多重边界示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MultiBoundDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<Integer> scoreList = CollUtil.newArrayList(90, 80, 100, 95);
        List<Double> priceList = CollUtil.newArrayList(19.9, 29.9, 9.9);

        Integer maxScore = max(scoreList);
        Double maxPrice = max(priceList);

        System.out.println("最高分：" + maxScore);
        System.out.println("最高价格：" + maxPrice);
    }

    /**
     * 获取列表中的最大值
     *
     * @param list 数据列表
     * @param <T>  数字且可比较的类型
     * @return 最大值
     */
    public static <T extends Number & Comparable<T>> T max(List<T> list) {
        if (CollUtil.isEmpty(list)) {
            return null;
        }

        T maxValue = list.get(0);
        for (T item : list) {
            if (item.compareTo(maxValue) > 0) {
                maxValue = item;
            }
        }

        return maxValue;
    }
}
```

在这个示例中，`T extends Number & Comparable<T>` 表示 `T` 必须同时满足两个条件：

| 条件             | 作用                  |
| ---------------- | --------------------- |
| `extends Number` | 保证 `T` 是数字类型   |
| `Comparable<T>`  | 保证 `T` 可以比较大小 |

多重边界常用于对泛型能力有更严格要求的场景，例如既要求对象有主键，又要求对象可排序、可启用、可序列化等。

下面是一个更贴近业务抽象的多重边界示例。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/BusinessBoundDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 业务多重边界示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BusinessBoundDemo {

    /**
     * 打印可启用实体信息
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     */
    public static <T extends BaseEntity & Named & Enabled> void printEntity(T entity) {
        if (!entity.isEnabled()) {
            System.out.println("实体未启用：" + entity.getName());
            return;
        }

        System.out.println("实体ID：" + entity.getId() + "，实体名称：" + entity.getName());
    }

    /**
     * 基础实体
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class BaseEntity {

        private Long id;

        /**
         * 获取实体ID
         *
         * @return 实体ID
         */
        public Long getId() {
            return id;
        }

        /**
         * 设置实体ID
         *
         * @param id 实体ID
         */
        public void setId(Long id) {
            this.id = id;
        }
    }

    /**
     * 命名能力
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public interface Named {

        /**
         * 获取名称
         *
         * @return 名称
         */
        String getName();
    }

    /**
     * 启用能力
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public interface Enabled {

        /**
         * 判断是否启用
         *
         * @return 是否启用
         */
        boolean isEnabled();
    }
}
```

这个示例中，`T` 必须继承 `BaseEntity`，并且同时实现 `Named` 和 `Enabled`。因此在 `printEntity` 方法中，既可以调用 `getId()`，也可以调用 `getName()` 和 `isEnabled()`。

### 泛型与继承关系

泛型中的继承关系和普通类的继承关系不同。即使 `Dog` 是 `Animal` 的子类，`List<Dog>` 也不是 `List<Animal>` 的子类。

也就是说，下面的写法是错误的：

```java
List<Dog> dogList = new ArrayList<>();

// 编译错误：List<Dog> 不能赋值给 List<Animal>
// List<Animal> animalList = dogList;
```

这样设计是为了保证类型安全。假设 `List<Dog>` 可以赋值给 `List<Animal>`，那么就可以向 `animalList` 中添加 `Cat`，最终会导致 `dogList` 中混入猫对象。

下面的代码演示泛型和继承关系的正确使用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericInheritanceDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛型与继承关系示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericInheritanceDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<Dog> dogList = CollUtil.newArrayList(new Dog("边牧"), new Dog("柴犬"));
        List<Animal> animalList = new ArrayList<>();

        printAnimals(dogList);

        addDog(animalList);
        System.out.println("动物列表数量：" + animalList.size());
    }

    /**
     * 读取 Animal 或其子类列表
     *
     * @param animals 动物列表
     */
    public static void printAnimals(List<? extends Animal> animals) {
        if (CollUtil.isEmpty(animals)) {
            System.out.println("动物列表为空");
            return;
        }

        for (Animal animal : animals) {
            System.out.println("动物名称：" + animal.getName());
        }

        // 编译错误：上界通配符不能安全写入具体子类对象
        // animals.add(new Dog("金毛"));
    }

    /**
     * 向 Dog 或其父类型列表中添加 Dog
     *
     * @param dogs Dog 或其父类型列表
     */
    public static void addDog(List<? super Dog> dogs) {
        dogs.add(new Dog("金毛"));
    }

    /**
     * 动物父类
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class Animal {

        private final String name;

        /**
         * 创建动物对象
         *
         * @param name 动物名称
         */
        public Animal(String name) {
            this.name = name;
        }

        /**
         * 获取动物名称
         *
         * @return 动物名称
         */
        public String getName() {
            return name;
        }
    }

    /**
     * 狗对象
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class Dog extends Animal {

        /**
         * 创建狗对象
         *
         * @param name 狗名称
         */
        public Dog(String name) {
            super(name);
        }
    }

    /**
     * 猫对象
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class Cat extends Animal {

        /**
         * 创建猫对象
         *
         * @param name 猫名称
         */
        public Cat(String name) {
            super(name);
        }
    }
}
```

泛型与继承关系可以总结为：

| 写法                                      | 是否成立 | 说明                           |
| ----------------------------------------- | -------- | ------------------------------ |
| `Dog extends Animal`                      | 成立     | 普通类继承关系成立             |
| `List<Dog> extends List<Animal>`          | 不成立   | 泛型类型之间默认不具备协变关系 |
| `List<? extends Animal>` 接收 `List<Dog>` | 成立     | 适合读取                       |
| `List<? super Dog>` 接收 `List<Animal>`   | 成立     | 适合写入                       |

理解这一点后，`? extends` 和 `? super` 的使用会清晰很多。

## 类型擦除

类型擦除是 Java 泛型的底层实现机制。Java 的泛型主要在编译阶段发挥作用，编译完成后，大部分泛型类型信息会被擦除。

也就是说，`List<String>` 和 `List<Integer>` 在编译后都会变成原始类型 `List`。泛型帮助编译器做类型检查，但运行时通常无法直接获取完整的泛型类型信息。

### 类型擦除机制

Java 泛型采用类型擦除实现。编译器会在编译阶段完成泛型类型检查，然后把泛型类型替换成对应的上界类型。

如果泛型没有指定上界，默认擦除为 `Object`。

源代码中定义如下：

```java
public class Box<T> {

    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
```

编译后可以近似理解为：

```java
public class Box {

    private Object value;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
```

如果泛型指定了上界，例如 `T extends Number`，那么擦除后会变成上界类型 `Number`。

源代码中定义如下：

```java
public class NumberValue<T extends Number> {

    private T value;

    public double doubleValue() {
        return value.doubleValue();
    }
}
```

编译后可以近似理解为：

```java
public class NumberValue {

    private Number value;

    public double doubleValue() {
        return value.doubleValue();
    }
}
```

类型擦除带来的结果是：泛型主要保证编译期类型安全，而不是运行时保留完整类型。

下面的代码演示 `List<String>` 和 `List<Integer>` 在运行时的类型是相同的。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/TypeErasureDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 类型擦除示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TypeErasureDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<String> stringList = CollUtil.newArrayList("Java");
        List<Integer> integerList = CollUtil.newArrayList(100);

        Class<?> stringListClass = stringList.getClass();
        Class<?> integerListClass = integerList.getClass();

        System.out.println("String 列表运行时类型：" + stringListClass);
        System.out.println("Integer 列表运行时类型：" + integerListClass);
        System.out.println("运行时类型是否相同：" + stringListClass.equals(integerListClass));
    }
}
```

运行结果中，`List<String>` 和 `List<Integer>` 的运行时类型都是集合实现类本身，例如 `java.util.ArrayList`，不会保留 `String` 或 `Integer` 这类泛型参数信息。

### 类型擦除后的类型转换

虽然泛型在编译后会被擦除，但编译器会在必要的位置自动插入类型转换代码。

例如下面的代码：

```java
List<String> list = new ArrayList<>();
list.add("Java");

String value = list.get(0);
```

经过类型擦除后，可以近似理解为：

```java
List list = new ArrayList();
list.add("Java");

String value = (String) list.get(0);
```

也就是说，泛型让开发者少写了强制类型转换，但底层仍然可能存在类型转换。只要不绕过泛型检查，编译器插入的转换通常是安全的。

下面的代码演示正常泛型使用和原始类型混用带来的风险。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/ErasureCastDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 类型擦除后的类型转换示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ErasureCastDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        normalGenericUse();
        rawTypeRisk();
    }

    /**
     * 正常使用泛型
     */
    private static void normalGenericUse() {
        List<String> list = CollUtil.newArrayList("Java", "Spring Boot");

        String first = list.get(0);

        System.out.println("正常泛型取值：" + first);
    }

    /**
     * 原始类型混用风险
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void rawTypeRisk() {
        List<String> stringList = CollUtil.newArrayList("Java");

        List rawList = stringList;
        rawList.add(100);

        try {
            String value = stringList.get(1);
            System.out.println("取值结果：" + value);
        } catch (ClassCastException e) {
            System.out.println("类型转换失败：" + e.getMessage());
        }
    }
}
```

在 `rawTypeRisk` 方法中，`List<String>` 被赋值给了原始类型 `List`，然后向其中添加了 `Integer`。这会绕过泛型的编译期检查，导致后续按 `String` 读取时出现 `ClassCastException`。

所以实际开发中应尽量避免使用原始类型，例如 `List`、`Map`、`Class`，应优先使用带泛型的写法：

```java
List<String> list = new ArrayList<>();
Map<String, Integer> map = new HashMap<>();
Class<User> userClass = User.class;
```

### 类型擦除的限制

类型擦除让 Java 泛型可以兼容早期版本的字节码，但也带来了一些限制。这些限制本质上都和“运行时泛型类型信息不完整”有关。

常见限制如下：

| 限制                         | 错误示例                                          | 原因                                   |
| ---------------------------- | ------------------------------------------------- | -------------------------------------- |
| 不能直接创建泛型对象         | `new T()`                                         | 运行时不知道 `T` 的具体类型            |
| 不能创建泛型数组             | `new T[10]`                                       | 数组运行时需要明确元素类型             |
| 不能使用基本类型作为泛型参数 | `List<int>`                                       | 泛型只支持引用类型                     |
| 不能直接判断具体泛型类型     | `list instanceof List<String>`                    | 运行时泛型类型已擦除                   |
| 不能定义相同擦除后的重载方法 | `method(List<String>)` 和 `method(List<Integer>)` | 擦除后方法签名相同                     |
| 静态成员不能使用类泛型参数   | `static T value`                                  | 类泛型属于对象级别，静态成员属于类级别 |

下面的代码集中展示类型擦除相关限制。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/ErasureLimitDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import java.lang.reflect.Array;

/**
 * 类型擦除限制示例
 *
 * @param <T> 数据类型
 * @author Ateng
 * @since 2026-05-15
 */
public class ErasureLimitDemo<T> {

    private T value;

    // 编译错误：静态字段不能使用类上的泛型参数
    // private static T staticValue;

    /**
     * 创建限制示例对象
     *
     * @param value 数据值
     */
    public ErasureLimitDemo(T value) {
        this.value = value;
    }

    /**
     * 获取数据值
     *
     * @return 数据值
     */
    public T getValue() {
        return value;
    }

    /**
     * 错误示例：不能直接创建泛型对象
     *
     * @return 空值
     */
    public T createError() {
        // 编译错误：运行时不知道 T 的具体类型
        // return new T();

        return null;
    }

    /**
     * 正确示例：通过 Class 对象创建实例
     *
     * @param clazz 类型对象
     * @return 实例对象
     * @throws ReflectiveOperationException 反射创建异常
     */
    public T createByClass(Class<T> clazz) throws ReflectiveOperationException {
        return clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * 正确示例：通过反射创建泛型数组
     *
     * @param clazz  类型对象
     * @param length 数组长度
     * @return 泛型数组
     */
    @SuppressWarnings("unchecked")
    public T[] createArray(Class<T> clazz, int length) {
        return (T[]) Array.newInstance(clazz, length);
    }

    /**
     * 错误示例：不能直接创建泛型数组
     *
     * @param length 数组长度
     * @return 空值
     */
    public T[] createArrayError(int length) {
        // 编译错误：不能直接创建泛型数组
        // return new T[length];

        return null;
    }
}
```

下面演示如何正确规避 `new T()` 和 `new T[]` 的问题。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/ErasureLimitUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import java.util.Arrays;

/**
 * 类型擦除限制使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ErasureLimitUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     * @throws ReflectiveOperationException 反射创建异常
     */
    public static void main(String[] args) throws ReflectiveOperationException {
        ErasureLimitDemo<UserInfo> demo = new ErasureLimitDemo<>(new UserInfo());

        UserInfo userInfo = demo.createByClass(UserInfo.class);
        UserInfo[] userInfoArray = demo.createArray(UserInfo.class, 3);

        System.out.println("创建对象：" + userInfo);
        System.out.println("创建数组：" + Arrays.toString(userInfoArray));
    }

    /**
     * 用户信息对象
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserInfo {

        private String username;

        /**
         * 创建用户信息对象
         */
        public UserInfo() {
            this.username = "默认用户";
        }

        /**
         * 获取用户名
         *
         * @return 用户名
         */
        public String getUsername() {
            return username;
        }

        /**
         * 设置用户名
         *
         * @param username 用户名
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * 转换为字符串
         *
         * @return 字符串
         */
        @Override
        public String toString() {
            return "UserInfo{username='" + username + "'}";
        }
    }
}
```

类型擦除的限制可以总结为：

```text
泛型用于编译阶段的类型检查。
运行阶段大多数泛型参数会被擦除。
如果运行时确实需要知道具体类型，通常需要额外传入 Class<T>、TypeReference<T> 或通过反射获取类型信息。
```

在日常开发中，只要避免原始类型、合理使用 `Class<T>`、不要依赖运行时判断具体泛型参数，就可以规避大多数类型擦除带来的问题。



## 泛型数组与集合

泛型和数组、集合都可以用于存储一组数据，但它们的类型检查机制不同。数组在运行时仍然保留元素类型，而泛型主要在编译期做类型检查，编译后会发生类型擦除。因此，泛型数组在 Java 中存在一些限制，实际开发中通常优先使用泛型集合。

### 泛型数组问题

Java 不允许直接创建泛型数组，例如 `new T[10]` 是非法的。

错误示例如下：

```java
public class ArrayBox<T> {

    public T[] createArray(int size) {
        // 编译错误：不能直接创建泛型数组
        // return new T[size];

        return null;
    }
}
```

原因是数组需要在运行时知道具体的元素类型，而泛型在编译后大部分类型信息会被擦除。编译器无法确认 `T` 到底是 `String`、`Integer` 还是其他类型，所以不能直接创建 `T[]`。

如果确实需要创建泛型数组，常见做法是额外传入 `Class<T>`，然后通过反射创建数组。

下面的代码演示泛型数组的正确创建方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericArrayDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * 泛型数组示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericArrayDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String[] stringArray = createArray(String.class, 3);
        stringArray[0] = "Java";
        stringArray[1] = "Spring Boot";
        stringArray[2] = "Vue";

        List<String> languageList = CollUtil.newArrayList("MyBatis-Plus", "Redis", "RabbitMQ");

        System.out.println("泛型数组：" + Arrays.toString(stringArray));
        System.out.println("泛型集合：" + languageList);
    }

    /**
     * 通过 Class 创建泛型数组
     *
     * @param clazz  数组元素类型
     * @param length 数组长度
     * @param <T>    元素类型
     * @return 泛型数组
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] createArray(Class<T> clazz, int length) {
        return (T[]) Array.newInstance(clazz, length);
    }
}
```

在这个示例中，`String.class` 明确告诉程序数组元素类型是 `String`，所以可以通过 `Array.newInstance` 创建对应类型的数组。

不过在日常业务开发中，不建议频繁使用泛型数组。更推荐使用 `List<T>`，因为集合天然支持泛型，使用方式更简单，也更符合 Java 泛型的设计方向。

推荐写法如下：

```java
List<String> list = CollUtil.newArrayList("Java", "Spring Boot", "Vue");
```

不推荐写法如下：

```java
// 不推荐：泛型数组创建和类型转换都比较麻烦
// T[] array = new T[10];
```

### 泛型集合使用

泛型集合是 Java 开发中最常见的泛型使用方式。集合中使用泛型后，可以明确限制元素类型，避免不同类型的数据混入同一个集合。

常见泛型集合写法如下：

| 集合类型     | 示例                        | 说明         |
| ------------ | --------------------------- | ------------ |
| `List<T>`    | `List<String>`              | 有序、可重复 |
| `Set<T>`     | `Set<Long>`                 | 无序、不重复 |
| `Map<K, V>`  | `Map<Long, User>`           | 键值对结构   |
| 嵌套泛型     | `List<Map<String, Object>>` | 复杂结构     |
| 业务泛型对象 | `Result<List<UserVO>>`      | 接口返回结构 |

下面的代码演示 `List`、`Set`、`Map` 和嵌套泛型的常见使用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericCollectionDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 泛型集合示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericCollectionDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        useList();
        useSet();
        useMap();
        useNestedGeneric();
    }

    /**
     * 使用 List 泛型集合
     */
    private static void useList() {
        List<String> languageList = CollUtil.newArrayList("Java", "Spring Boot", "Vue");

        String first = languageList.get(0);

        System.out.println("第一个技术：" + first);

        // 编译错误：Integer 不能添加到 List<String>
        // languageList.add(100);
    }

    /**
     * 使用 Set 泛型集合
     */
    private static void useSet() {
        Set<Long> userIdSet = new HashSet<>();
        userIdSet.add(1001L);
        userIdSet.add(1002L);
        userIdSet.add(1001L);

        System.out.println("用户ID集合：" + userIdSet);
    }

    /**
     * 使用 Map 泛型集合
     */
    private static void useMap() {
        Map<Long, UserVO> userMap = new HashMap<>();
        userMap.put(1001L, new UserVO(1001L, "Ateng"));
        userMap.put(1002L, new UserVO(1002L, "Java"));

        UserVO user = userMap.get(1001L);

        System.out.println("用户名称：" + user.getUsername());
    }

    /**
     * 使用嵌套泛型
     */
    private static void useNestedGeneric() {
        List<UserVO> userList = CollUtil.newArrayList(
                new UserVO(1001L, "Ateng"),
                new UserVO(1002L, "Spring")
        );

        Result<List<UserVO>> result = Result.success(userList);

        System.out.println("响应用户数量：" + result.getData().size());
    }

    /**
     * 用户视图对象
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserVO {

        private Long id;

        private String username;

        public UserVO(Long id, String username) {
            this.id = id;
            this.username = username;
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }
    }

    /**
     * 通用响应对象
     *
     * @param <T> 响应数据类型
     * @author Ateng
     * @since 2026-05-15
     */
    public static class Result<T> {

        private Integer code;

        private String message;

        private T data;

        /**
         * 创建成功响应
         *
         * @param data 响应数据
         * @param <T>  响应数据类型
         * @return 通用响应对象
         */
        public static <T> Result<T> success(T data) {
            Result<T> result = new Result<>();
            result.code = 200;
            result.message = "操作成功";
            result.data = data;
            return result;
        }

        public Integer getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
    }
}
```

泛型集合的重点是：集合声明时指定什么类型，后续添加和读取时就按照这个类型进行编译检查。例如 `List<String>` 只能添加 `String`，读取出来也会被识别为 `String`，不需要手动强制类型转换。

在实际业务开发中，常见写法如下：

```java
List<UserVO> userList = queryUserList();

Map<Long, UserVO> userMap = convertToUserMap(userList);

Result<List<UserVO>> result = Result.success(userList);
```

这种写法比使用 `Object` 或原始集合更加安全，也能让代码含义更清晰。

### 泛型集合类型安全

泛型集合的类型安全依赖于编译器检查。只要使用规范的泛型声明，编译器就能阻止错误类型的数据进入集合。

正确示例如下：

```java
List<String> list = CollUtil.newArrayList();

list.add("Java");
list.add("Spring Boot");

// 编译错误：不能向 List<String> 添加 Integer
// list.add(100);
```

但是，如果使用原始类型，或者进行不安全的强制转换，就可能绕过泛型检查，导致运行时异常。

下面的代码演示泛型集合的类型安全问题。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericCollectionSafeDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 泛型集合类型安全示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericCollectionSafeDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        safeUse();
        unsafeUse();
    }

    /**
     * 安全使用泛型集合
     */
    private static void safeUse() {
        List<String> languageList = CollUtil.newArrayList("Java", "Spring Boot");

        String first = languageList.get(0);

        System.out.println("安全取值：" + first);
    }

    /**
     * 不安全使用原始类型集合
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void unsafeUse() {
        List<String> languageList = CollUtil.newArrayList("Java", "Spring Boot");

        List rawList = languageList;
        rawList.add(100);

        try {
            String value = languageList.get(2);
            System.out.println("取值结果：" + value);
        } catch (ClassCastException e) {
            System.out.println("集合类型不安全：" + e.getMessage());
        }
    }
}
```

`unsafeUse` 方法中，`List<String>` 被赋值给了原始类型 `List`，然后添加了 `Integer`。这个操作绕过了泛型检查，导致后续按照 `String` 读取时可能出现 `ClassCastException`。

为了保证泛型集合类型安全，实际开发中建议遵守以下规则：

| 建议                   | 说明                                                    |
| ---------------------- | ------------------------------------------------------- |
| 不使用原始类型         | 避免使用 `List`、`Map`，优先使用 `List<T>`、`Map<K, V>` |
| 不随意使用强制转换     | 不要把未知集合强转为具体泛型集合                        |
| 方法参数声明清楚泛型   | 例如 `List<UserVO>`，不要写成 `List`                    |
| 返回值保留泛型         | 例如 `Result<List<UserVO>>`                             |
| 只读场景使用通配符     | 例如 `List<? extends Number>`                           |
| 写入场景使用下界通配符 | 例如 `List<? super Integer>`                            |

泛型集合的核心价值就是让类型错误尽量在编译阶段暴露，而不是等到运行时才出现异常。

## 常见问题

泛型的常见问题主要集中在通配符、类型擦除、基本类型、泛型对象创建等方面。理解这些问题，可以避免很多泛型使用中的编译错误和运行时异常。

### `List<Object>` 和 `List<?>` 的区别

`List<Object>` 和 `List<?>` 都看起来可以表示“任意类型列表”，但它们的含义完全不同。

`List<Object>` 表示这个集合的元素类型明确是 `Object`。因此，可以向其中添加 `String`、`Integer`、`User` 等任何 `Object` 的子类对象。

`List<?>` 表示这个集合的元素类型未知。它可以接收 `List<String>`、`List<Integer>`、`List<User>` 等不同类型的集合，但不能向其中添加具体元素。

对比如下：

| 对比项                  | `List<Object>`        | `List<?>`          |
| ----------------------- | --------------------- | ------------------ |
| 含义                    | 元素类型就是 `Object` | 元素类型未知       |
| 能否接收 `List<String>` | 不能                  | 能                 |
| 能否添加 `String`       | 能                    | 不能               |
| 读取元素类型            | `Object`              | `Object`           |
| 适合场景                | 需要存放多种对象      | 只读取未知类型集合 |

下面的代码演示二者区别。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/ListObjectAndWildcardDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * List<Object> 与 List<?> 区别示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ListObjectAndWildcardDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<String> stringList = CollUtil.newArrayList("Java", "Spring Boot");

        useObjectList();
        useWildcardList(stringList);

        // 编译错误：List<String> 不是 List<Object> 的子类型
        // useObjectList(stringList);
    }

    /**
     * 使用 List<Object>
     */
    private static void useObjectList() {
        List<Object> objectList = new ArrayList<>();

        objectList.add("Java");
        objectList.add(100);
        objectList.add(Boolean.TRUE);

        for (Object item : objectList) {
            System.out.println("Object 列表元素：" + item);
        }
    }

    /**
     * 使用 List<?>
     *
     * @param list 未知类型列表
     */
    private static void useWildcardList(List<?> list) {
        for (Object item : list) {
            System.out.println("通配符列表元素：" + item);
        }

        // 编译错误：未知类型列表不能添加具体元素
        // list.add("Java");
        // list.add(100);
    }
}
```

可以这样理解：

```text
List<Object>：我就是一个 Object 类型的列表，可以添加各种对象。
List<?>：我可以代表任意泛型列表，但我不知道具体元素类型，所以不能随便添加数据。
```

如果方法只是打印、统计、读取集合内容，推荐使用 `List<?>`。如果确实需要存放不同类型的对象，可以使用 `List<Object>`。

### ? extends 与 ? super 的区别

`? extends` 和 `? super` 都是泛型通配符的边界写法，但适用方向不同。

`? extends T` 表示某个未知类型是 `T` 或 `T` 的子类型，适合读取数据。

`? super T` 表示某个未知类型是 `T` 或 `T` 的父类型，适合写入数据。

对比如下：

| 对比项   | `? extends T`       | `? super T`            |
| -------- | ------------------- | ---------------------- |
| 含义     | T 或 T 的子类型     | T 或 T 的父类型        |
| 主要用途 | 读取数据            | 写入数据               |
| 读取结果 | 可以作为 T 读取     | 只能作为 Object 读取   |
| 写入能力 | 不能写入具体 T 对象 | 可以写入 T 或 T 的子类 |
| 典型原则 | Producer Extends    | Consumer Super         |

下面的代码演示二者区别。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/ExtendsAndSuperDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * extends 与 super 通配符区别示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ExtendsAndSuperDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<Integer> integerList = CollUtil.newArrayList(10, 20, 30);
        List<Number> numberList = new ArrayList<>();

        printNumbers(integerList);

        addIntegers(numberList);
        System.out.println("写入后的 Number 列表：" + numberList);
    }

    /**
     * 读取 Number 或其子类型集合
     *
     * @param list 数字列表
     */
    private static void printNumbers(List<? extends Number> list) {
        for (Number number : list) {
            System.out.println("读取数字：" + number);
        }

        // 编译错误：无法确认集合真实类型，不能写入具体数字
        // list.add(100);
        // list.add(100.5);
    }

    /**
     * 向 Integer 或其父类型集合中写入 Integer
     *
     * @param list 数字列表
     */
    private static void addIntegers(List<? super Integer> list) {
        list.add(100);
        list.add(200);

        Object first = list.get(0);
        System.out.println("读取结果只能安全视为 Object：" + first);
    }
}
```

可以直接按 PECS 原则记忆：

```text
读取数据，用 ? extends T。
写入数据，用 ? super T。
既要读取又要写入，通常直接使用明确的泛型类型 T。
```

例如集合复制方法通常这样写：

```java
public static <T> void copy(List<? extends T> source, List<? super T> target) {
    for (T item : source) {
        target.add(item);
    }
}
```

其中，`source` 负责提供数据，所以使用 `extends`；`target` 负责接收数据，所以使用 `super`。

### 泛型为什么不能使用基本类型

Java 泛型不能使用基本类型，例如 `int`、`long`、`double`、`boolean`。下面写法是错误的：

```java
// 编译错误：泛型参数不能使用基本类型
// List<int> list = new ArrayList<>();
// List<long> idList = new ArrayList<>();
```

原因是 Java 泛型要求类型参数必须是引用类型，而基本类型不是对象，也不是 `Object` 的子类。泛型在类型擦除后通常会变成 `Object` 或上界类型，因此不能使用基本类型。

正确写法是使用基本类型对应的包装类型：

| 基本类型  | 包装类型    |
| --------- | ----------- |
| `int`     | `Integer`   |
| `long`    | `Long`      |
| `double`  | `Double`    |
| `float`   | `Float`     |
| `boolean` | `Boolean`   |
| `char`    | `Character` |
| `byte`    | `Byte`      |
| `short`   | `Short`     |

示例如下：

```java
List<Integer> scoreList = CollUtil.newArrayList(90, 95, 100);

List<Long> userIdList = CollUtil.newArrayList(1001L, 1002L, 1003L);

List<Boolean> statusList = CollUtil.newArrayList(Boolean.TRUE, Boolean.FALSE);
```

下面的代码演示泛型集合和包装类型的使用。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/PrimitiveGenericDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 泛型不能使用基本类型示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PrimitiveGenericDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        List<Integer> scoreList = CollUtil.newArrayList(90, 95, 100);
        List<Long> userIdList = CollUtil.newArrayList(1001L, 1002L, 1003L);

        Integer firstScore = scoreList.get(0);
        Long firstUserId = userIdList.get(0);

        System.out.println("第一个分数：" + firstScore);
        System.out.println("第一个用户ID：" + firstUserId);
    }
}
```

虽然集合中不能直接使用基本类型，但 Java 支持自动装箱和自动拆箱。因此，把 `int` 添加到 `List<Integer>` 时，编译器会自动把它转换为 `Integer`。

例如：

```java
List<Integer> list = CollUtil.newArrayList();

list.add(100);          // 自动装箱：int 转 Integer
int value = list.get(0); // 自动拆箱：Integer 转 int
```

需要注意的是，自动拆箱时如果包装类型为 `null`，会抛出 `NullPointerException`。

```java
Integer number = null;

// 运行时抛出 NullPointerException
// int value = number;
```

所以在业务开发中，如果集合中可能存在空值，应先进行非空判断。

### 泛型为什么不能直接 new T()

泛型不能直接 `new T()`，原因是 Java 泛型存在类型擦除。程序运行时通常不知道 `T` 的真实类型，所以无法确定应该创建哪个类的对象。

错误示例如下：

```java
public class ObjectFactory<T> {

    public T create() {
        // 编译错误：不能直接创建泛型对象
        // return new T();

        return null;
    }
}
```

如果需要创建泛型对象，常见解决方案有三种。

第一种方式是传入 `Class<T>`，通过反射创建对象。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericObjectFactory.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 泛型对象工厂
 *
 * @param <T> 对象类型
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericObjectFactory<T> {

    private final Class<T> clazz;

    /**
     * 创建泛型对象工厂
     *
     * @param clazz 对象类型
     */
    public GenericObjectFactory(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 创建对象
     *
     * @return 对象实例
     */
    public T create() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("创建泛型对象失败：" + clazz.getName(), e);
        }
    }
}
```

下面演示对象工厂的使用方式。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/GenericObjectFactoryUseDemo.java`

```java
package io.github.atengk.basic.genericdemo;

/**
 * 泛型对象工厂使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class GenericObjectFactoryUseDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        GenericObjectFactory<UserInfo> factory = new GenericObjectFactory<>(UserInfo.class);

        UserInfo userInfo = factory.create();

        System.out.println("创建对象：" + userInfo);
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserInfo {

        private String username = "默认用户";

        public String getUsername() {
            return username;
        }

        @Override
        public String toString() {
            return "UserInfo{username='" + username + "'}";
        }
    }
}
```

第二种方式是传入 `Supplier<T>`，由调用方提供对象创建逻辑。这种方式比反射更灵活，也不要求类必须有无参构造方法。

文件位置：`src/main/java/io/github/atengk/basic/genericdemo/SupplierFactoryDemo.java`

```java
package io.github.atengk.basic.genericdemo;

import java.util.function.Supplier;

/**
 * Supplier 泛型对象创建示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SupplierFactoryDemo {

    /**
     * 程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        UserInfo userInfo = create(UserInfo::new);
        OrderInfo orderInfo = create(() -> new OrderInfo(1001L));

        System.out.println("用户对象：" + userInfo);
        System.out.println("订单对象：" + orderInfo);
    }

    /**
     * 根据 Supplier 创建对象
     *
     * @param supplier 对象提供者
     * @param <T>      对象类型
     * @return 对象实例
     */
    public static <T> T create(Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * 用户信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class UserInfo {

        private String username = "Ateng";

        @Override
        public String toString() {
            return "UserInfo{username='" + username + "'}";
        }
    }

    /**
     * 订单信息
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class OrderInfo {

        private Long orderId;

        public OrderInfo(Long orderId) {
            this.orderId = orderId;
        }

        @Override
        public String toString() {
            return "OrderInfo{orderId=" + orderId + "}";
        }
    }
}
```

第三种方式是直接由外部传入已经创建好的对象，泛型类只负责保存、处理或返回对象。

```java
GenericBox<UserInfo> box = new GenericBox<>(new UserInfo());
```

可以这样总结：

| 需求                     | 推荐方式                              |
| ------------------------ | ------------------------------------- |
| 只保存泛型对象           | 外部创建后传入                        |
| 需要在工具方法中创建对象 | 使用 `Supplier<T>`                    |
| 需要根据类型动态创建对象 | 使用 `Class<T>` + 反射                |
| 需要创建泛型数组         | 使用 `Class<T>` + `Array.newInstance` |

泛型不能直接 `new T()` 不是语法缺陷，而是类型擦除机制带来的结果。运行时不知道 `T` 的具体类型，自然无法直接创建对应对象。
