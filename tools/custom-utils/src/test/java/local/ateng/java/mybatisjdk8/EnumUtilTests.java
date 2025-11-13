package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.enums.BaseEnum;
import local.ateng.java.customutils.enums.StatusEnum;
import local.ateng.java.customutils.enums.TypeEnum;
import local.ateng.java.customutils.utils.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnumUtilTests {

    @Test
    void test00() {
        StatusEnum statusEnum = EnumUtil.getByFieldValue(StatusEnum.class, "code", 1);
        System.out.println(statusEnum.getName());
        System.out.println(statusEnum.getCode());
    }

    @Test
    void test001() {
        String name = EnumUtil.getNameByCode(StatusEnum.class, 1);
        System.out.println(name);
        // 输出: "启用"
        Integer code = EnumUtil.getCodeByName(StatusEnum.class, "禁用");
        System.out.println(code);
        // 输出: 0
    }

    @Test
    void test01() {
        Map<Integer, String> map = EnumUtil.toMap("code", "name", StatusEnum.class);
        System.out.println(map); // {1=启用, 0=禁用}
    }

    @Test
    void test02() {
        List<Map<String, Object>> labelValueList = EnumUtil.toLabelValueList("code", "name", StatusEnum.class);
        System.out.println(labelValueList); // [{label=禁用, value=0}, {label=启用, value=1}]
    }

    @Test
    void test03() {
        Map<String, List<Map<String, Object>>> listMap = EnumUtil.toMultiLabelValueMap("code", "name", StatusEnum.class, TypeEnum.class);
        System.out.println(listMap); // {Status=[{label=禁用, value=0}, {label=启用, value=1}], Type=[{label=禁用, value=0}, {label=启用, value=1}]}
    }

    @Test
    void test04() {
        TypeEnum e1 = BaseEnum.fromCode(TypeEnum.class, 1);
        TypeEnum e2 = BaseEnum.fromName(TypeEnum.class, "禁用");
        System.out.println(e1);  // ENABLED
        System.out.println(e2);  // DISABLED
    }

    @Test
    void test05() {
        Set<Class<? extends BaseEnum<?, ?>>> enums = EnumUtil.scanAllBaseEnums("local");
        System.out.println(enums);
    }

}
