# 桥接模式

桥接模式属于结构型模式，核心作用是把两个独立变化的维度拆开，让它们可以分别扩展，避免因为组合关系过多导致类爆炸。在当前设计模式文档体系中，桥接模式位于结构型模式分类下，重点用于“分离两个独立变化的维度”。

本文以 **JDK21 + Spring Boot 3** 后端项目为背景，通过“订单通知业务类型 + 通知发送渠道”的示例，说明桥接模式在真实项目中的落地方式。

## 基础配置

本示例模拟一个通知模块。系统中存在两个独立变化的维度：

```text
通知业务类型：订单支付成功通知、订单退款通知、库存预警通知
通知发送渠道：短信、邮件、企业微信
```

如果不用桥接模式，很容易写出下面这种类结构：

```text
OrderPaidSmsNotifier
OrderPaidEmailNotifier
OrderPaidWechatNotifier

RefundSmsNotifier
RefundEmailNotifier
RefundWechatNotifier

StockWarningSmsNotifier
StockWarningEmailNotifier
StockWarningWechatNotifier
```

业务类型有 3 个，发送渠道有 3 个，最终会出现 9 个组合类。后续如果再增加 2 个业务类型和 2 个发送渠道，类数量会继续膨胀。

桥接模式的处理方式是：通知业务类型只负责组织业务通知内容，发送渠道只负责具体发送。两者通过接口桥接起来，各自独立扩展。

本示例需要以下依赖。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验通知请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：用于字符串、集合、ID 等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖：用于单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

示例项目配置如下。真实项目中，短信、邮件、企业微信的配置应放到配置中心、密钥系统或环境变量中。

```yaml
server:
  port: 8080 # 示例服务端口

notice:
  email:
    from: notice@example.com # 模拟邮件发送方
  sms:
    sign-name: 示例商城 # 模拟短信签名
  wechat:
    robot-key: demo-robot-key # 模拟企业微信机器人 key
```

本示例的核心文件结构如下。

```text
src/main/java/io/github/atengk/designpattern/bridge
├── BridgeApplication.java
├── config
│   └── NoticeProperties.java
├── controller
│   └── NoticeController.java
├── dto
│   └── NoticeSendRequest.java
├── enums
│   ├── NoticeChannelType.java
│   └── NoticeSceneType.java
├── bridge
│   ├── NoticeChannel.java
│   ├── EmailNoticeChannel.java
│   ├── SmsNoticeChannel.java
│   ├── WechatNoticeChannel.java
│   ├── AbstractNoticeSender.java
│   ├── OrderPaidNoticeSender.java
│   ├── RefundNoticeSender.java
│   ├── StockWarningNoticeSender.java
│   ├── NoticeChannelRegistry.java
│   └── NoticeSenderRegistry.java
├── service
│   ├── NoticeService.java
│   └── NoticeServiceImpl.java
├── vo
│   ├── ApiResult.java
│   └── NoticeSendResultVO.java
└── web
    └── GlobalExceptionHandler.java
```

## 模式设计

桥接模式的关键不是“多态调用”本身，而是把两个变化维度拆成两个继承或实现体系。

在本示例中，两个维度分别是：

| 维度         | 抽象                   | 实现示例                                                     | 变化原因                   |
| ------------ | ---------------------- | ------------------------------------------------------------ | -------------------------- |
| 通知业务类型 | `AbstractNoticeSender` | `OrderPaidNoticeSender`、`RefundNoticeSender`、`StockWarningNoticeSender` | 业务场景增加或通知内容变化 |
| 通知发送渠道 | `NoticeChannel`        | `SmsNoticeChannel`、`EmailNoticeChannel`、`WechatNoticeChannel` | 通知渠道增加或发送协议变化 |

桥接关系体现在：`AbstractNoticeSender` 不直接发送短信、邮件或企业微信，而是持有 `NoticeChannel` 接口，由具体渠道实现真正发送。

核心流程如下。

```text
Controller
    ↓
NoticeService
    ↓
NoticeSenderRegistry 根据 scene 获取业务通知发送器
    ↓
NoticeChannelRegistry 根据 channel 获取发送渠道
    ↓
AbstractNoticeSender 组织通知内容
    ↓
NoticeChannel 完成具体发送
```

这种结构的好处是：

```text
新增业务通知类型：只新增一个 NoticeSender
新增发送渠道：只新增一个 NoticeChannel
业务通知类型和发送渠道可以自由组合
调用方只传 scene 和 channel，不关心内部组合类
```

