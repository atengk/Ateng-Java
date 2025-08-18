package local.ateng.java.customutils.entity;

import lombok.Data;

import java.util.List;

@Data
public class MyUser2 {
    private Long id;
    private String userName;
    private String tmp;
    private List<MyUser0> myUser0List;
}
