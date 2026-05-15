# Java 类型基础

Java 是一种强类型语言，变量在声明时必须明确数据类型。编译器会根据类型检查变量赋值、数值运算、方法调用和对象访问是否合法。

Java 的类型体系主要分为两类：基本类型和引用类型。基本类型用于保存简单数据值，引用类型用于表示对象、数组、字符串、集合等复杂数据结构。

## 类型体系概述

类型体系是理解 Java 变量、对象、方法参数、类型转换、包装类、集合和泛型的基础。Java 中的变量并不是随意保存数据，而是必须受类型约束。

从整体上看，Java 类型可以分为：

| 类型分类 | 说明             | 示例                               |
| -------- | ---------------- | ---------------------------------- |
| 基本类型 | 保存具体的数据值 | `int`、`long`、`double`、`boolean` |
| 引用类型 | 保存对象的引用   | `String`、`Object`、`List`、数组   |

基本类型更偏向保存“值本身”，引用类型更偏向保存“对象引用”。这个区别会影响默认值、空值判断、比较方式、方法调用以及参数传递。

### 基本类型

基本类型是 Java 语言内置的基础数据类型，一共有 8 种。它们不是对象，不能直接调用方法，主要用于保存简单数据。

| 类型分类 | 类型                           | 说明                       |
| -------- | ------------------------------ | -------------------------- |
| 整数类型 | `byte`、`short`、`int`、`long` | 用于表示整数               |
| 浮点类型 | `float`、`double`              | 用于表示小数               |
| 字符类型 | `char`                         | 用于表示单个字符           |
| 布尔类型 | `boolean`                      | 用于表示 `true` 或 `false` |

基本类型变量保存的是具体的值。

下面代码演示了常见基本类型的声明方式：

```java
int age = 18;
long userId = 10001L;
double price = 99.99;
char level = 'A';
boolean enabled = true;
```

在实际开发中，整数通常优先使用 `int` 或 `long`。如果数据可能超过 `int` 的范围，例如数据库主键、订单号、用户 ID，通常使用 `long`。小数默认使用 `double`，但涉及金额计算时不建议使用 `double` 或 `float`，应使用 `BigDecimal`。

基本类型不能赋值为 `null`。

```java
int count = 0;

// 编译错误：基本类型不能为 null
// int total = null;
```

如果需要表示“没有值”或“未知值”，可以使用对应的包装类型。

```java
Integer count = null;
Long userId = null;
Boolean enabled = null;
```

### 引用类型

引用类型用于表示对象。引用类型变量保存的不是对象本身，而是对象的引用。程序通过这个引用访问对象的属性和方法。

Java 中常见的引用类型包括：

| 类型     | 示例                             | 说明                       |
| -------- | -------------------------------- | -------------------------- |
| 类       | `String`、`Object`、`BigDecimal` | 最常见的引用类型           |
| 数组     | `int[]`、`String[]`              | 数组本身也是对象           |
| 接口     | `List`、`Map`、`Runnable`        | 变量可以指向接口实现类对象 |
| 枚举     | `Enum`                           | 用于表示固定范围的常量对象 |
| 包装类型 | `Integer`、`Long`、`Boolean`     | 基本类型对应的对象类型     |

引用类型变量可以赋值为 `null`，表示当前没有引用任何对象。

```java
String name = "Ateng";
BigDecimal amount = new BigDecimal("99.99");
List<String> names = new ArrayList<>();

String remark = null;
```

引用类型可以调用对象的方法。

```java
String name = "Ateng";

int length = name.length();
boolean startsWithA = name.startsWith("A");
```

数组虽然语法上比较特殊，但数组本身也是引用类型。

```java
int[] numbers = {1, 2, 3};

int first = numbers[0];
int size = numbers.length;
```

接口类型变量可以指向具体实现类对象，这是 Java 多态的基础。

```java
List<String> names = new ArrayList<>();

names.add("Java");
names.add("Spring Boot");
```

在这段代码中，变量类型是 `List<String>`，实际对象类型是 `ArrayList`。这种写法属于面向接口编程，可以降低代码对具体实现类的依赖。

### 基本类型与引用类型的区别

基本类型和引用类型的核心区别在于：基本类型变量保存的是具体值，引用类型变量保存的是对象引用。

| 对比项            | 基本类型                        | 引用类型                                 |
| ----------------- | ------------------------------- | ---------------------------------------- |
| 保存内容          | 具体数据值                      | 对象引用                                 |
| 是否为对象        | 不是对象                        | 是对象或指向对象                         |
| 是否可以为 `null` | 不可以                          | 可以                                     |
| 是否可以调用方法  | 不可以                          | 可以                                     |
| 默认值            | 有固定默认值，例如 `0`、`false` | 默认值为 `null`                          |
| 比较方式          | `==` 比较值                     | `==` 比较引用地址，`equals` 通常比较内容 |
| 常见场景          | 数值计算、状态标识、简单变量    | 对象建模、集合、字符串、业务数据         |

基本类型使用 `==` 比较的是值。

```java
int a = 100;
int b = 100;

boolean result = a == b; // true
```

引用类型使用 `==` 比较的是两个变量是否指向同一个对象。

```java
String a = new String("Java");
String b = new String("Java");

boolean sameReference = a == b;     // false
boolean sameContent = a.equals(b); // true
```

对于引用类型，业务开发中通常不要使用 `==` 比较内容，而应优先使用 `equals` 方法。尤其是 `String`、`Integer`、`Long`、`BigDecimal` 等类型，错误使用 `==` 容易产生隐藏问题。

引用类型可能为 `null`，直接调用方法会出现空指针异常。

```java
String name = null;

// 运行时报错：NullPointerException
// int length = name.length();
```

更安全的写法是先判断是否为空。

```java
String name = null;

if (name != null) {
    int length = name.length();
}
```

如果项目中已经引入 Hutool，可以使用 `StrUtil` 或 `ObjectUtil` 简化空值判断。

```java
import cn.hutool.core.util.StrUtil;

String name = null;

if (StrUtil.isNotBlank(name)) {
    int length = name.length();
}
```

Java 的方法参数传递始终是值传递。对于基本类型，传递的是值的副本；对于引用类型，传递的是引用地址的副本。

```java
int num = 10;
changePrimitive(num);
// num 仍然是 10

User user = new User();
user.setName("Ateng");

changeReference(user);
// user 对象内部的 name 可能已经被修改
```

需要注意的是，不能简单理解为“基本类型一定在栈中，引用类型一定在堆中”。更准确的理解是：基本类型变量表示具体值，引用类型变量表示对象引用；具体内存布局会受到变量位置、对象结构、JVM 实现和运行时优化影响。开发时应重点掌握类型语义，而不是死记内存位置。



## 基本数据类型

基本数据类型是 Java 内置的最基础类型，用于保存简单的数据值。Java 一共有 8 种基本数据类型，分为整数类型、浮点类型、字符类型和布尔类型。

基本数据类型不是对象，不能调用方法，也不能赋值为 `null`。它们适合用于局部计算、循环控制、状态判断和简单数值存储。

### 整数类型

整数类型用于表示没有小数部分的数值。Java 提供了 4 种整数类型：`byte`、`short`、`int`、`long`。

| 类型    | 占用空间 | 默认值 | 说明                     |
| ------- | -------- | ------ | ------------------------ |
| `byte`  | 1 字节   | `0`    | 范围较小，常用于字节数据 |
| `short` | 2 字节   | `0`    | 使用较少                 |
| `int`   | 4 字节   | `0`    | 最常用的整数类型         |
| `long`  | 8 字节   | `0L`   | 适合保存较大的整数       |

