package local.ateng.java.mybatisjdk8.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SqlAuditInnerInterceptor（改进版）
 *
 * <p>目的：
 * <ul>
 *     <li>在 MyBatis-Plus 的 InnerInterceptor 层记录最终要执行的 SQL 字符串（将 ? 替换为真实参数值）</li>
 *     <li>在替换参数后使用 JSqlParser（CCJSqlParserUtil.parse）再次解析/规范化 SQL，以接近 JDBC 层/数据库实际执行的 SQL 表现</li>
 * </ul>
 *
 * <p>特性与注意事项：
 * <ol>
 *     <li>支持常用参数类型的格式化：Number/Boolean/String/Date/Java8Time/Enum/UUID/Collection/Array/byte[] 等</li>
 *     <li>对于二进制/大对象会避免直接把全部内容打印，防止日志爆炸</li>
 *     <li>JSqlParser 解析失败时会回退到拼接后的 SQL（不会抛异常）</li>
 *     <li>不修改 SQL 的执行流程，仅用于记录/打印（非阻断器）</li>
 *     <li>生产环境请注意：最终 SQL 中包含真实参数，可能包含敏感数据，建议按需开启或做脱敏</li>
 * </ol>
 *
 * <p>依赖（示例）：
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.github.jsqlparser&lt;/groupId&gt;
 *   &lt;artifactId&gt;jsqlparser&lt;/artifactId&gt;
 *   &lt;version&gt;（请按项目实际版本填写，例如 4.x 系列）&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * <p>注册示例（建议把会修改 SQL 的 InnerInterceptor 先添加，审计拦截器放最后）：
 * <pre>
 * @Bean
 * public MybatisPlusInterceptor mybatisPlusInterceptor(SqlAuditInnerInterceptor sqlAuditInnerInterceptor) {
 *     MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
 *     interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
 *     interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
 *     interceptor.addInnerInterceptor(sqlAuditInnerInterceptor); // 放最后
 *     return interceptor;
 * }
 * </pre>
 *
 * @author 孔余
 * @since 2025-09-16
 */
