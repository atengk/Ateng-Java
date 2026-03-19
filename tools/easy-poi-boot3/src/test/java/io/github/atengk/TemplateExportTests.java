package io.github.atengk;

import cn.afterturn.easypoi.entity.ImageEntity;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
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
import java.util.*;

public class TemplateExportTests {

    @Test
    void test() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");
        data.put("age", "25");
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_users.xlsx");
        ExcelUtil.write(workbook, filePath);
        System.out.println("✅ 模板导出成功：" + filePath);
    }

    @Test
    void testScanAllSheet() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");
        data.put("age", "25");
        data.put("sex", "25");
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_multiple_sheet_template.xlsx",
                data,
                params -> params.setScanAllsheet(true)
        );
        Path filePath = Paths.get("target", "template_export_multiple_sheet_users.xlsx");
        ExcelUtil.write(workbook, filePath);
        System.out.println("✅ 模板导出成功：" + filePath);
    }

    @Test
    void test2() {
        List<MyUser> dataList = InitData.getDataList();
        Map<String, Object> data = new HashMap<>();
        data.put("list", dataList);
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_list_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_list_users.xlsx");
        ExcelUtil.write(workbook, filePath);
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
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_mix_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_mix_users.xlsx");
        ExcelUtil.write(workbook, filePath);
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

        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_format_template.xlsx",
                data
        );

        Path filePath = Paths.get("target", "template_export_users_format.xlsx");
        ExcelUtil.write(workbook, filePath);

        System.out.println("✅ 普通变量格式化模板导出成功：" + filePath);
    }

    @Test
    void test5() {
        Map<String, Object> data = new HashMap<>();
        data.put("gender", 1);

        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_format_dict_template.xlsx",
                data,
                params -> params.setDictHandler(new GenderDictHandler())
        );

        Path filePath = Paths.get("target", "template_export_users_format_dict.xlsx");
        ExcelUtil.write(workbook, filePath);

        System.out.println("✅ 普通变量 + dict 格式化模板导出成功：" + filePath);
    }

    @Test
    void testListFormatTemplateExport() throws Exception {
        Map<String, Object> data = new HashMap<>();

        List<Map<String, Object>> list = new ArrayList<>();

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> u = new HashMap<>();
            u.put("name", "User-" + i);
            u.put("age", 15 + i);
            u.put("birthday", fmt.parse("199" + i + "-06-18"));
            u.put("createTime", now);
            u.put("score", 80.8923 + i);
            u.put("ratio", 0.156 + i * 0.1);
            u.put("amount", 15000.567 + i * 1000);
            list.add(u);
        }

        data.put("list", list);

        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_list_format_template.xlsx",
                data
        );

        Path filePath = Paths.get("target", "template_export_format_users_list.xlsx");
        ExcelUtil.write(workbook, filePath);

        System.out.println("📦 列表模板导出成功：" + filePath);
    }

    @Test
    void testListFormatDictTemplateExport() throws Exception {
        Map<String, Object> data = new HashMap<>();

        List<Map<String, Object>> list = new ArrayList<>();

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> u = new HashMap<>();
            u.put("name", "User-" + i);
            u.put("gender", String.valueOf(RandomUtil.randomInt(1, 3)));
            u.put("age", 15 + i);
            u.put("birthday", fmt.parse("199" + i + "-06-18"));
            u.put("createTime", now);
            u.put("score", 80.8923 + i);
            u.put("ratio", 0.156 + i * 0.1);
            u.put("amount", 15000.567 + i * 1000);
            list.add(u);
        }

        data.put("list", list);

        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_list_format_dict_template.xlsx",
                data,
                params -> params.setDictHandler(new GenderDictHandler())
        );

        Path filePath = Paths.get("target", "template_export_format_dict_users_list.xlsx");
        ExcelUtil.write(workbook, filePath);

        System.out.println("📦 列表模板导出成功：" + filePath);
    }

    @Test
    void testDynamicHeaderTemplateExport() throws Exception {
        Map<String, Object> data = new HashMap<>();

        // 动态表头
        List<Map<String, Object>> colList = new ArrayList<>();

        int monthCount = RandomUtil.randomInt(3, 8); // 随机 3~7 列

        for (int i = 0; i < monthCount; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", "2024-" + (i + 1)); // 表头名称
            colList.add(m);
        }

        data.put("colList", colList);
        System.out.println(data);

        // 导出
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/dynamic_header_template.xlsx",
                data,
                params -> params.setColForEach(true)
        );

        ExcelUtil.write(
                workbook,
                Paths.get("target/dynamic_header.xlsx")
        );

        System.out.println("📦 动态表头导出成功");
    }

    @Test
    void testDynamicHeaderMergeTemplateExport() {
        Map<String, Object> data = new HashMap<>();

        // 动态表头
        List<Map<String, Object>> colList = new ArrayList<>();

        int monthCount = RandomUtil.randomInt(3, 8); // 随机 3~7 列

        for (int i = 0; i < monthCount; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", "2024-" + (i + 1)); // 表头名称
            colList.add(m);
        }

        data.put("tempName", "总表头");
        data.put("colList", colList);

        // 导出
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/dynamic_header_merge_template.xlsx",
                data,
                params -> params.setColForEach(true)
        );

        ExcelUtil.write(
                workbook,
                Paths.get("target/dynamic_header_merge.xlsx")
        );

        System.out.println("📦 横向合并表头导出成功");
    }

    @Test
    void testDynamicHeaderAndDataTemplateExport() {

        int monthCount = RandomUtil.randomInt(3, 8);
        int rowCount = RandomUtil.randomInt(3, 6);

        List<Map<String, Object>> titles = new ArrayList<>();

        for (int i = 0; i < monthCount; i++) {
            String date = "2024-" + (i + 1);

            Map<String, Object> title = new HashMap<>();
            title.put("name", date);
            // 关键：这里不是值，是表达式
            title.put("val", "t." + date);

            titles.add(title);
        }

        List<Map<String, Object>> dataList = new ArrayList<>();

        for (int r = 0; r < rowCount; r++) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 0; i < monthCount; i++) {
                String date = "2024-" + (i + 1);
                row.put(date, i + "" + r);
            }
            dataList.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("titles", titles);
        data.put("data", dataList);


        System.out.println(data);

        // 导出
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/dynamic_header_and_data_template.xlsx",
                data,
                params -> params.setColForEach(true)
        );

        ExcelUtil.write(
                workbook,
                Paths.get("target/dynamic_header_and_data.xlsx")
        );

        System.out.println("📦 横向动态表头 + 动态数据导出成功");
    }

    @Test
    void testDynamicHeaderAndData2TemplateExport() {

        int monthCount = RandomUtil.randomInt(3, 8);
        int rowCount = RandomUtil.randomInt(3, 6);

        List<Map<String, Object>> titles = new ArrayList<>();

        for (int i = 0; i < monthCount; i++) {
            String date = "2024-" + (i + 1);

            Map<String, Object> title = new HashMap<>();
            title.put("name", date);
            // 关键：这里不是值，是表达式
            title.put("val", "t." + date);

            titles.add(title);
        }

        List<Map<String, Object>> dataList = new ArrayList<>();

        for (int r = 0; r < rowCount; r++) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 0; i < monthCount; i++) {
                String date = "2024-" + (i + 1);
                row.put(date, i + "" + r);
            }

            row.put("name", "阿腾" + r);

            dataList.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("titles", titles);
        data.put("data", dataList);
        data.put("author", "Ateng");
        data.put("tempName", "EasyPoi模版导出综合示例");

        System.out.println(data);

        // 导出
        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/dynamic_header_and_data2_template.xlsx",
                data,
                params -> params.setColForEach(true)
        );

        ExcelUtil.write(
                workbook,
                Paths.get("target/dynamic_header_and_data2.xlsx")
        );

        System.out.println("📦 横向动态表头 + 动态数据导出成功");
    }

    @Test
    void testTemplateImage() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");

        byte[] imageBytes = HttpUtil.downloadBytes("https://placehold.co/100x100/png");

        ImageEntity image = new ImageEntity();
        image.setData(imageBytes);
        image.setType(ImageEntity.Data);
        // 设置宽高
        image.setWidth(0);
        image.setHeight(0);
        image.setRowspan(2);
        image.setColspan(2);
        image.setLocationType(ImageEntity.EMBED);

        data.put("photo", image);

        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_image_template.xlsx",
                data
        );

        ExcelUtil.write(
                workbook,
                Paths.get("target/template_export_image.xlsx")
        );

        System.out.println("模板图片插入成功");
    }

    @Test
    void testTemplateListImage() {

        List<Map<String, Object>> list = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "User-" + i);

            byte[] imageBytes = HttpUtil.downloadBytes("https://placehold.co/100x100/png");

            ImageEntity image = new ImageEntity();
            image.setData(imageBytes);
            image.setType(ImageEntity.Data);
            image.setWidth(0);
            image.setHeight(0);
            image.setRowspan(2);
            image.setColspan(2);
            image.setLocationType(ImageEntity.EMBED);

            row.put("photo", image);
            list.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);

        Workbook workbook = ExcelUtil.exportExcelByTemplate(
                "doc/user_list_image_template.xlsx",
                data
        );

        ExcelUtil.write(
                workbook,
                Paths.get("target/template_export_list_image.xlsx")
        );

        System.out.println("列表模板图片插入成功");
    }

}
