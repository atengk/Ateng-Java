package io.github.atengk.oa;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.utils.oa.DingTalkRobotUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 钉钉自定义机器人工具类测试。
 *
 * @author Ateng
 * @since 2026-05-14
 */
class DingTalkRobotUtilTest {

    private static final String TEST_ENABLED = "DINGTALK_ROBOT_TEST_ENABLED";

    private static final String ACCESS_TOKEN = "DINGTALK_ROBOT_ACCESS_TOKEN";

    private static final String WEBHOOK = "DINGTALK_ROBOT_WEBHOOK";

    private static final String SECRET = "DINGTALK_ROBOT_SECRET";

    @Test
    @DisplayName("builder：创建钉钉机器人客户端")
    void builder() {
        DingTalkRobotUtil robot = DingTalkRobotUtil.builder()
                .accessToken("test-token")
                .secret("test-secret")
                .timeoutMillis(3000)
                .throwExceptionOnFail(false)
                .build();

        String requestUrl = robot.buildRequestUrl();

        assertNotNull(robot);
        assertTrue(requestUrl.contains("access_token=test-token"));
        assertTrue(requestUrl.contains("timestamp="));
        assertTrue(requestUrl.contains("sign="));
    }

    @Test
    @DisplayName("ofWebhook：使用完整webhook创建客户端")
    void ofWebhook() {
        String webhook = "https://oapi.dingtalk.com/robot/send?access_token=test-token";

        DingTalkRobotUtil robot = DingTalkRobotUtil.ofWebhook(webhook);

        assertNotNull(robot);
        assertEquals(webhook, robot.buildRequestUrl());
    }

    @Test
    @DisplayName("ofWebhook：使用完整webhook和secret创建客户端")
    void ofWebhookWithSecret() {
        DingTalkRobotUtil robot = DingTalkRobotUtil.ofWebhook(
                "https://oapi.dingtalk.com/robot/send?access_token=test-token",
                "test-secret"
        );

        String requestUrl = robot.buildRequestUrl();

        assertNotNull(robot);
        assertTrue(requestUrl.contains("access_token=test-token"));
        assertTrue(requestUrl.contains("timestamp="));
        assertTrue(requestUrl.contains("sign="));
    }

    @Test
    @DisplayName("ofAccessToken：使用access_token创建客户端")
    void ofAccessToken() {
        DingTalkRobotUtil robot = DingTalkRobotUtil.ofAccessToken("test-token");

        String requestUrl = robot.buildRequestUrl();

        assertNotNull(robot);
        assertTrue(requestUrl.contains("access_token=test-token"));
    }

    @Test
    @DisplayName("ofAccessToken：使用access_token和secret创建客户端")
    void ofAccessTokenWithSecret() {
        DingTalkRobotUtil robot = DingTalkRobotUtil.ofAccessToken("test-token", "test-secret");

        String requestUrl = robot.buildRequestUrl();

        assertNotNull(robot);
        assertTrue(requestUrl.contains("access_token=test-token"));
        assertTrue(requestUrl.contains("timestamp="));
        assertTrue(requestUrl.contains("sign="));
    }

