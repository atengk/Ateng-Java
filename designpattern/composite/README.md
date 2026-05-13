# 组合模式

组合模式属于结构型模式，核心作用是用统一方式处理树形结构中的叶子节点和容器节点。在当前设计模式文档体系中，组合模式位于结构型模式分类下，适合菜单树、部门树、权限树、分类树、组织架构、文件目录等业务场景。

本文以 **JDK21 + Spring Boot 3** 后端项目为背景，通过“后台权限资源树”的示例，说明组合模式在真实项目中的落地方式。

## 基础配置

本示例模拟一个后台管理系统的权限资源模块。系统中存在三类权限资源：

```text
目录：可以包含菜单或子目录
菜单：可以包含按钮权限
按钮：叶子节点，不能再包含子节点
```

如果不用组合模式，代码中通常会出现大量类型判断：

```java
if (resourceType == CATALOG) {
    // 处理目录
} else if (resourceType == MENU) {
    // 处理菜单
} else if (resourceType == BUTTON) {
    // 处理按钮
}
```

当树形结构越来越复杂时，新增节点类型、统计权限编码、构建前端菜单树、校验节点关系都会变得混乱。

组合模式的处理方式是：把目录、菜单、按钮都抽象成统一的 `AuthResourceComponent`，调用方只面向统一接口处理，不直接关心当前节点是容器节点还是叶子节点。

本示例需要以下依赖。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验接口请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：用于集合、字符串、对象判断等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖：用于单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

示例项目配置如下。

```yaml
server:
  port: 8080 # 示例服务端口
```

本示例的核心文件结构如下。

```text
src/main/java/io/github/atengk/designpattern/composite
├── CompositeApplication.java
├── controller
│   └── AuthResourceController.java
├── dto
│   └── AuthResourceCreateRequest.java
├── enums
│   └── AuthResourceType.java
├── composite
│   ├── AuthResourceComponent.java
│   ├── AbstractAuthResourceComponent.java
│   ├── AuthResourceComposite.java
│   ├── AuthResourceLeaf.java
│   └── AuthResourceTreeFactory.java
├── repository
│   └── AuthResourceMemoryRepository.java
├── service
│   ├── AuthResourceService.java
│   └── AuthResourceServiceImpl.java
├── vo
│   ├── ApiResult.java
│   └── AuthResourceTreeVO.java
└── web
    └── GlobalExceptionHandler.java
```

## 模式设计

组合模式适合处理“整体和部分具有一致操作”的树形结构。在权限资源场景中，后台系统可能需要对整棵权限树做这些操作：

```text
构建权限树
统计所有权限编码
查找某个资源节点
计算节点总数
转换成前端树结构
```

无论当前节点是目录、菜单还是按钮，调用方都希望用统一方式处理。

本示例中的角色分工如下。

| 角色       | 示例类                          | 说明                           |
| ---------- | ------------------------------- | ------------------------------ |
| 抽象组件   | `AuthResourceComponent`         | 定义目录、菜单、按钮的统一操作 |
| 抽象基础类 | `AbstractAuthResourceComponent` | 保存节点公共属性和通用逻辑     |
| 容器节点   | `AuthResourceComposite`         | 表示目录或菜单，可以包含子节点 |
| 叶子节点   | `AuthResourceLeaf`              | 表示按钮权限，不能包含子节点   |
| 构建工厂   | `AuthResourceTreeFactory`       | 把扁平数据转换成组合树         |
| 调用方     | `AuthResourceServiceImpl`       | 面向统一组件接口处理树形结构   |

核心流程如下。

```text
Controller
    ↓
AuthResourceService
    ↓
AuthResourceMemoryRepository 查询扁平权限数据
    ↓
AuthResourceTreeFactory 构建组合树
    ↓
AuthResourceComponent 统一处理目录、菜单、按钮
    ↓
返回前端树结构或权限编码列表
```

组合模式的关键不是“递归”本身，而是让递归处理时不再散落大量节点类型判断。

## 核心代码

下面给出组合模式在 Spring Boot 项目中的关键实现。示例使用内存仓储模拟数据库数据，真实项目中可以替换为 MyBatis-Plus、JPA 或远程权限中心。

项目启动类负责启动 Spring Boot 应用。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/CompositeApplication.java`

```java
package io.github.atengk.designpattern.composite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 组合模式示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-13
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

权限资源类型枚举用于区分目录、菜单和按钮，并提供字符串解析能力。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/enums/AuthResourceType.java`

```java
package io.github.atengk.designpattern.composite.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

/**
 * 权限资源类型
 *
 * @author Ateng
 * @since 2026-05-13
 */
public enum AuthResourceType {

    /**
     * 目录节点，可以包含目录或菜单
     */
    CATALOG,

    /**
     * 菜单节点，可以包含按钮权限
     */
    MENU,

    /**
     * 按钮节点，叶子节点
     */
    BUTTON;