日常开发中，普通整数优先使用 `int`。如果是数据库主键、用户 ID、订单号、时间戳等可能较大的整数，通常使用 `long`。

整数类型的基本使用方式如下：

```java
int age = 18;
int count = 100;

long userId = 10001L;
long orderId = 202605150001L;
```

`long` 类型的字面量建议在数字后面加 `L`，不要使用小写 `l`，因为小写 `l` 容易和数字 `1` 混淆。

```java
long value1 = 100L;

// 不推荐：小写 l 可读性差
long value2 = 100l;
```

整数运算时需要注意溢出问题。超出类型取值范围后，结果不会自动报错，而是可能得到错误的数值。

```java
int max = Integer.MAX_VALUE;

int result = max + 1;

System.out.println(result); // -2147483648
```

如果参与运算的数据可能超过 `int` 范围，应提前使用 `long`。

```java
long max = Integer.MAX_VALUE;

long result = max + 1;

System.out.println(result); // 2147483648
```

### 浮点类型

浮点类型用于表示带有小数部分的数值。Java 提供了 2 种浮点类型：`float` 和 `double`。

| 类型     | 占用空间 | 默认值 | 说明                       |
| -------- | -------- | ------ | -------------------------- |
| `float`  | 4 字节   | `0.0f` | 单精度浮点数               |
| `double` | 8 字节   | `0.0d` | 双精度浮点数，默认小数类型 |

Java 中的小数字面量默认是 `double` 类型。如果要声明 `float`，需要在数字后面加 `F` 或 `f`。

```java
double price = 99.99;

float score = 88.5F;
```

浮点数采用二进制方式存储，很多十进制小数无法被精确表示，因此浮点数运算可能出现精度误差。

```java
double result = 0.1 + 0.2;

System.out.println(result); // 0.30000000000000004
```

因此，`float` 和 `double` 不适合用于金额、余额、税率结算等要求精确计算的业务场景。涉及金额时，通常使用 `BigDecimal`。

```java
BigDecimal price = new BigDecimal("0.1");
BigDecimal amount = new BigDecimal("0.2");

BigDecimal result = price.add(amount);

System.out.println(result); // 0.3
```

创建 `BigDecimal` 时推荐使用字符串构造方式，不建议直接使用 `double`。

```java
// 推荐
BigDecimal value1 = new BigDecimal("99.99");

// 不推荐：double 本身已经可能存在精度误差
BigDecimal value2 = new BigDecimal(99.99);
```

### 字符类型

字符类型使用 `char` 表示，用于保存单个字符。`char` 使用单引号声明。

```java
char level = 'A';
char gender = '男';
char symbol = '#';
```

`char` 只能保存一个字符，不能保存多个字符。

```java
char value = 'A';

// 编译错误：char 只能保存单个字符
// char error = 'AB';
```

如果需要保存多个字符，应使用 `String`。

```java
String name = "Ateng";
String message = "Java基础";
```

`char` 本质上可以参与数值运算，因为字符底层对应 Unicode 编码值。

```java
char ch = 'A';

int code = ch;

System.out.println(code); // 65
```

也可以通过数值转换得到对应字符。

```java
int code = 65;

char ch = (char) code;

System.out.println(ch); // A
```

实际开发中，`char` 使用频率低于 `String`。除非明确只需要保存一个字符，否则通常使用 `String` 更方便。

### 布尔类型

布尔类型使用 `boolean` 表示，只有两个取值：`true` 和 `false`。它通常用于条件判断、开关状态、校验结果和逻辑控制。

```java
boolean enabled = true;
boolean deleted = false;
boolean success = true;
```

布尔类型常用于 `if` 判断：

```java
boolean enabled = true;

if (enabled) {
    System.out.println("功能已启用");
}
```

也可以用于方法返回值，表示业务判断结果。

```java
public boolean isAdult(int age) {
    return age >= 18;
}
```

`boolean` 不能和整数互相转换。Java 不支持使用 `0` 表示 `false`，也不支持使用 `1` 表示 `true`。

```java
boolean enabled = true;

// 编译错误：Java 中 boolean 不能使用 1 或 0 表示
// boolean flag = 1;
```

这种设计可以减少隐式转换导致的判断错误，使代码语义更清晰。

### 默认值与取值范围

基本数据类型作为成员变量时，会有默认值；作为局部变量时，必须先赋值再使用。

基本类型的默认值如下：

| 类型      | 默认值     |
| --------- | ---------- |
| `byte`    | `0`        |
| `short`   | `0`        |
| `int`     | `0`        |
| `long`    | `0L`       |
| `float`   | `0.0f`     |
| `double`  | `0.0d`     |
| `char`    | `'\u0000'` |
| `boolean` | `false`    |

成员变量会自动初始化：

```java
public class User {
    private int age;
    private boolean enabled;
}
```

在这段代码中，`age` 的默认值是 `0`，`enabled` 的默认值是 `false`。

局部变量不会自动初始化，必须先赋值：

```java
int count;

// 编译错误：局部变量使用前必须赋值
// System.out.println(count);

count = 10;
System.out.println(count);
```

常见基本类型的取值范围如下：

| 类型      | 取值范围                                        |
| --------- | ----------------------------------------------- |
| `byte`    | `-128` 到 `127`                                 |
| `short`   | `-32768` 到 `32767`                             |
| `int`     | `-2147483648` 到 `2147483647`                   |
| `long`    | `-9223372036854775808` 到 `9223372036854775807` |
| `float`   | 约 `1.4E-45` 到 `3.4028235E38`                  |
| `double`  | 约 `4.9E-324` 到 `1.7976931348623157E308`       |
| `char`    | `0` 到 `65535`                                  |
| `boolean` | `true` 或 `false`                               |

如果需要在代码中查看类型范围，可以使用包装类提供的常量。

```java
System.out.println(Integer.MIN_VALUE);
System.out.println(Integer.MAX_VALUE);

System.out.println(Long.MIN_VALUE);
System.out.println(Long.MAX_VALUE);

System.out.println.Double.MIN_VALUE);
System.out.println.Double.MAX_VALUE);
```

上面最后两行写法有误，正确写法应使用 `Double` 类型名：

```java
System.out.println(Double.MIN_VALUE);
System.out.println(Double.MAX_VALUE);
```

## 包装类型

包装类型是基本类型对应的引用类型。Java 为每一种基本类型都提供了一个包装类，使基本类型可以像对象一样使用。

包装类型主要用于集合、泛型、对象属性、空值表达和工具方法调用等场景。

### 包装类概述

Java 的 8 种基本类型都有对应的包装类。

| 基本类型  | 包装类型    |
| --------- | ----------- |
| `byte`    | `Byte`      |
| `short`   | `Short`     |
| `int`     | `Integer`   |
| `long`    | `Long`      |
| `float`   | `Float`     |
| `double`  | `Double`    |
| `char`    | `Character` |
| `boolean` | `Boolean`   |

包装类型是引用类型，可以赋值为 `null`，也可以调用方法。

```java
Integer age = 18;
Long userId = 10001L;
Boolean enabled = true;

String ageText = age.toString();
```

包装类型常用于集合，因为 Java 泛型不支持基本类型。

```java
List<Integer> numbers = new ArrayList<>();

numbers.add(1);
numbers.add(2);
numbers.add(3);
```

下面这种写法是错误的：

```java
// 编译错误：泛型不能使用基本类型
// List<int> numbers = new ArrayList<>();
```

