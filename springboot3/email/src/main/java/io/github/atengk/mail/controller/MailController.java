package io.github.atengk.mail.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mail.dto.MailBatchSendRequest;
import io.github.atengk.mail.dto.MailSendRequest;
import io.github.atengk.mail.dto.MailSendResult;
import io.github.atengk.mail.dto.MailSendSummary;
import io.github.atengk.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 邮件服务接口。
 *
 * <p>
 * 邮件发送请求不传 from.mailConfig 时，默认使用 spring.mail 配置发送；
 * 传入 from.mailConfig 时，本次请求使用动态 SMTP 配置发送。
 * </p>
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mail")
public class MailController {

    private final MailService mailService;

    /**
     * 默认邮件异步线程池。
     */
    @Qualifier("mailTaskExecutor")
    private final Executor mailTaskExecutor;

    /**
     * 高优先级邮件异步线程池，可选注入。
     */
    @Qualifier("highPriorityMailTaskExecutor")
    private final ObjectProvider<Executor> highPriorityMailTaskExecutorProvider;

    /**
     * 低优先级邮件异步线程池，可选注入。
     */
    @Qualifier("lowPriorityMailTaskExecutor")
    private final ObjectProvider<Executor> lowPriorityMailTaskExecutorProvider;

        /**
     * 发送邮件，根据 contentType 自动选择纯文本、HTML 或模板发送。
     *
     * <pre>
     * # 不传 from.mailConfig，使用 spring.mail 默认配置发送
     * curl -X POST 'http://localhost:8080/api/mail/send' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-001",
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "默认邮箱发送",
     *     "content": "这封邮件使用 spring.mail 默认配置发送",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     *
     * # 传入 from.mailConfig，本次请求使用动态 SMTP 配置发送
     * curl -X POST 'http://localhost:8080/api/mail/send' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-DYNAMIC-001",
     *     "from": {
     *       "address": "sender@example.com",
     *       "name": "动态发件人",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "defaultEncoding": "UTF-8",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "starttlsRequired": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000,
     *         "forceFromSameAsUsername": true
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "动态邮箱发送",
     *     "content": "这封邮件使用 from.mailConfig 动态 SMTP 配置发送",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send")
    public ResponseEntity<MailSendResult> send(@RequestBody MailSendRequest request) {
        log.info("接收邮件发送请求，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return ResponseEntity.ok(mailService.send(request));
    }

        /**
     * 发送纯文本邮件。
     *
     * <pre>
     * # from 为空时使用 spring.mail 默认配置
     * curl -X POST 'http://localhost:8080/api/mail/send/text' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-TEXT-001",
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "纯文本邮件",
     *     "content": "这是一封纯文本邮件",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     *
     * # 如需临时切换 SMTP 账号，可在 from.mailConfig 中传入动态配置
     * curl -X POST 'http://localhost:8080/api/mail/send/text' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-TEXT-DYNAMIC-001",
     *     "from": {
     *       "name": "文本通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "动态纯文本邮件",
     *     "content": "这是一封使用动态 SMTP 配置发送的纯文本邮件",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send/text")
    public ResponseEntity<MailSendResult> sendText(@RequestBody MailSendRequest request) {
        log.info("接收纯文本邮件发送请求，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return ResponseEntity.ok(mailService.sendText(request));
    }

        /**
     * 发送 HTML 邮件。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/html' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-HTML-001",
     *     "from": {
     *       "name": "HTML 通知"
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "HTML 邮件",
     *     "content": "<h2>你好</h2><p>这是一封 HTML 邮件</p>",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     *
     * # from.name 只覆盖发件人显示名；如需切换 SMTP 账号，传入 from.mailConfig
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send/html")
    public ResponseEntity<MailSendResult> sendHtml(@RequestBody MailSendRequest request) {
        log.info("接收 HTML 邮件发送请求，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return ResponseEntity.ok(mailService.sendHtml(request));
    }

        /**
     * 发送模板邮件。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/template' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-TEMPLATE-001",
     *     "from": {
     *       "name": "模板通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "defaultEncoding": "UTF-8",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000,
     *         "forceFromSameAsUsername": true
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "模板邮件",
     *     "templateName": "mail/welcome.html",
     *     "templateVariables": {
     *       "username": "Ateng",
     *       "loginUrl": "https://example.com/login"
     *     },
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 邮件发送结果
     */
    @PostMapping("/send/template")
    public ResponseEntity<MailSendResult> sendTemplate(@RequestBody MailSendRequest request) {
        log.info("接收模板邮件发送请求，业务编号：{}，主题：{}，模板：{}",
                request.getBizId(), request.getSubject(), request.getTemplateName());
        return ResponseEntity.ok(mailService.sendTemplate(request));
    }