    /**
     * 根据类型编码解析资源类型
     *
     * @param type 类型编码
     * @return 权限资源类型
     */
    public static AuthResourceType parse(String type) {
        if (StrUtil.isBlank(type)) {
            throw new IllegalArgumentException("权限资源类型不能为空");
        }

        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.name(), type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的权限资源类型：{}", type)));
    }

    /**
     * 判断是否为叶子节点
     *
     * @return 是否为叶子节点
     */
    public boolean isLeaf() {
        return this == BUTTON;
    }
}
```

创建权限资源请求 DTO 用于模拟新增权限节点。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/dto/AuthResourceCreateRequest.java`

```java
package io.github.atengk.designpattern.composite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建权限资源请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class AuthResourceCreateRequest {

    /**
     * 资源名称
     */
    @NotBlank(message = "资源名称不能为空")
    private String name;

    /**
     * 资源类型：CATALOG、MENU、BUTTON
     */
    @NotBlank(message = "资源类型不能为空")
    private String type;

    /**
     * 父级资源 ID，根节点可为空
     */
    private String parentId;

    /**
     * 权限编码，按钮节点通常必填
     */
    private String permissionCode;

    /**
     * 排序值
     */
    @NotNull(message = "排序值不能为空")
    private Integer sort;
}
```

权限树返回 VO 用于向前端返回统一树结构。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/vo/AuthResourceTreeVO.java`

```java
package io.github.atengk.designpattern.composite.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 权限资源树节点返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class AuthResourceTreeVO {

    /**
     * 资源 ID
     */
    private String id;

    /**
     * 父级资源 ID
     */
    private String parentId;

    /**
     * 资源名称
     */
    private String name;

    /**
     * 资源类型
     */
    private String type;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 子节点
     */
    private List<AuthResourceTreeVO> children;
}
```

统一 API 返回对象用于包装接口响应。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/vo/ApiResult.java`

```java
package io.github.atengk.designpattern.composite.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 统一返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 业务状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @return API 返回对象
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败返回
     *
     * @param message 失败消息
     * @return API 返回对象
     */
    public static ApiResult<Void> fail(String message) {
        return ApiResult.<Void>builder()
                .code(500)
                .message(message)
                .build();
    }
}
```

`AuthResourceComponent` 是组合模式中的抽象组件，目录、菜单、按钮都实现这一套统一接口。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/composite/AuthResourceComponent.java`

```java
package io.github.atengk.designpattern.composite.composite;

import io.github.atengk.designpattern.composite.enums.AuthResourceType;
import io.github.atengk.designpattern.composite.vo.AuthResourceTreeVO;

import java.util.List;

/**
 * 权限资源组件接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface AuthResourceComponent {

    /**
     * 获取资源 ID
     *
     * @return 资源 ID
     */
    String getId();

    /**
     * 获取父级资源 ID
     *
     * @return 父级资源 ID
     */
    String getParentId();

    /**
     * 获取资源名称
     *
     * @return 资源名称
     */
    String getName();

    /**
     * 获取资源类型
     *
     * @return 资源类型
     */
    AuthResourceType getType();

    /**
     * 获取权限编码
     *
     * @return 权限编码
     */
    String getPermissionCode();

    /**
     * 获取排序值
     *
     * @return 排序值
     */
    Integer getSort();

    /**
     * 添加子节点
     *
     * @param child 子节点
     */
    void addChild(AuthResourceComponent child);

    /**
     * 移除子节点
     *
     * @param childId 子节点 ID
     */
    void removeChild(String childId);

    /**
     * 获取子节点列表
     *
     * @return 子节点列表
     */
    List<AuthResourceComponent> getChildren();

    /**
     * 获取当前节点及子节点中的所有权限编码
     *
     * @return 权限编码列表
     */
    List<String> getPermissionCodes();

    /**
     * 统计当前节点及子节点总数
     *
     * @return 节点总数
     */
    int count();

    /**
     * 转换成前端树节点
     *
     * @return 权限资源树节点
     */
    AuthResourceTreeVO toTreeVO();
}
```

抽象基础类保存公共字段，并提供通用的节点信息。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/composite/AbstractAuthResourceComponent.java`

```java
package io.github.atengk.designpattern.composite.composite;

import io.github.atengk.designpattern.composite.enums.AuthResourceType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 权限资源抽象组件
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public abstract class AbstractAuthResourceComponent implements AuthResourceComponent {

    /**
     * 资源 ID
     */
    private final String id;

    /**
     * 父级资源 ID
     */
    private final String parentId;

    /**
     * 资源名称
     */
    private final String name;

    /**
     * 资源类型
     */
    private final AuthResourceType type;

    /**
     * 权限编码
     */
    private final String permissionCode;

    /**
     * 排序值
     */
    private final Integer sort;
}
```

