package local.ateng.java.storage.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 文件分片信息表，仅在手动分片上传时使用 表定义层。
 *
 * @author ATeng
 * @since 2025-02-28
 */
public class FilePartDetailTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件分片信息表，仅在手动分片上传时使用
     */
    public static final FilePartDetailTableDef FILE_PART_DETAIL = new FilePartDetailTableDef();

    /**
     * 分片id
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 分片 ETag
     */
    public final QueryColumn E_TAG = new QueryColumn(this, "e_tag");

    /**
     * 哈希信息
     */
    public final QueryColumn HASH_INFO = new QueryColumn(this, "hash_info");

    /**
     * 文件大小，单位字节
     */
    public final QueryColumn PART_SIZE = new QueryColumn(this, "part_size");

    /**
     * 存储平台
     */
    public final QueryColumn PLATFORM = new QueryColumn(this, "platform");

    /**
     * 上传ID，仅在手动分片上传时使用
     */
    public final QueryColumn UPLOAD_ID = new QueryColumn(this, "upload_id");

    /**
     * 创建时间
     */
    public final QueryColumn CREATE_TIME = new QueryColumn(this, "create_time");

    /**
     * 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     */
    public final QueryColumn PART_NUMBER = new QueryColumn(this, "part_number");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, PLATFORM, UPLOAD_ID, E_TAG, PART_NUMBER, PART_SIZE, HASH_INFO, CREATE_TIME};

    public FilePartDetailTableDef() {
        super("", "file_part_detail");
    }

    private FilePartDetailTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public FilePartDetailTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new FilePartDetailTableDef("", "file_part_detail", alias));
    }

}
