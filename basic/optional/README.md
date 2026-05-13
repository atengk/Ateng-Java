# Optional

## Optional 概述

`Optional` 是 Java 8 引入的空值处理工具类，位于 `java.util` 包下。它本质上是一个容器对象，用来表示一个值“可能存在，也可能不存在”。

在 Java 项目开发中，`Optional` 的核心价值不是完全替代 `null`，而是让方法返回值中的“可能为空”语义更加明确。通过 `Optional`，调用方可以更直观地知道当前结果可能不存在，并在代码层面显式处理空值、默认值或业务异常。

`Optional` 常用于查询结果、配置读取、字段转换、条件过滤和业务异常处理等场景。合理使用 `Optional` 可以减少隐式空指针风险，使代码表达更加清晰，但过度使用也会降低代码可读性。

### Optional 的定位

`Optional` 的定位是“可能为空的返回值容器”。它主要用于方法返回值，用来表达某个方法的执行结果可能不存在。

传统写法中，方法返回 `null` 时，调用方必须依赖经验或文档说明判断是否需要空值校验。

```java
User user = userService.getById(userId);
if (user == null) {
    throw new ServiceException("用户不存在");
}
```

使用 `Optional` 后，方法签名本身就能表达结果可能为空。

```java
Optional<User> optionalUser = userService.findById(userId);

User user = optionalUser.orElseThrow(() -> new ServiceException("用户不存在"));
```

从项目开发角度看，`Optional` 更适合作为 API 设计中的语义约束。它可以提醒调用方必须关注空值结果，而不是默认认为方法一定会返回有效对象。

`Optional` 的主要定位如下：

| 定位         | 说明                                         |
| ------------ | -------------------------------------------- |
| 返回值容器   | 表示方法返回结果可能存在，也可能不存在       |
| 空值语义表达 | 让调用方通过方法签名感知空值风险             |
| 链式处理工具 | 配合 `map`、`flatMap`、`filter` 进行安全转换 |
| 异常转换工具 | 配合 `orElseThrow` 将空结果转换为业务异常    |

需要注意的是，`Optional` 不是所有 `null` 的替代品。它不适合用于实体类字段、方法参数和集合外层包装，也不适合强行改造所有普通空值判断逻辑。

### Optional 解决的问题

`Optional` 主要解决的是“空值返回不明确”和“调用方容易遗漏空值判断”的问题。

在 Java 项目中，很多空指针异常并不是因为业务逻辑复杂，而是因为方法返回了 `null`，但调用方没有及时判断。例如，根据用户 ID 查询用户时，如果用户不存在，直接访问用户属性就可能出现空指针异常。

```java
User user = userService.getById(userId);
String username = user.getUsername();
```

如果 `getById` 返回 `null`，上面的代码会抛出 `NullPointerException`。使用 `Optional` 后，可以把空值处理集中到结果获取阶段。

```java
String username = userService.findById(userId)
        .map(User::getUsername)
        .orElse("未知用户");
```

`Optional` 还可以减少多层对象访问时的重复空值判断。传统写法中，如果需要访问用户扩展信息中的邮箱，通常需要多层 `if` 判断。

```java
String email = null;
if (user != null && user.getProfile() != null) {
    email = user.getProfile().getEmail();
}
```

使用 `Optional` 后，可以将多层空值判断转换为链式处理。

```java
String email = Optional.ofNullable(user)
        .map(User::getProfile)
        .map(UserProfile::getEmail)
        .orElse("");
```

在默认值处理上，`Optional` 也可以让代码表达更加统一。例如，当用户昵称为空时，使用用户名作为默认昵称。

```java
String nickname = Optional.ofNullable(user.getNickname())
        .orElse(user.getUsername());
```

如果默认值的计算逻辑比较复杂，建议使用 `orElseGet`，避免默认值逻辑被提前执行。

```java
String nickname = Optional.ofNullable(user.getNickname())
        .orElseGet(() -> buildDefaultNickname(user));
```

在项目开发中，`Optional` 还经常用于将空结果转换为业务异常。例如，查询用户不存在时，直接抛出明确的业务异常。

```java
User user = userService.findById(userId)
        .orElseThrow(() -> new ServiceException("用户不存在"));
```

结合 Hutool 工具类时，可以在 `filter` 中处理字符串校验逻辑，使字段转换更加简洁。

```java
String displayName = Optional.ofNullable(user.getNickname())
        .filter(cn.hutool.core.util.StrUtil::isNotBlank)
        .orElse(user.getUsername());
```

总体来看，`Optional` 解决的不是所有空值问题，而是让“结果可能为空”这件事显式化、标准化，并强制调用方在合适的位置处理空值。

### Optional 的适用边界

`Optional` 有明确的适用边界。它适合用于方法返回值，尤其是返回单个对象且该对象可能不存在的场景。

推荐使用场景包括：

| 场景         | 是否推荐 | 说明                               |
| ------------ | -------- | ---------------------------------- |
| 查询单个对象 | 推荐     | 例如根据 ID 查询用户、订单、配置   |
| 读取可选配置 | 推荐     | 配置项不存在时可使用默认值         |
| 简单字段转换 | 推荐     | 配合 `map` 完成空值安全转换        |
| 条件过滤     | 推荐     | 配合 `filter` 判断结果是否满足条件 |
| 业务异常抛出 | 推荐     | 配合 `orElseThrow` 处理空结果      |

例如，Service 层可以将可能为空的查询结果包装为 `Optional` 返回。

```java
public Optional<User> findById(Long userId) {
    return Optional.ofNullable(userMapper.selectById(userId));
}
```

调用方根据业务场景决定如何处理结果。

```java
User user = userService.findById(userId)
        .orElseThrow(() -> new ServiceException("用户不存在"));
```

不推荐在实体类字段中使用 `Optional`。实体类通常需要参与 ORM 映射、JSON 序列化、参数绑定和对象转换，字段使用 `Optional` 会增加框架处理复杂度，也不符合常见 Java Bean 规范。

不推荐：

```java
public class User {
    private Optional<String> nickname;
}
```

推荐：

```java
public class User {
    private String nickname;
}
```

不推荐在方法参数中使用 `Optional`。方法参数是否允许为空，应通过参数校验、方法重载、DTO 字段约束或业务规则表达。

不推荐：

```java
public void updateNickname(Long userId, Optional<String> nickname) {
    // 不推荐
}
```

推荐：

```java
public void updateNickname(Long userId, String nickname) {
    if (cn.hutool.core.util.StrUtil.isBlank(nickname)) {
        log.info("昵称为空，不执行更新，用户ID：{}", userId);
        return;
    }

    // 执行更新逻辑
}
```

不推荐在集合类型外层使用 `Optional`。集合为空通常可以直接返回空集合，调用方处理起来更简单。

不推荐：

```java
public Optional<List<User>> listUsers() {
    // 不推荐
}
```

推荐：

```java
public List<User> listUsers() {
    return Collections.emptyList();
}
```

此外，不建议为了追求链式写法而过度使用 `Optional`。如果业务逻辑中包含多个分支判断、日志记录、状态变更、外部接口调用或复杂异常处理，普通 `if` 判断通常更清晰，也更容易调试。

`Optional` 的使用原则可以总结为：用于表达“返回结果可能不存在”，而不是用于替代所有空值判断；用于提升 API 语义和代码可读性，而不是为了让代码看起来更加函数式。



## 基础用法

本节介绍 `Optional` 的基础创建、判断、取值和默认值处理方式。项目开发中使用 `Optional` 时，应优先关注代码语义是否清晰，而不是单纯追求链式写法。

### 创建 Optional 对象

创建 `Optional` 对象主要有三种方式：`Optional.empty()`、`Optional.of()` 和 `Optional.ofNullable()`。不同创建方式对应不同的空值语义。

`Optional.empty()` 用于创建一个不包含任何值的空 `Optional`。

```java
Optional<User> optionalUser = Optional.empty();
```

`Optional.of()` 用于包装一个确定不为空的对象。如果传入 `null`，会直接抛出 `NullPointerException`。

```java
User user = new User();
Optional<User> optionalUser = Optional.of(user);
```

下面这种写法不推荐，因为 `user` 一旦为 `null`，代码会直接抛出异常。

```java
User user = null;
Optional<User> optionalUser = Optional.of(user);
```

`Optional.ofNullable()` 用于包装一个可能为空的对象。如果传入的对象为 `null`，会得到 `Optional.empty()`；如果不为空，则得到包含该对象的 `Optional`。

```java
User user = userMapper.selectById(userId);
Optional<User> optionalUser = Optional.ofNullable(user);
```

在项目开发中，最常用的是 `Optional.ofNullable()`。它适合处理数据库查询结果、外部接口响应、配置读取结果等可能为空的数据。

```java
public Optional<User> findById(Long userId) {
    User user = userMapper.selectById(userId);
    return Optional.ofNullable(user);
}
```

三种创建方式的区别如下：

| 创建方式                     | 是否允许传入 null | 适用场景              |
| ---------------------------- | ----------------- | --------------------- |
| `Optional.empty()`           | 不涉及            | 明确表示没有值        |
| `Optional.of(value)`         | 不允许            | 明确知道 value 不为空 |
| `Optional.ofNullable(value)` | 允许              | value 可能为空        |

实际项目中，如果不能完全确定对象一定不为空，应优先使用 `Optional.ofNullable()`，避免因为误用 `Optional.of()` 引入新的空指针异常。

### 判断值是否存在

`Optional` 提供了 `isPresent()` 和 `isEmpty()` 用于判断值是否存在。

`isPresent()` 表示当前 `Optional` 中是否存在值。

```java
Optional<User> optionalUser = userService.findById(userId);

if (optionalUser.isPresent()) {
    User user = optionalUser.get();
    log.info("查询到用户，用户ID：{}，用户名：{}", userId, user.getUsername());
}
```

`isEmpty()` 是 Java 11 引入的方法，用于判断当前 `Optional` 是否为空。

```java
Optional<User> optionalUser = userService.findById(userId);

if (optionalUser.isEmpty()) {
    log.info("用户不存在，用户ID：{}", userId);
    throw new ServiceException("用户不存在");
}
```

