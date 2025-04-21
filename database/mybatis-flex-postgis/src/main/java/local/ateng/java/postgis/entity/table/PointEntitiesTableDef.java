package local.ateng.java.postgis.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 *  表定义层。
 *
 * @author ATeng
 * @since 2025-04-21
 */
public class PointEntitiesTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public static final PointEntitiesTableDef POINT_ENTITIES = new PointEntitiesTableDef();

    
    public final QueryColumn ID = new QueryColumn(this, "id");

    
    public final QueryColumn GEOM = new QueryColumn(this, "geom");

    
    public final QueryColumn NAME = new QueryColumn(this, "name");

    
    public final QueryColumn CATEGORY = new QueryColumn(this, "category");

    
    public final QueryColumn CREATED_AT = new QueryColumn(this, "created_at");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, NAME, CATEGORY, CREATED_AT, GEOM};

    public PointEntitiesTableDef() {
        super("", "point_entities");
    }

    private PointEntitiesTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public PointEntitiesTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new PointEntitiesTableDef("", "point_entities", alias));
    }

}
