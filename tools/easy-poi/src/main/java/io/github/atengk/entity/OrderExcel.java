package io.github.atengk.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.annotation.ExcelEntity;
import lombok.Data;

@Data
public class OrderExcel {

    @Excel(name = "订单编号", width = 18, orderNum = "1")
    private String orderNo;

    @ExcelEntity(name = "收件人信息")
    private Receiver receiver;
}