package io.github.atengk;

import cn.hutool.core.util.RandomUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.handler.*;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelStyleUtil;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class ExportTests {

    @Test
    void testExportSimple() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_simple_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportGroup() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_group_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportMultiSheet() {
        String fileName = "target/export_multi_sheet_users.xlsx";
        try (ExcelWriter excelWriter = FesodSheet.write(fileName, MyUser.class).build()) {
            for (int i = 0; i < 5; i++) {
                WriteSheet writeSheet = FesodSheet.writerSheet(i, "用户列表" + i).build();
                excelWriter.write(InitData.getDataList(), writeSheet);
            }
        }
    }

    @Test
    void testExportImage() {
        List<MyUser> list = InitData.getDataList(5);
        String[] images = {"https://placehold.co/100x100/png?text=Ateng", "error"};
        list.forEach(item -> {
            item.setImageUrl(RandomUtil.randomEle(images));
        });
        String fileName = "target/export_image_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportMerge() {
        List<MyUser> list = InitData.getDataList();
        // 数据按照省份+城市排序
        list.sort(Comparator
                .comparing(MyUser::getProvince)
                .thenComparing(MyUser::getCity));
        String fileName = "target/export_merge_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                //.registerWriteHandler(new CellMergeHandler(0,1,2,3,4,5,6, 7, 8, 9))
                .registerWriteHandler(new CellMergeHandler())
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportDropdown() {
        String fileName = "target/export_dropdown_users.xlsx";

        Map<Integer, String[]> dropdownMap = new HashMap<>();
        dropdownMap.put(1, new String[]{"1", "2"});           // 第 2 列
        dropdownMap.put(3, new String[]{"男", "女", "未知"});  // 第 4 列

        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(new DropdownHandler(dropdownMap, 1))
                .sheet("用户列表")
                .doWrite(Collections.emptyList());
    }

    @Test
    void testExportComment() {
        String fileName = "target/export_comment_users.xlsx";

        Map<Integer, String> commentMap = new HashMap<>();
        commentMap.put(0, "请输入用户姓名，必填");
        commentMap.put(1, "请输入年龄，必须是正整数");
        commentMap.put(2, "手机号格式：11 位数字");
        commentMap.put(4, "分数范围：0 ~ 100");

        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(new CommentHandler(commentMap))
                .sheet("用户列表")
                .doWrite(Collections.emptyList());
    }

    @Test
    void testExportConverter() {
        List<MyUser> list = InitData.getDataList();
        list.forEach(item -> item.setGender(RandomUtil.randomEle(Arrays.asList(0, 1, 2))));
        String fileName = "target/export_converter_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportFreezeHead() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_freeze_head_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(new FreezeHeadHandler())
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportStyle() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_style_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(ExcelStyleUtil.getDefaultStyleStrategy())
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportCustomStyle() {
        List<MyUser> list = InitData.getDataList();

        // 默认表头字体大小（磅）
        Short DEFAULT_HEADER_FONT_SIZE = 14;
        // 默认内容字体大小（磅）
        Short DEFAULT_CONTENT_FONT_SIZE = 12;
        // 默认内容字体
        String DEFAULT_CONTENT_FONT_NAME = "微软雅黑";
        HorizontalCellStyleStrategy cellStyleStrategy = ExcelStyleUtil.buildCustomStyleStrategy(
                // 表头字体大小（单位：磅）
                DEFAULT_HEADER_FONT_SIZE,
                // 表头是否加粗
                false,
                // 表头是否斜体
                false,
                // 表头字体颜色（使用 IndexedColors 枚举值）
                IndexedColors.BLACK.getIndex(),
                // 表头字体名称
                DEFAULT_CONTENT_FONT_NAME,
                // 表头背景色（设置为浅灰色）
                IndexedColors.GREY_40_PERCENT.getIndex(),
                // 表头边框样式
                BorderStyle.DOUBLE,
                // 表头水平居中对齐
                HorizontalAlignment.CENTER,
                // 表头垂直居中对齐
                VerticalAlignment.CENTER,
                // 内容字体大小
                DEFAULT_CONTENT_FONT_SIZE,
                // 内容是否加粗
                false,
                // 内容是否斜体
                false,
                // 内容字体颜色（黑色）
                IndexedColors.BLACK.getIndex(),
                // 内容字体名称
                DEFAULT_CONTENT_FONT_NAME,
                // 内容背景色（为空表示不设置背景色）
                null,
                // 内容边框样式
                BorderStyle.DOUBLE,
                // 内容水平居中对齐
                HorizontalAlignment.CENTER,
                // 内容垂直居中对齐
                VerticalAlignment.CENTER,
                // 内容是否自动换行
                true
        );
        String fileName = "target/export_custom_style_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(cellStyleStrategy)
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportConditionStyle() {
        String fileName = "target/export_condition_style.xlsx";

        ConditionStyleHandler handler = new ConditionStyleHandler();

        // 数字判断，示例：第3列年龄 > 10000 则背景黄+字体红+加粗
        handler.addRule(1, new ConditionStyleHandler.ConditionRule(v -> {
                    if (v instanceof Number) {
                        return ((Number) v).doubleValue() > 60;
                    }
                    return false;
                }).backgroundColor(IndexedColors.YELLOW.getIndex())
                        .fontColor(IndexedColors.RED.getIndex())
                        .bold(true)
        );

        // 字符串判断
        handler.addRule(8, new ConditionStyleHandler.ConditionRule(v ->
                v instanceof String && "重庆".equals(v))
                .backgroundColor(IndexedColors.RED.getIndex())
                .fontColor(IndexedColors.WHITE.getIndex())
                .bold(true)
        );

        // 时间判断，示例：第 9 列是 LocalDateTime 类型，但 Excel 中会以 Double 存储
        handler.addRule(9, new ConditionStyleHandler.ConditionRule(v -> {
                    if (!(v instanceof Double)) {
                        return false;
                    }

                    LocalDateTime time = excelDateToLocalDateTime((Double) v);

                    // 判断逻辑
                    return time.isAfter(LocalDateTime.of(2026, 1, 26, 0, 0));
                }).backgroundColor(IndexedColors.BLUE.getIndex())
                        .fontColor(IndexedColors.GREEN.getIndex())
                        .bold(true)
        );

        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(handler)
                .sheet("用户列表")
                .doWrite(InitData.getDataList());
    }

    private static LocalDateTime excelDateToLocalDateTime(double excelDate) {
        // 25569 是 1970-01-01 和 1900-01-01 的天数差
        long epochSecond = (long) ((excelDate - 25569) * 86400);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
    }

}
