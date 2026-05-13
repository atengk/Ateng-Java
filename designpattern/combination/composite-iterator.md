# 组合模式 + 迭代器模式

组合模式和迭代器模式组合使用时，通常用于处理 **树形结构组织和统一遍历** 的业务场景。

组合模式负责把目录、菜单、按钮、部门、分类等树形节点组织成统一对象结构；迭代器模式负责在不暴露树形结构内部细节的情况下，提供统一遍历方式。这个组合适合放在你原文档的“设计模式组合使用”章节中，符合原文档中“组合模式组织树形结构，迭代器模式提供统一遍历方式”的定义。


```text
组合模式：用统一方式组织树形结构
迭代器模式：用统一方式遍历树形节点
```

## 适用场景

本示例以“权限菜单树”为业务场景。系统中的权限菜单通常包含目录、菜单和按钮：

| 节点类型      | 说明   | 示例             |
| --------- | ---- | -------------- |
| DIRECTORY | 目录节点 | 系统管理、订单中心      |
| MENU      | 菜单节点 | 用户管理、订单列表      |
| BUTTON    | 按钮节点 | 新增用户、删除订单、导出订单 |

这些节点天然是树形结构：

```text
系统菜单
  -> 工作台
      -> 首页
  -> 系统管理
      -> 用户管理
          -> 新增用户
          -> 删除用户
      -> 角色管理
  -> 订单中心
      -> 订单列表
          -> 导出订单
```

如果只用普通集合处理，业务代码里会反复出现递归、层级判断、节点类型判断。使用“组合模式 + 迭代器模式”后，结构变成：

```text
Controller
  -> Service
      -> MenuTreeBuilder：构建菜单组合树
          -> MenuComponent：统一菜单节点抽象
              -> MenuDirectory：目录节点，可包含子节点
              -> MenuLeaf：叶子节点，不能包含子节点
      -> MenuIterator：统一遍历菜单树
          -> DepthFirstMenuIterator：深度优先遍历
          -> BreadthFirstMenuIterator：广度优先遍历
```

## 基础配置

这里使用 JDK 21、Spring Boot 3、Maven、Lombok、Hutool 和 Spring Validation。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Validation：用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：提供字符串、对象、集合、日期、ID 等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、Builder、构造器、日志等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/combination/compositeiterator
├── composite
│   ├── MenuComponent.java
│   ├── MenuDirectory.java
│   └── MenuLeaf.java
├── controller
│   └── MenuController.java
├── converter
│   └── MenuNodeConverter.java
├── dto
│   ├── MenuNodeVO.java
│   ├── MenuQueryRequest.java
│   └── MenuQueryResponse.java
├── enums
│   ├── MenuNodeTypeEnum.java
│   └── TraversalTypeEnum.java
├── handler
│   └── GlobalExceptionHandler.java
├── iterator
│   ├── BreadthFirstMenuIterator.java
│   └── DepthFirstMenuIterator.java
├── repository
│   ├── MenuDataRepository.java
│   └── MenuRecord.java
├── result
│   └── Result.java
├── service
│   ├── MenuQueryService.java
│   └── impl
│       └── MenuQueryServiceImpl.java
└── tree
    └── MenuTreeBuilder.java
```

## 核心代码

这一部分给出核心实现。重点看两个位置：

```text
MenuComponent：组合模式的统一节点抽象
DepthFirstMenuIterator / BreadthFirstMenuIterator：迭代器模式的遍历实现
```

组合模式让目录节点和叶子节点都可以用 `MenuComponent` 表达；迭代器模式让业务层可以用统一方式遍历整棵菜单树，而不需要关心递归细节。

### 菜单节点类型枚举

菜单节点类型枚举用于统一维护目录、菜单和按钮三类节点。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/enums/MenuNodeTypeEnum.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 菜单节点类型枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum MenuNodeTypeEnum {

    DIRECTORY("DIRECTORY", "目录"),

    MENU("MENU", "菜单"),

    BUTTON("BUTTON", "按钮");

    private final String code;

    private final String description;

    /**
     * 根据编码获取菜单节点类型
     *
     * @param code 节点类型编码
     * @return 菜单节点类型
     */
    public static MenuNodeTypeEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (MenuNodeTypeEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的菜单节点类型：" + code);
    }
}
```

### 遍历类型枚举