容器节点表示目录或菜单，可以包含子节点。目录下面可以挂目录或菜单，菜单下面可以挂按钮。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/composite/AuthResourceComposite.java`

```java
package io.github.atengk.designpattern.composite.composite;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.composite.enums.AuthResourceType;
import io.github.atengk.designpattern.composite.vo.AuthResourceTreeVO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 权限资源容器节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class AuthResourceComposite extends AbstractAuthResourceComponent {

    private final List<AuthResourceComponent> children = new ArrayList<>();

    /**
     * 创建权限资源容器节点
     *
     * @param id             资源 ID
     * @param parentId       父级资源 ID
     * @param name           资源名称
     * @param type           资源类型
     * @param permissionCode 权限编码
     * @param sort           排序值
     */
    public AuthResourceComposite(String id, String parentId, String name, AuthResourceType type, String permissionCode, Integer sort) {
        super(id, parentId, name, type, permissionCode, sort);
        if (type.isLeaf()) {
            throw new IllegalArgumentException("容器节点不能使用叶子类型：" + type);
        }
    }

    /**
     * 添加子节点
     *
     * @param child 子节点
     */
    @Override
    public void addChild(AuthResourceComponent child) {
        if (child == null) {
            throw new IllegalArgumentException("子节点不能为空");
        }

        children.add(child);
        children.sort(Comparator.comparing(AuthResourceComponent::getSort));
        log.info("权限资源节点添加成功，parentId={}，childId={}，childName={}", getId(), child.getId(), child.getName());
    }

    /**
     * 移除子节点
     *
     * @param childId 子节点 ID
     */
    @Override
    public void removeChild(String childId) {
        if (StrUtil.isBlank(childId)) {
            throw new IllegalArgumentException("子节点 ID 不能为空");
        }

        boolean removed = children.removeIf(child -> StrUtil.equals(child.getId(), childId));
        log.info("权限资源节点移除结果，parentId={}，childId={}，removed={}", getId(), childId, removed);
    }

    /**
     * 获取子节点列表
     *
     * @return 子节点列表
     */
    @Override
    public List<AuthResourceComponent> getChildren() {
        return List.copyOf(children);
    }

    /**
     * 获取当前节点及子节点中的所有权限编码
     *
     * @return 权限编码列表
     */
    @Override
    public List<String> getPermissionCodes() {
        List<String> permissionCodes = new ArrayList<>();

        if (StrUtil.isNotBlank(getPermissionCode())) {
            permissionCodes.add(getPermissionCode());
        }

        for (AuthResourceComponent child : children) {
            permissionCodes.addAll(child.getPermissionCodes());
        }

        return permissionCodes;
    }

    /**
     * 统计当前节点及子节点总数
     *
     * @return 节点总数
     */
    @Override
    public int count() {
        int total = 1;
        for (AuthResourceComponent child : children) {
            total += child.count();
        }
        return total;
    }

    /**
     * 转换成前端树节点
     *
     * @return 权限资源树节点
     */
    @Override
    public AuthResourceTreeVO toTreeVO() {
        List<AuthResourceTreeVO> childVOList = children.stream()
                .sorted(Comparator.comparing(AuthResourceComponent::getSort))
                .map(AuthResourceComponent::toTreeVO)
                .toList();

        return AuthResourceTreeVO.builder()
                .id(getId())
                .parentId(getParentId())
                .name(getName())
                .type(getType().name())
                .permissionCode(getPermissionCode())
                .sort(getSort())
                .children(CollUtil.emptyIfNull(childVOList))
                .build();
    }
}
```

叶子节点表示按钮权限，不能继续添加子节点。对叶子节点调用 `addChild()` 会直接抛出异常，避免错误树结构进入系统。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/composite/AuthResourceLeaf.java`

```java
package io.github.atengk.designpattern.composite.composite;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.composite.enums.AuthResourceType;
import io.github.atengk.designpattern.composite.vo.AuthResourceTreeVO;

import java.util.List;

/**
 * 权限资源叶子节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class AuthResourceLeaf extends AbstractAuthResourceComponent {

    /**
     * 创建权限资源叶子节点
     *
     * @param id             资源 ID
     * @param parentId       父级资源 ID
     * @param name           资源名称
     * @param permissionCode 权限编码
     * @param sort           排序值
     */
    public AuthResourceLeaf(String id, String parentId, String name, String permissionCode, Integer sort) {
        super(id, parentId, name, AuthResourceType.BUTTON, permissionCode, sort);
        if (StrUtil.isBlank(permissionCode)) {
            throw new IllegalArgumentException("按钮权限节点必须配置权限编码");
        }
    }

    /**
     * 添加子节点
     *
     * @param child 子节点
     */
    @Override
    public void addChild(AuthResourceComponent child) {
        throw new UnsupportedOperationException("叶子节点不支持添加子节点");
    }

    /**
     * 移除子节点
     *
     * @param childId 子节点 ID
     */
    @Override
    public void removeChild(String childId) {
        throw new UnsupportedOperationException("叶子节点不支持移除子节点");
    }

    /**
     * 获取子节点列表
     *
     * @return 子节点列表
     */
    @Override
    public List<AuthResourceComponent> getChildren() {
        return List.of();
    }

    /**
     * 获取当前节点中的权限编码
     *
     * @return 权限编码列表
     */
    @Override
    public List<String> getPermissionCodes() {
        return List.of(getPermissionCode());
    }

    /**
     * 统计当前节点总数
     *
     * @return 节点总数
     */
    @Override
    public int count() {
        return 1;
    }

    /**
     * 转换成前端树节点
     *
     * @return 权限资源树节点
     */
    @Override
    public AuthResourceTreeVO toTreeVO() {
        return AuthResourceTreeVO.builder()
                .id(getId())
                .parentId(getParentId())
                .name(getName())
                .type(getType().name())
                .permissionCode(getPermissionCode())
                .sort(getSort())
                .children(List.of())
                .build();
    }
}
```

