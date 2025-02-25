package local.ateng.java.auth.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 存储系统中的权限信息 表定义层。
 *
 * @author ATeng
 * @since 2025-02-25
 */
public class SysPermissionTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储系统中的权限信息
     */
    public static final SysPermissionTableDef SYS_PERMISSION = new SysPermissionTableDef();

    /**
     * 创建时间
     */
    public final QueryColumn CREATED_TIME = new QueryColumn(this, "created_time");

    /**
     * 权限描述
     */
    public final QueryColumn DESCRIPTION = new QueryColumn(this, "description");

    /**
     * 更新时间
     */
    public final QueryColumn UPDATED_TIME = new QueryColumn(this, "updated_time");

    /**
     * 权限ID，主键，自增
     */
    public final QueryColumn PERMISSION_ID = new QueryColumn(this, "permission_id");

    /**
     * 权限名称
     */
    public final QueryColumn PERMISSION_NAME = new QueryColumn(this, "permission_name");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{PERMISSION_ID, PERMISSION_NAME, DESCRIPTION, CREATED_TIME, UPDATED_TIME};

    public SysPermissionTableDef() {
        super("", "sys_permission");
    }

    private SysPermissionTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public SysPermissionTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new SysPermissionTableDef("", "sys_permission", alias));
    }

}
