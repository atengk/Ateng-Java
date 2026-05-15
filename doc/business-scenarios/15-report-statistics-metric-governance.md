# 报表统计与数据口径治理

本案例基于「报表统计与数据口径治理」业务场景展开，核心目标是实现一个可落地的后端报表模块，覆盖运营后台中常见的订单统计、指标口径管理、热点报表缓存、明细下钻和 Excel 导出能力。该场景在原 README 中被定义为适用于运营后台、财务报表、订单统计、用户增长、BI 看板和管理驾驶舱等系统。

## 场景说明

本案例以「订单运营报表」为业务背景，模拟后台管理系统中常见的经营数据统计需求。系统需要按照时间范围、部门、渠道、订单状态等维度统计订单数量、支付金额、退款金额、客单价等核心指标，同时支持查看指标口径说明、查询统计明细、缓存热点报表结果，并提供 Excel 导出能力。

这里不实现完整 BI 系统，而是聚焦 Java 后端项目中最常见、最能体现实战能力的核心链路：

```text
业务订单数据
-> 报表查询条件
-> 统一指标口径
-> SQL 聚合统计
-> Redis 缓存热点结果
-> 返回报表汇总
-> 支持明细下钻
-> 支持 Excel 导出
```

本案例重点解决以下问题：

```text
统计口径如何统一维护
报表 SQL 如何组织
多条件统计如何实现
热点报表如何缓存
统计结果如何支持下钻
大数据导出如何处理
后续如何扩展 ClickHouse / Elasticsearch
```

### 业务目标

业务目标是建设一个轻量但完整的报表统计模块，帮助运营、财务、管理人员快速查看订单经营情况，并保证不同页面、不同人员看到的指标口径一致。

本案例主要实现以下目标：

| 目标         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 统一指标口径 | 将支付金额、退款金额、净收入、客单价等指标的计算规则统一维护 |
| 多维度统计   | 支持按日期、部门、渠道、订单状态进行聚合统计                 |
| 热点报表缓存 | 对高频查询的报表结果使用 Redis 缓存，降低数据库压力          |
| 明细下钻     | 从汇总数据跳转查看对应的订单明细                             |
| Excel 导出   | 将报表汇总结果导出为 Excel 文件                              |
| 可扩展存储   | 默认使用 MySQL 实现，后续可平滑扩展到 ClickHouse             |

本案例不做复杂的前端可视化大屏，只提供后端接口和核心实现。前端可以基于接口返回结果自行接入 ECharts、AntV、DataV 等图表组件。

### 核心功能

本模块围绕「报表查询、口径治理、缓存、下钻、导出」五个核心能力设计。

| 功能         | 实现内容                                                   |
| ------------ | ---------------------------------------------------------- |
| 报表汇总查询 | 根据查询条件统计订单数、支付金额、退款金额、净收入、客单价 |
| 指标口径查询 | 查询每个指标的名称、编码、计算规则、业务说明               |
| 报表缓存     | 使用 Redis 缓存热点查询结果，避免重复执行复杂 SQL          |
| 明细下钻     | 根据报表查询条件查询对应订单明细列表                       |
| Excel 导出   | 使用 EasyExcel 导出报表汇总数据                            |
| 定时快照     | 使用 XXL-JOB 定时生成昨日统计快照，便于后续查询加速        |

核心业务流程如下：

```text
用户进入运营报表页面
-> 选择时间范围、部门、渠道、订单状态
-> 后端生成查询条件签名
-> 优先查询 Redis 缓存
-> 缓存未命中则执行 MySQL 聚合 SQL
-> 返回汇总统计结果
-> 用户点击某个指标或日期
-> 查询对应订单明细
-> 用户点击导出
-> 生成 Excel 报表文件
```

本案例会实现以下接口：

| 接口                               | 说明               |
| ---------------------------------- | ------------------ |
| `POST /report/order/summary`       | 查询订单报表汇总   |
| `GET /report/metrics`              | 查询指标口径列表   |
| `POST /report/order/detail`        | 查询订单明细下钻   |
| `POST /report/order/export`        | 导出订单报表 Excel |
| `POST /report/order/cache/refresh` | 手动刷新报表缓存   |

### 技术选型

本案例默认使用 Spring Boot 3 + MyBatis-Plus + MySQL + Redis + EasyExcel 实现核心功能。该组合更适合中小型后台系统快速落地，后续数据量变大后，可以将统计查询迁移到 ClickHouse 或 Elasticsearch。

| 技术          | 用途                                      |
| ------------- | ----------------------------------------- |
| Spring Boot 3 | 后端基础框架                              |
| MyBatis-Plus  | 实体映射、分页查询、基础 CRUD             |
| MySQL         | 存储订单明细、指标口径、统计快照          |
| Redis         | 缓存热点报表查询结果                      |
| EasyExcel     | 导出报表 Excel                            |
| XXL-JOB       | 定时生成统计快照、缓存预热                |
| Hutool        | 日期处理、集合处理、JSON 序列化、摘要生成 |
| Lombok        | 简化实体、DTO、VO 代码                    |
| Sa-Token      | 预留登录用户与数据权限上下文              |
| ClickHouse    | 可选，用于后期承接大数据量统计分析        |
| Elasticsearch | 可选，用于后期支持复杂明细检索            |

推荐本案例的初始落地架构如下：

```text
Spring Boot 应用
├── Controller：提供报表查询、导出、口径查询接口
├── Service：处理统计逻辑、缓存逻辑、导出逻辑
├── Mapper：执行 MySQL 聚合 SQL 和明细查询
├── Redis：缓存热点报表结果
├── MySQL：存储业务明细、指标口径、统计快照
└── XXL-JOB：定时生成快照和预热缓存
```

数据库初期直接查询 MySQL 即可，适合数据量在百万级以内的后台系统。若订单明细达到千万级以上，建议引入以下优化路径：

```text
第一阶段：MySQL 聚合查询 + 索引优化 + Redis 缓存
第二阶段：MySQL 统计快照表 + 定时任务预聚合
第三阶段：Canal 同步订单数据到 ClickHouse
第四阶段：ClickHouse 承接复杂聚合统计，MySQL 只保留业务交易数据
```

本案例后续代码会先采用「MySQL + Redis + EasyExcel」实现核心功能，保证可以直接集成到普通 Spring Boot 后端项目中。

## 业务流程设计

本模块按照 README 中「采集业务数据、按时间 / 部门 / 渠道 / 状态聚合、生成统计结果、缓存热点报表、支持明细下钻、支持 Excel 导出」这一主链路设计。

### 报表查询流程

报表查询是本案例的核心入口。前端传入时间范围、部门、渠道、订单状态等条件后，后端先生成查询签名，再尝试读取 Redis 缓存。缓存不存在时，执行 MySQL 聚合 SQL，并将结果写入 Redis。

```text
用户请求订单报表
-> 校验时间范围
-> 组装查询条件
-> 生成缓存 Key
-> 查询 Redis 缓存
-> 缓存命中直接返回
-> 缓存未命中查询 MySQL
-> 聚合订单数、支付金额、退款金额、净收入、客单价
-> 写入 Redis 缓存
-> 返回报表结果
```

查询维度设计如下：

| 维度     | 字段                    | 说明                           |
| -------- | ----------------------- | ------------------------------ |
| 时间     | `stat_date`             | 按天统计                       |
| 部门     | `dept_id`               | 支持组织维度统计               |
| 渠道     | `channel_code`          | APP、小程序、PC、第三方渠道    |
| 状态     | `order_status`          | 待支付、已支付、已退款、已关闭 |
| 租户     | `tenant_id`             | 预留 SaaS 多租户隔离           |
| 数据权限 | `dept_id` / `tenant_id` | 预留数据权限过滤条件           |

核心聚合指标如下：

| 指标       | 字段               | 计算方式                          |
| ---------- | ------------------ | --------------------------------- |
| 订单数     | `order_count`      | `COUNT(*)`                        |
| 支付订单数 | `paid_order_count` | `SUM(order_status = 'PAID')`      |
| 支付金额   | `pay_amount`       | `SUM(pay_amount)`                 |
| 退款金额   | `refund_amount`    | `SUM(refund_amount)`              |
| 净收入     | `net_amount`       | `SUM(pay_amount - refund_amount)` |
| 客单价     | `avg_order_amount` | `SUM(pay_amount) / 支付订单数`    |

### 统计结果缓存流程

报表统计属于高频读、低频写的典型场景。同一组筛选条件在运营后台中可能被多人反复查询，因此适合使用 Redis 缓存查询结果。

```text
报表查询条件
-> 参数标准化
-> 使用 JSON 序列化查询条件
-> 使用 MD5 生成短签名
-> 拼接 Redis Key
-> 查询缓存
-> 未命中则查询数据库
-> 写入缓存并设置过期时间
```

缓存 Key 建议格式：

```text
report:order:summary:{tenantId}:{queryHash}
```

示例：

```text
report:order:summary:10001:7f86a1d9b3e4c8a2
```

缓存策略建议如下：

| 场景      | 缓存时间     | 说明                               |
| --------- | ------------ | ---------------------------------- |
| 今日数据  | 1 ~ 5 分钟   | 今日订单还在变化，缓存时间不宜过长 |
| 历史 7 天 | 10 ~ 30 分钟 | 查询频率高，数据基本稳定           |
| 历史月份  | 1 ~ 6 小时   | 历史数据变化较少                   |
| 快照数据  | 6 ~ 24 小时  | 适合定时预聚合后的结果             |

缓存失效方式：

```text
订单支付成功
-> 删除今日相关报表缓存

订单退款成功
-> 删除今日相关报表缓存

运营手动刷新
-> 删除指定条件缓存

定时任务预热
-> 重新生成热点报表缓存
```

实际项目中不建议精确删除所有组合条件缓存，因为组合维度过多。更实用的方式是：

```text
短 TTL 自动过期
+ 今日热点报表主动删除
+ 历史报表定时预热
```

### 明细下钻流程

明细下钻用于从汇总数据跳转到具体订单列表。例如运营人员看到某天某渠道退款金额异常，可以点击该行查看对应订单明细。

```text
用户点击报表行
-> 携带当前统计维度
-> 后端复用报表查询条件
-> 增加分页参数
-> 查询订单明细表
-> 返回订单列表
```

下钻查询和汇总查询必须使用同一套筛选条件，避免出现「汇总数量是 100，点进去只有 92 条」的问题。

下钻接口建议参数：

| 参数          | 说明     |
| ------------- | -------- |
| `startDate`   | 开始日期 |
| `endDate`     | 结束日期 |
| `deptId`      | 部门 ID  |
| `channelCode` | 渠道编码 |
| `orderStatus` | 订单状态 |
| `pageNum`     | 页码     |
| `pageSize`    | 每页数量 |

下钻返回字段建议控制在运营人员真正需要看的范围内：

| 字段           | 说明     |
| -------------- | -------- |
| `orderNo`      | 订单号   |
| `userId`       | 用户 ID  |
| `deptName`     | 所属部门 |
| `channelName`  | 渠道名称 |
| `orderStatus`  | 订单状态 |
| `payAmount`    | 支付金额 |
| `refundAmount` | 退款金额 |
| `payTime`      | 支付时间 |
| `createdTime`  | 创建时间 |

### Excel 导出流程

Excel 导出用于将当前报表查询结果导出给运营、财务或管理层。中小数据量可以同步导出，大数据量建议异步任务化。

本案例先实现同步导出，适合几千到几万行的报表汇总数据。

```text
用户点击导出
-> 前端传入当前查询条件
-> 后端校验导出范围
-> 查询报表聚合结果
-> 转换为 Excel 行对象
-> 使用 EasyExcel 写出
-> 浏览器下载文件
```

导出限制建议：

| 限制项       | 建议值                         | 说明                 |
| ------------ | ------------------------------ | -------------------- |
| 最大时间范围 | 31 天                          | 防止一次导出过大     |
| 最大导出行数 | 5 万行                         | 超过后建议走异步导出 |
| 文件名       | `订单报表_yyyyMMddHHmmss.xlsx` | 便于识别             |
| 金额格式     | 保留 2 位小数                  | 避免精度混乱         |

