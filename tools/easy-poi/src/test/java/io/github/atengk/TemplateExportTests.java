package io.github.atengk;

import io.github.atengk.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TemplateExportTests {

    @Test
    void test() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");
        data.put("age", "25");
        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_users.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);
        System.out.println("✅ 模板导出成功：" + filePath);
    }

}
