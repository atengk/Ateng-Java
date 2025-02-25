package local.ateng.java.auth.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 存储用户的基本信息 表定义层。
 *
 * @author ATeng
 * @since 2025-02-25
 */
public class SysUserTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储用户的基本信息
     */
    public static final SysUserTableDef SYS_USER = new SysUserTableDef();

    /**
     * 性别
     */
    public final QueryColumn SEX = new QueryColumn(this, "sex");

    /**
     * 用户邮箱
     */
    public final QueryColumn EMAIL = new QueryColumn(this, "email");

    /**
     * 用户ID，主键，自增
     */
    public final QueryColumn USER_ID = new QueryColumn(this, "user_id");

    /**
     * 用户昵称
     */
    public final QueryColumn NICK_NAME = new QueryColumn(this, "nick_name");

    /**
     * 用户密码（加密后）
     */
    public final QueryColumn PASSWORD = new QueryColumn(this, "password");

    /**
     * 用户名
     */
    public final QueryColumn USER_NAME = new QueryColumn(this, "user_name");

    /**
     * 创建时间
     */
    public final QueryColumn CREATE_TIME = new QueryColumn(this, "create_time");

    /**
     * 修改时间
     */
    public final QueryColumn UPDATE_TIME = new QueryColumn(this, "update_time");

    /**
     * 手机号码
     */
    public final QueryColumn PHONE_NUMBER = new QueryColumn(this, "phone_number");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{USER_ID, USER_NAME, NICK_NAME, PASSWORD, SEX, EMAIL, PHONE_NUMBER, CREATE_TIME, UPDATE_TIME};

    public SysUserTableDef() {
        super("", "sys_user");
    }

    private SysUserTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public SysUserTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new SysUserTableDef("", "sys_user", alias));
    }

}
