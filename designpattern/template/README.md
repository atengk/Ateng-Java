# 🧩 模板方法模式（Template Method Pattern）

## 🧭 一、模式简介

**模板方法模式（Template Method Pattern）** 是一种行为型设计模式，
 它在一个抽象类中定义了算法的**整体执行流程（模板）**，
 并允许子类通过**重写部分步骤**来自定义具体行为。

> ✅ 核心思想：**固定流程骨架，延迟实现细节。**

------

## 🧠 二、现实类比

就像泡茶流程是固定的：

1. 烧水
2. 放茶叶
3. 冲泡
4. 倒入茶杯

但每种茶（绿茶、红茶、乌龙茶）在第二步和第三步细节上不同。
 模板方法模式就像定义了泡茶模板，具体泡法由子类决定。

------

## 💼 三、项目场景：文件导入模板

假设我们有多种文件导入逻辑（Excel、CSV、JSON），
 它们都需要执行以下步骤：

1. 校验文件；
2. 解析内容；
3. 保存数据库；
4. 返回结果。

不同文件类型对应不同的实现细节。
 我们用模板方法模式统一流程。

------

## 🧱 四、项目结构设计

```
io.github.atengk.designpattern.templatemethod
├── template
│   ├── AbstractFileImportTemplate.java   # 抽象模板类
│   ├── ExcelFileImport.java              # Excel 导入实现
│   ├── CsvFileImport.java                # CSV 导入实现
│   └── JsonFileImport.java               # JSON 导入实现
└── controller
    └── FileImportController.java         # 控制器演示
```

------

## ⚙️ 五、核心代码实现

### 1️⃣ 抽象模板类

```java
package io.github.atengk.designpattern.templatemethod.template;

/**
 * 文件导入模板抽象类
 * 定义文件导入的通用流程（模板方法）
 */
public abstract class AbstractFileImportTemplate {

    /**
     * 模板方法：固定导入流程
     * final 关键字防止子类重写
     */
    public final void importFile(String filePath) {
        validateFile(filePath);
        Object data = parseFile(filePath);
        saveToDatabase(data);
        afterImport(filePath);
    }

    /**
     * 校验文件格式（通用步骤或可选重写）
     */
    protected void validateFile(String filePath) {
        System.out.println("校验文件格式：" + filePath);
    }

    /**
     * 解析文件内容（抽象方法，必须由子类实现）
     */
    protected abstract Object parseFile(String filePath);

    /**
     * 保存解析结果到数据库（抽象方法）
     */
    protected abstract void saveToDatabase(Object data);

    /**
     * 导入完成后的操作（钩子方法，可选）
     */
    protected void afterImport(String filePath) {
        System.out.println("文件导入完成：" + filePath);
    }
}
```

------

### 2️⃣ 子类实现：Excel 导入

```java
package io.github.atengk.designpattern.templatemethod.template;

import org.springframework.stereotype.Service;

/**
 * Excel 文件导入实现
 */
@Service("excelFileImport")
public class ExcelFileImport extends AbstractFileImportTemplate {

    @Override
    protected Object parseFile(String filePath) {
        System.out.println("解析 Excel 文件：" + filePath);
        return "Excel 数据内容";
    }

    @Override
    protected void saveToDatabase(Object data) {
        System.out.println("保存 Excel 数据：" + data);
    }
}
```

------

### 3️⃣ 子类实现：CSV 导入

```java
package io.github.atengk.designpattern.templatemethod.template;

import org.springframework.stereotype.Service;

/**
 * CSV 文件导入实现
 */
@Service("csvFileImport")
public class CsvFileImport extends AbstractFileImportTemplate {

    @Override
    protected Object parseFile(String filePath) {
        System.out.println("解析 CSV 文件：" + filePath);
        return "CSV 数据内容";
    }

    @Override
    protected void saveToDatabase(Object data) {
        System.out.println("保存 CSV 数据：" + data);
    }
}
```

------

### 4️⃣ 子类实现：JSON 导入

```java
package io.github.atengk.designpattern.templatemethod.template;

import org.springframework.stereotype.Service;

/**
 * JSON 文件导入实现
 */
@Service("jsonFileImport")
public class JsonFileImport extends AbstractFileImportTemplate {

    @Override
    protected Object parseFile(String filePath) {
        System.out.println("解析 JSON 文件：" + filePath);
        return "JSON 数据内容";
    }

    @Override
    protected void saveToDatabase(Object data) {
        System.out.println("保存 JSON 数据：" + data);
    }
}
```

------

### 5️⃣ 控制层调用示例

```java
package io.github.atengk.designpattern.templatemethod.controller;

import io.github.atengk.designpattern.templatemethod.template.AbstractFileImportTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模板方法模式示例控制器
 */
@RestController
@RequestMapping("/api/import")
public class FileImportController {

    private final Map<String, AbstractFileImportTemplate> importTemplates;

    public FileImportController(Map<String, AbstractFileImportTemplate> importTemplates) {
        this.importTemplates = importTemplates;
    }

    @GetMapping("/{type}")
    public String importFile(@PathVariable("type") String type, @RequestParam String filePath) {
        AbstractFileImportTemplate template = importTemplates.get(type + "FileImport");
        if (template == null) {
            return "不支持的文件类型：" + type;
        }
        template.importFile(filePath);
        return "导入成功：" + filePath;
    }
}
```

------

## 🚀 六、运行效果

请求：

```
GET http://localhost:8080/api/import/excel?filePath=/data/test.xlsx
```

输出：

```
校验文件格式：/data/test.xlsx
解析 Excel 文件：/data/test.xlsx
保存 Excel 数据：Excel 数据内容
文件导入完成：/data/test.xlsx
```

------

## ✅ 七、总结

| 特性            | 说明                                        |
| --------------- | ------------------------------------------- |
| **设计理念**    | 抽象类定义流程骨架，子类扩展具体步骤        |
| **优点**        | 复用流程逻辑、扩展灵活、统一规范            |
| **常见应用**    | 抽象Service模板、导出导入模板、任务执行模板 |
| **Spring 优化** | 可结合 BeanMap 自动管理模板，动态选择执行   |

------

## 🧩 八、拓展思路

模板方法模式常与以下模式配合：

- ✅ **策略模式**：在模板方法的步骤中动态选择策略；
- ✅ **工厂模式**：根据不同模板类型生成不同模板对象；
- ✅ **钩子方法（Hook）**：在模板中开放扩展点。

