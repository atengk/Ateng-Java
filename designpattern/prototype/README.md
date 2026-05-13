# 原型模式

原型模式属于创建型模式，用于通过复制已有对象来创建新对象。它适合对象创建成本较高、初始化字段较多、对象结构相似、需要基于模板快速生成新对象的场景。在当前 29 个设计模式文档体系中，原型模式属于 GoF 创建型模式，模块名为 `prototype`。

## 基础配置

本示例基于 JDK 21、Spring Boot 3、Maven、Hutool、Lombok 编写。示例场景是“审批流程模板复制”。系统中预置多个审批流程模板，例如采购审批、报销审批、合同审批。用户创建业务审批单时，不需要重新组装审批节点，而是从已有模板复制一份流程实例，再填充业务单号、申请人、实例名称等信息。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供原型模式验证接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation，用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、日期、ID、JSON 等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok，用于减少 getter、setter、构造方法和日志样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于验证原型复制逻辑 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

demo:
  prototype:
    # 默认租户编号
    default-tenant-id: TENANT_10001
    # 默认审批实例过期天数
    default-expire-days: 7
```

## 模式说明

原型模式解决的是“如何基于已有对象快速创建相似对象”的问题。它不是从零开始 `new` 一个对象，而是复制一个已有对象，然后修改少量字段。

在 Spring Boot 项目中，原型模式常见于：

```text
复制审批流程模板生成审批实例
复制报表配置生成用户自定义报表
复制活动模板生成营销活动
复制问卷模板生成新问卷
复制规则模板生成业务规则
复制导出任务模板生成导出任务
复制复杂请求对象生成批量任务参数
```

原型模式需要重点区分浅拷贝和深拷贝。

```text
浅拷贝：只复制对象本身，引用类型字段仍然指向同一个对象
深拷贝：对象本身和内部引用对象都会复制，新旧对象互不影响
```

在实际业务中，如果对象中包含 `List`、`Map`、子对象等引用类型，通常需要深拷贝。审批流程模板中包含多个审批节点，如果只做浅拷贝，新生成的审批实例修改节点状态时，可能会污染原始模板。

## 项目结构

本示例按照 Spring Boot 常规分层组织。核心类是 `ApprovalFlowPrototype`，它表示可以被复制的审批流程原型对象。

```text
src/main/java/io/github/atengk/pattern/prototype
├── PrototypeApplication.java
├── config
│   └── PrototypeDemoProperties.java
├── controller
│   └── ApprovalFlowController.java
├── domain
│   ├── ApprovalFlowPrototype.java
│   └── ApprovalStep.java
├── dto
│   └── ApprovalFlowCopyDTO.java
├── enums
│   ├── ApprovalStepStatusEnum.java
│   └── ApprovalTemplateEnum.java
├── prototype
│   └── BusinessPrototype.java
├── registry
│   └── ApprovalFlowPrototypeRegistry.java
├── service
│   ├── ApprovalFlowService.java
│   └── ApprovalFlowServiceImpl.java
└── vo
    └── ApprovalFlowInstanceVO.java
```

## 核心代码

这一部分给出原型模式的完整核心代码。重点是 `BusinessPrototype<T>` 接口和 `ApprovalFlowPrototype.copy()` 方法。这里不使用 `Object.clone()`，而是手写深拷贝逻辑，使复制行为更清晰、更可控。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/PrototypeApplication.java`

```java
package io.github.atengk.pattern.prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 原型模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class PrototypeApplication {

    /**
     * 启动原型模式示例应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PrototypeApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/prototype/config/PrototypeDemoProperties.java`

```java
package io.github.atengk.pattern.prototype.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 原型模式示例配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "demo.prototype")
public class PrototypeDemoProperties {

    /**
     * 默认租户编号
     */
    private String defaultTenantId = "TENANT_10001";

    /**
     * 默认审批实例过期天数
     */
    private Integer defaultExpireDays = 7;

}
```

