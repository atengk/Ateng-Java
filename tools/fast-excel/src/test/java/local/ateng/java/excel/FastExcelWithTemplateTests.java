package local.ateng.java.excel;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpUtil;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.excel.entity.MyImage;
import local.ateng.java.excel.entity.MyUser;
import local.ateng.java.excel.init.InitData;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class FastExcelWithTemplateTests {

    /**
     * 根据模板写入
     * 使用实体类
     */
    @Test
    public void writeExcel() throws IOException {
        String outputFile = "D:/demo.xlsx";
        InputStream inputStream = new ClassPathResource("doc/用户信息模板.xlsx").getInputStream();
        FastExcel
                .write(outputFile)
                .withTemplate(inputStream)
                .sheet("用户信息")
                .doFill(InitData.getDataList());
    }

    /**
     * 根据模板写入
     * 使用Map
     */
    @Test
    public void writeExcel2() throws IOException {
        // 假如存在一个Map
        List<Map> list = Convert.toList(Map.class, InitData.getDataList());
        //List<JSONObject> list = Convert.toList(JSONObject.class, InitData.getDataList());
        String outputFile = "D:/demo.xlsx";
        InputStream inputStream = new ClassPathResource("doc/用户信息模板.xlsx").getInputStream();
        FastExcel
                .write(outputFile)
                .withTemplate(inputStream)
                .sheet("用户信息")
                .doFill(list);
    }

    /**
     * 填充图片数据
     */
    @Test
    public void writeExcel3() throws IOException {
        List<MyImage> list = new ArrayList<>() {{
            add(new MyImage(1, "图片1", "http://192.168.1.12:9000/data/image/logo1.jpg"));
            add(new MyImage(2, "图片2", "http://192.168.1.12:9000/data/image/logo2.jpg"));
            add(new MyImage(3, "图片异常", "http://192.168.1.12:9000/data/image/error.jpg"));
            add(new MyImage(4, "图片不存在", "null"));
        }};
        String outputFile = "D:/demo.xlsx";
        InputStream inputStream = new ClassPathResource("doc/图片信息模板.xlsx").getInputStream();
        FastExcel
                .write(outputFile)
                .withTemplate(inputStream)
                .sheet("图片信息")
                .doFill(list);
    }

    /**
     * 自定义数据填充多个Sheet
     */
    @Test
    public void writeExcel4() throws IOException {
        // 构造数据: 用户信息Sheet
        List<MyUser> userList = InitData.getDataList();
        // 构造数据: 其他信息Sheet
        JSONObject json = JSONObject.of(
                "myName", "阿腾",
                "myDate", DateUtil.now(),
                "myImage", HttpUtil.downloadBytes("http://192.168.1.12:9000/data/image/logo1.jpg"),
                "myDesc", "Hello，我是Ateng！热衷于技术研究和探索，也喜欢户外运动，享受挑战和冒险的乐趣。"
        );
        // 读取文件
        String outputFile = "D:/demo.xlsx";
        InputStream inputStream = new ClassPathResource("doc/自定义填充模板.xlsx").getInputStream();
        // 写入数据
        try (ExcelWriter excelWriter = FastExcel.write(outputFile).withTemplate(inputStream).build()) {
            // 其他信息Sheet
            WriteSheet writeSheet = FastExcel.writerSheet("其他信息").build();
            excelWriter.fill(json, writeSheet);
            // 用户信息Sheet
            WriteSheet writeSheet2 = FastExcel.writerSheet("用户信息").build();
            excelWriter.fill(userList, writeSheet2);
        }
    }

    /**
     * 自定义数据填充多个Sheet，解决图片在单个单元格问题
     */
    @Test
    public void writeExcel5() throws IOException {
        // 构造数据: 用户信息Sheet
        List<MyUser> userList = InitData.getDataList();
        // 构造数据: 其他信息Sheet
        JSONObject json = JSONObject.of(
                "myName", "阿腾",
                "myDate", DateUtil.now(),
                //"myImage", HttpUtil.downloadBytes("http://192.168.1.12:9000/data/image/logo1.jpg"),
                "myDesc", "Hello，我是Ateng！热衷于技术研究和探索，也喜欢户外运动，享受挑战和冒险的乐趣。"
        );
        // 读取文件
        String outputFile = "D:/demo.xlsx";
        InputStream inputStream = new ClassPathResource("doc/自定义填充模板.xlsx").getInputStream();
        // 写入数据
        try (ExcelWriter excelWriter = FastExcel.write(outputFile).withTemplate(inputStream).build()) {
            // 用户信息Sheet
            WriteSheet writeSheet2 = FastExcel.writerSheet("用户信息").build();
            excelWriter.fill(userList, writeSheet2);
            // 其他信息Sheet
            WriteSheet writeSheet = FastExcel.writerSheet("其他信息").build();
            excelWriter.fill(json, writeSheet);
            // 其他信息Sheet，图片绘制位置
            Workbook workbook = excelWriter.writeContext().writeWorkbookHolder().getWorkbook();
            CreationHelper creationHelper = workbook.getCreationHelper();
            ClientAnchor clientAnchor = creationHelper.createClientAnchor();
            clientAnchor.setCol1(0);
            clientAnchor.setCol2(2);
            clientAnchor.setRow1(5);
            clientAnchor.setRow2(10);
            Drawing<?> drawingPatriarch = workbook.getSheet("其他信息").createDrawingPatriarch();
            byte[] bytes = HttpUtil.downloadBytes("http://192.168.1.12:9000/data/image/logo1.jpg");
            int picture = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);
            drawingPatriarch.createPicture(clientAnchor, picture);
        }
    }
}
