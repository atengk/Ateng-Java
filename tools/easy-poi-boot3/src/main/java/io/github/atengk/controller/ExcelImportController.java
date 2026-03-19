package io.github.atengk.controller;

import io.github.atengk.entity.MyUser;
import io.github.atengk.util.ExcelUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/excel/import")
public class ExcelImportController {

    /**
     * 导入Excel
     */
    @PostMapping("/simple")
    public List<MyUser> exportEntity(MultipartFile file) {
        List<MyUser> list = ExcelUtil.importExcel(file, MyUser.class);
        return list;
    }

}
