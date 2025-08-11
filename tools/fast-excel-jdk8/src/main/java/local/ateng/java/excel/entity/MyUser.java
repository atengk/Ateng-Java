package local.ateng.java.excel.entity;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import cn.idev.excel.annotation.format.NumberFormat;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentLoopMerge;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@HeadRowHeight(25)  // 设置表头行高
@ContentRowHeight(20)  // 设置数据内容行高
@ColumnWidth(15)       // 设置列宽
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ExcelProperty("主键id")
    @ColumnWidth(20) // 单独设置列宽
    private Long id;

    /**
     * 名称
     */
    @ExcelProperty("名称")
    private String name;

    /**
     * 年龄
     */
    @ExcelProperty("年龄")
    private Integer age;

    /**
     * 手机号码
     */
    @ExcelProperty("手机号码")
    private String phoneNumber;

    /**
     * 邮箱
     */
    @ExcelProperty("邮箱")
    @ColumnWidth(20) // 单独设置列宽
    private String email;

    /**
     * 分数
     */
    @ExcelProperty("分数")
    @NumberFormat("0.00")
    private BigDecimal score;

    /**
     * 比例
     */
    @ExcelProperty("比例")
    @NumberFormat("0.00%")
    private Double ratio;

    /**
     * 生日
     */
    @ExcelProperty("生日")
    @DateTimeFormat("yyyy年MM月dd日")
    @ColumnWidth(20) // 单独设置列宽
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @ExcelProperty("所在省份")
    private String province;

    /**
     * 所在省份2
     */
    @ExcelProperty("所在省份2")
    @ContentLoopMerge(eachRow = 2, columnExtend = 2)
    private String province2;

    /**
     * 所在城市
     */
    @ExcelProperty("所在城市")
    private String city;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(20) // 单独设置列宽
    private LocalDateTime createTime;

}