后续可扩展为异步导出：

```text
创建导出任务
-> 返回任务 ID
-> 后台异步生成 Excel
-> 上传 MinIO
-> 前端轮询任务状态
-> 下载文件
```

## 数据模型设计

本案例设计三张核心表：

```text
order_biz_detail                订单业务明细表
report_metric_define            报表指标口径表
report_order_summary_snapshot   订单统计快照表
```

### 订单业务明细表

订单业务明细表用于存储报表统计所需的订单宽表数据。实际项目中可以从订单主表、支付表、退款表、用户表、部门表中同步生成，也可以通过 Canal 同步到报表库。

本案例为了方便落地，直接使用一张宽表承接报表查询。

| 字段            | 说明     |
| --------------- | -------- |
| `order_no`      | 订单号   |
| `tenant_id`     | 租户 ID  |
| `dept_id`       | 部门 ID  |
| `channel_code`  | 渠道编码 |
| `order_status`  | 订单状态 |
| `pay_amount`    | 支付金额 |
| `refund_amount` | 退款金额 |
| `stat_date`     | 统计日期 |
| `pay_time`      | 支付时间 |

核心索引设计：

| 索引                                   | 说明                 |
| -------------------------------------- | -------------------- |
| `(tenant_id, stat_date)`               | 支持按租户和日期过滤 |
| `(tenant_id, dept_id, stat_date)`      | 支持部门维度统计     |
| `(tenant_id, channel_code, stat_date)` | 支持渠道维度统计     |
| `(tenant_id, order_status, stat_date)` | 支持状态维度统计     |
| `order_no` 唯一索引                    | 保证订单明细不重复   |

### 报表指标口径表

指标口径表用于统一维护每个报表指标的计算规则和业务解释，避免不同开发、不同页面、不同 SQL 对同一个指标使用不同算法。

示例：

| 指标编码           | 指标名称   | 统计口径                           |
| ------------------ | ---------- | ---------------------------------- |
| `ORDER_COUNT`      | 订单数     | 统计订单明细表中的订单总数         |
| `PAID_ORDER_COUNT` | 支付订单数 | 订单状态为已支付或已退款的订单数量 |
| `PAY_AMOUNT`       | 支付金额   | 已支付订单的支付金额汇总           |
| `REFUND_AMOUNT`    | 退款金额   | 已退款订单的退款金额汇总           |
| `NET_AMOUNT`       | 净收入     | 支付金额 - 退款金额                |
| `AVG_ORDER_AMOUNT` | 客单价     | 支付金额 / 支付订单数              |

该表不一定参与每次 SQL 统计，但它非常适合作为：

```text
报表页面指标说明
接口字段说明
统计口径审计
跨团队口径统一
```

### 报表统计快照表

统计快照表用于存储定时任务预聚合后的结果。对于历史数据，不需要每次都扫描订单明细表，可以直接查询快照表。

快照粒度建议：

```text
日期 + 租户 + 部门 + 渠道
```

例如：

```text
2026-05-15 + 租户 10001 + 部门 20001 + APP 渠道
```

快照表适合以下场景：

```text
历史日报
月度报表
管理驾驶舱首页卡片
高频固定维度统计
```

不适合过度追求所有组合维度都预聚合，否则快照表会膨胀。建议只保留最常用的统计维度。

## 项目结构

### 后端目录结构

本案例采用标准 Spring Boot 分层结构，包路径使用 `io.github.atengk.report`。

```text
src/main/java/io/github/atengk/report
├── ReportApplication.java
├── controller
│   ├── OrderReportController.java
│   └── ReportMetricController.java
├── service
│   ├── OrderReportService.java
│   ├── ReportMetricService.java
│   └── ReportCacheService.java
├── service/impl
│   ├── OrderReportServiceImpl.java
│   ├── ReportMetricServiceImpl.java
│   └── ReportCacheServiceImpl.java
├── mapper
│   ├── OrderBizDetailMapper.java
│   ├── ReportMetricDefineMapper.java
│   └── ReportOrderSummarySnapshotMapper.java
├── entity
│   ├── OrderBizDetail.java
│   ├── ReportMetricDefine.java
│   └── ReportOrderSummarySnapshot.java
├── dto
│   ├── OrderReportQueryDTO.java
│   └── OrderDetailQueryDTO.java
├── vo
│   ├── OrderReportSummaryVO.java
│   ├── OrderReportDetailVO.java
│   └── ReportMetricVO.java
├── excel
│   └── OrderReportExportExcel.java
├── job
│   └── OrderReportSnapshotJob.java
└── common
    ├── Result.java
    └── PageResult.java

src/main/resources
├── application.yml
└── mapper
    ├── OrderBizDetailMapper.xml
    ├── ReportMetricDefineMapper.xml
    └── ReportOrderSummarySnapshotMapper.xml
```

### 核心类职责

| 类                                 | 职责                                           |
| ---------------------------------- | ---------------------------------------------- |
| `OrderReportController`            | 提供订单报表汇总、明细下钻、导出、刷新缓存接口 |
| `ReportMetricController`           | 提供指标口径查询接口                           |
| `OrderReportService`               | 处理报表统计主流程                             |
| `ReportCacheService`               | 处理 Redis 缓存 Key、读取、写入、删除          |
| `ReportMetricService`              | 查询指标口径定义                               |
| `OrderBizDetailMapper`             | 执行订单明细聚合 SQL 和下钻查询                |
| `ReportMetricDefineMapper`         | 查询指标定义表                                 |
| `ReportOrderSummarySnapshotMapper` | 写入和查询统计快照                             |
| `OrderReportQueryDTO`              | 报表汇总查询参数                               |
| `OrderDetailQueryDTO`              | 明细下钻查询参数                               |
| `OrderReportSummaryVO`             | 报表汇总返回对象                               |
| `OrderReportDetailVO`              | 明细下钻返回对象                               |
| `OrderReportExportExcel`           | Excel 导出行对象                               |
| `OrderReportSnapshotJob`           | 定时生成昨日统计快照                           |

## 数据库脚本

本案例使用 MySQL 8。金额字段统一使用 `DECIMAL(18,2)`，避免使用 `DOUBLE` 或 `FLOAT` 造成金额精度问题。

### 业务明细表 DDL

下面脚本用于创建订单业务明细宽表，后续报表汇总和明细下钻都基于该表完成。

```sql
-- 订单业务明细表：用于报表统计和明细下钻
DROP TABLE IF EXISTS order_biz_detail;

CREATE TABLE order_biz_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID，单租户系统可固定为 0',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    dept_id BIGINT NOT NULL DEFAULT 0 COMMENT '部门 ID，用于组织维度统计和数据权限过滤',
    dept_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '部门名称，报表展示使用',
    channel_code VARCHAR(32) NOT NULL COMMENT '渠道编码：APP、WECHAT、PC、THIRD',
    channel_name VARCHAR(64) NOT NULL COMMENT '渠道名称',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态：WAIT_PAY、PAID、REFUNDED、CLOSED',
    pay_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    refund_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
    stat_date DATE NOT NULL COMMENT '统计日期，通常取订单支付日期或创建日期',
    pay_time DATETIME NULL COMMENT '支付时间',
    refund_time DATETIME NULL COMMENT '退款时间',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_tenant_stat_date (tenant_id, stat_date),
    KEY idx_tenant_dept_date (tenant_id, dept_id, stat_date),
    KEY idx_tenant_channel_date (tenant_id, channel_code, stat_date),
    KEY idx_tenant_status_date (tenant_id, order_status, stat_date),
    KEY idx_created_time (created_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单业务明细表';
```

### 指标口径表 DDL

下面脚本用于创建指标口径表，主要用于统一维护报表字段含义、计算规则和展示状态。

```sql
-- 报表指标口径表：统一维护指标编码、名称、计算规则和业务说明
DROP TABLE IF EXISTS report_metric_define;

CREATE TABLE report_metric_define (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    metric_code VARCHAR(64) NOT NULL COMMENT '指标编码',
    metric_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    metric_group VARCHAR(64) NOT NULL DEFAULT 'ORDER' COMMENT '指标分组：ORDER、USER、FINANCE',
    calc_rule VARCHAR(500) NOT NULL COMMENT '计算规则说明',
    data_scope VARCHAR(500) NOT NULL DEFAULT '' COMMENT '数据范围说明',
    display_order INT NOT NULL DEFAULT 0 COMMENT '展示排序',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0 禁用，1 启用',
    remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_metric_code (metric_code),
    KEY idx_metric_group (metric_group),
    KEY idx_enabled_order (enabled, display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '报表指标口径表';
```

### 统计快照表 DDL

下面脚本用于创建订单统计快照表，适合由 XXL-JOB 每天定时生成昨日统计结果。

```sql
-- 订单统计快照表：保存按日期、租户、部门、渠道预聚合后的报表结果
DROP TABLE IF EXISTS report_order_summary_snapshot;

CREATE TABLE report_order_summary_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    dept_id BIGINT NOT NULL DEFAULT 0 COMMENT '部门 ID',
    dept_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '部门名称',
    channel_code VARCHAR(32) NOT NULL COMMENT '渠道编码',
    channel_name VARCHAR(64) NOT NULL COMMENT '渠道名称',
    order_count BIGINT NOT NULL DEFAULT 0 COMMENT '订单数',
    paid_order_count BIGINT NOT NULL DEFAULT 0 COMMENT '支付订单数',
    refund_order_count BIGINT NOT NULL DEFAULT 0 COMMENT '退款订单数',
    pay_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    refund_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
    net_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '净收入：支付金额 - 退款金额',
    avg_order_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '客单价：支付金额 / 支付订单数',
    snapshot_time DATETIME NOT NULL COMMENT '快照生成时间',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_snapshot_dimension (stat_date, tenant_id, dept_id, channel_code),
    KEY idx_tenant_date (tenant_id, stat_date),
    KEY idx_dept_date (dept_id, stat_date),
    KEY idx_channel_date (channel_code, stat_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单统计快照表';
```

### 初始化数据

下面脚本用于初始化指标口径和部分订单测试数据，方便后续接口联调和 SQL 验证。

