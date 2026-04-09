package io.github.atengk.ip.controller;

import io.github.atengk.ip.model.IpInfo;
import io.github.atengk.ip.service.Ip2RegionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * IP 查询接口
 *
 * @author Ateng
 * @since 2026-04-09
 */
@RestController
public class IpController {

    private final Ip2RegionService ip2RegionService;

    public IpController(Ip2RegionService ip2RegionService) {
        this.ip2RegionService = ip2RegionService;
    }

    @GetMapping("/ip")
    public IpInfo getIp(@RequestParam String ip) {
        return ip2RegionService.search(ip);
    }
}