遍历类型枚举用于控制菜单树的遍历方式。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/enums/TraversalTypeEnum.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 遍历类型枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum TraversalTypeEnum {

    DEPTH_FIRST("DEPTH_FIRST", "深度优先遍历"),

    BREADTH_FIRST("BREADTH_FIRST", "广度优先遍历");

    private final String code;

    private final String description;

    /**
     * 根据编码获取遍历类型
     *
     * @param code 遍历类型编码
     * @return 遍历类型
     */
    public static TraversalTypeEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (TraversalTypeEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的遍历类型：" + code);
    }
}
```

### 菜单组合抽象节点

组合抽象节点统一定义目录、菜单、按钮的公共属性和操作。目录节点可以重写 `add`、`remove`、`getChildren`，叶子节点则保持默认行为。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/composite/MenuComponent.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.composite;

import io.github.atengk.pattern.combination.compositeiterator.enums.MenuNodeTypeEnum;
import io.github.atengk.pattern.combination.compositeiterator.iterator.BreadthFirstMenuIterator;
import io.github.atengk.pattern.combination.compositeiterator.iterator.DepthFirstMenuIterator;
import lombok.Getter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 菜单组合抽象节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public abstract class MenuComponent {

    private final Long id;

    private final Long parentId;

    private final String name;

    private final MenuNodeTypeEnum nodeType;

    private final String path;

    private final String permission;

    private final Integer sort;

    private final Boolean visible;

    /**
     * 初始化菜单节点
     *
     * @param id         节点ID
     * @param parentId   父节点ID
     * @param name       节点名称
     * @param nodeType   节点类型
     * @param path       路由路径
     * @param permission 权限标识
     * @param sort       排序值
     * @param visible    是否可见
     */
    protected MenuComponent(Long id,
                            Long parentId,
                            String name,
                            MenuNodeTypeEnum nodeType,
                            String path,
                            String permission,
                            Integer sort,
                            Boolean visible) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.nodeType = nodeType;
        this.path = path;
        this.permission = permission;
        this.sort = sort;
        this.visible = visible;
    }

    /**
     * 添加子节点
     *
     * @param component 子节点
     */
    public void add(MenuComponent component) {
        throw new UnsupportedOperationException("当前菜单节点不支持添加子节点");
    }

    /**
     * 移除子节点
     *
     * @param id 子节点ID
     */
    public void remove(Long id) {
        throw new UnsupportedOperationException("当前菜单节点不支持移除子节点");
    }

    /**
     * 获取子节点
     *
     * @return 子节点集合
     */
    public List<MenuComponent> getChildren() {
        return Collections.emptyList();
    }

    /**
     * 判断是否为组合节点
     *
     * @return 是否为组合节点
     */
    public boolean isComposite() {
        return false;
    }

    /**
     * 创建深度优先迭代器
     *
     * @return 深度优先迭代器
     */
    public Iterator<MenuComponent> depthFirstIterator() {
        return new DepthFirstMenuIterator(this);
    }

    /**
     * 创建广度优先迭代器
     *
     * @return 广度优先迭代器
     */
    public Iterator<MenuComponent> breadthFirstIterator() {
        return new BreadthFirstMenuIterator(this);
    }
}
```

### 菜单目录节点

菜单目录节点是组合节点，可以包含子目录、菜单和按钮。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/composite/MenuDirectory.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.composite;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.compositeiterator.enums.MenuNodeTypeEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单目录节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class MenuDirectory extends MenuComponent {

    private final List<MenuComponent> children = new ArrayList<>();

    /**
     * 初始化菜单目录节点
     *
     * @param id         节点ID
     * @param parentId   父节点ID
     * @param name       节点名称
     * @param path       路由路径
     * @param permission 权限标识
     * @param sort       排序值
     * @param visible    是否可见
     */
    public MenuDirectory(Long id,
                         Long parentId,
                         String name,
                         String path,
                         String permission,
                         Integer sort,
                         Boolean visible) {
        super(id, parentId, name, MenuNodeTypeEnum.DIRECTORY, path, permission, sort, visible);
    }

    /**
     * 添加子节点
     *
     * @param component 子节点
     */
    @Override
    public void add(MenuComponent component) {
        if (ObjectUtil.isNotNull(component)) {
            children.add(component);
        }
    }

    /**
     * 移除子节点
     *
     * @param id 子节点ID
     */
    @Override
    public void remove(Long id) {
        children.removeIf(item -> ObjectUtil.equal(item.getId(), id));
    }

    /**
     * 获取子节点
     *
     * @return 子节点集合
     */
    @Override
    public List<MenuComponent> getChildren() {
        return children;
    }

    /**
     * 判断是否为组合节点
     *
     * @return 是否为组合节点
     */
    @Override
    public boolean isComposite() {
        return true;
    }
}
```

### 菜单叶子节点

菜单叶子节点用于表达菜单或按钮。叶子节点不能继续添加子节点。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/composite/MenuLeaf.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.composite;

import io.github.atengk.pattern.combination.compositeiterator.enums.MenuNodeTypeEnum;

/**
 * 菜单叶子节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class MenuLeaf extends MenuComponent {

    /**
     * 初始化菜单叶子节点
     *
     * @param id         节点ID
     * @param parentId   父节点ID
     * @param name       节点名称
     * @param nodeType   节点类型
     * @param path       路由路径
     * @param permission 权限标识
     * @param sort       排序值
     * @param visible    是否可见
     */
    public MenuLeaf(Long id,
                    Long parentId,
                    String name,
                    MenuNodeTypeEnum nodeType,
                    String path,
                    String permission,
                    Integer sort,
                    Boolean visible) {
        super(id, parentId, name, nodeType, path, permission, sort, visible);
    }
}
```

