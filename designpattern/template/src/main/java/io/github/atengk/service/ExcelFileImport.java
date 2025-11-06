package io.github.atengk.service;


import org.springframework.stereotype.Service;

/**
 * Excel 文件导入实现
 */
@Service("excelFileImport")
public class ExcelFileImport extends AbstractFileImportTemplate {

    @Override
    protected Object parseFile(String filePath) {
        System.out.println("解析 Excel 文件：" + filePath);
        return "Excel 数据内容";
    }

    @Override
    protected void saveToDatabase(Object data) {
        System.out.println("保存 Excel 数据：" + data);
    }
}