## 核心代码

下面给出桥接模式在 Spring Boot 项目中的关键实现。示例代码重点展示结构拆分方式，不连接真实短信、邮件或企业微信平台。

项目启动类负责启动 Spring Boot 应用。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/BridgeApplication.java`

```java
package io.github.atengk.designpattern.bridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 桥接模式示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class BridgeApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BridgeApplication.class, args);
    }
}
```

通知配置类用于承接不同通知渠道的配置。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/config/NoticeProperties.java`

```java
package io.github.atengk.designpattern.bridge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 通知渠道配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@ConfigurationProperties(prefix = "notice")
public class NoticeProperties {

    /**
     * 邮件配置
     */
    private Email email = new Email();

    /**
     * 短信配置
     */
    private Sms sms = new Sms();

    /**
     * 企业微信配置
     */
    private Wechat wechat = new Wechat();

    /**
     * 邮件配置项
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class Email {

        /**
         * 邮件发送方
         */
        private String from;
    }

    /**
     * 短信配置项
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class Sms {

        /**
         * 短信签名
         */
        private String signName;
    }

    /**
     * 企业微信配置项
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class Wechat {

        /**
         * 企业微信机器人 key
         */
        private String robotKey;
    }
}
```

通知场景枚举表示业务通知类型，也就是桥接模式中的抽象维度之一。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/enums/NoticeSceneType.java`

```java
package io.github.atengk.designpattern.bridge.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

/**
 * 通知场景类型
 *
 * @author Ateng
 * @since 2026-05-13
 */
public enum NoticeSceneType {

    /**
     * 订单支付成功通知
     */
    ORDER_PAID,

    /**
     * 订单退款通知
     */
    REFUND,

    /**
     * 库存预警通知
     */
    STOCK_WARNING;

    /**
     * 根据场景编码解析通知场景
     *
     * @param scene 场景编码
     * @return 通知场景
     */
    public static NoticeSceneType parse(String scene) {
        if (StrUtil.isBlank(scene)) {
            throw new IllegalArgumentException("通知场景不能为空");
        }

        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.name(), scene))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的通知场景：{}", scene)));
    }
}
```

通知渠道枚举表示发送渠道，也就是桥接模式中的实现维度之一。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/enums/NoticeChannelType.java`

```java
package io.github.atengk.designpattern.bridge.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

/**
 * 通知渠道类型
 *
 * @author Ateng
 * @since 2026-05-13
 */
public enum NoticeChannelType {

    /**
     * 短信
     */
    SMS,

    /**
     * 邮件
     */
    EMAIL,

    /**
     * 企业微信
     */
    WECHAT;

    /**
     * 根据渠道编码解析通知渠道
     *
     * @param channel 渠道编码
     * @return 通知渠道
     */
    public static NoticeChannelType parse(String channel) {
        if (StrUtil.isBlank(channel)) {
            throw new IllegalArgumentException("通知渠道不能为空");
        }

        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.name(), channel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的通知渠道：{}", channel)));
    }
}
```

通知请求 DTO 统一承接外部请求参数，调用方通过 `scene` 和 `channel` 指定业务场景和发送渠道。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/dto/NoticeSendRequest.java`

```java
package io.github.atengk.designpattern.bridge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 通知发送请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class NoticeSendRequest {

    /**
     * 通知场景：ORDER_PAID、REFUND、STOCK_WARNING
     */
    @NotBlank(message = "通知场景不能为空")
    private String scene;

    /**
     * 通知渠道：SMS、EMAIL、WECHAT
     */
    @NotBlank(message = "通知渠道不能为空")
    private String channel;

    /**
     * 接收人，例如手机号、邮箱、企业微信用户 ID
     */
    @NotBlank(message = "接收人不能为空")
    private String receiver;

    /**
     * 业务参数，例如订单号、金额、商品名称、库存数量等
     */
    private Map<String, Object> params;
}
```

通知发送结果 VO 统一封装不同渠道的发送结果。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/vo/NoticeSendResultVO.java`

```java
package io.github.atengk.designpattern.bridge.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 通知发送结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class NoticeSendResultVO {

    /**
     * 通知场景
     */
    private String scene;

    /**
     * 通知渠道
     */
    private String channel;

    /**
     * 接收人
     */
    private String receiver;

    /**
     * 发送流水号
     */
    private String messageId;

    /**
     * 发送状态
     */
    private String status;

    /**
     * 返回消息
     */
    private String message;
}
```

