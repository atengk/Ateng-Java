package local.ateng.java.mybatisjdk8.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 状态枚举（支持 MyBatis-Plus 与 Jackson 序列化、反序列化）。
 *
 * <p>主要功能：
 * <ul>
 *     <li>通过 {@link EnumValue} 注解，支持 MyBatis-Plus 将枚举存储到数据库时使用 {@code code} 字段。</li>
 *     <li>通过 {@link JsonValue} 注解，支持 Jackson 在序列化枚举时输出 {@code name} 字段。</li>
 *     <li>通过 {@link JsonCreator} 注解，支持 Jackson 在反序列化时根据 {@code code} 自动映射枚举。</li>
 * </ul>
 *
 * <p>适用场景：
 * <ul>
 *     <li>数据库中存储数值型状态码。</li>
 *     <li>接口返回时需要输出中文或自定义描述。</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2025-08-25
 */
public enum StatusEnumJackson {

    /**
     * 离线状态。
     */
    OFFLINE(0, "离线"),

    /**
     * 在线状态。
     */
    ONLINE(1, "在线");

    /**
     * 枚举对应的数据库存储值。
     *
     * <p>该值与数据库字段绑定，通常为数值型标识。</p>
     */
    @EnumValue
    private final Integer code;

    /**
     * 枚举的展示名称。
     *
     * <p>在 Jackson 中，若字段或方法标注 {@link JsonValue}，
     * 则该值在序列化时会作为枚举的 JSON 输出。</p>
     *
     * <p>示例：
     * <pre>
     *     StatusEnumJackson.ONLINE  -> "在线"
     *     StatusEnumJackson.OFFLINE -> "离线"
     * </pre>
     * </p>
     */
    @JsonValue
    private final String name;

    StatusEnumJackson(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 获取枚举对应的存储值。
     *
     * @return 数据库存储的数值型标识
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取枚举的展示名称。
     *
     * <p>在 Jackson 中，该方法通常不会直接影响序列化结果，
     * 因为 {@link JsonValue} 已经标注在 {@code name} 字段上。</p>
     *
     * @return 枚举展示名称（中文）
     */
    public String getName() {
        return this.name;
    }

    /**
     * 根据存储值反序列化为枚举。
     *
     * <p>配合 {@link JsonCreator} 使用，Jackson 在反序列化时会调用该方法。</p>
     *
     * <p>示例：
     * <pre>
     *     // JSON: {"status":1} -> ONLINE
     *     StatusEnumJackson.fromCode(1); // ONLINE
     *
     *     // JSON: {"status":0} -> OFFLINE
     *     StatusEnumJackson.fromCode(0); // OFFLINE
     *
     *     // JSON: {"status":99} -> null
     *     StatusEnumJackson.fromCode(99); // null
     * </pre>
     * </p>
     *
     * @param code 数值型标识，可能为 null 或不在定义范围
     * @return 对应的枚举常量；未匹配时返回 null（可根据业务修改为默认值）
     */
    @JsonCreator
    public static StatusEnumJackson fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StatusEnumJackson e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

}
