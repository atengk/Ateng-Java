package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.listener.PatternMessageListener;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Redisson 消息能力测试控制器
 * 用于演示 RedissonService 的队列、延迟队列、发布订阅、模式订阅、可靠消息对象入口、Stream 消息流等能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/message")
@RequiredArgsConstructor
public class RedissonMessageController {

    private final RedissonService redissonService;

    private final Map<String, List<Integer>> topicListenerMap = new ConcurrentHashMap<>();

    private final Map<String, List<Integer>> patternTopicListenerMap = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Queue / BlockingQueue / DelayedQueue
    // -------------------------------------------------------------------------

    /**
     * 普通队列入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/queue/enqueue?queueKey=queue:test" \
     *   -H "Content-Type: application/json" \
     *   -d '{"id":1,"name":"Ateng"}'
     *
     * @param queueKey 队列 Key
     * @param value    元素
     * @return 是否入队成功
     */
    @PostMapping("/queue/enqueue")
    public Map<String, Object> enqueue(@RequestParam String queueKey,
                                       @RequestBody Object value) {
        checkKey(queueKey);

        boolean success = redissonService.enqueue(queueKey, value);
        log.info("普通队列入队成功，queueKey={}，success={}", queueKey, success);
        return ok(success);
    }

    /**
     * 普通队列出队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/queue/dequeue?queueKey=queue:test"
     *
     * @param queueKey 队列 Key
     * @return 出队元素
     */
    @PostMapping("/queue/dequeue")
    public Map<String, Object> dequeue(@RequestParam String queueKey) {
        checkKey(queueKey);

        Object value = redissonService.dequeue(queueKey);
        return ok(value);
    }

    /**
     * 阻塞队列入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-queue/enqueue?queueKey=queue:blocking:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"message-1"'
     *
     * @param queueKey 队列 Key
     * @param value    元素
     * @return 执行结果
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/blocking-queue/enqueue")
    public Map<String, Object> enqueueBlocking(@RequestParam String queueKey,
                                               @RequestBody Object value) throws InterruptedException {
        checkKey(queueKey);

        redissonService.enqueueBlocking(queueKey, value);
        log.info("阻塞队列入队成功，queueKey={}", queueKey);
        return ok(true);
    }

    /**
     * 阻塞队列超时入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-queue/enqueue-timeout?queueKey=queue:blocking:test&timeoutSeconds=3" \
     *   -H "Content-Type: application/json" \
     *   -d '"message-1"'
     *
     * @param queueKey       队列 Key
     * @param timeoutSeconds 超时秒数
     * @param value          元素
     * @return 是否入队成功
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/blocking-queue/enqueue-timeout")
    public Map<String, Object> enqueueBlockingTimeout(@RequestParam String queueKey,
                                                      @RequestParam(defaultValue = "3") Long timeoutSeconds,
                                                      @RequestBody Object value) throws InterruptedException {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        boolean success = redissonService.enqueueBlocking(queueKey, value, timeoutSeconds, TimeUnit.SECONDS);
        return ok(success);
    }

    /**
     * 阻塞队列超时出队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-queue/dequeue?queueKey=queue:blocking:test&timeoutSeconds=10"
     *
     * @param queueKey       队列 Key
     * @param timeoutSeconds 超时秒数
     * @return 出队元素
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/blocking-queue/dequeue")
    public Map<String, Object> dequeueBlocking(@RequestParam String queueKey,
                                               @RequestParam(defaultValue = "10") Long timeoutSeconds) throws InterruptedException {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        Object value = redissonService.dequeueBlocking(queueKey, timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 获取队列长度。
     *
     * curl "http://localhost:8080/redisson/message/queue/size?queueKey=queue:test"
     *
     * @param queueKey 队列 Key
     * @return 队列长度
     */
    @GetMapping("/queue/size")
    public Map<String, Object> queueSize(@RequestParam String queueKey) {
        checkKey(queueKey);

        long size = redissonService.queueSize(queueKey);
        return ok(size);
    }

    /**
     * 判断队列是否为空。
     *
     * curl "http://localhost:8080/redisson/message/queue/empty?queueKey=queue:test"
     *
     * @param queueKey 队列 Key
     * @return 是否为空
     */
    @GetMapping("/queue/empty")
    public Map<String, Object> isQueueEmpty(@RequestParam String queueKey) {
        checkKey(queueKey);

        boolean empty = redissonService.isQueueEmpty(queueKey);
        return ok(empty);
    }

