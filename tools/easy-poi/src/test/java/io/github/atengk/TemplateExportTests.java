package io.github.atengk;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.handler.GenderDictHandler;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    @Test
    void test2() {
        List<MyUser> dataList = InitData.getDataList();
        Map<String, Object> data = new HashMap<>();
        data.put("list", dataList);
        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_list_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_list_users.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);
        System.out.println("✅ 模板导出成功：" + filePath);
    }

    @Test
    void test3() {
        List<MyUser> dataList = InitData.getDataList(10);
        Map<String, Object> data = new HashMap<>();
        data.put("list", dataList);
        data.put("title", "EasyPoi 模版导出混合使用");
        data.put("author", "Ateng");
        data.put("time", DateUtil.now());
        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_mix_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_mix_users.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);
        System.out.println("✅ 模板导出成功：" + filePath);
    }

    @Test
    void test4() throws ParseException {
        Map<String, Object> data = new HashMap<>();

        Date date = new Date();
        Date formatDate = new SimpleDateFormat("yyyy-MM-dd").parse("1999-06-18");

        data.put("name", "Ateng");
        data.put("age", 25);
        data.put("createTime", date);
        data.put("birthday", formatDate);
        data.put("score", 87.456);
        data.put("ratio", 0.8567);

        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_format_template.xlsx",
                data
        );

        Path filePath = Paths.get("target", "template_export_users_format.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);

        System.out.println("✅ 普通变量格式化模板导出成功：" + filePath);
    }

    @Test
    void test5() {
        Map<String, Object> data = new HashMap<>();
        data.put("gender", 1);

        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_format_dict_template.xlsx",
                data,
                params -> params.setDictHandler(new GenderDictHandler())
        );

        Path filePath = Paths.get("target", "template_export_users_format_dict.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);

        System.out.println("✅ 普通变量 + dict 格式化模板导出成功：" + filePath);
    }

}
