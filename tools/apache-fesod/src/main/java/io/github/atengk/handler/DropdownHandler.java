package io.github.atengk.handler;

import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.handler.context.SheetWriteHandlerContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;

import java.util.HashMap;
import java.util.Map;

/**
 * Excel 下拉框处理器（用于导入模板）
 *
 * 特点：
 * 1. 支持多列不同下拉框
 * 2. 默认从数据行开始，一直到 Excel 最大行
 * 3. 适用于“只有表头 / 只有一行示例数据”的模板场景
 *
 * 使用示例：
 * Map<Integer, String[]> dropdownMap = new HashMap<>();
 * dropdownMap.put(1, new String[]{"1", "2"});
 * dropdownMap.put(3, new String[]{"男", "女"});
 *
 * EasyExcel.write(file, User.class)
 *     .registerWriteHandler(new DropdownHandler(dropdownMap, 1))
 *     .sheet().doWrite(Collections.emptyList());
 *
 * 说明：
 * rowStart = 1 表示从第 2 行开始（第 1 行是表头）
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class DropdownHandler implements SheetWriteHandler {

    /**
     * key：列索引（从 0 开始）
     * value：该列的下拉选项
     */
    private final Map<Integer, String[]> dropdownMap = new HashMap<>();

    /**
     * 下拉生效起始行（通常是 1，跳过表头）
     */
    private final int startRow;

    /**
     * Excel 最大行（XSSF 是 1048575）
     */
    private static final int EXCEL_MAX_ROW = 1_048_575;

    public DropdownHandler(Map<Integer, String[]> dropdownMap, int startRow) {
        if (dropdownMap != null) {
            this.dropdownMap.putAll(dropdownMap);
        }
        this.startRow = startRow < 0 ? 0 : startRow;
    }

    @Override
    public void afterSheetCreate(SheetWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        if (sheet == null || dropdownMap.isEmpty()) {
            return;
        }

        DataValidationHelper helper = sheet.getDataValidationHelper();

        for (Map.Entry<Integer, String[]> entry : dropdownMap.entrySet()) {
            Integer colIndex = entry.getKey();
            String[] options = entry.getValue();

            if (colIndex == null || options == null || options.length == 0) {
                continue;
            }

            // 下拉框约束
            DataValidationConstraint constraint =
                    helper.createExplicitListConstraint(options);

            // 整列生效：从 startRow 到 Excel 最大行
            CellRangeAddressList addressList =
                    new CellRangeAddressList(startRow, EXCEL_MAX_ROW, colIndex, colIndex);

            DataValidation validation =
                    helper.createValidation(constraint, addressList);

            // 兼容 Excel 行为
            validation.setSuppressDropDownArrow(true);
            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("输入错误", "请从下拉列表中选择合法值");

            sheet.addValidationData(validation);
        }
    }
}
