package local.ateng.java.snailjob.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyUserVo {
    private Long id;
    private String name;
    private Integer age;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
