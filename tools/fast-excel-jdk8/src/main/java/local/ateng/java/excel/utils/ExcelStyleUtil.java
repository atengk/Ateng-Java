package local.ateng.java.excel.utils;

import cn.idev.excel.util.StringUtils;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.idev.excel.write.handler.AbstractSheetWriteHandler;
import cn.idev.excel.write.handler.RowWriteHandler;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.handler.WriteHandler;
import cn.idev.excel.write.handler.context.RowWriteHandlerContext;
import cn.idev.excel.write.handler.context.SheetWriteHandlerContext;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.metadata.style.WriteFont;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import cn.idev.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import cn.idev.excel.write.style.row.SimpleRowHeightStyleStrategy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Excel 样式工具类，基于 FastExcel 实现。
 * <p>
 * 更多说明见：
 * <a href="https://github.com/fast-excel/fastexcel">StringUtil 使用文档</a>
 * </p>
 *
 * @author 孔余
 * @since 2025-08-06
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
    public static final Short DEFAULT_HEADER_FONT_SIZE = 14;

    /**
     * 默认内容字体大小（磅）
     */
    public static final Short DEFAULT_CONTENT_FONT_SIZE = 12;

    /**
     * 默认内容字体
     */
    public static final String DEFAULT_CONTENT_FONT_NAME = "微软雅黑";

    /**
     * 默认表头行高（单位：磅）
     */
    public static final Short DEFAULT_HEADER_ROW_HEIGHT = 20;

    /**
     * 默认内容行高（单位：磅）
     */
    public static final Short DEFAULT_CONTENT_ROW_HEIGHT = 15;

    /**
     * 默认冻结列数
     */
    public static final Integer DEFAULT_FREEZE_COLUMN = 1;

    /**
     * 获取默认的 Excel 单元格样式策略。
     * <p>包括表头和内容的统一居中、边框、字体、背景色等设置。</p>
     *
     * @return 默认样式策略（表头 + 内容）
     */
    public static HorizontalCellStyleStrategy getDefaultStyleStrategy() {
        // 构建 Excel 样式策略，设置表头和内容的字体、颜色、对齐方式、边框等样式
        HorizontalCellStyleStrategy strategy = buildCustomStyleStrategy(
                // 表头字体大小（单位：磅）
                DEFAULT_HEADER_FONT_SIZE,

                // 表头是否加粗
                false,

                // 表头是否斜体
                false,

                // 表头字体颜色（使用 IndexedColors 枚举值）
                IndexedColors.BLACK.getIndex(),

                // 表头字体名称
                DEFAULT_CONTENT_FONT_NAME,

                // 表头背景色（设置为浅灰色）
                IndexedColors.GREY_40_PERCENT.getIndex(),

                // 表头边框样式
                BorderStyle.DOUBLE,

                // 表头水平居中对齐
                HorizontalAlignment.CENTER,

                // 表头垂直居中对齐
                VerticalAlignment.CENTER,

                // 内容字体大小
                DEFAULT_CONTENT_FONT_SIZE,

                // 内容是否加粗
                false,

                // 内容是否斜体
                false,

                // 内容字体颜色（黑色）
                IndexedColors.BLACK.getIndex(),

                // 内容字体名称
                DEFAULT_CONTENT_FONT_NAME,

                // 内容背景色（为空表示不设置背景色）
                null,

                // 内容边框样式
                BorderStyle.DOUBLE,

                // 内容水平居中对齐
                HorizontalAlignment.CENTER,

                // 内容垂直居中对齐
                VerticalAlignment.CENTER,

                // 内容是否自动换行
                false
        );

        return strategy;
    }

    /**
     * 构建自定义样式策略，适用于 EasyExcel 多样式写入
     * 支持设置字体、颜色、背景、边框、对齐方式、是否换行等样式配置。
     *
     * @param headFontSize           表头字体大小
     * @param headBold               表头是否加粗
     * @param headItalic             表头是否斜体
     * @param headFontColor          表头字体颜色（IndexedColors 枚举值）
     * @param headFontName           表头字体名称（如 "微软雅黑"）
     * @param headBackgroundColor    表头背景色（IndexedColors 枚举值）
     * @param headBorderStyle        表头边框样式
     * @param headHorizontalAlign    表头水平对齐方式（HorizontalAlignment 枚举值）
     * @param headVerticalAlign      表头垂直对齐方式（VerticalAlignment 枚举值）
     * @param contentFontSize        内容字体大小
     * @param contentBold            内容是否加粗
     * @param contentItalic          内容是否斜体
     * @param contentFontColor       内容字体颜色（IndexedColors 枚举值）
     * @param contentFontName        内容字体名称
     * @param contentBackgroundColor 内容背景色（IndexedColors 枚举值）
     * @param contentBorderStyle     内容边框样式
     * @param contentHorizontalAlign 内容水平对齐方式（HorizontalAlignment 枚举值）
     * @param contentVerticalAlign   内容垂直对齐方式（VerticalAlignment 枚举值）
     * @param contentWrapped         内容是否自动换行
     * @return 样式策略对象，可用于 EasyExcel write 中注册
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
        // 构建表头样式对象
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setHorizontalAlignment(headHorizontalAlign);
        headStyle.setVerticalAlignment(headVerticalAlign);

        // 设置表头背景色（若传入为 null，则不设置填充）
        if (headBackgroundColor != null) {
            headStyle.setFillForegroundColor(headBackgroundColor);
            headStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        } else {
            headStyle.setFillPatternType(FillPatternType.NO_FILL);
        }

        // 构建表头字体样式
        WriteFont headFont = new WriteFont();
        headFont.setFontHeightInPoints(headFontSize);
        headFont.setBold(headBold);
        headFont.setItalic(headItalic);
        headFont.setColor(headFontColor);
        if (StringUtils.isNotBlank(headFontName)) {
            headFont.setFontName(headFontName);
        }
        headStyle.setWriteFont(headFont);

        // 构建内容样式对象
        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setHorizontalAlignment(contentHorizontalAlign);
        contentStyle.setVerticalAlignment(contentVerticalAlign);
        contentStyle.setWrapped(contentWrapped);

        // 设置内容背景色（若传入为 null，则不设置填充）
        if (contentBackgroundColor != null) {
            contentStyle.setFillForegroundColor(contentBackgroundColor);
            contentStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        } else {
            contentStyle.setFillPatternType(FillPatternType.NO_FILL);
        }

        // 构建内容字体样式
        WriteFont contentFont = new WriteFont();
        contentFont.setFontHeightInPoints(contentFontSize);
        contentFont.setBold(contentBold);
        contentFont.setItalic(contentItalic);
        contentFont.setColor(contentFontColor);
        if (StringUtils.isNotBlank(contentFontName)) {
            contentFont.setFontName(contentFontName);
        }
        contentStyle.setWriteFont(contentFont);

        // 应用表头边框样式
        applyBorderStyle(headStyle, headBorderStyle);

        // 应用内容边框样式
        applyBorderStyle(contentStyle, contentBorderStyle);

        // 返回组合的样式策略
        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }

    /**
     * 设置单元格的四个边的边框样式
     *
     * @param style       单元格写入样式对象
     * @param borderStyle 边框样式枚举（如 THIN、MEDIUM、DASHED 等）
     */
    private static void applyBorderStyle(WriteCellStyle style, BorderStyle borderStyle) {
        // 设置顶部边框样式
        style.setBorderTop(borderStyle);

        // 设置底部边框样式
        style.setBorderBottom(borderStyle);

        // 设置左侧边框样式
        style.setBorderLeft(borderStyle);

        // 设置右侧边框样式
        style.setBorderRight(borderStyle);
    }

    /**
     * 生成斑马纹样式的行级样式处理器。
     * <p>
     * 功能说明：
     * 每隔一行设置不同的背景色，用于提升表格可读性，常用于数据列表展示。
     * <p>
     * 使用方式：
     * EasyExcel.write(outputStream)
     * .registerWriteHandler(ExcelStyleUtil.zebraStripeHandler())
     * .sheet()
     * .doWrite(dataList);
     */
    public static RowWriteHandler zebraStripeHandler() {
        return new RowWriteHandler() {
            @Override
            public void afterRowDispose(RowWriteHandlerContext context) {
                // 从第二行开始应用斑马纹样式（跳过表头）
                if (context.getRowIndex() > 0 && context.getRowIndex() % 2 == 1) {
                    Row row = context.getRow();
                    for (Cell cell : row) {
                        CellStyle cellStyle = context.getWriteWorkbookHolder().getCachedWorkbook().createCellStyle();
                        cellStyle.cloneStyleFrom(cell.getCellStyle());
                        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        cell.setCellStyle(cellStyle);
                    }
                }
            }
        };
    }

    /**
     * 自动列宽策略（最长匹配内容）。
     * <p>
     * 功能说明：
     * 根据列中最长单元格的内容自动调整列宽，避免内容被遮挡。
     * <p>
     * 使用方式：
     * EasyExcel.write(outputStream)
     * .registerWriteHandler(ExcelStyleUtil.autoColumnWidthStrategy())
     * .sheet()
     * .doWrite(dataList);
     */
    public static AbstractColumnWidthStyleStrategy autoColumnWidthStrategy() {
        return new LongestMatchColumnWidthStyleStrategy();
    }

    /**
     * 获取默认行高样式策略
     *
     * @return 行高策略
     */
    public static SimpleRowHeightStyleStrategy defaultRowHeightStrategy() {
        return new SimpleRowHeightStyleStrategy(DEFAULT_HEADER_ROW_HEIGHT, DEFAULT_CONTENT_ROW_HEIGHT);
    }

    /**
     * 获取冻结列并隐藏列策略（列号从 0 开始）
     * 该方法不依赖外部类，直接内联 SheetWriteHandler 实现
     *
     * @param freezeColCount 要冻结的列数
     * @param hiddenCols     要隐藏的列索引数组
     * @return SheetWriteHandler 策略对象
     */
    public static SheetWriteHandler freezeAndHiddenStrategy(int freezeColCount, int[] hiddenCols) {
        return new AbstractSheetWriteHandler() {

            @Override
            public void beforeSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
                // 此方法保留为空，满足接口要求
            }

            @Override
            public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
                Sheet sheet = writeSheetHolder.getSheet();

                // 冻结列
                if (freezeColCount > 0) {
                    sheet.createFreezePane(freezeColCount, 0);
                }

                // 隐藏列
                if (hiddenCols != null && hiddenCols.length > 0) {
                    for (int colIndex : hiddenCols) {
                        sheet.setColumnHidden(colIndex, true);
                    }
                }
            }
        };
    }

    /**
     * 获取默认冻结首列策略（隐藏列为空）
     *
     * @return 策略实例
     */
    public static SheetWriteHandler defaultFreezeStrategy() {
        return freezeAndHiddenStrategy(DEFAULT_FREEZE_COLUMN, new int[]{});
    }

    /**
     * 隐藏指定列的 Sheet 处理器。
     * <p>
     * 功能说明：
     * 可隐藏某些列不显示在前端，但仍保留在文件中。
     * <p>
     * 使用方式：
     * EasyExcel.write(outputStream)
     * .registerWriteHandler(ExcelStyleUtil.hideColumns(1, 3))
     * .sheet()
     * .doWrite(dataList);
     *
     * @param columnIndexes 要隐藏的列索引（从0开始）
     */
    public static SheetWriteHandler hideColumns(int... columnIndexes) {
        return new SheetWriteHandler() {
            @Override
            public void beforeSheetCreate(SheetWriteHandlerContext context) {
            }

            @Override
            public void afterSheetCreate(SheetWriteHandlerContext context) {
                Sheet sheet = context.getWriteSheetHolder().getSheet();
                for (int index : columnIndexes) {
                    sheet.setColumnHidden(index, true);
                }
            }
        };
    }

    /**
     * 自动列宽 + 冻结首列处理器组合
     *
     * @return WriteHandler 复合处理器（通过注册多个处理器）
     */
    public static List<WriteHandler> autoWidthAndFreezeFirstCol() {
        List<WriteHandler> handlers = new ArrayList<>();
        // 自动列宽
        handlers.add(new LongestMatchColumnWidthStyleStrategy());
        // 冻结首列
        handlers.add(freezeAndHiddenStrategy(1, null));
        return handlers;
    }


    /**
     * 注册自动列宽 + 冻结首列写入器
     *
     * @param builder 写入器
     */
    public static void applyAutoWidthAndFreeze(ExcelWriterBuilder builder) {
        builder.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        builder.registerWriteHandler(freezeAndHiddenStrategy(1, null));
    }



    /**
     * 合并指定行列区域（静态范围）
     *
     * @param firstRow 起始行（从0开始）
     * @param lastRow  结束行（包含）
     * @param firstCol 起始列
     * @param lastCol  结束列
     * @return WriteHandler 合并策略
     */
    public static WriteHandler mergeFixedRegion(int firstRow, int lastRow, int firstCol, int lastCol) {
        return new SheetWriteHandler() {

            @Override
            public void beforeSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
                // 空实现，满足接口要求
            }

            @Override
            public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
                Sheet sheet = writeSheetHolder.getSheet();
                sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
            }
        };
    }

    /**
     * 多行表头相同内容横向 + 纵向 合并（仅表头区域）
     *
     * @param firstRowIndex 表头起始行索引（包含，0开始）
     * @param lastRowIndex  表头结束行索引（包含）
     * @param firstColIndex 表头起始列索引（包含）
     * @param lastColIndex  表头结束列索引（包含）
     * @return 表头合并处理器
     */
    public static SheetWriteHandler mergeSameHeaderCells(int firstRowIndex, int lastRowIndex,
                                                         int firstColIndex, int lastColIndex) {
        return new AbstractSheetWriteHandler() {
            @Override
            public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
                Sheet sheet = writeSheetHolder.getSheet();
                if (sheet == null || firstRowIndex > lastRowIndex || firstColIndex > lastColIndex) {
                    return;
                }

                // 横向合并：对每一行，处理指定列范围
                for (int rowIndex = firstRowIndex; rowIndex <= lastRowIndex; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }

                    int mergeStartCol = firstColIndex;
                    String prevValue = getCellStringValue(row.getCell(firstColIndex));

                    for (int colIndex = firstColIndex + 1; colIndex <= lastColIndex + 1; colIndex++) {
                        String currentValue = (colIndex <= lastColIndex)
                                ? getCellStringValue(row.getCell(colIndex)) : null;

                        if (Objects.equals(prevValue, currentValue)) {
                            // continue
                        } else {
                            if (mergeStartCol < colIndex - 1 && prevValue != null) {
                                sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, mergeStartCol, colIndex - 1));
                            }
                            mergeStartCol = colIndex;
                            prevValue = currentValue;
                        }
                    }
                }

                // 纵向合并：对每一列，处理指定行范围
                for (int colIndex = firstColIndex; colIndex <= lastColIndex; colIndex++) {
                    int mergeStartRow = firstRowIndex;
                    String prevValue = getCellStringValue(getCell(sheet, firstRowIndex, colIndex));

                    for (int rowIndex = firstRowIndex + 1; rowIndex <= lastRowIndex + 1; rowIndex++) {
                        String currentValue = (rowIndex <= lastRowIndex)
                                ? getCellStringValue(getCell(sheet, rowIndex, colIndex)) : null;

                        if (Objects.equals(prevValue, currentValue)) {
                            // continue
                        } else {
                            if (mergeStartRow < rowIndex - 1 && prevValue != null) {
                                sheet.addMergedRegion(new CellRangeAddress(mergeStartRow, rowIndex - 1, colIndex, colIndex));
                            }
                            mergeStartRow = rowIndex;
                            prevValue = currentValue;
                        }
                    }
                }
            }

            private String getCellStringValue(Cell cell) {
                if (cell == null) {
                    return null;
                }
                cell.setCellType(CellType.STRING);
                return cell.getStringCellValue().trim();
            }

            private Cell getCell(Sheet sheet, int rowIndex, int colIndex) {
                Row row = sheet.getRow(rowIndex);
                return row == null ? null : row.getCell(colIndex);
            }
        };
    }

    /**
     * 构建通用 CellStyle 样式（支持传参设置）
     *
     * @param workbook       工作簿对象（必传）
     * @param fontName       字体名称（如 "微软雅黑"）
     * @param fontSize       字体大小（单位：磅）
     * @param bold           是否加粗
     * @param wrapText       是否自动换行
     * @param centerAlign    是否水平居中
     * @param verticalCenter 是否垂直居中
     * @param border         是否启用边框
     * @param bgColor        背景色（IndexedColors 枚举，null 表示不设置）
     * @return 样式对象 CellStyle
     */
    public static CellStyle createCustomCellStyle(Workbook workbook,
                                                  String fontName,
                                                  short fontSize,
                                                  boolean bold,
                                                  boolean wrapText,
                                                  boolean centerAlign,
                                                  boolean verticalCenter,
                                                  boolean border,
                                                  IndexedColors bgColor) {
        CellStyle style = workbook.createCellStyle();

        // 字体设置
        Font font = workbook.createFont();
        font.setFontName(fontName != null ? fontName : "微软雅黑");
        font.setFontHeightInPoints(fontSize > 0 ? fontSize : 11);
        font.setBold(bold);
        style.setFont(font);

        // 自动换行
        style.setWrapText(wrapText);

        // 对齐方式
        if (centerAlign) {
            style.setAlignment(HorizontalAlignment.CENTER);
        }
        if (verticalCenter) {
            style.setVerticalAlignment(VerticalAlignment.CENTER);
        }

        // 设置边框
        if (border) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }

        // 背景填充
        if (bgColor != null) {
            style.setFillForegroundColor(bgColor.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        return style;
    }


}