在实体类、DTO、VO 等业务对象中，也经常使用包装类型。原因是包装类型可以区分“没有传值”和“传了默认值”。

```java
private Integer age;
private Boolean enabled;
```

例如，`Integer age = null` 表示年龄未知；`Integer age = 0` 表示年龄明确为 0。二者语义不同。

### 自动装箱与自动拆箱

自动装箱是指 Java 自动将基本类型转换为对应的包装类型。自动拆箱是指 Java 自动将包装类型转换为对应的基本类型。

自动装箱示例：

```java
Integer age = 18;
Long userId = 10001L;
Boolean enabled = true;
```

上面的代码等价于：

```java
Integer age = Integer.valueOf(18);
Long userId = Long.valueOf(10001L);
Boolean enabled = Boolean.valueOf(true);
```

自动拆箱示例：

```java
Integer age = 18;

int value = age;
```

上面的代码等价于：

```java
Integer age = 18;

int value = age.intValue();
```

自动装箱和自动拆箱让基本类型与包装类型之间的使用更方便，但也容易隐藏空指针问题。

```java
Integer count = 10;

int total = count + 5;

System.out.println(total); // 15
```

这段代码中，`count + 5` 会触发自动拆箱，先将 `Integer` 转换为 `int`，再进行数值运算。

集合中使用包装类型时，也会发生自动装箱和自动拆箱。

```java
List<Integer> numbers = new ArrayList<>();

numbers.add(1);       // 自动装箱
int first = numbers.get(0); // 自动拆箱
```

频繁装箱和拆箱会产生额外开销。在大量数值计算场景中，优先使用基本类型。

### 包装类型的空指针问题

包装类型是引用类型，可以为 `null`。当包装类型为 `null` 时，如果发生自动拆箱，就会抛出 `NullPointerException`。

```java
Integer count = null;

// 运行时报错：NullPointerException
// int total = count;
```

原因是自动拆箱时，底层会调用类似下面的方法：

```java
int total = count.intValue();
```

当 `count` 为 `null` 时，调用 `intValue()` 就会出现空指针异常。

在条件判断中也需要注意自动拆箱问题。

```java
Boolean enabled = null;

// 运行时报错：NullPointerException
// if (enabled) {
//     System.out.println("已启用");
// }
```

更安全的写法是使用 `Boolean.TRUE.equals`。

```java
Boolean enabled = null;

if (Boolean.TRUE.equals(enabled)) {
    System.out.println("已启用");
}
```

对于数值包装类型，可以先设置默认值。

```java
Integer count = null;

int total = count == null ? 0 : count;

System.out.println(total);
```

如果项目中已经引入 Hutool，可以使用 `ObjectUtil.defaultIfNull` 简化默认值处理。

```java
import cn.hutool.core.util.ObjectUtil;

Integer count = null;

int total = ObjectUtil.defaultIfNull(count, 0);

System.out.println(total);
```

在接口参数、数据库字段和业务对象中，如果字段允许为空，应优先使用包装类型；如果字段必须有值，进入业务逻辑前应完成非空校验或默认值处理。

```java
Integer pageNum = null;
Integer pageSize = null;

int finalPageNum = ObjectUtil.defaultIfNull(pageNum, 1);
int finalPageSize = ObjectUtil.defaultIfNull(pageSize, 10);
```

这种写法可以避免分页参数为空时触发自动拆箱异常。

### 包装类型缓存机制

Java 的部分包装类型存在缓存机制。以 `Integer` 为例，默认会缓存 `-128` 到 `127` 范围内的整数对象。

当使用自动装箱或 `Integer.valueOf()` 创建这个范围内的对象时，Java 会优先复用缓存对象。

```java
Integer a = 100;
Integer b = 100;

System.out.println(a == b); // true
```

因为 `100` 在 `Integer` 默认缓存范围内，`a` 和 `b` 指向的是同一个缓存对象。

如果数值超出默认缓存范围，通常会创建新的对象。

```java
Integer a = 200;
Integer b = 200;

System.out.println(a == b); // false
```

因此，包装类型不要使用 `==` 比较数值内容，应使用 `equals`。

```java
Integer a = 200;
Integer b = 200;

System.out.println(a.equals(b)); // true
```

为了避免空指针问题，也可以使用 `Objects.equals`。

```java
Integer a = null;
Integer b = 200;

System.out.println(Objects.equals(a, b)); // false
```

如果项目中已经引入 Hutool，也可以使用 `ObjectUtil.equals`。

```java
import cn.hutool.core.util.ObjectUtil;

Integer a = null;
Integer b = 200;

System.out.println(ObjectUtil.equals(a, b)); // false
```

常见包装类型缓存情况如下：

| 包装类型    | 缓存范围或缓存值     |
| ----------- | -------------------- |
| `Byte`      | 全部缓存             |
| `Short`     | `-128` 到 `127`      |
| `Integer`   | 默认 `-128` 到 `127` |
| `Long`      | `-128` 到 `127`      |
| `Character` | `0` 到 `127`         |
| `Boolean`   | `true` 和 `false`    |
| `Float`     | 不缓存               |
| `Double`    | 不缓存               |

包装类型缓存机制是性能优化手段，不应作为业务判断依据。开发中应统一遵循：包装类型比较内容使用 `equals`、`Objects.equals` 或 `ObjectUtil.equals`，不要使用 `==`。



## 类型转换

类型转换用于在不同数据类型之间进行转换。Java 中常见的类型转换包括基本类型之间的转换、包装类型与基本类型之间的转换、字符串与数值类型之间的转换。

类型转换需要重点关注两个问题：一是转换是否安全，二是转换过程中是否可能丢失精度或产生异常。

### 自动类型转换

自动类型转换也叫隐式类型转换，指的是 Java 在不需要开发者手动声明的情况下，自动将一种类型转换为另一种类型。

自动类型转换通常发生在小范围类型转换为大范围类型时。由于目标类型可以容纳原类型的值，因此这种转换通常是安全的。

常见的自动转换方向如下：

```text
byte -> short -> int -> long -> float -> double
char -> int -> long -> float -> double
```

整数类型之间的自动转换示例：

```java
int age = 18;
long userId = age;

System.out.println(userId); // 18
```

`int` 可以自动转换为 `long`，因为 `long` 的取值范围比 `int` 更大。

整数类型也可以自动转换为浮点类型：

```java
int count = 100;
double total = count;

System.out.println(total); // 100.0
```

需要注意的是，`long` 转换为 `float` 虽然属于自动类型转换，但可能会丢失精度。因为 `float` 的表示范围很大，但精度有限。

```java
long value = 1234567890123456789L;
float result = value;

System.out.println(result); // 可能输出近似值
```

所以，自动类型转换并不等于绝对没有风险。涉及大整数和精确计算时，需要关注精度问题。

`char` 类型也可以自动转换为 `int`，转换结果是字符对应的 Unicode 编码值。

```java
char ch = 'A';
int code = ch;

System.out.println(code); // 65
```

### 强制类型转换

强制类型转换也叫显式类型转换，指的是开发者手动将一种类型转换为另一种类型。通常发生在大范围类型转换为小范围类型时。

语法格式如下：

```java
目标类型 变量名 = (目标类型) 原始值;
```

`double` 强制转换为 `int` 时，小数部分会被直接截断，不会四舍五入。

```java
double price = 99.99;

int result = (int) price;

System.out.println(result); // 99
```

`long` 强制转换为 `int` 时，如果数值超出 `int` 的取值范围，会发生数据溢出。

