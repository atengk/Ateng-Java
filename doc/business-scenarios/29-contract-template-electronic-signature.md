# 合同 / 单据模板生成与电子签章

本文围绕「合同 / 单据模板生成与电子签章」场景实现一个可落地的后端案例，核心覆盖：模板维护、变量填充、Word/PDF 文件生成、电子签章发起、签署回调、文件归档。该场景来自 Java 后端经典业务场景中的第 29 项，原始功能描述包含模板维护、业务变量填充、Word/PDF 生成、签署发起、回调处理和归档留痕等能力。

## 场景目标

本案例实现一个简化版合同生成与电子签章模块，重点放在后端核心链路，不实现复杂的合同协同编辑、在线预览、复杂审批流和多签署方顺序签署编排。

最终目标是让业务系统能够通过接口完成以下动作：

```text
上传合同模板
-> 维护模板变量
-> 根据业务数据生成 Word 文件
-> 将 Word 转换为 PDF
-> 上传文件到 MinIO
-> 发起电子签署任务
-> 接收电子签平台回调
-> 更新签署状态
-> 下载并归档已签署文件
```

典型适用场景：

| 场景      | 示例                           |
| --------- | ------------------------------ |
| 合同系统  | 采购合同、销售合同、服务协议   |
| HR 系统   | 劳动合同、入职协议、证明文件   |
| 财务系统  | 报销单、付款申请单、结算确认单 |
| 采购系统  | 采购订单、验收单、供应商协议   |
| SaaS 平台 | 电子协议、授权书、在线确认单   |

### 核心业务流程

本案例采用「模板生成 + 文件归档 + 第三方电子签」的常见工程方案。

```text
业务系统
  |
  | 1. 上传 Word 模板
  v
合同模板服务
  |
  | 2. 保存模板文件和模板变量
  v
MinIO + MySQL
  |
  | 3. 业务方提交合同生成参数
  v
合同生成服务
  |
  | 4. poi-tl 填充 Word 模板
  | 5. LibreOffice 转 PDF
  | 6. 计算文件 SHA256 摘要
  | 7. 上传 Word / PDF 到 MinIO
  v
合同实例表
  |
  | 8. 发起电子签署
  v
电子签平台
  |
  | 9. 用户完成签署
  | 10. 回调签署结果
  v
签署回调接口
  |
  | 11. 验签、幂等、更新状态
  | 12. 下载已签署 PDF 并归档
  v
合同归档完成
```

核心状态流转建议如下：

```text
合同实例状态：

DRAFT           草稿
GENERATING      生成中
GENERATED       已生成
SIGNING         签署中
SIGNED          已签署
ARCHIVED        已归档
FAILED          处理失败
CANCELLED       已取消
```

签署任务状态建议如下：

```text
签署任务状态：

INIT            待发起
SUBMITTED       已提交签署平台
WAIT_SIGN       待签署
SIGNED          签署完成
REJECTED        签署拒绝
EXPIRED         签署过期
FAILED          签署失败
```

### 功能边界

本案例重点实现后端核心链路，覆盖能够体现 Java 后端业务建模、文件处理、第三方平台对接、回调幂等和归档留痕的部分。

本案例实现：

| 功能         | 说明                                       |
| ------------ | ------------------------------------------ |
| 合同模板上传 | 上传 `.docx` 模板文件，保存到 MinIO        |
| 模板变量维护 | 维护模板编码、模板名称、版本号和变量说明   |
| 合同实例生成 | 根据模板和业务变量生成合同 Word            |
| PDF 转换     | 使用 LibreOffice Headless 将 Word 转为 PDF |
| 文件摘要     | 使用 SHA256 计算文件摘要，辅助防篡改       |
| 文件归档     | Word、PDF、已签署 PDF 统一上传 MinIO       |
| 签署任务创建 | 保存签署任务，调用电子签平台               |
| 签署回调处理 | 验签、幂等、更新状态、归档已签署文件       |
| 查询合同详情 | 查询合同基础信息、文件地址、签署状态       |

本案例暂不实现：

| 功能             | 原因                                                        |
| ---------------- | ----------------------------------------------------------- |
| 在线合同编辑     | 通常依赖 OnlyOffice、WPS 开放平台或自研文档协同，复杂度较高 |
| 多签署方顺序签署 | 属于签署流程编排，可在基础签署任务上扩展                    |
| 合同审批流       | 可对接 Flowable / Camunda，不放在本案例核心链路             |
| 法律合规细节     | 不同电子签平台、地区法规、企业认证方式差异较大              |
| 复杂印章管理     | 印章权限、用印审批、印章风控独立成模块更合适                |
| 文件加密存储     | 可作为安全增强项，不影响主流程实现                          |

## 技术选型

本案例使用 Spring Boot 3 作为基础框架，使用 MyBatis-Plus 管理合同模板、合同实例、签署任务和回调记录，使用 MinIO 存储模板文件和生成后的合同文件，使用 poi-tl 完成 Word 模板变量填充，使用 LibreOffice Headless 完成 Word 到 PDF 的转换，使用 Hutool 处理摘要、日期、JSON、字符串和文件工具逻辑。

整体技术选型如下：

| 能力       | 技术方案                    |
| ---------- | --------------------------- |
| Web 接口   | Spring Boot 3               |
| 数据访问   | MyBatis-Plus                |
| 数据库     | MySQL                       |
| 模板填充   | poi-tl                      |
| Word 处理  | Apache POI                  |
| PDF 转换   | LibreOffice Headless        |
| 文件存储   | MinIO                       |
| 摘要计算   | Hutool SecureUtil           |
| JSON 处理  | Hutool JSONUtil             |
| 日期处理   | Hutool DateUtil             |
| 唯一 ID    | Hutool IdUtil               |
| 第三方调用 | Hutool HttpUtil / OpenFeign |
| 异步解耦   | RabbitMQ，可选              |
| 定时补偿   | XXL-JOB，可选               |

### 后端基础技术栈

后端基础技术栈采用常规 Spring Boot 单体模块结构，便于直接运行和调试。后续如果需要微服务化，可以将模板服务、文件服务、签署服务拆成独立服务。

建议项目模块结构：

```text
contract-sign-demo
├── src/main/java/io/github/atengk/contractsign
│   ├── controller
│   ├── service
│   ├── service/impl
│   ├── mapper
│   ├── entity
│   ├── dto
│   ├── vo
│   ├── enums
│   ├── client
│   ├── config
│   └── common
├── src/main/resources
│   ├── mapper
│   └── application.yml
└── pom.xml
```

基础组件职责：

| 组件       | 职责                                                     |
| ---------- | -------------------------------------------------------- |
| Controller | 提供模板上传、合同生成、发起签署、回调接收、详情查询接口 |
| Service    | 处理合同模板、合同实例、签署任务核心业务                 |
| Mapper     | MyBatis-Plus 数据访问                                    |
| Entity     | 数据库实体对象                                           |
| DTO        | 接口入参                                                 |
| VO         | 接口返回结果                                             |
| Client     | 封装电子签平台接口调用                                   |
| Config     | MinIO、签署平台、PDF 转换配置                            |
| Common     | 统一返回、异常、文件工具、签名工具                       |

### 模板生成技术选型

模板生成采用 `poi-tl`，原因是它比直接使用 Apache POI 操作文档更简单，适合处理合同、证明、申请单这类固定模板 + 变量填充场景。

模板示例：

```text
甲方：{{partyAName}}
乙方：{{partyBName}}
合同编号：{{contractNo}}
合同金额：{{amount}}
签署日期：{{signDate}}
```

变量填充示例数据：

```json
{
  "partyAName": "杭州示例科技有限公司",
  "partyBName": "上海测试信息技术有限公司",
  "contractNo": "HT202605150001",
  "amount": "100000.00",
  "signDate": "2026-05-15"
}
```

模板生成阶段只负责生成未签署文件，不直接处理签章逻辑。

```text
模板文件 .docx
  |
  | poi-tl 填充变量
  v
未签署 Word 文件
  |
  | LibreOffice 转换
  v
未签署 PDF 文件
```

选型建议：

| 方案                     | 适用情况                       | 本案例是否采用 |
| ------------------------ | ------------------------------ | -------------- |
| poi-tl                   | Word 模板变量填充，语法简单    | 采用           |
| Apache POI               | 需要更底层控制 Word 内容       | 辅助使用       |
| EasyPOI                  | 偏 Excel 和 Office 导入导出    | 不作为主方案   |
| Freemarker + HTML 转 PDF | 适合纯 HTML 模板               | 不作为主方案   |
| iText                    | PDF 编辑、加水印、合并、签名等 | 可选增强       |
| PDFBox                   | PDF 读取、摘要、基础处理       | 可选增强       |

### 文件存储与 PDF 处理方案

文件统一存储到 MinIO，不直接保存在服务器本地目录。服务器本地目录只作为临时工作目录，用于生成 Word、转换 PDF 和下载已签署文件。

文件存储路径建议按租户、业务类型、日期和合同编号组织：

```text
contract-template/{templateCode}/{version}/{fileName}.docx

contract-instance/{yyyyMMdd}/{contractNo}/source.docx
contract-instance/{yyyyMMdd}/{contractNo}/preview.pdf
contract-instance/{yyyyMMdd}/{contractNo}/signed.pdf
```

PDF 转换采用 LibreOffice Headless：

```text
Word .docx
  |
  | soffice --headless --convert-to pdf
  v
PDF .pdf
```

这种方式的优点是实现成本低、兼容性较好，适合后端服务自动化转换。实际部署时建议将 LibreOffice 放到独立容器或独立转换节点，避免大量转换任务影响主业务服务。

文件处理关键点：

| 处理点     | 实现方式                                      |
| ---------- | --------------------------------------------- |
| 临时目录   | 使用固定工作目录，例如 `/data/contract/tmp`   |
| 文件名     | 使用合同编号或雪花 ID，避免中文和特殊字符     |
| 文件摘要   | 生成文件后计算 SHA256                         |
| 文件上传   | 上传到 MinIO 后删除本地临时文件               |
| 文件访问   | 后端生成临时访问地址，不直接暴露永久公网地址  |
| 文件防篡改 | 归档记录保存文件大小、SHA256、MinIO objectKey |

### 电子签章平台对接方案

电子签章不建议在业务系统中自研完整能力。实际项目中通常对接第三方电子签平台，业务系统只负责发起签署、接收回调、同步状态和归档文件。

本案例将电子签平台抽象成统一客户端接口，避免业务代码绑定某一家平台 SDK。

核心接口抽象：

```text
创建签署任务
查询签署状态
下载已签署文件
验签回调请求
```

对接流程：

```text
业务系统创建签署任务
  |
  | 调用电子签平台创建合同签署流程
  v
电子签平台返回 signFlowId、signUrl
  |
  | 用户跳转签署链接完成签署
  v
电子签平台回调业务系统
  |
  | 业务系统验签、幂等、更新状态
  v
下载已签署 PDF 并归档
```

平台对接时建议保留以下字段：

| 字段            | 说明                     |
| --------------- | ------------------------ |
| signFlowId      | 电子签平台签署流程 ID    |
| signTaskNo      | 本系统签署任务号         |
| callbackEventId | 回调事件 ID，用于幂等    |
| signUrl         | 用户签署地址             |
| signedFileUrl   | 平台返回的已签署文件地址 |
| platformStatus  | 平台原始签署状态         |
| signStatus      | 本系统标准签署状态       |
| callbackBody    | 回调原文，便于排查问题   |

本案例默认提供一个 `MockEsignClient` 作为演示实现，用来模拟电子签平台返回签署链接、回调状态和已签署文件下载。实际生产环境只需要替换为真实平台实现，例如：

```text
EsignClient
  ├── MockEsignClient          本地演示实现
  ├── EsignBaoClient           e签宝实现
  ├── FadadaClient             法大大实现
  └── ShangshangSignClient     上上签实现
```

推荐设计原则：

| 原则         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 平台能力抽象 | Controller 和业务 Service 不直接依赖具体 SDK     |
| 回调先落库   | 回调原始报文先保存，再做业务处理                 |
| 回调必须验签 | 防止伪造签署结果                                 |
| 回调必须幂等 | 同一个平台事件重复通知不能重复更新               |
| 状态不可逆   | 已签署、已归档状态不能被低优先级状态覆盖         |
| 文件归档优先 | 最终以本系统归档文件为准，不长期依赖平台下载地址 |

## 数据库设计

数据库只保存合同模板、合同实例、签署任务和回调记录的元数据，真实文件统一存储在 MinIO 中。这样可以避免数据库存储大文件，同时方便后续做文件权限控制、归档留痕和对象存储迁移。本节延续第 29 个业务场景中的「模板生成、PDF、签章、归档」核心链路设计。

本案例默认使用 MySQL 8.x，表命名统一使用 `t_contract_` 前缀。主键使用业务系统生成的雪花 ID，后续代码中可使用 Hutool 的 `IdUtil.getSnowflakeNextId()` 生成。

### 合同模板表

合同模板表用于保存 Word 模板的基础信息、模板版本、模板文件地址和模板变量定义。模板文件本身不保存到数据库，只保存 MinIO 的 `object_key`。

模板变量建议使用 JSON 存储，便于后端生成合同时做变量校验，也便于前端动态渲染变量输入表单。

