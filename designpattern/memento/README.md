# 🧩 备忘录模式（Memento Pattern）

---

## 🌟 一、模式简介

**备忘录模式（Memento Pattern）** 是一种**行为型设计模式**，用于在不破坏封装性的前提下，保存一个对象的内部状态，以便在以后恢复。

在 **Spring Boot** 项目中，备忘录模式常用于：

* 状态回滚与撤销操作
* 历史记录管理
* 配置或对象状态的快照与恢复

---

## 🧠 二、场景举例（实战导向）

假设我们有一个「文本编辑器系统」，用户可以编辑内容并撤销修改。
通过备忘录模式，编辑器对象保存状态到备忘录中，撤销操作时恢复对应状态，避免直接暴露对象内部字段。

---

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── MementoController.java
 ├── service/
 │    └── memento/
 │          ├── Editor.java
 │          ├── EditorMemento.java
 │          └── Caretaker.java
 └── DesignPatternApplication.java
```

---

## 💡 四、代码实现（Spring Boot 实战版）

---

### 1️⃣ 备忘录类：`EditorMemento`

```java
package io.github.atengk.service.memento;

/**
 * 备忘录类，保存编辑器状态
 */
public class EditorMemento {

    private final String content;

    public EditorMemento(String content) {
        this.content = content;
    }

    /**
     * 获取保存的内容
     */
    public String getContent() {
        return content;
    }
}
```

---

### 2️⃣ 发起人类：`Editor`

```java
package io.github.atengk.service.memento;

import org.springframework.stereotype.Component;

/**
 * 编辑器（发起人）
 */
@Component
public class Editor {

    private String content = "";

    /**
     * 写入内容
     */
    public void write(String text) {
        content += text;
        System.out.println("【编辑器】当前内容：" + content);
    }

    /**
     * 创建备忘录
     */
    public EditorMemento save() {
        return new EditorMemento(content);
    }

    /**
     * 恢复状态
     */
    public void restore(EditorMemento memento) {
        content = memento.getContent();
        System.out.println("【编辑器】已恢复内容：" + content);
    }

    public String getContent() {
        return content;
    }
}
```

---

### 3️⃣ 管理者类：`Caretaker`

```java
package io.github.atengk.service.memento;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 备忘录管理者（Caretaker）
 */
@Component
public class Caretaker {

    private final List<EditorMemento> mementos = new ArrayList<>();

    /**
     * 添加备忘录
     */
    public void addMemento(EditorMemento memento) {
        mementos.add(memento);
    }

    /**
     * 获取指定版本的备忘录
     */
    public EditorMemento getMemento(int index) {
        if (index >= 0 && index < mementos.size()) {
            return mementos.get(index);
        }
        return null;
    }

    /**
     * 获取备忘录数量
     */
    public int size() {
        return mementos.size();
    }
}
```

---

### 4️⃣ 控制层：`MementoController`

```java
package io.github.atengk.controller;

import io.github.atengk.service.memento.Caretaker;
import io.github.atengk.service.memento.Editor;
import io.github.atengk.service.memento.EditorMemento;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 备忘录模式控制器演示
 */
@RestController
public class MementoController {

    private final Editor editor;
    private final Caretaker caretaker;

    public MementoController(Editor editor, Caretaker caretaker) {
        this.editor = editor;
        this.caretaker = caretaker;
    }

    /**
     * 写入内容接口
     */
    @GetMapping("/memento/write")
    public String write(@RequestParam String text) {
        editor.write(text);
        return "写入完成";
    }

    /**
     * 保存当前状态接口
     */
    @GetMapping("/memento/save")
    public String save() {
        EditorMemento memento = editor.save();
        caretaker.addMemento(memento);
        return "状态已保存，当前版本：" + (caretaker.size() - 1);
    }

    /**
     * 恢复指定版本状态接口
     */
    @GetMapping("/memento/restore")
    public String restore(@RequestParam int version) {
        EditorMemento memento = caretaker.getMemento(version);
        if (memento != null) {
            editor.restore(memento);
            return "已恢复到版本：" + version;
        } else {
            return "版本不存在";
        }
    }

    /**
     * 查看当前内容
     */
    @GetMapping("/memento/content")
    public String content() {
        return editor.getContent();
    }
}
```

---

## 🧩 五、运行效果

请求：

```
http://localhost:8080/memento/write?text=Hello
http://localhost:8080/memento/save
http://localhost:8080/memento/write?text= World
http://localhost:8080/memento/save
```

再恢复版本 0：

```
http://localhost:8080/memento/restore?version=0
```

控制台输出：

```
【编辑器】当前内容：Hello
【编辑器】当前内容：Hello World
【编辑器】已恢复内容：Hello
```

---

## 📘 六、总结与要点

| 特性              | 说明                                              |
| --------------- | ----------------------------------------------- |
| **模式类型**        | 行为型（Behavioral Pattern）                         |
| **核心角色**        | 发起人（Editor）+ 备忘录（EditorMemento）+ 管理者（Caretaker） |
| **Spring 实战应用** | 通过组件管理备忘录状态，实现状态快照和回滚                           |
| **适用场景**        | 撤销操作、历史记录、状态快照                                  |

