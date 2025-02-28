package local.ateng.java.storage.entity.table;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.table.TableDef;

import java.io.Serial;

/**
 * 文件记录表 表定义层。
 *
 * @author ATeng
 * @since 2025-02-28
 */
public class FileDetailTableDef extends TableDef {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件记录表
     */
    public static final FileDetailTableDef FILE_DETAIL = new FileDetailTableDef();

    /**
     * 文件id
     */
    public final QueryColumn ID = new QueryColumn(this, "id");

    /**
     * 文件扩展名
     */
    public final QueryColumn EXT = new QueryColumn(this, "ext");

    /**
     * 文件访问地址
     */
    public final QueryColumn URL = new QueryColumn(this, "url");

    /**
     * 附加属性
     */
    public final QueryColumn ATTR = new QueryColumn(this, "attr");

    /**
     * 存储路径
     */
    public final QueryColumn PATH = new QueryColumn(this, "path");

    /**
     * 文件大小，单位字节
     */
    public final QueryColumn SIZE = new QueryColumn(this, "size");

    /**
     * 缩略图访问路径
     */
    public final QueryColumn TH_URL = new QueryColumn(this, "th_url");

    /**
     * 缩略图大小，单位字节
     */
    public final QueryColumn TH_SIZE = new QueryColumn(this, "th_size");

    /**
     * 文件ACL
     */
    public final QueryColumn FILE_ACL = new QueryColumn(this, "file_acl");

    /**
     * 基础存储路径
     */
    public final QueryColumn BASE_PATH = new QueryColumn(this, "base_path");

    /**
     * 文件名称
     */
    public final QueryColumn FILENAME = new QueryColumn(this, "filename");

    /**
     * 哈希信息
     */
    public final QueryColumn HASH_INFO = new QueryColumn(this, "hash_info");

    /**
     * 文件元数据
     */
    public final QueryColumn METADATA = new QueryColumn(this, "metadata");

    /**
     * 文件所属对象id
     */
    public final QueryColumn OBJECT_ID = new QueryColumn(this, "object_id");

    /**
     * 存储平台
     */
    public final QueryColumn PLATFORM = new QueryColumn(this, "platform");

    /**
     * 上传ID，仅在手动分片上传时使用
     */
    public final QueryColumn UPLOAD_ID = new QueryColumn(this, "upload_id");

    /**
     * 缩略图文件ACL
     */
    public final QueryColumn TH_FILE_ACL = new QueryColumn(this, "th_file_acl");

    /**
     * 创建时间
     */
    public final QueryColumn CREATE_TIME = new QueryColumn(this, "create_time");

    /**
     * 文件所属对象类型，例如用户头像，评价图片
     */
    public final QueryColumn OBJECT_TYPE = new QueryColumn(this, "object_type");

    /**
     * 缩略图名称
     */
    public final QueryColumn TH_FILENAME = new QueryColumn(this, "th_filename");

    /**
     * 缩略图元数据
     */
    public final QueryColumn TH_METADATA = new QueryColumn(this, "th_metadata");

    /**
     * MIME类型
     */
    public final QueryColumn CONTENT_TYPE = new QueryColumn(this, "content_type");

    /**
     * 上传状态，仅在手动分片上传时使用，1：初始化完成，2：上传完成
     */
    public final QueryColumn UPLOAD_STATUS = new QueryColumn(this, "upload_status");

    /**
     * 文件用户元数据
     */
    public final QueryColumn USER_METADATA = new QueryColumn(this, "user_metadata");

    /**
     * 缩略图MIME类型
     */
    public final QueryColumn TH_CONTENT_TYPE = new QueryColumn(this, "th_content_type");

    /**
     * 缩略图用户元数据
     */
    public final QueryColumn TH_USER_METADATA = new QueryColumn(this, "th_user_metadata");

    /**
     * 原始文件名
     */
    public final QueryColumn ORIGINAL_FILENAME = new QueryColumn(this, "original_filename");

    /**
     * 所有字段。
     */
    public final QueryColumn ALL_COLUMNS = new QueryColumn(this, "*");

    /**
     * 默认字段，不包含逻辑删除或者 large 等字段。
     */
    public final QueryColumn[] DEFAULT_COLUMNS = new QueryColumn[]{ID, URL, SIZE, FILENAME, ORIGINAL_FILENAME, BASE_PATH, PATH, EXT, CONTENT_TYPE, PLATFORM, TH_URL, TH_FILENAME, TH_SIZE, TH_CONTENT_TYPE, OBJECT_ID, OBJECT_TYPE, METADATA, USER_METADATA, TH_METADATA, TH_USER_METADATA, ATTR, FILE_ACL, TH_FILE_ACL, HASH_INFO, UPLOAD_ID, UPLOAD_STATUS, CREATE_TIME};

    public FileDetailTableDef() {
        super("", "file_detail");
    }

    private FileDetailTableDef(String schema, String name, String alisa) {
        super(schema, name, alisa);
    }

    public FileDetailTableDef as(String alias) {
        String key = getNameWithSchema() + "." + alias;
        return getCache(key, k -> new FileDetailTableDef("", "file_detail", alias));
    }

}
