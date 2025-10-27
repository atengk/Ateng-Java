package local.ateng.java.email.service;

import org.springframework.scheduling.annotation.Async;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 邮件服务接口
 *
 * @author 孔余
 * @since 2025-08-09
 */
public interface MailService {

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本格式）
     */
    void sendTextMail(String to, String subject, String content);

    /**
     * 发送 HTML 格式的邮件
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendHtmlMail(String to, String subject, String html);

    /**
     * 使用动态发件人发送 HTML 邮件
     *
     * @param from     发件人邮箱
     * @param password 发件人授权码/密码
     * @param to       收件人邮箱
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    void sendHtmlMail(String from, String password, String to, String subject, String html);

    /**
     * 使用动态发件人 + 动态主机配置发送 HTML 邮件
     *
     * @param host     SMTP 服务器地址（例如 smtp.qq.com）
     * @param port     SMTP 服务器端口（25 / 465 / 587）
     * @param from     发件人邮箱
     * @param password 发件人授权码/密码
     * @param to       收件人邮箱
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    void sendHtmlMail(String host, int port, String from, String password, String to, String subject, String html);

    /**
     * 发送带附件的邮件（支持多个附件）
     *
     * @param to          收件人邮箱地址
     * @param subject     邮件主题
     * @param html        邮件正文（HTML 格式）
     * @param attachments 附件集合
     *                    key   为附件文件名
     *                    value 为附件的输入流
     */
    void sendMailWithAttachments(String to, String subject, String html, Map<String, InputStream> attachments);

    /**
     * 发送带图片的 HTML 邮件
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     * @param images  图片集合
     *                key   为图片 contentId（HTML 中引用时使用 cid:contentId）
     *                value 为图片的输入流
     */
    void sendMailWithImages(String to, String subject, String html, Map<String, InputStream> images);

    /**
     * 批量发送纯文本邮件
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本格式）
     */
    void sendBatchTextMail(List<String> toList, String subject, String content);

