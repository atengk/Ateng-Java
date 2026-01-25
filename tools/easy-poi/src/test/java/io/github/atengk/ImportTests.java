package io.github.atengk;

import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.entity.MyUser;
import io.github.atengk.entity.Student;
import io.github.atengk.handler.MyUserVerifyHandler;
import io.github.atengk.handler.NumberDataHandler;
import io.github.atengk.handler.NumberDictHandler;
import io.github.atengk.util.ExcelUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ImportTests {

    @Test
    public void testSimpleImport() {
        List<MyUser> list = ExcelUtil.importExcelFromClasspath("doc/import_simple_users.xlsx", MyUser.class);
        System.out.println("导入成功！数据: " + list);
    }

    @Test
    public void testSimpleMapImport() {
        List<Map> list = ExcelUtil.importExcelFromClasspath("doc/import_simple_users.xlsx", Map.class);
        System.out.println("导入成功！数据: " + list);
    }

    @Test
    public void testSimpleImageImport() {
        List<MyUser> list = ExcelUtil.importExcelFromClasspath("doc/import_simple_image_users.xlsx", MyUser.class);
        System.out.println("导入成功！数据: " + list);
        list.forEach(user -> {
            File file = new File(user.getAvatarUrl());
            System.out.println(StrUtil.format("姓名：{}，文件大小：{}", user.getName(), FileUtil.readBytes(file).length));
            FileUtil.del(file);
        });
    }

    @Test
    public void testSimpleReplaceImport() {
        List<MyUser> list = ExcelUtil.importExcelFromClasspath("doc/import_simple_replace_users.xlsx", MyUser.class);
        System.out.println("导入成功！数据: " + list);
    }

    @Test
    public void testSimpleEnumImport() {
        List<MyUser> list = ExcelUtil.importExcelFromClasspath("doc/import_simple_enum_users.xlsx", MyUser.class);
        System.out.println("导入成功！数据: " + list);
    }

    @Test
    public void testSimpleDictImport() {
        List<MyUser> list = ExcelUtil.importExcelFromClasspath(
                "doc/import_simple_dict_users.xlsx",
                MyUser.class,
                params -> params.setDictHandler(new NumberDictHandler())
        );
        System.out.println("导入成功！数据: " + list);
    }

    @Test
    public void testSimpleHandlerImport() {
        NumberDataHandler handler = new NumberDataHandler();
        // 指定要处理的字段，注意是Excel的字段名（表头）
        handler.setNeedHandlerFields(new String[]{"年龄段"});

        List<MyUser> list = ExcelUtil.importExcelFromClasspath(
                "doc/import_simple_handler_users.xlsx",
                MyUser.class,
                params -> params.setDataHandler(handler)
        );
        System.out.println("导入成功！数据: " + list);
    }

    @Test
    public void testImportWithErrorCollect() {
        ExcelImportResult<MyUser> result = ExcelUtil.importExcelMore(
                ExcelUtil.getInputStreamFromClasspath("doc/import_error_users.xlsx"),
                MyUser.class,
                params -> {
                    params.setNeedVerify(true);
                    params.setVerifyHandler(new MyUserVerifyHandler());
                }
        );

        System.out.println("是否存在校验失败：" + result.isVerifyFail());
        System.out.println("成功条数：" + result.getList().size());
        System.out.println("成功数据：" + result.getList());
        System.out.println("失败条数：" + result.getFailList().size());
        System.out.println("失败数据：" + result.getFailList());

        // 打印错误数据
        if (result.isVerifyFail() && result.getFailList() != null && !result.getFailList().isEmpty()) {
            result.getFailList().forEach(item -> System.out.println(StrUtil.format("第{}行，错误信息：{}", item.getRowNum(), item.getErrorMsg())));
        }

        // 把成功 Excel 导出来
        if (result.getWorkbook() != null) {
            ExcelUtil.write(result.getWorkbook(), Paths.get("target", "import_success_result.xlsx"));
            System.out.println("成功详情 Excel 已生成：target/import_success_result.xlsx");
        }
        // 把失败 Excel 导出来
        if (result.getFailWorkbook() != null) {
            ExcelUtil.write(result.getFailWorkbook(), Paths.get("target", "import_error_result.xlsx"));
            System.out.println("失败详情 Excel 已生成：target/import_error_result.xlsx");
        }
    }

    @Test
    public void testImportWithHibernateErrorCollect() {
        ExcelImportResult<MyUser> result = ExcelUtil.importExcelMore(
                ExcelUtil.getInputStreamFromClasspath("doc/import_error_users.xlsx"),
                MyUser.class,
                params -> params.setNeedVerify(true)
        );

        System.out.println("是否存在校验失败：" + result.isVerifyFail());
        System.out.println("成功条数：" + result.getList().size());
        System.out.println("成功数据：" + result.getList());
        System.out.println("失败条数：" + result.getFailList().size());
        System.out.println("失败数据：" + result.getFailList());

        // 打印错误数据
        if (result.isVerifyFail() && result.getFailList() != null && !result.getFailList().isEmpty()) {
            result.getFailList().forEach(item -> System.out.println(StrUtil.format("第{}行，错误信息：{}", item.getRowNum(), item.getErrorMsg())));
        }

        // 把成功 Excel 导出来
        if (result.getWorkbook() != null) {
            ExcelUtil.write(result.getWorkbook(), Paths.get("target", "import_success_result.xlsx"));
            System.out.println("成功详情 Excel 已生成：target/import_success_result.xlsx");
        }
        // 把失败 Excel 导出来
        if (result.getFailWorkbook() != null) {
            ExcelUtil.write(result.getFailWorkbook(), Paths.get("target", "import_error_result.xlsx"));
            System.out.println("失败详情 Excel 已生成：target/import_error_result.xlsx");
        }
    }

    @Test
    public void testImportWithMultiThread() {
        List<MyUser> list = ExcelUtil.importExcelFromClasspath(
                "doc/import_simple_users.xlsx",
                MyUser.class,
                params -> {
                    /**
                     * 开启多线程
                     */
                    params.setConcurrentTask(true);
                    /**
                     * 每个并发任务处理多少条数据
                     * 默认 1000，建议 500 ~ 2000
                     */
                    params.setCritical(500);
                }
        );
        System.out.println("导入成功！数据: " + list);
    }

    @Test
    public void testImportMultipleSheet1() {
        List<MyUser> list = ExcelUtil.importExcelFromClasspath(
                "doc/import_multi_sheet_users.xlsx",
                MyUser.class,
                params -> {
                    // 表头行数,默认1
                    params.setHeadRows(2);
                    // 开始读取的sheet位置,默认为0
                    params.setStartSheetIndex(0);
                    // 上传表格需要读取的sheet 数量,默认为1
                    params.setSheetNum(3);
                }
        );
        System.out.println("导入成功！数据条数: " + list.size());
        System.out.println("导入成功！数据: " + list);
    }

    @Test
    public void testImportMultipleSheet2() {
        String classPathExcel = "doc/import_multi_sheet.xlsx";
        // 用户列表
        List<MyUser> userList = ExcelUtil.importExcelFromClasspath(
                classPathExcel,
                MyUser.class,
                params -> params.setSheetName("用户列表")
        );
        System.out.println("导入成功！用户列表: " + userList);
        // 学生列表
        List<Student> studentList = ExcelUtil.importExcelFromClasspath(
                classPathExcel,
                Student.class,
                params -> params.setSheetName("学生列表")
        );
        System.out.println("导入成功！学生列表: " + studentList);
    }

    @Test
    public void testImportKeyValue() {
        ExcelImportResult<Map> result = ExcelUtil.importExcelMore(
                ExcelUtil.getInputStreamFromClasspath("doc/import_key_value.xlsx"),
                Map.class,
                params -> {
                    params.setSheetName("配置区");
                    params.setKeyMark(":");
                    params.setReadSingleCell(true);
                    // 从第 0 行开始，最多向下扫描 titleRows 行
                    params.setTitleRows(10);
                }
        );
        System.out.println(result.getMap());
    }

}
