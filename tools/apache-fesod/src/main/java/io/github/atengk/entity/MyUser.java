package io.github.atengk.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.format.DateTimeFormat;
import org.apache.fesod.sheet.annotation.format.NumberFormat;
import org.apache.fesod.sheet.annotation.write.style.*;
import org.apache.fesod.sheet.enums.BooleanEnum;
import org.apache.fesod.sheet.enums.poi.BorderStyleEnum;
import org.apache.fesod.sheet.enums.poi.HorizontalAlignmentEnum;
import org.apache.fesod.sheet.enums.poi.VerticalAlignmentEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@HeadFontStyle(fontName = "宋体", fontHeightInPoints = 11, bold = BooleanEnum.TRUE)
@ContentFontStyle(fontName = "宋体", fontHeightInPoints = 11, bold = BooleanEnum.FALSE)
@HeadStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER, fillBackgroundColor = 9, fillForegroundColor = 9, borderLeft = BorderStyleEnum.THIN, borderRight = BorderStyleEnum.THIN, borderTop = BorderStyleEnum.THIN, borderBottom = BorderStyleEnum.THIN)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER, fillBackgroundColor = 9, fillForegroundColor = 9, borderLeft = BorderStyleEnum.THIN, borderRight = BorderStyleEnum.THIN, borderTop = BorderStyleEnum.THIN, borderBottom = BorderStyleEnum.THIN)
@HeadRowHeight(25)  // 设置表头行高
@ContentRowHeight(20)  // 设置数据内容行高
@ColumnWidth(15)       // 设置列宽
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ExcelIgnore
    private Long id;

    /**
     * 名称
     */
    @ExcelProperty(value = "名称", index = 0)
    @ColumnWidth(20) // 单独设置列宽
    private String name;

    /**
     * 年龄
     */
    @ExcelProperty(value = "年龄", index = 1)
    private Integer age;

    /**
     * 手机号码
     */
    @ExcelProperty(value = "手机号码", index = 2)
    @ColumnWidth(30) // 单独设置列宽
    private String phoneNumber;

    /**
     * 邮箱
     */
    @ExcelProperty(value = "邮箱", index = 3)
    @ColumnWidth(30) // 单独设置列宽
    private String email;

    /**
     * 分数
     */
    @ExcelProperty(value = "分数", index = 4)
    @NumberFormat(value = "#,##0.00", roundingMode = RoundingMode.HALF_UP)
    private BigDecimal score;

    /**
     * 比例
     */
    @ExcelProperty(value = "比例", index = 5)
    @NumberFormat(value = "0.00%", roundingMode = RoundingMode.HALF_UP)
    private Double ratio;

    /**
     * 生日
     */
    @ExcelProperty(value = "生日", index = 6)
    @DateTimeFormat("yyyy年MM月dd日")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @ExcelProperty(value = "所在省份", index = 7)
    private String province;

    /**
     * 所在城市
     */
    @ExcelProperty(value = "所在城市", index = 8)
    private String city;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间", index = 9)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(30) // 单独设置列宽
    private LocalDateTime createTime;

}