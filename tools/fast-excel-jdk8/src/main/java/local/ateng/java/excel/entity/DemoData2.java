package local.ateng.java.excel.entity;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DemoData2 {
    @ExcelProperty("城市")
    private String city;

    @ExcelProperty("区域")
    private String region;

    @ExcelProperty("产品类别")
    private String category;

    @ExcelProperty("产品名称")
    private String product;

    @ExcelProperty("销量")
    private Integer sales;

    @ExcelProperty("负责人")
    private String manager;
}