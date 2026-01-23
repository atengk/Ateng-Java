package io.github.atengk;

import cn.afterturn.easypoi.entity.BaseTypeConstants;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.export.ExcelExportService;
import cn.afterturn.easypoi.handler.inter.IWriter;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.enums.UserStatus;
import io.github.atengk.handler.NumberDataHandler;
import io.github.atengk.handler.NumberDictHandler;
import io.github.atengk.init.InitData;
import io.github.atengk.style.CustomExcelExportStyler;
import io.github.atengk.util.ExcelStyleUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ExportTests {

    @Test
    public void testSimpleExport() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 2. 配置导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 使用 EasyPoi 直接生成 Workbook
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入本地文件
        String filePath = Paths.get("target", "simple_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        // 5. 关闭 workbook（释放资源）
        workbook.close();

        System.out.println("✅ 导出成功！文件路径: " + filePath);
    }

    @Test
    public void testMultiHeaderExport() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 2. 配置导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户数据（多级表头）");

        // 3. 导出
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入文件
        String filePath = Paths.get("target", "multi_header_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 多级表头导出成功！路径: " + filePath);
    }

    @Test
    public void testSimpleMergeExport() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 数据按照省份+城市排序
        userList.sort(Comparator
                .comparing(MyUser::getProvince)
                .thenComparing(MyUser::getCity));

        // 2. 配置导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 使用 EasyPoi 生成 Workbook
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入本地文件
        String filePath = Paths.get("target", "simple_export_merge_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 导出成功！文件路径: " + filePath);
    }

    @Test
    public void testStyledExport() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams();
        params.setSheetName("用户数据（带样式）");

        // 设置自定义样式处理器
        params.setStyle(CustomExcelExportStyler.class);

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "styled_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 带样式的 Excel 导出成功！路径: " + filePath);
    }

    @Test
    public void testConditionStyledExport() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams();
        String sheetName = "用户数据（带样式）";
        params.setSheetName(sheetName);

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 条件样式
        ExcelStyleUtil.applyByTitle(workbook, 0, "身份证", 3, (wb, cell) -> {
            String value = cell.getStringCellValue();
            if (value == null || value.trim().isEmpty()) {
                CellStyle style = wb.createCellStyle();
                style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(style);
            }
        });

        String filePath = Paths.get("target", "condition_styled_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 带样式的 Excel 导出成功！路径: " + filePath);
    }

    @Test
    public void testExportWithDict() throws Exception {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams("用户列表", "sheet1");
        params.setDictHandler(new NumberDictHandler());

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "dict_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功！");
    }

    @Test
    public void testSimpleExportWithEnumField() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 随机分配状态
        UserStatus[] statuses = UserStatus.values();
        for (int i = 0; i < userList.size(); i++) {
            userList.get(i).setStatus(statuses[i % statuses.length]);
        }

        // 2. 导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 导出 Excel
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入文件
        String filePath = Paths.get("target", "simple_export_users_enum.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        workbook.close();

        System.out.println("✅ 导出成功！路径: " + filePath);
    }

    @Test
    public void testSimpleExportWithHandler() throws Exception {

        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams("用户列表", "sheet1");

        NumberDataHandler handler = new NumberDataHandler();

        // 指定要处理的字段，注意是Excel的字段名（表头）
        handler.setNeedHandlerFields(new String[]{"年龄段"});

        // 设置给导出参数
        params.setDataHandler(handler);

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "data_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    @Test
    public void testImageExport() throws IOException {
        List<Object> imagePool = Arrays.asList(
                "D:/Temp/images/1.jpg",                               // 本地
                "https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg",   // 网络
                new File("D:/Temp/images/3.jpg")                      // File
        );

        List<MyUser> userList = InitData.getDataList();
        for (int i = 0; i < userList.size(); i++) {
            userList.get(i).setImage(imagePool.get(i % imagePool.size()));
        }

        ExportParams params = new ExportParams();
        params.setSheetName("用户数据（含图片）");

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "image_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 含图片的 Excel 导出成功！路径: " + filePath);
    }


    @Test
    public void testMultiSheetExport() throws IOException {
        // 1. 获取原始数据
        List<MyUser> userList = InitData.getDataList();

        // 2. 按省份分组（保留插入顺序，用 LinkedHashMap）
        Map<String, List<MyUser>> provinceGroups = userList.stream()
                .collect(Collectors.groupingBy(
                        MyUser::getProvince,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 3. 构造多 Sheet 数据：List<Map<String, Object>>
        List<Map<String, Object>> sheets = new ArrayList<>();
        for (Map.Entry<String, List<MyUser>> entry : provinceGroups.entrySet()) {
            String sheetName = entry.getKey();
            List<MyUser> data = entry.getValue();

            ExportParams exportParams = new ExportParams();
            exportParams.setSheetName(sheetName);
            exportParams.setTitle(sheetName + " 用户数据");

            // 每个 Sheet 用一个 Map 表示：<sheetName, dataList>
            Map<String, Object> sheet = new LinkedHashMap<>();
            sheet.put("title", exportParams);       // 顶级表头 和 Sheet 名称
            sheet.put("entity", MyUser.class);     // 表头
            sheet.put("data", data);               // 数据列表
            sheets.add(sheet);
        }

        // 4. 使用多 Sheet 导出方法
        Workbook workbook = ExcelExportUtil.exportExcel(sheets, ExcelType.XSSF);

        // 5. 写入文件
        String filePath = Paths.get("target", "multi_sheet_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 多 Sheet 导出成功！共 " + sheets.size() + " 个省份，路径: " + filePath);
    }

    @Test
    public void testBigDataExport() throws IOException {
        // 1. 生成大数据
        int total = 500_000;
        System.out.println("正在生成 " + total + " 条测试数据...");
        List<MyUser> dataList = InitData.getDataList(total);

        // 2. 创建 IWriter
        ExportParams params = new ExportParams();
        params.setSheetName("大数据用户");

        // 3. 获取 writer
        IWriter<Workbook> writer = ExcelExportUtil.exportBigExcel(params, MyUser.class);

        // 4. 分批写入
        int batchSize = 1000;
        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<MyUser> batch = dataList.subList(i, end);

            writer.write(batch);

            System.out.printf("已写入 %d / %d 行%n", end, dataList.size());
        }

        // 5. 获取Workbook 并写入文件
        Workbook workbook = writer.get();
        try (FileOutputStream fos = new FileOutputStream("target/big_data_users.xlsx")) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 大数据导出成功！");
    }

    @Test
    public void testSimpleExport2() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 2. 配置导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 使用 EasyPoi 直接生成 Workbook
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入本地文件
        String filePath = Paths.get("target", "simple_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        // 5. 关闭 workbook（释放资源）
        workbook.close();

        System.out.println("✅ 导出成功！文件路径: " + filePath);
    }

    @Test
    public void testSimpleExportWithMap() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("city", user.getCity());
            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        ExcelExportEntity id = new ExcelExportEntity("ID", "id");
        id.setWidth(20);
        entityList.add(id);
        ExcelExportEntity name = new ExcelExportEntity("姓名", "name");
        name.setWidth(30);
        entityList.add(name);
        ExcelExportEntity age = new ExcelExportEntity("年龄", "age");
        age.setWidth(20);
        entityList.add(age);
        ExcelExportEntity city = new ExcelExportEntity("城市", "city");
        city.setWidth(40);
        entityList.add(city);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_users_map.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }

    @Test
    public void testSimpleMergeExportWithMap() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        userList.sort(
                Comparator.comparing(MyUser::getProvince)
                        .thenComparing(MyUser::getCity)
        );

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("city", user.getCity());
            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        ExcelExportEntity id = new ExcelExportEntity("ID", "id");
        id.setWidth(20);
        entityList.add(id);
        ExcelExportEntity name = new ExcelExportEntity("姓名", "name");
        name.setWidth(30);
        entityList.add(name);
        ExcelExportEntity age = new ExcelExportEntity("年龄", "age");
        age.setWidth(20);
        entityList.add(age);
        ExcelExportEntity city = new ExcelExportEntity("城市", "city");
        city.setWidth(40);
        city.setMergeVertical(true);
        entityList.add(city);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_merge_users_map.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }

    @Test
    public void testMultiSheetDifferentHeadersSingleMethod() throws IOException {
        // ====== 准备数据 ======
        List<MyUser> userList1 = InitData.getDataList();
        List<MyUser> userList2 = InitData.getDataList();

        // ====== Sheet1 表头 ======
        List<ExcelExportEntity> entityList1 = new ArrayList<>();
        ExcelExportEntity id1 = new ExcelExportEntity("ID", "id");
        id1.setWidth(20);
        entityList1.add(id1);
        ExcelExportEntity name1 = new ExcelExportEntity("姓名", "name");
        name1.setWidth(30);
        entityList1.add(name1);
        ExcelExportEntity age1 = new ExcelExportEntity("年龄", "age");
        age1.setWidth(20);
        entityList1.add(age1);
        ExcelExportEntity city1 = new ExcelExportEntity("城市", "city");
        city1.setWidth(40);
        entityList1.add(city1);

        // ====== Sheet1 数据 ======
        List<Map<String, Object>> dataList1 = new ArrayList<>();
        for (MyUser u : userList1) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("age", u.getAge());
            map.put("city", u.getCity());
            dataList1.add(map);
        }

        // ====== Sheet2 表头 ======
        List<ExcelExportEntity> entityList2 = new ArrayList<>();
        ExcelExportEntity id2 = new ExcelExportEntity("用户ID", "id");
        id2.setWidth(25);
        entityList2.add(id2);
        ExcelExportEntity name2 = new ExcelExportEntity("用户姓名", "name");
        name2.setWidth(35);
        entityList2.add(name2);

        // ====== Sheet2 数据 ======
        List<Map<String, Object>> dataList2 = new ArrayList<>();
        for (MyUser u : userList2) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            dataList2.add(map);
        }

        // ====== 打包多 Sheet ======
        List<Map<String, Object>> sheets = new ArrayList<>();

        Map<String, Object> sheet1 = new HashMap<>();
        sheet1.put("title", new ExportParams("完整用户列表", null, "Sheet1"));
        sheet1.put("entity", entityList1); // List<ExcelExportEntity>
        sheet1.put("data", dataList1);     // List<Map>
        sheets.add(sheet1);

        Map<String, Object> sheet2 = new HashMap<>();
        sheet2.put("title", new ExportParams("简化用户列表", null, "Sheet2"));
        sheet2.put("entity", entityList2);
        sheet2.put("data", dataList2);
        sheets.add(sheet2);

        // ====== 创建 Workbook（关键） ======
        Workbook workbook = createWorkbookForMapSheets(sheets, ExcelType.XSSF);

        // ====== 写入文件 ======
        String filePath = Paths.get("target", "multi_sheet_diff_headers.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 多 Sheet 导出成功: " + filePath);
    }

    /**
     * 多 Sheet + Map + 不同表头导出
     */
    private Workbook createWorkbookForMapSheets(List<Map<String, Object>> sheets, ExcelType type) {
        Workbook workbook = type == ExcelType.HSSF ? new HSSFWorkbook() : new XSSFWorkbook();
        for (Map<String, Object> sheetMap : sheets) {
            ExportParams params = (ExportParams) sheetMap.get("title");
            List<ExcelExportEntity> entityList = (List<ExcelExportEntity>) sheetMap.get("entity");
            Collection<?> dataSet = (Collection<?>) sheetMap.get("data");
            new ExcelExportService().createSheetForMap(workbook, params, entityList, dataSet);
        }
        return workbook;
    }

    @Test
    public void testSimpleExportWithMapAndImage() throws IOException {
        List<MyUser> userList = InitData.getDataList(10);

        // 图片 URL
        String imageUrl = "https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg";

        // ====== 转成 List<Map> ======
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("city", user.getCity());

            // 图片列：先下载成 byte[]
            byte[] imageBytes = HttpUtil.downloadBytes(imageUrl);
            map.put("avatar", imageBytes);

            dataList.add(map);
        }

        // ====== 定义表头 ======
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));
        entityList.add(new ExcelExportEntity("年龄", "age"));
        entityList.add(new ExcelExportEntity("城市", "city"));

        // 图片列
        ExcelExportEntity avatarEntity = new ExcelExportEntity("头像", "avatar");
        avatarEntity.setType(BaseTypeConstants.IMAGE_TYPE);
        avatarEntity.setHeight(100); // 高度
        avatarEntity.setWidth(100);  // 宽度
        entityList.add(avatarEntity);

        // ====== 导出 Excel ======
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        // ====== 写入文件 ======
        String filePath = Paths.get("target", "simple_export_users_map_with_image.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 导出成功: " + filePath);
    }

    @Test
    public void testSimpleExportWithMap_Dict() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());

            // 假设 number 是数字，后面用字典映射
            map.put("number", RandomUtil.randomEle(Arrays.asList(1,2,3)));

            // 假设 city 是编码，后面用 handler 处理
            map.put("city", user.getCity());

            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));

        // 年龄字典映射
        ExcelExportEntity ageEntity = new ExcelExportEntity("年龄段", "number");
        // 映射：显示值_原始值
        ageEntity.setReplace(new String[]{
                "青年_1",
                "中年_2",
                "老年_3"
        });
        entityList.add(ageEntity);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_users_map_dict.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }

    @Test
    public void testSimpleExportWithMap_DictAndDropdown() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 1. 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());

            // 假设 number 是数字，后面用字典映射
            map.put("number", RandomUtil.randomEle(Arrays.asList(1, 2, 3)));

            // 假设 city 是编码，后面用 handler 处理
            map.put("city", user.getCity());

            dataList.add(map);
        }

        // 2. 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));

        // 年龄段列，字典映射 + 下拉
        ExcelExportEntity ageEntity = new ExcelExportEntity("年龄段", "number");
        // 显示值_原始值
        ageEntity.setReplace(new String[]{
                "青年_1",
                "中年_2",
                "老年_3"
        });
        // 下拉框，根据 Replace 的值生成
        ageEntity.setAddressList(true);
        entityList.add(ageEntity);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 导出 Excel
        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        // 4. 写入本地文件
        String filePath = Paths.get("target", "simple_export_users_map_dict_dropdown.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        workbook.close();

        System.out.println("✅ 导出成功: " + filePath);
    }


    @Test
    public void testSimpleExportWithMap_DataHandler() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("number", RandomUtil.randomEle(Arrays.asList(1, 2, 3)));
            map.put("city", user.getCity());
            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));
        entityList.add(new ExcelExportEntity("年龄段", "number"));
        entityList.add(new ExcelExportEntity("城市", "city"));

        // 创建自定义 DataHandler
        NumberDataHandler handler = new NumberDataHandler();
        handler.setNeedHandlerFields(new String[]{"年龄段"}); // 注意这里写 Map 的 name

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");
        params.setDataHandler(handler); // 设置 handler

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_users_map_datahandler.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }



}