统一 API 返回对象用于包装接口结果。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/vo/ApiResult.java`

```java
package io.github.atengk.designpattern.bridge.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 统一返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 业务状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @return API 返回对象
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败返回
     *
     * @param message 失败消息
     * @return API 返回对象
     */
    public static ApiResult<Void> fail(String message) {
        return ApiResult.<Void>builder()
                .code(500)
                .message(message)
                .build();
    }
}
```

`NoticeChannel` 是发送渠道接口，表示桥接模式中的实现接口。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/NoticeChannel.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import io.github.atengk.designpattern.bridge.enums.NoticeChannelType;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;

/**
 * 通知发送渠道接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface NoticeChannel {

    /**
     * 获取当前渠道类型
     *
     * @return 通知渠道类型
     */
    NoticeChannelType getChannel();

    /**
     * 发送通知
     *
     * @param scene    通知场景
     * @param receiver 接收人
     * @param title    通知标题
     * @param content  通知内容
     * @return 通知发送结果
     */
    NoticeSendResultVO send(String scene, String receiver, String title, String content);
}
```

短信渠道只负责短信发送，不关心业务通知内容来自订单、退款还是库存。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/SmsNoticeChannel.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.designpattern.bridge.config.NoticeProperties;
import io.github.atengk.designpattern.bridge.enums.NoticeChannelType;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信通知渠道
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsNoticeChannel implements NoticeChannel {

    private final NoticeProperties noticeProperties;

    /**
     * 获取当前渠道类型
     *
     * @return 通知渠道类型
     */
    @Override
    public NoticeChannelType getChannel() {
        return NoticeChannelType.SMS;
    }

    /**
     * 发送短信通知
     *
     * @param scene    通知场景
     * @param receiver 接收人
     * @param title    通知标题
     * @param content  通知内容
     * @return 通知发送结果
     */
    @Override
    public NoticeSendResultVO send(String scene, String receiver, String title, String content) {
        String messageId = "SMS_" + IdUtil.fastSimpleUUID();
        log.info("发送短信通知，signName={}，receiver={}，title={}，content={}",
                noticeProperties.getSms().getSignName(), receiver, title, content);

        return NoticeSendResultVO.builder()
                .scene(scene)
                .channel(getChannel().name())
                .receiver(receiver)
                .messageId(messageId)
                .status("SUCCESS")
                .message("短信通知发送成功")
                .build();
    }
}
```

邮件渠道只负责邮件发送，不参与业务场景判断。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/EmailNoticeChannel.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.designpattern.bridge.config.NoticeProperties;
import io.github.atengk.designpattern.bridge.enums.NoticeChannelType;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件通知渠道
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNoticeChannel implements NoticeChannel {

    private final NoticeProperties noticeProperties;

    /**
     * 获取当前渠道类型
     *
     * @return 通知渠道类型
     */
    @Override
    public NoticeChannelType getChannel() {
        return NoticeChannelType.EMAIL;
    }

    /**
     * 发送邮件通知
     *
     * @param scene    通知场景
     * @param receiver 接收人
     * @param title    通知标题
     * @param content  通知内容
     * @return 通知发送结果
     */
    @Override
    public NoticeSendResultVO send(String scene, String receiver, String title, String content) {
        String messageId = "EMAIL_" + IdUtil.fastSimpleUUID();
        log.info("发送邮件通知，from={}，receiver={}，title={}，content={}",
                noticeProperties.getEmail().getFrom(), receiver, title, content);

        return NoticeSendResultVO.builder()
                .scene(scene)
                .channel(getChannel().name())
                .receiver(receiver)
                .messageId(messageId)
                .status("SUCCESS")
                .message("邮件通知发送成功")
                .build();
    }
}
```

