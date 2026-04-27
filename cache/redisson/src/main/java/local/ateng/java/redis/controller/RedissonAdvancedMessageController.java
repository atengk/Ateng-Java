package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.queue.*;
import org.redisson.api.stream.StreamTrimArgs;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 高级消息与服务测试控制器
 * 用于演示 RedissonService 的可靠队列、Stream 高级治理、分布式执行器、远程服务、LiveObject 和对象监听能力。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Slf4j
@RestController
@RequestMapping("/redisson/advanced-message")
@RequiredArgsConstructor
public class RedissonAdvancedMessageController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // ReliableQueue / 可靠队列
    // -------------------------------------------------------------------------

    /**
     * 初始化可靠队列配置。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/config/init?key=rq:test&deliveryLimit=5&visibilitySeconds=30&ttlSeconds=3600&delaySeconds=0&maxSize=1000"
     *
     * @param key               队列 key
     * @param deliveryLimit     最大投递次数
     * @param visibilitySeconds 消息可见性超时秒数
     * @param ttlSeconds        消息 TTL 秒数，0 表示不限制
     * @param delaySeconds      默认延迟秒数，0 表示不延迟
     * @param maxSize           最大队列大小，0 表示不限制
     * @return 是否初始化成功
     */
    @PostMapping("/reliable-queue/config/init")
    public Map<String, Object> reliableQueueConfigInit(@RequestParam String key,
                                                       @RequestParam(defaultValue = "10") Integer deliveryLimit,
                                                       @RequestParam(defaultValue = "30") Long visibilitySeconds,
                                                       @RequestParam(defaultValue = "0") Long ttlSeconds,
                                                       @RequestParam(defaultValue = "0") Long delaySeconds,
                                                       @RequestParam(defaultValue = "0") Integer maxSize) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(deliveryLimit) && deliveryLimit > 0, "deliveryLimit必须大于0");
        Assert.isTrue(ObjectUtil.isNotNull(visibilitySeconds) && visibilitySeconds >= 0, "visibilitySeconds不能小于0");
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds >= 0, "ttlSeconds不能小于0");
        Assert.isTrue(ObjectUtil.isNotNull(delaySeconds) && delaySeconds >= 0, "delaySeconds不能小于0");
        Assert.isTrue(ObjectUtil.isNotNull(maxSize) && maxSize >= 0, "maxSize不能小于0");

        QueueConfig config = QueueConfig.defaults()
                .deliveryLimit(deliveryLimit)
                .visibility(Duration.ofSeconds(visibilitySeconds))
                .timeToLive(Duration.ofSeconds(ttlSeconds))
                .delay(Duration.ofSeconds(delaySeconds))
                .maxSize(maxSize);

        boolean success = redissonService.reliableQueueSetConfigIfAbsent(key, config);
        return ok(success);
    }

    /**
     * 覆盖可靠队列配置。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/advanced-message/reliable-queue/config?key=rq:test&deliveryLimit=5&visibilitySeconds=30&ttlSeconds=3600&delaySeconds=0&maxSize=1000"
     *
     * @param key               队列 key
     * @param deliveryLimit     最大投递次数
     * @param visibilitySeconds 消息可见性超时秒数
     * @param ttlSeconds        消息 TTL 秒数，0 表示不限制
     * @param delaySeconds      默认延迟秒数，0 表示不延迟
     * @param maxSize           最大队列大小，0 表示不限制
     * @return 执行结果
     */
    @PutMapping("/reliable-queue/config")
    public Map<String, Object> reliableQueueConfigSet(@RequestParam String key,
                                                      @RequestParam(defaultValue = "10") Integer deliveryLimit,
                                                      @RequestParam(defaultValue = "30") Long visibilitySeconds,
                                                      @RequestParam(defaultValue = "0") Long ttlSeconds,
                                                      @RequestParam(defaultValue = "0") Long delaySeconds,
                                                      @RequestParam(defaultValue = "0") Integer maxSize) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(deliveryLimit) && deliveryLimit > 0, "deliveryLimit必须大于0");

        QueueConfig config = QueueConfig.defaults()
                .deliveryLimit(deliveryLimit)
                .visibility(Duration.ofSeconds(visibilitySeconds))
                .timeToLive(Duration.ofSeconds(ttlSeconds))
                .delay(Duration.ofSeconds(delaySeconds))
                .maxSize(maxSize);

        redissonService.reliableQueueSetConfig(key, config);
        return ok(true);
    }

    /**
     * 添加可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/add?key=rq:test&delaySeconds=0&ttlSeconds=3600&deliveryLimit=5&priority=0" \
     * -H "Content-Type: application/json" \
     * -d '{"id":1,"name":"Ateng"}'
     *
     * @param key             队列 key
     * @param delaySeconds    消息延迟秒数
     * @param ttlSeconds      消息 TTL 秒数，0 表示不限制
     * @param deliveryLimit   最大投递次数
     * @param priority        优先级
     * @param deduplicationId 去重 ID，可选
     * @param payload         消息体
     * @return 消息信息
     */
    @PostMapping("/reliable-queue/add")
    public Map<String, Object> reliableQueueAdd(@RequestParam String key,
                                                @RequestParam(defaultValue = "0") Long delaySeconds,
                                                @RequestParam(defaultValue = "0") Long ttlSeconds,
                                                @RequestParam(defaultValue = "10") Integer deliveryLimit,
                                                @RequestParam(defaultValue = "0") Integer priority,
                                                @RequestParam(required = false) String deduplicationId,
                                                @RequestBody Object payload) {
        checkKey(key);
        Assert.notNull(payload, "payload不能为空");

        MessageArgs<Object> messageArgs = MessageArgs.payload(payload)
                .deliveryLimit(deliveryLimit)
                .priority(priority);

        if (ObjectUtil.isNotNull(delaySeconds) && delaySeconds > 0) {
            messageArgs.delay(Duration.ofSeconds(delaySeconds));
        }
        if (ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0) {
            messageArgs.timeToLive(Duration.ofSeconds(ttlSeconds));
        }
        if (StrUtil.isNotBlank(deduplicationId)) {
            messageArgs.deduplicationById(deduplicationId, Duration.ofHours(1));
        }

        Message<Object> message = redissonService.reliableQueueAdd(key, QueueAddArgs.messages(messageArgs));
        return ok(messageToMap(message));
    }

    /**
     * 批量添加可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/add-many?key=rq:test" \
     * -H "Content-Type: application/json" \
     * -d '["A","B","C"]'
     *
     * @param key      队列 key
     * @param payloads 消息体集合
     * @return 消息信息集合
     */
    @PostMapping("/reliable-queue/add-many")
    public Map<String, Object> reliableQueueAddMany(@RequestParam String key,
                                                    @RequestBody List<Object> payloads) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(payloads), "payloads不能为空");

        @SuppressWarnings("unchecked")
        MessageArgs<Object>[] args = payloads.stream()
                .filter(ObjectUtil::isNotNull)
                .map(MessageArgs::payload)
                .toArray(MessageArgs[]::new);

        if (ArrayUtil.isEmpty(args)) {
            return ok(List.of());
        }

        List<Message<Object>> messages = redissonService.reliableQueueAddMany(key, QueueAddArgs.messages(args));
        return ok(messages.stream().map(this::messageToMap).toList());
    }

    /**
     * 拉取一条可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/poll?key=rq:test&ackMode=MANUAL&visibilitySeconds=30&timeoutSeconds=3"
     *
     * @param key               队列 key
     * @param ackMode           确认模式，MANUAL 或 AUTO
     * @param visibilitySeconds 可见性超时秒数
     * @param timeoutSeconds    长轮询等待秒数
     * @return 消息信息
     */
    @PostMapping("/reliable-queue/poll")
    public Map<String, Object> reliableQueuePoll(@RequestParam String key,
                                                 @RequestParam(defaultValue = "MANUAL") AcknowledgeMode ackMode,
                                                 @RequestParam(defaultValue = "30") Long visibilitySeconds,
                                                 @RequestParam(defaultValue = "0") Long timeoutSeconds) {
        checkKey(key);
        Assert.notNull(ackMode, "ackMode不能为空");

        QueuePollArgs args = QueuePollArgs.defaults()
                .acknowledgeMode(ackMode)
                .visibility(Duration.ofSeconds(visibilitySeconds));

        if (ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds > 0) {
            args.timeout(Duration.ofSeconds(timeoutSeconds));
        }

        Message<Object> message = redissonService.reliableQueuePoll(key, args);
        return ok(messageToMap(message));
    }

    /**
     * 批量拉取可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/poll-many?key=rq:test&count=10&ackMode=MANUAL&visibilitySeconds=30&timeoutSeconds=3"
     *
     * @param key               队列 key
     * @param count             拉取数量
     * @param ackMode           确认模式
     * @param visibilitySeconds 可见性超时秒数
     * @param timeoutSeconds    长轮询等待秒数
     * @return 消息信息集合
     */
    @PostMapping("/reliable-queue/poll-many")
    public Map<String, Object> reliableQueuePollMany(@RequestParam String key,
                                                     @RequestParam(defaultValue = "10") Integer count,
                                                     @RequestParam(defaultValue = "MANUAL") AcknowledgeMode ackMode,
                                                     @RequestParam(defaultValue = "30") Long visibilitySeconds,
                                                     @RequestParam(defaultValue = "0") Long timeoutSeconds) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        QueuePollArgs args = QueuePollArgs.defaults()
                .acknowledgeMode(ackMode)
                .visibility(Duration.ofSeconds(visibilitySeconds))
                .count(count);

        if (ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds > 0) {
            args.timeout(Duration.ofSeconds(timeoutSeconds));
        }

        List<Message<Object>> messages = redissonService.reliableQueuePollMany(key, args);
        return ok(messages.stream().map(this::messageToMap).toList());
    }

    /**
     * 确认可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/ack?key=rq:test&ids=xxx&ids=yyy"
     *
     * @param key 队列 key
     * @param ids 消息 ID 集合
     * @return 执行结果
     */
    @PostMapping("/reliable-queue/ack")
    public Map<String, Object> reliableQueueAck(@RequestParam String key,
                                                @RequestParam List<String> ids) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(ids), "ids不能为空");

        redissonService.reliableQueueAck(key, QueueAckArgs.ids(ids.toArray(new String[0])));
        return ok(true);
    }

    /**
     * 拒绝可靠队列消息。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/reliable-queue/nack/rejected?key=rq:test&id=xxx"
     *
     * @param key 队列 key
     * @param id  消息 ID
     * @return 执行结果
     */
    @PostMapping("/reliable-queue/nack/rejected")
    public Map<String, Object> reliableQueueNackRejected(@RequestParam String key,
                                                         @RequestParam String id) {
        checkKey(key);
        checkKey(id);

        redissonService.reliableQueueNack(key, QueueNegativeAckArgs.rejected(id));
        return ok(true);
    }

    /**
     * 根据 ID 获取可靠队列消息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/reliable-queue/get?key=rq:test&id=xxx"
     *
     * @param key 队列 key
     * @param id  消息 ID
     * @return 消息信息
     */
    @GetMapping("/reliable-queue/get")
    public Map<String, Object> reliableQueueGet(@RequestParam String key,
                                                @RequestParam String id) {
        checkKey(key);
        checkKey(id);

        Message<Object> message = redissonService.reliableQueueGet(key, id);
        return ok(messageToMap(message));
    }

    /**
     * 获取可靠队列所有可拉取消息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/reliable-queue/list-all?key=rq:test"
     *
     * @param key 队列 key
     * @return 消息集合
     */
    @GetMapping("/reliable-queue/list-all")
    public Map<String, Object> reliableQueueListAll(@RequestParam String key) {
        checkKey(key);

        List<Message<Object>> messages = redissonService.reliableQueueListAll(key);
        return ok(messages.stream().map(this::messageToMap).toList());
    }

    /**
     * 获取可靠队列统计信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/reliable-queue/info?key=rq:test"
     *
     * @param key 队列 key
     * @return 统计信息
     */
    @GetMapping("/reliable-queue/info")
    public Map<String, Object> reliableQueueInfo(@RequestParam String key) {
        checkKey(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("size", redissonService.reliableQueueSize(key));
        data.put("delayedSize", redissonService.reliableQueueDelayedSize(key));
        data.put("unacknowledgedSize", redissonService.reliableQueueUnacknowledgedSize(key));
        data.put("deadLetterSources", redissonService.reliableQueueDeadLetterSources(key));
        return ok(data);
    }

    /**
     * 清空可靠队列。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/reliable-queue/clear?key=rq:test"
     *
     * @param key 队列 key
     * @return 是否清空成功
     */
    @DeleteMapping("/reliable-queue/clear")
    public Map<String, Object> reliableQueueClear(@RequestParam String key) {
        checkKey(key);

        boolean success = redissonService.reliableQueueClear(key);
        return ok(success);
    }

    /**
     * 禁用可靠队列操作。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/advanced-message/reliable-queue/operation/disable?key=rq:test&operation=ADD"
     *
     * @param key       队列 key
     * @param operation 队列操作
     * @return 执行结果
     */
    @PutMapping("/reliable-queue/operation/disable")
    public Map<String, Object> reliableQueueDisableOperation(@RequestParam String key,
                                                             @RequestParam QueueOperation operation) {
        checkKey(key);
        Assert.notNull(operation, "operation不能为空");

        redissonService.reliableQueueDisableOperation(key, operation);
        return ok(true);
    }

    /**
     * 启用可靠队列操作。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/advanced-message/reliable-queue/operation/enable?key=rq:test&operation=ADD"
     *
     * @param key       队列 key
     * @param operation 队列操作
     * @return 执行结果
     */
    @PutMapping("/reliable-queue/operation/enable")
    public Map<String, Object> reliableQueueEnableOperation(@RequestParam String key,
                                                            @RequestParam QueueOperation operation) {
        checkKey(key);
        Assert.notNull(operation, "operation不能为空");

        redissonService.reliableQueueEnableOperation(key, operation);
        return ok(true);
    }

    // -------------------------------------------------------------------------
    // Stream / 高级消费治理
    // -------------------------------------------------------------------------

    /**
     * 获取 Stream 详细信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/info?streamKey=stream:test"
     *
     * @param streamKey Stream key
     * @return Stream 信息
     */
    @GetMapping("/stream/info")
    public Map<String, Object> streamInfo(@RequestParam String streamKey) {
        checkKey(streamKey);

        StreamInfo<Object, Object> info = redissonService.streamInfo(streamKey);
        return ok(info);
    }

    /**
     * 获取 Stream 消费组列表。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/groups?streamKey=stream:test"
     *
     * @param streamKey Stream key
     * @return 消费组列表
     */
    @GetMapping("/stream/groups")
    public Map<String, Object> streamListGroups(@RequestParam String streamKey) {
        checkKey(streamKey);

        List<StreamGroup> groups = redissonService.streamListGroups(streamKey);
        return ok(groups);
    }

    /**
     * 获取 Stream 消费者列表。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/consumers?streamKey=stream:test&groupName=group:test"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 消费者列表
     */
    @GetMapping("/stream/consumers")
    public Map<String, Object> streamListConsumers(@RequestParam String streamKey,
                                                   @RequestParam String groupName) {
        checkKey(streamKey);
        checkKey(groupName);

        List<StreamConsumer> consumers = redissonService.streamListConsumers(streamKey, groupName);
        return ok(consumers);
    }

    /**
     * 创建 Stream 消费者。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/stream/consumer/create?streamKey=stream:test&groupName=group:test&consumerName=consumer:1"
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @return 执行结果
     */
    @PostMapping("/stream/consumer/create")
    public Map<String, Object> streamCreateConsumer(@RequestParam String streamKey,
                                                    @RequestParam String groupName,
                                                    @RequestParam String consumerName) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);

        redissonService.streamCreateConsumer(streamKey, groupName, consumerName);
        return ok(true);
    }

    /**
     * 删除 Stream 消费者。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/stream/consumer/remove?streamKey=stream:test&groupName=group:test&consumerName=consumer:1"
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @return 待处理消息数量
     */
    @DeleteMapping("/stream/consumer/remove")
    public Map<String, Object> streamRemoveConsumer(@RequestParam String streamKey,
                                                    @RequestParam String groupName,
                                                    @RequestParam String consumerName) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);

        long pendingCount = redissonService.streamRemoveConsumer(streamKey, groupName, consumerName);
        return ok(pendingCount);
    }

    /**
     * 删除 Stream 消费组。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/stream/group/remove?streamKey=stream:test&groupName=group:test"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 执行结果
     */
    @DeleteMapping("/stream/group/remove")
    public Map<String, Object> streamRemoveGroup(@RequestParam String streamKey,
                                                 @RequestParam String groupName) {
        checkKey(streamKey);
        checkKey(groupName);

        redissonService.streamRemoveGroup(streamKey, groupName);
        return ok(true);
    }

    /**
     * 更新 Stream 消费组读取起始 ID。
     * <p>
     * curl -X PUT "http://localhost:8080/redisson/advanced-message/stream/group/message-id?streamKey=stream:test&groupName=group:test&id=0-0"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param id        消息 ID
     * @return 执行结果
     */
    @PutMapping("/stream/group/message-id")
    public Map<String, Object> streamUpdateGroupMessageId(@RequestParam String streamKey,
                                                          @RequestParam String groupName,
                                                          @RequestParam String id) {
        checkKey(streamKey);
        checkKey(groupName);

        redissonService.streamUpdateGroupMessageId(streamKey, groupName, parseStreamMessageId(id));
        return ok(true);
    }

    /**
     * 获取 Stream 待处理消息概要。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/pending/info?streamKey=stream:test&groupName=group:test"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @return 待处理概要
     */
    @GetMapping("/stream/pending/info")
    public Map<String, Object> streamPendingInfo(@RequestParam String streamKey,
                                                 @RequestParam String groupName) {
        checkKey(streamKey);
        checkKey(groupName);

        PendingResult result = redissonService.streamPendingInfo(streamKey, groupName);
        return ok(result);
    }

    /**
     * 获取 Stream 待处理消息列表。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/pending/list?streamKey=stream:test&groupName=group:test&startId=0-0&endId=9999999999999-0&count=10"
     *
     * @param streamKey Stream key
     * @param groupName 消费组
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 待处理消息列表
     */
    @GetMapping("/stream/pending/list")
    public Map<String, Object> streamListPending(@RequestParam String streamKey,
                                                 @RequestParam String groupName,
                                                 @RequestParam(defaultValue = "0-0") String startId,
                                                 @RequestParam(defaultValue = "9999999999999-0") String endId,
                                                 @RequestParam(defaultValue = "10") Integer count) {
        checkKey(streamKey);
        checkKey(groupName);

        List<PendingEntry> result = redissonService.streamListPending(
                streamKey,
                groupName,
                parseStreamMessageId(startId),
                parseStreamMessageId(endId),
                count
        );
        return ok(result);
    }

    /**
     * 按范围读取 Stream 消息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/range?streamKey=stream:test&startId=0-0&endId=9999999999999-0&count=10"
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 消息 Map
     */
    @GetMapping("/stream/range")
    public Map<String, Object> streamRange(@RequestParam String streamKey,
                                           @RequestParam(defaultValue = "0-0") String startId,
                                           @RequestParam(defaultValue = "9999999999999-0") String endId,
                                           @RequestParam(required = false) Integer count) {
        checkKey(streamKey);

        Map<StreamMessageId, Map<Object, Object>> data;
        if (ObjectUtil.isNotNull(count) && count > 0) {
            data = redissonService.streamRange(streamKey, parseStreamMessageId(startId), parseStreamMessageId(endId), count);
        } else {
            data = redissonService.streamRange(streamKey, parseStreamMessageId(startId), parseStreamMessageId(endId));
        }

        return ok(streamMapToResponse(data));
    }

    /**
     * 按范围倒序读取 Stream 消息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/stream/range-reversed?streamKey=stream:test&startId=9999999999999-0&endId=0-0&count=10"
     *
     * @param streamKey Stream key
     * @param startId   开始 ID
     * @param endId     结束 ID
     * @param count     数量
     * @return 消息 Map
     */
    @GetMapping("/stream/range-reversed")
    public Map<String, Object> streamRangeReversed(@RequestParam String streamKey,
                                                   @RequestParam(defaultValue = "9999999999999-0") String startId,
                                                   @RequestParam(defaultValue = "0-0") String endId,
                                                   @RequestParam(required = false) Integer count) {
        checkKey(streamKey);

        Map<StreamMessageId, Map<Object, Object>> data;
        if (ObjectUtil.isNotNull(count) && count > 0) {
            data = redissonService.streamRangeReversed(streamKey, parseStreamMessageId(startId), parseStreamMessageId(endId), count);
        } else {
            data = redissonService.streamRangeReversed(streamKey, parseStreamMessageId(startId), parseStreamMessageId(endId));
        }

        return ok(streamMapToResponse(data));
    }

    /**
     * 转移待处理 Stream 消息所有权。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/stream/claim?streamKey=stream:test&groupName=group:test&consumerName=consumer:2&idleSeconds=60&ids=1714096800000-0"
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 新消费者
     * @param idleSeconds  最小空闲秒数
     * @param ids          消息 ID 集合
     * @return 转移后的消息
     */
    @PostMapping("/stream/claim")
    public Map<String, Object> streamClaim(@RequestParam String streamKey,
                                           @RequestParam String groupName,
                                           @RequestParam String consumerName,
                                           @RequestParam(defaultValue = "60") Long idleSeconds,
                                           @RequestParam List<String> ids) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        Assert.isTrue(CollUtil.isNotEmpty(ids), "ids不能为空");

        StreamMessageId[] messageIds = ids.stream()
                .map(this::parseStreamMessageId)
                .toArray(StreamMessageId[]::new);

        Map<StreamMessageId, Map<Object, Object>> data = redissonService.streamClaim(
                streamKey,
                groupName,
                consumerName,
                idleSeconds,
                TimeUnit.SECONDS,
                messageIds
        );

        return ok(streamMapToResponse(data));
    }

    /**
     * 自动转移待处理 Stream 消息所有权。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/stream/auto-claim?streamKey=stream:test&groupName=group:test&consumerName=consumer:2&idleSeconds=60&startId=0-0&count=10"
     *
     * @param streamKey    Stream key
     * @param groupName    消费组
     * @param consumerName 新消费者
     * @param idleSeconds  最小空闲秒数
     * @param startId      起始 ID
     * @param count        数量
     * @return 自动转移结果
     */
    @PostMapping("/stream/auto-claim")
    public Map<String, Object> streamAutoClaim(@RequestParam String streamKey,
                                               @RequestParam String groupName,
                                               @RequestParam String consumerName,
                                               @RequestParam(defaultValue = "60") Long idleSeconds,
                                               @RequestParam(defaultValue = "0-0") String startId,
                                               @RequestParam(defaultValue = "10") Integer count) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);

        AutoClaimResult<Object, Object> result = redissonService.streamAutoClaim(
                streamKey,
                groupName,
                consumerName,
                idleSeconds,
                TimeUnit.SECONDS,
                parseStreamMessageId(startId),
                count
        );

        return ok(result);
    }

    /**
     * 按最大长度裁剪 Stream。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/stream/trim/max-size?streamKey=stream:test&maxSize=1000"
     *
     * @param streamKey Stream key
     * @param maxSize   最大长度
     * @return 裁剪数量
     */
    @DeleteMapping("/stream/trim/max-size")
    public Map<String, Object> streamTrimMaxSize(@RequestParam String streamKey,
                                                 @RequestParam Integer maxSize) {
        checkKey(streamKey);
        Assert.isTrue(ObjectUtil.isNotNull(maxSize) && maxSize > 0, "maxSize必须大于0");

        long count = redissonService.streamTrim(streamKey, StreamTrimArgs.maxLen(maxSize).noLimit());
        return ok(count);
    }

    // -------------------------------------------------------------------------
    // Executor / Scheduler / RemoteService / LiveObject
    // -------------------------------------------------------------------------

    /**
     * 获取分布式执行器信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/executor/info?name=executor:test"
     *
     * @param name 执行器名称
     * @return 执行器信息
     */
    @GetMapping("/executor/info")
    public Map<String, Object> executorInfo(@RequestParam String name) {
        checkKey(name);

        RExecutorService executorService = redissonService.getExecutorService(name);
        RScheduledExecutorService scheduledExecutorService = redissonService.getScheduledExecutorService(name);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("executorClass", executorService.getClass().getName());
        data.put("scheduledExecutorClass", scheduledExecutorService.getClass().getName());
        data.put("shutdown", executorService.isShutdown());
        data.put("terminated", executorService.isTerminated());
        return ok(data);
    }

    /**
     * 关闭分布式执行器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/executor/shutdown?name=executor:test"
     *
     * @param name 执行器名称
     * @return 执行结果
     */
    @PostMapping("/executor/shutdown")
    public Map<String, Object> executorShutdown(@RequestParam String name) {
        checkKey(name);

        redissonService.executorShutdown(name);
        return ok(true);
    }

    /**
     * 获取远程服务对象信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/remote-service/info"
     *
     * @return 远程服务信息
     */
    @GetMapping("/remote-service/info")
    public Map<String, Object> remoteServiceInfo() {
        RRemoteService remoteService = redissonService.getRemoteService();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", remoteService.getClass().getName());
        return ok(data);
    }

    /**
     * 获取指定名称远程服务对象信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/remote-service/named-info?name=remote:test"
     *
     * @param name 服务名称
     * @return 远程服务信息
     */
    @GetMapping("/remote-service/named-info")
    public Map<String, Object> namedRemoteServiceInfo(@RequestParam String name) {
        checkKey(name);

        RRemoteService remoteService = redissonService.getRemoteService(name);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("className", remoteService.getClass().getName());
        return ok(data);
    }

    /**
     * 获取 LiveObject 服务对象信息。
     * <p>
     * curl "http://localhost:8080/redisson/advanced-message/live-object/info"
     *
     * @return LiveObject 服务信息
     */
    @GetMapping("/live-object/info")
    public Map<String, Object> liveObjectInfo() {
        RLiveObjectService liveObjectService = redissonService.getLiveObjectService();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", liveObjectService.getClass().getName());
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // Object Listener / 对象监听入口
    // -------------------------------------------------------------------------

    /**
     * 添加 Bucket 删除事件监听器。
     * 注意：该接口只是本地测试监听器注册，服务重启后监听器会丢失。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/listener/bucket/delete?key=bucket:test"
     *
     * @param key Redis 键
     * @return 监听器 ID
     */
    @PostMapping("/listener/bucket/delete")
    public Map<String, Object> addBucketDeleteListener(@RequestParam String key) {
        checkKey(key);

        DeletedObjectListener listener = name -> log.info("监听到 Bucket 删除事件，key={}，name={}", key, name);
        int listenerId = redissonService.addBucketListener(key, listener);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("listenerId", listenerId);
        return ok(data);
    }

    /**
     * 移除 Bucket 监听器。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/listener/bucket?key=bucket:test&listenerId=1"
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     * @return 执行结果
     */
    @DeleteMapping("/listener/bucket")
    public Map<String, Object> removeBucketListener(@RequestParam String key,
                                                    @RequestParam Integer listenerId) {
        checkKey(key);
        Assert.notNull(listenerId, "listenerId不能为空");

        redissonService.removeBucketListener(key, listenerId);
        return ok(true);
    }

    /**
     * 添加 Queue 对象监听器。
     * <p>
     * curl -X POST "http://localhost:8080/redisson/advanced-message/listener/queue?key=queue:test"
     *
     * @param key Redis 键
     * @return 监听器 ID
     */
    @PostMapping("/listener/queue")
    public Map<String, Object> addQueueListener(@RequestParam String key) {
        checkKey(key);

        DeletedObjectListener listener = name -> log.info("监听到 Bucket 删除事件，key={}，name={}", key, name);
        int listenerId = redissonService.addQueueListener(key, listener);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("listenerId", listenerId);
        return ok(data);
    }

    /**
     * 移除 Queue 对象监听器。
     * <p>
     * curl -X DELETE "http://localhost:8080/redisson/advanced-message/listener/queue?key=queue:test&listenerId=1"
     *
     * @param key        Redis 键
     * @param listenerId 监听器 ID
     * @return 执行结果
     */
    @DeleteMapping("/listener/queue")
    public Map<String, Object> removeQueueListener(@RequestParam String key,
                                                   @RequestParam Integer listenerId) {
        checkKey(key);
        Assert.notNull(listenerId, "listenerId不能为空");

        redissonService.removeQueueListener(key, listenerId);
        return ok(true);
    }

    /**
     * 转换可靠队列消息。
     *
     * @param message 可靠队列消息
     * @return Map
     */
    private Map<String, Object> messageToMap(Message<Object> message) {
        if (ObjectUtil.isNull(message)) {
            return null;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", message.getId());
        data.put("payload", message.getPayload());
        data.put("headers", message.getHeaders());
        return data;
    }

    /**
     * 解析 Stream 消息 ID。
     *
     * @param id ID 文本，格式为 0-0
     * @return StreamMessageId
     */
    private StreamMessageId parseStreamMessageId(String id) {
        Assert.isTrue(StrUtil.isNotBlank(id), "Stream消息ID不能为空");

        List<String> parts = StrUtil.split(id, "-");
        Assert.isTrue(parts.size() == 2, "Stream消息ID格式错误，正确格式如 0-0");

        long first = Long.parseLong(parts.get(0));
        long second = Long.parseLong(parts.get(1));
        return new StreamMessageId(first, second);
    }

    /**
     * Stream 消息转换为响应结构。
     *
     * @param streamMap Stream 消息 Map
     * @return 响应 Map
     */
    private Map<String, Object> streamMapToResponse(Map<StreamMessageId, Map<Object, Object>> streamMap) {
        if (CollUtil.isEmpty(streamMap)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        streamMap.forEach((id, body) -> result.put(id.toString(), body));
        return result;
    }

    /**
     * 校验 Redis Key。
     *
     * @param key Redis 键
     */
    private void checkKey(String key) {
        Assert.isTrue(StrUtil.isNotBlank(key), "Redis Key不能为空");
    }

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 响应 Map
     */
    private Map<String, Object> ok(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("message", "操作成功");
        result.put("data", data);
        return result;
    }

    /**
     * 可靠队列添加请求体。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    @Data
    public static class ReliableQueueAddRequest {

        /**
         * 消息体。
         */
        private Object payload;

        /**
         * 延迟秒数。
         */
        private Long delaySeconds;

        /**
         * 消息 TTL 秒数。
         */
        private Long ttlSeconds;

        /**
         * 最大投递次数。
         */
        private Integer deliveryLimit;

        /**
         * 优先级。
         */
        private Integer priority;

        /**
         * 去重 ID。
         */
        private String deduplicationId;

    }

}