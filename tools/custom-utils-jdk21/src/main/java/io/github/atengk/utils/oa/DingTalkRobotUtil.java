package io.github.atengk.utils.oa;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 钉钉自定义机器人消息发送工具类。
 *
 * @author Ateng
 * @since 2026-05-14
 */
public final class DingTalkRobotUtil {

    private static final Log log = LogFactory.get(DingTalkRobotUtil.class);

    private static final String ROBOT_SEND_URL = "https://oapi.dingtalk.com/robot/send";

    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;

    private static final int SUCCESS_CODE = 0;

    private final String webhook;

    private final String secret;

    private final int timeoutMillis;

    private final boolean throwExceptionOnFail;

    private DingTalkRobotUtil(Builder builder) {
        this.webhook = requireNotBlank(builder.webhook, "webhook");
        this.secret = StrUtil.trimToNull(builder.secret);
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
     * 使用完整 webhook 创建客户端。
     *
     * @param webhook 钉钉机器人完整 webhook
     * @return 钉钉机器人客户端
     */
    public static DingTalkRobotUtil ofWebhook(String webhook) {
        return builder()
                .webhook(webhook)
                .build();
    }

    /**
     * 使用完整 webhook 和加签密钥创建客户端。
     *
     * @param webhook 钉钉机器人完整 webhook
     * @param secret  加签密钥
     * @return 钉钉机器人客户端
     */
    public static DingTalkRobotUtil ofWebhook(String webhook, String secret) {
        return builder()
                .webhook(webhook)
                .secret(secret)
                .build();
    }

    /**
     * 使用 access_token 创建客户端。
     *
     * @param accessToken 钉钉机器人 access_token
     * @return 钉钉机器人客户端
     */
    public static DingTalkRobotUtil ofAccessToken(String accessToken) {
        return builder()
                .accessToken(accessToken)
                .build();
    }

    /**
     * 使用 access_token 和加签密钥创建客户端。
     *
     * @param accessToken 钉钉机器人 access_token
     * @param secret      加签密钥
     * @return 钉钉机器人客户端
     */
    public static DingTalkRobotUtil ofAccessToken(String accessToken, String secret) {
        return builder()
                .accessToken(accessToken)
                .secret(secret)
                .build();
    }

    /**
     * 发送文本消息。
     *
     * @param content 消息内容
     * @return 发送结果
     */
    public DingTalkRobotResult sendText(String content) {
        return sendText(content, At.none());
    }

    /**
     * 发送文本消息。
     *
     * @param content 消息内容
     * @param at      @配置
     * @return 发送结果
     */
    public DingTalkRobotResult sendText(String content, At at) {
        requireNotBlank(content, "文本消息内容");

        JSONObject payload = createBasePayload(MessageType.TEXT);
        payload.set("text", JSONUtil.createObj()
                .set("content", content));
        setAt(payload, at);

        return send(payload);
    }

    /**
     * 发送 Markdown 消息。
     *
     * @param title Markdown 标题
     * @param text  Markdown 内容
     * @return 发送结果
     */
    public DingTalkRobotResult sendMarkdown(String title, String text) {
        return sendMarkdown(title, text, At.none());
    }

    /**
     * 发送 Markdown 消息。
     *
     * @param title Markdown 标题
     * @param text  Markdown 内容
     * @param at    @配置
     * @return 发送结果
     */
    public DingTalkRobotResult sendMarkdown(String title, String text, At at) {
        requireNotBlank(title, "Markdown标题");
        requireNotBlank(text, "Markdown内容");

        JSONObject payload = createBasePayload(MessageType.MARKDOWN);
        payload.set("markdown", JSONUtil.createObj()
                .set("title", title)
                .set("text", text));
        setAt(payload, at);

        return send(payload);
    }

    /**
     * 发送链接消息。
     *
     * @param title      链接标题
     * @param text       链接描述
     * @param messageUrl 跳转地址
     * @param picUrl     图片地址，可为空
     * @return 发送结果
     */
    public DingTalkRobotResult sendLink(String title, String text, String messageUrl, String picUrl) {
        requireNotBlank(title, "链接标题");
        requireNotBlank(text, "链接描述");
        requireNotBlank(messageUrl, "链接跳转地址");

        JSONObject link = JSONUtil.createObj()
                .set("title", title)
                .set("text", text)
                .set("messageUrl", messageUrl);

        if (StrUtil.isNotBlank(picUrl)) {
            link.set("picUrl", picUrl);
        }

        JSONObject payload = createBasePayload(MessageType.LINK);
        payload.set("link", link);

        return send(payload);
    }

    /**
     * 发送单按钮 ActionCard 消息。
     *
     * @param title       卡片标题
     * @param text        Markdown 格式卡片内容
     * @param singleTitle 单按钮标题
     * @param singleUrl   单按钮跳转地址
     * @return 发送结果
     */
    public DingTalkRobotResult sendSingleActionCard(String title, String text, String singleTitle, String singleUrl) {
        requireNotBlank(title, "ActionCard标题");
        requireNotBlank(text, "ActionCard内容");
        requireNotBlank(singleTitle, "ActionCard单按钮标题");
        requireNotBlank(singleUrl, "ActionCard单按钮地址");

        JSONObject actionCard = JSONUtil.createObj()
                .set("title", title)
                .set("text", text)
                .set("singleTitle", singleTitle)
                .set("singleURL", singleUrl);

        JSONObject payload = createBasePayload(MessageType.ACTION_CARD);
        payload.set("actionCard", actionCard);

        return send(payload);
    }

    /**
     * 发送多按钮 ActionCard 消息。
     *
     * @param title             卡片标题
     * @param text              Markdown 格式卡片内容
     * @param horizontalButtons 是否横向排列按钮
     * @param buttons           按钮集合
     * @return 发送结果
     */
    public DingTalkRobotResult sendMultiActionCard(String title,
                                                   String text,
                                                   boolean horizontalButtons,
                                                   Collection<ActionButton> buttons) {
        requireNotBlank(title, "ActionCard标题");
        requireNotBlank(text, "ActionCard内容");

        if (CollUtil.isEmpty(buttons)) {
            throw new IllegalArgumentException("ActionCard按钮不能为空");
        }

        JSONArray buttonArray = JSONUtil.createArray();
        buttons.stream()
                .filter(Objects::nonNull)
                .map(ActionButton::toJson)
                .forEach(buttonArray::add);

        if (buttonArray.isEmpty()) {
            throw new IllegalArgumentException("ActionCard有效按钮不能为空");
        }

        JSONObject actionCard = JSONUtil.createObj()
                .set("title", title)
                .set("text", text)
                .set("btnOrientation", horizontalButtons ? "1" : "0")
                .set("btns", buttonArray);

        JSONObject payload = createBasePayload(MessageType.ACTION_CARD);
        payload.set("actionCard", actionCard);

        return send(payload);
    }

    /**
     * 发送 FeedCard 消息。
     *
     * @param links FeedCard链接集合
     * @return 发送结果
     */
    public DingTalkRobotResult sendFeedCard(Collection<FeedLink> links) {
        if (CollUtil.isEmpty(links)) {
            throw new IllegalArgumentException("FeedCard链接不能为空");
        }

        JSONArray linkArray = JSONUtil.createArray();
        links.stream()
                .filter(Objects::nonNull)
                .map(FeedLink::toJson)
                .forEach(linkArray::add);

        if (linkArray.isEmpty()) {
            throw new IllegalArgumentException("FeedCard有效链接不能为空");
        }

        JSONObject feedCard = JSONUtil.createObj()
                .set("links", linkArray);

        JSONObject payload = createBasePayload(MessageType.FEED_CARD);
        payload.set("feedCard", feedCard);

        return send(payload);
    }

    /**
     * 发送自定义 JSON 消息。
     *
     * @param payload 钉钉机器人消息体
     * @return 发送结果
     */
    public DingTalkRobotResult send(JSONObject payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("钉钉机器人消息体不能为空");
        }

        String requestUrl = buildRequestUrl();
        String requestBody = JSONUtil.toJsonStr(payload);

        try (HttpResponse response = HttpRequest.post(requestUrl)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestBody)
                .timeout(timeoutMillis)
                .execute()) {

            DingTalkRobotResult result = DingTalkRobotResult.parse(response.getStatus(), response.body());

            if (result.success()) {
                log.debug("钉钉机器人消息发送成功，msgtype={}", payload.getStr("msgtype"));
                return result;
            }

            log.warn("钉钉机器人消息发送失败，httpStatus={}，errcode={}，errmsg={}",
                    result.httpStatus(), result.errcode(), result.errmsg());

            if (throwExceptionOnFail) {
                throw new DingTalkRobotException(StrUtil.format(
                        "钉钉机器人消息发送失败，httpStatus={}，errcode={}，errmsg={}",
                        result.httpStatus(),
                        result.errcode(),
                        result.errmsg()
                ));
            }

            return result;
        } catch (DingTalkRobotException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(ex, "钉钉机器人消息发送异常：{}", ex.getMessage());

            if (throwExceptionOnFail) {
                throw new DingTalkRobotException("钉钉机器人消息发送异常：" + ex.getMessage(), ex);
            }

            return DingTalkRobotResult.fail(ex);
        }
    }

    /**
     * 构建当前请求地址。
     *
     * @return 请求地址
     */
    public String buildRequestUrl() {
        if (StrUtil.isBlank(secret)) {
            return webhook;
        }

        long timestamp = System.currentTimeMillis();
        String sign = createSign(timestamp, secret);

        UrlQuery query = new UrlQuery()
                .add("timestamp", timestamp)
                .add("sign", sign);

        return appendQuery(webhook, query);
    }

    /**
     * 创建钉钉机器人加签。
     *
     * @param timestamp 当前时间戳，单位毫秒
     * @param secret    加签密钥
     * @return 加签结果
     */
    public static String createSign(long timestamp, String secret) {
        requireNotBlank(secret, "钉钉机器人加签密钥");

        String content = timestamp + "\n" + secret;
        HMac hMac = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        byte[] digest = hMac.digest(content.getBytes(StandardCharsets.UTF_8));

        return Base64.encode(digest);
    }

    /**
     * 创建服务告警 Markdown 内容。
     *
     * @param title   告警标题
     * @param appName 应用名称
     * @param level   告警级别
     * @param content 告警内容
     * @return Markdown 内容
     */
    public static String buildAlarmMarkdown(String title, String appName, String level, String content) {
        return StrUtil.builder()
                .append("### ").append(StrUtil.blankToDefault(title, "服务告警")).append("\n\n")
                .append("- 应用名称：").append(StrUtil.blankToDefault(appName, "-")).append("\n")
                .append("- 告警级别：").append(StrUtil.blankToDefault(level, "-")).append("\n")
                .append("- 告警内容：").append(StrUtil.blankToDefault(content, "-")).append("\n")
                .append("- 触发时间：").append(LocalDateTime.now()).append("\n")
                .toString();
    }

    private static JSONObject createBasePayload(MessageType messageType) {
        return JSONUtil.createObj()
                .set("msgtype", messageType.code);
    }

    private static void setAt(JSONObject payload, At at) {
        At actualAt = at == null ? At.none() : at;
        if (actualAt.enabled()) {
            payload.set("at", actualAt.toJson());
        }
    }

    private static String buildWebhookByAccessToken(String accessToken) {
        requireNotBlank(accessToken, "access_token");

        UrlQuery query = new UrlQuery()
                .add("access_token", accessToken);

        return ROBOT_SEND_URL + "?" + query.build(CharsetUtil.CHARSET_UTF_8);
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
     * 钉钉机器人消息类型。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    private enum MessageType {

        TEXT("text"),

        MARKDOWN("markdown"),

        LINK("link"),

        ACTION_CARD("actionCard"),

        FEED_CARD("feedCard");

        private final String code;

        MessageType(String code) {
            this.code = code;
        }
    }

    /**
     * 钉钉机器人@配置。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record At(List<String> atMobiles, List<String> atUserIds, boolean atAll) {

        public At {
            atMobiles = cleanList(atMobiles);
            atUserIds = cleanList(atUserIds);
        }

        /**
         * 不@任何人。
         *
         * @return @配置
         */
        public static At none() {
            return new At(List.of(), List.of(), false);
        }

        /**
         * @所有人。
         *
         * @return @配置
         */
        public static At all() {
            return new At(List.of(), List.of(), true);
        }

        /**
         * 按手机号@用户。
         *
         * @param mobiles 手机号
         * @return @配置
         */
        public static At mobiles(String... mobiles) {
            return new At(cleanArray(mobiles), List.of(), false);
        }

        /**
         * 按用户ID@用户。
         *
         * @param userIds 用户ID
         * @return @配置
         */
        public static At userIds(String... userIds) {
            return new At(List.of(), cleanArray(userIds), false);
        }

        /**
         * 创建@配置。
         *
         * @param atMobiles 手机号集合
         * @param atUserIds 用户ID集合
         * @param atAll     是否@所有人
         * @return @配置
         */
        public static At of(Collection<String> atMobiles, Collection<String> atUserIds, boolean atAll) {
            return new At(cleanList(atMobiles), cleanList(atUserIds), atAll);
        }

        private boolean enabled() {
            return atAll || CollUtil.isNotEmpty(atMobiles) || CollUtil.isNotEmpty(atUserIds);
        }

        private JSONObject toJson() {
            JSONObject json = JSONUtil.createObj()
                    .set("isAtAll", atAll);

            if (CollUtil.isNotEmpty(atMobiles)) {
                json.set("atMobiles", atMobiles);
            }

            if (CollUtil.isNotEmpty(atUserIds)) {
                json.set("atUserIds", atUserIds);
            }

            return json;
        }
    }

    /**
     * 钉钉 ActionCard 按钮。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record ActionButton(String title, String actionUrl) {

        public ActionButton {
            title = requireNotBlank(title, "按钮标题");
            actionUrl = requireNotBlank(actionUrl, "按钮跳转地址");
        }

        /**
         * 创建 ActionCard 按钮。
         *
         * @param title     按钮标题
         * @param actionUrl 按钮跳转地址
         * @return 按钮对象
         */
        public static ActionButton of(String title, String actionUrl) {
            return new ActionButton(title, actionUrl);
        }

        private JSONObject toJson() {
            return JSONUtil.createObj()
                    .set("title", title)
                    .set("actionURL", actionUrl);
        }
    }

    /**
     * 钉钉 FeedCard 链接。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record FeedLink(String title, String messageUrl, String picUrl) {

        public FeedLink {
            title = requireNotBlank(title, "FeedCard标题");
            messageUrl = requireNotBlank(messageUrl, "FeedCard跳转地址");
            picUrl = requireNotBlank(picUrl, "FeedCard图片地址");
        }

        /**
         * 创建 FeedCard 链接。
         *
         * @param title      标题
         * @param messageUrl 跳转地址
         * @param picUrl     图片地址
         * @return FeedCard 链接
         */
        public static FeedLink of(String title, String messageUrl, String picUrl) {
            return new FeedLink(title, messageUrl, picUrl);
        }

        private JSONObject toJson() {
            return JSONUtil.createObj()
                    .set("title", title)
                    .set("messageURL", messageUrl)
                    .set("picURL", picUrl);
        }
    }

    /**
     * 钉钉机器人发送结果。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public record DingTalkRobotResult(int httpStatus, Integer errcode, String errmsg, String rawBody) {

        /**
         * 判断是否发送成功。
         *
         * @return true 表示成功
         */
        public boolean success() {
            return httpStatus >= 200 && httpStatus < 300 && Integer.valueOf(SUCCESS_CODE).equals(errcode);
        }

        /**
         * 解析钉钉响应。
         *
         * @param httpStatus HTTP状态码
         * @param rawBody    原始响应体
         * @return 发送结果
         */
        public static DingTalkRobotResult parse(int httpStatus, String rawBody) {
            if (StrUtil.isBlank(rawBody)) {
                return new DingTalkRobotResult(httpStatus, -1, "响应内容为空", rawBody);
            }

            try {
                JSONObject json = JSONUtil.parseObj(rawBody);
                Integer errcode = Convert.toInt(json.get("errcode"), -1);
                String errmsg = StrUtil.blankToDefault(json.getStr("errmsg"), rawBody);
                return new DingTalkRobotResult(httpStatus, errcode, errmsg, rawBody);
            } catch (Exception ex) {
                return new DingTalkRobotResult(httpStatus, -1, "响应内容不是合法JSON：" + rawBody, rawBody);
            }
        }

        /**
         * 创建失败结果。
         *
         * @param ex 异常
         * @return 发送结果
         */
        public static DingTalkRobotResult fail(Exception ex) {
            return new DingTalkRobotResult(-1, -1, ex.getMessage(), null);
        }
    }

    /**
     * 钉钉机器人异常。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public static class DingTalkRobotException extends RuntimeException {

        /**
         * 创建钉钉机器人异常。
         *
         * @param message 异常消息
         */
        public DingTalkRobotException(String message) {
            super(message);
        }

        /**
         * 创建钉钉机器人异常。
         *
         * @param message 异常消息
         * @param cause   原始异常
         */
        public DingTalkRobotException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 钉钉机器人客户端构建器。
     *
     * @author Ateng
     * @since 2026-05-14
     */
    public static class Builder {

        private String webhook;

        private String accessToken;

        private String secret;

        private int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;

        private boolean throwExceptionOnFail = true;

        /**
         * 设置完整 webhook。
         *
         * @param webhook 钉钉机器人完整 webhook
         * @return 当前构建器
         */
        public Builder webhook(String webhook) {
            this.webhook = webhook;
            return this;
        }

        /**
         * 设置 access_token。
         *
         * @param accessToken 钉钉机器人 access_token
         * @return 当前构建器
         */
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /**
         * 设置加签密钥。
         *
         * @param secret 加签密钥
         * @return 当前构建器
         */
        public Builder secret(String secret) {
            this.secret = secret;
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
         * 构建钉钉机器人客户端。
         *
         * @return 钉钉机器人客户端
         */
        public DingTalkRobotUtil build() {
            if (StrUtil.isBlank(webhook) && StrUtil.isBlank(accessToken)) {
                throw new IllegalArgumentException("webhook和access_token不能同时为空");
            }

            String finalWebhook = StrUtil.isNotBlank(webhook)
                    ? webhook.trim()
                    : buildWebhookByAccessToken(accessToken);

            this.webhook = finalWebhook;
            return new DingTalkRobotUtil(this);
        }
    }

}