package io.github.atengk.task.dto;

import lombok.Data;

@Data
public class OrderDTO {

    private Long orderId;
    private String userName;
    private Double amount;
}
