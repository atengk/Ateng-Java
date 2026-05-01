# 设计模式：备忘录模式

备忘录模式用于在不破坏对象封装性的前提下，保存对象某一时刻的内部状态，并在需要时恢复到之前状态。在 JDK21 和 Spring Boot 3 项目中，备忘录模式常用于草稿回滚、编辑器撤销、配置版本恢复、审批表单快照、规则配置回滚、订单操作前状态保存、流程设计器版本管理等场景。

需要注意：备忘录模式关注的是“保存状态并恢复状态”。如果只是记录操作日志，不一定是备忘录模式；如果只是复制模板对象，更适合原型模式；如果是把操作封装成可撤销动作，更适合命令模式；如果需要保存对象历史状态并支持回滚，备忘录模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证备忘录模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、时间等通用处理 -->
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

备忘录模式的核心目标是把对象的历史状态保存到一个备忘录对象中，由管理者保存备忘录，需要回滚时再把备忘录交回原对象恢复状态。

常见角色如下：

| 角色       | 说明                                       |
| ---------- | ------------------------------------------ |
| Originator | 发起人，需要保存和恢复状态的业务对象       |
| Memento    | 备忘录，保存发起人某一时刻的状态           |
| Caretaker  | 管理者，负责保存备忘录，但不修改备忘录内容 |
| Client     | 调用方，触发保存、修改、恢复等操作         |

典型结构如下：

```text
调用方
    -> Originator.createMemento()
        -> Caretaker.save(memento)

调用方
    -> Caretaker.get(snapshotId)
        -> Originator.restore(memento)
```

备忘录模式强调封装性。理论上，`Caretaker` 不应该直接理解或修改 `Memento` 内部状态，只负责保存和取出。实际 Java 项目中常用 `record`、不可变类或只读对象表示备忘录，避免被外部随意修改。

备忘录模式和普通日志的区别在于：

```text
操作日志：记录发生了什么。
备忘录：保存当时是什么状态，并允许恢复。
```

在 Spring Boot 项目中，常见优先级通常是：

```text
不可变 Memento + Caretaker 管理历史 > 直接暴露对象字段快照 > 手写大量回滚字段
```

备忘录模式适合需要版本恢复、撤销、回滚的场景。对于核心业务，需要结合数据库事务、版本号、操作审计和权限校验使用。

## 普通 Java 备忘录模式

普通 Java 备忘录模式适合不依赖 Spring 容器的本地状态保存和恢复。下面以文本编辑器为例，编辑器可以写入内容、保存快照、撤销到上一个快照。

整体流程如下：

```text
创建编辑器 -> 编辑内容 -> 保存快照 -> 再次编辑 -> 保存快照 -> 撤销恢复
```

### 文件结构

```text
src/main/java/io/github/atengk/design/memento/simple/
├── TextEditorMemento.java
├── TextEditor.java
└── TextEditorHistory.java
```

文件位置：`src/main/java/io/github/atengk/design/memento/simple/TextEditorMemento.java`

下面是文本编辑器备忘录对象。它保存编辑器某一时刻的内容、光标位置和保存时间。

```java
package io.github.atengk.design.memento.simple;

import java.time.LocalDateTime;

/**
 * 文本编辑器备忘录
 *
 * @param content        文本内容
 * @param cursorPosition 光标位置
 * @param saveTime       保存时间
 * @author Ateng
 * @since 2026-04-30
 */
public record TextEditorMemento(
        String content,
        Integer cursorPosition,
        LocalDateTime saveTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/memento/simple/TextEditor.java`

下面是文本编辑器对象，也就是备忘录模式中的发起人。它负责创建快照和恢复快照。

```java
package io.github.atengk.design.memento.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 文本编辑器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class TextEditor {

    private String content = "";
    private Integer cursorPosition = 0;

    /**
     * 写入文本
     *
     * @param text 文本内容
     */
    public void write(String text) {
        if (text == null) {
            log.warn("写入文本失败，文本内容为空");
            throw new IllegalArgumentException("文本内容不能为空");
        }

        this.content = StrUtil.format("{}{}", this.content, text);
        this.cursorPosition = this.content.length();

        log.info("写入文本成功，当前长度：{}，光标位置：{}", this.content.length(), this.cursorPosition);
    }

    /**
     * 替换全部文本
     *
     * @param content 新文本内容
     */
    public void replace(String content) {
        if (content == null) {
            log.warn("替换文本失败，文本内容为空");
            throw new IllegalArgumentException("文本内容不能为空");
        }

        this.content = content;
        this.cursorPosition = content.length();

        log.info("替换文本成功，当前长度：{}，光标位置：{}", this.content.length(), this.cursorPosition);
    }

    /**
     * 创建备忘录
     *
     * @return 文本编辑器备忘录
     */
    public TextEditorMemento createMemento() {
        TextEditorMemento memento = new TextEditorMemento(
                this.content,
                this.cursorPosition,
                LocalDateTime.now()
        );

        log.info("创建文本编辑器备忘录成功，内容长度：{}，光标位置：{}",
                this.content.length(), this.cursorPosition);
        return memento;
    }

    /**
     * 从备忘录恢复状态
     *
     * @param memento 文本编辑器备忘录
     */
    public void restore(TextEditorMemento memento) {
        if (memento == null) {
            log.warn("恢复文本编辑器失败，备忘录为空");
            throw new IllegalArgumentException("备忘录不能为空");
        }

        this.content = memento.content();
        this.cursorPosition = memento.cursorPosition();

        log.info("恢复文本编辑器成功，内容长度：{}，光标位置：{}，快照时间：{}",
                this.content.length(), this.cursorPosition, memento.saveTime());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/memento/simple/TextEditorHistory.java`

