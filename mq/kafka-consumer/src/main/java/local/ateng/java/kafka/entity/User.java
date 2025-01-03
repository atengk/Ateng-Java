package local.ateng.java.kafka.entity;

import cn.hutool.core.date.DateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private LocalDateTime createdAt;

}
