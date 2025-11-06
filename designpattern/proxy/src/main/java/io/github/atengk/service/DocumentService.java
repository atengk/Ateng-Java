package io.github.atengk.service;

/**
 * 文档访问服务接口
 */
public interface DocumentService {

    /**
     * 打开指定文档
     *
     * @param filename 文件名
     */
    void openDocument(String filename);
}