```sql
-- 合同模板表：保存模板基础信息、版本、文件地址和变量定义
CREATE TABLE `t_contract_template` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码，例如 PURCHASE_CONTRACT',
  `template_name` VARCHAR(128) NOT NULL COMMENT '模板名称',
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '模板版本号',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `object_key` VARCHAR(512) NOT NULL COMMENT 'MinIO模板文件objectKey',
  `file_sha256` VARCHAR(128) NOT NULL COMMENT '模板文件SHA256摘要',
  `variable_schema` JSON NULL COMMENT '模板变量定义JSON',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 DISABLED禁用',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code_version_deleted` (`template_code`, `version_no`, `deleted`),
  KEY `idx_template_code` (`template_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板表';
```

`variable_schema` 示例：

```json
{
  "variables": [
    {
      "key": "partyAName",
      "name": "甲方名称",
      "required": true,
      "type": "string"
    },
    {
      "key": "partyBName",
      "name": "乙方名称",
      "required": true,
      "type": "string"
    },
    {
      "key": "contractAmount",
      "name": "合同金额",
      "required": true,
      "type": "decimal"
    },
    {
      "key": "signDate",
      "name": "签署日期",
      "required": false,
      "type": "date"
    }
  ]
}
```

核心设计点：

| 字段              | 说明                                        |
| ----------------- | ------------------------------------------- |
| `template_code`   | 业务侧使用的模板编码，不建议直接使用模板 ID |
| `version_no`      | 支持同一个模板编码存在多个版本              |
| `object_key`      | MinIO 中的模板文件路径                      |
| `file_sha256`     | 用于判断模板文件是否被替换或篡改            |
| `variable_schema` | 约束模板变量，生成合同时可做必填校验        |
| `status`          | 禁用后的模板不能继续生成新合同              |

### 合同实例表

合同实例表用于保存某一次合同生成结果。一个合同模板可以生成多个合同实例，每个合同实例对应一份具体业务合同或单据。

合同实例表需要保存 Word 文件、PDF 文件和最终已签署 PDF 文件的地址，同时保存文件摘要，方便归档校验。

```sql
-- 合同实例表：保存每一份实际生成出来的合同或单据
CREATE TABLE `t_contract_instance` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `contract_no` VARCHAR(64) NOT NULL COMMENT '合同编号',
  `template_id` BIGINT NOT NULL COMMENT '合同模板ID',
  `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码',
  `template_version` INT NOT NULL COMMENT '模板版本号',
  `business_type` VARCHAR(64) NOT NULL COMMENT '业务类型，例如 PURCHASE、HR、FINANCE',
  `business_id` VARCHAR(128) NOT NULL COMMENT '业务主键ID',
  `contract_title` VARCHAR(255) NOT NULL COMMENT '合同标题',
  `variable_data` JSON NOT NULL COMMENT '本次生成合同使用的变量数据',
  `word_object_key` VARCHAR(512) NULL COMMENT '生成后的Word文件objectKey',
  `pdf_object_key` VARCHAR(512) NULL COMMENT '未签署PDF文件objectKey',
  `signed_pdf_object_key` VARCHAR(512) NULL COMMENT '已签署PDF文件objectKey',
  `pdf_sha256` VARCHAR(128) NULL COMMENT '未签署PDF SHA256摘要',
  `signed_pdf_sha256` VARCHAR(128) NULL COMMENT '已签署PDF SHA256摘要',
  `file_size` BIGINT NULL COMMENT '未签署PDF文件大小，单位字节',
  `signed_file_size` BIGINT NULL COMMENT '已签署PDF文件大小，单位字节',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿 GENERATING生成中 GENERATED已生成 SIGNING签署中 SIGNED已签署 ARCHIVED已归档 FAILED失败 CANCELLED取消',
  `fail_reason` VARCHAR(1000) NULL COMMENT '失败原因',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  UNIQUE KEY `uk_business_type_id_deleted` (`business_type`, `business_id`, `deleted`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同实例表';
```

核心设计点：

| 字段                          | 说明                                                   |
| ----------------------------- | ------------------------------------------------------ |
| `contract_no`                 | 合同业务编号，展示给用户和外部系统                     |
| `business_type + business_id` | 防止同一个业务单据重复生成合同                         |
| `variable_data`               | 保留生成时的原始变量快照，避免业务数据变化影响历史合同 |
| `word_object_key`             | 生成后的 Word 文件地址                                 |
| `pdf_object_key`              | 未签署 PDF 文件地址                                    |
| `signed_pdf_object_key`       | 已签署归档文件地址                                     |
| `pdf_sha256`                  | 文件防篡改校验                                         |
| `status`                      | 控制合同生成、签署、归档状态流转                       |

### 签署任务表

签署任务表用于保存本系统和电子签平台之间的任务映射关系。一个合同实例可以对应一个签署任务，复杂场景下也可以扩展为一个合同实例对应多个签署任务。

本案例按简单模型设计：一份合同实例只发起一次签署任务。

```sql
-- 签署任务表：保存本系统签署任务和第三方电子签平台之间的映射关系
CREATE TABLE `t_contract_sign_task` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `sign_task_no` VARCHAR(64) NOT NULL COMMENT '本系统签署任务号',
  `contract_id` BIGINT NOT NULL COMMENT '合同实例ID',
  `contract_no` VARCHAR(64) NOT NULL COMMENT '合同编号',
  `platform_code` VARCHAR(64) NOT NULL COMMENT '电子签平台编码，例如 MOCK、ESIGN、FADADA',
  `platform_flow_id` VARCHAR(128) NULL COMMENT '第三方平台签署流程ID',
  `signer_name` VARCHAR(128) NOT NULL COMMENT '签署人姓名',
  `signer_mobile` VARCHAR(32) NULL COMMENT '签署人手机号',
  `signer_email` VARCHAR(128) NULL COMMENT '签署人邮箱',
  `sign_url` VARCHAR(1000) NULL COMMENT '签署链接',
  `status` VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '状态：INIT待发起 SUBMITTED已提交 WAIT_SIGN待签署 SIGNED已签署 REJECTED拒签 EXPIRED过期 FAILED失败',
  `platform_status` VARCHAR(64) NULL COMMENT '平台原始状态',
  `submitted_at` DATETIME NULL COMMENT '提交签署平台时间',
  `signed_at` DATETIME NULL COMMENT '签署完成时间',
  `expired_at` DATETIME NULL COMMENT '签署过期时间',
  `fail_reason` VARCHAR(1000) NULL COMMENT '失败原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sign_task_no` (`sign_task_no`),
  UNIQUE KEY `uk_contract_id_deleted` (`contract_id`, `deleted`),
  KEY `idx_contract_no` (`contract_no`),
  KEY `idx_platform_flow_id` (`platform_flow_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同签署任务表';
```

核心设计点：

| 字段               | 说明                                    |
| ------------------ | --------------------------------------- |
| `sign_task_no`     | 本系统内部签署任务号                    |
| `platform_code`    | 支持后续切换不同电子签平台              |
| `platform_flow_id` | 第三方平台签署流程 ID，回调时通常会带回 |
| `sign_url`         | 签署链接，可返回给前端跳转              |
| `status`           | 本系统标准状态                          |
| `platform_status`  | 平台原始状态，便于排查问题              |
| `expired_at`       | 后续可用于定时关闭或补偿查询            |

### 签署回调记录表

签署回调记录表用于保存电子签平台每一次回调的原始数据。回调记录应先落库，再处理业务状态，这样出现异常时可以追溯和补偿。

回调幂等建议使用 `platform_code + event_id` 做唯一约束。如果平台没有事件 ID，可以使用回调请求体的 SHA256 摘要作为幂等键。

```sql
-- 签署回调记录表：保存电子签平台回调原文、验签结果和处理结果
CREATE TABLE `t_contract_sign_callback` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `platform_code` VARCHAR(64) NOT NULL COMMENT '电子签平台编码',
  `event_id` VARCHAR(128) NOT NULL COMMENT '平台回调事件ID或请求体摘要',
  `platform_flow_id` VARCHAR(128) NULL COMMENT '第三方平台签署流程ID',
  `sign_task_no` VARCHAR(64) NULL COMMENT '本系统签署任务号',
  `contract_no` VARCHAR(64) NULL COMMENT '合同编号',
  `event_type` VARCHAR(64) NULL COMMENT '事件类型，例如 SIGN_COMPLETED',
  `callback_body` JSON NOT NULL COMMENT '回调原始JSON报文',
  `signature` VARCHAR(512) NULL COMMENT '回调签名',
  `verify_result` TINYINT NOT NULL DEFAULT 0 COMMENT '验签结果：0未通过 1通过',
  `handle_status` VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '处理状态：INIT待处理 SUCCESS成功 FAILED失败 IGNORED忽略',
  `handle_message` VARCHAR(1000) NULL COMMENT '处理说明',
  `callback_at` DATETIME NULL COMMENT '平台回调时间',
  `handled_at` DATETIME NULL COMMENT '本系统处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_event` (`platform_code`, `event_id`),
  KEY `idx_platform_flow_id` (`platform_flow_id`),
  KEY `idx_sign_task_no` (`sign_task_no`),
  KEY `idx_contract_no` (`contract_no`),
  KEY `idx_handle_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同签署回调记录表';
```

核心设计点：

| 字段             | 说明                               |
| ---------------- | ---------------------------------- |
| `event_id`       | 回调幂等键，防止重复处理           |
| `callback_body`  | 保存平台原始报文，方便排查争议问题 |
| `signature`      | 保存回调签名                       |
| `verify_result`  | 验签结果必须落库                   |
| `handle_status`  | 标记回调是否处理成功               |
| `handle_message` | 保存失败原因，便于补偿任务扫描     |

回调处理推荐顺序：

```text
接收回调
-> 计算 event_id
-> 插入回调记录
-> 如果唯一索引冲突，直接返回成功
-> 验签
-> 查询签署任务
-> 校验状态是否允许流转
-> 更新签署任务状态
-> 更新合同实例状态
-> 下载已签署文件
-> 上传 MinIO 归档
-> 更新回调处理结果
```

## 项目依赖与配置

项目依赖围绕四类能力展开：Web 接口、数据库访问、模板生成、文件存储与转换。电子签平台本案例先抽象接口，默认使用 Mock 实现；真实平台 SDK 可在后续替换。

### Maven 依赖配置

下面是核心依赖配置，可以直接加入 `pom.xml`。版本号建议统一由 Spring Boot Parent 和项目属性管理。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation：用于 DTO 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化 CRUD 和分页能力 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 8.x -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok：减少实体类、DTO、VO 样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：用于 ID、日期、JSON、摘要、文件工具等常用能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- poi-tl：Word 模板变量填充 -->
    <dependency>
        <groupId>com.deepoove</groupId>
        <artifactId>poi-tl</artifactId>
        <version>1.12.2</version>
    </dependency>

    <!-- Apache POI：Office 文档处理基础依赖，poi-tl 底层也会使用 -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>

    <!-- MinIO SDK：上传、下载、生成临时访问地址 -->
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <version>8.5.12</version>
    </dependency>

    <!-- OkHttp：MinIO SDK 和部分 HTTP 调用场景会使用 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- Apache Commons IO：文件名、流处理、临时文件处理辅助工具 -->
    <dependency>
        <groupId>commons-io</groupId>
        <artifactId>commons-io</artifactId>
        <version>2.16.1</version>
    </dependency>

    <!-- RabbitMQ：可选，用于签署回调后异步归档或通知 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Spring Boot Test：单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果当前案例先不接入 MQ，可以临时删除 `spring-boot-starter-amqp`。核心链路可以通过同步方式完成，后续再把「下载已签署文件并归档」改造成异步任务。

### MinIO 配置

MinIO 用于保存模板文件、生成后的 Word、未签署 PDF 和已签署 PDF。生产环境中不建议将 MinIO 的永久访问地址直接返回给前端，建议由后端生成带有效期的临时访问地址。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 后端服务端口
  port: 8080

spring:
  application:
    # 服务名称，后续接入注册中心时可复用
    name: contract-sign-demo

  datasource:
    # MySQL 连接地址，rewriteBatchedStatements 用于提升批量写入性能
    url: jdbc:mysql://127.0.0.1:3306/contract_sign_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true
    # 数据库账号，生产环境建议使用配置中心或环境变量
    username: root
    # 数据库密码，禁止提交真实生产密码
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  servlet:
    multipart:
      # 单个模板文件最大限制
      max-file-size: 20MB
      # 单次请求最大限制
      max-request-size: 30MB

mybatis-plus:
  configuration:
    # 开发环境可开启 SQL 日志，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 逻辑删除字段
      logic-delete-field: deleted
      # 已删除值
      logic-delete-value: 1
      # 未删除值
      logic-not-delete-value: 0

contract:
  minio:
    # MinIO 服务地址，生产环境建议使用内网地址
    endpoint: http://127.0.0.1:9000
    # MinIO 访问账号，生产环境建议使用环境变量注入
    access-key: minioadmin
    # MinIO 访问密钥，禁止提交真实生产密钥
    secret-key: minioadmin
    # 合同文件桶名称
    bucket-name: contract-files
    # 预览地址有效期，单位秒
    preview-expire-seconds: 3600
```

MinIO 本地启动命令：

```bash
docker run -d \
  --name minio-contract \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -v /data/minio/contract:/data \
  minio/minio server /data --console-address ":9001"
```

上面的命令会启动一个本地 MinIO 服务，API 地址为 `http://127.0.0.1:9000`，控制台地址为 `http://127.0.0.1:9001`。`/data/minio/contract` 是宿主机挂载目录，可按实际环境调整。

### LibreOffice 转换配置

LibreOffice 用于将 `.docx` 转换成 `.pdf`。本案例通过 Java 执行本地 `soffice` 命令完成转换。

Linux 环境安装 LibreOffice：

```bash
# Ubuntu / Debian
apt-get update
apt-get install -y libreoffice libreoffice-writer fonts-noto-cjk

# CentOS / Rocky Linux
yum install -y libreoffice libreoffice-writer google-noto-cjk-fonts
```

安装后验证命令：

```bash
soffice --headless --version
```

如果能正常输出 LibreOffice 版本，说明转换命令可用。

配置文件增加 PDF 转换参数。

文件位置：`src/main/resources/application.yml`

```yaml
contract:
  libre-office:
    # LibreOffice 可执行文件路径，Linux 常见为 /usr/bin/soffice
    command-path: /usr/bin/soffice
    # 合同生成临时目录，用于暂存 Word 和 PDF
    work-dir: /data/contract/tmp
    # 单次转换超时时间，单位秒
    timeout-seconds: 60
```

推荐在 Docker 镜像中预装 LibreOffice，或者将 PDF 转换独立成一个转换服务。单体项目中可以先直接调用本机命令，后续再拆分。

转换命令实际形态如下：

```bash
soffice \
  --headless \
  --convert-to pdf \
  --outdir /data/contract/tmp \
  /data/contract/tmp/HT202605150001.docx
```

参数说明：

| 参数               | 说明                       |
| ------------------ | -------------------------- |
| `--headless`       | 无界面模式，适合服务器环境 |
| `--convert-to pdf` | 指定转换目标格式为 PDF     |
| `--outdir`         | 指定 PDF 输出目录          |
| 最后一个参数       | 待转换的 Word 文件路径     |

### 电子签平台配置

电子签平台配置用于保存平台编码、接口地址、应用 ID、密钥和回调地址。本案例默认使用 `mock` 平台，便于先打通业务链路。后续替换真实平台时，只需要新增对应的 `EsignClient` 实现类。

文件位置：`src/main/resources/application.yml`

```yaml
contract:
  esign:
    # 当前启用的电子签平台：mock、esign、fadada
    platform-code: mock
    # 第三方平台接口地址，mock 模式下可配置为本地地址
    endpoint: http://127.0.0.1:8080/mock-esign
    # 应用ID，真实平台由电子签服务商提供
    app-id: demo-app-id
    # 应用密钥，生产环境必须放到配置中心或环境变量
    app-secret: demo-app-secret
    # 回调验签密钥，mock 模式下用于模拟签名校验
    callback-secret: demo-callback-secret
    # 签署完成后的平台回调地址，真实环境必须是公网可访问地址
    callback-url: http://127.0.0.1:8080/api/contracts/sign/callback
    # 签署链接过期小时数
    sign-url-expire-hours: 24
```

真实平台对接时，建议使用下面的配置结构区分不同平台：

```yaml
contract:
  esign:
    # 当前启用的平台编码
    platform-code: esign

    platforms:
      mock:
        # 本地模拟平台地址
        endpoint: http://127.0.0.1:8080/mock-esign
        app-id: mock-app-id
        app-secret: mock-app-secret
        callback-secret: mock-callback-secret

      esign:
        # e签宝接口地址，示例值，实际以平台文档为准
        endpoint: https://openapi.esign.cn
        app-id: ${ESIGN_APP_ID}
        app-secret: ${ESIGN_APP_SECRET}
        callback-secret: ${ESIGN_CALLBACK_SECRET}

      fadada:
        # 法大大接口地址，示例值，实际以平台文档为准
        endpoint: https://api.fadada.com
        app-id: ${FADADA_APP_ID}
        app-secret: ${FADADA_APP_SECRET}
        callback-secret: ${FADADA_CALLBACK_SECRET}
```

电子签平台配置的关键点：

| 配置项                  | 说明                                   |
| ----------------------- | -------------------------------------- |
| `platform-code`         | 当前启用的平台，用于选择具体客户端实现 |
| `endpoint`              | 平台接口地址                           |
| `app-id`                | 平台分配的应用 ID                      |
| `app-secret`            | 请求签名密钥                           |
| `callback-secret`       | 回调验签密钥                           |
| `callback-url`          | 平台回调本系统的地址                   |
| `sign-url-expire-hours` | 签署链接有效期                         |

生产环境注意事项：

| 项目     | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 密钥管理 | 不要写死在 `application.yml`，使用 Nacos、Kubernetes Secret 或环境变量 |
| 回调地址 | 必须使用 HTTPS 公网地址                                      |
| 回调验签 | 所有回调必须验签，不允许只靠 IP 白名单                       |
| 请求日志 | 记录请求流水，但手机号、身份证号、密钥等敏感字段必须脱敏     |
| 超时配置 | 第三方调用必须设置连接超时和读取超时                         |
| 重试策略 | 创建签署任务可以有限重试，回调处理必须幂等                   |

## 合同模板维护

合同模板维护负责把业务可复用的 `.docx` 模板保存起来，并记录模板编码、模板版本、模板变量和 MinIO 文件地址。该部分对应原场景中的「维护合同模板」和「模板变量填充」前置能力，是后续生成 Word / PDF 的基础。

模板文件推荐使用 Word 占位符语法，例如：

```text
甲方：{{partyAName}}
乙方：{{partyBName}}
合同编号：{{contractNo}}
合同金额：{{contractAmount}}
签署日期：{{signDate}}
```

模板维护的核心流程如下：

```text
上传 .docx 模板
-> 校验模板编码和文件格式
-> 解析模板变量
-> 生成模板版本号
-> 上传模板文件到 MinIO
-> 保存模板记录
```

### 模板上传接口

模板上传接口用于接收后台上传的 Word 模板文件。这里先实现核心逻辑：校验 `.docx` 文件、解析变量、上传 MinIO、保存模板元数据。

接口设计：

| 项目         | 内容                                   |
| ------------ | -------------------------------------- |
| 请求方式     | `POST`                                 |
| 接口路径     | `/api/contract/templates/upload`       |
| Content-Type | `multipart/form-data`                  |
| 主要参数     | `templateCode`、`templateName`、`file` |
| 返回结果     | 模板 ID、模板编码、版本号、变量列表    |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/contract/templates/upload' \
  -F 'templateCode=PURCHASE_CONTRACT' \
  -F 'templateName=采购合同模板' \
  -F 'file=@/Users/ateng/template/purchase-contract.docx'
```

本节代码默认已经创建 `ContractTemplateMapper` 和 `ContractTemplate` 实体，字段与前面 `t_contract_template` 表保持一致。完整实体和 Mapper 可以在后续「核心代码实现」章节统一展开。

文件位置：`src/main/java/io/github/atengk/contractsign/controller/ContractTemplateController.java`

下面的 Controller 提供模板上传和模板变量解析接口，适合作为后台管理端的模板维护入口。

```java
package io.github.atengk.contractsign.controller;

import io.github.atengk.contractsign.dto.ContractTemplateUploadDTO;
import io.github.atengk.contractsign.service.ContractTemplateService;
import io.github.atengk.contractsign.vo.ContractTemplateUploadVO;
import io.github.atengk.contractsign.vo.TemplateVariableVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 合同模板管理接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contract/templates")
public class ContractTemplateController {

    private final ContractTemplateService contractTemplateService;

    /**
     * 上传合同模板。
     *
     * @param templateCode 模板编码
     * @param templateName 模板名称
     * @param file 模板文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ContractTemplateUploadVO uploadTemplate(@RequestParam String templateCode,
                                                   @RequestParam String templateName,
                                                   @RequestPart("file") MultipartFile file) {
        ContractTemplateUploadDTO dto = new ContractTemplateUploadDTO();
        dto.setTemplateCode(templateCode);
        dto.setTemplateName(templateName);
        dto.setFile(file);
        return contractTemplateService.uploadTemplate(dto);
    }

    /**
     * 解析模板变量。
     *
     * @param file 模板文件
     * @return 变量列表
     */
    @PostMapping("/variables/parse")
    public List<TemplateVariableVO> parseVariables(@RequestPart("file") MultipartFile file) {
        return contractTemplateService.parseVariables(file);
    }
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/dto/ContractTemplateUploadDTO.java`

下面的 DTO 用于承载模板上传参数，文件对象只在 Controller 到 Service 之间传递，不入库。

```java
package io.github.atengk.contractsign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 合同模板上传参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ContractTemplateUploadDTO {

    /**
     * 模板编码，例如 PURCHASE_CONTRACT。
     */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    /**
     * 模板名称。
     */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /**
     * Word模板文件。
     */
    @NotNull(message = "模板文件不能为空")
    private MultipartFile file;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/vo/TemplateVariableVO.java`

下面的 VO 用于返回从 Word 模板中解析出的变量名，后续也可以扩展变量中文名、类型、是否必填等配置。

```java
package io.github.atengk.contractsign.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模板变量返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVariableVO {

    /**
     * 变量编码。
     */
    private String key;

    /**
     * 变量名称，默认与变量编码相同。
     */
    private String name;

    /**
     * 是否必填。
     */
    private Boolean required;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/vo/ContractTemplateUploadVO.java`

下面的 VO 用于返回模板上传结果，前端可以直接展示模板 ID、版本号和变量列表。

```java
package io.github.atengk.contractsign.vo;

import lombok.Data;

import java.util.List;

/**
 * 合同模板上传返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ContractTemplateUploadVO {

    /**
     * 模板ID。
     */
    private Long templateId;

    /**
     * 模板编码。
     */
    private String templateCode;

    /**
     * 模板名称。
     */
    private String templateName;

    /**
     * 版本号。
     */
    private Integer versionNo;

    /**
     * 模板变量。
     */
    private List<TemplateVariableVO> variables;
}
```

### 模板变量解析

模板变量解析用于从 `.docx` 文件中提取 `{{xxx}}` 格式的占位符。这里使用正则扫描 Word 文本内容，适合常见段落和表格中的变量。

文件位置：`src/main/java/io/github/atengk/contractsign/service/ContractTemplateService.java`

```java
package io.github.atengk.contractsign.service;

import io.github.atengk.contractsign.dto.ContractTemplateUploadDTO;
import io.github.atengk.contractsign.vo.ContractTemplateUploadVO;
import io.github.atengk.contractsign.vo.TemplateVariableVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 合同模板服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContractTemplateService {

    /**
     * 上传合同模板。
     *
     * @param dto 上传参数
     * @return 上传结果
     */
    ContractTemplateUploadVO uploadTemplate(ContractTemplateUploadDTO dto);

    /**
     * 解析模板变量。
     *
     * @param file 模板文件
     * @return 变量列表
     */
    List<TemplateVariableVO> parseVariables(MultipartFile file);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/ContractTemplateServiceImpl.java`

下面的实现类完成模板格式校验、变量解析、版本号生成、MinIO 上传和数据库保存。代码中 `ContractTemplateMapper`、`ContractTemplate` 按前面表结构创建即可。

```java
package io.github.atengk.contractsign.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.contractsign.dto.ContractTemplateUploadDTO;
import io.github.atengk.contractsign.entity.ContractTemplate;
import io.github.atengk.contractsign.mapper.ContractTemplateMapper;
import io.github.atengk.contractsign.service.ContractFileStorageService;
import io.github.atengk.contractsign.service.ContractTemplateService;
import io.github.atengk.contractsign.vo.ContractTemplateUploadVO;
import io.github.atengk.contractsign.vo.TemplateVariableVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合同模板服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractTemplateServiceImpl implements ContractTemplateService {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final ContractTemplateMapper contractTemplateMapper;
    private final ContractFileStorageService contractFileStorageService;

    /**
     * 上传合同模板。
     *
     * @param dto 上传参数
     * @return 上传结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractTemplateUploadVO uploadTemplate(ContractTemplateUploadDTO dto) {
        MultipartFile file = dto.getFile();
        checkTemplateFile(file);

        List<TemplateVariableVO> variables = parseVariables(file);
        Integer nextVersionNo = getNextVersionNo(dto.getTemplateCode());

        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "contract-template.docx");
        String datePath = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
        String objectKey = StrUtil.format("contract-template/{}/{}/{}_{}",
                dto.getTemplateCode(), nextVersionNo, datePath, originalFilename);

        try {
            String fileSha256 = DigestUtil.sha256Hex(file.getInputStream());
            contractFileStorageService.upload(objectKey, file.getInputStream(), file.getSize(), file.getContentType());

            ContractTemplate template = new ContractTemplate();
            template.setId(IdUtil.getSnowflakeNextId());
            template.setTemplateCode(dto.getTemplateCode());
            template.setTemplateName(dto.getTemplateName());
            template.setVersionNo(nextVersionNo);
            template.setFileName(originalFilename);
            template.setObjectKey(objectKey);
            template.setFileSha256(fileSha256);
            template.setVariableSchema(JSONUtil.toJsonStr(buildVariableSchema(variables)));
            template.setStatus("ENABLED");
            contractTemplateMapper.insert(template);

            log.info("合同模板上传成功，templateCode={}，versionNo={}，objectKey={}",
                    dto.getTemplateCode(), nextVersionNo, objectKey);

            ContractTemplateUploadVO vo = new ContractTemplateUploadVO();
            vo.setTemplateId(template.getId());
            vo.setTemplateCode(template.getTemplateCode());
            vo.setTemplateName(template.getTemplateName());
            vo.setVersionNo(template.getVersionNo());
            vo.setVariables(variables);
            return vo;
        } catch (Exception e) {
            log.error("合同模板上传失败，templateCode={}", dto.getTemplateCode(), e);
            throw new IllegalStateException("合同模板上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解析模板变量。
     *
     * @param file 模板文件
     * @return 变量列表
     */
    @Override
    public List<TemplateVariableVO> parseVariables(MultipartFile file) {
        checkTemplateFile(file);

        Set<String> variableKeys = new LinkedHashSet<>();
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                collectVariables(paragraph.getText(), variableKeys);
            }

            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        collectVariables(cell.getText(), variableKeys);
                    }
                }
            }

            List<TemplateVariableVO> variables = variableKeys.stream()
                    .map(key -> new TemplateVariableVO(key, key, Boolean.TRUE))
                    .toList();

            log.info("模板变量解析完成，变量数量={}", variables.size());
            return variables;
        } catch (Exception e) {
            log.error("模板变量解析失败，fileName={}", file.getOriginalFilename(), e);
            throw new IllegalStateException("模板变量解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 校验模板文件。
     *
     * @param file 模板文件
     */
    private void checkTemplateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("模板文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (StrUtil.isBlank(fileName) || !StrUtil.endWithIgnoreCase(fileName, ".docx")) {
            throw new IllegalArgumentException("仅支持上传 .docx 模板文件");
        }
    }

    /**
     * 提取文本中的模板变量。
     *
     * @param text 文本内容
     * @param variableKeys 变量集合
     */
    private void collectVariables(String text, Set<String> variableKeys) {
        if (StrUtil.isBlank(text)) {
            return;
        }
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            variableKeys.add(matcher.group(1));
        }
    }

    /**
     * 获取下一个模板版本号。
     *
     * @param templateCode 模板编码
     * @return 下一个版本号
     */
    private Integer getNextVersionNo(String templateCode) {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getTemplateCode, templateCode)
                .orderByDesc(ContractTemplate::getVersionNo)
                .last("LIMIT 1");

        ContractTemplate latest = contractTemplateMapper.selectOne(wrapper);
        return latest == null ? 1 : latest.getVersionNo() + 1;
    }

    /**
     * 构建变量结构。
     *
     * @param variables 变量列表
     * @return 变量结构
     */
    private Map<String, Object> buildVariableSchema(List<TemplateVariableVO> variables) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("variables", CollUtil.emptyIfNull(variables));
        return schema;
    }
}
```

### 模板版本管理

模板版本管理的核心规则是：同一个 `template_code` 可以存在多个版本，但同一个版本号不能重复。生成合同时默认使用启用状态下的最新版本，也可以指定版本号。

推荐规则：

| 规则       | 说明                                                   |
| ---------- | ------------------------------------------------------ |
| 新模板上传 | `version_no = 当前最大版本 + 1`                        |
| 历史模板   | 不物理删除，只做逻辑删除或禁用                         |
| 合同实例   | 必须保存当时使用的 `template_id` 和 `template_version` |
| 禁用模板   | 禁用后不能生成新合同，但历史合同仍可查询               |
| 版本回滚   | 可以把旧版本重新复制为一个新版本，不建议直接覆盖旧记录 |

查询最新可用模板的逻辑如下：

```text
template_code = ?
status = ENABLED
deleted = 0
ORDER BY version_no DESC
LIMIT 1
```

## 合同文件生成

合同文件生成负责把业务数据填充到 Word 模板中，并转换成 PDF 后上传到 MinIO。这个过程是合同系统最核心的链路之一，建议在数据库中明确记录每一步状态，便于失败后排查和重试。

核心流程：

```text
接收生成请求
-> 查询模板
-> 校验变量
-> 下载模板文件
-> poi-tl 渲染 Word
-> LibreOffice 转 PDF
-> 计算 PDF 摘要
-> 上传 Word 和 PDF 到 MinIO
-> 保存合同实例
```

### 业务变量组装

业务变量建议由业务系统传入，合同模块只做格式校验和填充，不主动查询过多业务表。这样合同模块可以保持相对通用。

请求示例：

```json
{
  "templateCode": "PURCHASE_CONTRACT",
  "businessType": "PURCHASE",
  "businessId": "PO202605150001",
  "contractTitle": "采购合同-PO202605150001",
  "variables": {
    "partyAName": "杭州示例科技有限公司",
    "partyBName": "上海测试信息技术有限公司",
    "contractNo": "HT202605150001",
    "contractAmount": "100000.00",
    "signDate": "2026-05-15"
  }
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/dto/ContractGenerateDTO.java`

下面的 DTO 用于承载合同生成请求，其中 `variables` 直接保存模板变量键值。

```java
package io.github.atengk.contractsign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * 合同生成参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ContractGenerateDTO {

    /**
     * 模板编码。
     */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    /**
     * 业务类型。
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /**
     * 业务ID。
     */
    @NotBlank(message = "业务ID不能为空")
    private String businessId;

    /**
     * 合同标题。
     */
    @NotBlank(message = "合同标题不能为空")
    private String contractTitle;

    /**
     * 模板变量。
     */
    @NotEmpty(message = "模板变量不能为空")
    private Map<String, Object> variables;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/vo/ContractGenerateVO.java`

下面的 VO 用于返回合同生成结果，包括合同编号、文件地址和状态。

```java
package io.github.atengk.contractsign.vo;

import lombok.Data;

/**
 * 合同生成返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ContractGenerateVO {

    /**
     * 合同ID。
     */
    private Long contractId;

    /**
     * 合同编号。
     */
    private String contractNo;

    /**
     * 合同标题。
     */
    private String contractTitle;

    /**
     * 未签署PDF文件objectKey。
     */
    private String pdfObjectKey;

    /**
     * 文件摘要。
     */
    private String pdfSha256;

    /**
     * 合同状态。
     */
    private String status;
}
```

### Word 模板填充

Word 模板填充使用 `poi-tl` 实现。核心思路是先从 MinIO 下载模板文件到本地临时目录，再使用变量 Map 渲染成新的 `.docx` 文件。

文件位置：`src/main/java/io/github/atengk/contractsign/service/ContractGenerateService.java`

```java
package io.github.atengk.contractsign.service;

import io.github.atengk.contractsign.dto.ContractGenerateDTO;
import io.github.atengk.contractsign.vo.ContractGenerateVO;

/**
 * 合同生成服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContractGenerateService {

    /**
     * 生成合同文件。
     *
     * @param dto 生成参数
     * @return 生成结果
     */
    ContractGenerateVO generate(ContractGenerateDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/ContractGenerateServiceImpl.java`

下面的实现类串联模板查询、Word 渲染、PDF 转换、摘要计算、MinIO 上传和合同实例保存。

```java
package io.github.atengk.contractsign.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.deepoove.poi.XWPFTemplate;
import io.github.atengk.contractsign.dto.ContractGenerateDTO;
import io.github.atengk.contractsign.entity.ContractInstance;
import io.github.atengk.contractsign.entity.ContractTemplate;
import io.github.atengk.contractsign.mapper.ContractInstanceMapper;
import io.github.atengk.contractsign.mapper.ContractTemplateMapper;
import io.github.atengk.contractsign.service.ContractFileStorageService;
import io.github.atengk.contractsign.service.ContractGenerateService;
import io.github.atengk.contractsign.service.WordToPdfService;
import io.github.atengk.contractsign.vo.ContractGenerateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

/**
 * 合同生成服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractGenerateServiceImpl implements ContractGenerateService {

    private final ContractTemplateMapper contractTemplateMapper;
    private final ContractInstanceMapper contractInstanceMapper;
    private final ContractFileStorageService contractFileStorageService;
    private final WordToPdfService wordToPdfService;

    /**
     * 生成合同文件。
     *
     * @param dto 生成参数
     * @return 生成结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractGenerateVO generate(ContractGenerateDTO dto) {
        ContractTemplate template = getLatestEnabledTemplate(dto.getTemplateCode());
        String contractNo = buildContractNo(dto.getBusinessType());

        File templateFile = null;
        File wordFile = null;
        File pdfFile = null;

        try {
            String datePath = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
            String basePath = StrUtil.format("contract-instance/{}/{}", datePath, contractNo);

            templateFile = FileUtil.createTempFile("template-", ".docx", true);
            wordFile = FileUtil.createTempFile(contractNo + "-", ".docx", true);

            contractFileStorageService.download(template.getObjectKey(), templateFile);

            renderWord(templateFile, wordFile, dto);

            pdfFile = wordToPdfService.convert(wordFile);

            String wordObjectKey = basePath + "/source.docx";
            String pdfObjectKey = basePath + "/preview.pdf";
            String pdfSha256 = DigestUtil.sha256Hex(pdfFile);
            long pdfFileSize = FileUtil.size(pdfFile);

            contractFileStorageService.upload(wordObjectKey, wordFile);
            contractFileStorageService.upload(pdfObjectKey, pdfFile);

            ContractInstance instance = new ContractInstance();
            instance.setId(IdUtil.getSnowflakeNextId());
            instance.setContractNo(contractNo);
            instance.setTemplateId(template.getId());
            instance.setTemplateCode(template.getTemplateCode());
            instance.setTemplateVersion(template.getVersionNo());
            instance.setBusinessType(dto.getBusinessType());
            instance.setBusinessId(dto.getBusinessId());
            instance.setContractTitle(dto.getContractTitle());
            instance.setVariableData(JSONUtil.toJsonStr(dto.getVariables()));
            instance.setWordObjectKey(wordObjectKey);
            instance.setPdfObjectKey(pdfObjectKey);
            instance.setPdfSha256(pdfSha256);
            instance.setFileSize(pdfFileSize);
            instance.setStatus("GENERATED");
            contractInstanceMapper.insert(instance);

            log.info("合同文件生成成功，contractNo={}，pdfObjectKey={}", contractNo, pdfObjectKey);

            ContractGenerateVO vo = new ContractGenerateVO();
            vo.setContractId(instance.getId());
            vo.setContractNo(contractNo);
            vo.setContractTitle(instance.getContractTitle());
            vo.setPdfObjectKey(pdfObjectKey);
            vo.setPdfSha256(pdfSha256);
            vo.setStatus(instance.getStatus());
            return vo;
        } catch (Exception e) {
            log.error("合同文件生成失败，businessType={}，businessId={}", dto.getBusinessType(), dto.getBusinessId(), e);
            throw new IllegalStateException("合同文件生成失败：" + e.getMessage(), e);
        } finally {
            FileUtil.del(templateFile);
            FileUtil.del(wordFile);
            FileUtil.del(pdfFile);
        }
    }

    /**
     * 查询最新启用模板。
     *
     * @param templateCode 模板编码
     * @return 模板记录
     */
    private ContractTemplate getLatestEnabledTemplate(String templateCode) {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getTemplateCode, templateCode)
                .eq(ContractTemplate::getStatus, "ENABLED")
                .orderByDesc(ContractTemplate::getVersionNo)
                .last("LIMIT 1");

        ContractTemplate template = contractTemplateMapper.selectOne(wrapper);
        if (template == null) {
            throw new IllegalArgumentException("未找到可用合同模板：" + templateCode);
        }
        return template;
    }

    /**
     * 渲染Word模板。
     *
     * @param templateFile 模板文件
     * @param wordFile 输出Word文件
     * @param dto 生成参数
     */
    private void renderWord(File templateFile, File wordFile, ContractGenerateDTO dto) {
        try (XWPFTemplate template = XWPFTemplate.compile(templateFile);
             FileOutputStream outputStream = new FileOutputStream(wordFile)) {
            template.render(dto.getVariables());
            template.write(outputStream);
        } catch (Exception e) {
            throw new IllegalStateException("Word模板填充失败：" + e.getMessage(), e);
        }
    }

    /**
     * 构建合同编号。
     *
     * @param businessType 业务类型
     * @return 合同编号
     */
    private String buildContractNo(String businessType) {
        String date = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
        long snowflakeId = IdUtil.getSnowflakeNextId();
        return StrUtil.format("HT{}{}{}", businessType, date, String.valueOf(snowflakeId).substring(8));
    }
}
```

### Word 转 PDF

Word 转 PDF 使用 LibreOffice Headless 命令实现。该服务只负责转换，不负责业务状态更新。

文件位置：`src/main/java/io/github/atengk/contractsign/config/LibreOfficeProperties.java`

下面的配置类读取 `application.yml` 中的 LibreOffice 参数。

```java
package io.github.atengk.contractsign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LibreOffice转换配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "contract.libre-office")
public class LibreOfficeProperties {