```java
long value = 2147483648L;

int result = (int) value;

System.out.println(result); // -2147483648
```

这种结果通常不是业务期望的结果，因此强制类型转换需要谨慎使用。

字符和整数之间也可以进行强制类型转换：

```java
int code = 65;

char ch = (char) code;

System.out.println(ch); // A
```

强制类型转换不会自动校验业务合理性。开发中如果需要安全转换，应先判断取值范围。

```java
long value = 100L;

if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
    int result = (int) value;
    System.out.println(result);
}
```

如果项目中已经引入 Hutool，可以使用 `NumberUtil` 或 `Convert` 处理常见转换，代码会更简洁。

```java
import cn.hutool.core.convert.Convert;

String value = "100";

Integer result = Convert.toInt(value);

System.out.println(result); // 100
```

`Convert.toInt` 在处理常见字符串、数字对象转换时比较方便。如果字符串内容不是合法数字，仍然需要结合业务场景进行异常处理或默认值处理。

### 数值运算中的类型提升

数值运算中的类型提升是指多个数值参与运算时，Java 会先将较小的类型提升为较大的类型，然后再进行计算。

对于 `byte`、`short`、`char` 类型，只要参与算术运算，就会先提升为 `int`。

```java
byte a = 10;
byte b = 20;

// 编译错误：a + b 的结果是 int，不能直接赋值给 byte
// byte result = a + b;

int result = a + b;

System.out.println(result); // 30
```

`short` 类型也一样：

```java
short a = 10;
short b = 20;

int result = a + b;

System.out.println(result); // 30
```

`char` 参与运算时，也会提升为 `int`。

```java
char ch = 'A';

int result = ch + 1;

System.out.println(result); // 66
```

如果表达式中包含 `long`，整体结果会提升为 `long`。

```java
int a = 10;
long b = 20L;

long result = a + b;

System.out.println(result); // 30
```

如果表达式中包含 `float`，整体结果会提升为 `float`。

```java
int a = 10;
float b = 20.5F;

float result = a + b;

System.out.println(result); // 30.5
```

如果表达式中包含 `double`，整体结果会提升为 `double`。

```java
int a = 10;
double b = 20.5;

double result = a + b;

System.out.println(result); // 30.5
```

常见类型提升规则可以简单理解为：

| 运算场景                         | 运算结果类型 |
| -------------------------------- | ------------ |
| `byte`、`short`、`char` 参与运算 | `int`        |
| 表达式中包含 `long`              | `long`       |
| 表达式中包含 `float`             | `float`      |
| 表达式中包含 `double`            | `double`     |

需要注意，复合赋值运算符会隐含强制类型转换。

```java
byte value = 10;

value += 1;

System.out.println(value); // 11
```

上面的代码可以编译通过，因为 `value += 1` 等价于：

```java
byte value = 10;

value = (byte) (value + 1);
```

但下面这种写法不能编译通过：

```java
byte value = 10;

// 编译错误：value + 1 的结果是 int
// value = value + 1;
```

### 字符串与数值类型转换

字符串与数值类型转换在接口参数、配置读取、表单提交、数据库字段处理等场景中非常常见。

字符串转换为基本数值类型，可以使用包装类提供的解析方法。

```java
String ageText = "18";
String priceText = "99.99";

int age = Integer.parseInt(ageText);
double price = Double.parseDouble(priceText);

System.out.println(age);
System.out.println(price);
```

如果字符串内容不是合法数字，会抛出 `NumberFormatException`。

```java
String value = "abc";

// 运行时报错：NumberFormatException
// int result = Integer.parseInt(value);
```

更稳妥的写法是先判断字符串内容是否为数字。

```java
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

String value = "123";

if (StrUtil.isNotBlank(value) && NumberUtil.isNumber(value)) {
    int result = Integer.parseInt(value);
    System.out.println(result);
}
```

如果项目中使用 Hutool，可以使用 `Convert` 简化转换，并指定默认值。

```java
import cn.hutool.core.convert.Convert;

String ageText = "18";
String errorText = "abc";

Integer age = Convert.toInt(ageText, 0);
Integer errorValue = Convert.toInt(errorText, 0);

System.out.println(age);        // 18
System.out.println(errorValue); // 0
```

数值类型转换为字符串，可以使用 `String.valueOf`。

```java
int age = 18;
double price = 99.99;

String ageText = String.valueOf(age);
String priceText = String.valueOf(price);

System.out.println(ageText);
System.out.println(priceText);
```

也可以使用空字符串拼接，但不建议为了转换类型而这样写。

```java
int age = 18;

String ageText = age + "";
```

这种写法虽然简单，但语义不如 `String.valueOf` 清晰。

包装类型转换为字符串时，需要注意空值问题。

```java
Integer age = null;

String ageText = String.valueOf(age);

System.out.println(ageText); // null
```

这里输出的是字符串 `"null"`，不是空引用 `null`。如果业务上不希望出现 `"null"` 字符串，应先进行空值处理。

```java
import cn.hutool.core.util.ObjectUtil;

Integer age = null;

String ageText = ObjectUtil.isNull(age) ? "" : String.valueOf(age);

System.out.println(ageText);
```

## String 类型

`String` 是 Java 中最常用的引用类型之一，用于表示字符串。字符串在业务开发中使用非常频繁，例如用户名、手机号、接口参数、JSON 字段、日志内容、SQL 条件等。

虽然 `String` 使用起来像基础类型一样简单，但它本质上是一个引用类型，并且具有不可变性和字符串常量池机制。

### String 的不可变性

`String` 是不可变对象。字符串对象一旦创建，其内容就不能被修改。

```java
String name = "Java";

name = name + "基础";

System.out.println(name); // Java基础
```

这段代码看起来像是修改了原来的字符串，实际上是创建了一个新的字符串对象，然后让变量 `name` 指向新的对象。

也就是说，变量的引用可以改变，但原来的字符串对象内容不会改变。

```java
String value = "abc";

String result = value.toUpperCase();

System.out.println(value);  // abc
System.out.println(result); // ABC
```

`toUpperCase()` 不会修改原来的 `value`，而是返回一个新的字符串。

`String` 不可变有几个重要好处：

| 好处                | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| 安全                | 字符串常用于参数、路径、SQL、网络地址等，不可变可以减少被意外修改的风险 |
| 可缓存              | 字符串常量池可以复用字符串对象                               |
| 线程安全            | 多个线程共享同一个字符串对象时，不会出现内容被修改的问题     |
| 适合作为 Map 的 key | 不可变对象的哈希值更稳定                                     |

不过，频繁拼接字符串时，如果每次都创建新对象，会产生额外开销。

```java
String result = "";

for (int i = 0; i < 5; i++) {
    result += i;
}

System.out.println(result);
```

在循环中大量拼接字符串时，应优先使用 `StringBuilder`。

```java
StringBuilder builder = new StringBuilder();

for (int i = 0; i < 5; i++) {
    builder.append(i);
}

String result = builder.toString();

System.out.println(result);
```

### 字符串常量池

字符串常量池是 JVM 为了复用字符串对象而设计的机制。使用字面量创建字符串时，字符串会进入常量池。

```java
String a = "Java";
String b = "Java";

System.out.println(a == b); // true
```

`a` 和 `b` 都指向字符串常量池中的同一个 `"Java"` 对象，所以 `==` 结果为 `true`。

使用 `new String()` 创建字符串时，会创建新的字符串对象。

```java
String a = "Java";
String b = new String("Java");

System.out.println(a == b);      // false
System.out.println(a.equals(b)); // true
```

