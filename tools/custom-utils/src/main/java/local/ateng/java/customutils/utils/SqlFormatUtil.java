package local.ateng.java.customutils.utils;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQL 格式化工具类。
 * 提供常用的 SQL 美化、参数替换、方言切换、自定义配置与安全兜底能力。
 * 格式化失败时默认返回原 SQL，并记录日志，避免影响业务流程。
 *
 * @author Ateng
 * @since 2026-04-16
 */
public final class SqlFormatUtil {

    /**
     * 日志对象。
     */
    private static final Logger log = LoggerFactory.getLogger(SqlFormatUtil.class);

    /**
     * 默认缩进。
     */
    private static final String DEFAULT_INDENT = "    ";

    /**
     * 禁止实例化工具类。
     */
    private SqlFormatUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 对 SQL 进行默认格式化。
     *
     * @param sql 待格式化 SQL
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                return SqlFormatter.format(sql);
            }
        });
    }

    /**
     * 对 SQL 进行格式化，并指定缩进。
     *
     * @param sql    待格式化 SQL
     * @param indent 缩进字符串，传空时使用默认缩进
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final String indent) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                return SqlFormatter.format(sql, resolveIndent(indent));
            }
        });
    }

    /**
     * 对 SQL 进行格式化，并使用自定义 FormatConfig。
     *
     * @param sql    待格式化 SQL
     * @param config 格式化配置；为空时使用默认格式化
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final FormatConfig config) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                if (config == null) {
                    return SqlFormatter.format(sql);
                }
                return SqlFormatter.format(sql, config);
            }
        });
    }

    /**
     * 对 SQL 进行格式化，并替换 List 位置参数。
     *
     * @param sql    待格式化 SQL
     * @param params 位置参数集合
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final List<?> params) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                if (params == null || params.isEmpty()) {
                    return SqlFormatter.format(sql);
                }
                return SqlFormatter.format(sql, params);
            }
        });
    }

    /**
     * 对 SQL 进行格式化，并指定缩进，同时替换 List 位置参数。
     *
     * @param sql    待格式化 SQL
     * @param indent 缩进字符串，传空时使用默认缩进
     * @param params 位置参数集合
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final String indent, final List<?> params) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                if (params == null || params.isEmpty()) {
                    return SqlFormatter.format(sql, resolveIndent(indent));
                }
                return SqlFormatter.format(sql, resolveIndent(indent), params);
            }
        });
    }

    /**
     * 对 SQL 进行格式化，并替换 Map 命名参数。
     *
     * @param sql    待格式化 SQL
     * @param params 命名参数集合
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final Map<String, ?> params) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                if (params == null || params.isEmpty()) {
                    return SqlFormatter.format(sql);
                }
                return SqlFormatter.format(sql, params);
            }
        });
    }

    /**
     * 对 SQL 进行格式化，并指定缩进，同时替换 Map 命名参数。
     *
     * @param sql    待格式化 SQL
     * @param indent 缩进字符串，传空时使用默认缩进
     * @param params 命名参数集合
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final String indent, final Map<String, ?> params) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                if (params == null || params.isEmpty()) {
                    return SqlFormatter.format(sql, resolveIndent(indent));
                }
                return SqlFormatter.format(sql, resolveIndent(indent), params);
            }
        });
    }

    /**
     * 按指定方言格式化 SQL。
     *
     * @param sql     待格式化 SQL
     * @param dialect 方言
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final Dialect dialect) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                return SqlFormatter.of(resolveDialect(dialect)).format(sql);
            }
        });
    }

    /**
     * 按指定方言和缩进格式化 SQL。
     *
     * @param sql     待格式化 SQL
     * @param dialect 方言
     * @param indent  缩进字符串，传空时使用默认缩进
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final Dialect dialect, final String indent) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                FormatConfig config = FormatConfig.builder()
                        .indent(resolveIndent(indent))
                        .build();

                return SqlFormatter
                        .of(resolveDialect(dialect))
                        .format(sql, config);
            }
        });
    }

    /**
     * 按字符串方言和缩进格式化 SQL。
     *
     * @param sql         待格式化 SQL
     * @param dialectName 方言名称，例如 mysql、postgresql、plsql、sparksql、standard
     * @param indent      缩进字符串，传空时使用默认缩进
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final String dialectName, final String indent) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                FormatConfig config = FormatConfig.builder()
                        .indent(resolveIndent(indent))
                        .build();

                return SqlFormatter
                        .of(resolveDialect(dialectName))
                        .format(sql, config);
            }
        });
    }

    /**
     * 按指定方言并使用 FormatConfig 自定义格式化。
     *
     * @param sql     待格式化 SQL
     * @param dialect 方言
     * @param config  格式化配置
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final Dialect dialect, final FormatConfig config) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                Dialect actualDialect = resolveDialect(dialect);
                if (config == null) {
                    return SqlFormatter.of(actualDialect).format(sql);
                }
                return SqlFormatter.of(actualDialect).format(sql, config);
            }
        });
    }

    /**
     * 按字符串方言并通过 FormatConfig 自定义格式化。
     *
     * @param sql         待格式化 SQL
     * @param dialectName 方言名称
     * @param config      格式化配置
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    public static String format(String sql, final String dialectName, final FormatConfig config) {
        return execute(sql, new SqlSupplier() {
            @Override
            public String get() {
                Dialect actualDialect = resolveDialect(dialectName);
                if (config == null) {
                    return SqlFormatter.of(actualDialect).format(sql);
                }
                return SqlFormatter.of(actualDialect).format(sql, config);
            }
        });
    }

    /**
     * 判断 SQL 是否为空白。
     *
     * @param sql SQL 字符串
     * @return true 表示为空白
     */
    public static boolean isBlank(String sql) {
        return sql == null || sql.trim().isEmpty();
    }

    /**
     * 将 SQL 规范化为去除首尾空白后的字符串。
     *
     * @param sql SQL 字符串
     * @return 规范化后的字符串；传入 null 时返回 null
     */
    public static String trim(String sql) {
        return sql == null ? null : sql.trim();
    }

    /**
     * 统一执行格式化逻辑。
     *
     * @param sql      原始 SQL
     * @param supplier 格式化执行器
     * @return 格式化后的 SQL；失败时返回原 SQL
     */
    private static String execute(String sql, SqlSupplier supplier) {
        if (sql == null) {
            return null;
        }
        if (sql.trim().isEmpty()) {
            return sql;
        }
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("SQL 格式化失败，已返回原 SQL: {}", e.getMessage(), e);
            return sql;
        }
    }

    /**
     * 解析缩进字符串。
     *
     * @param indent 缩进
     * @return 解析后的缩进字符串
     */
    private static String resolveIndent(String indent) {
        return indent == null || indent.length() == 0 ? DEFAULT_INDENT : indent;
    }

    /**
     * 解析方言对象。
     *
     * @param dialect 方言对象
     * @return 可用方言；为空时返回标准方言
     */
    private static Dialect resolveDialect(Dialect dialect) {
        return dialect == null ? Dialect.StandardSql : dialect;
    }

    /**
     * 解析字符串方言。
     *
     * @param dialectName 方言名称
     * @return 可用方言；无法识别时回退为标准方言
     */
    private static Dialect resolveDialect(String dialectName) {
        if (dialectName == null || dialectName.trim().isEmpty()) {
            return Dialect.StandardSql;
        }

        String normalized = dialectName.trim().toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");

        if ("mysql".equals(normalized) || "mariadb".equals(normalized)) {
            return Dialect.MySql;
        }
        if ("postgresql".equals(normalized) || "postgres".equals(normalized) || "pgsql".equals(normalized)) {
            return Dialect.PostgreSql;
        }
        if ("plsql".equals(normalized) || "pl/sql".equals(normalized) || "oracle".equals(normalized)) {
            return Dialect.PlSql;
        }
        if ("sparksql".equals(normalized) || "spark".equals(normalized)) {
            return Dialect.SparkSql;
        }
        if ("standard".equals(normalized) || "standardsql".equals(normalized) || "ansi".equals(normalized)) {
            return Dialect.StandardSql;
        }

        try {
            return Dialect.valueOf(dialectName.trim());
        } catch (Exception e) {
            log.warn("未识别的 SQL 方言，已回退为 StandardSql: {}", dialectName);
            return Dialect.StandardSql;
        }
    }

    /**
     * SQL 格式化执行器。
     */
    @FunctionalInterface
    private interface SqlSupplier {

        /**
         * 执行格式化。
         *
         * @return 格式化结果
         */
        String get();
    }
}