### 深度优先菜单迭代器

深度优先迭代器按照“先访问当前节点，再访问子节点”的顺序遍历菜单树。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/iterator/DepthFirstMenuIterator.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.iterator;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.compositeiterator.composite.MenuComponent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 深度优先菜单迭代器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class DepthFirstMenuIterator implements Iterator<MenuComponent> {

    private final Deque<MenuComponent> stack = new ArrayDeque<>();

    /**
     * 初始化深度优先迭代器
     *
     * @param root 根节点
     */
    public DepthFirstMenuIterator(MenuComponent root) {
        if (ObjectUtil.isNotNull(root)) {
            stack.push(root);
        }
    }

    /**
     * 判断是否存在下一个节点
     *
     * @return 是否存在下一个节点
     */
    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    /**
     * 获取下一个节点
     *
     * @return 菜单节点
     */
    @Override
    public MenuComponent next() {
        if (!hasNext()) {
            throw new NoSuchElementException("菜单树已经遍历完成");
        }

        MenuComponent current = stack.pop();
        List<MenuComponent> children = current.getChildren();

        for (int index = children.size() - 1; index >= 0; index--) {
            stack.push(children.get(index));
        }

        return current;
    }
}
```

### 广度优先菜单迭代器

广度优先迭代器按照“先访问同层节点，再访问下一层节点”的顺序遍历菜单树。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/iterator/BreadthFirstMenuIterator.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.iterator;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.compositeiterator.composite.MenuComponent;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * 广度优先菜单迭代器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BreadthFirstMenuIterator implements Iterator<MenuComponent> {

    private final Queue<MenuComponent> queue = new ArrayDeque<>();

    /**
     * 初始化广度优先迭代器
     *
     * @param root 根节点
     */
    public BreadthFirstMenuIterator(MenuComponent root) {
        if (ObjectUtil.isNotNull(root)) {
            queue.offer(root);
        }
    }

    /**
     * 判断是否存在下一个节点
     *
     * @return 是否存在下一个节点
     */
    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    /**
     * 获取下一个节点
     *
     * @return 菜单节点
     */
    @Override
    public MenuComponent next() {
        if (!hasNext()) {
            throw new NoSuchElementException("菜单树已经遍历完成");
        }

        MenuComponent current = queue.poll();
        queue.addAll(current.getChildren());
        return current;
    }
}
```

### 菜单数据记录

菜单数据记录用于模拟从数据库查询出来的菜单权限数据。真实项目中可以替换成 MyBatis-Plus 实体或 VO。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/repository/MenuRecord.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.repository;

import lombok.Builder;
import lombok.Data;

