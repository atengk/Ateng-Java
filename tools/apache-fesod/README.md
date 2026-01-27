# Apache Fesod

快速、简洁、解决大文件内存溢出的 Java 处理电子表格工具

- [官网地址](https://fesod.apache.org/zh-cn/)



## 基础配置

**添加依赖**

```xml
<properties>
    <fesod.version>2.0.0</fesod.version>
</properties>

<dependencies>
    <!-- Apache Fesod -->
    <dependency>
        <groupId>org.apache.fesod</groupId>
        <artifactId>fesod-sheet</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.fesod</groupId>
            <artifactId>fesod-bom</artifactId>
            <version>${fesod.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
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

### 给实体类添加注解

常用注解，更多参考 [官网文档](https://fesod.apache.org/zh-cn/docs/sheet/help/annotation)

| 注解                  | 参数                                                         | 类型                         | 含义                                 | 默认值 / 备注                                                |
| --------------------- | ------------------------------------------------------------ | ---------------------------- | ------------------------------------ | ------------------------------------------------------------ |
| **@ExcelProperty**    | `value`                                                      | `String` / `String[]`        | Excel 列名，用于匹配/写入列标题      | 空（需配合 index 或 value 才生效）([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `index`                                                      | `int`                        | Excel 列索引位置                     | `-1`（不指定）([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `order`                                                      | `int`                        | 配合 `value` 排序优先级              | `Integer.MAX_VALUE`([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `converter`                                                  | `Class<? extends Converter>` | 自定义转换器类                       | 默认自动选择([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
| **@ExcelIgnore**      | *无*                                                         | /                            | 标记字段不参与 Excel 读写            | —([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
| **@DateTimeFormat**   | `value`                                                      | `String`                     | 日期时间格式（如 `yyyy-MM-dd`）      | 空([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `use1904windowing`                                           | `boolean`                    | 是否使用 1904 日期系统               | 自动选择([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
| **@NumberFormat**     | `value`                                                      | `String`                     | 数字格式（如 `#,##0.00`）            | 空([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `roundingMode`                                               | `RoundingMode`               | 小数舍入模式                         | `RoundingMode.HALF_UP`([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
| **@ColumnWidth**      | `value`                                                      | `int`                        | 列宽（字符单位）                     | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
| **@HeadFontStyle**    | `fontName`                                                   | `String`                     | 字体名称                             | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `fontHeightInPoints`                                         | `short`                      | 字体大小（磅）                       | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `bold`                                                       | `BooleanEnum`                | 是否加粗                             | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `italic`                                                     | `BooleanEnum`                | 是否斜体                             | —（可选）([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `strikeout`                                                  | `BooleanEnum`                | 是否删除线                           | —（可选）([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `color`                                                      | `short/int`                  | 字体颜色索引                         | —（可选）([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `underline`                                                  | `short/int`                  | 下划线样式                           | —（可选）([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
|                       | `charset`                                                    | `short/int`                  | 字体编码                             | —（可选）([Apache Fesod](https://fesod.apache.org/docs/help/annotation/?utm_source=chatgpt.com)) |
| **@ContentFontStyle** | 同 `@HeadFontStyle`                                          | —                            | 内容单元格字体样式参数               | 用法同上([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
| **@HeadStyle**        | `horizontalAlignment`                                        | `HorizontalAlignmentEnum`    | 水平对齐枚举                         | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `verticalAlignment`                                          | `VerticalAlignmentEnum`      | 垂直对齐枚举                         | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `verticalAlignment`                                          | `VerticalAlignmentEnum`      | 垂直对齐枚举                         | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `fillForegroundColor`                                        | `short/int`                  | 单元格填充前景色                     | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `wrapped`                                                    | `BooleanEnum.DEFAULT`        | 设置文本是否在单元格内自动换行       | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `borderLeft` / `borderRight` / `borderTop` / `borderBottom`  | `BorderStyleEnum`            | 边框样式                             | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
| *其他可选样式属性*    | `dataFormat`, `hidden`, `locked`, `quotePrefix`, `wrapped`, `rotation`, `indent`, `leftBorderColor`, `rightBorderColor`, `topBorderColor`, `bottomBorderColor`, `fillPatternType` | 多种类型                     | 详细可参考 POI 样式及 EasyExcel 文档 | 注：Fesod 也支持类似参数，但官方文档未全部列出，需要根据 POI 和源码使用([CSDN博客](https://blog.csdn.net/weixin_45151960/article/details/109095332?utm_source=chatgpt.com)) |
| **@ContentStyle**     | 同 `@HeadStyle`                                              | —                            | 内容单元格样式参数                   | 同样支持 POI 样式多数参数([CSDN博客](https://blog.csdn.net/weixin_45151960/article/details/109095332?utm_source=chatgpt.com)) |
| **@HeadRowHeight**    | `value`                                                      | `int`                        | 表头行高（单位：点/像素）            | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
| **@ContentRowHeight** | `value`                                                      | `int`                        | 内容行高（单位：点/像素）            | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |

```java
package io.github.atengk.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.format.DateTimeFormat;
import org.apache.fesod.sheet.annotation.format.NumberFormat;
import org.apache.fesod.sheet.annotation.write.style.*;
import org.apache.fesod.sheet.enums.BooleanEnum;
import org.apache.fesod.sheet.enums.poi.BorderStyleEnum;
import org.apache.fesod.sheet.enums.poi.HorizontalAlignmentEnum;
import org.apache.fesod.sheet.enums.poi.VerticalAlignmentEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@HeadFontStyle(fontName = "宋体", fontHeightInPoints = 11, bold = BooleanEnum.TRUE)
@ContentFontStyle(fontName = "宋体", fontHeightInPoints = 11, bold = BooleanEnum.FALSE)
@HeadStyle(wrapped = BooleanEnum.TRUE, horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER, fillBackgroundColor = 9, fillForegroundColor = 9, borderLeft = BorderStyleEnum.THIN, borderRight = BorderStyleEnum.THIN, borderTop = BorderStyleEnum.THIN, borderBottom = BorderStyleEnum.THIN)
@ContentStyle(wrapped = BooleanEnum.TRUE, horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER, fillBackgroundColor = 9, fillForegroundColor = 9, borderLeft = BorderStyleEnum.THIN, borderRight = BorderStyleEnum.THIN, borderTop = BorderStyleEnum.THIN, borderBottom = BorderStyleEnum.THIN)
@HeadRowHeight(25)  // 设置表头行高
@ContentRowHeight(20)  // 设置数据内容行高
@ColumnWidth(15)       // 设置列宽
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ExcelIgnore
    private Long id;

    /**
     * 名称
     */
    @ExcelProperty(value = "名称", index = 0)
    @ColumnWidth(20) // 单独设置列宽
    private String name;

    /**
     * 年龄
     */
    @ExcelProperty(value = "年龄", index = 1)
    private Integer age;

    /**
     * 手机号码
     */
    @ExcelProperty(value = "手机号码", index = 2)
    @ColumnWidth(30) // 单独设置列宽
    private String phoneNumber;

    /**
     * 邮箱
     */
    @ExcelProperty(value = "邮箱", index = 3)
    @ColumnWidth(30) // 单独设置列宽
    private String email;

    /**
     * 分数
     */
    @ExcelProperty(value = "分数", index = 4)
    @NumberFormat(value = "#,##0.00", roundingMode = RoundingMode.HALF_UP)
    private BigDecimal score;

    /**
     * 比例
     */
    @ExcelProperty(value = "比例", index = 5)
    @NumberFormat(value = "0.00%", roundingMode = RoundingMode.HALF_UP)
    private Double ratio;

    /**
     * 生日
     */
    @ExcelProperty(value = "生日", index = 6)
    @DateTimeFormat("yyyy年MM月dd日")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @ExcelProperty(value = "所在省份", index = 7)
    private String province;

    /**
     * 所在城市
     */
    @ExcelProperty(value = "所在城市", index = 8)
    private String city;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间", index = 9)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(30) // 单独设置列宽
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
        return getDataList(1000);
    }

    public static List<MyUser> getDataList(int total) {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        List<MyUser> userList = new ArrayList();
        for (int i = 1; i <= total; i++) {
            MyUser user = new MyUser();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setAge(faker.number().numberBetween(1, 100));
            user.setPhoneNumber(faker.phoneNumber().cellPhone());
            user.setEmail(faker.internet().emailAddress());
            user.setScore(BigDecimal.valueOf(faker.number().randomDouble(2, 0, 100000)));
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



## 创建工具类



## 导出 Excel（Export）

### 简单对象导出（单表头）

**使用方法**

```java
    @Test
    void testExportSimple() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_simple_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }
```

![image-20260125143734313](./assets/image-20260125143734313.png)

### `导入模版` 导出

导出一个只有表头或只有少量示例数据的一个模版，用于用户后续导入使用

```java
    @Test
    void testExportTemplate() {
        String fileName = "target/export_template_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(Collections.emptyList());
    }
```

![image-20260127142142461](./assets/image-20260127142142461.png)

### 多级表头导出（合并单元格）

多级表头通过 `@ExcelProperty` 注解指定主标题和子标题，并自动合并单元格。

假设我们希望 Excel 表头结构如下：

```
| 基本信息        | 联系方式      | 成绩信息        | 地理位置   | 时间信息         |
| 用户ID | 姓名 | 年龄 | 手机号 | 邮箱 | 分数 | 比例 | 省份 | 城市 | 生日       | 创建时间           |点击复制错误复制
```

修改 `MyUser` 实体类，修改 `value` 和 `index`

> 如果不配置 index ，最终导出的数据分组数据会乱

```
@ExcelProperty(value = {"基本信息", "名称"}, index = 0)
```

**修改后的实体类**

```java
package io.github.atengk.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.format.DateTimeFormat;
import org.apache.fesod.sheet.annotation.format.NumberFormat;
import org.apache.fesod.sheet.annotation.write.style.*;
import org.apache.fesod.sheet.enums.BooleanEnum;
import org.apache.fesod.sheet.enums.poi.BorderStyleEnum;
import org.apache.fesod.sheet.enums.poi.HorizontalAlignmentEnum;
import org.apache.fesod.sheet.enums.poi.VerticalAlignmentEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@HeadFontStyle(fontName = "宋体", fontHeightInPoints = 11, bold = BooleanEnum.TRUE)
@ContentFontStyle(fontName = "宋体", fontHeightInPoints = 11, bold = BooleanEnum.FALSE)
@HeadStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER, fillBackgroundColor = 9, fillForegroundColor = 9, borderLeft = BorderStyleEnum.THIN, borderRight = BorderStyleEnum.THIN, borderTop = BorderStyleEnum.THIN, borderBottom = BorderStyleEnum.THIN)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER, verticalAlignment = VerticalAlignmentEnum.CENTER, fillBackgroundColor = 9, fillForegroundColor = 9, borderLeft = BorderStyleEnum.THIN, borderRight = BorderStyleEnum.THIN, borderTop = BorderStyleEnum.THIN, borderBottom = BorderStyleEnum.THIN)
@HeadRowHeight(25)  // 设置表头行高
@ContentRowHeight(20)  // 设置数据内容行高
@ColumnWidth(15)       // 设置列宽
public class MyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ExcelIgnore
    private Long id;

    /**
     * 名称
     */
    @ExcelProperty(value = {"基本信息", "名称"}, index = 0)
    @ColumnWidth(20) // 单独设置列宽
    private String name;

    /**
     * 年龄
     */
    @ExcelProperty(value = {"基本信息", "年龄"}, index = 1)
    private Integer age;

    /**
     * 手机号码
     */
    @ExcelProperty(value = {"联系方式", "手机号码"}, index = 2)
    @ColumnWidth(30) // 单独设置列宽
    private String phoneNumber;

    /**
     * 邮箱
     */
    @ExcelProperty(value = {"联系方式", "邮箱"}, index = 3)
    @ColumnWidth(30) // 单独设置列宽
    private String email;

    /**
     * 分数
     */
    @ExcelProperty(value = {"成绩信息", "分数"}, index = 4)
    @NumberFormat(value = "#,##0.00", roundingMode = RoundingMode.HALF_UP)
    private BigDecimal score;

    /**
     * 比例
     */
    @ExcelProperty(value = {"成绩信息", "比例"}, index = 5)
    @NumberFormat(value = "0.00%", roundingMode = RoundingMode.HALF_UP)
    private Double ratio;

    /**
     * 生日
     */
    @ExcelProperty(value = {"时间信息", "生日"}, index = 8)
    @DateTimeFormat("yyyy年MM月dd日")
    private LocalDate birthday;

    /**
     * 所在省份
     */
    @ExcelProperty(value = {"地理位置", "所在省份"}, index = 6)
    private String province;

    /**
     * 所在城市
     */
    @ExcelProperty(value = {"地理位置", "所在城市"}, index = 7)
    private String city;

    /**
     * 创建时间
     */
    @ExcelProperty(value = {"时间信息", "创建时间"}, index = 9)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ColumnWidth(30) // 单独设置列宽
    private LocalDateTime createTime;

}
```

**使用方法**

```java
    @Test
    void testExportGroup() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_group_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }
```

![image-20260126092932936](./assets/image-20260126092932936.png)

### 导出为多个 Sheet

**使用方法**

```java
    @Test
    void testExportMultiSheet() {
        String fileName = "target/export_multi_sheet_users.xlsx";
        try (ExcelWriter excelWriter = FesodSheet.write(fileName, MyUser.class).build()) {
            for (int i = 0; i < 5; i++) {
                WriteSheet writeSheet = FesodSheet.writerSheet(i, "用户列表" + i).build();
                excelWriter.write(InitData.getDataList(), writeSheet);
            }
        }
    }
```

![image-20260126094235955](./assets/image-20260126094235955.png)

### 数据映射（Converter 转换器）

#### 创建Converter 

```java
package io.github.atengk.util;

import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;

/**
 * 性别字段 Excel 映射转换器
 * <p>
 * 功能说明：
 * 1. 导出时：将 Java 中的性别编码转换为 Excel 可读文本
 * 2. 导入时：将 Excel 中的性别文本转换为 Java 性别编码
 * <p>
 * 映射规则：
 * Java -> Excel
 * 1  -> 男
 * 2  -> 女
 * 0  -> 未知
 * <p>
 * Excel -> Java
 * 男   -> 1
 * 女   -> 2
 * 未知 -> 0
 * <p>
 * 使用方式：
 * 在实体字段上配置：
 *
 * @author 孔余
 * @ExcelProperty(value = "性别", converter = GenderConverter.class)
 * <p>
 * 适用场景：
 * - 枚举字段导入导出
 * - 字典字段导入导出
 * - 固定映射规则字段
 * @since 2026-01-26
 */
public class GenderConverter implements Converter<Integer> {

    /**
     * 返回当前转换器支持的 Java 类型
     *
     * @return Java 字段类型
     */
    @Override
    public Class<?> supportJavaTypeKey() {
        return Integer.class;
    }

    /**
     * 返回当前转换器支持的 Excel 单元格类型
     *
     * @return Excel 单元格类型枚举
     */
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    /**
     * Excel -> Java 数据转换
     * <p>
     * 在 Excel 导入时执行：
     * 将单元格中的文本转换为 Java 字段值
     *
     * @param cellData            Excel 单元格数据
     * @param contentProperty     字段内容属性
     * @param globalConfiguration 全局配置
     * @return Java 字段值
     */
    @Override
    public Integer convertToJavaData(ReadCellData<?> cellData,
                                     ExcelContentProperty contentProperty,
                                     GlobalConfiguration globalConfiguration) {

        String value = cellData.getStringValue();

        if ("男".equals(value)) {
            return 1;
        }

        if ("女".equals(value)) {
            return 2;
        }

        if ("未知".equals(value)) {
            return 0;
        }

        return null;
    }

    /**
     * Java -> Excel 数据转换
     * <p>
     * 在 Excel 导出时执行：
     * 将 Java 字段值转换为 Excel 单元格显示文本
     *
     * @param value               Java 字段值
     * @param contentProperty     字段内容属性
     * @param globalConfiguration 全局配置
     * @return Excel 单元格数据对象
     */
    @Override
    public WriteCellData<?> convertToExcelData(Integer value,
                                               ExcelContentProperty contentProperty,
                                               GlobalConfiguration globalConfiguration) {

        String text;

        if (value == null) {
            text = "";
        } else if (value == 1) {
            text = "男";
        } else if (value == 2) {
            text = "女";
        } else if (value == 0) {
            text = "未知";
        } else {
            text = "未知";
        }

        return new WriteCellData<>(text);
    }
}
```

#### 添加字段

```java
    /**
     * 性别
     */
    @ExcelProperty(value = "性别", converter = GenderConverter.class)
    private Integer gender;
```

#### 使用方法

```java
    @Test
    void testExportConverter() {
        List<MyUser> list = InitData.getDataList();
        list.forEach(item -> item.setGender(RandomUtil.randomEle(Arrays.asList(0, 1, 2))));
        String fileName = "target/export_converter_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }
```

![image-20260126151836969](./assets/image-20260126151836969.png)



### 导出图片

如果图片的字段是String，可以按照本章节操作。如果是 文件、流、字节数组、URL 就直接使用，不用配置 Converter

参考文档：[链接](https://fesod.apache.org/zh-cn/docs/sheet/write/image)

#### 创建Converter

```java
package io.github.atengk.util;

import org.apache.fesod.common.util.IoUtils;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 自定义 Excel 图片转换器，用于将 URL 字符串转换为 Excel 图片。
 * 该转换器支持：
 * 1. 通过 URL 下载图片并插入到 Excel 单元格中。
 * 2. 如果图片 URL 为空或无效，则使用默认图片 URL 进行替代。
 * 3. 保障 Excel 生成时不会因图片下载失败而导致异常。
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class StringUrlImageConverter implements Converter<String> {

    /**
     * 默认图片 URL（当原始 URL 无效或下载失败时使用）
     */
    private static final String DEFAULT_IMAGE_URL = "https://placehold.co/100x100/png?text=Default";
    private static final Logger log = LoggerFactory.getLogger(StringUrlImageConverter.class);

    /**
     * 将 URL 字符串转换为 Excel 可用的图片数据。
     *
     * @param url                 图片的 URL 字符串
     * @param contentProperty     Excel 内容属性（未使用）
     * @param globalConfiguration 全局配置（未使用）
     * @return WriteCellData<?> 包含图片字节数组的 Excel 数据单元
     */
    @Override
    public WriteCellData<?> convertToExcelData(String url, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        // 如果 URL 为空，则使用默认图片 URL
        String finalUrl = (url == null || url.trim().isEmpty()) ? DEFAULT_IMAGE_URL : url;

        // 下载图片（如果失败，则返回默认图片）
        byte[] imageBytes = downloadImage(finalUrl);
        if (imageBytes == null) {
            imageBytes = downloadImage(DEFAULT_IMAGE_URL);
        }

        return new WriteCellData<>(imageBytes);
    }

    /**
     * 从指定的 URL 下载图片并转换为字节数组。
     *
     * @param imageUrl 图片的 URL 地址
     * @return 图片的字节数组，如果下载失败则返回 null
     */
    private byte[] downloadImage(String imageUrl) {
        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            return IoUtils.toByteArray(inputStream);
        } catch (IOException e) {
            log.error("图片下载失败：{}，使用默认图片替代", imageUrl, e);
        }
        // 失败时返回 null
        return null;
    }
}
```

#### 添加图片字段

- 添加图片字段并设置 converter
- 图片内容宽度行高比例推荐：1:5

```java
@ContentRowHeight(100)  // 设置数据内容行高
public class MyUser implements Serializable {

    /**
     * 图片
     */
    @ExcelProperty(value = "图片", converter = StringUrlImageConverter.class)
    @ColumnWidth(20)
    private String imageUrl;

}
```

#### 使用方法

```java
    @Test
    void testExportImage() {
        List<MyUser> list = InitData.getDataList(5);
        String[] images = {"https://placehold.co/100x100/png?text=Ateng", "error"};
        list.forEach(item -> {
            item.setImageUrl(RandomUtil.randomEle(images));
        });
        String fileName = "target/export_image_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }
```

![image-20260126095727338](./assets/image-20260126095727338.png)

### 合并单元格

#### 创建处理器

```java
package io.github.atengk.handler;

import org.apache.fesod.sheet.write.handler.CellWriteHandler;
import org.apache.fesod.sheet.write.handler.context.CellWriteHandlerContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Excel 单元格自动纵向合并策略（支持指定列或所有列）。
 * <p>
 * 功能特点：
 * <ul>
 *     <li>连续相邻行相同值自动合并（支持多列选择）</li>
 *     <li>合并后保留原单元格样式，仅增加水平垂直居中</li>
 *     <li>支持公式计算后的显示值比较，避免类型差异</li>
 *     <li>使用缓存减少重复样式创建</li>
 * </ul>
 * 使用场景：
 * <pre>
 * EasyExcel.write(file, Data.class)
 *     .registerWriteHandler(new CellMergeHandler(0, 1))
 *     .sheet().doWrite(dataList);
 * </pre>
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class CellMergeHandler implements CellWriteHandler {

    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 需要合并的列索引（从 0 开始）。
     * 数组为空表示处理所有列。
     */
    private final int[] mergeColumns;

    /**
     * 是否合并所有列（根据 mergeColumns 判断）。
     */
    private final boolean mergeAllColumns;

    /**
     * 单元格值格式化工具，用于比较时统一格式。
     */
    private DataFormatter formatter;

    /**
     * 公式计算器（懒加载，按需创建）。
     */
    private FormulaEvaluator evaluator;

    /**
     * 样式缓存，key 为原样式，value 为添加了居中属性的新样式。
     * 避免频繁创建重复的 CellStyle 对象。
     */
    private final Map<CellStyle, CellStyle> styleCache = new HashMap<>();

    /**
     * 列合并区域缓存。
     * key 为列索引，value 为该列当前的最后一个合并区域。
     */
    private final Map<Integer, CellRangeAddress> lastMergedRegionByCol = new HashMap<>();

    /**
     * 构造方法，合并所有列。
     */
    public CellMergeHandler() {
        this.mergeColumns = new int[0];
        this.mergeAllColumns = true;
    }

    /**
     * 构造方法，仅合并指定列。
     *
     * @param mergeColumns 列索引（从 0 开始）；null 或空表示合并所有列
     */
    public CellMergeHandler(int... mergeColumns) {
        this.mergeColumns = mergeColumns == null ? new int[0] : Arrays.copyOf(mergeColumns, mergeColumns.length);
        this.mergeAllColumns = this.mergeColumns.length == 0;
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        // 跳过表头行
        if (Boolean.TRUE.equals(context.getHead())) {
            return;
        }

        // 获取当前单元格与 Sheet
        final Cell cell = context.getCell();
        if (cell == null) {
            return;
        }
        final Sheet sheet = context.getWriteSheetHolder().getSheet();
        if (sheet == null) {
            return;
        }

        // 初始化工具类（懒加载）
        if (formatter == null) {
            formatter = new DataFormatter(Locale.getDefault());
        }
        if (evaluator == null) {
            evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        }

        final int rowIndex = cell.getRowIndex();
        final int colIndex = cell.getColumnIndex();

        // 检查是否需要处理该列
        if (!shouldMergeColumn(colIndex)) {
            return;
        }

        // 第一行不合并
        if (rowIndex <= 0) {
            return;
        }

        // 获取当前值与上一行值
        final String current = getCellText(cell);
        if (isBlank(current)) {
            return;
        }
        final String prev = getCellText(getCell(sheet, rowIndex - 1, colIndex));

        // 值不同则不合并
        if (!Objects.equals(current, prev)) {
            return;
        }

        // 查找或创建合并区域
        CellRangeAddress region = lastMergedRegionByCol.get(colIndex);
        if (region == null || region.getLastRow() != rowIndex - 1) {
            Integer idx = findMergedRegionIndex(sheet, rowIndex - 1, colIndex);
            if (idx != null) {
                region = sheet.getMergedRegion(idx);
                region = new CellRangeAddress(region.getFirstRow(), region.getLastRow(),
                        region.getFirstColumn(), region.getLastColumn());
            } else {
                CellRangeAddress newRegion = new CellRangeAddress(rowIndex - 1, rowIndex, colIndex, colIndex);
                sheet.addMergedRegion(newRegion);
                setRegionCenterStyle(sheet, newRegion);
                lastMergedRegionByCol.put(colIndex, newRegion);
                return;
            }
        }

        // 扩展已有合并区域
        removeRegionIfPresent(sheet, region);
        CellRangeAddress extended = new CellRangeAddress(region.getFirstRow(), rowIndex,
                colIndex, colIndex);
        sheet.addMergedRegion(extended);
        setRegionCenterStyle(sheet, extended);
        lastMergedRegionByCol.put(colIndex, extended);
    }

    /**
     * 判断当前列是否需要合并。
     *
     * @param colIndex 列索引
     * @return 是否需要处理
     */
    private boolean shouldMergeColumn(int colIndex) {
        if (mergeAllColumns) {
            return true;
        }
        for (int c : mergeColumns) {
            if (c == colIndex) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取单元格文本值。
     * 统一使用 DataFormatter 格式化，支持公式计算。
     *
     * @param cell 单元格
     * @return 格式化后的文本
     */
    private String getCellText(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        switch (type) {
            case STRING:
                return trimToEmpty(cell.getStringCellValue());

            case NUMERIC:
                try {
                    Date date = cell.getDateCellValue();
                    return formatDate(date);
                } catch (Exception e) {
                    // 不是日期，才当普通数字处理
                    return trimToEmpty(formatter.formatCellValue(cell));
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case BLANK:
                return "";

            default:
                return trimToEmpty(formatter.formatCellValue(cell));
        }
    }

    /**
     * 时间格式化
     *
     * @param date 时间
     * @return 格式化时间字符串
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_TIME_FORMAT.format(date);
    }

    /**
     * 获取指定位置的单元格。
     *
     * @param sheet Sheet
     * @param row   行号
     * @param col   列号
     * @return 单元格
     */
    private Cell getCell(Sheet sheet, int row, int col) {
        if (sheet == null || row < 0 || col < 0) {
            return null;
        }
        Row r = sheet.getRow(row);
        return (r == null) ? null : r.getCell(col);
    }

    /**
     * 查找包含指定行列的单列合并区域索引。
     *
     * @param sheet Sheet
     * @param row   行号
     * @param col   列号
     * @return 区域索引，找不到返回 null
     */
    private Integer findMergedRegionIndex(Sheet sheet, int row, int col) {
        int num = sheet.getNumMergedRegions();
        for (int i = 0; i < num; i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstColumn() == col && region.getLastColumn() == col
                    && region.getFirstRow() <= row && row <= region.getLastRow()) {
                return i;
            }
        }
        return null;
    }

    /**
     * 删除指定的合并区域（按范围精确匹配）。
     *
     * @param sheet  Sheet
     * @param target 合并区域
     */
    private void removeRegionIfPresent(Sheet sheet, CellRangeAddress target) {
        if (sheet == null || target == null) {
            return;
        }
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.getFirstRow() == target.getFirstRow()
                    && r.getLastRow() == target.getLastRow()
                    && r.getFirstColumn() == target.getFirstColumn()
                    && r.getLastColumn() == target.getLastColumn()) {
                sheet.removeMergedRegion(i);
                return;
            }
        }
    }

    /**
     * 将合并区域设置为居中（保留原样式，使用缓存减少重复创建）。
     *
     * @param sheet  Sheet
     * @param region 合并区域
     */
    private void setRegionCenterStyle(Sheet sheet, CellRangeAddress region) {
        if (sheet == null || region == null) {
            return;
        }

        Workbook wb = sheet.getWorkbook();

        for (int row = region.getFirstRow(); row <= region.getLastRow(); row++) {
            Row r = sheet.getRow(row);
            if (r == null) {
                continue;
            }

            for (int col = region.getFirstColumn(); col <= region.getLastColumn(); col++) {
                Cell cell = r.getCell(col);
                if (cell == null) {
                    continue;
                }

                CellStyle oldStyle = cell.getCellStyle();
                CellStyle newStyle = styleCache.get(oldStyle);
                if (newStyle == null) {
                    newStyle = wb.createCellStyle();
                    if (oldStyle != null) {
                        newStyle.cloneStyleFrom(oldStyle);
                    }
                    newStyle.setAlignment(HorizontalAlignment.CENTER);
                    newStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                    styleCache.put(oldStyle, newStyle);
                }
                cell.setCellStyle(newStyle);
            }
        }
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param s 字符串
     * @return 是否为空白
     */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 去除首尾空格，null 转为空串。
     *
     * @param s 字符串
     * @return 去空格后的字符串
     */
    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
```

#### 使用方法

- 所有列相同的内容自动合并，不用传参：`new CellMergeStrategy()`
- 指定列相同的内容自动合并，传参列索引，从0开始，和 `@ExcelProperty(index = 9)` 保持一致：`new CellMergeStrategy(0,1,2,3,4,5,6, 7, 8, 9)`

```java
    @Test
    void testExportMerge() {
        List<MyUser> list = InitData.getDataList();
        // 数据按照省份+城市排序
        list.sort(Comparator
                .comparing(MyUser::getProvince)
                .thenComparing(MyUser::getCity));
        String fileName = "target/export_merge_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                //.registerWriteHandler(new CellMergeHandler(0,1,2,3,4,5,6, 7, 8, 9))
                .registerWriteHandler(new CellMergeHandler())
                .sheet("用户列表")
                .doWrite(list);
    }
```

![image-20260126113059945](./assets/image-20260126113059945.png)

### 生成下拉

#### 创建处理器

```java
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
```

#### 使用方法

```java
    @Test
    void testExportDropdown() {
        String fileName = "target/export_dropdown_users.xlsx";

        Map<Integer, String[]> dropdownMap = new HashMap<>();
        dropdownMap.put(1, new String[]{"1", "2"});           // 第 2 列
        dropdownMap.put(3, new String[]{"男", "女", "未知"});  // 第 4 列

        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(new DropdownHandler(dropdownMap, 1))
                .sheet("用户列表")
                .doWrite(Collections.emptyList());
    }
```

![image-20260126135624771](./assets/image-20260126135624771.png)

### 生成批注

#### 创建处理器

```java
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
```

#### 使用方法

```java
    @Test
    void testExportComment() {
        String fileName = "target/export_comment_users.xlsx";

        Map<Integer, String> commentMap = new HashMap<>();
        commentMap.put(0, "请输入用户姓名，必填");
        commentMap.put(1, "请输入年龄，必须是正整数");
        commentMap.put(2, "手机号格式：11 位数字");
        commentMap.put(4, "分数范围：0 ~ 100");

        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(new CommentHandler(commentMap))
                .sheet("用户列表")
                .doWrite(Collections.emptyList());
    }
```

- 每个指定列的表头右上角都有一个红色小三角
- 鼠标悬停显示你写的说明文字

![image-20260126141121014](./assets/image-20260126141121014.png)

### 冻结表头

#### 创建处理器

```java
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
```

#### 使用方法

```java
    @Test
    void testExportFreezeHead() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_freeze_head_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(new FreezeHeadHandler())
                .sheet("用户列表")
                .doWrite(list);
    }
```

![image-20260126155732959](./assets/image-20260126155732959.png)

### 自定义样式

#### 创建样式工具类

```java
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
```

#### 使用方法

**使用默认配置**

```java
    @Test
    void testExportStyle() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_style_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(ExcelStyleUtil.getDefaultStyleStrategy())
                .sheet("用户列表")
                .doWrite(list);
    }
```

![image-20260126171849203](./assets/image-20260126171849203.png)

**自定义配置**

```java
    @Test
    void testExportCustomStyle() {
        List<MyUser> list = InitData.getDataList();

        // 默认表头字体大小（磅）
         Short DEFAULT_HEADER_FONT_SIZE = 14;
        // 默认内容字体大小（磅）
         Short DEFAULT_CONTENT_FONT_SIZE = 12;
        // 默认内容字体
         String DEFAULT_CONTENT_FONT_NAME = "微软雅黑";
        HorizontalCellStyleStrategy cellStyleStrategy = ExcelStyleUtil.buildCustomStyleStrategy(
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
                true
        );
        String fileName = "target/export_custom_style_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(cellStyleStrategy)
                .sheet("用户列表")
                .doWrite(list);
    }
```

![image-20260126172242970](./assets/image-20260126172242970.png)

### 条件样式

#### 创建处理器

```java
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
```

#### 使用方法

```java

    @Test
    void testExportConditionStyle() {
        String fileName = "target/export_condition_style.xlsx";

        ConditionStyleHandler handler = new ConditionStyleHandler();

        // 数字判断，示例：第3列年龄 > 10000 则背景黄+字体红+加粗
        handler.addRule(1, new ConditionStyleHandler.ConditionRule(v -> {
                    if (v instanceof Number) {
                        return ((Number) v).doubleValue() > 60;
                    }
                    return false;
                }).backgroundColor(IndexedColors.YELLOW.getIndex())
                        .fontColor(IndexedColors.RED.getIndex())
                        .bold(true)
        );

        // 字符串判断
        handler.addRule(8, new ConditionStyleHandler.ConditionRule(v ->
                v instanceof String && "重庆".equals(v))
                .backgroundColor(IndexedColors.RED.getIndex())
                .fontColor(IndexedColors.WHITE.getIndex())
                .bold(true)
        );

        // 时间判断，示例：第 9 列是 LocalDateTime 类型，但 Excel 中会以 Double 存储
        handler.addRule(9, new ConditionStyleHandler.ConditionRule(v -> {
                    if (!(v instanceof Double)) {
                        return false;
                    }

                    LocalDateTime time = excelDateToLocalDateTime((Double) v);

                    // 判断逻辑
                    return time.isAfter(LocalDateTime.of(2026, 1, 26, 0, 0));
                }).backgroundColor(IndexedColors.BLUE.getIndex())
                        .fontColor(IndexedColors.GREEN.getIndex())
                        .bold(true)
        );

        FesodSheet
                .write(fileName, MyUser.class)
                .registerWriteHandler(handler)
                .sheet("用户列表")
                .doWrite(InitData.getDataList());
    }

    private static LocalDateTime excelDateToLocalDateTime(double excelDate) {
        // 25569 是 1970-01-01 和 1900-01-01 的天数差
        long epochSecond = (long) ((excelDate - 25569) * 86400);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault());
    }
```

![image-20260126214421594](./assets/image-20260126214421594.png)

### 使用 `List<Map>` 导出（无实体类）

#### 简单动态数据

**使用方法**

```java
    @Test
    void testExportDynamic() {
        // 动态生成一级表头
        List<String> headers = new ArrayList<>();
        int randomInt = RandomUtil.randomInt(1, 20);
        for (int i = 0; i < randomInt; i++) {
            headers.add("表头" + (i + 1));
        }
        System.out.println(headers);

        // 动态生成 Map 数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j), "数据" + (i + 1) + "-" + (j + 1));
            }
            dataList.add(row);
        }
        System.out.println(dataList);

        // 导出
        ExcelUtil.exportDynamicSimple(
                ExcelUtil.toOutputStream("target/export_dynamic.xlsx"),
                headers,
                dataList,
                "用户列表"
        );
    }
```

> 输出：
>
> [表头1, 表头2, 表头3, 表头4, 表头5, 表头6, 表头7, 表头8, 表头9, 表头10]
> [{表头6=数据1-6, 表头7=数据1-7, 表头4=数据1-4, 表头5=数据1-5, 表头8=数据1-8, 表头9=数据1-9, 表头10=数据1-10, 表头2=数据1-2, 表头3=数据1-3, 表头1=数据1-1}, {表头6=数据2-6, 表头7=数据2-7, 表头4=数据2-4, 表头5=数据2-5, 表头8=数据2-8, 表头9=数据2-9, 表头10=数据2-10, 表头2=数据2-2, 表头3=数据2-3, 表头1=数据2-1}, {表头6=数据3-6, 表头7=数据3-7, 表头4=数据3-4, 表头5=数据3-5, 表头8=数据3-8, 表头9=数据3-9, 表头10=数据3-10, 表头2=数据3-2, 表头3=数据3-3, 表头1=数据3-1}, {表头6=数据4-6, 表头7=数据4-7, 表头4=数据4-4, 表头5=数据4-5, 表头8=数据4-8, 表头9=数据4-9, 表头10=数据4-10, 表头2=数据4-2, 表头3=数据4-3, 表头1=数据4-1}, {表头6=数据5-6, 表头7=数据5-7, 表头4=数据5-4, 表头5=数据5-5, 表头8=数据5-8, 表头9=数据5-9, 表头10=数据5-10, 表头2=数据5-2, 表头3=数据5-3, 表头1=数据5-1}, {表头6=数据6-6, 表头7=数据6-7, 表头4=数据6-4, 表头5=数据6-5, 表头8=数据6-8, 表头9=数据6-9, 表头10=数据6-10, 表头2=数据6-2, 表头3=数据6-3, 表头1=数据6-1}, {表头6=数据7-6, 表头7=数据7-7, 表头4=数据7-4, 表头5=数据7-5, 表头8=数据7-8, 表头9=数据7-9, 表头10=数据7-10, 表头2=数据7-2, 表头3=数据7-3, 表头1=数据7-1}, {表头6=数据8-6, 表头7=数据8-7, 表头4=数据8-4, 表头5=数据8-5, 表头8=数据8-8, 表头9=数据8-9, 表头10=数据8-10, 表头2=数据8-2, 表头3=数据8-3, 表头1=数据8-1}, {表头6=数据9-6, 表头7=数据9-7, 表头4=数据9-4, 表头5=数据9-5, 表头8=数据9-8, 表头9=数据9-9, 表头10=数据9-10, 表头2=数据9-2, 表头3=数据9-3, 表头1=数据9-1}, {表头6=数据10-6, 表头7=数据10-7, 表头4=数据10-4, 表头5=数据10-5, 表头8=数据10-8, 表头9=数据10-9, 表头10=数据10-10, 表头2=数据10-2, 表头3=数据10-3, 表头1=数据10-1}]

![image-20260127102136641](./assets/image-20260127102136641.png)

#### 导出图片

**使用方法**

字段是二进制就会转换成图片

```java
    @Test
    void testExportDynamicImage() {
        // 表头
        List<ExcelUtil.HeaderItem> headers = new ArrayList<>();
        headers.add(new ExcelUtil.HeaderItem(
                Collections.singletonList("姓名"), "name"));
        headers.add(new ExcelUtil.HeaderItem(
                Collections.singletonList("年龄"), "age"));
        headers.add(new ExcelUtil.HeaderItem(
                Collections.singletonList("图片"), "image"));
        System.out.println(JSONUtil.toJsonStr(headers));

        // 数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "用户" + (i + 1));
            row.put("age", 20 + i);
            row.put("image", HttpUtil.downloadBytes("https://placehold.co/100x100/png"));
            dataList.add(row);
        }
        //System.out.println(JSONUtil.toJsonStr(dataList));

        // 导出
        ExcelUtil.exportDynamic(
                ExcelUtil.toOutputStream("target/export_dynamic_image.xlsx"),
                headers,
                dataList,
                "用户列表"
        );
    }
```

> 输出：
>
> [{"path":["姓名"],"field":"name"},{"path":["年龄"],"field":"age"},{"path":["图片"],"field":"image"}]

![image-20260127103027017](./assets/image-20260127103027017.png)

#### 多级表头

**使用方法**

```java
    @Test
    void testExportDynamicMultiHead() {
        // 多级表头（一级 + 二级）
        List<ExcelUtil.HeaderItem> headers = new ArrayList<>();
        headers.add(new ExcelUtil.HeaderItem(
                Arrays.asList("用户信息", "姓名"), "name"));
        headers.add(new ExcelUtil.HeaderItem(
                Arrays.asList("用户信息", "年龄"), "age"));
        headers.add(new ExcelUtil.HeaderItem(
                Arrays.asList("系统信息", "登录次数"), "loginCount"));
        System.out.println(JSONUtil.toJsonStr(headers));

        // 数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "用户" + (i + 1));
            row.put("age", 20 + i);
            row.put("loginCount", 100 + i);
            dataList.add(row);
        }
        System.out.println(JSONUtil.toJsonStr(dataList));

        // 导出
        ExcelUtil.exportDynamicComplex(
                ExcelUtil.toOutputStream("target/export_dynamic_multi_head.xlsx"),
                headers,
                dataList,
                "用户列表"
        );
    }
```

> 输出：
>
> [{"path":["用户信息","姓名"],"field":"name"},{"path":["用户信息","年龄"],"field":"age"},{"path":["系统信息","登录次数"],"field":"loginCount"}]
> [{"name":"用户1","age":20,"loginCount":100},{"name":"用户2","age":21,"loginCount":101},{"name":"用户3","age":22,"loginCount":102},{"name":"用户4","age":23,"loginCount":103},{"name":"用户5","age":24,"loginCount":104},{"name":"用户6","age":25,"loginCount":105},{"name":"用户7","age":26,"loginCount":106},{"name":"用户8","age":27,"loginCount":107},{"name":"用户9","age":28,"loginCount":108},{"name":"用户10","age":29,"loginCount":109}]

![image-20260127102358005](./assets/image-20260127102358005.png)

#### 多 Sheet

**使用方法**

```java
    @Test
    void testExportDynamicMultiSheet() {
        // 表头
        List<ExcelUtil.HeaderItem> headers = Arrays.asList(
                new ExcelUtil.HeaderItem(Collections.singletonList("姓名"), "name"),
                new ExcelUtil.HeaderItem(Collections.singletonList("年龄"), "age"),
                new ExcelUtil.HeaderItem(Collections.singletonList("登录次数"), "loginCount")
        );
        // 数据
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "用户" + (i + 1));
            row.put("age", 20 + i);
            row.put("loginCount", 100 + i);
            rows.add(row);
        }
        // Sheet
        List<ExcelUtil.SheetData> sheets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            sheets.add(new ExcelUtil.SheetData("用户列表" + i, headers, rows));
        }
        System.out.println(sheets);
        // 导出
        ExcelUtil.exportDynamicMultiSheet(
                ExcelUtil.toOutputStream("target/export_dynamic_multi_sheet.xlsx"),
                sheets
        );
    }
```

![image-20260127102449064](./assets/image-20260127102449064.png)

#### 调整列宽

##### 创建处理器

```java
package io.github.atengk.handler;

import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.handler.context.SheetWriteHandlerContext;
import org.apache.fesod.sheet.write.metadata.holder.WriteSheetHolder;
import org.apache.fesod.sheet.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.Collections;
import java.util.Map;

/**
 * Sheet 级别行高、列宽统一处理器。
 * <p>
 * 功能：
 * <ul>
 *     <li>自动识别表头行数（支持多级表头）</li>
 *     <li>设置表头行高</li>
 *     <li>设置内容行高</li>
 *     <li>设置指定列的列宽</li>
 * </ul>
 * <p>
 * 设计原则：
 * <ul>
 *     <li>列宽在 Sheet 创建时设置</li>
 *     <li>行高在 Sheet 写入完成后设置</li>
 *     <li>列宽单位使用“字符宽度”，内部自动 *256</li>
 *     <li>使用者不需要关心表头是几级</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class RowColumnDimensionHandler implements SheetWriteHandler {

    /**
     * 表头行高（单位：磅）
     */
    private final short headRowHeight;

    /**
     * 内容行高（单位：磅）
     */
    private final short contentRowHeight;

    /**
     * 指定列宽配置
     * key：列索引（从 0 开始）
     * value：列宽（字符宽度，不需要乘 256）
     */
    private final Map<Integer, Integer> columnWidthMap;

    public RowColumnDimensionHandler(short headRowHeight,
                                     short contentRowHeight,
                                     Map<Integer, Integer> columnWidthMap) {
        this.headRowHeight = headRowHeight;
        this.contentRowHeight = contentRowHeight;
        this.columnWidthMap = columnWidthMap == null ? Collections.emptyMap() : columnWidthMap;
    }

    /**
     * Sheet 创建完成后回调。
     * <p>
     * 此时 Row 还未创建，只能做 Sheet 结构类操作：
     * <ul>
     *     <li>列宽</li>
     *     <li>冻结窗格</li>
     *     <li>打印设置</li>
     * </ul>
     */
    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder,
                                 WriteSheetHolder writeSheetHolder) {

        Sheet sheet = writeSheetHolder.getSheet();
        setColumnWidth(sheet);
    }

    /**
     * Sheet 写入完成后的回调。
     * <p>
     * 此时：
     * <ul>
     *     <li>所有 Row 已存在</li>
     *     <li>可以安全设置行高</li>
     * </ul>
     */
    @Override
    public void afterSheetDispose(SheetWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        setRowHeight(sheet, context.getWriteSheetHolder());
    }

    /**
     * 设置列宽（字符宽度 → Excel 单位 *256）
     */
    private void setColumnWidth(Sheet sheet) {
        if (columnWidthMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Integer> entry : columnWidthMap.entrySet()) {
            Integer columnIndex = entry.getKey();
            Integer columnWidth = entry.getValue();

            if (columnIndex == null || columnWidth == null || columnWidth <= 0) {
                continue;
            }

            sheet.setColumnWidth(columnIndex, columnWidth * 256);
        }
    }

    /**
     * 设置行高，自动区分表头行和内容行。
     */
    private void setRowHeight(Sheet sheet, WriteSheetHolder writeSheetHolder) {

        int headRowCount = writeSheetHolder
                .getExcelWriteHeadProperty()
                .getHeadRowNumber();

        int lastRowNum = sheet.getLastRowNum();

        for (int rowIndex = 0; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            if (rowIndex < headRowCount) {
                row.setHeightInPoints(headRowHeight);
            } else {
                row.setHeightInPoints(contentRowHeight);
            }
        }
    }
}
```

##### 使用方法

```java
    @Test
    void testExportDynamicRowColumn() {
        // 动态生成一级表头
        List<String> headers = new ArrayList<>();
        int randomInt = RandomUtil.randomInt(1, 20);
        for (int i = 0; i < randomInt; i++) {
            headers.add("表头" + (i + 1));
        }
        System.out.println(headers);

        // 动态生成 Map 数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j), "数据" + (i + 1) + "-" + (j + 1));
            }
            dataList.add(row);
        }
        System.out.println(dataList);

        // 设置列宽
        Map<Integer, Integer> columnWidthMap = new HashMap<>();
        columnWidthMap.put(0, 20);
        columnWidthMap.put(1, 30);
        columnWidthMap.put(2, 25);

        // 设置表头、内容高度
        RowColumnDimensionHandler handler = new RowColumnDimensionHandler(
                (short) 50,
                (short) 30,
                columnWidthMap
        );

        // 导出
        ExcelUtil.exportDynamicSimple(
                ExcelUtil.toOutputStream("target/export_dynamic_row_column.xlsx"),
                headers,
                dataList,
                "用户列表",
                handler,
                ExcelStyleUtil.getDefaultStyleStrategy()
        );
    }
```

![image-20260127141509063](./assets/image-20260127141509063.png)



### 导出CSV

详情参考官网文档：[链接](https://fesod.apache.org/zh-cn/docs/sheet/write/csv)

#### 实体导出

```java
    @Test
    void testExportSimple() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_simple_users.csv";
        FesodSheet
                .write(fileName, MyUser.class)
                .csv()
                .doWrite(list);
    }
