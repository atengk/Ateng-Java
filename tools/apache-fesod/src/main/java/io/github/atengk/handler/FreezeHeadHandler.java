package io.github.atengk.handler;

import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.handler.context.SheetWriteHandlerContext;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Excel 冻结表头处理器（自动适配多级表头）
 * <p>
 * 功能说明：
 * 1. 只冻结表头区域
 * 2. 自动识别多级表头（单级 / 二级 / 三级…）
 * 3. 不冻结任何列
 * <p>
 * 冻结规则：
 * - 冻结行数 = Fesod 自动计算出的表头总行数
 * - 冻结列数 = 0
 * <p>
 * 适用场景：
 * - 导出模板
 * - 导入模板
 * - 多级表头 Excel
 * <p>
 * 使用示例：
 * FesodSheet.write(fileName, MyUser.class)
 * .registerWriteHandler(new FreezeHeadHandler())
 * .sheet("用户列表")
 * .doWrite(data);
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class FreezeHeadHandler implements SheetWriteHandler {

    @Override
    public void afterSheetCreate(SheetWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        if (sheet == null) {
            return;
        }

        /*
         * Fesod 已经帮我们算好了真实表头行数：
         * 单级表头 → 1
         * 二级表头 → 2
         * 三级表头 → 3
         * …
         */
        int headRowNumber = context.getWriteSheetHolder()
                .getExcelWriteHeadProperty()
                .getHeadRowNumber();

        if (headRowNumber <= 0) {
            return;
        }

        /*
         * 只冻结行，不冻结列：
         * colSplit = 0
         * rowSplit = 表头总行数
         */
        sheet.createFreezePane(0, headRowNumber);
    }
}