下面是文本编辑器历史管理器，也就是备忘录模式中的管理者。它只负责保存和取出快照，不直接修改编辑器内容。

```java
package io.github.atengk.design.memento.simple;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 文本编辑器历史管理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class TextEditorHistory {

    private final Deque<TextEditorMemento> history = new ArrayDeque<>();

    /**
     * 保存快照
     *
     * @param memento 文本编辑器备忘录
     */
    public void save(TextEditorMemento memento) {
        if (memento == null) {
            log.warn("保存文本快照失败，备忘录为空");
            throw new IllegalArgumentException("备忘录不能为空");
        }

        history.push(memento);
        log.info("保存文本快照成功，历史数量：{}", history.size());
    }

    /**
     * 获取最近一次快照
     *
     * @return 文本编辑器备忘录
     */
    public TextEditorMemento popLatest() {
        if (history.isEmpty()) {
            log.warn("获取文本快照失败，历史快照为空");
            throw new IllegalStateException("没有可恢复的历史快照");
        }

        TextEditorMemento memento = history.pop();
        log.info("获取最近文本快照成功，剩余历史数量：{}", history.size());
        return memento;
    }

    /**
     * 获取历史数量
     *
     * @return 历史数量
     */
    public int size() {
        return history.size();
    }
}
```

使用方式：

```java
TextEditor editor = new TextEditor();
TextEditorHistory history = new TextEditorHistory();

editor.write("第一段内容");
history.save(editor.createMemento());

editor.write("，第二段内容");
history.save(editor.createMemento());

editor.write("，错误内容");

editor.restore(history.popLatest());
```

执行后，编辑器会恢复到保存第二次快照时的状态。`TextEditorHistory` 不需要知道文本编辑器内部如何保存内容，它只负责管理 `TextEditorMemento`。

## Spring Boot 备忘录模式

Spring Boot 项目中，备忘录模式更常用于草稿、配置、规则、表单等业务对象的版本快照。下面以文档草稿为例，用户可以创建文档、编辑文档、保存快照、恢复到指定快照。

整体流程如下：

```text
Controller
    -> DocumentDraftService
        -> DocumentStore 保存当前文档
        -> DocumentSnapshotManager 保存历史快照
        -> DocumentEditor 创建和恢复备忘录
```

示例中使用内存 Map 模拟数据库存储。实际项目中可以将当前文档保存到业务表，将快照保存到历史表。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── MementoApplication.java
├── controller/
│   └── DocumentDraftController.java
├── dto/
│   ├── DocumentCreateRequest.java
│   ├── DocumentEditRequest.java
│   ├── DocumentRestoreRequest.java
│   ├── DocumentResponse.java
│   └── DocumentSnapshotResponse.java
├── memento/
│   ├── DocumentSnapshot.java
│   ├── DocumentEditor.java
│   └── DocumentSnapshotManager.java
├── store/
│   └── DocumentMemoryStore.java
└── service/
    ├── DocumentDraftService.java
    └── impl/
        └── DocumentDraftServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/MementoApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 备忘录模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class MementoApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MementoApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/DocumentCreateRequest.java`

下面是文档创建请求对象。

```java
package io.github.atengk.design.dto;

/**
 * 文档创建请求
 *
 * @param title      文档标题
 * @param content    文档内容
 * @param operatorId 操作人ID
 * @author Ateng
 * @since 2026-04-30
 */