如果项目使用的是 Java 8，则不能使用 `isEmpty()`，可以使用 `!isPresent()` 替代。

```java
Optional<User> optionalUser = userService.findById(userId);

if (!optionalUser.isPresent()) {
    throw new ServiceException("用户不存在");
}
```

`Optional` 还提供了 `ifPresent()`，用于在值存在时执行指定逻辑。

```java
userService.findById(userId)
        .ifPresent(user -> log.info("查询到用户，用户ID：{}，用户名：{}", userId, user.getUsername()));
```

如果需要同时处理“存在”和“不存在”两种情况，Java 9 之后可以使用 `ifPresentOrElse()`。

```java
userService.findById(userId)
        .ifPresentOrElse(
                user -> log.info("查询到用户，用户ID：{}，用户名：{}", userId, user.getUsername()),
                () -> log.info("用户不存在，用户ID：{}", userId)
        );
```

需要注意，`isPresent()` 加 `get()` 的写法虽然可用，但如果只是为了取值或抛异常，通常不如 `orElseThrow()` 清晰。

不推荐频繁写成：

```java
Optional<User> optionalUser = userService.findById(userId);

if (optionalUser.isPresent()) {
    return optionalUser.get();
}

throw new ServiceException("用户不存在");
```

推荐写成：

```java
return userService.findById(userId)
        .orElseThrow(() -> new ServiceException("用户不存在"));
```

### 获取 Optional 中的值

获取 `Optional` 中的值可以使用 `get()`，但不推荐直接使用。因为当 `Optional` 为空时，`get()` 会抛出 `NoSuchElementException`。

```java
Optional<User> optionalUser = userService.findById(userId);
User user = optionalUser.get();
```

上面的写法只有在你非常确定 `Optional` 一定有值时才可以使用。但在大多数业务代码中，“非常确定”往往只是开发者的主观判断，后续需求变更或数据异常都可能导致运行时错误。

推荐使用 `orElseThrow()` 获取值，并在值不存在时抛出明确的业务异常。

```java
User user = userService.findById(userId)
        .orElseThrow(() -> new ServiceException("用户不存在"));
```

如果业务允许为空时使用默认对象，可以使用 `orElse()`。

```java
User defaultUser = new User();
defaultUser.setUsername("默认用户");

User user = userService.findById(userId)
        .orElse(defaultUser);
```

如果默认对象的创建逻辑比较复杂，建议使用 `orElseGet()` 延迟创建。

```java
User user = userService.findById(userId)
        .orElseGet(() -> buildDefaultUser(userId));
```

如果只需要在值存在时执行业务逻辑，不需要显式获取对象，可以使用 `ifPresent()`。

```java
userService.findById(userId)
        .ifPresent(user -> log.info("处理用户数据，用户ID：{}，用户名：{}", user.getId(), user.getUsername()));
```

常见取值方式可以按业务语义选择：

| 取值方式                         | 空值处理                            | 推荐程度 |
| -------------------------------- | ----------------------------------- | -------- |
| `get()`                          | 空值时抛出 `NoSuchElementException` | 不推荐   |
| `orElse(defaultValue)`           | 空值时返回默认值                    | 推荐     |
| `orElseGet(supplier)`            | 空值时延迟生成默认值                | 推荐     |
| `orElseThrow(exceptionSupplier)` | 空值时抛出指定异常                  | 推荐     |
| `ifPresent(consumer)`            | 有值时执行逻辑                      | 推荐     |

### 默认值处理

`Optional` 的默认值处理主要通过 `orElse()` 和 `orElseGet()` 完成。

`orElse()` 用于直接提供一个默认值。无论 `Optional` 中是否有值，`orElse()` 中的默认值表达式都会被执行。

```java
String nickname = Optional.ofNullable(user.getNickname())
        .orElse("默认昵称");
```

当默认值是固定值时，使用 `orElse()` 比较合适。

```java
Integer status = Optional.ofNullable(user.getStatus())
        .orElse(0);
```

当默认值需要通过方法计算、数据库查询、远程接口调用或复杂对象构建得到时，应使用 `orElseGet()`。

```java
String nickname = Optional.ofNullable(user.getNickname())
        .orElseGet(() -> buildDefaultNickname(user));
```

下面的代码用于说明 `orElse()` 和 `orElseGet()` 在执行时机上的区别。

```java
String nickname = Optional.ofNullable(user.getNickname())
        .orElse(buildDefaultNickname(user));

String nicknameLazy = Optional.ofNullable(user.getNickname())
        .orElseGet(() -> buildDefaultNickname(user));
```

第一段代码中，即使 `user.getNickname()` 不为空，`buildDefaultNickname(user)` 也会被执行。第二段代码中，只有当 `user.getNickname()` 为空时，`buildDefaultNickname(user)` 才会执行。

在项目开发中，可以按以下规则选择：

| 场景                           | 推荐方法        |
| ------------------------------ | --------------- |
| 默认值是固定字符串、数字、枚举 | `orElse()`      |
| 默认值需要调用方法生成         | `orElseGet()`   |
| 默认值创建成本较高             | `orElseGet()`   |
| 默认值依赖数据库或外部接口     | `orElseGet()`   |
| 空值应视为业务异常             | `orElseThrow()` |

结合 Hutool 处理字符串默认值时，可以先用 `filter` 过滤空白字符串，再使用默认值。

```java
String displayName = Optional.ofNullable(user.getNickname())
        .filter(cn.hutool.core.util.StrUtil::isNotBlank)
        .orElse(user.getUsername());
```

这种写法不仅能处理 `null`，也能处理空字符串和空白字符串，更符合实际业务中的字段展示逻辑。

## 常用 API 使用

本节介绍 `Optional` 在项目中最常用的几个 API，包括 `map`、`flatMap`、`filter`、`orElse`、`orElseGet` 和 `orElseThrow`。这些方法是 `Optional` 实现链式空值处理的核心。

### map 方法

`map` 方法用于在值存在时对值进行转换。如果 `Optional` 为空，则不会执行转换逻辑，最终仍然返回空的 `Optional`。

常见场景是从对象中提取某个字段。

```java
Optional<User> optionalUser = userService.findById(userId);

Optional<String> username = optionalUser.map(User::getUsername);
```

如果需要在用户存在时获取用户名，用户不存在时返回默认值，可以写成：

```java
String username = userService.findById(userId)
        .map(User::getUsername)
        .orElse("未知用户");
```

`map` 也适合用于 DTO 或 VO 转换。

```java
UserVO userVO = userService.findById(userId)
        .map(user -> {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            return vo;
        })
        .orElseThrow(() -> new ServiceException("用户不存在"));
```

如果转换逻辑比较简单，可以直接使用方法引用。

```java
UserVO userVO = userService.findById(userId)
        .map(UserConvert::toVO)
        .orElseThrow(() -> new ServiceException("用户不存在"));
```

`map` 的特点是：输入一个普通值，输出一个转换后的普通值，最终由 `Optional` 自动包装转换结果。

可以理解为：

```java
Optional<User> -> map(User::getUsername) -> Optional<String>
```

需要注意，如果 `map` 中的转换结果为 `null`，最终会得到 `Optional.empty()`，不会得到包含 `null` 的 `Optional`。

```java
Optional<String> nickname = userService.findById(userId)
        .map(User::getNickname);
```

如果 `User` 存在但 `nickname` 为 `null`，那么 `nickname` 的结果是 `Optional.empty()`。

### flatMap 方法

`flatMap` 方法也用于转换，但它要求转换函数本身返回 `Optional`。它的作用是避免出现 `Optional<Optional<T>>` 这种嵌套结构。

假设用户对象中有一个获取邮箱的方法，返回值已经是 `Optional<String>`。

```java
public Optional<String> getEmail() {
    return Optional.ofNullable(this.email);
}
```

如果使用 `map`，结果会变成嵌套的 `Optional`。

```java
Optional<Optional<String>> email = userService.findById(userId)
        .map(User::getEmail);
```

这种结果使用起来不方便。此时应使用 `flatMap`。

```java
Optional<String> email = userService.findById(userId)
        .flatMap(User::getEmail);
```

`flatMap` 会把内部的 `Optional` 展平，最终得到单层结构。

```java
String email = userService.findById(userId)
        .flatMap(User::getEmail)
        .orElse("");
```

在实际项目中，`flatMap` 常用于多个返回值都是 `Optional` 的方法连续调用。

```java
String cityName = userService.findById(userId)
        .flatMap(userService::findAddressByUser)
        .map(Address::getCityName)
        .orElse("未知城市");
```

可以理解为：

```java
Optional<User> -> flatMap(findAddressByUser) -> Optional<Address> -> map(Address::getCityName) -> Optional<String>
```

`map` 和 `flatMap` 的区别如下：

| 方法      | 转换函数返回值 | 最终结果      | 适用场景           |
| --------- | -------------- | ------------- | ------------------ |
| `map`     | 普通对象       | `Optional<R>` | 字段提取、对象转换 |
| `flatMap` | `Optional<R>`  | `Optional<R>` | 避免 Optional 嵌套 |

简单判断原则是：如果转换方法返回普通对象，用 `map`；如果转换方法本身已经返回 `Optional`，用 `flatMap`。

### filter 方法

`filter` 方法用于在值存在时进行条件过滤。如果值不存在，直接返回空 `Optional`；如果值存在但不满足条件，也返回空 `Optional`；如果满足条件，则保留原值。

例如，只有用户状态为启用时才继续处理。

```java
User user = userService.findById(userId)
        .filter(item -> Objects.equals(item.getStatus(), 1))
        .orElseThrow(() -> new ServiceException("用户不存在或已被禁用"));
```

`filter` 也适合处理字符串有效性判断。结合 Hutool 可以同时排除 `null`、空字符串和空白字符串。

```java
String nickname = Optional.ofNullable(user.getNickname())
        .filter(cn.hutool.core.util.StrUtil::isNotBlank)
        .orElse(user.getUsername());
```

如果业务中需要判断某个字段满足条件后再转换，可以将 `filter` 和 `map` 组合使用。

