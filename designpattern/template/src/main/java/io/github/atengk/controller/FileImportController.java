package io.github.atengk.controller;

import io.github.atengk.service.AbstractFileImportTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模板方法模式示例控制器
 */
@RestController
@RequestMapping("/api/import")
public class FileImportController {

    private final Map<String, AbstractFileImportTemplate> importTemplates;

    public FileImportController(Map<String, AbstractFileImportTemplate> importTemplates) {
        this.importTemplates = importTemplates;
    }

    @GetMapping("/{type}")
    public String importFile(@PathVariable("type") String type, @RequestParam String filePath) {
        AbstractFileImportTemplate template = importTemplates.get(type + "FileImport");
        if (template == null) {
            return "不支持的文件类型：" + type;
        }
        template.importFile(filePath);
        return "导入成功：" + filePath;
    }
}