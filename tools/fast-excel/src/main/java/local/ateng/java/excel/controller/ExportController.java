package local.ateng.java.excel.controller;

import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.excel.entity.MyUser;
import local.ateng.java.excel.handler.CustomCellStyleWriteHandler;
import local.ateng.java.excel.init.InitData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/export")
public class ExportController {

    /**
     * 导出 Excel 文件的接口
     * 访问路径: GET /export/simple
     *
     * @param response HTTP 响应对象，用于返回 Excel 文件流
     * @throws IOException 可能的 IO 异常
     */
    @GetMapping("/simple")
    public void simple(HttpServletResponse response) throws IOException {
        // 设置响应的内容类型为 Excel 文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 设置字符编码，防止文件名乱码
        response.setCharacterEncoding("utf-8");

        // 生成导出文件的名称，避免重复，使用时间戳
        String name = "用户文件";
        String filename = URLEncoder.encode(name, "UTF-8") // 进行 URL 编码，防止中文乱码
                .replaceAll("\\+", "%20")  // 处理 "+" 号替换为空格
                + System.currentTimeMillis()  // 添加时间戳，防止文件重名
                + ExcelTypeEnum.XLSX.getValue();  // 获取 Excel 文件的扩展名（.xlsx）

        // 设置 HTTP 响应头，通知浏览器以附件方式下载
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename);

        // 使用 FastExcel 进行 Excel 导出
        FastExcel
                .write(response.getOutputStream(), MyUser.class)  // 指定数据模型类 MyUser
                .registerWriteHandler(CustomCellStyleWriteHandler.cellStyleStrategy())  // 注册自定义样式处理器
                .sheet(name)  // 设置 Excel 工作表名称
                .doWrite(InitData.getDataList());  // 写入数据
    }

}
