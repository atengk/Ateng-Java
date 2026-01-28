package io.github.atengk.controller;

import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        // 准备数据
        // 列表变量
        List<MyUser> userList = InitData.getDataList(2);
        List<Map<String, Object>> otherList = new ArrayList<>();
        otherList.add(new HashMap<String, Object>() {{
            put("type", "类型1");
            put("count", 10);
        }});
        otherList.add(new HashMap<String, Object>() {{
            put("type", "类型2");
            put("count", 20);
        }});
        Map<String, List<?>> listMap = new HashMap<>();
        listMap.put("userList", userList);
        listMap.put("otherList", otherList);
        // 普通变量
        HashMap<String, Object> data = new HashMap<>();
        data.put("createTime", LocalDateTime.now());
        data.put("createTimeStr", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("author", "Ateng");
        data.put("authorZh", "阿腾");

        // 导出多列
        ExcelUtil.exportExcelTemplateToResponse(
                response,
                "用户列表.xlsx",
                ExcelUtil.toInputStreamFromClasspath("doc/template_user_mix.xlsx"),
                data,
                listMap
                );
    }

}
