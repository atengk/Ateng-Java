package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class TextContentDesensitizedTest {

    @Test
    void shouldMaskTextContent() {
        assertEquals("手机号 138****5678", DesensitizedUtil.text("手机号 13812345678"));
        assertEquals("邮箱 t**t@example.com", DesensitizedUtil.content("邮箱 test@example.com"));
        assertEquals("备注 110***********1234", DesensitizedUtil.remark("备注 110101199001011234"));
        assertEquals("卡号 6222***********2020", DesensitizedUtil.message("卡号 6222020202020202020"));
        assertEquals("手机号 138****5678", DesensitizedUtil.logMessage("手机号 13812345678"));
    }

    @Test
    void shouldReplaceByKeywordAndRegex() {
        assertEquals("用户***访问", DesensitizedUtil.replaceSensitiveWords("用户密码访问", List.of("密码")));
        assertEquals("订单***", DesensitizedUtil.replaceByRegex("订单ABC123", "[A-Z]{3}\\d+", "***"));
        assertEquals("abc123***", DesensitizedUtil.maskMatched("abc123456", "\\d+", 3, 0));
        assertEquals("手机 138****5678", DesensitizedUtil.maskMobileInText("手机 13812345678"));
        assertEquals("邮箱 t**t@example.com", DesensitizedUtil.maskEmailInText("邮箱 test@example.com"));
        assertEquals("证件 110***********1234", DesensitizedUtil.maskIdCardInText("证件 110101199001011234"));
        assertEquals("卡号 6222***********2020", DesensitizedUtil.maskBankCardInText("卡号 6222020202020202020"));
    }

    @Test
    void shouldHandleBoundaryAndRegexException() {
        assertNull(DesensitizedUtil.text(null));
        assertEquals("abc", DesensitizedUtil.replaceByKeywords("abc", null));
        assertThrows(PatternSyntaxException.class, () -> DesensitizedUtil.replaceByRegex("abc", "[", "*"));
        assertThrows(IllegalArgumentException.class, () -> DesensitizedUtil.maskMatched("abc", "[a-z]", -1, 0));
    }
}