```java
String mobile = userService.findById(userId)
        .filter(user -> Objects.equals(user.getStatus(), 1))
        .map(User::getMobile)
        .filter(cn.hutool.core.util.StrUtil::isNotBlank)
        .orElseThrow(() -> new ServiceException("用户手机号不存在或用户状态不可用"));
```

这段代码表达的业务含义是：先查询用户，再判断用户是否启用，然后获取手机号，最后判断手机号是否有效。如果任意一步不满足条件，则抛出业务异常。

`filter` 不适合承载复杂业务逻辑。如果过滤条件中需要写大量判断、日志、状态修改或外部接口调用，应改为普通 `if` 语句，避免链式代码难以阅读和调试。

### orElse 与 orElseGet

`orElse` 和 `orElseGet` 都用于在 `Optional` 为空时提供默认值。它们的区别在于默认值的执行时机。

`orElse` 接收一个已经准备好的默认值。

```java
String nickname = Optional.ofNullable(user.getNickname())
        .orElse("默认昵称");
```

`orElseGet` 接收一个 `Supplier`，只有当 `Optional` 为空时才会执行。

```java
String nickname = Optional.ofNullable(user.getNickname())
        .orElseGet(() -> buildDefaultNickname(user));
```

如果默认值是常量、枚举、简单对象，可以使用 `orElse`。

```java
Integer status = Optional.ofNullable(user.getStatus())
        .orElse(0);
```

如果默认值需要调用方法生成，应使用 `orElseGet`。

```java
User user = userService.findById(userId)
        .orElseGet(() -> createGuestUser(userId));
```

下面这种写法需要谨慎。

```java
User user = userService.findById(userId)
        .orElse(createGuestUser(userId));
```

即使 `findById(userId)` 查询到了用户，`createGuestUser(userId)` 仍然会被执行。如果 `createGuestUser` 中包含数据库写入、远程调用、日志记录或复杂计算，就可能带来额外开销，甚至造成业务副作用。

因此，项目中的选择原则是：

| 默认值类型         | 推荐方法    |
| ------------------ | ----------- |
| 固定字符串         | `orElse`    |
| 固定数字           | `orElse`    |
| 固定枚举           | `orElse`    |
| 简单默认对象       | `orElse`    |
| 方法计算结果       | `orElseGet` |
| 数据库查询结果     | `orElseGet` |
| 远程接口调用结果   | `orElseGet` |
| 可能有副作用的逻辑 | `orElseGet` |

### orElseThrow 方法

`orElseThrow` 用于在 `Optional` 为空时抛出异常，是项目开发中非常常用的空结果处理方式。

在 Service 层中，查询单个对象不存在时，通常可以使用 `orElseThrow` 抛出业务异常。

```java
User user = userService.findById(userId)
        .orElseThrow(() -> new ServiceException("用户不存在"));
```

如果异常消息需要包含业务参数，可以在异常构造逻辑中拼接。

```java
User user = userService.findById(userId)
        .orElseThrow(() -> new ServiceException("用户不存在，用户ID：" + userId));
```

结合 Hutool 可以格式化异常消息，使代码更统一。

```java
User user = userService.findById(userId)
        .orElseThrow(() -> new ServiceException(
                cn.hutool.core.util.StrUtil.format("用户不存在，用户ID：{}", userId)
        ));
```

`orElseThrow` 适合以下场景：

| 场景           | 示例                               |
| -------------- | ---------------------------------- |
| 查询数据不存在 | 用户不存在、订单不存在、配置不存在 |
| 前置条件不满足 | 当前用户未绑定手机号               |
| 业务状态不可用 | 用户已禁用、订单已关闭             |
| 必需字段缺失   | 关键配置为空、认证信息不存在       |

例如，查询启用状态的用户，不存在或状态不可用时直接抛出异常。

```java
User user = userService.findById(userId)
        .filter(item -> Objects.equals(item.getStatus(), 1))
        .orElseThrow(() -> new ServiceException(
                cn.hutool.core.util.StrUtil.format("用户不存在或状态不可用，用户ID：{}", userId)
        ));
```

`orElseThrow` 也可以和 `map` 配合使用，用于获取必需字段。

```java
String mobile = userService.findById(userId)
        .map(User::getMobile)
        .filter(cn.hutool.core.util.StrUtil::isNotBlank)
        .orElseThrow(() -> new ServiceException(
                cn.hutool.core.util.StrUtil.format("用户手机号不存在，用户ID：{}", userId)
        ));
```

在 Java 10 之后，`orElseThrow()` 也提供了无参形式。如果 `Optional` 为空，会抛出 `NoSuchElementException`。

```java
User user = userService.findById(userId)
        .orElseThrow();
```

不过在业务项目中，不推荐使用无参 `orElseThrow()`，因为它抛出的异常信息不够明确。更推荐抛出项目中的业务异常，便于接口返回、日志排查和问题定位。

```java
User user = userService.findById(userId)
        .orElseThrow(() -> new ServiceException("用户不存在"));
```

总体来说，`orElseThrow` 是 `Optional` 在 Service 层中最实用的方法之一。它可以把“查询不到数据”从空指针风险转换为明确的业务异常，使代码语义更清晰，也更符合后端接口开发的异常处理习惯。



## 项目开发实践

本节从实际 Java 项目分层角度说明 `Optional` 的使用方式。项目中不建议到处使用 `Optional`，而应根据 Controller、Service、Repository、DTO 转换等不同层的职责进行区分。

### Service 层中的 Optional 使用

Service 层是 `Optional` 最常见的使用位置。通常可以将“可能查不到数据”的内部查询方法设计为返回 `Optional<T>`，然后在对外业务方法中根据业务语义转换为默认值、业务异常或响应对象。

推荐做法是：内部查询方法可以返回 `Optional`，对外业务方法不一定返回 `Optional`。例如，`findOptionalById` 表达“查询结果可能为空”，而 `getDetail` 表达“必须查到用户，否则抛出业务异常”。

文件位置：`src/main/java/io/github/atengk/optional/service/UserService.java`

下面的接口用于区分可选查询方法和明确业务返回方法。

```java
package io.github.atengk.optional.service;

import io.github.atengk.optional.entity.User;
import io.github.atengk.optional.vo.UserDetailVO;

import java.util.Optional;

/**
 * 用户服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserService {

    Optional<User> findOptionalById(Long userId);

    UserDetailVO getDetail(Long userId);

}
```

文件位置：`src/main/java/io/github/atengk/optional/service/impl/UserServiceImpl.java`

下面的实现类演示了 Service 层如何使用 `Optional` 处理查询结果、业务异常和字段转换。

```java
package io.github.atengk.optional.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.optional.convert.UserConvert;
import io.github.atengk.optional.entity.User;
import io.github.atengk.optional.exception.BusinessException;
import io.github.atengk.optional.mapper.UserMapper;
import io.github.atengk.optional.service.UserService;
import io.github.atengk.optional.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserConvert userConvert;

    @Override
    public Optional<User> findOptionalById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }

    @Override
    public UserDetailVO getDetail(Long userId) {
        User user = this.findOptionalById(userId)
                .orElseThrow(() -> {
                    log.info("用户不存在，用户ID：{}", userId);
                    return new BusinessException(StrUtil.format("用户不存在，用户ID：{}", userId));
                });

        return userConvert.toDetailVO(user);
    }

}
```

这种写法的优点是职责清晰。`findOptionalById` 只负责表达查询结果可能为空，`getDetail` 负责把空结果转换成业务异常。调用方不需要再写重复的 `null` 判断。

在 Service 层中，`Optional` 常用于以下几类方法：

| 方法类型             | 是否适合返回 Optional | 示例                                      |
| -------------------- | --------------------- | ----------------------------------------- |
| 内部查询方法         | 适合                  | `findOptionalById(Long id)`               |
| 根据唯一字段查询     | 适合                  | `findOptionalByUsername(String username)` |
| 获取业务详情         | 不一定适合            | 可直接返回 VO，不存在时抛异常             |
| 创建、更新、删除方法 | 不适合                | 通常返回结果对象、布尔值或无返回          |
| 批量查询方法         | 不适合                | 应返回集合，空结果返回空集合              |

### Repository 查询结果处理

Repository 或 Mapper 层负责和数据库交互。对于 MyBatis-Plus、JPA 等持久层框架来说，单条查询通常会返回对象或 `null`。项目中可以在 Service 层将查询结果包装为 `Optional`，也可以在 Repository 层提供语义更明确的方法。

如果使用 MyBatis-Plus，Mapper 通常保持简洁，不强制返回 `Optional`。

文件位置：`src/main/java/io/github/atengk/optional/mapper/UserMapper.java`

下面的 Mapper 保持 MyBatis-Plus 默认风格，由 Service 层负责包装查询结果。

```java
package io.github.atengk.optional.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.optional.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

在 Service 层中统一包装：

```java
public Optional<User> findOptionalById(Long userId) {
    return Optional.ofNullable(userMapper.selectById(userId));
}
```

如果项目中有独立的 Repository 层，也可以在 Repository 层直接返回 `Optional`，让数据访问语义更加明确。

文件位置：`src/main/java/io/github/atengk/optional/repository/UserRepository.java`

下面的 Repository 用于封装 Mapper 查询，并将可能为空的数据库结果转换为 `Optional`。

```java
package io.github.atengk.optional.repository;

import io.github.atengk.optional.entity.User;
import io.github.atengk.optional.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;

    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }

}
```

Repository 层使用 `Optional` 时，需要注意两个边界。

第一，单条结果可以使用 `Optional<T>`。例如根据 ID、手机号、用户名查询单个用户。

```java
public Optional<User> findByUsername(String username) {
    return Optional.ofNullable(userMapper.selectOne(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getUsername, username)
                    .last("limit 1")
    ));
}
```

第二，集合结果不建议使用 `Optional<List<T>>`。如果没有数据，应返回空集合。

```java
public List<User> listEnabledUsers() {
    return userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, 1)
    );
}
```

集合本身已经能表达“没有数据”，使用空集合比使用 `Optional<List<T>>` 更符合 Java 项目开发习惯。

### Controller 参数与响应处理

Controller 层不推荐把 `Optional` 暴露在请求参数或响应对象中。Controller 的职责是接收请求、校验参数、调用 Service，并返回明确的接口结果。

不推荐这样写：

```java
@GetMapping("/detail")
public Result<UserDetailVO> detail(Optional<Long> userId) {
    // 不推荐在 Controller 参数中使用 Optional
}
```

推荐直接使用普通参数，并通过参数校验或业务判断处理空值。

文件位置：`src/main/java/io/github/atengk/optional/controller/UserController.java`

下面的 Controller 使用普通请求参数接收用户 ID，由 Service 层处理数据不存在的业务异常。

```java
package io.github.atengk.optional.controller;

