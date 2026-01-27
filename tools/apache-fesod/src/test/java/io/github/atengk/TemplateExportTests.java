package io.github.atengk;

import cn.hutool.http.HttpUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.enums.WriteDirectionEnum;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;
import org.apache.fesod.sheet.write.metadata.fill.FillWrapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Test
    void testTemplateMultiListExport() {
        // 准备数据
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

        // 导出多列
        try (ExcelWriter writer = FesodSheet
                .write("target/export_template_multi_user_list.xlsx")
                .withTemplate(ExcelUtil.toInputStreamFromClasspath("doc/template_user_multi_list.xlsx")).build()
        ) {
            WriteSheet writeSheet = FesodSheet.writerSheet().build();
            // 使用 FillWrapper 进行多列表填充
            writer.fill(new FillWrapper("userList", userList), writeSheet);
            writer.fill(new FillWrapper("otherList", otherList), writeSheet);
        }
    }

    @Test
    void testTemplateMixExport() {
        // 准备数据
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
        HashMap<String, Object> data = new HashMap<>();
        data.put("createTime", LocalDateTime.now());
        data.put("createTimeStr", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("author", "Ateng");
        data.put("authorZh", "阿腾");

        // 导出多列
        try (ExcelWriter writer = FesodSheet
                .write("target/export_template_mix.xlsx")
                .withTemplate(ExcelUtil.toInputStreamFromClasspath("doc/template_user_mix.xlsx")).build()
        ) {
            WriteSheet writeSheet = FesodSheet.writerSheet().build();
            // 使用 FillWrapper 进行多列表填充
            writer.fill(new FillWrapper("userList", userList), writeSheet);
            writer.fill(new FillWrapper("otherList", otherList), writeSheet);
            // 填充普通变量数据
            writer.fill(data, writeSheet);
        }
    }

    // 第一个Sheet的列表数据对象
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class SalesData {
        private String productName;
        private Double amount;
        private String completionRate;
    }

    // 第二个Sheet的列表数据对象
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class SummaryData {
        private Integer rank;
        private String department;
        private Double score;
    }

    @Test
    void testTemplateMultiSheetExport() {
        // ---------- 准备第一个Sheet的数据 ----------
        // 1. 普通变量数据
        Map<String, Object> sheet1Data = new HashMap<>();
        sheet1Data.put("department", "华东大区");
        sheet1Data.put("quarter", "2025年第四季度");

        // 2. 列表数据
        List<SalesData> salesList = new ArrayList<>();
        salesList.add(new SalesData("产品A", 450.5, "112.6%"));
        salesList.add(new SalesData("产品B", 380.0, "95.0%"));
        salesList.add(new SalesData("产品C", 520.3, "130.1%"));

        // ---------- 准备第二个Sheet的数据 ----------
        // 1. 普通变量数据
        Map<String, Object> sheet2Data = new HashMap<>();
        sheet2Data.put("totalAmount", 1350.8);
        sheet2Data.put("averageRate", "112.6%");

        // 2. 列表数据
        List<SummaryData> summaryList = new ArrayList<>();
        summaryList.add(new SummaryData(1, "华东大区", 98.5));
        summaryList.add(new SummaryData(2, "华北大区", 92.0));
        summaryList.add(new SummaryData(3, "华南大区", 88.5));

        // ---------- 执行多Sheet填充 ----------
        String templatePath = "doc/template_multi_sheet.xlsx";
        String outputPath = "target/export_template_multi_sheet.xlsx";

        try (ExcelWriter writer = FesodSheet
                .write(outputPath)
                .withTemplate(ExcelUtil.toInputStreamFromClasspath(templatePath))
                .build()
        ) {
            // 1. 填充第一个Sheet (使用默认的sheet索引0或名称"Sheet1")
            WriteSheet writeSheet1 = FesodSheet.writerSheet().build(); // 默认指向第一个Sheet
            writer.fill(sheet1Data, writeSheet1); // 填充普通变量
            writer.fill(new FillWrapper("salesList", salesList), writeSheet1); // 填充列表

            // 2. 填充第二个Sheet (指定sheet名称或索引)
            // 方式一：通过索引（从0开始）
            // WriteSheet writeSheet2 = FesodSheet.writerSheet(1).build();
            // 方式二：通过名称（推荐，更清晰）
            WriteSheet writeSheet2 = FesodSheet.writerSheet("Summary").build();

            writer.fill(sheet2Data, writeSheet2); // 填充普通变量
            writer.fill(new FillWrapper("summaryList", summaryList), writeSheet2); // 填充列表

        } // try-with-resources 自动关闭 writer
    }

    @Test
    void testTemplateHorizontalExport() {
        // 准备数据
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(new HashMap<String, Object>() {{
            put("type", "类型1");
            put("count", 10);
        }});
        list.add(new HashMap<String, Object>() {{
            put("type", "类型2");
            put("count", 20);
        }});
        HashMap<String, Object> data = new HashMap<>();
        data.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("author", "Ateng");

        // 填充导出
        try (ExcelWriter writer = FesodSheet
                .write("target/export_template_horizontal.xlsx")
                .withTemplate(ExcelUtil.toInputStreamFromClasspath("doc/template_horizontal.xlsx")).build()
        ) {
            WriteSheet writeSheet = FesodSheet.writerSheet().build();
            // 使用 FillWrapper 进行多列表填充，配置为横向填充
            FillConfig fillConfig = FillConfig.builder().direction(WriteDirectionEnum.HORIZONTAL).build();
            writer.fill(new FillWrapper("list", list), fillConfig, writeSheet);
            // 填充普通变量数据
            writer.fill(data, writeSheet);
        }
    }

    @Test
    void testTemplateImage() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");

        byte[] imageBytes = HttpUtil.downloadBytes("https://placehold.co/100x100/png");
        data.put("photo", imageBytes);

        // 填充导出
        try (ExcelWriter writer = FesodSheet
                .write("target/export_template_image.xlsx")
                .withTemplate(ExcelUtil.toInputStreamFromClasspath("doc/template_image.xlsx")).build()
        ) {
            WriteSheet writeSheet = FesodSheet.writerSheet().build();
            // 填充普通变量数据
            writer.fill(data, writeSheet);
        }
    }

    @Test
    void testTemplateImageList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "User-" + i);
            byte[] imageBytes = HttpUtil.downloadBytes("https://placehold.co/100x100/png");
            row.put("photo", imageBytes);
            list.add(row);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);

        // 填充导出
        try (ExcelWriter writer = FesodSheet
                .write("target/export_template_image_list.xlsx")
                .withTemplate(ExcelUtil.toInputStreamFromClasspath("doc/template_image_list.xlsx")).build()
        ) {
            WriteSheet writeSheet = FesodSheet.writerSheet().build();
            // 使用 FillWrapper 进行多列表填充
            writer.fill(new FillWrapper("list", list), writeSheet);
        }
    }

}
