# 工厂方法模式 + 策略模式

工厂方法模式和策略模式也经常组合使用。和“简单工厂模式 + 策略模式”相比，这个组合更适合 **策略对象的创建过程本身也存在差异** 的场景。

简单工厂通常是一个工厂类集中根据类型返回策略对象；工厂方法模式则是把策略对象的创建过程交给不同的具体工厂，每个工厂负责创建一种策略对象。这样不仅可以扩展策略逻辑，也可以扩展策略对象的创建过程。

```text
工厂方法模式：负责把不同策略对象的创建过程拆给不同具体工厂
策略模式：负责封装不同业务处理逻辑
```

## 适用场景

本示例以“文件导出任务”为业务场景。系统支持多种导出格式，不同格式不仅导出逻辑不同，创建导出策略时需要的初始化参数也不同。

| 导出类型  | 策略逻辑         | 创建差异   |
| ----- | ------------ | ------ |
| EXCEL | 按 Excel 模板导出 | 需要模板编码 |
| CSV   | 按 CSV 格式导出   | 需要分隔符  |
| PDF   | 按 PDF 文件导出   | 需要水印文本 |

如果只使用策略模式，通常可以把 EXCEL、CSV、PDF 的导出逻辑拆成不同策略类。但当每个策略对象创建时还需要不同初始化参数时，把创建逻辑都放在一个简单工厂中会越来越臃肿。

使用“工厂方法模式 + 策略模式”后，结构变成：

```text
Controller
  -> Service
      -> 工厂注册器：根据导出类型找到具体工厂
          -> 具体工厂：创建对应策略对象
              -> 策略对象：执行具体导出逻辑
```

## 基础配置

这里使用 JDK 21、Spring Boot 3、Maven、Lombok、Hutool 和 Spring Validation。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Validation：用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：提供字符串、集合、日期、ID、对象判断等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少实体类、构造器、日志等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy
├── controller
│   └── ExportTaskController.java
├── dto
│   ├── ExportRequest.java
│   └── ExportResponse.java
├── enums
│   └── ExportTypeEnum.java
├── factory
│   ├── ExportStrategyFactory.java
│   ├── ExportStrategyFactoryRegistry.java
│   └── impl
│       ├── CsvExportStrategyFactory.java
│       ├── ExcelExportStrategyFactory.java
│       └── PdfExportStrategyFactory.java
├── service
│   ├── ExportTaskService.java
│   └── impl
│       └── ExportTaskServiceImpl.java
├── strategy
│   ├── ExportStrategy.java
│   └── impl
│       ├── CsvExportStrategy.java
│       ├── ExcelExportStrategy.java
│       └── PdfExportStrategy.java
└── util
    └── ExportFileNameBuilder.java
```

## 核心代码

这一部分给出核心实现。这里的关键点是：`ExportStrategyFactory` 是工厂方法接口，`ExcelExportStrategyFactory`、`CsvExportStrategyFactory`、`PdfExportStrategyFactory` 是具体工厂，每个具体工厂负责创建自己的策略对象。

### 导出类型枚举

导出类型枚举用于统一维护导出类型编码。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/enums/ExportTypeEnum.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 导出类型枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public enum ExportTypeEnum {

    EXCEL("EXCEL", "Excel导出"),

    CSV("CSV", "CSV导出"),

    PDF("PDF", "PDF导出");

    private final String code;

    private final String description;

    /**
     * 根据编码获取导出类型
     *
     * @param code 导出类型编码
     * @return 导出类型枚举
     */
    public static ExportTypeEnum of(String code) {
        String actualCode = StrUtil.trimToEmpty(code);
        for (ExportTypeEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), actualCode)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的导出类型：" + code);
    }
}
```

### 导出请求对象

导出请求对象用于接收导出任务参数。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/dto/ExportRequest.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 导出请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ExportRequest {

    /**
     * 导出类型：EXCEL、CSV、PDF
     */
    @NotBlank(message = "导出类型不能为空")
    private String exportType;

    /**
     * 业务编码，例如 order、user、product
     */
    @NotBlank(message = "业务编码不能为空")
    private String businessCode;

    /**
     * 操作人ID
     */
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    /**
     * 查询条件
     */
    private Map<String, Object> condition;
}
```

### 导出响应对象

导出响应对象用于返回导出任务结果。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/dto/ExportResponse.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 导出响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class ExportResponse {

    /**
     * 导出任务编号
     */
    private String taskNo;

    /**
     * 导出类型
     */
    private String exportType;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 响应消息
     */
    private String message;
}
```

