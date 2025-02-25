package local.ateng.java.auth.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 存储系统中的角色信息 表定义层。
 *
 * @author ATeng
 * @since 2025-02-25
 */
public class SysRoleTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储系统中的角色信息
     */
    public static final SysRoleTableDef SYS_ROLE = new SysRoleTableDef();

    /**
     * 角色ID，主键，自增
     */
    public final QueryColumn ROLE_ID = new QueryColumn(this, "role_id");

    /**
     * 角色名称
     */
    public final QueryColumn ROLE_NAME = new QueryColumn(this, "role_name");

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_TIME = new QueryColumn(this, "created_time");

    /**
     * 角色描述
     */
    public final QueryColumn DESCRIPTION = new QueryColumn(this, "description");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_TIME = new QueryColumn(this, "updated_time");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ROLE_ID, ROLE_NAME, DESCRIPTION, CREATED_TIME, UPDATED_TIME};

    public SysRoleTableDef() {
        super("", "sys_role");
    }

    private SysRoleTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public SysRoleTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new SysRoleTableDef("", "sys_role", alias));
    }

}
