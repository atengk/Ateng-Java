# 面向对象基础

## 面向对象概述

面向对象是 Java 编程中最核心的思想之一。Java 程序通常不是简单地按照步骤堆叠代码，而是围绕“类”和“对象”组织程序结构。

在面向对象思想中，程序会把现实世界中的事物抽象成对象，再通过对象的属性和行为来描述业务。对象之间通过方法调用进行协作，从而完成复杂的业务功能。

### 面向对象编程思想

面向对象编程，简称 OOP，英文全称是 Object-Oriented Programming。它的核心思想是：把数据和操作数据的行为封装到对象中，通过对象之间的协作完成程序功能。

在现实世界中，一个“学生”可以有姓名、年龄、学号等属性，也可以有学习、考试、自我介绍等行为。映射到 Java 程序中，可以把“学生”抽象成一个 `Student` 类，然后通过这个类创建具体的学生对象。

面向对象编程主要关注三个问题：

| 问题               | Java 中的体现 | 示例                 |
| ------------------ | ------------- | -------------------- |
| 这个事物是谁       | 类、对象      | `Student`、`student` |
| 这个事物有什么特征 | 成员变量      | `name`、`age`        |
| 这个事物能做什么   | 成员方法      | `introduce()`        |

下面的代码演示如何使用类描述学生，并创建学生对象调用方法。

文件位置：`src/main/java/io/github/atengk/oop/Student.java`

```java
package io.github.atengk.oop;

/**
 * 学生类，用于演示面向对象中的属性和行为
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Student {

    private String name;

    private Integer age;

    public Student(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public void introduce() {
        System.out.println("大家好，我是" + name + "，今年" + age + "岁。");
    }
}
```

下面的代码演示如何创建 `Student` 对象并调用对象方法。

文件位置：`src/main/java/io/github/atengk/oop/StudentTest.java`

```java
package io.github.atengk.oop;

/**
 * 学生测试类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StudentTest {

    public static void main(String[] args) {
        Student student = new Student("张三", 18);
        student.introduce();
    }
}
```

在这个示例中，`Student` 是类，表示一类学生的抽象定义；`student` 是对象，表示通过类创建出来的具体学生实例；`name` 和 `age` 是对象的属性；`introduce()` 是对象的行为。

面向对象编程的重点不是先考虑“第一步做什么、第二步做什么”，而是先分析程序中有哪些对象，每个对象具备哪些属性和行为，然后让这些对象相互配合完成业务逻辑。

### 面向对象与面向过程的区别

面向过程和面向对象是两种不同的编程思想。

面向过程更关注“执行步骤”。程序会按照一个个方法或函数的调用顺序执行，适合处理逻辑简单、流程固定的问题。

面向对象更关注“对象职责”。程序会先抽象出对象，再把数据和行为封装到对象内部，通过对象之间的调用完成业务处理。它更适合处理业务复杂、结构清晰、需要长期维护和扩展的系统。

下面以“用户登录”为例进行对比。

#### 面向过程写法

面向过程写法通常会把数据和判断逻辑直接写在同一个流程中。

文件位置：`src/main/java/io/github/atengk/oop/ProcessLoginDemo.java`

```java
package io.github.atengk.oop;

/**
 * 面向过程登录示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ProcessLoginDemo {

    public static void main(String[] args) {
        String username = "admin";
        String password = "123456";

        if ("admin".equals(username) && "123456".equals(password)) {
            System.out.println("登录成功");
        } else {
            System.out.println("用户名或密码错误");
        }
    }
}
```

这种写法比较直接，适合简单场景。但如果后续需要增加验证码校验、账号状态校验、密码加密、登录日志等功能，所有逻辑容易堆积在同一个方法中，代码维护成本会逐渐升高。

#### 面向对象写法

面向对象写法会把用户数据和登录逻辑拆分到不同的类中，让每个类负责自己的职责。

文件位置：`src/main/java/io/github/atengk/oop/User.java`

```java
package io.github.atengk.oop;

/**
 * 用户实体类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class User {

    private String username;

    private String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
```

下面的代码用于封装登录判断逻辑。

文件位置：`src/main/java/io/github/atengk/oop/LoginService.java`

```java
package io.github.atengk.oop;

/**
 * 登录服务类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LoginService {

    public boolean login(User user) {
        return "admin".equals(user.getUsername()) && "123456".equals(user.getPassword());
    }
}
```

下面的代码用于创建对象并完成登录调用。

文件位置：`src/main/java/io/github/atengk/oop/ObjectLoginDemo.java`

```java
package io.github.atengk.oop;

/**
 * 面向对象登录示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ObjectLoginDemo {

    public static void main(String[] args) {
        User user = new User("admin", "123456");
        LoginService loginService = new LoginService();

        boolean result = loginService.login(user);

        if (result) {
            System.out.println("登录成功");
        } else {
            System.out.println("用户名或密码错误");
        }
    }
}
```

面向对象写法代码量可能更多，但结构更清晰。`User` 负责描述用户数据，`LoginService` 负责处理登录逻辑，`ObjectLoginDemo` 负责组装对象并调用方法。

当业务变复杂时，面向对象写法更容易扩展。例如后续增加验证码校验，可以继续在登录服务中扩展逻辑，而不是把所有代码都堆在 `main` 方法中。

#### 二者对比

| 对比项            | 面向过程                  | 面向对象                   |
| ----------------- | ------------------------- | -------------------------- |
| 核心关注点        | 执行步骤                  | 对象职责                   |
| 代码组织方式      | 按流程组织方法            | 按对象组织属性和行为       |
| 数据和行为关系    | 数据和行为通常分离        | 数据和行为封装在对象中     |
| 适合场景          | 简单流程、脚本、小功能    | 复杂业务、长期维护系统     |
| 扩展性            | 业务复杂后较难维护        | 更容易扩展和复用           |
| Java 中的典型体现 | `main` 方法中直接处理流程 | 类、对象、封装、继承、多态 |

面向过程并不是错误的思想，它适合简单问题。面向对象也不是所有场景都必须复杂设计，它更适合中大型程序中进行职责拆分、结构设计和代码复用。

### Java 面向对象的核心特性

Java 面向对象主要包含三大核心特性：封装、继承和多态。这三个特性是理解 Java 面向对象编程的基础。

#### 封装

封装是指把对象的属性和行为包装在类中，并通过访问修饰符控制外部访问权限。

通常情况下，成员变量会使用 `private` 修饰，防止外部直接修改；对外提供 `public` 方法进行访问和操作。这样可以保护对象内部数据，提高代码安全性和可维护性。

下面的代码演示封装的基本写法。

文件位置：`src/main/java/io/github/atengk/oop/EncapsulationUser.java`

```java
package io.github.atengk.oop;

/**
 * 封装示例用户类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EncapsulationUser {

    private String username;

    private Integer age;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            System.out.println("用户名不能为空");
            return;
        }
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        if (age == null || age < 0 || age > 150) {
            System.out.println("年龄不合法");
            return;
        }
        this.age = age;
    }
}
```

封装的作用主要包括：

| 作用         | 说明                                     |
| ------------ | ---------------------------------------- |
| 隐藏内部细节 | 外部不需要知道类内部如何实现             |
| 保护数据安全 | 防止外部随意修改对象属性                 |
| 统一访问入口 | 通过方法控制属性读取和修改               |
| 提高维护性   | 内部实现变化时，尽量减少对外部代码的影响 |

#### 继承

继承是指一个类可以继承另一个类的属性和方法，从而实现代码复用。

在 Java 中，使用 `extends` 关键字表示继承关系。被继承的类称为父类或基类，继承父类的类称为子类或派生类。

下面的代码演示继承的基本写法。

文件位置：`src/main/java/io/github/atengk/oop/Animal.java`

```java
package io.github.atengk.oop;

/**
 * 动物父类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Animal {

    private String name;

    public Animal(String name) {
        this.name = name;
    }

    public void eat() {
        System.out.println(name + "正在吃东西");
    }

    public String getName() {
        return name;
    }
}
```

下面的代码演示子类继承父类。

文件位置：`src/main/java/io/github/atengk/oop/Dog.java`

```java
package io.github.atengk.oop;

/**
 * 狗子类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Dog extends Animal {

    public Dog(String name) {
        super(name);
    }

    public void bark() {
        System.out.println(getName() + "正在叫");
    }
}
```

下面的代码演示如何调用继承自父类的方法和子类自己的方法。

文件位置：`src/main/java/io/github/atengk/oop/ExtendsDemo.java`

```java
package io.github.atengk.oop;

/**
 * 继承测试类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ExtendsDemo {

    public static void main(String[] args) {
        Dog dog = new Dog("旺财");
        dog.eat();
        dog.bark();
    }
}
```

继承的主要作用是代码复用和功能扩展。子类可以直接使用父类中非私有的属性和方法，也可以根据自己的业务需求新增方法或重写父类方法。

需要注意的是，Java 只支持单继承，也就是一个类只能直接继承一个父类。但一个类可以实现多个接口。

#### 多态

多态是指同一个对象在不同场景下可以表现出不同的形态。简单来说，就是父类引用可以指向子类对象，程序在运行时根据实际对象类型调用对应的方法。

多态通常需要满足三个条件：

| 条件                 | 说明                             |
| -------------------- | -------------------------------- |
| 存在继承或实现关系   | 子类继承父类，或类实现接口       |
| 存在方法重写         | 子类重写父类或接口中的方法       |
| 父类引用指向子类对象 | 例如 `Animal animal = new Dog()` |

下面的代码演示多态的基本用法。

文件位置：`src/main/java/io/github/atengk/oop/PolymorphismAnimal.java`

```java
package io.github.atengk.oop;

/**
 * 多态动物父类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PolymorphismAnimal {

    public void sound() {
        System.out.println("动物发出声音");
    }
}
```

下面的代码演示子类重写父类方法。

文件位置：`src/main/java/io/github/atengk/oop/PolymorphismDog.java`

```java
package io.github.atengk.oop;

/**
 * 多态狗子类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PolymorphismDog extends PolymorphismAnimal {

    @Override
    public void sound() {
        System.out.println("狗发出汪汪声");
    }
}
```

下面的代码演示另一个子类的不同实现。

文件位置：`src/main/java/io/github/atengk/oop/PolymorphismCat.java`

```java
package io.github.atengk.oop;

/**
 * 多态猫子类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PolymorphismCat extends PolymorphismAnimal {

    @Override
    public void sound() {
        System.out.println("猫发出喵喵声");
    }
}
```

下面的代码演示父类引用指向不同子类对象时的运行效果。

文件位置：`src/main/java/io/github/atengk/oop/PolymorphismDemo.java`

```java
package io.github.atengk.oop;

/**
 * 多态测试类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PolymorphismDemo {

    public static void main(String[] args) {
        PolymorphismAnimal dog = new PolymorphismDog();
        PolymorphismAnimal cat = new PolymorphismCat();

        dog.sound();
        cat.sound();
    }
}
```

程序运行时，`dog.sound()` 会调用 `PolymorphismDog` 中重写后的方法，`cat.sound()` 会调用 `PolymorphismCat` 中重写后的方法。这就是多态的体现。

多态的优势是提高程序扩展性。调用方只需要面向父类或接口编程，不需要强依赖具体子类。当新增子类时，原有调用逻辑通常不需要大量修改。

#### 核心特性总结

| 特性 | 关键字或体现                        | 核心作用             |
| ---- | ----------------------------------- | -------------------- |
| 封装 | `private`、`public`、Getter、Setter | 隐藏细节，保护数据   |
| 继承 | `extends`                           | 代码复用，功能扩展   |
| 多态 | 方法重写、父类引用指向子类对象      | 提高扩展性，降低耦合 |

封装解决的是“类内部数据如何保护”的问题；继承解决的是“相同代码如何复用”的问题；多态解决的是“不同对象如何统一调用”的问题。

这三个特性共同构成了 Java 面向对象编程的基础，也是后续学习类与对象、抽象类、接口、内部类和枚举的重要前提。





## 类与对象

类与对象是 Java 面向对象编程中最基础的概念。类可以理解为对象的模板，对象可以理解为根据类创建出来的具体实例。