```sql
-- 初始化报表指标口径
INSERT INTO report_metric_define (
    metric_code,
    metric_name,
    metric_group,
    calc_rule,
    data_scope,
    display_order,
    enabled,
    remark
) VALUES
(
    'ORDER_COUNT',
    '订单数',
    'ORDER',
    '统计订单业务明细表中符合筛选条件的订单总数，包含待支付、已支付、已退款、已关闭订单',
    'order_biz_detail.deleted = 0',
    1,
    1,
    '用于观察整体订单规模'
),
(
    'PAID_ORDER_COUNT',
    '支付订单数',
    'ORDER',
    '统计订单状态为 PAID、REFUNDED 的订单数量',
    'order_status IN (''PAID'', ''REFUNDED'') AND deleted = 0',
    2,
    1,
    '已退款订单曾经支付成功，因此计入支付订单数'
),
(
    'REFUND_ORDER_COUNT',
    '退款订单数',
    'ORDER',
    '统计订单状态为 REFUNDED 的订单数量',
    'order_status = ''REFUNDED'' AND deleted = 0',
    3,
    1,
    '用于观察退款规模'
),
(
    'PAY_AMOUNT',
    '支付金额',
    'FINANCE',
    '汇总符合筛选条件订单的 pay_amount 字段',
    'order_status IN (''PAID'', ''REFUNDED'') AND deleted = 0',
    4,
    1,
    '金额字段统一使用 DECIMAL(18,2)'
),
(
    'REFUND_AMOUNT',
    '退款金额',
    'FINANCE',
    '汇总符合筛选条件订单的 refund_amount 字段',
    'order_status = ''REFUNDED'' AND deleted = 0',
    5,
    1,
    '退款金额只统计已退款订单'
),
(
    'NET_AMOUNT',
    '净收入',
    'FINANCE',
    '净收入 = 支付金额 - 退款金额',
    'pay_amount - refund_amount',
    6,
    1,
    '财务分析常用指标'
),
(
    'AVG_ORDER_AMOUNT',
    '客单价',
    'FINANCE',
    '客单价 = 支付金额 / 支付订单数；支付订单数为 0 时返回 0',
    'SUM(pay_amount) / COUNT(paid_order)',
    7,
    1,
    '用于衡量单个支付订单平均贡献金额'
);

-- 初始化订单业务明细数据
INSERT INTO order_biz_detail (
    order_no,
    tenant_id,
    user_id,
    dept_id,
    dept_name,
    channel_code,
    channel_name,
    order_status,
    pay_amount,
    refund_amount,
    stat_date,
    pay_time,
    refund_time,
    created_time
) VALUES
(
    'ORD202605150001',
    10001,
    90001,
    20001,
    '华东运营部',
    'APP',
    '移动 APP',
    'PAID',
    199.00,
    0.00,
    '2026-05-15',
    '2026-05-15 10:12:30',
    NULL,
    '2026-05-15 10:10:00'
),
(
    'ORD202605150002',
    10001,
    90002,
    20001,
    '华东运营部',
    'WECHAT',
    '微信小程序',
    'REFUNDED',
    299.00,
    299.00,
    '2026-05-15',
    '2026-05-15 11:20:10',
    '2026-05-15 15:30:00',
    '2026-05-15 11:18:00'
),
(
    'ORD202605150003',
    10001,
    90003,
    20002,
    '华南运营部',
    'PC',
    'PC 官网',
    'PAID',
    88.00,
    0.00,
    '2026-05-15',
    '2026-05-15 14:05:22',
    NULL,
    '2026-05-15 14:00:00'
),
(
    'ORD202605150004',
    10001,
    90004,
    20002,
    '华南运营部',
    'APP',
    '移动 APP',
    'WAIT_PAY',
    0.00,
    0.00,
    '2026-05-15',
    NULL,
    NULL,
    '2026-05-15 16:30:00'
),
(
    'ORD202605140001',
    10001,
    90005,
    20001,
    '华东运营部',
    'THIRD',
    '第三方渠道',
    'PAID',
    520.00,
    0.00,
    '2026-05-14',
    '2026-05-14 09:45:00',
    NULL,
    '2026-05-14 09:40:00'
);

-- 初始化一条统计快照数据，便于验证快照查询
INSERT INTO report_order_summary_snapshot (
    stat_date,
    tenant_id,
    dept_id,
    dept_name,
    channel_code,
    channel_name,
    order_count,
    paid_order_count,
    refund_order_count,
    pay_amount,
    refund_amount,
    net_amount,
    avg_order_amount,
    snapshot_time
) VALUES
(
    '2026-05-14',
    10001,
    20001,
    '华东运营部',
    'THIRD',
    '第三方渠道',
    1,
    1,
    0,
    520.00,
    0.00,
    520.00,
    520.00,
    NOW()
);
```

## 核心代码实现

本节实现报表统计模块的核心后端代码，围绕「统计口径统一、复杂 SQL 聚合、缓存热点报表、明细下钻、Excel 导出」展开，这些也是原 README 中该场景的核心难点。

以下代码默认依赖：

```text
Spring Boot 3
MyBatis-Plus
MySQL
Redis
EasyExcel
Hutool
Lombok
```

### 报表查询 DTO

报表查询 DTO 用于承接前端传入的筛选条件，包括时间范围、租户、部门、渠道、订单状态和是否启用缓存。

文件位置：`src/main/java/io/github/atengk/report/dto/OrderReportQueryDTO.java`

```java
package io.github.atengk.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 订单报表查询参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderReportQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 租户 ID，单租户系统可固定为 0
     */
    private Long tenantId = 0L;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 渠道编码：APP、WECHAT、PC、THIRD
     */
    private String channelCode;

    /**
     * 订单状态：WAIT_PAY、PAID、REFUNDED、CLOSED
     */
    private String orderStatus;

    /**
     * 是否启用缓存
     */
    private Boolean useCache = Boolean.TRUE;
}
```

明细下钻 DTO 复用报表筛选条件，并增加分页参数。

文件位置：`src/main/java/io/github/atengk/report/dto/OrderDetailQueryDTO.java`

```java
package io.github.atengk.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单报表明细下钻查询参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailQueryDTO extends OrderReportQueryDTO {

    /**
     * 页码
     */
    @Min(value = 1, message = "页码不能小于 1")
    private Long pageNum = 1L;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 500, message = "每页数量不能超过 500")
    private Long pageSize = 20L;
}
```

### 报表结果 VO

报表汇总 VO 用于返回按日期、部门、渠道聚合后的统计结果。

文件位置：`src/main/java/io/github/atengk/report/vo/OrderReportSummaryVO.java`

```java
package io.github.atengk.report.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单报表汇总返回结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderReportSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate statDate;

    private Long tenantId;

    private Long deptId;

    private String deptName;

    private String channelCode;

    private String channelName;

    private Long orderCount;

    private Long paidOrderCount;

    private Long refundOrderCount;

    private BigDecimal payAmount;

    private BigDecimal refundAmount;

    private BigDecimal netAmount;

    private BigDecimal avgOrderAmount;
}
```

明细下钻 VO 用于返回具体订单列表。

文件位置：`src/main/java/io/github/atengk/report/vo/OrderReportDetailVO.java`

```java
package io.github.atengk.report.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单报表明细返回结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderReportDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long tenantId;

    private Long userId;

    private Long deptId;

    private String deptName;

    private String channelCode;

    private String channelName;

    private String orderStatus;

    private BigDecimal payAmount;

    private BigDecimal refundAmount;

    private LocalDate statDate;

    private LocalDateTime payTime;

    private LocalDateTime refundTime;

    private LocalDateTime createdTime;
}
```

指标口径 VO 用于给前端展示指标名称、编码、计算规则和数据范围。

文件位置：`src/main/java/io/github/atengk/report/vo/ReportMetricVO.java`

```java
package io.github.atengk.report.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 报表指标口径返回结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ReportMetricVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String metricCode;

    private String metricName;

    private String metricGroup;

    private String calcRule;

    private String dataScope;

    private Integer displayOrder;

    private String remark;
}
```

### 指标口径实体

指标口径实体对应 `report_metric_define` 表，用于统一维护报表指标的业务含义和计算规则。

文件位置：`src/main/java/io/github/atengk/report/entity/ReportMetricDefine.java`

```java
package io.github.atengk.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表指标口径定义实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("report_metric_define")
public class ReportMetricDefine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String metricCode;

    private String metricName;

    private String metricGroup;

    private String calcRule;

    private String dataScope;

    private Integer displayOrder;

    private Integer enabled;

    private String remark;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
```

订单业务明细实体用于承接 `order_biz_detail` 表，后续明细下钻和聚合统计都基于该表。

文件位置：`src/main/java/io/github/atengk/report/entity/OrderBizDetail.java`

```java
package io.github.atengk.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单业务明细实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("order_biz_detail")
public class OrderBizDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long tenantId;

    private Long userId;

    private Long deptId;

    private String deptName;

    private String channelCode;

    private String channelName;

    private String orderStatus;

    private BigDecimal payAmount;

    private BigDecimal refundAmount;

    private LocalDate statDate;

    private LocalDateTime payTime;

    private LocalDateTime refundTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
```

### 统计快照实体

统计快照实体对应 `report_order_summary_snapshot` 表，用于保存定时任务预聚合后的统计结果。

文件位置：`src/main/java/io/github/atengk/report/entity/ReportOrderSummarySnapshot.java`

```java
package io.github.atengk.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单报表统计快照实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("report_order_summary_snapshot")
public class ReportOrderSummarySnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    private Long tenantId;

    private Long deptId;

    private String deptName;

    private String channelCode;

    private String channelName;

    private Long orderCount;

    private Long paidOrderCount;

    private Long refundOrderCount;

    private BigDecimal payAmount;

    private BigDecimal refundAmount;

    private BigDecimal netAmount;

    private BigDecimal avgOrderAmount;

    private LocalDateTime snapshotTime;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
```

### Mapper 接口

订单明细 Mapper 负责执行报表汇总 SQL 和明细下钻分页 SQL。

文件位置：`src/main/java/io/github/atengk/report/mapper/OrderBizDetailMapper.java`

```java
package io.github.atengk.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.report.dto.OrderDetailQueryDTO;
import io.github.atengk.report.dto.OrderReportQueryDTO;
import io.github.atengk.report.entity.OrderBizDetail;
import io.github.atengk.report.vo.OrderReportDetailVO;
import io.github.atengk.report.vo.OrderReportSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单业务明细 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface OrderBizDetailMapper extends BaseMapper<OrderBizDetail> {

    /**
     * 查询订单报表汇总数据
     *
     * @param query 查询参数
     * @return 汇总结果
     */
    List<OrderReportSummaryVO> selectOrderSummary(@Param("query") OrderReportQueryDTO query);

    /**
     * 分页查询订单明细下钻数据
     *
     * @param page  分页参数
     * @param query 查询参数
     * @return 明细分页结果
     */
    IPage<OrderReportDetailVO> selectOrderDetailPage(Page<OrderReportDetailVO> page,
                                                     @Param("query") OrderDetailQueryDTO query);
}
```

指标口径 Mapper 直接继承 MyBatis-Plus 的 `BaseMapper`。

文件位置：`src/main/java/io/github/atengk/report/mapper/ReportMetricDefineMapper.java`

```java
package io.github.atengk.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.report.entity.ReportMetricDefine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报表指标口径 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ReportMetricDefineMapper extends BaseMapper<ReportMetricDefine> {
}
```

统计快照 Mapper 用于后续定时任务写入快照数据。

文件位置：`src/main/java/io/github/atengk/report/mapper/ReportOrderSummarySnapshotMapper.java`

```java
package io.github.atengk.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.report.entity.ReportOrderSummarySnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单报表统计快照 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ReportOrderSummarySnapshotMapper extends BaseMapper<ReportOrderSummarySnapshot> {
}
```

### Mapper XML 统计 SQL

统计 SQL 集中放在 XML 中，避免把复杂聚合逻辑写到 Java 代码里。这里按日期、部门、渠道聚合统计订单数、支付订单数、退款订单数、支付金额、退款金额、净收入和客单价。

文件位置：`src/main/resources/mapper/OrderBizDetailMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.report.mapper.OrderBizDetailMapper">

    <!-- 订单报表公共筛选条件，汇总查询和明细下钻必须复用同一套条件，避免汇总与明细不一致 -->
    <sql id="OrderReportWhere">
        deleted = 0
        <if test="query.tenantId != null">
            AND tenant_id = #{query.tenantId}
        </if>
        <if test="query.startDate != null">
            AND stat_date <![CDATA[ >= ]]> #{query.startDate}
        </if>
        <if test="query.endDate != null">
            AND stat_date <![CDATA[ <= ]]> #{query.endDate}
        </if>
        <if test="query.deptId != null">
            AND dept_id = #{query.deptId}
        </if>
        <if test="query.channelCode != null and query.channelCode != ''">
            AND channel_code = #{query.channelCode}
        </if>
        <if test="query.orderStatus != null and query.orderStatus != ''">
            AND order_status = #{query.orderStatus}
        </if>
    </sql>

    <!-- 订单报表汇总：按日期、部门、渠道聚合核心经营指标 -->
    <select id="selectOrderSummary"
            resultType="io.github.atengk.report.vo.OrderReportSummaryVO">
        SELECT
            stat_date AS statDate,
            tenant_id AS tenantId,
            dept_id AS deptId,
            dept_name AS deptName,
            channel_code AS channelCode,
            channel_name AS channelName,
            COUNT(1) AS orderCount,
            SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN 1 ELSE 0 END) AS paidOrderCount,
            SUM(CASE WHEN order_status = 'REFUNDED' THEN 1 ELSE 0 END) AS refundOrderCount,
            IFNULL(SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN pay_amount ELSE 0 END), 0) AS payAmount,
            IFNULL(SUM(CASE WHEN order_status = 'REFUNDED' THEN refund_amount ELSE 0 END), 0) AS refundAmount,
            IFNULL(SUM(
                CASE
                    WHEN order_status IN ('PAID', 'REFUNDED')
                    THEN pay_amount - refund_amount
                    ELSE 0
                END
            ), 0) AS netAmount,
            CASE
                WHEN SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN 1 ELSE 0 END) = 0
                THEN 0
                ELSE ROUND(
                    SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN pay_amount ELSE 0 END)
                    / SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN 1 ELSE 0 END),
                    2
                )
            END AS avgOrderAmount
        FROM order_biz_detail
        WHERE
            <include refid="OrderReportWhere"/>
        GROUP BY
            stat_date,
            tenant_id,
            dept_id,
            dept_name,
            channel_code,
            channel_name
        ORDER BY
            stat_date DESC,
            dept_id ASC,
            channel_code ASC
    </select>

    <!-- 订单明细下钻：复用报表筛选条件，返回具体订单列表 -->
    <select id="selectOrderDetailPage"
            resultType="io.github.atengk.report.vo.OrderReportDetailVO">
        SELECT
            order_no AS orderNo,
            tenant_id AS tenantId,
            user_id AS userId,
            dept_id AS deptId,
            dept_name AS deptName,
            channel_code AS channelCode,
            channel_name AS channelName,
            order_status AS orderStatus,
            pay_amount AS payAmount,
            refund_amount AS refundAmount,
            stat_date AS statDate,
            pay_time AS payTime,
            refund_time AS refundTime,
            created_time AS createdTime
        FROM order_biz_detail
        WHERE
            <include refid="OrderReportWhere"/>
        ORDER BY
            created_time DESC,
            id DESC
    </select>

</mapper>
```

