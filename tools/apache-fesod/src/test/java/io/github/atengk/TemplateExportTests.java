package io.github.atengk;

import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelUtil;
import org.apache.fesod.sheet.FesodSheet;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateExportTests {

    @Test
    void testTemplateExport() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");
        data.put("age", "25");

        FesodSheet
                .write("target/export_template_user_simple.xlsx")
                .withTemplate(ExcelUtil.toInputStreamFromClasspath("doc/template_user_simple.xlsx"))
                .sheet()
                .doFill(data);
    }

    @Test
    void testTemplateListExport() {
        List<MyUser> dataList = InitData.getDataList();
        Map<String, Object> data = new HashMap<>();
        data.put("list", dataList);

        FesodSheet
                .write("target/export_template_user_list.xlsx")
                .withTemplate(ExcelUtil.toInputStreamFromClasspath("doc/template_user_list.xlsx"))
                .sheet()
                .doFill(data);
    }

}
