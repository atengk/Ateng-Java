# 🧩 代理模式（Proxy Pattern）

------

## 🌟 一、模式简介

**代理模式（Proxy Pattern）** 是一种结构型设计模式，它为某个对象提供一个**代理对象**，
 由代理对象来控制对原对象的访问。代理可以在访问目标对象前后增加额外的逻辑，例如：

- 权限控制
- 日志记录
- 缓存处理
- 远程调用
- 性能监控

在 **Spring AOP** 中，底层大量使用了代理模式（例如 `@Transactional`、`@Async`、`@Cacheable` 都是通过代理实现的）。

------

## 🧠 二、场景举例（实战导向）

假设我们有一个「文档访问系统」，用户可以访问一些文件，但并不是所有文件都能随意访问，
 我们希望在访问文件前先进行权限校验，这时就非常适合用代理模式。

------

## 🏗️ 三、项目结构

```
io.github.atengk
 ├── controller/
 │    └── DocumentController.java
 ├── service/
 │    ├── DocumentService.java
 │    ├── impl/
 │    │    └── RealDocumentService.java
 │    └── proxy/
 │         └── DocumentServiceProxy.java
 └── DesignPatternApplication.java
```

------

## 💡 四、代码实现（Spring Boot 实战版）

------

### 1️⃣ 接口：`DocumentService`

```java
package io.github.atengk.service;

/**
 * 文档访问服务接口
 */
public interface DocumentService {

    /**
     * 打开指定文档
     *
     * @param filename 文件名
     */
    void openDocument(String filename);
}
```

------

### 2️⃣ 真实服务类：`RealDocumentService`

```java
package io.github.atengk.service.impl;

import io.github.atengk.service.DocumentService;
import org.springframework.stereotype.Service;

/**
 * 真实的文档服务类，执行实际的文件访问逻辑
 */
@Service("realDocumentService")
public class RealDocumentService implements DocumentService {

    @Override
    public void openDocument(String filename) {
        System.out.println("【文件访问】正在打开文档：" + filename);
    }
}
```

------

### 3️⃣ 代理类：`DocumentServiceProxy`

```java
package io.github.atengk.service.proxy;

import io.github.atengk.service.DocumentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 文档访问代理类，控制访问权限
 */
@Service("documentServiceProxy")
public class DocumentServiceProxy implements DocumentService {

    private final DocumentService realDocumentService;

    public DocumentServiceProxy(@Qualifier("realDocumentService") DocumentService realDocumentService) {
        this.realDocumentService = realDocumentService;
    }

    @Override
    public void openDocument(String filename) {
        if (checkAccess(filename)) {
            System.out.println("【权限验证】访问通过");
            realDocumentService.openDocument(filename);
        } else {
            System.out.println("【权限验证】访问被拒绝，您无权查看：" + filename);
        }
    }

    /**
     * 模拟权限检查逻辑
     */
    private boolean checkAccess(String filename) {
        // 模拟：如果文件名包含 "secret" 则拒绝访问
        return !filename.contains("secret");
    }
}
```

------

### 4️⃣ 控制层：`DocumentController`

```java
package io.github.atengk.controller;

import io.github.atengk.service.DocumentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文档控制层，用于测试代理模式
 */
@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(@Qualifier("documentServiceProxy") DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/open")
    public String openDocument(String filename) {
        documentService.openDocument(filename);
        return "文档访问结束";
    }
}
```

------

## 🧩 五、运行效果

请求：

```
http://localhost:8080/open?filename=readme.txt
```

输出：

```
【权限验证】访问通过
【文件访问】正在打开文档：readme.txt
```

请求：

```
http://localhost:8080/open?filename=secret-plan.pdf
```

输出：

```
【权限验证】访问被拒绝，您无权查看：secret-plan.pdf
```

------

## 📘 六、总结与要点

| 特性                | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| **模式类型**        | 结构型（Structural Pattern）                                 |
| **核心角色**        | 真实对象（Real Subject） + 代理对象（Proxy） + 接口（Subject） |
| **Spring 实战应用** | AOP 动态代理、事务控制、权限拦截、缓存封装等                 |
| **适用场景**        | 想在目标对象执行前后插入额外逻辑时使用                       |

------

## 🪄 七、扩展：静态代理 vs 动态代理

| 类型     | 实现方式                         | 优点                       | 缺点                 |
| -------- | -------------------------------- | -------------------------- | -------------------- |
| 静态代理 | 代码手动实现（如上例）           | 结构清晰、控制力强         | 代码量多、维护成本高 |
| 动态代理 | JDK Proxy / CGLIB（Spring 默认） | 通用性强、无需重复写代理类 | 可读性较低、调试较难 |

