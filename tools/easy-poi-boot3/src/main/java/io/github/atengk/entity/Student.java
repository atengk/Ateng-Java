package io.github.atengk.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;

@Data
public class Student {
    @Excel(name = "学生姓名", width = 12, orderNum = "1")
    private String name;

    @Excel(name = "学生年龄", width = 12, orderNum = "2")
    private Integer age;
}
