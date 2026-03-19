package io.github.atengk.handler;

import cn.afterturn.easypoi.excel.entity.result.ExcelVerifyHandlerResult;
import cn.afterturn.easypoi.handler.inter.IExcelVerifyHandler;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.MyUser;

import java.math.BigDecimal;

public class MyUserVerifyHandler implements IExcelVerifyHandler<MyUser> {

    @Override
    public ExcelVerifyHandlerResult verifyHandler(MyUser user) {

        // 1. 姓名必填
        if (StrUtil.isBlank(user.getName())) {
            return new ExcelVerifyHandlerResult(false, "姓名不能为空");
        }

        // 2. 年龄范围
        if (user.getAge() == null || user.getAge() < 0 || user.getAge() > 120) {
            return new ExcelVerifyHandlerResult(false, "年龄必须在 0~120 之间");
        }

        // 3. 手机号格式
        if (!StrUtil.isBlank(user.getPhoneNumber())
                && !user.getPhoneNumber().matches("^1[3-9]\\d{9}$")) {
            return new ExcelVerifyHandlerResult(false, "手机号格式不正确");
        }

        // 4. 分数范围
        if (user.getScore() != null && user.getScore().compareTo(new BigDecimal("100")) > 0) {
            return new ExcelVerifyHandlerResult(false, "分数不能大于 100");
        }

        return new ExcelVerifyHandlerResult(true);
    }
}
