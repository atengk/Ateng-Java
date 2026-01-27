package io.github.atengk;

import cn.hutool.json.JSONUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelUtil;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FastExcel;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.support.ExcelTypeEnum;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.junit.jupiter.api.Test;

import java.util.*;

public class ExportCSVTests {

    @Test
    void testExportSimple() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_simple_users.csv";
        FesodSheet
                .write(fileName, MyUser.class)
                .csv()
                .doWrite(list);
    }

    @Test
    void testExportDynamic() {
        // 表头
        List<ExcelUtil.HeaderItem> headers = Arrays.asList(
                new ExcelUtil.HeaderItem(Collections.singletonList("姓名"), "name"),
                new ExcelUtil.HeaderItem(Collections.singletonList("年龄"), "age"),
                new ExcelUtil.HeaderItem(Collections.singletonList("登录次数"), "loginCount")
        );
        // 数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "用户" + (i + 1));
            row.put("age", 20 + i);
            row.put("loginCount", 100 + i);
            dataList.add(row);
        }
        System.out.println(JSONUtil.toJsonStr(dataList));
        // 导出
        String fileName = "target/export_dynamic_users.csv";
        FesodSheet
                .write(fileName)
                .head(ExcelUtil.buildHead(headers))
                .csv()
                .doWrite(ExcelUtil.buildRows(headers, dataList));
    }

    @Test
    void testExportBatch() {
        String fileName = "target/export_batch_users.csv";

        try (ExcelWriter excelWriter = FesodSheet
                .write(fileName, MyUser.class)
                .excelType(ExcelTypeEnum.CSV)
                .build()) {
            WriteSheet writeSheet = FastExcel.writerSheet().build();
            // 第一批数据
            excelWriter.write(InitData.getDataList(2), writeSheet);
            // 第二批数据
            excelWriter.write(InitData.getDataList(2), writeSheet);
        }
    }

}