`==` 比较的是引用地址，`equals` 比较的是字符串内容。

所以，在业务开发中比较字符串内容，应始终使用 `equals`，不要使用 `==`。

```java
String input = "Java";

if ("Java".equals(input)) {
    System.out.println("匹配成功");
}
```

推荐把常量字符串写在前面，这样即使变量为 `null`，也不会抛出空指针异常。

```java
String input = null;

if ("Java".equals(input)) {
    System.out.println("匹配成功");
}
```

如果项目中已经引入 Hutool，可以使用 `StrUtil.equals`。

```java
import cn.hutool.core.util.StrUtil;

String input = null;

if (StrUtil.equals(input, "Java")) {
    System.out.println("匹配成功");
}
```

`StrUtil.equals` 可以安全处理 `null` 值。

### String 创建方式

`String` 常见创建方式有两种：字面量创建和构造方法创建。

字面量创建方式：

```java
String name = "Java";
```

这种方式会优先从字符串常量池中查找是否已经存在相同内容的字符串。如果存在，则直接复用；如果不存在，则创建后放入常量池。

构造方法创建方式：

```java
String name = new String("Java");
```

这种方式通常会创建新的字符串对象，不推荐在普通业务代码中这样创建字符串。

两种方式的比较如下：

```java
String a = "Java";
String b = "Java";
String c = new String("Java");

System.out.println(a == b);      // true
System.out.println(a == c);      // false
System.out.println(a.equals(c)); // true
```

字符串还可以通过字符数组创建：

```java
char[] chars = {'J', 'a', 'v', 'a'};

String value = new String(chars);

System.out.println(value); // Java
```

也可以通过字节数组创建，但需要注意字符编码。

```java
import java.nio.charset.StandardCharsets;

byte[] bytes = "Java基础".getBytes(StandardCharsets.UTF_8);

String value = new String(bytes, StandardCharsets.UTF_8);

System.out.println(value); // Java基础
```

涉及中文、文件、网络传输、接口数据时，应明确指定编码，避免不同环境默认编码不一致导致乱码。

### StringBuilder 与 StringBuffer

`StringBuilder` 和 `StringBuffer` 都用于可变字符串拼接。它们可以在原有对象基础上追加内容，适合频繁拼接字符串的场景。

| 类型            | 是否线程安全       | 性能             | 使用场景                         |
| --------------- | ------------------ | ---------------- | -------------------------------- |
| `String`        | 不可变，可安全共享 | 频繁拼接性能较低 | 少量字符串、常量字符串、普通文本 |
| `StringBuilder` | 非线程安全         | 较高             | 单线程字符串拼接                 |
| `StringBuffer`  | 线程安全           | 相对较低         | 多线程共享字符串拼接             |

日常开发中，局部变量拼接字符串通常使用 `StringBuilder`。

```java
StringBuilder builder = new StringBuilder();

builder.append("Java");
builder.append("基础");
builder.append("-");
builder.append("StringBuilder");

String result = builder.toString();

System.out.println(result); // Java基础-StringBuilder
```

`StringBuilder` 适合循环拼接：

```java
List<String> names = List.of("Java", "Spring Boot", "MyBatis-Plus");

StringBuilder builder = new StringBuilder();

for (String name : names) {
    builder.append(name).append(",");
}

String result = builder.toString();

System.out.println(result); // Java,Spring Boot,MyBatis-Plus,
```

上面代码最后会多一个逗号。实际开发中，可以使用 Hutool 的 `StrUtil.join` 简化集合拼接。

```java
import cn.hutool.core.util.StrUtil;

List<String> names = List.of("Java", "Spring Boot", "MyBatis-Plus");

String result = StrUtil.join(",", names);

System.out.println(result); // Java,Spring Boot,MyBatis-Plus
```

`StringBuffer` 的方法使用 `synchronized` 保证线程安全，但性能通常低于 `StringBuilder`。

```java
StringBuffer buffer = new StringBuffer();

buffer.append("Java");
buffer.append("基础");

String result = buffer.toString();

System.out.println(result);
```

如果字符串拼接发生在方法内部的局部变量中，通常没有线程安全问题，优先使用 `StringBuilder`。如果多个线程共享同一个可变字符串对象，才考虑 `StringBuffer` 或其他线程安全方案。

## BigDecimal 类型

`BigDecimal` 是 Java 中用于高精度数值计算的类型，常用于金额、余额、汇率、税率、折扣、积分、统计报表等场景。

`BigDecimal` 可以避免 `float` 和 `double` 在十进制小数计算中产生的精度问题，是金融、支付和电商业务中非常重要的数据类型。

### BigDecimal 的使用场景

`BigDecimal` 适合用于要求精确计算的小数场景。

常见使用场景包括：

| 场景     | 示例                         |
| -------- | ---------------------------- |
| 金额计算 | 商品价格、订单金额、支付金额 |
| 财务计算 | 余额、手续费、税费、利润     |
| 比例计算 | 折扣、费率、汇率             |
| 统计计算 | 平均值、占比、增长率         |

不推荐使用 `double` 处理金额：

```java
double a = 0.1;
double b = 0.2;

double result = a + b;

System.out.println(result); // 0.30000000000000004
```

推荐使用 `BigDecimal`：

```java
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");

BigDecimal result = a.add(b);

System.out.println(result); // 0.3
```

`BigDecimal` 的常见运算方法如下：

| 方法        | 说明                 |
| ----------- | -------------------- |
| `add`       | 加法                 |
| `subtract`  | 减法                 |
| `multiply`  | 乘法                 |
| `divide`    | 除法                 |
| `compareTo` | 数值比较             |
| `setScale`  | 设置小数位和舍入模式 |

基本运算示例：

```java
BigDecimal price = new BigDecimal("99.99");
BigDecimal count = new BigDecimal("3");

BigDecimal total = price.multiply(count);

System.out.println(total); // 299.97
```

如果项目中使用 Hutool，可以使用 `NumberUtil` 简化常见金额计算。

```java
import cn.hutool.core.util.NumberUtil;

BigDecimal price = new BigDecimal("99.99");
BigDecimal count = new BigDecimal("3");

BigDecimal total = NumberUtil.mul(price, count);

System.out.println(total); // 299.97
```

### BigDecimal 创建方式

`BigDecimal` 常见创建方式有三种：字符串构造、`valueOf` 方法、`double` 构造。

推荐方式一：使用字符串构造。

```java
BigDecimal price = new BigDecimal("99.99");
BigDecimal rate = new BigDecimal("0.15");
```

这种方式可以准确表示十进制小数，推荐用于业务代码。

推荐方式二：使用 `BigDecimal.valueOf`。

```java
BigDecimal price = BigDecimal.valueOf(99.99);
BigDecimal count = BigDecimal.valueOf(3);
```

`BigDecimal.valueOf(double)` 内部会使用 `Double.toString(double)` 的字符串结果进行转换，通常比直接使用 `new BigDecimal(double)` 更安全。

不推荐方式：直接使用 `double` 构造。

```java
BigDecimal value = new BigDecimal(0.1);

System.out.println(value);
```

输出结果可能是：

```text
0.1000000000000000055511151231257827021181583404541015625
```

原因是 `0.1` 这个 `double` 值在传入 `BigDecimal` 构造方法前，已经不是精确的十进制 `0.1`。

推荐写法如下：

```java
BigDecimal value1 = new BigDecimal("0.1");
BigDecimal value2 = BigDecimal.valueOf(0.1);

System.out.println(value1); // 0.1
System.out.println(value2); // 0.1
```

