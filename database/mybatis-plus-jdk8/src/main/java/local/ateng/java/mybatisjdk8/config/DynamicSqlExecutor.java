package local.ateng.java.mybatisjdk8.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import local.ateng.java.mybatisjdk8.exception.DynamicSqlException;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 动态 SQL 执行器
 *
 * <p>该组件用于在运行时动态构建并执行 MyBatis MappedStatement。
 * 支持 SELECT、INSERT、UPDATE、DELETE 四种 SQL 类型，
 * 并可结合 MyBatis-Plus 的 {@link TableId} 注解解析主键字段，
 * 在插入操作中启用主键回填功能。</p>
 *
 * <p>核心功能包括：
 * <ul>
 *     <li>运行时注册 {@link MappedStatement}</li>
 *     <li>动态生成唯一的 statementId，避免冲突</li>
 *     <li>支持批量插入与主键自动回填</li>
 *     <li>通过 {@link SqlSession} 直接执行 SQL</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2025-09-16
 */
public class DynamicSqlExecutor {

    private static final Logger log = LoggerFactory.getLogger(DynamicSqlExecutor.class);

    /**
     * 动态 Mapper 的命名空间
     */
    private static final String NAMESPACE = "local.ateng.java.injector.DynamicMapper";

    private final SqlSessionFactory sqlSessionFactory;

    public DynamicSqlExecutor(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /* ==================== SELECT ==================== */

    /**
     * 简单 SELECT
     *
     * @param sql        SQL 语句，必须以 SELECT 开头
     * @param resultType 结果类型（VO 或 Map）
     * @param param      参数对象，可以为 null
     * @param <T>        结果泛型
     * @return 查询结果集合
     */
    public <T> List<T> select(String sql, Class<T> resultType, Object param) {
        return execute(sql, SqlCommandType.SELECT, resultType, param, false);
    }


    /**
     * SELECT 支持 MP Wrapper 条件
     *
     * @param sql        SQL 语句
     * @param resultType 返回类型
     * @param wrapper    MP Wrapper 条件
     */
    public <T> List<T> select(String sql, Class<T> resultType, Wrapper<?> wrapper) {
        // 将 Wrapper 放入 paramMap
        Map<String, Object> paramMap = Collections.singletonMap(Constants.WRAPPER, wrapper);
        // 拼接 wrapper 条件到 SQL
        String finalSql = sql;
        if (wrapper != null && wrapper.getCustomSqlSegment() != null && !wrapper.getCustomSqlSegment().isEmpty()) {
            finalSql = sql + " " + wrapper.getCustomSqlSegment();
        }
        return execute(finalSql, SqlCommandType.SELECT, resultType, paramMap, false);
    }

    /**
     * SELECT 支持分页 IPage
     *
     * @param sql        SQL 语句
     * @param resultType 返回类型
     * @param page       分页对象
     * @param wrapper    MyBatis Plus Wrapper 对象，用于拼接自定义条件，必须为 @Param("ew")
     *
     */
    public <T> IPage<T> selectPage(String sql, Class<T> resultType, IPage<T> page, Wrapper<?> wrapper) {
        return selectPage(sql, resultType, page, wrapper, null);
    }

    /**
     * 动态分页查询，支持 Wrapper、自定义查询条件（query map）和复杂 SQL（CTE + 动态标签）。
     *
     * @param sql        原始 SQL，可包含 <if>/<foreach>/<where>/<set> 等动态标签
     * @param resultType 返回结果类型（VO 或 JSONObject）
     * @param page       分页对象 IPage（例如 Page<T>），可为 null 表示不分页
     * @param wrapper    MyBatis Plus Wrapper 对象，用于拼接自定义条件，必须为 @Param("ew")
     * @param query      自定义查询条件 Map
     * @param <T>        泛型类型
     * @return 分页结果 IPage<T> 或 List<T>（如果 page 为 null，则返回 List）
     */
    public <T> IPage<T> selectPage(String sql,
                                   Class<T> resultType,
                                   IPage<T> page,
                                   Wrapper<?> wrapper,
                                   Map<String, Object> query) {

        Map<String, Object> param = new HashMap<>();
        param.put(Constants.WRAPPER, wrapper);
        param.put("query", query);
        param.put("page", page);

        // 自动拼接 Wrapper 条件
        String finalSql = sql;
        if (wrapper != null && wrapper.getCustomSqlSegment() != null && !wrapper.getCustomSqlSegment().isEmpty()) {
            finalSql = sql + " " + wrapper.getCustomSqlSegment();
        }

        // 动态标签处理（<if>/<foreach>等）和 XML 特殊字符转义
        boolean containsDynamicTag = containsDynamicTag(finalSql);
        String scriptSql = containsDynamicTag ? "<script>" + escapeXmlForScript(finalSql) + "</script>" : finalSql;

        String msId = registerMappedStatement(scriptSql, SqlCommandType.SELECT, resultType, false, param);

        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            List<T> records = sqlSession.selectList(msId, param);
            page.setRecords(records);
            return page;
        } catch (Exception e) {
            log.error("执行动态 SELECT 失败", e);
            throw new DynamicSqlException("执行动态 SELECT 失败", e);
        }
    }

