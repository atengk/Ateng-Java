package local.ateng.java.redis.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.redis.service.RedissonService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.GeoPosition;
import org.redisson.api.GeoUnit;
import org.redisson.api.geo.GeoSearchArgs;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redisson 场景能力测试控制器
 * 用于演示 RedissonService 的 BitSet 签到、HyperLogLog UV、Geo 地理位置、分布式 Session 等能力。
 *
 * @author Ateng
 * @since 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/redisson/scene")
@RequiredArgsConstructor
public class RedissonSceneController {

    private final RedissonService redissonService;

    // -------------------------------------------------------------------------
    // BitSet / 签到
    // -------------------------------------------------------------------------

    /**
     * 设置位图指定位置。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/bit/set?key=bit:test&index=1&value=true"
     *
     * @param key   Redis 键
     * @param index 位索引
     * @param value 位值
     * @return 执行结果
     */
    @PostMapping("/bit/set")
    public Map<String, Object> bitSet(@RequestParam String key,
                                      @RequestParam Long index,
                                      @RequestParam Boolean value) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(index) && index >= 0, "index不能小于0");
        Assert.notNull(value, "value不能为空");

        redissonService.bitSet(key, index, value);
        log.info("BitSet 设置成功，key={}，index={}，value={}", key, index, value);
        return ok(true);
    }

    /**
     * 获取位图指定位置。
     *
     * curl "http://localhost:8080/redisson/scene/bit/get?key=bit:test&index=1"
     *
     * @param key   Redis 键
     * @param index 位索引
     * @return 位值
     */
    @GetMapping("/bit/get")
    public Map<String, Object> bitGet(@RequestParam String key,
                                      @RequestParam Long index) {
        checkKey(key);
        Assert.isTrue(ObjectUtil.isNotNull(index) && index >= 0, "index不能小于0");

        boolean value = redissonService.bitGet(key, index);
        return ok(value);
    }

    /**
     * 获取位图 true 数量。
     *
     * curl "http://localhost:8080/redisson/scene/bit/count?key=bit:test"
     *
     * @param key Redis 键
     * @return 数量
     */
    @GetMapping("/bit/count")
    public Map<String, Object> bitCount(@RequestParam String key) {
        checkKey(key);

        long count = redissonService.bitCount(key);
        return ok(count);
    }

    /**
     * 清空位图。
     *
     * curl -X DELETE "http://localhost:8080/redisson/scene/bit/clear?key=bit:test"
     *
     * @param key Redis 键
     * @return 执行结果
     */
    @DeleteMapping("/bit/clear")
    public Map<String, Object> bitClear(@RequestParam String key) {
        checkKey(key);

        redissonService.bitClear(key);
        log.info("BitSet 清空成功，key={}", key);
        return ok(true);
    }

    /**
     * 用户签到。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/sign/do?keyPrefix=sign&userId=1001&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期，不传默认今天
     * @return 执行结果
     */
    @PostMapping("/sign/do")
    public Map<String, Object> sign(@RequestParam(defaultValue = "sign") String keyPrefix,
                                    @RequestParam String userId,
                                    @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(userId);

        LocalDate targetDate = parseDate(date);
        redissonService.sign(keyPrefix, userId, targetDate);

        log.info("用户签到成功，keyPrefix={}，userId={}，date={}", keyPrefix, userId, targetDate);
        return ok(true);
    }

    /**
     * 判断用户是否签到。
     *
     * curl "http://localhost:8080/redisson/scene/sign/check?keyPrefix=sign&userId=1001&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期，不传默认今天
     * @return 是否签到
     */
    @GetMapping("/sign/check")
    public Map<String, Object> isSigned(@RequestParam(defaultValue = "sign") String keyPrefix,
                                        @RequestParam String userId,
                                        @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(userId);

        LocalDate targetDate = parseDate(date);
        boolean signed = redissonService.isSigned(keyPrefix, userId, targetDate);

        return ok(signed);
    }

    /**
     * 获取用户指定年份签到天数。
     *
     * curl "http://localhost:8080/redisson/scene/sign/count?keyPrefix=sign&userId=1001&year=2026"
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param year      年份，不传默认当前年
     * @return 签到天数
     */
    @GetMapping("/sign/count")
    public Map<String, Object> getSignCount(@RequestParam(defaultValue = "sign") String keyPrefix,
                                            @RequestParam String userId,
                                            @RequestParam(required = false) Integer year) {
        checkKey(keyPrefix);
        checkKey(userId);

        int targetYear = ObjectUtil.defaultIfNull(year, LocalDate.now().getYear());
        long count = redissonService.getSignCount(keyPrefix, userId, targetYear);

        return ok(count);
    }

    /**
     * 获取用户连续签到天数。
     *
     * curl "http://localhost:8080/redisson/scene/sign/continuous?keyPrefix=sign&userId=1001&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param userId    用户 ID
     * @param date      日期，不传默认今天
     * @return 连续签到天数
     */
    @GetMapping("/sign/continuous")
    public Map<String, Object> getContinuousSignCount(@RequestParam(defaultValue = "sign") String keyPrefix,
                                                      @RequestParam String userId,
                                                      @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(userId);

        LocalDate targetDate = parseDate(date);
        int count = redissonService.getContinuousSignCount(keyPrefix, userId, targetDate);

        return ok(count);
    }

    // -------------------------------------------------------------------------
    // HyperLogLog / UV
    // -------------------------------------------------------------------------

    /**
     * 添加 HyperLogLog 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/hll/add?key=hll:test" \
     *   -H "Content-Type: application/json" \
     *   -d '"user:1"'
     *
     * @param key   Redis 键
     * @param value 元素
     * @return 是否改变基数估算
     */
    @PostMapping("/hll/add")
    public Map<String, Object> hllAdd(@RequestParam String key,
                                      @RequestBody Object value) {
        checkKey(key);
        Assert.notNull(value, "value不能为空");

        boolean success = redissonService.hllAdd(key, value);
        return ok(success);
    }

    /**
     * 批量添加 HyperLogLog 元素。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/hll/add-all?key=hll:test" \
     *   -H "Content-Type: application/json" \
     *   -d '["user:1","user:2","user:3"]'
     *
     * @param key    Redis 键
     * @param values 元素集合
     * @return 是否改变基数估算
     */
    @PostMapping("/hll/add-all")
    public Map<String, Object> hllAddAll(@RequestParam String key,
                                         @RequestBody List<Object> values) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(values), "values不能为空");

        boolean success = redissonService.hllAddAll(key, values);
        return ok(success);
    }

    /**
     * 获取 HyperLogLog 基数估算。
     *
     * curl "http://localhost:8080/redisson/scene/hll/count?key=hll:test"
     *
     * @param key Redis 键
     * @return 基数估算
     */
    @GetMapping("/hll/count")
    public Map<String, Object> hllCount(@RequestParam String key) {
        checkKey(key);

        long count = redissonService.hllCount(key);
        return ok(count);
    }

    /**
     * 合并 HyperLogLog。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/hll/merge?destKey=hll:merge&keys=hll:a&keys=hll:b"
     *
     * @param destKey 目标 Key
     * @param keys    源 Key 集合
     * @return 合并后基数估算
     */
    @PostMapping("/hll/merge")
    public Map<String, Object> hllMerge(@RequestParam String destKey,
                                        @RequestParam List<String> keys) {
        checkKey(destKey);
        Assert.isTrue(CollUtil.isNotEmpty(keys), "keys不能为空");

        long count = redissonService.hllMerge(destKey, keys.toArray(new String[0]));
        return ok(count);
    }

    /**
     * 记录 UV。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/uv/record?keyPrefix=uv&bizKey=home&userFlag=user:1&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param userFlag  用户唯一标识
     * @param date      日期，不传默认今天
     * @return 是否改变基数估算
     */
    @PostMapping("/uv/record")
    public Map<String, Object> uvRecord(@RequestParam(defaultValue = "uv") String keyPrefix,
                                        @RequestParam String bizKey,
                                        @RequestParam String userFlag,
                                        @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(bizKey);
        checkKey(userFlag);

        LocalDate targetDate = parseDate(date);
        boolean success = redissonService.uvRecord(keyPrefix, bizKey, userFlag, targetDate);

        log.info("UV 记录成功，keyPrefix={}，bizKey={}，userFlag={}，date={}",
                keyPrefix, bizKey, userFlag, targetDate);
        return ok(success);
    }

    /**
     * 获取 UV。
     *
     * curl "http://localhost:8080/redisson/scene/uv/count?keyPrefix=uv&bizKey=home&date=2026-04-26"
     *
     * @param keyPrefix 业务 key 前缀
     * @param bizKey    业务标识
     * @param date      日期，不传默认今天
     * @return UV 数量
     */
    @GetMapping("/uv/count")
    public Map<String, Object> uvCount(@RequestParam(defaultValue = "uv") String keyPrefix,
                                       @RequestParam String bizKey,
                                       @RequestParam(required = false) String date) {
        checkKey(keyPrefix);
        checkKey(bizKey);

        LocalDate targetDate = parseDate(date);
        long count = redissonService.uvCount(keyPrefix, bizKey, targetDate);

        return ok(count);
    }

    // -------------------------------------------------------------------------
    // Geo / LBS
    // -------------------------------------------------------------------------

    /**
     * 添加地理位置。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/geo/add?key=geo:store&member=store:1&longitude=116.397128&latitude=39.916527"
     *
     * @param key       Redis 键
     * @param member    成员
     * @param longitude 经度
     * @param latitude  纬度
     * @return 添加数量
     */
    @PostMapping("/geo/add")
    public Map<String, Object> geoAdd(@RequestParam String key,
                                      @RequestParam String member,
                                      @RequestParam Double longitude,
                                      @RequestParam Double latitude) {
        checkKey(key);
        checkKey(member);
        checkGeoCoordinate(longitude, latitude);

        long count = redissonService.geoAdd(key, longitude, latitude, member);
        log.info("Geo 添加成功，key={}，member={}，longitude={}，latitude={}", key, member, longitude, latitude);
        return ok(count);
    }

    /**
     * 成员不存在时添加地理位置。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/geo/try-add?key=geo:store&member=store:2&longitude=116.407128&latitude=39.926527"
     *
     * @param key       Redis 键
     * @param member    成员
     * @param longitude 经度
     * @param latitude  纬度
     * @return 是否添加成功
     */
    @PostMapping("/geo/try-add")
    public Map<String, Object> geoTryAdd(@RequestParam String key,
                                         @RequestParam String member,
                                         @RequestParam Double longitude,
                                         @RequestParam Double latitude) {
        checkKey(key);
        checkKey(member);
        checkGeoCoordinate(longitude, latitude);

        boolean success = redissonService.geoTryAdd(key, longitude, latitude, member);
        return ok(success);
    }

    /**
     * 计算两个成员距离。
     *
     * curl "http://localhost:8080/redisson/scene/geo/distance?key=geo:store&member1=store:1&member2=store:2&unit=KILOMETERS"
     *
     * @param key     Redis 键
     * @param member1 成员 1
     * @param member2 成员 2
     * @param unit    距离单位
     * @return 距离
     */
    @GetMapping("/geo/distance")
    public Map<String, Object> geoDistance(@RequestParam String key,
                                           @RequestParam String member1,
                                           @RequestParam String member2,
                                           @RequestParam(defaultValue = "KILOMETERS") GeoUnit unit) {
        checkKey(key);
        checkKey(member1);
        checkKey(member2);
        Assert.notNull(unit, "unit不能为空");

        Double distance = redissonService.geoDistance(key, member1, member2, unit);
        return ok(distance);
    }

    /**
     * 查询成员 GeoHash。
     *
     * curl "http://localhost:8080/redisson/scene/geo/hash?key=geo:store&members=store:1&members=store:2"
     *
     * @param key     Redis 键
     * @param members 成员集合
     * @return GeoHash Map
     */
    @GetMapping("/geo/hash")
    public Map<String, Object> geoHash(@RequestParam String key,
                                       @RequestParam List<String> members) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(members), "members不能为空");

        Map<Object, String> data = redissonService.geoHash(key, members.toArray());
        return ok(data);
    }

    /**
     * 查询成员坐标。
     *
     * curl "http://localhost:8080/redisson/scene/geo/position?key=geo:store&members=store:1&members=store:2"
     *
     * @param key     Redis 键
     * @param members 成员集合
     * @return 坐标 Map
     */
    @GetMapping("/geo/position")
    public Map<String, Object> geoPosition(@RequestParam String key,
                                           @RequestParam List<String> members) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(members), "members不能为空");

        Map<Object, GeoPosition> data = redissonService.geoPosition(key, members.toArray());
        return ok(data);
    }

    /**
     * 根据坐标查询附近成员。
     *
     * curl "http://localhost:8080/redisson/scene/geo/search?key=geo:store&longitude=116.397128&latitude=39.916527&radius=5&unit=KILOMETERS"
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半径
     * @param unit      距离单位
     * @return 附近成员
     */
    @GetMapping("/geo/search")
    public Map<String, Object> geoSearch(@RequestParam String key,
                                         @RequestParam Double longitude,
                                         @RequestParam Double latitude,
                                         @RequestParam Double radius,
                                         @RequestParam(defaultValue = "KILOMETERS") GeoUnit unit) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.isTrue(ObjectUtil.isNotNull(radius) && radius > 0, "radius必须大于0");
        Assert.notNull(unit, "unit不能为空");

        GeoSearchArgs args = GeoSearchArgs.from(longitude, latitude)
                .radius(radius, unit);

        List<Object> data = redissonService.geoSearch(key, args);
        return ok(data);
    }

    /**
     * 根据坐标查询附近成员及距离。
     *
     * curl "http://localhost:8080/redisson/scene/geo/search-with-distance?key=geo:store&longitude=116.397128&latitude=39.916527&radius=5&unit=KILOMETERS"
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半径
     * @param unit      距离单位
     * @return 成员距离 Map
     */
    @GetMapping("/geo/search-with-distance")
    public Map<String, Object> geoSearchWithDistance(@RequestParam String key,
                                                     @RequestParam Double longitude,
                                                     @RequestParam Double latitude,
                                                     @RequestParam Double radius,
                                                     @RequestParam(defaultValue = "KILOMETERS") GeoUnit unit) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.isTrue(ObjectUtil.isNotNull(radius) && radius > 0, "radius必须大于0");
        Assert.notNull(unit, "unit不能为空");

        GeoSearchArgs args = GeoSearchArgs.from(longitude, latitude)
                .radius(radius, unit);

        Map<Object, Double> data = redissonService.geoSearchWithDistance(key, args);
        return ok(data);
    }

    /**
     * 根据坐标查询附近成员及坐标。
     *
     * curl "http://localhost:8080/redisson/scene/geo/search-with-position?key=geo:store&longitude=116.397128&latitude=39.916527&radius=5&unit=KILOMETERS"
     *
     * @param key       Redis 键
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半径
     * @param unit      距离单位
     * @return 成员坐标 Map
     */
    @GetMapping("/geo/search-with-position")
    public Map<String, Object> geoSearchWithPosition(@RequestParam String key,
                                                     @RequestParam Double longitude,
                                                     @RequestParam Double latitude,
                                                     @RequestParam Double radius,
                                                     @RequestParam(defaultValue = "KILOMETERS") GeoUnit unit) {
        checkKey(key);
        checkGeoCoordinate(longitude, latitude);
        Assert.isTrue(ObjectUtil.isNotNull(radius) && radius > 0, "radius必须大于0");
        Assert.notNull(unit, "unit不能为空");

        GeoSearchArgs args = GeoSearchArgs.from(longitude, latitude)
                .radius(radius, unit);

        Map<Object, GeoPosition> data = redissonService.geoSearchWithPosition(key, args);
        return ok(data);
    }

    /**
     * 删除地理位置成员。
     *
     * curl -X DELETE "http://localhost:8080/redisson/scene/geo/remove?key=geo:store&members=store:1&members=store:2"
     *
     * @param key     Redis 键
     * @param members 成员集合
     * @return 删除数量
     */
    @DeleteMapping("/geo/remove")
    public Map<String, Object> geoRemove(@RequestParam String key,
                                         @RequestParam List<String> members) {
        checkKey(key);
        Assert.isTrue(CollUtil.isNotEmpty(members), "members不能为空");

        long count = redissonService.geoRemove(key, members.toArray());
        log.info("Geo 成员删除成功，key={}，members={}，count={}", key, members, count);
        return ok(count);
    }

    // -------------------------------------------------------------------------
    // Session
    // -------------------------------------------------------------------------

    /**
     * 创建或覆盖会话。
     *
     * curl -X POST "http://localhost:8080/redisson/scene/session/set?keyPrefix=session&token=token001&ttlSeconds=1800" \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":1001,"username":"ateng"}'
     *
     * @param keyPrefix  会话 key 前缀
     * @param token      Token
     * @param ttlSeconds 过期秒数
     * @param session    会话对象
     * @return 执行结果
     */
    @PostMapping("/session/set")
    public Map<String, Object> sessionSet(@RequestParam(defaultValue = "session") String keyPrefix,
                                          @RequestParam String token,
                                          @RequestParam(defaultValue = "1800") Long ttlSeconds,
                                          @RequestBody Map<String, Object> session) {
        checkKey(keyPrefix);
        checkKey(token);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");
        Assert.isTrue(CollUtil.isNotEmpty(session), "session不能为空");

        redissonService.sessionSet(keyPrefix, token, session, Duration.ofSeconds(ttlSeconds));
        log.info("Session 写入成功，keyPrefix={}，token={}，ttlSeconds={}", keyPrefix, token, ttlSeconds);
        return ok(true);
    }

    /**
     * 获取会话。
     *
     * curl "http://localhost:8080/redisson/scene/session/get?keyPrefix=session&token=token001"
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 会话对象
     */
    @GetMapping("/session/get")
    public Map<String, Object> sessionGet(@RequestParam(defaultValue = "session") String keyPrefix,
                                          @RequestParam String token) {
        checkKey(keyPrefix);
        checkKey(token);

        Object session = redissonService.sessionGet(keyPrefix, token, Object.class);
        return ok(session);
    }

    /**
     * 刷新会话过期时间。
     *
     * curl -X PUT "http://localhost:8080/redisson/scene/session/refresh?keyPrefix=session&token=token001&ttlSeconds=1800"
     *
     * @param keyPrefix  会话 key 前缀
     * @param token      Token
     * @param ttlSeconds 过期秒数
     * @return 是否刷新成功
     */
    @PutMapping("/session/refresh")
    public Map<String, Object> sessionRefresh(@RequestParam(defaultValue = "session") String keyPrefix,
                                              @RequestParam String token,
                                              @RequestParam(defaultValue = "1800") Long ttlSeconds) {
        checkKey(keyPrefix);
        checkKey(token);
        Assert.isTrue(ObjectUtil.isNotNull(ttlSeconds) && ttlSeconds > 0, "ttlSeconds必须大于0");

        boolean success = redissonService.sessionRefresh(keyPrefix, token, Duration.ofSeconds(ttlSeconds));
        return ok(success);
    }

    /**
     * 删除会话。
     *
     * curl -X DELETE "http://localhost:8080/redisson/scene/session/delete?keyPrefix=session&token=token001"
     *
     * @param keyPrefix 会话 key 前缀
     * @param token     Token
     * @return 是否删除成功
     */
    @DeleteMapping("/session/delete")
    public Map<String, Object> sessionDelete(@RequestParam(defaultValue = "session") String keyPrefix,
                                             @RequestParam String token) {
        checkKey(keyPrefix);
        checkKey(token);

        boolean success = redissonService.sessionDelete(keyPrefix, token);
        log.info("Session 删除完成，keyPrefix={}，token={}，success={}", keyPrefix, token, success);
        return ok(success);
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
     * 解析日期。
     *
     * @param date 日期文本
     * @return 日期
     */
    private LocalDate parseDate(String date) {
        if (StrUtil.isBlank(date)) {
            return LocalDate.now();
        }
        return LocalDate.parse(date);
    }

    /**
     * 校验 Geo 坐标。
     *
     * @param longitude 经度
     * @param latitude  纬度
     */
    private void checkGeoCoordinate(Double longitude, Double latitude) {
        Assert.notNull(longitude, "longitude不能为空");
        Assert.notNull(latitude, "latitude不能为空");
        Assert.isTrue(longitude >= -180D && longitude <= 180D, "经度必须在 -180 到 180 之间");
        Assert.isTrue(latitude >= -90D && latitude <= 90D, "纬度必须在 -90 到 90 之间");
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
     * Geo 批量添加请求体。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class GeoAddBatchRequest {

        /**
         * Redis 键。
         */
        private String key;

        /**
         * 地理位置集合。
         */
        private List<GeoItem> items;

    }

    /**
     * Geo 位置项。
     *
     * @author Ateng
     * @since 2026-04-26
     */
    @Data
    public static class GeoItem {

        /**
         * 成员。
         */
        private String member;

        /**
         * 经度。
         */
        private Double longitude;

        /**
         * 纬度。
         */
        private Double latitude;

    }

}