如果字符串来源于接口参数，需要先判断是否为空或是否合法。

```java
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

String amountText = "99.99";

if (StrUtil.isNotBlank(amountText) && NumberUtil.isNumber(amountText)) {
    BigDecimal amount = new BigDecimal(amountText);
    System.out.println(amount);
}
```

如果使用 Hutool，也可以使用 `Convert` 转换，并设置默认值。

```java
import cn.hutool.core.convert.Convert;

String amountText = "99.99";

BigDecimal amount = Convert.toBigDecimal(amountText, BigDecimal.ZERO);

System.out.println(amount); // 99.99
```

### 精度问题

`BigDecimal` 本身可以表示高精度小数，但错误的创建方式、除法运算和比较方式仍然可能产生问题。

首先，避免使用 `new BigDecimal(double)`。

```java
BigDecimal value = new BigDecimal(0.1);

System.out.println(value); // 精度异常
```

应改为：

```java
BigDecimal value = new BigDecimal("0.1");

System.out.println(value); // 0.1
```

其次，`BigDecimal` 除法可能出现除不尽的情况。如果没有指定小数位和舍入模式，会抛出 `ArithmeticException`。

```java
BigDecimal a = new BigDecimal("10");
BigDecimal b = new BigDecimal("3");

// 运行时报错：ArithmeticException
// BigDecimal result = a.divide(b);
```

正确写法是指定小数位和舍入模式。

```java
import java.math.RoundingMode;

BigDecimal a = new BigDecimal("10");
BigDecimal b = new BigDecimal("3");

BigDecimal result = a.divide(b, 2, RoundingMode.HALF_UP);

System.out.println(result); // 3.33
```

再次，`BigDecimal` 的 `equals` 会比较数值和精度，`compareTo` 只比较数值大小。

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");

System.out.println(a.equals(b));    // false
System.out.println(a.compareTo(b)); // 0
```

所以，在业务中判断两个金额是否相等，通常使用 `compareTo`。

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");

if (a.compareTo(b) == 0) {
    System.out.println("金额相等");
}
```

如果项目中使用 Hutool，可以使用 `NumberUtil.equals` 进行数值比较。

```java
import cn.hutool.core.util.NumberUtil;

BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");

if (NumberUtil.equals(a, b)) {
    System.out.println("金额相等");
}
```

### 舍入模式

舍入模式用于控制小数位保留时的取舍规则。`BigDecimal` 通常配合 `setScale` 或 `divide` 使用舍入模式。

常见舍入模式如下：

| 舍入模式                 | 说明                             |
| ------------------------ | -------------------------------- |
| `RoundingMode.HALF_UP`   | 四舍五入，最常用                 |
| `RoundingMode.HALF_DOWN` | 五舍六入                         |
| `RoundingMode.HALF_EVEN` | 银行家舍入法，向最接近的偶数舍入 |
| `RoundingMode.UP`        | 远离 0 方向舍入                  |
| `RoundingMode.DOWN`      | 向 0 方向舍入，直接截断          |
| `RoundingMode.CEILING`   | 向正无穷方向舍入                 |
| `RoundingMode.FLOOR`     | 向负无穷方向舍入                 |

保留两位小数并四舍五入：

```java
import java.math.RoundingMode;

BigDecimal amount = new BigDecimal("99.995");

BigDecimal result = amount.setScale(2, RoundingMode.HALF_UP);

System.out.println(result); // 100.00
```

直接截断小数：

```java
import java.math.RoundingMode;

BigDecimal amount = new BigDecimal("99.999");

BigDecimal result = amount.setScale(2, RoundingMode.DOWN);

System.out.println(result); // 99.99
```

除法中使用舍入模式：

```java
import java.math.RoundingMode;

BigDecimal total = new BigDecimal("10");
BigDecimal count = new BigDecimal("3");

BigDecimal average = total.divide(count, 2, RoundingMode.HALF_UP);

System.out.println(average); // 3.33
```

使用 Hutool 处理保留小数：

```java
import cn.hutool.core.util.NumberUtil;

BigDecimal amount = new BigDecimal("99.995");

BigDecimal result = NumberUtil.round(amount, 2);

System.out.println(result); // 100.00
```

实际开发中，金额展示和金额计算要区分清楚。计算过程中不要过早舍入，避免多次计算导致误差累积；通常在最终展示、入库或生成账单结果时统一处理小数位。

比较推荐的金额处理原则如下：

| 场景       | 建议                                         |
| ---------- | -------------------------------------------- |
| 金额创建   | 使用字符串或 `BigDecimal.valueOf`            |
| 金额计算   | 使用 `add`、`subtract`、`multiply`、`divide` |
| 金额比较   | 使用 `compareTo` 或 `NumberUtil.equals`      |
| 金额除法   | 必须指定小数位和舍入模式                     |
| 金额展示   | 使用 `setScale` 或格式化工具                 |
| 金额默认值 | 使用 `BigDecimal.ZERO`                       |



## 对象比较

对象比较是 Java 开发中的高频基础问题。基本类型可以直接使用 `==` 比较值，但引用类型需要区分“引用地址比较”和“内容比较”。

在业务开发中，常见比较对象包括 `String`、`Integer`、`Long`、`BigDecimal`、实体对象、DTO 对象和集合元素。错误使用比较方式，容易导致条件判断失效、集合去重异常、Map 查询失败等问题。

### 等号比较

`==` 是 Java 中的等号比较运算符。对于不同类型，`==` 的含义不同。

对于基本类型，`==` 比较的是具体值。

```java
int a = 100;
int b = 100;

System.out.println(a == b); // true
```

对于引用类型，`==` 比较的是两个变量是否指向同一个对象。

```java
String a = new String("Java");
String b = new String("Java");

System.out.println(a == b); // false
```

虽然 `a` 和 `b` 的字符串内容相同，但它们是两个不同对象，所以 `==` 返回 `false`。

字符串字面量因为存在字符串常量池，可能会出现 `==` 返回 `true` 的情况。

```java
String a = "Java";
String b = "Java";

System.out.println(a == b); // true
```

这并不代表字符串内容比较应该使用 `==`。这里只是因为两个变量刚好指向常量池中的同一个字符串对象。

包装类型也不建议使用 `==` 比较内容。

```java
Integer a = 200;
Integer b = 200;

System.out.println(a == b); // false
```

`Integer` 默认缓存 `-128` 到 `127` 范围内的对象，超出范围后通常会创建新对象。

```java
Integer a = 100;
Integer b = 100;

System.out.println(a == b); // true
```

这种结果容易误导开发者。因此，包装类型比较内容时，应使用 `equals`、`Objects.equals` 或 Hutool 的 `ObjectUtil.equals`。

### equals 方法

`equals` 方法用于比较两个对象是否“相等”。它定义在 `Object` 类中，所有 Java 对象都继承该方法。

如果一个类没有重写 `equals` 方法，那么默认比较效果和 `==` 类似，比较的是对象引用地址。

```java
Object a = new Object();
Object b = new Object();

System.out.println(a.equals(b)); // false
```

`String` 已经重写了 `equals` 方法，所以可以用于比较字符串内容。

```java
String a = new String("Java");
String b = new String("Java");

System.out.println(a.equals(b)); // true
```

字符串比较时，推荐把常量放在前面，避免变量为 `null` 时出现空指针异常。

```java
String type = null;

if ("admin".equals(type)) {
    System.out.println("管理员");
}
```

如果写成下面这样，当 `type` 为 `null` 时会出现 `NullPointerException`。