树工厂负责把扁平权限资源数据转换成组合树。真实项目中，扁平数据通常来自数据库。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/composite/AuthResourceTreeFactory.java`

```java
package io.github.atengk.designpattern.composite.composite;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.composite.dto.AuthResourceCreateRequest;
import io.github.atengk.designpattern.composite.enums.AuthResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 权限资源树工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthResourceTreeFactory {

    /**
     * 根据扁平权限资源构建组合树
     *
     * @param resources 扁平权限资源列表
     * @return 权限资源根节点列表
     */
    public List<AuthResourceComponent> buildTree(List<AuthResourceCreateRequest> resources) {
        if (CollUtil.isEmpty(resources)) {
            return List.of();
        }

        Map<String, AuthResourceComponent> componentMap = resources.stream()
                .map(this::createComponent)
                .collect(Collectors.toMap(AuthResourceComponent::getId, Function.identity()));

        for (AuthResourceComponent component : componentMap.values()) {
            String parentId = component.getParentId();
            if (StrUtil.isBlank(parentId)) {
                continue;
            }

            AuthResourceComponent parent = componentMap.get(parentId);
            if (parent == null) {
                log.warn("权限资源父节点不存在，childId={}，parentId={}", component.getId(), parentId);
                continue;
            }

            parent.addChild(component);
        }

        return componentMap.values()
                .stream()
                .filter(component -> StrUtil.isBlank(component.getParentId()))
                .sorted(Comparator.comparing(AuthResourceComponent::getSort))
                .toList();
    }

    /**
     * 根据请求对象创建组件节点
     *
     * @param request 权限资源请求
     * @return 权限资源组件
     */
    private AuthResourceComponent createComponent(AuthResourceCreateRequest request) {
        AuthResourceType type = AuthResourceType.parse(request.getType());

        if (type.isLeaf()) {
            return new AuthResourceLeaf(
                    request.getName(),
                    request.getParentId(),
                    request.getName(),
                    request.getPermissionCode(),
                    request.getSort()
            );
        }

        return new AuthResourceComposite(
                request.getName(),
                request.getParentId(),
                request.getName(),
                type,
                request.getPermissionCode(),
                request.getSort()
        );
    }
}
```

上面的 `createComponent()` 为了示例简洁，暂时用 `name` 作为资源 ID。真实项目中应该使用数据库主键、雪花 ID 或 UUID。下面的内存仓储会给每条资源生成稳定 ID，因此实际构建树时建议使用仓储实体。为了避免 DTO 同时承载 ID 和创建请求，可以在仓储层使用内部记录对象。

内存仓储模拟权限资源表，并提供查询和新增能力。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/repository/AuthResourceMemoryRepository.java`

