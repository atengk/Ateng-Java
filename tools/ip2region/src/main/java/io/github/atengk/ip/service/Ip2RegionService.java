package io.github.atengk.ip.service;

import io.github.atengk.ip.model.IpInfo;

/**
 * IP 归属地服务
 *
 * @author Ateng
 * @since 2026-04-09
 */
public interface Ip2RegionService {

    /**
     * 查询 IP 归属地
     *
     * @param ip IP 地址
     * @return 结构化信息
     */
    IpInfo search(String ip);

    /**
     * 查询原始字符串
     */
    String searchRaw(String ip);
}