企业微信渠道只负责企业微信消息发送。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/WechatNoticeChannel.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.designpattern.bridge.config.NoticeProperties;
import io.github.atengk.designpattern.bridge.enums.NoticeChannelType;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 企业微信通知渠道
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatNoticeChannel implements NoticeChannel {

    private final NoticeProperties noticeProperties;

    /**
     * 获取当前渠道类型
     *
     * @return 通知渠道类型
     */
    @Override
    public NoticeChannelType getChannel() {
        return NoticeChannelType.WECHAT;
    }

    /**
     * 发送企业微信通知
     *
     * @param scene    通知场景
     * @param receiver 接收人
     * @param title    通知标题
     * @param content  通知内容
     * @return 通知发送结果
     */
    @Override
    public NoticeSendResultVO send(String scene, String receiver, String title, String content) {
        String messageId = "WECHAT_" + IdUtil.fastSimpleUUID();
        log.info("发送企业微信通知，robotKey={}，receiver={}，title={}，content={}",
                noticeProperties.getWechat().getRobotKey(), receiver, title, content);

        return NoticeSendResultVO.builder()
                .scene(scene)
                .channel(getChannel().name())
                .receiver(receiver)
                .messageId(messageId)
                .status("SUCCESS")
                .message("企业微信通知发送成功")
                .build();
    }
}
```

`AbstractNoticeSender` 是业务通知抽象类，表示桥接模式中的抽象部分。它负责业务通知模板组织，并通过 `NoticeChannel` 完成发送。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/AbstractNoticeSender.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.designpattern.bridge.dto.NoticeSendRequest;
import io.github.atengk.designpattern.bridge.enums.NoticeSceneType;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;

import java.util.Map;

/**
 * 通知发送抽象类
 *
 * @author Ateng
 * @since 2026-05-13
 */
public abstract class AbstractNoticeSender {

    /**
     * 获取当前通知场景
     *
     * @return 通知场景
     */
    public abstract NoticeSceneType getScene();

    /**
     * 构建通知标题
     *
     * @param params 业务参数
     * @return 通知标题
     */
    protected abstract String buildTitle(Map<String, Object> params);

    /**
     * 构建通知内容
     *
     * @param params 业务参数
     * @return 通知内容
     */
    protected abstract String buildContent(Map<String, Object> params);

    /**
     * 发送通知
     *
     * @param request 请求参数
     * @param channel 通知渠道
     * @return 通知发送结果
     */
    public NoticeSendResultVO send(NoticeSendRequest request, NoticeChannel channel) {
        Map<String, Object> params = MapUtil.emptyIfNull(request.getParams());
        String title = buildTitle(params);
        String content = buildContent(params);
        return channel.send(getScene().name(), request.getReceiver(), title, content);
    }
}
```

订单支付成功通知只关心“支付成功”这个业务场景下的标题和内容，不关心最终通过短信、邮件还是企业微信发送。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/OrderPaidNoticeSender.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.bridge.enums.NoticeSceneType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单支付成功通知发送器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class OrderPaidNoticeSender extends AbstractNoticeSender {

    /**
     * 获取当前通知场景
     *
     * @return 通知场景
     */
    @Override
    public NoticeSceneType getScene() {
        return NoticeSceneType.ORDER_PAID;
    }

    /**
     * 构建订单支付成功通知标题
     *
     * @param params 业务参数
     * @return 通知标题
     */
    @Override
    protected String buildTitle(Map<String, Object> params) {
        return "订单支付成功通知";
    }

    /**
     * 构建订单支付成功通知内容
     *
     * @param params 业务参数
     * @return 通知内容
     */
    @Override
    protected String buildContent(Map<String, Object> params) {
        String orderNo = Convert.toStr(params.get("orderNo"), "未知订单");
        String amount = Convert.toStr(params.get("amount"), "0.00");
        return StrUtil.format("您的订单 {} 已支付成功，支付金额 {} 元。", orderNo, amount);
    }
}
```

退款通知只处理退款业务内容，发送渠道仍然由外部桥接进来。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/RefundNoticeSender.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.bridge.enums.NoticeSceneType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单退款通知发送器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class RefundNoticeSender extends AbstractNoticeSender {

    /**
     * 获取当前通知场景
     *
     * @return 通知场景
     */
    @Override
    public NoticeSceneType getScene() {
        return NoticeSceneType.REFUND;
    }

    /**
     * 构建退款通知标题
     *
     * @param params 业务参数
     * @return 通知标题
     */
    @Override
    protected String buildTitle(Map<String, Object> params) {
        return "订单退款通知";
    }

    /**
     * 构建退款通知内容
     *
     * @param params 业务参数
     * @return 通知内容
     */
    @Override
    protected String buildContent(Map<String, Object> params) {
        String orderNo = Convert.toStr(params.get("orderNo"), "未知订单");
        String refundAmount = Convert.toStr(params.get("refundAmount"), "0.00");
        return StrUtil.format("您的订单 {} 已发起退款，退款金额 {} 元。", orderNo, refundAmount);
    }
}
```

