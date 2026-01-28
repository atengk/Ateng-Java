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
public class ImportExportController {

    /**
     * 导入Excel
     */
    @PostMapping("/simple")
    public List<MyUser> simple(MultipartFile file) {
        List<MyUser> userList = ExcelUtil.importExcel(ExcelUtil.toInputStream(file), MyUser.class);
        return userList;
    }

}
