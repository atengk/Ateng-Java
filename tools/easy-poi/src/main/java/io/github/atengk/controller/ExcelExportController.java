package io.github.atengk.controller;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import io.github.atengk.util.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/excel/export")
public class ExcelExportController {

    /**
     * 导出Excel
     */
    @GetMapping("/entity")
    public void exportEntity(HttpServletResponse response) {
        List<MyUser> list = InitData.getDataList();
        String fileName = "用户列表.xlsx";
        ExcelUtil.exportExcel(
                MyUser.class,
                list,
                fileName,
                response,
                params -> params.setSheetName("用户列表")
        );
    }

    /**
     * 动态导出 Excel
     */
    @GetMapping("/dynamic")
    public void exportDynamic(HttpServletResponse response) {
        List<MyUser> userList = InitData.getDataList();

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("city", user.getCity());
            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        ExcelExportEntity id = new ExcelExportEntity("ID", "id");
        id.setWidth(20);
        entityList.add(id);
        ExcelExportEntity name = new ExcelExportEntity("姓名", "name");
        name.setWidth(30);
        entityList.add(name);
        ExcelExportEntity age = new ExcelExportEntity("年龄", "age");
        age.setWidth(20);
        entityList.add(age);
        ExcelExportEntity city = new ExcelExportEntity("城市", "city");
        city.setWidth(40);
        entityList.add(city);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);
        String fileName = "用户列表.xlsx";
        ExcelUtil.write(workbook,  fileName, response);
    }

}