```java
package io.github.atengk.designpattern.composite.repository;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.composite.enums.AuthResourceType;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 权限资源内存仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Repository
public class AuthResourceMemoryRepository {

    private final List<AuthResourceRecord> records = new CopyOnWriteArrayList<>();

    /**
     * 初始化示例权限资源数据
     */
    public AuthResourceMemoryRepository() {
        records.add(AuthResourceRecord.builder()
                .id("system")
                .parentId(null)
                .name("系统管理")
                .type(AuthResourceType.CATALOG)
                .permissionCode(null)
                .sort(1)
                .build());

        records.add(AuthResourceRecord.builder()
                .id("user")
                .parentId("system")
                .name("用户管理")
                .type(AuthResourceType.MENU)
                .permissionCode("system:user:view")
                .sort(1)
                .build());

        records.add(AuthResourceRecord.builder()
                .id("user-create")
                .parentId("user")
                .name("新增用户")
                .type(AuthResourceType.BUTTON)
                .permissionCode("system:user:create")
                .sort(1)
                .build());

        records.add(AuthResourceRecord.builder()
                .id("user-delete")
                .parentId("user")
                .name("删除用户")
                .type(AuthResourceType.BUTTON)
                .permissionCode("system:user:delete")
                .sort(2)
                .build());

        records.add(AuthResourceRecord.builder()
                .id("role")
                .parentId("system")
                .name("角色管理")
                .type(AuthResourceType.MENU)
                .permissionCode("system:role:view")
                .sort(2)
                .build());

        records.add(AuthResourceRecord.builder()
                .id("role-grant")
                .parentId("role")
                .name("分配权限")
                .type(AuthResourceType.BUTTON)
                .permissionCode("system:role:grant")
                .sort(1)
                .build());

        records.add(AuthResourceRecord.builder()
                .id("order")
                .parentId(null)
                .name("订单管理")
                .type(AuthResourceType.CATALOG)
                .permissionCode(null)
                .sort(2)
                .build());

        records.add(AuthResourceRecord.builder()
                .id("order-list")
                .parentId("order")
                .name("订单列表")
                .type(AuthResourceType.MENU)
                .permissionCode("order:list:view")
                .sort(1)
                .build());

        records.add(AuthResourceRecord.builder()
                .id("order-export")
                .parentId("order-list")
                .name("导出订单")
                .type(AuthResourceType.BUTTON)
                .permissionCode("order:list:export")
                .sort(1)
                .build());
    }

    /**
     * 查询全部权限资源
     *
     * @return 权限资源记录列表
     */
    public List<AuthResourceRecord> listAll() {
        return records.stream()
                .sorted(Comparator.comparing(AuthResourceRecord::getSort))
                .toList();
    }

    /**
     * 新增权限资源
     *
     * @param parentId       父级资源 ID
     * @param name           资源名称
     * @param type           资源类型
     * @param permissionCode 权限编码
     * @param sort           排序值
     * @return 新增后的权限资源记录
     */
    public AuthResourceRecord save(String parentId, String name, AuthResourceType type, String permissionCode, Integer sort) {
        String id = StrUtil.lowerFirst(type.name()) + "-" + IdUtil.fastSimpleUUID();

        AuthResourceRecord record = AuthResourceRecord.builder()
                .id(id)
                .parentId(parentId)
                .name(name)
                .type(type)
                .permissionCode(permissionCode)
                .sort(sort)
                .build();

        records.add(record);
        return record;
    }

    /**
     * 判断资源是否存在
     *
     * @param id 资源 ID
     * @return 是否存在
     */
    public boolean existsById(String id) {
        return records.stream().anyMatch(record -> StrUtil.equals(record.getId(), id));
    }

    /**
     * 权限资源记录
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    @Builder
    public static class AuthResourceRecord {

        /**
         * 资源 ID
         */
        private String id;

        /**
         * 父级资源 ID
         */
        private String parentId;

        /**
         * 资源名称
         */
        private String name;

        /**
         * 资源类型
         */
        private AuthResourceType type;

        /**
         * 权限编码
         */
        private String permissionCode;

        /**
         * 排序值
         */
        private Integer sort;
    }
}
```

为了让树工厂直接使用仓储记录，需要将前面的 `AuthResourceTreeFactory` 调整为基于 `AuthResourceRecord` 构建。实际项目建议使用这一版。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/composite/AuthResourceTreeFactory.java`

```java
package io.github.atengk.designpattern.composite.composite;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.composite.repository.AuthResourceMemoryRepository.AuthResourceRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 权限资源树工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AuthResourceTreeFactory {

    /**
     * 根据扁平权限资源构建组合树
     *
     * @param records 扁平权限资源记录列表
     * @return 权限资源根节点列表
     */
    public List<AuthResourceComponent> buildTree(List<AuthResourceRecord> records) {
        if (CollUtil.isEmpty(records)) {
            return List.of();
        }

        Map<String, AuthResourceComponent> componentMap = records.stream()
                .map(this::createComponent)
                .collect(Collectors.toMap(AuthResourceComponent::getId, Function.identity()));

        for (AuthResourceComponent component : componentMap.values()) {
            String parentId = component.getParentId();
            if (StrUtil.isBlank(parentId)) {
                continue;
            }

            AuthResourceComponent parent = componentMap.get(parentId);
            if (parent == null) {
                log.warn("权限资源父节点不存在，childId={}，parentId={}", component.getId(), parentId);
                continue;
            }

            parent.addChild(component);
        }

        return componentMap.values()
                .stream()
                .filter(component -> StrUtil.isBlank(component.getParentId()))
                .sorted(Comparator.comparing(AuthResourceComponent::getSort))
                .toList();
    }

    /**
     * 根据仓储记录创建组件节点
     *
     * @param record 权限资源记录
     * @return 权限资源组件
     */
    private AuthResourceComponent createComponent(AuthResourceRecord record) {
        if (record.getType().isLeaf()) {
            return new AuthResourceLeaf(
                    record.getId(),
                    record.getParentId(),
                    record.getName(),
                    record.getPermissionCode(),
                    record.getSort()
            );
        }

        return new AuthResourceComposite(
                record.getId(),
                record.getParentId(),
                record.getName(),
                record.getType(),
                record.getPermissionCode(),
                record.getSort()
        );
    }
}
```

权限资源服务接口定义业务操作入口。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/service/AuthResourceService.java`

