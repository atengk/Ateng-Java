package io.github.atengk.controller;

import io.github.atengk.service.DocumentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文档控制层，用于测试代理模式
 */
@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(@Qualifier("documentServiceProxy") DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/open")
    public String openDocument(String filename) {
        documentService.openDocument(filename);
        return "文档访问结束";
    }
}
