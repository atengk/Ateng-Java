package local.ateng.java.excel;

import cn.idev.excel.EasyExcel;
import local.ateng.java.excel.entity.DemoData;
import local.ateng.java.excel.entity.DemoData2;
import local.ateng.java.excel.entity.MyUser;
import local.ateng.java.excel.handler.AutoMergeStrategy;
import local.ateng.java.excel.handler.CellMergeStrategy;
import local.ateng.java.excel.handler.RowColMergeStrategy;
import local.ateng.java.excel.init.InitData;
import local.ateng.java.excel.utils.ExcelStyleUtil;
import local.ateng.java.excel.utils.ExcelUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ExcelUtilTests {

    @Test
    void test1() throws IOException {
        List<MyUser> dataList = InitData.getDataList();

        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test1.xlsx"))) {
            ExcelUtil.writeToOutputStream(dataList, MyUser.class, out);
        }

    }

    @Test
    void test11() throws IOException {
        // 1. 生成多个 sheet 的数据
        List<List<MyUser>> allDataList = new ArrayList<>();
        List<String> sheetNameList = new ArrayList<>();

        int count = 3;
        for (int i = 1; i <= count; i++) {
            List<MyUser> dataList = InitData.getDataList(); // 假设每次都生成一份数据
            allDataList.add(dataList);
            sheetNameList.add("用户Sheet" + i);
        }

        // 2. 写入 Excel
        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test11.xlsx"))) {
            ExcelUtil.writeToOutputStream(allDataList, MyUser.class, sheetNameList, out);
        }
    }

    @Test
    void test13() throws IOException {
        List<MyUser> dataList = InitData.getDataList();

        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test13.xlsx"))) {
            ExcelUtil.writeToOutputStream(dataList, MyUser.class, out, ExcelStyleUtil.getDefaultStyleStrategy());
        }

    }

    @Test
    void test133() throws IOException {
        List<MyUser> dataList = InitData.getDataList();

        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test133.xlsx"))) {
            ExcelUtil.writeToOutputStream(dataList, MyUser.class, out, ExcelStyleUtil.zebraStripeHandler());
        }

    }

    @Test
    void test2() throws IOException {
        List<MyUser> dataList = InitData.getDataList();
        ExcelUtil.writeToFile(dataList, MyUser.class, "D:/Temp/excel/test2.xlsx");
    }

    @Test
    void test22() throws IOException {
        // 1. 生成多个 sheet 的数据
        List<List<MyUser>> allDataList = new ArrayList<>();
        List<String> sheetNameList = new ArrayList<>();

        int count = 3;
        for (int i = 1; i <= count; i++) {
            List<MyUser> dataList = InitData.getDataList(); // 假设每次都生成一份数据
            allDataList.add(dataList);
            sheetNameList.add("用户Sheet" + i);
        }

        // 2. 写入 Excel
        ExcelUtil.writeToFile(allDataList, MyUser.class, sheetNameList, "D:/Temp/excel/test22.xlsx");

    }

    @Test
    void test3() throws IOException {
        List<String> headerList = Arrays.asList("姓名", "年龄", "部门");
        List<List<Object>> dataList = Arrays.asList(
                Arrays.asList("张三", "25", "技术部"),
                Arrays.asList("李四", "30", "市场部")
        );

        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test2.xlsx"))) {
            ExcelUtil.writeWithSimpleHeader(out, headerList, dataList);
        }

    }

    @Test
    void testWriteImage() throws IOException {
        // 1. 表头
        List<String> headerList = Arrays.asList("姓名", "年龄", "头像");

        // 2. 读取本地图片
        byte[] imageBytes = Files.readAllBytes(Paths.get("C:\\Users\\admin\\Pictures\\Saved Pictures\\通知.jpeg"));

        // 3. 转 Base64 （只支持二级制的图片数据）
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 4. 组装数据（注意 List<List<Object>>）
        List<List<Object>> dataList = new ArrayList<>();
        dataList.add(Arrays.asList("张三", 25, imageBytes));     // 用 byte[] 写图片
        dataList.add(Arrays.asList("王五", 28, "市场部"));      // 仍然支持普通字符串

        // 5. 写入 Excel
        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test-image.xlsx"))) {
            ExcelUtil.writeWithSimpleHeader(out, headerList, dataList);
        }
    }


    @Test
    void test4() throws IOException {
        // ================== 表头定义 ==================
        // headerList 是一个二维列表，用于定义每一列的“多级表头”
        // 外层 List 表示每一列，内层 List 表示该列从上到下的多级标题

        // 本例构造如下表头结构：
        // | 人员信息 | 人员信息 |
        // | 姓名     | 年龄     |
        // 也就是两列，两级表头，"人员信息" 为父级表头，"姓名"、"年龄" 为子级表头
        List<List<String>> headerList = Arrays.asList(
                Arrays.asList("人员信息", "姓名"),  // 第1列：从上到下表头为“人员信息” → “姓名”
                Arrays.asList("人员信息", "年龄")   // 第2列：从上到下表头为“人员信息” → “年龄”
        );

        // ================== 数据定义 ==================
        // 每一个内层 List 代表一行数据，对应 headerList 的每一列
        // 必须保证每行的列数和 headerList 的列数一致
        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("张三", "25"),
                Arrays.asList("李四", "30")
        );

        // ================== 写入 Excel 文件 ==================
        // 使用工具类 ExcelUtil 中的 writeMultiLevelHeader 方法写出多级表头的数据
        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test3.xlsx"))) {
            ExcelUtil.writeWithMultiLevelHeader(out, headerList, dataList);
        }
    }

    @Test
    void test41() throws IOException {
        // ================== 表头定义 ==================
        // headerList 是一个二维列表，用于定义每一列的“多级表头”
        // 外层 List 表示每一列，内层 List 表示该列从上到下的多级标题

        // 本例构造如下表头结构：
        // | 人员信息 | 人员信息 |
        // | 姓名     | 年龄     |
        // 也就是两列，两级表头，"人员信息" 为父级表头，"姓名"、"年龄" 为子级表头
        List<List<String>> headerList = Arrays.asList(
                Arrays.asList("人员信息", "人员信息"),
                Arrays.asList("姓名", "年龄")
        );
        // 转换表头
        headerList = ExcelUtil.buildMultiLevelHeader(headerList);

        // ================== 数据定义 ==================
        // 每一个内层 List 代表一行数据，对应 headerList 的每一列
        // 必须保证每行的列数和 headerList 的列数一致
        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("张三", "25"),
                Arrays.asList("李四", "30")
        );

        // ================== 写入 Excel 文件 ==================
        // 使用工具类 ExcelUtil 中的 writeMultiLevelHeader 方法写出多级表头的数据
        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test3.xlsx"))) {
            ExcelUtil.writeWithMultiLevelHeader(out, headerList, dataList, ExcelStyleUtil.defaultRowHeightStrategy());
        }
    }

    @Test
    void testOneLevelSimpleHeader() throws IOException {
        // 只有一级表头，headerList 中每列只包含一个字符串
        List<List<String>> headerList = Arrays.asList(
                Collections.singletonList("姓名"),
                Collections.singletonList("年龄"),
                Collections.singletonList("城市")
        );

        // 数据部分
        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("张三", "25", "上海"),
                Arrays.asList("李四", "30", "北京")
        );

        // 写入文件
        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test_one_level.xlsx"))) {
            ExcelUtil.writeWithMultiLevelHeader(out, headerList, dataList);
        }
    }

    @Test
    void testThreeLevelHeader() throws IOException {
        // 构造多级表头：学生成绩 → 期中/期末 → 科目
        List<List<String>> headerList = Arrays.asList(
                Arrays.asList("学生成绩", "期中", "数学"),
                Arrays.asList("学生成绩", "期中", "语文"),
                Arrays.asList("学生成绩", "期末", "数学")
        );

        // 构造数据，每行 3 列，对应上面的表头顺序
        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("80", "85", "90"),
                Arrays.asList("70", "88", "95")
        );

        // 写入 Excel 文件
        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test_three_level.xlsx"))) {
            ExcelUtil.writeWithMultiLevelHeader(out, headerList, dataList);
        }
    }

    @Test
    void testUnbalancedHeader() throws IOException {
        // 构造表头：
        // 第1列：基本信息 → 姓名（2级）
        // 第2列：年龄（1级）
        // 第3列：地址（1级）
        List<List<String>> headerList = Arrays.asList(
                Arrays.asList("基本信息", "姓名"),
                Arrays.asList("年龄"),
                Arrays.asList("地址")
        );

        // 构造数据，每行 3 列
        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("张三", "25", "上海"),
                Arrays.asList("李四", "30", "北京")
        );

        // 写入 Excel 文件
        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test_unbalanced_header.xlsx"))) {
            ExcelUtil.writeWithMultiLevelHeader(out, headerList, dataList);
        }
    }

    @Test
    void testCompanyFinanceHeader() throws IOException {
        // 构造表头：
        // 前两列属于“公司信息”，后两列属于“财务信息”
        List<List<String>> headerList = Arrays.asList(
                Arrays.asList("公司信息", "名称"),
                Arrays.asList("公司信息", "地区"),
                Arrays.asList("财务信息", "营收"),
                Arrays.asList("财务信息", "利润")
        );

        // 构造数据，每行 4 列
        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("A公司", "华东", "1000万", "200万"),
                Arrays.asList("B公司", "华南", "1500万", "300万")
        );

        // 写入 Excel 文件
        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test_company_finance.xlsx"))) {
            ExcelUtil.writeWithMultiLevelHeader(out, headerList, dataList);
        }
    }

    @Test
    void testWriteSuperComplexMultiLevelHeader() throws IOException {
        // 构建多级表头：三层结构，模拟一个复杂报表结构
        List<List<String>> headerList = Arrays.asList(
                Arrays.asList("项目情况", "人员信息", "姓名"),
                Arrays.asList("项目情况", "人员信息", "年龄"),
                Arrays.asList("项目情况", "人员信息", "岗位"),
                Arrays.asList("项目情况", "绩效信息", "绩效评分"),
                Arrays.asList("项目情况", "绩效信息", "绩效等级"),
                Arrays.asList("项目情况", "联系方式", "电话"),
                Arrays.asList("项目情况", "联系方式", "邮箱")
        );

        /*
         * 说明：
         * 每个 List<String> 表示一列的表头结构，从左到右依次是表头的多级层级。
         * 比如 ["项目情况", "人员信息", "姓名"] 表示该列的表头为：
         * ┌──────┬────────┐
         * │ 项目情况       │
         * │      ┌──────┬─┴─────┐
         * │      │ 人员信息     │
         * │      │     ┌───┬────┐
         * │      │     │ 姓名 │ 年龄 │ ...
         */

        // 构建数据内容：与 headerList 列数对齐
        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("张三", "28", "工程师", "85", "A", "13800138000", "zhangsan@example.com"),
                Arrays.asList("李四", "32", "产品经理", "92", "A+", "13900139000", "lisi@example.com"),
                Arrays.asList("王五", "26", "测试", "78", "B", "13700137000", "wangwu@example.com")
        );

        // 输出路径
        Path path = Paths.get("D:/Temp/excel/test-super-complex.xlsx");

        try (OutputStream out = Files.newOutputStream(path)) {
            ExcelUtil.writeWithMultiLevelHeader(out, headerList, dataList);
        }
    }

    @Test
    void testVerticalRepeatData() throws IOException {
        // 构建多级表头，例如“部门”列数据重复
        List<List<String>> headerList = Arrays.asList(
                Arrays.asList("人员信息", "人员信息", "联系方式", "联系方式"),
                Arrays.asList("姓名", "姓名", "电话", "老年电话"),
                Arrays.asList("姓名", "姓名", "手机", "手机")
        );


        // 数据中“部门”列有重复值
        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("张三", "张三", "13800138000"),
                Arrays.asList("李四", "李四", "13900139000")
        );

        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test_vertical_repeat.xlsx"))) {
            ExcelUtil.writeWithMultiLevelHeader(out, headerList, dataList);
        }
    }

    @Test
    void test20250811_02() throws IOException {
        List<List<String>> headerList = Arrays.asList(
                Arrays.asList("人员信息", "姓名"),
                Arrays.asList("人员信息", "年龄")
        );

        List<List<String>> dataList = Arrays.asList(
                Arrays.asList("张三", "25"),
                Arrays.asList("李四", "30"),
                Arrays.asList("李四", "31"),
                Arrays.asList("张三", "25"),
                Arrays.asList("李四", "30"),
                Arrays.asList("李四", "李四"),
                Arrays.asList("李四", "李四"),
                Arrays.asList("王五", "31")
        );

        int headerRows = headerList.get(0).size(); // 这里为 2（多级表头高度）
        int[] mergeCols = new int[]{0}; // 合并第一列（姓名）

        try (OutputStream out = Files.newOutputStream(Paths.get("D:/Temp/excel/test20250811_01_fixed.xlsx"))) {
            EasyExcel.write(out)
                    .head(headerList)
                    .registerWriteHandler(new AutoMergeStrategy(mergeCols, headerRows, dataList))
                    .sheet("Sheet1")
                    .doWrite(dataList);
        }
    }

    @Test
    void cellMerge() {
        // 1. 准备数据
        List<DemoData> dataList = Arrays.asList(
                new DemoData("研发部", "研发部", 100),
                new DemoData("研发部", "研发部", 200),
                new DemoData("市场部", "王五", 150),
                new DemoData("市场部", "赵六", 300)
        );

        // 2. 创建合并策略（自动识别表头，合并第0,1,2列）
        CellMergeStrategy mergeStrategy = new CellMergeStrategy(0, 1, 2);

        // 3. 执行导出
        EasyExcel.write("D:\\temp\\202508\\部门业绩表.xlsx", DemoData.class)
                .registerWriteHandler(mergeStrategy)
                .sheet("部门业绩")
                .doWrite(dataList);
    }

    @Test
    void testMultiColumnMerge() {
        List<DemoData2> dataList = Arrays.asList(
                // 北京-朝阳区-电子产品-手机，连续两行完全相同，姓名不同
                new DemoData2("北京", "北京", "电子产品", "手机", 1200, "张三"),
                new DemoData2("北京", "北京", "电子产品", "手机", 800, "李四"),

                // 北京-朝阳区-电子产品-平板，连续两行完全相同
                new DemoData2("北京", "北京", "电子产品", "平板", 500, "李四"),
                new DemoData2("北京", "朝阳区", "电子产品", "平板", 600, "李四"),

                // 北京-海淀区-电子产品-手机，连续两行完全相同
                new DemoData2("北京", "海淀区", "电子产品", "手机", 900, "王五"),
                new DemoData2("北京", "海淀区", "电子产品", "手机", 1100, "王五"),

                // 上海-浦东新区-家电-电视，连续两行完全相同
                new DemoData2("上海", "浦东新区", "家电", "电视", 1500, "赵六"),
                new DemoData2("上海", "浦东新区", "家电", "电视", 1200, "赵六"),

                // 上海-徐汇区-家电-冰箱，连续两行完全相同
                new DemoData2("上海", "徐汇区", "家电", "冰箱", 800, "钱七"),
                new DemoData2("上海", "徐汇区", "家电", "冰箱", 700, "钱七")
        );

        String fileName = "D:\\temp\\202508\\销售数据报表.xlsx";
        EasyExcel.write(fileName, DemoData2.class)
                .registerWriteHandler(new RowColMergeStrategy(0, 5))
                .sheet("销售数据")
                .doWrite(dataList);

        System.out.println("Excel导出完成：" + fileName);
    }


}
