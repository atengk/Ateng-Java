package local.ateng.java.customutils.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MyUser1 {
    private Long id;
    private String userName;
    private LocalDate today;
    private LocalDateTime createTime;
    private MyUser0 myUser0;
    private List<MyUser0> myUser0List;
}
