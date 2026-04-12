package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门店业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final GeoService geoService;

    /**
     * 添加门店
     */
    public void addStore(String name, double lng, double lat) {
        geoService.add(name, lng, lat);
    }

    /**
     * 查询附近门店
     */
    public List<String> nearby(double lng, double lat, double radius) {
        return geoService.radius(lng, lat, radius);
    }

    /**
     * 计算距离
     */
    public Double distance(String a, String b) {
        return geoService.distance(a, b);
    }

}
