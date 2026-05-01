# 设计模式：原型模式

原型模式用于通过复制已有对象来创建新对象，而不是每次都从零开始构造。在 JDK21 和 Spring Boot 3 项目中，原型模式常用于复杂配置复制、模板对象复制、导出任务复制、审批流程复制、消息模板复制、表单模板复制、查询条件复制等场景。

需要注意：原型模式关注的是“复制已有对象”。如果对象创建过程复杂且需要一步步组装，更适合构建者模式；如果根据类型创建不同对象，更适合工厂模式；如果需要从一个已有模板快速生成相似对象，原型模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证原型模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于对象拷贝、JSON深拷贝、字符串、ID 等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化 Getter、Setter、Builder、日志等样板代码 -->
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

原型模式的核心目标是复用已有对象的状态，通过复制快速创建新对象，避免重复初始化复杂字段。

常见角色如下：

| 角色              | 说明                                          |
| ----------------- | --------------------------------------------- |
| Prototype         | 原型接口，定义复制方法                        |
| ConcretePrototype | 具体原型对象，实现复制逻辑                    |
| PrototypeRegistry | 原型注册表，缓存多个模板对象                  |
| Client            | 调用方，从原型对象复制出新对象                |
| Clone Context     | 复制后的上下文调整，例如新 ID、新名称、新时间 |

常见实现方式如下：

| 实现方式                         | 是否推荐         | 适用场景                                  |
| -------------------------------- | ---------------- | ----------------------------------------- |
| 拷贝构造方法                     | 推荐             | 字段明确、复制逻辑可控                    |
| 手写 `copy` 方法                 | 推荐             | 需要定制复制逻辑                          |
| Hutool `BeanUtil.copyProperties` | 推荐             | 普通 Bean 浅拷贝                          |
| JSON 序列化深拷贝                | 可用             | 简单对象图深拷贝                          |
| `Cloneable` + `clone`            | 谨慎使用         | Java 原生机制较旧，可读性一般             |
| Spring `prototype` scope         | 不是典型原型模式 | 每次从容器获取新 Bean，不等于复制已有对象 |

在 Spring Boot 项目中，常见优先级通常是：

```text
手写 copy 方法 > 拷贝构造方法 > Hutool BeanUtil 浅拷贝 > JSON 深拷贝 > Cloneable
```

浅拷贝和深拷贝是原型模式的重点区别。

```text
浅拷贝：复制对象本身，引用类型字段仍然指向同一个对象。
深拷贝：复制对象本身，也复制引用类型字段指向的对象。
```

如果对象中包含 `List`、`Map`、自定义对象等引用类型字段，就必须明确是否需要深拷贝。

## 普通 Java 原型

普通 Java 原型适合不依赖 Spring 容器的模板复制场景。下面以导出任务模板为例，不同用户可以基于同一个导出模板快速生成自己的导出任务。

整体流程如下：

```text
创建导出任务模板 -> 复制模板 -> 修改任务ID、操作人、创建时间 -> 得到新的导出任务
```

### 文件结构

```text
src/main/java/io/github/atengk/design/prototype/simple/
├── ExportField.java
└── ExportTask.java
```

文件位置：`src/main/java/io/github/atengk/design/prototype/simple/ExportField.java`

下面是导出字段对象，用于描述导出文件中的字段配置。

```java
package io.github.atengk.design.prototype.simple;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 导出字段
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
@Setter
@AllArgsConstructor
public class ExportField {

    private String fieldName;
    private String title;
    private Integer width;

    /**
     * 复制导出字段
     *
     * @return 新的导出字段
     */
    public ExportField copy() {
        return new ExportField(this.fieldName, this.title, this.width);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/prototype/simple/ExportTask.java`

下面是导出任务原型对象。`copyAsNewTask` 会深拷贝字段列表，并重置任务 ID、操作人和创建时间。