## 枚举定义

这一部分定义审批模板类型和审批节点状态。模板类型用于选择要复制的原型对象，节点状态用于区分模板节点和实例节点的处理状态。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/enums/ApprovalTemplateEnum.java`

```java
package io.github.atengk.pattern.prototype.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审批模板枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public enum ApprovalTemplateEnum {

    PURCHASE("PURCHASE", "采购审批模板"),

    REIMBURSEMENT("REIMBURSEMENT", "报销审批模板"),

    CONTRACT("CONTRACT", "合同审批模板");

    private final String code;

    private final String description;

    ApprovalTemplateEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码解析审批模板
     *
     * @param code 模板编码
     * @return 审批模板
     */
    public static ApprovalTemplateEnum parse(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的审批模板：{}", code)));
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/prototype/enums/ApprovalStepStatusEnum.java`

```java
package io.github.atengk.pattern.prototype.enums;

import lombok.Getter;

/**
 * 审批节点状态枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public enum ApprovalStepStatusEnum {

    WAITING("WAITING", "待审批"),

    APPROVED("APPROVED", "已通过"),

    REJECTED("REJECTED", "已拒绝");

    private final String code;

    private final String description;

    ApprovalStepStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
```

## 原型接口和领域对象

这一部分是原型模式的核心。`BusinessPrototype<T>` 定义统一复制方法，具体原型对象实现自己的复制逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/prototype/BusinessPrototype.java`

```java
package io.github.atengk.pattern.prototype.prototype;

/**
 * 业务原型接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface BusinessPrototype<T> {

    /**
     * 复制当前对象
     *
     * @return 新对象
     */
    T copy();

}
```

下面的 `ApprovalStep` 表示审批节点。它也提供 `copy()` 方法，用于支持审批流程的深拷贝。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/domain/ApprovalStep.java`

```java
package io.github.atengk.pattern.prototype.domain;

import io.github.atengk.pattern.prototype.enums.ApprovalStepStatusEnum;
import io.github.atengk.pattern.prototype.prototype.BusinessPrototype;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批节点
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStep implements BusinessPrototype<ApprovalStep> {

    /**
     * 节点序号
     */
    private Integer stepNo;

    /**
     * 节点名称
     */
    private String stepName;

    /**
     * 审批人角色
     */
    private String approverRole;

    /**
     * 审批状态
     */
    private ApprovalStepStatusEnum status;

    /**
     * 复制审批节点
     *
     * @return 新审批节点
     */
    @Override
    public ApprovalStep copy() {
        return new ApprovalStep(
                this.stepNo,
                this.stepName,
                this.approverRole,
                this.status
        );
    }

}
```

下面的 `ApprovalFlowPrototype` 表示审批流程原型对象。它内部包含审批节点列表，因此 `copy()` 方法必须复制每一个节点，不能只复制 `List` 引用。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/domain/ApprovalFlowPrototype.java`

```java
package io.github.atengk.pattern.prototype.domain;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.prototype.prototype.BusinessPrototype;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批流程原型对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@Setter
public class ApprovalFlowPrototype implements BusinessPrototype<ApprovalFlowPrototype> {

    /**
     * 流程实例编号
     */
    private String flowId;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 业务单号
     */
    private String businessNo;

    /**
     * 申请人编号
     */
    private String applicantId;

    /**
     * 过期天数
     */
    private Integer expireDays;

    /**
     * 审批节点列表
     */
    private List<ApprovalStep> steps = new ArrayList<>();

    /**
     * 复制审批流程原型对象
     *
     * @return 新审批流程对象
     */
    @Override
    public ApprovalFlowPrototype copy() {
        ApprovalFlowPrototype target = new ApprovalFlowPrototype();
        target.setFlowId(IdUtil.fastSimpleUUID());
        target.setTemplateCode(this.templateCode);
        target.setFlowName(this.flowName);
        target.setTenantId(this.tenantId);
        target.setBusinessNo(this.businessNo);
        target.setApplicantId(this.applicantId);
        target.setExpireDays(this.expireDays);

        if (CollUtil.isNotEmpty(this.steps)) {
            List<ApprovalStep> copiedSteps = this.steps.stream()
                    .map(ApprovalStep::copy)
                    .toList();
            target.setSteps(new ArrayList<>(copiedSteps));
        }

        return target;
    }

}
```

## DTO 和 VO

这一部分定义接口请求对象和响应对象。调用方只需要指定模板编码、业务单号和申请人，系统会基于模板复制出新的审批流程实例。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/dto/ApprovalFlowCopyDTO.java`

```java
package io.github.atengk.pattern.prototype.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批流程复制请求对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ApprovalFlowCopyDTO {

    /**
     * 模板编码：PURCHASE、REIMBURSEMENT、CONTRACT
     */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    /**
     * 业务单号
     */
    @NotBlank(message = "业务单号不能为空")
    private String businessNo;

    /**
     * 申请人编号
     */
    @NotBlank(message = "申请人编号不能为空")
    private String applicantId;

    /**
     * 自定义流程名称
     */
    private String customFlowName;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/prototype/vo/ApprovalFlowInstanceVO.java`

```java
package io.github.atengk.pattern.prototype.vo;

import java.util.List;

/**
 * 审批流程实例响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record ApprovalFlowInstanceVO(
        String flowId,
        String templateCode,
        String flowName,
        String tenantId,
        String businessNo,
        String applicantId,
        Integer expireDays,
        List<ApprovalStepVO> steps,
        String createTime
) {

    /**
     * 审批节点响应对象
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public record ApprovalStepVO(
            Integer stepNo,
            String stepName,
            String approverRole,
            String status
    ) {
    }

}
```

## 原型注册表

原型注册表用于保存系统预置的审批流程模板。真实项目中，这些模板可以来自数据库、配置中心、缓存或后台配置页面。本示例为了突出原型模式，使用内存初始化模板。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/registry/ApprovalFlowPrototypeRegistry.java`

```java
package io.github.atengk.pattern.prototype.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.prototype.config.PrototypeDemoProperties;
import io.github.atengk.pattern.prototype.domain.ApprovalFlowPrototype;
import io.github.atengk.pattern.prototype.domain.ApprovalStep;
import io.github.atengk.pattern.prototype.enums.ApprovalStepStatusEnum;
import io.github.atengk.pattern.prototype.enums.ApprovalTemplateEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 审批流程原型注册表
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class ApprovalFlowPrototypeRegistry {

    private final Map<ApprovalTemplateEnum, ApprovalFlowPrototype> prototypeMap;

    /**
     * 初始化审批流程原型注册表
     *
     * @param properties 原型模式示例配置
     */
    public ApprovalFlowPrototypeRegistry(PrototypeDemoProperties properties) {
        Map<ApprovalTemplateEnum, ApprovalFlowPrototype> tempPrototypeMap = new EnumMap<>(ApprovalTemplateEnum.class);

        tempPrototypeMap.put(ApprovalTemplateEnum.PURCHASE, createPurchasePrototype(properties));
        tempPrototypeMap.put(ApprovalTemplateEnum.REIMBURSEMENT, createReimbursementPrototype(properties));
        tempPrototypeMap.put(ApprovalTemplateEnum.CONTRACT, createContractPrototype(properties));

        this.prototypeMap = Collections.unmodifiableMap(tempPrototypeMap);
        log.info("初始化审批流程原型注册表完成，模板数量：{}", MapUtil.size(prototypeMap));
    }

    /**
     * 根据模板编码获取审批流程原型副本
     *
     * @param templateCode 模板编码
     * @return 审批流程原型副本
     */
    public ApprovalFlowPrototype copyByTemplateCode(String templateCode) {
        ApprovalTemplateEnum template = ApprovalTemplateEnum.parse(templateCode);
        ApprovalFlowPrototype prototype = prototypeMap.get(template);

        if (prototype == null) {
            throw new IllegalArgumentException(StrUtil.format("审批流程原型不存在：{}", templateCode));
        }

        ApprovalFlowPrototype copiedPrototype = prototype.copy();
        log.info("复制审批流程原型，templateCode：{}，newFlowId：{}", template.getCode(), copiedPrototype.getFlowId());
        return copiedPrototype;
    }

    /**
     * 获取已注册原型数量
     *
     * @return 已注册原型数量
     */
    public int registeredCount() {
        return MapUtil.size(prototypeMap);
    }

    /**
     * 创建采购审批流程原型
     *
     * @param properties 示例配置
     * @return 采购审批流程原型
     */
    private ApprovalFlowPrototype createPurchasePrototype(PrototypeDemoProperties properties) {
        ApprovalFlowPrototype prototype = createBasePrototype(
                ApprovalTemplateEnum.PURCHASE,
                "采购审批流程模板",
                properties
        );

        prototype.setSteps(CollUtil.newArrayList(
                new ApprovalStep(1, "部门主管审批", "DEPARTMENT_MANAGER", ApprovalStepStatusEnum.WAITING),
                new ApprovalStep(2, "采购经理审批", "PURCHASE_MANAGER", ApprovalStepStatusEnum.WAITING),
                new ApprovalStep(3, "财务审批", "FINANCE_MANAGER", ApprovalStepStatusEnum.WAITING)
        ));

        return prototype;
    }

    /**
     * 创建报销审批流程原型
     *
     * @param properties 示例配置
     * @return 报销审批流程原型
     */
    private ApprovalFlowPrototype createReimbursementPrototype(PrototypeDemoProperties properties) {
        ApprovalFlowPrototype prototype = createBasePrototype(
                ApprovalTemplateEnum.REIMBURSEMENT,
                "报销审批流程模板",
                properties
        );

        prototype.setSteps(CollUtil.newArrayList(
                new ApprovalStep(1, "直属主管审批", "DIRECT_MANAGER", ApprovalStepStatusEnum.WAITING),
                new ApprovalStep(2, "财务复核", "FINANCE_AUDITOR", ApprovalStepStatusEnum.WAITING)
        ));

        return prototype;
    }

    /**
     * 创建合同审批流程原型
     *
     * @param properties 示例配置
     * @return 合同审批流程原型
     */
    private ApprovalFlowPrototype createContractPrototype(PrototypeDemoProperties properties) {
        ApprovalFlowPrototype prototype = createBasePrototype(
                ApprovalTemplateEnum.CONTRACT,
                "合同审批流程模板",
                properties
        );

        prototype.setSteps(CollUtil.newArrayList(
                new ApprovalStep(1, "业务负责人审批", "BUSINESS_OWNER", ApprovalStepStatusEnum.WAITING),
                new ApprovalStep(2, "法务审批", "LEGAL_MANAGER", ApprovalStepStatusEnum.WAITING),
                new ApprovalStep(3, "财务审批", "FINANCE_MANAGER", ApprovalStepStatusEnum.WAITING),
                new ApprovalStep(4, "总经理审批", "GENERAL_MANAGER", ApprovalStepStatusEnum.WAITING)
        ));

        return prototype;
    }

    /**
     * 创建基础审批流程原型
     *
     * @param template   模板枚举
     * @param flowName   流程名称
     * @param properties 示例配置
     * @return 基础审批流程原型
     */
    private ApprovalFlowPrototype createBasePrototype(ApprovalTemplateEnum template,
                                                      String flowName,
                                                      PrototypeDemoProperties properties) {
        ApprovalFlowPrototype prototype = new ApprovalFlowPrototype();
        prototype.setTemplateCode(template.getCode());
        prototype.setFlowName(flowName);
        prototype.setTenantId(properties.getDefaultTenantId());
        prototype.setExpireDays(properties.getDefaultExpireDays());
        return prototype;
    }

}
```

## 业务层调用

业务层通过原型注册表复制模板，然后填充当前业务实例字段。这样创建审批实例时不需要重复组装审批节点，模板变更和实例创建逻辑也更容易维护。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/service/ApprovalFlowService.java`

```java
package io.github.atengk.pattern.prototype.service;

import io.github.atengk.pattern.prototype.dto.ApprovalFlowCopyDTO;
import io.github.atengk.pattern.prototype.vo.ApprovalFlowInstanceVO;

/**
 * 审批流程服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ApprovalFlowService {

    /**
     * 复制审批流程模板并创建实例
     *
     * @param copyDTO 复制请求
     * @return 审批流程实例
     */
    ApprovalFlowInstanceVO copyFromTemplate(ApprovalFlowCopyDTO copyDTO);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/prototype/service/ApprovalFlowServiceImpl.java`

```java
package io.github.atengk.pattern.prototype.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.pattern.prototype.domain.ApprovalFlowPrototype;
import io.github.atengk.pattern.prototype.dto.ApprovalFlowCopyDTO;
import io.github.atengk.pattern.prototype.registry.ApprovalFlowPrototypeRegistry;
import io.github.atengk.pattern.prototype.vo.ApprovalFlowInstanceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审批流程服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    private final ApprovalFlowPrototypeRegistry prototypeRegistry;

    /**
     * 复制审批流程模板并创建实例
     *
     * @param copyDTO 复制请求
     * @return 审批流程实例
     */
    @Override
    public ApprovalFlowInstanceVO copyFromTemplate(ApprovalFlowCopyDTO copyDTO) {
        log.info("准备复制审批流程模板，请求参数：{}", JSONUtil.toJsonStr(copyDTO));

        ApprovalFlowPrototype flowInstance = prototypeRegistry.copyByTemplateCode(copyDTO.getTemplateCode());
        flowInstance.setBusinessNo(copyDTO.getBusinessNo());
        flowInstance.setApplicantId(copyDTO.getApplicantId());

        if (StrUtil.isNotBlank(copyDTO.getCustomFlowName())) {
            flowInstance.setFlowName(copyDTO.getCustomFlowName());
        }

        ApprovalFlowInstanceVO result = convertToVO(flowInstance);
        log.info("审批流程实例创建完成，flowId：{}，businessNo：{}", result.flowId(), result.businessNo());
        return result;
    }

    /**
     * 转换为响应对象
     *
     * @param flowInstance 审批流程实例
     * @return 响应对象
     */
    private ApprovalFlowInstanceVO convertToVO(ApprovalFlowPrototype flowInstance) {
        List<ApprovalFlowInstanceVO.ApprovalStepVO> stepVOList = flowInstance.getSteps().stream()
                .map(step -> new ApprovalFlowInstanceVO.ApprovalStepVO(
                        step.getStepNo(),
                        step.getStepName(),
                        step.getApproverRole(),
                        step.getStatus().getCode()
                ))
                .toList();

        return new ApprovalFlowInstanceVO(
                flowInstance.getFlowId(),
                flowInstance.getTemplateCode(),
                flowInstance.getFlowName(),
                flowInstance.getTenantId(),
                flowInstance.getBusinessNo(),
                flowInstance.getApplicantId(),
                flowInstance.getExpireDays(),
                stepVOList,
                DateUtil.now()
        );
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/prototype/controller/ApprovalFlowController.java`

```java
package io.github.atengk.pattern.prototype.controller;

import io.github.atengk.pattern.prototype.dto.ApprovalFlowCopyDTO;
import io.github.atengk.pattern.prototype.service.ApprovalFlowService;
import io.github.atengk.pattern.prototype.vo.ApprovalFlowInstanceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审批流程接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/api/patterns/prototype/approval-flows")
@RequiredArgsConstructor
public class ApprovalFlowController {

    private final ApprovalFlowService approvalFlowService;

    /**
     * 根据模板复制审批流程实例
     *
     * @param copyDTO 复制请求
     * @return 审批流程实例
     */
    @PostMapping("/copy")
    public ApprovalFlowInstanceVO copy(@Valid @RequestBody ApprovalFlowCopyDTO copyDTO) {
        return approvalFlowService.copyFromTemplate(copyDTO);
    }

}
```

## 使用方式

启动项目后，通过接口传入模板编码和业务信息，系统会基于预置模板复制出新的审批流程实例。

接口地址：

```text
POST /api/patterns/prototype/approval-flows/copy
```

采购审批复制请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/prototype/approval-flows/copy" \
  -H "Content-Type: application/json" \
  -d '{
    "templateCode": "PURCHASE",
    "businessNo": "PURCHASE_ORDER_10001",
    "applicantId": "USER_001",
    "customFlowName": "办公设备采购审批"
  }'
