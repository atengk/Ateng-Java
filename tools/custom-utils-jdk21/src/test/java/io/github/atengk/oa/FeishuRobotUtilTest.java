package io.github.atengk.oa;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.utils.oa.FeishuRobotUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 飞书自定义机器人工具类测试。
 *
 * @author Ateng
 * @since 2026-05-14
 */
class FeishuRobotUtilTest {

    private static final String TEST_ENABLED = "FEISHU_ROBOT_TEST_ENABLED";

    private static final String ROBOT_TOKEN = "FEISHU_ROBOT_TOKEN";

    private static final String WEBHOOK = "FEISHU_ROBOT_WEBHOOK";

    private static final String SECRET = "FEISHU_ROBOT_SECRET";

    private static final String TENANT_ACCESS_TOKEN = "FEISHU_TENANT_ACCESS_TOKEN";

    private static final String IMAGE_KEY = "FEISHU_IMAGE_KEY";

    private static final String PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";

    @Test
    @DisplayName("builder：创建飞书机器人客户端")
    void builder() {
        FeishuRobotUtil robot = FeishuRobotUtil.builder()
                .robotToken("test-token")
                .secret("test-secret")
                .tenantAccessToken("tenant-access-token")
                .timeoutMillis(3000)
                .throwExceptionOnFail(false)
                .build();

        assertNotNull(robot);
    }

    @Test
    @DisplayName("ofRobotToken：使用机器人token创建客户端")
    void ofRobotToken() {
        FeishuRobotUtil robot = FeishuRobotUtil.ofRobotToken("test-token");

        assertNotNull(robot);
    }

    @Test
    @DisplayName("ofRobotToken：使用机器人token和secret创建客户端")
    void ofRobotTokenWithSecret() {
        FeishuRobotUtil robot = FeishuRobotUtil.ofRobotToken("test-token", "test-secret");

        assertNotNull(robot);
    }

    @Test
    @DisplayName("ofWebhook：使用完整webhook创建客户端")
    void ofWebhook() {
        FeishuRobotUtil robot = FeishuRobotUtil.ofWebhook(
                "https://open.feishu.cn/open-apis/bot/v2/hook/test-token"
        );

        assertNotNull(robot);
    }

    @Test
    @DisplayName("ofWebhook：使用完整webhook和secret创建客户端")
    void ofWebhookWithSecret() {
        FeishuRobotUtil robot = FeishuRobotUtil.ofWebhook(
                "https://open.feishu.cn/open-apis/bot/v2/hook/test-token",
                "test-secret"
        );

        assertNotNull(robot);
    }

