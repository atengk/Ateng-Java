package io.github.atengk.oa;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.utils.oa.WeComRobotUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 企业微信群机器人工具类测试。
 *
 * @author Ateng
 * @since 2026-05-14
 */
class WeComRobotUtilTest {

    private static final String TEST_ENABLED = "WECOM_ROBOT_TEST_ENABLED";

    private static final String ROBOT_KEY = "WECOM_ROBOT_KEY";

    private static final String WEBHOOK = "WECOM_ROBOT_WEBHOOK";

    private static final String PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";

    @Test
    @DisplayName("builder：创建企业微信机器人客户端")
    void builder() {
        WeComRobotUtil robot = WeComRobotUtil.builder()
                .robotKey("test-key")
                .timeoutMillis(3000)
                .throwExceptionOnFail(false)
                .build();

        assertNotNull(robot);
    }

    @Test
    @DisplayName("ofKey：使用机器人key创建客户端")
    void ofKey() {
        WeComRobotUtil robot = WeComRobotUtil.ofKey("test-key");

        assertNotNull(robot);
    }

    @Test
    @DisplayName("ofWebhook：使用完整webhook创建客户端")
    void ofWebhook() {
        WeComRobotUtil robot = WeComRobotUtil.ofWebhook(
                "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test-key"
        );

        assertNotNull(robot);
    }