```java
package io.github.atengk.design.prototype.simple;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导出任务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
@Setter
@AllArgsConstructor
public class ExportTask {

    private String taskId;
    private String taskName;
    private String reportType;
    private Long operatorId;
    private List<ExportField> fields;
    private LocalDateTime createTime;

    /**
     * 基于当前任务复制一个新任务
     *
     * @param newTaskName   新任务名称
     * @param newOperatorId 新操作人ID
     * @return 新导出任务
     */
    public ExportTask copyAsNewTask(String newTaskName, Long newOperatorId) {
        if (StrUtil.isBlank(newTaskName)) {
            log.warn("复制导出任务失败，新任务名称为空");
            throw new IllegalArgumentException("新任务名称不能为空");
        }

        if (newOperatorId == null || newOperatorId <= 0) {
            log.warn("复制导出任务失败，新操作人ID不合法，操作人ID：{}", newOperatorId);
            throw new IllegalArgumentException("新操作人ID必须大于0");
        }

        List<ExportField> copiedFields = this.fields.stream()
                .map(ExportField::copy)
                .toList();

        ExportTask copiedTask = new ExportTask(
                "TASK" + IdUtil.getSnowflakeNextId(),
                newTaskName,
                this.reportType,
                newOperatorId,
                copiedFields,
                DateUtil.date().toLocalDateTime()
        );

        log.info("复制导出任务成功，原任务ID：{}，新任务ID：{}，创建时间：{}",
                this.taskId, copiedTask.getTaskId(), copiedTask.getCreateTime());

        return copiedTask;
    }
}
```

使用方式：

```java
ExportTask templateTask = new ExportTask(
        "TASK_TEMPLATE",
        "订单报表模板",
        "order",
        10001L,
        List.of(
                new ExportField("orderNo", "订单号", 180),
                new ExportField("amount", "订单金额", 120),
                new ExportField("status", "订单状态", 100)
        ),
        LocalDateTime.now()
);

ExportTask newTask = templateTask.copyAsNewTask("订单报表导出-20260430", 20001L);
```

这里的 `fields` 使用了深拷贝。复制后的新任务修改字段标题或宽度，不会影响原模板任务。

## 浅拷贝和深拷贝

原型模式最容易出问题的地方是引用类型字段。浅拷贝会导致两个对象共享同一个引用对象，后续修改可能互相影响。

下面通过导出任务字段列表说明差异。

浅拷贝示例：

```java
ExportTask copiedTask = new ExportTask(
        "TASK_NEW",
        this.taskName,
        this.reportType,
        newOperatorId,
        this.fields,
        LocalDateTime.now()
);
```

这种写法中，`copiedTask.fields` 和 `this.fields` 指向同一个列表。只要其中一个对象修改字段列表，另一个对象也会受到影响。

深拷贝示例：

```java
List<ExportField> copiedFields = this.fields.stream()
        .map(ExportField::copy)
        .toList();
```

这种写法会为每个字段创建新对象，复制后的任务和原任务互不影响。

在业务项目中可以按字段类型选择复制方式：

| 字段类型                               | 推荐复制方式                       |
| -------------------------------------- | ---------------------------------- |
| `String`、包装类型、`BigDecimal`、枚举 | 可直接赋值                         |
| `LocalDateTime`、`LocalDate`           | 通常可直接赋值，因为对象不可变     |
| `List<T>`                              | 新建列表，必要时复制元素           |
| `Map<K, V>`                            | 新建 Map，必要时复制 value         |
| 自定义可变对象                         | 提供 `copy` 方法或使用深拷贝       |
| Entity 对象                            | 谨慎复制，避免复制主键和持久化状态 |

## Hutool 对象复制

在 Spring Boot 项目中，如果对象是普通 JavaBean，可以使用 Hutool `BeanUtil.copyProperties` 快速做浅拷贝。它适合字段简单、引用类型不需要深拷贝的对象。

下面以消息模板复制为例。

### 文件结构

```text
src/main/java/io/github/atengk/design/prototype/hutool/
└── MessageTemplate.java
```

文件位置：`src/main/java/io/github/atengk/design/prototype/hutool/MessageTemplate.java`

下面是消息模板对象，使用 Hutool 复制字段，并重置模板 ID 和创建人。