库存预警通知表示另一个独立业务场景，仍然可以复用所有已有发送渠道。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/StockWarningNoticeSender.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.bridge.enums.NoticeSceneType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 库存预警通知发送器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class StockWarningNoticeSender extends AbstractNoticeSender {

    /**
     * 获取当前通知场景
     *
     * @return 通知场景
     */
    @Override
    public NoticeSceneType getScene() {
        return NoticeSceneType.STOCK_WARNING;
    }

    /**
     * 构建库存预警通知标题
     *
     * @param params 业务参数
     * @return 通知标题
     */
    @Override
    protected String buildTitle(Map<String, Object> params) {
        return "库存预警通知";
    }

    /**
     * 构建库存预警通知内容
     *
     * @param params 业务参数
     * @return 通知内容
     */
    @Override
    protected String buildContent(Map<String, Object> params) {
        String productName = Convert.toStr(params.get("productName"), "未知商品");
        Integer stock = Convert.toInt(params.get("stock"), 0);
        return StrUtil.format("商品 {} 当前库存为 {}，请及时补货。", productName, stock);
    }
}
```

通知渠道注册器负责按渠道类型获取具体发送渠道。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/NoticeChannelRegistry.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.designpattern.bridge.enums.NoticeChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 通知渠道注册器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NoticeChannelRegistry {

    private final Map<NoticeChannelType, NoticeChannel> channelMap;

    /**
     * 初始化通知渠道注册器
     *
     * @param channels 通知渠道列表
     */
    public NoticeChannelRegistry(List<NoticeChannel> channels) {
        if (CollUtil.isEmpty(channels)) {
            throw new IllegalStateException("未找到任何通知渠道");
        }

        Map<NoticeChannelType, NoticeChannel> tempMap = new EnumMap<>(NoticeChannelType.class);
        for (NoticeChannel channel : channels) {
            NoticeChannel oldChannel = tempMap.put(channel.getChannel(), channel);
            if (oldChannel != null) {
                throw new IllegalStateException("通知渠道重复注册：" + channel.getChannel());
            }
            log.info("通知渠道注册成功，channel={}，class={}", channel.getChannel(), channel.getClass().getSimpleName());
        }

        this.channelMap = Map.copyOf(tempMap);
    }

    /**
     * 根据渠道编码获取通知渠道
     *
     * @param channel 渠道编码
     * @return 通知渠道
     */
    public NoticeChannel getChannel(String channel) {
        NoticeChannelType channelType = NoticeChannelType.parse(channel);
        NoticeChannel noticeChannel = channelMap.get(channelType);

        if (noticeChannel == null) {
            throw new IllegalArgumentException("未找到通知渠道：" + channel);
        }

        return noticeChannel;
    }
}
```

通知发送器注册器负责按业务场景获取具体通知发送器。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/NoticeSenderRegistry.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.designpattern.bridge.enums.NoticeSceneType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 通知发送器注册器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class NoticeSenderRegistry {

    private final Map<NoticeSceneType, AbstractNoticeSender> senderMap;

    /**
     * 初始化通知发送器注册器
     *
     * @param senders 通知发送器列表
     */
    public NoticeSenderRegistry(List<AbstractNoticeSender> senders) {
        if (CollUtil.isEmpty(senders)) {
            throw new IllegalStateException("未找到任何通知发送器");
        }

        Map<NoticeSceneType, AbstractNoticeSender> tempMap = new EnumMap<>(NoticeSceneType.class);
        for (AbstractNoticeSender sender : senders) {
            AbstractNoticeSender oldSender = tempMap.put(sender.getScene(), sender);
            if (oldSender != null) {
                throw new IllegalStateException("通知发送器重复注册：" + sender.getScene());
            }
            log.info("通知发送器注册成功，scene={}，class={}", sender.getScene(), sender.getClass().getSimpleName());
        }

        this.senderMap = Map.copyOf(tempMap);
    }

    /**
     * 根据场景编码获取通知发送器
     *
     * @param scene 场景编码
     * @return 通知发送器
     */
    public AbstractNoticeSender getSender(String scene) {
        NoticeSceneType sceneType = NoticeSceneType.parse(scene);
        AbstractNoticeSender sender = senderMap.get(sceneType);

        if (sender == null) {
            throw new IllegalArgumentException("未找到通知发送器：" + scene);
        }

        return sender;
    }
}
```

通知服务接口定义统一业务入口。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/service/NoticeService.java`

