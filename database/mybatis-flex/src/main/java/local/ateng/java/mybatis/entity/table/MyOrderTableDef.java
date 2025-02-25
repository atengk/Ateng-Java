package local.ateng.java.mybatis.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 订单信息表，存储用户的订单数据 表定义层。
 *
 * @author ATeng
 * @since 2025-02-25
 */
public class MyOrderTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单信息表，存储用户的订单数据
     */
    public static final MyOrderTableDef MY_ORDER = new MyOrderTableDef();

    /**
     * 订单ID，主键，自增
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 订单日期
     */
    public final QueryColumn DATE = new QueryColumn(this, "date");

    /**
     * 用户ID，外键，关联用户表
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 订单总金额，精确到小数点后两位
     */
    public final QueryColumn TOTAL_AMOUNT = new QueryColumn(this, "total_amount");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, USER_ID, DATE, TOTAL_AMOUNT};

    public MyOrderTableDef() {
        super("", "my_order");
    }

    private MyOrderTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public MyOrderTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new MyOrderTableDef("", "my_order", alias));
    }

}
