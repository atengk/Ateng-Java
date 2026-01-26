package io.github.atengk.handler;

import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.handler.context.SheetWriteHandlerContext;
import org.apache.poi.ss.usermodel.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Excel 批注处理器（用于导入模板，自动适配多级表头）
 * <p>
 * 功能特点：
 * <ul>
 *     <li>支持多列不同批注</li>
 *     <li>自动识别表头层级，批注始终加在“最底层表头”</li>
 *     <li>兼容单级 / 多级表头</li>
 *     <li>非常适合 Excel 导入模板字段说明</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * Map<Integer, String> commentMap = new HashMap<>();
 * commentMap.put(0, "请输入用户姓名，必填");
 * commentMap.put(1, "年龄，必须是整数");
 * commentMap.put(3, "性别：男 / 女 / 未知");
 *
 * FesodSheet.write(file, User.class)
 *     .registerWriteHandler(new CommentHandler(commentMap))
 *     .sheet("用户列表")
 *     .doWrite(Collections.emptyList());
 * </pre>
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class CommentHandler implements SheetWriteHandler {

    /**
     * key：列索引（从 0 开始）
     * value：批注内容
     */
    private final Map<Integer, String> commentMap = new HashMap<>();

    public CommentHandler(Map<Integer, String> commentMap) {
        if (commentMap != null) {
            this.commentMap.putAll(commentMap);
        }
    }

    @Override
    public void afterSheetCreate(SheetWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        if (sheet == null || commentMap.isEmpty()) {
            return;
        }

        Workbook workbook = sheet.getWorkbook();
        CreationHelper factory = workbook.getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();

        /*
         * 表头总行数：
         * 单级表头 = 1
         * 二级表头 = 2
         * 三级表头 = 3
         */
        int headRowNumber = context.getWriteSheetHolder()
                .getExcelWriteHeadProperty()
                .getHeadRowNumber();

        /*
         * 真正字段所在行 = 最后一行表头
         */
        int realHeadRowIndex = headRowNumber - 1;

        for (Map.Entry<Integer, String> entry : commentMap.entrySet()) {
            Integer colIndex = entry.getKey();
            String commentText = entry.getValue();

            if (colIndex == null || commentText == null || commentText.trim().isEmpty()) {
                continue;
            }

            Row row = sheet.getRow(realHeadRowIndex);
            if (row == null) {
                row = sheet.createRow(realHeadRowIndex);
            }

            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                cell = row.createCell(colIndex);
            }

            // 批注显示区域（右下角弹出）
            ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(colIndex);
            anchor.setCol2(colIndex + 3);
            anchor.setRow1(realHeadRowIndex);
            anchor.setRow2(realHeadRowIndex + 4);

            Comment comment = drawing.createCellComment(anchor);
            comment.setString(factory.createRichTextString(commentText));
            comment.setAuthor("Ateng");

            cell.setCellComment(comment);
        }
    }
}