```java
package io.github.atengk.designpattern.bridge.service;

import io.github.atengk.designpattern.bridge.dto.NoticeSendRequest;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;

/**
 * 通知服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface NoticeService {

    /**
     * 发送通知
     *
     * @param request 通知发送请求
     * @return 通知发送结果
     */
    NoticeSendResultVO send(NoticeSendRequest request);
}
```

通知服务实现类把业务场景和发送渠道桥接起来，完成最终发送。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/service/NoticeServiceImpl.java`

```java
package io.github.atengk.designpattern.bridge.service;

import io.github.atengk.designpattern.bridge.bridge.AbstractNoticeSender;
import io.github.atengk.designpattern.bridge.bridge.NoticeChannel;
import io.github.atengk.designpattern.bridge.bridge.NoticeChannelRegistry;
import io.github.atengk.designpattern.bridge.bridge.NoticeSenderRegistry;
import io.github.atengk.designpattern.bridge.dto.NoticeSendRequest;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeSenderRegistry noticeSenderRegistry;
    private final NoticeChannelRegistry noticeChannelRegistry;

    /**
     * 发送通知
     *
     * @param request 通知发送请求
     * @return 通知发送结果
     */
    @Override
    public NoticeSendResultVO send(NoticeSendRequest request) {
        log.info("开始发送通知，scene={}，channel={}，receiver={}",
                request.getScene(), request.getChannel(), request.getReceiver());

        AbstractNoticeSender sender = noticeSenderRegistry.getSender(request.getScene());
        NoticeChannel channel = noticeChannelRegistry.getChannel(request.getChannel());
        NoticeSendResultVO result = sender.send(request, channel);

        log.info("通知发送完成，scene={}，channel={}，receiver={}，messageId={}",
                result.getScene(), result.getChannel(), result.getReceiver(), result.getMessageId());

        return result;
    }
}
```

Controller 对外提供统一通知发送接口。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/controller/NoticeController.java`

```java
package io.github.atengk.designpattern.bridge.controller;

import io.github.atengk.designpattern.bridge.dto.NoticeSendRequest;
import io.github.atengk.designpattern.bridge.service.NoticeService;
import io.github.atengk.designpattern.bridge.vo.ApiResult;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 发送通知
     *
     * @param request 通知发送请求
     * @return 通知发送结果
     */
    @PostMapping("/send")
    public ApiResult<NoticeSendResultVO> send(@Valid @RequestBody NoticeSendRequest request) {
        return ApiResult.success(noticeService.send(request));
    }
}
```

全局异常处理器用于统一处理参数校验异常和业务异常。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/web/GlobalExceptionHandler.java`

```java
package io.github.atengk.designpattern.bridge.web;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.bridge.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return API 返回对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数不合法");

        log.warn("请求参数校验失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("请求参数错误，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理业务状态异常
     *
     * @param exception 业务状态异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException exception) {
        log.warn("业务处理失败，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }
}
```

## 使用方式

启动项目后，通过统一接口发送通知。调用方通过 `scene` 指定业务场景，通过 `channel` 指定发送渠道。

订单支付成功后，通过短信通知用户。

```bash
curl -X POST 'http://localhost:8080/api/notices/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "scene": "ORDER_PAID",
    "channel": "SMS",
    "receiver": "13800000000",
    "params": {
      "orderNo": "ORDER202605130001",
      "amount": "99.90"
    }
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "scene": "ORDER_PAID",
    "channel": "SMS",
    "receiver": "13800000000",
    "messageId": "SMS_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "status": "SUCCESS",
    "message": "短信通知发送成功"
  }
}
```

订单退款后，通过邮件通知用户。

```bash
curl -X POST 'http://localhost:8080/api/notices/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "scene": "REFUND",
    "channel": "EMAIL",
    "receiver": "user@example.com",
    "params": {
      "orderNo": "ORDER202605130002",
      "refundAmount": "66.60"
    }
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "scene": "REFUND",
    "channel": "EMAIL",
    "receiver": "user@example.com",
    "messageId": "EMAIL_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "status": "SUCCESS",
    "message": "邮件通知发送成功"
  }
}
```

库存预警时，通过企业微信通知运营人员。

```bash
curl -X POST 'http://localhost:8080/api/notices/send' \
  -H 'Content-Type: application/json' \
  -d '{
    "scene": "STOCK_WARNING",
    "channel": "WECHAT",
    "receiver": "operation-user",
    "params": {
      "productName": "JDK21 实战课程",
      "stock": 3
    }
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "scene": "STOCK_WARNING",
    "channel": "WECHAT",
    "receiver": "operation-user",
    "messageId": "WECHAT_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "status": "SUCCESS",
    "message": "企业微信通知发送成功"
  }
}
```

## 验证方式

可以从下面几个角度验证桥接模式是否落地成功。

第一，通知业务类型和通知渠道没有互相继承。`OrderPaidNoticeSender` 不继承 `SmsNoticeChannel`，`SmsNoticeChannel` 也不依赖订单业务。两者通过 `NoticeChannel` 接口组合。

第二，新增业务场景不需要新增所有渠道组合类。例如新增“优惠券到账通知”，只需要新增：

```text
CouponReceivedNoticeSender
```

它可以自动复用已有的短信、邮件和企业微信渠道。

第三，新增发送渠道不需要修改已有业务场景。例如新增“钉钉通知渠道”，只需要新增：

```text
DingTalkNoticeChannel
```

已有的订单支付通知、退款通知、库存预警通知都可以直接使用钉钉渠道。

第四，启动日志中应该能看到两个维度分别注册成功。

```text
通知发送器注册成功，scene=ORDER_PAID，class=OrderPaidNoticeSender
通知发送器注册成功，scene=REFUND，class=RefundNoticeSender
通知发送器注册成功，scene=STOCK_WARNING，class=StockWarningNoticeSender

