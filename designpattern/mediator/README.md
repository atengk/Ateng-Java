# 设计模式：中介者模式

中介者模式用于把多个对象之间复杂的相互调用关系，集中交给一个中介者对象处理，从而降低对象之间的直接耦合。在 JDK21 和 Spring Boot 3 项目中，中介者模式常用于聊天室消息转发、工单协同、审批流协作、订单多模块协作、页面组件联动、任务调度协同、领域对象之间的事件协调等场景。

需要注意：中介者模式关注的是“对象之间不要直接互相调用，而是通过中介者通信”。如果只是把多个子系统封装成一个简单入口，更接近外观模式；如果只是发布事件给多个订阅者，更接近观察者模式；如果是多个处理器按顺序处理请求，更适合责任链模式。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证中介者模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、集合等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法、Getter 等样板代码 -->
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

中介者模式的核心目标是把对象之间的网状依赖变成星形依赖。原本多个对象之间互相调用，后续会出现依赖混乱、修改影响范围大、调用链难追踪等问题。引入中介者后，各对象只依赖中介者，由中介者统一协调交互。

没有中介者时的关系：

```text
用户模块 -> 通知模块
用户模块 -> 审计模块
客服模块 -> 通知模块
客服模块 -> 工单模块
工单模块 -> 审计模块
工单模块 -> 通知模块
```

使用中介者后的关系：

```text
用户模块 ┐
客服模块 ├── WorkOrderMediator ── 通知模块
工单模块 ┘                       └── 审计模块
```

常见角色如下：

| 角色              | 说明                                   |
| ----------------- | -------------------------------------- |
| Mediator          | 中介者接口，定义对象之间通信入口       |
| ConcreteMediator  | 具体中介者，负责协调多个同事对象       |
| Colleague         | 同事对象，参与协作但不直接依赖其他同事 |
| ConcreteColleague | 具体同事对象，完成自身职责             |
| Client            | 调用方，触发某个协作动作               |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 中介者 > 普通 Java 中介者 > 多对象互相注入调用
```

中介者模式适合对象之间交互关系复杂、协作规则集中变化的场景。如果只是单向的一次性服务编排，外观模式通常更直接。

## 普通 Java 中介者模式

普通 Java 中介者模式适合不依赖 Spring 容器的对象协作场景。下面以聊天室为例，多个用户之间不直接互相发送消息，而是通过聊天室中介者统一转发。

整体关系如下：

```text
ChatUser
    -> ChatRoomMediator
        -> 找到目标用户
        -> 转发消息
```

### 文件结构

```text
src/main/java/io/github/atengk/design/mediator/simple/
├── ChatRoomMediator.java
├── SimpleChatRoomMediator.java
└── ChatUser.java
```

文件位置：`src/main/java/io/github/atengk/design/mediator/simple/ChatRoomMediator.java`

下面是聊天室中介者接口，定义用户注册和消息发送能力。

```java
package io.github.atengk.design.mediator.simple;

