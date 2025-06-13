package local.ateng.java.mybatis.entity;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * Json表
 * </p>
 *
 * @author Ateng
 * @since 2025-06-13
 */
@Getter
@Setter
@ToString
@TableName(value = "my_json", autoResultMap = true)
public class MyJson implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 名称
     */
    @TableField("name")
    private String name;

    /**
     * JSONObject数据
     */
    @TableField(value = "my_json_object", typeHandler = JacksonTypeHandler.class)
    private JSONObject myJsonObject;

    /**
     * JSONOArray数据
     */
    @TableField(value = "my_json_array", typeHandler = JacksonTypeHandler.class)
    private JSONArray myJsonArray;
}
