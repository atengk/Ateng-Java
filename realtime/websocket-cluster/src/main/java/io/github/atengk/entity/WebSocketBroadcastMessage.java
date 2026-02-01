package io.github.atengk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketBroadcastMessage implements Serializable {
    private static final long serialVersionUID = 1L;


    private String fromNode;
    private String payload;
    private Set<String> targetUsers;
}
