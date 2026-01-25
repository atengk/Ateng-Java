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
|                       | `fillForegroundColor`                                        | `short/int`                  | 单元格填充前景色                     | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
|                       | `fillBackgroundColor`                                        | `short/int`                  | 单元格背景色                         | —([Apache Fesod](https://fesod.apache.org/docs/sheet/write/style?utm_source=chatgpt.com)) |
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

### 多级表头导出（合并单元格）

**使用方法**

```java

```

