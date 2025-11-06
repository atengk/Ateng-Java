package io.github.atengk.service.impl;

import io.github.atengk.service.DocumentService;
import org.springframework.stereotype.Service;

/**
 * 真实的文档服务类，执行实际的文件访问逻辑
 */
@Service("realDocumentService")
public class RealDocumentService implements DocumentService {

    @Override
    public void openDocument(String filename) {
        System.out.println("【文件访问】正在打开文档：" + filename);
    }
}
