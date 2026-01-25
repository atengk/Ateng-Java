package io.github.atengk.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.annotation.ExcelIgnore;
import cn.afterturn.easypoi.handler.inter.IExcelDataModel;
import cn.afterturn.easypoi.handler.inter.IExcelModel;
import io.github.atengk.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUser implements Serializable, IExcelModel, IExcelDataModel {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Excel(name = "用户ID", width = 15, type = 10, orderNum = "1")
    private Long id;

    /**
     * 名称
     */
    @Excel(name = "姓名", width = 12, orderNum = "2")
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 年龄
     */
    @Excel(name = "年龄", width = 8, type = 10, orderNum = "3")
    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于 0")
    @Max(value = 120, message = "年龄不能大于 120")
    private Integer age;

    /**
     * 手机号码
     */
    @Excel(name = "手机号", width = 15, orderNum = "4")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    /**
     * 邮箱
     */
    @Excel(name = "邮箱", width = 20, orderNum = "5")
    private String email;

    /**
     * 分数
     */
    @Excel(name = "分数", width = 10, type = 10, format = "#,##0.00", orderNum = "6")
    @NotNull(message = "分数不能为空")
    @Min(value = 0, message = "分数不能小于 0")
    @Max(value = 100, message = "分数不能大于 100")
    private BigDecimal score;

    /**
     * 比例
     */
    @Excel(name = "比例", width = 12, type = 10, format = "0.00000%", orderNum = "7")
    private Double ratio;

    /**
     * 生日
     */
    @Excel(name = "生日", width = 12, format = "yyyy-MM-dd", orderNum = "8")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @Excel(name = "省份", width = 10, orderNum = "9")
    private String province;

    /**
     * 所在城市
     */
    @Excel(name = "城市", width = 10, orderNum = "10")
    private String city;

    /**
     * 创建时间
     */
    @Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss", orderNum = "11")
    private LocalDateTime createTime;

    /**
     * 图片
     */
//    @Excel(name = "图片", type = 2, width = 15, height = 30, orderNum = "12")
    @ExcelIgnore
    private Object image;

    /**
     * 图片
     */
//    @Excel(name = "图片", type = 2, savePath = "target/", width = 15, height = 30, orderNum = "12")
    @ExcelIgnore
    private String avatarUrl;

    // 1→青年 2→中年 3→老年
//    @Excel(name = "年龄段", dict = "ageDict", addressList = true)
//    @Excel(name = "年龄段")
//    @Excel(name = "年龄段", replace = {"青年_1", "中年_2", "老年_3"})
    @ExcelIgnore
//    @Excel(name = "年龄段", dict = "ageDict")
    private Integer number;

    /**
     * 用户状态
     * enumExportField: 导出 Excel 显示哪个字段
     * enumImportMethod: 导入 Excel 时通过静态方法将值转换为枚举
     */
//    @Excel(name = "状态", enumExportField = "name", enumImportMethod = "getByName")
    @ExcelIgnore
    private UserStatus status;

    @ExcelIgnore
    private String errorMsg;

    /**
     * 获取错误数据
     *
     * @return 错误数据
     */
    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * 设置错误信息
     *
     * @param errorMsg 错误数据
     */
    @Override
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @ExcelIgnore
    private Integer rowNum;

    /**
     * 获取行号
     *
     * @return 数据行号
     */
    @Override
    public Integer getRowNum() {
        return rowNum;
    }

    /**
     * 设置行号
     *
     * @param rowNum 数据行号
     */
    @Override
    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }
}
