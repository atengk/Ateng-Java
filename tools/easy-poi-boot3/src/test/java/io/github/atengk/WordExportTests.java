package io.github.atengk;

import cn.afterturn.easypoi.entity.ImageEntity;
import cn.hutool.http.HttpUtil;
import io.github.atengk.util.ExcelUtil;
import io.github.atengk.util.WordUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

public class WordExportTests {

    @Test
    void testWordSimpleExport() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");
        data.put("age", 25);
        XWPFDocument document = WordUtil.export(ExcelUtil.getInputStreamFromClasspath("doc/word_template_simple_export.docx"), data);
        WordUtil.write(document, "target/word_template_simple_export.docx");
    }

    @Test
    void testWordListExport() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");
        data.put("age", 25);

        // ===== 构造列表数据（循环 100 次）=====
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("time", LocalDateTime.now());
            row.put("amount", i * 1000);
            list.add(row);
        }

        data.put("list", list);

        XWPFDocument document = WordUtil.export(
                ExcelUtil.getInputStreamFromClasspath("doc/word_template_list_export.docx"),
                data
        );
        WordUtil.write(document, "target/word_template_list_export.docx");
    }

    @Test
    void testWordFormatExport() {
        // ========= 构建数据模型 =========
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");                  // 字符串字段
        data.put("age", 25);                        // 数值字段

        // 日期字段示例（支持原始值和格式化）
        data.put("createTime", new Date());         // 当前时间
        data.put("birthday", new GregorianCalendar(1999, Calendar.JUNE, 5).getTime());

        // 数值字段示例（支持原始与格式化）
        data.put("score", 89.756);                  // 原始分数
        data.put("ratio", 0.3765);                  // 比例（用于百分比格式化）

        // ========= 渲染到模板 =========
        XWPFDocument document = WordUtil.export(
                ExcelUtil.getInputStreamFromClasspath("doc/word_template_format_export.docx"),
                data
        );

        // ========= 写出到目标 =========
        WordUtil.write(document, "target/word_template_format_export.docx");
    }

    @Test
    void testWordImageExport() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");
        data.put("age", 25);
        byte[] image = HttpUtil.downloadBytes("https://placehold.co/100x100/png?text=Ateng");
        ImageEntity imageEntity = new ImageEntity(image, 100, 100);
        data.put("image", imageEntity);
        XWPFDocument document = WordUtil.export(ExcelUtil.getInputStreamFromClasspath("doc/word_template_image_export.docx"), data);
        WordUtil.write(document, "target/word_template_image_export.docx");
    }

}