/**
 * 菜单数据记录
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class MenuRecord {

    /**
     * 节点ID
     */
    private Long id;

    /**
     * 父节点ID
     */
    private Long parentId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 是否可见
     */
    private Boolean visible;
}
```

### 菜单数据仓储

菜单数据仓储用于模拟查询用户可访问的菜单节点。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/repository/MenuDataRepository.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 菜单数据仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Repository
public class MenuDataRepository {

    /**
     * 查询用户菜单记录
     *
     * @param userId 用户ID
     * @return 菜单记录集合
     */
    public List<MenuRecord> findByUserId(Long userId) {
        log.info("查询用户菜单数据，用户ID：{}", userId);

        return List.of(
                MenuRecord.builder()
                        .id(1L)
                        .parentId(null)
                        .name("工作台")
                        .nodeType("DIRECTORY")
                        .path("/dashboard")
                        .permission("dashboard")
                        .sort(10)
                        .visible(true)
                        .build(),
                MenuRecord.builder()
                        .id(2L)
                        .parentId(1L)
                        .name("首页")
                        .nodeType("MENU")
                        .path("/dashboard/home")
                        .permission("dashboard:home:view")
                        .sort(10)
                        .visible(true)
                        .build(),
                MenuRecord.builder()
                        .id(10L)
                        .parentId(null)
                        .name("系统管理")
                        .nodeType("DIRECTORY")
                        .path("/system")
                        .permission("system")
                        .sort(20)
                        .visible(true)
                        .build(),
                MenuRecord.builder()
                        .id(11L)
                        .parentId(10L)
                        .name("用户管理")
                        .nodeType("MENU")
                        .path("/system/user")
                        .permission("system:user:view")
                        .sort(10)
                        .visible(true)
                        .build(),
                MenuRecord.builder()
                        .id(12L)
                        .parentId(11L)
                        .name("新增用户")
                        .nodeType("BUTTON")
                        .path(null)
                        .permission("system:user:add")
                        .sort(10)
                        .visible(false)
                        .build(),
                MenuRecord.builder()
                        .id(13L)
                        .parentId(11L)
                        .name("删除用户")
                        .nodeType("BUTTON")
                        .path(null)
                        .permission("system:user:delete")
                        .sort(20)
                        .visible(false)
                        .build(),
                MenuRecord.builder()
                        .id(14L)
                        .parentId(10L)
                        .name("角色管理")
                        .nodeType("MENU")
                        .path("/system/role")
                        .permission("system:role:view")
                        .sort(20)
                        .visible(true)
                        .build(),
                MenuRecord.builder()
                        .id(20L)
                        .parentId(null)
                        .name("订单中心")
                        .nodeType("DIRECTORY")
                        .path("/order")
                        .permission("order")
                        .sort(30)
                        .visible(true)
                        .build(),
                MenuRecord.builder()
                        .id(21L)
                        .parentId(20L)
                        .name("订单列表")
                        .nodeType("MENU")
                        .path("/order/list")
                        .permission("order:list:view")
                        .sort(10)
                        .visible(true)
                        .build(),
                MenuRecord.builder()
                        .id(22L)
                        .parentId(21L)
                        .name("导出订单")
                        .nodeType("BUTTON")
                        .path(null)
                        .permission("order:list:export")
                        .sort(10)
                        .visible(false)
                        .build()
        );
    }
}
```

### 菜单树构建器

