# 设计模式：访问者模式

访问者模式用于在不修改对象结构类的前提下，为一组对象新增操作逻辑。在 JDK21 和 Spring Boot 3 项目中，访问者模式常用于报表导出、规则巡检、对象结构遍历、复杂对象审计、权限树扫描、订单聚合校验、AST 语法树处理、流程节点分析等场景。

需要注意：访问者模式关注的是“对象结构稳定，但操作经常扩展”。如果对象类型经常变化，访问者模式会导致所有访问者都要修改；如果只是根据类型选择一个算法，更适合策略模式；如果只是遍历集合元素，更适合迭代器模式；如果需要对树形结构统一处理，可以和组合模式一起使用。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证访问者模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、金额等通用处理 -->
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

访问者模式的核心目标是把“数据结构”和“作用于数据结构上的操作”拆开。元素类只负责暴露 `accept` 方法，具体操作由访问者实现。

常见角色如下：

| 角色            | 说明                                       |
| --------------- | ------------------------------------------ |
| Visitor         | 访问者接口，为不同元素类型定义访问方法     |
| ConcreteVisitor | 具体访问者，实现具体操作逻辑               |
| Element         | 元素接口，定义 `accept` 方法               |
| ConcreteElement | 具体元素，调用访问者的对应 `visit` 方法    |
| ObjectStructure | 对象结构，保存一组元素并统一接受访问者访问 |
| Client          | 调用方，选择访问者并触发访问               |

典型结构如下：

```text
ReportElement
├── TextElement
├── TableElement
└── ChartElement

ReportVisitor
├── MarkdownExportVisitor
└── PlainTextExportVisitor
```

访问者模式的关键是“双分派”。调用方先调用元素的 `accept(visitor)`，具体元素再回调访问者的 `visit(this)`。这样访问者可以根据真实元素类型执行不同逻辑。

```text
element.accept(visitor)
    -> visitor.visit(textElement)
    -> visitor.visit(tableElement)
    -> visitor.visit(chartElement)
```

在 Spring Boot 项目中，常见优先级通常是：

```text
对象结构稳定 + 操作经常扩展：访问者模式
对象类型经常扩展 + 操作稳定：普通多态或策略模式
```

访问者模式适合对象类型较稳定的结构。例如订单由订单信息、支付信息、物流信息组成，这些结构不经常变化；但对订单的操作可能有摘要生成、风控巡检、审计导出、统计分析等多种访问逻辑。

## 普通 Java 访问者模式

普通 Java 访问者模式适合不依赖 Spring 容器的对象结构访问。下面以报表导出为例，报表中有文本、表格、图表三种元素。元素结构稳定，但导出格式可能不断增加。

示例支持两种访问者：

```text
MarkdownExportVisitor   导出 Markdown
PlainTextExportVisitor  导出纯文本
```

### 文件结构

```text
src/main/java/io/github/atengk/design/visitor/simple/
├── ReportElement.java
├── ReportVisitor.java
├── TextElement.java
├── TableElement.java
├── ChartElement.java
├── MarkdownExportVisitor.java
├── PlainTextExportVisitor.java
└── ReportDocument.java
```

文件位置：`src/main/java/io/github/atengk/design/visitor/simple/ReportElement.java`

下面是报表元素接口，所有具体报表元素都实现 `accept` 方法。

```java
package io.github.atengk.design.visitor.simple;

/**
 * 报表元素
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface ReportElement {

    /**
     * 接受访问者访问
     *
     * @param visitor 报表访问者
     */
    void accept(ReportVisitor visitor);
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/simple/ReportVisitor.java`

下面是报表访问者接口，为每一种报表元素定义访问方法。

```java
package io.github.atengk.design.visitor.simple;

/**
 * 报表访问者
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface ReportVisitor {

    /**
     * 访问文本元素
     *
     * @param element 文本元素
     */
    void visit(TextElement element);

    /**
     * 访问表格元素
     *
     * @param element 表格元素
     */
    void visit(TableElement element);

    /**
     * 访问图表元素
     *
     * @param element 图表元素
     */
    void visit(ChartElement element);
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/simple/TextElement.java`

下面是文本元素，保存标题和正文内容。

```java
package io.github.atengk.design.visitor.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 文本报表元素
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class TextElement implements ReportElement {

    private final String title;
    private final String content;

    /**
     * 创建文本报表元素
     *
     * @param title   标题
     * @param content 内容
     */
    public TextElement(String title, String content) {
        if (StrUtil.hasBlank(title, content)) {
            log.warn("创建文本元素失败，标题或内容为空");
            throw new IllegalArgumentException("标题和内容不能为空");
        }

        this.title = title;
        this.content = content;
    }

    /**
     * 接受访问者访问
     *
     * @param visitor 报表访问者
     */
    @Override
    public void accept(ReportVisitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("报表访问者不能为空");
        }

        visitor.visit(this);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/simple/TableElement.java`

下面是表格元素，保存表格名称、表头和行数。

```java
package io.github.atengk.design.visitor.simple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 表格报表元素
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class TableElement implements ReportElement {

    private final String tableName;
    private final List<String> headers;
    private final Integer rowCount;

    /**
     * 创建表格报表元素
     *
     * @param tableName 表格名称
     * @param headers   表头列表
     * @param rowCount  行数
     */
    public TableElement(String tableName, List<String> headers, Integer rowCount) {
        if (StrUtil.isBlank(tableName)) {
            log.warn("创建表格元素失败，表格名称为空");
            throw new IllegalArgumentException("表格名称不能为空");
        }

        if (CollUtil.isEmpty(headers)) {
            log.warn("创建表格元素失败，表头为空，表格名称：{}", tableName);
            throw new IllegalArgumentException("表头不能为空");
        }

        if (rowCount == null || rowCount < 0) {
            log.warn("创建表格元素失败，行数不合法，表格名称：{}，行数：{}", tableName, rowCount);
            throw new IllegalArgumentException("行数不能小于0");
        }

        this.tableName = tableName;
        this.headers = List.copyOf(headers);
        this.rowCount = rowCount;
    }

    /**
     * 接受访问者访问
     *
     * @param visitor 报表访问者
     */
    @Override
    public void accept(ReportVisitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("报表访问者不能为空");
        }

        visitor.visit(this);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/simple/ChartElement.java`