```java
package io.github.atengk.designpattern.composite.service;

import io.github.atengk.designpattern.composite.dto.AuthResourceCreateRequest;
import io.github.atengk.designpattern.composite.vo.AuthResourceTreeVO;

import java.util.List;

/**
 * 权限资源服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface AuthResourceService {

    /**
     * 查询权限资源树
     *
     * @return 权限资源树
     */
    List<AuthResourceTreeVO> listTree();

    /**
     * 查询所有权限编码
     *
     * @return 权限编码列表
     */
    List<String> listPermissionCodes();

    /**
     * 统计权限资源节点数量
     *
     * @return 节点数量
     */
    Integer countResources();

    /**
     * 创建权限资源
     *
     * @param request 创建权限资源请求
     * @return 创建后的权限资源树节点
     */
    AuthResourceTreeVO create(AuthResourceCreateRequest request);
}
```

服务实现类面向统一的 `AuthResourceComponent` 处理树，不需要单独判断目录、菜单和按钮的递归逻辑。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/service/AuthResourceServiceImpl.java`

```java
package io.github.atengk.designpattern.composite.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.composite.composite.AuthResourceComponent;
import io.github.atengk.designpattern.composite.composite.AuthResourceTreeFactory;
import io.github.atengk.designpattern.composite.dto.AuthResourceCreateRequest;
import io.github.atengk.designpattern.composite.enums.AuthResourceType;
import io.github.atengk.designpattern.composite.repository.AuthResourceMemoryRepository;
import io.github.atengk.designpattern.composite.repository.AuthResourceMemoryRepository.AuthResourceRecord;
import io.github.atengk.designpattern.composite.vo.AuthResourceTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限资源服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthResourceServiceImpl implements AuthResourceService {

    private final AuthResourceMemoryRepository authResourceMemoryRepository;
    private final AuthResourceTreeFactory authResourceTreeFactory;

    /**
     * 查询权限资源树
     *
     * @return 权限资源树
     */
    @Override
    public List<AuthResourceTreeVO> listTree() {
        List<AuthResourceComponent> roots = buildResourceTree();

        log.info("查询权限资源树成功，rootSize={}", roots.size());
        return roots.stream()
                .map(AuthResourceComponent::toTreeVO)
                .toList();
    }

    /**
     * 查询所有权限编码
     *
     * @return 权限编码列表
     */
    @Override
    public List<String> listPermissionCodes() {
        List<AuthResourceComponent> roots = buildResourceTree();
        List<String> permissionCodes = new ArrayList<>();

        for (AuthResourceComponent root : roots) {
            permissionCodes.addAll(root.getPermissionCodes());
        }

        log.info("查询权限编码成功，size={}", permissionCodes.size());
        return permissionCodes;
    }

    /**
     * 统计权限资源节点数量
     *
     * @return 节点数量
     */
    @Override
    public Integer countResources() {
        List<AuthResourceComponent> roots = buildResourceTree();
        int total = roots.stream()
                .mapToInt(AuthResourceComponent::count)
                .sum();

        log.info("统计权限资源节点成功，total={}", total);
        return total;
    }

    /**
     * 创建权限资源
     *
     * @param request 创建权限资源请求
     * @return 创建后的权限资源树节点
     */
    @Override
    public AuthResourceTreeVO create(AuthResourceCreateRequest request) {
        AuthResourceType type = AuthResourceType.parse(request.getType());

        if (StrUtil.isNotBlank(request.getParentId()) && !authResourceMemoryRepository.existsById(request.getParentId())) {
            throw new IllegalArgumentException("父级资源不存在：" + request.getParentId());
        }

        if (type.isLeaf() && StrUtil.isBlank(request.getPermissionCode())) {
            throw new IllegalArgumentException("按钮权限必须配置权限编码");
        }

        AuthResourceRecord record = authResourceMemoryRepository.save(
                request.getParentId(),
                request.getName(),
                type,
                request.getPermissionCode(),
                request.getSort()
        );

        log.info("创建权限资源成功，id={}，name={}，type={}", record.getId(), record.getName(), record.getType());

        return AuthResourceTreeVO.builder()
                .id(record.getId())
                .parentId(record.getParentId())
                .name(record.getName())
                .type(record.getType().name())
                .permissionCode(record.getPermissionCode())
                .sort(record.getSort())
                .children(List.of())
                .build();
    }

    /**
     * 构建权限资源组合树
     *
     * @return 权限资源根节点列表
     */
    private List<AuthResourceComponent> buildResourceTree() {
        List<AuthResourceRecord> records = authResourceMemoryRepository.listAll();
        if (CollUtil.isEmpty(records)) {
            return List.of();
        }

        return authResourceTreeFactory.buildTree(records);
    }
}
```

Controller 对外提供权限树查询、权限编码查询、节点统计和新增接口。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/controller/AuthResourceController.java`