菜单树构建器负责把平铺菜单记录转换成组合树。目录节点使用 `MenuDirectory`，菜单和按钮使用 `MenuLeaf`。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/tree/MenuTreeBuilder.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.tree;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.compositeiterator.composite.MenuComponent;
import io.github.atengk.pattern.combination.compositeiterator.composite.MenuDirectory;
import io.github.atengk.pattern.combination.compositeiterator.composite.MenuLeaf;
import io.github.atengk.pattern.combination.compositeiterator.enums.MenuNodeTypeEnum;
import io.github.atengk.pattern.combination.compositeiterator.repository.MenuRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单树构建器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class MenuTreeBuilder {

    /**
     * 构建菜单组合树
     *
     * @param records       菜单记录集合
     * @param includeButton 是否包含按钮节点
     * @return 菜单根节点
     */
    public MenuComponent build(List<MenuRecord> records, Boolean includeButton) {
        MenuDirectory root = new MenuDirectory(0L, null, "系统菜单", "/", "root", 0, true);
        if (CollUtil.isEmpty(records)) {
            log.info("菜单记录为空，返回空菜单根节点");
            return root;
        }

        Map<Long, MenuComponent> nodeMap = new LinkedHashMap<>();
        for (MenuRecord record : records) {
            MenuComponent component = convert(record, includeButton);
            if (ObjectUtil.isNotNull(component)) {
                nodeMap.put(component.getId(), component);
            }
        }

        for (MenuComponent component : nodeMap.values()) {
            MenuComponent parent = nodeMap.get(component.getParentId());
            if (ObjectUtil.isNull(parent)) {
                root.add(component);
            } else {
                parent.add(component);
            }
        }

        sortChildren(root);
        log.info("菜单组合树构建完成，节点数量：{}", nodeMap.size());
        return root;
    }

    /**
     * 转换菜单记录为菜单节点
     *
     * @param record        菜单记录
     * @param includeButton 是否包含按钮节点
     * @return 菜单节点
     */
    private MenuComponent convert(MenuRecord record, Boolean includeButton) {
        MenuNodeTypeEnum nodeType = MenuNodeTypeEnum.of(record.getNodeType());
        if (MenuNodeTypeEnum.BUTTON.equals(nodeType) && Boolean.FALSE.equals(includeButton)) {
            return null;
        }

        if (MenuNodeTypeEnum.DIRECTORY.equals(nodeType)) {
            return new MenuDirectory(
                    record.getId(),
                    record.getParentId(),
                    record.getName(),
                    record.getPath(),
                    record.getPermission(),
                    record.getSort(),
                    record.getVisible()
            );
        }

        return new MenuLeaf(
                record.getId(),
                record.getParentId(),
                record.getName(),
                nodeType,
                record.getPath(),
                record.getPermission(),
                record.getSort(),
                record.getVisible()
        );
    }

    /**
     * 递归排序子节点
     *
     * @param component 菜单节点
     */
    private void sortChildren(MenuComponent component) {
        if (!component.isComposite()) {
            return;
        }

        component.getChildren().sort(Comparator.comparing(MenuComponent::getSort));
        for (MenuComponent child : component.getChildren()) {
            sortChildren(child);
        }
    }
}
```

### 菜单节点 VO

菜单节点 VO 用于返回前端可直接使用的菜单树结构。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/dto/MenuNodeVO.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单节点 VO
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class MenuNodeVO {

    /**
     * 节点ID
     */
    private Long id;

    /**
     * 父节点ID
     */
    private Long parentId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 是否可见
     */
    private Boolean visible;

    /**
     * 子节点
     */
    @Builder.Default
    private List<MenuNodeVO> children = new ArrayList<>();
}
```

### 菜单节点转换器

菜单节点转换器用于把组合树节点转换成前端响应对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/converter/MenuNodeConverter.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.converter;

import io.github.atengk.pattern.combination.compositeiterator.composite.MenuComponent;
import io.github.atengk.pattern.combination.compositeiterator.dto.MenuNodeVO;

import java.util.List;

/**
 * 菜单节点转换器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class MenuNodeConverter {

    private MenuNodeConverter() {
    }

    /**
     * 转换菜单节点为 VO
     *
     * @param component 菜单节点
     * @return 菜单节点 VO
     */
    public static MenuNodeVO toVo(MenuComponent component) {
        List<MenuNodeVO> children = component.getChildren()
                .stream()
                .map(MenuNodeConverter::toVo)
                .toList();

        return MenuNodeVO.builder()
                .id(component.getId())
                .parentId(component.getParentId())
                .name(component.getName())
                .nodeType(component.getNodeType().getCode())
                .path(component.getPath())
                .permission(component.getPermission())
                .sort(component.getSort())
                .visible(component.getVisible())
                .children(children)
                .build();
    }

    /**
     * 转换菜单节点为扁平 VO
     *
     * @param component 菜单节点
     * @return 菜单节点 VO
     */
    public static MenuNodeVO toFlatVo(MenuComponent component) {
        return MenuNodeVO.builder()
                .id(component.getId())
                .parentId(component.getParentId())
                .name(component.getName())
                .nodeType(component.getNodeType().getCode())
                .path(component.getPath())
                .permission(component.getPermission())
                .sort(component.getSort())
                .visible(component.getVisible())
                .build();
    }
}
```

### 菜单查询请求对象

菜单查询请求对象用于控制是否包含按钮，以及使用哪种遍历方式返回扁平节点列表。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/dto/MenuQueryRequest.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单查询请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class MenuQueryRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 遍历方式：DEPTH_FIRST、BREADTH_FIRST
     */
    private String traversalType;

    /**
     * 是否包含按钮节点
     */
    private Boolean includeButton;
}
```

### 菜单查询响应对象

