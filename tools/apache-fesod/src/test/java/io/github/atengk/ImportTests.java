package io.github.atengk;

import io.github.atengk.entity.MyUser;
import io.github.atengk.entity.Other;
import io.github.atengk.listener.*;
import io.github.atengk.util.ExcelUtil;
import org.apache.fesod.sheet.ExcelReader;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.read.metadata.ReadSheet;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportTests {

    @Test
    public void testImportEntitySimple() {
        List<MyUser> list = FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_simple_users.xlsx"))
                .head(MyUser.class)
                .sheet()
                .doReadSync();
        System.out.println(list);
        System.out.println(list.get(0).getName());
    }

    @Test
    public void testImportMapSimple() {
        // key 是列索引，value 是单元格内容
        List<Map<Integer, String>> list = FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_simple_users.xlsx"))
                .sheet()
                .doReadSync();
        System.out.println(list);
    }

    @Test
    public void testImportValidation() {
        List<MyUser> list = FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_error_users.xlsx"), new ValidationUserListener())
                .head(MyUser.class)
                .sheet()
                .doReadSync();
        System.out.println(list);
        System.out.println(list.get(0).getName());
    }

    @Test
    public void testImportValidationAll() {
        ValidationAllUserListener listener = new ValidationAllUserListener();
        FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_error_users.xlsx"), listener)
                .head(MyUser.class)
                .sheet()
                .doRead();
        // 获取效验数据
        List<MyUser> successList = listener.getSuccessList();
        List<String> errorList = listener.getErrorList();
        boolean hasError = listener.hasError();
        // 错误信息
        if (hasError) {
            System.out.println("错误信息：" + errorList);
        }
        // 正确数据
        System.out.println("正确数据：" + successList);
    }

    @Test
    public void testImportImage() {
        List<MyUser> list = FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_image_users.xlsx"))
                .head(MyUser.class)
                .sheet()
                .doReadSync();
        System.out.println(list);
        System.out.println(list.get(0).getName());
    }

    @Test
    public void testImportImagePoi() {
        // 1. 先用 Fesod 读取结构化数据
        List<MyUser> list = FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_image_users.xlsx"))
                .head(MyUser.class)
                .sheet()
                .doReadSync();

        // 2. 再用 POI 读取图片
        Map<Integer, String> imageMap = readImages(
                ExcelUtil.toInputStreamFromClasspath("doc/import_image_users.xlsx"), 10);

        // 3. 把图片 URL 填充回 MyUser
        for (int i = 0; i < list.size(); i++) {
            MyUser user = list.get(i);
            String imagePath = imageMap.get(i + 1);
            // 注意：通常 Excel 第一行是表头，所以图片行号要 +1
            user.setImageUrl(imagePath);
        }

        // 4. 验证结果
        System.out.println(list);
        System.out.println(list.get(0).getName());
        System.out.println(list.get(0).getImageUrl());
    }

    /**
     * 读取 Excel 中的所有图片
     *
     * @param inputStream Excel 输入流
     * @param columnIndex 图片所在列
     * @return key = 行号(0-based)，value = 图片文件路径（或 URL）
     */
    public static Map<Integer, String> readImages(InputStream inputStream, int columnIndex) {
        Map<Integer, String> imageMap = new HashMap<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            if (!(sheet instanceof XSSFSheet)) {
                return imageMap;
            }

            XSSFSheet xssfSheet = (XSSFSheet) sheet;
            XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
            if (drawing == null) {
                return imageMap;
            }

            for (XSSFShape shape : drawing.getShapes()) {
                if (!(shape instanceof XSSFPicture)) {
                    continue;
                }

                XSSFPicture picture = (XSSFPicture) shape;
                XSSFClientAnchor anchor = picture.getPreferredSize();

                int row = anchor.getRow1();
                int col = anchor.getCol1();

                // 只处理“图片列”的图片
                if (col != columnIndex) {
                    continue;
                }

                XSSFPictureData pictureData = picture.getPictureData();
                byte[] bytes = pictureData.getData();
                String ext = pictureData.suggestFileExtension();

                String fileName = "img_" + row + "." + ext;
                File file = new File("target/excel-images/" + fileName);
                file.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(bytes);
                }

                imageMap.put(row, file.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new RuntimeException("读取 Excel 图片失败", e);
        }

        return imageMap;
    }


    @Test
    public void testImportConverter() {
        List<MyUser> list = FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_converter_users.xlsx"))
                .head(MyUser.class)
                .sheet()
                .doReadSync();
        System.out.println(list);
        System.out.println(list.get(0).getName());
    }

    @Test
    public void testImportListener() {
        FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_listener_users.xlsx"))
                .head(MyUser.class)
                .registerReadListener(new MyUserBatchReadListener())
                .sheet()
                .doRead();
    }

    @Test
    public void testImportMultiSheet() {
        MyUserReadListener userListener = new MyUserReadListener();
        OtherReadListener otherListener = new OtherReadListener();

        try (ExcelReader excelReader = FesodSheet
                .read(ExcelUtil.toInputStreamFromClasspath("doc/import_multi_sheets.xlsx"))
                .build()) {

            ReadSheet readSheet1 = FesodSheet
                    .readSheet(0)
                    .head(MyUser.class)
                    .registerReadListener(userListener)
                    .build();

            ReadSheet readSheet2 = FesodSheet
                    .readSheet("其他数据")
                    .head(Other.class)
                    .registerReadListener(otherListener)
                    .build();

            excelReader.read(readSheet1, readSheet2);
            excelReader.finish();
        }

        // 读取结果
        System.out.println("Sheet0 用户数据：");
        System.out.println(userListener.getDataList());

        System.out.println("“其他数据” Sheet 数据：");
        System.out.println(otherListener.getDataList());
    }


}