    /**
     * soffice命令路径。
     */
    private String commandPath;

    /**
     * 临时工作目录。
     */
    private String workDir;

    /**
     * 转换超时时间，单位秒。
     */
    private Long timeoutSeconds;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/WordToPdfService.java`

```java
package io.github.atengk.contractsign.service;

import java.io.File;

/**
 * Word转PDF服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface WordToPdfService {

    /**
     * Word转换为PDF。
     *
     * @param wordFile Word文件
     * @return PDF文件
     */
    File convert(File wordFile);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/LibreOfficeWordToPdfServiceImpl.java`

下面的实现类通过 `ProcessBuilder` 调用 `soffice` 命令，并校验转换后的 PDF 是否存在。

```java
package io.github.atengk.contractsign.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.contractsign.config.LibreOfficeProperties;
import io.github.atengk.contractsign.service.WordToPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * LibreOffice Word转PDF服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibreOfficeWordToPdfServiceImpl implements WordToPdfService {

    private final LibreOfficeProperties libreOfficeProperties;

    /**
     * Word转换为PDF。
     *
     * @param wordFile Word文件
     * @return PDF文件
     */
    @Override
    public File convert(File wordFile) {
        if (wordFile == null || !wordFile.exists()) {
            throw new IllegalArgumentException("Word文件不存在");
        }

        File workDir = FileUtil.mkdir(libreOfficeProperties.getWorkDir());
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    libreOfficeProperties.getCommandPath(),
                    "--headless",
                    "--convert-to",
                    "pdf",
                    "--outdir",
                    workDir.getAbsolutePath(),
                    wordFile.getAbsolutePath()
            );
            processBuilder.redirectErrorStream(true);

            log.info("开始执行Word转PDF，wordFile={}", wordFile.getAbsolutePath());
            Process process = processBuilder.start();

            boolean finished = process.waitFor(libreOfficeProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Word转PDF超时");
            }

            if (process.exitValue() != 0) {
                throw new IllegalStateException("Word转PDF命令执行失败，exitCode=" + process.exitValue());
            }

            String pdfName = StrUtil.subBefore(wordFile.getName(), ".", true) + ".pdf";
            File pdfFile = FileUtil.file(workDir, pdfName);
            if (!pdfFile.exists()) {
                throw new IllegalStateException("Word转PDF失败，未生成PDF文件");
            }

            log.info("Word转PDF成功，pdfFile={}", pdfFile.getAbsolutePath());
            return pdfFile;
        } catch (Exception e) {
            log.error("Word转PDF异常，wordFile={}", wordFile.getAbsolutePath(), e);
            throw new IllegalStateException("Word转PDF异常：" + e.getMessage(), e);
        }
    }
}
```

### 文件摘要与防篡改

文件摘要用于证明归档文件没有被替换。生成 PDF 后立即计算 SHA256，并保存到 `t_contract_instance.pdf_sha256`。

核心代码在生成服务中已经体现：

```java
String pdfSha256 = DigestUtil.sha256Hex(pdfFile);
long pdfFileSize = FileUtil.size(pdfFile);
```

防篡改策略建议：

| 策略           | 说明                                       |
| -------------- | ------------------------------------------ |
| 保存 SHA256    | 归档时保存文件摘要，后续下载可重新计算比对 |
| 保存文件大小   | 辅助判断文件是否被替换                     |
| 保存 objectKey | 不直接依赖临时访问 URL                     |
| 已归档不可覆盖 | 已签署文件上传路径不允许覆盖               |
| 操作留痕       | 上传、生成、签署、归档都记录操作日志       |

### 文件上传 MinIO

文件上传服务统一封装 MinIO 操作，避免业务 Service 直接依赖 MinIO SDK。

文件位置：`src/main/java/io/github/atengk/contractsign/config/MinioProperties.java`

下面的配置类读取 `application.yml` 中的 MinIO 参数。

```java
package io.github.atengk.contractsign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "contract.minio")
public class MinioProperties {