通知渠道注册成功，channel=SMS，class=SmsNoticeChannel
通知渠道注册成功，channel=EMAIL，class=EmailNoticeChannel
通知渠道注册成功，channel=WECHAT，class=WechatNoticeChannel
```

## 扩展新业务场景

如果要新增“优惠券到账通知”，只需要增加一个通知场景发送器。

先在 `NoticeSceneType` 中新增枚举：

```java
COUPON_RECEIVED
```

然后新增业务通知发送器。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/CouponReceivedNoticeSender.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.bridge.enums.NoticeSceneType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 优惠券到账通知发送器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class CouponReceivedNoticeSender extends AbstractNoticeSender {

    /**
     * 获取当前通知场景
     *
     * @return 通知场景
     */
    @Override
    public NoticeSceneType getScene() {
        return NoticeSceneType.COUPON_RECEIVED;
    }

    /**
     * 构建优惠券到账通知标题
     *
     * @param params 业务参数
     * @return 通知标题
     */
    @Override
    protected String buildTitle(Map<String, Object> params) {
        return "优惠券到账通知";
    }

    /**
     * 构建优惠券到账通知内容
     *
     * @param params 业务参数
     * @return 通知内容
     */
    @Override
    protected String buildContent(Map<String, Object> params) {
        String couponName = Convert.toStr(params.get("couponName"), "优惠券");
        String expireTime = Convert.toStr(params.get("expireTime"), "未设置过期时间");
        return StrUtil.format("您收到一张 {}，有效期至 {}，请及时使用。", couponName, expireTime);
    }
}
```

新增后，不需要修改短信、邮件、企业微信渠道，也不需要修改 `NoticeServiceImpl`。新业务场景可以直接与已有渠道组合。

## 扩展新发送渠道

如果要新增钉钉通知渠道，只需要增加一个渠道实现。

先在 `NoticeChannelType` 中新增枚举：

```java
DING_TALK
```

然后新增钉钉通知渠道。

文件位置：`src/main/java/io/github/atengk/designpattern/bridge/bridge/DingTalkNoticeChannel.java`

```java
package io.github.atengk.designpattern.bridge.bridge;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.designpattern.bridge.enums.NoticeChannelType;
import io.github.atengk.designpattern.bridge.vo.NoticeSendResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉通知渠道
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class DingTalkNoticeChannel implements NoticeChannel {

    /**
     * 获取当前渠道类型
     *
     * @return 通知渠道类型
     */
    @Override
    public NoticeChannelType getChannel() {
        return NoticeChannelType.DING_TALK;
    }

    /**
     * 发送钉钉通知
     *
     * @param scene    通知场景
     * @param receiver 接收人
     * @param title    通知标题
     * @param content  通知内容
     * @return 通知发送结果
     */
    @Override
    public NoticeSendResultVO send(String scene, String receiver, String title, String content) {
        String messageId = "DING_" + IdUtil.fastSimpleUUID();
        log.info("发送钉钉通知，receiver={}，title={}，content={}", receiver, title, content);

        return NoticeSendResultVO.builder()
                .scene(scene)
                .channel(getChannel().name())
                .receiver(receiver)
                .messageId(messageId)
                .status("SUCCESS")
                .message("钉钉通知发送成功")
                .build();
    }
}
```

