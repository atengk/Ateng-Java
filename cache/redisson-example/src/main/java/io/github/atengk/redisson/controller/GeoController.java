package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地理位置测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/geo")
@RequiredArgsConstructor
public class GeoController {

    private final StoreService storeService;

    /**
     * 添加门店
     *
     * curl -X POST "http://localhost:8080/geo/add?name=store1&lng=116.4&lat=39.9"
     */
    @PostMapping("/add")
    public Object add(@RequestParam String name,
                      @RequestParam Double lng,
                      @RequestParam Double lat) {

        if (ObjectUtil.hasEmpty(name, lng, lat)) {
            return "参数不能为空";
        }

        storeService.addStore(name, lng, lat);

        return "添加成功";
    }

    /**
     * 查询附近门店
     *
     * curl "http://localhost:8080/geo/nearby?lng=116.4&lat=39.9&radius=3"
     */
    @GetMapping("/nearby")
    public Object nearby(@RequestParam Double lng,
                         @RequestParam Double lat,
                         @RequestParam Double radius) {

        if (ObjectUtil.hasEmpty(lng, lat, radius)) {
            return "参数不能为空";
        }

        List<String> result = storeService.nearby(lng, lat, radius);

        return result;
    }

    /**
     * 计算距离
     *
     * curl "http://localhost:8080/geo/distance?a=store1&b=store2"
     */
    @GetMapping("/distance")
    public Object distance(@RequestParam String a,
                           @RequestParam String b) {

        if (ObjectUtil.hasEmpty(a, b)) {
            return "参数不能为空";
        }

        return storeService.distance(a, b);
    }

}