```

响应示例：

```json
{
  "flowId": "9ef8c0d6238e4af5acaa0f48c5de9c62",
  "templateCode": "PURCHASE",
  "flowName": "办公设备采购审批",
  "tenantId": "TENANT_10001",
  "businessNo": "PURCHASE_ORDER_10001",
  "applicantId": "USER_001",
  "expireDays": 7,
  "steps": [
    {
      "stepNo": 1,
      "stepName": "部门主管审批",
      "approverRole": "DEPARTMENT_MANAGER",
      "status": "WAITING"
    },
    {
      "stepNo": 2,
      "stepName": "采购经理审批",
      "approverRole": "PURCHASE_MANAGER",
      "status": "WAITING"
    },
    {
      "stepNo": 3,
      "stepName": "财务审批",
      "approverRole": "FINANCE_MANAGER",
      "status": "WAITING"
    }
  ],
  "createTime": "2026-05-13 15:20:30"
}
```

报销审批复制请求：

```bash
curl -X POST "http://localhost:8080/api/patterns/prototype/approval-flows/copy" \
  -H "Content-Type: application/json" \
  -d '{
    "templateCode": "REIMBURSEMENT",
    "businessNo": "REIMBURSEMENT_20001",
    "applicantId": "USER_002"
  }'
```

响应示例：

```json
{
  "flowId": "cb2d4f0d2ec94bdb8a79c7050f7f9054",
  "templateCode": "REIMBURSEMENT",
  "flowName": "报销审批流程模板",
  "tenantId": "TENANT_10001",
  "businessNo": "REIMBURSEMENT_20001",
  "applicantId": "USER_002",
  "expireDays": 7,
  "steps": [
    {
      "stepNo": 1,
      "stepName": "直属主管审批",
      "approverRole": "DIRECT_MANAGER",
      "status": "WAITING"
    },
    {
      "stepNo": 2,
      "stepName": "财务复核",
      "approverRole": "FINANCE_AUDITOR",
      "status": "WAITING"
    }
  ],
  "createTime": "2026-05-13 15:21:06"
}
```

## 深拷贝和浅拷贝对比

原型模式最容易出问题的地方是引用类型字段。审批流程对象中有 `List<ApprovalStep>`，如果只是简单复制列表引用，新旧流程会共享同一批节点。

错误示例：

```java
ApprovalFlowPrototype target = new ApprovalFlowPrototype();
target.setSteps(this.steps);
```

这种写法是浅拷贝。复制后的流程实例和模板对象共享同一个 `steps` 列表。只要实例修改了节点状态，模板中的节点也会被修改。

正确示例：

```java
List<ApprovalStep> copiedSteps = this.steps.stream()
        .map(ApprovalStep::copy)
        .toList();

