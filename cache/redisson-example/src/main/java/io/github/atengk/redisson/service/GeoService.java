package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.GeoEntry;
import org.redisson.api.GeoUnit;
import org.redisson.api.RGeo;
import org.redisson.api.RedissonClient;
import org.redisson.api.geo.GeoSearchArgs;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 地理位置服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoService {

    private final RedissonClient redissonClient;

    private static final String GEO_KEY = "geo:store";

    /**
     * 添加位置（门店/用户）
     *
     * @param name 名称
     * @param longitude 经度
     * @param latitude 纬度
     */
    public void add(String name, double longitude, double latitude) {

        RGeo<String> geo = redissonClient.getGeo(GEO_KEY);

        geo.add(longitude, latitude, name);

        log.info("添加地理位置成功，name={}，lng={}，lat={}", name, longitude, latitude);
    }

    /**
     * 查询附近（指定坐标）
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @param radius 半径（km）
     * @return 结果
     */
    public List<String> radius(double longitude, double latitude, double radius) {

        RGeo<String> geo = redissonClient.getGeo(GEO_KEY);

        return geo.search(
                GeoSearchArgs.from(longitude, latitude)
                        .radius(radius, GeoUnit.KILOMETERS)
        );
    }

    /**
     * 计算两点距离（米）
     *
     * @param name1 点1
     * @param name2 点2
     * @return 距离
     */
    public Double distance(String name1, String name2) {

        RGeo<String> geo = redissonClient.getGeo(GEO_KEY);

        return geo.dist(name1, name2, GeoUnit.METERS);
    }

    /**
     * 批量添加
     *
     * @param entries 坐标集合
     */
    public void batchAdd(List<GeoEntry> entries) {

        RGeo<String> geo = redissonClient.getGeo(GEO_KEY);

        geo.add(entries.toArray(new GeoEntry[0]));

        log.info("批量添加地理位置成功，size={}", entries.size());
    }

}