在 Java 中，程序通常先定义类，再通过类创建对象。类中可以包含成员变量、成员方法和构造方法，用于描述对象的属性、行为和初始化方式。

### 类的定义

类是对一类事物的抽象描述。它定义了这类事物应该具备哪些属性，以及可以执行哪些行为。

在 Java 中，使用 `class` 关键字定义类。类名通常使用大驼峰命名法，也就是每个单词首字母大写，例如 `User`、`Student`、`OrderInfo`。

一个基本的类通常由以下几部分组成：

| 组成部分 | 说明                     |
| -------- | ------------------------ |
| 类名     | 用于表示一类事物         |
| 成员变量 | 用于描述对象的属性       |
| 成员方法 | 用于描述对象的行为       |
| 构造方法 | 用于创建对象时初始化数据 |

下面的代码定义了一个用户类，用于描述用户的基本信息和行为。

文件位置：`src/main/java/io/github/atengk/oop/User.java`

```java
package io.github.atengk.oop;

/**
 * 用户类，用于演示类的基本定义
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class User {

    private Long id;

    private String username;

    private Integer age;

    public void introduce() {
        System.out.println("用户ID：" + id + "，用户名：" + username + "，年龄：" + age);
    }
}
```

在这个示例中，`User` 是类名，`id`、`username`、`age` 是成员变量，`introduce()` 是成员方法。

需要注意的是，类只是模板，本身并不代表具体的数据。只有通过类创建对象后，成员变量才会真正保存具体的值。

### 对象的创建

对象是类的具体实例。定义类只是描述一类事物的结构，而对象才是真正可以使用的数据实体。

在 Java 中，创建对象通常使用 `new` 关键字。

基本语法如下：

```java
类名 对象名 = new 类名();
```

例如：

```java
User user = new User();
```

其中，左边的 `User` 表示引用变量的类型，`user` 是对象引用名称，右边的 `new User()` 表示在内存中创建一个新的 `User` 对象。

下面的代码演示对象的创建和使用。

文件位置：`src/main/java/io/github/atengk/oop/UserObjectDemo.java`

```java
package io.github.atengk.oop;

/**
 * 用户对象创建示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserObjectDemo {

    public static void main(String[] args) {
        User user = new User();
        user.introduce();
    }
}
```

上面的代码中，`new User()` 创建了一个用户对象，`user` 保存了这个对象的引用。通过 `user.introduce()` 可以调用对象中的成员方法。

对象创建后，每个对象都有自己独立的一份成员变量数据。即使多个对象来自同一个类，它们保存的数据也可以不同。

下面的代码演示创建多个对象。

文件位置：`src/main/java/io/github/atengk/oop/MultiUserDemo.java`

```java
package io.github.atengk.oop;

/**
 * 多对象创建示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MultiUserDemo {

    public static void main(String[] args) {
        User user1 = new User();
        User user2 = new User();

        user1.introduce();
        user2.introduce();

        System.out.println(user1 == user2);
    }
}
```

运行结果中，`user1 == user2` 的结果为 `false`，因为它们是两个不同的对象，只是都由 `User` 类创建而来。

### 成员变量

成员变量是定义在类中、方法外的变量，用于描述对象的属性。

例如用户对象可以有用户 ID、用户名、年龄等属性，这些属性就可以定义为成员变量。

成员变量有默认值。当创建对象后，如果没有手动赋值，Java 会根据变量类型自动赋予默认值。

| 数据类型 | 默认值     |
| -------- | ---------- |
| 整数类型 | `0`        |
| 浮点类型 | `0.0`      |
| 字符类型 | `'\u0000'` |
| 布尔类型 | `false`    |
| 引用类型 | `null`     |

下面的代码演示成员变量的默认值。

文件位置：`src/main/java/io/github/atengk/oop/MemberVariableDemo.java`

```java
package io.github.atengk.oop;

/**
 * 成员变量默认值示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MemberVariableDemo {

    private int number;

    private boolean enabled;

    private String name;

    public void show() {
        System.out.println("number = " + number);
        System.out.println("enabled = " + enabled);
        System.out.println("name = " + name);
    }

    public static void main(String[] args) {
        MemberVariableDemo demo = new MemberVariableDemo();
        demo.show();
    }
}
```

成员变量通常不建议直接暴露给外部访问。实际开发中，一般会使用 `private` 修饰成员变量，然后通过 Getter 和 Setter 方法进行访问，这属于封装的内容。

### 成员方法

成员方法是定义在类中的方法，用于描述对象可以执行的行为。

成员方法可以访问本类中的成员变量，也可以接收参数、返回结果，和普通方法的语法类似。

基本语法如下：

```java
访问修饰符 返回值类型 方法名(参数列表) {
    方法体;
}
```

下面的代码演示成员方法的定义和调用。

文件位置：`src/main/java/io/github/atengk/oop/Product.java`

```java
package io.github.atengk.oop;

/**
 * 商品类，用于演示成员方法
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Product {

    private String name;

    private BigDecimal price;

    private Integer stock;

    public Product(String name, BigDecimal price, Integer stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public void showInfo() {
        System.out.println("商品名称：" + name);
        System.out.println("商品价格：" + price);
        System.out.println("商品库存：" + stock);
    }

    public boolean hasStock() {
        return stock != null && stock > 0;
    }

    public void reduceStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            System.out.println("扣减数量必须大于0");
            return;
        }

        if (stock == null || stock < quantity) {
            System.out.println("库存不足");
            return;
        }

        stock -= quantity;
        System.out.println("库存扣减成功，剩余库存：" + stock);
    }
}
```

上面的代码中，`showInfo()` 没有返回值，用于输出商品信息；`hasStock()` 有返回值，用于判断是否有库存；`reduceStock()` 接收参数，用于扣减库存。

需要注意，这个类中使用了 `BigDecimal`，实际代码需要导入对应类。

完整代码如下。

文件位置：`src/main/java/io/github/atengk/oop/Product.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 商品类，用于演示成员方法
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Product {

    private String name;

    private BigDecimal price;

    private Integer stock;

    public Product(String name, BigDecimal price, Integer stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public void showInfo() {
        System.out.println("商品名称：" + name);
        System.out.println("商品价格：" + price);
        System.out.println("商品库存：" + stock);
    }

    public boolean hasStock() {
        return stock != null && stock > 0;
    }

    public void reduceStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            System.out.println("扣减数量必须大于0");
            return;
        }

        if (stock == null || stock < quantity) {
            System.out.println("库存不足");
            return;
        }

        stock -= quantity;
        System.out.println("库存扣减成功，剩余库存：" + stock);
    }
}
```

下面的代码演示如何调用商品对象中的成员方法。

文件位置：`src/main/java/io/github/atengk/oop/ProductDemo.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 商品成员方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ProductDemo {

    public static void main(String[] args) {
        Product product = new Product("机械键盘", new BigDecimal("299.00"), 10);

        product.showInfo();

        if (product.hasStock()) {
            product.reduceStock(2);
        }
    }
}
```

成员方法的作用是把对象相关的行为封装到类内部。调用方只需要通过对象调用方法，不需要关心对象内部如何处理数据。

### 构造方法

构造方法是一种特殊的方法，用于创建对象时初始化对象数据。

构造方法的特点如下：

| 特点                 | 说明                                       |
| -------------------- | ------------------------------------------ |
| 方法名必须和类名一致 | 例如类名是 `User`，构造方法也必须叫 `User` |
| 没有返回值类型       | 不能写 `void`                              |
| 创建对象时自动调用   | 执行 `new User()` 时会调用构造方法         |
| 可以重载             | 一个类中可以有多个参数不同的构造方法       |

如果一个类中没有手动定义任何构造方法，Java 会默认提供一个无参构造方法。如果已经手动定义了构造方法，Java 不会再自动提供无参构造方法。

下面的代码演示无参构造方法和有参构造方法。

文件位置：`src/main/java/io/github/atengk/oop/Order.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 订单类，用于演示构造方法
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Order {

    private String orderNo;

    private BigDecimal amount;

    public Order() {
        System.out.println("无参构造方法被调用");
    }

    public Order(String orderNo, BigDecimal amount) {
        this.orderNo = orderNo;
        this.amount = amount;
        System.out.println("有参构造方法被调用");
    }

    public void showInfo() {
        System.out.println("订单编号：" + orderNo);
        System.out.println("订单金额：" + amount);
    }
}
```

下面的代码演示不同构造方法的调用方式。

文件位置：`src/main/java/io/github/atengk/oop/OrderDemo.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 订单构造方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class OrderDemo {

    public static void main(String[] args) {
        Order order1 = new Order();
        order1.showInfo();

        Order order2 = new Order("ORDER_1001", new BigDecimal("199.90"));
        order2.showInfo();
    }
}
```

构造方法通常用于对象初始化。例如创建订单对象时，可以直接传入订单编号和订单金额，避免先创建空对象再逐个赋值。

## 封装

封装是面向对象的核心特性之一。它的主要思想是把对象的属性和行为封装到类中，并通过访问修饰符控制外部代码对内部数据的访问。

实际开发中，通常不会把成员变量直接暴露为 `public`，而是使用 `private` 修饰成员变量，再通过公共方法提供受控访问。

### 封装的作用

封装的核心作用是隐藏内部实现细节，保护对象数据，并提供统一的访问入口。

如果成员变量可以被外部随意修改，就可能出现不合法的数据。例如用户年龄被设置为 `-1`，账户余额被直接改成负数，这些都会破坏对象状态。

下面是不推荐的写法。

文件位置：`src/main/java/io/github/atengk/oop/BadAccount.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 不推荐的账户类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BadAccount {

    public String accountNo;

    public BigDecimal balance;
}
```

这种写法中，外部代码可以直接修改余额。

```java
BadAccount account = new BadAccount();
account.balance = new BigDecimal("-1000");
```

这显然是不合理的。通过封装，可以把余额设置为私有属性，并通过方法控制修改规则。

文件位置：`src/main/java/io/github/atengk/oop/BankAccount.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 银行账户类，用于演示封装
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BankAccount {

    private String accountNo;

    private BigDecimal balance;

    public BankAccount(String accountNo, BigDecimal balance) {
        if (accountNo == null || accountNo.isBlank()) {
            throw new IllegalArgumentException("账户编号不能为空");
        }
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("账户余额不能为负数");
        }

        this.accountNo = accountNo;
        this.balance = balance;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("存款金额必须大于0");
            return;
        }

        balance = balance.add(amount);
        System.out.println("存款成功，当前余额：" + balance);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("取款金额必须大于0");
            return;
        }

        if (balance.compareTo(amount) < 0) {
            System.out.println("余额不足，取款失败");
            return;
        }

        balance = balance.subtract(amount);
        System.out.println("取款成功，当前余额：" + balance);
    }
}
```

通过封装，外部代码不能直接修改 `balance`，只能通过 `deposit()` 和 `withdraw()` 方法操作余额。这样可以在方法内部统一进行参数校验，保证对象数据合法。

封装的常见作用如下：

| 作用           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 保护数据安全   | 防止外部直接修改内部属性                       |
| 隐藏实现细节   | 外部只需要知道怎么调用，不需要知道内部怎么实现 |
| 提高代码维护性 | 内部逻辑变化时，减少对外部代码的影响           |
| 统一校验规则   | 可以在方法中集中处理参数校验                   |
| 降低耦合度     | 调用方依赖公共方法，而不是直接依赖内部字段     |

### 访问修饰符

访问修饰符用于控制类、成员变量、成员方法和构造方法的访问范围。

Java 中常见的访问修饰符包括 `public`、`protected`、默认访问权限和 `private`。

| 修饰符      | 当前类   | 同包类   | 子类               | 任意位置             |
| ----------- | -------- | -------- | ------------------ | -------------------- |
| `public`    | 可以访问 | 可以访问 | 可以访问           | 可以访问             |
| `protected` | 可以访问 | 可以访问 | 可以访问           | 不同包非子类不可访问 |
| 默认不写    | 可以访问 | 可以访问 | 不同包子类不可访问 | 不可访问             |
| `private`   | 可以访问 | 不可访问 | 不可访问           | 不可访问             |

