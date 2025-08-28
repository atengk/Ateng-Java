package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.WordUtil;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class WordUtilTests {

    @Test
    public void test() throws IOException {
        XWPFDocument doc = WordUtil.createDocument();

        WordUtil.addTitle(doc, "合同示例", 20);
        WordUtil.addParagraph(doc, "甲方：XXX 公司", "宋体", 12, ParagraphAlignment.LEFT);
        WordUtil.addParagraph(doc, "乙方：YYY 公司", "宋体", 12, ParagraphAlignment.LEFT);

        String[][] tableData = {
                {"姓名", "职位", "部门"},
                {"张三", "经理", "市场部"},
                {"李四", "工程师", "研发部"}
        };
        WordUtil.addTable(doc, tableData);

        WordUtil.addPageBreak(doc);
        WordUtil.addParagraph(doc, "这是第二页的内容", "宋体", 12, ParagraphAlignment.LEFT);

        WordUtil.addHeader(doc, "示例合同");
        WordUtil.addFooter(doc, "第 1 页");

        WordUtil.writeDocument(doc, "D:\\Temp\\word\\示例合同.docx");
    }

}