target.setSteps(new ArrayList<>(copiedSteps));
```

这种写法是深拷贝。新的流程对象拥有新的节点列表，每个节点也是新对象。实例节点状态变化不会影响模板节点。

## 使用 Hutool JSON 做深拷贝

如果对象结构比较规整，也可以使用 Hutool 的 JSON 序列化方式进行深拷贝。它适合简单 POJO，但不适合包含复杂泛型、多态对象、非标准构造逻辑、敏感字段或不可序列化资源的对象。

下面示例用于说明思路，不建议替代所有手写复制逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/prototype/util/PrototypeCopyUtil.java`

```java
package io.github.atengk.pattern.prototype.util;

import cn.hutool.json.JSONUtil;

/**
 * 原型复制工具类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class PrototypeCopyUtil {

    private PrototypeCopyUtil() {
    }

    /**
     * 通过 JSON 序列化实现深拷贝
     *
     * @param source 原始对象
     * @param targetClass 目标类型
     * @return 深拷贝对象
     * @param <T> 对象类型
     */
    public static <T> T copyByJson(T source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        String json = JSONUtil.toJsonStr(source);
        return JSONUtil.toBean(json, targetClass);
    }

}
```

调用示例：

```java
ApprovalFlowPrototype copied = PrototypeCopyUtil.copyByJson(source, ApprovalFlowPrototype.class);
```

