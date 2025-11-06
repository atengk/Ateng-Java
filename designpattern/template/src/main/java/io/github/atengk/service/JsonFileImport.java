package io.github.atengk.service;


import org.springframework.stereotype.Service;

/**
 * JSON 文件导入实现
 */
@Service("jsonFileImport")
public class JsonFileImport extends AbstractFileImportTemplate {

    @Override
    protected Object parseFile(String filePath) {
        System.out.println("解析 JSON 文件：" + filePath);
        return "JSON 数据内容";
    }

    @Override
    protected void saveToDatabase(Object data) {
        System.out.println("保存 JSON 数据：" + data);
    }
}