下面是图表元素，保存图表名称、图表类型和数据点数量。

```java
package io.github.atengk.design.visitor.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 图表报表元素
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class ChartElement implements ReportElement {

    private final String chartName;
    private final String chartType;
    private final Integer dataPointCount;

    /**
     * 创建图表报表元素
     *
     * @param chartName      图表名称
     * @param chartType      图表类型
     * @param dataPointCount 数据点数量
     */
    public ChartElement(String chartName, String chartType, Integer dataPointCount) {
        if (StrUtil.hasBlank(chartName, chartType)) {
            log.warn("创建图表元素失败，图表名称或类型为空");
            throw new IllegalArgumentException("图表名称和类型不能为空");
        }

        if (dataPointCount == null || dataPointCount < 0) {
            log.warn("创建图表元素失败，数据点数量不合法，图表名称：{}，数据点数量：{}", chartName, dataPointCount);
            throw new IllegalArgumentException("数据点数量不能小于0");
        }

        this.chartName = chartName;
        this.chartType = chartType;
        this.dataPointCount = dataPointCount;
    }

    /**
     * 接受访问者访问
     *
     * @param visitor 报表访问者
     */
    @Override
    public void accept(ReportVisitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("报表访问者不能为空");
        }

        visitor.visit(this);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/simple/MarkdownExportVisitor.java`

下面是 Markdown 导出访问者，用于把不同报表元素导出为 Markdown 文本。

```java
package io.github.atengk.design.visitor.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Markdown报表导出访问者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class MarkdownExportVisitor implements ReportVisitor {

    private final StringBuilder builder = new StringBuilder();

    /**
     * 访问文本元素
     *
     * @param element 文本元素
     */
    @Override
    public void visit(TextElement element) {
        builder.append("## ")
                .append(element.getTitle())
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(element.getContent())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        log.info("导出Markdown文本元素，标题：{}", element.getTitle());
    }

    /**
     * 访问表格元素
     *
     * @param element 表格元素
     */
    @Override
    public void visit(TableElement element) {
        builder.append("## 表格：")
                .append(element.getTableName())
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("- 表头：")
                .append(StrUtil.join("、", element.getHeaders()))
                .append(System.lineSeparator())
                .append("- 行数：")
                .append(element.getRowCount())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        log.info("导出Markdown表格元素，表格名称：{}", element.getTableName());
    }

    /**
     * 访问图表元素
     *
     * @param element 图表元素
     */
    @Override
    public void visit(ChartElement element) {
        builder.append("## 图表：")
                .append(element.getChartName())
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("- 类型：")
                .append(element.getChartType())
                .append(System.lineSeparator())
                .append("- 数据点数量：")
                .append(element.getDataPointCount())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        log.info("导出Markdown图表元素，图表名称：{}", element.getChartName());
    }

    /**
     * 获取导出结果
     *
     * @return Markdown文本
     */
    public String exportText() {
        return builder.toString();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/simple/PlainTextExportVisitor.java`

下面是纯文本导出访问者，用于把不同报表元素导出为普通文本。

```java
package io.github.atengk.design.visitor.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 纯文本报表导出访问者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class PlainTextExportVisitor implements ReportVisitor {

    private final StringBuilder builder = new StringBuilder();

    /**
     * 访问文本元素
     *
     * @param element 文本元素
     */
    @Override
    public void visit(TextElement element) {
        builder.append("文本：")
                .append(element.getTitle())
                .append("，内容：")
                .append(element.getContent())
                .append(System.lineSeparator());

        log.info("导出纯文本元素，标题：{}", element.getTitle());
    }

    /**
     * 访问表格元素
     *
     * @param element 表格元素
     */
    @Override
    public void visit(TableElement element) {
        builder.append("表格：")
                .append(element.getTableName())
                .append("，表头：")
                .append(StrUtil.join("、", element.getHeaders()))
                .append("，行数：")
                .append(element.getRowCount())
                .append(System.lineSeparator());

        log.info("导出纯文本表格元素，表格名称：{}", element.getTableName());
    }

    /**
     * 访问图表元素
     *
     * @param element 图表元素
     */
    @Override
    public void visit(ChartElement element) {
        builder.append("图表：")
                .append(element.getChartName())
                .append("，类型：")
                .append(element.getChartType())
                .append("，数据点数量：")
                .append(element.getDataPointCount())
                .append(System.lineSeparator());

        log.info("导出纯文本图表元素，图表名称：{}", element.getChartName());
    }

    /**
     * 获取导出结果
     *
     * @return 纯文本
     */
    public String exportText() {
        return builder.toString();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/simple/ReportDocument.java`

下面是报表文档对象结构，负责保存报表元素并统一接受访问者访问。

```java
package io.github.atengk.design.visitor.simple;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 报表文档对象结构
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ReportDocument {

    private final List<ReportElement> elements = new ArrayList<>();

    /**
     * 添加报表元素
     *
     * @param element 报表元素
     */
    public void addElement(ReportElement element) {
        if (element == null) {
            log.warn("添加报表元素失败，元素为空");
            throw new IllegalArgumentException("报表元素不能为空");
        }

        elements.add(element);
        log.info("添加报表元素成功，当前元素数量：{}", elements.size());
    }

    /**
     * 接受访问者访问
     *
     * @param visitor 报表访问者
     */
    public void accept(ReportVisitor visitor) {
        if (visitor == null) {
            log.warn("访问报表文档失败，访问者为空");
            throw new IllegalArgumentException("报表访问者不能为空");
        }

        for (ReportElement element : elements) {
            element.accept(visitor);
        }

        log.info("报表文档访问完成，元素数量：{}", elements.size());
    }
}
```