手写 `copy()` 的优点是复制规则明确，可以排除不应复制的字段，例如数据库主键、创建时间、审批状态、锁版本号等。JSON 深拷贝的优点是代码少，但复制规则不够显式。业务对象较复杂时，建议手写复制逻辑。

## 验证方式

可以通过单元测试验证原型复制出的对象是否是新对象，并确认内部节点列表也被深拷贝。

文件位置：`src/test/java/io/github/atengk/pattern/prototype/ApprovalFlowPrototypeRegistryTest.java`

```java
package io.github.atengk.pattern.prototype;

import io.github.atengk.pattern.prototype.domain.ApprovalFlowPrototype;
import io.github.atengk.pattern.prototype.registry.ApprovalFlowPrototypeRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 审批流程原型注册表测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@SpringBootTest
class ApprovalFlowPrototypeRegistryTest {

    private final ApprovalFlowPrototypeRegistry prototypeRegistry;

    ApprovalFlowPrototypeRegistryTest(ApprovalFlowPrototypeRegistry prototypeRegistry) {
        this.prototypeRegistry = prototypeRegistry;
    }

    /**
     * 验证同一模板复制出的流程实例是不同对象
     */
    @Test
    void shouldCopyDifferentFlowInstanceFromSameTemplate() {
        ApprovalFlowPrototype firstFlow = prototypeRegistry.copyByTemplateCode("PURCHASE");
        ApprovalFlowPrototype secondFlow = prototypeRegistry.copyByTemplateCode("PURCHASE");

        log.info("第一次复制流程编号：{}", firstFlow.getFlowId());
        log.info("第二次复制流程编号：{}", secondFlow.getFlowId());

        Assertions.assertNotSame(firstFlow, secondFlow);
        Assertions.assertNotEquals(firstFlow.getFlowId(), secondFlow.getFlowId());
        Assertions.assertEquals(firstFlow.getTemplateCode(), secondFlow.getTemplateCode());
    }

    /**
     * 验证审批节点列表已经深拷贝
     */
    @Test
    void shouldDeepCopyApprovalSteps() {
        ApprovalFlowPrototype firstFlow = prototypeRegistry.copyByTemplateCode("CONTRACT");
        ApprovalFlowPrototype secondFlow = prototypeRegistry.copyByTemplateCode("CONTRACT");

        log.info("第一个流程节点数量：{}", firstFlow.getSteps().size());
        log.info("第二个流程节点数量：{}", secondFlow.getSteps().size());

        Assertions.assertNotSame(firstFlow.getSteps(), secondFlow.getSteps());
        Assertions.assertNotSame(firstFlow.getSteps().get(0), secondFlow.getSteps().get(0));
        Assertions.assertEquals(firstFlow.getSteps().get(0).getStepName(), secondFlow.getSteps().get(0).getStepName());
    }

    /**
     * 验证不支持的模板编码会抛出异常
     */
    @Test
    void shouldThrowExceptionWhenTemplateUnsupported() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> prototypeRegistry.copyByTemplateCode("UNKNOWN")
        );

        log.info("不支持模板异常信息：{}", exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("不支持的审批模板"));
    }

}
```