    /* ==================== INSERT ==================== */

    /**
     * 单条插入，支持主键回写
     *
     * @param sql   SQL 语句
     * @param param 实体对象
     * @return 影响行数
     */
    public int insert(String sql, Object param) {
        List<Integer> result = execute(sql, SqlCommandType.INSERT, int.class, param, true);
        return result.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 批量插入（逐条执行，支持主键回写）
     *
     * @param sql  SQL 语句
     * @param list 实体对象集合
     * @return 总影响行数
     */
    public int insertBatch(String sql, List<?> list) {
        List<Integer> result = execute(sql, SqlCommandType.INSERT, int.class, list, true);
        return result.stream().mapToInt(Integer::intValue).sum();
    }

    /* ==================== UPDATE ==================== */

    /**
     * 单条更新
     *
     * @param sql   SQL 语句
     * @param param 参数对象
     * @return 影响行数
     */
    public int update(String sql, Object param) {
        List<Integer> result = execute(sql, SqlCommandType.UPDATE, int.class, param, false);
        return result.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 批量更新（逐条执行）
     *
     * @param sql  SQL 语句
     * @param list 参数集合
     * @return 总影响行数
     */
    public int updateBatch(String sql, List<?> list) {
        List<Integer> result = execute(sql, SqlCommandType.UPDATE, int.class, list, false);
        return result.stream().mapToInt(Integer::intValue).sum();
    }

    /* ==================== DELETE ==================== */

    /**
     * 单条删除
     *
     * @param sql   SQL 语句
     * @param param 参数对象
     * @return 影响行数
     */
    public int delete(String sql, Object param) {
        List<Integer> result = execute(sql, SqlCommandType.DELETE, int.class, param, false);
        return result.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 批量删除（逐条执行）
     *
     * @param sql  SQL 语句
     * @param list 参数集合
     * @return 总影响行数
     */
    public int deleteBatch(String sql, List<?> list) {
        List<Integer> result = execute(sql, SqlCommandType.DELETE, int.class, list, false);
        return result.stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * 动态执行 SQL（支持 SELECT / INSERT / UPDATE / DELETE）
     * <p>
     * - SELECT：返回查询结果列表
     * - INSERT / UPDATE / DELETE：
     * 如果参数是单对象，返回单条受影响行数的 List
     * 如果参数是 List，根据 useBatchSql 判断：
     * 1. useBatchSql = false → 逐条执行（支持主键回写）
     * 2. useBatchSql = true  → foreach 批量执行（高性能，不支持主键回写）
     *
     * @param sql             动态 SQL
     * @param commandType     SQL 类型
     * @param resultType      结果映射类型
     * @param param           参数对象（单对象或 List）
     * @param enableKeyReturn 是否开启主键回写（仅对 INSERT 生效）
     * @param <T>             返回结果类型
     * @return 查询结果或受影响行数列表
     */
    private <T> List<T> execute(String sql,
                                SqlCommandType commandType,
                                Class<T> resultType,
                                Object param,
                                boolean enableKeyReturn) {

        String msId = registerMappedStatement(sql, commandType, resultType, enableKeyReturn, param);

        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            switch (commandType) {
                case SELECT:
                    return sqlSession.selectList(msId, param);

                case INSERT:
                    return (List<T>) doExecuteBatch(sqlSession, msId, param, SqlCommandType.INSERT);

                case UPDATE:
                    return (List<T>) doExecuteBatch(sqlSession, msId, param, SqlCommandType.UPDATE);

                case DELETE:
                    return (List<T>) doExecuteBatch(sqlSession, msId, param, SqlCommandType.DELETE);

                default:
                    throw new DynamicSqlException("不支持的 SQL 类型: " + commandType);
            }
        } catch (Exception e) {
            log.error("执行动态 SQL 失败，类型 [{}]，SQL [{}]", commandType, sql, e);
            throw new DynamicSqlException("执行动态 SQL 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 INSERT / UPDATE / DELETE，支持单条或批量
     *
     * @param sqlSession  MyBatis SqlSession
     * @param msId        MappedStatement ID
     * @param param       参数对象或 List
     * @param commandType SQL 类型
     * @return 每条操作的受影响行数列表
     */
    private List<Integer> doExecuteBatch(SqlSession sqlSession,
                                         String msId,
                                         Object param,
                                         SqlCommandType commandType) {
        // 单对象直接执行
        if (!(param instanceof List)) {
            return Collections.singletonList(executeOne(sqlSession, msId, param, commandType));
        }

        List<?> list = (List<?>) param;

        // 默认逐条执行模式
        List<Integer> results = new ArrayList<>();
        for (Object obj : list) {
            results.add(executeOne(sqlSession, msId, obj, commandType));
        }
        return results;
    }

    /**
     * 执行单条 INSERT / UPDATE / DELETE
     */
    private int executeOne(SqlSession sqlSession,
                           String msId,
                           Object param,
                           SqlCommandType commandType) {
        switch (commandType) {
            case INSERT:
                return sqlSession.insert(msId, param);
            case UPDATE:
                return sqlSession.update(msId, param);
            case DELETE:
                return sqlSession.delete(msId, param);
            default:
                throw new DynamicSqlException("不支持的单条执行 SQL 类型: " + commandType);
        }
    }

    /**
     * 注册并构建 MyBatis 的 MappedStatement
     * <p>
     * 核心流程：
     * 1. 对输入的 SQL 进行格式化（去除注释、压缩为单行）
     * 2. 调用 validateSql 进行基础校验，防止非法 SQL
     * 3. 判断是否包含动态 SQL 标签（如 <if>、<foreach>）
     * 4. 根据动态标签的存在与否决定是否包装 <script>
     * 5. 创建 SqlSource，并封装为 MappedStatement
     * 6. 针对不同的 SQL 类型（SELECT / INSERT）进行结果映射或主键回写配置
     *
     * @param sql             原始 SQL 语句
     * @param commandType     SQL 命令类型（SELECT、INSERT、UPDATE、DELETE）
     * @param resultType      查询结果映射的 Java 类型（仅在 SELECT 时生效）
     * @param enableKeyReturn 是否启用自动主键回写（仅在 INSERT 时生效）
     * @param param           SQL 参数对象（可为 Map 或实体类）
     * @param <T>             返回结果类型
     * @return MappedStatement 的唯一 ID
     */
    private <T> String registerMappedStatement(String sql,
                                               SqlCommandType commandType,
                                               Class<T> resultType,
                                               boolean enableKeyReturn,
                                               Object param) {

        // 1. 格式化 SQL，去除注释并压缩为单行
        String formattedSql = formatSql(sql);

        // 2. 调用 SQL 校验方法，确保 SQL 符合当前 commandType 的约束
        validateSql(formattedSql, commandType);

        Configuration configuration = sqlSessionFactory.getConfiguration();
        String msId = buildStatementId(sql, commandType, resultType);

        // 3. 如果当前 SQL 已经注册过，则直接复用，避免重复注册
        if (configuration.hasStatement(msId)) {
            return msId;
        }

        // 4. 判断是否包含动态标签（如 <if>、<foreach> 等）
        boolean containsDynamicTag = containsDynamicTag(formattedSql);

        // 5. 构造最终 SQL，如果包含动态标签则必须包裹 <script>
        String scriptSql;
        if (containsDynamicTag) {
            scriptSql = "<script>" + escapeXmlForScript(formattedSql) + "</script>";
        } else {
            scriptSql = formattedSql;
        }

        // 6. 基于最终 SQL 构建 SqlSource
        SqlSource sqlSource = configuration.getDefaultScriptingLanguageInstance()
                .createSqlSource(configuration, scriptSql, param == null ? Object.class : param.getClass());

        // 7. 构建 MappedStatement
        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, msId, sqlSource, commandType);

        // 8. 针对不同的 SQL 类型做额外处理
        if (commandType == SqlCommandType.SELECT) {
            // SELECT：需要配置结果映射
            String resultMapId = msId + "-Inline";
            ResultMap resultMap = new ResultMap.Builder(configuration, resultMapId, resultType, new ArrayList<>()).build();
            builder.resultMaps(Collections.singletonList(resultMap));
        } else if (commandType == SqlCommandType.INSERT) {
            // INSERT：可选的自动主键回写配置
            configureKeyGenerator(builder, param, enableKeyReturn);
        }

        // 9. 将构建完成的 MappedStatement 注册到 MyBatis Configuration
        configuration.addMappedStatement(builder.build());
        return msId;
    }

    /**
     * 配置主键回填
     *
     * @param builder         MappedStatement 构建器
     * @param param           参数对象
     * @param enableKeyReturn 是否启用主键回填
     */
    private void configureKeyGenerator(MappedStatement.Builder builder, Object param, boolean enableKeyReturn) {
        if (!enableKeyReturn || param == null) {
            builder.keyGenerator(NoKeyGenerator.INSTANCE);
            return;
        }

        Class<?> clazz = (param instanceof List && !((List<?>) param).isEmpty())
                ? ((List<?>) param).get(0).getClass()
                : param.getClass();

        Field keyField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(TableId.class))
                .findFirst()
                .orElse(null);

        if (keyField != null) {
            TableId tableId = keyField.getAnnotation(TableId.class);
            if (tableId.type() == IdType.AUTO) {
                builder.keyGenerator(Jdbc3KeyGenerator.INSTANCE);
                builder.keyProperty(keyField.getName());
                builder.keyColumn(tableId.value());
                return;
            }
        }

        builder.keyGenerator(NoKeyGenerator.INSTANCE);
    }

    /**
     * 生成唯一的 statementId
     *
     * @param sql         SQL 语句
     * @param commandType SQL 类型
     * @param resultType  返回类型
     * @return 唯一 statementId
     */
    private String buildStatementId(String sql, SqlCommandType commandType, Class<?> resultType) {
        int hashCode = sql.hashCode();
        return NAMESPACE + "." + commandType.name() + "_" + resultType.getSimpleName() + "_" + hashCode;
    }

    /**
     * 校验 SQL 合法性
     *
     * @param sql  SQL 语句
     * @param type SQL 类型
     */
    private void validateSql(String sql, SqlCommandType type) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new DynamicSqlException("动态 SQL 不能为空");
        }

        // 去掉开头注释和空白，去掉 /* */ 和 -- 注释
        String normalized = sql.trim()
                .replaceAll("(?s)^(/\\*.*?\\*/|--.*?\\n)*", "")
                .toLowerCase(Locale.ENGLISH);

        switch (type) {
            case SELECT:
                if (!normalized.matches("^(with\\s+.*?select|select|\\().*")) {
                    throw new DynamicSqlException("SELECT SQL 必须以 SELECT、WITH 或括号开头");
                }
                break;
            case INSERT:
                if (!normalized.startsWith("insert")) {
                    throw new DynamicSqlException("INSERT SQL 必须以 INSERT 开头");
                }
                break;
            case UPDATE:
                if (!normalized.startsWith("update")) {
                    throw new DynamicSqlException("UPDATE SQL 必须以 UPDATE 开头");
                }
                break;
            case DELETE:
                if (!normalized.startsWith("delete")) {
                    throw new DynamicSqlException("DELETE SQL 必须以 DELETE 开头");
                }
                break;
            default:
                throw new DynamicSqlException("未知 SQL 类型: " + type);
        }
    }

    /**
     * 判断 SQL 是否包含动态标签
     */
    private boolean containsDynamicTag(String sql) {
        if (sql == null) {
            return false;
        }
        // 扩展支持所有常用 MyBatis 标签
        Pattern pattern = Pattern.compile("<\\s*(if|foreach|where|set|choose|trim|when|otherwise|bind).*?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        return pattern.matcher(sql).find();
    }

    /**
     * 转义 XML 中特殊字符，但保留 MyBatis 动态标签
     */
    private String escapeXmlForScript(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        // 动态标签列表
        String[] tags = {"if", "foreach", "where", "set", "choose", "trim", "when", "otherwise", "bind"};
        Map<String, String> placeholderMap = new HashMap<>();

        // 使用正则暂存所有动态标签（支持嵌套）
        String tagPattern = "<\\s*(?i)(" + String.join("|", tags) + ")\\b.*?>.*?</\\s*\\1\\s*>";
        Pattern pattern = Pattern.compile(tagPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        int index = 0;
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String tag = matcher.group();
            String key = "%%DYNAMIC_TAG_" + UUID.randomUUID().toString() + "%%";
            placeholderMap.put(key, tag);
            matcher.appendReplacement(sb, key);
        }
        matcher.appendTail(sb);

        // 转义剩余 SQL 特殊字符
        String escaped = sb.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        // 恢复动态标签
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            escaped = escaped.replace(entry.getKey(), entry.getValue());
        }

        return escaped;
    }

    /**
     * 格式化 SQL，删除注释并压缩为一行
     * - 移除单行注释 -- …
     * - 移除块注释 /* … * /
     * - 压缩多余空格
     * - 保留动态标签
     */
    private String formatSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        // 1. 删除块注释 /* ... */
        String noBlockComments = sql.replaceAll("/\\*.*?\\*/", " ");

        // 2. 删除单行注释 -- ...
        String noLineComments = Arrays.stream(noBlockComments.split("\\r?\\n"))
                .map(line -> {
                    int index = line.indexOf("--");
                    if (index >= 0) {
                        return line.substring(0, index);
                    } else {
                        return line;
                    }
                })
                .collect(Collectors.joining(" "));

        // 3. 压缩多余空格
        String compressed = noLineComments.replaceAll("\\s+", " ");

        // 4. 保证动态标签前后有空格，便于 containsDynamicTag 正确识别
        String tagsPattern = "(?i)<\\s*(if|foreach|where|set|choose|trim|when|otherwise|bind)\\b";
        compressed = compressed.replaceAll(tagsPattern, " $0");

        return compressed.trim();
    }


}