使用方式：

```java
ReportDocument document = new ReportDocument();
document.addElement(new TextElement("订单报表", "本报表统计订单核心指标"));
document.addElement(new TableElement("订单明细", List.of("订单号", "金额", "状态"), 120));
document.addElement(new ChartElement("订单金额趋势", "line", 30));

MarkdownExportVisitor markdownVisitor = new MarkdownExportVisitor();
document.accept(markdownVisitor);
String markdownText = markdownVisitor.exportText();

PlainTextExportVisitor plainTextVisitor = new PlainTextExportVisitor();
document.accept(plainTextVisitor);
String plainText = plainTextVisitor.exportText();
```

如果以后新增导出为 HTML、PDF、Excel 的操作，只需要新增新的访问者。报表元素类不用修改。

## Spring Boot 访问者模式

Spring Boot 项目中，访问者模式适合处理结构稳定的复杂业务对象。下面以订单巡检为例，订单巡检对象由订单信息、支付信息、物流信息三类元素组成。元素结构相对稳定，但访问操作可能有摘要生成、风险检查、审计导出等多种类型。

示例支持两种访问者：

```text
summary  生成订单摘要
risk     执行订单风险检查
```

整体流程如下：

```text
Controller
    -> OrderInspectionService
        -> 构建订单元素列表
        -> OrderInspectionVisitorContext 选择访问者
        -> 元素 accept(visitor, result)
        -> 返回巡检结果
```