### 文件名构建工具

文件名构建工具用于统一生成导出文件名，避免每个策略重复拼接文件名。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/util/ExportFileNameBuilder.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 导出文件名构建器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class ExportFileNameBuilder {

    /**
     * 构建导出文件名
     *
     * @param businessCode 业务编码
     * @param exportType   导出类型
     * @param suffix       文件后缀
     * @return 文件名
     */
    public String build(String businessCode, String exportType, String suffix) {
        String timestamp = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        return StrUtil.format("{}_{}_{}.{}", businessCode, exportType, timestamp, suffix);
    }
}
```

### 策略接口

策略接口只关心导出行为，不关心策略对象由谁创建。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/strategy/ExportStrategy.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.strategy;

import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportResponse;

/**
 * 导出策略接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ExportStrategy {

    /**
     * 执行导出
     *
     * @param request 导出请求
     * @return 导出响应
     */
    ExportResponse export(ExportRequest request);
}
```

### Excel 导出策略

Excel 导出策略使用模板编码执行导出逻辑，模板编码由具体工厂创建策略时传入。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/strategy/impl/ExcelExportStrategy.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.strategy.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportResponse;
import io.github.atengk.pattern.combination.factorymethodstrategy.util.ExportFileNameBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Excel 导出策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RequiredArgsConstructor
public class ExcelExportStrategy implements ExportStrategy {

    private final ExportFileNameBuilder fileNameBuilder;

    private final String templateCode;

    /**
     * 执行 Excel 导出
     *
     * @param request 导出请求
     * @return 导出响应
     */
    @Override
    public ExportResponse export(ExportRequest request) {
        String taskNo = IdUtil.fastSimpleUUID();
        String fileName = fileNameBuilder.build(request.getBusinessCode(), request.getExportType(), "xlsx");

        log.info("执行Excel导出，任务编号：{}，业务编码：{}，模板编码：{}，操作人：{}",
                taskNo, request.getBusinessCode(), templateCode, request.getOperatorId());

        return ExportResponse.builder()
                .taskNo(taskNo)
                .exportType(request.getExportType())
                .fileName(fileName)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .status("SUCCESS")
                .message("Excel导出任务创建成功")
                .build();
    }
}
```

注意：上面代码需要导入策略接口。

```java
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
```

### CSV 导出策略

CSV 导出策略使用分隔符执行导出逻辑，分隔符由具体工厂创建策略时传入。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/strategy/impl/CsvExportStrategy.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.strategy.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportResponse;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.util.ExportFileNameBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CSV 导出策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RequiredArgsConstructor
public class CsvExportStrategy implements ExportStrategy {

    private final ExportFileNameBuilder fileNameBuilder;

    private final String delimiter;

    /**
     * 执行 CSV 导出
     *
     * @param request 导出请求
     * @return 导出响应
     */
    @Override
    public ExportResponse export(ExportRequest request) {
        String taskNo = IdUtil.fastSimpleUUID();
        String fileName = fileNameBuilder.build(request.getBusinessCode(), request.getExportType(), "csv");

        log.info("执行CSV导出，任务编号：{}，业务编码：{}，分隔符：{}，操作人：{}",
                taskNo, request.getBusinessCode(), delimiter, request.getOperatorId());

        return ExportResponse.builder()
                .taskNo(taskNo)
                .exportType(request.getExportType())
                .fileName(fileName)
                .contentType("text/csv")
                .status("SUCCESS")
                .message("CSV导出任务创建成功")
                .build();
    }
}
```

### PDF 导出策略

PDF 导出策略使用水印文本执行导出逻辑，水印文本由具体工厂创建策略时传入。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/strategy/impl/PdfExportStrategy.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.strategy.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportResponse;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.util.ExportFileNameBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PDF 导出策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RequiredArgsConstructor
public class PdfExportStrategy implements ExportStrategy {

    private final ExportFileNameBuilder fileNameBuilder;

    private final String watermarkText;

