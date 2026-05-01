# 设计模式：组合模式

组合模式用于把对象组织成树形结构，让调用方可以用统一方式处理单个对象和对象集合。在 JDK21 和 Spring Boot 3 项目中，组合模式常用于菜单树、权限树、组织架构树、分类树、文件目录树、区域层级、部门员工结构、规则节点树等场景。

需要注意：组合模式关注的是“整体和部分统一处理”。如果只是简化多个子系统调用，更适合外观模式；如果是给对象增强能力，更适合装饰器模式；如果是树形结构中叶子节点和容器节点需要统一对外行为，组合模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证组合模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、ID 等通用处理 -->
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

组合模式的核心目标是让叶子对象和容器对象实现同一个接口，使调用方不需要区分“单个对象”和“对象集合”。

常见角色如下：

| 角色      | 说明                                                         |
| --------- | ------------------------------------------------------------ |
| Component | 抽象组件，定义叶子节点和组合节点的统一行为                   |
| Leaf      | 叶子节点，不能再包含子节点                                   |
| Composite | 组合节点，可以包含多个子节点                                 |
| Client    | 调用方，面向 Component 编程，不关心具体是 Leaf 还是 Composite |

常见实现方式如下：

| 实现方式             | 是否推荐 | 适用场景                                                     |
| -------------------- | -------- | ------------------------------------------------------------ |
| 透明组合模式         | 谨慎使用 | Component 中统一定义 `add`、`remove`，叶子节点不支持时抛异常 |
| 安全组合模式         | 推荐     | 只有组合节点暴露子节点管理方法                               |
| Spring Boot 树构建   | 强烈推荐 | 菜单树、权限树、部门树、分类树                               |
| 直接嵌套 List DTO    | 可用     | 只做数据展示，不需要统一行为                                 |
| 大量递归散落在业务层 | 不推荐   | 递归逻辑分散，维护成本高                                     |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Boot 树组件组合 > 安全组合模式 > 透明组合模式 > 手写分散递归
```

组合模式特别适合树形结构。如果业务对象天然存在父子关系，并且父节点和子节点有一部分相同行为，就可以考虑组合模式。

## 普通 Java 组合模式

普通 Java 组合模式适合不依赖 Spring 容器的树结构处理。下面以文件系统为例，文件是叶子节点，目录是组合节点，文件和目录都可以统一计算大小、打印结构。

整体结构如下：

```text
目录
├── 文件
├── 文件
└── 子目录
    ├── 文件
    └── 文件
```

调用方只面向 `FileSystemComponent`，不需要区分当前对象是文件还是目录。

### 文件结构

```text
src/main/java/io/github/atengk/design/composite/simple/
├── FileSystemComponent.java
├── FileNode.java
└── DirectoryNode.java
```

文件位置：`src/main/java/io/github/atengk/design/composite/simple/FileSystemComponent.java`

下面是文件系统组件接口，文件和目录都实现该接口。

```java
package io.github.atengk.design.composite.simple;