执行测试：

```bash
mvn test -Dtest=ApprovalFlowPrototypeRegistryTest
```

命令说明：`-Dtest=ApprovalFlowPrototypeRegistryTest` 表示只运行当前原型模式测试类，用于快速验证原型复制、深拷贝和异常分支逻辑。

## 和其他创建型模式的区别

原型模式、构建者模式、工厂方法模式都可以创建对象，但它们解决的问题不同。

```text
工厂方法模式：由具体工厂创建对象，关注创建职责分配
抽象工厂模式：创建一组相关对象，关注产品族一致性
构建者模式：分步骤构建复杂对象，关注构建过程清晰
原型模式：复制已有对象生成新对象，关注对象复用和复制
```

可以按下面的方式判断是否适合使用原型模式：

```text
对象大部分字段来自已有模板：适合原型模式
对象字段很多且需要逐步设置：适合构建者模式
对象需要根据类型选择具体实现：适合工厂相关模式
对象需要创建一整套相关产品：适合抽象工厂模式
```

在审批场景中：

```text
复制采购审批模板生成采购审批实例：原型模式
构建订单提交上下文：构建者模式
根据支付渠道创建支付客户端：工厂方法模式
创建某个平台下的支付、退款、查询客户端：抽象工厂模式
```

## 开发建议

