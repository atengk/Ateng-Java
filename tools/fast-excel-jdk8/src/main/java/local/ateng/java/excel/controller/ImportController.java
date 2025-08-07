package local.ateng.java.excel.controller;

import cn.idev.excel.FastExcel;
import local.ateng.java.excel.entity.MyUser;
import local.ateng.java.excel.listener.MyUserListener;
import local.ateng.java.excel.listener.MyUserMapListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/import")
public class ImportController {


    @PostMapping("/simple")
    public ResponseEntity<String> simple(@RequestParam("file") MultipartFile file) throws IOException {
        List<MyUser> list = FastExcel
                .read(file.getInputStream(), MyUser.class, new MyUserListener())
                .sheet()
                .doReadSync();
        System.out.println(list);
        return ResponseEntity.ok("文件上传并处理成功！");
    }

    @PostMapping("/ignore")
    public ResponseEntity<String> ignore(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = "D:/demo.xlsx";
        FastExcel
                .read(fileName, new MyUserMapListener())
                .sheet()
                .doRead();
        List<MyUser> userList = new ArrayList<>(MyUserMapListener.userList);
        MyUserMapListener.userList.clear();
        System.out.println(userList);
        return ResponseEntity.ok("文件上传并处理成功！" + userList.size());
    }

}