菜单查询响应对象同时返回菜单树和遍历后的扁平节点列表。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/dto/MenuQueryResponse.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 菜单查询响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class MenuQueryResponse {

    /**
     * 遍历方式
     */
    private String traversalType;

    /**
     * 菜单树
     */
    private MenuNodeVO tree;

    /**
     * 扁平节点列表
     */
    private List<MenuNodeVO> flatNodes;

    /**
     * 节点总数
     */
    private Integer totalNodeCount;
}
```

### Service 接口

Service 接口对外暴露菜单查询能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/service/MenuQueryService.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.service;

import io.github.atengk.pattern.combination.compositeiterator.dto.MenuQueryRequest;
import io.github.atengk.pattern.combination.compositeiterator.dto.MenuQueryResponse;

/**
 * 菜单查询服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface MenuQueryService {

    /**
     * 查询用户菜单
     *
     * @param request 菜单查询请求
     * @return 菜单查询响应
     */
    MenuQueryResponse queryUserMenu(MenuQueryRequest request);
}
```

### Service 实现类

Service 实现类负责查询菜单记录、构建组合树，并通过迭代器生成扁平节点列表。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/service/impl/MenuQueryServiceImpl.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.compositeiterator.composite.MenuComponent;
import io.github.atengk.pattern.combination.compositeiterator.converter.MenuNodeConverter;
import io.github.atengk.pattern.combination.compositeiterator.dto.MenuNodeVO;
import io.github.atengk.pattern.combination.compositeiterator.dto.MenuQueryRequest;
import io.github.atengk.pattern.combination.compositeiterator.dto.MenuQueryResponse;
import io.github.atengk.pattern.combination.compositeiterator.enums.TraversalTypeEnum;
import io.github.atengk.pattern.combination.compositeiterator.repository.MenuDataRepository;
import io.github.atengk.pattern.combination.compositeiterator.repository.MenuRecord;
import io.github.atengk.pattern.combination.compositeiterator.service.MenuQueryService;
import io.github.atengk.pattern.combination.compositeiterator.tree.MenuTreeBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 菜单查询服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuQueryServiceImpl implements MenuQueryService {

    private final MenuDataRepository menuDataRepository;

    private final MenuTreeBuilder menuTreeBuilder;

    /**
     * 查询用户菜单
     *
     * @param request 菜单查询请求
     * @return 菜单查询响应
     */
    @Override
    public MenuQueryResponse queryUserMenu(MenuQueryRequest request) {
        Boolean includeButton = !Boolean.FALSE.equals(request.getIncludeButton());
        String traversalCode = StrUtil.blankToDefault(
                request.getTraversalType(),
                TraversalTypeEnum.DEPTH_FIRST.getCode()
        );
        TraversalTypeEnum traversalType = TraversalTypeEnum.of(traversalCode);

        List<MenuRecord> records = menuDataRepository.findByUserId(request.getUserId());
        MenuComponent root = menuTreeBuilder.build(records, includeButton);

        Iterator<MenuComponent> iterator = createIterator(root, traversalType);
        List<MenuNodeVO> flatNodes = new ArrayList<>();
        while (iterator.hasNext()) {
            MenuComponent component = iterator.next();
            flatNodes.add(MenuNodeConverter.toFlatVo(component));
        }

        log.info("用户菜单查询完成，用户ID：{}，遍历方式：{}，节点数量：{}",
                request.getUserId(), traversalType.getCode(), flatNodes.size());

        return MenuQueryResponse.builder()
                .traversalType(traversalType.getCode())
                .tree(MenuNodeConverter.toVo(root))
                .flatNodes(flatNodes)
                .totalNodeCount(flatNodes.size())
                .build();
    }

    /**
     * 创建菜单树迭代器
     *
     * @param root          菜单根节点
     * @param traversalType 遍历类型
     * @return 菜单节点迭代器
     */
    private Iterator<MenuComponent> createIterator(MenuComponent root, TraversalTypeEnum traversalType) {
        if (TraversalTypeEnum.BREADTH_FIRST.equals(traversalType)) {
            return root.breadthFirstIterator();
        }
        return root.depthFirstIterator();
    }
}
```

### 统一响应对象

统一响应对象用于包装接口返回结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/result/Result.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应编码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败响应
     *
     * @param code    响应编码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
```

### 全局异常处理器

