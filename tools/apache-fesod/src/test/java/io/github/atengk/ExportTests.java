package io.github.atengk;

import cn.hutool.core.util.RandomUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.handler.CellMergeHandler;
import io.github.atengk.handler.CommentHandler;
import io.github.atengk.handler.DropdownHandler;
import io.github.atengk.init.InitData;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.junit.jupiter.api.Test;

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



}
