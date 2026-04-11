# IntelliJ IDEA 配置

IntelliJ IDEA 常用配置与开发效率优化方案。

---

## 实时模版

实时模板（Live Templates）用于快速生成常用代码结构，建议按团队规范统一配置，避免重复手写样板代码。

---

### 日志模板（log）

输入 `log` + Tab 生成日志对象

```
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger($CLASS$.class);
```

变量：

- CLASS = className()

含义说明：

className() 表示 IntelliJ IDEA 内置的表达式，用于自动获取当前所在 Java 类的类名（不包含包名）。例如在 UserServiceImpl 类中使用该模板时，IDEA 会自动将 $CLASS$ 替换为 UserServiceImpl，从而生成：

```
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserServiceImpl.class);
```

该表达式的作用是避免手动填写类名，保证日志对象始终与当前类绑定，减少重构或复制代码时遗漏修改类名的问题。

![image-20260411115833072](./assets/image-20260411115833072.png)

---

### 生成类注释（zs）

输入 `zs` + Tab 快速生成 Java 类注释模板，用于统一类级说明与作者信息。

```id="kq8v2m"
 /**
 * $CLASS_COMMENT$
 *
 * @author Ateng
 * @since $DATE$
 */
```

变量：

* CLASS_COMMENT = completeSmart()
* DATE = date("yyyy-MM-dd")

含义说明：

CLASS_COMMENT 用于填写当前类的业务说明或功能描述，例如“用户服务实现类”、“订单查询控制器”等。该字段支持智能补全（completeSmart），可根据当前文件名或上下文自动推测，也允许手动修改。

DATE 使用 IDEA 内置日期函数 date("yyyy-MM-dd")，用于自动生成当前日期，确保类注释中的时间统一规范，无需手动维护。

该模板的作用是保证所有 Java 类具备统一结构的类级文档信息，提高代码可读性与维护一致性，特别适用于 Spring Boot 分层架构中的 Controller、Service、DAO 等核心层。

![image-20260411115850810](./assets/image-20260411115850810.png)