        /**
     * 批量发送邮件。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/batch' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "batchBizId": "MAIL-BATCH-001",
     *     "batchSize": 50,
     *     "batchInterval": "PT1S",
     *     "continueOnError": true,
     *     "messages": [
     *       {
     *         "bizId": "MAIL-BATCH-001-1",
     *         "from": {
     *           "name": "批量通知",
     *           "mailConfig": {
     *             "host": "smtp.example.com",
     *             "port": 587,
     *             "username": "sender@example.com",
     *             "password": "请替换为邮箱授权码",
     *             "auth": true,
     *             "starttlsEnable": true,
     *             "connectionTimeout": 10000,
     *             "timeout": 10000,
     *             "writeTimeout": 10000
     *           }
     *         },
     *         "to": [
     *           {
     *             "address": "user1@example.com",
     *             "name": "用户1"
     *           }
     *         ],
     *         "subject": "批量邮件 1",
     *         "content": "这是第一封使用动态 SMTP 配置的批量邮件",
     *         "contentType": "TEXT",
     *         "priority": "NORMAL",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       },
     *       {
     *         "bizId": "MAIL-BATCH-001-2",
     *         "to": [
     *           {
     *             "address": "user2@example.com",
     *             "name": "用户2"
     *           }
     *         ],
     *         "subject": "批量邮件 2",
     *         "content": "这封邮件未传 from.mailConfig，使用 spring.mail 默认配置",
     *         "contentType": "TEXT",
     *         "priority": "NORMAL",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       }
     *     ]
     *   }'
     * </pre>
     *
     * @param request 批量邮件发送请求
     * @return 批量邮件发送汇总结果
     */
    @PostMapping("/send/batch")
    public ResponseEntity<MailSendSummary> sendBatch(@RequestBody MailBatchSendRequest request) {
        log.info("接收批量邮件发送请求，批量编号：{}，数量：{}",
                request.getBatchBizId(), request.getMessages() == null ? 0 : request.getMessages().size());
        return ResponseEntity.ok(mailService.sendBatch(request));
    }