public class SqlPrintInnerInterceptor implements InnerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SqlPrintInnerInterceptor.class);

    /**
     * 是否启用 JSqlParser 解析（解析失败会回退）
     * 在某些生产环境/极端 SQL 下可以关闭以避免兼容性问题。
     */
    private boolean parseWithJsqlParser = true;

    /**
     * 构造函数（默认开启 JSqlParser）
     */
    public SqlPrintInnerInterceptor() {
        this(true);
    }

    public SqlPrintInnerInterceptor(boolean parseWithJsqlParser) {
        this.parseWithJsqlParser = parseWithJsqlParser;
    }

    public void setParseWithJsqlParser(boolean parseWithJsqlParser) {
        this.parseWithJsqlParser = parseWithJsqlParser;
    }

    // ======================================================
    // 拦截点：查询与更新（MyBatis-Plus 常用的两个钩子）
    // ======================================================

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        try {
            String finalSql = buildFinalSql(ms, boundSql, parameter);
            String displaySql = finalSql.length() > 200 ? finalSql.substring(0, 200) + "..." : finalSql;
            logger.info("[SQL] id={} | sql={}", ms.getId(), displaySql);
        } catch (Exception e) {
            // 审计日志不能影响正常执行，捕获所有异常并用 warn 记录
            logger.warn("[SQL] 构建/打印最终 SQL 失败，id={}，原因：{}", ms.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        try {
            BoundSql boundSql = ms.getBoundSql(parameter);
            String finalSql = buildFinalSql(ms, boundSql, parameter);
            String displaySql = finalSql.length() > 200 ? finalSql.substring(0, 200) + "..." : finalSql;
            logger.info("[SQL] id={} | sql={}", ms.getId(), displaySql);
        } catch (Exception e) {
            logger.warn("[SQL] 构建/打印最终 SQL 失败，id={}，原因：{}", ms.getId(), e.getMessage(), e);
        }
    }

    // ======================================================
    // 核心：把 BoundSql + 参数 -> 最终 SQL 字符串（并通过 JSqlParser 解析）
    // ======================================================
    private String buildFinalSql(MappedStatement ms, BoundSql boundSql, Object parameter) {
        Configuration configuration = ms.getConfiguration();
        String sql = boundSql.getSql();
        if (sql == null) {
            return "";
        }
        // 把多空白（换行、制表等）压缩成单个空格，便于输出
        String normalizedSql = sql.replaceAll("\\s+", " ").trim();

        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings == null || parameterMappings.isEmpty()) {
            // 无参数，直接尝试解析并返回
            return tryParseSql(normalizedSql);
        }

        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        MetaObject metaObject = parameter == null ? null : configuration.newMetaObject(parameter);

        StringBuilder sb = new StringBuilder(normalizedSql);

        for (ParameterMapping pm : parameterMappings) {
            // 跳过 OUT 模式参数
            if (pm.getMode() == ParameterMode.OUT) {
                continue;
            }

            String propName = pm.getProperty();
            Object value;

            // 1) 先尝试 AdditionalParameter（foreach 等会生成）
            if (boundSql.hasAdditionalParameter(propName)) {
                value = boundSql.getAdditionalParameter(propName);
            } else if (parameter == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameter.getClass())) {
                // 传入的是单个基础类型参数（如 mapper.method(1)）
                value = parameter;
            } else {
                // 传入的是对象（POJO）或 Map 等，使用 MetaObject 获取对应属性
                value = metaObject == null ? null : metaObject.getValue(propName);
            }

            String formatted = formatParameter(value);

            // 将第一个 '?' 替换为 formatted（顺序替换）
            int idx = sb.indexOf("?");
            if (idx == -1) {
                // 兜底：没有占位符（极少见），则在末尾追加
                sb.append(" ").append(formatted);
            } else {
                sb.replace(idx, idx + 1, formatted);
            }
        }

        String replacedSql = sb.toString();
        return tryParseSql(replacedSql);
    }

    // ======================================================
    // 参数格式化：把 Java 对象格式化为 SQL 可读的字面量（或合适的占位说明）
    // ======================================================
    private String formatParameter(Object value) {
        if (value == null) {
            return "NULL";
        }

        // 数字类型（Integer, Long, Double, BigDecimal, BigInteger 等）—— 不加引号
        if (value instanceof Number) {
            // BigDecimal 保留其 toString 表现（避免科学计数法）
            return value.toString();
        }

        // 布尔类型：使用 1 / 0（兼容多数 MySQL 场景）；如果你更喜欢 TRUE/FALSE，可改为 "TRUE"/"FALSE"
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }

        // 字符串类型：需要 SQL 单引号并转义单引号
        if (value instanceof String) {
            return "'" + escapeSql((String) value) + "'";
        }

        // Character -> 当作单字符字符串
        if (value instanceof Character) {
            return "'" + escapeSql(value.toString()) + "'";
        }

        // java.util.Date 及子类（包括 Timestamp） -> 'yyyy-MM-dd HH:mm:ss'
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "'" + sdf.format((Date) value) + "'";
        }

        // Java 8 时间 API
        if (value instanceof LocalDateTime) {
            return "'" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format((LocalDateTime) value) + "'";
        }
        if (value instanceof LocalDate) {
            return "'" + DateTimeFormatter.ofPattern("yyyy-MM-dd").format((LocalDate) value) + "'";
        }
        if (value instanceof LocalTime) {
            return "'" + DateTimeFormatter.ofPattern("HH:mm:ss").format((LocalTime) value) + "'";
        }
        if (value instanceof OffsetDateTime) {
            return "'" + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format((OffsetDateTime) value) + "'";
        }
        if (value instanceof ZonedDateTime) {
            return "'" + DateTimeFormatter.ISO_ZONED_DATE_TIME.format((ZonedDateTime) value) + "'";
        }
        if (value instanceof Instant) {
            return "'" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format((Instant) value) + "'";
        }

        // Enum -> 使用 name()（并加引号）
        if (value instanceof Enum) {
            return "'" + escapeSql(((Enum<?>) value).name()) + "'";
        }

        // UUID -> 当作字符串
        if (value instanceof UUID) {
            return "'" + value.toString() + "'";
        }

        // 集合 -> (a, b, c) 样式
        if (value instanceof Collection) {
            Collection<?> coll = (Collection<?>) value;
            if (coll.isEmpty()) {
                return "(NULL)"; // 防止生成 IN ()
            }
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (Object o : coll) {
                sj.add(formatParameter(o));
            }
            return sj.toString();
        }

        // 数组 -> (a, b, c)
        if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            if (len == 0) {
                return "(NULL)";
            }
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (int i = 0; i < len; i++) {
                Object o = java.lang.reflect.Array.get(value, i);
                sj.add(formatParameter(o));
            }
            return sj.toString();
        }

        // 二进制 -> 避免日志爆炸，打印长度提示
        if (value instanceof byte[]) {
            return "BINARY[" + ((byte[]) value).length + "]";
        }

        // Map（常见于 paramMap） -> 尽量友好展示（但不会直接嵌入 SQL）
        if (value instanceof Map) {
            // Map 通常不是直接出现在单个占位符中（MyBatis 会展开），但兜底展示
            Map<?, ?> map = (Map<?, ?>) value;
            return "'" + escapeSql(map.toString()) + "'";
        }

        // 其他类型（使用 toString，并加引号）
        return "'" + escapeSql(String.valueOf(value)) + "'";
    }

    // ======================================================
    // 使用 JSqlParser 解析/规范化 SQL；解析失败则回退到原始 SQL（不抛异常）
    // ======================================================
    private String tryParseSql(String sql) {
        if (!parseWithJsqlParser) {
            return sql;
        }
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            // Statement.toString() 会返回标准化后的 SQL（JSqlParser 的字符串表示）
            String parsed = stmt.toString();
            // 去除可能产生的多余空白（保持一致）
            return parsed.replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            // 解析失败（可能是方言或 JSqlParser 的限制），调试级别记录失败原因，并返回原 SQL
            logger.debug("[SQL] JSqlParser 解析 SQL 失败，使用原 SQL。原因：{}", e.getMessage());
            return sql;
        }
    }

    // ======================================================
    // 简单 SQL 字符串转义（主要转义单引号为两个单引号）
    // ======================================================
    private String escapeSql(String input) {
        if (input == null) {
            return null;
        }
        // 把单引号转义成 SQL 风格的两个单引号
        return input.replace("'", "''");
    }
}
