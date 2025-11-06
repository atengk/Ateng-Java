package io.github.atengk.service;

/**
 * 文件导入模板抽象类
 * 定义文件导入的通用流程（模板方法）
 */
public abstract class AbstractFileImportTemplate {

    /**
     * 模板方法：固定导入流程
     * final 关键字防止子类重写
     */
    public final void importFile(String filePath) {
        validateFile(filePath);
        Object data = parseFile(filePath);
        saveToDatabase(data);
        afterImport(filePath);
    }

    /**
     * 校验文件格式（通用步骤或可选重写）
     */
    protected void validateFile(String filePath) {
        System.out.println("校验文件格式：" + filePath);
    }

    /**
     * 解析文件内容（抽象方法，必须由子类实现）
     */
    protected abstract Object parseFile(String filePath);

    /**
     * 保存解析结果到数据库（抽象方法）
     */
    protected abstract void saveToDatabase(Object data);

    /**
     * 导入完成后的操作（钩子方法，可选）
     */
    protected void afterImport(String filePath) {
        System.out.println("文件导入完成：" + filePath);
    }
}
