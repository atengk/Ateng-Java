package io.github.atengk.util;

import org.apache.fesod.common.util.StringUtils;
import org.apache.fesod.sheet.write.metadata.style.WriteCellStyle;
import org.apache.fesod.sheet.write.metadata.style.WriteFont;
import org.apache.fesod.sheet.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.*;

/**
 * Excel 样式工具类（基于 Apache Fesod）
 * <p>
 * 统一管理 Excel 导出中表头与内容的样式策略构建逻辑。
 * 对外只提供样式策略构建能力，内部实现全部封装。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-26
 */
public final class ExcelStyleUtil {

    /**
     * 禁止实例化工具类
     */
    private ExcelStyleUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 默认表头字体大小（磅）
     */
    public static final short DEFAULT_HEADER_FONT_SIZE = 11;

    /**
     * 默认内容字体大小（磅）
     */
    public static final short DEFAULT_CONTENT_FONT_SIZE = 11;

    /**
     * 默认字体名称
     */
    public static final String DEFAULT_FONT_NAME = "宋体";

    /**
     * 构建默认样式策略（推荐直接使用）
     *
     * @return 默认的表头 + 内容样式策略
     */
    public static HorizontalCellStyleStrategy getDefaultStyleStrategy() {
        return buildCustomStyleStrategy(
                DEFAULT_HEADER_FONT_SIZE,
                true,
                false,
                IndexedColors.BLACK.getIndex(),
                DEFAULT_FONT_NAME,
                IndexedColors.WHITE.getIndex(),
                BorderStyle.THIN,
                HorizontalAlignment.CENTER,
                VerticalAlignment.CENTER,

                DEFAULT_CONTENT_FONT_SIZE,
                false,
                false,
                IndexedColors.BLACK.getIndex(),
                DEFAULT_FONT_NAME,
                null,
                BorderStyle.THIN,
                HorizontalAlignment.CENTER,
                VerticalAlignment.CENTER,
                false
        );
    }

    /**
     * 构建完全可配置的 Excel 样式策略。
     * <p>
     * 该方法用于一次性构建“表头样式 + 内容样式”的组合策略，
     * 支持字体、颜色、背景、边框、对齐方式、是否自动换行等所有常用样式配置，
     * 适用于导出 Excel 时对整体风格进行统一控制。
     * </p>
     *
     * @param headFontSize           表头字体大小（单位：磅）
     * @param headBold               表头字体是否加粗
     * @param headItalic             表头字体是否斜体
     * @param headFontColor          表头字体颜色（IndexedColors 枚举值）
     * @param headFontName           表头字体名称，如“微软雅黑”
     * @param headBackgroundColor    表头背景色（IndexedColors 枚举值），为 null 表示不设置背景色
     * @param headBorderStyle        表头单元格边框样式
     * @param headHorizontalAlign    表头水平对齐方式
     * @param headVerticalAlign      表头垂直对齐方式
     * @param contentFontSize        内容字体大小（单位：磅）
     * @param contentBold            内容字体是否加粗
     * @param contentItalic          内容字体是否斜体
     * @param contentFontColor       内容字体颜色（IndexedColors 枚举值）
     * @param contentFontName        内容字体名称
     * @param contentBackgroundColor 内容背景色（IndexedColors 枚举值），为 null 表示不设置背景色
     * @param contentBorderStyle     内容单元格边框样式
     * @param contentHorizontalAlign 内容水平对齐方式
     * @param contentVerticalAlign   内容垂直对齐方式
     * @param contentWrapped         内容是否自动换行
     * @return 水平样式策略对象（包含表头样式 + 内容样式）
     */
    public static HorizontalCellStyleStrategy buildCustomStyleStrategy(
            short headFontSize,
            boolean headBold,
            boolean headItalic,
            short headFontColor,
            String headFontName,
            Short headBackgroundColor,
            BorderStyle headBorderStyle,
            HorizontalAlignment headHorizontalAlign,
            VerticalAlignment headVerticalAlign,

            short contentFontSize,
            boolean contentBold,
            boolean contentItalic,
            short contentFontColor,
            String contentFontName,
            Short contentBackgroundColor,
            BorderStyle contentBorderStyle,
            HorizontalAlignment contentHorizontalAlign,
            VerticalAlignment contentVerticalAlign,
            boolean contentWrapped
    ) {

        WriteCellStyle headStyle = buildCellStyle(
                headHorizontalAlign,
                headVerticalAlign,
                headBackgroundColor,
                headFontSize,
                headBold,
                headItalic,
                headFontColor,
                headFontName,
                headBorderStyle,
                false
        );

        WriteCellStyle contentStyle = buildCellStyle(
                contentHorizontalAlign,
                contentVerticalAlign,
                contentBackgroundColor,
                contentFontSize,
                contentBold,
                contentItalic,
                contentFontColor,
                contentFontName,
                contentBorderStyle,
                contentWrapped
        );

        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }

