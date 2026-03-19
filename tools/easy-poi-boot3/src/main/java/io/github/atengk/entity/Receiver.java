package io.github.atengk.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;

@Data
public class Receiver {

    @Excel(name = "收件人姓名", width = 15, orderNum = "2")
    private String name;

    @Excel(name = "联系电话", width = 15, orderNum = "3")
    private String phone;

    @Excel(name = "收件城市", width = 15, orderNum = "4")
    private String city;
}