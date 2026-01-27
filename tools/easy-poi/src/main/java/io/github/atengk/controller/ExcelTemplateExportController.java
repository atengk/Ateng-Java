package io.github.atengk.controller;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/excel/template/export")
public class ExcelTemplateExportController {

    /**
     * 模版导出Excel
     */
    @GetMapping("/simple")
    public void simple(HttpServletResponse response) {
        List<MyUser> dataList = InitData.getDataList(10);
        Map<String, Object> data = new HashMap<>();
        data.put("list", dataList);
        data.put("title", "EasyPoi 模版导出混合使用");
        data.put("author", "Ateng");
        data.put("time", DateUtil.now());
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_mix_template.xlsx",
                data
        );
        String fileName = "用户列表.xlsx";
        ExcelUtil.write(workbook, fileName, response);
    }

}