### 报表统计 Service

报表统计 Service 负责参数校验、缓存读取、数据库统计和缓存回写。查询范围默认限制为 31 天，避免运营后台一次性扫过多数据。

文件位置：`src/main/java/io/github/atengk/report/service/OrderReportService.java`

```java
package io.github.atengk.report.service;

import io.github.atengk.report.dto.OrderReportQueryDTO;
import io.github.atengk.report.vo.OrderReportSummaryVO;

import java.util.List;

/**
 * 订单报表统计 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface OrderReportService {

    /**
     * 查询订单报表汇总数据
     *
     * @param query 查询参数
     * @return 汇总结果
     */
    List<OrderReportSummaryVO> querySummary(OrderReportQueryDTO query);

    /**
     * 刷新订单报表缓存
     *
     * @param query 查询参数
     * @return 最新汇总结果
     */
    List<OrderReportSummaryVO> refreshSummaryCache(OrderReportQueryDTO query);
}
```

文件位置：`src/main/java/io/github/atengk/report/service/impl/OrderReportServiceImpl.java`

```java
package io.github.atengk.report.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.report.dto.OrderReportQueryDTO;
import io.github.atengk.report.mapper.OrderBizDetailMapper;
import io.github.atengk.report.service.OrderReportService;
import io.github.atengk.report.service.ReportCacheService;
import io.github.atengk.report.vo.OrderReportSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 订单报表统计 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReportServiceImpl implements OrderReportService {

    private static final long MAX_QUERY_DAYS = 31L;

    private final OrderBizDetailMapper orderBizDetailMapper;

    private final ReportCacheService reportCacheService;

    /**
     * 查询订单报表汇总数据
     *
     * @param query 查询参数
     * @return 汇总结果
     */
    @Override
    public List<OrderReportSummaryVO> querySummary(OrderReportQueryDTO query) {
        this.checkAndFillQuery(query);

        String cacheKey = reportCacheService.buildSummaryCacheKey(query);
        if (BooleanUtil.isTrue(query.getUseCache())) {
            List<OrderReportSummaryVO> cachedList = reportCacheService.getObject(
                    cacheKey,
                    new TypeReference<List<OrderReportSummaryVO>>() {
                    }
            );
            if (cachedList != null) {
                log.info("订单报表命中缓存，cacheKey={}", cacheKey);
                return cachedList;
            }
        }

        List<OrderReportSummaryVO> summaryList = orderBizDetailMapper.selectOrderSummary(query);
        reportCacheService.setObject(cacheKey, summaryList, this.calcCacheTtl(query));

        log.info("订单报表查询完成，cacheKey={}，resultSize={}", cacheKey, CollUtil.size(summaryList));
        return summaryList;
    }

    /**
     * 刷新订单报表缓存
     *
     * @param query 查询参数
     * @return 最新汇总结果
     */
    @Override
    public List<OrderReportSummaryVO> refreshSummaryCache(OrderReportQueryDTO query) {
        this.checkAndFillQuery(query);

        String cacheKey = reportCacheService.buildSummaryCacheKey(query);
        reportCacheService.delete(cacheKey);

        query.setUseCache(Boolean.FALSE);
        List<OrderReportSummaryVO> summaryList = orderBizDetailMapper.selectOrderSummary(query);
        reportCacheService.setObject(cacheKey, summaryList, this.calcCacheTtl(query));

        log.info("订单报表缓存刷新完成，cacheKey={}，resultSize={}", cacheKey, CollUtil.size(summaryList));
        return summaryList;
    }

    /**
     * 校验并补全查询参数
     *
     * @param query 查询参数
     */
    private void checkAndFillQuery(OrderReportQueryDTO query) {
        if (query == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        if (query.getStartDate() == null || query.getEndDate() == null) {
            throw new IllegalArgumentException("查询日期不能为空");
        }
        if (query.getStartDate().isAfter(query.getEndDate())) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        Date startDate = DateUtil.parseDate(query.getStartDate().toString());
        Date endDate = DateUtil.parseDate(query.getEndDate().toString());
        long days = DateUtil.betweenDay(startDate, endDate, true);

        if (days > MAX_QUERY_DAYS) {
            throw new IllegalArgumentException("报表查询范围不能超过 31 天");
        }

        query.setTenantId(ObjectUtil.defaultIfNull(query.getTenantId(), 0L));
        query.setUseCache(ObjectUtil.defaultIfNull(query.getUseCache(), Boolean.TRUE));
    }

    /**
     * 计算报表缓存时间
     *
     * @param query 查询参数
     * @return 缓存时间
     */
    private Duration calcCacheTtl(OrderReportQueryDTO query) {
        LocalDate today = LocalDate.now();

        if (!query.getEndDate().isBefore(today)) {
            return Duration.ofMinutes(3);
        }
        if (query.getEndDate().isBefore(today.minusDays(7))) {
            return Duration.ofHours(2);
        }
        return Duration.ofMinutes(20);
    }
}
```

### 报表缓存 Service

报表缓存 Service 统一负责 Redis Key 生成、读取、写入和删除。缓存 Key 使用查询条件生成 MD5 摘要，避免 Key 过长。

文件位置：`src/main/java/io/github/atengk/report/service/ReportCacheService.java`

```java
package io.github.atengk.report.service;

import cn.hutool.core.lang.TypeReference;
import io.github.atengk.report.dto.OrderReportQueryDTO;

import java.time.Duration;

/**
 * 报表缓存 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ReportCacheService {

    /**
     * 构建订单报表汇总缓存 Key
     *
     * @param query 查询参数
     * @return 缓存 Key
     */
    String buildSummaryCacheKey(OrderReportQueryDTO query);

    /**
     * 获取缓存对象
     *
     * @param key           缓存 Key
     * @param typeReference 类型引用
     * @param <T>           返回类型
     * @return 缓存对象
     */
    <T> T getObject(String key, TypeReference<T> typeReference);

    /**
     * 写入缓存对象
     *
     * @param key   缓存 Key
     * @param value 缓存值
     * @param ttl   过期时间
     */
    void setObject(String key, Object value, Duration ttl);

    /**
     * 删除缓存
     *
     * @param key 缓存 Key
     */
    void delete(String key);
}
```

文件位置：`src/main/java/io/github/atengk/report/service/impl/ReportCacheServiceImpl.java`

```java
package io.github.atengk.report.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.report.dto.OrderReportQueryDTO;
import io.github.atengk.report.service.ReportCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报表缓存 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCacheServiceImpl implements ReportCacheService {

    private static final String ORDER_SUMMARY_CACHE_KEY = "report:order:summary:{}:{}";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构建订单报表汇总缓存 Key
     *
     * @param query 查询参数
     * @return 缓存 Key
     */
    @Override
    public String buildSummaryCacheKey(OrderReportQueryDTO query) {
        Map<String, Object> signMap = new LinkedHashMap<>();
        signMap.put("startDate", query.getStartDate());
        signMap.put("endDate", query.getEndDate());
        signMap.put("tenantId", query.getTenantId());
        signMap.put("deptId", query.getDeptId());
        signMap.put("channelCode", query.getChannelCode());
        signMap.put("orderStatus", query.getOrderStatus());

        String hash = DigestUtil.md5Hex(JSONUtil.toJsonStr(signMap));
        return StrUtil.format(ORDER_SUMMARY_CACHE_KEY, query.getTenantId(), hash);
    }

    /**
     * 获取缓存对象
     *
     * @param key           缓存 Key
     * @param typeReference 类型引用
     * @param <T>           返回类型
     * @return 缓存对象
     */
    @Override
    public <T> T getObject(String key, TypeReference<T> typeReference) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }

        try {
            return JSONUtil.toBean(json, typeReference, false);
        } catch (Exception e) {
            log.warn("报表缓存反序列化失败，key={}", key, e);
            this.delete(key);
            return null;
        }
    }

    /**
     * 写入缓存对象
     *
     * @param key   缓存 Key
     * @param value 缓存值
     * @param ttl   过期时间
     */
    @Override
    public void setObject(String key, Object value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), ttl);
        log.info("报表缓存写入完成，key={}，ttl={}秒", key, ttl.toSeconds());
    }

    /**
     * 删除缓存
     *
     * @param key 缓存 Key
     */
    @Override
    public void delete(String key) {
        Boolean deleted = stringRedisTemplate.delete(key);
        log.info("报表缓存删除完成，key={}，deleted={}", key, deleted);
    }
}
```

### 明细下钻 Service

明细下钻 Service 负责分页查询订单明细。它必须复用和汇总查询一致的筛选条件，保证汇总结果和下钻结果口径一致。

文件位置：`src/main/java/io/github/atengk/report/service/OrderReportDetailService.java`

```java
package io.github.atengk.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.report.dto.OrderDetailQueryDTO;
import io.github.atengk.report.vo.OrderReportDetailVO;

/**
 * 订单报表明细下钻 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface OrderReportDetailService {

    /**
     * 分页查询订单明细
     *
     * @param query 查询参数
     * @return 明细分页结果
     */
    IPage<OrderReportDetailVO> queryDetailPage(OrderDetailQueryDTO query);
}
```

文件位置：`src/main/java/io/github/atengk/report/service/impl/OrderReportDetailServiceImpl.java`

```java
package io.github.atengk.report.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.report.dto.OrderDetailQueryDTO;
import io.github.atengk.report.mapper.OrderBizDetailMapper;
import io.github.atengk.report.service.OrderReportDetailService;
import io.github.atengk.report.vo.OrderReportDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单报表明细下钻 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReportDetailServiceImpl implements OrderReportDetailService {

    private final OrderBizDetailMapper orderBizDetailMapper;

    /**
     * 分页查询订单明细
     *
     * @param query 查询参数
     * @return 明细分页结果
     */
    @Override
    public IPage<OrderReportDetailVO> queryDetailPage(OrderDetailQueryDTO query) {
        this.checkAndFillQuery(query);

        Page<OrderReportDetailVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<OrderReportDetailVO> resultPage = orderBizDetailMapper.selectOrderDetailPage(page, query);

        log.info("订单报表明细查询完成，pageNum={}，pageSize={}，total={}",
                query.getPageNum(), query.getPageSize(), resultPage.getTotal());
        return resultPage;
    }

    /**
     * 校验并补全查询参数
     *
     * @param query 查询参数
     */
    private void checkAndFillQuery(OrderDetailQueryDTO query) {
        if (query == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        if (query.getStartDate() == null || query.getEndDate() == null) {
            throw new IllegalArgumentException("查询日期不能为空");
        }
        if (query.getStartDate().isAfter(query.getEndDate())) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        query.setTenantId(ObjectUtil.defaultIfNull(query.getTenantId(), 0L));
        query.setPageNum(ObjectUtil.defaultIfNull(query.getPageNum(), 1L));
        query.setPageSize(ObjectUtil.defaultIfNull(query.getPageSize(), 20L));
    }
}
```