    /**
     * 清空队列。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/queue/clear?queueKey=queue:test"
     *
     * @param queueKey 队列 Key
     * @return 执行结果
     */
    @DeleteMapping("/queue/clear")
    public Map<String, Object> clearQueue(@RequestParam String queueKey) {
        checkKey(queueKey);

        redissonService.clearQueue(queueKey);
        log.info("队列清空成功，queueKey={}", queueKey);
        return ok(true);
    }

    /**
     * 删除队列元素。
     *
     * curl -X POST "http://localhost:8080/redisson/message/queue/remove?queueKey=queue:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"message-1"'
     *
     * @param queueKey 队列 Key
     * @param value    元素
     * @return 是否删除成功
     */
    @PostMapping("/queue/remove")
    public Map<String, Object> removeFromQueue(@RequestParam String queueKey,
                                               @RequestBody Object value) {
        checkKey(queueKey);

        boolean success = redissonService.removeFromQueue(queueKey, value);
        return ok(success);
    }

    /**
     * 添加延迟队列任务。
     *
     * curl -X POST "http://localhost:8080/redisson/message/delayed-queue/enqueue?queueKey=queue:delay:test&delaySeconds=10" \
     *   -H "Content-Type: application/json" \
     *   -d '{"taskId":1,"type":"timeout-close-order"}'
     *
     * @param queueKey     队列 Key
     * @param delaySeconds 延迟秒数
     * @param value        元素
     * @return 执行结果
     */
    @PostMapping("/delayed-queue/enqueue")
    public Map<String, Object> enqueueDelayed(@RequestParam String queueKey,
                                              @RequestParam(defaultValue = "10") Long delaySeconds,
                                              @RequestBody Object value) {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(delaySeconds) && delaySeconds >= 0, "delaySeconds不能小于0");