```java
String type = null;

// 运行时报错：NullPointerException
// if (type.equals("admin")) {
//     System.out.println("管理员");
// }
```

自定义对象如果需要根据业务字段判断是否相等，需要重写 `equals` 方法。

下面代码演示了一个用户对象根据 `id` 判断是否为同一个用户。

```java
package io.github.atengk.type;

import java.util.Objects;

/**
 * 用户信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class User {

    private Long id;

    private String username;

    public User(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    /**
     * 判断两个用户是否相等
     *
     * @param obj 待比较对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof User user)) {
            return false;
        }
        return Objects.equals(this.id, user.id);
    }

    /**
     * 根据用户 ID 生成哈希值
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    /**
     * 获取用户 ID
     *
     * @return 用户 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }
}
```

使用示例：

```java
User user1 = new User(1L, "Ateng");
User user2 = new User(1L, "Java");

System.out.println(user1.equals(user2)); // true
```

这段代码中，两个对象的 `username` 不同，但 `id` 相同，所以根据当前业务规则认为它们是同一个用户。

重写 `equals` 时需要注意以下规则：

| 规则   | 说明                                                         |
| ------ | ------------------------------------------------------------ |
| 自反性 | `x.equals(x)` 必须为 `true`                                  |
| 对称性 | `x.equals(y)` 和 `y.equals(x)` 结果应一致                    |
| 传递性 | 如果 `x.equals(y)`、`y.equals(z)` 为 `true`，则 `x.equals(z)` 也应为 `true` |
| 一致性 | 对象字段未变化时，多次调用结果应一致                         |
| 非空性 | `x.equals(null)` 必须为 `false`                              |

实际开发中，如果使用 Lombok，可以通过 `@EqualsAndHashCode` 生成 `equals` 和 `hashCode`，但仍然要明确基于哪些字段比较，避免错误地把所有字段都纳入比较。

### hashCode 方法

`hashCode` 方法用于返回对象的哈希值。它主要用于哈希结构，例如 `HashMap`、`HashSet`、`Hashtable` 等。

`equals` 和 `hashCode` 有一个非常重要的约定：

如果两个对象通过 `equals` 比较相等，那么它们的 `hashCode` 必须相等。

如果只重写 `equals`，不重写 `hashCode`，在 `HashSet`、`HashMap` 等集合中可能出现异常行为。

下面示例演示对象在 `HashSet` 中的去重逻辑。

```java
Set<User> users = new HashSet<>();

users.add(new User(1L, "Ateng"));
users.add(new User(1L, "Java"));

System.out.println(users.size()); // 1
```

由于 `User` 根据 `id` 重写了 `equals` 和 `hashCode`，两个 `id` 相同的对象会被认为是重复元素。

如果没有正确重写 `hashCode`，即使 `equals` 返回 `true`，`HashSet` 也可能无法正确去重。

常见原则如下：

| 场景                          | 建议                                            |
| ----------------------------- | ----------------------------------------------- |
| 重写 `equals`                 | 必须同时重写 `hashCode`                         |
| 使用对象作为 `HashMap` 的 key | 必须保证 `equals` 和 `hashCode` 稳定            |
| 使用可变字段生成 `hashCode`   | 谨慎使用，字段变化后可能导致集合查找失败        |
| 实体对象比较                  | 优先选择稳定唯一字段，例如主键 ID、业务唯一编码 |

不要使用容易变化的字段生成 `hashCode`。例如把 `username`、`status`、`updateTime` 等可变字段作为哈希依据，可能导致对象放入 `HashSet` 后无法被正确查找。

```java
Set<User> users = new HashSet<>();

User user = new User(1L, "Ateng");
users.add(user);

System.out.println(users.contains(user)); // true
```

如果参与 `hashCode` 的字段在加入集合后被修改，`contains`、`remove` 等操作可能出现异常结果。因此，作为哈希依据的字段应尽量稳定。

### Objects 工具类

`Objects` 是 JDK 提供的工具类，位于 `java.util` 包下，常用于对象比较、空值判断、哈希值生成等场景。

常用方法如下：

| 方法                          | 说明                                |
| ----------------------------- | ----------------------------------- |
| `Objects.equals(a, b)`        | 安全比较两个对象是否相等            |
| `Objects.isNull(obj)`         | 判断对象是否为 `null`               |
| `Objects.nonNull(obj)`        | 判断对象是否不为 `null`             |
| `Objects.hash(...)`           | 根据多个字段生成哈希值              |
| `Objects.requireNonNull(obj)` | 要求对象不能为 `null`，否则抛出异常 |

`Objects.equals` 可以安全处理空值。

```java
String a = null;
String b = "Java";

System.out.println(Objects.equals(a, b)); // false
```

它的效果类似下面的逻辑：

```java
boolean result = a == b || (a != null && a.equals(b));
```

对象空值判断：

```java
String name = null;

if (Objects.isNull(name)) {
    System.out.println("名称为空");
}

if (Objects.nonNull(name)) {
    System.out.println("名称不为空");
}
```

生成哈希值：

```java
Long id = 1L;
String username = "Ateng";

int hash = Objects.hash(id, username);

System.out.println(hash);
```

参数非空校验：

```java
String username = null;

// 运行时报错：NullPointerException
// Objects.requireNonNull(username, "用户名不能为空");
```

如果项目中已经引入 Hutool，也可以使用 `ObjectUtil`，可读性较好。

```java
import cn.hutool.core.util.ObjectUtil;

Integer a = null;
Integer b = 100;

System.out.println(ObjectUtil.equals(a, b)); // false

if (ObjectUtil.isNull(a)) {
    System.out.println("数据为空");
}
```

对于字符串判断，推荐使用 Hutool 的 `StrUtil`。

```java
import cn.hutool.core.util.StrUtil;

String username = " ";

if (StrUtil.isBlank(username)) {
    System.out.println("用户名为空");
}
```

`StrUtil.isBlank` 会把 `null`、空字符串和全空白字符串都视为空，在接口参数校验中比较常用。

## 常见问题

这一部分整理 Java 类型基础中最容易出现的比较和精度问题。重点关注包装类型比较、`BigDecimal` 比较、浮点数精度和字符串比较。

### Integer 比较问题

`Integer` 是 `int` 的包装类型。由于 `Integer` 是引用类型，使用 `==` 比较时比较的是对象引用，而不是单纯的数值内容。

```java
Integer a = 100;
Integer b = 100;

System.out.println(a == b); // true
```

这个结果为 `true`，是因为 `Integer` 默认缓存了 `-128` 到 `127` 范围内的对象。

当数值超出缓存范围后，结果可能变为 `false`。

```java
Integer a = 200;
Integer b = 200;

System.out.println(a == b); // false
```

这类问题在开发中很隐蔽，因为小数字测试时可能正常，大数字上线后可能异常。

正确写法是使用 `Objects.equals` 或 Hutool 的 `ObjectUtil.equals`。

```java
Integer a = 200;
Integer b = 200;

System.out.println(Objects.equals(a, b)); // true
```

使用 Hutool：

```java
import cn.hutool.core.util.ObjectUtil;

Integer a = 200;
Integer b = 200;

System.out.println(ObjectUtil.equals(a, b)); // true
```

还需要注意自动拆箱导致的空指针问题。

```java
Integer count = null;

// 运行时报错：NullPointerException
// if (count > 0) {
//     System.out.println("数量大于 0");
// }
```

因为 `count > 0` 会触发自动拆箱，相当于调用 `count.intValue()`。

安全写法：

