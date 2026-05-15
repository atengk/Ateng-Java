# Excel 批量导入、校验、错误回执

本案例实现一个可复用的 Excel 导入模块：用户上传 Excel 后，系统创建导入任务，异步解析文件，完成字段校验和业务校验，合法数据批量入库，错误数据生成回执文件，前端可查询导入进度并下载错误回执。原 README 中该场景的核心要求是覆盖上传 Excel、创建任务、异步解析、批量校验、批量入库、错误回执和进度查询。

## 功能目标

本案例不只演示“读取 Excel”，而是按真实后台系统的导入链路实现一套完整闭环。核心目标是让导入过程可追踪、可重试、可定位错误，并支持部分成功、部分失败的业务场景。

最终实现的能力如下：

```text
上传 Excel
-> 保存原始文件
-> 创建导入任务
-> 投递 RabbitMQ 消息
-> 异步解析 Excel
-> 校验每一行数据
-> 正确数据批量入库
-> 错误数据写入错误回执
-> 更新导入任务进度
-> 查询任务状态
-> 下载错误回执文件
```

导入完成后，用户可以看到：

```text
任务状态：待处理 / 处理中 / 成功 / 部分成功 / 失败
总行数：Excel 中实际数据行数
成功行数：成功入库的数据数量
失败行数：校验失败的数据数量
错误回执：包含原始数据和错误原因的 Excel 文件
```

### 实现范围

本案例以“客户资料导入”为业务示例，字段设计保持通用，便于迁移到用户导入、商品导入、库存导入、账单导入等场景。

示例导入字段如下：

| 字段     | 是否必填 | 校验规则               | 说明             |
| -------- | -------- | ---------------------- | ---------------- |
| 客户名称 | 是       | 长度 2-50              | 客户基础名称     |
| 手机号   | 是       | 手机号格式、数据库唯一 | 作为业务唯一标识 |
| 邮箱     | 否       | 邮箱格式               | 可为空           |
| 客户等级 | 是       | NORMAL / VIP           | 示例字典字段     |
| 所属城市 | 否       | 长度 2-50              | 普通文本字段     |
| 备注     | 否       | 长度不超过 200         | 补充说明         |

本案例实现以下接口：

| 接口                                 | 方法 | 作用                      |
| ------------------------------------ | ---- | ------------------------- |
| `/customer/import/template`          | GET  | 下载导入模板              |
| `/customer/import/upload`            | POST | 上传 Excel 并创建导入任务 |
| `/customer/import/progress/{taskId}` | GET  | 查询导入进度              |
| `/customer/import/receipt/{taskId}`  | GET  | 下载错误回执              |

本案例重点实现核心链路，不展开后台菜单、权限控制、前端页面、多租户、复杂数据权限等外围能力。实际项目中可以在 Controller 层接入 Sa-Token，在数据表中增加 `tenant_id`，并在任务查询和文件下载时校验当前用户是否有权限访问该导入任务。

### 核心流程

导入流程采用“上传接口快速返回 + MQ 异步处理”的方式，避免大文件解析占用 HTTP 请求线程。

```text
用户上传 Excel
        |
        v
Controller 接收文件
        |
        v
保存原始 Excel 文件
        |
        v
创建 import_task 任务记录
        |
        v
发送 RabbitMQ 导入消息
        |
        v
接口返回 taskId
        |
        v
消费者异步解析 Excel
        |
        v
EasyExcel 逐行读取数据
        |
        v
字段格式校验 + 业务规则校验
        |
        +----------------------+
        |                      |
        v                      v
正确数据批量入库        错误数据收集错误原因
        |                      |
        +----------+-----------+
                   v
          生成错误回执 Excel
                   |
                   v
          更新导入任务统计信息
```

任务状态建议按以下枚举流转：

```text
PENDING
待处理，上传成功但还未开始消费

PROCESSING
处理中，消费者已经开始解析 Excel

SUCCESS
全部成功，失败行数为 0

PART_SUCCESS
部分成功，成功行数大于 0，失败行数大于 0

FAILED
全部失败，或者文件解析异常、系统异常导致任务失败
```

错误回执的处理原则是：只要存在失败行，就生成错误回执文件。回执文件中保留原始导入字段，并追加一列“错误原因”，便于业务人员修改后重新上传。

### 技术选型

本案例基于 Spring Boot 3 实现，Excel 解析使用 EasyExcel，数据库访问使用 MyBatis-Plus，异步导入使用 RabbitMQ，文件存储可以先用本地目录，后续平滑替换为 MinIO。

| 技术             | 用途             | 说明                                       |
| ---------------- | ---------------- | ------------------------------------------ |
| Spring Boot 3    | 基础框架         | 提供 Web、配置、依赖注入能力               |
| EasyExcel        | Excel 解析与生成 | 适合大文件逐行读取，避免一次性加载整个文件 |
| MyBatis-Plus     | 数据库访问       | 简化 CRUD、批量保存、分页查询              |
| MySQL            | 数据存储         | 保存客户数据、导入任务、导入明细           |
| RabbitMQ         | 异步处理         | 上传接口只创建任务，真正导入由消费者完成   |
| Hutool           | 工具类           | 用于字符串、集合、日期、文件等常用处理     |
| Lombok           | 简化实体代码     | 减少 Getter、Setter、构造器样板代码        |
| MinIO / 本地文件 | 文件存储         | 原始文件、模板文件、错误回执文件存储       |

本案例先采用本地文件存储，便于快速跑通核心功能：

```text
/data/excel-import/original     原始上传文件
/data/excel-import/receipt      错误回执文件
/data/excel-import/template     导入模板文件
```

后续如果接入 MinIO，只需要替换文件存储服务，不影响导入任务、Excel 解析、校验和批量入库逻辑。

## 项目准备

本节先完成项目依赖、基础配置和文件存储准备。该场景原始技术栈建议使用 Spring Boot、EasyExcel、MyBatis-Plus、Redis、RabbitMQ、XXL-JOB、MinIO、Hutool、MySQL；这里先实现核心导入闭环，使用 Spring Boot 3、EasyExcel、MyBatis-Plus、RabbitMQ、Hutool、MySQL、本地文件存储，MinIO 作为后续可替换方案。

### Maven 依赖

项目使用 Spring Boot 3，Excel 解析使用 EasyExcel，数据库访问使用 MyBatis-Plus，异步导入使用 RabbitMQ。这里先给出核心依赖，后续代码都基于这些依赖实现。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Web 接口能力：上传文件、下载模板、查询进度 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于 Controller 入参和业务 DTO 校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- RabbitMQ：用于 Excel 异步导入任务投递和消费 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化单表 CRUD 和批量入库 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- EasyExcel：Excel 读取、模板生成、错误回执生成 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>easyexcel</artifactId>
        <version>4.0.3</version>
    </dependency>

    <!-- Hutool：字符串、集合、文件、日期、ID 等工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Lombok：简化 Entity、DTO、VO 的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- MinIO：如果使用对象存储保存原始文件和错误回执，则打开该依赖 -->
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <version>8.5.12</version>
    </dependency>

    <!-- 测试依赖：用于后续单元测试和接口验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 配置文件

这里使用本地文件存储优先，便于快速跑通导入流程。RabbitMQ 用于异步导入，MySQL 用于保存导入任务、导入明细和客户数据。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: excel-import-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/excel_import_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  servlet:
    multipart:
      # 单个 Excel 文件最大 20MB，可按业务调整
      max-file-size: 20MB
      # 单次请求最大 25MB
      max-request-size: 25MB

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        # 手动确认，避免消费失败后消息误丢
        acknowledge-mode: manual
        # 每次只拉取少量消息，避免大文件任务同时堆积在同一个消费者
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2000ms

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    # 开发阶段打开 SQL 日志，生产环境建议关闭或改成标准日志输出
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

excel-import:
  storage:
    # local 表示本地文件存储；minio 表示对象存储
    type: local
    # 本地文件根目录
    local-root-path: /data/excel-import
    # 原始上传文件目录
    original-dir: original
    # 错误回执文件目录
    receipt-dir: receipt
    # 模板文件目录
    template-dir: template

  mq:
    # 导入任务交换机
    exchange: excel.import.exchange
    # 导入任务队列
    queue: excel.import.queue
    # 导入任务路由键
    routing-key: excel.import.customer

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: excel-import
```

配置属性建议单独映射成 Java 配置类，后续文件保存、MQ 投递、模板下载都会使用它。

文件位置：`src/main/java/io/github/atengk/excel/config/ExcelImportProperties.java`

```java
package io.github.atengk.excel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Excel 导入配置属性
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "excel-import")
public class ExcelImportProperties {

    /**
     * 文件存储配置
     */
    private Storage storage = new Storage();

    /**
     * MQ 配置
     */
    private Mq mq = new Mq();

    @Data
    public static class Storage {

        /**
         * 存储类型：local / minio
         */
        private String type;

        /**
         * 本地文件根目录
         */
        private String localRootPath;

        /**
         * 原始文件目录
         */
        private String originalDir;

        /**
         * 错误回执目录
         */
        private String receiptDir;

        /**
         * 模板文件目录
         */
        private String templateDir;
    }

    @Data
    public static class Mq {

        /**
         * 交换机
         */
        private String exchange;

        /**
         * 队列
         */
        private String queue;

        /**
         * 路由键
         */
        private String routingKey;
    }
}
```

### 文件存储目录或 MinIO 配置

本地文件存储适合开发环境和单体项目快速验证。生产环境如果是多节点部署，建议改成 MinIO、OSS、S3 等对象存储，否则不同节点之间可能无法访问同一份原始 Excel 或错误回执文件。

本地目录结构如下：

```text
/data/excel-import
├── original
│   └── 20260515
│       └── customer_import_1001.xlsx
├── receipt
│   └── 20260515
│       └── customer_import_1001_receipt.xlsx
└── template
    └── customer_import_template.xlsx
```

开发环境可以用下面命令提前创建目录：

```bash
mkdir -p /data/excel-import/original
mkdir -p /data/excel-import/receipt
mkdir -p /data/excel-import/template
chmod -R 755 /data/excel-import
```

如果使用 MinIO，建议按业务模块划分对象路径，而不是把所有文件都放在桶根目录。

```text
bucket: excel-import