全局异常处理器用于统一处理参数异常和业务异常。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.compositeiterator.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = ObjectUtil.isNotNull(fieldError)
                ? StrUtil.blankToDefault(fieldError.getDefaultMessage(), "请求参数不合法")
                : "请求参数不合法";

        log.warn("请求参数校验失败：{}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("菜单查询业务异常：{}", exception.getMessage());
        return Result.fail(400, exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
```

### Controller

Controller 提供菜单查询接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/controller/MenuController.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.controller;

import io.github.atengk.pattern.combination.compositeiterator.dto.MenuQueryRequest;
import io.github.atengk.pattern.combination.compositeiterator.dto.MenuQueryResponse;
import io.github.atengk.pattern.combination.compositeiterator.result.Result;
import io.github.atengk.pattern.combination.compositeiterator.service.MenuQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 菜单控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuQueryService menuQueryService;

    /**
     * 查询用户菜单
     *
     * @param request 菜单查询请求
     * @return 菜单查询响应
     */
    @PostMapping("/query")
    public Result<MenuQueryResponse> queryUserMenu(@Valid @RequestBody MenuQueryRequest request) {
        return Result.success(menuQueryService.queryUserMenu(request));
    }
}
```

## 使用方式

启动 Spring Boot 项目后，可以通过菜单查询接口验证组合模式和迭代器模式是否同时生效。

接口信息如下：

```text
请求地址：POST /api/menus/query
Content-Type：application/json
```

深度优先遍历请求示例：

```bash
curl -X POST "http://localhost:8080/api/menus/query" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "traversalType": "DEPTH_FIRST",
    "includeButton": true
  }'
```

深度优先遍历时，节点顺序类似：

```text
系统菜单
工作台
首页
系统管理
用户管理
新增用户
删除用户
角色管理
订单中心
订单列表
导出订单
```

响应示例节选：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "traversalType": "DEPTH_FIRST",
    "totalNodeCount": 11,
    "flatNodes": [
      {
        "id": 0,
        "name": "系统菜单",
        "nodeType": "DIRECTORY",
        "path": "/",
        "permission": "root"
      },
      {
        "id": 1,
        "name": "工作台",
        "nodeType": "DIRECTORY",
        "path": "/dashboard",
        "permission": "dashboard"
      },
      {
        "id": 2,
        "name": "首页",
        "nodeType": "MENU",
        "path": "/dashboard/home",
        "permission": "dashboard:home:view"
      }
    ]
  }
}
```

广度优先遍历请求示例：

```bash
curl -X POST "http://localhost:8080/api/menus/query" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "traversalType": "BREADTH_FIRST",
    "includeButton": true
  }'
```

广度优先遍历时，节点顺序类似：

```text
系统菜单
工作台
系统管理
订单中心
首页
用户管理
角色管理
订单列表
新增用户
删除用户
导出订单
```

不包含按钮节点请求示例：

```bash
curl -X POST "http://localhost:8080/api/menus/query" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "traversalType": "DEPTH_FIRST",
    "includeButton": false
  }'
