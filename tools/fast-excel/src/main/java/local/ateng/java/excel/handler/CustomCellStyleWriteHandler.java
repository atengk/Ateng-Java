package local.ateng.java.excel.handler;

import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.metadata.style.WriteFont;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.*;

public class CustomCellStyleWriteHandler {

    /**
     * 设置内容和表头的样式策略
     * @return HorizontalCellStyleStrategy
     */
    public static HorizontalCellStyleStrategy cellStyleStrategy() {
        // 设置数据内容样式
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentWriteCellStyle.setWrapped(false); // 不自动换行
        setBorderStyleAndFont(contentWriteCellStyle, (short) 12, false, false); // 12号字体，不加粗，无背景色

        // 设置表头样式
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex()); // 替换为浅灰色
        headWriteCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        setBorderStyleAndFont(headWriteCellStyle, (short) 14, true, true); // 14号字体，加粗，有背景色

        return new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
    }

    /**
     * 设置边框样式、字体大小及背景色
     * @param writeCellStyle 写入样式
     * @param fontSize 字体大小
     * @param bold 是否加粗
     * @param hasBackground 是否需要背景色
     */
    private static void setBorderStyleAndFont(WriteCellStyle writeCellStyle, short fontSize, boolean bold, boolean hasBackground) {
        // 设置边框样式（改为双线边框）
        writeCellStyle.setBorderLeft(BorderStyle.DOUBLE);
        writeCellStyle.setBorderTop(BorderStyle.DOUBLE);
        writeCellStyle.setBorderRight(BorderStyle.DOUBLE);
        writeCellStyle.setBorderBottom(BorderStyle.DOUBLE);

        // 设置字体
        WriteFont writeFont = new WriteFont();
        writeFont.setColor(IndexedColors.BLACK.getIndex());
        writeFont.setFontHeightInPoints(fontSize);
        writeFont.setBold(bold);
        writeCellStyle.setWriteFont(writeFont);

        // 是否需要背景色
        if (!hasBackground) {
            writeCellStyle.setFillPatternType(FillPatternType.NO_FILL);
        }
    }
}