本示例中的访问者设计为无状态 Spring Bean，把结果放到 `OrderInspectionResult` 中，避免 Spring 单例 Bean 保存请求级状态。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── VisitorApplication.java
├── controller/
│   └── OrderInspectionController.java
├── context/
│   └── OrderInspectionVisitorContext.java
├── dto/
│   ├── OrderInspectionRequest.java
│   ├── OrderInspectionResponse.java
│   └── OrderInspectionResult.java
├── element/
│   ├── OrderInspectionElement.java
│   ├── OrderInfoElement.java
│   ├── PaymentInfoElement.java
│   └── DeliveryInfoElement.java
├── visitor/
│   ├── OrderInspectionVisitor.java
│   ├── SummaryOrderInspectionVisitor.java
│   └── RiskOrderInspectionVisitor.java
└── service/
    ├── OrderInspectionService.java
    └── impl/
        └── OrderInspectionServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/VisitorApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 访问者模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class VisitorApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(VisitorApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderInspectionRequest.java`

下面是订单巡检请求对象，包含订单、支付和物流三类数据。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单巡检请求
 *
 * @param visitorType      访问者类型
 * @param orderNo          订单号
 * @param userId           用户ID
 * @param orderStatus      订单状态
 * @param orderAmount      订单金额
 * @param paid             是否已支付
 * @param payAmount        支付金额
 * @param shipped          是否已发货
 * @param deliveryCompany  物流公司
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderInspectionRequest(
        String visitorType,
        String orderNo,
        Long userId,
        String orderStatus,
        BigDecimal orderAmount,
        Boolean paid,
        BigDecimal payAmount,
        Boolean shipped,
        String deliveryCompany
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderInspectionResult.java`

下面是订单巡检结果上下文，访问者会把摘要和风险信息写入该对象。

```java
package io.github.atengk.design.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单巡检结果
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
public class OrderInspectionResult {

    private final List<String> summaries = new ArrayList<>();
    private final List<String> risks = new ArrayList<>();

    /**
     * 添加摘要信息
     *
     * @param summary 摘要信息
     */
    public void addSummary(String summary) {
        summaries.add(summary);
    }

    /**
     * 添加风险信息
     *
     * @param risk 风险信息
     */
    public void addRisk(String risk) {
        risks.add(risk);
    }

    /**
     * 判断是否存在风险
     *
     * @return true 表示存在风险，false 表示无风险
     */
    public boolean hasRisk() {
        return !risks.isEmpty();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderInspectionResponse.java`

下面是订单巡检响应对象。

```java
package io.github.atengk.design.dto;

import java.util.List;

/**
 * 订单巡检响应
 *
 * @param visitorType 访问者类型
 * @param orderNo     订单号
 * @param summaries   摘要信息
 * @param risks       风险信息
 * @param passed      是否通过
 * @param message     响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderInspectionResponse(
        String visitorType,
        String orderNo,
        List<String> summaries,
        List<String> risks,
        Boolean passed,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/element/OrderInspectionElement.java`

下面是订单巡检元素接口，所有具体元素都通过该接口接受访问者访问。

```java
package io.github.atengk.design.element;

import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.visitor.OrderInspectionVisitor;

/**
 * 订单巡检元素
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderInspectionElement {

    /**
     * 接受订单巡检访问者访问
     *
     * @param visitor 订单巡检访问者
     * @param result  订单巡检结果
     */
    void accept(OrderInspectionVisitor visitor, OrderInspectionResult result);
}
```

文件位置：`src/main/java/io/github/atengk/design/element/OrderInfoElement.java`

下面是订单信息元素，保存订单号、用户、状态和订单金额。

```java
package io.github.atengk.design.element;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.visitor.OrderInspectionVisitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 订单信息元素
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class OrderInfoElement implements OrderInspectionElement {

    private final String orderNo;
    private final Long userId;
    private final String orderStatus;
    private final BigDecimal orderAmount;

    /**
     * 创建订单信息元素
     *
     * @param orderNo     订单号
     * @param userId      用户ID
     * @param orderStatus 订单状态
     * @param orderAmount 订单金额
     */
    public OrderInfoElement(String orderNo, Long userId, String orderStatus, BigDecimal orderAmount) {
        if (StrUtil.hasBlank(orderNo, orderStatus)) {
            log.warn("创建订单信息元素失败，订单号或订单状态为空");
            throw new IllegalArgumentException("订单号和订单状态不能为空");
        }

        if (userId == null || userId <= 0) {
            log.warn("创建订单信息元素失败，用户ID不合法，用户ID：{}", userId);
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("创建订单信息元素失败，订单金额不合法，订单号：{}，金额：{}", orderNo, orderAmount);
            throw new IllegalArgumentException("订单金额不能小于0");
        }

        this.orderNo = orderNo;
        this.userId = userId;
        this.orderStatus = orderStatus;
        this.orderAmount = orderAmount;
    }

    /**
     * 接受订单巡检访问者访问
     *
     * @param visitor 订单巡检访问者
     * @param result  订单巡检结果
     */
    @Override
    public void accept(OrderInspectionVisitor visitor, OrderInspectionResult result) {
        if (visitor == null || result == null) {
            throw new IllegalArgumentException("访问者和结果对象不能为空");
        }

        visitor.visit(this, result);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/element/PaymentInfoElement.java`

下面是支付信息元素，保存支付状态和支付金额。

```java
package io.github.atengk.design.element;

import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.visitor.OrderInspectionVisitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 支付信息元素
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Getter
public class PaymentInfoElement implements OrderInspectionElement {

    private final Boolean paid;
    private final BigDecimal payAmount;

    /**
     * 创建支付信息元素
     *
     * @param paid      是否已支付
     * @param payAmount 支付金额
     */
    public PaymentInfoElement(Boolean paid, BigDecimal payAmount) {
        if (paid == null) {
            log.warn("创建支付信息元素失败，支付状态为空");
            throw new IllegalArgumentException("支付状态不能为空");
        }

        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("创建支付信息元素失败，支付金额不合法，金额：{}", payAmount);
            throw new IllegalArgumentException("支付金额不能小于0");
        }

        this.paid = paid;
        this.payAmount = payAmount;
    }

    /**
     * 接受订单巡检访问者访问
     *
     * @param visitor 订单巡检访问者
     * @param result  订单巡检结果
     */
    @Override
    public void accept(OrderInspectionVisitor visitor, OrderInspectionResult result) {
        if (visitor == null || result == null) {
            throw new IllegalArgumentException("访问者和结果对象不能为空");
        }

        visitor.visit(this, result);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/element/DeliveryInfoElement.java`

下面是物流信息元素，保存发货状态和物流公司。

```java
package io.github.atengk.design.element;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.visitor.OrderInspectionVisitor;
import lombok.Getter;

/**
 * 物流信息元素
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
public class DeliveryInfoElement implements OrderInspectionElement {

    private final Boolean shipped;
    private final String deliveryCompany;

    /**
     * 创建物流信息元素
     *
     * @param shipped         是否已发货
     * @param deliveryCompany 物流公司
     */
    public DeliveryInfoElement(Boolean shipped, String deliveryCompany) {
        if (shipped == null) {
            throw new IllegalArgumentException("发货状态不能为空");
        }

        if (Boolean.TRUE.equals(shipped) && StrUtil.isBlank(deliveryCompany)) {
            throw new IllegalArgumentException("已发货订单必须填写物流公司");
        }

        this.shipped = shipped;
        this.deliveryCompany = StrUtil.nullToDefault(deliveryCompany, "");
    }

    /**
     * 接受订单巡检访问者访问
     *
     * @param visitor 订单巡检访问者
     * @param result  订单巡检结果
     */
    @Override
    public void accept(OrderInspectionVisitor visitor, OrderInspectionResult result) {
        if (visitor == null || result == null) {
            throw new IllegalArgumentException("访问者和结果对象不能为空");
        }

        visitor.visit(this, result);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/OrderInspectionVisitor.java`

下面是订单巡检访问者接口，为每一种订单元素定义访问方法。

```java
package io.github.atengk.design.visitor;

import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.element.DeliveryInfoElement;
import io.github.atengk.design.element.OrderInfoElement;
import io.github.atengk.design.element.PaymentInfoElement;

/**
 * 订单巡检访问者
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderInspectionVisitor {

    /**
     * 获取访问者类型
     *
     * @return 访问者类型
     */
    String visitorType();

    /**
     * 访问订单信息元素
     *
     * @param element 订单信息元素
     * @param result  巡检结果
     */
    void visit(OrderInfoElement element, OrderInspectionResult result);

    /**
     * 访问支付信息元素
     *
     * @param element 支付信息元素
     * @param result  巡检结果
     */
    void visit(PaymentInfoElement element, OrderInspectionResult result);

    /**
     * 访问物流信息元素
     *
     * @param element 物流信息元素
     * @param result  巡检结果
     */
    void visit(DeliveryInfoElement element, OrderInspectionResult result);
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/SummaryOrderInspectionVisitor.java`

下面是摘要访问者，用于生成订单摘要信息。

```java
package io.github.atengk.design.visitor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.element.DeliveryInfoElement;
import io.github.atengk.design.element.OrderInfoElement;
import io.github.atengk.design.element.PaymentInfoElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单摘要巡检访问者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class SummaryOrderInspectionVisitor implements OrderInspectionVisitor {

    /**
     * 获取访问者类型
     *
     * @return 访问者类型
     */
    @Override
    public String visitorType() {
        return "summary";
    }

    /**
     * 访问订单信息元素
     *
     * @param element 订单信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(OrderInfoElement element, OrderInspectionResult result) {
        String summary = StrUtil.format("订单摘要：订单号={}，用户ID={}，状态={}，金额={}",
                element.getOrderNo(), element.getUserId(), element.getOrderStatus(), element.getOrderAmount());

        result.addSummary(summary);
        log.info("生成订单信息摘要，订单号：{}", element.getOrderNo());
    }

    /**
     * 访问支付信息元素
     *
     * @param element 支付信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(PaymentInfoElement element, OrderInspectionResult result) {
        String summary = StrUtil.format("支付摘要：是否已支付={}，支付金额={}",
                element.getPaid(), element.getPayAmount());

        result.addSummary(summary);
        log.info("生成支付信息摘要，是否已支付：{}", element.getPaid());
    }

    /**
     * 访问物流信息元素
     *
     * @param element 物流信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(DeliveryInfoElement element, OrderInspectionResult result) {
        String summary = StrUtil.format("物流摘要：是否已发货={}，物流公司={}",
                element.getShipped(), StrUtil.blankToDefault(element.getDeliveryCompany(), "无"));

        result.addSummary(summary);
        log.info("生成物流信息摘要，是否已发货：{}", element.getShipped());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/visitor/RiskOrderInspectionVisitor.java`

下面是风险访问者，用于检查订单、支付和物流之间是否存在风险。

```java
package io.github.atengk.design.visitor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.element.DeliveryInfoElement;
import io.github.atengk.design.element.OrderInfoElement;
import io.github.atengk.design.element.PaymentInfoElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单风险巡检访问者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class RiskOrderInspectionVisitor implements OrderInspectionVisitor {

    /**
     * 获取访问者类型
     *
     * @return 访问者类型
     */
    @Override
    public String visitorType() {
        return "risk";
    }

    /**
     * 访问订单信息元素
     *
     * @param element 订单信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(OrderInfoElement element, OrderInspectionResult result) {
        if (element.getOrderAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            result.addRisk(StrUtil.format("订单金额较高，订单号={}，金额={}", element.getOrderNo(), element.getOrderAmount()));
            log.warn("发现订单金额风险，订单号：{}，金额：{}", element.getOrderNo(), element.getOrderAmount());
        }

        if (StrUtil.equalsIgnoreCase(element.getOrderStatus(), "CANCELED")) {
            result.addRisk(StrUtil.format("订单已取消，需要确认后续支付和物流状态，订单号={}", element.getOrderNo()));
            log.warn("发现取消订单风险，订单号：{}", element.getOrderNo());
        }
    }

    /**
     * 访问支付信息元素
     *
     * @param element 支付信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(PaymentInfoElement element, OrderInspectionResult result) {
        if (Boolean.FALSE.equals(element.getPaid()) && element.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {
            result.addRisk(StrUtil.format("支付状态异常，未支付但存在支付金额={}", element.getPayAmount()));
            log.warn("发现支付状态风险，支付金额：{}", element.getPayAmount());
        }

        if (Boolean.TRUE.equals(element.getPaid()) && element.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addRisk("支付状态异常，已支付但支付金额小于等于0");
            log.warn("发现支付金额风险，支付状态已支付但金额异常");
        }
    }

    /**
     * 访问物流信息元素
     *
     * @param element 物流信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(DeliveryInfoElement element, OrderInspectionResult result) {
        if (Boolean.TRUE.equals(element.getShipped()) && StrUtil.isBlank(element.getDeliveryCompany())) {
            result.addRisk("物流状态异常，已发货但物流公司为空");
            log.warn("发现物流信息风险，已发货但物流公司为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/OrderInspectionVisitorContext.java`

下面是访问者上下文，负责根据访问者类型选择具体访问者。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.visitor.OrderInspectionVisitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单巡检访问者上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderInspectionVisitorContext {

    private final Map<String, OrderInspectionVisitor> visitorMap;

    /**
     * 创建订单巡检访问者上下文
     *
     * @param visitors 订单巡检访问者列表
     */
    public OrderInspectionVisitorContext(List<OrderInspectionVisitor> visitors) {
        if (CollUtil.isEmpty(visitors)) {
            log.warn("订单巡检访问者列表为空");
            this.visitorMap = Map.of();
            return;
        }

        this.visitorMap = visitors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        visitor -> StrUtil.trim(visitor.visitorType()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化订单巡检访问者上下文，支持访问者类型：{}", visitorMap.keySet());
    }

    /**
     * 获取访问者
     *
     * @param visitorType 访问者类型
     * @return 订单巡检访问者
     */
    public OrderInspectionVisitor getVisitor(String visitorType) {
        if (StrUtil.isBlank(visitorType)) {
            log.warn("获取订单巡检访问者失败，访问者类型为空");
            throw new IllegalArgumentException("访问者类型不能为空");
        }

        OrderInspectionVisitor visitor = visitorMap.get(StrUtil.trim(visitorType).toLowerCase());
        if (visitor == null) {
            log.warn("获取订单巡检访问者失败，不支持的访问者类型：{}", visitorType);
            throw new IllegalArgumentException("不支持的访问者类型：" + visitorType);
        }

        return visitor;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderInspectionService.java`

下面是订单巡检服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderInspectionRequest;
import io.github.atengk.design.dto.OrderInspectionResponse;

/**
 * 订单巡检服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderInspectionService {

    /**
     * 巡检订单
     *
     * @param request 订单巡检请求
     * @return 订单巡检响应
     */
    OrderInspectionResponse inspect(OrderInspectionRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderInspectionServiceImpl.java`

下面是订单巡检服务实现，负责构建对象结构并触发访问者访问。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.context.OrderInspectionVisitorContext;
import io.github.atengk.design.dto.OrderInspectionRequest;
import io.github.atengk.design.dto.OrderInspectionResponse;
import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.element.DeliveryInfoElement;
import io.github.atengk.design.element.OrderInfoElement;
import io.github.atengk.design.element.OrderInspectionElement;
import io.github.atengk.design.element.PaymentInfoElement;
import io.github.atengk.design.service.OrderInspectionService;
import io.github.atengk.design.visitor.OrderInspectionVisitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单巡检服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInspectionServiceImpl implements OrderInspectionService {

    private final OrderInspectionVisitorContext visitorContext;

    /**
     * 巡检订单
     *
     * @param request 订单巡检请求
     * @return 订单巡检响应
     */
    @Override
    public OrderInspectionResponse inspect(OrderInspectionRequest request) {
        validateRequest(request);

        List<OrderInspectionElement> elements = List.of(
                new OrderInfoElement(request.orderNo(), request.userId(), request.orderStatus(), request.orderAmount()),
                new PaymentInfoElement(request.paid(), request.payAmount()),
                new DeliveryInfoElement(request.shipped(), request.deliveryCompany())
        );

        OrderInspectionVisitor visitor = visitorContext.getVisitor(request.visitorType());
        OrderInspectionResult result = new OrderInspectionResult();

        for (OrderInspectionElement element : elements) {
            element.accept(visitor, result);
        }

        boolean passed = !result.hasRisk();
        log.info("订单巡检完成，访问者类型：{}，订单号：{}，是否通过：{}",
                visitor.visitorType(), request.orderNo(), passed);

        return new OrderInspectionResponse(
                visitor.visitorType(),
                request.orderNo(),
                result.getSummaries(),
                result.getRisks(),
                passed,
                passed ? "巡检通过" : "巡检存在风险"
        );
    }

    /**
     * 校验订单巡检请求
     *
     * @param request 订单巡检请求
     */
    private void validateRequest(OrderInspectionRequest request) {
        if (request == null) {
            log.warn("订单巡检失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.visitorType(), request.orderNo(), request.orderStatus())) {
            log.warn("订单巡检失败，访问者类型、订单号或订单状态为空");
            throw new IllegalArgumentException("访问者类型、订单号和订单状态不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("订单巡检失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.orderAmount() == null || request.orderAmount().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("订单巡检失败，订单金额不合法，订单号：{}，金额：{}", request.orderNo(), request.orderAmount());
            throw new IllegalArgumentException("订单金额不能小于0");
        }

        if (request.paid() == null || request.shipped() == null) {
            log.warn("订单巡检失败，支付状态或发货状态为空，订单号：{}", request.orderNo());
            throw new IllegalArgumentException("支付状态和发货状态不能为空");
        }

        if (request.payAmount() == null || request.payAmount().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("订单巡检失败，支付金额不合法，订单号：{}，金额：{}", request.orderNo(), request.payAmount());
            throw new IllegalArgumentException("支付金额不能小于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderInspectionController.java`

下面是订单巡检接口，用于验证访问者模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.OrderInspectionRequest;
import io.github.atengk.design.dto.OrderInspectionResponse;
import io.github.atengk.design.service.OrderInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单巡检控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/visitor/order-inspection")
public class OrderInspectionController {

    private final OrderInspectionService orderInspectionService;

    /**
     * 巡检订单
     *
     * @param visitorType     访问者类型
     * @param orderNo         订单号
     * @param userId          用户ID
     * @param orderStatus     订单状态
     * @param orderAmount     订单金额
     * @param paid            是否已支付
     * @param payAmount       支付金额
     * @param shipped         是否已发货
     * @param deliveryCompany 物流公司
     * @return 订单巡检响应
     */
    @GetMapping("/inspect")
    public OrderInspectionResponse inspect(@RequestParam String visitorType,
                                           @RequestParam String orderNo,
                                           @RequestParam Long userId,
                                           @RequestParam String orderStatus,
                                           @RequestParam BigDecimal orderAmount,
                                           @RequestParam Boolean paid,
                                           @RequestParam BigDecimal payAmount,
                                           @RequestParam Boolean shipped,
                                           @RequestParam(required = false) String deliveryCompany) {
        OrderInspectionRequest request = new OrderInspectionRequest(
                visitorType,
                orderNo,
                userId,
                orderStatus,
                orderAmount,
                paid,
                payAmount,
                shipped,
                deliveryCompany
        );

        return orderInspectionService.inspect(request);
    }
}
```

接口调用示例：

```bash
curl "http://localhost:8080/visitor/order-inspection/inspect?visitorType=summary&orderNo=ORDER10001&userId=10001&orderStatus=PAID&orderAmount=199.00&paid=true&payAmount=199.00&shipped=false"

curl "http://localhost:8080/visitor/order-inspection/inspect?visitorType=risk&orderNo=ORDER10002&userId=10002&orderStatus=CANCELED&orderAmount=19999.00&paid=false&payAmount=10.00&shipped=false"
```

摘要访问者可能返回：

```json
{
  "visitorType": "summary",
  "orderNo": "ORDER10001",
  "summaries": [
    "订单摘要：订单号=ORDER10001，用户ID=10001，状态=PAID，金额=199.00",
    "支付摘要：是否已支付=true，支付金额=199.00",
    "物流摘要：是否已发货=false，物流公司=无"
  ],
  "risks": [],
  "passed": true,
  "message": "巡检通过"
}
```

风险访问者可能返回：

```json
{
  "visitorType": "risk",
  "orderNo": "ORDER10002",
  "summaries": [],
  "risks": [
    "订单金额较高，订单号=ORDER10002，金额=19999.00",
    "订单已取消，需要确认后续支付和物流状态，订单号=ORDER10002",
    "支付状态异常，未支付但存在支付金额=10.00"
  ],
  "passed": false,
  "message": "巡检存在风险"
}
```

这种方式的优点是订单元素结构不需要随着操作类型增加而变化。新增“审计导出”“统计分析”“合规检查”时，只需要新增访问者 Bean。

## 扩展一个新访问者

访问者模式最适合扩展新的操作。下面以“审计导出访问者”为例，新增访问者类型 `audit`。新增后，订单信息、支付信息、物流信息都可以按审计格式输出。

文件位置：`src/main/java/io/github/atengk/design/visitor/AuditOrderInspectionVisitor.java`

下面是审计导出访问者。它会把不同元素转换成审计摘要。

```java
package io.github.atengk.design.visitor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderInspectionResult;
import io.github.atengk.design.element.DeliveryInfoElement;
import io.github.atengk.design.element.OrderInfoElement;
import io.github.atengk.design.element.PaymentInfoElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单审计导出访问者
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class AuditOrderInspectionVisitor implements OrderInspectionVisitor {

    /**
     * 获取访问者类型
     *
     * @return 访问者类型
     */
    @Override
    public String visitorType() {
        return "audit";
    }

    /**
     * 访问订单信息元素
     *
     * @param element 订单信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(OrderInfoElement element, OrderInspectionResult result) {
        result.addSummary(StrUtil.format("AUDIT_ORDER|orderNo={}|userId={}|status={}|amount={}",
                element.getOrderNo(), element.getUserId(), element.getOrderStatus(), element.getOrderAmount()));

        log.info("导出订单审计信息，订单号：{}", element.getOrderNo());
    }

    /**
     * 访问支付信息元素
     *
     * @param element 支付信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(PaymentInfoElement element, OrderInspectionResult result) {
        result.addSummary(StrUtil.format("AUDIT_PAYMENT|paid={}|payAmount={}",
                element.getPaid(), element.getPayAmount()));

        log.info("导出支付审计信息，是否已支付：{}", element.getPaid());
    }

    /**
     * 访问物流信息元素
     *
     * @param element 物流信息元素
     * @param result  巡检结果
     */
    @Override
    public void visit(DeliveryInfoElement element, OrderInspectionResult result) {
        result.addSummary(StrUtil.format("AUDIT_DELIVERY|shipped={}|deliveryCompany={}",
                element.getShipped(), StrUtil.blankToDefault(element.getDeliveryCompany(), "NONE")));

        log.info("导出物流审计信息，是否已发货：{}", element.getShipped());
    }
}
```

调用示例：

```bash
curl "http://localhost:8080/visitor/order-inspection/inspect?visitorType=audit&orderNo=ORDER10003&userId=10003&orderStatus=SHIPPED&orderAmount=299.00&paid=true&payAmount=299.00&shipped=true&deliveryCompany=顺丰速运"
```

新增 `AuditOrderInspectionVisitor` 后，`OrderInspectionController`、`OrderInspectionServiceImpl`、订单元素类都不需要修改。Spring 会自动把新的访问者加入 `OrderInspectionVisitorContext`。

## 扩展一个新元素类型

访问者模式扩展新操作很方便，但扩展新元素类型比较麻烦。比如订单巡检对象新增“售后信息元素”后，访问者接口必须新增一个访问方法。

示例新增元素：

```java
void visit(AfterSaleInfoElement element, OrderInspectionResult result);
```

然后所有访问者都必须实现该方法：

```text
SummaryOrderInspectionVisitor  需要新增 visit(AfterSaleInfoElement)
RiskOrderInspectionVisitor     需要新增 visit(AfterSaleInfoElement)
AuditOrderInspectionVisitor    需要新增 visit(AfterSaleInfoElement)
```

这就是访问者模式的典型缺点：新增访问者容易，新增元素类型困难。

因此在使用访问者模式前，需要判断对象结构是否稳定。如果元素类型经常变化，比如订单对象隔三差五新增支付分账元素、优惠元素、会员元素、发票元素，访问者模式的维护成本会明显升高。

## 访问者模式和策略模式的区别

访问者模式和策略模式都可以把业务操作从主流程中抽离出来，但二者关注点不同。

| 对比项       | 访问者模式                           | 策略模式                     |
| ------------ | ------------------------------------ | ---------------------------- |
| 核心目的     | 对一组不同元素执行某种操作           | 从多个算法中选择一个执行     |
| 操作对象     | 通常是一组异构对象                   | 通常是同一种输入上下文       |
| 扩展操作     | 很方便，新增访问者即可               | 很方便，新增策略即可         |
| 扩展元素类型 | 较麻烦，需要修改所有访问者           | 通常不涉及元素类型           |
| 典型场景     | AST 遍历、报表元素导出、订单结构巡检 | 优惠计算、物流计费、支付渠道 |

简单理解：

```text
访问者模式：一组不同类型的元素，需要新增一类统一操作。
策略模式：同一个业务输入，需要选择一种算法执行。
```

如果是订单结构中有订单信息、支付信息、物流信息，并且要对它们做摘要、审计、风险检查，适合访问者模式。如果只是根据优惠类型选择满减、折扣、立减算法，适合策略模式。

## 访问者模式和组合模式的关系

访问者模式经常和组合模式一起使用。组合模式负责组织树形结构，访问者模式负责对树中不同节点执行操作。

例如权限树：

```text
目录节点
├── 菜单节点
│   ├── 按钮节点
│   └── API节点
```

组合模式解决“节点如何组织成树”，访问者模式解决“如何遍历并对不同节点执行不同操作”。

常见访问者包括：

```text
权限编码采集访问者
权限树导出访问者
权限风险扫描访问者
权限菜单统计访问者
```

如果树节点类型稳定，但后续需要不断增加遍历操作，组合模式 + 访问者模式会比较合适。

## 访问者模式和迭代器模式的区别

访问者模式和迭代器模式都可能遍历对象结构，但目的不同。

| 对比项           | 访问者模式                          | 迭代器模式                   |
| ---------------- | ----------------------------------- | ---------------------------- |
| 核心目的         | 对不同元素执行不同访问操作          | 顺序访问集合元素             |
| 关注点           | 操作扩展                            | 遍历方式                     |
| 是否关心元素类型 | 关心                                | 通常不关心                   |
| 典型方法         | `accept(visitor)`、`visit(element)` | `hasNext()`、`next()`        |
| 典型场景         | 报表导出、AST 分析、结构审计        | 分页扫描、集合遍历、游标查询 |

简单理解：

```text
迭代器模式：一个一个取元素。
访问者模式：取到元素后，根据元素类型执行访问逻辑。
```

实际项目中二者可以组合。迭代器负责遍历元素集合，访问者负责对每个元素执行操作。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行摘要巡检：

```bash
curl "http://localhost:8080/visitor/order-inspection/inspect?visitorType=summary&orderNo=ORDER10001&userId=10001&orderStatus=PAID&orderAmount=199.00&paid=true&payAmount=199.00&shipped=false"
```

执行风险巡检：

```bash
curl "http://localhost:8080/visitor/order-inspection/inspect?visitorType=risk&orderNo=ORDER10002&userId=10002&orderStatus=CANCELED&orderAmount=19999.00&paid=false&payAmount=10.00&shipped=false"
```

执行审计导出：

```bash
curl "http://localhost:8080/visitor/order-inspection/inspect?visitorType=audit&orderNo=ORDER10003&userId=10003&orderStatus=SHIPPED&orderAmount=299.00&paid=true&payAmount=299.00&shipped=true&deliveryCompany=顺丰速运"
```

如果访问者模式正常，可以看到类似日志：

```text
初始化订单巡检访问者上下文，支持访问者类型：[summary, risk, audit]
生成订单信息摘要，订单号：ORDER10001
生成支付信息摘要，是否已支付：true
生成物流信息摘要，是否已发货：false
订单巡检完成，访问者类型：summary，订单号：ORDER10001，是否通过：true
```

执行不支持的访问者类型：

```bash
curl "http://localhost:8080/visitor/order-inspection/inspect?visitorType=unknown&orderNo=ORDER10001&userId=10001&orderStatus=PAID&orderAmount=199.00&paid=true&payAmount=199.00&shipped=false"
```

异常日志示例：

```text
获取订单巡检访问者失败，不支持的访问者类型：unknown
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

访问者模式适合对象结构稳定、操作容易增加的场景。它不适合元素类型经常变化的业务模型，因为每新增一个元素类型，都需要修改访问者接口和所有具体访问者。

适合使用访问者模式的场景：

```text
报表元素导出
权限树扫描
AST语法树分析
订单聚合结构巡检
流程节点统计
复杂对象审计导出
文件结构分析
对象结构稳定但操作经常增加
```

不太适合使用访问者模式的场景：

```text
元素类型频繁变化
对象结构很简单
只有一种操作
业务人员难以理解双分派
普通多态或策略模式即可解决
```

不要为了少量 `if else` 强行引入访问者模式。如果元素只有一两个类型、操作也不扩展，访问者模式会增加类数量。

不推荐在元素中堆积所有操作：

```java
public class OrderInfoElement {

    public String buildSummary() {
        return null;
    }

    public String exportAudit() {
        return null;
    }

    public String checkRisk() {
        return null;
    }
}
```

这种写法会让元素类越来越胖。推荐把操作拆成访问者：

```java
public class SummaryOrderInspectionVisitor implements OrderInspectionVisitor {
}

public class RiskOrderInspectionVisitor implements OrderInspectionVisitor {
}

public class AuditOrderInspectionVisitor implements OrderInspectionVisitor {
}
```

访问者如果注册为 Spring 单例 Bean，不要在访问者中保存请求级可变状态。

错误示例：

```java
private List<String> currentSummaries;
private List<String> currentRisks;
private String currentOrderNo;
```

推荐使用方法参数传递结果上下文：

```java
public void visit(OrderInfoElement element, OrderInspectionResult result) {
    result.addSummary("订单摘要");
}
```

访问者模式容易让接口变重。元素类型很多时，访问者接口会出现大量 `visit` 方法。

示例：

```text
visit(OrderInfoElement)
visit(PaymentInfoElement)
visit(DeliveryInfoElement)
visit(InvoiceInfoElement)
visit(CouponInfoElement)
visit(MemberInfoElement)
visit(AfterSaleInfoElement)
```

如果元素类型不稳定，建议重新评估是否应该使用访问者模式，或者把元素按更稳定的抽象分类。

访问者模式中的访问逻辑应该尽量保持单一职责。摘要访问者只生成摘要，风险访问者只做风险检查，审计访问者只生成审计内容。不要把多个无关操作塞进一个访问者。

不推荐：

```java
public class AllInOneOrderVisitor implements OrderInspectionVisitor {
    // 生成摘要
    // 检查风险
    // 写数据库
    // 发送消息
    // 导出文件
}
```

推荐：

```text
SummaryOrderInspectionVisitor
RiskOrderInspectionVisitor
AuditOrderInspectionVisitor
```

如果访问操作涉及数据库更新、外部接口调用、消息发送，需要谨慎控制副作用。访问者模式更适合分析、转换、校验、导出这类操作。如果访问过程中修改业务状态，要明确事务边界和失败补偿。

## 总结

在 JDK21 和 Spring Boot 3 项目中，访问者模式的实践重点是把对象结构和作用于对象结构的操作分离，让新增操作时尽量不修改元素类。

普通 Java 访问者模式适合理解双分派、元素接口和访问者接口。Spring Boot 项目中更推荐使用“元素接口 + 多个具体元素 + 访问者接口 + 多个访问者 Bean + 访问者上下文”的结构。对于订单巡检、报表导出、权限树扫描、AST 分析、流程节点统计等对象结构稳定但操作频繁扩展的场景，访问者模式可以让操作扩展更加清晰。

访问者模式不是为了替代所有多态和策略。它最适合处理“元素结构稳定、访问操作多变、需要对不同元素类型执行不同逻辑”的场景。实际落地时，需要重点关注元素类型扩展成本、访问者单例线程安全、操作副作用和职责边界。