```

响应结果中不会包含 `BUTTON` 类型节点，例如“新增用户”“删除用户”“导出订单”会被过滤掉。

不支持的遍历方式请求示例：

```bash
curl -X POST "http://localhost:8080/api/menus/query" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "traversalType": "RANDOM",
    "includeButton": true
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "不支持的遍历类型：RANDOM",
  "data": null
}
```

## 新增节点类型

当业务需要新增一种节点类型时，例如新增“外链菜单”，可以在枚举中增加类型，并在构建器中增加转换规则。

第一步，在 `MenuNodeTypeEnum` 中新增枚举：

```java
EXTERNAL_LINK("EXTERNAL_LINK", "外链菜单");
```

第二步，在 `MenuTreeBuilder.convert()` 中把 `EXTERNAL_LINK` 作为叶子节点处理即可：

```java
return new MenuLeaf(
        record.getId(),
        record.getParentId(),
        record.getName(),
        nodeType,
        record.getPath(),
        record.getPermission(),
        record.getSort(),
        record.getVisible()
);
```

如果外链菜单仍然不能包含子节点，就可以复用 `MenuLeaf`。如果外链菜单未来也允许包含子节点，则可以新建一个组合节点类。

## 新增遍历方式

当业务需要新增一种遍历方式时，例如“只遍历可见菜单”，可以新增一个迭代器类，不需要修改组合节点的树形结构。

下面示例新增一个只返回可见节点的深度优先迭代器。

文件位置：`src/main/java/io/github/atengk/pattern/combination/compositeiterator/iterator/VisibleMenuIterator.java`

```java
package io.github.atengk.pattern.combination.compositeiterator.iterator;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.compositeiterator.composite.MenuComponent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 可见菜单迭代器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class VisibleMenuIterator implements Iterator<MenuComponent> {

    private final Deque<MenuComponent> stack = new ArrayDeque<>();

    private MenuComponent nextVisible;

    /**
     * 初始化可见菜单迭代器
     *
     * @param root 根节点
     */
    public VisibleMenuIterator(MenuComponent root) {
        if (ObjectUtil.isNotNull(root)) {
            stack.push(root);
        }
        moveToNextVisible();
    }

    /**
     * 判断是否存在下一个可见节点
     *
     * @return 是否存在下一个可见节点
     */
    @Override
    public boolean hasNext() {
        return ObjectUtil.isNotNull(nextVisible);
    }

    /**
     * 获取下一个可见节点
     *
     * @return 菜单节点
     */
    @Override
    public MenuComponent next() {
        if (!hasNext()) {
            throw new NoSuchElementException("可见菜单已经遍历完成");
        }

        MenuComponent current = nextVisible;
        moveToNextVisible();
        return current;
    }

    /**
     * 移动到下一个可见节点
     */
    private void moveToNextVisible() {
        nextVisible = null;

        while (!stack.isEmpty()) {
            MenuComponent current = stack.pop();
            List<MenuComponent> children = current.getChildren();
            for (int index = children.size() - 1; index >= 0; index--) {
                stack.push(children.get(index));
            }

            if (Boolean.TRUE.equals(current.getVisible())) {
                nextVisible = current;
                return;
            }
        }
    }
}
```

新增迭代器后，只需要在 Service 中增加一个遍历类型分支即可。树形节点对象不需要调整。

## 验证方式

可以从以下几个方面验证组合模式和迭代器模式是否生效：

```text
1. 目录节点可以添加子节点
2. 菜单和按钮节点不能添加子节点
3. 菜单数据可以被构建成统一的 MenuComponent 树
4. 深度优先遍历时，先遍历父节点，再递归遍历子节点
5. 广度优先遍历时，先遍历同层节点，再遍历下一层节点
6. 新增节点类型时，可以复用组合结构
7. 新增遍历方式时，不需要修改已有节点类
```

正常查询日志示例：

```text
查询用户菜单数据，用户ID：1001
菜单组合树构建完成，节点数量：10
用户菜单查询完成，用户ID：1001，遍历方式：DEPTH_FIRST，节点数量：11
```

如果不包含按钮节点，日志示例：

```text
查询用户菜单数据，用户ID：1001
菜单组合树构建完成，节点数量：7
用户菜单查询完成，用户ID：1001，遍历方式：DEPTH_FIRST，节点数量：8
```

这里节点数量多 1，是因为结果中包含了虚拟根节点“系统菜单”。

## 组合效果

组合模式和迭代器模式组合后，各自负责不同变化点：

| 模式    | 职责     | 在示例中的体现                                                      |
| ----- | ------ | ------------------------------------------------------------ |
| 组合模式  | 组织树形结构 | `MenuComponent`、`MenuDirectory`、`MenuLeaf` 统一表达目录、菜单、按钮      |
| 迭代器模式 | 统一遍历结构 | `DepthFirstMenuIterator`、`BreadthFirstMenuIterator` 提供不同遍历方式 |

这种组合适合处理下面两类变化：

```text
第一类变化：节点结构是树形的
例如菜单树、部门树、分类树、权限树、组织架构树

第二类变化：遍历方式会变化
例如深度优先、广度优先、只遍历可见节点、只遍历按钮节点
```

相比只使用组合模式，这个组合可以避免业务层到处写递归遍历逻辑。相比只使用迭代器模式，这个组合可以先用统一对象结构表达树形关系，再在此基础上提供遍历能力。

## 注意事项

组合模式和迭代器模式组合使用时，要明确两者边界：

```text
组合模式负责节点组织
迭代器模式负责节点遍历
```

不要把遍历逻辑写满在业务 Service 中。菜单、部门、分类这类树形结构经常会被多个业务复用，如果每个业务都自己写递归，很容易出现排序不一致、过滤规则不一致、节点遗漏等问题。

也不要让组合节点承担过多业务查询职责。`MenuComponent` 应该表达节点结构和基础行为，数据库查询、权限过滤、数据转换应该放在 Repository、Builder、Converter 或 Service 中。

这个组合最适合的判断标准是：**树形组织归组合，遍历访问归迭代器**。
