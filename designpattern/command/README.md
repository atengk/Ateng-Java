## 🧩 命令模式（Command Pattern）

### 一、模式简介

**命令模式（Command Pattern）** 是一种将“请求”封装为对象的设计模式，从而使你能够用不同的请求对客户进行参数化，对请求排队或记录日志，并支持可撤销操作。
它将“发送请求的对象（Invoker）”与“执行请求的对象（Receiver）”解耦。

在实际项目中常用于：

* 任务调度（异步任务封装）
* 审批流程（每个审批动作为命令）
* 撤销 / 回滚操作（Undo/Redo）
* 动态菜单、按钮绑定命令执行

---

### 二、模式结构说明

```
Command（命令接口）
  ├── execute()：执行命令
  ├── undo()：撤销命令（可选）

ConcreteCommand（具体命令实现）
  ├── 持有 Receiver 的引用
  ├── 调用 Receiver 完成实际逻辑

Receiver（接收者）
  ├── 真正执行业务逻辑的类

Invoker（调用者）
  ├── 负责调用命令对象的 execute() 方法
```

---

### 三、实战案例：文件操作命令系统

**场景说明**：
我们模拟一个“文件操作服务”，不同的操作（创建文件、删除文件）被封装为命令对象，由命令调用器（`FileCommandInvoker`）统一管理和执行。

---

### 四、代码结构

```
io.github.atengk
└── service
    └── command
        ├── command
        │    ├── Command.java
        │    ├── CreateFileCommand.java
        │    └── DeleteFileCommand.java
        ├── receiver
        │    └── FileSystemReceiver.java
        ├── invoker
        │    └── FileCommandInvoker.java
        └── CommandPatternDemo.java
```

---

### 五、代码实现

#### 1️⃣ 命令接口

```java
package io.github.atengk.service.command.command;

/**
 * 命令接口，定义执行与撤销操作
 */
public interface Command {

    /**
     * 执行命令
     */
    void execute();

    /**
     * 撤销命令（可选实现）
     */
    default void undo() {
        // 默认空实现，可由子类重写
    }
}
```

---

#### 2️⃣ 接收者（Receiver）

```java
package io.github.atengk.service.command.receiver;

import org.springframework.stereotype.Service;

/**
 * 文件系统接收者，负责执行实际的文件操作逻辑
 */
@Service
public class FileSystemReceiver {

    public void createFile(String fileName) {
        System.out.println("正在创建文件：" + fileName);
    }

    public void deleteFile(String fileName) {
        System.out.println("正在删除文件：" + fileName);
    }
}
```

---

#### 3️⃣ 具体命令实现

```java
package io.github.atengk.service.command.command;

import io.github.atengk.service.command.receiver.FileSystemReceiver;

/**
 * 创建文件命令
 */
public class CreateFileCommand implements Command {

    private final FileSystemReceiver receiver;
    private final String fileName;

    public CreateFileCommand(FileSystemReceiver receiver, String fileName) {
        this.receiver = receiver;
        this.fileName = fileName;
    }

    @Override
    public void execute() {
        receiver.createFile(fileName);
    }

    @Override
    public void undo() {
        receiver.deleteFile(fileName);
    }
}
```

```java
package io.github.atengk.service.command.command;

import io.github.atengk.service.command.receiver.FileSystemReceiver;

/**
 * 删除文件命令
 */
public class DeleteFileCommand implements Command {

    private final FileSystemReceiver receiver;
    private final String fileName;

    public DeleteFileCommand(FileSystemReceiver receiver, String fileName) {
        this.receiver = receiver;
        this.fileName = fileName;
    }

    @Override
    public void execute() {
        receiver.deleteFile(fileName);
    }

    @Override
    public void undo() {
        receiver.createFile(fileName);
    }
}
```

---

#### 4️⃣ 命令调用者（Invoker）

```java
package io.github.atengk.service.command.invoker;

import io.github.atengk.service.command.command.Command;
import org.springframework.stereotype.Component;

import java.util.Stack;

/**
 * 命令调用者，负责执行命令与管理撤销操作
 */
@Component
public class FileCommandInvoker {

    private final Stack<Command> commandHistory = new Stack<>();

    /**
     * 执行命令
     */
    public void executeCommand(Command command) {
        command.execute();
        commandHistory.push(command);
    }

    /**
     * 撤销上一步命令
     */
    public void undoLastCommand() {
        if (!commandHistory.isEmpty()) {
            Command last = commandHistory.pop();
            last.undo();
        } else {
            System.out.println("没有可撤销的命令。");
        }
    }
}
```

---

#### 5️⃣ 示例运行类

```java
package io.github.atengk.service.command;

import io.github.atengk.service.command.command.Command;
import io.github.atengk.service.command.command.CreateFileCommand;
import io.github.atengk.service.command.command.DeleteFileCommand;
import io.github.atengk.service.command.invoker.FileCommandInvoker;
import io.github.atengk.service.command.receiver.FileSystemReceiver;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 命令模式示例演示
 */
@Component
public class CommandPatternDemo {

    private final FileCommandInvoker invoker;
    private final FileSystemReceiver receiver;

    public CommandPatternDemo(FileCommandInvoker invoker, FileSystemReceiver receiver) {
        this.invoker = invoker;
        this.receiver = receiver;
    }

    @PostConstruct
    public void runDemo() {
        Command create = new CreateFileCommand(receiver, "demo.txt");
        Command delete = new DeleteFileCommand(receiver, "demo.txt");

        System.out.println("=== 执行命令 ===");
        invoker.executeCommand(create);
        invoker.executeCommand(delete);

        System.out.println("=== 撤销上一步 ===");
        invoker.undoLastCommand();
    }
}
```

---

### 六、输出示例

```
=== 执行命令 ===
正在创建文件：demo.txt
正在删除文件：demo.txt
=== 撤销上一步 ===
正在创建文件：demo.txt
```

---

### 七、总结与应用场景

✅ **优点：**

* 命令与调用者解耦，易扩展、易测试。
* 可记录命令历史，实现撤销、重做等高级功能。
* 可将命令放入队列中异步执行。

⚠️ **缺点：**

* 类数量较多，命令过多时管理成本上升。

📌 **常见应用场景：**

* 操作日志（Undo/Redo）
* 工作流系统（每一步为命令）
* 任务调度系统
* 消息队列封装（命令即消息）

