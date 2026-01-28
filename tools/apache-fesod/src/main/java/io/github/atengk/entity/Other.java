package io.github.atengk.entity;

import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelProperty;

@Data
public class Other {
    /**
     * 名称
     */
    @ExcelProperty(value = "名称", index = 0)
    private String name;

    /**
     * 年龄
     */
    @ExcelProperty(value = "年龄", index = 1)
    private Integer age;

}