    /**
     * 执行 PDF 导出
     *
     * @param request 导出请求
     * @return 导出响应
     */
    @Override
    public ExportResponse export(ExportRequest request) {
        String taskNo = IdUtil.fastSimpleUUID();
        String fileName = fileNameBuilder.build(request.getBusinessCode(), request.getExportType(), "pdf");

        log.info("执行PDF导出，任务编号：{}，业务编码：{}，水印：{}，操作人：{}",
                taskNo, request.getBusinessCode(), watermarkText, request.getOperatorId());

        return ExportResponse.builder()
                .taskNo(taskNo)
                .exportType(request.getExportType())
                .fileName(fileName)
                .contentType("application/pdf")
                .status("SUCCESS")
                .message("PDF导出任务创建成功")
                .build();
    }
}
```

### 工厂方法接口

工厂方法接口定义创建策略对象的方法。不同具体工厂负责创建不同类型的策略对象。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/factory/ExportStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.factory;

import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.enums.ExportTypeEnum;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;

/**
 * 导出策略工厂方法接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ExportStrategyFactory {

    /**
     * 当前工厂支持的导出类型
     *
     * @return 导出类型
     */
    ExportTypeEnum exportType();

    /**
     * 创建导出策略
     *
     * @param request 导出请求
     * @return 导出策略
     */
    ExportStrategy createStrategy(ExportRequest request);
}
```

### Excel 策略工厂

Excel 策略工厂负责创建 Excel 导出策略，并决定使用哪个模板编码。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/factory/impl/ExcelExportStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.factory.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.enums.ExportTypeEnum;
import io.github.atengk.pattern.combination.factorymethodstrategy.factory.ExportStrategyFactory;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.impl.ExcelExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.util.ExportFileNameBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Excel 导出策略工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelExportStrategyFactory implements ExportStrategyFactory {

    private final ExportFileNameBuilder fileNameBuilder;

    /**
     * 当前工厂支持的导出类型
     *
     * @return 导出类型
     */
    @Override
    public ExportTypeEnum exportType() {
        return ExportTypeEnum.EXCEL;
    }

    /**
     * 创建 Excel 导出策略
     *
     * @param request 导出请求
     * @return 导出策略
     */
    @Override
    public ExportStrategy createStrategy(ExportRequest request) {
        String templateCode = StrUtil.format("{}_excel_template", request.getBusinessCode());

        log.info("创建Excel导出策略，业务编码：{}，模板编码：{}",
                request.getBusinessCode(), templateCode);

        return new ExcelExportStrategy(fileNameBuilder, templateCode);
    }
}
```

### CSV 策略工厂

CSV 策略工厂负责创建 CSV 导出策略，并决定使用什么分隔符。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/factory/impl/CsvExportStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.factory.impl;

import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.enums.ExportTypeEnum;
import io.github.atengk.pattern.combination.factorymethodstrategy.factory.ExportStrategyFactory;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.impl.CsvExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.util.ExportFileNameBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CSV 导出策略工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvExportStrategyFactory implements ExportStrategyFactory {

    private final ExportFileNameBuilder fileNameBuilder;

    /**
     * 当前工厂支持的导出类型
     *
     * @return 导出类型
     */
    @Override
    public ExportTypeEnum exportType() {
        return ExportTypeEnum.CSV;
    }

    /**
     * 创建 CSV 导出策略
     *
     * @param request 导出请求
     * @return 导出策略
     */
    @Override
    public ExportStrategy createStrategy(ExportRequest request) {
        String delimiter = ",";

        log.info("创建CSV导出策略，业务编码：{}，分隔符：{}",
                request.getBusinessCode(), delimiter);

        return new CsvExportStrategy(fileNameBuilder, delimiter);
    }
}
```

### PDF 策略工厂