实际开发中，常见使用习惯如下：

| 使用位置 | 推荐修饰符            | 说明                                          |
| -------- | --------------------- | --------------------------------------------- |
| 类       | `public`              | 对外暴露的类通常使用 `public`                 |
| 成员变量 | `private`             | 避免外部直接修改对象状态                      |
| 构造方法 | `public` 或 `private` | 普通对象通常 `public`，工具类可使用 `private` |
| 成员方法 | `public`、`private`   | 对外功能用 `public`，内部辅助方法用 `private` |

下面的代码演示不同访问修饰符的使用方式。

文件位置：`src/main/java/io/github/atengk/oop/AccessModifierDemo.java`

```java
package io.github.atengk.oop;

/**
 * 访问修饰符示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AccessModifierDemo {

    public String publicValue = "public";

    protected String protectedValue = "protected";

    String defaultValue = "default";

    private String privateValue = "private";

    public void publicMethod() {
        System.out.println("public方法可以被任意位置访问");
    }

    protected void protectedMethod() {
        System.out.println("protected方法可以被同包类或子类访问");
    }

    void defaultMethod() {
        System.out.println("默认方法只能被同包类访问");
    }

    private void privateMethod() {
        System.out.println("private方法只能在当前类内部访问");
    }

    public void showPrivateValue() {
        System.out.println(privateValue);
        privateMethod();
    }
}
```

封装中最常用的是 `private` 和 `public`。成员变量通常使用 `private`，对外提供的业务方法通常使用 `public`。

### Getter 与 Setter

Getter 和 Setter 是访问和修改私有成员变量的常见方法。

Getter 用于获取属性值，Setter 用于设置属性值。它们可以让外部代码在不直接访问成员变量的情况下，对对象属性进行读取和修改。

基本命名规则如下：

| 方法类型       | 命名规则                 | 示例                           |
| -------------- | ------------------------ | ------------------------------ |
| Getter         | `get + 属性名首字母大写` | `getUsername()`                |
| Setter         | `set + 属性名首字母大写` | `setUsername(String username)` |
| Boolean Getter | `is + 属性名首字母大写`  | `isEnabled()`                  |

下面的代码演示 Getter 和 Setter 的基本写法。

文件位置：`src/main/java/io/github/atengk/oop/UserProfile.java`

```java
package io.github.atengk.oop;

/**
 * 用户资料类，用于演示Getter和Setter
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserProfile {

    private String username;

    private Integer age;

    private boolean enabled;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            System.out.println("用户名不能为空");
            return;
        }
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        if (age == null || age < 0 || age > 150) {
            System.out.println("年龄不合法");
            return;
        }
        this.age = age;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

下面的代码演示 Getter 和 Setter 的调用。

文件位置：`src/main/java/io/github/atengk/oop/UserProfileDemo.java`

```java
package io.github.atengk.oop;