/**
 * 聊天室中介者
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface ChatRoomMediator {

    /**
     * 注册用户
     *
     * @param user 聊天用户
     */
    void register(ChatUser user);

    /**
     * 发送消息
     *
     * @param fromUserId 发送人ID
     * @param toUserId   接收人ID
     * @param content    消息内容
     */
    void sendMessage(String fromUserId, String toUserId, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/mediator/simple/ChatUser.java`

下面是聊天用户对象。用户只依赖聊天室中介者，不直接持有其他用户对象。

```java
package io.github.atengk.design.mediator.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天用户
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class ChatUser {

    private final String userId;
    private final String username;
    private final ChatRoomMediator mediator;

    /**
     * 创建聊天用户
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param mediator 聊天室中介者
     */
    public ChatUser(String userId, String username, ChatRoomMediator mediator) {
        if (StrUtil.hasBlank(userId, username)) {
            throw new IllegalArgumentException("用户ID和用户名不能为空");
        }
        if (mediator == null) {
            throw new IllegalArgumentException("聊天室中介者不能为空");
        }

        this.userId = userId;
        this.username = username;
        this.mediator = mediator;
    }

    /**
     * 发送消息
     *
     * @param toUserId 接收人ID
     * @param content  消息内容
     */
    public void send(String toUserId, String content) {
        log.info("用户发送消息，发送人：{}，接收人ID：{}，内容：{}", username, toUserId, content);
        mediator.sendMessage(userId, toUserId, content);
    }

    /**
     * 接收消息
     *
     * @param fromUserId 发送人ID
     * @param content    消息内容
     */
    public void receive(String fromUserId, String content) {
        log.info("用户收到消息，接收人：{}，发送人ID：{}，内容：{}", username, fromUserId, content);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/mediator/simple/SimpleChatRoomMediator.java`

下面是聊天室中介者实现，负责维护用户列表并转发消息。

```java
package io.github.atengk.design.mediator.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单聊天室中介者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class SimpleChatRoomMediator implements ChatRoomMediator {

    private final Map<String, ChatUser> userMap = new ConcurrentHashMap<>();

    /**
     * 注册用户
     *
     * @param user 聊天用户
     */
    @Override
    public void register(ChatUser user) {
        if (user == null) {
            log.warn("注册聊天用户失败，用户为空");
            throw new IllegalArgumentException("用户不能为空");
        }

        userMap.put(user.getUserId(), user);
        log.info("注册聊天用户成功，用户ID：{}，用户名：{}，当前用户数：{}",
                user.getUserId(), user.getUsername(), userMap.size());
    }

    /**
     * 发送消息
     *
     * @param fromUserId 发送人ID
     * @param toUserId   接收人ID
     * @param content    消息内容
     */
    @Override
    public void sendMessage(String fromUserId, String toUserId, String content) {
        if (StrUtil.hasBlank(fromUserId, toUserId, content)) {
            log.warn("聊天室发送消息失败，发送人、接收人或内容为空");
            throw new IllegalArgumentException("发送人、接收人和内容不能为空");
        }

        ChatUser targetUser = userMap.get(toUserId);
        if (targetUser == null) {
            log.warn("聊天室发送消息失败，接收人不存在，接收人ID：{}", toUserId);
            throw new IllegalArgumentException("接收人不存在：" + toUserId);
        }

        log.info("聊天室中介者转发消息，发送人ID：{}，接收人ID：{}", fromUserId, toUserId);
        targetUser.receive(fromUserId, content);
    }
}
```

使用方式：

```java
ChatRoomMediator mediator = new SimpleChatRoomMediator();

ChatUser ateng = new ChatUser("10001", "Ateng", mediator);
ChatUser blair = new ChatUser("10002", "Blair", mediator);

mediator.register(ateng);
mediator.register(blair);

ateng.send("10002", "你好，这是一条通过中介者转发的消息");
```

在这个示例中，`ChatUser` 之间没有直接引用关系。用户发送消息时只调用 `ChatRoomMediator`，由中介者决定如何找到目标用户并完成转发。

## Spring Boot 中介者模式

Spring Boot 项目中，中介者模式更适合处理多个业务组件之间的协作。下面以工单协同为例，工单创建、工单分配、通知发送、审计记录本来可能互相调用。使用中介者模式后，各组件只负责自己的职责，中介者负责协调它们之间的交互。

整体流程如下：

```text
Controller
    -> WorkOrderMediator
        -> WorkOrderService
        -> AgentService
        -> NoticeService
        -> AuditService
```

示例支持两个动作：

```text
create  创建工单
assign  分配工单
```

创建工单时，中介者会协调工单服务、通知服务和审计服务。分配工单时，中介者会协调客服服务、工单服务、通知服务和审计服务。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── MediatorApplication.java
├── controller/
│   └── WorkOrderController.java
├── dto/
│   ├── WorkOrderCommand.java
│   └── WorkOrderResponse.java
├── mediator/
│   ├── WorkOrderMediator.java
│   └── DefaultWorkOrderMediator.java
└── service/
    ├── WorkOrderService.java
    ├── AgentService.java
    ├── NoticeService.java
    ├── AuditService.java
    └── impl/
        ├── WorkOrderServiceImpl.java
        ├── AgentServiceImpl.java
        ├── NoticeServiceImpl.java
        └── AuditServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/MediatorApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 中介者模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class MediatorApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MediatorApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/WorkOrderCommand.java`

下面是工单协作命令对象，承载一次工单操作需要的参数。

```java
package io.github.atengk.design.dto;

/**
 * 工单协作命令
 *
 * @param action      操作类型
 * @param workOrderNo 工单号
 * @param title       工单标题
 * @param userId      用户ID
 * @param agentId     客服ID
 * @param content     工单内容
 * @author Ateng
 * @since 2026-04-30
 */
public record WorkOrderCommand(
        String action,
        String workOrderNo,
        String title,
        Long userId,
        Long agentId,
        String content
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/WorkOrderResponse.java`

下面是工单协作响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 工单协作响应
 *
 * @param action      操作类型
 * @param workOrderNo 工单号
 * @param agentId     客服ID
 * @param success     是否成功
 * @param message     响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record WorkOrderResponse(
        String action,
        String workOrderNo,
        Long agentId,
        Boolean success,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/WorkOrderService.java`

下面是工单服务接口，只处理工单自身能力，不直接调用通知、审计或客服服务。

```java
package io.github.atengk.design.service;

/**
 * 工单服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface WorkOrderService {

    /**
     * 创建工单
     *
     * @param title   工单标题
     * @param userId  用户ID
     * @param content 工单内容
     * @return 工单号
     */
    String create(String title, Long userId, String content);

    /**
     * 分配工单
     *
     * @param workOrderNo 工单号
     * @param agentId     客服ID
     */
    void assign(String workOrderNo, Long agentId);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/AgentService.java`

下面是客服服务接口，只负责客服相关能力。

```java
package io.github.atengk.design.service;

/**
 * 客服服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface AgentService {

    /**
     * 获取可用客服ID
     *
     * @return 客服ID
     */
    Long findAvailableAgent();

    /**
     * 校验客服是否可接单
     *
     * @param agentId 客服ID
     */
    void checkAgentAvailable(Long agentId);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/NoticeService.java`

下面是通知服务接口，只负责发送通知。

```java
package io.github.atengk.design.service;

/**
 * 通知服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface NoticeService {

    /**
     * 发送工单创建通知
     *
     * @param userId      用户ID
     * @param workOrderNo 工单号
     */
    void sendCreatedNotice(Long userId, String workOrderNo);

    /**
     * 发送工单分配通知
     *
     * @param agentId     客服ID
     * @param workOrderNo 工单号
     */
    void sendAssignedNotice(Long agentId, String workOrderNo);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/AuditService.java`

下面是审计服务接口，只负责记录操作审计。

```java
package io.github.atengk.design.service;

/**
 * 审计服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface AuditService {

    /**
     * 记录操作日志
     *
     * @param action      操作类型
     * @param workOrderNo 工单号
     * @param operatorId  操作人ID
     */
    void record(String action, String workOrderNo, Long operatorId);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/WorkOrderServiceImpl.java`

下面是工单服务实现。它只处理创建和分配工单本身，不感知其他服务。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.service.WorkOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工单服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    /**
     * 创建工单
     *
     * @param title   工单标题
     * @param userId  用户ID
     * @param content 工单内容
     * @return 工单号
     */
    @Override
    public String create(String title, Long userId, String content) {
        if (StrUtil.hasBlank(title, content)) {
            log.warn("创建工单失败，标题或内容为空");
            throw new IllegalArgumentException("工单标题和内容不能为空");
        }

        if (userId == null || userId <= 0) {
            log.warn("创建工单失败，用户ID不合法，用户ID：{}", userId);
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        String workOrderNo = "WO" + IdUtil.getSnowflakeNextId();
        log.info("创建工单成功，工单号：{}，用户ID：{}，标题：{}", workOrderNo, userId, title);
        return workOrderNo;
    }

    /**
     * 分配工单
     *
     * @param workOrderNo 工单号
     * @param agentId     客服ID
     */
    @Override
    public void assign(String workOrderNo, Long agentId) {
        if (StrUtil.isBlank(workOrderNo)) {
            log.warn("分配工单失败，工单号为空");
            throw new IllegalArgumentException("工单号不能为空");
        }

        if (agentId == null || agentId <= 0) {
            log.warn("分配工单失败，客服ID不合法，客服ID：{}", agentId);
            throw new IllegalArgumentException("客服ID必须大于0");
        }

        log.info("分配工单成功，工单号：{}，客服ID：{}", workOrderNo, agentId);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/AgentServiceImpl.java`

下面是客服服务实现，负责选择和校验客服。

```java
package io.github.atengk.design.service.impl;

import io.github.atengk.design.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 客服服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private static final Long MOCK_AGENT_ID = 90001L;

    /**
     * 获取可用客服ID
     *
     * @return 客服ID
     */
    @Override
    public Long findAvailableAgent() {
        log.info("查询可用客服成功，客服ID：{}", MOCK_AGENT_ID);
        return MOCK_AGENT_ID;
    }

    /**
     * 校验客服是否可接单
     *
     * @param agentId 客服ID
     */
    @Override
    public void checkAgentAvailable(Long agentId) {
        if (agentId == null || agentId <= 0) {
            log.warn("客服校验失败，客服ID不合法，客服ID：{}", agentId);
            throw new IllegalArgumentException("客服ID必须大于0");
        }

        log.info("客服校验通过，客服ID：{}", agentId);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/NoticeServiceImpl.java`

下面是通知服务实现，负责发送工单相关通知。

```java
package io.github.atengk.design.service.impl;

import io.github.atengk.design.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class NoticeServiceImpl implements NoticeService {

    /**
     * 发送工单创建通知
     *
     * @param userId      用户ID
     * @param workOrderNo 工单号
     */
    @Override
    public void sendCreatedNotice(Long userId, String workOrderNo) {
        log.info("发送工单创建通知，用户ID：{}，工单号：{}", userId, workOrderNo);
    }

    /**
     * 发送工单分配通知
     *
     * @param agentId     客服ID
     * @param workOrderNo 工单号
     */
    @Override
    public void sendAssignedNotice(Long agentId, String workOrderNo) {
        log.info("发送工单分配通知，客服ID：{}，工单号：{}", agentId, workOrderNo);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/AuditServiceImpl.java`

下面是审计服务实现，负责记录工单操作日志。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审计服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    /**
     * 记录操作日志
     *
     * @param action      操作类型
     * @param workOrderNo 工单号
     * @param operatorId  操作人ID
     */
    @Override
    public void record(String action, String workOrderNo, Long operatorId) {
        if (StrUtil.hasBlank(action, workOrderNo)) {
            log.warn("记录审计日志失败，操作类型或工单号为空");
            throw new IllegalArgumentException("操作类型和工单号不能为空");
        }

        log.info("记录工单审计日志，操作：{}，工单号：{}，操作人ID：{}", action, workOrderNo, operatorId);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/mediator/WorkOrderMediator.java`

下面是工单中介者接口，定义工单协作入口。

```java
package io.github.atengk.design.mediator;

import io.github.atengk.design.dto.WorkOrderCommand;
import io.github.atengk.design.dto.WorkOrderResponse;

/**
 * 工单中介者
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface WorkOrderMediator {

    /**
     * 处理工单协作命令
     *
     * @param command 工单协作命令
     * @return 工单协作响应
     */
    WorkOrderResponse handle(WorkOrderCommand command);
}
```

文件位置：`src/main/java/io/github/atengk/design/mediator/DefaultWorkOrderMediator.java`

下面是工单中介者实现。它集中协调工单、客服、通知和审计服务之间的协作关系。

```java
package io.github.atengk.design.mediator;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.WorkOrderCommand;
import io.github.atengk.design.dto.WorkOrderResponse;
import io.github.atengk.design.service.AgentService;
import io.github.atengk.design.service.AuditService;
import io.github.atengk.design.service.NoticeService;
import io.github.atengk.design.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认工单中介者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultWorkOrderMediator implements WorkOrderMediator {

    private static final String ACTION_CREATE = "create";
    private static final String ACTION_ASSIGN = "assign";

    private final WorkOrderService workOrderService;
    private final AgentService agentService;
    private final NoticeService noticeService;
    private final AuditService auditService;

    /**
     * 处理工单协作命令
     *
     * @param command 工单协作命令
     * @return 工单协作响应
     */
    @Override
    public WorkOrderResponse handle(WorkOrderCommand command) {
        validateCommand(command);

        String action = StrUtil.trim(command.action()).toLowerCase();

        if (ACTION_CREATE.equals(action)) {
            return createWorkOrder(command);
        }

        if (ACTION_ASSIGN.equals(action)) {
            return assignWorkOrder(command);
        }

        log.warn("处理工单协作失败，不支持的操作类型：{}", command.action());
        throw new IllegalArgumentException("不支持的操作类型：" + command.action());
    }

    /**
     * 创建工单
     *
     * @param command 工单协作命令
     * @return 工单协作响应
     */
    private WorkOrderResponse createWorkOrder(WorkOrderCommand command) {
        String workOrderNo = workOrderService.create(command.title(), command.userId(), command.content());
        Long agentId = agentService.findAvailableAgent();

        workOrderService.assign(workOrderNo, agentId);
        noticeService.sendCreatedNotice(command.userId(), workOrderNo);
        noticeService.sendAssignedNotice(agentId, workOrderNo);
        auditService.record(ACTION_CREATE, workOrderNo, command.userId());

        log.info("中介者完成工单创建协作，工单号：{}，用户ID：{}，客服ID：{}",
                workOrderNo, command.userId(), agentId);

        return new WorkOrderResponse(
                ACTION_CREATE,
                workOrderNo,
                agentId,
                true,
                "工单创建成功"
        );
    }

    /**
     * 分配工单
     *
     * @param command 工单协作命令
     * @return 工单协作响应
     */
    private WorkOrderResponse assignWorkOrder(WorkOrderCommand command) {
        if (StrUtil.isBlank(command.workOrderNo())) {
            log.warn("分配工单失败，工单号为空");
            throw new IllegalArgumentException("工单号不能为空");
        }

        Long agentId = command.agentId();
        agentService.checkAgentAvailable(agentId);

        workOrderService.assign(command.workOrderNo(), agentId);
        noticeService.sendAssignedNotice(agentId, command.workOrderNo());
        auditService.record(ACTION_ASSIGN, command.workOrderNo(), agentId);

        log.info("中介者完成工单分配协作，工单号：{}，客服ID：{}", command.workOrderNo(), agentId);

        return new WorkOrderResponse(
                ACTION_ASSIGN,
                command.workOrderNo(),
                agentId,
                true,
                "工单分配成功"
        );
    }

    /**
     * 校验工单协作命令
     *
     * @param command 工单协作命令
     */
    private void validateCommand(WorkOrderCommand command) {
        if (command == null) {
            log.warn("处理工单协作失败，命令为空");
            throw new IllegalArgumentException("工单协作命令不能为空");
        }

        if (StrUtil.isBlank(command.action())) {
            log.warn("处理工单协作失败，操作类型为空");
            throw new IllegalArgumentException("操作类型不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/WorkOrderController.java`

下面是工单接口，用于验证中介者模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.WorkOrderCommand;
import io.github.atengk.design.dto.WorkOrderResponse;
import io.github.atengk.design.mediator.WorkOrderMediator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 工单控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mediator/work-order")
public class WorkOrderController {

    private final WorkOrderMediator workOrderMediator;

    /**
     * 创建工单
     *
     * @param title   工单标题
     * @param userId  用户ID
     * @param content 工单内容
     * @return 工单协作响应
     */
    @PostMapping("/create")
    public WorkOrderResponse create(@RequestParam String title,
                                    @RequestParam Long userId,
                                    @RequestParam String content) {
        WorkOrderCommand command = new WorkOrderCommand(
                "create",
                null,
                title,
                userId,
                null,
                content
        );

        return workOrderMediator.handle(command);
    }

    /**
     * 分配工单
     *
     * @param workOrderNo 工单号
     * @param agentId     客服ID
     * @return 工单协作响应
     */
    @PostMapping("/assign")
    public WorkOrderResponse assign(@RequestParam String workOrderNo,
                                    @RequestParam Long agentId) {
        WorkOrderCommand command = new WorkOrderCommand(
                "assign",
                workOrderNo,
                null,
                null,
                agentId,
                null
        );

        return workOrderMediator.handle(command);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/mediator/work-order/create?title=订单支付异常&userId=10001&content=用户反馈订单支付后状态未更新"

curl -X POST "http://localhost:8080/mediator/work-order/assign?workOrderNo=WO2019776866538487808&agentId=90001"
```

创建工单可能返回：

```json
{
  "action": "create",
  "workOrderNo": "WO2019776866538487808",
  "agentId": 90001,
  "success": true,
  "message": "工单创建成功"
}
```

在这个结构中，`WorkOrderService`、`AgentService`、`NoticeService`、`AuditService` 不互相注入。它们之间的交互关系统一由 `DefaultWorkOrderMediator` 管理。

## 扩展新的协作动作

在中介者模式中，扩展新协作动作通常是在中介者中新增协调逻辑，或者将动作处理拆成独立处理器。下面以“关闭工单”为例，给出简化扩展方式。

先在工单服务中新增关闭工单方法。

文件位置：`src/main/java/io/github/atengk/design/service/WorkOrderService.java`

```java
/**
 * 关闭工单
 *
 * @param workOrderNo 工单号
 */
void close(String workOrderNo);
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/WorkOrderServiceImpl.java`

```java
/**
 * 关闭工单
 *
 * @param workOrderNo 工单号
 */
@Override
public void close(String workOrderNo) {
    if (StrUtil.isBlank(workOrderNo)) {
        log.warn("关闭工单失败，工单号为空");
        throw new IllegalArgumentException("工单号不能为空");
    }

    log.info("关闭工单成功，工单号：{}", workOrderNo);
}
```

然后在中介者中增加动作常量和分支：

```java
private static final String ACTION_CLOSE = "close";
if (ACTION_CLOSE.equals(action)) {
    return closeWorkOrder(command);
}
```

新增关闭工单协调方法：

```java
/**
 * 关闭工单
 *
 * @param command 工单协作命令
 * @return 工单协作响应
 */
private WorkOrderResponse closeWorkOrder(WorkOrderCommand command) {
    if (StrUtil.isBlank(command.workOrderNo())) {
        log.warn("关闭工单失败，工单号为空");
        throw new IllegalArgumentException("工单号不能为空");
    }

    workOrderService.close(command.workOrderNo());
    auditService.record(ACTION_CLOSE, command.workOrderNo(), command.userId());

    log.info("中介者完成工单关闭协作，工单号：{}", command.workOrderNo());

    return new WorkOrderResponse(
            ACTION_CLOSE,
            command.workOrderNo(),
            null,
            true,
            "工单关闭成功"
    );
}
```

如果中介者中的动作越来越多，建议不要继续在一个类里堆 `if else`。可以把“动作分发”与“协作逻辑”拆开，组合命令模式或策略模式：

```text
WorkOrderMediator
    -> WorkOrderActionHandler
        -> CreateWorkOrderHandler
        -> AssignWorkOrderHandler
        -> CloseWorkOrderHandler
```

这种写法可以避免中介者变成新的上帝类。

## 中介者模式和外观模式的区别

中介者模式和外观模式都可能表现为“一个类调用多个服务”，但二者意图不同。

| 对比项               | 中介者模式                 | 外观模式                     |
| -------------------- | -------------------------- | ---------------------------- |
| 核心目的             | 解耦多个对象之间的相互交互 | 给复杂子系统提供简单入口     |
| 对象关系             | 多个同事对象通过中介者通信 | 调用方通过门面调用多个子系统 |
| 关注点               | 对象之间如何协作           | 调用方如何更简单地使用系统   |
| 是否强调对象互相解耦 | 强调                       | 不一定强调                   |
| 典型场景             | 聊天室、组件联动、工单协同 | 下单门面、报表导出、文件处理 |

简单理解：

```text
中介者模式：对象之间不要互相找对方，统一找中介者。
外观模式：调用方不要了解复杂子系统，统一找门面入口。
```

如果重点是减少多个对象之间的互相依赖，使用中介者模式。如果重点是给外部调用方提供一个简洁入口，使用外观模式。

## 中介者模式和观察者模式的区别

中介者模式和观察者模式都可以用于对象间通信，但通信方式不同。

| 对比项   | 中介者模式                   | 观察者模式                           |
| -------- | ---------------------------- | ------------------------------------ |
| 核心目的 | 集中协调对象交互             | 事件发布后通知订阅者                 |
| 通信方向 | 通常由中介者主动协调         | 发布者不关心订阅者                   |
| 控制逻辑 | 中介者掌握协作流程           | 观察者各自响应事件                   |
| 耦合关系 | 同事对象依赖中介者           | 发布者依赖事件机制或主题             |
| 典型场景 | 聊天室、UI组件联动、工单协同 | 用户注册事件、订单支付事件、消息订阅 |

简单理解：

```text
中介者模式：中介者知道谁该和谁协作。
观察者模式：发布事件，谁订阅谁处理。
```

如果需要一个中心对象明确控制协作顺序和参与者，使用中介者模式。如果只是某个事件发生后通知多个监听器，使用观察者模式。

## 中介者模式和责任链模式的区别

中介者模式和责任链模式都能降低主流程复杂度，但解决的问题不同。

| 对比项       | 中介者模式                 | 责任链模式                     |
| ------------ | -------------------------- | ------------------------------ |
| 核心目的     | 协调多个对象之间的交互     | 多个处理器按顺序处理同一个请求 |
| 结构关系     | 多个对象围绕一个中介者协作 | 多个处理器组成链               |
| 是否强调顺序 | 可有顺序，但不是核心       | 强调执行顺序                   |
| 是否强调中断 | 不强调                     | 强调中断或放行                 |
| 典型场景     | 工单协同、聊天室、组件联动 | 参数校验、风控链、过滤链       |

简单理解：

```text
中介者模式：多个对象之间怎么协作。
责任链模式：一个请求经过哪些关卡。
```

工单创建后要协调客服、通知、审计，适合中介者模式。订单提交前依次校验参数、库存、金额、风控，适合责任链模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行创建工单接口：

```bash
curl -X POST "http://localhost:8080/mediator/work-order/create?title=订单支付异常&userId=10001&content=用户反馈订单支付后状态未更新"
```

执行分配工单接口：

```bash
curl -X POST "http://localhost:8080/mediator/work-order/assign?workOrderNo=WO2019776866538487808&agentId=90001"
```

如果中介者模式正常，可以看到类似日志：

```text
创建工单成功，工单号：WO2019776866538487808，用户ID：10001，标题：订单支付异常
查询可用客服成功，客服ID：90001
分配工单成功，工单号：WO2019776866538487808，客服ID：90001
发送工单创建通知，用户ID：10001，工单号：WO2019776866538487808
发送工单分配通知，客服ID：90001，工单号：WO2019776866538487808
记录工单审计日志，操作：create，工单号：WO2019776866538487808，操作人ID：10001
中介者完成工单创建协作，工单号：WO2019776866538487808，用户ID：10001，客服ID：90001
```

执行非法请求：

```bash
curl -X POST "http://localhost:8080/mediator/work-order/assign?workOrderNo=WO2019776866538487808&agentId=0"
```

异常日志示例：

```text
客服校验失败，客服ID不合法，客服ID：0
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

中介者模式适合多个对象之间交互复杂的场景，但不要把所有业务都塞进中介者。中介者应该负责协调，而不是替代所有业务服务。

适合使用中介者模式的场景：

```text
多个对象之间互相依赖严重
对象协作流程经常变化
想减少对象之间直接注入
需要集中控制协作规则
页面组件或业务组件需要统一协调
```

不太适合使用中介者模式的场景：

```text
对象之间交互很简单
只有单向服务调用
只是封装一个流程入口
中介者会变成巨大流程类
普通外观模式即可解决
```

不推荐多个服务互相注入形成网状依赖：

```java
@Service
public class WorkOrderServiceImpl {

    private final NoticeService noticeService;
    private final AuditService auditService;
    private final AgentService agentService;
}
```

推荐由中介者集中协调：

```java
@Component
public class DefaultWorkOrderMediator {

    private final WorkOrderService workOrderService;
    private final NoticeService noticeService;
    private final AuditService auditService;
    private final AgentService agentService;
}
```

但中介者也不能无限膨胀。如果一个中介者中出现大量动作分支、复杂状态判断和长事务流程，需要继续拆分。

不推荐：

```java
public WorkOrderResponse handle(WorkOrderCommand command) {
    // create 逻辑 100 行
    // assign 逻辑 100 行
    // close 逻辑 100 行
    // reopen 逻辑 100 行
    // transfer 逻辑 100 行
    return null;
}
```

推荐结合命令模式或策略模式拆分：

```text
WorkOrderMediator
    -> CreateWorkOrderHandler
    -> AssignWorkOrderHandler
    -> CloseWorkOrderHandler
```

Spring Bean 默认是单例，中介者中不要保存请求级状态。

错误示例：

```java
private String currentWorkOrderNo;
private Long currentUserId;
private Long currentAgentId;
```

推荐使用方法参数和局部变量：

```java
public WorkOrderResponse handle(WorkOrderCommand command) {
    String workOrderNo = workOrderService.create(command.title(), command.userId(), command.content());
    return buildResponse(workOrderNo);
}
```

如果中介者协调的是核心业务流程，例如订单、支付、工单、审批，需要额外考虑事务边界、幂等、并发、消息可靠投递和失败补偿。中介者模式只解决对象协作结构问题，不自动保证业务一致性。

生产环境中常见关注点包括：

```text
中介者是否过度集中
协作流程是否可测试
异常后是否需要补偿
通知失败是否影响主流程
审计失败是否阻断操作
多服务调用是否需要事务
是否需要异步消息解耦
```

如果某些协作动作不是强一致要求，例如通知发送、审计记录，可以考虑通过 MQ 或应用事件异步处理，避免中介者同步调用过多外部服务。

## 总结

在 JDK21 和 Spring Boot 3 项目中，中介者模式的实践重点是把多个对象之间的复杂交互集中到中介者中，让各业务对象只关注自身职责，避免互相直接依赖。

普通 Java 中介者适合理解聊天室、组件联动等对象协作问题。Spring Boot 项目中更推荐使用“中介者接口 + 具体中介者 Bean + 多个独立业务服务”的结构。对于工单协同、审批协同、页面组件交互、任务调度协调等场景，中介者模式可以减少网状依赖，让对象协作关系更清晰。

中介者模式不是为了替代所有 Service 编排，也不是为了把所有流程都集中到一个类中。它最适合处理“多个对象之间交互复杂，并且需要统一协调规则”的场景。实际落地时，需要控制中介者职责边界，必要时结合命令模式、策略模式、事件机制或消息队列，避免中介者演变成新的上帝类。
