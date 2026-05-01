# 设计模式：享元模式

享元模式用于共享大量细粒度对象，减少重复对象创建带来的内存开销。在 JDK21 和 Spring Boot 3 项目中，享元模式常用于字典项缓存、权限节点样式、商品标签样式、消息模板、文件图标、报表单元格样式、规则配置、枚举型处理器、连接池、线程池等场景。

需要注意：享元模式关注的是“共享可复用对象”。如果对象每次都有独立状态，不适合强行共享；如果只是缓存查询结果，不一定是享元模式；如果对象内部状态可以共享，外部变化数据通过方法参数传入，享元模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证享元模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、集合、金额等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、Getter、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于单元测试验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 3，建议使用 JDK17 及以上版本。当前文档以 JDK21 为基准，示例代码可以直接用于 Spring Boot 3 项目。

## 核心概念

享元模式的核心目标是把对象状态拆成内部状态和外部状态。内部状态可以共享，放在享元对象内部；外部状态不能共享，由调用方在使用时传入。

常见角色如下：

| 角色              | 说明                                           |
| ----------------- | ---------------------------------------------- |
| Flyweight         | 享元接口，定义共享对象的统一行为               |
| ConcreteFlyweight | 具体享元对象，保存可共享的内部状态             |
| FlyweightFactory  | 享元工厂，负责创建和缓存享元对象               |
| Intrinsic State   | 内部状态，可共享，例如样式编码、颜色、模板内容 |
| Extrinsic State   | 外部状态，不共享，例如用户ID、订单号、商品名称 |

典型结构如下：

```text
调用方
    -> FlyweightFactory
        -> 根据 key 获取共享对象
            -> Flyweight.operation(extrinsicState)
```

享元模式最关键的是分清内部状态和外部状态。

```text
内部状态：不随每次调用变化，可以被多个调用方共享。
外部状态：每次调用不同，不能放到共享对象成员变量中。
```

以商品标签样式为例：

```text
内部状态：标签类型、背景色、字体色、图标、固定前缀。
外部状态：商品ID、商品名称、库存数量、价格、操作人。
```

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 单例享元 / 工厂缓存享元 > 普通 Java 享元 > 每次 new 大量重复对象
```

享元模式适合对象数量大、对象内部大部分状态重复、共享对象不可变或接近不可变的场景。

## 普通 Java 享元模式

普通 Java 享元模式适合不依赖 Spring 容器的对象共享场景。下面以菜单图标渲染为例，系统中可能有大量菜单节点，但图标类型只有少数几种。图标编码、名称、颜色属于内部状态，可以共享；菜单编码、菜单名称属于外部状态，每次渲染时传入。

整体关系如下：

```text
MenuIconFactory
    -> 根据 iconCode 获取共享 MenuIcon
        -> MenuIcon.render(menuCode, menuName)
```

### 文件结构

```text
src/main/java/io/github/atengk/design/flyweight/simple/
├── MenuIcon.java
├── SharedMenuIcon.java
├── MenuIconFactory.java
└── MenuNode.java
```

文件位置：`src/main/java/io/github/atengk/design/flyweight/simple/MenuIcon.java`

下面是菜单图标享元接口。渲染时传入外部状态。

```java
package io.github.atengk.design.flyweight.simple;