PDF 策略工厂负责创建 PDF 导出策略，并决定使用什么水印文本。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/factory/impl/PdfExportStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.factory.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.enums.ExportTypeEnum;
import io.github.atengk.pattern.combination.factorymethodstrategy.factory.ExportStrategyFactory;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.impl.PdfExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.util.ExportFileNameBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PDF 导出策略工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfExportStrategyFactory implements ExportStrategyFactory {

    private final ExportFileNameBuilder fileNameBuilder;

    /**
     * 当前工厂支持的导出类型
     *
     * @return 导出类型
     */
    @Override
    public ExportTypeEnum exportType() {
        return ExportTypeEnum.PDF;
    }

    /**
     * 创建 PDF 导出策略
     *
     * @param request 导出请求
     * @return 导出策略
     */
    @Override
    public ExportStrategy createStrategy(ExportRequest request) {
        String watermarkText = StrUtil.format("operator_{}", request.getOperatorId());

        log.info("创建PDF导出策略，业务编码：{}，水印：{}",
                request.getBusinessCode(), watermarkText);

        return new PdfExportStrategy(fileNameBuilder, watermarkText);
    }
}
```

### 工厂注册器

工厂注册器负责收集所有具体工厂，并根据导出类型找到对应工厂。它本身不创建具体策略对象，只负责定位具体工厂。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/factory/ExportStrategyFactoryRegistry.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.combination.factorymethodstrategy.enums.ExportTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 导出策略工厂注册器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ExportStrategyFactoryRegistry {

    private final Map<ExportTypeEnum, ExportStrategyFactory> factoryMap;

    /**
     * 初始化导出策略工厂注册器
     *
     * @param factoryList 导出策略工厂集合
     */
    public ExportStrategyFactoryRegistry(List<ExportStrategyFactory> factoryList) {
        if (CollUtil.isEmpty(factoryList)) {
            throw new IllegalStateException("导出策略工厂不能为空");
        }

        Map<ExportTypeEnum, ExportStrategyFactory> tempFactoryMap = new EnumMap<>(ExportTypeEnum.class);
        for (ExportStrategyFactory factory : factoryList) {
            ExportStrategyFactory oldFactory = tempFactoryMap.put(factory.exportType(), factory);
            if (ObjectUtil.isNotNull(oldFactory)) {
                throw new IllegalStateException("导出策略工厂重复注册：" + factory.exportType().getCode());
            }
        }

        this.factoryMap = Collections.unmodifiableMap(tempFactoryMap);
        log.info("导出策略工厂注册器初始化完成，工厂数量：{}", this.factoryMap.size());
    }

    /**
     * 根据导出类型获取具体工厂
     *
     * @param exportTypeCode 导出类型编码
     * @return 导出策略工厂
     */
    public ExportStrategyFactory getFactory(String exportTypeCode) {
        ExportTypeEnum exportType = ExportTypeEnum.of(exportTypeCode);
        ExportStrategyFactory factory = factoryMap.get(exportType);
        if (ObjectUtil.isNull(factory)) {
            throw new IllegalArgumentException("未找到导出策略工厂：" + exportTypeCode);
        }

        log.info("命中导出策略工厂，导出类型：{}，工厂类：{}",
                exportType.getCode(), factory.getClass().getSimpleName());
        return factory;
    }
}
```

### Service 接口

Service 接口对外提供创建导出任务的能力。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/service/ExportTaskService.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.service;

import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportResponse;

/**
 * 导出任务服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ExportTaskService {

    /**
     * 创建导出任务
     *
     * @param request 导出请求
     * @return 导出响应
     */
    ExportResponse createExportTask(ExportRequest request);
}
```

### Service 实现类

Service 实现类负责编排整体流程：先找到具体工厂，再由具体工厂创建策略对象，最后执行策略逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/service/impl/ExportTaskServiceImpl.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.service.impl;

import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportResponse;
import io.github.atengk.pattern.combination.factorymethodstrategy.factory.ExportStrategyFactory;
import io.github.atengk.pattern.combination.factorymethodstrategy.factory.ExportStrategyFactoryRegistry;
import io.github.atengk.pattern.combination.factorymethodstrategy.service.ExportTaskService;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 导出任务服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTaskServiceImpl implements ExportTaskService {

    private final ExportStrategyFactoryRegistry factoryRegistry;

    /**
     * 创建导出任务
     *
     * @param request 导出请求
     * @return 导出响应
     */
    @Override
    public ExportResponse createExportTask(ExportRequest request) {
        ExportStrategyFactory factory = factoryRegistry.getFactory(request.getExportType());
        ExportStrategy strategy = factory.createStrategy(request);
        ExportResponse response = strategy.export(request);

        log.info("导出任务创建完成，任务编号：{}，导出类型：{}，文件名：{}",
                response.getTaskNo(), response.getExportType(), response.getFileName());

        return response;
    }
}
```

### Controller

Controller 提供创建导出任务接口。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/controller/ExportTaskController.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.controller;

import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportResponse;
import io.github.atengk.pattern.combination.factorystrategy.result.Result;
import io.github.atengk.pattern.combination.factorymethodstrategy.service.ExportTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 导出任务控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/export-tasks")
public class ExportTaskController {

    private final ExportTaskService exportTaskService;

