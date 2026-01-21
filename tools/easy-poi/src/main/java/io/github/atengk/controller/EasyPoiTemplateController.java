package io.github.atengk.controller;

import cn.afterturn.easypoi.entity.vo.NormalExcelConstants;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.view.PoiBaseView;
import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EasyPOI 模板导出测试 Controller
 * 仅用于验证 Spring Boot 3 + EasyPOI 是否能正常工作
 */
@RestController
public class EasyPoiTemplateController {


    @GetMapping("/easypoi/export")
    public void export(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // 1. 获取数据
        List<MyUser> userList = InitData.getDataList();

        // 2. 自定义表头
        List<ExcelExportEntity> headers = new ArrayList<>();
        headers.add(new ExcelExportEntity("ID", "id"));
        headers.add(new ExcelExportEntity("姓名", "name"));
        headers.add(new ExcelExportEntity("年龄", "age"));
        headers.add(new ExcelExportEntity("手机号", "phoneNumber"));
        headers.add(new ExcelExportEntity("邮箱", "email"));
        headers.add(new ExcelExportEntity("分数", "score"));
        headers.add(new ExcelExportEntity("比例", "ratio"));
        headers.add(new ExcelExportEntity("生日", "birthday"));
        headers.add(new ExcelExportEntity("省份", "province"));
        headers.add(new ExcelExportEntity("城市", "city"));
        headers.add(new ExcelExportEntity("创建时间", "createTime"));

        // 3. 数据转为 Map
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("phoneNumber", user.getPhoneNumber());
            map.put("email", user.getEmail());
            map.put("score", user.getScore());
            map.put("ratio", user.getRatio());
            map.put("birthday", user.getBirthday());
            map.put("province", user.getProvince());
            map.put("city", user.getCity());
            map.put("createTime", user.getCreateTime());
            dataList.add(map);
        }

        // 4. 组装 EasyPOI MVC Model
        ExportParams params = new ExportParams("MyUser 数据表", "用户数据");

        Map<String, Object> model = new HashMap<>();
        model.put(NormalExcelConstants.FILE_NAME, "MyUser数据");
        model.put(NormalExcelConstants.PARAMS, params);
        model.put(NormalExcelConstants.DATA_LIST, dataList);
        model.put(NormalExcelConstants.MAP_LIST, headers);

        // 5. 通过 PoiBaseView 输出
        PoiBaseView.render(model, request, response, NormalExcelConstants.EASYPOI_EXCEL_VIEW);


    }

}
