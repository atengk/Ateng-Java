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

### 自定义列宽、字体、样式

**创建自定义样式处理器**

```java
package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

public class MyExcelStyle extends AbstractExcelExportStyler {

    /**
     * 构造器中必须调用 createStyles
     */
    public MyExcelStyle(Workbook workbook) {
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

        // 水平 + 垂直居中
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * 表头（多级表头时用）
     */
    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    /**
     * 普通字符串样式（父类在 createStyles 中会调用）
     */
    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWarp) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        // 水平 + 垂直居中
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWarp);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * 数字/特殊字段样式（父类在 createStyles 中会调用）
     */
    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWarp) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWarp);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * 模板 foreach 场景，一般直接复用普通样式
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return this.stringNoneStyle;
    }

    /**
     * 覆盖 EasyPOI 默认的“奇偶行斑马纹”样式选择逻辑。
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return this.stringNoneStyle;
    }

    /**
     * 根据单元格数据内容返回最终使用的样式
     */
    @Override
    public CellStyle getStyles(Cell cell,
                               int dataRow,
                               ExcelExportEntity entity,
                               Object obj,
                               Object data) {
        return this.stringNoneStyle;
    }

}
```

使用方法

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

```
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

重点是 `dict = "ageDict"` ，这个 key 要和 handler 里保持一致。

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

### 枚举处理xx

### 下拉生成xx

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

#### 字典映射

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

