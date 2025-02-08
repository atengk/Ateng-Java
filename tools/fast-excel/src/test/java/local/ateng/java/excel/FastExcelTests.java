package local.ateng.java.excel;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.RandomUtil;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import local.ateng.java.excel.entity.MyImage;
import local.ateng.java.excel.entity.MyUser;
import local.ateng.java.excel.handler.CustomCellStyleWriteHandler;
import local.ateng.java.excel.init.InitData;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FastExcelTests {

    @Test
    public void writeExcel() {
        String fileName = "D:/demo.xlsx";
        // 创建一个名为“模板”的 sheet 页，并写入数据
        FastExcel.write(fileName, MyUser.class).sheet("模板").doWrite(InitData.getDataList());
    }

    /**
     * 分批多次写入
     * 分批写入数据到同一个 Sheet 或多个 Sheet，可实现大数据量的分页写入。
     */
    @Test
    public void writeExcel2() {
        String fileName = "D:/demo.xlsx";
        try (ExcelWriter excelWriter = FastExcel.write(fileName, MyUser.class).build()) {
            for (int i = 0; i < 5; i++) {
                WriteSheet writeSheet = FastExcel.writerSheet(i, "模版" + i).build();
                excelWriter.write(InitData.getDataList(), writeSheet);
            }
        }
    }

    /**
     * 使用自定义处理器，修改表格样式
     */
    @Test
    public void writeExcel3() {
        String fileName = "D:/demo.xlsx";
        FastExcel
                .write(fileName, MyUser.class)
                .registerWriteHandler(CustomCellStyleWriteHandler.cellStyleStrategy())
                .sheet("模板")
                .doWrite(InitData.getDataList());
    }

    /**
     * 导出图片
     */
    @Test
    public void writeExcel4() {
        List<MyImage> list = new ArrayList<>() {{
            add(new MyImage(1, "图片1", "http://192.168.1.12:9000/data/image/logo1.jpg"));
            add(new MyImage(2, "图片2", "http://192.168.1.12:9000/data/image/logo2.jpg"));
            add(new MyImage(3, "图片异常", "http://192.168.1.12:9000/data/image/error.jpg"));
            add(new MyImage(4, "图片不存在", "null"));
        }};
        String fileName = "D:/demo.xlsx";
        FastExcel
                .write(fileName, MyImage.class)
                .registerWriteHandler(CustomCellStyleWriteHandler.cellStyleStrategy())
                .sheet("模板")
                .doWrite(list);
    }

    /**
     * 动态表头（数据）写入
     */
    @Test
    public void writeExcel5() {
        // 生成动态表头，最终要转换为这种格式：[[header1], [header2], [header3]...]
        List<String> headerList = new ArrayList<>();
        int randomInt = RandomUtil.randomInt(1, 20);
        for (int i = 0; i < randomInt; i++) {
            headerList.add("表头" + (i + 1));
        }
        List<List<String>> head = headerList.stream()
                .map(s -> Collections.singletonList(s))
                .collect(Collectors.toList());
        System.out.println(head);
        // 生成数据
        List<List<String>> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {  // 假设我们生成 10 行数据
            List<String> row = new ArrayList<>();
            for (int j = 0; j < headerList.size(); j++) {  // 每行数据的列数与表头列数一致
                row.add("数据" + (i + 1) + "-" + (j + 1));  // 模拟数据
            }
            data.add(row);
        }
        System.out.println(data);
        // 导出数据
        String fileName = "D:/demo.xlsx";
        FastExcel
                .write(fileName)
                .registerWriteHandler(CustomCellStyleWriteHandler.cellStyleStrategy())
                .head(head)
                .sheet("模板")
                .doWrite(data);
    }

}
