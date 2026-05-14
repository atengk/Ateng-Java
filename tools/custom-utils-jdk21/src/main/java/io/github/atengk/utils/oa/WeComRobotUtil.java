package io.github.atengk.utils.oa;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 企业微信群机器人消息发送工具类。
 *
 * @author Ateng
 * @since 2026-05-14
 */
public final class WeComRobotUtil {

    private static final Log log = LogFactory.get(WeComRobotUtil.class);

    private static final String ROBOT_SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send";

    private static final String ROBOT_UPLOAD_MEDIA_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/upload_media";

    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;

    private static final int SUCCESS_CODE = 0;

    private final String webhook;

    private final String robotKey;

    private final int timeoutMillis;

    private final boolean throwExceptionOnFail;

    private WeComRobotUtil(Builder builder) {
        this.webhook = requireNotBlank(builder.webhook, "webhook");
        this.robotKey = StrUtil.trimToNull(builder.robotKey);
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
     * 使用机器人 key 创建客户端。
     *
     * @param robotKey 企业微信群机器人 key
     * @return 企业微信机器人客户端
     */
    public static WeComRobotUtil ofKey(String robotKey) {
        return builder()
                .robotKey(robotKey)
                .build();
    }

    /**
     * 使用完整 webhook 创建客户端。
     *
     * @param webhook 企业微信群机器人完整 webhook
     * @return 企业微信机器人客户端
     */
    public static WeComRobotUtil ofWebhook(String webhook) {
        return builder()
                .webhook(webhook)
                .build();
    }

    /**
     * 发送文本消息。
     *
     * @param content 消息内容
     * @return 发送结果
     */
    public WeComRobotResult sendText(String content) {
        return sendText(content, Mention.none());
    }

    /**
     * 发送文本消息。
     *
     * @param content 消息内容
     * @param mention @配置
     * @return 发送结果
     */
    public WeComRobotResult sendText(String content, Mention mention) {
        requireNotBlank(content, "文本消息内容");

        JSONObject text = JSONUtil.createObj()
                .set("content", content);

        Mention actualMention = mention == null ? Mention.none() : mention;
        if (CollUtil.isNotEmpty(actualMention.mentionedList())) {
            text.set("mentioned_list", actualMention.mentionedList());
        }
        if (CollUtil.isNotEmpty(actualMention.mentionedMobileList())) {
            text.set("mentioned_mobile_list", actualMention.mentionedMobileList());
        }

        JSONObject payload = createBasePayload(MessageType.TEXT);
        payload.set("text", text);

        return send(payload);
    }

    /**
     * 发送 Markdown 消息。
     *
     * @param content Markdown内容
     * @return 发送结果
     */
    public WeComRobotResult sendMarkdown(String content) {
        requireNotBlank(content, "Markdown消息内容");

        JSONObject payload = createBasePayload(MessageType.MARKDOWN);
        payload.set("markdown", JSONUtil.createObj()
                .set("content", content));

        return send(payload);
    }

    /**
     * 发送图片消息。
     *
     * @param imageFile 图片文件，建议小于2MB
     * @return 发送结果
     */
    public WeComRobotResult sendImage(File imageFile) {
        checkReadableFile(imageFile, "图片文件");

        byte[] imageBytes = FileUtil.readBytes(imageFile);
        String base64 = Base64.encode(imageBytes);
        String md5 = DigestUtil.md5Hex(imageBytes);

        return sendImage(base64, md5);
    }

    /**
     * 发送图片消息。
     *
     * @param base64 图片Base64内容
     * @param md5    图片MD5值
     * @return 发送结果
     */
    public WeComRobotResult sendImage(String base64, String md5) {
        requireNotBlank(base64, "图片Base64内容");
        requireNotBlank(md5, "图片MD5值");

        JSONObject payload = createBasePayload(MessageType.IMAGE);
        payload.set("image", JSONUtil.createObj()
                .set("base64", base64)
                .set("md5", md5));

        return send(payload);
    }

    /**
     * 发送图文消息。
     *
     * @param articles 图文集合
     * @return 发送结果
     */
    public WeComRobotResult sendNews(Collection<Article> articles) {
        if (CollUtil.isEmpty(articles)) {
            throw new IllegalArgumentException("图文消息不能为空");
        }

        JSONArray articleArray = JSONUtil.createArray();
        articles.stream()
                .filter(Objects::nonNull)
                .map(Article::toJson)
                .forEach(articleArray::add);

        if (articleArray.isEmpty()) {
            throw new IllegalArgumentException("有效图文消息不能为空");
        }

        JSONObject payload = createBasePayload(MessageType.NEWS);
        payload.set("news", JSONUtil.createObj()
                .set("articles", articleArray));

        return send(payload);
    }

    /**
     * 上传文件并发送文件消息。
     *
     * @param file 文件
     * @return 发送结果
     */
    public WeComRobotResult sendFile(File file) {
        WeComMediaResult mediaResult = uploadFile(file);
        if (!mediaResult.success()) {
            if (throwExceptionOnFail) {
                throw new WeComRobotException("企业微信机器人文件上传失败：" + mediaResult.errmsg());
            }
            return WeComRobotResult.fromMediaResult(mediaResult);
        }

        return sendFileByMediaId(mediaResult.mediaId());
    }

    /**
     * 根据 media_id 发送文件消息。
     *
     * @param mediaId 文件media_id
     * @return 发送结果
     */
    public WeComRobotResult sendFileByMediaId(String mediaId) {
        requireNotBlank(mediaId, "media_id");

        JSONObject payload = createBasePayload(MessageType.FILE);
        payload.set("file", JSONUtil.createObj()
                .set("media_id", mediaId));

        return send(payload);
    }

    /**
     * 上传文件到企业微信群机器人临时素材。
     *
     * @param file 文件
     * @return 上传结果
     */
    public WeComMediaResult uploadFile(File file) {
        checkReadableFile(file, "上传文件");
        String requestUrl = buildUploadMediaUrl("file");

        try (HttpResponse response = HttpRequest.post(requestUrl)
                .form("media", file)
                .timeout(timeoutMillis)
                .execute()) {

            WeComMediaResult result = WeComMediaResult.parse(response.getStatus(), response.body());

            if (result.success()) {
                log.debug("企业微信机器人文件上传成功，fileName={}，mediaId={}", file.getName(), result.mediaId());
                return result;
            }

            log.warn("企业微信机器人文件上传失败，httpStatus={}，errcode={}，errmsg={}",
                    result.httpStatus(), result.errcode(), result.errmsg());

            if (throwExceptionOnFail) {
                throw new WeComRobotException(StrUtil.format(
                        "企业微信机器人文件上传失败，httpStatus={}，errcode={}，errmsg={}",
                        result.httpStatus(),
                        result.errcode(),
                        result.errmsg()
                ));
            }

            return result;
        } catch (WeComRobotException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(ex, "企业微信机器人文件上传异常：{}", ex.getMessage());

            if (throwExceptionOnFail) {
                throw new WeComRobotException("企业微信机器人文件上传异常：" + ex.getMessage(), ex);
            }

            return WeComMediaResult.fail(ex);
        }
    }

    /**
     * 发送模板卡片消息。
     *
     * @param templateCard 模板卡片JSON对象
     * @return 发送结果
     */
    public WeComRobotResult sendTemplateCard(JSONObject templateCard) {
        if (templateCard == null || templateCard.isEmpty()) {
            throw new IllegalArgumentException("模板卡片内容不能为空");
        }

        JSONObject payload = createBasePayload(MessageType.TEMPLATE_CARD);
        payload.set("template_card", templateCard);

        return send(payload);
    }

    /**
     * 发送自定义 JSON 消息。
     *
     * @param payload 企业微信机器人消息体
     * @return 发送结果
     */
    public WeComRobotResult send(JSONObject payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("企业微信机器人消息体不能为空");
        }

        String requestBody = JSONUtil.toJsonStr(payload);

        try (HttpResponse response = HttpRequest.post(webhook)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestBody)
                .timeout(timeoutMillis)
                .execute()) {

            WeComRobotResult result = WeComRobotResult.parse(response.getStatus(), response.body());

            if (result.success()) {
                log.debug("企业微信机器人消息发送成功，msgtype={}", payload.getStr("msgtype"));
                return result;
            }

            log.warn("企业微信机器人消息发送失败，httpStatus={}，errcode={}，errmsg={}",
                    result.httpStatus(), result.errcode(), result.errmsg());

            if (throwExceptionOnFail) {
                throw new WeComRobotException(StrUtil.format(
                        "企业微信机器人消息发送失败，httpStatus={}，errcode={}，errmsg={}",
                        result.httpStatus(),
                        result.errcode(),
                        result.errmsg()
                ));
            }

            return result;
        } catch (WeComRobotException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(ex, "企业微信机器人消息发送异常：{}", ex.getMessage());

            if (throwExceptionOnFail) {
                throw new WeComRobotException("企业微信机器人消息发送异常：" + ex.getMessage(), ex);
            }

            return WeComRobotResult.fail(ex);
        }
    }

    /**
     * 创建服务告警 Markdown 内容。
     *
     * @param title   标题
     * @param appName 应用名称
     * @param level   告警级别
     * @param content 告警内容
     * @return Markdown内容
     */
    public static String buildAlarmMarkdown(String title, String appName, String level, String content) {
        return StrUtil.builder()
                .append("### ").append(StrUtil.blankToDefault(title, "服务告警")).append("\n")
                .append("> 应用名称：<font color=\"info\">").append(StrUtil.blankToDefault(appName, "-")).append("</font>\n")
                .append("> 告警级别：<font color=\"warning\">").append(StrUtil.blankToDefault(level, "-")).append("</font>\n")
                .append("> 告警内容：<font color=\"comment\">").append(StrUtil.blankToDefault(content, "-")).append("</font>\n")
                .append("> 处理状态：<font color=\"warning\">待处理</font>")
                .toString();
    }

    private String buildUploadMediaUrl(String type) {
        String key = requireNotBlank(robotKey, "robotKey，上传文件时必须配置机器人key或使用包含key参数的完整webhook");

        UrlQuery query = new UrlQuery()
                .add("key", key)
                .add("type", StrUtil.blankToDefault(type, "file"));

        return ROBOT_UPLOAD_MEDIA_URL + "?" + query.build(CharsetUtil.CHARSET_UTF_8);
    }

    private static JSONObject createBasePayload(MessageType messageType) {
        return JSONUtil.createObj()
                .set("msgtype", messageType.code);
    }

    private static String buildWebhookByKey(String robotKey) {
        requireNotBlank(robotKey, "robotKey");

        UrlQuery query = new UrlQuery()
                .add("key", robotKey);

        return ROBOT_SEND_URL + "?" + query.build(CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 从完整webhook中提取key。
     *
     * @param webhook 企业微信机器人完整webhook
     * @return key
     */
    private static String extractKeyFromWebhook(String webhook) {
        if (StrUtil.isBlank(webhook)) {
            return null;
        }

        UrlQuery query = UrlQuery.of(webhook, CharsetUtil.CHARSET_UTF_8, true);
        CharSequence key = query.get("key");

        return key == null ? null : key.toString();
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
     * 企业微信机器人消息类型。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    private enum MessageType {

        TEXT("text"),

        MARKDOWN("markdown"),

        IMAGE("image"),

        NEWS("news"),

        FILE("file"),

        TEMPLATE_CARD("template_card");

        private final String code;

        MessageType(String code) {
            this.code = code;
        }
    }

    /**
     * 企业微信文本消息@配置。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record Mention(List<String> mentionedList, List<String> mentionedMobileList) {

        public Mention {
            mentionedList = cleanList(mentionedList);
            mentionedMobileList = cleanList(mentionedMobileList);
        }

        /**
         * 不@任何人。
         *
         * @return @配置
         */
        public static Mention none() {
            return new Mention(List.of(), List.of());
        }

        /**
         * @所有人。
         *
         * @return @配置
         */
        public static Mention all() {
            return new Mention(List.of("@all"), List.of());
        }

        /**
         * 按企业微信用户ID@用户。
         *
         * @param userIds 用户ID
         * @return @配置
         */
        public static Mention users(String... userIds) {
            return new Mention(cleanArray(userIds), List.of());
        }

        /**
         * 按手机号@用户。
         *
         * @param mobiles 手机号
         * @return @配置
         */
        public static Mention mobiles(String... mobiles) {
            return new Mention(List.of(), cleanArray(mobiles));
        }

        /**
         * 创建@配置。
         *
         * @param mentionedList       用户ID集合
         * @param mentionedMobileList 手机号集合
         * @return @配置
         */
        public static Mention of(Collection<String> mentionedList, Collection<String> mentionedMobileList) {
            return new Mention(cleanList(mentionedList), cleanList(mentionedMobileList));
        }
    }

    /**
     * 企业微信图文消息文章。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record Article(String title, String description, String url, String picUrl) {

        public Article {
            title = requireNotBlank(title, "图文标题");
            description = StrUtil.blankToDefault(description, "");
            url = requireNotBlank(url, "图文跳转地址");
            picUrl = StrUtil.blankToDefault(picUrl, "");
        }

        /**
         * 创建图文消息文章。
         *
         * @param title       标题
         * @param description 描述
         * @param url         跳转地址
         * @param picUrl      图片地址
         * @return 图文文章
         */
        public static Article of(String title, String description, String url, String picUrl) {
            return new Article(title, description, url, picUrl);
        }

        private JSONObject toJson() {
            JSONObject json = JSONUtil.createObj()
                    .set("title", title)
                    .set("description", description)
                    .set("url", url);

            if (StrUtil.isNotBlank(picUrl)) {
                json.set("picurl", picUrl);
            }

            return json;
        }
    }

    /**
     * 企业微信机器人消息发送结果。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record WeComRobotResult(int httpStatus, Integer errcode, String errmsg, String rawBody) {

        /**
         * 判断是否发送成功。
         *
         * @return true 表示发送成功
         */
        public boolean success() {
            return httpStatus >= 200 && httpStatus < 300 && Integer.valueOf(SUCCESS_CODE).equals(errcode);
        }

        /**
         * 解析企业微信响应。
         *
         * @param httpStatus HTTP状态码
         * @param rawBody    原始响应体
         * @return 发送结果
         */
        public static WeComRobotResult parse(int httpStatus, String rawBody) {
            if (StrUtil.isBlank(rawBody)) {
                return new WeComRobotResult(httpStatus, -1, "响应内容为空", rawBody);
            }

            try {
                JSONObject json = JSONUtil.parseObj(rawBody);
                Integer errcode = Convert.toInt(json.get("errcode"), -1);
                String errmsg = StrUtil.blankToDefault(json.getStr("errmsg"), rawBody);
                return new WeComRobotResult(httpStatus, errcode, errmsg, rawBody);
            } catch (Exception ex) {
                return new WeComRobotResult(httpStatus, -1, "响应内容不是合法JSON：" + rawBody, rawBody);
            }
        }

        /**
         * 创建异常失败结果。
         *
         * @param ex 异常
         * @return 发送结果
         */
        public static WeComRobotResult fail(Exception ex) {
            return new WeComRobotResult(-1, -1, ex.getMessage(), null);
        }

        /**
         * 从媒体上传结果转换为发送结果。
         *
         * @param mediaResult 媒体上传结果
         * @return 发送结果
         */
        public static WeComRobotResult fromMediaResult(WeComMediaResult mediaResult) {
            return new WeComRobotResult(
                    mediaResult.httpStatus(),
                    mediaResult.errcode(),
                    mediaResult.errmsg(),
                    mediaResult.rawBody()
            );
        }
    }

    /**
     * 企业微信机器人媒体上传结果。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record WeComMediaResult(int httpStatus,
                                   Integer errcode,
                                   String errmsg,
                                   String type,
                                   String mediaId,
                                   Long createdAt,
                                   String rawBody) {

        /**
         * 判断是否上传成功。
         *
         * @return true 表示上传成功
         */
        public boolean success() {
            return httpStatus >= 200
                    && httpStatus < 300
                    && Integer.valueOf(SUCCESS_CODE).equals(errcode)
                    && StrUtil.isNotBlank(mediaId);
        }

        /**
         * 解析企业微信媒体上传响应。
         *
         * @param httpStatus HTTP状态码
         * @param rawBody    原始响应体
         * @return 媒体上传结果
         */
        public static WeComMediaResult parse(int httpStatus, String rawBody) {
            if (StrUtil.isBlank(rawBody)) {
                return new WeComMediaResult(httpStatus, -1, "响应内容为空", null, null, null, rawBody);
            }

            try {
                JSONObject json = JSONUtil.parseObj(rawBody);
                Integer errcode = Convert.toInt(json.get("errcode"), -1);
                String errmsg = StrUtil.blankToDefault(json.getStr("errmsg"), rawBody);
                String type = json.getStr("type");
                String mediaId = json.getStr("media_id");
                Long createdAt = Convert.toLong(json.get("created_at"), null);

                return new WeComMediaResult(httpStatus, errcode, errmsg, type, mediaId, createdAt, rawBody);
            } catch (Exception ex) {
                return new WeComMediaResult(httpStatus, -1, "响应内容不是合法JSON：" + rawBody, null, null, null, rawBody);
            }
        }

        /**
         * 创建异常失败结果。
         *
         * @param ex 异常
         * @return 媒体上传结果
         */
        public static WeComMediaResult fail(Exception ex) {
            return new WeComMediaResult(-1, -1, ex.getMessage(), null, null, null, null);
        }
    }

    /**
     * 企业微信机器人异常。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public static class WeComRobotException extends RuntimeException {

        /**
         * 创建企业微信机器人异常。
         *
         * @param message 异常消息
         */
        public WeComRobotException(String message) {
            super(message);
        }

        /**
         * 创建企业微信机器人异常。
         *
         * @param message 异常消息
         * @param cause   原始异常
         */
        public WeComRobotException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 企业微信机器人客户端构建器。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public static class Builder {

        private String webhook;

        private String robotKey;

        private int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;

        private boolean throwExceptionOnFail = true;

        /**
         * 设置机器人 key。
         *
         * @param robotKey 企业微信群机器人 key
         * @return 当前构建器
         */
        public Builder robotKey(String robotKey) {
            this.robotKey = robotKey;
            return this;
        }

        /**
         * 设置完整 webhook。
         *
         * @param webhook 企业微信群机器人完整 webhook
         * @return 当前构建器
         */
        public Builder webhook(String webhook) {
            this.webhook = webhook;
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
         * 构建企业微信机器人客户端。
         *
         * @return 企业微信机器人客户端
         */
        public WeComRobotUtil build() {
            if (StrUtil.isBlank(webhook) && StrUtil.isBlank(robotKey)) {
                throw new IllegalArgumentException("webhook和robotKey不能同时为空");
            }

            String finalWebhook = StrUtil.isNotBlank(webhook)
                    ? webhook.trim()
                    : buildWebhookByKey(robotKey);

            String finalRobotKey = StrUtil.isNotBlank(robotKey)
                    ? robotKey.trim()
                    : extractKeyFromWebhook(finalWebhook);

            this.webhook = finalWebhook;
            this.robotKey = finalRobotKey;

            return new WeComRobotUtil(this);
        }
    }
}
