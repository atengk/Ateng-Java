package io.github.atengk.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.annotation.ExcelCollection;
import lombok.Data;

import java.util.List;

@Data
public class CourseExcel {
    @Excel(name = "课程名称", width = 15, needMerge = true, orderNum = "1")
    private String courseName;

    @ExcelCollection(name = "学生列表", orderNum = "4")
    private List<Student> students;
}
