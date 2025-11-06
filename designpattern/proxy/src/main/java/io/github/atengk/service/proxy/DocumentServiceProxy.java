package io.github.atengk.service.proxy;

import io.github.atengk.service.DocumentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 文档访问代理类，控制访问权限
 */
@Service("documentServiceProxy")
public class DocumentServiceProxy implements DocumentService {

    private final DocumentService realDocumentService;

    public DocumentServiceProxy(@Qualifier("realDocumentService") DocumentService realDocumentService) {
        this.realDocumentService = realDocumentService;
    }

    @Override
    public void openDocument(String filename) {
        if (checkAccess(filename)) {
            System.out.println("【权限验证】访问通过");
            realDocumentService.openDocument(filename);
        } else {
            System.out.println("【权限验证】访问被拒绝，您无权查看：" + filename);
        }
    }

    /**
     * 模拟权限检查逻辑
     */
    private boolean checkAccess(String filename) {
        // 模拟：如果文件名包含 "secret" 则拒绝访问
        return !filename.contains("secret");
    }
}