customer/original/20260515/customer_import_1001.xlsx
customer/receipt/20260515/customer_import_1001_receipt.xlsx
customer/template/customer_import_template.xlsx
```

MinIO 只影响文件保存和下载，不影响导入任务表、校验逻辑、批量入库和错误回执生成逻辑。后续可以抽象一个 `FileStorageService`，本地存储和 MinIO 存储分别实现该接口。

## 数据库设计

本案例使用三张核心表：导入任务表记录任务状态和统计信息，导入明细表记录每一行的处理结果，业务数据表保存成功导入的客户资料。

### 导入任务表

导入任务表用于前端查询导入进度，也用于排查导入失败原因。每次上传 Excel 都会生成一条任务记录。

表名：`import_task`

```sql
CREATE TABLE import_task (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_no VARCHAR(64) NOT NULL COMMENT '导入任务编号',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型：CUSTOMER_IMPORT',
    original_file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    original_file_path VARCHAR(500) NOT NULL COMMENT '原始文件存储路径',
    receipt_file_path VARCHAR(500) DEFAULT NULL COMMENT '错误回执文件路径',
    status VARCHAR(32) NOT NULL COMMENT '任务状态：PENDING/PROCESSING/SUCCESS/PART_SUCCESS/FAILED',
    total_count INT NOT NULL DEFAULT 0 COMMENT '总行数',
    success_count INT NOT NULL DEFAULT 0 COMMENT '成功行数',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败行数',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '任务级失败原因',
    start_time DATETIME DEFAULT NULL COMMENT '开始处理时间',
    finish_time DATETIME DEFAULT NULL COMMENT '处理完成时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_biz_type_status (biz_type, status),
    KEY idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Excel导入任务表';
```

核心字段说明：

| 字段                | 说明                                           |
| ------------------- | ---------------------------------------------- |
| `task_no`           | 对外展示的导入任务编号，建议使用日期 + 雪花 ID |
| `biz_type`          | 业务类型，用于区分客户导入、商品导入、库存导入 |
| `status`            | 任务状态，前端进度查询主要依赖该字段           |
| `total_count`       | 实际读取到的数据行数，不包含表头               |
| `success_count`     | 成功入库数量                                   |
| `fail_count`        | 校验失败或入库失败数量                         |
| `receipt_file_path` | 有失败数据时生成错误回执路径                   |

### 导入明细表

导入明细表用于记录每一行的处理结果，方便用户追踪具体是哪一行失败。错误回执可以基于解析期间的内存数据生成，也可以基于该表二次生成。

表名：`import_task_detail`

```sql
CREATE TABLE import_task_detail (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '导入任务ID',
    row_index INT NOT NULL COMMENT 'Excel行号，从1开始',
    row_data JSON NOT NULL COMMENT '原始行数据JSON',
    status VARCHAR(32) NOT NULL COMMENT '处理状态：SUCCESS/FAILED',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误原因，多个错误用分号分隔',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    KEY idx_task_id_status (task_id, status),
    KEY idx_task_id_row_index (task_id, row_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Excel导入明细表';
```

设计要点：

| 字段            | 说明                                            |
| --------------- | ----------------------------------------------- |
| `row_index`     | 保存 Excel 原始行号，错误定位更直观             |
| `row_data`      | 保存原始行数据 JSON，方便问题排查和重新生成回执 |
| `status`        | 当前行处理结果                                  |
| `error_message` | 记录字段格式错误、重复数据、业务规则错误等原因  |

如果数据量非常大，明细表可以只记录失败行，成功行不落明细，以减少数据库写入压力。本案例为了链路完整，默认成功和失败都记录。

### 业务数据表

业务数据表以“客户资料”为例。手机号作为业务唯一键，用于演示数据库重复校验和唯一索引兜底。

表名：`customer_info`

```sql
CREATE TABLE customer_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    mobile VARCHAR(20) NOT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    customer_level VARCHAR(32) NOT NULL COMMENT '客户等级：NORMAL/VIP',
    city VARCHAR(50) DEFAULT NULL COMMENT '所属城市',
    remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
    import_task_id BIGINT DEFAULT NULL COMMENT '来源导入任务ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_mobile_deleted (mobile, deleted),
    KEY idx_import_task_id (import_task_id),
    KEY idx_customer_level (customer_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户资料表';
```

这里使用 `UNIQUE KEY uk_mobile_deleted (mobile, deleted)` 做最后一层防重保护。即使业务校验漏掉并发场景，数据库唯一索引也能阻止重复手机号写入。

## Excel 模板设计

Excel 模板要尽量让业务人员一眼看懂，同时让后端解析稳定。建议第一行为表头，第二行开始为数据，不合并单元格，不使用复杂公式，不隐藏必填字段。

### 导入字段定义

客户导入模板字段如下：

| Excel 表头 | Java 字段       | 是否必填 | 示例值                                      | 校验规则           |
| ---------- | --------------- | -------- | ------------------------------------------- | ------------------ |
| 客户名称   | `customerName`  | 是       | 张三客户                                    | 长度 2-50          |
| 手机号     | `mobile`        | 是       | 13800138000                                 | 中国大陆手机号格式 |
| 邮箱       | `email`         | 否       | [test@example.com](mailto:test@example.com) | 非空时校验邮箱格式 |
| 客户等级   | `customerLevel` | 是       | NORMAL                                      | 只允许 NORMAL、VIP |
| 所属城市   | `city`          | 否       | 杭州                                        | 长度不超过 50      |
| 备注       | `remark`        | 否       | 首次导入                                    | 长度不超过 200     |

对应的 EasyExcel 行对象如下。该类既用于模板生成，也用于读取用户上传的 Excel。

文件位置：`src/main/java/io/github/atengk/excel/dto/CustomerImportExcelRow.java`

```java
package io.github.atengk.excel.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 客户导入 Excel 行数据
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CustomerImportExcelRow {

    /**
     * 客户名称
     */
    @ExcelProperty("客户名称")
    private String customerName;

    /**
     * 手机号
     */
    @ExcelProperty("手机号")
    private String mobile;

    /**
     * 邮箱
     */
    @ExcelProperty("邮箱")
    private String email;

    /**
     * 客户等级
     */
    @ExcelProperty("客户等级")
    private String customerLevel;

    /**
     * 所属城市
     */
    @ExcelProperty("所属城市")
    private String city;

    /**
     * 备注
     */
    @ExcelProperty("备注")
    private String remark;
}
```

错误回执需要比导入模板多一列“错误原因”。可以单独定义一个回执行对象，避免污染正常导入对象。

文件位置：`src/main/java/io/github/atengk/excel/dto/CustomerImportReceiptRow.java`

```java
package io.github.atengk.excel.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户导入错误回执行数据
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerImportReceiptRow extends CustomerImportExcelRow {

    /**
     * 错误原因
     */
    @ExcelProperty("错误原因")
    private String errorMessage;
}
```

### 模板下载接口

模板下载接口直接使用 EasyExcel 写出空模板，也可以写入一行示例数据。这里采用“表头 + 示例数据”的方式，方便业务人员按格式填写。

文件位置：`src/main/java/io/github/atengk/excel/controller/CustomerImportController.java`

```java
package io.github.atengk.excel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.excel.EasyExcel;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 客户导入接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequestMapping("/customer/import")
public class CustomerImportController {

    /**
     * 下载客户导入模板
     *
     * @param response HTTP 响应
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        String fileName = "客户导入模板_" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + ".xlsx";
        String encodeFileName = URLUtil.encode(fileName, StandardCharsets.UTF_8);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeFileName);

        try {
            List<CustomerImportExcelRow> rows = buildTemplateRows();
            EasyExcel.write(response.getOutputStream(), CustomerImportExcelRow.class)
                    .sheet("客户导入")
                    .doWrite(rows);
            log.info("客户导入模板下载成功，文件名：{}", fileName);
        } catch (Exception e) {
            log.error("客户导入模板下载失败", e);
            throw new IllegalStateException("客户导入模板下载失败");
        }
    }

    /**
     * 构建模板示例数据
     *
     * @return 示例数据
     */
    private List<CustomerImportExcelRow> buildTemplateRows() {
        CustomerImportExcelRow row = new CustomerImportExcelRow();
        row.setCustomerName("张三客户");
        row.setMobile("13800138000");
        row.setEmail("test@example.com");
        row.setCustomerLevel("NORMAL");
        row.setCity("杭州");
        row.setRemark("示例数据，正式导入前可删除");
        return List.of(row);
    }
}
```

接口调用方式：

```bash
curl -X GET "http://localhost:8080/customer/import/template" \
  -o customer_import_template.xlsx
```

该接口会下载一个 Excel 文件，文件中包含固定表头和一行示例数据。正式导入时，业务人员可以删除示例行后填写真实数据。

### 示例 Excel 结构

下载后的 Excel 结构如下：

| 客户名称 | 手机号      | 邮箱                                        | 客户等级 | 所属城市 | 备注                       |
| -------- | ----------- | ------------------------------------------- | -------- | -------- | -------------------------- |
| 张三客户 | 13800138000 | [test@example.com](mailto:test@example.com) | NORMAL   | 杭州     | 示例数据，正式导入前可删除 |
| 李四客户 | 13900139000 | [lisi@example.com](mailto:lisi@example.com) | VIP      | 上海     | 重点客户                   |
| 王五客户 | 13700137000 |                                             | NORMAL   | 广州     |                            |

后续导入解析时，系统按表头映射到 `CustomerImportExcelRow`，从第二行开始逐行读取数据。建议模板中固定以下约束：

| 约束                      | 原因                              |
| ------------------------- | --------------------------------- |
| 第一行必须是表头          | EasyExcel 根据表头映射字段        |
| 不允许合并单元格          | 合并单元格会导致行数据不稳定      |
| 不允许修改表头名称        | 表头变化会导致字段读取为空        |
| 客户等级使用固定字典值    | 便于后端校验和落库                |
| 手机号按文本填写          | 避免 Excel 将手机号转成科学计数法 |
| 单次导入建议不超过 5 万行 | 控制导入耗时和错误回执大小        |

## 接口设计

本节定义 Excel 导入模块对外暴露的接口。该模块围绕“上传 Excel、创建导入任务、异步解析、查询进度、下载错误回执”展开，符合原 README 中对 Excel 批量导入场景的功能描述。

### 上传 Excel 接口

上传接口只负责保存原始文件、创建导入任务、投递 MQ 消息，然后立即返回 `taskId`。真正的 Excel 解析、校验和入库由 RabbitMQ 消费者异步完成。

| 项目         | 内容                      |
| ------------ | ------------------------- |
| 接口地址     | `/customer/import/upload` |
| 请求方式     | `POST`                    |
| Content-Type | `multipart/form-data`     |
| 请求参数     | `file`                    |
| 返回结果     | 导入任务 ID               |

请求示例：

```bash
curl -X POST "http://localhost:8080/customer/import/upload" \
  -F "file=@customer_import.xlsx"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 100001
}
```

### 查询导入进度接口

前端上传成功后，拿到 `taskId` 定时轮询该接口即可。状态为 `SUCCESS`、`PART_SUCCESS`、`FAILED` 时表示任务已结束。

| 项目     | 内容                                       |
| -------- | ------------------------------------------ |
| 接口地址 | `/customer/import/progress/{taskId}`       |
| 请求方式 | `GET`                                      |
| 路径参数 | `taskId`                                   |
| 返回结果 | 导入进度、成功数量、失败数量、错误回执状态 |

请求示例：

```bash
curl -X GET "http://localhost:8080/customer/import/progress/100001"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": 100001,
    "taskNo": "IMP20260515100001",
    "status": "PART_SUCCESS",
    "statusName": "部分成功",
    "totalCount": 100,
    "successCount": 92,
    "failCount": 8,
    "hasReceipt": true,
    "errorMessage": null
  }
}
```

### 下载错误回执接口

当 `failCount > 0` 时，系统会生成错误回执。错误回执保留原始导入字段，并追加“错误原因”列。

| 项目     | 内容                                |
| -------- | ----------------------------------- |
| 接口地址 | `/customer/import/receipt/{taskId}` |
| 请求方式 | `GET`                               |
| 路径参数 | `taskId`                            |
| 返回结果 | Excel 文件流                        |

请求示例：

```bash
curl -X GET "http://localhost:8080/customer/import/receipt/100001" \
  -o customer_import_receipt.xlsx
```

### 下载导入模板接口

模板下载接口用于生成标准 Excel 模板，避免业务人员修改表头导致后端字段映射失败。

| 项目     | 内容                        |
| -------- | --------------------------- |
| 接口地址 | `/customer/import/template` |
| 请求方式 | `GET`                       |
| 返回结果 | Excel 模板文件流            |

请求示例：

```bash
curl -X GET "http://localhost:8080/customer/import/template" \
  -o customer_import_template.xlsx
```

## 核心代码实现

下面给出核心链路代码。代码基于前面章节的 Maven 依赖、`application.yml` 配置、数据库表和 Excel 行对象继续实现。

### 导入任务创建

导入任务创建发生在上传接口中。系统先校验文件格式，再保存原始 Excel，最后写入 `import_task` 表并投递 MQ 消息。

通用响应对象用于统一接口返回结构。

文件位置：`src/main/java/io/github/atengk/excel/common/Result.java`

```java
package io.github.atengk.excel.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 统一响应
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 失败响应
     *
     * @param message 失败消息
     * @return 统一响应
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

任务状态枚举用于统一任务状态值，避免代码中到处硬编码字符串。

文件位置：`src/main/java/io/github/atengk/excel/enums/ImportTaskStatusEnum.java`

```java
package io.github.atengk.excel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Excel 导入任务状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum ImportTaskStatusEnum {

    PENDING("待处理"),

    PROCESSING("处理中"),

    SUCCESS("全部成功"),

    PART_SUCCESS("部分成功"),

    FAILED("失败");

    private final String name;
}
```

行处理状态枚举用于导入明细表。

文件位置：`src/main/java/io/github/atengk/excel/enums/ImportRowStatusEnum.java`

```java
package io.github.atengk.excel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Excel 导入行处理状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum ImportRowStatusEnum {

    SUCCESS("成功"),

    FAILED("失败");

    private final String name;
}
```

导入任务实体对应 `import_task` 表。

文件位置：`src/main/java/io/github/atengk/excel/entity/ImportTask.java`

```java
package io.github.atengk.excel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Excel 导入任务实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("import_task")
public class ImportTask {

    @TableId
    private Long id;

    private String taskNo;

    private String bizType;

    private String originalFileName;

    private String originalFilePath;

    private String receiptFilePath;

    private String status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private Long createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
```

导入明细实体对应 `import_task_detail` 表。

文件位置：`src/main/java/io/github/atengk/excel/entity/ImportTaskDetail.java`

```java
package io.github.atengk.excel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Excel 导入明细实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("import_task_detail")
public class ImportTaskDetail {

    @TableId
    private Long id;

    private Long taskId;

    private Integer rowIndex;

    private String rowData;

    private String status;

    private String errorMessage;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
```

客户资料实体对应 `customer_info` 表。

文件位置：`src/main/java/io/github/atengk/excel/entity/CustomerInfo.java`

```java
package io.github.atengk.excel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户资料实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("customer_info")
public class CustomerInfo {

    @TableId
    private Long id;

    private String customerName;

    private String mobile;

    private String email;

    private String customerLevel;

    private String city;

    private String remark;

    private Long importTaskId;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
```

Mapper 使用 MyBatis-Plus 基础能力即可，复杂 SQL 暂时不需要 XML。

文件位置：`src/main/java/io/github/atengk/excel/mapper/ImportTaskMapper.java`

```java
package io.github.atengk.excel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.excel.entity.ImportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Excel 导入任务 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ImportTaskMapper extends BaseMapper<ImportTask> {
}
```

文件位置：`src/main/java/io/github/atengk/excel/mapper/ImportTaskDetailMapper.java`

```java
package io.github.atengk.excel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.excel.entity.ImportTaskDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * Excel 导入明细 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ImportTaskDetailMapper extends BaseMapper<ImportTaskDetail> {
}
```

文件位置：`src/main/java/io/github/atengk/excel/mapper/CustomerInfoMapper.java`

```java
package io.github.atengk.excel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.excel.entity.CustomerInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户资料 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface CustomerInfoMapper extends BaseMapper<CustomerInfo> {
}
```

MyBatis-Plus Service 用于支持 `saveBatch` 批量写入。

文件位置：`src/main/java/io/github/atengk/excel/service/ImportTaskService.java`

```java
package io.github.atengk.excel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.excel.entity.ImportTask;

/**
 * Excel 导入任务 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ImportTaskService extends IService<ImportTask> {
}
```

文件位置：`src/main/java/io/github/atengk/excel/service/impl/ImportTaskServiceImpl.java`

```java
package io.github.atengk.excel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.excel.entity.ImportTask;
import io.github.atengk.excel.mapper.ImportTaskMapper;
import io.github.atengk.excel.service.ImportTaskService;
import org.springframework.stereotype.Service;

/**
 * Excel 导入任务 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Service
public class ImportTaskServiceImpl extends ServiceImpl<ImportTaskMapper, ImportTask> implements ImportTaskService {
}
```

文件位置：`src/main/java/io/github/atengk/excel/service/ImportTaskDetailService.java`

```java
package io.github.atengk.excel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.excel.entity.ImportTaskDetail;

/**
 * Excel 导入明细 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ImportTaskDetailService extends IService<ImportTaskDetail> {
}
```

文件位置：`src/main/java/io/github/atengk/excel/service/impl/ImportTaskDetailServiceImpl.java`

```java
package io.github.atengk.excel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.excel.entity.ImportTaskDetail;
import io.github.atengk.excel.mapper.ImportTaskDetailMapper;
import io.github.atengk.excel.service.ImportTaskDetailService;
import org.springframework.stereotype.Service;

/**
 * Excel 导入明细 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Service
public class ImportTaskDetailServiceImpl extends ServiceImpl<ImportTaskDetailMapper, ImportTaskDetail>
        implements ImportTaskDetailService {
}
```

文件位置：`src/main/java/io/github/atengk/excel/service/CustomerInfoService.java`

```java
package io.github.atengk.excel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.excel.entity.CustomerInfo;

/**
 * 客户资料 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface CustomerInfoService extends IService<CustomerInfo> {
}
```

文件位置：`src/main/java/io/github/atengk/excel/service/impl/CustomerInfoServiceImpl.java`

```java
package io.github.atengk.excel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.excel.entity.CustomerInfo;
import io.github.atengk.excel.mapper.CustomerInfoMapper;
import io.github.atengk.excel.service.CustomerInfoService;
import org.springframework.stereotype.Service;

/**
 * 客户资料 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Service
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo>
        implements CustomerInfoService {
}
```

### Excel 文件上传

文件上传需要完成三件事：校验文件、保存原始文件、生成导入任务。这里先使用本地文件存储，后续替换 MinIO 时只需要替换存储服务。

文件存储服务负责保存原始 Excel 和错误回执文件。

文件位置：`src/main/java/io/github/atengk/excel/storage/LocalFileStorageService.java`

```java
package io.github.atengk.excel.storage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import io.github.atengk.excel.config.ExcelImportProperties;
import io.github.atengk.excel.dto.CustomerImportReceiptRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 本地文件存储服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private final ExcelImportProperties properties;

    /**
     * 保存原始 Excel 文件
     *
     * @param file   上传文件
     * @param taskNo 任务编号
     * @return 相对文件路径
     */
    public String saveOriginalFile(MultipartFile file, String taskNo) {
        try {
            String datePath = DateUtil.format(new Date(), "yyyyMMdd");
            String extName = FileUtil.extName(file.getOriginalFilename());
            String fileName = taskNo + "_" + IdUtil.fastSimpleUUID() + "." + extName;

            String relativePath = FileUtil.normalize(
                    properties.getStorage().getOriginalDir() + "/" + datePath + "/" + fileName
            );
            File targetFile = FileUtil.file(properties.getStorage().getLocalRootPath(), relativePath);
            FileUtil.mkParentDirs(targetFile);

            file.transferTo(targetFile);
            log.info("原始 Excel 文件保存成功，路径：{}", targetFile.getAbsolutePath());
            return relativePath;
        } catch (Exception e) {
            log.error("原始 Excel 文件保存失败", e);
            throw new IllegalStateException("原始 Excel 文件保存失败");
        }
    }

    /**
     * 保存错误回执文件
     *
     * @param taskNo 任务编号
     * @param rows   错误回执数据
     * @return 相对文件路径
     */
    public String saveReceiptFile(String taskNo, List<CustomerImportReceiptRow> rows) {
        String datePath = DateUtil.format(new Date(), "yyyyMMdd");
        String fileName = taskNo + "_receipt.xlsx";

        String relativePath = FileUtil.normalize(
                properties.getStorage().getReceiptDir() + "/" + datePath + "/" + fileName
        );
        File targetFile = FileUtil.file(properties.getStorage().getLocalRootPath(), relativePath);
        FileUtil.mkParentDirs(targetFile);

        EasyExcel.write(targetFile, CustomerImportReceiptRow.class)
                .sheet("错误回执")
                .doWrite(rows);

        log.info("错误回执文件生成成功，路径：{}", targetFile.getAbsolutePath());
        return relativePath;
    }

    /**
     * 根据相对路径获取本地文件
     *
     * @param relativePath 相对路径
     * @return 本地文件
     */
    public File getFile(String relativePath) {
        return FileUtil.file(properties.getStorage().getLocalRootPath(), relativePath);
    }
}
```

导入接口 Controller 只做参数接收和服务调用，不在 Controller 中写复杂业务逻辑。

文件位置：`src/main/java/io/github/atengk/excel/controller/CustomerImportController.java`

```java
package io.github.atengk.excel.controller;

import io.github.atengk.excel.common.Result;
import io.github.atengk.excel.service.CustomerImportService;
import io.github.atengk.excel.vo.ImportTaskProgressVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 客户导入接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/customer/import")
public class CustomerImportController {

    private final CustomerImportService customerImportService;

    /**
     * 上传 Excel 并创建导入任务
     *
     * @param file Excel 文件
     * @return 导入任务 ID
     */
    @PostMapping("/upload")
    public Result<Long> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(customerImportService.upload(file));
    }

    /**
     * 查询导入进度
     *
     * @param taskId 任务 ID
     * @return 导入进度
     */
    @GetMapping("/progress/{taskId}")
    public Result<ImportTaskProgressVO> progress(@PathVariable Long taskId) {
        return Result.ok(customerImportService.getProgress(taskId));
    }

    /**
     * 下载错误回执
     *
     * @param taskId   任务 ID
     * @param response HTTP 响应
     */
    @GetMapping("/receipt/{taskId}")
    public void downloadReceipt(@PathVariable Long taskId, HttpServletResponse response) {
        customerImportService.downloadReceipt(taskId, response);
    }

    /**
     * 下载导入模板
     *
     * @param response HTTP 响应
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        customerImportService.downloadTemplate(response);
    }
}
```

### RabbitMQ 异步投递

上传接口不能直接解析 Excel，否则大文件会占用 HTTP 线程。这里使用 RabbitMQ 异步投递导入任务。

MQ 消息只需要携带 `taskId`，消费者根据 `taskId` 查询任务和原始文件路径即可。

文件位置：`src/main/java/io/github/atengk/excel/mq/CustomerImportMessage.java`

```java
package io.github.atengk.excel.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 客户导入 MQ 消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerImportMessage implements Serializable {

    private Long taskId;
}
```

RabbitMQ 配置声明交换机、队列、绑定关系，并使用 JSON 消息转换器。

文件位置：`src/main/java/io/github/atengk/excel/config/ExcelImportRabbitConfig.java`

```java
package io.github.atengk.excel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Excel 导入 RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@RequiredArgsConstructor
public class ExcelImportRabbitConfig {

    private final ExcelImportProperties properties;

    /**
     * 导入任务交换机
     *
     * @return Direct 交换机
     */
    @Bean
    public DirectExchange excelImportExchange() {
        return ExchangeBuilder.directExchange(properties.getMq().getExchange())
                .durable(true)
                .build();
    }

    /**
     * 导入任务队列
     *
     * @return 队列
     */
    @Bean
    public Queue excelImportQueue() {
        return QueueBuilder.durable(properties.getMq().getQueue())
                .build();
    }

    /**
     * 绑定交换机和队列
     *
     * @return 绑定关系
     */
    @Bean
    public Binding excelImportBinding() {
        return BindingBuilder.bind(excelImportQueue())
                .to(excelImportExchange())
                .with(properties.getMq().getRoutingKey());
    }

    /**
     * JSON 消息转换器
     *
     * @return 消息转换器
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param messageConverter  消息转换器
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
```

消费者收到消息后调用导入服务执行真正的 Excel 解析。这里的策略是：业务层已经把任务标记为失败时，消费者确认消息，避免无限重试；生产环境可以进一步接入死信队列和补偿任务。

文件位置：`src/main/java/io/github/atengk/excel/mq/CustomerImportConsumer.java`

```java
package io.github.atengk.excel.mq;

import com.rabbitmq.client.Channel;
import io.github.atengk.excel.service.CustomerImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 客户导入 MQ 消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerImportConsumer {

    private final CustomerImportService customerImportService;

    /**
     * 消费客户导入任务
     *
     * @param importMessage 导入消息
     * @param message       原始消息
     * @param channel       RabbitMQ 通道
     */
    @RabbitListener(queues = "${excel-import.mq.queue}")
    public void consume(CustomerImportMessage importMessage, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("开始消费客户导入任务，taskId：{}", importMessage.getTaskId());
            customerImportService.executeImport(importMessage.getTaskId());
            channel.basicAck(deliveryTag, false);
            log.info("客户导入任务消费成功，taskId：{}", importMessage.getTaskId());
        } catch (Exception e) {
            log.error("客户导入任务消费失败，taskId：{}", importMessage.getTaskId(), e);
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception ackException) {
                log.error("客户导入任务消息确认失败，taskId：{}", importMessage.getTaskId(), ackException);
            }
        }
    }
}
```

### EasyExcel 监听器解析

EasyExcel 监听器负责逐行读取 Excel。它不一次性把整个文件加载到内存，而是按批次缓存数据，达到批次大小后交给批处理服务完成校验、入库和明细记录。

先定义解析过程中的中间对象，用于携带原始行、行号和错误原因。

文件位置：`src/main/java/io/github/atengk/excel/model/ParsedImportRow.java`

```java
package io.github.atengk.excel.model;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 解析后的中间行对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ParsedImportRow {

    private Integer rowIndex;

    private CustomerImportExcelRow row;

    private List<String> errorMessages = new ArrayList<>();

    /**
     * 判断当前行是否校验通过
     *
     * @return true 表示无错误
     */
    public boolean isValid() {
        return CollUtil.isEmpty(errorMessages);
    }

    /**
     * 追加错误消息
     *
     * @param message 错误消息
     */
    public void addError(String message) {
        this.errorMessages.add(message);
    }

    /**
     * 合并错误消息
     *
     * @return 错误消息文本
     */
    public String joinErrorMessage() {
        return String.join("；", errorMessages);
    }
}
```

批处理结果对象用于累计成功数量、失败数量和错误回执数据。

文件位置：`src/main/java/io/github/atengk/excel/model/BatchImportResult.java`

```java
package io.github.atengk.excel.model;

import io.github.atengk.excel.dto.CustomerImportReceiptRow;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批处理结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class BatchImportResult {

    private int totalCount;

    private int successCount;

    private int failCount;

    private List<CustomerImportReceiptRow> receiptRows = new ArrayList<>();

    /**
     * 累加另一个批处理结果
     *
     * @param result 批处理结果
     */
    public void add(BatchImportResult result) {
        this.totalCount += result.getTotalCount();
        this.successCount += result.getSuccessCount();
        this.failCount += result.getFailCount();
        this.receiptRows.addAll(result.getReceiptRows());
    }
}
```

监听器按批次处理数据。这里把 Excel 内部手机号重复校验放在监听器中，因为它需要跨批次记住已经出现过的手机号。

文件位置：`src/main/java/io/github/atengk/excel/listener/CustomerImportReadListener.java`

```java
package io.github.atengk.excel.listener;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import io.github.atengk.excel.model.BatchImportResult;
import io.github.atengk.excel.model.ParsedImportRow;
import io.github.atengk.excel.service.CustomerImportBatchService;
import io.github.atengk.excel.validator.CustomerImportValidator;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户导入 Excel 读取监听器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RequiredArgsConstructor
public class CustomerImportReadListener extends AnalysisEventListener<CustomerImportExcelRow> {

    private static final int BATCH_SIZE = 500;

    private final Long taskId;

    private final CustomerImportValidator validator;

    private final CustomerImportBatchService batchService;

    private final List<ParsedImportRow> cachedRows = new ArrayList<>(BATCH_SIZE);

    private final Set<String> excelMobileSet = new HashSet<>();

    @Getter
    private final BatchImportResult result = new BatchImportResult();

    /**
     * 逐行读取 Excel 数据
     *
     * @param data    当前行数据
     * @param context 解析上下文
     */
    @Override
    public void invoke(CustomerImportExcelRow data, AnalysisContext context) {
        if (validator.isBlankRow(data)) {
            return;
        }

        Integer rowIndex = context.readRowHolder().getRowIndex() + 1;

        ParsedImportRow parsedRow = new ParsedImportRow();
        parsedRow.setRowIndex(rowIndex);
        parsedRow.setRow(data);
        parsedRow.getErrorMessages().addAll(validator.validateBasic(data));

        if (validator.hasMobile(data) && !excelMobileSet.add(data.getMobile())) {
            parsedRow.addError("手机号在 Excel 中重复");
        }

        cachedRows.add(parsedRow);

        if (cachedRows.size() >= BATCH_SIZE) {
            flush();
        }
    }

    /**
     * 所有数据读取完成后处理剩余缓存
     *
     * @param context 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        flush();
        log.info("客户导入 Excel 解析完成，taskId：{}，总行数：{}，成功：{}，失败：{}",
                taskId, result.getTotalCount(), result.getSuccessCount(), result.getFailCount());
    }

    /**
     * 刷新批次数据
     */
    private void flush() {
        if (CollUtil.isEmpty(cachedRows)) {
            return;
        }

        BatchImportResult batchResult = batchService.processBatch(taskId, cachedRows);
        result.add(batchResult);
        cachedRows.clear();
    }
}
```

### 字段格式校验

字段格式校验只处理单行数据本身能判断的问题，例如必填、长度、手机号格式、邮箱格式、枚举值是否合法。数据库重复、跨表规则等放到业务规则校验中处理。

文件位置：`src/main/java/io/github/atengk/excel/validator/CustomerImportValidator.java`

```java
package io.github.atengk.excel.validator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.lang.Validator;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 客户导入数据校验器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
public class CustomerImportValidator {

    private static final Set<String> CUSTOMER_LEVEL_SET = Set.of("NORMAL", "VIP");

    /**
     * 判断是否为空行
     *
     * @param row Excel 行数据
     * @return true 表示空行
     */
    public boolean isBlankRow(CustomerImportExcelRow row) {
        if (row == null) {
            return true;
        }

        return StrUtil.isAllBlank(
                row.getCustomerName(),
                row.getMobile(),
                row.getEmail(),
                row.getCustomerLevel(),
                row.getCity(),
                row.getRemark()
        );
    }

    /**
     * 判断是否有手机号
     *
     * @param row Excel 行数据
     * @return true 表示手机号不为空
     */
    public boolean hasMobile(CustomerImportExcelRow row) {
        return row != null && StrUtil.isNotBlank(row.getMobile());
    }

    /**
     * 基础字段格式校验
     *
     * @param row Excel 行数据
     * @return 错误消息列表
     */
    public List<String> validateBasic(CustomerImportExcelRow row) {
        List<String> errors = new ArrayList<>();

        if (StrUtil.isBlank(row.getCustomerName())) {
            errors.add("客户名称不能为空");
        } else if (StrUtil.length(row.getCustomerName()) < 2 || StrUtil.length(row.getCustomerName()) > 50) {
            errors.add("客户名称长度必须在 2-50 之间");
        }

        if (StrUtil.isBlank(row.getMobile())) {
            errors.add("手机号不能为空");
        } else if (!Validator.isMobile(row.getMobile())) {
            errors.add("手机号格式不正确");
        }

        if (StrUtil.isNotBlank(row.getEmail()) && !Validator.isEmail(row.getEmail())) {
            errors.add("邮箱格式不正确");
        }

        if (StrUtil.isBlank(row.getCustomerLevel())) {
            errors.add("客户等级不能为空");
        } else if (!CollUtil.contains(CUSTOMER_LEVEL_SET, row.getCustomerLevel())) {
            errors.add("客户等级只能是 NORMAL 或 VIP");
        }

        if (StrUtil.isNotBlank(row.getCity()) && StrUtil.length(row.getCity()) > 50) {
            errors.add("所属城市长度不能超过 50");
        }

        if (StrUtil.isNotBlank(row.getRemark()) && StrUtil.length(row.getRemark()) > 200) {
            errors.add("备注长度不能超过 200");
        }

        return errors;
    }
}
```

### 业务规则校验

业务规则校验放在批处理服务中完成。这里主要校验手机号是否已存在数据库。为了避免每行查询一次数据库，先收集当前批次的手机号，再使用 `in` 条件一次性查询。

批处理服务同时承担业务校验、批量入库、失败明细记录和错误回执数据构建。

文件位置：`src/main/java/io/github/atengk/excel/service/CustomerImportBatchService.java`

```java
package io.github.atengk.excel.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.excel.dto.CustomerImportReceiptRow;
import io.github.atengk.excel.entity.CustomerInfo;
import io.github.atengk.excel.entity.ImportTaskDetail;
import io.github.atengk.excel.enums.ImportRowStatusEnum;
import io.github.atengk.excel.model.BatchImportResult;
import io.github.atengk.excel.model.ParsedImportRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户导入批处理服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImportBatchService {

    private final CustomerInfoService customerInfoService;

    private final ImportTaskDetailService importTaskDetailService;

    /**
     * 处理一批 Excel 行数据
     *
     * @param taskId 导入任务 ID
     * @param rows   解析后的行数据
     * @return 批处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult processBatch(Long taskId, List<ParsedImportRow> rows) {
        BatchImportResult result = new BatchImportResult();
        result.setTotalCount(rows.size());

        checkDatabaseDuplicate(rows);

        List<CustomerInfo> customerList = rows.stream()
                .filter(ParsedImportRow::isValid)
                .map(row -> buildCustomerInfo(taskId, row))
                .toList();

        if (CollUtil.isNotEmpty(customerList)) {
            customerInfoService.saveBatch(customerList, 500);
            log.info("客户导入批量入库成功，taskId：{}，数量：{}", taskId, customerList.size());
        }

        List<ImportTaskDetail> detailList = rows.stream()
                .map(row -> buildImportTaskDetail(taskId, row))
                .toList();

        if (CollUtil.isNotEmpty(detailList)) {
            importTaskDetailService.saveBatch(detailList, 500);
        }

        List<CustomerImportReceiptRow> receiptRows = rows.stream()
                .filter(row -> !row.isValid())
                .map(this::buildReceiptRow)
                .toList();

        result.setSuccessCount(customerList.size());
        result.setFailCount(rows.size() - customerList.size());
        result.setReceiptRows(receiptRows);

        return result;
    }

    /**
     * 校验数据库中是否已存在手机号
     *
     * @param rows 当前批次行数据
     */
    private void checkDatabaseDuplicate(List<ParsedImportRow> rows) {
        Set<String> mobileSet = rows.stream()
                .filter(ParsedImportRow::isValid)
                .map(row -> row.getRow().getMobile())
                .collect(Collectors.toSet());

        if (CollUtil.isEmpty(mobileSet)) {
            return;
        }

        Set<String> existsMobileSet = customerInfoService.list(
                        Wrappers.lambdaQuery(CustomerInfo.class)
                                .select(CustomerInfo::getMobile)
                                .in(CustomerInfo::getMobile, mobileSet)
                )
                .stream()
                .map(CustomerInfo::getMobile)
                .collect(Collectors.toSet());

        if (CollUtil.isEmpty(existsMobileSet)) {
            return;
        }

        rows.stream()
                .filter(ParsedImportRow::isValid)
                .filter(row -> existsMobileSet.contains(row.getRow().getMobile()))
                .forEach(row -> row.addError("手机号已存在"));
    }

    /**
     * 构建客户资料实体
     *
     * @param taskId 导入任务 ID
     * @param row    解析后的行数据
     * @return 客户资料实体
     */
    private CustomerInfo buildCustomerInfo(Long taskId, ParsedImportRow row) {
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setCustomerName(row.getRow().getCustomerName());
        customerInfo.setMobile(row.getRow().getMobile());
        customerInfo.setEmail(row.getRow().getEmail());
        customerInfo.setCustomerLevel(row.getRow().getCustomerLevel());
        customerInfo.setCity(row.getRow().getCity());
        customerInfo.setRemark(row.getRow().getRemark());
        customerInfo.setImportTaskId(taskId);
        return customerInfo;
    }

    /**
     * 构建导入明细实体
     *
     * @param taskId 导入任务 ID
     * @param row    解析后的行数据
     * @return 导入明细实体
     */
    private ImportTaskDetail buildImportTaskDetail(Long taskId, ParsedImportRow row) {
        ImportTaskDetail detail = new ImportTaskDetail();
        detail.setTaskId(taskId);
        detail.setRowIndex(row.getRowIndex());
        detail.setRowData(JSONUtil.toJsonStr(row.getRow()));
        detail.setStatus(row.isValid() ? ImportRowStatusEnum.SUCCESS.name() : ImportRowStatusEnum.FAILED.name());
        detail.setErrorMessage(row.isValid() ? null : row.joinErrorMessage());
        return detail;
    }

    /**
     * 构建错误回执行
     *
     * @param row 解析后的行数据
     * @return 错误回执行
     */
    private CustomerImportReceiptRow buildReceiptRow(ParsedImportRow row) {
        CustomerImportReceiptRow receiptRow = new CustomerImportReceiptRow();
        BeanUtil.copyProperties(row.getRow(), receiptRow);
        receiptRow.setErrorMessage(row.joinErrorMessage());
        return receiptRow;
    }
}
```

### 批量入库处理

批量入库已经在 `CustomerImportBatchService#processBatch` 中完成，核心是这段逻辑：

```java
List<CustomerInfo> customerList = rows.stream()
        .filter(ParsedImportRow::isValid)
        .map(row -> buildCustomerInfo(taskId, row))
        .toList();

if (CollUtil.isNotEmpty(customerList)) {
    customerInfoService.saveBatch(customerList, 500);
    log.info("客户导入批量入库成功，taskId：{}，数量：{}", taskId, customerList.size());
}
```

这里的批量大小设置为 `500`，适合普通业务导入。数据量更大时可以根据数据库性能调整到 `1000` 或 `2000`，但不建议一次性提交几万行，容易造成事务过大、锁持有时间过长和内存压力过高。

需要注意的是，业务层已经做了数据库重复校验，但仍然建议保留 `customer_info.mobile` 的唯一索引。业务校验用于友好返回错误原因，唯一索引用于兜底防并发重复写入。

### 失败数据记录

失败数据会写入两处：

| 位置                 | 用途                               |
| -------------------- | ---------------------------------- |
| `import_task_detail` | 保存每一行的处理结果，便于后台排查 |
| 错误回执 Excel       | 给业务人员下载修改后重新导入       |

导入明细记录的核心代码在 `buildImportTaskDetail` 中：

```java
ImportTaskDetail detail = new ImportTaskDetail();
detail.setTaskId(taskId);
detail.setRowIndex(row.getRowIndex());
detail.setRowData(JSONUtil.toJsonStr(row.getRow()));
detail.setStatus(row.isValid() ? ImportRowStatusEnum.SUCCESS.name() : ImportRowStatusEnum.FAILED.name());
detail.setErrorMessage(row.isValid() ? null : row.joinErrorMessage());
```

建议保留 `rowIndex` 和 `rowData`。`rowIndex` 用于定位 Excel 原始行号，`rowData` 用于问题排查或后续重新生成错误回执。

### 错误回执文件生成

错误回执只在存在失败行时生成。回执行对象比普通导入对象多一个 `错误原因` 字段。

如果前面章节已经创建过 `CustomerImportReceiptRow`，这里保持一致即可。

文件位置：`src/main/java/io/github/atengk/excel/dto/CustomerImportReceiptRow.java`

```java
package io.github.atengk.excel.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户导入错误回执行数据
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerImportReceiptRow extends CustomerImportExcelRow {

    /**
     * 错误原因
     */
    @ExcelProperty("错误原因")
    private String errorMessage;
}
```

错误回执文件生成逻辑在 `LocalFileStorageService#saveReceiptFile` 中：

```java
EasyExcel.write(targetFile, CustomerImportReceiptRow.class)
        .sheet("错误回执")
        .doWrite(rows);
```

生成后的相对路径会写回 `import_task.receipt_file_path`，前端查询进度时根据 `hasReceipt` 判断是否展示“下载错误回执”按钮。

### 导入进度更新

导入进度由 `import_task` 表承载。上传时为 `PENDING`，消费者开始处理后更新为 `PROCESSING`，处理完成后根据成功数和失败数更新为最终状态。

先定义进度查询 VO。

文件位置：`src/main/java/io/github/atengk/excel/vo/ImportTaskProgressVO.java`

```java
package io.github.atengk.excel.vo;

import lombok.Data;

/**
 * Excel 导入进度 VO
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ImportTaskProgressVO {

    private Long taskId;

    private String taskNo;

    private String status;

    private String statusName;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private Boolean hasReceipt;

    private String errorMessage;
}
```

导入服务接口定义上传、进度查询、文件下载和异步执行方法。

文件位置：`src/main/java/io/github/atengk/excel/service/CustomerImportService.java`

```java
package io.github.atengk.excel.service;

import io.github.atengk.excel.vo.ImportTaskProgressVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 客户导入 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface CustomerImportService {

    /**
     * 上传 Excel 并创建导入任务
     *
     * @param file Excel 文件
     * @return 导入任务 ID
     */
    Long upload(MultipartFile file);

    /**
     * 查询导入进度
     *
     * @param taskId 任务 ID
     * @return 导入进度
     */
    ImportTaskProgressVO getProgress(Long taskId);

    /**
     * 下载错误回执
     *
     * @param taskId   任务 ID
     * @param response HTTP 响应
     */
    void downloadReceipt(Long taskId, HttpServletResponse response);

    /**
     * 下载导入模板
     *
     * @param response HTTP 响应
     */
    void downloadTemplate(HttpServletResponse response);

    /**
     * 执行导入任务
     *
     * @param taskId 任务 ID
     */
    void executeImport(Long taskId);
}
```

导入服务实现类串联完整链路：上传创建任务、投递 MQ、查询进度、下载回执、执行异步导入、更新任务状态。

文件位置：`src/main/java/io/github/atengk/excel/service/impl/CustomerImportServiceImpl.java`

```java
package io.github.atengk.excel.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.excel.EasyExcel;
import io.github.atengk.excel.config.ExcelImportProperties;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import io.github.atengk.excel.entity.ImportTask;
import io.github.atengk.excel.enums.ImportTaskStatusEnum;
import io.github.atengk.excel.listener.CustomerImportReadListener;
import io.github.atengk.excel.model.BatchImportResult;
import io.github.atengk.excel.mq.CustomerImportMessage;
import io.github.atengk.excel.service.CustomerImportBatchService;
import io.github.atengk.excel.service.CustomerImportService;
import io.github.atengk.excel.service.ImportTaskService;
import io.github.atengk.excel.storage.LocalFileStorageService;
import io.github.atengk.excel.validator.CustomerImportValidator;
import io.github.atengk.excel.vo.ImportTaskProgressVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 客户导入 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImportServiceImpl implements CustomerImportService {

    private static final String BIZ_TYPE = "CUSTOMER_IMPORT";

    private final ImportTaskService importTaskService;

    private final LocalFileStorageService fileStorageService;

    private final RabbitTemplate rabbitTemplate;

    private final ExcelImportProperties properties;

    private final CustomerImportValidator validator;

    private final CustomerImportBatchService batchService;

    /**
     * 上传 Excel 并创建导入任务
     *
     * @param file Excel 文件
     * @return 导入任务 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upload(MultipartFile file) {
        checkExcelFile(file);

        Long taskId = IdUtil.getSnowflakeNextId();
        String taskNo = "IMP" + DateUtil.format(DateUtil.date(), "yyyyMMdd") + IdUtil.getSnowflakeNextIdStr();

        String originalFilePath = fileStorageService.saveOriginalFile(file, taskNo);

        ImportTask task = new ImportTask();
        task.setId(taskId);
        task.setTaskNo(taskNo);
        task.setBizType(BIZ_TYPE);
        task.setOriginalFileName(file.getOriginalFilename());
        task.setOriginalFilePath(originalFilePath);
        task.setStatus(ImportTaskStatusEnum.PENDING.name());
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);

        importTaskService.save(task);

        rabbitTemplate.convertAndSend(
                properties.getMq().getExchange(),
                properties.getMq().getRoutingKey(),
                new CustomerImportMessage(taskId)
        );

        log.info("客户导入任务创建成功，taskId：{}，taskNo：{}", taskId, taskNo);
        return taskId;
    }

    /**
     * 查询导入进度
     *
     * @param taskId 任务 ID
     * @return 导入进度
     */
    @Override
    public ImportTaskProgressVO getProgress(Long taskId) {
        ImportTask task = getTaskOrThrow(taskId);

        ImportTaskStatusEnum statusEnum = ImportTaskStatusEnum.valueOf(task.getStatus());

        ImportTaskProgressVO vo = new ImportTaskProgressVO();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setStatus(task.getStatus());
        vo.setStatusName(statusEnum.getName());
        vo.setTotalCount(task.getTotalCount());
        vo.setSuccessCount(task.getSuccessCount());
        vo.setFailCount(task.getFailCount());
        vo.setHasReceipt(StrUtil.isNotBlank(task.getReceiptFilePath()));
        vo.setErrorMessage(task.getErrorMessage());
        return vo;
    }

    /**
     * 下载错误回执
     *
     * @param taskId   任务 ID
     * @param response HTTP 响应
     */
    @Override
    public void downloadReceipt(Long taskId, HttpServletResponse response) {
        ImportTask task = getTaskOrThrow(taskId);

        if (StrUtil.isBlank(task.getReceiptFilePath())) {
            throw new IllegalArgumentException("当前任务没有错误回执");
        }

        File receiptFile = fileStorageService.getFile(task.getReceiptFilePath());
        if (!FileUtil.exist(receiptFile)) {
            throw new IllegalArgumentException("错误回执文件不存在");
        }

        String fileName = "客户导入错误回执_" + task.getTaskNo() + ".xlsx";
        String encodeFileName = URLUtil.encode(fileName, StandardCharsets.UTF_8);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeFileName);

        try (InputStream inputStream = FileUtil.getInputStream(receiptFile)) {
            IoUtil.copy(inputStream, response.getOutputStream());
            log.info("错误回执下载成功，taskId：{}，文件：{}", taskId, receiptFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("错误回执下载失败，taskId：{}", taskId, e);
            throw new IllegalStateException("错误回执下载失败");
        }
    }

    /**
     * 下载导入模板
     *
     * @param response HTTP 响应
     */
    @Override
    public void downloadTemplate(HttpServletResponse response) {
        String fileName = "客户导入模板_" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + ".xlsx";
        String encodeFileName = URLUtil.encode(fileName, StandardCharsets.UTF_8);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeFileName);

        try {
            EasyExcel.write(response.getOutputStream(), CustomerImportExcelRow.class)
                    .sheet("客户导入")
                    .doWrite(buildTemplateRows());
            log.info("客户导入模板下载成功，文件名：{}", fileName);
        } catch (Exception e) {
            log.error("客户导入模板下载失败", e);
            throw new IllegalStateException("客户导入模板下载失败");
        }
    }

    /**
     * 执行导入任务
     *
     * @param taskId 任务 ID
     */
    @Override
    public void executeImport(Long taskId) {
        ImportTask task = getTaskOrThrow(taskId);

        try {
            updateProcessing(taskId);

            File originalFile = fileStorageService.getFile(task.getOriginalFilePath());
            if (!FileUtil.exist(originalFile)) {
                throw new IllegalArgumentException("原始 Excel 文件不存在");
            }

            CustomerImportReadListener listener = new CustomerImportReadListener(taskId, validator, batchService);

            EasyExcel.read(originalFile, CustomerImportExcelRow.class, listener)
                    .sheet()
                    .doRead();

            BatchImportResult result = listener.getResult();
            String receiptFilePath = null;

            if (result.getFailCount() > 0) {
                receiptFilePath = fileStorageService.saveReceiptFile(task.getTaskNo(), result.getReceiptRows());
            }

            updateFinished(taskId, result, receiptFilePath);
        } catch (Exception e) {
            log.error("客户导入任务执行失败，taskId：{}", taskId, e);
            updateFailed(taskId, e);
            throw e;
        }
    }

    /**
     * 校验 Excel 文件
     *
     * @param file Excel 文件
     */
    private void checkExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel 文件不能为空");
        }

        String extName = StrUtil.emptyToDefault(FileUtil.extName(file.getOriginalFilename()), "")
                .toLowerCase(Locale.ROOT);

        if (!List.of("xls", "xlsx").contains(extName)) {
            throw new IllegalArgumentException("仅支持 xls、xlsx 格式文件");
        }
    }

    /**
     * 根据任务 ID 获取导入任务
     *
     * @param taskId 任务 ID
     * @return 导入任务
     */
    private ImportTask getTaskOrThrow(Long taskId) {
        ImportTask task = importTaskService.getById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("导入任务不存在");
        }
        return task;
    }

    /**
     * 更新任务为处理中
     *
     * @param taskId 任务 ID
     */
    private void updateProcessing(Long taskId) {
        ImportTask update = new ImportTask();
        update.setId(taskId);
        update.setStatus(ImportTaskStatusEnum.PROCESSING.name());
        update.setStartTime(LocalDateTime.now());
        importTaskService.updateById(update);
    }

    /**
     * 更新任务为已完成
     *
     * @param taskId          任务 ID
     * @param result          导入结果
     * @param receiptFilePath 错误回执路径
     */
    private void updateFinished(Long taskId, BatchImportResult result, String receiptFilePath) {
        ImportTask update = new ImportTask();
        update.setId(taskId);
        update.setTotalCount(result.getTotalCount());
        update.setSuccessCount(result.getSuccessCount());
        update.setFailCount(result.getFailCount());
        update.setReceiptFilePath(receiptFilePath);
        update.setFinishTime(LocalDateTime.now());

        if (result.getFailCount() == 0) {
            update.setStatus(ImportTaskStatusEnum.SUCCESS.name());
        } else if (result.getSuccessCount() == 0) {
            update.setStatus(ImportTaskStatusEnum.FAILED.name());
        } else {
            update.setStatus(ImportTaskStatusEnum.PART_SUCCESS.name());
        }

        importTaskService.updateById(update);
        log.info("客户导入任务完成，taskId：{}，总数：{}，成功：{}，失败：{}",
                taskId, result.getTotalCount(), result.getSuccessCount(), result.getFailCount());
    }

    /**
     * 更新任务为失败
     *
     * @param taskId 任务 ID
     * @param e      异常
     */
    private void updateFailed(Long taskId, Exception e) {
        ImportTask update = new ImportTask();
        update.setId(taskId);
        update.setStatus(ImportTaskStatusEnum.FAILED.name());
        update.setErrorMessage(StrUtil.subPre(StrUtil.blankToDefault(e.getMessage(), "系统异常"), 1000));
        update.setFinishTime(LocalDateTime.now());
        importTaskService.updateById(update);
    }

    /**
     * 构建模板示例数据
     *
     * @return 模板示例数据
     */
    private List<CustomerImportExcelRow> buildTemplateRows() {
        CustomerImportExcelRow row = new CustomerImportExcelRow();
        row.setCustomerName("张三客户");
        row.setMobile("13800138000");
        row.setEmail("test@example.com");
        row.setCustomerLevel("NORMAL");
        row.setCity("杭州");
        row.setRemark("示例数据，正式导入前可删除");
        return List.of(row);
    }
}
```

至此，核心链路已经闭合：

```text
上传 Excel
-> 保存原始文件
-> 创建 import_task
-> 投递 RabbitMQ
-> 消费者异步执行
-> EasyExcel 批量解析
-> 字段格式校验
-> 数据库重复校验
-> 正确数据批量入库
-> 失败数据写入 import_task_detail
-> 生成错误回执
-> 更新任务进度
```

这个版本已经能覆盖真实后台系统中最常见的 Excel 导入场景。后续如果要增强，可以继续补充全局异常处理、接口鉴权、MinIO 存储实现、死信队列、导入任务重试和 XXL-JOB 补偿扫描。

## 校验规则实现

校验规则是 Excel 导入的核心。原 README 中该场景的难点包括批量校验、重复数据处理、部分成功部分失败、错误行回写等，这里将校验拆成两层：单行字段校验和批量业务校验。

单行字段校验负责必填、格式、字典值；批量业务校验负责数据库重复、Excel 内部重复等需要跨行或查库的规则。

推荐校验顺序如下：

```text
读取 Excel 行
-> 空行过滤
-> 必填校验
-> 字段格式校验
-> 数据字典校验
-> Excel 内部重复校验
-> 数据库重复校验
-> 校验通过的数据批量入库
-> 校验失败的数据生成错误回执
```

### 必填校验

必填校验只判断字段是否为空，不处理格式是否合法。这样可以让错误原因更清晰，例如“手机号不能为空”和“手机号格式不正确”不会混在一起。

下面是完整的客户导入校验器，包含必填、格式和字典校验。

文件位置：`src/main/java/io/github/atengk/excel/validator/CustomerImportValidator.java`

```java
package io.github.atengk.excel.validator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import io.github.atengk.excel.enums.CustomerLevelEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 客户导入字段校验器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
public class CustomerImportValidator {

    /**
     * 判断是否为空行
     *
     * @param row Excel 行数据
     * @return true 表示空行
     */
    public boolean isBlankRow(CustomerImportExcelRow row) {
        if (row == null) {
            return true;
        }

        return StrUtil.isAllBlank(
                row.getCustomerName(),
                row.getMobile(),
                row.getEmail(),
                row.getCustomerLevel(),
                row.getCity(),
                row.getRemark()
        );
    }

    /**
     * 判断是否有手机号
     *
     * @param row Excel 行数据
     * @return true 表示手机号不为空
     */
    public boolean hasMobile(CustomerImportExcelRow row) {
        return row != null && StrUtil.isNotBlank(row.getMobile());
    }

    /**
     * 执行单行基础校验
     *
     * @param row Excel 行数据
     * @return 错误信息列表
     */
    public List<String> validateBasic(CustomerImportExcelRow row) {
        List<String> errors = new ArrayList<>();
        errors.addAll(validateRequired(row));
        errors.addAll(validateFormat(row));
        errors.addAll(validateDictionary(row));
        return errors;
    }

    /**
     * 必填校验
     *
     * @param row Excel 行数据
     * @return 错误信息列表
     */
    public List<String> validateRequired(CustomerImportExcelRow row) {
        List<String> errors = new ArrayList<>();

        if (StrUtil.isBlank(row.getCustomerName())) {
            errors.add("客户名称不能为空");
        }

        if (StrUtil.isBlank(row.getMobile())) {
            errors.add("手机号不能为空");
        }

        if (StrUtil.isBlank(row.getCustomerLevel())) {
            errors.add("客户等级不能为空");
        }

        return errors;
    }

    /**
     * 字段格式校验
     *
     * @param row Excel 行数据
     * @return 错误信息列表
     */
    public List<String> validateFormat(CustomerImportExcelRow row) {
        List<String> errors = new ArrayList<>();

        if (StrUtil.isNotBlank(row.getCustomerName())
                && (StrUtil.length(row.getCustomerName()) < 2 || StrUtil.length(row.getCustomerName()) > 50)) {
            errors.add("客户名称长度必须在 2-50 之间");
        }

        if (StrUtil.isNotBlank(row.getMobile()) && !Validator.isMobile(row.getMobile())) {
            errors.add("手机号格式不正确");
        }

        if (StrUtil.isNotBlank(row.getEmail()) && !Validator.isEmail(row.getEmail())) {
            errors.add("邮箱格式不正确");
        }

        if (StrUtil.isNotBlank(row.getCity()) && StrUtil.length(row.getCity()) > 50) {
            errors.add("所属城市长度不能超过 50");
        }

        if (StrUtil.isNotBlank(row.getRemark()) && StrUtil.length(row.getRemark()) > 200) {
            errors.add("备注长度不能超过 200");
        }

        return errors;
    }

    /**
     * 数据字典校验
     *
     * @param row Excel 行数据
     * @return 错误信息列表
     */
    public List<String> validateDictionary(CustomerImportExcelRow row) {
        List<String> errors = new ArrayList<>();

        if (StrUtil.isBlank(row.getCustomerLevel())) {
            return errors;
        }

        Set<String> levelCodes = CustomerLevelEnum.codeSet();
        if (!CollUtil.contains(levelCodes, row.getCustomerLevel())) {
            errors.add("客户等级只能填写 NORMAL 或 VIP");
        }

        return errors;
    }
}
```

### 字段格式校验

字段格式校验建议只处理“当前单元格本身是否符合格式”，不要在这里查数据库，也不要处理跨行重复。这样校验器可以保持轻量，后续商品导入、库存导入也能复用类似写法。

本案例格式校验规则如下：

| 字段     | 校验规则             | 错误提示                     |
| -------- | -------------------- | ---------------------------- |
| 客户名称 | 2-50 个字符          | 客户名称长度必须在 2-50 之间 |
| 手机号   | 手机号格式           | 手机号格式不正确             |
| 邮箱     | 非空时必须是邮箱格式 | 邮箱格式不正确               |
| 所属城市 | 不超过 50 个字符     | 所属城市长度不能超过 50      |
| 备注     | 不超过 200 个字符    | 备注长度不能超过 200         |

核心代码已经放在 `validateFormat` 方法中：

```java
if (StrUtil.isNotBlank(row.getMobile()) && !Validator.isMobile(row.getMobile())) {
    errors.add("手机号格式不正确");
}

if (StrUtil.isNotBlank(row.getEmail()) && !Validator.isEmail(row.getEmail())) {
    errors.add("邮箱格式不正确");
}
```

这里使用 Hutool 的 `Validator`，避免自己维护手机号和邮箱正则。真实项目中如果手机号规则更复杂，例如支持海外手机号，可以替换为业务自定义正则。

### 数据字典校验

客户等级属于字典字段。这里先用枚举实现，适合固定值较少的场景。如果项目中已经有字典表，可以改成从 Redis 或数据库读取字典项。

文件位置：`src/main/java/io/github/atengk/excel/enums/CustomerLevelEnum.java`

```java
package io.github.atengk.excel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户等级枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum CustomerLevelEnum {

    NORMAL("普通客户"),

    VIP("VIP客户");

    private final String name;

    /**
     * 获取全部客户等级编码
     *
     * @return 编码集合
     */
    public static Set<String> codeSet() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}
```

如果使用字典表，建议将校验逻辑改成下面这种方式：

```text
启动时加载字典到 Redis
-> 导入时从 Redis 查询合法字典值
-> Redis 未命中再查数据库
-> 校验 Excel 字段是否属于合法字典值
```

客户等级这种低频变更字段，可以先用枚举；状态、类型、渠道等后台可配置字段，建议使用字典表加缓存。

### 数据库重复校验

数据库重复校验不能逐行查库，否则 1 万行 Excel 就会产生 1 万次 SQL。正确做法是按批次收集手机号，使用 `IN` 一次性查出已存在的数据，再回填到对应行的错误信息中。

下面的业务校验器只处理查库类校验。

文件位置：`src/main/java/io/github/atengk/excel/validator/CustomerImportBusinessValidator.java`

```java
package io.github.atengk.excel.validator;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.excel.entity.CustomerInfo;
import io.github.atengk.excel.model.ParsedImportRow;
import io.github.atengk.excel.service.CustomerInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户导入业务规则校验器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerImportBusinessValidator {

    private final CustomerInfoService customerInfoService;

    /**
     * 校验手机号是否已存在数据库
     *
     * @param rows 当前批次行数据
     */
    public void validateDatabaseDuplicate(List<ParsedImportRow> rows) {
        Set<String> mobileSet = rows.stream()
                .filter(ParsedImportRow::isValid)
                .map(row -> row.getRow().getMobile())
                .collect(Collectors.toSet());

        if (CollUtil.isEmpty(mobileSet)) {
            return;
        }

        Set<String> existsMobileSet = customerInfoService.list(
                        Wrappers.lambdaQuery(CustomerInfo.class)
                                .select(CustomerInfo::getMobile)
                                .in(CustomerInfo::getMobile, mobileSet)
                )
                .stream()
                .map(CustomerInfo::getMobile)
                .collect(Collectors.toSet());

        if (CollUtil.isEmpty(existsMobileSet)) {
            return;
        }

        rows.stream()
                .filter(ParsedImportRow::isValid)
                .filter(row -> existsMobileSet.contains(row.getRow().getMobile()))
                .forEach(row -> row.addError("手机号已存在"));

        log.info("客户导入数据库重复校验完成，批次数量：{}，重复手机号数量：{}", rows.size(), existsMobileSet.size());
    }
}
```

然后在批处理服务中调用该校验器。

文件位置：`src/main/java/io/github/atengk/excel/service/CustomerImportBatchService.java`

```java
package io.github.atengk.excel.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.excel.dto.CustomerImportReceiptRow;
import io.github.atengk.excel.entity.CustomerInfo;
import io.github.atengk.excel.entity.ImportTaskDetail;
import io.github.atengk.excel.enums.ImportRowStatusEnum;
import io.github.atengk.excel.model.BatchImportResult;
import io.github.atengk.excel.model.ParsedImportRow;
import io.github.atengk.excel.validator.CustomerImportBusinessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户导入批处理服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImportBatchService {

    private final CustomerInfoService customerInfoService;

    private final ImportTaskDetailService importTaskDetailService;

    private final CustomerImportBusinessValidator businessValidator;

    /**
     * 处理一批 Excel 行数据
     *
     * @param taskId 导入任务 ID
     * @param rows   解析后的行数据
     * @return 批处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult processBatch(Long taskId, List<ParsedImportRow> rows) {
        BatchImportResult result = new BatchImportResult();
        result.setTotalCount(rows.size());

        businessValidator.validateDatabaseDuplicate(rows);

        List<CustomerInfo> customerList = rows.stream()
                .filter(ParsedImportRow::isValid)
                .map(row -> buildCustomerInfo(taskId, row))
                .toList();

        if (CollUtil.isNotEmpty(customerList)) {
            customerInfoService.saveBatch(customerList, 500);
            log.info("客户导入批量入库成功，taskId：{}，数量：{}", taskId, customerList.size());
        }

        List<ImportTaskDetail> detailList = rows.stream()
                .map(row -> buildImportTaskDetail(taskId, row))
                .toList();

        if (CollUtil.isNotEmpty(detailList)) {
            importTaskDetailService.saveBatch(detailList, 500);
        }

        List<CustomerImportReceiptRow> receiptRows = rows.stream()
                .filter(row -> !row.isValid())
                .map(this::buildReceiptRow)
                .toList();

        result.setSuccessCount(customerList.size());
        result.setFailCount(rows.size() - customerList.size());
        result.setReceiptRows(receiptRows);
        return result;
    }

    /**
     * 构建客户资料实体
     *
     * @param taskId 导入任务 ID
     * @param row    解析后的行数据
     * @return 客户资料实体
     */
    private CustomerInfo buildCustomerInfo(Long taskId, ParsedImportRow row) {
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setCustomerName(row.getRow().getCustomerName());
        customerInfo.setMobile(row.getRow().getMobile());
        customerInfo.setEmail(row.getRow().getEmail());
        customerInfo.setCustomerLevel(row.getRow().getCustomerLevel());
        customerInfo.setCity(row.getRow().getCity());
        customerInfo.setRemark(row.getRow().getRemark());
        customerInfo.setImportTaskId(taskId);
        return customerInfo;
    }

    /**
     * 构建导入明细实体
     *
     * @param taskId 导入任务 ID
     * @param row    解析后的行数据
     * @return 导入明细实体
     */
    private ImportTaskDetail buildImportTaskDetail(Long taskId, ParsedImportRow row) {
        ImportTaskDetail detail = new ImportTaskDetail();
        detail.setTaskId(taskId);
        detail.setRowIndex(row.getRowIndex());
        detail.setRowData(JSONUtil.toJsonStr(row.getRow()));
        detail.setStatus(row.isValid() ? ImportRowStatusEnum.SUCCESS.name() : ImportRowStatusEnum.FAILED.name());
        detail.setErrorMessage(row.isValid() ? null : row.joinErrorMessage());
        return detail;
    }

    /**
     * 构建错误回执行
     *
     * @param row 解析后的行数据
     * @return 错误回执行
     */
    private CustomerImportReceiptRow buildReceiptRow(ParsedImportRow row) {
        CustomerImportReceiptRow receiptRow = new CustomerImportReceiptRow();
        BeanUtil.copyProperties(row.getRow(), receiptRow);
        receiptRow.setErrorMessage(row.joinErrorMessage());
        return receiptRow;
    }
}
```

### Excel 内部重复校验

Excel 内部重复校验用于处理“同一个 Excel 中出现两个相同手机号”的情况。这个校验不能只看当前批次，否则第 1 批和第 2 批之间的重复数据会漏掉。

推荐在监听器中维护一个 `Set<String>`，整个文件解析期间都共享这个集合。

文件位置：`src/main/java/io/github/atengk/excel/listener/CustomerImportReadListener.java`

```java
package io.github.atengk.excel.listener;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import io.github.atengk.excel.model.BatchImportResult;
import io.github.atengk.excel.model.ParsedImportRow;
import io.github.atengk.excel.service.CustomerImportBatchService;
import io.github.atengk.excel.validator.CustomerImportValidator;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户导入 Excel 读取监听器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RequiredArgsConstructor
public class CustomerImportReadListener extends AnalysisEventListener<CustomerImportExcelRow> {

    private static final int BATCH_SIZE = 500;

    private final Long taskId;

    private final CustomerImportValidator validator;

    private final CustomerImportBatchService batchService;

    private final List<ParsedImportRow> cachedRows = new ArrayList<>(BATCH_SIZE);

    private final Set<String> excelMobileSet = new HashSet<>();

    @Getter
    private final BatchImportResult result = new BatchImportResult();

    /**
     * 逐行读取 Excel 数据
     *
     * @param data    当前行数据
     * @param context 解析上下文
     */
    @Override
    public void invoke(CustomerImportExcelRow data, AnalysisContext context) {
        if (validator.isBlankRow(data)) {
            return;
        }

        Integer rowIndex = context.readRowHolder().getRowIndex() + 1;

        ParsedImportRow parsedRow = new ParsedImportRow();
        parsedRow.setRowIndex(rowIndex);
        parsedRow.setRow(data);
        parsedRow.getErrorMessages().addAll(validator.validateBasic(data));

        validateExcelDuplicate(parsedRow);

        cachedRows.add(parsedRow);

        if (cachedRows.size() >= BATCH_SIZE) {
            flush();
        }
    }

    /**
     * 全部读取完成后处理剩余数据
     *
     * @param context 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        flush();
        log.info("客户导入 Excel 解析完成，taskId：{}，总行数：{}，成功：{}，失败：{}",
                taskId, result.getTotalCount(), result.getSuccessCount(), result.getFailCount());
    }

    /**
     * 校验 Excel 内部手机号重复
     *
     * @param parsedRow 解析后的行数据
     */
    private void validateExcelDuplicate(ParsedImportRow parsedRow) {
        if (!validator.hasMobile(parsedRow.getRow())) {
            return;
        }

        String mobile = parsedRow.getRow().getMobile();
        if (!excelMobileSet.add(mobile)) {
            parsedRow.addError("手机号在 Excel 中重复");
        }
    }

    /**
     * 刷新批次数据
     */
    private void flush() {
        if (CollUtil.isEmpty(cachedRows)) {
            return;
        }

        BatchImportResult batchResult = batchService.processBatch(taskId, cachedRows);
        result.add(batchResult);
        cachedRows.clear();
    }
}
```

这里的重复规则是：第一次出现的手机号允许继续处理，第二次及以后出现的相同手机号标记为失败。

例如：

| Excel 行号 | 手机号      | 结果                        |
| ---------- | ----------- | --------------------------- |
| 2          | 13800138000 | 通过                        |
| 3          | 13900139000 | 通过                        |
| 4          | 13800138000 | 失败：手机号在 Excel 中重复 |

如果业务要求“只要 Excel 内部重复，该手机号所有行都失败”，则需要先扫描全量手机号计数，再统一回填错误。该策略更严格，但会增加一次预处理成本。

## 错误回执实现

错误回执用于把失败行返还给业务人员。回执文件应尽量保留原始字段，并追加“错误原因”，用户修改后可以重新上传。

本案例的错误回执结构如下：

| 客户名称 | 手机号      | 邮箱                                  | 客户等级 | 所属城市 | 备注 | 错误原因                       |
| -------- | ----------- | ------------------------------------- | -------- | -------- | ---- | ------------------------------ |
| A客户    | 13800138000 | test                                  | NORMAL   | 杭州     |      | 邮箱格式不正确                 |
| B客户    | 13900139000 | [b@example.com](mailto:b@example.com) | GOLD     | 上海     |      | 客户等级只能填写 NORMAL 或 VIP |
| C客户    | 13800138000 | [c@example.com](mailto:c@example.com) | VIP      | 广州     |      | 手机号在 Excel 中重复          |

### 错误信息回写

错误信息回写的关键是每一行都要携带自己的错误集合。前面已经定义了 `ParsedImportRow`，这里再给出完整版本。

文件位置：`src/main/java/io/github/atengk/excel/model/ParsedImportRow.java`

```java
package io.github.atengk.excel.model;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 解析后的中间行对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ParsedImportRow {

    private Integer rowIndex;

    private CustomerImportExcelRow row;

    private List<String> errorMessages = new ArrayList<>();

    /**
     * 判断当前行是否校验通过
     *
     * @return true 表示无错误
     */
    public boolean isValid() {
        return CollUtil.isEmpty(errorMessages);
    }

    /**
     * 追加错误信息
     *
     * @param message 错误信息
     */
    public void addError(String message) {
        this.errorMessages.add(message);
    }

    /**
     * 合并错误信息
     *
     * @return 错误信息文本
     */
    public String joinErrorMessage() {
        return String.join("；", errorMessages);
    }
}
```

错误回执行对象比正常导入对象多一列“错误原因”。

文件位置：`src/main/java/io/github/atengk/excel/dto/CustomerImportReceiptRow.java`

```java
package io.github.atengk.excel.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户导入错误回执行数据
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerImportReceiptRow extends CustomerImportExcelRow {

    /**
     * 错误原因
     */
    @ExcelProperty("错误原因")
    private String errorMessage;
}
```

回写转换逻辑建议独立成方法，不要散落在监听器或 Controller 中。

```java
private CustomerImportReceiptRow buildReceiptRow(ParsedImportRow row) {
    CustomerImportReceiptRow receiptRow = new CustomerImportReceiptRow();
    BeanUtil.copyProperties(row.getRow(), receiptRow);
    receiptRow.setErrorMessage(row.joinErrorMessage());
    return receiptRow;
}
```

### 回执文件生成

回执文件使用 EasyExcel 生成。只要 `failCount > 0`，就生成错误回执；如果全部成功，则不生成。

下面将错误回执生成逻辑独立为服务，便于后续替换成本地存储、MinIO、OSS 等不同实现。

文件位置：`src/main/java/io/github/atengk/excel/service/CustomerImportReceiptService.java`

```java
package io.github.atengk.excel.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import io.github.atengk.excel.dto.CustomerImportReceiptRow;
import io.github.atengk.excel.model.ParsedImportRow;
import io.github.atengk.excel.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户导入错误回执服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImportReceiptService {

    private final FileStorageService fileStorageService;

    /**
     * 根据错误行生成回执文件
     *
     * @param taskNo 任务编号
     * @param rows   错误行
     * @return 回执文件路径
     */
    public String generateReceipt(String taskNo, List<ParsedImportRow> rows) {
        List<CustomerImportReceiptRow> receiptRows = rows.stream()
                .filter(row -> !row.isValid())
                .map(this::buildReceiptRow)
                .toList();

        if (CollUtil.isEmpty(receiptRows)) {
            log.info("客户导入无失败数据，不生成错误回执，taskNo：{}", taskNo);
            return null;
        }

        String receiptPath = fileStorageService.saveReceiptFile(taskNo, receiptRows);
        log.info("客户导入错误回执生成成功，taskNo：{}，失败行数：{}，路径：{}", taskNo, receiptRows.size(), receiptPath);
        return receiptPath;
    }

    /**
     * 构建错误回执行
     *
     * @param row 解析后的行数据
     * @return 错误回执行
     */
    private CustomerImportReceiptRow buildReceiptRow(ParsedImportRow row) {
        CustomerImportReceiptRow receiptRow = new CustomerImportReceiptRow();
        BeanUtil.copyProperties(row.getRow(), receiptRow);
        receiptRow.setErrorMessage(row.joinErrorMessage());
        return receiptRow;
    }
}
```

如果继续沿用前面 `BatchImportResult` 中收集的 `receiptRows`，也可以直接调用文件存储服务保存：

```java
if (result.getFailCount() > 0) {
    receiptFilePath = fileStorageService.saveReceiptFile(task.getTaskNo(), result.getReceiptRows());
}
```

这两种方式都可以。推荐前面这种“批处理时收集回执行”的方式，性能更好，不需要再次扫描明细表。

### 回执文件上传或保存

为了让本地存储和 MinIO 存储可以平滑切换，先抽象一个文件存储接口。

文件位置：`src/main/java/io/github/atengk/excel/storage/FileStorageService.java`

```java
package io.github.atengk.excel.storage;

import io.github.atengk.excel.dto.CustomerImportReceiptRow;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * Excel 导入文件存储接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface FileStorageService {

    /**
     * 保存原始 Excel 文件
     *
     * @param file   上传文件
     * @param taskNo 任务编号
     * @return 文件路径
     */
    String saveOriginalFile(MultipartFile file, String taskNo);

    /**
     * 保存错误回执文件
     *
     * @param taskNo 任务编号
     * @param rows   错误回执数据
     * @return 文件路径
     */
    String saveReceiptFile(String taskNo, List<CustomerImportReceiptRow> rows);

    /**
     * 获取本地文件
     *
     * @param filePath 文件路径
     * @return 文件对象
     */
    File getFile(String filePath);
}
```

本地文件存储实现如下。生产环境如果使用 MinIO，只需要新增一个 `MinioFileStorageService` 实现同一个接口。

文件位置：`src/main/java/io/github/atengk/excel/storage/LocalFileStorageService.java`

```java
package io.github.atengk.excel.storage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import io.github.atengk.excel.config.ExcelImportProperties;
import io.github.atengk.excel.dto.CustomerImportReceiptRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 本地 Excel 导入文件存储服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final ExcelImportProperties properties;

    /**
     * 保存原始 Excel 文件
     *
     * @param file   上传文件
     * @param taskNo 任务编号
     * @return 文件相对路径
     */
    @Override
    public String saveOriginalFile(MultipartFile file, String taskNo) {
        try {
            String datePath = DateUtil.format(new Date(), "yyyyMMdd");
            String extName = FileUtil.extName(file.getOriginalFilename());
            String fileName = taskNo + "_" + IdUtil.fastSimpleUUID() + "." + extName;

            String relativePath = FileUtil.normalize(
                    properties.getStorage().getOriginalDir() + "/" + datePath + "/" + fileName
            );

            File targetFile = FileUtil.file(properties.getStorage().getLocalRootPath(), relativePath);
            FileUtil.mkParentDirs(targetFile);

            file.transferTo(targetFile);
            log.info("原始 Excel 文件保存成功，路径：{}", targetFile.getAbsolutePath());
            return relativePath;
        } catch (Exception e) {
            log.error("原始 Excel 文件保存失败", e);
            throw new IllegalStateException("原始 Excel 文件保存失败");
        }
    }

    /**
     * 保存错误回执文件
     *
     * @param taskNo 任务编号
     * @param rows   错误回执数据
     * @return 文件相对路径
     */
    @Override
    public String saveReceiptFile(String taskNo, List<CustomerImportReceiptRow> rows) {
        String datePath = DateUtil.format(new Date(), "yyyyMMdd");
        String fileName = taskNo + "_receipt.xlsx";

        String relativePath = FileUtil.normalize(
                properties.getStorage().getReceiptDir() + "/" + datePath + "/" + fileName
        );

        File targetFile = FileUtil.file(properties.getStorage().getLocalRootPath(), relativePath);
        FileUtil.mkParentDirs(targetFile);

        EasyExcel.write(targetFile, CustomerImportReceiptRow.class)
                .sheet("错误回执")
                .doWrite(rows);

        log.info("错误回执文件保存成功，路径：{}", targetFile.getAbsolutePath());
        return relativePath;
    }

    /**
     * 获取本地文件
     *
     * @param filePath 文件相对路径
     * @return 本地文件
     */
    @Override
    public File getFile(String filePath) {
        return FileUtil.file(properties.getStorage().getLocalRootPath(), filePath);
    }
}
```

如果要接入 MinIO，保持接口不变，保存策略变成：

```text
EasyExcel 先写入临时文件
-> MinIO 上传临时文件
-> 返回 objectName
-> 删除本地临时文件
-> import_task.receipt_file_path 保存 objectName
```

这种方式不会影响业务校验、任务状态更新和前端下载接口。

### 回执下载

回执下载接口根据 `taskId` 查询导入任务，判断是否存在 `receiptFilePath`，然后读取文件并写入响应流。

文件位置：`src/main/java/io/github/atengk/excel/service/impl/CustomerImportServiceImpl.java`

```java
package io.github.atengk.excel.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import io.github.atengk.excel.entity.ImportTask;
import io.github.atengk.excel.service.ImportTaskService;
import io.github.atengk.excel.storage.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 客户导入回执下载服务片段
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerImportReceiptDownloadService {

    private final ImportTaskService importTaskService;

    private final FileStorageService fileStorageService;

    /**
     * 下载错误回执
     *
     * @param taskId   导入任务 ID
     * @param response HTTP 响应
     */
    public void downloadReceipt(Long taskId, HttpServletResponse response) {
        ImportTask task = importTaskService.getById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("导入任务不存在");
        }

        if (StrUtil.isBlank(task.getReceiptFilePath())) {
            throw new IllegalArgumentException("当前任务没有错误回执");
        }

        File receiptFile = fileStorageService.getFile(task.getReceiptFilePath());
        if (!FileUtil.exist(receiptFile)) {
            throw new IllegalArgumentException("错误回执文件不存在");
        }

        String fileName = "客户导入错误回执_" + task.getTaskNo() + ".xlsx";
        String encodeFileName = URLUtil.encode(fileName, StandardCharsets.UTF_8);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeFileName);

        try (InputStream inputStream = FileUtil.getInputStream(receiptFile)) {
            IoUtil.copy(inputStream, response.getOutputStream());
            log.info("错误回执下载成功，taskId：{}，文件路径：{}", taskId, task.getReceiptFilePath());
        } catch (Exception e) {
            log.error("错误回执下载失败，taskId：{}", taskId, e);
            throw new IllegalStateException("错误回执下载失败");
        }
    }
}
```

Controller 中调用方式如下：

```java
@GetMapping("/receipt/{taskId}")
public void downloadReceipt(@PathVariable Long taskId, HttpServletResponse response) {
    customerImportService.downloadReceipt(taskId, response);
}
```

前端或测试时可以这样下载：

```bash
curl -X GET "http://localhost:8080/customer/import/receipt/100001" \
  -o customer_import_receipt.xlsx
```

这一部分完成后，导入模块的校验和错误回执链路已经完整：

```text
字段校验
-> 字典校验
-> Excel 内部重复校验
-> 数据库重复校验
-> 失败行记录错误原因
-> 生成错误回执 Excel
-> 保存回执文件路径
-> 通过接口下载回执
```

## 异常与幂等处理

Excel 导入的异常处理不能只靠 `try-catch`。该场景本身涉及异步任务、批量入库、部分成功部分失败、错误回执和进度查询，原 README 中也明确提到重复数据处理、部分成功部分失败、异步任务、进度查询和错误行回写是核心难点。

本案例采用三层幂等策略：

```text
上传幂等：同一个文件短时间重复上传，返回已有任务
任务幂等：同一个 taskId 不重复执行已完成任务
数据幂等：客户手机号唯一索引兜底，防止重复入库
```

### 重复上传处理

重复上传最常见的场景是：用户点了两次上传按钮，或者前端超时后重试上传。处理方式是计算 Excel 文件 MD5，在一定时间窗口内发现相同文件时，直接返回已有任务 ID。

先给 `import_task` 表增加文件摘要字段。

```sql
ALTER TABLE import_task
    ADD COLUMN file_md5 VARCHAR(64) DEFAULT NULL COMMENT '上传文件MD5' AFTER original_file_path,
    ADD KEY idx_biz_md5_created_time (biz_type, file_md5, created_time);
```

文件 MD5 工具类如下。这里使用 Hutool 的 `SecureUtil` 计算文件摘要。

文件位置：`src/main/java/io/github/atengk/excel/util/ExcelFileDigestUtil.java`

```java
package io.github.atengk.excel.util;

import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel 文件摘要工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ExcelFileDigestUtil {

    private ExcelFileDigestUtil() {
    }

    /**
     * 计算上传文件 MD5
     *
     * @param file 上传文件
     * @return MD5 摘要
     */
    public static String md5(MultipartFile file) {
        try {
            String md5 = SecureUtil.md5(file.getInputStream());
            log.info("Excel 文件 MD5 计算完成，文件名：{}，md5：{}", file.getOriginalFilename(), md5);
            return md5;
        } catch (Exception e) {
            log.error("Excel 文件 MD5 计算失败，文件名：{}", file.getOriginalFilename(), e);
            throw new IllegalStateException("Excel 文件摘要计算失败");
        }
    }
}
```

`ImportTask` 实体增加字段：

```java
private String fileMd5;
```

上传逻辑中，在保存文件前先计算 MD5 并查询是否存在同一天的相同任务。

文件位置：`src/main/java/io/github/atengk/excel/service/impl/CustomerImportServiceImpl.java`

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Long upload(MultipartFile file) {
    checkExcelFile(file);

    String fileMd5 = ExcelFileDigestUtil.md5(file);
    ImportTask existsTask = findSameFileTask(fileMd5);
    if (existsTask != null) {
        log.info("检测到重复上传 Excel，直接返回已有任务，taskId：{}，fileMd5：{}", existsTask.getId(), fileMd5);
        return existsTask.getId();
    }

    Long taskId = IdUtil.getSnowflakeNextId();
    String taskNo = "IMP" + DateUtil.format(DateUtil.date(), "yyyyMMdd") + IdUtil.getSnowflakeNextIdStr();

    String originalFilePath = fileStorageService.saveOriginalFile(file, taskNo);

    ImportTask task = new ImportTask();
    task.setId(taskId);
    task.setTaskNo(taskNo);
    task.setBizType(BIZ_TYPE);
    task.setOriginalFileName(file.getOriginalFilename());
    task.setOriginalFilePath(originalFilePath);
    task.setFileMd5(fileMd5);
    task.setStatus(ImportTaskStatusEnum.PENDING.name());
    task.setTotalCount(0);
    task.setSuccessCount(0);
    task.setFailCount(0);

    importTaskService.save(task);

    rabbitTemplate.convertAndSend(
            properties.getMq().getExchange(),
            properties.getMq().getRoutingKey(),
            new CustomerImportMessage(taskId)
    );

    log.info("客户导入任务创建成功，taskId：{}，taskNo：{}，fileMd5：{}", taskId, taskNo, fileMd5);
    return taskId;
}

/**
 * 查询当天是否存在相同文件导入任务
 *
 * @param fileMd5 文件 MD5
 * @return 已存在任务
 */
private ImportTask findSameFileTask(String fileMd5) {
    LocalDateTime beginTime = LocalDateTime.now().toLocalDate().atStartOfDay();

    return importTaskService.getOne(
            Wrappers.lambdaQuery(ImportTask.class)
                    .eq(ImportTask::getBizType, BIZ_TYPE)
                    .eq(ImportTask::getFileMd5, fileMd5)
                    .ge(ImportTask::getCreatedTime, beginTime)
                    .in(ImportTask::getStatus,
                            ImportTaskStatusEnum.PENDING.name(),
                            ImportTaskStatusEnum.PROCESSING.name(),
                            ImportTaskStatusEnum.SUCCESS.name(),
                            ImportTaskStatusEnum.PART_SUCCESS.name())
                    .last("LIMIT 1")
    );
}
```

注意：这里没有给 `file_md5` 加唯一索引，因为同一个文件在不同日期、不同业务或用户主动重新导入时可能是合理操作。是否强制唯一，要看业务规则。

### 消费失败重试

消费失败要区分两类异常：

| 异常类型                                        | 推荐处理                                         |
| ----------------------------------------------- | ------------------------------------------------ |
| MQ 临时异常、数据库短暂不可用、文件系统短暂异常 | 可以有限重试                                     |
| 已经发生部分入库后的系统异常                    | 不建议无限自动重试，应标记失败并人工确认或走补偿 |

先给任务表增加重试字段。

```sql
ALTER TABLE import_task
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '当前重试次数' AFTER error_message,
    ADD COLUMN max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数' AFTER retry_count;
```

`ImportTask` 实体增加字段：

```java
private Integer retryCount;

private Integer maxRetryCount;
```

消费者中做有限重试。失败后先判断是否允许重试，允许则 `basicNack` 重新入队；超过最大次数则确认消息并保留任务失败状态。

文件位置：`src/main/java/io/github/atengk/excel/mq/CustomerImportConsumer.java`

```java
package io.github.atengk.excel.mq;

import com.rabbitmq.client.Channel;
import io.github.atengk.excel.entity.ImportTask;
import io.github.atengk.excel.service.CustomerImportService;
import io.github.atengk.excel.service.ImportTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 客户导入 MQ 消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerImportConsumer {

    private final CustomerImportService customerImportService;

    private final ImportTaskService importTaskService;

    /**
     * 消费客户导入任务
     *
     * @param importMessage 导入消息
     * @param message       原始消息
     * @param channel       RabbitMQ 通道
     */
    @RabbitListener(queues = "${excel-import.mq.queue}")
    public void consume(CustomerImportMessage importMessage, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Long taskId = importMessage.getTaskId();

        try {
            log.info("开始消费客户导入任务，taskId：{}", taskId);
            customerImportService.executeImport(taskId);
            channel.basicAck(deliveryTag, false);
            log.info("客户导入任务消费成功，taskId：{}", taskId);
        } catch (Exception e) {
            log.error("客户导入任务消费异常，taskId：{}", taskId, e);
            handleConsumeFailure(taskId, deliveryTag, channel);
        }
    }

    /**
     * 处理消费失败
     *
     * @param taskId      任务 ID
     * @param deliveryTag 消息投递标识
     * @param channel     RabbitMQ 通道
     */
    private void handleConsumeFailure(Long taskId, long deliveryTag, Channel channel) {
        try {
            ImportTask task = importTaskService.getById(taskId);
            if (task == null) {
                log.warn("客户导入任务不存在，直接确认消息，taskId：{}", taskId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Integer retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
            Integer maxRetryCount = task.getMaxRetryCount() == null ? 3 : task.getMaxRetryCount();

            if (retryCount < maxRetryCount) {
                ImportTask update = new ImportTask();
                update.setId(taskId);
                update.setRetryCount(retryCount + 1);
                importTaskService.updateById(update);

                log.warn("客户导入任务准备重试，taskId：{}，当前重试次数：{}", taskId, retryCount + 1);
                channel.basicNack(deliveryTag, false, true);
                return;
            }

            log.error("客户导入任务超过最大重试次数，确认消息并结束重试，taskId：{}", taskId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ackException) {
            log.error("客户导入失败消息处理异常，taskId：{}", taskId, ackException);
        }
    }
}
```

这个版本适合演示核心思路。生产环境更推荐使用“重试队列 + TTL + 死信队列”，避免 `basicNack(..., true)` 立即重新入队导致短时间内连续失败。

推荐生产级流程如下：

```text
导入队列消费失败
-> 投递到重试队列
-> TTL 延迟 1 分钟
-> 重新路由回导入队列
-> 超过最大次数
-> 投递到死信队列
-> 后台人工处理或定时补偿
```

### 部分成功部分失败

Excel 导入通常不能因为一行错误就让整个文件失败。更实用的策略是：合法数据正常入库，非法数据进入错误回执。

本案例中的结果判断规则如下：

| 成功数 | 失败数 | 最终状态                             |
| ------ | ------ | ------------------------------------ |
| `> 0`  | `0`    | `SUCCESS`                            |
| `> 0`  | `> 0`  | `PART_SUCCESS`                       |
| `0`    | `> 0`  | `FAILED`                             |
| `0`    | `0`    | `FAILED`，通常表示空文件或无有效数据 |

核心代码如下：

```java
private void updateFinished(Long taskId, BatchImportResult result, String receiptFilePath) {
    ImportTask update = new ImportTask();
    update.setId(taskId);
    update.setTotalCount(result.getTotalCount());
    update.setSuccessCount(result.getSuccessCount());
    update.setFailCount(result.getFailCount());
    update.setReceiptFilePath(receiptFilePath);
    update.setFinishTime(LocalDateTime.now());

    if (result.getTotalCount() == 0) {
        update.setStatus(ImportTaskStatusEnum.FAILED.name());
        update.setErrorMessage("Excel 无有效数据");
    } else if (result.getFailCount() == 0) {
        update.setStatus(ImportTaskStatusEnum.SUCCESS.name());
    } else if (result.getSuccessCount() == 0) {
        update.setStatus(ImportTaskStatusEnum.FAILED.name());
    } else {
        update.setStatus(ImportTaskStatusEnum.PART_SUCCESS.name());
    }

    importTaskService.updateById(update);
    log.info("客户导入任务完成，taskId：{}，总数：{}，成功：{}，失败：{}",
            taskId, result.getTotalCount(), result.getSuccessCount(), result.getFailCount());
}
```

如果业务要求“只要有一行错误，整批都不入库”，可以改成先全量校验，再统一入库。但这种方式对大文件不友好，也不符合大多数后台导入场景。

### 任务状态流转

任务状态要尽量简单，避免出现太多中间状态。建议使用下面这套状态机：

```text
PENDING
  |
  v
PROCESSING
  |
  +----> SUCCESS
  |
  +----> PART_SUCCESS
  |
  +----> FAILED
```

状态含义如下：

| 状态           | 含义               | 是否终态 |
| -------------- | ------------------ | -------- |
| `PENDING`      | 上传成功，等待消费 | 否       |
| `PROCESSING`   | 正在解析和入库     | 否       |
| `SUCCESS`      | 全部导入成功       | 是       |
| `PART_SUCCESS` | 部分成功，部分失败 | 是       |
| `FAILED`       | 全部失败或系统异常 | 是       |

为了防止已完成任务被重复消费，`executeImport` 开始时要校验任务状态。

```java
@Override
public void executeImport(Long taskId) {
    ImportTask task = getTaskOrThrow(taskId);

    if (isFinishedStatus(task.getStatus())) {
        log.info("导入任务已结束，跳过重复消费，taskId：{}，status：{}", taskId, task.getStatus());
        return;
    }

    try {
        updateProcessing(taskId);

        File originalFile = fileStorageService.getFile(task.getOriginalFilePath());
        if (!FileUtil.exist(originalFile)) {
            throw new IllegalArgumentException("原始 Excel 文件不存在");
        }

        CustomerImportReadListener listener = new CustomerImportReadListener(taskId, validator, batchService);

        EasyExcel.read(originalFile, CustomerImportExcelRow.class, listener)
                .sheet()
                .doRead();

        BatchImportResult result = listener.getResult();
        String receiptFilePath = null;

        if (result.getFailCount() > 0) {
            receiptFilePath = fileStorageService.saveReceiptFile(task.getTaskNo(), result.getReceiptRows());
        }

        updateFinished(taskId, result, receiptFilePath);
    } catch (Exception e) {
        log.error("客户导入任务执行失败，taskId：{}", taskId, e);
        updateFailed(taskId, e);
        throw e;
    }
}

/**
 * 判断是否为终态
 *
 * @param status 任务状态
 * @return true 表示终态
 */
private boolean isFinishedStatus(String status) {
    return ImportTaskStatusEnum.SUCCESS.name().equals(status)
            || ImportTaskStatusEnum.PART_SUCCESS.name().equals(status)
            || ImportTaskStatusEnum.FAILED.name().equals(status);
}
```

为了避免并发消费者同时处理同一个任务，建议将 `PROCESSING` 更新改成带状态条件的更新。

```java
/**
 * 尝试将任务更新为处理中
 *
 * @param taskId 任务 ID
 * @return true 表示抢占任务成功
 */
private boolean tryUpdateProcessing(Long taskId) {
    boolean updated = importTaskService.update(
            Wrappers.lambdaUpdate(ImportTask.class)
                    .eq(ImportTask::getId, taskId)
                    .eq(ImportTask::getStatus, ImportTaskStatusEnum.PENDING.name())
                    .set(ImportTask::getStatus, ImportTaskStatusEnum.PROCESSING.name())
                    .set(ImportTask::getStartTime, LocalDateTime.now())
    );

    if (!updated) {
        log.info("导入任务状态已变化，跳过处理，taskId：{}", taskId);
    }

    return updated;
}
```

然后在 `executeImport` 中使用：

```java
if (!tryUpdateProcessing(taskId)) {
    return;
}
```

这样即使 MQ 重复投递，同一个 `taskId` 也只会被一个消费者成功处理。

## 功能验证

功能验证建议使用真实 Excel 文件测试，而不是只用接口联调。至少覆盖正常导入、格式错误、重复数据和大批量数据四类场景。

### 正常导入测试

准备一个 `customer_import_success.xlsx`，内容如下：

| 客户名称 | 手机号      | 邮箱                                                | 客户等级 | 所属城市 | 备注     |
| -------- | ----------- | --------------------------------------------------- | -------- | -------- | -------- |
| 张三客户 | 13800138000 | [zhangsan@example.com](mailto:zhangsan@example.com) | NORMAL   | 杭州     | 正常客户 |
| 李四客户 | 13900139000 | [lisi@example.com](mailto:lisi@example.com)         | VIP      | 上海     | VIP客户  |

上传文件：

```bash
curl -X POST "http://localhost:8080/customer/import/upload" \
  -F "file=@customer_import_success.xlsx"
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 100001
}
```

查询进度：

```bash
curl -X GET "http://localhost:8080/customer/import/progress/100001"
```

预期结果：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": 100001,
    "status": "SUCCESS",
    "statusName": "全部成功",
    "totalCount": 2,
    "successCount": 2,
    "failCount": 0,
    "hasReceipt": false
  }
}
```

数据库验证：

```sql
SELECT customer_name, mobile, email, customer_level, city
FROM customer_info
WHERE import_task_id = 100001;

SELECT status, total_count, success_count, fail_count, receipt_file_path
FROM import_task
WHERE id = 100001;
```

预期结果是客户表新增 2 条数据，任务状态为 `SUCCESS`，没有错误回执。

### 格式错误测试

准备一个 `customer_import_format_error.xlsx`，内容如下：

| 客户名称 | 手机号      | 邮箱                                              | 客户等级 | 所属城市 | 备注         |
| -------- | ----------- | ------------------------------------------------- | -------- | -------- | ------------ |
| A        | 13800138000 | [a@example.com](mailto:a@example.com)             | NORMAL   | 杭州     | 客户名称过短 |
| 李四客户 | 123         | [lisi@example.com](mailto:lisi@example.com)       | VIP      | 上海     | 手机号错误   |
| 王五客户 | 13700137000 | error-email                                       | NORMAL   | 广州     | 邮箱错误     |
| 赵六客户 | 13600136000 | [zhaoliu@example.com](mailto:zhaoliu@example.com) | GOLD     | 深圳     | 字典错误     |

上传：

```bash
curl -X POST "http://localhost:8080/customer/import/upload" \
  -F "file=@customer_import_format_error.xlsx"
```

查询进度后，预期状态为 `FAILED` 或 `PART_SUCCESS`，取决于是否存在成功行。以上示例全部错误，预期为：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "FAILED",
    "statusName": "失败",
    "totalCount": 4,
    "successCount": 0,
    "failCount": 4,
    "hasReceipt": true
  }
}
```

下载错误回执：

```bash
curl -X GET "http://localhost:8080/customer/import/receipt/100002" \
  -o customer_import_format_error_receipt.xlsx
```

回执中应包含类似错误原因：

| 客户名称 | 手机号      | 邮箱                                              | 客户等级 | 错误原因                       |
| -------- | ----------- | ------------------------------------------------- | -------- | ------------------------------ |
| A        | 13800138000 | [a@example.com](mailto:a@example.com)             | NORMAL   | 客户名称长度必须在 2-50 之间   |
| 李四客户 | 123         | [lisi@example.com](mailto:lisi@example.com)       | VIP      | 手机号格式不正确               |
| 王五客户 | 13700137000 | error-email                                       | NORMAL   | 邮箱格式不正确                 |
| 赵六客户 | 13600136000 | [zhaoliu@example.com](mailto:zhaoliu@example.com) | GOLD     | 客户等级只能填写 NORMAL 或 VIP |

### 重复数据测试

重复数据需要验证两类：Excel 内部重复和数据库已有数据重复。

先插入一条数据库已有客户：

```sql
INSERT INTO customer_info (
    id,
    customer_name,
    mobile,
    email,
    customer_level,
    city,
    remark,
    created_time,
    updated_time,
    deleted
) VALUES (
    900001,
    '已存在客户',
    '13800138000',
    'exists@example.com',
    'NORMAL',
    '杭州',
    '数据库已有数据',
    NOW(),
    NOW(),
    0
);
```

准备 `customer_import_duplicate.xlsx`：

| 客户名称 | 手机号      | 邮箱                                                | 客户等级 | 所属城市 | 备注           |
| -------- | ----------- | --------------------------------------------------- | -------- | -------- | -------------- |
| 张三客户 | 13800138000 | [zhangsan@example.com](mailto:zhangsan@example.com) | NORMAL   | 杭州     | 数据库已存在   |
| 李四客户 | 13900139000 | [lisi@example.com](mailto:lisi@example.com)         | VIP      | 上海     | 第一次出现     |
| 李四重复 | 13900139000 | [lisi2@example.com](mailto:lisi2@example.com)       | VIP      | 上海     | Excel 内部重复 |
| 王五客户 | 13700137000 | [wangwu@example.com](mailto:wangwu@example.com)     | NORMAL   | 广州     | 正常数据       |

上传：

```bash
curl -X POST "http://localhost:8080/customer/import/upload" \
  -F "file=@customer_import_duplicate.xlsx"
```

预期结果：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "PART_SUCCESS",
    "statusName": "部分成功",
    "totalCount": 4,
    "successCount": 2,
    "failCount": 2,
    "hasReceipt": true
  }
}
```

预期入库数据：

```sql
SELECT customer_name, mobile
FROM customer_info
WHERE import_task_id = 100003;
```

应该只看到：

| customer_name | mobile      |
| ------------- | ----------- |
| 李四客户      | 13900139000 |
| 王五客户      | 13700137000 |

错误回执应包含：

| 手机号      | 错误原因              |
| ----------- | --------------------- |
| 13800138000 | 手机号已存在          |
| 13900139000 | 手机号在 Excel 中重复 |

### 大批量数据测试

大批量测试用于验证 EasyExcel 分批读取、批量入库和错误回执生成是否稳定。建议先测 1 万行，再测 5 万行。

可以使用下面的 SQL 生成一批已存在手机号，用于模拟部分重复：

```sql
INSERT INTO customer_info (
    id,
    customer_name,
    mobile,
    email,
    customer_level,
    city,
    remark,
    created_time,
    updated_time,
    deleted
)
SELECT
    910000 + seq AS id,
    CONCAT('存量客户', seq) AS customer_name,
    CONCAT('138', LPAD(seq, 8, '0')) AS mobile,
    CONCAT('exists', seq, '@example.com') AS email,
    'NORMAL' AS customer_level,
    '杭州' AS city,
    '大批量测试存量数据' AS remark,
    NOW(),
    NOW(),
    0
FROM (
    SELECT 1 seq UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) t;
```

如果需要快速生成测试 Excel，可以写一个简单测试类生成 10000 行数据。

文件位置：`src/test/java/io/github/atengk/excel/CustomerImportExcelMockTest.java`

```java
package io.github.atengk.excel;

import com.alibaba.excel.EasyExcel;
import io.github.atengk.excel.dto.CustomerImportExcelRow;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户导入测试 Excel 生成器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class CustomerImportExcelMockTest {

    /**
     * 生成 10000 行客户导入测试数据
     */
    @Test
    void generateLargeExcel() {
        List<CustomerImportExcelRow> rows = new ArrayList<>();

        for (int i = 1; i <= 10000; i++) {
            CustomerImportExcelRow row = new CustomerImportExcelRow();
            row.setCustomerName("批量客户" + i);
            row.setMobile("139" + String.format("%08d", i));
            row.setEmail("batch" + i + "@example.com");
            row.setCustomerLevel(i % 5 == 0 ? "VIP" : "NORMAL");
            row.setCity("杭州");
            row.setRemark("大批量导入测试");
            rows.add(row);
        }

        EasyExcel.write("customer_import_large.xlsx", CustomerImportExcelRow.class)
                .sheet("客户导入")
                .doWrite(rows);
    }
}
```

上传大文件：

```bash
curl -X POST "http://localhost:8080/customer/import/upload" \
  -F "file=@customer_import_large.xlsx"
```

观察任务状态：

```bash
curl -X GET "http://localhost:8080/customer/import/progress/100004"
```

数据库验证：

```sql
SELECT status, total_count, success_count, fail_count, error_message
FROM import_task
WHERE id = 100004;

SELECT COUNT(*)
FROM customer_info
WHERE import_task_id = 100004;

SELECT COUNT(*)
FROM import_task_detail
WHERE task_id = 100004;
```

预期结果：

```text
import_task.total_count = 10000
customer_info 当前任务入库数量 = success_count
import_task_detail 当前任务明细数量 = total_count
如果没有错误数据，则状态为 SUCCESS
如果有重复或格式错误，则状态为 PART_SUCCESS 或 FAILED
```

大批量测试重点关注以下指标：

| 指标     | 建议观察点                                   |
| -------- | -------------------------------------------- |
| 内存占用 | EasyExcel 是否按批处理，是否出现内存持续上涨 |
| SQL 次数 | 是否按批查重、批量入库，避免逐行查库         |
| 任务状态 | 是否能从 `PROCESSING` 正确流转到终态         |
| 错误回执 | 大量失败行时是否能正常生成和下载             |
| 接口响应 | 上传接口是否快速返回，不等待导入完成         |

到这里，Excel 批量导入的异常处理、幂等控制和主要验证用例已经完整。实际项目中还可以继续增强死信队列、XXL-JOB 补偿扫描、导入任务人工重试、导入进度 WebSocket 推送等能力。

## 本案例完整代码结构

本案例代码按 Spring Boot 常见分层组织，核心链路覆盖上传 Excel、创建导入任务、RabbitMQ 异步消费、EasyExcel 解析、校验、批量入库、错误回执生成和进度查询，和 README 中第 14 个 Excel 导入场景的功能要求一致。

完整目录结构如下：

```text
src/main/java/io/github/atengk/excel
├── common
│   └── Result.java
├── config
│   ├── ExcelImportProperties.java
│   └── ExcelImportRabbitConfig.java
├── controller
│   └── CustomerImportController.java
├── dto
│   ├── CustomerImportExcelRow.java
│   └── CustomerImportReceiptRow.java
├── entity
│   ├── CustomerInfo.java
│   ├── ImportTask.java
│   └── ImportTaskDetail.java
├── enums
│   ├── CustomerLevelEnum.java
│   ├── ImportRowStatusEnum.java
│   └── ImportTaskStatusEnum.java
├── listener
│   └── CustomerImportReadListener.java
├── mapper
│   ├── CustomerInfoMapper.java
│   ├── ImportTaskDetailMapper.java
│   └── ImportTaskMapper.java
├── model
│   ├── BatchImportResult.java
│   └── ParsedImportRow.java
├── mq
│   ├── CustomerImportConsumer.java
│   └── CustomerImportMessage.java
├── service
│   ├── CustomerImportBatchService.java
│   ├── CustomerImportReceiptService.java
│   ├── CustomerImportService.java
│   ├── CustomerInfoService.java
│   ├── ImportTaskDetailService.java
│   ├── ImportTaskService.java
│   └── impl
│       ├── CustomerImportServiceImpl.java
│       ├── CustomerInfoServiceImpl.java
│       ├── ImportTaskDetailServiceImpl.java
│       └── ImportTaskServiceImpl.java
├── storage
│   ├── FileStorageService.java
│   └── LocalFileStorageService.java
├── util
│   └── ExcelFileDigestUtil.java
├── validator
│   ├── CustomerImportBusinessValidator.java
│   └── CustomerImportValidator.java
└── vo
    └── ImportTaskProgressVO.java

src/main/resources
├── application.yml
└── mapper
    └── .gitkeep

src/test/java/io/github/atengk/excel
└── CustomerImportExcelMockTest.java
```

### Controller

Controller 层只负责接收 HTTP 请求、返回接口结果，不处理 Excel 解析和业务校验细节。

```text
controller
└── CustomerImportController.java
```

| 文件                            | 作用                                                     |
| ------------------------------- | -------------------------------------------------------- |
| `CustomerImportController.java` | 提供上传 Excel、查询导入进度、下载错误回执、下载模板接口 |

接口清单：

```text
POST /customer/import/upload
GET  /customer/import/progress/{taskId}
GET  /customer/import/receipt/{taskId}
GET  /customer/import/template
```

### Service

Service 层负责串联核心业务流程，是本案例最主要的实现位置。

```text
service
├── CustomerImportService.java
├── CustomerImportBatchService.java
├── CustomerImportReceiptService.java
├── CustomerInfoService.java
├── ImportTaskDetailService.java
├── ImportTaskService.java
└── impl
    ├── CustomerImportServiceImpl.java
    ├── CustomerInfoServiceImpl.java
    ├── ImportTaskDetailServiceImpl.java
    └── ImportTaskServiceImpl.java
```

| 文件                                | 作用                                                      |
| ----------------------------------- | --------------------------------------------------------- |
| `CustomerImportService.java`        | 客户导入主服务接口                                        |
| `CustomerImportServiceImpl.java`    | 上传文件、创建任务、投递 MQ、执行导入、查询进度、下载回执 |
| `CustomerImportBatchService.java`   | 批量校验、批量入库、导入明细记录、错误回执数据构建        |
| `CustomerImportReceiptService.java` | 错误回执生成服务，可选增强类                              |
| `CustomerInfoService.java`          | 客户资料基础 Service                                      |
| `CustomerInfoServiceImpl.java`      | 客户资料基础 Service 实现                                 |
| `ImportTaskService.java`            | 导入任务基础 Service                                      |
| `ImportTaskServiceImpl.java`        | 导入任务基础 Service 实现                                 |
| `ImportTaskDetailService.java`      | 导入明细基础 Service                                      |
| `ImportTaskDetailServiceImpl.java`  | 导入明细基础 Service 实现                                 |

主流程集中在 `CustomerImportServiceImpl`：

```text
upload()
-> checkExcelFile()
-> ExcelFileDigestUtil.md5()
-> findSameFileTask()
-> fileStorageService.saveOriginalFile()
-> importTaskService.save()
-> rabbitTemplate.convertAndSend()

executeImport()
-> tryUpdateProcessing()
-> EasyExcel.read()
-> CustomerImportReadListener
-> fileStorageService.saveReceiptFile()
-> updateFinished()
```

### Listener

Listener 层负责 EasyExcel 的逐行读取和批次刷新。

```text
listener
└── CustomerImportReadListener.java
```

| 文件                              | 作用                                                         |
| --------------------------------- | ------------------------------------------------------------ |
| `CustomerImportReadListener.java` | 逐行读取 Excel，过滤空行，执行基础校验，处理 Excel 内部重复，按批次提交数据 |

监听器内部关键成员：

```text
BATCH_SIZE = 500
cachedRows：当前批次缓存
excelMobileSet：整个 Excel 文件内的手机号去重集合
result：累计导入结果
```

处理方式：

```text
invoke()
-> 空行过滤
-> 计算 Excel 行号
-> 字段基础校验
-> Excel 内部重复校验
-> 写入 cachedRows
-> 达到批次大小后 flush()

doAfterAllAnalysed()
-> 处理剩余 cachedRows
-> 输出最终统计
```

### Mapper

Mapper 层使用 MyBatis-Plus 的 `BaseMapper` 即可，不需要手写 XML。

```text
mapper
├── CustomerInfoMapper.java
├── ImportTaskDetailMapper.java
└── ImportTaskMapper.java
```

| 文件                          | 对应表               | 作用          |
| ----------------------------- | -------------------- | ------------- |
| `CustomerInfoMapper.java`     | `customer_info`      | 客户资料 CRUD |
| `ImportTaskMapper.java`       | `import_task`        | 导入任务 CRUD |
| `ImportTaskDetailMapper.java` | `import_task_detail` | 导入明细 CRUD |

如果后续要做导入任务分页查询、按业务类型统计导入结果，可以在 Mapper 中补充自定义 SQL。

### Entity

Entity 层对应数据库三张核心表。

```text
entity
├── CustomerInfo.java
├── ImportTask.java
└── ImportTaskDetail.java
```

| 文件                    | 对应表               | 作用                                           |
| ----------------------- | -------------------- | ---------------------------------------------- |
| `CustomerInfo.java`     | `customer_info`      | 保存成功导入的客户资料                         |
| `ImportTask.java`       | `import_task`        | 保存导入任务状态、文件路径、统计数量、错误信息 |
| `ImportTaskDetail.java` | `import_task_detail` | 保存每一行导入结果、原始行数据和错误原因       |

核心字段关系：

```text
import_task.id
    |
    +-- customer_info.import_task_id
    |
    +-- import_task_detail.task_id
```

这样可以通过一个 `taskId` 追踪一次导入产生的业务数据和明细数据。

### DTO

DTO 层主要用于 Excel 行数据映射。

```text
dto
├── CustomerImportExcelRow.java
└── CustomerImportReceiptRow.java
```

| 文件                            | 作用                                             |
| ------------------------------- | ------------------------------------------------ |
| `CustomerImportExcelRow.java`   | EasyExcel 读取用户上传 Excel，也用于生成导入模板 |
| `CustomerImportReceiptRow.java` | 生成错误回执，比导入行多一个“错误原因”字段       |

字段映射关系：

| Excel 表头 | Java 字段                      |
| ---------- | ------------------------------ |
| 客户名称   | `customerName`                 |
| 手机号     | `mobile`                       |
| 邮箱       | `email`                        |
| 客户等级   | `customerLevel`                |
| 所属城市   | `city`                         |
| 备注       | `remark`                       |
| 错误原因   | `errorMessage`，仅错误回执使用 |

### VO

VO 层用于接口返回，避免直接暴露数据库实体。

```text
vo
└── ImportTaskProgressVO.java
```

| 文件                        | 作用                     |
| --------------------------- | ------------------------ |
| `ImportTaskProgressVO.java` | 查询导入进度接口返回对象 |

返回字段建议保持简洁：

```text
taskId
taskNo
status
statusName
totalCount
successCount
failCount
hasReceipt
errorMessage
```

前端根据 `status` 判断是否继续轮询，根据 `hasReceipt` 判断是否展示“下载错误回执”按钮。

### MQ Consumer

MQ 层用于异步导入，避免上传接口长时间阻塞。

```text
mq
├── CustomerImportConsumer.java
└── CustomerImportMessage.java
```

| 文件                          | 作用                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| `CustomerImportMessage.java`  | RabbitMQ 消息体，只携带 `taskId`                             |
| `CustomerImportConsumer.java` | 消费导入任务，调用 `customerImportService.executeImport(taskId)` |

消息流转：

```text
上传接口创建任务
-> 发送 CustomerImportMessage(taskId)
-> RabbitMQ 队列
-> CustomerImportConsumer 消费
-> executeImport(taskId)
-> 更新任务状态
```

消费者建议开启手动 ACK：

```text
处理成功：basicAck
可重试失败：basicNack 并重新入队
超过重试次数：basicAck，任务保留 FAILED 状态
```

### Excel 工具类

工具类和辅助类用于降低主业务代码复杂度。

```text
util
└── ExcelFileDigestUtil.java

storage
├── FileStorageService.java
└── LocalFileStorageService.java

validator
├── CustomerImportValidator.java
└── CustomerImportBusinessValidator.java

model
├── BatchImportResult.java
└── ParsedImportRow.java

enums
├── CustomerLevelEnum.java
├── ImportRowStatusEnum.java
└── ImportTaskStatusEnum.java

config
├── ExcelImportProperties.java
└── ExcelImportRabbitConfig.java

common
└── Result.java
```

| 文件                                   | 类型     | 作用                                   |
| -------------------------------------- | -------- | -------------------------------------- |
| `ExcelFileDigestUtil.java`             | 工具类   | 计算上传文件 MD5，用于重复上传判断     |
| `FileStorageService.java`              | 存储接口 | 抽象原始文件、错误回执文件的保存与读取 |
| `LocalFileStorageService.java`         | 存储实现 | 本地文件保存，后续可替换为 MinIO       |
| `CustomerImportValidator.java`         | 校验器   | 必填、格式、字典等单行基础校验         |
| `CustomerImportBusinessValidator.java` | 校验器   | 数据库重复等业务校验                   |
| `ParsedImportRow.java`                 | 中间模型 | 保存 Excel 行号、原始行对象、错误信息  |
| `BatchImportResult.java`               | 中间模型 | 保存批处理统计结果和错误回执数据       |
| `CustomerLevelEnum.java`               | 枚举     | 客户等级字典                           |
| `ImportTaskStatusEnum.java`            | 枚举     | 导入任务状态                           |
| `ImportRowStatusEnum.java`             | 枚举     | 导入明细状态                           |
| `ExcelImportProperties.java`           | 配置类   | 映射 `excel-import` 配置项             |
| `ExcelImportRabbitConfig.java`         | 配置类   | 声明 RabbitMQ 交换机、队列、绑定关系   |
| `Result.java`                          | 通用对象 | 统一接口响应格式                       |

本案例最终形成的调用链如下：

```text
CustomerImportController.upload()
-> CustomerImportServiceImpl.upload()
-> ExcelFileDigestUtil.md5()
-> LocalFileStorageService.saveOriginalFile()
-> ImportTaskService.save()
-> RabbitTemplate.convertAndSend()
-> CustomerImportConsumer.consume()
-> CustomerImportServiceImpl.executeImport()
-> CustomerImportReadListener.invoke()
-> CustomerImportValidator.validateBasic()
-> CustomerImportBatchService.processBatch()
-> CustomerImportBusinessValidator.validateDatabaseDuplicate()
-> CustomerInfoService.saveBatch()
-> ImportTaskDetailService.saveBatch()
-> LocalFileStorageService.saveReceiptFile()
-> ImportTaskService.updateById()
```

到这里，README 中“Excel 批量导入、校验、错误回执”的文档主体已经完整，后续可以按这个结构直接整理成一个独立项目案例。