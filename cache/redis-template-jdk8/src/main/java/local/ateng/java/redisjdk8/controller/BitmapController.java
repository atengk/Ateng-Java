package local.ateng.java.redisjdk8.controller;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Bitmap 控制器
 *
 * @author Ateng
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/bitmap")
@RequiredArgsConstructor
public class BitmapController {

    @Qualifier("jacksonRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置某一位为 true（签到）
     *
     * @param key    Redis Key
     * @param offset 偏移量（从0开始）
     * @return 是否成功
     */
    @PostMapping("/set")
    public Boolean setBit(@RequestParam String key,
                          @RequestParam Long offset) {
        if (ObjectUtil.hasEmpty(key, offset)) {
            return false;
        }
        return redisTemplate.opsForValue().setBit(key, offset, true);
    }

    /**
     * 获取某一位的值
     *
     * @param key    Redis Key
     * @param offset 偏移量
     * @return true/false
     */
    @GetMapping("/get")
    public Boolean getBit(@RequestParam String key,
                          @RequestParam Long offset) {
        if (ObjectUtil.hasEmpty(key, offset)) {
            return false;
        }
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 统计 Bitmap 中 1 的个数（签到总天数）
     *
     * @param key Redis Key
     * @return 数量
     */
    @GetMapping("/count")
    public Long bitCount(@RequestParam String key) {
        if (ObjectUtil.isEmpty(key)) {
            return 0L;
        }
        return redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.bitCount(key.getBytes())
        );
    }

    /**
     * 获取某一段 Bitmap（用于分析）
     *
     * @param key   Redis Key
     * @param start 开始位
     * @param end   结束位
     * @return bit 列表
     */
    @GetMapping("/range")
    public List<Integer> getBitRange(@RequestParam String key,
                                     @RequestParam Integer start,
                                     @RequestParam Integer end) {
        if (ObjectUtil.hasEmpty(key, start, end)) {
            return new ArrayList<>();
        }

        return redisTemplate.execute((RedisCallback<List<Integer>>) connection -> {
            List<Integer> result = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                Boolean bit = connection.getBit(key.getBytes(), i);
                result.add(Boolean.TRUE.equals(bit) ? 1 : 0);
            }
            return result;
        });
    }

    /**
     * 计算连续签到天数（从当前 offset 往前统计）
     *
     * @param key    Redis Key
     * @param offset 当前天（例如：今天是第几天 - 1）
     * @return 连续签到天数
     */
    @GetMapping("/continuous")
    public Integer getContinuousSignCount(@RequestParam String key,
                                          @RequestParam Integer offset) {
        if (ObjectUtil.hasEmpty(key, offset)) {
            return 0;
        }

        return redisTemplate.execute((RedisCallback<Integer>) connection -> {
            int count = 0;

            for (int i = offset; i >= 0; i--) {
                Boolean bit = connection.getBit(key.getBytes(), i);
                if (Boolean.TRUE.equals(bit)) {
                    count++;
                } else {
                    break;
                }
            }
            return count;
        });
    }

    /**
     * 使用 BITFIELD 获取连续签到天数（优化连续签到性能）
     *
     * @param key    Redis Key
     * @param offset 今天是第几天（day-1）
     * @param limit  最大回溯天数（建议 7 / 16 / 32 / 64）
     * @return long 值（二进制表示）
     */
    @GetMapping("/bitfield")
    public Integer getContinuousByBitField(@RequestParam String key,
                                           @RequestParam Integer offset,
                                           @RequestParam(defaultValue = "64") Integer limit) {

        if (ObjectUtil.hasEmpty(key, offset, limit)) {
            return 0;
        }

        return redisTemplate.execute((RedisCallback<Integer>) connection -> {

            /**
             * 计算起始位置（防止负数）
             */
            int start = offset - limit + 1;
            if (start < 0) {
                start = 0;
            }

            /**
             * 实际读取长度
             */
            int realLimit = offset - start + 1;

            List<Long> result = connection.bitField(
                    redisTemplate.getStringSerializer().serialize(key),
                    BitFieldSubCommands.create()
                            .get(BitFieldSubCommands.BitFieldType.unsigned(realLimit))
                            .valueAt(start)
            );

            if (ObjectUtil.isEmpty(result)) {
                return 0;
            }

            Long num = result.get(0);
            if (num == null || num == 0) {
                return 0;
            }

            /**
             * 位运算：计算连续签到天数（从最低位开始）
             */
            int count = 0;
            while ((num & 1) == 1) {
                count++;
                num >>= 1;
            }

            return count;
        });
    }

    /**
     * 清空 Bitmap
     *
     * @param key Redis Key
     * @return 是否删除成功
     */
    @DeleteMapping("/clear")
    public Boolean clear(@RequestParam String key) {
        if (ObjectUtil.isEmpty(key)) {
            return false;
        }
        return redisTemplate.delete(key);
    }
}