```java
package io.github.atengk.design.prototype.hutool;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 消息模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
@Setter
public class MessageTemplate {

    private String templateId;
    private String templateCode;
    private String title;
    private String content;
    private Long creatorId;
    private LocalDateTime createTime;

    /**
     * 复制为新消息模板
     *
     * @param newTemplateCode 新模板编码
     * @param newCreatorId    新创建人ID
     * @return 新消息模板
     */
    public MessageTemplate copyAsNewTemplate(String newTemplateCode, Long newCreatorId) {
        if (StrUtil.isBlank(newTemplateCode)) {
            log.warn("复制消息模板失败，新模板编码为空");
            throw new IllegalArgumentException("新模板编码不能为空");
        }

        if (newCreatorId == null || newCreatorId <= 0) {
            log.warn("复制消息模板失败，新创建人ID不合法，创建人ID：{}", newCreatorId);
            throw new IllegalArgumentException("新创建人ID必须大于0");
        }

        MessageTemplate copiedTemplate = BeanUtil.copyProperties(this, MessageTemplate.class);
        copiedTemplate.setTemplateId("TPL" + IdUtil.getSnowflakeNextId());
        copiedTemplate.setTemplateCode(newTemplateCode);
        copiedTemplate.setCreatorId(newCreatorId);
        copiedTemplate.setCreateTime(DateUtil.date().toLocalDateTime());

        log.info("复制消息模板成功，原模板ID：{}，新模板ID：{}", this.templateId, copiedTemplate.getTemplateId());
        return copiedTemplate;
    }
}
```

使用方式：

```java
MessageTemplate template = new MessageTemplate();
template.setTemplateId("TPL_TEMPLATE");
template.setTemplateCode("ORDER_PAY_SUCCESS");
template.setTitle("订单支付成功");
template.setContent("你的订单 ${orderNo} 已支付成功");
template.setCreatorId(10001L);
template.setCreateTime(LocalDateTime.now());

MessageTemplate copiedTemplate = template.copyAsNewTemplate("ORDER_PAY_SUCCESS_COPY", 20001L);
```

这种方式代码简洁，但它是浅拷贝。如果对象中包含可变集合或嵌套对象，需要额外处理。

## JSON 深拷贝

对于简单对象图，可以使用 JSON 序列化和反序列化实现深拷贝。Hutool 的 `JSONUtil` 可以完成这种复制方式。

这种方式适合对象结构不复杂、字段都能正常 JSON 序列化的场景。它不适合复制包含文件流、线程、数据库连接、Lambda、代理对象、循环引用的复杂对象。

### 文件结构

```text
src/main/java/io/github/atengk/design/prototype/deepcopy/
├── WorkflowNode.java
└── WorkflowTemplate.java
```

文件位置：`src/main/java/io/github/atengk/design/prototype/deepcopy/WorkflowNode.java`

下面是审批流程节点对象。

```java
package io.github.atengk.design.prototype.deepcopy;

import lombok.Getter;
import lombok.Setter;

/**
 * 审批流程节点
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
@Setter
public class WorkflowNode {

    private String nodeCode;
    private String nodeName;
    private Integer sortNo;
}
```

文件位置：`src/main/java/io/github/atengk/design/prototype/deepcopy/WorkflowTemplate.java`

下面是审批流程模板对象，通过 JSON 深拷贝复制流程节点列表。

```java
package io.github.atengk.design.prototype.deepcopy;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批流程模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
@Setter
public class WorkflowTemplate {

    private String templateId;
    private String templateName;
    private String bizType;
    private List<WorkflowNode> nodes;
    private Long creatorId;
    private LocalDateTime createTime;

    /**
     * 复制为新审批流程模板
     *
     * @param newTemplateName 新模板名称
     * @param newCreatorId    新创建人ID
     * @return 新审批流程模板
     */
    public WorkflowTemplate copyAsNewTemplate(String newTemplateName, Long newCreatorId) {
        if (StrUtil.isBlank(newTemplateName)) {
            log.warn("复制审批流程模板失败，新模板名称为空");
            throw new IllegalArgumentException("新模板名称不能为空");
        }

        WorkflowTemplate copiedTemplate = JSONUtil.toBean(JSONUtil.toJsonStr(this), WorkflowTemplate.class);
        copiedTemplate.setTemplateId("WF" + IdUtil.getSnowflakeNextId());
        copiedTemplate.setTemplateName(newTemplateName);
        copiedTemplate.setCreatorId(newCreatorId);
        copiedTemplate.setCreateTime(DateUtil.date().toLocalDateTime());

        log.info("复制审批流程模板成功，原模板ID：{}，新模板ID：{}",
                this.templateId, copiedTemplate.getTemplateId());

        return copiedTemplate;
    }
}
```