### Excel 导出 Service

Excel 导出 Service 复用报表汇总查询逻辑，并将 VO 转换为 EasyExcel 导出对象。这里采用同步导出，适合几千到几万行以内的报表结果。

文件位置：`src/main/java/io/github/atengk/report/excel/OrderReportExportExcel.java`

```java
package io.github.atengk.report.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单报表导出 Excel 行对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderReportExportExcel {

    @ExcelProperty("统计日期")
    private LocalDate statDate;

    @ExcelProperty("租户ID")
    private Long tenantId;

    @ExcelProperty("部门ID")
    private Long deptId;

    @ExcelProperty("部门名称")
    private String deptName;

    @ExcelProperty("渠道编码")
    private String channelCode;

    @ExcelProperty("渠道名称")
    private String channelName;

    @ExcelProperty("订单数")
    private Long orderCount;

    @ExcelProperty("支付订单数")
    private Long paidOrderCount;

    @ExcelProperty("退款订单数")
    private Long refundOrderCount;

    @ExcelProperty("支付金额")
    private BigDecimal payAmount;

    @ExcelProperty("退款金额")
    private BigDecimal refundAmount;

    @ExcelProperty("净收入")
    private BigDecimal netAmount;

    @ExcelProperty("客单价")
    private BigDecimal avgOrderAmount;
}
```

文件位置：`src/main/java/io/github/atengk/report/service/OrderReportExportService.java`

```java
package io.github.atengk.report.service;

import io.github.atengk.report.dto.OrderReportQueryDTO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 订单报表 Excel 导出 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface OrderReportExportService {

    /**
     * 导出订单报表
     *
     * @param query    查询参数
     * @param response HTTP 响应
     */
    void exportSummary(OrderReportQueryDTO query, HttpServletResponse response);
}
```

文件位置：`src/main/java/io/github/atengk/report/service/impl/OrderReportExportServiceImpl.java`

```java
package io.github.atengk.report.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.excel.EasyExcel;
import io.github.atengk.report.dto.OrderReportQueryDTO;
import io.github.atengk.report.excel.OrderReportExportExcel;
import io.github.atengk.report.service.OrderReportExportService;
import io.github.atengk.report.service.OrderReportService;
import io.github.atengk.report.vo.OrderReportSummaryVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * 订单报表 Excel 导出 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReportExportServiceImpl implements OrderReportExportService {

    private final OrderReportService orderReportService;

    /**
     * 导出订单报表
     *
     * @param query    查询参数
     * @param response HTTP 响应
     */
    @Override
    public void exportSummary(OrderReportQueryDTO query, HttpServletResponse response) {
        try {
            query.setUseCache(Boolean.FALSE);
            List<OrderReportSummaryVO> summaryList = orderReportService.querySummary(query);
            List<OrderReportExportExcel> excelList = BeanUtil.copyToList(summaryList, OrderReportExportExcel.class);

            String fileName = "订单报表_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
            String encodedFileName = URLUtil.encode(fileName, StandardCharsets.UTF_8);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName);

            EasyExcel.write(response.getOutputStream(), OrderReportExportExcel.class)
                    .sheet("订单报表")
                    .doWrite(excelList);

            log.info("订单报表导出完成，fileName={}，rowCount={}", fileName, CollUtil.size(excelList));
        } catch (Exception e) {
            log.error("订单报表导出失败", e);
            throw new IllegalStateException("订单报表导出失败");
        }
    }
}
```

### 指标口径查询 Service

指标口径查询 Service 用于返回启用状态的指标定义，前端可以在报表页面中展示「指标说明」。

文件位置：`src/main/java/io/github/atengk/report/service/ReportMetricService.java`

```java
package io.github.atengk.report.service;

import io.github.atengk.report.vo.ReportMetricVO;

import java.util.List;

/**
 * 报表指标口径 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ReportMetricService {

    /**
     * 查询启用的指标口径列表
     *
     * @return 指标口径列表
     */
    List<ReportMetricVO> listEnabledMetrics();
}
```

文件位置：`src/main/java/io/github/atengk/report/service/impl/ReportMetricServiceImpl.java`

```java
package io.github.atengk.report.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.report.entity.ReportMetricDefine;
import io.github.atengk.report.mapper.ReportMetricDefineMapper;
import io.github.atengk.report.service.ReportMetricService;
import io.github.atengk.report.vo.ReportMetricVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 报表指标口径 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMetricServiceImpl implements ReportMetricService {

    private final ReportMetricDefineMapper reportMetricDefineMapper;

    /**
     * 查询启用的指标口径列表
     *
     * @return 指标口径列表
     */
    @Override
    public List<ReportMetricVO> listEnabledMetrics() {
        List<ReportMetricDefine> metricList = reportMetricDefineMapper.selectList(
                Wrappers.lambdaQuery(ReportMetricDefine.class)
                        .eq(ReportMetricDefine::getEnabled, 1)
                        .orderByAsc(ReportMetricDefine::getDisplayOrder)
        );

        log.info("查询报表指标口径完成，count={}", metricList.size());
        return BeanUtil.copyToList(metricList, ReportMetricVO.class);
    }
}
```

### 报表 Controller

报表 Controller 对外提供汇总查询、明细下钻、Excel 导出和缓存刷新接口。

文件位置：`src/main/java/io/github/atengk/report/controller/OrderReportController.java`

```java
package io.github.atengk.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.report.dto.OrderDetailQueryDTO;
import io.github.atengk.report.dto.OrderReportQueryDTO;
import io.github.atengk.report.service.OrderReportDetailService;
import io.github.atengk.report.service.OrderReportExportService;
import io.github.atengk.report.service.OrderReportService;
import io.github.atengk.report.vo.OrderReportDetailVO;
import io.github.atengk.report.vo.OrderReportSummaryVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单报表 Controller
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/report/order")
public class OrderReportController {

    private final OrderReportService orderReportService;

    private final OrderReportDetailService orderReportDetailService;

    private final OrderReportExportService orderReportExportService;

    /**
     * 查询订单报表汇总
     *
     * @param query 查询参数
     * @return 汇总结果
     */
    @PostMapping("/summary")
    public List<OrderReportSummaryVO> summary(@Valid @RequestBody OrderReportQueryDTO query) {
        return orderReportService.querySummary(query);
    }

    /**
     * 查询订单明细下钻
     *
     * @param query 查询参数
     * @return 明细分页结果
     */
    @PostMapping("/detail")
    public IPage<OrderReportDetailVO> detail(@Valid @RequestBody OrderDetailQueryDTO query) {
        return orderReportDetailService.queryDetailPage(query);
    }

    /**
     * 导出订单报表
     *
     * @param query    查询参数
     * @param response HTTP 响应
     */
    @PostMapping("/export")
    public void export(@Valid @RequestBody OrderReportQueryDTO query, HttpServletResponse response) {
        orderReportExportService.exportSummary(query, response);
    }

    /**
     * 手动刷新订单报表缓存
     *
     * @param query 查询参数
     * @return 最新汇总结果
     */
    @PostMapping("/cache/refresh")
    public List<OrderReportSummaryVO> refreshCache(@Valid @RequestBody OrderReportQueryDTO query) {
        return orderReportService.refreshSummaryCache(query);
    }
}
```

指标口径 Controller 用于给前端页面提供统一的指标解释。

文件位置：`src/main/java/io/github/atengk/report/controller/ReportMetricController.java`

```java
package io.github.atengk.report.controller;

import io.github.atengk.report.service.ReportMetricService;
import io.github.atengk.report.vo.ReportMetricVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表指标口径 Controller
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/report/metrics")
public class ReportMetricController {

    private final ReportMetricService reportMetricService;

    /**
     * 查询启用的指标口径列表
     *
     * @return 指标口径列表
     */
    @GetMapping
    public List<ReportMetricVO> listMetrics() {
        return reportMetricService.listEnabledMetrics();
    }
}
```

当前核心代码已经覆盖以下链路：

```text
报表汇总查询
-> Redis 缓存查询
-> MySQL 聚合统计
-> 缓存回写
-> 明细下钻分页
-> Excel 同步导出
-> 指标口径查询
```

接口路径汇总如下：

| 接口                          | 方法 | 说明               |
| ----------------------------- | ---- | ------------------ |
| `/report/order/summary`       | POST | 查询订单报表汇总   |
| `/report/order/detail`        | POST | 查询订单明细下钻   |
| `/report/order/export`        | POST | 导出订单报表 Excel |
| `/report/order/cache/refresh` | POST | 手动刷新报表缓存   |
| `/report/metrics`             | GET  | 查询指标口径列表   |

## 定时统计任务

报表统计通常不能完全依赖实时 SQL，尤其是历史报表、首页驾驶舱、财务日报这类高频查询场景。这里使用 XXL-JOB 定时生成昨日统计快照，并预热常用报表缓存，对应 README 中提到的「实时统计和离线统计取舍、复杂 SQL 优化、缓存失效」这些核心难点。

本节新增以下文件：

```text
src/main/java/io/github/atengk/report
├── job
│   └── OrderReportSnapshotJob.java
├── service
│   └── OrderReportSnapshotService.java
├── service/impl
│   └── OrderReportSnapshotServiceImpl.java
└── mapper
    └── ReportOrderSummarySnapshotMapper.java

src/main/resources/mapper
└── ReportOrderSummarySnapshotMapper.xml
```

如果项目还没有接入 XXL-JOB，需要补充以下依赖和配置。

文件位置：`pom.xml`

```xml
<!-- XXL-JOB：用于执行报表快照生成和缓存预热任务 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.1</version>
</dependency>
```

文件位置：`src/main/resources/application.yml`

```yaml
xxl:
  job:
    admin:
      # XXL-JOB 调度中心地址
      addresses: http://127.0.0.1:8080/xxl-job-admin
    executor:
      # 执行器名称，需要和调度中心配置一致
      appname: report-service
      # 执行器注册地址，为空时自动注册
      address:
      # 执行器 IP，容器部署时可显式指定
      ip:
      # 执行器端口
      port: 9999
      # 执行日志保存路径
      logpath: ./logs/xxl-job
      # 执行日志保留天数
      logretentiondays: 30
    # XXL-JOB 通信 Token，没有配置可留空
    accessToken:
```

### XXL-JOB 任务入口

XXL-JOB 任务入口只负责任务参数解析、调用快照生成、调用缓存预热，不直接写 SQL。这样可以保证任务调度层足够薄，核心逻辑仍然留在 Service 中，方便接口触发、单元测试和后续补偿重跑。

支持两种执行方式：

```text
不传参数：默认生成昨日、tenantId = 0 的快照，并预热缓存
传 JSON 参数：按指定 tenantId 和 statDate 重建快照
```

任务参数示例：

```json
{
  "tenantId": 10001,
  "statDate": "2026-05-15"
}
```

文件位置：`src/main/java/io/github/atengk/report/job/OrderReportSnapshotJob.java`

```java
package io.github.atengk.report.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.report.service.OrderReportSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;

/**
 * 订单报表快照定时任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReportSnapshotJob {

    private final OrderReportSnapshotService orderReportSnapshotService;

    /**
     * 订单报表快照任务
     */
    @XxlJob("orderReportSnapshotJob")
    public void orderReportSnapshotJob() {
        try {
            String jobParam = XxlJobHelper.getJobParam();
            Long tenantId = 0L;
            LocalDate statDate = DateUtil.offsetDay(new Date(), -1)
                    .toLocalDateTime()
                    .toLocalDate();

            if (StrUtil.isNotBlank(jobParam)) {
                JSONObject jsonObject = JSONUtil.parseObj(jobParam);
                tenantId = jsonObject.getLong("tenantId", 0L);

                String statDateStr = jsonObject.getStr("statDate");
                if (StrUtil.isNotBlank(statDateStr)) {
                    statDate = LocalDate.parse(statDateStr);
                }
            }

            log.info("开始执行订单报表快照任务，tenantId={}，statDate={}", tenantId, statDate);

            orderReportSnapshotService.rebuildDailySnapshot(tenantId, statDate);
            orderReportSnapshotService.warmupHotSummaryCache(tenantId);

            String message = StrUtil.format("订单报表快照任务执行成功，tenantId={}，statDate={}", tenantId, statDate);
            XxlJobHelper.handleSuccess(message);
            log.info(message);
        } catch (Exception e) {
            log.error("订单报表快照任务执行失败", e);
            XxlJobHelper.handleFail("订单报表快照任务执行失败：" + e.getMessage());
        }
    }
}
```