        redissonService.enqueueDelayed(queueKey, value, delaySeconds, TimeUnit.SECONDS);
        log.info("延迟队列任务写入成功，queueKey={}，delaySeconds={}", queueKey, delaySeconds);
        return ok(true);
    }

    /**
     * 获取延迟队列对象信息。
     *
     * curl "http://localhost:8080/redisson/message/delayed-queue/info?queueKey=queue:delay:test"
     *
     * @param queueKey 队列 Key
     * @return 延迟队列信息
     */
    @GetMapping("/delayed-queue/info")
    public Map<String, Object> delayedQueueInfo(@RequestParam String queueKey) {
        checkKey(queueKey);

        RDelayedQueue<Object> delayedQueue = redissonService.getDelayedQueue(queueKey);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", delayedQueue.getClass().getName());
        data.put("queueKey", queueKey);
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // Deque / RingBuffer / PriorityQueue / ReliableQueue
    // -------------------------------------------------------------------------

    /**
     * 阻塞双端队列左侧入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-deque/left-push?key=deque:blocking:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 执行结果
     */
    @PostMapping("/blocking-deque/left-push")
    public Map<String, Object> blockingDequeLeftPush(@RequestParam String key,
                                                     @RequestBody Object value) {
        checkKey(key);

        RBlockingDeque<Object> deque = redissonService.getBlockingDeque(key);
        deque.offerFirst(value);
        return ok(true);
    }

    /**
     * 阻塞双端队列右侧入队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-deque/right-push?key=deque:blocking:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"B"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 执行结果
     */
    @PostMapping("/blocking-deque/right-push")
    public Map<String, Object> blockingDequeRightPush(@RequestParam String key,
                                                      @RequestBody Object value) {
        checkKey(key);

        RBlockingDeque<Object> deque = redissonService.getBlockingDeque(key);
        deque.offerLast(value);
        return ok(true);
    }

    /**
     * 阻塞双端队列左侧出队。
     *
     * curl -X POST "http://localhost:8080/redisson/message/blocking-deque/left-pop?key=deque:blocking:test&timeoutSeconds=5"
     *
     * @param key            Redis 键
     * @param timeoutSeconds 等待秒数
     * @return 元素
     * @throws InterruptedException 线程中断时抛出
     */
    @PostMapping("/blocking-deque/left-pop")
    public Map<String, Object> blockingDequeLeftPop(@RequestParam String key,
                                                    @RequestParam(defaultValue = "5") Long timeoutSeconds) throws InterruptedException {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        RBlockingDeque<Object> deque = redissonService.getBlockingDeque(key);
        Object value = deque.pollFirst(timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 初始化环形缓冲队列容量。
     *
     * curl -X POST "http://localhost:8080/redisson/message/ring-buffer/init?key=ring:test&capacity=10"
     *
     * @param key      Redis 键
     * @param capacity 容量
     * @return 是否初始化成功
     */
    @PostMapping("/ring-buffer/init")
    public Map<String, Object> ringBufferInit(@RequestParam String key,
                                              @RequestParam Integer capacity) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(capacity) && capacity > 0, "capacity必须大于0");

        RRingBuffer<Object> ringBuffer = redissonService.getRingBuffer(key);
        boolean success = ringBuffer.trySetCapacity(capacity);
        return ok(success);
    }

    /**
     * 环形缓冲队列添加元素。
     *
     * curl -X POST "http://localhost:8080/redisson/message/ring-buffer/add?key=ring:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否添加成功
     */
    @PostMapping("/ring-buffer/add")
    public Map<String, Object> ringBufferAdd(@RequestParam String key,
                                             @RequestBody Object value) {
        checkKey(key);

        RRingBuffer<Object> ringBuffer = redissonService.getRingBuffer(key);
        boolean success = ringBuffer.add(value);
        return ok(success);
    }

    /**
     * 获取环形缓冲队列信息。
     *
     * curl "http://localhost:8080/redisson/message/ring-buffer/info?key=ring:test"
     *
     * @param key Redis 键
     * @return 信息
     */
    @GetMapping("/ring-buffer/info")
    public Map<String, Object> ringBufferInfo(@RequestParam String key) {
        checkKey(key);

        RRingBuffer<Object> ringBuffer = redissonService.getRingBuffer(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("size", ringBuffer.size());
        data.put("capacity", ringBuffer.capacity());
        data.put("remainingCapacity", ringBuffer.remainingCapacity());
        data.put("values", ringBuffer.readAll());
        return ok(data);
    }

    /**
     * 优先级队列添加元素。
     * 注意：元素需要实现 Comparable，否则会在运行时排序失败。
     *
     * curl -X POST "http://localhost:8080/redisson/message/priority-queue/add?key=priority:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"A"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否添加成功
     */
    @PostMapping("/priority-queue/add")
    public Map<String, Object> priorityQueueAdd(@RequestParam String key,
                                                @RequestBody Object value) {
        checkKey(key);

        RPriorityQueue<Object> priorityQueue = redissonService.getPriorityQueue(key);
        boolean success = priorityQueue.offer(value);
        return ok(success);
    }

    /**
     * 优先级队列弹出元素。
     *
     * curl -X POST "http://localhost:8080/redisson/message/priority-queue/poll?key=priority:test"
     *
     * @param key Redis 键
     * @return 元素
     */
    @PostMapping("/priority-queue/poll")
    public Map<String, Object> priorityQueuePoll(@RequestParam String key) {
        checkKey(key);

        RPriorityQueue<Object> priorityQueue = redissonService.getPriorityQueue(key);
        Object value = priorityQueue.poll();
        return ok(value);
    }

    /**
     * 获取可靠队列对象信息。
     *
     * curl "http://localhost:8080/redisson/message/reliable-queue/info?key=reliable:test"
     *
     * @param key Redis 键
     * @return 可靠队列信息
     */
    @GetMapping("/reliable-queue/info")
    public Map<String, Object> reliableQueueInfo(@RequestParam String key) {
        checkKey(key);

        RReliableQueue<Object> reliableQueue = redissonService.getReliableQueue(key);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", reliableQueue.getClass().getName());
        data.put("key", key);
        data.put("exists", reliableQueue.isExists());
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // Topic / PatternTopic / ReliableTopic
    // -------------------------------------------------------------------------

    /**
     * 发布消息。
     *
     * curl -X POST "http://localhost:8080/redisson/message/topic/publish?channel=topic:test" \
     *   -H "Content-Type: application/json" \
     *   -d '{"type":"notice","content":"hello"}'
     *
     * @param channel 频道
     * @param message 消息
     * @return 接收客户端数量
     */
    @PostMapping("/topic/publish")
    public Map<String, Object> publish(@RequestParam String channel,
                                       @RequestBody Object message) {
        checkKey(channel);

        long receivers = redissonService.publish(channel, message);
        log.info("Redis Topic 消息发布成功，channel={}，receivers={}", channel, receivers);
        return ok(receivers);
    }

    /**
     * 订阅频道。
     *
     * curl -X POST "http://localhost:8080/redisson/message/topic/subscribe?channel=topic:test"
     *
     * @param channel 频道
     * @return 监听器 ID
     */
    @PostMapping("/topic/subscribe")
    public Map<String, Object> subscribe(@RequestParam String channel) {
        checkKey(channel);

        Consumer<Object> consumer = message -> log.info("收到 Redis Topic 消息，channel={}，message={}", channel, message);
        int listenerId = redissonService.subscribe(channel, consumer);

        topicListenerMap.computeIfAbsent(channel, item -> new CopyOnWriteArrayList<>()).add(listenerId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("channel", channel);
        data.put("listenerId", listenerId);
        return ok(data);
    }

    /**
     * 取消指定频道监听器。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/topic/unsubscribe?channel=topic:test&listenerId=1"
     *
     * @param channel    频道
     * @param listenerId 监听器 ID
     * @return 执行结果
     */
    @DeleteMapping("/topic/unsubscribe")
    public Map<String, Object> unsubscribe(@RequestParam String channel,
                                           @RequestParam Integer listenerId) {
        checkKey(channel);
        Assert.notNull(listenerId, "listenerId不能为空");

        redissonService.unsubscribe(channel, listenerId);
        removeListenerId(topicListenerMap, channel, listenerId);
        return ok(true);
    }

    /**
     * 取消频道全部监听器。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/topic/unsubscribe-all?channel=topic:test"
     *
     * @param channel 频道
     * @return 执行结果
     */
    @DeleteMapping("/topic/unsubscribe-all")
    public Map<String, Object> unsubscribeAll(@RequestParam String channel) {
        checkKey(channel);

        redissonService.unsubscribe(channel);
        topicListenerMap.remove(channel);
        return ok(true);
    }

    /**
     * 查看当前 Controller 记录的 Topic 监听器。
     *
     * curl "http://localhost:8080/redisson/message/topic/listeners"
     *
     * @return 监听器记录
     */
    @GetMapping("/topic/listeners")
    public Map<String, Object> topicListeners() {
        return ok(topicListenerMap);
    }

    /**
     * 订阅模式频道。
     *
     * curl -X POST "http://localhost:8080/redisson/message/pattern-topic/subscribe?pattern=topic:*"
     *
     * @param pattern 频道通配符
     * @return 监听器 ID
     */
    @PostMapping("/pattern-topic/subscribe")
    public Map<String, Object> patternSubscribe(@RequestParam String pattern) {
        checkKey(pattern);

        RPatternTopic patternTopic = redissonService.getPatternTopic(pattern);
        PatternMessageListener<Object> listener = (patternText, channel, message) ->
                log.info("收到 Redis PatternTopic 消息，pattern={}，channel={}，message={}", patternText, channel, message);

        int listenerId = patternTopic.addListener(Object.class, listener);
        patternTopicListenerMap.computeIfAbsent(pattern, item -> new CopyOnWriteArrayList<>()).add(listenerId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pattern", pattern);
        data.put("listenerId", listenerId);
        return ok(data);
    }

    /**
     * 取消模式频道监听器。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/pattern-topic/unsubscribe?pattern=topic:*&listenerId=1"
     *
     * @param pattern    频道通配符
     * @param listenerId 监听器 ID
     * @return 执行结果
     */
    @DeleteMapping("/pattern-topic/unsubscribe")
    public Map<String, Object> patternUnsubscribe(@RequestParam String pattern,
                                                  @RequestParam Integer listenerId) {
        checkKey(pattern);
        Assert.notNull(listenerId, "listenerId不能为空");

        redissonService.getPatternTopic(pattern).removeListener(listenerId);
        removeListenerId(patternTopicListenerMap, pattern, listenerId);
        return ok(true);
    }

    /**
     * 获取可靠主题对象信息。
     *
     * curl "http://localhost:8080/redisson/message/reliable-topic/info?topic=reliable-topic:test"
     *
     * @param topic 主题
     * @return 可靠主题信息
     */
    @GetMapping("/reliable-topic/info")
    public Map<String, Object> reliableTopicInfo(@RequestParam String topic) {
        checkKey(topic);

        RReliableTopic reliableTopic = redissonService.getReliableTopic(topic);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", reliableTopic.getClass().getName());
        data.put("topic", topic);
        return ok(data);
    }

    // -------------------------------------------------------------------------
    // Stream
    // -------------------------------------------------------------------------

    /**
     * 添加 Stream 消息。
     *
     * curl -X POST "http://localhost:8080/redisson/message/stream/add?streamKey=stream:test" \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":"1001","event":"login","time":"2026-04-26 10:00:00"}'
     *
     * @param streamKey Stream Key
     * @param entries   消息字段
     * @return 消息 ID
     */
    @PostMapping("/stream/add")
    public Map<String, Object> streamAdd(@RequestParam String streamKey,
                                         @RequestBody Map<Object, Object> entries) {
        checkKey(streamKey);
        Assert.isTrue(CollUtil.isNotEmpty(entries), "entries不能为空");

        StreamMessageId id = redissonService.streamAdd(streamKey, entries);
        log.info("Redis Stream 消息写入成功，streamKey={}，id={}", streamKey, id);
        return ok(idToMap(id));
    }

    /**
     * 读取 Stream 消息。
     *
     * curl "http://localhost:8080/redisson/message/stream/read?streamKey=stream:test&id=0-0&count=10"
     *
     * @param streamKey Stream Key
     * @param id        起始 ID，格式如 0-0
     * @param count     数量
     * @return 消息 Map
     */
    @GetMapping("/stream/read")
    public Map<String, Object> streamRead(@RequestParam String streamKey,
                                          @RequestParam(defaultValue = "0-0") String id,
                                          @RequestParam(defaultValue = "10") Integer count) {
        checkKey(streamKey);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        StreamMessageId startId = parseStreamMessageId(id);
        StreamReadArgs args = StreamReadArgs.greaterThan(startId).count(count);
        Map<StreamMessageId, Map<Object, Object>> data = redissonService.streamRead(streamKey, args);

        return ok(streamMapToResponse(data));
    }

    /**
     * 创建 Stream 消费组。
     *
     * curl -X POST "http://localhost:8080/redisson/message/stream/group/create?streamKey=stream:test&groupName=group:test&id=0-0"
     *
     * @param streamKey Stream Key
     * @param groupName 消费组
     * @param id        起始 ID，格式如 0-0
     * @return 执行结果
     */
    @PostMapping("/stream/group/create")
    public Map<String, Object> streamCreateGroup(@RequestParam String streamKey,
                                                 @RequestParam String groupName,
                                                 @RequestParam(defaultValue = "0-0") String id) {
        checkKey(streamKey);
        checkKey(groupName);

        StreamMessageId startId = parseStreamMessageId(id);
        redissonService.streamCreateGroup(streamKey, groupName, startId);

        return ok(true);
    }

    /**
     * 读取消费组未投递的新消息。
     *
     * curl "http://localhost:8080/redisson/message/stream/group/read-new?streamKey=stream:test&groupName=group:test&consumerName=consumer:1&count=10"
     *
     * @param streamKey    Stream Key
     * @param groupName    消费组
     * @param consumerName 消费者
     * @param count        数量
     * @return 消息 Map
     */
    @GetMapping("/stream/group/read-new")
    public Map<String, Object> streamReadGroupNew(@RequestParam String streamKey,
                                                  @RequestParam String groupName,
                                                  @RequestParam String consumerName,
                                                  @RequestParam(defaultValue = "10") Integer count) {
        checkKey(streamKey);
        checkKey(groupName);
        checkKey(consumerName);
        Assert.isTrue(ObjectUtil.isNotNull(count) && count > 0, "count必须大于0");

        StreamReadGroupArgs args = StreamReadGroupArgs.neverDelivered().count(count);
        Map<StreamMessageId, Map<Object, Object>> data = redissonService.streamReadGroup(
                streamKey,
                groupName,
                consumerName,
                args
        );

        return ok(streamMapToResponse(data));
    }

    /**
     * 确认 Stream 消息。
     *
     * curl -X POST "http://localhost:8080/redisson/message/stream/ack?streamKey=stream:test&groupName=group:test&ids=1714096800000-0"
     *
     * @param streamKey Stream Key
     * @param groupName 消费组
     * @param ids       消息 ID 集合
     * @return 确认数量
     */
    @PostMapping("/stream/ack")
    public Map<String, Object> streamAck(@RequestParam String streamKey,
                                         @RequestParam String groupName,
                                         @RequestParam List<String> ids) {
        checkKey(streamKey);
        checkKey(groupName);
        Assert.isTrue(CollUtil.isNotEmpty(ids), "ids不能为空");

        StreamMessageId[] messageIds = ids.stream()
                .map(this::parseStreamMessageId)
                .toArray(StreamMessageId[]::new);

        long count = redissonService.streamAck(streamKey, groupName, messageIds);
        return ok(count);
    }

    /**
     * 删除 Stream 消息。
     *
     * curl -X DELETE "http://localhost:8080/redisson/message/stream/remove?streamKey=stream:test&ids=1714096800000-0"
     *
     * @param streamKey Stream Key
     * @param ids       消息 ID 集合
     * @return 删除数量
     */
    @DeleteMapping("/stream/remove")
    public Map<String, Object> streamRemove(@RequestParam String streamKey,
                                            @RequestParam List<String> ids) {
        checkKey(streamKey);
        Assert.isTrue(CollUtil.isNotEmpty(ids), "ids不能为空");

        StreamMessageId[] messageIds = ids.stream()
                .map(this::parseStreamMessageId)
                .toArray(StreamMessageId[]::new);

        long count = redissonService.streamRemove(streamKey, messageIds);
        return ok(count);
    }

    /**
     * 获取 Stream 长度。
     *
     * curl "http://localhost:8080/redisson/message/stream/size?streamKey=stream:test"
     *
     * @param streamKey Stream Key
     * @return 长度
     */
    @GetMapping("/stream/size")
    public Map<String, Object> streamSize(@RequestParam String streamKey) {
        checkKey(streamKey);

        long size = redissonService.streamSize(streamKey);
        return ok(size);
    }

    /**
     * 模拟延迟队列消费者。
     * 调用后会阻塞等待一条消息，适合本地测试，不建议生产接口直接暴露。
     *
     * curl "http://localhost:8080/redisson/message/debug/delayed-queue/take?queueKey=queue:delay:test&timeoutSeconds=30"
     *
     * @param queueKey       队列 Key
     * @param timeoutSeconds 等待秒数
     * @return 消费到的消息
     * @throws InterruptedException 线程中断时抛出
     */
    @GetMapping("/debug/delayed-queue/take")
    public Map<String, Object> debugDelayedQueueTake(@RequestParam String queueKey,
                                                     @RequestParam(defaultValue = "30") Long timeoutSeconds) throws InterruptedException {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        Object value = redissonService.getBlockingQueue(queueKey).poll(timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
    }

    /**
     * 模拟阻塞消费者。
     * 调用后会阻塞等待一条消息，适合本地测试，不建议生产接口直接暴露。
     *
     * curl "http://localhost:8080/redisson/message/debug/blocking-queue/take?queueKey=queue:blocking:test&timeoutSeconds=30"
     *
     * @param queueKey       队列 Key
     * @param timeoutSeconds 等待秒数
     * @return 消费到的消息
     * @throws InterruptedException 线程中断时抛出
     */
    @GetMapping("/debug/blocking-queue/take")
    public Map<String, Object> debugBlockingQueueTake(@RequestParam String queueKey,
                                                      @RequestParam(defaultValue = "30") Long timeoutSeconds) throws InterruptedException {
        checkKey(queueKey);
        Assert.isTrue(ObjectUtil.isNotNull(timeoutSeconds) && timeoutSeconds >= 0, "timeoutSeconds不能小于0");

        Object value = redissonService.dequeueBlocking(queueKey, timeoutSeconds, TimeUnit.SECONDS);
        return ok(value);
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
     * 移除监听器 ID。
     *
     * @param listenerMap 监听器 Map
     * @param key         监听 Key
     * @param listenerId  监听器 ID
     */
    private void removeListenerId(Map<String, List<Integer>> listenerMap, String key, Integer listenerId) {
        List<Integer> listenerIds = listenerMap.get(key);
        if (CollUtil.isEmpty(listenerIds)) {
            return;
        }

        listenerIds.remove(listenerId);
        if (CollUtil.isEmpty(listenerIds)) {
            listenerMap.remove(key);
        }
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
     * StreamMessageId 转换为 Map。
     *
     * @param id StreamMessageId
     * @return Map
     */
    private Map<String, Object> idToMap(StreamMessageId id) {
        if (ObjectUtil.isNull(id)) {
            return null;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id.toString());
        return data;
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
     * Stream 添加请求体。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class StreamAddRequest {

        /**
         * Stream Key。
         */
        private String streamKey;

        /**
         * 消息字段。
         */
        private Map<Object, Object> entries;

    }

}