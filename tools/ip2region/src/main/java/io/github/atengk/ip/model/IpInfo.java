package io.github.atengk.ip.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * IP 信息
 *
 * @author Ateng
 * @since 2026-04-09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String country;
    private String area;
    private String province;
    private String city;
    private String isp;

}