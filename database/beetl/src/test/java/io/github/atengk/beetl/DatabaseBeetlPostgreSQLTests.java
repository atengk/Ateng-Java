package io.github.atengk.beetl;

import io.github.atengk.beetl.model.ColumnMeta;
import io.github.atengk.beetl.model.TableMeta;
import io.github.atengk.beetl.service.DynamicDdlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.List;

@SpringBootTest
public class DatabaseBeetlPostgreSQLTests {

    @Autowired
    private DynamicDdlService dynamicDdlService;
    @Qualifier("postgresqlDataSource")
    @Autowired
    DataSource dataSource;

    /**
     * 常用字段创建示例
     * CREATE TABLE t_demo_all_columns
     * (
     *     id          bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
     *     biz_no      varchar(64)     NOT NULL UNIQUE COMMENT '业务编号',
     *     name        varchar(128)    NOT NULL COMMENT '名称',
     *     status      tinyint         NOT NULL DEFAULT 0 COMMENT '状态：0-禁用 1-启用',
     *     amount      decimal(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '金额',
     *     remark      text COMMENT '备注',
     *     create_time datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     *     update_time datetime        NOT NULL
     *         ON UPDATE
     *             CURRENT_TIMESTAMP COMMENT '更新时间',
     *     deleted     tinyint         NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
     *     PRIMARY KEY (id)
     * ) ENGINE = InnoDB
     *   DEFAULT CHARSET = utf8mb4
     *   COLLATE = utf8mb4_general_ci COMMENT ='字段能力对照表';
     */
    @Test
    public void testCreateTable_AllCommonColumns_Pgsql() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_all_columns");
        table.setIfNotExists(true);
        table.setComment("字段能力对照表");

        ColumnMeta id = new ColumnMeta();
        id.setName("id");
        id.setType("bigint");
        id.setPrimaryKey(true);
        id.setAutoIncrement(true);
        id.setNullable(false);
        id.setComment("主键ID");

        ColumnMeta bizNo = new ColumnMeta();
        bizNo.setName("biz_no");
        bizNo.setType("varchar");
        bizNo.setLength(64);
        bizNo.setUnique(true);
        bizNo.setNullable(false);
        bizNo.setComment("业务编号");

        ColumnMeta name = new ColumnMeta();
        name.setName("name");
        name.setType("varchar");
        name.setLength(128);
        name.setNullable(false);
        name.setComment("名称");

        ColumnMeta status = new ColumnMeta();
        status.setName("status");
        status.setType("smallint");
        status.setNullable(false);
        status.setDefaultValue("0");
        status.setComment("状态：0-禁用 1-启用");

        ColumnMeta amount = new ColumnMeta();
        amount.setName("amount");
        amount.setType("numeric");
        amount.setPrecision(10);
        amount.setScale(2);
        amount.setNullable(false);
        amount.setDefaultValue("0.00");
        amount.setComment("金额");

        ColumnMeta remark = new ColumnMeta();
        remark.setName("remark");
        remark.setType("text");
        remark.setNullable(true);
        remark.setComment("备注");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("timestamp");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        ColumnMeta updateTime = new ColumnMeta();
        updateTime.setName("update_time");
        updateTime.setType("timestamp");
        updateTime.setNullable(false);
        updateTime.setDefaultValue("CURRENT_TIMESTAMP");
        updateTime.setComment("更新时间");

        ColumnMeta deleted = new ColumnMeta();
        deleted.setName("deleted");
        deleted.setType("smallint");
        deleted.setNullable(false);
        deleted.setDefaultValue("0");
        deleted.setComment("逻辑删除标识");

        table.setColumns(List.of(
                id,
                bizNo,
                name,
                status,
                amount,
                remark,
                createTime,
                updateTime,
                deleted
        ));