    /**
     * 批量发送 HTML 邮件
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendBatchHtmlMail(List<String> toList, String subject, String html);

    /**
     * 使用动态发件人批量发送 HTML 邮件
     *
     * @param from     发件人邮箱
     * @param password 发件人授权码/密码
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendBatchHtmlMail(String from, String password, List<String> toList, String subject, String html);

    /**
     * 异步发送邮件（适用于耗时的发送场景）
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    @Async
    void sendHtmlMailAsync(String to, String subject, String html);

    /**
     * 批量异步发送 HTML 邮件
     *
     * <p>每封邮件都是异步发送，不阻塞调用线程。</p>
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    @Async
    void sendBatchHtmlMailAsync(List<String> toList, String subject, String html);

    /**
     * 发送 HTML 邮件，支持抄送（Cc）和密送（Bcc），可为空
     *
     * @param to      收件人邮箱地址
     * @param ccList  抄送邮箱地址列表，可为空
     * @param bccList 密送邮箱地址列表，可为空
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendHtmlMailWithCcBcc(String to, List<String> ccList, List<String> bccList, String subject, String html);

    /**
     * 异步发送 HTML 邮件，支持抄送（Cc）和密送（Bcc）
     *
     * @param to      收件人邮箱地址
     * @param ccList  抄送邮箱地址列表，可为空
     * @param bccList 密送邮箱地址列表，可为空
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    @Async
    void sendHtmlMailWithCcBccAsync(String to, List<String> ccList, List<String> bccList, String subject, String html);

    /**
     * 发送全功能邮件（HTML + 可选抄送/密送 + 附件 + 内嵌图片）
     *
     * @param to          收件人邮箱地址
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文（HTML 格式）
     * @param attachments 附件集合，key=附件名，value=InputStream，可为空
     * @param images      图片集合，key=contentId，value=InputStream，可为空
     */
    void sendMail(String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

    /**
     * 发送全功能邮件（HTML + 可选抄送/密送 + 附件 + 内嵌图片）异步
     *
     * @param to          收件人邮箱地址
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文（HTML 格式）
     * @param attachments 附件集合，key=附件名，value=InputStream，可为空
     * @param images      图片集合，key=contentId，value=InputStream，可为空
     */
    @Async
    void sendMailAsync(String to,
                       List<String> ccList,
                       List<String> bccList,
                       String subject,
                       String html,
                       Map<String, InputStream> attachments,
                       Map<String, InputStream> images);

    /**
     * 发送全功能邮件（支持自定义发件账号，HTML + 可选抄送/密送 + 附件 + 内嵌图片）
     *
     * <p>该方法允许调用时指定发件人邮箱及密码（授权码），
     * 其他服务器参数（host、port、协议等）依然从配置文件中读取。</p>
     *
     * @param from        发件人邮箱地址
     * @param password    发件人邮箱密码或授权码
     * @param to          收件人邮箱地址，不能为空
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文内容（HTML 格式）
     * @param attachments 附件集合，key = 附件名，value = InputStream，可为空
     * @param images      内嵌图片集合，key = contentId，value = InputStream，可为空，
     *                    contentId 对应 HTML 内容中的 "cid:xxx" 引用
     */
    void sendMail(String from,
                  String password,
                  String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

    /**
     * 发送全功能邮件（支持自定义发件账号，HTML + 可选抄送/密送 + 附件 + 内嵌图片） 异步
     *
     * <p>该方法允许调用时指定发件人邮箱及密码（授权码），
     * 其他服务器参数（host、port、协议等）依然从配置文件中读取。</p>
     *
     * @param from        发件人邮箱地址
     * @param password    发件人邮箱密码或授权码
     * @param to          收件人邮箱地址，不能为空
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文内容（HTML 格式）
     * @param attachments 附件集合，key = 附件名，value = InputStream，可为空
     * @param images      内嵌图片集合，key = contentId，value = InputStream，可为空，
     *                    contentId 对应 HTML 内容中的 "cid:xxx" 引用
     */
    @Async
    void sendMailAsync(String from,
                  String password,
                  String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

    /**
     * 发送全功能邮件（支持自定义发件账号，HTML + 可选抄送/密送 + 附件 + 内嵌图片）
     *
     * <p>该方法允许在调用时指定邮件服务器主机、端口、账号及密码，
     * 适用于多租户场景或需要动态切换发件人账号的场景。</p>
     *
     * @param host        邮件服务器地址，例如 "smtp.example.com"
     * @param port        邮件服务器端口，通常为 25、465（SSL）、587（TLS）
     * @param from        发件人邮箱地址（即实际账号）
     * @param password    发件人邮箱密码或授权码
     * @param to          收件人邮箱地址，不能为空
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文内容（HTML 格式）
     * @param attachments 附件集合，key = 附件名，value = InputStream，可为空
     * @param images      内嵌图片集合，key = contentId，value = InputStream，可为空，
     *                    contentId 对应 HTML 内容中的 "cid:xxx" 引用
     */
    void sendMail(String host,
                  int port,
                  String from,
                  String password,
                  String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

    /**
     * 发送全功能邮件（支持自定义发件账号，HTML + 可选抄送/密送 + 附件 + 内嵌图片） 异步
     *
     * <p>该方法允许在调用时指定邮件服务器主机、端口、账号及密码，
     * 适用于多租户场景或需要动态切换发件人账号的场景。</p>
     *
     * @param host        邮件服务器地址，例如 "smtp.example.com"
     * @param port        邮件服务器端口，通常为 25、465（SSL）、587（TLS）
     * @param from        发件人邮箱地址（即实际账号）
     * @param password    发件人邮箱密码或授权码
     * @param to          收件人邮箱地址，不能为空
     * @param ccList      抄送邮箱列表，可为空
     * @param bccList     密送邮箱列表，可为空
     * @param subject     邮件主题
     * @param html        邮件正文内容（HTML 格式）
     * @param attachments 附件集合，key = 附件名，value = InputStream，可为空
     * @param images      内嵌图片集合，key = contentId，value = InputStream，可为空，
     *                    contentId 对应 HTML 内容中的 "cid:xxx" 引用
     */
    @Async
    void sendMailAsync(String host,
                  int port,
                  String from,
                  String password,
                  String to,
                  List<String> ccList,
                  List<String> bccList,
                  String subject,
                  String html,
                  Map<String, InputStream> attachments,
                  Map<String, InputStream> images);

}