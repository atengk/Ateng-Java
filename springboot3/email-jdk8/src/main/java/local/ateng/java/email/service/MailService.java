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
     * @param to       收件人邮箱地址
     * @param subject  邮件主题
     * @param content  邮件正文（纯文本格式）
     */
    void sendTextMail(String to, String subject, String content);

    /**
     * 发送 HTML 格式的邮件
     *
     * @param to       收件人邮箱地址
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    void sendHtmlMail(String to, String subject, String html);

    /**
     * 发送带附件的邮件（支持多个附件）
     *
     * @param to            收件人邮箱地址
     * @param subject       邮件主题
     * @param html          邮件正文（HTML 格式）
     * @param attachments   附件集合
     *                      key   为附件文件名
     *                      value 为附件的输入流
     */
    void sendMailWithAttachments(String to, String subject, String html, Map<String, InputStream> attachments);

    /**
     * 发送带图片的 HTML 邮件
     *
     * @param to         收件人邮箱地址
     * @param subject    邮件主题
     * @param html       邮件正文（HTML 格式）
     * @param images     图片集合
     *                   key   为图片 contentId（HTML 中引用时使用 cid:contentId）
     *                   value 为图片的输入流
     */
    void sendMailWithImages(String to, String subject, String html, Map<String, InputStream> images);

    /**
     * 批量发送纯文本邮件
     *
     * @param toList   收件人邮箱地址列表
     * @param subject  邮件主题
     * @param content  邮件正文（纯文本格式）
     */
    void sendBatchTextMail(List<String> toList, String subject, String content);

    /**
     * 批量发送 HTML 邮件
     *
     * @param toList   收件人邮箱地址列表
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    void sendBatchHtmlMail(List<String> toList, String subject, String html);

    /**
     * 异步发送邮件（适用于耗时的发送场景）
     *
     * @param to       收件人邮箱地址
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    @Async
    void sendHtmlMailAsync(String to, String subject, String html);

    /**
     * 批量异步发送 HTML 邮件
     *
     * <p>每封邮件都是异步发送，不阻塞调用线程。</p>
     *
     * @param toList   收件人邮箱地址列表
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    @Async
    void sendBatchHtmlMailAsync(List<String> toList, String subject, String html);

    /**
     * 发送 HTML 邮件，支持抄送（Cc）和密送（Bcc），可为空
     *
     * @param to       收件人邮箱地址
     * @param ccList   抄送邮箱地址列表，可为空
     * @param bccList  密送邮箱地址列表，可为空
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
     */
    void sendHtmlMailWithCcBcc(String to, List<String> ccList, List<String> bccList, String subject, String html);

    /**
     * 异步发送 HTML 邮件，支持抄送（Cc）和密送（Bcc）
     *
     * @param to       收件人邮箱地址
     * @param ccList   抄送邮箱地址列表，可为空
     * @param bccList  密送邮箱地址列表，可为空
     * @param subject  邮件主题
     * @param html     邮件正文（HTML 格式）
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

}