/**
 * 菜单图标享元
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MenuIcon {

    /**
     * 渲染菜单图标
     *
     * @param menuCode 菜单编码
     * @param menuName 菜单名称
     * @return 渲染结果
     */
    String render(String menuCode, String menuName);

    /**
     * 获取图标编码
     *
     * @return 图标编码
     */
    String iconCode();
}
```

文件位置：`src/main/java/io/github/atengk/design/flyweight/simple/SharedMenuIcon.java`

下面是共享菜单图标对象。它保存图标编码、图标名称和颜色，这些都是可以共享的内部状态。

```java
package io.github.atengk.design.flyweight.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 共享菜单图标
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class SharedMenuIcon implements MenuIcon {

    private final String iconCode;
    private final String iconName;
    private final String color;

    /**
     * 创建共享菜单图标
     *
     * @param iconCode 图标编码
     * @param iconName 图标名称
     * @param color    图标颜色
     */
    public SharedMenuIcon(String iconCode, String iconName, String color) {
        if (StrUtil.hasBlank(iconCode, iconName, color)) {
            log.warn("创建共享菜单图标失败，图标编码、名称或颜色为空");
            throw new IllegalArgumentException("图标编码、名称和颜色不能为空");
        }

        this.iconCode = iconCode;
        this.iconName = iconName;
        this.color = color;

        log.info("创建共享菜单图标，图标编码：{}，图标名称：{}，颜色：{}", iconCode, iconName, color);
    }

    /**
     * 渲染菜单图标
     *
     * @param menuCode 菜单编码
     * @param menuName 菜单名称
     * @return 渲染结果
     */
    @Override
    public String render(String menuCode, String menuName) {
        if (StrUtil.hasBlank(menuCode, menuName)) {
            log.warn("渲染菜单图标失败，菜单编码或名称为空");
            throw new IllegalArgumentException("菜单编码和名称不能为空");
        }

        return StrUtil.format("菜单[{}-{}] 使用图标[{}-{}]，颜色：{}",
                menuCode, menuName, iconCode, iconName, color);
    }

    /**
     * 获取图标编码
     *
     * @return 图标编码
     */
    @Override
    public String iconCode() {
        return iconCode;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/flyweight/simple/MenuIconFactory.java`

下面是菜单图标享元工厂。它通过 `ConcurrentHashMap` 缓存图标对象，避免重复创建。

```java
package io.github.atengk.design.flyweight.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 菜单图标享元工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class MenuIconFactory {

    private final Map<String, MenuIcon> iconCache = new ConcurrentHashMap<>();

    /**
     * 获取菜单图标
     *
     * @param iconCode 图标编码
     * @return 菜单图标享元
     */
    public MenuIcon getIcon(String iconCode) {
        if (StrUtil.isBlank(iconCode)) {
            log.warn("获取菜单图标失败，图标编码为空");
            throw new IllegalArgumentException("图标编码不能为空");
        }

        String key = StrUtil.trim(iconCode).toLowerCase();
        return iconCache.computeIfAbsent(key, this::createIcon);
    }

    /**
     * 获取缓存数量
     *
     * @return 缓存数量
     */
    public int cacheSize() {
        return iconCache.size();
    }

    /**
     * 创建菜单图标
     *
     * @param iconCode 图标编码
     * @return 菜单图标享元
     */
    private MenuIcon createIcon(String iconCode) {
        log.info("菜单图标缓存未命中，开始创建图标，图标编码：{}", iconCode);

        return switch (iconCode) {
            case "system" -> new SharedMenuIcon("system", "系统图标", "#1677ff");
            case "user" -> new SharedMenuIcon("user", "用户图标", "#52c41a");
            case "order" -> new SharedMenuIcon("order", "订单图标", "#fa8c16");
            case "report" -> new SharedMenuIcon("report", "报表图标", "#722ed1");
            default -> new SharedMenuIcon(iconCode, "默认图标", "#8c8c8c");
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/flyweight/simple/MenuNode.java`

下面是菜单节点对象。它保存菜单自身数据，并引用共享图标对象。

```java
package io.github.atengk.design.flyweight.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 菜单节点
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
public class MenuNode {

    private final String menuCode;
    private final String menuName;
    private final MenuIcon menuIcon;

    /**
     * 创建菜单节点
     *
     * @param menuCode 菜单编码
     * @param menuName 菜单名称
     * @param menuIcon 菜单图标享元
     */
    public MenuNode(String menuCode, String menuName, MenuIcon menuIcon) {
        if (StrUtil.hasBlank(menuCode, menuName)) {
            throw new IllegalArgumentException("菜单编码和名称不能为空");
        }
        if (menuIcon == null) {
            throw new IllegalArgumentException("菜单图标不能为空");
        }

        this.menuCode = menuCode;
        this.menuName = menuName;
        this.menuIcon = menuIcon;
    }

    /**
     * 渲染菜单节点
     *
     * @return 渲染结果
     */
    public String render() {
        return menuIcon.render(menuCode, menuName);
    }
}
```

使用方式：

```java
MenuIconFactory iconFactory = new MenuIconFactory();

MenuNode userMenu = new MenuNode("system:user", "用户管理", iconFactory.getIcon("user"));
MenuNode roleMenu = new MenuNode("system:role", "角色管理", iconFactory.getIcon("user"));
MenuNode orderMenu = new MenuNode("order:list", "订单列表", iconFactory.getIcon("order"));

String userRenderText = userMenu.render();
String roleRenderText = roleMenu.render();
String orderRenderText = orderMenu.render();

int cacheSize = iconFactory.cacheSize();
```

这里 `userMenu` 和 `roleMenu` 使用的是同一个 `user` 图标享元对象。菜单编码和菜单名称不同，但图标编码、名称、颜色是共享的。

## Spring Boot 享元模式

Spring Boot 项目中，享元模式常用于共享样式、模板、规则、处理器等不可变对象。下面以商品标签渲染为例，商品数量可能很多，但标签样式只有少数几种。标签样式对象可以共享，商品信息通过上下文传入。

整体流程如下：

```text
Controller
    -> ProductLabelService
        -> LabelStyleFactory
            -> LabelStyle 共享样式
                -> render(LabelRenderContext)
```

示例支持三种标签样式：

```text
promotion     促销标签
stock_warning 库存预警标签
new_product   新品标签
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── FlyweightApplication.java
├── controller/
│   └── ProductLabelController.java
├── dto/
│   ├── LabelRenderContext.java
│   ├── LabelRenderRequest.java
│   └── LabelRenderResponse.java
├── flyweight/
│   ├── LabelStyle.java
│   ├── PromotionLabelStyle.java
│   ├── StockWarningLabelStyle.java
│   └── NewProductLabelStyle.java
├── factory/
│   └── LabelStyleFactory.java
└── service/
    ├── ProductLabelService.java
    └── impl/
        └── ProductLabelServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/FlyweightApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 享元模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class FlyweightApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FlyweightApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/LabelRenderRequest.java`

下面是商品标签渲染请求对象。它包含样式编码和商品外部状态。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 标签渲染请求
 *
 * @param styleCode   标签样式编码
 * @param productId   商品ID
 * @param productName 商品名称
 * @param price       商品价格
 * @param stock       商品库存
 * @author Ateng
 * @since 2026-04-30
 */
public record LabelRenderRequest(
        String styleCode,
        Long productId,
        String productName,
        BigDecimal price,
        Integer stock
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/LabelRenderContext.java`

下面是标签渲染上下文，也就是享元模式中的外部状态。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 标签渲染上下文
 *
 * @param productId   商品ID
 * @param productName 商品名称
 * @param price       商品价格
 * @param stock       商品库存
 * @author Ateng
 * @since 2026-04-30
 */
public record LabelRenderContext(
        Long productId,
        String productName,
        BigDecimal price,
        Integer stock
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/LabelRenderResponse.java`

下面是标签渲染响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 标签渲染响应
 *
 * @param styleCode   标签样式编码
 * @param productId   商品ID
 * @param productName 商品名称
 * @param labelText   标签文本
 * @param styleText   样式文本
 * @param cacheSize   当前享元缓存数量
 * @author Ateng
 * @since 2026-04-30
 */
public record LabelRenderResponse(
        String styleCode,
        Long productId,
        String productName,
        String labelText,
        String styleText,
        Integer cacheSize
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/flyweight/LabelStyle.java`

下面是标签样式享元接口。样式对象内部保存共享样式信息，渲染时接收外部上下文。

```java
package io.github.atengk.design.flyweight;

import io.github.atengk.design.dto.LabelRenderContext;

/**
 * 标签样式享元
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface LabelStyle {

    /**
     * 获取样式编码
     *
     * @return 样式编码
     */
    String styleCode();

    /**
     * 渲染标签文本
     *
     * @param context 标签渲染上下文
     * @return 标签文本
     */
    String renderLabel(LabelRenderContext context);

    /**
     * 获取样式描述
     *
     * @return 样式描述
     */
    String styleText();
}
```

文件位置：`src/main/java/io/github/atengk/design/flyweight/PromotionLabelStyle.java`

下面是促销标签样式。标签颜色、图标、前缀属于内部状态，可被多个商品共享。

```java
package io.github.atengk.design.flyweight;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.LabelRenderContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 促销标签样式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class PromotionLabelStyle implements LabelStyle {

    private final String backgroundColor = "#fff1f0";
    private final String fontColor = "#cf1322";
    private final String icon = "🔥";
    private final String prefix = "限时促销";

    /**
     * 获取样式编码
     *
     * @return 样式编码
     */
    @Override
    public String styleCode() {
        return "promotion";
    }

    /**
     * 渲染标签文本
     *
     * @param context 标签渲染上下文
     * @return 标签文本
     */
    @Override
    public String renderLabel(LabelRenderContext context) {
        validateContext(context);

        BigDecimal discountPrice = NumberUtil.mul(context.price(), BigDecimal.valueOf(0.9));
        return StrUtil.format("{} {}：{}，促销价 {} 元",
                icon, prefix, context.productName(), discountPrice);
    }

    /**
     * 获取样式描述
     *
     * @return 样式描述
     */
    @Override
    public String styleText() {
        return StrUtil.format("background:{};color:{};prefix:{}", backgroundColor, fontColor, prefix);
    }

    /**
     * 校验上下文
     *
     * @param context 标签渲染上下文
     */
    private void validateContext(LabelRenderContext context) {
        if (context == null || StrUtil.isBlank(context.productName()) || context.price() == null) {
            log.warn("渲染促销标签失败，上下文、商品名称或价格为空");
            throw new IllegalArgumentException("商品名称和价格不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/flyweight/StockWarningLabelStyle.java`

下面是库存预警标签样式。

```java
package io.github.atengk.design.flyweight;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.LabelRenderContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 库存预警标签样式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class StockWarningLabelStyle implements LabelStyle {

    private final String backgroundColor = "#fff7e6";
    private final String fontColor = "#d46b08";
    private final String icon = "⚠";
    private final String prefix = "库存预警";

    /**
     * 获取样式编码
     *
     * @return 样式编码
     */
    @Override
    public String styleCode() {
        return "stock_warning";
    }

    /**
     * 渲染标签文本
     *
     * @param context 标签渲染上下文
     * @return 标签文本
     */
    @Override
    public String renderLabel(LabelRenderContext context) {
        if (context == null || StrUtil.isBlank(context.productName()) || context.stock() == null) {
            log.warn("渲染库存预警标签失败，上下文、商品名称或库存为空");
            throw new IllegalArgumentException("商品名称和库存不能为空");
        }

        return StrUtil.format("{} {}：{}，当前库存 {} 件",
                icon, prefix, context.productName(), context.stock());
    }

    /**
     * 获取样式描述
     *
     * @return 样式描述
     */
    @Override
    public String styleText() {
        return StrUtil.format("background:{};color:{};prefix:{}", backgroundColor, fontColor, prefix);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/flyweight/NewProductLabelStyle.java`

下面是新品标签样式。

```java
package io.github.atengk.design.flyweight;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.LabelRenderContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 新品标签样式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class NewProductLabelStyle implements LabelStyle {

    private final String backgroundColor = "#f6ffed";
    private final String fontColor = "#389e0d";
    private final String icon = "🆕";
    private final String prefix = "新品上架";

    /**
     * 获取样式编码
     *
     * @return 样式编码
     */
    @Override
    public String styleCode() {
        return "new_product";
    }

    /**
     * 渲染标签文本
     *
     * @param context 标签渲染上下文
     * @return 标签文本
     */
    @Override
    public String renderLabel(LabelRenderContext context) {
        if (context == null || StrUtil.isBlank(context.productName())) {
            log.warn("渲染新品标签失败，上下文或商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        return StrUtil.format("{} {}：{}", icon, prefix, context.productName());
    }

    /**
     * 获取样式描述
     *
     * @return 样式描述
     */
    @Override
    public String styleText() {
        return StrUtil.format("background:{};color:{};prefix:{}", backgroundColor, fontColor, prefix);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/LabelStyleFactory.java`

下面是标签样式享元工厂。它负责按样式编码缓存并返回共享样式对象。

```java
package io.github.atengk.design.factory;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.flyweight.LabelStyle;
import io.github.atengk.design.flyweight.NewProductLabelStyle;
import io.github.atengk.design.flyweight.PromotionLabelStyle;
import io.github.atengk.design.flyweight.StockWarningLabelStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标签样式享元工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class LabelStyleFactory {

    private final Map<String, LabelStyle> styleCache = new ConcurrentHashMap<>();

    /**
     * 获取标签样式
     *
     * @param styleCode 样式编码
     * @return 标签样式享元
     */
    public LabelStyle getStyle(String styleCode) {
        if (StrUtil.isBlank(styleCode)) {
            log.warn("获取标签样式失败，样式编码为空");
            throw new IllegalArgumentException("样式编码不能为空");
        }

        String key = StrUtil.trim(styleCode).toLowerCase();
        return styleCache.computeIfAbsent(key, this::createStyle);
    }

    /**
     * 获取缓存数量
     *
     * @return 缓存数量
     */
    public int cacheSize() {
        return styleCache.size();
    }

    /**
     * 清理样式缓存
     */
    public void clearCache() {
        styleCache.clear();
        log.info("清理标签样式享元缓存完成");
    }

    /**
     * 创建标签样式
     *
     * @param styleCode 样式编码
     * @return 标签样式享元
     */
    private LabelStyle createStyle(String styleCode) {
        log.info("标签样式缓存未命中，开始创建样式，样式编码：{}", styleCode);

        return switch (styleCode) {
            case "promotion" -> new PromotionLabelStyle();
            case "stock_warning" -> new StockWarningLabelStyle();
            case "new_product" -> new NewProductLabelStyle();
            default -> {
                log.warn("创建标签样式失败，不支持的样式编码：{}", styleCode);
                throw new IllegalArgumentException("不支持的标签样式：" + styleCode);
            }
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/ProductLabelService.java`

下面是商品标签服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.LabelRenderRequest;
import io.github.atengk.design.dto.LabelRenderResponse;

/**
 * 商品标签服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface ProductLabelService {

    /**
     * 渲染商品标签
     *
     * @param request 标签渲染请求
     * @return 标签渲染响应
     */
    LabelRenderResponse render(LabelRenderRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/ProductLabelServiceImpl.java`

下面是商品标签服务实现。它从享元工厂获取共享样式对象，再将商品上下文作为外部状态传入。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.LabelRenderContext;
import io.github.atengk.design.dto.LabelRenderRequest;
import io.github.atengk.design.dto.LabelRenderResponse;
import io.github.atengk.design.factory.LabelStyleFactory;
import io.github.atengk.design.flyweight.LabelStyle;
import io.github.atengk.design.service.ProductLabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 商品标签服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLabelServiceImpl implements ProductLabelService {

    private final LabelStyleFactory labelStyleFactory;

    /**
     * 渲染商品标签
     *
     * @param request 标签渲染请求
     * @return 标签渲染响应
     */
    @Override
    public LabelRenderResponse render(LabelRenderRequest request) {
        validateRequest(request);

        LabelStyle labelStyle = labelStyleFactory.getStyle(request.styleCode());
        LabelRenderContext context = new LabelRenderContext(
                request.productId(),
                request.productName(),
                request.price(),
                request.stock()
        );

        String labelText = labelStyle.renderLabel(context);
        String styleText = labelStyle.styleText();

        log.info("商品标签渲染完成，样式编码：{}，商品ID：{}，缓存数量：{}",
                labelStyle.styleCode(), request.productId(), labelStyleFactory.cacheSize());

        return new LabelRenderResponse(
                labelStyle.styleCode(),
                request.productId(),
                request.productName(),
                labelText,
                styleText,
                labelStyleFactory.cacheSize()
        );
    }

    /**
     * 校验标签渲染请求
     *
     * @param request 标签渲染请求
     */
    private void validateRequest(LabelRenderRequest request) {
        if (request == null) {
            log.warn("渲染商品标签失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.styleCode(), request.productName())) {
            log.warn("渲染商品标签失败，样式编码或商品名称为空");
            throw new IllegalArgumentException("样式编码和商品名称不能为空");
        }

        if (request.productId() == null || request.productId() <= 0) {
            log.warn("渲染商品标签失败，商品ID不合法，商品ID：{}", request.productId());
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("渲染商品标签失败，商品价格不合法，商品价格：{}", request.price());
            throw new IllegalArgumentException("商品价格必须大于0");
        }

        if (request.stock() == null || request.stock() < 0) {
            log.warn("渲染商品标签失败，商品库存不合法，商品库存：{}", request.stock());
            throw new IllegalArgumentException("商品库存不能小于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/ProductLabelController.java`

下面是商品标签接口，用于验证享元模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.LabelRenderRequest;
import io.github.atengk.design.dto.LabelRenderResponse;
import io.github.atengk.design.service.ProductLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 商品标签控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/flyweight/product-label")
public class ProductLabelController {

    private final ProductLabelService productLabelService;

    /**
     * 渲染商品标签
     *
     * @param styleCode   标签样式编码
     * @param productId   商品ID
     * @param productName 商品名称
     * @param price       商品价格
     * @param stock       商品库存
     * @return 标签渲染响应
     */
    @GetMapping("/render")
    public LabelRenderResponse render(@RequestParam String styleCode,
                                      @RequestParam Long productId,
                                      @RequestParam String productName,
                                      @RequestParam BigDecimal price,
                                      @RequestParam Integer stock) {
        LabelRenderRequest request = new LabelRenderRequest(
                styleCode,
                productId,
                productName,
                price,
                stock
        );

        return productLabelService.render(request);
    }
}
```

接口调用示例：

```bash
curl "http://localhost:8080/flyweight/product-label/render?styleCode=promotion&productId=10001&productName=机械键盘&price=199.00&stock=50"

curl "http://localhost:8080/flyweight/product-label/render?styleCode=promotion&productId=10002&productName=无线鼠标&price=99.00&stock=80"

curl "http://localhost:8080/flyweight/product-label/render?styleCode=stock_warning&productId=10003&productName=显示器&price=999.00&stock=3"
```

促销标签可能返回：

```json
{
  "styleCode": "promotion",
  "productId": 10001,
  "productName": "机械键盘",
  "labelText": "🔥 限时促销：机械键盘，促销价 179.100 元",
  "styleText": "background:#fff1f0;color:#cf1322;prefix:限时促销",
  "cacheSize": 1
}
```

连续多次使用 `promotion` 样式时，缓存数量不会重复增长，因为共享的是同一个促销标签样式对象。

## 扩展一个新享元对象

在享元模式中，扩展新的共享对象通常是在享元工厂中增加一个创建分支，或者把享元对象注册为 Spring Bean 后通过 Map 管理。下面以“会员专享标签”为例，新增样式 `vip`。

文件位置：`src/main/java/io/github/atengk/design/flyweight/VipLabelStyle.java`

下面是会员专享标签样式。

```java
package io.github.atengk.design.flyweight;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.LabelRenderContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 会员专享标签样式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class VipLabelStyle implements LabelStyle {

    private final String backgroundColor = "#f9f0ff";
    private final String fontColor = "#722ed1";
    private final String icon = "👑";
    private final String prefix = "会员专享";

    /**
     * 获取样式编码
     *
     * @return 样式编码
     */
    @Override
    public String styleCode() {
        return "vip";
    }

    /**
     * 渲染标签文本
     *
     * @param context 标签渲染上下文
     * @return 标签文本
     */
    @Override
    public String renderLabel(LabelRenderContext context) {
        if (context == null || StrUtil.isBlank(context.productName())) {
            log.warn("渲染会员专享标签失败，上下文或商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        return StrUtil.format("{} {}：{}", icon, prefix, context.productName());
    }

    /**
     * 获取样式描述
     *
     * @return 样式描述
     */
    @Override
    public String styleText() {
        return StrUtil.format("background:{};color:{};prefix:{}", backgroundColor, fontColor, prefix);
    }
}
```

在 `LabelStyleFactory` 的 `createStyle` 中新增分支：

```java
case "vip" -> new VipLabelStyle();
```

调用示例：

```bash
curl "http://localhost:8080/flyweight/product-label/render?styleCode=vip&productId=10004&productName=会员礼盒&price=299.00&stock=20"
```

如果享元类型会频繁增加，可以进一步改造成 Spring Bean 注册方式，避免每次都修改工厂的 `switch`。

## Spring Bean 享元注册方式

如果享元对象本身是无状态或只持有不可变内部状态，可以直接把每个享元对象注册为 Spring 单例 Bean。Spring 默认单例本身就是一种共享机制。下面是改造思路。

文件位置：`src/main/java/io/github/atengk/design/flyweight/SpringPromotionLabelStyle.java`

下面的写法将促销标签样式直接作为 Spring Bean 共享。

```java
package io.github.atengk.design.flyweight;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.LabelRenderContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Spring促销标签样式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class SpringPromotionLabelStyle implements LabelStyle {

    /**
     * 获取样式编码
     *
     * @return 样式编码
     */
    @Override
    public String styleCode() {
        return "spring_promotion";
    }

    /**
     * 渲染标签文本
     *
     * @param context 标签渲染上下文
     * @return 标签文本
     */
    @Override
    public String renderLabel(LabelRenderContext context) {
        if (context == null || StrUtil.isBlank(context.productName())) {
            log.warn("渲染Spring促销标签失败，商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        return StrUtil.format("Spring共享促销标签：{}", context.productName());
    }

    /**
     * 获取样式描述
     *
     * @return 样式描述
     */
    @Override
    public String styleText() {
        return "background:#fff1f0;color:#cf1322;prefix:Spring共享促销";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/SpringLabelStyleRegistry.java`

下面是基于 Spring Bean 的享元注册表。它收集所有 `LabelStyle` Bean，并按样式编码建立索引。

```java
package io.github.atengk.design.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.flyweight.LabelStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring标签样式享元注册表
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class SpringLabelStyleRegistry {

    private final Map<String, LabelStyle> styleMap;

    /**
     * 创建Spring标签样式享元注册表
     *
     * @param styles 标签样式列表
     */
    public SpringLabelStyleRegistry(List<LabelStyle> styles) {
        if (CollUtil.isEmpty(styles)) {
            log.warn("Spring标签样式列表为空");
            this.styleMap = Map.of();
            return;
        }

        this.styleMap = styles.stream()
                .collect(Collectors.toUnmodifiableMap(
                        style -> StrUtil.trim(style.styleCode()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化Spring标签样式享元注册表，样式数量：{}，样式编码：{}",
                styleMap.size(), styleMap.keySet());
    }

    /**
     * 获取标签样式
     *
     * @param styleCode 样式编码
     * @return 标签样式
     */
    public LabelStyle getStyle(String styleCode) {
        if (StrUtil.isBlank(styleCode)) {
            log.warn("获取Spring标签样式失败，样式编码为空");
            throw new IllegalArgumentException("样式编码不能为空");
        }

        LabelStyle style = styleMap.get(StrUtil.trim(styleCode).toLowerCase());
        if (style == null) {
            log.warn("获取Spring标签样式失败，不支持的样式编码：{}", styleCode);
            throw new IllegalArgumentException("不支持的样式编码：" + styleCode);
        }

        return style;
    }
}
```

这种方式更符合 Spring Boot 项目的扩展习惯。新增享元对象时，只需要新增一个 `@Component` 实现类，不需要改动注册表逻辑。

## 内部状态和外部状态

享元模式最容易出错的地方，是把外部状态误放进共享对象成员变量。共享对象如果保存了请求级数据，在并发环境下会出现数据串扰。

推荐内部状态：

```text
样式编码
颜色
图标
固定模板
固定规则
不可变配置
```

推荐外部状态：

```text
用户ID
订单号
商品ID
商品名称
当前库存
当前价格
本次操作人
本次请求流水号
```

错误示例：

```java
public class PromotionLabelStyle implements LabelStyle {

    private Long currentProductId;
    private String currentProductName;

    public String renderLabel(LabelRenderContext context) {
        this.currentProductId = context.productId();
        this.currentProductName = context.productName();
        return currentProductName;
    }
}
```

这种写法在 Spring 单例或工厂缓存对象中是危险的。多个请求同时渲染时，成员变量会互相覆盖。

推荐写法：

```java
public String renderLabel(LabelRenderContext context) {
    return StrUtil.format("促销商品：{}", context.productName());
}
```

共享对象只保存可复用状态，每次变化的数据都通过方法参数传入。

## 享元模式和缓存的区别

享元模式经常使用缓存，但它和普通缓存不是一回事。

| 对比项   | 享元模式                     | 普通缓存                     |
| -------- | ---------------------------- | ---------------------------- |
| 核心目的 | 共享大量细粒度对象           | 减少重复查询或计算           |
| 关注点   | 对象共享、状态拆分           | 数据读取性能                 |
| 典型对象 | 样式、模板、处理器、图标     | 查询结果、接口响应、统计结果 |
| 状态要求 | 内部状态可共享，外部状态传入 | 不一定拆分状态               |
| 使用方式 | 工厂返回共享对象             | 缓存命中返回数据             |

简单理解：

```text
享元模式：共享对象本身。
普通缓存：缓存数据结果。
```

例如共享 `PromotionLabelStyle` 是享元模式。缓存某个商品的详情 JSON 是普通缓存。两者可以组合使用，但意图不同。

## 享元模式和单例模式的区别

享元模式和单例模式都可能共享对象，但粒度不同。

| 对比项       | 享元模式                 | 单例模式                 |
| ------------ | ------------------------ | ------------------------ |
| 对象数量     | 可以有多个共享对象       | 通常只有一个实例         |
| 访问方式     | 通过 key 从工厂获取      | 通过固定入口获取唯一实例 |
| 关注点       | 共享大量细粒度对象       | 保证全局唯一             |
| 典型场景     | 字典项、样式、图标、模板 | 配置管理器、上下文对象   |
| 是否需要工厂 | 通常需要                 | 不一定                   |

简单理解：

```text
单例模式：这个类全局只有一个对象。
享元模式：这类对象按 key 共享，能复用就复用。
```

Spring Bean 默认单例可以作为享元对象的载体，但不是所有单例 Bean 都是享元模式。享元模式更强调大量相似对象的共享和内部状态、外部状态的拆分。

## 享元模式和原型模式的区别

享元模式和原型模式都和对象创建有关，但方向相反。

| 对比项   | 享元模式                   | 原型模式                     |
| -------- | -------------------------- | ---------------------------- |
| 核心目的 | 共享对象，减少创建数量     | 复制对象，快速创建新对象     |
| 对象关系 | 多个调用方共享同一个对象   | 基于模板复制出新对象         |
| 状态处理 | 内部状态共享，外部状态传入 | 复制已有对象状态             |
| 典型场景 | 样式、图标、模板、规则     | 审批流模板复制、导出任务复制 |
| 风险点   | 请求级状态污染共享对象     | 浅拷贝导致引用共享           |

简单理解：

```text
享元模式：别重复创建，大家共享一个。
原型模式：已有一个模板，复制一份新的。
```

商品标签样式适合享元模式。基于一个审批流程模板复制出新的审批流程，适合原型模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行促销标签渲染：

```bash
curl "http://localhost:8080/flyweight/product-label/render?styleCode=promotion&productId=10001&productName=机械键盘&price=199.00&stock=50"
```

再次执行同一标签样式但换不同商品：

```bash
curl "http://localhost:8080/flyweight/product-label/render?styleCode=promotion&productId=10002&productName=无线鼠标&price=99.00&stock=80"
```

执行库存预警标签渲染：

```bash
curl "http://localhost:8080/flyweight/product-label/render?styleCode=stock_warning&productId=10003&productName=显示器&price=999.00&stock=3"
```

如果享元模式正常，可以看到类似日志：

```text
标签样式缓存未命中，开始创建样式，样式编码：promotion
商品标签渲染完成，样式编码：promotion，商品ID：10001，缓存数量：1
商品标签渲染完成，样式编码：promotion，商品ID：10002，缓存数量：1
标签样式缓存未命中，开始创建样式，样式编码：stock_warning
商品标签渲染完成，样式编码：stock_warning，商品ID：10003，缓存数量：2
```

重点观察第二次 `promotion` 请求。它不会再次创建 `PromotionLabelStyle`，而是复用缓存中的共享对象。

执行不支持的样式编码：

```bash
curl "http://localhost:8080/flyweight/product-label/render?styleCode=unknown&productId=10004&productName=测试商品&price=10.00&stock=1"
```

异常日志示例：

```text
创建标签样式失败，不支持的样式编码：unknown
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

享元模式适合共享大量细粒度对象，但不适合所有对象。只有当对象数量大、内部状态重复、外部状态可以清晰拆分时，享元模式才有明显收益。

适合使用享元模式的场景：

```text
大量重复样式对象
大量重复模板对象
大量重复规则对象
大量重复字典项对象
大量重复图标对象
大量无状态处理器
对象创建成本较高且可复用
```

不太适合使用享元模式的场景：

```text
对象数量很少
对象状态每次都不同
对象包含请求级可变状态
对象生命周期非常短且创建成本低
共享后反而增加理解成本
```

不要把请求级状态放进享元对象成员变量中。Spring Bean 默认是单例，工厂缓存对象也会被多个请求共享。

错误示例：

```java
private Long currentUserId;
private String currentOrderNo;
private String currentProductName;
```

推荐把这些数据放到上下文对象中：

```java
public String renderLabel(LabelRenderContext context) {
    return StrUtil.format("商品：{}", context.productName());
}
```

享元对象最好设计为不可变对象。内部状态通过构造方法设置，后续不再修改。

推荐写法：

```java
private final String backgroundColor;
private final String fontColor;
private final String prefix;
```

如果享元缓存可能无限增长，需要设计清理策略。本文示例中的样式编码是有限集合，因此可以长期缓存。如果 key 来自用户输入且数量不可控，就要谨慎。

风险示例：

```text
styleCode=user_custom_10001
styleCode=user_custom_10002
styleCode=user_custom_10003
...
```

这种情况下缓存可能持续增长，导致内存压力。可以考虑限制缓存大小、使用 Caffeine、Redis 或定期清理。

生产环境中常见控制方式：

```text
限制 key 来源
限制缓存最大数量
缓存过期时间
定期清理不活跃对象
监控缓存命中率和缓存大小
```

享元模式不应该掩盖业务语义。如果对象本身应该是独立的业务实体，例如订单、支付单、用户会话，不应该为了减少对象数量而强行共享。

不推荐共享这些对象：

```text
订单实体
支付单实体
用户会话上下文
请求上下文
事务上下文
当前操作日志对象
```

这些对象通常都有独立状态，强行共享会导致数据污染和并发问题。

## 总结

在 JDK21 和 Spring Boot 3 项目中，享元模式的实践重点是把可共享的内部状态提取到共享对象中，把每次变化的外部状态通过参数传入，从而减少重复对象创建和内存占用。

普通 Java 享元模式适合理解对象共享、内部状态和外部状态拆分。Spring Boot 项目中更常见的是“享元接口 + 具体享元对象 + 享元工厂或 Spring 单例注册表”的结构。对于商品标签样式、消息模板、菜单图标、字典项、规则配置、无状态处理器等场景，享元模式可以减少重复对象数量，并让共享对象管理更加清晰。

享元模式不是普通缓存的同义词，也不是单例模式的替代品。它最适合处理“大量对象高度相似，且共享状态和变化状态可以清晰拆分”的场景。实际落地时，需要重点控制请求级状态污染、缓存无限增长、线程安全和对象职责边界。
