package local.ateng.java.excel.entity;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DemoData {
    // 需要合并的列
    @ExcelProperty("部门")
    private String department;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("业绩")
    private Integer performance;
}