XXL-JOB 调度中心中配置如下：

| 配置项     | 值                                   |
| ---------- | ------------------------------------ |
| JobHandler | `orderReportSnapshotJob`             |
| 调度类型   | CRON                                 |
| CRON       | `0 10 1 * * ?`                       |
| 说明       | 每天凌晨 1:10 生成昨日快照并预热缓存 |

### 统计快照生成逻辑

统计快照生成逻辑采用「先删后插」方式重建指定日期的数据，适合补偿重跑。比如某天订单数据修复后，可以重新执行指定日期任务，保证快照结果覆盖旧数据。

先在快照 Mapper 中增加自定义聚合写入方法。

文件位置：`src/main/java/io/github/atengk/report/mapper/ReportOrderSummarySnapshotMapper.java`

```java
package io.github.atengk.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.report.entity.ReportOrderSummarySnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 订单报表统计快照 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ReportOrderSummarySnapshotMapper extends BaseMapper<ReportOrderSummarySnapshot> {

    /**
     * 按日期生成订单报表快照
     *
     * @param tenantId 租户 ID
     * @param statDate 统计日期
     * @return 写入行数
     */
    int insertSnapshotByDate(@Param("tenantId") Long tenantId, @Param("statDate") LocalDate statDate);
}
```

下面的 SQL 直接从 `order_biz_detail` 表聚合生成快照，统计口径和报表实时查询保持一致。

文件位置：`src/main/resources/mapper/ReportOrderSummarySnapshotMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.report.mapper.ReportOrderSummarySnapshotMapper">

    <!-- 按指定日期和租户生成订单统计快照，适合定时任务和补偿重跑 -->
    <insert id="insertSnapshotByDate">
        INSERT INTO report_order_summary_snapshot (
            stat_date,
            tenant_id,
            dept_id,
            dept_name,
            channel_code,
            channel_name,
            order_count,
            paid_order_count,
            refund_order_count,
            pay_amount,
            refund_amount,
            net_amount,
            avg_order_amount,
            snapshot_time
        )
        SELECT
            stat_date AS stat_date,
            tenant_id AS tenant_id,
            dept_id AS dept_id,
            dept_name AS dept_name,
            channel_code AS channel_code,
            channel_name AS channel_name,
            COUNT(1) AS order_count,
            SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN 1 ELSE 0 END) AS paid_order_count,
            SUM(CASE WHEN order_status = 'REFUNDED' THEN 1 ELSE 0 END) AS refund_order_count,
            IFNULL(SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN pay_amount ELSE 0 END), 0) AS pay_amount,
            IFNULL(SUM(CASE WHEN order_status = 'REFUNDED' THEN refund_amount ELSE 0 END), 0) AS refund_amount,
            IFNULL(SUM(
                CASE
                    WHEN order_status IN ('PAID', 'REFUNDED')
                    THEN pay_amount - refund_amount
                    ELSE 0
                END
            ), 0) AS net_amount,
            CASE
                WHEN SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN 1 ELSE 0 END) = 0
                THEN 0
                ELSE ROUND(
                    SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN pay_amount ELSE 0 END)
                    / SUM(CASE WHEN order_status IN ('PAID', 'REFUNDED') THEN 1 ELSE 0 END),
                    2
                )
            END AS avg_order_amount,
            NOW() AS snapshot_time
        FROM order_biz_detail
        WHERE
            deleted = 0
            AND tenant_id = #{tenantId}
            AND stat_date = #{statDate}
        GROUP BY
            stat_date,
            tenant_id,
            dept_id,
            dept_name,
            channel_code,
            channel_name
    </insert>

</mapper>
```

快照 Service 负责删除旧快照、重新生成快照、预热缓存。

文件位置：`src/main/java/io/github/atengk/report/service/OrderReportSnapshotService.java`

```java
package io.github.atengk.report.service;

import java.time.LocalDate;

/**
 * 订单报表快照 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface OrderReportSnapshotService {

    /**
     * 重建指定日期的日报快照
     *
     * @param tenantId 租户 ID
     * @param statDate 统计日期
     */
    void rebuildDailySnapshot(Long tenantId, LocalDate statDate);

    /**
     * 预热热点报表缓存
     *
     * @param tenantId 租户 ID
     */
    void warmupHotSummaryCache(Long tenantId);
}
```

文件位置：`src/main/java/io/github/atengk/report/service/impl/OrderReportSnapshotServiceImpl.java`

```java
package io.github.atengk.report.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.report.dto.OrderReportQueryDTO;
import io.github.atengk.report.entity.ReportOrderSummarySnapshot;
import io.github.atengk.report.mapper.ReportOrderSummarySnapshotMapper;
import io.github.atengk.report.service.OrderReportService;
import io.github.atengk.report.service.OrderReportSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单报表快照 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReportSnapshotServiceImpl implements OrderReportSnapshotService {

    private final ReportOrderSummarySnapshotMapper reportOrderSummarySnapshotMapper;

    private final OrderReportService orderReportService;

    /**
     * 重建指定日期的日报快照
     *
     * @param tenantId 租户 ID
     * @param statDate 统计日期
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildDailySnapshot(Long tenantId, LocalDate statDate) {
        Long finalTenantId = ObjectUtil.defaultIfNull(tenantId, 0L);
        LocalDate finalStatDate = ObjectUtil.defaultIfNull(statDate, LocalDate.now().minusDays(1));

        int deletedCount = reportOrderSummarySnapshotMapper.delete(
                Wrappers.lambdaQuery(ReportOrderSummarySnapshot.class)
                        .eq(ReportOrderSummarySnapshot::getTenantId, finalTenantId)
                        .eq(ReportOrderSummarySnapshot::getStatDate, finalStatDate)
        );

        int insertedCount = reportOrderSummarySnapshotMapper.insertSnapshotByDate(finalTenantId, finalStatDate);

        log.info("订单报表快照重建完成，tenantId={}，statDate={}，deletedCount={}，insertedCount={}",
                finalTenantId, finalStatDate, deletedCount, insertedCount);
    }

    /**
     * 预热热点报表缓存
     *
     * @param tenantId 租户 ID
     */
    @Override
    public void warmupHotSummaryCache(Long tenantId) {
        Long finalTenantId = ObjectUtil.defaultIfNull(tenantId, 0L);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<OrderReportQueryDTO> hotQueryList = CollUtil.newArrayList(
                buildQuery(finalTenantId, today, today, null, null),
                buildQuery(finalTenantId, yesterday, yesterday, null, null),
                buildQuery(finalTenantId, today.minusDays(6), today, null, null),
                buildQuery(finalTenantId, today, today, null, "APP"),
                buildQuery(finalTenantId, today, today, null, "WECHAT")
        );

        for (OrderReportQueryDTO query : hotQueryList) {
            try {
                orderReportService.refreshSummaryCache(query);
            } catch (Exception e) {
                log.warn("订单报表热点缓存预热失败，tenantId={}，startDate={}，endDate={}，channelCode={}",
                        query.getTenantId(), query.getStartDate(), query.getEndDate(), query.getChannelCode(), e);
            }
        }

        log.info("订单报表热点缓存预热完成，tenantId={}，count={}", finalTenantId, hotQueryList.size());
    }

    /**
     * 构建热点查询条件
     *
     * @param tenantId    租户 ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param deptId      部门 ID
     * @param channelCode 渠道编码
     * @return 查询条件
     */
    private OrderReportQueryDTO buildQuery(Long tenantId,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           Long deptId,
                                           String channelCode) {
        OrderReportQueryDTO query = new OrderReportQueryDTO();
        query.setTenantId(tenantId);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setDeptId(deptId);
        query.setChannelCode(channelCode);
        query.setUseCache(Boolean.FALSE);
        return query;
    }
}
```

### 缓存预热逻辑

缓存预热本质上是提前执行几组高频查询，并把结果写入 Redis。这里预热五类常见查询：

```text
今日全渠道报表
昨日全渠道报表
最近 7 天全渠道报表
今日 APP 渠道报表
今日微信小程序渠道报表
```

预热逻辑已经集成在 `OrderReportSnapshotServiceImpl#warmupHotSummaryCache` 中，任务执行完成后，Redis 中会生成类似下面的 Key：

```text
report:order:summary:10001:5d41402abc4b2a76b9719d911017c592
report:order:summary:10001:098f6bcd4621d373cade4e832627b4f6
```

如果订单支付、退款成功后需要主动失效缓存，可以在订单状态变更后调用前面已经实现的刷新接口：

```text
POST /report/order/cache/refresh
```

实际生产中更推荐这样处理：

```text
今日数据：短缓存 TTL + 必要时主动刷新
历史数据：长缓存 TTL + 每日任务预热
固定看板：优先查询快照表
复杂明细：走分页，不做大结果集缓存
```

## 接口测试

下面使用 `curl` 测试核心接口。测试前需要确保已经执行前面的建表 SQL 和初始化数据。

### 查询报表汇总

接口地址：

```text
POST /report/order/summary
```

请求示例：

```bash
curl -X POST 'http://localhost:8080/report/order/summary' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 10001,
    "startDate": "2026-05-14",
    "endDate": "2026-05-15",
    "useCache": true
  }'
```

返回示例：

```json
[
  {
    "statDate": "2026-05-15",
    "tenantId": 10001,
    "deptId": 20001,
    "deptName": "华东运营部",
    "channelCode": "APP",
    "channelName": "移动 APP",
    "orderCount": 1,
    "paidOrderCount": 1,
    "refundOrderCount": 0,
    "payAmount": 199.00,
    "refundAmount": 0.00,
    "netAmount": 199.00,
    "avgOrderAmount": 199.00
  },
  {
    "statDate": "2026-05-15",
    "tenantId": 10001,
    "deptId": 20001,
    "deptName": "华东运营部",
    "channelCode": "WECHAT",
    "channelName": "微信小程序",
    "orderCount": 1,
    "paidOrderCount": 1,
    "refundOrderCount": 1,
    "payAmount": 299.00,
    "refundAmount": 299.00,
    "netAmount": 0.00,
    "avgOrderAmount": 299.00
  }
]
```

指定渠道查询：

```bash
curl -X POST 'http://localhost:8080/report/order/summary' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 10001,
    "startDate": "2026-05-15",
    "endDate": "2026-05-15",
    "channelCode": "APP",
    "useCache": true
  }'
```

手动刷新缓存：

```bash
curl -X POST 'http://localhost:8080/report/order/cache/refresh' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 10001,
    "startDate": "2026-05-15",
    "endDate": "2026-05-15"
  }'
```

### 查询指标口径

接口地址：

```text
GET /report/metrics
```

请求示例：

```bash
curl -X GET 'http://localhost:8080/report/metrics'
```

返回示例：

```json
[
  {
    "metricCode": "ORDER_COUNT",
    "metricName": "订单数",
    "metricGroup": "ORDER",
    "calcRule": "统计订单业务明细表中符合筛选条件的订单总数，包含待支付、已支付、已退款、已关闭订单",
    "dataScope": "order_biz_detail.deleted = 0",
    "displayOrder": 1,
    "remark": "用于观察整体订单规模"
  },
  {
    "metricCode": "PAY_AMOUNT",
    "metricName": "支付金额",
    "metricGroup": "FINANCE",
    "calcRule": "汇总符合筛选条件订单的 pay_amount 字段",
    "dataScope": "order_status IN ('PAID', 'REFUNDED') AND deleted = 0",
    "displayOrder": 4,
    "remark": "金额字段统一使用 DECIMAL(18,2)"
  }
]
```