使用方式：

```java
WorkflowNode firstNode = new WorkflowNode();
firstNode.setNodeCode("SUBMIT");
firstNode.setNodeName("提交申请");
firstNode.setSortNo(1);

WorkflowNode secondNode = new WorkflowNode();
secondNode.setNodeCode("APPROVE");
secondNode.setNodeName("主管审批");
secondNode.setSortNo(2);

WorkflowTemplate template = new WorkflowTemplate();
template.setTemplateId("WF_TEMPLATE");
template.setTemplateName("请假审批模板");
template.setBizType("leave");
template.setNodes(List.of(firstNode, secondNode));
template.setCreatorId(10001L);
template.setCreateTime(LocalDateTime.now());

WorkflowTemplate copiedTemplate = template.copyAsNewTemplate("请假审批模板副本", 20001L);
```

JSON 深拷贝写法简单，但会带来序列化成本，并且要求字段能正确被序列化和反序列化。对性能敏感或对象结构复杂的场景，建议手写复制逻辑。

## Spring Boot 原型注册表

Spring Boot 项目中可以使用原型注册表统一管理多个模板对象，调用方根据模板编码获取副本，而不是每次重新构造模板。

下面以通知模板为例，系统内置多个通知模板，业务调用时按模板编码复制一个新模板，然后填充实际参数。

整体流程如下：

```text
启动时注册模板 -> 根据模板编码获取副本 -> 填充业务参数 -> 返回渲染结果
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── PrototypeApplication.java
├── controller/
│   └── NoticeTemplateController.java
├── dto/
│   ├── NoticeTemplate.java
│   └── NoticeRenderResponse.java
├── registry/
│   └── NoticeTemplateRegistry.java
└── service/
    ├── NoticeRenderService.java
    └── impl/
        └── NoticeRenderServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/PrototypeApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 原型模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class PrototypeApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PrototypeApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/NoticeTemplate.java`

下面是通知模板原型对象。`copy` 方法用于复制模板，避免调用方直接修改注册表中的模板对象。

```java
package io.github.atengk.design.dto;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 通知模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
@Setter
public class NoticeTemplate {

    private String templateId;
    private String templateCode;
    private String title;
    private String content;
    private String channel;
    private LocalDateTime createTime;

    /**
     * 复制通知模板
     *
     * @return 新通知模板
     */
    public NoticeTemplate copy() {
        NoticeTemplate copiedTemplate = BeanUtil.copyProperties(this, NoticeTemplate.class);
        copiedTemplate.setTemplateId("NT" + IdUtil.getSnowflakeNextId());
        copiedTemplate.setCreateTime(DateUtil.date().toLocalDateTime());

        log.debug("复制通知模板，模板编码：{}，新模板ID：{}", this.templateCode, copiedTemplate.getTemplateId());
        return copiedTemplate;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/NoticeRenderResponse.java`

下面是通知模板渲染响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 通知渲染响应
 *
 * @param templateCode 模板编码
 * @param channel      通知渠道
 * @param title        渲染后标题
 * @param content      渲染后内容
 * @author Ateng
 * @since 2026-04-30
 */
