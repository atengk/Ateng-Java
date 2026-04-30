package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObjectCollectionDesensitizedTest {

    @Test
    void shouldMaskMapAndMaps() {
        Map<String, Object> source = Map.of(
                "mobile", "13812345678",
                "email", "test@example.com",
                "remark", "普通备注"
        );
        Map<String, Object> masked = DesensitizedUtil.map(source, Map.of("remark", DesensitizedUtil.DesensitizedType.TEXT));
        assertEquals("138****5678", masked.get("mobile"));
        assertEquals("t**t@example.com", masked.get("email"));
        assertEquals("普通备注", masked.get("remark"));

        List<Map<String, Object>> rows = DesensitizedUtil.maps(List.of(source), null);
        assertEquals(1, rows.size());
        assertEquals("138****5678", rows.getFirst().get("mobile"));
    }

    @Test
    void shouldMaskBeanAndBeans() {
        UserInfo user = new UserInfo();
        user.mobile = "13812345678";
        user.email = "test@example.com";
        user.password = "secret";
        DesensitizedUtil.bean(user, null);
        assertEquals("138****5678", user.mobile);
        assertEquals("t**t@example.com", user.email);
        assertEquals("***", user.password);

        UserInfo another = new UserInfo();
        another.idCard = "110101199001011234";
        List<UserInfo> users = DesensitizedUtil.beans(List.of(another), null);
        assertEquals("110***********1234", users.getFirst().idCard);
    }

    @Test
    void shouldMaskFieldsAndJson() {
        Map<String, Object> fields = DesensitizedUtil.fields(Map.of("mobile", "13812345678", "amount", "100"));
        assertEquals("138****5678", fields.get("mobile"));
        assertEquals("***", fields.get("amount"));
        assertEquals("138****5678", DesensitizedUtil.field("mobile", "13812345678"));

        String json = DesensitizedUtil.json("{\"mobile\":\"13812345678\",\"age\":30,\"name\":\"张三\"}");
        assertTrue(json.contains("\"mobile\":\"138****5678\""));
        assertTrue(json.contains("\"name\":\"张*\""));

        String jsonObject = DesensitizedUtil.jsonObject("{\"email\":\"test@example.com\"}");
        assertTrue(jsonObject.contains("t**t@example.com"));

        String jsonArray = DesensitizedUtil.jsonArray("[{\"idCard\":\"110101199001011234\"}]");
        assertTrue(jsonArray.contains("110***********1234"));
    }

    @Test
    void shouldHandleBoundaryObjectValue() {
        assertTrue(DesensitizedUtil.map(null, null).isEmpty());
        assertTrue(DesensitizedUtil.maps(null, null).isEmpty());
        assertNull(DesensitizedUtil.bean(null, null));
        assertTrue(DesensitizedUtil.beans(null, null).isEmpty());
        assertNull(DesensitizedUtil.json(null));
    }

    static class UserInfo {
        String mobile;
        String email;
        String password;
        String idCard;
    }
}
