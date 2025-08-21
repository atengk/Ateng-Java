package local.ateng.java.mybatisjdk8.enums;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StatusEnum {

    OFFLINE(0, "离线"),
    ONLINE(1, "在线");

    @EnumValue
    private final int code;
    //@JsonValue // Jackson
    //@JSONField(value = true) // Fastjson2
    private final String name;

    @JSONField // Fastjson1
    public String getValue() {
        return this.name;
    }

}
