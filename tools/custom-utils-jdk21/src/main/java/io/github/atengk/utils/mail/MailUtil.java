package io.github.atengk.utils.mail;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromStringTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.RecipientStringTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SentDateTerm;
import jakarta.mail.search.SubjectTerm;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件工具类，基于 JDK 21 与 Jakarta Mail 提供邮件发送、接收、解析、附件、模板、校验等通用能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class MailUtil {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String DEFAULT_INBOX = "INBOX";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final long DEFAULT_MAX_ATTACHMENT_SIZE = 25L * 1024L * 1024L;
    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}|\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private MailUtil() {
        throw new UnsupportedOperationException("MailUtil 是静态工具类，禁止实例化");
    }

    /**
     * 邮件服务器配置。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailConfig {
        public String protocol;
        public String host;
        public int port;
        public String username;
        public String password;
        public boolean auth = true;
        public boolean ssl;
        public boolean startTls;
        public boolean debug;
        public int connectionTimeoutMillis = 10_000;
        public int timeoutMillis = 10_000;
        public int writeTimeoutMillis = 10_000;
        public Properties extraProperties = new Properties();

        /**
         * 创建空邮件配置。
         */
        public MailConfig() {
        }
    }

    /**
     * 邮件附件描述。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailAttachment {
        public String fileName;
        public String contentType;
        public byte[] content;
        public Path path;
        public boolean inline;
        public String contentId;

        /**
         * 创建空附件描述。
         */
        public MailAttachment() {
        }
    }

    /**
     * 邮件消息描述。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailMessage {
        public String from;
        public String fromName;
        public List<String> replyTo = new ArrayList<>();
        public List<String> to = new ArrayList<>();
        public List<String> cc = new ArrayList<>();
        public List<String> bcc = new ArrayList<>();
        public String subject;
        public String content;
        public boolean html;
        public Charset charset = DEFAULT_CHARSET;
        public Date sentDate;
        public List<MailAttachment> attachments = new ArrayList<>();
        public List<MailAttachment> inlineResources = new ArrayList<>();
        public Map<String, String> headers = new LinkedHashMap<>();

        /**
         * 创建空邮件消息。
         */
        public MailMessage() {
        }
    }

    /**
     * 邮件模板描述。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailTemplate {
        public String subjectTemplate;
        public String contentTemplate;
        public Map<String, ?> variables = new LinkedHashMap<>();
        public boolean html = true;

        /**
         * 创建空邮件模板。
         */
        public MailTemplate() {
        }
    }

    /**
     * 邮件发送结果。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailSendResult {
        public boolean success;
        public String messageId;
        public String errorMessage;
        public String rootCauseMessage;
        public long costMillis;
        public Throwable exception;

        /**
         * 创建空发送结果。
         */
        public MailSendResult() {
        }
    }

    /**
     * 批量邮件发送结果。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailBatchSendResult {
        public int total;
        public int successCount;
        public int failureCount;
        public List<MailSendResult> results = new ArrayList<>();

        /**
         * 创建空批量发送结果。
         */
        public MailBatchSendResult() {
        }
    }

    /**
     * 邮件读取结果。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailReadResult {
        public String messageId;
        public String subject;
        public String from;
        public List<String> to = new ArrayList<>();
        public List<String> cc = new ArrayList<>();
        public List<String> bcc = new ArrayList<>();
        public Date sentDate;
        public Date receivedDate;
        public String textContent;
        public String htmlContent;
        public Map<String, List<String>> headers = new LinkedHashMap<>();
        public List<MailAttachment> attachments = new ArrayList<>();
        public List<MailAttachment> inlineResources = new ArrayList<>();

        /**
         * 创建空读取结果。
         */
        public MailReadResult() {
        }
    }

    /**
     * 邮件搜索条件。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailSearchCondition {
        public String subject;
        public String sender;
        public String recipient;
        public Date startDate;
        public Date endDate;
        public Boolean unread;
        public Boolean flagged;
        public Boolean hasAttachment;

        /**
         * 创建空搜索条件。
         */
        public MailSearchCondition() {
        }
    }

    /**
     * 邮件文件夹摘要。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class MailFolderInfo {
        public String fullName;
        public String name;
        public int messageCount;
        public int unreadCount;
        public boolean open;

        /**
         * 创建空文件夹摘要。
         */
        public MailFolderInfo() {
        }
    }

    /**
     * 创建 SMTP 邮件发送配置。
     *
     * @param host     SMTP 主机
     * @param port     SMTP 端口
     * @param username 用户名
     * @param password 密码或授权码
     * @param ssl      是否启用 SSL
     * @param startTls 是否启用 STARTTLS
     * @return SMTP 配置
     */
    public static MailConfig createSmtpConfig(String host, int port, String username, String password, boolean ssl, boolean startTls) {
        MailConfig config = createConfig("smtp", host, port, username, password, ssl, startTls);
        if (ssl) {
            config.protocol = "smtps";
        }
        return config;
    }

    /**
     * 创建 IMAP 邮件接收配置。
     *
     * @param host     IMAP 主机
     * @param port     IMAP 端口
     * @param username 用户名
     * @param password 密码或授权码
     * @param ssl      是否启用 SSL
     * @return IMAP 配置
     */
    public static MailConfig createImapConfig(String host, int port, String username, String password, boolean ssl) {
        MailConfig config = createConfig("imap", host, port, username, password, ssl, false);
        if (ssl) {
            config.protocol = "imaps";
        }
        return config;
    }

    /**
     * 创建 POP3 邮件接收配置。
     *
     * @param host     POP3 主机
     * @param port     POP3 端口
     * @param username 用户名
     * @param password 密码或授权码
     * @param ssl      是否启用 SSL
     * @return POP3 配置
     */
    public static MailConfig createPop3Config(String host, int port, String username, String password, boolean ssl) {
        MailConfig config = createConfig("pop3", host, port, username, password, ssl, false);
        if (ssl) {
            config.protocol = "pop3s";
        }
        return config;
    }

    /**
     * 将邮件配置转换为 Jakarta Mail 属性。
     *
     * @param config 邮件配置
     * @return 邮件属性
     */
    public static Properties toProperties(MailConfig config) {
        validateConfig(config);
        Properties properties = new Properties();
        String protocol = normalizeProtocol(config.protocol);
        properties.put("mail.transport.protocol", protocol.startsWith("smtp") ? protocol : "smtp");
        properties.put("mail.store.protocol", protocol);
        properties.put("mail." + protocol + ".host", config.host);
        properties.put("mail." + protocol + ".port", String.valueOf(config.port));
        properties.put("mail." + protocol + ".auth", String.valueOf(config.auth));
        properties.put("mail." + protocol + ".connectiontimeout", String.valueOf(config.connectionTimeoutMillis));
        properties.put("mail." + protocol + ".timeout", String.valueOf(config.timeoutMillis));
        properties.put("mail." + protocol + ".writetimeout", String.valueOf(config.writeTimeoutMillis));
        if (protocol.startsWith("smtp")) {
            properties.put("mail.smtp.host", config.host);
            properties.put("mail.smtp.port", String.valueOf(config.port));
            properties.put("mail.smtp.auth", String.valueOf(config.auth));
            properties.put("mail.smtp.starttls.enable", String.valueOf(config.startTls));
            properties.put("mail.smtp.ssl.enable", String.valueOf(config.ssl || "smtps".equals(protocol)));
            properties.put("mail.smtp.connectiontimeout", String.valueOf(config.connectionTimeoutMillis));
            properties.put("mail.smtp.timeout", String.valueOf(config.timeoutMillis));
            properties.put("mail.smtp.writetimeout", String.valueOf(config.writeTimeoutMillis));
        }
        if (config.ssl) {
            properties.put("mail." + protocol + ".ssl.enable", "true");
        }
        if (config.extraProperties != null) {
            properties.putAll(config.extraProperties);
        }
        properties.put("mail.debug", String.valueOf(config.debug));
        return properties;
    }

    /**
     * 根据邮件配置构建邮件会话。
     *
     * @param config 邮件配置
     * @return 邮件会话
     */
    public static Session buildSession(MailConfig config) {
        validateConfig(config);
        Properties properties = toProperties(config);
        Authenticator authenticator = null;
        if (config.auth && hasText(config.username)) {
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.username, nullToEmpty(config.password));
                }
            };
        }
        Session session = Session.getInstance(properties, authenticator);
        session.setDebug(config.debug);
        return session;
    }

    /**
     * 校验邮件配置是否完整。
     *
     * @param config 邮件配置
     */
    public static void validateConfig(MailConfig config) {
        requireNonNull(config, "邮件配置不能为空");
        requireText(config.protocol, "邮件协议不能为空");
        requireText(config.host, "邮件服务器主机不能为空");
        if (config.port <= 0 || config.port > 65535) {
            throw new IllegalArgumentException("邮件服务器端口必须在 1 到 65535 之间");
        }
        if (config.connectionTimeoutMillis < 0 || config.timeoutMillis < 0 || config.writeTimeoutMillis < 0) {
            throw new IllegalArgumentException("邮件超时时间不能为负数");
        }
    }

    /**
     * 测试 SMTP 连接是否可用。
     *
     * @param config SMTP 配置
     * @return 连接成功返回 true，否则返回 false
     */
    public static boolean testSmtpConnection(MailConfig config) {
        try {
            Session session = buildSession(config);
            Transport transport = null;
            try {
                transport = session.getTransport(normalizeProtocol(config.protocol));
                transport.connect(config.host, config.port, config.username, config.password);
                return true;
            } finally {
                if (transport != null && transport.isConnected()) {
                    transport.close();
                }
            }
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 测试 IMAP 连接是否可用。
     *
     * @param config IMAP 配置
     * @return 连接成功返回 true，否则返回 false
     */
    public static boolean testImapConnection(MailConfig config) {
        return testStoreConnection(config);
    }

    /**
     * 测试 POP3 连接是否可用。
     *
     * @param config POP3 配置
     * @return 连接成功返回 true，否则返回 false
     */
    public static boolean testPop3Connection(MailConfig config) {
        return testStoreConnection(config);
    }

    /**
     * 按配置协议测试连接是否可用。
     *
     * @param config 邮件配置
     * @return 连接成功返回 true，否则返回 false
     */
    public static boolean checkConnection(MailConfig config) {
        validateConfig(config);
        String protocol = normalizeProtocol(config.protocol);
        if (protocol.startsWith("smtp")) {
            return testSmtpConnection(config);
        }
        return testStoreConnection(config);
    }

    /**
     * 安静关闭自动关闭资源。
     *
     * @param closeable 待关闭资源
     */
    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // 静默关闭
        }
    }

    /**
     * 安静关闭邮件存储连接。
     *
     * @param store 邮件存储连接
     */
    public static void closeQuietly(Store store) {
        if (store == null) {
            return;
        }
        try {
            if (store.isConnected()) {
                store.close();
            }
        } catch (MessagingException ignored) {
            // 静默关闭
        }
    }

    /**
     * 安静关闭邮件文件夹。
     *
     * @param folder  邮件文件夹
     * @param expunge 是否同步删除已标记删除的邮件
     */
    public static void closeQuietly(Folder folder, boolean expunge) {
        if (folder == null) {
            return;
        }
        try {
            if (folder.isOpen()) {
                folder.close(expunge);
            }
        } catch (MessagingException ignored) {
            // 静默关闭
        }
    }

    /**
     * 判断邮箱地址是否合法。
     *
     * @param address 邮箱地址
     * @return 合法返回 true，否则返回 false
     */
    public static boolean isValidAddress(String address) {
        if (!hasText(address)) {
            return false;
        }
        String trimmed = address.trim();
        try {
            InternetAddress internetAddress = new InternetAddress(trimmed, true);
            internetAddress.validate();
            return SIMPLE_EMAIL_PATTERN.matcher(internetAddress.getAddress()).matches();
        } catch (AddressException ex) {
            return false;
        }
    }

    /**
     * 判断邮箱地址集合是否全部合法。
     *
     * @param addresses 邮箱地址集合
     * @return 全部合法返回 true，否则返回 false
     */
    public static boolean isValidAddressList(Collection<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return false;
        }
        for (String address : addresses) {
            if (!isValidAddress(address)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断邮箱地址字符串列表是否全部合法。
     *
     * @param addresses 邮箱地址字符串，支持逗号和分号分隔
     * @return 全部合法返回 true，否则返回 false
     */
    public static boolean isValidAddressList(String addresses) {
        return isValidAddressList(splitAddresses(addresses));
    }

    /**
     * 解析单个邮箱地址。
     *
     * @param address 邮箱地址
     * @return InternetAddress 对象
     */
    public static InternetAddress parseAddress(String address) {
        requireText(address, "邮箱地址不能为空");
        try {
            InternetAddress internetAddress = new InternetAddress(address.trim(), true);
            internetAddress.validate();
            return internetAddress;
        } catch (AddressException ex) {
            throw new IllegalArgumentException("邮箱地址格式错误: " + address, ex);
        }
    }

    /**
     * 解析邮箱地址列表。
     *
     * @param addresses 邮箱地址集合
     * @return InternetAddress 列表
     */
    public static List<InternetAddress> parseAddressList(Collection<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }
        List<InternetAddress> result = new ArrayList<>();
        for (String address : addresses) {
            if (hasText(address)) {
                result.add(parseAddress(address));
            }
        }
        return result;
    }

    /**
     * 格式化邮箱地址和显示名称。
     *
     * @param address 邮箱地址
     * @param personal 显示名称
     * @return 格式化后的邮箱地址
     */
    public static String formatAddress(String address, String personal) {
        try {
            InternetAddress internetAddress = new InternetAddress(parseAddress(address).getAddress(), personal, DEFAULT_CHARSET.name());
            return internetAddress.toUnicodeString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("邮箱地址格式化失败", ex);
        }
    }

    /**
     * 标准化邮箱地址。
     *
     * @param address 邮箱地址
     * @return 小写且去除首尾空白后的邮箱地址
     */
    public static String normalizeAddress(String address) {
        if (!hasText(address)) {
            return "";
        }
        return Normalizer.normalize(parseAddress(address).getAddress().trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    /**
     * 去除重复邮箱地址。
     *
     * @param addresses 邮箱地址集合
     * @return 去重后的邮箱地址列表
     */
    public static List<String> removeDuplicateAddresses(Collection<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String address : addresses) {
            if (isValidAddress(address)) {
                normalized.add(normalizeAddress(address));
            }
        }
        return new ArrayList<>(normalized);
    }

    /**
     * 过滤非法邮箱地址。
     *
     * @param addresses 邮箱地址集合
     * @return 合法邮箱地址列表
     */
    public static List<String> filterInvalidAddresses(Collection<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String address : addresses) {
            if (isValidAddress(address)) {
                result.add(normalizeAddress(address));
            }
        }
        return result;
    }

    /**
     * 拆分邮箱地址字符串。
     *
     * @param addresses 邮箱地址字符串，支持逗号和分号分隔
     * @return 邮箱地址列表
     */
    public static List<String> splitAddresses(String addresses) {
        if (!hasText(addresses)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : addresses.split("[;,]")) {
            if (hasText(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    /**
     * 拼接邮箱地址列表。
     *
     * @param addresses 邮箱地址集合
     * @return 逗号分隔的邮箱地址字符串
     */
    public static String joinAddresses(Collection<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return "";
        }
        return String.join(",", removeDuplicateAddresses(addresses));
    }

    /**
     * 构建纯文本邮件。
     *
     * @param session 邮件会话
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param content 正文
     * @return MIME 邮件
     */
    public static MimeMessage buildTextMessage(Session session, String from, Collection<String> to, String subject, String content) {
        MailMessage message = new MailMessage();
        message.from = from;
        message.to.addAll(to == null ? List.of() : to);
        message.subject = subject;
        message.content = content;
        message.html = false;
        return buildMimeMessage(session, message);
    }

    /**
     * 构建 HTML 邮件。
     *
     * @param session 邮件会话
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param html    HTML 正文
     * @return MIME 邮件
     */
    public static MimeMessage buildHtmlMessage(Session session, String from, Collection<String> to, String subject, String html) {
        MailMessage message = new MailMessage();
        message.from = from;
        message.to.addAll(to == null ? List.of() : to);
        message.subject = subject;
        message.content = html;
        message.html = true;
        return buildMimeMessage(session, message);
    }

    /**
     * 根据邮件消息描述构建 MIME 邮件。
     *
     * @param session     邮件会话
     * @param mailMessage 邮件消息描述
     * @return MIME 邮件
     */
    public static MimeMessage buildMimeMessage(Session session, MailMessage mailMessage) {
        requireNonNull(session, "邮件会话不能为空");
        validateMessage(mailMessage);
        try {
            MimeMessage mimeMessage = new MimeMessage(session);
            setFrom(mimeMessage, mailMessage.from, mailMessage.fromName);
            if (!mailMessage.replyTo.isEmpty()) {
                setReplyTo(mimeMessage, mailMessage.replyTo);
            }
            setRecipients(mimeMessage, Message.RecipientType.TO, mailMessage.to);
            setCcRecipients(mimeMessage, mailMessage.cc);
            setBccRecipients(mimeMessage, mailMessage.bcc);
            setSubject(mimeMessage, mailMessage.subject, charsetName(mailMessage.charset));
            if (mailMessage.sentDate != null) {
                setSentDate(mimeMessage, mailMessage.sentDate);
            } else {
                setSentDate(mimeMessage, new Date());
            }
            for (Map.Entry<String, String> entry : safeMap(mailMessage.headers).entrySet()) {
                if (hasText(entry.getKey()) && entry.getValue() != null) {
                    mimeMessage.setHeader(entry.getKey(), entry.getValue());
                }
            }
            if (mailMessage.attachments.isEmpty() && mailMessage.inlineResources.isEmpty()) {
                setContent(mimeMessage, mailMessage.content, mailMessage.html, charsetName(mailMessage.charset));
            } else {
                mimeMessage.setContent(buildMultipartContent(mailMessage.content, mailMessage.html, mailMessage.charset, mailMessage.attachments, mailMessage.inlineResources));
            }
            mimeMessage.saveChanges();
            return mimeMessage;
        } catch (MessagingException ex) {
            throw new IllegalStateException("构建 MIME 邮件失败", ex);
        }
    }

    /**
     * 根据邮件消息描述构建邮件。
     *
     * @param session     邮件会话
     * @param mailMessage 邮件消息描述
     * @return MIME 邮件
     */
    public static MimeMessage buildMessage(Session session, MailMessage mailMessage) {
        return buildMimeMessage(session, mailMessage);
    }

    /**
     * 设置邮件发件人。
     *
     * @param message  MIME 邮件
     * @param from     发件人地址
     * @param personal 发件人显示名称
     */
    public static void setFrom(MimeMessage message, String from, String personal) {
        requireNonNull(message, "邮件对象不能为空");
        requireText(from, "发件人不能为空");
        try {
            InternetAddress address = hasText(personal)
                    ? new InternetAddress(parseAddress(from).getAddress(), personal, DEFAULT_CHARSET.name())
                    : parseAddress(from);
            message.setFrom(address);
        } catch (Exception ex) {
            throw new IllegalArgumentException("设置发件人失败", ex);
        }
    }

    /**
     * 设置邮件回复地址。
     *
     * @param message 邮件对象
     * @param replyTo 回复地址集合
     */
    public static void setReplyTo(MimeMessage message, Collection<String> replyTo) {
        requireNonNull(message, "邮件对象不能为空");
        try {
            message.setReplyTo(toAddressArray(replyTo));
        } catch (MessagingException ex) {
            throw new IllegalStateException("设置回复地址失败", ex);
        }
    }

    /**
     * 设置指定类型收件人。
     *
     * @param message 邮件对象
     * @param type    收件人类型
     * @param addresses 邮箱地址集合
     */
    public static void setRecipients(MimeMessage message, Message.RecipientType type, Collection<String> addresses) {
        requireNonNull(message, "邮件对象不能为空");
        requireNonNull(type, "收件人类型不能为空");
        if (addresses == null || addresses.isEmpty()) {
            return;
        }
        try {
            message.setRecipients(type, toAddressArray(addresses));
        } catch (MessagingException ex) {
            throw new IllegalArgumentException("设置收件人失败", ex);
        }
    }

    /**
     * 设置抄送人。
     *
     * @param message 邮件对象
     * @param addresses 抄送地址集合
     */
    public static void setCcRecipients(MimeMessage message, Collection<String> addresses) {
        setRecipients(message, Message.RecipientType.CC, addresses);
    }

    /**
     * 设置密送人。
     *
     * @param message 邮件对象
     * @param addresses 密送地址集合
     */
    public static void setBccRecipients(MimeMessage message, Collection<String> addresses) {
        setRecipients(message, Message.RecipientType.BCC, addresses);
    }

    /**
     * 设置邮件主题。
     *
     * @param message 邮件对象
     * @param subject 邮件主题
     * @param charset 字符集名称
     */
    public static void setSubject(MimeMessage message, String subject, String charset) {
        requireNonNull(message, "邮件对象不能为空");
        try {
            message.setSubject(nullToEmpty(subject), defaultCharset(charset));
        } catch (MessagingException ex) {
            throw new IllegalStateException("设置邮件主题失败", ex);
        }
    }

    /**
     * 设置邮件正文内容。
     *
     * @param message 邮件对象
     * @param content 正文内容
     * @param html    是否 HTML 正文
     * @param charset 字符集名称
     */
    public static void setContent(MimeMessage message, String content, boolean html, String charset) {
        requireNonNull(message, "邮件对象不能为空");
        try {
            String type = html ? "text/html; charset=" + defaultCharset(charset) : "text/plain; charset=" + defaultCharset(charset);
            message.setContent(nullToEmpty(content), type);
        } catch (MessagingException ex) {
            throw new IllegalStateException("设置邮件正文失败", ex);
        }
    }

    /**
     * 设置邮件发送时间。
     *
     * @param message 邮件对象
     * @param sentDate 发送时间
     */
    public static void setSentDate(MimeMessage message, Date sentDate) {
        requireNonNull(message, "邮件对象不能为空");
        requireNonNull(sentDate, "发送时间不能为空");
        try {
            message.setSentDate(sentDate);
        } catch (MessagingException ex) {
            throw new IllegalStateException("设置发送时间失败", ex);
        }
    }

    /**
     * 复制 MIME 邮件对象。
     *
     * @param message 原始 MIME 邮件
     * @return 复制后的 MIME 邮件
     */
    public static MimeMessage copyMessage(MimeMessage message) {
        requireNonNull(message, "邮件对象不能为空");
        try {
            return new MimeMessage(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("复制邮件失败", ex);
        }
    }

    /**
     * 复制邮件消息描述。
     *
     * @param source 原始邮件消息描述
     * @return 复制后的邮件消息描述
     */
    public static MailMessage copyMessage(MailMessage source) {
        requireNonNull(source, "邮件消息不能为空");
        MailMessage target = new MailMessage();
        target.from = source.from;
        target.fromName = source.fromName;
        target.replyTo = new ArrayList<>(source.replyTo);
        target.to = new ArrayList<>(source.to);
        target.cc = new ArrayList<>(source.cc);
        target.bcc = new ArrayList<>(source.bcc);
        target.subject = source.subject;
        target.content = source.content;
        target.html = source.html;
        target.charset = source.charset;
        target.sentDate = source.sentDate == null ? null : new Date(source.sentDate.getTime());
        target.attachments = new ArrayList<>(source.attachments);
        target.inlineResources = new ArrayList<>(source.inlineResources);
        target.headers = new LinkedHashMap<>(source.headers);
        return target;
    }

    /**
     * 发送已构建的 MIME 邮件。
     *
     * @param message MIME 邮件
     */
    public static void send(MimeMessage message) {
        requireNonNull(message, "邮件对象不能为空");
        try {
            Transport.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("发送邮件失败", ex);
        }
    }

    /**
     * 按配置发送邮件消息。
     *
     * @param config      邮件配置
     * @param mailMessage 邮件消息描述
     * @return 发送结果
     */
    public static MailSendResult send(MailConfig config, MailMessage mailMessage) {
        Instant start = Instant.now();
        try {
            Session session = buildSession(config);
            MimeMessage mimeMessage = buildMimeMessage(session, mailMessage);
            Transport.send(mimeMessage);
            return buildSuccessResult(extractMessageId(mimeMessage), calculateCostTime(start));
        } catch (Exception ex) {
            return buildFailureResult(ex, calculateCostTime(start));
        }
    }

    /**
     * 发送纯文本邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人集合
     * @param subject 主题
     * @param content 正文
     * @return 发送结果
     */
    public static MailSendResult sendText(MailConfig config, String from, Collection<String> to, String subject, String content) {
        MailMessage message = baseMessage(from, to, subject, content, false);
        return send(config, message);
    }

    /**
     * 发送 HTML 邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人集合
     * @param subject 主题
     * @param html    HTML 正文
     * @return 发送结果
     */
    public static MailSendResult sendHtml(MailConfig config, String from, Collection<String> to, String subject, String html) {
        MailMessage message = baseMessage(from, to, subject, html, true);
        return send(config, message);
    }

    /**
     * 发送带附件邮件。
     *
     * @param config      邮件配置
     * @param mailMessage 邮件消息描述
     * @param attachments 附件集合
     * @return 发送结果
     */
    public static MailSendResult sendWithAttachment(MailConfig config, MailMessage mailMessage, Collection<MailAttachment> attachments) {
        MailMessage copy = copyMessage(mailMessage);
        if (attachments != null) {
            copy.attachments.addAll(attachments);
        }
        return send(config, copy);
    }

    /**
     * 发送带内嵌资源邮件。
     *
     * @param config          邮件配置
     * @param mailMessage     邮件消息描述
     * @param inlineResources 内嵌资源集合
     * @return 发送结果
     */
    public static MailSendResult sendWithInlineResource(MailConfig config, MailMessage mailMessage, Collection<MailAttachment> inlineResources) {
        MailMessage copy = copyMessage(mailMessage);
        if (inlineResources != null) {
            copy.inlineResources.addAll(inlineResources);
        }
        return send(config, copy);
    }

    /**
     * 发送模板邮件。
     *
     * @param config   邮件配置
     * @param from     发件人
     * @param to       收件人集合
     * @param template 邮件模板
     * @return 发送结果
     */
    public static MailSendResult sendTemplate(MailConfig config, String from, Collection<String> to, MailTemplate template) {
        return send(config, buildTemplateMessage(from, to, template));
    }

    /**
     * 批量发送邮件。
     *
     * @param config   邮件配置
     * @param messages 邮件消息集合
     * @return 发送结果列表
     */
    public static List<MailSendResult> sendBatch(MailConfig config, Collection<MailMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<MailSendResult> results = new ArrayList<>();
        for (MailMessage message : messages) {
            results.add(send(config, message));
        }
        return results;
    }

    /**
     * 异步发送邮件。
     *
     * @param config      邮件配置
     * @param mailMessage 邮件消息描述
     * @return 异步发送结果
     */
    public static CompletableFuture<MailSendResult> sendAsync(MailConfig config, MailMessage mailMessage) {
        return CompletableFuture.supplyAsync(() -> send(config, mailMessage));
    }

    /**
     * 使用指定线程执行器异步发送邮件。
     *
     * @param config      邮件配置
     * @param mailMessage 邮件消息描述
     * @param executor    线程执行器
     * @return 异步发送结果
     */
    public static CompletableFuture<MailSendResult> sendAsync(MailConfig config, MailMessage mailMessage, Executor executor) {
        requireNonNull(executor, "线程执行器不能为空");
        return CompletableFuture.supplyAsync(() -> send(config, mailMessage), executor);
    }

    /**
     * 异步批量发送邮件。
     *
     * @param config   邮件配置
     * @param messages 邮件消息集合
     * @return 异步批量发送结果
     */
    public static CompletableFuture<List<MailSendResult>> sendBatchAsync(MailConfig config, Collection<MailMessage> messages) {
        return CompletableFuture.supplyAsync(() -> sendBatch(config, messages));
    }

    /**
     * 带重试机制发送邮件。
     *
     * @param config      邮件配置
     * @param mailMessage 邮件消息描述
     * @param retryTimes  重试次数
     * @param interval    重试间隔
     * @return 最终发送结果
     */
    public static MailSendResult sendWithRetry(MailConfig config, MailMessage mailMessage, int retryTimes, Duration interval) {
        if (retryTimes < 0) {
            throw new IllegalArgumentException("重试次数不能为负数");
        }
        Duration safeInterval = interval == null ? Duration.ZERO : interval;
        MailSendResult result = null;
        for (int i = 0; i <= retryTimes; i++) {
            result = send(config, mailMessage);
            if (result.success || i == retryTimes) {
                return result;
            }
            sleepQuietly(safeInterval);
        }
        return result;
    }

    /**
     * 静默发送邮件，异常会转换为失败结果。
     *
     * @param config      邮件配置
     * @param mailMessage 邮件消息描述
     * @return 发送结果
     */
    public static MailSendResult sendQuietly(MailConfig config, MailMessage mailMessage) {
        return send(config, mailMessage);
    }

    /**
     * 发送邮件并返回结果。
     *
     * @param config      邮件配置
     * @param mailMessage 邮件消息描述
     * @return 发送结果
     */
    public static MailSendResult sendAndReturnResult(MailConfig config, MailMessage mailMessage) {
        return send(config, mailMessage);
    }

    /**
     * 重新发送邮件。
     *
     * @param config      邮件配置
     * @param mailMessage 邮件消息描述
     * @return 发送结果
     */
    public static MailSendResult resend(MailConfig config, MailMessage mailMessage) {
        return send(config, mailMessage);
    }

    /**
     * 添加收件人。
     *
     * @param message 邮件消息
     * @param address 收件人地址
     * @return 邮件消息
     */
    public static MailMessage addTo(MailMessage message, String address) {
        requireNonNull(message, "邮件消息不能为空");
        message.to.add(normalizeAddress(address));
        return message;
    }

    /**
     * 添加抄送人。
     *
     * @param message 邮件消息
     * @param address 抄送地址
     * @return 邮件消息
     */
    public static MailMessage addCc(MailMessage message, String address) {
        requireNonNull(message, "邮件消息不能为空");
        message.cc.add(normalizeAddress(address));
        return message;
    }

    /**
     * 添加密送人。
     *
     * @param message 邮件消息
     * @param address 密送地址
     * @return 邮件消息
     */
    public static MailMessage addBcc(MailMessage message, String address) {
        requireNonNull(message, "邮件消息不能为空");
        message.bcc.add(normalizeAddress(address));
        return message;
    }

    /**
     * 添加回复地址。
     *
     * @param message 邮件消息
     * @param address 回复地址
     * @return 邮件消息
     */
    public static MailMessage addReplyTo(MailMessage message, String address) {
        requireNonNull(message, "邮件消息不能为空");
        message.replyTo.add(normalizeAddress(address));
        return message;
    }

    /**
     * 合并多个收件人集合并去重。
     *
     * @param recipientGroups 收件人集合数组
     * @return 合并后的收件人列表
     */
    @SafeVarargs
    public static List<String> mergeRecipients(Collection<String>... recipientGroups) {
        if (recipientGroups == null || recipientGroups.length == 0) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (Collection<String> group : recipientGroups) {
            result.addAll(removeDuplicateAddresses(group));
        }
        return new ArrayList<>(result);
    }

    /**
     * 判断邮件消息是否包含指定收件人。
     *
     * @param message 邮件消息
     * @param address 邮箱地址
     * @return 包含返回 true，否则返回 false
     */
    public static boolean hasRecipient(MailMessage message, String address) {
        requireNonNull(message, "邮件消息不能为空");
        String normalized = normalizeAddress(address);
        return message.to.contains(normalized) || message.cc.contains(normalized) || message.bcc.contains(normalized);
    }

    /**
     * 统计邮件消息收件人总数。
     *
     * @param message 邮件消息
     * @return 收件人数量
     */
    public static int countRecipients(MailMessage message) {
        requireNonNull(message, "邮件消息不能为空");
        return safeList(message.to).size() + safeList(message.cc).size() + safeList(message.bcc).size();
    }

    /**
     * 清空邮件消息所有收件人。
     *
     * @param message 邮件消息
     */
    public static void clearRecipients(MailMessage message) {
        requireNonNull(message, "邮件消息不能为空");
        message.to.clear();
        message.cc.clear();
        message.bcc.clear();
        message.replyTo.clear();
    }

    /**
     * 校验邮件收件人是否合法。
     *
     * @param message 邮件消息
     */
    public static void validateRecipients(MailMessage message) {
        requireNonNull(message, "邮件消息不能为空");
        if (countRecipients(message) == 0) {
            throw new IllegalArgumentException("至少需要一个收件人");
        }
        List<String> all = new ArrayList<>();
        all.addAll(safeList(message.to));
        all.addAll(safeList(message.cc));
        all.addAll(safeList(message.bcc));
        if (!isValidAddressList(all)) {
            throw new IllegalArgumentException("存在非法收件人地址");
        }
    }

    /**
     * 按收件人类型分组。
     *
     * @param message 邮件消息
     * @return 收件人类型分组
     */
    public static Map<String, List<String>> groupRecipients(MailMessage message) {
        requireNonNull(message, "邮件消息不能为空");
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("to", new ArrayList<>(safeList(message.to)));
        result.put("cc", new ArrayList<>(safeList(message.cc)));
        result.put("bcc", new ArrayList<>(safeList(message.bcc)));
        result.put("replyTo", new ArrayList<>(safeList(message.replyTo)));
        return result;
    }

    /**
     * 添加附件。
     *
     * @param message    邮件消息
     * @param attachment 附件
     * @return 邮件消息
     */
    public static MailMessage addAttachment(MailMessage message, MailAttachment attachment) {
        requireNonNull(message, "邮件消息不能为空");
        validateAttachment(attachment);
        message.attachments.add(attachment);
        return message;
    }

    /**
     * 添加文件附件。
     *
     * @param message 邮件消息
     * @param path    文件路径
     * @return 邮件消息
     */
    public static MailMessage addFileAttachment(MailMessage message, Path path) {
        return addAttachment(message, buildAttachment(path));
    }

    /**
     * 添加字节数组附件。
     *
     * @param message     邮件消息
     * @param fileName    文件名
     * @param content     附件内容
     * @param contentType 内容类型
     * @return 邮件消息
     */
    public static MailMessage addByteAttachment(MailMessage message, String fileName, byte[] content, String contentType) {
        return addAttachment(message, buildAttachment(fileName, content, contentType));
    }

    /**
     * 添加输入流附件。
     *
     * @param message     邮件消息
     * @param fileName    文件名
     * @param inputStream 输入流
     * @param contentType 内容类型
     * @return 邮件消息
     */
    public static MailMessage addStreamAttachment(MailMessage message, String fileName, InputStream inputStream, String contentType) {
        requireNonNull(inputStream, "附件输入流不能为空");
        try {
            return addAttachment(message, buildAttachment(fileName, inputStream.readAllBytes(), contentType));
        } catch (IOException ex) {
            throw new UncheckedIOException("读取附件输入流失败", ex);
        }
    }

    /**
     * 添加内嵌资源。
     *
     * @param message     邮件消息
     * @param contentId   内容 ID
     * @param fileName    文件名
     * @param content     内容
     * @param contentType 内容类型
     * @return 邮件消息
     */
    public static MailMessage addInlineResource(MailMessage message, String contentId, String fileName, byte[] content, String contentType) {
        requireNonNull(message, "邮件消息不能为空");
        MailAttachment attachment = buildAttachment(fileName, content, contentType);
        attachment.inline = true;
        attachment.contentId = requireText(contentId, "内容 ID 不能为空");
        message.inlineResources.add(attachment);
        return message;
    }

    /**
     * 根据文件路径构建附件。
     *
     * @param path 文件路径
     * @return 附件描述
     */
    public static MailAttachment buildAttachment(Path path) {
        requireNonNull(path, "附件路径不能为空");
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("附件文件不存在或不是普通文件: " + path);
        }
        MailAttachment attachment = new MailAttachment();
        attachment.path = path;
        attachment.fileName = path.getFileName().toString();
        attachment.contentType = detectContentType(path);
        return attachment;
    }

    /**
     * 根据字节内容构建附件。
     *
     * @param fileName    文件名
     * @param content     附件内容
     * @param contentType 内容类型
     * @return 附件描述
     */
    public static MailAttachment buildAttachment(String fileName, byte[] content, String contentType) {
        requireText(fileName, "附件文件名不能为空");
        requireNonNull(content, "附件内容不能为空");
        MailAttachment attachment = new MailAttachment();
        attachment.fileName = fileName;
        attachment.content = Arrays.copyOf(content, content.length);
        attachment.contentType = hasText(contentType) ? contentType : DEFAULT_CONTENT_TYPE;
        return attachment;
    }

    /**
     * 获取附件名称。
     *
     * @param attachment 附件
     * @return 附件名称
     */
    public static String getAttachmentName(MailAttachment attachment) {
        requireNonNull(attachment, "附件不能为空");
        if (hasText(attachment.fileName)) {
            return attachment.fileName;
        }
        if (attachment.path != null && attachment.path.getFileName() != null) {
            return attachment.path.getFileName().toString();
        }
        return "attachment";
    }

    /**
     * 识别文件内容类型。
     *
     * @param path 文件路径
     * @return 内容类型
     */
    public static String detectContentType(Path path) {
        requireNonNull(path, "文件路径不能为空");
        try {
            String contentType = Files.probeContentType(path);
            return hasText(contentType) ? contentType : DEFAULT_CONTENT_TYPE;
        } catch (IOException ex) {
            return DEFAULT_CONTENT_TYPE;
        }
    }

    /**
     * 校验附件是否合法。
     *
     * @param attachment 附件
     */
    public static void validateAttachment(MailAttachment attachment) {
        requireNonNull(attachment, "附件不能为空");
        if (!hasText(attachment.fileName) && attachment.path == null) {
            throw new IllegalArgumentException("附件文件名不能为空");
        }
        if (attachment.path == null && attachment.content == null) {
            throw new IllegalArgumentException("附件内容不能为空");
        }
        validateAttachmentSize(attachment, DEFAULT_MAX_ATTACHMENT_SIZE);
    }

    /**
     * 校验附件大小。
     *
     * @param attachment 附件
     * @param maxBytes   最大字节数
     */
    public static void validateAttachmentSize(MailAttachment attachment, long maxBytes) {
        requireNonNull(attachment, "附件不能为空");
        if (maxBytes < 0) {
            throw new IllegalArgumentException("最大附件大小不能为负数");
        }
        long size = attachment.content != null ? attachment.content.length : fileSize(attachment.path);
        if (size > maxBytes) {
            throw new IllegalArgumentException("附件大小超过限制: " + size + " bytes");
        }
    }

    /**
     * 校验附件类型是否在允许范围内。
     *
     * @param attachment          附件
     * @param allowedContentTypes 允许的内容类型集合
     */
    public static void validateAttachmentType(MailAttachment attachment, Collection<String> allowedContentTypes) {
        requireNonNull(attachment, "附件不能为空");
        if (!isAllowedAttachmentType(attachment, allowedContentTypes)) {
            throw new IllegalArgumentException("附件类型不允许: " + attachment.contentType);
        }
    }

    /**
     * 从邮件内容读取附件。
     *
     * @param part 邮件部分
     * @return 附件列表
     */
    public static List<MailAttachment> readAttachments(Part part) {
        List<MailAttachment> result = new ArrayList<>();
        collectAttachments(part, false, result);
        return result;
    }

    /**
     * 保存邮件附件到目录。
     *
     * @param part      邮件部分
     * @param directory 保存目录
     * @return 已保存文件路径列表
     */
    public static List<Path> saveAttachments(Part part, Path directory) {
        requireNonNull(directory, "保存目录不能为空");
        try {
            Files.createDirectories(directory);
            List<Path> paths = new ArrayList<>();
            for (MailAttachment attachment : readAttachments(part)) {
                String safeName = sanitizeFileName(getAttachmentName(attachment));
                Path target = directory.resolve(safeName);
                byte[] content = attachment.content;
                if (content == null && attachment.path != null) {
                    content = Files.readAllBytes(attachment.path);
                }
                Files.write(target, content == null ? new byte[0] : content);
                paths.add(target);
            }
            return paths;
        } catch (IOException ex) {
            throw new UncheckedIOException("保存附件失败", ex);
        }
    }

    /**
     * 从邮件消息中移除指定附件。
     *
     * @param message  邮件消息
     * @param fileName 附件文件名
     * @return 是否移除成功
     */
    public static boolean removeAttachment(MailMessage message, String fileName) {
        requireNonNull(message, "邮件消息不能为空");
        requireText(fileName, "附件文件名不能为空");
        return message.attachments.removeIf(item -> fileName.equals(getAttachmentName(item)));
    }

    /**
     * 渲染模板内容。
     *
     * @param template  模板内容，支持 ${name} 和 {{name}}
     * @param variables 模板变量
     * @return 渲染结果
     */
    public static String renderTemplate(String template, Map<String, ?> variables) {
        if (template == null) {
            return "";
        }
        Map<String, ?> safeVariables = variables == null ? Map.of() : variables;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            Object value = safeVariables.get(key);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    /**
     * 渲染简单模板内容。
     *
     * @param template  模板内容
     * @param variables 模板变量
     * @return 渲染结果
     */
    public static String renderSimpleTemplate(String template, Map<String, ?> variables) {
        return renderTemplate(template, variables);
    }

    /**
     * 从文件加载模板内容。
     *
     * @param path 模板文件路径
     * @return 模板内容
     */
    public static String loadTemplate(Path path) {
        requireNonNull(path, "模板路径不能为空");
        try {
            return Files.readString(path, DEFAULT_CHARSET);
        } catch (IOException ex) {
            throw new UncheckedIOException("加载邮件模板失败", ex);
        }
    }

    /**
     * 发送模板邮件。
     *
     * @param config   邮件配置
     * @param from     发件人
     * @param to       收件人集合
     * @param template 模板
     * @return 发送结果
     */
    public static MailSendResult sendTemplateMail(MailConfig config, String from, Collection<String> to, MailTemplate template) {
        return sendTemplate(config, from, to, template);
    }

    /**
     * 构建模板邮件消息。
     *
     * @param from     发件人
     * @param to       收件人集合
     * @param template 模板
     * @return 邮件消息
     */
    public static MailMessage buildTemplateMessage(String from, Collection<String> to, MailTemplate template) {
        requireNonNull(template, "邮件模板不能为空");
        validateTemplateVariables(template.contentTemplate, template.variables);
        MailMessage message = new MailMessage();
        message.from = from;
        message.to.addAll(to == null ? List.of() : to);
        message.subject = renderTemplate(template.subjectTemplate, template.variables);
        message.content = renderTemplate(template.contentTemplate, template.variables);
        message.html = template.html;
        return message;
    }

    /**
     * 校验模板变量是否完整。
     *
     * @param template  模板内容
     * @param variables 模板变量
     */
    public static void validateTemplateVariables(String template, Map<String, ?> variables) {
        Set<String> placeholders = extractPlaceholders(template);
        Map<String, ?> safeVariables = variables == null ? Map.of() : variables;
        for (String placeholder : placeholders) {
            if (!safeVariables.containsKey(placeholder)) {
                throw new IllegalArgumentException("缺少模板变量: " + placeholder);
            }
        }
    }

    /**
     * 替换模板占位符。
     *
     * @param template  模板内容
     * @param variables 模板变量
     * @return 替换结果
     */
    public static String replacePlaceholders(String template, Map<String, ?> variables) {
        return renderTemplate(template, variables);
    }

    /**
     * 提取模板占位符名称。
     *
     * @param template 模板内容
     * @return 占位符集合
     */
    public static Set<String> extractPlaceholders(String template) {
        if (template == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            result.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        }
        return result;
    }

    /**
     * 对模板变量执行 HTML 转义。
     *
     * @param variables 模板变量
     * @return 转义后的变量
     */
    public static Map<String, String> escapeHtmlVariables(Map<String, ?> variables) {
        if (variables == null || variables.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            result.put(entry.getKey(), escapeHtml(String.valueOf(entry.getValue())));
        }
        return result;
    }

    /**
     * 预览模板渲染结果。
     *
     * @param template  模板内容
     * @param variables 模板变量
     * @return 预览内容
     */
    public static String previewTemplate(String template, Map<String, ?> variables) {
        return renderTemplate(template, variables);
    }

    /**
     * 编码文本内容。
     *
     * @param text 文本
     * @return 编码后的文本
     */
    public static String encodeText(String text) {
        try {
            return MimeUtility.encodeText(nullToEmpty(text), DEFAULT_CHARSET.name(), null);
        } catch (Exception ex) {
            throw new IllegalArgumentException("文本编码失败", ex);
        }
    }

    /**
     * 解码文本内容。
     *
     * @param text 文本
     * @return 解码后的文本
     */
    public static String decodeText(String text) {
        try {
            return MimeUtility.decodeText(nullToEmpty(text));
        } catch (Exception ex) {
            throw new IllegalArgumentException("文本解码失败", ex);
        }
    }

    /**
     * 编码邮件主题。
     *
     * @param subject 邮件主题
     * @return 编码后的主题
     */
    public static String encodeSubject(String subject) {
        return encodeText(subject);
    }

    /**
     * 解码邮件主题。
     *
     * @param subject 邮件主题
     * @return 解码后的主题
     */
    public static String decodeSubject(String subject) {
        return decodeText(subject);
    }

    /**
     * 将普通文本包装为简单 HTML 内容。
     *
     * @param content 文本内容
     * @return HTML 内容
     */
    public static String toHtmlContent(String content) {
        return "<html><body>" + escapeHtml(nullToEmpty(content)).replace("\n", "<br>") + "</body></html>";
    }

    /**
     * 将 HTML 内容转换为纯文本。
     *
     * @param html HTML 内容
     * @return 纯文本内容
     */
    public static String toPlainText(String html) {
        return unescapeHtml(stripHtml(html)).replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();
    }

    /**
     * 去除 HTML 标签。
     *
     * @param html HTML 内容
     * @return 去除标签后的文本
     */
    public static String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return HTML_TAG_PATTERN.matcher(html.replaceAll("(?i)<br\\s*/?>", "\n")).replaceAll("");
    }

    /**
     * HTML 转义。
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * HTML 反转义。
     *
     * @param text 转义文本
     * @return 原始文本
     */
    public static String unescapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    /**
     * 获取邮件文本正文。
     *
     * @param part 邮件部分
     * @return 文本正文
     */
    public static String getTextContent(Part part) {
        return getContentByMimeType(part, "text/plain");
    }

    /**
     * 获取邮件 HTML 正文。
     *
     * @param part 邮件部分
     * @return HTML 正文
     */
    public static String getHtmlContent(Part part) {
        return getContentByMimeType(part, "text/html");
    }

    /**
     * 判断内容是否为 HTML。
     *
     * @param content 内容
     * @return 是 HTML 返回 true，否则返回 false
     */
    public static boolean isHtmlContent(String content) {
        if (!hasText(content)) {
            return false;
        }
        return Pattern.compile("<\\s*(html|body|div|p|span|br|table|a|img)[\\s>/]", Pattern.CASE_INSENSITIVE).matcher(content).find();
    }

    /**
     * 构建包含正文和附件的多部分内容。
     *
     * @param content     正文
     * @param html        是否 HTML 正文
     * @param attachments 附件集合
     * @return MIME 多部分内容
     */
    public static MimeMultipart buildMultipartContent(String content, boolean html, Collection<MailAttachment> attachments) {
        return buildMultipartContent(content, html, DEFAULT_CHARSET, attachments, List.of());
    }

    /**
     * 解析多部分邮件内容。
     *
     * @param part 邮件部分
     * @return text、html、attachments 三类内容
     */
    public static Map<String, Object> parseMultipartContent(Part part) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", getTextContent(part));
        result.put("html", getHtmlContent(part));
        result.put("attachments", readAttachments(part));
        return result;
    }

    /**
     * 查询文件夹邮件列表。
     *
     * @param folder 邮件文件夹
     * @return 邮件数组
     */
    public static Message[] listMessages(Folder folder) {
        requireOpenFolder(folder);
        try {
            return folder.getMessages();
        } catch (MessagingException ex) {
            throw new IllegalStateException("查询邮件列表失败", ex);
        }
    }

    /**
     * 查询未读邮件列表。
     *
     * @param folder 邮件文件夹
     * @return 未读邮件数组
     */
    public static Message[] listUnreadMessages(Folder folder) {
        return searchUnreadMessages(folder);
    }

    /**
     * 读取单封邮件。
     *
     * @param message 邮件
     * @return 邮件读取结果
     */
    public static MailReadResult readMessage(Message message) {
        return parseMessage(message);
    }

    /**
     * 批量读取邮件。
     *
     * @param messages 邮件数组
     * @return 邮件读取结果列表
     */
    public static List<MailReadResult> readMessages(Message[] messages) {
        if (messages == null || messages.length == 0) {
            return List.of();
        }
        List<MailReadResult> results = new ArrayList<>();
        for (Message message : messages) {
            results.add(readMessage(message));
        }
        return results;
    }

    /**
     * 读取最新邮件。
     *
     * @param folder 邮件文件夹
     * @return 最新邮件，文件夹为空时返回 null
     */
    public static Message readLatestMessage(Folder folder) {
        requireOpenFolder(folder);
        try {
            int count = folder.getMessageCount();
            return count <= 0 ? null : folder.getMessage(count);
        } catch (MessagingException ex) {
            throw new IllegalStateException("读取最新邮件失败", ex);
        }
    }

    /**
     * 拉取指定文件夹邮件。
     *
     * @param store      邮件存储连接
     * @param folderName 文件夹名称
     * @param limit      最大数量
     * @return 邮件数组
     */
    public static Message[] fetchMessages(Store store, String folderName, int limit) {
        requireNonNull(store, "邮件存储连接不能为空");
        if (limit < 0) {
            throw new IllegalArgumentException("邮件数量限制不能为负数");
        }
        Folder folder = null;
        try {
            folder = openFolder(store.getFolder(hasText(folderName) ? folderName : DEFAULT_INBOX), Folder.READ_ONLY);
            Message[] messages = folder.getMessages();
            if (limit == 0 || messages.length <= limit) {
                return messages;
            }
            return Arrays.copyOfRange(messages, messages.length - limit, messages.length);
        } catch (MessagingException ex) {
            throw new IllegalStateException("拉取邮件失败", ex);
        } finally {
            closeQuietly(folder, false);
        }
    }

    /**
     * 拉取指定文件夹未读邮件。
     *
     * @param store      邮件存储连接
     * @param folderName 文件夹名称
     * @return 未读邮件数组
     */
    public static Message[] fetchUnreadMessages(Store store, String folderName) {
        requireNonNull(store, "邮件存储连接不能为空");
        Folder folder = null;
        try {
            folder = openFolder(store.getFolder(hasText(folderName) ? folderName : DEFAULT_INBOX), Folder.READ_ONLY);
            return listUnreadMessages(folder);
        } catch (MessagingException ex) {
            throw new IllegalStateException("拉取未读邮件失败", ex);
        } finally {
            closeQuietly(folder, false);
        }
    }

    /**
     * 统计文件夹邮件数量。
     *
     * @param folder 邮件文件夹
     * @return 邮件数量
     */
    public static int countMessages(Folder folder) {
        requireFolder(folder);
        try {
            return folder.getMessageCount();
        } catch (MessagingException ex) {
            throw new IllegalStateException("统计邮件数量失败", ex);
        }
    }

    /**
     * 统计文件夹未读邮件数量。
     *
     * @param folder 邮件文件夹
     * @return 未读数量
     */
    public static int countUnreadMessages(Folder folder) {
        requireFolder(folder);
        try {
            return folder.getUnreadMessageCount();
        } catch (MessagingException ex) {
            throw new IllegalStateException("统计未读邮件数量失败", ex);
        }
    }

    /**
     * 获取邮件头。
     *
     * @param message 邮件
     * @param name    头名称
     * @return 邮件头值
     */
    public static String getMessageHeader(Message message, String name) {
        requireNonNull(message, "邮件不能为空");
        requireText(name, "邮件头名称不能为空");
        try {
            String[] values = message.getHeader(name);
            return values == null || values.length == 0 ? null : values[0];
        } catch (MessagingException ex) {
            throw new IllegalStateException("获取邮件头失败", ex);
        }
    }

    /**
     * 获取邮件正文。
     *
     * @param message 邮件
     * @return 邮件正文，优先返回文本正文
     */
    public static String getMessageBody(Message message) {
        String text = getTextContent(message);
        return hasText(text) ? text : getHtmlContent(message);
    }

    /**
     * 获取邮件附件。
     *
     * @param message 邮件
     * @return 附件列表
     */
    public static List<MailAttachment> getMessageAttachments(Message message) {
        return readAttachments(message);
    }

    /**
     * 判断指定编号邮件是否存在。
     *
     * @param folder        邮件文件夹
     * @param messageNumber 邮件编号
     * @return 存在返回 true，否则返回 false
     */
    public static boolean existsMessage(Folder folder, int messageNumber) {
        requireFolder(folder);
        if (messageNumber <= 0) {
            return false;
        }
        try {
            return messageNumber <= folder.getMessageCount();
        } catch (MessagingException ex) {
            throw new IllegalStateException("判断邮件是否存在失败", ex);
        }
    }

    /**
     * 按条件搜索邮件。
     *
     * @param folder    邮件文件夹
     * @param condition 搜索条件
     * @return 邮件数组
     */
    public static Message[] searchMessages(Folder folder, MailSearchCondition condition) {
        requireOpenFolder(folder);
        requireNonNull(condition, "搜索条件不能为空");
        try {
            SearchTerm term = buildSearchTerm(condition);
            Message[] messages = term == null ? folder.getMessages() : folder.search(term);
            if (condition.hasAttachment == null) {
                return messages;
            }
            return filterMessages(messages, message -> hasAttachment(message) == condition.hasAttachment).toArray(Message[]::new);
        } catch (MessagingException ex) {
            throw new IllegalStateException("搜索邮件失败", ex);
        }
    }

    /**
     * 按主题搜索邮件。
     *
     * @param folder  邮件文件夹
     * @param subject 主题关键字
     * @return 邮件数组
     */
    public static Message[] searchBySubject(Folder folder, String subject) {
        requireText(subject, "主题关键字不能为空");
        MailSearchCondition condition = new MailSearchCondition();
        condition.subject = subject;
        return searchMessages(folder, condition);
    }

    /**
     * 按发件人搜索邮件。
     *
     * @param folder 邮件文件夹
     * @param sender 发件人关键字
     * @return 邮件数组
     */
    public static Message[] searchBySender(Folder folder, String sender) {
        requireText(sender, "发件人关键字不能为空");
        MailSearchCondition condition = new MailSearchCondition();
        condition.sender = sender;
        return searchMessages(folder, condition);
    }

    /**
     * 按收件人搜索邮件。
     *
     * @param folder    邮件文件夹
     * @param recipient 收件人关键字
     * @return 邮件数组
     */
    public static Message[] searchByRecipient(Folder folder, String recipient) {
        requireText(recipient, "收件人关键字不能为空");
        MailSearchCondition condition = new MailSearchCondition();
        condition.recipient = recipient;
        return searchMessages(folder, condition);
    }

    /**
     * 搜索未读邮件。
     *
     * @param folder 邮件文件夹
     * @return 未读邮件数组
     */
    public static Message[] searchUnreadMessages(Folder folder) {
        MailSearchCondition condition = new MailSearchCondition();
        condition.unread = true;
        return searchMessages(folder, condition);
    }

    /**
     * 搜索已标记邮件。
     *
     * @param folder 邮件文件夹
     * @return 已标记邮件数组
     */
    public static Message[] searchFlaggedMessages(Folder folder) {
        MailSearchCondition condition = new MailSearchCondition();
        condition.flagged = true;
        return searchMessages(folder, condition);
    }

    /**
     * 搜索带附件邮件。
     *
     * @param folder 邮件文件夹
     * @return 带附件邮件数组
     */
    public static Message[] searchMessagesWithAttachment(Folder folder) {
        MailSearchCondition condition = new MailSearchCondition();
        condition.hasAttachment = true;
        return searchMessages(folder, condition);
    }

    /**
     * 按接收时间范围搜索邮件。
     *
     * @param folder    邮件文件夹
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 邮件数组
     */
    public static Message[] searchByDateRange(Folder folder, Date startDate, Date endDate) {
        MailSearchCondition condition = new MailSearchCondition();
        condition.startDate = startDate;
        condition.endDate = endDate;
        return searchMessages(folder, condition);
    }

    /**
     * 使用自定义条件过滤邮件。
     *
     * @param messages  邮件数组
     * @param predicate 过滤条件
     * @return 过滤后的邮件列表
     */
    public static List<Message> filterMessages(Message[] messages, Predicate<Message> predicate) {
        requireNonNull(predicate, "过滤条件不能为空");
        if (messages == null || messages.length == 0) {
            return List.of();
        }
        List<Message> result = new ArrayList<>();
        for (Message message : messages) {
            if (predicate.test(message)) {
                result.add(message);
            }
        }
        return result;
    }

    /**
     * 对邮件数组排序。
     *
     * @param messages   邮件数组
     * @param comparator 排序器
     * @return 排序后的邮件列表
     */
    public static List<Message> sortMessages(Message[] messages, Comparator<Message> comparator) {
        requireNonNull(comparator, "排序器不能为空");
        List<Message> result = new ArrayList<>();
        if (messages != null) {
            result.addAll(Arrays.asList(messages));
        }
        result.sort(comparator);
        return result;
    }

    /**
     * 标记邮件为已读。
     *
     * @param message 邮件
     */
    public static void markAsRead(Message message) {
        setMessageFlag(message, Flags.Flag.SEEN, true);
    }

    /**
     * 标记邮件为未读。
     *
     * @param message 邮件
     */
    public static void markAsUnread(Message message) {
        setMessageFlag(message, Flags.Flag.SEEN, false);
    }

    /**
     * 标记邮件为重要。
     *
     * @param message 邮件
     */
    public static void markAsFlagged(Message message) {
        setMessageFlag(message, Flags.Flag.FLAGGED, true);
    }

    /**
     * 取消邮件重要标记。
     *
     * @param message 邮件
     */
    public static void unmarkFlagged(Message message) {
        setMessageFlag(message, Flags.Flag.FLAGGED, false);
    }

    /**
     * 删除邮件。
     *
     * @param message 邮件
     */
    public static void deleteMessage(Message message) {
        setMessageFlag(message, Flags.Flag.DELETED, true);
    }

    /**
     * 批量删除邮件。
     *
     * @param messages 邮件集合
     */
    public static void deleteMessages(Collection<? extends Message> messages) {
        if (messages == null) {
            return;
        }
        for (Message message : messages) {
            deleteMessage(message);
        }
    }

    /**
     * 移动邮件到目标文件夹。
     *
     * @param message      邮件
     * @param targetFolder 目标文件夹
     */
    public static void moveMessage(Message message, Folder targetFolder) {
        moveMessages(List.of(requireNonNull(message, "邮件不能为空")), targetFolder);
    }

    /**
     * 批量移动邮件到目标文件夹。
     *
     * @param messages     邮件集合
     * @param targetFolder 目标文件夹
     */
    public static void moveMessages(Collection<? extends Message> messages, Folder targetFolder) {
        requireNonNull(targetFolder, "目标文件夹不能为空");
        if (messages == null || messages.isEmpty()) {
            return;
        }
        try {
            Message[] array = messages.toArray(Message[]::new);
            Folder sourceFolder = array[0].getFolder();
            sourceFolder.copyMessages(array, targetFolder);
            deleteMessages(messages);
        } catch (MessagingException ex) {
            throw new IllegalStateException("移动邮件失败", ex);
        }
    }

    /**
     * 归档邮件到 Archive 文件夹。
     *
     * @param message 邮件
     */
    public static void archiveMessage(Message message) {
        requireNonNull(message, "邮件不能为空");
        try {
            Store store = message.getFolder().getStore();
            Folder archive = store.getFolder("Archive");
            if (!archive.exists()) {
                archive.create(Folder.HOLDS_MESSAGES);
            }
            moveMessage(message, archive);
        } catch (MessagingException ex) {
            throw new IllegalStateException("归档邮件失败", ex);
        }
    }

    /**
     * 复制邮件到目标文件夹。
     *
     * @param message      邮件
     * @param targetFolder 目标文件夹
     */
    public static void copyMessage(Message message, Folder targetFolder) {
        requireNonNull(message, "邮件不能为空");
        requireNonNull(targetFolder, "目标文件夹不能为空");
        try {
            message.getFolder().copyMessages(new Message[]{message}, targetFolder);
        } catch (MessagingException ex) {
            throw new IllegalStateException("复制邮件失败", ex);
        }
    }

    /**
     * 恢复邮件，即取消删除标记。
     *
     * @param message 邮件
     */
    public static void restoreMessage(Message message) {
        setMessageFlag(message, Flags.Flag.DELETED, false);
    }

    /**
     * 设置邮件标记。
     *
     * @param message 邮件
     * @param flag    标记
     * @param value   标记值
     */
    public static void setMessageFlag(Message message, Flags.Flag flag, boolean value) {
        requireNonNull(message, "邮件不能为空");
        requireNonNull(flag, "邮件标记不能为空");
        try {
            message.setFlag(flag, value);
        } catch (MessagingException ex) {
            throw new IllegalStateException("设置邮件标记失败", ex);
        }
    }

    /**
     * 查询所有文件夹。
     *
     * @param store 邮件存储连接
     * @return 文件夹列表
     */
    public static List<Folder> listFolders(Store store) {
        requireNonNull(store, "邮件存储连接不能为空");
        try {
            Folder root = store.getDefaultFolder();
            return Arrays.asList(root.list("*"));
        } catch (MessagingException ex) {
            throw new IllegalStateException("查询文件夹失败", ex);
        }
    }

    /**
     * 获取指定文件夹。
     *
     * @param store      邮件存储连接
     * @param folderName 文件夹名称
     * @return 文件夹
     */
    public static Folder getFolder(Store store, String folderName) {
        requireNonNull(store, "邮件存储连接不能为空");
        requireText(folderName, "文件夹名称不能为空");
        try {
            return store.getFolder(folderName);
        } catch (MessagingException ex) {
            throw new IllegalStateException("获取文件夹失败", ex);
        }
    }

    /**
     * 创建文件夹。
     *
     * @param store      邮件存储连接
     * @param folderName 文件夹名称
     * @return 创建成功返回 true，否则返回 false
     */
    public static boolean createFolder(Store store, String folderName) {
        try {
            Folder folder = getFolder(store, folderName);
            return folder.exists() || folder.create(Folder.HOLDS_MESSAGES);
        } catch (MessagingException ex) {
            throw new IllegalStateException("创建文件夹失败", ex);
        }
    }

    /**
     * 重命名文件夹。
     *
     * @param store   邮件存储连接
     * @param oldName 原文件夹名称
     * @param newName 新文件夹名称
     * @return 重命名成功返回 true，否则返回 false
     */
    public static boolean renameFolder(Store store, String oldName, String newName) {
        try {
            return getFolder(store, oldName).renameTo(getFolder(store, newName));
        } catch (MessagingException ex) {
            throw new IllegalStateException("重命名文件夹失败", ex);
        }
    }

    /**
     * 删除文件夹。
     *
     * @param store      邮件存储连接
     * @param folderName 文件夹名称
     * @return 删除成功返回 true，否则返回 false
     */
    public static boolean deleteFolder(Store store, String folderName) {
        try {
            return getFolder(store, folderName).delete(true);
        } catch (MessagingException ex) {
            throw new IllegalStateException("删除文件夹失败", ex);
        }
    }

    /**
     * 判断文件夹是否存在。
     *
     * @param store      邮件存储连接
     * @param folderName 文件夹名称
     * @return 存在返回 true，否则返回 false
     */
    public static boolean existsFolder(Store store, String folderName) {
        try {
            return getFolder(store, folderName).exists();
        } catch (MessagingException ex) {
            throw new IllegalStateException("判断文件夹是否存在失败", ex);
        }
    }

    /**
     * 打开文件夹。
     *
     * @param folder 文件夹
     * @param mode   打开模式
     * @return 已打开的文件夹
     */
    public static Folder openFolder(Folder folder, int mode) {
        requireFolder(folder);
        try {
            if (!folder.isOpen()) {
                folder.open(mode);
            }
            return folder;
        } catch (MessagingException ex) {
            throw new IllegalStateException("打开文件夹失败", ex);
        }
    }

    /**
     * 关闭文件夹。
     *
     * @param folder  文件夹
     * @param expunge 是否同步删除已标记删除的邮件
     */
    public static void closeFolder(Folder folder, boolean expunge) {
        closeQuietly(folder, expunge);
    }

    /**
     * 统计文件夹邮件数量。
     *
     * @param store      邮件存储连接
     * @param folderName 文件夹名称
     * @return 邮件数量
     */
    public static int countFolderMessages(Store store, String folderName) {
        Folder folder = null;
        try {
            folder = openFolder(getFolder(store, folderName), Folder.READ_ONLY);
            return countMessages(folder);
        } finally {
            closeQuietly(folder, false);
        }
    }

    /**
     * 统计文件夹未读邮件数量。
     *
     * @param store      邮件存储连接
     * @param folderName 文件夹名称
     * @return 未读数量
     */
    public static int countFolderUnreadMessages(Store store, String folderName) {
        Folder folder = null;
        try {
            folder = openFolder(getFolder(store, folderName), Folder.READ_ONLY);
            return countUnreadMessages(folder);
        } finally {
            closeQuietly(folder, false);
        }
    }

    /**
     * 解析邮件。
     *
     * @param message 邮件
     * @return 邮件读取结果
     */
    public static MailReadResult parseMessage(Message message) {
        requireNonNull(message, "邮件不能为空");
        try {
            MailReadResult result = new MailReadResult();
            result.messageId = parseMessageId(message);
            result.subject = parseSubject(message);
            result.from = parseSender(message);
            result.to = parseRecipients(message, Message.RecipientType.TO);
            result.cc = parseRecipients(message, Message.RecipientType.CC);
            result.bcc = parseRecipients(message, Message.RecipientType.BCC);
            result.sentDate = parseSentDate(message);
            result.receivedDate = parseReceivedDate(message);
            result.textContent = getTextContent(message);
            result.htmlContent = getHtmlContent(message);
            result.headers = parseHeaders(message);
            result.attachments = parseAttachments(message);
            result.inlineResources = parseInlineResources(message);
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("解析邮件失败", ex);
        }
    }

    /**
     * 解析原始邮件内容。
     *
     * @param session 邮件会话
     * @param input   原始邮件输入流
     * @return MIME 邮件
     */
    public static MimeMessage parseRawMessage(Session session, InputStream input) {
        requireNonNull(session, "邮件会话不能为空");
        requireNonNull(input, "原始邮件输入流不能为空");
        try {
            return new MimeMessage(session, input);
        } catch (MessagingException ex) {
            throw new IllegalArgumentException("解析原始邮件失败", ex);
        }
    }

    /**
     * 解析邮件头。
     *
     * @param message 邮件
     * @return 邮件头映射
     */
    public static Map<String, List<String>> parseHeaders(Message message) {
        requireNonNull(message, "邮件不能为空");
        try {
            Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Enumeration<?> headers = message.getAllHeaders();
            while (headers.hasMoreElements()) {
                jakarta.mail.Header header = (jakarta.mail.Header) headers.nextElement();
                result.computeIfAbsent(header.getName(), key -> new ArrayList<>()).add(header.getValue());
            }
            return result;
        } catch (MessagingException ex) {
            throw new IllegalStateException("解析邮件头失败", ex);
        }
    }

    /**
     * 解析发件人。
     *
     * @param message 邮件
     * @return 发件人字符串
     */
    public static String parseSender(Message message) {
        requireNonNull(message, "邮件不能为空");
        try {
            Address[] from = message.getFrom();
            return from == null || from.length == 0 ? null : from[0].toString();
        } catch (MessagingException ex) {
            throw new IllegalStateException("解析发件人失败", ex);
        }
    }

    /**
     * 解析指定类型收件人。
     *
     * @param message 邮件
     * @param type    收件人类型
     * @return 收件人列表
     */
    public static List<String> parseRecipients(Message message, Message.RecipientType type) {
        requireNonNull(message, "邮件不能为空");
        requireNonNull(type, "收件人类型不能为空");
        try {
            Address[] addresses = message.getRecipients(type);
            if (addresses == null || addresses.length == 0) {
                return List.of();
            }
            return Arrays.stream(addresses).map(Address::toString).toList();
        } catch (MessagingException ex) {
            throw new IllegalStateException("解析收件人失败", ex);
        }
    }

    /**
     * 解析邮件主题。
     *
     * @param message 邮件
     * @return 邮件主题
     */
    public static String parseSubject(Message message) {
        requireNonNull(message, "邮件不能为空");
        try {
            return decodeSubject(message.getSubject());
        } catch (MessagingException ex) {
            throw new IllegalStateException("解析主题失败", ex);
        }
    }

    /**
     * 解析邮件发送时间。
     *
     * @param message 邮件
     * @return 发送时间
     */
    public static Date parseSentDate(Message message) {
        requireNonNull(message, "邮件不能为空");
        try {
            return message.getSentDate();
        } catch (MessagingException ex) {
            throw new IllegalStateException("解析发送时间失败", ex);
        }
    }

    /**
     * 解析邮件接收时间。
     *
     * @param message 邮件
     * @return 接收时间
     */
    public static Date parseReceivedDate(Message message) {
        requireNonNull(message, "邮件不能为空");
        try {
            return message.getReceivedDate();
        } catch (MessagingException ex) {
            throw new IllegalStateException("解析接收时间失败", ex);
        }
    }

    /**
     * 解析邮件 Message-ID。
     *
     * @param message 邮件
     * @return Message-ID
     */
    public static String parseMessageId(Message message) {
        return getMessageHeader(message, "Message-ID");
    }

    /**
     * 解析邮件正文。
     *
     * @param message 邮件
     * @return 邮件正文
     */
    public static String parseBody(Message message) {
        return getMessageBody(message);
    }

    /**
     * 解析邮件附件。
     *
     * @param part 邮件部分
     * @return 附件列表
     */
    public static List<MailAttachment> parseAttachments(Part part) {
        return readAttachments(part);
    }

    /**
     * 解析邮件内嵌资源。
     *
     * @param part 邮件部分
     * @return 内嵌资源列表
     */
    public static List<MailAttachment> parseInlineResources(Part part) {
        List<MailAttachment> result = new ArrayList<>();
        collectAttachments(part, true, result);
        return result;
    }

    /**
     * 校验邮件消息完整性。
     *
     * @param message 邮件消息
     */
    public static void validateMessage(MailMessage message) {
        requireNonNull(message, "邮件消息不能为空");
        validateSender(message.from);
        validateRecipients(message);
        validateSubject(message.subject);
        validateContent(message.content);
        validateAttachmentLimit(message.attachments, 100);
        validateTotalSize(message, DEFAULT_MAX_ATTACHMENT_SIZE * 2);
    }

    /**
     * 校验邮件主题。
     *
     * @param subject 邮件主题
     */
    public static void validateSubject(String subject) {
        if (subject == null) {
            throw new IllegalArgumentException("邮件主题不能为 null");
        }
        if (subject.length() > 998) {
            throw new IllegalArgumentException("邮件主题长度不能超过 998 个字符");
        }
    }

    /**
     * 校验邮件正文。
     *
     * @param content 邮件正文
     */
    public static void validateContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("邮件正文不能为 null");
        }
    }

    /**
     * 校验发件人。
     *
     * @param sender 发件人地址
     */
    public static void validateSender(String sender) {
        if (!isValidAddress(sender)) {
            throw new IllegalArgumentException("发件人地址非法");
        }
    }

    /**
     * 校验收件人数量限制。
     *
     * @param recipients 收件人集合
     * @param maxCount   最大数量
     */
    public static void validateRecipientLimit(Collection<String> recipients, int maxCount) {
        if (maxCount < 0) {
            throw new IllegalArgumentException("最大收件人数量不能为负数");
        }
        int size = recipients == null ? 0 : recipients.size();
        if (size > maxCount) {
            throw new IllegalArgumentException("收件人数量超过限制: " + size);
        }
    }

    /**
     * 校验附件数量限制。
     *
     * @param attachments 附件集合
     * @param maxCount    最大数量
     */
    public static void validateAttachmentLimit(Collection<MailAttachment> attachments, int maxCount) {
        if (maxCount < 0) {
            throw new IllegalArgumentException("最大附件数量不能为负数");
        }
        int size = attachments == null ? 0 : attachments.size();
        if (size > maxCount) {
            throw new IllegalArgumentException("附件数量超过限制: " + size);
        }
    }

    /**
     * 校验邮件总大小。
     *
     * @param message  邮件消息
     * @param maxBytes 最大字节数
     */
    public static void validateTotalSize(MailMessage message, long maxBytes) {
        requireNonNull(message, "邮件消息不能为空");
        if (maxBytes < 0) {
            throw new IllegalArgumentException("最大邮件大小不能为负数");
        }
        long size = nullToEmpty(message.content).getBytes(message.charset == null ? DEFAULT_CHARSET : message.charset).length;
        for (MailAttachment attachment : safeList(message.attachments)) {
            size += attachment.content != null ? attachment.content.length : fileSize(attachment.path);
        }
        if (size > maxBytes) {
            throw new IllegalArgumentException("邮件总大小超过限制: " + size + " bytes");
        }
    }

    /**
     * 判断邮箱域名是否在允许列表中。
     *
     * @param address        邮箱地址
     * @param allowedDomains 允许域名集合
     * @return 允许返回 true，否则返回 false
     */
    public static boolean isAllowedDomain(String address, Collection<String> allowedDomains) {
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return true;
        }
        String domain = extractDomain(address);
        for (String allowedDomain : allowedDomains) {
            if (domain.equalsIgnoreCase(nullToEmpty(allowedDomain).trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断邮箱域名是否在阻止列表中。
     *
     * @param address        邮箱地址
     * @param blockedDomains 阻止域名集合
     * @return 阻止返回 true，否则返回 false
     */
    public static boolean isBlockedDomain(String address, Collection<String> blockedDomains) {
        if (blockedDomains == null || blockedDomains.isEmpty()) {
            return false;
        }
        String domain = extractDomain(address);
        for (String blockedDomain : blockedDomains) {
            if (domain.equalsIgnoreCase(nullToEmpty(blockedDomain).trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断附件类型是否允许。
     *
     * @param attachment          附件
     * @param allowedContentTypes 允许内容类型集合
     * @return 允许返回 true，否则返回 false
     */
    public static boolean isAllowedAttachmentType(MailAttachment attachment, Collection<String> allowedContentTypes) {
        requireNonNull(attachment, "附件不能为空");
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            return true;
        }
        String contentType = hasText(attachment.contentType) ? attachment.contentType : DEFAULT_CONTENT_TYPE;
        for (String allowed : allowedContentTypes) {
            if (contentType.equalsIgnoreCase(nullToEmpty(allowed).trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本是否包含敏感词。
     *
     * @param content        文本内容
     * @param sensitiveWords 敏感词集合
     * @return 包含返回 true，否则返回 false
     */
    public static boolean containsSensitiveWords(String content, Collection<String> sensitiveWords) {
        if (!hasText(content) || sensitiveWords == null || sensitiveWords.isEmpty()) {
            return false;
        }
        String lowerContent = content.toLowerCase(Locale.ROOT);
        for (String word : sensitiveWords) {
            if (hasText(word) && lowerContent.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 邮箱地址脱敏。
     *
     * @param address 邮箱地址
     * @return 脱敏后的邮箱地址
     */
    public static String maskAddress(String address) {
        if (!isValidAddress(address)) {
            return "";
        }
        String normalized = normalizeAddress(address);
        int at = normalized.indexOf('@');
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    /**
     * 邮件内容脱敏。
     *
     * @param content 邮件内容
     * @return 脱敏后的内容
     */
    public static String maskContent(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "***@***")
                .replaceAll("(?<!\\d)\\d{11}(?!\\d)", "***********");
    }

    /**
     * 将集合按指定大小拆分为批次。
     *
     * @param items     原始集合
     * @param batchSize 批次大小
     * @param <T>       元素类型
     * @return 批次列表
     */
    public static <T> List<List<T>> splitBatch(Collection<T> items, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("批次大小必须大于 0");
        }
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<T> list = new ArrayList<>(items);
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            result.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return result;
    }

    /**
     * 按批次发送邮件。
     *
     * @param config    邮件配置
     * @param messages  邮件集合
     * @param batchSize 批次大小
     * @return 批量发送结果
     */
    public static MailBatchSendResult sendBatchByGroup(MailConfig config, Collection<MailMessage> messages, int batchSize) {
        List<MailSendResult> results = new ArrayList<>();
        for (List<MailMessage> batch : splitBatch(messages, batchSize)) {
            results.addAll(sendBatch(config, batch));
        }
        return buildBatchResult(results);
    }

    /**
     * 按限速批量发送邮件。
     *
     * @param config          邮件配置
     * @param messages        邮件集合
     * @param batchSize       批次大小
     * @param intervalBetween 批次间隔
     * @return 批量发送结果
     */
    public static MailBatchSendResult sendBatchWithLimit(MailConfig config, Collection<MailMessage> messages, int batchSize, Duration intervalBetween) {
        List<MailSendResult> results = new ArrayList<>();
        List<List<MailMessage>> batches = splitBatch(messages, batchSize);
        for (int i = 0; i < batches.size(); i++) {
            results.addAll(sendBatch(config, batches.get(i)));
            if (i < batches.size() - 1) {
                sleepQuietly(intervalBetween == null ? Duration.ZERO : intervalBetween);
            }
        }
        return buildBatchResult(results);
    }

    /**
     * 带重试机制批量发送邮件。
     *
     * @param config     邮件配置
     * @param messages   邮件集合
     * @param retryTimes 重试次数
     * @param interval   重试间隔
     * @return 批量发送结果
     */
    public static MailBatchSendResult sendBatchWithRetry(MailConfig config, Collection<MailMessage> messages, int retryTimes, Duration interval) {
        if (messages == null || messages.isEmpty()) {
            return buildBatchResult(List.of());
        }
        List<MailSendResult> results = new ArrayList<>();
        for (MailMessage message : messages) {
            results.add(sendWithRetry(config, message, retryTimes, interval));
        }
        return buildBatchResult(results);
    }

    /**
     * 收集发送结果并构建批量结果。
     *
     * @param results 发送结果集合
     * @return 批量发送结果
     */
    public static MailBatchSendResult collectSendResults(Collection<MailSendResult> results) {
        return buildBatchResult(results);
    }

    /**
     * 过滤失败发送结果。
     *
     * @param results 发送结果集合
     * @return 失败结果列表
     */
    public static List<MailSendResult> filterFailedResults(Collection<MailSendResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream().filter(result -> result == null || !result.success).toList();
    }

    /**
     * 过滤成功发送结果。
     *
     * @param results 发送结果集合
     * @return 成功结果列表
     */
    public static List<MailSendResult> filterSuccessResults(Collection<MailSendResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream().filter(result -> result != null && result.success).toList();
    }

    /**
     * 重试失败邮件。
     *
     * @param config     邮件配置
     * @param messages   待重试邮件集合
     * @param retryTimes 重试次数
     * @param interval   重试间隔
     * @return 批量发送结果
     */
    public static MailBatchSendResult retryFailedMessages(MailConfig config, Collection<MailMessage> messages, int retryTimes, Duration interval) {
        return sendBatchWithRetry(config, messages, retryTimes, interval);
    }

    /**
     * 计算批次数量。
     *
     * @param total     总数量
     * @param batchSize 批次大小
     * @return 批次数量
     */
    public static int calculateBatchCount(int total, int batchSize) {
        if (total < 0) {
            throw new IllegalArgumentException("总数量不能为负数");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("批次大小必须大于 0");
        }
        return (total + batchSize - 1) / batchSize;
    }

    /**
     * 构建批量发送结果。
     *
     * @param results 发送结果集合
     * @return 批量发送结果
     */
    public static MailBatchSendResult buildBatchResult(Collection<MailSendResult> results) {
        MailBatchSendResult batchResult = new MailBatchSendResult();
        if (results == null || results.isEmpty()) {
            return batchResult;
        }
        batchResult.results = new ArrayList<>(results);
        batchResult.total = batchResult.results.size();
        batchResult.successCount = filterSuccessResults(batchResult.results).size();
        batchResult.failureCount = batchResult.total - batchResult.successCount;
        return batchResult;
    }

    /**
     * 为邮件消息请求已读回执。
     *
     * @param message        邮件消息
     * @param receiptAddress 回执接收地址
     * @return 邮件消息
     */
    public static MailMessage requestReadReceipt(MailMessage message, String receiptAddress) {
        requireNonNull(message, "邮件消息不能为空");
        requireText(receiptAddress, "回执地址不能为空");
        message.headers.put("Disposition-Notification-To", parseAddress(receiptAddress).toString());
        return message;
    }

    /**
     * 为邮件消息请求送达回执。
     *
     * @param message        邮件消息
     * @param receiptAddress 回执接收地址
     * @return 邮件消息
     */
    public static MailMessage requestDeliveryReceipt(MailMessage message, String receiptAddress) {
        requireNonNull(message, "邮件消息不能为空");
        requireText(receiptAddress, "回执地址不能为空");
        message.headers.put("Return-Receipt-To", parseAddress(receiptAddress).toString());
        message.headers.put("Delivery-Notification-To", parseAddress(receiptAddress).toString());
        return message;
    }

    /**
     * 判断邮件是否为已读回执。
     *
     * @param message 邮件
     * @return 是已读回执返回 true，否则返回 false
     */
    public static boolean isReadReceipt(Message message) {
        String contentType = safeContentType(message);
        return contentType.toLowerCase(Locale.ROOT).contains("disposition-notification")
                || Optional.ofNullable(getMessageHeader(message, "Disposition")).orElse("").toLowerCase(Locale.ROOT).contains("notification");
    }

    /**
     * 判断邮件是否为送达回执。
     *
     * @param message 邮件
     * @return 是送达回执返回 true，否则返回 false
     */
    public static boolean isDeliveryReceipt(Message message) {
        String contentType = safeContentType(message);
        return contentType.toLowerCase(Locale.ROOT).contains("delivery-status")
                || Optional.ofNullable(getMessageHeader(message, "Delivery-Status")).orElse("").length() > 0;
    }

    /**
     * 判断邮件是否为退信。
     *
     * @param message 邮件
     * @return 是退信返回 true，否则返回 false
     */
    public static boolean isBounceMail(Message message) {
        String from = nullToEmpty(parseSender(message)).toLowerCase(Locale.ROOT);
        String subject = nullToEmpty(parseSubject(message)).toLowerCase(Locale.ROOT);
        return from.contains("mailer-daemon") || from.contains("postmaster")
                || subject.contains("undeliver") || subject.contains("failure") || subject.contains("退信");
    }

    /**
     * 解析退信原因。
     *
     * @param message 退信邮件
     * @return 退信原因
     */
    public static String parseBounceReason(Message message) {
        String body = getMessageBody(message);
        if (!hasText(body)) {
            return "未知退信原因";
        }
        for (String line : body.split("\\R")) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("reason") || lower.contains("diagnostic") || lower.contains("error")) {
                return line.trim();
            }
        }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    /**
     * 解析退信地址。
     *
     * @param message 退信邮件
     * @return 退信地址列表
     */
    public static List<String> parseBounceAddress(Message message) {
        String body = getMessageBody(message);
        if (!hasText(body)) {
            return List.of();
        }
        Matcher matcher = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE).matcher(body);
        Set<String> result = new LinkedHashSet<>();
        while (matcher.find()) {
            result.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(result);
    }

    /**
     * 解析投递状态。
     *
     * @param message 邮件
     * @return 投递状态描述
     */
    public static String parseDeliveryStatus(Message message) {
        String header = getMessageHeader(message, "Status");
        if (hasText(header)) {
            return header;
        }
        return isBounceMail(message) ? "failed" : "unknown";
    }

    /**
     * 构建回执请求头。
     *
     * @param receiptAddress 回执地址
     * @return 回执请求头
     */
    public static Map<String, String> buildReceiptHeaders(String receiptAddress) {
        String address = parseAddress(receiptAddress).toString();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Disposition-Notification-To", address);
        headers.put("Return-Receipt-To", address);
        headers.put("Delivery-Notification-To", address);
        return headers;
    }

    /**
     * 处理退信邮件并返回摘要信息。
     *
     * @param message 退信邮件
     * @return 退信摘要
     */
    public static Map<String, Object> handleBounceMail(Message message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bounce", isBounceMail(message));
        result.put("reason", parseBounceReason(message));
        result.put("addresses", parseBounceAddress(message));
        result.put("status", parseDeliveryStatus(message));
        return result;
    }

    /**
     * 构建发送结果。
     *
     * @param success   是否成功
     * @param messageId 消息 ID
     * @param exception 异常
     * @param costMillis 耗时毫秒数
     * @return 发送结果
     */
    public static MailSendResult buildSendResult(boolean success, String messageId, Throwable exception, long costMillis) {
        MailSendResult result = new MailSendResult();
        result.success = success;
        result.messageId = messageId;
        result.exception = exception;
        result.costMillis = Math.max(0, costMillis);
        if (exception != null) {
            result.errorMessage = getErrorMessage(exception);
            result.rootCauseMessage = getRootCauseMessage(exception);
        }
        return result;
    }

    /**
     * 构建成功发送结果。
     *
     * @param messageId  消息 ID
     * @param costMillis 耗时毫秒数
     * @return 成功结果
     */
    public static MailSendResult buildSuccessResult(String messageId, long costMillis) {
        return buildSendResult(true, messageId, null, costMillis);
    }

    /**
     * 构建失败发送结果。
     *
     * @param exception  异常
     * @param costMillis 耗时毫秒数
     * @return 失败结果
     */
    public static MailSendResult buildFailureResult(Throwable exception, long costMillis) {
        return buildSendResult(false, null, exception, costMillis);
    }

    /**
     * 构建批量发送结果。
     *
     * @param results 发送结果集合
     * @return 批量发送结果
     */
    public static MailBatchSendResult buildBatchSendResult(Collection<MailSendResult> results) {
        return buildBatchResult(results);
    }

    /**
     * 获取异常信息。
     *
     * @param throwable 异常
     * @return 异常信息
     */
    public static String getErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        return hasText(throwable.getMessage()) ? throwable.getMessage() : throwable.getClass().getSimpleName();
    }

    /**
     * 获取根因异常信息。
     *
     * @param throwable 异常
     * @return 根因异常信息
     */
    public static String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return getErrorMessage(root);
    }

    /**
     * 格式化邮件发送日志。
     *
     * @param result 发送结果
     * @return 日志字符串
     */
    public static String formatSendLog(MailSendResult result) {
        requireNonNull(result, "发送结果不能为空");
        return "邮件发送" + (result.success ? "成功" : "失败")
                + ", messageId=" + nullToEmpty(result.messageId)
                + ", costMillis=" + result.costMillis
                + (result.success ? "" : ", error=" + nullToEmpty(result.errorMessage));
    }

    /**
     * 格式化邮件接收日志。
     *
     * @param result 邮件读取结果
     * @return 日志字符串
     */
    public static String formatReceiveLog(MailReadResult result) {
        requireNonNull(result, "邮件读取结果不能为空");
        return "邮件接收, messageId=" + nullToEmpty(result.messageId)
                + ", from=" + nullToEmpty(result.from)
                + ", subject=" + nullToEmpty(result.subject);
    }

    /**
     * 计算从开始时间到当前的耗时。
     *
     * @param start 开始时间
     * @return 耗时毫秒数
     */
    public static long calculateCostTime(Instant start) {
        requireNonNull(start, "开始时间不能为空");
        return Math.max(0, Duration.between(start, Instant.now()).toMillis());
    }

    /**
     * 判断发送结果是否成功。
     *
     * @param result 发送结果
     * @return 成功返回 true，否则返回 false
     */
    public static boolean isSendSuccess(MailSendResult result) {
        return result != null && result.success;
    }

    /**
     * 快速发送简单文本邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param content 正文
     * @return 发送结果
     */
    public static MailSendResult sendSimpleText(MailConfig config, String from, String to, String subject, String content) {
        return sendText(config, from, List.of(to), subject, content);
    }

    /**
     * 快速发送简单 HTML 邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param html    HTML 正文
     * @return 发送结果
     */
    public static MailSendResult sendSimpleHtml(MailConfig config, String from, String to, String subject, String html) {
        return sendHtml(config, from, List.of(to), subject, html);
    }

    /**
     * 快速发送单附件邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param content 正文
     * @param path    附件路径
     * @return 发送结果
     */
    public static MailSendResult sendSimpleAttachment(MailConfig config, String from, String to, String subject, String content, Path path) {
        MailMessage message = baseMessage(from, List.of(to), subject, content, false);
        addFileAttachment(message, path);
        return send(config, message);
    }

    /**
     * 快速发送模板邮件。
     *
     * @param config   邮件配置
     * @param from     发件人
     * @param to       收件人
     * @param template 模板
     * @return 发送结果
     */
    public static MailSendResult sendSimpleTemplate(MailConfig config, String from, String to, MailTemplate template) {
        return sendTemplate(config, from, List.of(to), template);
    }

    /**
     * 发送验证码邮件。
     *
     * @param config 邮件配置
     * @param from   发件人
     * @param to     收件人
     * @param code   验证码
     * @return 发送结果
     */
    public static MailSendResult sendVerificationCode(MailConfig config, String from, String to, String code) {
        requireText(code, "验证码不能为空");
        return sendSimpleText(config, from, to, "验证码", "您的验证码是: " + code);
    }

    /**
     * 发送通知邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param content 通知内容
     * @return 发送结果
     */
    public static MailSendResult sendNotice(MailConfig config, String from, String to, String subject, String content) {
        return sendSimpleText(config, from, to, subject, content);
    }

    /**
     * 发送告警邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param content 告警内容
     * @return 发送结果
     */
    public static MailSendResult sendAlert(MailConfig config, String from, String to, String subject, String content) {
        return sendSimpleText(config, from, to, "[告警] " + nullToEmpty(subject), content);
    }

    /**
     * 发送报表邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param content 正文
     * @param report  报表文件路径
     * @return 发送结果
     */
    public static MailSendResult sendReport(MailConfig config, String from, String to, String subject, String content, Path report) {
        return sendSimpleAttachment(config, from, to, subject, content, report);
    }

    /**
     * 发送文件邮件。
     *
     * @param config  邮件配置
     * @param from    发件人
     * @param to      收件人
     * @param subject 主题
     * @param file    文件路径
     * @return 发送结果
     */
    public static MailSendResult sendFile(MailConfig config, String from, String to, String subject, Path file) {
        return sendSimpleAttachment(config, from, to, subject, "请查收附件。", file);
    }

    /**
     * 快速读取未读邮件。
     *
     * @param folder 邮件文件夹
     * @return 未读邮件读取结果列表
     */
    public static List<MailReadResult> readUnread(Folder folder) {
        return readMessages(listUnreadMessages(folder));
    }

    private static MailConfig createConfig(String protocol, String host, int port, String username, String password, boolean ssl, boolean startTls) {
        MailConfig config = new MailConfig();
        config.protocol = protocol;
        config.host = host;
        config.port = port;
        config.username = username;
        config.password = password;
        config.ssl = ssl;
        config.startTls = startTls;
        return config;
    }

    private static boolean testStoreConnection(MailConfig config) {
        try {
            Session session = buildSession(config);
            Store store = null;
            try {
                store = session.getStore(normalizeProtocol(config.protocol));
                store.connect(config.host, config.port, config.username, config.password);
                return true;
            } finally {
                closeQuietly(store);
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private static String normalizeProtocol(String protocol) {
        return requireText(protocol, "邮件协议不能为空").trim().toLowerCase(Locale.ROOT);
    }

    private static MailMessage baseMessage(String from, Collection<String> to, String subject, String content, boolean html) {
        MailMessage message = new MailMessage();
        message.from = from;
        message.to.addAll(to == null ? List.of() : to);
        message.subject = subject;
        message.content = content;
        message.html = html;
        return message;
    }

    private static Address[] toAddressArray(Collection<String> addresses) {
        return parseAddressList(addresses).toArray(Address[]::new);
    }

    private static MimeMultipart buildMultipartContent(String content, boolean html, Charset charset, Collection<MailAttachment> attachments, Collection<MailAttachment> inlineResources) {
        try {
            MimeMultipart mixed = new MimeMultipart("mixed");
            MimeBodyPart contentPart = new MimeBodyPart();
            if (inlineResources != null && !inlineResources.isEmpty()) {
                MimeMultipart related = new MimeMultipart("related");
                MimeBodyPart body = new MimeBodyPart();
                body.setContent(nullToEmpty(content), (html ? "text/html" : "text/plain") + "; charset=" + charsetName(charset));
                related.addBodyPart(body);
                for (MailAttachment inline : inlineResources) {
                    inline.inline = true;
                    MimeBodyPart inlinePart = toBodyPart(inline);
                    related.addBodyPart(inlinePart);
                }
                contentPart.setContent(related);
            } else {
                contentPart.setContent(nullToEmpty(content), (html ? "text/html" : "text/plain") + "; charset=" + charsetName(charset));
            }
            mixed.addBodyPart(contentPart);
            if (attachments != null) {
                for (MailAttachment attachment : attachments) {
                    mixed.addBodyPart(toBodyPart(attachment));
                }
            }
            return mixed;
        } catch (MessagingException ex) {
            throw new IllegalStateException("构建多部分邮件内容失败", ex);
        }
    }

    private static MimeBodyPart toBodyPart(MailAttachment attachment) {
        validateAttachment(attachment);
        try {
            MimeBodyPart bodyPart = new MimeBodyPart();
            String fileName = getAttachmentName(attachment);
            if (attachment.path != null) {
                FileDataSource dataSource = new FileDataSource(attachment.path.toFile());
                bodyPart.setDataHandler(new DataHandler(dataSource));
            } else {
                ByteArrayDataSource dataSource = new ByteArrayDataSource(attachment.content, hasText(attachment.contentType) ? attachment.contentType : DEFAULT_CONTENT_TYPE);
                bodyPart.setDataHandler(new DataHandler(dataSource));
            }
            bodyPart.setFileName(MimeUtility.encodeText(fileName, DEFAULT_CHARSET.name(), null));
            if (attachment.inline) {
                bodyPart.setDisposition(Part.INLINE);
                if (hasText(attachment.contentId)) {
                    bodyPart.setHeader("Content-ID", "<" + attachment.contentId + ">");
                }
            } else {
                bodyPart.setDisposition(Part.ATTACHMENT);
            }
            return bodyPart;
        } catch (Exception ex) {
            throw new IllegalStateException("构建附件部分失败", ex);
        }
    }

    private static String getContentByMimeType(Part part, String mimeType) {
        if (part == null) {
            return "";
        }
        try {
            if (part.isMimeType(mimeType)) {
                Object content = part.getContent();
                return content == null ? "" : String.valueOf(content);
            }
            if (part.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) part.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    String content = getContentByMimeType(multipart.getBodyPart(i), mimeType);
                    if (hasText(content)) {
                        return content;
                    }
                }
            }
            return "";
        } catch (Exception ex) {
            throw new IllegalStateException("读取邮件内容失败", ex);
        }
    }

    private static void collectAttachments(Part part, boolean inlineOnly, List<MailAttachment> result) {
        if (part == null) {
            return;
        }
        try {
            if (part.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) part.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    collectAttachments(multipart.getBodyPart(i), inlineOnly, result);
                }
                return;
            }
            String disposition = part.getDisposition();
            boolean inline = Part.INLINE.equalsIgnoreCase(disposition);
            boolean attachment = Part.ATTACHMENT.equalsIgnoreCase(disposition) || hasText(part.getFileName());
            if ((inlineOnly && inline) || (!inlineOnly && attachment && !inline)) {
                MailAttachment item = new MailAttachment();
                item.fileName = hasText(part.getFileName()) ? MimeUtility.decodeText(part.getFileName()) : "attachment";
                item.contentType = part.getContentType();
                item.inline = inline;
                item.contentId = trimContentId(firstHeader(part, "Content-ID"));
                try (InputStream inputStream = part.getInputStream()) {
                    item.content = inputStream.readAllBytes();
                }
                result.add(item);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("读取附件失败", ex);
        }
    }

    private static String firstHeader(Part part, String name) throws MessagingException {
        String[] headers = part.getHeader(name);
        return headers == null || headers.length == 0 ? null : headers[0];
    }

    private static String trimContentId(String contentId) {
        if (!hasText(contentId)) {
            return null;
        }
        String value = contentId.trim();
        if (value.startsWith("<") && value.endsWith(">") && value.length() > 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static SearchTerm buildSearchTerm(MailSearchCondition condition) {
        List<SearchTerm> terms = new ArrayList<>();
        if (hasText(condition.subject)) {
            terms.add(new SubjectTerm(condition.subject));
        }
        if (hasText(condition.sender)) {
            terms.add(new FromStringTerm(condition.sender));
        }
        if (hasText(condition.recipient)) {
            terms.add(new OrTerm(new RecipientStringTerm(Message.RecipientType.TO, condition.recipient),
                    new OrTerm(new RecipientStringTerm(Message.RecipientType.CC, condition.recipient), new RecipientStringTerm(Message.RecipientType.BCC, condition.recipient))));
        }
        if (condition.startDate != null) {
            terms.add(new ReceivedDateTerm(ComparisonTerm.GE, condition.startDate));
        }
        if (condition.endDate != null) {
            terms.add(new ReceivedDateTerm(ComparisonTerm.LE, condition.endDate));
        }
        if (condition.unread != null) {
            terms.add(new FlagTerm(new Flags(Flags.Flag.SEEN), !condition.unread));
        }
        if (condition.flagged != null) {
            terms.add(new FlagTerm(new Flags(Flags.Flag.FLAGGED), condition.flagged));
        }
        if (terms.isEmpty()) {
            return null;
        }
        SearchTerm term = terms.get(0);
        for (int i = 1; i < terms.size(); i++) {
            term = new AndTerm(term, terms.get(i));
        }
        return term;
    }

    private static boolean hasAttachment(Message message) {
        try {
            return !readAttachments(message).isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }

    private static void requireFolder(Folder folder) {
        requireNonNull(folder, "邮件文件夹不能为空");
    }

    private static void requireOpenFolder(Folder folder) {
        requireFolder(folder);
        if (!folder.isOpen()) {
            throw new IllegalArgumentException("邮件文件夹未打开");
        }
    }

    private static String safeContentType(Part part) {
        requireNonNull(part, "邮件部分不能为空");
        try {
            return nullToEmpty(part.getContentType());
        } catch (MessagingException ex) {
            return "";
        }
    }

    private static String extractMessageId(MimeMessage message) {
        try {
            return message.getMessageID();
        } catch (MessagingException ex) {
            return null;
        }
    }

    private static String extractDomain(String address) {
        String normalized = normalizeAddress(address);
        int at = normalized.indexOf('@');
        if (at < 0 || at == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(at + 1);
    }

    private static long fileSize(Path path) {
        if (path == null) {
            return 0L;
        }
        try {
            return Files.size(path);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取文件大小失败", ex);
        }
    }

    private static String sanitizeFileName(String fileName) {
        return nullToEmpty(fileName).replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String charsetName(Charset charset) {
        return (charset == null ? DEFAULT_CHARSET : charset).name();
    }

    private static String defaultCharset(String charset) {
        return hasText(charset) ? charset : DEFAULT_CHARSET.name();
    }

    private static <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static <K, V> Map<K, V> safeMap(Map<K, V> map) {
        return map == null ? Map.of() : map;
    }

    private static void sleepQuietly(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