public record DocumentCreateRequest(
        String title,
        String content,
        Long operatorId
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/DocumentEditRequest.java`

下面是文档编辑请求对象。

```java
package io.github.atengk.design.dto;

/**
 * 文档编辑请求
 *
 * @param documentId 文档ID
 * @param title      文档标题
 * @param content    文档内容
 * @param operatorId 操作人ID
 * @author Ateng
 * @since 2026-04-30
 */
public record DocumentEditRequest(
        Long documentId,
        String title,
        String content,
        Long operatorId
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/DocumentRestoreRequest.java`

下面是文档恢复请求对象。

```java
package io.github.atengk.design.dto;

/**
 * 文档恢复请求
 *
 * @param documentId 文档ID
 * @param snapshotId 快照ID
 * @param operatorId 操作人ID
 * @author Ateng
 * @since 2026-04-30
 */
public record DocumentRestoreRequest(
        Long documentId,
        String snapshotId,
        Long operatorId
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/DocumentResponse.java`

下面是文档响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 文档响应
 *
 * @param documentId 文档ID
 * @param title      文档标题
 * @param content    文档内容
 * @param version    文档版本
 * @param message    响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record DocumentResponse(
        Long documentId,
        String title,
        String content,
        Integer version,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/DocumentSnapshotResponse.java`

下面是文档快照响应对象。

```java
package io.github.atengk.design.dto;

import java.time.LocalDateTime;

/**
 * 文档快照响应
 *
 * @param snapshotId 快照ID
 * @param documentId 文档ID
 * @param title      快照标题
 * @param version    快照版本
 * @param operatorId 操作人ID
 * @param createTime 创建时间
 * @author Ateng
 * @since 2026-04-30
 */
public record DocumentSnapshotResponse(
        String snapshotId,
        Long documentId,
        String title,
        Integer version,
        Long operatorId,
        LocalDateTime createTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/memento/DocumentSnapshot.java`

下面是文档快照对象，也就是备忘录。它使用 `record` 表示不可变快照，避免历史状态被修改。

```java
package io.github.atengk.design.memento;

import java.time.LocalDateTime;

/**
 * 文档快照备忘录
 *
 * @param snapshotId 快照ID
 * @param documentId 文档ID
 * @param title      文档标题
 * @param content    文档内容
 * @param version    文档版本
 * @param operatorId 操作人ID
 * @param createTime 创建时间
 * @author Ateng
 * @since 2026-04-30
 */
public record DocumentSnapshot(
        String snapshotId,
        Long documentId,
        String title,
        String content,
        Integer version,
        Long operatorId,
        LocalDateTime createTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/memento/DocumentEditor.java`

下面是文档编辑器，也就是发起人。它负责创建快照和从快照恢复文档状态。

```java
package io.github.atengk.design.memento;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 文档编辑器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class DocumentEditor {

    private final Long documentId;
    private String title;
    private String content;
    private Integer version;

    /**
     * 创建文档编辑器
     *
     * @param documentId 文档ID
     * @param title      文档标题
     * @param content    文档内容
     * @param version    文档版本
     */
    public DocumentEditor(Long documentId, String title, String content, Integer version) {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("文档ID必须大于0");
        }

        if (StrUtil.isBlank(title)) {
            throw new IllegalArgumentException("文档标题不能为空");
        }

        this.documentId = documentId;
        this.title = title;
        this.content = StrUtil.nullToDefault(content, "");
        this.version = version == null || version <= 0 ? 1 : version;
    }

    /**
     * 编辑文档
     *
     * @param title   文档标题
     * @param content 文档内容
     */
    public void edit(String title, String content) {
        if (StrUtil.isBlank(title)) {
            log.warn("编辑文档失败，标题为空，文档ID：{}", documentId);
            throw new IllegalArgumentException("文档标题不能为空");
        }

        this.title = title;
        this.content = StrUtil.nullToDefault(content, "");
        this.version = this.version + 1;

        log.info("编辑文档成功，文档ID：{}，版本：{}", documentId, version);
    }

    /**
     * 创建文档快照
     *
     * @param operatorId 操作人ID
     * @return 文档快照
     */
    public DocumentSnapshot createSnapshot(Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            log.warn("创建文档快照失败，操作人ID不合法，文档ID：{}，操作人ID：{}", documentId, operatorId);
            throw new IllegalArgumentException("操作人ID必须大于0");
        }

        DocumentSnapshot snapshot = new DocumentSnapshot(
                "SNAP" + IdUtil.getSnowflakeNextId(),
                documentId,
                title,
                content,
                version,
                operatorId,
                LocalDateTime.now()
        );

        log.info("创建文档快照成功，文档ID：{}，快照ID：{}，版本：{}",
                documentId, snapshot.snapshotId(), version);
        return snapshot;
    }

    /**
     * 从文档快照恢复状态
     *
     * @param snapshot 文档快照
     */
    public void restore(DocumentSnapshot snapshot) {
        if (snapshot == null) {
            log.warn("恢复文档失败，快照为空，文档ID：{}", documentId);
            throw new IllegalArgumentException("文档快照不能为空");
        }

        if (!documentId.equals(snapshot.documentId())) {
            log.warn("恢复文档失败，快照文档ID不匹配，当前文档ID：{}，快照文档ID：{}",
                    documentId, snapshot.documentId());
            throw new IllegalArgumentException("快照不属于当前文档");
        }

        this.title = snapshot.title();
        this.content = snapshot.content();
        this.version = snapshot.version();

        log.info("恢复文档成功，文档ID：{}，快照ID：{}，恢复版本：{}",
                documentId, snapshot.snapshotId(), snapshot.version());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/memento/DocumentSnapshotManager.java`

下面是文档快照管理器，也就是管理者。它只负责保存和查询快照，不修改快照内容。

```java
package io.github.atengk.design.memento;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档快照管理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class DocumentSnapshotManager {

    private final Map<Long, List<DocumentSnapshot>> snapshotMap = new ConcurrentHashMap<>();

    /**
     * 保存文档快照
     *
     * @param snapshot 文档快照
     */
    public void save(DocumentSnapshot snapshot) {
        if (snapshot == null) {
            log.warn("保存文档快照失败，快照为空");
            throw new IllegalArgumentException("文档快照不能为空");
        }

        snapshotMap.computeIfAbsent(snapshot.documentId(), key -> new ArrayList<>()).add(snapshot);
        log.info("保存文档快照成功，文档ID：{}，快照ID：{}，当前快照数量：{}",
                snapshot.documentId(), snapshot.snapshotId(), count(snapshot.documentId()));
    }

    /**
     * 查询文档快照列表
     *
     * @param documentId 文档ID
     * @return 文档快照列表
     */
    public List<DocumentSnapshot> list(Long documentId) {
        if (documentId == null || documentId <= 0) {
            log.warn("查询文档快照失败，文档ID不合法，文档ID：{}", documentId);
            throw new IllegalArgumentException("文档ID必须大于0");
        }

        return CollUtil.emptyIfNull(snapshotMap.get(documentId)).stream()
                .sorted(Comparator.comparing(DocumentSnapshot::createTime).reversed())
                .toList();
    }

    /**
     * 获取指定文档快照
     *
     * @param documentId 文档ID
     * @param snapshotId 快照ID
     * @return 文档快照
     */
    public DocumentSnapshot get(Long documentId, String snapshotId) {
        if (documentId == null || documentId <= 0) {
            log.warn("获取文档快照失败，文档ID不合法，文档ID：{}", documentId);
            throw new IllegalArgumentException("文档ID必须大于0");
        }

        if (StrUtil.isBlank(snapshotId)) {
            log.warn("获取文档快照失败，快照ID为空，文档ID：{}", documentId);
            throw new IllegalArgumentException("快照ID不能为空");
        }

        return list(documentId).stream()
                .filter(snapshot -> snapshot.snapshotId().equals(snapshotId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("获取文档快照失败，快照不存在，文档ID：{}，快照ID：{}", documentId, snapshotId);
                    return new IllegalArgumentException("文档快照不存在：" + snapshotId);
                });
    }

    /**
     * 获取文档快照数量
     *
     * @param documentId 文档ID
     * @return 快照数量
     */
    public int count(Long documentId) {
        return CollUtil.size(snapshotMap.get(documentId));
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/store/DocumentMemoryStore.java`

下面是文档内存存储组件，用于模拟当前文档持久化。生产环境中可以替换为数据库表。

```java
package io.github.atengk.design.store;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.memento.DocumentEditor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档内存存储
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class DocumentMemoryStore {

    private final Map<Long, DocumentEditor> documentMap = new ConcurrentHashMap<>();

    /**
     * 创建文档
     *
     * @param title   文档标题
     * @param content 文档内容
     * @return 文档编辑器
     */
    public DocumentEditor create(String title, String content) {
        if (StrUtil.isBlank(title)) {
            log.warn("创建文档失败，标题为空");
            throw new IllegalArgumentException("文档标题不能为空");
        }

        Long documentId = IdUtil.getSnowflakeNextId();
        DocumentEditor editor = new DocumentEditor(documentId, title, content, 1);
        documentMap.put(documentId, editor);

        log.info("创建文档成功，文档ID：{}，标题：{}", documentId, title);
        return editor;
    }

    /**
     * 保存文档
     *
     * @param editor 文档编辑器
     */
    public void save(DocumentEditor editor) {
        if (editor == null) {
            log.warn("保存文档失败，文档为空");
            throw new IllegalArgumentException("文档不能为空");
        }

        documentMap.put(editor.getDocumentId(), editor);
        log.info("保存文档成功，文档ID：{}，版本：{}", editor.getDocumentId(), editor.getVersion());
    }

    /**
     * 获取文档
     *
     * @param documentId 文档ID
     * @return 文档编辑器
     */
    public DocumentEditor get(Long documentId) {
        if (documentId == null || documentId <= 0) {
            log.warn("获取文档失败，文档ID不合法，文档ID：{}", documentId);
            throw new IllegalArgumentException("文档ID必须大于0");
        }

        DocumentEditor editor = documentMap.get(documentId);
        if (editor == null) {
            log.warn("获取文档失败，文档不存在，文档ID：{}", documentId);
            throw new IllegalArgumentException("文档不存在：" + documentId);
        }

        return editor;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/DocumentDraftService.java`

下面是文档草稿服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.DocumentCreateRequest;
import io.github.atengk.design.dto.DocumentEditRequest;
import io.github.atengk.design.dto.DocumentResponse;
import io.github.atengk.design.dto.DocumentRestoreRequest;
import io.github.atengk.design.dto.DocumentSnapshotResponse;

import java.util.List;

/**
 * 文档草稿服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface DocumentDraftService {

    /**
     * 创建文档
     *
     * @param request 文档创建请求
     * @return 文档响应
     */
    DocumentResponse create(DocumentCreateRequest request);

    /**
     * 编辑文档并保存编辑前快照
     *
     * @param request 文档编辑请求
     * @return 文档响应
     */
    DocumentResponse edit(DocumentEditRequest request);

    /**
     * 恢复文档到指定快照
     *
     * @param request 文档恢复请求
     * @return 文档响应
     */
    DocumentResponse restore(DocumentRestoreRequest request);

    /**
     * 查询文档快照列表
     *
     * @param documentId 文档ID
     * @return 快照列表
     */
    List<DocumentSnapshotResponse> listSnapshots(Long documentId);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/DocumentDraftServiceImpl.java`

下面是文档草稿服务实现。编辑前会先创建快照，恢复时会从快照管理器取出快照并恢复文档状态。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.DocumentCreateRequest;
import io.github.atengk.design.dto.DocumentEditRequest;
import io.github.atengk.design.dto.DocumentResponse;
import io.github.atengk.design.dto.DocumentRestoreRequest;
import io.github.atengk.design.dto.DocumentSnapshotResponse;
import io.github.atengk.design.memento.DocumentEditor;
import io.github.atengk.design.memento.DocumentSnapshot;
import io.github.atengk.design.memento.DocumentSnapshotManager;
import io.github.atengk.design.service.DocumentDraftService;
import io.github.atengk.design.store.DocumentMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档草稿服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDraftServiceImpl implements DocumentDraftService {

    private final DocumentMemoryStore documentMemoryStore;
    private final DocumentSnapshotManager documentSnapshotManager;

    /**
     * 创建文档
     *
     * @param request 文档创建请求
     * @return 文档响应
     */
    @Override
    public DocumentResponse create(DocumentCreateRequest request) {
        validateCreateRequest(request);

        DocumentEditor editor = documentMemoryStore.create(request.title(), request.content());
        DocumentSnapshot snapshot = editor.createSnapshot(request.operatorId());
        documentSnapshotManager.save(snapshot);

        log.info("创建文档并保存初始快照完成，文档ID：{}，快照ID：{}",
                editor.getDocumentId(), snapshot.snapshotId());

        return toResponse(editor, "创建成功");
    }

    /**
     * 编辑文档并保存编辑前快照
     *
     * @param request 文档编辑请求
     * @return 文档响应
     */
    @Override
    public DocumentResponse edit(DocumentEditRequest request) {
        validateEditRequest(request);

        DocumentEditor editor = documentMemoryStore.get(request.documentId());

        DocumentSnapshot beforeSnapshot = editor.createSnapshot(request.operatorId());
        documentSnapshotManager.save(beforeSnapshot);

        editor.edit(request.title(), request.content());
        documentMemoryStore.save(editor);

        log.info("编辑文档完成，文档ID：{}，编辑前快照ID：{}，当前版本：{}",
                editor.getDocumentId(), beforeSnapshot.snapshotId(), editor.getVersion());

        return toResponse(editor, "编辑成功");
    }

    /**
     * 恢复文档到指定快照
     *
     * @param request 文档恢复请求
     * @return 文档响应
     */
    @Override
    public DocumentResponse restore(DocumentRestoreRequest request) {
        validateRestoreRequest(request);

        DocumentEditor editor = documentMemoryStore.get(request.documentId());

        DocumentSnapshot beforeRestoreSnapshot = editor.createSnapshot(request.operatorId());
        documentSnapshotManager.save(beforeRestoreSnapshot);

        DocumentSnapshot targetSnapshot = documentSnapshotManager.get(request.documentId(), request.snapshotId());
        editor.restore(targetSnapshot);
        documentMemoryStore.save(editor);

        log.info("恢复文档完成，文档ID：{}，目标快照ID：{}，恢复前快照ID：{}",
                request.documentId(), request.snapshotId(), beforeRestoreSnapshot.snapshotId());

        return toResponse(editor, "恢复成功");
    }

    /**
     * 查询文档快照列表
     *
     * @param documentId 文档ID
     * @return 快照列表
     */
    @Override
    public List<DocumentSnapshotResponse> listSnapshots(Long documentId) {
        return documentSnapshotManager.list(documentId).stream()
                .map(snapshot -> new DocumentSnapshotResponse(
                        snapshot.snapshotId(),
                        snapshot.documentId(),
                        snapshot.title(),
                        snapshot.version(),
                        snapshot.operatorId(),
                        snapshot.createTime()
                ))
                .toList();
    }

    /**
     * 转换为文档响应
     *
     * @param editor  文档编辑器
     * @param message 响应消息
     * @return 文档响应
     */
    private DocumentResponse toResponse(DocumentEditor editor, String message) {
        return new DocumentResponse(
                editor.getDocumentId(),
                editor.getTitle(),
                editor.getContent(),
                editor.getVersion(),
                message
        );
    }

    /**
     * 校验文档创建请求
     *
     * @param request 文档创建请求
     */
    private void validateCreateRequest(DocumentCreateRequest request) {
        if (request == null) {
            log.warn("创建文档失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.title())) {
            log.warn("创建文档失败，标题为空");
            throw new IllegalArgumentException("文档标题不能为空");
        }

        validateOperatorId(request.operatorId());
    }

    /**
     * 校验文档编辑请求
     *
     * @param request 文档编辑请求
     */
    private void validateEditRequest(DocumentEditRequest request) {
        if (request == null) {
            log.warn("编辑文档失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.documentId() == null || request.documentId() <= 0) {
            log.warn("编辑文档失败，文档ID不合法，文档ID：{}", request.documentId());
            throw new IllegalArgumentException("文档ID必须大于0");
        }

        if (StrUtil.isBlank(request.title())) {
            log.warn("编辑文档失败，标题为空，文档ID：{}", request.documentId());
            throw new IllegalArgumentException("文档标题不能为空");
        }

        validateOperatorId(request.operatorId());
    }

    /**
     * 校验文档恢复请求
     *
     * @param request 文档恢复请求
     */
    private void validateRestoreRequest(DocumentRestoreRequest request) {
        if (request == null) {
            log.warn("恢复文档失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.documentId() == null || request.documentId() <= 0) {
            log.warn("恢复文档失败，文档ID不合法，文档ID：{}", request.documentId());
            throw new IllegalArgumentException("文档ID必须大于0");
        }

        if (StrUtil.isBlank(request.snapshotId())) {
            log.warn("恢复文档失败，快照ID为空，文档ID：{}", request.documentId());
            throw new IllegalArgumentException("快照ID不能为空");
        }

        validateOperatorId(request.operatorId());
    }

    /**
     * 校验操作人ID
     *
     * @param operatorId 操作人ID
     */
    private void validateOperatorId(Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            log.warn("文档操作失败，操作人ID不合法，操作人ID：{}", operatorId);
            throw new IllegalArgumentException("操作人ID必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/DocumentDraftController.java`

下面是文档草稿接口，用于验证备忘录模式的保存快照和恢复快照能力。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.DocumentCreateRequest;
import io.github.atengk.design.dto.DocumentEditRequest;
import io.github.atengk.design.dto.DocumentResponse;
import io.github.atengk.design.dto.DocumentRestoreRequest;
import io.github.atengk.design.dto.DocumentSnapshotResponse;
import io.github.atengk.design.service.DocumentDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档草稿控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/memento/document")
public class DocumentDraftController {

    private final DocumentDraftService documentDraftService;

    /**
     * 创建文档
     *
     * @param title      文档标题
     * @param content    文档内容
     * @param operatorId 操作人ID
     * @return 文档响应
     */
    @PostMapping("/create")
    public DocumentResponse create(@RequestParam String title,
                                   @RequestParam String content,
                                   @RequestParam Long operatorId) {
        DocumentCreateRequest request = new DocumentCreateRequest(title, content, operatorId);
        return documentDraftService.create(request);
    }

    /**
     * 编辑文档
     *
     * @param documentId 文档ID
     * @param title      文档标题
     * @param content    文档内容
     * @param operatorId 操作人ID
     * @return 文档响应
     */
    @PostMapping("/edit")
    public DocumentResponse edit(@RequestParam Long documentId,
                                 @RequestParam String title,
                                 @RequestParam String content,
                                 @RequestParam Long operatorId) {
        DocumentEditRequest request = new DocumentEditRequest(documentId, title, content, operatorId);
        return documentDraftService.edit(request);
    }

    /**
     * 恢复文档
     *
     * @param documentId 文档ID
     * @param snapshotId 快照ID
     * @param operatorId 操作人ID
     * @return 文档响应
     */
    @PostMapping("/restore")
    public DocumentResponse restore(@RequestParam Long documentId,
                                    @RequestParam String snapshotId,
                                    @RequestParam Long operatorId) {
        DocumentRestoreRequest request = new DocumentRestoreRequest(documentId, snapshotId, operatorId);
        return documentDraftService.restore(request);
    }

    /**
     * 查询文档快照列表
     *
     * @param documentId 文档ID
     * @return 文档快照列表
     */
    @GetMapping("/snapshots")
    public List<DocumentSnapshotResponse> listSnapshots(@RequestParam Long documentId) {
        return documentDraftService.listSnapshots(documentId);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/memento/document/create?title=设计模式笔记&content=初始内容&operatorId=10001"

curl -X POST "http://localhost:8080/memento/document/edit?documentId=2019776866538487808&title=设计模式笔记&content=增加备忘录模式内容&operatorId=10001"

curl "http://localhost:8080/memento/document/snapshots?documentId=2019776866538487808"

curl -X POST "http://localhost:8080/memento/document/restore?documentId=2019776866538487808&snapshotId=SNAP2019776866538487809&operatorId=10001"
```

创建文档可能返回：

```json
{
  "documentId": 2019776866538487808,
  "title": "设计模式笔记",
  "content": "初始内容",
  "version": 1,
  "message": "创建成功"
}
```

快照列表可能返回：

```json
[
  {
    "snapshotId": "SNAP2019776866538487809",
    "documentId": 2019776866538487808,
    "title": "设计模式笔记",
    "version": 1,
    "operatorId": 10001,
    "createTime": "2026-04-30T10:20:30"
  }
]
```

这种方式的核心是：编辑前先保存旧状态，恢复前也保存当前状态。这样恢复本身也可以被追溯，甚至可以再次恢复到恢复前状态。

## 数据库落地方式

实际 Spring Boot 项目中，文档当前状态和历史快照通常会拆成两张表。当前状态放业务表，历史状态放快照表。备忘录模式中的 `Memento` 对应历史快照记录，`Caretaker` 对应快照管理服务。

示例表结构如下：

```sql
-- 文档当前状态表
CREATE TABLE document_draft (
    id BIGINT PRIMARY KEY COMMENT '文档ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content TEXT COMMENT '文档内容',
    version INT NOT NULL DEFAULT 1 COMMENT '当前版本',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) COMMENT='文档草稿表';

-- 文档快照表
CREATE TABLE document_snapshot (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    snapshot_id VARCHAR(64) NOT NULL COMMENT '快照ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    title VARCHAR(200) NOT NULL COMMENT '快照标题',
    content TEXT COMMENT '快照内容',
    version INT NOT NULL COMMENT '快照版本',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_snapshot_id (snapshot_id),
    KEY idx_document_id (document_id)
) COMMENT='文档快照表';
```

如果使用 MyBatis-Plus，可以将 `DocumentSnapshot` 对应到 `document_snapshot` 表，将 `DocumentEditor` 当前状态对应到 `document_draft` 表。编辑时推荐在事务中完成：

```text
查询当前文档
保存当前快照
更新文档内容和版本
提交事务
```

恢复时推荐在事务中完成：

```text
查询当前文档
保存恢复前快照
查询目标快照
用目标快照覆盖当前文档
提交事务
```

对于核心业务，不建议只保存在内存中。本文使用内存存储只是为了让示例更聚焦于备忘录模式结构。

## 扩展自动保存快照

在实际项目中，快照保存不一定只发生在用户手动操作时。对于文档、规则、配置类系统，常见做法是编辑前自动保存快照，或者定时保存草稿快照。

可以在 Service 中增加自动快照策略：

```text
每次编辑前保存快照
每隔指定时间保存快照
内容变化超过指定长度保存快照
用户点击发布前保存快照
重要字段变更前保存快照
```

示例中 `edit` 方法采用的是“编辑前保存快照”：

```java
DocumentSnapshot beforeSnapshot = editor.createSnapshot(request.operatorId());
documentSnapshotManager.save(beforeSnapshot);

editor.edit(request.title(), request.content());
documentMemoryStore.save(editor);
```

这种方式可以保证编辑失败之前已有旧状态可恢复。生产环境中还需要结合事务，避免“快照保存成功但文档更新失败”导致历史状态混乱。

如果快照很多，需要增加清理策略：

```text
每个文档最多保留最近 50 个快照
只保留最近 30 天快照
发布版本永久保留
普通自动保存快照定期清理
```

## 备忘录模式和命令模式的区别

备忘录模式和命令模式都可以支持撤销，但关注点不同。

| 对比项   | 备忘录模式                   | 命令模式                       |
| -------- | ---------------------------- | ------------------------------ |
| 核心目的 | 保存对象状态并恢复           | 封装操作并支持执行、撤销       |
| 撤销方式 | 恢复到历史快照               | 执行反向操作或补偿操作         |
| 保存内容 | 对象状态                     | 操作本身和操作参数             |
| 适合场景 | 文档恢复、配置回滚、表单快照 | 编辑器命令、菜单动作、任务调度 |
| 风险点   | 快照过多占用存储             | undo 语义不一定可靠            |

简单理解：

```text
备忘录模式：回到之前的状态。
命令模式：撤销之前的动作。
```

文本编辑器中，保存完整文本快照并恢复，适合备忘录模式。把“插入文字”“删除文字”封装为命令，再执行反向操作，适合命令模式。

实际项目中二者可以组合使用：

```text
命令模式：封装编辑动作
备忘录模式：在执行命令前保存快照
```

这样既能记录操作，也能恢复状态。

## 备忘录模式和原型模式的区别

备忘录模式和原型模式都可能涉及对象状态复制，但目的不同。

| 对比项   | 备忘录模式               | 原型模式               |
| -------- | ------------------------ | ---------------------- |
| 核心目的 | 保存历史状态并恢复       | 复制已有对象创建新对象 |
| 时间维度 | 强调历史版本             | 不强调历史版本         |
| 使用对象 | 快照对象通常由管理者保存 | 新对象由调用方使用     |
| 典型场景 | 配置回滚、草稿恢复       | 模板复制、审批流复制   |
| 关注点   | 恢复原对象状态           | 创建相似新对象         |

简单理解：

```text
备忘录模式：保存过去的自己，未来可以恢复。
原型模式：复制一个新的自己，后续独立使用。
```

配置变更前保存旧配置并支持回滚，适合备忘录模式。基于一个审批流程模板复制出新流程，适合原型模式。

## 备忘录模式和快照表

业务系统中常见的历史表、版本表、快照表，本质上经常体现备忘录模式思想。但不是所有历史表都等于备忘录模式。

符合备忘录模式的快照表通常具备：

```text
保存对象完整或关键状态
可以根据快照恢复业务对象
快照由管理者保存和查询
快照不被业务随意修改
```

普通流水表通常只记录操作过程，不一定能恢复状态：

```text
谁在什么时候执行了什么动作
请求参数是什么
结果成功还是失败
错误信息是什么
```

如果历史记录无法还原对象状态，它更像操作日志，而不是备忘录。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

创建文档：

```bash
curl -X POST "http://localhost:8080/memento/document/create?title=设计模式笔记&content=初始内容&operatorId=10001"
```

编辑文档：

```bash
curl -X POST "http://localhost:8080/memento/document/edit?documentId=2019776866538487808&title=设计模式笔记&content=增加备忘录模式内容&operatorId=10001"
```

查询快照：

```bash
curl "http://localhost:8080/memento/document/snapshots?documentId=2019776866538487808"
```

恢复快照：

```bash
curl -X POST "http://localhost:8080/memento/document/restore?documentId=2019776866538487808&snapshotId=SNAP2019776866538487809&operatorId=10001"
```

如果备忘录模式正常，可以看到类似日志：

```text
创建文档成功，文档ID：2019776866538487808，标题：设计模式笔记
创建文档快照成功，文档ID：2019776866538487808，快照ID：SNAP2019776866538487809，版本：1
保存文档快照成功，文档ID：2019776866538487808，快照ID：SNAP2019776866538487809，当前快照数量：1
编辑文档成功，文档ID：2019776866538487808，版本：2
恢复文档成功，文档ID：2019776866538487808，快照ID：SNAP2019776866538487809，恢复版本：1
```

执行不存在的快照恢复：

```bash
curl -X POST "http://localhost:8080/memento/document/restore?documentId=2019776866538487808&snapshotId=UNKNOWN&operatorId=10001"
```

异常日志示例：

```text
获取文档快照失败，快照不存在，文档ID：2019776866538487808，快照ID：UNKNOWN
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

备忘录模式适合保存和恢复对象状态，但不要滥用完整快照。对象内容很大、变更很频繁时，完整快照会带来明显存储压力。

适合使用备忘录模式的场景：

```text
文档草稿恢复
配置版本回滚
规则发布前快照
审批表单历史版本
流程设计器版本恢复
页面设计器撤销恢复
重要业务对象变更前留档
```

不太适合使用备忘录模式的场景：

```text
对象状态很大且变化频繁
只需要记录操作日志
只需要审计谁做了什么
对象状态无法安全恢复
恢复动作会破坏业务一致性
```

不要让 `Caretaker` 修改 `Memento`。备忘录对象最好设计为不可变对象。

推荐写法：

```java
public record DocumentSnapshot(...) {
}
```

不推荐写法：

```java
public class DocumentSnapshot {

    private String content;

    public void setContent(String content) {
        this.content = content;
    }
}
```

如果备忘录中包含敏感信息，例如手机号、身份证号、密钥、合同内容，需要考虑加密、脱敏、访问权限和审计日志。

常见处理方式：

```text
敏感字段加密存储
快照访问需要权限校验
恢复操作记录审计日志
快照下载需要水印或审批
快照数据设置保留周期
```

恢复状态不是简单覆盖字段。对于订单、支付、库存、账户余额等核心业务对象，恢复历史状态可能引发严重一致性问题。

不建议直接快照恢复的对象：

```text
支付单状态
库存数量
账户余额
订单履约状态
已对外发送的通知状态
已提交第三方系统的业务状态
```

这些场景通常需要补偿流程，而不是简单恢复旧状态。例如支付成功后不能恢复成未支付，应该走退款、冲正或人工处理流程。

如果快照保存和业务更新必须保持一致，需要放在同一个事务中：

```text
保存编辑前快照
更新当前业务对象
提交事务
```

如果快照保存失败，是否允许继续更新业务对象，要根据业务要求决定。配置管理系统通常不允许；普通草稿系统可以允许但需要提示。

快照数量需要控制。常见策略包括：

```text
只保留最近 N 个快照
只保留最近 N 天快照
发布版本永久保留
自动保存快照定期清理
手动保存快照优先保留
```

Spring Bean 默认是单例，发起人对象如果有请求级状态，不建议直接做成单例 Bean。本文中的 `DocumentEditor` 是普通对象，由存储组件管理；如果做成 Spring Bean，必须避免成员变量保存当前请求状态。

错误示例：

```java
@Component
public class DocumentEditor {

    private Long currentDocumentId;
    private String currentContent;
}
```

推荐使用普通对象或方法局部变量承载状态：

```java
DocumentEditor editor = documentMemoryStore.get(documentId);
DocumentSnapshot snapshot = editor.createSnapshot(operatorId);
```

## 总结

在 JDK21 和 Spring Boot 3 项目中，备忘录模式的实践重点是在不破坏对象封装的前提下保存历史状态，并在需要时恢复到指定历史状态。

普通 Java 备忘录模式适合理解编辑器撤销、状态保存和恢复。Spring Boot 项目中更推荐使用“发起人对象 + 不可变快照 + 快照管理器 + 业务服务协调”的结构。对于文档草稿、配置管理、规则发布、流程设计器、表单编辑器等场景，备忘录模式可以提供清晰的版本保存和回滚能力。

备忘录模式不是操作日志，也不是普通缓存。它最适合处理“对象状态需要被保存，并且未来可能恢复”的场景。实际落地时，需要重点关注快照大小、存储周期、恢复权限、事务一致性、敏感信息保护和业务对象是否允许被历史状态覆盖。