        /**
     * 异步发送邮件，使用默认邮件线程池。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/async' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-ASYNC-001",
     *     "from": {
     *       "name": "异步通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "异步邮件",
     *     "content": "这是一封使用动态 SMTP 配置发送的异步邮件",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 异步邮件发送结果
     */
    @PostMapping("/send/async")
    public CompletableFuture<ResponseEntity<MailSendResult>> sendAsync(@RequestBody MailSendRequest request) {
        log.info("接收异步邮件发送请求，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return mailService.sendAsync(request).thenApply(ResponseEntity::ok);
    }

        /**
     * 提交异步邮件发送任务，接口立即返回。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/submit' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-SUBMIT-001",
     *     "from": {
     *       "name": "后台发送通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "异步提交邮件",
     *     "content": "接口立即返回，邮件后台发送",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 提交结果
     */
    @PostMapping("/send/submit")
    public ResponseEntity<Map<String, Object>> submitSend(@RequestBody MailSendRequest request) {
        log.info("提交异步邮件发送任务，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());

        mailService.sendAsync(request)
                .thenAccept(result -> {
                    if (Boolean.TRUE.equals(result.getSuccess())) {
                        log.info("异步邮件发送成功，业务编号：{}，消息编号：{}", result.getBizId(), result.getMessageId());
                        return;
                    }
                    log.warn("异步邮件发送失败，业务编号：{}，原因：{}", result.getBizId(), result.getErrorMessage());
                })
                .exceptionally(ex -> {
                    log.error("异步邮件发送异常，业务编号：{}，原因：{}", request.getBizId(), ex.getMessage(), ex);
                    return null;
                });

        return ResponseEntity.accepted().body(MapUtil.<String, Object>builder()
                .put("submitted", true)
                .put("bizId", request.getBizId())
                .put("message", "邮件发送任务已提交")
                .build());
    }

        /**
     * 异步发送邮件，使用指定线程池。
     *
     * <pre>
     * # executorType 支持 default、high、low；from.mailConfig 控制本次使用的 SMTP 配置
     * curl -X POST 'http://localhost:8080/api/mail/send/async/executor/default' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-ASYNC-EXECUTOR-001",
     *     "from": {
     *       "name": "指定线程池通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "指定线程池异步邮件",
     *     "content": "这是一封使用指定线程池和动态 SMTP 配置发送的异步邮件",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     *
     * curl -X POST 'http://localhost:8080/api/mail/send/async/executor/high' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-ASYNC-HIGH-001",
     *     "to": [
     *       {
     *         "address": "vip@example.com",
     *         "name": "重要用户"
     *       }
     *     ],
     *     "subject": "高优先级异步邮件",
     *     "content": "未传 from.mailConfig 时使用 spring.mail 默认配置",
     *     "contentType": "TEXT",
     *     "priority": "HIGH",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param executorType 线程池类型，支持 default、high、low
     * @param request      邮件发送请求
     * @return 异步邮件发送结果
     */
    @PostMapping("/send/async/executor/{executorType}")
    public CompletableFuture<ResponseEntity<MailSendResult>> sendAsyncWithExecutor(@PathVariable String executorType,
                                                                                   @RequestBody MailSendRequest request) {
        Executor executor = resolveExecutor(executorType);
        log.info("接收指定线程池异步邮件发送请求，线程池类型：{}，业务编号：{}，主题：{}",
                executorType, request.getBizId(), request.getSubject());
        return mailService.sendAsync(request, executor).thenApply(ResponseEntity::ok);
    }

        /**
     * 异步批量发送邮件，使用默认邮件线程池。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/batch/async' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "batchBizId": "MAIL-BATCH-ASYNC-001",
     *     "batchSize": 50,
     *     "batchInterval": "PT1S",
     *     "continueOnError": true,
     *     "messages": [
     *       {
     *         "bizId": "MAIL-BATCH-ASYNC-001-1",
     *         "from": {
     *           "name": "异步批量通知",
     *           "mailConfig": {
     *             "host": "smtp.example.com",
     *             "port": 587,
     *             "username": "sender@example.com",
     *             "password": "请替换为邮箱授权码",
     *             "auth": true,
     *             "starttlsEnable": true,
     *             "connectionTimeout": 10000,
     *             "timeout": 10000,
     *             "writeTimeout": 10000
     *           }
     *         },
     *         "to": [
     *           {
     *             "address": "user1@example.com",
     *             "name": "用户1"
     *           }
     *         ],
     *         "subject": "异步批量邮件 1",
     *         "content": "这是第一封使用动态 SMTP 配置的异步批量邮件",
     *         "contentType": "TEXT",
     *         "priority": "NORMAL",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       }
     *     ]
     *   }'
     * </pre>
     *
     * @param request 批量邮件发送请求
     * @return 异步批量邮件发送汇总结果
     */
    @PostMapping("/send/batch/async")
    public CompletableFuture<ResponseEntity<MailSendSummary>> sendBatchAsync(@RequestBody MailBatchSendRequest request) {
        log.info("接收异步批量邮件发送请求，批量编号：{}，数量：{}",
                request.getBatchBizId(), request.getMessages() == null ? 0 : request.getMessages().size());
        return mailService.sendBatchAsync(request).thenApply(ResponseEntity::ok);
    }

        /**
     * 异步批量发送邮件，使用指定线程池。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/send/batch/async/executor/default' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "batchBizId": "MAIL-BATCH-EXECUTOR-001",
     *     "batchSize": 50,
     *     "batchInterval": "PT1S",
     *     "continueOnError": true,
     *     "messages": [
     *       {
     *         "bizId": "MAIL-BATCH-EXECUTOR-001-1",
     *         "from": {
     *           "name": "指定线程池批量通知",
     *           "mailConfig": {
     *             "host": "smtp.example.com",
     *             "port": 587,
     *             "username": "sender@example.com",
     *             "password": "请替换为邮箱授权码",
     *             "auth": true,
     *             "starttlsEnable": true,
     *             "connectionTimeout": 10000,
     *             "timeout": 10000,
     *             "writeTimeout": 10000
     *           }
     *         },
     *         "to": [
     *           {
     *             "address": "user1@example.com",
     *             "name": "用户1"
     *           }
     *         ],
     *         "subject": "指定线程池批量邮件 1",
     *         "content": "这是第一封指定线程池和动态 SMTP 配置的批量邮件",
     *         "contentType": "TEXT",
     *         "priority": "NORMAL",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       }
     *     ]
     *   }'
     *
     * curl -X POST 'http://localhost:8080/api/mail/send/batch/async/executor/low' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "batchBizId": "MAIL-BATCH-LOW-001",
     *     "batchSize": 100,
     *     "batchInterval": "PT2S",
     *     "continueOnError": true,
     *     "messages": [
     *       {
     *         "bizId": "MAIL-BATCH-LOW-001-1",
     *         "to": [
     *           {
     *             "address": "user1@example.com",
     *             "name": "用户1"
     *           }
     *         ],
     *         "subject": "低优先级批量邮件",
     *         "content": "未传 from.mailConfig 时使用 spring.mail 默认配置",
     *         "contentType": "TEXT",
     *         "priority": "LOW",
     *         "charset": "UTF-8",
     *         "throwException": false
     *       }
     *     ]
     *   }'
     * </pre>
     *
     * @param executorType 线程池类型，支持 default、high、low
     * @param request      批量邮件发送请求
     * @return 异步批量邮件发送汇总结果
     */
    @PostMapping("/send/batch/async/executor/{executorType}")
    public CompletableFuture<ResponseEntity<MailSendSummary>> sendBatchAsyncWithExecutor(@PathVariable String executorType,
                                                                                         @RequestBody MailBatchSendRequest request) {
        Executor executor = resolveExecutor(executorType);
        log.info("接收指定线程池异步批量邮件发送请求，线程池类型：{}，批量编号：{}，数量：{}",
                executorType, request.getBatchBizId(), request.getMessages() == null ? 0 : request.getMessages().size());
        return mailService.sendBatchAsync(request, executor).thenApply(ResponseEntity::ok);
    }

    /**
     * 渲染邮件模板。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/template/render' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "templateName": "mail/welcome.html",
     *     "variables": {
     *       "username": "Ateng",
     *       "loginUrl": "https://example.com/login"
     *     }
     *   }'
     * </pre>
     *
     * @param request 模板渲染请求
     * @return 渲染后的模板内容
     */
    @PostMapping("/template/render")
    public ResponseEntity<String> renderTemplate(@RequestBody TemplateRenderRequest request) {
        log.info("接收邮件模板渲染请求，模板：{}", request.templateName());
        return ResponseEntity.ok(mailService.renderTemplate(request.templateName(), request.variables()));
    }

        /**
     * 校验邮件发送请求。
     *
     * <pre>
     * curl -X POST 'http://localhost:8080/api/mail/validate' \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "bizId": "MAIL-VALIDATE-001",
     *     "from": {
     *       "name": "校验通知",
     *       "mailConfig": {
     *         "host": "smtp.example.com",
     *         "port": 587,
     *         "username": "sender@example.com",
     *         "password": "请替换为邮箱授权码",
     *         "auth": true,
     *         "starttlsEnable": true,
     *         "connectionTimeout": 10000,
     *         "timeout": 10000,
     *         "writeTimeout": 10000,
     *         "forceFromSameAsUsername": true
     *       }
     *     },
     *     "to": [
     *       {
     *         "address": "user@example.com",
     *         "name": "测试用户"
     *       }
     *     ],
     *     "subject": "校验邮件",
     *     "content": "这是需要校验的邮件内容",
     *     "contentType": "TEXT",
     *     "priority": "NORMAL",
     *     "charset": "UTF-8",
     *     "throwException": false
     *   }'
     * </pre>
     *
     * @param request 邮件发送请求
     * @return 校验结果
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody MailSendRequest request) {
        mailService.validate(request);
        log.info("邮件发送请求校验通过，业务编号：{}，主题：{}", request.getBizId(), request.getSubject());
        return ResponseEntity.ok(MapUtil.<String, Object>builder()
                .put("valid", true)
                .put("message", "邮件发送请求校验通过")
                .build());
    }

    /**
     * 检查邮件服务器连接。
     *
     * <pre>
     * curl -X GET 'http://localhost:8080/api/mail/connection'
     * </pre>
     *
     * @return 连接检查结果
     */
    @GetMapping("/connection")
    public ResponseEntity<Map<String, Object>> checkConnection() {
        boolean connected = mailService.checkConnection();
        log.info("邮件服务器连接检查完成，结果：{}", connected);
        return ResponseEntity.ok(MapUtil.<String, Object>builder()
                .put("connected", connected)
                .put("message", connected ? "邮件服务器连接正常" : "邮件服务器连接异常")
                .build());
    }

    /**
     * 解析异步线程池。
     *
     * @param executorType 线程池类型
     * @return 线程池
     */
    private Executor resolveExecutor(String executorType) {
        String type = StrUtil.blankToDefault(executorType, "default").toLowerCase();

        return switch (type) {
            case "default", "mail" -> mailTaskExecutor;
            case "high" -> highPriorityMailTaskExecutorProvider.getIfAvailable(() -> mailTaskExecutor);
            case "low" -> lowPriorityMailTaskExecutorProvider.getIfAvailable(() -> mailTaskExecutor);
            default -> throw new IllegalArgumentException("不支持的邮件线程池类型：" + executorType);
        };
    }

    /**
     * 邮件模板渲染请求。
     *
     * @param templateName 模板名称
     * @param variables    模板变量
     */
    public record TemplateRenderRequest(String templateName, Map<String, Object> variables) {
    }
}