    /**
     * 创建导出任务
     *
     * @param request 导出请求
     * @return 导出响应
     */
    @PostMapping
    public Result<ExportResponse> createExportTask(@Valid @RequestBody ExportRequest request) {
        return Result.success(exportTaskService.createExportTask(request));
    }
}
```

如果没有复用上一节的 `Result`，可以把 `Result` 复制到当前模块，例如：

```java
import io.github.atengk.pattern.combination.factorymethodstrategy.result.Result;
```

## 使用方式

启动 Spring Boot 项目后，可以通过接口创建不同类型的导出任务。

接口信息如下：

```text
请求地址：POST /api/export-tasks
Content-Type：application/json
```

Excel 导出请求示例：

```bash
curl -X POST "http://localhost:8080/api/export-tasks" \
  -H "Content-Type: application/json" \
  -d '{
    "exportType": "EXCEL",
    "businessCode": "order",
    "operatorId": 1001,
    "condition": {
      "startTime": "2026-05-01 00:00:00",
      "endTime": "2026-05-13 23:59:59"
    }
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskNo": "7b4f4d72f7f74e9f94e31f4b5f1b0b68",
    "exportType": "EXCEL",
    "fileName": "order_EXCEL_20260513143000.xlsx",
    "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "status": "SUCCESS",
    "message": "Excel导出任务创建成功"
  }
}
```

CSV 导出请求示例：

```bash
curl -X POST "http://localhost:8080/api/export-tasks" \
  -H "Content-Type: application/json" \
  -d '{
    "exportType": "CSV",
    "businessCode": "user",
    "operatorId": 1002,
    "condition": {
      "status": "ENABLE"
    }
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskNo": "e0df86c936694b338580957b20d307cb",
    "exportType": "CSV",
    "fileName": "user_CSV_20260513143100.csv",
    "contentType": "text/csv",
    "status": "SUCCESS",
    "message": "CSV导出任务创建成功"
  }
}
```

PDF 导出请求示例：

```bash
curl -X POST "http://localhost:8080/api/export-tasks" \
  -H "Content-Type: application/json" \
  -d '{
    "exportType": "PDF",
    "businessCode": "product",
    "operatorId": 1003,
    "condition": {
      "categoryId": 10
    }
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskNo": "4d66f6d9b7f84c49bba2c2d6d7b8396f",
    "exportType": "PDF",
    "fileName": "product_PDF_20260513143200.pdf",
    "contentType": "application/pdf",
    "status": "SUCCESS",
    "message": "PDF导出任务创建成功"
  }
}
```

不支持的导出类型请求示例：

```bash
curl -X POST "http://localhost:8080/api/export-tasks" \
  -H "Content-Type: application/json" \
  -d '{
    "exportType": "WORD",
    "businessCode": "order",
    "operatorId": 1004
  }'
```

响应示例：

```json
{
  "code": 400,
  "message": "不支持的导出类型：WORD",
  "data": null
}
```

## 新增导出类型

当新增一种导出类型时，例如新增 `JSON` 导出，不需要修改已有的 Excel、CSV、PDF 工厂和策略，也不需要修改 Service 主流程。

第一步，在 `ExportTypeEnum` 中新增枚举：

```java
JSON("JSON", "JSON导出");
```

第二步，新增 JSON 导出策略。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/strategy/impl/JsonExportStrategy.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.strategy.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportResponse;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.util.ExportFileNameBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 导出策略
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RequiredArgsConstructor
public class JsonExportStrategy implements ExportStrategy {

    private final ExportFileNameBuilder fileNameBuilder;

    private final Boolean prettyFormat;

    /**
     * 执行 JSON 导出
     *
     * @param request 导出请求
     * @return 导出响应
     */
    @Override
    public ExportResponse export(ExportRequest request) {
        String taskNo = IdUtil.fastSimpleUUID();
        String fileName = fileNameBuilder.build(request.getBusinessCode(), request.getExportType(), "json");

        log.info("执行JSON导出，任务编号：{}，业务编码：{}，格式化输出：{}，操作人：{}",
                taskNo, request.getBusinessCode(), prettyFormat, request.getOperatorId());

        return ExportResponse.builder()
                .taskNo(taskNo)
                .exportType(request.getExportType())
                .fileName(fileName)
                .contentType("application/json")
                .status("SUCCESS")
                .message("JSON导出任务创建成功")
                .build();
    }
}
```

第三步，新增 JSON 策略工厂。

文件位置：`src/main/java/io/github/atengk/pattern/combination/factorymethodstrategy/factory/impl/JsonExportStrategyFactory.java`