```java
package io.github.atengk.designpattern.composite.controller;

import io.github.atengk.designpattern.composite.dto.AuthResourceCreateRequest;
import io.github.atengk.designpattern.composite.service.AuthResourceService;
import io.github.atengk.designpattern.composite.vo.ApiResult;
import io.github.atengk.designpattern.composite.vo.AuthResourceTreeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限资源接口控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/resources")
public class AuthResourceController {

    private final AuthResourceService authResourceService;

    /**
     * 查询权限资源树
     *
     * @return 权限资源树
     */
    @GetMapping("/tree")
    public ApiResult<List<AuthResourceTreeVO>> listTree() {
        return ApiResult.success(authResourceService.listTree());
    }

    /**
     * 查询所有权限编码
     *
     * @return 权限编码列表
     */
    @GetMapping("/permission-codes")
    public ApiResult<List<String>> listPermissionCodes() {
        return ApiResult.success(authResourceService.listPermissionCodes());
    }

    /**
     * 统计权限资源节点数量
     *
     * @return 节点数量
     */
    @GetMapping("/count")
    public ApiResult<Integer> countResources() {
        return ApiResult.success(authResourceService.countResources());
    }

    /**
     * 创建权限资源
     *
     * @param request 创建权限资源请求
     * @return 创建后的权限资源
     */
    @PostMapping
    public ApiResult<AuthResourceTreeVO> create(@Valid @RequestBody AuthResourceCreateRequest request) {
        return ApiResult.success(authResourceService.create(request));
    }
}
```

全局异常处理器用于统一处理参数校验异常、非法参数异常和不支持操作异常。

文件位置：`src/main/java/io/github/atengk/designpattern/composite/web/GlobalExceptionHandler.java`

```java
package io.github.atengk.designpattern.composite.web;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.composite.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
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
     * @return API 返回对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数不合法");

        log.warn("请求参数校验失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("请求参数错误，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理不支持操作异常
     *
     * @param exception 不支持操作异常
     * @return API 返回对象
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ApiResult<Void> handleUnsupportedOperationException(UnsupportedOperationException exception) {
        log.warn("不支持的资源操作，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }
}
```

## 使用方式

启动项目后，可以通过接口查看组合模式构建出来的权限树。

查询权限资源树：

