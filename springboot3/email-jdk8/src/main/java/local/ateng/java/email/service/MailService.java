package local.ateng.java.email.service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 邮件服务接口
 * <p>
 * 该接口封装了邮件发送的常见功能，支持发送：
 * <ul>
 *     <li>纯文本邮件</li>
 *     <li>HTML 格式邮件</li>
 *     <li>带附件邮件</li>
 *     <li>带内嵌资源的邮件（例如内嵌图片）</li>
 *     <li>模板邮件（如基于 FreeMarker、Thymeleaf 等）</li>
 *     <li>群发邮件、抄送、密送</li>
 * </ul>
 *
 * @author 孔余
 * @since 2025-08-09
 */
public interface MailService {

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人邮箱地址（单个）
     * @param subject 邮件主题
     * @param content 邮件内容（纯文本）
     */
    void sendTextMail(String to, String subject, String content);

    /**
     * 发送纯文本邮件（带邮箱格式校验开关）
     *
     * @param to                  收件人邮箱
     * @param subject             邮件主题
     * @param content             邮件内容
     * @param throwOnInvalidEmail 是否在邮箱格式错误时抛出异常（true 抛出，false 跳过）
     */
    void sendTextMail(String to, String subject, String content, boolean throwOnInvalidEmail);

    /**
     * 发送纯文本邮件（支持多个收件人）
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param content 邮件内容（纯文本）
     */
    void sendTextMailList(List<String> toList, String subject, String content);

    /**
     * 发送 HTML 邮件（带邮箱格式校验）
     *
     * @param to                  收件人邮箱
     * @param subject             邮件主题
     * @param html                HTML 格式邮件正文
     * @param throwOnInvalidEmail 当邮箱格式无效时是否抛出异常
     */
    void sendHtmlMail(String to, String subject, String html, boolean throwOnInvalidEmail);

    /**
     * 发送 HTML 格式邮件
     *
     * @param to      收件人邮箱地址（单个）
     * @param subject 邮件主题
     * @param html    邮件内容（HTML 格式）
     */
    void sendHtmlMail(String to, String subject, String html);

    /**
     * 发送 HTML 格式邮件（支持多个收件人）
     *
     * @param toList  收件人邮箱地址列表
     * @param subject 邮件主题
     * @param html    邮件内容（HTML 格式）
     */
    void sendHtmlMailList(List<String> toList, String subject, String html);

    /**
     * 发送带附件的邮件
     *
     * @param to        收件人邮箱地址（单个）
     * @param subject   邮件主题
     * @param html      邮件内容（HTML 格式，可为空）
     * @param files     附件文件列表
     */
    void sendMailWithAttachments(String to, String subject, String html, List<File> files);

    /**
     * 发送带附件的邮件（支持多个收件人）
     *
     * @param toList    收件人邮箱地址列表
     * @param subject   邮件主题
     * @param html      邮件内容（HTML 格式，可为空）
     * @param files     附件文件列表
     */
    void sendMailWithAttachmentsList(List<String> toList, String subject, String html, List<File> files);

    /**
     * 发送带内嵌静态资源的 HTML 邮件
     *
     * @param to         收件人邮箱地址（单个）
     * @param subject    邮件主题
     * @param html       邮件内容（HTML 格式）
     * @param resourceId 静态资源 ID（HTML 中引用，例如 <img src='cid:logo'>）
     * @param resource   静态资源文件
     */
    void sendMailWithInlineResource(String to, String subject, String html, String resourceId, File resource);

    /**
     * 发送带内嵌静态资源的 HTML 邮件（支持多个资源）
     *
     * @param to         收件人邮箱地址（单个）
     * @param subject    邮件主题
     * @param html       邮件内容（HTML 格式）
     * @param resources  资源 Map（key 为 resourceId，value 为文件对象）
     */
    void sendMailWithInlineResources(String to, String subject, String html, Map<String, File> resources);

    /**
     * 发送模板邮件（例如基于 FreeMarker、Thymeleaf 等）
     *
     * @param to          收件人邮箱地址（单个）
     * @param subject     邮件主题
     * @param template    模板名称（如 template.html 或 template.ftl）
     * @param variables   模板变量（key 为变量名，value 为变量值）
     */
    void sendTemplateMail(String to, String subject, String template, Map<String, Object> variables);

    /**
     * 群发邮件（支持 To、Cc、Bcc）
     *
     * @param toList  收件人邮箱地址列表
     * @param ccList  抄送邮箱地址列表（可为空）
     * @param bccList 密送邮箱地址列表（可为空）
     * @param subject 邮件主题
     * @param html    邮件内容（HTML 或纯文本）
     * @param files   附件列表（可为空）
     */
    void sendMailWithCcBcc(List<String> toList,
                           List<String> ccList,
                           List<String> bccList,
                           String subject,
                           String html,
                           List<File> files);

}