import io.github.atengk.optional.common.Result;
import io.github.atengk.optional.service.UserService;
import io.github.atengk.optional.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/detail")
    public Result<UserDetailVO> detail(@RequestParam Long userId) {
        UserDetailVO userDetailVO = userService.getDetail(userId);
        return Result.success(userDetailVO);
    }

}
```

如果参数是可选的，也不建议使用 `Optional` 参数，而是使用普通类型接收，然后在业务中判断。

```java
@GetMapping("/users/search")
public Result<List<UserDetailVO>> search(@RequestParam(required = false) String keyword) {
    List<UserDetailVO> records = userService.search(keyword);
    return Result.success(records);
}
```

Service 层可以结合 Hutool 处理可选参数。

```java
public List<UserDetailVO> search(String keyword) {
    LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();

    if (StrUtil.isNotBlank(keyword)) {
        queryWrapper.like(User::getUsername, keyword);
    }

    return userMapper.selectList(queryWrapper)
            .stream()
            .map(userConvert::toDetailVO)
            .toList();
}
```

Controller 层的使用原则是：参数保持普通类型，响应保持明确结构，`Optional` 不向接口调用方暴露。这样可以避免 JSON 序列化结构异常，也能让接口文档更清晰。

### DTO 转换中的 Optional 使用

DTO、VO 转换中可以适度使用 `Optional`，特别是处理字段默认值、空字符串过滤、多层对象取值时。但不建议在 DTO 字段类型中使用 `Optional`。

不推荐：

```java
public class UserDetailVO {
    private Optional<String> nickname;
}
```

推荐：

```java
public class UserDetailVO {
    private String nickname;
}
```

在转换逻辑中，可以使用 `Optional` 处理字段空值。

文件位置：`src/main/java/io/github/atengk/optional/convert/UserConvert.java`

下面的转换类演示了如何在 DTO 转换中使用 `Optional` 处理昵称、手机号和状态文案。

```java
package io.github.atengk.optional.convert;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.optional.entity.User;
import io.github.atengk.optional.vo.UserDetailVO;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * 用户转换器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class UserConvert {

    public UserDetailVO toDetailVO(User user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());

        String displayName = Optional.ofNullable(user.getNickname())
                .filter(StrUtil::isNotBlank)
                .orElse(user.getUsername());

        String mobile = Optional.ofNullable(user.getMobile())
                .filter(StrUtil::isNotBlank)
                .orElse("未绑定");

        String statusName = Optional.ofNullable(user.getStatus())
                .filter(status -> Objects.equals(status, 1))
                .map(status -> "启用")
                .orElse("禁用");

        vo.setDisplayName(displayName);
        vo.setMobile(mobile);
        vo.setStatusName(statusName);
        return vo;
    }

}
```

DTO 转换中使用 `Optional` 的重点是保持链式逻辑短小。如果一个字段的转换包含多个业务判断、远程查询、异常处理或日志记录，应拆成普通方法或使用 `if` 判断。

例如，下面这种写法不建议继续堆叠链式调用：

```java
String value = Optional.ofNullable(user)
        .map(User::getProfile)
        .map(UserProfile::getCompany)
        .map(Company::getAddress)
        .map(Address::getCity)
        .map(City::getName)
        .filter(StrUtil::isNotBlank)
        .orElse("未知城市");
```

如果链路过长，推荐拆成独立方法，提高可读性。

```java
private String getCityName(User user) {
    if (user == null || user.getProfile() == null) {
        return "未知城市";
    }

    Company company = user.getProfile().getCompany();
    if (company == null || company.getAddress() == null || company.getAddress().getCity() == null) {
        return "未知城市";
    }

    String cityName = company.getAddress().getCity().getName();
    return StrUtil.blankToDefault(cityName, "未知城市");
}
```

DTO 转换中可以使用 `Optional`，但不要让链式调用变成隐藏业务复杂度的工具。

## 推荐使用场景

本节总结 `Optional` 在项目开发中推荐使用的场景。总体原则是：当一个方法返回单个对象，并且该对象在业务上确实可能不存在时，适合使用 `Optional` 表达这种语义。

### 查询结果可能为空

查询结果可能为空是 `Optional` 最典型的使用场景。例如，根据用户 ID、订单号、手机号、用户名查询单条数据时，数据库中可能不存在对应记录。

推荐：

```java
public Optional<User> findOptionalById(Long userId) {
    return Optional.ofNullable(userMapper.selectById(userId));
}
```

调用方可以根据业务决定如何处理。

```java
User user = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException(StrUtil.format("用户不存在，用户ID：{}", userId)));
```

如果业务允许不存在，则可以使用默认值或跳过处理。

```java
String username = userService.findOptionalById(userId)
        .map(User::getUsername)
        .orElse("未知用户");
```

这种方式比返回 `null` 更清晰，因为方法签名已经告诉调用方：结果可能不存在，必须处理。

### 链式空值处理

当需要从对象中提取字段，并且中间结果可能为空时，可以使用 `map` 进行链式处理。

例如，获取用户昵称，昵称为空时使用用户名。

```java
String displayName = userService.findOptionalById(userId)
        .map(User::getNickname)
        .filter(StrUtil::isNotBlank)
        .orElse("未知用户");
```

如果需要从多层对象中取值，也可以使用 `map` 进行空值安全访问。

```java
String email = Optional.ofNullable(user)
        .map(User::getProfile)
        .map(UserProfile::getEmail)
        .filter(StrUtil::isNotBlank)
        .orElse("");
```

这类场景适合使用 `Optional`，因为它可以减少重复的 `if-null` 判断，让空值处理集中在一条清晰的调用链中。

但链式调用不应过长。通常建议一条 `Optional` 链控制在 3 到 5 个关键步骤以内。如果继续增加分支、日志或复杂计算，应拆成独立方法。

### 条件过滤与转换

`filter` 和 `map` 组合适合处理“满足条件后再转换”的场景。例如，只有启用状态的用户才能继续读取手机号。

```java
String mobile = userService.findOptionalById(userId)
        .filter(user -> Objects.equals(user.getStatus(), 1))
        .map(User::getMobile)
        .filter(StrUtil::isNotBlank)
        .orElseThrow(() -> new BusinessException(
                StrUtil.format("用户不存在、状态不可用或手机号为空，用户ID：{}", userId)
        ));
```

这段代码表达了完整的处理链路：

| 步骤                          | 说明                     |
| ----------------------------- | ------------------------ |
| `findOptionalById`            | 查询用户，结果可能为空   |
| `filter(status == 1)`         | 只保留启用状态的用户     |
| `map(User::getMobile)`        | 提取手机号               |
| `filter(StrUtil::isNotBlank)` | 排除空手机号             |
| `orElseThrow`                 | 不满足条件时抛出业务异常 |

适合使用 `filter` 的条件通常应比较简单，例如状态判断、字段非空判断、类型判断、枚举判断等。

不建议在 `filter` 中写复杂业务逻辑，例如数据库更新、外部接口调用、大段分支判断等。复杂逻辑应使用普通方法，并通过方法名表达业务语义。

```java
User user = userService.findOptionalById(userId)
        .filter(this::isAvailableUser)
        .orElseThrow(() -> new BusinessException("用户不可用"));
```

这样比在 `filter` 中写大段 lambda 更清晰。

### 业务异常抛出

`orElseThrow` 非常适合将空结果转换为业务异常。项目中常见的“用户不存在”“订单不存在”“配置不存在”等场景，都可以使用这种方式处理。

```java
User user = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException(StrUtil.format("用户不存在，用户ID：{}", userId)));
```

如果需要先过滤业务状态，再抛出异常，也可以结合 `filter`。

```java
User user = userService.findOptionalById(userId)
        .filter(item -> Objects.equals(item.getStatus(), 1))
        .orElseThrow(() -> new BusinessException(StrUtil.format("用户不存在或已禁用，用户ID：{}", userId)));
```

在业务异常场景中，建议始终使用带异常供应器的 `orElseThrow`，不要使用无参 `orElseThrow()`。

不推荐：

```java
User user = userService.findOptionalById(userId)
        .orElseThrow();
```

推荐：

```java
User user = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException(StrUtil.format("用户不存在，用户ID：{}", userId)));
```

原因是无参 `orElseThrow()` 抛出的异常通常是 `NoSuchElementException`，异常语义不适合直接作为业务异常返回给前端，也不利于日志排查。

## 不推荐使用场景

本节说明 `Optional` 在项目中不推荐使用的场景。`Optional` 的价值在于表达返回值可能为空，而不是替代所有空值判断。滥用 `Optional` 会造成代码复杂、框架适配困难和可读性下降。

### 实体类字段中使用 Optional

不推荐在 Entity、DTO、VO、BO 等数据对象字段中使用 `Optional`。这些对象通常需要参与 ORM 映射、JSON 序列化、参数绑定、对象复制和接口文档生成，字段使用 `Optional` 会增加框架处理复杂度。

不推荐：

```java
package io.github.atengk.optional.entity;

import java.util.Optional;

