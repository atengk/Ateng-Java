package local.ateng.java.mybatisjdk8.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * MyBatis-Plus 自定义拦截器：打印最终可执行 SQL（含参数替换）。
 *
 * <p>该拦截器在 SQL 执行前将 {@link BoundSql} 中的参数替换到占位符中，
 * 输出完整可执行 SQL 日志，适用于 SQL 调试、慢查询分析、审计日志记录等场景。</p>
 *
 * <p>注意：
 * <ul>
 *     <li>仅用于日志打印，不影响 SQL 执行。</li>
 *     <li>对于复杂自定义 TypeHandler，输出为 {@code toString()} 结果。</li>
 * </ul>
 * </p>
 */
public class ExecutableSqlInterceptor implements InnerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ExecutableSqlInterceptor.class);

    // ==================== 查询拦截 ====================

    /**
     * 查询执行前判断是否继续执行 {@link #beforeQuery}。
     *
     * @param executor      MyBatis 执行器
     * @param ms            映射语句对象
     * @param parameter     方法入参
     * @param rowBounds     分页参数
     * @param resultHandler 查询结果处理器
     * @param boundSql      SQL 绑定对象
     * @return true 继续执行 beforeQuery，false 跳过
     * @throws SQLException SQL 异常
     */
    @Override
    public boolean willDoQuery(Executor executor, MappedStatement ms, Object parameter,
                               RowBounds rowBounds, ResultHandler resultHandler,
                               BoundSql boundSql) throws SQLException {
        return true; // 始终允许查询执行
    }

    /**
     * 查询执行前回调方法，用于打印最终可执行 SQL。
     *
     * @param executor      MyBatis 执行器
     * @param ms            映射语句对象
     * @param parameter     方法入参
     * @param rowBounds     分页参数
     * @param resultHandler 查询结果处理器
     * @param boundSql      SQL 绑定对象
     * @throws SQLException SQL 异常
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler,
                            BoundSql boundSql) throws SQLException {
        String sql = getExecutableSql(ms.getConfiguration(), boundSql);
        log.info("[MyBatis-Plus SQL][QUERY] {}", sql);
    }

    // ==================== 更新/插入/删除拦截 ====================

    /**
     * 更新执行前判断是否继续执行 {@link #beforeUpdate}。
     *
     * @param executor  MyBatis 执行器
     * @param ms        映射语句对象
     * @param parameter 方法入参
     * @return true 继续执行 beforeUpdate，false 跳过
     * @throws SQLException SQL 异常
     */
    @Override
    public boolean willDoUpdate(Executor executor, MappedStatement ms, Object parameter)
            throws SQLException {
        return true;
    }

    /**
     * 更新/插入/删除执行前回调方法，用于打印最终可执行 SQL。
     *
     * @param executor  MyBatis 执行器
     * @param ms        映射语句对象
     * @param parameter 方法入参
     * @throws SQLException SQL 异常
     */
    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter)
            throws SQLException {
        BoundSql boundSql = ms.getSqlSource().getBoundSql(parameter);
        String sql = getExecutableSql(ms.getConfiguration(), boundSql);
        log.info("[MyBatis-Plus SQL][{}] {}", ms.getSqlCommandType(), sql);
    }

    // ==================== StatementHandler 预处理 ====================

    /**
     * SQL 预处理阶段回调，可获取 StatementHandler 和 Connection。
     * <p>可用于统一拦截或修改 SQL，但生产环境一般仅用于打印。</p>
     *
     * @param sh                 StatementHandler 对象
     * @param connection         数据库连接
     * @param transactionTimeout 事务超时时间
     */
    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        // 预留扩展点
    }

    /**
     * 获取 BoundSql 前回调方法，可用于最后阶段修改 SQL。
     *
     * @param sh StatementHandler 对象
     */
    @Override
    public void beforeGetBoundSql(StatementHandler sh) {
        // 预留扩展点
    }

    // ==================== 核心方法：拼接最终 SQL ====================

    /**
     * 将 BoundSql 中的占位符替换为参数值，生成最终可执行 SQL。
     *
     * @param configuration MyBatis 配置对象
     * @param boundSql      SQL 绑定对象
     * @return 可执行 SQL 字符串
     */
    public static String getExecutableSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql();

        if (parameterMappings != null && !parameterMappings.isEmpty() && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            MetaObject metaObject = configuration.newMetaObject(parameterObject);

            for (ParameterMapping parameterMapping : parameterMappings) {
                if (parameterMapping.getMode() == ParameterMode.OUT) {
                    continue; // 跳过输出参数
                }

                String propertyName = parameterMapping.getProperty();
                Object value;

                if (boundSql.hasAdditionalParameter(propertyName)) {
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (metaObject.hasGetter(propertyName)) {
                    value = metaObject.getValue(propertyName);
                } else {
                    value = parameterObject;
                }

                String valueStr = getParameterValue(value, parameterMapping, typeHandlerRegistry);
                sql = sql.replaceFirst("\\?", valueStr.replace("$", "\\$"));
            }
        }

        return sql;
    }

    /**
     * 获取参数字符串表示，兼容枚举、日期、String、自定义 TypeHandler。
     *
     * @param value               参数值
     * @param parameterMapping    参数映射
     * @param typeHandlerRegistry TypeHandler 注册表
     * @return 可替换 SQL 的字符串
     */
    private static String getParameterValue(Object value, ParameterMapping parameterMapping,
                                            TypeHandlerRegistry typeHandlerRegistry) {
        if (value == null) {
            return "NULL";
        }

        Class<?> javaType = parameterMapping.getJavaType();
        TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(javaType);

        if (typeHandler != null && !(value instanceof String)) {
            return "'" + value.toString() + "'";
        }

        if (value instanceof String) {
            return "'" + value + "'";
        }
        if (value instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
                    DateFormat.DEFAULT, Locale.CHINA);
            return "'" + formatter.format((Date) value) + "'";
        }
        if (value instanceof Enum<?>) {
            return "'" + ((Enum<?>) value).name() + "'";
        }

        return value.toString();
    }

    // ==================== 扩展配置 ====================

    /**
     * 设置自定义属性，可通过配置文件注入，如日志开关。
     *
     * @param properties 属性集合
     */
    @Override
    public void setProperties(Properties properties) {
        // 可用于读取配置参数，例如 SQL 日志开关
    }
}

