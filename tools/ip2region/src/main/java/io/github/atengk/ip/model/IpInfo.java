package io.github.atengk.ip.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP 信息
 *
 * @author Ateng
 * @since 2026-04-09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpInfo {
    private String country;
    private String area;
    private String province;
    private String city;
    private String isp;
}