    /**
     * MinIO服务地址。
     */
    private String endpoint;

    /**
     * 访问账号。
     */
    private String accessKey;

    /**
     * 访问密钥。
     */
    private String secretKey;

    /**
     * 桶名称。
     */
    private String bucketName;

    /**
     * 预览地址有效期，单位秒。
     */
    private Integer previewExpireSeconds;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/config/MinioConfig.java`

下面的配置类初始化 `MinioClient`。

```java
package io.github.atengk.contractsign.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO客户端配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    /**
     * 创建MinIO客户端。
     *
     * @return MinIO客户端
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/ContractFileStorageService.java`

```java
package io.github.atengk.contractsign.service;

import java.io.File;
import java.io.InputStream;

/**
 * 合同文件存储服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContractFileStorageService {

    /**
     * 上传本地文件。
     *
     * @param objectKey 对象Key
     * @param file 本地文件
     */
    void upload(String objectKey, File file);

    /**
     * 上传输入流。
     *
     * @param objectKey 对象Key
     * @param inputStream 输入流
     * @param size 文件大小
     * @param contentType 文件类型
     */
    void upload(String objectKey, InputStream inputStream, long size, String contentType);

    /**
     * 下载文件到本地。
     *
     * @param objectKey 对象Key
     * @param targetFile 目标文件
     */
    void download(String objectKey, File targetFile);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/MinioContractFileStorageServiceImpl.java`

下面的实现类封装 MinIO 上传和下载，并在首次使用时自动创建桶。

```java
package io.github.atengk.contractsign.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.contractsign.config.MinioProperties;
import io.github.atengk.contractsign.service.ContractFileStorageService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;

/**
 * MinIO合同文件存储服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioContractFileStorageServiceImpl implements ContractFileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上传本地文件。
     *
     * @param objectKey 对象Key
     * @param file 本地文件
     */
    @Override
    public void upload(String objectKey, File file) {
        try {
            String contentType = getContentType(file.getName());
            try (InputStream inputStream = FileUtil.getInputStream(file)) {
                upload(objectKey, inputStream, FileUtil.size(file), contentType);
            }
        } catch (Exception e) {
            log.error("上传合同文件失败，objectKey={}", objectKey, e);
            throw new IllegalStateException("上传合同文件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 上传输入流。
     *
     * @param objectKey 对象Key
     * @param inputStream 输入流
     * @param size 文件大小
     * @param contentType 文件类型
     */
    @Override
    public void upload(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(StrUtil.blankToDefault(contentType, "application/octet-stream"))
                    .build());

            log.info("合同文件上传MinIO成功，bucket={}，objectKey={}",
                    minioProperties.getBucketName(), objectKey);
        } catch (Exception e) {
            log.error("合同文件上传MinIO失败，objectKey={}", objectKey, e);
            throw new IllegalStateException("合同文件上传MinIO失败：" + e.getMessage(), e);
        }
    }

    /**
     * 下载文件到本地。
     *
     * @param objectKey 对象Key
     * @param targetFile 目标文件
     */
    @Override
    public void download(String objectKey, File targetFile) {
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .object(objectKey)
                .build());
             OutputStream outputStream = FileUtil.getOutputStream(targetFile)) {
            inputStream.transferTo(outputStream);
            log.info("合同文件下载成功，objectKey={}，targetFile={}", objectKey, targetFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("合同文件下载失败，objectKey={}", objectKey, e);
            throw new IllegalStateException("合同文件下载失败：" + e.getMessage(), e);
        }
    }

