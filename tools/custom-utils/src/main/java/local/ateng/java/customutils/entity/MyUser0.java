package local.ateng.java.customutils.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MyUser0 {
    @NotNull
    private Long id;
    @NotBlank
    private String userName;
    private LocalDate today;
    private LocalDateTime createTime;
}