这个接口适合在前端报表页面中做「指标说明」弹窗，避免运营、财务、研发对同一个字段理解不一致。

### 明细下钻查询

接口地址：

```text
POST /report/order/detail
```

请求示例：

```bash
curl -X POST 'http://localhost:8080/report/order/detail' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 10001,
    "startDate": "2026-05-15",
    "endDate": "2026-05-15",
    "deptId": 20001,
    "channelCode": "WECHAT",
    "pageNum": 1,
    "pageSize": 20
  }'
```

返回示例：

```json
{
  "records": [
    {
      "orderNo": "ORD202605150002",
      "tenantId": 10001,
      "userId": 90002,
      "deptId": 20001,
      "deptName": "华东运营部",
      "channelCode": "WECHAT",
      "channelName": "微信小程序",
      "orderStatus": "REFUNDED",
      "payAmount": 299.00,
      "refundAmount": 299.00,
      "statDate": "2026-05-15",
      "payTime": "2026-05-15T11:20:10",
      "refundTime": "2026-05-15T15:30:00",
      "createdTime": "2026-05-15T11:18:00"
    }
  ],
  "total": 1,
  "size": 20,
  "current": 1,
  "pages": 1
}
```

按订单状态下钻查询：

```bash
curl -X POST 'http://localhost:8080/report/order/detail' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 10001,
    "startDate": "2026-05-15",
    "endDate": "2026-05-15",
    "orderStatus": "REFUNDED",
    "pageNum": 1,
    "pageSize": 20
  }'
```

明细下钻接口必须和汇总接口使用同一套筛选条件。当前 XML 中已经通过 `OrderReportWhere` 公共 SQL 片段保证了这一点。

### 导出 Excel

接口地址：

```text
POST /report/order/export
```

请求示例：

```bash
curl -X POST 'http://localhost:8080/report/order/export' \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": 10001,
    "startDate": "2026-05-14",
    "endDate": "2026-05-15",
    "useCache": false
  }' \
  -o 订单报表.xlsx
```

浏览器下载时，请求体和汇总查询一致。后端会返回 Excel 文件流，文件名格式如下：

```text
订单报表_20260515123045.xlsx
```

导出结果字段如下：

| 字段       | 说明               |
| ---------- | ------------------ |
| 统计日期   | `statDate`         |
| 租户 ID    | `tenantId`         |
| 部门 ID    | `deptId`           |
| 部门名称   | `deptName`         |
| 渠道编码   | `channelCode`      |
| 渠道名称   | `channelName`      |
| 订单数     | `orderCount`       |
| 支付订单数 | `paidOrderCount`   |
| 退款订单数 | `refundOrderCount` |
| 支付金额   | `payAmount`        |
| 退款金额   | `refundAmount`     |
| 净收入     | `netAmount`        |
| 客单价     | `avgOrderAmount`   |

同步导出适合报表汇总数据。如果后续要导出几十万行明细，建议改为异步导出任务：

```text
创建导出任务
-> 后台分页查询明细
-> EasyExcel 分批写入
-> 上传 MinIO
-> 返回下载地址
```

当前案例先保持同步导出，核心逻辑更清晰，也更适合普通后台报表模块快速落地。

## 实现效果

本节基于前面初始化数据和接口实现，展示报表模块的实际返回效果。重点验证四件事：汇总统计是否正确、明细下钻是否和汇总口径一致、Excel 导出字段是否完整、缓存和口径治理是否具备可扩展性。该模块对应 README 中「按时间、部门、渠道、状态聚合」「缓存热点报表」「支持明细下钻」「支持 Excel 导出」的核心功能链路。

### 报表汇总返回示例

使用以下查询条件：

```json
{
  "tenantId": 10001,
  "startDate": "2026-05-15",
  "endDate": "2026-05-15",
  "useCache": true
}
```

返回结果示例：

```json
[
  {
    "statDate": "2026-05-15",
    "tenantId": 10001,
    "deptId": 20001,
    "deptName": "华东运营部",
    "channelCode": "APP",
    "channelName": "移动 APP",
    "orderCount": 1,
    "paidOrderCount": 1,
    "refundOrderCount": 0,
    "payAmount": 199.00,
    "refundAmount": 0.00,
    "netAmount": 199.00,
    "avgOrderAmount": 199.00
  },
  {
    "statDate": "2026-05-15",
    "tenantId": 10001,
    "deptId": 20001,
    "deptName": "华东运营部",
    "channelCode": "WECHAT",
    "channelName": "微信小程序",
    "orderCount": 1,
    "paidOrderCount": 1,
    "refundOrderCount": 1,
    "payAmount": 299.00,
    "refundAmount": 299.00,
    "netAmount": 0.00,
    "avgOrderAmount": 299.00
  },
  {
    "statDate": "2026-05-15",
    "tenantId": 10001,
    "deptId": 20002,
    "deptName": "华南运营部",
    "channelCode": "PC",
    "channelName": "PC 官网",
    "orderCount": 1,
    "paidOrderCount": 1,
    "refundOrderCount": 0,
    "payAmount": 88.00,
    "refundAmount": 0.00,
    "netAmount": 88.00,
    "avgOrderAmount": 88.00
  },
  {
    "statDate": "2026-05-15",
    "tenantId": 10001,
    "deptId": 20002,
    "deptName": "华南运营部",
    "channelCode": "APP",
    "channelName": "移动 APP",
    "orderCount": 1,
    "paidOrderCount": 0,
    "refundOrderCount": 0,
    "payAmount": 0.00,
    "refundAmount": 0.00,
    "netAmount": 0.00,
    "avgOrderAmount": 0.00
  }
]
```

从结果可以看出：

| 数据              | 说明                                                       |
| ----------------- | ---------------------------------------------------------- |
| `ORD202605150001` | 已支付订单，计入支付订单数和支付金额                       |
| `ORD202605150002` | 已退款订单，计入支付订单数、退款订单数、支付金额、退款金额 |
| `ORD202605150003` | 已支付订单，计入支付统计                                   |
| `ORD202605150004` | 待支付订单，只计入订单数，不计入支付金额                   |

这里的关键点是：已退款订单仍然计入支付订单数，因为它曾经支付成功；退款金额单独统计，净收入使用支付金额减退款金额。这个规则必须在指标口径表中明确维护，否则运营和财务很容易对数据产生不同理解。

### 明细下钻返回示例

当运营人员发现 `2026-05-15` 微信小程序渠道退款金额异常时，可以携带同一组筛选条件查询明细。

请求参数：

```json
{
  "tenantId": 10001,
  "startDate": "2026-05-15",
  "endDate": "2026-05-15",
  "deptId": 20001,
  "channelCode": "WECHAT",
  "pageNum": 1,
  "pageSize": 20
}
```

返回结果示例：

```json
{
  "records": [
    {
      "orderNo": "ORD202605150002",
      "tenantId": 10001,
      "userId": 90002,
      "deptId": 20001,
      "deptName": "华东运营部",
      "channelCode": "WECHAT",
      "channelName": "微信小程序",
      "orderStatus": "REFUNDED",
      "payAmount": 299.00,
      "refundAmount": 299.00,
      "statDate": "2026-05-15",
      "payTime": "2026-05-15T11:20:10",
      "refundTime": "2026-05-15T15:30:00",
      "createdTime": "2026-05-15T11:18:00"
    }
  ],
  "total": 1,
  "size": 20,
  "current": 1,
  "pages": 1
}
```

该结果和汇总数据保持一致：

| 汇总字段           | 汇总值 | 明细验证                              |
| ------------------ | ------ | ------------------------------------- |
| `orderCount`       | 1      | 明细返回 1 条                         |
| `paidOrderCount`   | 1      | 该订单状态为 `REFUNDED`，曾经支付成功 |
| `refundOrderCount` | 1      | 该订单状态为 `REFUNDED`               |
| `payAmount`        | 299.00 | 明细订单支付金额为 299.00             |
| `refundAmount`     | 299.00 | 明细订单退款金额为 299.00             |
| `netAmount`        | 0.00   | `299.00 - 299.00 = 0.00`              |

报表系统最容易出问题的地方不是 SQL 写不出来，而是汇总和明细条件不一致。前面代码通过 XML 中的公共 SQL 片段 `OrderReportWhere` 复用了同一套筛选条件，这是保证口径一致的关键实现点。

### Excel 导出结果

调用导出接口：

```text
POST /report/order/export
```

请求参数：

```json
{
  "tenantId": 10001,
  "startDate": "2026-05-14",
  "endDate": "2026-05-15",
  "useCache": false
}
```

导出的文件名示例：

```text
订单报表_20260515123045.xlsx
```

Excel 内容示例：

| 统计日期   | 租户ID | 部门ID | 部门名称   | 渠道编码 | 渠道名称   | 订单数 | 支付订单数 | 退款订单数 | 支付金额 | 退款金额 | 净收入 | 客单价 |
| ---------- | ------ | ------ | ---------- | -------- | ---------- | ------ | ---------- | ---------- | -------- | -------- | ------ | ------ |
| 2026-05-15 | 10001  | 20001  | 华东运营部 | APP      | 移动 APP   | 1      | 1          | 0          | 199.00   | 0.00     | 199.00 | 199.00 |
| 2026-05-15 | 10001  | 20001  | 华东运营部 | WECHAT   | 微信小程序 | 1      | 1          | 1          | 299.00   | 299.00   | 0.00   | 299.00 |
| 2026-05-15 | 10001  | 20002  | 华南运营部 | PC       | PC 官网    | 1      | 1          | 0          | 88.00    | 0.00     | 88.00  | 88.00  |
| 2026-05-15 | 10001  | 20002  | 华南运营部 | APP      | 移动 APP   | 1      | 0          | 0          | 0.00     | 0.00     | 0.00   | 0.00   |
| 2026-05-14 | 10001  | 20001  | 华东运营部 | THIRD    | 第三方渠道 | 1      | 1          | 0          | 520.00   | 0.00     | 520.00 | 520.00 |

当前实现采用同步导出，适合导出报表汇总结果。如果后续要导出订单明细，建议改成异步导出任务，否则大数据量下容易出现接口超时、内存上涨、浏览器连接中断等问题。

## 关键问题处理

本节处理报表统计模块中最容易踩坑的几个问题：统计口径不一致、大数据分页性能差、缓存失效混乱、数据权限遗漏。这些问题也是报表类功能能否体现后端工程质量的关键。

### 统计口径统一

统计口径统一的核心不是写一个说明文档，而是让「接口返回字段、SQL 计算逻辑、指标口径表、Excel 导出字段」保持一致。

本案例通过三层控制口径：

```text
第一层：report_metric_define 表维护指标解释
第二层：Mapper XML 中固定指标 SQL 计算逻辑
第三层：VO / Excel 字段和指标编码保持一致
```

建议每个核心指标都维护以下信息：

| 内容     | 示例                                                   |
| -------- | ------------------------------------------------------ |
| 指标编码 | `PAY_AMOUNT`                                           |
| 指标名称 | 支付金额                                               |
| 计算规则 | 已支付和已退款订单的 `pay_amount` 汇总                 |
| 数据范围 | `order_status IN ('PAID', 'REFUNDED') AND deleted = 0` |
| 异常说明 | 已关闭、待支付订单不计入支付金额                       |
| 维护人   | 可扩展字段，例如财务负责人或数据负责人                 |
| 生效时间 | 可扩展字段，用于口径变更追踪                           |

口径统一建议遵守以下规则：

```text
同一个指标只允许有一个官方计算口径
统计 SQL 和指标口径说明必须同时变更
涉及财务类指标时，需要保留历史口径变更记录
前端展示名称可以变化，但后端指标编码不要随意变化
Excel 导出字段必须和页面展示字段保持一致
```

如果后续要支持口径版本治理，可以扩展一张口径版本表。

下面脚本用于扩展指标口径版本记录，适合财务报表、管理驾驶舱这类强审计场景。

