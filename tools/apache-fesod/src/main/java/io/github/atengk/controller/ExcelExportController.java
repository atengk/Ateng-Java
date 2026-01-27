package io.github.atengk.controller;

import io.github.atengk.util.ExcelUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/excel/export")
public class ExcelExportController {

    /**
     * 动态导出 Excel（一级表头）到浏览器
     */
    @GetMapping("/dynamic")
    public void exportDynamic(HttpServletResponse response) {

        // 生成随机表头
        List<String> headers = new ArrayList<>();
        int randomInt = new Random().nextInt(20) + 1;
        for (int i = 0; i < randomInt; i++) {
            headers.add("表头" + (i + 1));
        }

        // 生成随机数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j), "数据" + (i + 1) + "-" + (j + 1));
            }
            dataList.add(row);
        }

        // 导出文件
        String fileName = "动态导出.xlsx";
        ExcelUtil.exportDynamicSimpleToResponse(response, fileName, headers, dataList, "用户列表");
    }

}
