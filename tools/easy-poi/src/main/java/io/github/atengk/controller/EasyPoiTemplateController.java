package io.github.atengk.controller;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.hutool.core.thread.ThreadUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * EasyPOI 模板导出测试 Controller
 * 仅用于验证 Spring Boot 3 + EasyPOI 是否能正常工作
 */
@RestController
public class EasyPoiTemplateController {


    @GetMapping("/easypoi/export")
    public void export(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        ExcelUtil.write(workbook, "用户列表", response);

        ThreadUtil.sleep(20000);
    }

}