    @Test
    @DisplayName("sendText：发送普通文本消息")
    void sendText() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.sendText("钉钉机器人测试：普通文本消息");

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendText：发送带@配置的文本消息")
    void sendTextWithAt() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.sendText(
                "钉钉机器人测试：带@配置的文本消息",
                DingTalkRobotUtil.At.none()
        );

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendMarkdown：发送Markdown消息")
    void sendMarkdown() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.sendMarkdown(
                "钉钉Markdown测试",
                "### 钉钉机器人测试\n\n- 类型：Markdown\n- 状态：成功"
        );

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendMarkdown：发送带@配置的Markdown消息")
    void sendMarkdownWithAt() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.sendMarkdown(
                "钉钉Markdown测试",
                "### 钉钉机器人测试\n\n- 类型：Markdown + At\n- 状态：成功",
                DingTalkRobotUtil.At.none()
        );

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendLink：发送链接消息")
    void sendLink() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.sendLink(
                "钉钉链接测试",
                "这是一条钉钉机器人链接消息",
                "https://example.com",
                ""
        );

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendSingleActionCard：发送单按钮ActionCard消息")
    void sendSingleActionCard() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.sendSingleActionCard(
                "钉钉ActionCard测试",
                "### 单按钮ActionCard\n\n这是一条测试消息。",
                "查看详情",
                "https://example.com"
        );

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendMultiActionCard：发送多按钮ActionCard消息")
    void sendMultiActionCard() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.sendMultiActionCard(
                "钉钉多按钮ActionCard测试",
                "### 多按钮ActionCard\n\n请选择操作。",
                true,
                List.of(
                        DingTalkRobotUtil.ActionButton.of("查看日志", "https://example.com/logs"),
                        DingTalkRobotUtil.ActionButton.of("查看监控", "https://example.com/metrics")
                )
        );

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendFeedCard：发送FeedCard消息")
    void sendFeedCard() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.sendFeedCard(List.of(
                DingTalkRobotUtil.FeedLink.of(
                        "钉钉FeedCard测试",
                        "https://example.com",
                        "https://img.alicdn.com/imgextra/i4/O1CN01yf6W8k1Z9uAvF2cY8_!!6000000003154-2-tps-240-240.png"
                )
        ));

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("send：发送自定义JSON消息")
    void send() {
        DingTalkRobotUtil robot = realRobot();

        DingTalkRobotUtil.DingTalkRobotResult result = robot.send(JSONUtil.createObj()
                .set("msgtype", "text")
                .set("text", JSONUtil.createObj()
                        .set("content", "钉钉机器人测试：自定义JSON消息")));

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("buildRequestUrl：构建请求地址")
    void buildRequestUrl() {
        DingTalkRobotUtil robot = DingTalkRobotUtil.ofAccessToken("test-token", "test-secret");

        String requestUrl = robot.buildRequestUrl();

        assertTrue(requestUrl.contains("access_token=test-token"));
        assertTrue(requestUrl.contains("timestamp="));
        assertTrue(requestUrl.contains("sign="));
    }

    @Test
    @DisplayName("createSign：创建钉钉机器人签名")
    void createSign() {
        String sign = DingTalkRobotUtil.createSign(1710000000000L, "test-secret");

        assertNotNull(sign);
        assertFalse(sign.isBlank());
    }

    @Test
    @DisplayName("buildAlarmMarkdown：构建告警Markdown内容")
    void buildAlarmMarkdown() {
        String markdown = DingTalkRobotUtil.buildAlarmMarkdown(
                "服务告警",
                "order-service",
                "ERROR",
                "订单创建接口异常"
        );

        assertTrue(markdown.contains("服务告警"));
        assertTrue(markdown.contains("order-service"));
        assertTrue(markdown.contains("ERROR"));
        assertTrue(markdown.contains("订单创建接口异常"));
    }

    @Test
    @DisplayName("At.none：不@任何人")
    void atNone() {
        DingTalkRobotUtil.At at = DingTalkRobotUtil.At.none();

        assertFalse(at.atAll());
        assertTrue(at.atMobiles().isEmpty());
        assertTrue(at.atUserIds().isEmpty());
    }

    @Test
    @DisplayName("At.all：@所有人")
    void atAll() {
        DingTalkRobotUtil.At at = DingTalkRobotUtil.At.all();

        assertTrue(at.atAll());
    }

    @Test
    @DisplayName("At.mobiles：按手机号@用户")
    void atMobiles() {
        DingTalkRobotUtil.At at = DingTalkRobotUtil.At.mobiles("13800000000", "13800000000", " ");

        assertEquals(1, at.atMobiles().size());
        assertEquals("13800000000", at.atMobiles().getFirst());
    }

    @Test
    @DisplayName("At.userIds：按用户ID@用户")
    void atUserIds() {
        DingTalkRobotUtil.At at = DingTalkRobotUtil.At.userIds("user001", "user001", " ");

        assertEquals(1, at.atUserIds().size());
        assertEquals("user001", at.atUserIds().getFirst());
    }

    @Test
    @DisplayName("ActionButton.of：创建ActionCard按钮")
    void actionButtonOf() {
        DingTalkRobotUtil.ActionButton button = DingTalkRobotUtil.ActionButton.of("查看详情", "https://example.com");

        assertEquals("查看详情", button.title());
        assertEquals("https://example.com", button.actionUrl());
    }

    @Test
    @DisplayName("FeedLink.of：创建FeedCard链接")
    void feedLinkOf() {
        DingTalkRobotUtil.FeedLink link = DingTalkRobotUtil.FeedLink.of(
                "标题",
                "https://example.com",
                "https://example.com/a.png"
        );

        assertEquals("标题", link.title());
        assertEquals("https://example.com", link.messageUrl());
        assertEquals("https://example.com/a.png", link.picUrl());
    }

    @Test
    @DisplayName("DingTalkRobotResult.parse：解析钉钉响应")
    void dingTalkRobotResultParse() {
        DingTalkRobotUtil.DingTalkRobotResult result = DingTalkRobotUtil.DingTalkRobotResult.parse(
                200,
                "{\"errcode\":0,\"errmsg\":\"ok\"}"
        );

        assertTrue(result.success());
        assertEquals(0, result.errcode());
        assertEquals("ok", result.errmsg());
    }

    private DingTalkRobotUtil realRobot() {
        Assumptions.assumeTrue(
                StrUtil.equalsIgnoreCase(System.getenv(TEST_ENABLED), "true"),
                "未启用钉钉机器人集成测试，请配置 DINGTALK_ROBOT_TEST_ENABLED=true"
        );

        String accessToken = System.getenv(ACCESS_TOKEN);
        String webhook = System.getenv(WEBHOOK);
        String secret = System.getenv(SECRET);

        Assumptions.assumeTrue(
                StrUtil.isNotBlank(accessToken) || StrUtil.isNotBlank(webhook),
                "请配置 DINGTALK_ROBOT_ACCESS_TOKEN 或 DINGTALK_ROBOT_WEBHOOK"
        );

        DingTalkRobotUtil.Builder builder = DingTalkRobotUtil.builder()
                .secret(secret)
                .timeoutMillis(8000)
                .throwExceptionOnFail(false);

        if (StrUtil.isNotBlank(webhook)) {
            builder.webhook(webhook);
        } else {
            builder.accessToken(accessToken);
        }

        return builder.build();
    }
}