```java
package io.github.atengk.pattern.combination.factorymethodstrategy.factory.impl;

import io.github.atengk.pattern.combination.factorymethodstrategy.dto.ExportRequest;
import io.github.atengk.pattern.combination.factorymethodstrategy.enums.ExportTypeEnum;
import io.github.atengk.pattern.combination.factorymethodstrategy.factory.ExportStrategyFactory;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.ExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.strategy.impl.JsonExportStrategy;
import io.github.atengk.pattern.combination.factorymethodstrategy.util.ExportFileNameBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON 导出策略工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonExportStrategyFactory implements ExportStrategyFactory {

    private final ExportFileNameBuilder fileNameBuilder;

    /**
     * 当前工厂支持的导出类型
     *
     * @return 导出类型
     */
    @Override
    public ExportTypeEnum exportType() {
        return ExportTypeEnum.JSON;
    }

    /**
     * 创建 JSON 导出策略
     *
     * @param request 导出请求
     * @return 导出策略
     */
    @Override
    public ExportStrategy createStrategy(ExportRequest request) {
        Boolean prettyFormat = true;

        log.info("创建JSON导出策略，业务编码：{}，格式化输出：{}",
                request.getBusinessCode(), prettyFormat);

        return new JsonExportStrategy(fileNameBuilder, prettyFormat);
    }
}
```

新增完成后，`ExportStrategyFactoryRegistry` 会自动收集新的 `JsonExportStrategyFactory`。只要 `exportType()` 不重复，系统就能识别新的导出类型。

## 验证方式

可以从以下几个方面验证组合模式是否生效：

```text
1. 启动项目时，日志应输出导出策略工厂注册器初始化完成
2. 请求 EXCEL 类型时，应命中 ExcelExportStrategyFactory
3. 请求 CSV 类型时，应命中 CsvExportStrategyFactory
4. 请求 PDF 类型时，应命中 PdfExportStrategyFactory
5. 每个具体工厂都应该创建自己的策略对象
6. 新增 JSON 导出时，不需要修改 Service 主流程
7. 请求不存在的导出类型时，应返回 400 错误
```

启动日志示例：

```text
导出策略工厂注册器初始化完成，工厂数量：3
```

Excel 导出日志示例：

```text
命中导出策略工厂，导出类型：EXCEL，工厂类：ExcelExportStrategyFactory
创建Excel导出策略，业务编码：order，模板编码：order_excel_template
执行Excel导出，任务编号：7b4f4d72f7f74e9f94e31f4b5f1b0b68，业务编码：order，模板编码：order_excel_template，操作人：1001
导出任务创建完成，任务编号：7b4f4d72f7f74e9f94e31f4b5f1b0b68，导出类型：EXCEL，文件名：order_EXCEL_20260513143000.xlsx
```

## 组合效果

工厂方法模式和策略模式组合后，各自负责不同变化点：

| 模式     | 职责          | 在示例中的体现                                                                                     |
| ------ | ----------- | ------------------------------------------------------------------------------------------- |
| 工厂方法模式 | 扩展策略对象的创建过程 | `ExcelExportStrategyFactory`、`CsvExportStrategyFactory`、`PdfExportStrategyFactory` 分别创建不同策略 |
| 策略模式   | 扩展具体业务处理逻辑  | `ExcelExportStrategy`、`CsvExportStrategy`、`PdfExportStrategy` 分别执行不同导出逻辑                    |

这种组合适合处理两类变化：

```text
第一类变化：业务处理逻辑变化
例如 EXCEL、CSV、PDF 的导出逻辑不同

第二类变化：对象创建过程变化
例如 EXCEL 需要模板编码，CSV 需要分隔符，PDF 需要水印文本
```

相比“简单工厂模式 + 策略模式”，这个组合的扩展粒度更细。简单工厂通常适合对象创建逻辑比较简单的场景；工厂方法模式更适合不同策略对象创建过程本身也比较复杂、且未来还会继续扩展的场景。

## 注意事项

工厂方法模式和策略模式组合使用时，不要把业务执行逻辑写进具体工厂。具体工厂只负责创建策略对象，业务执行逻辑仍然应该放在策略类中。

在 Spring Boot 项目中，可以用 Spring 自动收集所有具体工厂 Bean，然后通过注册器维护导出类型和工厂之间的映射关系。注册器只负责定位工厂，不直接创建策略对象，这样可以避免退化成一个大而全的简单工厂。

如果策略对象本身已经是无状态 Spring Bean，并且创建过程没有差异，那么使用“简单工厂模式 + 策略模式”通常更直接。如果每种策略创建时都需要不同依赖、不同配置、不同初始化参数或不同构建流程，再考虑使用“工厂方法模式 + 策略模式”。