    @Test
    @DisplayName("sendText：发送普通文本消息")
    void sendText() {
        FeishuRobotUtil robot = realRobot();

        FeishuRobotUtil.FeishuRobotResult result = robot.sendText("飞书机器人测试：普通文本消息");

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("sendText：发送带@配置的文本消息")
    void sendTextWithMentions() {
        FeishuRobotUtil robot = realRobot();

        FeishuRobotUtil.FeishuRobotResult result = robot.sendText(
                "飞书机器人测试：带@配置的文本消息",
                List.of(FeishuRobotUtil.Mention.all())
        );

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("sendPost：发送中文富文本消息")
    void sendPost() {
        FeishuRobotUtil robot = realRobot();

        FeishuRobotUtil.FeishuRobotResult result = robot.sendPost(
                "飞书富文本测试",
                List.of(
                        List.of(FeishuRobotUtil.PostElement.text("飞书机器人测试：中文富文本消息")),
                        List.of(FeishuRobotUtil.PostElement.link("示例链接", "https://example.com"))
                )
        );

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("sendPost：发送指定语言富文本消息")
    void sendPostWithLanguage() {
        FeishuRobotUtil robot = realRobot();

        FeishuRobotUtil.FeishuRobotResult result = robot.sendPost(
                "zh_cn",
                "飞书富文本测试",
                List.of(
                        List.of(FeishuRobotUtil.PostElement.text("类型：指定语言富文本")),
                        List.of(FeishuRobotUtil.PostElement.text("状态：成功"))
                )
        );

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("sendImageByKey：根据image_key发送图片消息")
    void sendImageByKey() {
        FeishuRobotUtil robot = realRobot();
        String imageKey = System.getenv(IMAGE_KEY);

        Assumptions.assumeTrue(StrUtil.isNotBlank(imageKey), "请配置 FEISHU_IMAGE_KEY");

        FeishuRobotUtil.FeishuRobotResult result = robot.sendImageByKey(imageKey);

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("uploadImage：使用默认tenant_access_token上传图片")
    void uploadImage() {
        FeishuRobotUtil robot = realRobotWithTenantAccessToken();
        File imageFile = createTempPng();

        FeishuRobotUtil.FeishuImageResult result = robot.uploadImage(imageFile);

        assertTrue(result.success(), result.msg());
        assertTrue(StrUtil.isNotBlank(result.imageKey()));
    }

    @Test
    @DisplayName("uploadImage：使用指定tenant_access_token上传图片")
    void uploadImageWithTenantAccessToken() {
        FeishuRobotUtil robot = realRobot();
        String tenantAccessToken = System.getenv(TENANT_ACCESS_TOKEN);
        File imageFile = createTempPng();

        Assumptions.assumeTrue(StrUtil.isNotBlank(tenantAccessToken), "请配置 FEISHU_TENANT_ACCESS_TOKEN");

        FeishuRobotUtil.FeishuImageResult result = robot.uploadImage(imageFile, tenantAccessToken);

        assertTrue(result.success(), result.msg());
        assertTrue(StrUtil.isNotBlank(result.imageKey()));
    }

    @Test
    @DisplayName("sendImage：上传本地图片并发送")
    void sendImage() {
        FeishuRobotUtil robot = realRobotWithTenantAccessToken();
        File imageFile = createTempPng();

        FeishuRobotUtil.FeishuRobotResult result = robot.sendImage(imageFile);

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("sendInteractiveCard：发送交互卡片消息")
    void sendInteractiveCard() {
        FeishuRobotUtil robot = realRobot();

        JSONObject card = FeishuRobotUtil.buildAlarmCard(
                "飞书交互卡片测试",
                "order-service",
                "INFO",
                "这是一条交互卡片测试消息",
                "https://example.com"
        );

        FeishuRobotUtil.FeishuRobotResult result = robot.sendInteractiveCard(card);

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("sendAlarmCard：发送告警卡片消息")
    void sendAlarmCard() {
        FeishuRobotUtil robot = realRobot();

        FeishuRobotUtil.FeishuRobotResult result = robot.sendAlarmCard(
                "服务告警",
                "order-service",
                "ERROR",
                "订单创建接口异常",
                "https://example.com"
        );

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("send：发送自定义JSON消息")
    void send() {
        FeishuRobotUtil robot = realRobot();

        FeishuRobotUtil.FeishuRobotResult result = robot.send(JSONUtil.createObj()
                .set("msg_type", "text")
                .set("content", JSONUtil.createObj()
                        .set("text", "飞书机器人测试：自定义JSON消息")));

        assertTrue(result.success(), result.msg());
    }

    @Test
    @DisplayName("buildAlarmPostLines：构建告警富文本内容")
    void buildAlarmPostLines() {
        List<List<FeishuRobotUtil.PostElement>> lines = FeishuRobotUtil.buildAlarmPostLines(
                "服务告警",
                "order-service",
                "ERROR",
                "订单创建接口异常"
        );

        assertFalse(lines.isEmpty());
        assertEquals(5, lines.size());
    }

    @Test
    @DisplayName("buildAlarmCard：构建告警交互卡片")
    void buildAlarmCard() {
        JSONObject card = FeishuRobotUtil.buildAlarmCard(
                "服务告警",
                "order-service",
                "ERROR",
                "订单创建接口异常",
                "https://example.com"
        );

        assertEquals("red", card.getJSONObject("header").getStr("template"));
        assertFalse(card.getJSONArray("elements").isEmpty());
    }

    @Test
    @DisplayName("createSign：创建飞书机器人签名")
    void createSign() {
        String sign = FeishuRobotUtil.createSign(1710000000L, "test-secret");

        assertNotNull(sign);
        assertFalse(sign.isBlank());
    }

    @Test
    @DisplayName("Mention.user：创建@指定用户配置")
    void mentionUser() {
        FeishuRobotUtil.Mention mention = FeishuRobotUtil.Mention.user("ou_xxx", "张三");

        assertEquals("ou_xxx", mention.userId());
        assertEquals("张三", mention.userName());
    }

    @Test
    @DisplayName("Mention.all：创建@所有人配置")
    void mentionAll() {
        FeishuRobotUtil.Mention mention = FeishuRobotUtil.Mention.all();

        assertEquals("all", mention.userId());
        assertEquals("所有人", mention.userName());
    }

    @Test
    @DisplayName("PostElement.text：创建文本富文本元素")
    void postElementText() {
        FeishuRobotUtil.PostElement element = FeishuRobotUtil.PostElement.text("文本内容");

        assertEquals("text", element.tag());
        assertEquals("文本内容", element.text());
    }

    @Test
    @DisplayName("PostElement.link：创建链接富文本元素")
    void postElementLink() {
        FeishuRobotUtil.PostElement element = FeishuRobotUtil.PostElement.link("链接", "https://example.com");

        assertEquals("a", element.tag());
        assertEquals("链接", element.text());
        assertEquals("https://example.com", element.href());
    }

    @Test
    @DisplayName("PostElement.at：创建@富文本元素")
    void postElementAt() {
        FeishuRobotUtil.PostElement element = FeishuRobotUtil.PostElement.at(
                FeishuRobotUtil.Mention.user("ou_xxx", "张三")
        );

        assertEquals("at", element.tag());
        assertEquals("ou_xxx", element.userId());
        assertEquals("张三", element.userName());
    }

    @Test
    @DisplayName("PostElement.image：创建图片富文本元素")
    void postElementImage() {
        FeishuRobotUtil.PostElement element = FeishuRobotUtil.PostElement.image("img_xxx");

        assertEquals("img", element.tag());
        assertEquals("img_xxx", element.imageKey());
    }

    @Test
    @DisplayName("FeishuRobotResult.parse：解析飞书机器人响应")
    void feishuRobotResultParse() {
        FeishuRobotUtil.FeishuRobotResult result = FeishuRobotUtil.FeishuRobotResult.parse(
                200,
                "{\"code\":0,\"msg\":\"success\"}"
        );

        assertTrue(result.success());
        assertEquals(0, result.code());
        assertEquals("success", result.msg());
    }

    @Test
    @DisplayName("FeishuImageResult.parse：解析飞书图片上传响应")
    void feishuImageResultParse() {
        FeishuRobotUtil.FeishuImageResult result = FeishuRobotUtil.FeishuImageResult.parse(
                200,
                "{\"code\":0,\"msg\":\"success\",\"data\":{\"image_key\":\"img_xxx\"}}"
        );

        assertTrue(result.success());
        assertEquals("img_xxx", result.imageKey());
    }

    private FeishuRobotUtil realRobot() {
        Assumptions.assumeTrue(
                StrUtil.equalsIgnoreCase(System.getenv(TEST_ENABLED), "true"),
                "未启用飞书机器人集成测试，请配置 FEISHU_ROBOT_TEST_ENABLED=true"
        );

        String robotToken = System.getenv(ROBOT_TOKEN);
        String webhook = System.getenv(WEBHOOK);
        String secret = System.getenv(SECRET);

        Assumptions.assumeTrue(
                StrUtil.isNotBlank(robotToken) || StrUtil.isNotBlank(webhook),
                "请配置 FEISHU_ROBOT_TOKEN 或 FEISHU_ROBOT_WEBHOOK"
        );

        FeishuRobotUtil.Builder builder = FeishuRobotUtil.builder()
                .secret(secret)
                .timeoutMillis(8000)
                .throwExceptionOnFail(false);

        if (StrUtil.isNotBlank(webhook)) {
            builder.webhook(webhook);
        } else {
            builder.robotToken(robotToken);
        }

        return builder.build();
    }

    private FeishuRobotUtil realRobotWithTenantAccessToken() {
        String tenantAccessToken = System.getenv(TENANT_ACCESS_TOKEN);

        Assumptions.assumeTrue(StrUtil.isNotBlank(tenantAccessToken), "请配置 FEISHU_TENANT_ACCESS_TOKEN");

        String robotToken = System.getenv(ROBOT_TOKEN);
        String webhook = System.getenv(WEBHOOK);
        String secret = System.getenv(SECRET);

        Assumptions.assumeTrue(
                StrUtil.isNotBlank(robotToken) || StrUtil.isNotBlank(webhook),
                "请配置 FEISHU_ROBOT_TOKEN 或 FEISHU_ROBOT_WEBHOOK"
        );

        FeishuRobotUtil.Builder builder = FeishuRobotUtil.builder()
                .secret(secret)
                .tenantAccessToken(tenantAccessToken)
                .timeoutMillis(10000)
                .throwExceptionOnFail(false);

        if (StrUtil.isNotBlank(webhook)) {
            builder.webhook(webhook);
        } else {
            builder.robotToken(robotToken);
        }

        return builder.build();
    }

    private File createTempPng() {
        File file = FileUtil.createTempFile("feishu-robot-image-", ".png", true);
        FileUtil.writeBytes(Base64.decode(PNG_BASE64), file);
        return file;
    }
}
