package local.ateng.java.mybatis.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 *  表定义层。
 *
 * @author ATeng
 * @since 2025-02-25
 */
public class MyUserTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public static final MyUserTableDef MY_USER = new MyUserTableDef();

    
    public final QueryColumn ID = new QueryColumn(this, "id");

    
    public final QueryColumn AGE = new QueryColumn(this, "age");

    /**
     * 用户所在城市
     */
    public final QueryColumn CITY = new QueryColumn(this, "city");

    /**
     * 用户姓名
     */
    public final QueryColumn NAME = new QueryColumn(this, "name");

    
    public final QueryColumn SCORE = new QueryColumn(this, "score");

    /**
     * 用户生日
     */
    public final QueryColumn BIRTHDAY = new QueryColumn(this, "birthday");

    /**
     * 用户所在省份
     */
    public final QueryColumn PROVINCE = new QueryColumn(this, "province");

    
    public final QueryColumn CREATE_TIME = new QueryColumn(this, "create_time");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, NAME, AGE, SCORE, BIRTHDAY, PROVINCE, CITY, CREATE_TIME};

    public MyUserTableDef() {
        super("", "my_user");
    }

    private MyUserTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public MyUserTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new MyUserTableDef("", "my_user", alias));
    }

}