    /**
     * 构建单个单元格的写入样式对象。
     * <p>
     * 该方法为内部通用构建方法，用于根据参数组合生成 WriteCellStyle，
     * 同时完成对齐方式、背景色、字体样式、边框样式以及是否自动换行的统一设置。
     * </p>
     *
     * @param horizontalAlignment 水平对齐方式
     * @param verticalAlignment   垂直对齐方式
     * @param backgroundColor     背景色（IndexedColors 枚举值），为 null 表示不设置背景
     * @param fontSize            字体大小（磅）
     * @param bold                是否加粗
     * @param italic              是否斜体
     * @param fontColor           字体颜色（IndexedColors 枚举值）
     * @param fontName            字体名称
     * @param borderStyle         边框样式
     * @param wrapped             是否自动换行
     * @return 构建完成的 WriteCellStyle 对象
     */
    private static WriteCellStyle buildCellStyle(
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            Short backgroundColor,
            short fontSize,
            boolean bold,
            boolean italic,
            short fontColor,
            String fontName,
            BorderStyle borderStyle,
            boolean wrapped
    ) {
        WriteCellStyle style = new WriteCellStyle();
        style.setHorizontalAlignment(horizontalAlignment);
        style.setVerticalAlignment(verticalAlignment);
        style.setWrapped(wrapped);

        applyBackground(style, backgroundColor);
        style.setWriteFont(buildFont(fontSize, bold, italic, fontColor, fontName));
        applyBorder(style, borderStyle);

        return style;
    }

    /**
     * 构建字体样式对象。
     * <p>
     * 统一封装字体大小、加粗、斜体、颜色及字体名称的设置逻辑，
     * 供单元格样式构建过程复用。
     * </p>
     *
     * @param fontSize  字体大小（磅）
     * @param bold      是否加粗
     * @param italic    是否斜体
     * @param fontColor 字体颜色（IndexedColors 枚举值）
     * @param fontName  字体名称
     * @return 构建完成的 WriteFont 对象
     */
    private static WriteFont buildFont(
            short fontSize,
            boolean bold,
            boolean italic,
            short fontColor,
            String fontName
    ) {
        WriteFont font = new WriteFont();
        font.setFontHeightInPoints(fontSize);
        font.setBold(bold);
        font.setItalic(italic);
        font.setColor(fontColor);

        if (StringUtils.isNotBlank(fontName)) {
            font.setFontName(fontName);
        }

        return font;
    }

    /**
     * 设置单元格背景色。
     * <p>
     * 若 backgroundColor 不为 null，则设置填充颜色并启用实心填充模式；
     * 若为 null，则表示不设置背景色，采用无填充模式。
     * </p>
     *
     * @param style           单元格写入样式对象
     * @param backgroundColor 背景色（IndexedColors 枚举值），可为 null
     */
    private static void applyBackground(WriteCellStyle style, Short backgroundColor) {
        if (backgroundColor != null) {
            style.setFillForegroundColor(backgroundColor);
            style.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        } else {
            style.setFillPatternType(FillPatternType.NO_FILL);
        }
    }

    /**
     * 设置单元格四个方向的边框样式。
     * <p>
     * 包括：上、下、左、右四条边统一使用同一种边框样式，
     * 用于快速构建风格统一的表格边框效果。
     * </p>
     *
     * @param style       单元格写入样式对象
     * @param borderStyle 边框样式枚举（如 THIN、MEDIUM、DASHED、DOUBLE 等）
     */
    private static void applyBorder(WriteCellStyle style, BorderStyle borderStyle) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
    }

}