```

![image-20260127155926394](./assets/image-20260127155926394.png)

#### 动态数据导出

```java
    @Test
    void testExportDynamic() {
        // 表头
        List<ExcelUtil.HeaderItem> headers = Arrays.asList(
                new ExcelUtil.HeaderItem(Collections.singletonList("姓名"), "name"),
                new ExcelUtil.HeaderItem(Collections.singletonList("年龄"), "age"),
                new ExcelUtil.HeaderItem(Collections.singletonList("登录次数"), "loginCount")
        );
        // 数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "用户" + (i + 1));
            row.put("age", 20 + i);
            row.put("loginCount", 100 + i);
            dataList.add(row);
        }
        System.out.println(JSONUtil.toJsonStr(dataList));
        // 导出
        String fileName = "target/export_dynamic_users.csv";
        FesodSheet
                .write(fileName)
                .head(ExcelUtil.buildHead(headers))
                .csv()
                .doWrite(ExcelUtil.buildRows(headers, dataList));
    }
```

![image-20260127155957511](./assets/image-20260127155957511.png)

#### 分批次导出

```java
    @Test
    void testExportBatch() {
        String fileName = "target/export_batch_users.csv";

        try (ExcelWriter excelWriter = FesodSheet
                .write(fileName, MyUser.class)
                .excelType(ExcelTypeEnum.CSV)
                .build()) {
            WriteSheet writeSheet = FastExcel.writerSheet().build();
            // 第一批数据
            excelWriter.write(InitData.getDataList(2), writeSheet);
            // 第二批数据
            excelWriter.write(InitData.getDataList(2), writeSheet);
        }
    }
