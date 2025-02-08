package local.ateng.java.excel.entity;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import local.ateng.java.excel.converter.StringUrlImageConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ContentRowHeight(100)
@HeadRowHeight(25)
@ColumnWidth(25)
public class MyImage {
    @ExcelProperty(value = "序号")
    private Integer id;
    @ExcelProperty(value = "名字")
    private String name;
    @ExcelProperty(value = "图片", converter = StringUrlImageConverter.class)
    @ColumnWidth(25)
    private String url;
}
