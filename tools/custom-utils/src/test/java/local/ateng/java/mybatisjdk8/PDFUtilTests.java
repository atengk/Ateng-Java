package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.PDFUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class PDFUtilTests {
    @Test
    void test() throws Exception {
        // 1. 生成一个简单 PDF (标题 + 段落)
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add("第一段：这是一个 OpenPDF 工具类的示例。");
        paragraphs.add("第二段：支持中文，避免乱码。");
        paragraphs.add("第三段：适用于日常报表、合同生成等场景。");
        PDFUtil.createPdf("D:\\Temp\\pdf\\demo_simple.pdf", "PDF 示例文档", paragraphs);

        // 2. 生成带表格的 PDF
        List<String[]> tableData = new ArrayList<>();
        tableData.add(new String[]{"编号", "姓名", "分数"});
        tableData.add(new String[]{"1", "张三", "90"});
        tableData.add(new String[]{"2", "李四", "85"});
        tableData.add(new String[]{"3", "王五", "78"});
        PDFUtil.createPdfWithTable("D:\\Temp\\pdf\\demo_table.pdf", "成绩单", tableData);

        /*// 3. 合并多个 PDF
        List<String> pdfList = Arrays.asList("demo_simple.pdf", "demo_table.pdf");
        PDFUtil.mergePdf(pdfList, "demo_merged.pdf");

        // 4. 拆分 PDF
        PDFUtil.splitPdf("demo_merged.pdf", "./split");

        // 5. 提取文本
        String text = PDFUtil.extractText("demo_simple.pdf");
        System.out.println("提取的文本内容：\n" + text);

        // 6. 添加文字水印
        PDFUtil.addTextWatermark("demo_simple.pdf", "demo_watermark_text.pdf", "内部资料，请勿外传");

        // 7. 添加图片水印
        PDFUtil.addImageWatermark("demo_simple.pdf", "demo_watermark_image.pdf", "logo.png");

        // 8. PDF 加密（用户密码+所有者密码）
        PDFUtil.encryptPdf("demo_simple.pdf", "demo_encrypt.pdf", "user123", "owner123");

        // 9. PDF 转字节流（可用于 Web 下载）
        byte[] pdfBytes = PDFUtil.toByteArray("demo_simple.pdf");
        System.out.println("PDF 转换为字节数组，大小：" + pdfBytes.length + " bytes");*/
    }
}