```java
import cn.hutool.core.util.ObjectUtil;

Integer count = null;

int finalCount = ObjectUtil.defaultIfNull(count, 0);

if (finalCount > 0) {
    System.out.println("数量大于 0");
}
```

开发建议如下：

| 场景                          | 推荐写法                  |
| ----------------------------- | ------------------------- |
| 比较两个 `Integer` 是否相等   | `Objects.equals(a, b)`    |
| 使用 Hutool 比较              | `ObjectUtil.equals(a, b)` |
| 判断 `Integer` 是否大于某个值 | 先处理 `null`，再比较     |
| DTO 字段可能为空              | 使用包装类型              |
| 局部计算必须有值              | 转成基本类型前先给默认值  |

### BigDecimal 比较问题

`BigDecimal` 比较时最常见的问题是误用 `equals`。

`BigDecimal.equals` 不仅比较数值，还比较小数位精度。

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");

System.out.println(a.equals(b));    // false
System.out.println(a.compareTo(b)); // 0
```

`1.0` 和 `1.00` 在数值上相等，但精度不同，所以 `equals` 返回 `false`。

业务中比较金额、余额、费率等数值是否相等，通常使用 `compareTo`。

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");

if (a.compareTo(b) == 0) {
    System.out.println("数值相等");
}
```

判断大小时也使用 `compareTo`。

```java
BigDecimal amount = new BigDecimal("99.99");
BigDecimal limit = new BigDecimal("100");

if (amount.compareTo(limit) < 0) {
    System.out.println("金额小于限制");
}

if (amount.compareTo(limit) <= 0) {
    System.out.println("金额小于或等于限制");
}

if (amount.compareTo(BigDecimal.ZERO) > 0) {
    System.out.println("金额大于 0");
}
```

如果项目中使用 Hutool，可以使用 `NumberUtil.equals` 比较数值。

```java
import cn.hutool.core.util.NumberUtil;

BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");

System.out.println(NumberUtil.equals(a, b)); // true
```

`BigDecimal` 还需要注意创建方式。不要使用 `new BigDecimal(double)`。

```java
BigDecimal value = new BigDecimal(0.1);

System.out.println(value); // 可能输出很长的小数
```

推荐写法：

```java
BigDecimal value1 = new BigDecimal("0.1");
BigDecimal value2 = BigDecimal.valueOf(0.1);

System.out.println(value1);
System.out.println(value2);
```

开发建议如下：

| 场景             | 推荐写法                                |
| ---------------- | --------------------------------------- |
| 创建金额         | `new BigDecimal("99.99")`               |
| 从 `double` 转换 | `BigDecimal.valueOf(99.99)`             |
| 判断金额相等     | `a.compareTo(b) == 0`                   |
| 判断金额大于 0   | `amount.compareTo(BigDecimal.ZERO) > 0` |
| Hutool 数值比较  | `NumberUtil.equals(a, b)`               |
| 除法运算         | 必须指定小数位和舍入模式                |

### 浮点数精度问题

`float` 和 `double` 使用二进制浮点数表示小数，很多十进制小数无法被精确表示。因此，浮点数计算可能出现精度误差。

```java
double result = 0.1 + 0.2;

System.out.println(result); // 0.30000000000000004
```

这不是 Java 独有的问题，而是二进制浮点数表示十进制小数时的常见现象。

因此，不建议直接使用 `==` 比较浮点数。

```java
double result = 0.1 + 0.2;

// 不推荐
System.out.println(result == 0.3); // false
```

如果只是普通科学计算或非金额类业务，可以使用误差范围比较。

```java
double result = 0.1 + 0.2;
double expected = 0.3;
double epsilon = 0.000001;

if (Math.abs(result - expected) < epsilon) {
    System.out.println("近似相等");
}
```

如果是金额、余额、支付、财务、统计结算等场景，应使用 `BigDecimal`。

```java
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");

BigDecimal result = a.add(b);

System.out.println(result); // 0.3
```

使用 Hutool 简化计算：

```java
import cn.hutool.core.util.NumberUtil;

BigDecimal result = NumberUtil.add(new BigDecimal("0.1"), new BigDecimal("0.2"));

System.out.println(result); // 0.3
```

浮点数开发建议如下：

| 场景           | 建议                                            |
| -------------- | ----------------------------------------------- |
| 金额计算       | 使用 `BigDecimal`                               |
| 普通小数计算   | 可以使用 `double`                               |
| 浮点数相等判断 | 使用误差范围                                    |
| 数据库存金额   | 使用 `DECIMAL` 类型，不使用 `FLOAT` 或 `DOUBLE` |
| 接口金额字段   | 推荐使用字符串或 `BigDecimal` 接收              |

### 字符串比较问题

字符串比较最常见的问题是使用 `==` 比较内容。

```java
String a = new String("Java");
String b = new String("Java");

System.out.println(a == b);      // false
System.out.println(a.equals(b)); // true
```

`==` 比较的是引用地址，`equals` 比较的是字符串内容。

字符串字面量可能因为常量池复用导致 `==` 返回 `true`。

```java
String a = "Java";
String b = "Java";

System.out.println(a == b); // true
```

这只是常量池复用的结果，不能作为字符串内容比较的通用写法。

正确写法是使用 `equals`。

```java
String type = "admin";

if ("admin".equals(type)) {
    System.out.println("管理员");
}
```

把常量写在前面，可以避免变量为 `null` 时出现空指针异常。

```java
String type = null;

if ("admin".equals(type)) {
    System.out.println("管理员");
}
```

如果需要忽略大小写，可以使用 `equalsIgnoreCase`。

```java
String type = "ADMIN";

if ("admin".equalsIgnoreCase(type)) {
    System.out.println("管理员");
}
```

如果项目中使用 Hutool，推荐使用 `StrUtil.equals` 和 `StrUtil.equalsIgnoreCase`。

```java
import cn.hutool.core.util.StrUtil;

String type = null;

if (StrUtil.equals(type, "admin")) {
    System.out.println("管理员");
}

if (StrUtil.equalsIgnoreCase(type, "ADMIN")) {
    System.out.println("管理员");
}
```

判断字符串是否为空时，不建议只使用 `str != null`，因为空字符串和空白字符串也可能是无效数据。

```java
String username = " ";

if (username != null) {
    System.out.println("这里只能判断不是 null，不能判断是否有有效内容");
}
```

推荐使用 Hutool 的 `StrUtil.isBlank` 或 `StrUtil.isNotBlank`。

```java
import cn.hutool.core.util.StrUtil;

String username = " ";

if (StrUtil.isBlank(username)) {
    System.out.println("用户名不能为空");
}
```

字符串比较开发建议如下：

| 场景              | 推荐写法                                  |
| ----------------- | ----------------------------------------- |
| 比较字符串内容    | `"固定值".equals(value)`                  |
| 忽略大小写比较    | `"固定值".equalsIgnoreCase(value)`        |
| Hutool 内容比较   | `StrUtil.equals(value, target)`           |
| Hutool 忽略大小写 | `StrUtil.equalsIgnoreCase(value, target)` |
| 判断空字符串      | `StrUtil.isBlank(value)`                  |
| 判断非空字符串    | `StrUtil.isNotBlank(value)`               |
| 避免空指针        | 常量放前面，或使用工具类                  |

对象比较的整体原则可以总结为：基本类型用 `==`，引用类型比较内容优先用 `equals`，空值安全比较使用 `Objects.equals` 或 `ObjectUtil.equals`，金额比较使用 `BigDecimal.compareTo`，字符串判断优先使用 `equals` 或 `StrUtil`。