/**
 * 用户资料Getter和Setter调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UserProfileDemo {

    public static void main(String[] args) {
        UserProfile userProfile = new UserProfile();

        userProfile.setUsername("ateng");
        userProfile.setAge(20);
        userProfile.setEnabled(true);

        System.out.println("用户名：" + userProfile.getUsername());
        System.out.println("年龄：" + userProfile.getAge());
        System.out.println("是否启用：" + userProfile.isEnabled());
    }
}
```

Getter 和 Setter 不只是简单地读取和赋值，它们还可以加入参数校验、格式处理、默认值处理等逻辑。

例如 `setAge()` 方法中限制年龄必须在合理范围内，这样可以避免对象中出现非法数据。

### this 关键字

`this` 是 Java 中的关键字，表示当前对象本身。

在类的成员方法或构造方法中，可以使用 `this` 访问当前对象的成员变量和成员方法。

`this` 常见使用场景如下：

| 使用场景               | 说明                                           |
| ---------------------- | ---------------------------------------------- |
| 区分成员变量和局部变量 | 当变量名相同时，`this.name` 表示成员变量       |
| 调用当前对象的方法     | 可以使用 `this.method()`                       |
| 调用本类其他构造方法   | 可以使用 `this(...)`，但必须放在构造方法第一行 |
| 返回当前对象           | 常用于链式调用                                 |

最常见的场景是构造方法或 Setter 方法中的参数名和成员变量名相同。

文件位置：`src/main/java/io/github/atengk/oop/ThisUser.java`

```java
package io.github.atengk.oop;

/**
 * this关键字示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThisUser {

    private String username;

    private Integer age;

    public ThisUser() {
        this("默认用户", 18);
    }

    public ThisUser(String username, Integer age) {
        this.username = username;
        this.age = age;
    }

    public void updateAge(Integer age) {
        this.age = age;
    }

    public void showInfo() {
        this.printLine();
        System.out.println("用户名：" + this.username);
        System.out.println("年龄：" + this.age);
        this.printLine();
    }

    private void printLine() {
        System.out.println("--------------------");
    }
}
```

下面的代码演示 `this` 的使用效果。

文件位置：`src/main/java/io/github/atengk/oop/ThisUserDemo.java`

```java
package io.github.atengk.oop;

/**
 * this关键字调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThisUserDemo {

    public static void main(String[] args) {
        ThisUser user1 = new ThisUser();
        user1.showInfo();

        ThisUser user2 = new ThisUser("张三", 22);
        user2.updateAge(23);
        user2.showInfo();
    }
}
```

在 `ThisUser(String username, Integer age)` 构造方法中，参数名和成员变量名相同。此时 `this.username` 表示当前对象的成员变量，`username` 表示构造方法的参数。

如果不使用 `this`，代码就会变成参数自己给自己赋值，成员变量不会被正确初始化。

错误示例：

```java
public ThisUser(String username, Integer age) {
    username = username;
    age = age;
}
```

这种写法不会把参数值赋给成员变量，因此实际开发中，当成员变量和局部变量同名时，应使用 `this` 明确区分。

## 继承

继承是 Java 面向对象的重要特性之一。它允许一个类继承另一个类的属性和方法，从而实现代码复用和功能扩展。

在继承关系中，被继承的类称为父类、基类或超类；继承父类的类称为子类或派生类。

### 继承的基本语法

Java 中使用 `extends` 关键字表示继承关系。

基本语法如下：

```java
public class 子类 extends 父类 {
    子类自己的成员变量和成员方法;
}
```

下面的代码定义一个动物父类。

文件位置：`src/main/java/io/github/atengk/oop/Animal.java`

```java
package io.github.atengk.oop;

/**
 * 动物父类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Animal {

    private String name;

    public Animal(String name) {
        this.name = name;
    }

    public void eat() {
        System.out.println(name + "正在吃东西");
    }

    public void sleep() {
        System.out.println(name + "正在睡觉");
    }

    public String getName() {
        return name;
    }
}
```

下面的代码定义一个狗子类，继承 `Animal` 父类。

文件位置：`src/main/java/io/github/atengk/oop/Dog.java`

```java
package io.github.atengk.oop;

/**
 * 狗子类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Dog extends Animal {

    public Dog(String name) {
        super(name);
    }

    public void bark() {
        System.out.println(getName() + "正在汪汪叫");
    }
}
```

下面的代码演示继承后的方法调用。

文件位置：`src/main/java/io/github/atengk/oop/ExtendsDemo.java`

```java
package io.github.atengk.oop;

/**
 * 继承调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ExtendsDemo {

    public static void main(String[] args) {
        Dog dog = new Dog("旺财");

        dog.eat();
        dog.sleep();
        dog.bark();
    }
}
```

`Dog` 类继承了 `Animal` 类，所以 `Dog` 对象可以调用父类中的 `eat()` 和 `sleep()` 方法，也可以调用自己定义的 `bark()` 方法。

继承的主要作用如下：

| 作用     | 说明                                     |
| -------- | ---------------------------------------- |
| 代码复用 | 子类可以直接使用父类中非私有的成员       |
| 功能扩展 | 子类可以在父类基础上新增自己的属性和方法 |
| 统一抽象 | 可以把多个子类的公共内容抽取到父类中     |
| 支持多态 | 父类引用可以指向子类对象                 |

需要注意的是，Java 只支持单继承，一个类只能直接继承一个父类。

错误示例：

```java
public class Dog extends Animal, Pet {
}
```

Java 不允许一个类同时继承多个父类。如果需要实现多种能力，可以使用接口。

### 方法重写

方法重写是指子类重新定义父类中已有的方法。

当父类中的方法不能满足子类需求时，子类可以对该方法进行重写。重写后，子类对象调用该方法时，会执行子类自己的实现。

方法重写需要满足以下规则：

| 规则                 | 说明                                           |
| -------------------- | ---------------------------------------------- |
| 方法名相同           | 子类方法名必须和父类方法名一致                 |
| 参数列表相同         | 参数个数、类型、顺序必须一致                   |
| 返回值类型兼容       | 返回值类型必须相同，或是父类返回值类型的子类型 |
| 访问权限不能更小     | 子类方法访问权限不能比父类更严格               |
| 建议使用 `@Override` | 可以让编译器检查是否真的完成重写               |

下面的代码定义一个父类方法。

文件位置：`src/main/java/io/github/atengk/oop/BaseAnimal.java`

```java
package io.github.atengk.oop;

/**
 * 基础动物父类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BaseAnimal {

    public void sound() {
        System.out.println("动物发出声音");
    }
}
```

下面的代码演示子类重写父类方法。

文件位置：`src/main/java/io/github/atengk/oop/BaseDog.java`

```java
package io.github.atengk.oop;

/**
 * 基础狗子类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BaseDog extends BaseAnimal {

    @Override
    public void sound() {
        System.out.println("狗发出汪汪声");
    }
}
```

下面的代码演示方法重写后的调用效果。

文件位置：`src/main/java/io/github/atengk/oop/OverrideDemo.java`

```java
package io.github.atengk.oop;

/**
 * 方法重写调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class OverrideDemo {

    public static void main(String[] args) {
        BaseAnimal animal = new BaseAnimal();
        animal.sound();

        BaseDog dog = new BaseDog();
        dog.sound();
    }
}
```

运行结果如下：

```text
动物发出声音
狗发出汪汪声
```

方法重写常用于子类根据自身特点改写父类行为。例如所有动物都会发出声音，但狗、猫、鸟发出的声音不同，这时就可以让不同子类重写 `sound()` 方法。

### super 关键字

`super` 是 Java 中用于访问父类内容的关键字。

在子类中，可以使用 `super` 访问父类的成员变量、成员方法和构造方法。

常见用法如下：

| 用法               | 说明                                         |
| ------------------ | -------------------------------------------- |
| `super.成员变量`   | 访问父类成员变量                             |
| `super.成员方法()` | 调用父类成员方法                             |
| `super(...)`       | 调用父类构造方法，必须放在子类构造方法第一行 |

下面的代码演示 `super` 调用父类构造方法和父类成员方法。

文件位置：`src/main/java/io/github/atengk/oop/ParentAnimal.java`

```java
package io.github.atengk.oop;

/**
 * 父类动物
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ParentAnimal {

    private String name;

    public ParentAnimal(String name) {
        this.name = name;
        System.out.println("父类构造方法执行");
    }

    public void eat() {
        System.out.println(name + "正在吃东西");
    }

    public String getName() {
        return name;
    }
}
```

下面的代码演示子类中使用 `super`。

文件位置：`src/main/java/io/github/atengk/oop/ChildDog.java`

```java
package io.github.atengk.oop;

/**
 * 子类狗
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ChildDog extends ParentAnimal {

    public ChildDog(String name) {
        super(name);
        System.out.println("子类构造方法执行");
    }

    public void eat() {
        super.eat();
        System.out.println(getName() + "吃完后摇尾巴");
    }
}
```

下面的代码演示 `super` 的调用效果。

文件位置：`src/main/java/io/github/atengk/oop/SuperDemo.java`

```java
package io.github.atengk.oop;

/**
 * super关键字调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SuperDemo {

    public static void main(String[] args) {
        ChildDog dog = new ChildDog("旺财");
        dog.eat();
    }
}
```

运行结果如下：

```text
父类构造方法执行
子类构造方法执行
旺财正在吃东西
旺财吃完后摇尾巴
```

在 `ChildDog` 的构造方法中，`super(name)` 表示调用父类 `ParentAnimal` 的有参构造方法。在 `eat()` 方法中，`super.eat()` 表示先调用父类的吃东西逻辑，再执行子类自己的扩展逻辑。

`super` 和 `this` 的区别如下：

| 对比项         | `this`                   | `super`                   |
| -------------- | ------------------------ | ------------------------- |
| 含义           | 当前对象本身             | 当前对象中的父类部分      |
| 调用成员变量   | 当前类成员变量           | 父类成员变量              |
| 调用成员方法   | 当前类成员方法           | 父类成员方法              |
| 调用构造方法   | 调用本类其他构造方法     | 调用父类构造方法          |
| 构造方法中位置 | `this(...)` 必须在第一行 | `super(...)` 必须在第一行 |

需要注意，`this(...)` 和 `super(...)` 都要求必须写在构造方法第一行，因此二者不能同时出现在同一个构造方法中。

### 继承关系中的构造方法

在继承关系中，创建子类对象时，会先调用父类构造方法，再调用子类构造方法。

这是因为子类对象中包含父类继承下来的内容，必须先完成父类部分的初始化，再初始化子类自己的内容。

如果父类有无参构造方法，子类构造方法中即使不写 `super()`，Java 也会默认调用父类无参构造方法。

下面的代码演示默认调用父类无参构造方法。

文件位置：`src/main/java/io/github/atengk/oop/ConstructorParent.java`

```java
package io.github.atengk.oop;

/**
 * 构造方法父类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConstructorParent {

    public ConstructorParent() {
        System.out.println("父类无参构造方法");
    }
}
```

文件位置：`src/main/java/io/github/atengk/oop/ConstructorChild.java`

```java
package io.github.atengk.oop;

/**
 * 构造方法子类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConstructorChild extends ConstructorParent {

    public ConstructorChild() {
        System.out.println("子类无参构造方法");
    }
}
```

文件位置：`src/main/java/io/github/atengk/oop/ConstructorDemo.java`

```java
package io.github.atengk.oop;

/**
 * 继承构造方法调用顺序示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConstructorDemo {

    public static void main(String[] args) {
        ConstructorChild child = new ConstructorChild();
    }
}
```

运行结果如下：

```text
父类无参构造方法
子类无参构造方法
```

如果父类没有无参构造方法，只有有参构造方法，那么子类必须在构造方法中使用 `super(...)` 显式调用父类有参构造方法。

下面的代码演示这种情况。

文件位置：`src/main/java/io/github/atengk/oop/Person.java`

```java
package io.github.atengk.oop;

/**
 * 人员父类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Person {

    private String name;

    public Person(String name) {
        this.name = name;
        System.out.println("Person有参构造方法");
    }

    public String getName() {
        return name;
    }
}
```

文件位置：`src/main/java/io/github/atengk/oop/Teacher.java`

```java
package io.github.atengk.oop;

/**
 * 教师子类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class Teacher extends Person {

    private String subject;

    public Teacher(String name, String subject) {
        super(name);
        this.subject = subject;
        System.out.println("Teacher有参构造方法");
    }

    public void teach() {
        System.out.println(getName() + "正在教授" + subject);
    }
}
```

文件位置：`src/main/java/io/github/atengk/oop/TeacherDemo.java`

```java
package io.github.atengk.oop;

/**
 * 教师构造方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TeacherDemo {

    public static void main(String[] args) {
        Teacher teacher = new Teacher("李老师", "Java基础");
        teacher.teach();
    }
}
```

运行结果如下：

```text
Person有参构造方法
Teacher有参构造方法
李老师正在教授Java基础
```

继承关系中的构造方法需要注意以下几点：

| 注意点                         | 说明                                                    |
| ------------------------------ | ------------------------------------------------------- |
| 构造方法不能被继承             | 子类不能继承父类构造方法                                |
| 创建子类对象会先初始化父类     | 先执行父类构造方法，再执行子类构造方法                  |
| 子类默认调用父类无参构造       | 如果子类构造方法没有写 `super(...)`，默认调用 `super()` |
| 父类没有无参构造时必须显式调用 | 子类必须使用 `super(...)` 调用父类有参构造              |
| `super(...)` 必须在第一行      | 否则编译报错                                            |

继承可以减少重复代码，但不能滥用。实际开发中，只有当多个类之间确实存在“is-a”关系时，才适合使用继承。例如狗是动物、猫是动物，这种关系适合继承；订单和用户之间没有“is-a”关系，就不应该使用继承。



## 多态

多态是 Java 面向对象的核心特性之一。它表示同一个父类引用，在运行时可以指向不同的子类对象，并表现出不同的行为。

多态可以让程序面向父类或接口编程，而不是强依赖具体实现类。这样在后续新增实现类时，原有调用代码通常不需要大量修改。

### 多态的基本概念

多态的字面意思是“多种形态”。在 Java 中，多态通常表现为：父类引用指向子类对象，调用方法时执行的是子类重写后的方法。

多态成立通常需要满足三个条件：

| 条件                 | 说明                                            |
| -------------------- | ----------------------------------------------- |
| 存在继承或实现关系   | 子类继承父类，或者类实现接口                    |
| 存在方法重写         | 子类重写父类或接口中的方法                      |
| 父类引用指向子类对象 | 例如 `PayChannel channel = new AliPayChannel()` |

下面通过支付渠道示例演示多态。

下面的代码定义支付渠道父类，提供统一的支付方法。

文件位置：`src/main/java/io/github/atengk/oop/PayChannel.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 支付渠道父类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PayChannel {

    public void pay(BigDecimal amount) {
        System.out.println("使用默认支付渠道支付：" + amount + "元");
    }
}
```

下面的代码定义支付宝支付渠道，并重写父类的支付方法。

文件位置：`src/main/java/io/github/atengk/oop/AliPayChannel.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 支付宝支付渠道
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AliPayChannel extends PayChannel {

    @Override
    public void pay(BigDecimal amount) {
        System.out.println("使用支付宝支付：" + amount + "元");
    }

    public void queryAliPayBill() {
        System.out.println("查询支付宝账单");
    }
}
```

下面的代码定义微信支付渠道，并重写父类的支付方法。

文件位置：`src/main/java/io/github/atengk/oop/WechatPayChannel.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 微信支付渠道
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class WechatPayChannel extends PayChannel {

    @Override
    public void pay(BigDecimal amount) {
        System.out.println("使用微信支付：" + amount + "元");
    }

    public void queryWechatBill() {
        System.out.println("查询微信账单");
    }
}
```

下面的代码演示父类引用指向不同子类对象时的执行效果。

文件位置：`src/main/java/io/github/atengk/oop/PolymorphismDemo.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 多态调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PolymorphismDemo {

    public static void main(String[] args) {
        PayChannel aliPay = new AliPayChannel();
        PayChannel wechatPay = new WechatPayChannel();

        aliPay.pay(new BigDecimal("100.00"));
        wechatPay.pay(new BigDecimal("200.00"));
    }
}
```

运行结果如下：

```text
使用支付宝支付：100.00元
使用微信支付：200.00元
```

虽然变量类型都是 `PayChannel`，但是实际对象分别是 `AliPayChannel` 和 `WechatPayChannel`，因此调用 `pay()` 方法时，会根据实际对象类型执行不同的重写逻辑。

多态的核心价值是降低代码耦合。调用方只需要关注父类或接口定义的方法，不需要关心具体是哪一种子类实现。

### 向上转型

向上转型是指把子类对象赋值给父类引用。

基本语法如下：

```java
父类类型 变量名 = new 子类类型();
```

例如：

```java
PayChannel channel = new AliPayChannel();
```

这里 `AliPayChannel` 是子类，`PayChannel` 是父类。把子类对象交给父类引用保存，就是向上转型。

向上转型是自动完成的，不需要强制类型转换。因为子类本身就是一种父类类型，例如支付宝支付渠道本质上也是一种支付渠道。

下面的代码演示向上转型。

文件位置：`src/main/java/io/github/atengk/oop/UpcastingDemo.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 向上转型示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UpcastingDemo {

    public static void main(String[] args) {
        PayChannel channel = new AliPayChannel();
        channel.pay(new BigDecimal("99.90"));
    }
}
```

运行结果如下：

```text
使用支付宝支付：99.90元
```

向上转型后，父类引用只能调用父类中声明过的方法。即使实际对象是 `AliPayChannel`，也不能直接调用 `AliPayChannel` 独有的方法。

例如下面的代码会编译报错：

```java
PayChannel channel = new AliPayChannel();
channel.queryAliPayBill();
```

原因是 `channel` 的编译类型是 `PayChannel`，而 `PayChannel` 中没有定义 `queryAliPayBill()` 方法。

向上转型的优点是可以让方法接收父类类型，从而兼容所有子类对象。

下面的代码演示通过父类参数接收不同支付渠道。

文件位置：`src/main/java/io/github/atengk/oop/PaymentService.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 支付服务类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PaymentService {

    public void submitPay(PayChannel payChannel, BigDecimal amount) {
        if (payChannel == null) {
            System.out.println("支付渠道不能为空");
            return;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("支付金额必须大于0");
            return;
        }

        payChannel.pay(amount);
    }
}
```

下面的代码演示同一个服务方法接收不同子类对象。

文件位置：`src/main/java/io/github/atengk/oop/PaymentServiceDemo.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 支付服务调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PaymentServiceDemo {

    public static void main(String[] args) {
        PaymentService paymentService = new PaymentService();

        paymentService.submitPay(new AliPayChannel(), new BigDecimal("88.00"));
        paymentService.submitPay(new WechatPayChannel(), new BigDecimal("66.00"));
    }
}
```

`submitPay()` 方法的参数类型是 `PayChannel`，因此它可以接收 `AliPayChannel`、`WechatPayChannel` 等所有 `PayChannel` 的子类对象。这就是多态在实际开发中的常见应用方式。

### 向下转型

向下转型是指把父类引用转换回子类类型。

基本语法如下：

```java
子类类型 变量名 = (子类类型) 父类引用;
```

向下转型通常用于调用子类独有的方法。

例如，向上转型后，父类引用不能直接调用 `AliPayChannel` 独有的 `queryAliPayBill()` 方法。如果确实需要调用，就需要向下转型。

下面的代码演示向下转型。

文件位置：`src/main/java/io/github/atengk/oop/DowncastingDemo.java`

```java
package io.github.atengk.oop;

/**
 * 向下转型示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DowncastingDemo {

    public static void main(String[] args) {
        PayChannel channel = new AliPayChannel();

        AliPayChannel aliPayChannel = (AliPayChannel) channel;
        aliPayChannel.queryAliPayBill();
    }
}
```

运行结果如下：

```text
查询支付宝账单
```

向下转型需要注意类型安全。如果父类引用实际指向的对象不是目标子类类型，强制转换时会出现 `ClassCastException`。

下面是错误示例：

```java
PayChannel channel = new WechatPayChannel();

AliPayChannel aliPayChannel = (AliPayChannel) channel;
aliPayChannel.queryAliPayBill();
```

上面的代码可以通过编译，但运行时会报错。因为 `channel` 实际指向的是 `WechatPayChannel` 对象，不能强制转换成 `AliPayChannel`。

因此，向下转型之前通常需要使用 `instanceof` 进行类型判断。

### instanceof 关键字

`instanceof` 用于判断一个对象是否属于某个类或其子类类型，也可以判断对象是否实现了某个接口。

基本语法如下：

```java
对象 instanceof 类型
```

如果对象属于该类型，结果为 `true`；否则结果为 `false`。

下面的代码演示传统写法。

文件位置：`src/main/java/io/github/atengk/oop/InstanceofDemo.java`

```java
package io.github.atengk.oop;

/**
 * instanceof关键字示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class InstanceofDemo {

    public static void main(String[] args) {
        PayChannel channel = new AliPayChannel();

        if (channel instanceof AliPayChannel) {
            AliPayChannel aliPayChannel = (AliPayChannel) channel;
            aliPayChannel.queryAliPayBill();
        } else if (channel instanceof WechatPayChannel) {
            WechatPayChannel wechatPayChannel = (WechatPayChannel) channel;
            wechatPayChannel.queryWechatBill();
        } else {
            System.out.println("未知支付渠道");
        }
    }
}
```

在 Java 16 及以上版本中，可以使用模式匹配写法，判断类型的同时完成变量转换。

文件位置：`src/main/java/io/github/atengk/oop/InstanceofPatternDemo.java`

```java
package io.github.atengk.oop;

/**
 * instanceof模式匹配示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class InstanceofPatternDemo {

    public static void main(String[] args) {
        PayChannel channel = new WechatPayChannel();

        if (channel instanceof AliPayChannel aliPayChannel) {
            aliPayChannel.queryAliPayBill();
        } else if (channel instanceof WechatPayChannel wechatPayChannel) {
            wechatPayChannel.queryWechatBill();
        } else {
            System.out.println("未知支付渠道");
        }
    }
}
```

`instanceof` 常用于向下转型前的类型检查，可以避免类型转换异常。

不过在实际开发中，如果频繁使用 `instanceof` 判断不同子类类型，往往说明代码设计可能不够面向对象。很多情况下，可以通过方法重写、多态调用、接口抽象等方式减少显式类型判断。

## 抽象类

抽象类是 Java 中一种不能直接创建对象的类。它主要用于抽取多个子类的公共属性和公共行为，同时允许定义必须由子类实现的抽象方法。

抽象类适合表达一类事物的共同模板。例如不同报表导出器都有“校验文件名、导出报表”的流程，但具体导出 Excel、PDF、CSV 的实现不同，此时可以使用抽象类。

### 抽象类的定义

在 Java 中，使用 `abstract` 关键字修饰类，就可以定义抽象类。

基本语法如下：

```java
public abstract class 类名 {
    普通成员变量;
    普通成员方法;
    抽象方法;
}
```

抽象类的特点如下：

| 特点                 | 说明                             |
| -------------------- | -------------------------------- |
| 不能直接创建对象     | 不能使用 `new 抽象类()`          |
| 可以有成员变量       | 和普通类一样可以定义属性         |
| 可以有构造方法       | 子类创建对象时会调用父类构造方法 |
| 可以有普通方法       | 普通方法可以直接写完整实现       |
| 可以有抽象方法       | 抽象方法没有方法体，交给子类实现 |
| 子类必须实现抽象方法 | 除非子类本身也是抽象类           |

下面的代码定义一个报表导出抽象类。

文件位置：`src/main/java/io/github/atengk/oop/AbstractReportExporter.java`

```java
package io.github.atengk.oop;

/**
 * 报表导出抽象类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public abstract class AbstractReportExporter {

    private String reportName;

    public AbstractReportExporter(String reportName) {
        this.reportName = reportName;
    }

    public void export(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            System.out.println("导出文件名不能为空");
            return;
        }

        System.out.println("开始导出报表：" + reportName);
        doExport(fileName);
        System.out.println("报表导出完成：" + fileName);
    }

    public String getReportName() {
        return reportName;
    }

    protected abstract void doExport(String fileName);
}
```

这个类使用 `abstract` 修饰，因此它是抽象类，不能直接创建对象。

错误示例：

```java
AbstractReportExporter exporter = new AbstractReportExporter("用户报表");
```

上面的代码无法通过编译，因为抽象类不能直接实例化。

抽象类虽然不能直接创建对象，但它可以通过子类对象间接使用。

下面的代码定义 Excel 报表导出子类。

文件位置：`src/main/java/io/github/atengk/oop/ExcelReportExporter.java`

```java
package io.github.atengk.oop;

/**
 * Excel报表导出器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ExcelReportExporter extends AbstractReportExporter {

    public ExcelReportExporter(String reportName) {
        super(reportName);
    }

    @Override
    protected void doExport(String fileName) {
        System.out.println("按照Excel格式导出文件：" + fileName);
    }
}
```

下面的代码定义 PDF 报表导出子类。

文件位置：`src/main/java/io/github/atengk/oop/PdfReportExporter.java`

```java
package io.github.atengk.oop;

/**
 * PDF报表导出器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PdfReportExporter extends AbstractReportExporter {

    public PdfReportExporter(String reportName) {
        super(reportName);
    }

    @Override
    protected void doExport(String fileName) {
        System.out.println("按照PDF格式导出文件：" + fileName);
    }
}
```

下面的代码演示抽象类的使用方式。

文件位置：`src/main/java/io/github/atengk/oop/AbstractClassDemo.java`

```java
package io.github.atengk.oop;

/**
 * 抽象类调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AbstractClassDemo {

    public static void main(String[] args) {
        AbstractReportExporter excelExporter = new ExcelReportExporter("用户统计报表");
        AbstractReportExporter pdfExporter = new PdfReportExporter("订单统计报表");

        excelExporter.export("user-report.xlsx");
        pdfExporter.export("order-report.pdf");
    }
}
```

在这个示例中，`AbstractReportExporter` 负责定义公共导出流程，`ExcelReportExporter` 和 `PdfReportExporter` 负责实现具体导出格式。

### 抽象方法

抽象方法是只有方法声明、没有方法体的方法。它必须使用 `abstract` 关键字修饰，并且只能定义在抽象类或接口中。

基本语法如下：

```java
访问修饰符 abstract 返回值类型 方法名(参数列表);
```

例如：

```java
protected abstract void doExport(String fileName);
```

抽象方法的特点如下：

| 特点                   | 说明                             |
| ---------------------- | -------------------------------- |
| 没有方法体             | 不能写 `{}`                      |
| 必须用 `abstract` 修饰 | 抽象方法需要明确声明             |
| 所在类必须是抽象类     | 普通类中不能定义抽象方法         |
| 子类必须重写           | 普通子类必须实现父类所有抽象方法 |

下面是错误写法：

```java
public abstract void doExport(String fileName) {
    System.out.println("导出文件");
}
```

抽象方法不能有方法体，因此上面的写法是错误的。

下面也是错误写法：

```java
public class ReportExporter {

    public abstract void doExport(String fileName);
}
```

普通类中不能定义抽象方法。如果类中包含抽象方法，这个类也必须声明为抽象类。

抽象方法的作用是定义规范。父类只规定子类必须具备某种行为，但不关心具体怎么实现。

例如，所有报表导出器都必须具备“执行导出”的能力，但 Excel、PDF、CSV 的导出方式不同，因此父类可以定义抽象方法，由不同子类分别实现。

### 抽象类的使用场景

抽象类通常用于“有共同属性、共同方法，并且存在部分行为需要子类自己实现”的场景。

常见使用场景如下：

| 场景         | 说明                                     |
| ------------ | ---------------------------------------- |
| 抽取公共属性 | 多个子类都有相同字段，可以放到抽象父类中 |
| 抽取公共方法 | 多个子类有相同逻辑，可以放到抽象父类中   |
| 定义通用流程 | 父类固定整体流程，子类实现部分步骤       |
| 约束子类行为 | 父类定义抽象方法，要求子类必须实现       |
| 配合多态使用 | 使用抽象父类引用接收不同子类对象         |

抽象类非常适合实现模板方法模式。模板方法模式是指：父类定义固定执行流程，流程中的某些具体步骤延迟到子类实现。

前面的 `AbstractReportExporter` 就是一个简单的模板方法示例：

```java
public void export(String fileName) {
    if (fileName == null || fileName.isBlank()) {
        System.out.println("导出文件名不能为空");
        return;
    }

    System.out.println("开始导出报表：" + reportName);
    doExport(fileName);
    System.out.println("报表导出完成：" + fileName);
}
```

其中，`export()` 是固定流程，`doExport()` 是变化点。不同子类只需要实现自己的导出方式即可。

抽象类适合表达“是什么”的继承关系。例如：

| 抽象类                   | 子类                                       |
| ------------------------ | ------------------------------------------ |
| `AbstractReportExporter` | `ExcelReportExporter`、`PdfReportExporter` |
| `AbstractUserService`    | `AdminUserService`、`MemberUserService`    |
| `AbstractMessageHandler` | `EmailMessageHandler`、`SmsMessageHandler` |

如果多个类之间存在明显的共同父类，并且需要复用成员变量或普通方法，可以优先考虑抽象类。

## 接口

接口是 Java 中用于定义行为规范的一种类型。它强调某个类“能做什么”，而不是“是什么”。

接口常用于定义能力、规范、扩展点和解耦边界。例如短信发送、邮件发送、微信消息发送都可以抽象成“消息发送能力”。

### 接口的定义

在 Java 中，使用 `interface` 关键字定义接口。

基本语法如下：

```java
public interface 接口名 {
    方法声明;
}
```

接口名通常使用大驼峰命名法。对于表示能力的接口，常见命名方式有 `XxxService`、`XxxHandler`、`XxxProcessor`、`XxxSender` 等。

下面的代码定义一个消息发送接口。

文件位置：`src/main/java/io/github/atengk/oop/MessageSender.java`

```java
package io.github.atengk.oop;

/**
 * 消息发送接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface MessageSender {

    void send(String receiver, String content);
}
```

接口本身不能直接创建对象。

错误示例：

```java
MessageSender sender = new MessageSender();
```

接口需要由类使用 `implements` 关键字实现。

下面的代码定义邮件消息发送实现类。

文件位置：`src/main/java/io/github/atengk/oop/EmailMessageSender.java`

```java
package io.github.atengk.oop;

/**
 * 邮件消息发送器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EmailMessageSender implements MessageSender {

    @Override
    public void send(String receiver, String content) {
        System.out.println("发送邮件给：" + receiver + "，内容：" + content);
    }
}
```

下面的代码定义短信消息发送实现类。

文件位置：`src/main/java/io/github/atengk/oop/SmsMessageSender.java`

```java
package io.github.atengk.oop;

/**
 * 短信消息发送器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class SmsMessageSender implements MessageSender {

    @Override
    public void send(String receiver, String content) {
        System.out.println("发送短信给：" + receiver + "，内容：" + content);
    }
}
```

下面的代码演示接口的使用方式。

文件位置：`src/main/java/io/github/atengk/oop/InterfaceDemo.java`

```java
package io.github.atengk.oop;

/**
 * 接口调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class InterfaceDemo {

    public static void main(String[] args) {
        MessageSender emailSender = new EmailMessageSender();
        MessageSender smsSender = new SmsMessageSender();

        emailSender.send("admin@example.com", "系统运行正常");
        smsSender.send("13800000000", "验证码：123456");
    }
}
```

在这个示例中，变量类型都是 `MessageSender`，实际对象分别是 `EmailMessageSender` 和 `SmsMessageSender`。这也是多态的一种体现。

### 接口方法

接口中的方法主要包括抽象方法、默认方法和静态方法。

在 Java 8 之前，接口中主要只能定义抽象方法。Java 8 开始，接口支持默认方法和静态方法。

接口中的抽象方法默认使用 `public abstract` 修饰，因此可以省略不写。

下面两种写法含义相同：

```java
void send(String receiver, String content);
public abstract void send(String receiver, String content);
```

接口中的成员变量默认使用 `public static final` 修饰，也就是公共静态常量。

下面两种写法含义相同：

```java
String DEFAULT_ENCODING = "UTF-8";
public static final String DEFAULT_ENCODING = "UTF-8";
```

下面的代码演示接口常量和抽象方法。

文件位置：`src/main/java/io/github/atengk/oop/FileStorage.java`

```java
package io.github.atengk.oop;

/**
 * 文件存储接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface FileStorage {

    String DEFAULT_BUCKET = "default";

    String upload(String fileName, byte[] content);

    boolean delete(String fileName);
}
```

下面的代码定义本地文件存储实现类。

文件位置：`src/main/java/io/github/atengk/oop/LocalFileStorage.java`

```java
package io.github.atengk.oop;

/**
 * 本地文件存储实现类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LocalFileStorage implements FileStorage {

    @Override
    public String upload(String fileName, byte[] content) {
        if (fileName == null || fileName.isBlank()) {
            return "文件名不能为空";
        }

        if (content == null || content.length == 0) {
            return "文件内容不能为空";
        }

        return "文件已上传到本地存储：" + DEFAULT_BUCKET + "/" + fileName;
    }

    @Override
    public boolean delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            System.out.println("文件名不能为空");
            return false;
        }

        System.out.println("删除本地文件：" + fileName);
        return true;
    }
}
```

一个类实现接口后，必须实现接口中的所有抽象方法。否则，这个类也必须声明为抽象类。

错误示例：

```java
public class LocalFileStorage implements FileStorage {
}
```

因为没有实现 `upload()` 和 `delete()` 方法，所以上面的代码会编译报错。

接口可以实现多态，也可以降低调用方和具体实现类之间的耦合。调用方只依赖接口，不直接依赖具体实现。

### 默认方法与静态方法

Java 8 开始，接口可以定义默认方法和静态方法。

默认方法使用 `default` 关键字修饰。它可以有方法体，实现类可以直接继承，也可以选择重写。

静态方法使用 `static` 关键字修饰。它属于接口本身，只能通过接口名调用，不能通过实现类对象调用。

下面的代码演示默认方法和静态方法。

文件位置：`src/main/java/io/github/atengk/oop/MessageFormatter.java`

```java
package io.github.atengk.oop;

/**
 * 消息格式化接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface MessageFormatter {

    String format(String content);

    default String formatWithPrefix(String content) {
        if (content == null || content.isBlank()) {
            return "[系统消息] 空消息";
        }
        return "[系统消息] " + format(content);
    }

    static boolean isBlank(String content) {
        return content == null || content.isBlank();
    }
}
```

下面的代码定义普通文本格式化实现类。

文件位置：`src/main/java/io/github/atengk/oop/TextMessageFormatter.java`

```java
package io.github.atengk.oop;

/**
 * 文本消息格式化器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TextMessageFormatter implements MessageFormatter {

    @Override
    public String format(String content) {
        if (MessageFormatter.isBlank(content)) {
            return "空消息";
        }
        return content.trim();
    }
}
```

下面的代码定义告警消息格式化实现类，并重写默认方法。

文件位置：`src/main/java/io/github/atengk/oop/AlarmMessageFormatter.java`

```java
package io.github.atengk.oop;

/**
 * 告警消息格式化器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AlarmMessageFormatter implements MessageFormatter {

    @Override
    public String format(String content) {
        if (MessageFormatter.isBlank(content)) {
            return "空告警";
        }
        return content.trim();
    }

    @Override
    public String formatWithPrefix(String content) {
        return "[告警消息] " + format(content);
    }
}
```

下面的代码演示默认方法和静态方法的调用。

文件位置：`src/main/java/io/github/atengk/oop/DefaultStaticMethodDemo.java`

```java
package io.github.atengk.oop;

/**
 * 接口默认方法和静态方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DefaultStaticMethodDemo {

    public static void main(String[] args) {
        MessageFormatter textFormatter = new TextMessageFormatter();
        MessageFormatter alarmFormatter = new AlarmMessageFormatter();

        System.out.println(textFormatter.formatWithPrefix("服务启动成功"));
        System.out.println(alarmFormatter.formatWithPrefix("CPU使用率过高"));

        System.out.println(MessageFormatter.isBlank(""));
    }
}
```

运行结果如下：

```text
[系统消息] 服务启动成功
[告警消息] CPU使用率过高
true
```

默认方法的主要作用是增强接口的扩展能力。假设一个接口已经有很多实现类，如果直接新增抽象方法，所有实现类都必须修改。而新增默认方法时，实现类可以不修改，直接继承默认实现。

静态方法通常用于放置与接口能力相关的工具逻辑。例如 `MessageFormatter.isBlank()` 用于判断消息内容是否为空，它和消息格式化能力相关，因此可以放在接口中。

需要注意的是，接口的静态方法不能被实现类重写。

### 接口与抽象类的区别

接口和抽象类都可以用于抽象设计，也都不能直接创建对象。但它们的设计侧重点不同。

抽象类更强调“是什么”，适合表达父子类之间的继承关系。接口更强调“能做什么”，适合表达能力、规范和扩展点。

二者常见区别如下：

| 对比项   | 抽象类                       | 接口                              |
| -------- | ---------------------------- | --------------------------------- |
| 关键字   | `abstract class`             | `interface`                       |
| 继承方式 | 子类使用 `extends`           | 实现类使用 `implements`           |
| 继承数量 | 一个类只能继承一个抽象类     | 一个类可以实现多个接口            |
| 设计重点 | 表示“是什么”                 | 表示“能做什么”                    |
| 成员变量 | 可以有普通成员变量           | 主要是 `public static final` 常量 |
| 构造方法 | 可以有构造方法               | 不能有构造方法                    |
| 普通方法 | 可以有普通方法               | Java 8 后支持默认方法、静态方法   |
| 抽象方法 | 可以有抽象方法               | 可以有抽象方法                    |
| 代码复用 | 更适合复用公共状态和公共逻辑 | 更适合定义行为规范                |
| 使用场景 | 父子类有共同属性和流程       | 多个类具备同一种能力              |

下面通过一个简单场景说明如何选择。

如果要描述“员工”这一类对象，不同员工都有姓名、工号、计算薪资的行为，那么可以使用抽象类。

```java
public abstract class AbstractEmployee {

    private String name;

    private String employeeNo;

    public abstract BigDecimal calculateSalary();
}
```

因为员工之间存在明显的“is-a”关系：正式员工是员工，兼职员工也是员工。

如果要描述“可导出”这种能力，不同类型的对象都可能具备导出能力，例如订单、用户、报表都可以导出，那么更适合使用接口。

```java
public interface Exportable {

    String export();
}
```

因为“可导出”不是一种具体事物，而是一种能力。

实际开发中可以按照下面的原则选择：

| 选择原则                         | 推荐方案   |
| -------------------------------- | ---------- |
| 多个类有共同属性和公共逻辑       | 使用抽象类 |
| 多个类只是具备相同行为能力       | 使用接口   |
| 需要单继承复用代码               | 使用抽象类 |
| 需要多种能力组合                 | 使用接口   |
| 需要定义扩展规范                 | 使用接口   |
| 需要固定流程并让子类实现部分步骤 | 使用抽象类 |

在 Java 项目中，接口使用频率通常更高，尤其是在分层架构中。例如 `UserService`、`OrderService`、`MessageHandler` 通常会设计为接口，再由具体实现类完成业务逻辑。

抽象类更适合放公共模板逻辑，例如基础控制器、基础服务类、通用处理器、模板方法流程等。

简单总结：

| 类型   | 适合解决的问题                           |
| ------ | ---------------------------------------- |
| 抽象类 | 解决“同类对象的公共代码复用和模板约束”   |
| 接口   | 解决“不同对象具备相同行为规范和能力扩展” |

接口和抽象类并不是互斥关系。实际开发中，经常会同时使用：接口定义能力规范，抽象类提供部分公共实现，具体类完成最终业务逻辑。



## 内部类

内部类是定义在另一个类内部的类。Java 中允许在类的内部继续定义类，用于表示两个类之间关系紧密，或者内部类只服务于外部类的场景。

内部类可以访问外部类的成员，能够增强封装性，但如果使用过多，也会让代码结构变复杂。因此内部类适合用于局部封装、辅助建模、事件回调、临时实现等场景。

Java 中常见的内部类包括成员内部类、静态内部类、局部内部类和匿名内部类。

### 成员内部类

成员内部类是定义在类的成员位置上的内部类，也就是定义在外部类中、方法外部的类。

成员内部类属于外部类的对象，需要先创建外部类对象，再通过外部类对象创建内部类对象。

基本语法如下：

```java
外部类.内部类 对象名 = 外部类对象.new 内部类();
```

下面的代码演示成员内部类的定义和使用。

文件位置：`src/main/java/io/github/atengk/oop/MemberInnerClassDemo.java`

```java
package io.github.atengk.oop;

/**
 * 成员内部类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MemberInnerClassDemo {

    private String username = "张三";

    public class UserAddress {

        private String city = "重庆";

        public void showAddress() {
            System.out.println(username + "居住在" + city);
        }
    }

    public static void main(String[] args) {
        MemberInnerClassDemo outer = new MemberInnerClassDemo();
        UserAddress address = outer.new UserAddress();

        address.showAddress();
    }
}
```

运行结果如下：

```text
张三居住在重庆
```

在这个示例中，`UserAddress` 是成员内部类，它可以直接访问外部类 `MemberInnerClassDemo` 中的成员变量 `username`。

成员内部类的特点如下：

| 特点                 | 说明                      |
| -------------------- | ------------------------- |
| 定义位置             | 定义在外部类中、方法外    |
| 是否依赖外部类对象   | 依赖                      |
| 是否能访问外部类成员 | 可以访问，包括私有成员    |
| 创建方式             | `外部类对象.new 内部类()` |
| 适合场景             | 内部类和外部类对象强相关  |

成员内部类适合表达“内部类依赖外部类对象状态”的场景。例如一个用户对象内部包含地址信息、订单对象内部包含订单明细等。

### 静态内部类

静态内部类是使用 `static` 修饰的内部类。它属于外部类本身，不依赖外部类对象。

静态内部类不能直接访问外部类的非静态成员。如果需要访问外部类的非静态成员，必须通过外部类对象访问。

基本语法如下：

```java
外部类.静态内部类 对象名 = new 外部类.静态内部类();
```

下面的代码演示静态内部类的定义和使用。

文件位置：`src/main/java/io/github/atengk/oop/StaticInnerClassDemo.java`

```java
package io.github.atengk.oop;

/**
 * 静态内部类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StaticInnerClassDemo {

    private static String systemName = "用户管理系统";

    private String username = "李四";

    public static class SystemConfig {

        private String version = "v1.0.0";

        public void showConfig() {
            System.out.println("系统名称：" + systemName);
            System.out.println("系统版本：" + version);
        }
    }

    public static void main(String[] args) {
        StaticInnerClassDemo.SystemConfig config = new StaticInnerClassDemo.SystemConfig();
        config.showConfig();
    }
}
```

运行结果如下：

```text
系统名称：用户管理系统
系统版本：v1.0.0
```

静态内部类 `SystemConfig` 可以直接访问外部类的静态变量 `systemName`，但不能直接访问外部类的非静态变量 `username`。

如果静态内部类需要访问外部类对象成员，可以通过外部类对象访问。

```java
StaticInnerClassDemo outer = new StaticInnerClassDemo();
System.out.println(outer.username);
```

静态内部类的特点如下：

| 特点                           | 说明                                               |
| ------------------------------ | -------------------------------------------------- |
| 定义位置                       | 定义在外部类中、方法外                             |
| 是否使用 `static`              | 使用                                               |
| 是否依赖外部类对象             | 不依赖                                             |
| 是否能直接访问外部类非静态成员 | 不能                                               |
| 创建方式                       | `new 外部类.静态内部类()`                          |
| 适合场景                       | 内部类只是逻辑上归属于外部类，不需要依赖外部类对象 |

静态内部类常见于配置类、构建器、分组封装等场景。例如某个类只在当前外部类语义下使用，但又不需要访问外部类对象状态，就可以定义成静态内部类。

### 局部内部类

局部内部类是定义在方法、代码块或构造方法内部的类。它的作用范围只在当前方法或代码块中。

局部内部类使用较少，通常用于某个类只在当前方法中临时使用，不希望暴露到方法外部的场景。

下面的代码演示局部内部类。

文件位置：`src/main/java/io/github/atengk/oop/LocalInnerClassDemo.java`

```java
package io.github.atengk.oop;

/**
 * 局部内部类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LocalInnerClassDemo {

    public void printUserInfo(String username) {
        String prefix = "用户信息：";

        class UserPrinter {

            public void print() {
                System.out.println(prefix + username);
            }
        }

        UserPrinter printer = new UserPrinter();
        printer.print();
    }

    public static void main(String[] args) {
        LocalInnerClassDemo demo = new LocalInnerClassDemo();
        demo.printUserInfo("王五");
    }
}
```

运行结果如下：

```text
用户信息：王五
```

局部内部类可以访问所在方法中的局部变量，但这些局部变量必须是事实上的 final，也就是定义后不能再被修改。

下面是错误示例：

```java
public void printUserInfo(String username) {
    username = "赵六";

    class UserPrinter {

        public void print() {
            System.out.println(username);
        }
    }
}
```

如果局部变量被局部内部类访问，那么该变量不能在后续代码中被重新赋值。

局部内部类的特点如下：

| 特点                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 定义位置             | 方法、构造方法或代码块内部           |
| 作用范围             | 只能在当前方法或代码块中使用         |
| 是否能访问外部类成员 | 可以                                 |
| 是否能访问局部变量   | 可以，但局部变量必须是事实上的 final |
| 适合场景             | 临时封装只在当前方法中使用的类       |

实际开发中，局部内部类使用频率不高。很多场景会使用匿名内部类或 Lambda 表达式替代。

### 匿名内部类

匿名内部类是没有名字的内部类，通常用于临时创建接口或抽象类的实现对象。

匿名内部类适合只使用一次的实现逻辑。如果一个实现类只在某个地方使用一次，就没有必要单独创建一个类文件，可以使用匿名内部类简化代码。

基本语法如下：

```java
接口或父类 对象名 = new 接口或父类() {
    方法重写;
};
```

下面先定义一个任务接口。

文件位置：`src/main/java/io/github/atengk/oop/TaskHandler.java`

```java
package io.github.atengk.oop;

/**
 * 任务处理接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TaskHandler {

    void handle(String taskName);
}
```

下面的代码演示匿名内部类的使用。

文件位置：`src/main/java/io/github/atengk/oop/AnonymousInnerClassDemo.java`

```java
package io.github.atengk.oop;

/**
 * 匿名内部类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AnonymousInnerClassDemo {

    public static void main(String[] args) {
        TaskHandler handler = new TaskHandler() {

            @Override
            public void handle(String taskName) {
                System.out.println("正在处理任务：" + taskName);
            }
        };

        handler.handle("数据同步");
    }
}
```

运行结果如下：

```text
正在处理任务：数据同步
```

匿名内部类的本质是创建了一个接口或抽象类的子类对象，只是这个子类没有名字。

如果接口中只有一个抽象方法，也就是函数式接口，那么匿名内部类通常可以使用 Lambda 表达式简化。

```java
TaskHandler handler = taskName -> System.out.println("正在处理任务：" + taskName);
handler.handle("数据同步");
```

匿名内部类的特点如下：

| 特点             | 说明                 |
| ---------------- | -------------------- |
| 是否有类名       | 没有                 |
| 是否可以重复使用 | 不适合重复使用       |
| 常见用途         | 临时实现接口或抽象类 |
| 优点             | 代码更紧凑           |
| 缺点             | 逻辑复杂时可读性较差 |

匿名内部类适合简单的临时实现。如果实现逻辑较复杂，或者需要多处复用，应该单独定义实现类。

## 枚举

枚举是 Java 中用于表示固定数量常量对象的一种特殊类型。

当某个变量的取值范围是固定的，例如订单状态、用户性别、支付状态、消息类型等，就可以使用枚举。相比直接使用字符串或数字，枚举更安全、更清晰，也更便于统一管理。

### 枚举的定义

在 Java 中，使用 `enum` 关键字定义枚举。

基本语法如下：

```java
public enum 枚举名 {
    枚举常量1,
    枚举常量2,
    枚举常量3
}
```

下面的代码定义订单状态枚举。

文件位置：`src/main/java/io/github/atengk/oop/OrderStatus.java`

```java
package io.github.atengk.oop;

/**
 * 订单状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
public enum OrderStatus {

    CREATED,

    PAID,

    SHIPPED,

    FINISHED,

    CANCELED
}
```

下面的代码演示枚举的基本使用。

文件位置：`src/main/java/io/github/atengk/oop/EnumBasicDemo.java`

```java
package io.github.atengk.oop;

/**
 * 枚举基础使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EnumBasicDemo {

    public static void main(String[] args) {
        OrderStatus status = OrderStatus.PAID;

        if (status == OrderStatus.PAID) {
            System.out.println("订单已支付，可以安排发货");
        }

        System.out.println("枚举名称：" + status.name());
        System.out.println("枚举序号：" + status.ordinal());
    }
}
```

运行结果如下：

```text
订单已支付，可以安排发货
枚举名称：PAID
枚举序号：1
```

枚举常见方法如下：

| 方法                   | 说明                          |
| ---------------------- | ----------------------------- |
| `name()`               | 获取枚举常量名称              |
| `ordinal()`            | 获取枚举常量序号，从 `0` 开始 |
| `values()`             | 获取当前枚举的所有常量        |
| `valueOf(String name)` | 根据名称获取枚举常量          |

下面的代码演示遍历枚举。

文件位置：`src/main/java/io/github/atengk/oop/EnumValuesDemo.java`

```java
package io.github.atengk.oop;

/**
 * 枚举遍历示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EnumValuesDemo {

    public static void main(String[] args) {
        for (OrderStatus status : OrderStatus.values()) {
            System.out.println(status.name() + " -> " + status.ordinal());
        }
    }
}
```

需要注意，实际开发中通常不建议依赖 `ordinal()` 处理业务逻辑。因为枚举常量顺序一旦调整，`ordinal()` 的值就会变化，容易造成业务错误。

### 枚举字段与方法

枚举不仅可以定义常量，还可以定义字段、构造方法和普通方法。

实际开发中，枚举通常不会只定义英文常量名，而是会给每个枚举常量绑定业务编码和中文说明。

下面的代码演示带字段和方法的枚举。

文件位置：`src/main/java/io/github/atengk/oop/PayStatus.java`

```java
package io.github.atengk.oop;

/**
 * 支付状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
public enum PayStatus {

    WAIT_PAY(0, "待支付"),

    PAY_SUCCESS(1, "支付成功"),

    PAY_FAILED(2, "支付失败"),

    REFUNDED(3, "已退款");

    private final Integer code;

    private final String description;

    PayStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PayStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (PayStatus status : PayStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }

        return null;
    }
}
```

枚举的构造方法默认是 `private`，即使不写 `private`，外部也不能直接调用枚举构造方法创建对象。

错误示例：

```java
PayStatus status = new PayStatus(1, "支付成功");
```

枚举常量必须写在枚举类的最前面。如果枚举中还有字段、构造方法或普通方法，枚举常量列表最后需要使用分号 `;` 结束。

下面的代码演示根据状态编码获取枚举，并读取枚举说明。

文件位置：`src/main/java/io/github/atengk/oop/EnumFieldMethodDemo.java`

```java
package io.github.atengk.oop;

/**
 * 枚举字段和方法调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EnumFieldMethodDemo {

    public static void main(String[] args) {
        PayStatus status = PayStatus.getByCode(1);

        if (status == null) {
            System.out.println("未知支付状态");
            return;
        }

        System.out.println("状态编码：" + status.getCode());
        System.out.println("状态说明：" + status.getDescription());
    }
}
```

运行结果如下：

```text
状态编码：1
状态说明：支付成功
```

带字段的枚举适合替代业务中的魔法值。例如不要在代码中到处写 `0`、`1`、`2` 表示状态，而是统一使用 `PayStatus.WAIT_PAY`、`PayStatus.PAY_SUCCESS` 等枚举值。

下面是不推荐的写法：

```java
if (payStatus == 1) {
    System.out.println("支付成功");
}
```

推荐写法如下：

```java
if (payStatus == PayStatus.PAY_SUCCESS.getCode()) {
    System.out.println("支付成功");
}
```

如果比较的是枚举对象本身，可以直接使用 `==`。

```java
PayStatus status = PayStatus.PAY_SUCCESS;

if (status == PayStatus.PAY_SUCCESS) {
    System.out.println("支付成功");
}
```

枚举对象是固定的单例常量，因此枚举对象之间可以安全使用 `==` 比较。

### 枚举的常见使用场景

枚举适合用于值固定、语义明确、不希望被随意扩展的场景。

常见使用场景如下：

| 场景     | 示例                                   |
| -------- | -------------------------------------- |
| 订单状态 | 待支付、已支付、已发货、已完成、已取消 |
| 支付状态 | 待支付、支付成功、支付失败、已退款     |
| 用户状态 | 正常、禁用、锁定、注销                 |
| 性别类型 | 男、女、未知                           |
| 消息类型 | 短信、邮件、站内信、微信消息           |
| 审核状态 | 待审核、审核通过、审核拒绝             |
| 日志级别 | DEBUG、INFO、WARN、ERROR               |

下面的代码演示使用枚举处理订单状态流转。

文件位置：`src/main/java/io/github/atengk/oop/OrderStatusService.java`

```java
package io.github.atengk.oop;

/**
 * 订单状态服务示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class OrderStatusService {

    public void handleStatus(OrderStatus status) {
        if (status == null) {
            System.out.println("订单状态不能为空");
            return;
        }

        switch (status) {
            case CREATED:
                System.out.println("订单已创建，等待用户支付");
                break;
            case PAID:
                System.out.println("订单已支付，准备发货");
                break;
            case SHIPPED:
                System.out.println("订单已发货，等待用户收货");
                break;
            case FINISHED:
                System.out.println("订单已完成");
                break;
            case CANCELED:
                System.out.println("订单已取消");
                break;
            default:
                System.out.println("未知订单状态");
                break;
        }
    }
}
```

下面的代码演示调用订单状态服务。

文件位置：`src/main/java/io/github/atengk/oop/OrderStatusDemo.java`

```java
package io.github.atengk.oop;

/**
 * 订单状态枚举使用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class OrderStatusDemo {

    public static void main(String[] args) {
        OrderStatusService service = new OrderStatusService();
        service.handleStatus(OrderStatus.PAID);
    }
}
```

枚举的优势如下：

| 优势       | 说明                           |
| ---------- | ------------------------------ |
| 类型安全   | 只能使用枚举中定义好的值       |
| 语义清晰   | 比数字和字符串更容易理解       |
| 便于维护   | 状态值集中定义，统一管理       |
| 可扩展字段 | 可以绑定编码、描述、排序等信息 |
| 支持方法   | 可以在枚举中封装查找和判断逻辑 |

使用枚举可以减少魔法值，让业务代码更清晰。

## 常见问题

这一部分整理 Java 面向对象基础中最常见、最容易混淆的问题，包括重载与重写、`this` 与 `super`、抽象类与接口、`==` 与 `equals()`。

这些内容在面试和实际开发中都很常见，需要重点理解概念区别和使用场景。

### 重载与重写的区别

重载和重写都是 Java 中和方法相关的概念，但它们解决的问题不同。

重载是同一个类中方法名相同、参数列表不同。重写是子类重新实现父类中已有的方法。

| 对比项   | 重载                   | 重写                     |
| -------- | ---------------------- | ------------------------ |
| 英文     | Overload               | Override                 |
| 发生位置 | 同一个类中             | 父子类之间               |
| 方法名   | 必须相同               | 必须相同                 |
| 参数列表 | 必须不同               | 必须相同                 |
| 返回值   | 不能只靠返回值区分重载 | 返回值类型必须相同或兼容 |
| 访问权限 | 没有特殊限制           | 子类方法权限不能更小     |
| 典型作用 | 提供多种参数调用方式   | 子类改写父类行为         |
| 注解     | 不使用 `@Override`     | 建议使用 `@Override`     |

下面的代码演示方法重载。

文件位置：`src/main/java/io/github/atengk/oop/MethodOverloadDemo.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 方法重载示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MethodOverloadDemo {

    public BigDecimal calculateAmount(BigDecimal price, Integer quantity) {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal calculateAmount(BigDecimal price, Integer quantity, BigDecimal discountAmount) {
        BigDecimal amount = calculateAmount(price, quantity);

        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return amount;
        }

        return amount.subtract(discountAmount);
    }

    public static void main(String[] args) {
        MethodOverloadDemo demo = new MethodOverloadDemo();

        System.out.println(demo.calculateAmount(new BigDecimal("99.00"), 2));
        System.out.println(demo.calculateAmount(new BigDecimal("99.00"), 2, new BigDecimal("20.00")));
    }
}
```

在这个示例中，两个 `calculateAmount()` 方法都在同一个类中，方法名相同，但参数列表不同，因此属于方法重载。

下面的代码演示方法重写。

文件位置：`src/main/java/io/github/atengk/oop/MethodOverrideParent.java`

```java
package io.github.atengk.oop;

/**
 * 方法重写父类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MethodOverrideParent {

    public void printType() {
        System.out.println("父类类型");
    }
}
```

文件位置：`src/main/java/io/github/atengk/oop/MethodOverrideChild.java`

```java
package io.github.atengk.oop;

/**
 * 方法重写子类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MethodOverrideChild extends MethodOverrideParent {

    @Override
    public void printType() {
        System.out.println("子类类型");
    }
}
```

文件位置：`src/main/java/io/github/atengk/oop/MethodOverrideDemo.java`

```java
package io.github.atengk.oop;

/**
 * 方法重写调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MethodOverrideDemo {

    public static void main(String[] args) {
        MethodOverrideParent parent = new MethodOverrideParent();
        MethodOverrideParent child = new MethodOverrideChild();

        parent.printType();
        child.printType();
    }
}
```

运行结果如下：

```text
父类类型
子类类型
```

重载关注的是“同一个方法名支持不同参数”；重写关注的是“子类改变父类方法的实现”。

### this 与 super 的区别

`this` 和 `super` 都可以在类中访问成员变量、成员方法和构造方法，但它们表示的对象范围不同。

`this` 表示当前对象本身，`super` 表示当前对象中的父类部分。

| 对比项                 | `this`                                         | `super`                                       |
| ---------------------- | ---------------------------------------------- | --------------------------------------------- |
| 含义                   | 当前对象本身                                   | 当前对象中的父类部分                          |
| 访问成员变量           | 当前类成员变量                                 | 父类成员变量                                  |
| 访问成员方法           | 当前类成员方法                                 | 父类成员方法                                  |
| 调用构造方法           | 调用本类其他构造方法                           | 调用父类构造方法                              |
| 构造方法中使用         | `this(...)` 必须在第一行                       | `super(...)` 必须在第一行                     |
| 是否可同时调用构造方法 | 不能和 `super(...)` 同时出现在同一个构造方法中 | 不能和 `this(...)` 同时出现在同一个构造方法中 |

下面的代码演示 `this` 和 `super` 的区别。

文件位置：`src/main/java/io/github/atengk/oop/ThisSuperParent.java`

```java
package io.github.atengk.oop;

/**
 * this和super父类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThisSuperParent {

    protected String name = "父类名称";

    public ThisSuperParent() {
        System.out.println("父类无参构造方法");
    }

    public void printName() {
        System.out.println("父类方法：" + name);
    }
}
```

文件位置：`src/main/java/io/github/atengk/oop/ThisSuperChild.java`

```java
package io.github.atengk.oop;

/**
 * this和super子类示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThisSuperChild extends ThisSuperParent {

    private String name = "子类名称";

    public ThisSuperChild() {
        super();
        System.out.println("子类无参构造方法");
    }

    public void printInfo() {
        System.out.println("this.name = " + this.name);
        System.out.println("super.name = " + super.name);

        this.printName();
        super.printName();
    }

    @Override
    public void printName() {
        System.out.println("子类方法：" + name);
    }
}
```

文件位置：`src/main/java/io/github/atengk/oop/ThisSuperDemo.java`

```java
package io.github.atengk.oop;

/**
 * this和super调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThisSuperDemo {

    public static void main(String[] args) {
        ThisSuperChild child = new ThisSuperChild();
        child.printInfo();
    }
}
```

运行结果如下：

```text
父类无参构造方法
子类无参构造方法
this.name = 子类名称
super.name = 父类名称
子类方法：子类名称
父类方法：父类名称
```

`this.printName()` 调用的是当前类中重写后的方法，`super.printName()` 调用的是父类中的原始方法。

需要注意，`this(...)` 和 `super(...)` 都必须放在构造方法第一行，所以不能同时写在同一个构造方法中。

错误示例：

```java
public ThisSuperChild() {
    this("张三");
    super();
}
```

这段代码会编译失败，因为构造方法第一行只能调用一个构造方法。

### 抽象类与接口的选择

抽象类和接口都可以用于抽象设计，但它们的设计目的不同。

抽象类适合表达“是什么”的父子关系，接口适合表达“能做什么”的能力关系。

例如，`Dog` 是一种 `Animal`，这种关系适合使用继承或抽象类。`Dog` 能跑、能叫、能被训练，这些能力更适合用接口描述。

选择时可以参考下面的规则：

| 场景                       | 推荐选择 |
| -------------------------- | -------- |
| 多个类存在共同父类关系     | 抽象类   |
| 多个类有共同属性           | 抽象类   |
| 多个类有公共流程或公共实现 | 抽象类   |
| 多个类只是具备同一种能力   | 接口     |
| 一个类需要具备多种能力     | 接口     |
| 需要定义扩展规范           | 接口     |
| 需要配合多态解耦业务实现   | 接口优先 |

下面用支付场景说明。

如果要定义所有支付渠道都必须具备“支付能力”，更适合使用接口。

文件位置：`src/main/java/io/github/atengk/oop/Payable.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 支付能力接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface Payable {

    void pay(BigDecimal amount);
}
```

不同支付渠道可以实现这个接口。

文件位置：`src/main/java/io/github/atengk/oop/CardPayable.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 银行卡支付实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CardPayable implements Payable {

    @Override
    public void pay(BigDecimal amount) {
        System.out.println("使用银行卡支付：" + amount + "元");
    }
}
```

如果多个支付渠道都有公共字段和公共校验逻辑，则可以使用抽象类提供公共实现。

文件位置：`src/main/java/io/github/atengk/oop/AbstractPayHandler.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 支付处理抽象类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public abstract class AbstractPayHandler implements Payable {

    private String channelName;

    public AbstractPayHandler(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public void pay(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("支付金额必须大于0");
            return;
        }

        System.out.println("支付渠道：" + channelName);
        doPay(amount);
    }

    protected abstract void doPay(BigDecimal amount);
}
```

文件位置：`src/main/java/io/github/atengk/oop/UnionPayHandler.java`

```java
package io.github.atengk.oop;

import java.math.BigDecimal;

/**
 * 银联支付处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class UnionPayHandler extends AbstractPayHandler {

    public UnionPayHandler() {
        super("银联支付");
    }

    @Override
    protected void doPay(BigDecimal amount) {
        System.out.println("调用银联接口完成支付：" + amount + "元");
    }
}
```

在这个设计中，`Payable` 负责定义支付能力，`AbstractPayHandler` 负责抽取公共支付流程，`UnionPayHandler` 负责具体支付实现。

这种设计在实际开发中很常见：接口定义规范，抽象类复用公共逻辑，具体类实现业务细节。

简单总结如下：

| 类型   | 更关注                       | 典型用途                       |
| ------ | ---------------------------- | ------------------------------ |
| 抽象类 | 同类对象的公共结构和公共逻辑 | 模板方法、公共字段、公共校验   |
| 接口   | 不同对象具备的共同能力       | 规范定义、多实现扩展、业务解耦 |

### == 与 equals 的区别

`==` 和 `equals()` 都可以用于比较，但它们比较的内容不同。

对于基本数据类型，`==` 比较的是值。

对于引用数据类型，`==` 比较的是对象地址，也就是两个引用是否指向同一个对象。

`equals()` 是 `Object` 类中的方法，默认也是比较对象地址。但很多类会重写 `equals()` 方法，使它比较对象内容，例如 `String`、`Integer`、`BigDecimal` 等。

#### 基本数据类型比较

下面的代码演示基本数据类型使用 `==`。

文件位置：`src/main/java/io/github/atengk/oop/PrimitiveCompareDemo.java`

```java
package io.github.atengk.oop;

/**
 * 基本数据类型比较示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class PrimitiveCompareDemo {

    public static void main(String[] args) {
        int number1 = 100;
        int number2 = 100;

        System.out.println(number1 == number2);
    }
}
```

运行结果如下：

```text
true
```

对于基本数据类型，`==` 比较的是变量保存的具体值。

#### 引用数据类型比较

下面的代码演示引用数据类型使用 `==` 和 `equals()` 的区别。

文件位置：`src/main/java/io/github/atengk/oop/StringCompareDemo.java`

```java
package io.github.atengk.oop;

/**
 * 字符串比较示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StringCompareDemo {

    public static void main(String[] args) {
        String str1 = new String("Java");
        String str2 = new String("Java");

        System.out.println(str1 == str2);
        System.out.println(str1.equals(str2));
    }
}
```

运行结果如下：

```text
false
true
```

`str1 == str2` 的结果是 `false`，因为它们是两个不同的对象，内存地址不同。

`str1.equals(str2)` 的结果是 `true`，因为 `String` 类重写了 `equals()` 方法，比较的是字符串内容。

#### 自定义对象比较

如果自定义类没有重写 `equals()`，默认比较对象地址。

下面的代码定义一个用户类，没有重写 `equals()`。

文件位置：`src/main/java/io/github/atengk/oop/CompareUser.java`

```java
package io.github.atengk.oop;

/**
 * 用户比较示例类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CompareUser {

    private Long id;

    private String username;

    public CompareUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }
}
```

下面的代码演示默认比较结果。

文件位置：`src/main/java/io/github/atengk/oop/ObjectCompareDemo.java`

```java
package io.github.atengk.oop;

/**
 * 对象比较示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ObjectCompareDemo {

    public static void main(String[] args) {
        CompareUser user1 = new CompareUser(1L, "admin");
        CompareUser user2 = new CompareUser(1L, "admin");

        System.out.println(user1 == user2);
        System.out.println(user1.equals(user2));
    }
}
```

运行结果如下：

```text
false
false
```

虽然两个对象的属性值相同，但因为没有重写 `equals()`，所以 `equals()` 默认仍然比较对象地址。

如果希望根据对象内容比较，需要重写 `equals()` 和 `hashCode()` 方法。

下面的代码演示重写后的写法。

文件位置：`src/main/java/io/github/atengk/oop/EqualsUser.java`

```java
package io.github.atengk.oop;

import java.util.Objects;

/**
 * 重写equals和hashCode的用户类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EqualsUser {

    private Long id;

    private String username;

    public EqualsUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof EqualsUser equalsUser)) {
            return false;
        }

        return Objects.equals(id, equalsUser.id)
                && Objects.equals(username, equalsUser.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
```

下面的代码演示重写后的比较结果。

文件位置：`src/main/java/io/github/atengk/oop/EqualsCompareDemo.java`

```java
package io.github.atengk.oop;

/**
 * equals比较调用示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EqualsCompareDemo {

    public static void main(String[] args) {
        EqualsUser user1 = new EqualsUser(1L, "admin");
        EqualsUser user2 = new EqualsUser(1L, "admin");

        System.out.println(user1 == user2);
        System.out.println(user1.equals(user2));
    }
}
```

运行结果如下：

```text
false
true
```

`==` 仍然是 `false`，因为两个对象地址不同；`equals()` 是 `true`，因为已经按照 `id` 和 `username` 比较对象内容。

#### 使用建议

实际开发中可以按照下面的规则选择：

| 场景               | 推荐方式               |
| ------------------ | ---------------------- |
| 基本数据类型比较   | 使用 `==`              |
| 枚举比较           | 使用 `==`              |
| 字符串内容比较     | 使用 `equals()`        |
| 包装类型内容比较   | 使用 `equals()`        |
| 自定义对象内容比较 | 重写 `equals()` 后使用 |
| 判断是否同一个对象 | 使用 `==`              |

字符串比较时，推荐把确定不为 `null` 的字符串写在前面，避免空指针异常。

```java
String status = null;

if ("SUCCESS".equals(status)) {
    System.out.println("处理成功");
}
```

不推荐写法如下：

```java
String status = null;

if (status.equals("SUCCESS")) {
    System.out.println("处理成功");
}
```

第二种写法中，如果 `status` 为 `null`，会出现 `NullPointerException`。

`==` 和 `equals()` 的核心区别可以总结为一句话：`==` 比较变量保存的值，对于引用类型来说这个值是对象地址；`equals()` 默认比较地址，但可以被重写为比较对象内容。
