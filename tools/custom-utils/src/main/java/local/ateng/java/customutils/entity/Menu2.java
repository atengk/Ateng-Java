package local.ateng.java.customutils.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Menu2 {
    private Integer id;
    private Integer parentId;
    private String name;
    private Boolean disabled;
    private List<Menu2> children;
}