public record NoticeRenderResponse(
        String templateCode,
        String channel,
        String title,
        String content
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/registry/NoticeTemplateRegistry.java`

下面是通知模板注册表。它在初始化时注册几个模板原型，并在获取模板时返回副本。

```java
package io.github.atengk.design.registry;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NoticeTemplate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知模板注册表
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class NoticeTemplateRegistry {

    private final Map<String, NoticeTemplate> templateMap = new ConcurrentHashMap<>();

    /**
     * 初始化通知模板原型
     */
    @PostConstruct
    public void init() {
        registerTemplate(buildTemplate(
                "ORDER_PAY_SUCCESS",
                "订单支付成功",
                "你的订单 ${orderNo} 已支付成功，支付金额 ${amount} 元。",
                "sms"
        ));

        registerTemplate(buildTemplate(
                "ORDER_SHIPPED",
                "订单已发货",
                "你的订单 ${orderNo} 已发货，物流单号 ${expressNo}。",
                "sms"
        ));

        log.info("初始化通知模板注册表完成，模板数量：{}", templateMap.size());
    }

    /**
     * 根据模板编码获取模板副本
     *
     * @param templateCode 模板编码
     * @return 通知模板副本
     */
    public NoticeTemplate getTemplateCopy(String templateCode) {
        if (StrUtil.isBlank(templateCode)) {
            log.warn("获取通知模板失败，模板编码为空");
            throw new IllegalArgumentException("模板编码不能为空");
        }

        NoticeTemplate template = templateMap.get(templateCode);
        if (template == null) {
            log.warn("获取通知模板失败，模板不存在，模板编码：{}", templateCode);
            throw new IllegalArgumentException("模板不存在：" + templateCode);
        }

        return template.copy();
    }

    /**
     * 注册通知模板
     *
     * @param template 通知模板
     */
    private void registerTemplate(NoticeTemplate template) {
        templateMap.put(template.getTemplateCode(), template);
        log.info("注册通知模板，模板编码：{}", template.getTemplateCode());
    }

    /**
     * 构建通知模板
     *
     * @param templateCode 模板编码
     * @param title        标题
     * @param content      内容
     * @param channel      通知渠道
     * @return 通知模板
     */
    private NoticeTemplate buildTemplate(String templateCode, String title, String content, String channel) {
        NoticeTemplate template = new NoticeTemplate();
        template.setTemplateId("TEMPLATE_" + templateCode);
        template.setTemplateCode(templateCode);
        template.setTitle(title);
        template.setContent(content);
        template.setChannel(channel);
        template.setCreateTime(DateUtil.date().toLocalDateTime());
        return template;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/NoticeRenderService.java`

下面是通知模板渲染服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.NoticeRenderResponse;

/**
 * 通知渲染服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface NoticeRenderService {

    /**
     * 渲染通知模板
     *
     * @param templateCode 模板编码
     * @param orderNo      订单号
     * @param amount       金额
     * @return 通知渲染响应
     */
    NoticeRenderResponse renderPaySuccessNotice(String templateCode, String orderNo, String amount);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/NoticeRenderServiceImpl.java`

下面是通知模板渲染服务实现。它从注册表获取模板副本，然后替换模板变量。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.NoticeRenderResponse;
import io.github.atengk.design.dto.NoticeTemplate;
import io.github.atengk.design.registry.NoticeTemplateRegistry;
import io.github.atengk.design.service.NoticeRenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知渲染服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeRenderServiceImpl implements NoticeRenderService {

    private final NoticeTemplateRegistry noticeTemplateRegistry;

    /**
     * 渲染通知模板
     *
     * @param templateCode 模板编码
     * @param orderNo      订单号
     * @param amount       金额
     * @return 通知渲染响应
     */
    @Override
    public NoticeRenderResponse renderPaySuccessNotice(String templateCode, String orderNo, String amount) {
        if (StrUtil.hasBlank(templateCode, orderNo, amount)) {
            log.warn("渲染通知模板失败，模板编码、订单号或金额为空");
            throw new IllegalArgumentException("模板编码、订单号和金额不能为空");
        }

        NoticeTemplate template = noticeTemplateRegistry.getTemplateCopy(templateCode);

        String renderedContent = template.getContent()
                .replace("${orderNo}", orderNo)
                .replace("${amount}", amount);

        log.info("渲染通知模板成功，模板编码：{}，订单号：{}", templateCode, orderNo);

        return new NoticeRenderResponse(
                template.getTemplateCode(),
                template.getChannel(),
                template.getTitle(),
                renderedContent
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/NoticeTemplateController.java`

下面是通知模板接口，用于验证原型注册表和模板复制效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.NoticeRenderResponse;
import io.github.atengk.design.service.NoticeRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知模板控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/prototype/notice-template")
public class NoticeTemplateController {

    private final NoticeRenderService noticeRenderService;

    /**
     * 渲染订单支付成功通知
     *
     * @param templateCode 模板编码
     * @param orderNo      订单号
     * @param amount       金额
     * @return 通知渲染响应
     */
    @GetMapping("/render-pay-success")
    public NoticeRenderResponse renderPaySuccess(@RequestParam String templateCode,
                                                 @RequestParam String orderNo,
                                                 @RequestParam String amount) {
        return noticeRenderService.renderPaySuccessNotice(templateCode, orderNo, amount);
    }
}
```

接口调用示例：

```bash
curl "http://localhost:8080/prototype/notice-template/render-pay-success?templateCode=ORDER_PAY_SUCCESS&orderNo=ORDER10001&amount=99.90"
```

可能返回：

```json
{
  "templateCode": "ORDER_PAY_SUCCESS",
  "channel": "sms",
  "title": "订单支付成功",
  "content": "你的订单 ORDER10001 已支付成功，支付金额 99.90 元。"
}
```

这种方式的优点是模板原型只初始化一次，后续使用时都从注册表复制副本。调用方修改副本不会污染注册表中的原型对象。

## Spring 原型作用域

Spring 的 `prototype` 作用域和设计模式中的原型模式名字相似，但含义不同。

Spring `prototype` 表示每次从容器获取 Bean 时，Spring 都创建一个新的 Bean 实例。

文件位置：`src/main/java/io/github/atengk/design/component/TaskContext.java`

下面是一个 Spring 原型作用域 Bean 示例。

```java
package io.github.atengk.design.component;

import cn.hutool.core.util.IdUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 任务上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
@Component
@Scope("prototype")
public class TaskContext {

    private final String contextId = IdUtil.fastSimpleUUID();

    /**
     * 创建任务上下文
     */
    public TaskContext() {
        log.info("创建任务上下文，contextId：{}", contextId);
    }
}
```

如果每次通过 `ApplicationContext.getBean(TaskContext.class)` 获取 Bean，都会得到新实例。

```java
TaskContext firstContext = applicationContext.getBean(TaskContext.class);
TaskContext secondContext = applicationContext.getBean(TaskContext.class);
```

但这不是典型原型模式，因为它不是从一个已有对象复制出新对象，而是由 Spring 每次重新创建一个新对象。

简单区别如下：

| 对比项               | 设计模式中的原型模式         | Spring prototype 作用域 |
| -------------------- | ---------------------------- | ----------------------- |
| 创建方式             | 复制已有对象                 | 容器每次创建新 Bean     |
| 是否依赖已有对象状态 | 是                           | 不一定                  |
| 典型用途             | 模板复制、配置复制、任务复制 | 每次获取独立 Bean 实例  |
| 是否等价             | 不等价                       | 不等价                  |

## 原型模式和构建者模式的区别

原型模式和构建者模式都属于创建型设计模式，但关注点不同。

| 对比项   | 原型模式                     | 构建者模式                |
| -------- | ---------------------------- | ------------------------- |
| 核心目的 | 复制已有对象创建新对象       | 一步步组装复杂对象        |
| 依赖对象 | 依赖已有原型对象             | 不依赖已有对象            |
| 适合场景 | 模板复制、配置复制、流程复制 | 字段多、参数多、可选项多  |
| 创建方式 | `copy`、`clone`、深拷贝      | `builder().xxx().build()` |
| 风险点   | 浅拷贝导致共享引用           | 默认值和必填字段校验      |

简单理解：

```text
原型模式：已经有一个差不多的对象，我复制一份再改。
构建者模式：我要从零开始清晰地组装一个复杂对象。
```

如果系统中有订单导出模板、审批流模板、消息模板，基于模板生成副本更适合原型模式。如果是创建订单命令、复杂查询条件、导出配置，更适合构建者模式。

## 原型模式和工厂模式的区别

原型模式和工厂模式都可以创建对象，但创建依据不同。

| 对比项             | 原型模式               | 工厂模式               |
| ------------------ | ---------------------- | ---------------------- |
| 核心目的           | 复制已有对象           | 根据类型创建对象       |
| 对象来源           | 原型对象副本           | 工厂方法或具体实现类   |
| 是否保留原对象状态 | 通常保留大部分状态     | 通常重新初始化         |
| 典型场景           | 模板任务复制、流程复制 | 支付处理器、文件解析器 |
| 扩展方式           | 注册新原型             | 新增产品类或工厂逻辑   |

简单理解：

```text
原型模式：根据已有模板复制一份。
工厂模式：根据类型决定创建哪一种对象。
```

如果要根据 `alipay` 创建支付宝处理器，使用工厂模式。如果要基于“订单导出模板”复制出一个新的导出任务，使用原型模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行通知模板渲染接口：

```bash
curl "http://localhost:8080/prototype/notice-template/render-pay-success?templateCode=ORDER_PAY_SUCCESS&orderNo=ORDER10001&amount=99.90"
```

如果原型注册表正常，可以看到类似日志：

```text
注册通知模板，模板编码：ORDER_PAY_SUCCESS
注册通知模板，模板编码：ORDER_SHIPPED
初始化通知模板注册表完成，模板数量：2
渲染通知模板成功，模板编码：ORDER_PAY_SUCCESS，订单号：ORDER10001
```

执行不存在的模板编码：

```bash
curl "http://localhost:8080/prototype/notice-template/render-pay-success?templateCode=UNKNOWN&orderNo=ORDER10001&amount=99.90"
```

异常日志示例：

```text
获取通知模板失败，模板不存在，模板编码：UNKNOWN
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

如果要验证深拷贝，可以复制导出任务后修改新任务字段：

```java
ExportTask copiedTask = templateTask.copyAsNewTask("订单报表导出", 20001L);
copiedTask.getFields().get(0).setTitle("新订单号");
```

如果原模板字段标题没有变化，说明字段列表复制是安全的深拷贝。

## 注意事项

原型模式最重要的问题是明确浅拷贝和深拷贝边界。只要对象中有集合、Map、自定义可变对象，就不能简单认为复制是安全的。

不推荐在包含可变引用字段时直接浅拷贝：

```java
copiedTask.setFields(this.fields);
```

推荐复制集合本身，并在必要时复制集合元素：

```java
List<ExportField> copiedFields = this.fields.stream()
        .map(ExportField::copy)
        .toList();
```

不要盲目使用 `Cloneable`。Java 原生 `clone` 机制可读性差，默认也是浅拷贝，并且容易遗漏引用字段处理。

不推荐写法：

```java
@Override
protected Object clone() throws CloneNotSupportedException {
    return super.clone();
}
```

如果确实使用 `clone`，也要在方法中显式处理引用类型字段。但在业务项目中，通常更推荐手写 `copy` 方法。

原型对象如果放在注册表中，应避免被外部直接修改。注册表应该返回副本，而不是返回原型对象本身。

错误示例：

```java
public NoticeTemplate getTemplate(String templateCode) {
    return templateMap.get(templateCode);
}
```

推荐写法：

```java
public NoticeTemplate getTemplateCopy(String templateCode) {
    return templateMap.get(templateCode).copy();
}
```

复制 Entity 对象时要特别谨慎。不要把数据库主键、版本号、创建时间、持久化状态直接复制到新对象中。

常见需要重置的字段：

```text
id
version
createTime
createBy
updateTime
updateBy
status
bizNo
traceId
```

推荐复制业务配置字段，重置身份字段：

```java
copiedEntity.setId(null);
copiedEntity.setCreateTime(LocalDateTime.now());
copiedEntity.setStatus("DRAFT");
```

如果对象非常复杂，包含循环引用、延迟加载代理、文件流、线程池、数据库连接等资源，不建议使用通用深拷贝。应该手写明确的复制逻辑，只复制业务需要的字段。

生产环境中，原型模式常和注册表、工厂、构建者组合使用：

```text
原型注册表：保存多个模板对象
原型复制：从模板生成副本
构建者模式：补充新对象的差异字段
工厂模式：根据模板类型选择原型
```

## 总结

在 JDK21 和 Spring Boot 3 项目中，原型模式的实践重点是基于已有对象快速创建相似对象，同时明确浅拷贝和深拷贝边界。

普通 Java 原型适合导出任务、审批流程、消息模板等对象复制。Hutool `BeanUtil.copyProperties` 适合简单 Bean 浅拷贝。JSON 深拷贝适合简单对象图复制。Spring Boot 项目中，可以使用“原型对象 + 原型注册表 + copy 方法”的结构统一管理模板对象。

原型模式不是为了替代构造方法，也不是为了替代 Spring `prototype` 作用域。它最适合处理“已经有一个模板对象，需要快速复制出一个相似对象并修改少量字段”的场景。