/**
 * 用户实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class User {

    private Long id;

    private String username;

    private Optional<String> nickname;

}
```

推荐：

```java
package io.github.atengk.optional.entity;

/**
 * 用户实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class User {

    private Long id;

    private String username;

    private String nickname;

}
```

字段是否为空，应由字段本身表达。需要处理空值时，在业务逻辑或转换逻辑中使用 `Optional` 即可。

```java
String displayName = Optional.ofNullable(user.getNickname())
        .filter(StrUtil::isNotBlank)
        .orElse(user.getUsername());
```

实体类字段使用 `Optional` 的主要问题包括：

| 问题            | 说明                                          |
| --------------- | --------------------------------------------- |
| ORM 映射复杂    | MyBatis、JPA 等框架通常不按 Optional 字段设计 |
| JSON 结构不友好 | 可能序列化出额外包装结构                      |
| Bean 规范不一致 | Getter、Setter、对象复制工具处理不自然        |
| 代码可读性下降  | 字段访问需要额外拆包                          |

### 方法参数中使用 Optional

不推荐将 `Optional` 作为方法参数。方法参数是否允许为空，应通过参数校验、方法重载、DTO 字段约束或业务规则表达。

不推荐：

```java
public void updateNickname(Long userId, Optional<String> nickname) {
    String finalNickname = nickname
            .filter(StrUtil::isNotBlank)
            .orElseThrow(() -> new BusinessException("昵称不能为空"));

    // 执行更新
}
```

推荐直接传入普通参数：

```java
public void updateNickname(Long userId, String nickname) {
    if (StrUtil.isBlank(nickname)) {
        throw new BusinessException("昵称不能为空");
    }

    // 执行更新
}
```

如果参数确实有不同业务语义，可以使用方法重载或请求 DTO 表达。

```java
public void clearNickname(Long userId) {
    // 清空昵称
}

public void updateNickname(Long userId, String nickname) {
    // 修改昵称
}
```

方法参数使用 `Optional` 会把空值处理责任转移给调用方，导致调用代码变得啰嗦。

```java
userService.updateNickname(userId, Optional.ofNullable(nickname));
```

这种写法没有明显收益，反而增加了调用成本。

### 集合类型外层使用 Optional

不推荐返回 `Optional<List<T>>`、`Optional<Set<T>>`、`Optional<Map<K, V>>`。集合本身已经可以通过空集合表达“没有数据”。

不推荐：

```java
public Optional<List<User>> listEnabledUsers() {
    List<User> users = userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, 1)
    );
    return Optional.ofNullable(users);
}
```

推荐：

```java
public List<User> listEnabledUsers() {
    List<User> users = userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, 1)
    );
    return Optional.ofNullable(users).orElseGet(Collections::emptyList);
}
```

如果使用 MyBatis-Plus，`selectList` 通常返回集合对象，没有数据时一般是空集合。即使某些方法可能返回 `null`，也应该在方法内部转换为空集合，而不是把 `Optional<List<T>>` 暴露给调用方。

调用方处理空集合更自然：

```java
List<User> users = userService.listEnabledUsers();

if (CollUtil.isEmpty(users)) {
    log.info("暂无启用用户");
    return;
}
```

这里可以结合 Hutool 的 `CollUtil` 判断集合是否为空。

```java
if (CollUtil.isEmpty(users)) {
    log.info("用户列表为空，跳过后续处理");
    return;
}
```

集合返回值的推荐规则如下：

| 返回类型         | 推荐写法                      |
| ---------------- | ----------------------------- |
| 单个对象可能为空 | `Optional<T>`                 |
| 多个对象可能为空 | `List<T>`，无数据返回空集合   |
| Map 结果可能为空 | `Map<K, V>`，无数据返回空 Map |
| 数组结果可能为空 | 返回空数组或空集合            |

### 过度链式调用

`Optional` 的链式调用可以提升简单空值处理的可读性，但过度链式调用会让代码难以理解、调试和维护。

不推荐把复杂业务逻辑全部塞进一条链中。

```java
String result = userService.findOptionalById(userId)
        .filter(user -> Objects.equals(user.getStatus(), 1))
        .map(User::getProfile)
        .map(UserProfile::getCompany)
        .map(Company::getAddress)
        .map(Address::getCity)
        .map(City::getName)
        .filter(StrUtil::isNotBlank)
        .map(String::trim)
        .map(String::toUpperCase)
        .orElse("UNKNOWN");
```

这段代码虽然可以运行，但阅读成本较高。一旦中间某一步需要加日志、异常、状态判断或外部调用，就很难维护。

推荐拆成语义明确的方法：

```java
public String getUserCityName(Long userId) {
    User user = userService.findOptionalById(userId)
            .filter(this::isEnabledUser)
            .orElseThrow(() -> new BusinessException(StrUtil.format("用户不存在或不可用，用户ID：{}", userId)));

    return resolveCityName(user);
}

private boolean isEnabledUser(User user) {
    return Objects.equals(user.getStatus(), 1);
}

private String resolveCityName(User user) {
    if (user.getProfile() == null) {
        return "UNKNOWN";
    }

    Company company = user.getProfile().getCompany();
    if (company == null || company.getAddress() == null || company.getAddress().getCity() == null) {
        return "UNKNOWN";
    }

    String cityName = company.getAddress().getCity().getName();
    return StrUtil.blankToDefault(cityName, "UNKNOWN").trim().toUpperCase();
}
```

判断是否过度使用 `Optional`，可以参考以下标准：

| 判断标准              | 建议                     |
| --------------------- | ------------------------ |
| 链式调用超过 5 步     | 考虑拆分方法             |
| lambda 中出现多行逻辑 | 考虑抽取方法             |
| 需要记录日志          | 优先使用普通分支         |
| 需要修改状态          | 不建议放在 Optional 链中 |
| 需要抛出多种异常      | 使用普通业务流程更清晰   |
| 需要调用外部接口      | 不建议隐藏在链式调用中   |

`Optional` 的目标是让空值处理更清晰，而不是让所有业务逻辑都变成函数式链式调用。项目中应优先保证可读性、可调试性和业务语义清楚。



## 与传统空值判断对比

本节对比传统 `if-null` 判断方式和 `Optional` 链式处理方式。两种方式没有绝对优劣，关键在于业务场景是否适合。简单空值转换、默认值处理和查询结果处理适合使用 `Optional`；复杂业务分支、日志记录、状态修改和多异常处理更适合使用传统 `if-null` 判断。

### if-null 判断方式

`if-null` 是 Java 项目中最直接的空值判断方式。它的优点是逻辑直观、调试方便、适合复杂业务流程。缺点是当空值判断层级较多时，代码容易变得冗长，空值处理逻辑也容易分散。

下面的代码演示传统 `if-null` 判断方式，适合包含多分支、日志记录和业务异常的场景。

```java
public UserDetailVO getUserDetail(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
        log.info("用户不存在，用户ID：{}", userId);
        throw new BusinessException(StrUtil.format("用户不存在，用户ID：{}", userId));
    }

    if (!Objects.equals(user.getStatus(), 1)) {
        log.info("用户状态不可用，用户ID：{}，状态：{}", userId, user.getStatus());
        throw new BusinessException(StrUtil.format("用户状态不可用，用户ID：{}", userId));
    }

    UserDetailVO vo = new UserDetailVO();
    vo.setId(user.getId());
    vo.setUsername(user.getUsername());

    if (StrUtil.isNotBlank(user.getNickname())) {
        vo.setDisplayName(user.getNickname());
    } else {
        vo.setDisplayName(user.getUsername());
    }

    return vo;
}
```

这种写法的业务流程很清楚：先查用户，再判断用户是否存在，然后判断状态，最后组装返回对象。对于需要记录日志、区分异常类型、处理多个业务分支的代码，`if-null` 判断通常更容易维护。

`if-null` 判断适合以下场景：

| 场景             | 说明                         |
| ---------------- | ---------------------------- |
| 业务分支较多     | 多个条件分别对应不同处理逻辑 |
| 需要记录日志     | 每个异常分支需要输出不同日志 |
| 需要修改状态     | 中间过程涉及数据变更         |
| 需要调用外部接口 | 不建议隐藏在链式调用中       |
| 需要抛出多种异常 | 每种异常需要明确上下文       |

在复杂业务逻辑中，不要为了使用 `Optional` 而强行改造 `if-null`。可读性和可维护性优先于写法形式。

### Optional 链式处理方式

`Optional` 链式处理方式更适合简单的空值转换、字段提取、条件过滤和默认值处理。它可以把“查询、过滤、转换、异常处理”放在一条链路中表达，减少重复的 `null` 判断。

下面的代码演示使用 `Optional` 完成查询结果处理、状态过滤和对象转换。

```java
public UserDetailVO getUserDetail(Long userId) {
    return Optional.ofNullable(userMapper.selectById(userId))
            .filter(user -> Objects.equals(user.getStatus(), 1))
            .map(user -> {
                UserDetailVO vo = new UserDetailVO();
                vo.setId(user.getId());
                vo.setUsername(user.getUsername());

                String displayName = Optional.ofNullable(user.getNickname())
                        .filter(StrUtil::isNotBlank)
                        .orElse(user.getUsername());

                vo.setDisplayName(displayName);
                return vo;
            })
            .orElseThrow(() -> new BusinessException(
                    StrUtil.format("用户不存在或状态不可用，用户ID：{}", userId)
            ));
}
```

这段代码适合逻辑比较短的场景。它表达的流程是：查询用户，过滤启用状态，转换为 VO，如果中间任意一步不满足条件则抛出业务异常。

`Optional` 链式处理的典型优势是字段提取和默认值处理。例如，获取用户展示名称时，昵称存在且不为空则使用昵称，否则使用用户名。

```java
String displayName = Optional.ofNullable(user.getNickname())
        .filter(StrUtil::isNotBlank)
        .orElse(user.getUsername());
```

多层对象取值时，`Optional` 也能减少重复判断。

```java
String email = Optional.ofNullable(user)
        .map(User::getProfile)
        .map(UserProfile::getEmail)
        .filter(StrUtil::isNotBlank)
        .orElse("");
```

如果使用传统 `if-null` 写法，则需要显式判断每一层对象。

```java
String email = "";
if (user != null && user.getProfile() != null && StrUtil.isNotBlank(user.getProfile().getEmail())) {
    email = user.getProfile().getEmail();
}
```

`Optional` 链式处理适合以下场景：

| 场景             | 说明                                    |
| ---------------- | --------------------------------------- |
| 查询结果可能为空 | 使用 `Optional.ofNullable` 包装查询结果 |
| 简单字段提取     | 使用 `map` 提取字段                     |
| 简单条件过滤     | 使用 `filter` 判断状态、非空、枚举值    |
| 默认值处理       | 使用 `orElse` 或 `orElseGet`            |
| 空结果转异常     | 使用 `orElseThrow`                      |

需要注意的是，`Optional` 链式调用不适合承载复杂业务流程。如果链路过长，或者 lambda 中出现大量逻辑，应拆成独立方法。

### 可读性与维护性对比

`if-null` 和 `Optional` 的选择，本质上是可读性和维护性的取舍。`Optional` 可以让简单空值处理更紧凑，但复杂业务逻辑使用 `if-null` 更清楚。

| 对比项       | if-null 判断       | Optional 链式处理              |
| ------------ | ------------------ | ------------------------------ |
| 代码直观性   | 高，流程清晰       | 简单场景清晰，复杂场景可能难读 |
| 空值语义     | 依赖开发者主动判断 | 方法返回值可直接表达可能为空   |
| 调试便利性   | 方便逐行调试       | 链式调用调试相对不直观         |
| 默认值处理   | 需要手动分支       | `orElse`、`orElseGet` 更简洁   |
| 异常处理     | 适合多异常分支     | 适合统一空结果异常             |
| 复杂业务流程 | 更适合             | 不适合过度承载                 |
| 字段转换     | 代码略冗长         | 更简洁                         |
| 多层对象取值 | 判断较多           | 链式访问更清晰                 |

推荐选择规则如下：

| 场景                       | 推荐方式                 |
| -------------------------- | ------------------------ |
| 单个查询结果可能为空       | `Optional`               |
| 查询不到数据直接抛业务异常 | `Optional.orElseThrow`   |
| 字段默认值处理             | `Optional`               |
| 多层对象简单取值           | `Optional.map`           |
| 多个业务分支               | `if-null`                |
| 需要多处日志记录           | `if-null`                |
| 需要修改对象状态           | `if-null`                |
| 需要调用外部接口           | `if-null` 或拆分方法     |
| 链式调用超过 5 步          | 拆分方法或使用 `if-null` |

在团队规范中，可以采用以下原则：`Optional` 用于表达“返回值可能不存在”，`if-null` 用于处理“复杂业务流程”。不要为了统一形式强行替换，也不要在所有场景都坚持传统空值判断。

## 常见问题

本节整理项目开发中使用 `Optional` 时最容易出现的问题，包括 `get()` 方法误用、`orElse` 默认值提前执行、`Optional` 嵌套和空集合返回值选择。

### get 方法误用

`get()` 是 `Optional` 中最容易被误用的方法。当 `Optional` 为空时，调用 `get()` 会抛出 `NoSuchElementException`。如果没有提前判断，`get()` 和直接访问 `null` 对象一样存在运行时风险。

不推荐直接调用 `get()`：

```java
Optional<User> optionalUser = userService.findOptionalById(userId);
User user = optionalUser.get();
```

如果 `optionalUser` 为空，上面的代码会直接抛出异常，而且异常信息通常不包含明确的业务上下文。

有些代码会先判断 `isPresent()`，再调用 `get()`。

```java
Optional<User> optionalUser = userService.findOptionalById(userId);

if (optionalUser.isPresent()) {
    User user = optionalUser.get();
    return userConvert.toDetailVO(user);
}

throw new BusinessException("用户不存在");
```

这种写法可以避免 `NoSuchElementException`，但代码比较啰嗦。如果只是为了“有值则返回，无值则抛异常”，推荐使用 `orElseThrow()`。

```java
User user = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException(
                StrUtil.format("用户不存在，用户ID：{}", userId)
        ));

return userConvert.toDetailVO(user);
```

如果只是为了“有值则转换，无值则返回默认值”，推荐使用 `map` 和 `orElse`。

```java
String username = userService.findOptionalById(userId)
        .map(User::getUsername)
        .orElse("未知用户");
```

如果只是为了“有值才执行某段逻辑”，推荐使用 `ifPresent()`。

```java
userService.findOptionalById(userId)
        .ifPresent(user -> log.info("查询到用户，用户ID：{}，用户名：{}", user.getId(), user.getUsername()));
```

`get()` 并不是完全不能使用，但只应在非常确定 `Optional` 一定有值的局部场景中使用。业务项目中更推荐使用 `orElseThrow()`、`orElse()`、`orElseGet()` 和 `ifPresent()`。

### orElse 性能问题

`orElse()` 和 `orElseGet()` 都可以提供默认值，但它们的执行时机不同。`orElse()` 中的默认值会提前计算，无论 `Optional` 中是否存在值，默认值表达式都会执行。

下面的代码存在潜在性能问题。

```java
User user = userService.findOptionalById(userId)
        .orElse(createDefaultUser(userId));
```

即使 `findOptionalById(userId)` 查询到了用户，`createDefaultUser(userId)` 仍然会被执行。如果这个方法只是创建一个简单对象，影响可能不明显；但如果它包含数据库查询、远程接口调用、文件读取或复杂计算，就可能造成额外开销。

推荐使用 `orElseGet()` 延迟执行默认值逻辑。

```java
User user = userService.findOptionalById(userId)
        .orElseGet(() -> createDefaultUser(userId));
```

下面的代码更能体现差异。

```java
public User createDefaultUser(Long userId) {
    log.info("开始创建默认用户，用户ID：{}", userId);

    User user = new User();
    user.setId(userId);
    user.setUsername("默认用户");
    return user;
}
```

如果使用 `orElse(createDefaultUser(userId))`，只要代码执行到这一行，日志就会输出。
如果使用 `orElseGet(() -> createDefaultUser(userId))`，只有当 `Optional` 为空时才会输出日志。

选择规则如下：

| 场景                       | 推荐方法    |
| -------------------------- | ----------- |
| 默认值是固定字符串         | `orElse`    |
| 默认值是固定数字           | `orElse`    |
| 默认值是固定枚举           | `orElse`    |
| 默认值是已经存在的简单对象 | `orElse`    |
| 默认值需要调用方法生成     | `orElseGet` |
| 默认值涉及数据库查询       | `orElseGet` |
| 默认值涉及远程接口调用     | `orElseGet` |
| 默认值逻辑可能有副作用     | `orElseGet` |

项目开发中可以简单记忆：默认值已经存在，用 `orElse`；默认值需要计算，用 `orElseGet`。

### Optional 嵌套问题

`Optional` 嵌套通常是误用 `map` 导致的。当 `map` 中调用的方法本身已经返回 `Optional` 时，结果会变成 `Optional<Optional<T>>`。

假设用户对象中有一个方法返回可选邮箱：

```java
public Optional<String> getEmailOptional() {
    return Optional.ofNullable(this.email);
}
```

如果使用 `map` 调用这个方法，会得到嵌套结构。

```java
Optional<Optional<String>> email = userService.findOptionalById(userId)
        .map(User::getEmailOptional);
```

这种结果使用起来很不方便，调用方还需要拆两层 `Optional`。

正确做法是使用 `flatMap()`。

```java
Optional<String> email = userService.findOptionalById(userId)
        .flatMap(User::getEmailOptional);
```

如果最终需要字符串结果，可以继续接 `orElse()`。

```java
String email = userService.findOptionalById(userId)
        .flatMap(User::getEmailOptional)
        .filter(StrUtil::isNotBlank)
        .orElse("");
```

`map` 和 `flatMap` 的选择规则如下：

| 方法      | 转换函数返回值  | 示例                              |
| --------- | --------------- | --------------------------------- |
| `map`     | 普通对象        | `map(User::getUsername)`          |
| `flatMap` | `Optional` 对象 | `flatMap(User::getEmailOptional)` |

简单理解：方法返回普通值，用 `map`；方法返回 `Optional`，用 `flatMap`。

项目中也要避免把方法设计成多层 `Optional`。例如下面这种返回值不推荐：

```java
public Optional<Optional<User>> findUser(Long userId) {
    return Optional.of(userRepository.findById(userId));
}
```

推荐保持单层 `Optional`：

```java
public Optional<User> findUser(Long userId) {
    return userRepository.findById(userId);
}
```

`Optional` 嵌套会降低代码可读性，也会让调用方不清楚每一层空值分别代表什么含义。

### 空集合与 Optional 的选择

集合类型不推荐使用 `Optional` 包装。对于 `List`、`Set`、`Map` 等集合，空集合已经可以表达“没有数据”，不需要再用 `Optional` 表达一层“不存在”。

不推荐：

```java
public Optional<List<User>> listEnabledUsers() {
    List<User> users = userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, 1)
    );
    return Optional.ofNullable(users);
}
```

推荐：

```java
public List<User> listEnabledUsers() {
    List<User> users = userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, 1)
    );

    return Optional.ofNullable(users).orElseGet(Collections::emptyList);
}
```

调用方可以直接判断集合是否为空。

```java
List<User> users = userService.listEnabledUsers();

if (CollUtil.isEmpty(users)) {
    log.info("启用用户列表为空");
    return;
}
```

如果返回 `Optional<List<User>>`，调用方处理会变得复杂。

```java
Optional<List<User>> optionalUsers = userService.listEnabledUsers();

if (optionalUsers.isPresent() && CollUtil.isNotEmpty(optionalUsers.get())) {
    // 处理用户列表
}
```

这种写法没有必要。集合为空和集合不存在在大多数业务场景中可以统一处理为“没有数据”。

集合返回值推荐规则如下：

| 数据类型         | 推荐返回值        |
| ---------------- | ----------------- |
| 查询单个用户     | `Optional<User>`  |
| 查询用户列表     | `List<User>`      |
| 查询用户 ID 集合 | `Set<Long>`       |
| 查询用户映射关系 | `Map<Long, User>` |
| 没有列表数据     | 空集合            |
| 没有 Map 数据    | 空 Map            |

如果需要保证返回值不是 `null`，可以在方法内部转换为空集合。

```java
public List<User> listUsersByStatus(Integer status) {
    List<User> users = userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, status)
    );

    return Optional.ofNullable(users).orElseGet(Collections::emptyList);
}
```

也可以结合 Hutool 判断集合后再处理。

```java
List<User> users = userService.listUsersByStatus(1);

if (CollUtil.isEmpty(users)) {
    log.info("用户列表为空，状态：{}", 1);
    return;
}

users.forEach(user -> log.info("处理用户，用户ID：{}，用户名：{}", user.getId(), user.getUsername()));
```

总体原则是：单个对象可能不存在，可以使用 `Optional<T>`；多个对象没有数据，应返回空集合，不建议使用 `Optional<Collection<T>>`。



## 编码规范

本节用于约束项目中 `Optional` 的使用方式。`Optional` 本身并不复杂，但如果缺少统一规范，容易出现实体字段使用 `Optional`、方法参数使用 `Optional`、过度链式调用、滥用 `get()` 等问题。团队中应明确 `Optional` 的使用边界，让代码风格保持一致。

### 方法返回值规范

`Optional` 最推荐的使用位置是方法返回值，尤其是返回单个对象且结果可能不存在的查询方法。方法签名使用 `Optional<T>` 后，调用方可以直接感知该方法存在空结果风险。

推荐用于单个对象查询：

```java
public Optional<User> findOptionalById(Long userId) {
    return Optional.ofNullable(userMapper.selectById(userId));
}
```

推荐用于根据唯一字段查询：

```java
public Optional<User> findOptionalByUsername(String username) {
    if (StrUtil.isBlank(username)) {
        return Optional.empty();
    }

    return Optional.ofNullable(userMapper.selectOne(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getUsername, username)
                    .last("limit 1")
    ));
}
```

不推荐用于集合返回值。集合查询没有数据时，应返回空集合，而不是返回 `Optional<List<T>>`。

不推荐：

```java
public Optional<List<User>> listEnabledUsers() {
    List<User> users = userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, 1)
    );
    return Optional.ofNullable(users);
}
```

推荐：

```java
public List<User> listEnabledUsers() {
    List<User> users = userMapper.selectList(
            Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, 1)
    );
    return Optional.ofNullable(users).orElseGet(Collections::emptyList);
}
```

不推荐用于新增、修改、删除等命令式方法。命令式方法通常应该返回明确的业务结果，例如 `Boolean`、影响行数、详情对象或直接无返回值。

不推荐：

```java
public Optional<User> updateUser(UserUpdateDTO updateDTO) {
    // 不推荐使用 Optional 表达更新结果
}
```

推荐：

```java
public UserDetailVO updateUser(UserUpdateDTO updateDTO) {
    // 修改成功后返回明确的详情对象
}
```

方法返回值使用规则可以统一为：

| 方法类型                 | 推荐返回值                   | 说明                   |
| ------------------------ | ---------------------------- | ---------------------- |
| 根据 ID 查询单个对象     | `Optional<T>`                | 结果可能不存在         |
| 根据唯一字段查询单个对象 | `Optional<T>`                | 结果可能不存在         |
| 查询列表                 | `List<T>`                    | 无数据返回空集合       |
| 查询 Map                 | `Map<K, V>`                  | 无数据返回空 Map       |
| 新增数据                 | `T`、`Boolean`、`Long`       | 返回新增结果或主键     |
| 修改数据                 | `T`、`Boolean`、`Integer`    | 返回修改结果或影响行数 |
| 删除数据                 | `Boolean`、`Integer`、`void` | 返回删除结果或无返回   |
| 业务详情接口             | `VO`                         | 不存在时通常抛业务异常 |

命名上，返回 `Optional` 的方法建议体现“可能为空”的语义，例如 `findOptionalById`、`findByUsername`、`getOptionalConfig`。如果团队希望更明确，可以统一使用 `findOptionalXxx` 命名。

```java
Optional<User> findOptionalById(Long userId);

Optional<User> findOptionalByUsername(String username);

Optional<SystemConfig> findOptionalConfig(String configKey);
```

### 异常处理规范

`Optional` 常用于将空结果转换为业务异常。项目中应优先使用带异常供应器的 `orElseThrow`，并抛出项目统一的业务异常类型。

推荐：

```java
User user = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException(
                StrUtil.format("用户不存在，用户ID：{}", userId)
        ));
```

不推荐使用无参 `orElseThrow()`：

```java
User user = userService.findOptionalById(userId)
        .orElseThrow();
```

无参 `orElseThrow()` 抛出的是 `NoSuchElementException`，异常语义不明确，不适合作为业务接口异常直接返回给前端。

也不推荐先 `isPresent()` 再 `get()`，然后手动抛异常：

```java
Optional<User> optionalUser = userService.findOptionalById(userId);

if (optionalUser.isPresent()) {
    return optionalUser.get();
}

throw new BusinessException("用户不存在");
```

推荐直接使用 `orElseThrow()`：

```java
User user = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException("用户不存在"));
```

如果空结果前还需要进行业务状态过滤，可以结合 `filter` 使用。

```java
User user = userService.findOptionalById(userId)
        .filter(item -> Objects.equals(item.getStatus(), 1))
        .orElseThrow(() -> new BusinessException(
                StrUtil.format("用户不存在或已禁用，用户ID：{}", userId)
        ));
```

当需要区分不同异常原因时，不建议把所有条件都塞进一条 `Optional` 链中。此时使用普通 `if` 判断更清晰。

```java
User user = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException(
                StrUtil.format("用户不存在，用户ID：{}", userId)
        ));

if (!Objects.equals(user.getStatus(), 1)) {
    log.info("用户状态不可用，用户ID：{}，状态：{}", userId, user.getStatus());
    throw new BusinessException("用户状态不可用");
}
```

异常处理规范建议如下：

| 场景                 | 推荐方式                                        |
| -------------------- | ----------------------------------------------- |
| 空结果统一抛业务异常 | `orElseThrow(() -> new BusinessException(...))` |
| 空结果使用默认值     | `orElse` 或 `orElseGet`                         |
| 多个异常原因需要区分 | 使用普通 `if` 判断                              |
| 需要记录详细日志     | 使用普通 `if` 判断或在异常前记录日志            |
| 不允许返回空结果     | 不使用 `get()`，使用 `orElseThrow`              |

### 命名与可读性规范

`Optional` 的命名应表达清楚“可选值”的语义。局部变量可以使用 `optionalXxx`，但不要在所有地方机械套用。对于返回 `Optional` 的方法，方法名可以使用 `find`、`findOptional` 等前缀，避免使用容易误解为必定存在的 `get`。

推荐：

```java
Optional<User> optionalUser = userService.findOptionalById(userId);
```

不推荐：

```java
Optional<User> user = userService.getById(userId);
```

如果方法名是 `getById`，调用方通常会认为一定能获取到对象；如果实际可能为空，建议命名为 `findById` 或 `findOptionalById`。

推荐命名：

```java
public Optional<User> findOptionalById(Long userId) {
    return Optional.ofNullable(userMapper.selectById(userId));
}
```

对于最终已经取出的对象，不应继续使用 `optional` 命名。

推荐：

```java
User user = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException("用户不存在"));
```

不推荐：

```java
User optionalUser = userService.findOptionalById(userId)
        .orElseThrow(() -> new BusinessException("用户不存在"));
```

链式调用应保持简短清晰。建议一条 `Optional` 链只处理一个明确目标，例如“取字段默认值”“过滤状态后返回对象”“空结果抛异常”。

推荐：

```java
String displayName = Optional.ofNullable(user.getNickname())
        .filter(StrUtil::isNotBlank)
        .orElse(user.getUsername());
```

不推荐把复杂业务流程全部写进一条链中：

```java
String result = userService.findOptionalById(userId)
        .filter(user -> Objects.equals(user.getStatus(), 1))
        .map(User::getProfile)
        .map(UserProfile::getCompany)
        .map(Company::getAddress)
        .map(Address::getCity)
        .map(City::getName)
        .filter(StrUtil::isNotBlank)
        .map(String::trim)
        .map(String::toUpperCase)
        .orElse("UNKNOWN");
```

可读性规范建议如下：

| 规范项                   | 建议                                      |
| ------------------------ | ----------------------------------------- |
| 返回 Optional 的查询方法 | 使用 `find` 或 `findOptional` 前缀        |
| 局部 Optional 变量       | 可命名为 `optionalUser`、`optionalConfig` |
| 已拆包对象               | 使用普通变量名，如 `user`、`config`       |
| 链式调用长度             | 控制在 3 到 5 个关键步骤以内              |
| lambda 逻辑              | 不写复杂多行逻辑，复杂逻辑抽方法          |
| 业务分支                 | 多分支场景优先使用 `if` 判断              |

### 团队使用约定

团队中使用 `Optional` 时，建议形成统一约定，避免不同开发者各写各的风格。约定的目标不是限制所有写法，而是让代码在项目内保持一致。

推荐团队约定如下：

| 约定项               | 规则                                   |
| -------------------- | -------------------------------------- |
| 单个查询结果可能为空 | 可以返回 `Optional<T>`                 |
| 集合查询结果为空     | 返回空集合，不返回 `Optional<List<T>>` |
| 实体类字段           | 不使用 `Optional`                      |
| DTO、VO 字段         | 不使用 `Optional`                      |
| Controller 参数      | 不使用 `Optional`                      |
| Service 内部查询方法 | 可以使用 `Optional`                    |
| 对外接口返回值       | 不直接返回 `Optional`                  |
| 空结果转异常         | 使用带异常信息的 `orElseThrow`         |
| 默认值计算成本高     | 使用 `orElseGet`                       |
| 禁止直接取值         | 避免直接使用 `get()`                   |

可以将团队规范写成更明确的代码风格要求：

```java
// 推荐：查询单个对象可能为空
Optional<User> findOptionalById(Long userId);

// 推荐：列表查询直接返回集合
List<User> listByStatus(Integer status);

// 不推荐：集合外层包装 Optional
Optional<List<User>> listByStatus(Integer status);

// 不推荐：参数中使用 Optional
void updateNickname(Long userId, Optional<String> nickname);

// 推荐：参数使用普通类型
void updateNickname(Long userId, String nickname);
```

对于代码评审，可以重点检查以下问题：

| 检查项                          | 判断标准                                          |
| ------------------------------- | ------------------------------------------------- |
| 是否直接调用 `get()`            | 一般要求改为 `orElseThrow`、`orElse`、`orElseGet` |
| 是否使用 `Optional<List<T>>`    | 要求改为空集合返回                                |
| 是否在实体字段中使用 `Optional` | 要求改为普通字段                                  |
| 是否在方法参数中使用 `Optional` | 要求改为普通参数或方法重载                        |
| 是否链式调用过长                | 要求拆分方法或改用普通分支                        |
| 是否使用 `orElse` 调用复杂方法  | 要求改为 `orElseGet`                              |

团队使用 `Optional` 的核心原则是：让空值语义更明确，让调用方处理更规范，而不是为了形式上消灭所有 `null`。

## 示例场景

本节通过常见业务场景说明 `Optional` 的落地方式，包括用户查询、配置读取、数据转换和业务校验。示例代码以 Spring Boot 项目中的 Service 层和转换逻辑为主。

### 用户查询场景

用户查询是 `Optional` 最常见的使用场景。根据用户 ID 查询数据时，数据库中可能不存在对应记录，因此可以先返回 `Optional<User>`，再由业务方法决定是否抛出异常。

文件位置：`src/main/java/io/github/atengk/optional/service/impl/UserQueryService.java`

下面的服务类演示了用户查询场景中 `Optional` 的典型用法，包括可选查询、详情查询和展示名称处理。

```java
package io.github.atengk.optional.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.optional.convert.UserConvert;
import io.github.atengk.optional.entity.User;
import io.github.atengk.optional.exception.BusinessException;
import io.github.atengk.optional.mapper.UserMapper;
import io.github.atengk.optional.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserMapper userMapper;
    private final UserConvert userConvert;

    public Optional<User> findOptionalById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }

    public UserDetailVO getUserDetail(Long userId) {
        User user = this.findOptionalById(userId)
                .orElseThrow(() -> {
                    log.info("用户不存在，用户ID：{}", userId);
                    return new BusinessException(StrUtil.format("用户不存在，用户ID：{}", userId));
                });

        return userConvert.toDetailVO(user);
    }

    public String getDisplayName(Long userId) {
        return this.findOptionalById(userId)
                .map(user -> Optional.ofNullable(user.getNickname())
                        .filter(StrUtil::isNotBlank)
                        .orElse(user.getUsername()))
                .orElse("未知用户");
    }

}
```

这个示例中，`findOptionalById` 负责表达查询结果可能为空；`getUserDetail` 负责把空结果转换为业务异常；`getDisplayName` 负责在用户不存在或昵称为空时提供默认展示名称。

调用方式示例：

```java
UserDetailVO detailVO = userQueryService.getUserDetail(10001L);

String displayName = userQueryService.getDisplayName(10001L);
```

用户查询场景中建议遵循以下规则：

| 场景               | 推荐写法                    |
| ------------------ | --------------------------- |
| 查询用户可能不存在 | `Optional<User>`            |
| 用户不存在要报错   | `orElseThrow`               |
| 用户不存在允许兜底 | `orElse` 或 `orElseGet`     |
| 字段为空使用默认值 | `map` + `filter` + `orElse` |

### 配置读取场景

配置读取也是适合使用 `Optional` 的场景。配置项可能不存在、可能为空字符串，也可能需要默认值。使用 `Optional` 可以让默认值处理更集中。

文件位置：`src/main/java/io/github/atengk/optional/service/SystemConfigService.java`

下面的服务类演示了如何读取系统配置，并通过 `Optional` 处理空配置、空白字符串和默认值。

```java
package io.github.atengk.optional.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 系统配置服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final Environment environment;

    public Optional<String> findOptionalProperty(String key) {
        if (StrUtil.isBlank(key)) {
            return Optional.empty();
        }

        return Optional.ofNullable(environment.getProperty(key))
                .filter(StrUtil::isNotBlank);
    }

    public String getPropertyOrDefault(String key, String defaultValue) {
        return this.findOptionalProperty(key)
                .orElse(defaultValue);
    }

    public Integer getIntPropertyOrDefault(String key, Integer defaultValue) {
        return this.findOptionalProperty(key)
                .map(Integer::valueOf)
                .orElse(defaultValue);
    }

}
```

配置示例：

```yaml
app:
  upload:
    path: /data/upload
    max-size: 10485760
```

调用方式示例：

```java
String uploadPath = systemConfigService.getPropertyOrDefault("app.upload.path", "/tmp/upload");

Integer maxSize = systemConfigService.getIntPropertyOrDefault("app.upload.max-size", 5242880);
```

如果配置缺失必须抛出异常，可以使用 `orElseThrow`。

```java
String uploadPath = systemConfigService.findOptionalProperty("app.upload.path")
        .orElseThrow(() -> new BusinessException("上传路径配置不存在"));
```

配置读取场景中，`Optional` 适合处理以下问题：

| 问题               | 处理方式                      |
| ------------------ | ----------------------------- |
| 配置不存在         | `Optional.empty()`            |
| 配置为空字符串     | `filter(StrUtil::isNotBlank)` |
| 配置有默认值       | `orElse(defaultValue)`        |
| 默认值需要动态生成 | `orElseGet(...)`              |
| 配置必须存在       | `orElseThrow(...)`            |

需要注意，如果配置转换可能失败，例如字符串转数字可能抛出 `NumberFormatException`，应结合异常处理或配置校验机制，不要把所有逻辑都塞进 `map` 中。

### 数据转换场景

数据转换中可以使用 `Optional` 处理字段默认值、空白字符串过滤和简单状态转换。常见场景是 Entity 转 VO、DTO 转 Entity 或接口响应组装。

文件位置：`src/main/java/io/github/atengk/optional/convert/UserConvert.java`

下面的转换类演示了如何在 Entity 转 VO 时使用 `Optional` 处理展示名称、手机号和状态文案。

```java
package io.github.atengk.optional.convert;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.optional.entity.User;
import io.github.atengk.optional.vo.UserDetailVO;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * 用户数据转换器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class UserConvert {

    public UserDetailVO toDetailVO(User user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());

        String displayName = Optional.ofNullable(user.getNickname())
                .filter(StrUtil::isNotBlank)
                .orElse(user.getUsername());

        String mobile = Optional.ofNullable(user.getMobile())
                .filter(StrUtil::isNotBlank)
                .map(item -> DesensitizedUtil.mobilePhone(item))
                .orElse("未绑定");

        String statusName = Optional.ofNullable(user.getStatus())
                .filter(status -> Objects.equals(status, 1))
                .map(status -> "启用")
                .orElse("禁用");

        vo.setDisplayName(displayName);
        vo.setMobile(mobile);
        vo.setStatusName(statusName);
        return vo;
    }

}
```

这个转换逻辑中，`Optional` 用在三个地方：

| 字段       | 处理逻辑                           |
| ---------- | ---------------------------------- |
| `nickname` | 昵称为空时使用用户名               |
| `mobile`   | 手机号不为空时脱敏，否则显示未绑定 |
| `status`   | 状态为 1 时显示启用，否则显示禁用  |

如果转换逻辑比较复杂，应抽取独立方法，避免一个转换方法中出现过长的 `Optional` 链。

```java
private String resolveDisplayName(User user) {
    return Optional.ofNullable(user.getNickname())
            .filter(StrUtil::isNotBlank)
            .orElse(user.getUsername());
}

private String resolveMobile(String mobile) {
    return Optional.ofNullable(mobile)
            .filter(StrUtil::isNotBlank)
            .map(DesensitizedUtil::mobilePhone)
            .orElse("未绑定");
}
```

数据转换场景中，`Optional` 的使用原则是：适合处理字段级别的简单空值逻辑，不适合承载复杂业务规则。复杂转换应拆分为独立方法或交给专门的转换组件处理。

### 业务校验场景

业务校验中可以使用 `Optional` 表达“存在并满足条件才继续处理”的语义。常见场景包括用户必须存在、用户状态必须可用、手机号必须已绑定、配置必须存在等。

下面的代码演示用户下单前的基础校验逻辑。

```java
public User validateUserForOrder(Long userId) {
    return userService.findOptionalById(userId)
            .filter(user -> Objects.equals(user.getStatus(), 1))
            .filter(user -> StrUtil.isNotBlank(user.getMobile()))
            .orElseThrow(() -> new BusinessException(
                    StrUtil.format("用户不存在、已禁用或未绑定手机号，用户ID：{}", userId)
            ));
}
```

这段代码适合用于统一错误提示的简单校验。如果需要区分不同错误原因，建议拆成普通判断。

```java
public User validateUserForOrder(Long userId) {
    User user = userService.findOptionalById(userId)
            .orElseThrow(() -> new BusinessException(
                    StrUtil.format("用户不存在，用户ID：{}", userId)
            ));

    if (!Objects.equals(user.getStatus(), 1)) {
        log.info("用户状态不可用，用户ID：{}，状态：{}", userId, user.getStatus());
        throw new BusinessException("用户状态不可用");
    }

    if (StrUtil.isBlank(user.getMobile())) {
        log.info("用户未绑定手机号，用户ID：{}", userId);
        throw new BusinessException("用户未绑定手机号");
    }

    return user;
}
```

两种写法的选择规则如下：

| 校验类型                   | 推荐方式                          |
| -------------------------- | --------------------------------- |
| 多个条件失败后提示相同错误 | `Optional.filter().orElseThrow()` |
| 不同条件需要不同错误提示   | 普通 `if` 判断                    |
| 需要记录详细日志           | 普通 `if` 判断                    |
| 条件判断简单               | `filter`                          |
| 条件判断复杂               | 抽取方法或使用 `if`               |

如果校验条件本身有明确业务含义，可以抽取方法后再配合 `filter` 使用。

```java
public User validateEnabledUser(Long userId) {
    return userService.findOptionalById(userId)
            .filter(this::isEnabledUser)
            .orElseThrow(() -> new BusinessException(
                    StrUtil.format("用户不存在或状态不可用，用户ID：{}", userId)
            ));
}

private boolean isEnabledUser(User user) {
    return Objects.equals(user.getStatus(), 1);
}
```

业务校验场景中，`Optional` 可以让简单校验更紧凑，但不要牺牲异常准确性和日志可观测性。只要不同校验失败原因需要分别提示，就应优先使用普通分支判断。