在 Spring Boot 项目中使用原型模式时，建议遵循以下原则：

```text
模板对象和实例对象字段高度相似时，再考虑原型模式
包含集合、Map、子对象时，优先实现深拷贝
不要直接复制数据库主键、版本号、创建时间等实例唯一字段
复制后应重新生成业务实例编号
原型注册表可以来自数据库、缓存、配置中心或 Spring Bean
复制规则复杂时，优先手写 copy()，不要完全依赖通用工具
原型对象尽量不要持有数据库连接、线程池、文件句柄等资源对象
```

如果业务模板需要持久化，可以把原型模板存储在数据库中，启动时加载到缓存，或者每次从数据库查询后复制。是否缓存取决于模板变更频率和读取压力。

## 常见问题

原型模式不是简单的 `BeanUtils.copyProperties()`。`BeanUtils.copyProperties()` 通常更偏字段复制，且对深拷贝、字段排除、业务默认值处理不够显式。原型模式强调的是“基于已有原型对象创建新对象”，复制规则是业务设计的一部分。

Spring 的 `@Scope("prototype")` 和 GoF 原型模式不是一回事。`@Scope("prototype")` 表示 Spring 容器每次获取 Bean 时创建一个新 Bean；GoF 原型模式表示对象自己具备复制能力，可以从已有对象复制出新对象。二者都和“多实例”有关，但使用场景不同。

原型模式不适合所有对象。对于简单 DTO、简单实体、只有两三个字段的对象，直接构造或使用构建者即可。只有当对象创建依赖已有模板、初始化复杂、复制价值明显时，原型模式才有意义。

## 总结

原型模式用于通过复制已有对象来创建新对象。它适合模板复制、配置复制、流程复制、规则复制等场景，能够减少重复初始化逻辑，并让对象创建过程更高效、更集中。

本示例的核心流程可以概括为：

```text
系统启动时初始化审批流程原型注册表
每个审批模板保存一份原型对象
调用方传入模板编码和业务信息
注册表根据模板编码找到原型对象
调用 copy() 复制出新的流程实例
业务层填充业务单号、申请人和自定义名称
接口返回新的审批流程实例
```

在真实 Spring Boot 项目中，原型模式常用于审批流、报表模板、营销活动模板、问卷模板、规则模板、导出任务模板等模块。它的核心价值是复用已有对象结构，并通过深拷贝避免模板和实例之间相互污染。