    /**
     * 确保存储桶存在。
     */
    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioProperties.getBucketName())
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .build());
            log.info("MinIO存储桶创建成功，bucket={}", minioProperties.getBucketName());
        }
    }

    /**
     * 获取文件类型。
     *
     * @param fileName 文件名
     * @return 文件类型
     */
    private String getContentType(String fileName) {
        if (StrUtil.endWithIgnoreCase(fileName, ".pdf")) {
            return "application/pdf";
        }
        if (StrUtil.endWithIgnoreCase(fileName, ".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }
}
```

## 电子签章发起

电子签章发起负责将已生成的 PDF 提交到电子签平台，并创建本系统签署任务。真实项目中不建议在业务代码里直接调用某一家平台 SDK，应通过 `EsignClient` 抽象屏蔽平台差异。

核心流程：

```text
前端选择合同发起签署
-> 校验合同状态为 GENERATED
-> 创建本系统签署任务
-> 调用电子签平台创建签署流程
-> 保存 platform_flow_id 和 sign_url
-> 更新合同状态为 SIGNING
```

### 创建签署任务

签署任务请求参数主要包含合同 ID、签署人姓名、手机号、邮箱等信息。

文件位置：`src/main/java/io/github/atengk/contractsign/dto/StartSignDTO.java`

```java
package io.github.atengk.contractsign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起签署参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class StartSignDTO {

    /**
     * 合同ID。
     */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /**
     * 签署人姓名。
     */
    @NotBlank(message = "签署人姓名不能为空")
    private String signerName;

    /**
     * 签署人手机号。
     */
    private String signerMobile;

    /**
     * 签署人邮箱。
     */
    private String signerEmail;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/vo/StartSignVO.java`

```java
package io.github.atengk.contractsign.vo;

import lombok.Data;

/**
 * 发起签署返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class StartSignVO {

    /**
     * 签署任务ID。
     */
    private Long signTaskId;

    /**
     * 签署任务号。
     */
    private String signTaskNo;

    /**
     * 合同编号。
     */
    private String contractNo;

    /**
     * 签署链接。
     */
    private String signUrl;

    /**
     * 签署状态。
     */
    private String status;
}
```

### 调用电子签平台接口

先定义一个电子签客户端接口。业务 Service 只依赖接口，不关心具体平台是 mock、e签宝、法大大还是其他电子签服务。

文件位置：`src/main/java/io/github/atengk/contractsign/client/EsignClient.java`

```java
package io.github.atengk.contractsign.client;

import io.github.atengk.contractsign.client.dto.EsignCreateFlowRequest;
import io.github.atengk.contractsign.client.dto.EsignCreateFlowResponse;

/**
 * 电子签平台客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface EsignClient {

    /**
     * 创建签署流程。
     *
     * @param request 创建签署流程请求
     * @return 创建签署流程响应
     */
    EsignCreateFlowResponse createSignFlow(EsignCreateFlowRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/client/dto/EsignCreateFlowRequest.java`

```java
package io.github.atengk.contractsign.client.dto;

import lombok.Data;

/**
 * 电子签创建流程请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class EsignCreateFlowRequest {

    /**
     * 本系统签署任务号。
     */
    private String signTaskNo;

    /**
     * 合同编号。
     */
    private String contractNo;

    /**
     * 合同标题。
     */
    private String contractTitle;

    /**
     * PDF文件objectKey。
     */
    private String pdfObjectKey;

    /**
     * 签署人姓名。
     */
    private String signerName;

    /**
     * 签署人手机号。
     */
    private String signerMobile;

    /**
     * 签署人邮箱。
     */
    private String signerEmail;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/client/dto/EsignCreateFlowResponse.java`

```java
package io.github.atengk.contractsign.client.dto;

import lombok.Data;

/**
 * 电子签创建流程响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class EsignCreateFlowResponse {

    /**
     * 第三方平台签署流程ID。
     */
    private String platformFlowId;

    /**
     * 签署链接。
     */
    private String signUrl;

    /**
     * 平台原始状态。
     */
    private String platformStatus;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/client/MockEsignClient.java`

下面的 Mock 实现用于本地打通流程。后续替换真实电子签平台时，只需要新增实现类并根据 `platform-code` 选择 Bean。

```java
package io.github.atengk.contractsign.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.contractsign.client.dto.EsignCreateFlowRequest;
import io.github.atengk.contractsign.client.dto.EsignCreateFlowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock电子签平台客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class MockEsignClient implements EsignClient {

    /**
     * 创建签署流程。
     *
     * @param request 创建签署流程请求
     * @return 创建签署流程响应
     */
    @Override
    public EsignCreateFlowResponse createSignFlow(EsignCreateFlowRequest request) {
        String platformFlowId = "MOCK_FLOW_" + IdUtil.fastSimpleUUID();
        String signUrl = StrUtil.format("http://127.0.0.1:8080/mock-sign?flowId={}&taskNo={}",
                platformFlowId, request.getSignTaskNo());

        log.info("Mock电子签创建签署流程成功，signTaskNo={}，platformFlowId={}",
                request.getSignTaskNo(), platformFlowId);

        EsignCreateFlowResponse response = new EsignCreateFlowResponse();
        response.setPlatformFlowId(platformFlowId);
        response.setSignUrl(signUrl);
        response.setPlatformStatus("WAIT_SIGN");
        return response;
    }
}
```

### 保存签署任务状态

签署任务保存需要同时更新 `t_contract_sign_task` 和 `t_contract_instance`。这里建议放在同一个本地事务中处理：签署任务创建成功后，合同状态从 `GENERATED` 更新为 `SIGNING`。

文件位置：`src/main/java/io/github/atengk/contractsign/service/ContractSignService.java`

```java
package io.github.atengk.contractsign.service;

import io.github.atengk.contractsign.dto.StartSignDTO;
import io.github.atengk.contractsign.vo.StartSignVO;

/**
 * 合同签署服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContractSignService {

    /**
     * 发起合同签署。
     *
     * @param dto 发起签署参数
     * @return 发起签署结果
     */
    StartSignVO startSign(StartSignDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/ContractSignServiceImpl.java`

下面的实现类完成合同状态校验、签署任务创建、调用电子签平台和状态更新。

```java
package io.github.atengk.contractsign.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.contractsign.client.EsignClient;
import io.github.atengk.contractsign.client.dto.EsignCreateFlowRequest;
import io.github.atengk.contractsign.client.dto.EsignCreateFlowResponse;
import io.github.atengk.contractsign.dto.StartSignDTO;
import io.github.atengk.contractsign.entity.ContractInstance;
import io.github.atengk.contractsign.entity.ContractSignTask;
import io.github.atengk.contractsign.mapper.ContractInstanceMapper;
import io.github.atengk.contractsign.mapper.ContractSignTaskMapper;
import io.github.atengk.contractsign.service.ContractSignService;
import io.github.atengk.contractsign.vo.StartSignVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 合同签署服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractSignServiceImpl implements ContractSignService {

    private final ContractInstanceMapper contractInstanceMapper;
    private final ContractSignTaskMapper contractSignTaskMapper;
    private final EsignClient esignClient;

    /**
     * 发起合同签署。
     *
     * @param dto 发起签署参数
     * @return 发起签署结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartSignVO startSign(StartSignDTO dto) {
        ContractInstance contract = contractInstanceMapper.selectById(dto.getContractId());
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }
        if (!StrUtil.equals(contract.getStatus(), "GENERATED")) {
            throw new IllegalStateException("当前合同状态不允许发起签署：" + contract.getStatus());
        }
        if (StrUtil.isBlank(contract.getPdfObjectKey())) {
            throw new IllegalStateException("合同PDF文件不存在，不能发起签署");
        }

        String signTaskNo = buildSignTaskNo(contract.getContractNo());

        ContractSignTask signTask = new ContractSignTask();
        signTask.setId(IdUtil.getSnowflakeNextId());
        signTask.setSignTaskNo(signTaskNo);
        signTask.setContractId(contract.getId());
        signTask.setContractNo(contract.getContractNo());
        signTask.setPlatformCode("MOCK");
        signTask.setSignerName(dto.getSignerName());
        signTask.setSignerMobile(dto.getSignerMobile());
        signTask.setSignerEmail(dto.getSignerEmail());
        signTask.setStatus("INIT");
        contractSignTaskMapper.insert(signTask);

        EsignCreateFlowRequest request = new EsignCreateFlowRequest();
        request.setSignTaskNo(signTaskNo);
        request.setContractNo(contract.getContractNo());
        request.setContractTitle(contract.getContractTitle());
        request.setPdfObjectKey(contract.getPdfObjectKey());
        request.setSignerName(dto.getSignerName());
        request.setSignerMobile(dto.getSignerMobile());
        request.setSignerEmail(dto.getSignerEmail());

        EsignCreateFlowResponse response = esignClient.createSignFlow(request);

        signTask.setPlatformFlowId(response.getPlatformFlowId());
        signTask.setSignUrl(response.getSignUrl());
        signTask.setPlatformStatus(response.getPlatformStatus());
        signTask.setStatus("WAIT_SIGN");
        signTask.setSubmittedAt(new Date());
        contractSignTaskMapper.updateById(signTask);

        contract.setStatus("SIGNING");
        contractInstanceMapper.updateById(contract);

        log.info("合同发起签署成功，contractNo={}，signTaskNo={}，platformFlowId={}",
                contract.getContractNo(), signTaskNo, response.getPlatformFlowId());

        StartSignVO vo = new StartSignVO();
        vo.setSignTaskId(signTask.getId());
        vo.setSignTaskNo(signTaskNo);
        vo.setContractNo(contract.getContractNo());
        vo.setSignUrl(response.getSignUrl());
        vo.setStatus(signTask.getStatus());
        return vo;
    }

    /**
     * 构建签署任务号。
     *
     * @param contractNo 合同编号
     * @return 签署任务号
     */
    private String buildSignTaskNo(String contractNo) {
        return StrUtil.format("SIGN_{}_{}", contractNo, DateUtil.current());
    }
}
```

发起签署接口可以放在合同业务 Controller 中。

文件位置：`src/main/java/io/github/atengk/contractsign/controller/ContractWorkflowController.java`

下面的 Controller 提供合同生成和发起签署两个接口。

```java
package io.github.atengk.contractsign.controller;

import io.github.atengk.contractsign.dto.ContractGenerateDTO;
import io.github.atengk.contractsign.dto.StartSignDTO;
import io.github.atengk.contractsign.service.ContractGenerateService;
import io.github.atengk.contractsign.service.ContractSignService;
import io.github.atengk.contractsign.vo.ContractGenerateVO;
import io.github.atengk.contractsign.vo.StartSignVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 合同业务流程接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")
public class ContractWorkflowController {

    private final ContractGenerateService contractGenerateService;
    private final ContractSignService contractSignService;

    /**
     * 生成合同。
     *
     * @param dto 生成参数
     * @return 生成结果
     */
    @PostMapping("/generate")
    public ContractGenerateVO generate(@Valid @RequestBody ContractGenerateDTO dto) {
        return contractGenerateService.generate(dto);
    }

    /**
     * 发起签署。
     *
     * @param dto 发起签署参数
     * @return 发起签署结果
     */
    @PostMapping("/sign/start")
    public StartSignVO startSign(@Valid @RequestBody StartSignDTO dto) {
        return contractSignService.startSign(dto);
    }
}
```

发起签署请求示例：

```bash
curl -X POST 'http://localhost:8080/api/contracts/sign/start' \
  -H 'Content-Type: application/json' \
  -d '{
    "contractId": 10001,
    "signerName": "张三",
    "signerMobile": "13800000000",
    "signerEmail": "zhangsan@example.com"
  }'
```

响应示例：

```json
{
  "signTaskId": 10002,
  "signTaskNo": "SIGN_HTPURCHASE202605150001_1780000000000",
  "contractNo": "HTPURCHASE202605150001",
  "signUrl": "http://127.0.0.1:8080/mock-sign?flowId=MOCK_FLOW_xxx&taskNo=SIGN_xxx",
  "status": "WAIT_SIGN"
}
```

当前阶段完成后，主链路已经可以走到：

```text
上传模板
-> 生成合同 Word
-> 转换 PDF
-> 上传 MinIO
-> 创建签署任务
-> 获取签署链接
```

下一步需要继续实现「签署回调处理」，重点包括回调验签、回调幂等、签署状态更新和已签署文件归档。

## 签署回调处理

签署回调处理用于接收电子签平台推送的签署结果，并完成验签、幂等、状态更新和已签署文件归档。这个链路不能只更新状态，必须把平台回调原文、验签结果、处理结果和归档文件摘要都保存下来，便于后续审计和问题追踪。该部分对应原场景中的「回调签署结果」和「归档合同文件」能力。

推荐回调处理顺序如下：

```text
接收电子签平台回调
-> 保存回调原始报文
-> 根据 eventId 做幂等控制
-> 校验回调签名
-> 查询签署任务
-> 判断状态是否允许流转
-> 更新签署任务为已签署
-> 下载已签署PDF
-> 上传已签署PDF到MinIO
-> 更新合同实例为已归档
-> 更新回调记录为处理成功
```

### 回调参数验签

回调验签的目标是确认请求确实来自电子签平台，避免外部伪造「签署完成」事件。不同平台验签规则不同，本案例先使用 `HmacSHA256` 模拟电子签平台回调签名。

Mock 回调请求示例：

```json
{
  "platformCode": "MOCK",
  "eventId": "EVT_202605150001",
  "eventType": "SIGN_COMPLETED",
  "platformFlowId": "MOCK_FLOW_abc123",
  "signTaskNo": "SIGN_HTPURCHASE202605150001_1780000000000",
  "contractNo": "HTPURCHASE202605150001",
  "platformStatus": "SIGNED",
  "signedFileUrl": "mock://signed-file/MOCK_FLOW_abc123",
  "callbackTime": "2026-05-15 10:30:00"
}
```

签名请求头示例：

```text
X-Esign-Signature: 2d9f0b8b6f7a...
```

电子签客户端接口需要补充「验签」和「下载已签署文件」能力。

文件位置：`src/main/java/io/github/atengk/contractsign/client/EsignClient.java`

下面的接口在之前创建签署流程的基础上，增加回调验签和下载已签署文件两个方法。

```java
package io.github.atengk.contractsign.client;

import io.github.atengk.contractsign.client.dto.EsignCreateFlowRequest;
import io.github.atengk.contractsign.client.dto.EsignCreateFlowResponse;

import java.io.File;

/**
 * 电子签平台客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface EsignClient {

    /**
     * 创建签署流程。
     *
     * @param request 创建签署流程请求
     * @return 创建签署流程响应
     */
    EsignCreateFlowResponse createSignFlow(EsignCreateFlowRequest request);

    /**
     * 校验回调签名。
     *
     * @param body 回调原始请求体
     * @param signature 请求签名
     * @return 是否验签通过
     */
    Boolean verifyCallbackSign(String body, String signature);

    /**
     * 下载已签署文件。
     *
     * @param platformFlowId 平台签署流程ID
     * @param targetFile 目标文件
     */
    void downloadSignedFile(String platformFlowId, File targetFile);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/config/EsignProperties.java`

下面的配置类读取电子签平台配置，Mock 验签会使用 `callbackSecret` 生成 HMAC 签名。

```java
package io.github.atengk.contractsign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 电子签平台配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "contract.esign")
public class EsignProperties {

    /**
     * 平台编码。
     */
    private String platformCode;

    /**
     * 平台接口地址。
     */
    private String endpoint;

    /**
     * 应用ID。
     */
    private String appId;

    /**
     * 应用密钥。
     */
    private String appSecret;

    /**
     * 回调验签密钥。
     */
    private String callbackSecret;

    /**
     * 回调地址。
     */
    private String callbackUrl;

    /**
     * 签署链接过期小时数。
     */
    private Integer signUrlExpireHours;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/client/MockEsignClient.java`

下面是 Mock 电子签客户端的完整实现，包含创建签署流程、回调验签和模拟下载已签署文件。

```java
package io.github.atengk.contractsign.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.github.atengk.contractsign.client.dto.EsignCreateFlowRequest;
import io.github.atengk.contractsign.client.dto.EsignCreateFlowResponse;
import io.github.atengk.contractsign.config.EsignProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Mock电子签平台客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockEsignClient implements EsignClient {

    private final EsignProperties esignProperties;

    /**
     * 创建签署流程。
     *
     * @param request 创建签署流程请求
     * @return 创建签署流程响应
     */
    @Override
    public EsignCreateFlowResponse createSignFlow(EsignCreateFlowRequest request) {
        String platformFlowId = "MOCK_FLOW_" + IdUtil.fastSimpleUUID();
        String signUrl = StrUtil.format("http://127.0.0.1:8080/mock-sign?flowId={}&taskNo={}",
                platformFlowId, request.getSignTaskNo());

        log.info("Mock电子签创建签署流程成功，signTaskNo={}，platformFlowId={}",
                request.getSignTaskNo(), platformFlowId);

        EsignCreateFlowResponse response = new EsignCreateFlowResponse();
        response.setPlatformFlowId(platformFlowId);
        response.setSignUrl(signUrl);
        response.setPlatformStatus("WAIT_SIGN");
        return response;
    }

    /**
     * 校验回调签名。
     *
     * @param body 回调原始请求体
     * @param signature 请求签名
     * @return 是否验签通过
     */
    @Override
    public Boolean verifyCallbackSign(String body, String signature) {
        if (StrUtil.hasBlank(body, signature)) {
            log.warn("电子签回调验签失败，请求体或签名为空");
            return false;
        }

        String localSignature = SecureUtil.hmacSha256(
                esignProperties.getCallbackSecret().getBytes(StandardCharsets.UTF_8)
        ).digestHex(body);

        boolean verified = StrUtil.equalsIgnoreCase(localSignature, signature);
        if (!verified) {
            log.warn("电子签回调验签不通过，localSignature={}，requestSignature={}", localSignature, signature);
        }
        return verified;
    }

    /**
     * 下载已签署文件。
     *
     * @param platformFlowId 平台签署流程ID
     * @param targetFile 目标文件
     */
    @Override
    public void downloadSignedFile(String platformFlowId, File targetFile) {
        if (StrUtil.isBlank(platformFlowId)) {
            throw new IllegalArgumentException("平台签署流程ID不能为空");
        }

        String mockPdfContent = """
                %PDF-1.4
                1 0 obj
                << /Type /Catalog /Pages 2 0 R >>
                endobj
                2 0 obj
                << /Type /Pages /Kids [3 0 R] /Count 1 >>
                endobj
                3 0 obj
                << /Type /Page /Parent 2 0 R /MediaBox [0 0 300 144] >>
                endobj
                trailer
                << /Root 1 0 R >>
                %%EOF
                """;

        FileUtil.writeUtf8String(mockPdfContent, targetFile);
        log.info("Mock电子签已签署文件下载成功，platformFlowId={}，targetFile={}",
                platformFlowId, targetFile.getAbsolutePath());
    }
}
```

生成 Mock 签名时，可以用下面这段临时代码或单元测试生成 `X-Esign-Signature`：

```java
String signature = SecureUtil.hmacSha256("demo-callback-secret".getBytes(StandardCharsets.UTF_8))
        .digestHex(callbackBody);
```

### 回调幂等控制

电子签平台的回调可能重复发送，所以必须做幂等控制。本案例使用 `platform_code + event_id` 唯一索引控制重复回调。

幂等规则：

| 场景                     | 处理方式                             |
| ------------------------ | ------------------------------------ |
| 首次收到回调             | 插入回调记录并继续处理               |
| 重复 eventId             | 不再重复更新状态，直接返回成功       |
| 同一签署流程重复完成事件 | 如果任务已是 `SIGNED`，直接忽略      |
| 验签失败                 | 记录失败，不更新合同和签署任务       |
| 未找到签署任务           | 记录为 `IGNORED`，不抛出业务状态变更 |

文件位置：`src/main/java/io/github/atengk/contractsign/service/ContractSignCallbackService.java`

```java
package io.github.atengk.contractsign.service;

/**
 * 合同签署回调服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContractSignCallbackService {

    /**
     * 处理电子签平台回调。
     *
     * @param body 回调原始请求体
     * @param signature 回调签名
     */
    void handleCallback(String body, String signature);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/ContractSignCallbackServiceImpl.java`

下面的实现类完成回调落库、幂等判断、验签、状态更新和已签署文件归档。代码默认已经存在 `ContractSignCallback`、`ContractSignTask`、`ContractInstance` 实体和对应 Mapper。

```java
package io.github.atengk.contractsign.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.contractsign.client.EsignClient;
import io.github.atengk.contractsign.entity.ContractInstance;
import io.github.atengk.contractsign.entity.ContractSignCallback;
import io.github.atengk.contractsign.entity.ContractSignTask;
import io.github.atengk.contractsign.mapper.ContractInstanceMapper;
import io.github.atengk.contractsign.mapper.ContractSignCallbackMapper;
import io.github.atengk.contractsign.mapper.ContractSignTaskMapper;
import io.github.atengk.contractsign.service.ContractFileStorageService;
import io.github.atengk.contractsign.service.ContractSignCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Date;

/**
 * 合同签署回调服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractSignCallbackServiceImpl implements ContractSignCallbackService {

    private final EsignClient esignClient;
    private final ContractSignTaskMapper contractSignTaskMapper;
    private final ContractInstanceMapper contractInstanceMapper;
    private final ContractSignCallbackMapper contractSignCallbackMapper;
    private final ContractFileStorageService contractFileStorageService;

    /**
     * 处理电子签平台回调。
     *
     * @param body 回调原始请求体
     * @param signature 回调签名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(String body, String signature) {
        if (StrUtil.isBlank(body)) {
            throw new IllegalArgumentException("回调请求体不能为空");
        }

        JSONObject jsonObject = JSONUtil.parseObj(body);
        String platformCode = StrUtil.blankToDefault(jsonObject.getStr("platformCode"), "MOCK");
        String eventId = StrUtil.blankToDefault(jsonObject.getStr("eventId"), DigestUtil.sha256Hex(body));
        String platformFlowId = jsonObject.getStr("platformFlowId");
        String signTaskNo = jsonObject.getStr("signTaskNo");
        String contractNo = jsonObject.getStr("contractNo");
        String eventType = jsonObject.getStr("eventType");

        ContractSignCallback callback = buildCallbackRecord(
                platformCode, eventId, platformFlowId, signTaskNo, contractNo, eventType, body, signature
        );

        boolean inserted = saveCallbackRecord(callback);
        if (!inserted) {
            log.info("电子签回调重复通知，platformCode={}，eventId={}", platformCode, eventId);
            return;
        }

        boolean verified = esignClient.verifyCallbackSign(body, signature);
        callback.setVerifyResult(verified ? 1 : 0);

        if (!verified) {
            callback.setHandleStatus("FAILED");
            callback.setHandleMessage("回调验签失败");
            callback.setHandledAt(new Date());
            contractSignCallbackMapper.updateById(callback);
            log.warn("电子签回调验签失败，platformCode={}，eventId={}", platformCode, eventId);
            return;
        }

        ContractSignTask signTask = findSignTask(platformFlowId, signTaskNo);
        if (signTask == null) {
            callback.setHandleStatus("IGNORED");
            callback.setHandleMessage("未找到签署任务");
            callback.setHandledAt(new Date());
            contractSignCallbackMapper.updateById(callback);
            log.warn("电子签回调未找到签署任务，platformFlowId={}，signTaskNo={}", platformFlowId, signTaskNo);
            return;
        }

        if (!StrUtil.equals(eventType, "SIGN_COMPLETED")) {
            callback.setHandleStatus("IGNORED");
            callback.setHandleMessage("非签署完成事件，已忽略");
            callback.setHandledAt(new Date());
            contractSignCallbackMapper.updateById(callback);
            log.info("电子签回调事件已忽略，eventType={}，signTaskNo={}", eventType, signTask.getSignTaskNo());
            return;
        }

        if (StrUtil.equalsAny(signTask.getStatus(), "SIGNED", "REJECTED", "EXPIRED")) {
            callback.setHandleStatus("IGNORED");
            callback.setHandleMessage("签署任务已处于终态，重复事件忽略");
            callback.setHandledAt(new Date());
            contractSignCallbackMapper.updateById(callback);
            log.info("签署任务已处于终态，signTaskNo={}，status={}", signTask.getSignTaskNo(), signTask.getStatus());
            return;
        }

        archiveSignedFile(signTask, callback);

        callback.setHandleStatus("SUCCESS");
        callback.setHandleMessage("签署完成回调处理成功");
        callback.setHandledAt(new Date());
        contractSignCallbackMapper.updateById(callback);

        log.info("电子签回调处理成功，contractNo={}，signTaskNo={}，platformFlowId={}",
                signTask.getContractNo(), signTask.getSignTaskNo(), signTask.getPlatformFlowId());
    }

    /**
     * 构建回调记录。
     *
     * @param platformCode 平台编码
     * @param eventId 事件ID
     * @param platformFlowId 平台流程ID
     * @param signTaskNo 签署任务号
     * @param contractNo 合同编号
     * @param eventType 事件类型
     * @param body 回调原文
     * @param signature 回调签名
     * @return 回调记录
     */
    private ContractSignCallback buildCallbackRecord(String platformCode,
                                                     String eventId,
                                                     String platformFlowId,
                                                     String signTaskNo,
                                                     String contractNo,
                                                     String eventType,
                                                     String body,
                                                     String signature) {
        ContractSignCallback callback = new ContractSignCallback();
        callback.setId(IdUtil.getSnowflakeNextId());
        callback.setPlatformCode(platformCode);
        callback.setEventId(eventId);
        callback.setPlatformFlowId(platformFlowId);
        callback.setSignTaskNo(signTaskNo);
        callback.setContractNo(contractNo);
        callback.setEventType(eventType);
        callback.setCallbackBody(body);
        callback.setSignature(signature);
        callback.setVerifyResult(0);
        callback.setHandleStatus("INIT");
        callback.setCallbackAt(new Date());
        return callback;
    }

    /**
     * 保存回调记录。
     *
     * @param callback 回调记录
     * @return 是否首次保存
     */
    private boolean saveCallbackRecord(ContractSignCallback callback) {
        try {
            contractSignCallbackMapper.insert(callback);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /**
     * 查询签署任务。
     *
     * @param platformFlowId 平台流程ID
     * @param signTaskNo 签署任务号
     * @return 签署任务
     */
    private ContractSignTask findSignTask(String platformFlowId, String signTaskNo) {
        LambdaQueryWrapper<ContractSignTask> wrapper = new LambdaQueryWrapper<ContractSignTask>()
                .eq(StrUtil.isNotBlank(platformFlowId), ContractSignTask::getPlatformFlowId, platformFlowId)
                .or(StrUtil.isNotBlank(signTaskNo), query -> query.eq(ContractSignTask::getSignTaskNo, signTaskNo))
                .last("LIMIT 1");
        return contractSignTaskMapper.selectOne(wrapper);
    }

    /**
     * 归档已签署文件并更新状态。
     *
     * @param signTask 签署任务
     * @param callback 回调记录
     */
    private void archiveSignedFile(ContractSignTask signTask, ContractSignCallback callback) {
        ContractInstance contract = contractInstanceMapper.selectById(signTask.getContractId());
        if (contract == null) {
            throw new IllegalStateException("合同实例不存在，contractId=" + signTask.getContractId());
        }

        File signedFile = FileUtil.createTempFile("signed-", ".pdf", true);
        try {
            esignClient.downloadSignedFile(signTask.getPlatformFlowId(), signedFile);

            String signedObjectKey = buildSignedObjectKey(contract);
            String signedSha256 = DigestUtil.sha256Hex(signedFile);
            long signedFileSize = FileUtil.size(signedFile);

            contractFileStorageService.upload(signedObjectKey, signedFile);

            signTask.setStatus("SIGNED");
            signTask.setPlatformStatus("SIGNED");
            signTask.setSignedAt(new Date());
            contractSignTaskMapper.updateById(signTask);

            contract.setSignedPdfObjectKey(signedObjectKey);
            contract.setSignedPdfSha256(signedSha256);
            contract.setSignedFileSize(signedFileSize);
            contract.setStatus("ARCHIVED");
            contractInstanceMapper.updateById(contract);

            callback.setContractNo(contract.getContractNo());

            log.info("已签署合同归档成功，contractNo={}，signedObjectKey={}，sha256={}",
                    contract.getContractNo(), signedObjectKey, signedSha256);
        } finally {
            FileUtil.del(signedFile);
        }
    }

    /**
     * 构建已签署文件objectKey。
     *
     * @param contract 合同实例
     * @return 已签署文件objectKey
     */
    private String buildSignedObjectKey(ContractInstance contract) {
        if (StrUtil.isNotBlank(contract.getPdfObjectKey())
                && StrUtil.endWith(contract.getPdfObjectKey(), "/preview.pdf")) {
            return StrUtil.replace(contract.getPdfObjectKey(), "/preview.pdf", "/signed.pdf");
        }

        String datePath = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
        return StrUtil.format("contract-instance/{}/{}/signed.pdf", datePath, contract.getContractNo());
    }
}
```

### 签署状态更新

状态更新必须遵守「状态不可逆」规则。签署完成、拒签、过期这类终态不能被低优先级状态覆盖。

推荐状态流转规则：

| 当前状态    | 回调事件         | 目标状态   | 是否允许 |
| ----------- | ---------------- | ---------- | -------- |
| `WAIT_SIGN` | `SIGN_COMPLETED` | `SIGNED`   | 允许     |
| `SIGNED`    | `SIGN_COMPLETED` | `SIGNED`   | 忽略     |
| `REJECTED`  | `SIGN_COMPLETED` | 不变       | 不允许   |
| `EXPIRED`   | `SIGN_COMPLETED` | 不变       | 不允许   |
| `WAIT_SIGN` | `SIGN_REJECTED`  | `REJECTED` | 可扩展   |
| `WAIT_SIGN` | `SIGN_EXPIRED`   | `EXPIRED`  | 可扩展   |

合同实例状态建议：

```text
GENERATED
-> SIGNING
-> ARCHIVED
```

如果需要更细，可以拆成：

```text
GENERATED
-> SIGNING
-> SIGNED
-> ARCHIVED
```

本案例在收到签署完成回调后，直接完成归档，所以合同实例最终状态更新为 `ARCHIVED`。

### 已签署文件归档

已签署文件归档不要长期依赖电子签平台的文件下载地址。平台文件地址可能过期，也可能需要鉴权。业务系统应在收到签署完成回调后，主动下载已签署 PDF，并上传到自己的 MinIO。

归档后需要保存：

| 内容           | 字段                      |
| -------------- | ------------------------- |
| 已签署文件路径 | `signed_pdf_object_key`   |
| 已签署文件摘要 | `signed_pdf_sha256`       |
| 已签署文件大小 | `signed_file_size`        |
| 合同状态       | `ARCHIVED`                |
| 签署完成时间   | `signed_at`               |
| 回调处理结果   | `handle_status = SUCCESS` |

归档验证方式：

```text
查询 t_contract_instance
-> signed_pdf_object_key 不为空
-> signed_pdf_sha256 不为空
-> status = ARCHIVED

查询 t_contract_sign_task
-> status = SIGNED
-> signed_at 不为空

查询 t_contract_sign_callback
-> verify_result = 1
-> handle_status = SUCCESS
```

## 接口设计

接口设计围绕模板维护、合同生成、签署发起、回调接收和详情查询展开。后台管理端通常调用上传模板接口；业务系统调用生成合同和发起签署接口；电子签平台调用签署回调接口；前端详情页调用查询合同详情接口。

### 上传模板接口

该接口用于上传 `.docx` 合同模板，并自动解析模板变量。

| 项目         | 内容                             |
| ------------ | -------------------------------- |
| 接口路径     | `/api/contract/templates/upload` |
| 请求方式     | `POST`                           |
| Content-Type | `multipart/form-data`            |
| 说明         | 上传 Word 模板，生成新版本模板   |

请求参数：

| 参数           | 位置      | 必填 | 说明             |
| -------------- | --------- | ---- | ---------------- |
| `templateCode` | form-data | 是   | 模板编码         |
| `templateName` | form-data | 是   | 模板名称         |
| `file`         | form-data | 是   | `.docx` 模板文件 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/contract/templates/upload' \
  -F 'templateCode=PURCHASE_CONTRACT' \
  -F 'templateName=采购合同模板' \
  -F 'file=@/data/templates/purchase-contract.docx'
```

响应示例：

```json
{
  "templateId": 1780000000000000001,
  "templateCode": "PURCHASE_CONTRACT",
  "templateName": "采购合同模板",
  "versionNo": 1,
  "variables": [
    {
      "key": "partyAName",
      "name": "partyAName",
      "required": true
    },
    {
      "key": "partyBName",
      "name": "partyBName",
      "required": true
    },
    {
      "key": "contractAmount",
      "name": "contractAmount",
      "required": true
    }
  ]
}
```

### 生成合同接口

该接口用于根据模板编码和变量数据生成合同文件，最终输出未签署 Word 和 PDF，并把文件上传到 MinIO。

| 项目         | 内容                         |
| ------------ | ---------------------------- |
| 接口路径     | `/api/contracts/generate`    |
| 请求方式     | `POST`                       |
| Content-Type | `application/json`           |
| 说明         | 根据模板生成合同 Word 和 PDF |

请求参数：

| 参数            | 类型   | 必填 | 说明         |
| --------------- | ------ | ---- | ------------ |
| `templateCode`  | string | 是   | 模板编码     |
| `businessType`  | string | 是   | 业务类型     |
| `businessId`    | string | 是   | 业务主键     |
| `contractTitle` | string | 是   | 合同标题     |
| `variables`     | object | 是   | 模板变量键值 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/contracts/generate' \
  -H 'Content-Type: application/json' \
  -d '{
    "templateCode": "PURCHASE_CONTRACT",
    "businessType": "PURCHASE",
    "businessId": "PO202605150001",
    "contractTitle": "采购合同-PO202605150001",
    "variables": {
      "partyAName": "杭州示例科技有限公司",
      "partyBName": "上海测试信息技术有限公司",
      "contractNo": "HT202605150001",
      "contractAmount": "100000.00",
      "signDate": "2026-05-15"
    }
  }'
```

响应示例：

```json
{
  "contractId": 1780000000000000002,
  "contractNo": "HTPURCHASE202605150001",
  "contractTitle": "采购合同-PO202605150001",
  "pdfObjectKey": "contract-instance/20260515/HTPURCHASE202605150001/preview.pdf",
  "pdfSha256": "1f4d6b0f6c9b8e5a...",
  "status": "GENERATED"
}
```

### 发起签署接口

该接口用于对已经生成 PDF 的合同发起电子签署流程，返回签署链接给前端。

| 项目         | 内容                         |
| ------------ | ---------------------------- |
| 接口路径     | `/api/contracts/sign/start`  |
| 请求方式     | `POST`                       |
| Content-Type | `application/json`           |
| 说明         | 创建签署任务并调用电子签平台 |

请求参数：

| 参数           | 类型   | 必填 | 说明         |
| -------------- | ------ | ---- | ------------ |
| `contractId`   | number | 是   | 合同实例 ID  |
| `signerName`   | string | 是   | 签署人姓名   |
| `signerMobile` | string | 否   | 签署人手机号 |
| `signerEmail`  | string | 否   | 签署人邮箱   |

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/contracts/sign/start' \
  -H 'Content-Type: application/json' \
  -d '{
    "contractId": 1780000000000000002,
    "signerName": "张三",
    "signerMobile": "13800000000",
    "signerEmail": "zhangsan@example.com"
  }'
```

响应示例：

```json
{
  "signTaskId": 1780000000000000003,
  "signTaskNo": "SIGN_HTPURCHASE202605150001_1780000000000",
  "contractNo": "HTPURCHASE202605150001",
  "signUrl": "http://127.0.0.1:8080/mock-sign?flowId=MOCK_FLOW_abc123&taskNo=SIGN_HTPURCHASE202605150001_1780000000000",
  "status": "WAIT_SIGN"
}
```

### 签署回调接口

该接口由电子签平台调用。业务系统接收到回调后，需要先保存原始报文，再做验签、幂等和状态更新。

| 项目         | 内容                           |
| ------------ | ------------------------------ |
| 接口路径     | `/api/contracts/sign/callback` |
| 请求方式     | `POST`                         |
| Content-Type | `application/json`             |
| 说明         | 接收电子签平台签署结果回调     |

文件位置：`src/main/java/io/github/atengk/contractsign/controller/ContractSignCallbackController.java`

下面的 Controller 接收电子签回调原文和签名请求头，并交给回调服务处理。

```java
package io.github.atengk.contractsign.controller;

import io.github.atengk.contractsign.service.ContractSignCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同签署回调接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts/sign")
public class ContractSignCallbackController {

    private final ContractSignCallbackService contractSignCallbackService;

    /**
     * 接收电子签平台回调。
     *
     * @param signature 回调签名
     * @param body 回调原始请求体
     * @return 平台要求的成功响应
     */
    @PostMapping("/callback")
    public String callback(@RequestHeader(value = "X-Esign-Signature", required = false) String signature,
                           @RequestBody String body) {
        contractSignCallbackService.handleCallback(body, signature);
        return "success";
    }
}
```

请求示例：

```bash
BODY='{
  "platformCode": "MOCK",
  "eventId": "EVT_202605150001",
  "eventType": "SIGN_COMPLETED",
  "platformFlowId": "MOCK_FLOW_abc123",
  "signTaskNo": "SIGN_HTPURCHASE202605150001_1780000000000",
  "contractNo": "HTPURCHASE202605150001",
  "platformStatus": "SIGNED",
  "signedFileUrl": "mock://signed-file/MOCK_FLOW_abc123",
  "callbackTime": "2026-05-15 10:30:00"
}'

SIGNATURE='这里填HmacSHA256签名'

curl -X POST 'http://localhost:8080/api/contracts/sign/callback' \
  -H 'Content-Type: application/json' \
  -H "X-Esign-Signature: ${SIGNATURE}" \
  -d "${BODY}"
```

成功响应：

```text
success
```

回调处理后应验证数据库：

```sql
-- 查看合同是否归档
SELECT contract_no, status, signed_pdf_object_key, signed_pdf_sha256
FROM t_contract_instance
WHERE contract_no = 'HTPURCHASE202605150001';

-- 查看签署任务是否完成
SELECT sign_task_no, status, platform_flow_id, signed_at
FROM t_contract_sign_task
WHERE contract_no = 'HTPURCHASE202605150001';

-- 查看回调是否处理成功
SELECT event_id, verify_result, handle_status, handle_message
FROM t_contract_sign_callback
WHERE contract_no = 'HTPURCHASE202605150001';
```

### 查询合同详情接口

查询合同详情用于前端展示合同基础信息、生成文件、签署状态和归档文件。这里建议只返回 `objectKey` 或后端生成的临时访问地址，不返回 MinIO 永久地址。

文件位置：`src/main/java/io/github/atengk/contractsign/vo/ContractDetailVO.java`

下面的 VO 用于聚合合同实例和签署任务信息。

```java
package io.github.atengk.contractsign.vo;

import lombok.Data;

import java.util.Date;

/**
 * 合同详情返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ContractDetailVO {

    /**
     * 合同ID。
     */
    private Long contractId;

    /**
     * 合同编号。
     */
    private String contractNo;

    /**
     * 合同标题。
     */
    private String contractTitle;

    /**
     * 模板编码。
     */
    private String templateCode;

    /**
     * 模板版本。
     */
    private Integer templateVersion;

    /**
     * 业务类型。
     */
    private String businessType;

    /**
     * 业务ID。
     */
    private String businessId;

    /**
     * 未签署PDF文件objectKey。
     */
    private String pdfObjectKey;

    /**
     * 已签署PDF文件objectKey。
     */
    private String signedPdfObjectKey;

    /**
     * 未签署PDF摘要。
     */
    private String pdfSha256;

    /**
     * 已签署PDF摘要。
     */
    private String signedPdfSha256;

    /**
     * 合同状态。
     */
    private String contractStatus;

    /**
     * 签署任务号。
     */
    private String signTaskNo;

    /**
     * 签署链接。
     */
    private String signUrl;

    /**
     * 签署状态。
     */
    private String signStatus;

    /**
     * 签署完成时间。
     */
    private Date signedAt;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/ContractQueryService.java`

```java
package io.github.atengk.contractsign.service;

import io.github.atengk.contractsign.vo.ContractDetailVO;

/**
 * 合同查询服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContractQueryService {

    /**
     * 查询合同详情。
     *
     * @param contractId 合同ID
     * @return 合同详情
     */
    ContractDetailVO detail(Long contractId);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/ContractQueryServiceImpl.java`

下面的实现类查询合同实例和签署任务，并组装详情返回对象。

```java
package io.github.atengk.contractsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.contractsign.entity.ContractInstance;
import io.github.atengk.contractsign.entity.ContractSignTask;
import io.github.atengk.contractsign.mapper.ContractInstanceMapper;
import io.github.atengk.contractsign.mapper.ContractSignTaskMapper;
import io.github.atengk.contractsign.service.ContractQueryService;
import io.github.atengk.contractsign.vo.ContractDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 合同查询服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractQueryServiceImpl implements ContractQueryService {

    private final ContractInstanceMapper contractInstanceMapper;
    private final ContractSignTaskMapper contractSignTaskMapper;

    /**
     * 查询合同详情。
     *
     * @param contractId 合同ID
     * @return 合同详情
     */
    @Override
    public ContractDetailVO detail(Long contractId) {
        ContractInstance contract = contractInstanceMapper.selectById(contractId);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        ContractSignTask signTask = contractSignTaskMapper.selectOne(
                new LambdaQueryWrapper<ContractSignTask>()
                        .eq(ContractSignTask::getContractId, contractId)
                        .last("LIMIT 1")
        );

        ContractDetailVO vo = new ContractDetailVO();
        vo.setContractId(contract.getId());
        vo.setContractNo(contract.getContractNo());
        vo.setContractTitle(contract.getContractTitle());
        vo.setTemplateCode(contract.getTemplateCode());
        vo.setTemplateVersion(contract.getTemplateVersion());
        vo.setBusinessType(contract.getBusinessType());
        vo.setBusinessId(contract.getBusinessId());
        vo.setPdfObjectKey(contract.getPdfObjectKey());
        vo.setSignedPdfObjectKey(contract.getSignedPdfObjectKey());
        vo.setPdfSha256(contract.getPdfSha256());
        vo.setSignedPdfSha256(contract.getSignedPdfSha256());
        vo.setContractStatus(contract.getStatus());

        if (signTask != null) {
            vo.setSignTaskNo(signTask.getSignTaskNo());
            vo.setSignUrl(signTask.getSignUrl());
            vo.setSignStatus(signTask.getStatus());
            vo.setSignedAt(signTask.getSignedAt());
        }

        log.info("查询合同详情成功，contractId={}，contractNo={}", contractId, contract.getContractNo());
        return vo;
    }
}
```

将查询接口补充到已有的 `ContractWorkflowController` 中。

文件位置：`src/main/java/io/github/atengk/contractsign/controller/ContractWorkflowController.java`

下面是补充后的合同流程 Controller，包含生成合同、发起签署和查询详情接口。

```java
package io.github.atengk.contractsign.controller;

import io.github.atengk.contractsign.dto.ContractGenerateDTO;
import io.github.atengk.contractsign.dto.StartSignDTO;
import io.github.atengk.contractsign.service.ContractGenerateService;
import io.github.atengk.contractsign.service.ContractQueryService;
import io.github.atengk.contractsign.service.ContractSignService;
import io.github.atengk.contractsign.vo.ContractDetailVO;
import io.github.atengk.contractsign.vo.ContractGenerateVO;
import io.github.atengk.contractsign.vo.StartSignVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 合同业务流程接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")
public class ContractWorkflowController {

    private final ContractGenerateService contractGenerateService;
    private final ContractSignService contractSignService;
    private final ContractQueryService contractQueryService;

    /**
     * 生成合同。
     *
     * @param dto 生成参数
     * @return 生成结果
     */
    @PostMapping("/generate")
    public ContractGenerateVO generate(@Valid @RequestBody ContractGenerateDTO dto) {
        return contractGenerateService.generate(dto);
    }

    /**
     * 发起签署。
     *
     * @param dto 发起签署参数
     * @return 发起签署结果
     */
    @PostMapping("/sign/start")
    public StartSignVO startSign(@Valid @RequestBody StartSignDTO dto) {
        return contractSignService.startSign(dto);
    }

    /**
     * 查询合同详情。
     *
     * @param contractId 合同ID
     * @return 合同详情
     */
    @GetMapping("/{contractId}")
    public ContractDetailVO detail(@PathVariable Long contractId) {
        return contractQueryService.detail(contractId);
    }
}
```

查询合同详情请求示例：

```bash
curl -X GET 'http://localhost:8080/api/contracts/1780000000000000002'
```

响应示例：

```json
{
  "contractId": 1780000000000000002,
  "contractNo": "HTPURCHASE202605150001",
  "contractTitle": "采购合同-PO202605150001",
  "templateCode": "PURCHASE_CONTRACT",
  "templateVersion": 1,
  "businessType": "PURCHASE",
  "businessId": "PO202605150001",
  "pdfObjectKey": "contract-instance/20260515/HTPURCHASE202605150001/preview.pdf",
  "signedPdfObjectKey": "contract-instance/20260515/HTPURCHASE202605150001/signed.pdf",
  "pdfSha256": "1f4d6b0f6c9b8e5a...",
  "signedPdfSha256": "9a4c6b7e8f0a1c2d...",
  "contractStatus": "ARCHIVED",
  "signTaskNo": "SIGN_HTPURCHASE202605150001_1780000000000",
  "signUrl": "http://127.0.0.1:8080/mock-sign?flowId=MOCK_FLOW_abc123&taskNo=SIGN_xxx",
  "signStatus": "SIGNED",
  "signedAt": "2026-05-15T10:30:00.000+00:00"
}
```

到这里，合同签署主链路已经闭环：

```text
上传模板
-> 生成合同 Word/PDF
-> 上传 MinIO
-> 发起签署
-> 接收签署完成回调
-> 验签与幂等
-> 下载已签署 PDF
-> 上传 MinIO 归档
-> 查询合同详情
```

## 核心代码实现

这一部分补齐前面链路中引用到的核心实体、枚举、Mapper 和关键服务文件。完整主流程围绕「模板维护、变量填充、Word/PDF 生成、签署发起、回调归档」展开，与原始第 29 个场景中的核心能力保持一致。

建议核心文件结构如下：

```text
src/main/java/io/github/atengk/contractsign
├── client
│   ├── EsignClient.java
│   └── MockEsignClient.java
├── config
│   ├── EsignProperties.java
│   ├── LibreOfficeProperties.java
│   ├── MinioConfig.java
│   └── MinioProperties.java
├── controller
│   ├── ContractTemplateController.java
│   ├── ContractWorkflowController.java
│   └── ContractSignCallbackController.java
├── dto
│   ├── ContractGenerateDTO.java
│   ├── ContractTemplateUploadDTO.java
│   └── StartSignDTO.java
├── entity
│   ├── ContractTemplate.java
│   ├── ContractInstance.java
│   ├── ContractSignTask.java
│   └── ContractSignCallback.java
├── enums
│   ├── ContractStatusEnum.java
│   ├── SignTaskStatusEnum.java
│   └── CallbackHandleStatusEnum.java
├── mapper
│   ├── ContractTemplateMapper.java
│   ├── ContractInstanceMapper.java
│   ├── ContractSignTaskMapper.java
│   └── ContractSignCallbackMapper.java
├── service
└── service/impl
```

### 实体类与枚举

实体类与前面数据库表一一对应，JSON 字段先用 `String` 保存，实际项目也可以接入 MyBatis TypeHandler 转为对象。状态字段使用字符串保存，业务代码通过枚举维护可读性和状态约束。

文件位置：`src/main/java/io/github/atengk/contractsign/enums/ContractStatusEnum.java`

下面的枚举定义合同实例状态，包含是否终态的判断方法。

```java
package io.github.atengk.contractsign.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 合同状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum ContractStatusEnum {

    DRAFT("DRAFT", "草稿"),
    GENERATING("GENERATING", "生成中"),
    GENERATED("GENERATED", "已生成"),
    SIGNING("SIGNING", "签署中"),
    SIGNED("SIGNED", "已签署"),
    ARCHIVED("ARCHIVED", "已归档"),
    FAILED("FAILED", "处理失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    /**
     * 判断是否终态。
     *
     * @param code 状态编码
     * @return 是否终态
     */
    public static boolean isFinalStatus(String code) {
        return StrUtil.equalsAny(code, ARCHIVED.code, FAILED.code, CANCELLED.code);
    }
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/enums/SignTaskStatusEnum.java`

下面的枚举定义签署任务状态，用于控制回调状态不可逆。

```java
package io.github.atengk.contractsign.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 签署任务状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum SignTaskStatusEnum {

    INIT("INIT", "待发起"),
    SUBMITTED("SUBMITTED", "已提交"),
    WAIT_SIGN("WAIT_SIGN", "待签署"),
    SIGNED("SIGNED", "已签署"),
    REJECTED("REJECTED", "已拒签"),
    EXPIRED("EXPIRED", "已过期"),
    FAILED("FAILED", "签署失败");

    private final String code;
    private final String desc;

    /**
     * 判断是否终态。
     *
     * @param code 状态编码
     * @return 是否终态
     */
    public static boolean isFinalStatus(String code) {
        return StrUtil.equalsAny(code, SIGNED.code, REJECTED.code, EXPIRED.code, FAILED.code);
    }
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/enums/CallbackHandleStatusEnum.java`

下面的枚举定义回调记录处理状态。

```java
package io.github.atengk.contractsign.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 回调处理状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum CallbackHandleStatusEnum {

    INIT("INIT", "待处理"),
    SUCCESS("SUCCESS", "处理成功"),
    FAILED("FAILED", "处理失败"),
    IGNORED("IGNORED", "已忽略");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/entity/ContractTemplate.java`

下面的实体对应合同模板表。

```java
package io.github.atengk.contractsign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 合同模板实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("t_contract_template")
public class ContractTemplate {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String templateCode;

    private String templateName;

    private Integer versionNo;

    private String fileName;

    private String objectKey;

    private String fileSha256;

    private String variableSchema;

    private String status;

    private String remark;

    private Long createdBy;

    private Date createdAt;

    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/entity/ContractInstance.java`

下面的实体对应合同实例表，保存生成文件和归档文件的 objectKey 与摘要。

```java
package io.github.atengk.contractsign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 合同实例实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("t_contract_instance")
public class ContractInstance {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String contractNo;

    private Long templateId;

    private String templateCode;

    private Integer templateVersion;

    private String businessType;

    private String businessId;

    private String contractTitle;

    private String variableData;

    private String wordObjectKey;

    private String pdfObjectKey;

    private String signedPdfObjectKey;

    private String pdfSha256;

    private String signedPdfSha256;

    private Long fileSize;

    private Long signedFileSize;

    private String status;

    private String failReason;

    private Long createdBy;

    private Date createdAt;

    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/entity/ContractSignTask.java`

下面的实体对应签署任务表。

```java
package io.github.atengk.contractsign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 合同签署任务实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("t_contract_sign_task")
public class ContractSignTask {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String signTaskNo;

    private Long contractId;

    private String contractNo;

    private String platformCode;

    private String platformFlowId;

    private String signerName;

    private String signerMobile;

    private String signerEmail;

    private String signUrl;

    private String status;

    private String platformStatus;

    private Date submittedAt;

    private Date signedAt;

    private Date expiredAt;

    private String failReason;

    private Date createdAt;

    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/entity/ContractSignCallback.java`

下面的实体对应签署回调记录表。

```java
package io.github.atengk.contractsign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 合同签署回调实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("t_contract_sign_callback")
public class ContractSignCallback {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String platformCode;

    private String eventId;

    private String platformFlowId;

    private String signTaskNo;

    private String contractNo;

    private String eventType;

    private String callbackBody;

    private String signature;

    private Integer verifyResult;

    private String handleStatus;

    private String handleMessage;

    private Date callbackAt;

    private Date handledAt;

    private Date createdAt;
}
```

### Mapper 与 Service

Mapper 使用 MyBatis-Plus 的 `BaseMapper` 即可满足本案例 CRUD 需求。复杂查询可以后续再补 XML，本案例核心链路不需要手写 SQL。

文件位置：`src/main/java/io/github/atengk/contractsign/mapper/ContractTemplateMapper.java`

```java
package io.github.atengk.contractsign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.contractsign.entity.ContractTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同模板Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ContractTemplateMapper extends BaseMapper<ContractTemplate> {
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/mapper/ContractInstanceMapper.java`

```java
package io.github.atengk.contractsign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.contractsign.entity.ContractInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同实例Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ContractInstanceMapper extends BaseMapper<ContractInstance> {
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/mapper/ContractSignTaskMapper.java`

```java
package io.github.atengk.contractsign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.contractsign.entity.ContractSignTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同签署任务Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ContractSignTaskMapper extends BaseMapper<ContractSignTask> {
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/mapper/ContractSignCallbackMapper.java`

```java
package io.github.atengk.contractsign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.contractsign.entity.ContractSignCallback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同签署回调Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ContractSignCallbackMapper extends BaseMapper<ContractSignCallback> {
}
```

本案例核心 Service 已在前文展开，最终文件清单如下：

```text
src/main/java/io/github/atengk/contractsign/service/ContractTemplateService.java
src/main/java/io/github/atengk/contractsign/service/ContractGenerateService.java
src/main/java/io/github/atengk/contractsign/service/ContractSignService.java
src/main/java/io/github/atengk/contractsign/service/ContractSignCallbackService.java
src/main/java/io/github/atengk/contractsign/service/ContractQueryService.java
src/main/java/io/github/atengk/contractsign/service/ContractFileStorageService.java
src/main/java/io/github/atengk/contractsign/service/WordToPdfService.java
```

对应实现类：

```text
src/main/java/io/github/atengk/contractsign/service/impl/ContractTemplateServiceImpl.java
src/main/java/io/github/atengk/contractsign/service/impl/ContractGenerateServiceImpl.java
src/main/java/io/github/atengk/contractsign/service/impl/ContractSignServiceImpl.java
src/main/java/io/github/atengk/contractsign/service/impl/ContractSignCallbackServiceImpl.java
src/main/java/io/github/atengk/contractsign/service/impl/ContractQueryServiceImpl.java
src/main/java/io/github/atengk/contractsign/service/impl/MinioContractFileStorageServiceImpl.java
src/main/java/io/github/atengk/contractsign/service/impl/LibreOfficeWordToPdfServiceImpl.java
```

### 模板生成服务

模板生成服务的核心职责是：读取 MinIO 中的 `.docx` 模板，使用业务变量填充模板，并输出新的 Word 文件。前文已在 `ContractGenerateServiceImpl#renderWord` 中给出私有方法，这里建议抽取成独立服务，便于单独测试。

文件位置：`src/main/java/io/github/atengk/contractsign/service/WordTemplateRenderService.java`

```java
package io.github.atengk.contractsign.service;

import java.io.File;
import java.util.Map;

/**
 * Word模板渲染服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface WordTemplateRenderService {

    /**
     * 渲染Word模板。
     *
     * @param templateFile 模板文件
     * @param outputFile 输出文件
     * @param variables 模板变量
     */
    void render(File templateFile, File outputFile, Map<String, Object> variables);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/PoiTlWordTemplateRenderServiceImpl.java`

下面的实现类使用 `poi-tl` 完成 Word 模板变量填充。

```java
package io.github.atengk.contractsign.service.impl;

import cn.hutool.core.collection.MapUtil;
import com.deepoove.poi.XWPFTemplate;
import io.github.atengk.contractsign.service.WordTemplateRenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

/**
 * poi-tl Word模板渲染服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class PoiTlWordTemplateRenderServiceImpl implements WordTemplateRenderService {

    /**
     * 渲染Word模板。
     *
     * @param templateFile 模板文件
     * @param outputFile 输出文件
     * @param variables 模板变量
     */
    @Override
    public void render(File templateFile, File outputFile, Map<String, Object> variables) {
        if (templateFile == null || !templateFile.exists()) {
            throw new IllegalArgumentException("Word模板文件不存在");
        }
        if (MapUtil.isEmpty(variables)) {
            throw new IllegalArgumentException("模板变量不能为空");
        }

        try (XWPFTemplate template = XWPFTemplate.compile(templateFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            template.render(variables);
            template.write(outputStream);
            log.info("Word模板渲染成功，templateFile={}，outputFile={}",
                    templateFile.getAbsolutePath(), outputFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Word模板渲染失败，templateFile={}", templateFile.getAbsolutePath(), e);
            throw new IllegalStateException("Word模板渲染失败：" + e.getMessage(), e);
        }
    }
}
```

如果使用该独立服务，可以把前文 `ContractGenerateServiceImpl` 中的私有 `renderWord` 方法替换为：

```java
wordTemplateRenderService.render(templateFile, wordFile, dto.getVariables());
```

### PDF 转换服务

PDF 转换服务已经在前文通过 `LibreOfficeWordToPdfServiceImpl` 实现。实际接入时需要确认三个点：

```text
1. application.yml 中 contract.libre-office.command-path 指向正确的 soffice 路径
2. 服务器已安装 LibreOffice 和中文字体
3. work-dir 目录有读写权限
```

最小可用配置如下：

```yaml
contract:
  libre-office:
    # LibreOffice 命令路径
    command-path: /usr/bin/soffice
    # Word/PDF 临时工作目录
    work-dir: /data/contract/tmp
    # 转换超时时间，单位秒
    timeout-seconds: 60
```

服务器验证命令：

```bash
soffice --headless --version
mkdir -p /data/contract/tmp
chmod 755 /data/contract/tmp
```

如果模板中包含中文，必须安装中文字体，否则 PDF 可能出现乱码或字体缺失。

```bash
# Ubuntu / Debian
apt-get update
apt-get install -y libreoffice libreoffice-writer fonts-noto-cjk

# CentOS / Rocky Linux
yum install -y libreoffice libreoffice-writer google-noto-cjk-fonts
```

### MinIO 文件服务

MinIO 文件服务已经在前文通过 `MinioContractFileStorageServiceImpl` 实现。实际项目中建议增加一个生成临时访问地址的方法，便于合同详情页预览 PDF。

文件位置：`src/main/java/io/github/atengk/contractsign/service/ContractFileStorageService.java`

在原接口中补充预览地址方法。

```java
package io.github.atengk.contractsign.service;

import java.io.File;
import java.io.InputStream;

/**
 * 合同文件存储服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ContractFileStorageService {

    /**
     * 上传本地文件。
     *
     * @param objectKey 对象Key
     * @param file 本地文件
     */
    void upload(String objectKey, File file);

    /**
     * 上传输入流。
     *
     * @param objectKey 对象Key
     * @param inputStream 输入流
     * @param size 文件大小
     * @param contentType 文件类型
     */
    void upload(String objectKey, InputStream inputStream, long size, String contentType);

    /**
     * 下载文件到本地。
     *
     * @param objectKey 对象Key
     * @param targetFile 目标文件
     */
    void download(String objectKey, File targetFile);

    /**
     * 生成临时预览地址。
     *
     * @param objectKey 对象Key
     * @return 临时访问地址
     */
    String previewUrl(String objectKey);
}
```

文件位置：`src/main/java/io/github/atengk/contractsign/service/impl/MinioContractFileStorageServiceImpl.java`

在原实现类中补充下面的方法。

```java
/**
 * 生成临时预览地址。
 *
 * @param objectKey 对象Key
 * @return 临时访问地址
 */
@Override
public String previewUrl(String objectKey) {
    try {
        String url = minioClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectKey)
                        .method(io.minio.http.Method.GET)
                        .expiry(minioProperties.getPreviewExpireSeconds())
                        .build()
        );
        log.info("生成合同文件临时预览地址成功，objectKey={}", objectKey);
        return url;
    } catch (Exception e) {
        log.error("生成合同文件临时预览地址失败，objectKey={}", objectKey, e);
        throw new IllegalStateException("生成合同文件临时预览地址失败：" + e.getMessage(), e);
    }
}
```

然后可以在 `ContractDetailVO` 中扩展：

```java
private String pdfPreviewUrl;

private String signedPdfPreviewUrl;
```

并在 `ContractQueryServiceImpl` 中按需设置：

```java
if (StrUtil.isNotBlank(contract.getPdfObjectKey())) {
    vo.setPdfPreviewUrl(contractFileStorageService.previewUrl(contract.getPdfObjectKey()));
}
if (StrUtil.isNotBlank(contract.getSignedPdfObjectKey())) {
    vo.setSignedPdfPreviewUrl(contractFileStorageService.previewUrl(contract.getSignedPdfObjectKey()));
}
```

### 电子签客户端

电子签客户端的核心是把平台差异封装在 `EsignClient` 后面。业务层只关心三个动作：

```text
创建签署流程
验签回调请求
下载已签署文件
```

本案例使用 `MockEsignClient` 打通流程。真实对接时，一般替换这些方法：

| 方法                 | 真实平台实现                               |
| -------------------- | ------------------------------------------ |
| `createSignFlow`     | 调用电子签平台创建签署流程接口             |
| `verifyCallbackSign` | 按平台文档校验请求头、时间戳、随机数、签名 |
| `downloadSignedFile` | 调用平台文件下载接口，保存到本地临时文件   |

真实平台实现建议命名：

```text
src/main/java/io/github/atengk/contractsign/client/EsignBaoClient.java
src/main/java/io/github/atengk/contractsign/client/FadadaClient.java
src/main/java/io/github/atengk/contractsign/client/ShangshangSignClient.java
```

如果项目中同时存在多个平台实现，建议通过配置选择客户端。

文件位置：`src/main/java/io/github/atengk/contractsign/config/EsignClientConfig.java`

下面的配置类根据平台编码选择电子签客户端。当前只有 Mock 实现，后续可以继续扩展。

```java
package io.github.atengk.contractsign.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.contractsign.client.EsignClient;
import io.github.atengk.contractsign.client.MockEsignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 电子签客户端配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@RequiredArgsConstructor
public class EsignClientConfig {

    private final EsignProperties esignProperties;
    private final MockEsignClient mockEsignClient;

    /**
     * 选择电子签客户端。
     *
     * @return 电子签客户端
     */
    @Bean
    public EsignClient esignClient() {
        if (StrUtil.equalsIgnoreCase(esignProperties.getPlatformCode(), "mock")) {
            return mockEsignClient;
        }
        throw new IllegalArgumentException("暂不支持的电子签平台：" + esignProperties.getPlatformCode());
    }
}
```

如果 `MockEsignClient` 本身已经使用 `@Component` 且没有其他 `EsignClient` 实现，可以暂时不加这个配置类。等接入多平台时再增加。

### 签署回调处理器

签署回调处理器已经在 `ContractSignCallbackServiceImpl` 中实现。实际项目中建议把「事件解析」和「业务处理」拆开，方便支持签署完成、拒签、过期等不同事件。

当前最小闭环只处理：

```text
SIGN_COMPLETED -> 签署完成 -> 下载已签署文件 -> 上传MinIO -> 合同归档
```

如果要扩展拒签和过期，可以在 `handleCallback` 中增加事件分支：

```java
if (StrUtil.equals(eventType, "SIGN_REJECTED")) {
    updateRejected(signTask, callback);
    return;
}

if (StrUtil.equals(eventType, "SIGN_EXPIRED")) {
    updateExpired(signTask, callback);
    return;
}
```

补充两个处理方法即可。

下面的代码用于处理拒签和过期事件。

```java
/**
 * 处理拒签事件。
 *
 * @param signTask 签署任务
 * @param callback 回调记录
 */
private void updateRejected(ContractSignTask signTask, ContractSignCallback callback) {
    if (SignTaskStatusEnum.isFinalStatus(signTask.getStatus())) {
        callback.setHandleStatus("IGNORED");
        callback.setHandleMessage("签署任务已处于终态，拒签事件忽略");
        callback.setHandledAt(new Date());
        contractSignCallbackMapper.updateById(callback);
        return;
    }

    signTask.setStatus("REJECTED");
    signTask.setPlatformStatus("REJECTED");
    contractSignTaskMapper.updateById(signTask);

    ContractInstance contract = contractInstanceMapper.selectById(signTask.getContractId());
    if (contract != null) {
        contract.setStatus("FAILED");
        contract.setFailReason("签署人拒签");
        contractInstanceMapper.updateById(contract);
    }

    callback.setHandleStatus("SUCCESS");
    callback.setHandleMessage("拒签事件处理成功");
    callback.setHandledAt(new Date());
    contractSignCallbackMapper.updateById(callback);

    log.info("电子签拒签事件处理成功，signTaskNo={}", signTask.getSignTaskNo());
}

/**
 * 处理过期事件。
 *
 * @param signTask 签署任务
 * @param callback 回调记录
 */
private void updateExpired(ContractSignTask signTask, ContractSignCallback callback) {
    if (SignTaskStatusEnum.isFinalStatus(signTask.getStatus())) {
        callback.setHandleStatus("IGNORED");
        callback.setHandleMessage("签署任务已处于终态，过期事件忽略");
        callback.setHandledAt(new Date());
        contractSignCallbackMapper.updateById(callback);
        return;
    }

    signTask.setStatus("EXPIRED");
    signTask.setPlatformStatus("EXPIRED");
    signTask.setExpiredAt(new Date());
    contractSignTaskMapper.updateById(signTask);

    ContractInstance contract = contractInstanceMapper.selectById(signTask.getContractId());
    if (contract != null) {
        contract.setStatus("FAILED");
        contract.setFailReason("签署任务已过期");
        contractInstanceMapper.updateById(contract);
    }

    callback.setHandleStatus("SUCCESS");
    callback.setHandleMessage("过期事件处理成功");
    callback.setHandledAt(new Date());
    contractSignCallbackMapper.updateById(callback);

    log.info("电子签过期事件处理成功，signTaskNo={}", signTask.getSignTaskNo());
}
```

## 功能验证

功能验证按主链路顺序执行：模板上传、合同生成、PDF 转换、签署回调、归档文件检查。建议先使用 Mock 电子签客户端跑通闭环，再替换真实平台 SDK。

验证前需要确认：

```text
MySQL 已创建数据库和四张表
MinIO 已启动并且配置正确
LibreOffice 已安装并可执行 soffice --headless --version
application.yml 中 contract.esign.platform-code = mock
```

### 模板上传验证

先准备一个 `purchase-contract.docx` 模板，内容包含以下变量：

```text
采购合同

甲方：{{partyAName}}
乙方：{{partyBName}}
合同编号：{{contractNo}}
合同金额：{{contractAmount}}
签署日期：{{signDate}}
```

调用模板上传接口：

```bash
curl -X POST 'http://localhost:8080/api/contract/templates/upload' \
  -F 'templateCode=PURCHASE_CONTRACT' \
  -F 'templateName=采购合同模板' \
  -F 'file=@/data/templates/purchase-contract.docx'
```

预期结果：

```json
{
  "templateCode": "PURCHASE_CONTRACT",
  "templateName": "采购合同模板",
  "versionNo": 1,
  "variables": [
    {
      "key": "partyAName",
      "name": "partyAName",
      "required": true
    }
  ]
}
```

数据库验证：

```sql
SELECT id, template_code, template_name, version_no, object_key, file_sha256, status
FROM t_contract_template
WHERE template_code = 'PURCHASE_CONTRACT'
ORDER BY version_no DESC;
```

MinIO 验证：

```text
bucket: contract-files
objectKey: contract-template/PURCHASE_CONTRACT/1/xxxx_purchase-contract.docx
```

### 合同生成验证

调用合同生成接口：

```bash
curl -X POST 'http://localhost:8080/api/contracts/generate' \
  -H 'Content-Type: application/json' \
  -d '{
    "templateCode": "PURCHASE_CONTRACT",
    "businessType": "PURCHASE",
    "businessId": "PO202605150001",
    "contractTitle": "采购合同-PO202605150001",
    "variables": {
      "partyAName": "杭州示例科技有限公司",
      "partyBName": "上海测试信息技术有限公司",
      "contractNo": "HT202605150001",
      "contractAmount": "100000.00",
      "signDate": "2026-05-15"
    }
  }'
```

预期结果：

```json
{
  "contractNo": "HTPURCHASE20260515xxxx",
  "contractTitle": "采购合同-PO202605150001",
  "pdfObjectKey": "contract-instance/20260515/HTPURCHASE20260515xxxx/preview.pdf",
  "status": "GENERATED"
}
```

数据库验证：

```sql
SELECT id, contract_no, template_code, business_type, business_id, pdf_object_key, pdf_sha256, status
FROM t_contract_instance
WHERE business_type = 'PURCHASE'
  AND business_id = 'PO202605150001';
```

预期状态：

```text
status = GENERATED
word_object_key 不为空
pdf_object_key 不为空
pdf_sha256 不为空
```

### PDF 转换验证

PDF 转换是否成功，主要看生成接口是否能拿到 `preview.pdf`，以及 MinIO 中是否存在 PDF 文件。

本地先验证 LibreOffice 命令：

```bash
soffice --headless --version
```

如果要单独验证某个 Word 文件能否转换：

```bash
mkdir -p /data/contract/tmp

soffice \
  --headless \
  --convert-to pdf \
  --outdir /data/contract/tmp \
  /data/templates/purchase-contract.docx

ls -lh /data/contract/tmp
```

如果接口生成失败，并且日志中出现字体、权限或命令不存在问题，优先检查：

```text
/usr/bin/soffice 是否存在
/data/contract/tmp 是否可写
服务器是否安装中文字体
容器内是否包含 LibreOffice
```

常见错误：

| 错误                                    | 处理方式                                       |
| --------------------------------------- | ---------------------------------------------- |
| `Cannot run program "/usr/bin/soffice"` | 安装 LibreOffice 或修正 command-path           |
| PDF 中文乱码                            | 安装 `fonts-noto-cjk` 或其他中文字体           |
| 未生成 PDF 文件                         | 检查 Word 模板是否损坏，检查 work-dir 权限     |
| 转换超时                                | 增大 timeout-seconds，或把转换服务拆到独立节点 |

### 签署回调验证

先发起签署：

```bash
curl -X POST 'http://localhost:8080/api/contracts/sign/start' \
  -H 'Content-Type: application/json' \
  -d '{
    "contractId": 1780000000000000002,
    "signerName": "张三",
    "signerMobile": "13800000000",
    "signerEmail": "zhangsan@example.com"
  }'
```

拿到响应中的 `signTaskNo` 和日志中的 `platformFlowId` 后，构造回调请求体。

```json
{
  "platformCode": "MOCK",
  "eventId": "EVT_202605150001",
  "eventType": "SIGN_COMPLETED",
  "platformFlowId": "MOCK_FLOW_abc123",
  "signTaskNo": "SIGN_HTPURCHASE202605150001_1780000000000",
  "contractNo": "HTPURCHASE202605150001",
  "platformStatus": "SIGNED",
  "signedFileUrl": "mock://signed-file/MOCK_FLOW_abc123",
  "callbackTime": "2026-05-15 10:30:00"
}
```

为了方便本地测试，可以增加一个临时签名工具接口或直接写单元测试生成签名。下面给出临时工具类方法。

文件位置：`src/test/java/io/github/atengk/contractsign/MockSignTest.java`

下面的测试用于生成 Mock 回调签名。

```java
package io.github.atengk.contractsign;

import cn.hutool.crypto.SecureUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Mock电子签回调签名测试
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MockSignTest {

    /**
     * 生成Mock回调签名。
     */
    @Test
    void generateSignature() {
        String body = """
                {
                  "platformCode": "MOCK",
                  "eventId": "EVT_202605150001",
                  "eventType": "SIGN_COMPLETED",
                  "platformFlowId": "MOCK_FLOW_abc123",
                  "signTaskNo": "SIGN_HTPURCHASE202605150001_1780000000000",
                  "contractNo": "HTPURCHASE202605150001",
                  "platformStatus": "SIGNED",
                  "signedFileUrl": "mock://signed-file/MOCK_FLOW_abc123",
                  "callbackTime": "2026-05-15 10:30:00"
                }
                """;

        String signature = SecureUtil.hmacSha256("demo-callback-secret".getBytes(StandardCharsets.UTF_8))
                .digestHex(body);

        System.out.println(signature);
    }
}
```

调用回调接口：

```bash
BODY='{
  "platformCode": "MOCK",
  "eventId": "EVT_202605150001",
  "eventType": "SIGN_COMPLETED",
  "platformFlowId": "MOCK_FLOW_abc123",
  "signTaskNo": "SIGN_HTPURCHASE202605150001_1780000000000",
  "contractNo": "HTPURCHASE202605150001",
  "platformStatus": "SIGNED",
  "signedFileUrl": "mock://signed-file/MOCK_FLOW_abc123",
  "callbackTime": "2026-05-15 10:30:00"
}'

SIGNATURE='替换为MockSignTest输出的签名'

curl -X POST 'http://localhost:8080/api/contracts/sign/callback' \
  -H 'Content-Type: application/json' \
  -H "X-Esign-Signature: ${SIGNATURE}" \
  -d "${BODY}"
```

预期响应：

```text
success
```

数据库验证：

```sql
SELECT event_id, verify_result, handle_status, handle_message
FROM t_contract_sign_callback
WHERE event_id = 'EVT_202605150001';
```

预期结果：

```text
verify_result = 1
handle_status = SUCCESS
```

再次使用同一个 `eventId` 调用回调接口，预期仍返回 `success`，但不会重复更新合同和签署任务。

### 归档文件验证

归档验证重点检查三个位置：合同实例表、签署任务表、MinIO 文件。

合同实例验证：

```sql
SELECT contract_no,
       status,
       signed_pdf_object_key,
       signed_pdf_sha256,
       signed_file_size
FROM t_contract_instance
WHERE contract_no = 'HTPURCHASE202605150001';
```

预期结果：

```text
status = ARCHIVED
signed_pdf_object_key 不为空
signed_pdf_sha256 不为空
signed_file_size > 0
```

签署任务验证：

```sql
SELECT sign_task_no,
       platform_flow_id,
       status,
       platform_status,
       signed_at
FROM t_contract_sign_task
WHERE contract_no = 'HTPURCHASE202605150001';
```

预期结果：

```text
status = SIGNED
platform_status = SIGNED
signed_at 不为空
```

MinIO 验证：

```text
bucket: contract-files
objectKey: contract-instance/20260515/HTPURCHASE202605150001/signed.pdf
```

查询合同详情：

```bash
curl -X GET 'http://localhost:8080/api/contracts/1780000000000000002'
```

预期响应中应包含：

```json
{
  "contractStatus": "ARCHIVED",
  "signStatus": "SIGNED",
  "signedPdfObjectKey": "contract-instance/20260515/HTPURCHASE202605150001/signed.pdf",
  "signedPdfSha256": "9a4c6b7e8f0a1c2d..."
}
```

完整主链路验证完成后，说明本案例已经实现核心闭环：

```text
模板上传成功
-> 变量解析成功
-> 合同生成成功
-> Word 转 PDF 成功
-> PDF 上传 MinIO 成功
-> 签署任务创建成功
-> 回调验签成功
-> 回调幂等生效
-> 已签署 PDF 归档成功
-> 合同详情可查询
```