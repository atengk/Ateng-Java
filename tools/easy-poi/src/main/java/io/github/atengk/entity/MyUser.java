package io.github.atengk.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.annotation.ExcelIgnore;
import io.github.atengk.enums.UserStatus;
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
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Excel(name = "用户ID", width = 15, type = 10, groupName = "基本信息", orderNum = "1")
    private Long id;

    /**
     * 名称
     */
    @Excel(name = "姓名", width = 12, groupName = "基本信息", orderNum = "2")
    private String name;

    /**
     * 年龄
     */
    @Excel(name = "年龄", width = 8, type = 10, groupName = "基本信息", orderNum = "3")
    private Integer age;

    /**
     * 手机号码
     */
    @Excel(name = "手机号", width = 15, groupName = "联系方式", orderNum = "4")
    private String phoneNumber;

    /**
     * 邮箱
     */
    @Excel(name = "邮箱", width = 20, groupName = "联系方式", orderNum = "5")
    private String email;

    /**
     * 分数
     */
    @Excel(name = "分数", width = 10, type = 10, format = "#,##0.00", groupName = "成绩信息", orderNum = "6")
    private BigDecimal score;

    /**
     * 比例
     */
    @Excel(name = "比例", width = 12, type = 10, format = "0.00000%", groupName = "成绩信息", orderNum = "7")
    private Double ratio;

    /**
     * 生日
     */
    @Excel(name = "生日", width = 12, format = "yyyy-MM-dd", groupName = "时间信息", orderNum = "8")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @Excel(name = "省份", width = 10, groupName = "地理位置", orderNum = "9", mergeVertical = true)
    private String province;

    /**
     * 所在城市
     */
    @Excel(name = "城市", width = 10, groupName = "地理位置", orderNum = "10", mergeVertical = true, mergeRely = {9})
    private String city;

    /**
     * 创建时间
     */
    @Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss", groupName = "时间信息", orderNum = "11")
    private LocalDateTime createTime;

    /**
     * 图片
     */
    //@Excel(name = "图片", type = 2, orderNum = "12", )
    @ExcelIgnore
    private Object image;

    // 1→青年 2→中年 3→老年
//    @Excel(name = "年龄段", dict = "ageDict", addressList = true)
    //@Excel(name = "年龄段")
    @ExcelIgnore
    private Integer number;

    /**
     * 用户状态
     * enumExportField: 导出 Excel 显示哪个字段
     * enumImportMethod: 导入 Excel 时通过静态方法将值转换为枚举
     */
    //@Excel(name = "状态", enumExportField = "name", enumImportMethod = "getByName")
    @ExcelIgnore
    private UserStatus status;
}
