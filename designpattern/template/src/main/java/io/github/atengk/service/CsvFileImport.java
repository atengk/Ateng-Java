package io.github.atengk.service;

import org.springframework.stereotype.Service;

/**
 * CSV 文件导入实现
 */
@Service("csvFileImport")
public class CsvFileImport extends AbstractFileImportTemplate {

    @Override
    protected Object parseFile(String filePath) {
        System.out.println("解析 CSV 文件：" + filePath);
        return "CSV 数据内容";
    }

    @Override
    protected void saveToDatabase(Object data) {
        System.out.println("保存 CSV 数据：" + data);
    }
}