/**
 * 文件系统组件
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface FileSystemComponent {

    /**
     * 获取名称
     *
     * @return 名称
     */
    String name();

    /**
     * 计算大小
     *
     * @return 大小，单位：字节
     */
    long size();

    /**
     * 打印树形结构
     *
     * @param indent 缩进
     * @return 树形结构文本
     */
    String print(String indent);
}
```

文件位置：`src/main/java/io/github/atengk/design/composite/simple/FileNode.java`

下面是文件节点，也就是叶子节点。它没有子节点，只返回自身大小。

```java
package io.github.atengk.design.composite.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件节点
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class FileNode implements FileSystemComponent {

    private final String name;
    private final long size;

    /**
     * 创建文件节点
     *
     * @param name 文件名
     * @param size 文件大小
     */
    public FileNode(String name, long size) {
        if (StrUtil.isBlank(name)) {
            log.warn("创建文件节点失败，文件名为空");
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (size < 0) {
            log.warn("创建文件节点失败，文件大小不合法，文件名：{}，大小：{}", name, size);
            throw new IllegalArgumentException("文件大小不能小于0");
        }

        this.name = name;
        this.size = size;
    }

    /**
     * 获取名称
     *
     * @return 名称
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 计算大小
     *
     * @return 文件大小
     */
    @Override
    public long size() {
        return size;
    }

    /**
     * 打印树形结构
     *
     * @param indent 缩进
     * @return 树形结构文本
     */
    @Override
    public String print(String indent) {
        return StrUtil.format("{}- {} ({} bytes)", indent, name, size);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/composite/simple/DirectoryNode.java`

下面是目录节点，也就是组合节点。它可以包含文件节点或其他目录节点。

```java
package io.github.atengk.design.composite.simple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 目录节点
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class DirectoryNode implements FileSystemComponent {

    private final String name;
    private final List<FileSystemComponent> children = new ArrayList<>();

    /**
     * 创建目录节点
     *
     * @param name 目录名
     */
    public DirectoryNode(String name) {
        if (StrUtil.isBlank(name)) {
            log.warn("创建目录节点失败，目录名为空");
            throw new IllegalArgumentException("目录名不能为空");
        }

        this.name = name;
    }

    /**
     * 添加子节点
     *
     * @param component 文件系统组件
     */
    public void add(FileSystemComponent component) {
        if (component == null) {
            log.warn("添加目录子节点失败，子节点为空，目录：{}", name);
            throw new IllegalArgumentException("子节点不能为空");
        }

        children.add(component);
        log.info("添加目录子节点成功，目录：{}，子节点：{}，子节点数量：{}", name, component.name(), children.size());
    }

    /**
     * 移除子节点
     *
     * @param component 文件系统组件
     */
    public void remove(FileSystemComponent component) {
        if (component == null) {
            return;
        }

        children.remove(component);
        log.info("移除目录子节点成功，目录：{}，子节点：{}，子节点数量：{}", name, component.name(), children.size());
    }

    /**
     * 获取名称
     *
     * @return 名称
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 计算大小
     *
     * @return 目录总大小
     */
    @Override
    public long size() {
        if (CollUtil.isEmpty(children)) {
            return 0L;
        }

        return children.stream()
                .mapToLong(FileSystemComponent::size)
                .sum();
    }

    /**
     * 打印树形结构
     *
     * @param indent 缩进
     * @return 树形结构文本
     */
    @Override
    public String print(String indent) {
        StringBuilder builder = new StringBuilder();
        builder.append(StrUtil.format("{}+ {} ({} bytes)", indent, name, size()));

        for (FileSystemComponent child : children) {
            builder.append(System.lineSeparator())
                    .append(child.print(indent + "  "));
        }

        return builder.toString();
    }
}
```

使用方式：

```java
DirectoryNode root = new DirectoryNode("project");

root.add(new FileNode("pom.xml", 2048));
root.add(new FileNode("README.md", 1024));

DirectoryNode src = new DirectoryNode("src");
src.add(new FileNode("Application.java", 4096));
src.add(new FileNode("UserController.java", 8192));

root.add(src);

long totalSize = root.size();
String treeText = root.print("");
```

可能输出：

```text
+ project (15360 bytes)
  - pom.xml (2048 bytes)
  - README.md (1024 bytes)
  + src (12288 bytes)
    - Application.java (4096 bytes)
    - UserController.java (8192 bytes)
```

这里的 `root`、`src`、`FileNode` 都可以通过 `FileSystemComponent` 统一处理。调用方不需要关心当前对象是单个文件还是目录。

## Spring Boot 组合模式

Spring Boot 项目中更常见的组合模式，是把数据库中的扁平数据组装成树形结构，并对叶子节点和组合节点提供统一处理能力。下面以权限资源树为例，目录和菜单可以包含子资源，按钮是叶子资源。

资源类型如下：

```text
DIRECTORY 目录
MENU      菜单
BUTTON    按钮
```

权限资源树示例：

```text
系统管理
├── 用户管理
│   ├── 新增用户
│   └── 删除用户
└── 角色管理
    ├── 新增角色
    └── 分配权限
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── CompositeApplication.java
├── component/
│   ├── PermissionComponent.java
│   ├── PermissionComposite.java
│   └── PermissionLeaf.java
├── controller/
│   └── PermissionController.java
├── dto/
│   ├── PermissionResourceDefinition.java
│   └── PermissionResourceResponse.java
├── enums/
│   └── PermissionResourceType.java
└── service/
    ├── PermissionTreeService.java
    └── impl/
        └── PermissionTreeServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/CompositeApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 组合模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class CompositeApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CompositeApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/enums/PermissionResourceType.java`

下面是权限资源类型枚举。

```java
package io.github.atengk.design.enums;

/**
 * 权限资源类型
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum PermissionResourceType {

    /**
     * 目录
     */
    DIRECTORY,

    /**
     * 菜单
     */
    MENU,

    /**
     * 按钮
     */
    BUTTON
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PermissionResourceDefinition.java`

下面是权限资源定义对象，用于模拟数据库中的扁平权限数据。

```java
package io.github.atengk.design.dto;

import io.github.atengk.design.enums.PermissionResourceType;

/**
 * 权限资源定义
 *
 * @param code       资源编码
 * @param parentCode 父资源编码
 * @param name       资源名称
 * @param type       资源类型
 * @param sortNo     排序号
 * @author Ateng
 * @since 2026-04-30
 */
public record PermissionResourceDefinition(
        String code,
        String parentCode,
        String name,
        PermissionResourceType type,
        Integer sortNo
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PermissionResourceResponse.java`

下面是权限资源响应对象，用于返回树形结构。

```java
package io.github.atengk.design.dto;

import io.github.atengk.design.enums.PermissionResourceType;

import java.util.List;

/**
 * 权限资源响应
 *
 * @param code     资源编码
 * @param name     资源名称
 * @param type     资源类型
 * @param sortNo   排序号
 * @param children 子资源列表
 * @author Ateng
 * @since 2026-04-30
 */
public record PermissionResourceResponse(
        String code,
        String name,
        PermissionResourceType type,
        Integer sortNo,
        List<PermissionResourceResponse> children
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/component/PermissionComponent.java`

下面是权限组件接口。目录、菜单、按钮都通过该接口统一处理。

```java
package io.github.atengk.design.component;

import io.github.atengk.design.dto.PermissionResourceResponse;
import io.github.atengk.design.enums.PermissionResourceType;

import java.util.List;
import java.util.Set;

/**
 * 权限组件
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PermissionComponent {

    /**
     * 获取资源编码
     *
     * @return 资源编码
     */
    String code();

    /**
     * 获取资源名称
     *
     * @return 资源名称
     */
    String name();

    /**
     * 获取资源类型
     *
     * @return 资源类型
     */
    PermissionResourceType type();

    /**
     * 获取排序号
     *
     * @return 排序号
     */
    Integer sortNo();

    /**
     * 采集权限编码
     *
     * @return 权限编码集合
     */
    Set<String> collectCodes();

    /**
     * 转换为响应对象
     *
     * @return 权限资源响应
     */
    PermissionResourceResponse toResponse();

    /**
     * 获取子节点
     *
     * @return 子节点列表
     */
    default List<PermissionComponent> children() {
        return List.of();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/component/PermissionLeaf.java`

下面是权限叶子节点。按钮通常是叶子节点，不再包含子资源。

```java
package io.github.atengk.design.component;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PermissionResourceResponse;
import io.github.atengk.design.enums.PermissionResourceType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * 权限叶子节点
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class PermissionLeaf implements PermissionComponent {

    private final String code;
    private final String name;
    private final PermissionResourceType type;
    private final Integer sortNo;

    /**
     * 创建权限叶子节点
     *
     * @param code   资源编码
     * @param name   资源名称
     * @param type   资源类型
     * @param sortNo 排序号
     */
    public PermissionLeaf(String code, String name, PermissionResourceType type, Integer sortNo) {
        if (StrUtil.hasBlank(code, name)) {
            log.warn("创建权限叶子节点失败，资源编码或名称为空");
            throw new IllegalArgumentException("资源编码和名称不能为空");
        }

        this.code = code;
        this.name = name;
        this.type = type;
        this.sortNo = sortNo == null ? 0 : sortNo;
    }

    /**
     * 获取资源编码
     *
     * @return 资源编码
     */
    @Override
    public String code() {
        return code;
    }

    /**
     * 获取资源名称
     *
     * @return 资源名称
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 获取资源类型
     *
     * @return 资源类型
     */
    @Override
    public PermissionResourceType type() {
        return type;
    }

    /**
     * 获取排序号
     *
     * @return 排序号
     */
    @Override
    public Integer sortNo() {
        return sortNo;
    }

    /**
     * 采集权限编码
     *
     * @return 权限编码集合
     */
    @Override
    public Set<String> collectCodes() {
        return Set.of(code);
    }

    /**
     * 转换为响应对象
     *
     * @return 权限资源响应
     */
    @Override
    public PermissionResourceResponse toResponse() {
        return new PermissionResourceResponse(code, name, type, sortNo, List.of());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/component/PermissionComposite.java`

下面是权限组合节点。目录和菜单可以包含子节点，并且可以递归采集子节点权限编码。

```java
package io.github.atengk.design.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PermissionResourceResponse;
import io.github.atengk.design.enums.PermissionResourceType;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 权限组合节点
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class PermissionComposite implements PermissionComponent {

    private final String code;
    private final String name;
    private final PermissionResourceType type;
    private final Integer sortNo;
    private final List<PermissionComponent> children = new CopyOnWriteArrayList<>();

    /**
     * 创建权限组合节点
     *
     * @param code   资源编码
     * @param name   资源名称
     * @param type   资源类型
     * @param sortNo 排序号
     */
    public PermissionComposite(String code, String name, PermissionResourceType type, Integer sortNo) {
        if (StrUtil.hasBlank(code, name)) {
            log.warn("创建权限组合节点失败，资源编码或名称为空");
            throw new IllegalArgumentException("资源编码和名称不能为空");
        }

        this.code = code;
        this.name = name;
        this.type = type;
        this.sortNo = sortNo == null ? 0 : sortNo;
    }

    /**
     * 添加子节点
     *
     * @param component 权限组件
     */
    public void add(PermissionComponent component) {
        if (component == null) {
            log.warn("添加权限子节点失败，子节点为空，父节点：{}", code);
            throw new IllegalArgumentException("子节点不能为空");
        }

        children.add(component);
        log.info("添加权限子节点成功，父节点：{}，子节点：{}", code, component.code());
    }

    /**
     * 获取资源编码
     *
     * @return 资源编码
     */
    @Override
    public String code() {
        return code;
    }

    /**
     * 获取资源名称
     *
     * @return 资源名称
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 获取资源类型
     *
     * @return 资源类型
     */
    @Override
    public PermissionResourceType type() {
        return type;
    }

    /**
     * 获取排序号
     *
     * @return 排序号
     */
    @Override
    public Integer sortNo() {
        return sortNo;
    }

    /**
     * 获取子节点
     *
     * @return 子节点列表
     */
    @Override
    public List<PermissionComponent> children() {
        return children.stream()
                .sorted(Comparator.comparing(PermissionComponent::sortNo))
                .toList();
    }

    /**
     * 采集权限编码
     *
     * @return 权限编码集合
     */
    @Override
    public Set<String> collectCodes() {
        Set<String> codes = new LinkedHashSet<>();
        codes.add(code);

        if (CollUtil.isNotEmpty(children)) {
            for (PermissionComponent child : children()) {
                codes.addAll(child.collectCodes());
            }
        }

        return codes;
    }

    /**
     * 转换为响应对象
     *
     * @return 权限资源响应
     */
    @Override
    public PermissionResourceResponse toResponse() {
        List<PermissionResourceResponse> childResponses = children().stream()
                .map(PermissionComponent::toResponse)
                .toList();

        return new PermissionResourceResponse(code, name, type, sortNo, childResponses);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/PermissionTreeService.java`

下面是权限树服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.PermissionResourceResponse;

import java.util.List;
import java.util.Set;

/**
 * 权限树服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PermissionTreeService {

    /**
     * 获取权限资源树
     *
     * @return 权限资源树
     */
    List<PermissionResourceResponse> listTree();

    /**
     * 获取全部权限编码
     *
     * @return 权限编码集合
     */
    Set<String> listPermissionCodes();
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/PermissionTreeServiceImpl.java`

下面是权限树服务实现。它先模拟一组扁平权限资源，再组装成组合树。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.component.PermissionComponent;
import io.github.atengk.design.component.PermissionComposite;
import io.github.atengk.design.component.PermissionLeaf;
import io.github.atengk.design.dto.PermissionResourceDefinition;
import io.github.atengk.design.dto.PermissionResourceResponse;
import io.github.atengk.design.enums.PermissionResourceType;
import io.github.atengk.design.service.PermissionTreeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 权限树服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class PermissionTreeServiceImpl implements PermissionTreeService {

    /**
     * 获取权限资源树
     *
     * @return 权限资源树
     */
    @Override
    public List<PermissionResourceResponse> listTree() {
        List<PermissionComponent> rootComponents = buildPermissionTree();

        List<PermissionResourceResponse> responses = rootComponents.stream()
                .map(PermissionComponent::toResponse)
                .toList();

        log.info("获取权限资源树成功，根节点数量：{}", responses.size());
        return responses;
    }

    /**
     * 获取全部权限编码
     *
     * @return 权限编码集合
     */
    @Override
    public Set<String> listPermissionCodes() {
        Set<String> codes = new LinkedHashSet<>();

        for (PermissionComponent component : buildPermissionTree()) {
            codes.addAll(component.collectCodes());
        }

        log.info("采集权限编码成功，权限数量：{}", codes.size());
        return codes;
    }

    /**
     * 构建权限树
     *
     * @return 权限组件根节点列表
     */
    private List<PermissionComponent> buildPermissionTree() {
        List<PermissionResourceDefinition> definitions = mockDefinitions();

        Map<String, PermissionComponent> componentMap = definitions.stream()
                .collect(Collectors.toMap(
                        PermissionResourceDefinition::code,
                        this::createComponent,
                        (first, second) -> first
                ));

        for (PermissionResourceDefinition definition : definitions) {
            if (StrUtil.isBlank(definition.parentCode())) {
                continue;
            }

            PermissionComponent parent = componentMap.get(definition.parentCode());
            PermissionComponent current = componentMap.get(definition.code());

            if (parent instanceof PermissionComposite composite) {
                composite.add(current);
            } else {
                log.warn("权限树构建异常，父节点不是组合节点，父节点：{}，当前节点：{}",
                        definition.parentCode(), definition.code());
            }
        }

        List<PermissionComponent> roots = definitions.stream()
                .filter(definition -> StrUtil.isBlank(definition.parentCode()))
                .map(definition -> componentMap.get(definition.code()))
                .sorted(Comparator.comparing(PermissionComponent::sortNo))
                .toList();

        if (CollUtil.isEmpty(roots)) {
            log.warn("权限树根节点为空");
        }

        return roots;
    }

    /**
     * 创建权限组件
     *
     * @param definition 权限资源定义
     * @return 权限组件
     */
    private PermissionComponent createComponent(PermissionResourceDefinition definition) {
        if (definition.type() == PermissionResourceType.BUTTON) {
            return new PermissionLeaf(definition.code(), definition.name(), definition.type(), definition.sortNo());
        }

        return new PermissionComposite(definition.code(), definition.name(), definition.type(), definition.sortNo());
    }

    /**
     * 模拟权限资源定义
     *
     * @return 权限资源定义列表
     */
    private List<PermissionResourceDefinition> mockDefinitions() {
        return List.of(
                new PermissionResourceDefinition("system", null, "系统管理", PermissionResourceType.DIRECTORY, 100),
                new PermissionResourceDefinition("system:user", "system", "用户管理", PermissionResourceType.MENU, 110),
                new PermissionResourceDefinition("system:user:add", "system:user", "新增用户", PermissionResourceType.BUTTON, 111),
                new PermissionResourceDefinition("system:user:delete", "system:user", "删除用户", PermissionResourceType.BUTTON, 112),
                new PermissionResourceDefinition("system:role", "system", "角色管理", PermissionResourceType.MENU, 120),
                new PermissionResourceDefinition("system:role:add", "system:role", "新增角色", PermissionResourceType.BUTTON, 121),
                new PermissionResourceDefinition("system:role:assign", "system:role", "分配权限", PermissionResourceType.BUTTON, 122),
                new PermissionResourceDefinition("monitor", null, "系统监控", PermissionResourceType.DIRECTORY, 200),
                new PermissionResourceDefinition("monitor:log", "monitor", "日志管理", PermissionResourceType.MENU, 210),
                new PermissionResourceDefinition("monitor:log:query", "monitor:log", "查询日志", PermissionResourceType.BUTTON, 211)
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/PermissionController.java`

下面是权限资源接口，用于验证组合模式构建树和采集权限编码的效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.PermissionResourceResponse;
import io.github.atengk.design.service.PermissionTreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 权限资源控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/composite/permission")
public class PermissionController {

    private final PermissionTreeService permissionTreeService;

    /**
     * 获取权限资源树
     *
     * @return 权限资源树
     */
    @GetMapping("/tree")
    public List<PermissionResourceResponse> listTree() {
        return permissionTreeService.listTree();
    }

    /**
     * 获取全部权限编码
     *
     * @return 权限编码集合
     */
    @GetMapping("/codes")
    public Set<String> listPermissionCodes() {
        return permissionTreeService.listPermissionCodes();
    }
}
```

接口调用示例：

```bash
curl "http://localhost:8080/composite/permission/tree"

curl "http://localhost:8080/composite/permission/codes"
```

权限树可能返回：

```json
[
  {
    "code": "system",
    "name": "系统管理",
    "type": "DIRECTORY",
    "sortNo": 100,
    "children": [
      {
        "code": "system:user",
        "name": "用户管理",
        "type": "MENU",
        "sortNo": 110,
        "children": [
          {
            "code": "system:user:add",
            "name": "新增用户",
            "type": "BUTTON",
            "sortNo": 111,
            "children": []
          }
        ]
      }
    ]
  }
]
```

权限编码接口会递归采集所有目录、菜单和按钮编码：

```json
[
  "system",
  "system:user",
  "system:user:add",
  "system:user:delete",
  "system:role",
  "system:role:add",
  "system:role:assign",
  "monitor",
  "monitor:log",
  "monitor:log:query"
]
```

## 扩展一个新节点类型

在组合模式中，扩展新节点类型通常需要先确认它是否可以包含子节点。如果可以包含子节点，就实现为组合节点；如果不能包含子节点，就实现为叶子节点。

例如新增 `API` 类型，用于表示后端接口权限。

文件位置：`src/main/java/io/github/atengk/design/enums/PermissionResourceType.java`

在枚举中新增：

```java
API
```

如果 `API` 是叶子节点，可以调整 `createComponent` 方法：

```java
private PermissionComponent createComponent(PermissionResourceDefinition definition) {
    if (definition.type() == PermissionResourceType.BUTTON || definition.type() == PermissionResourceType.API) {
        return new PermissionLeaf(definition.code(), definition.name(), definition.type(), definition.sortNo());
    }

    return new PermissionComposite(definition.code(), definition.name(), definition.type(), definition.sortNo());
}
```

然后新增模拟数据：

```java
new PermissionResourceDefinition("system:user:api:list", "system:user", "用户分页接口", PermissionResourceType.API, 113)
```

如果未来 `API_GROUP` 可以包含多个 `API`，则 `API_GROUP` 应该使用 `PermissionComposite`，`API` 使用 `PermissionLeaf`。

## 透明组合模式和安全组合模式

组合模式常见两种写法：透明组合模式和安全组合模式。二者主要区别在于 `add`、`remove` 等子节点管理方法放在哪里。

透明组合模式把子节点管理方法放在统一接口中。

```java
public interface Component {
    void add(Component component);
    void remove(Component component);
}
```

这种方式的优点是调用方完全统一，缺点是叶子节点也暴露了 `add`、`remove`，但叶子节点实际上不支持这些操作，通常只能抛异常。

安全组合模式只在组合节点中提供 `add`、`remove`。

```java
public class PermissionComposite implements PermissionComponent {

    public void add(PermissionComponent component) {
        // 添加子节点
    }
}
```

这种方式类型更安全，叶子节点不会暴露无意义方法。本文示例采用的是安全组合模式。

在 Spring Boot 业务项目中，更推荐安全组合模式。尤其是菜单、权限、组织架构这类业务树，叶子节点不应该暴露添加子节点的能力。

## 组合模式和装饰器模式的区别

组合模式和装饰器模式都可能包含对象引用，但目的不同。

| 对比项   | 组合模式                       | 装饰器模式                     |
| -------- | ------------------------------ | ------------------------------ |
| 核心目的 | 表达整体和部分的树形关系       | 给对象叠加增强能力             |
| 结构关系 | 一对多，父节点包含多个子节点   | 一对一，装饰器包装一个目标对象 |
| 关注点   | 统一处理叶子节点和组合节点     | 保持接口不变并增强行为         |
| 典型场景 | 菜单树、权限树、目录树、组织树 | 缓存增强、审计增强、限流增强   |
| 是否递归 | 通常递归处理                   | 通常链式包装                   |

简单理解：

```text
组合模式：一个对象里面包含多个同类对象，形成树。
装饰器模式：一个对象外面包一层增强对象。
```

权限树、文件目录树适合组合模式。订单服务外面包审计、幂等、限流，适合装饰器模式。

## 组合模式和外观模式的区别

组合模式和外观模式都可以隐藏复杂性，但关注点不同。

| 对比项           | 组合模式                       | 外观模式                     |
| ---------------- | ------------------------------ | ---------------------------- |
| 核心目的         | 统一处理树形结构中的整体和部分 | 简化多个子系统的调用         |
| 对象结构         | 树形结构                       | 多个子系统组合调用           |
| 调用方式         | 递归或统一接口处理节点         | 调用一个门面入口完成流程     |
| 典型场景         | 菜单树、分类树、目录树         | 下单流程、报表导出、支付聚合 |
| 是否强调父子关系 | 强调                           | 不强调                       |

简单理解：

```text
组合模式：处理树。
外观模式：包一层简单入口。
```

菜单、权限、组织架构这类父子结构适合组合模式。下单时统一调用用户、库存、订单、支付、通知多个子系统，更适合外观模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行获取权限树接口：

```bash
curl "http://localhost:8080/composite/permission/tree"
```

执行获取权限编码接口：

```bash
curl "http://localhost:8080/composite/permission/codes"
```

如果组合模式正常，可以看到类似日志：

```text
添加权限子节点成功，父节点：system，子节点：system:user
添加权限子节点成功，父节点：system:user，子节点：system:user:add
添加权限子节点成功，父节点：system:user，子节点：system:user:delete
添加权限子节点成功，父节点：system，子节点：system:role
获取权限资源树成功，根节点数量：2
采集权限编码成功，权限数量：10
```

如果模拟数据中出现父节点不存在，建议在构建树时记录异常数据。

示例日志：

```text
权限树构建异常，父节点不存在，父节点：unknown，当前节点：system:user:add
```

实际项目中可以将这种数据作为脏数据处理，或者在后台管理保存权限资源时就校验父节点合法性。

## 注意事项

组合模式适合树形结构，但不建议为了使用设计模式强行把普通列表改造成树。如果业务对象没有稳定父子关系，组合模式反而会增加复杂度。

适合使用组合模式的场景：

```text
菜单树
权限树
部门树
分类树
目录树
区域树
规则节点树
组织架构树
```

不太适合使用组合模式的场景：

```text
普通分页列表
无父子关系的数据集合
只需要简单分组的统计结果
没有统一行为的对象集合
```

构建树时要注意循环引用问题。例如 A 的父节点是 B，B 的父节点又是 A，会导致递归异常或死循环。

错误数据示例：

```text
A.parentCode = B
B.parentCode = A
```

实际项目中建议在保存节点时校验：

```text
父节点必须存在
父节点不能是自己
不能形成祖先循环
叶子节点不能添加子节点
同一父节点下编码不能重复
```

组合节点中不要无限制递归。对于层级很深的树，递归可能带来性能问题或栈深度问题。业务上通常应限制最大层级。

示例限制：

```text
菜单树最大 4 级
部门树最大 10 级
分类树最大 5 级
```

如果树数据来自数据库，生产环境中建议先查询扁平列表，再在内存中构建树，而不是每个节点递归查询数据库。

不推荐：

```java
public List<Node> listChildren(String parentCode) {
    // 每个节点都查一次数据库，层级深时会产生大量 SQL
}
```

推荐：

```java
List<Node> allNodes = nodeMapper.selectList(...);
Map<String, List<Node>> groupByParent = allNodes.stream()
        .collect(Collectors.groupingBy(Node::getParentCode));
```

Spring Bean 默认是单例，不要在组件构建服务中保存请求级树数据到成员变量。

错误示例：

```java
private List<PermissionComponent> currentTree;
private Set<String> currentCodes;
```

推荐使用局部变量：

```java
public List<PermissionResourceResponse> listTree() {
    List<PermissionComponent> rootComponents = buildPermissionTree();
    return rootComponents.stream().map(PermissionComponent::toResponse).toList();
}
```

如果树形结构需要频繁查询且数据变化不频繁，可以结合 Redis 或本地缓存。但需要在菜单、权限、分类变更时及时清理缓存。

## 总结

在 JDK21 和 Spring Boot 3 项目中，组合模式的实践重点是用统一接口处理树形结构中的叶子节点和组合节点。

普通 Java 组合模式适合文件目录、规则节点、本地树结构处理。Spring Boot 项目中更常见的是菜单树、权限树、部门树、分类树等业务结构。推荐使用“组件接口 + 叶子节点 + 组合节点 + 树构建服务”的结构，让调用方统一处理节点，不直接关心节点是单个对象还是对象集合。

组合模式不是为了替代所有列表处理，而是为了解决“整体和部分具有一致行为，并且对象天然形成树形结构”的问题。实际落地时，需要重点关注循环引用、层级深度、树构建性能、缓存一致性和节点职责边界。
