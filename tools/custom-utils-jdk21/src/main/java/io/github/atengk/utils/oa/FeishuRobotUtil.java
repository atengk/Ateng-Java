package io.github.atengk.utils.oa;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 飞书自定义机器人消息发送工具类。
 *
 * @author Ateng
 * @since 2026-05-14
 */
public final class FeishuRobotUtil {

    private static final Log log = LogFactory.get(FeishuRobotUtil.class);

    private static final String ROBOT_SEND_URL_PREFIX = "https://open.feishu.cn/open-apis/bot/v2/hook/";

    private static final String IMAGE_UPLOAD_URL = "https://open.feishu.cn/open-apis/im/v1/images";

    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;

    private static final int SUCCESS_CODE = 0;

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String webhook;

    private final String secret;

    private final String tenantAccessToken;

    private final int timeoutMillis;

    private final boolean throwExceptionOnFail;

    private FeishuRobotUtil(Builder builder) {
        this.webhook = requireNotBlank(builder.webhook, "webhook");
        this.secret = StrUtil.trimToNull(builder.secret);
        this.tenantAccessToken = StrUtil.trimToNull(builder.tenantAccessToken);
        this.timeoutMillis = builder.timeoutMillis > 0 ? builder.timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
        this.throwExceptionOnFail = builder.throwExceptionOnFail;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 使用机器人 token 创建客户端。
     *
     * @param robotToken 飞书机器人 webhook token
     * @return 飞书机器人客户端
     */
    public static FeishuRobotUtil ofRobotToken(String robotToken) {
        return builder()
                .robotToken(robotToken)
                .build();
    }

    /**
     * 使用机器人 token 和签名密钥创建客户端。
     *
     * @param robotToken 飞书机器人 webhook token
     * @param secret     签名密钥
     * @return 飞书机器人客户端
     */
    public static FeishuRobotUtil ofRobotToken(String robotToken, String secret) {
        return builder()
                .robotToken(robotToken)
                .secret(secret)
                .build();
    }

    /**
     * 使用完整 webhook 创建客户端。
     *
     * @param webhook 飞书机器人完整 webhook
     * @return 飞书机器人客户端
     */
    public static FeishuRobotUtil ofWebhook(String webhook) {
        return builder()
                .webhook(webhook)
                .build();
    }

    /**
     * 使用完整 webhook 和签名密钥创建客户端。
     *
     * @param webhook 飞书机器人完整 webhook
     * @param secret  签名密钥
     * @return 飞书机器人客户端
     */
    public static FeishuRobotUtil ofWebhook(String webhook, String secret) {
        return builder()
                .webhook(webhook)
                .secret(secret)
                .build();
    }

    /**
     * 发送文本消息。
     *
     * @param content 消息内容
     * @return 发送结果
     */
    public FeishuRobotResult sendText(String content) {
        requireNotBlank(content, "文本消息内容");

        JSONObject payload = createBasePayload(MessageType.TEXT);
        payload.set("content", JSONUtil.createObj()
                .set("text", content));

        return send(payload);
    }

    /**
     * 发送带@的文本消息。
     *
     * @param content 消息内容
     * @param mentions @用户集合
     * @return 发送结果
     */
    public FeishuRobotResult sendText(String content, Collection<Mention> mentions) {
        requireNotBlank(content, "文本消息内容");

        StringBuilder textBuilder = StrUtil.builder(content);
        if (CollUtil.isNotEmpty(mentions)) {
            mentions.stream()
                    .filter(Objects::nonNull)
                    .map(Mention::toTextMention)
                    .filter(StrUtil::isNotBlank)
                    .forEach(item -> textBuilder.append(" ").append(item));
        }

        return sendText(textBuilder.toString());
    }

    /**
     * 发送中文富文本消息。
     *
     * @param title 标题
     * @param lines 富文本行集合
     * @return 发送结果
     */
    public FeishuRobotResult sendPost(String title, Collection<List<PostElement>> lines) {
        return sendPost("zh_cn", title, lines);
    }

    /**
     * 发送富文本消息。
     *
     * @param language 语言，例如 zh_cn、en_us
     * @param title    标题
     * @param lines    富文本行集合
     * @return 发送结果
     */
    public FeishuRobotResult sendPost(String language, String title, Collection<List<PostElement>> lines) {
        requireNotBlank(language, "富文本语言");
        requireNotBlank(title, "富文本标题");

        if (CollUtil.isEmpty(lines)) {
            throw new IllegalArgumentException("富文本内容不能为空");
        }

        JSONArray contentArray = JSONUtil.createArray();
        lines.stream()
                .filter(CollUtil::isNotEmpty)
                .map(this::toPostLineArray)
                .filter(CollUtil::isNotEmpty)
                .forEach(contentArray::add);

        if (contentArray.isEmpty()) {
            throw new IllegalArgumentException("有效富文本内容不能为空");
        }

        JSONObject payload = createBasePayload(MessageType.POST);
        payload.set("content", JSONUtil.createObj()
                .set("post", JSONUtil.createObj()
                        .set(language, JSONUtil.createObj()
                                .set("title", title)
                                .set("content", contentArray))));

        return send(payload);
    }

    /**
     * 发送图片消息。
     *
     * @param imageKey 飞书图片 image_key
     * @return 发送结果
     */
    public FeishuRobotResult sendImageByKey(String imageKey) {
        requireNotBlank(imageKey, "image_key");

        JSONObject payload = createBasePayload(MessageType.IMAGE);
        payload.set("content", JSONUtil.createObj()
                .set("image_key", imageKey));

        return send(payload);
    }

    /**
     * 上传本地图片并发送图片消息。
     *
     * @param imageFile 图片文件
     * @return 发送结果
     */
    public FeishuRobotResult sendImage(File imageFile) {
        FeishuImageResult imageResult = uploadImage(imageFile);
        if (!imageResult.success()) {
            if (throwExceptionOnFail) {
                throw new FeishuRobotException("飞书图片上传失败：" + imageResult.msg());
            }
            return FeishuRobotResult.fromImageResult(imageResult);
        }

        return sendImageByKey(imageResult.imageKey());
    }

    /**
     * 上传本地图片获取 image_key。
     *
     * @param imageFile 图片文件
     * @return 图片上传结果
     */
    public FeishuImageResult uploadImage(File imageFile) {
        return uploadImage(imageFile, tenantAccessToken);
    }

    /**
     * 上传本地图片获取 image_key。
     *
     * @param imageFile         图片文件
     * @param tenantAccessToken 飞书应用 tenant_access_token
     * @return 图片上传结果
     */
    public FeishuImageResult uploadImage(File imageFile, String tenantAccessToken) {
        checkReadableFile(imageFile, "图片文件");
        requireNotBlank(tenantAccessToken, "tenant_access_token");

        try (HttpResponse response = HttpRequest.post(IMAGE_UPLOAD_URL)
                .header("Authorization", "Bearer " + tenantAccessToken.trim())
                .form("image_type", "message")
                .form("image", imageFile)
                .timeout(timeoutMillis)
                .execute()) {

            FeishuImageResult result = FeishuImageResult.parse(response.getStatus(), response.body());

            if (result.success()) {
                log.debug("飞书图片上传成功，fileName={}，imageKey={}", imageFile.getName(), result.imageKey());
                return result;
            }

            log.warn("飞书图片上传失败，httpStatus={}，code={}，msg={}",
                    result.httpStatus(), result.code(), result.msg());

            if (throwExceptionOnFail) {
                throw new FeishuRobotException(StrUtil.format(
                        "飞书图片上传失败，httpStatus={}，code={}，msg={}",
                        result.httpStatus(),
                        result.code(),
                        result.msg()
                ));
            }

            return result;
        } catch (FeishuRobotException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(ex, "飞书图片上传异常：{}", ex.getMessage());

            if (throwExceptionOnFail) {
                throw new FeishuRobotException("飞书图片上传异常：" + ex.getMessage(), ex);
            }

            return FeishuImageResult.fail(ex);
        }
    }

    /**
     * 发送交互卡片消息。
     *
     * @param card 卡片JSON
     * @return 发送结果
     */
    public FeishuRobotResult sendInteractiveCard(JSONObject card) {
        if (card == null || card.isEmpty()) {
            throw new IllegalArgumentException("交互卡片不能为空");
        }

        JSONObject payload = createBasePayload(MessageType.INTERACTIVE);
        payload.set("card", card);

        return send(payload);
    }

    /**
     * 发送告警卡片消息。
     *
     * @param title   标题
     * @param appName 应用名称
     * @param level   告警级别
     * @param content 告警内容
     * @param detailUrl 详情地址，可为空
     * @return 发送结果
     */
    public FeishuRobotResult sendAlarmCard(String title, String appName, String level, String content, String detailUrl) {
        JSONObject card = buildAlarmCard(title, appName, level, content, detailUrl);
        return sendInteractiveCard(card);
    }

    /**
     * 发送自定义 JSON 消息。
     *
     * @param payload 飞书机器人消息体
     * @return 发送结果
     */
    public FeishuRobotResult send(JSONObject payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("飞书机器人消息体不能为空");
        }

        JSONObject requestPayload = JSONUtil.parseObj(JSONUtil.toJsonStr(payload));
        appendSignIfNecessary(requestPayload);

        String requestBody = JSONUtil.toJsonStr(requestPayload);

        try (HttpResponse response = HttpRequest.post(webhook)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestBody)
                .timeout(timeoutMillis)
                .execute()) {

            FeishuRobotResult result = FeishuRobotResult.parse(response.getStatus(), response.body());

            if (result.success()) {
                log.debug("飞书机器人消息发送成功，msg_type={}", requestPayload.getStr("msg_type"));
                return result;
            }

            log.warn("飞书机器人消息发送失败，httpStatus={}，code={}，msg={}",
                    result.httpStatus(), result.code(), result.msg());

            if (throwExceptionOnFail) {
                throw new FeishuRobotException(StrUtil.format(
                        "飞书机器人消息发送失败，httpStatus={}，code={}，msg={}",
                        result.httpStatus(),
                        result.code(),
                        result.msg()
                ));
            }

            return result;
        } catch (FeishuRobotException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(ex, "飞书机器人消息发送异常：{}", ex.getMessage());

            if (throwExceptionOnFail) {
                throw new FeishuRobotException("飞书机器人消息发送异常：" + ex.getMessage(), ex);
            }

            return FeishuRobotResult.fail(ex);
        }
    }

    /**
     * 创建飞书告警富文本内容。
     *
     * @param title   标题
     * @param appName 应用名称
     * @param level   告警级别
     * @param content 告警内容
     * @return 富文本行集合
     */
    public static List<List<PostElement>> buildAlarmPostLines(String title, String appName, String level, String content) {
        return List.of(
                List.of(PostElement.text(StrUtil.blankToDefault(title, "服务告警"))),
                List.of(PostElement.text("应用名称：" + StrUtil.blankToDefault(appName, "-"))),
                List.of(PostElement.text("告警级别：" + StrUtil.blankToDefault(level, "-"))),
                List.of(PostElement.text("告警内容：" + StrUtil.blankToDefault(content, "-"))),
                List.of(PostElement.text("触发时间：" + LocalDateTime.now()))
        );
    }

    /**
     * 创建飞书告警交互卡片。
     *
     * @param title     标题
     * @param appName   应用名称
     * @param level     告警级别
     * @param content   告警内容
     * @param detailUrl 详情地址，可为空
     * @return 交互卡片JSON
     */
    public static JSONObject buildAlarmCard(String title, String appName, String level, String content, String detailUrl) {
        JSONArray elements = JSONUtil.createArray()
                .set(JSONUtil.createObj()
                        .set("tag", "div")
                        .set("text", larkMarkdown("**应用名称：** " + StrUtil.blankToDefault(appName, "-"))))
                .set(JSONUtil.createObj()
                        .set("tag", "div")
                        .set("text", larkMarkdown("**告警级别：** " + StrUtil.blankToDefault(level, "-"))))
                .set(JSONUtil.createObj()
                        .set("tag", "div")
                        .set("text", larkMarkdown("**告警内容：** " + StrUtil.blankToDefault(content, "-"))))
                .set(JSONUtil.createObj()
                        .set("tag", "div")
                        .set("text", larkMarkdown("**触发时间：** " + LocalDateTime.now())));

        if (StrUtil.isNotBlank(detailUrl)) {
            elements.set(JSONUtil.createObj()
                    .set("tag", "action")
                    .set("actions", JSONUtil.createArray()
                            .set(JSONUtil.createObj()
                                    .set("tag", "button")
                                    .set("text", plainText("查看详情"))
                                    .set("type", "primary")
                                    .set("url", detailUrl))));
        }

        return JSONUtil.createObj()
                .set("config", JSONUtil.createObj()
                        .set("wide_screen_mode", true))
                .set("header", JSONUtil.createObj()
                        .set("template", "red")
                        .set("title", plainText(StrUtil.blankToDefault(title, "服务告警"))))
                .set("elements", elements);
    }

    /**
     * 创建飞书签名。
     *
     * @param timestampSeconds 秒级时间戳
     * @param secret           签名密钥
     * @return 签名
     */
    public static String createSign(long timestampSeconds, String secret) {
        requireNotBlank(secret, "飞书机器人签名密钥");

        try {
            String stringToSign = timestampSeconds + "\n" + secret;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    stringToSign.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            return Base64.encode(mac.doFinal(new byte[0]));
        } catch (Exception ex) {
            throw new FeishuRobotException("飞书机器人签名生成失败：" + ex.getMessage(), ex);
        }
    }

    private JSONArray toPostLineArray(List<PostElement> elements) {
        JSONArray lineArray = JSONUtil.createArray();
        elements.stream()
                .filter(Objects::nonNull)
                .map(PostElement::toJson)
                .forEach(lineArray::add);
        return lineArray;
    }

    private void appendSignIfNecessary(JSONObject payload) {
        if (StrUtil.isBlank(secret)) {
            return;
        }

        long timestampSeconds = Instant.now().getEpochSecond();
        payload.set("timestamp", String.valueOf(timestampSeconds));
        payload.set("sign", createSign(timestampSeconds, secret));
    }

    private static JSONObject createBasePayload(MessageType messageType) {
        return JSONUtil.createObj()
                .set("msg_type", messageType.code);
    }

    private static JSONObject plainText(String content) {
        return JSONUtil.createObj()
                .set("tag", "plain_text")
                .set("content", StrUtil.blankToDefault(content, ""));
    }

    private static JSONObject larkMarkdown(String content) {
        return JSONUtil.createObj()
                .set("tag", "lark_md")
                .set("content", StrUtil.blankToDefault(content, ""));
    }

    private static String buildWebhookByRobotToken(String robotToken) {
        return ROBOT_SEND_URL_PREFIX + requireNotBlank(robotToken, "robotToken");
    }

    private static void checkReadableFile(File file, String fieldName) {
        if (file == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException(fieldName + "不存在：" + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException(fieldName + "不是普通文件：" + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException(fieldName + "不可读：" + file.getAbsolutePath());
        }
        if (FileUtil.isEmpty(file)) {
            throw new IllegalArgumentException(fieldName + "不能为空文件：" + file.getAbsolutePath());
        }
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private static List<String> cleanList(Collection<String> values) {
        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static List<String> cleanArray(String... values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        return cleanList(Arrays.asList(values));
    }

    /**
     * 拼接URL查询参数。
     *
     * @param url      原始URL
     * @param urlQuery 查询参数
     * @return 拼接后的URL
     */
    private static String appendQuery(String url, UrlQuery urlQuery) {
        requireNotBlank(url, "url");

        if (urlQuery == null) {
            return url;
        }

        String query = urlQuery.build(CharsetUtil.CHARSET_UTF_8);
        if (StrUtil.isBlank(query)) {
            return url;
        }

        String joinSymbol = url.contains("?") ? "&" : "?";
        return url + joinSymbol + query;
    }

    /**
     * 飞书机器人消息类型。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    private enum MessageType {

        TEXT("text"),

        POST("post"),

        IMAGE("image"),

        INTERACTIVE("interactive");

        private final String code;

        MessageType(String code) {
            this.code = code;
        }
    }

    /**
     * 飞书@用户配置。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record Mention(String userId, String userName) {

        public Mention {
            userId = requireNotBlank(userId, "userId");
            userName = StrUtil.blankToDefault(userName, "用户");
        }

        /**
         * @指定用户。
         *
         * @param userId   用户ID
         * @param userName 用户名称
         * @return @配置
         */
        public static Mention user(String userId, String userName) {
            return new Mention(userId, userName);
        }

        /**
         * @所有人。
         *
         * @return @配置
         */
        public static Mention all() {
            return new Mention("all", "所有人");
        }

        private String toTextMention() {
            return StrUtil.format("<at user_id=\"{}\">{}</at>", userId, userName);
        }

        private JSONObject toPostMention() {
            return JSONUtil.createObj()
                    .set("tag", "at")
                    .set("user_id", userId)
                    .set("user_name", userName);
        }
    }

    /**
     * 飞书富文本元素。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record PostElement(String tag, String text, String href, String userId, String userName, String imageKey) {

        /**
         * 创建文本元素。
         *
         * @param text 文本内容
         * @return 富文本元素
         */
        public static PostElement text(String text) {
            return new PostElement("text", StrUtil.blankToDefault(text, ""), null, null, null, null);
        }

        /**
         * 创建链接元素。
         *
         * @param text 链接文本
         * @param href 跳转地址
         * @return 富文本元素
         */
        public static PostElement link(String text, String href) {
            return new PostElement("a", requireNotBlank(text, "链接文本"), requireNotBlank(href, "链接地址"), null, null, null);
        }

        /**
         * 创建@元素。
         *
         * @param mention @配置
         * @return 富文本元素
         */
        public static PostElement at(Mention mention) {
            if (mention == null) {
                throw new IllegalArgumentException("@配置不能为空");
            }
            return new PostElement("at", null, null, mention.userId(), mention.userName(), null);
        }

        /**
         * 创建图片元素。
         *
         * @param imageKey 图片 image_key
         * @return 富文本元素
         */
        public static PostElement image(String imageKey) {
            return new PostElement("img", null, null, null, null, requireNotBlank(imageKey, "image_key"));
        }

        private JSONObject toJson() {
            return switch (tag) {
                case "text" -> JSONUtil.createObj()
                        .set("tag", "text")
                        .set("text", text);
                case "a" -> JSONUtil.createObj()
                        .set("tag", "a")
                        .set("text", text)
                        .set("href", href);
                case "at" -> new Mention(userId, userName).toPostMention();
                case "img" -> JSONUtil.createObj()
                        .set("tag", "img")
                        .set("image_key", imageKey);
                default -> throw new IllegalArgumentException("不支持的富文本元素类型：" + tag);
            };
        }
    }

    /**
     * 飞书机器人消息发送结果。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record FeishuRobotResult(int httpStatus, Integer code, Integer statusCode, String msg, String rawBody) {

        /**
         * 判断是否发送成功。
         *
         * @return true 表示发送成功
         */
        public boolean success() {
            if (httpStatus < 200 || httpStatus >= 300) {
                return false;
            }
            if (code != null) {
                return Integer.valueOf(SUCCESS_CODE).equals(code);
            }
            return Integer.valueOf(SUCCESS_CODE).equals(statusCode);
        }

        /**
         * 解析飞书机器人响应。
         *
         * @param httpStatus HTTP状态码
         * @param rawBody    原始响应体
         * @return 发送结果
         */
        public static FeishuRobotResult parse(int httpStatus, String rawBody) {
            if (StrUtil.isBlank(rawBody)) {
                return new FeishuRobotResult(httpStatus, -1, null, "响应内容为空", rawBody);
            }

            try {
                JSONObject json = JSONUtil.parseObj(rawBody);
                Integer code = json.containsKey("code") ? Convert.toInt(json.get("code"), -1) : null;
                Integer statusCode = json.containsKey("StatusCode") ? Convert.toInt(json.get("StatusCode"), -1) : null;
                String msg = StrUtil.firstNonBlank(
                        json.getStr("msg"),
                        json.getStr("errmsg"),
                        json.getStr("StatusMessage"),
                        rawBody
                );

                return new FeishuRobotResult(httpStatus, code, statusCode, msg, rawBody);
            } catch (Exception ex) {
                return new FeishuRobotResult(httpStatus, -1, null, "响应内容不是合法JSON：" + rawBody, rawBody);
            }
        }

        /**
         * 创建异常失败结果。
         *
         * @param ex 异常
         * @return 发送结果
         */
        public static FeishuRobotResult fail(Exception ex) {
            return new FeishuRobotResult(-1, -1, null, ex.getMessage(), null);
        }

        /**
         * 从图片上传结果转换发送结果。
         *
         * @param imageResult 图片上传结果
         * @return 发送结果
         */
        public static FeishuRobotResult fromImageResult(FeishuImageResult imageResult) {
            return new FeishuRobotResult(
                    imageResult.httpStatus(),
                    imageResult.code(),
                    null,
                    imageResult.msg(),
                    imageResult.rawBody()
            );
        }
    }

    /**
     * 飞书图片上传结果。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record FeishuImageResult(int httpStatus, Integer code, String msg, String imageKey, String rawBody) {

        /**
         * 判断是否上传成功。
         *
         * @return true 表示上传成功
         */
        public boolean success() {
            return httpStatus >= 200
                    && httpStatus < 300
                    && Integer.valueOf(SUCCESS_CODE).equals(code)
                    && StrUtil.isNotBlank(imageKey);
        }

        /**
         * 解析飞书图片上传响应。
         *
         * @param httpStatus HTTP状态码
         * @param rawBody    原始响应体
         * @return 图片上传结果
         */
        public static FeishuImageResult parse(int httpStatus, String rawBody) {
            if (StrUtil.isBlank(rawBody)) {
                return new FeishuImageResult(httpStatus, -1, "响应内容为空", null, rawBody);
            }

            try {
                JSONObject json = JSONUtil.parseObj(rawBody);
                Integer code = Convert.toInt(json.get("code"), -1);
                String msg = StrUtil.blankToDefault(json.getStr("msg"), rawBody);
                JSONObject data = json.getJSONObject("data");
                String imageKey = data == null ? null : data.getStr("image_key");

                return new FeishuImageResult(httpStatus, code, msg, imageKey, rawBody);
            } catch (Exception ex) {
                return new FeishuImageResult(httpStatus, -1, "响应内容不是合法JSON：" + rawBody, null, rawBody);
            }
        }

        /**
         * 创建异常失败结果。
         *
         * @param ex 异常
         * @return 图片上传结果
         */
        public static FeishuImageResult fail(Exception ex) {
            return new FeishuImageResult(-1, -1, ex.getMessage(), null, null);
        }
    }

    /**
     * 飞书机器人异常。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public static class FeishuRobotException extends RuntimeException {

        /**
         * 创建飞书机器人异常。
         *
         * @param message 异常消息
         */
        public FeishuRobotException(String message) {
            super(message);
        }

        /**
         * 创建飞书机器人异常。
         *
         * @param message 异常消息
         * @param cause   原始异常
         */
        public FeishuRobotException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 飞书机器人客户端构建器。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public static class Builder {

        private String webhook;

        private String robotToken;

        private String secret;

        private String tenantAccessToken;

        private int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;

        private boolean throwExceptionOnFail = true;

        /**
         * 设置机器人 token。
         *
         * @param robotToken 飞书机器人 webhook token
         * @return 当前构建器
         */
        public Builder robotToken(String robotToken) {
            this.robotToken = robotToken;
            return this;
        }

        /**
         * 设置完整 webhook。
         *
         * @param webhook 飞书机器人完整 webhook
         * @return 当前构建器
         */
        public Builder webhook(String webhook) {
            this.webhook = webhook;
            return this;
        }

        /**
         * 设置签名密钥。
         *
         * @param secret 签名密钥
         * @return 当前构建器
         */
        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        /**
         * 设置 tenant_access_token。
         *
         * @param tenantAccessToken 飞书应用 tenant_access_token
         * @return 当前构建器
         */
        public Builder tenantAccessToken(String tenantAccessToken) {
            this.tenantAccessToken = tenantAccessToken;
            return this;
        }

        /**
         * 设置请求超时时间。
         *
         * @param timeoutMillis 超时时间，单位毫秒
         * @return 当前构建器
         */
        public Builder timeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        /**
         * 设置发送失败时是否抛出异常。
         *
         * @param throwExceptionOnFail true 表示失败时抛出异常
         * @return 当前构建器
         */
        public Builder throwExceptionOnFail(boolean throwExceptionOnFail) {
            this.throwExceptionOnFail = throwExceptionOnFail;
            return this;
        }

        /**
         * 构建飞书机器人客户端。
         *
         * @return 飞书机器人客户端
         */
        public FeishuRobotUtil build() {
            if (StrUtil.isBlank(webhook) && StrUtil.isBlank(robotToken)) {
                throw new IllegalArgumentException("webhook和robotToken不能同时为空");
            }

            String finalWebhook = StrUtil.isNotBlank(webhook)
                    ? webhook.trim()
                    : buildWebhookByRobotToken(robotToken);

            this.webhook = finalWebhook;
            return new FeishuRobotUtil(this);
        }
    }
}
