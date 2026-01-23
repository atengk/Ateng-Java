# EasyPoi

EasyPOI 是一个基于 Apache POI 的 Java Excel 工具框架，封装了导入、导出和模板填充等常用功能。它通过注解和模板方式大幅简化 Excel 操作，支持复杂表头、样式继承、图片、多 Sheet 以及大数据量处理，特别适合报表、对账单和固定格式文档的快速开发。

- [参考文档链接](https://www.yuque.com/guomingde/easypoi/pc8qzzvkqbvsq5v0)



## 基础配置

**添加依赖**

```xml
<!-- Easy Poi -->
<dependency>
    <groupId>cn.afterturn</groupId>
    <artifactId>easypoi-spring-boot-starter</artifactId>
    <version>4.5.0</version>
</dependency>
```



## 数据准备

### 创建实体类

```java
package io.github.atengk.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 手机号码
     */
    private String phoneNumber;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 比例
     */
    private Double ratio;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 所在省份
     */
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
```

### 给实体类添加 `@Excel` 注解

EasyPoi 默认**不会自动映射字段**，必须通过 `@Excel` 显式标注需要导出的字段。

| 参数名                | 类型     | 默认值                | 示例                    | 功能说明                                |
| --------------------- | -------- | --------------------- | ----------------------- | --------------------------------------- |
| `name`                | String   | —                     | `"姓名"`                | Excel 列名（必填）                      |
| `orderNum`            | String   | `"0"`                 | `"1"`                   | 列排序，支持 `a_id` 方式                |
| `width`               | double   | `10`                  | `20`                    | 列宽（字符单位，1中文=2字符）           |
| `type`                | int      | `1`                   | `2`                     | 1文本，2图片，3函数，10数字，11特殊符号 |
| `groupName`           | String   | `""`                  | `"基本信息"`            | 表头分组（双行显示）                    |
| `suffix`              | String   | `""`                  | `"%"`                   | 显示后缀，如 `90 → 90%`                 |
| `isWrap`              | boolean  | `true`                | `false`                 | 是否换行（支持`\n`）                    |
| `mergeVertical`       | boolean  | `false`               | `true`                  | 相同内容自动纵向合并                    |
| `mergeRely`           | int[]    | `{}`                  | `{1}`                   | 依赖列自动合并                          |
| `needMerge`           | boolean  | `false`               | `true`                  | List模式下纵向合并                      |
| `isColumnHidden`      | boolean  | `false`               | `true`                  | 隐藏该列                                |
| `fixedIndex`          | int      | `-1`                  | `0`                     | 固定列位置                              |
| `numFormat`           | String   | `""`                  | `"#.##"`                | 数字格式化（DecimalFormat）             |
| `databaseFormat`      | String   | `"yyyyMMddHHmmss"`    | `"yyyy-MM-dd"`          | DB 字符串日期转换格式                   |
| `exportFormat`        | String   | `""`                  | `"yyyy-MM-dd"`          | 导出日期格式                            |
| `importFormat`        | String   | `""`                  | `"yyyy-MM-dd HH:mm:ss"` | 导入日期格式                            |
| `format`              | String   | `""`                  | `"yyyy-MM-dd"`          | 同时指定 `export+import`                |
| `timezone`            | String   | `""`                  | `"GMT+8"`               | 日期时区                                |
| `replace`             | String[] | `{}`                  | `{"男_1","女_2"}`       | 字段替换（导入导出双向）                |
| `dict`                | String   | `""`                  | `"sex"`                 | 数据字典名称                            |
| `addressList`         | boolean  | `false`               | `true`                  | 下拉（使用 replace 或 dict）            |
| `isStatistics`        | boolean  | `false`               | `true`                  | 自动统计数字列（最后一行求和）          |
| `isHyperlink`         | boolean  | `false`               | `true`                  | 是否超链接，需要实现接口                |
| `imageType`           | int      | `1`                   | `2`                     | 图片来源：1文件，2数据库                |
| `savePath`            | String   | `"/excel/upload/img"` | `"/img/save"`           | 图片导入保存路径                        |
| `isImportField`       | String   | `"false"`             | `"true"`                | 导入字段检查是否存在                    |
| `enumExportField`     | String   | `""`                  | `"value"`               | 枚举导出字段                            |
| `enumImportMethod`    | String   | `""`                  | `"getByValue"`          | 枚举导入方法                            |
| `desensitizationRule` | String   | `""`                  | `"6_4"`                 | 数据脱敏规则（身份证、手机号等）        |
| `height`              | double   | `10`                  | `15`                    | （Deprecated）建议用样式设置行高        |

```java
package io.github.atengk.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Excel(name = "用户ID", width = 15, type = 10) // type=10 表示数字（Long）
    private Long id;

    /**
     * 名称
     */
    @Excel(name = "姓名", width = 12)
    private String name;

    /**
     * 年龄
     */
    @Excel(name = "年龄", width = 8, type = 10)
    private Integer age;

    /**
     * 手机号码
     */
    @Excel(name = "手机号", width = 15)
    private String phoneNumber;

    /**
     * 邮箱
     */
    @Excel(name = "邮箱", width = 20)
    private String email;

    /**
     * 分数
     */
    @Excel(name = "分数", width = 10, type = 10, format = "#,##0.00")
    private BigDecimal score;

    /**
     * 比例
     */
    @Excel(name = "比例", width = 12, type = 10, format = "0.00000%")
    private Double ratio;

    /**
     * 生日
     */
    @Excel(name = "生日", width = 12, format = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @Excel(name = "省份", width = 10)
    private String province;

    /**
     * 所在城市
     */
    @Excel(name = "城市", width = 10)
    private String city;

    /**
     * 创建时间
     */
    @Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
```

### 初始化数据

```java
package io.github.atengk.init;

import com.github.javafaker.Faker;
import io.github.atengk.entity.MyUser;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 初始化数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@Getter
public class InitData {

    public static List<MyUser> getDataList() {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        List<MyUser> userList = new ArrayList();
        for (int i = 1; i <= 1000; i++) {
            MyUser user = new MyUser();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setAge(faker.number().numberBetween(0, 1));
            user.setPhoneNumber(faker.phoneNumber().cellPhone());
            user.setEmail(faker.internet().emailAddress());
            user.setScore(BigDecimal.valueOf(faker.number().randomDouble(2, 0, 100)));
            user.setRatio(faker.number().randomDouble(5, 0, 1));
            user.setBirthday(LocalDate.now());
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        return userList;
    }

}
```



## 导出 Excel（Export）

### 简单对象导出（单表头）

```java
    @Test
    public void testSimpleExport() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 2. 配置导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 使用 EasyPoi 直接生成 Workbook
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入本地文件
        String filePath = Paths.get("target", "simple_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        // 5. 关闭 workbook（释放资源）
        workbook.close();

        System.out.println("✅ 导出成功！文件路径: " + filePath);
    }
```

![image-20260121163310928](./assets/image-20260121163310928.png)

### 多级表头导出（合并单元格）

在 EasyPoi 中，多级表头通过 `@Excel` 注解的 `groupName` 属性实现。同一 `groupName` 的字段会被归到一个父级表头下，并自动合并单元格。

假设我们希望 Excel 表头结构如下：

```
| 基本信息        | 联系方式      | 成绩信息        | 地理位置   | 时间信息         |
| 用户ID | 姓名 | 年龄 | 手机号 | 邮箱 | 分数 | 比例 | 省份 | 城市 | 生日       | 创建时间           |
```

修改 `MyUser` 实体类，添加 `groupName` 和 `orderNum`

> 如果不配置 orderNum ，最终导出的数据分组数据会乱

```java
package io.github.atengk.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Excel(name = "用户ID", width = 15, type = 10, groupName = "基本信息", orderNum = "1")
    private Long id;

    /**
     * 名称
     */
    @Excel(name = "姓名", width = 12, groupName = "基本信息", orderNum = "2")
    private String name;

    /**
     * 年龄
     */
    @Excel(name = "年龄", width = 8, type = 10, groupName = "基本信息", orderNum = "3")
    private Integer age;

    /**
     * 手机号码
     */
    @Excel(name = "手机号", width = 15, groupName = "联系方式", orderNum = "4")
    private String phoneNumber;

    /**
     * 邮箱
     */
    @Excel(name = "邮箱", width = 20, groupName = "联系方式", orderNum = "5")
    private String email;

    /**
     * 分数
     */
    @Excel(name = "分数", width = 10, type = 10, format = "#,##0.00", groupName = "成绩信息", orderNum = "6")
    private BigDecimal score;

    /**
     * 比例
     */
    @Excel(name = "比例", width = 12, type = 10, format = "0.00000%", groupName = "成绩信息", orderNum = "7")
    private Double ratio;

    /**
     * 生日
     */
    @Excel(name = "生日", width = 12, format = "yyyy-MM-dd", groupName = "时间信息", orderNum = "8")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @Excel(name = "省份", width = 10, groupName = "地理位置", orderNum = "9")
    private String province;

    /**
     * 所在城市
     */
    @Excel(name = "城市", width = 10, groupName = "地理位置", orderNum = "10")
    private String city;

    /**
     * 创建时间
     */
    @Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss", groupName = "时间信息", orderNum = "11")
    private LocalDateTime createTime;

}
```

使用方法

```java
    @Test
    public void testMultiHeaderExport() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 2. 配置导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户数据（多级表头）");

        // 3. 导出
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入文件
        String filePath = Paths.get("target", "multi_header_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 多级表头导出成功！路径: " + filePath);
    }
```

![image-20260121164012214](./assets/image-20260121164012214.png)

### 合并单元格

| 合并方式             | 使用的注解参数         | 行为说明                       |
| -------------------- | ---------------------- | ------------------------------ |
| 基于内容相同纵向合并 | `mergeVertical = true` | 同一列连续相同内容自动纵向合并 |
| 依赖其他列合并       | `mergeRely = {列索引}` | 只有当依赖列也相同时才合并     |

#### 纵向单列合并

- 相同省份 → 合并
- 相同城市 → 合并

**实体注解添加参数**

```java
@Excel(name = "省份", width = 10, groupName = "地理位置", orderNum = "9", mergeVertical = true)
private String province;

@Excel(name = "城市", width = 10, groupName = "地理位置", orderNum = "10", mergeVertical = true)
private String city;
```

**使用方法**

```java
    @Test
    public void testSimpleMergeExport() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 数据按照省份+城市排序
        userList.sort(Comparator
                .comparing(MyUser::getProvince)
                .thenComparing(MyUser::getCity));

        // 2. 配置导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 使用 EasyPoi 生成 Workbook
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入本地文件
        String filePath = Paths.get("target", "simple_export_merge_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 导出成功！文件路径: " + filePath);
    }
```

![image-20260123102411429](./assets/image-20260123102411429.png)



### 自定义样式

#### 基础使用

**创建自定义样式处理器**

```java
package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * 自定义 Excel 样式处理器
 * 支持：
 * - 表头样式（加粗、居中、背景色）
 * - 普通单元格样式（左/中/右对齐）
 * - 数字/特殊字段样式
 * - 斑马纹行（可扩展）
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class MyExcelStyle extends AbstractExcelExportStyler {

    public MyExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    /**
     * 表头样式（默认居中、加粗、灰色背景）
     */
    @Override
    public CellStyle getTitleStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 表头多级样式复用 getTitleStyle
     */
    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    /**
     * 普通单元格（左对齐）
     */
    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 数字/特殊字段样式（右对齐）
     */
    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 居中样式（可直接用于数字或文字列）
     */
    public CellStyle stringCenterStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 设置单元格细边框
     */
    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * 模板 foreach 场景，直接复用普通样式
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return this.stringNoneStyle;
    }

    /**
     * 覆盖默认奇偶行逻辑，全部使用普通样式
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return this.stringNoneStyle;
    }

    /**
     * 根据单元格内容返回最终样式，默认全部左对齐
     */
    @Override
    public CellStyle getStyles(Cell cell, int dataRow, ExcelExportEntity entity, Object obj, Object data) {
        return this.stringNoneStyle;
    }
}
```

**使用方法**

```java
    @Test
    public void testStyledExport() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams();
        params.setSheetName("用户数据（带样式）");

        // 设置自定义样式处理器
        params.setStyle(MyExcelStyle.class);

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "styled_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 带样式的 Excel 导出成功！路径: " + filePath);
    }
```

![image-20260121170414455](./assets/image-20260121170414455.png)

------

#### 居中样式（表格整齐、美观）

```java
package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * 全部内容居中对齐的 Excel 样式处理器
 * 特点：
 * 1. 表头居中 + 加粗 + 背景色
 * 2. 所有数据列（文本、数字、日期等）全部水平、垂直居中
 * 3. 不使用 EasyPOI 默认的奇偶行斑马纹逻辑
 * 4. 常用于报表型、展示型 Excel，视觉最规整
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class CenterAlignExcelStyle extends AbstractExcelExportStyler {

    /**
     * 构造器中必须调用 createStyles
     */
    public CenterAlignExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    /**
     * 表头样式（居中、加粗、背景色）
     */
    @Override
    public CellStyle getTitleStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 多级表头样式，直接复用表头样式
     */
    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    /**
     * 普通字符串样式（全部居中）
     */
    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 数字/特殊字段样式（同样全部居中）
     */
    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 模板 foreach 场景，复用普通居中样式
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return this.stringNoneStyle;
    }

    /**
     * 覆盖 EasyPOI 默认的奇偶行样式逻辑，全部统一为居中样式
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return this.stringNoneStyle;
    }

    /**
     * 根据单元格内容返回最终样式，强制所有内容居中
     */
    @Override
    public CellStyle getStyles(Cell cell,
                               int dataRow,
                               ExcelExportEntity entity,
                               Object obj,
                               Object data) {
        return this.stringNoneStyle;
    }

    /**
     * 统一设置细边框
     */
    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
```

![image-20260122121022151](./assets/image-20260122121022151.png)

------

#### 斑马纹样式（奇偶行交替底色）

```java
package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * 斑马纹 Excel 样式处理器（奇偶行交替背景色）
 *
 * 特点：
 * 1. 表头：加粗 + 居中 + 深灰色背景
 * 2. 数据行：
 *    - 偶数行：白色背景
 *    - 奇数行：浅灰色背景
 * 3. 所有内容统一居中显示
 * 4. 适合数据量大、需要快速区分行的表格
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class ZebraStripeExcelStyle extends AbstractExcelExportStyler {

    /**
     * 构造器必须调用 createStyles
     */
    public ZebraStripeExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    /**
     * 表头样式
     */
    @Override
    public CellStyle getTitleStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 多级表头复用
     */
    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    /**
     * 偶数行样式（白色背景，居中）
     */
    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 奇数行样式（浅灰色背景，居中）
     */
    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 模板 foreach 直接用偶数行样式
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return this.stringNoneStyle;
    }

    /**
     * 覆盖默认奇偶逻辑：
     * true  → 使用 stringNoneStyle（偶数行）
     * false → 使用 stringSeptailStyle（奇数行）
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return noneStyler ? this.stringNoneStyle : this.stringSeptailStyle;
    }

    /**
     * 根据数据行号决定斑马纹
     * dataRow 从 0 开始：
     *  - 偶数行 → 白色
     *  - 奇数行 → 灰色
     */
    @Override
    public CellStyle getStyles(Cell cell,
                               int dataRow,
                               ExcelExportEntity entity,
                               Object obj,
                               Object data) {
        if (dataRow % 2 == 0) {
            return this.stringNoneStyle;
        }
        return this.stringSeptailStyle;
    }

    /**
     * 统一细边框
     */
    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
```

![image-20260122152902260](./assets/image-20260122152902260.png)

------

#### 表头高亮 + 数据居中

```java
package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * 表头高亮 + 数据全部居中 样式处理器
 *
 * 特点：
 * 1. 表头：
 *    - 加粗
 *    - 居中
 *    - 金黄色背景（醒目、偏“报表系统风格”）
 * 2. 数据行：
 *    - 所有列统一居中
 *    - 无斑马纹（纯净、规整）
 * 3. 适合：
 *    - 统计报表
 *    - 汇总数据
 *    - 领导查看型 Excel
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class HeaderHighlightCenterAlignExcelStyle extends AbstractExcelExportStyler {

    public HeaderHighlightCenterAlignExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    /**
     * 表头样式：高亮背景 + 加粗 + 居中
     */
    @Override
    public CellStyle getTitleStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 高亮：金黄色背景
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 多级表头复用同一套高亮样式
     */
    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    /**
     * 普通数据样式：全部居中
     */
    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 数字/特殊字段样式：同样全部居中
     */
    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
        return stringNoneStyle(workbook, isWrap);
    }

    /**
     * 模板 foreach 直接复用普通样式
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return this.stringNoneStyle;
    }

    /**
     * 关闭 EasyPOI 默认的奇偶行处理，全部使用同一套样式
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return this.stringNoneStyle;
    }

    /**
     * 所有数据单元格统一居中
     */
    @Override
    public CellStyle getStyles(Cell cell,
                               int dataRow,
                               ExcelExportEntity entity,
                               Object obj,
                               Object data) {
        return this.stringNoneStyle;
    }

    /**
     * 统一细边框
     */
    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
```

![image-20260122153145866](./assets/image-20260122153145866.png)

#### 通用样式

```java
package io.github.atengk.style;

import cn.afterturn.easypoi.entity.BaseTypeConstants;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * Excel 导出样式策略实现类。
 *
 * <p>样式适用范围：</p>
 * <ul>
 *     <li>普通导出场景</li>
 *     <li>模板渲染场景（Foreach）</li>
 * </ul>
 *
 * <p>样式策略说明：</p>
 * <ul>
 *     <li>表头类单元格：加粗、灰底、水平垂直居中</li>
 *     <li>文本类单元格：左对齐</li>
 *     <li>数字类单元格：右对齐</li>
 *     <li>日期类单元格：居中</li>
 *     <li>图片类型单元格：居中</li>
 *     <li>所有单元格统一配置细边框</li>
 * </ul>
 *
 * <p>基于 EasyPOI 类型常量 {@link BaseTypeConstants} 映射样式。</p>
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class CustomExcelExportStyler extends AbstractExcelExportStyler {

    /**
     * 表头单元格样式（灰底、加粗、居中对齐）
     */
    private final CellStyle headerCenterStyle;

    /**
     * 文本单元格样式（左对齐）
     */
    private final CellStyle textLeftStyle;

    /**
     * 数字单元格样式（右对齐）
     */
    private final CellStyle numberRightStyle;

    /**
     * 日期单元格样式（居中对齐）
     */
    private final CellStyle dateCenterStyle;

    /**
     * 图片单元格样式（居中对齐）
     */
    private final CellStyle imageCenterStyle;

    /**
     * 构造函数。
     *
     * @param workbook Excel 工作簿实例
     */
    public CustomExcelExportStyler(Workbook workbook) {
        super.createStyles(workbook);
        this.headerCenterStyle = createHeaderStyle();
        this.textLeftStyle = createTextStyle();
        this.numberRightStyle = createNumberStyle();
        this.dateCenterStyle = createDateStyle();
        this.imageCenterStyle = createImageStyle();
    }

    /**
     * 创建表头样式。
     * <p>配置内容：</p>
     * <ul>
     *     <li>字体加粗</li>
     *     <li>灰色背景填充</li>
     *     <li>水平垂直居中</li>
     *     <li>细边框</li>
     * </ul>
     *
     * @return 表头样式
     */
    private CellStyle createHeaderStyle() {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setThinBorder(style);

        return style;
    }

    /**
     * 创建文本类型样式（左对齐）。
     *
     * @return 文本样式
     */
    private CellStyle createTextStyle() {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setThinBorder(style);

        return style;
    }

    /**
     * 创建数字类型样式（右对齐）。
     *
     * @return 数字样式
     */
    private CellStyle createNumberStyle() {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setThinBorder(style);

        return style;
    }

    /**
     * 创建日期类型样式（居中）。
     *
     * @return 日期样式
     */
    private CellStyle createDateStyle() {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setThinBorder(style);

        return style;
    }

    /**
     * 创建图片样式
     *
     * @return 图片样式
     */
    private CellStyle createImageStyle() {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(style);
        return style;
    }

    /**
     * 表头样式适配（调用自定义样式）。
     *
     * @param color 表头颜色（框架传入）
     * @return 表头样式
     */
    @Override
    public CellStyle getTitleStyle(short color) {
        return headerCenterStyle;
    }

    /**
     * 多级表头样式适配。
     *
     * @param color 表头颜色
     * @return 表头样式
     */
    @Override
    public CellStyle getHeaderStyle(short color) {
        return headerCenterStyle;
    }

    /**
     * 样式选择入口（无 Cell 上下文版本）。
     *
     * <p>该方法用于注解模式导出，框架只提供字段元数据 entity。
     * 根据源码 type 值判断样式：</p>
     * <ul>
     *     <li>1 → 文本</li>
     *     <li>2 → 图片</li>
     *     <li>3 → 函数</li>
     *     <li>10 → 数字</li>
     *     <li>11 → 特殊符号</li>
     *     <li>其他/空 → 默认文本</li>
     * </ul>
     *
     * @param noneStyler 是否忽略框架默认样式
     * @param entity     字段元数据对象
     * @return CellStyle 样式实例
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        if (entity == null) {
            return textLeftStyle;
        }

        Integer type = entity.getType();

        // 定义字段类型常量（替代魔法值）
        final int TYPE_TEXT = 1;
        final int TYPE_IMAGE = 2;
        final int TYPE_FUNCTION = 3;
        final int TYPE_NUMBER = 10;
        final int TYPE_SPECIAL = 11;

        if (type == null || type == TYPE_TEXT) {
            // 文本类型
            return textLeftStyle;
        } else if (type == TYPE_IMAGE) {
            // 图片类型
            return imageCenterStyle;
        } else if (type == TYPE_FUNCTION) {
            // 函数类型（可居中或文本）
            return textLeftStyle;
        } else if (type == TYPE_NUMBER) {
            // 数字类型
            return numberRightStyle;
        } else if (type == TYPE_SPECIAL) {
            // 特殊符号
            return textLeftStyle;
        } else {
            // 默认文本
            return textLeftStyle;
        }
    }

    /**
     * 样式选择入口（带 Cell 上下文版本）。
     *
     * <p>该方法在动态渲染、Foreach 模板或模板填充模式下触发，
     * 框架会提供 Cell、行号、值等上下文信息。
     * 样式决策基于 Java 数据类型进行，不再依赖框架内部常量。</p>
     *
     * <p>样式映射规则：</p>
     * <ul>
     *     <li>表头行：headerCenterStyle</li>
     *     <li>Number 类型 → numberRightStyle（右对齐）</li>
     *     <li>Date / Calendar → dateCenterStyle（居中）</li>
     *     <li>Boolean → textLeftStyle（左对齐）</li>
     *     <li>Byte[] / InputStream → dateCenterStyle（居中，可用于图片）</li>
     *     <li>其他类型 → textLeftStyle（左对齐）</li>
     * </ul>
     *
     * @param cell   当前 POI Cell 对象
     * @param row    数据行行号（不含表头）
     * @param entity 字段元数据对象
     * @param obj    当前整行数据对象
     * @param value  字段对应的原始值
     * @return CellStyle 样式实例
     */
    @Override
    public CellStyle getStyles(Cell cell, int row, ExcelExportEntity entity, Object obj, Object value) {
        // 如果当前行是表头或表头级别（可通过行号判断）
        if (row < 0) {
            return headerCenterStyle;
        }

        // 根据 Java 类型决定样式
        if (value == null) {
            return textLeftStyle;
        }
        if (value instanceof Number) {
            return numberRightStyle;
        }
        if (value instanceof java.util.Date || value instanceof java.util.Calendar) {
            return dateCenterStyle;
        }
        if (value instanceof Boolean) {
            return textLeftStyle;
        }
        if (value instanceof byte[] || value instanceof java.io.InputStream) {
            return dateCenterStyle;
        }

        // 默认文本
        return textLeftStyle;
    }

    /**
     * Foreach 模板渲染使用的样式选择。
     *
     * <p>该方法在处理 `{{$fe:list t.name}}` 等模板语法时触发，
     * 字段元数据需从 ExcelForEachParams 中提取，因此与注解模式入口分离。</p>
     *
     * @param isSingle 是否为单列渲染（框架内部字段）
     * @param params   Foreach 参数对象，包含字段元数据
     * @return CellStyle 样式实例
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams params) {
        // 只能基于模板的 CellStyle 或字段类型来返回样式
        // 因为模板渲染不会传入实际对象值
        if (params.getCellStyle() != null) {
            // 优先使用模板自带样式
            return params.getCellStyle();
        }
        // 默认文本左对齐
        return textLeftStyle;
    }

    /**
     * 设置细边框，增强单元格视觉边界。
     *
     * @param style 单元格样式对象
     */
    private void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
```

![image-20260123101543171](./assets/image-20260123101543171.png)

------

### 条件样式

#### 创建函数接口

```java
package io.github.atengk.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * 单元格样式处理器函数接口
 *
 * @author 孔余
 * @since 2026-01-22
 */
@FunctionalInterface
public interface CellStyleHandler {

    /**
     * 对单个单元格执行样式处理逻辑。
     *
     * @param workbook 当前工作簿对象，用于创建和复用 CellStyle
     * @param cell     当前需要处理的单元格
     */
    void handle(Workbook workbook, Cell cell);
}
```

#### 创建样式工具类

```java
package io.github.atengk.util;

import org.apache.poi.ss.usermodel.*;

/**
 * Excel 样式后处理工具类。
 * <p>
 * 适用于 EasyPOI 导出完成后的 Workbook 二次加工场景，
 * 通过“表头名称”定位指定列，对该列下所有数据单元格统一应用自定义样式规则。
 * </p>
 * <p>
 * 设计思想：
 * <ul>
 *     <li>不依赖 EasyPOI 内部样式回调机制，避免样式不生效问题</li>
 *     <li>直接基于 Apache POI 对 Workbook 进行后处理，稳定可控</li>
 *     <li>以“表头名称”为唯一定位依据，避免列顺序变动导致样式失效</li>
 * </ul>
 * <p>
 * 主要能力：
 * <ul>
 *     <li>支持通过 Sheet 下标或 Sheet 名称定位工作表</li>
 *     <li>支持自动扫描多行表头（1 行 / 2 行 / 3 行…）</li>
 *     <li>自动从表头下一行作为数据起始行</li>
 *     <li>支持为指定列批量应用任意样式策略</li>
 * </ul>
 * <p>
 * 典型使用示例：
 * <pre>
 * ExcelStyleUtil.applyByTitle(workbook, 0, "分数", 5, (wb, cell) -> {
 *     int score = (int) cell.getNumericCellValue();
 *     if (score < 60) {
 *         CellStyle style = wb.createCellStyle();
 *         style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
 *         style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
 *         style.setAlignment(HorizontalAlignment.CENTER);
 *         cell.setCellStyle(style);
 *     }
 * });
 * </pre>
 * <p>
 * 该类是一个纯工具类，不允许被实例化。
 *
 * @author 孔余
 * @since 2026-01-22
 */
public final class ExcelStyleUtil {

    private ExcelStyleUtil() {
    }

    /**
     * 通过表头名称，对指定列应用样式处理。
     * 默认 Sheet 第 1 个，最大扫描表头行数为 3
     *
     * @param workbook  工作簿对象
     * @param titleName 表头名称，例如：分数、年龄、状态
     * @param handler   单元格样式处理器
     */
    public static void applyByTitle(
            Workbook workbook,
            String titleName,
            CellStyleHandler handler) {

        Sheet sheet = workbook.getSheetAt(0);
        applyInternal(workbook, sheet, titleName, 3, handler);
    }

    /**
     * 通过 Sheet 下标 + 表头名称，对指定列应用样式处理。
     *
     * @param workbook         工作簿对象
     * @param sheetIndex       Sheet 下标，从 0 开始
     * @param titleName        表头名称，例如：分数、年龄、状态
     * @param maxHeaderRowScan 最大扫描表头行数，用于适配多行表头结构
     *                         通常取值 3~5 即可
     * @param handler          单元格样式处理器
     */
    public static void applyByTitle(
            Workbook workbook,
            int sheetIndex,
            String titleName,
            int maxHeaderRowScan,
            CellStyleHandler handler) {

        Sheet sheet = workbook.getSheetAt(sheetIndex);
        applyInternal(workbook, sheet, titleName, maxHeaderRowScan, handler);
    }

    /**
     * 通过 Sheet 名称 + 表头名称，对指定列应用样式处理。
     *
     * @param workbook         工作簿对象
     * @param sheetName        Sheet 名称
     * @param titleName        表头名称，例如：分数、年龄、状态
     * @param maxHeaderRowScan 最大扫描表头行数，用于适配多行表头结构
     * @param handler          单元格样式处理器
     */
    public static void applyByTitle(
            Workbook workbook,
            String sheetName,
            String titleName,
            int maxHeaderRowScan,
            CellStyleHandler handler) {

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            return;
        }
        applyInternal(workbook, sheet, titleName, maxHeaderRowScan, handler);
    }

    /**
     * 内部统一处理逻辑。
     * <p>
     * 主要流程：
     * <ol>
     *     <li>在前 N 行中定位表头所在行与列</li>
     *     <li>以表头行的下一行作为数据起始行</li>
     *     <li>对目标列的所有单元格逐个执行样式处理器</li>
     * </ol>
     */
    private static void applyInternal(
            Workbook workbook,
            Sheet sheet,
            String titleName,
            int maxHeaderRowScan,
            CellStyleHandler handler) {

        HeaderLocation location = findHeader(sheet, titleName, maxHeaderRowScan);
        if (location == null) {
            return;
        }

        int headerRowIndex = location.headerRowIndex;
        int colIndex = location.colIndex;

        int firstDataRowIndex = headerRowIndex + 1;

        for (int rowIndex = firstDataRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                continue;
            }

            handler.handle(workbook, cell);
        }
    }

    /**
     * 在指定的前 N 行中扫描表头名称，返回表头所在的行号与列号。
     *
     * @param sheet            当前 Sheet
     * @param titleName        表头名称
     * @param maxHeaderRowScan 最大扫描行数
     * @return 表头位置信息，未找到返回 null
     */
    private static HeaderLocation findHeader(
            Sheet sheet,
            String titleName,
            int maxHeaderRowScan) {

        int scanLimit = Math.min(maxHeaderRowScan, sheet.getLastRowNum() + 1);

        for (int rowIndex = 0; rowIndex < scanLimit; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
                Cell cell = row.getCell(colIndex);
                if (cell == null) {
                    continue;
                }

                if (cell.getCellType() == CellType.STRING
                        && titleName.equals(cell.getStringCellValue().trim())) {
                    return new HeaderLocation(rowIndex, colIndex);
                }
            }
        }
        return null;
    }

    /**
     * 表头定位结果封装对象。
     * <p>
     * 用于同时返回：
     * <ul>
     *     <li>表头所在行号</li>
     *     <li>表头所在列号</li>
     * </ul>
     */
    private static final class HeaderLocation {

        private final int headerRowIndex;
        private final int colIndex;

        private HeaderLocation(int headerRowIndex, int colIndex) {
            this.headerRowIndex = headerRowIndex;
            this.colIndex = colIndex;
        }
    }
}
```

#### 使用示例

##### 基本使用

```java
    @Test
    public void testConditionStyledExport() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams();
        String sheetName = "用户数据（带样式）";
        params.setSheetName(sheetName);

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 条件样式
        ExcelStyleUtil.applyByTitle(workbook, "分数", (wb, cell) -> {
            int score;
            try {
                if (cell.getCellType() == CellType.NUMERIC) {
                    score = (int) cell.getNumericCellValue();
                } else {
                    score = Integer.parseInt(cell.getStringCellValue());
                }
            } catch (Exception e) {
                return;
            }

            CellStyle style = wb.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);

            if (score < 60) {
                style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            } else if (score > 90) {
                style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            } else {
                style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            }

            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);
        });

        String filePath = Paths.get("target", "condition_styled_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 带样式的 Excel 导出成功！路径: " + filePath);
    }
```

![image-20260122170145594](./assets/image-20260122170145594.png)

##### 使用 Sheet 下标

```java
    @Test
    public void testConditionStyledExport() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams();
        params.setSheetName("用户数据（带样式）");

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 条件样式
        ExcelStyleUtil.applyByTitle(workbook, 0, "分数", 3,(wb, cell) -> {
            int score;
            try {
                if (cell.getCellType() == CellType.NUMERIC) {
                    score = (int) cell.getNumericCellValue();
                } else {
                    score = Integer.parseInt(cell.getStringCellValue());
                }
            } catch (Exception e) {
                return;
            }

            CellStyle style = wb.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);

            if (score < 60) {
                style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            } else if (score > 90) {
                style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            } else {
                style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            }

            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);
        });

        String filePath = Paths.get("target", "condition_styled_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 带样式的 Excel 导出成功！路径: " + filePath);
    }
```

![image-20260122170145594](./assets/image-20260122170145594.png)

##### 使用 Sheet 名称

```java
    @Test
    public void testConditionStyledExport() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams();
        String sheetName = "用户数据（带样式）";
        params.setSheetName(sheetName);

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 条件样式
        ExcelStyleUtil.applyByTitle(workbook, sheetName, "分数", 3,(wb, cell) -> {
            int score;
            try {
                if (cell.getCellType() == CellType.NUMERIC) {
                    score = (int) cell.getNumericCellValue();
                } else {
                    score = Integer.parseInt(cell.getStringCellValue());
                }
            } catch (Exception e) {
                return;
            }

            CellStyle style = wb.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);

            if (score < 60) {
                style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            } else if (score > 90) {
                style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            } else {
                style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            }

            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cell.setCellStyle(style);
        });

        String filePath = Paths.get("target", "condition_styled_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 带样式的 Excel 导出成功！路径: " + filePath);
    }
```

![image-20260122170145594](./assets/image-20260122170145594.png)

##### 统一某一列全部居左

```java
ExcelStyleUtil.applyByTitle(workbook, 0, "年龄", 3, (wb, cell) -> {
    CellStyle style = wb.createCellStyle();
    style.setAlignment(HorizontalAlignment.LEFT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    cell.setCellStyle(style);
});
```

![image-20260122170954421](./assets/image-20260122170954421.png)

------

##### 手机号脱敏显示（后处理脱敏）

```java
ExcelStyleUtil.applyByTitle(workbook, 0, "手机号", 3, (wb, cell) -> {
    String value = cell.getStringCellValue();
    if (value.length() >= 7) {
        String masked = value.substring(0, 3) + "****" + value.substring(value.length() - 4);
        cell.setCellValue(masked);
    }
});
```

用途：

- 手机号
- 身份证
- 银行卡

比 EasyPOI 自带脱敏规则更灵活。

![image-20260122171039317](./assets/image-20260122171039317.png)

------

##### 状态字段颜色标识（业务系统最常见）

```java
ExcelStyleUtil.applyByTitle(workbook, 0, "省份", 3, (wb, cell) -> {
    String status = cell.getStringCellValue();

    CellStyle style = wb.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);

    if ("重庆市".equals(status)) {
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
    } else if ("成都省".equals(status)) {
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
    } else {
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    }

    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    cell.setCellStyle(style);
});
```

报表系统非常爱用这一套。

![image-20260122171236905](./assets/image-20260122171236905.png)

------

##### 金额列高亮（财务报表经典）

```java
ExcelStyleUtil.applyByTitle(workbook, 0, "分数", 3, (wb, cell) -> {
    double amount;
    try {
        amount = cell.getNumericCellValue();
    } catch (Exception e) {
        return;
    }

    CellStyle style = wb.createCellStyle();
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);

    if (amount < 0.5) {
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());      // 亏损
    } else if (amount < 0.9) {
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex()); // 大额
    }

    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    cell.setCellStyle(style);
});
```

![image-20260122171450803](./assets/image-20260122171450803.png)

------

##### 时间列高亮

```java
// 条件样式
ExcelStyleUtil.applyByTitle(workbook, 0, "生日", 3, (wb, cell) -> {
    LocalDate birthday;
    try {
        birthday = LocalDate.parse(cell.getStringCellValue());
    } catch (Exception e) {
        return;
    }

    CellStyle style = wb.createCellStyle();
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);

    if (LocalDate.parse("2000-01-01").compareTo(birthday) <= 0) {
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
    } else {
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    }

    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    cell.setCellStyle(style);
});
```

![image-20260122172000426](./assets/image-20260122172000426.png)

##### 空值标红（数据质量校验神器）

```java
ExcelStyleUtil.applyByTitle(workbook, 0, "身份证", 3, (wb, cell) -> {
    String value = cell.getStringCellValue();
    if (value == null || value.trim().isEmpty()) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(style);
    }
});
```

导出来就是“数据质量检查表”。

------

##### 分数分级渲染（你现在这个的加强版）

```java
ExcelStyleUtil.applyByTitle(workbook, 0, "分数", 3, (wb, cell) -> {
    int score;
    try {
        score = (int) cell.getNumericCellValue();
    } catch (Exception e) {
        return;
    }

    CellStyle style = wb.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);

    if (score < 60) {
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
    } else if (score < 80) {
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    } else {
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
    }

    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    cell.setCellStyle(style);
});
```

------

##### 等级型数据（风险 / 优先级）

```java
ExcelStyleUtil.applyByTitle(workbook, 0, "风险等级", 3, (wb, cell) -> {
    String level = cell.getStringCellValue();

    CellStyle style = wb.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);

    switch (level) {
        case "高":
            style.setFillForegroundColor(IndexedColors.RED.getIndex());
            break;
        case "中":
            style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            break;
        case "低":
            style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            break;
        default:
            return;
    }

    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    cell.setCellStyle(style);
});
```

------



### 数据映射

#### 用 `replace` 属性做固定字典映射

**实体类定义**

注意：`replace` 数组格式必须是："显示值_实际值"

```
    // 1→青年 2→中年 3→老年
    @Excel(name = "年龄段", replace = {"青年_1", "中年_2", "老年_3"})
    private Integer number;
```

**导出代码保持不变**

![image-20260121213210297](./assets/image-20260121213210297.png)

#### 使用`IExcelDictHandler` 自定义处理

**在字段上加字典标识**

重点是 `dict = "ageDict"` ，这个 key 要和 handler 里保持一致。

```
@Excel(name = "年龄段", dict = "ageDict")
private Integer number;
```

**实现 `IExcelDictHandler`**

```java
package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDictHandler;

public class NumberDictHandler implements IExcelDictHandler {

    @Override
    public String toName(String dict, Object obj, String name, Object value) {
        if ("ageDict".equals(dict)) {
            if (value == null) {
                return "";
            }
            switch (value.toString()) {
                case "1": return "青年";
                case "2": return "中年";
                case "3": return "老年";
            }
        }
        return null;
    }

    @Override
    public String toValue(String dict, Object obj, String name, Object value) {
        if ("ageDict".equals(dict)) {
            if (value == null) {
                return null;
            }
            switch (value.toString()) {
                case "青年": return "1";
                case "中年": return "2";
                case "老年": return "3";
            }
        }
        return null;
    }
}
```

**在 ExportParams 中注册 Handler**

```java
    @Test
    public void testExportWithDict() throws Exception {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams("用户列表", "sheet1");
        params.setDictHandler(new NumberDictHandler());

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "dict_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功！");
    }
```

![image-20260121215517632](./assets/image-20260121215517632.png)

#### 使用 `IExcelDataHandler` 自定义处理

**字段配置**

```
@Excel(name = "年龄段")
private Integer number;
```

**实现 `IExcelDataHandler`**

```java
package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDataHandler;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;

import java.util.HashMap;
import java.util.Map;

/**
 * number 字段导入导出的自定义处理器
 *
 * 功能：
 * - 导出：1 -> 一号、2 -> 二号、3 -> 三号
 * - 导入：一号 -> 1、二号 -> 2、三号 -> 3
 *
 * 注意点：
 * - 实现 IExcelDataHandler 全部方法
 */
public class NumberDataHandler implements IExcelDataHandler<Object> {

    private String[] needHandlerFields;

    /**
     * 字典映射（可改）
     */
    private static final Map<String, String> EXPORT_MAP = new HashMap<>();
    private static final Map<String, String> IMPORT_MAP = new HashMap<>();

    static {
        EXPORT_MAP.put("1", "一号");
        EXPORT_MAP.put("2", "二号");
        EXPORT_MAP.put("3", "三号");

        IMPORT_MAP.put("一号", "1");
        IMPORT_MAP.put("二号", "2");
        IMPORT_MAP.put("三号", "3");
    }

    @Override
    public Object exportHandler(Object obj, String name, Object value) {
        if (!match(name)) {
            return value;
        }
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value);
        return EXPORT_MAP.getOrDefault(raw, raw);
    }

    @Override
    public Object importHandler(Object obj, String name, Object value) {
        if (!match(name)) {
            return value;
        }
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value);
        return IMPORT_MAP.getOrDefault(raw, raw);
    }

    @Override
    public String[] getNeedHandlerFields() {
        return needHandlerFields;
    }

    @Override
    public void setNeedHandlerFields(String[] fields) {
        this.needHandlerFields = fields;
    }

    @Override
    public void setMapValue(Map<String, Object> map, String originKey, Object value) {
        if (!match(originKey)) {
            map.put(originKey, value);
            return;
        }

        if (value != null) {
            String raw = String.valueOf(value);
            map.put(originKey, IMPORT_MAP.getOrDefault(raw, raw));
        } else {
            map.put(originKey, null);
        }
    }

    @Override
    public Hyperlink getHyperlink(CreationHelper creationHelper, Object obj, String name, Object value) {
        // 这里通常不用超链接，返回 null 即可
        return null;
    }

    /**
     * 判断字段是否在处理范围
     */
    private boolean match(String name) {
        if (needHandlerFields == null) {
            return false;
        }
        for (String field : needHandlerFields) {
            if (field.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
```

**使用方法**

```java
    @Test
    public void testSimpleExportWithHandler() throws Exception {

        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams("用户列表", "sheet1");

        NumberDataHandler handler = new NumberDataHandler();

        // 指定要处理的字段，注意是Excel的字段名（表头）
        handler.setNeedHandlerFields(new String[]{"年龄段"});

        // 设置给导出参数
        params.setDataHandler(handler);

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "data_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();
    }
```

![image-20260122074717444](./assets/image-20260122074717444.png)

### 生成下拉 `addressList`

@Excel 加入 addressList 是否生成下拉的选项，默认false。目前下拉只支持`replace`和`dict`两个取值地方生成。

#### 用 `replace`

**实体类定义**

注意：`replace` 数组格式必须是："显示值_实际值"

```
    // 1→青年 2→中年 3→老年
    @Excel(name = "年龄段", replace = {"青年_1", "中年_2", "老年_3"}, addressList = true)
    private Integer number;
```

**导出代码保持不变**

![image-20260122085841144](./assets/image-20260122085841144.png)

#### 使用`IExcelDictHandler` 

**在字段上加字典标识**

重点是 `dict = "ageDict"` ，这个 key 要和 handler 里保持一致。

```
@Excel(name = "年龄段", dict = "ageDict", addressList = true)
private Integer number;
```

**实现 `IExcelDictHandler`**

```java
package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDictHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NumberDictHandler implements IExcelDictHandler {
    @Override
    public List<Map> getList(String dict) {
        List<Map> list = new ArrayList<>();
        Map<String, String> dictMap = new HashMap<>(2);
        dictMap.put("dictKey", "1");
        dictMap.put("dictValue", "青年");
        list.add(dictMap);
        dictMap = new HashMap<>(2);
        dictMap.put("dictKey", "2");
        dictMap.put("dictValue", "中年");
        list.add(dictMap);
        dictMap = new HashMap<>(2);
        dictMap.put("dictKey", "3");
        dictMap.put("dictValue", "老年");
        list.add(dictMap);
        return list;
    }

    @Override
    public String toName(String dict, Object obj, String name, Object value) {
        if ("ageDict".equals(dict)) {
            if (value == null) {
                return "";
            }
            switch (value.toString()) {
                case "1":
                    return "青年";
                case "2":
                    return "中年";
                case "3":
                    return "老年";
            }
        }
        return null;
    }

    @Override
    public String toValue(String dict, Object obj, String name, Object value) {
        if ("ageDict".equals(dict)) {
            if (value == null) {
                return null;
            }
            switch (value.toString()) {
                case "青年":
                    return "1";
                case "中年":
                    return "2";
                case "老年":
                    return "3";
            }
        }
        return null;
    }
}
```

**在 ExportParams 中注册 Handler**

```java
    @Test
    public void testExportWithDict() throws Exception {
        List<MyUser> userList = InitData.getDataList();

        ExportParams params = new ExportParams("用户列表", "sheet1");
        params.setDictHandler(new NumberDictHandler());

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "dict_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功！");
    }
```

![image-20260122090213563](./assets/image-20260122090213563.png)

### 枚举处理

**创建枚举**

```java
package io.github.atengk.enums;

public enum UserStatus {
    NORMAL(0, "正常"),
    FROZEN(1, "冻结"),
    DELETED(2, "已删除");

    private final int code;
    private final String name;

    UserStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 根据 code 获取 name
     */
    public static String getNameByCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status.name;
            }
        }
        return null;
    }

    /**
     * 根据 name 获取枚举
     */
    public static UserStatus getByName(String name) {
        for (UserStatus status : values()) {
            if (status.name.equals(name)) {
                return status;
            }
        }
        return null;
    }
}
```

**添加字段**

```java
    /**
     * 用户状态
     * enumExportField: 导出 Excel 显示哪个字段
     * enumImportMethod: 导入 Excel 时通过静态方法将值转换为枚举
     */
    @Excel(name = "状态", enumExportField = "name", enumImportMethod = "getByName")
    private UserStatus status;
```

**使用方法**

```java
    @Test
    public void testSimpleExportWithEnumField() throws IOException {
        // 1. 准备数据
        List<MyUser> userList = InitData.getDataList();

        // 随机分配状态
        UserStatus[] statuses = UserStatus.values();
        for (int i = 0; i < userList.size(); i++) {
            userList.get(i).setStatus(statuses[i % statuses.length]);
        }

        // 2. 导出参数
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 导出 Excel
        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        // 4. 写入文件
        String filePath = Paths.get("target", "simple_export_users_enum.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        workbook.close();

        System.out.println("✅ 导出成功！路径: " + filePath);
    }
```

![image-20260122090823922](./assets/image-20260122090823922.png)

### 数据脱敏

#### 注解脱敏（简单规则）

```java
@Excel(name = "姓名", desensitizationRule = "1,6")  // 规则2：保留头1，尾6，其余用*
private String name;

@Excel(name = "身份证", desensitizationRule = "6_4") // 规则1：保留头6，尾4，中间*
private String idCard;

@Excel(name = "手机号", desensitizationRule = "3_4") // 规则1：保留头3，尾4
private String phoneNumber;

@Excel(name = "邮箱", desensitizationRule = "1~@")  // 规则3：保留第一位和@之后的内容
private String email;
```

- 6_4` → 保留头6位、尾4位，中间用 `*

- `3_4` → 保留头3位、尾4位
- `1,6` → 保留头1位、尾6位
- `1~@` → 特殊符号规则，保留第一位和 `@` 之后

![image-20260122093949163](./assets/image-20260122093949163.png)

#### Map / 自定义表头脱敏

```
ExcelExportEntity phoneEntity = new ExcelExportEntity("手机号", "phoneNumber");
phoneEntity.setDesensitizationRule("3_4");

ExcelExportEntity emailEntity = new ExcelExportEntity("邮箱", "email");
emailEntity.setDesensitizationRule("1~@");
```

#### 自定义脱敏逻辑

参考章节：数据映射 / 使用 `IExcelDataHandler` 自定义处理

### 导出图片

更新 `MyUser` 实体类，添加图片字段

- `type = 2` 表示这是一个图片类型。
- 支持：`String 本地路径`、`String http URL`、`base64 字符串`、
- 类型也可以是 `byte[]`、`InputStream`、`File`、`URL`、`classpath 资源流`
- 也可以直接Object，所有类型都支持

```java
    /**
     * 图片
     */
    @Excel(name = "图片", type = 2, orderNum = "12")
    private Object image;
```

使用方法

```java
    @Test
    public void testImageExport() throws IOException {
        List<Object> imagePool = Arrays.asList(
                "D:/Temp/images/1.jpg",                               // 本地
                "https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg",   // 网络
                new File("D:/Temp/images/3.jpg")                      // File
        );

        List<MyUser> userList = InitData.getDataList();
        for (int i = 0; i < userList.size(); i++) {
            userList.get(i).setImage(imagePool.get(i % imagePool.size()));
        }

        ExportParams params = new ExportParams();
        params.setSheetName("用户数据（含图片）");

        Workbook workbook = ExcelExportUtil.exportExcel(params, MyUser.class, userList);

        String filePath = Paths.get("target", "image_export_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 含图片的 Excel 导出成功！路径: " + filePath);
    }
```



### 导出为多个 Sheet

```java
    @Test
    public void testMultiSheetExport() throws IOException {
        // 1. 获取原始数据
        List<MyUser> userList = InitData.getDataList();

        // 2. 按省份分组（保留插入顺序，用 LinkedHashMap）
        Map<String, List<MyUser>> provinceGroups = userList.stream()
                .collect(Collectors.groupingBy(
                        MyUser::getProvince,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 3. 构造多 Sheet 数据：List<Map<String, Object>>
        List<Map<String, Object>> sheets = new ArrayList<>();
        for (Map.Entry<String, List<MyUser>> entry : provinceGroups.entrySet()) {
            String sheetName = entry.getKey();
            List<MyUser> data = entry.getValue();

            ExportParams exportParams = new ExportParams();
            exportParams.setSheetName(sheetName);
            exportParams.setTitle(sheetName + " 用户数据");

            // 每个 Sheet 用一个 Map 表示：<sheetName, dataList>
            Map<String, Object> sheet = new LinkedHashMap<>();
            sheet.put("title", exportParams);       // 顶级表头 和 Sheet 名称
            sheet.put("entity", MyUser.class);     // 表头
            sheet.put("data", data);               // 数据列表
            sheets.add(sheet);
        }

        // 4. 使用多 Sheet 导出方法
        Workbook workbook = ExcelExportUtil.exportExcel(sheets, ExcelType.XSSF);

        // 5. 写入文件
        String filePath = Paths.get("target", "multi_sheet_users.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 多 Sheet 导出成功！共 " + sheets.size() + " 个省份，路径: " + filePath);
    }
```

![image-20260121195156735](./assets/image-20260121195156735.png)

### 大数据量导出（分批写入，避免内存溢出）

```java
    @Test
    public void testBigDataExport() throws IOException {
        // 1. 生成大数据
        int total = 500_000;
        System.out.println("正在生成 " + total + " 条测试数据...");
        List<MyUser> dataList = InitData.getDataList(total);

        // 2. 创建 IWriter
        ExportParams params = new ExportParams();
        params.setSheetName("大数据用户");

        // 3. 获取 writer
        IWriter<Workbook> writer = ExcelExportUtil.exportBigExcel(params, MyUser.class);

        // 4. 分批写入
        int batchSize = 1000;
        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<MyUser> batch = dataList.subList(i, end);

            writer.write(batch);

            System.out.printf("已写入 %d / %d 行%n", end, dataList.size());
        }

        // 5. 获取Workbook 并写入文件
        Workbook workbook = writer.get();
        try (FileOutputStream fos = new FileOutputStream("target/big_data_users.xlsx")) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 大数据导出成功！");
    }
```

![image-20260121200002905](./assets/image-20260121200002905.png)

### 使用 `List<Map>` 导出（无实体类）

#### 常规使用

```java
    @Test
    public void testSimpleExportWithMap() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("city", user.getCity());
            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));
        entityList.add(new ExcelExportEntity("年龄", "age"));
        entityList.add(new ExcelExportEntity("城市", "city"));

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_users_map.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }
```

![image-20260121201011767](./assets/image-20260121201011767.png)

#### 调整列宽

```java
    @Test
    public void testSimpleExportWithMap() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("city", user.getCity());
            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        ExcelExportEntity id = new ExcelExportEntity("ID", "id");
        id.setWidth(20);
        entityList.add(id);
        ExcelExportEntity name = new ExcelExportEntity("姓名", "name");
        name.setWidth(30);
        entityList.add(name);
        ExcelExportEntity age = new ExcelExportEntity("年龄", "age");
        age.setWidth(20);
        entityList.add(age);
        ExcelExportEntity city = new ExcelExportEntity("城市", "city");
        city.setWidth(40);
        entityList.add(city);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_users_map.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }
```

![image-20260121202206155](./assets/image-20260121202206155.png)

#### 多 Sheet

```java
    @Test
    public void testMultiSheetDifferentHeadersSingleMethod() throws IOException {
        // ====== 准备数据 ======
        List<MyUser> userList1 = InitData.getDataList();
        List<MyUser> userList2 = InitData.getDataList();

        // ====== Sheet1 表头 ======
        List<ExcelExportEntity> entityList1 = new ArrayList<>();
        ExcelExportEntity id1 = new ExcelExportEntity("ID", "id");
        id1.setWidth(20);
        entityList1.add(id1);
        ExcelExportEntity name1 = new ExcelExportEntity("姓名", "name");
        name1.setWidth(30);
        entityList1.add(name1);
        ExcelExportEntity age1 = new ExcelExportEntity("年龄", "age");
        age1.setWidth(20);
        entityList1.add(age1);
        ExcelExportEntity city1 = new ExcelExportEntity("城市", "city");
        city1.setWidth(40);
        entityList1.add(city1);

        // ====== Sheet1 数据 ======
        List<Map<String, Object>> dataList1 = new ArrayList<>();
        for (MyUser u : userList1) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("age", u.getAge());
            map.put("city", u.getCity());
            dataList1.add(map);
        }

        // ====== Sheet2 表头 ======
        List<ExcelExportEntity> entityList2 = new ArrayList<>();
        ExcelExportEntity id2 = new ExcelExportEntity("用户ID", "id");
        id2.setWidth(25);
        entityList2.add(id2);
        ExcelExportEntity name2 = new ExcelExportEntity("用户姓名", "name");
        name2.setWidth(35);
        entityList2.add(name2);

        // ====== Sheet2 数据 ======
        List<Map<String, Object>> dataList2 = new ArrayList<>();
        for (MyUser u : userList2) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            dataList2.add(map);
        }

        // ====== 打包多 Sheet ======
        List<Map<String, Object>> sheets = new ArrayList<>();

        Map<String, Object> sheet1 = new HashMap<>();
        sheet1.put("title", new ExportParams("完整用户列表", null, "Sheet1"));
        sheet1.put("entity", entityList1); // List<ExcelExportEntity>
        sheet1.put("data", dataList1);     // List<Map>
        sheets.add(sheet1);

        Map<String, Object> sheet2 = new HashMap<>();
        sheet2.put("title", new ExportParams("简化用户列表", null, "Sheet2"));
        sheet2.put("entity", entityList2);
        sheet2.put("data", dataList2);
        sheets.add(sheet2);

        // ====== 创建 Workbook（关键） ======
        Workbook workbook = createWorkbookForMapSheets(sheets, ExcelType.XSSF);

        // ====== 写入文件 ======
        String filePath = Paths.get("target", "multi_sheet_diff_headers.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 多 Sheet 导出成功: " + filePath);
    }

    /**
     * 多 Sheet + Map + 不同表头导出
     */
    private Workbook createWorkbookForMapSheets(List<Map<String, Object>> sheets, ExcelType type) {
        Workbook workbook = type == ExcelType.HSSF ? new HSSFWorkbook() : new XSSFWorkbook();
        for (Map<String, Object> sheetMap : sheets) {
            ExportParams params = (ExportParams) sheetMap.get("title");
            List<ExcelExportEntity> entityList = (List<ExcelExportEntity>) sheetMap.get("entity");
            Collection<?> dataSet = (Collection<?>) sheetMap.get("data");
            new ExcelExportService().createSheetForMap(workbook, params, entityList, dataSet);
        }
        return workbook;
    }
```

![image-20260121203250589](./assets/image-20260121203250589.png)

#### 合并单元格

使用 `ExcelExportEntity.setMergeVertical(true)` 自动合并内容相同的单元格

```java

    @Test
    public void testSimpleMergeExportWithMap() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        userList.sort(
                Comparator.comparing(MyUser::getProvince)
                        .thenComparing(MyUser::getCity)
        );

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("city", user.getCity());
            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        ExcelExportEntity id = new ExcelExportEntity("ID", "id");
        id.setWidth(20);
        entityList.add(id);
        ExcelExportEntity name = new ExcelExportEntity("姓名", "name");
        name.setWidth(30);
        entityList.add(name);
        ExcelExportEntity age = new ExcelExportEntity("年龄", "age");
        age.setWidth(20);
        entityList.add(age);
        ExcelExportEntity city = new ExcelExportEntity("城市", "city");
        city.setWidth(40);
        city.setMergeVertical(true);
        entityList.add(city);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_merge_users_map.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }
```

![image-20260123103821173](./assets/image-20260123103821173.png)

#### 导出图片

图片列需要先下载成 byte[]

```java

    @Test
    public void testSimpleExportWithMapAndImage() throws IOException {
        List<MyUser> userList = InitData.getDataList(10);

        // 图片 URL
        String imageUrl = "https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg";

        // ====== 转成 List<Map> ======
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("age", user.getAge());
            map.put("city", user.getCity());

            // 图片列：先下载成 byte[]
            byte[] imageBytes = HttpUtil.downloadBytes(imageUrl);
            map.put("avatar", imageBytes);

            dataList.add(map);
        }

        // ====== 定义表头 ======
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));
        entityList.add(new ExcelExportEntity("年龄", "age"));
        entityList.add(new ExcelExportEntity("城市", "city"));

        // 图片列
        ExcelExportEntity avatarEntity = new ExcelExportEntity("头像", "avatar");
        avatarEntity.setType(BaseTypeConstants.IMAGE_TYPE);
        avatarEntity.setHeight(100); // 高度
        avatarEntity.setWidth(100);  // 宽度
        entityList.add(avatarEntity);

        // ====== 导出 Excel ======
        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        // ====== 写入文件 ======
        String filePath = Paths.get("target", "simple_export_users_map_with_image.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("✅ 导出成功: " + filePath);
    }
```

![image-20260121204443236](./assets/image-20260121204443236.png)

#### 字典映射 replace

使用ExcelExportEntity.setReplace构建映射：显示值_原始值（String）

```java
    @Test
    public void testSimpleExportWithMap_Dict() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());

            // 假设 number 是数字，后面用字典映射
            map.put("number", RandomUtil.randomEle(Arrays.asList(1,2,3)));

            // 假设 city 是编码，后面用 handler 处理
            map.put("city", user.getCity());

            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));

        // 年龄字典映射
        ExcelExportEntity ageEntity = new ExcelExportEntity("年龄段", "number");
        // 映射：显示值_原始值
        ageEntity.setReplace(new String[]{
                "青年_1",
                "中年_2",
                "老年_3"
        });
        entityList.add(ageEntity);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_users_map_dict.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }
```

![image-20260121212513900](./assets/image-20260121212513900.png)

#### 生成下拉

```java
    @Test
    public void testSimpleExportWithMap_DictAndDropdown() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 1. 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());

            // 假设 number 是数字，后面用字典映射
            map.put("number", RandomUtil.randomEle(Arrays.asList(1, 2, 3)));

            // 假设 city 是编码，后面用 handler 处理
            map.put("city", user.getCity());

            dataList.add(map);
        }

        // 2. 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));

        // 年龄段列，字典映射 + 下拉
        ExcelExportEntity ageEntity = new ExcelExportEntity("年龄段", "number");
        // 显示值_原始值
        ageEntity.setReplace(new String[]{
                "青年_1",
                "中年_2",
                "老年_3"
        });
        // 下拉框，根据 Replace 的值生成
        ageEntity.setAddressList(true);
        entityList.add(ageEntity);

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");

        // 3. 导出 Excel
        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        // 4. 写入本地文件
        String filePath = Paths.get("target", "simple_export_users_map_dict_dropdown.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        workbook.close();

        System.out.println("✅ 导出成功: " + filePath);
    }
```

![image-20260122092042713](./assets/image-20260122092042713.png)

#### 使用 `IExcelDataHandler` 自定义处理

**实现 `IExcelDataHandler`**

```java
package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDataHandler;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;

import java.util.HashMap;
import java.util.Map;

/**
 * number 字段导入导出的自定义处理器
 *
 * 功能：
 * - 导出：1 -> 一号、2 -> 二号、3 -> 三号
 * - 导入：一号 -> 1、二号 -> 2、三号 -> 3
 *
 * 注意点：
 * - 实现 IExcelDataHandler 全部方法
 */
public class NumberDataHandler implements IExcelDataHandler<Object> {

    private String[] needHandlerFields;

    /**
     * 字典映射（可改）
     */
    private static final Map<String, String> EXPORT_MAP = new HashMap<>();
    private static final Map<String, String> IMPORT_MAP = new HashMap<>();

    static {
        EXPORT_MAP.put("1", "一号");
        EXPORT_MAP.put("2", "二号");
        EXPORT_MAP.put("3", "三号");

        IMPORT_MAP.put("一号", "1");
        IMPORT_MAP.put("二号", "2");
        IMPORT_MAP.put("三号", "3");
    }

    @Override
    public Object exportHandler(Object obj, String name, Object value) {
        if (!match(name)) {
            return value;
        }
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value);
        return EXPORT_MAP.getOrDefault(raw, raw);
    }

    @Override
    public Object importHandler(Object obj, String name, Object value) {
        if (!match(name)) {
            return value;
        }
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value);
        return IMPORT_MAP.getOrDefault(raw, raw);
    }

    @Override
    public String[] getNeedHandlerFields() {
        return needHandlerFields;
    }

    @Override
    public void setNeedHandlerFields(String[] fields) {
        this.needHandlerFields = fields;
    }

    @Override
    public void setMapValue(Map<String, Object> map, String originKey, Object value) {
        if (!match(originKey)) {
            map.put(originKey, value);
            return;
        }

        if (value != null) {
            String raw = String.valueOf(value);
            map.put(originKey, IMPORT_MAP.getOrDefault(raw, raw));
        } else {
            map.put(originKey, null);
        }
    }

    @Override
    public Hyperlink getHyperlink(CreationHelper creationHelper, Object obj, String name, Object value) {
        // 这里通常不用超链接，返回 null 即可
        return null;
    }

    /**
     * 判断字段是否在处理范围
     */
    private boolean match(String name) {
        if (needHandlerFields == null) {
            return false;
        }
        for (String field : needHandlerFields) {
            if (field.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
```

**使用方法**

```java
    @Test
    public void testSimpleExportWithMap_DataHandler() throws IOException {
        List<MyUser> userList = InitData.getDataList();

        // 转成 List<Map>
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (MyUser user : userList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("number", RandomUtil.randomEle(Arrays.asList(1, 2, 3)));
            map.put("city", user.getCity());
            dataList.add(map);
        }

        // 定义表头（key 对应 map 的 key，name 是显示在 Excel 的标题）
        List<ExcelExportEntity> entityList = new ArrayList<>();
        entityList.add(new ExcelExportEntity("ID", "id"));
        entityList.add(new ExcelExportEntity("姓名", "name"));
        entityList.add(new ExcelExportEntity("年龄段", "number"));
        entityList.add(new ExcelExportEntity("城市", "city"));

        // 创建自定义 DataHandler
        NumberDataHandler handler = new NumberDataHandler();
        handler.setNeedHandlerFields(new String[]{"年龄段"}); // 注意这里写 Map 的 name

        ExportParams params = new ExportParams();
        params.setSheetName("用户列表");
        params.setDataHandler(handler); // 设置 handler

        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, dataList);

        String filePath = Paths.get("target", "simple_export_users_map_datahandler.xlsx").toString();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出成功: " + filePath);
    }
```

![image-20260122075310039](./assets/image-20260122075310039.png)

## 模板导出（Template Export）

| 功能                   | 指令      | 普通变量示例                              | 列表变量示例（$fe 中使用）              |
| ---------------------- | --------- | ----------------------------------------- | --------------------------------------- |
| 普通取值               | 无指令    | `{{name}}`                                | `t.name`                                |
| 数值单元格             | `n:`      | `{{n:age}}`                               | `n:t.age`                               |
| 时间格式化             | `fd:`     | `{{fd:(createTime;yyyy-MM-dd HH:mm:ss)}}` | `fd:(t.createTime;yyyy-MM-dd HH:mm:ss)` |
| 数字格式化             | `fn:`     | `{{fn:(score;###.00)}}`                   | `fn:(t.score;###.00)`                   |
| 字符串长度             | `le:`     | `{{le:(name)}}`                           | `le:(t.name)`                           |
| 三目运算               | `?:`      | `{{age > 18 ? '成年' : '未成年'}}`        | `t.age > 18 ? '成年' : '未成年'`        |
| 遍历并新建行           | `fe:`     | 不适用                                    | `{{fe:list t t.name t.age}}`            |
| 遍历但不新建行         | `!fe:`    | 不适用                                    | `{{!fe:list t t.name}}`                 |
| 下移插入遍历（最常用） | `$fe:`    | 不适用                                    | `{{ $fe:list t.name t.age t.phone }}`   |
| 横向遍历               | `#fe:`    | 不适用                                    | `{{#fe:list t.name}}`                   |
| 横向遍历取值           | `v_fe:`   | 不适用                                    | `{{v_fe:list}}`                         |
| 删除当前列             | `!if:`    | `{{!if:(age < 18)}}`                      | `!if:(t.age < 18)`                      |
| 字典转换               | `dict:`   | `{{dict:gender;gender}}`                  | `dict:gender;t.gender`                  |
| 国际化                 | `i18n:`   | `{{i18n:key}}`                            | `i18n:key`                              |
| 循环序号               | `&INDEX&` | 不适用                                    | `&INDEX&`                               |
| 空值占位               | `&NULL&`  | `{{&NULL&}}`                              | `&NULL&`                                |
| 换行导出               | `]]`      | `{{name]]age}}`                           | `t.name]]t.age`                         |
| 统计求和               | `sum:`    | `{{sum:score}}`                           | `sum:t.score`                           |
| 计算表达式             | `cal:`    | `{{cal:(price*count)}}`                   | `cal:(t.price*t.count)`                 |
| 常量输出               | `'常量'`  | `{{'正常'}}`                              | `'正常'`                                |

### 创建工具类

**创建函数接口**

```java
package io.github.atengk.util;

import cn.afterturn.easypoi.excel.entity.TemplateExportParams;

/**
 * Excel 模板导出参数配置回调接口
 *
 * @author 孔余
 * @since 2026-01-22
 */
@FunctionalInterface
public interface TemplateParamsConfigurer {

    /**
     * 对 EasyPOI 的 {@link TemplateExportParams} 进行个性化配置
     *
     * @param params EasyPOI 模板导出参数对象
     */
    void configure(TemplateExportParams params);
}
```

**创建工具类**

```java
package io.github.atengk.util;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Excel 工具类（基于 EasyPOI + Apache POI 封装）
 *
 * <p>
 * 提供企业级 Excel 处理能力的统一入口，主要用于：
 * </p>
 *
 * <ul>
 *     <li>基于模板（.xlsx）填充数据并生成 Workbook</li>
 *     <li>支持从 classpath、本地文件、对象存储、网络流等多种来源读取模板</li>
 *     <li>支持 Workbook 导出到本地文件、HTTP 响应流、文件流等多种场景</li>
 *     <li>支持对导出完成后的 Workbook 进行二次样式加工（指定列、条件样式、斑马纹、表头高亮等）</li>
 * </ul>
 *
 * <p>
 * 设计目标：
 * </p>
 *
 * <ul>
 *     <li>屏蔽 EasyPOI 与 POI 的底层复杂度，对外提供简单、稳定的 API</li>
 *     <li>所有方法均为静态方法，符合工具类的使用语义</li>
 *     <li>适用于报表系统、数据导出、运营数据分析、模板化 Excel 生成等企业级场景</li>
 * </ul>
 *
 * <p>
 * 典型使用流程：
 * </p>
 *
 * <pre>
 * Workbook workbook = ExcelUtil.exportByTemplate("doc/user_template.xlsx", data);
 *
 * ExcelUtil.applyByTitle(workbook, 0, "分数", 1, (wb, cell) -> {
 *     // 自定义样式处理
 * });
 *
 * ExcelUtil.exportToResponse(workbook, "用户数据.xlsx", response);
 * </pre>
 *
 * <p>
 * 该类为纯工具类：
 * </p>
 * <ul>
 *     <li>禁止实例化（私有构造方法）</li>
 *     <li>不保存任何状态，线程安全</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-22
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /**
     * 将 Workbook 导出为本地 Excel 文件
     *
     * <p>
     * 适用于：
     * - 单元测试
     * - 本地调试
     * - 定时任务批量生成文件
     * - 数据归档
     * </p>
     *
     * @param workbook 已生成的 Workbook 对象
     * @param filePath 目标文件完整路径，例如：target/user.xlsx
     */
    public static void exportToFile(Workbook workbook, Path filePath) {
        if (workbook == null) {
            throw new IllegalArgumentException("Workbook 不能为空");
        }
        if (filePath == null) {
            throw new IllegalArgumentException("filePath 不能为空");
        }

        try {
            // 确保父目录存在
            Files.createDirectories(filePath.getParent());

            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("导出 Excel 文件失败: " + filePath, e);
        }
    }

    /**
     * 将 Workbook 通过 Spring Boot 接口直接输出给前端下载
     *
     * <p>
     * Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     * Content-Disposition: attachment; filename="xxx.xlsx"
     * </p>
     * <p>
     * 适用于：
     * - 浏览器下载 Excel
     * - 前端点击“导出”按钮
     * - SaaS 系统在线报表导出
     *
     * @param workbook 已生成的 Workbook
     * @param fileName 下载文件名，例如：用户数据.xlsx
     * @param response HttpServletResponse
     */
    public static void exportToResponse(
            Workbook workbook,
            String fileName,
            HttpServletResponse response) {

        if (workbook == null) {
            throw new IllegalArgumentException("Workbook 不能为空");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName 不能为空");
        }
        if (response == null) {
            throw new IllegalArgumentException("HttpServletResponse 不能为空");
        }

        try {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");

            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFileName + "\"");

            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
            throw new IllegalStateException("通过接口导出 Excel 失败", e);
        }
    }

    /**
     * 读取 Excel 模板并导出
     *
     * @param templatePath 模板路径（相对于 resources），如：doc/user_template.xlsx
     * @param data         模板参数数据
     * @return 填充完成后的 Workbook
     */
    public static Workbook exportByTemplate(String templatePath, Map<String, Object> data) {
        return exportByTemplate(templatePath, data, null);
    }

    /**
     * 读取 Excel 模板并导出（终极企业版）
     * <p>
     * 特点：
     * - ExcelUtil 不关心你配哪些参数
     * - 所有 TemplateExportParams 能力全部开放
     * - 以后 EasyPOI 新增参数，你完全不用改工具类
     *
     * @param templatePath 模板路径（相对 resources）
     * @param data         模板数据
     * @param configurer   参数配置回调，可为 null
     */
    public static Workbook exportByTemplate(
            String templatePath,
            Map<String, Object> data,
            TemplateParamsConfigurer configurer) {

        Resource resource = new ClassPathResource(templatePath);

        if (!resource.exists()) {
            throw new IllegalArgumentException("模板文件不存在: " + templatePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            TemplateExportParams params = new TemplateExportParams(inputStream);

            if (configurer != null) {
                configurer.configure(params);
            }

            return ExcelExportUtil.exportExcel(params, data);
        } catch (IOException e) {
            throw new IllegalStateException("读取模板文件失败: " + templatePath, e);
        }
    }

    /**
     * 通过模板文件流导出 Excel
     *
     * <p>
     * 适用于模板来源不固定的场景，例如：
     * - 文件服务器
     * - 对象存储（OSS、MinIO）
     * - 远程下载
     * - 数据库存储模板
     * </p>
     *
     * <p>
     * 注意：
     * - 该方法不会关闭传入的 InputStream，调用方自行管理生命周期
     * - 适合对流进行复用或统一关闭管理的场景
     * </p>
     *
     * @param templateInputStream 模板文件输入流
     * @param data                模板参数数据
     * @return 填充完成后的 Workbook
     */
    public static Workbook exportByTemplate(InputStream templateInputStream,
                                            Map<String, Object> data) {

        if (templateInputStream == null) {
            throw new IllegalArgumentException("templateInputStream 不能为空");
        }
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
        }

        try {
            TemplateExportParams params = new TemplateExportParams(templateInputStream);
            return ExcelExportUtil.exportExcel(params, data);
        } catch (Exception e) {
            throw new IllegalStateException("通过模板流导出 Excel 失败", e);
        }
    }

}
```

### 填充普通变量数据

**创建模版**

```
src
 └─ main
    └─ resources
       └─ doc
          └─ user_template.xlsx
```

EasyPOI 模板语法：

- 普通变量：`{{name}}`

```
姓名：{{ name }}
年龄：{{ age }}
```

![image-20260122173817680](./assets/image-20260122173817680.png)

**使用方法**

```java
    @Test
    void test() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Ateng");
        data.put("age", "25");
        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_users.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);
        System.out.println("✅ 模板导出成功：" + filePath);
    }
```

![image-20260122174632474](./assets/image-20260122174632474.png)

###  填充列表变量数据

**创建模版**

```
src
 └─ main
    └─ resources
       └─ doc
          └─ user_list_template.xlsx
```

EasyPOI 模板语法：

- 列表变量：`{{ $fe:  集合名   单个元素别名   第1个字段 第1个字段 ... 第n个字段 }}`
- 其中 `集合名` 就是data中的list：`data.list`

| 姓名               | 年龄  | 手机号码      | 邮箱    | 分数    | 比例    | 生日       | 所在省份   | 所在城市 | 创建时间        |
| ------------------ | ----- | ------------- | ------- | ------- | ------- | ---------- | ---------- | -------- | --------------- |
| {{ $fe:list t.name | t.age | t.phoneNumber | t.email | t.score | t.ratio | t.birthday | t.province | t.city   | t.createTime }} |

```
姓名	年龄	手机号码	邮箱	分数	比例	生日	所在省份	所在城市	创建时间
{{ $fe:list t.name	t.age	t.phoneNumber	t.email	t.score	t.ratio	t.birthday	t.province	t.city	t.createTime }}
```

![image-20260122194802444](./assets/image-20260122194802444.png)

**使用方法**

```java
    @Test
    void test2() {
        List<MyUser> dataList = InitData.getDataList();
        Map<String, Object> data = new HashMap<>();
        data.put("list", dataList);
        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_list_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_list_users.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);
        System.out.println("✅ 模板导出成功：" + filePath);
    }
```

![image-20260122195643662](./assets/image-20260122195643662.png)

### 填充普通和列表变量数据（混合）

**创建模版**

```
src
 └─ main
    └─ resources
       └─ doc
          └─ user_mix_template.xlsx
```

EasyPOI 模板语法：

- 普通变量：`{{name}}`
- 列表变量：`{{ $fe:  集合名   单个元素别名   第1个字段 第1个字段 ... 第n个字段 }}`
- 其中 `集合名` 就是data中的list：`data.list`

| 标题：{{ title }}  |       |               |         |         |         |            |                    |                  |                 |
| ------------------ | ----- | ------------- | ------- | ------- | ------- | ---------- | ------------------ | ---------------- | --------------- |
|                    |       |               |         |         |         |            |                    |                  |                 |
| 姓名               | 年龄  | 手机号码      | 邮箱    | 分数    | 比例    | 生日       | 所在省份           | 所在城市         | 创建时间        |
| {{ $fe:list t.name | t.age | t.phoneNumber | t.email | t.score | t.ratio | t.birthday | t.province         | t.city           | t.createTime }} |
|                    |       |               |         |         |         |            |                    |                  |                 |
|                    |       |               |         |         |         |            | 作者：{{ author }} | 时间：{{ time }} |                 |

```
标题：{{ title }}									
									
姓名	年龄	手机号码	邮箱	分数	比例	生日	所在省份	所在城市	创建时间
{{ $fe:list t.name	t.age	t.phoneNumber	t.email	t.score	t.ratio	t.birthday	t.province	t.city	t.createTime }}
									
							作者：{{ author }}	时间：{{ time }}	
```

![image-20260122201626314](./assets/image-20260122201626314.png)

**使用方法**

```java
    @Test
    void test3() {
        List<MyUser> dataList = InitData.getDataList(10);
        Map<String, Object> data = new HashMap<>();
        data.put("list", dataList);
        data.put("title", "EasyPoi 模版导出混合使用");
        data.put("author", "Ateng");
        data.put("time", DateUtil.now());
        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_mix_template.xlsx",
                data
        );
        Path filePath = Paths.get("target", "template_export_mix_users.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);
        System.out.println("✅ 模板导出成功：" + filePath);
    }
```

![image-20260122201926756](./assets/image-20260122201926756.png)

### 格式化

#### 普通变量

**创建模版**

```
src
 └─ main
    └─ resources
       └─ doc
          └─ user_format_template.xlsx
```

**模版内容**

```
字段说明	模板表达式
姓名	{{name}}
年龄（数值）	{{n:age}}
年龄描述（三目）	{{age > 18 ? '成年' : '未成年'}}
创建时间原始值	{{createTime}}
创建时间格式化	{{fd:(createTime;yyyy-MM-dd HH:mm:ss)}}
生日（仅日期）	{{fd:(birthday;yyyy-MM-dd)}}
分数原始值	{{score}}
分数两位小数	{{fn:(score;###.00)}}
比例原始值	{{ratio}}
比例百分比	{{fn:(ratio;0.00%)}}
字符串长度	{{le:(name)}}
```

![image-20260122204111151](./assets/image-20260122204111151.png)

**使用方法**

```java
    @Test
    void test4() throws ParseException {
        Map<String, Object> data = new HashMap<>();

        Date date = new Date();
        Date formatDate = new SimpleDateFormat("yyyy-MM-dd").parse("1999-06-18");

        data.put("name", "Ateng");
        data.put("age", 25);
        data.put("createTime", date);
        data.put("birthday", formatDate);
        data.put("score", 87.456);
        data.put("ratio", 0.8567);

        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_format_template.xlsx",
                data
        );

        Path filePath = Paths.get("target", "template_export_users_format.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);

        System.out.println("✅ 普通变量格式化模板导出成功：" + filePath);
    }
```

![image-20260122204458759](./assets/image-20260122204458759.png)

注意几个“企业级细节”：

| 字段       | 要点                                       |
| ---------- | ------------------------------------------ |
| age        | 必须给数字，不要给字符串，否则 `n:` 会失效 |
| createTime | 必须是 `Date` 或 `LocalDateTime`           |
| birthday   | 同上                                       |
| score      | Double / BigDecimal                        |
| ratio      | 小数 0.8567 → 显示为 85.67%                |

---

#### 普通变量 + dict

**创建模版**

```
src
 └─ main
    └─ resources
       └─ doc
          └─ user_format_dict_template.xlsx
```

**模版内容**

```
字段说明	模板表达式
性别原始值	{{gender}}
性别字典翻译(dict)	{{dict:genderDict;gender}}
```

![image-20260122211524313](./assets/image-20260122211524313.png)

**创建字典处理器**

```java
package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDictHandler;

/**
 * 性别字典处理器
 *
 * 统一维护性别字段的「值 ↔ 显示名称」映射关系：
 *
 * 数据库存值：
 *  1 → 男
 *  2 → 女
 *
 * 使用场景：
 * 1. 导出时：
 *    {{dict:genderDict;gender}}
 *    调用 toName，把 1 / 2 转换为 男 / 女
 *
 * 2. 导入时：
 *    Excel 中是 男 / 女
 *    调用 toValue，把 男 / 女 转换为 1 / 2
 *
 * 这样可以做到：
 * - Excel 对业务人员友好（看中文）
 * - 系统内部对数据库友好（存编码）
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class GenderDictHandler implements IExcelDictHandler {

    /**
     * 导出时调用：将“字典值”转换为“显示名称”
     *
     * @param dict  字典标识，例如：gender
     * @param obj   当前行对象
     * @param name  当前字段名称
     * @param value 当前字段原始值，例如：1、2
     * @return 转换后的显示值，例如：男、女
     */
    @Override
    public String toName(String dict, Object obj, String name, Object value) {
        if (!"genderDict".equals(dict)) {
            return value == null ? "" : value.toString();
        }

        if (value == null) {
            return "";
        }

        switch (value.toString()) {
            case "1":
                return "男";
            case "2":
                return "女";
            default:
                return "未知";
        }
    }

    /**
     * 导入时调用：将“显示名称”反向转换为“字典值”
     *
     * Excel 中如果填写：
     *  男 → 返回 1
     *  女 → 返回 2
     *
     * @param dict  字典标识，例如：gender
     * @param obj   当前行对象
     * @param name  当前字段名称
     * @param value Excel 中读取到的值，例如：男、女
     * @return 转换后的字典值，例如：1、2
     */
    @Override
    public String toValue(String dict, Object obj, String name, Object value) {
        if (!"genderDict".equals(dict)) {
            return value == null ? "" : value.toString();
        }

        if (value == null) {
            return "";
        }

        switch (value.toString().trim()) {
            case "男":
                return "1";
            case "女":
                return "2";
            default:
                return "";
        }
    }
}
```

**使用方法**

如有数据需要格式化，只有在业务中处理好，EasyPoi的模版导出的变量只负责渲染数据

```java
    @Test
    void test5() {
        Map<String, Object> data = new HashMap<>();
        data.put("gender", 1);

        Workbook workbook = ExcelUtil.exportByTemplate(
                "doc/user_format_dict_template.xlsx",
                data,
                params -> params.setDictHandler(new GenderDictHandler())
        );

        Path filePath = Paths.get("target", "template_export_users_format_dict.xlsx");
        ExcelUtil.exportToFile(workbook, filePath);

        System.out.println("✅ 普通变量 + dict 格式化模板导出成功：" + filePath);
    }
```

![image-20260122211340465](./assets/image-20260122211340465.png)

#### 列表变量xxx



#### 列表变量 + dict xxx



### 模板中图片动态插入

### 模板中公式保留与计算



## 参考

### 📁 EasyPoi 功能使用目录

#### 1. 环境准备与依赖引入
- 添加 Maven/Gradle 依赖
- 配置 Spring Boot（如适用）
- 基础注解类说明（@Excel、@ExcelCollection 等）

#### 2. 导出 Excel（Export）
- 2.1 简单对象导出（单表头）
- 2.2 多级表头导出（合并单元格）
- 2.3 自定义列宽、字体、样式
- 2.4 导出图片（本地路径 / Base64）
- 2.5 导出为多个 Sheet
- 2.6 大数据量导出（分批写入，避免内存溢出）

#### 3. 导入 Excel（Import）
- 3.1 基础数据导入（自动类型转换）
- 3.2 自定义校验规则（如手机号、邮箱格式）
- 3.3 导入错误信息收集与反馈
- 3.4 支持多 Sheet 导入
- 3.5 导入时忽略空行或无效行

#### 4. 模板导出（Template Export）
- 4.1 使用 Excel 模板文件（.xlsx）填充数据
- 4.2 模板中动态表格（List 数据填充）
- 4.3 模板中图片动态插入
- 4.4 模板中公式保留与计算

#### 5. 注解详解与高级用法
- @Excel：字段映射、类型、宽度、格式化等
- @ExcelCollection：一对多集合导出
- @ExcelEntity：嵌套对象支持
- 自定义字典转换（dictHandler）
- 自定义日期/数字格式

#### 6. Web 场景集成
- 6.1 Spring Boot 中导出接口（返回文件流）
- 6.2 前端上传 Excel 文件并解析
- 6.3 导出文件名中文处理（避免乱码）
- 6.4 异步导出 + 下载链接通知（可选）

#### 7. 性能与优化
- 内存控制（SXSSF 模式）
- 导出进度监听（大数据场景）
- 缓存模板提升性能

#### 8. 常见问题与解决方案
- 时间格式不一致
- 数字被识别为文本
- 导入时类型转换异常
- 中文乱码处理
- Excel 版本兼容性（.xls vs .xlsx）

---

