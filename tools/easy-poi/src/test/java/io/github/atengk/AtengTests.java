package io.github.atengk;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import io.github.atengk.style.MyExcelStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class AtengTests {

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
    public void testStyledExport() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams();
        params.setSheetName("用户数据（带样式）");

        // 设置自定义样式处理器
        params.setStyle(MyExcelStyle.class);

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "styled_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 带样式的 Excel 导出成功！路径: " + filePath);
    }

    @Test
    public void testImageExport() throws IOException {
        List<Object> imagePool = Arrays.asList(
                "D:/Temp/images/1.jpg",                               // 本地
                "https://picsum.photos/200/200",                      // 网络
                Files.readAllBytes(Paths.get("D:/Temp/images/2.png")),// byte[]
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

}
