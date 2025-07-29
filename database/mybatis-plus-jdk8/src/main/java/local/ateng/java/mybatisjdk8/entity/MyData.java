package local.ateng.java.mybatisjdk8.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MyData implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String address;
    private Double score;
    private BigDecimal salary;
    private LocalDateTime dateTime;
}