    @Test
    @DisplayName("sendText：发送普通文本消息")
    void sendText() {
        WeComRobotUtil robot = realRobot();

        WeComRobotUtil.WeComRobotResult result = robot.sendText("企业微信机器人测试：普通文本消息");

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendText：发送带@配置的文本消息")
    void sendTextWithMention() {
        WeComRobotUtil robot = realRobot();

        WeComRobotUtil.WeComRobotResult result = robot.sendText(
                "企业微信机器人测试：带@配置的文本消息",
                WeComRobotUtil.Mention.none()
        );

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendMarkdown：发送Markdown消息")
    void sendMarkdown() {
        WeComRobotUtil robot = realRobot();

        WeComRobotUtil.WeComRobotResult result = robot.sendMarkdown("""
                ### 企业微信机器人测试
                > 类型：<font color="info">Markdown</font>
                > 状态：<font color="info">成功</font>
                """);

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendImage：通过文件发送图片消息")
    void sendImageWithFile() {
        WeComRobotUtil robot = realRobot();
        File imageFile = createTempPng();

        WeComRobotUtil.WeComRobotResult result = robot.sendImage(imageFile);

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendImage：通过base64和md5发送图片消息")
    void sendImageWithBase64AndMd5() {
        WeComRobotUtil robot = realRobot();
        byte[] imageBytes = Base64.decode(PNG_BASE64);
        String md5 = DigestUtil.md5Hex(imageBytes);

        WeComRobotUtil.WeComRobotResult result = robot.sendImage(PNG_BASE64, md5);

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendNews：发送图文消息")
    void sendNews() {
        WeComRobotUtil robot = realRobot();

        WeComRobotUtil.WeComRobotResult result = robot.sendNews(List.of(
                WeComRobotUtil.Article.of(
                        "企业微信图文测试",
                        "这是一条企业微信机器人图文消息",
                        "https://example.com",
                        "https://res.mail.qq.com/node/ww/wwopenmng/images/independent/doc/test_pic_msg1.png"
                )
        ));

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("uploadFile：上传文件到企业微信群机器人临时素材")
    void uploadFile() {
        WeComRobotUtil robot = realRobot();
        File file = createTempTextFile();

        WeComRobotUtil.WeComMediaResult result = robot.uploadFile(file);

        assertTrue(result.success(), result.errmsg());
        assertTrue(StrUtil.isNotBlank(result.mediaId()));
    }

    @Test
    @DisplayName("sendFile：上传并发送文件消息")
    void sendFile() {
        WeComRobotUtil robot = realRobot();
        File file = createTempTextFile();

        WeComRobotUtil.WeComRobotResult result = robot.sendFile(file);

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendFileByMediaId：根据media_id发送文件消息")
    void sendFileByMediaId() {
        WeComRobotUtil robot = realRobot();
        File file = createTempTextFile();

        WeComRobotUtil.WeComMediaResult mediaResult = robot.uploadFile(file);
        assertTrue(mediaResult.success(), mediaResult.errmsg());

        WeComRobotUtil.WeComRobotResult result = robot.sendFileByMediaId(mediaResult.mediaId());

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("sendTemplateCard：发送模板卡片消息")
    void sendTemplateCard() {
        WeComRobotUtil robot = realRobot();

        JSONObject templateCard = JSONUtil.createObj()
                .set("card_type", "text_notice")
                .set("source", JSONUtil.createObj()
                        .set("desc", "测试中心")
                        .set("desc_color", 1))
                .set("main_title", JSONUtil.createObj()
                        .set("title", "企业微信模板卡片测试")
                        .set("desc", "这是一条模板卡片测试消息"))
                .set("emphasis_content", JSONUtil.createObj()
                        .set("title", "SUCCESS")
                        .set("desc", "执行结果"))
                .set("card_action", JSONUtil.createObj()
                        .set("type", 1)
                        .set("url", "https://example.com"));

        WeComRobotUtil.WeComRobotResult result = robot.sendTemplateCard(templateCard);

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("send：发送自定义JSON消息")
    void send() {
        WeComRobotUtil robot = realRobot();

        WeComRobotUtil.WeComRobotResult result = robot.send(JSONUtil.createObj()
                .set("msgtype", "text")
                .set("text", JSONUtil.createObj()
                        .set("content", "企业微信机器人测试：自定义JSON消息")));

        assertTrue(result.success(), result.errmsg());
    }

    @Test
    @DisplayName("buildAlarmMarkdown：构建企业微信告警Markdown内容")
    void buildAlarmMarkdown() {
        String markdown = WeComRobotUtil.buildAlarmMarkdown(
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
    @DisplayName("Mention.none：不@任何人")
    void mentionNone() {
        WeComRobotUtil.Mention mention = WeComRobotUtil.Mention.none();

        assertTrue(mention.mentionedList().isEmpty());
        assertTrue(mention.mentionedMobileList().isEmpty());
    }

    @Test
    @DisplayName("Mention.all：@所有人")
    void mentionAll() {
        WeComRobotUtil.Mention mention = WeComRobotUtil.Mention.all();

        assertEquals("@all", mention.mentionedList().getFirst());
    }

    @Test
    @DisplayName("Mention.users：按用户ID@用户")
    void mentionUsers() {
        WeComRobotUtil.Mention mention = WeComRobotUtil.Mention.users("zhangsan", "zhangsan", " ");

        assertEquals(1, mention.mentionedList().size());
        assertEquals("zhangsan", mention.mentionedList().getFirst());
    }

    @Test
    @DisplayName("Mention.mobiles：按手机号@用户")
    void mentionMobiles() {
        WeComRobotUtil.Mention mention = WeComRobotUtil.Mention.mobiles("13800000000", "13800000000", " ");

        assertEquals(1, mention.mentionedMobileList().size());
        assertEquals("13800000000", mention.mentionedMobileList().getFirst());
    }

    @Test
    @DisplayName("Article.of：创建图文消息文章")
    void articleOf() {
        WeComRobotUtil.Article article = WeComRobotUtil.Article.of(
                "标题",
                "描述",
                "https://example.com",
                "https://example.com/a.png"
        );

        assertEquals("标题", article.title());
        assertEquals("描述", article.description());
        assertEquals("https://example.com", article.url());
        assertEquals("https://example.com/a.png", article.picUrl());
    }

    @Test
    @DisplayName("WeComRobotResult.parse：解析企业微信发送响应")
    void weComRobotResultParse() {
        WeComRobotUtil.WeComRobotResult result = WeComRobotUtil.WeComRobotResult.parse(
                200,
                "{\"errcode\":0,\"errmsg\":\"ok\"}"
        );

        assertTrue(result.success());
        assertEquals(0, result.errcode());
        assertEquals("ok", result.errmsg());
    }

    @Test
    @DisplayName("WeComMediaResult.parse：解析企业微信媒体上传响应")
    void weComMediaResultParse() {
        WeComRobotUtil.WeComMediaResult result = WeComRobotUtil.WeComMediaResult.parse(
                200,
                "{\"errcode\":0,\"errmsg\":\"ok\",\"type\":\"file\",\"media_id\":\"media001\",\"created_at\":1710000000}"
        );

        assertTrue(result.success());
        assertEquals("media001", result.mediaId());
    }

    private WeComRobotUtil realRobot() {
        Assumptions.assumeTrue(
                StrUtil.equalsIgnoreCase(System.getenv(TEST_ENABLED), "true"),
                "未启用企业微信机器人集成测试，请配置 WECOM_ROBOT_TEST_ENABLED=true"
        );

        String robotKey = System.getenv(ROBOT_KEY);
        String webhook = System.getenv(WEBHOOK);

        Assumptions.assumeTrue(
                StrUtil.isNotBlank(robotKey) || StrUtil.isNotBlank(webhook),
                "请配置 WECOM_ROBOT_KEY 或 WECOM_ROBOT_WEBHOOK"
        );

        WeComRobotUtil.Builder builder = WeComRobotUtil.builder()
                .timeoutMillis(8000)
                .throwExceptionOnFail(false);

        if (StrUtil.isNotBlank(webhook)) {
            builder.webhook(webhook);
        } else {
            builder.robotKey(robotKey);
        }

        return builder.build();
    }

    private File createTempPng() {
        File file = FileUtil.createTempFile("wecom-robot-image-", ".png", true);
        FileUtil.writeBytes(Base64.decode(PNG_BASE64), file);
        return file;
    }

    private File createTempTextFile() {
        File file = FileUtil.createTempFile("wecom-robot-file-", ".txt", true);
        FileUtil.writeUtf8String("企业微信机器人文件上传测试", file);
        return file;
    }
}
