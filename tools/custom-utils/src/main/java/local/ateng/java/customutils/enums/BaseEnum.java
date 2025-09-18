package local.ateng.java.customutils.enums;

/**
 * 基础枚举接口（支持泛型）
 *
 * <p>
 * 用于统一定义项目中的枚举类型，避免重复编写 getter 方法。
 * 泛型参数：
 * <ul>
 *     <li>C - code 的类型（如 Integer、String、Long 等）</li>
 *     <li>N - name 的类型（通常是 String，也可以是其它类型）</li>
 * </ul>
 * <p>
 * 示例：
 * <pre>
 *     public enum StatusEnum implements BaseEnum<Integer, String> {
 *         OFFLINE(0, "离线"),
 *         ONLINE(1, "在线");
 *
 *         private final Integer code;
 *         private final String name;
 *
 *         StatusEnum(Integer code, String name) {
 *             this.code = code;
 *             this.name = name;
 *         }
 *
 *         &#64;Override
 *         public Integer getCode() {
 *             return code;
 *         }
 *
 *         &#64;Override
 *         public String getName() {
 *             return name;
 *         }
 *     }
 * </pre>
 *
 * @param <C> code 的类型（如 Integer、String、Long 等）
 * @param <N> name 的类型（如 String、I18nLabel 等）
 * @author 孔余
 * @since 2025-08-21
 */
public interface BaseEnum<C, N> {

    /**
     * 获取枚举的唯一编码
     *
     * @return 枚举的 code 值
     */
    C getCode();

    /**
     * 获取枚举的描述/名称
     *
     * @return 枚举的 name 值
     */
    N getName();

    /**
     * 根据 code 获取对应的枚举实例
     *
     * @param enumClass 枚举类
     * @param code      待匹配的枚举 code
     * @param <E>       枚举类型（需实现 BaseEnum）
     * @param <C>       code 类型
     * @param <N>       name 类型
     * @return 匹配到的枚举实例；若未找到，返回 {@code null}
     */
    static <E extends Enum<E> & BaseEnum<C, N>, C, N> E fromCode(Class<E> enumClass, C code) {
        if (enumClass == null || code == null) {
            return null;
        }
        for (E e : enumClass.getEnumConstants()) {
            if (code.equals(e.getCode())) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据 name 获取对应的枚举实例
     *
     * @param enumClass 枚举类
     * @param name      待匹配的枚举名称
     * @param <E>       枚举类型（需实现 BaseEnum）
     * @param <C>       code 类型
     * @param <N>       name 类型
     * @return 匹配到的枚举实例；若未找到，返回 {@code null}
     */
    static <E extends Enum<E> & BaseEnum<C, N>, C, N> E fromName(Class<E> enumClass, N name) {
        if (enumClass == null || name == null) {
            return null;
        }
        for (E e : enumClass.getEnumConstants()) {
            if (name.equals(e.getName())) {
                return e;
            }
        }
        return null;
    }
}