新增后，订单支付成功通知、退款通知、库存预警通知都可以通过 `channel=DING_TALK` 发送。已有业务通知发送器不需要修改。

## 适用场景

桥接模式适合存在两个或多个独立变化维度的场景。判断是否适合使用桥接模式，可以看是否出现了“组合类爆炸”的趋势。

常见 Spring Boot 项目场景如下。

| 场景     | 抽象维度     | 实现维度                            |
| -------- | ------------ | ----------------------------------- |
| 通知系统 | 业务通知类型 | 短信、邮件、站内信、企业微信、钉钉  |
| 报表导出 | 报表类型     | Excel、CSV、PDF、对象存储、本地文件 |
| 文件处理 | 业务文件类型 | MinIO、OSS、COS、S3                 |
| 支付能力 | 支付业务动作 | 支付宝、微信、银联、Stripe          |
| 消息推送 | 推送场景     | Kafka、RabbitMQ、RocketMQ           |
| 数据同步 | 同步任务类型 | HTTP、MQ、数据库、文件              |

桥接模式尤其适合“业务能力”和“技术实现”都经常变化的模块。它能让业务维度和技术维度分别演进，不互相污染。

## 和其他模式的区别

桥接模式容易和适配器模式、策略模式、装饰器模式混淆。区分时重点看模式解决的问题。

| 模式       | 关注点                 | 和桥接模式的区别                           |
| ---------- | ---------------------- | ------------------------------------------ |
| 桥接模式   | 拆分独立变化维度       | 一开始就为了避免多个维度组合膨胀           |
| 适配器模式 | 兼容已有不一致接口     | 通常是让旧接口或第三方接口符合当前系统接口 |
| 策略模式   | 替换一组算法或业务处理 | 通常只有一个变化维度，重点是选择不同策略   |
| 装饰器模式 | 动态增强对象能力       | 重点是功能叠加，不是拆分两个独立维度       |
| 外观模式   | 简化复杂子系统调用     | 重点是给外部提供统一入口，不是维度拆分     |

本示例中，通知业务类型和通知渠道是两个独立变化维度，所以更适合归类为桥接模式。如果只有“不同渠道发送通知”一个变化点，则更像策略模式。

## 注意事项

桥接模式不适合简单场景。如果系统只有一个业务类型和两个渠道，直接写两个实现类可能更清晰。只有当多个维度都可能持续扩展时，桥接模式才有明显收益。

桥接模式的抽象接口要稳定。比如 `NoticeChannel.send()` 应该表达“发送通知”这个稳定能力，不应该把某个具体渠道的字段直接暴露出来，例如 `templateCode`、`robotKey`、`emailCc` 等。渠道专属参数应封装在渠道内部配置或上下文对象中。

不要把业务规则放到渠道实现中。短信、邮件、企业微信只负责发送，不应该判断订单状态、退款状态或库存阈值。业务规则应该放在业务服务、领域服务或具体通知发送器中。

注册器要做好重复注册检查。如果两个类都声明支持同一个 `NoticeSceneType` 或 `NoticeChannelType`，启动阶段应直接失败，避免运行时出现不可预测行为。

如果通知内容模板越来越复杂，可以把 `buildTitle()` 和 `buildContent()` 进一步拆成模板服务、模板引擎或数据库模板配置。桥接模式不要求所有内容都写在 Java 类中。

## 总结

桥接模式的关键价值是把两个独立变化的维度拆开，让它们通过接口组合，而不是通过大量继承类硬编码组合关系。

在本示例中：

```text
通知业务类型是一条变化轴
通知发送渠道是另一条变化轴
AbstractNoticeSender 负责业务内容组织
NoticeChannel 负责具体渠道发送
NoticeServiceImpl 负责把两条变化轴桥接起来
```

最终效果是：

```text
新增业务场景不影响已有发送渠道
新增发送渠道不影响已有业务场景
避免 OrderSms、OrderEmail、RefundSms、RefundEmail 这类组合类膨胀
业务维度和技术实现维度边界更清晰
代码更容易扩展、测试和维护
```
