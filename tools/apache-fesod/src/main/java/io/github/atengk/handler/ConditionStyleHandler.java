package io.github.atengk.handler;

import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.write.handler.CellWriteHandler;
import org.apache.fesod.sheet.write.handler.context.CellWriteHandlerContext;
import org.apache.fesod.sheet.write.metadata.style.WriteCellStyle;
import org.apache.fesod.sheet.write.metadata.style.WriteFont;
import org.apache.poi.ss.usermodel.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 条件样式处理器
 * <p>
 * 功能特性：
 * 1. 支持对指定列添加条件样式（列定位）
 * 2. 根据单元格值动态判定样式是否生效（条件断言）
 * 3. 样式与 Fesod 注解 / 默认样式可共存（基于 WriteCellData 模型）
 * <p>
 * 典型业务场景：
 * - 高分高亮（如分数 >= 90）
 * - 金额预警（如金额 > 10000）
 * - 状态标色（如状态 == "异常"）
 * - 风险数据标红
 * <p>
 * 样式合并机制说明：
 * Fesod 的样式最终由 WriteCellData 合并生成，因此必须通过：
 * WriteCellData -> WriteCellStyle -> WriteFont
 * 的链路注入，否则可能被覆盖。
 * <p>
 * 使用示例：
 * ConditionStyleHandler handler = new ConditionStyleHandler();
 * handler.addRule(3, new ConditionRule(v -> (Double) v > 10000)
 * .backgroundColor(IndexedColors.YELLOW.getIndex())
 * .fontColor(IndexedColors.RED.getIndex())
 * .bold(true));
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class ConditionStyleHandler implements CellWriteHandler {

    /**
     * 条件规则映射：key = 列索引（从0开始），value = 条件规则
     */
    private final Map<Integer, ConditionRule> ruleMap = new HashMap<>();

    /**
     * 注册某一列的条件规则
     *
     * @param columnIndex 列索引（从0开始）
     * @param rule        条件规则
     */
    public void addRule(int columnIndex, ConditionRule rule) {
        ruleMap.put(columnIndex, rule);
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {

        // 表头不处理
        if (Boolean.TRUE.equals(context.getHead())) {
            return;
        }

        Cell cell = context.getCell();
        if (cell == null) {
            return;
        }

        // 列命中规则才处理
        int columnIndex = cell.getColumnIndex();
        ConditionRule rule = ruleMap.get(columnIndex);
        if (rule == null) {
            return;
        }

        // 获取真实值用于条件判断
        Object value = getCellValue(cell);
        if (value == null || !rule.getPredicate().test(value)) {
            return;
        }

        // 必须通过 WriteCellData 来设置样式，否则可能被覆盖
        WriteCellData<?> cellData = context.getFirstCellData();
        if (cellData == null) {
            return;
        }

        // 获取或创建样式对象（不会覆盖注解样式）
        WriteCellStyle style = cellData.getOrCreateStyle();

        // 设置背景色
        if (rule.getBackgroundColor() != null) {
            style.setFillForegroundColor(rule.getBackgroundColor());
            style.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        }

        // 设置字体（颜色、加粗）
        if (rule.getFontColor() != null || rule.isBold()) {
            WriteFont font = style.getWriteFont();
            if (font == null) {
                font = new WriteFont();
                style.setWriteFont(font);
            }
            if (rule.isBold()) {
                font.setBold(true);
            }
            if (rule.getFontColor() != null) {
                font.setColor(rule.getFontColor());
            }
        }
    }

    /**
     * 读取单元格的实际值
     * <p>
     * 注意：
     * Excel 内部仅存储基本类型，例如：
     * - 字符串 -> STRING
     * - 数字/日期 -> NUMERIC
     * - 布尔值 -> BOOLEAN
     * <p>
     * 特别说明（非常重要）：
     * ================
     * Excel 没有 LocalDateTime / LocalDate 类型
     * 时间在存储时会被转换为 Double（序列号），例如：
     * 2026-01-26 10:30:00 -> 46048.4375
     * <p>
     * 所以如果你导出的实体字段是 LocalDateTime，读取时只能拿到 Double。
     *
     * @param cell 当前单元格
     * @return 单元格中的有效数据
     */
    private Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 日期也会走这里
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return null;
        }
    }

    /**
     * 条件规则定义（链式构建）
     */
    public static class ConditionRule {

        /**
         * 条件断言：用于判定是否触发样式
         */
        private final Predicate<Object> predicate;

        /**
         * 背景色（POI 的 IndexedColors 索引）
         */
        private Short backgroundColor;

        /**
         * 字体颜色（POI 的 IndexedColors 索引）
         */
        private Short fontColor;

        /**
         * 是否加粗
         */
        private boolean bold;

        public ConditionRule(Predicate<Object> predicate) {
            this.predicate = predicate;
        }

        public Predicate<Object> getPredicate() {
            return predicate;
        }

        public Short getBackgroundColor() {
            return backgroundColor;
        }

        public ConditionRule backgroundColor(Short backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Short getFontColor() {
            return fontColor;
        }

        public ConditionRule fontColor(Short fontColor) {
            this.fontColor = fontColor;
            return this;
        }

        public boolean isBold() {
            return bold;
        }

        public ConditionRule bold(boolean bold) {
            this.bold = bold;
            return this;
        }
    }
}
