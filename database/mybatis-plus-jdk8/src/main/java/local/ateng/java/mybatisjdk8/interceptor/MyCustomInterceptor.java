package local.ateng.java.mybatisjdk8.interceptor;

import cn.hutool.db.sql.SqlUtil;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * 自定义 MyBatis-Plus 内置拦截器示例，实现了 InnerInterceptor 接口的全部方法。
 * <p>
 * 该拦截器可以在 SQL 执行的各个阶段获取 SQL 信息，包括查询、更新、插入、删除等操作。
 * 适用于 SQL 日志打印、性能监控、动态 SQL 修改等场景。
 * </p>
 *
 * @author 孔余
 * @since 2025-08-22
 */
public class MyCustomInterceptor implements InnerInterceptor {

    /**
     * 查询执行前是否继续执行 beforeQuery。
     *
     * @param executor      执行器
     * @param ms            映射语句对象
     * @param parameter     方法入参
     * @param rowBounds     分页参数
     * @param resultHandler 查询结果处理器
     * @param boundSql      SQL 绑定对象
     * @return true 继续执行 beforeQuery，false 跳过
     * @throws SQLException SQL 异常
     */
    @Override
    public boolean willDoQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                               ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 可在这里根据需求动态决定是否执行查询
        return true;
    }

    /**
     * 查询执行前回调方法，可获取 BoundSql。
     *
     * @param executor      执行器
     * @param ms            映射语句对象
     * @param parameter     方法入参
     * @param rowBounds     分页参数
     * @param resultHandler 查询结果处理器
     * @param boundSql      SQL 绑定对象
     * @throws SQLException SQL 异常
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        String executableSql = getExecutableSql(ms.getConfiguration(), boundSql);
        System.out.println("[查询 SQL] " + executableSql);
    }

    /**
     * 更新执行前是否继续执行 beforeUpdate。
     *
     * @param executor  执行器
     * @param ms        映射语句对象
     * @param parameter 方法入参
     * @return true 继续执行 beforeUpdate，false 跳过
     * @throws SQLException SQL 异常
     */
    @Override
    public boolean willDoUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        return true;
    }

    /**
     * 更新执行前回调方法，可获取 BoundSql 或 MappedStatement 信息。
     *
     * @param executor  执行器
     * @param ms        映射语句对象
     * @param parameter 方法入参
     * @throws SQLException SQL 异常
     */
    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        BoundSql boundSql = ms.getSqlSource().getBoundSql(parameter);
        String executableSql = getExecutableSql(ms.getConfiguration(), boundSql);
        System.out.println("[更新/插入/删除 SQL] " + executableSql + "，类型: " + ms.getSqlCommandType());
    }

    /**
     * SQL 预处理阶段回调，可获取 StatementHandler 和 Connection。
     * <p>
     * 该方法可用于统一获取所有类型 SQL，或对 SQL 进行修改。
     * </p>
     *
     * @param sh                 StatementHandler
     * @param connection         数据库连接
     * @param transactionTimeout 事务超时时间
     */
    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        BoundSql boundSql = sh.getBoundSql();
        String sql = boundSql.getSql();
        System.out.println("[SQL 准备阶段] " + SqlUtil.formatSql(sql));
    }

    public static String getExecutableSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql();

        if (parameterMappings != null && !parameterMappings.isEmpty() && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            MetaObject metaObject = configuration.newMetaObject(parameterObject);

            for (ParameterMapping parameterMapping : parameterMappings) {
                String propertyName = parameterMapping.getProperty();
                Object value;
                if (metaObject.hasGetter(propertyName)) {
                    value = metaObject.getValue(propertyName);
                } else {
                    value = parameterObject;
                }

                String valueStr;
                if (value instanceof String) {
                    valueStr = "'" + value + "'";
                } else if (value == null) {
                    valueStr = "NULL";
                } else {
                    valueStr = value.toString();
                }

                // 替换第一个 ?，注意使用 regex escape
                sql = sql.replaceFirst("\\?", valueStr);
            }
        }
        return sql;
    }

    /**
     * 获取 BoundSql 前的回调，可用于最后阶段修改 SQL。
     *
     * @param sh StatementHandler
     */
    @Override
    public void beforeGetBoundSql(StatementHandler sh) {
        BoundSql boundSql = sh.getBoundSql();
        String sql = boundSql.getSql();
        System.out.println("[获取 BoundSql 前] " + sql);
    }

    /**
     * 设置自定义配置属性，可通过配置文件注入参数。
     *
     * @param properties 属性集合
     */
    @Override
    public void setProperties(Properties properties) {
        // 可读取配置参数，例如日志开关、SQL 修改规则等
    }

}