```

![image-20260127160034936](./assets/image-20260127160034936.png)



## SpringBoot 使用

### 导出数据

**使用方法**

```java
    /**
     * 导出Excel
     */
    @GetMapping("/entity")
    public void exportEntity(HttpServletResponse response) {
        List<MyUser> list = InitData.getDataList();
        String fileName = "用户列表.xlsx";
        ExcelUtil.exportToResponse(
                response,
                fileName,
                list,
                MyUser.class,
                "用户列表"
        );
    }
```

![image-20260127153438525](./assets/image-20260127153438525.png)

### 导出动态数据

**使用方法**

```java
    /**
     * 动态导出 Excel
     */
    @GetMapping("/dynamic")
    public void exportDynamic(HttpServletResponse response) {

        // 生成随机表头
        List<String> headers = new ArrayList<>();
        int randomInt = new Random().nextInt(20) + 1;
        for (int i = 0; i < randomInt; i++) {
            headers.add("表头" + (i + 1));
        }

        // 生成随机数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j), "数据" + (i + 1) + "-" + (j + 1));
            }
            dataList.add(row);
        }

        // 导出文件
        String fileName = "动态导出.xlsx";
        ExcelUtil.exportDynamicSimpleToResponse(
                response, 
                fileName, 
                headers, 
                dataList, 
                "用户列表"
        );
    }
```

![image-20260127112241102](./assets/image-20260127112241102.png)