```bash
curl -X GET 'http://localhost:8080/api/auth/resources/tree'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "system",
      "parentId": null,
      "name": "系统管理",
      "type": "CATALOG",
      "permissionCode": null,
      "sort": 1,
      "children": [
        {
          "id": "user",
          "parentId": "system",
          "name": "用户管理",
          "type": "MENU",
          "permissionCode": "system:user:view",
          "sort": 1,
          "children": [
            {
              "id": "user-create",
              "parentId": "user",
              "name": "新增用户",
              "type": "BUTTON",
              "permissionCode": "system:user:create",
              "sort": 1,
              "children": []
            },
            {
              "id": "user-delete",
              "parentId": "user",
              "name": "删除用户",
              "type": "BUTTON",
              "permissionCode": "system:user:delete",
              "sort": 2,
              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

查询所有权限编码：

```bash
curl -X GET 'http://localhost:8080/api/auth/resources/permission-codes'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    "system:user:view",
    "system:user:create",
    "system:user:delete",
    "system:role:view",
    "system:role:grant",
    "order:list:view",
    "order:list:export"
  ]
}
```

统计权限资源节点数量：

```bash
curl -X GET 'http://localhost:8080/api/auth/resources/count'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 9
}
```

新增按钮权限节点：

```bash
curl -X POST 'http://localhost:8080/api/auth/resources' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "编辑用户",
    "type": "BUTTON",
    "parentId": "user",
    "permissionCode": "system:user:update",
    "sort": 3
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "button_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "parentId": "user",
    "name": "编辑用户",
    "type": "BUTTON",
    "permissionCode": "system:user:update",
    "sort": 3,
    "children": []
  }
}
```

## 验证方式

可以从下面几个角度验证组合模式是否落地成功。

第一，调用方统一面向 `AuthResourceComponent` 编程。`AuthResourceServiceImpl` 在统计权限编码、统计节点数量、转换前端树时，不需要分别处理目录、菜单、按钮。

第二，容器节点和叶子节点有统一接口。`AuthResourceComposite` 和 `AuthResourceLeaf` 都实现了 `AuthResourceComponent`，调用方可以递归调用 `getPermissionCodes()`、`count()`、`toTreeVO()`。

第三，叶子节点明确禁止添加子节点。对 `AuthResourceLeaf.addChild()` 的调用会抛出 `UnsupportedOperationException`，可以避免按钮下面继续挂子节点。

第四，树结构操作被封装在组件内部。比如获取所有权限编码时，调用方不需要写递归，只需要调用根节点的 `getPermissionCodes()`。

可以重点查看日志：

```text
权限资源节点添加成功，parentId=system，childId=user，childName=用户管理
权限资源节点添加成功，parentId=user，childId=user-create，childName=新增用户
查询权限编码成功，size=7
统计权限资源节点成功，total=9
```

## 扩展新节点类型

如果后续要新增“接口权限”节点，例如 `API`，可以根据它是否允许包含子节点决定实现方式。

如果 `API` 是叶子节点，可以在 `AuthResourceType` 中新增：

```java
API
```

并调整 `isLeaf()`：

```java
public boolean isLeaf() {
    return this == BUTTON || this == API;
}
```

如果 `API` 仍然需要挂载子接口或操作权限，则可以让它作为容器节点，不加入 `isLeaf()` 判断。

组合模式的重点是：新增节点类型时，尽量不要修改调用方递归处理逻辑，而是让新节点继续遵守 `AuthResourceComponent` 的统一接口。

## 适用场景

组合模式适合处理明显的树形结构，并且整体和部分需要支持相同操作的场景。

常见 Spring Boot 项目场景如下。

| 场景         | 容器节点             | 叶子节点                 |
| ------------ | -------------------- | ------------------------ |
| 权限资源树   | 目录、菜单           | 按钮、接口权限           |
| 部门组织树   | 公司、部门、小组     | 员工                     |
| 商品分类树   | 一级分类、二级分类   | 具体商品分类             |
| 文件目录树   | 文件夹               | 文件                     |
| 评论树       | 一级评论、回复评论   | 无子回复评论             |
| 表单组件树   | 分组、容器、布局组件 | 输入框、选择器、日期组件 |
| 工作流节点树 | 阶段、分组节点       | 审批节点、抄送节点       |

组合模式尤其适合以下情况：

```text
节点存在父子关系
节点需要递归处理
容器节点和叶子节点对外具有一致操作
调用方不希望关心具体节点类型
树结构未来可能扩展更多节点类型
```

## 和其他模式的区别

组合模式容易和迭代器模式、装饰器模式、桥接模式混淆。区分时重点看模式解决的问题。

| 模式       | 关注点               | 和组合模式的区别                       |
| ---------- | -------------------- | -------------------------------------- |
| 组合模式   | 组织树形结构         | 重点是让容器节点和叶子节点统一处理     |
| 迭代器模式 | 遍历集合或结构       | 重点是隐藏遍历方式，不一定组织树       |
| 装饰器模式 | 动态增强对象能力     | 重点是给对象叠加功能，不是表达父子层级 |
| 桥接模式   | 拆分两个独立变化维度 | 重点是避免组合类爆炸，不一定有树结构   |
| 责任链模式 | 按顺序传递请求       | 重点是链式处理，不是整体和部分结构     |

本示例中，权限资源天然具有树形结构，目录、菜单、按钮又需要统一转换、统计和提取权限编码，因此适合使用组合模式。

## 注意事项

组合模式不适合所有层级数据。如果只是简单的两级列表，直接用普通 DTO 组装可能更清晰。只有当树结构递归较多、节点类型较多、统一操作较多时，组合模式才有明显价值。

叶子节点是否暴露 `addChild()` 方法要谨慎。透明式组合模式会让叶子节点也拥有 `addChild()` 方法，但运行时抛出异常。安全式组合模式会把子节点管理能力只放在容器节点上。本文示例采用透明式写法，优点是调用方统一，缺点是叶子节点存在不支持操作的方法。

树构建时要处理脏数据。例如父节点不存在、循环引用、重复 ID、按钮节点挂子节点等。生产项目中建议在入库时做严格校验，避免每次构建树时才发现数据异常。

树形结构不要无限递归。对于部门树、评论树、分类树等用户可配置数据，需要限制最大层级，防止异常数据导致栈溢出或接口响应过大。

如果树数据来自数据库，建议一次性查询扁平列表后在内存中构建树，避免递归查询数据库产生 N+1 查询问题。

## 总结

组合模式的核心价值是让调用方用统一方式处理树形结构中的整体和部分。

在本示例中：

```text
AuthResourceComponent 定义统一组件接口
AuthResourceComposite 表示目录或菜单等容器节点
AuthResourceLeaf 表示按钮等叶子节点
AuthResourceTreeFactory 负责把扁平数据构建成组合树
AuthResourceServiceImpl 面向统一组件完成查询、统计和转换
```

最终效果是：

```text
目录、菜单、按钮可以统一处理
递归逻辑被封装在组件内部
调用方不需要散落大量类型判断
新增节点类型时影响范围更小
权限树构建、统计、转换更清晰
```