        dynamicDdlService.createTable(dataSource, table);
    }

    /**
     * 联合主键创建示例
     * CREATE TABLE IF NOT EXISTS t_demo_order_item
     * (
     *     order_id     bigint      NOT NULL,
     *     item_id      bigint      NOT NULL,
     *     product_code varchar(64) NOT NULL,
     *     quantity     int         NOT NULL DEFAULT 1,
     *     create_time  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
     *     PRIMARY KEY (order_id, item_id)
     * );
     * COMMENT
     *     ON TABLE t_demo_order_item IS '订单明细（联合主键示例）';
     * COMMENT
     *     ON COLUMN t_demo_order_item.order_id IS '订单ID';
     * COMMENT
     *     ON COLUMN t_demo_order_item.item_id IS '明细ID';
     * COMMENT
     *     ON COLUMN t_demo_order_item.product_code IS '商品编码';
     * COMMENT
     *     ON COLUMN t_demo_order_item.quantity IS '数量';
     * COMMENT
     *     ON COLUMN t_demo_order_item.create_time IS '创建时间';
     */
    @Test
    public void testCreateTable_CompositePrimaryKey() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_order_item");
        table.setIfNotExists(true);
        table.setComment("订单明细（联合主键示例）");
        

        ColumnMeta orderId = new ColumnMeta();
        orderId.setName("order_id");
        orderId.setType("bigint");
        orderId.setPrimaryKey(true);
        orderId.setNullable(false);
        orderId.setComment("订单ID");

        ColumnMeta itemId = new ColumnMeta();
        itemId.setName("item_id");
        itemId.setType("bigint");
        itemId.setPrimaryKey(true);
        itemId.setNullable(false);
        itemId.setComment("明细ID");

        ColumnMeta productCode = new ColumnMeta();
        productCode.setName("product_code");
        productCode.setType("varchar");
        productCode.setLength(64);
        productCode.setNullable(false);
        productCode.setComment("商品编码");

        ColumnMeta quantity = new ColumnMeta();
        quantity.setName("quantity");
        quantity.setType("int");
        quantity.setNullable(false);
        quantity.setDefaultValue("1");
        quantity.setComment("数量");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("timestamp");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        table.setColumns(List.of(
                orderId,
                itemId,
                productCode,
                quantity,
                createTime
        ));

        dynamicDdlService.createTable(dataSource, table);
    }

    /**
     * 创建JSON字段表
     * CREATE TABLE IF NOT EXISTS t_demo_json
     * (
     *     id          bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
     *     config      json                                NOT NULL,
     *     ext         json,
     *     create_time timestamp                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
     *     PRIMARY KEY (id)
     * );
     * COMMENT
     *     ON TABLE t_demo_json IS 'JSON 字段示例';
     * COMMENT
     *     ON COLUMN t_demo_json.id IS '主键';
     * COMMENT
     *     ON COLUMN t_demo_json.config IS '配置信息(JSON)';
     * COMMENT
     *     ON COLUMN t_demo_json.ext IS '扩展字段';
     * COMMENT
     *     ON COLUMN t_demo_json.create_time IS '创建时间';
     */
    @Test
    public void testCreateTable_WithJsonColumn() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_json");
        table.setIfNotExists(true);
        table.setComment("JSON 字段示例");
        

        ColumnMeta id = new ColumnMeta();
        id.setName("id");
        id.setType("bigint");
        id.setPrimaryKey(true);
        id.setAutoIncrement(true);
        id.setNullable(false);
        id.setComment("主键");

        ColumnMeta config = new ColumnMeta();
        config.setName("config");
        config.setType("json");
        config.setNullable(false);
        config.setComment("配置信息(JSON)");

        ColumnMeta ext = new ColumnMeta();
        ext.setName("ext");
        ext.setType("json");
        ext.setNullable(true);
        ext.setComment("扩展字段");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("timestamp");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        table.setColumns(List.of(
                id,
                config,
                ext,
                createTime
        ));

        dynamicDdlService.createTable(dataSource, table);
    }


}