```sql
-- 报表指标口径版本表：用于记录指标口径变更历史
CREATE TABLE report_metric_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    metric_code VARCHAR(64) NOT NULL COMMENT '指标编码',
    version_no VARCHAR(32) NOT NULL COMMENT '版本号，例如 v1、v2',
    calc_rule VARCHAR(1000) NOT NULL COMMENT '计算规则',
    data_scope VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '数据范围',
    change_reason VARCHAR(500) NOT NULL DEFAULT '' COMMENT '变更原因',
    effective_date DATE NOT NULL COMMENT '生效日期',
    created_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_metric_version (metric_code, version_no),
    KEY idx_metric_effective_date (metric_code, effective_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '报表指标口径版本表';
```

口径变更时，不建议直接修改旧规则后覆盖历史，而应该新增版本。例如：

```text
2026-05-01 前：支付金额只统计 PAID 状态
2026-05-01 后：支付金额统计 PAID、REFUNDED 状态
```

否则历史报表重跑后，可能出现「同一天数据在不同时间查询结果不一致」的问题。

### 大数据量分页

明细下钻查询在数据量大时，最容易出现慢 SQL。普通 `LIMIT offset, size` 在页码很深时性能会变差，因为数据库需要扫描并跳过大量记录。

当前案例中使用 MyBatis-Plus 分页，适合中小数据量后台查询。如果订单明细达到千万级，需要做进一步优化。

常规分页：

```sql
-- 普通分页：页码越深，offset 越大，性能越差
SELECT *
FROM order_biz_detail
WHERE tenant_id = 10001
  AND stat_date BETWEEN '2026-05-01' AND '2026-05-15'
  AND deleted = 0
ORDER BY created_time DESC, id DESC
LIMIT 100000, 20;
```

深分页建议改成游标分页，也就是基于上一页最后一条数据继续查询。

下面 SQL 用于深分页场景，通过 `lastCreatedTime` 和 `lastId` 继续向后翻页。

```sql
-- 游标分页：适合订单明细、日志明细、流水明细等大数据量下钻场景
SELECT
    order_no,
    tenant_id,
    user_id,
    dept_id,
    dept_name,
    channel_code,
    channel_name,
    order_status,
    pay_amount,
    refund_amount,
    stat_date,
    pay_time,
    refund_time,
    created_time
FROM order_biz_detail
WHERE tenant_id = 10001
  AND stat_date BETWEEN '2026-05-01' AND '2026-05-15'
  AND deleted = 0
  AND (
      created_time < '2026-05-15 11:18:00'
      OR (created_time = '2026-05-15 11:18:00' AND id < 10086)
  )
ORDER BY created_time DESC, id DESC
LIMIT 20;
```

为了支持游标分页，可以给明细查询 DTO 增加以下字段：

```java
package io.github.atengk.report.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 订单报表游标分页查询参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCursorDetailQueryDTO extends OrderReportQueryDTO {

    /**
     * 上一页最后一条记录的创建时间
     */
    private LocalDateTime lastCreatedTime;

    /**
     * 上一页最后一条记录的 ID
     */
    private Long lastId;

    /**
     * 每页数量
     */
    private Integer pageSize = 20;
}
```

大数据量下钻建议策略：

| 场景           | 建议方案                                    |
| -------------- | ------------------------------------------- |
| 前 100 页以内  | 普通分页可以接受                            |
| 深分页查询     | 使用游标分页                                |
| 按订单号搜索   | 走唯一索引                                  |
| 按用户查询订单 | 增加 `(tenant_id, user_id, stat_date)` 索引 |
| 大范围明细导出 | 异步任务 + 分批查询 + EasyExcel 分批写      |
| 多条件复杂检索 | 同步到 Elasticsearch                        |
| 大规模聚合分析 | 同步到 ClickHouse                           |

索引也要围绕查询条件设计。当前案例已提供常用组合索引：

```text
(tenant_id, stat_date)
(tenant_id, dept_id, stat_date)
(tenant_id, channel_code, stat_date)
(tenant_id, order_status, stat_date)
```

如果明细下钻主要按照创建时间排序，建议补充下面的索引。

```sql
-- 明细下钻分页优化索引：适合按租户、日期过滤后按创建时间倒序分页
ALTER TABLE order_biz_detail
ADD INDEX idx_tenant_date_created_id (
    tenant_id,
    stat_date,
    created_time,
    id
);
```

### 缓存失效策略

报表缓存不能只考虑「如何写入」，更重要的是「什么时候失效」。如果缓存策略设计不好，会出现今日数据不准、历史数据长期不更新、刷新接口删除不干净等问题。

本案例采用的策略是：

```text
今日数据：短 TTL，默认 3 分钟
近 7 天历史数据：中等 TTL，默认 20 分钟
更早历史数据：长 TTL，默认 2 小时
热点报表：XXL-JOB 定时预热
手动刷新：删除指定查询条件缓存后重新查询
```

这种策略比「订单变更后精确删除所有相关缓存」更稳妥。因为报表筛选条件组合很多，例如日期、部门、渠道、状态、租户都可能参与缓存 Key，精确删除所有组合 Key 的复杂度很高，也容易漏删。

推荐缓存处理方式：

| 数据类型       | 策略                                      |
| -------------- | ----------------------------------------- |
| 今日数据       | 短 TTL 为主，订单状态变更后可刷新重点缓存 |
| 昨日数据       | 定时快照 + 定时预热                       |
| 历史数据       | 长 TTL，必要时提供手动刷新                |
| 管理驾驶舱首页 | 固定 Key，定时预热                        |
| 明细分页       | 通常不缓存，依赖索引和分页优化            |

如果需要按租户批量删除报表缓存，可以额外维护缓存 Key 集合。

下面代码展示如何在写入缓存时记录 Key，便于按租户清理。

```java
package io.github.atengk.report.service.impl;

import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 报表缓存 Key 注册示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ReportCacheKeyRegistryDemo {

    private static final String CACHE_KEY_SET = "report:order:summary:keys:{}";

    private final StringRedisTemplate stringRedisTemplate;

    public ReportCacheKeyRegistryDemo(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 注册缓存 Key
     *
     * @param tenantId 租户 ID
     * @param cacheKey 缓存 Key
     */
    public void registerKey(Long tenantId, String cacheKey) {
        String keySet = StrUtil.format(CACHE_KEY_SET, tenantId);
        stringRedisTemplate.opsForSet().add(keySet, cacheKey);
        stringRedisTemplate.expire(keySet, Duration.ofDays(1));
    }

    /**
     * 按租户清理缓存 Key
     *
     * @param tenantId 租户 ID
     */
    public void clearTenantKeys(Long tenantId) {
        String keySet = StrUtil.format(CACHE_KEY_SET, tenantId);
        var keys = stringRedisTemplate.opsForSet().members(keySet);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        stringRedisTemplate.delete(keys);
        stringRedisTemplate.delete(keySet);
    }
}
```

实际生产中还可以使用以下增强方案：

```text
缓存空结果，防止重复穿透数据库
缓存 TTL 增加随机秒数，避免同一时间批量过期
热点查询定时预热，降低首次访问延迟
刷新缓存接口加权限控制，避免普通用户频繁刷新
大报表查询加分布式锁，避免缓存击穿
```

### 数据权限预留点

报表统计一般涉及运营、财务、管理层数据，不能只按查询条件过滤，还要叠加当前登录用户的数据权限。

常见权限范围如下：

| 权限范围           | 说明                     |
| ------------------ | ------------------------ |
| 全部数据           | 管理员、老板、财务负责人 |
| 本部门数据         | 部门负责人               |
| 本部门及子部门数据 | 区域负责人               |
| 指定部门数据       | 自定义授权               |
| 本人数据           | 业务员、客服、销售       |

当前案例已经在表中预留了 `tenant_id` 和 `dept_id` 字段。后续接入 Sa-Token 或 Spring Security 后，可以在 Service 层计算当前用户可见部门范围，然后传给 Mapper。

建议新增一个权限上下文对象。

```java
package io.github.atengk.report.security;

import lombok.Data;

import java.util.List;

/**
 * 报表数据权限上下文
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ReportDataScope {

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 是否允许查看全部数据
     */
    private Boolean allData = Boolean.FALSE;

    /**
     * 可见部门 ID 集合
     */
    private List<Long> visibleDeptIds;
}
```

报表查询 DTO 可以扩展一个内部字段，不由前端传入，只由后端权限逻辑设置。

```java
package io.github.atengk.report.dto;

import lombok.Data;

import java.util.List;

/**
 * 报表数据权限查询片段
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ReportDataScopeQuery {

    /**
     * 是否允许查看全部数据
     */
    private Boolean allData;

    /**
     * 可见部门 ID 集合
     */
    private List<Long> visibleDeptIds;
}
```

Mapper XML 中可以追加数据权限条件。

```xml
<!-- 数据权限过滤条件：由后端根据当前登录用户计算，不允许前端直接传入 -->
<if test="query.allData == false">
    <if test="query.visibleDeptIds != null and query.visibleDeptIds.size() > 0">
        AND dept_id IN
        <foreach collection="query.visibleDeptIds" item="deptId" open="(" separator="," close=")">
            #{deptId}
        </foreach>
    </if>
</if>
```

实际项目中更推荐这样处理：

```text
Controller 不接收权限字段
Service 根据当前登录用户计算权限范围
Mapper 只接收后端计算后的 deptId 集合
管理员可以绕过部门条件
普通用户必须附加部门过滤
租户 ID 必须从登录上下文获取，不建议完全信任前端传参
```

接入 Sa-Token 后，可以在 Service 层做类似处理：

```java
package io.github.atengk.report.security;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 报表数据权限解析器示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class ReportDataScopeResolver {

    /**
     * 解析当前登录用户的数据权限
     *
     * @return 数据权限上下文
     */
    public ReportDataScope resolveCurrentUserScope() {
        Object loginId = StpUtil.getLoginId();

        ReportDataScope scope = new ReportDataScope();
        scope.setTenantId(10001L);

        if (StpUtil.hasRole("report_admin")) {
            scope.setAllData(Boolean.TRUE);
            scope.setVisibleDeptIds(CollUtil.newArrayList());
            log.info("当前用户拥有报表全部数据权限，loginId={}", loginId);
            return scope;
        }

        scope.setAllData(Boolean.FALSE);
        scope.setVisibleDeptIds(this.queryUserDeptScope(loginId));
        log.info("当前用户拥有部门级报表数据权限，loginId={}，deptIds={}", loginId, scope.getVisibleDeptIds());
        return scope;
    }

    /**
     * 查询用户可见部门范围
     *
     * @param loginId 登录用户 ID
     * @return 部门 ID 集合
     */
    private List<Long> queryUserDeptScope(Object loginId) {
        // 实际项目中应从组织架构服务、权限服务或缓存中查询
        return CollUtil.newArrayList(20001L, 20002L);
    }
}
```

最终建议把数据权限作为报表模块的固定前置条件，而不是每个接口单独手写。可以通过以下方式逐步演进：

```text
第一阶段：Service 手动计算 deptId 集合并传入 Mapper
第二阶段：封装 ReportDataScopeResolver 统一解析权限
第三阶段：通过 AOP 自动注入权限上下文
第四阶段：通过 MyBatis 拦截器自动追加 tenant_id、dept_id 条件
```

对于当前案例，保留 `tenant_id`、`dept_id`、`dept_name` 字段已经足够支撑后续接入数据权限。实际上线时，至少要保证：

```text
tenant_id 从登录上下文获取
dept_id 范围由后端权限系统计算
前端传入的 deptId 只能作为筛选条件，不能作为权限依据
导出接口必须和查询接口使用同一套权限过滤
统计快照表也要带 tenant_id 和 dept_id
```

至此，一个轻量但完整的「报表统计与数据口径治理」后端案例已经覆盖：

```text
业务流程设计
数据模型设计
数据库脚本
汇总统计 SQL
Redis 缓存
明细下钻
Excel 导出
XXL-JOB 快照任务
缓存预热
指标口径治理
分页优化
数据权限预留
```